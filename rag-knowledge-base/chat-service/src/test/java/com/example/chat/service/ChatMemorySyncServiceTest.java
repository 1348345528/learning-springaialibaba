package com.example.chat.service;

import com.example.chat.model.ChatMemoryMessage;
import com.example.chat.repository.MysqlChatMemoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatMemorySyncService.
 */
class ChatMemorySyncServiceTest {

    private MysqlChatMemoryRepository mysqlRepository;
    private ChatMemorySyncService syncService;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        mysqlRepository = mock(MysqlChatMemoryRepository.class);
        executorService = Executors.newSingleThreadExecutor();
        syncService = new ChatMemorySyncService(mysqlRepository, executorService);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("asyncSyncToMysql should persist message to MySQL")
    void asyncSyncToMysql_shouldPersistMessage() throws InterruptedException {
        // Given
        String conversationId = UUID.randomUUID().toString();
        ChatMemoryMessage message = new ChatMemoryMessage("user", "Hello!");

        // When
        syncService.asyncSyncToMysql(conversationId, message);

        // Wait for async execution
        Thread.sleep(200);

        // Then
        verify(mysqlRepository, timeout(1000)).saveMessage(conversationId, message);
    }

    @Test
    @DisplayName("asyncSyncToMysql should handle exceptions gracefully")
    void asyncSyncToMysql_shouldHandleExceptionsGracefully() throws InterruptedException {
        // Given
        String conversationId = UUID.randomUUID().toString();
        ChatMemoryMessage message = new ChatMemoryMessage("user", "Hello!");

        doThrow(new RuntimeException("DB Error"))
            .doNothing()
            .when(mysqlRepository).saveMessage(anyString(), any(ChatMemoryMessage.class));

        // When - should not throw
        syncService.asyncSyncToMysql(conversationId, message);

        // Wait for async execution
        Thread.sleep(500);

        // Then - should retry and eventually succeed
        verify(mysqlRepository, timeout(1000).atLeast(1)).saveMessage(conversationId, message);
    }

    @Test
    @DisplayName("syncBatch should sync multiple messages")
    void syncBatch_shouldSyncMultipleMessages() throws InterruptedException {
        // Given
        String conversationId = UUID.randomUUID().toString();
        var messages = List.of(
            new ChatMemoryMessage("user", "Msg 1"),
            new ChatMemoryMessage("assistant", "Msg 2"),
            new ChatMemoryMessage("user", "Msg 3")
        );

        // When
        syncService.syncBatch(conversationId, messages);

        // Wait for async execution
        Thread.sleep(300);

        // Then
        verify(mysqlRepository, timeout(1000).times(3)).saveMessage(eq(conversationId), any(ChatMemoryMessage.class));
    }
}
