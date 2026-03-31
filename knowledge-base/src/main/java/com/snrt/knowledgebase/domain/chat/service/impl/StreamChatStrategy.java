package com.snrt.knowledgebase.domain.chat.service.impl;

import com.snrt.knowledgebase.domain.chat.dto.ChatRequest;
import com.snrt.knowledgebase.domain.chat.dto.ChatResponseDTO;
import com.snrt.knowledgebase.domain.chat.dto.StreamChatResponse;
import com.snrt.knowledgebase.domain.chat.service.ChatStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamChatStrategy implements ChatStrategy {

    private final ChatStrategyHelper chatStrategyHelper;

    @Override
    public ChatResponseDTO chat(ChatRequest request) {
        throw new UnsupportedOperationException("流式模式不支持同步响应");
    }

    @Override
    public Flux<StreamChatResponse> streamChat(ChatRequest request) {
        return chatStrategyHelper.executeStreamChat(request);
    }

    @Override
    public boolean supports(Boolean stream) {
        return stream != null && stream;
    }
}
