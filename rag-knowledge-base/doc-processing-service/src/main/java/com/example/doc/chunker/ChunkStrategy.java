package com.example.doc.chunker;

import java.util.List;

public interface ChunkStrategy {
    List<TextChunk> chunk(String text, ChunkConfig config);

    String getName();
}
