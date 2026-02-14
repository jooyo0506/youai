package com.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * API 工具定义模型。
 * 对应 OpenAI 兼容接口中的 `tools` 字段结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiTool {

    /** `type`：工具类型，固定为 `function`。 */
    private String type = "function";

    /** `function`：函数定义对象，包含名称、描述、参数结构。 */
    private FunctionDefinition function;

    /**
     * 便捷工厂方法：快速构造一个工具定义。
     *
     * @param name 函数名
     * @param description 函数用途说明
     * @param parameters 函数参数 JSON Schema
     * @return 构造完成的 `ApiTool` 对象
     */
    public static ApiTool of(String name, String description, Map<String, Object> parameters) {
        return ApiTool.builder()
                .type("function")
                .function(FunctionDefinition.builder()
                        .name(name)
                        .description(description)
                        .parameters(parameters)
                        .build())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionDefinition {
        /** `name`：函数名称。 */
        private String name;
        /** `description`：函数用途说明。 */
        private String description;
        /** `parameters`：函数参数 JSON Schema。 */
        private Map<String, Object> parameters;
    }
}
