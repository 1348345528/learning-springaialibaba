package com.example.rag.repository;

import com.example.rag.entity.Chunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, String> {
    List<Chunk> findByIdIn(List<String> ids);

    Page<Chunk> findByDocumentNameContaining(String keyword, Pageable pageable);

    @Query("SELECT DISTINCT c.documentName FROM Chunk c")
    List<String> findDistinctDocumentNames();
}
