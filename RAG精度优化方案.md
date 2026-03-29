# RAG精度优化方案

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档版本 | v1.0 |
| 编写日期 | 2026-03-27 |
| 适用范围 | Knowledge Base 知识库问答系统 |

---

## 一、现状分析

### 1.1 系统架构概览

当前系统采用典型的RAG（Retrieval-Augmented Generation）架构：

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   用户提问   │───→│  向量检索    │───→│  Prompt构建  │───→│  LLM生成    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                          ↑
                          │
                   ┌─────────────┐
                   │  pgvector   │
                   │ 向量数据库   │
                   └─────────────┘
```

### 1.2 当前技术栈

| 层级 | 技术选型 |
|------|----------|
| 后端框架 | Spring Boot 3.2.0 + Spring AI 1.1.2 |
| 向量数据库 | PostgreSQL + pgvector |
| 大模型 | Ollama(deepseek-r1:1.5b) / 智谱AI(GLM-4) |
| Embedding | nomic-embed-text |
| 文件存储 | MinIO |
| 前端 | Vue3 + Element Plus |

### 1.3 现存问题

1. **Token消耗高**：上下文拼接策略简单，冗余信息多
2. **检索精度有限**：单一向量检索，缺乏语义精排
3. **并发能力弱**：同步处理文档，缺乏削峰机制
4. **缓存利用率低**：未充分利用多级缓存

---

## 二、优化目标

| 目标维度 | 具体指标 |
|----------|----------|
| **省Token** | 降低30%+ Token消耗 |
| **提精度** | 回答相关性提升20%+ |
| **高并发** | 支持10倍并发量提升 |

---

## 三、详细优化方案

### 3.1 省Token优化

#### 3.1.1 动态上下文压缩

**问题描述**：
当前代码中 `MAX_CONTEXT_LENGTH = 10` 直接拼接历史消息，Token消耗随对话轮数线性增长。

**优化方案**：

```java
/**
 * 对话摘要管理器
 * 当历史消息超过阈值时，使用轻量级模型生成摘要
 */
@Component
public class ConversationSummarizer {
    
    private static final int SUMMARIZE_THRESHOLD = 6;  // 超过6轮触发摘要
    private static final int SUMMARY_MAX_TOKENS = 200; // 摘要长度限制
    
    /**
     * 压缩对话历史
     * 原始消息 → 摘要 + 最近2轮完整对话
     */
    public String compressContext(List<ChatMessage> messages) {
        if (messages.size() <= SUMMARIZE_THRESHOLD) {
            return formatMessages(messages); // 直接返回原始消息
        }
        
        // 早期消息生成摘要
        List<ChatMessage> earlyMessages = messages.subList(0, messages.size() - 2);
        String summary = generateSummary(earlyMessages);
        
        // 保留最近2轮完整对话
        List<ChatMessage> recentMessages = messages.subList(messages.size() - 2, messages.size());
        
        return String.format("【历史摘要】%s\n\n【最近对话】%s", 
            summary, formatMessages(recentMessages));
    }
}
```

**预期收益**：长对话场景Token消耗降低40-60%

#### 3.1.2 检索结果精筛

**问题描述**：
当前 `buildPromptWithSources()` 将所有检索到的文档块直接拼入Prompt，包含低相关性内容。

**优化方案**：

```java
/**
 * 增强的Prompt构建，增加相关性过滤
 */
