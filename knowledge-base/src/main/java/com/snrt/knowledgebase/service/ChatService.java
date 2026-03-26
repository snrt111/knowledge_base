package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.dto.*;
import com.snrt.knowledgebase.entity.ChatMessage;
import com.snrt.knowledgebase.entity.ChatSession;
import com.snrt.knowledgebase.entity.KnowledgeBase;
import com.snrt.knowledgebase.repository.ChatMessageRepository;
import com.snrt.knowledgebase.repository.ChatSessionRepository;
import com.snrt.knowledgebase.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    @Transactional(readOnly = true)
    public PageResult<ChatSessionDTO> listSessions(Integer page, Integer size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("updateTime").descending());
        Page<ChatSession> sessionPage;

        if (keyword != null && !keyword.isEmpty()) {
            sessionPage = sessionRepository.searchByTitle(keyword, pageable);
        } else {
            sessionPage = sessionRepository.findByIsDeletedFalseOrderByUpdateTimeDesc(pageable);
        }

        List<ChatSessionDTO> dtoList = sessionPage.getContent().stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, sessionPage.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public ChatSessionDTO getSession(String id) {
        ChatSession session = sessionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        return convertSessionToDTO(session);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getSessionMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId).stream()
                .map(this::convertMessageToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatSessionDTO createSession(String title, String knowledgeBaseId) {
        ChatSession session = new ChatSession();
        session.setTitle(title != null ? title : "新对话");

        if (knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
            KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(knowledgeBaseId)
                    .orElseThrow(() -> new RuntimeException("知识库不存在"));
            session.setKnowledgeBase(kb);
        }

        ChatSession saved = sessionRepository.save(session);
        return convertSessionToDTO(saved);
    }

    @Transactional
    public void deleteSession(String id) {
        ChatSession session = sessionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        session.setIsDeleted(true);
        sessionRepository.save(session);
    }

    @Transactional
    public String chat(ChatRequest request) {
        ChatSession session = getOrCreateSession(request);
        log.info("[大模型请求] 会话ID: {}, 知识库ID: {}, 用户消息: {}",
                session.getId(),
                request.getKnowledgeBaseId(),
                request.getMessage());

        saveMessage(session.getId(), ChatMessage.MessageRole.USER, request.getMessage());

        String knowledgeBaseId = session.getKnowledgeBase() != null ? session.getKnowledgeBase().getId() : null;
        String prompt = buildPrompt(request.getMessage(), knowledgeBaseId);
        log.debug("[大模型处理] 会话ID: {}, 构建的Prompt: {}", session.getId(), prompt);

        long startTime = System.currentTimeMillis();
        String response = chatModel.call(prompt);
        long costTime = System.currentTimeMillis() - startTime;

        log.info("[大模型回答] 会话ID: {}, 响应耗时: {}ms, 回答内容: {}",
                session.getId(),
                costTime,
                response);

        saveMessage(session.getId(), ChatMessage.MessageRole.ASSISTANT, response);

        return response;
    }

    @Transactional
    public Flux<String> streamChat(ChatRequest request) {
        ChatSession session = getOrCreateSession(request);
        log.info("[大模型流式请求] 会话ID: {}, 知识库ID: {}, 用户消息: {}",
                session.getId(),
                request.getKnowledgeBaseId(),
                request.getMessage());

        saveMessage(session.getId(), ChatMessage.MessageRole.USER, request.getMessage());

        String knowledgeBaseId = session.getKnowledgeBase() != null ? session.getKnowledgeBase().getId() : null;
        String prompt = buildPrompt(request.getMessage(), knowledgeBaseId);
        log.debug("[大模型流式处理] 会话ID: {}, 构建的Prompt: {}", session.getId(), prompt);

        StringBuilder fullResponse = new StringBuilder();
        long startTime = System.currentTimeMillis();

        return chatModel.stream(prompt)
                .doOnNext(chunk -> {
                    fullResponse.append(chunk);
                    log.debug("[大模型流式响应] 会话ID: {}, 接收数据块: {}", session.getId(), chunk);
                })
                .doOnComplete(() -> {
                    long costTime = System.currentTimeMillis() - startTime;
                    log.info("[大模型流式回答完成] 会话ID: {}, 响应耗时: {}ms, 完整回答: {}",
                            session.getId(),
                            costTime,
                            fullResponse.toString());
                    saveMessage(session.getId(), ChatMessage.MessageRole.ASSISTANT, fullResponse.toString());
                })
                .doOnError(error -> {
                    long costTime = System.currentTimeMillis() - startTime;
                    log.error("[大模型流式错误] 会话ID: {}, 耗时: {}ms, 错误信息: {}",
                            session.getId(),
                            costTime,
                            error.getMessage(),
                            error);
                });
    }

    private ChatSession getOrCreateSession(ChatRequest request) {
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            return sessionRepository.findByIdAndIsDeletedFalse(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("会话不存在"));
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

    private String buildPrompt(String message, String knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            log.debug("[Prompt构建] 未指定知识库，直接返回用户消息");
            return message;
        }

        log.info("[Prompt构建] 知识库ID: {}, 开始检索相关文档", knowledgeBaseId);

        // 从向量存储中检索与知识库相关的文档
        List<Document> relevantDocs = vectorStore.similaritySearch(message);
        log.debug("[Prompt构建] 检索到 {} 个相关文档", relevantDocs.size());

        // 过滤出属于指定知识库的文档
        List<Document> filteredDocs = relevantDocs.stream()
                .filter(doc -> {
                    Object kbId = doc.getMetadata().get("knowledgeBaseId");
                    return kbId != null && kbId.equals(knowledgeBaseId);
                })
                .collect(Collectors.toList());

        log.info("[Prompt构建] 知识库ID: {}, 过滤后剩余 {} 个文档", knowledgeBaseId, filteredDocs.size());

        String context = filteredDocs.stream()
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

    private void saveMessage(String sessionId, ChatMessage.MessageRole role, String content) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));

        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        messageRepository.save(message);
    }

    private ChatSessionDTO convertSessionToDTO(ChatSession session) {
        ChatSessionDTO dto = new ChatSessionDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        if (session.getKnowledgeBase() != null) {
            dto.setKnowledgeBaseId(session.getKnowledgeBase().getId());
            dto.setKnowledgeBaseName(session.getKnowledgeBase().getName());
        }
        dto.setMessageCount(session.getMessages().size());
        dto.setCreateTime(session.getCreateTime());
        dto.setUpdateTime(session.getUpdateTime());
        return dto;
    }

    private ChatMessageDTO convertMessageToDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setRole(message.getRole().name().toLowerCase());
        dto.setContent(message.getContent());
        dto.setCreateTime(message.getCreateTime());
        return dto;
    }
}
