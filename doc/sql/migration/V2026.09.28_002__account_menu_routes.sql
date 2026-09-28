-- =============================================================================
-- 迁移脚本：个人中心 / 个人设置 菜单驱动化
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.28_002
-- 目标库    : blade（blade_menu / blade_role_menu 所在库）
-- 影响表    : blade_menu（新增「个人中心」分组 + 个人设置/个人中心 两个页面菜单）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 不存在才插入；授权按 menu_id+role_id 不存在才插入）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_menu` m ON m.id = rm.menu_id
--     WHERE m.`code` IN ('account','account_settings','account_center');
--   DELETE FROM `blade_menu` WHERE `code` IN ('account','account_settings','account_center');
--
-- 背景
--   原前端在 config/routes.ts 静态注册 /account/settings、/account/center。
--   按本项目「前端路由 100% 由 blade_menu 驱动」的约定，改为由后端菜单驱动：
--   在 blade_menu 中新增「个人中心」分组及其两个子页面菜单，并授权给角色。
--   头像下拉菜单的「个人设置 / 个人中心」通过 history.push('/account/settings') 进入。
--
-- 前端解析规则（src/app.tsx loopMenuItem / patchClientRoutes）
--   * category=1 + 有 path 的行 → 注册页面路由（挂到 '/' 之下，渲染在 ProLayout/左侧菜单内）
--   * 组件文件 = ./pages/{path首段Pascal}/{path次段Pascal}/{path末段Pascal}.tsx
--       /account/settings  →  ./pages/Account/Settings/index.tsx（目录入口，loopMenuItem 已加 index 回退）
--       /account/center    →  ./pages/Account/Center/Center.tsx
-- =============================================================================

USE `blade`;

-- 1) 个人中心（分组父菜单，path=/account，无独立页面，重定向到首个子项）-----------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       0,
       'account', '个人中心', 'account', '/account', 'user', 100,
       1, 0, 1, 0,
       '个人中心分组（个人设置/个人中心），由头像下拉菜单「个人设置」进入',
       0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'account' AND `is_deleted` = 0);

-- 2) 个人设置（页面菜单）-----------------------------------------------------------
SET @p_id = (SELECT `id` FROM `blade_menu` WHERE `code` = 'account' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       @p_id,
       'account_settings', '个人设置', 'account_settings', '/account/settings', 'setting', 1,
       1, 0, 1, 0,
       '个人设置页（菜单驱动路由；组件 ./pages/Account/Settings/index.tsx）',
       0, '000000', 1
FROM DUAL
WHERE @p_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'account_settings' AND `is_deleted` = 0);

-- 3) 个人中心（页面菜单）-----------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       @p_id,
       'account_center', '个人中心', 'account_center', '/account/center', 'team', 2,
       1, 0, 1, 0,
       '个人中心页（菜单驱动路由；组件 ./pages/Account/Center/Center.tsx）',
       0, '000000', 1
FROM DUAL
WHERE @p_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'account_center' AND `is_deleted` = 0);

-- 4) 授权给「超级管理员」与「用户」角色（role_id 取自 blade_role 种子）-------------------
SET @m_settings = (SELECT `id` FROM `blade_menu` WHERE `code` = 'account_settings' AND `is_deleted` = 0 LIMIT 1);
SET @m_center   = (SELECT `id` FROM `blade_menu` WHERE `code` = 'account_center'   AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM (SELECT @m_settings AS id) m
CROSS JOIN `blade_role` r
WHERE r.`role_alias` IN ('administrator', 'user') AND r.`is_deleted` = 0
  AND m.id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.id AND rm.`role_id` = r.`id`);

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM (SELECT @m_center AS id) m
CROSS JOIN `blade_role` r
WHERE r.`role_alias` IN ('administrator', 'user') AND r.`is_deleted` = 0
  AND m.id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.id AND rm.`role_id` = r.`id`);

-- 5) 校验输出 ------------------------------------------------------------------------
SELECT m.`id`, m.`parent_id`, m.`code`, m.`path`, m.`category`, m.`is_open`,
       (SELECT COUNT(*) FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id`) AS granted_roles
FROM `blade_menu` m
WHERE m.`code` IN ('account', 'account_settings', 'account_center') AND m.`is_deleted` = 0
ORDER BY m.`sort`;
