-- MySQL dump 10.13  Distrib 8.0.33, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: blade
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
-- Table structure for table `addresses`
--

DROP TABLE IF EXISTS `addresses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `addresses` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `postal_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_default` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_client`
--

DROP TABLE IF EXISTS `blade_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_client` (
  `id` bigint NOT NULL COMMENT '主键',
  `client_id` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户端id',
  `client_secret` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户端密钥',
  `resource_ids` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '资源集合',
  `scope` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授权范围',
  `authorized_grant_types` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授权类型',
  `web_server_redirect_uri` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '回调地址',
  `authorities` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限',
  `access_token_validity` int NOT NULL COMMENT '令牌过期秒数',
  `refresh_token_validity` int NOT NULL COMMENT '刷新令牌过期秒数',
  `additional_information` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附加说明',
  `autoapprove` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '自动授权',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int NOT NULL COMMENT '状态',
  `is_deleted` int NOT NULL COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户端表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_code`
--

DROP TABLE IF EXISTS `blade_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_code` (
  `id` bigint NOT NULL COMMENT '主键',
  `datasource_id` bigint DEFAULT NULL COMMENT '数据源主键',
  `service_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务名称',
  `code_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模块名称',
  `table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表名',
  `table_prefix` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表前缀',
  `pk_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主键名',
  `package_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '后端包名',
  `base_mode` int DEFAULT NULL COMMENT '基础业务模式',
  `wrap_mode` int DEFAULT NULL COMMENT '包装器模式',
  `api_path` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '后端路径',
  `web_path` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '前端路径',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码生成表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_datasource`
--

DROP TABLE IF EXISTS `blade_datasource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_datasource` (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `driver_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '驱动类',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '连接地址',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `password` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int DEFAULT NULL COMMENT '状态',
  `is_deleted` int DEFAULT NULL COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据源配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_dept`
--

DROP TABLE IF EXISTS `blade_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_dept` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父主键',
  `ancestors` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '祖级列表',
  `dept_name` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门名',
  `full_name` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门全称',
  `sort` int DEFAULT NULL COMMENT '排序',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `code` varchar(45) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门编码',
  `status` int DEFAULT NULL COMMENT '业务状态[1:正常]',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_dict`
--

DROP TABLE IF EXISTS `blade_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_dict` (
  `id` bigint NOT NULL COMMENT '主键',
  `parent_id` bigint DEFAULT '0' COMMENT '父主键',
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字典码',
  `dict_key` int DEFAULT NULL COMMENT '字典值',
  `dict_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字典名称',
  `sort` int DEFAULT NULL COMMENT '排序',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字典备注',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_log_api`
--

DROP TABLE IF EXISTS `blade_log_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_log_api` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `service_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务ID',
  `server_host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器名',
  `server_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器IP地址',
  `env` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器环境',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '日志类型',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '日志标题',
  `method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作方式',
  `request_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求URI',
  `user_agent` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户代理',
  `remote_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作IP地址',
  `method_class` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法类',
  `method_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法名',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '操作提交的数据',
  `time` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='接口日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_log_error`
--

DROP TABLE IF EXISTS `blade_log_error`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_log_error` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `service_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务ID',
  `server_host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器名',
  `server_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器IP地址',
  `env` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '系统环境',
  `method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作方式',
  `request_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求URI',
  `user_agent` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户代理',
  `stack_trace` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '堆栈',
  `exception_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '异常名',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '异常信息',
  `line_number` int DEFAULT NULL COMMENT '错误行数',
  `remote_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作IP地址',
  `method_class` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法类',
  `file_name` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件名',
  `method_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法名',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '操作提交的数据',
  `time` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='错误日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_log_usual`
--

DROP TABLE IF EXISTS `blade_log_usual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_log_usual` (
  `id` bigint NOT NULL COMMENT '编号',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `service_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务ID',
  `server_host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器名',
  `server_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器IP地址',
  `env` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '系统环境',
  `log_level` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '日志级别',
  `log_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '日志业务id',
  `log_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '日志数据',
  `method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作方式',
  `request_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求URI',
  `remote_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作IP地址',
  `method_class` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法类',
  `method_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法名',
  `user_agent` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户代理',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '操作提交的数据',
  `time` datetime DEFAULT NULL COMMENT '执行时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通用日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_menu`
--

DROP TABLE IF EXISTS `blade_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_menu` (
  `id` bigint NOT NULL COMMENT '主键',
  `parent_id` bigint DEFAULT '0' COMMENT '父级菜单',
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单名称',
  `alias` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单别名',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求地址',
  `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单资源',
  `sort` int DEFAULT NULL COMMENT '排序',
  `category` int DEFAULT NULL COMMENT '菜单类型',
  `action` int DEFAULT '0' COMMENT '操作按钮类型',
  `is_open` int DEFAULT '1' COMMENT '是否打开新页面',
  `is_component` int DEFAULT '0' COMMENT '是否生成组件',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件路径',
  `component_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'bundled' COMMENT '组件类型: bundled-内置, remote-远程',
  `remote_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '远程组件地址',
  `cache_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件版本号',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int DEFAULT NULL COMMENT '业务状态[1:正常]',
  `source_menu_id` bigint DEFAULT NULL COMMENT '源菜单ID（租户副本指向超级管理员原始菜单ID）',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_source_menu_id` (`source_menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_menu_component`
--

DROP TABLE IF EXISTS `blade_menu_component`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_menu_component` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `component_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组件名称',
  `component_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组件路径',
  `component_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'bundled' COMMENT '组件类型: bundled-内置, remote-远程',
  `remote_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '远程组件URL',
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版本号',
  `md5_hash` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'MD5校验值',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_user` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_component_path` (`component_path`),
  UNIQUE KEY `uk_component_name` (`component_name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单组件配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_notice`
--

DROP TABLE IF EXISTS `blade_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_notice` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标题',
  `category` int DEFAULT NULL COMMENT '类型',
  `release_time` datetime DEFAULT NULL COMMENT '发布时间',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int DEFAULT NULL COMMENT '状态',
  `is_deleted` int DEFAULT NULL COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_package_menu`
--

DROP TABLE IF EXISTS `blade_package_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_package_menu` (
  `id` bigint NOT NULL COMMENT '主键',
  `package_id` bigint DEFAULT NULL COMMENT '产品包ID',
  `menu_id` bigint DEFAULT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  KEY `idx_package_id` (`package_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='产品包菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_param`
--

DROP TABLE IF EXISTS `blade_param`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_param` (
  `id` bigint NOT NULL COMMENT '主键',
  `param_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '参数名',
  `param_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '参数键',
  `param_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '参数值',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int DEFAULT NULL COMMENT '状态',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参数表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_post`
--

DROP TABLE IF EXISTS `blade_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_post` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `category` int DEFAULT NULL COMMENT '岗位类型',
  `post_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '岗位编号',
  `post_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '岗位名称',
  `sort` int DEFAULT NULL COMMENT '岗位排序',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '岗位描述',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int DEFAULT NULL COMMENT '状态',
  `is_deleted` int DEFAULT NULL COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_region`
--

DROP TABLE IF EXISTS `blade_region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_region` (
  `code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '区划编号',
  `parent_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父区划编号',
  `ancestors` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '祖区划编号',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区划名称',
  `province_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区级名称',
  `town_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '镇级区划编号',
  `town_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '镇级名称',
  `village_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '村级区划编号',
  `village_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '村级名称',
  `level` int DEFAULT NULL COMMENT '层级',
  `sort` int DEFAULT NULL COMMENT '排序',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='行政区划表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_report_file`
--

DROP TABLE IF EXISTS `blade_report_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_report_file` (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '文件名',
  `content` mediumblob COMMENT '文件内容',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报表文件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_role`
--

DROP TABLE IF EXISTS `blade_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_role` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父主键',
  `role_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色名',
  `sort` int DEFAULT NULL COMMENT '排序',
  `role_alias` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色别名',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_role_menu`
--

DROP TABLE IF EXISTS `blade_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_role_menu` (
  `id` bigint NOT NULL COMMENT '主键',
  `menu_id` bigint DEFAULT NULL COMMENT '菜单id',
  `role_id` bigint DEFAULT NULL COMMENT '角色id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_role_scope`
--

DROP TABLE IF EXISTS `blade_role_scope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_role_scope` (
  `id` bigint NOT NULL COMMENT '主键',
  `scope_category` int DEFAULT NULL COMMENT '权限类型(1:数据权限、2:接口权限)',
  `scope_id` bigint DEFAULT NULL COMMENT '权限id',
  `role_id` bigint DEFAULT NULL COMMENT '角色id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_scope_api`
--

DROP TABLE IF EXISTS `blade_scope_api`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_scope_api` (
  `id` bigint NOT NULL COMMENT '主键',
  `menu_id` bigint DEFAULT NULL COMMENT '菜单主键',
  `resource_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '资源编号',
  `scope_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接口权限名',
  `scope_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接口权限地址',
  `scope_type` int DEFAULT NULL COMMENT '接口权限类型',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接口权限备注',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int DEFAULT '1' COMMENT '状态',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='接口权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_scope_data`
--

DROP TABLE IF EXISTS `blade_scope_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_scope_data` (
  `id` bigint NOT NULL COMMENT '主键',
  `menu_id` bigint DEFAULT NULL COMMENT '菜单主键',
  `resource_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '资源编号',
  `scope_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据权限名称',
  `scope_field` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据权限字段',
  `scope_class` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据权限类名',
  `scope_column` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据权限字段',
  `scope_type` int DEFAULT NULL COMMENT '数据权限类型',
  `scope_value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据权限值域',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据权限备注',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int DEFAULT NULL COMMENT '状态',
  `is_deleted` int DEFAULT NULL COMMENT '是否已删除',
  `tenant_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_tenant`
--

DROP TABLE IF EXISTS `blade_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_tenant` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户ID',
  `tenant_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户名称',
  `domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '域名地址',
  `linkman` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人',
  `contact_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系地址',
  `account_number` int DEFAULT NULL COMMENT '账号额度',
  `expire_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '过期时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int DEFAULT NULL COMMENT '状态',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  `package_id` bigint DEFAULT NULL COMMENT '产品包ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_tenant_package`
--

DROP TABLE IF EXISTS `blade_tenant_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_tenant_package` (
  `id` bigint NOT NULL COMMENT '主键',
  `package_name` varchar(100) DEFAULT NULL COMMENT '产品包名称',
  `package_code` varchar(100) DEFAULT NULL COMMENT '产品包编码',
  `description` varchar(500) DEFAULT NULL COMMENT '产品包描述',
  `status` int DEFAULT '1' COMMENT '状态',
  `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='租户产品包表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_top_menu`
--

DROP TABLE IF EXISTS `blade_top_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_top_menu` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户id',
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '顶部菜单编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '顶部菜单名',
  `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '顶部菜单资源',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '顶部菜单路由',
  `sort` int DEFAULT NULL COMMENT '顶部菜单排序',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int DEFAULT '1' COMMENT '状态',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='顶部菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_top_menu_setting`
--

DROP TABLE IF EXISTS `blade_top_menu_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_top_menu_setting` (
  `id` bigint NOT NULL COMMENT '主键',
  `top_menu_id` bigint DEFAULT NULL COMMENT '顶部菜单主键',
  `menu_id` bigint DEFAULT NULL COMMENT '菜单主键',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='顶部菜单配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_user`
--

DROP TABLE IF EXISTS `blade_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_user` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户编号',
  `account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '账号',
  `password` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '昵称',
  `real_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '真名',
  `avatar` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像',
  `wx_openid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信用户唯一标识',
  `wx_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信用户授权码',
  `member_level` tinyint DEFAULT '0' COMMENT '会员等级：0 普通用户，1-9 对应不同等级',
  `member_points` int DEFAULT '0' COMMENT '会员积分',
  `member_growth` int DEFAULT '0' COMMENT '会员成长值',
  `member_experience` int DEFAULT '0' COMMENT '会员经验值',
  `member_start_time` datetime DEFAULT NULL COMMENT '会员开始时间',
  `member_end_time` datetime DEFAULT NULL COMMENT '会员到期时间',
  `member_status` tinyint DEFAULT '0' COMMENT '会员状态：0 非会员，1 会员，2 过期会员',
  `total_consumption` decimal(10,2) DEFAULT '0.00' COMMENT '累计消费金额',
  `last_member_update` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '会员信息最后更新时间',
  `gender` tinyint DEFAULT '0' COMMENT '性别：0 未知，1 男，2 女',
  `mall_birthday` date DEFAULT NULL COMMENT '生日（商城）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '昵称（商城）',
  `email` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机',
  `birthday` datetime DEFAULT NULL COMMENT '生日',
  `sex` smallint DEFAULT NULL COMMENT '性别',
  `role_id` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色id',
  `dept_id` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门id',
  `post_id` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '岗位id',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` int DEFAULT NULL COMMENT '状态',
  `is_deleted` int DEFAULT '0' COMMENT '是否已删除',
  `open_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_wx_openid` (`wx_openid`),
  KEY `idx_phone` (`phone`),
  KEY `idx_member_level` (`member_level`),
  KEY `idx_member_status` (`member_status`),
  KEY `idx_blade_user_open_id` (`open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blade_user_oauth`
--

DROP TABLE IF EXISTS `blade_user_oauth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blade_user_oauth` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户ID',
  `uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方系统用户ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '账号',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `avatar` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像',
  `blog` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应用主页',
  `company` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '公司名',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地址',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮件',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `gender` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '性别',
  `source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户第三方认证表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform`
--

DROP TABLE IF EXISTS `lowcode_dbform`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表名',
  `table_describe` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表描述',
  `table_type` int DEFAULT NULL COMMENT '表类型;1单表、2树表、3主表、4附表',
  `table_classify` int DEFAULT NULL COMMENT '表分类',
  `table_id_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主键类型',
  `table_select` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表格选择类型',
  `is_db_sync` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '同步数据库状态;N=未同步  Y=已同步',
  `is_des_form` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否用设计器表单;N=不 启用  Y=已启用',
  `sub_table_mapping` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附表-映射关系',
  `sub_table_sort` int DEFAULT NULL COMMENT '附表-排序序号',
  `sub_table_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附表-Tab标题',
  `theme_template` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题模板',
  `desform_web_id` bigint DEFAULT NULL COMMENT 'WEB表单设计ID',
  `tree_style` varchar(126) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '树表样式',
  `tree_mode` varchar(126) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '树表模式',
  `tree_label_field` varchar(126) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '树表展开字段',
  `operate_menu_style` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作栏样式',
  `form_style` int DEFAULT '1' COMMENT '表单风格',
  `sub_table_list_str` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '子表列表',
  `view_default_field` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否显示系统字段;N=不 启用  Y=已启用',
  `group_dbform_id` bigint DEFAULT NULL COMMENT '表单开发分组id',
  `orderby_config` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '排序配置',
  `where_config` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '默认条件配置',
  `data_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '数据配置',
  `basic_function` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '基础功能',
  `basic_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '基础配置',
  `table_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '表格配置',
  `data_sources_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '表视图数据来源配置',
  `table_style` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '单表样式',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_table_name` (`table_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_button`
--

DROP TABLE IF EXISTS `lowcode_dbform_button`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_button` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `button_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮名称',
  `button_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮编码',
  `button_icon` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮图标',
  `button_location` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮位置',
  `button_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮类型',
  `button_sort` int DEFAULT NULL COMMENT '按钮排序',
  `button_exp` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '按钮显隐增强',
  `button_show` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮显示',
  `button_auth` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限控制',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbformid` (`dbform_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-自定义按钮';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_enhance_java`
--

DROP TABLE IF EXISTS `lowcode_dbform_enhance_java`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_enhance_java` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `button_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮编码',
  `java_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'java类型;spring   class http online_edit',
  `java_class_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'java类路径',
  `online_script` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '在线脚本',
  `active_status` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生效状态;N=不生效 Y=生效',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `list_result_handle_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结果处理类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbformid` (`dbform_id`) USING BTREE,
  KEY `ind_button_code` (`button_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-JAVA增强';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_enhance_js`
--

DROP TABLE IF EXISTS `lowcode_dbform_enhance_js`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_enhance_js` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `button_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮编码',
  `js_script` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'JS脚本',
  `active_status` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生效状态;N=不生效 Y=生效',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `sort` int DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbformid` (`dbform_id`) USING BTREE,
  KEY `ind_button_code` (`button_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-JS增强';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_enhance_sql`
--

DROP TABLE IF EXISTS `lowcode_dbform_enhance_sql`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_enhance_sql` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `button_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮编码',
  `sql_script` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'SQL脚本',
  `active_status` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生效状态;N=不生效 Y=生效',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `sort` int DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbformid` (`dbform_id`) USING BTREE,
  KEY `ind_button_code` (`button_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-SQL增强';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_field`
--

DROP TABLE IF EXISTS `lowcode_dbform_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_field` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `field_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段',
  `field_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段名称',
  `field_len` int DEFAULT NULL COMMENT '字段长度',
  `field_point_len` int DEFAULT NULL COMMENT '小数位数',
  `field_default_val` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '默认值',
  `field_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段类型',
  `field_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `is_primary_key` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否主键;N=否 Y=是',
  `is_null` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否允许为空;N=否 Y=是',
  `sort_num` int DEFAULT NULL COMMENT '排序',
  `is_db` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否同步到数据库 N=否 Y=是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbformid` (`dbform_id`) USING BTREE,
  KEY `ind_fieldcode` (`field_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-数据库字段表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_field_query`
--

DROP TABLE IF EXISTS `lowcode_dbform_field_query`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_field_query` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `field_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段',
  `query_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '查询类型',
  `query_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '查询配置',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbform_id` (`dbform_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-字段查询配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_field_web`
--

DROP TABLE IF EXISTS `lowcode_dbform_field_web`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_field_web` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `field_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段',
  `is_db_select` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否查询;N=不查询 Y=查询',
  `is_show_list` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列表是否显示;N=不显示 Y=显示',
  `is_show_form` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表单是否显示;N=不显示 Y=显示',
  `is_show_column` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否可控;N=不显示 Y=显示',
  `is_show_sort` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否排序;N=不显示 Y=显示',
  `is_required` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否必填;N=否 Y=是',
  `control_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '控件类型',
  `controls_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '控件配置',
  `cell_width` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列宽',
  `cell_width_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列宽类型',
  `verify_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '校验配置',
  `format_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '页面格式化显示配置',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbform_id` (`dbform_id`) USING BTREE,
  KEY `ind_field_code` (`field_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-页面字段表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_index`
--

DROP TABLE IF EXISTS `lowcode_dbform_index`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_index` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `index_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '索引名称',
  `index_field` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '索引字段',
  `index_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '索引类型',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbformid` (`dbform_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-索引配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_role_button`
--

DROP TABLE IF EXISTS `lowcode_dbform_role_button`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_role_button` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `role_id` bigint DEFAULT NULL COMMENT '角色id',
  `button_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮编码',
  `button_auth` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '按钮权限',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbformid` (`dbform_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-按钮权限';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_role_field`
--

DROP TABLE IF EXISTS `lowcode_dbform_role_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_role_field` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `role_id` bigint DEFAULT NULL COMMENT '角色id',
  `field_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段编码',
  `field_auth` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段权限',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbformid` (`dbform_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-字段权限';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_dbform_summary`
--

DROP TABLE IF EXISTS `lowcode_dbform_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_dbform_summary` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `dbform_id` bigint DEFAULT NULL COMMENT '表单开发id',
  `summary_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '汇总名称',
  `summary_field` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '汇总字段',
  `summary_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '汇总类型',
  `summary_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '汇总配置',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_dbformid` (`dbform_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发-汇总配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_desform`
--

DROP TABLE IF EXISTS `lowcode_desform`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_desform` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `desform_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表单设计名称',
  `desform_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '表单设计JSON',
  `group_desform_id` bigint DEFAULT NULL COMMENT '表单设计分组id',
  `is_open` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否开放',
  `is_template` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否模板',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='Web表单设计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_group_dbform`
--

DROP TABLE IF EXISTS `lowcode_group_dbform`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_group_dbform` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分组名称',
  `group_icon` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分组图标',
  `group_sort` int DEFAULT NULL COMMENT '分组排序',
  `parent_id` bigint DEFAULT NULL COMMENT '父级id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='表单开发分组';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_group_desform`
--

DROP TABLE IF EXISTS `lowcode_group_desform`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_group_desform` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `pid` bigint DEFAULT NULL COMMENT 'pid',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='表单设计分组';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lowcode_log_history_desform`
--

DROP TABLE IF EXISTS `lowcode_log_history_desform`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lowcode_log_history_desform` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户编号',
  `create_user` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门id',
  `update_user` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '是否删除',
  `desform_id` bigint DEFAULT NULL COMMENT '表单设计id',
  `desform_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表单设计名称',
  `desform_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '表单设计JSON',
  `group_desform_id` bigint DEFAULT NULL COMMENT '分组id',
  `is_open` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否开放',
  `create_user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人名称',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ind_desform_id` (`desform_id`) USING BTREE,
  KEY `ind_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='表单设计-历史数据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_address`
--

DROP TABLE IF EXISTS `mall_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_address` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '地址 ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收货人姓名',
  `consignee` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收货人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收货人电话',
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '省份',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '城市',
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '区县',
  `detail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址',
  `postal_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮政编码',
  `detail_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详细地址',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认：0 否，1 是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常，0 删除',
  `is_deleted` tinyint DEFAULT '0' COMMENT '是否已删除',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_mall_address_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2045531763184312322 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地址表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_addresses`
--

DROP TABLE IF EXISTS `mall_addresses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_addresses` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `postal_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_default` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_admin`
--

DROP TABLE IF EXISTS `mall_admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_admin` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '管理员 ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt 加密）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常，0 禁用',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_admin_role_backup`
--

DROP TABLE IF EXISTS `mall_admin_role_backup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_admin_role_backup` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `admin_id` bigint unsigned NOT NULL COMMENT '管理员 ID',
  `role_id` int unsigned NOT NULL COMMENT '角色 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role` (`admin_id`,`role_id`),
  KEY `fk_admin_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_admin_role_migration`
--

DROP TABLE IF EXISTS `mall_admin_role_migration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_admin_role_migration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '迁移 ID',
  `admin_id` bigint NOT NULL COMMENT '管理员 ID',
  `old_role_id` int NOT NULL COMMENT '原角色 ID',
  `new_role_id` bigint NOT NULL COMMENT '新角色 ID',
  `migration_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '迁移时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_old_role_id` (`old_role_id`),
  KEY `idx_new_role_id` (`new_role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商城管理员角色迁移记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_brand`
--

DROP TABLE IF EXISTS `mall_brand`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_brand` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '品牌ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '品牌名称',
  `logo` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '品牌 logo',
  `website` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌官网',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '品牌描述',
  `story` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '品牌故事',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `is_recommend` tinyint DEFAULT '0' COMMENT '是否推荐：0 否，1 是',
  `is_manufacturer` tinyint DEFAULT '0' COMMENT '是否制造商直供：0 否，1 是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常，0 禁用',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_mall_brand_tenant_id` (`tenant_id`),
  KEY `idx_mall_brand_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=2045116194911997955 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品牌表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_cart`
--

DROP TABLE IF EXISTS `mall_cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_price` decimal(10,2) NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `selected` int NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `selected_specs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '选中的规格（JSON 格式）',
  `selected_color` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '选中的颜色',
  `selected_size` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '选中的尺寸',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID（关联到具体的商品规格）',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `status` int DEFAULT '1' COMMENT '状态：1正常，0禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_user_product` (`user_id`,`product_id`),
  KEY `idx_mall_cart_tenant_id` (`tenant_id`),
  KEY `idx_mall_cart_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=2045435159110545410 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_category`
--

DROP TABLE IF EXISTS `mall_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '分类描述',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '分类级别：1 顶级，2 二级，3 三级',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类图标',
  `image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '分类图片',
  `banner` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '分类 banner',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常，0 禁用',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户 ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_mall_category_tenant_id` (`tenant_id`),
  KEY `idx_mall_category_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=2045014253238276099 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_category_attribute`
--

DROP TABLE IF EXISTS `mall_category_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_category_attribute` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性 ID',
  `category_id` bigint NOT NULL COMMENT '分类 ID',
  `name` varchar(100) NOT NULL COMMENT '属性名称',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '属性类型：1 单选，2 多选，3 文本输入，4 数字输入，5 日期',
  `is_required` tinyint DEFAULT '0' COMMENT '是否必填：0 否，1 是',
  `is_searchable` tinyint DEFAULT '0' COMMENT '是否可用于搜索筛选：0 否，1 是',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(255) DEFAULT NULL COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_type` (`type`),
  KEY `idx_mall_category_attribute_tenant_id` (`tenant_id`),
  KEY `idx_mall_category_attribute_is_deleted` (`is_deleted`),
  KEY `idx_mall_category_attribute_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2045417134407876611 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_category_attribute_value`
--

DROP TABLE IF EXISTS `mall_category_attribute_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_category_attribute_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性值 ID',
  `attribute_id` bigint NOT NULL COMMENT '属性 ID',
  `value` varchar(200) NOT NULL COMMENT '属性值',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(255) DEFAULT NULL COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_attribute_id` (`attribute_id`),
  KEY `idx_mall_category_attribute_value_tenant_id` (`tenant_id`),
  KEY `idx_mall_category_attribute_value_is_deleted` (`is_deleted`),
  KEY `idx_mall_category_attribute_value_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2045417134676312067 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类属性值表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_category_brand`
--

DROP TABLE IF EXISTS `mall_category_brand`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_category_brand` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联 ID',
  `category_id` bigint NOT NULL COMMENT '分类 ID',
  `brand_id` bigint NOT NULL COMMENT '品牌 ID',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(255) DEFAULT NULL COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_brand` (`category_id`,`brand_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_brand_id` (`brand_id`),
  KEY `idx_mall_category_brand_tenant_id` (`tenant_id`),
  KEY `idx_mall_category_brand_is_deleted` (`is_deleted`),
  KEY `idx_mall_category_brand_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类 - 品牌关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_category_param_template`
--

DROP TABLE IF EXISTS `mall_category_param_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_category_param_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板 ID',
  `category_id` bigint NOT NULL COMMENT '分类 ID',
  `name` varchar(100) NOT NULL COMMENT '参数名称',
  `value` varchar(500) NOT NULL COMMENT '参数值',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `type` int NOT NULL DEFAULT '1' COMMENT '参数类型：1-单选，2-多选',
  `is_required` int NOT NULL DEFAULT '0' COMMENT '是否必填',
  `is_searchable` int NOT NULL DEFAULT '0' COMMENT '是否可搜索',
  `tenant_id` varchar(255) DEFAULT NULL COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_mall_category_param_template_tenant_id` (`tenant_id`),
  KEY `idx_mall_category_param_template_is_deleted` (`is_deleted`),
  KEY `idx_mall_category_param_template_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2045421139175473155 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类参数模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_coupon`
--

DROP TABLE IF EXISTS `mall_coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_coupon` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '优惠券 ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '优惠券名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '优惠券码',
  `type` tinyint NOT NULL COMMENT '优惠券类型：1 固定金额，2 百分比',
  `value` decimal(10,2) NOT NULL COMMENT '优惠金额或百分比',
  `min_spend` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '最低消费金额',
  `max_discount` decimal(10,2) DEFAULT NULL COMMENT '最大优惠金额（针对百分比类型）',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `total_quantity` int NOT NULL DEFAULT '0' COMMENT '总发行量（0 表示无限制）',
  `used_quantity` int NOT NULL DEFAULT '0' COMMENT '已使用数量',
  `per_user_limit` int NOT NULL DEFAULT '1' COMMENT '每用户限领数量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常，0 禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`),
  KEY `idx_start_end_time` (`start_time`,`end_time`),
  KEY `idx_mall_coupon_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_logistics`
--

DROP TABLE IF EXISTS `mall_logistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_logistics` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '物流 ID',
  `order_id` bigint unsigned NOT NULL COMMENT '订单 ID',
  `tracking_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物流单号',
  `logistics_company` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物流公司',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物流状态：PENDING待发货，SHIPPED已发货，IN_TRANSIT运输中，DELIVERED已送达，SIGNED已签收',
  `latest_info` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最新物流信息',
  `tracking_details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '物流轨迹JSON',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户 ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_tracking_no` (`tracking_no`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_mall_logistics_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_member_account`
--

DROP TABLE IF EXISTS `mall_member_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_member_account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `level_id` bigint unsigned DEFAULT NULL COMMENT '当前会员等级 ID',
  `points` int NOT NULL DEFAULT '0' COMMENT '可用积分',
  `total_points` int NOT NULL DEFAULT '0' COMMENT '累计获得积分',
  `used_points` int NOT NULL DEFAULT '0' COMMENT '已使用积分',
  `growth` int NOT NULL DEFAULT '0' COMMENT '成长值',
  `experience` int NOT NULL DEFAULT '0' COMMENT '经验值',
  `membership_start` datetime DEFAULT NULL COMMENT '会员开始时间',
  `membership_end` datetime DEFAULT NULL COMMENT '会员到期时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0 非会员，1 会员，2 过期',
  `total_consumption` decimal(10,2) DEFAULT '0.00' COMMENT '累计消费金额',
  `order_count` int NOT NULL DEFAULT '0' COMMENT '订单数量',
  `last_checkin` date DEFAULT NULL COMMENT '最后签到日期',
  `continuous_checkin_days` int NOT NULL DEFAULT '0' COMMENT '连续签到天数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_level_id` (`level_id`),
  KEY `idx_status` (`status`),
  KEY `idx_membership_end` (`membership_end`),
  KEY `idx_mall_member_account_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_member_benefit`
--

DROP TABLE IF EXISTS `mall_member_benefit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_member_benefit` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `level_id` bigint unsigned NOT NULL COMMENT '会员等级 ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权益名称',
  `type` tinyint NOT NULL COMMENT '权益类型：1 折扣，2 包邮，3 专属客服，4 生日礼，5 退换货',
  `value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权益值（如折扣率、金额等）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '权益描述',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权益图标',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_level_id` (`level_id`),
  KEY `idx_type` (`type`),
  KEY `idx_mall_member_benefit_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_member_growth_log`
--

DROP TABLE IF EXISTS `mall_member_growth_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_member_growth_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `growth_value` int NOT NULL COMMENT '成长值变动（正数增加，负数减少）',
  `type` tinyint NOT NULL COMMENT '变动类型：1 消费，2 签到，3 活动，4 系统调整',
  `source_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源类型：ORDER, CHECKIN, ACTIVITY, SYSTEM',
  `source_id` bigint DEFAULT NULL COMMENT '来源 ID（如订单 ID）',
  `before_growth` int NOT NULL DEFAULT '0' COMMENT '变动前成长值',
  `after_growth` int NOT NULL DEFAULT '0' COMMENT '变动后成长值',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_source` (`source_type`,`source_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_mall_member_growth_log_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_member_level`
--

DROP TABLE IF EXISTS `mall_member_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_member_level` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '会员等级 ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '等级名称（如：普通会员、黄金会员）',
  `level_value` int NOT NULL COMMENT '等级值（1-9，值越大等级越高）',
  `min_growth` int NOT NULL DEFAULT '0' COMMENT '最低成长值要求',
  `max_growth` int DEFAULT NULL COMMENT '最高成长值（NULL 表示无上限）',
  `min_experience` int NOT NULL DEFAULT '0' COMMENT '最低经验值要求',
  `discount_rate` decimal(3,2) DEFAULT '1.00' COMMENT '折扣率（0.01-1.00）',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '等级图标',
  `benefits` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '权益描述（JSON 格式）',
  `price` decimal(10,2) DEFAULT '0.00' COMMENT '购买价格（0 表示免费）',
  `duration_days` int DEFAULT '365' COMMENT '有效期天数（0 表示永久）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_value` (`level_value`),
  KEY `idx_status` (`status`),
  KEY `idx_mall_member_level_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_member_points_log`
--

DROP TABLE IF EXISTS `mall_member_points_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_member_points_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `points_value` int NOT NULL COMMENT '积分变动（正数获得，负数消费）',
  `type` tinyint NOT NULL COMMENT '变动类型：1 获得，2 消费，3 过期，4 系统调整',
  `sub_type` tinyint DEFAULT NULL COMMENT '子类型：1 购物，2 签到，3 活动，4 兑换，5 退款',
  `source_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源类型：ORDER, CHECKIN, ACTIVITY, EXCHANGE, SYSTEM',
  `source_id` bigint DEFAULT NULL COMMENT '来源 ID（如订单 ID）',
  `before_points` int NOT NULL DEFAULT '0' COMMENT '变动前积分',
  `after_points` int NOT NULL DEFAULT '0' COMMENT '变动后积分',
  `expire_date` date DEFAULT NULL COMMENT '过期日期（仅获得类型）',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_sub_type` (`sub_type`),
  KEY `idx_source` (`source_type`,`source_id`),
  KEY `idx_expire_date` (`expire_date`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_mall_member_points_log_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_order`
--

DROP TABLE IF EXISTS `mall_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_order` (
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
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING 待支付，PAID 已支付，SHIPPED 已发货，COMPLETED 已完成，CANCELLED 已取消',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `fk_order_address` (`shipping_address_id`),
  KEY `idx_mall_order_tenant_id` (`tenant_id`),
  KEY `idx_mall_order_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_order_item`
--

DROP TABLE IF EXISTS `mall_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_order_item` (
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
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_mall_order_item_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_payment`
--

DROP TABLE IF EXISTS `mall_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付 ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `payment_no` varchar(64) NOT NULL COMMENT '支付流水号',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `payment_method` varchar(32) NOT NULL COMMENT '支付方式：BALANCE(余额), WECHAT(微信), ALIPAY(支付宝)',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态：PENDING(待支付), PAID(已支付), FAILED(失败), CANCELLED(已取消), REFUNDING(退款中), REFUNDED(已退款)',
  `transaction_id` varchar(128) DEFAULT NULL COMMENT '第三方支付交易号',
  `payment_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `expire_time` datetime NOT NULL COMMENT '支付过期时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_mall_payment_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_permission_backup`
--

DROP TABLE IF EXISTS `mall_permission_backup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_permission_backup` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '权限 ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限代码',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '权限类型：1 菜单，2 按钮',
  `parent_id` int unsigned NOT NULL DEFAULT '0' COMMENT '父权限 ID',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜单路径',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '菜单图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常，0 禁用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product`
--

DROP TABLE IF EXISTS `mall_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '产品 ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品名称',
  `subtitle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商品副标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '产品描述',
  `detail_description` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '产品详细描述（支持 HTML 富文本）',
  `tags` json DEFAULT NULL COMMENT '产品标签（JSON 数组，如：["热卖","新品","包邮"]）',
  `price` decimal(10,2) NOT NULL COMMENT '产品价格',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `cost_price` decimal(10,2) DEFAULT NULL COMMENT '成本价',
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存',
  `stock_warning` int DEFAULT '10' COMMENT '库存预警值',
  `sales` int NOT NULL DEFAULT '0' COMMENT '销量',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '商品浏览量',
  `favorite_count` int NOT NULL DEFAULT '0' COMMENT '商品收藏量',
  `user_rating` decimal(2,1) NOT NULL DEFAULT '0.0' COMMENT '用户评分：0.0-5.0',
  `user_review_count` int NOT NULL DEFAULT '0' COMMENT '用户评价总数',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重（值越大越靠前，相同则按创建时间倒序）',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '件' COMMENT '商品单位（默认为"件"）',
  `weight` decimal(10,3) DEFAULT NULL COMMENT '商品重量（单位：千克）',
  `product_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商品编号/SKU 编码',
  `min_purchase` int NOT NULL DEFAULT '1' COMMENT '最低起购数量',
  `max_purchase` int DEFAULT NULL COMMENT '单用户最高限购数量（NULL 表示不限制）',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `brand_id` bigint NOT NULL COMMENT '品牌ID',
  `main_image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '商品主图',
  `is_new` tinyint NOT NULL DEFAULT '0' COMMENT '是否新品：0 否，1 是',
  `is_hot` tinyint NOT NULL DEFAULT '0' COMMENT '是否热销：0 否，1 是',
  `is_recommend` tinyint NOT NULL DEFAULT '0' COMMENT '是否推荐：0 否，1 是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `on_shelf_time` datetime DEFAULT NULL COMMENT '商品上架时间',
  `off_shelf_time` datetime DEFAULT NULL COMMENT '商品下架时间',
  `promotion_id` bigint DEFAULT NULL COMMENT '当前生效的促销 ID',
  `promotion_price` decimal(10,2) DEFAULT NULL COMMENT '促销价格',
  `promotion_end_time` datetime DEFAULT NULL COMMENT '促销结束时间',
  `product_sn` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商品货号',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `gift_point` int NOT NULL DEFAULT '0' COMMENT '赠送积分',
  `gift_growth` int NOT NULL DEFAULT '0' COMMENT '赠送成长值',
  `use_point_limit` int NOT NULL DEFAULT '0' COMMENT '积分使用限制',
  `is_preview` tinyint NOT NULL DEFAULT '0' COMMENT '是否预告商品',
  `is_membership` tinyint NOT NULL DEFAULT '0' COMMENT '会员专属商品：0 否，1 是',
  `service_ids` json DEFAULT NULL COMMENT '服务保证 IDs',
  `promotion_type` int DEFAULT NULL COMMENT '促销类型',
  `detail_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详情页标题',
  `detail_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '详情页描述',
  `keywords` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关键字',
  `note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_brand_id` (`brand_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_new` (`is_new`),
  KEY `idx_is_hot` (`is_hot`),
  KEY `idx_status_sort_order` (`status`,`sort_order`),
  KEY `idx_view_sales` (`view_count`,`sales`),
  KEY `idx_user_rating_review` (`user_rating`,`user_review_count`),
  KEY `idx_status_on_shelf_time` (`status`,`on_shelf_time`),
  KEY `idx_price` (`price`),
  KEY `idx_mall_product_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2046634507299233795 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_album_image`
--

DROP TABLE IF EXISTS `mall_product_album_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_album_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `image_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '图片 URL',
  `sort` int NOT NULL DEFAULT '0',
  `is_main` int NOT NULL DEFAULT '0',
  `description` varchar(255) DEFAULT NULL,
  `width` int DEFAULT NULL,
  `height` int DEFAULT NULL,
  `size` bigint DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID（关联到具体的商品规格）',
  `color` varchar(50) DEFAULT NULL COMMENT '颜色属性标识',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sort` (`sort`),
  KEY `idx_mall_product_album_image_tenant_id` (`tenant_id`),
  KEY `idx_mall_product_album_image_is_deleted` (`is_deleted`),
  KEY `idx_mall_product_album_image_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2046634507655749634 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_attribute_value`
--

DROP TABLE IF EXISTS `mall_product_attribute_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_attribute_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `attribute_id` bigint NOT NULL COMMENT '属性 ID',
  `value` varchar(500) NOT NULL COMMENT '属性值',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_attribute` (`product_id`,`attribute_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_attribute_id` (`attribute_id`),
  KEY `idx_mall_product_attribute_value_tenant_id` (`tenant_id`),
  KEY `idx_mall_product_attribute_value_is_deleted` (`is_deleted`),
  KEY `idx_mall_product_attribute_value_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2046634507592835078 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品属性值表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_channel`
--

DROP TABLE IF EXISTS `mall_product_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `channel_id` bigint NOT NULL COMMENT '渠道 ID',
  `channel_product_id` varchar(100) DEFAULT NULL COMMENT '渠道商品 ID',
  `channel_product_url` varchar(500) DEFAULT NULL COMMENT '渠道商品链接',
  `channel_price` decimal(10,2) DEFAULT NULL COMMENT '渠道价格',
  `channel_stock` int DEFAULT NULL COMMENT '渠道库存',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态：active inactive',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_channel` (`product_id`,`channel_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品渠道关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_color`
--

DROP TABLE IF EXISTS `mall_product_color`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_color` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '颜色 ID',
  `product_id` bigint unsigned NOT NULL COMMENT '产品 ID',
  `color_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '颜色名称',
  `color_value` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '颜色值（如：#FF0000）',
  `stock` int NOT NULL DEFAULT '0' COMMENT '该颜色库存',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `create_user` bigint DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `status` int DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_mall_product_color_tenant_id` (`tenant_id`),
  KEY `idx_mall_product_color_is_deleted` (`is_deleted`),
  KEY `idx_mall_product_color_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品颜色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_image`
--

DROP TABLE IF EXISTS `mall_product_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_image` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '图片 ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `image_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '图片 URL',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `create_user` bigint DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `status` int DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_mall_product_image_tenant_id` (`tenant_id`),
  KEY `idx_mall_product_image_is_deleted` (`is_deleted`),
  KEY `idx_mall_product_image_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2046634507525726211 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_param`
--

DROP TABLE IF EXISTS `mall_product_param`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_param` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `param_name` varchar(100) NOT NULL COMMENT '参数名称',
  `param_value` varchar(500) NOT NULL COMMENT '参数值',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_mall_product_param_tenant_id` (`tenant_id`),
  KEY `idx_mall_product_param_is_deleted` (`is_deleted`),
  KEY `idx_mall_product_param_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2045421468742909954 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品参数表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_relation`
--

DROP TABLE IF EXISTS `mall_product_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联 ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `related_product_id` bigint NOT NULL COMMENT '关联商品ID',
  `type` tinyint NOT NULL COMMENT '关联类型：1 相关商品，2 搭配商品，3 推荐商品',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_related_type` (`product_id`,`related_product_id`,`type`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_related_product_id` (`related_product_id`),
  KEY `idx_type` (`type`),
  KEY `idx_mall_product_relation_tenant_id` (`tenant_id`),
  KEY `idx_mall_product_relation_is_deleted` (`is_deleted`),
  KEY `idx_mall_product_relation_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_size`
--

DROP TABLE IF EXISTS `mall_product_size`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_size` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '尺寸 ID',
  `product_id` bigint unsigned NOT NULL COMMENT '产品 ID',
  `size_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '尺寸名称',
  `stock` int NOT NULL DEFAULT '0' COMMENT '该尺寸库存',
  `tenant_id` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `create_user` bigint DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `status` int DEFAULT NULL,
  `is_deleted` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品尺寸表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_sku`
--

DROP TABLE IF EXISTS `mall_product_sku`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `sku_code` varchar(100) DEFAULT NULL COMMENT 'SKU 编码',
  `sku_name` varchar(200) DEFAULT NULL COMMENT 'SKU 规格名称',
  `spec1` varchar(100) DEFAULT NULL COMMENT '规格值 1',
  `spec2` varchar(100) DEFAULT NULL COMMENT '规格值 2',
  `spec3` varchar(100) DEFAULT NULL COMMENT '规格值 3',
  `spec4` varchar(100) DEFAULT NULL COMMENT '规格值 4',
  `price` decimal(10,2) DEFAULT NULL COMMENT 'SKU 价格',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT 'SKU 原价',
  `stock` int DEFAULT '0' COMMENT 'SKU 库存',
  `image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'SKU 图片',
  `status` tinyint DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `promotion_id` bigint DEFAULT NULL COMMENT '当前生效的促销 ID',
  `promotion_price` decimal(10,2) DEFAULT NULL COMMENT '促销价格',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku_code` (`sku_code`),
  KEY `idx_status` (`status`),
  KEY `idx_mall_product_sku_tenant_id` (`tenant_id`),
  KEY `idx_mall_product_sku_is_deleted` (`is_deleted`),
  KEY `idx_mall_product_sku_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2046634507525726212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品 SKU 表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_spec_attribute`
--

DROP TABLE IF EXISTS `mall_product_spec_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_spec_attribute` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性 ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `name` varchar(100) NOT NULL COMMENT '属性名称（如：颜色、尺码）',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_mall_product_spec_attribute_tenant_id` (`tenant_id`),
  KEY `idx_mall_product_spec_attribute_is_deleted` (`is_deleted`),
  KEY `idx_mall_product_spec_attribute_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2046634507785773061 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品规格属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_spec_value`
--

DROP TABLE IF EXISTS `mall_product_spec_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_spec_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性值 ID',
  `attribute_id` bigint NOT NULL COMMENT '属性 ID',
  `value` varchar(100) NOT NULL COMMENT '属性值（如：红色、XL）',
  `image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '规格值图片',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_attribute_id` (`attribute_id`),
  KEY `idx_mall_product_spec_value_tenant_id` (`tenant_id`),
  KEY `idx_mall_product_spec_value_is_deleted` (`is_deleted`),
  KEY `idx_mall_product_spec_value_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2046634507785773062 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品规格属性值表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_product_specification`
--

DROP TABLE IF EXISTS `mall_product_specification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_product_specification` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '规格 ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `spec_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规格名称',
  `spec_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_mall_product_specification_tenant_id` (`tenant_id`),
  KEY `idx_mall_product_specification_is_deleted` (`is_deleted`),
  KEY `idx_mall_product_specification_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品规格表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_promotion`
--

DROP TABLE IF EXISTS `mall_promotion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_promotion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '促销 ID',
  `name` varchar(200) NOT NULL COMMENT '促销名称',
  `description` varchar(500) DEFAULT NULL COMMENT '促销描述',
  `type` tinyint NOT NULL COMMENT '促销类型：1 满减，2 折扣，3 秒杀，4 团购',
  `rules` json NOT NULL COMMENT '促销规则 JSON',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `status` tinyint DEFAULT '0' COMMENT '状态：0 未开始，1 进行中，2 已结束，3 已停用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`),
  KEY `idx_mall_promotion_tenant_id` (`tenant_id`),
  KEY `idx_mall_promotion_is_deleted` (`is_deleted`),
  KEY `idx_mall_promotion_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='促销规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_promotion_product`
--

DROP TABLE IF EXISTS `mall_promotion_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_promotion_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `promotion_id` bigint NOT NULL COMMENT '促销 ID',
  `product_id` bigint NOT NULL COMMENT '商品 ID',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID（为空表示作用于商品所有 SKU）',
  `promotion_price` decimal(10,2) DEFAULT NULL COMMENT '促销价格',
  `limit_quantity` int DEFAULT NULL COMMENT '限购数量',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_promotion_product_sku` (`promotion_id`,`product_id`,`sku_id`),
  KEY `idx_promotion_id` (`promotion_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_mall_promotion_product_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='促销商品关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_refund`
--

DROP TABLE IF EXISTS `mall_refund`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_refund` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '退款 ID',
  `payment_no` varchar(64) NOT NULL COMMENT '支付流水号',
  `refund_no` varchar(64) NOT NULL COMMENT '退款流水号',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `refund_amount` decimal(10,2) NOT NULL COMMENT '退款金额',
  `reason` varchar(500) DEFAULT NULL COMMENT '退款原因',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '退款状态：PENDING(待退款), SUCCESS(成功), FAILED(失败), CANCELLED(已取消)',
  `refund_method` varchar(32) NOT NULL COMMENT '退款方式：BALANCE(退回余额), WECHAT(微信), ALIPAY(支付宝)',
  `transaction_id` varchar(128) DEFAULT NULL COMMENT '第三方退款交易号',
  `refund_time` datetime DEFAULT NULL COMMENT '退款完成时间',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_payment_no` (`payment_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_mall_refund_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_review`
--

DROP TABLE IF EXISTS `mall_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_review` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '评价 ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `product_id` bigint unsigned NOT NULL COMMENT '产品 ID',
  `order_id` bigint unsigned NOT NULL COMMENT '订单 ID',
  `order_item_id` bigint unsigned NOT NULL COMMENT '订单项 ID',
  `rating` tinyint NOT NULL COMMENT '评分：1-5 星',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评价内容',
  `anonymous` tinyint NOT NULL DEFAULT '0' COMMENT '是否匿名：0 否，1 是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常，0 禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_rating` (`rating`),
  KEY `fk_review_order_item` (`order_item_id`),
  KEY `idx_mall_review_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_review_image`
--

DROP TABLE IF EXISTS `mall_review_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_review_image` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '评价图片 ID',
  `review_id` bigint unsigned NOT NULL COMMENT '评价 ID',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片地址',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_review_id` (`review_id`),
  KEY `idx_mall_review_image_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_role_backup`
--

DROP TABLE IF EXISTS `mall_role_backup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_role_backup` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '角色 ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常，0 禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_role_menu_migration`
--

DROP TABLE IF EXISTS `mall_role_menu_migration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_role_menu_migration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '迁移 ID',
  `role_id` bigint NOT NULL COMMENT '角色 ID',
  `menu_id` bigint NOT NULL COMMENT '菜单 ID',
  `migration_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '迁移时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单迁移记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_role_permission_backup`
--

DROP TABLE IF EXISTS `mall_role_permission_backup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_role_permission_backup` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` int unsigned NOT NULL COMMENT '角色 ID',
  `permission_id` int unsigned NOT NULL COMMENT '权限 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `fk_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_sales_channel`
--

DROP TABLE IF EXISTS `mall_sales_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_sales_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '渠道名称',
  `code` varchar(50) NOT NULL COMMENT '渠道代码',
  `icon` varchar(255) DEFAULT NULL COMMENT '渠道图标',
  `description` varchar(500) DEFAULT NULL COMMENT '渠道描述',
  `config` text COMMENT '渠道配置 JSON',
  `status` int DEFAULT '1' COMMENT '状态：1 启用 0 禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_mall_sales_channel_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售渠道表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_sku_stock_log`
--

DROP TABLE IF EXISTS `mall_sku_stock_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_sku_stock_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志 ID',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `product_id` bigint NOT NULL COMMENT '商品 ID',
  `type` tinyint NOT NULL COMMENT '变动类型：1 入库，2 出库，3 调整',
  `quantity` int NOT NULL COMMENT '变动数量（正数表示增加，负数表示减少）',
  `before_stock` int NOT NULL COMMENT '变动前库存',
  `after_stock` int NOT NULL COMMENT '变动后库存',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人名称',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(255) DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_type` (`type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_mall_sku_stock_log_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SKU 库存变动日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_user`
--

DROP TABLE IF EXISTS `mall_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `gender` tinyint DEFAULT '0' COMMENT '性别：0 未知，1 男，2 女',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `register_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 正常，0 禁用',
  `is_admin` tinyint NOT NULL DEFAULT '0' COMMENT '是否管理员：0 否，1 是',
  `wx_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信用户唯一标识',
  `wx_openid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `member_level` tinyint DEFAULT '0' COMMENT '会员等级：0 普通用户，1-9 对应不同等级',
  `member_points` int DEFAULT '0' COMMENT '会员积分',
  `member_growth` int DEFAULT '0' COMMENT '会员成长值',
  `member_experience` int DEFAULT '0' COMMENT '会员经验值',
  `member_start_time` datetime DEFAULT NULL COMMENT '会员开始时间',
  `member_end_time` datetime DEFAULT NULL COMMENT '会员到期时间',
  `member_status` tinyint DEFAULT '0' COMMENT '会员状态：0 非会员，1 会员，2 过期会员',
  `total_consumption` decimal(10,2) DEFAULT '0.00' COMMENT '累计消费金额',
  `last_member_update` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '会员信息最后更新时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `wx_openid` (`wx_openid`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mall_user_coupon`
--

DROP TABLE IF EXISTS `mall_user_coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mall_user_coupon` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户优惠券 ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户 ID',
  `coupon_id` bigint unsigned NOT NULL COMMENT '优惠券 ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 未使用，2 已使用，3 已过期',
  `is_deleted` tinyint DEFAULT '0' COMMENT '是否已删除',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `used_at` datetime DEFAULT NULL COMMENT '使用时间',
  `tenant_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户 ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_status` (`status`),
  KEY `idx_mall_user_coupon_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_id_mapping`
--

DROP TABLE IF EXISTS `user_id_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_id_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '映射 ID',
  `old_user_id` bigint NOT NULL COMMENT '原用户 ID',
  `new_user_id` bigint NOT NULL COMMENT '新用户 ID',
  `source_table` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源表名',
  `migration_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '迁移时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_old_user_id` (`old_user_id`),
  KEY `idx_new_user_id` (`new_user_id`),
  KEY `idx_source_table` (`source_table`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户 ID 映射表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `v_backup_tables_info`
--

DROP TABLE IF EXISTS `v_backup_tables_info`;
/*!50001 DROP VIEW IF EXISTS `v_backup_tables_info`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_backup_tables_info` AS SELECT 
 1 AS `table_name`,
 1 AS `description`,
 1 AS `backup_date`,
 1 AS `remark`*/;
SET character_set_client = @saved_cs_client;

--
-- Dumping routines for database 'blade'
--

--
-- Final view structure for view `v_backup_tables_info`
--

/*!50001 DROP VIEW IF EXISTS `v_backup_tables_info`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_backup_tables_info` AS select 'mall_role_backup' AS `table_name`,'原商城角色表 - 已迁移到 blade_role' AS `description`,'2026-03-17' AS `backup_date`,'数据迁移后备份，可安全删除' AS `remark` union all select 'mall_permission_backup' AS `table_name`,'原商城权限表 - 已迁移到 blade_menu' AS `description`,'2026-03-17' AS `backup_date`,'数据迁移后备份，可安全删除' AS `remark` union all select 'mall_admin_role_backup' AS `table_name`,'原商城管理员角色关联表 - 已迁移到 blade_user.role_id' AS `description`,'2026-03-17' AS `backup_date`,'数据迁移后备份，可安全删除' AS `remark` union all select 'mall_role_permission_backup' AS `table_name`,'原商城角色权限关联表 - 已迁移到 blade_role_menu' AS `description`,'2026-03-17' AS `backup_date`,'数据迁移后备份，可安全删除' AS `remark` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-26  9:36:51
