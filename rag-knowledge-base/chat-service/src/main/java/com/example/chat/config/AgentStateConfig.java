package com.example.chat.config;

import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Agent 状态管理配置。
 * <p>
 * 状态存储：RedisSaver（Redis 持久化，重启不丢失）
 * TTL：12 小时，每次 agent.stream() 后由 AgentStateManager 续期
 * 降级恢复：TTL 到期后从 MySQL 加载历史
 */
@Configuration
public class AgentStateConfig {

    /** Agent 会话有效期：12 小时 */
    public static final Duration STATE_TTL = Duration.ofHours(12);

    /** Redis TTL 标记键前缀 */
    public static final String ALIVE_KEY_PREFIX = "agent:alive:";

    /** RedisSaver checker key patterns — must match RedisSaver internals */
    public static final String CHECKPOINT_KEY_PREFIX = "graph:checkpoint:content:";
    public static final String THREAD_META_KEY_PREFIX = "graph:thread:meta:";
    public static final String THREAD_REVERSE_KEY_PREFIX = "graph:thread:reverse:";
    public static final String LOCK_KEY_PREFIX = "graph:checkpoint:lock:";

    @Bean
    public RedisSaver redisSaver(RedissonClient redissonClient) {
        return RedisSaver.builder()
                .redisson(redissonClient)
                .build();
    }

    public static String aliveKey(String threadId) {
        return ALIVE_KEY_PREFIX + threadId;
    }
}
