package com.example.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * RAG 知识库检索工具 — 封装为 ToolCallback，让 ReactAgent 在 ReAct 循环中自主决定何时检索。
 * <p>
 * 输入：用户的检索 query 字符串
 * 输出：相关知识块文本（格式化后的上下文）
 */
@Slf4j
public class RagRetrievalTool implements java.util.function.Function<RagRetrievalTool.Request, RagRetrievalTool.Response> {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagRetrievalTool(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Response apply(Request request) {
        String query = request.query();
        int topK = request.topK() > 0 ? request.topK() : 5;

        List<RetrievalResult> chunks;
        try {
            chunks = retrieveChunks(query, topK).block();
        } catch (Exception e) {
            log.error("RAG retrieval failed for query '{}': {}", query, e.getMessage());
            return new Response("检索失败: " + e.getMessage(), List.of());
        }

        if (chunks == null || chunks.isEmpty()) {
            return new Response("未找到与 '" + query + "' 相关的知识库内容。", List.of());
        }

        String context = buildContext(chunks);
        return new Response(context, chunks);
    }

    private Mono<List<RetrievalResult>> retrieveChunks(String query, int topK) {
        try {
            Mono<List<Map<String, Object>>> responseMono = webClient.post()
                    .uri("/api/vector/search")
                    .bodyValue(Map.of("query", query, "topK", topK))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .timeout(java.time.Duration.ofSeconds(30))
                    .doOnError(e -> log.error("Failed to retrieve chunks: {}", e.getMessage()));

            return responseMono.map(list -> list.stream()
                    .map(this::toRetrievalResult)
                    .toList())
                    .onErrorReturn(List.of());
        } catch (Exception e) {
            log.error("Failed to retrieve chunks", e);
            return Mono.just(List.of());
        }
    }

    @SuppressWarnings("unchecked")
    private RetrievalResult toRetrievalResult(Map<String, Object> map) {
        Object contentObj = map.get("content");
        Object documentNameObj = map.get("documentName");
        Object scoreObj = map.get("score");

        String content = contentObj != null ? contentObj.toString() : "";
        String documentName = documentNameObj != null ? documentNameObj.toString() : "";
        float score = scoreObj != null ? Float.parseFloat(scoreObj.toString()) : 0.0f;

        return new RetrievalResult(content, documentName, score);
    }

    private String buildContext(List<RetrievalResult> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievalResult chunk = chunks.get(i);
            sb.append("[%d] %s (source: %s, score: %.2f)\n"
                    .formatted(i + 1, chunk.content(), chunk.documentName(), chunk.score()));
        }
        return sb.toString();
    }

    /**
     * Tool 输入参数
     */
    public record Request(String query, int topK) {
        public Request(String query) {
            this(query, 5);
        }
    }

    /**
     * Tool 返回结果
     */
    public record Response(String context, List<RetrievalResult> chunks) {}

    /**
     * 单条检索结果
     */
    public record RetrievalResult(String content, String documentName, float score) {}
}
