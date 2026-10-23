-- V2026.10.22_012__blade_doc_stat_menu_grant.sql
-- 文档统计菜单（docengine_stat）授权给 administrator 角色，使其出现在 /menu/routes。幂等。
INSERT INTO blade_role_menu (id, menu_id, role_id)
SELECT 2113309000000000001, 2113308963321065473, 1123598816738675201 FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM blade_role_menu r
  WHERE r.menu_id = 2113308963321065473 AND r.role_id = 1123598816738675201
);
