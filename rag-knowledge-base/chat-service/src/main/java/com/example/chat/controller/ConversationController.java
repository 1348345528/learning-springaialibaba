package com.example.chat.controller;

import com.example.chat.dto.ConversationDto;
import com.example.chat.dto.CreateConversationRequest;
import com.example.chat.dto.MessageDto;
import com.example.chat.entity.ConversationEntity;
import com.example.chat.model.ChatMemoryMessage;
import com.example.chat.service.MultiLevelChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for conversation management.
 */
@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    private final MultiLevelChatMemory chatMemory;

    public ConversationController(MultiLevelChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    /**
     * Create a new conversation.
     */
    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(@RequestBody CreateConversationRequest request) {
        String conversationId = UUID.randomUUID().toString();
        String title = request.getTitle() != null ? request.getTitle() : "New Conversation";

        // Create conversation in both L1 and L2
        chatMemory.createConversation(conversationId, title);

        ConversationDto dto = new ConversationDto(
            conversationId,
            title,
            System.currentTimeMillis()
        );

        return ResponseEntity.ok(dto);
    }

    /**
     * Get all conversations.
     */
    @GetMapping
    public ResponseEntity<List<ConversationDto>> listConversations() {
        List<ConversationEntity> conversations = chatMemory.listConversations();
        List<ConversationDto> dtos = conversations.stream()
            .map(this::toConversationDto)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a single conversation.
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(@PathVariable String conversationId) {
        Optional<ConversationEntity> conversation = chatMemory.getConversation(conversationId);
        return conversation
            .map(c -> ResponseEntity.ok(toConversationDto(c)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get messages for a conversation.
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable String conversationId) {
        List<ChatMemoryMessage> messages = chatMemory.get(conversationId);
        List<MessageDto> dtos = messages.stream()
            .map(msg -> new MessageDto(msg.role(), msg.content(), System.currentTimeMillis()))
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Update conversation title.
     */
    @PutMapping("/{conversationId}/title")
    public ResponseEntity<ConversationDto> updateTitle(
            @PathVariable String conversationId,
            @RequestBody String title
    ) {
        Optional<ConversationEntity> updated = chatMemory.updateConversationTitle(conversationId, title);
        return updated
            .map(c -> ResponseEntity.ok(toConversationDto(c)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a conversation.
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String conversationId) {
        chatMemory.deleteConversation(conversationId);
        return ResponseEntity.noContent().build();
    }

    private ConversationDto toConversationDto(ConversationEntity entity) {
        long timestamp = entity.getUpdatedAt() != null
            ? entity.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
            : System.currentTimeMillis();
        return new ConversationDto(
            entity.getConversationId(),
            entity.getTitle(),
            timestamp
        );
    }
}
