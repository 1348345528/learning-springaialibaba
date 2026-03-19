package com.example.ai.handler;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;
import org.springaicommunity.mcp.annotation.McpToolListChanged;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class McpClientHandlers {

//    @McpLogging(clients = "server1")
//    public void handleLoggingMessage(McpSchema.LoggingMessageNotification notification) {
//        System.out.println("Received log: " + notification.level() +
//                " - " + notification.data());
//    }
//
//    @McpSampling(clients = "server1")
//    public McpSchema.CreateMessageResult handleSamplingRequest(McpSchema.CreateMessageRequest request) {
//        // 处理请求并生成响应
//        String response = request.messages().toString();
//
//        return McpSchema.CreateMessageResult.builder()
//                .role(McpSchema.Role.ASSISTANT)
//                .content(new McpSchema.TextContent(response))
//                .model("gpt-4")
//                .build();
//    }
//
//    @McpProgress(clients = "server1")
//    public void handleProgressNotification(McpSchema.ProgressNotification notification) {
//        double percentage = notification.progress() * 100;
//        System.out.println(String.format("Progress: %.2f%% - %s",
//                percentage, notification.message()));
//    }
//
//    @McpToolListChanged(clients = "server1")
//    public void handleToolListChanged(List<McpSchema.Tool> updatedTools) {
//        System.out.println("Tool list updated: " + updatedTools.size() + " tools available");
//        // 更新本地工具注册表
//        toolRegistry.updateTools(updatedTools);
//    }
}
