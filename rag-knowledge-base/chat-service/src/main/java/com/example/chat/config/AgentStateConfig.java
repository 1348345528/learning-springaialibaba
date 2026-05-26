package com.example.chat.config;

import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Agent state and context engineering configuration.
 * <p>
 * State storage: RedisSaver (persistent checkpoints via Redis).
 * Context compaction: SummarizationHook (auto-summarize old messages).
 * TTL renewal: handled by ChatHistorySyncHook after each agent loop.
 * History recovery: ChatHistorySyncHook loads from MySQL when Redis expires.
 */
@Configuration
public class AgentStateConfig {

    /** Agent session TTL: 24 hours. 每次对话后通过 ChatHistorySyncHook 自动续期。 */
    public static final Duration STATE_TTL = Duration.ofHours(24);

    /** Redis alive marker key prefix. */
    public static final String ALIVE_KEY_PREFIX = "agent:alive:";

    /** RedisSaver checkpoint key patterns — must match RedisSaver internals. */
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

    /**
     * SummarizationHook — automatically summarizes conversation history when
     * token count exceeds the threshold, preventing context window overflow.
     */
    @Bean
    public SummarizationHook summarizationHook(ChatModel chatModel) {
        return SummarizationHook.builder()
                .model(chatModel)
                .maxTokensBeforeSummary(4000)
                .messagesToKeep(10)
                .keepFirstUserMessage(true)
                .build();
    }

    public static String aliveKey(String threadId) {
        return ALIVE_KEY_PREFIX + threadId;
    }
}
