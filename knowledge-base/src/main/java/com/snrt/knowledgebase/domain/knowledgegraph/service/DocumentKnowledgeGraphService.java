package com.snrt.knowledgebase.domain.knowledgegraph.service;

import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraphRelationEntity;
import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraphNodeEntity;
import com.snrt.knowledgebase.domain.knowledgegraph.repository.KnowledgeGraphRelationRepository;
import com.snrt.knowledgebase.domain.knowledgegraph.repository.KnowledgeGraphNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentKnowledgeGraphService {

    private final KnowledgeGraphNodeRepository nodeRepository;
    private final KnowledgeGraphRelationRepository relationRepository;

    public static final String NODE_LABEL_DOCUMENT = "DOCUMENT";
    public static final String NODE_LABEL_ENTITY = "ENTITY";
    public static final String NODE_LABEL_CONCEPT = "CONCEPT";

    public static final String RELATION_TYPE_EXTRACTS = "EXTRACTS";
    public static final String RELATION_TYPE_DISCUSSES = "DISCUSSES";
    public static final String RELATION_TYPE_CONTAINS = "CONTAINS";
    public static final String RELATION_TYPE_SIMILAR = "SIMILAR";

    public void linkDocumentToEntity(String documentUuid, String entityName, String entityLabel, String knowledgeGraphUuid) {
        log.debug("[文档-实体关联] 开始关联: documentUuid={}, entityName={}, entityLabel={}, knowledgeGraphUuid={}", 
                documentUuid, entityName, entityLabel, knowledgeGraphUuid);

        var entityNode = nodeRepository.findByNameAndKnowledgeGraphUuidAndIsDeletedFalse(entityName, knowledgeGraphUuid);
        if (entityNode.isEmpty()) {
            log.info("[文档-实体关联] 实体不存在，创建新实体: entityName={}, knowledgeGraphUuid={}", entityName, knowledgeGraphUuid);
            var newNode = new KnowledgeGraphNodeEntity();
            newNode.setUuid(java.util.UUID.randomUUID().toString());
            newNode.setLabel(entityLabel);
            newNode.setName(entityName);
            newNode.setKnowledgeGraphUuid(knowledgeGraphUuid);
            newNode.setIsDeleted(false);
            nodeRepository.save(newNode);
            entityNode = Optional.of(newNode);
            log.debug("[文档-实体关联] 新实体创建成功: entityUuid={}", newNode.getUuid());
        } else {
            log.debug("[文档-实体关联] 实体已存在: entityUuid={}", entityNode.get().getUuid());
        }

        var documentNode = nodeRepository.findByUuidAndIsDeletedFalse(documentUuid);
        if (documentNode.isEmpty()) {
            log.warn("[文档-实体关联] 文档节点不存在: documentUuid={}", documentUuid);
            return;
        }

        var existingRelation = relationRepository.findByFromNodeUuidAndKnowledgeGraphUuidAndIsDeletedFalse(
                documentNode.get().getUuid(), knowledgeGraphUuid);
        String entityUuid = entityNode.get().getUuid();
        boolean alreadyLinked = existingRelation.stream()
                .anyMatch(r -> r.getToNodeUuid().equals(entityUuid) && 
                              r.getType().equals(RELATION_TYPE_EXTRACTS));
        
        if (!alreadyLinked) {
            var relation = new KnowledgeGraphRelationEntity();
            relation.setUuid(java.util.UUID.randomUUID().toString());
            relation.setType(RELATION_TYPE_EXTRACTS);
            relation.setFromNodeUuid(documentNode.get().getUuid());
            relation.setToNodeUuid(entityNode.get().getUuid());
            relation.setKnowledgeGraphUuid(knowledgeGraphUuid);
            relation.setIsDeleted(false);
            relationRepository.save(relation);
            log.info("[文档-实体关联] 创建成功: documentUuid={} -> entityName={}", documentUuid, entityName);
        } else {
            log.debug("[文档-实体关联] 关系已存在，跳过: documentUuid={} -> entityName={}", documentUuid, entityName);
        }
    }

    public List<String> getDocumentsByEntity(String entityName, String knowledgeGraphUuid) {
        var entityNode = nodeRepository.findByNameAndKnowledgeGraphUuidAndIsDeletedFalse(entityName, knowledgeGraphUuid);
        if (entityNode.isEmpty()) {
            return List.of();
        }

        var relations = relationRepository.findByToNodeUuidAndKnowledgeGraphUuidAndIsDeletedFalse(
                entityNode.get().getUuid(), knowledgeGraphUuid);

        return relations.stream()
                .map(KnowledgeGraphRelationEntity::getFromNodeUuid)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    public void createEntityRelationship(String fromEntityName, String toEntityName, String relationType, 
                                         String knowledgeGraphUuid) {
        log.debug("创建实体关系: from={}, to={}, type={}, knowledgeGraphUuid={}", 
                fromEntityName, toEntityName, relationType, knowledgeGraphUuid);

        var fromNode = nodeRepository.findByNameAndKnowledgeGraphUuidAndIsDeletedFalse(fromEntityName, knowledgeGraphUuid);
        var toNode = nodeRepository.findByNameAndKnowledgeGraphUuidAndIsDeletedFalse(toEntityName, knowledgeGraphUuid);

        if (fromNode.isEmpty() || toNode.isEmpty()) {
            log.warn("实体不存在，无法创建关系: from={}, to={}", fromEntityName, toEntityName);
            return;
        }

        var existingRelation = relationRepository.findByFromNodeUuidAndKnowledgeGraphUuidAndIsDeletedFalse(
                fromNode.get().getUuid(), knowledgeGraphUuid);
        String toNodeUuid = toNode.get().getUuid();
        boolean alreadyExists = existingRelation.stream()
                .anyMatch(r -> r.getToNodeUuid().equals(toNodeUuid) && 
                              r.getType().equals(relationType));
        
        if (!alreadyExists) {
            var relation = new KnowledgeGraphRelationEntity();
            relation.setUuid(java.util.UUID.randomUUID().toString());
            relation.setType(relationType);
            relation.setFromNodeUuid(fromNode.get().getUuid());
            relation.setToNodeUuid(toNode.get().getUuid());
            relation.setKnowledgeGraphUuid(knowledgeGraphUuid);
            relation.setIsDeleted(false);
            relationRepository.save(relation);
            log.info("创建实体关系: {} -[{}]-> {}", fromEntityName, relationType, toEntityName);
        }
    }
}
