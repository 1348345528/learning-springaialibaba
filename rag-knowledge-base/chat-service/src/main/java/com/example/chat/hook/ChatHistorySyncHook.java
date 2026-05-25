package com.example.chat.hook;

import com.example.chat.config.AgentStateConfig;
import com.example.chat.entity.ChatMessageEntity;
import com.example.chat.entity.ConversationEntity;
import com.example.chat.repository.jpa.ChatMessageJpaRepository;
import com.example.chat.repository.jpa.ConversationJpaRepository;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import org.redisson.api.RMap;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 对话消息同步 Hook — 在 Agent 的 State Graph 和 MySQL 之间自动同步消息。
 * <p>
 * <b>为什么需要这个 Hook？</b>
 * <p>
 * ReactAgent 的消息存在两个地方：
 * <ol>
 *   <li><b>State Graph</b>（Redis checkpoint）— Agent 运行时内存，由框架自动管理。
 *       消息通过 AppendStrategy 累积在 "messages" 键下，RedisSaver 做 checkpoint 持久化。</li>
 *   <li><b>MySQL</b> — 供前端 UI 展示会话列表和历史消息。</li>
 * </ol>
 * 两个存储各司其职：
 * State Graph 是"运行时真相"，供 LLM 理解上下文；
 * MySQL 是"持久化展示"，供用户翻阅历史。
 * <p>
 * <b>两个生命周期钩子：</b>
 * <ul>
 *   <li><b>beforeAgent</b>（Agent 循环开始前）—
 *       Redis checkpoint 过期时 State Graph 是空的，从 MySQL 加载最近 20 条消息
 *       注入 State Graph，让 Agent 能接上之前的对话。</li>
 *   <li><b>afterAgent</b>（Agent 循环结束后）—
 *       把本轮新增的消息同步到 MySQL，同时续期 Redis checkpoint 的 TTL。</li>
 * </ul>
 * <p>
 * <b>去重策略：</b>
 * <p>
 * 不用计数器，而是区分两种场景：
 * <ol>
 *   <li><b>正常流程</b>（Redis 存活）— State Graph 包含完整历史。
 *       MySQL 里有 N 条，State Graph 有 N+M 条 → 只同步尾部 M 条新消息。</li>
 *   <li><b>恢复流程</b>（Redis 过期）— State Graph 只有从 MySQL 恢复的最近 20 条 + 本轮新增。
 *       beforeAgent 把恢复条数记在 Redis 里，afterAgent 从那个位置往后同步，
 *       不会把恢复的 20 条重新写入 MySQL。</li>
 * </ol>
 * <p>
 * 替代了之前手写的 {@code AgentStateManager} + {@code MultiLevelChatMemory}。
 */
