-- -----------------------------------------------------------------------------
-- 迁移脚本：wf_process_definition 补版本组锚点列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.13_001
-- 目标库    : blade_workflow
-- 影响表    : wf_process_definition
-- 变更类型  : 加列（DDL）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  : ALTER TABLE `wf_process_definition` DROP COLUMN `active_version_id`;
--
-- 背景（流程版本控制）
--   参考 ecology workflow_base.activeVersionID 的版本组模型：
--   同一流程的多个版本（v1/v2/v3…，同 proc_key、version 递增）各自一行记录，
--   组内每行的 active_version_id 都指向「当前激活版本」的 defId；
--   版本切换/激活 = 改锚点；新发起实例始终使用激活版本，在途实例由
--   Flowable 原生绑定创建时的 ACT_RE_PROCDEF.ID_ 隔离，不受新版本影响。
--
--   存量数据兼容：列加好后 active_version_id 全为 NULL，视为「单版本流程」
--   （组 = 自身）；首次 deploy/activate 时回填为自身 id 完成初始化。
--
-- ⚠️ 执行顺序：先执行本脚本，再重启 blade-workflow。
-- ⚠️ 严禁使用 blade-service/blade-workflow/src/main/resources/sql/workflow-schema.sql
--    来"同步结构"——该脚本会 DROP TABLE 后重建，清空全部业务数据。
-- =============================================================================

USE `blade_workflow`;

SET @v_db  = DATABASE();
SET @v_tbl = 'wf_process_definition';

-- active_version_id -----------------------------------------------------------
SET @v_col = 'active_version_id';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_process_definition`
         ADD COLUMN `active_version_id` BIGINT UNSIGNED NULL
         COMMENT ''版本组锚点：指向当前激活版本的 defId；首版=自身id，NULL=单版本流程（组=自身）''
         AFTER `version`',
    'SELECT ''SKIP: wf_process_definition.active_version_id already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 版本组定位辅助索引（组查询 = active_version_id = ? OR id = ?）----------------
SET @v_idx = 'idx_active_version';
SET @v_idx_exists = (
    SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND INDEX_NAME = @v_idx
);
SET @v_sql = IF(
    @v_idx_exists = 0,
    'ALTER TABLE `wf_process_definition` ADD INDEX `idx_active_version` (`active_version_id`)',
    'SELECT ''SKIP: idx_active_version already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 校验（应返回 1）--------------------------------------------------------------
SELECT COUNT(1) AS active_version_id_should_be_1
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'wf_process_definition'
  AND COLUMN_NAME  = 'active_version_id';
