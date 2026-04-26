-- Active: 1774619621678@@127.0.0.1@3306@blade_pay
-- =====================================================
-- 为 blade_payment 表添加 status 字段
-- 创建时间：2026-04-26
-- =====================================================

-- 添加 status 字段
SET @dbname = DATABASE();
SET @tablename = 'blade_payment';
SET @columnname = 'status';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' varchar(20) DEFAULT ''PENDING'' COMMENT ''支付状态：PENDING-待支付,SUCCESS-成功,FAILED-失败,REFUNDED-已退款''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
