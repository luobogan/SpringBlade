-- MySQL dump 10.13  Distrib 8.0.33, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: blade_pay
-- ------------------------------------------------------
-- Server version	8.0.23

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `blade_payment`
--

DROP TABLE IF EXISTS `blade_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付 ID',
  `payment_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付流水号',
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `payment_method` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付方式：BALANCE(余额), WECHAT(微信), ALIPAY(支付宝)',
  `payment_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '支付状态：PENDING(待支付), SUCCESS(已支付), FAILED(失败), CLOSED(已关闭), REFUNDING(退款中), REFUNDED(已退款)',
  `transaction_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方支付交易号',
  `payment_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `expire_time` datetime DEFAULT NULL COMMENT '支付过期时间',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '支付状态：PENDING-待支付,SUCCESS-成功,FAILED-失败,REFUNDED-已退款',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_payment_status` (`payment_status`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=2048370667641954307 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blade_payment`
--

/*!40000 ALTER TABLE `blade_payment` DISABLE KEYS */;
INSERT INTO `blade_payment` VALUES (2048277818338500609,'PAY202604261347413244','202604261347416234',2042170824913412098,459.00,'WECHAT','PENDING',NULL,NULL,'2026-04-26 14:17:42',NULL,'000000',NULL,NULL,'2026-04-26 13:47:41',NULL,'2026-04-26 13:47:41',0,'PENDING'),(2048278956458696706,'PAY202604261352137697','202604261352131907',2042170824913412098,459.00,'WECHAT','PENDING',NULL,NULL,'2026-04-26 14:22:13',NULL,'000000',NULL,NULL,'2026-04-26 13:52:13',NULL,'2026-04-26 13:52:13',0,'PENDING'),(2048352774661398529,'PAY202604261845320493','202604261845321701',2042170824913412098,459.00,'WECHAT','PENDING',NULL,NULL,'2026-04-26 19:15:33',NULL,'000000',NULL,NULL,'2026-04-26 18:45:32',NULL,'2026-04-26 18:45:32',0,'PENDING'),(2048356432824295425,'PAY202604261900043804','202604261845321701',2042170824913412098,459.00,'WECHAT','PENDING',NULL,NULL,'2026-04-26 19:30:05',NULL,'000000',NULL,NULL,'2026-04-26 19:00:04',NULL,'2026-04-26 19:00:04',0,'PENDING'),(2048370667641954306,'PAY202604261956381362','202604261352131907',2042170824913412098,459.00,'WECHAT','PENDING',NULL,NULL,'2026-04-26 20:26:39',NULL,'000000',NULL,NULL,'2026-04-26 19:56:38',NULL,'2026-04-26 19:56:38',0,'PENDING');
/*!40000 ALTER TABLE `blade_payment` ENABLE KEYS */;

--
-- Table structure for table `blade_refund`
--

DROP TABLE IF EXISTS `blade_refund`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_refund` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '退款 ID',
  `refund_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '退款单号',
  `payment_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付单号',
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `amount` decimal(10,2) NOT NULL COMMENT '退款金额',
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退款原因',
  `refund_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '退款状态：PENDING(待处理), SUCCESS(已退款), FAILED(失败)',
  `refunded_at` datetime DEFAULT NULL COMMENT '退款完成时间',
  `refund_transaction_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退款交易号',
  `callback_data` text COLLATE utf8mb4_unicode_ci COMMENT '回调数据',
  `fail_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人 ID',
  `audited_at` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核备注',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blade_refund`
--

/*!40000 ALTER TABLE `blade_refund` DISABLE KEYS */;
/*!40000 ALTER TABLE `blade_refund` ENABLE KEYS */;

--
-- Dumping routines for database 'blade_pay'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-26 19:58:00
