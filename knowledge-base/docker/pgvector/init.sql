-- ============================================
-- 智能知识库系统数据库初始化脚本
-- 版本: 1.3
-- 说明: 合并了所有数据库初始化和迁移脚本，添加RBAC权限管理
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
-- 2.5 RBAC权限管理表
-- ============================================

-- 用户表 (sys_user)
CREATE TABLE IF NOT EXISTS sys_user (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.id IS '主键ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码(BCrypt加密)';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.avatar IS '头像URL';
COMMENT ON COLUMN sys_user.is_active IS '是否启用';
COMMENT ON COLUMN sys_user.is_deleted IS '是否删除';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
COMMENT ON COLUMN sys_user.last_login_time IS '最后登录时间';

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_sys_user_email ON sys_user(email);
CREATE INDEX IF NOT EXISTS idx_sys_user_phone ON sys_user(phone);

-- 角色表 (sys_role)
CREATE TABLE IF NOT EXISTS sys_role (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    sort INT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_name UNIQUE (name),
    CONSTRAINT uk_sys_role_code UNIQUE (code)
);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.id IS '主键ID';
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.code IS '角色编码';
COMMENT ON COLUMN sys_role.description IS '角色描述';
COMMENT ON COLUMN sys_role.sort IS '排序';
COMMENT ON COLUMN sys_role.is_active IS '是否启用';
COMMENT ON COLUMN sys_role.is_deleted IS '是否删除';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';

-- 权限表 (sys_permission)
CREATE TABLE IF NOT EXISTS sys_permission (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    parent_id VARCHAR(36),
    path VARCHAR(200),
    icon VARCHAR(50),
    component VARCHAR(200),
    sort INT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_permission_code UNIQUE (code)
);

COMMENT ON TABLE sys_permission IS '权限表';
COMMENT ON COLUMN sys_permission.id IS '主键ID';
COMMENT ON COLUMN sys_permission.name IS '权限名称';
COMMENT ON COLUMN sys_permission.code IS '权限编码';
COMMENT ON COLUMN sys_permission.type IS '权限类型: MENU/BUTTON/API';
COMMENT ON COLUMN sys_permission.parent_id IS '父级权限ID';
COMMENT ON COLUMN sys_permission.path IS '路由路径';
COMMENT ON COLUMN sys_permission.icon IS '图标';
COMMENT ON COLUMN sys_permission.component IS '组件路径';
COMMENT ON COLUMN sys_permission.sort IS '排序';
COMMENT ON COLUMN sys_permission.is_active IS '是否启用';
COMMENT ON COLUMN sys_permission.is_deleted IS '是否删除';
COMMENT ON COLUMN sys_permission.create_time IS '创建时间';
COMMENT ON COLUMN sys_permission.update_time IS '更新时间';

-- 权限表索引
CREATE INDEX IF NOT EXISTS idx_sys_permission_parent_id ON sys_permission(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_permission_type ON sys_permission(type);

-- 用户角色关联表 (sys_user_role)
CREATE TABLE IF NOT EXISTS sys_user_role (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

COMMENT ON TABLE sys_user_role IS '用户角色关联表';
COMMENT ON COLUMN sys_user_role.id IS '主键ID';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';
COMMENT ON COLUMN sys_user_role.create_time IS '创建时间';

-- 用户角色关联表索引
CREATE INDEX IF NOT EXISTS idx_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role_id ON sys_user_role(role_id);

-- 角色权限关联表 (sys_role_permission)
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id VARCHAR(36) PRIMARY KEY,
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
);

COMMENT ON TABLE sys_role_permission IS '角色权限关联表';
COMMENT ON COLUMN sys_role_permission.id IS '主键ID';
COMMENT ON COLUMN sys_role_permission.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_permission.permission_id IS '权限ID';
COMMENT ON COLUMN sys_role_permission.create_time IS '创建时间';

-- 角色权限关联表索引
CREATE INDEX IF NOT EXISTS idx_role_permission_role_id ON sys_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_permission_id ON sys_role_permission(permission_id);

-- ============================================
-- 7. 初始化RBAC数据
-- ============================================

-- 初始化角色
INSERT INTO sys_role (id, name, code, description, sort, is_active, is_deleted, create_time, update_time) 
VALUES 
('admin', '超级管理员', 'ADMIN', '系统超级管理员，拥有所有权限', 1, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user', '普通用户', 'USER', '普通用户，拥有基本操作权限', 2, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 初始化权限
INSERT INTO sys_permission (id, name, code, type, parent_id, path, icon, component, sort, is_active, is_deleted, create_time, update_time) 
VALUES 
-- 用户管理菜单
('user', '用户管理', 'user:menu', 'MENU', NULL, '/user', 'User', '/views/user/UserListView', 1, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user:list', '用户列表', 'user:list', 'BUTTON', 'user', NULL, NULL, NULL, 1, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user:add', '新增用户', 'user:add', 'BUTTON', 'user', NULL, NULL, NULL, 2, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user:edit', '编辑用户', 'user:edit', 'BUTTON', 'user', NULL, NULL, NULL, 3, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user:delete', '删除用户', 'user:delete', 'BUTTON', 'user', NULL, NULL, NULL, 4, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- 角色管理菜单
('role', '角色管理', 'role:menu', 'MENU', NULL, '/role', 'UserFilled', '/views/user/RoleListView', 2, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('role:list', '角色列表', 'role:list', 'BUTTON', 'role', NULL, NULL, NULL, 1, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('role:add', '新增角色', 'role:add', 'BUTTON', 'role', NULL, NULL, NULL, 2, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('role:edit', '编辑角色', 'role:edit', 'BUTTON', 'role', NULL, NULL, NULL, 3, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('role:delete', '删除角色', 'role:delete', 'BUTTON', 'role', NULL, NULL, NULL, 4, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 给超级管理员分配所有权限
DO $$
DECLARE
    admin_role_id VARCHAR(36) := 'admin';
    permission_record RECORD;
BEGIN
    FOR permission_record IN SELECT id FROM sys_permission LOOP
        INSERT INTO sys_role_permission (id, role_id, permission_id, create_time)
        SELECT gen_random_uuid(), admin_role_id, permission_record.id, CURRENT_TIMESTAMP
        WHERE NOT EXISTS (
            SELECT 1 FROM sys_role_permission 
            WHERE role_id = admin_role_id AND permission_id = permission_record.id
        );
    END LOOP;
END
$$;

-- ============================================
-- 8. 初始化数据
-- ============================================

-- 默认管理员用户由应用启动时自动初始化 (DataInitializer)
-- 用户名: admin, 密码: admin123

-- 插入默认知识库示例（如果需要）
-- INSERT INTO knowledge_base (id, name, description) 
-- VALUES ('default', '默认知识库', '系统默认知识库') 
-- ON CONFLICT (id) DO NOTHING;
