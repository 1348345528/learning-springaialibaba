package com.example.rag.doc.service;

import com.alibaba.fastjson.JSON;
import com.example.rag.doc.chunker.ChunkConfig;
import com.example.rag.doc.chunker.ChunkStrategy;
import com.example.rag.doc.chunker.TextChunk;
import com.example.rag.doc.dto.ChunkDto;
import com.example.rag.doc.dto.ChunkRequest;
import com.example.rag.entity.Chunk;
import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.alibaba.cloud.ai.reader.poi.PoiDocumentReader;
import com.example.rag.repository.ChunkRepository;
import com.example.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final ChunkRepository chunkRepository;
    private final Map<String, ChunkStrategy> strategyMap;
    private final VectorService vectorService;

    public List<ChunkDto> uploadAndChunk(MultipartFile file, ChunkRequest request) {
        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName);

        List<Document> documents;
        try {
            if (isOfficeFile(extension)) {
                PoiDocumentReader reader = new PoiDocumentReader(file.getResource());
                documents = reader.read();
            } else {
                TikaDocumentParser parser = new TikaDocumentParser();
                documents = parser.parse(file.getInputStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read document", e);
        }
        String content = documents.stream()
                .map(Document::getText)
                .filter(t -> t != null && !Objects.isNull(t))
                .collect(Collectors.joining("\n"));

        if (content.isEmpty()) {
            throw new RuntimeException("Document content is empty");
        }

        return processContent(content, fileName, request);
    }

    private boolean isOfficeFile(String extension) {
        return Set.of("docx", "doc", "xlsx", "xls", "pptx", "ppt").contains(extension.toLowerCase());
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 通过 ChunkStrategy 接口统一调度，Spring 自动发现所有 @Component 实现。
     */
    public List<ChunkDto> processContent(String content, String documentName, ChunkRequest request) {
        ChunkStrategy chunker = strategyMap.get(request.getStrategy());
        if (chunker == null) {
            throw new IllegalArgumentException("不支持的分块策略: " + request.getStrategy()
                    + "，可用策略: " + strategyMap.keySet());
        }

        List<TextChunk> chunks = chunker.chunk(content, buildConfig(request));
        return saveAndIndexChunks(chunks, documentName, request.getStrategy(), request.getTags());
    }

    private ChunkConfig buildConfig(ChunkRequest request) {
        ChunkConfig config = new ChunkConfig();
        if (request.getChunkSize() != null) config.setChunkSize(request.getChunkSize());
        if (request.getOverlap() != null) config.setOverlap(request.getOverlap());
        if (request.getKeepHeaders() != null) config.setKeepHeaders(request.getKeepHeaders());
        if (request.getMinParagraphLength() != null) config.setMinParagraphLength(request.getMinParagraphLength());
        if (request.getDelimiters() != null) config.setDelimiters(request.getDelimiters());
        if (request.getHeaderLevels() != null) {
            config.setHeaderLevels(Arrays.stream(request.getHeaderLevels()).mapToInt(Integer::intValue).toArray());
        }
        return config;
    }

    private List<ChunkDto> saveAndIndexChunks(List<TextChunk> chunks, String documentName,
                                               String strategy, List<String> tags) {
        Map<Integer, String> parentIndexToId = new HashMap<>();
        String docId = UUID.randomUUID().toString();

        return chunks.stream().map(tc -> {
            Integer parentIndex = extractParentIndex(tc.getTags());
            Integer localIndex = extractLocalIndex(tc.getTags());
            boolean isHierarchical = isHierarchicalChunk(tc.getTags());

            Chunk chunk = Chunk.builder()
                    .id(UUID.randomUUID().toString())
                    .content(tc.getContent())
                    .documentName(documentName)
                    .chunkSize(tc.getContent().length())
                    .chunkIndex(tc.getIndex())
                    .strategy(strategy)
                    .startPos(tc.getStartPos())
                    .endPos(tc.getEndPos())
                    .tags(JSON.toJSONString(mergeTags(tc.getTags(), tags)))
                    .parentIndex(parentIndex)
                    .localIndex(localIndex)
                    .isChild(isHierarchical && parentIndex != null)
                    .isParent(false)
                    .build();

            if (parentIndex != null && isHierarchical) {
                String parentId = parentIndexToId.get(parentIndex);
                if (parentId == null) {
                    parentId = UUID.randomUUID().toString();
                    parentIndexToId.put(parentIndex, parentId);
                }
                chunk.setParentId(parentId);
            }

            chunkRepository.save(chunk);

            if (!isHierarchical || parentIndex == null) {
                parentIndexToId.put(tc.getIndex(), chunk.getId());
            }

            indexToVectorStoreAsync(chunk, docId);

            return toDto(chunk);
        }).collect(Collectors.toList());
    }

    private Integer extractParentIndex(List<String> tags) {
        if (tags == null) return null;
        for (String tag : tags) {
            if (tag.startsWith("parent_index:")) {
                return Integer.parseInt(tag.substring("parent_index:".length()));
            }
        }
        return null;
    }

    private Integer extractLocalIndex(List<String> tags) {
        if (tags == null) return null;
        for (String tag : tags) {
            if (tag.startsWith("child_local_index:")) {
                return Integer.parseInt(tag.substring("child_local_index:".length()));
            }
        }
        return null;
    }

    private boolean isHierarchicalChunk(List<String> tags) {
        if (tags == null) return false;
        return tags.stream().anyMatch(tag -> tag.equals("hierarchical:true"));
    }

    private List<String> mergeTags(List<String> chunkTags, List<String> requestTags) {
        List<String> result = new ArrayList<>();
        if (chunkTags != null) result.addAll(chunkTags);
        if (requestTags != null) {
            for (String tag : requestTags) {
                if (!result.contains(tag)) result.add(tag);
            }
        }
        return result;
    }

    /**
     * 直接调用 VectorService 索引到 Milvus，无需 HTTP 中转。
     */
    @Async
    public void indexToVectorStoreAsync(Chunk chunk, String docId) {
        try {
            vectorService.indexChunk(
                    chunk.getId(),
                    chunk.getContent(),
                    chunk.getDocumentName(),
                    docId,
                    chunk.getChunkIndex() != null ? chunk.getChunkIndex() : 0
            );
            log.info("Chunk indexed to Milvus: {}", chunk.getId());
        } catch (Exception e) {
            log.error("Failed to index chunk to vector store: {}", chunk.getId(), e);
        }
    }

    // ==================== Chunk CRUD ====================

    public Page<ChunkDto> getChunks(int page, int size, String keyword) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Chunk> chunkPage = keyword != null && !keyword.trim().isEmpty()
                ? chunkRepository.findByDocumentNameContaining(keyword, pageRequest)
                : chunkRepository.findAll(pageRequest);
        return chunkPage.map(this::toDto);
    }

    public List<String> getDocumentNames() {
        return chunkRepository.findDistinctDocumentNames();
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
        indexToVectorStoreAsync(chunk, chunk.getId());

        return toDto(chunk);
    }

    public void deleteChunk(String id) {
        chunkRepository.deleteById(id);
    }

    public void deleteChunks(List<String> ids) {
        chunkRepository.deleteAllById(ids);
    }

    private ChunkDto toDto(Chunk chunk) {
        return ChunkDto.builder()
                .id(chunk.getId())
                .content(chunk.getContent())
                .documentName(chunk.getDocumentName())
                .chunkSize(chunk.getChunkSize())
                .chunkIndex(chunk.getChunkIndex())
                .strategy(chunk.getStrategy())
                .startPos(chunk.getStartPos())
                .endPos(chunk.getEndPos())
                .parentId(chunk.getParentId())
                .parentIndex(chunk.getParentIndex())
                .localIndex(chunk.getLocalIndex())
                .isParent(chunk.getIsParent())
                .isChild(chunk.getIsChild())
                .tags(JSON.parseArray(chunk.getTags(), String.class))
                .metadata(chunk.getMetadata() != null
                        ? JSON.parseObject(chunk.getMetadata(), Map.class)
                        : null)
                .createdAt(chunk.getCreatedAt())
                .updatedAt(chunk.getUpdatedAt())
                .build();
    }
}
