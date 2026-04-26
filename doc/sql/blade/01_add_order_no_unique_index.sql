-- =====================================================
-- 为 blade_order 表的 order_no 字段添加唯一索引
-- 创建时间：2026-04-25
-- =====================================================

-- 开始事务
START TRANSACTION;

-- 检查索引是否存在，如果不存在则添加
SET @index_exists = (
    SELECT COUNT(*) 
    FROM information_schema.statistics 
    WHERE table_schema = DATABASE() 
    AND table_name = 'blade_order' 
    AND index_name = 'uk_order_no'
);

SET @sql = IF(@index_exists = 0, 
    'ALTER TABLE `blade_order` ADD UNIQUE INDEX `uk_order_no` (`order_no`)', 
    'SELECT ''索引 uk_order_no 已存在，跳过创建'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 提交事务
COMMIT;
