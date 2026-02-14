package com.agent.core.memory;

import com.agent.model.ChatCompletionRequest;
import com.agent.model.Message;
import com.agent.service.LlmClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 对话记忆示例服务。
 * 包含多种记忆策略：完整缓冲、窗口裁剪、按 token 裁剪、带时间戳记忆。
 */
@Slf4j
@Service
public class ConversationMemoryService {

    /**
     * 完整缓冲记忆：保留全部会话历史。
     */
    public static class ConversationBufferMemory {
        /** `messages`：完整消息列表，首条一般是系统提示词。 */
        private final List<Message> messages;

        /** `systemPrompt`：系统角色提示词，用于重置时恢复。 */
        private final String systemPrompt;

        public ConversationBufferMemory(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            this.messages = new ArrayList<>();
            this.messages.add(Message.system(systemPrompt));
        }

        public ConversationBufferMemory() {
            this("你是一个有帮助的助手。");
        }

        /**
         * 添加用户消息。
         *
         * @param content 用户输入正文
         */
        public void addUserMessage(String content) {
            messages.add(Message.user(content));
        }

        /**
         * 添加助手消息。
         *
         * @param content 助手输出正文
         */
        public void addAssistantMessage(String content) {
            messages.add(Message.assistant(content));
        }

        /**
         * 获取历史消息副本，避免外部直接改写内部列表。
         */
        public List<Message> getMessages() {
            return new ArrayList<>(messages);
        }

        /**
         * 清空历史，仅保留系统提示词。
         */
        public void clear() {
            messages.clear();
            messages.add(Message.system(systemPrompt));
        }

