package com.example.rag.service;

import com.example.rag.dto.SearchResult;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorService {

    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;
    private final RerankerService rerankerService;
    private final HydeService hydeService;

    // ==================== Indexing ====================

    public void indexChunk(String id, String content, String documentName,
                           String docId, int chunkIndex) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or empty");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or empty");
        }

        float[] embedding = embeddingService.embed(content);
        milvusService.insert(id, embedding, content,
                documentName != null ? documentName : "",
                docId != null ? docId : "",
                chunkIndex);
    }

    public void indexChunks(List<MilvusService.InsertData> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        // Batch embed
        List<String> texts = dataList.stream()
                .map(d -> d.content())
                .collect(Collectors.toList());
        List<float[]> embeddings = embeddingService.embed(texts);

        // Attach embeddings to data
        List<MilvusService.InsertData> enriched = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            MilvusService.InsertData d = dataList.get(i);
            enriched.add(new MilvusService.InsertData(
                    d.id(), embeddings.get(i), d.content(),
                    d.documentName(), d.docId(), d.chunkIndex()));
        }
        milvusService.batchInsert(enriched);
    }

    // ==================== Dense-Only Search ====================

    public List<SearchResult> search(String query, int topK, Map<String, String> filters) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query cannot be null or empty");
        }
        if (topK <= 0) topK = 5;

        float[] queryEmbedding = embeddingService.embed(query);
        List<SearchResp.SearchResult> results = milvusService.denseSearch(queryEmbedding, topK, filters);

        return results.stream()
                .map(r -> SearchResult.builder()
                        .id(String.valueOf(r.getId()))
                        .score(r.getScore())
                        .denseScore(r.getScore())
                        .content(String.valueOf(r.getEntity().getOrDefault("content", "")))
                        .documentName(String.valueOf(r.getEntity().getOrDefault("document_name", "")))
                        .docId(String.valueOf(r.getEntity().getOrDefault("doc_id", "")))
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== Hybrid Search (Dense + Sparse + RRF) ====================

    public List<SearchResult> hybridSearch(String query, int topK,
                                            float denseWeight, float sparseWeight,
                                            int rrfK, boolean useRrf) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query cannot be null or empty");
        }
        if (topK <= 0) topK = 5;

        float[] queryEmbedding = embeddingService.embed(query);
        List<SearchResp.SearchResult> results = milvusService.hybridSearch(
                queryEmbedding, query, topK * 2, denseWeight, sparseWeight, rrfK, useRrf);

        return results.stream()
                .map(r -> SearchResult.builder()
                        .id(String.valueOf(r.getId()))
                        .score(r.getScore())
                        .content(String.valueOf(r.getEntity().getOrDefault("content", "")))
                        .documentName(String.valueOf(r.getEntity().getOrDefault("document_name", "")))
                        .docId(String.valueOf(r.getEntity().getOrDefault("doc_id", "")))
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== Full Pipeline: Hybrid + Rerank + HyDE ====================

    public List<SearchResult> fullPipelineSearch(String query, int topK,
                                                  boolean useHyde, boolean useRerank,
                                                  int rerankTopN) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query cannot be null or empty");
        }
        if (topK <= 0) topK = 5;

        // Step 1: HyDE query expansion (optional)
        String searchQuery = query;
        if (useHyde) {
            try {
                searchQuery = hydeService.generateHypotheticalDocument(query);
                log.debug("HyDE generated query: {}", searchQuery.substring(0, Math.min(200, searchQuery.length())));
            } catch (Exception e) {
                log.warn("HyDE generation failed, falling back to original query: {}", e.getMessage());
            }
        }

        // Step 2: Hybrid search (dense + sparse)
        float[] queryEmbedding = embeddingService.embed(searchQuery);
        int candidateTopK = useRerank ? rerankTopN : topK;
        List<SearchResp.SearchResult> hybridResults = milvusService.hybridSearch(
                queryEmbedding, searchQuery, candidateTopK, 0.7f, 0.3f, 60, true);

        // Step 3: Rerank (optional)
        if (useRerank && hybridResults.size() > topK) {
            return rerankResults(query, hybridResults, topK);
        }

        return hybridResults.stream()
                .limit(topK)
                .map(r -> SearchResult.builder()
                        .id(String.valueOf(r.getId()))
                        .score(r.getScore())
                        .content(String.valueOf(r.getEntity().getOrDefault("content", "")))
                        .documentName(String.valueOf(r.getEntity().getOrDefault("document_name", "")))
                        .docId(String.valueOf(r.getEntity().getOrDefault("doc_id", "")))
                        .build())
                .collect(Collectors.toList());
    }

    private List<SearchResult> rerankResults(String query,
                                              List<SearchResp.SearchResult> candidates,
                                              int topK) {
        List<String> documents = candidates.stream()
                .map(r -> String.valueOf(r.getEntity().getOrDefault("content", "")))
                .collect(Collectors.toList());

        List<String> docIds = candidates.stream()
                .map(r -> String.valueOf(r.getEntity().getOrDefault("doc_id", "")))
                .collect(Collectors.toList());

        List<RerankerService.RerankResult> reranked = rerankerService.rerank(query, documents, topK);

        return reranked.stream()
                .map(rr -> {
                    int idx = rr.index();
                    SearchResp.SearchResult original = candidates.get(idx);
                    return SearchResult.builder()
                            .id(String.valueOf(original.getId()))
                            .score(rr.score())
                            .content(rr.document())
                            .documentName(String.valueOf(original.getEntity().getOrDefault("document_name", "")))
                            .docId(idx < docIds.size() ? docIds.get(idx) : "")
                            .build();
                })
                .collect(Collectors.toList());
    }
}