private PromptResult buildPromptWithSources(String message, String knowledgeBaseId) {
    // 1. 执行向量检索
    SearchRequest request = SearchRequest.builder()
        .query(message)
        .topK(10)  // 先召回较多结果
        .filterKnowledgeBase(knowledgeBaseId)
        .build();
    
    List<Document> candidates = vectorStore.similaritySearch(request);
    
    // 2. 相似度阈值过滤（新增）
    double SIMILARITY_THRESHOLD = 0.75;
    List<Document> filtered = candidates.stream()
        .filter(doc -> doc.getScore() >= SIMILARITY_THRESHOLD)
        .limit(5)  // 限制最终数量
        .collect(Collectors.toList());
    
    // 3. 去重处理（新增）
    List<Document> deduplicated = deduplicateByContent(filtered);
    
    // 4. 构建Prompt
    String prompt = buildSystemPrompt(deduplicated, message);
    
    return new PromptResult(prompt, extractSources(deduplicated));
}
```

**预期收益**：减少无关内容，提升回答质量，节省20% Token

#### 3.1.3 Prompt模板优化

**当前模板问题**：
- 系统Prompt过长，包含冗余描述
- 指令使用自然语言，Token效率低

**优化方案**：

```java
// 优化前（冗长）
private static final String SYSTEM_TEMPLATE = """
    你是一个专业的知识库助手。你的任务是根据提供的参考文档回答用户问题。
    请遵循以下规则：
    1. 仅基于提供的参考文档回答问题
    2. 如果文档中没有相关信息，请明确告知
    3. 回答要准确、简洁、专业
    4. 引用相关文档来源
    """;

// 优化后（精简结构化）
private static final String SYSTEM_TEMPLATE = """
    <role>知识库助手</role>
    <rules>
    1.仅基于<docs>回答
    2.无信息时明确告知
    3.准确简洁专业
    4.标注引用来源[doc_id]
    </rules>
    <docs>{documents}</docs>
    <question>{question}</question>
    """;
```

**预期收益**：系统Prompt Token减少50%

#### 3.1.4 多级缓存机制

**缓存架构设计**：

```
┌─────────────────────────────────────────────────────────────┐
│                        缓存层级架构                          │
├─────────────────────────────────────────────────────────────┤
│  L1: 本地缓存 (Caffeine)                                     │
│      - Embedding结果缓存                                     │
│      - 热点Query响应缓存                                     │
│      - TTL: 5-10分钟                                         │
├─────────────────────────────────────────────────────────────┤
│  L2: 分布式缓存 (Redis)                                      │
│      - 向量检索结果缓存                                      │
│      - 对话历史缓存                                          │
│      - TTL: 10-30分钟                                        │
├─────────────────────────────────────────────────────────────┤
│  L3: 语义缓存 (向量相似度)                                    │
│      - 相似Query结果复用                                     │
│      - 使用SimHash/MinHash快速去重                          │
└─────────────────────────────────────────────────────────────┘
```

**实现代码**：

```java
@Component
public class RAGCacheManager {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private final Cache<String, Embedding> embeddingCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build();
    
    /**
     * 语义缓存键生成
     */
    public String generateSemanticKey(String query) {
        // 使用SimHash生成语义指纹
        return "rag:semantic:" + SimHash.hash(query);
    }
    
    /**
     * 缓存向量检索结果
     */
    public void cacheSearchResult(String query, List<Document> results) {
        String key = generateSemanticKey(query);
        redisTemplate.opsForValue().set(key, serialize(results), Duration.ofMinutes(10));
    }
    
    /**
     * 获取缓存的检索结果
     */
    public Optional<List<Document>> getCachedResult(String query) {
        String key = generateSemanticKey(query);
        String cached = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(cached).map(this::deserialize);
    }
}
```

**预期收益**：热点Query Token消耗降低80%+

---

### 3.2 提升精度优化

#### 3.2.1 RAG架构升级

**当前架构 vs 优化架构**：

```
【当前架构】                    【优化架构】
                                 
用户Query ──────┐               用户Query ───→ Query理解/改写 ───┐
                │                                               │
                ↓                                               ↓
向量检索 ───→ 结果拼接 ───→ LLM    多路召回 ───→ 重排序 ───→ 上下文压缩 ───→ LLM
                ↑                              ↑
                │               ┌──────────────┼──────────────┐
           pgvector            向量检索      关键词检索      知识图谱
```

#### 3.2.2 多路召回策略

**实现方案**：

```java
/**
 * 多路召回检索器
 */
@Component
public class MultiRetriever {
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private ElasticsearchClient esClient;  // 新增
    
    /**
     * 执行多路召回
     */
    public List<Document> retrieve(String query, String knowledgeBaseId) {
        // 1. 向量检索（语义相似度）
        CompletableFuture<List<Document>> vectorFuture = CompletableFuture.supplyAsync(() ->
            vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(10)
                .filterKnowledgeBase(knowledgeBaseId)
                .build())
        );
        
