# 数据库设计与优化指南

## 1. 关系型数据库基础

### 1.1 数据库设计原则

数据库设计是应用开发的基石，良好的设计能够提高性能、保证数据完整性并简化维护工作。

#### 设计规范

1. **命名规范**
   - 表名使用小写字母，单词间用下划线分隔
   - 主键统一命名为 `id`
   - 外键使用 `表名_id` 格式
   - 索引使用 `idx_字段名` 或 `uk_字段名` 格式

2. **字段设计**
   - 选择合适的数据类型，避免过度设计
   - 字符集统一使用 UTF-8 (utf8mb4)
   - 所有表必须包含 `created_at` 和 `updated_at` 字段
   - 软删除使用 `deleted_at` 字段

3. **索引设计**
   - 主键自动创建聚簇索引
   - 外键必须创建索引
   - 频繁查询的字段建立索引
   - 避免过多索引（一般不超过 5 个）

### 1.2 范式与反范式

#### 三大范式

**第一范式 (1NF)**：原子性，每列不可再分

```sql
-- 不符合 1NF
CREATE TABLE student (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    courses VARCHAR(255)  -- 'Math,English,Physics'
);

-- 符合 1NF
CREATE TABLE student (
    id INT PRIMARY KEY,
    name VARCHAR(50)
);

CREATE TABLE student_course (
    student_id INT,
    course_name VARCHAR(50),
    PRIMARY KEY (student_id, course_name)
);
```

**第二范式 (2NF)**：完全依赖，非主键字段必须完全依赖于主键

```sql
-- 不符合 2NF（联合主键）
CREATE TABLE order_item (
    order_id INT,
    product_id INT,
    product_name VARCHAR(100),  -- 只依赖 product_id
    quantity INT,
    PRIMARY KEY (order_id, product_id)
);

-- 符合 2NF
CREATE TABLE order_item (
    order_id INT,
    product_id INT,
    quantity INT,
    PRIMARY KEY (order_id, product_id)
);

CREATE TABLE product (
    id INT PRIMARY KEY,
    name VARCHAR(100)
);
```

**第三范式 (3NF)**：消除传递依赖

```sql
-- 不符合 3NF
CREATE TABLE employee (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    dept_id INT,
    dept_name VARCHAR(50)  -- 可通过 dept_id 关联获取
);

-- 符合 3NF
CREATE TABLE employee (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    dept_id INT
);

CREATE TABLE department (
    id INT PRIMARY KEY,
    name VARCHAR(50)
);
```

#### 反范式化

适当反范式化可以提高查询性能：

```sql
-- 反范式化：冗余存储订单总金额，避免实时计算
CREATE TABLE orders (
    id INT PRIMARY KEY,
    user_id INT,
    total_amount DECIMAL(10,2),  -- 冗余字段
    item_count INT,               -- 冗余字段
    created_at TIMESTAMP
);
```

## 2. MySQL 优化

### 2.1 索引优化

#### 索引类型

```sql
-- B+Tree 索引（默认）
CREATE INDEX idx_name ON users(name);

-- 唯一索引
CREATE UNIQUE INDEX uk_email ON users(email);

-- 复合索引
CREATE INDEX idx_name_age ON users(name, age);

-- 前缀索引
CREATE INDEX idx_content ON articles(content(100));

-- 全文索引
CREATE FULLTEXT INDEX ft_title ON articles(title, content);
```

#### 索引使用原则

1. **最左前缀原则**

```sql
-- 索引 (a, b, c)
WHERE a = 1              -- 使用索引
WHERE a = 1 AND b = 2    -- 使用索引
WHERE a = 1 AND b = 2 AND c = 3  -- 使用索引
WHERE b = 2              -- 不使用索引
WHERE a = 1 AND c = 3    -- 只使用 a
```

2. **避免索引失效**

```sql
-- 索引失效的情况
WHERE name LIKE '%john%'     -- 前导模糊查询
WHERE YEAR(created_at) = 2024  -- 对字段使用函数
WHERE id + 1 = 100           -- 对字段进行计算
WHERE status != 'active'     -- 不等于
WHERE status IS NULL         -- IS NULL（部分场景）
```

### 2.2 SQL 优化

#### 查询优化

