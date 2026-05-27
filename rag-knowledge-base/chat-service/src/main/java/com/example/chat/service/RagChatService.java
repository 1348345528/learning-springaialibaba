package com.example.chat.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.store.stores.RedisStore;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson2.JSONObject;
import com.example.chat.dto.ChatRequest;
import com.example.chat.entity.ConversationEntity;
import com.example.chat.hook.ChatHistorySyncHook;
import com.example.chat.repository.jpa.ConversationJpaRepository;
import com.example.chat.service.ReportGenerationTool.ReportInfo;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class RagChatService {

    private static final String AGENT_INSTRUCTION = """
            You are a helpful AI assistant for the RAG Knowledge Base system.

            Available tools:
            - search_knowledge_base: search document chunks from the knowledge base
            - get_current_time: get current date and time
            - calculate: evaluate mathematical expressions
            - get_document_stats: list documents and chunk counts

            When you need factual information, use search_knowledge_base to retrieve relevant document chunks.
            Always base your answers on the retrieved context.
            If the retrieved context doesn't contain relevant information, say so honestly.

            Keep responses concise, accurate, and helpful.
            """;

    private final ChatModel chatModel;
    private final RedisSaver redisSaver;
    private final List<ToolCallback> builtinTools;
    private final McpToolRegistryService mcpToolRegistry;
    private final ChatHistorySyncHook chatHistorySyncHook;
    private final SummarizationHook summarizationHook;
    private final ConversationJpaRepository conversationRepo;
    private final ReportGenerationTool reportGenerationTool;

    public RagChatService(ChatModel chatModel,
                          RedisSaver redisSaver,
                          List<ToolCallback> builtinTools,
                          McpToolRegistryService mcpToolRegistry,
                          ChatHistorySyncHook chatHistorySyncHook,
                          SummarizationHook summarizationHook,
                          ConversationJpaRepository conversationRepo,
                          ReportGenerationTool reportGenerationTool) {
        this.chatModel = chatModel;
        this.redisSaver = redisSaver;
        this.builtinTools = builtinTools;
        this.mcpToolRegistry = mcpToolRegistry;
        this.chatHistorySyncHook = chatHistorySyncHook;
        this.summarizationHook = summarizationHook;
        this.conversationRepo = conversationRepo;
        this.reportGenerationTool = reportGenerationTool;
    }

    public Flux<ServerSentEvent<String>> chatStream(ChatRequest request) {
        String cid = request.getConversationId();
        final String conversationId = (cid != null && !cid.isBlank()) ? cid : UUID.randomUUID().toString();

        // 0. 确保 MySQL 里存在这个会话（前置创建）
        ensureConversationExists(conversationId);

        // 1. 组装 ToolCallback 列表
        List<ToolCallback> allTools = new ArrayList<>(builtinTools);
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
                .chatOptions(DashScopeChatOptions.builder().enableThinking(true).build())
                .saver(redisSaver)
                .build();

        // 4. 流式执行
        StringBuilder fullContent = new StringBuilder();

        // 设置当前会话 ID，供 ReportGenerationTool 使用
        reportGenerationTool.setCurrentConversationId(conversationId);

        Flux<NodeOutput> streamFlux;
        try {
            streamFlux = agent.stream(request.getMessage(), config);
        } catch (GraphRunnerException e) {
            log.error("Failed to start agent stream", e);
            reportGenerationTool.clearCurrentConversationId();
            return Flux.error(e);
        }

        Flux<ServerSentEvent<String>> eventFlux = streamFlux
                .flatMap(RagChatService::extractToken)
                .doOnNext(event -> {
                    if ("message".equals(event.event())) {
                        String data = event.data();
                        if (data != null) {
                            fullContent.append(data);
                        }
                    }
                });

        return eventFlux
                .concatWith(Flux.defer(() -> {
                    // 检查是否有报表生成
                    ReportInfo reportInfo = reportGenerationTool.pollReport(conversationId);
                    if (reportInfo != null) {
                        String json = String.format("{\"reportId\":%d,\"reportName\":\"%s\",\"url\":\"%s\"}",
                                reportInfo.reportId(),
                                reportInfo.reportName().replace("\"", "\\\""),
                                reportInfo.url());
                        return Flux.just(ServerSentEvent.builder(json).event("report").build());
                    }
                    return Flux.empty();
                }))
                .concatWith(Flux.just(ServerSentEvent.builder("[DONE]").build())
                        .doOnComplete(() -> {
                            // 清除 ThreadLocal
                            reportGenerationTool.clearCurrentConversationId();
                            // 5. 自动生成对话标题（首轮完成后）
                            autoTitle(conversationId, request.getMessage());
                        }));
    }

    /** 从 NodeOutput 中提取模型输出的 token，区分思考内容与回复内容。 */
    private static Flux<ServerSentEvent<String>> extractToken(NodeOutput nodeOutput) {
        if (nodeOutput instanceof StreamingOutput streamingOutput) {
            if (streamingOutput.getOutputType() == OutputType.AGENT_MODEL_STREAMING) {
                Object msg = streamingOutput.message();
                if (msg instanceof AssistantMessage assistantMsg) {
                    List<ServerSentEvent<String>> events = new ArrayList<>();

                    // 思考链路以 metadata 方式传递，key 为 "reasoningContent"
                    Map<String, Object> meta = assistantMsg.getMetadata();
                    if (meta != null && meta.containsKey("reasoningContent")) {
                        Object reasoning = meta.get("reasoningContent");
                        if (reasoning != null && !reasoning.toString().isEmpty()) {
                            log.debug("思考: {}", reasoning);
                            events.add(ServerSentEvent.builder(reasoning.toString())
                                    .event("reasoning")
                                    .build());
                        }
                    }

                    // 模型回复内容
                    String text = assistantMsg.getText();
                    if (text != null && !text.isEmpty()) {
                        events.add(ServerSentEvent.builder(text)
                                .event("message")
                                .build());
                    }

                    if (!events.isEmpty()) {
                        return Flux.fromIterable(events);
                    }
                }
            }
        }
        return Flux.empty();
    }

    /** 确保 MySQL 里存在这个会话，没有则创建。 */
    private void ensureConversationExists(String conversationId) {
        if (!conversationRepo.existsByConversationId(conversationId)) {
            ConversationEntity entity = new ConversationEntity();
            entity.setConversationId(conversationId);
            entity.setTitle("New Conversation");
            conversationRepo.saveAndFlush(entity);
        }
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
