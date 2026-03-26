package com.snrt.knowledgebase.model;

import org.springframework.ai.chat.model.ChatModel;

public interface ChatModelProvider {

    String getProviderName();

    ChatModel createModel();
}
