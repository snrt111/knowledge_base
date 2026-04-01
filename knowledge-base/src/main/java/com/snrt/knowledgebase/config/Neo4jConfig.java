package com.snrt.knowledgebase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Neo4j配置
 * 
 * 配置Neo4j图数据库的连接参数
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "neo4j")
public class Neo4jConfig {

    private String uri = "bolt://localhost:7687";
    private String username = "neo4j";
    private String password = "password";
    private boolean enabled = true;
}
