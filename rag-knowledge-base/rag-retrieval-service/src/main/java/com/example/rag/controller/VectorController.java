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

    @PostMapping("/search")
    public List<SearchResult> search(@Valid @RequestBody SearchRequest request) {
        return vectorService.search(
                request.getQuery(),
                request.getTopK(),
                request.getFilters()
        );
    }

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
