package com.example.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String sessionId;
    private String message;
    private String type; // TEXT, CARD, ERROR
    private Map<String, Object> data;
    private String confirmAction; // 确认操作类型
    private Map<String, Object> confirmParams; // 确认操作参数
}
