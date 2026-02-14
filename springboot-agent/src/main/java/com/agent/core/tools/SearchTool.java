package com.agent.core.tools;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 网页搜索工具（示例数据）。
 */
@Slf4j
@Component
public class SearchTool implements BaseTool {

    /**
     * 返回工具名称。
     */
    @Override
    public String getName() {
        return "web_search";
    }

    /**
     * 返回工具描述。
     */
    @Override
    public String getDescription() {
        return "搜索互联网信息，适合查询实时信息或未知事实。";
    }

    /**
     * 定义搜索参数结构。
     */
    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of(
                                "type", "string",
                                "description", "搜索关键词"
                        ),
                        "num_results", Map.of(
                                "type", "integer",
                                "description", "返回结果数量"
                        )
                ),
                "required", List.of("query")
        );
    }

    /**
     * 执行搜索并返回 JSON 字符串结果。
     *
     * @param arguments 输入参数，读取 `query` 与可选 `num_results`
     * @return 模拟搜索结果
     */
    @Override
    public String run(JsonNode arguments) {
        // `query`：用户要检索的关键词
        String query = arguments.path("query").asText();
        // `numResults`：希望返回的结果条数，默认 3 条
        int numResults = arguments.has("num_results") ? arguments.path("num_results").asInt() : 3;

        Map<String, List<SearchResult>> mockDb = Map.of(
                "python", List.of(
                        new SearchResult("Python 官方网站", "python.org", "Python 是一种通用编程语言。"),
                        new SearchResult("Python 文档", "docs.python.org", "学习 Python 语法与标准库。")
                ),
                "langchain", List.of(
                        new SearchResult("LangChain 文档", "langchain.com", "LangChain 是大模型应用开发框架。"),
                        new SearchResult("LangChain GitHub", "github.com/langchain", "快速入门与示例代码。")
                ),
                "机器学习", List.of(
                        new SearchResult("机器学习入门", "ml-tutorial.com", "机器学习核心概念和案例。"),
                        new SearchResult("深度学习课程", "deeplearning.ai", "系统学习深度学习。")
                )
        );

        List<SearchResult> results = new ArrayList<>();
        String queryLower = query.toLowerCase();

        for (var entry : mockDb.entrySet()) {
            if (entry.getKey().toLowerCase().contains(queryLower)
                    || queryLower.contains(entry.getKey().toLowerCase())) {
                results.addAll(entry.getValue());
                break;
            }
        }

        if (results.isEmpty()) {
            results.add(new SearchResult("搜索结果: " + query, "example.com", "关于 " + query + " 的相关信息。"));
        }

        if (results.size() > numResults) {
            results = results.subList(0, numResults);
        }

        StringBuilder json = new StringBuilder();
        json.append("{\"query\": \"").append(query).append("\", \"results\": [");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("{\"title\": \"").append(results.get(i).title())
                    .append("\", \"url\": \"").append(results.get(i).url())
                    .append("\", \"snippet\": \"").append(results.get(i).snippet()).append("\"}");
        }
        json.append("]}");

        return json.toString();
    }

    /**
     * 搜索结果结构。
     *
     * @param title 标题
     * @param url 链接
     * @param snippet 摘要
     */
    private record SearchResult(String title, String url, String snippet) {
    }
}
