package com.agent.core.tools;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * 工具基础接口。
 * 所有工具都需要提供名称、描述、参数定义和执行逻辑。
 */
public interface BaseTool {

    /**
     * 返回工具名称。
     * 该名称会映射到模型工具调用中的 `function.name`。
     */
    String getName();

    /**
     * 返回工具描述，帮助模型理解工具用途。
     */
    String getDescription();

    /**
     * 返回工具参数的 JSON Schema 定义。
     */
    Map<String, Object> getParameters();

    /**
     * 执行工具并返回结果字符串。
     *
     * @param arguments 工具参数 JSON
     * @return 执行结果（通常为 JSON 字符串）
     */
    String run(JsonNode arguments);

    /**
     * 转换为接口需要的 `tools` 数据结构。
     */
    default Map<String, Object> toApiFormat() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", getName(),
                        "description", getDescription(),
                        "parameters", getParameters()
                )
        );
    }
}
