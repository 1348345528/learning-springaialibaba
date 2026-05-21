package com.example.chat.config;

import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Agent 状态管理配置。
 * <p>
 * 状态存储：MemorySaver（JVM 内存，读写零延迟）
 * TTL 管理：Redis 轻量标记键（agent:alive:{threadId}），由 AgentStateManager 维护
 * 降级恢复：TTL 到期后从 MySQL 加载历史回填
 */
@Configuration
public class AgentStateConfig {

    /** Agent 会话有效期：12 小时 */
    public static final Duration STATE_TTL = Duration.ofHours(12);

    /** Redis TTL 标记键前缀 */
    public static final String ALIVE_KEY_PREFIX = "agent:alive:";

    @Bean
    public MemorySaver memorySaver() {
        return new MemorySaver();
    }

    /** 构建 Redis 存活标记 key */
    public static String aliveKey(String threadId) {
        return ALIVE_KEY_PREFIX + threadId;
    }
}