        // 2. 关键词检索（精确匹配）
        CompletableFuture<List<Document>> keywordFuture = CompletableFuture.supplyAsync(() ->
            keywordSearch(query, knowledgeBaseId)
        );
        
        // 3. 等待所有结果
        List<Document> vectorResults = vectorFuture.join();
        List<Document> keywordResults = keywordFuture.join();
        
        // 4. 结果融合（RRF算法）
        return reciprocalRankFusion(vectorResults, keywordResults);
    }
    
    /**
     * RRF融合算法
     * 公式: score = Σ(1 / (k + rank))
     */
    private List<Document> reciprocalRankFusion(List<Document>... resultLists) {
        final int K = 60;  // RRF常数
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, Document> docMap = new HashMap<>();
        
        for (List<Document> results : resultLists) {
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                String id = doc.getId();
                double score = 1.0 / (K + i + 1);
                scoreMap.merge(id, score, Double::sum);
                docMap.putIfAbsent(id, doc);
            }
        }
        
        return scoreMap.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(10)
            .map(e -> docMap.get(e.getKey()))
            .collect(Collectors.toList());
    }
}
```

**预期收益**：召回率提升15-25%

#### 3.2.3 重排序模型

**方案设计**：

```java
/**
 * 重排序服务
 * 使用Cross-Encoder模型精排
 */
@Component
public class RerankService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${rerank.model.url}")
    private String modelUrl;
    
    /**
     * 对检索结果重排序
     */
    public List<Document> rerank(String query, List<Document> candidates, int topK) {
        // 构建批处理请求
        List<RerankRequest.Pair> pairs = candidates.stream()
            .map(doc -> new RerankRequest.Pair(query, doc.getText()))
            .collect(Collectors.toList());
        
        RerankRequest request = new RerankRequest(pairs);
        
        // 调用重排序模型（如bge-reranker-large）
        RerankResponse response = restTemplate.postForObject(
            modelUrl, request, RerankResponse.class);
        
        // 按分数排序并返回TopK
        List<ScoredDocument> scored = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            scored.add(new ScoredDocument(
                candidates.get(i), 
                response.getScores().get(i)
            ));
        }
        
        return scored.stream()
            .sorted(Comparator.comparing(ScoredDocument::getScore).reversed())
            .limit(topK)
            .map(ScoredDocument::getDocument)
            .collect(Collectors.toList());
    }
}
```

**模型选型建议**：

| 模型 | 语言 | 精度 | 延迟 | 适用场景 |
|------|------|------|------|----------|
| bge-reranker-large | 多语言 | 高 | 中等 | 通用场景 |
| bge-reranker-base | 多语言 | 中高 | 低 | 延迟敏感 |
| Cohere Rerank | 英文 | 高 | 中等 | 英文为主 |

**预期收益**：Top5准确率提升20-30%

#### 3.2.4 文档分块策略优化

**当前问题**：
- 固定CHUNK_SIZE，未考虑语义边界
- 缺乏重叠，容易切断上下文

**优化方案**：

```java
/**
 * 智能文档分块器
 */
@Component
public class SmartDocumentSplitter {
    
    /**
     * 语义感知的分块策略
     */
    public List<Document> split(List<Document> documents) {
        List<Document> chunks = new ArrayList<>();
        
        for (Document doc : documents) {
            String text = doc.getText();
            
            // 1. 按语义边界（段落、句子）初步分割
            List<String> paragraphs = splitBySemanticBoundary(text);
            
            // 2. 智能合并，保持语义完整性
            List<String> merged = mergeParagraphs(paragraphs, 800, 1500); // min:800, max:1500
            
            // 3. 添加重叠窗口
            List<String> withOverlap = addOverlap(merged, 100); // 100字符重叠
            
            // 4. 构建Document对象
            for (int i = 0; i < withOverlap.size(); i++) {
                Document chunk = new Document(withOverlap.get(i));
                chunk.getMetadata().putAll(doc.getMetadata());
                chunk.getMetadata().put("chunk_index", i);
                chunk.getMetadata().put("chunk_total", withOverlap.size());
                chunks.add(chunk);
            }
        }
        
        return chunks;
    }
    
