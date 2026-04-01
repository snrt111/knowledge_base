package com.snrt.knowledgebase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 知识图谱配置
 * 
 * 配置知识图谱相关参数：
 * - 是否启用知识图谱
 * - 默认最大深度
 * - 默认返回结果数
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge-graph")
public class KnowledgeGraphConfig {

    private boolean enabled = true;

    private int defaultMaxDepth = 3;

    private int defaultTopK = 10;
}
