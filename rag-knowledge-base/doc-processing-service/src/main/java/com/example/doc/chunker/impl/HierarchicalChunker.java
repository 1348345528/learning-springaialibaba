package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import com.example.doc.chunker.config.HierarchicalChunkConfig;
import com.example.doc.chunker.config.RecursiveChunkConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 分层分块器
 * <p>
 * 实现父子结构的分层分块策略。该分块器创建两层分块结构：
 * 父块（较大）用于提供上下文，子块（较小）用于精确检索。
 * 这种结构特别适合需要同时考虑上下文和精确匹配的 RAG 场景。
 * </p>
 *
 * <h3>工作原理</h3>
 * <ol>
 *   <li>首先将文档分割成父块（较大的上下文单元）</li>
 *   <li>然后将每个父块分割成多个子块（较小的检索单元）</li>
 *   <li>建立父子块之间的引用关系</li>
 *   <li>检索时通过子块定位，通过父块获取完整上下文</li>
 * </ol>
 *
 * <h3>子块分割策略</h3>
 * <ul>
 *   <li><b>RECURSIVE</b>：使用递归分块器分割子块（默认）</li>
 *   <li><b>FIXED</b>：使用固定长度分割子块</li>
 *   <li><b>SENTENCE</b>：按句子分割子块</li>
 * </ul>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HierarchicalChunker implements ChunkStrategy {

    /**
     * 分块器名称
     */
    private static final String NAME = "hierarchical";

    /**
     * 默认分层配置
     */
    private static final HierarchicalChunkConfig DEFAULT_CONFIG = HierarchicalChunkConfig.defaultConfig();

    /**
     * 递归分块器（用于子块分割）
     */
    private final RecursiveChunker recursiveChunker;

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        // 将通用配置转换为分层分块配置
        HierarchicalChunkConfig hierarchicalConfig = convertConfig(config);
        return chunk(text, hierarchicalConfig);
    }

    /**
     * 使用分层配置进行分块
     *
     * @param text   待分块的文本
     * @param config 分层分块配置
     * @return 分块列表（包含父子块信息）
     */
    public List<TextChunk> chunk(String text, HierarchicalChunkConfig config) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用配置或默认值
        HierarchicalChunkConfig effectiveConfig = config != null ? config : DEFAULT_CONFIG;
        effectiveConfig.validate();

        log.debug("开始分层分块，父块大小: {}, 子块大小: {}, 子块策略: {}",
                effectiveConfig.getParentChunkSize(),
                effectiveConfig.getChildChunkSize(),
                effectiveConfig.getChildSplitStrategy());

        // Step 1: 分割父块
        List<ParentChunk> parentChunks = splitParentChunks(text, effectiveConfig);
        log.debug("分割出 {} 个父块", parentChunks.size());

        // Step 2: 分割子块
        List<TextChunk> allChunks = new ArrayList<>();
        int globalIndex = 0;

        for (ParentChunk parent : parentChunks) {
            // 分割子块
            List<TextChunk> childChunks = splitChildChunks(parent, effectiveConfig);

            // 为子块设置父块引用和全局索引
            for (TextChunk child : childChunks) {
                // 创建包含父块信息的扩展 TextChunk
                TextChunk enrichedChild = TextChunk.builder()
                        .content(child.getContent())
                        .index(globalIndex++)
                        .startPos(parent.startPos + child.getStartPos())
                        .endPos(parent.startPos + child.getEndPos())
                        .tags(createTagsWithParentInfo(parent, child))
                        .build();

                allChunks.add(enrichedChild);
            }

            log.debug("父块 {} 生成了 {} 个子块", parent.index, childChunks.size());
        }

        log.info("分层分块完成，共 {} 个父块，{} 个子块", parentChunks.size(), allChunks.size());
        return allChunks;
    }

    /**
     * 分割父块
     *
     * @param text   完整文本
     * @param config 配置
     * @return 父块列表
     */
    private List<ParentChunk> splitParentChunks(String text, HierarchicalChunkConfig config) {
        List<ParentChunk> parents = new ArrayList<>();
        String[] separators = config.getEffectiveParentSeparators();
        int parentSize = config.getParentChunkSize();
        boolean keepSeparator = Boolean.TRUE.equals(config.getKeepSeparator());

        // 使用递归方式分割父块
        List<String> rawChunks = splitRecursively(text, separators, parentSize, 0);

        int currentPos = 0;
        for (int i = 0; i < rawChunks.size(); i++) {
            String content = rawChunks.get(i).trim();
            if (!content.isEmpty()) {
                parents.add(new ParentChunk(i, content, currentPos));
                currentPos += content.length();
            }
        }

        return parents;
    }

    /**
     * 递归分割文本
     *
     * @param text       文本
     * @param separators 分隔符列表
     * @param maxSize    最大大小
     * @param level      当前分隔符层级
     * @return 分割后的文本列表
     */
    private List<String> splitRecursively(String text, String[] separators, int maxSize, int level) {
        List<String> result = new ArrayList<>();

        if (text.length() <= maxSize) {
            result.add(text);
            return result;
        }

        if (level >= separators.length) {
            // 没有更多分隔符，强制分割
            return splitByFixedSize(text, maxSize);
        }

        String separator = separators[level];
        String[] parts = text.split(Pattern.quote(separator), -1);

        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (current.length() + part.length() + separator.length() > maxSize && current.length() > 0) {
                result.add(current.toString());
                current = new StringBuilder();
            }

            if (current.length() > 0) {
                current.append(separator);
            }
            current.append(part);
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        // 检查是否有过大的分块需要进一步分割
        List<String> finalResult = new ArrayList<>();
        for (String chunk : result) {
            if (chunk.length() > maxSize) {
                finalResult.addAll(splitRecursively(chunk, separators, maxSize, level + 1));
            } else {
                finalResult.add(chunk);
            }
        }

        return finalResult;
    }

    /**
     * 按固定大小分割
     *
     * @param text    文本
     * @param maxSize 最大大小
     * @return 分割后的文本列表
     */
    private List<String> splitByFixedSize(String text, int maxSize) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < text.length(); i += maxSize) {
            int end = Math.min(i + maxSize, text.length());
            result.add(text.substring(i, end));
        }
        return result;
    }

    /**
     * 分割子块
     *
     * @param parent 父块
     * @param config 配置
     * @return 子块列表
     */
    private List<TextChunk> splitChildChunks(ParentChunk parent, HierarchicalChunkConfig config) {
        switch (config.getChildSplitStrategy()) {
            case RECURSIVE:
                return splitByRecursive(parent, config);
            case FIXED:
                return splitByFixed(parent, config);
            case SENTENCE:
                return splitBySentence(parent, config);
            default:
                return splitByRecursive(parent, config);
        }
    }

    /**
     * 使用递归策略分割子块
     *
     * @param parent 父块
     * @param config 配置
     * @return 子块列表
     */
    private List<TextChunk> splitByRecursive(ParentChunk parent, HierarchicalChunkConfig config) {
        RecursiveChunkConfig recursiveConfig = RecursiveChunkConfig.builder()
                .chunkSize(config.getChildChunkSize())
                .overlap(config.getChildOverlap())
                .minChunkSize(config.getMinChildChunkSize())
                .separators(List.of(config.getEffectiveChildSeparators()))
                .keepSeparator(config.getKeepSeparator())
                .build();

        return recursiveChunker.chunk(parent.content, recursiveConfig);
    }

    /**
     * 使用固定长度分割子块
     *
     * @param parent 父块
     * @param config 配置
     * @return 子块列表
     */
    private List<TextChunk> splitByFixed(ParentChunk parent, HierarchicalChunkConfig config) {
        List<TextChunk> chunks = new ArrayList<>();
        int childSize = config.getChildChunkSize();
        int overlap = config.getChildOverlap();
        String content = parent.content;

        int pos = 0;
        int index = 0;
        while (pos < content.length()) {
            int end = Math.min(pos + childSize, content.length());
            String chunkContent = content.substring(pos, end);

            chunks.add(TextChunk.builder()
                    .content(chunkContent)
                    .index(index++)
                    .startPos(pos)
                    .endPos(end)
                    .build());

            pos += (childSize - overlap);
            if (pos < 0) pos = 0; // 防止无限循环
        }

        return chunks;
    }

    /**
     * 按句子分割子块
     *
     * @param parent 父块
     * @param config 配置
     * @return 子块列表
     */
    private List<TextChunk> splitBySentence(ParentChunk parent, HierarchicalChunkConfig config) {
        List<TextChunk> chunks = new ArrayList<>();
        String content = parent.content;
        int childSize = config.getChildChunkSize();
        int minSize = config.getMinChildChunkSize();

        // 按句子分割
        Pattern sentencePattern = Pattern.compile("([。.!?！？]+\\s*)");
        String[] sentences = sentencePattern.split(content);

        // 收集分隔符
        java.util.regex.Matcher matcher = sentencePattern.matcher(content);
        List<String> delimiters = new ArrayList<>();
        while (matcher.find()) {
            delimiters.add(matcher.group());
        }

        // 重组句子
        List<String> fullSentences = new ArrayList<>();
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            if (i < delimiters.size()) {
                sentence += delimiters.get(i);
            }
            if (!sentence.trim().isEmpty()) {
                fullSentences.add(sentence);
            }
        }

        // 按大小合并句子成子块
        StringBuilder current = new StringBuilder();
        int startPos = 0;
        int index = 0;

        for (String sentence : fullSentences) {
            if (current.length() + sentence.length() > childSize && current.length() >= minSize) {
                chunks.add(TextChunk.builder()
                        .content(current.toString().trim())
                        .index(index++)
                        .startPos(startPos)
                        .endPos(startPos + current.length())
                        .build());
                startPos += current.length();
                current = new StringBuilder();
            }
            current.append(sentence);
        }

        if (current.length() > 0) {
            chunks.add(TextChunk.builder()
                    .content(current.toString().trim())
                    .index(index)
                    .startPos(startPos)
                    .endPos(startPos + current.length())
                    .build());
        }

        return chunks;
    }

    /**
     * 创建包含父块信息的标签
     *
     * @param parent 父块
     * @param child  子块
     * @return 标签列表
     */
    private List<String> createTagsWithParentInfo(ParentChunk parent, TextChunk child) {
        List<String> tags = new ArrayList<>();
        tags.add("parent_index:" + parent.index);
        tags.add("parent_size:" + parent.content.length());
        tags.add("child_local_index:" + child.getIndex());
        tags.add("hierarchical:true");
        return tags;
    }

    /**
     * 将通用 ChunkConfig 转换为 HierarchicalChunkConfig
     *
     * @param config 通用配置
     * @return 分层分块配置
     */
    private HierarchicalChunkConfig convertConfig(ChunkConfig config) {
        HierarchicalChunkConfig.HierarchicalChunkConfigBuilder builder = HierarchicalChunkConfig.builder();

        if (config.getChunkSize() > 0) {
            // 如果只指定了 chunkSize，假设它是子块大小
            builder.childChunkSize(config.getChunkSize());
            builder.parentChunkSize(config.getChunkSize() * 10); // 父块默认是子块的 10 倍
        }
        if (config.getOverlap() >= 0) {
            builder.childOverlap(config.getOverlap());
        }
        if (config.getDelimiters() != null && config.getDelimiters().length > 0) {
            builder.childSeparators(config.getDelimiters());
        }

        return builder.build();
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * 父块内部类
     */
    private static class ParentChunk {
        final int index;
        final String content;
        final int startPos;

        ParentChunk(int index, String content, int startPos) {
            this.index = index;
            this.content = content;
            this.startPos = startPos;
        }
    }

    /**
     * 分层分块结果 DTO
     * <p>
     * 包含父子块完整信息的结果对象
     * </p>
     */
    public static class HierarchicalChunkResult {
        private final List<TextChunk> allChunks;
        private final Map<Integer, List<TextChunk>> parentToChildrenMap;
        private final Map<Integer, Integer> childToParentMap;

        public HierarchicalChunkResult(List<TextChunk> allChunks) {
            this.allChunks = allChunks;
            this.parentToChildrenMap = new HashMap<>();
            this.childToParentMap = new HashMap<>();
            buildRelationships();
        }

        private void buildRelationships() {
            for (TextChunk chunk : allChunks) {
                if (chunk.getTags() != null) {
                    Integer parentIndex = extractParentIndex(chunk.getTags());
                    if (parentIndex != null) {
                        parentToChildrenMap
                                .computeIfAbsent(parentIndex, k -> new ArrayList<>())
                                .add(chunk);
                        childToParentMap.put(chunk.getIndex(), parentIndex);
                    }
                }
            }
        }

        private Integer extractParentIndex(List<String> tags) {
            for (String tag : tags) {
                if (tag.startsWith("parent_index:")) {
                    return Integer.parseInt(tag.substring("parent_index:".length()));
                }
            }
            return null;
        }

        public List<TextChunk> getAllChunks() {
            return allChunks;
        }

        public List<TextChunk> getChildrenOfParent(int parentIndex) {
            return parentToChildrenMap.getOrDefault(parentIndex, new ArrayList<>());
        }

        public Integer getParentOfChild(int childIndex) {
            return childToParentMap.get(childIndex);
        }

        public int getParentCount() {
            return parentToChildrenMap.size();
        }
    }
}
