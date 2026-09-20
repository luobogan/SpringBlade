-- =============================================================================
-- 迁移脚本 001：流程中心（用户侧）菜单驱动化
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.20_001
-- 目标库    : blade
-- 影响表    : blade_menu（父菜单 + 4 子页 + 3 权限按钮）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code + tenant 不存在才插入；授权按 menu_id+role_id 不存在才插入）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_role` r ON r.id = rm.role_id
--     WHERE r.role_alias = 'workflow';
--   DELETE FROM `blade_menu` WHERE `code` IN
--     ('workflow_center','workflow_create','workflow_todo','workflow_done',
--      'workflow_request','workflow_create_submit','workflow_todo_forward','workflow_todo_sign');
--
-- 背景
--   前端本来在 config/routes.ts 静态写死 4 条「流程中心」路由。现改为由后端
--   blade_menu 数据驱动：/menu/routes 动态下发后，前端 loopMenuItem 按 path 首+末段
--   拼出组件路径 ./pages/Workflow/{Page}/{Page}.tsx，故子页必须采用「两段路径」。
--   工作流控制器均 @PreAuth(hasRole('workflow'))，菜单/按钮须授权给 role_alias='workflow'。
-- =============================================================================

USE `blade`;

-- 1) 父菜单：流程中心（category=1 分组，无组件，重定向到首个子页）-----------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `name` = '流程建模' AND `is_deleted` = 0 LIMIT 1),
       'workflow_center', '流程中心', 'workflow_center', '/workflow', 'api', 60,
       1, 1, 0, 0, NULL, 0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'workflow_center' AND `is_deleted` = 0);

-- 2) 子页（category=1，两段路径，组件 = ./pages/Workflow/{Page}/{Page}.tsx）--------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'workflow_center' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, v.`path`, 'api', v.`sort`,
       1, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'workflow_create'  AS `code`, '新建流程' AS `name`, '/workflow/create'  AS `path`, 1 AS `sort`
  UNION ALL SELECT 'workflow_todo',    '待办事宜', '/workflow/todo',    2
  UNION ALL SELECT 'workflow_done',    '已办事宜', '/workflow/done',    3
  UNION ALL SELECT 'workflow_request', '我的请求', '/workflow/request', 4
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 3) 权限按钮（category=2，is_component=0，path 留空，供前端按钮 code 门禁）-------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = v.`parent` AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'workflow_create' AS `parent`, 'workflow_create_submit' AS `code`, '提交' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'workflow_todo', 'workflow_todo_forward', '转办', 1
  UNION ALL SELECT 'workflow_todo', 'workflow_todo_sign',    '加签', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 4) 授权给「流程管理员」角色（role_alias='workflow'，同租户）---------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`role_alias` = 'workflow' AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND m.`code` IN (
    'workflow_center',
    'workflow_create', 'workflow_todo', 'workflow_done', 'workflow_request',
    'workflow_create_submit', 'workflow_todo_forward', 'workflow_todo_sign'
  )
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );
