package com.snrt.knowledgebase.service.retrieval;

import com.snrt.knowledgebase.dto.DocumentSourceDTO;
import com.snrt.knowledgebase.service.retrieval.QueryRewriterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 高级检索服务
 *
 * 整合多路召回和重排序，提供高质量的检索结果
 *
 * 检索流程：
 * 1. 多路召回（向量 + 关键词）
 * 2. 粗排（RRF融合）
 * 3. 精排（Cross-Encoder重排序）
 * 4. 结果格式化
 *
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedRetrievalService {

    private final MultiRetriever multiRetriever;
    private final CrossEncoderReranker reranker;
    private final HydeService hydeService;
    private final QueryRewriterService queryRewriterService;

    /**
     * 执行高级检索（查询改写 + 多路召回 + 重排序）
     *
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID
     * @param topK 最终返回结果数量
     * @return 检索结果文档列表
     */
    public List<Document> retrieve(String query, String knowledgeBaseId, int topK) {
        log.info("[高级检索] 开始，查询: {}, 知识库: {}, topK: {}", query, knowledgeBaseId, topK);

        // 1. 查询改写
        String retrievalQuery = query;
        if (queryRewriterService.shouldRewrite(query)) {
            log.info("[高级检索] 执行查询改写");
            QueryRewriterService.RewrittenQuery rewrittenQuery = queryRewriterService.rewriteQuery(query);
            retrievalQuery = rewrittenQuery.getEnhancedQuery();
            log.info("[高级检索] 使用增强查询进行检索");
        }

        // 2. 多路召回（获取更多候选）
        int recallCount = topK * 3; // 召回3倍数量的候选
        List<Document> recalledDocs = multiRetriever.retrieve(retrievalQuery, knowledgeBaseId, recallCount);

        if (recalledDocs.isEmpty()) {
            log.warn("[高级检索] 未召回任何文档");
            return List.of();
        }

        log.info("[高级检索] 多路召回: {} 个文档", recalledDocs.size());

        // 3. 重排序（精排）
        List<Document> rerankedDocs = reranker.rerankByRules(retrievalQuery, recalledDocs, topK);

        log.info("[高级检索] 重排序完成，返回前 {} 个结果", rerankedDocs.size());

        // 4. 记录分数信息
        rerankedDocs.forEach(doc -> {
            Double rrfScore = (Double) doc.getMetadata().get("rrf_score");
            Double ruleScore = (Double) doc.getMetadata().get("rule_score");
            log.debug("[高级检索] 文档: {}, RRF分数: {}, 规则分数: {}",
                    getDocumentName(doc), rrfScore, ruleScore);
        });

        return rerankedDocs;
    }

    /**
     * 执行检索并转换为 DocumentSourceDTO
     */
    public List<DocumentSourceDTO> retrieveAndConvert(String query, String knowledgeBaseId, int topK) {
        List<Document> documents = retrieve(query, knowledgeBaseId, topK);
        return convertToDocumentSources(documents);
    }

    /**
     * 将 Document 转换为 DocumentSourceDTO
     */
    private List<DocumentSourceDTO> convertToDocumentSources(List<Document> documents) {
        // 按文档ID分组
        java.util.Map<String, List<Document>> docsById = documents.stream()
                .collect(Collectors.groupingBy(this::getDocumentId));

        return docsById.entrySet().stream()
                .map(entry -> createDocumentSource(entry.getKey(), entry.getValue()))
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
     * 创建文档来源DTO
     */
    private DocumentSourceDTO createDocumentSource(String docId, List<Document> chunks) {
        Document firstChunk = chunks.get(0);

        String docName = getMetadataString(firstChunk, "document_name", "未知文档");
        String kbName = getMetadataString(firstChunk, "knowledge_base_name", "未知知识库");

        // 计算综合分数
        double score = chunks.stream()
                .mapToDouble(doc -> {
                    Double rrf = (Double) doc.getMetadata().get("rrf_score");
                    Double rule = (Double) doc.getMetadata().get("rule_score");
                    double s = 0;
                    if (rrf != null) s += rrf * 0.3;
                    if (rule != null) s += rule * 0.7;
                    return s;
                })
                .max()
                .orElse(0.0);

        // 提取片段
        List<String> snippets = chunks.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isEmpty())
                .limit(3)
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
     * 获取文档ID
     */
    private String getDocumentId(Document doc) {
        Object docId = doc.getMetadata().get("document_id");
        return docId != null ? docId.toString() : "unknown";
    }

    /**
     * 获取文档名称
     */
    private String getDocumentName(Document doc) {
        Object docName = doc.getMetadata().get("document_name");
        return docName != null ? docName.toString() : "unknown";
    }

    /**
     * 获取元数据字符串
     */
    private String getMetadataString(Document doc, String key, String defaultValue) {
        Object value = doc.getMetadata().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 估算检索质量分数
     * 用于判断是否需要扩展查询或提示用户
     */
    public double estimateRetrievalQuality(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0.0;
        }

        // 基于 top 结果的分数计算质量
        double topScore = documents.stream()
                .mapToDouble(doc -> {
                    Double ruleScore = (Double) doc.getMetadata().get("rule_score");
                    return ruleScore != null ? ruleScore : 0.0;
                })
                .max()
                .orElse(0.0);

        // 归一化到 0-1
        return Math.min(topScore / 20.0, 1.0);
    }

    /**
     * 执行智能检索（查询改写 + 根据查询自动选择是否使用HyDE）
     *
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID
     * @param topK 最终返回结果数量
     * @return 检索结果文档列表
     */
    public List<Document> smartRetrieve(String query, String knowledgeBaseId, int topK) {
        log.info("[智能检索] 开始，查询: {}, 知识库: {}, topK: {}", query, knowledgeBaseId, topK);

        // 1. 执行查询改写
        String processedQuery = query;
        if (queryRewriterService.shouldRewrite(query)) {
            log.info("[智能检索] 执行查询改写");
            QueryRewriterService.RewrittenQuery rewrittenQuery = queryRewriterService.rewriteQuery(query);
            processedQuery = rewrittenQuery.getEnhancedQuery();
        }

        // 2. 判断是否使用HyDE
        boolean useHyde = hydeService.shouldUseHyde(processedQuery);
        log.info("[智能检索] 是否使用HyDE: {}", useHyde);

        if (useHyde) {
            // 使用HyDE检索
            return hydeService.retrieveWithHyde(processedQuery, knowledgeBaseId, topK);
        } else {
            // 使用普通检索
            return retrieve(processedQuery, knowledgeBaseId, topK);
        }
    }

    /**
     * 执行智能检索并转换为 DocumentSourceDTO
     */
    public List<DocumentSourceDTO> smartRetrieveAndConvert(String query, String knowledgeBaseId, int topK) {
        List<Document> documents = smartRetrieve(query, knowledgeBaseId, topK);
        return convertToDocumentSources(documents);
    }
}