    /**
     * 按语义边界分割
     */
    private List<String> splitBySemanticBoundary(String text) {
        // 优先按标题分割，其次按段落，最后按句子
        List<String> boundaries = Arrays.asList(
            "\n#{1,6} ",           // Markdown标题
            "\n\n",                // 段落
            "(?<=[.!?。！？])\s+"  // 句子
        );
        
        for (String pattern : boundaries) {
            List<String> parts = Arrays.asList(text.split(pattern));
            if (parts.size() > 1) {
                return parts;
            }
        }
        
        return Collections.singletonList(text);
    }
}
```

**预期收益**：检索相关性提升15%

#### 3.2.5 引用溯源机制

**实现方案**：

```java
/**
 * 强制引用Prompt模板
 */
private static final String CITATION_PROMPT = """
    <role>知识库助手</role>
    <instruction>
    基于<docs>回答<question>。
    必须在回答中使用[citation:doc_id]标注引用来源。
    仅引用确实使用的文档。
    </instruction>
    
    <docs>
    {documents}
    </docs>
    
    <question>{question}</question>
    
    <format>
    回答内容...[citation:doc_001]
    更多内容...[citation:doc_002]
    
    ## 参考来源
    - [1] 文档标题1 (doc_001)
    - [2] 文档标题2 (doc_002)
    </format>
    """;
```

**预期收益**：回答可信度显著提升

---

### 3.3 高并发优化

#### 3.3.1 架构升级

**优化后架构**：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              接入层                                      │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                  │
│  │   Nginx     │───→│  Rate Limiter│───→│  Load Balancer│               │
│  │  (SSL/静态)  │    │  (限流)      │    │  (负载均衡)   │               │
│  └─────────────┘    └─────────────┘    └─────────────┘                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                           服务层（多实例）                                │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     Spring Boot Application                      │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │   │
│  │  │  Chat API   │  │ Document API│  │  Search API │  │ Admin   │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────┘ │   │
│  │                                                                  │   │
│  │  ┌─────────────────────────────────────────────────────────┐    │   │
│  │  │              AI Gateway (Resilience4j)                   │    │   │
│  │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐     │    │   │
│  │  │  │Circuit  │  │  Rate   │  │  Bulk   │  │  Retry  │     │    │   │
│  │  │  │Breaker  │  │ Limiter │  │  Head   │  │  Policy │     │    │   │
│  │  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘     │    │   │
│  │  └─────────────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ↓               ↓               ↓
            ┌───────────┐   ┌───────────┐   ┌───────────┐
            │   Redis   │   │ RabbitMQ  │   │  Milvus   │
            │  (缓存)    │   │  (队列)   │   │(向量数据库)│
            └───────────┘   └───────────┘   └───────────┘
```

#### 3.3.2 AI Gateway 限流熔断

**实现方案**：

```java
/**
 * AI服务网关配置
 */
@Configuration
public class AIGatewayConfig {
    
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> circuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                .failureRateThreshold(50)          // 失败率阈值50%
                .slowCallRateThreshold(80)         // 慢调用阈值80%
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .permittedNumberOfCallsInHalfOpenState(10)
                .slidingWindowSize(100)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30))
                .build())
            .build());
    }
    
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.of(RateLimiterConfig.custom()
            .limitForPeriod(100)                  // 每秒100请求
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMillis(100))
            .build());
    }
}

/**
 * 带保护的Chat服务
 */
@Service
public class ProtectedChatService {
    
    @CircuitBreaker(name = "chat", fallbackMethod = "chatFallback")
    @RateLimiter(name = "chat")
    @TimeLimiter(name = "chat")
    public Mono<String> chatWithProtection(String message) {
        return webClient.post()
            .uri(aiServiceUrl)
            .bodyValue(message)
            .retrieve()
            .bodyToMono(String.class);
    }
    
    /**
     * 降级策略：返回缓存或友好提示
     */
    public Mono<String> chatFallback(String message, Throwable ex) {
        log.warn("AI服务降级，原因: {}", ex.getMessage());
        return Mono.just("服务繁忙，请稍后重试");
    }
}
```

#### 3.3.3 消息队列削峰

