package com.agent.core.tools;

import com.agent.model.ApiTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工具管理器。
 * 负责工具注册、查询、元数据导出和执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolManager {

    /** `tools`：按注册顺序保存工具对象列表。 */
    private final List<BaseTool> tools = new ArrayList<>();

    /** `toolMap`：工具名称到工具实例的快速索引。 */
    private final Map<String, BaseTool> toolMap = new HashMap<>();

    /**
     * 注册工具；若同名工具已存在则忽略。
     *
     * @param tool 要注册的工具实例
     */
    public void registerTool(BaseTool tool) {
        if (!toolMap.containsKey(tool.getName())) {
            tools.add(tool);
            toolMap.put(tool.getName(), tool);
            log.info("注册工具: {}", tool.getName());
        }
    }

    /**
     * 按名称查询工具。
     *
     * @param name 工具名
     * @return 工具实例（可选）
     */
    public Optional<BaseTool> getTool(String name) {
        return Optional.ofNullable(toolMap.get(name));
    }

    /**
     * 获取全部工具的接口格式定义。
     */
    public List<Map<String, Object>> getApiTools() {
        return tools.stream().map(BaseTool::toApiFormat).toList();
    }

    /**
     * 获取全部工具的 `ApiTool` 列表。
     */
    public List<ApiTool> getApiToolList() {
        return tools.stream()
                .map(tool -> ApiTool.of(tool.getName(), tool.getDescription(), tool.getParameters()))
                .toList();
    }

    /**
     * 执行指定工具。
     *
     * @param name 工具名
     * @param arguments 工具参数
     * @return 工具返回结果字符串
     */
    public String execute(String name, JsonNode arguments) {
        BaseTool tool = toolMap.get(name);
        if (tool == null) {
            return String.format("{\"error\": \"工具 '%s' 不存在\"}", name);
        }

        try {
            log.debug("执行工具: {} 参数: {}", name, arguments);
            return tool.run(arguments);
        } catch (Exception e) {
            log.error("工具执行失败: {}", e.getMessage(), e);
            return String.format("{\"error\": \"%s\"}", e.getMessage());
        }
    }

    /**
     * 列出所有工具名称。
     */
    public List<String> listTools() {
        return new ArrayList<>(toolMap.keySet());
    }

    /**
     * 返回已注册工具数量。
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * 获取工具信息列表（名称+描述）。
     */
    public List<ToolInfo> getToolInfos() {
        return tools.stream().map(tool -> new ToolInfo(tool.getName(), tool.getDescription())).toList();
    }

    /**
     * 工具简要信息。
     *
     * @param name 工具名称
     * @param description 工具说明
     */
    public record ToolInfo(String name, String description) {
    }
}
