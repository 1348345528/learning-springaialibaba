-- =============================================
-- RAG 知识库系统 - 数据库初始化脚本
-- 数据库: MySQL (rag_knowledgebase)
-- 执行方式: mysql -u root -p < init-database.sql
-- =============================================

-- =============================================
-- 0. 创建数据库
-- =============================================
CREATE DATABASE IF NOT EXISTS rag_knowledgebase
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE rag_knowledgebase;

-- =============================================
-- 1. 认证授权服务 (auth-service) 表
-- =============================================

-- 1.1 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT          NOT NULL AUTO_INCREMENT  PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL                 COMMENT '用户名',
    password    VARCHAR(255)    NOT NULL                 COMMENT 'BCrypt 加密密码',
    email       VARCHAR(100)    DEFAULT NULL             COMMENT '邮箱',
    status      INT             DEFAULT 1                COMMENT '状态: 1=启用, 0=禁用',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 1.2 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT          NOT NULL AUTO_INCREMENT  PRIMARY KEY,
    name        VARCHAR(50)     NOT NULL                 COMMENT '角色名称',
    code        VARCHAR(50)     NOT NULL                 COMMENT '角色编码',
    description VARCHAR(255)    DEFAULT NULL             COMMENT '角色描述',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX idx_role_code (code),
    UNIQUE INDEX idx_role_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 1.3 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT          NOT NULL AUTO_INCREMENT  PRIMARY KEY,
    parent_id   BIGINT          DEFAULT 0                COMMENT '父菜单ID, 0=顶级菜单',
    name        VARCHAR(50)     NOT NULL                 COMMENT '菜单名称',
    path        VARCHAR(255)    DEFAULT NULL             COMMENT '路由路径',
    component   VARCHAR(255)    DEFAULT NULL             COMMENT '前端组件名',
    icon        VARCHAR(50)     DEFAULT NULL             COMMENT '图标',
    sort_order  INT             DEFAULT 0                COMMENT '排序号',
    menu_type   INT             NOT NULL                 COMMENT '类型: 1=目录, 2=菜单, 3=按钮',
    permission  VARCHAR(100)    DEFAULT NULL             COMMENT '权限标识',
    visible     INT             DEFAULT 1                COMMENT '是否可见: 1=可见, 0=隐藏',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

-- 1.4 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id     BIGINT          NOT NULL                 COMMENT '用户ID',
    role_id     BIGINT          NOT NULL                 COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 1.5 角色-菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id     BIGINT          NOT NULL                 COMMENT '角色ID',
    menu_id     BIGINT          NOT NULL                 COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- =============================================
-- 2. 聊天服务 (chat-service) 表
-- =============================================