**文档处理异步化**：

```java
/**
 * 文档处理消息队列配置
 */
@Configuration
public class DocumentQueueConfig {
    
    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    public static final String DOCUMENT_QUEUE = "document.processing.queue";
    public static final String DOCUMENT_ROUTING_KEY = "document.process";
    
    @Bean
    public Queue documentQueue() {
        return QueueBuilder.durable(DOCUMENT_QUEUE)
            .withArgument("x-max-priority", 10)        // 优先级队列
            .withArgument("x-message-ttl", 3600000)    // 1小时过期
            .withArgument("x-dead-letter-exchange", "document.dlx")
            .build();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setConfirmCallback((correlation, ack, reason) -> {
            if (!ack) {
                log.error("消息发送失败: {}", reason);
            }
        });
        return template;
    }
}

/**
 * 文档生产者
 */
@Service
public class DocumentMessageProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendDocumentProcessingTask(DocumentTask task) {
        MessageProperties props = new MessageProperties();
        props.setPriority(task.getPriority());  // 大文件低优先级
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        
        Message message = new Message(
            serialize(task), 
            props
        );
        
        rabbitTemplate.send(
            DocumentQueueConfig.DOCUMENT_EXCHANGE,
            DocumentQueueConfig.DOCUMENT_ROUTING_KEY,
            message
        );
    }
}

/**
 * 文档消费者（多实例并发处理）
 */
@Component
public class DocumentMessageConsumer {
    
    @RabbitListener(
        queues = DocumentQueueConfig.DOCUMENT_QUEUE,
        concurrency = "5-20"  // 5-20个并发消费者
    )
    public void processDocument(DocumentTask task) {
        log.info("处理文档任务: {}", task.getDocumentId());
        documentProcessingService.process(task);
    }
}
```

**预期收益**：文档处理能力提升10倍，系统稳定性大幅提升

#### 3.3.4 线程池优化

**优化配置**：

```java
@Configuration
public class OptimizedThreadPoolConfig {
    
    /**
     * 计算密集型任务：向量计算、Embedding
     */
    @Bean("computeExecutor")
    public Executor computeExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cores);
        executor.setMaxPoolSize(cores * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("compute-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    /**
     * IO密集型任务：AI模型调用
     */
    @Bean("ioExecutor")
    public Executor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("ai-io-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("AI调用队列满，任务被拒绝");
            throw new ServiceUnavailableException("服务繁忙");
        });
        executor.initialize();
        return executor;
    }
    
    /**
     * 快速响应线程池：简单查询
     */
    @Bean("fastExecutor")
    public Executor fastExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("fast-");
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}
```

#### 3.3.5 数据库优化

**pgvector优化**：

```sql
-- 1. 创建高效的HNSW索引（已配置，建议调整参数）
CREATE INDEX ON vector_store 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);  -- 根据数据量调整

-- 2. 查询时设置ef_search（精度vs速度权衡）
SET hnsw.ef_search = 100;  -- 默认100，可提高到200-400提升精度

-- 3. 分区表（大数据量时）
CREATE TABLE vector_store_partitioned (
    id uuid PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(768),
    knowledge_base_id varchar(50)
) PARTITION BY HASH (knowledge_base_id);

-- 创建分区
CREATE TABLE vector_store_p0 PARTITION OF vector_store_partitioned FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE vector_store_p1 PARTITION OF vector_store_partitioned FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE vector_store_p2 PARTITION OF vector_store_partitioned FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE vector_store_p3 PARTITION OF vector_store_partitioned FOR VALUES WITH (MODULUS 4, REMAINDER 3);
```

**连接池优化**：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50          # 根据压测调整
      minimum-idle: 20
      connection-timeout: 10000      # 10秒
      idle-timeout: 300000           # 5分钟
      max-lifetime: 600000           # 10分钟
      leak-detection-threshold: 60000 # 连接泄漏检测
```

#### 3.3.6 水平扩展方案

**部署架构**：

```yaml
# docker-compose.yml 生产环境配置
version: '3.8'

