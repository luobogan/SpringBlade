-- V2026.10.22_009__blade_doc_menu_role_grant.sql
-- 目录配置菜单（docengine_categoryconfig）授权给 administrator 角色，使其出现在 /menu/routes。
-- 幂等：WHERE NOT EXISTS 防重复授权。目标库: blade。
INSERT INTO blade_role_menu (id, menu_id, role_id)
SELECT 2113288300000000001, 2113288204980760577, 1123598816738675201 FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM blade_role_menu r
  WHERE r.menu_id = 2113288204980760577 AND r.role_id = 1123598816738675201
);
