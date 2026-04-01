package com.snrt.knowledgebase.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天策略工厂
 * 
 * 根据是否流式响应获取对应的聊天策略实现
 * 使用策略模式，支持同步和流式两种聊天模式
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStrategyFactory {

    private final List<ChatStrategy> strategies;

    /**
     * 根据流式标志获取对应的聊天策略
     * 
     * @param stream 是否流式响应
     * @return 聊天策略
     */
    public ChatStrategy getStrategy(Boolean stream) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(stream))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的聊天模式: " + stream));
    }
}
