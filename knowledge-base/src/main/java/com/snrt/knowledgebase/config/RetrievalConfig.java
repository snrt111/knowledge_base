package com.snrt.knowledgebase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 检索配置
 *
 * @author SNRT
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "retrieval")
public class RetrievalConfig {

    /**
     * 是否启用多路召回
     */
    private boolean multiRetrieveEnabled = true;

    /**
     * 是否启用重排序
     */
    private boolean rerankEnabled = true;

    /**
     * 向量检索召回数量
     */
    private int vectorRecallCount = 20;

    /**
     * 关键词检索召回数量
     */
    private int keywordRecallCount = 10;

    /**
     * 最终返回结果数量
     */
    private int topK = 5;

    /**
     * RRF融合算法的k值
     */
    private int rrfK = 60;

    /**
     * 相似度阈值
     */
    private double similarityThreshold = 0.5;

    /**
     * 是否启用全文检索
     */
    private boolean fullTextSearchEnabled = true;

    /**
     * 重排序模式：model（模型）/ rule（规则）
     */
    private String rerankMode = "rule";

    /**
     * 模型重排序的最大候选数
     */
    private int modelRerankMaxCandidates = 10;
}
