package com.example.chat.service;

import com.example.chat.model.ChatMemoryMessage;
import com.example.chat.repository.MysqlChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Service for asynchronously syncing chat memory from L1 (Redis) to L2 (MySQL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMemorySyncService {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final MysqlChatMemoryRepository mysqlRepository;
    private final ExecutorService executorService;

    /**
     * Asynchronously sync a single message to MySQL.
     */
    public void asyncSyncToMysql(String conversationId, ChatMemoryMessage message) {
        executorService.submit(() -> {
            syncWithRetry(conversationId, message, 0);
        });
    }

    /**
     * Sync a batch of messages to MySQL.
     */
    public void syncBatch(String conversationId, List<ChatMemoryMessage> messages) {
        executorService.submit(() -> {
            for (ChatMemoryMessage message : messages) {
                syncWithRetry(conversationId, message, 0);
            }
        });
    }

    /**
     * Sync with retry mechanism.
     */
    private void syncWithRetry(String conversationId, ChatMemoryMessage message, int attempt) {
        try {
            mysqlRepository.saveMessage(conversationId, message);
            log.debug("Successfully synced message to MySQL for conversation: {}", conversationId);
        } catch (Exception e) {
            log.warn("Failed to sync message to MySQL (attempt {}/{}): {}",
                    attempt + 1, MAX_RETRIES, e.getMessage());

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                syncWithRetry(conversationId, message, attempt + 1);
            } else {
                log.error("Max retries exhausted for conversation: {}", conversationId);
            }
        }
    }
}
