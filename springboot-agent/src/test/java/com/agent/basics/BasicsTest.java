package com.agent.basics;

import com.agent.basics.prompt.BasicPromptsService;
import com.agent.basics.api.OpenAiBasicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础功能测试
 * 运行前请设置环境变量：DEEPSEEK_API_KEY 或 OPENAI_API_KEY
 */
@SpringBootTest
public class BasicsTest {

    @Autowired
    private BasicPromptsService basicPromptsService;

    @Autowired
    private OpenAiBasicService openAiBasicService;

    /**
     * 测试1：基础文本补全
     */
    @Test
    void testBasicCompletion() {
        String result = basicPromptsService.basicCompletion("你好，请介绍一下自己");
        assertNotNull(result);
        assertTrue(result.length() > 0);
        System.out.println("=== 基础文本补全 ===");
        System.out.println(result);
    }

    /**
     * 测试2：系统提示词 - 定义AI角色
     */
    @Test
    void testSystemPrompt() {
        String result = basicPromptsService.systemPromptExample();
        assertNotNull(result);
        System.out.println("=== 系统提示词 ===");
        System.out.println(result);
    }

    /**
     * 测试3：Few-shot示例学习
     */
    @Test
    void testFewShot() {
        String result = basicPromptsService.fewShotExample();
        assertNotNull(result);
        System.out.println("=== Few-shot示例 ===");
        System.out.println(result);
    }

    /**
     * 测试4：思维链推理
     */
    @Test
    void testChainOfThought() {
        String result = basicPromptsService.chainOfThoughtExample();
        assertNotNull(result);
        System.out.println("=== 思维链 ===");
        System.out.println(result);
    }

    /**
     * 测试5：基础对话
     */
    @Test
    void testBasicChat() {
        var response = openAiBasicService.basicChat();
        assertNotNull(response);
        assertNotNull(response.getChoices());
        assertTrue(response.getChoices().size() > 0);
        System.out.println("=== 基础对话 ===");
        System.out.println("模型: " + response.getModel());
        System.out.println("回复: " + response.getChoices().get(0).getMessage().getContent());
    }

    /**
     * 测试6：Temperature参数对比
     */
    @Test
    void testTemperature() {
        Map<Double, String> results = openAiBasicService.temperatureComparison();
        assertNotNull(results);
        assertEquals(3, results.size());
        System.out.println("=== Temperature参数对比 ===");
        results.forEach((k, v) -> System.out.println(k + ": " + v));
    }

    /**
     * 测试7：多轮对话
     */
    @Test
    void testMultiTurnChat() {
        var messages = openAiBasicService.multiTurnChat();
        assertNotNull(messages);
        assertTrue(messages.size() > 2);
        System.out.println("=== 多轮对话 ===");
        messages.forEach(m -> System.out.println(m.getRole() + ": " + m.getContent()));
    }
}
