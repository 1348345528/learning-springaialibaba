package com.example.rag.doc.service;

import com.example.rag.doc.chunker.ChunkConfig;
import com.example.rag.doc.chunker.ChunkStrategy;
import com.example.rag.doc.chunker.TextChunk;
import com.example.rag.doc.dto.ChunkPreviewRequest;
import com.example.rag.doc.dto.ChunkPreviewResponse;
import com.example.rag.doc.dto.ChunkPreviewResponse.ChunkPreviewItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkPreviewService {

    private final Map<String, ChunkStrategy> strategyMap;

    public ChunkPreviewResponse preview(ChunkPreviewRequest request) {
        String content = request.getContent();
        String strategy = request.getStrategy();
        log.info("开始分块预览，策略: {}, 文本长度: {}", strategy, content.length());

        ChunkStrategy chunker = strategyMap.get(strategy);
        if (chunker == null) {
            throw new IllegalArgumentException("不支持的分块策略: " + strategy
                    + "，可用策略: " + strategyMap.keySet());
        }

        ChunkConfig config = buildConfig(request);
        List<TextChunk> chunks = chunker.chunk(content, config);

        Map<String, Object> statistics = new HashMap<>();
        return buildResponse(content, strategy, chunks, statistics);
    }

    private ChunkConfig buildConfig(ChunkPreviewRequest request) {
        ChunkConfig config = new ChunkConfig();
        if (request.getChunkSize() != null) config.setChunkSize(request.getChunkSize());
        if (request.getOverlap() != null) config.setOverlap(request.getOverlap());
        if (request.getMinChunkSize() != null) config.setMinParagraphLength(request.getMinChunkSize());
        if (request.getSeparators() != null && !request.getSeparators().isEmpty()) {
            config.setDelimiters(request.getSeparators().toArray(new String[0]));
        }
        return config;
    }

    private ChunkPreviewResponse buildResponse(String content, String strategy,
                                                List<TextChunk> chunks, Map<String, Object> statistics) {
        List<Integer> sizes = chunks.stream().map(c -> c.getContent().length()).collect(Collectors.toList());

        double avgSize = sizes.stream().mapToInt(Integer::intValue).average().orElse(0);
        int minSize = sizes.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maxSize = sizes.stream().mapToInt(Integer::intValue).max().orElse(0);

        Map<Integer, Integer> parentChildCount = new HashMap<>();
        List<ChunkPreviewItem> items = new ArrayList<>();

        for (TextChunk chunk : chunks) {
            Integer parentIndex = extractParentIndex(chunk.getTags());
            Integer localIndex = extractLocalIndex(chunk.getTags());

            if (parentIndex != null) {
                parentChildCount.merge(parentIndex, 1, Integer::sum);
            }

            String previewText = chunk.getContent().length() <= 100
                    ? chunk.getContent()
                    : chunk.getContent().substring(0, 100) + "...";

            items.add(ChunkPreviewItem.builder()
                    .index(chunk.getIndex())
                    .content(chunk.getContent())
                    .length(chunk.getContent().length())
                    .startPos(chunk.getStartPos())
                    .endPos(chunk.getEndPos())
                    .preview(previewText)
                    .parentIndex(parentIndex)
                    .localIndex(localIndex)
                    .build());
        }

        statistics.put("sizeDistribution", calculateSizeDistribution(sizes));
        statistics.put("totalCharacters", chunks.stream().mapToInt(c -> c.getContent().length()).sum());

        ChunkPreviewResponse.ChunkPreviewResponseBuilder builder = ChunkPreviewResponse.builder()
                .strategy(strategy)
                .originalLength(content.length())
                .totalChunks(chunks.size())
                .averageChunkSize(avgSize)
                .minChunkSize(minSize)
                .maxChunkSize(maxSize)
                .chunks(items)
                .statistics(statistics);

        if ("hierarchical".equalsIgnoreCase(strategy)) {
            int parentCount = parentChildCount.size();
            double avgChildren = parentCount > 0 ? (double) chunks.size() / parentCount : 0;
            builder.parentCount(parentCount).averageChildrenPerParent(avgChildren);
        }

        return builder.build();
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

    private Map<String, Integer> calculateSizeDistribution(List<Integer> sizes) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        int[] thresholds = {100, 200, 500, 1000, 2000, 5000};
        String[] labels = {"0-100", "101-200", "201-500", "501-1000", "1001-2000", "2001-5000", "5001+"};
        int[] counts = new int[labels.length];

        for (int size : sizes) {
            int bucket = 0;
            for (int i = 0; i < thresholds.length; i++) {
                if (size <= thresholds[i]) { bucket = i; break; }
                bucket = i + 1;
            }
            counts[bucket]++;
        }

        for (int i = 0; i < labels.length; i++) {
            if (counts[i] > 0) distribution.put(labels[i], counts[i]);
        }
        return distribution;
    }
}
