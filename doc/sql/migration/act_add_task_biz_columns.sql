-- =============================================================================
-- 去 wf_* 表改造 · P4/P5 任务级业务列（方案 A1：扩展 ACT 表 + JdbcTemplate 读写）
-- 作用：在 Flowable 8.1 原生 ACT_RU_TASK / ACT_HI_TASKINST 上补充 blade 任务业务列，
--       使「待办/已办」列表可直接从 ACT_* 检索，无需回读 wf_task（为 wf_task 退役铺路）。
--
-- 背景：wf_task 是 ACT_RU_TASK 之上的业务富化层。经盘点（2026-09-29）：
--   · ACT 原生已覆盖：PROC_INST_ID_(instId) / TASK_DEF_KEY_(nodeKey) / ASSIGNEE_(assignee)
--                     / CREATE_TIME_(receiveTime) / DUE_DATE_(dueTime) / END_TIME_(operateTime)
--   · 以下 6 类是 wf_task 独有、ACT 没有的，必须先落列 + 双写，才能忠实翻源：
--       IS_TEST_         测试态（待办/已办列表须整组排除测试任务）
--       BUSINESS_STATUS_ blade 子状态（2已办/4办结/6自动提交/7协办/8抄送/11传阅）
--                        —— ACT_HI_TASKINST 只有「已完成」语义，无子状态，故必须落列
--       ORIGINAL_USER_   代理人代办时的原处理人
--       SIGN_ORDER_      会签关系
--       VIEW_TIME_       首次查看时间（流程轨迹「已查看」判定）
--       TIMEOUT_HANDLED_ 超时动作是否已执行（防重复触发）
--
-- ⚠️ 安全前提（务必先读）：
--   1. 本脚本 USE blade；jeelowcode 模块也有 ACT_* 表，严禁跨库执行。
--   2. 列/索引添加均经 INFORMATION_SCHEMA 幂等守卫，可重复执行不报错。
--   3. ACT_RU_TASK.BUSINESS_STATUS_ 已由 P3-1 的 act_add_columns.sql 添加，
--      此处再次调用会被幂等守卫跳过（安全）。
--   4. 不加 TENANT_ID_ —— Flowable 8.1 标准表已带该列。
--   5. DUE_DATE_ 由 Flowable 8101 升级脚本自动补齐（schemaUpdate=true），此处不重复加。
--   6. 加列 ALGORITHM=INSTANT（不可与 LOCK 组合，否则 ERROR 1221）；
--      索引 ALGORITHM=INPLACE,LOCK=NONE（在线 DDL）。INSTANT 不支持时改用 INPLACE。
-- =============================================================================

USE blade;

DELIMITER $$

-- 幂等加列：仅当列不存在时才 ALTER（与 act_add_columns.sql 同范式，重复定义无害）
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

-- 幂等加索引：仅当索引不存在时才创建
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

-- ───────────────────────────── ACT_RU_TASK（待办） ─────────────────────────────
CALL blade_add_col('ACT_RU_TASK', 'BUSINESS_STATUS_', "varchar(32)  DEFAULT NULL COMMENT 'blade 任务子状态(TODO/DONE/FINISHED/AUTO_SUBMIT/COADJUTANT/CIRCULATE/READ)'");
CALL blade_add_col('ACT_RU_TASK', 'IS_TEST_',         "tinyint      DEFAULT 0    COMMENT '是否测试态任务 0否1是（测试任务不进生产待办/已办列表）'");
CALL blade_add_col('ACT_RU_TASK', 'ORIGINAL_USER_',   "varchar(64)  DEFAULT NULL COMMENT '代理人代办时的原处理人ID'");
CALL blade_add_col('ACT_RU_TASK', 'SIGN_ORDER_',      "int          DEFAULT NULL COMMENT '会签关系（对齐 wf_task.sign_order）'");
CALL blade_add_col('ACT_RU_TASK', 'VIEW_TIME_',       "datetime(3)  DEFAULT NULL COMMENT '首次查看时间（流程轨迹已查看判定）'");
CALL blade_add_col('ACT_RU_TASK', 'TIMEOUT_HANDLED_', "tinyint      DEFAULT 0    COMMENT '超时动作是否已执行 0未1已（防重复触发）'");
-- blade 业务侧任务ID（wf_task.id）：待办/已办翻源后，列表必须继续吐【业务任务ID】——
-- 操作接口（approve/转办/退回/查看）一律按 wf_task.id 查（requireTodoTask → selectById），
-- 若列表改吐引擎任务 ID_ 会导致「点同意查不到任务」。故必须冗余此列。
CALL blade_add_col('ACT_RU_TASK', 'BIZ_TASK_ID_',    "bigint       DEFAULT NULL COMMENT 'blade 业务任务ID(wf_task.id)：翻源后列表仍吐此ID，操作链路不变'");

