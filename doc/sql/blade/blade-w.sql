-- MySQL dump 10.13  Distrib 8.0.23, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: blade_workflow
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
-- Table structure for table `wf_approval_log`
--

DROP TABLE IF EXISTS `wf_approval_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_approval_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inst_id` bigint unsigned NOT NULL COMMENT '流程实例ID',
  `task_id` bigint unsigned DEFAULT NULL COMMENT '任务ID',
  `node_key` varchar(64) NOT NULL DEFAULT '' COMMENT '节点Key',
  `operator` bigint NOT NULL COMMENT '操作人',
  `log_type` varchar(2) NOT NULL COMMENT '0批准 2提交 3退回 7转发 9批注 h转办 s督办 t抄送 y批示（对齐 RequestLogType）',
  `opinion` varchar(2000) NOT NULL DEFAULT '' COMMENT '审批意见',
  `operate_time` datetime NOT NULL COMMENT '操作时间（分区列）',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`,`operate_time`),
  KEY `idx_inst_time` (`inst_id`,`operate_time`),
  KEY `idx_operator_time` (`operator`,`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批流转记录'
/*!50500 PARTITION BY RANGE  COLUMNS(operate_time)
(PARTITION p202609 VALUES LESS THAN ('2026-10-01') ENGINE = InnoDB,
 PARTITION p202610 VALUES LESS THAN ('2026-11-01') ENGINE = InnoDB,
 PARTITION p202611 VALUES LESS THAN ('2026-12-01') ENGINE = InnoDB,
 PARTITION p202612 VALUES LESS THAN ('2027-01-01') ENGINE = InnoDB,
 PARTITION p_max VALUES LESS THAN (MAXVALUE) ENGINE = InnoDB) */;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_approval_log`
--

LOCK TABLES `wf_approval_log` WRITE;
/*!40000 ALTER TABLE `wf_approval_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_approval_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_form_snapshot`
--

DROP TABLE IF EXISTS `wf_form_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_form_snapshot` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inst_id` bigint unsigned NOT NULL COMMENT '流程实例ID',
  `node_key` varchar(64) NOT NULL DEFAULT '' COMMENT '节点Key',
  `layout_id` bigint DEFAULT NULL COMMENT '当时的 form_layout.id（布局快照）',
  `data_json` json NOT NULL COMMENT '主表+明细全量值',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_inst_node` (`inst_id`,`node_key`),
  KEY `idx_inst_time` (`inst_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表单数据快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_form_snapshot`
--

LOCK TABLES `wf_form_snapshot` WRITE;
/*!40000 ALTER TABLE `wf_form_snapshot` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_form_snapshot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_instance`
--

DROP TABLE IF EXISTS `wf_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_instance` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `engine_inst_id` varchar(64) NOT NULL DEFAULT '' COMMENT '引擎实例ID（Flowable PROC_INST_ID_），弱关联，不依赖 ACT_* 表',
  `def_id` bigint unsigned NOT NULL COMMENT '流程定义ID',
  `form_id` bigint NOT NULL COMMENT '表单ID（workflow_bill.id）',
  `data_id` bigint NOT NULL COMMENT '业务数据ID（formtable_main_{id}.id）',
  `title` varchar(400) NOT NULL DEFAULT '' COMMENT '流程标题',
  `biz_key` varchar(128) NOT NULL COMMENT '业务主键 formId:dataId，便于反查',
  `current_node_key` varchar(64) NOT NULL DEFAULT '' COMMENT '当前节点',
  `starter` bigint NOT NULL COMMENT '发起人',
  `start_time` datetime NOT NULL COMMENT '发起时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `urgency` tinyint NOT NULL DEFAULT '0' COMMENT '紧急程度 0/1/2（对齐 requestlevel）',
  `parent_id` bigint unsigned DEFAULT NULL COMMENT '父流程实例（子流程）',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '0' COMMENT '0运行中 1通过 2不通过 3撤销 4暂停（对齐 ecology currentstatus 语义）',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_engine_inst` (`engine_inst_id`),
  UNIQUE KEY `uk_biz_key` (`biz_key`),
  KEY `idx_status_start` (`status`,`start_time`),
  KEY `idx_starter_status` (`starter`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程实例';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_instance`
--

