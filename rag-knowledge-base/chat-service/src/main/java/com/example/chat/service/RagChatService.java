package com.example.chat.service;

import com.example.chat.dto.ChatRequest;
import com.example.chat.model.ChatMemoryMessage;
import com.example.chat.repository.MysqlChatMemoryRepository;
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

    private final ChatModel chatModel;
    private final org.springframework.web.reactive.function.client.WebClient webClient;
    private final MultiLevelChatMemory chatMemory;
    private final HistorySummaryService historySummaryService;
    private final MysqlChatMemoryRepository mysqlRepository;

    public Flux<ServerSentEvent<String>> chatStream(ChatRequest request) {
        String conversationId = request.getConversationId();

        // 1. 保存用户消息
        chatMemory.add(conversationId, ChatMemoryMessage.user(request.getMessage()));

        // 2. 从知识库检索相关知识块
        List<RetrievalResult> chunks = retrieveChunks(request.getMessage(), 5).block();
        String context = buildContext(chunks);

        // 3. 加载历史会话并压缩成摘要
        List<ChatMemoryMessage> recentHistory = chatMemory.getRecent(conversationId, 10);
        String historySummary = historySummaryService.summarize(recentHistory);

        // 4. 构建系统提示词（含历史摘要和 RAG 上下文）
        String systemPrompt = buildSystemPrompt(context, historySummary);

        // 5. 累积完整回复内容，用于流结束后保存
        StringBuilder fullContent = new StringBuilder();

        Flux<String> tokenFlux = ChatClient.builder(chatModel)
                .build()
                .prompt()
                .system(systemPrompt)
                .user(request.getMessage())
                .stream()
                .content()
                .doOnNext(token -> fullContent.append(token));

        // 6. 在流结束时添加 [DONE] 标记，并保存 AI 回复
        return tokenFlux
                .map(token -> ServerSentEvent.builder(token).build())
                .concatWith(Flux.just(ServerSentEvent.builder("[DONE]").build())
                        .doOnComplete(() -> {
                            // 保存 AI 回复
                            String response = fullContent.toString();
                            if (!response.isEmpty()) {
                                chatMemory.add(conversationId, ChatMemoryMessage.assistant(response));
                            }
                            // 首轮对话后自动生成标题（截取用户首条消息前20字）
                            long msgCount = chatMemory.getMessageCount(conversationId);
                            if (msgCount <= 2) {
                                String title = request.getMessage();
                                if (title.length() > 20) {
                                    title = title.substring(0, 20) + "...";
                                }
                                mysqlRepository.updateConversationTitle(conversationId, title);
                            }
                        }));
    }

    private String buildSystemPrompt(String context, String historySummary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a helpful AI assistant that answers questions based on the provided context from the knowledge base.\n");
        prompt.append("If the context doesn't contain relevant information, say you don't know based on the available information.\n");

        if (!historySummary.isEmpty()) {
            prompt.append("\nConversation history summary:\n").append(historySummary).append("\n");
        }

        prompt.append("\nContext from knowledge base:\n").append(context);
        return prompt.toString();
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
