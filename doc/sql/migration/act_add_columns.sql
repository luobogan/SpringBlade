-- =============================================================================
-- 去 wf_* 表改造 · P3-1 运行期原生业务列
-- 作用：在 Flowable 8.1 原生 ACT_* 表上补充 blade 业务列 / 复合索引，使运行期台账
--       可直接从 ACT_* 检索，无需回写 wf_instance/wf_task。
--
-- ⚠️ 安全前提（务必先读）：
--   1. 本脚本 USE blade；jeelowcode 模块也有 ACT_* 表，严禁跨库执行。
--   2. 列/索引添加均经 INFORMATION_SCHEMA 幂等守卫，可重复执行不报错。
--   3. 不加 TENANT_ID_ —— Flowable 8.1 已在 ACT_HI_PROCINST/ACT_RU_TASK 等标准带该列。
--   4. 不加 ACT_RU_TASK.DUE_DATE_ —— Flowable 8.1 的 8101 升级脚本（schemaUpdate=true
--      已在 FlowableConfig 开启）会自动补齐 DUE_DATE_/CLAIM_TIME_/CLAIMED_BY_，重复加会报错。
--   5. 加列用 ALGORITHM=INSTANT（INSTANT 本身即无锁，<b>不可</b>与 LOCK 子句组合，否则报
--      ERROR 1221）；加索引用 ALGORITHM=INPLACE,LOCK=NONE（在线 DDL）。若版本不支持 INSTANT，
--      请人工把列加法的 ALGORITHM=INSTANT 改为 INPLACE（仍可与 LOCK=NONE 组合）。
-- =============================================================================

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

-- ───────────────────────────── ACT_HI_PROCINST ─────────────────────────────
-- 流程实例级业务列（业务终态/检索维度）
CALL blade_add_col('ACT_HI_PROCINST', 'DEF_ID_',        "varchar(64)  DEFAULT NULL COMMENT 'blade 业务定义ID(wf_definition.id)'");
CALL blade_add_col('ACT_HI_PROCINST', 'DATA_ID_',       "varchar(64)  DEFAULT NULL COMMENT '业务数据ID(表单数据主键)'");
CALL blade_add_col('ACT_HI_PROCINST', 'FORM_ID_',       "varchar(64)  DEFAULT NULL COMMENT '表单ID'");
CALL blade_add_col('ACT_HI_PROCINST', 'TITLE_',         "varchar(255) DEFAULT NULL COMMENT '业务标题'");
CALL blade_add_col('ACT_HI_PROCINST', 'IS_TEST_',       "tinyint      DEFAULT 0   COMMENT '是否测试流程 0否1是'");
CALL blade_add_col('ACT_HI_PROCINST', 'BUSINESS_STATUS_',"varchar(32)  DEFAULT NULL COMMENT '业务终态/流转状态(原生，非变量)'");

-- 复合索引（多租户逻辑隔离 + 高频检索）
CALL blade_add_idx('ACT_HI_PROCINST', 'IDX_HI_PROC_TENANT_STATUS', 'TENANT_ID_, BUSINESS_STATUS_');
CALL blade_add_idx('ACT_HI_PROCINST', 'IDX_HI_PROC_DEF_DATA',     'DEF_ID_, DATA_ID_');
CALL blade_add_idx('ACT_HI_PROCINST', 'IDX_HI_PROC_TEST',         'TENANT_ID_, IS_TEST_, END_TIME_');

-- ───────────────────────────── ACT_RU_TASK ─────────────────────────────
-- 任务级业务列（任务业务状态经 PROC_INST_ID_ 可 JOIN 实例表；此处冗余列便于直查，可选）
CALL blade_add_col('ACT_RU_TASK', 'BUSINESS_STATUS_', "varchar(32) DEFAULT NULL COMMENT '任务业务状态(经PROC_INST_ID_可JOIN实例)'");

CALL blade_add_idx('ACT_RU_TASK', 'IDX_RU_TASK_TENANT_STATUS', 'TENANT_ID_, BUSINESS_STATUS_');

-- ───────────────────────────── ACT_HI_PROCINST 业务维度补全 ─────────────────────────────
-- 对齐 wf_instance 全字段（去 wf_ 表后这些维度改由 ACT_HI_PROCINST 原生业务列承载）。
-- 关键点：BUSINESS_ID_ 承载原 wf_instance.id 的雪花值，业务表 formtable_main_*.request_id
--         仍指向该雪花值，迁移后 InstanceVO.id 继续返回雪花，外键不断裂。
CALL blade_add_col('ACT_HI_PROCINST', 'BUSINESS_ID_',           "bigint       DEFAULT NULL COMMENT 'blade 业务实例ID(原 wf_instance.id 雪花, 业务表 request_id 外键)'");
CALL blade_add_col('ACT_HI_PROCINST', 'STARTER_',               "bigint       DEFAULT NULL COMMENT '发起人(原 wf_instance.starter)'");
CALL blade_add_col('ACT_HI_PROCINST', 'CURRENT_NODE_KEY_',       "varchar(64)  DEFAULT NULL COMMENT '当前节点Key(advance 维护, 并行分支安全)'");
CALL blade_add_col('ACT_HI_PROCINST', 'URGENCY_',               "int          DEFAULT 0   COMMENT '紧急程度 0/1/2'");
CALL blade_add_col('ACT_HI_PROCINST', 'BUSINESS_ROW_READY_',     "tinyint      DEFAULT NULL COMMENT 'L3自检:业务行就绪 1/0/NULL'");
CALL blade_add_col('ACT_HI_PROCINST', 'REQUEST_ID_BOUND_',       "tinyint      DEFAULT NULL COMMENT 'L3自检:request_id 回填 1/0/NULL'");
CALL blade_add_col('ACT_HI_PROCINST', 'ENGINE_DEPLOY_MATCHED_',  "tinyint      DEFAULT NULL COMMENT 'L3自检:引擎latest==定义部署 1/0/NULL'");
CALL blade_add_col('ACT_HI_PROCINST', 'PARENT_ID_',             "bigint       DEFAULT NULL COMMENT '父流程实例ID(子流程)'");
CALL blade_add_col('ACT_HI_PROCINST', 'TEST_DEPLOYMENT_ID_',     "varchar(64)  DEFAULT NULL COMMENT '测试临时部署ID(清理用)'");
CALL blade_add_col('ACT_HI_PROCINST', 'PENDING_STATUS_',         "int          DEFAULT NULL COMMENT '期望终态intent 1通过/2不通过/3撤销/NULL'");

-- 高频检索复合索引（多租户逻辑隔离 + 读源切换查询）
CALL blade_add_idx('ACT_HI_PROCINST', 'IDX_HI_PROC_BUSINESS_ID', 'BUSINESS_ID_');
CALL blade_add_idx('ACT_HI_PROCINST', 'IDX_HI_PROC_STARTER',     'TENANT_ID_, STARTER_, IS_TEST_, START_TIME_');
CALL blade_add_idx('ACT_HI_PROCINST', 'IDX_HI_PROC_FORM',        'TENANT_ID_, FORM_ID_, IS_TEST_');
CALL blade_add_idx('ACT_HI_PROCINST', 'IDX_HI_PROC_BIZKEY',      'TENANT_ID_, BUSINESS_KEY_');

-- ───────────────────────────── 清理临时过程 ─────────────────────────────
DROP PROCEDURE IF EXISTS blade_add_col;
DROP PROCEDURE IF EXISTS blade_add_idx;
