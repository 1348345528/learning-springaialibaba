package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CustomRuleChunker implements ChunkStrategy {

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        List<TextChunk> chunks = new ArrayList<>();
        String delimiter = config.getDelimiters()[0];
        String[] parts = text.split(Pattern.quote(delimiter));

        StringBuilder currentChunk = new StringBuilder();
        int currentPos = 0;

        for (String part : parts) {
            if (currentChunk.length() + part.length() > config.getChunkSize() && currentChunk.length() > 0) {
                chunks.add(TextChunk.builder()
                        .content(currentChunk.toString().trim())
                        .index(chunks.size())
                        .startPos(currentPos)
                        .endPos(currentPos + currentChunk.length())
                        .build());
                currentPos += currentChunk.length();
                currentChunk = new StringBuilder();
            }
            currentChunk.append(part).append(delimiter);
        }

        if (currentChunk.length() > 0) {
            chunks.add(TextChunk.builder()
                    .content(currentChunk.toString().trim())
                    .index(chunks.size())
                    .startPos(currentPos)
                    .endPos(currentPos + currentChunk.length())
                    .build());
        }

        return chunks;
    }

    @Override
    public String getName() {
        return "custom_rule";
    }
}
