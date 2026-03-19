package com.example.rag.controller;

import com.example.rag.dto.IndexRequest;
import com.example.rag.dto.SearchRequest;
import com.example.rag.dto.SearchResult;
import com.example.rag.service.VectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vector")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VectorController {

    private final VectorService vectorService;

    @PostMapping("/index")
    public void indexChunk(@Valid @RequestBody IndexRequest request) {
        vectorService.indexChunk(
                request.getId(),
                request.getContent(),
                request.getDocumentName()
        );
    }

    @PostMapping("/search")
    public List<SearchResult> search(@Valid @RequestBody SearchRequest request) {
        return vectorService.search(request.getQuery(), request.getTopK());
    }
}
