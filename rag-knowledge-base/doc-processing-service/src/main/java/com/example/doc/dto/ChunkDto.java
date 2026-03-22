package com.example.doc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分块数据传输对象
 * <p>
 * 包含分块的完整信息，包括父子块关系和元数据。
 * </p>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkDto {
    /**
     * 分块唯一标识
     */
    private String id;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 所属文档名称
     */
    private String documentName;

    /**
     * 分块大小（字符数）
     */
    private Integer chunkSize;

    /**
     * 分块索引（全局）
     */
    private Integer chunkIndex;

    /**
     * 分块策略名称
     */
    private String strategy;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ========== 分块位置信息 ==========

    /**
     * 在原文中的起始位置
     */
    private Integer startPos;

    /**
     * 在原文中的结束位置
     */
    private Integer endPos;

    // ========== 分层分块关系 ==========

    /**
     * 父块 ID（分层分块时使用）
     * <p>
     * 如果当前分块是子块，此字段指向其父块的 ID。
     * </p>
     */
    private String parentId;

    /**
     * 父块索引（分层分块时使用）
     */
    private Integer parentIndex;

    /**
     * 在父块内的局部索引（分层分块时使用）
     */
    private Integer localIndex;

    /**
     * 子块 ID 列表（分层分块时，当前块为父块时使用）
     */
    private List<String> childIds;

    /**
     * 是否为父块
     */
    private Boolean isParent;

    /**
     * 是否为子块
     */
    private Boolean isChild;

    // ========== 元数据 ==========

    /**
     * 扩展元数据
     * <p>
     * 用于存储分块器特定的元数据信息。
     * </p>
     */
    private Map<String, Object> metadata;

    /**
     * 相似度分数（语义分块时使用）
     * <p>
     * 该分块与前一个分块的边界相似度。
     * </p>
     */
    private Double similarityScore;

    /**
     * 断点类型（语义分块时使用）
     */
    private String breakpointType;

    /**
     * 创建基础分块 DTO（用于向后兼容）
     *
     * @param id           分块 ID
     * @param content      内容
     * @param documentName 文档名称
     * @param chunkSize    分块大小
     * @param chunkIndex   分块索引
     * @param strategy     分块策略
     * @param tags         标签
     * @param createdAt    创建时间
     * @param updatedAt    更新时间
     * @return ChunkDto
     */
    public static ChunkDto of(String id, String content, String documentName,
                               Integer chunkSize, Integer chunkIndex, String strategy,
                               List<String> tags, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return ChunkDto.builder()
                .id(id)
                .content(content)
                .documentName(documentName)
                .chunkSize(chunkSize)
                .chunkIndex(chunkIndex)
                .strategy(strategy)
                .tags(tags)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
