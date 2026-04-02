# RAG 精度优化文档

## 问题描述

当用户询问"Vue 3 简介"时，匹配结果中出现了 SpringBoot 相关文档，说明向量检索的匹配度不够精确，存在以下问题：

1. **没有设置相似度阈值** - 低相关度的文档也被返回
2. **没有限制返回结果数量** - 返回结果过多，质量参差不齐
3. **缺少相似度分数展示** - 无法判断文档与问题的相关程度
4. **文本分块策略可能不够精细** - 分块大小和重叠度需要优化

---

## 优化方案

### 方案一：使用 SearchRequest 进行精确检索（推荐）

Spring AI 提供了 `SearchRequest` 类，可以设置相似度阈值和返回数量。

**修改文件：** `src/main/java/com/snrt/knowledgebase/service/ChatService.java`

**修改内容：**

```java
import org.springframework.ai.vectorstore.SearchRequest;

private PromptResult buildPromptWithSources(String message, String knowledgeBaseId) {
    if (knowledgeBaseId == null) {
        log.debug("[Prompt构建] 未指定知识库，直接返回用户消息");
        return new PromptResult(message, null);
    }

    log.info("[Prompt构建] 知识库ID: {}, 开始检索相关文档", knowledgeBaseId);

    // 使用 SearchRequest 设置相似度阈值和返回数量
    SearchRequest searchRequest = SearchRequest.builder()
        .query(message)
        .topK(20)                    // 增加初始检索数量，后续再过滤
        .similarityThreshold(0.5)    // 设置相似度阈值 0.5（可根据实际情况调整）
        .build();

    List<Document> relevantDocs = vectorStore.similaritySearch(searchRequest);
    log.debug("[Prompt构建] 检索到 {} 个相关文档", relevantDocs.size());
    
    // ... 后续逻辑保持不变
}
```

**参数说明：**
- `topK(20)` - 初始检索20个文档，给后续过滤留有余地
- `similarityThreshold(0.5)` - 只返回相似度大于等于0.5的文档（注意：pgvector返回的是余弦距离，需要转换为相似度，详见下文"余弦距离与相似度的关系"）

---

### 方案二：增加后置过滤和排序

在检索后增加基于相似度分数的过滤和排序，确保返回最相关的内容。

**修改文件：** `src/main/java/com/snrt/knowledgebase/service/ChatService.java`

**修改内容：**

```java
private PromptResult buildPromptWithSources(String message, String knowledgeBaseId) {
    // ... 前面的代码 ...

    List<Document> relevantDocs = vectorStore.similaritySearch(searchRequest);
    
    // 按相似度分数排序（从高到低）
    relevantDocs.sort((d1, d2) -> {
        Double score1 = (Double) d1.getMetadata().get("distance");
        Double score2 = (Double) d2.getMetadata().get("distance");
        return Double.compare(score2 != null ? score2 : 0, score1 != null ? score1 : 0);
    });

    // 过滤并限制结果
    List<Document> filteredDocs = relevantDocs.stream()
        .filter(doc -> {
            // 知识库ID过滤
            Object kbId = doc.getMetadata().get(Constants.VectorStore.METADATA_KNOWLEDGE_BASE_ID);
            boolean kbMatch = kbId != null && kbId.equals(knowledgeBaseId);
            
            // 相似度过滤（如果 SearchRequest 的阈值不够，可以在这里二次过滤）
            Double score = (Double) doc.getMetadata().get("distance");
            boolean scoreMatch = score == null || score >= 0.7; // 相似度阈值
            
            return kbMatch && scoreMatch;
        })
        .limit(Constants.Chat.MAX_CONTEXT_LENGTH)
        .collect(Collectors.toList());

    // ... 后续代码 ...
}
```

---

### 方案三：优化文档分块策略

调整分块大小和重叠度，提高检索精度。

**修改文件：** `src/main/java/com/snrt/knowledgebase/constants/Constants.java`

**修改内容：**

```java
public static final class VectorStore {
    public static final int CHUNK_SIZE = 800;      // 从 1000 调整为 800
    public static final int CHUNK_OVERLAP = 100;   // 从 200 调整为 100
    // ... 其他常量
}
```

