package com.snrt.knowledgebase.domain.knowledgegraph.repository;

import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraphRelationEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识图谱关系Repository
 * 
 * 提供知识图谱关系的CRUD操作和查询功能
 * 
 * @author SNRT
 * @since 1.0
 */
@Repository
public interface KnowledgeGraphRelationRepository extends Neo4jRepository<KnowledgeGraphRelationEntity, String> {

    /**
     * 根据UUID查找未删除的关系
     * 
     * @param uuid 关系UUID
     * @return 关系可选值
     */
    Optional<KnowledgeGraphRelationEntity> findByUuidAndIsDeletedFalse(String uuid);

    /**
     * 根据知识图谱UUID查找未删除的关系列表
     * 
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 关系列表
     */
    List<KnowledgeGraphRelationEntity> findByKnowledgeGraphUuidAndIsDeletedFalse(String knowledgeGraphUuid);

    /**
     * 根据关系类型和知识图谱UUID查找未删除的关系列表
     * 
     * @param type 关系类型
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 关系列表
     */
    List<KnowledgeGraphRelationEntity> findByTypeAndKnowledgeGraphUuidAndIsDeletedFalse(String type, String knowledgeGraphUuid);

    /**
     * 根据起始节点UUID和知识图谱UUID查找未删除的关系列表
     * 
     * @param fromNodeUuid 起始节点UUID
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 关系列表
     */
    List<KnowledgeGraphRelationEntity> findByFromNodeUuidAndKnowledgeGraphUuidAndIsDeletedFalse(String fromNodeUuid, String knowledgeGraphUuid);

    /**
     * 根据目标节点UUID和知识图谱UUID查找未删除的关系列表
     * 
     * @param toNodeUuid 目标节点UUID
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 关系列表
     */
    List<KnowledgeGraphRelationEntity> findByToNodeUuidAndKnowledgeGraphUuidAndIsDeletedFalse(String toNodeUuid, String knowledgeGraphUuid);

    /**
     * 根据知识图谱UUID统计关系数量
     * 
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 关系数量
     */
    long countByKnowledgeGraphUuidAndIsDeletedFalse(String knowledgeGraphUuid);

    /**
     * 根据知识图谱UUID列表查找未删除的关系列表
     * 
     * @param knowledgeGraphUuids 知识图谱UUID列表
     * @return 关系列表
     */
    List<KnowledgeGraphRelationEntity> findByKnowledgeGraphUuidIn(List<String> knowledgeGraphUuids);
}
