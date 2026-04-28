package com.example.chat.repository;

import com.example.chat.model.ChatMemoryMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisChatMemoryRepository.
 * Tests the L1 cache layer operations.
 */
@ExtendWith(MockitoExtension.class)
class RedisChatMemoryRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    private RedisChatMemoryRepository repository;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        repository = new RedisChatMemoryRepository(redisTemplate);
    }

    @Test
    @DisplayName("addMessage should store message in Redis list")
    void addMessage_shouldStoreMessage() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        ChatMemoryMessage message = new ChatMemoryMessage("user", "Hello, world!");

        // When
        repository.addMessage(conversationId, message);

        // Then
        String expectedKey = "chat:memory:" + conversationId;
        verify(listOperations).rightPush(eq(expectedKey), any(ChatMemoryMessage.class));
        verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofHours(24)));
    }

    @Test
    @DisplayName("getMessages should return list of messages from Redis")
    void getMessages_shouldReturnMessages() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        String key = "chat:memory:" + conversationId;
        List<ChatMemoryMessage> expectedMessages = List.of(
            new ChatMemoryMessage("user", "Hello"),
            new ChatMemoryMessage("assistant", "Hi there!")
        );

        when(listOperations.range(key, 0, -1)).thenReturn(expectedMessages.stream().map(m -> (Object) m).toList());

        // When
        List<ChatMemoryMessage> result = repository.getMessages(conversationId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("user");
        assertThat(result.get(1).role()).isEqualTo("assistant");
    }

    @Test
    @DisplayName("getMessages should return empty list when no messages exist")
    void getMessages_shouldReturnEmptyList_whenNoMessages() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        String key = "chat:memory:" + conversationId;

        when(listOperations.range(key, 0, -1)).thenReturn(List.of());

        // When
        List<ChatMemoryMessage> result = repository.getMessages(conversationId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("clearMessages should delete all messages for conversation")
    void clearMessages_shouldDeleteAllMessages() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        String key = "chat:memory:" + conversationId;

        // When
        repository.clearMessages(conversationId);

        // Then
        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("exists should return true when conversation has messages")
    void exists_shouldReturnTrue_whenMessagesExist() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        String key = "chat:memory:" + conversationId;

        lenient().when(redisTemplate.hasKey(key)).thenReturn(true);

        // When
        boolean result = repository.exists(conversationId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("exists should return false when no messages exist")
    void exists_shouldReturnFalse_whenNoMessages() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        String key = "chat:memory:" + conversationId;

        lenient().when(redisTemplate.hasKey(key)).thenReturn(false);

        // When
        boolean result = repository.exists(conversationId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getMessageCount should return correct count")
    void getMessageCount_shouldReturnCorrectCount() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        String key = "chat:memory:" + conversationId;

        when(listOperations.size(key)).thenReturn(5L);

        // When
        long result = repository.getMessageCount(conversationId);

        // Then
        assertThat(result).isEqualTo(5L);
    }
}
