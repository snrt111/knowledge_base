# Spring Boot 核心技术详解

## 1. Spring Boot 简介

Spring Boot 是 Spring 框架的扩展，旨在简化 Spring 应用的初始搭建和开发过程。它采用"约定优于配置"的理念，让开发者能够快速创建独立运行的、生产级别的 Spring 应用。

### 1.1 核心特性

- **自动配置**：根据 classpath 中的依赖自动配置 Spring 应用
- **起步依赖**：简化 Maven 配置，通过 starter 快速集成各种技术栈
- **内嵌服务器**：内置 Tomcat、Jetty 或 Undertow，无需部署 WAR 文件
- **Actuator 监控**：提供生产就绪功能，如健康检查、指标监控等

### 1.2 快速开始

创建一个简单的 Spring Boot 应用：

```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

## 2. 自动配置原理

Spring Boot 的自动配置通过 `@EnableAutoConfiguration` 注解实现，它会扫描 classpath 下的 `META-INF/spring.factories` 文件，加载其中定义的自动配置类。

### 2.1 条件注解

Spring Boot 提供了多种条件注解来控制配置的生效条件：

| 注解 | 说明 |
|------|------|
| `@ConditionalOnClass` | 当类存在时生效 |
| `@ConditionalOnMissingBean` | 当 Bean 不存在时生效 |
| `@ConditionalOnProperty` | 当配置属性满足条件时生效 |
| `@ConditionalOnWebApplication` | 当是 Web 应用时生效 |

### 2.2 自定义自动配置

```java
@Configuration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyProperties.class)
public class MyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyProperties properties) {
        return new MyService(properties);
    }
}
```

## 3. Spring Boot Starter

Starter 是 Spring Boot 的核心概念之一，它将相关的依赖和配置打包在一起，简化项目配置。

### 3.1 常用 Starter

- `spring-boot-starter-web`：Web 应用开发
- `spring-boot-starter-data-jpa`：JPA 数据访问
- `spring-boot-starter-security`：安全控制
- `spring-boot-starter-test`：测试支持
- `spring-boot-starter-actuator`：监控和管理

### 3.2 创建自定义 Starter

1. 创建自动配置类
2. 创建 `META-INF/spring.factories` 文件
3. 注册自动配置类

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.MyAutoConfiguration
```

## 4. 配置文件

Spring Boot 支持多种格式的配置文件：

### 4.1 application.properties

```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/test
spring.datasource.username=root
spring.datasource.password=123456
```

### 4.2 application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test
    username: root
    password: 123456
```

### 4.3 多环境配置

Spring Boot 支持通过 profile 区分不同环境：

- `application-dev.yaml`：开发环境
- `application-test.yaml`：测试环境
- `application-prod.yaml`：生产环境

激活指定环境：

```bash
java -jar app.jar --spring.profiles.active=prod
```

## 5. 数据访问

Spring Boot 简化了数据访问层的开发，支持多种数据访问技术。

### 5.1 Spring Data JPA

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByName(String name);
}
```

### 5.2 MyBatis 集成

添加依赖：

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.0</version>
</dependency>
```

配置：

```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.entity
```

## 6. Web 开发

Spring Boot 提供了强大的 Web 开发支持。

### 6.1 RESTful API

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> list() {
        return userService.findAll();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userService.save(user);
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        return userService.update(id, user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

### 6.2 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse error = new ErrorResponse(500, e.getMessage());
        return ResponseEntity.status(500).body(error);
    }
}
```

## 7. 安全控制

Spring Security 是 Spring Boot 的安全框架。

### 7.1 基本配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(withDefaults());
        return http.build();
    }
}
```

### 7.2 JWT 认证

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 86400000);

        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
}
```

## 8. 性能优化

### 8.1 连接池配置

推荐使用 HikariCP：

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
```

### 8.2 缓存配置

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("users", "orders");
    }
}
```

## 9. 部署与监控

### 9.1 打包部署

```bash
# Maven 打包
mvn clean package

# 运行
java -jar target/myapp.jar
```

### 9.2 Actuator 端点

常用端点：

- `/actuator/health`：健康检查
- `/actuator/info`：应用信息
- `/actuator/metrics`：指标数据
- `/actuator/loggers`：日志级别

## 10. 最佳实践

1. **合理划分模块**：按功能模块划分，保持单一职责
2. **统一异常处理**：使用全局异常处理器
3. **接口版本控制**：在 URL 或 Header 中包含版本信息
4. **日志规范**：使用 SLF4J + Logback，合理设置日志级别
5. **配置外部化**：敏感配置使用环境变量或配置中心
6. **健康检查**：配置 Actuator 健康检查端点
7. **优雅停机**：配置优雅停机，确保请求处理完成

---

本文档详细介绍了 Spring Boot 的核心技术，包括自动配置、Starter、数据访问、Web 开发、安全控制等方面，帮助开发者快速掌握 Spring Boot 开发技能。
