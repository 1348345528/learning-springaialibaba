package com.example.rag.controller;

import com.example.rag.dto.CollectionInfo;
import com.example.rag.service.MilvusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final MilvusService milvusService;

    @GetMapping
    public CollectionInfo getCollection() {
        return milvusService.getCollectionInfo();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return milvusService.getStats();
    }

    @PostMapping("/recreate")
    public Map<String, String> recreate() {
        milvusService.dropCollection();
        milvusService.createCollectionWithSchema();
        return Map.of("status", "ok", "message", "Collection recreated");
    }

    @PostMapping("/reload")
    public Map<String, String> reload() {
        milvusService.loadCollection();
        return Map.of("status", "ok", "message", "Collection reloaded");
    }

    @PostMapping("/index/rebuild")
    public Map<String, String> rebuildIndex() {
        milvusService.createDenseIndex();
        milvusService.createSparseIndex();
        milvusService.loadCollection();
        return Map.of("status", "ok", "message", "Indexes rebuilt");
    }
}
