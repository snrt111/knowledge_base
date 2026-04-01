package com.snrt.knowledgebase.infrastructure.retrieval;

import com.snrt.knowledgebase.config.RetrievalConfig;
import com.snrt.knowledgebase.domain.knowledgegraph.service.DocumentKnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedRetrievalService {

    private static final String[] COMPLEX_KEYWORDS = {"技术", "产品", "服务", "功能", "模块", "组件", "系统"};
    private static final String[] SIMPLE_KEYWORDS = {"AI", "机器学习", "深度学习", "NLP", "LLM"};

    private final MultiRetriever multiRetriever;
    private final CrossEncoderReranker reranker;
    private final HydeService hydeService;
    private final QueryRewriterService queryRewriterService;
    private final DocumentKnowledgeGraphService documentKnowledgeGraphService;
    private final RetrievalConfig retrievalConfig;

    public List<Document> retrieve(String query, String knowledgeBaseId, int topK) {
        String traceId = generateTraceId();
        Instant start = Instant.now();

        log.info("[{}] [高级检索] 开始，查询: '{}', 知识库: {}, topK: {}",
                traceId, query, knowledgeBaseId, topK);
        // 查询改写
        String processedQuery = rewriteQueryIfNeed(query, traceId);
        // 召回
        List<Document> recalledDocs = recallDocuments(processedQuery, knowledgeBaseId, topK, traceId);
        // 知识图谱召回
        List<Document> kgDocs = retrieveFromKnowledgeGraph(processedQuery, knowledgeBaseId, traceId);
        // 合并召回结果
        List<Document> allRecalledDocs = mergeRecalledDocs(recalledDocs, kgDocs, traceId);
        // rerank
        List<Document> rerankedDocs = rerankDocuments(processedQuery, allRecalledDocs, topK, traceId);
        // 返回结果
        logResultDetails(rerankedDocs, traceId);

        log.info("[{}] [高级检索] 完成，总耗时: {}ms, 最终返回文档数: {}",
                traceId, Duration.between(start, Instant.now()).toMillis(), rerankedDocs.size());

        return rerankedDocs;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String rewriteQueryIfNeed(String query, String traceId) {
        Instant rewriteStart = Instant.now();
        QueryRewriterService.RewrittenQuery rewrittenQuery = queryRewriterService.rewriteQuery(query);
        String enhancedQuery = rewrittenQuery.getEnhancedQuery();
        Instant rewriteEnd = Instant.now();

        log.info("[{}] [高级检索] 查询改写完成，耗时: {}ms, 原查询: '{}', 增强查询: '{}'",
                traceId, Duration.between(rewriteStart, rewriteEnd).toMillis(), query, enhancedQuery);

        return enhancedQuery;
    }

    private List<Document> recallDocuments(String query, String knowledgeBaseId, int topK, String traceId) {
        int recallCount = topK * retrievalConfig.getMultiRetrieve().getVectorRecallMultiplier();

        log.info("[{}] [高级检索] 开始多路召回，召回数量: {}", traceId, recallCount);
        Instant recallStart = Instant.now();

        List<Document> recalledDocs = multiRetriever.retrieve(query, knowledgeBaseId, recallCount);

        Instant recallEnd = Instant.now();
        log.info("[{}] [高级检索] 向量+关键词召回完成，耗时: {}ms, 召回文档数: {}",
                traceId, Duration.between(recallStart, recallEnd).toMillis(), recalledDocs.size());

        return recalledDocs;
    }

    private List<Document> retrieveFromKnowledgeGraph(String query, String knowledgeBaseId, String traceId) {
        log.debug("[{}] [知识图谱检索] 开始从知识图谱检索: '{}'", traceId, query);

        try {
            List<String> entities = extractEntitiesFromQuery(query);
            log.debug("[{}] [知识图谱检索] 从查询中提取实体: {}", traceId, entities);

            return buildKnowledgeGraphDocuments(entities, knowledgeBaseId, traceId);
        } catch (Exception e) {
            log.error("[{}] [知识图谱检索] 检索失败", traceId, e);
            return Collections.emptyList();
        }
    }

    private List<String> extractEntitiesFromQuery(String query) {
        List<String> entities = extractWithComplexKeywords(query);

        if (entities.isEmpty()) {
            entities = extractWithSimpleKeywords(query);
        }

        return entities;
    }

    private List<String> extractWithComplexKeywords(String query) {
        List<String> entities = new ArrayList<>();

        for (String keyword : COMPLEX_KEYWORDS) {
            int index = query.indexOf(keyword);
            if (index > 0) {
                String entity = query.substring(0, index + keyword.length());
                entities.add(entity);
            }
        }

        return entities;
    }

    private List<String> extractWithSimpleKeywords(String query) {
        List<String> entities = new ArrayList<>();

        for (String keyword : SIMPLE_KEYWORDS) {
            if (query.contains(keyword)) {
                entities.add(keyword);
            }
        }

        return entities;
    }

    private List<Document> buildKnowledgeGraphDocuments(List<String> entities, String knowledgeBaseId, String traceId) {
        List<Document> kgDocs = new ArrayList<>();

        for (String entity : entities) {
            List<String> docUuids = documentKnowledgeGraphService.getDocumentsByEntity(entity, knowledgeBaseId);
            log.debug("[{}] [知识图谱检索] 实体 '{}' 相关文档: {}", traceId, entity, docUuids);

            docUuids.forEach(docUuid -> {
                Document kgDoc = new Document(docUuid);
                kgDoc.getMetadata().put("document_id", docUuid);
                kgDoc.getMetadata().put("document_name", "实体: " + entity);
                kgDoc.getMetadata().put("kg_score", 1.0);
                kgDoc.getMetadata().put("kg_entity", entity);
                kgDoc.getMetadata().put("retrieval_source", "knowledge_graph");
                kgDocs.add(kgDoc);
            });
        }

        log.debug("[{}] [知识图谱检索] 完成，返回文档数: {}", traceId, kgDocs.size());
        return kgDocs;
    }

    private List<Document> mergeRecalledDocs(List<Document> recalledDocs, List<Document> kgDocs, String traceId) {
        List<Document> allRecalledDocs = new ArrayList<>(recalledDocs);

        kgDocs.forEach(doc -> {
            if (allRecalledDocs.stream().noneMatch(d -> getDocumentId(d).equals(getDocumentId(doc)))) {
                allRecalledDocs.add(doc);
                log.debug("[{}] [高级检索] 添加知识图谱召回文档: {}", traceId, getDocumentName(doc));
            }
        });

        log.info("[{}] [高级检索] 融合召回完成，总候选数: {}", traceId, allRecalledDocs.size());
        return allRecalledDocs;
    }

    private List<Document> rerankDocuments(String query, List<Document> docs, int topK, String traceId) {
        log.info("[{}] [高级检索] 开始重排序，输入文档数: {}, 目标返回数: {}",
                traceId, docs.size(), topK);

        Instant rerankStart = Instant.now();
        List<Document> rerankedDocs = reranker.rerankByRules(query, docs, topK);
        Instant rerankEnd = Instant.now();

        log.info("[{}] [高级检索] 重排序完成，耗时: {}ms, 返回前 {} 个结果",
                traceId, Duration.between(rerankStart, rerankEnd).toMillis(), rerankedDocs.size());

        return rerankedDocs;
    }

    private void logResultDetails(List<Document> documents, String traceId) {
        log.debug("[{}] [高级检索] 重排序结果详情:", traceId);

        int logCount = Math.min(documents.size(), 5);
        for (int i = 0; i < logCount; i++) {
            Document doc = documents.get(i);
            String docName = getDocumentName(doc);
            Double rrfScore = getMetadataDouble(doc, "rrf_score");
            Double ruleScore = getMetadataDouble(doc, "rule_score");
            Double kgScore = getMetadataDouble(doc, "kg_score");

            log.debug("[{}] [高级检索] 文档{}: {}, RRF分数: {:.3f}, 规则分数: {:.3f}, 知识图谱分数: {:.3f}",
                    traceId, i + 1, docName, rrfScore, ruleScore, kgScore);
        }
    }

    private Double getMetadataDouble(Document doc, String key) {
        Object value = doc.getMetadata().get(key);
        return value instanceof Double ? (Double) value : 0.0;
    }



    public List<Document> smartRetrieve(String query, String knowledgeBaseId, int topK) {
        String traceId = generateTraceId();
        Instant start = Instant.now();

        log.info("[{}] [智能检索] 开始，查询: '{}', 知识库: {}, topK: {}",
                traceId, query, knowledgeBaseId, topK);

        String processedQuery = rewriteQueryIfNeed(query, traceId);

        boolean useHyde = hydeService.shouldUseHyde(processedQuery);
        log.info("[{}] [智能检索] 是否使用HyDE: {}", traceId, useHyde);

        List<Document> result;
        if (useHyde) {
            result = executeHydeRetrieval(processedQuery, knowledgeBaseId, topK, traceId, start);
        } else {
            result = executeStandardRetrieval(processedQuery, knowledgeBaseId, topK, traceId, start);
        }

        log.info("[{}] [智能检索] 完成，总耗时: {}ms, 最终返回文档数: {}",
                traceId, Duration.between(start, Instant.now()).toMillis(), result.size());

        return result;
    }

    private List<Document> executeHydeRetrieval(String processedQuery, String knowledgeBaseId,
                                                int topK, String traceId, Instant start) {
        log.info("[{}] [智能检索] 使用HyDE检索", traceId);
        Instant hydeStart = Instant.now();

        List<Document> result = hydeService.retrieveWithHyde(processedQuery, knowledgeBaseId, topK);

        Instant hydeEnd = Instant.now();
        log.info("[{}] [智能检索] HyDE检索完成，耗时: {}ms, 返回文档数: {}",
                traceId, Duration.between(hydeStart, hydeEnd).toMillis(), result.size());

        return result;
    }

    private List<Document> executeStandardRetrieval(String processedQuery, String knowledgeBaseId,
                                                    int topK, String traceId, Instant start) {
        log.info("[{}] [智能检索] 使用普通高级检索", traceId);
        return retrieve(processedQuery, knowledgeBaseId, topK);
    }



    public double estimateRetrievalQuality(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0.0;
        }

        double topScore = documents.stream()
                .mapToDouble(doc -> getMetadataDouble(doc, "rule_score"))
                .max()
                .orElse(0.0);

        return Math.min(topScore / retrievalConfig.getVector().getSimilarityThreshold() * 10, 1.0);
    }

    private String getDocumentId(Document doc) {
        Object docId = doc.getMetadata().get("document_id");
        return docId != null ? docId.toString() : "unknown";
    }

    private String getDocumentName(Document doc) {
        Object docName = doc.getMetadata().get("document_name");
        return docName != null ? docName.toString() : "unknown";
    }
}
