package com.example.chat.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private int topK = 5;
    private boolean stream = true;
}
