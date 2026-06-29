package com.example.chat.graph;

public interface GraphAgentIntentClassifier {

    GraphAgentRouteDecision classify(String message, InvoiceSession pendingSession);
}