-- 2.1 会话表
CREATE TABLE IF NOT EXISTS conversations (
    id              BIGINT          NOT NULL AUTO_INCREMENT  PRIMARY KEY,
    conversation_id VARCHAR(36)     NOT NULL                 COMMENT '会话UUID',
    title           VARCHAR(255)    NOT NULL                 COMMENT '会话标题',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX idx_conversation_id (conversation_id),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 2.2 聊天消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id              BIGINT          NOT NULL AUTO_INCREMENT  PRIMARY KEY,
    conversation_id BIGINT          NOT NULL                 COMMENT '关联会话ID(conversations.id)',
    role            VARCHAR(20)     NOT NULL                 COMMENT '角色: user/assistant/system',
    content         TEXT            NOT NULL                 COMMENT '消息内容',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_msg_conversation_id (conversation_id),
    INDEX idx_msg_created_at (created_at),
    CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- =============================================
-- 3. 向量检索服务 (rag-retrieval-service) 表
-- =============================================

-- 3.1 文档分块表
CREATE TABLE IF NOT EXISTS chunks (
    id              VARCHAR(255)    NOT NULL                 PRIMARY KEY COMMENT '分块唯一ID',
    content         TEXT            DEFAULT NULL             COMMENT '分块文本内容',
    document_name   VARCHAR(255)    DEFAULT NULL             COMMENT '源文档名',
    chunk_size      INT             DEFAULT NULL             COMMENT '分块大小',
    chunk_index     INT             DEFAULT NULL             COMMENT '分块序号',
    strategy        VARCHAR(50)     DEFAULT NULL             COMMENT '分块策略',
    tags            JSON            DEFAULT NULL             COMMENT '标签(JSON)',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    start_pos       INT             DEFAULT NULL             COMMENT '源文档起始位置',
    end_pos         INT             DEFAULT NULL             COMMENT '源文档结束位置',
    parent_id       VARCHAR(255)    DEFAULT NULL             COMMENT '父分块ID(分层策略)',
    parent_index    INT             DEFAULT NULL             COMMENT '父分块序号',
    local_index     INT             DEFAULT NULL             COMMENT '父级内本地序号',
    is_parent       TINYINT(1)      DEFAULT NULL             COMMENT '是否父分块',
    is_child        TINYINT(1)      DEFAULT NULL             COMMENT '是否子分块',
    metadata        JSON            DEFAULT NULL             COMMENT '元数据(JSON)',
    INDEX idx_document_name (document_name),
    INDEX idx_chunk_index (chunk_index),
    INDEX idx_strategy (strategy),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分块表';

-- =============================================
-- 4. 初始化数据
-- =============================================

-- 4.1 管理员用户 (默认密码: admin123)
-- BCrypt 密码需通过 BCryptPasswordEncoder 生成
-- 以下密文对应明文 "admin123"，如需更换密码请重新生成
INSERT IGNORE INTO sys_user (id, username, password, email, status) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@example.com', 1);

-- 4.2 角色
INSERT IGNORE INTO sys_role (id, name, code, description) VALUES
(1, '管理员',   'ROLE_ADMIN', '系统管理员，拥有所有权限'),
(2, '普通用户', 'ROLE_USER',  '普通用户，基础功能权限');

-- 4.3 菜单
INSERT IGNORE INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, menu_type, permission, visible) VALUES
-- 顶级目录
(1,  0, '首页',     '/dashboard',    'Dashboard',    'HomeOutlined',      1, 1, NULL,            1),
(2,  0, '系统管理', NULL,            NULL,           'SettingOutlined',  10, 1, NULL,            1),
(3,  0, 'AI 对话',  '/chat',         'Chat',         'MessageOutlined',   2, 2, NULL,            1),
(4,  0, '文档管理', NULL,            NULL,           'FileOutlined',      3, 1, NULL,            1),
(5,  0, 'MCP 管理', '/mcp',          'McpManagement', 'ApiOutlined',      4, 2, NULL,            1),
-- 系统管理子菜单
(6,  2, '用户管理', '/system/user',  'UserManagement', 'UserOutlined',    1, 2, 'user:list',    1),
(7,  2, '角色管理', '/system/role',  'RoleManagement', 'TeamOutlined',    2, 2, 'role:list',    1),
(8,  2, '菜单管理', '/system/menu',  'MenuManagement', 'MenuOutlined',    3, 2, 'menu:list',    1),
-- 文档管理子菜单
(9,  4, '文档上传', '/doc/upload',   'DocUpload',      'UploadOutlined',  1, 2, 'doc:upload',   1),
(10, 4, '分块管理', '/doc/chunks',   'ChunkManager',   'BlockOutlined',   2, 2, 'chunk:list',   1);

-- 4.4 给管理员角色分配所有菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 4.5 给普通用户角色分配部分菜单（不包含系统管理）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE parent_id = 0 AND menu_type IN (1, 2);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE name IN ('文档上传', '分块管理');

-- 4.6 给管理员用户分配管理员角色
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- =============================================
-- 验证
-- =============================================
SELECT '>>> Tables created:' AS '';
SHOW TABLES;

SELECT '>>> Users:' AS '';
SELECT id, username, email, status FROM sys_user;

SELECT '>>> Roles:' AS '';
SELECT id, name, code FROM sys_role;

SELECT '>>> Menus:' AS '';
SELECT id, parent_id, name, path, menu_type FROM sys_menu ORDER BY sort_order, id;

SELECT '>>> User-Role assignments:' AS '';
SELECT u.username, r.name AS role_name
FROM sys_user_role ur
JOIN sys_user u ON ur.user_id = u.id
JOIN sys_role r ON ur.role_id = r.id;

SELECT '>>> Role-Menu assignments:' AS '';
SELECT r.name AS role_name, COUNT(rm.menu_id) AS menu_count
FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.id
GROUP BY r.name;
