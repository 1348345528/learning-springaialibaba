package com.example.doc.service;

import com.alibaba.fastjson.JSON;
import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import com.example.doc.chunker.config.HierarchicalChunkConfig;
import com.example.doc.chunker.config.RecursiveChunkConfig;
import com.example.doc.chunker.config.SemanticChunkConfig;
import com.example.doc.chunker.impl.HierarchicalChunker;
import com.example.doc.chunker.impl.RecursiveChunker;
import com.example.doc.chunker.impl.TrueSemanticChunker;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档服务类
 * <p>
 * 提供文档上传、分块、检索和管理功能。
 * 支持多种分块策略，包括递归分块、语义分块和分层分块。
 * </p>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class DocumentService {

    private final DocumentParserFactory parserFactory;
    private final List<ChunkStrategy> chunkStrategies;
    private final ChunkRepository chunkRepository;
    private final RecursiveChunker recursiveChunker;
    private final TrueSemanticChunker trueSemanticChunker;
    private final HierarchicalChunker hierarchicalChunker;

    @Value("${app.rag-service-url:http://localhost:8081}")
    private String ragServiceUrl;

    private WebClient getRagServiceClient() {
        return WebClient.create(ragServiceUrl);
    }

    /**
     * 上传文档并进行分块处理
     *
     * @param file    上传的文件
     * @param request 分块请求参数
     * @return 分块结果列表
     */
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

        return processContent(content, fileName, request);
    }

    /**
     * 处理文本内容并进行分块
     *
     * @param content      文本内容
     * @param documentName 文档名称
     * @param request      分块请求参数
     * @return 分块结果列表
     */
    public List<ChunkDto> processContent(String content, String documentName, ChunkRequest request) {
        String strategy = request.getStrategy();
        List<TextChunk> chunks;

        // 根据策略类型选择合适的分块器
        switch (strategy) {
            case "recursive":
                chunks = chunkWithRecursive(content, request);
                break;
            case "true_semantic":
                chunks = chunkWithTrueSemantic(content, request);
                break;
            case "hierarchical":
                chunks = chunkWithHierarchical(content, request);
                break;
            default:
                // 使用传统分块策略
                ChunkConfig config = buildChunkConfig(request);
                ChunkStrategy chunkStrategy = getChunkStrategy(strategy);
                chunks = chunkStrategy.chunk(content, config);
        }

        return saveAndIndexChunks(chunks, documentName, strategy, request.getTags());
    }

    /**
     * 使用递归分块器进行分块
     *
     * @param content 文本内容
     * @param request 分块请求
     * @return 分块列表
     */
    private List<TextChunk> chunkWithRecursive(String content, ChunkRequest request) {
        RecursiveChunkConfig config = buildRecursiveConfig(request);
        return recursiveChunker.chunk(content, config);
    }

    /**
     * 使用语义分块器进行分块
     *
     * @param content 文本内容
     * @param request 分块请求
     * @return 分块列表
     */
    private List<TextChunk> chunkWithTrueSemantic(String content, ChunkRequest request) {
        SemanticChunkConfig config = buildSemanticConfig(request);
        return trueSemanticChunker.chunk(content, config);
    }

    /**
     * 使用分层分块器进行分块
     *
     * @param content 文本内容
     * @param request 分块请求
     * @return 分块列表
     */
    private List<TextChunk> chunkWithHierarchical(String content, ChunkRequest request) {
        HierarchicalChunkConfig config = buildHierarchicalConfig(request);
        return hierarchicalChunker.chunk(content, config);
    }

    /**
     * 保存分块并索引到向量数据库
     *
     * @param chunks       分块列表
     * @param documentName 文档名称
     * @param strategy     分块策略
     * @param tags         标签
     * @return 分块 DTO 列表
     */
    private List<ChunkDto> saveAndIndexChunks(List<TextChunk> chunks, String documentName,
                                               String strategy, List<String> tags) {
        // 用于存储父子块关系
        Map<Integer, String> parentIndexToId = new HashMap<>();

        return chunks.stream().map(tc -> {
            // 从标签中提取父子块关系信息
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
                    .isParent(false) // 暂时设为 false，后续可更新
                    .build();

            // 如果是分层分块的子块，设置父块 ID
            if (parentIndex != null && isHierarchical) {
                String parentId = parentIndexToId.get(parentIndex);
                if (parentId == null) {
                    // 创建父块记录（如果不存在）
                    parentId = UUID.randomUUID().toString();
                    parentIndexToId.put(parentIndex, parentId);
                }
                chunk.setParentId(parentId);
            }

            chunkRepository.save(chunk);

            // 更新父块 ID 映射
            if (!isHierarchical || parentIndex == null) {
                parentIndexToId.put(tc.getIndex(), chunk.getId());
            }

            // 异步索引到 Milvus
            indexToVectorStoreAsync(chunk);

            return toDto(chunk);
        }).collect(Collectors.toList());
    }

    /**
     * 从标签中提取父块索引
     */
    private Integer extractParentIndex(List<String> tags) {
        if (tags == null) return null;
        for (String tag : tags) {
            if (tag.startsWith("parent_index:")) {
                return Integer.parseInt(tag.substring("parent_index:".length()));
            }
        }
        return null;
    }

    /**
     * 从标签中提取局部索引
     */
    private Integer extractLocalIndex(List<String> tags) {
        if (tags == null) return null;
        for (String tag : tags) {
            if (tag.startsWith("child_local_index:")) {
                return Integer.parseInt(tag.substring("child_local_index:".length()));
            }
        }
        return null;
    }

    /**
     * 判断是否为分层分块
     */
    private boolean isHierarchicalChunk(List<String> tags) {
        if (tags == null) return false;
        return tags.stream().anyMatch(tag -> tag.equals("hierarchical:true"));
    }

    /**
     * 合并标签
     */
    private List<String> mergeTags(List<String> chunkTags, List<String> requestTags) {
        List<String> result = new ArrayList<>();
        if (chunkTags != null) {
            result.addAll(chunkTags);
        }
        if (requestTags != null) {
            // 过滤掉已存在的标签
            for (String tag : requestTags) {
                if (!result.contains(tag)) {
                    result.add(tag);
                }
            }
        }
        return result;
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

    /**
     * 获取分块列表（分页）
     */
    public Page<ChunkDto> getChunks(int page, int size, String keyword) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Chunk> chunkPage = keyword != null && !keyword.trim().isEmpty()
                ? chunkRepository.findByDocumentNameContaining(keyword, pageRequest)
                : chunkRepository.findAll(pageRequest);
        return chunkPage.map(this::toDto);
    }

    /**
     * 获取所有文档名称
     */
    public List<String> getDocumentNames() {
        return chunkRepository.findDistinctDocumentNames();
    }

    /**
     * 获取单个分块
     */
    public ChunkDto getChunk(String id) {
        return toDto(chunkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chunk not found: " + id)));
    }

    /**
     * 更新分块
     */
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

    /**
     * 删除单个分块
     */
    public void deleteChunk(String id) {
        chunkRepository.deleteById(id);
    }

    /**
     * 批量删除分块
     */
    public void deleteChunks(List<String> ids) {
        chunkRepository.deleteAllById(ids);
    }

    /**
     * 构建通用分块配置
     */
    private ChunkConfig buildChunkConfig(ChunkRequest request) {
        ChunkConfig config = new ChunkConfig();
        if (request.getChunkSize() != null) config.setChunkSize(request.getChunkSize());
        if (request.getOverlap() != null) config.setOverlap(request.getOverlap());
        if (request.getKeepHeaders() != null) config.setKeepHeaders(request.getKeepHeaders());
        if (request.getMinParagraphLength() != null)
            config.setMinParagraphLength(request.getMinParagraphLength());
        if (request.getDelimiters() != null) config.setDelimiters(request.getDelimiters());
        if (request.getHeaderLevels() != null) {
            Integer[] levels = request.getHeaderLevels();
            config.setHeaderLevels(Arrays.stream(levels).mapToInt(Integer::intValue).toArray());
        }
        return config;
    }

    /**
     * 构建递归分块配置
     */
    private RecursiveChunkConfig buildRecursiveConfig(ChunkRequest request) {
        RecursiveChunkConfig.RecursiveChunkConfigBuilder builder = RecursiveChunkConfig.builder();

        if (request.getChunkSize() != null) builder.chunkSize(request.getChunkSize());
        if (request.getOverlap() != null) builder.overlap(request.getOverlap());
        if (request.getMinChunkSize() != null) builder.minChunkSize(request.getMinChunkSize());
        if (request.getKeepSeparator() != null) builder.keepSeparator(request.getKeepSeparator());
        if (request.getTrimWhitespace() != null) builder.trimWhitespace(request.getTrimWhitespace());
        if (request.getDelimiters() != null)
            builder.separators(Arrays.asList(request.getDelimiters()));

        return builder.build();
    }

    /**
     * 构建语义分块配置
     */
    private SemanticChunkConfig buildSemanticConfig(ChunkRequest request) {
        SemanticChunkConfig.SemanticChunkConfigBuilder builder = SemanticChunkConfig.builder();

        if (request.getSimilarityThreshold() != null)
            builder.similarityThreshold(request.getSimilarityThreshold());
        if (request.getPercentileThreshold() != null)
            builder.percentileThreshold(request.getPercentileThreshold());
        if (request.getUseDynamicThreshold() != null)
            builder.useDynamicThreshold(request.getUseDynamicThreshold());
        if (request.getBreakpointMethod() != null) {
            try {
                builder.breakpointMethod(
                        SemanticChunkConfig.BreakpointMethod.valueOf(request.getBreakpointMethod()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid breakpoint method: {}, using default", request.getBreakpointMethod());
            }
        }
        if (request.getMaxChunkSize() != null) builder.maxChunkSize(request.getMaxChunkSize());
        if (request.getMinParagraphLength() != null)
            builder.minChunkSize(request.getMinParagraphLength());

        return builder.build();
    }

    /**
     * 构建分层分块配置
     */
    private HierarchicalChunkConfig buildHierarchicalConfig(ChunkRequest request) {
        HierarchicalChunkConfig.HierarchicalChunkConfigBuilder builder = HierarchicalChunkConfig.builder();

        if (request.getParentChunkSize() != null)
            builder.parentChunkSize(request.getParentChunkSize());
        if (request.getChildChunkSize() != null)
            builder.childChunkSize(request.getChildChunkSize());
        if (request.getChildOverlap() != null)
            builder.childOverlap(request.getChildOverlap());
        if (request.getChildSplitStrategy() != null) {
            try {
                builder.childSplitStrategy(
                        HierarchicalChunkConfig.ChildSplitStrategy.valueOf(request.getChildSplitStrategy()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid child split strategy: {}, using default", request.getChildSplitStrategy());
            }
        }
        if (request.getDelimiters() != null)
            builder.childSeparators(request.getDelimiters());
        if (request.getKeepSeparator() != null)
            builder.keepSeparator(request.getKeepSeparator());
        if (request.getStoreParentReference() != null)
            builder.storeParentReference(request.getStoreParentReference());

        HierarchicalChunkConfig config = builder.build();
        try {
            config.validate();
        } catch (IllegalArgumentException e) {
            log.error("Invalid hierarchical config: {}", e.getMessage());
            throw e;
        }

        return config;
    }

    /**
     * 获取分块策略
     */
    private ChunkStrategy getChunkStrategy(String strategyName) {
        return chunkStrategies.stream()
                .filter(s -> s.getName().equals(strategyName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown strategy: " + strategyName));
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 转换为 DTO
     */
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
