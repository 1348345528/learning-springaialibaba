package com.example.chat.service;

import com.example.chat.entity.ConversationEntity;
import com.example.chat.model.ChatMemoryMessage;
import com.example.chat.repository.MysqlChatMemoryRepository;
import com.example.chat.repository.RedisChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Multi-level ChatMemory implementation.
 * Combines L1 (Redis) cache with L2 (MySQL) persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiLevelChatMemory {

    private final RedisChatMemoryRepository redisRepository;
    private final MysqlChatMemoryRepository mysqlRepository;
    private final ChatMemorySyncService syncService;

    /**
     * Add a message to the conversation memory.
     * Stores in L1 (Redis) and triggers async sync to L2 (MySQL).
     */
    public void add(String conversationId, ChatMemoryMessage message) {
        log.debug("Adding message to conversation: {}", conversationId);
        redisRepository.addMessage(conversationId, message);
        syncService.asyncSyncToMysql(conversationId, message);
    }

    /**
     * Get messages for a conversation.
     * Tries L1 (Redis) first, falls back to L2 (MySQL) on cache miss.
     */
    public List<ChatMemoryMessage> get(String conversationId) {
        // Check L1 cache first
        if (redisRepository.exists(conversationId)) {
            log.debug("L1 cache hit for conversation: {}", conversationId);
            return redisRepository.getMessages(conversationId);
        }

        // L1 cache miss, load from L2
        log.debug("L1 cache miss for conversation: {}, loading from L2", conversationId);
        List<ChatMemoryMessage> messages = mysqlRepository.getMessages(conversationId);

        // Populate L1 cache for future access
        if (!messages.isEmpty()) {
            redisRepository.populateCache(conversationId, messages);
        }

        return messages;
    }

    /**
     * Get the most recent N messages for a conversation.
     */
    public List<ChatMemoryMessage> getRecent(String conversationId, int limit) {
        // Check L1 cache first
        if (redisRepository.exists(conversationId)) {
            return redisRepository.getRecentMessages(conversationId, limit);
        }

        // L1 cache miss, load from L2
        List<ChatMemoryMessage> messages = mysqlRepository.getMessages(conversationId);
        int size = messages.size();
        int fromIndex = Math.max(0, size - limit);
        List<ChatMemoryMessage> recentMessages = messages.subList(fromIndex, size);

        // Populate L1 cache
        redisRepository.populateCache(conversationId, recentMessages);

        return recentMessages;
    }

    /**
     * Clear all messages for a conversation.
     * Removes from both L1 and L2.
     */
    public void clear(String conversationId) {
        log.debug("Clearing conversation: {}", conversationId);
        redisRepository.clearMessages(conversationId);
        mysqlRepository.deleteConversation(conversationId);
    }

    /**
     * Check if a conversation exists.
     */
    public boolean exists(String conversationId) {
        return redisRepository.exists(conversationId) || mysqlRepository.exists(conversationId);
    }

    /**
     * Get the total message count for a conversation.
     */
    public long getMessageCount(String conversationId) {
        // Check L1 first
        if (redisRepository.exists(conversationId)) {
            return redisRepository.getMessageCount(conversationId);
        }

        // Fallback to L2
        return mysqlRepository.getMessageCount(conversationId);
    }

    /**
     * Create a new conversation.
     */
    public void createConversation(String conversationId, String title) {
        mysqlRepository.createConversation(conversationId, title);
    }

    /**
     * List all conversations.
     */
    public List<ConversationEntity> listConversations() {
        return mysqlRepository.listConversations();
    }

    /**
     * Update conversation title.
     */
    public Optional<ConversationEntity> updateConversationTitle(String conversationId, String title) {
        return mysqlRepository.updateConversationTitle(conversationId, title);
    }

    /**
     * Delete a conversation.
     */
    public void deleteConversation(String conversationId) {
        clear(conversationId);
    }

    /**
     * Get a conversation by ID.
     */
    public Optional<ConversationEntity> getConversation(String conversationId) {
        return mysqlRepository.getConversation(conversationId);
    }
}
