-- 添加全文检索支持

-- 1. 尝试安装中文全文检索扩展（如果可用）
-- 注意：如果 zhparser 未安装，将使用默认的 simple 配置
DO $$
BEGIN
    -- 尝试安装 zhparser（PostgreSQL 中文分词器）
    CREATE EXTENSION IF NOT EXISTS zhparser;
    
    -- 检查 zhparser 是否成功安装
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'zhparser') THEN
        -- 创建中文全文检索配置
        DROP TEXT SEARCH CONFIGURATION IF EXISTS chinese;
        CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
        
        -- 添加映射
        ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l WITH simple;
        
        RAISE NOTICE 'zhparser 扩展安装成功，chinese 配置已创建';
    ELSE
        RAISE NOTICE 'zhparser 扩展未安装，将使用 simple 配置';
    END IF;
EXCEPTION WHEN OTHERS THEN
    -- 如果 zhparser 不可用，使用默认的 simple 配置
    RAISE NOTICE 'zhparser 不可用，使用默认 simple 配置: %', SQLERRM;
END
$$;

-- 2. 创建全文检索索引
-- 注意：pgvector 的 vector_store 表中的 content 字段需要添加全文检索索引
-- 使用 simple 配置，兼容性更好（不需要安装 zhparser 扩展）

-- 检查表是否存在
DO $$
DECLARE
    search_config TEXT := 'simple';
BEGIN
    -- 如果 chinese 配置存在则使用，否则使用 simple
    IF EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese') THEN
        search_config := 'chinese';
        RAISE NOTICE '使用 chinese 全文检索配置';
    ELSE
        RAISE NOTICE '使用 simple 全文检索配置（chinese 配置不存在）';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vector_store') THEN
        -- 添加全文检索列（如果不存在）
        ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS content_tsv tsvector;
        
        -- 创建更新全文检索列的函数（使用动态配置）
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
        EXECUTE format('UPDATE vector_store SET content_tsv = to_tsvector(%L, COALESCE(content, ''''))', search_config);
        
        -- 创建全文检索索引
        CREATE INDEX IF NOT EXISTS idx_vector_store_content_tsv ON vector_store USING GIN(content_tsv);
        
        RAISE NOTICE '全文检索索引创建完成，使用配置: %', search_config;
    END IF;
END
$$;

-- 3. 创建用于关键词检索的函数
-- 使用 simple 配置，与应用程序代码保持一致
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
    -- 如果 chinese 配置存在则使用，否则使用 simple
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

-- 4. 添加注释
COMMENT ON FUNCTION keyword_search IS '基于全文检索的关键词搜索函数';
