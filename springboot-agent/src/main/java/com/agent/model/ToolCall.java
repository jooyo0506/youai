package com.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用模型。
 * 用于承载 assistant 消息里的工具调用信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCall {

    /** `id`：本次工具调用唯一 ID。 */
    private String id;

    /** `function`：函数调用详情（函数名 + 参数）。 */
    private FunctionCall function;

    /** `type`：工具类型，固定为 `function`。 */
    private String type = "function";
}
