package com.example.doc.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 分块请求 DTO
 * <p>
 * 包含文本分块所需的各项参数。支持多种分块策略的详细配置。
 * </p>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Data
public class ChunkRequest {
    /**
     * 待分块的文本内容
     */
    private String content;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 分块策略名称
     * <p>
     * 支持的策略：fixed_length, semantic, hybrid, custom_rule, recursive, true_semantic, hierarchical
     * </p>
     */
    private String strategy;

    // ========== 通用分块参数 ==========

    /**
     * 目标分块大小（字符数）
     */
    @Min(value = 50, message = "分块大小不能小于50字符")
    @Max(value = 10000, message = "分块大小不能超过10000字符")
    private Integer chunkSize;

    /**
     * 分块之间的重叠大小（字符数）
     */
    @Min(value = 0, message = "重叠大小不能为负数")
    @Max(value = 500, message = "重叠大小不能超过500字符")
    private Integer overlap;

    /**
     * 是否保留标题
     */
    private Boolean keepHeaders;

    /**
     * 最小段落长度
     */
    @Min(value = 10, message = "最小段落长度不能小于10字符")
    private Integer minParagraphLength;

    /**
     * 自定义分隔符数组
     */
    private String[] delimiters;

    /**
     * 标题级别数组
     */
    private Integer[] headerLevels;

    /**
     * 标签列表
     */
    private List<String> tags;

    // ========== 递归分块参数 ==========

    /**
     * 递归分块：最小分块大小
     */
    @Min(value = 10, message = "最小分块大小不能小于10字符")
    private Integer minChunkSize;

    /**
     * 递归分块：是否保留分隔符
     */
    private Boolean keepSeparator;

    /**
     * 递归分块：是否去除首尾空白
     */
    private Boolean trimWhitespace;

    // ========== 语义分块参数 ==========

    /**
     * 语义分块：相似度阈值
     * <p>
     * 当两个相邻句子的相似度低于此阈值时，会在它们之间创建分块边界。
     * 值范围：0.0 ~ 1.0
     * </p>
     */
    @DecimalMin(value = "0.0", message = "相似度阈值不能小于0")
    @DecimalMax(value = "1.0", message = "相似度阈值不能大于1")
    private Double similarityThreshold;

    /**
     * 语义分块：百分位数阈值
     * <p>
     * 当使用 PERCENTILE 断点检测方法时使用
     * </p>
     */
    @DecimalMin(value = "0.0", message = "百分位数阈值不能小于0")
    @DecimalMax(value = "1.0", message = "百分位数阈值不能大于1")
    private Double percentileThreshold;

    /**
     * 语义分块：是否使用动态阈值
     */
    private Boolean useDynamicThreshold;

    /**
     * 语义分块：断点检测方法
     * <p>
     * 可选值：PERCENTILE, THRESHOLD, GRADIENT
     * </p>
     */
    private String breakpointMethod;

    /**
     * 语义分块：最大分块大小
     */
    @Min(value = 200, message = "最大分块大小不能小于200字符")
    @Max(value = 10000, message = "最大分块大小不能超过10000字符")
    private Integer maxChunkSize;

    // ========== 分层分块参数 ==========

    /**
     * 分层分块：父块大小
     */
    @Min(value = 500, message = "父块大小不能小于500字符")
    @Max(value = 20000, message = "父块大小不能超过20000字符")
    private Integer parentChunkSize;

    /**
     * 分层分块：子块大小
     */
    @Min(value = 50, message = "子块大小不能小于50字符")
    @Max(value = 2000, message = "子块大小不能超过2000字符")
    private Integer childChunkSize;

    /**
     * 分层分块：子块重叠大小
     */
    @Min(value = 0, message = "子块重叠不能为负数")
    @Max(value = 500, message = "子块重叠不能超过500字符")
    private Integer childOverlap;

    /**
     * 分层分块：子块分割策略
     * <p>
     * 可选值：RECURSIVE, FIXED, SENTENCE
     * </p>
     */
    private String childSplitStrategy;

    /**
     * 分层分块：是否存储父块引用
     */
    private Boolean storeParentReference;
}
