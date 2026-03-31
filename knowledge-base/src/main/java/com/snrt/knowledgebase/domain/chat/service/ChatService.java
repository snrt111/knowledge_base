package com.snrt.knowledgebase.domain.chat.service;

import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.chat.dto.StreamChatResponse;
import com.snrt.knowledgebase.domain.chat.dto.ChatMessageDTO;
import com.snrt.knowledgebase.domain.chat.dto.ChatRequest;
import com.snrt.knowledgebase.domain.chat.dto.ChatResponseDTO;
import com.snrt.knowledgebase.domain.chat.dto.ChatSessionDTO;
import com.snrt.knowledgebase.domain.chat.entity.ChatSession;
import com.snrt.knowledgebase.common.exception.ResourceNotFoundException;
import com.snrt.knowledgebase.domain.chat.repository.ChatMessageMapper;
import com.snrt.knowledgebase.domain.chat.repository.ChatSessionMapper;
import com.snrt.knowledgebase.domain.chat.repository.ChatMessageRepository;
import com.snrt.knowledgebase.domain.chat.repository.ChatSessionRepository;
import com.snrt.knowledgebase.domain.knowledge.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatStrategyFactory strategyFactory;
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
            var kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(knowledgeBaseId)
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
        ChatStrategy strategy = strategyFactory.getStrategy(request.getStream());
        return strategy.chat(request);
    }

    @Transactional
    public Flux<StreamChatResponse> streamChat(ChatRequest request) {
        ChatStrategy strategy = strategyFactory.getStrategy(true);
        return strategy.streamChat(request);
    }

    private void enrichMessageCount(ChatSessionDTO dto) {
        if (dto != null && dto.getId() != null) {
            long count = messageRepository.countBySessionId(dto.getId());
            dto.setMessageCount((int) count);
        }
    }
}
