package com.snrt.knowledgebase.domain.knowledgegraph.repository;

import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraphNodeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识图谱节点Repository
 * 
 * 提供知识图谱节点的CRUD操作和查询功能
 * 
 * @author SNRT
 * @since 1.0
 */
@Repository
public interface KnowledgeGraphNodeRepository extends Neo4jRepository<KnowledgeGraphNodeEntity, String> {

    /**
     * 根据UUID查找未删除的节点
     * 
     * @param uuid 节点UUID
     * @return 节点可选值
     */
    Optional<KnowledgeGraphNodeEntity> findByUuidAndIsDeletedFalse(String uuid);

    /**
     * 根据知识图谱UUID查找未删除的节点列表
     * 
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 节点列表
     */
    List<KnowledgeGraphNodeEntity> findByKnowledgeGraphUuidAndIsDeletedFalse(String knowledgeGraphUuid);

    /**
     * 根据标签和知识图谱UUID查找未删除的节点列表
     * 
     * @param label 节点标签
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 节点列表
     */
    List<KnowledgeGraphNodeEntity> findByLabelAndKnowledgeGraphUuidAndIsDeletedFalse(String label, String knowledgeGraphUuid);

    /**
     * 根据名称、知识图谱UUID查找未删除的节点
     * 
     * @param name 节点名称
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 节点可选值
     */
    Optional<KnowledgeGraphNodeEntity> findByNameAndKnowledgeGraphUuidAndIsDeletedFalse(String name, String knowledgeGraphUuid);

    /**
     * 根据知识图谱UUID统计节点数量
     * 
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 节点数量
     */
    long countByKnowledgeGraphUuidAndIsDeletedFalse(String knowledgeGraphUuid);

    /**
     * 根据知识图谱UUID列表查找未删除的节点列表
     * 
     * @param knowledgeGraphUuids 知识图谱UUID列表
     * @return 节点列表
     */
    List<KnowledgeGraphNodeEntity> findByKnowledgeGraphUuidIn(List<String> knowledgeGraphUuids);
}
