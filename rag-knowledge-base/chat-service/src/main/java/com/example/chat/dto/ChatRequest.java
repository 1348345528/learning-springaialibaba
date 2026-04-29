package com.example.chat.dto;

/**
 * Request DTO for chat messages.
 */
public class ChatRequest {
    private String conversationId;
    private String message;
    private int topK = 5;
    private boolean stream = true;
    private String[] toolNames;

    // Default constructor
    public ChatRequest() {}

    // Getters and Setters
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public String[] getToolNames() {
        return toolNames;
    }

    public void setToolNames(String[] toolNames) {
        this.toolNames = toolNames;
    }
}
