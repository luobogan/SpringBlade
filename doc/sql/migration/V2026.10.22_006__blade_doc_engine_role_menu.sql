-- V2026.10.22_006__blade_doc_engine_role_menu.sql
-- 内容引擎：将「检索/模板/回收站/在线编辑」4 菜单授予 administrator 角色（1123598816738675201），
-- 使 /menu/routes 对这些角色下发（前端侧边栏 + 路由注册）。幂等：WHERE NOT EXISTS 防重复授权。
-- 目标库: blade（blade_role_menu 所在库）。

INSERT INTO blade_role_menu (id, menu_id, role_id)
SELECT t.id, t.menu_id, t.role_id FROM (
  SELECT 2113270000000000001 AS id, 2113246706520207361 AS menu_id, 1123598816738675201 AS role_id
  UNION ALL SELECT 2113270000000000002, 2113246706654425089, 1123598816738675201
  UNION ALL SELECT 2113270000000000003, 2113246706784448514, 1123598816738675201
  UNION ALL SELECT 2113270000000000004, 2113246706851557378, 1123598816738675201
) t
WHERE NOT EXISTS (
  SELECT 1 FROM blade_role_menu r WHERE r.menu_id = t.menu_id AND r.role_id = t.role_id
);
