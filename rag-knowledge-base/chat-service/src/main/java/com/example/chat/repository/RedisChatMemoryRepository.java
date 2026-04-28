package com.example.chat.repository;

import com.example.chat.model.ChatMemoryMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Redis-based repository for ChatMemory (L1 Cache).
 * Provides fast in-memory storage for recent conversation messages.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisChatMemoryRepository {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Add a message to the conversation's memory in Redis.
     */
    public void addMessage(String conversationId, ChatMemoryMessage message) {
        String key = getKey(conversationId);
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        listOps.rightPush(key, message);
        redisTemplate.expire(key, DEFAULT_TTL);
        log.debug("Added message to Redis for conversation: {}", conversationId);
    }

    /**
     * Get all messages for a conversation from Redis.
     */
    @SuppressWarnings("unchecked")
    public List<ChatMemoryMessage> getMessages(String conversationId) {
        String key = getKey(conversationId);
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        List<Object> objects = listOps.range(key, 0, -1);

        if (objects == null || objects.isEmpty()) {
            return Collections.emptyList();
        }

        return objects.stream()
                .filter(obj -> obj instanceof ChatMemoryMessage)
                .map(obj -> (ChatMemoryMessage) obj)
                .toList();
    }

    /**
     * Get the most recent N messages for a conversation.
     */
    @SuppressWarnings("unchecked")
    public List<ChatMemoryMessage> getRecentMessages(String conversationId, int limit) {
        String key = getKey(conversationId);
        ListOperations<String, Object> listOps = redisTemplate.opsForList();

        // Get from the end of the list (most recent)
        List<Object> objects = listOps.range(key, -limit, -1);

        if (objects == null || objects.isEmpty()) {
            return Collections.emptyList();
        }

        return objects.stream()
                .filter(obj -> obj instanceof ChatMemoryMessage)
                .map(obj -> (ChatMemoryMessage) obj)
                .toList();
    }

    /**
     * Clear all messages for a conversation from Redis.
     */
    public void clearMessages(String conversationId) {
        String key = getKey(conversationId);
        redisTemplate.delete(key);
        log.debug("Cleared messages from Redis for conversation: {}", conversationId);
    }

    /**
     * Check if messages exist for a conversation in Redis.
     */
    public boolean exists(String conversationId) {
        String key = getKey(conversationId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Get the count of messages for a conversation in Redis.
     */
    public long getMessageCount(String conversationId) {
        String key = getKey(conversationId);
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        Long count = listOps.size(key);
        return count != null ? count : 0L;
    }

    /**
     * Populate the Redis cache with messages (used when loading from MySQL).
     */
    public void populateCache(String conversationId, List<ChatMemoryMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        String key = getKey(conversationId);
        ListOperations<String, Object> listOps = redisTemplate.opsForList();

        // Clear existing data first
        redisTemplate.delete(key);

        // Add all messages
        for (ChatMemoryMessage message : messages) {
            listOps.rightPush(key, message);
        }

        redisTemplate.expire(key, DEFAULT_TTL);
        log.debug("Populated Redis cache with {} messages for conversation: {}", messages.size(), conversationId);
    }

    private String getKey(String conversationId) {
        Objects.requireNonNull(conversationId, "Conversation ID must not be null");
        return KEY_PREFIX + conversationId;
    }
}
