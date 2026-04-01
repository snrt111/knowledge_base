package com.snrt.knowledgebase.domain.chat.service;

import com.snrt.knowledgebase.common.constants.Constants;
import com.snrt.knowledgebase.common.util.LogUtils;
import com.snrt.knowledgebase.domain.chat.dto.PromptResult;
import com.snrt.knowledgebase.domain.document.dto.DocumentSourceDTO;
import com.snrt.knowledgebase.domain.document.service.RAGCacheManager;
import com.snrt.knowledgebase.infrastructure.retrieval.AdvancedRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RAGPromptBuilder {

    private final RAGCacheManager ragCacheManager;
    private final AdvancedRetrievalService advancedRetrievalService;
    private final DocumentSourceConverter documentSourceConverter;

    public PromptResult build(String message, String knowledgeBaseId, String compressedContext) {
        String traceId = LogUtils.getTraceId();
        logRagStart(traceId, message, knowledgeBaseId, compressedContext);

        if (knowledgeBaseId == null) {
            return buildWithoutKnowledgeBase(traceId, message, compressedContext);
        }

        return buildWithKnowledgeBase(traceId, message, knowledgeBaseId, compressedContext);
    }

    private PromptResult buildWithoutKnowledgeBase(String traceId, String message, String compressedContext) {
        log.info("[{}] [RAG流程] 未指定知识库，直接返回用户消息", traceId);
        String prompt = buildSystemPrompt(null, message, compressedContext);
        log.info("[{}] [RAG流程] 构建完成，Prompt长度: {}字符，无检索文档", traceId, prompt.length());
        return new PromptResult(prompt, null);
    }

    private PromptResult buildWithKnowledgeBase(String traceId, String message, String knowledgeBaseId, String compressedContext) {
        log.info("[{}] [RAG流程] 知识库ID: {}, 开始检索相关文档", traceId, knowledgeBaseId);

        Optional<RAGCacheManager.CachedSearchResult> cachedResult = 
                ragCacheManager.getCachedSearchResult(message, knowledgeBaseId);

        if (cachedResult.isPresent()) {
            return buildFromCache(traceId, cachedResult.get(), message, compressedContext);
        }

        return buildFromRetrieval(traceId, message, knowledgeBaseId, compressedContext);
    }

    private PromptResult buildFromCache(String traceId, RAGCacheManager.CachedSearchResult cached, 
                                        String message, String compressedContext) {
        log.info("[{}] [RAG流程] 命中缓存，缓存结果: {}个文档", traceId, cached.getDocuments().size());
        List<Document> docs = cached.getDocuments();
        List<DocumentSourceDTO> sources = cached.getSources();
        String prompt = buildSystemPrompt(docs, message, compressedContext);
        log.info("[{}] [RAG流程] 构建完成，Prompt长度: {}字符，使用缓存文档: {}个", 
                traceId, prompt.length(), docs.size());
        return new PromptResult(prompt, sources);
    }

    private PromptResult buildFromRetrieval(String traceId, String message, String knowledgeBaseId, 
                                            String compressedContext) {
        log.info("[{}] [RAG流程] 未命中缓存，使用高级检索服务（智能检索+HyDE）", traceId);

        Instant retrievalStart = Instant.now();
        List<Document> retrievedDocs = advancedRetrievalService.smartRetrieve(
                message, knowledgeBaseId, Constants.Chat.MAX_RETRIEVAL_RESULTS);
        Duration retrievalDuration = Duration.between(retrievalStart, Instant.now());

        log.info("[{}] [RAG流程] 检索完成，耗时: {}ms，返回文档数: {}", 
                traceId, retrievalDuration.toMillis(), retrievedDocs.size());

        if (retrievedDocs.isEmpty()) {
            log.warn("[{}] [RAG流程] 未检索到相关文档", traceId);
            String prompt = buildSystemPrompt(null, message, compressedContext);
            log.info("[{}] [RAG流程] 构建完成，Prompt长度: {}字符，无检索文档", traceId, prompt.length());
            return new PromptResult(prompt, null);
        }

        return buildFromDocuments(traceId, message, knowledgeBaseId, compressedContext, retrievedDocs);
    }

    private PromptResult buildFromDocuments(String traceId, String message, String knowledgeBaseId,
                                            String compressedContext, List<Document> retrievedDocs) {
        log.debug("[{}] [RAG流程] 开始转换文档为DocumentSourceDTO，输入文档数: {}", traceId, retrievedDocs.size());
        List<DocumentSourceDTO> documentSources = documentSourceConverter.convert(retrievedDocs);
        log.info("[{}] [RAG流程] 转换完成，输出DocumentSourceDTO数: {}", traceId, documentSources.size());

        String prompt = buildSystemPrompt(retrievedDocs, message, compressedContext);
        log.info("[{}] [RAG流程] Prompt构建完成，长度: {}字符，包含{}个参考文档", 
                traceId, prompt.length(), retrievedDocs.size());

        ragCacheManager.cacheSearchResult(message, knowledgeBaseId, retrievedDocs, documentSources);
        log.info("[{}] [RAG流程] 缓存完成", traceId);

        logRetrievalQuality(traceId, retrievedDocs);
        logDocumentDetails(traceId, retrievedDocs);

        log.info("[{}] [RAG流程] 完成，总文档数: {}, 构建的Prompt长度: {}字符", 
                traceId, retrievedDocs.size(), prompt.length());
        return new PromptResult(prompt, documentSources);
    }

    public String buildSystemPrompt(List<Document> documents, String userMessage, String compressedContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专业的知识库助手。请基于提供的参考文档和对话历史回答用户问题。\n\n");

        appendContext(prompt, compressedContext);
        appendDocuments(prompt, documents);
        appendUserMessage(prompt, userMessage);

        return prompt.toString();
    }

    private void appendContext(StringBuilder prompt, String compressedContext) {
        if (compressedContext != null && !compressedContext.isEmpty()) {
            prompt.append("=== 对话历史 ===\n");
            prompt.append(compressedContext);
            prompt.append("\n\n");
        }
    }

    private void appendDocuments(StringBuilder prompt, List<Document> documents) {
        if (documents != null && !documents.isEmpty()) {
            prompt.append("=== 参考文档 ===\n");
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                prompt.append(String.format("[文档%d] %s\n", i + 1, doc.getText()));
            }
            prompt.append("\n");
        }
    }

    private void appendUserMessage(StringBuilder prompt, String userMessage) {
        prompt.append("=== 当前问题 ===\n");
        prompt.append(userMessage);
    }

    private void logRagStart(String traceId, String message, String knowledgeBaseId, String compressedContext) {
        log.info("[{}] [RAG流程] 开始构建Prompt，用户查询: '{}', 知识库ID: {}, 压缩上下文长度: {}字符",
                traceId, message, knowledgeBaseId, compressedContext != null ? compressedContext.length() : 0);
    }

    private void logRetrievalQuality(String traceId, List<Document> retrievedDocs) {
        double quality = advancedRetrievalService.estimateRetrievalQuality(retrievedDocs);
        String level = getQualityLevel(quality);
        log.info("[{}] [RAG流程] 检索质量评估: 分数 = {:.2f}, 等级 = {}", traceId, quality, level);
    }

    private void logDocumentDetails(String traceId, List<Document> retrievedDocs) {
        log.debug("[{}] [RAG流程] 检索文档详情:", traceId);
        int maxDocs = Math.min(retrievedDocs.size(), 5);
        for (int i = 0; i < maxDocs; i++) {
            Document doc = retrievedDocs.get(i);
            String docName = getMetadataString(doc, Constants.VectorStore.METADATA_DOCUMENT_NAME, "未知文档");
            Double rrfScore = (Double) doc.getMetadata().get("rrf_score");
            Double ruleScore = (Double) doc.getMetadata().get("rule_score");
            log.debug("[{}] [RAG流程] 文档{}: {}，RRF分数: {:.3f}，规则分数: {:.3f}", 
                    traceId, i + 1, docName, rrfScore, ruleScore);
        }
    }

    private String getMetadataString(Document doc, String key, String defaultValue) {
        Object value = doc.getMetadata().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private String getQualityLevel(double quality) {
        if (quality >= 0.8) return "优秀";
        if (quality >= 0.6) return "良好";
        if (quality >= 0.4) return "一般";
        return "较差";
    }
}
