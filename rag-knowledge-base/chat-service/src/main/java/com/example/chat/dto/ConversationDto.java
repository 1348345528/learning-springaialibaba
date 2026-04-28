package com.example.chat.dto;

/**
 * DTO for conversation data.
 */
public record ConversationDto(
    String conversationId,
    String title,
    Long updatedAt
) {
}
