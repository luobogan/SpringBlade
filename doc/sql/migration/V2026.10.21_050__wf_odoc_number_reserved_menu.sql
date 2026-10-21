-- -----------------------------------------------------------------------------
-- 迁移脚本：注册「文号保留号」前端菜单
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.21_050
-- 目标库    : blade（真实 dev 库名是 blade）
-- 影响表    : blade_menu（插入）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（INSERT IGNORE，固定主键，重跑按 PK 跳过）
-- 回滚      : DELETE FROM blade_menu WHERE id = 2108000000000213;
--             DELETE FROM blade_role_menu WHERE menu_id = 2108000000000213;
--
-- 背景
--   文号保留号后端已就绪（/odoc/number-reserved，WfOdocController），numberReservedApi 已定义，
--   前端维护页位于 src/pages/Odoc/NumberReservedManage/index.tsx。
--   path→组件规则（app.tsx buildComponentLoaders 约定③，normalizeDirKey 会去连字符）：
--   /odoc/number-reserved → odoc/numberreserved → ./pages/Odoc/NumberReservedManage/index.tsx。
--   父菜单 2108000000000201 = 公文管理（见 V2026.10.16_010）。
--   生效方式：插入后重新登录/刷新菜单（前端菜单有 localStorage 缓存）。

INSERT IGNORE INTO blade.blade_menu
(id, parent_id, code, name, alias, path, source, category, action, is_open, is_component, component, component_type, sort, status, is_deleted, tenant_id, create_time, update_time) VALUES
(2108000000000213, 2108000000000201, 'odoc_number_reserved', '文号保留号', 'odoc_number_reserved', '/odoc/number-reserved', 'numberreserved', 1, 0, 1, 0, NULL, 'bundled', 11, 1, 0, '000000', NOW(), NOW());

-- 授权给 admin 的两个角色（administrator / workflow）
INSERT IGNORE INTO blade.blade_role_menu (id, menu_id, role_id) VALUES
(2108000000000433, 2108000000000213, 1123598816738675201),
(2108000000000434, 2108000000000213, 1020723482528317444);
