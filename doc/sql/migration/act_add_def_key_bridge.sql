-- =============================================================
-- R1 / D6 / M1：业务定义 ↔ 引擎定义 1:N 的桥接列 DEF_KEY_
-- 出处：《Flowable8承接台账模块-去wf_表改造分析.md》§15.1 M1、§17.1 D6、§17.3 R1
--
-- 背景：wf_process_definition（业务 defId）对应 ACT_RE_PROCDEF 的多个版本
--       （每次部署生成新 ID_，KEY_ 不变）。§11 只加了 DEF_ID_，
--       【无法从引擎 PROC_DEF_ID_ 反查业务 defId / 版本组】。
-- 方案：在 ACT_HI_PROCINST 冗余引擎 KEY_（= ACT_RE_PROCDEF.KEY_），
--       配合既有桥接表 flow_def_bridge（procKey → defId）即可双向反查。
--
-- ⚠️ 硬约束（§11.1.5）：新增列必须 NULL-able 且带 DEFAULT，
--    否则 MySQL 严格模式下引擎 INSERT 会因缺列失败，直接导致流程发起失败。
-- ⚠️ 必须显式 USE blade：jeelowcode 库同样存在 ACT_* 表。
-- ⚠️ 幂等：加列/加索引经 INFORMATION_SCHEMA 守卫，重复执行 SKIP，不报 duplicate。
-- =============================================================
USE blade;

DELIMITER $$

-- 幂等加列：仅当列不存在时才 ALTER。
DROP PROCEDURE IF EXISTS blade_add_col$$
CREATE PROCEDURE blade_add_col(
  IN p_tbl VARCHAR(64), IN p_col VARCHAR(64), IN p_def VARCHAR(512))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'blade' AND TABLE_NAME = p_tbl AND COLUMN_NAME = p_col
  ) THEN
    SET @sql = CONCAT('ALTER TABLE ', p_tbl, ' ADD COLUMN ', p_col, ' ', p_def, ', ALGORITHM=INSTANT');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    SELECT CONCAT('ADDED ', p_tbl, '.', p_col) AS result;
  ELSE
    SELECT CONCAT('SKIP  ', p_tbl, '.', p_col, ' (exists)') AS result;
  END IF;
END$$

-- 幂等加索引：仅当索引不存在时才创建。
DROP PROCEDURE IF EXISTS blade_add_idx$$
CREATE PROCEDURE blade_add_idx(
  IN p_tbl VARCHAR(64), IN p_idx VARCHAR(64), IN p_cols VARCHAR(512))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = 'blade' AND TABLE_NAME = p_tbl AND INDEX_NAME = p_idx
  ) THEN
    SET @sql = CONCAT('ALTER TABLE ', p_tbl, ' ADD INDEX ', p_idx, ' (', p_cols, '), ALGORITHM=INPLACE, LOCK=NONE');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    SELECT CONCAT('ADDED INDEX ', p_tbl, '.', p_idx) AS result;
  ELSE
    SELECT CONCAT('SKIP  INDEX ', p_tbl, '.', p_idx, ' (exists)') AS result;
  END IF;
END$$

DELIMITER ;

-- 1) 加列（幂等守卫：列已存在则 SKIP，可重复执行）
CALL blade_add_col('ACT_HI_PROCINST', 'DEF_KEY_', "VARCHAR(255) NULL DEFAULT NULL COMMENT '引擎流程定义KEY（ACT_RE_PROCDEF.KEY_）；桥接业务defId与引擎版本组'");

-- 2) 索引：多域共用按 TENANT_ID_ 复合（§13.3 修订口径）
CALL blade_add_idx('ACT_HI_PROCINST', 'IDX_HI_PROC_T_DEFKEY', 'TENANT_ID_, DEF_KEY_');

-- 清理临时过程
DROP PROCEDURE IF EXISTS blade_add_col;
DROP PROCEDURE IF EXISTS blade_add_idx;

-- 3) 存量回填（幂等：仅补 DEF_KEY_ 为空且有 PROC_DEF_ID_ 的行）
--    先核对匹配行数，再 UPDATE（防漏回填）
SELECT COUNT(*) AS backfill_candidates
  FROM ACT_HI_PROCINST h
  JOIN ACT_RE_PROCDEF p ON p.ID_ = h.PROC_DEF_ID_
 WHERE h.DEF_KEY_ IS NULL AND h.PROC_DEF_ID_ IS NOT NULL;

UPDATE ACT_HI_PROCINST h
  JOIN ACT_RE_PROCDEF p ON p.ID_ = h.PROC_DEF_ID_
   SET h.DEF_KEY_ = p.KEY_
 WHERE h.DEF_KEY_ IS NULL AND h.PROC_DEF_ID_ IS NOT NULL;

-- 4) 校验：总数 / 已回填 / 仍缺失（缺失应为"PROC_DEF_ID_ 为空的存量孤儿行"）
SELECT COUNT(*) AS total,
       SUM(CASE WHEN DEF_KEY_ IS NOT NULL THEN 1 ELSE 0 END) AS with_key,
       SUM(CASE WHEN DEF_KEY_ IS NULL THEN 1 ELSE 0 END) AS missing
  FROM ACT_HI_PROCINST;

SELECT COUNT(*) AS missing_but_has_procdef
  FROM ACT_HI_PROCINST WHERE DEF_KEY_ IS NULL AND PROC_DEF_ID_ IS NOT NULL;
