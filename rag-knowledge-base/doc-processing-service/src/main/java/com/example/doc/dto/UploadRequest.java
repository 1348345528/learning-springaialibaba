package com.example.doc.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UploadRequest {
    private MultipartFile file;
    private String strategy;
    private Integer chunkSize;
    private Integer overlap;
    private Boolean keepHeaders;
    private Integer minParagraphLength;
    private String[] delimiters;
    private Integer[] headerLevels;
    private List<String> tags;
}