**调整说明：**
- 减小 `CHUNK_SIZE` 可以提高检索的精确度，但可能丢失上下文
- 减小 `CHUNK_OVERLAP` 可以减少冗余，但需要权衡信息连续性

---

### 方案四：在返回结果中显示相似度分数

让用户了解每个来源的匹配程度，便于判断答案可信度。

**修改文件：** `src/main/java/com/snrt/knowledgebase/service/ChatService.java`

**修改内容：**

```java
List<DocumentSourceDTO> documentSources = docsByDocumentId.entrySet().stream()
    .map(entry -> {
        String docId = entry.getKey();
        List<Document> docChunks = entry.getValue();
        Document firstDoc = docChunks.get(0);

        Object docName = firstDoc.getMetadata().get(Constants.VectorStore.METADATA_DOCUMENT_NAME);
        Object kbName = firstDoc.getMetadata().get(Constants.VectorStore.METADATA_KNOWLEDGE_BASE_NAME);
        
        // 获取相似度分数
        Double score = (Double) firstDoc.getMetadata().get("distance");
        if (score == null) {
            score = 0.0;
        }

        // 收集所有分块的片段内容
        List<String> snippets = docChunks.stream()
            .map(doc -> {
                String text = doc.getText();
                return text.length() > 200 ? text.substring(0, 200) + "..." : text;
            })
            .collect(Collectors.toList());

        return DocumentSourceDTO.builder()
            .documentId(docId.equals("unknown") ? null : docId)
            .documentName(docName != null ? docName.toString() : "未知文档")
            .knowledgeBaseName(kbName != null ? kbName.toString() : "未知知识库")
            .score(score)  // 设置相似度分数
            .snippet(snippets.isEmpty() ? "" : snippets.get(0))
            .snippets(snippets)
            .build();
    })
    // 按相似度分数排序
    .sorted((s1, s2) -> Double.compare(s2.getScore() != null ? s2.getScore() : 0, 
                                       s1.getScore() != null ? s1.getScore() : 0))
    .collect(Collectors.toList());
```

---

### 方案五：前端显示相似度分数

在前端界面展示每个来源的匹配度，帮助用户判断信息可靠性。

**修改文件：** `knowledge-base-ui/src/components/chat/SourcesPanel.vue`（或相关组件）

**修改内容：**

```vue
<template>
  <div class="source-list">
    <div class="source-item" v-for="source in sources" :key="source.documentId">
      <div class="source-header">
        <span class="source-name">{{ source.documentName }}</span>
        <span class="source-score" v-if="source.score !== null">
          匹配度: {{ (source.score * 100).toFixed(1) }}%
        </span>
      </div>
      <div class="source-kb">{{ source.knowledgeBaseName }}</div>
      <div class="source-snippet">{{ source.snippet }}</div>
    </div>
  </div>
</template>

<style scoped>
.source-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.source-score {
  font-size: 12px;
  color: #409eff;
  background-color: #ecf5ff;
  padding: 2px 8px;
  border-radius: 4px;
}
</style>
```

---

## 重要概念：余弦距离与相似度的关系

### 核心区别

在使用 pgvector 向量数据库时，需要特别注意 **余弦距离（Cosine Distance）** 和 **余弦相似度（Cosine Similarity）** 的区别：

| 概念 | 计算公式 | 取值范围 | 含义 |
|------|---------|---------|------|
| **余弦距离** | `distance = 1 - similarity` | 0.0 ~ 2.0 | **越小越相似** |
| **余弦相似度** | `similarity = 1 - distance` | -1.0 ~ 1.0 | **越大越相似** |

### 实际应用中的转换

pgvector 默认返回的是 **余弦距离**，但我们在业务逻辑中通常使用 **相似度** 更直观：

```java
// 从 metadata 中获取的是余弦距离
Object distanceObj = doc.getMetadata().get("distance");
double distance = ((Number) distanceObj).doubleValue();

// 转换为相似度（范围 0~1）
double similarity = 1.0 - distance;
```

### 分数对照表

