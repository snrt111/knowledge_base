package com.snrt.knowledgebase.infrastructure.storage;

import com.snrt.knowledgebase.domain.document.service.RAGCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 缓存统计日志记录器
 *
 * 定时记录RAG缓存的命中率、大小等统计信息
 *
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class CacheStatsLogger {

    private final RAGCacheManager ragCacheManager;

    @Autowired
    public CacheStatsLogger(RAGCacheManager ragCacheManager) {
        this.ragCacheManager = ragCacheManager;
    }

    /**
     * 每5分钟记录一次缓存统计
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void logCacheStatsPeriodically() {
        try {
            ragCacheManager.logCacheStats();
        } catch (Exception e) {
            log.error("定时记录缓存统计失败", e);
        }
    }

    /**
     * 每小时重置一次Redis缓存统计计数器
     * 避免长时间累积导致统计失真
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void resetRedisStatsPeriodically() {
        try {
            log.info("定时重置Redis缓存统计计数器");
            ragCacheManager.resetStats();
        } catch (Exception e) {
            log.error("定时重置缓存统计失败", e);
        }
    }
}