        /**
         * 以可读文本格式输出历史（跳过首条系统消息）。
         */
        public String getHistoryString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < messages.size(); i++) {
                Message msg = messages.get(i);
                String role = "user".equals(msg.getRole()) ? "用户" : "助手";
                sb.append(role).append(": ").append(msg.getContent()).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * 窗口记忆：仅保留最近 N 轮消息（用户+助手算一轮）。
     */
    public static class ConversationWindowMemory {
        /** `windowSize`：保留的最近轮次数。 */
        private final int windowSize;

        /** `systemPrompt`：每次请求时注入的系统提示词。 */
        private final String systemPrompt;

        /** `messages`：窗口内的普通消息（不含系统提示词）。 */
        private final List<Message> messages;

        public ConversationWindowMemory(int windowSize, String systemPrompt) {
            this.windowSize = windowSize;
            this.systemPrompt = systemPrompt;
            this.messages = new ArrayList<>();
        }

        public ConversationWindowMemory(int windowSize) {
            this(windowSize, "你是一个有帮助的助手。");
        }

        /**
         * 添加用户消息并触发窗口裁剪。
         */
        public void addUserMessage(String content) {
            messages.add(Message.user(content));
            trim();
        }

        /**
         * 添加助手消息并触发窗口裁剪。
         */
        public void addAssistantMessage(String content) {
            messages.add(Message.assistant(content));
            trim();
        }

        /**
         * 按窗口大小裁剪历史。
         * 计算规则：一轮消息约等于 2 条（用户+助手）。
         */
        private void trim() {
            int maxMessages = windowSize * 2;
            if (messages.size() > maxMessages) {
                messages.subList(0, messages.size() - maxMessages).clear();
            }
        }

        /**
         * 获取给模型使用的消息列表（包含系统提示词）。
         */
        public List<Message> getMessages() {
            List<Message> result = new ArrayList<>();
            result.add(Message.system(systemPrompt));
            result.addAll(messages);
            return result;
        }
    }

    /**
     * token 记忆：按最大 token 预算裁剪历史。
     * 注意：本类使用粗略估算，不是精确 token 统计。
     */
    public static class ConversationTokenBufferMemory {
        /** `maxTokens`：允许发送给模型的近似 token 上限。 */
        private final int maxTokens;

        /** `systemPrompt`：系统提示词，会固定计入 token 预算。 */
        private final String systemPrompt;

        /** `messages`：参与 token 裁剪的历史消息（不含系统提示词）。 */
        private final List<Message> messages;

        public ConversationTokenBufferMemory(int maxTokens, String systemPrompt) {
            this.maxTokens = maxTokens;
            this.systemPrompt = systemPrompt;
            this.messages = new ArrayList<>();
        }

        public ConversationTokenBufferMemory(int maxTokens) {
            this(maxTokens, "你是一个有帮助的助手。");
        }

        /**
         * 粗略估算 token 数。
         * 经验值：中文平均约 2 个字符 ≈ 1 个 token。
         */
        private int estimateTokens(String text) {
            return text.length() / 2;
        }

        /**
         * 添加用户消息并按预算裁剪。
         */
        public void addUserMessage(String content) {
            messages.add(Message.user(content));
            trim();
        }

        /**
         * 添加助手消息并按预算裁剪。
         */
        public void addAssistantMessage(String content) {
            messages.add(Message.assistant(content));
            trim();
        }

        /**
         * 从最新消息向前回溯，尽量保留更多近邻上下文。
         */
        private void trim() {
            int totalTokens = estimateTokens(systemPrompt);
            List<Message> keepMessages = new ArrayList<>();

            for (int i = messages.size() - 1; i >= 0; i--) {
                Message msg = messages.get(i);
                int msgTokens = estimateTokens(msg.getContent());
                if (totalTokens + msgTokens <= maxTokens) {
                    keepMessages.add(0, msg);
                    totalTokens += msgTokens;
                } else {
                    break;
                }
            }
            messages.clear();
            messages.addAll(keepMessages);
        }

        /**
         * 获取给模型使用的消息列表（含系统提示词）。
         */
        public List<Message> getMessages() {
            List<Message> result = new ArrayList<>();
            result.add(Message.system(systemPrompt));
            result.addAll(messages);
            return result;
        }
    }

    /**
     * 带时间戳的消息结构。
     *
     * @param role 角色（`user` / `assistant` / `system` 等）
     * @param content 消息正文
     * @param timestamp 记录写入时间
     * @param metadata 附加元数据
     */
    public record TimestampedMessage(
            String role,
            String content,
            LocalDateTime timestamp,
            Map<String, Object> metadata
    ) {
        public TimestampedMessage(String role, String content, Map<String, Object> metadata) {
            this(role, content, LocalDateTime.now(), metadata != null ? metadata : Map.of());
        }

        public TimestampedMessage(String role, String content) {
            this(role, content, LocalDateTime.now(), Map.of());
        }
    }

    /**
     * 带时间戳记忆容器。
     */
    public static class TimestampedMemory {
        /** `systemPrompt`：系统提示词。 */
        private final String systemPrompt;

        /** `history`：按时间追加的消息历史。 */
        private final List<TimestampedMessage> history;

        public TimestampedMemory(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            this.history = new ArrayList<>();
        }

        public TimestampedMemory() {
            this("你是一个有帮助的助手。");
        }

        /**
         * 添加消息（可带元数据）。
         */
        public void addMessage(String role, String content, Map<String, Object> metadata) {
            history.add(new TimestampedMessage(role, content, metadata));
        }

        /**
         * 添加消息（不带元数据）。
         */
        public void addMessage(String role, String content) {
            addMessage(role, content, null);
        }

        /**
         * 获取最近 N 分钟的消息。
         *
         * @param minutes 时间窗口（分钟）
         * @return 过滤后的消息列表
         */
        public List<TimestampedMessage> getRecent(int minutes) {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
            List<TimestampedMessage> result = new ArrayList<>();
            for (TimestampedMessage msg : history) {
                if (msg.timestamp().isAfter(cutoff)) {
                    result.add(msg);
                }
            }
            return result;
        }

        /**
         * 转换为模型接口需要的消息格式。
         */
        public List<Message> getMessagesForApi() {
            List<Message> result = new ArrayList<>();
            result.add(Message.system(systemPrompt));
            for (TimestampedMessage msg : history) {
                result.add(Message.builder().role(msg.role()).content(msg.content()).build());
            }
            return result;
        }
    }

    /**
     * 记忆型聊天机器人示例。
     */
    public static class ChatBot {
        /** `memoryType`：记忆策略类型（`buffer`/`window`/`token`）。 */
        private final String memoryType;

        /** `bufferMemory`：完整缓冲记忆实例。 */
        private final ConversationBufferMemory bufferMemory;

        /** `windowMemory`：窗口记忆实例。 */
        private final ConversationWindowMemory windowMemory;

        /** `tokenMemory`：token 裁剪记忆实例。 */
        private final ConversationTokenBufferMemory tokenMemory;

        public ChatBot(String memoryType) {
            this.memoryType = memoryType;
            this.bufferMemory = new ConversationBufferMemory();
            this.windowMemory = new ConversationWindowMemory(3);
            this.tokenMemory = new ConversationTokenBufferMemory(1000);
        }

        /**
         * 使用指定记忆策略执行一次问答。
         *
         * @param userPrompt 用户输入
         * @param llmClientService 模型调用服务
         * @return 助手回复
         */
        public String chat(String userPrompt, LlmClientService llmClientService) {
            if (memoryType.equals("buffer")) {
                bufferMemory.addUserMessage(userPrompt);
                ChatCompletionRequest request = ChatCompletionRequest.builder().messages(bufferMemory.getMessages()).build();
                String assistantResponse = llmClientService.chatCompletion(request).getChoices().get(0).getMessage().getContent();
                bufferMemory.addAssistantMessage(assistantResponse);
                return assistantResponse;
            } else if (memoryType.equals("window")) {
                windowMemory.addUserMessage(userPrompt);
                ChatCompletionRequest request = ChatCompletionRequest.builder().messages(windowMemory.getMessages()).build();
                String assistantResponse = llmClientService.chatCompletion(request).getChoices().get(0).getMessage().getContent();
                windowMemory.addAssistantMessage(assistantResponse);
                return assistantResponse;
            } else {
                tokenMemory.addUserMessage(userPrompt);
                ChatCompletionRequest request = ChatCompletionRequest.builder().messages(tokenMemory.getMessages()).build();
                String assistantResponse = llmClientService.chatCompletion(request).getChoices().get(0).getMessage().getContent();
                tokenMemory.addAssistantMessage(assistantResponse);
                return assistantResponse;
            }
        }

        /**
         * 获取历史文本（仅 `buffer` 策略支持完整文本输出）。
         */
        public String getHistory() {
            if (memoryType.equals("buffer")) {
                return bufferMemory.getHistoryString();
            }
            return "";
        }
    }
}
