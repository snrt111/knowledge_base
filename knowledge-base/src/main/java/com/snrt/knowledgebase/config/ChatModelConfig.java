package com.snrt.knowledgebase.config;

import com.snrt.knowledgebase.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@Slf4j
@Configuration
public class ChatModelConfig {

    public static final String ZHIPU_AI = "zhipuAiChatModel";
    public static final String OLLAMA = "ollamaChatModel";

    @Bean
    @Primary
    public ChatModel chatModel(
            Map<String, ChatModel> chatModels,
            ChatModelProperties properties) {
        String provider = properties.getProvider() != null ? properties.getProvider() : Constants.Chat.DEFAULT_PROVIDER;
        
        ChatModel model = switch (provider) {
            case Constants.Chat.OLLAMA_PROVIDER -> chatModels.get(OLLAMA);
            case Constants.Chat.ZHIPU_AI_PROVIDER -> chatModels.get(ZHIPU_AI);
            default -> chatModels.get(ZHIPU_AI);
        };
        
        log.info("初始化ChatModel: provider={}", provider);
        return model;
    }
}
