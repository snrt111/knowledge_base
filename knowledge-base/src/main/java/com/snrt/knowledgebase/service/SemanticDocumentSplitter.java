package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 智能语义文档分块器
 * 
 * 核心特性：
 * 1. 语义边界识别 - 按标题、段落、句子等语义单元分割
 * 2. 智能合并策略 - 动态调整块大小，保持语义完整性
 * 3. 重叠窗口 - 相邻块保留重叠内容，避免上下文断裂
 * 4. 元数据增强 - 记录分块位置、层级等辅助信息
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Component
public class SemanticDocumentSplitter {

    /**
     * 分块配置参数
     */
    public static class SplitConfig {
        // 目标块大小（字符数）
        private int targetChunkSize = 800;
        // 最小块大小
        private int minChunkSize = 300;
        // 最大块大小
        private int maxChunkSize = 1500;
        // 重叠大小
        private int overlapSize = 100;
        // 是否保留标题层级
        private boolean preserveHierarchy = true;
        // 是否启用语义边界检测
        private boolean enableSemanticBoundary = true;

        public static SplitConfig defaultConfig() {
            return new SplitConfig();
        }

        public static SplitConfig forLargeDocument() {
            SplitConfig config = new SplitConfig();
            config.targetChunkSize = 1200;
            config.maxChunkSize = 2000;
            config.overlapSize = 150;
            return config;
        }

        public static SplitConfig forPreciseRetrieval() {
            SplitConfig config = new SplitConfig();
            config.targetChunkSize = 500;
            config.maxChunkSize = 800;
            config.overlapSize = 80;
            return config;
        }

        // Getters and Setters
        public int getTargetChunkSize() { return targetChunkSize; }
        public void setTargetChunkSize(int targetChunkSize) { this.targetChunkSize = targetChunkSize; }
        public int getMinChunkSize() { return minChunkSize; }
        public void setMinChunkSize(int minChunkSize) { this.minChunkSize = minChunkSize; }
        public int getMaxChunkSize() { return maxChunkSize; }
        public void setMaxChunkSize(int maxChunkSize) { this.maxChunkSize = maxChunkSize; }
        public int getOverlapSize() { return overlapSize; }
        public void setOverlapSize(int overlapSize) { this.overlapSize = overlapSize; }
        public boolean isPreserveHierarchy() { return preserveHierarchy; }
        public void setPreserveHierarchy(boolean preserveHierarchy) { this.preserveHierarchy = preserveHierarchy; }
        public boolean isEnableSemanticBoundary() { return enableSemanticBoundary; }
        public void setEnableSemanticBoundary(boolean enableSemanticBoundary) { this.enableSemanticBoundary = enableSemanticBoundary; }
    }

    /**
     * 语义单元类型
     */
    public enum SemanticUnit {
        HEADING_1,      // 一级标题
        HEADING_2,      // 二级标题
        HEADING_3,      // 三级标题
        PARAGRAPH,      // 段落
        LIST_ITEM,      // 列表项
        CODE_BLOCK,     // 代码块
        TABLE,          // 表格
        SENTENCE        // 句子
    }

    /**
     * 语义片段
     */
    public static class SemanticSegment {
        private final String content;
        private final SemanticUnit unitType;
        private final String headingContext;  // 所属标题上下文
        private final int level;              // 层级深度
        private final int position;           // 在文档中的位置

        public SemanticSegment(String content, SemanticUnit unitType, 
                               String headingContext, int level, int position) {
            this.content = content;
            this.unitType = unitType;
            this.headingContext = headingContext;
            this.level = level;
            this.position = position;
        }

        public String getContent() { return content; }
        public SemanticUnit getUnitType() { return unitType; }
        public String getHeadingContext() { return headingContext; }
        public int getLevel() { return level; }
        public int getPosition() { return position; }
        public int getLength() { return content.length(); }

        @Override
        public String toString() {
            return String.format("Segment[%s, len=%d, level=%d]: %s...", 
                unitType, content.length(), level, 
                content.substring(0, Math.min(50, content.length())));
        }
    }