services:
  # 应用层 - 3个实例
  app:
    image: knowledge-base:latest
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '2.0'
          memory: 4G
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres-cluster
      - REDIS_HOST=redis-cluster
      - MILVUS_HOST=milvus-cluster
  
  # Nginx负载均衡
  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    ports:
      - "80:80"
      - "443:443"
  
  # Milvus向量数据库集群
  milvus-standalone:
    image: milvusdb/milvus:v2.3.3
    environment:
      ETCD_ENDPOINTS: etcd:2379
      MINIO_ADDRESS: minio:9000
```

---

## 四、模型选型建议

### 4.1 Embedding模型

| 模型 | 维度 | 语言 | 特点 | 推荐场景 |
|------|------|------|------|----------|
| **bge-m3** | 1024 | 多语言 | 多粒度、多任务 | 首选推荐 |
| bge-large-zh | 1024 | 中文 | 中文优化 | 纯中文场景 |
| nomic-embed-text | 768 | 英文 | 轻量快速 | 资源受限 |
| text-embedding-3 | 1536 | 多语言 | OpenAI出品 | 高质量要求 |

### 4.2 重排序模型

| 模型 | 语言 | 精度 | 延迟 | 部署方式 |
|------|------|------|------|----------|
| **bge-reranker-v2-m3** | 多语言 | 高 | 中等 | 本地部署 |
| bge-reranker-large | 多语言 | 很高 | 较高 | 本地部署 |
| Cohere Rerank | 英文 | 高 | 低 | API调用 |

### 4.3 大模型选择策略

```java
/**
 * 模型路由策略
 */
@Component
public class ModelRouter {
    
    public ChatModel route(String query, List<Document> context) {
        // 1. 分析查询复杂度
        Complexity complexity = analyzeComplexity(query, context);
        
        return switch (complexity) {
            case SIMPLE -> getLightModel();      // 简单FAQ：小模型
            case STANDARD -> getDefaultModel();  // 标准RAG：默认模型
            case COMPLEX -> getPowerfulModel();  // 复杂推理：大模型
        };
    }
    
    private Complexity analyzeComplexity(String query, List<Document> context) {
        // 基于以下因素判断：
        // - 问题长度和关键词
        // - 检索结果数量和相关性
        // - 是否需要多步推理
        // - 历史对话复杂度
    }
}
```

---

## 五、配置参数汇总

### 5.1 推荐配置（生产环境）

```yaml
# application-optimized.yaml

# ==================== 数据库配置 ====================
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 20
      connection-timeout: 10000
      
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true

# ==================== AI模型配置 ====================
  ai:
    ollama:
      chat:
        options:
          model: deepseek-r1:7b  # 升级模型
          temperature: 0.3       # 降低随机性
          max-tokens: 2048       # 限制输出长度
          top-p: 0.9
          
    zhipuai:
      chat:
        options:
          model: glm-4-flash
          temperature: 0.3
          max-tokens: 2048
          
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1024         # 匹配bge-m3
        max-document-batch-size: 500

# ==================== 优化配置 ====================
optimization:
  # 缓存配置
  cache:
    embedding-ttl: 3600
    search-result-ttl: 300
    response-ttl: 600
    
  # 检索配置
  retrieval:
    vector-top-k: 20           # 召回数量
    similarity-threshold: 0.75  # 相似度阈值
    rerank-top-k: 5            # 精排后数量
    enable-keyword-search: true # 启用关键词检索
    
  # 上下文配置
  context:
    max-history-messages: 10
    summarize-threshold: 6
    summary-max-tokens: 200
    max-context-chunks: 5
    
  # 限流配置
  rate-limit:
    requests-per-minute-per-user: 60
    tokens-per-minute-per-user: 10000
    concurrent-requests-per-user: 5
    
  # 线程池配置
  thread-pool:
    document-core-size: 8
    document-max-size: 20
    chat-core-size: 16
    chat-max-size: 50

# ==================== 外部服务配置 ====================
rerank:
  model:
    url: http://localhost:8000/rerank
    timeout: 5000
