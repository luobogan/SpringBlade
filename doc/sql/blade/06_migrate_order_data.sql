-- =====================================================
-- 从旧表迁移订单数据到新表
-- 创建时间：2026-04-26
-- 执行数据库：blade_order
-- =====================================================

-- 迁移订单数据
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

-- 迁移订单项数据
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
