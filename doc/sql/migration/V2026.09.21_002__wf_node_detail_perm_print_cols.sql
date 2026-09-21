-- -----------------------------------------------------------------------------
-- 迁移脚本：wf_node_detail_perm 增加「明细表打印设置」三列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.21_002
-- 目标库    : blade_workflow
-- 影响表    : wf_node_detail_perm
-- 变更类型  : 加列（DDL）
-- 是否幂等  : 是（列已存在则跳过）
-- 是否丢数据: 否
-- 回滚脚本  : ALTER TABLE `wf_node_detail_perm`
--                 DROP COLUMN `print_serial`,
--                 DROP COLUMN `allow_scroll`,
--                 DROP COLUMN `open_paging`;
--
-- 背景（对齐 ecology workflow_nodeformgroup 的 detailgroupattr 10 位权限串）：
--   本项目此前只落了「可新增/可编辑/可删除/隐藏空行/默认行数/必须新增」六维，
--   漏掉了「打印序号 / 允许滚动 / 开启分页」三维（明细表在打印态下的表现）。
--   本迁移补齐这三列（TINYINT，NULL 表示未配置 → 运行期按 0 处理）。
-- =============================================================================

USE `blade_workflow`;

SET @v_db  = DATABASE();
SET @v_tbl = 'wf_node_detail_perm';

-- print_serial 打印序号 --------------------------------------------------------
SET @v_col = 'print_serial';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_node_detail_perm`
         ADD COLUMN `print_serial` TINYINT NULL
         COMMENT ''明细表打印序号 1=打印 0=不打印 NULL=未配置（运行期按 0）''
         AFTER `required`',
    'SELECT ''SKIP: wf_node_detail_perm.print_serial already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- allow_scroll 允许滚动 --------------------------------------------------------
SET @v_col = 'allow_scroll';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_node_detail_perm`
         ADD COLUMN `allow_scroll` TINYINT NULL
         COMMENT ''明细表打印允许滚动 1=允许 0=不允许 NULL=未配置（运行期按 0）''
         AFTER `print_serial`',
    'SELECT ''SKIP: wf_node_detail_perm.allow_scroll already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- open_paging 开启分页 ---------------------------------------------------------
SET @v_col = 'open_paging';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_node_detail_perm`
         ADD COLUMN `open_paging` TINYINT NULL
         COMMENT ''明细表打印开启分页 1=分页 0=不分页 NULL=未配置（运行期按 0）''
         AFTER `allow_scroll`',
    'SELECT ''SKIP: wf_node_detail_perm.open_paging already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 校验 ------------------------------------------------------------------------
SELECT
    (SELECT COUNT(1) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_detail_perm'
        AND COLUMN_NAME = 'print_serial')   AS print_serial_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_detail_perm'
        AND COLUMN_NAME = 'allow_scroll')   AS allow_scroll_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_detail_perm'
        AND COLUMN_NAME = 'open_paging')    AS open_paging_should_be_1;
