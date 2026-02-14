package com.agent.controller;

import com.agent.basics.prompt.BasicPromptsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 提示词示例接口控制器。
 */
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    /** `basicPromptsService`：封装所有提示词示例逻辑。 */
    private final BasicPromptsService basicPromptsService;

    /**
     * 基础补全接口：读取请求体中的 `prompt` 并返回模型结果。
     *
     * @param request 键值请求体，至少包含 `prompt`
     * @return 响应结果
     */
    @PostMapping("/basic")
    public ResponseEntity<Map<String, String>> basicPrompt(@RequestBody Map<String, String> request) {
        String result = basicPromptsService.basicCompletion(request.get("prompt"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * 系统提示词示例接口。
     */
    @GetMapping("/system-prompt")
    public ResponseEntity<Map<String, String>> systemPromptExample() {
        String result = basicPromptsService.systemPromptExample();
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * 结构化输出示例接口。
     */
    @GetMapping("/structured-output")
    public ResponseEntity<Map<String, String>> structuredOutputExample() {
        String result = basicPromptsService.structuredOutputExample();
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * 角色扮演示例接口。
     */
    @GetMapping("/role-playing")
    public ResponseEntity<Map<String, String>> rolePlayingExample() {
        String result = basicPromptsService.rolePlayingExample();
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * 少样本示例接口。
     */
    @GetMapping("/few-shot")
    public ResponseEntity<Map<String, String>> fewShotExample() {
        String result = basicPromptsService.fewShotExample();
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * 分步推理示例接口。
     */
    @GetMapping("/chain-of-thought")
    public ResponseEntity<Map<String, String>> chainOfThoughtExample() {
        String result = basicPromptsService.chainOfThoughtExample();
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * 一次运行全部提示词示例。
     */
    @GetMapping("/run-all")
    public ResponseEntity<Map<String, String>> runAllExamples() {
        return ResponseEntity.ok(basicPromptsService.runAllExamples());
    }
}
