package com.snrt.knowledgebase.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@Configuration
public class ChatModelConfig {

    public static final String ZHIPU_AI = "zhipuAiChatModel";
    public static final String OLLAMA = "ollamaChatModel";

    @Bean
    @Primary
    public ChatModel chatModel(
            Map<String, ChatModel> chatModels,
            ChatModelProperties properties) {
        return switch (properties.getProvider()) {
            case "ollama" -> chatModels.get(OLLAMA);
            case "zhipuai" -> chatModels.get(ZHIPU_AI);
            default -> chatModels.get(ZHIPU_AI);
        };
    }
}
