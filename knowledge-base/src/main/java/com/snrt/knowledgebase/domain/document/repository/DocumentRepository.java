package com.snrt.knowledgebase.domain.document.repository;

import com.snrt.knowledgebase.domain.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档Repository
 * 
 * 提供文档的CRUD操作和查询功能
 * 
 * @author SNRT
 * @since 1.0
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    /**
     * 分页查询未删除的文档
     * 
     * @param pageable 分页参数
     * @return 文档分页列表
     */
    Page<Document> findByIsDeletedFalse(Pageable pageable);

    /**
     * 根据知识库ID分页查询未删除的文档
     * 
     * @param knowledgeBaseId 知识库ID
     * @param pageable 分页参数
     * @return 文档分页列表
     */
    Page<Document> findByKnowledgeBaseIdAndIsDeletedFalse(String knowledgeBaseId, Pageable pageable);

    /**
     * 根据知识库ID和文档名称模糊查询未删除的文档
     * 
     * @param knowledgeBaseId 知识库ID
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页列表
     */
    @Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.knowledgeBase.id = :kbId "
            + "AND LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Document> findByKnowledgeBaseIdAndNameContainingAndIsDeletedFalse(
            @Param("kbId") String knowledgeBaseId,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 根据ID查找未删除的文档
     * 
     * @param id 文档ID
     * @return 文档可选值
     */
    Optional<Document> findByIdAndIsDeletedFalse(String id);

    /**
     * 根据ID查找未删除的文档并关联知识库
     * 
     * @param id 文档ID
     * @return 文档可选值
     */
    @Query("SELECT d FROM Document d JOIN FETCH d.knowledgeBase WHERE d.id = :id AND d.isDeleted = false")
    Optional<Document> findByIdAndIsDeletedFalseWithKnowledgeBase(@Param("id") String id);

    /**
     * 根据知识库ID查询未删除的文档列表
     * 
     * @param knowledgeBaseId 知识库ID
     * @return 文档列表
     */
    List<Document> findByKnowledgeBaseIdAndIsDeletedFalse(String knowledgeBaseId);

    /**
     * 根据知识库ID统计未删除的文档数量
     * 
     * @param knowledgeBaseId 知识库ID
     * @return 文档数量
     */
    long countByKnowledgeBaseIdAndIsDeletedFalse(String knowledgeBaseId);

    /**
     * 统计未删除的文档总数
     * 
     * @return 文档数量
     */
    long countByIsDeletedFalse();
}
