package com.snrt.knowledgebase.infrastructure.retrieval;

import com.snrt.knowledgebase.config.RetrievalConfig;
import com.snrt.knowledgebase.domain.document.entity.VectorDocument;
import com.snrt.knowledgebase.domain.document.repository.VectorStoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全文检索器 (BM25优化版)
 *
 * 基于 PostgreSQL 全文检索实现，支持：
 * 1. 中文分词 (zhparser)
 * 2. BM25 相关性评分 (ts_rank)
 * 3. 多字段加权检索
 * 4. 查询扩展（同义词、模糊匹配）
 *
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class FullTextRetriever {

    private final VectorStoreRepository vectorStoreRepository;
    private final RetrievalConfig retrievalConfig;

    public FullTextRetriever(VectorStoreRepository vectorStoreRepository, RetrievalConfig retrievalConfig) {
        this.vectorStoreRepository = vectorStoreRepository;
        this.retrievalConfig = retrievalConfig;
    }

    /**
     * 执行 BM25 全文检索（使用配置默认选项）
     *
     * @param query 查询关键词
     * @param knowledgeBaseId 知识库ID
     * @param topK 返回结果数量
     * @return 按 BM25 分数排序的文档列表
     */
    public List<Document> search(String query, String knowledgeBaseId, int topK) {
        // 从配置创建默认选项
        SearchOptions options = new SearchOptions();
        RetrievalConfig.BM25Config bm25Config = retrievalConfig.getBm25();
        options.setRequireAllTerms(bm25Config.isRequireAllTerms());
        options.setPrefixMatch(bm25Config.isPrefixMatch());
        options.setExpandSynonyms(bm25Config.isExpandSynonyms());
        options.setFuzzyMatch(bm25Config.isFuzzyMatch());
        options.setUseNormalization(bm25Config.isUseNormalization());

        return searchWithOptions(query, knowledgeBaseId, topK, options);
    }

    /**
     * 执行带选项的全文检索
     *
     * @param query 查询关键词
     * @param knowledgeBaseId 知识库ID
     * @param topK 返回结果数量
     * @param options 检索选项
     * @return 按 BM25 分数排序的文档列表
     */
    public List<Document> searchWithOptions(String query, String knowledgeBaseId, int topK, SearchOptions options) {
        try {
            log.debug("[BM25全文检索] 查询: {}, 知识库: {}, 选项: {}", query, knowledgeBaseId, options);

            // 1. 查询预处理
            String processedQuery = preprocessQuery(query, options);
            if (processedQuery.isEmpty()) {
                log.warn("[BM25全文检索] 查询为空，返回空结果");
                return Collections.emptyList();
            }

            // 2. 直接使用处理后的查询（plainto_tsquery会自动处理）
            log.debug("[BM25全文检索] 原始查询: '{}', 处理后: '{}'", query, processedQuery);

            if (processedQuery.isEmpty()) {
                log.warn("[BM25全文检索] 处理后查询为空，返回空结果");
                return Collections.emptyList();
            }

            // 3. 执行检索
            // 归一化模式: 1=长度归一化, 2=唯一词数归一化, 8=词频归一化, 0=不归一化
            int normalizationMode = options.useNormalization() ? 1 : 0;
            List<VectorDocument> results;
            if (knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
                results = vectorStoreRepository.searchByBM25AndKnowledgeBase(
                    processedQuery, knowledgeBaseId, topK, normalizationMode
                );
            } else {
                results = vectorStoreRepository.searchByBM25(
                    processedQuery, topK, normalizationMode
                );
            }

            // 4. 过滤匹配度低于阈值的结果，并转换为 Document
            // 注意：ts_rank_cd 返回的分数通常较低（0.1 左右即为有效匹配），阈值不宜设置过高
            double minScore = 0.05;
            List<Document> documents = results.stream()
                    .filter(doc -> doc.getRank() != null && doc.getRank() >= minScore)
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());

            // 5. 如果启用模糊匹配，进行二次过滤
            if (options.isFuzzyMatch()) {
                documents = applyFuzzyMatch(documents, query, topK);
            }

            log.info("[BM25全文检索] 查询: '{}', 匹配度>={}%的结果: {} 个", query, (int)(minScore * 100), documents.size());
            return documents;

        } catch (Exception e) {
            log.error("[BM25全文检索] 失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询预处理
     */
    private String preprocessQuery(String query, SearchOptions options) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        String processed = query.trim();

        // 移除特殊字符
        processed = processed.replaceAll("[<>'\"]", "");

        // 如果启用查询扩展，添加同义词
        if (options.isExpandSynonyms()) {
            processed = expandQueryWithSynonyms(processed);
        }

        return processed;
    }

    /**
     * 构建 PostgreSQL tsquery
     */
    private String buildTsQuery(String query, SearchOptions options) {
        // 分词处理
        String[] tokens = query.split("\\s+");

        if (tokens.length == 0) {
            return "";
        }

        // 构建 tsquery
        StringBuilder tsQuery = new StringBuilder();
        String operator = options.isRequireAllTerms() ? " & " : " | ";

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.isEmpty()) continue;

            // 清理token中的特殊字符，只保留字母、数字和中文
            token = token.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
            if (token.isEmpty()) continue;

            // 对token进行转义，防止tsquery语法错误
            token = escapeTsQueryToken(token);

            // 添加前缀匹配支持
            if (options.isPrefixMatch()) {
                token = token + ":*";
            }

            if (i > 0) {
                tsQuery.append(operator);
            }
            tsQuery.append(token);
        }

        return tsQuery.toString();
    }

    /**
     * 转义 tsquery 特殊字符
     */
    private String escapeTsQueryToken(String token) {
        // PostgreSQL tsquery 特殊字符: & | ! ( ) : * \
        return token.replace("\\", "\\\\")
                    .replace("&", "\\&")
                    .replace("|", "\\|")
                    .replace("!", "\\!")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace(":", "\\:");
    }

    /**
     * 查询扩展 - 添加同义词
     */
    private String expandQueryWithSynonyms(String query) {
        // 简单的同义词扩展示例
        Map<String, List<String>> synonymMap = Map.of(
            "电脑", List.of("计算机", "笔记本", "台式机"),
            "手机", List.of("移动电话", "智能手机", "iPhone"),
            "问题", List.of("疑问", "故障", "错误")
        );

        StringBuilder expanded = new StringBuilder(query);

        for (Map.Entry<String, List<String>> entry : synonymMap.entrySet()) {
            if (query.contains(entry.getKey())) {
                for (String synonym : entry.getValue()) {
                    expanded.append(" ").append(synonym);
                }
            }
        }

        return expanded.toString();
    }

    /**
     * 应用模糊匹配过滤
     */
    private List<Document> applyFuzzyMatch(List<Document> documents, String query, int topK) {
        // 计算编辑距离或相似度进行二次排序
        return documents.stream()
                .sorted((d1, d2) -> {
                    double score1 = calculateFuzzyScore(d1.getText(), query);
                    double score2 = calculateFuzzyScore(d2.getText(), query);
                    return Double.compare(score2, score1);
                })
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 计算模糊匹配分数（简化版）
     */
    private double calculateFuzzyScore(String text, String query) {
        if (text == null || query == null) return 0.0;

        // 包含完整查询词得高分
        if (text.toLowerCase().contains(query.toLowerCase())) {
            return 1.0;
        }

        // 计算词匹配率
        String[] queryWords = query.toLowerCase().split("\\s+");
        String lowerText = text.toLowerCase();

        int matchCount = 0;
        for (String word : queryWords) {
            if (lowerText.contains(word)) {
                matchCount++;
            }
        }

        return (double) matchCount / queryWords.length;
    }

    /**
     * 将 VectorDocument 转换为 Spring AI Document
     */
    private Document convertToDocument(VectorDocument vectorDoc) {
        Document doc = new Document(vectorDoc.getContent());

        // 复制 metadata
        if (vectorDoc.getMetadata() != null) {
            vectorDoc.getMetadata().forEach((key, value) -> {
                if (value != null) {
                    doc.getMetadata().put(key, value);
                }
            });
        }

        // 添加文档ID和名称
        if (vectorDoc.getDocumentId() != null) {
            doc.getMetadata().put("document_id", vectorDoc.getDocumentId());
        }
        if (vectorDoc.getKnowledgeBaseId() != null) {
            doc.getMetadata().put("knowledge_base_id", vectorDoc.getKnowledgeBaseId());
        }
        doc.getMetadata().put("document_name", vectorDoc.getDocumentName());
        doc.getMetadata().put("knowledge_base_name", vectorDoc.getKnowledgeBaseName());

        // 标记为全文检索结果
        doc.getMetadata().put("retrieval_type", "fulltext");

        return doc;
    }

    /**
     * 检索选项
     */
    public static class SearchOptions {
        private boolean requireAllTerms = false;  // 是否要求所有词都匹配
        private boolean prefixMatch = true;       // 是否启用前缀匹配
        private boolean expandSynonyms = false;   // 是否扩展同义词
        private boolean fuzzyMatch = false;       // 是否启用模糊匹配
        private boolean useNormalization = true;  // 是否使用文档长度归一化

        public static SearchOptions defaults() {
            return new SearchOptions();
        }

        public static SearchOptions strict() {
            SearchOptions options = new SearchOptions();
            options.requireAllTerms = true;
            return options;
        }

        public static SearchOptions fuzzy() {
            SearchOptions options = new SearchOptions();
            options.fuzzyMatch = true;
            options.prefixMatch = true;
            return options;
        }

        public static SearchOptions expanded() {
            SearchOptions options = new SearchOptions();
            options.expandSynonyms = true;
            return options;
        }

        // Getters and Setters
        public boolean isRequireAllTerms() { return requireAllTerms; }
        public void setRequireAllTerms(boolean requireAllTerms) { this.requireAllTerms = requireAllTerms; }

        public boolean isPrefixMatch() { return prefixMatch; }
        public void setPrefixMatch(boolean prefixMatch) { this.prefixMatch = prefixMatch; }

        public boolean isExpandSynonyms() { return expandSynonyms; }
        public void setExpandSynonyms(boolean expandSynonyms) { this.expandSynonyms = expandSynonyms; }

        public boolean isFuzzyMatch() { return fuzzyMatch; }
        public void setFuzzyMatch(boolean fuzzyMatch) { this.fuzzyMatch = fuzzyMatch; }

        public boolean useNormalization() { return useNormalization; }
        public void setUseNormalization(boolean useNormalization) { this.useNormalization = useNormalization; }

        @Override
        public String toString() {
            return String.format("SearchOptions{allTerms=%s, prefix=%s, synonyms=%s, fuzzy=%s, norm=%s}",
                    requireAllTerms, prefixMatch, expandSynonyms, fuzzyMatch, useNormalization);
        }
    }
}
