-- =============================================================================
-- 去 wf_* 表改造 · T-2 数据模型收口（§11.6.3 租户复合索引补缺）
--
-- ⚠️ 重要前提（评审 §11.6 结论）：主文档 §11.6「T-2 数据模型终稿」已严重过时。
--   实际 ACT_* 加列/索引早已由以下脚本落地（幂等 blade_add_col / blade_add_idx）：
--     · act_add_columns.sql        → ACT_HI_PROCINST 业务列 + 复合索引（含 DEF_ID_/DATA_ID_/
--                                     FORM_ID_/TITLE_/IS_TEST_/BUSINESS_STATUS_/BUSINESS_ID_/
--                                     STARTER_/CURRENT_NODE_KEY_/URGENCY_/PARENT_ID_/PENDING_STATUS_ 等）
--     · act_add_task_biz_columns.sql → ACT_RU_TASK / ACT_HI_TASKINST 任务业务列（IS_TEST_/
--                                     TIMEOUT_HANDLED_/ORIGINAL_USER_/SIGN_ORDER_/VIEW_TIME_/
--                                     BIZ_TASK_ID_/BIZ_ASSIGNEE_ 等）
--     · act_add_def_key_bridge.sql → ACT_HI_PROCINST.DEF_KEY_ + IDX_HI_PROC_T_DEFKEY
--     · act_add_comment_tenant.sql → ACT_HI_COMMENT.TENANT_ID_ + IDX_HI_COMMENT_TENANT
--     · V2026.09.30_001__act_ru_task_due_index.sql → IDX_RU_TASK_DUE(DUE_DATE_)
--   本脚本仅补齐 §11.6.3 中「TENANT_ID_ 引导的复合索引」尚未创建的 5 个，使模型与
--   设计稿（§13.3 多租户复合索引口径）完全对齐。
--
-- ⚠️ 安全前提：USE blade；列/索引添加经 INFORMATION_SCHEMA 幂等守卫，可重复执行不报错。
-- ⚠️ 已知差异（待 T-2 评审拍板，非本脚本范围）：
--     · §11.6.2 写 DEF_ID_ 为 BIGINT，实际实现为 varchar(64)（act_add_columns.sql）。
--       已实现并回填，改类型会触发回填重跑，本脚本不改；仅记录待决策。
-- =============================================================================

USE blade;

DELIMITER $$

-- 幂等加索引：仅当索引不存在时才创建（与既有 migration 同范式）
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

-- ───────────────────────────── ACT_HI_PROCINST ─────────────────────────────
-- 业务定义反查（TENANT_ID_ 引导，覆盖按租户 + defId 检索）
CALL blade_add_idx('ACT_HI_PROCINST', 'IDX_HI_PRO_T_DEFID',   'TENANT_ID_, DEF_ID_');
-- 业务数据反查（按租户 + dataId 检索）
CALL blade_add_idx('ACT_HI_PROCINST', 'IDX_HI_PRO_T_DATAID',  'TENANT_ID_, DATA_ID_');

-- ───────────────────────────── ACT_RU_TASK ─────────────────────────────
-- 超时扫描（TENANT_ID_ 引导）：WfTimeoutJob 扫 DUE_DATE_ <= now（§11.3.2 / §12.9）。
-- 注：既有 IDX_RU_TASK_DUE(DUE_DATE_) 已覆盖全局超时扫描；本复合索引服务于租户内
--     超时横扫场景，可与 IDX_RU_TASK_DUE 二选一保留，cutover 后建议择一删除以免冗余。
CALL blade_add_idx('ACT_RU_TASK', 'IDX_RU_TASK_T_DUE',       'TENANT_ID_, DUE_DATE_');
-- 超时处理去重扫描（TENANT_ID_ + 是否已处理 + 到期时间）
CALL blade_add_idx('ACT_RU_TASK', 'IDX_RU_TASK_T_TIMEOUT',   'TENANT_ID_, TIMEOUT_HANDLED_, DUE_DATE_');

-- ───────────────────────────── ACT_HI_TASKINST ─────────────────────────────
-- 已办列表按租户 + 办理人检索（TENANT_ID_ 引导；既有 IDX_HI_TASK_ASSIGNEE_END 为
--   (ASSIGNEE_, END_TIME_, IS_TEST_)，缺租户引导，多租户下本索引更优）
CALL blade_add_idx('ACT_HI_TASKINST', 'IDX_HI_TASK_T_ASSIGNEE', 'TENANT_ID_, ASSIGNEE_');

-- ───────────────────────────── 清理临时过程 ─────────────────────────────
DROP PROCEDURE IF EXISTS blade_add_idx;

-- ───────────────────────────── 校验 ─────────────────────────────
SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'blade'
  AND INDEX_NAME IN ('IDX_HI_PRO_T_DEFID','IDX_HI_PRO_T_DATAID','IDX_RU_TASK_T_DUE','IDX_RU_TASK_T_TIMEOUT','IDX_HI_TASK_T_ASSIGNEE')
GROUP BY TABLE_NAME, INDEX_NAME;
