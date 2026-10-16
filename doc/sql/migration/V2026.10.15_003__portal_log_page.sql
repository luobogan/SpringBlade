-- =============================================================================
-- 迁移脚本 V2026.10.15_003：门户审计日志页菜单
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_003
-- 目标库    : blade
-- 影响表    : blade_menu（审计页菜单 1）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §13.4（门户审计 PortalLogInfo）
--              后端 GET /log/list 已就绪（HAS_ADMIN），本脚本登记前端页面菜单。
-- 说明      : 路径两段 /portal/log → 组件 ./pages/Portal/Log/index.tsx（末段 log → Log）。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` = 'portal_log_list';
--   DELETE FROM `blade_menu` WHERE `code` = 'portal_log_list';
-- =============================================================================

USE `blade`;

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_log_list', '门户审计日志', 'portal_log_list', '/portal/log', 'api', 7,
       1, 1, 0, 0,
       '门户审计日志页（普通子菜单 → 组件 ./pages/Portal/Log/index.tsx；后端 /log/list 限 administrator）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_log_list' AND m.`is_deleted` = 0);

-- 授权：仅超级管理员（审计属管理端能力）
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` = 'portal_log_list'
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 校验输出 ---------------------------------------------------------------------
SELECT m.`code`, m.`name`, m.`path`, m.`category`
  FROM `blade_menu` m
 WHERE m.`code` = 'portal_log_list' AND m.`is_deleted` = 0;
