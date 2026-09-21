-- ============================================================================
-- 版本    : V2026.09.21_009
-- 目标库  : blade_workflow
-- 影响表  : wf_node_operator（加 group_name/can_view/协办字段）
-- 变更类型: 节点操作者补充「操作组名称与可见性」+「协办/征询意见人」
-- 是否幂等: 是（information_schema 存在性守卫 + PREPARE 动态 SQL）
-- 是否丢数据: 否
-- 回滚脚本: 见各 ALTER 反向 DROP COLUMN（按需）
-- 决策依据: 泛微节点信息「操作者」的操作组名称/可见性、协办/征询意见人。
-- ============================================================================

USE `blade_workflow`;

-- 操作组名称：从 condition_json.name 抽出为独立列，便于查询与展示
SET @v = (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='wf_node_operator' AND COLUMN_NAME='group_name');
SET @sql = IF(@v=0,
  'ALTER TABLE `wf_node_operator` ADD COLUMN `group_name` VARCHAR(64) NULL COMMENT ''操作组名称''',
  'SELECT ''skip: group_name exists'' AS migration_info');
PREPARE _s FROM @sql; EXECUTE _s; DEALLOCATE PREPARE _s;

-- 操作组可见性：1=可见 0=不可见（表单填写人是否能看到该操作组）
SET @v = (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='wf_node_operator' AND COLUMN_NAME='can_view');
SET @sql = IF(@v=0,
  'ALTER TABLE `wf_node_operator` ADD COLUMN `can_view` TINYINT NOT NULL DEFAULT 1 COMMENT ''操作组可见性 1=可见 0=不可见''',
  'SELECT ''skip: can_view exists'' AS migration_info');
PREPARE _s FROM @sql; EXECUTE _s; DEALLOCATE PREPARE _s;

-- 协办/征询意见人开关
SET @v = (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='wf_node_operator' AND COLUMN_NAME='is_coadjutant');
SET @sql = IF(@v=0,
  'ALTER TABLE `wf_node_operator` ADD COLUMN `is_coadjutant` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否协办/征询意见人 1=是 0=否''',
  'SELECT ''skip: is_coadjutant exists'' AS migration_info');
PREPARE _s FROM @sql; EXECUTE _s; DEALLOCATE PREPARE _s;

-- 协办签字类型
SET @v = (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='wf_node_operator' AND COLUMN_NAME='sign_type');
SET @sql = IF(@v=0,
  'ALTER TABLE `wf_node_operator` ADD COLUMN `sign_type` INT NULL COMMENT ''协办签字类型''',
  'SELECT ''skip: sign_type exists'' AS migration_info');
PREPARE _s FROM @sql; EXECUTE _s; DEALLOCATE PREPARE _s;

-- 系统协办
SET @v = (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='wf_node_operator' AND COLUMN_NAME='is_sys_coadjutant');
SET @sql = IF(@v=0,
  'ALTER TABLE `wf_node_operator` ADD COLUMN `is_sys_coadjutant` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否系统协办 1=是 0=否''',
  'SELECT ''skip: is_sys_coadjutant exists'' AS migration_info');
PREPARE _s FROM @sql; EXECUTE _s; DEALLOCATE PREPARE _s;

-- 提交时描述协办
SET @v = (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='wf_node_operator' AND COLUMN_NAME='is_submit_desc');
SET @sql = IF(@v=0,
  'ALTER TABLE `wf_node_operator` ADD COLUMN `is_submit_desc` TINYINT NOT NULL DEFAULT 0 COMMENT ''提交时是否填写协办描述 1=是 0=否''',
  'SELECT ''skip: is_submit_desc exists'' AS migration_info');
PREPARE _s FROM @sql; EXECUTE _s; DEALLOCATE PREPARE _s;

-- 待办（协办待处理）
SET @v = (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='wf_node_operator' AND COLUMN_NAME='is_pending');
SET @sql = IF(@v=0,
  'ALTER TABLE `wf_node_operator` ADD COLUMN `is_pending` TINYINT NOT NULL DEFAULT 0 COMMENT ''协办是否生成待办 1=是 0=否''',
  'SELECT ''skip: is_pending exists'' AS migration_info');
PREPARE _s FROM @sql; EXECUTE _s; DEALLOCATE PREPARE _s;

-- 协办可修改表单
SET @v = (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='wf_node_operator' AND COLUMN_NAME='is_modify');
SET @sql = IF(@v=0,
  'ALTER TABLE `wf_node_operator` ADD COLUMN `is_modify` TINYINT NOT NULL DEFAULT 0 COMMENT ''协办是否可修改表单 1=是 0=否''',
  'SELECT ''skip: is_modify exists'' AS migration_info');
PREPARE _s FROM @sql; EXECUTE _s; DEALLOCATE PREPARE _s;

-- 协办/征询意见人（人员id串，逗号分隔）
SET @v = (SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='wf_node_operator' AND COLUMN_NAME='coadjutants');
SET @sql = IF(@v=0,
  'ALTER TABLE `wf_node_operator` ADD COLUMN `coadjutants` VARCHAR(512) NULL COMMENT ''协办/征询意见人（人员id串，逗号分隔）''',
  'SELECT ''skip: coadjutants exists'' AS migration_info');
PREPARE _s FROM @sql; EXECUTE _s; DEALLOCATE PREPARE _s;
