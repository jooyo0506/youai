package com.agent.basics.prompt;

import com.agent.model.ChatCompletionRequest;
import com.agent.model.Message;
import com.agent.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提示词基础能力示例服务。
 * 包含：基础补全、系统提示词、结构化输出、角色扮演、少样本和分步推理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BasicPromptsService {

    /** `llmClientService`：执行大模型对话请求的服务。 */
    private final LlmClientService llmClientService;

    /**
     * 基础文本补全：把用户输入直接交给模型。
     *
     * @param prompt 用户输入
     * @return 模型回复
     */
    public String basicCompletion(String prompt) {
        return llmClientService.chat(prompt);
    }

    /**
     * 系统提示词示例：通过 `system` 消息约束回答风格。
     *
     * @return 模型回复
     */
    public String systemPromptExample() {
        List<Message> messages = List.of(
                Message.system("你是一位专业 Java 讲师，用简洁易懂的语言回答问题。"),
                Message.user("什么是注解（Annotation）？")
        );
        return sendMessage(messages);
    }

    /**
     * 结构化输出示例：要求模型只返回 JSON。
     *
     * @return 模型回复
     */
    public String structuredOutputExample() {
        String prompt = """
                请分析下面文本情绪，并仅返回 JSON：
                文本：今天项目终于跑通了，我很开心。
                返回格式：
                {
                  "sentiment": "positive/negative/neutral",
                  "confidence": 0.0,
                  "keywords": ["关键词"]
                }
                """;
        return basicCompletion(prompt);
    }

    /**
     * 角色扮演示例：让模型按指定角色进行回答。
     *
     * @return 模型回复
     */
    public String rolePlayingExample() {
        List<Message> messages = List.of(
                Message.system("""
                        你是苏格拉底式导师。
                        请通过提问一步步引导对方思考，不要直接给结论。
                        """),
                Message.user("什么是幸福？")
        );
        return sendMessage(messages);
    }

    /**
     * 少样本示例：先给样例，再让模型按样例格式输出。
     *
     * @return 模型回复
     */
    public String fewShotExample() {
        String prompt = """
                请把阿拉伯数字转成中文数字：

                示例1：
                输入：1
                输出：一

                示例2：
                输入：25
                输出：二十五

                示例3：
                输入：108
                输出：一百零八

                现在请转换：
                输入：365
                输出：
                """;
        return basicCompletion(prompt);
    }

    /**
     * 分步推理示例：要求模型按步骤展示推理过程。
     *
     * @return 模型回复
     */
    public String chainOfThoughtExample() {
        String prompt = """
                商店第一天卖出 50 个苹果，第二天比第一天多 20%，
                第三天卖出前两天总和。请计算三天总销量。
                请按以下步骤回答：
                1. 第二天销量
                2. 前两天总和
                3. 第三天销量
                4. 三天总销量
                """;
        return basicCompletion(prompt);
    }

    /**
     * 依次执行全部示例并返回结果。
     *
     * @return 示例名到模型输出的映射
     */
    public Map<String, String> runAllExamples() {
        Map<String, String> results = new HashMap<>();

        String result1 = basicCompletion("用一句话解释什么是人工智能。");
        results.put("basic_completion", result1);
        log.info("basic_completion: {}", result1);

        String result2 = systemPromptExample();
        results.put("system_prompt", result2);
        log.info("system_prompt: {}", result2);

        String result3 = structuredOutputExample();
        results.put("structured_output", result3);
        log.info("structured_output: {}", result3);

        String result4 = rolePlayingExample();
        results.put("role_playing", result4);
        log.info("role_playing: {}", result4);

        String result5 = fewShotExample();
        results.put("few_shot", result5);
        log.info("few_shot: {}", result5);

        String result6 = chainOfThoughtExample();
        results.put("chain_of_thought", result6);
        log.info("chain_of_thought: {}", result6);

        return results;
    }

    /**
     * 发送消息列表并返回助手文本。
     *
     * @param messages 消息列表
     * @return 助手回复
     */
    private String sendMessage(List<Message> messages) {
        // `messageList`：复制一份可变列表，避免修改调用方传入集合
        List<Message> messageList = new ArrayList<>(messages);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(messageList)
                .temperature(0.7)
                .build();

        return llmClientService.chatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
    }
}