| 余弦距离 | 余弦相似度 | 匹配程度 | 业务含义 |
|---------|-----------|---------|---------|
| 0.0 | 1.0 (100%) | 完全相同 | 理想状态，很少达到 |
| 0.2 | 0.8 (80%) | 高度相似 | 非常相关，推荐阈值上限 |
| 0.3 | 0.7 (70%) | 比较相似 | 相关，可作为严格阈值 |
| **0.45** | **0.55 (55%)** | **一般相似** | **Vue 3 文档实际分数** |
| 0.5 | 0.5 (50%) | 基本相关 | 建议的最低阈值 |
| 0.7 | 0.3 (30%) | 弱相关 | 通常应该过滤掉 |
| 1.0 | 0.0 (0%) | 不相关 | 完全不同 |

### 阈值设置建议

根据实际测试数据（Vue 3 文档相似度约 0.51-0.55）：

```java
// 推荐阈值范围
double similarityThreshold = 0.5;  // 平衡精度和召回率（当前推荐）
double similarityThreshold = 0.55; // 更严格，可能过滤部分内容
double similarityThreshold = 0.45; // 更宽松，可能包含更多内容
```

### 日志解读示例

```
[Prompt构建] 文档: test-rag-vue3-guide.md, 余弦距离: 0.452, 相似度: 0.55, 知识库匹配: true, 相似度匹配: true
[Prompt构建] 文档: test-rag-springboot.md, 余弦距离: 0.650, 相似度: 0.35, 知识库匹配: true, 相似度匹配: false ← 被过滤
```

**解读：**
- Vue 3 文档：相似度 0.55 > 阈值 0.5，**保留**
- SpringBoot 文档：相似度 0.35 < 阈值 0.5，**过滤**

---

## 实施建议

### 推荐实施顺序

1. **首先实施方案一** - 使用 `SearchRequest` 设置相似度阈值（0.5）和返回数量
2. **然后实施方案四** - 在返回结果中包含相似度分数，便于调试和展示
3. **根据效果考虑方案三** - 调整分块策略

### 相似度阈值调整建议

**注意：** 以下阈值指的是**余弦相似度**（范围 0~1），不是余弦距离。

| 相似度阈值 | 对应余弦距离 | 适用场景 | 预期效果 |
|-----------|-------------|----------|----------|
| 0.45-0.5 | 0.5-0.55 | 宽松匹配 | 召回率高，可能包含弱相关内容 |
| **0.5-0.55** | **0.45-0.5** | **平衡推荐** | **Vue 3 文档实际分数范围，推荐默认值** |
| 0.6-0.7 | 0.3-0.4 | 严格匹配 | 精度高，可能漏掉部分内容 |
| 0.7+ | <0.3 | 非常严格 | 只保留高度相关内容 |

### 验证方法

1. 使用测试问题验证检索效果：
   - "Vue 3 简介" - 应该只返回 Vue 相关文档
   - "SpringBoot 自动配置原理" - 应该只返回 SpringBoot 相关文档

2. 观察相似度分数分布，调整阈值到合适范围

3. 监控检索结果数量和质量，确保用户体验

---

## 相关文件清单

- `src/main/java/com/snrt/knowledgebase/service/ChatService.java` - 核心检索逻辑
- `src/main/java/com/snrt/knowledgebase/constants/Constants.java` - 分块参数配置
- `src/main/java/com/snrt/knowledgebase/dto/DocumentSourceDTO.java` - 文档来源DTO
- `knowledge-base-ui/src/components/chat/SourcesPanel.vue` - 前端展示组件（路径可能不同）

---

## 注意事项

1. **理解距离与相似度的区别**：pgvector 返回的是余弦距离（越小越相似），业务逻辑通常使用相似度（越大越相似），转换公式为 `similarity = 1 - distance`
2. **相似度阈值需要根据实际数据调整**，不同 embedding 模型的分数分布可能不同
3. **修改分块策略后需要重新索引文档**，否则不会生效
4. **建议先在测试环境验证效果**，再应用到生产环境
5. **可以添加配置项**，让相似度阈值可动态调整，无需重启服务
6. **观察日志中的实际分数**，根据真实数据调整阈值，不要仅凭理论值设置
