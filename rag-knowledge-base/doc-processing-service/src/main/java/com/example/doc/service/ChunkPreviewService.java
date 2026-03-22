package com.example.doc.service;

import com.alibaba.fastjson.JSON;
import com.example.doc.chunker.TextChunk;
import com.example.doc.chunker.config.HierarchicalChunkConfig;
import com.example.doc.chunker.config.RecursiveChunkConfig;
import com.example.doc.chunker.config.SemanticChunkConfig;
import com.example.doc.chunker.impl.HierarchicalChunker;
import com.example.doc.chunker.impl.RecursiveChunker;
import com.example.doc.chunker.impl.TrueSemanticChunker;
import com.example.doc.dto.ChunkPreviewRequest;
import com.example.doc.dto.ChunkPreviewResponse;
import com.example.doc.dto.ChunkPreviewResponse.ChunkPreviewItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分块预览服务
 * <p>
 * 提供分块预览功能，允许用户在不实际保存分块的情况下查看分块效果。
 * 支持所有分块策略的预览，并提供详细的统计信息。
 * </p>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkPreviewService {

    private final RecursiveChunker recursiveChunker;
    private final TrueSemanticChunker trueSemanticChunker;
    private final HierarchicalChunker hierarchicalChunker;

    /**
     * 预览分块效果
     *
     * @param request 预览请求
     * @return 预览响应
     */
    public ChunkPreviewResponse preview(ChunkPreviewRequest request) {
        String content = request.getContent();
        String strategy = request.getStrategy();

        log.info("开始分块预览，策略: {}, 文本长度: {}", strategy, content.length());

        List<TextChunk> chunks;
        Map<String, Object> statistics = new HashMap<>();

        // 根据策略进行分块
        switch (strategy.toLowerCase()) {
            case "recursive":
                chunks = previewRecursive(content, request, statistics);
                break;
            case "true_semantic":
                chunks = previewTrueSemantic(content, request, statistics);
                break;
            case "hierarchical":
                chunks = previewHierarchical(content, request, statistics);
                break;
            case "fixed_length":
                chunks = previewFixedLength(content, request, statistics);
                break;
            case "hybrid":
                // 混合分块：使用递归分块作为基础
                chunks = previewRecursive(content, request, statistics);
                break;
            case "custom_rule":
                // 自定义规则：使用递归分块，支持自定义分隔符
                chunks = previewRecursive(content, request, statistics);
                break;
            default:
                throw new IllegalArgumentException("不支持的分块策略: " + strategy);
        }

        // 构建响应
        return buildResponse(content, strategy, chunks, statistics);
    }

    /**
     * 使用递归分块策略预览
     */
    private List<TextChunk> previewRecursive(String content, ChunkPreviewRequest request,
                                              Map<String, Object> statistics) {
        RecursiveChunkConfig config = buildRecursiveConfig(request);
        statistics.put("config", Map.of(
                "chunkSize", config.getChunkSize(),
                "overlap", config.getOverlap(),
                "minChunkSize", config.getMinChunkSize(),
                "keepSeparator", config.getKeepSeparator(),
                "separators", config.getEffectiveSeparators()
        ));
        return recursiveChunker.chunk(content, config);
    }

    /**
     * 使用语义分块策略预览
     */
    private List<TextChunk> previewTrueSemantic(String content, ChunkPreviewRequest request,
                                                 Map<String, Object> statistics) {
        SemanticChunkConfig config = buildSemanticConfig(request);
        statistics.put("config", Map.of(
                "similarityThreshold", config.getSimilarityThreshold(),
                "percentileThreshold", config.getPercentileThreshold(),
                "breakpointMethod", config.getBreakpointMethod().name(),
                "useDynamicThreshold", config.getUseDynamicThreshold()
        ));
        return trueSemanticChunker.chunk(content, config);
    }

    /**
     * 使用分层分块策略预览
     */
    private List<TextChunk> previewHierarchical(String content, ChunkPreviewRequest request,
                                                 Map<String, Object> statistics) {
        HierarchicalChunkConfig config = buildHierarchicalConfig(request);
        config.validate();
        statistics.put("config", Map.of(
                "parentChunkSize", config.getParentChunkSize(),
                "childChunkSize", config.getChildChunkSize(),
                "childOverlap", config.getChildOverlap(),
                "childSplitStrategy", config.getChildSplitStrategy().name()
        ));
        return hierarchicalChunker.chunk(content, config);
    }

    /**
     * 使用固定长度分块策略预览
     */
    private List<TextChunk> previewFixedLength(String content, ChunkPreviewRequest request,
                                                Map<String, Object> statistics) {
        int chunkSize = request.getChunkSize() != null ? request.getChunkSize() : 500;
        int overlap = request.getOverlap() != null ? request.getOverlap() : 0;

        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            String chunkContent = content.substring(start, end);

            TextChunk chunk = TextChunk.builder()
                    .content(chunkContent)
                    .index(index)
                    .startPos(start)
                    .endPos(end)
                    .build();
            chunks.add(chunk);

            start = end - overlap;
            if (start <= chunks.get(chunks.size() - 1).getStartPos()) {
                start = end; // 避免无限循环
            }
            index++;
        }

        statistics.put("config", Map.of(
                "chunkSize", chunkSize,
                "overlap", overlap,
                "totalChunks", chunks.size()
        ));

        return chunks;
    }

    /**
     * 构建递归分块配置
     */
    private RecursiveChunkConfig buildRecursiveConfig(ChunkPreviewRequest request) {
        RecursiveChunkConfig.RecursiveChunkConfigBuilder builder = RecursiveChunkConfig.builder();

        if (request.getChunkSize() != null) builder.chunkSize(request.getChunkSize());
        if (request.getOverlap() != null) builder.overlap(request.getOverlap());
        if (request.getMinChunkSize() != null) builder.minChunkSize(request.getMinChunkSize());
        if (request.getKeepSeparator() != null) builder.keepSeparator(request.getKeepSeparator());

        if (request.getSeparators() != null && !request.getSeparators().isEmpty()) {
            builder.separators(request.getSeparators());
        }

        return builder.build();
    }

    /**
     * 构建语义分块配置
     */
    private SemanticChunkConfig buildSemanticConfig(ChunkPreviewRequest request) {
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
                log.warn("无效的断点检测方法: {}", request.getBreakpointMethod());
            }
        }
        if (request.getMaxChunkSize() != null) builder.maxChunkSize(request.getMaxChunkSize());

        return builder.build();
    }

    /**
     * 构建分层分块配置
     */
    private HierarchicalChunkConfig buildHierarchicalConfig(ChunkPreviewRequest request) {
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
                log.warn("无效的子块分割策略: {}", request.getChildSplitStrategy());
            }
        }

        return builder.build();
    }

    /**
     * 构建预览响应
     */
    private ChunkPreviewResponse buildResponse(String content, String strategy,
                                                List<TextChunk> chunks, Map<String, Object> statistics) {
        // 计算统计信息
        List<Integer> sizes = chunks.stream()
                .map(c -> c.getContent().length())
                .collect(Collectors.toList());

        double avgSize = sizes.stream().mapToInt(Integer::intValue).average().orElse(0);
        int minSize = sizes.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maxSize = sizes.stream().mapToInt(Integer::intValue).max().orElse(0);

        // 构建分块详情
        List<ChunkPreviewItem> items = new ArrayList<>();
        Map<Integer, Integer> parentChildCount = new HashMap<>();

        for (TextChunk chunk : chunks) {
            // 提取父子块关系信息
            Integer parentIndex = extractParentIndex(chunk.getTags());
            Integer localIndex = extractLocalIndex(chunk.getTags());

            if (parentIndex != null) {
                parentChildCount.merge(parentIndex, 1, Integer::sum);
            }

            // 创建预览
            String previewText = createPreview(chunk.getContent(), 100);

            ChunkPreviewItem item = ChunkPreviewItem.builder()
                    .index(chunk.getIndex())
                    .content(chunk.getContent())
                    .length(chunk.getContent().length())
                    .startPos(chunk.getStartPos())
                    .endPos(chunk.getEndPos())
                    .preview(previewText)
                    .parentIndex(parentIndex)
                    .localIndex(localIndex)
                    .build();

            items.add(item);
        }

        // 添加统计信息
        statistics.put("sizeDistribution", calculateSizeDistribution(sizes));
        statistics.put("totalCharacters", chunks.stream()
                .mapToInt(c -> c.getContent().length())
                .sum());

        // 构建响应
        ChunkPreviewResponse.ChunkPreviewResponseBuilder responseBuilder = ChunkPreviewResponse.builder()
                .strategy(strategy)
                .originalLength(content.length())
                .totalChunks(chunks.size())
                .averageChunkSize(avgSize)
                .minChunkSize(minSize)
                .maxChunkSize(maxSize)
                .chunks(items)
                .statistics(statistics);

        // 分层分块特定统计
        if ("hierarchical".equalsIgnoreCase(strategy)) {
            int parentCount = parentChildCount.size();
            double avgChildren = parentCount > 0
                    ? (double) chunks.size() / parentCount
                    : 0;
            responseBuilder
                    .parentCount(parentCount)
                    .averageChildrenPerParent(avgChildren);
        }

        return responseBuilder.build();
    }

    /**
     * 创建内容预览
     */
    private String createPreview(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
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
     * 计算分块大小分布
     */
    private Map<String, Integer> calculateSizeDistribution(List<Integer> sizes) {
        Map<String, Integer> distribution = new LinkedHashMap<>();

        // 定义大小区间
        int[] thresholds = {100, 200, 500, 1000, 2000, 5000};
        String[] labels = {"0-100", "101-200", "201-500", "501-1000", "1001-2000", "2001-5000", "5001+"};

        int[] counts = new int[labels.length];

        for (int size : sizes) {
            int bucket = 0;
            for (int i = 0; i < thresholds.length; i++) {
                if (size <= thresholds[i]) {
                    bucket = i;
                    break;
                }
                bucket = i + 1;
            }
            counts[bucket]++;
        }

        for (int i = 0; i < labels.length; i++) {
            if (counts[i] > 0) {
                distribution.put(labels[i], counts[i]);
            }
        }

        return distribution;
    }
}
