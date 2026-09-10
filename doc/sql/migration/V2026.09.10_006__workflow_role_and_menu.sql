-- =============================================================================
-- 迁移脚本 006：引入「流程管理员」业务角色（role_alias = workflow）
--              并授权 workflow 菜单 / 操作按钮，实现前后端一致的「角色可见性」
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.10_006
-- 目标库    : blade（角色与菜单在 blade 库，非 blade_workflow）
-- 影响表    : blade_role（新增角色）、blade_menu（新增操作按钮）、blade_role_menu（授权）
-- 变更类型  : DML（角色 + 菜单 + 关联）
-- 是否幂等  : 是（均按「不存在才插入」判断）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_role` r ON r.id = rm.role_id
--     WHERE r.role_alias = 'workflow';
--   DELETE FROM `blade_role` WHERE `role_alias` = 'workflow';
--   DELETE FROM `blade_menu` WHERE `code` IN
--     ('workflow_add','workflow_view','workflow_edit','workflow_deploy','workflow_enable');
--
-- 背景
--   workflow（blade-workflow）与 formmode（blade-formmode）两侧控制器的 @PreAuth
--   已统一改为 hasRole('workflow')（常量 WorkflowConstant.HAS_ROLE_WORKFLOW）。
--   本例以 blade_role.role_alias 作为 @PreAuth 的匹配值（登录时用户 roles 取 role_alias）。
--   本脚本负责把「流程管理员」角色、菜单与操作按钮落库并授权，使前端（菜单驱动 +
--   按钮 code 门禁）与后端角色门禁口径一致：只有被授予该角色的用户才能看到并调用。
--
-- ⚠️ 执行顺序：先执行本脚本，再重启/重部署 blade-workflow 与 blade-formmode。
-- =============================================================================

USE `blade`;

-- 1) 新增「流程管理员」角色 -----------------------------------------------------
--    role_alias 必须为 'workflow'（@PreAuth hasRole('workflow') 按 role_alias 匹配）。
--    按「已存在 workflow 菜单」的租户逐个补建，保证多租户一致。
INSERT INTO `blade_role`
  (`id`, `tenant_id`, `parent_id`, `role_name`, `sort`, `role_alias`, `is_deleted`, `status`)
SELECT UUID_SHORT(), m.`tenant_id`, 0, '流程管理员', 10, 'workflow', 0, 1
FROM `blade_menu` m
WHERE m.`code` = 'workflow' AND m.`is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role` r
    WHERE r.`role_alias` = 'workflow' AND r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
  )
GROUP BY m.`tenant_id`;

-- 2) 新增 workflow 操作按钮（category=2 权限码；is_component=0 不注册路由）-------
--    挂在同租户的「流程设计」列表菜单（code='workflow'）下，供前端按钮 code 门禁使用。
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), w.`id`, v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, w.`tenant_id`, 1
FROM `blade_menu` w
JOIN (
            SELECT 'workflow_add'    AS `code`, '新增'       AS `name`, 1 AS `sort`
  UNION ALL SELECT 'workflow_view',           '查看',                  2
  UNION ALL SELECT 'workflow_edit',           '编辑',                  3
  UNION ALL SELECT 'workflow_deploy',         '部署',                  4
  UNION ALL SELECT 'workflow_enable',         '启用/停用',             5
) v
WHERE w.`code` = 'workflow' AND w.`is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `blade_menu` x
    WHERE x.`code` = v.`code` AND x.`tenant_id` = w.`tenant_id` AND x.`is_deleted` = 0
  );

-- 3) 授权菜单与按钮给「流程管理员」角色（同租户）--------------------------------
--    含父级「流程建模」(code='mode')、「流程设计」列表、以及设计/操作按钮。
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`role_alias` = 'workflow' AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`code` IN (
    'mode',
    'workflow', 'workflow_design',
    'workflow_add', 'workflow_view', 'workflow_edit', 'workflow_deploy', 'workflow_enable'
  )
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 4) 将角色授予用户（按需执行）--------------------------------------------------
--    推荐改用「系统管理 → 用户管理」界面勾选「流程管理员」角色。
--    blade_user.role_id 为逗号分隔的角色ID；下面按账号追加本角色ID（幂等）。
-- UPDATE `blade_user` u
--   JOIN `blade_role` r ON r.`role_alias` = 'workflow' AND r.`tenant_id` = u.`tenant_id` AND r.`is_deleted` = 0
--   SET u.`role_id` = CONCAT(NULLIF(u.`role_id`, ''), ',', r.`id`)
--   WHERE u.`account` = '请替换为你的账号' AND FIND_IN_SET(r.`id`, u.`role_id`) = 0;
