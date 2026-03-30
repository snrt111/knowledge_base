package com.snrt.knowledgebase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.snrt.knowledgebase.dto.CacheStatsDTO;
import com.snrt.knowledgebase.dto.DocumentSourceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class RAGCacheManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // L1: 本地缓存 - Embedding结果缓存
    private Cache<String, float[]> embeddingCache;

    // L1: 本地缓存 - 热点Query响应缓存
    private Cache<String, CachedSearchResult> localSearchCache;

    // 缓存统计计数器
    private final AtomicLong redisHitCount = new AtomicLong(0);
    private final AtomicLong redisMissCount = new AtomicLong(0);
    private final AtomicLong chatResponseHitCount = new AtomicLong(0);
    private final AtomicLong chatResponseMissCount = new AtomicLong(0);

    private static final String REDIS_KEY_PREFIX = "rag:search:";
    private static final String REDIS_RESPONSE_PREFIX = "rag:response:";
    private static final String REDIS_HYDE_PREFIX = "rag:hyde:";
    private static final String REDIS_REWRITE_PREFIX = "rag:rewrite:";
    private static final Duration REDIS_TTL = Duration.ofMinutes(10);
    private static final Duration LOCAL_TTL = Duration.ofMinutes(5);

    // 命中率阈值配置
    private static final double HIT_RATE_HEALTHY = 0.7;
    private static final double HIT_RATE_WARNING = 0.4;

    @PostConstruct
    public void init() {
        embeddingCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats()
                .build();

        localSearchCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(LOCAL_TTL)
                .recordStats()
                .build();

        log.info("RAG缓存管理器初始化完成");
    }

    /**
     * 生成查询的缓存键（使用MD5哈希）
     */
    public String generateCacheKey(String query) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(query.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("生成缓存键失败", e);
            return String.valueOf(query.hashCode());
        }
    }

    /**
     * 生成语义缓存键（基于SimHash思想，相似查询使用相同键）
     */
    public String generateSemanticKey(String query) {
        String normalized = query.toLowerCase()
                .replaceAll("[^\\u4e00-\\u9fa5a-z0-9]", "")
                .trim();
        if (normalized.isEmpty()) {
            normalized = query.toLowerCase().trim();
        }
        return generateCacheKey(normalized);
    }

    // ==================== Embedding缓存 (L1本地缓存) ====================

    public void cacheEmbedding(String text, float[] embedding) {
        String key = generateCacheKey(text);
        embeddingCache.put(key, embedding);
        log.debug("Embedding缓存已保存: key={}", key);
    }

    public Optional<float[]> getCachedEmbedding(String text) {
        String key = generateCacheKey(text);
        float[] cached = embeddingCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Embedding缓存命中: key={}", key);
            return Optional.of(cached);
        }
        return Optional.empty();
    }

    // ==================== 检索结果缓存 (L1本地 + L2 Redis) ====================

    public void cacheSearchResult(String query, String knowledgeBaseId, List<Document> results, List<DocumentSourceDTO> sources) {
        String semanticKey = generateSemanticKey(query);
        String cacheKey = REDIS_KEY_PREFIX + knowledgeBaseId + ":" + semanticKey;

        CachedSearchResult cachedResult = new CachedSearchResult(results, sources);

        // L1: 本地缓存
        localSearchCache.put(cacheKey, cachedResult);

        // L2: Redis缓存（序列化存储）
        try {
            String json = serializeSearchResult(cachedResult);
            redisTemplate.opsForValue().set(cacheKey, json, REDIS_TTL);
            log.debug("检索结果缓存已保存: key={}, documents={}", cacheKey, results.size());
        } catch (Exception e) {
            log.warn("Redis缓存保存失败: {}", e.getMessage());
        }
    }

    public Optional<CachedSearchResult> getCachedSearchResult(String query, String knowledgeBaseId) {
        String semanticKey = generateSemanticKey(query);
        String cacheKey = REDIS_KEY_PREFIX + knowledgeBaseId + ":" + semanticKey;

        // L1: 先查本地缓存
        CachedSearchResult localResult = localSearchCache.getIfPresent(cacheKey);
        if (localResult != null) {
            log.debug("检索结果本地缓存命中: key={}", cacheKey);
            return Optional.of(localResult);
        }

        // L2: 查Redis缓存
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                CachedSearchResult result = deserializeSearchResult(json);
                // 回填本地缓存
                localSearchCache.put(cacheKey, result);
                redisHitCount.incrementAndGet();
                log.debug("检索结果Redis缓存命中: key={}", cacheKey);
                return Optional.of(result);
            } else {
                redisMissCount.incrementAndGet();
            }
        } catch (Exception e) {
            log.warn("Redis缓存读取失败: {}", e.getMessage());
            redisMissCount.incrementAndGet();
        }

        return Optional.empty();
    }

    // ==================== 聊天响应缓存 (L2 Redis) ====================

    public void cacheChatResponse(String query, String knowledgeBaseId, String response) {
        String semanticKey = generateSemanticKey(query);
        String cacheKey = REDIS_RESPONSE_PREFIX + knowledgeBaseId + ":" + semanticKey;

        try {
            redisTemplate.opsForValue().set(cacheKey, response, Duration.ofMinutes(5));
            log.debug("聊天响应缓存已保存: key={}", cacheKey);
        } catch (Exception e) {
            log.warn("聊天响应缓存保存失败: {}", e.getMessage());
        }
    }

    public Optional<String> getCachedChatResponse(String query, String knowledgeBaseId) {
        String semanticKey = generateSemanticKey(query);
        String cacheKey = REDIS_RESPONSE_PREFIX + knowledgeBaseId + ":" + semanticKey;

        try {
            String response = redisTemplate.opsForValue().get(cacheKey);
            if (response != null) {
                chatResponseHitCount.incrementAndGet();
                log.debug("聊天响应缓存命中: key={}", cacheKey);
                return Optional.of(response);
            } else {
                chatResponseMissCount.incrementAndGet();
            }
        } catch (Exception e) {
            log.warn("聊天响应缓存读取失败: {}", e.getMessage());
            chatResponseMissCount.incrementAndGet();
        }

        return Optional.empty();
    }

    // ==================== HyDE假设答案缓存 (L2 Redis) ====================

    public void cacheHypotheticalAnswer(String query, String answer) {
        String semanticKey = generateSemanticKey(query);
        String cacheKey = REDIS_HYDE_PREFIX + semanticKey;

        try {
            redisTemplate.opsForValue().set(cacheKey, answer, Duration.ofMinutes(10));
            log.debug("HyDE假设答案缓存已保存: key={}", cacheKey);
        } catch (Exception e) {
            log.warn("HyDE假设答案缓存保存失败: {}", e.getMessage());
        }
    }

    public Optional<String> getCachedHypotheticalAnswer(String query) {
        String semanticKey = generateSemanticKey(query);
        String cacheKey = REDIS_HYDE_PREFIX + semanticKey;

        try {
            String answer = redisTemplate.opsForValue().get(cacheKey);
            if (answer != null) {
                log.debug("HyDE假设答案缓存命中: key={}", cacheKey);
                return Optional.of(answer);
            }
        } catch (Exception e) {
            log.warn("HyDE假设答案缓存读取失败: {}", e.getMessage());
        }

        return Optional.empty();
    }

    // ==================== 查询改写缓存 (L2 Redis) ====================

    public void cacheRewrittenQuery(String query, Object rewrittenQuery) {
        String semanticKey = generateSemanticKey(query);
        String cacheKey = REDIS_REWRITE_PREFIX + semanticKey;

        try {
            String json = objectMapper.writeValueAsString(rewrittenQuery);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(10));
            log.debug("查询改写结果缓存已保存: key={}", cacheKey);
        } catch (Exception e) {
            log.warn("查询改写结果缓存保存失败: {}", e.getMessage());
        }
    }

    public Optional<com.snrt.knowledgebase.service.retrieval.QueryRewriterService.RewrittenQuery> getCachedRewrittenQuery(String query) {
        String semanticKey = generateSemanticKey(query);
        String cacheKey = REDIS_REWRITE_PREFIX + semanticKey;

        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                com.snrt.knowledgebase.service.retrieval.QueryRewriterService.RewrittenQuery result = 
                    objectMapper.readValue(json, com.snrt.knowledgebase.service.retrieval.QueryRewriterService.RewrittenQuery.class);
                log.debug("查询改写结果缓存命中: key={}", cacheKey);
                return Optional.of(result);
            }
        } catch (Exception e) {
            log.warn("查询改写结果缓存读取失败: {}", e.getMessage());
        }

        return Optional.empty();
    }

    // ==================== 缓存统计 ====================

    /**
     * 获取完整的缓存统计信息
     */
    public CacheStatsDTO getCacheStats() {
        CacheStats embeddingStats = embeddingCache.stats();
        CacheStats localSearchStats = localSearchCache.stats();

        CacheStatsDTO.CacheDetailStats embeddingDetail = buildCacheDetailStats(
                "Embedding缓存", embeddingCache.estimatedSize(), embeddingStats);

        CacheStatsDTO.CacheDetailStats localSearchDetail = buildCacheDetailStats(
                "本地检索缓存", localSearchCache.estimatedSize(), localSearchStats);

        CacheStatsDTO.RedisCacheStats redisDetail = buildRedisCacheStats();

        // 计算综合命中率
        double overallHitRate = calculateOverallHitRate(embeddingStats, localSearchStats);

        return CacheStatsDTO.builder()
                .timestamp(LocalDateTime.now())
                .embeddingCacheStats(embeddingDetail)
                .localSearchCacheStats(localSearchDetail)
                .redisCacheStats(redisDetail)
                .overallHitRate(overallHitRate)
                .build();
    }

    /**
     * 构建本地缓存详细统计
     */
    private CacheStatsDTO.CacheDetailStats buildCacheDetailStats(String cacheName, long size, CacheStats stats) {
        long hitCount = stats.hitCount();
        long missCount = stats.missCount();
        long totalRequests = hitCount + missCount;
        double hitRate = totalRequests > 0 ? (double) hitCount / totalRequests : 0.0;

        return CacheStatsDTO.CacheDetailStats.builder()
                .cacheName(cacheName)
                .size(size)
                .hitRate(hitRate)
                .hitCount(hitCount)
                .missCount(missCount)
                .loadCount(stats.loadCount())
                .loadFailureCount(stats.loadFailureCount())
                .averageLoadPenalty(stats.averageLoadPenalty() / 1_000_000.0) // 转换为毫秒
                .evictionCount(stats.evictionCount())
                .totalRequests(totalRequests)
                .status(determineCacheStatus(hitRate))
                .build();
    }

    /**
     * 构建Redis缓存统计
     */
    private CacheStatsDTO.RedisCacheStats buildRedisCacheStats() {
        try {
            long searchResultCount = countRedisKeys(REDIS_KEY_PREFIX + "*");
            long chatResponseCount = countRedisKeys(REDIS_RESPONSE_PREFIX + "*");
            long hydeCount = countRedisKeys(REDIS_HYDE_PREFIX + "*");
            long rewriteCount = countRedisKeys(REDIS_REWRITE_PREFIX + "*");

            long redisHits = redisHitCount.get();
            long redisMisses = redisMissCount.get();
            long chatHits = chatResponseHitCount.get();
            long chatMisses = chatResponseMissCount.get();

            return CacheStatsDTO.RedisCacheStats.builder()
                    .connected(true)
                    .searchResultKeyCount(searchResultCount)
                    .chatResponseKeyCount(chatResponseCount)
                    .hydeKeyCount(hydeCount)
                    .rewriteKeyCount(rewriteCount)
                    .totalKeyCount(searchResultCount + chatResponseCount + hydeCount + rewriteCount)
                    .memoryUsage(null) // Redis内存使用需要额外配置
                    .build();
        } catch (Exception e) {
            log.warn("获取Redis统计失败: {}", e.getMessage());
            return CacheStatsDTO.RedisCacheStats.builder()
                    .connected(false)
                    .searchResultKeyCount(0L)
                    .chatResponseKeyCount(0L)
                    .hydeKeyCount(0L)
                    .totalKeyCount(0L)
                    .build();
        }
    }

    /**
     * 统计Redis中匹配模式的键数量
     */
    private long countRedisKeys(String pattern) {
        try {
            AtomicLong count = new AtomicLong(0);
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) (connection) -> {
                try (var cursor = connection.keyCommands().scan(ScanOptions.scanOptions().match(pattern).build())) {
                    while (cursor.hasNext()) {
                        count.incrementAndGet();
                        // 限制最大统计数量，避免性能问题
                        if (count.get() >= 10000) {
                            break;
                        }
                    }
                }
                return null;
            });
            return count.get();
        } catch (Exception e) {
            log.warn("统计Redis键数量失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 计算综合命中率
     */
    private double calculateOverallHitRate(CacheStats embeddingStats, CacheStats localSearchStats) {
        long totalHits = embeddingStats.hitCount() + localSearchStats.hitCount() + redisHitCount.get() + chatResponseHitCount.get();
        long totalMisses = embeddingStats.missCount() + localSearchStats.missCount() + redisMissCount.get() + chatResponseMissCount.get();
        long total = totalHits + totalMisses;
        return total > 0 ? (double) totalHits / total : 0.0;
    }

    /**
     * 根据命中率确定缓存状态
     */
    private String determineCacheStatus(double hitRate) {
        if (hitRate >= HIT_RATE_HEALTHY) {
            return "HEALTHY";
        } else if (hitRate >= HIT_RATE_WARNING) {
            return "WARNING";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * 重置统计计数器（用于定期重置或测试）
     */
    public void resetStats() {
        redisHitCount.set(0);
        redisMissCount.set(0);
        chatResponseHitCount.set(0);
        chatResponseMissCount.set(0);
        log.info("RAG缓存统计计数器已重置");
    }

    /**
     * 记录缓存统计到日志
     */
    public void logCacheStats() {
        CacheStatsDTO stats = getCacheStats();
        log.info("=== RAG缓存统计 ===");
        log.info("统计时间: {}", stats.getTimestamp());
        log.info("综合命中率: {:.2%}", stats.getOverallHitRate());

        CacheStatsDTO.CacheDetailStats embeddingStats = stats.getEmbeddingCacheStats();
        log.info("Embedding缓存 - 大小: {}, 命中率: {:.2%} ({}/{}), 状态: {}",
                embeddingStats.getSize(),
                embeddingStats.getHitRate(),
                embeddingStats.getHitCount(),
                embeddingStats.getTotalRequests(),
                embeddingStats.getStatus());

        CacheStatsDTO.CacheDetailStats localSearchStats = stats.getLocalSearchCacheStats();
        log.info("本地检索缓存 - 大小: {}, 命中率: {:.2%} ({}/{}), 状态: {}",
                localSearchStats.getSize(),
                localSearchStats.getHitRate(),
                localSearchStats.getHitCount(),
                localSearchStats.getTotalRequests(),
                localSearchStats.getStatus());

        CacheStatsDTO.RedisCacheStats redisStats = stats.getRedisCacheStats();
        log.info("Redis缓存 - 连接状态: {}, 检索结果键: {}, 聊天响应键: {}, HyDE键: {}, 查询改写键: {}",
                redisStats.getConnected() ? "正常" : "异常",
                redisStats.getSearchResultKeyCount(),
                redisStats.getChatResponseKeyCount(),
                redisStats.getHydeKeyCount(),
                redisStats.getRewriteKeyCount());
    }

    // ==================== 序列化/反序列化 ====================

    private String serializeSearchResult(CachedSearchResult result) throws JsonProcessingException {
        return objectMapper.writeValueAsString(result);
    }

    private CachedSearchResult deserializeSearchResult(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, CachedSearchResult.class);
    }

    // ==================== 缓存数据类 ====================

    public static class CachedSearchResult {
        private List<SerializableDocument> documents;
        private List<DocumentSourceDTO> sources;

        public CachedSearchResult() {
        }

        public CachedSearchResult(List<Document> documents, List<DocumentSourceDTO> sources) {
            this.documents = documents.stream()
                    .map(SerializableDocument::fromDocument)
                    .toList();
            this.sources = sources;
        }

        public List<Document> getDocuments() {
            if (documents == null) {
                return Collections.emptyList();
            }
            return documents.stream()
                    .map(SerializableDocument::toDocument)
                    .toList();
        }

        public void setDocuments(List<SerializableDocument> documents) {
            this.documents = documents;
        }

        public List<DocumentSourceDTO> getSources() {
            return sources != null ? sources : Collections.emptyList();
        }

        public void setSources(List<DocumentSourceDTO> sources) {
            this.sources = sources;
        }
    }

    /**
     * 可序列化的Document包装类
     */
    public static class SerializableDocument {
        private String id;
        private String text;
        private Map<String, Object> metadata;

        public SerializableDocument() {
        }

        public static SerializableDocument fromDocument(Document doc) {
            SerializableDocument sd = new SerializableDocument();
            sd.setId(doc.getId());
            sd.setText(doc.getText());
            sd.setMetadata(doc.getMetadata());
            return sd;
        }

        public Document toDocument() {
            Document doc = new Document(text != null ? text : "");
            doc.getMetadata().putAll(metadata != null ? metadata : Collections.emptyMap());
            return doc;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
