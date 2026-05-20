package com.example.rag.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class HybridSearchRequest {
    @NotBlank(message = "query cannot be blank")
    private String query;

    @Min(value = 1, message = "topK must be at least 1")
    private Integer topK = 5;

    private Float denseWeight = 0.7f;
    private Float sparseWeight = 0.3f;
    private Integer rrfK = 60;
    private Boolean useRrf = true;

    private Map<String, String> filters;

    private Boolean useHyde = false;
    private Boolean useRerank = true;
    private Integer rerankTopN = 20;
}
