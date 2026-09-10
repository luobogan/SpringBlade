-- =====================================================
-- 字段定义逻辑删除支持 - 数据库变更脚本（幂等版）
-- 说明：为 workflow_billfield、mode_form_field_extend、mode_form_field_option
--       添加 is_deleted 和 status 字段，支持逻辑删除
-- 特性：可重复执行，不会报错
-- =====================================================

-- 设置分隔符为 //（支持在存储过程中使用分号）
DELIMITER //

-- ==================== 1. workflow_billfield 表 ====================

-- 添加 is_deleted 字段
DROP PROCEDURE IF EXISTS add_col_workflow_billfield_is_deleted //
CREATE PROCEDURE add_col_workflow_billfield_is_deleted()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'workflow_billfield' 
          AND COLUMN_NAME = 'is_deleted'
    ) THEN
        ALTER TABLE `workflow_billfield` 
          ADD COLUMN `is_deleted` INT(1) DEFAULT 0 COMMENT '是否删除（0未删除 1已删除）';
    END IF;
END //

-- 添加 status 字段
DROP PROCEDURE IF EXISTS add_col_workflow_billfield_status //
CREATE PROCEDURE add_col_workflow_billfield_status()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'workflow_billfield' 
          AND COLUMN_NAME = 'status'
    ) THEN
        ALTER TABLE `workflow_billfield` 
          ADD COLUMN `status` INT(1) DEFAULT 1 COMMENT '状态（1正常 0禁用 -1已删除）';
    END IF;
END //

-- 添加索引（幂等）
DROP PROCEDURE IF EXISTS add_idx_workflow_billfield //
CREATE PROCEDURE add_idx_workflow_billfield()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_billfield' AND INDEX_NAME = 'idx_is_deleted') THEN
        ALTER TABLE `workflow_billfield` ADD INDEX `idx_is_deleted` (`is_deleted`);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_billfield' AND INDEX_NAME = 'idx_status') THEN
        ALTER TABLE `workflow_billfield` ADD INDEX `idx_status` (`status`);
    END IF;
END //

-- 执行存储过程
CALL add_col_workflow_billfield_is_deleted() //
CALL add_col_workflow_billfield_status() //
CALL add_idx_workflow_billfield() //
DROP PROCEDURE add_col_workflow_billfield_is_deleted //
DROP PROCEDURE add_col_workflow_billfield_status //
DROP PROCEDURE add_idx_workflow_billfield //

-- ==================== 2. mode_form_field_extend 表 ====================

-- 添加 is_deleted 字段
DROP PROCEDURE IF EXISTS add_col_extend_is_deleted //
CREATE PROCEDURE add_col_extend_is_deleted()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'mode_form_field_extend' 
          AND COLUMN_NAME = 'is_deleted'
    ) THEN
        ALTER TABLE `mode_form_field_extend` 
          ADD COLUMN `is_deleted` INT(1) DEFAULT 0 COMMENT '是否删除（0未删除 1已删除）';
    END IF;
END //

-- 添加 status 字段
DROP PROCEDURE IF EXISTS add_col_extend_status //
CREATE PROCEDURE add_col_extend_status()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'mode_form_field_extend' 
          AND COLUMN_NAME = 'status'
    ) THEN
        ALTER TABLE `mode_form_field_extend` 
          ADD COLUMN `status` INT(1) DEFAULT 1 COMMENT '状态（1正常 0禁用 -1已删除）';
    END IF;
END //

-- 添加索引
DROP PROCEDURE IF EXISTS add_idx_extend //
CREATE PROCEDURE add_idx_extend()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mode_form_field_extend' AND INDEX_NAME = 'idx_is_deleted') THEN
        ALTER TABLE `mode_form_field_extend` ADD INDEX `idx_is_deleted` (`is_deleted`);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mode_form_field_extend' AND INDEX_NAME = 'idx_status') THEN
        ALTER TABLE `mode_form_field_extend` ADD INDEX `idx_status` (`status`);
    END IF;
END //

CALL add_col_extend_is_deleted() //
CALL add_col_extend_status() //
CALL add_idx_extend() //
DROP PROCEDURE add_col_extend_is_deleted //
DROP PROCEDURE add_col_extend_status //
DROP PROCEDURE add_idx_extend //

-- ==================== 3. mode_form_field_option 表 ====================

-- 添加 is_deleted 字段
DROP PROCEDURE IF EXISTS add_col_option_is_deleted //
CREATE PROCEDURE add_col_option_is_deleted()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'mode_form_field_option' 
          AND COLUMN_NAME = 'is_deleted'
    ) THEN
        ALTER TABLE `mode_form_field_option` 
          ADD COLUMN `is_deleted` INT(1) DEFAULT 0 COMMENT '是否删除（0未删除 1已删除）';
    END IF;
END //

-- 添加 status 字段
DROP PROCEDURE IF EXISTS add_col_option_status //
CREATE PROCEDURE add_col_option_status()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'mode_form_field_option' 
          AND COLUMN_NAME = 'status'
    ) THEN
        ALTER TABLE `mode_form_field_option` 
          ADD COLUMN `status` INT(1) DEFAULT 1 COMMENT '状态（1正常 0禁用 -1已删除）';
    END IF;
END //

-- 添加索引
DROP PROCEDURE IF EXISTS add_idx_option //
CREATE PROCEDURE add_idx_option()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mode_form_field_option' AND INDEX_NAME = 'idx_is_deleted') THEN
        ALTER TABLE `mode_form_field_option` ADD INDEX `idx_is_deleted` (`is_deleted`);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mode_form_field_option' AND INDEX_NAME = 'idx_status') THEN
        ALTER TABLE `mode_form_field_option` ADD INDEX `idx_status` (`status`);
    END IF;
END //

CALL add_col_option_is_deleted() //
CALL add_col_option_status() //
CALL add_idx_option() //
DROP PROCEDURE add_col_option_is_deleted //
DROP PROCEDURE add_col_option_status //
DROP PROCEDURE add_idx_option //

-- 恢复分隔符
DELIMITER ;

-- =====================================================
-- 回滚脚本（如需回滚，执行以下 SQL）
-- =====================================================
/*
ALTER TABLE `workflow_billfield` DROP INDEX `idx_status`, DROP INDEX `idx_is_deleted`, DROP COLUMN `status`, DROP COLUMN `is_deleted`;
ALTER TABLE `mode_form_field_extend` DROP INDEX `idx_status`, DROP INDEX `idx_is_deleted`, DROP COLUMN `status`, DROP COLUMN `is_deleted`;
ALTER TABLE `mode_form_field_option` DROP INDEX `idx_status`, DROP INDEX `idx_is_deleted`, DROP COLUMN `status`, DROP COLUMN `is_deleted`;
*/
