package com.agent.basics.api;

import com.agent.model.ChatCompletionRequest;
import com.agent.model.ChatCompletionResponse;
import com.agent.model.Message;
import com.agent.service.LlmClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型兼容接口基础用法示例服务。
 * 用途：演示单轮、多轮、参数控制、JSON 输出和错误处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiBasicService {

    /** `llmClientService`：统一的大模型调用入口。 */
    private final LlmClientService llmClientService;

    /** `objectMapper`：用于解析模型返回的 JSON 文本。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 基础对话示例：最小化请求参数，直接获取一条助手回复。
     *
     * @return 模型完整响应对象
     */
    public ChatCompletionResponse basicChat() {
        // `messages`：本次请求发送给模型的消息列表
        List<Message> messages = List.of(
                Message.system("你是一个有帮助的助手。"),
                Message.user("你好，请简单介绍一下你自己。")
        );

        // `request`：聊天补全请求体
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(messages)
                .temperature(0.7)
                .build();

        ChatCompletionResponse response = llmClientService.chatCompletion(request);

        log.info("=== 基础对话 ===");
        log.info("模型: {}", response.getModel());
        log.info("回复: {}", response.getChoices().get(0).getMessage().getContent());
        log.info("Token 使用: {}", response.getUsage());

        return response;
    }

    /**
     * 多轮对话示例：将历史消息持续加入请求，保持上下文连续。
     *
     * @return 对话结束后的完整消息列表
     */
    public List<Message> multiTurnChat() {
        // `messages`：会不断追加用户与助手消息，形成会话历史
        List<Message> messages = new ArrayList<>();
        messages.add(Message.system("你是一位数学老师，用简洁易懂的语言解释概念。"));

        // `conversations`：本示例预设的用户输入序列
        String[] conversations = {
                "什么是质数？",
                "能举几个例子吗？",
                "100 以内有多少个质数？"
        };

        log.info("");
        log.info("=== 多轮对话 ===");

        for (String userInput : conversations) {
            messages.add(Message.user(userInput));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .messages(messages)
                    .build();

            ChatCompletionResponse response = llmClientService.chatCompletion(request);
            // `assistantMessage`：当前轮次模型返回的文本
            String assistantMessage = response.getChoices().get(0).getMessage().getContent();
            messages.add(Message.assistant(assistantMessage));

            log.info("");
            log.info("用户: {}", userInput);
            log.info("助手: {}", assistantMessage);
        }

        return messages;
    }

    /**
     * 温度参数对比示例：观察随机性变化对结果的影响。
     *
     * @return 键为温度值、值为模型回复
     */
    public Map<Double, String> temperatureComparison() {
        String prompt = "给我一个创业点子。";

        log.info("");
        log.info("=== 温度参数对比 ===");

        // `results`：按插入顺序记录不同温度下的输出
        Map<Double, String> results = new LinkedHashMap<>();

        for (double temp : List.of(0.0, 0.5, 1.0)) {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .messages(List.of(Message.user(prompt)))
                    .temperature(temp)
                    .maxTokens(100)
                    .build();

            ChatCompletionResponse response = llmClientService.chatCompletion(request);
            String content = response.getChoices().get(0).getMessage().getContent();

            results.put(temp, content);
            log.info("");
            log.info("temperature={}: {}", temp, content);
        }

        return results;
    }

    /**
     * 最大输出长度示例：观察不同上限下回答的长度差异。
     *
     * @return 键为 `maxTokens`，值为对应回复
     */
    public Map<Integer, String> maxTokensExample() {
        String prompt = "详细介绍 Java 编程语言的历史和特点。";

        log.info("");
        log.info("=== 最大输出长度示例 ===");

        Map<Integer, String> results = new LinkedHashMap<>();

        for (int tokens : List.of(50, 100, 200)) {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .messages(List.of(Message.user(prompt)))
                    .maxTokens(tokens)
                    .build();

            ChatCompletionResponse response = llmClientService.chatCompletion(request);
            String content = response.getChoices().get(0).getMessage().getContent();

            results.put(tokens, content);
            log.info("");
            log.info("maxTokens={}（实际 {} 字符）: {}", tokens, content.length(), content);
        }

        return results;
    }

    /**
     * JSON 模式示例：要求模型只返回可解析的 JSON 字符串。
     *
     * @return 解析后的 JSON 节点；解析失败时返回 `null`
     */
    public JsonNode jsonMode() {
        List<Message> messages = List.of(
                Message.system("你是一个 API，只返回 JSON 格式数据。"),
                Message.user("分析“北京今天晴，气温 25 度”，提取城市、天气、温度。")
        );

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(messages)
                .responseFormat(Map.of("type", "json_object"))
                .build();

        ChatCompletionResponse response = llmClientService.chatCompletion(request);

        log.info("");
        log.info("=== JSON 模式 ===");

        try {
            JsonNode result = objectMapper.readTree(response.getChoices().get(0).getMessage().getContent());
            log.info(result.toString());
            return result;
        } catch (Exception e) {
            log.error("解析 JSON 响应失败", e);
            return null;
        }
    }

    /**
     * 错误处理示例：按异常信息区分限流、连接或通用错误。
     *
     * @return 成功时返回模型文本，失败时返回错误信息
     */
    public String errorHandling() {
        log.info("");
        log.info("=== 错误处理示例 ===");

        try {
            String response = llmClientService.chat("测试");
            log.info("请求成功");
            return response;
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("rate limit")) {
                log.info("触发限流: {}", message);
            } else if (message.contains("connection")) {
                log.info("连接错误: {}", message);
            } else {
                log.info("接口错误: {}", message);
            }
            return "Error: " + message;
        }
    }

    /**
     * 顺序执行本类全部示例并返回汇总结果。
     *
     * @return 示例名称到执行结果的映射
     */
    public Map<String, Object> runAllExamples() {
        Map<String, Object> results = new LinkedHashMap<>();

        basicChat();
        results.put("basic_chat", "查看日志");

        multiTurnChat();
        results.put("multi_turn_chat", "查看日志");

        results.put("temperature_comparison", temperatureComparison());
        results.put("max_tokens_example", maxTokensExample());
        results.put("json_mode", jsonMode());
        results.put("error_handling", errorHandling());

        return results;
    }
}
