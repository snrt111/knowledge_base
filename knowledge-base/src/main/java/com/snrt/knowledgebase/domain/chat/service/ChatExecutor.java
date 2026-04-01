package com.snrt.knowledgebase.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snrt.knowledgebase.domain.chat.dto.ChatRequest;
import com.snrt.knowledgebase.domain.chat.dto.ChatResponseDTO;
import com.snrt.knowledgebase.domain.chat.dto.PromptResult;
import com.snrt.knowledgebase.domain.chat.entity.ChatMessage;
import com.snrt.knowledgebase.domain.chat.entity.ChatSession;
import com.snrt.knowledgebase.domain.chat.repository.ChatModelFactory;
import com.snrt.knowledgebase.domain.chat.repository.ChatMessageRepository;
import com.snrt.knowledgebase.domain.chat.repository.ChatSessionRepository;
import com.snrt.knowledgebase.domain.document.dto.DocumentSourceDTO;
import com.snrt.knowledgebase.domain.document.service.ConversationSummarizer;
import com.snrt.knowledgebase.domain.document.service.RAGCacheManager;
import com.snrt.knowledgebase.infrastructure.retrieval.AdvancedRetrievalService;
import com.snrt.knowledgebase.common.constants.Constants;
import com.snrt.knowledgebase.common.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatExecutor {

    private final ChatModelFactory chatModelFactory;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final RAGCacheManager ragCacheManager;
    private final ConversationSummarizer conversationSummarizer;
    private final AdvancedRetrievalService advancedRetrievalService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatResponseDTO executeSyncChat(ChatRequest request) {
        Instant start = Instant.now();
        String traceId = LogUtils.getTraceId();

        ChatSession session = sessionRepository.findByIdAndIsDeletedFalse(request.getSessionId())
                .orElseGet(() -> createNewSession(request));

        log.info("[{}] [大模型请求] 会话ID: {}, 知识库ID: {}, 用户消息: {}",
                traceId, session.getId(), request.getKnowledgeBaseId(), request.getMessage());

        saveUserMessage(session.getId(), request.getMessage());

        String knowledgeBaseId = getKnowledgeBaseId(session);

        List<ChatMessage> historyMessages = messageRepository.findBySessionIdOrderByCreateTimeAsc(session.getId());
        String compressedContext = compressContext(historyMessages);

        PromptResult promptResult = buildPromptWithSources(request.getMessage(), knowledgeBaseId, compressedContext);
        log.debug("[{}] [大模型处理] 会话ID: {}, 构建的Prompt长度: {} 字符",
                traceId, session.getId(), promptResult.getPrompt().length());

        ChatModel chatModel = chatModelFactory.getDefaultModel();
        String response = chatModel.call(promptResult.getPrompt());

        Duration duration = Duration.between(start, Instant.now());
        LogUtils.logPerformance("chat", duration, 5000);

        log.info("[{}] [大模型回答] 会话ID: {}, 响应耗时: {}ms, 回答长度: {} 字符",
                traceId, session.getId(), duration.toMillis(), response.length());

        saveAssistantMessage(session.getId(), response, promptResult.getDocumentSources());

        return ChatResponseDTO.builder()
                .content(response)
                .sources(promptResult.getDocumentSources())
                .build();
    }

    private ChatSession createNewSession(ChatRequest request) {
        String title = request.getMessage() != null && request.getMessage().length() > 20
                ? request.getMessage().substring(0, 20) + "..."
                : request.getMessage();

        ChatSession session = new ChatSession();
        session.setTitle(title);

        if (request.getKnowledgeBaseId() != null && !request.getKnowledgeBaseId().isEmpty()) {
            var kb = sessionRepository.findByIdAndIsDeletedFalse(request.getKnowledgeBaseId())
                    .map(chatSession -> chatSession.getKnowledgeBase()).orElse(null);
            session.setKnowledgeBase(kb);
        }

        return sessionRepository.save(session);
    }

    private void saveUserMessage(String sessionId, String message) {
        saveMessage(sessionId, ChatMessage.MessageRole.USER, message, null);
    }

    private void saveAssistantMessage(String sessionId, String response, List<DocumentSourceDTO> sources) {
        String sourcesJson = convertToJson(sources);
        saveMessage(sessionId, ChatMessage.MessageRole.ASSISTANT, response, sourcesJson);
    }

    private void saveMessage(String sessionId, ChatMessage.MessageRole role, String content, String sources) {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));

        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setDocumentSources(sources);
        messageRepository.save(message);
    }

    private String getKnowledgeBaseId(ChatSession session) {
        return session.getKnowledgeBase() != null ? session.getKnowledgeBase().getId() : null;
    }

    private String compressContext(List<ChatMessage> historyMessages) {
        return conversationSummarizer.compressContext(historyMessages);
    }

    private String convertToJson(List<DocumentSourceDTO> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            log.warn("转换文档来源为JSON失败", e);
            return null;
        }
    }

    private PromptResult buildPromptWithSources(String message, String knowledgeBaseId, String compressedContext) {
        String traceId = LogUtils.getTraceId();
        log.info("[{}] [RAG流程] 开始构建Prompt，用户查询: '{}', 知识库ID: {}, 压缩上下文长度: {}字符",
                traceId, message, knowledgeBaseId, compressedContext != null ? compressedContext.length() : 0);

        if (knowledgeBaseId == null) {
            log.info("[{}] [RAG流程] 未指定知识库，直接返回用户消息", traceId);
            String prompt = buildSystemPrompt(null, message, compressedContext);
            log.info("[{}] [RAG流程] 构建完成，Prompt长度: {}字符，无检索文档", traceId, prompt.length());
            return new PromptResult(prompt, null);
        }

        log.info("[{}] [RAG流程] 知识库ID: {}, 开始检索相关文档", traceId, knowledgeBaseId);

        Optional<RAGCacheManager.CachedSearchResult> cachedResult = 
                ragCacheManager.getCachedSearchResult(message, knowledgeBaseId);

        if (cachedResult.isPresent()) {
            log.info("[{}] [RAG流程] 命中缓存，缓存结果: {}个文档", traceId, cachedResult.get().getDocuments().size());
            List<Document> cachedDocs = cachedResult.get().getDocuments();
            List<DocumentSourceDTO> cachedSources = cachedResult.get().getSources();
            String prompt = buildSystemPrompt(cachedDocs, message, compressedContext);
            log.info("[{}] [RAG流程] 构建完成，Prompt长度: {}字符，使用缓存文档: {}个", traceId, prompt.length(), cachedDocs.size());
            return new PromptResult(prompt, cachedSources);
        }

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

        log.debug("[{}] [RAG流程] 开始转换文档为DocumentSourceDTO，输入文档数: {}", traceId, retrievedDocs.size());
        List<DocumentSourceDTO> documentSources = convertToDocumentSources(retrievedDocs);
        log.info("[{}] [RAG流程] 转换完成，输出DocumentSourceDTO数: {}", traceId, documentSources.size());

        String prompt = buildSystemPrompt(retrievedDocs, message, compressedContext);
        log.info("[{}] [RAG流程] Prompt构建完成，长度: {}字符，包含{}个参考文档", 
                traceId, prompt.length(), retrievedDocs.size());

        ragCacheManager.cacheSearchResult(message, knowledgeBaseId, retrievedDocs, documentSources);
        log.info("[{}] [RAG流程] 缓存完成", traceId);

        double quality = advancedRetrievalService.estimateRetrievalQuality(retrievedDocs);
        log.info("[{}] [RAG流程] 检索质量评估: 分数 = {:.2f}, 等级 = {}", 
                traceId, quality, getQualityLevel(quality));

        log.debug("[{}] [RAG流程] 检索文档详情:", traceId);
        for (int i = 0; i < Math.min(retrievedDocs.size(), 5); i++) {
            Document doc = retrievedDocs.get(i);
            String docName = getMetadataString(doc, Constants.VectorStore.METADATA_DOCUMENT_NAME, "未知文档");
            Double rrfScore = (Double) doc.getMetadata().get("rrf_score");
            Double ruleScore = (Double) doc.getMetadata().get("rule_score");
            log.debug("[{}] [RAG流程] 文档{}: {}，RRF分数: {:.3f}，规则分数: {:.3f}", 
                    traceId, i+1, docName, rrfScore, ruleScore);
        }

        log.info("[{}] [RAG流程] 完成，总文档数: {}, 构建的Prompt长度: {}字符", 
                traceId, retrievedDocs.size(), prompt.length());
        return new PromptResult(prompt, documentSources);
    }

    private String getQualityLevel(double quality) {
        if (quality >= 0.8) return "优秀"; 
        if (quality >= 0.6) return "良好"; 
        if (quality >= 0.4) return "一般"; 
        return "较差";
    }

    private List<DocumentSourceDTO> convertToDocumentSources(List<Document> documents) {
        Map<String, List<Document>> docsById = documents.stream()
                .collect(Collectors.groupingBy(this::getDocumentId));

        double minScoreThreshold = 0.5;

        return docsById.entrySet().stream()
                .map(entry -> createDocumentSource(entry.getKey(), entry.getValue()))
                .filter(source -> source.getScore() != null && source.getScore() >= minScoreThreshold)
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

    private DocumentSourceDTO createDocumentSource(String docId, List<Document> chunks) {
        Document firstChunk = chunks.get(0);

        String docName = getMetadataString(firstChunk, Constants.VectorStore.METADATA_DOCUMENT_NAME, "未知文档");
        String kbName = getMetadataString(firstChunk, Constants.VectorStore.METADATA_KNOWLEDGE_BASE_NAME, "未知知识库");

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

    private String getDocumentId(Document doc) {
        Object docId = doc.getMetadata().get(Constants.VectorStore.METADATA_DOCUMENT_ID);
        return docId != null ? docId.toString() : "unknown";
    }

    private String getMetadataString(Document doc, String key, String defaultValue) {
        Object value = doc.getMetadata().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private String buildSystemPrompt(List<Document> documents, String userMessage, String compressedContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专业的知识库助手。请基于提供的参考文档和对话历史回答用户问题。\n\n");

        if (compressedContext != null && !compressedContext.isEmpty()) {
            prompt.append("=== 对话历史 ===\n");
            prompt.append(compressedContext);
            prompt.append("\n\n");
        }

        if (documents != null && !documents.isEmpty()) {
            prompt.append("=== 参考文档 ===\n");
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                prompt.append(String.format("[文档%d] %s\n", i + 1, doc.getText()));
            }
            prompt.append("\n");
        }

        prompt.append("=== 当前问题 ===\n");
        prompt.append(userMessage);

        return prompt.toString();
    }
}
