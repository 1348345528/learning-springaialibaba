package com.example.doc.chunker.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 语义分块器配置类
 * <p>
 * 用于配置基于 Embedding 相似度的语义分块策略。该分块器通过计算相邻句子之间的
 * 语义相似度来检测主题边界，从而实现更加智能的分块效果。
 * </p>
 *
 * <h3>断点检测方法</h3>
 * <ul>
 *   <li>PERCENTILE - 百分位数法：选择相似度低于某个百分位的句子作为断点</li>
 *   <li>THRESHOLD - 阈值法：相似度低于固定阈值时创建断点</li>
 *   <li>GRADIENT - 梯度法：检测相似度变化的梯度来识别边界</li>
 * </ul>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticChunkConfig {

    /**
     * 默认相似度阈值
     */
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.45;

    /**
     * 默认百分位数阈值
     */
    public static final double DEFAULT_PERCENTILE_THRESHOLD = 0.8;

    /**
     * 默认最小分块大小
     */
    public static final int DEFAULT_MIN_CHUNK_SIZE = 50;

    /**
     * 默认最大分块大小
     */
    public static final int DEFAULT_MAX_CHUNK_SIZE = 2000;

    /**
     * 断点检测方法枚举
     */
    public enum BreakpointMethod {
        /**
         * 百分位数法：选择相似度低于某个百分位的句子作为断点
         */
        PERCENTILE,

        /**
         * 阈值法：相似度低于固定阈值时创建断点
         */
        THRESHOLD,

        /**
         * 梯度法：检测相似度变化的梯度来识别边界
         */
        GRADIENT
    }

    /**
     * 相似度阈值
     * <p>
     * 当两个相邻句子的相似度低于此阈值时，会在它们之间创建分块边界。
     * 值范围：0.0 ~ 1.0，值越小分块越少，值越大分块越多。
     * 推荐值：0.3 ~ 0.6
     * </p>
     */
    @NotNull
    @DecimalMin(value = "0.0", message = "相似度阈值不能小于0")
    @DecimalMax(value = "1.0", message = "相似度阈值不能大于1")
    @Builder.Default
    private Double similarityThreshold = DEFAULT_SIMILARITY_THRESHOLD;

    /**
     * 百分位数阈值
     * <p>
     * 当使用 PERCENTILE 断点检测方法时，选择相似度最低的 X% 的位置作为断点。
     * 值范围：0.0 ~ 1.0，例如 0.8 表示选择相似度最低的 20% 位置作为断点。
     * </p>
     */
    @NotNull
    @DecimalMin(value = "0.0", message = "百分位数阈值不能小于0")
    @DecimalMax(value = "1.0", message = "百分位数阈值不能大于1")
    @Builder.Default
    private Double percentileThreshold = DEFAULT_PERCENTILE_THRESHOLD;

    /**
     * 是否使用动态阈值
     * <p>
     * 如果为 true，系统会根据文本特征自动调整相似度阈值；
     * 如果为 false，使用固定的 similarityThreshold 值。
     * </p>
     */
    @Builder.Default
    private Boolean useDynamicThreshold = true;

    /**
     * 断点检测方法
     */
    @Builder.Default
    private BreakpointMethod breakpointMethod = BreakpointMethod.PERCENTILE;

    /**
     * 最小分块大小（字符数）
     * <p>
     * 分块的最小有效长度，避免生成过小的分块。
     * </p>
     */
    @NotNull
    @Min(value = 20, message = "最小分块大小不能小于20字符")
    @Max(value = 1000, message = "最小分块大小不能超过1000字符")
    @Builder.Default
    private Integer minChunkSize = DEFAULT_MIN_CHUNK_SIZE;

    /**
     * 最大分块大小（字符数）
     * <p>
     * 分块的最大长度，超过此长度会强制分割。
     * </p>
     */
    @NotNull
    @Min(value = 200, message = "最大分块大小不能小于200字符")
    @Max(value = 10000, message = "最大分块大小不能超过10000字符")
    @Builder.Default
    private Integer maxChunkSize = DEFAULT_MAX_CHUNK_SIZE;

    /**
     * 句子分割正则表达式
     * <p>
     * 用于将文本分割成句子的正则表达式。默认支持中英文句号、问号、感叹号。
     * </p>
     */
    @Builder.Default
    private String sentenceSplitPattern = "[。.!?！？]+\\s*";

    /**
     * 是否保留句子分隔符
     * <p>
     * 如果为 true，分割后的句子会保留分隔符（句号、问号等）。
     * </p>
     */
    @Builder.Default
    private Boolean keepSentenceDelimiter = true;

    /**
     * Embedding 服务 URL
     * <p>
     * 用于计算句子向量的 Embedding 服务地址。
     * 如果为空，将使用默认配置的服务地址。
     * </p>
     */
    private String embeddingServiceUrl;

    /**
     * 批量计算 Embedding 的大小
     * <p>
     * 一次请求中计算 Embedding 的最大句子数量，用于优化性能。
     * </p>
     */
    @Builder.Default
    private Integer embeddingBatchSize = 32;

    /**
     * 获取有效的相似度阈值
     * <p>
     * 如果启用了动态阈值，返回 null 表示需要动态计算；
     * 否则返回配置的固定阈值。
     * </p>
     *
     * @return 相似度阈值
     */
    public Double getEffectiveSimilarityThreshold() {
        if (Boolean.TRUE.equals(useDynamicThreshold)) {
            return null; // 需要动态计算
        }
        return similarityThreshold;
    }

    /**
     * 创建默认配置
     *
     * @return 默认的语义分块配置
     */
    public static SemanticChunkConfig defaultConfig() {
        return SemanticChunkConfig.builder().build();
    }

    /**
     * 创建指定相似度阈值的配置
     *
     * @param threshold 相似度阈值
     * @return 语义分块配置
     */
    public static SemanticChunkConfig ofThreshold(double threshold) {
        return SemanticChunkConfig.builder()
                .similarityThreshold(threshold)
                .useDynamicThreshold(false)
                .build();
    }

    /**
     * 创建指定百分位数阈值的配置
     *
     * @param percentile 百分位数阈值
     * @return 语义分块配置
     */
    public static SemanticChunkConfig ofPercentile(double percentile) {
        return SemanticChunkConfig.builder()
                .percentileThreshold(percentile)
                .breakpointMethod(BreakpointMethod.PERCENTILE)
                .build();
    }
}
