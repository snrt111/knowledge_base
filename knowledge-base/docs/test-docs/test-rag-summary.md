# RAG 测试文档汇总

## 文档列表

本目录包含以下测试文档，用于测试知识库系统的 RAG（检索增强生成）功能：

### 1. Spring Boot 核心技术详解
**文件名**: `test-rag-spring-boot.md`

**内容概述**:
- Spring Boot 自动配置原理
- Spring Boot Starter 机制
- 配置文件管理
- 数据访问（JPA、MyBatis）
- Web 开发（RESTful API、异常处理）
- 安全控制（Spring Security、JWT）
- 性能优化
- 部署与监控

**测试场景**:
- 询问 Spring Boot 自动配置原理
- 询问如何实现 RESTful API
- 询问 Spring Security 配置

---

### 2. 微服务架构设计与实践
**文件名**: `test-rag-microservices.md`

**内容概述**:
- 微服务架构概述与拆分策略
- 服务通信（同步/异步）
- 服务发现与注册（Eureka、Consul）
- 负载均衡
- 服务网关（Spring Cloud Gateway）
- 熔断与限流（Resilience4j）
- 分布式事务（Saga、Seata）
- 监控与追踪
- 容器化与编排

**测试场景**:
- 询问微服务拆分原则
- 询问服务间通信方式
- 询问分布式事务解决方案

---

### 3. Vue 3 完全指南
**文件名**: `test-rag-vue3-guide.md`

**内容概述**:
- Vue 3 新特性与组合式 API
- setup 函数与 script setup
- 响应式基础（ref、reactive）
- 计算属性与侦听器
- 组件系统（Props、事件、插槽）
- 组合式函数（Composables）
- 状态管理（Pinia）
- 路由（Vue Router 4）
- 动画与过渡

**测试场景**:
- 询问 Vue 3 组合式 API
- 询问组件通信方式
- 询问状态管理方案

---

### 4. Docker 与 Kubernetes 实战指南
**文件名**: `test-rag-docker-k8s.md`

**内容概述**:
- Docker 基础与核心概念
- Dockerfile 编写与多阶段构建
- Docker Compose 编排
- Kubernetes 架构与核心资源
- Pod、Deployment、Service 配置
- ConfigMap 和 Secret
- Ingress 配置
- Helm 包管理
- CI/CD 集成

**测试场景**:
- 询问 Docker 常用命令
- 询问 Kubernetes 资源对象
- 询问 Helm Chart 结构

---

### 5. 数据库设计与优化指南
**文件名**: `test-rag-database-guide.md`

**内容概述**:
- 数据库设计原则与范式
- MySQL 索引优化
- SQL 优化技巧
- 表结构优化（分区、分表）
- PostgreSQL 高级特性（JSON/JSONB、全文搜索）
- 窗口函数与递归查询
- 数据库性能监控
- 数据迁移与备份

**测试场景**:
- 询问数据库设计范式
- 询问 SQL 优化方法
- 询问 PostgreSQL JSON 操作

---

### 6. Java 并发编程实战
**文件名**: `test-rag-java-concurrent.md`

**内容概述**:
- 线程创建与管理
- 线程同步机制（synchronized、Lock）
- 原子类（AtomicInteger、LongAdder）
- 并发集合（ConcurrentHashMap、BlockingQueue）
- 并发工具类（CountDownLatch、CyclicBarrier、Semaphore）
- CompletableFuture 异步编程
- Fork/Join 框架
- 线程安全最佳实践

**测试场景**:
- 询问线程池配置
- 询问锁的类型与选择
- 询问 CompletableFuture 用法

---

### 7. React Hooks 完全指南
**文件名**: `test-rag-react-hooks.md`

**内容概述**:
- Hooks 简介与规则
- 基础 Hooks（useState、useEffect、useContext）
- 额外 Hooks（useReducer、useCallback、useMemo、useRef）
- 自定义 Hooks（useLocalStorage、useFetch、useDebounce）
- Hooks 性能优化

**测试场景**:
- 询问 useEffect 依赖管理
- 询问 useCallback 和 useMemo 区别
- 询问自定义 Hooks 编写

---

## 测试建议

### 单文档检索测试
1. 针对每个文档提出 3-5 个具体问题
2. 验证答案是否来自正确的文档
3. 检查答案的准确性和完整性

### 跨文档检索测试
1. 提出涉及多个技术栈的问题
   - 例如："如何在 Spring Boot 中集成 Vue 3 前端？"
   - 例如："Docker 容器中的 Java 应用如何连接 MySQL？"

### 边界测试
1. 提出模糊问题，测试检索准确性
2. 提出文档中不存在的问题，测试拒绝回答能力
3. 测试长文档的分块和检索效果

### 性能测试
1. 测试大量文档下的检索速度
2. 测试并发查询的响应时间

## 示例测试问题

### Spring Boot
- Spring Boot 的自动配置是如何实现的？
- 如何自定义一个 Spring Boot Starter？
- Spring Boot 中如何配置多环境？

### 微服务
- 微服务拆分的原则有哪些？
- 服务间通信有哪些方式？
- 如何解决分布式事务问题？

### Vue 3
- Vue 3 的组合式 API 有什么优势？
- ref 和 reactive 有什么区别？
- 如何实现组件间的状态共享？

### Docker/K8s
- Docker 和虚拟机的区别是什么？
- Kubernetes 中 Pod 和 Deployment 有什么区别？
- 如何编写一个 Helm Chart？

### 数据库
- 数据库设计的三大范式是什么？
- 如何优化慢查询？
- PostgreSQL 的 JSONB 类型有什么优势？

### Java 并发
- Java 中有哪些线程同步机制？
- 线程池的核心参数有哪些？
- CompletableFuture 如何使用？

### React Hooks
- useEffect 的依赖数组有什么作用？
- useCallback 和 useMemo 有什么区别？
- 如何编写自定义 Hook？

---

## 文档统计

| 文档 | 主题 | 预估字数 |
|------|------|----------|
| test-rag-spring-boot.md | Spring Boot | ~8000 |
| test-rag-microservices.md | 微服务架构 | ~10000 |
| test-rag-vue3-guide.md | Vue 3 | ~9000 |
| test-rag-docker-k8s.md | Docker/K8s | ~8500 |
| test-rag-database-guide.md | 数据库 | ~8000 |
| test-rag-java-concurrent.md | Java 并发 | ~7500 |
| test-rag-react-hooks.md | React Hooks | ~7000 |

**总计**: 约 58000 字，涵盖 7 个技术领域

---

本文档汇总了所有 RAG 测试文档的信息，可用于验证知识库系统的文档检索和问答功能。
