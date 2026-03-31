package com.snrt.knowledgebase.common.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RAG缓存统计DTO
 *
 * 用于暴露缓存命中率、大小等监控指标
 *
 * @author SNRT
 * @since 1.0
 */
@Data
@Builder
public class CacheStatsDTO {

    /**
     * 统计时间戳
     */
    private LocalDateTime timestamp;

    /**
     * Embedding缓存统计
     */
    private CacheDetailStats embeddingCacheStats;

    /**
     * 本地检索缓存统计
     */
    private CacheDetailStats localSearchCacheStats;

    /**
     * Redis缓存统计（估算）
     */
    private RedisCacheStats redisCacheStats;

    /**
     * 综合命中率
     */
    private Double overallHitRate;

    /**
     * 缓存详细统计
     */
    @Data
    @Builder
    public static class CacheDetailStats {
        /**
         * 缓存名称
         */
        private String cacheName;

        /**
         * 当前缓存条目数
         */
        private Long size;

        /**
         * 命中率 (0.0 - 1.0)
         */
        private Double hitRate;

        /**
         * 命中次数
         */
        private Long hitCount;

        /**
         * 未命中次数
         */
        private Long missCount;

        /**
         * 加载次数
         */
        private Long loadCount;

        /**
         * 加载失败次数
         */
        private Long loadFailureCount;

        /**
         * 平均加载时间(ms)
         */
        private Double averageLoadPenalty;

        /**
         * 驱逐次数
         */
        private Long evictionCount;

        /**
         * 总请求数
         */
        private Long totalRequests;

        /**
         * 缓存状态: HEALTHY(健康)、WARNING(警告)、CRITICAL(临界)
         */
        private String status;
    }

    /**
     * Redis缓存统计
     */
    @Data
    @Builder
    public static class RedisCacheStats {
        /**
         * Redis连接状态
         */
        private Boolean connected;

        /**
         * 检索结果缓存键数量（估算）
         */
        private Long searchResultKeyCount;

        /**
         * 聊天响应缓存键数量（估算）
         */
        private Long chatResponseKeyCount;

        /**
         * HyDE假设答案缓存键数量（估算）
         */
        private Long hydeKeyCount;

        /**
         * 查询改写缓存键数量（估算）
         */
        private Long rewriteKeyCount;

        /**
         * 总键数量
         */
        private Long totalKeyCount;

        /**
         * 内存使用量（字节）
         */
        private Long memoryUsage;
    }
}
