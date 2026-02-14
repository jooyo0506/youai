package com.agent.basics;

import com.agent.projects.rag.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG（检索增强生成）功能测试
 */
@SpringBootTest
public class RagTest {

    @Autowired
    private RagService ragService;

    /**
     * 测试1：加载示例文档
     */
    @Test
    void testLoadSampleDocuments() {
        // 先清空
        ragService.clearKnowledgeBase();

        // 加载示例文档
        ragService.loadSampleDocuments();

        // 验证文档数量
        int count = ragService.getDocumentCount();
        System.out.println("=== 加载示例文档 ===");
        System.out.println("文档数量: " + count);

        assertTrue(count > 0);
    }

    /**
     * 测试2：RAG问答 - Java相关问题
     */
    @Test
    void testRagQueryJava() {
        // 确保有文档
        ragService.clearKnowledgeBase();
        ragService.loadSampleDocuments();

        // 提问Java相关问题
        String answer = ragService.query("Java有什么特点？");

        System.out.println("=== RAG问答 - Java ===");
        System.out.println("问题: Java有什么特点？");
        System.out.println("回答: " + answer);

        assertNotNull(answer);
        assertTrue(answer.length() > 0);
    }

    /**
     * 测试3：RAG问答 - Python相关问题
     */
    @Test
    void testRagQueryPython() {
        ragService.clearKnowledgeBase();
        ragService.loadSampleDocuments();

        String answer = ragService.query("Python适合做什么开发？");

        System.out.println("=== RAG问答 - Python ===");
        System.out.println("问题: Python适合做什么开发？");
        System.out.println("回答: " + answer);

        assertNotNull(answer);
    }

    /**
     * 测试4：RAG问答 - Agent相关问题
     */
    @Test
    void testRagQueryAgent() {
        ragService.clearKnowledgeBase();
        ragService.loadSampleDocuments();

        String answer = ragService.query("什么是AI Agent？");

        System.out.println("=== RAG问答 - Agent ===");
        System.out.println("问题: 什么是AI Agent？");
        System.out.println("回答: " + answer);

        assertNotNull(answer);
    }

    /**
     * 测试5：RAG问答 - RAG相关问题
     */
    @Test
    void testRagQueryRag() {
        ragService.clearKnowledgeBase();
        ragService.loadSampleDocuments();

        String answer = ragService.query("什么是RAG？RAG解决什么问题？");

        System.out.println("=== RAG问答 - RAG ===");
        System.out.println("问题: 什么是RAG？RAG解决什么问题？");
        System.out.println("回答: " + answer);

        assertNotNull(answer);
    }

    /**
     * 测试6：添加自定义文档
     */
    @Test
    void testAddCustomDocument() {
        ragService.clearKnowledgeBase();

        // 添加自定义文档
        String customDoc = "Spring Boot是一个基于Spring框架的快速开发框架。" +
                "它提供了自动配置、嵌入式服务器、starter依赖等特性。" +
                "可以快速创建独立的、生产级别的Spring应用。";

        ragService.addDocument(customDoc, Map.of("source", "custom", "topic", "spring-boot"));

        // 验证文档数量
        int count = ragService.getDocumentCount();
        System.out.println("=== 添加自定义文档 ===");
        System.out.println("文档数量: " + count);

        assertTrue(count > 0);

        // 基于自定义文档提问
        String answer = ragService.query("Spring Boot有什么特点？");
        System.out.println("回答: " + answer);

        assertNotNull(answer);
    }

    /**
     * 测试7：检索功能测试
     */
    @Test
    void testSearch() {
        ragService.clearKnowledgeBase();
        ragService.loadSampleDocuments();

        // 直接检索
        var results = ragService.search(java.util.List.of(0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0), 3);

        System.out.println("=== 检索测试 ===");
        System.out.println("检索结果数量: " + results.size());
        for (var r : results) {
            System.out.println("相似度: " + r.score() + ", 内容: " + r.content().substring(0, Math.min(50, r.content().length())) + "...");
        }

        assertNotNull(results);
    }

    /**
     * 测试8：清空知识库
     */
    @Test
    void testClearKnowledgeBase() {
        ragService.loadSampleDocuments();
        int countBefore = ragService.getDocumentCount();

        ragService.clearKnowledgeBase();
        int countAfter = ragService.getDocumentCount();

        System.out.println("=== 清空知识库 ===");
        System.out.println("清空前: " + countBefore);
        System.out.println("清空后: " + countAfter);

        assertEquals(0, countAfter);
    }
}
