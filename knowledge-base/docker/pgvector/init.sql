-- ============================================
-- 智能知识库系统数据库初始化脚本
-- 版本: 1.2
-- 说明: 合并了所有数据库初始化和迁移脚本
-- ============================================

-- 设置时区
SET timezone = 'Asia/Shanghai';

-- ============================================
-- 1. 启用必要的扩展
-- ============================================

-- 启用 pgvector 扩展（用于向量相似度检索）
CREATE EXTENSION IF NOT EXISTS vector;

-- 尝试安装中文全文检索扩展（如果可用）
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS zhparser;
    RAISE NOTICE 'zhparser 扩展安装成功';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'zhparser 扩展不可用，将使用默认 simple 配置: %', SQLERRM;
END
$$;

-- ============================================
-- 2. 创建业务数据表
-- ============================================

-- 知识库表 (knowledge_base)
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

-- 文档表 (document)
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

-- 会话表 (chat_session)
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

-- 消息表 (chat_message)
CREATE TABLE IF NOT EXISTS chat_message (
    id VARCHAR(36) PRIMARY KEY,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    document_sources TEXT,
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id)
);

COMMENT ON TABLE chat_message IS '消息表';
COMMENT ON COLUMN chat_message.id IS '主键ID';
COMMENT ON COLUMN chat_message.role IS '角色: USER, ASSISTANT, SYSTEM';
COMMENT ON COLUMN chat_message.content IS '消息内容';
COMMENT ON COLUMN chat_message.session_id IS '所属会话ID';
COMMENT ON COLUMN chat_message.create_time IS '创建时间';
COMMENT ON COLUMN chat_message.document_sources IS 'AI回答引用的文档来源信息，以JSON格式存储';

-- 消息表索引
CREATE INDEX IF NOT EXISTS idx_message_session_id ON chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_message_create_time ON chat_message(create_time);

-- ============================================
-- 3. 创建向量存储表 (Spring AI PgVectorStore)
-- ============================================

CREATE TABLE IF NOT EXISTS vector_store (
    id BIGSERIAL PRIMARY KEY,
    content TEXT,
    metadata JSON,
    embedding vector(768)
);

COMMENT ON TABLE vector_store IS '向量存储表';
COMMENT ON COLUMN vector_store.id IS '主键ID';
COMMENT ON COLUMN vector_store.content IS '文本内容';
COMMENT ON COLUMN vector_store.metadata IS '元数据';
COMMENT ON COLUMN vector_store.embedding IS '向量嵌入';

-- ============================================
-- 4. 创建向量索引 (HNSW 算法)
-- ============================================

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding ON vector_store USING HNSW (embedding vector_cosine_ops);

-- ============================================
-- 5. 创建全文检索支持
-- ============================================

-- 创建中文全文检索配置（如果 zhparser 已安装）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'zhparser') THEN
        DROP TEXT SEARCH CONFIGURATION IF EXISTS chinese;
        CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
        ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l WITH simple;
        RAISE NOTICE 'chinese 全文检索配置已创建';
    ELSE
        RAISE NOTICE 'zhparser 未安装，跳过 chinese 配置';
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE '创建 chinese 配置失败: %', SQLERRM;
END
$$;

-- 添加全文检索列
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vector_store') THEN
        ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS content_tsv tsvector;
    END IF;
END
$$;

-- 创建更新全文检索列的触发器函数
CREATE OR REPLACE FUNCTION update_content_tsv()
RETURNS TRIGGER AS $$
DECLARE
    cfg TEXT := 'simple';
BEGIN
    IF EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese') THEN
        cfg := 'chinese';
    END IF;
    NEW.content_tsv := to_tsvector(cfg, COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
DROP TRIGGER IF EXISTS trigger_update_content_tsv ON vector_store;
CREATE TRIGGER trigger_update_content_tsv
    BEFORE INSERT OR UPDATE ON vector_store
    FOR EACH ROW
    EXECUTE FUNCTION update_content_tsv();

-- 更新现有数据
DO $$
DECLARE
    search_config TEXT := 'simple';
BEGIN
    IF EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese') THEN
        search_config := 'chinese';
    END IF;
    EXECUTE format('UPDATE vector_store SET content_tsv = to_tsvector(%L, COALESCE(content, ''''))', search_config);
END
$$;

-- 创建全文检索索引
CREATE INDEX IF NOT EXISTS idx_vector_store_content_tsv ON vector_store USING GIN(content_tsv);

-- ============================================
-- 6. 创建关键词搜索函数
-- ============================================

CREATE OR REPLACE FUNCTION keyword_search(
    query_text TEXT,
    kb_id TEXT DEFAULT NULL,
    result_limit INT DEFAULT 10
)
RETURNS TABLE (
    id BIGINT,
    content TEXT,
    metadata JSONB,
    rank REAL
) AS $$
DECLARE
    search_config TEXT := 'simple';
BEGIN
    IF EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese') THEN
        search_config := 'chinese';
    END IF;
    
    RETURN QUERY
    EXECUTE format('
        SELECT 
            vs.id,
            vs.content,
            vs.metadata,
            ts_rank(vs.content_tsv, plainto_tsquery(%L, $1))::REAL as rank
        FROM vector_store vs
        WHERE vs.content_tsv @@ plainto_tsquery(%L, $1)
            AND ($2 IS NULL OR vs.metadata->>''knowledge_base_id'' = $2)
        ORDER BY rank DESC
        LIMIT $3
    ', search_config, search_config)
    USING query_text, kb_id, result_limit;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION keyword_search IS '基于全文检索的关键词搜索函数';

-- ============================================
-- 7. 初始化数据（可选）
-- ============================================

-- 插入默认知识库示例（如果需要）
-- INSERT INTO knowledge_base (id, name, description) 
-- VALUES ('default', '默认知识库', '系统默认知识库') 
-- ON CONFLICT (id) DO NOTHING;
