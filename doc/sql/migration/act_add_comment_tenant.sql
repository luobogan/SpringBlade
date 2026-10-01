-- =============================================================
-- D8 / H2 / R7：ACT_HI_COMMENT 新增租户列 TENANT_ID_
-- 出处：《Flowable8承接台账模块-去wf_表改造分析.md》§15.3 H2、§17.1 D8、§17.3 R7
--       决策结论：《去wf_表-D系列决策结论.md》D8（2026-10-01 拍板：加列）
--
-- 背景：act_hi_comment 原生无 TENANT_ID_ 列（§13.8 实测），审批日志按租户检索
--       只能 JOIN ACT_HI_PROCINST（PROC_INST_ID_）继承租户。为支撑
--       「按租户横扫审批意见」高频场景，决策直接加列；既有按 PROC_INST_ID_
--       驱动的查询不受影响，列值与 JOIN 推导值必须完全一致（V22）。
--
-- 写侧：ProcessServiceImpl.addComment 写入意见后，由 WfCommentTenantWriter
--       从 ACT_HI_PROCINST.TENANT_ID_ 反查回填本列
--       （开关 blade.workflow.comment-tenant-write，见 application.yml）。
--
-- ⚠️ 硬约束（§11.1.5）：新增列必须 NULL-able 且带 DEFAULT（''），
--    否则 MySQL 严格模式下引擎 INSERT 缺列直接失败 → 审批意见写入失败。
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
CALL blade_add_col('ACT_HI_COMMENT', 'TENANT_ID_', "VARCHAR(64) NULL DEFAULT '' COMMENT '租户ID（D8 决策新增）：按租户横扫审批意见；写侧自 ACT_HI_PROCINST 反查回填'");

-- 2) 索引：横扫场景 = 等值租户过滤 + 时间排序（equality first, then range/sort）
CALL blade_add_idx('ACT_HI_COMMENT', 'IDX_HI_COMMENT_TENANT', 'TENANT_ID_, TIME_');

-- 清理临时过程
DROP PROCEDURE IF EXISTS blade_add_col;
DROP PROCEDURE IF EXISTS blade_add_idx;

-- 3) 存量回填（幂等：仅补 TENANT_ID_ 为空且能关联到实例的行）
--    先核对匹配行数，再 UPDATE（防漏回填）
SELECT COUNT(*) AS backfill_candidates
  FROM ACT_HI_COMMENT c
  JOIN ACT_HI_PROCINST p ON p.PROC_INST_ID_ = c.PROC_INST_ID_
 WHERE (c.TENANT_ID_ IS NULL OR c.TENANT_ID_ = '')
   AND p.TENANT_ID_ IS NOT NULL AND p.TENANT_ID_ <> '';

UPDATE ACT_HI_COMMENT c
  JOIN ACT_HI_PROCINST p ON p.PROC_INST_ID_ = c.PROC_INST_ID_
   SET c.TENANT_ID_ = p.TENANT_ID_
 WHERE (c.TENANT_ID_ IS NULL OR c.TENANT_ID_ = '')
   AND p.TENANT_ID_ IS NOT NULL AND p.TENANT_ID_ <> '';

-- 4) 校验：总数 / 已有租户 / 仍为空
--    仍为空的行 = 无法关联实例（PROC_INST_ID_ 为空的任务级意见）或实例本身无租户，
--    需人工核对，不阻塞。
SELECT COUNT(*) AS total,
       SUM(CASE WHEN TENANT_ID_ IS NOT NULL AND TENANT_ID_ <> '' THEN 1 ELSE 0 END) AS with_tenant,
       SUM(CASE WHEN TENANT_ID_ IS NULL OR TENANT_ID_ = '' THEN 1 ELSE 0 END) AS missing
  FROM ACT_HI_COMMENT;

-- 5) V22 一致性：列值与 JOIN 实例表推导值必须完全一致（mismatch 应为 0）
SELECT COUNT(*) AS tenant_mismatch
  FROM ACT_HI_COMMENT c
  JOIN ACT_HI_PROCINST p ON p.PROC_INST_ID_ = c.PROC_INST_ID_
 WHERE c.TENANT_ID_ IS NOT NULL AND c.TENANT_ID_ <> ''
   AND p.TENANT_ID_ IS NOT NULL AND p.TENANT_ID_ <> ''
   AND c.TENANT_ID_ <> p.TENANT_ID_;

-- 6) EXPLAIN：横扫场景索引命中验证（V1 方法论；预期 type=ref 命中 IDX_HI_COMMENT_TENANT）
EXPLAIN SELECT TYPE_, TIME_, MESSAGE_
  FROM ACT_HI_COMMENT
 WHERE TENANT_ID_ = '000000'
 ORDER BY TIME_ DESC;
