package com.snrt.knowledgebase.service.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snrt.knowledgebase.constants.Constants;
import com.snrt.knowledgebase.dto.*;
import com.snrt.knowledgebase.entity.ChatMessage;
import com.snrt.knowledgebase.entity.ChatSession;
import com.snrt.knowledgebase.entity.KnowledgeBase;
import com.snrt.knowledgebase.exception.ResourceNotFoundException;
import com.snrt.knowledgebase.mapper.ChatMessageMapper;
import com.snrt.knowledgebase.mapper.ChatSessionMapper;
import com.snrt.knowledgebase.model.ChatModelFactory;
import com.snrt.knowledgebase.repository.ChatMessageRepository;
import com.snrt.knowledgebase.repository.ChatSessionRepository;
import com.snrt.knowledgebase.repository.KnowledgeBaseRepository;
import com.snrt.knowledgebase.service.ConversationSummarizer;
import com.snrt.knowledgebase.service.RAGCacheManager;
import com.snrt.knowledgebase.service.retrieval.AdvancedRetrievalService;
import com.snrt.knowledgebase.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStrategyHelper {

    private final ChatModelFactory chatModelFactory;
    private final VectorStore vectorStore;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RAGCacheManager ragCacheManager;
    private final ConversationSummarizer conversationSummarizer;
    private final AdvancedRetrievalService advancedRetrievalService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatResponseDTO executeChat(ChatRequest request) {
        Instant start = Instant.now();
        String traceId = LogUtils.getTraceId();

        ChatSession session = getOrCreateSession(request);
        log.info("[{}] [大模型请求] 会话ID: {}, 知识库ID: {}, 用户消息: {}",
                traceId, session.getId(), request.getKnowledgeBaseId(), request.getMessage());

        saveMessage(session.getId(), ChatMessage.MessageRole.USER, request.getMessage(), null);

        String knowledgeBaseId = session.getKnowledgeBase() != null ? session.getKnowledgeBase().getId() : null;

        // 获取并压缩历史消息上下文
        List<ChatMessage> historyMessages = messageRepository.findBySessionIdOrderByCreateTimeAsc(session.getId());
        String compressedContext = conversationSummarizer.compressContext(historyMessages);
        log.info("[{}] [上下文压缩] 会话ID: {}, 原始消息数: {}, 压缩后长度: {} 字符, 是否压缩: {}",
                traceId, session.getId(), historyMessages.size(), compressedContext.length(),
                conversationSummarizer.needCompression(historyMessages));

        PromptResult promptResult = buildPromptWithSources(request.getMessage(), knowledgeBaseId, compressedContext);
        log.debug("[{}] [大模型处理] 会话ID: {}, 构建的Prompt长度: {} 字符",
                traceId, session.getId(), promptResult.getPrompt().length());

        ChatModel chatModel = chatModelFactory.getDefaultModel();
        String response = chatModel.call(promptResult.getPrompt());

        Duration duration = Duration.between(start, Instant.now());
        LogUtils.logPerformance("chat", duration, 5000);

        log.info("[{}] [大模型回答] 会话ID: {}, 响应耗时: {}ms, 回答长度: {} 字符",
                traceId, session.getId(), duration.toMillis(), response.length());

        String documentSourcesJson = convertDocumentSourcesToJson(promptResult.getDocumentSources());
        saveMessage(session.getId(), ChatMessage.MessageRole.ASSISTANT, response, documentSourcesJson);

        return ChatResponseDTO.builder()
                .content(response)
                .sources(promptResult.getDocumentSources())
                .build();
    }

    public Flux<StreamChatResponse> executeStreamChat(ChatRequest request) {
        Instant start = Instant.now();
        String traceId = LogUtils.getTraceId();

        ChatSession session = getOrCreateSession(request);
        log.info("[{}] [大模型流式请求] 会话ID: {}, 知识库ID: {}, 用户消息: {}",
                traceId, session.getId(), request.getKnowledgeBaseId(), request.getMessage());

        saveMessage(session.getId(), ChatMessage.MessageRole.USER, request.getMessage(), null);

        String knowledgeBaseId = session.getKnowledgeBase() != null ? session.getKnowledgeBase().getId() : null;

        // 获取并压缩历史消息上下文
        List<ChatMessage> historyMessages = messageRepository.findBySessionIdOrderByCreateTimeAsc(session.getId());
        String compressedContext = conversationSummarizer.compressContext(historyMessages);
        log.info("[{}] [上下文压缩] 会话ID: {}, 原始消息数: {}, 压缩后长度: {} 字符, 是否压缩: {}",
                traceId, session.getId(), historyMessages.size(), compressedContext.length(),
                conversationSummarizer.needCompression(historyMessages));

        PromptResult promptResult = buildPromptWithSources(request.getMessage(), knowledgeBaseId, compressedContext);
        log.debug("[{}] [大模型流式处理] 会话ID: {}, 构建的Prompt长度: {} 字符",
                traceId, session.getId(), promptResult.getPrompt().length());

        ChatModel chatModel = chatModelFactory.getDefaultModel();
        StringBuilder fullResponse = new StringBuilder();

        StreamChatResponse sourcesResponse = StreamChatResponse.sources(promptResult.getDocumentSources());

        return chatModel.stream(promptResult.getPrompt())
                .map(StreamChatResponse::content)
                .doOnNext(response -> {
                    if (response.getContent() != null) {
                        fullResponse.append(response.getContent());
                        log.debug("[{}] [大模型流式响应] 会话ID: {}, 接收数据块长度: {}",
                                traceId, session.getId(), response.getContent().length());
                    }
                })
                .startWith(sourcesResponse)
                .concatWithValues(StreamChatResponse.complete())
                .doOnComplete(() -> {
                    Duration duration = Duration.between(start, Instant.now());
                    LogUtils.logPerformance("streamChat", duration, 10000);

                    log.info("[{}] [大模型流式回答完成] 会话ID: {}, 响应耗时: {}ms, 完整回答长度: {} 字符",
                            traceId, session.getId(), duration.toMillis(), fullResponse.length());
                    String documentSourcesJson = convertDocumentSourcesToJson(promptResult.getDocumentSources());
                    saveMessage(session.getId(), ChatMessage.MessageRole.ASSISTANT, fullResponse.toString(), documentSourcesJson);
                })
                .doOnError(error -> {
                    Duration duration = Duration.between(start, Instant.now());
                    log.error("[{}] [大模型流式错误] 会话ID: {}, 耗时: {}ms, 错误信息: {}",
                            traceId, session.getId(), duration.toMillis(), error.getMessage(), error);
                });
    }

    private ChatSession getOrCreateSession(ChatRequest request) {
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            return sessionRepository.findByIdAndIsDeletedFalse(request.getSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("会话", request.getSessionId()));
        }

        String title = request.getMessage().length() > 20
                ? request.getMessage().substring(0, 20) + "..."
                : request.getMessage();

        return createSessionEntity(title, request.getKnowledgeBaseId());
    }

    private ChatSession createSessionEntity(String title, String knowledgeBaseId) {
        ChatSession session = new ChatSession();
        session.setTitle(title);

        if (knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
            KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(knowledgeBaseId)
                    .orElse(null);
            session.setKnowledgeBase(kb);
        }

        return sessionRepository.save(session);
    }

    private PromptResult buildPromptWithSources(String message, String knowledgeBaseId, String compressedContext) {
        if (knowledgeBaseId == null) {
            log.debug("[Prompt构建] 未指定知识库，直接返回用户消息");
            // 即使没有知识库，也使用压缩后的上下文
            String prompt = buildSystemPrompt(null, message, compressedContext);
            return new PromptResult(prompt, null);
        }

        log.info("[Prompt构建] 知识库ID: {}, 开始检索相关文档", knowledgeBaseId);

        // 检查缓存
        Optional<RAGCacheManager.CachedSearchResult> cachedResult =
                ragCacheManager.getCachedSearchResult(message, knowledgeBaseId);

        if (cachedResult.isPresent()) {
            log.info("[Prompt构建] 命中缓存，直接返回缓存结果");
            List<Document> cachedDocs = cachedResult.get().getDocuments();
            List<DocumentSourceDTO> cachedSources = cachedResult.get().getSources();
            String prompt = buildSystemPrompt(cachedDocs, message, compressedContext);
            return new PromptResult(prompt, cachedSources);
        }

        // 使用高级检索服务（智能检索，自动选择是否使用HyDE）
        log.info("[Prompt构建] 使用高级检索服务（智能检索+HyDE）");
        List<Document> retrievedDocs = advancedRetrievalService.smartRetrieve(
                message, knowledgeBaseId, Constants.Chat.MAX_RETRIEVAL_RESULTS);

        if (retrievedDocs.isEmpty()) {
            log.warn("[Prompt构建] 未检索到相关文档");
            String prompt = buildSystemPrompt(null, message, compressedContext);
            return new PromptResult(prompt, null);
        }

        // 转换为 DocumentSourceDTO
        List<DocumentSourceDTO> documentSources = convertToDocumentSources(retrievedDocs);

        // 构建 Prompt
        String prompt = buildSystemPrompt(retrievedDocs, message, compressedContext);

        // 缓存结果
        ragCacheManager.cacheSearchResult(message, knowledgeBaseId, retrievedDocs, documentSources);

        // 记录检索质量
        double quality = advancedRetrievalService.estimateRetrievalQuality(retrievedDocs);
        log.info("[Prompt构建] 检索完成，文档数: {}, 质量分数: {:.2f}",
                retrievedDocs.size(), quality);

        return new PromptResult(prompt, documentSources);
    }

    /**
     * 将 Document 转换为 DocumentSourceDTO
     */
    private List<DocumentSourceDTO> convertToDocumentSources(List<Document> documents) {
        // 按文档ID分组
        Map<String, List<Document>> docsById = documents.stream()
                .collect(Collectors.groupingBy(this::getDocumentId));

        // 最小分数阈值 (50%)
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

    /**
     * 创建文档来源DTO
     */
    private DocumentSourceDTO createDocumentSource(String docId, List<Document> chunks) {
        Document firstChunk = chunks.get(0);

        String docName = getMetadataString(firstChunk, Constants.VectorStore.METADATA_DOCUMENT_NAME, "未知文档");
        String kbName = getMetadataString(firstChunk, Constants.VectorStore.METADATA_KNOWLEDGE_BASE_NAME, "未知知识库");

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

    private List<Document> retrieveRelevantDocuments(String message) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(message)
                .topK(20)
                .similarityThreshold(0.5)
                .build();

        List<Document> docs = vectorStore.similaritySearch(searchRequest);
        log.info("[Prompt构建] 向量检索返回 {} 个文档", docs.size());
        return docs;
    }

    private List<Document> filterDocumentsByRelevance(List<Document> docs, String knowledgeBaseId) {
        List<Document> filtered = docs.stream()
                .filter(doc -> isDocumentRelevant(doc, knowledgeBaseId, Constants.Chat.SIMILARITY_THRESHOLD))
                .collect(Collectors.toList());

        List<Document> deduplicated = deduplicateDocuments(filtered);

        List<Document> limited = deduplicated.stream()
                .limit(Constants.Chat.MAX_RETRIEVAL_RESULTS)
                .collect(Collectors.toList());

        log.info("[Prompt构建] 过滤后: 原始{}个 → 过滤后{}个 → 去重后{}个 → 最终{}个",
                docs.size(), filtered.size(), deduplicated.size(), limited.size());
        return limited;
    }

    private List<Document> deduplicateDocuments(List<Document> docs) {
        Map<String, Document> bestChunkByDoc = new HashMap<>();

        for (Document doc : docs) {
            String docId = getDocumentId(doc);
            double similarity = calculateSimilarity(doc);

            Document existing = bestChunkByDoc.get(docId);
            if (existing == null || similarity > calculateSimilarity(existing)) {
                bestChunkByDoc.put(docId, doc);
            }
        }

        return bestChunkByDoc.values().stream()
                .sorted(Comparator.comparingDouble(this::calculateSimilarity).reversed())
                .collect(Collectors.toList());
    }

    private boolean isDocumentRelevant(Document doc, String knowledgeBaseId, double similarityThreshold) {
        Object kbId = doc.getMetadata().get(Constants.VectorStore.METADATA_KNOWLEDGE_BASE_ID);
        boolean kbMatch = kbId != null && kbId.equals(knowledgeBaseId);

        double similarity = calculateSimilarity(doc);
        boolean similarityMatch = similarity >= similarityThreshold;

        Object docName = doc.getMetadata().get(Constants.VectorStore.METADATA_DOCUMENT_NAME);
        Object distance = doc.getMetadata().get("distance");
        log.info("[Prompt构建] 文档: {}, 距离: {}, 相似度: {:.2f}, 知识库匹配: {}, 相似度匹配: {}",
                docName, distance, similarity, kbMatch, similarityMatch);

        return kbMatch && similarityMatch;
    }

    private double calculateSimilarity(Document doc) {
        Object distanceObj = doc.getMetadata().get("distance");
        if (distanceObj instanceof Number) {
            return 1.0 - ((Number) distanceObj).doubleValue();
        }
        return 0.0;
    }

    private List<String> extractSnippets(List<Document> chunks) {
        return chunks.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isEmpty())
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * 获取文档ID
     */
    private String getDocumentId(Document doc) {
        Object docId = doc.getMetadata().get(Constants.VectorStore.METADATA_DOCUMENT_ID);
        return docId != null ? docId.toString() : "unknown";
    }

    /**
     * 获取元数据字符串
     */
    private String getMetadataString(Document doc, String key, String defaultValue) {
        Object value = doc.getMetadata().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private String buildSystemPrompt(List<Document> documents, String userMessage, String compressedContext) {
        StringBuilder prompt = new StringBuilder();

        // 添加系统角色定义
        prompt.append("你是一个专业的知识库助手。请基于提供的参考文档和对话历史回答用户问题。\n\n");

        // 添加压缩后的对话历史（如果有）
        if (compressedContext != null && !compressedContext.isEmpty()) {
            prompt.append("=== 对话历史 ===\n");
            prompt.append(compressedContext);
            prompt.append("\n\n");
        }

        // 添加检索到的文档内容（如果有）
        if (documents != null && !documents.isEmpty()) {
            prompt.append("=== 参考文档 ===\n");
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                prompt.append(String.format("[文档%d] %s\n", i + 1, doc.getText()));
            }
            prompt.append("\n");
        }

        // 添加用户当前问题
        prompt.append("=== 当前问题 ===\n");
        prompt.append(userMessage);

        return prompt.toString();
    }

    private void saveMessage(String sessionId, ChatMessage.MessageRole role, String content, String sources) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("会话", sessionId));

        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setDocumentSources(sources);
        messageRepository.save(message);
    }

    private String convertDocumentSourcesToJson(List<DocumentSourceDTO> sources) {
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

    private static class PromptResult {
        private final String prompt;
        private final List<DocumentSourceDTO> documentSources;

        PromptResult(String prompt, List<DocumentSourceDTO> documentSources) {
            this.prompt = prompt;
            this.documentSources = documentSources;
        }

        public String getPrompt() {
            return prompt;
        }

        public List<DocumentSourceDTO> getDocumentSources() {
            return documentSources;
        }
    }
}
