package com.agent.projects.toolagent;

import com.agent.core.tools.ToolManager;
import com.agent.model.ApiTool;
import com.agent.model.ChatCompletionRequest;
import com.agent.model.ChatCompletionResponse;
import com.agent.model.Message;
import com.agent.service.LlmClientService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具智能体服务。
 * 能力：天气查询、网页搜索、计算、日期时间等多工具协同调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolAgentService {

    /** `llmClientService`：模型调用服务。 */
    private final LlmClientService llmClientService;

    /** `toolManager`：工具注册、查询与执行入口。 */
    private final ToolManager toolManager;

    /** `objectMapper`：解析模型生成的工具参数 JSON。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 工具智能体实例。
     */
    public static class ToolAgent {
        /** `llmClientService`：模型调用服务。 */
        private final LlmClientService llmClientService;

        /** `toolManager`：工具管理器。 */
        private final ToolManager toolManager;

        /** `objectMapper`：JSON 解析器。 */
        private final ObjectMapper objectMapper;

        /** `model`：当前智能体使用的模型名。 */
        private String model;

        /** `maxIterations`：最多工具循环轮次，防止死循环。 */
        private int maxIterations = 5;

        /** `verbose`：是否打印详细日志。 */
        private boolean verbose = true;

        public ToolAgent(LlmClientService llmClientService, ToolManager toolManager, ObjectMapper objectMapper) {
            this.llmClientService = llmClientService;
            this.toolManager = toolManager;
            this.objectMapper = objectMapper;
            this.model = llmClientService.getProviderConfig().model();
        }

        /**
         * 设置当前模型。
         */
        public void setModel(String model) {
            this.model = model;
        }

        /**
         * 设置最大迭代轮次。
         */
        public void setMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
        }

        /**
         * 设置是否输出详细日志。
         */
        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        /**
         * 运行工具智能体。
         *
         * @param query 用户问题
         * @return 工具智能体执行结果
         * @throws JsonProcessingException 工具参数 JSON 解析异常
         */
        public ToolAgentResult run(String query) throws JsonProcessingException {
            if (verbose) {
                log.info("");
                log.info("=".repeat(60));
                log.info("问题: {}", query);
                log.info("=".repeat(60));
            }

            // `messages`：发送给模型的上下文消息序列
            List<Message> messages = new ArrayList<>();
            messages.add(Message.system("""
                    你是一个能够使用工具的智能助手。
                    请根据用户问题选择合适的工具获取信息，然后给出完整回答。
                    如果有需要，可以串联多个工具。
                    """));
            messages.add(Message.user(query));

            // `apiTools`：提供给模型的工具定义列表
            List<ApiTool> apiTools = toolManager.getApiToolList();
            // `toolExecutions`：记录每次工具调用过程，便于回显和调试
            List<ToolExecution> toolExecutions = new ArrayList<>();

            for (int iteration = 0; iteration < maxIterations; iteration++) {
                if (verbose) {
                    log.info("");
                    log.info("--- 迭代 {} ---", iteration + 1);
                }

                ChatCompletionRequest request = ChatCompletionRequest.builder()
                        .messages(messages)
                        .tools(apiTools)
                        .toolChoice("auto")
                        .temperature(0.0)
                        .model(model)
                        .build();

                ChatCompletionResponse response = llmClientService.chatCompletion(request);
                Message assistantMessage = response.getChoices().get(0).getMessage();
                messages.add(assistantMessage);

                if (assistantMessage.getToolCalls() == null || assistantMessage.getToolCalls().length == 0) {
                    String finalAnswer = assistantMessage.getContent();
                    if (verbose) {
                        log.info("");
                        log.info("最终回答: {}", finalAnswer);
                    }
                    return new ToolAgentResult(finalAnswer, toolExecutions, iteration + 1);
                }

                for (var toolCall : assistantMessage.getToolCalls()) {
                    String toolName = toolCall.getFunction().getName();
                    String toolArgs = toolCall.getFunction().getArguments();

                    if (verbose) {
                        log.info("调用工具: {}", toolName);
                        log.info("参数: {}", toolArgs);
                    }

                    String result = toolManager.execute(toolName, objectMapper.readTree(toolArgs));

                    if (verbose) {
                        log.info("结果: {}", result.length() > 100 ? result.substring(0, 100) + "..." : result);
                    }

                    toolExecutions.add(new ToolExecution(toolName, toolArgs, result));
                    messages.add(Message.tool(toolCall.getId(), toolName, result));
                }
            }

            return new ToolAgentResult("达到最大迭代次数，无法完成任务。", toolExecutions, maxIterations);
        }

        /**
         * 生成可用工具列表文本。
         */
        public String listTools() {
            StringBuilder sb = new StringBuilder("可用工具:\n");
            for (ToolManager.ToolInfo info : toolManager.getToolInfos()) {
                String desc = info.description().length() > 50
                        ? info.description().substring(0, 50) + "..."
                        : info.description();
                sb.append("  - ").append(info.name()).append(": ").append(desc).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * 创建新的工具智能体实例。
     */
    public ToolAgent createAgent() {
        return new ToolAgent(llmClientService, toolManager, objectMapper);
    }

    /**
     * 工具智能体运行结果。
     *
     * @param finalAnswer 最终回答文本
     * @param toolExecutions 工具执行过程列表
     * @param iterations 实际迭代次数
     */
    public record ToolAgentResult(
            String finalAnswer,
            List<ToolExecution> toolExecutions,
            int iterations
    ) {
    }

    /**
     * 单次工具调用记录。
     *
     * @param toolName 工具名
     * @param arguments 调用参数 JSON 文本
     * @param result 工具返回结果
     */
    public record ToolExecution(
            String toolName,
            String arguments,
            String result
    ) {
    }
}
