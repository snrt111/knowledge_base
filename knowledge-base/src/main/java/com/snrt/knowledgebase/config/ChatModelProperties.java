package com.snrt.knowledgebase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 聊天模型属性配置
 * 
 * 配置聊天模型的相关属性：
 * - 模型提供商（provider）
 * - 轻量级模型提供商（lightModelProvider）
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "chat.model")
public class ChatModelProperties {

    private String provider = "ollama";

    private String lightModelProvider;

    /**
     * 获取轻量级模型提供商
     * 如果未配置，则使用默认提供商
     * 
     * @return 轻量级模型提供商
     */
    public String getLightModelProvider() {
        return lightModelProvider != null ? lightModelProvider : provider;
    }
}
