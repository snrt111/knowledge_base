package com.snrt.knowledgebase.service.retrieval;

import com.snrt.knowledgebase.constants.Constants;
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
 * 实现向量检索 + 关键词检索的混合召回策略，提升检索覆盖率
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class MultiRetriever {

    private final VectorStore vectorStore;
    private final KeywordRetriever keywordRetriever;

    public MultiRetriever(VectorStore vectorStore, KeywordRetriever keywordRetriever) {
        this.vectorStore = vectorStore;
        this.keywordRetriever = keywordRetriever;
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
        log.info("[多路召回] 开始检索，查询: {}, 知识库: {}, topK: {}", query, knowledgeBaseId, topK);

        // 1. 向量检索（语义相似度）
        CompletableFuture<List<Document>> vectorFuture = CompletableFuture.supplyAsync(() ->
            retrieveByVector(query, knowledgeBaseId, topK * 2)
        );

        // 2. 关键词检索（精确匹配）
        CompletableFuture<List<Document>> keywordFuture = CompletableFuture.supplyAsync(() ->
            retrieveByKeyword(query, knowledgeBaseId, topK)
        );

        // 3. 合并结果
        CompletableFuture.allOf(vectorFuture, keywordFuture).join();

        try {
            List<Document> vectorResults = vectorFuture.get();
            List<Document> keywordResults = keywordFuture.get();

            log.info("[多路召回] 向量检索: {} 个, 关键词检索: {} 个", 
                vectorResults.size(), keywordResults.size());

            // 4. 融合排序（RRF算法）
            List<Document> fusedResults = reciprocalRankFusion(vectorResults, keywordResults, topK);

            log.info("[多路召回] 融合后结果: {} 个", fusedResults.size());
            return fusedResults;

        } catch (Exception e) {
            log.error("[多路召回] 检索失败: {}", e.getMessage(), e);
            // 降级：只返回向量检索结果
            return retrieveByVector(query, knowledgeBaseId, topK);
        }
    }

    /**
     * 向量检索
     */
    private List<Document> retrieveByVector(String query, String knowledgeBaseId, int topK) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(0.3)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            // 过滤知识库
            return results.stream()
                    .filter(doc -> isFromKnowledgeBase(doc, knowledgeBaseId))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[向量检索] 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 关键词检索
     */
    private List<Document> retrieveByKeyword(String query, String knowledgeBaseId, int topK) {
        return keywordRetriever.search(query, knowledgeBaseId, topK);
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
