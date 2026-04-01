package com.snrt.knowledgebase.domain.knowledgegraph.repository;

import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraphNodeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeGraphNodeRepository extends Neo4jRepository<KnowledgeGraphNodeEntity, String> {

    Optional<KnowledgeGraphNodeEntity> findByUuidAndIsDeletedFalse(String uuid);

    List<KnowledgeGraphNodeEntity> findByKnowledgeGraphUuidAndIsDeletedFalse(String knowledgeGraphUuid);

    List<KnowledgeGraphNodeEntity> findByLabelAndKnowledgeGraphUuidAndIsDeletedFalse(String label, String knowledgeGraphUuid);

    Optional<KnowledgeGraphNodeEntity> findByNameAndKnowledgeGraphUuidAndIsDeletedFalse(String name, String knowledgeGraphUuid);

    long countByKnowledgeGraphUuidAndIsDeletedFalse(String knowledgeGraphUuid);

    List<KnowledgeGraphNodeEntity> findByKnowledgeGraphUuidIn(List<String> knowledgeGraphUuids);
}
