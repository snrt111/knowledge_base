# 智能知识库系统 (Knowledge Base System)

基于 RAG（检索增强生成）技术的智能知识库问答系统，支持文档管理、向量化存储和 AI 智能问答。

## 项目简介

本项目是一个前后端分离的智能知识库系统，采用 Spring AI + Vue3 技术栈，支持多种 AI 模型（智谱 AI、Ollama 本地模型），实现文档的智能解析、向量存储和语义检索问答。

### 核心特性

- **高级 RAG 架构**：多路召回 + 重排序 + HyDE，检索精度提升显著
- **知识图谱增强**：Neo4j 知识图谱构建实体关系，辅助检索
- **多级缓存策略**：Caffeine 本地缓存 + Redis 分布式缓存，性能大幅提升
- **异步消息处理**：RabbitMQ 消息队列削峰，支持高并发
- **对话智能压缩**：长对话自动摘要，节省 Token 消耗

---

## 技术架构

### 系统架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                  用户层                                       │
│                          ┌──────────────────┐                                │
│                          │   Vue3 前端界面   │                                │
│                          └────────┬─────────┘                                │
└───────────────────────────────────┼──────────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼──────────────────────────────────────────┐
│                               服务层                                          │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                    Spring Boot Application                              │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌───────────┐│ │
│  │  │  Chat API    │  │Document API  │  │  Search API  │  │  KG API   ││ │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └─────┬─────┘│ │
│  │         │                   │                   │                  │      │ │
│  │  ┌──────▼───────────────────▼───────────────────▼──────────────────▼────┐│ │
│  │  │                          核心服务层                                       ││ │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             ││ │
│  │  │  │  对话服务     │  │  文档服务     │  │  检索服务     │  ...        ││ │
│  │  │  └──────────────┘  └──────────────┘  └──────────────┘             ││ │
│  │  └────────────────────────────────────────────────────────────────────────┘│ │
│  │  ┌────────────────────────────────────────────────────────────────────────┐│ │
│  │  │                      基础设施层                                           ││ │
│  │  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            ││ │
│  │  │  │ 缓存层    │  │ 消息队列  │  │ AI网关    │  │ 监控层    │  ...       ││ │
│  │  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘            ││ │
│  │  └────────────────────────────────────────────────────────────────────────┘│ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────┼──────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ↓               ↓               ↓
            ┌───────────┐   ┌───────────┐   ┌───────────┐
            │  PostgreSQL │   │   Neo4j   │   │   MinIO   │
            │  + pgvector │   │(知识图谱)  │   │(对象存储) │
            └───────────┘   └───────────┘   └───────────┘
                    ↓               ↓
            ┌───────────┐   ┌───────────┐
            │   Redis   │   │ RabbitMQ  │
            │  (缓存)    │   │  (队列)   │
            └───────────┘   └───────────┘
