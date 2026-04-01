package com.snrt.knowledgebase.config;

import com.snrt.knowledgebase.common.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * 聊天模型配置
 * 
 * 配置和管理聊天模型（ChatModel）的Bean
 * 支持多种模型提供商（智谱AI、Ollama等）
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Configuration
public class ChatModelConfig {

    public static final String ZHIPU_AI = "zhipuAiChatModel";
    public static final String OLLAMA = "ollamaChatModel";

    /**
     * 创建默认聊天模型Bean
     * 
     * @param chatModels 所有聊天模型Map
     * @param properties 聊天模型属性
     * @return 默认聊天模型
     */
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
