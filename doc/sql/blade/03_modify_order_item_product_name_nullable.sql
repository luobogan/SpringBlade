-- =====================================================
-- 修改 blade_order_item 表的 product_name 字段允许为空
-- 创建时间：2026-04-26
-- =====================================================

-- 开始事务
START TRANSACTION;

-- 修改 product_name 和 product_image 字段允许为空
ALTER TABLE `blade_order_item` 
MODIFY COLUMN `product_name` varchar(255) DEFAULT NULL COMMENT '商品名称',
MODIFY COLUMN `product_image` varchar(500) DEFAULT NULL COMMENT '商品图片';

-- 提交事务
COMMIT;
