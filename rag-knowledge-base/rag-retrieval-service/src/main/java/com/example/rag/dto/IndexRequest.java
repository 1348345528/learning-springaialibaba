package com.example.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IndexRequest {
    @NotBlank(message = "id cannot be blank")
    private String id;

    @NotBlank(message = "content cannot be blank")
    private String content;

    private String documentName;
}
