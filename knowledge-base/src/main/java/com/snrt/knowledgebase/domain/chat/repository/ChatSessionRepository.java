package com.snrt.knowledgebase.domain.chat.repository;

import com.snrt.knowledgebase.domain.chat.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    Page<ChatSession> findByIsDeletedFalseOrderByUpdateTimeDesc(Pageable pageable);

    Optional<ChatSession> findByIdAndIsDeletedFalse(String id);

    @Query("SELECT s FROM ChatSession s WHERE s.isDeleted = false AND s.title LIKE %:keyword% ORDER BY s.updateTime DESC")
    Page<ChatSession> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    long countByIsDeletedFalse();
}
