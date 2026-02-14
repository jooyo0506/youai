package com.agent.core.tools;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.List;
import java.util.Map;

/**
 * 计算器工具。
 */
@Slf4j
@Component
public class CalculatorTool implements BaseTool {

    /** `engine`：用于执行数学表达式的脚本引擎。 */
    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");

    /**
     * 返回工具名称。
     */
    @Override
    public String getName() {
        return "calculator";
    }

    /**
     * 返回工具描述。
     */
    @Override
    public String getDescription() {
        return "计算数学表达式，支持加减乘除、幂运算和括号。";
    }

    /**
     * 定义计算器参数结构。
     */
    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "expression", Map.of(
                                "type", "string",
                                "description", "数学表达式，例如 2+3*4 或 Math.pow(2, 10)"
                        )
                ),
                "required", List.of("expression")
        );
    }

    /**
     * 执行表达式并返回结果。
     *
     * @param arguments 输入参数，读取 `expression` 字段
     * @return JSON 字符串结果
     */
    @Override
    public String run(JsonNode arguments) {
        // `expression`：用户要计算的数学表达式文本
        String expression = arguments.path("expression").asText();

        try {
            engine.eval("""
                    const sin = Math.sin;
                    const cos = Math.cos;
                    const tan = Math.tan;
                    const sqrt = Math.sqrt;
                    const log = Math.log;
                    const log10 = Math.log10;
                    const exp = Math.exp;
                    const pow = Math.pow;
                    const abs = Math.abs;
                    const PI = Math.PI;
                    const E = Math.E;
                    const floor = Math.floor;
                    const ceil = Math.ceil;
                    """);

            Object result = engine.eval(expression);
            return String.format("{\"expression\": \"%s\", \"result\": %s}", expression, result);
        } catch (Exception e) {
            return String.format("{\"error\": \"计算错误: %s\"}", e.getMessage());
        }
    }
}
