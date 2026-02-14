package com.agent.controller;

import com.agent.core.react.ReactAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ReAct 智能体接口控制器。
 */
@RestController
@RequestMapping("/api/react")
@RequiredArgsConstructor
public class ReactAgentController {

    /** `reactAgentService`：封装 ReAct 推理与工具调用流程。 */
    private final ReactAgentService reactAgentService;

    /**
     * 运行 ReAct 流程。
     *
     * @param request 请求体，包含问题和可选最大轮次
     * @return 推理结果
     */
    @PostMapping("/run")
    public ResponseEntity<ReactAgentService.ReactResult> runReactAgent(@RequestBody ReactRequest request) {
        ReactAgentService.ReactResult result = reactAgentService.runReactAgent(
                request.question(),
                request.maxIterations() != null ? request.maxIterations() : 5
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 查看当前已注册的 ReAct 工具。
     *
     * @return 工具列表和数量
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> listTools() {
        return ResponseEntity.ok(Map.of(
                "tools", reactAgentService.listTools(),
                "count", reactAgentService.listTools().size()
        ));
    }

    /**
     * ReAct 请求体。
     *
     * @param question 用户问题文本
     * @param maxIterations 最大迭代轮次（可选）
     */
    public record ReactRequest(
            String question,
            Integer maxIterations
    ) {
    }
}
