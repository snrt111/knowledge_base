package com.snrt.knowledgebase.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snrt.knowledgebase.common.response.ApiResponse;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.chat.dto.ChatMessageDTO;
import com.snrt.knowledgebase.domain.chat.dto.ChatRequest;
import com.snrt.knowledgebase.domain.chat.dto.ChatResponseDTO;
import com.snrt.knowledgebase.domain.chat.dto.ChatSessionDTO;
import com.snrt.knowledgebase.domain.chat.dto.CreateChatSessionRequest;
import com.snrt.knowledgebase.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天控制器
 * 
 * 提供智能问答相关的REST API接口：
 * - 会话管理：创建、查询、删除会话
 * - 消息管理：查询会话消息
 * - 智能问答：同步和流式聊天
 * 
 * @author SNRT
 * @since 1.0
 */
@Tag(name = "智能问答", description = "聊天会话管理和 AI 智能问答功能")
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询会话列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词
     * @return 会话列表分页结果
     */
    @GetMapping("/sessions")
    public ApiResponse<PageResult<ChatSessionDTO>> listSessions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(chatService.listSessions(page, size, keyword));
    }

    /**
     * 查询会话详情
     * 
     * @param id 会话ID
     * @return 会话详情
     */
    @GetMapping("/sessions/{id}")
    public ApiResponse<ChatSessionDTO> getSession(@PathVariable String id) {
        return ApiResponse.success(chatService.getSession(id));
    }

    /**
     * 查询会话消息列表
     * 
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessageDTO>> getSessionMessages(@PathVariable String id) {
        return ApiResponse.success(chatService.getSessionMessages(id));
    }

    /**
     * 创建新会话
     * 
     * @param request 创建请求
     * @return 创建的会话
     */
    @PostMapping("/sessions")
    public ApiResponse<ChatSessionDTO> createSession(@Valid @RequestBody CreateChatSessionRequest request) {
        return ApiResponse.success(chatService.createSession(request.getTitle(), request.getKnowledgeBaseId()));
    }

    /**
     * 删除会话
     * 
     * @param id 会话ID
     * @return 删除结果
     */
    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable String id) {
        chatService.deleteSession(id);
        return ApiResponse.success();
    }

    /**
     * 同步聊天
     * 
     * @param request 聊天请求
     * @return AI回答
     */
    @PostMapping
    public ApiResponse<ChatResponseDTO> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.chat(request));
    }

    /**
     * 流式聊天（SSE）
     * 
     * @param message 用户消息
     * @param sessionId 会话ID
     * @param knowledgeBaseId 知识库ID
     * @return 流式响应
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String knowledgeBaseId) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setSessionId(sessionId);
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setStream(true);

        return chatService.streamChat(request)
                .map(response -> {
                    try {
                        return objectMapper.writeValueAsString(response);
                    } catch (JsonProcessingException e) {
                        log.error("序列化流式响应失败", e);
                        return "{\"type\":\"error\",\"content\":\"序列化失败\"}";
                    }
                });
    }
}
