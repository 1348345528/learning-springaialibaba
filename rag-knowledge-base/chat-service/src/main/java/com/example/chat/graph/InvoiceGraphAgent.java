package com.example.chat.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class InvoiceGraphAgent extends ReactAgent {

    private static final String INTENT_ROUTER = "intent_router";
    private static final String AGENT_MODEL = "_AGENT_MODEL_";
    private static final String AGENT_TOOL = "_AGENT_TOOL_";
    private static final String INVOICE_FLOW = "invoice_flow";

    private final AgentLlmNode llmNode;
    private final AgentToolNode toolNode;
    private final GraphAgentIntentClassifier intentClassifier;
    private final InvoiceMockService invoiceMockService;
    private final List<? extends Hook> hooks;
    private final StateSerializer stateSerializer;
    private final boolean hasTools;

    public InvoiceGraphAgent(AgentLlmNode llmNode,
                             AgentToolNode toolNode,
                             CompileConfig compileConfig,
                             Builder builder,
                             GraphAgentIntentClassifier intentClassifier,
                             InvoiceMockService invoiceMockService,
                             List<? extends Hook> hooks,
                             StateSerializer stateSerializer) {
        super(llmNode, toolNode, compileConfig, builder);
        this.llmNode = llmNode;
        this.toolNode = toolNode;
        this.intentClassifier = intentClassifier;
        this.invoiceMockService = invoiceMockService;
        this.hooks = hooks == null ? List.of() : hooks;
        this.stateSerializer = stateSerializer;
        this.hasTools = toolNode.getToolCallbacks() != null && !toolNode.getToolCallbacks().isEmpty();
    }

    @Override
    protected StateGraph initGraph() throws GraphStateException {
        List<Hook> activeHooks = new ArrayList<>(hooks);
        Set<String> hookNames = new HashSet<>();
        for (Hook hook : activeHooks) {
            if (!hookNames.add(Hook.getFullHookName(hook))) {
                throw new IllegalArgumentException("Duplicate hook instances found");
            }
            hook.setAgentName(this.name);
            hook.setAgent(this);
        }

        List<Hook> beforeAgentHooks = filterHooks(activeHooks, HookPosition.BEFORE_AGENT);
        List<Hook> afterAgentHooks = filterHooks(activeHooks, HookPosition.AFTER_AGENT);
        List<Hook> beforeModelHooks = filterHooks(activeHooks, HookPosition.BEFORE_MODEL);

        StateGraph graph = new StateGraph(this.name, buildKeyStrategyFactory(activeHooks), stateSerializer);
        graph.addNode(INTENT_ROUTER, AsyncNodeAction.node_async(this::routeIntent));
        graph.addNode(AGENT_MODEL, AsyncNodeActionWithConfig.node_async(this.llmNode));
        if (hasTools) {
            graph.addNode(AGENT_TOOL, AsyncNodeActionWithConfig.node_async(this.toolNode));
        }
        graph.addNode(INVOICE_FLOW, AsyncNodeAction.node_async(this::invoiceFlow));

        for (Hook hook : beforeAgentHooks) {
            if (hook instanceof MessagesAgentHook messagesAgentHook) {
                graph.addNode(Hook.getFullHookName(hook) + ".before", MessagesAgentHook.beforeAgentAction(messagesAgentHook));
            }
        }
        for (Hook hook : afterAgentHooks) {
            if (hook instanceof MessagesAgentHook messagesAgentHook) {
                graph.addNode(Hook.getFullHookName(hook) + ".after", MessagesAgentHook.afterAgentAction(messagesAgentHook));
            }
        }
        for (Hook hook : beforeModelHooks) {
            if (hook instanceof MessagesModelHook messagesModelHook) {
                graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", MessagesModelHook.beforeModelAction(messagesModelHook));
            }
        }

        String entryNode = beforeAgentHooks.isEmpty()
                ? INTENT_ROUTER
                : Hook.getFullHookName(beforeAgentHooks.get(0)) + ".before";
        String modelEntryNode = beforeModelHooks.isEmpty()
                ? AGENT_MODEL
                : Hook.getFullHookName(beforeModelHooks.get(0)) + ".beforeModel";
        String exitNode = afterAgentHooks.isEmpty()
                ? StateGraph.END
                : Hook.getFullHookName(afterAgentHooks.get(afterAgentHooks.size() - 1)) + ".after";

        graph.addEdge(StateGraph.START, entryNode);
        chainHook(graph, beforeAgentHooks, ".before", INTENT_ROUTER);
        chainHook(graph, beforeModelHooks, ".beforeModel", AGENT_MODEL);
        chainHookReverse(graph, afterAgentHooks, ".after", StateGraph.END);

        graph.addConditionalEdges(INTENT_ROUTER, AsyncEdgeAction.edge_async(this::routeAfterIntent), Map.of(
                modelEntryNode, modelEntryNode,
                INVOICE_FLOW, INVOICE_FLOW
        ));

        if (hasTools) {
            graph.addConditionalEdges(AGENT_MODEL, AsyncEdgeAction.edge_async(makeModelToTools(modelEntryNode, exitNode)),
                    Map.of(AGENT_TOOL, AGENT_TOOL, modelEntryNode, modelEntryNode, exitNode, exitNode));
            graph.addConditionalEdges(AGENT_TOOL, AsyncEdgeAction.edge_async(makeToolsToModelEdge(modelEntryNode)),
                    Map.of(modelEntryNode, modelEntryNode));
        } else {
            graph.addEdge(AGENT_MODEL, exitNode);
        }

        graph.addEdge(INVOICE_FLOW, exitNode);
        return graph;
    }

    private Map<String, Object> routeIntent(OverAllState state) {
        String message = latestUserText(state);
        InvoiceSession pendingSession = pendingSession(state);
        GraphAgentRouteDecision decision = intentClassifier.classify(message, pendingSession);
        log.debug("Invoice graph route decision: route={}, action={}, policyNo={}, pending={}, message={}",
                decision.route(), decision.action(), decision.policyNo(), pendingSession != null, message);
        Map<String, Object> result = new HashMap<>();
        result.put("route", decision.route());
        result.put("invoiceAction", decision.action());
        if (StringUtils.hasText(decision.policyNo())) {
            result.put("policyNo", decision.policyNo());
        } else if (pendingSession != null) {
            result.put("policyNo", pendingSession.preview().policyNo());
        }
        return result;
    }

    private String routeAfterIntent(OverAllState state) {
        GraphAgentRoute route = state.value("route", GraphAgentRoute.class).orElse(GraphAgentRoute.REACT_AGENT);
        return switch (route) {
            case INVOICE_AGENT -> INVOICE_FLOW;
            case REACT_AGENT -> {
                String beforeModelNode = firstHookNode(HookPosition.BEFORE_MODEL, ".beforeModel");
                yield beforeModelNode == null ? AGENT_MODEL : beforeModelNode;
            }
        };
    }

    private Map<String, Object> invoiceFlow(OverAllState state) {
        InvoiceGraphAction action = state.value("invoiceAction", InvoiceGraphAction.class).orElse(InvoiceGraphAction.START);
        log.debug("Invoice graph invoice_flow action={}, policyNo={}, pending={}",
                action, state.value("policyNo", ""), state.value("invoicePending", Boolean.class).orElse(false));
        return switch (action) {
            case CONFIRM -> generateInvoice(state);
            case REJECT -> rejectInvoice(state);
            case START -> startInvoice(state);
        };
    }

    private Map<String, Object> startInvoice(OverAllState state) {
        String policyNo = state.value("policyNo", "");
        InvoiceEligibility eligibility = invoiceMockService.checkInvoiceEligibility(policyNo);
        if (eligibility.eligible()) {
            return assembleInvoice(policyNo, eligibility);
        }
        return Map.of(
                "eligibility", eligibility,
                "invoicePending", false,
                "messages", List.of(new AssistantMessage("This policy cannot be invoiced: " + eligibility.reason()))
        );
    }

    private Map<String, Object> assembleInvoice(String policyNo, InvoiceEligibility eligibility) {
        InvoicePreview preview = invoiceMockService.assembleInvoicePreview(policyNo);
        return Map.of(
                "eligibility", eligibility,
                "invoicePending", true,
                "invoicePreview", preview,
                "messages", List.of(new AssistantMessage(previewMessage(preview)))
        );
    }

    private Map<String, Object> generateInvoice(OverAllState state) {
        InvoiceSession session = pendingSession(state);
        if (session == null) {
            return Map.of("messages", List.of(new AssistantMessage("No invoice preview is waiting for confirmation.")));
        }
        InvoiceResult result = invoiceMockService.generateInvoice(session.preview());
        return Map.of(
                "invoicePending", false,
                "invoiceResult", result,
                "messages", List.of(new AssistantMessage("Invoice generated: " + result.invoiceNo()))
        );
    }

    private Map<String, Object> rejectInvoice(OverAllState state) {
        return Map.of(
                "invoicePending", false,
                "messages", List.of(new AssistantMessage("Invoice flow cancelled."))
        );
    }

    private EdgeAction makeModelToTools(String modelDestination, String endDestination) {
        return state -> {
            List<Message> messages = messages(state);
            if (messages.isEmpty()) {
                return endDestination;
            }
            Message lastMessage = messages.get(messages.size() - 1);
            if (lastMessage instanceof AssistantMessage assistantMessage) {
                return assistantMessage.hasToolCalls() ? AGENT_TOOL : endDestination;
            }
            if (lastMessage instanceof ToolResponseMessage && messages.size() >= 2) {
                Message secondLastMessage = messages.get(messages.size() - 2);
                if (secondLastMessage instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
                    return modelDestination;
                }
            }
            return endDestination;
        };
    }

    private EdgeAction makeToolsToModelEdge(String modelDestination) {
        return state -> modelDestination;
    }

    private KeyStrategyFactory buildKeyStrategyFactory(List<Hook> activeHooks) {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("messages", new AppendStrategy());
            strategies.put("route", new ReplaceStrategy());
            strategies.put("invoiceAction", new ReplaceStrategy());
            strategies.put("policyNo", new ReplaceStrategy());
            strategies.put("eligibility", new ReplaceStrategy());
            strategies.put("invoicePending", new ReplaceStrategy());
            strategies.put("invoicePreview", new ReplaceStrategy());
            strategies.put("invoiceResult", new ReplaceStrategy());
            strategies.put("invoiceEvent", new ReplaceStrategy());
            if (StringUtils.hasLength(this.outputKey)) {
                strategies.put(this.outputKey, this.outputKeyStrategy == null ? new ReplaceStrategy() : this.outputKeyStrategy);
            }
            for (Hook hook : activeHooks) {
                Map<String, KeyStrategy> hookStrategies = hook.getKeyStrategys();
                if (hookStrategies != null && !hookStrategies.isEmpty()) {
                    strategies.putAll(hookStrategies);
                }
            }
            return strategies;
        };
    }

    private static List<Hook> filterHooks(List<? extends Hook> hooks, HookPosition position) {
        return hooks.stream()
                .filter(hook -> Arrays.asList(hook.getHookPositions()).contains(position))
                .sorted(Comparator.comparingInt(Hook::getOrder))
                .collect(Collectors.toList());
    }

    private static void chainHook(StateGraph graph, List<Hook> hooks, String suffix, String target) throws GraphStateException {
        if (hooks.isEmpty()) {
            return;
        }
        for (int i = 0; i < hooks.size() - 1; i++) {
            graph.addEdge(Hook.getFullHookName(hooks.get(i)) + suffix, Hook.getFullHookName(hooks.get(i + 1)) + suffix);
        }
        graph.addEdge(Hook.getFullHookName(hooks.get(hooks.size() - 1)) + suffix, target);
    }

    private static void chainHookReverse(StateGraph graph, List<Hook> hooks, String suffix, String target) throws GraphStateException {
        if (hooks.isEmpty()) {
            return;
        }
        for (int i = hooks.size() - 1; i > 0; i--) {
            graph.addEdge(Hook.getFullHookName(hooks.get(i)) + suffix, Hook.getFullHookName(hooks.get(i - 1)) + suffix);
        }
        graph.addEdge(Hook.getFullHookName(hooks.get(0)) + suffix, target);
    }

    private String firstHookNode(HookPosition position, String suffix) {
        return hooks.stream()
                .filter(hook -> Arrays.asList(hook.getHookPositions()).contains(position))
                .min(Comparator.comparingInt(Hook::getOrder))
                .map(hook -> Hook.getFullHookName(hook) + suffix)
                .orElse(null);
    }

    private static String latestUserText(OverAllState state) {
        List<Message> messages = messages(state);
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if ("USER".equalsIgnoreCase(message.getMessageType().name())) {
                return Objects.toString(message.getText(), "");
            }
        }
        return "";
    }

    private static InvoiceSession pendingSession(OverAllState state) {
        boolean pending = Boolean.TRUE.equals(state.value("invoicePending", Boolean.class).orElse(false));
        if (!pending) {
            return null;
        }
        InvoicePreview preview = invoicePreview(state);
        return preview == null ? null : new InvoiceSession(preview);
    }

    @SuppressWarnings("unchecked")
    private static InvoicePreview invoicePreview(OverAllState state) {
        Object value = state.value("invoicePreview").orElse(null);
        if (value instanceof InvoicePreview preview) {
            return preview;
        }
        if (!(value instanceof Map<?, ?> rawMap)) {
            return null;
        }
        Map<String, Object> map = (Map<String, Object>) rawMap;
        Object amount = map.get("amount");
        return new InvoicePreview(
                Objects.toString(map.get("policyNo"), ""),
                Objects.toString(map.get("policyHolder"), ""),
                Objects.toString(map.get("buyerName"), ""),
                Objects.toString(map.get("taxNo"), ""),
                amount instanceof BigDecimal decimal ? decimal : new BigDecimal(Objects.toString(amount, "0")),
                Objects.toString(map.get("itemName"), "")
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Message> messages(OverAllState state) {
        return state.value("messages")
                .filter(List.class::isInstance)
                .map(value -> (List<Message>) value)
                .orElse(List.of());
    }

    private static String previewMessage(InvoicePreview preview) {
        BigDecimal amount = preview.amount();
        return """
                Invoice preview is ready. Please reply with confirm or reject.
                Policy: %s
                Buyer: %s
                Tax No: %s
                Item: %s
                Amount: %s
                """.formatted(preview.policyNo(), preview.buyerName(), preview.taxNo(), preview.itemName(), amount);
    }
}