```sql
-- 避免 SELECT *
SELECT id, name, email FROM users WHERE status = 'active';

-- 使用覆盖索引
SELECT id, name FROM users WHERE name = 'John';
-- 如果索引是 (name)，且查询字段只有 id 和 name，则无需回表

-- 使用 EXPLAIN 分析查询
EXPLAIN SELECT * FROM users WHERE email = 'john@example.com';

-- 优化分页查询
-- 低效
SELECT * FROM orders LIMIT 1000000, 10;
-- 高效
SELECT * FROM orders WHERE id > 1000000 LIMIT 10;
-- 或使用延迟关联
SELECT * FROM orders o
JOIN (SELECT id FROM orders LIMIT 1000000, 10) tmp ON o.id = tmp.id;
```

#### JOIN 优化

```sql
-- 确保关联字段有索引
CREATE INDEX idx_user_id ON orders(user_id);

-- 小表驱动大表
SELECT * FROM small_table s
JOIN large_table l ON s.id = l.small_id;

-- 避免多表 JOIN（超过 3 个表）
-- 考虑分步查询或冗余字段
```

### 2.3 表结构优化

#### 分区表

```sql
-- 范围分区
CREATE TABLE logs (
    id INT,
    message TEXT,
    created_at TIMESTAMP
) PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION pfuture VALUES LESS THAN MAXVALUE
);

-- 列表分区
CREATE TABLE orders (
    id INT,
    status VARCHAR(20),
    amount DECIMAL(10,2)
) PARTITION BY LIST COLUMNS(status) (
    PARTITION p_pending VALUES IN ('pending', 'processing'),
    PARTITION p_complete VALUES IN ('completed', 'delivered'),
    PARTITION p_cancel VALUES IN ('cancelled', 'refunded')
);

-- Hash 分区
CREATE TABLE transactions (
    id INT,
    user_id INT,
    amount DECIMAL(10,2)
) PARTITION BY HASH(user_id) PARTITIONS 4;
```

#### 分表策略

```sql
-- 水平分表：按用户 ID 取模
-- user_0, user_1, user_2, user_3

-- 垂直分表：将大字段分离
-- 主表
CREATE TABLE user_basic (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    email VARCHAR(100),
    created_at TIMESTAMP
);

-- 扩展表
CREATE TABLE user_profile (
    user_id INT PRIMARY KEY,
    bio TEXT,
    avatar_url VARCHAR(255),
    address JSON,
    FOREIGN KEY (user_id) REFERENCES user_basic(id)
);
```

## 3. PostgreSQL 高级特性

### 3.1 JSON/JSONB 操作

```sql
-- 创建包含 JSON 的表
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    event_type VARCHAR(50),
    payload JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 插入 JSON 数据
INSERT INTO events (event_type, payload) VALUES (
    'user_login',
    '{"user_id": 123, "ip": "192.168.1.1", "device": "mobile"}'
);

-- 查询 JSON 字段
SELECT * FROM events WHERE payload->>'user_id' = '123';
SELECT payload->>'ip' as ip_address FROM events;

-- 使用 JSON 操作符
SELECT * FROM events WHERE payload @> '{"device": "mobile"}';
SELECT payload #> '{address,city}' as city FROM events;

-- 创建 GIN 索引加速 JSON 查询
CREATE INDEX idx_events_payload ON events USING GIN (payload);
```

### 3.2 全文搜索

```sql
-- 创建全文搜索向量
CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255),
    content TEXT,
    search_vector tsvector
);

-- 自动更新搜索向量
CREATE OR REPLACE FUNCTION update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('chinese', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('chinese', COALESCE(NEW.content, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER articles_search_vector_update
    BEFORE INSERT OR UPDATE ON articles
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();

-- 全文搜索查询
SELECT * FROM articles
WHERE search_vector @@ plainto_tsquery('chinese', '数据库优化')
ORDER BY ts_rank(search_vector, plainto_tsquery('chinese', '数据库优化')) DESC;

-- 创建 GIN 索引
CREATE INDEX idx_articles_search ON articles USING GIN (search_vector);
```

### 3.3 窗口函数

