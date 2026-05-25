package com.example.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档统计工具 — 让 Agent 能查询知识库中有哪些文档、多少个分块。
 */
@Slf4j
public class DocStatsTool implements java.util.function.Function<DocStatsTool.Request, DocStatsTool.Response> {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocStatsTool(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Response apply(Request request) {
        try {
            // 获取所有文档名称
            List<String> docNames = webClient.get()
                    .uri("/api/doc/documents")
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<String>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .doOnError(e -> log.error("获取文档列表失败: {}", e.getMessage()))
                    .onErrorReturn(List.of())
                    .block();

            if (docNames == null || docNames.isEmpty()) {
                return new Response(0, List.of(), "知识库中暂无文档");
            }

            // 获取分块信息
            Map<String, Object> pageMap = webClient.get()
                    .uri("/api/doc/chunks?page=0&size=1")
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .onErrorReturn(Map.of())
                    .block();

            long totalChunks = 0;
            if (pageMap != null && pageMap.get("totalElements") instanceof Number n) {
                totalChunks = n.longValue();
            }

            StringBuilder summary = new StringBuilder();
            summary.append("知识库共有 ").append(docNames.size()).append(" 个文档");
            if (totalChunks > 0) {
                summary.append("，合计 ").append(totalChunks).append(" 个分块");
            }
            summary.append("：\n");
            for (int i = 0; i < docNames.size(); i++) {
                summary.append("  [").append(i + 1).append("] ").append(docNames.get(i)).append("\n");
            }

            return new Response(docNames.size(), docNames, summary.toString().trim());
        } catch (Exception e) {
            log.error("查询文档统计失败: {}", e.getMessage());
            return new Response(0, List.of(), "查询文档统计失败: " + e.getMessage());
        }
    }

    public record Request() {}

    public record Response(long documentCount, List<String> documentNames, String summary) {}
}
