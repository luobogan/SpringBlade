-- Active: 1774619621678@@127.0.0.1@3306@blade_order
-- =====================================================
-- 从旧表迁移订单和支付数据到新表
-- 创建时间：2026-04-26
-- =====================================================

-- 迁移订单数据（在 blade_order 数据库执行）
-- 注意：此脚本需要在 blade_order 数据库中执行
INSERT INTO `blade_order` (
    `id`, `order_no`, `user_id`, `total_amount`, `actual_amount`,
    `coupon_id`, `coupon_amount`, `payment_method`, `payment_no`,
    `payment_time`, `shipping_method`, `shipping_address_id`,
    `tracking_no`, `shipping_time`, `confirm_time`, `order_status`,
    `remark`, `tenant_id`, `create_user`, `create_dept`,
    `create_time`, `update_user`, `update_time`, `status`, `is_deleted`
) SELECT
    `id`, `order_no`, `user_id`, `total_amount`, `actual_amount`,
    `coupon_id`, `coupon_amount`, `payment_method`, `payment_no`,
    `payment_time`, `shipping_method`, `shipping_address_id`,
    `tracking_no`, `shipping_time`, `confirm_time`, `order_status`,
    `remark`, `tenant_id`, `create_user`, `create_dept`,
    `create_time`, `update_user`, `update_time`, 1, `is_deleted`
FROM `blade`.`mall_order`
WHERE NOT EXISTS (SELECT 1 FROM `blade_order` LIMIT 1);

-- 迁移订单项数据（在 blade_order 数据库执行）
INSERT INTO `blade_order_item` (
    `id`, `order_id`, `product_id`, `product_name`, `product_image`,
    `price`, `quantity`, `total_price`, `sku_attributes`, `sku_id`,
    `tenant_id`, `create_user`, `create_dept`, `create_time`,
    `update_user`, `update_time`, `status`, `is_deleted`
) SELECT
    `id`, `order_id`, `product_id`, `product_name`, `product_image`,
    `price`, `quantity`, `total_price`, `sku_attributes`, `sku_id`,
    `tenant_id`, `create_user`, `create_dept`, `create_time`,
    `update_user`, `update_time`, 1, `is_deleted`
FROM `blade`.`mall_order_item`
WHERE NOT EXISTS (SELECT 1 FROM `blade_order_item` LIMIT 1);

-- =====================================================
-- 以下脚本需要在 blade_pay 数据库中执行
-- =====================================================

-- 迁移支付数据（在 blade_pay 数据库执行）
INSERT INTO `blade_payment` (
    `id`, `payment_no`, `order_no`, `user_id`, `amount`,
    `payment_method`, `status`, `transaction_id`, `payment_time`,
    `expire_time`, `remark`, `tenant_id`, `create_user`, `create_dept`,
    `create_time`, `update_user`, `update_time`, `is_deleted`
) SELECT
    `id`, `payment_no`, `order_no`, `user_id`, `amount`,
    `payment_method`,
    CASE `status`
        WHEN 'PENDING' THEN 'PENDING'
        WHEN 'PAID' THEN 'SUCCESS'
        WHEN 'FAILED' THEN 'FAILED'
        WHEN 'CANCELLED' THEN 'CLOSED'
        WHEN 'REFUNDING' THEN 'REFUNDING'
        WHEN 'REFUNDED' THEN 'REFUNDED'
        ELSE `status`
    END AS `status`,
    `transaction_id`, `payment_time`, `expire_time`, `remark`,
    `tenant_id`, `create_user`, `create_dept`, `create_time`,
    `update_user`, `update_time`, `is_deleted`
FROM `blade`.`mall_payment`
WHERE NOT EXISTS (SELECT 1 FROM `blade_payment` LIMIT 1);
