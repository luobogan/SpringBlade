-- V2026.10.22_013__blade_doc_center_menu_grant.sql
-- 知识中心菜单（docengine_center）授权给 administrator 角色，使其出现在 /menu/routes。幂等。
INSERT INTO blade_role_menu (id, menu_id, role_id)
SELECT 2113424400000000001, 2113424331842695169, 1123598816738675201 FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM blade_role_menu r
  WHERE r.menu_id = 2113424331842695169 AND r.role_id = 1123598816738675201
);
