package com.example.chat.dto.mcp;

import java.util.List;

public class McpServerInfo {

    private Long id;
    private String name;
    private String description;
    private McpServerType type;
    private McpServerStatus status;
    private int toolCount;
    private String errorMessage;
    private List<McpToolInfo> tools;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public McpServerType getType() { return type; }
    public void setType(McpServerType type) { this.type = type; }

    public McpServerStatus getStatus() { return status; }
    public void setStatus(McpServerStatus status) { this.status = status; }

    public int getToolCount() { return toolCount; }
    public void setToolCount(int toolCount) { this.toolCount = toolCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<McpToolInfo> getTools() { return tools; }
    public void setTools(List<McpToolInfo> tools) { this.tools = tools; }
}
