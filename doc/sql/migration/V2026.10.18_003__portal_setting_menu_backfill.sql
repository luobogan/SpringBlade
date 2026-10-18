-- =============================================================================
-- 迁移脚本 V2026.10.18_003：补齐「门户应用设置」菜单（修复 /portal/setting 404）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.18_003
-- 目标库    : blade
-- 影响表    : blade_menu（INSERT 幂等补齐）、blade_role_menu（授权 administrator）
-- 现象      : 直接访问 /portal/setting 报 404（组件 ./pages/Portal/Setting/index.tsx
--             与后端 /blade-portal/setting 都在，缺的是 blade_menu 里的菜单行）。
-- 原因      : 该菜单由 V2026.10.16_002 注册（code=portal_setting，path=/portal/setting，
--             parent=portal）。本项目的页面路由是「菜单驱动」：blade_menu 没有行 →
--             /menu/routes 不下发 → 前端 patchClientRoutes 注册不到 → 落到 /* 404。
--             常见于：该迁移未执行、或执行时父菜单 portal 尚未建好（脚本的
--             FROM blade_menu p WHERE p.code='portal' 取不到父级 → 静默跳过）。
-- 方案      : 幂等补齐菜单行 + 按钮权限行 + administrator 授权。
-- 是否幂等  : 是（按 code 判重；授权按 menu_id+role_id 判重）
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_setting','portal_setting_manage','portal_setting_maint');
--   DELETE FROM `blade_menu` WHERE `code` IN ('portal_setting','portal_setting_manage','portal_setting_maint');
-- =============================================================================

USE `blade`;

-- -----------------------------------------------------------------------------
-- 0) 前置诊断：portal 父菜单必须存在（不存在则先跑 V2026.10.13_002__portal_menu.sql）
-- -----------------------------------------------------------------------------
SELECT `id`, `code`, `name`, `path`
  FROM `blade_menu`
 WHERE `code` = 'portal' AND `is_deleted` = 0;

-- -----------------------------------------------------------------------------
-- 1) 页面菜单：门户应用设置（两段连写路径 → ./pages/Portal/Setting/index.tsx）
--    注意 sort=12 与 V2026.10.16_002 保持一致，避免顺序漂移
-- -----------------------------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_setting', '门户应用设置', 'portal_setting', '/portal/setting', 'api', 12,
       1, 1, 0, 0,
       '门户应用设置（普通子菜单 → 组件 ./pages/Portal/Setting/index.tsx；path 末段须连写 setting）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_setting' AND m.`is_deleted` = 0);

-- -----------------------------------------------------------------------------
-- 2) 按钮权限 + 权限码（category=2，不进侧边栏；前端 usePageButtons 门禁用）
-- -----------------------------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_setting_manage' AS `code`, '门户应用设置按钮' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_setting_maint',   '门户应用设置维护权限', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = v.`code` AND m.`is_deleted` = 0);

-- -----------------------------------------------------------------------------
-- 3) 授权：仅 administrator（门户应用设置属管理端能力）
-- -----------------------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0 AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_setting','portal_setting_manage','portal_setting_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- -----------------------------------------------------------------------------
-- 4) 路径纠偏：历史行若被写成别的大小写/连字符，这里对齐
-- -----------------------------------------------------------------------------
UPDATE `blade_menu` SET `path` = '/portal/setting'
 WHERE `code` = 'portal_setting' AND `is_deleted` = 0 AND `path` <> '/portal/setting';

-- -----------------------------------------------------------------------------
-- 5) 校验输出：应看到 1 行 category=1 的 /portal/setting + 2 行 category=2 按钮
-- -----------------------------------------------------------------------------
SELECT m.`code`, m.`name`, m.`path`, m.`category`, m.`sort`,
       (SELECT COUNT(*) FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id`) AS `role_grant_cnt`
  FROM `blade_menu` m
 WHERE m.`code` IN ('portal_setting','portal_setting_manage','portal_setting_maint')
   AND m.`is_deleted` = 0;