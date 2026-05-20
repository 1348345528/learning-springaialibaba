package com.example.rag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    @Column(name = "start_pos")
    private Integer startPos;

    @Column(name = "end_pos")
    private Integer endPos;

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "parent_index")
    private Integer parentIndex;

    @Column(name = "local_index")
    private Integer localIndex;

    @Column(name = "is_parent")
    private Boolean isParent;

    @Column(name = "is_child")
    private Boolean isChild;

    @Column(columnDefinition = "JSON")
    private String metadata;
}
