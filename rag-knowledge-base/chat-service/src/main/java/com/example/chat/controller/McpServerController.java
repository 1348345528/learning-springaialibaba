package com.example.chat.controller;

import com.example.chat.dto.mcp.McpServerConfigDTO;
import com.example.chat.dto.mcp.McpServerInfo;
import com.example.chat.dto.mcp.McpToolInfo;
import com.example.chat.service.McpToolRegistryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
@CrossOrigin(origins = "*")
public class McpServerController {

    private final McpToolRegistryService mcpRegistry;

    public McpServerController(McpToolRegistryService mcpRegistry) {
        this.mcpRegistry = mcpRegistry;
    }

    /**
     * 获取所有已注册的 MCP Server
     */
    @GetMapping("/servers")
    public ResponseEntity<List<McpServerInfo>> listServers() {
        return ResponseEntity.ok(mcpRegistry.listServers());
    }

    /**
     * 注册并连接新的 MCP Server
     */
    @PostMapping("/servers")
    public ResponseEntity<McpServerInfo> register(@Valid @RequestBody McpServerConfigDTO dto) {
        McpServerInfo info = mcpRegistry.connect(dto);
        return ResponseEntity.ok(info);
    }

    /**
     * 断开并删除 MCP Server
     */
    @DeleteMapping("/servers/{id}")
    public ResponseEntity<Void> unregister(@PathVariable Long id) {
        mcpRegistry.disconnect(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 重连 MCP Server
     */
    @PostMapping("/servers/{id}/reconnect")
    public ResponseEntity<McpServerInfo> reconnect(@PathVariable Long id, @Valid @RequestBody McpServerConfigDTO dto) {
        McpServerInfo info = mcpRegistry.reconnect(id, dto);
        return ResponseEntity.ok(info);
    }

    /**
     * 获取所有已连接的可用工具
     */
    @GetMapping("/tools")
    public ResponseEntity<List<McpToolInfo>> listTools() {
        return ResponseEntity.ok(mcpRegistry.getAvailableTools());
    }
}
