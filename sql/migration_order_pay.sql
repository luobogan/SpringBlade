-- =====================================================
-- 订单与支付微服务拆分 - 数据库迁移脚本
-- 迁移: mall_order -> blade_order
--       mall_order_item -> blade_order_item
--       mall_payment -> blade_payment
-- =====================================================

-- 1. 创建 blade_order 数据库
CREATE DATABASE IF NOT EXISTS `blade_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `blade_order`;

-- 2. 创建 blade_order 订单表
CREATE TABLE `blade_order` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '订单 ID',
    `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号',
    `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
    `total_amount` decimal(10,2) NOT NULL COMMENT '总金额',
    `actual_amount` decimal(10,2) NOT NULL COMMENT '实际支付金额',
    `coupon_id` bigint unsigned DEFAULT NULL COMMENT '使用的优惠券 ID',
    `coupon_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '优惠券金额',
    `payment_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付方式',
    `payment_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付单号',
    `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
    `shipping_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配送方式',
    `shipping_address_id` bigint unsigned DEFAULT NULL COMMENT '配送地址 ID',
    `tracking_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物流单号',
    `shipping_time` datetime DEFAULT NULL COMMENT '发货时间',
    `confirm_time` datetime DEFAULT NULL COMMENT '确认收货时间',
    `order_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单状态：PENDING待支付，PAID已支付，SHIPPED已发货，COMPLETED已完成，CANCELLED已取消，PENDING_REVIEW待评价，RETURN_AFTER_SALES退换/售后',
    `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单备注',
    `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
    `create_user` bigint DEFAULT NULL COMMENT '创建人',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` bigint DEFAULT NULL COMMENT '修改人',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_status` (`order_status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 3. 创建 blade_order_item 订单项表
CREATE TABLE `blade_order_item` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '订单项 ID',
    `order_id` bigint unsigned NOT NULL COMMENT '订单 ID',
    `product_id` bigint unsigned NOT NULL COMMENT '产品 ID',
    `product_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品名称（下单时快照）',
    `product_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品图片（下单时快照）',
    `price` decimal(10,2) NOT NULL COMMENT '产品价格（下单时快照）',
    `quantity` int NOT NULL COMMENT '购买数量',
    `total_price` decimal(10,2) NOT NULL COMMENT '小计金额',
    `sku_attributes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SKU 属性（如：颜色、尺寸等）',
    `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID',
    `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` bigint DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

-- 4. 创建 blade_payment 数据库
CREATE DATABASE IF NOT EXISTS `blade_pay` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `blade_pay`;

-- 5. 创建 blade_payment 支付表
CREATE TABLE `blade_payment` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付 ID',
    `payment_no` varchar(64) NOT NULL COMMENT '支付流水号',
    `order_no` varchar(64) NOT NULL COMMENT '订单号',
    `user_id` bigint NOT NULL COMMENT '用户 ID',
    `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
    `payment_method` varchar(32) NOT NULL COMMENT '支付方式：BALANCE(余额), WECHAT(微信), ALIPAY(支付宝)',
    `payment_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态：PENDING(待支付), SUCCESS(已支付), FAILED(失败), CLOSED(已关闭), REFUNDING(退款中), REFUNDED(已退款)',
    `transaction_id` varchar(128) DEFAULT NULL COMMENT '第三方支付交易号',
    `payment_time` datetime DEFAULT NULL COMMENT '支付完成时间',
    `expire_time` datetime DEFAULT NULL COMMENT '支付过期时间',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
    `create_user` bigint DEFAULT NULL COMMENT '创建人',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` bigint DEFAULT NULL COMMENT '修改人',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_payment_status` (`payment_status`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付表';

-- 6. 创建 blade_refund 退款表
CREATE TABLE `blade_refund` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '退款 ID',
    `refund_no` varchar(64) NOT NULL COMMENT '退款单号',
    `payment_no` varchar(64) NOT NULL COMMENT '支付单号',
    `order_no` varchar(64) NOT NULL COMMENT '订单号',
    `user_id` bigint NOT NULL COMMENT '用户 ID',
    `amount` decimal(10,2) NOT NULL COMMENT '退款金额',
    `reason` varchar(255) DEFAULT NULL COMMENT '退款原因',
    `refund_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '退款状态：PENDING(待处理), SUCCESS(已退款), FAILED(失败)',
    `refunded_at` datetime DEFAULT NULL COMMENT '退款完成时间',
    `refund_transaction_id` varchar(128) DEFAULT NULL COMMENT '退款交易号',
    `callback_data` text COMMENT '回调数据',
    `fail_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
    `auditor_id` bigint DEFAULT NULL COMMENT '审核人 ID',
    `audited_at` datetime DEFAULT NULL COMMENT '审核时间',
    `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核备注',
    `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
    `create_user` bigint DEFAULT NULL COMMENT '创建人',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` bigint DEFAULT NULL COMMENT '修改人',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    KEY `idx_payment_no` (`payment_no`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_refund_status` (`refund_status`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款表';

-- =====================================================
-- 数据迁移
-- =====================================================

-- 7. 迁移订单数据 (mall_order -> blade_order.blade_order)
-- 注意：请根据实际情况修改源数据库名称（如 blade 或 blade_mall）
INSERT INTO `blade_order`.`blade_order` (
    `id`, `order_no`, `user_id`, `total_amount`, `actual_amount`,
    `coupon_id`, `coupon_amount`, `payment_method`, `payment_no`,
    `payment_time`, `shipping_method`, `shipping_address_id`,
    `tracking_no`, `shipping_time`, `confirm_time`, `order_status`,
    `remark`, `tenant_id`, `create_user`, `create_dept`,
    `create_time`, `update_user`, `update_time`, `is_deleted`
)
SELECT
    `id`, `order_no`, `user_id`, `total_amount`, `actual_amount`,
    `coupon_id`, `coupon_amount`, `payment_method`, `payment_no`,
    `payment_time`, `shipping_method`, `shipping_address_id`,
    `tracking_no`, `shipping_time`, `confirm_time`, `order_status`,
    `remark`, `tenant_id`, `create_user`, `create_dept`,
    `create_time`, `update_user`, `update_time`, `is_deleted`
FROM `blade`.`mall_order`;

-- 8. 迁移订单项数据 (mall_order_item -> blade_order.blade_order_item)
INSERT INTO `blade_order`.`blade_order_item` (
    `id`, `order_id`, `product_id`, `product_name`, `product_image`,
    `price`, `quantity`, `total_price`, `sku_attributes`, `sku_id`,
    `tenant_id`, `create_time`, `update_user`, `update_time`
)
SELECT
    `id`, `order_id`, `product_id`, `product_name`, `product_image`,
    `price`, `quantity`, `total_price`, `sku_attributes`, `sku_id`,
    `tenant_id`, `create_time`, `update_user`, `update_time`
FROM `blade`.`mall_order_item`;

-- 9. 迁移支付数据 (mall_payment -> blade_pay.blade_payment)
INSERT INTO `blade_pay`.`blade_payment` (
    `id`, `payment_no`, `order_no`, `user_id`, `amount`,
    `payment_method`, `payment_status`, `transaction_id`,
    `payment_time`, `expire_time`, `remark`, `tenant_id`,
    `create_user`, `create_dept`, `create_time`, `update_user`,
    `update_time`
)
SELECT
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
    END AS `payment_status`,
    `transaction_id`, `payment_time`, `expire_time`, `remark`, `tenant_id`,
    `create_user`, `create_dept`, `create_time`, `update_user`,
    `update_time`
FROM `blade`.`mall_payment`;

-- =====================================================
-- 验证迁移结果
-- =====================================================

-- 验证订单数量
SELECT COUNT(*) AS order_count FROM `blade_order`.`blade_order`;
SELECT COUNT(*) AS order_item_count FROM `blade_order`.`blade_order_item`;
SELECT COUNT(*) AS payment_count FROM `blade_pay`.`blade_payment`;

-- =====================================================
-- 清理旧表（执行前请确认数据迁移成功）
-- =====================================================

-- DROP TABLE IF EXISTS `mall_order_item`;
-- DROP TABLE IF EXISTS `mall_order`;
-- DROP TABLE IF EXISTS `mall_payment`;
