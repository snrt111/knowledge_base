package com.snrt.knowledgebase.infrastructure.retrieval;

import com.snrt.knowledgebase.config.RetrievalConfig;
import com.snrt.knowledgebase.domain.document.dto.DocumentSourceDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档来源转换器
 * 
 * 将检索结果中的Document列表转换为DocumentSourceDTO列表，用于前端展示。
 * 支持按文档ID聚合多个chunk，并计算综合相关性分数。
 * 
 * @author SNRT
 * @since 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentSourceConverter {

    private static final int DEFAULT_MAX_SNIPPET_COUNT = 3;

    /**
     * 将Document列表转换为DocumentSourceDTO列表
     * 
     * @param documents 检索结果文档列表
     * @param config 检索配置
     * @return 文档来源DTO列表
     */
    public static List<DocumentSourceDTO> convert(List<Document> documents, RetrievalConfig config) {
        return documents.stream()
                .collect(Collectors.groupingBy(DocumentSourceConverter::getDocumentId))
                .entrySet().stream()
                .map(entry -> createDocumentSource(entry.getKey(), entry.getValue(), config))
                .sorted((s1, s2) -> {
                    Double score1 = s1.getScore();
                    Double score2 = s2.getScore();
                    if (score1 == null && score2 == null) return 0;
                    if (score1 == null) return 1;
                    if (score2 == null) return -1;
                    return Double.compare(score2, score1);
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建DocumentSourceDTO
     * 
     * @param docId 文档ID
     * @param chunks 文档chunk列表
     * @param config 检索配置
     * @return 文档来源DTO
     */
    private static DocumentSourceDTO createDocumentSource(String docId, List<Document> chunks, RetrievalConfig config) {
        Document firstChunk = chunks.get(0);

        String docName = getMetadataString(firstChunk, "document_name", "未知文档");
        String kbName = getMetadataString(firstChunk, "knowledge_base_name", "未知知识库");

        double score = calculateScore(chunks, config);

        int maxSnippetCount = config.getVector().getDefaultTopK();
        if (maxSnippetCount <= 0) {
            maxSnippetCount = DEFAULT_MAX_SNIPPET_COUNT;
        }

        List<String> snippets = chunks.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isEmpty())
                .limit(maxSnippetCount)
                .collect(Collectors.toList());

        return DocumentSourceDTO.builder()
                .documentId("unknown".equals(docId) ? null : docId)
                .documentName(docName)
                .knowledgeBaseName(kbName)
                .score(score)
                .snippet(snippets.isEmpty() ? "" : snippets.get(0))
                .snippets(snippets)
                .build();
    }

    /**
     * 计算文档综合相关性分数
     * 
     * @param chunks 文档chunk列表
     * @param config 检索配置
     * @return 综合分数
     */
    private static double calculateScore(List<Document> chunks, RetrievalConfig config) {
        double rrfWeight = 0.3;
        double ruleWeight = 0.5;
        double kgWeight = 0.2;

        return chunks.stream()
                .mapToDouble(doc -> {
                    Double rrf = getMetadataDouble(doc, "rrf_score");
                    Double rule = getMetadataDouble(doc, "rule_score");
                    Double kg = getMetadataDouble(doc, "kg_score");
                    
                    double score = 0;
                    if (rrf != null) score += rrf * rrfWeight;
                    if (rule != null) score += rule * ruleWeight;
                    if (kg != null) score += kg * kgWeight;
                    
                    return score;
                })
                .max()
                .orElse(0.0);
    }

    /**
     * 从元数据中获取Double值
     * 
     * @param doc 文档
     * @param key 元数据键
     * @return Double值，不存在则返回0.0
     */
    private static Double getMetadataDouble(Document doc, String key) {
        Object value = doc.getMetadata().get(key);
        return value instanceof Double ? (Double) value : 0.0;
    }

    /**
     * 从元数据中获取字符串值
     * 
     * @param doc 文档
     * @param key 元数据键
     * @param defaultValue 默认值
     * @return 字符串值
     */
    private static String getMetadataString(Document doc, String key, String defaultValue) {
        Object value = doc.getMetadata().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 获取文档ID
     * 
     * @param doc 文档
     * @return 文档ID
     */
    private static String getDocumentId(Document doc) {
        Object docId = doc.getMetadata().get("document_id");
        return docId != null ? docId.toString() : "unknown";
    }
}
