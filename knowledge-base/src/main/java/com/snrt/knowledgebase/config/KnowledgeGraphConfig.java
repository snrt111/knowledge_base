package com.snrt.knowledgebase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "knowledge-graph")
public class KnowledgeGraphConfig {

    private boolean enabled = true;

    private int defaultMaxDepth = 3;

    private int defaultTopK = 10;
}
