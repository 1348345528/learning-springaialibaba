package com.example.doc.repository;

import com.example.doc.entity.Chunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, String> {

    Page<Chunk> findByDocumentNameContaining(String documentName, Pageable pageable);

    List<Chunk> findByDocumentName(String documentName);

    void deleteByDocumentName(String documentName);

    long countByDocumentName(String documentName);

    @Query("SELECT DISTINCT c.documentName FROM Chunk c ORDER BY c.documentName")
    List<String> findDistinctDocumentNames();
}
