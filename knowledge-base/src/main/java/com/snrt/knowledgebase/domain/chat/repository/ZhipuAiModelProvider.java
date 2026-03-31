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
public class ZhipuAiModelProvider implements ChatModelProvider {

    @Qualifier("zhipuAiChatModel")
    private final ChatModel zhipuAiChatModel;

    @Override
    public String getProviderName() {
        return Constants.Chat.ZHIPU_AI_PROVIDER;
    }

    @Override
    public ChatModel createModel() {
        log.debug("创建智谱AI模型实例");
        return zhipuAiChatModel;
    }
}
