package com.example.chat.dto;

/**
 * Request DTO for creating a new conversation.
 */
public class CreateConversationRequest {
    private String title;

    // Default constructor
    public CreateConversationRequest() {
        this.title = "New Conversation";
    }

    // Constructor with title
    public CreateConversationRequest(String title) {
        this.title = title != null ? title : "New Conversation";
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
