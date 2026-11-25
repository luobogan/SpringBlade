-- =============================================================================
-- V2026.11.25_002：权限配置分步向导 - 授权方案模型
-- =============================================================================
-- 说明：
--   1. 旧权限数据不迁移，由上线脚本/管理端确认后清除。
--   2. 一个方案只对应一个授权对象。
--   3. subject_type: 1角色 2部门 3岗位 4人员 5分部 6矩阵 7全员
--   4. 安全级别是过滤条件，不是主体类型。
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `mode_auth_scheme` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000',
  `mode_id` bigint NOT NULL,
  `form_id` bigint NOT NULL,
  `scheme_name` varchar(200) NOT NULL,
  `subject_type` tinyint NOT NULL COMMENT '1角色 2部门 3岗位 4人员 5分部 6矩阵 7全员',
  `subject_id` bigint NOT NULL DEFAULT 0,
  `subject_name` varchar(200) DEFAULT NULL,
  `include_sub` tinyint NOT NULL DEFAULT 0,
  `matrix_value_field` varchar(64) DEFAULT NULL,
  `showlevel` int DEFAULT NULL,
  `showlevel2` int DEFAULT NULL,
  `priority` int NOT NULL DEFAULT 100,
  `status` tinyint NOT NULL DEFAULT 1,
  `version` bigint NOT NULL DEFAULT 1,
  `create_user` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_scheme_subject` (`tenant_id`, `mode_id`, `subject_type`, `subject_id`),
  KEY `idx_auth_scheme_form` (`tenant_id`, `form_id`, `status`),
  KEY `idx_auth_scheme_priority` (`tenant_id`, `mode_id`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单授权方案：一个方案对应一个授权对象';

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='moderightinfo' AND COLUMN_NAME='scheme_id');
SET @sql = IF(@col=0, 'ALTER TABLE `moderightinfo` ADD COLUMN `scheme_id` bigint NULL COMMENT ''授权方案ID''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@col=0, 'CREATE INDEX `idx_moderight_scheme` ON `moderightinfo` (`tenant_id`, `scheme_id`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='modefieldauthorize' AND COLUMN_NAME='scheme_id');
SET @sql = IF(@col=0, 'ALTER TABLE `modefieldauthorize` ADD COLUMN `scheme_id` bigint NULL COMMENT ''授权方案ID''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='mode_field_auth_group' AND COLUMN_NAME='scheme_id');
SET @sql = IF(@col=0, 'ALTER TABLE `mode_field_auth_group` ADD COLUMN `scheme_id` bigint NULL COMMENT ''授权方案ID''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@col=0, 'CREATE INDEX `idx_mfag_scheme` ON `mode_field_auth_group` (`tenant_id`, `scheme_id`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='modedatasharerule' AND COLUMN_NAME='scheme_id');
SET @sql = IF(@col=0, 'ALTER TABLE `modedatasharerule` ADD COLUMN `scheme_id` bigint NULL COMMENT ''授权方案ID''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@col=0, 'CREATE INDEX `idx_mds_rule_scheme` ON `modedatasharerule` (`tenant_id`, `scheme_id`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='modedatasharerule' AND COLUMN_NAME='scope_type');
SET @sql = IF(@col=0, 'ALTER TABLE `modedatasharerule` ADD COLUMN `scope_type` tinyint NULL COMMENT ''1全部 2本人 3所在机构 4所在机构及子级 5自定义''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='modedatasharerule' AND COLUMN_NAME='scope_column');
SET @sql = IF(@col=0, 'ALTER TABLE `modedatasharerule` ADD COLUMN `scope_column` varchar(128) NULL COMMENT ''数据范围过滤列''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='modedatasharerule' AND COLUMN_NAME='scope_value');
SET @sql = IF(@col=0, 'ALTER TABLE `modedatasharerule` ADD COLUMN `scope_value` varchar(2000) NULL COMMENT ''自定义权限值域''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='blade_menu_share' AND COLUMN_NAME='scheme_id');
SET @sql = IF(@col=0, 'ALTER TABLE `blade_menu_share` ADD COLUMN `scheme_id` bigint NULL COMMENT ''授权方案ID''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@col=0, 'CREATE INDEX `idx_menu_share_scheme` ON `blade_menu_share` (`tenant_id`, `scheme_id`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
