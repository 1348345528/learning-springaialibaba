package com.example.chat.dto.mcp;

public class McpToolInfo {

    private String name;
    private String description;
    private String serverName;

    public McpToolInfo() {}

    public McpToolInfo(String name, String description, String serverName) {
        this.name = name;
        this.description = description;
        this.serverName = serverName;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
}
