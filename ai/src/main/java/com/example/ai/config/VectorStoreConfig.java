package com.example.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 手动声明VectorStore Bean，解决1.1.x版本注入失败问题
 * 注入的是 内存版SimpleVectorStore，零配置、免费、适合测试开发
 */
@Configuration
public class VectorStoreConfig {

    /**
     * 核心Bean：创建SimpleVectorStore对象
     * 依赖注入EmbeddingModel（你的项目里已经能正常注入，无需额外配置）
     */
    @Bean
    public VectorStore vectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {

        return SimpleVectorStore.builder(embeddingModel).build();
    }
}