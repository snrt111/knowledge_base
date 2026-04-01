package com.snrt.knowledgebase.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 
 * 配置两级缓存架构：
 * - L1本地缓存（Caffeine）：Embedding结果、检索结果、聊天响应
 * - L2分布式缓存（Redis）：检索结果、聊天响应
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_EMBEDDING = "embeddingCache";
    public static final String CACHE_SEARCH_RESULT = "searchResultCache";
    public static final String CACHE_CHAT_RESPONSE = "chatResponseCache";

    /**
     * 创建Caffeine本地缓存管理器
     * 
     * @return CaffeineCacheManager实例
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats());
        cacheManager.setCacheNames(java.util.Arrays.asList(
                CACHE_EMBEDDING,
                CACHE_SEARCH_RESULT,
                CACHE_CHAT_RESPONSE
        ));
        log.info("Caffeine本地缓存管理器初始化完成");
        return cacheManager;
    }

    /**
     * 创建Redis分布式缓存管理器
     * 
     * @param connectionFactory Redis连接工厂
     * @return RedisCacheManager实例
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration(CACHE_SEARCH_RESULT, config.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CACHE_CHAT_RESPONSE, config.entryTtl(Duration.ofMinutes(5)))
                .transactionAware()
                .build();

        log.info("Redis分布式缓存管理器初始化完成");
        return cacheManager;
    }
}
