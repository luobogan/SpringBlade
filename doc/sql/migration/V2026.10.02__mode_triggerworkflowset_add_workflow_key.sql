-- =====================================================================
-- V2026.10.02__mode_triggerworkflowset_add_workflow_key.sql
-- 任务 2（「以 Flowable 为唯一事实源」改造）：mode_triggerworkflowset 新增 workflow_key 列。
-- 说明：tenant_id 列已存在（见 blade.sql:6754），本脚本只加 workflow_key（绑定 Flowable procKey）。
--       workflowid（wf_process_definition.id）保留一个版本周期作为回退，不立即删除。
-- 幂等：用 information_schema 守卫，列已存在则跳过 ALTER。属一次性 DDL，执行前请确认环境。
-- 执行库：blade（formmode 主库）。
-- =====================================================================

SET @col := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mode_triggerworkflowset' AND COLUMN_NAME = 'workflow_key'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE mode_triggerworkflowset ADD COLUMN workflow_key VARCHAR(255) NULL COMMENT ''流程定义Key(BPMN process id, Flowable 事实源)'' AFTER workflowname',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
