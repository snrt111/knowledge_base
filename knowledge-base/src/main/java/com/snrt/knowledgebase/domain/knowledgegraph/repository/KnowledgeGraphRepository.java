package com.snrt.knowledgebase.domain.knowledgegraph.repository;

import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraph;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeGraphRepository extends Neo4jRepository<KnowledgeGraph, String> {

    Optional<KnowledgeGraph> findByUuidAndIsDeletedFalse(String uuid);

    List<KnowledgeGraph> findByKnowledgeBaseIdAndIsDeletedFalse(String knowledgeBaseId);

    List<KnowledgeGraph> findByNameContainingAndIsDeletedFalse(String name);

    long countByIsDeletedFalse();

    long countByKnowledgeBaseIdAndIsDeletedFalse(String knowledgeBaseId);
}
