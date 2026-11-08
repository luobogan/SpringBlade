-- =============================================================================
-- 待办/已办列表查询索引修复（本期范围第 2 项，见 doc/md/流程列表实时刷新方案与分页性能决策.md 第 10.3 节）
--
-- 问题：WfTaskActReader 用 COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_) = ? / COALESCE(IS_TEST_,0)=0
--       包住列，使 ACT_HI_TASKINST / ACT_RU_TASK 上既有 ASSIGNEE_ 索引全部无法命中，
--       实测 EXPLAIN type=ALL + Using filesort（dev MySQL 8.0.23，ACT_HI_TASKINST 326 行）。
--
-- 方案：① 回填权威列 BIZ_ASSIGNEE_，使 BIZ_ASSIGNEE_ = ? 与 COALESCE(...) = ? 完全等价；
--       ② 在 BIZ_ASSIGNEE_ 上建含排序列的复合索引；
--       ③ 配套改写 WfTaskActReader 的 WHERE 条件（见同批次 Java 改动）。
--
-- ⚠️ 执行前置（必须按顺序，且需 DBA/用户确认后才执行）：
--   1) 先跑下方「前置校验」的 3 条 SELECT，确认两表 BIZ_ASSIGNEE_ IS NULL 的分布；
--   2) 本脚本回填是幂等的，执行后再次校验「BIZ 为空但 ASSIGNEE_ 有值」应为 0；
--   3) 必须先执行本脚本（回填 + 建索引），再部署 WfTaskActReader 的 SQL 改写，
--      否则生产上若存在 BIZ 为空的行，简化条件会漏单；
--   4) dev 仅 326/37 行，优化器恒选全表，性能结论须在生产量级数据的影子库用 EXPLAIN 验证。
--
-- 本文件仅为迁移脚本，**未在本次开发中执行任何 DDL / 数据变更**。
-- =============================================================================

USE blade;

-- -----------------------------------------------------------------------------
-- 0. 前置校验（只读，先手工跑，记录结果）
-- -----------------------------------------------------------------------------
-- ① 历史表：BIZ 为空但 ASSIGNEE_ 有值（简化为 BIZ_ASSIGNEE_=? 会漏掉的行）
SELECT COUNT(*) AS hi_biz_null_assignee_present FROM ACT_HI_TASKINST
WHERE BIZ_ASSIGNEE_ IS NULL AND ASSIGNEE_ IS NOT NULL;
-- ② 运行表：同上
SELECT COUNT(*) AS ru_biz_null_assignee_present FROM ACT_RU_TASK
WHERE BIZ_ASSIGNEE_ IS NULL AND ASSIGNEE_ IS NOT NULL;
-- ③ 两表：两列都有值但不一致（写入链路漂移，先查 WfTaskActWriter）
SELECT COUNT(*) AS hi_both_set_but_differ FROM ACT_HI_TASKINST
WHERE BIZ_ASSIGNEE_ IS NOT NULL AND ASSIGNEE_ IS NOT NULL AND BIZ_ASSIGNEE_ <> ASSIGNEE_;
SELECT COUNT(*) AS ru_both_set_but_differ FROM ACT_RU_TASK
WHERE BIZ_ASSIGNEE_ IS NOT NULL AND ASSIGNEE_ IS NOT NULL AND BIZ_ASSIGNEE_ <> ASSIGNEE_;

DELIMITER $$

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

-- -----------------------------------------------------------------------------
-- 1. 回填权威列（幂等，可重复执行）
--    场景 a：BIZ 为空但引擎 ASSIGNEE_ 有值 → 直接取 ASSIGNEE_，不依赖 wf_task，
--            避免误用逻辑删除行（对应文档 9.3 M3）。
--    场景 b：两列都为空 → 仅按 BIZ_TASK_ID_ 关联**未逻辑删除**的 wf_task 补。
-- -----------------------------------------------------------------------------
UPDATE ACT_HI_TASKINST
SET BIZ_ASSIGNEE_ = ASSIGNEE_
WHERE BIZ_ASSIGNEE_ IS NULL AND ASSIGNEE_ IS NOT NULL;

UPDATE ACT_RU_TASK
SET BIZ_ASSIGNEE_ = ASSIGNEE_
WHERE BIZ_ASSIGNEE_ IS NULL AND ASSIGNEE_ IS NOT NULL;

UPDATE ACT_HI_TASKINST h
JOIN wf_task t ON h.BIZ_TASK_ID_ = t.id AND t.is_deleted = 0
SET h.BIZ_ASSIGNEE_ = t.assignee
WHERE h.BIZ_ASSIGNEE_ IS NULL AND t.assignee IS NOT NULL;

UPDATE ACT_RU_TASK r
JOIN wf_task t ON r.BIZ_TASK_ID_ = t.id AND t.is_deleted = 0
SET r.BIZ_ASSIGNEE_ = t.assignee
WHERE r.BIZ_ASSIGNEE_ IS NULL AND t.assignee IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 2. 建索引（列顺序含 ORDER BY 的排序列，用于消除 filesort）
--    已办：WHERE BIZ_ASSIGNEE_=? AND END_TIME_ IS NOT NULL ORDER BY END_TIME_ DESC
--    待办：WHERE BIZ_ASSIGNEE_=?                       ORDER BY CREATE_TIME_ DESC
-- -----------------------------------------------------------------------------
CALL blade_add_idx('ACT_HI_TASKINST', 'IDX_HI_TASK_BIZ_ASSIGNEE_END',
                   'BIZ_ASSIGNEE_, END_TIME_, IS_TEST_');
CALL blade_add_idx('ACT_RU_TASK', 'IDX_RU_TASK_BIZ_ASSIGNEE_CREATE',
                   'BIZ_ASSIGNEE_, CREATE_TIME_, IS_TEST_');

-- 多租户大表可另评估（默认不建，避免冗余索引）：
-- CALL blade_add_idx('ACT_HI_TASKINST', 'IDX_HI_TASK_T_BIZ_ASSIGNEE',
--                    'TENANT_ID_, BIZ_ASSIGNEE_, END_TIME_');

DROP PROCEDURE IF EXISTS blade_add_idx;

-- -----------------------------------------------------------------------------
-- 3. 后置校验
-- -----------------------------------------------------------------------------
-- 简化条件安全性：以下两条必须为 0
SELECT COUNT(*) AS hi_must_be_zero FROM ACT_HI_TASKINST
WHERE BIZ_ASSIGNEE_ IS NULL AND ASSIGNEE_ IS NOT NULL;
SELECT COUNT(*) AS ru_must_be_zero FROM ACT_RU_TASK
WHERE BIZ_ASSIGNEE_ IS NULL AND ASSIGNEE_ IS NOT NULL;

SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'blade'
  AND INDEX_NAME IN ('IDX_HI_TASK_BIZ_ASSIGNEE_END', 'IDX_RU_TASK_BIZ_ASSIGNEE_CREATE')
GROUP BY TABLE_NAME, INDEX_NAME;
