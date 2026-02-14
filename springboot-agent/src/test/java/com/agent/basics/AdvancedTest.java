package com.agent.basics;

import com.agent.basics.api.StreamingService;
import com.agent.core.react.ReactAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 进阶功能测试 - 第二阶段
 */
@SpringBootTest
public class AdvancedTest {

    @Autowired
    private StreamingService streamingService;

    @Autowired
    private ReactAgentService reactAgentService;

    /**
     * 测试1：流式响应
     */
    @Test
    void testStreaming() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        StringBuilder fullContent = new StringBuilder();

        streamingService.streamChat(
                "用一句话介绍Java",
                chunk -> {
                    fullContent.append(chunk);
                    System.out.print(chunk);
                },
                () -> {
                    success.set(true);
                    latch.countDown();
                },
                error -> {
                    System.err.println("错误: " + error.getMessage());
                    latch.countDown();
                }
        );

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        System.out.println("\n=== 流式响应完成 ===");
        System.out.println("完整内容: " + fullContent.toString());
        assertTrue(completed || fullContent.length() > 0);
    }

    /**
     * 测试2：ReAct Agent - 计算问题
     */
    @Test
    void testReactCalculate() {
        var result = reactAgentService.runReactAgent("计算 (15 + 25) * 3 的结果");

        System.out.println("=== ReAct计算测试 ===");
        System.out.println("迭代次数: " + result.iterations());
        System.out.println("最终答案: " + result.finalAnswer());

        assertNotNull(result.finalAnswer());
    }

    /**
     * 测试3：ReAct Agent - 搜索问题
     */
    @Test
    void testReactSearch() {
        var result = reactAgentService.runReactAgent("请搜索什么是Python");

        System.out.println("=== ReAct搜索测试 ===");
        System.out.println("迭代次数: " + result.iterations());
        System.out.println("最终答案: " + result.finalAnswer());

        assertNotNull(result.finalAnswer());
    }

    /**
     * 测试4：ReAct Agent - 天气查询
     */
    @Test
    void testReactWeather() {
        var result = reactAgentService.runReactAgent("北京今天的天气怎么样？");

        System.out.println("=== ReAct天气测试 ===");
        System.out.println("迭代次数: " + result.iterations());
        System.out.println("最终答案: " + result.finalAnswer());

        assertNotNull(result.finalAnswer());
    }

    /**
     * 测试5：列出可用工具
     */
    @Test
    void testListTools() {
        List<String> tools = reactAgentService.listTools();

        System.out.println("=== 可用工具 ===");
        tools.forEach(System.out::println);

        assertTrue(tools.size() >= 3);
        assertTrue(tools.contains("search"));
        assertTrue(tools.contains("calculate"));
        assertTrue(tools.contains("get_weather"));
    }
}
