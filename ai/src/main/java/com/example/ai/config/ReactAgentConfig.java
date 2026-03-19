package com.example.ai.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReactAgentConfig {

    /**
     * 使用 MiniMax 模型的 Agent（默认）
     */
    @Bean
    ReactAgent miniMaxAgent(@Qualifier("miniMaxChatModel") ChatModel chatModel) {
       return ReactAgent.builder()
                .name("miniMaxAgent")
                .model(chatModel)
//                .systemPrompt(SYSTEM_PROMPT)
                .build();
    }

    /**
     * 使用 OpenAI(DeepSeek) 模型的 Agent
     */
    @Bean
    ReactAgent openAiAgent(@Qualifier("openAiChatModel") ChatModel chatModel) {
       return ReactAgent.builder()
                .name("openAiAgent")
                .model(chatModel)
//                .systemPrompt(SYSTEM_PROMPT)
                .build();
    }

}
