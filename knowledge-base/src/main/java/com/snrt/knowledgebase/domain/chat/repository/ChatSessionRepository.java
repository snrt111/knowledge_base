package com.snrt.knowledgebase.domain.chat.repository;

import com.snrt.knowledgebase.domain.chat.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 聊天会话Repository
 * 
 * 提供聊天会话的CRUD操作和查询功能
 * 
 * @author SNRT
 * @since 1.0
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    /**
     * 分页查询未删除的聊天会话，按更新时间降序
     * 
     * @param pageable 分页参数
     * @return 聊天会话分页列表
     */
    Page<ChatSession> findByIsDeletedFalseOrderByUpdateTimeDesc(Pageable pageable);

    /**
     * 根据ID查找未删除的聊天会话
     * 
     * @param id 会话ID
     * @return 聊天会话可选值
     */
    Optional<ChatSession> findByIdAndIsDeletedFalse(String id);

    /**
     * 根据标题模糊查询未删除的聊天会话
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 聊天会话分页列表
     */
    @Query("SELECT s FROM ChatSession s WHERE s.isDeleted = false AND s.title LIKE %:keyword% ORDER BY s.updateTime DESC")
    Page<ChatSession> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 统计未删除的聊天会话总数
     * 
     * @return 聊天会话数量
     */
    long countByIsDeletedFalse();
}
