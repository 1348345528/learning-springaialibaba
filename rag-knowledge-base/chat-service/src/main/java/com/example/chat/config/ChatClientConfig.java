package com.example.chat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        // 不设置 defaultToolCallbacks，否则 prompt 级的 .toolCallbacks() 会失效
        return builder.build();
    }
}
