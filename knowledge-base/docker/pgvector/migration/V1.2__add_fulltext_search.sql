-- 添加全文检索支持

-- 1. 安装中文全文检索扩展（如果可用）
DO $$
BEGIN
    -- 尝试安装 zhparser（PostgreSQL 中文分词器）
    CREATE EXTENSION IF NOT EXISTS zhparser;
    
    -- 创建中文全文检索配置
    DROP TEXT SEARCH CONFIGURATION IF EXISTS chinese;
    CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
    
    -- 添加映射
    ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l WITH simple;
    
EXCEPTION WHEN OTHERS THEN
    -- 如果 zhparser 不可用，使用默认的 simple 配置
    RAISE NOTICE 'zhparser 不可用，使用默认配置';
END
$$;

-- 2. 创建全文检索索引
-- 注意：pgvector 的 vector_store 表中的 content 字段需要添加全文检索索引

-- 检查表是否存在
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vector_store') THEN
        -- 添加全文检索列（如果不存在）
        ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS content_tsv tsvector;
        
        -- 创建更新全文检索列的函数
        CREATE OR REPLACE FUNCTION update_content_tsv()
        RETURNS TRIGGER AS $$
        BEGIN
            NEW.content_tsv := to_tsvector('chinese', COALESCE(NEW.content, ''));
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
        UPDATE vector_store SET content_tsv = to_tsvector('chinese', COALESCE(content, ''));
        
        -- 创建全文检索索引
        CREATE INDEX IF NOT EXISTS idx_vector_store_content_tsv ON vector_store USING GIN(content_tsv);
        
        RAISE NOTICE '全文检索索引创建完成';
    END IF;
END
$$;

-- 3. 创建用于关键词检索的函数
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
BEGIN
    RETURN QUERY
    SELECT 
        vs.id,
        vs.content,
        vs.metadata,
        ts_rank(vs.content_tsv, plainto_tsquery('chinese', query_text))::REAL as rank
    FROM vector_store vs
    WHERE vs.content_tsv @@ plainto_tsquery('chinese', query_text)
        AND (kb_id IS NULL OR vs.metadata->>'knowledge_base_id' = kb_id)
    ORDER BY rank DESC
    LIMIT result_limit;
END;
$$ LANGUAGE plpgsql;

-- 4. 添加注释
COMMENT ON FUNCTION keyword_search IS '基于全文检索的关键词搜索函数';
