package com.snrt.knowledgebase.infrastructure.knowledgegraph;

import com.snrt.knowledgebase.domain.knowledgegraph.service.DocumentKnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识图谱同步服务
 * 
 * 提供文档与知识图谱的异步同步功能：
 * - 文档内容提取并关联实体
 * - 实体关系的批量同步
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGraphSyncService {

    private final DocumentKnowledgeGraphService documentKnowledgeGraphService;
    private final EntityExtractionService entityExtractionService;

    public static final String DEFAULT_KNOWLEDGE_GRAPH_UUID = "default";

    /**
     * 异步同步文档到知识图谱
     * 
     * 执行流程：
     * 1. 从文档内容中提取实体
     * 2. 遍历实体列表，将文档与每个实体进行关联
     * 3. 统计成功和失败的实体数量
     * 4. 记录同步结果日志
     * 
     * @param documentUuid 文档UUID
     * @param content 文档内容
     * @param knowledgeBaseId 知识库ID
     */
    @Async
    public void syncDocumentToKnowledgeGraph(String documentUuid, String content, String knowledgeBaseId) {
        log.info("[知识图谱同步] 开始同步文档到知识图谱: documentUuid={}, knowledgeBaseId={}, contentLength={}", 
                documentUuid, knowledgeBaseId, content != null ? content.length() : 0);
        
        try {
            List<String> entities = extractEntitiesFromContent(content, documentUuid);
            
            if (entities.isEmpty()) {
                log.warn("[知识图谱同步] 未提取到任何实体，跳过关联: documentUuid={}", documentUuid);
                return;
            }
            
            syncEntitiesToGraph(entities, documentUuid, knowledgeBaseId);
            
        } catch (Exception e) {
            log.error("[知识图谱同步] 同步失败: documentUuid={}", documentUuid, e);
        }
    }

    /**
     * 从内容中提取实体
     * 
     * @param content 文档内容
     * @param documentUuid 文档UUID（用于日志追踪）
     * @return 实体列表
     */
    private List<String> extractEntitiesFromContent(String content, String documentUuid) {
        log.debug("[知识图谱同步] 开始提取实体: documentUuid={}", documentUuid);
        List<String> entities = entityExtractionService.extractEntities(content);
        log.info("[知识图谱同步] 提取到 {} 个实体: {}", entities.size(), entities);
        return entities;
    }

    /**
     * 将实体列表同步到知识图谱
     * 
     * @param entities 实体列表
     * @param documentUuid 文档UUID
     * @param knowledgeBaseId 知识库ID
     */
    private void syncEntitiesToGraph(List<String> entities, String documentUuid, String knowledgeBaseId) {
        int successCount = 0;
        int failCount = 0;
        
        for (String entity : entities) {
            try {
                linkDocumentToEntity(documentUuid, entity, knowledgeBaseId);
                successCount++;
                log.debug("[知识图谱同步] 成功关联实体: documentUuid={}, entity={}", documentUuid, entity);
            } catch (Exception e) {
                failCount++;
                log.error("[知识图谱同步] 关联实体失败: documentUuid={}, entity={}, error={}", 
                        documentUuid, entity, e.getMessage());
            }
        }
        
        log.info("[知识图谱同步] 同步完成: documentUuid={}, 成功关联 {} 个实体, 失败 {} 个实体", 
                documentUuid, successCount, failCount);
    }

    /**
     * 关联文档与实体
     * 
     * @param documentUuid 文档UUID
     * @param entityName 实体名称
     * @param knowledgeBaseId 知识库ID
     */
    private void linkDocumentToEntity(String documentUuid, String entityName, String knowledgeBaseId) {
        documentKnowledgeGraphService.linkDocumentToEntity(
                documentUuid,
                entityName,
                DocumentKnowledgeGraphService.NODE_LABEL_ENTITY,
                knowledgeBaseId
        );
    }

    /**
     * 异步同步实体关系到知识图谱
     * 
     * 执行流程：
     * 1. 定义实体关系对
     * 2. 遍历关系对，创建实体间的相似关系
     * 3. 记录同步结果
     * 
     * @param knowledgeBaseId 知识库ID
     */
    @Async
    public void syncEntityRelationships(String knowledgeBaseId) {
        log.info("开始同步实体关系: knowledgeBaseId={}", knowledgeBaseId);
        
        try {
            List<String[]> relationshipPairs = buildRelationshipPairs();
            
            for (String[] pair : relationshipPairs) {
                createEntityRelationship(pair[0], pair[1], knowledgeBaseId);
            }
            
            log.info("实体关系同步完成: knowledgeBaseId={}", knowledgeBaseId);
        } catch (Exception e) {
            log.error("实体关系同步失败: knowledgeBaseId={}", knowledgeBaseId, e);
        }
    }

    /**
     * 构建实体关系对列表
     * 
     * @return 实体关系对列表
     */
    private List<String[]> buildRelationshipPairs() {
        return List.of(
            new String[]{"AI技术", "机器学习"},
            new String[]{"机器学习", "深度学习"},
            new String[]{"深度学习", "NLP"},
            new String[]{"NLP", "LLM"},
            new String[]{"LLM", "大语言模型"}
        );
    }

    /**
     * 创建实体间的关系
     * 
     * @param fromEntityName 起始实体名称
     * @param toEntityName 目标实体名称
     * @param knowledgeBaseId 知识库ID
     */
    private void createEntityRelationship(String fromEntityName, String toEntityName, String knowledgeBaseId) {
        documentKnowledgeGraphService.createEntityRelationship(
                fromEntityName,
                toEntityName,
                DocumentKnowledgeGraphService.RELATION_TYPE_SIMILAR,
                knowledgeBaseId
        );
    }
}
