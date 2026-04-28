package com.example.chat.service;

import com.example.chat.model.ChatMemoryMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistorySummaryService {

    private final ChatModel chatModel;

    public String summarize(List<ChatMemoryMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("请用一句话概括以下对话的核心内容：\n");
        for (ChatMemoryMessage msg : messages) {
            String prefix = "user".equals(msg.role()) ? "用户" : "助手";
            sb.append(prefix).append(": ").append(msg.content()).append("\n");
        }

        try {
            String summary = ChatClient.builder(chatModel)
                    .build()
                    .prompt()
                    .system("你是一个对话摘要助手。请用一句简洁的话概括对话的核心内容，不要添加无关信息。")
                    .user(sb.toString())
                    .call()
                    .content();

            return summary != null ? summary.trim() : "";
        } catch (Exception e) {
            log.warn("Failed to summarize conversation history: {}", e.getMessage());
            return "";
        }
    }
}
