# 智能知识库系统 (Knowledge Base System)

基于 RAG（检索增强生成）技术的智能知识库问答系统，支持文档管理、向量化存储和 AI 智能问答。

## 项目简介

本项目是一个前后端分离的智能知识库系统，采用 Spring AI + Vue3 技术栈，支持多种 AI 模型（智谱 AI、Ollama 本地模型），实现文档的智能解析、向量存储和语义检索问答。

## 技术架构

### 后端技术栈 (knowledge-base/)

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.2.0 | 应用框架 |
| Spring AI | 1.1.2 | AI 开发框架 |
| Spring Data JPA | - | 数据持久层 |
| PostgreSQL | - | 关系型数据库 |
| pgvector | latest | 向量扩展插件 |
| Lombok | - | 代码简化工具 |
| SpringDoc OpenAPI | 2.3.0 | API 文档 |

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

### AI 模型支持

- **智谱 AI (Zhipu AI)**: GLM-4 系列模型
- **Ollama**: 支持本地部署的开源模型（如 DeepSeek、Llama 等）

## 功能特性

### 知识库管理
- 创建、编辑、删除知识库
- 知识库列表分页查询
- 支持关键词搜索

### 文档管理
- 支持多种文档格式上传（PDF、Word、TXT、Markdown 等）
- 文档自动解析和向量化
- 文档与知识库关联管理

### 智能问答
- 基于知识库的 RAG 问答
- 支持流式输出 (SSE)
- 多轮对话会话管理
- 历史消息记录

### 向量检索
- 基于 pgvector 的向量存储
- HNSW 索引加速相似度搜索
- 余弦距离计算

## 系统截图

### 知识库管理
知识库列表页面，支持创建、编辑、删除知识库，以及关键词搜索功能。

![知识库管理](https://gitee.com/snrt111/blog-images/raw/master/blog/20260326081054517.png)

### 文档管理
文档上传和管理页面，支持多种格式文档的上传、解析和向量化处理。

![文档管理](https://gitee.com/snrt111/blog-images/raw/master/blog/20260326081344322.png)

### 智能问答
AI 智能问答界面，支持基于知识库的 RAG 问答，流式输出显示，多轮对话会话管理。

![智能问答](https://gitee.com/snrt111/blog-images/raw/master/blog/20260326081503120.png)

## 项目结构

```
knowledge-base/
├── knowledge-base/                 # 后端项目
│   ├── src/main/java/com/snrt/knowledgebase/
│   │   ├── config/                 # 配置类
│   │   ├── controller/             # 控制器层
│   │   ├── dto/                    # 数据传输对象
│   │   ├── entity/                 # 实体类
│   │   ├── exception/              # 异常处理
│   │   ├── repository/             # 数据访问层
│   │   └── service/                # 业务逻辑层
│   ├── src/main/resources/
│   │   ├── application.yaml        # 主配置文件
│   │   ├── application-dev.yaml    # 开发环境配置
│   │   └── application-prod.yaml   # 生产环境配置
│   ├── docker/
│   │   └── pgvector/
│   │       └── init.sql            # 数据库初始化脚本
│   ├── uploads/documents/          # 文档上传目录
│   ├── docker-compose.yml          # Docker Compose 配置
│   └── pom.xml                     # Maven 配置
│
└── knowledge-base-ui/              # 前端项目
    ├── src/
    │   ├── api/                    # API 接口
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

## 快速开始

### 环境要求

- JDK 17+
- Node.js 20.19.0+ 或 22.12.0+
- PostgreSQL 14+ (带 pgvector 扩展)
- Maven 3.8+
- Docker & Docker Compose (可选)

### 方式一：Docker 部署（推荐）

1. 启动数据库服务
```bash
cd knowledge-base
docker-compose up -d
```

2. 启动后端服务
```bash
./mvnw spring-boot:run
# 或
mvn spring-boot:run
```

3. 启动前端服务
```bash
cd knowledge-base-ui
npm install
npm run dev
```

### 方式二：本地部署

#### 1. 数据库准备

安装 PostgreSQL 并启用 pgvector 扩展：

```sql
CREATE DATABASE knowledge_base;
\c knowledge_base
CREATE EXTENSION IF NOT EXISTS vector;
```

#### 2. 后端配置

编辑 `knowledge-base/src/main/resources/application.yaml`：

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
```

启动后端：
```bash
cd knowledge-base
mvn clean install
mvn spring-boot:run
```

#### 3. 前端配置

编辑 `knowledge-base-ui/.env`（如需要修改 API 地址）：

```bash
cd knowledge-base-ui
npm install
npm run dev
```

## 配置说明

### AI 模型配置

系统支持两种 AI 模型配置方式，可在 `application.yaml` 中切换：

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
```

### 向量存储配置

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true        # 自动创建表
        table-name: vector_store       # 向量表名
        index-type: HNSW              # 索引类型
        distance-type: COSINE_DISTANCE # 距离计算方式
        dimensions: 768               # 向量维度
```

## API 文档

启动后端服务后，访问 Swagger UI：

```
http://localhost:8080/swagger-ui.html
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
- `DELETE /api/documents/{id}` - 删除文档

### 智能问答
- `GET /api/chat/sessions` - 获取会话列表
- `POST /api/chat/sessions` - 创建会话
- `POST /api/chat` - 发送消息（非流式）
- `GET /api/chat/stream` - 流式对话（SSE）

## 开发说明

### 代码规范

- 后端遵循阿里巴巴 Java 开发手册
- 前端遵循 Vue3 + TypeScript 官方风格指南
- 每个文件最大行数限制：500 行（不含配置文件）

### 分支管理

- `main`: 主分支，稳定版本
- `develop`: 开发分支
- `feature/*`: 功能分支

## 生产部署

### 后端打包

```bash
cd knowledge-base
mvn clean package -P prod
java -jar target/knowledge-base-0.0.1-SNAPSHOT.jar
```

### 前端打包

```bash
cd knowledge-base-ui
npm run build
```

打包后的文件位于 `dist/` 目录，可部署到 Nginx 等 Web 服务器。

## 常见问题

### 1. pgvector 扩展未安装

确保 PostgreSQL 已安装 pgvector 扩展：
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. AI 模型调用失败

- 检查 API Key 是否正确配置
- 确认网络可以访问智谱 AI 服务
- 如使用 Ollama，确保服务已启动

### 3. 文档解析失败

确保上传的文档格式受支持，且文件未损坏。

## 技术亮点

- 采用 Spring AI 框架简化 AI 功能开发
- 使用 pgvector 实现高效的向量检索
- 支持多种 AI 模型灵活切换
- 前后端分离，接口 RESTful 设计
- 支持流式输出，提升用户体验

## 许可证

[MIT License](LICENSE)

## 贡献指南

欢迎提交 Issue 和 Pull Request。

## 联系方式

如有问题，请通过 Issue 联系。
