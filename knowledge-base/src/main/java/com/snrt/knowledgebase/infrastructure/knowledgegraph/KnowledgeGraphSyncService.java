package com.snrt.knowledgebase.infrastructure.knowledgegraph;

import com.snrt.knowledgebase.domain.knowledgegraph.service.DocumentKnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGraphSyncService {

    private final DocumentKnowledgeGraphService documentKnowledgeGraphService;
    private final EntityExtractionService entityExtractionService;

    public static final String DEFAULT_KNOWLEDGE_GRAPH_UUID = "default";

    @Async
    public void syncDocumentToKnowledgeGraph(String documentUuid, String content, String knowledgeBaseId) {
        log.info("[知识图谱同步] 开始同步文档到知识图谱: documentUuid={}, knowledgeBaseId={}, contentLength={}", 
                documentUuid, knowledgeBaseId, content != null ? content.length() : 0);
        
        try {
            log.debug("[知识图谱同步] 开始提取实体: documentUuid={}", documentUuid);
            List<String> entities = entityExtractionService.extractEntities(content);
            log.info("[知识图谱同步] 提取到 {} 个实体: {}", entities.size(), entities);
            
            if (entities.isEmpty()) {
                log.warn("[知识图谱同步] 未提取到任何实体，跳过关联: documentUuid={}", documentUuid);
                return;
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (String entity : entities) {
                try {
                    documentKnowledgeGraphService.linkDocumentToEntity(
                            documentUuid,
                            entity,
                            DocumentKnowledgeGraphService.NODE_LABEL_ENTITY,
                            knowledgeBaseId
                    );
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
        } catch (Exception e) {
            log.error("[知识图谱同步] 同步失败: documentUuid={}", documentUuid, e);
        }
    }

    @Async
    public void syncEntityRelationships(String knowledgeBaseId) {
        log.info("开始同步实体关系: knowledgeBaseId={}", knowledgeBaseId);
        
        try {
            String[] entityPairs = {
                "AI技术,机器学习",
                "机器学习,深度学习",
                "深度学习,NLP",
                "NLP,LLM",
                "LLM,大语言模型"
            };
            
            for (String pair : entityPairs) {
                String[] entities = pair.split(",");
                if (entities.length == 2) {
                    documentKnowledgeGraphService.createEntityRelationship(
                            entities[0],
                            entities[1],
                            DocumentKnowledgeGraphService.RELATION_TYPE_SIMILAR,
                            knowledgeBaseId
                    );
                }
            }
            
            log.info("实体关系同步完成: knowledgeBaseId={}", knowledgeBaseId);
        } catch (Exception e) {
            log.error("实体关系同步失败: knowledgeBaseId={}", knowledgeBaseId, e);
        }
    }
}
