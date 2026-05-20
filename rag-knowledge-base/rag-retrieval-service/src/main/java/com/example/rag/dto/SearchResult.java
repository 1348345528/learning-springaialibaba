package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private String id;
    private float score;
    private Float denseScore;
    private Float sparseScore;
    private String content;
    private String documentName;
    private String docId;
    private String source;
}
