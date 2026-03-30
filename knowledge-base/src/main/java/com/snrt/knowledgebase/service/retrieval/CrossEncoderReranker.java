package com.snrt.knowledgebase.service.retrieval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cross-Encoder 重排序器
 *
 * 使用大模型对召回结果进行精排，提升检索质量
 * 适用于对前N个结果进行精确排序的场景
 *
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class CrossEncoderReranker {

    private final ChatModel chatModel;

    public CrossEncoderReranker(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 对检索结果进行重排序
     *
     * @param query 用户查询
     * @param documents 待排序的文档列表
     * @param topK 返回前K个结果
     * @return 重排序后的文档列表
     */
    public List<Document> rerank(String query, List<Document> documents, int topK) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        // 如果文档数量较少，直接返回
        if (documents.size() <= 3) {
            log.info("[重排序] 文档数量较少 ({}个)，直接返回", documents.size());
            return documents.stream().limit(topK).collect(Collectors.toList());
        }

        String traceId = java.util.UUID.randomUUID().toString().substring(0, 8);
        java.time.Instant start = java.time.Instant.now();
        log.info("[{}] [重排序] 开始，查询: '{}', 待排序文档: {} 个, topK: {}", 
                traceId, query, documents.size(), topK);

        try {
            // 1. 使用轻量级规则预过滤（可选）
            log.info("[{}] [重排序] 开始预过滤，输入文档数: {}", traceId, documents.size());
            java.time.Instant filterStart = java.time.Instant.now();
            List<Document> candidates = preFilterByLength(documents, 10);
            java.time.Duration filterDuration = java.time.Duration.between(filterStart, java.time.Instant.now());
            log.info("[{}] [重排序] 预过滤完成，耗时: {}ms, 过滤后文档数: {}", 
                    traceId, filterDuration.toMillis(), candidates.size());

            // 2. 构建重排序 Prompt
            log.info("[{}] [重排序] 构建重排序Prompt，使用{}个候选文档", traceId, candidates.size());
            java.time.Instant promptStart = java.time.Instant.now();
            String rerankPrompt = buildRerankPrompt(query, candidates);
            java.time.Duration promptDuration = java.time.Duration.between(promptStart, java.time.Instant.now());
            log.info("[{}] [重排序] Prompt构建完成，耗时: {}ms, Prompt长度: {}字符", 
                    traceId, promptDuration.toMillis(), rerankPrompt.length());

            // 3. 调用模型获取排序结果
            log.info("[{}] [重排序] 调用大模型进行重排序", traceId);
            java.time.Instant modelStart = java.time.Instant.now();
            String response = chatModel.call(rerankPrompt);
            java.time.Duration modelDuration = java.time.Duration.between(modelStart, java.time.Instant.now());
            log.info("[{}] [重排序] 模型调用完成，耗时: {}ms, 响应长度: {}字符", 
                    traceId, modelDuration.toMillis(), response.length());

            // 4. 解析排序结果
            log.info("[{}] [重排序] 解析排序结果", traceId);
            java.time.Instant parseStart = java.time.Instant.now();
            List<Integer> rankIndices = parseRankResult(response, candidates.size());
            java.time.Duration parseDuration = java.time.Duration.between(parseStart, java.time.Instant.now());
            log.info("[{}] [重排序] 解析完成，耗时: {}ms, 排序索引数: {}", 
                    traceId, parseDuration.toMillis(), rankIndices.size());

            // 5. 根据排序结果重新组织文档
            log.info("[{}] [重排序] 应用排序结果", traceId);
            java.time.Instant applyStart = java.time.Instant.now();
            List<Document> reranked = applyRanking(candidates, rankIndices);
            java.time.Duration applyDuration = java.time.Duration.between(applyStart, java.time.Instant.now());
            log.info("[{}] [重排序] 排序应用完成，耗时: {}ms, 排序后文档数: {}", 
                    traceId, applyDuration.toMillis(), reranked.size());

            // 记录排序结果详情
            log.debug("[{}] [重排序] 排序结果详情:", traceId);
            for (int i = 0; i < Math.min(reranked.size(), 5); i++) {
                Document doc = reranked.get(i);
                String docName = getDocumentName(doc);
                Double rerankScore = (Double) doc.getMetadata().get("rerank_score");
                log.debug("[{}] [重排序] 文档{}: {}, 重排序分数: {:.3f}", 
                        traceId, i+1, docName, rerankScore);
            }

            java.time.Duration totalDuration = java.time.Duration.between(start, java.time.Instant.now());
            log.info("[{}] [重排序] 完成，总耗时: {}ms, 返回前 {} 个结果", 
                    traceId, totalDuration.toMillis(), Math.min(topK, reranked.size()));
            return reranked.stream().limit(topK).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[{}] [重排序] 失败: {}, 使用原始排序", traceId, e.getMessage(), e);
            // 降级：返回原始排序结果
            log.info("[{}] [重排序] 降级处理：使用原始排序，返回前{}个结果", traceId, topK);
            return documents.stream().limit(topK).collect(Collectors.toList());
        }
    }

    /**
     * 构建重排序 Prompt
     */
    private String buildRerankPrompt(String query, List<Document> documents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据用户查询，对以下文档进行相关性排序。\n\n");
        prompt.append("用户查询：").append(query).append("\n\n");
        prompt.append("文档列表（按编号）：\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String content = doc.getText();
            // 截断过长的内容
            if (content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            prompt.append(String.format("[%d] %s\n\n", i, content));
        }

        prompt.append("请输出文档编号的相关性排序（最相关的在前），格式如下：\n");
        prompt.append("排序结果：3,1,4,2,0\n");
        prompt.append("只需输出排序结果一行，不要其他解释。");

        return prompt.toString();
    }

    /**
     * 解析重排序结果
     */
    private List<Integer> parseRankResult(String response, int maxIndex) {
        List<Integer> indices = new ArrayList<>();

        try {
            // 提取数字
            String[] parts = response.replaceAll("[^0-9,]", "").split(",");

            for (String part : parts) {
                if (part.trim().isEmpty()) continue;
                int index = Integer.parseInt(part.trim());
                if (index >= 0 && index < maxIndex && !indices.contains(index)) {
                    indices.add(index);
                }
            }

            // 补充未包含的索引
            for (int i = 0; i < maxIndex; i++) {
                if (!indices.contains(i)) {
                    indices.add(i);
                }
            }

        } catch (Exception e) {
            log.warn("[重排序] 解析结果失败: {}, 使用原始顺序", e.getMessage());
            // 返回原始顺序
            for (int i = 0; i < maxIndex; i++) {
                indices.add(i);
            }
        }

        return indices;
    }

    /**
     * 应用排序结果
     */
    private List<Document> applyRanking(List<Document> documents, List<Integer> rankIndices) {
        List<Document> result = new ArrayList<>();
        for (int index : rankIndices) {
            if (index < documents.size()) {
                Document doc = documents.get(index);
                // 添加重排序分数
                int rank = result.size() + 1;
                doc.getMetadata().put("rerank_score", 1.0 / rank);
                result.add(doc);
            }
        }
        return result;
    }

    /**
     * 按长度预过滤（移除过长或过短的文档）
     */
    private List<Document> preFilterByLength(List<Document> documents, int maxCount) {
        return documents.stream()
                .filter(doc -> {
                    String text = doc.getText();
                    // 过滤过短（少于10字符）或过长（超过5000字符）的文档
                    return text != null && text.length() >= 10 && text.length() <= 5000;
                })
                .limit(maxCount)
                .collect(Collectors.toList());
    }

    /**
     * 轻量级重排序（基于规则，无需调用模型）
     * 适用于对性能要求高的场景
     */
    public List<Document> rerankByRules(String query, List<Document> documents, int topK) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        String traceId = java.util.UUID.randomUUID().toString().substring(0, 8);
        java.time.Instant start = java.time.Instant.now();
        log.info("[{}] [规则重排序] 开始，查询: '{}', 待排序文档: {} 个, topK: {}", 
                traceId, query, documents.size(), topK);

        String queryLower = query.toLowerCase();
        List<String> queryTerms = Arrays.asList(queryLower.split("\\s+"));
        log.info("[{}] [规则重排序] 提取查询关键词: {}", traceId, String.join(", ", queryTerms));

        java.time.Instant scoreStart = java.time.Instant.now();
        List<Document> scoredDocs = documents.stream()
                .map(doc -> {
                    double score = calculateRelevanceScore(doc, queryTerms, queryLower);
                    doc.getMetadata().put("rule_score", score);
                    return doc;
                })
                .collect(Collectors.toList());
        java.time.Duration scoreDuration = java.time.Duration.between(scoreStart, java.time.Instant.now());

        java.time.Instant sortStart = java.time.Instant.now();
        List<Document> rerankedDocs = scoredDocs.stream()
                .sorted((d1, d2) -> {
                    Double s1 = (Double) d1.getMetadata().get("rule_score");
                    Double s2 = (Double) d2.getMetadata().get("rule_score");
                    return Double.compare(s2, s1);
                })
                .limit(topK)
                .collect(Collectors.toList());
        java.time.Duration sortDuration = java.time.Duration.between(sortStart, java.time.Instant.now());

        // 记录排序结果详情
        log.debug("[{}] [规则重排序] 排序结果详情:", traceId);
        for (int i = 0; i < Math.min(rerankedDocs.size(), 5); i++) {
            Document doc = rerankedDocs.get(i);
            String docName = getDocumentName(doc);
            Double ruleScore = (Double) doc.getMetadata().get("rule_score");
            log.debug("[{}] [规则重排序] 文档{}: {}, 规则分数: {:.3f}", 
                    traceId, i+1, docName, ruleScore);
        }

        java.time.Duration totalDuration = java.time.Duration.between(start, java.time.Instant.now());
        log.info("[{}] [规则重排序] 完成，总耗时: {}ms, 评分耗时: {}ms, 排序耗时: {}ms, 返回前 {} 个结果", 
                traceId, totalDuration.toMillis(), scoreDuration.toMillis(), sortDuration.toMillis(), rerankedDocs.size());

        return rerankedDocs;
    }

    /**
     * 计算相关性分数（基于关键词匹配）
     */
    private double calculateRelevanceScore(Document doc, List<String> queryTerms, String queryLower) {
        String text = doc.getText().toLowerCase();
        double score = 0.0;

        // 1. 完整查询匹配
        if (text.contains(queryLower)) {
            score += 10.0;
        }

        // 2. 关键词匹配
        for (String term : queryTerms) {
            if (term.length() < 2) continue; // 跳过单字符

            int count = countOccurrences(text, term);
            score += count * 2.0;

            // 标题匹配（如果有标题元数据）
            Object title = doc.getMetadata().get("document_name");
            if (title != null && title.toString().toLowerCase().contains(term)) {
                score += 5.0;
            }
        }

        // 3. 向量检索分数（如果有）
        Object vectorScore = doc.getMetadata().get("rrf_score");
        if (vectorScore instanceof Number) {
            score += ((Number) vectorScore).doubleValue() * 5.0;
        }

        // 4. 长度惩罚（过长的文档适当降权）
        int length = text.length();
        if (length > 1000) {
            score *= 0.9;
        }

        return score;
    }

    /**
     * 统计子串出现次数
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /**
     * 获取文档名称
     */
    private String getDocumentName(Document doc) {
        Object docName = doc.getMetadata().get("document_name");
        return docName != null ? docName.toString() : "未知文档";
    }
}
