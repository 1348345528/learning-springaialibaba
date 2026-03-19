package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HybridChunker implements ChunkStrategy {

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        // First do semantic chunking
        SemanticChunker semanticChunker = new SemanticChunker();
        List<TextChunk> semanticChunks = semanticChunker.chunk(text, config);

        List<TextChunk> result = new ArrayList<>();
        int index = 0;

        for (TextChunk chunk : semanticChunks) {
            if (chunk.getContent().length() > config.getChunkSize()) {
                // Do fixed-length chunking for oversized chunks
                FixedLengthChunker fixedChunker = new FixedLengthChunker();
                for (TextChunk subChunk : fixedChunker.chunk(chunk.getContent(), config)) {
                    subChunk.setIndex(index++);
                    result.add(subChunk);
                }
            } else {
                chunk.setIndex(index++);
                result.add(chunk);
            }
        }

        return result;
    }

    @Override
    public String getName() {
        return "hybrid";
    }
}
