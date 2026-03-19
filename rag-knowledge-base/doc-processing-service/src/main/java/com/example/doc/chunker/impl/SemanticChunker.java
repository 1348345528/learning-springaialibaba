package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SemanticChunker implements ChunkStrategy {

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        List<TextChunk> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();
        int currentPos = 0;

        for (String para : paragraphs) {
            if (para.trim().length() < config.getMinParagraphLength()) {
                continue;
            }

            if (currentChunk.length() + para.length() > config.getChunkSize()) {
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(chunks.size(), currentChunk.toString(), currentPos));
                    currentPos += currentChunk.length();
                    currentChunk = new StringBuilder();
                }
            }
            currentChunk.append(para).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(createChunk(chunks.size(), currentChunk.toString(), currentPos));
        }

        return chunks;
    }

    private TextChunk createChunk(int index, String content, int pos) {
        return TextChunk.builder()
                .content(content.trim())
                .index(index)
                .startPos(pos)
                .endPos(pos + content.length())
                .build();
    }

    @Override
    public String getName() {
        return "semantic";
    }
}
