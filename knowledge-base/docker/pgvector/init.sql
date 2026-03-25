-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 设置时区
SET timezone = 'Asia/Shanghai';

-- ============================================
-- 知识库表 (knowledge_base)
-- ============================================
CREATE TABLE IF NOT EXISTS knowledge_base (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE knowledge_base IS '知识库表';
COMMENT ON COLUMN knowledge_base.id IS '主键ID';
COMMENT ON COLUMN knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN knowledge_base.description IS '知识库描述';
COMMENT ON COLUMN knowledge_base.create_time IS '创建时间';
COMMENT ON COLUMN knowledge_base.update_time IS '更新时间';
COMMENT ON COLUMN knowledge_base.is_deleted IS '是否删除';

-- ============================================
-- 文档表 (document)
-- ============================================
CREATE TABLE IF NOT EXISTS document (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    type VARCHAR(50),
    size BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    knowledge_base_id VARCHAR(36) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_document_knowledge_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id)
);

COMMENT ON TABLE document IS '文档表';
COMMENT ON COLUMN document.id IS '主键ID';
COMMENT ON COLUMN document.name IS '文档名称';
COMMENT ON COLUMN document.file_path IS '文件路径';
COMMENT ON COLUMN document.type IS '文件类型';
COMMENT ON COLUMN document.size IS '文件大小(字节)';
COMMENT ON COLUMN document.status IS '处理状态: PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN document.knowledge_base_id IS '所属知识库ID';
COMMENT ON COLUMN document.create_time IS '创建时间';
COMMENT ON COLUMN document.update_time IS '更新时间';
COMMENT ON COLUMN document.is_deleted IS '是否删除';

-- 文档表索引
CREATE INDEX IF NOT EXISTS idx_document_knowledge_base_id ON document(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_document_status ON document(status);

-- ============================================
-- 会话表 (chat_session)
-- ============================================
CREATE TABLE IF NOT EXISTS chat_session (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(100),
    knowledge_base_id VARCHAR(36),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_session_knowledge_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id)
);

COMMENT ON TABLE chat_session IS '会话表';
COMMENT ON COLUMN chat_session.id IS '主键ID';
COMMENT ON COLUMN chat_session.title IS '会话标题';
COMMENT ON COLUMN chat_session.knowledge_base_id IS '关联知识库ID';
COMMENT ON COLUMN chat_session.create_time IS '创建时间';
COMMENT ON COLUMN chat_session.update_time IS '更新时间';
COMMENT ON COLUMN chat_session.is_deleted IS '是否删除';

-- 会话表索引
CREATE INDEX IF NOT EXISTS idx_session_knowledge_base_id ON chat_session(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_session_update_time ON chat_session(update_time);

-- ============================================
-- 消息表 (chat_message)
-- ============================================
CREATE TABLE IF NOT EXISTS chat_message (
    id VARCHAR(36) PRIMARY KEY,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id)
);

COMMENT ON TABLE chat_message IS '消息表';
COMMENT ON COLUMN chat_message.id IS '主键ID';
COMMENT ON COLUMN chat_message.role IS '角色: USER, ASSISTANT, SYSTEM';
COMMENT ON COLUMN chat_message.content IS '消息内容';
COMMENT ON COLUMN chat_message.session_id IS '所属会话ID';
COMMENT ON COLUMN chat_message.create_time IS '创建时间';

-- 消息表索引
CREATE INDEX IF NOT EXISTS idx_message_session_id ON chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_message_create_time ON chat_message(create_time);

-- ============================================
-- 向量存储表 (vector_store)
-- Spring AI PgVectorStore 使用的表，用于存储文档向量
-- ============================================
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    metadata JSON,
    embedding vector(768)
);

COMMENT ON TABLE vector_store IS '向量存储表';
COMMENT ON COLUMN vector_store.id IS '主键ID';
COMMENT ON COLUMN vector_store.content IS '文本内容';
COMMENT ON COLUMN vector_store.metadata IS '元数据';
COMMENT ON COLUMN vector_store.embedding IS '向量嵌入';

-- 向量索引（使用 HNSW 算法）
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding ON vector_store USING HNSW (embedding vector_cosine_ops);

-- ============================================
-- 初始化数据（可选）
-- ============================================
-- 插入默认知识库示例（如果需要）
-- INSERT INTO knowledge_base (id, name, description) 
-- VALUES ('default', '默认知识库', '系统默认知识库') 
-- ON CONFLICT (id) DO NOTHING;
