package com.agent.basics;

import com.agent.projects.chat.ChatAgentService;
import com.agent.projects.toolagent.ToolAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第三阶段：实战项目测试
 */
@SpringBootTest
public class ProjectTest {

    @Autowired
    private ChatAgentService chatAgentService;

    @Autowired
    private ToolAgentService toolAgentService;

    /**
     * 测试1：获取可用人设列表
     */
    @Test
    void testGetAvailablePersonas() {
        var personas = chatAgentService.getAvailablePersonas();

        System.out.println("=== 可用人设列表 ===");
        for (var p : personas) {
            System.out.println("- " + p.key() + ": " + p.name());
        }

        assertNotNull(personas);
        assertTrue(personas.size() >= 4);
    }

    /**
     * 测试2：创建对话智能体并进行对话
     */
    @Test
    void testChatAgentBasic() {
        var agent = chatAgentService.createAgent();

        // 第一轮对话
        var response1 = agent.chat("你好，请用一句话介绍自己");
        System.out.println("=== 对话1 ===");
        System.out.println("回复: " + response1.content());
        System.out.println("人设: " + response1.personaName());
        assertNotNull(response1.content());

        // 第二轮对话（测试记忆）
        var response2 = agent.chat("我刚才问你什么了？");
        System.out.println("=== 对话2（测试记忆）===");
        System.out.println("回复: " + response2.content());
        assertNotNull(response2.content());
    }

    /**
     * 测试3：切换人设
     */
    @Test
    void testPersonaSwitch() {
        var agent = chatAgentService.createAgent();

        // 切换到编程老师人设
        agent.setPersona(ChatAgentService.Persona.TEACHER);
        var response1 = agent.chat("什么是多态？");
        System.out.println("=== 编程老师人设 ===");
        System.out.println("回复: " + response1.content());
        assertNotNull(response1.content());

        // 切换到创意写手人设
        agent.setPersona(ChatAgentService.Persona.CREATIVE);
        var response2 = agent.chat("写一首关于春天的诗");
        System.out.println("=== 创意写手人设 ===");
        System.out.println("回复: " + response2.content());
        assertNotNull(response2.content());
    }

    /**
     * 测试4：使用命令控制
     */
    @Test
    void testCommands() {
        var agent = chatAgentService.createAgent();

        // 测试 /help 命令
        var helpResponse = agent.chat("/help");
        System.out.println("=== /help 命令 ===");
        System.out.println(helpResponse.content());
        assertTrue(helpResponse.isCommand());

        // 测试 /persona list 命令
        var listResponse = agent.chat("/persona list");
        System.out.println("=== /persona list 命令 ===");
        System.out.println(listResponse.content());
        assertTrue(listResponse.isCommand());

        // 测试 /clear 命令
        var clearResponse = agent.chat("/clear");
        System.out.println("=== /clear 命令 ===");
        System.out.println(clearResponse.content());
        assertTrue(clearResponse.isCommand());
    }

    /**
     * 测试5：创建工具智能体并调用工具
     */
    @Test
    void testToolAgent() throws Exception {
        var agent = toolAgentService.createAgent();

        // 测试天气查询
        var result1 = agent.run("北京今天天气怎么样？");
        System.out.println("=== 天气查询 ===");
        System.out.println("最终答案: " + result1.finalAnswer());
        System.out.println("迭代次数: " + result1.iterations());
        assertNotNull(result1.finalAnswer());

        // 打印工具调用记录
        if (!result1.toolExecutions().isEmpty()) {
            System.out.println("工具调用记录:");
            for (var exec : result1.toolExecutions()) {
                System.out.println("  - " + exec.toolName() + ": " + exec.arguments());
            }
        }
    }

    /**
     * 测试6：工具智能体计算功能
     */
    @Test
    void testToolAgentCalculate() throws Exception {
        var agent = toolAgentService.createAgent();

        var result = agent.run("请计算 (125 + 375) * 8 的结果");
        System.out.println("=== 工具智能体计算测试 ===");
        System.out.println("最终答案: " + result.finalAnswer());
        System.out.println("迭代次数: " + result.iterations());

        assertNotNull(result.finalAnswer());
    }

    /**
     * 测试7：列出工具智能体的可用工具
     */
    @Test
    void testListTools() {
        var agent = toolAgentService.createAgent();

        var toolsList = agent.listTools();
        System.out.println("=== 可用工具列表 ===");
        System.out.println(toolsList);

        assertNotNull(toolsList);
    }

    /**
     * 测试8：获取对话历史记录
     */
    @Test
    void testGetConversationHistory() {
        var agent = chatAgentService.createAgent();

        // 进行几轮对话
        agent.chat("你好");
        agent.chat("今天天气很好");
        agent.chat("我刚才说了什么？");

        // 获取最近的消息
        var recentMessages = agent.getRecentMessages(10);
        System.out.println("=== 对话历史记录 ===");
        System.out.println("最近消息数量: " + recentMessages.size());
        for (var msg : recentMessages) {
            System.out.println("[" + msg.timestamp() + "] " + msg.role() + ": " + msg.content());
        }

        assertTrue(recentMessages.size() >= 2);
    }
}