```

---

## 六、实施路线图

### 6.1 阶段规划

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           实施路线图                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  第一阶段（1-2周）：基础优化                                              │
│  ├─ [P0] 向量检索缓存实现                                                │
│  ├─ [P0] Prompt压缩优化                                                  │
│  ├─ [P0] 相似度阈值过滤                                                  │
│  └─ [P0] 线程池参数调优                                                  │
│                              ↓                                          │
│  第二阶段（2-3周）：精度提升                                              │
│  ├─ [P1] 多路召回实现（向量+关键词）                                      │
│  ├─ [P1] 重排序模型集成                                                   │
│  ├─ [P1] 智能分块策略                                                     │
│  └─ [P1] 引用溯源机制                                                     │
│                              ↓                                          │
│  第三阶段（3-4周）：并发增强                                              │
│  ├─ [P2] 消息队列集成（RabbitMQ）                                         │
│  ├─ [P2] AI Gateway限流熔断                                               │
│  ├─ [P2] Redis集群部署                                                    │
│  └─ [P2] 应用多实例部署                                                   │
│                              ↓                                          │
│  第四阶段（4-6周）：架构升级                                              │
│  ├─ [P3] pgvector → Milvus迁移                                           │
│  ├─ [P3] 知识图谱集成                                                     │
│  ├─ [P3] 模型路由策略                                                     │
│  └─ [P3] 全链路监控                                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 预期收益

| 阶段 | 优化项 | Token节省 | 精度提升 | 并发提升 |
|------|--------|-----------|----------|----------|
| 第一阶段 | 缓存+Prompt优化 | 30% | 5% | 20% |
| 第二阶段 | 多路召回+重排序 | - | 25% | - |
| 第三阶段 | 消息队列+限流 | - | - | 500% |
| 第四阶段 | Milvus+图谱 | 10% | 15% | 1000% |
| **总计** | **全部优化** | **40%** | **45%** | **1500%** |

---

## 七、监控指标

### 7.1 关键指标定义

```yaml
# 业务指标
metrics:
  token:
    - input_tokens_per_request      # 单次请求输入Token数
    - output_tokens_per_request     # 单次请求输出Token数
    - token_cost_per_request        # 单次请求Token成本
    
  quality:
    - retrieval_recall_rate         # 检索召回率
    - retrieval_precision_rate      # 检索精确率
    - rerank_ndcg@5                 # 重排序NDCG
    - user_satisfaction_score       # 用户满意度
    
  performance:
    - p50_response_time             # P50响应时间
    - p99_response_time             # P99响应时间
    - throughput_qps                # 系统吞吐量
    - concurrent_users              # 并发用户数
    
  stability:
    - error_rate                    # 错误率
    - circuit_breaker_open_count    # 熔断次数
    - rate_limit_hit_count          # 限流触发次数
```

### 7.2 告警阈值

| 指标 | 警告阈值 | 严重阈值 |
|------|----------|----------|
| P99响应时间 | > 5s | > 10s |
| 错误率 | > 1% | > 5% |
| Token消耗环比 | > 20% | > 50% |
| 队列堆积 | > 100 | > 500 |

---

## 八、风险评估

| 风险项 | 影响 | 概率 | 应对措施 |
|--------|------|------|----------|
| 重排序模型延迟高 | 响应变慢 | 中 | 异步精排、模型量化 |
| 缓存数据不一致 | 回答错误 | 低 | 版本控制、增量更新 |
| Milvus迁移复杂 | 服务中断 | 中 | 双写过渡、灰度迁移 |
| 模型切换不稳定 | 质量波动 | 低 | A/B测试、灰度发布 |

---

## 九、附录

### 9.1 参考资源

- [BGE Embedding模型](https://github.com/FlagOpen/FlagEmbedding)
- [Spring AI文档](https://docs.spring.io/spring-ai/reference/)
- [pgvector最佳实践](https://github.com/pgvector/pgvector)
- [Milvus官方文档](https://milvus.io/docs)

### 9.2 术语表

| 术语 | 解释 |
|------|------|
| RAG | Retrieval-Augmented Generation，检索增强生成 |
| Embedding | 文本向量化表示 |
| HNSW | Hierarchical Navigable Small World，向量索引算法 |
| Rerank | 重排序，对初步检索结果精细排序 |
| RRF | Reciprocal Rank Fusion，倒数排名融合 |

---

**文档结束**
