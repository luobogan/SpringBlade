-- =============================================================================
-- 迁移 015：流程定义 / 实例记录「实际使用的 processDefinitionId」+ 灰度标记
--
-- 背景（见 doc/md/流程测试与生产上线隔离方案.md §3）
--   现状发起走 startProcessInstanceByKey（ProcessServiceImpl.java:41-47），
--   由引擎解析「该 procKey 的最新部署」——版本取决于部署时序，无法精确路由与回滚。
--   本次升级为「按 processDefinitionId 启动」（startProcessInstanceById），
--   需要把「本次实际使用的那一版」记下来，作为审计、灰度路由与回滚的依据。
--
-- 新增列
--   wf_process_definition.proc_def_id  VARCHAR(64)  激活版本对应的 ACT_RE_PROCDEF.ID_
--   wf_instance.proc_def_id            VARCHAR(64)  本实例实际使用的 ACT_RE_PROCDEF.ID_
--   wf_instance.is_gray                TINYINT      1=灰度实例（走灰度版本） 0=正式版本
--
-- 说明
--   · 全部可空：存量行保持 NULL（未知），不影响既有流程；由后续部署/发起逐步回填。
--   · 仅加列、不改类型、不删列（遵循迁移规范：加列一律可空 + 默认值）。
--   · 幂等：information_schema 守卫 + PREPARE 动态 SQL + 尾部校验，可重复执行。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. wf_process_definition.proc_def_id
-- -----------------------------------------------------------------------------
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'blade_workflow'
      AND TABLE_NAME = 'wf_process_definition'
      AND COLUMN_NAME = 'proc_def_id') = 0,
  "ALTER TABLE wf_process_definition ADD COLUMN proc_def_id VARCHAR(64) DEFAULT NULL COMMENT '激活版本对应的 Flowable processDefinitionId（ACT_RE_PROCDEF.ID_）'",
  "SELECT 'skip: wf_process_definition.proc_def_id already exists'"
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. wf_instance.proc_def_id
-- -----------------------------------------------------------------------------
SET @ddl2 = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'blade_workflow'
      AND TABLE_NAME = 'wf_instance'
      AND COLUMN_NAME = 'proc_def_id') = 0,
  "ALTER TABLE wf_instance ADD COLUMN proc_def_id VARCHAR(64) DEFAULT NULL COMMENT '本实例实际使用的 Flowable processDefinitionId（审计/回滚依据）'",
  "SELECT 'skip: wf_instance.proc_def_id already exists'"
);
PREPARE stmt2 FROM @ddl2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- -----------------------------------------------------------------------------
-- 3. wf_instance.is_gray
-- -----------------------------------------------------------------------------
SET @ddl3 = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'blade_workflow'
      AND TABLE_NAME = 'wf_instance'
      AND COLUMN_NAME = 'is_gray') = 0,
  "ALTER TABLE wf_instance ADD COLUMN is_gray TINYINT NOT NULL DEFAULT 0 COMMENT '灰度实例 1=走灰度版本 0=正式版本'",
  "SELECT 'skip: wf_instance.is_gray already exists'"
);
PREPARE stmt3 FROM @ddl3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;

-- -----------------------------------------------------------------------------
-- 4. 校验（三列都应存在；期望均为 1）
-- -----------------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='blade_workflow' AND TABLE_NAME='wf_process_definition'
      AND COLUMN_NAME='proc_def_id') AS has_def_proc_def_id,
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='blade_workflow' AND TABLE_NAME='wf_instance'
      AND COLUMN_NAME='proc_def_id') AS has_inst_proc_def_id,
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='blade_workflow' AND TABLE_NAME='wf_instance'
      AND COLUMN_NAME='is_gray') AS has_inst_is_gray;

-- =============================================================================
-- 回滚脚本（如需撤销，取消注释执行）
--   ALTER TABLE blade_workflow.wf_process_definition DROP COLUMN proc_def_id;
--   ALTER TABLE blade_workflow.wf_instance DROP COLUMN proc_def_id;
--   ALTER TABLE blade_workflow.wf_instance DROP COLUMN is_gray;
-- =============================================================================
