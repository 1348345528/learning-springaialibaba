package com.example.doc.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChunkRequest {
    private String content;
    private String documentName;
    private String strategy;
    private Integer chunkSize;
    private Integer overlap;
    private Boolean keepHeaders;
    private Integer minParagraphLength;
    private String[] delimiters;
    private Integer[] headerLevels;
    private List<String> tags;
}
