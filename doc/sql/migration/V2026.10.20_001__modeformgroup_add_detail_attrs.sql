-- =============================================================================
-- 迁移脚本：modeformgroup 增加「明细表组属性」6 列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.20_001
-- 目标库    : blade（formmode 所有表统一在 blade 库）
-- 影响表    : modeformgroup
-- 变更类型  : 加列（DDL）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否（仅新增列，默认值保持「允许」，不改变既有明细表行为）
-- 回滚脚本  :
--   ALTER TABLE `modeformgroup` DROP COLUMN `isadd`;
--   ALTER TABLE `modeformgroup` DROP COLUMN `isedit`;
--   ALTER TABLE `modeformgroup` DROP COLUMN `isdelete`;
--   ALTER TABLE `modeformgroup` DROP COLUMN `iscopy`;
--   ALTER TABLE `modeformgroup` DROP COLUMN `isprintserial`;
--   ALTER TABLE `modeformgroup` DROP COLUMN `allowscroll`;
--
-- 背景
--   对齐泛微 E9 `modeformgroup`（设计器侧称 dtladd / dtledit / dtldelete / dtlcopy /
--   isprintserial / allowscroll）：控制某个明细表在运行期是否允许「新增 / 编辑 /
--   删除 / 复制」行、打印是否带序号、是否允许横向滚动。
--   此前本表只有 modeid/groupname/grouptype/dsporder/showcondition，明细表缺少这些
--   设计期开关（设计器里 0 命中），是明细表可用性的硬门槛。
--
--   ⚠️ 语义区分：这 6 列是**设计期开关**，与流程节点级明细权限
--      （blade-workflow 的 wf:detailTablePerm：canAdd/canEdit/...）不是同一层，
--      运行期应取「设计期开关 AND 节点权限」的叠加结果。
--
-- 默认值说明
--   isadd/isedit/isdelete/iscopy 默认 1（允许）——保证存量明细表行为不变（此前无此限制，等价允许）；
--   isprintserial（打印序号）默认 0（不打序号，与既有打印表现一致）；
--   allowscroll 默认 0（不强制横向滚动，与既有表现一致）。
--
-- ⚠️ 执行顺序：先执行本脚本，再重启/重部署 blade-formmode（实体已同步加字段）。
-- =============================================================================

USE `blade`;

SET @v_db  = DATABASE();
SET @v_tbl = 'modeformgroup';

-- isadd（允许新增明细行）-------------------------------------------------------
SET @v_col = 'isadd';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `modeformgroup`
         ADD COLUMN `isadd` TINYINT NOT NULL DEFAULT 1
         COMMENT ''是否允许新增明细行（1是 0否），对齐 ecology isadd / 设计器 dtladd''
         AFTER `showcondition`',
    'SELECT ''SKIP: modeformgroup.isadd already exists'' AS migration_info'
);
PREPARE _stmt_isadd FROM @v_sql; EXECUTE _stmt_isadd; DEALLOCATE PREPARE _stmt_isadd;

-- isedit（允许编辑明细行）------------------------------------------------------
SET @v_col = 'isedit';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `modeformgroup`
         ADD COLUMN `isedit` TINYINT NOT NULL DEFAULT 1
         COMMENT ''是否允许编辑明细行（1是 0否），对齐 ecology isedit / 设计器 dtledit''
         AFTER `isadd`',
    'SELECT ''SKIP: modeformgroup.isedit already exists'' AS migration_info'
);
PREPARE _stmt_isedit FROM @v_sql; EXECUTE _stmt_isedit; DEALLOCATE PREPARE _stmt_isedit;

-- isdelete（允许删除明细行）----------------------------------------------------
SET @v_col = 'isdelete';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `modeformgroup`
         ADD COLUMN `isdelete` TINYINT NOT NULL DEFAULT 1
         COMMENT ''是否允许删除明细行（1是 0否），对齐 ecology isdelete / 设计器 dtldelete''
         AFTER `isedit`',
    'SELECT ''SKIP: modeformgroup.isdelete already exists'' AS migration_info'
);
PREPARE _stmt_isdelete FROM @v_sql; EXECUTE _stmt_isdelete; DEALLOCATE PREPARE _stmt_isdelete;

-- iscopy（允许复制明细行）------------------------------------------------------
SET @v_col = 'iscopy';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `modeformgroup`
         ADD COLUMN `iscopy` TINYINT NOT NULL DEFAULT 1
         COMMENT ''是否允许复制明细行（1是 0否），对齐 ecology iscopy / 设计器 dtlcopy''
         AFTER `isdelete`',
    'SELECT ''SKIP: modeformgroup.iscopy already exists'' AS migration_info'
);
PREPARE _stmt_iscopy FROM @v_sql; EXECUTE _stmt_iscopy; DEALLOCATE PREPARE _stmt_iscopy;

-- isprintserial（打印显示明细序号）---------------------------------------------
SET @v_col = 'isprintserial';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `modeformgroup`
         ADD COLUMN `isprintserial` TINYINT NOT NULL DEFAULT 0
         COMMENT ''打印时是否显示明细序号（1是 0否），对齐 ecology isprintserial''
         AFTER `iscopy`',
    'SELECT ''SKIP: modeformgroup.isprintserial already exists'' AS migration_info'
);
PREPARE _stmt_isprintserial FROM @v_sql; EXECUTE _stmt_isprintserial; DEALLOCATE PREPARE _stmt_isprintserial;

-- allowscroll（明细表横向滚动）-------------------------------------------------
SET @v_col = 'allowscroll';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `modeformgroup`
         ADD COLUMN `allowscroll` TINYINT NOT NULL DEFAULT 0
         COMMENT ''明细表是否允许横向滚动（1是 0否），对齐 ecology allowscroll''
         AFTER `isprintserial`',
    'SELECT ''SKIP: modeformgroup.allowscroll already exists'' AS migration_info'
);
PREPARE _stmt_allowscroll FROM @v_sql; EXECUTE _stmt_allowscroll; DEALLOCATE PREPARE _stmt_allowscroll;

-- 校验（六列应各返回 1）--------------------------------------------------------
SELECT
    (SELECT COUNT(1) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'modeformgroup' AND COLUMN_NAME = 'isadd')          AS isadd_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'modeformgroup' AND COLUMN_NAME = 'isedit')         AS isedit_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'modeformgroup' AND COLUMN_NAME = 'isdelete')       AS isdelete_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'modeformgroup' AND COLUMN_NAME = 'iscopy')         AS iscopy_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'modeformgroup' AND COLUMN_NAME = 'isprintserial')  AS isprintserial_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'modeformgroup' AND COLUMN_NAME = 'allowscroll')    AS allowscroll_should_be_1;
