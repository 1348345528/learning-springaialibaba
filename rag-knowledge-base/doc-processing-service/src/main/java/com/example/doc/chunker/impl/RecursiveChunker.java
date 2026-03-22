package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import com.example.doc.chunker.config.RecursiveChunkConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 递归分块器
 * <p>
 * 实现基于多级分隔符的递归文本分块算法。该分块器按照分隔符优先级依次尝试
 * 分割文本，如果某个分隔符产生的分块仍然过大，则使用下一级分隔符继续分割，
 * 直到每个分块大小满足要求。
 * </p>
 *
 * <h3>工作原理</h3>
 * <ol>
 *   <li>尝试使用最高优先级分隔符分割文本</li>
 *   <li>检查每个分块大小是否在目标范围内</li>
 *   <li>对于过大的分块，使用下一级分隔符继续分割</li>
 *   <li>重复以上步骤直到所有分块满足大小要求或使用完所有分隔符</li>
 *   <li>合并过小的分块，添加重叠区域</li>
 * </ol>
 *
 * <h3>默认分隔符优先级</h3>
 * <pre>
 * "\n\n\n" (三级换行) → "\n\n" (双换行) → "\n" (单换行)
 * → "。" (中文句号) → "." (英文句号) → "," (逗号) → " " (空格) → "" (字符分割)
 * </pre>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Component
@Slf4j
public class RecursiveChunker implements ChunkStrategy {

    /**
     * 分块器名称
     */
    private static final String NAME = "recursive";

