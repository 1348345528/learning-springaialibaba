package com.example.chat.service;

import com.example.chat.entity.ConversationEntity;
import com.example.chat.model.ChatMemoryMessage;
import com.example.chat.repository.MysqlChatMemoryRepository;
import com.example.chat.repository.RedisChatMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MultiLevelChatMemory.
 * Tests the L1 (Redis) + L2 (MySQL) multi-level cache logic.
 */
@ExtendWith(MockitoExtension.class)
class MultiLevelChatMemoryTest {

    @Mock
    private RedisChatMemoryRepository redisRepository;

    @Mock
    private MysqlChatMemoryRepository mysqlRepository;

    @Mock
    private ChatMemorySyncService syncService;

    private MultiLevelChatMemory chatMemory;

    @BeforeEach
    void setUp() {
        chatMemory = new MultiLevelChatMemory(redisRepository, mysqlRepository, syncService);
    }

    @Test
    @DisplayName("add should store message in L1 cache and trigger async sync to L2")
    void add_shouldStoreInL1AndTriggerAsyncSync() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        ChatMemoryMessage message = new ChatMemoryMessage("user", "Hello!");

        // When
        chatMemory.add(conversationId, message);

        // Then
        verify(redisRepository).addMessage(conversationId, message);
        verify(syncService).asyncSyncToMysql(conversationId, message);
    }

    @Test
    @DisplayName("get should return messages from L1 cache when available")
    void get_shouldReturnFromL1Cache_whenAvailable() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        List<ChatMemoryMessage> l1Messages = List.of(
            new ChatMemoryMessage("user", "Hello"),
            new ChatMemoryMessage("assistant", "Hi!")
        );

        when(redisRepository.exists(conversationId)).thenReturn(true);
        when(redisRepository.getMessages(conversationId)).thenReturn(l1Messages);

        // When
        List<ChatMemoryMessage> result = chatMemory.get(conversationId);

        // Then
        assertThat(result).isEqualTo(l1Messages);
        verify(redisRepository).getMessages(conversationId);
        verify(mysqlRepository, never()).getMessages(anyString());
    }

    @Test
    @DisplayName("get should fallback to L2 when L1 cache miss")
    void get_shouldFallbackToL2_whenL1Miss() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        List<ChatMemoryMessage> l2Messages = List.of(
            new ChatMemoryMessage("user", "Hello from DB"),
            new ChatMemoryMessage("assistant", "Hi from DB!")
        );

        when(redisRepository.exists(conversationId)).thenReturn(false);
        when(mysqlRepository.getMessages(conversationId)).thenReturn(l2Messages);

        // When
        List<ChatMemoryMessage> result = chatMemory.get(conversationId);

        // Then
        assertThat(result).isEqualTo(l2Messages);
        verify(mysqlRepository).getMessages(conversationId);
        // Should populate L1 cache
        verify(redisRepository).populateCache(eq(conversationId), anyList());
    }

    @Test
    @DisplayName("get should return empty list when both L1 and L2 miss")
    void get_shouldReturnEmpty_whenBothMiss() {
        // Given
        String conversationId = UUID.randomUUID().toString();

        when(redisRepository.exists(conversationId)).thenReturn(false);
        when(mysqlRepository.getMessages(conversationId)).thenReturn(List.of());

        // When
        List<ChatMemoryMessage> result = chatMemory.get(conversationId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("clear should remove from both L1 and L2")
    void clear_shouldRemoveFromBothL1AndL2() {
        // Given
        String conversationId = UUID.randomUUID().toString();

        // When
        chatMemory.clear(conversationId);

        // Then
        verify(redisRepository).clearMessages(conversationId);
        verify(mysqlRepository).deleteConversation(conversationId);
    }

    @Test
    @DisplayName("exists should return true when L1 has data")
    void exists_shouldReturnTrue_whenL1HasData() {
        // Given
        String conversationId = UUID.randomUUID().toString();

        when(redisRepository.exists(conversationId)).thenReturn(true);

        // When
        boolean result = chatMemory.exists(conversationId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("exists should check L2 when L1 misses")
    void exists_shouldCheckL2_whenL1Misses() {
        // Given
        String conversationId = UUID.randomUUID().toString();

        when(redisRepository.exists(conversationId)).thenReturn(false);
        when(mysqlRepository.exists(conversationId)).thenReturn(true);

        // When
        boolean result = chatMemory.exists(conversationId);

        // Then
        assertThat(result).isTrue();
        verify(mysqlRepository).exists(conversationId);
    }

    @Test
    @DisplayName("createConversation should create in L2")
    void createConversation_shouldCreateInL2() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        String title = "Test";

        // When
        chatMemory.createConversation(conversationId, title);

        // Then
        verify(mysqlRepository).createConversation(conversationId, title);
    }

    @Test
    @DisplayName("listConversations should return from L2")
    void listConversations_shouldReturnFromL2() {
        // Given
        ConversationEntity conv = new ConversationEntity();
        conv.setConversationId(UUID.randomUUID().toString());
        conv.setTitle("Test");

        when(mysqlRepository.listConversations()).thenReturn(List.of(conv));

        // When
        List<ConversationEntity> result = chatMemory.listConversations();

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("deleteConversation should clear all data")
    void deleteConversation_shouldClearAllData() {
        // Given
        String conversationId = UUID.randomUUID().toString();

        // When
        chatMemory.deleteConversation(conversationId);

        // Then
        verify(redisRepository).clearMessages(conversationId);
        verify(mysqlRepository).deleteConversation(conversationId);
    }

    @Test
    @DisplayName("getConversation should return from L2")
    void getConversation_shouldReturnFromL2() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        ConversationEntity conv = new ConversationEntity();
        conv.setConversationId(conversationId);
        conv.setTitle("Test");

        when(mysqlRepository.getConversation(conversationId)).thenReturn(Optional.of(conv));

        // When
        Optional<ConversationEntity> result = chatMemory.getConversation(conversationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getConversationId()).isEqualTo(conversationId);
    }
}
