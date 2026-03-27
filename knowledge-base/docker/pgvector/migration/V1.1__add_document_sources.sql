-- ============================================
-- 迁移脚本: 添加文档来源字段到消息表
-- ============================================

-- 添加 document_sources 列到 chat_message 表
ALTER TABLE chat_message
ADD COLUMN IF NOT EXISTS document_sources TEXT;

COMMENT ON COLUMN chat_message.document_sources IS 'AI回答引用的文档来源信息，以JSON格式存储';
