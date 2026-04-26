-- MySQL dump 10.13  Distrib 8.0.33, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: blade_order
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
-- Table structure for table `blade_order`
--

DROP TABLE IF EXISTS `blade_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `order_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单状态：PENDING待支付，PAID已支付，SHIPPED已发货，COMPLETED已完成，CANCELLED已取消，PENDING_REVIEW待评价，RETURN_AFTER_SALES退换/售后',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单备注',
  `tenant_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
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
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blade_order`
--

/*!40000 ALTER TABLE `blade_order` DISABLE KEYS */;
INSERT INTO `blade_order` VALUES (1,'202603261506126670',2036826812715196418,12.00,12.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(2,'202603261517062840',2036826812715196418,12.00,12.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(3,'202603261543409476',2036826812715196418,10.00,10.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(4,'202603261543508858',2036826812715196418,10.00,10.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(5,'202603261548537369',2036826812715196418,10.00,10.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(6,'202603261549096410',2036826812715196418,10.00,10.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(7,'202603261551359089',2036826812715196418,10.00,10.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(8,'202603261556473710',2036826812715196418,10.00,10.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(9,'202603261558293685',2036826812715196418,10.00,10.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(10,'202603261600065499',2036826812715196418,10.00,10.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(11,'202603261600374587',2036826812715196418,2.00,2.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(12,'202603261606033224',2036826812715196418,20.00,20.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(13,'202603261606364503',2036826812715196418,4.00,4.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(14,'202603261620382520',2036826812715196418,10.00,10.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(15,'202603261621232411',2036826812715196418,8.00,8.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(16,'202603261631171869',2036826812715196418,6.00,6.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(17,'202603261633484285',2036826812715196418,2.00,2.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0),(18,'202603261634079282',2036826812715196418,10.00,10.00,NULL,0.00,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,'','000000',NULL,NULL,'2026-04-09 17:14:59',NULL,'2026-04-09 17:15:02',0);
/*!40000 ALTER TABLE `blade_order` ENABLE KEYS */;

--
-- Table structure for table `blade_order_item`
--

DROP TABLE IF EXISTS `blade_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `tenant_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blade_order_item`
--

/*!40000 ALTER TABLE `blade_order_item` DISABLE KEYS */;
INSERT INTO `blade_order_item` VALUES (1,1,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,6,12.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(2,2,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,6,12.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(3,3,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,5,10.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(4,4,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,5,10.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(5,5,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,5,10.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(6,6,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,5,10.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(7,7,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,5,10.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(8,8,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,5,10.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(9,9,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,5,10.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(10,10,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,5,10.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(11,11,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,1,2.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(12,12,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,10,20.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(13,13,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,2,4.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(14,14,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,5,10.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(15,15,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,4,8.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(16,16,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,3,6.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(17,17,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,1,2.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL),(18,18,81,'Huawei Mate 60 Pro','http://192.168.1.5:8085/uploads/product/2d7b5456-30f6-4489-a21b-a25e6dd7f62a.jpg',2.00,5,10.00,'颜色: 红色 内存: 6GB 存储容量: 128GB 网络类型: 5G',54,'000000',NULL,NULL,NULL);
/*!40000 ALTER TABLE `blade_order_item` ENABLE KEYS */;

--
-- Dumping routines for database 'blade_order'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-26  9:36:56
