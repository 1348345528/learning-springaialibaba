package com.example.chat.repository;

import com.example.chat.entity.ChatMessageEntity;
import com.example.chat.entity.ConversationEntity;
import com.example.chat.model.ChatMemoryMessage;
import com.example.chat.repository.jpa.ChatMessageJpaRepository;
import com.example.chat.repository.jpa.ConversationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MysqlChatMemoryRepository.
 * Tests the L2 persistent layer operations.
 */
@ExtendWith(MockitoExtension.class)
class MysqlChatMemoryRepositoryTest {

    @Mock
    private ConversationJpaRepository conversationRepository;

    @Mock
    private ChatMessageJpaRepository messageRepository;

    private MysqlChatMemoryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MysqlChatMemoryRepository(conversationRepository, messageRepository);
    }

    @Test
    @DisplayName("createConversation should create new conversation")
    void createConversation_shouldCreateConversation() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        String title = "Test Conversation";

        ConversationEntity savedEntity = new ConversationEntity();
        savedEntity.setId(1L);
        savedEntity.setConversationId(conversationId);
        savedEntity.setTitle(title);
        savedEntity.setCreatedAt(LocalDateTime.now());
        savedEntity.setUpdatedAt(LocalDateTime.now());

        when(conversationRepository.save(any(ConversationEntity.class))).thenReturn(savedEntity);

        // When
        ConversationEntity result = repository.createConversation(conversationId, title);

        // Then
        assertThat(result.getConversationId()).isEqualTo(conversationId);
        assertThat(result.getTitle()).isEqualTo(title);
        verify(conversationRepository).save(any(ConversationEntity.class));
    }

    @Test
    @DisplayName("getConversation should return conversation when exists")
    void getConversation_shouldReturnConversation_whenExists() {
        // Given
        String conversationId = UUID.randomUUID().toString();
        ConversationEntity entity = new ConversationEntity();
        entity.setConversationId(conversationId);
        entity.setTitle("Test");

        when(conversationRepository.findByConversationId(conversationId))
            .thenReturn(Optional.of(entity));

        // When
        Optional<ConversationEntity> result = repository.getConversation(conversationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getConversationId()).isEqualTo(conversationId);
    }

    @Test
    @DisplayName("getConversation should return empty when not exists")
    void getConversation_shouldReturnEmpty_whenNotExists() {
        // Given
        String conversationId = UUID.randomUUID().toString();

        when(conversationRepository.findByConversationId(conversationId))
            .thenReturn(Optional.empty());

        // When
        Optional<ConversationEntity> result = repository.getConversation(conversationId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMessages should return messages from database")
    void getMessages_shouldReturnMessages() {
        // Given
        String conversationId = UUID.randomUUID().toString();

        ConversationEntity conversation = new ConversationEntity();
        conversation.setId(1L);
        conversation.setConversationId(conversationId);

        ChatMessageEntity msg1 = new ChatMessageEntity();
        msg1.setConversation(conversation);
        msg1.setRole("user");
        msg1.setContent("Hello");
        msg1.setCreatedAt(LocalDateTime.now());

        ChatMessageEntity msg2 = new ChatMessageEntity();
        msg2.setConversation(conversation);
        msg2.setRole("assistant");
        msg2.setContent("Hi there!");
        msg2.setCreatedAt(LocalDateTime.now());

        when(conversationRepository.findByConversationId(conversationId))
            .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1L))
            .thenReturn(List.of(msg1, msg2));

        // When
        List<ChatMemoryMessage> result = repository.getMessages(conversationId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("user");
        assertThat(result.get(1).role()).isEqualTo("assistant");
    }

    @Test
    @DisplayName("getMessages should return empty list when conversation not found")
    void getMessages_shouldReturnEmptyList_whenConversationNotFound() {
        // Given
        String conversationId = UUID.randomUUID().toString();

        when(conversationRepository.findByConversationId(conversationId))
            .thenReturn(Optional.empty());

        // When
        List<ChatMemoryMessage> result = repository.getMessages(conversationId);

        // Then
        assertThat(result).isEmpty();
        verify(messageRepository, never()).findByConversationIdOrderByCreatedAtAsc(anyLong());
    }

    @Test
    @DisplayName("deleteConversation should remove conversation and messages")
    void deleteConversation_shouldRemoveConversationAndMessages() {
        // Given
        String conversationId = UUID.randomUUID().toString();

        ConversationEntity conversation = new ConversationEntity();
        conversation.setId(1L);
        conversation.setConversationId(conversationId);

        when(conversationRepository.findByConversationId(conversationId))
            .thenReturn(Optional.of(conversation));

        // When
        repository.deleteConversation(conversationId);

        // Then
        verify(messageRepository).deleteByConversationId(1L);
        verify(conversationRepository).delete(conversation);
    }

    @Test
    @DisplayName("listConversations should return all conversations")
    void listConversations_shouldReturnAllConversations() {
        // Given
        ConversationEntity conv1 = new ConversationEntity();
        conv1.setConversationId(UUID.randomUUID().toString());
        conv1.setTitle("Conv 1");
        conv1.setCreatedAt(LocalDateTime.now());

        ConversationEntity conv2 = new ConversationEntity();
        conv2.setConversationId(UUID.randomUUID().toString());
        conv2.setTitle("Conv 2");
        conv2.setCreatedAt(LocalDateTime.now());

        when(conversationRepository.findAllByOrderByUpdatedAtDesc())
            .thenReturn(List.of(conv1, conv2));

        // When
        List<ConversationEntity> result = repository.listConversations();

        // Then
        assertThat(result).hasSize(2);
    }
}