LOCK TABLES `wf_instance` WRITE;
/*!40000 ALTER TABLE `wf_instance` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_migration_map`
--

DROP TABLE IF EXISTS `wf_migration_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_migration_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ec_wf_id` bigint NOT NULL COMMENT 'ecology workflow_base.id',
  `ec_request_id` bigint DEFAULT NULL COMMENT 'ecology workflow_requestbase.requestid（在途实例）',
  `ec_node_id` bigint DEFAULT NULL COMMENT 'ecology 节点ID',
  `new_def_id` bigint unsigned DEFAULT NULL COMMENT '新流程定义ID',
  `new_inst_id` bigint unsigned DEFAULT NULL COMMENT '新流程实例ID',
  `new_node_key` varchar(64) DEFAULT NULL COMMENT '新节点Key',
  `migrate_time` datetime NOT NULL COMMENT '迁移时间',
  `err_msg` varchar(1000) NOT NULL DEFAULT '' COMMENT '错误信息',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '0' COMMENT '0待迁移 1成功 2失败',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ec_wf` (`ec_wf_id`,`ec_node_id`),
  KEY `idx_ec_request` (`ec_request_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ecology 存量迁移映射';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_migration_map`
--

LOCK TABLES `wf_migration_map` WRITE;
/*!40000 ALTER TABLE `wf_migration_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_migration_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_node_detail_perm`
--

DROP TABLE IF EXISTS `wf_node_detail_perm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_node_detail_perm` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `def_id` bigint unsigned NOT NULL COMMENT '流程定义ID',
  `node_key` varchar(64) NOT NULL COMMENT '节点Key',
  `dt_index` int NOT NULL COMMENT '明细表序号',
  `can_add` tinyint NOT NULL DEFAULT '1' COMMENT '可新增行',
  `can_edit` tinyint NOT NULL DEFAULT '1' COMMENT '可编辑行',
  `can_delete` tinyint NOT NULL DEFAULT '1' COMMENT '可删除行',
  `hide_empty` tinyint NOT NULL DEFAULT '0' COMMENT '隐藏空行',
  `default_rows` int NOT NULL DEFAULT '1' COMMENT '默认行数',
  `required` tinyint NOT NULL DEFAULT '0' COMMENT '必须至少一条',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_dt` (`def_id`,`node_key`,`dt_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点级明细表权限';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_node_detail_perm`
--

LOCK TABLES `wf_node_detail_perm` WRITE;
/*!40000 ALTER TABLE `wf_node_detail_perm` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_node_detail_perm` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_node_field_perm`
--

DROP TABLE IF EXISTS `wf_node_field_perm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_node_field_perm` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `def_id` bigint unsigned NOT NULL COMMENT '流程定义ID',
  `node_key` varchar(64) NOT NULL COMMENT '节点Key',
  `scope` varchar(32) NOT NULL COMMENT 'main 或 dt{idx}（与 data-excelp-scope 对齐）',
  `field_name` varchar(128) NOT NULL COMMENT '字段名（与 data-excelp-field 对齐）',
  `perm` tinyint NOT NULL DEFAULT '2' COMMENT '0隐藏 1只读 2可编辑 3必填（对齐 ecology fieldattr）',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_scope_field` (`def_id`,`node_key`,`scope`,`field_name`),
  KEY `idx_def_node` (`def_id`,`node_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点级字段权限矩阵';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_node_field_perm`
--

LOCK TABLES `wf_node_field_perm` WRITE;
/*!40000 ALTER TABLE `wf_node_field_perm` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_node_field_perm` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_node_link`
--

DROP TABLE IF EXISTS `wf_node_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_node_link` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `def_id` bigint unsigned NOT NULL COMMENT '流程定义ID',
  `from_node_key` varchar(64) NOT NULL COMMENT '源节点',
  `to_node_key` varchar(64) NOT NULL COMMENT '目标节点',
  `is_reject` tinyint NOT NULL DEFAULT '0' COMMENT '是否退回线（对齐 ISREJECT）',
  `is_must_pass` tinyint NOT NULL DEFAULT '0' COMMENT '分叉必经分支（对齐 ISMUSTPASS）',
  `condition_expr` text COMMENT '条件表达式',
  `condition_cn` varchar(1000) NOT NULL DEFAULT '' COMMENT '条件中文描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_def_from` (`def_id`,`from_node_key`),
  KEY `idx_def_reject` (`def_id`,`is_reject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程出口（连线）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_node_link`
--

LOCK TABLES `wf_node_link` WRITE;
/*!40000 ALTER TABLE `wf_node_link` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_node_link` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_node_operator`
--

DROP TABLE IF EXISTS `wf_node_operator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_node_operator` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `node_id` bigint unsigned NOT NULL COMMENT '流程节点ID',
  `group_no` int NOT NULL DEFAULT '1' COMMENT '操作组序号',
  `op_type` int NOT NULL COMMENT '操作者类型（对齐 OperatorDBType：3人员 1部门 30分部 2角色 58岗位 4所有人 / 17创建人本人 18创建人上级 19本部门 / 40本人 41上级 / 5字段-人员 6字段-人员上级 42字段-部门 43字段-角色 / 99矩阵 97接口 98SQL）',
  `obj_id` varchar(64) NOT NULL DEFAULT '' COMMENT '对象ID或表单字段名',
  `level_min` int DEFAULT NULL COMMENT '安全级别下限（对齐 LEVEL_N）',
  `level_max` int DEFAULT NULL COMMENT '安全级别上限（对齐 LEVEL2_N）',
  `bhxj` tinyint NOT NULL DEFAULT '0' COMMENT '0本部 1含下级 2含上级 3逐级向上（对齐 BHXJ）',
  `sign_order` tinyint NOT NULL DEFAULT '0' COMMENT '会签关系（对齐 SIGNORDER）',
  `batch_no` int NOT NULL DEFAULT '0' COMMENT '批次（依次审批顺序，对齐 ORDERS）',
  `condition_json` json DEFAULT NULL COMMENT '操作者生效条件',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_node_group` (`node_id`,`group_no`),
  KEY `idx_node_type` (`node_id`,`op_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点操作者';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_node_operator`
--

LOCK TABLES `wf_node_operator` WRITE;
/*!40000 ALTER TABLE `wf_node_operator` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_node_operator` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_process_definition`
--

DROP TABLE IF EXISTS `wf_process_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_process_definition` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `proc_key` varchar(64) NOT NULL COMMENT '引擎流程Key（= BPMN process id）',
  `form_id` bigint NOT NULL COMMENT '关联 workflow_bill.id（表单）',
  `name` varchar(200) NOT NULL COMMENT '流程名称',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `is_free` tinyint NOT NULL DEFAULT '0' COMMENT '是否自由流程',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '0' COMMENT '0草稿 1已发布 2停用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除:1已删 0未删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_proc_key_version` (`proc_key`,`version`),
  KEY `idx_form_status` (`form_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_process_definition`
--

LOCK TABLES `wf_process_definition` WRITE;
/*!40000 ALTER TABLE `wf_process_definition` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_process_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_process_node`
--

DROP TABLE IF EXISTS `wf_process_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_process_node` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `def_id` bigint unsigned NOT NULL COMMENT '流程定义ID',
  `node_key` varchar(64) NOT NULL COMMENT '引擎节点ID',
  `node_name` varchar(200) NOT NULL COMMENT '节点名称',
  `node_type` tinyint NOT NULL COMMENT '0创建 1审批 2提交 3归档 5等待 6自动处理（对齐 ecology NodeType）',
  `sign_order` tinyint NOT NULL DEFAULT '0' COMMENT '0或签 1会签 2依次 3抄送不需提交 4抄送需提交（对齐 SignOrder）',
  `merge_type` tinyint NOT NULL DEFAULT '0' COMMENT '0普通 1分叉起点 2分叉中间 3按分支数合并 4指定分支合并 5比例合并（对齐 nodeattribute）',
  `pass_num` int NOT NULL DEFAULT '0' COMMENT '合并阈值：分支数或百分比（对齐 passnum）',
  `allow_reject` tinyint NOT NULL DEFAULT '1' COMMENT '允许退回',
  `allow_forward` tinyint NOT NULL DEFAULT '0' COMMENT '允许转发/转办',
  `auto_approve` tinyint NOT NULL DEFAULT '0' COMMENT '自动批准',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `ext_json` json DEFAULT NULL COMMENT '扩展属性（超时、提醒、签章等）',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_def_node` (`def_id`,`node_key`),
  KEY `idx_def_sort` (`def_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程节点';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_process_node`
--

LOCK TABLES `wf_process_node` WRITE;
/*!40000 ALTER TABLE `wf_process_node` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_process_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_task`
--

DROP TABLE IF EXISTS `wf_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inst_id` bigint unsigned NOT NULL COMMENT '流程实例ID',
  `engine_task_id` varchar(64) NOT NULL DEFAULT '' COMMENT '引擎任务ID（Flowable），弱关联',
  `node_key` varchar(64) NOT NULL COMMENT '节点Key',
  `assignee` bigint NOT NULL COMMENT '办理人',
  `original_user` bigint DEFAULT NULL COMMENT '代理人代办时的原处理人',
  `sign_order` tinyint NOT NULL DEFAULT '0' COMMENT '会签关系',
  `receive_time` datetime DEFAULT NULL COMMENT '接收时间',
  `operate_time` datetime DEFAULT NULL COMMENT '处理时间',
  `due_time` datetime DEFAULT NULL COMMENT '截止时间',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '0' COMMENT '0待办 2已办 4办结 6自动提交 7协办 8抄送 11传阅（对齐 isremark）',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_assignee_status` (`assignee`,`status`),
  KEY `idx_inst_node` (`inst_id`,`node_key`),
  KEY `idx_engine_task` (`engine_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程任务（待办/已办）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_task`
--

LOCK TABLES `wf_task` WRITE;
/*!40000 ALTER TABLE `wf_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_task` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-09 17:35:37
