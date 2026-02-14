package com.agent.controller;

import com.agent.projects.chat.ChatAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 对话智能体接口控制器。
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatAgentController {

    /** `chatAgentService`：创建与管理对话智能体的服务。 */
    private final ChatAgentService chatAgentService;

    /**
     * 创建会话接口。
     * 说明：当前实现只生成并返回一个 `sessionId`。
     *
     * @return 会话标识
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createSession() {
        String sessionId = java.util.UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    /**
     * 发送消息接口：创建一个智能体并执行一次对话。
     *
     * @param request 请求体，包含用户消息和可选人设
     * @return 对话响应
     */
    @PostMapping("/send")
    public ResponseEntity<ChatAgentService.ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        ChatAgentService.ChatAgent agent = chatAgentService.createAgent();

        if (request.persona() != null && !request.persona().isEmpty()) {
            try {
                ChatAgentService.Persona persona = ChatAgentService.Persona.valueOf(request.persona().toUpperCase());
                agent.setPersona(persona);
            } catch (IllegalArgumentException e) {
                // 人设非法时保持默认人设，不中断主流程
            }
        }

        ChatAgentService.ChatResponse response = agent.chat(request.message());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取可选人设列表。
     *
     * @return 人设信息数组
     */
    @GetMapping("/personas")
    public ResponseEntity<List<ChatAgentService.PersonaInfo>> getPersonas() {
        return ResponseEntity.ok(chatAgentService.getAvailablePersonas());
    }

    /**
     * 对话请求体。
     *
     * @param message 用户输入文本
     * @param persona 人设键值（可选）
     */
    public record ChatRequest(
            String message,
            String persona
    ) {
    }
}
