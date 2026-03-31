package com.snrt.knowledgebase.domain.chat.repository;

import com.snrt.knowledgebase.common.constants.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaModelProvider implements ChatModelProvider {

    @Qualifier("ollamaChatModel")
    private final ChatModel ollamaChatModel;

    @Override
    public String getProviderName() {
        return Constants.Chat.OLLAMA_PROVIDER;
    }

    @Override
    public ChatModel createModel() {
        log.debug("创建Ollama模型实例");
        return ollamaChatModel;
    }
}