```

---

## 技术栈

### 后端技术栈 (knowledge-base/)

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.2.0 | 应用框架 |
| Spring AI | 1.1.2 | AI 开发框架 |
| Spring Data JPA | - | 数据持久层 |
| PostgreSQL | 14+ | 关系型数据库 |
| pgvector | latest | 向量扩展插件 |
| Neo4j | - | 知识图谱数据库 |
| MinIO | latest | 对象存储 |
| Redis | - | 分布式缓存 |
| RabbitMQ | - | 消息队列 |
| Caffeine | - | 本地缓存 |
| Lombok | - | 代码简化工具 |
| MapStruct | 1.5.5 | 对象映射工具 |
| Knife4j | 4.4.0 | API 文档 |

### 前端技术栈 (knowledge-base-ui/)

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.30 | 前端框架 |
| TypeScript | 5.9.3 | 类型支持 |
| Element Plus | 2.13.6 | UI 组件库 |
| Vite | 7.3.1 | 构建工具 |
| Vue Router | 4.6.4 | 路由管理 |
| Axios | 1.13.6 | HTTP 客户端 |
| Marked | 17.0.5 | Markdown 解析 |
| Highlight.js | 11.11.1 | 代码高亮 |
| @js-preview/* | 1.0.3 | 文档预览组件 |

### AI 模型支持

- **智谱 AI (Zhipu AI)**: GLM-4 系列模型
- **Ollama**: 支持本地部署的开源模型（如 DeepSeek、Llama 等）

---

## 功能特性

### 🏗️ 知识库管理

- 创建、编辑、删除知识库
- 知识库列表分页查询
- 支持关键词搜索

### 📄 文档管理

- 支持多种文档格式上传（PDF、Word、TXT、Markdown 等）
- 文档自动解析和向量化
- 智能语义分块（保留语义边界）
- 文档在线预览（PDF、Word、Excel、PPT）
- 文档与知识库关联管理
- MinIO 对象存储支持
- RabbitMQ 异步处理队列

### 🤖 智能问答

- 基于知识库的 RAG 问答
- 支持流式输出 (SSE)
- 多轮对话会话管理
- 历史消息记录
- 引用来源展示
- 对话自动摘要压缩

### 🔍 高级检索

- **多路召回**：向量检索 + BM25 全文检索
- **RRF 融合排序**：倒数排名融合算法
- **Cross-Encoder 重排序**：轻量级模型精排
- **HyDE**：假设文档生成增强检索
- **查询改写**：LLM 智能改写用户查询
- **知识图谱检索**：实体关系辅助检索

### 💾 缓存策略

- **L1 本地缓存**：Caffeine 缓存 Embedding 和热点 Query
- **L2 分布式缓存**：Redis 缓存向量检索结果和对话历史
- **缓存监控**：实时监控缓存命中率和统计信息

### 🕸️ 知识图谱

- 自动从文档中提取实体和关系
- Neo4j 图数据库存储
- 知识图谱辅助检索
- 实体关系可视化

---

## 系统截图

### 知识库管理
知识库列表页面，支持创建、编辑、删除知识库，以及关键词搜索功能。
![知识库管理](https://gitee.com/snrt111/blog-images/raw/master/blog/20260327123100578.png)

### 文档管理
文档上传和管理页面，支持多种格式文档的上传、解析和向量化处理。
![文档管理](https://gitee.com/snrt111/blog-images/raw/master/blog/20260327122736192.png)
文档预览
![文档预览](https://gitee.com/snrt111/blog-images/raw/master/blog/20260327122915217.png)

### 智能问答
AI 智能问答界面，支持基于知识库的 RAG 问答，流式输出显示，多轮对话会话管理。

![智能问答](https://gitee.com/snrt111/blog-images/raw/master/blog/20260327120745122.png)

---

## 项目结构

```
knowledge-base/
├── knowledge-base/                 # 后端项目
│   ├── src/main/java/com/snrt/knowledgebase/
│   │   ├── common/                 # 公共模块
│   │   │   ├── aspect/             # AOP 切面
│   │   │   ├── constants/          # 常量定义
│   │   │   ├── enums/              # 枚举类
│   │   │   ├── exception/          # 异常处理
│   │   │   ├── response/           # 统一响应
│   │   │   └── util/               # 工具类
│   │   ├── config/                 # 配置类
│   │   ├── controller/             # 控制器层
│   │   ├── domain/                 # 领域层（DDD 架构）
│   │   │   ├── chat/               # 对话领域
│   │   │   │   ├── dto/            # 数据传输对象
│   │   │   │   ├── entity/         # 实体类
│   │   │   │   ├── repository/     # 数据访问层
│   │   │   │   └── service/        # 业务逻辑层
│   │   │   ├── document/           # 文档领域
│   │   │   ├── knowledge/          # 知识库领域
│   │   │   └── knowledgegraph/     # 知识图谱领域
│   │   └── infrastructure/         # 基础设施层
│   │       ├── knowledgegraph/     # 知识图谱基础设施
│   │       ├── messaging/          # 消息队列
│   │       ├── retrieval/          # 检索服务
│   │       └── storage/            # 存储服务
│   ├── src/main/resources/
│   │   ├── application.yaml        # 主配置文件
│   │   ├── application-dev.yaml    # 开发环境配置
│   │   └── application-prod.yaml   # 生产环境配置
│   ├── docker/
│   │   └── pgvector/
│   │       ├── init.sql            # 数据库初始化脚本
│   │       └── migration/          # 数据库迁移脚本
│   ├── docker-compose.yml          # Docker Compose 配置
│   └── pom.xml                     # Maven 配置
│
└── knowledge-base-ui/              # 前端项目
    ├── src/
    │   ├── api/                    # API 接口
    │   ├── components/             # 公共组件
    │   ├── composables/            # Vue3 组合式函数
    │   ├── layouts/                # 布局组件
    │   ├── router/                 # 路由配置
    │   ├── types/                  # TypeScript 类型
    │   ├── utils/                  # 工具函数
    │   ├── views/                  # 页面视图
    │   ├── App.vue                 # 根组件
    │   └── main.ts                 # 入口文件
    ├── package.json                # NPM 配置
    └── vite.config.ts              # Vite 配置
