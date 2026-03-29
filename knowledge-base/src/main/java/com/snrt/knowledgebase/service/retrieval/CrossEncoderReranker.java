package com.snrt.knowledgebase.service.retrieval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

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
            return documents.stream().limit(topK).collect(Collectors.toList());
        }

        try {
            log.info("[重排序] 查询: {}, 待排序文档: {} 个", query, documents.size());

            // 1. 使用轻量级规则预过滤（可选）
            List<Document> candidates = preFilterByLength(documents, 10);

            // 2. 构建重排序 Prompt
            String rerankPrompt = buildRerankPrompt(query, candidates);

            // 3. 调用模型获取排序结果
            String response = chatModel.call(rerankPrompt);

            // 4. 解析排序结果
            List<Integer> rankIndices = parseRankResult(response, candidates.size());

            // 5. 根据排序结果重新组织文档
            List<Document> reranked = applyRanking(candidates, rankIndices);

            log.info("[重排序] 完成，返回前 {} 个结果", Math.min(topK, reranked.size()));
            return reranked.stream().limit(topK).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[重排序] 失败: {}, 使用原始排序", e.getMessage());
            // 降级：返回原始排序结果
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

        String queryLower = query.toLowerCase();
        List<String> queryTerms = Arrays.asList(queryLower.split("\\s+"));

        return documents.stream()
                .map(doc -> {
                    double score = calculateRelevanceScore(doc, queryTerms, queryLower);
                    doc.getMetadata().put("rule_score", score);
                    return doc;
                })
                .sorted((d1, d2) -> {
                    Double s1 = (Double) d1.getMetadata().get("rule_score");
                    Double s2 = (Double) d2.getMetadata().get("rule_score");
                    return Double.compare(s2, s1);
                })
                .limit(topK)
                .collect(Collectors.toList());
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
}
