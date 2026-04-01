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

/**
 * 聊天服务
 * 
 * 提供完整的聊天对话管理功能：
 * - 会话管理（创建、查询、删除）
 * - 消息管理（查询会话消息）
 * - 智能聊天（普通聊天和流式聊天）
 * - 会话统计（消息数量）
 * 
 * @author SNRT
 * @since 1.0
 */
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

    /**
     * 分页查询会话列表
     * 
     * 支持按关键词搜索会话标题：
     * - 按更新时间倒序排列
     * - 支持关键词搜索
     * - 统计每个会话的消息数量
     * 
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词（可选）
     * @return 会话分页结果
     */
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

    /**
     * 获取单个会话详情
     * 
     * @param id 会话ID
     * @return 会话DTO
     * @throws ResourceNotFoundException 会话不存在时抛出
     */
    @Transactional(readOnly = true)
    public ChatSessionDTO getSession(String id) {
        ChatSession session = sessionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("会话", id));
        ChatSessionDTO dto = chatSessionMapper.toDTO(session);
        enrichMessageCount(dto);
        return dto;
    }

    /**
     * 查询会话消息列表
     * 
     * 按创建时间正序排列，返回完整的对话历史
     * 
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getSessionMessages(String sessionId) {
        return chatMessageMapper.toDTOList(
                messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId)
        );
    }

    /**
     * 创建新会话
     * 
     * @param title 会话标题（可选）
     * @param knowledgeBaseId 知识库ID（可选）
     * @return 会话DTO
     */
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

    /**
     * 删除会话（软删除）
     * 
     * @param id 会话ID
     * @throws ResourceNotFoundException 会话不存在时抛出
     */
    @Transactional
    public void deleteSession(String id) {
        ChatSession session = sessionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("会话", id));
        session.setIsDeleted(true);
        sessionRepository.save(session);
        log.info("会话删除成功: id={}", id);
    }

    /**
     * 普通聊天（非流式）
     * 
     * 使用策略模式选择聊天策略：
     * - 非流式策略：一次性返回完整响应
     * 
     * @param request 聊天请求
     * @return 聊天响应DTO
     */
    @Transactional
    public ChatResponseDTO chat(ChatRequest request) {
        ChatStrategy strategy = strategyFactory.getStrategy(request.getStream());
        return strategy.chat(request);
    }

    /**
     * 流式聊天
     * 
     * 使用策略模式选择聊天策略：
     * - 流式策略：逐字返回响应（Server-Sent Events）
     * 
     * @param request 聊天请求
     * @return 流式响应Flux
     */
    @Transactional
    public Flux<StreamChatResponse> streamChat(ChatRequest request) {
        ChatStrategy strategy = strategyFactory.getStrategy(true);
        return strategy.streamChat(request);
    }

    /**
     * 补充消息数量信息
     * 
     * @param dto 会话DTO
     */
    private void enrichMessageCount(ChatSessionDTO dto) {
        if (dto != null && dto.getId() != null) {
            long count = messageRepository.countBySessionId(dto.getId());
            dto.setMessageCount((int) count);
        }
    }
}
