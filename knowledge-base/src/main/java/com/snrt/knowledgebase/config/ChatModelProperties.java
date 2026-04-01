package com.snrt.knowledgebase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chat.model")
public class ChatModelProperties {

    private String provider = "ollama";

    private String lightModelProvider;

    public String getLightModelProvider() {
        return lightModelProvider != null ? lightModelProvider : provider;
    }
}
