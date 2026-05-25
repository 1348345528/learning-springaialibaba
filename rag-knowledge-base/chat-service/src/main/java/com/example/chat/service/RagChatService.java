package com.example.chat.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.example.chat.dto.ChatRequest;
import com.example.chat.hook.ChatHistorySyncHook;
import com.example.chat.repository.jpa.ConversationJpaRepository;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RagChatService {

    private static final String AGENT_INSTRUCTION = """
            You are a helpful AI assistant for the RAG Knowledge Base system.

            When you need factual information or background knowledge, use the search_knowledge_base tool to retrieve relevant document chunks.
            Always base your answers on the retrieved context.
            If the retrieved context doesn't contain relevant information, say so honestly.

            Keep responses concise, accurate, and helpful.
            """;

    private final ChatModel chatModel;
    private final RedisSaver redisSaver;
    private final ToolCallback ragRetrievalCallback;
    private final McpToolRegistryService mcpToolRegistry;
    private final ChatHistorySyncHook chatHistorySyncHook;
    private final SummarizationHook summarizationHook;
    private final ConversationJpaRepository conversationRepo;

    public RagChatService(ChatModel chatModel,
                          RedisSaver redisSaver,
                          ToolCallback ragRetrievalCallback,
                          McpToolRegistryService mcpToolRegistry,
                          ChatHistorySyncHook chatHistorySyncHook,
                          SummarizationHook summarizationHook,
                          ConversationJpaRepository conversationRepo) {
        this.chatModel = chatModel;
        this.redisSaver = redisSaver;
        this.ragRetrievalCallback = ragRetrievalCallback;
        this.mcpToolRegistry = mcpToolRegistry;
        this.chatHistorySyncHook = chatHistorySyncHook;
        this.summarizationHook = summarizationHook;
        this.conversationRepo = conversationRepo;
    }

    public Flux<ServerSentEvent<String>> chatStream(ChatRequest request) {
        String conversationId = request.getConversationId();

        // 1. 组装 ToolCallback 列表
        List<ToolCallback> allTools = new ArrayList<>();
        allTools.add(ragRetrievalCallback);
        ToolCallback[] mcpTools = mcpToolRegistry.lookup(request.getToolNames());
        if (mcpTools.length > 0) {
            allTools.addAll(List.of(mcpTools));
        }

        // 2. 构建配置
        RunnableConfig config = RunnableConfig.builder()
                .threadId(conversationId)
                .build();

        // 3. 构建 ReactAgent，注入 Hook
        //    - ChatHistorySyncHook: beforeAgent 恢复历史 / afterAgent 同步 MySQL + 续 TTL
        //    - SummarizationHook: beforeModel 自动摘要压缩超长上下文
        ReactAgent agent = ReactAgent.builder()
                .name("rag_agent")
                .model(chatModel)
                .instruction(AGENT_INSTRUCTION)
                .hooks(chatHistorySyncHook, summarizationHook)
                .tools(allTools.toArray(new ToolCallback[0]))
                .saver(redisSaver)
                .build();

        // 4. 流式执行
        StringBuilder fullContent = new StringBuilder();

        Flux<NodeOutput> streamFlux;
        try {
            streamFlux = agent.stream(request.getMessage(), config);
        } catch (GraphRunnerException e) {
            log.error("Failed to start agent stream", e);
            return Flux.error(e);
        }

        Flux<String> tokenFlux = streamFlux
                .flatMap(nodeOutput -> extractToken(nodeOutput))
                .doOnNext(token -> fullContent.append(token));

        return tokenFlux
                .map(token -> ServerSentEvent.builder(token).build())
                .concatWith(Flux.just(ServerSentEvent.builder("[DONE]").build())
                        .doOnComplete(() -> {
                            // 5. 自动生成对话标题（首轮完成后）
                            autoTitle(conversationId, request.getMessage());
                        }));
    }

    /** 从 NodeOutput 中提取模型生成的文本 token。 */
    private static Flux<String> extractToken(NodeOutput nodeOutput) {
        if (nodeOutput instanceof StreamingOutput streamingOutput) {
            if (streamingOutput.getOutputType() == OutputType.AGENT_MODEL_STREAMING) {
                Object msg = streamingOutput.message();
                if (msg instanceof AssistantMessage assistantMsg) {
                    String text = assistantMsg.getText();
                    if (text != null && !text.isEmpty()) {
                        return Flux.just(text);
                    }
                }
            }
        }
        return Flux.empty();
    }

    /** 首轮对话完成后，用第一条用户消息截取标题。 */
    private void autoTitle(String conversationId, String firstMessage) {
        try {
            conversationRepo.findByConversationId(conversationId).ifPresent(conv -> {
                // Only set title for fresh conversations (title still at default)
                if ("New Conversation".equals(conv.getTitle())) {
                    String title = firstMessage;
                    if (title != null && title.length() > 20) {
                        title = title.substring(0, 20) + "...";
                    }
                    conv.setTitle(title);
                    conversationRepo.save(conv);
                }
            });
        } catch (Exception e) {
            log.debug("Auto-title failed for {}: {}", conversationId, e.getMessage());
        }
    }
}
