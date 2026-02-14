package com.agent.core.tools;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 天气查询工具（示例数据）。
 */
@Slf4j
@Component
public class WeatherTool implements BaseTool {

    /**
     * 返回工具名称。
     */
    @Override
    public String getName() {
        return "get_weather";
    }

    /**
     * 返回工具描述。
     */
    @Override
    public String getDescription() {
        return "查询指定城市天气，包含温度、天气、湿度和风力。";
    }

    /**
     * 定义天气工具参数结构。
     */
    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of(
                                "type", "string",
                                "description", "城市名称，例如 北京、上海"
                        ),
                        "unit", Map.of(
                                "type", "string",
                                "enum", List.of("celsius", "fahrenheit"),
                                "description", "温度单位"
                        )
                ),
                "required", List.of("city")
        );
    }

    /**
     * 根据城市返回天气结果。
     *
     * @param arguments 输入参数
     * @return JSON 字符串天气数据
     */
    @Override
    public String run(JsonNode arguments) {
        // `city`：目标城市
        String city = arguments.path("city").asText();
        // `unit`：温度单位，默认摄氏度
        String unit = arguments.has("unit") ? arguments.path("unit").asText() : "celsius";

        Map<String, WeatherData> weatherData = Map.of(
                "北京", new WeatherData(25, "晴", 45, "东北风 3 级"),
                "上海", new WeatherData(28, "多云", 65, "东风 2 级"),
                "广州", new WeatherData(32, "雷阵雨", 80, "南风 4 级"),
                "深圳", new WeatherData(30, "晴间多云", 70, "东南风 3 级"),
                "杭州", new WeatherData(27, "阴", 60, "西北风 2 级"),
                "成都", new WeatherData(22, "小雨", 75, "微风")
        );

        WeatherData data = weatherData.get(city);
        if (data == null) {
            return String.format("{\"error\": \"未找到城市 '%s' 的天气信息\"}", city);
        }

        int temp = data.temp();
        if ("fahrenheit".equals(unit)) {
            temp = (int) (temp * 9.0 / 5.0 + 32);
        }
        String unitStr = "celsius".equals(unit) ? "°C" : "°F";

        return String.format(
                "{\"city\": \"%s\", \"temperature\": \"%d%s\", \"condition\": \"%s\", \"humidity\": \"%d%%\", \"wind\": \"%s\"}",
                city, temp, unitStr, data.condition(), data.humidity(), data.wind()
        );
    }

    /**
     * 城市天气数据结构。
     *
     * @param temp 温度（摄氏度）
     * @param condition 天气状况
     * @param humidity 湿度百分比
     * @param wind 风力描述
     */
    private record WeatherData(int temp, String condition, int humidity, String wind) {
    }
}
