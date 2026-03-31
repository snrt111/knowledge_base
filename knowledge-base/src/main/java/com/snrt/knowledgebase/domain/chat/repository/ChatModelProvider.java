package com.snrt.knowledgebase.domain.chat.repository;

import org.springframework.ai.chat.model.ChatModel;

public interface ChatModelProvider {

    String getProviderName();

    ChatModel createModel();
}
