package com.example.chat.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceGraphAgentTest {

    @Test
    void topRouterHasOnlyReactAndInvoiceBranches() {
        assertThat(GraphAgentRoute.values()).containsExactly(GraphAgentRoute.REACT_AGENT, GraphAgentRoute.INVOICE_AGENT);
    }

    @Test
    void startsInvoiceFlowAndStoresPreviewInGraphState() throws Exception {
        InvoiceGraphAgent agent = testAgent((message, pendingSession) ->
                new GraphAgentRouteDecision(GraphAgentRoute.INVOICE_AGENT, InvoiceGraphAction.START, "P123456"));

        OverAllState state = agent.invoke("help me invoice policy P123456", config()).orElseThrow();

        assertThat(state.value("invoicePending", Boolean.class)).contains(true);
        assertThat(invoicePreview(state).policyNo()).isEqualTo("P123456");
        assertThat(lastAssistantText(state)).contains("Invoice preview is ready");
    }

    @Test
    void confirmsPendingInvoiceUsingTheSameGraphState() throws Exception {
        InvoiceGraphAgent agent = testAgent((message, pendingSession) -> pendingSession == null
                ? new GraphAgentRouteDecision(GraphAgentRoute.INVOICE_AGENT, InvoiceGraphAction.START, "P123456")
                : new GraphAgentRouteDecision(GraphAgentRoute.INVOICE_AGENT, InvoiceGraphAction.CONFIRM, pendingSession.preview().policyNo()));
        RunnableConfig config = config();

        OverAllState previewState = agent.invoke("invoice policy P123456", config).orElseThrow();
        OverAllState resultState = agent.getCompiledGraph().invoke(previewState.input(
                Map.of("messages", List.of(new UserMessage("confirm")))), config).orElseThrow();

        assertThat(resultState.value("invoicePending", Boolean.class)).contains(false);
        assertThat(invoiceResult(resultState).invoiceNo()).isEqualTo("INV-P123456");
        assertThat(lastAssistantText(resultState)).contains("Invoice generated");
    }

    @Test
    void rejectsPendingInvoiceUsingTheSameGraphState() throws Exception {
        InvoiceGraphAgent agent = testAgent((message, pendingSession) -> pendingSession == null
                ? new GraphAgentRouteDecision(GraphAgentRoute.INVOICE_AGENT, InvoiceGraphAction.START, "P123456")
                : new GraphAgentRouteDecision(GraphAgentRoute.INVOICE_AGENT, InvoiceGraphAction.REJECT, pendingSession.preview().policyNo()));
        RunnableConfig config = config();

        OverAllState previewState = agent.invoke("invoice policy P123456", config).orElseThrow();
        OverAllState resultState = agent.getCompiledGraph().invoke(previewState.input(
                Map.of("messages", List.of(new UserMessage("reject")))), config).orElseThrow();

        assertThat(resultState.value("invoicePending", Boolean.class)).contains(false);
        assertThat(lastAssistantText(resultState)).contains("Invoice flow cancelled");
    }

    private static InvoiceGraphAgent testAgent(GraphAgentIntentClassifier classifier) {
        AgentLlmNode llmNode = AgentLlmNode.builder()
                .agentName("test_invoice_graph_agent")
                .build();
        AgentToolNode toolNode = AgentToolNode.builder()
                .agentName("test_invoice_graph_agent")
                .toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false).build())
                .build();
        return new InvoiceGraphAgent(
                llmNode,
                toolNode,
                CompileConfig.builder().build(),
                ReactAgent.builder().name("test_invoice_graph_agent"),
                classifier,
                new InvoiceMockService(),
                List.of(),
                new SpringAIJacksonStateSerializer(OverAllState::new)
        );
    }

    private static RunnableConfig config() {
        return RunnableConfig.builder()
                .threadId("invoice-test-thread")
                .build();
    }

    private static String lastAssistantText(OverAllState state) {
        List<Message> messages = messages(state);
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof AssistantMessage assistantMessage) {
                return assistantMessage.getText();
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static InvoicePreview invoicePreview(OverAllState state) {
        Object value = state.value("invoicePreview").orElseThrow();
        if (value instanceof InvoicePreview preview) {
            return preview;
        }
        Map<String, Object> map = (Map<String, Object>) value;
        return new InvoicePreview(
                Objects.toString(map.get("policyNo"), ""),
                Objects.toString(map.get("policyHolder"), ""),
                Objects.toString(map.get("buyerName"), ""),
                Objects.toString(map.get("taxNo"), ""),
                new java.math.BigDecimal(Objects.toString(map.get("amount"), "0")),
                Objects.toString(map.get("itemName"), "")
        );
    }

    @SuppressWarnings("unchecked")
    private static InvoiceResult invoiceResult(OverAllState state) {
        Object value = state.value("invoiceResult").orElseThrow();
        if (value instanceof InvoiceResult result) {
            return result;
        }
        Map<String, Object> map = (Map<String, Object>) value;
        return new InvoiceResult(
                Objects.toString(map.get("invoiceNo"), ""),
                Objects.toString(map.get("policyNo"), ""),
                Objects.toString(map.get("status"), ""),
                Objects.toString(map.get("downloadUrl"), "")
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Message> messages(OverAllState state) {
        return state.value("messages")
                .filter(List.class::isInstance)
                .map(value -> (List<Message>) value)
                .orElse(List.of());
    }
}
