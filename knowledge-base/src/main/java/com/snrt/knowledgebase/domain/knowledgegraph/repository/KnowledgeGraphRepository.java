package com.snrt.knowledgebase.domain.knowledgegraph.repository;

import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraph;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识图谱Repository
 * 
 * 提供知识图谱的CRUD操作和查询功能
 * 
 * @author SNRT
 * @since 1.0
 */
@Repository
public interface KnowledgeGraphRepository extends Neo4jRepository<KnowledgeGraph, String> {

    /**
     * 根据UUID查找未删除的知识图谱
     * 
     * @param uuid 知识图谱UUID
     * @return 知识图谱可选值
     */
    Optional<KnowledgeGraph> findByUuidAndIsDeletedFalse(String uuid);

    /**
     * 根据知识库ID查找未删除的知识图谱列表
     * 
     * @param knowledgeBaseId 知识库ID
     * @return 知识图谱列表
     */
    List<KnowledgeGraph> findByKnowledgeBaseIdAndIsDeletedFalse(String knowledgeBaseId);

    /**
     * 根据名称模糊查询未删除的知识图谱
     * 
     * @param name 知识图谱名称
     * @return 知识图谱列表
     */
    List<KnowledgeGraph> findByNameContainingAndIsDeletedFalse(String name);

    /**
     * 统计未删除的知识图谱总数
     * 
     * @return 知识图谱数量
     */
    long countByIsDeletedFalse();

    /**
     * 根据知识库ID统计未删除的知识图谱数量
     * 
     * @param knowledgeBaseId 知识库ID
     * @return 知识图谱数量
     */
    long countByKnowledgeBaseIdAndIsDeletedFalse(String knowledgeBaseId);
}
