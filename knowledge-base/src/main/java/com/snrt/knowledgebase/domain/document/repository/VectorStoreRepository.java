package com.snrt.knowledgebase.domain.document.repository;

import com.snrt.knowledgebase.domain.document.entity.VectorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 向量存储 Repository
 *
 * 用于全文检索和向量数据查询
 *
 * @author SNRT
 * @since 1.0
 */
@Repository
public interface VectorStoreRepository extends JpaRepository<VectorDocument, Long> {

    /**
     * 使用 PostgreSQL 全文检索进行关键词搜索
     *
     * @param query 查询关键词（tsquery 格式）
     * @param limit 返回结果数量
     * @return 匹配的文档列表
     */
    @Query(value = """
        SELECT
            id,
            content,
            metadata,
            embedding,
            ts_rank(to_tsvector('simple', content), to_tsquery('simple', :query)) as rank
        FROM vector_store
        WHERE to_tsvector('simple', content) @@ to_tsquery('simple', :query)
        ORDER BY rank DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<VectorDocument> searchByFullText(@Param("query") String query, @Param("limit") int limit);

    /**
     * 使用 PostgreSQL 全文检索进行关键词搜索（带知识库过滤）
     *
     * @param query 查询关键词（tsquery 格式）
     * @param knowledgeBaseId 知识库ID
     * @param limit 返回结果数量
     * @return 匹配的文档列表
     */
    @Query(value = """
        SELECT
            id,
            content,
            metadata,
            embedding,
            ts_rank(to_tsvector('simple', content), to_tsquery('simple', :query)) as rank
        FROM vector_store
        WHERE to_tsvector('simple', content) @@ to_tsquery('simple', :query)
            AND metadata->>'knowledge_base_id' = :kbId
        ORDER BY rank DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<VectorDocument> searchByFullTextAndKnowledgeBase(
            @Param("query") String query,
            @Param("kbId") String knowledgeBaseId,
            @Param("limit") int limit);

    /**
     * 使用 BM25 算法进行全文检索（带文档长度归一化）
     *
     * @param query 查询关键词（普通文本，会自动转换为tsquery）
     * @param limit 返回结果数量
     * @param normalizationMode 归一化模式 (0-32, 常用: 1=长度归一化, 2=唯一词数归一化, 8=词频归一化)
     * @return 匹配的文档列表
     */
    @Query(value = """
        SELECT
            id,
            content,
            metadata,
            embedding,
            ts_rank_cd(to_tsvector('simple', content), plainto_tsquery('simple', :query), :normalizationMode) as rank
        FROM vector_store
        WHERE to_tsvector('simple', content) @@ plainto_tsquery('simple', :query)
        ORDER BY rank DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<VectorDocument> searchByBM25(
            @Param("query") String query,
            @Param("limit") int limit,
            @Param("normalizationMode") int normalizationMode);

    /**
     * 使用 BM25 算法进行全文检索（带知识库过滤）
     *
     * @param query 查询关键词（普通文本，会自动转换为tsquery）
     * @param knowledgeBaseId 知识库ID
     * @param limit 返回结果数量
     * @param normalizationMode 归一化模式 (0-32, 常用: 1=长度归一化, 2=唯一词数归一化, 8=词频归一化)
     * @return 匹配的文档列表
     */
    @Query(value = """
        SELECT
            id,
            content,
            metadata,
            embedding,
            ts_rank_cd(to_tsvector('simple', content), plainto_tsquery('simple', :query), :normalizationMode) as rank
        FROM vector_store
        WHERE to_tsvector('simple', content) @@ plainto_tsquery('simple', :query)
            AND metadata->>'knowledge_base_id' = :kbId
        ORDER BY rank DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<VectorDocument> searchByBM25AndKnowledgeBase(
            @Param("query") String query,
            @Param("kbId") String knowledgeBaseId,
            @Param("limit") int limit,
            @Param("normalizationMode") int normalizationMode);

    /**
     * 检查全文检索是否可用
     *
     * @return 如果可用返回 true
     */
    @Query(value = "SELECT to_tsvector('simple', 'test') @@ to_tsquery('simple', 'test')", nativeQuery = true)
    Boolean isFullTextSearchAvailable();

    /**
     * 根据文档ID删除向量数据
     *
     * @param documentId 文档ID
     */
    @Query(value = "DELETE FROM vector_store WHERE metadata->>'document_id' = :documentId", nativeQuery = true)
    void deleteByDocumentId(@Param("documentId") String documentId);

    /**
     * 根据知识库ID统计文档数量
     *
     * @param knowledgeBaseId 知识库ID
     * @return 文档数量
     */
    @Query(value = """
        SELECT COUNT(*) 
        FROM vector_store 
        WHERE metadata->>'knowledge_base_id' = :kbId
        """, nativeQuery = true)
    Long countByKnowledgeBaseId(@Param("kbId") String knowledgeBaseId);
}
