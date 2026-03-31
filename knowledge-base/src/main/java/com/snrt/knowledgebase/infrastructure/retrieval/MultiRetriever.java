package com.snrt.knowledgebase.infrastructure.retrieval;

import com.snrt.knowledgebase.common.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 多路召回检索器
 *
 * 实现向量检索 + BM25全文检索的混合召回策略，提升检索覆盖率
 *
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class MultiRetriever {

    private final VectorStore vectorStore;
    private final FullTextRetriever fullTextRetriever;

    public MultiRetriever(VectorStore vectorStore, FullTextRetriever fullTextRetriever) {
        this.vectorStore = vectorStore;
        this.fullTextRetriever = fullTextRetriever;
    }

    /**
     * 执行多路召回检索
     *
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID
     * @param topK 最终返回的结果数量
     * @return 召回的文档列表
     */
    public List<Document> retrieve(String query, String knowledgeBaseId, int topK) {
        String traceId = java.util.UUID.randomUUID().toString().substring(0, 8);
        java.time.Instant start = java.time.Instant.now();
        log.info("[{}] [多路召回] 开始检索，查询: '{}', 知识库: {}, topK: {}", traceId, query, knowledgeBaseId, topK);

        // 1. 向量检索（语义相似度）
        log.info("[{}] [多路召回] 启动向量检索任务，召回数量: {}", traceId, topK * 2);
        CompletableFuture<List<Document>> vectorFuture = CompletableFuture.supplyAsync(() -> {
            java.time.Instant vectorStart = java.time.Instant.now();
            List<Document> results = retrieveByVector(query, knowledgeBaseId, topK * 2);
            java.time.Duration vectorDuration = java.time.Duration.between(vectorStart, java.time.Instant.now());
            log.info("[{}] [多路召回] 向量检索完成，耗时: {}ms, 召回文档数: {}", 
                    traceId, vectorDuration.toMillis(), results.size());
            return results;
        });

        // 2. BM25全文检索（关键词匹配）
        log.info("[{}] [多路召回] 启动BM25全文检索任务，召回数量: {}", traceId, topK);
        CompletableFuture<List<Document>> fullTextFuture = CompletableFuture.supplyAsync(() -> {
            java.time.Instant fullTextStart = java.time.Instant.now();
            List<Document> results = retrieveByFullText(query, knowledgeBaseId, topK);
            java.time.Duration fullTextDuration = java.time.Duration.between(fullTextStart, java.time.Instant.now());
            log.info("[{}] [多路召回] BM25全文检索完成，耗时: {}ms, 召回文档数: {}", 
                    traceId, fullTextDuration.toMillis(), results.size());
            return results;
        });

        // 3. 合并结果
        log.info("[{}] [多路召回] 等待所有检索任务完成", traceId);
        CompletableFuture.allOf(vectorFuture, fullTextFuture).join();

        try {
            List<Document> vectorResults = vectorFuture.get();
            List<Document> fullTextResults = fullTextFuture.get();

            log.info("[{}] [多路召回] 检索结果汇总: 向量检索: {} 个, BM25全文检索: {} 个",
                traceId, vectorResults.size(), fullTextResults.size());

            // 4. 融合排序（RRF算法）
            log.info("[{}] [多路召回] 开始RRF融合排序，输入文档数: {}（向量）+ {}（全文）= {} 个", 
                    traceId, vectorResults.size(), fullTextResults.size(), 
                    vectorResults.size() + fullTextResults.size());
            java.time.Instant fusionStart = java.time.Instant.now();
            List<Document> fusedResults = reciprocalRankFusion(vectorResults, fullTextResults, topK);
            java.time.Duration fusionDuration = java.time.Duration.between(fusionStart, java.time.Instant.now());

            log.info("[{}] [多路召回] 融合排序完成，耗时: {}ms, 融合后结果: {} 个", 
                    traceId, fusionDuration.toMillis(), fusedResults.size());

            // 记录融合结果详情
            log.debug("[{}] [多路召回] 融合结果详情:", traceId);
            for (int i = 0; i < Math.min(fusedResults.size(), 5); i++) {
                Document doc = fusedResults.get(i);
                String docName = getDocumentName(doc);
                Double rrfScore = (Double) doc.getMetadata().get("rrf_score");
                log.debug("[{}] [多路召回] 文档{}: {}, RRF分数: {:.3f}", 
                        traceId, i+1, docName, rrfScore);
            }

            java.time.Duration totalDuration = java.time.Duration.between(start, java.time.Instant.now());
            log.info("[{}] [多路召回] 完成，总耗时: {}ms, 最终返回文档数: {}", 
                    traceId, totalDuration.toMillis(), fusedResults.size());
            return fusedResults;

        } catch (Exception e) {
            log.error("[{}] [多路召回] 检索失败: {}, 执行降级策略", traceId, e.getMessage(), e);
            // 降级：只返回向量检索结果
            log.info("[{}] [多路召回] 降级处理：只返回向量检索结果", traceId);
            List<Document> fallbackResults = retrieveByVector(query, knowledgeBaseId, topK);
            log.info("[{}] [多路召回] 降级完成，返回向量检索结果数: {}", traceId, fallbackResults.size());
            return fallbackResults;
        }
    }

    /**
     * 获取文档名称
     */
    private String getDocumentName(Document doc) {
        Object docName = doc.getMetadata().get(Constants.VectorStore.METADATA_DOCUMENT_NAME);
        return docName != null ? docName.toString() : "未知文档";
    }

    /**
     * 向量检索
     */
    private List<Document> retrieveByVector(String query, String knowledgeBaseId, int topK) {
        String traceId = java.util.UUID.randomUUID().toString().substring(0, 8);
        log.debug("[{}] [向量检索] 开始，查询: '{}', 知识库: {}, topK: {}, 相似度阈值: 0.3", 
                traceId, query, knowledgeBaseId, topK);

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(0.3)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.debug("[{}] [向量检索] 原始结果数: {}", traceId, results.size());
            
            // 过滤知识库
            List<Document> filteredResults = results.stream()
                    .filter(doc -> isFromKnowledgeBase(doc, knowledgeBaseId))
                    .collect(Collectors.toList());

            log.debug("[{}] [向量检索] 过滤后结果数: {} (知识库: {})", 
                    traceId, filteredResults.size(), knowledgeBaseId);

            // 记录前3个结果的相似度分数
            for (int i = 0; i < Math.min(filteredResults.size(), 3); i++) {
                Document doc = filteredResults.get(i);
                String docName = getDocumentName(doc);
                Object distance = doc.getMetadata().get("distance");
                double similarity = 1.0 - (distance instanceof Number ? ((Number) distance).doubleValue() : 0.0);
                log.debug("[{}] [向量检索] 文档{}: {}, 相似度: {:.3f}", 
                        traceId, i+1, docName, similarity);
            }

            return filteredResults;

        } catch (Exception e) {
            log.error("[{}] [向量检索] 失败: {}", traceId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * BM25全文检索
     */
    private List<Document> retrieveByFullText(String query, String knowledgeBaseId, int topK) {
        String traceId = java.util.UUID.randomUUID().toString().substring(0, 8);
        log.debug("[{}] [BM25全文检索] 开始，查询: '{}', 知识库: {}, topK: {}", 
                traceId, query, knowledgeBaseId, topK);

        List<Document> results = fullTextRetriever.search(query, knowledgeBaseId, topK);
        log.debug("[{}] [BM25全文检索] 完成，返回结果数: {}", traceId, results.size());

        // 记录前3个结果
        for (int i = 0; i < Math.min(results.size(), 3); i++) {
            Document doc = results.get(i);
            String docName = getDocumentName(doc);
            log.debug("[{}] [BM25全文检索] 文档{}: {}", traceId, i+1, docName);
        }

        return results;
    }

    /**
     * RRF (Reciprocal Rank Fusion) 融合排序算法
     * 
     * 公式: score = Σ(1 / (k + rank))
     * k 通常取 60
     */
    private List<Document> reciprocalRankFusion(
            List<Document> vectorResults, 
            List<Document> keywordResults, 
            int topK) {
        
        final int K = 60; // RRF常数
        Map<String, Document> docMap = new HashMap<>();
        Map<String, Double> scoreMap = new HashMap<>();

        // 处理向量检索结果
        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String docId = getDocumentId(doc);
            
            docMap.put(docId, doc);
            double score = 1.0 / (K + i + 1);
            scoreMap.merge(docId, score, Double::sum);
        }

        // 处理关键词检索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            Document doc = keywordResults.get(i);
            String docId = getDocumentId(doc);
            
            docMap.put(docId, doc);
            double score = 1.0 / (K + i + 1);
            scoreMap.merge(docId, score, Double::sum);
        }

        // 按分数排序并返回topK
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    Document doc = docMap.get(entry.getKey());
                    // 将融合分数存入metadata
                    doc.getMetadata().put("rrf_score", entry.getValue());
                    return doc;
                })
                .collect(Collectors.toList());
    }

    /**
     * 检查文档是否来自指定知识库
     */
    private boolean isFromKnowledgeBase(Document doc, String knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return true;
        }
        Object kbId = doc.getMetadata().get(Constants.VectorStore.METADATA_KNOWLEDGE_BASE_ID);
        return kbId != null && kbId.equals(knowledgeBaseId);
    }

    /**
     * 获取文档唯一标识
     */
    private String getDocumentId(Document doc) {
        Object docId = doc.getMetadata().get(Constants.VectorStore.METADATA_DOCUMENT_ID);
        Object chunkId = doc.getMetadata().get(Constants.VectorStore.METADATA_CHUNK_ID);
        
        if (docId != null && chunkId != null) {
            return docId + "_" + chunkId;
        }
        return doc.getText().hashCode() + "";
    }
}
