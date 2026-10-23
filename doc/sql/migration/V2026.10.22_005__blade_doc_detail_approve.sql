-- V2026.10.22_005__blade_doc_detail_approve.sql
-- 内容引擎 · 流程审批集成：doc_detail 增加审批流程实例ID列。幂等、库无关。
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'doc_detail' AND column_name = 'approve_inst_id');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `doc_detail` ADD COLUMN `approve_inst_id` BIGINT NULL COMMENT ''审批流程实例ID(发起审批时回填)''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
