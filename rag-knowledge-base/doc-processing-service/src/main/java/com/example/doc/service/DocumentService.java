package com.example.doc.service;

import com.alibaba.fastjson.JSON;
import com.example.doc.dto.UploadRequest;

import java.util.Arrays;
import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import com.example.doc.dto.ChunkDto;
import com.example.doc.dto.ChunkRequest;
import com.example.doc.entity.Chunk;
import com.example.doc.parser.DocumentParser;
import com.example.doc.parser.DocumentParserFactory;
import com.example.doc.repository.ChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class DocumentService {

    private final DocumentParserFactory parserFactory;
    private final List<ChunkStrategy> chunkStrategies;
    private final ChunkRepository chunkRepository;

    @Value("${app.rag-service-url:http://localhost:8081}")
    private String ragServiceUrl;

    private WebClient getRagServiceClient() {
        return WebClient.create(ragServiceUrl);
    }

    public List<ChunkDto> uploadAndChunk(MultipartFile file, ChunkRequest request) {
        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName);

        DocumentParser parser = parserFactory.getParser(extension);
        String content;
        try {
            content = parser.parse(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse document", e);
        }

        ChunkConfig config = buildChunkConfig(request);
        ChunkStrategy strategy = getChunkStrategy(request.getStrategy());

        List<TextChunk> chunks = strategy.chunk(content, config);

        return chunks.stream().map(tc -> {
            Chunk chunk = Chunk.builder()
                    .id(UUID.randomUUID().toString())
                    .content(tc.getContent())
                    .documentName(fileName)
                    .chunkSize(tc.getContent().length())
                    .chunkIndex(tc.getIndex())
                    .strategy(strategy.getName())
                    .tags(JSON.toJSONString(tc.getTags() != null ? tc.getTags() : request.getTags()))
                    .build();
            chunkRepository.save(chunk);

            // 异步索引到 Milvus
            indexToVectorStoreAsync(chunk);

            return toDto(chunk);
        }).collect(Collectors.toList());
    }

    /**
     * 异步将 chunk 索引到 Milvus 向量数据库
     */
    @Async
    public void indexToVectorStoreAsync(Chunk chunk) {
        try {
            getRagServiceClient().post()
                    .uri("/api/vector/index")
                    .bodyValue(java.util.Map.of(
                            "id", chunk.getId(),
                            "content", chunk.getContent(),
                            "documentName", chunk.getDocumentName()
                    ))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Chunk indexed to Milvus: {}", chunk.getId());
        } catch (Exception e) {
            log.error("Failed to index chunk to vector store: {}", chunk.getId(), e);
        }
    }

    public Page<ChunkDto> getChunks(int page, int size, String keyword) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Chunk> chunkPage = keyword != null
                ? chunkRepository.findByDocumentNameContaining(keyword, pageRequest)
                : chunkRepository.findAll(pageRequest);
        return chunkPage.map(this::toDto);
    }

    public ChunkDto getChunk(String id) {
        return toDto(chunkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chunk not found: " + id)));
    }

    public ChunkDto updateChunk(String id, ChunkRequest request) {
        Chunk chunk = chunkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chunk not found: " + id));

        if (request.getContent() != null) chunk.setContent(request.getContent());
        if (request.getTags() != null) chunk.setTags(JSON.toJSONString(request.getTags()));

        chunkRepository.save(chunk);

        // 异步更新 Milvus 向量
        indexToVectorStoreAsync(chunk);

        return toDto(chunk);
    }

    public void deleteChunk(String id) {
        chunkRepository.deleteById(id);
    }

    public void deleteChunks(List<String> ids) {
        chunkRepository.deleteAllById(ids);
    }

    private ChunkConfig buildChunkConfig(ChunkRequest request) {
        ChunkConfig config = new ChunkConfig();
        if (request.getChunkSize() != null) config.setChunkSize(request.getChunkSize());
        if (request.getOverlap() != null) config.setOverlap(request.getOverlap());
        if (request.getKeepHeaders() != null) config.setKeepHeaders(request.getKeepHeaders());
        if (request.getMinParagraphLength() != null) config.setMinParagraphLength(request.getMinParagraphLength());
        if (request.getDelimiters() != null) config.setDelimiters(request.getDelimiters());
        if (request.getHeaderLevels() != null) {
            Integer[] levels = request.getHeaderLevels();
            config.setHeaderLevels(Arrays.stream(levels).mapToInt(Integer::intValue).toArray());
        }
        return config;
    }

    private ChunkStrategy getChunkStrategy(String strategyName) {
        return chunkStrategies.stream()
                .filter(s -> s.getName().equals(strategyName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown strategy: " + strategyName));
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private ChunkDto toDto(Chunk chunk) {
        return ChunkDto.builder()
                .id(chunk.getId())
                .content(chunk.getContent())
                .documentName(chunk.getDocumentName())
                .chunkSize(chunk.getChunkSize())
                .chunkIndex(chunk.getChunkIndex())
                .strategy(chunk.getStrategy())
                .tags(JSON.parseArray(chunk.getTags(), String.class))
                .createdAt(chunk.getCreatedAt())
                .updatedAt(chunk.getUpdatedAt())
                .build();
    }
}
