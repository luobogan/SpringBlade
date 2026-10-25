-- -----------------------------------------------------------------------------
-- 迁移脚本：公文电子签章「回调后推进流程」所需字段（esign 签章完成后由回调推进引擎）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.25_004
-- 目标库    : blade（本 dev 环境各服务共用 blade 库；生产拆库则为 blade_workflow）
-- 影响表    : wf_odoc_sign_flow
-- 变更类型  : DDL（ADD COLUMN + ADD INDEX，均带默认值/可空，不动存量数据）
-- 是否幂等  : 是（information_schema 守卫 + PREPARE 动态 SQL，可重复执行）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `wf_odoc_sign_flow`
--     DROP INDEX `idx_wosf_advance`,
--     DROP COLUMN `node_key`, DROP COLUMN `task_id`, DROP COLUMN `engine_task_id`,
--     DROP COLUMN `operator`, DROP COLUMN `vars_json`, DROP COLUMN `advance_status`,
--     DROP COLUMN `advance_time`, DROP COLUMN `advance_msg`;
-- 设计文档  : doc/esign/电子签章集成技术方案.md
--
-- 背景
--   用户拍板：第三方签章（e签宝）改为「签完才流转」——提交动作不再直接推进引擎，
--   而是把「待推进上下文」落在 wf_odoc_sign_flow，签署完成（回调或补偿）后再推进引擎。
--   本表新增列即承载该上下文：
--     node_key      签章节点 Key（推进时定位节点配置、执行节点后置动作）
--     task_id       业务待办ID（wf_task.id，拒签/过期时恢复为待办用）
--     engine_task_id 引擎任务ID（推进时 completeTask 用）
--     operator      提交人（推进时作为 currentOperator，生成后续节点待办）
--     vars_json     提交变量 JSON（推进时回填引擎，保证变量不丢）
--     advance_status 推进状态 0待推进 1推进中 2已推进 3已终止(拒签/过期)
--     advance_time  推进完成时间
--     advance_msg   推进结果/失败原因
-- =============================================================================

-- 1) node_key
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'node_key');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `node_key` VARCHAR(64) DEFAULT NULL COMMENT ''签章节点Key''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) task_id
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'task_id');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `task_id` BIGINT DEFAULT NULL COMMENT ''业务待办ID(wf_task.id)''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) engine_task_id
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'engine_task_id');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `engine_task_id` VARCHAR(64) DEFAULT NULL COMMENT ''引擎任务ID(推进时completeTask)''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) operator
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'operator');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `operator` BIGINT DEFAULT NULL COMMENT ''提交人(推进时作为currentOperator)''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) vars_json
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'vars_json');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `vars_json` TEXT DEFAULT NULL COMMENT ''提交变量JSON(推进时回填引擎)''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) advance_status
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'advance_status');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `advance_status` INT NOT NULL DEFAULT 0 COMMENT ''推进状态 0待推进 1推进中 2已推进 3已终止(拒签/过期)''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7) advance_time
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'advance_time');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `advance_time` DATETIME DEFAULT NULL COMMENT ''推进完成时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 8) advance_msg
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'advance_msg');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `advance_msg` VARCHAR(500) DEFAULT NULL COMMENT ''推进结果/失败原因''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 9) 索引：补偿任务按「已签署未推进」扫描
SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND INDEX_NAME = 'idx_wosf_advance');
SET @sql := IF(@idx = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD INDEX `idx_wosf_advance` (`sign_status`, `advance_status`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
