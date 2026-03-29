package com.snrt.knowledgebase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.snrt.knowledgebase.dto.DocumentSourceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private static final String REDIS_KEY_PREFIX = "rag:search:";
    private static final String REDIS_RESPONSE_PREFIX = "rag:response:";
    private static final Duration REDIS_TTL = Duration.ofMinutes(10);
    private static final Duration LOCAL_TTL = Duration.ofMinutes(5);

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
                log.debug("检索结果Redis缓存命中: key={}", cacheKey);
                return Optional.of(result);
            }
        } catch (Exception e) {
            log.warn("Redis缓存读取失败: {}", e.getMessage());
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
                log.debug("聊天响应缓存命中: key={}", cacheKey);
                return Optional.of(response);
            }
        } catch (Exception e) {
            log.warn("聊天响应缓存读取失败: {}", e.getMessage());
        }

        return Optional.empty();
    }

    // ==================== 缓存统计 ====================

    public void logCacheStats() {
        log.info("=== RAG缓存统计 ===");
        log.info("Embedding缓存 - 估计大小: {}, 命中率: {}",
                embeddingCache.estimatedSize(),
                embeddingCache.stats().hitRate());
        log.info("本地检索缓存 - 估计大小: {}, 命中率: {}",
                localSearchCache.estimatedSize(),
                localSearchCache.stats().hitRate());
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
