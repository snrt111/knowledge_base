package com.snrt.knowledgebase.service;

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
import com.snrt.knowledgebase.util.LogUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatModelFactory chatModelFactory;
    private final VectorStore vectorStore;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Prompt构建结果
     * 包含构建好的prompt字符串和引用的文档来源信息
     */
    @Getter
    private static class PromptResult {
        private final String prompt;
        private final List<DocumentSourceDTO> documentSources;

        PromptResult(String prompt, List<DocumentSourceDTO> documentSources) {
            this.prompt = prompt;
            this.documentSources = documentSources;
        }
    }

    @Transactional(readOnly = true)
    public PageResult<ChatSessionDTO> listSessions(Integer page, Integer size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("updateTime").descending());
        Page<ChatSession> sessionPage;

        if (keyword != null && !keyword.isEmpty()) {
            sessionPage = sessionRepository.searchByTitle(keyword, pageable);
        } else {
            sessionPage = sessionRepository.findByIsDeletedFalseOrderByUpdateTimeDesc(pageable);
        }

        List<ChatSessionDTO> dtoList = chatSessionMapper.toDTOList(sessionPage.getContent());
        dtoList.forEach(this::enrichMessageCount);

        return PageResult.of(dtoList, sessionPage.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public ChatSessionDTO getSession(String id) {
        ChatSession session = sessionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("会话", id));
        ChatSessionDTO dto = chatSessionMapper.toDTO(session);
        enrichMessageCount(dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getSessionMessages(String sessionId) {
        return chatMessageMapper.toDTOList(
                messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId)
        );
    }

    @Transactional
    public ChatSessionDTO createSession(String title, String knowledgeBaseId) {
        ChatSession session = new ChatSession();
        session.setTitle(title != null ? title : "新对话");

        if (knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
            KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(knowledgeBaseId)
                    .orElseThrow(() -> new ResourceNotFoundException("知识库", knowledgeBaseId));
            session.setKnowledgeBase(kb);
        }

        ChatSession saved = sessionRepository.save(session);
        log.info("会话创建成功: id={}, title={}", saved.getId(), saved.getTitle());
        return chatSessionMapper.toDTO(saved);
    }

    @Transactional
    public void deleteSession(String id) {
        ChatSession session = sessionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("会话", id));
        session.setIsDeleted(true);
        sessionRepository.save(session);
        log.info("会话删除成功: id={}", id);
    }

    @Transactional
    public ChatResponseDTO chat(ChatRequest request) {
        Instant start = Instant.now();
        String traceId = LogUtils.getTraceId();

        ChatSession session = getOrCreateSession(request);
        log.info("[{}] [大模型请求] 会话ID: {}, 知识库ID: {}, 用户消息: {}",
                traceId, session.getId(), request.getKnowledgeBaseId(), request.getMessage());

        saveMessage(session.getId(), ChatMessage.MessageRole.USER, request.getMessage(), null);

        String knowledgeBaseId = session.getKnowledgeBase() != null ? session.getKnowledgeBase().getId() : null;
        PromptResult promptResult = buildPromptWithSources(request.getMessage(), knowledgeBaseId);
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

    @Transactional
    public Flux<StreamChatResponse> streamChat(ChatRequest request) {
        Instant start = Instant.now();
        String traceId = LogUtils.getTraceId();

        ChatSession session = getOrCreateSession(request);
        log.info("[{}] [大模型流式请求] 会话ID: {}, 知识库ID: {}, 用户消息: {}",
                traceId, session.getId(), request.getKnowledgeBaseId(), request.getMessage());

        saveMessage(session.getId(), ChatMessage.MessageRole.USER, request.getMessage(), null);

        String knowledgeBaseId = session.getKnowledgeBase() != null ? session.getKnowledgeBase().getId() : null;
        PromptResult promptResult = buildPromptWithSources(request.getMessage(), knowledgeBaseId);
        log.debug("[{}] [大模型流式处理] 会话ID: {}, 构建的Prompt长度: {} 字符",
                traceId, session.getId(), promptResult.getPrompt().length());

        ChatModel chatModel = chatModelFactory.getDefaultModel();
        StringBuilder fullResponse = new StringBuilder();

        // 先发送文档来源信息
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

    /**
     * 构建Prompt并提取文档来源信息
     *
     * @param message 用户消息
     * @param knowledgeBaseId 知识库ID
     * @return PromptResult 包含prompt字符串和文档来源信息
     */
    private PromptResult buildPromptWithSources(String message, String knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            log.debug("[Prompt构建] 未指定知识库，直接返回用户消息");
            return new PromptResult(message, null);
        }

        log.info("[Prompt构建] 知识库ID: {}, 开始检索相关文档", knowledgeBaseId);

        // 1. 向量检索：从向量数据库中检索相关文档
        List<Document> relevantDocs = retrieveRelevantDocuments(message);
        
        // 2. 文档过滤：根据知识库ID和相似度阈值过滤文档
        List<Document> filteredDocs = filterDocumentsByRelevance(relevantDocs, knowledgeBaseId);
        
        // 3. 构建文档来源：将文档分组并构建DTO
        List<DocumentSourceDTO> documentSources = buildDocumentSources(filteredDocs);
        
        // 4. 构建Prompt：将文档内容组合成Prompt
        String prompt = buildSystemPrompt(filteredDocs, message);

        return new PromptResult(prompt, documentSources);
    }

    /**
     * 从向量数据库检索相关文档
     */
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

    /**
     * 根据知识库ID和相似度过滤文档
     */
    private List<Document> filterDocumentsByRelevance(List<Document> docs, String knowledgeBaseId) {
        final double SIMILARITY_THRESHOLD = 0.5;
        
        List<Document> filtered = docs.stream()
                .filter(doc -> isDocumentRelevant(doc, knowledgeBaseId, SIMILARITY_THRESHOLD))
                .limit(Constants.Chat.MAX_CONTEXT_LENGTH)
                .collect(Collectors.toList());
        
        log.info("[Prompt构建] 过滤后剩余 {} 个文档", filtered.size());
        return filtered;
    }

    /**
     * 判断文档是否符合相关性要求
     */
    private boolean isDocumentRelevant(Document doc, String knowledgeBaseId, double similarityThreshold) {
        // 检查知识库匹配
        Object kbId = doc.getMetadata().get(Constants.VectorStore.METADATA_KNOWLEDGE_BASE_ID);
        boolean kbMatch = kbId != null && kbId.equals(knowledgeBaseId);
        
        // 计算并检查相似度
        double similarity = calculateSimilarity(doc);
        boolean similarityMatch = similarity >= similarityThreshold;
        
        // 记录日志
        Object docName = doc.getMetadata().get(Constants.VectorStore.METADATA_DOCUMENT_NAME);
        Object distance = doc.getMetadata().get("distance");
        log.info("[Prompt构建] 文档: {}, 距离: {}, 相似度: {:.2f}, 知识库匹配: {}, 相似度匹配: {}",
                docName, distance, similarity, kbMatch, similarityMatch);
        
        return kbMatch && similarityMatch;
    }

    /**
     * 计算文档的相似度分数
     * 公式：余弦相似度 = 1 - 余弦距离
     */
    private double calculateSimilarity(Document doc) {
        Object distanceObj = doc.getMetadata().get("distance");
        if (distanceObj instanceof Number) {
            return 1.0 - ((Number) distanceObj).doubleValue();
        }
        return 0.0;
    }

    /**
     * 构建文档来源DTO列表
     */
    private List<DocumentSourceDTO> buildDocumentSources(List<Document> docs) {
        // 按文档ID分组，并记录每个文档的最高相似度分数,并按相似度从高到低排序Value
        Map<String, List<Document>> docsById = groupAndSortBySimilarity(docs);

        log.info("[Prompt构建] 来自 {} 个不同文件", docsById.size());

        // 构建DTO并按相似度分数排序（分数高的在前）
        List<DocumentSourceDTO> sources = docsById.entrySet().stream()
                .map(entry -> createDocumentSource(entry.getKey(), entry.getValue()))
                .sorted((s1, s2) -> {
                    Double score1 = s1.getScore();
                    Double score2 = s2.getScore();
                    // 将null视为最低分数，按降序排列
                    if (score1 == null && score2 == null) return 0;
                    if (score1 == null) return 1;
                    if (score2 == null) return -1;
                    return Double.compare(score2, score1);
                })
                .collect(Collectors.toList());

        // 记录每个来源的相似度分数
        sources.forEach(source -> {
            log.debug("[Prompt构建] 文档来源: {}, 相似度: {:.2f}",
                    source.getDocumentName(), source.getScore());
        });

        log.info("[Prompt构建] 提取到 {} 个文档来源", sources.size());
        return sources;
    }

    /**
     * 按文档ID分组，并按相似度从高到低排序组内列表
     */
    private Map<String, List<Document>> groupAndSortBySimilarity(List<Document> docs) {
        return docs.stream()
                .collect(Collectors.groupingBy(
                        this::getDocumentId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    list.sort(Comparator.comparingDouble(this::calculateSimilarity).reversed());
                                    return list;
                                }
                        )
                ));
    }

    /**
     * 获取文档ID
     */
    private String getDocumentId(Document doc) {
        Object docId = doc.getMetadata().get(Constants.VectorStore.METADATA_DOCUMENT_ID);
        return docId != null ? docId.toString() : "unknown";
    }

    /**
     * 创建单个文档来源DTO
     */
    private DocumentSourceDTO createDocumentSource(String docId, List<Document> chunks) {
        Document firstChunk = chunks.get(0);
        
        String docName = getMetadataString(firstChunk, Constants.VectorStore.METADATA_DOCUMENT_NAME, "未知文档");
        String kbName = getMetadataString(firstChunk, Constants.VectorStore.METADATA_KNOWLEDGE_BASE_NAME, "未知知识库");
        Double similarity = calculateSimilarity(firstChunk);
        List<String> snippets = extractSnippets(chunks);

        log.debug("[Prompt构建] 文档来源: documentId={}, documentName={}, 相似度={:.2f}, 分块数={}",
                docId, docName, similarity, chunks.size());

        return DocumentSourceDTO.builder()
                .documentId("unknown".equals(docId) ? null : docId)
                .documentName(docName)
                .knowledgeBaseName(kbName)
                .score(similarity)
                .snippet(snippets.isEmpty() ? "" : snippets.get(0))
                .snippets(snippets)
                .build();
    }

    /**
     * 从metadata中获取字符串值
     */
    private String getMetadataString(Document doc, String key, String defaultValue) {
        Object value = doc.getMetadata().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 提取文档片段
     */
    private List<String> extractSnippets(List<Document> chunks) {
        return chunks.stream()
                .map(chunk -> {
                    String text = chunk.getText();
                    return text.length() > 200 ? text.substring(0, 200) + "..." : text;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建系统Prompt
     */
    private String buildSystemPrompt(List<Document> docs, String message) {
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = """
            你是一个专业的AI助手。请基于以下知识库内容回答用户问题。
            如果知识库内容不足以回答问题，请明确告知用户。

            知识库内容：
            {context}

            用户问题：{message}
            """;

        SystemPromptTemplate template = new SystemPromptTemplate(systemPrompt);
        Prompt prompt = template.create(Map.of("context", context, "message", message));
        
        log.debug("[Prompt构建] 最终Prompt长度: {} 字符", prompt.getContents().length());
        return prompt.getContents();
    }

    /**
     * 将文档来源列表转换为JSON字符串
     */
    private String convertDocumentSourcesToJson(List<DocumentSourceDTO> documentSources) {
        if (documentSources == null || documentSources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(documentSources);
        } catch (Exception e) {
            log.warn("转换文档来源为JSON失败: {}", e.getMessage());
            return null;
        }
    }

    private void saveMessage(String sessionId, ChatMessage.MessageRole role, String content, String documentSources) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("会话", sessionId));

        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setDocumentSources(documentSources);
        messageRepository.save(message);
    }

    private void enrichMessageCount(ChatSessionDTO dto) {
        if (dto != null && dto.getId() != null) {
            dto.setMessageCount((int) messageRepository.countBySessionId(dto.getId()));
        }
    }
}
