package com.example.doc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 分块预览请求 DTO
 * <p>
 * 用于在不实际保存分块的情况下预览分块效果。
 * </p>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkPreviewRequest {

    /**
     * 待预览分块的文本内容
     */
    @NotBlank(message = "文本内容不能为空")
    @Size(max = 1000000, message = "文本内容不能超过100万字符")
    private String content;

    /**
     * 分块策略名称
     * <p>
     * 支持：recursive, true_semantic, hierarchical
     * </p>
     */
    @NotBlank(message = "分块策略不能为空")
    private String strategy;

    // ========== 递归分块参数 ==========

    /**
     * 目标分块大小
     */
    private Integer chunkSize;

    /**
     * 重叠大小
     */
    private Integer overlap;

    /**
     * 最小分块大小
     */
    private Integer minChunkSize;

    /**
     * 是否保留分隔符
     */
    private Boolean keepSeparator;

    /**
     * 自定义分隔符列表
     */
    private List<String> separators;

    // ========== 语义分块参数 ==========

    /**
     * 相似度阈值
     */
    private Double similarityThreshold;

    /**
     * 百分位数阈值
     */
    private Double percentileThreshold;

    /**
     * 是否使用动态阈值
     */
    private Boolean useDynamicThreshold;

    /**
     * 断点检测方法：PERCENTILE, THRESHOLD, GRADIENT
     */
    private String breakpointMethod;

    /**
     * 最大分块大小
     */
    private Integer maxChunkSize;

    // ========== 分层分块参数 ==========

    /**
     * 父块大小
     */
    private Integer parentChunkSize;

    /**
     * 子块大小
     */
    private Integer childChunkSize;

    /**
     * 子块重叠
     */
    private Integer childOverlap;

    /**
     * 子块分割策略：RECURSIVE, FIXED, SENTENCE
     */
    private String childSplitStrategy;
}
