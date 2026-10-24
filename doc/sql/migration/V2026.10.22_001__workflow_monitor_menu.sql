-- =============================================================================
-- 迁移脚本：流程监控菜单（管理员视角，全租户实例检索 + 监管动作）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.22_001
-- 目标库    : blade
-- 影响表    : blade_menu（1 个子页）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code + tenant 不存在才插入；授权按 menu_id+role_id 不存在才插入）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_menu` m ON m.id = rm.menu_id
--     WHERE m.`code` = 'workflow_monitor';
--   DELETE FROM `blade_menu` WHERE `code` = 'workflow_monitor';
--
-- 背景
--   前端路由 100% 由 blade_menu 数据驱动：loopMenuItem 按 path 首+末段拼出组件路径
--   ./pages/Workflow/{Page}/{Page}.tsx。故子页必须采用「两段路径」/workflow/monitor
--   → ./pages/Workflow/Monitor/Monitor.tsx（见 V2026.09.20_001 注释）。
--   控制器 WfMonitorController 全部端点 @PreAuth(hasRole('workflow'))，菜单/按钮须
--   授权给 role_alias='workflow'（流程管理员）。页面内监管动作（强制收回/归档/删除/批量）
--   均为控制器级角色门禁，无需再单独建按钮码。
-- =============================================================================

USE `blade`;

-- 1) 子页：流程监控（挂在「流程中心」下，两段路径 → ./pages/Workflow/Monitor/Monitor.tsx）----
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'workflow_center' AND `is_deleted` = 0 LIMIT 1),
       'workflow_monitor', '流程监控', 'workflow_monitor', '/workflow/monitor', 'api', 5,
       1, 1, 0, 0, NULL, 0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'workflow_monitor' AND `is_deleted` = 0);

-- 2) 授权给「流程管理员」角色（role_alias='workflow'，同租户）---------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`role_alias` = 'workflow' AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND m.`code` = 'workflow_monitor'
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );
