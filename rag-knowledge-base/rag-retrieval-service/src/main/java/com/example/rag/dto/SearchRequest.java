package com.example.rag.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {
    @NotBlank(message = "query cannot be blank")
    private String query;

    @Min(value = 1, message = "topK must be at least 1")
    private Integer topK = 5;
}
