package com.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具函数调用模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionCall {

    /** `name`：函数名称，对应工具定义里的函数名。 */
    private String name;

    /** `arguments`：函数参数，JSON 字符串格式。 */
    private String arguments;
}
