package com.example.chat.repository.jpa;

import com.example.chat.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for ConversationEntity.
 */
@Repository
public interface ConversationJpaRepository extends JpaRepository<ConversationEntity, Long> {

    /**
     * Find conversation by its unique conversation ID.
     */
    Optional<ConversationEntity> findByConversationId(String conversationId);

    /**
     * Find all conversations ordered by last update time (most recent first).
     */
    List<ConversationEntity> findAllByOrderByUpdatedAtDesc();

    /**
     * Check if conversation exists by conversation ID.
     */
    boolean existsByConversationId(String conversationId);
}
