-- =============================================================================
-- 流程设计「删除」操作按钮 —— 菜单数据
--
-- 对应前端 System/Workflow/Workflow.tsx 里的「删除」按钮（hasPerm('workflow_delete')）。
-- 机制（与 workflow_design_menu.sql 一致）：
--   * usePageButtons() 从当前路由末段取 code（/system/workflow -> 'workflow'），
--     再 getButton('workflow') 返回该菜单的子按钮；故本按钮的 parent_id 必须指向
--     code='workflow' 的列表菜单，自身 code='workflow_delete' 用于前端门禁。
--   * 本按钮是列表内的 Popconfirm 删除按钮，不注册独立组件路由，
--     因此 is_component=0、path=''，不会触发前端自动路由注册。
--
-- 目标库：blade（blade_menu 所在库）
-- 幂等：已存在则跳过。
-- =============================================================================

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT
  2061825613800460292,                 -- id（唯一即可）
  (SELECT id FROM `blade_menu` WHERE `code` = 'workflow' AND `is_deleted` = 0 LIMIT 1),  -- 指向流程设计列表菜单
  'workflow_delete', '删除', 'workflow_delete', '', 'api', 20, 2, 1, 0, 0, NULL, 0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'workflow_delete' AND `is_deleted` = 0);
