package com.example.chat.graph;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.example.chat.hook.ChatHistorySyncHook;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceGraphAgentService {

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

    private final ChatClient chatClient;
    private final RedisSaver redisSaver;
    private final ChatHistorySyncHook chatHistorySyncHook;
    private final SummarizationHook summarizationHook;
    private final GraphAgentIntentClassifier intentClassifier;
    private final InvoiceMockService invoiceMockService;

    public InvoiceGraphAgentService(ChatClient chatClient,
                                    RedisSaver redisSaver,
                                    ChatHistorySyncHook chatHistorySyncHook,
                                    SummarizationHook summarizationHook,
                                    GraphAgentIntentClassifier intentClassifier,
                                    InvoiceMockService invoiceMockService) {
        this.chatClient = chatClient;
        this.redisSaver = redisSaver;
        this.chatHistorySyncHook = chatHistorySyncHook;
        this.summarizationHook = summarizationHook;
        this.intentClassifier = intentClassifier;
        this.invoiceMockService = invoiceMockService;
    }

    public InvoiceGraphAgent createAgent(List<ToolCallback> tools) {
        List<Hook> hooks = List.of(chatHistorySyncHook, summarizationHook);
        AgentLlmNode llmNode = AgentLlmNode.builder()
                .agentName("invoice_graph_agent")
                .chatClient(chatClient)
                .instruction(AGENT_INSTRUCTION)
                .chatOptions(DashScopeChatOptions.builder().enableThinking(true).build())
                .toolCallbacks(tools)
                .build();

        AgentToolNode toolNode = AgentToolNode.builder()
                .agentName("invoice_graph_agent")
                .toolCallbacks(tools)
                .toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build())
                .build();

        CompileConfig compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(redisSaver).build())
                .build();

        Builder builder = ReactAgent.builder()
                .name("invoice_graph_agent")
                .instruction(AGENT_INSTRUCTION)
                .hooks(hooks)
                .tools(tools)
                .chatOptions(DashScopeChatOptions.builder().enableThinking(true).build())
                .saver(redisSaver);

        return new InvoiceGraphAgent(
                llmNode,
                toolNode,
                compileConfig,
                builder,
                intentClassifier,
                invoiceMockService,
                hooks,
                new SpringAIJacksonStateSerializer(OverAllState::new)
        );
    }
}
