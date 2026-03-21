package com.example.chat.service;

import com.example.chat.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatService {

    private final ChatModel miniMaxChatModel;
    private final org.springframework.web.reactive.function.client.WebClient webClient;

    public Flux<ServerSentEvent<String>> chatStream(ChatRequest request) {
        // 暂时跳过 RAG retrieval，直接调用 AI（用于测试中文问题）
        String context = "You are a helpful AI assistant.";

        // Build system prompt with context
        String systemPrompt = """
            You are a helpful AI assistant.

            Context:
            %s
            """.formatted(context);

        // Stream response with proper SSE format
        Flux<String> tokenFlux = ChatClient.builder(miniMaxChatModel)
                .build()
                .prompt()
                .system(systemPrompt)
                .user(request.getMessage())
                .stream()
                .content();

        // 在流结束时添加 [DONE] 标记
        // 注意：不要在每个token后添加空格，这会破坏包含换行符的格式
        // 空格应该由前端根据上下文自行处理
        return tokenFlux
                .map(token -> ServerSentEvent.builder(token).build())
                .concatWith(Flux.just(ServerSentEvent.builder("[DONE]").build()));
    }

    private Mono<List<RetrievalResult>> retrieveChunks(String query, int topK) {
        try {
            @SuppressWarnings("unchecked")
            Mono<List<Map<String, Object>>> responseMono = webClient.post()
                    .uri("/api/vector/search")
                    .bodyValue(Map.of("query", query, "topK", topK))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .doOnError(e -> log.error("Failed to retrieve chunks from RAG service: {}", e.getMessage()));

            return responseMono.map(list -> {
                return list.stream()
                        .map(this::toRetrievalResult)
                        .toList();
            }).onErrorReturn(List.of());
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
        if (chunks.isEmpty()) {
            return "No relevant information found in the knowledge base.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievalResult chunk = chunks.get(i);
            sb.append("[%d] %s (source: %s, score: %.2f)\n"
                    .formatted(i + 1, chunk.content(), chunk.documentName(), chunk.score()));
        }
        return sb.toString();
    }

    record RetrievalResult(String content, String documentName, float score) {}
}
