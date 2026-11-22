-- =============================================================================
-- 迁移脚本 V2026.11.22_001：字段权限授权对象组
-- -----------------------------------------------------------------------------
-- 目标库：blade
-- 说明：
--   多选授权对象时，不再把字段权限复制成多条明细；改为一个“授权对象组”
--   绑定多个对象，一组字段权限只保存一份。
--   老数据 group_id=null 继续按 objtype+objid 兼容。
-- 回滚：
--   ALTER TABLE modefieldauthorize DROP COLUMN group_id;
--   DROP TABLE IF EXISTS mode_field_auth_subject;
--   DROP TABLE IF EXISTS mode_field_auth_group;
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `mode_field_auth_group` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(12) DEFAULT '000000',
  `modeid` bigint NOT NULL,
  `group_name` varchar(100) DEFAULT '',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_mfag_mode` (`tenant_id`, `modeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段权限授权对象组';

CREATE TABLE IF NOT EXISTS `mode_field_auth_subject` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(12) DEFAULT '000000',
  `group_id` bigint NOT NULL,
  `objtype` int NOT NULL,
  `objid` bigint NOT NULL,
  `includesub` int DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mfas_group` (`group_id`),
  KEY `idx_mfas_subject` (`tenant_id`, `objtype`, `objid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段权限授权对象组成员';

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='modefieldauthorize' AND COLUMN_NAME='group_id'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE modefieldauthorize ADD COLUMN group_id bigint NULL COMMENT "授权对象组ID；null=老的单主体授权"',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='modefieldauthorize' AND INDEX_NAME='idx_mfa_group'
);
SET @sql = IF(@idx_exists = 0,
  'CREATE INDEX idx_mfa_group ON modefieldauthorize (group_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
