package com.snrt.knowledgebase.domain.chat.service.impl;

import com.snrt.knowledgebase.domain.chat.dto.ChatRequest;
import com.snrt.knowledgebase.domain.chat.dto.ChatResponseDTO;
import com.snrt.knowledgebase.domain.chat.dto.StreamChatResponse;
import com.snrt.knowledgebase.domain.chat.service.ChatExecutor;
import com.snrt.knowledgebase.domain.chat.service.ChatStrategy;
import com.snrt.knowledgebase.domain.chat.service.StreamChatExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncChatStrategy implements ChatStrategy {

    private final ChatExecutor chatExecutor;

    @Override
    public ChatResponseDTO chat(ChatRequest request) {
        return chatExecutor.executeSyncChat(request);
    }

    @Override
    public Flux<StreamChatResponse> streamChat(ChatRequest request) {
        return Flux.error(new UnsupportedOperationException("同步模式不支持流式响应"));
    }

    @Override
    public boolean supports(Boolean stream) {
        return stream == null || !stream;
    }
}
