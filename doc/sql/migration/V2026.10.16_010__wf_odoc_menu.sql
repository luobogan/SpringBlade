-- -----------------------------------------------------------------------------
-- 迁移脚本：注册「公文管理」前端菜单（后端菜单驱动路由）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_010
-- 目标库    : blade（注意：真实 dev 库名是 blade，非 blade_workflow）
-- 影响表    : blade_menu（插入）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（INSERT IGNORE，固定主键，重跑按 PK 跳过）
-- 回滚      : DELETE FROM blade_menu WHERE id IN (2108000000000201..206);
--             DELETE FROM blade_role_menu WHERE menu_id IN (2108000000000201..206);
--
-- 背景
--   前端 ant-design-pro 路由由后端菜单驱动（app.tsx dynamicRoutes + patchClientRoutes）。
--   页面已就绪于 src/pages/Odoc/{TodoList,Supervise,Report,DocTypeChange,Exchange}/index.tsx。
--   path→组件规则（app.tsx buildComponentLoaders 约定③）：/odoc/todo-list → ./pages/Odoc/TodoList/index.tsx。
--   授权角色取自 admin 登录取到的 role_id（1123598816738675201, 1020723482528317444）。
--   生效方式：插入后重新登录/刷新菜单（前端菜单有 localStorage 缓存）。

INSERT IGNORE INTO blade.blade_menu
(id, parent_id, code, name, alias, path, source, category, action, is_open, is_component, component, component_type, sort, status, is_deleted, tenant_id, create_time, update_time) VALUES
(2108000000000201, 0,                   'odoc',           '公文管理',   'odoc',           '/odoc',               'file-text',  1, 0, 1, 0, NULL, 'bundled', 50, 1, 0, '000000', NOW(), NOW()),
(2108000000000202, 2108000000000201,    'odoc_todo',      '待办处理',   'odoc_todo',      '/odoc/todo-list',     'file-text',  1, 0, 1, 0, NULL, 'bundled', 1,  1, 0, '000000', NOW(), NOW()),
(2108000000000203, 2108000000000201,    'odoc_supervise', '公文督办',   'odoc_supervise', '/odoc/supervise',     'bell',       1, 0, 1, 0, NULL, 'bundled', 2,  1, 0, '000000', NOW(), NOW()),
(2108000000000204, 2108000000000201,    'odoc_report',    '公文统计',   'odoc_report',    '/odoc/report',        'bar-chart',  1, 0, 1, 0, NULL, 'bundled', 3,  1, 0, '000000', NOW(), NOW()),
(2108000000000205, 2108000000000201,    'odoc_doctype',   '文种转换',   'odoc_doctype',   '/odoc/doc-type-change','swap',      1, 0, 1, 0, NULL, 'bundled', 4,  1, 0, '000000', NOW(), NOW()),
(2108000000000206, 2108000000000201,    'odoc_exchange',  '公文交换',   'odoc_exchange',  '/odoc/exchange',      'swap',       1, 0, 1, 0, NULL, 'bundled', 5,  1, 0, '000000', NOW(), NOW());

-- 授权给 admin 的两个角色（administrator / workflow）
INSERT IGNORE INTO blade.blade_role_menu (id, menu_id, role_id) VALUES
(2108000000000311, 2108000000000201, 1123598816738675201),
(2108000000000312, 2108000000000202, 1123598816738675201),
(2108000000000313, 2108000000000203, 1123598816738675201),
(2108000000000314, 2108000000000204, 1123598816738675201),
(2108000000000315, 2108000000000205, 1123598816738675201),
(2108000000000316, 2108000000000206, 1123598816738675201),
(2108000000000321, 2108000000000201, 1020723482528317444),
(2108000000000322, 2108000000000202, 1020723482528317444),
(2108000000000323, 2108000000000203, 1020723482528317444),
(2108000000000324, 2108000000000204, 1020723482528317444),
(2108000000000325, 2108000000000205, 1020723482528317444),
(2108000000000326, 2108000000000206, 1020723482528317444);
