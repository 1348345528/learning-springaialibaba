package com.example.chat.service;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.example.chat.dto.ChatRequest;
import com.example.chat.entity.ConversationEntity;
import com.example.chat.graph.GraphAgentRoute;
import com.example.chat.graph.InvoiceGraphAgent;
import com.example.chat.graph.InvoiceGraphAgentService;
import com.example.chat.repository.jpa.ConversationJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class GraphAgentChatService {

    private final InvoiceGraphAgentService invoiceGraphAgentService;
    private final List<ToolCallback> builtinTools;
    private final McpToolRegistryService mcpToolRegistry;
    private final ConversationJpaRepository conversationRepo;
    private final ReportGenerationTool reportGenerationTool;
    private final ObjectMapper objectMapper;

    public GraphAgentChatService(InvoiceGraphAgentService invoiceGraphAgentService,
                                 List<ToolCallback> builtinTools,
                                 McpToolRegistryService mcpToolRegistry,
                                 ConversationJpaRepository conversationRepo,
                                 ReportGenerationTool reportGenerationTool,
                                 ObjectMapper objectMapper) {
        this.invoiceGraphAgentService = invoiceGraphAgentService;
        this.builtinTools = builtinTools;
        this.mcpToolRegistry = mcpToolRegistry;
        this.conversationRepo = conversationRepo;
        this.reportGenerationTool = reportGenerationTool;
        this.objectMapper = objectMapper;
    }

    public Flux<ServerSentEvent<String>> chatStream(ChatRequest request) {
        return Flux.defer(() -> {
            String conversationId = ensureConversationId(request);
            ensureConversationExists(conversationId);

            List<ToolCallback> allTools = new ArrayList<>(builtinTools);
            ToolCallback[] mcpTools = mcpToolRegistry.lookup(request.getToolNames());
            if (mcpTools.length > 0) {
                allTools.addAll(List.of(mcpTools));
            }

            InvoiceGraphAgent agent = invoiceGraphAgentService.createAgent(allTools);
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(conversationId)
                    .build();

            Flux<NodeOutput> stream;
            try {
                stream = agent.stream(request.getMessage(), config);
            } catch (GraphRunnerException e) {
                log.error("Failed to start graph agent stream", e);
                return Flux.error(e);
            }

            return stream
                    .flatMap(this::extractGraphEvent)
                    .concatWith(Flux.defer(() -> {
                        List<ServerSentEvent<String>> tailEvents = new ArrayList<>();
                        ReportGenerationTool.ReportInfo reportInfo = reportGenerationTool.pollReport();
                        if (reportInfo != null) {
                            tailEvents.add(ServerSentEvent.builder(toJson(reportInfo)).event("report").build());
                        }
                        autoTitle(conversationId, request.getMessage());
                        tailEvents.add(ServerSentEvent.builder("[DONE]").build());
                        return Flux.fromIterable(tailEvents);
                    }));
        });
    }

    private Flux<ServerSentEvent<String>> extractGraphEvent(NodeOutput nodeOutput) {
        if (nodeOutput instanceof StreamingOutput streamingOutput) {
            return extractStreamingToken(streamingOutput);
        }

        String node = nodeOutput.node();
        log.debug("Graph agent node output: node={}, route={}, invoicePending={}, hasPreview={}, hasResult={}, stateKeys={}",
                node,
                nodeOutput.state().value("route").orElse(null),
                nodeOutput.state().value("invoicePending").orElse(null),
                nodeOutput.state().value("invoicePreview").isPresent(),
                nodeOutput.state().value("invoiceResult").isPresent(),
                nodeOutput.state().data().keySet());
        if (!"invoice_flow".equals(node) && !nodeOutput.isEND()) {
            return Flux.empty();
        }
        if (!isInvoiceState(nodeOutput, node)) {
            return Flux.empty();
        }

        List<ServerSentEvent<String>> events = new ArrayList<>();
        lastAssistantMessage(nodeOutput).ifPresent(text ->
                events.add(ServerSentEvent.builder(text).event("message").build()));
        boolean invoicePending = nodeOutput.state().value("invoicePending", Boolean.class).orElse(false);
        if (invoicePending) {
            nodeOutput.state().value("invoicePreview").ifPresent(preview ->
                    events.add(ServerSentEvent.builder(toJson(preview)).event("invoice_preview").build()));
        }
        nodeOutput.state().value("invoiceResult").ifPresent(result ->
                events.add(ServerSentEvent.builder(toJson(result)).event("invoice_result").build()));
        log.debug("Graph agent invoice SSE events from node {}: {}", node, events.size());
        return Flux.fromIterable(events);
    }

    private Flux<ServerSentEvent<String>> extractStreamingToken(StreamingOutput streamingOutput) {
        if (streamingOutput.getOutputType() != OutputType.AGENT_MODEL_STREAMING) {
            return Flux.empty();
        }
        Object msg = streamingOutput.message();
        if (!(msg instanceof AssistantMessage assistantMsg)) {
            return Flux.empty();
        }

        List<ServerSentEvent<String>> events = new ArrayList<>();
        Object reasoning = assistantMsg.getMetadata() == null ? null : assistantMsg.getMetadata().get("reasoningContent");
        if (reasoning != null && !reasoning.toString().isEmpty()) {
            events.add(ServerSentEvent.builder(reasoning.toString()).event("reasoning").build());
        }
        String text = assistantMsg.getText();
        if (text != null && !text.isEmpty()) {
            events.add(ServerSentEvent.builder(text).event("message").build());
        }
        return Flux.fromIterable(events);
    }

    private static boolean isInvoiceState(NodeOutput nodeOutput, String node) {
        Object route = nodeOutput.state().value("route").orElse(null);
        if (route == GraphAgentRoute.INVOICE_AGENT || String.valueOf(route).contains("INVOICE_AGENT")) {
            return true;
        }
        return "invoice_flow".equals(node);
    }

    private static java.util.Optional<String> lastAssistantMessage(NodeOutput nodeOutput) {
        List<Message> messages = messages(nodeOutput);
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof AssistantMessage assistantMessage) {
                return java.util.Optional.ofNullable(assistantMessage.getText());
            }
        }
        return java.util.Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static List<Message> messages(NodeOutput nodeOutput) {
        return nodeOutput.state().value("messages")
                .filter(List.class::isInstance)
                .map(value -> (List<Message>) value)
                .orElse(List.of());
    }

    private String ensureConversationId(ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
            request.setConversationId(conversationId);
        }
        return conversationId;
    }

    private void ensureConversationExists(String conversationId) {
        if (!conversationRepo.existsByConversationId(conversationId)) {
            ConversationEntity entity = new ConversationEntity();
            entity.setConversationId(conversationId);
            entity.setTitle("New Conversation");
            conversationRepo.saveAndFlush(entity);
        }
    }

    private void autoTitle(String conversationId, String firstMessage) {
        try {
            conversationRepo.findByConversationId(conversationId).ifPresent(conv -> {
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
            log.debug("Graph agent auto-title failed for {}: {}", conversationId, e.getMessage());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize graph agent SSE payload", e);
        }
    }
}
