-- -----------------------------------------------------------------------------
-- 迁移脚本：流程测试「测试态标记」+ 测试部署追踪
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.17_002
-- 目标库    : blade_workflow
-- 影响表    : wf_instance（新增 is_test、test_deployment_id）、wf_task（新增 is_test）
-- 变更类型  : DDL
-- 是否幂等  : 是（每列用 information_schema 判存在再 ALTER）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `wf_instance` DROP COLUMN `is_test`, DROP COLUMN `test_deployment_id`;
--   ALTER TABLE `wf_task` DROP COLUMN `is_test`;
--
-- 背景
--   流程测试从「配置走查」升级为「真实引擎 + 真实表单 + 测试态标记」（对齐 ecology
--   workflow_requestbase.deleted=1）：草稿流程临时部署到 Flowable 真实发起，跑完真实流转，
--   但实例/待办打 is_test=1 标记，可一键清理，不污染正常流程数据。
--   test_deployment_id 记录本次测试产生的临时部署，便于清理时级联卸载（cascade 清掉 ACT_* 数据）。

-- wf_instance.is_test
SET @c1 = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_instance' AND column_name = 'is_test');
SET @s1 = IF(@c1 = 0,
  "ALTER TABLE wf_instance ADD COLUMN is_test TINYINT NOT NULL DEFAULT 0 COMMENT '测试态标记 1=测试产生的实例/数据'",
  'SELECT 1');
PREPARE stmt FROM @s1; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- wf_instance.test_deployment_id
SET @c2 = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_instance' AND column_name = 'test_deployment_id');
SET @s2 = IF(@c2 = 0,
  "ALTER TABLE wf_instance ADD COLUMN test_deployment_id VARCHAR(64) NULL COMMENT '测试临时部署ID（清理时级联卸载）'",
  'SELECT 1');
PREPARE stmt FROM @s2; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- wf_task.is_test
SET @c3 = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wf_task' AND column_name = 'is_test');
SET @s3 = IF(@c3 = 0,
  "ALTER TABLE wf_task ADD COLUMN is_test TINYINT NOT NULL DEFAULT 0 COMMENT '测试态标记 1=测试产生的待办'",
  'SELECT 1');
PREPARE stmt FROM @s3; EXECUTE stmt; DEALLOCATE PREPARE stmt;
