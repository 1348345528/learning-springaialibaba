package com.example.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private String sessionId;
    private String userId;
    private String message;
    private List<ImageContent> images;

    @Data
    public static class ImageContent {
        private String filename;
        private String base64Data;
        private String mimeType;
    }
}
