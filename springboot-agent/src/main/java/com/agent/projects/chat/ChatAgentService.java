package com.agent.projects.chat;

import com.agent.core.memory.ConversationMemoryService;
import com.agent.model.ChatCompletionRequest;
import com.agent.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 对话智能体服务。
 * 支持多轮对话、记忆管理、人设切换和命令控制。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAgentService {

    /** `llmClientService`：大模型调用服务。 */
    private final LlmClientService llmClientService;

    /**
     * 预置人设定义。
     */
    public enum Persona {
        ASSISTANT("小助手", "你是一个友好、有帮助的 AI 助手。请用简洁清晰的语言回答问题。"),
        TEACHER("编程老师", """
                你是一位经验丰富的编程老师。
                - 用简单易懂的语言解释概念
                - 给出实际的代码示例
                - 鼓励学生思考和提问
                - 如果学生有错误，请耐心引导纠正
                """),
        CREATIVE("创意写手", """
                你是一位才华横溢的创意写手。
                - 语言生动有趣
                - 善于使用比喻和修辞
                - 能够写诗、故事、文案等各种内容
                - 风格可以根据需求调整
                """),
        ANALYST("数据分析师", """
                你是一位专业的数据分析师。
                - 善于分析问题和数据
                - 提供结构化分析框架
                - 用数据和事实支持观点
                - 给出可执行的建议
                """),
        CUSTOM("自定义角色", "");

        /** `name`：人设显示名称。 */
        private final String name;

        /** `systemPrompt`：该人设对应的系统提示词。 */
        private final String systemPrompt;

        Persona(String name, String systemPrompt) {
            this.name = name;
            this.systemPrompt = systemPrompt;
        }

        /**
         * 返回人设显示名。
         */
        public String getName() {
            return name;
        }

        /**
         * 返回人设系统提示词。
         */
        public String getSystemPrompt() {
            return systemPrompt;
        }
    }

    /**
     * 获取全部可选人设信息。
     */
    public List<PersonaInfo> getAvailablePersonas() {
        List<PersonaInfo> list = new ArrayList<>();
        for (Persona p : Persona.values()) {
            list.add(new PersonaInfo(p.name().toLowerCase(), p.getName(), p.getSystemPrompt()));
        }
        return list;
    }

    /**
     * 对话智能体实例。
     */
    public static class ChatAgent {
        /** `llmClientService`：模型调用服务。 */
        private final LlmClientService llmClientService;

        /** `persona`：当前生效的人设。 */
        private Persona persona = Persona.ASSISTANT;

        /** `customSystemPrompt`：自定义人设提示词，仅 CUSTOM 模式使用。 */
        private String customSystemPrompt = "";

        /** `memory`：带时间戳的对话记忆。 */
        private ConversationMemoryService.TimestampedMemory memory;

        /** `model`：当前使用的模型名。 */
        private String model;

        /** `temperature`：采样温度，控制回答发散度。 */
        private double temperature = 0.7;

        /** `stream`：是否启用流式（当前同步接口暂未实际使用）。 */
        private boolean stream = true;

        public ChatAgent(LlmClientService llmClientService) {
            this.llmClientService = llmClientService;
            this.memory = new ConversationMemoryService.TimestampedMemory(persona.getSystemPrompt());
            this.model = llmClientService.getProviderConfig().model();
        }

        /**
         * 切换预设人设，并重置记忆上下文。
         */
        public void setPersona(Persona persona) {
            this.persona = persona;
            this.customSystemPrompt = "";
            this.memory = new ConversationMemoryService.TimestampedMemory(persona.getSystemPrompt());
        }

        /**
         * 设置自定义系统提示词，并切换为自定义人设。
         */
        public void setCustomSystemPrompt(String prompt) {
            this.customSystemPrompt = prompt;
            this.persona = Persona.CUSTOM;
            this.memory = new ConversationMemoryService.TimestampedMemory(prompt);
        }

        /**
         * 处理用户输入并返回回答。
         *
         * @param userInput 用户输入文本
         * @return 对话结果
         */
        public ChatResponse chat(String userInput) {
            if (userInput.startsWith("/")) {
                return handleCommand(userInput);
            }

            memory.addMessage("user", userInput);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .messages(memory.getMessagesForApi())
                    .model(model)
                    .temperature(temperature)
                    .stream(false)
                    .build();

            String responseText = llmClientService.chatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();

            memory.addMessage("assistant", responseText);
            return new ChatResponse(responseText, false, persona.getName());
        }

        /**
         * 处理斜杠命令，例如 `/help`、`/persona`、`/clear`。
         */
        private ChatResponse handleCommand(String command) {
            String[] parts = command.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String arg = parts.length > 1 ? parts[1] : "";

            return switch (cmd) {
                case "/help" -> new ChatResponse(getHelp(), true, "系统");
                case "/clear" -> {
                    resetMemory();
                    yield new ChatResponse("对话记录已清空。", true, "系统");
                }
                case "/persona" -> {
                    if (!arg.isEmpty()) {
                        if (arg.equals("list")) {
                            yield new ChatResponse(listAvailablePersonas(), true, "系统");
                        }
                        try {
                            Persona p = Persona.valueOf(arg.toUpperCase());
                            setPersona(p);
                            yield new ChatResponse("已切换到人设: " + persona.getName(), true, "系统");
                        } catch (IllegalArgumentException e) {
                            yield new ChatResponse("未知人设: " + arg, true, "系统");
                        }
                    }
                    yield new ChatResponse("当前人设: " + persona.getName(), true, "系统");
                }
                case "/custom" -> {
                    if (!arg.isEmpty()) {
                        setCustomSystemPrompt(arg);
                        yield new ChatResponse("已设置自定义人设。", true, "系统");
                    }
                    yield new ChatResponse("请提供自定义系统提示词。", true, "系统");
                }
                case "/model" -> {
                    if (!arg.isEmpty()) {
                        this.model = arg;
                        yield new ChatResponse("模型已切换为: " + arg, true, "系统");
                    }
                    yield new ChatResponse("当前模型: " + model, true, "系统");
                }
                case "/temp" -> {
                    try {
                        if (!arg.isEmpty()) {
                            this.temperature = Double.parseDouble(arg);
                            yield new ChatResponse("temperature 已设置为: " + temperature, true, "系统");
                        }
                    } catch (NumberFormatException e) {
                        // 参数非法时回退到当前值展示
                    }
                    yield new ChatResponse("当前 temperature: " + temperature, true, "系统");
                }
                case "/stream" -> {
                    this.stream = !this.stream;
                    yield new ChatResponse("流式输出: " + (stream ? "开启" : "关闭"), true, "系统");
                }
                default -> new ChatResponse("未知命令: " + cmd + "，输入 /help 查看帮助。", true, "系统");
            };
        }

        /**
         * 根据当前人设重置记忆。
         */
        private void resetMemory() {
            String systemPrompt = persona == Persona.CUSTOM ? customSystemPrompt : persona.getSystemPrompt();
            memory = new ConversationMemoryService.TimestampedMemory(systemPrompt);
        }

        /**
         * 返回命令帮助文本。
         */
        private String getHelp() {
            return """
                    可用命令：
                      /help              - 显示帮助信息
                      /clear             - 清空对话记录
                      /persona [名称]     - 切换或查看人设（list 查看可用人设）
                      /custom [提示词]    - 设置自定义人设
                      /model [模型名]     - 切换或查看模型
                      /temp [值]          - 设置 temperature（0-1）
                      /stream            - 切换流式输出
                    """;
        }

        /**
         * 返回可用人设列表文本。
         */
        private String listAvailablePersonas() {
            return "可用人设: " + String.join(", ",
                    Arrays.stream(Persona.values())
                            .filter(p -> p != Persona.CUSTOM)
                            .map(p -> p.name().toLowerCase())
                            .toList());
        }

        /**
         * 获取最近消息记录。
         *
         * @param count 返回条数上限
         * @return 最近对话列表
         */
        public List<ConversationEntry> getRecentMessages(int count) {
            return memory.getRecent(30).stream()
                    .limit(count)
                    .map(msg -> new ConversationEntry(
                            msg.role(),
                            msg.content(),
                            msg.timestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    ))
                    .toList();
        }
    }

    /**
     * 创建新的对话智能体实例。
     */
    public ChatAgent createAgent() {
        return new ChatAgent(llmClientService);
    }

    /**
     * 对话响应结构。
     *
     * @param content 回复内容
     * @param isCommand 是否由命令触发
     * @param personaName 当前人设名称
     */
    public record ChatResponse(
            String content,
            boolean isCommand,
            String personaName
    ) {
    }

    /**
     * 人设信息结构。
     *
     * @param key 人设键（程序内部值）
     * @param name 人设显示名
     * @param systemPrompt 人设提示词
     */
    public record PersonaInfo(
            String key,
            String name,
            String systemPrompt
    ) {
    }

    /**
     * 对话记录条目。
     *
     * @param role 消息角色
     * @param content 消息内容
     * @param timestamp 消息时间
     */
    public record ConversationEntry(
            String role,
            String content,
            String timestamp
    ) {
    }
}
