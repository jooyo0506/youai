package com.agent.basics.api;

import com.agent.model.ChatCompletionRequest;
import com.agent.model.Message;
import com.agent.service.LlmClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 流式响应示例服务。
 * 作用：演示如何通过 SSE 持续接收模型增量输出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    /** `llmClientService`：提供模型地址、密钥等基础配置。 */
    private final LlmClientService llmClientService;

    /** `objectMapper`：用于解析每行 SSE 的 JSON 数据。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** `webClient`：发起流式 HTTP 请求的客户端。 */
    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    /**
     * 流式对话（回调版）：每收到一个增量片段就回调一次 `onChunk`。
     *
     * @param prompt 用户输入文本
     * @param onChunk 增量文本回调
     * @param onComplete 流结束回调
     * @param onError 异常回调
     */
    public void streamChat(String prompt, Consumer<String> onChunk, Runnable onComplete, Consumer<Exception> onError) {
        LlmClientService.ProviderConfig config = llmClientService.getProviderConfig();

        log.info("调用 DeepSeek API, model: {}, baseUrl: {}", config.model(), config.baseUrl());

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(config.model())
                .messages(List.of(Message.user(prompt)))
                .stream(true)
                .build();

        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            log.debug("请求体: {}", jsonBody);

            webClient.post()
                    .uri(config.baseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .doOnNext(line -> log.debug("原始SSE行: {}", line))
                    .subscribe(
                            line -> processSseLine(line, onChunk),
                            error -> {
                                log.error("流式调用失败", error);
                                if (onError != null) {
                                    onError.accept(new RuntimeException("流式请求失败", error));
                                }
                            },
                            () -> {
                                log.debug("流式调用完成");
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            }
                    );
        } catch (IOException e) {
            if (onError != null) {
                onError.accept(e);
            }
        }
    }

    /**
     * 解析单行 SSE 数据并提取增量文本。
     *
     * @param line SSE 单行原始文本
     * @param onChunk 提取到文本后的回调
     */
    private void processSseLine(String line, Consumer<String> onChunk) {
        if (line == null || line.isEmpty()) {
            return;
        }
        if (!line.startsWith("data: ")) {
            return;
        }

        // `data`：去掉 `data: ` 前缀后的负载内容
        String data = line.substring(6);
        if ("[DONE]".equals(data)) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(data);
            log.debug("收到流式数据: {}", root);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).path("delta");
                JsonNode content = delta.path("content");
                if (!content.isMissingNode() && !content.isNull()) {
                    log.debug("提取到内容: {}", content.asText());
                    onChunk.accept(content.asText());
                }
            }
        } catch (IOException e) {
            log.error("解析流式数据失败: {}", data, e);
        }
    }

    /**
     * 流式对话（SSE 发射器版）：适合直接透传给前端。
     *
     * @param prompt 用户输入文本
     * @return 可持续推送消息的 `SseEmitter`
     */
    public SseEmitter streamChatWithEmitter(String prompt) {
        SseEmitter emitter = new SseEmitter(60000L);

        streamChat(
                prompt,
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        log.error("发送 SSE 事件失败", e);
                        emitter.completeWithError(e);
                    }
                },
                emitter::complete,
                emitter::completeWithError
        );

        return emitter;
    }

    /**
     * 流式对话（异步聚合版）：异步返回完整文本。
     *
     * @param prompt 用户输入文本
     * @return 最终完整回复的 `CompletableFuture`
     */
    public CompletableFuture<String> streamChatAsync(String prompt) {
        CompletableFuture<String> future = new CompletableFuture<>();
        // `fullResponse`：累积每个 chunk，最终拼成完整回答
        StringBuilder fullResponse = new StringBuilder();

        streamChat(
                prompt,
                chunk -> {
                    System.out.print(chunk);
                    fullResponse.append(chunk);
                },
                () -> future.complete(fullResponse.toString()),
                future::completeExceptionally
        );

        return future;
    }

    /**
     * 指定系统提示词的流式对话。
     *
     * @param systemPrompt 系统角色提示词
     * @param userPrompt 用户提问
     * @param onChunk 增量回调
     * @param onComplete 完成回调
     */
    public void streamChatWithSystem(String systemPrompt, String userPrompt, Consumer<String> onChunk, Runnable onComplete) {
        LlmClientService.ProviderConfig config = llmClientService.getProviderConfig();

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(config.model())
                .messages(List.of(Message.system(systemPrompt), Message.user(userPrompt)))
                .stream(true)
                .build();

        try {
            String jsonBody = objectMapper.writeValueAsString(request);

            webClient.post()
                    .uri(config.baseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .subscribe(
                            line -> processSseLine(line, onChunk),
                            error -> log.error("流式调用失败", error),
                            () -> {
                                log.debug("流式调用完成");
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            }
                    );
        } catch (IOException e) {
            log.error("创建流式请求失败", e);
        }
    }
}
