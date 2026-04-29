package com.example.chat.dto.mcp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class McpServerConfigDTO {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private McpServerType type;

    // STDIO fields
    private String command;
    private String args;

    // SSE fields
    private String url;

    private String envVars;

    public @NotBlank String getName() { return name; }
    public void setName(@NotBlank String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public @NotNull McpServerType getType() { return type; }
    public void setType(@NotNull McpServerType type) { this.type = type; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getArgs() { return args; }
    public void setArgs(String args) { this.args = args; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getEnvVars() { return envVars; }
    public void setEnvVars(String envVars) { this.envVars = envVars; }
}
