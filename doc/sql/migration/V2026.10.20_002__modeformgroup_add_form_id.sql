-- =============================================================================
-- 迁移脚本：modeformgroup 增加 form_id（设计器定位维度）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.20_002
-- 目标库    : blade
-- 影响表    : modeformgroup
-- 变更类型  : 加列 + 加索引（DDL）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `modeformgroup` DROP INDEX `idx_mfg_form_order`;
--   ALTER TABLE `modeformgroup` DROP COLUMN `form_id`;
--
-- 背景
--   Excel 设计器（pages/FormMode/ExcelDesign）只有 formId（布局按表单归属），
--   拿不到 modeid；而 modeformgroup 原只有 modeid（模块维度）。
--   为使设计器能读写「明细表组属性」，需按「formId + 明细表序号(dsporder)」定位，
--   故补 form_id 列 + 联合索引。modeid 保留原语义（模块维度），可为空。
--
-- ⚠️ 执行顺序：V2026.10.20_001（加 6 个属性列）→ 本脚本 → 重启 blade-formmode。
-- =============================================================================

USE `blade`;

SET @v_db  = DATABASE();
SET @v_tbl = 'modeformgroup';

-- form_id --------------------------------------------------------------------
SET @v_col = 'form_id';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `modeformgroup`
         ADD COLUMN `form_id` BIGINT DEFAULT NULL
         COMMENT ''表单ID（设计器按 formId + dsporder 定位明细组）''
         AFTER `modeid`',
    'SELECT ''SKIP: modeformgroup.form_id already exists'' AS migration_info'
);
PREPARE _stmt_form_id FROM @v_sql; EXECUTE _stmt_form_id; DEALLOCATE PREPARE _stmt_form_id;

-- 索引 idx_mfg_form_order -----------------------------------------------------
SET @v_idx = 'idx_mfg_form_order';
SET @v_idx_exists = (
    SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND INDEX_NAME = @v_idx
);
SET @v_sql = IF(
    @v_idx_exists = 0,
    'ALTER TABLE `modeformgroup` ADD INDEX `idx_mfg_form_order` (`form_id`, `dsporder`)',
    'SELECT ''SKIP: modeformgroup.idx_mfg_form_order already exists'' AS migration_info'
);
PREPARE _stmt_idx FROM @v_sql; EXECUTE _stmt_idx; DEALLOCATE PREPARE _stmt_idx;

-- 校验（列应返回 1）------------------------------------------------------------
SELECT
    (SELECT COUNT(1) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'modeformgroup' AND COLUMN_NAME = 'form_id') AS form_id_should_be_1;
