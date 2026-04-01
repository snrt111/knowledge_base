package com.snrt.knowledgebase.infrastructure.retrieval;

import com.snrt.knowledgebase.common.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 多路召回检索器
 * 
 * 实现多路召回（Multi-Retrieval）策略，结合向量检索和全文检索的优势，
 * 使用RRF（Reciprocal Rank Fusion）算法融合不同检索源的结果，提升检索质量。
 * 
 * 核心特性：
 * <ul>
 *   <li>并行执行向量检索和BM25全文检索，提高检索效率</li>
 *   <li>使用RRF算法融合多路召回结果，保证结果多样性</li>
 *   <li>支持知识库过滤，确保结果相关性</li>
 *   <li>异常降级策略，保证系统可用性</li>
 *   <li>详细的日志跟踪，便于问题排查</li>
 * </ul>
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class MultiRetriever {

    private final VectorStore vectorStore;
    private final FullTextRetriever fullTextRetriever;

    /**
     * 构造函数
     * 
     * @param vectorStore 向量存储服务
     * @param fullTextRetriever 全文检索器
     */
    public MultiRetriever(VectorStore vectorStore, FullTextRetriever fullTextRetriever) {
        this.vectorStore = vectorStore;
        this.fullTextRetriever = fullTextRetriever;
    }

    /**
     * 执行多路召回检索
     * 
     * 并行执行向量检索和BM25全文检索，使用RRF算法融合结果。
     * 当检索失败时，自动降级为纯向量检索。
     * 
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID，用于过滤结果
     * @param topK 返回结果数量
     * @return 融合后的文档列表
     */
    public List<Document> retrieve(String query, String knowledgeBaseId, int topK) {
        String traceId = buildTraceId();
        Instant start = Instant.now();
        log.info("[{}] [多路召回] 开始检索，查询: '{}', 知识库: {}, topK: {}", traceId, query, knowledgeBaseId, topK);

        CompletableFuture<List<Document>> vectorFuture = submitVectorRetrievalTask(query, knowledgeBaseId, topK * 2, traceId);
        CompletableFuture<List<Document>> fullTextFuture = submitFullTextRetrievalTask(query, knowledgeBaseId, topK, traceId);

        CompletableFuture.allOf(vectorFuture, fullTextFuture).join();

        try {
            List<Document> vectorResults = vectorFuture.get();
            List<Document> fullTextResults = fullTextFuture.get();

            log.info("[{}] [多路召回] 检索结果汇总: 向量检索: {} 个, BM25全文检索: {} 个",
                    traceId, vectorResults.size(), fullTextResults.size());

            List<Document> fusedResults = performRrfFusion(vectorResults, fullTextResults, topK, traceId);

            Duration totalDuration = Duration.between(start, Instant.now());
            log.info("[{}] [多路召回] 完成，总耗时: {}ms, 最终返回文档数: {}",
                    traceId, totalDuration.toMillis(), fusedResults.size());
            return fusedResults;

        } catch (Exception e) {
            log.error("[{}] [多路召回] 检索失败: {}, 执行降级策略", traceId, e.getMessage(), e);
            List<Document> fallbackResults = retrieveByVector(query, knowledgeBaseId, topK);
            log.info("[{}] [多路召回] 降级完成，返回向量检索结果数: {}", traceId, fallbackResults.size());
            return fallbackResults;
        }
    }

    /**
     * 构建追踪ID
     * 
     * @return 8位UUID字符串
     */
    private String buildTraceId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 提交向量检索异步任务
     * 
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID
     * @param topK 召回数量
     * @param traceId 追踪ID
     * @return 异步任务Future
     */
    private CompletableFuture<List<Document>> submitVectorRetrievalTask(String query, String knowledgeBaseId, int topK, String traceId) {
        log.info("[{}] [多路召回] 启动向量检索任务，召回数量: {}", traceId, topK);
        return CompletableFuture.supplyAsync(() -> executeVectorRetrieval(query, knowledgeBaseId, topK, traceId));
    }

    /**
     * 执行向量检索
     * 
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID
     * @param topK 召回数量
     * @param traceId 追踪ID
     * @return 检索结果
     */
    private List<Document> executeVectorRetrieval(String query, String knowledgeBaseId, int topK, String traceId) {
        Instant start = Instant.now();
        List<Document> results = retrieveByVector(query, knowledgeBaseId, topK);
        Duration duration = Duration.between(start, Instant.now());
        log.info("[{}] [多路召回] 向量检索完成，耗时: {}ms, 召回文档数: {}",
                traceId, duration.toMillis(), results.size());
        return results;
    }

    /**
     * 提交BM25全文检索异步任务
     * 
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID
     * @param topK 召回数量
     * @param traceId 追踪ID
     * @return 异步任务Future
     */
    private CompletableFuture<List<Document>> submitFullTextRetrievalTask(String query, String knowledgeBaseId, int topK, String traceId) {
        log.info("[{}] [多路召回] 启动BM25全文检索任务，召回数量: {}", traceId, topK);
        return CompletableFuture.supplyAsync(() -> executeFullTextRetrieval(query, knowledgeBaseId, topK, traceId));
    }

    /**
     * 执行BM25全文检索
     * 
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID
     * @param topK 召回数量
     * @param traceId 追踪ID
     * @return 检索结果
     */
    private List<Document> executeFullTextRetrieval(String query, String knowledgeBaseId, int topK, String traceId) {
        Instant start = Instant.now();
        List<Document> results = retrieveByFullText(query, knowledgeBaseId, topK);
        Duration duration = Duration.between(start, Instant.now());
        log.info("[{}] [多路召回] BM25全文检索完成，耗时: {}ms, 召回文档数: {}",
                traceId, duration.toMillis(), results.size());
        return results;
    }

    /**
     * 执行RRF（Reciprocal Rank Fusion）融合排序
     * 
     * 将向量检索和BM25检索的结果进行融合，使用RRF算法计算综合分数。
     * RRF算法公式：score = sum(1 / (K + rank_i))，其中K为常数，rank_i为在第i个检索源中的排名。
     * 
     * @param vectorResults 向量检索结果
     * @param fullTextResults BM25检索结果
     * @param topK 返回结果数量
     * @param traceId 追踪ID
     * @return 融合后的文档列表
     */
    private List<Document> performRrfFusion(List<Document> vectorResults, List<Document> fullTextResults, int topK, String traceId) {
        log.info("[{}] [多路召回] 开始RRF融合排序，输入文档数: {}（向量）+ {}（全文）= {} 个",
                traceId, vectorResults.size(), fullTextResults.size(),
                vectorResults.size() + fullTextResults.size());
        Instant start = Instant.now();
        List<Document> fusedResults = reciprocalRankFusion(vectorResults, fullTextResults, topK);
        Duration duration = Duration.between(start, Instant.now());
        log.info("[{}] [多路召回] 融合排序完成，耗时: {}ms, 融合后结果: {} 个",
                traceId, duration.toMillis(), fusedResults.size());
        logFusionResultsDetails(fusedResults, traceId);
        return fusedResults;
    }

    /**
     * 记录融合结果详情（调试级别）
     * 
     * @param fusedResults 融合结果
     * @param traceId 追踪ID
     */
    private void logFusionResultsDetails(List<Document> fusedResults, String traceId) {
        if (log.isDebugEnabled()) {
            log.debug("[{}] [多路召回] 融合结果详情:", traceId);
            for (int i = 0; i < Math.min(fusedResults.size(), 5); i++) {
                Document doc = fusedResults.get(i);
                String docName = getDocumentName(doc);
                Double rrfScore = (Double) doc.getMetadata().get("rrf_score");
                log.debug("[{}] [多路召回] 文档{}: {}, RRF分数: {:.3f}",
                        traceId, i + 1, docName, rrfScore);
            }
        }
    }

    /**
     * 通过向量检索获取文档
     * 
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID
     * @param topK 召回数量
     * @return 检索结果
     */
    private List<Document> retrieveByVector(String query, String knowledgeBaseId, int topK) {
        String traceId = buildTraceId();
        log.debug("[{}] [向量检索] 开始，查询: '{}', 知识库: {}, topK: {}, 相似度阈值: 0.3",
                traceId, query, knowledgeBaseId, topK);

        try {
            SearchRequest searchRequest = buildSearchRequest(query, topK);
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.debug("[{}] [向量检索] 原始结果数: {}", traceId, results.size());

            List<Document> filteredResults = filterDocumentsByKnowledgeBase(results, knowledgeBaseId, traceId);
            log.debug("[{}] [向量检索] 过滤后结果数: {} (知识库: {})",
                    traceId, filteredResults.size(), knowledgeBaseId);

            logVectorSearchResults(filteredResults, traceId);

            return filteredResults;

        } catch (Exception e) {
            log.error("[{}] [向量检索] 失败: {}", traceId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建搜索请求
     * 
     * @param query 用户查询
     * @param topK 召回数量
     * @return 搜索请求
     */
    private SearchRequest buildSearchRequest(String query, int topK) {
        return SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.3)
                .build();
    }

    /**
     * 按知识库过滤文档
     * 
     * @param results 检索结果
     * @param knowledgeBaseId 知识库ID
     * @param traceId 追踪ID
     * @return 过滤后的文档列表
     */
    private List<Document> filterDocumentsByKnowledgeBase(List<Document> results, String knowledgeBaseId, String traceId) {
        List<Document> filteredResults = results.stream()
                .filter(doc -> isFromKnowledgeBase(doc, knowledgeBaseId))
                .collect(Collectors.toList());
        return filteredResults;
    }

    /**
     * 记录向量检索结果详情（调试级别）
     * 
     * @param filteredResults 过滤后的结果
     * @param traceId 追踪ID
     */
    private void logVectorSearchResults(List<Document> filteredResults, String traceId) {
        for (int i = 0; i < Math.min(filteredResults.size(), 3); i++) {
            Document doc = filteredResults.get(i);
            String docName = getDocumentName(doc);
            Object distance = doc.getMetadata().get("distance");
            double similarity = 1.0 - (distance instanceof Number ? ((Number) distance).doubleValue() : 0.0);
            log.debug("[{}] [向量检索] 文档{}: {}, 相似度: {:.3f}",
                    traceId, i + 1, docName, similarity);
        }
    }

    /**
     * 通过BM25全文检索获取文档
     * 
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID
     * @param topK 召回数量
     * @return 检索结果
     */
    private List<Document> retrieveByFullText(String query, String knowledgeBaseId, int topK) {
        String traceId = buildTraceId();
        log.debug("[{}] [BM25全文检索] 开始，查询: '{}', 知识库: {}, topK: {}",
                traceId, query, knowledgeBaseId, topK);

        List<Document> results = fullTextRetriever.search(query, knowledgeBaseId, topK);
        log.debug("[{}] [BM25全文检索] 完成，返回结果数: {}", traceId, results.size());

        logFullTextSearchResults(results, traceId);

        return results;
    }

    /**
     * 记录BM25检索结果详情（调试级别）
     * 
     * @param results 检索结果
     * @param traceId 追踪ID
     */
    private void logFullTextSearchResults(List<Document> results, String traceId) {
        for (int i = 0; i < Math.min(results.size(), 3); i++) {
            Document doc = results.get(i);
            String docName = getDocumentName(doc);
            log.debug("[{}] [BM25全文检索] 文档{}: {}", traceId, i + 1, docName);
        }
    }

    /**
     * 执行RRF（Reciprocal Rank Fusion）融合算法
     * 
     * RRF算法通过倒数排名累加的方式融合多路检索结果，公式为：
     * score = sum(1 / (K + rank_i))，其中K为常数（默认60），rank_i为文档在第i个检索源中的排名。
     * 
     * @param vectorResults 向量检索结果
     * @param keywordResults 关键词检索结果
     * @param topK 返回结果数量
     * @return 融合后的文档列表
     */
    private List<Document> reciprocalRankFusion(
            List<Document> vectorResults,
            List<Document> keywordResults,
            int topK) {

        final int K = 60;
        Map<String, Document> docMap = new HashMap<>();
        Map<String, Double> scoreMap = new HashMap<>();

        processRetrievalResults(vectorResults, K, docMap, scoreMap);
        processRetrievalResults(keywordResults, K, docMap, scoreMap);

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    Document doc = docMap.get(entry.getKey());
                    doc.getMetadata().put("rrf_score", entry.getValue());
                    return doc;
                })
                .collect(Collectors.toList());
    }

    /**
     * 处理检索结果，计算RRF分数
     * 
     * @param results 检索结果列表
     * @param K RRF算法常数
     * @param docMap 文档ID到文档的映射
     * @param scoreMap 文档ID到RRF分数的映射
     */
    private void processRetrievalResults(List<Document> results, int K,
                                         Map<String, Document> docMap, Map<String, Double> scoreMap) {
        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            String docId = getDocumentId(doc);
            docMap.put(docId, doc);
            double score = 1.0 / (K + i + 1);
            scoreMap.merge(docId, score, Double::sum);
        }
    }

    /**
     * 检查文档是否属于指定知识库
     * 
     * @param doc 文档
     * @param knowledgeBaseId 知识库ID
     * @return 是否属于指定知识库
     */
    private boolean isFromKnowledgeBase(Document doc, String knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return true;
        }
        Object kbId = doc.getMetadata().get(Constants.VectorStore.METADATA_KNOWLEDGE_BASE_ID);
        return kbId != null && kbId.equals(knowledgeBaseId);
    }

    /**
     * 获取文档名称
     * 
     * @param doc 文档
     * @return 文档名称
     */
    private String getDocumentName(Document doc) {
        Object docName = doc.getMetadata().get(Constants.VectorStore.METADATA_DOCUMENT_NAME);
        return docName != null ? docName.toString() : "未知文档";
    }

    /**
     * 获取文档唯一标识符
     * 
     * @param doc 文档
     * @return 文档ID（格式：documentId_chunkId 或 文本哈希码）
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
