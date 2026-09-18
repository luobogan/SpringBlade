-- =============================================================================
-- 独立菜单：「流程测试 / 调试」
--
-- 机制（与前端 app.tsx 菜单驱动路由对齐）：
--   * 列表页（category=1）：组件路径 = ./pages/{Module}/{Page}/{Page}.tsx
--       -> /formmode/test  ->  FormMode/Test/Test.tsx
--     注：app.tsx 的 toPascalCase 会对末段做「常见单词回替」，
--         'workflowtest' 会被转成 'Workflowtest'（t 小写），与文件名大小写不一致，
--         故这里末段用 'test'（无常见单词干扰），转换结果 'Test' 与文件名完全一致。
--   * 菜单挂在「流程建模」（id=2061823901329051649）下。
--   * 后端接口复用既有 /api/blade-workflow/test/*（run / list / cleanup / detail）。
--
-- 本脚本幂等：已存在则跳过。
-- =============================================================================

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT
  2061825613800460310,                 -- id（唯一即可）
  2061823901329051649,                 -- parent_id：流程建模 菜单 id
  'workflow_test', '流程测试调试', 'workflow_test', '/formmode/test', 'api', 60, 1, 1, 0, 1,
  '独立流程测试调试页：真实引擎跑测、分支覆盖、测试数据打标与清理', 0, '000000', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'workflow_test' AND `is_deleted` = 0);
