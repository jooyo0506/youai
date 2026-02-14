package com.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天补全请求模型。
 * 该对象会被序列化为 `/chat/completions` 请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    /**
     * `model`：要调用的模型名称。
     * 示例：`deepseek-chat`、`gpt-4o-mini`。
     */
    private String model;

    /**
     * `messages`：消息列表，按对话顺序传给模型。
     */
    private List<Message> messages;

    /**
     * `temperature`：采样温度，范围通常为 0~2。
     * 值越高随机性越强，值越低输出越稳定。
     */
    private Double temperature;

    /**
     * `maxTokens`：模型最大输出 token 数上限。
     */
    private Integer maxTokens;

    /**
     * `stop`：停止词列表，命中后立即截断输出。
     */
    private List<String> stop;

    /**
     * `stream`：是否开启流式返回。
     * `true` 为增量输出，`false` 为一次性输出。
     */
    private Boolean stream;

    /**
     * `tools`：可供模型调用的工具定义列表。
     */
    private List<ApiTool> tools;

    /**
     * `toolChoice`：工具调用策略。
     * 可选：`auto`、`none` 或指定某个工具。
     */
    private Object toolChoice;

    /**
     * `responseFormat`：响应格式约束配置。
     * 例如：`{"type":"json_object"}`。
     */
    private Map<String, Object> responseFormat;
}
