package com.example.doc.controller;

import com.example.doc.dto.ChunkDto;
import com.example.doc.dto.ChunkRequest;
import com.example.doc.dto.UploadRequest;
import com.example.doc.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/doc")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public List<ChunkDto> upload(@ModelAttribute UploadRequest uploadRequest) {
        ChunkRequest request = new ChunkRequest();
        request.setStrategy(uploadRequest.getStrategy());
        request.setChunkSize(uploadRequest.getChunkSize());
        request.setOverlap(uploadRequest.getOverlap());
        request.setKeepHeaders(uploadRequest.getKeepHeaders());
        request.setMinParagraphLength(uploadRequest.getMinParagraphLength());
        request.setDelimiters(uploadRequest.getDelimiters());
        request.setHeaderLevels(uploadRequest.getHeaderLevels());
        request.setTags(uploadRequest.getTags());
        return documentService.uploadAndChunk(uploadRequest.getFile(), request);
    }

    @GetMapping("/chunks")
    public Page<ChunkDto> getChunks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return documentService.getChunks(page, size, keyword);
    }

    @GetMapping("/documents")
    public List<String> getDocumentNames() {
        return documentService.getDocumentNames();
    }

    @GetMapping("/chunks/{id}")
    public ChunkDto getChunk(@PathVariable String id) {
        return documentService.getChunk(id);
    }

    @PutMapping("/chunks/{id}")
    public ChunkDto updateChunk(@PathVariable String id, @RequestBody ChunkRequest request) {
        return documentService.updateChunk(id, request);
    }

    @DeleteMapping("/chunks/{id}")
    public void deleteChunk(@PathVariable String id) {
        documentService.deleteChunk(id);
    }

    @DeleteMapping("/chunks/batch")
    public void deleteChunks(@RequestBody List<String> ids) {
        documentService.deleteChunks(ids);
    }
}
