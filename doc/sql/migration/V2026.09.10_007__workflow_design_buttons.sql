-- =============================================================================
-- 迁移脚本 007：流程设计页（WorkflowDesign / BpmnDesigner）操作按钮 code 门禁
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.10_007
-- 目标库    : blade
-- 影响表    : blade_menu（新增按钮）、blade_role_menu（授权）
-- 变更类型  : DML
-- 是否幂等  : 是（按 code 不存在才插入）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm
--     JOIN `blade_menu` m ON m.id = rm.menu_id
--     WHERE m.`code` IN ('workflow_design_create','workflow_design_deploy',
--                        'workflow_design_save_perm','workflow_design_save_canvas');
--   DELETE FROM `blade_menu` WHERE `code` IN
--     ('workflow_design_create','workflow_design_deploy',
--      'workflow_design_save_perm','workflow_design_save_canvas');
--
-- 背景
--   设计页按钮以 `usePageButtons('workflow_design')` 取权限：getButton('workflow_design')
--   返回 code='workflow_design' 菜单的子按钮。故需把设计页操作按钮挂到该菜单下，并按角色授权。
--   前端（WorkflowDesign.tsx / BpmnDesigner.tsx）按下列 code 控制显隐：
--     workflow_design_create       新建
--     workflow_design_deploy       部署（列表行 / 画布「部署到引擎」）
--     workflow_design_save_perm    保存权限（节点字段权限矩阵）
--     workflow_design_save_canvas  保存画布（BPMN）
--
-- ⚠️ 依赖：需先存在 code='workflow_design' 的菜单（workflow_design_menu.sql）与
--          code='workflow' 业务角色（迁移 006）。执行顺序：workflow_design_menu.sql → 006 → 007。
-- =============================================================================

USE `blade`;

-- 1) 新增设计页操作按钮（category=2 权限码；is_component=0 不注册路由）----------
--    挂在同租户的「流程设计」按钮菜单（code='workflow_design'）下。
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), d.`id`, v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, d.`tenant_id`, 1
FROM `blade_menu` d
JOIN (
            SELECT 'workflow_design_create'      AS `code`, '新建'     AS `name`, 1 AS `sort`
  UNION ALL SELECT 'workflow_design_deploy',              '部署',              2
  UNION ALL SELECT 'workflow_design_save_perm',           '保存权限',          3
  UNION ALL SELECT 'workflow_design_save_canvas',         '保存画布',          4
) v
WHERE d.`code` = 'workflow_design' AND d.`is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `blade_menu` x
    WHERE x.`code` = v.`code` AND x.`tenant_id` = d.`tenant_id` AND x.`is_deleted` = 0
  );

-- 2) 授权给「流程管理员」角色（同租户）-----------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`role_alias` = 'workflow' AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`code` IN (
    'workflow_design_create', 'workflow_design_deploy',
    'workflow_design_save_perm', 'workflow_design_save_canvas'
  )
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );
