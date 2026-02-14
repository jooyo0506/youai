package com.agent.service;

import com.agent.config.LlmProperties;
import com.agent.model.ApiTool;
import com.agent.model.ChatCompletionRequest;
import com.agent.model.ChatCompletionResponse;
import com.agent.model.Message;
import com.agent.model.ToolCall;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 大语言模型客户端服务。
 * 作用：统一封装各家兼容 OpenAI 协议的聊天补全调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClientService {

    /** `llmProperties`：应用配置对象，包含提供商、密钥、模型等信息。 */
    private final LlmProperties llmProperties;

    /** `objectMapper`：JSON 序列化与反序列化工具。 */
    private final ObjectMapper objectMapper;

    /** `httpClient`：底层 HTTP 客户端，统一设置连接与读写超时。 */
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 根据 `llm.provider` 读取当前提供商配置。
     *
     * @return `ProviderConfig`：包含 `baseUrl`、`apiKey`、`model` 三个核心字段
     */
    public ProviderConfig getProviderConfig() {
        return switch (llmProperties.getProvider().toLowerCase()) {
            case "openai" -> new ProviderConfig(
                    llmProperties.getOpenai().getBaseUrl(),
                    llmProperties.getOpenai().getApiKey(),
                    llmProperties.getOpenai().getModel()
            );
            case "anthropic" -> new ProviderConfig(
                    llmProperties.getAnthropic().getBaseUrl(),
                    llmProperties.getAnthropic().getApiKey(),
                    llmProperties.getAnthropic().getModel()
            );
            default -> new ProviderConfig(
                    llmProperties.getDeepseek().getBaseUrl(),
                    llmProperties.getDeepseek().getApiKey(),
                    llmProperties.getDeepseek().getModel()
            );
        };
    }

    /**
     * 发送聊天补全请求并返回结构化响应。
     *
     * @param request 调用参数对象，包含消息、温度、工具等字段
     * @return `ChatCompletionResponse`：模型返回的完整响应
     */
    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request) {
        ProviderConfig config = getProviderConfig();

        if (request.getModel() == null) {
            request.setModel(config.model());
        }

        try {
            // `jsonBody`：最终发往模型服务端的请求 JSON 文本
            String jsonBody = objectMapper.writeValueAsString(request);

            // `httpRequest`：OkHttp 层面的 HTTP 请求对象
            Request httpRequest = new Request.Builder()
                    .url(config.baseUrl() + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + config.apiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                // `responseBody`：接口返回原始文本，失败时用于定位问题
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    log.error("模型接口调用失败: {} - {}", response.code(), responseBody);
                    throw new RuntimeException("模型接口调用失败: " + response.code() + " - " + responseBody);
                }

                return objectMapper.readValue(responseBody, ChatCompletionResponse.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("调用大模型接口失败", e);
        }
    }

    /**
     * 简化调用：仅传入用户提示词，使用默认参数执行一次对话。
     *
     * @param prompt 用户输入文本
     * @return 助手回复文本
     */
    public String chat(String prompt) {
        return chat(prompt, null, 0.7, null);
    }

    /**
     * 自定义参数对话。
     *
     * @param prompt 用户问题正文
     * @param systemPrompt 系统提示词，用于定义助手行为
     * @param temperature 采样温度，越高越发散
     * @param maxTokens 最长输出 token 数
     * @return 助手回复文本
     */
    public String chat(String prompt, String systemPrompt, Double temperature, Integer maxTokens) {
        // `messages`：本次会话发送给模型的消息序列
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null) {
            messages.add(Message.system(systemPrompt));
        }
        messages.add(Message.user(prompt));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        ChatCompletionResponse response = chatCompletion(request);
        return response.getChoices().get(0).getMessage().getContent();
    }

    /**
     * 发起带工具定义的对话请求，允许模型返回工具调用指令。
     *
     * @param messages 当前上下文消息
     * @param tools 可用工具定义列表
     * @param toolChoice 工具策略（`auto`、`none` 或具体工具名）
     * @return 模型响应对象
     */
    public ChatCompletionResponse chatWithTools(
            List<Message> messages,
            List<ApiTool> tools,
            String toolChoice
    ) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(messages)
                .tools(tools)
                .toolChoice(toolChoice != null ? toolChoice : "auto")
                .temperature(0.0)
                .build();

        return chatCompletion(request);
    }

    /**
     * 自动执行工具调用循环，直到模型给出最终答案或达到轮次上限。
     *
     * @param messages 初始消息列表
     * @param tools 可用工具定义
     * @param toolExecutor 工具执行器回调
     * @return 最终答案文本
     */
    public String executeToolCall(
            List<Message> messages,
            List<ApiTool> tools,
            ToolExecutor toolExecutor
    ) {
        // `maxIterations`：最多允许模型与工具往返的轮数，避免死循环
        int maxIterations = 5;

        // `conversation`：会动态追加助手消息和工具结果的工作副本
        List<Message> conversation = new ArrayList<>(messages);

        for (int i = 0; i < maxIterations; i++) {
            log.debug("工具调用迭代轮次 {}", i + 1);

            ChatCompletionResponse response = chatWithTools(conversation, tools, "auto");
            Message assistantMessage = response.getChoices().get(0).getMessage();
            conversation.add(assistantMessage);

            if (assistantMessage.getToolCalls() == null || assistantMessage.getToolCalls().length == 0) {
                return assistantMessage.getContent();
            }

            for (ToolCall toolCall : assistantMessage.getToolCalls()) {
                // `toolName`：模型选择要执行的工具名
                String toolName = toolCall.getFunction().getName();
                // `arguments`：模型给出的工具参数 JSON 字符串
                String arguments = toolCall.getFunction().getArguments();

                log.debug("执行工具: {}, 参数: {}", toolName, arguments);

                try {
                    // `argsNode`：解析后的参数树，便于执行器按字段读取
                    JsonNode argsNode = objectMapper.readTree(arguments);
                    String result = toolExecutor.execute(toolName, argsNode);
                    conversation.add(Message.tool(toolCall.getId(), toolName, result));
                } catch (Exception e) {
                    log.error("执行工具 {} 失败: {}", toolName, e.getMessage());
                    conversation.add(Message.tool(
                            toolCall.getId(),
                            toolName,
                            "{\"error\": \"" + e.getMessage() + "\"}"
                    ));
                }
            }
        }

        return "达到最大迭代次数，无法完成任务。";
    }

    /**
     * 提供商连接配置。
     *
     * @param baseUrl 接口基础地址
     * @param apiKey 鉴权密钥
     * @param model 默认模型名称
     */
    public record ProviderConfig(String baseUrl, String apiKey, String model) {
    }

    /**
     * 工具执行器函数式接口。
     */
    @FunctionalInterface
    public interface ToolExecutor {
        /**
         * 执行指定工具。
         *
         * @param toolName 工具名称
         * @param arguments 工具参数 JSON 树
         * @return 工具执行结果文本
         */
        String execute(String toolName, JsonNode arguments);
    }
}
