package com.example.chat.controller;

import com.example.chat.dto.ConversationDto;
import com.example.chat.dto.CreateConversationRequest;
import com.example.chat.dto.MessageDto;
import com.example.chat.entity.ChatMessageEntity;
import com.example.chat.entity.ConversationEntity;
import com.example.chat.hook.ChatHistorySyncHook;
import com.example.chat.repository.jpa.ChatMessageJpaRepository;
import com.example.chat.repository.jpa.ConversationJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for conversation management.
 * <p>
 * Reads/writes directly via JPA repositories. Redis state cleanup on delete
 * is delegated to {@link ChatHistorySyncHook#deleteState(String)}.
 */
@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ConversationJpaRepository conversationRepo;
    private final ChatMessageJpaRepository messageRepo;
    private final ChatHistorySyncHook chatHistorySyncHook;

    public ConversationController(ConversationJpaRepository conversationRepo,
                                  ChatMessageJpaRepository messageRepo,
                                  ChatHistorySyncHook chatHistorySyncHook) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.chatHistorySyncHook = chatHistorySyncHook;
    }

    /** Create a new conversation. */
    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(@RequestBody CreateConversationRequest request) {
        String conversationId = UUID.randomUUID().toString();
        String title = request.getTitle() != null ? request.getTitle() : "New Conversation";

        ConversationEntity entity = new ConversationEntity();
        entity.setConversationId(conversationId);
        entity.setTitle(title);
        conversationRepo.save(entity);

        return ResponseEntity.ok(new ConversationDto(conversationId, title, System.currentTimeMillis()));
    }

    /** Get all conversations. */
    @GetMapping
    public ResponseEntity<List<ConversationDto>> listConversations() {
        List<ConversationDto> dtos = conversationRepo.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(this::toConversationDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /** Get a single conversation. */
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(@PathVariable String conversationId) {
        return conversationRepo.findByConversationId(conversationId)
                .map(c -> ResponseEntity.ok(toConversationDto(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Get messages for a conversation. */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable String conversationId) {
        Optional<ConversationEntity> convOpt = conversationRepo.findByConversationId(conversationId);
        if (convOpt.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<MessageDto> dtos = messageRepo
                .findByConversationIdOrderByCreatedAtAsc(convOpt.get().getId())
                .stream()
                .map(entity -> new MessageDto(
                        entity.getRole(),
                        entity.getContent(),
                        entity.getCreatedAt() != null
                                ? entity.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
                                : System.currentTimeMillis()))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /** Update conversation title. */
    @PutMapping("/{conversationId}/title")
    public ResponseEntity<ConversationDto> updateTitle(
            @PathVariable String conversationId,
            @RequestBody String title) {
        return conversationRepo.findByConversationId(conversationId)
                .map(conv -> {
                    conv.setTitle(title);
                    conversationRepo.save(conv);
                    return ResponseEntity.ok(toConversationDto(conv));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Delete a conversation and all its messages (MySQL + Redis). */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String conversationId) {
        conversationRepo.findByConversationId(conversationId).ifPresent(conv -> {
            messageRepo.deleteByConversationId(conv.getId());
            conversationRepo.delete(conv);
        });
        // Also clean up Redis checkpoint + alive marker
        chatHistorySyncHook.deleteState(conversationId);
        return ResponseEntity.noContent().build();
    }

    private ConversationDto toConversationDto(ConversationEntity entity) {
        long timestamp = entity.getUpdatedAt() != null
                ? entity.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
                : System.currentTimeMillis();
        return new ConversationDto(entity.getConversationId(), entity.getTitle(), timestamp);
    }
}
