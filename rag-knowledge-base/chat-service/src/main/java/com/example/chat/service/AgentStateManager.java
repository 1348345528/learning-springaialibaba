package com.example.chat.service;

import com.example.chat.config.AgentStateConfig;
import com.example.chat.model.ChatMemoryMessage;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Agent 状态管理：TTL 标记 + 过期降级 + 定期清理。
 * <p>
 * 设计：
 * - Redis 维护轻量标记键 agent:alive:{threadId}（12h TTL）
 * - 键存在 → Agent 会话活跃，MemorySaver 中状态有效
 * - 键不存在 → 会话过期，从 MySQL 取历史拼入用户消息
 * - 每次 agent.stream() 完成后同时续期 alive 标记和 checkpoint 数据的 TTL
 * - 定时任务扫描并清理无 alive 标记的孤儿 checkpoint 数据
 */
@Component
@Slf4j
public class AgentStateManager {

    private final StringRedisTemplate redis;
    private final RedissonClient redissonClient;
    private final MultiLevelChatMemory chatMemory;

    public AgentStateManager(StringRedisTemplate redis,
                             RedissonClient redissonClient,
                             MultiLevelChatMemory chatMemory) {
        this.redis = redis;
        this.redissonClient = redissonClient;
        this.chatMemory = chatMemory;
    }

    /** 检查 Agent 会话是否存活 */
    public boolean isAlive(String threadId) {
        return Boolean.TRUE.equals(redis.hasKey(AgentStateConfig.aliveKey(threadId)));
    }

    /** 续期 TTL：alive 标记 + 对应的 checkpoint 数据 */
    public void markAlive(String threadId) {
        Duration ttl = AgentStateConfig.STATE_TTL;
        redis.opsForValue().set(AgentStateConfig.aliveKey(threadId),
                String.valueOf(System.currentTimeMillis()), ttl);
        renewCheckpointTtl(threadId, ttl);
    }

    /**
     * 给 RedisSaver 存储的 checkpoint 相关 Redis key 续期 TTL。
     * <p>
     * RedisSaver 内部 key 结构：
     * - thread_meta:{threadId} → Map (thread_id=UUID, is_released, thread_name)
     * - checkpoints:{uuid} → Bucket (序列化的 checkpoint 列表)
     * - thread_meta_reverse:{uuid} → Map (反向映射)
     * - checkpoint:{threadId} → Lock (分布式锁，不续 TTL 也无妨)
     */
    private void renewCheckpointTtl(String threadId, Duration ttl) {
        try {
            RMap<String, String> threadMeta = redissonClient.getMap(
                    AgentStateConfig.THREAD_META_KEY_PREFIX + threadId);
            String activeUuid = threadMeta.get("thread_id");
            if (activeUuid != null) {
                RBucket<Object> checkpointBucket = redissonClient.getBucket(
                        AgentStateConfig.CHECKPOINT_KEY_PREFIX + activeUuid);
                checkpointBucket.expire(ttl);

                RMap<String, String> reverseMap = redissonClient.getMap(
                        AgentStateConfig.THREAD_REVERSE_KEY_PREFIX + activeUuid);
                reverseMap.expire(ttl);
            }
            threadMeta.expire(ttl);
        } catch (Exception e) {
            log.debug("Failed to renew checkpoint TTL for threadId={}: {}", threadId, e.getMessage());
        }
    }

    /**
     * 从 MySQL 加载历史，拼成上下文前缀。
     * <p>
     * 返回带历史上下文的用户消息，Agent 第一次请求时拼在前面。
     * 会话过期后 Agent 的 MemorySaver state 丢失，但通过消息上下文恢复连贯性。
     */
    public String buildContextPrefix(String threadId) {
        List<ChatMemoryMessage> history = chatMemory.getRecent(threadId, 20);
        if (history.isEmpty()) return null;

        StringBuilder ctx = new StringBuilder("[以下是之前的对话历史，请基于此继续]\n");
        for (ChatMemoryMessage m : history) {
            ctx.append(m.role()).append(": ").append(m.content()).append("\n");
        }
        ctx.append("---\n当前问题: ");
        return ctx.toString();
    }

    /** 删除会话状态 */
    public void deleteState(String threadId) {
        redis.delete(AgentStateConfig.aliveKey(threadId));
        deleteCheckpointData(threadId);
    }

    /** 删除对应的 checkpoint 数据 */
    private void deleteCheckpointData(String threadId) {
        try {
            RMap<String, String> threadMeta = redissonClient.getMap(
                    AgentStateConfig.THREAD_META_KEY_PREFIX + threadId);
            String activeUuid = threadMeta.get("thread_id");
            if (activeUuid != null) {
                redissonClient.getBucket(AgentStateConfig.CHECKPOINT_KEY_PREFIX + activeUuid).delete();
                redissonClient.getMap(AgentStateConfig.THREAD_REVERSE_KEY_PREFIX + activeUuid).delete();
            }
            threadMeta.delete();
        } catch (Exception e) {
            log.debug("Failed to delete checkpoint data for threadId={}: {}", threadId, e.getMessage());
        }
    }

    /**
     * 定时清理孤儿 checkpoint 数据（无对应 alive 标记的过期数据）。
     * 每 30 分钟执行一次。
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void cleanupOrphanedCheckpoints() {
        try {
            Set<String> checkpointKeys = redis.keys(AgentStateConfig.CHECKPOINT_KEY_PREFIX + "*");
            if (checkpointKeys == null || checkpointKeys.isEmpty()) return;

            int cleaned = 0;
            for (String cpKey : checkpointKeys) {
                // 反向查：通过 thread_meta_reverse:{uuid} 找到 threadId
                String uuid = cpKey.substring(AgentStateConfig.CHECKPOINT_KEY_PREFIX.length());
                RMap<String, String> reverseMap = redissonClient.getMap(
                        AgentStateConfig.THREAD_REVERSE_KEY_PREFIX + uuid);
                String threadId = reverseMap.get("thread_name");
                if (threadId != null && !isAlive(threadId)) {
                    redissonClient.getBucket(cpKey).delete();
                    reverseMap.delete();
                    redissonClient.getMap(AgentStateConfig.THREAD_META_KEY_PREFIX + threadId).delete();
                    cleaned++;
                } else if (threadId == null) {
                    // 反向映射丢失，直接清理孤儿 checkpoints bucket
                    redissonClient.getBucket(cpKey).delete();
                    reverseMap.delete();
                    cleaned++;
                }
            }
            if (cleaned > 0) {
                log.info("Cleaned up {} orphaned checkpoint entries", cleaned);
            }
        } catch (Exception e) {
            log.warn("Orphaned checkpoint cleanup failed: {}", e.getMessage());
        }
    }
}
