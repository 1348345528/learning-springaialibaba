package com.example.rag.controller;

import com.example.rag.dto.HybridSearchRequest;
import com.example.rag.dto.IndexRequest;
import com.example.rag.dto.SearchRequest;
import com.example.rag.dto.SearchResult;
import com.example.rag.service.VectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vector")
@RequiredArgsConstructor
public class VectorController {

    private final VectorService vectorService;

    /**
     * 插入向量：文本 → DashScope Embedding 稠密向量 → Milvus（BM25 稀疏向量由 Milvus 自动生成）
     */
    @PostMapping("/index")
    public Map<String, String> indexChunk(@Valid @RequestBody IndexRequest request) {
        vectorService.indexChunk(
                request.getId(),
                request.getContent(),
                request.getDocumentName(),
                request.getDocId(),
                request.getChunkIndex() != null ? request.getChunkIndex() : 0
        );
        return Map.of("status", "ok", "id", request.getId());
    }

    /**
     * 稠密向量检索：HNSW 索引 + COSINE 相似度，支持元数据过滤
     */
    @PostMapping("/search")
    public List<SearchResult> search(@Valid @RequestBody SearchRequest request) {
        return vectorService.search(
                request.getQuery(),
                request.getTopK(),
                request.getFilters()
        );
    }

    /**
     * 混合检索：稠密向量(HNSW) + 稀疏向量(BM25) 双路召回 → RRF/Weighted 融合
     */
    @PostMapping("/search/hybrid")
    public List<SearchResult> hybridSearch(@Valid @RequestBody HybridSearchRequest request) {
        return vectorService.hybridSearch(
                request.getQuery(),
                request.getTopK(),
                request.getDenseWeight() != null ? request.getDenseWeight() : 0.7f,
                request.getSparseWeight() != null ? request.getSparseWeight() : 0.3f,
                request.getRrfK() != null ? request.getRrfK() : 60,
                request.getUseRrf() != null ? request.getUseRrf() : true
        );
    }

    /**
     * 全流程检索：HyDE 查询扩展 → 混合检索(HNSW+BM25) → RRF 融合 → DashScope gte-rerank 精排
     */
    @PostMapping("/search/full")
    public List<SearchResult> fullPipelineSearch(@Valid @RequestBody HybridSearchRequest request) {
        return vectorService.fullPipelineSearch(
                request.getQuery(),
                request.getTopK(),
                request.getUseHyde() != null && request.getUseHyde(),
                request.getUseRerank() == null || request.getUseRerank(),
                request.getRerankTopN() != null ? request.getRerankTopN() : 20
        );
    }
}
