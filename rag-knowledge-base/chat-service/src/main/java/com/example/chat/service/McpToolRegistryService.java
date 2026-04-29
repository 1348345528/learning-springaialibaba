package com.example.chat.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import com.example.chat.dto.mcp.McpServerConfigDTO;
import com.example.chat.dto.mcp.McpServerInfo;
import com.example.chat.dto.mcp.McpServerStatus;
import com.example.chat.dto.mcp.McpServerType;
import com.example.chat.dto.mcp.McpToolInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class McpToolRegistryService {

    private static final Logger log = LoggerFactory.getLogger(McpToolRegistryService.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final Map<Long, McpSyncClient> clientMap = new ConcurrentHashMap<>();
    private final Map<String, ToolCallback> toolRegistry = new ConcurrentHashMap<>();
    private final Map<Long, McpServerInfo> serverMap = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接并注册 MCP Server，提取工具存入内存 Map
     */
    public McpServerInfo connect(McpServerConfigDTO dto) {
        Long id = idGen.getAndIncrement();
        McpSyncClient client;
        try {
            client = createClient(dto);
        } catch (Exception e) {
            throw new RuntimeException("创建 MCP 客户端失败: " + e.getMessage());
        }

        // 初始化
        try {
            client.initialize();
        } catch (Exception e) {
            closeClient(client);
            throw new RuntimeException("MCP Server [" + dto.getName() + "] 初始化失败: " + e.getMessage());
        }

        // 验证连接
        try {
            client.ping();
        } catch (Exception e) {
            closeClient(client);
            throw new RuntimeException("MCP Server [" + dto.getName() + "] 连接不可达: " + e.getMessage());
        }

        clientMap.put(id, client);

        // 获取工具列表
        List<McpToolInfo> toolInfos = new ArrayList<>();
        try {
            McpSchema.ListToolsResult toolsResult = client.listTools();
            List<McpSchema.Tool> tools = toolsResult.tools();
            for (McpSchema.Tool tool : tools) {
                // 用工具名直接作为 key，前端传的就是工具名
                // 注意：不同 Server 的同名工具会互相覆盖，避免注册同名工具
                ToolCallback callback = SyncMcpToolCallback.builder()
                        .mcpClient(client)
                        .tool(tool)
                        .build();
                toolRegistry.put(tool.name(), callback);
                toolInfos.add(new McpToolInfo(tool.name(), tool.description(), dto.getName()));
            }
        } catch (Exception e) {
            // 工具列表获取失败不算致命，工具选择器里不可见而已
            log.warn("MCP Server [{}] listTools 失败: {}", dto.getName(), e.getMessage());
        }

        McpServerInfo info = new McpServerInfo();
        info.setId(id);
        info.setName(dto.getName());
        info.setDescription(dto.getDescription());
        info.setType(dto.getType());
        info.setStatus(McpServerStatus.ONLINE);
        info.setToolCount(toolInfos.size());
        info.setTools(toolInfos);
        serverMap.put(id, info);

        log.info("MCP Server [{}] connected, {} tools registered", dto.getName(), toolInfos.size());
        return info;
    }

    /**
     * 断开 MCP Server 连接
     */
    public void disconnect(Long serverId) {
        McpSyncClient client = clientMap.remove(serverId);
        if (client != null) {
            closeClient(client);
        }
        McpServerInfo serverInfo = serverMap.remove(serverId);
        if (serverInfo != null && serverInfo.getTools() != null) {
            for (McpToolInfo tool : serverInfo.getTools()) {
                toolRegistry.remove(tool.getName());
            }
        }
        log.info("MCP Server [{}] disconnected", serverId);
    }

    /**
     * 根据工具名称列表查询 ToolCallback
     * 支持直接传工具名，也支持传 serverId:toolName 格式
     */
    public ToolCallback[] lookup(String... toolNames) {
        if (toolNames == null || toolNames.length == 0) {
            return new ToolCallback[0];
        }
        return Arrays.stream(toolNames)
                .map(name -> {
                    ToolCallback cb = toolRegistry.get(name);
                    if (cb != null) return cb;
                    // 兼容 serverId:toolName 格式
                    int idx = name.indexOf(':');
                    if (idx > 0) {
                        return toolRegistry.get(name.substring(idx + 1));
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toArray(ToolCallback[]::new);
    }

    /**
     * 获取所有已连接的 MCP Server 列表
     */
    public List<McpServerInfo> listServers() {
        return new ArrayList<>(serverMap.values());
    }

    /**
     * 获取所有可用的工具列表
     */
    public List<McpToolInfo> getAvailableTools() {
        List<McpToolInfo> all = new ArrayList<>();
        for (McpServerInfo server : serverMap.values()) {
            if (server.getTools() != null) {
                all.addAll(server.getTools());
            }
        }
        return all;
    }

    /**
     * 重连 MCP Server
     */
    public McpServerInfo reconnect(Long serverId, McpServerConfigDTO dto) {
        disconnect(serverId);
        return connect(dto);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down all MCP connections...");
        clientMap.forEach((id, client) -> closeClient(client));
        clientMap.clear();
        toolRegistry.clear();
        serverMap.clear();
    }

    private McpSyncClient createClient(McpServerConfigDTO dto) {
        var jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        if (dto.getType() == McpServerType.STDIO) {
            var paramsBuilder = ServerParameters.builder(dto.getCommand());
            List<String> args = parseJsonArray(dto.getArgs());
            if (!args.isEmpty()) {
                paramsBuilder.args(args);
            }
            Map<String, String> env = parseEnvVars(dto.getEnvVars());
            if (!env.isEmpty()) {
                paramsBuilder.env(env);
            }
            return McpClient.sync(new StdioClientTransport(paramsBuilder.build(), jsonMapper))
                    .requestTimeout(REQUEST_TIMEOUT)
                    .build();
        } else {
            return McpClient.sync(
                    HttpClientSseClientTransport.builder(dto.getUrl()).build()
            ).requestTimeout(REQUEST_TIMEOUT).build();
        }
    }

    private void closeClient(McpSyncClient client) {
        try {
            client.close();
        } catch (Exception e) {
            log.warn("关闭 MCP 连接异常: {}", e.getMessage());
        }
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 args JSON 失败: {}", json);
            return Collections.emptyList();
        }
    }

    private Map<String, String> parseEnvVars(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("解析 envVars JSON 失败: {}", json);
            return Collections.emptyMap();
        }
    }
}
