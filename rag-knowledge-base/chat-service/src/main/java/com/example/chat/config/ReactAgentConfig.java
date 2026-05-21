package com.example.chat.config;

import com.example.chat.service.RagRetrievalTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ReactAgentConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public ToolCallback ragRetrievalCallback(WebClient webClient) {
        RagRetrievalTool tool = new RagRetrievalTool(webClient);
        return FunctionToolCallback
                .builder("search_knowledge_base", tool)
                .description("""
                        从知识库中检索与查询相关的文档块。
                        当你需要查找某个主题的相关信息、背景知识或文档内容时使用此工具。
                        参数 query: 要检索的问题或关键词。
                        参数 topK: 返回结果数量（默认5）。
                        返回: 相关知识块及其来源文档和相关性分数。
                        """)
                .inputType(RagRetrievalTool.Request.class)
                .build();
    }
}
