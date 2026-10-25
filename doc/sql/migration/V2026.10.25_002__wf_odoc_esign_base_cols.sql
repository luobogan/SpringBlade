-- -----------------------------------------------------------------------------
-- 迁移脚本：修正 wf_odoc_sign_* 三表的公共列与业务状态列命名
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.25_002
-- 目标库    : blade（生产拆库则为 blade_workflow）
-- 影响表    : wf_odoc_sign_flow、wf_odoc_sign_signer、wf_odoc_sign_notify
-- 变更类型  : 改表（DDL，仅补列 + 改名，不动数据）
-- 是否幂等  : 是（information_schema 列存在性守卫 + PREPARE 动态 SQL）
-- 是否丢数据: 否（仅 ADD COLUMN / CHANGE COLUMN，均带默认值）
-- 回滚脚本  :
--   ALTER TABLE `wf_odoc_sign_flow`   CHANGE COLUMN `sign_status` `status` INT NOT NULL DEFAULT 0;
--   ALTER TABLE `wf_odoc_sign_signer` CHANGE COLUMN `sign_status` `status` INT NOT NULL DEFAULT 0;
--   ALTER TABLE `wf_odoc_sign_flow`   DROP COLUMN `create_user`, DROP COLUMN `create_dept`,
--         DROP COLUMN `update_user`, DROP COLUMN `update_time`, DROP COLUMN `status`, DROP COLUMN `is_deleted`;
--   ALTER TABLE `wf_odoc_sign_signer` DROP COLUMN `create_user`, DROP COLUMN `create_dept`,
--         DROP COLUMN `update_user`, DROP COLUMN `status`;
--   ALTER TABLE `wf_odoc_sign_notify` DROP COLUMN `create_user`, DROP COLUMN `create_dept`,
--         DROP COLUMN `create_time`, DROP COLUMN `update_user`, DROP COLUMN `update_time`,
--         DROP COLUMN `status`, DROP COLUMN `is_deleted`;
-- 设计文档  : doc/esign/电子签章集成技术方案.md
--
-- 背景
--   V2026.10.25_001 首版建表存在两处缺陷，本脚本修正：
--   ① 公共列缺失：wf_odoc_sign_signer 缺 create_user / create_dept / update_user，
--      wf_odoc_sign_notify 缺 create_user / create_dept / create_time / update_user /
--      update_time / is_deleted；三表均缺 BaseEntity 的 `status` 列
--      （1正常 0禁用）。实体继承 TenantEntity，MyBatis-Plus 生成的 SQL 会引用这些列，
--      缺列即报 Unknown column。参见 V2026.09.21_013__wf_base_entity_missing_cols.sql。
--   ② 业务状态列与公共列撞名：首版把签署业务状态放在 `status`，与 BaseEntity 的
--      `status`（启用/禁用）语义冲突。改名为 `sign_status`：
--        wf_odoc_sign_flow.sign_status   : 0待发起 1进行中 2已完成 3已撤销 4已拒签 5已过期 9失败
--        wf_odoc_sign_signer.sign_status : 0待签 1已签 2拒签
--      wf_odoc_sign_notify 的业务状态位叫 processed，本身不冲突，保持不变。
--
--   已按最终口径重建过表的环境执行本脚本为全空操作（守卫命中即跳过）。
-- =============================================================================

-- =============================================================================
-- 1) wf_odoc_sign_flow
-- =============================================================================
-- 1.1 业务状态列改名：status -> sign_status
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'sign_status');
SET @has_status := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0 AND @has_status = 1,
  'ALTER TABLE `wf_odoc_sign_flow` CHANGE COLUMN `status` `sign_status` INT NOT NULL DEFAULT 0 COMMENT ''签署状态 0待发起 1进行中 2已完成 3已撤销 4已拒签 5已过期 9失败''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 补 create_user
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'create_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 补 create_dept
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 补 update_user
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'update_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.5 补 update_time
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'update_time');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `update_time` DATETIME DEFAULT NULL COMMENT ''修改时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.6 补公共 status 列（1正常 0禁用）
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT ''状态:1正常 0禁用''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.7 补 is_deleted
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_flow` ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0 COMMENT ''逻辑删除 0未删 1已删''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.8 索引改名：idx_wosf_status -> idx_wosf_sign_status
SET @idx := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND INDEX_NAME = 'idx_wosf_sign_status');
SET @old := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_flow' AND INDEX_NAME = 'idx_wosf_status');
SET @sql := IF(@idx = 0 AND @old = 1, 'ALTER TABLE `wf_odoc_sign_flow` RENAME INDEX `idx_wosf_status` TO `idx_wosf_sign_status`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 2) wf_odoc_sign_signer
-- =============================================================================
-- 2.1 业务状态列改名：status -> sign_status
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_signer' AND COLUMN_NAME = 'sign_status');
SET @has_status := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_signer' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0 AND @has_status = 1,
  'ALTER TABLE `wf_odoc_sign_signer` CHANGE COLUMN `status` `sign_status` INT NOT NULL DEFAULT 0 COMMENT ''签署状态 0待签 1已签 2拒签''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.2 补 create_user
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_signer' AND COLUMN_NAME = 'create_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_signer` ADD COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.3 补 create_dept
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_signer' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_signer` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.4 补 update_user
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_signer' AND COLUMN_NAME = 'update_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_signer` ADD COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2.5 补公共 status 列（1正常 0禁用）
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_signer' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_signer` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT ''状态:1正常 0禁用''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 3) wf_odoc_sign_notify
-- =============================================================================
-- 3.1 补 create_user
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_notify' AND COLUMN_NAME = 'create_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_notify` ADD COLUMN `create_user` BIGINT DEFAULT NULL COMMENT ''创建人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.2 补 create_dept
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_notify' AND COLUMN_NAME = 'create_dept');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_notify` ADD COLUMN `create_dept` BIGINT DEFAULT NULL COMMENT ''创建部门''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.3 补 create_time
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_notify' AND COLUMN_NAME = 'create_time');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_notify` ADD COLUMN `create_time` DATETIME DEFAULT NULL COMMENT ''创建时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.4 补 update_user
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_notify' AND COLUMN_NAME = 'update_user');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_notify` ADD COLUMN `update_user` BIGINT DEFAULT NULL COMMENT ''修改人''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.5 补 update_time
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_notify' AND COLUMN_NAME = 'update_time');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_notify` ADD COLUMN `update_time` DATETIME DEFAULT NULL COMMENT ''修改时间''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.6 补公共 status 列（1正常 0禁用）
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_notify' AND COLUMN_NAME = 'status');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_notify` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT ''状态:1正常 0禁用''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3.7 补 is_deleted
SET @col := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_odoc_sign_notify' AND COLUMN_NAME = 'is_deleted');
SET @sql := IF(@col = 0, 'ALTER TABLE `wf_odoc_sign_notify` ADD COLUMN `is_deleted` INT NOT NULL DEFAULT 0 COMMENT ''逻辑删除 0未删 1已删''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
