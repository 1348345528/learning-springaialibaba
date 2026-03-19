package com.example.doc.chunker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {
    private String content;
    private int index;
    private int startPos;
    private int endPos;
    private List<String> tags;
}
