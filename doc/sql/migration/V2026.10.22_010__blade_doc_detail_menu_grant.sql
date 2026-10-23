-- V2026.10.22_010__blade_doc_detail_menu_grant.sql
-- 文档详情菜单（docengine_detail，组件型 category=2/isComponent=1）授权给 administrator 角色，
-- 使 /menu/routes 下发该路由（前端可由检索页「详情」跳入）。幂等。
INSERT INTO blade_role_menu (id, menu_id, role_id)
SELECT 2113299700000000001, 2113299654440824834, 1123598816738675201 FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM blade_role_menu r
  WHERE r.menu_id = 2113299654440824834 AND r.role_id = 1123598816738675201
);
