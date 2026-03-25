package com.snrt.knowledgebase.repository;

import com.snrt.knowledgebase.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    Page<Document> findByIsDeletedFalse(Pageable pageable);

    Page<Document> findByKnowledgeBaseIdAndIsDeletedFalse(String knowledgeBaseId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.knowledgeBase.id = :kbId AND d.name LIKE %:keyword%")
    Page<Document> findByKnowledgeBaseIdAndNameContainingAndIsDeletedFalse(
            @Param("kbId") String knowledgeBaseId,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<Document> findByIdAndIsDeletedFalse(String id);

    List<Document> findByKnowledgeBaseIdAndIsDeletedFalse(String knowledgeBaseId);

    long countByKnowledgeBaseIdAndIsDeletedFalse(String knowledgeBaseId);

    long countByIsDeletedFalse();
}
