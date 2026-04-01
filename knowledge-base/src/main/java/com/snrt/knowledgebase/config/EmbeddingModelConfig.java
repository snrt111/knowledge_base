package com.snrt.knowledgebase.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * 嵌入模型配置
 * 
 * 配置和管理嵌入模型（EmbeddingModel）的Bean
 * 支持多种模型提供商（智谱AI、Ollama等）
 * 
 * @author SNRT
 * @since 1.0
 */
@Configuration
public class EmbeddingModelConfig {

    public static final String ZHIPU_AI = "zhiPuAiEmbeddingModel";
    public static final String OLLAMA = "ollamaEmbeddingModel";

    /**
     * 创建默认嵌入模型Bean
     * 
     * @param embeddingModels 所有嵌入模型Map
     * @param properties 聊天模型属性
     * @return 默认嵌入模型
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel(
            Map<String, EmbeddingModel> embeddingModels,
            ChatModelProperties properties) {
        return switch (properties.getProvider()) {
            case "ollama" -> embeddingModels.get(OLLAMA);
            case "zhipuai" -> embeddingModels.get(ZHIPU_AI);
            default -> embeddingModels.get(ZHIPU_AI);
        };
    }
}
