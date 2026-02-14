package com.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息模型。
 * 用于统一表达 system/user/assistant/tool 四类消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    /** `role`：消息角色（system/user/assistant/tool）。 */
    private String role;

    /** `content`：消息正文内容。 */
    private String content;

    /** `toolCallId`：工具调用 ID（仅 tool 消息需要）。 */
    private String toolCallId;

    /** `name`：工具名称（仅 tool 消息需要）。 */
    private String name;

    /** `toolCalls`：工具调用列表（仅 assistant 消息可能携带）。 */
    private ToolCall[] toolCalls;

    /** 生成用户消息对象。 */
    public static Message user(String content) {
        return Message.builder().role("user").content(content).build();
    }

    /** 生成系统消息对象。 */
    public static Message system(String content) {
        return Message.builder().role("system").content(content).build();
    }

    /** 生成助手消息对象。 */
    public static Message assistant(String content) {
        return Message.builder().role("assistant").content(content).build();
    }

    /** 生成工具响应消息对象。 */
    public static Message tool(String toolCallId, String name, String content) {
        return Message.builder().role("tool").toolCallId(toolCallId).name(name).content(content).build();
    }
}
