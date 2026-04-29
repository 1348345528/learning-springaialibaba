-- =============================================
-- 1. 插入 MCP 管理菜单项（顶级菜单）
-- =============================================
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort_order, menu_type, permission, visible, create_time)
SELECT 0, 'MCP 管理', '/mcp', 'McpManagement', 'ApiOutlined', 3, 2, NULL, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/mcp');

-- =============================================
-- 2. 给管理员角色授权（role_id = 1）
-- =============================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE path = '/mcp'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = (SELECT id FROM sys_menu WHERE path = '/mcp'));

-- =============================================
-- 3. 给普通用户角色授权（role_id = 2）
-- =============================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE path = '/mcp'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 2 AND menu_id = (SELECT id FROM sys_menu WHERE path = '/mcp'));

-- =============================================
-- 验证插入结果
-- =============================================
SELECT '=== sys_menu ===' AS '';
SELECT * FROM sys_menu WHERE path = '/mcp';

SELECT '=== sys_role_menu ===' AS '';
SELECT r.name AS role_name, m.name AS menu_name
FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.id
JOIN sys_menu m ON rm.menu_id = m.id
WHERE m.path = '/mcp';
