package com.snrt.knowledgebase.service.retrieval;

import com.snrt.knowledgebase.entity.VectorDocument;
import com.snrt.knowledgebase.repository.VectorStoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关键词检索器
 *
 * 基于 PostgreSQL 全文检索实现关键词匹配
 * 使用 JPA 方式访问数据，与项目其他 Repository 保持一致
 *
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class KeywordRetriever {

    private final VectorStoreRepository vectorStoreRepository;

    public KeywordRetriever(VectorStoreRepository vectorStoreRepository) {
        this.vectorStoreRepository = vectorStoreRepository;
    }

    /**
     * 执行关键词检索
     *
     * @param query 查询关键词
     * @param knowledgeBaseId 知识库ID
     * @param topK 返回结果数量
     * @return 匹配的文档列表
     */
    public List<Document> search(String query, String knowledgeBaseId, int topK) {
        try {
            log.debug("[关键词检索] 查询: {}, 知识库: {}", query, knowledgeBaseId);

            // 构建全文检索查询
            String tsQuery = buildTsQuery(query);

            if (tsQuery.isEmpty()) {
                log.warn("[关键词检索] 查询为空，返回空结果");
                return Collections.emptyList();
            }

            // 使用 JPA Repository 进行查询
            List<VectorDocument> results;
            if (knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
                results = vectorStoreRepository.searchByFullTextAndKnowledgeBase(tsQuery, knowledgeBaseId, topK);
            } else {
                results = vectorStoreRepository.searchByFullText(tsQuery, topK);
            }

            // 转换为 Spring AI Document
            List<Document> documents = results.stream()
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());

            log.info("[关键词检索] 返回 {} 个结果", documents.size());
            return documents;

        } catch (Exception e) {
            log.error("[关键词检索] 失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
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

        // 添加文档ID和名称（确保一致性）
        if (vectorDoc.getDocumentId() != null) {
            doc.getMetadata().put("document_id", vectorDoc.getDocumentId());
        }
        if (vectorDoc.getKnowledgeBaseId() != null) {
            doc.getMetadata().put("knowledge_base_id", vectorDoc.getKnowledgeBaseId());
        }
        doc.getMetadata().put("document_name", vectorDoc.getDocumentName());
        doc.getMetadata().put("knowledge_base_name", vectorDoc.getKnowledgeBaseName());

        return doc;
    }

    /**
     * 构建 PostgreSQL tsquery
     *
     * 将用户查询转换为 PostgreSQL 全文检索查询格式
     * 例如: "人工智能 发展" -> "人工智能 & 发展"
     */
    private String buildTsQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        // 分词并构建查询
        String[] tokens = query.trim().split("\\s+");

        return Arrays.stream(tokens)
                .filter(token -> !token.isEmpty())
                .map(this::escapeTsQueryToken)
                .collect(Collectors.joining(" & "));
    }

    /**
     * 转义特殊字符
     */
    private String escapeTsQueryToken(String token) {
        // PostgreSQL tsquery 特殊字符: ! & | ( )
        return token.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("&", "\\&")
                    .replace("|", "\\|")
                    .replace("!", "\\!")
                    .replace("(", "\\(")
                    .replace(")", "\\)");
    }

    /**
     * 检查 PostgreSQL 中文全文检索是否可用
     */
    public boolean isFullTextSearchAvailable() {
        try {
            Boolean result = vectorStoreRepository.isFullTextSearchAvailable();
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("[关键词检索] 中文全文检索不可用: {}", e.getMessage());
            return false;
        }
    }
}