    /**
     * 默认递归配置
     */
    private static final RecursiveChunkConfig DEFAULT_CONFIG = RecursiveChunkConfig.defaultConfig();

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        // 将通用配置转换为递归分块配置
        RecursiveChunkConfig recursiveConfig = convertConfig(config);
        return chunk(text, recursiveConfig);
    }

    /**
     * 使用递归配置进行分块
     *
     * @param text   待分块的文本
     * @param config 递归分块配置
     * @return 分块列表
     */
    public List<TextChunk> chunk(String text, RecursiveChunkConfig config) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用配置或默认值
        RecursiveChunkConfig effectiveConfig = config != null ? config : DEFAULT_CONFIG;
        int chunkSize = effectiveConfig.getChunkSize();
        int minChunkSize = effectiveConfig.getMinChunkSize();
        List<String> separators = effectiveConfig.getEffectiveSeparators();

        log.debug("开始递归分块，目标大小: {}, 最小大小: {}, 分隔符数量: {}",
                chunkSize, minChunkSize, separators.size());

        // 执行递归分块
        List<String> rawChunks = splitTextRecursively(text, separators, chunkSize);

        // 合并过小的分块并添加重叠
        List<TextChunk> result = mergeAndBuildChunks(rawChunks, effectiveConfig);

        log.info("递归分块完成，生成分块数量: {}", result.size());
        return result;
    }

    /**
     * 递归分割文本
     * <p>
     * 按分隔符优先级递归分割文本，确保每个分块不超过目标大小。
     * </p>
     *
     * @param text       待分割的文本
     * @param separators 分隔符列表（按优先级排列）
     * @param chunkSize  目标分块大小
     * @return 分割后的文本片段列表
     */
    private List<String> splitTextRecursively(String text, List<String> separators, int chunkSize) {
        List<String> result = new ArrayList<>();

        // 基准情况：文本长度已经满足要求
        if (text.length() <= chunkSize) {
            result.add(text);
            return result;
        }

        // 如果没有分隔符了，按字符分割
        if (separators.isEmpty()) {
            return splitByFixedSize(text, chunkSize);
        }

        // 获取当前优先级的分隔符
        String separator = separators.get(0);
        List<String> nextSeparators = separators.subList(1, separators.size());

        // 按当前分隔符分割
        List<String> splits = splitBySeparator(text, separator);

        // 用于累积当前分块
        StringBuilder currentChunk = new StringBuilder();

        for (String split : splits) {
            // 如果单个分割片段仍然过大，递归使用下一级分隔符
            if (split.length() > chunkSize) {
                // 先保存当前累积的分块
                if (currentChunk.length() > 0) {
                    result.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                // 递归分割过大的片段
                result.addAll(splitTextRecursively(split, nextSeparators, chunkSize));
            } else {
                // 检查添加后是否会超过目标大小
                int newLength = currentChunk.length() + split.length();
                if (separator != null && !separator.isEmpty()) {
                    newLength += separator.length();
                }

                if (currentChunk.length() > 0 && newLength > chunkSize) {
                    // 超过大小，保存当前分块并开始新的
                    result.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }

                // 添加到当前分块
                if (currentChunk.length() > 0 && separator != null && !separator.isEmpty()) {
                    currentChunk.append(separator);
                }
                currentChunk.append(split);
            }
        }

        // 添加最后一个分块
        if (currentChunk.length() > 0) {
            result.add(currentChunk.toString());
        }

        return result;
    }

    /**
     * 按分隔符分割文本
     *
     * @param text      待分割的文本
     * @param separator 分隔符
     * @return 分割后的文本片段列表
     */
    private List<String> splitBySeparator(String text, String separator) {
        List<String> result = new ArrayList<>();

        if (separator == null || separator.isEmpty()) {
            // 空分隔符：按字符分割
            for (char c : text.toCharArray()) {
                result.add(String.valueOf(c));
            }
            return result;
        }

        // 按分隔符分割，保留分隔符信息用于后续重建
        String[] parts = text.split(java.util.regex.Pattern.quote(separator), -1);

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.add(parts[i]);
            }
        }

        return result;
    }

    /**
     * 按固定大小分割文本
     *
     * @param text      待分割的文本
     * @param chunkSize 分块大小
     * @return 分割后的文本片段列表
     */
    private List<String> splitByFixedSize(String text, int chunkSize) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            result.add(text.substring(i, end));
        }
        return result;
    }

    /**
     * 合并过小的分块并构建最终的分块对象
     *
     * @param rawChunks 原始分块列表
     * @param config    配置
     * @return 最终的分块对象列表
     */
    private List<TextChunk> mergeAndBuildChunks(List<String> rawChunks, RecursiveChunkConfig config) {
        List<TextChunk> result = new ArrayList<>();
        int chunkSize = config.getChunkSize();
        int overlap = config.getOverlap();
        int minChunkSize = config.getMinChunkSize();
        boolean trimWhitespace = Boolean.TRUE.equals(config.getTrimWhitespace());

        // 合并过小的分块
        List<String> mergedChunks = mergeSmallChunks(rawChunks, minChunkSize, chunkSize);

        // 添加重叠区域并构建最终分块
        int currentPos = 0;
        for (int i = 0; i < mergedChunks.size(); i++) {
            String content = mergedChunks.get(i);

            // 处理空白字符
            if (trimWhitespace) {
                content = content.trim();
            }

            // 计算实际起始位置（考虑重叠）
            int startPos = Math.max(0, currentPos - (i > 0 ? overlap : 0));

            // 添加重叠内容
            if (i > 0 && overlap > 0 && !mergedChunks.get(i - 1).isEmpty()) {
                String prevChunk = mergedChunks.get(i - 1);
                int overlapStart = Math.max(0, prevChunk.length() - overlap);
                String overlapContent = prevChunk.substring(overlapStart);
                content = overlapContent + content;
            }

            TextChunk chunk = TextChunk.builder()
                    .content(content)
                    .index(result.size())
                    .startPos(startPos)
                    .endPos(startPos + content.length())
                    .build();

            result.add(chunk);
            currentPos += mergedChunks.get(i).length();
        }

        return result;
    }

    /**
     * 合并过小的分块
     *
     * @param chunks       原始分块列表
     * @param minChunkSize 最小分块大小
     * @param maxChunkSize 最大分块大小
     * @return 合并后的分块列表
     */
    private List<String> mergeSmallChunks(List<String> chunks, int minChunkSize, int maxChunkSize) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String chunk : chunks) {
            // 如果当前累积内容加上新分块不会超过最大大小
            if (current.length() + chunk.length() <= maxChunkSize) {
                current.append(chunk);
            } else {
                // 当前累积内容已经足够大，保存它
                if (current.length() >= minChunkSize) {
                    result.add(current.toString());
                    current = new StringBuilder(chunk);
                } else {
                    // 当前累积内容太小，继续累积
                    current.append(chunk);
                }
            }
        }

        // 处理最后一个分块
        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * 将通用 ChunkConfig 转换为 RecursiveChunkConfig
     *
     * @param config 通用配置
     * @return 递归分块配置
     */
    private RecursiveChunkConfig convertConfig(ChunkConfig config) {
        RecursiveChunkConfig.RecursiveChunkConfigBuilder builder = RecursiveChunkConfig.builder();

        if (config.getChunkSize() > 0) {
            builder.chunkSize(config.getChunkSize());
        }
        if (config.getOverlap() >= 0) {
            builder.overlap(config.getOverlap());
        }
        if (config.getMinParagraphLength() > 0) {
            builder.minChunkSize(config.getMinParagraphLength());
        }
        if (config.getDelimiters() != null && config.getDelimiters().length > 0) {
            builder.separators(Arrays.asList(config.getDelimiters()));
        }

        return builder.build();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