    // 正则表达式模式
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6}\\s+.+)$", Pattern.MULTILINE);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern TABLE_PATTERN = Pattern.compile("(\\|[^\\n]+\\|\\n\\|[-:|\\s]+\\|\\n(?:\\|[^\\n]+\\|\\n?)+)");
    private static final Pattern LIST_PATTERN = Pattern.compile("^([\\s]*[-*+\\d\\.]\\s+.+)$", Pattern.MULTILINE);
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?。！？]+[.!?。！？]+");

    /**
     * 主入口：执行智能语义分块
     * 
     * @param documents 原始文档列表
     * @param config 分块配置
     * @return 分块后的文档列表
     */
    public List<Document> split(List<Document> documents, SplitConfig config) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> result = new ArrayList<>();
        int globalChunkIndex = 0;

        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.trim().isEmpty()) {
                continue;
            }

            log.debug("开始分块文档，原始长度: {}", content.length());

            // 步骤1：识别语义边界
            List<SemanticSegment> segments = extractSemanticSegments(content);
            log.debug("识别到 {} 个语义单元", segments.size());

            // 步骤2：智能合并
            List<List<SemanticSegment>> chunks = mergeSegments(segments, config);
            log.debug("合并为 {} 个块", chunks.size());

            // 步骤3：添加重叠并生成最终文档
            List<Document> docChunks = createChunksWithOverlap(chunks, config, doc.getMetadata(), globalChunkIndex);
            
            result.addAll(docChunks);
            globalChunkIndex += docChunks.size();

            log.info("文档分块完成：原始 {} 字符，生成 {} 个块", 
                content.length(), docChunks.size());
        }

        return result;
    }

    /**
     * 便捷方法：使用默认配置
     */
    public List<Document> split(List<Document> documents) {
        return split(documents, SplitConfig.defaultConfig());
    }

    /**
     * 提取语义片段
     */
    private List<SemanticSegment> extractSemanticSegments(String content) {
        List<SemanticSegment> segments = new ArrayList<>();
        
        // 检测文档类型
        boolean isMarkdown = content.contains("#") || content.contains("```");
        
        if (isMarkdown) {
            segments = extractMarkdownSegments(content);
        } else {
            segments = extractPlainTextSegments(content);
        }

        // 按位置排序
        segments.sort(Comparator.comparingInt(SemanticSegment::getPosition));
        
        return segments;
    }

    /**
     * 提取Markdown语义片段
     */
    private List<SemanticSegment> extractMarkdownSegments(String content) {
        List<SemanticSegment> segments = new ArrayList<>();
        StringBuilder currentHeading = new StringBuilder();
        int currentLevel = 0;
        int position = 0;

        String[] lines = content.split("\n");
        StringBuilder paragraph = new StringBuilder();
        int paragraphStart = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // 检测代码块
            if (trimmed.startsWith("```")) {
                // 先保存当前段落
                if (paragraph.length() > 0) {
                    segments.add(createSegment(paragraph.toString(), SemanticUnit.PARAGRAPH, 
                        currentHeading.toString(), currentLevel, paragraphStart));
                    paragraph.setLength(0);
                }

                // 提取完整代码块
                StringBuilder codeBlock = new StringBuilder();
                codeBlock.append(line).append("\n");
                int codeStart = position;
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("```")) {
                    codeBlock.append(lines[i]).append("\n");
                    i++;
                }
                if (i < lines.length) {
                    codeBlock.append(lines[i]);
                }
                
                segments.add(createSegment(codeBlock.toString(), SemanticUnit.CODE_BLOCK,
                    currentHeading.toString(), currentLevel, codeStart));
                position += codeBlock.length();
                continue;
            }

            // 检测标题
            Matcher headingMatcher = Pattern.compile("^(#{1,6})\\s+(.+)$").matcher(trimmed);
            if (headingMatcher.matches()) {
                // 保存当前段落
                if (paragraph.length() > 0) {
                    segments.add(createSegment(paragraph.toString(), SemanticUnit.PARAGRAPH,
                        currentHeading.toString(), currentLevel, paragraphStart));
                    paragraph.setLength(0);
                }

                int level = headingMatcher.group(1).length();
                String title = headingMatcher.group(2);
                
                // 更新当前标题上下文
                currentHeading.append(" / ").append(title);
                currentLevel = level;
                
                SemanticUnit unitType = level == 1 ? SemanticUnit.HEADING_1 :
                                       level == 2 ? SemanticUnit.HEADING_2 :
                                       SemanticUnit.HEADING_3;
                
                segments.add(createSegment(line, unitType, 
                    currentHeading.toString(), level, position));
                position += line.length() + 1;
                paragraphStart = position;
                continue;
            }

            // 检测列表项
            if (trimmed.matches("^[-*+\\d\\.]\\s+.+")) {
                if (paragraph.length() > 0 && !paragraph.toString().trim().matches("^[-*+\\d\\.]\\s+.+")) {
                    segments.add(createSegment(paragraph.toString(), SemanticUnit.PARAGRAPH,
                        currentHeading.toString(), currentLevel, paragraphStart));
                    paragraph.setLength(0);
                    paragraphStart = position;
                }
            }

            // 累积段落内容
            if (paragraph.length() == 0) {
                paragraphStart = position;
            }
            paragraph.append(line).append("\n");
            position += line.length() + 1;
        }

        // 处理最后一个段落
        if (paragraph.length() > 0) {
            segments.add(createSegment(paragraph.toString().trim(), SemanticUnit.PARAGRAPH,
                currentHeading.toString(), currentLevel, paragraphStart));
        }

        return segments;
    }

    /**
     * 提取纯文本语义片段
     */
    private List<SemanticSegment> extractPlainTextSegments(String content) {
        List<SemanticSegment> segments = new ArrayList<>();
        
        // 按段落分割
        String[] paragraphs = content.split("\n\\s*\n");
        int position = 0;
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // 如果段落太长，按句子分割
            if (paragraph.length() > 500) {
                List<String> sentences = splitIntoSentences(paragraph);
                for (String sentence : sentences) {
                    segments.add(createSegment(sentence, SemanticUnit.SENTENCE, "", 0, position));
                    position += sentence.length();
                }
            } else {
                segments.add(createSegment(paragraph, SemanticUnit.PARAGRAPH, "", 0, position));
                position += paragraph.length();
            }
        }

        return segments;
    }

    /**
     * 将文本分割成句子
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        
        // 如果没有匹配到句子，返回整个文本
        if (sentences.isEmpty()) {
            sentences.add(text);
        }
        
        return sentences;
    }

    /**
     * 创建语义片段
     */
    private SemanticSegment createSegment(String content, SemanticUnit unitType, 
                                          String headingContext, int level, int position) {
        // 清理标题上下文
        String cleanHeading = headingContext.startsWith(" / ") ? 
            headingContext.substring(3) : headingContext;
        
        return new SemanticSegment(content.trim(), unitType, cleanHeading, level, position);
    }

    /**
     * 智能合并语义片段
     */
    private List<List<SemanticSegment>> mergeSegments(List<SemanticSegment> segments, SplitConfig config) {
        List<List<SemanticSegment>> chunks = new ArrayList<>();
        List<SemanticSegment> currentChunk = new ArrayList<>();
        int currentSize = 0;

        for (int i = 0; i < segments.size(); i++) {
            SemanticSegment segment = segments.get(i);
            int segmentSize = segment.getLength();

            // 特殊情况：单个片段就超过最大大小
            if (segmentSize > config.getMaxChunkSize()) {
                // 先保存当前块
                if (!currentChunk.isEmpty()) {
                    chunks.add(new ArrayList<>(currentChunk));
                    currentChunk.clear();
                    currentSize = 0;
                }
                
                // 对大片段进行强制分割
                List<SemanticSegment> subSegments = forceSplitSegment(segment, config.getMaxChunkSize());
                for (SemanticSegment sub : subSegments) {
                    List<SemanticSegment> singleChunk = Collections.singletonList(sub);
                    chunks.add(singleChunk);
                }
                continue;
            }

            // 检查是否需要开启新块
            boolean shouldStartNewChunk = false;

            // 条件1：当前块加上新片段超过最大大小
            if (currentSize + segmentSize > config.getMaxChunkSize()) {
                shouldStartNewChunk = true;
            }
            
            // 条件2：遇到高级别标题（H1/H2）且当前块已达到最小大小
            if ((segment.getUnitType() == SemanticUnit.HEADING_1 || 
                 segment.getUnitType() == SemanticUnit.HEADING_2) && 
                currentSize >= config.getMinChunkSize()) {
                shouldStartNewChunk = true;
            }

            // 条件3：当前块已达到目标大小，且新片段是完整单元
            if (currentSize >= config.getTargetChunkSize() && 
                currentSize >= config.getMinChunkSize() &&
                (segment.getUnitType() == SemanticUnit.PARAGRAPH ||
                 segment.getUnitType() == SemanticUnit.CODE_BLOCK)) {
                shouldStartNewChunk = true;
            }

            if (shouldStartNewChunk && !currentChunk.isEmpty()) {
                chunks.add(new ArrayList<>(currentChunk));
                currentChunk.clear();
                currentSize = 0;
            }

            currentChunk.add(segment);
            currentSize += segmentSize;
        }

        // 处理最后一个块
        if (!currentChunk.isEmpty()) {
            // 如果最后一个块太小，尝试与前一个合并
            if (currentSize < config.getMinChunkSize() && chunks.size() > 0) {
                List<SemanticSegment> lastChunk = chunks.get(chunks.size() - 1);
                int lastChunkSize = lastChunk.stream().mapToInt(SemanticSegment::getLength).sum();
                
                if (lastChunkSize + currentSize <= config.getMaxChunkSize()) {
                    lastChunk.addAll(currentChunk);
                } else {
                    chunks.add(currentChunk);
                }
            } else {
                chunks.add(currentChunk);
            }
        }

        return chunks;
    }

    /**
     * 强制分割超大片段
     */
    private List<SemanticSegment> forceSplitSegment(SemanticSegment segment, int maxSize) {
        List<SemanticSegment> result = new ArrayList<>();
        String content = segment.getContent();
        
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + maxSize, content.length());
            
            // 尝试在句子边界分割
            if (end < content.length()) {
                int sentenceEnd = findLastSentenceEnd(content, start, end);
                if (sentenceEnd > start) {
                    end = sentenceEnd;
                }
            }
            
            String subContent = content.substring(start, end);
            result.add(new SemanticSegment(subContent, segment.getUnitType(),
                segment.getHeadingContext(), segment.getLevel(), segment.getPosition() + start));
            
            start = end;
        }
        
        return result;
    }

    /**
     * 查找最后一个句子结束位置
     */
    private int findLastSentenceEnd(String text, int start, int end) {
        String substring = text.substring(start, end);
        Matcher matcher = SENTENCE_PATTERN.matcher(substring);
        
        int lastEnd = -1;
        while (matcher.find()) {
            lastEnd = start + matcher.end();
        }
        
        return lastEnd;
    }

    /**
     * 创建带重叠的最终文档块
     */
    private List<Document> createChunksWithOverlap(List<List<SemanticSegment>> chunks, 
                                                    SplitConfig config,
                                                    Map<String, Object> originalMetadata,
                                                    int startIndex) {
        List<Document> result = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            List<SemanticSegment> chunk = chunks.get(i);
            
            // 合并片段内容
            StringBuilder content = new StringBuilder();
            StringBuilder headingContext = new StringBuilder();
            Set<String> unitTypes = new HashSet<>();
            int minLevel = Integer.MAX_VALUE;
            
            for (SemanticSegment segment : chunk) {
                // 添加标题上下文
                if (!segment.getHeadingContext().isEmpty() && 
                    headingContext.indexOf(segment.getHeadingContext()) < 0) {
                    if (headingContext.length() > 0) {
                        headingContext.append(" > ");
                    }
                    headingContext.append(segment.getHeadingContext());
                }
                
                content.append(segment.getContent()).append("\n\n");
                unitTypes.add(segment.getUnitType().name());
                minLevel = Math.min(minLevel, segment.getLevel());
            }

            // 添加前一块的重叠内容
            String overlapPrefix = "";
            if (i > 0 && config.getOverlapSize() > 0) {
                List<SemanticSegment> prevChunk = chunks.get(i - 1);
                overlapPrefix = extractOverlapFromEnd(prevChunk, config.getOverlapSize());
            }

            // 组装最终内容
            String finalContent = overlapPrefix + content.toString().trim();
            
            // 创建Document
            Document doc = new Document(finalContent);
            
            // 复制原始元数据
            if (originalMetadata != null) {
                doc.getMetadata().putAll(originalMetadata);
            }
            
            // 添加分块相关元数据
            int chunkIndex = startIndex + i;
            doc.getMetadata().put("chunkIndex", chunkIndex);
            doc.getMetadata().put("chunkTotal", chunks.size() + startIndex);
            doc.getMetadata().put("chunkSize", finalContent.length());
            doc.getMetadata().put("headingContext", headingContext.toString());
            doc.getMetadata().put("semanticUnits", String.join(",", unitTypes));
            doc.getMetadata().put("hierarchyLevel", minLevel);
            doc.getMetadata().put("hasOverlap", !overlapPrefix.isEmpty());
            
            result.add(doc);
        }

        return result;
    }

    /**
     * 从块末尾提取重叠内容
     */
    private String extractOverlapFromEnd(List<SemanticSegment> chunks, int overlapSize) {
        StringBuilder content = new StringBuilder();
        for (int i = chunks.size() - 1; i >= 0 && content.length() < overlapSize; i--) {
            content.insert(0, chunks.get(i).getContent() + " ");
        }
        
        String result = content.toString().trim();
        if (result.length() > overlapSize) {
            // 在句子边界截断
            int cutPoint = findLastSentenceEnd(result, 0, overlapSize);
            if (cutPoint > overlapSize / 2) {
                result = result.substring(0, cutPoint);
            } else {
                result = result.substring(0, overlapSize);
            }
        }
        
        return "【上文】" + result + "\n\n【当前内容】\n";
    }

    /**
     * 分析分块质量
     */
    public ChunkAnalysis analyzeChunkQuality(List<Document> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new ChunkAnalysis(0, 0, 0.0, 0, 0, Collections.emptyList());
        }

        List<Integer> sizes = chunks.stream()
            .map(d -> d.getText().length())
            .collect(Collectors.toList());
        
        int totalSize = sizes.stream().mapToInt(Integer::intValue).sum();
        double avgSize = sizes.stream().mapToInt(Integer::intValue).average().orElse(0);
        int minSize = sizes.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maxSize = sizes.stream().mapToInt(Integer::intValue).max().orElse(0);
        
        return new ChunkAnalysis(chunks.size(), totalSize, avgSize, minSize, maxSize, sizes);
    }

    /**
     * 分块分析结果
     */
    public static class ChunkAnalysis {
        private final int chunkCount;
        private final int totalSize;
        private final double avgSize;
        private final int minSize;
        private final int maxSize;
        private final List<Integer> sizeDistribution;

        public ChunkAnalysis(int chunkCount, int totalSize, double avgSize, 
                            int minSize, int maxSize, List<Integer> sizeDistribution) {
            this.chunkCount = chunkCount;
            this.totalSize = totalSize;
            this.avgSize = avgSize;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.sizeDistribution = sizeDistribution;
        }

        public int getChunkCount() { return chunkCount; }
        public int getTotalSize() { return totalSize; }
        public double getAvgSize() { return avgSize; }
        public int getMinSize() { return minSize; }
        public int maxSize() { return maxSize; }
        public List<Integer> getSizeDistribution() { return sizeDistribution; }

        @Override
        public String toString() {
            return String.format(
                "ChunkAnalysis{count=%d, total=%d, avg=%.1f, min=%d, max=%d}",
                chunkCount, totalSize, avgSize, minSize, maxSize
            );
        }
    }
}
