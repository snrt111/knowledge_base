package com.snrt.knowledgebase.domain.knowledge.repository;

import com.snrt.knowledgebase.domain.knowledge.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 知识库Repository
 * 
 * 提供知识库的CRUD操作和查询功能
 * 
 * @author SNRT
 * @since 1.0
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, String> {

    /**
     * 分页查询未删除的知识库
     * 
     * @param pageable 分页参数
     * @return 知识库分页列表
     */
    Page<KnowledgeBase> findByIsDeletedFalse(Pageable pageable);

    /**
     * 根据关键词模糊查询未删除的知识库
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 知识库分页列表
     */
    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.isDeleted = false AND kb.name LIKE %:keyword%")
    Page<KnowledgeBase> findByNameContainingAndIsDeletedFalse(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据ID查找未删除的知识库
     * 
     * @param id 知识库ID
     * @return 知识库可选值
     */
    Optional<KnowledgeBase> findByIdAndIsDeletedFalse(String id);

    /**
     * 统计未删除的知识库总数
     * 
     * @return 知识库数量
     */
    long countByIsDeletedFalse();
}
