package com.example.doc.chunker.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 分层分块器配置类
 * <p>
 * 用于配置父子结构的分层分块策略。该分块器创建两层分块结构：
 * 父块（较大）用于提供上下文，子块（较小）用于精确检索。
 * 这种结构特别适合需要同时考虑上下文和精确匹配的 RAG 场景。
 * </p>
 *
 * <h3>子块分割策略</h3>
 * <ul>
 *   <li>RECURSIVE - 使用递归分块器分割子块</li>
 *   <li>FIXED - 使用固定长度分割子块</li>
 *   <li>SENTENCE - 按句子分割子块</li>
 * </ul>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HierarchicalChunkConfig {

    /**
     * 默认父块大小
     */
    public static final int DEFAULT_PARENT_CHUNK_SIZE = 2000;

    /**
     * 默认子块大小
     */
    public static final int DEFAULT_CHILD_CHUNK_SIZE = 200;

    /**
     * 默认子块重叠大小
     */
    public static final int DEFAULT_CHILD_OVERLAP = 20;

    /**
     * 子块分割策略枚举
     */
    public enum ChildSplitStrategy {
        /**
         * 使用递归分块器分割子块
         */
        RECURSIVE,

        /**
         * 使用固定长度分割子块
         */
        FIXED,

        /**
         * 按句子分割子块
         */
        SENTENCE
    }

    /**
     * 父块大小（字符数）
     * <p>
     * 父块是较大的上下文单元，用于提供完整的语义背景。
     * 父块的大小应该足够包含完整的主题或段落。
     * </p>
     */
    @NotNull
    @Min(value = 500, message = "父块大小不能小于500字符")
    @Max(value = 20000, message = "父块大小不能超过20000字符")
    @Builder.Default
    private Integer parentChunkSize = DEFAULT_PARENT_CHUNK_SIZE;

    /**
     * 子块大小（字符数）
     * <p>
     * 子块是较小的检索单元，用于精确匹配用户查询。
     * 子块的大小应该足够小以保持精确性，但也要包含足够的语义信息。
     * </p>
     */
    @NotNull
    @Min(value = 50, message = "子块大小不能小于50字符")
    @Max(value = 2000, message = "子块大小不能超过2000字符")
    @Builder.Default
    private Integer childChunkSize = DEFAULT_CHILD_CHUNK_SIZE;

    /**
     * 子块之间的重叠大小（字符数）
     * <p>
     * 相邻子块之间的重叠区域，有助于保持子块间的上下文连贯性。
     * </p>
     */
    @NotNull
    @Min(value = 0, message = "子块重叠不能为负数")
    @Max(value = 500, message = "子块重叠不能超过500字符")
    @Builder.Default
    private Integer childOverlap = DEFAULT_CHILD_OVERLAP;

    /**
     * 子块分割策略
     * <p>
     * 决定如何将父块分割成子块的方法。
     * </p>
     */
    @Builder.Default
    private ChildSplitStrategy childSplitStrategy = ChildSplitStrategy.RECURSIVE;

    /**
     * 父块分割分隔符
     * <p>
     * 用于分割父块的分隔符列表，按优先级排列。
     * 如果未指定，将使用默认分隔符。
     * </p>
     */
    private String[] parentSeparators;

    /**
     * 子块分割分隔符
     * <p>
     * 用于分割子块的分隔符列表，按优先级排列。
     * 如果未指定，将使用默认分隔符。
     * </p>
     */
    private String[] childSeparators;

    /**
     * 是否保留分隔符
     * <p>
     * 如果为 true，分割后的文本会保留分隔符。
     * </p>
     */
    @Builder.Default
    private Boolean keepSeparator = true;

    /**
     * 最小子块大小（字符数）
     * <p>
     * 子块的最小有效长度，小于此长度的子块会被合并。
     * </p>
     */
    @Builder.Default
    private Integer minChildChunkSize = 50;

    /**
     * 是否为每个子块存储父块引用
     * <p>
     * 如果为 true，每个子块会包含对其父块的引用，
     * 便于检索时获取完整上下文。
     * </p>
     */
    @Builder.Default
    private Boolean storeParentReference = true;

    /**
     * 获取有效的父块分隔符
     *
     * @return 父块分隔符数组
     */
    public String[] getEffectiveParentSeparators() {
        return parentSeparators != null && parentSeparators.length > 0
                ? parentSeparators
                : new String[]{"\n\n\n", "\n\n", "\n", "。", "."};
    }

    /**
     * 获取有效的子块分隔符
     *
     * @return 子块分隔符数组
     */
    public String[] getEffectiveChildSeparators() {
        return childSeparators != null && childSeparators.length > 0
                ? childSeparators
                : new String[]{"\n\n", "\n", "。", ".", ",", " ", ""};
    }

    /**
     * 验证配置的有效性
     *
     * @throws IllegalArgumentException 如果配置无效
     */
    public void validate() {
        if (childChunkSize >= parentChunkSize) {
            throw new IllegalArgumentException(
                    "子块大小(" + childChunkSize + ")必须小于父块大小(" + parentChunkSize + ")");
        }
        if (childOverlap >= childChunkSize) {
            throw new IllegalArgumentException(
                    "子块重叠(" + childOverlap + ")必须小于子块大小(" + childChunkSize + ")");
        }
    }

    /**
     * 创建默认配置
     *
     * @return 默认的分层分块配置
     */
    public static HierarchicalChunkConfig defaultConfig() {
        return HierarchicalChunkConfig.builder().build();
    }

    /**
     * 创建指定父子块大小的配置
     *
     * @param parentSize 父块大小
     * @param childSize  子块大小
     * @return 分层分块配置
     */
    public static HierarchicalChunkConfig of(int parentSize, int childSize) {
        HierarchicalChunkConfig config = HierarchicalChunkConfig.builder()
                .parentChunkSize(parentSize)
                .childChunkSize(childSize)
                .build();
        config.validate();
        return config;
    }

    /**
     * 创建指定父子块大小和重叠的配置
     *
     * @param parentSize   父块大小
     * @param childSize    子块大小
     * @param childOverlap 子块重叠
     * @return 分层分块配置
     */
    public static HierarchicalChunkConfig of(int parentSize, int childSize, int childOverlap) {
        HierarchicalChunkConfig config = HierarchicalChunkConfig.builder()
                .parentChunkSize(parentSize)
                .childChunkSize(childSize)
                .childOverlap(childOverlap)
                .build();
        config.validate();
        return config;
    }
}
