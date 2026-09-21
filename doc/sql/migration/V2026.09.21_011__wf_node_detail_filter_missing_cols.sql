-- -----------------------------------------------------------------------------
-- 迁移脚本：为 wf_node_detail_filter 补齐 BaseEntity 必备列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.21_011
-- 目标库    : blade_workflow
-- 影响表    : wf_node_detail_filter（修正）
-- 变更类型  : 改表（DDL）
-- 是否幂等  : 是（information_schema 列存在性守卫 + PREPARE 动态 SQL）
-- 是否丢数据: 否
-- 回滚脚本  : ALTER TABLE `wf_node_detail_filter`
--               DROP COLUMN `create_dept`, DROP COLUMN `status`, DROP COLUMN `is_deleted`;
--
-- 背景：原 V2026.09.21_003 建表漏建 BaseEntity 继承链要求的
--   create_dept / status / is_deleted 三列，导致 WfNodeDetailFilterMapper 查询时报
--   Unknown column 'create_dept' in 'field list'。此处幂等补齐。
-- =============================================================================

USE `blade_workflow`;

-- 1) create_dept ---------------------------------------------------------------
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_detail_filter'
    AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0,
  'ALTER TABLE `wf_node_detail_filter` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) status -------------------------------------------------------------------
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_detail_filter'
    AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0,
  'ALTER TABLE `wf_node_detail_filter` ADD COLUMN `status` INT DEFAULT 1 COMMENT ''业务状态[1:正常]''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) is_deleted ---------------------------------------------------------------
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_detail_filter'
    AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0,
  'ALTER TABLE `wf_node_detail_filter` ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否已删除[0:未删除,1:删除]''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 校验 ------------------------------------------------------------------------
SELECT
  (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_detail_filter'
      AND COLUMN_NAME = 'create_dept') AS create_dept_should_be_1,
  (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_detail_filter'
      AND COLUMN_NAME = 'status') AS status_should_be_1,
  (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_detail_filter'
      AND COLUMN_NAME = 'is_deleted') AS is_deleted_should_be_1;
