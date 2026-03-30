package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.dto.ApiResponse;
import com.snrt.knowledgebase.dto.CacheStatsDTO;
import com.snrt.knowledgebase.service.RAGCacheManager;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 缓存监控控制器
 *
 * 提供缓存命中率、大小等监控指标的查询接口
 *
 * @author SNRT
 * @since 1.0
 */
@Tag(name = "系统监控", description = "缓存监控和系统健康检查")
@Slf4j
@RestController
@RequestMapping("/api/monitor")
public class CacheMonitorController {

    private final RAGCacheManager ragCacheManager;

    @Autowired
    public CacheMonitorController(RAGCacheManager ragCacheManager) {
        this.ragCacheManager = ragCacheManager;
    }

    /**
     * 获取RAG缓存完整统计信息
     *
     * @return 缓存统计DTO
     */
    @GetMapping("/cache/stats")
    public ApiResponse<CacheStatsDTO> getCacheStats() {
        log.info("查询RAG缓存统计信息");
        CacheStatsDTO stats = ragCacheManager.getCacheStats();
        return ApiResponse.success(stats);
    }

    /**
     * 获取缓存健康状态
     *
     * @return 健康状态摘要
     */
    @GetMapping("/cache/health")
    public ApiResponse<Map<String, Object>> getCacheHealth() {
        CacheStatsDTO stats = ragCacheManager.getCacheStats();

        Map<String, Object> health = Map.of(
                "timestamp", stats.getTimestamp(),
                "overallHitRate", stats.getOverallHitRate(),
                "overallStatus", determineOverallStatus(stats),
                "embeddingCacheStatus", stats.getEmbeddingCacheStats().getStatus(),
                "localSearchCacheStatus", stats.getLocalSearchCacheStats().getStatus(),
                "redisConnected", stats.getRedisCacheStats().getConnected()
        );

        return ApiResponse.success(health);
    }

    /**
     * 记录当前缓存统计到日志
     *
     * @return 操作结果
     */
    @PostMapping("/cache/log")
    public ApiResponse<Map<String, String>> logCacheStats() {
        log.info("手动触发缓存统计日志记录");
        ragCacheManager.logCacheStats();
        return ApiResponse.success(Map.of("message", "缓存统计已记录到日志"));
    }

    /**
     * 重置缓存统计计数器
     *
     * @return 操作结果
     */
    @PostMapping("/cache/reset")
    public ApiResponse<Map<String, String>> resetCacheStats() {
        log.info("手动重置缓存统计计数器");
        ragCacheManager.resetStats();
        return ApiResponse.success(Map.of("message", "缓存统计计数器已重置"));
    }

    /**
     * 获取缓存命中率摘要
     *
     * @return 命中率信息
     */
    @GetMapping("/cache/hit-rate")
    public ApiResponse<Map<String, Object>> getHitRate() {
        CacheStatsDTO stats = ragCacheManager.getCacheStats();

        Map<String, Object> hitRateInfo = Map.of(
                "overallHitRate", String.format("%.2f%%", stats.getOverallHitRate() * 100),
                "embeddingCacheHitRate", String.format("%.2f%%", stats.getEmbeddingCacheStats().getHitRate() * 100),
                "localSearchCacheHitRate", String.format("%.2f%%", stats.getLocalSearchCacheStats().getHitRate() * 100),
                "embeddingCacheRequests", stats.getEmbeddingCacheStats().getTotalRequests(),
                "localSearchCacheRequests", stats.getLocalSearchCacheStats().getTotalRequests()
        );

        return ApiResponse.success(hitRateInfo);
    }

    /**
     * 确定整体缓存状态
     */
    private String determineOverallStatus(CacheStatsDTO stats) {
        double hitRate = stats.getOverallHitRate();
        boolean redisConnected = stats.getRedisCacheStats().getConnected();

        if (!redisConnected) {
            return "DEGRADED"; // Redis连接异常，降级状态
        }

        if (hitRate >= 0.7) {
            return "HEALTHY";
        } else if (hitRate >= 0.4) {
            return "WARNING";
        } else {
            return "CRITICAL";
        }
    }
}
