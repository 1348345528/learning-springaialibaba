package com.example.rag.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    @Value("${milvus.host}")
    private String host;

    @Value("${milvus.port}")
    private int port;

    @Value("${milvus.username:}")
    private String username;

    @Value("${milvus.password:}")
    private String password;

    @Bean
    public MilvusClientV2 milvusClient() {
        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                .uri(String.format("http://%s:%d", host, port))
                .connectTimeoutMs(2000L);

        if (username != null && !username.isEmpty()) {
            builder.token(username + ":" + password);
        }

        return new MilvusClientV2(builder.build());
    }
}
