-- =============================================================================
-- 迁移：wf_process_definition.test_proc_def_id —— 测试态配置读源指向「测试部署」
--
-- 背景（真实用户困惑，见 doc/md/工作流清库-新模型验证.md 步骤 4）
--   画布「保存」只更新草稿 bpmn_xml，**不刷新部署**；而配置面板（节点信息 / 出口信息）
--   按 wf_process_definition.proc_def_id 读**已部署**的 BPMN 模型。于是：
--     在设计器配好操作菜单/表单内容 → 「保存」→「测 试」→ 打开测试页
--   proc_def_id 仍指向上一次**正式**部署（可能是很久以前的、没有 extJson 的版本），
--   面板读到的 extJson 为空 → 表现为「我明明配了，操作菜单却没了」。
--
--   另有版本错配：deploy-test 生成的 __test 部署（带最新配置）不被面板读取，
--   导致「测试实际跑的 BPMN」与「配置面板展示的 BPMN」是两个不同版本。
--
-- 解决
--   记录测试态最近一次测试部署的 procDefId；配置展示类读源
--   （WfBpmnExtensionReader#nodes/#links）在 status=3（测试态）时优先读它。
--
-- 边界（刻意收窄，避免影响运行期）
--   · 仅 nodes()/links() 使用；node()/operators() 与运行期共用，仍读正式部署 proc_def_id，
--     以免真实实例在测试态读到被 neutralizeForTest 降级过的元素
--     （businessRuleTask / ThrowEvent / 带定时器事件 → manualTask）。
--   · status=1（已发布）时不使用本列，面板照旧读正式部署。
--
-- 说明
--   · 全部可空：存量行保持 NULL（= 从未点过「测 试」），行为与本次迁移前完全一致。
--   · 仅加列、不改类型、不删列（遵循迁移规范：加列一律可空 + 默认值）。
--   · 幂等：information_schema 守卫 + PREPARE 动态 SQL + 尾部校验，可重复执行。
--   · 用 DATABASE() 而非硬编码库名，使脚本在各环境（blade / blade_workflow）通用。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 加列
-- -----------------------------------------------------------------------------
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_process_definition'
      AND COLUMN_NAME = 'test_proc_def_id') = 0,
  "ALTER TABLE wf_process_definition ADD COLUMN test_proc_def_id VARCHAR(64) DEFAULT NULL COMMENT '测试态最近一次测试部署的 processDefinitionId（procKey__test）；配置面板在 status=3 时优先读它；NULL=从未测试部署'",
  "SELECT 'skip: wf_process_definition.test_proc_def_id already exists'"
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. 校验（期望 1）
-- -----------------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_process_definition'
      AND COLUMN_NAME = 'test_proc_def_id') AS has_test_proc_def_id;

-- -----------------------------------------------------------------------------
-- 3. 存量回填：把当前处于测试态(status=3)、且已有 __test 部署的定义补上
--    test_proc_def_id，使其配置面板立刻与测试运行版本一致。
--    不存在 __test 部署时保持 NULL（安全，等用户下次点「测 试」自动写入）。
-- -----------------------------------------------------------------------------
UPDATE wf_process_definition d
JOIN (
  SELECT LEFT(p.KEY_, CHAR_LENGTH(p.KEY_) - 6) AS base_key,
         MAX(p.ID_) AS proc_def_id
  FROM ACT_RE_PROCDEF p
  WHERE RIGHT(p.KEY_, 6) = '__test'
  GROUP BY LEFT(p.KEY_, CHAR_LENGTH(p.KEY_) - 6)
) t ON t.base_key = d.proc_key
SET d.test_proc_def_id = t.proc_def_id
WHERE d.status = 3
  AND (d.test_proc_def_id IS NULL OR d.test_proc_def_id = '');

SELECT id, proc_key, status, test_proc_def_id
FROM wf_process_definition
WHERE test_proc_def_id IS NOT NULL;

-- =============================================================================
-- 回滚脚本（如需撤销，取消注释执行）
--   ALTER TABLE wf_process_definition DROP COLUMN test_proc_def_id;
-- 行为回滚：不加列亦可 —— resolveConfigProcDefId 在 test_proc_def_id 为 NULL 时
--           自动回退读正式部署，与本次迁移前一致。
-- =============================================================================
