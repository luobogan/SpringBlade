-- -----------------------------------------------------------------------------
-- 迁移脚本：注册「公文基础数据」前端菜单（公文规范/意见模板/收发文单位/收文公共组）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_040
-- 目标库    : blade（真实 dev 库名是 blade）
-- 影响表    : blade_menu（插入）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（INSERT IGNORE，固定主键，重跑按 PK 跳过）
-- 回滚      : DELETE FROM blade_menu WHERE id IN (2108000000000207..210);
--             DELETE FROM blade_role_menu WHERE menu_id IN (2108000000000207..210);
--
-- 背景
--   公文基础数据后端已就绪（/odoc/dict|opinion|receive-unit|receive-group/**），
--   前端维护页位于 src/pages/Odoc/{DictManage,OpinionManage,ReceiveUnitManage,ReceiveGroupManage}/index.tsx。
--   path→组件规则（app.tsx buildComponentLoaders 约定③）：
--     /odoc/dict          → ./pages/Odoc/DictManage/index.tsx
--     /odoc/opinion       → ./pages/Odoc/OpinionManage/index.tsx
--     /odoc/receive-unit  → ./pages/Odoc/ReceiveUnitManage/index.tsx
--     /odoc/receive-group → ./pages/Odoc/ReceiveGroupManage/index.tsx
--   父菜单 2108000000000201 = 公文管理（见 V2026.10.16_010）。
--   生效方式：插入后重新登录/刷新菜单（前端菜单有 localStorage 缓存）。

INSERT IGNORE INTO blade.blade_menu
(id, parent_id, code, name, alias, path, source, category, action, is_open, is_component, component, component_type, sort, status, is_deleted, tenant_id, create_time, update_time) VALUES
(2108000000000207, 2108000000000201, 'odoc_dict',        '公文规范',   'odoc_dict',        '/odoc/dict',         'book',    1, 0, 1, 0, NULL, 'bundled', 6, 1, 0, '000000', NOW(), NOW()),
(2108000000000208, 2108000000000201, 'odoc_opinion',     '意见模板',   'odoc_opinion',     '/odoc/opinion',      'message', 1, 0, 1, 0, NULL, 'bundled', 7, 1, 0, '000000', NOW(), NOW()),
(2108000000000209, 2108000000000201, 'odoc_receive_unit','收发文单位', 'odoc_receive_unit','/odoc/receive-unit', 'team',    1, 0, 1, 0, NULL, 'bundled', 8, 1, 0, '000000', NOW(), NOW()),
(2108000000000210, 2108000000000201, 'odoc_receve_group','收文公共组', 'odoc_receve_group','/odoc/receive-group','cluster', 1, 0, 1, 0, NULL, 'bundled', 9, 1, 0, '000000', NOW(), NOW());

-- 授权给 admin 的两个角色（administrator / workflow）
INSERT IGNORE INTO blade.blade_role_menu (id, menu_id, role_id) VALUES
(2108000000000411, 2108000000000207, 1123598816738675201),
(2108000000000412, 2108000000000208, 1123598816738675201),
(2108000000000413, 2108000000000209, 1123598816738675201),
(2108000000000414, 2108000000000210, 1123598816738675201),
(2108000000000421, 2108000000000207, 1020723482528317444),
(2108000000000422, 2108000000000208, 1020723482528317444),
(2108000000000423, 2108000000000209, 1020723482528317444),
(2108000000000424, 2108000000000210, 1020723482528317444);
