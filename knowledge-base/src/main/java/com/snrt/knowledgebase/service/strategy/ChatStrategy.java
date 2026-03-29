package com.snrt.knowledgebase.service.strategy;

import com.snrt.knowledgebase.dto.ChatRequest;
import com.snrt.knowledgebase.dto.ChatResponseDTO;
import com.snrt.knowledgebase.dto.StreamChatResponse;
import reactor.core.publisher.Flux;

public interface ChatStrategy {

    ChatResponseDTO chat(ChatRequest request);

    Flux<StreamChatResponse> streamChat(ChatRequest request);

    boolean supports(Boolean stream);
}
