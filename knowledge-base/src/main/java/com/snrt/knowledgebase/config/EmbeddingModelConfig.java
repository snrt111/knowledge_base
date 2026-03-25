package com.snrt.knowledgebase.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@Configuration
public class EmbeddingModelConfig {

    public static final String ZHIPU_AI = "zhiPuAiEmbeddingModel";
    public static final String OLLAMA = "ollamaEmbeddingModel";

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
