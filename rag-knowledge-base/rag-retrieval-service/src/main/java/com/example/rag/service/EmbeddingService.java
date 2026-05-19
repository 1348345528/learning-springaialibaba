package com.example.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public float[] embed(String text) {
        try {
            List<float[]> embeddings = embeddingModel.embed(List.of(text));
            if (embeddings == null || embeddings.isEmpty()) {
                throw new RuntimeException("No embeddings returned from DashScope");
            }
            float[] embedding = embeddings.get(0);
            log.debug("Generated embedding for text: {} (dimension: {})",
                    text.substring(0, Math.min(50, text.length())), embedding.length);
            return embedding;
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    public List<float[]> embed(List<String> texts) {
        return embeddingModel.embed(texts);
    }
}