-- 待办列表高频检索：按办理人 + 测试态过滤
CALL blade_add_idx('ACT_RU_TASK', 'IDX_RU_TASK_ASSIGNEE_TEST', 'ASSIGNEE_, IS_TEST_');
CALL blade_add_idx('ACT_RU_TASK', 'IDX_RU_TASK_INST_NODE',     'PROC_INST_ID_, TASK_DEF_KEY_');

-- ───────────────────────────── ACT_HI_TASKINST（已办） ─────────────────────────────
-- history=audit 下 Flowable 在【任务创建时】即落 HI 行、完成时补 END_TIME_，
-- 故业务列必须在创建时同步写入 HI 表，否则任务完成（RU 行删除）后业务维度丢失。
CALL blade_add_col('ACT_HI_TASKINST', 'BUSINESS_STATUS_', "varchar(32)  DEFAULT NULL COMMENT 'blade 任务子状态(DONE/FINISHED/AUTO_SUBMIT/COADJUTANT/CIRCULATE/READ)'");
CALL blade_add_col('ACT_HI_TASKINST', 'IS_TEST_',         "tinyint      DEFAULT 0    COMMENT '是否测试态任务 0否1是'");
CALL blade_add_col('ACT_HI_TASKINST', 'ORIGINAL_USER_',   "varchar(64)  DEFAULT NULL COMMENT '代理人代办时的原处理人ID'");
CALL blade_add_col('ACT_HI_TASKINST', 'SIGN_ORDER_',      "int          DEFAULT NULL COMMENT '会签关系'");
CALL blade_add_col('ACT_HI_TASKINST', 'VIEW_TIME_',       "datetime(3)  DEFAULT NULL COMMENT '首次查看时间'");
CALL blade_add_col('ACT_HI_TASKINST', 'TIMEOUT_HANDLED_', "tinyint      DEFAULT 0    COMMENT '超时动作是否已执行 0未1已'");
CALL blade_add_col('ACT_HI_TASKINST', 'BIZ_TASK_ID_',    "bigint       DEFAULT NULL COMMENT 'blade 业务任务ID(wf_task.id)：翻源后列表仍吐此ID'");

-- 已办列表高频检索：按办理人 + 完成时间 + 测试态过滤（done 读 END_TIME_ IS NOT NULL）
CALL blade_add_idx('ACT_HI_TASKINST', 'IDX_HI_TASK_ASSIGNEE_END',  'ASSIGNEE_, END_TIME_, IS_TEST_');
CALL blade_add_idx('ACT_HI_TASKINST', 'IDX_HI_TASK_INST_NODE',     'PROC_INST_ID_, TASK_DEF_KEY_');

-- =============================================================================
-- 校验：确认 6 列 + 索引均已就位（两表各 6 列应为 6；索引各 2）
-- =============================================================================
SELECT TABLE_NAME, COUNT(*) AS col_cnt
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'blade'
  AND TABLE_NAME IN ('ACT_RU_TASK', 'ACT_HI_TASKINST')
  AND COLUMN_NAME IN ('BUSINESS_STATUS_','IS_TEST_','ORIGINAL_USER_','SIGN_ORDER_','VIEW_TIME_','TIMEOUT_HANDLED_','BIZ_TASK_ID_')
GROUP BY TABLE_NAME;

SELECT TABLE_NAME, INDEX_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'blade'
  AND INDEX_NAME IN ('IDX_RU_TASK_ASSIGNEE_TEST','IDX_RU_TASK_INST_NODE','IDX_HI_TASK_ASSIGNEE_END','IDX_HI_TASK_INST_NODE')
GROUP BY TABLE_NAME, INDEX_NAME;
