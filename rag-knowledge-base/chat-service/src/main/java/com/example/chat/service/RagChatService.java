package com.example.chat.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.example.chat.dto.ChatRequest;
import com.example.chat.model.ChatMemoryMessage;
import com.example.chat.repository.MysqlChatMemoryRepository;
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
    private final MemorySaver memorySaver;
    private final ToolCallback ragRetrievalCallback;
    private final McpToolRegistryService mcpToolRegistry;
    private final MultiLevelChatMemory chatMemory;
    private final MysqlChatMemoryRepository mysqlRepository;
    private final AgentStateManager agentStateManager;

    public RagChatService(ChatModel chatModel,
                          MemorySaver memorySaver,
                          ToolCallback ragRetrievalCallback,
                          McpToolRegistryService mcpToolRegistry,
                          MultiLevelChatMemory chatMemory,
                          MysqlChatMemoryRepository mysqlRepository,
                          AgentStateManager agentStateManager) {
        this.chatModel = chatModel;
        this.memorySaver = memorySaver;
        this.ragRetrievalCallback = ragRetrievalCallback;
        this.mcpToolRegistry = mcpToolRegistry;
        this.chatMemory = chatMemory;
        this.mysqlRepository = mysqlRepository;
        this.agentStateManager = agentStateManager;
    }

    public Flux<ServerSentEvent<String>> chatStream(ChatRequest request) {
        String conversationId = request.getConversationId();

        // 1. 保存用户消息
        chatMemory.add(conversationId, ChatMemoryMessage.user(request.getMessage()));

        // 2. 组装 ToolCallback 列表
        List<ToolCallback> allTools = new ArrayList<>();
        allTools.add(ragRetrievalCallback);
        ToolCallback[] mcpTools = mcpToolRegistry.lookup(request.getToolNames());
        if (mcpTools.length > 0) {
            allTools.addAll(List.of(mcpTools));
        }

        // 3. 构建配置
        RunnableConfig config = RunnableConfig.builder()
                .threadId(conversationId)
                .build();

        // 4. 过期检测：Redis TTL 标记不存在 → 从 MySQL 拼历史上下文
        String userMessage = request.getMessage();
        if (!agentStateManager.isAlive(conversationId)) {
            String context = agentStateManager.buildContextPrefix(conversationId);
            if (context != null) {
                userMessage = context + userMessage;
            }
        }

        // 5. 构建 ReactAgent（MCP 工具动态变化，每次请求构建）
        ReactAgent agent = ReactAgent.builder()
                .name("rag_agent")
                .model(chatModel)
                .instruction(AGENT_INSTRUCTION)
                .tools(allTools.toArray(new ToolCallback[0]))
                .saver(memorySaver)
                .build();

        // 6. 流式执行
        StringBuilder fullContent = new StringBuilder();

        Flux<NodeOutput> streamFlux;
        try {
            streamFlux = agent.stream(userMessage, config);
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
                            // 7. 保存 AI 回复 + 续期 TTL
                            String response = fullContent.toString();
                            if (!response.isEmpty()) {
                                chatMemory.add(conversationId, ChatMemoryMessage.assistant(response));
                            }
                            agentStateManager.markAlive(conversationId);

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
}
