package com.agent.projects.rag;

import com.agent.model.ChatCompletionRequest;
import com.agent.model.Message;
import com.agent.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG（检索增强生成）服务。
 * 核心流程：文档加载 -> 切分 -> 向量化 -> 存储 -> 检索 -> 生成回答
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    /** `llmClientService`：LLM调用服务。 */
    private final LlmClientService llmClientService;

    /** `documentStore`：文档向量存储（内存实现）。 */
    private final DocumentStore documentStore = new DocumentStore();

    /**
     * 文档向量存储（内存实现）。
     */
    public static class DocumentStore {
        /** `documents`：文档列表，每条包含文本和对应的向量。 */
        private final List<DocumentChunk> documents = new ArrayList<>();

        /**
         * 添加文档块。
         */
        public void addChunk(DocumentChunk chunk) {
            documents.add(chunk);
        }

        /**
         * 清空文档。
         */
        public void clear() {
            documents.clear();
        }

        /**
         * 获取所有文档。
         */
        public List<DocumentChunk> getAll() {
            return new ArrayList<>(documents);
        }

        /**
         * 获取文档数量。
         */
        public int size() {
            return documents.size();
        }
    }

    /**
     * 文档块结构。
     *
     * @param content 文档文本内容
     * @param embedding 文档向量表示
     * @param metadata 元数据（如来源文件）
     */
    public record DocumentChunk(
            String content,
            List<Double> embedding,
            Map<String, Object> metadata
    ) {
    }

    /**
     * 搜索结果结构。
     *
     * @param content 相关文档内容
     * @param score 相似度分数
     * @param metadata 元数据
     */
    public record SearchResult(
            String content,
            double score,
            Map<String, Object> metadata
    ) {
    }

    /**
     * 添加文档到知识库。
     *
     * @param text 文档文本
     * @param metadata 元数据
     */
    public void addDocument(String text, Map<String, Object> metadata) {
        // 1. 文档切分（简单按段落切分）
        List<String> chunks = splitIntoChunks(text);

        // 2. 对每个chunk进行向量化并存储
        for (String chunk : chunks) {
            List<Double> embedding = getEmbedding(chunk);
            documentStore.addChunk(new DocumentChunk(chunk, embedding, metadata));
        }

        log.info("已添加文档，包含 {} 个chunk", chunks.size());
    }

    /**
     * 添加纯文本到知识库（无元数据）。
     */
    public void addDocument(String text) {
        addDocument(text, Map.of("source", "user-input"));
    }

    /**
     * 清空知识库。
     */
    public void clearKnowledgeBase() {
        documentStore.clear();
        log.info("知识库已清空");
    }

    /**
     * 获取知识库中的文档数量。
     */
    public int getDocumentCount() {
        return documentStore.size();
    }

    /**
     * 问答：检索相关文档并生成回答。
     *
     * @param question 用户问题
     * @return 回答结果
     */
    public String query(String question) {
        return query(question, 3);
    }

    /**
     * 问答（指定返回相关文档数量）。
     *
     * @param question 用户问题
     * @param topK 返回top K个相关文档
     * @return 回答结果
     */
    public String query(String question, int topK) {
        log.info("");
        log.info("=".repeat(60));
        log.info("RAG问答 - 问题: {}", question);
        log.info("=".repeat(60));

        // 1. 将问题向量化
        List<Double> questionEmbedding = getEmbedding(question);

        // 2. 相似度检索
        List<SearchResult> results = search(questionEmbedding, topK);

        log.info("找到 {} 个相关文档:", results.size());
        for (int i = 0; i < results.size(); i++) {
            log.info("  [{}] 相似度: {:.4f}", i + 1, results.get(i).score());
            log.info("      内容: {}", results.get(i).content().substring(0, Math.min(100, results.get(i).content().length())) + "...");
        }

        // 3. 构建增强Prompt
        String context = results.stream()
                .map(SearchResult::content)
                .collect(Collectors.joining("\n\n"));

        String enhancedPrompt = String.format("""
                你是一个问答助手。请根据以下参考文档回答用户问题。

                参考文档：
                %s

                用户问题：%s

                要求：
                1. 仅根据参考文档回答，不要编造信息
                2. 如果参考文档中没有相关信息，请说明"根据提供的信息无法回答"
                3. 回答要准确、简洁

                回答：
                """, context, question);

        // 4. 调用LLM生成回答
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(List.of(Message.user(enhancedPrompt)))
                .temperature(0.7)
                .build();

        String answer = llmClientService.chatCompletion(request)
                .getChoices().get(0).getMessage().getContent();

        log.info("最终回答: {}", answer);
        return answer;
    }

    /**
     * 相似度搜索。
     *
     * @param queryEmbedding 查询向量
     * @param topK 返回top K个结果
     * @return 搜索结果列表
     */
    public List<SearchResult> search(List<Double> queryEmbedding, int topK) {
        // 计算余弦相似度
        List<SearchResult> results = new ArrayList<>();

        for (DocumentChunk doc : documentStore.getAll()) {
            double similarity = cosineSimilarity(queryEmbedding, doc.embedding());
            results.add(new SearchResult(doc.content(), similarity, doc.metadata()));
        }

        // 按相似度排序，取topK
        return results.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 获取文本的向量表示（使用LLM的embedding接口）。
     * 注意：这里使用一个简化实现，通过LLM将文本转为"伪向量"用于演示。
     * 生产环境应使用专门的embedding模型（如text-embedding-3-small）。
     */
    private List<Double> getEmbedding(String text) {
        // 使用简化方法：将文本特征转为向量
        // 生产环境应调用专门的embedding API
        return textToEmbedding(text);
    }

    /**
     * 简化的文本转向量方法（用于演示）。
     * 实际生产应使用专门的embedding API。
     */
    private List<Double> textToEmbedding(String text) {
        // 使用文本哈希和特征提取生成伪向量
        // 这只是一个演示，生产环境请使用真正的embedding API
        List<Double> embedding = new ArrayList<>();
        String lowerText = text.toLowerCase();

        // 基于关键词的简单特征向量
        String[] keywords = {
                "java", "python", "编程", "代码", "开发", "ai", "agent", "llm",
                "模型", "函数", "工具", "搜索", "计算", "天气", "api", "服务"
        };

        for (String kw : keywords) {
            double value = lowerText.contains(kw) ? 1.0 : 0.0;
            embedding.add(value);
        }

        // 添加更多维度（基于文本长度和哈希）
        embedding.add((double) text.length() / 100.0);
        embedding.add((double) text.hashCode() % 100 / 100.0);

        // 归一化
        return normalize(embedding);
    }

    /**
     * 余弦相似度计算。
     */
    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 向量归一化。
     */
    private List<Double> normalize(List<Double> vector) {
        double sum = 0.0;
        for (Double v : vector) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            return vector;
        }
        return vector.stream().map(v -> v / norm).collect(Collectors.toList());
    }

    /**
     * 文档切分（简单实现）。
     * 按段落和固定长度切分。
     */
    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        // 按段落分割
        String[] paragraphs = text.split("\n");

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) {
                continue;
            }

            // 如果段落太长，进一步切分
            if (para.length() > 500) {
                // 按句子切分
                String[] sentences = para.split("[。！？.!?]");
                StringBuilder currentChunk = new StringBuilder();

                for (String sentence : sentences) {
                    if (currentChunk.length() + sentence.length() > 500) {
                        if (currentChunk.length() > 0) {
                            chunks.add(currentChunk.toString());
                        }
                        currentChunk = new StringBuilder(sentence);
                    } else {
                        if (currentChunk.length() > 0) {
                            currentChunk.append("。");
                        }
                        currentChunk.append(sentence);
                    }
                }

                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                }
            } else {
                chunks.add(para);
            }
        }

        return chunks;
    }

    /**
     * 预加载示例文档到知识库。
     */
    public void loadSampleDocuments() {
        // 添加Java相关文档
        addDocument("""
                Java是一种面向对象的编程语言，由Sun Microsystems于1995年发布。
                Java的特点是"一次编写，到处运行"，具有跨平台特性。
                Java广泛应用于企业级应用、Android开发和大规模系统构建。
                常用版本包括Java 8、11、17和21。
                """, Map.of("source", "java-guide", "topic", "java"));

        // 添加Python文档
        addDocument("""
                Python是一种解释型、高级、通用的编程语言。
                Python语法简洁优雅，易于学习，广泛应用于数据分析、机器学习、Web开发等领域。
                Python拥有丰富的第三方库，如NumPy、Pandas、TensorFlow等。
                Python有Python 2和Python 3两个主要版本，目前主流是Python 3。
                """, Map.of("source", "python-guide", "topic", "python"));

        // 添加AI Agent文档
        addDocument("""
                AI Agent（人工智能代理）是一种能够感知环境、做出决策并执行动作的智能系统。
                Agent的核心能力包括：感知、推理、行动和学习。
                常见的Agent架构包括ReAct（推理+行动）、Tool Agent（工具Agent）等。
                Agent可以通过调用外部工具来扩展能力，如搜索、计算、API调用等。
                """, Map.of("source", "agent-guide", "topic", "ai-agent"));

        // 添加RAG文档
        addDocument("""
                RAG（检索增强生成）是一种结合检索和生成的AI技术。
                RAG的核心思想是让LLM能够访问外部知识库，提高回答的准确性。
                RAG的工作流程：用户问题 -> 向量化 -> 检索知识库 -> 获取相关文档 -> 增强Prompt -> 生成回答。
                RAG可以解决LLM的幻觉问题和知识时效性问题。
                常见的RAG组件包括：向量数据库、Embedding模型、检索器等。
                """, Map.of("source", "rag-guide", "topic", "rag"));

        log.info("已加载示例文档，知识库共 {} 个文档", getDocumentCount());
    }
}