```sql
-- 排名函数
SELECT
    name,
    department,
    salary,
    RANK() OVER (PARTITION BY department ORDER BY salary DESC) as rank,
    DENSE_RANK() OVER (PARTITION BY department ORDER BY salary DESC) as dense_rank,
    ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) as row_num
FROM employees;

-- 聚合窗口函数
SELECT
    date,
    amount,
    SUM(amount) OVER (ORDER BY date) as running_total,
    AVG(amount) OVER (ORDER BY date ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) as moving_avg
FROM sales;

-- 取值窗口函数
SELECT
    name,
    salary,
    LAG(salary) OVER (ORDER BY salary) as prev_salary,
    LEAD(salary) OVER (ORDER BY salary) as next_salary,
    FIRST_VALUE(salary) OVER (ORDER BY salary) as lowest_salary,
    LAST_VALUE(salary) OVER (ORDER BY salary ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) as highest_salary
FROM employees;
```

### 3.4 CTE 递归查询

```sql
-- 查询组织架构树
WITH RECURSIVE org_tree AS (
    -- 锚点成员：顶级部门
    SELECT id, name, parent_id, 0 as level
    FROM departments
    WHERE parent_id IS NULL

    UNION ALL

    -- 递归成员：子部门
    SELECT d.id, d.name, d.parent_id, ot.level + 1
    FROM departments d
    JOIN org_tree ot ON d.parent_id = ot.id
)
SELECT * FROM org_tree ORDER BY level, id;

-- 查询分类路径
WITH RECURSIVE category_path AS (
    SELECT id, name, parent_id, name as path
    FROM categories
    WHERE id = 5

    UNION ALL

    SELECT c.id, c.name, c.parent_id, c.name || ' > ' || cp.path
    FROM categories c
    JOIN category_path cp ON c.id = cp.parent_id
)
SELECT path FROM category_path WHERE parent_id IS NULL;
```

## 4. 数据库性能监控

### 4.1 MySQL 性能监控

```sql
-- 查看慢查询日志配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- 查看正在执行的查询
SHOW PROCESSLIST;
SELECT * FROM information_schema.PROCESSLIST WHERE TIME > 10;

-- 查看锁等待
SELECT * FROM information_schema.INNODB_LOCK_WAITS;
SELECT * FROM information_schema.INNODB_LOCKS;

-- 查看表统计信息
SHOW TABLE STATUS LIKE 'users';

-- 查看索引使用情况
SELECT * FROM sys.schema_unused_indexes;
SELECT * FROM sys.schema_redundant_indexes;
```

### 4.2 PostgreSQL 性能监控

```sql
-- 查看活动连接
SELECT * FROM pg_stat_activity WHERE state = 'active';

-- 查看慢查询
SELECT * FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- 查看表统计信息
SELECT * FROM pg_stat_user_tables
ORDER BY seq_scan DESC;

-- 查看索引使用情况
SELECT * FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- 查看锁信息
SELECT * FROM pg_locks WHERE NOT granted;

-- 查看缓冲区命中率
SELECT
    sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) as hit_ratio
FROM pg_statio_user_tables;
```

## 5. 数据迁移与备份

### 5.1 MySQL 备份恢复

```bash
# 使用 mysqldump 备份
mysqldump -u root -p --single-transaction --routines --triggers mydb > backup.sql

# 压缩备份
mysqldump -u root -p mydb | gzip > backup.sql.gz

# 恢复数据
mysql -u root -p mydb < backup.sql

# 使用 xtrabackup 热备份
xtrabackup --backup --target-dir=/backup/full
xtrabackup --prepare --target-dir=/backup/full
```

### 5.2 PostgreSQL 备份恢复

```bash
# pg_dump 逻辑备份
pg_dump -U postgres -d mydb -f backup.sql
pg_dump -U postgres -d mydb -Fc -f backup.dump  # 自定义格式

# 恢复
psql -U postgres -d mydb -f backup.sql
pg_restore -U postgres -d mydb backup.dump

# pg_basebackup 物理备份
pg_basebackup -U postgres -D /backup/pg_backup -Fp -Xs -P -v

# 使用 WAL 归档实现 PITR
# postgresql.conf
archive_mode = on
archive_command = 'cp %p /archive/%f'
wal_level = replica
```

---

本文档详细介绍了数据库设计的核心原则、MySQL 和 PostgreSQL 的优化技巧、高级特性以及性能监控方法，是数据库开发和管理的完整参考指南。
