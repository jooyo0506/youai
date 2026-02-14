package com.agent.core.react;

import com.agent.model.ChatCompletionRequest;
import com.agent.model.Message;
import com.agent.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReAct 模式实现服务。
 * ReAct = 推理（Reasoning）+ 行动（Acting）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactAgentService {

    /** `llmClientService`：模型调用服务。 */
    private final LlmClientService llmClientService;

    /** `scriptEngine`：用于执行计算表达式。 */
    private final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("javascript");

    /** `tools`：已注册工具映射，键是工具名，值是工具实现。 */
    private final Map<String, ReactTool> tools = new LinkedHashMap<>();

    /**
     * Bean 初始化后注册默认工具。
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        registerDefaultTools();
    }

    /**
     * 注册内置工具：搜索、计算、天气。
     */
    private void registerDefaultTools() {
        registerTool(new ReactTool("search", "搜索信息。输入：搜索关键词。", this::search));
        registerTool(new ReactTool("calculate", "计算数学表达式。输入：例如 2+3*4。", this::calculate));
        registerTool(new ReactTool("get_weather", "查询城市天气。输入：城市名。", this::getWeather));
    }

    /**
     * 注册一个可用工具。
     *
     * @param tool 工具对象
     */
    public void registerTool(ReactTool tool) {
        tools.put(tool.getName(), tool);
        log.info("注册工具: {}", tool.getName());
    }

    /**
     * 构造 ReAct 提示词模板。
     *
     * @param question 用户问题
     * @return 完整提示词
     */
    private String createReactPrompt(String question) {
        StringBuilder toolDescriptions = new StringBuilder();
        for (var entry : tools.entrySet()) {
            ReactTool tool = entry.getValue();
            toolDescriptions.append("- ").append(tool.getName())
                    .append(": ").append(tool.getDescription()).append("\n");
        }

        return String.format("""
                你是一个能够使用工具解决问题的智能助手。
                可用工具：
                %s

                请按以下格式思考和行动：
                Thought: 我需要如何思考这个问题
                Action: 工具名称
                Action Input: 工具输入
                Observation: 工具返回结果
                ...（重复以上过程直到得到答案）
                Thought: 我现在知道最终答案了
                Final Answer: 最终答案

                问题：%s

                让我开始思考：
                """, toolDescriptions, question);
    }

    /**
     * 从模型输出中解析 `Action`。
     */
    private Optional<String> parseAction(String response) {
        Pattern pattern = Pattern.compile("Action:\\s*(\\w+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    /**
     * 从模型输出中解析 `Action Input`。
     */
    private Optional<String> parseActionInput(String response) {
        Pattern pattern = Pattern.compile("Action Input:\\s*(.+?)(?=\\n|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).trim());
        }
        return Optional.empty();
    }

    /**
     * 从模型输出中解析 `Final Answer`。
     */
    private Optional<String> parseFinalAnswer(String response) {
        Pattern pattern = Pattern.compile("Final Answer:\\s*(.+?)(?=\\n|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).trim());
        }
        return Optional.empty();
    }

    /**
     * 执行 ReAct 主循环：推理 -> 选工具 -> 执行工具 -> 继续推理。
     *
     * @param question 用户问题
     * @param maxIterations 最大迭代轮次
     * @return ReAct 执行结果
     */
    public ReactResult runReactAgent(String question, int maxIterations) {
        return runReactAgent(question, maxIterations, true);
    }

    /**
     * 执行 ReAct 主循环（带验证选项）。
     *
     * @param question 用户问题
     * @param maxIterations 最大迭代轮次
     * @param enableSelfCheck 是否启用 Self-Check 验证答案
     * @return ReAct 执行结果
     */
    public ReactResult runReactAgent(String question, int maxIterations, boolean enableSelfCheck) {
        log.info("");
        log.info("=".repeat(60));
        log.info("问题: {}", question);
        log.info("Self-Check: {}", enableSelfCheck ? "启用" : "禁用");
        log.info("=".repeat(60));

        String prompt = createReactPrompt(question);
        // `fullResponse`：累计记录模型推理轨迹和观察结果
        StringBuilder fullResponse = new StringBuilder();

        for (int i = 0; i < maxIterations; i++) {
            log.info("");
            log.info("--- 迭代 {} ---", i + 1);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .messages(List.of(Message.user(prompt + fullResponse)))
                    .temperature(0.0)
                    .stop(List.of("Observation:"))
                    .build();

            String llmOutput = llmClientService.chatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();

            fullResponse.append(llmOutput);
            log.info(llmOutput);

            Optional<String> finalAnswer = parseFinalAnswer(llmOutput);
            if (finalAnswer.isPresent()) {
                // Self-Check: 验证答案是否正确
                if (enableSelfCheck) {
                    log.info("");
                    log.info(">>> Self-Check: 验证答案...");

                    boolean isValid = verifyAnswer(question, finalAnswer.get());
                    if (isValid) {
                        log.info(">>> Self-Check: 答案验证通过 ✓");
                        return new ReactResult(finalAnswer.get(), fullResponse.toString(), i + 1);
                    } else {
                        log.info(">>> Self-Check: 答案可能不正确，继续验证...");
                        // 添加反馈，继续迭代
                        fullResponse.append("\nThought: 上一个答案可能不正确，我需要重新验证或搜索更多信息。\n");
                        continue;
                    }
                }

                log.info("");
                log.info("最终答案: {}", finalAnswer.get());
                return new ReactResult(finalAnswer.get(), fullResponse.toString(), i + 1);
            }

            Optional<String> action = parseAction(llmOutput);
            Optional<String> actionInput = parseActionInput(llmOutput);

            if (action.isPresent() && actionInput.isPresent()) {
                String toolName = action.get();
                String input = actionInput.get();

                if (tools.containsKey(toolName)) {
                    log.info("");
                    log.info("执行工具: {}({})", toolName, input);
                    String toolResult = tools.get(toolName).execute(input);
                    String observation = "\nObservation: " + toolResult + "\n";
                    fullResponse.append(observation);
                    log.info("观察结果: {}", toolResult);
                } else {
                    fullResponse.append("\nObservation: 工具 '").append(toolName).append("' 不存在\n");
                }
            } else {
                fullResponse.append("\nThought: ");
            }
        }

        return new ReactResult("达到最大迭代次数，未能得出答案。", fullResponse.toString(), maxIterations);
    }

    /**
     * Self-Check: 验证答案是否正确。
     * 通过让 LLM 重新审视问题和答案来判断。
     *
     * @param question 原始问题
     * @param answer 候选答案
     * @return 验证结果
     */
    private boolean verifyAnswer(String question, String answer) {
        String verifyPrompt = String.format("""
                请验证以下答案是否正确回答了问题。

                问题: %s
                答案: %s

                请仔细检查：
                1. 答案是否直接回答了问题？
                2. 答案中的事实是否合理？
                3. 是否有明显的错误或矛盾？

                如果答案正确，请回复：VERIFIED
                如果答案可能错误，请回复：INVALID 并说明原因
                """, question, answer);

        ChatCompletionRequest verifyRequest = ChatCompletionRequest.builder()
                .messages(List.of(Message.user(verifyPrompt)))
                .temperature(0.0)
                .build();

        String verifyResult = llmClientService.chatCompletion(verifyRequest)
                .getChoices().get(0).getMessage().getContent();

        log.info("验证结果: {}", verifyResult);
        return verifyResult.contains("VERIFIED");
    }

    /**
     * 使用默认最大迭代轮次（5）运行 ReAct。
     */
    public ReactResult runReactAgent(String question) {
        return runReactAgent(question, 5);
    }

    /**
     * 搜索工具（示例数据）。
     */
    private String search(String query) {
        Map<String, String> mockResults = Map.of(
                "python", "Python 是解释型高级通用编程语言，由 Guido van Rossum 于 1991 年发布。",
                "langchain", "LangChain 是构建大模型应用的开发框架，提供链、代理、记忆等组件。",
                "agent", "智能体是能够感知环境、做决策并执行动作的软件系统。"
        );

        String queryLower = query.toLowerCase();
        for (var entry : mockResults.entrySet()) {
            if (entry.getKey().toLowerCase().contains(queryLower)
                    || queryLower.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        return "未找到关于 '" + query + "' 的信息。";
    }

    /**
     * 计算工具。
     */
    private String calculate(String expression) {
        try {
            Object result = scriptEngine.eval(expression);
            return String.valueOf(result);
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }

    /**
     * 天气工具（示例数据）。
     */
    private String getWeather(String city) {
        Map<String, String> weatherData = Map.of(
                "北京", "晴，25°C",
                "上海", "多云，28°C",
                "深圳", "小雨，30°C"
        );

        return weatherData.getOrDefault(city, "未找到 " + city + " 的天气信息。");
    }

    /**
     * 列出当前可用工具名称。
     */
    public List<String> listTools() {
        return new ArrayList<>(tools.keySet());
    }

    /**
     * ReAct 工具定义。
     */
    public static class ReactTool {
        /** `name`：工具名称，供模型在 `Action` 中引用。 */
        private final String name;

        /** `description`：工具用途描述，供模型选择时参考。 */
        private final String description;

        /** `executeFunction`：工具实际执行函数。 */
        private final java.util.function.Function<String, String> executeFunction;

        public ReactTool(String name, String description, java.util.function.Function<String, String> executeFunction) {
            this.name = name;
            this.description = description;
            this.executeFunction = executeFunction;
        }

        /**
         * 返回工具名称。
         */
        public String getName() {
            return name;
        }

        /**
         * 返回工具描述。
         */
        public String getDescription() {
            return description;
        }

        /**
         * 执行工具函数。
         *
         * @param input 输入参数
         * @return 执行结果
         */
        public String execute(String input) {
            return executeFunction.apply(input);
        }
    }

    /**
     * ReAct 运行结果。
     *
     * @param finalAnswer 最终答案
     * @param fullResponse 全量推理轨迹文本
     * @param iterations 实际迭代轮次
     */
    public record ReactResult(
            String finalAnswer,
            String fullResponse,
            int iterations
    ) {
    }
}
