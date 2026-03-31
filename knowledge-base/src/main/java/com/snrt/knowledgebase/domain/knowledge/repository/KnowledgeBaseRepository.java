package com.snrt.knowledgebase.domain.knowledge.repository;

import com.snrt.knowledgebase.domain.knowledge.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, String> {

    Page<KnowledgeBase> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.isDeleted = false AND kb.name LIKE %:keyword%")
    Page<KnowledgeBase> findByNameContainingAndIsDeletedFalse(@Param("keyword") String keyword, Pageable pageable);

    Optional<KnowledgeBase> findByIdAndIsDeletedFalse(String id);

    long countByIsDeletedFalse();
}
