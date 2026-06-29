package com.example.chat.graph;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LlmGraphAgentIntentClassifier implements GraphAgentIntentClassifier {

    private static final Pattern POLICY_PATTERN = Pattern.compile("(?i)(?:policyNo|policy|保单号?)\\s*[:：=]?\\s*([A-Za-z0-9_-]{3,})");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("(?i)route\\s*[:：=]\\s*([A-Z_]+)");
    private static final Pattern ACTION_PATTERN = Pattern.compile("(?i)action\\s*[:：=]\\s*([A-Z_]+)");

    private final ChatModel chatModel;

    public LlmGraphAgentIntentClassifier(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public GraphAgentRouteDecision classify(String message, InvoiceSession pendingSession) {
        String answer = callLlm(message, pendingSession);
        GraphAgentRoute route = parseRoute(answer, pendingSession);
        InvoiceGraphAction action = parseAction(answer, pendingSession);
        String policyNo = parsePolicyNo(answer);
        if (policyNo == null || policyNo.isBlank()) {
            policyNo = parsePolicyNo(message);
        }
        if ((policyNo == null || policyNo.isBlank()) && pendingSession != null) {
            policyNo = pendingSession.preview().policyNo();
        }
        return new GraphAgentRouteDecision(route, action, policyNo);
    }

    private String callLlm(String message, InvoiceSession pendingSession) {
        String pendingText = pendingSession == null
                ? "No invoice preview is waiting for confirmation."
                : "There is a pending invoice preview for policy " + pendingSession.preview().policyNo() + ".";
        String prompt = """
                You are the top router of a Spring AI Alibaba graph agent.
                Classify the user's latest message into one top-level route and one invoice action.
                Top-level route:
                - REACT_AGENT: normal chat, RAG, tools, report generation, or anything unrelated to invoice issuance.
                - INVOICE_AGENT: user is starting, confirming, or rejecting an insurance invoice flow.

                Invoice action when route=INVOICE_AGENT:
                - START: user wants to issue an invoice for an insurance policy.
                - CONFIRM: an invoice preview is pending and the user confirms generation.
                - REJECT: an invoice preview is pending and the user rejects, cancels, or says the preview is wrong.

                %s

                Return three lines only:
                route=<REACT_AGENT or INVOICE_AGENT>
                action=<START, CONFIRM, or REJECT; START if route=REACT_AGENT>
                policyNo=<policy number if present, otherwise empty>

                User message:
                %s
                """.formatted(pendingText, message);

        ChatResponse response = chatModel.call(new Prompt(prompt));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return response.getResult().getOutput().getText();
    }

    static GraphAgentRoute parseRoute(String answer, InvoiceSession pendingSession) {
        if (answer == null) {
            return GraphAgentRoute.REACT_AGENT;
        }
        Matcher matcher = ROUTE_PATTERN.matcher(answer);
        String routeText = matcher.find() ? matcher.group(1) : answer;
        String normalized = routeText.toUpperCase(Locale.ROOT);
        for (GraphAgentRoute route : GraphAgentRoute.values()) {
            if (normalized.contains(route.name())) {
                return route;
            }
        }
        if (normalized.contains("INVOICE")) {
            return GraphAgentRoute.INVOICE_AGENT;
        }
        return pendingSession == null ? GraphAgentRoute.REACT_AGENT : GraphAgentRoute.INVOICE_AGENT;
    }

    static InvoiceGraphAction parseAction(String answer, InvoiceSession pendingSession) {
        if (answer == null) {
            return pendingSession == null ? InvoiceGraphAction.START : InvoiceGraphAction.REJECT;
        }
        Matcher matcher = ACTION_PATTERN.matcher(answer);
        String actionText = matcher.find() ? matcher.group(1) : answer;
        String normalized = actionText.toUpperCase(Locale.ROOT);
        for (InvoiceGraphAction action : InvoiceGraphAction.values()) {
            if (normalized.contains(action.name())) {
                return action;
            }
        }
        if (normalized.contains("CONFIRM") || normalized.contains("APPROVE")) {
            return InvoiceGraphAction.CONFIRM;
        }
        if (normalized.contains("REJECT") || normalized.contains("CANCEL")) {
            return InvoiceGraphAction.REJECT;
        }
        return InvoiceGraphAction.START;
    }

    static String parsePolicyNo(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = POLICY_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
