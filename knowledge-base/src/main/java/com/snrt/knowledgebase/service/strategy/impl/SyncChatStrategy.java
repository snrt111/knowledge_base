package com.snrt.knowledgebase.service.strategy.impl;

import com.snrt.knowledgebase.dto.ChatRequest;
import com.snrt.knowledgebase.dto.ChatResponseDTO;
import com.snrt.knowledgebase.dto.StreamChatResponse;
import com.snrt.knowledgebase.service.strategy.ChatStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncChatStrategy implements ChatStrategy {

    private final ChatStrategyHelper chatStrategyHelper;

    @Override
    public ChatResponseDTO chat(ChatRequest request) {
        return chatStrategyHelper.executeChat(request);
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
