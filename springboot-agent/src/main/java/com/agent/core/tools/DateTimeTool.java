package com.agent.core.tools;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 日期时间工具。
 */
@Slf4j
@Component
public class DateTimeTool implements BaseTool {

    /** `DATE_FORMATTER`：日期格式（年-月-日）。 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** `DATETIME_FORMATTER`：日期时间格式（年-月-日 时:分:秒）。 */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 返回工具名称。
     */
    @Override
    public String getName() {
        return "get_datetime";
    }

    /**
     * 返回工具描述。
     */
    @Override
    public String getDescription() {
        return "获取当前日期时间，或执行日期加减、日期差计算。";
    }

    /**
     * 定义工具参数结构。
     */
    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "operation", Map.of(
                                "type", "string",
                                "enum", List.of("now", "add_days", "diff_days"),
                                "description", "操作类型：now=当前时间，add_days=日期加减，diff_days=计算天数差"
                        ),
                        "date", Map.of(
                                "type", "string",
                                "description", "日期，格式 yyyy-MM-dd"
                        ),
                        "days", Map.of(
                                "type", "integer",
                                "description", "增减天数（用于 add_days）"
                        )
                ),
                "required", List.of("operation")
        );
    }

    /**
     * 根据操作类型执行日期时间功能。
     *
     * @param arguments 输入参数
     * @return JSON 字符串结果
     */
    @Override
    public String run(JsonNode arguments) {
        // `operation`：本次要执行的子功能类型
        String operation = arguments.path("operation").asText();

        try {
            return switch (operation) {
                case "now" -> handleNow();
                case "add_days" -> handleAddDays(arguments);
                case "diff_days" -> handleDiffDays(arguments);
                default -> "{\"error\": \"无效的操作类型\"}";
            };
        } catch (Exception e) {
            return String.format("{\"error\": \"%s\"}", e.getMessage());
        }
    }

    /**
     * 返回当前日期时间信息。
     */
    private String handleNow() {
        LocalDateTime now = LocalDateTime.now();
        String weekday = getWeekdayName(now.getDayOfWeek());

        return String.format(
                "{\"datetime\": \"%s\", \"weekday\": \"%s\", \"timestamp\": %d}",
                now.format(DATETIME_FORMATTER), weekday, now.toEpochSecond(ZoneOffset.UTC) * 1000
        );
    }

    /**
     * 按指定天数对日期做加减。
     */
    private String handleAddDays(JsonNode arguments) {
        String dateStr = arguments.path("date").asText();
        int days = arguments.path("days").asInt();

        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
        LocalDate result = date.plusDays(days);

        return String.format(
                "{\"original\": \"%s\", \"days_added\": %d, \"result\": \"%s\", \"weekday\": \"%s\"}",
                dateStr, days, result.format(DATE_FORMATTER), getWeekdayName(result.getDayOfWeek())
        );
    }

    /**
     * 计算目标日期与今天的天数差。
     */
    private String handleDiffDays(JsonNode arguments) {
        String dateStr = arguments.path("date").asText();

        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
        LocalDate now = LocalDate.now();
        long diff = ChronoUnit.DAYS.between(now, date);

        return String.format(
                "{\"from\": \"%s\", \"to\": \"%s\", \"days_difference\": %d}",
                now.format(DATE_FORMATTER), dateStr, diff
        );
    }

    /**
     * 将星期枚举转换为中文。
     */
    private String getWeekdayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }
}
