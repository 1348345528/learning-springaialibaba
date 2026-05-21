package com.example.chat.service;

import com.example.chat.config.AgentStateConfig;
import com.example.chat.model.ChatMemoryMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent 状态管理：TTL 标记 + 过期降级。
 * <p>
 * 设计：
 * - Redis 维护轻量标记键 agent:alive:{threadId}（12h TTL）
 * - 键存在 → Agent 会话活跃，MemorySaver 中状态有效
 * - 键不存在 → 会话过期，从 MySQL 取历史拼入用户消息
 * - 每次 agent.stream() 完成后续期 TTL
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentStateManager {

    private final StringRedisTemplate redis;
    private final MultiLevelChatMemory chatMemory;

    /** 检查 Agent 会话是否存活 */
    public boolean isAlive(String threadId) {
        return Boolean.TRUE.equals(redis.hasKey(AgentStateConfig.aliveKey(threadId)));
    }

    /** 续期 TTL */
    public void markAlive(String threadId) {
        redis.opsForValue().set(AgentStateConfig.aliveKey(threadId),
                String.valueOf(System.currentTimeMillis()), AgentStateConfig.STATE_TTL);
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
    }
}
