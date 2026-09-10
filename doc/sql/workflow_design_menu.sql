-- =============================================================================
-- 流程设计「进入设计」操作按钮 —— 菜单数据
--
-- 机制说明（与前端 app.tsx 菜单驱动路由对齐）：
--   * 前端路由完全由 blade_menu 驱动，无需在 config/routes.ts 写死。
--   * 列表页（category=1）：组件路径 = ./pages/{Module}/{Page}/{Page}.tsx
--       -> /system/workflow  -> System/Workflow/Workflow.tsx（现有列表页）
--   * 按钮页（category=2 且 is_component=1 且带 path）：组件路径 = ./pages/{PascalPath}.tsx
--       -> /formmode/workflowdesign -> FormMode/WorkflowDesign/index.tsx（现有设计页，命名已匹配）
--   * 按钮取权限码方式：usePageButtons() 从当前路由末段取 code（/system/workflow -> 'workflow'），
--     再 getButton('workflow') 返回该菜单的子按钮。故“进入设计”按钮的 parent_id 必须指向
--     code='workflow' 的列表菜单，且其自身 code='workflow_design' 用于前端门禁。
--   * 路径必须是 /formmode/workflowdesign（无连字符）；连字符会变成 Workflow-Design 导致匹配失败。
--
-- 本脚本幂等：已存在则跳过。
--   若“流程设计”列表菜单已在库中存在（code='workflow'），第二条 INSERT 的 parent_id 子查询会自动命中它；
--   若尚不存在，则第一条会先创建（挂在“流程建模”菜单下）。
-- =============================================================================

-- ① 流程设计列表菜单（若不存在）。path=/system/workflow 才能解析到 src/pages/System/Workflow/Workflow.tsx
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT
  2061825613800460290,                 -- id（唯一即可）
  2061823901329051649,                 -- parent_id：流程建模 菜单 id（seed 中 code='mode'）
  'workflow', '流程设计', 'workflow', '/system/workflow', 'api', 50, 1, 1, 0, 1, NULL, 0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'workflow' AND `is_deleted` = 0);

-- ② 「进入设计」操作按钮（category=2 按钮；is_component=1 触发前端自动注册路由 /formmode/workflowdesign）
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT
  2061825613800460291,                 -- id（唯一即可）
  (SELECT id FROM `blade_menu` WHERE `code` = 'workflow' AND `is_deleted` = 0 LIMIT 1),  -- 指向流程设计列表菜单
  'workflow_design', '流程设计', 'workflow_design', '/formmode/workflowdesign', '', 10, 2, 1, 0, 1, NULL, 0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'workflow_design' AND `is_deleted` = 0);
