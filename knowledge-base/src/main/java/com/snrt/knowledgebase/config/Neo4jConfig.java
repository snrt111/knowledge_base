package com.snrt.knowledgebase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "neo4j")
public class Neo4jConfig {

    private String uri = "bolt://localhost:7687";
    private String username = "neo4j";
    private String password = "password";
    private boolean enabled = true;
}
