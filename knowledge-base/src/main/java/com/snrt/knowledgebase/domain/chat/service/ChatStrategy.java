package com.snrt.knowledgebase.domain.chat.service;

import com.snrt.knowledgebase.domain.chat.dto.ChatRequest;
import com.snrt.knowledgebase.domain.chat.dto.ChatResponseDTO;
import com.snrt.knowledgebase.domain.chat.dto.StreamChatResponse;
import reactor.core.publisher.Flux;

public interface ChatStrategy {

    ChatResponseDTO chat(ChatRequest request);

    Flux<StreamChatResponse> streamChat(ChatRequest request);

    boolean supports(Boolean stream);
}
