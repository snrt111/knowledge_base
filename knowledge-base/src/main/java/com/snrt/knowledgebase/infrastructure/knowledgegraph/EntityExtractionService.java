package com.snrt.knowledgebase.infrastructure.knowledgegraph;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.snrt.knowledgebase.domain.chat.repository.ChatModelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityExtractionService {

    private final ChatModelFactory chatModelFactory;

    private Cache<String, List<String>> entityCache;

    private static final String[] FALLBACK_ENTITIES = {
        "人工智能", "机器学习", "深度学习", "神经网络", "自然语言处理",
        "计算机视觉", "语音识别", "知识图谱", "大语言模型", "LLM",
        "Transformer", "BERT", "GPT", "ChatGPT", "AI",
        "Java", "Python", "Spring", "微服务", "分布式",
        "数据库", "缓存", "消息队列", "容器", "Kubernetes"
    };

    private static final Pattern ENTITY_PATTERN = Pattern.compile(
        "([\\u4e00-\\u9fa5a-zA-Z0-9_\\-]+(?:技术|系统|框架|平台|服务|模型|算法|架构|组件|模块|工具|语言|协议|标准|方法|理论|概念|原理|应用|场景|方案|策略|模式|设计|开发|测试|部署|运维|安全|性能|优化|扩展|集成|接口|API|SDK|库|包|模块|组件|服务|应用|系统|平台|框架|工具|语言|协议|标准|方法|理论|概念|原理|应用|场景|方案|策略|模式|设计|开发|测试|部署|运维|安全|性能|优化|扩展|集成|接口))"
    );

    @PostConstruct
    public void init() {
        entityCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build();
        log.info("[实体提取服务] 初始化完成");
    }

    public List<String> extractEntities(String content) {
        if (content == null || content.isBlank()) {
            log.warn("[实体提取服务] 内容为空，返回空实体列表");
            return new ArrayList<>();
        }

        String cacheKey = generateCacheKey(content);
        List<String> cachedEntities = entityCache.getIfPresent(cacheKey);
        if (cachedEntities != null) {
            log.debug("[实体提取服务] 命中缓存，返回 {} 个实体", cachedEntities.size());
            return cachedEntities;
        }

        List<String> entities = extractEntitiesWithLLM(content);
        
        if (entities.isEmpty()) {
            log.info("[实体提取服务] LLM未提取到实体，使用规则提取");
            entities = extractEntitiesWithRules(content);
        }

        if (entities.isEmpty()) {
            log.info("[实体提取服务] 规则未提取到实体，使用兜底匹配");
            entities = extractEntitiesWithFallback(content);
        }

        entities = deduplicateAndLimit(entities, 20);

        entityCache.put(cacheKey, entities);
        
        log.info("[实体提取服务] 提取完成，共 {} 个实体: {}", entities.size(), entities);
        return entities;
    }

    private List<String> extractEntitiesWithLLM(String content) {
        Instant start = Instant.now();
        log.debug("[实体提取服务] 开始LLM实体提取，内容长度: {}", content.length());

        try {
            String prompt = buildExtractionPrompt(content);
            ChatModel chatModel = chatModelFactory.getLightModel();
            String response = chatModel.call(prompt);

            List<String> entities = parseLLMResponse(response);
            
            Duration duration = Duration.between(start, Instant.now());
            log.debug("[实体提取服务] LLM提取完成，耗时: {}ms，提取 {} 个实体", 
                    duration.toMillis(), entities.size());
            
            return entities;
        } catch (Exception e) {
            log.error("[实体提取服务] LLM提取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String buildExtractionPrompt(String content) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请从以下文本中提取关键实体（如技术名称、产品名称、概念术语等）。\n");
        prompt.append("要求：\n");
        prompt.append("1. 只提取文本中明确提到的实体\n");
        prompt.append("2. 每个实体一行，不要编号和标点\n");
        prompt.append("3. 实体应该是名词或名词短语\n");
        prompt.append("4. 最多提取10个最重要的实体\n");
        prompt.append("5. 如果没有明显实体，输出\"无\"\n\n");
        prompt.append("文本：\n");
        
        if (content.length() > 2000) {
            prompt.append(content.substring(0, 2000)).append("...");
        } else {
            prompt.append(content);
        }
        
        prompt.append("\n\n实体列表：");
        return prompt.toString();
    }

    private List<String> parseLLMResponse(String response) {
        List<String> entities = new ArrayList<>();
        
        if (response == null || response.isBlank() || response.contains("无")) {
            return entities;
        }

        String[] lines = response.split("\n");
        for (String line : lines) {
            String entity = line.trim()
                    .replaceAll("^[0-9]+[.、)）]\\s*", "")
                    .replaceAll("^[\\-•*]\\s*", "")
                    .replaceAll("[,，;；。.!！?？]$", "")
                    .trim();
            
            if (!entity.isEmpty() && entity.length() >= 2 && entity.length() <= 20) {
                entities.add(entity);
            }
        }

        return entities;
    }

    private List<String> extractEntitiesWithRules(String content) {
        List<String> entities = new ArrayList<>();
        
        Matcher matcher = ENTITY_PATTERN.matcher(content);
        while (matcher.find()) {
            String entity = matcher.group(1);
            if (entity.length() >= 2 && entity.length() <= 20) {
                entities.add(entity);
            }
        }

        return entities;
    }

    private List<String> extractEntitiesWithFallback(String content) {
        List<String> entities = new ArrayList<>();
        
        for (String term : FALLBACK_ENTITIES) {
            if (content.contains(term)) {
                entities.add(term);
            }
        }

        return entities;
    }

    private String generateCacheKey(String content) {
        int hashCode = content.hashCode();
        int length = content.length();
        return hashCode + "_" + length;
    }

    private List<String> deduplicateAndLimit(List<String> entities, int maxCount) {
        return entities.stream()
                .distinct()
                .limit(maxCount)
                .toList();
    }
}
