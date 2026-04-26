-- =====================================================
-- 从旧表迁移支付数据到新表
-- 创建时间：2026-04-26
-- 执行数据库：blade_pay
-- =====================================================

-- 迁移支付数据
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
