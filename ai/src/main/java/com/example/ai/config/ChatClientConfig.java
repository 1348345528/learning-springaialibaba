package com.example.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {


    /**
     * 使用 MiniMax 模型的 ChatClient
     */
    @Bean
    ChatClient miniMaxChatClient(
            @Qualifier("miniMaxChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    /**
     * 使用 OpenAI(DeepSeek) 模型的 ChatClient
     */
    @Bean
    ChatClient openAiChatClient(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }
}
