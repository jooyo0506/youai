package com.agent.controller;

import com.agent.core.tools.ToolManager;
import com.agent.projects.toolagent.ToolAgentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 工具智能体接口控制器。
 */
@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolAgentController {

    /** `toolAgentService`：创建并执行工具智能体。 */
    private final ToolAgentService toolAgentService;

    /** `toolManager`：工具注册与直接调用入口。 */
    private final ToolManager toolManager;

    /** `objectMapper`：将请求参数转换为 JSON 树。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 运行工具智能体，让模型自动选择并调用工具。
     *
     * @param request 智能体执行请求体
     * @return 智能体执行结果
     * @throws JsonProcessingException 工具参数解析失败时抛出
     */
    @PostMapping("/agent/run")
    public ResponseEntity<ToolAgentService.ToolAgentResult> runAgent(@RequestBody AgentRunRequest request)
            throws JsonProcessingException {
        ToolAgentService.ToolAgent agent = toolAgentService.createAgent();

        if (request.verbose() != null) {
            agent.setVerbose(request.verbose());
        }

        ToolAgentService.ToolAgentResult result = agent.run(request.query());
        return ResponseEntity.ok(result);
    }

    /**
     * 列出当前系统注册的全部工具。
     *
     * @return 工具列表与总数
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listTools() {
        return ResponseEntity.ok(Map.of(
                "tools", toolManager.getToolInfos(),
                "count", toolManager.getToolCount()
        ));
    }

    /**
     * 直接执行一次指定工具。
     *
     * @param request 工具执行请求体
     * @return 工具执行结果
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeTool(@RequestBody ToolExecutionRequest request) {
        String result = toolManager.execute(request.toolName(), objectMapper.valueToTree(request.arguments()));
        return ResponseEntity.ok(Map.of(
                "toolName", request.toolName(),
                "result", result
        ));
    }

    /**
     * 智能体运行请求体。
     *
     * @param query 用户问题
     * @param verbose 是否输出详细日志
     */
    public record AgentRunRequest(
            String query,
            Boolean verbose
    ) {
    }

    /**
     * 工具执行请求体。
     *
     * @param toolName 工具名称
     * @param arguments 工具参数对象
     */
    public record ToolExecutionRequest(
            String toolName,
            Map<String, Object> arguments
    ) {
    }
}
