-- =============================================================================
-- P3-6 超时扫描切源配套索引：ACT_RU_TASK.DUE_DATE_
-- =============================================================================
-- 用途：WfTimeoutJob 扫描源由 wf_task.due_time 切到原生 ACT_RU_TASK.DUE_DATE_
--       （blade.workflow 超时扫描 WHERE DUE_DATE_ <= now AND TIMEOUT_HANDLED_ 未处理），
--       按本文档 §11.3.2 / §12.9 清单补建扫描索引。
-- 说明：ACT_RU_TASK 为高频写入表，索引克制、仅建必需（§14「改源码红线」配套）。
-- 幂等：重复执行报 Duplicate key name 可忽略（或先查 information_schema.statistics）。
-- =============================================================================

USE blade;

ALTER TABLE ACT_RU_TASK ADD INDEX IDX_RU_TASK_DUE (DUE_DATE_);

-- 校验
SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME) AS cols
FROM information_schema.statistics
WHERE table_schema = 'blade' AND table_name = 'ACT_RU_TASK' AND INDEX_NAME = 'IDX_RU_TASK_DUE';
