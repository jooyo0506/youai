package com.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天补全响应模型。
 * 对应 OpenAI 兼容接口返回结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionResponse {

    /** `id`：本次响应的唯一标识。 */
    private String id;

    /** `object`：对象类型，通常是 `chat.completion`。 */
    private String object;

    /** `created`：响应创建时间戳（秒级）。 */
    private Long created;

    /** `model`：实际使用的模型名称。 */
    private String model;

    /** `choices`：候选回复列表。 */
    private List<Choice> choices;

    /** `usage`：token 使用统计信息。 */
    private Usage usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        /** `index`：候选回复在数组中的序号。 */
        private Integer index;

        /** `message`：模型返回的消息体。 */
        private Message message;

        /** `finishReason`：停止原因（例如 `stop`、`length`、`tool_calls`）。 */
        private String finishReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        /** `promptTokens`：输入 token 数。 */
        private Integer promptTokens;

        /** `completionTokens`：输出 token 数。 */
        private Integer completionTokens;

        /** `totalTokens`：总 token 数（输入 + 输出）。 */
        private Integer totalTokens;
    }
}
