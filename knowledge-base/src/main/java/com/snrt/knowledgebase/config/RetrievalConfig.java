package com.snrt.knowledgebase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 检索配置
 * 
 * 配置多路召回、BM25、向量检索等参数
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "retrieval")
public class RetrievalConfig {

    /**
     * 多路召回配置
     */
    private MultiRetrieveConfig multiRetrieve = new MultiRetrieveConfig();

    /**
     * BM25全文检索配置
     */
    private BM25Config bm25 = new BM25Config();

    /**
     * 向量检索配置
     */
    private VectorConfig vector = new VectorConfig();

    /**
     * 多路召回配置
     * 
     * 配置多路召回策略：
     * - 向量检索和全文检索结果融合
     * - RRF（Reciprocal Rank Fusion）算法参数
     * - 各路召回的倍数配置
     */
    @Data
    public static class MultiRetrieveConfig {
        /**
         * 是否启用多路召回
         */
        private boolean enabled = true;

        /**
         * RRF融合算法的k值
         * 
         * 用于计算RRF得分，k值越大，排名越靠前的文档得分优势越明显
         */
        private int rrfK = 60;

        /**
         * 向量检索召回倍数
         * 
         * 向量检索返回的结果数 = topK * vectorRecallMultiplier
         */
        private int vectorRecallMultiplier = 2;

        /**
         * 全文检索召回倍数
         * 
         * 全文检索返回的结果数 = topK * fullTextRecallMultiplier
         */
        private int fullTextRecallMultiplier = 1;
    }

    /**
     * BM25全文检索配置
     * 
     * 配置BM25算法参数：
     * - 词频饱和度参数k1
     * - 文档长度归一化参数b
     * - 前缀匹配、同义词扩展、模糊匹配等高级特性
     */
    @Data
    public static class BM25Config {
        /**
         * 是否启用BM25全文检索
         */
        private boolean enabled = true;

        /**
         * 词频饱和度参数 (k1)
         * 
         * 值越大，词频对评分的影响越大
         * 典型值：1.2-2.0
         */
        private float k1 = 1.2f;

        /**
         * 文档长度归一化参数 (b)
         * 
         * 0.0-1.0，0表示不归一化，1表示完全归一化
         * 典型值：0.75
         */
        private float b = 0.75f;

        /**
         * 是否启用前缀匹配
         */
        private boolean prefixMatch = true;

        /**
         * 是否启用查询扩展（同义词）
         */
        private boolean expandSynonyms = false;

        /**
         * 是否启用模糊匹配
         */
        private boolean fuzzyMatch = false;

        /**
         * 是否要求所有查询词都匹配
         */
        private boolean requireAllTerms = false;

        /**
         * 是否使用文档长度归一化
         */
        private boolean useNormalization = true;
    }

    /**
     * 向量检索配置
     * 
     * 配置向量相似度检索参数：
     * - 相似度阈值
     * - 默认返回结果数
     */
    @Data
    public static class VectorConfig {
        /**
         * 相似度阈值
         * 
         * 只返回相似度大于该阈值的文档
         * 范围：0.0-1.0
         */
        private double similarityThreshold = 0.3;

        /**
         * 默认返回结果数
         */
        private int defaultTopK = 10;
    }
}
