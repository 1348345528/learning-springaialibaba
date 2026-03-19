package com.example.doc.chunker;

import lombok.Data;

@Data
public class ChunkConfig {
    private int chunkSize = 500;
    private int overlap = 50;
    private boolean keepHeaders = true;
    private int minParagraphLength = 50;
    private String[] delimiters = new String[]{"\n\n", "\n", ". "};
    private int[] headerLevels = new int[]{1, 2, 3};
}
