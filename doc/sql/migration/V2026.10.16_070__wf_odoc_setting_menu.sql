-- -----------------------------------------------------------------------------
-- 迁移脚本：注册「公文设置」前端菜单（通用设置）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_070
-- 目标库    : blade（真实 dev 库名是 blade）
-- 影响表    : blade_menu（插入）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（INSERT IGNORE，固定主键，重跑按 PK 跳过）
-- 回滚      : DELETE FROM blade_menu WHERE id = 2108000000000212;
--             DELETE FROM blade_role_menu WHERE menu_id = 2108000000000212;
--
-- 背景
--   公文通用设置后端已就绪（/odoc/setting，WfOdocSettingController），前端页位于
--   src/pages/Odoc/Setting/index.tsx。
--   path→组件规则（app.tsx buildComponentLoaders 约定③）：/odoc/setting → ./pages/Odoc/Setting/index.tsx。
--   父菜单 2108000000000201 = 公文管理（见 V2026.10.16_010）。
--   生效方式：插入后重新登录/刷新菜单（前端菜单有 localStorage 缓存）。

INSERT IGNORE INTO blade.blade_menu
(id, parent_id, code, name, alias, path, source, category, action, is_open, is_component, component, component_type, sort, status, is_deleted, tenant_id, create_time, update_time) VALUES
(2108000000000212, 2108000000000201, 'odoc_setting', '公文设置', 'odoc_setting', '/odoc/setting', 'setting', 1, 0, 1, 0, NULL, 'bundled', 11, 1, 0, '000000', NOW(), NOW());

-- 授权给 admin 的两个角色（administrator / workflow）
INSERT IGNORE INTO blade.blade_role_menu (id, menu_id, role_id) VALUES
(2108000000000441, 2108000000000212, 1123598816738675201),
(2108000000000442, 2108000000000212, 1020723482528317444);
