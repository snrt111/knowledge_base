# Docker Compose 配置说明

本项目的 docker-compose.yml 配置提供了开发环境所需的常用中间件。

## 包含的服务

### 1. PostgreSQL (端口: 5432)
- **版本**: 15-alpine
- **默认用户**: postgres
- **默认密码**: postgres
- **默认数据库**: knowledge_base
- **额外数据库**: test_db, cache_db
- **管理工具**: 可使用 pgAdmin 或 DBeaver 连接

### 2. MySQL (端口: 3306)
- **版本**: 8.0
- **Root密码**: root
- **应用用户**: kb_user
- **应用密码**: kb_password
- **默认数据库**: knowledge_base
- **额外数据库**: test_db, cache_db
- **管理工具**: 可使用 phpMyAdmin 或 MySQL Workbench 连接

### 3. Redis (端口: 6379)
- **版本**: 7-alpine
- **无密码**: 默认无密码配置（生产环境请配置密码）
- **管理工具**: 可使用 RedisInsight 连接

### 4. RabbitMQ (端口: 5672, 15672)
- **版本**: 3.12-management-alpine
- **AMQP端口**: 5672
- **管理界面**: http://localhost:15672
- **默认用户**: admin
- **默认密码**: admin

### 5. MinIO (端口: 9000, 9001)
- **版本**: latest
- **API端口**: 9000
- **控制台端口**: 9001
- **访问地址**: http://localhost:9001
- **默认用户**: minioadmin
- **默认密码**: minioadmin

## 使用方法

### 启动所有服务
```bash
docker-compose up -d
```

### 停止所有服务
```bash
docker-compose down
```

### 停止并删除所有数据卷
```bash
docker-compose down -v
```

### 查看服务状态
```bash
docker-compose ps
```

### 查看服务日志
```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs postgres
docker-compose logs mysql
docker-compose logs redis
docker-compose logs rabbitmq
docker-compose logs minio
```

### 重启某个服务
```bash
docker-compose restart postgres
```

## 多数据库配置说明

### PostgreSQL 多数据库配置

PostgreSQL 容器启动时会自动执行 `docker/postgres/init-db.sql` 脚本，该脚本会创建以下数据库：
- knowledge_base
- test_db
- cache_db

在 Spring Boot 应用中，可以通过以下方式连接不同的数据库：

**application.yaml 配置示例**:
```yaml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://localhost:5432/knowledge_base
      username: postgres
      password: postgres
    test:
      url: jdbc:postgresql://localhost:5432/test_db
      username: postgres
      password: postgres
    cache:
      url: jdbc:postgresql://localhost:5432/cache_db
      username: postgres
      password: postgres
```

### MySQL 多数据库配置

MySQL 容器启动时会自动执行 `docker/mysql/init-db.sql` 脚本，该脚本会创建以下数据库：
- knowledge_base
- test_db
- cache_db

在 Spring Boot 应用中，可以通过以下方式连接不同的数据库：

**application.yaml 配置示例**:
```yaml
spring:
  datasource:
    primary:
      url: jdbc:mysql://localhost:3306/knowledge_base?useSSL=false&serverTimezone=UTC
      username: kb_user
      password: kb_password
    test:
      url: jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC
      username: kb_user
      password: kb_password
    cache:
      url: jdbc:mysql://localhost:3306/cache_db?useSSL=false&serverTimezone=UTC
      username: kb_user
      password: kb_password
```

## 注意事项

1. **端口冲突**: 如果本地已经运行了相同端口的服务，请修改 docker-compose.yml 中的端口映射
2. **数据持久化**: 所有数据都存储在 Docker volumes 中，删除容器不会丢失数据
3. **安全性**: 当前配置仅适用于开发环境，生产环境请修改默认密码并启用安全配置
4. **资源限制**: 可根据实际需求在 docker-compose.yml 中添加资源限制配置

## 常见问题

### Q: 如何修改默认密码？
A: 修改 docker-compose.yml 中对应服务的 environment 配置，然后重新创建容器：
```bash
docker-compose down
docker-compose up -d
```

### Q: 如何备份数据？
A: 使用 docker exec 命令执行备份：
```bash
# PostgreSQL 备份
docker exec kb-postgres pg_dump -U postgres knowledge_base > backup.sql

# MySQL 备份
docker exec kb-mysql mysqldump -uroot -proot knowledge_base > backup.sql
```

### Q: 如何恢复数据？
A: 使用 docker exec 命令执行恢复：
```bash
# PostgreSQL 恢复
docker exec -i kb-postgres psql -U postgres knowledge_base < backup.sql

# MySQL 恢复
docker exec -i kb-mysql mysql -uroot -proot knowledge_base < backup.sql
```
