-- =============================================================================
-- 迁移：wf_process_definition.archive_manage / recall_manage
--      —— 流程级「归档管理」与「撤回管理」策略
--
-- 背景（对标 ecology workflow_base）
--   E9 把这两组开关放在流程基础表 workflow_base 上：
--     · isoverrb  = 归档可强制收回
--     · isoveriv  = 归档可干预
--     · isrejectremind / ischangrejectnode / isselectrejectnode = 退回/撤回提醒与节点选择
--   本项目对应的流程模板表是 wf_process_definition，节点级退回配置已在 BPMN 节点
--   extJson.settings.reject.* / preOperate 中，故这里只补齐**流程级**两块 JSON。
--
-- 解决
--   让「归档管理 / 撤回管理」成为流程级可配置项：流程设计页录入 → 落 wf_process_definition 行 →
--   流程监控按同一份配置决定「强制归档 / 强制收回 / 回退」的显隐，后端再按同一份配置做强制校验
--   （单一事实源，监控端不另存配置）。
--
-- 边界（遵循迁移规范：加列一律可空 + 默认值）
--   · 仅加列、不改类型、不删列。
--   · 全部可空：存量行 NULL = 未配置。
--     未配置口径（与后端 WfArchiveRecallPolicy / 前端 resolveArchiveRecall 完全一致）：
--       - 既有能力（允许强制归档 / 允许强制收回 / 允许监控回退）→ 保持迁移前的原有行为（开启）；
--       - 全新能力（归档后允许强制收回 / 归档后允许流程干预）→ 默认关闭，
--         需在「流程设计 → 归档与撤回」页签显式开启后可用。
--     即：本次迁移对存量流程**零行为变更**。
--   · 幂等：两列各自独立做 information_schema 守卫（单列已存在时只补另一列，
--     支持「部分已存在」的自愈），可重复执行。
--   · 用 DATABASE() 而非硬编码库名，使脚本在各环境（blade / blade_workflow）通用。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 加列：archive_manage（独立幂等）
-- -----------------------------------------------------------------------------
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_process_definition'
      AND COLUMN_NAME = 'archive_manage') = 0,
  "ALTER TABLE wf_process_definition ADD COLUMN archive_manage TEXT DEFAULT NULL COMMENT '归档管理策略JSON：强制归档/可归档节点范围/归档后可强制收回/归档后可流程干预；NULL=未配置（既有能力保持开启，全新能力默认关闭）'",
  "SELECT 'skip: wf_process_definition.archive_manage already exists'"
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. 加列：recall_manage（独立幂等）
-- -----------------------------------------------------------------------------
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_process_definition'
      AND COLUMN_NAME = 'recall_manage') = 0,
  "ALTER TABLE wf_process_definition ADD COLUMN recall_manage TEXT DEFAULT NULL COMMENT '撤回管理策略JSON：允许强制收回/允许监控回退/提醒选择总闸/强制收回落点范围；NULL=未配置（既有能力保持开启，提醒总闸默认关闭）'",
  "SELECT 'skip: wf_process_definition.recall_manage already exists'"
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 3. 校验（期望两列均为 1）
-- -----------------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_process_definition'
      AND COLUMN_NAME = 'archive_manage') AS has_archive_manage,
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_process_definition'
      AND COLUMN_NAME = 'recall_manage') AS has_recall_manage;

-- -----------------------------------------------------------------------------
-- 4. 存量现状（archive_manage_unset=1 表示未配置 → 保持迁移前行为）
--    无需回填：NULL = 未配置。
--    如需为某条流程开启，请到「流程设计 → 归档与撤回」页签配置后保存。
-- -----------------------------------------------------------------------------
SELECT id, proc_key, status,
       (archive_manage IS NULL) AS archive_manage_unset,
       (recall_manage IS NULL) AS recall_manage_unset
FROM wf_process_definition
ORDER BY id DESC
LIMIT 20;

-- =============================================================================
-- 回滚脚本（如需撤销，取消注释执行）
--   ALTER TABLE wf_process_definition DROP COLUMN archive_manage, DROP COLUMN recall_manage;
-- 行为回滚：不加列亦可 —— 解析器对 NULL 一律降级为「未配置」，
--           既有能力保持开启、全新能力关闭，与本次迁移前完全一致。
-- =============================================================================
