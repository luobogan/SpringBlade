-- =============================================================================
-- 迁移脚本 V2026.11.19_002：权限三表 objid 改为 BIGINT（支持雪花ID角色/部门）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.11.19_002
-- 目标库    : blade
-- 影响表    : moderightinfo、modefieldauthorize
-- 变更类型  : DDL（列类型扩宽）
-- 是否幂等  : 是（MySQL 8 用 information_schema 判定）
-- 是否丢数据: 否（INT → BIGINT 只扩宽，不丢数据）
--
-- 背景
--   SpringBlade 的角色/部门/岗位 ID 是 19 位雪花 ID（> INT 上限 2147483647），
--   但 moderightinfo.objid / modefieldauthorize.objid 建表为 INT，
--   导致按角色(objtype=1)/部门/岗位配置权限时写入报
--   "Data truncation: Out of range value for column 'objid'"。
--   modedatasharerule / modedatashare_single 的 objid 此前已扩为 BIGINT，本脚本补齐另两表。
--
-- 回滚脚本  :
--   ALTER TABLE moderightinfo MODIFY COLUMN objid INT NULL;
--   ALTER TABLE modefieldauthorize MODIFY COLUMN objid INT NULL;
--   （注意：回滚仅当表中无超过 INT 上限的值时可行）
-- =============================================================================

USE `blade`;

-- 1) moderightinfo.objid INT → BIGINT
SET @t = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='moderightinfo'
            AND COLUMN_NAME='objid' AND DATA_TYPE='int');
SET @s = IF(@t > 0,
  'ALTER TABLE moderightinfo MODIFY COLUMN objid BIGINT NULL COMMENT ''授权对象ID（角色/部门/岗位/人员；Support 雪花ID）''',
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) modefieldauthorize.objid INT → BIGINT
SET @t = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='modefieldauthorize'
            AND COLUMN_NAME='objid' AND DATA_TYPE='int');
SET @s = IF(@t > 0,
  'ALTER TABLE modefieldauthorize MODIFY COLUMN objid BIGINT NULL COMMENT ''授权对象ID（支持雪花ID）''',
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