@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class ChatHistorySyncHook extends MessagesAgentHook {

    private static final Logger log = LoggerFactory.getLogger(ChatHistorySyncHook.class);

    /** Redis 过期时从 MySQL 恢复多少条最近消息 */
    private static final int RECOVERY_MESSAGE_LIMIT = 20;



    private final ConversationJpaRepository conversationRepo;
    private final ChatMessageJpaRepository messageRepo;
    private final StringRedisTemplate redis;
    private final RedissonClient redissonClient;

    public ChatHistorySyncHook(ConversationJpaRepository conversationRepo,
                               ChatMessageJpaRepository messageRepo,
                               StringRedisTemplate redis,
                               RedissonClient redissonClient) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.redis = redis;
        this.redissonClient = redissonClient;
    }

    // ── beforeAgent：Redis 过期时从 MySQL 恢复历史 ──────────────────

    @Override
    public AgentCommand beforeAgent(List<Message> previousMessages, RunnableConfig config) {
        String threadId = config.threadId().orElse(null);
        if (threadId == null) {
            return new AgentCommand(previousMessages);
        }

        // State Graph 里已经有消息 → Redis checkpoint 正常恢复的，不用管
        if (!previousMessages.isEmpty()) {
            log.debug("State Graph 已有 {} 条消息, thread={}, 无需从 MySQL 恢复",
                    previousMessages.size(), threadId);
            return new AgentCommand(previousMessages);
        }

        // State Graph 是空的 → Redis 过期了，从 MySQL 捞历史
        List<Message> recovered = loadRecentFromMysql(threadId);
        if (recovered.isEmpty()) {
            log.debug("MySQL 中无历史消息, thread={}", threadId);
            return new AgentCommand(previousMessages);
        }

        log.info("从 MySQL 恢复了 {} 条消息, thread={} (Redis 已过期)",
                recovered.size(), threadId);

        // REPLACE 用恢复的消息替换 State Graph 中空的消息列表
        return new AgentCommand(recovered, UpdatePolicy.REPLACE);
    }

    // ── afterAgent：同步到 MySQL + 续期 TTL ────────────────────────

    @Override
    public AgentCommand afterAgent(List<Message> previousMessages, RunnableConfig config) {
        String threadId = config.threadId().orElse(null);
        if (threadId == null) {
            return new AgentCommand(previousMessages);
        }

        // 1. 把本轮新增的消息写入 MySQL
        syncToMysql(threadId, previousMessages);

        // 2. 续期 Redis checkpoint 的 TTL（12h）
        renewTtl(threadId);

        // 3. 不修改消息，原样返回
        return new AgentCommand(previousMessages);
    }

    // ── MySQL 同步逻辑 ──────────────────────────────────────────────

    /**
     * 把 State Graph 中的新消息同步到 MySQL。
     * <p>
     * <b>不使用索引计数，改用内容匹配定位"已同步到哪了"。</b>
     * 这样 SummarizationHook 压缩 State Graph 不会影响 MySQL ——
     * MySQL 始终保持完整历史，用户可追溯所有对话。
     * <p>
     * 逻辑：从 State Graph 尾部往前找，对比 MySQL 尾部，
     * 找到"最后一条两边都有的消息"，其后就是需要新增的。
     */
    private void syncToMysql(String threadId, List<Message> messages) {
        if (messages.isEmpty()) return;

        // 确保 MySQL 里有这个会话
        ConversationEntity conversation = conversationRepo.findByConversationId(threadId)
                .orElseGet(() -> {
                    ConversationEntity entity = new ConversationEntity();
                    entity.setConversationId(threadId);
                    entity.setTitle("New Conversation");
                    return conversationRepo.save(entity);
                });

        // 去重：从尾部对比 State Graph 和 MySQL，找到"分叉点"
        int firstNewIndex = findFirstNewIndex(conversation.getId(), messages);

        if (firstNewIndex >= messages.size()) {
            return; // 全部已同步
        }

        // 从分叉点开始写入新消息到 MySQL
        int saved = 0;
        for (int i = firstNewIndex; i < messages.size(); i++) {
            Message msg = messages.get(i);
            ChatMessageEntity entity = new ChatMessageEntity();
            entity.setConversation(conversation);
            entity.setRole(toRoleString(msg));
            entity.setContent(msg.getText());
            messageRepo.save(entity);
            saved++;
        }

        // 更新会话的 updatedAt
        conversationRepo.save(conversation);

        if (saved > 0) {
            log.debug("同步 {} 条新消息到 MySQL, thread={} (sg={}, from={})",
                    saved, threadId, messages.size(), firstNewIndex);
        }
    }

    /**
     * 从 State Graph 尾部往前，对比 MySQL 尾部，找到第一条"MySQL 还没有"的消息位置。
     * <p>
     * 同时处理 {@code recovered} 标记（Redis 过期恢复场景）。
     */
    private int findFirstNewIndex(Long conversationId, List<Message> sgMessages) {
        // MySQL 中已有的消息
        List<ChatMessageEntity> mysqlMessages = messageRepo
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        if (mysqlMessages.isEmpty()) {
            return 0; // MySQL 空的，全部同步
        }

        // 从尾部往前对比，找"最后一条匹配的消息"
        int sgIdx = sgMessages.size() - 1;
        int mysqlIdx = mysqlMessages.size() - 1;
        int matchCount = 0;

        while (sgIdx >= 0 && mysqlIdx >= 0) {
            Message sgMsg = sgMessages.get(sgIdx);
            ChatMessageEntity mysqlMsg = mysqlMessages.get(mysqlIdx);

            if (messagesMatch(sgMsg, mysqlMsg)) {
                matchCount++;
                sgIdx--;
                mysqlIdx--;
            } else {
                // 不匹配，尝试往前多看几条（压缩可能导致 SG 跳过一些消息）
                // SG 当前这条可能在 MySQL 更前面的位置
                boolean found = false;
                for (int lookback = 1; lookback <= 10 && (mysqlIdx - lookback) >= 0; lookback++) {
                    if (messagesMatch(sgMsg, mysqlMessages.get(mysqlIdx - lookback))) {
                        mysqlIdx = mysqlIdx - lookback - 1;
                        sgIdx--;
                        matchCount++;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    break; // 确实不匹配，这就是分叉点
                }
            }
        }

        // 分叉点 = SG 中第一条未匹配的消息
        int firstNew = sgMessages.size() - matchCount;
        return Math.max(0, firstNew);
    }

    /** 判断一条 State Graph Message 和 MySQL ChatMessageEntity 是否代表同一条消息 */
    private static boolean messagesMatch(Message sgMsg, ChatMessageEntity mysqlMsg) {
        String sgRole = toRoleString(sgMsg);
        String mysqlRole = mysqlMsg.getRole();
        if (!sgRole.equals(mysqlRole)) return false;
        // 内容对比：SG 的 getText() vs MySQL 的 content
        String sgText = sgMsg.getText();
        String mysqlText = mysqlMsg.getContent();
        return sgText != null && sgText.equals(mysqlText);
    }

    /** 从 MySQL 加载最近 N 条消息（Redis 过期恢复用） */
    private List<Message> loadRecentFromMysql(String threadId) {
        Optional<ConversationEntity> conversationOpt = conversationRepo.findByConversationId(threadId);
        if (conversationOpt.isEmpty()) {
            return List.of();
        }

        List<ChatMessageEntity> entities = messageRepo
                .findByConversationIdOrderByCreatedAtAsc(conversationOpt.get().getId());

        if (entities.isEmpty()) {
            return List.of();
        }

        // 只取最近 N 条（全量可能导致上下文过长）
        int fromIndex = Math.max(0, entities.size() - RECOVERY_MESSAGE_LIMIT);
        List<ChatMessageEntity> recent = entities.subList(fromIndex, entities.size());

        List<Message> messages = new ArrayList<>();
        for (ChatMessageEntity entity : recent) {
            messages.add(toSpringAiMessage(entity));
        }

        return messages;
    }

    // ── TTL 管理 ────────────────────────────────────────────────────

    /** 续期 alive 标记 + checkpoint 数据的 TTL */
    private void renewTtl(String threadId) {
        Duration ttl = AgentStateConfig.STATE_TTL;
        // 续期 alive 标记
        redis.opsForValue().set(AgentStateConfig.aliveKey(threadId),
                String.valueOf(System.currentTimeMillis()), ttl);

        // 续期 checkpoint 相关的 Redis key
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
            log.debug("续期 checkpoint TTL 失败, thread={}: {}", threadId, e.getMessage());
        }
    }

    /** 删除会话的所有 Redis 状态（ConversationController 删除会话时调用） */
    public void deleteState(String threadId) {
        redis.delete(AgentStateConfig.aliveKey(threadId));

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
            log.debug("删除 checkpoint 数据失败, thread={}: {}", threadId, e.getMessage());
        }
    }

    // ── 工具方法 ────────────────────────────────────────────────────

    /** Spring AI Message → MySQL 角色字符串 */
    private static String toRoleString(Message msg) {
        if (msg instanceof AssistantMessage) return "assistant";
        if (msg instanceof UserMessage) return "user";
        // 兜底：用 MessageType 的名字
        String type = msg.getMessageType().name().toLowerCase();
        return switch (type) {
            case "assistant" -> "assistant";
            case "user" -> "user";
            case "system" -> "system";
            case "tool" -> "tool";
            default -> "unknown";
        };
    }

    /** MySQL 实体 → Spring AI Message */
    private static Message toSpringAiMessage(ChatMessageEntity entity) {
        return switch (entity.getRole()) {
            case "user" -> new UserMessage(entity.getContent());
            case "assistant" -> new AssistantMessage(entity.getContent());
            default -> new UserMessage(entity.getContent());
        };
    }

    // ── Hook 元数据 ─────────────────────────────────────────────────

    @Override
    public String getName() {
        return "ChatHistorySync";
    }

    @Override
    public List<JumpTo> canJumpTo() {
        return List.of();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    // Hook 接口要求实现 setAgent/getAgent（MessagesAgentHook 未提供默认实现）
    private com.alibaba.cloud.ai.graph.agent.ReactAgent agent;

    @Override
    public com.alibaba.cloud.ai.graph.agent.ReactAgent getAgent() {
        return agent;
    }

    @Override
    public void setAgent(com.alibaba.cloud.ai.graph.agent.ReactAgent agent) {
        this.agent = agent;
    }
}
