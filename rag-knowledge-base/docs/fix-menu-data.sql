-- =============================================
-- 修复菜单数据
-- 执行方式: mysql -u root -p rag_knowledgebase < fix-menu-data.sql
-- =============================================

-- 1. 清空现有菜单数据（保留自增ID）
DELETE FROM sys_role_menu;
DELETE FROM sys_menu;

-- 2. 重新插入菜单数据（与 DataInitializer.java 保持一致）
-- 顶级目录
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, menu_type, permission, visible, create_time) VALUES
(1, 0, 'RAG知识库维护', NULL, NULL, 'DatabaseOutlined', 1, 1, NULL, 1, NOW());

-- RAG 子菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, menu_type, permission, visible, create_time) VALUES
(2, 1, '文档管理', '/rag/document', 'DocumentUpload', NULL, 2, 2, NULL, 1, NOW()),
(3, 1, '知识块管理', '/rag/chunk', 'ChunkManagement', NULL, 2, 2, NULL, 1, NOW());

-- 智能问答和 MCP 管理
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, menu_type, permission, visible, create_time) VALUES
(4, 0, '智能问答', '/chat', 'ChatQA', 'RobotOutlined', 2, 2, NULL, 1, NOW()),
(5, 0, 'MCP 管理', '/mcp', 'McpManagement', 'ApiOutlined', 3, 2, NULL, 1, NOW());

-- 系统管理目录
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, menu_type, permission, visible, create_time) VALUES
(6, 0, '系统管理', NULL, NULL, 'SettingOutlined', 4, 1, 'ROLE_ADMIN', 1, NOW());

-- 系统管理子菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, menu_type, permission, visible, create_time) VALUES
(7, 6, '用户管理', '/sys/user', 'UserManagement', NULL, 2, 2, 'ROLE_ADMIN', 1, NOW()),
(8, 6, '角色管理', '/sys/role', 'RoleManagement', NULL, 2, 2, 'ROLE_ADMIN', 1, NOW()),
(9, 6, '菜单管理', '/sys/menu', 'MenuManagement', NULL, 2, 2, 'ROLE_ADMIN', 1, NOW());

-- 3. 重新分配角色菜单权限
-- 管理员角色 (假设 id=1)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 普通用户角色 (假设 id=2) - 只分配 RAG、智能问答、MCP
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE id IN (1, 2, 3, 4, 5);

-- 4. 验证结果
SELECT '>>> 修复后的菜单数据:' AS '';
SELECT id, parent_id, name, path, menu_type, visible FROM sys_menu ORDER BY id;

SELECT '>>> 角色菜单分配:' AS '';
SELECT r.name AS role_name, m.name AS menu_name
FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.id
JOIN sys_menu m ON rm.menu_id = m.id
ORDER BY r.id, m.id;
