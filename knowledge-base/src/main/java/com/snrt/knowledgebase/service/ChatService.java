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

        saveMessage(session.getId(), ChatMessage.MessageRole.USER, request.getMessage());

        String knowledgeBaseId = session.getKnowledgeBase() != null ? session.getKnowledgeBase().getId() : null;
        String prompt = buildPrompt(request.getMessage(), knowledgeBaseId);

        String response = chatModel.call(prompt);

        saveMessage(session.getId(), ChatMessage.MessageRole.ASSISTANT, response);

        return response;
    }

    @Transactional
    public Flux<String> streamChat(ChatRequest request) {
        ChatSession session = getOrCreateSession(request);

        saveMessage(session.getId(), ChatMessage.MessageRole.USER, request.getMessage());

        String knowledgeBaseId = session.getKnowledgeBase() != null ? session.getKnowledgeBase().getId() : null;
        String prompt = buildPrompt(request.getMessage(), knowledgeBaseId);

        StringBuilder fullResponse = new StringBuilder();

        return chatModel.stream(prompt)
                .doOnNext(chunk -> fullResponse.append(chunk))
                .doOnComplete(() -> {
                    saveMessage(session.getId(), ChatMessage.MessageRole.ASSISTANT, fullResponse.toString());
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
            return message;
        }

        List<Document> relevantDocs = vectorStore.similaritySearch(message);

        String context = relevantDocs.stream()
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
