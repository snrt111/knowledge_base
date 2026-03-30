package com.snrt.knowledgebase.service.retrieval;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.snrt.knowledgebase.model.ChatModelFactory;
import com.snrt.knowledgebase.service.RAGCacheManager;
import com.snrt.knowledgebase.service.retrieval.CrossEncoderReranker;
import com.snrt.knowledgebase.service.retrieval.MultiRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * HyDE (假设答案检索) 服务
 *
 * 实现流程：
 * 1. 接收用户查询
 * 2. 使用LLM生成假设答案
 * 3. 将原始查询和假设答案组合成增强查询
 * 4. 执行检索
 *
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HydeService {

    private final ChatModelFactory chatModelFactory;
    private final RAGCacheManager ragCacheManager;
    private final MultiRetriever multiRetriever;
    private final CrossEncoderReranker reranker;

    // L1: 本地缓存 - 假设答案缓存
    private Cache<String, String> hypotheticalAnswerCache;

    @PostConstruct
    public void init() {
        hypotheticalAnswerCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats()
                .build();
        log.info("HyDE本地缓存初始化完成");
    }

    /**
     * 生成假设答案
     *
     * @param query 用户查询
     * @return 假设答案
     */
    public String generateHypotheticalAnswer(String query) {
        Instant start = Instant.now();
        log.info("[HyDE] 开始生成假设答案，查询: {}", query);

        // 1. 检查本地缓存（L1）
        String localCachedAnswer = hypotheticalAnswerCache.getIfPresent(query);
        if (localCachedAnswer != null) {
            log.info("[HyDE] 命中本地缓存，直接返回假设答案");
            return localCachedAnswer;
        }

        // 2. 检查Redis缓存（L2）
        Optional<String> cachedAnswer = ragCacheManager.getCachedHypotheticalAnswer(query);
        if (cachedAnswer.isPresent()) {
            log.info("[HyDE] 命中Redis缓存，直接返回假设答案");
            // 回填本地缓存
            hypotheticalAnswerCache.put(query, cachedAnswer.get());
            return cachedAnswer.get();
        }

        // 3. 使用LLM生成假设答案
        String prompt = buildHydePrompt(query);
        ChatModel chatModel = chatModelFactory.getDefaultModel();
        String hypotheticalAnswer = chatModel.call(prompt);

        // 4. 缓存结果（L1 + L2）
        hypotheticalAnswerCache.put(query, hypotheticalAnswer);
        ragCacheManager.cacheHypotheticalAnswer(query, hypotheticalAnswer);

        Duration duration = Duration.between(start, Instant.now());
        log.info("[HyDE] 假设答案生成完成，耗时: {}ms，答案长度: {} 字符", 
                duration.toMillis(), hypotheticalAnswer.length());

        return hypotheticalAnswer;
    }

    /**
     * 执行HyDE检索
     *
     * @param query 用户查询
     * @param knowledgeBaseId 知识库ID
     * @param topK 返回结果数量
     * @return 检索结果
     */
    public List<Document> retrieveWithHyde(String query, String knowledgeBaseId, int topK) {
        Instant start = Instant.now();
        log.info("[HyDE] 开始执行HyDE检索，查询: {}, 知识库: {}, topK: {}", 
                query, knowledgeBaseId, topK);

        // 1. 生成假设答案
        String hypotheticalAnswer = generateHypotheticalAnswer(query);

        // 2. 构建增强查询（原始查询 + 假设答案）
        String enhancedQuery = buildEnhancedQuery(query, hypotheticalAnswer);
        log.debug("[HyDE] 增强查询: {}", enhancedQuery);

        // 3. 使用增强查询执行检索（复用AdvancedRetrievalService的逻辑）
        int recallCount = topK * 3; // 召回3倍数量的候选
        List<Document> recalledDocs = multiRetriever.retrieve(enhancedQuery, knowledgeBaseId, recallCount);

        if (recalledDocs.isEmpty()) {
            log.warn("[HyDE] 未召回任何文档");
            return List.of();
        }

        log.info("[HyDE] 多路召回: {} 个文档", recalledDocs.size());

        // 重排序（精排）
        List<Document> rerankedDocs = reranker.rerankByRules(enhancedQuery, recalledDocs, topK);

        log.info("[HyDE] 重排序完成，返回前 {} 个结果", rerankedDocs.size());

        Duration duration = Duration.between(start, Instant.now());
        log.info("[HyDE] 检索完成，返回 {} 个结果，耗时: {}ms", rerankedDocs.size(), duration.toMillis());

        return rerankedDocs;
    }

    /**
     * 构建生成假设答案的prompt
     */
    private String buildHydePrompt(String query) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下问题生成一个详细的假设答案。");
        prompt.append("这个答案不需要基于真实知识，只是作为检索的辅助。");
        prompt.append("答案应该详细、具体，包含可能的相关信息。\n\n");
        prompt.append("问题：").append(query).append("\n\n");
        prompt.append("假设答案：");
        return prompt.toString();
    }

    /**
     * 构建增强查询
     */
    private String buildEnhancedQuery(String originalQuery, String hypotheticalAnswer) {
        StringBuilder enhancedQuery = new StringBuilder();
        enhancedQuery.append("原始问题：").append(originalQuery).append("\n");
        enhancedQuery.append("假设答案：").append(hypotheticalAnswer);
        return enhancedQuery.toString();
    }

    /**
     * 检查是否应该使用HyDE
     *
     * @param query 用户查询
     * @return 是否使用HyDE
     */
    public boolean shouldUseHyde(String query) {
        // 基于查询长度和复杂度决定是否使用HyDE
        // 太短的查询可能不需要HyDE
        int queryLength = query.length();
        return queryLength > 10; // 长度大于10的查询使用HyDE
    }
}
