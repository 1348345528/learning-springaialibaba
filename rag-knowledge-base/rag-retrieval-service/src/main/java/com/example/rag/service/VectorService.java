package com.example.rag.service;

import com.example.rag.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorService {

    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;

    public void indexChunk(String id, String content, String documentName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or empty");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or empty");
        }
        if (documentName == null) {
            documentName = "";
        }

        float[] embedding = embeddingService.embed(content);
        milvusService.insert(id, embedding, content, documentName);
    }

    public List<SearchResult> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query cannot be null or empty");
        }
        if (topK <= 0) {
            topK = 5;
        }

        float[] queryEmbedding = embeddingService.embed(query);
        return milvusService.search(queryEmbedding, topK);
    }
}
