package com.example.chat.repository;

import com.example.chat.entity.ChatMessageEntity;
import com.example.chat.entity.ConversationEntity;
import com.example.chat.model.ChatMemoryMessage;
import com.example.chat.repository.jpa.ChatMessageJpaRepository;
import com.example.chat.repository.jpa.ConversationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MySQL-based repository for ChatMemory (L2 Persistence).
 * Provides durable persistent storage for conversation messages.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MysqlChatMemoryRepository {

    private final ConversationJpaRepository conversationRepository;
    private final ChatMessageJpaRepository messageRepository;

    /**
     * Create a new conversation.
     */
    public ConversationEntity createConversation(String conversationId, String title) {
        ConversationEntity entity = new ConversationEntity();
        entity.setConversationId(conversationId);
        entity.setTitle(title);
        return conversationRepository.save(entity);
    }

    /**
     * Get a conversation by ID.
     */
    public Optional<ConversationEntity> getConversation(String conversationId) {
        return conversationRepository.findByConversationId(conversationId);
    }

    /**
     * Save a message to the conversation.
     */
    public void saveMessage(String conversationId, ChatMemoryMessage message) {
        Optional<ConversationEntity> conversationOpt = conversationRepository.findByConversationId(conversationId);

        if (conversationOpt.isEmpty()) {
            log.warn("Conversation not found: {}", conversationId);
            return;
        }

        ConversationEntity conversation = conversationOpt.get();

        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setConversation(conversation);
        entity.setRole(message.role());
        entity.setContent(message.content());
        messageRepository.save(entity);

        // Update conversation's updatedAt timestamp
        conversationRepository.save(conversation);
        log.debug("Saved message to conversation: {}", conversationId);
    }

    /**
     * Get all messages for a conversation.
     */
    public List<ChatMemoryMessage> getMessages(String conversationId) {
        Optional<ConversationEntity> conversationOpt = conversationRepository.findByConversationId(conversationId);
        if (conversationOpt.isEmpty()) {
            return List.of();
        }

        List<ChatMessageEntity> entities = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationOpt.get().getId());
        return entities.stream()
            .map(this::toChatMemoryMessage)
            .toList();
    }

    /**
     * Delete a conversation and all its messages.
     */
    public void deleteConversation(String conversationId) {
        conversationRepository.findByConversationId(conversationId)
            .ifPresent(conversation -> {
                messageRepository.deleteByConversationId(conversation.getId());
                conversationRepository.delete(conversation);
                log.debug("Deleted conversation: {}", conversationId);
            });
    }

    /**
     * List all conversations.
     */
    public List<ConversationEntity> listConversations() {
        return conversationRepository.findAllByOrderByUpdatedAtDesc();
    }

    /**
     * Update conversation title.
     */
    public Optional<ConversationEntity> updateConversationTitle(String conversationId, String newTitle) {
        return conversationRepository.findByConversationId(conversationId)
            .map(conversation -> {
                conversation.setTitle(newTitle);
                return conversationRepository.save(conversation);
            });
    }

    /**
     * Check if conversation exists.
     */
    public boolean exists(String conversationId) {
        return conversationRepository.existsByConversationId(conversationId);
    }

    /**
     * Get message count for a conversation.
     */
    public long getMessageCount(String conversationId) {
        Optional<ConversationEntity> conversationOpt = conversationRepository.findByConversationId(conversationId);
        if (conversationOpt.isEmpty()) {
            return 0L;
        }
        return messageRepository.countByConversationId(conversationOpt.get().getId());
    }

    private ChatMemoryMessage toChatMemoryMessage(ChatMessageEntity entity) {
        return new ChatMemoryMessage(entity.getRole(), entity.getContent());
    }
}
