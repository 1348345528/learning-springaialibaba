package com.example.chat.service;

import com.example.chat.entity.ConversationEntity;
import com.example.chat.model.ChatMemoryMessage;
import com.example.chat.repository.MysqlChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Chat memory service — stores conversation messages for UI display.
 * <p>
 * Direct MySQL persistence. Agent state management is handled separately by RedisSaver.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiLevelChatMemory {

    private final MysqlChatMemoryRepository mysqlRepository;

    /** Add a message to the conversation. */
    public void add(String conversationId, ChatMemoryMessage message) {
        mysqlRepository.saveMessage(conversationId, message);
    }

    /** Get all messages for a conversation. */
    public List<ChatMemoryMessage> get(String conversationId) {
        return mysqlRepository.getMessages(conversationId);
    }

    /** Get the most recent N messages. */
    public List<ChatMemoryMessage> getRecent(String conversationId, int limit) {
        List<ChatMemoryMessage> messages = mysqlRepository.getMessages(conversationId);
        int size = messages.size();
        int fromIndex = Math.max(0, size - limit);
        return messages.subList(fromIndex, size);
    }

    /** Create a new conversation. */
    public void createConversation(String conversationId, String title) {
        mysqlRepository.createConversation(conversationId, title);
    }

    /** List all conversations. */
    public List<ConversationEntity> listConversations() {
        return mysqlRepository.listConversations();
    }

    /** Update conversation title. */
    public Optional<ConversationEntity> updateConversationTitle(String conversationId, String title) {
        return mysqlRepository.updateConversationTitle(conversationId, title);
    }

    /** Delete a conversation and all its messages. */
    public void deleteConversation(String conversationId) {
        mysqlRepository.deleteConversation(conversationId);
    }

    /** Get a conversation by ID. */
    public Optional<ConversationEntity> getConversation(String conversationId) {
        return mysqlRepository.getConversation(conversationId);
    }

    /** Get message count. */
    public long getMessageCount(String conversationId) {
        return mysqlRepository.getMessageCount(conversationId);
    }

    /** Check if conversation exists. */
    public boolean exists(String conversationId) {
        return mysqlRepository.exists(conversationId);
    }
}
