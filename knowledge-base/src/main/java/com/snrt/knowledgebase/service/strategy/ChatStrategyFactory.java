package com.snrt.knowledgebase.service.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStrategyFactory {

    private final List<ChatStrategy> strategies;

    public ChatStrategy getStrategy(Boolean stream) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(stream))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的聊天模式: " + stream));
    }
}
