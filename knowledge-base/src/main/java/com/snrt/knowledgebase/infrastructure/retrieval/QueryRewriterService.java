package com.snrt.knowledgebase.infrastructure.retrieval;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.snrt.knowledgebase.domain.chat.repository.ChatModelFactory;
import com.snrt.knowledgebase.domain.document.service.RAGCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 查询改写/扩展服务
 *
 * 功能：
 * 1. 查询理解与意图识别
 * 2. 同义词扩展
 * 3. 多语言查询翻译
 * 4. 模糊查询处理
 * 5. 查询拆分与重组
 *
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriterService {

    private final ChatModelFactory chatModelFactory;
    private final RAGCacheManager ragCacheManager;

    // L1: 本地缓存 - 改写查询缓存
    private Cache<String, RewrittenQuery> rewrittenQueryCache;

    // 常见同义词映射
    private final Map<String, List<String>> synonymMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // 初始化缓存
        rewrittenQueryCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats()
                .build();
        log.info("查询改写服务本地缓存初始化完成");

        // 初始化常见同义词
        initSynonyms();
    }

    /**
     * 初始化常见同义词
     */
    private void initSynonyms() {
        // 技术相关同义词
        synonymMap.put("人工智能", Arrays.asList("AI", "机器学习", "深度学习"));
        synonymMap.put("Java", Arrays.asList("java", "JVM", "Java语言"));
        synonymMap.put("Spring", Arrays.asList("spring", "Spring Boot", "Spring框架"));
        
        // 通用同义词
        synonymMap.put("如何", Arrays.asList("怎么", "怎样", "如何操作"));
        synonymMap.put("什么", Arrays.asList("什么是", "何谓", "什么意思"));
        synonymMap.put("问题", Arrays.asList("故障", "错误", "异常"));
        synonymMap.put("解决方案", Arrays.asList("解决方法", "处理方式", "应对策略"));
    }

    /**
     * 执行查询改写和扩展
     *
     * @param originalQuery 原始查询
     * @return 改写后的查询信息
     */
    public RewrittenQuery rewriteQuery(String originalQuery) {
        Instant start = Instant.now();
        log.info("[查询改写] 开始，原始查询: {}", originalQuery);

        // 1. 检查缓存
        RewrittenQuery cachedResult = rewrittenQueryCache.getIfPresent(originalQuery);
        if (cachedResult != null) {
            log.info("[查询改写] 命中本地缓存");
            return cachedResult;
        }

        // 2. 检查Redis缓存
        Optional<RewrittenQuery> redisResult = ragCacheManager.getCachedRewrittenQuery(originalQuery);
        if (redisResult.isPresent()) {
            log.info("[查询改写] 命中Redis缓存");
            // 回填本地缓存
            rewrittenQueryCache.put(originalQuery, redisResult.get());
            return redisResult.get();
        }

        // 3. 执行查询改写
        RewrittenQuery rewrittenQuery = new RewrittenQuery();
        rewrittenQuery.setOriginalQuery(originalQuery);

        // 3.1 基础处理
        String normalizedQuery = normalizeQuery(originalQuery);
        rewrittenQuery.setNormalizedQuery(normalizedQuery);

        // 3.2 同义词扩展
        List<String> expandedQueries = expandWithSynonyms(normalizedQuery);
        rewrittenQuery.setExpandedQueries(expandedQueries);

        // 3.3 使用LLM进行智能改写
        String llmRewrittenQuery = generateLLMRewrittenQuery(originalQuery);
        rewrittenQuery.setLlmRewrittenQuery(llmRewrittenQuery);

        // 3.4 构建最终增强查询
        String enhancedQuery = buildEnhancedQuery(originalQuery, llmRewrittenQuery, expandedQueries);
        rewrittenQuery.setEnhancedQuery(enhancedQuery);

        // 4. 缓存结果
        rewrittenQueryCache.put(originalQuery, rewrittenQuery);
        ragCacheManager.cacheRewrittenQuery(originalQuery, rewrittenQuery);

        Duration duration = Duration.between(start, Instant.now());
        log.info("[查询改写] 完成，耗时: {}ms", duration.toMillis());
        log.debug("[查询改写] 增强查询: {}", enhancedQuery);

        return rewrittenQuery;
    }

    /**
     * 标准化查询
     */
    private String normalizeQuery(String query) {
        // 去除多余空格
        String normalized = query.trim().replaceAll("\\s+|\\t+|\\n+", " ");
        // 转换为小写（可选，根据需要）
        // normalized = normalized.toLowerCase();
        return normalized;
    }

    /**
     * 使用同义词扩展查询
     */
    private List<String> expandWithSynonyms(String query) {
        List<String> expanded = new ArrayList<>();
        expanded.add(query); // 保留原始查询

        // 遍历同义词映射，替换查询中的关键词
        for (Map.Entry<String, List<String>> entry : synonymMap.entrySet()) {
            String keyword = entry.getKey();
            List<String> synonyms = entry.getValue();

            if (query.contains(keyword)) {
                for (String synonym : synonyms) {
                    String expandedQuery = query.replace(keyword, synonym);
                    if (!expanded.contains(expandedQuery)) {
                        expanded.add(expandedQuery);
                    }
                }
            }
        }

        return expanded;
    }

    /**
     * 使用LLM生成改写查询
     */
    private String generateLLMRewrittenQuery(String originalQuery) {
        String prompt = buildRewritePrompt(originalQuery);
        ChatModel chatModel = chatModelFactory.getDefaultModel();
        return chatModel.call(prompt);
    }

    /**
     * 构建查询改写的prompt
     */
    private String buildRewritePrompt(String originalQuery) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请帮我改写以下用户查询，使其更适合进行信息检索。\n");
        prompt.append("改写要求：\n");
        prompt.append("1. 保持原始查询的核心意图\n");
        prompt.append("2. 扩展相关概念和术语\n");
        prompt.append("3. 消除歧义，明确表达\n");
        prompt.append("4. 生成一个更全面、更具体的查询语句\n\n");
        prompt.append("原始查询：").append(originalQuery).append("\n\n");
        prompt.append("改写后的查询：");
        return prompt.toString();
    }

    /**
     * 构建最终增强查询
     */
    private String buildEnhancedQuery(String originalQuery, String llmRewrittenQuery, List<String> expandedQueries) {
        StringBuilder enhanced = new StringBuilder();
        enhanced.append("原始查询：").append(originalQuery).append("\n");
        enhanced.append("改写查询：").append(llmRewrittenQuery).append("\n");
        
        if (!expandedQueries.isEmpty() && expandedQueries.size() > 1) {
            enhanced.append("相关查询：").append(String.join(" | ", expandedQueries.subList(1, expandedQueries.size())));
        }
        
        return enhanced.toString();
    }



    /**
     * 查询改写结果类
     */
    public static class RewrittenQuery {
        private String originalQuery;
        private String normalizedQuery;
        private List<String> expandedQueries;
        private String llmRewrittenQuery;
        private String enhancedQuery;

        // Getters and Setters
        public String getOriginalQuery() {
            return originalQuery;
        }

        public void setOriginalQuery(String originalQuery) {
            this.originalQuery = originalQuery;
        }

        public String getNormalizedQuery() {
            return normalizedQuery;
        }

        public void setNormalizedQuery(String normalizedQuery) {
            this.normalizedQuery = normalizedQuery;
        }

        public List<String> getExpandedQueries() {
            return expandedQueries;
        }

        public void setExpandedQueries(List<String> expandedQueries) {
            this.expandedQueries = expandedQueries;
        }

        public String getLlmRewrittenQuery() {
            return llmRewrittenQuery;
        }

        public void setLlmRewrittenQuery(String llmRewrittenQuery) {
            this.llmRewrittenQuery = llmRewrittenQuery;
        }

        public String getEnhancedQuery() {
            return enhancedQuery;
        }

        public void setEnhancedQuery(String enhancedQuery) {
            this.enhancedQuery = enhancedQuery;
        }
    }
}