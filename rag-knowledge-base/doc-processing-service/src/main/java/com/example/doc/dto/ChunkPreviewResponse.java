package com.example.doc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 分块预览响应 DTO
 * <p>
 * 包含分块预览的完整结果，包括统计信息和每个分块的详情。
 * </p>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkPreviewResponse {

    /**
     * 使用的分块策略
     */
    private String strategy;

    /**
     * 原始文本长度
     */
    private Integer originalLength;

    /**
     * 分块总数
     */
    private Integer totalChunks;

    /**
     * 平均分块大小
     */
    private Double averageChunkSize;

    /**
     * 最小分块大小
     */
    private Integer minChunkSize;

    /**
     * 最大分块大小
     */
    private Integer maxChunkSize;

    /**
     * 分块详情列表
     */
    private List<ChunkPreviewItem> chunks;

    /**
     * 策略特定统计信息
     */
    private Map<String, Object> statistics;

    /**
     * 分层分块：父块数量
     */
    private Integer parentCount;

    /**
     * 分层分块：平均每父块的子块数
     */
    private Double averageChildrenPerParent;

    /**
     * 单个分块预览项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkPreviewItem {
        /**
         * 分块索引
         */
        private Integer index;

        /**
         * 分块内容
         */
        private String content;

        /**
         * 内容长度
         */
        private Integer length;

        /**
         * 起始位置
         */
        private Integer startPos;

        /**
         * 结束位置
         */
        private Integer endPos;

        /**
         * 内容预览（前100字符）
         */
        private String preview;

        /**
         * 父块索引（分层分块时使用）
         */
        private Integer parentIndex;

        /**
         * 局部索引（分层分块时使用）
         */
        private Integer localIndex;

        /**
         * 相似度分数（语义分块时使用）
         */
        private Double similarityScore;

        /**
         * 额外元数据
         */
        private Map<String, Object> metadata;
    }
}
