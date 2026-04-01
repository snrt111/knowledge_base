package com.snrt.knowledgebase.domain.chat.service;

import com.snrt.knowledgebase.domain.chat.dto.ChatRequest;
import com.snrt.knowledgebase.domain.chat.dto.ChatResponseDTO;
import com.snrt.knowledgebase.domain.chat.dto.StreamChatResponse;
import reactor.core.publisher.Flux;

/**
 * 聊天策略接口
 * 
 * 定义聊天的核心方法，支持同步和流式两种模式
 * 
 * @author SNRT
 * @since 1.0
 */
public interface ChatStrategy {

    /**
     * 同步聊天
     * 
     * @param request 聊天请求
     * @return 聊天响应DTO
     */
    ChatResponseDTO chat(ChatRequest request);

    /**
     * 流式聊天
     * 
     * @param request 聊天请求
     * @return 流式响应Flux
     */
    Flux<StreamChatResponse> streamChat(ChatRequest request);

    /**
     * 检查是否支持该流式模式
     * 
     * @param stream 是否流式
     * @return 是否支持
     */
    boolean supports(Boolean stream);
}
