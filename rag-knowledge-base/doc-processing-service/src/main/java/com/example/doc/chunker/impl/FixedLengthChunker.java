package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FixedLengthChunker implements ChunkStrategy {

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        List<TextChunk> chunks = new ArrayList<>();
        int chunkSize = config.getChunkSize();
        int overlap = config.getOverlap();

        for (int i = 0; i < text.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, text.length());
            String content = text.substring(i, end);
            chunks.add(TextChunk.builder()
                    .content(content)
                    .index(chunks.size())
                    .startPos(i)
                    .endPos(end)
                    .build());

            if (end == text.length()) break;
        }
        return chunks;
    }

    @Override
    public String getName() {
        return "fixed_length";
    }
}
