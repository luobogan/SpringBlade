-- =====================================================
-- 为 blade_order 表添加 status 字段
-- 创建时间：2026-04-25
-- =====================================================

-- 开始事务
START TRANSACTION;

-- 添加 status 字段
ALTER TABLE `blade_order` 
ADD COLUMN `status` int DEFAULT 1 COMMENT '业务状态：1-正常' AFTER `update_time`;

-- 提交事务
COMMIT;