```

---

## 快速开始

### 环境要求

- JDK 17+
- Node.js 20.19.0+ 或 22.12.0+
- PostgreSQL 14+ (带 pgvector 扩展)
- Neo4j 4.4+ (可选，知识图谱功能)
- Redis 6.0+
- RabbitMQ 3.8+
- Maven 3.8+
- Docker & Docker Compose (可选)

### 方式一：Docker 部署（推荐）

1. **启动基础设施服务**（PostgreSQL + Redis + RabbitMQ + MinIO）
```bash
cd knowledge-base
docker-compose up -d
```

2. **启动后端服务**
```bash
./mvnw spring-boot:run
# 或
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. **启动前端服务**
```bash
cd knowledge-base-ui
npm install
npm run dev
```

访问地址：
- 前端页面：http://localhost:5173
- 后端 API：http://localhost:8080
- API 文档：http://localhost:8080/doc.html
- MinIO 控制台：http://localhost:9001
- Neo4j 浏览器：http://localhost:7474

### 方式二：本地部署

#### 1. 数据库准备

安装 PostgreSQL 并启用 pgvector 扩展：

```sql
CREATE DATABASE knowledge_base;
\c knowledge_base
CREATE EXTENSION IF NOT EXISTS vector;
```

#### 2. 后端配置

编辑 `knowledge-base/src/main/resources/application-dev.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/knowledge_base
    username: your_username
    password: your_password
  ai:
    zhipuai:
      api-key: your_api_key_here
    ollama:
      base-url: http://localhost:11434  # 如使用本地模型
  data:
    redis:
      host: localhost
      port: 6379
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: your_password
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

启动后端：
```bash
cd knowledge-base
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 3. 前端配置

编辑 `knowledge-base-ui/.env`（如需要修改 API 地址）：

```bash
cd knowledge-base-ui
npm install
npm run dev
```

---

## 配置说明

### Ollama 安装与模型下载

本项目默认通过 Ollama 在本地调用对话模型与 Embedding 模型。使用前请先安装 Ollama，并拉取与 `application.yaml` 中 `spring.ai.ollama` 配置一致的模型（默认：`deepseek-r1:1.5b`、`nomic-embed-text`）。

#### 安装

