-- -----------------------------------------------------------------------------
-- 迁移脚本：wf_instance.engine_inst_id 改为可空
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.21_012
-- 目标库    : blade_workflow
-- 影响表    : wf_instance（修正）
-- 变更类型  : 改列（DDL）+ 数据规整（DML）
-- 是否幂等  : 是（MODIFY 可重复执行；UPDATE ''→NULL 无行时影响 0 行）
-- 是否丢数据: 否（仅把空串 NORMALIZE 为 NULL）
-- 回滚脚本  : ALTER TABLE `wf_instance` MODIFY COLUMN `engine_inst_id` VARCHAR(64)
--               NOT NULL DEFAULT '' COMMENT '引擎实例ID（Flowable PROC_INST_ID_），弱关联';
--
-- 背景：uk_engine_inst 唯一键作用在 engine_inst_id 上，而该列原为 NOT NULL DEFAULT ''。
--   草稿（saveDraft）未进引擎、engine_inst_id 恒为 ''，第二条草稿即报
--   Duplicate entry '' for key 'wf_instance.uk_engine_inst'。
--   语义上「未进引擎」应为 NULL（MySQL 唯一索引允许多个 NULL），故改为可空，并把历史
--   遗留的 '' 规整为 NULL。
-- =============================================================================

USE `blade_workflow`;

-- 1) 改列为可空 ---------------------------------------------------------------
ALTER TABLE `wf_instance`
  MODIFY COLUMN `engine_inst_id` VARCHAR(64) NULL DEFAULT NULL
  COMMENT '引擎实例ID（Flowable PROC_INST_ID_），弱关联，不依赖 ACT_* 表；草稿等未进引擎的实例为 NULL（唯一索引允许多个 NULL）';

-- 2) 历史 '' 规整为 NULL（草稿遗留；影响 0 行即表示无需处理）--------------------
UPDATE `wf_instance` SET `engine_inst_id` = NULL WHERE `engine_inst_id` = '';

-- 校验 ------------------------------------------------------------------------
SELECT
  COLUMN_NAME,
  IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_instance'
  AND COLUMN_NAME = 'engine_inst_id';
