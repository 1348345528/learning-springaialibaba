package com.example.chat.repository.jpa;

import com.example.chat.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository for ChatMessageEntity.
 */
@Repository
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * Find all messages for a conversation, ordered by creation time.
     */
    List<ChatMessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Delete all messages for a conversation.
     */
    void deleteByConversationId(Long conversationId);

    /**
     * Count messages for a conversation.
     */
    long countByConversationId(Long conversationId);
}