1. 前往 [Ollama 官网下载页](https://ollama.com/download)，按操作系统获取安装包或安装命令。

```
Ollama0.6.1.exe /DIR="D:\tools\ollama"
```
2. **Windows / macOS**：运行安装程序；安装完成后 Ollama 一般会作为后台服务运行（默认监听 `http://127.0.0.1:11434`）。
3. **Linux**：可用官方一键脚本（需可访问 ollama.com）：

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

4. 在终端执行 `ollama --version`，确认已安装成功。

#### 下载（拉取）模型

在终端执行 `ollama pull &lt;模型名&gt;`，首次运行会从网络下载模型到本机。与本仓库默认配置对应的命令为：

```bash
ollama pull deepseek-r1:1.5b
ollama pull nomic-embed-text
```

- **对话模型**（`spring.ai.ollama.chat.options.model`）：上例为 `deepseek-r1:1.5b`，可按机器配置替换为 [Ollama 模型库](https://ollama.com/library) 中其他名称，并同步修改 `application.yaml`。
- **向量模型**（`spring.ai.ollama.embedding.options.model`）：须与 `spring.ai.vectorstore.pgvector.dimensions` 一致；当前默认 `nomic-embed-text` 为 **768** 维，若更换 Embedding 模型，请一并调整向量维度配置。

查看已安装模型：

```bash
ollama list
```

快速验证对话是否正常：

```bash
ollama run deepseek-r1:1.5b "你好"
```

完成后确保本机 `http://localhost:11434` 可访问，并与下文 **Ollama 本地模型配置** 中的 `base-url` 一致。

### AI 模型配置

系统支持两种 AI 模型配置方式，可在 `application-dev.yaml` 或 `application-prod.yaml` 中切换：

#### 智谱 AI 配置
```yaml
spring:
  ai:
    zhipuai:
      api-key: ${ZHIPUAI_API_KEY}
      chat:
        options:
          model: glm-4.7-flash
          temperature: 0.7

chat:
  model:
    provider: zhipuai
```

#### Ollama 本地模型配置
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: deepseek-r1:1.5b
          temperature: 0.7
      embedding:
        options:
          model: nomic-embed-text

chat:
  model:
    provider: ollama
```

### 向量存储配置

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true        # 自动创建表（生产环境建议关闭）
        table-name: vector_store       # 向量表名
        index-type: HNSW              # 索引类型
        distance-type: COSINE_DISTANCE # 距离计算方式
        dimensions: 768               # 向量维度
        max-document-batch-size: 10000 # 批量处理大小
```

### 检索优化配置

```yaml
retrieval:
  # 多路召回配置
  multi-retriever:
    enabled: true
    vector-top-k: 20
    keyword-top-k: 10
    final-top-k: 10
  
  # 重排序配置
  reranker:
    enabled: true
    model: cross-encoder
    top-k: 5
  
  # HyDE 配置
  hyde:
    enabled: false
    num-hypothetical-docs: 3
  
  # 查询改写配置
  query-rewriter:
    enabled: false
```

### 缓存配置

```yaml
spring:
  cache:
    type: caffeine,redis
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=10m
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 10
```

### MinIO 对象存储配置

```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin123
  bucket-name: knowledge-base
```

### RabbitMQ 消息队列配置

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        concurrency: 5
        max-concurrency: 20
        prefetch: 1
```

### Neo4j 知识图谱配置

```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: neo4j
```

---

## API 文档

启动后端服务后，访问 Knife4j UI：

```
http://localhost:8080/doc.html
```

## 主要接口

### 知识库管理
- `GET /api/knowledge-base` - 获取知识库列表
- `POST /api/knowledge-base` - 创建知识库
- `PUT /api/knowledge-base/{id}` - 更新知识库
- `DELETE /api/knowledge-base/{id}` - 删除知识库

### 文档管理
- `GET /api/documents` - 获取文档列表
- `POST /api/documents/upload` - 上传文档
- `GET /api/documents/{id}/preview` - 预览文档
- `DELETE /api/documents/{id}` - 删除文档

### 智能问答
- `GET /api/chat/sessions` - 获取会话列表
- `POST /api/chat/sessions` - 创建会话
- `DELETE /api/chat/sessions/{id}` - 删除会话
- `POST /api/chat` - 发送消息（非流式）
- `GET /api/chat/stream` - 流式对话（SSE）

### 知识图谱
- `GET /api/knowledge-graph` - 获取知识图谱列表
- `POST /api/knowledge-graph` - 创建知识图谱
- `GET /api/knowledge-graph/{id}/graph` - 获取图谱数据
- `POST /api/knowledge-graph/sync` - 同步文档到知识图谱

### 缓存监控
- `GET /api/cache/stats` - 获取缓存统计信息
- `POST /api/cache/clear` - 清除缓存

---

## 核心功能说明

### 🔍 多路召回检索

系统采用向量检索 + BM25 全文检索的混合召回策略：

1. **向量检索**：基于语义相似度，召回 20 个候选文档
2. **BM25 全文检索**：基于关键词匹配，召回 10 个候选文档
3. **RRF 融合**：使用倒数排名融合算法合并结果
4. **降级策略**：任意检索失败时，自动降级为仅向量检索

### 📊 Cross-Encoder 重排序

使用轻量级 Cross-Encoder 模型对初步召回结果进行精细排序，提升 TopK 准确率。

### 🧠 HyDE (Hypothetical Document Embedding)

通过 LLM 生成假设答案文档，再对假设文档进行检索，可显著提升检索质量。

### 🔄 查询改写

使用 LLM 智能改写用户查询，扩展同义词和相关词，提升召回率。

### 🕸️ 知识图谱增强

1. 从文档中自动提取实体和关系
2. 构建 Neo4j 知识图谱
3. 检索时结合知识图谱，提供更丰富的上下文信息

### 💬 对话摘要压缩

当历史消息超过 6 轮时，自动使用轻量级模型生成摘要，保留最近 2 轮完整对话，大幅节省 Token 消耗。

### 💾 多级缓存

- **L1 缓存**：Caffeine 本地缓存，存储 Embedding 结果和热点 Query，TTL 10 分钟
- **L2 缓存**：Redis 分布式缓存，存储向量检索结果和对话历史，TTL 30 分钟
- **缓存监控**：实时统计缓存命中率，支持手动清除缓存

### 📨 消息队列处理

文档上传后通过 RabbitMQ 异步处理：
- 支持并发消费（5-20 个消费者）
- 消息持久化
- 失败重试机制

---

## 开发说明

### 代码规范

- 后端遵循阿里巴巴 Java 开发手册
- 前端遵循 Vue3 + TypeScript 官方风格指南
- 采用 DDD（领域驱动设计）架构
- 每个文件最大行数限制：500 行（不含配置文件）

### 分支管理

- `main`: 主分支，稳定版本
- `develop`: 开发分支
- `feature/*`: 功能分支

---

## 生产部署

### 环境变量配置

生产环境建议使用环境变量配置敏感信息：

```bash
# 数据库配置
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=knowledge_base
export DB_USERNAME=postgres
export DB_PASSWORD=your_password

# Redis 配置
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_password

# RabbitMQ 配置
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest

# Neo4j 配置
export NEO4J_URI=bolt://localhost:7687
export NEO4J_USERNAME=neo4j
export NEO4J_PASSWORD=your_password

# AI 模型配置
export ZHIPUAI_API_KEY=your_api_key
export CHAT_MODEL_PROVIDER=zhipuai

# MinIO 配置
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin123
```

### 后端打包部署

```bash
cd knowledge-base
mvn clean package -P prod
java -jar target/knowledge-base-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 前端打包部署

```bash
cd knowledge-base-ui
npm run build
```

打包后的文件位于 `dist/` 目录，可部署到 Nginx 等 Web 服务器。

#### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/knowledge-base-ui/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote.remote-addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # SSE 流式输出配置
    location /api/chat/stream {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote.remote-addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding off;
    }
}
```

---

## 常见问题

### 1. pgvector 扩展未安装

确保 PostgreSQL 已安装 pgvector 扩展：
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. AI 模型调用失败

- 检查 API Key 是否正确配置
- 确认网络可以访问智谱 AI 服务
- 如使用 Ollama，确保服务已启动并模型已下载
- 检查 `chat.model.provider` 配置是否正确

### 3. 文档解析失败

- 确保上传的文档格式受支持（PDF、Word、TXT、Markdown）
- 检查文件是否损坏
- 确认 Tika 文档解析器依赖已正确引入

### 4. MinIO 连接失败

- 检查 MinIO 服务是否已启动
- 确认 endpoint、access-key、secret-key 配置正确
- 检查 bucket 是否已创建

### 5. 向量检索无结果

- 确认文档已成功解析并生成向量
- 检查向量维度配置是否匹配（默认 768 维）
- 验证 pgvector 扩展是否正确安装

### 6. RabbitMQ 连接失败

- 检查 RabbitMQ 服务是否已启动
- 确认连接配置（host、port、username、password）正确
- 检查虚拟主机配置

### 7. Redis 连接失败

- 检查 Redis 服务是否已启动
- 确认连接配置正确
- 验证密码认证

### 8. Neo4j 连接失败

- 检查 Neo4j 服务是否已启动
- 确认 URI 和认证信息正确
- 如不需要知识图谱功能，可禁用相关配置

---

## 技术亮点

- 采用 Spring AI 框架简化 AI 功能开发
- 高级 RAG 架构：多路召回 + 重排序 + HyDE
- 知识图谱增强检索
- 使用 pgvector 实现高效的向量检索
- 支持多种 AI 模型灵活切换
- 前后端分离，接口 RESTful 设计
- 支持流式输出，提升用户体验
- 文档在线预览功能
- 对象存储支持大文件管理
- 多级缓存策略（Caffeine + Redis）
- RabbitMQ 异步消息处理
- 对话自动摘要压缩
- 完整的监控体系

---

## 许可证

[MIT License](LICENSE)

---

## 贡献指南

欢迎提交 Issue 和 Pull Request。

---

## 联系方式

如有问题，请通过 Issue 联系。

---

## 附录

### 相关文档

- [Docker Compose 说明](./knowledge-base/docker-compose-README.md)

### 核心代码文件

| 文件 | 说明 |
|------|------|
| `MultiRetriever.java` | 多路召回检索器 |
| `CrossEncoderReranker.java` | Cross-Encoder 重排序 |
| `HydeService.java` | HyDE 假设文档生成 |
| `QueryRewriterService.java` | 查询改写服务 |
| `ConversationSummarizer.java` | 对话摘要压缩 |
| `RAGCacheManager.java` | 多级缓存管理 |
| `SemanticDocumentSplitter.java` | 智能语义分块 |
| `DocumentProcessProducer.java` | 文档处理消息生产者 |
| `KnowledgeGraphService.java` | 知识图谱服务 |
