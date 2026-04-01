package com.snrt.knowledgebase.domain.knowledgegraph.repository;

import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraphRelationEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeGraphRelationRepository extends Neo4jRepository<KnowledgeGraphRelationEntity, String> {

    Optional<KnowledgeGraphRelationEntity> findByUuidAndIsDeletedFalse(String uuid);

    List<KnowledgeGraphRelationEntity> findByKnowledgeGraphUuidAndIsDeletedFalse(String knowledgeGraphUuid);

    List<KnowledgeGraphRelationEntity> findByTypeAndKnowledgeGraphUuidAndIsDeletedFalse(String type, String knowledgeGraphUuid);

    List<KnowledgeGraphRelationEntity> findByFromNodeUuidAndKnowledgeGraphUuidAndIsDeletedFalse(String fromNodeUuid, String knowledgeGraphUuid);

    List<KnowledgeGraphRelationEntity> findByToNodeUuidAndKnowledgeGraphUuidAndIsDeletedFalse(String toNodeUuid, String knowledgeGraphUuid);

    long countByKnowledgeGraphUuidAndIsDeletedFalse(String knowledgeGraphUuid);

    List<KnowledgeGraphRelationEntity> findByKnowledgeGraphUuidIn(List<String> knowledgeGraphUuids);
}
