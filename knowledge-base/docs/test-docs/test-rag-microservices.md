# 微服务架构设计与实践

## 1. 微服务架构概述

微服务架构是一种将单一应用程序划分为一组小型服务的方法，每个服务运行在自己的进程中，服务之间通过轻量级机制（通常是 HTTP API）进行通信。

### 1.1 微服务 vs 单体架构

| 特性 | 单体架构 | 微服务架构 |
|------|----------|------------|
| 代码库 | 单一 | 多个独立 |
| 部署 | 整体部署 | 独立部署 |
| 扩展 | 整体扩展 | 按需扩展 |
| 技术栈 | 统一 | 多样化 |
| 复杂度 | 代码复杂，部署简单 | 代码简单，部署复杂 |

### 1.2 微服务的优势

- **独立部署**：每个服务可以独立开发、部署和扩展
- **技术异构**：不同服务可以使用不同的技术栈
- **故障隔离**：单个服务故障不会影响整个系统
- **团队自治**：小团队负责单个服务，提高开发效率

### 1.3 微服务的挑战

- **分布式复杂性**：网络延迟、分布式事务、服务发现等
- **运维复杂度**：需要完善的监控、日志、追踪系统
- **数据一致性**：分布式数据管理困难
- **测试复杂度**：集成测试和端到端测试复杂

## 2. 服务拆分策略

### 2.1 拆分原则

1. **单一职责原则**：每个服务只负责一个业务领域
2. **高内聚低耦合**：服务内部功能紧密相关，服务间依赖最小化
3. **业务边界清晰**：按照业务领域边界划分服务
4. **数据独立性**：每个服务管理自己的数据

### 2.2 拆分方法

#### 按业务领域拆分（DDD）

```
电商系统示例：
├── 用户服务 (User Service)
├── 商品服务 (Product Service)
├── 订单服务 (Order Service)
├── 库存服务 (Inventory Service)
├── 支付服务 (Payment Service)
└── 物流服务 (Logistics Service)
```

#### 按功能拆分

```
内容管理系统示例：
├── 内容服务 (Content Service)
├── 评论服务 (Comment Service)
├── 推荐服务 (Recommendation Service)
├── 搜索服务 (Search Service)
└── 通知服务 (Notification Service)
```

## 3. 服务通信

### 3.1 同步通信

#### RESTful API

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public Order createOrder(@RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }
}
```

#### gRPC

```protobuf
syntax = "proto3";

service OrderService {
    rpc CreateOrder (OrderRequest) returns (OrderResponse);
    rpc GetOrder (OrderId) returns (Order);
}

message OrderRequest {
    string user_id = 1;
    repeated OrderItem items = 2;
}

message OrderResponse {
    string order_id = 1;
    double total_amount = 2;
}
```

### 3.2 异步通信

#### 消息队列

```java
@Component
public class OrderEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        rabbitTemplate.convertAndSend("order.exchange", "order.created", event);
    }
}

@Component
public class InventoryEventListener {

    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 扣减库存
        inventoryService.deductStock(event.getOrder());
    }
}
```

#### 事件驱动架构

```java
// 领域事件
public class OrderCreatedEvent extends DomainEvent {
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}

// 事件处理器
@Component
public class OrderCreatedHandler {

    @EventListener
    public void handle(OrderCreatedEvent event) {
        // 发送通知
        notificationService.sendOrderConfirmation(event);
        // 更新统计
        analyticsService.trackOrder(event);
    }
}
```

## 4. 服务发现与注册

### 4.1 Eureka 服务注册中心

```yaml
# Eureka Server 配置
server:
  port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
```

```java
// 服务注册
@SpringBootApplication
@EnableEurekaClient
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### 4.2 Consul 服务发现

```yaml
spring:
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        service-name: order-service
        health-check-interval: 10s
```

## 5. 负载均衡

### 5.1 客户端负载均衡（Ribbon）

```java
@Configuration
public class RibbonConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
public class OrderService {

    @Autowired
    private RestTemplate restTemplate;

    public User getUser(String userId) {
        // 自动负载均衡
        return restTemplate.getForObject(
            "http://user-service/api/users/{id}",
            User.class,
            userId
        );
    }
}
```

### 5.2 Spring Cloud LoadBalancer

```java
@Configuration
public class LoadBalancerConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}

@Service
public class ProductService {

    @Autowired
    private WebClient.Builder webClientBuilder;

    public Mono<Product> getProduct(String productId) {
        return webClientBuilder.build()
            .get()
            .uri("http://product-service/api/products/{id}", productId)
            .retrieve()
            .bodyToMono(Product.class);
    }
}
```

## 6. 服务网关

### 6.1 Spring Cloud Gateway

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: orderServiceCircuitBreaker
                fallbackUri: forward:/fallback
```

### 6.2 网关过滤器

```java
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<Config> {

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = request.getHeaders().getFirst("Authorization");

            if (token == null || !isValidToken(token)) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            return chain.filter(exchange);
        };
    }
}
```

## 7. 熔断与限流

### 7.1 Resilience4j 熔断器

```java
@Configuration
public class Resilience4jConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slidingWindowSize(10)
                .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(4))
                .build())
            .build());
    }
}

@Service
public class PaymentService {

    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(PaymentRequest request) {
        // 调用支付接口
        return paymentClient.charge(request);
    }

    public PaymentResult fallbackPayment(PaymentRequest request, Exception ex) {
        // 降级处理
        return PaymentResult.failed("Payment service unavailable");
    }
}
```

### 7.2 限流

```java
@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<Config> {

    private final RateLimiter rateLimiter = RateLimiter.create(100.0); // 每秒100个请求

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!rateLimiter.tryAcquire()) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }
}
```

## 8. 分布式事务

### 8.1 Saga 模式

```java
// Saga 编排器
@Component
public class OrderSaga {

    @Autowired
    private OrderService orderService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private PaymentService paymentService;

    public void execute(CreateOrderRequest request) {
        try {
            // 1. 创建订单
            Order order = orderService.createOrder(request);

            // 2. 扣减库存
            inventoryService.deductStock(order);

            // 3. 处理支付
            paymentService.processPayment(order);

            // 4. 确认订单
            orderService.confirmOrder(order.getId());

        } catch (Exception e) {
            // 补偿操作
            compensate(request);
        }
    }

    private void compensate(CreateOrderRequest request) {
        // 执行补偿操作
    }
}
```

### 8.2 Seata 分布式事务

```java
@Service
public class BusinessService {

    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public void purchase(String userId, String commodityCode, int orderCount) {
        // 扣减库存
        storageService.deduct(commodityCode, orderCount);

        // 创建订单
        orderService.create(userId, commodityCode, orderCount);

        // 扣减余额
        accountService.debit(userId, orderCount * price);
    }
}
```

## 9. 监控与追踪

### 9.1 Micrometer + Prometheus

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 9.2 Zipkin 分布式追踪

```yaml
spring:
  zipkin:
    base-url: http://localhost:9411
  sleuth:
    sampler:
      probability: 1.0
```

```java
@Service
public class OrderService {

    @NewSpan("create-order")
    public Order createOrder(@SpanTag("userId") String userId, OrderRequest request) {
        // 业务逻辑
    }
}
```

## 10. 容器化与编排

### 10.1 Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/order-service.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 10.2 Kubernetes 部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: order-service:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
---
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

---

本文档全面介绍了微服务架构的设计原则、服务拆分策略、通信方式、服务发现、负载均衡、网关、熔断限流、分布式事务、监控追踪以及容器化部署等内容，是微服务开发的完整指南。
