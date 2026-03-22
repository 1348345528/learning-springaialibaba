package com.example.doc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 分块实体类
 * <p>
 * 表示文档分块在数据库中的存储结构。
 * 支持分层分块的父子关系。
 * </p>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Entity
@Table(name = "chunks", indexes = {
        @Index(name = "idx_document_name", columnList = "document_name"),
        @Index(name = "idx_chunk_index", columnList = "chunk_index"),
        @Index(name = "idx_strategy", columnList = "strategy"),
        @Index(name = "idx_parent_id", columnList = "parent_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {

    @Id
    private String id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "chunk_size")
    private Integer chunkSize;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    private String strategy;

    @Column(columnDefinition = "JSON")
    private String tags;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========== 分块位置信息 ==========

    /**
     * 在原文中的起始位置
     */
    @Column(name = "start_pos")
    private Integer startPos;

    /**
     * 在原文中的结束位置
     */
    @Column(name = "end_pos")
    private Integer endPos;

    // ========== 分层分块关系 ==========

    /**
     * 父块 ID
     */
    @Column(name = "parent_id")
    private String parentId;

    /**
     * 父块索引
     */
    @Column(name = "parent_index")
    private Integer parentIndex;

    /**
     * 在父块内的局部索引
     */
    @Column(name = "local_index")
    private Integer localIndex;

    /**
     * 是否为父块
     */
    @Column(name = "is_parent")
    private Boolean isParent;

    /**
     * 是否为子块
     */
    @Column(name = "is_child")
    private Boolean isChild;

    // ========== 扩展元数据 ==========

    /**
     * 扩展元数据（JSON 格式）
     */
    @Column(columnDefinition = "JSON")
    private String metadata;
}
