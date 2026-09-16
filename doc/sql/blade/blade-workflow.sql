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
-- Table structure for table `act_evt_log`
--

DROP TABLE IF EXISTS `act_evt_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_evt_log` (
  `LOG_NR_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DATA_` longblob,
  `LOCK_OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IS_PROCESSED_` tinyint DEFAULT '0',
  PRIMARY KEY (`LOG_NR_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_evt_log`
--

LOCK TABLES `act_evt_log` WRITE;
/*!40000 ALTER TABLE `act_evt_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_evt_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ge_bytearray`
--

DROP TABLE IF EXISTS `act_ge_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ge_bytearray` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `BYTES_` longblob,
  `GENERATED_` tinyint DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_BYTEARR_DEPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_BYTEARR_DEPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ge_bytearray`
--

LOCK TABLES `act_ge_bytearray` WRITE;
/*!40000 ALTER TABLE `act_ge_bytearray` DISABLE KEYS */;
INSERT INTO `act_ge_bytearray` VALUES ('2',1,'D:\\workproject\\springbladeandreact\\springBlade\\blade-service\\blade-workflow\\target\\classes\\processes\\simple-approval.bpmn20.xml','1',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\r\n             xmlns:flowable=\"http://flowable.org/bpmn\"\r\n             targetNamespace=\"http://springblade.workflow\"\r\n             id=\"definitions_simpleApproval\">\r\n\r\n    <process id=\"simpleApproval\" name=\"Simple Approval\" isExecutable=\"true\">\r\n        <startEvent id=\"startEvent\" name=\"发起\"/>\r\n        <userTask id=\"approveTask\" name=\"审批\" flowable:assignee=\"${approver}\"/>\r\n        <endEvent id=\"endEvent\" name=\"结束\"/>\r\n\r\n        <sequenceFlow id=\"flow_start_approve\" sourceRef=\"startEvent\" targetRef=\"approveTask\"/>\r\n        <sequenceFlow id=\"flow_approve_end\" sourceRef=\"approveTask\" targetRef=\"endEvent\"/>\r\n    </process>\r\n\r\n</definitions>\r\n',0);
/*!40000 ALTER TABLE `act_ge_bytearray` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ge_property`
--

DROP TABLE IF EXISTS `act_ge_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ge_property` (
  `NAME_` varchar(64) COLLATE utf8_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ge_property`
--

LOCK TABLES `act_ge_property` WRITE;
/*!40000 ALTER TABLE `act_ge_property` DISABLE KEYS */;
INSERT INTO `act_ge_property` VALUES ('cfg.execution-related-entities-count','true',1),('cfg.task-related-entities-count','true',1),('common.schema.version','7.1.0.2',1),('eventregistry.schema.version','7.1.0.2',1),('next.dbid','5001',3),('schema.history','create(7.1.0.2)',1),('schema.version','7.1.0.2',1);
/*!40000 ALTER TABLE `act_ge_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_actinst`
--

DROP TABLE IF EXISTS `act_hi_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_actinst` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_actinst`
--

LOCK TABLES `act_hi_actinst` WRITE;
/*!40000 ALTER TABLE `act_hi_actinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_actinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_attachment`
--

DROP TABLE IF EXISTS `act_hi_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_attachment` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `URL_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `CONTENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_attachment`
--

LOCK TABLES `act_hi_attachment` WRITE;
/*!40000 ALTER TABLE `act_hi_attachment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_comment`
--

DROP TABLE IF EXISTS `act_hi_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_comment` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACTION_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `MESSAGE_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `FULL_MSG_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_comment`
--

LOCK TABLES `act_hi_comment` WRITE;
/*!40000 ALTER TABLE `act_hi_comment` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_detail`
--

DROP TABLE IF EXISTS `act_hi_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_detail` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACT_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin NOT NULL,
  `VAR_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_TIME` (`TIME_`),
  KEY `ACT_IDX_HI_DETAIL_NAME` (`NAME_`),
  KEY `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_detail`
--

LOCK TABLES `act_hi_detail` WRITE;
/*!40000 ALTER TABLE `act_hi_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_entitylink`
--

DROP TABLE IF EXISTS `act_hi_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_entitylink` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_entitylink`
--

LOCK TABLES `act_hi_entitylink` WRITE;
/*!40000 ALTER TABLE `act_hi_entitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_entitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_identitylink`
--

DROP TABLE IF EXISTS `act_hi_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_identitylink` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_identitylink`
--

LOCK TABLES `act_hi_identitylink` WRITE;
/*!40000 ALTER TABLE `act_hi_identitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_identitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_procinst`
--

DROP TABLE IF EXISTS `act_hi_procinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_procinst` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `END_ACT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUPER_PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDX_HI_PRO_SUPER_PROCINST` (`SUPER_PROCESS_INSTANCE_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_procinst`
--

LOCK TABLES `act_hi_procinst` WRITE;
/*!40000 ALTER TABLE `act_hi_procinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_procinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_taskinst`
--

DROP TABLE IF EXISTS `act_hi_taskinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_taskinst` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `COMPLETED_BY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `FORM_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_taskinst`
--

LOCK TABLES `act_hi_taskinst` WRITE;
/*!40000 ALTER TABLE `act_hi_taskinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_taskinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_tsk_log`
--

DROP TABLE IF EXISTS `act_hi_tsk_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_tsk_log` (
  `ID_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DATA_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ACT_HI_TSK_LOG_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_tsk_log`
--

LOCK TABLES `act_hi_tsk_log` WRITE;
/*!40000 ALTER TABLE `act_hi_tsk_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_tsk_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_hi_varinst`
--

DROP TABLE IF EXISTS `act_hi_varinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_hi_varinst` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin NOT NULL,
  `VAR_TYPE_` varchar(100) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_EXE` (`EXECUTION_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_hi_varinst`
--

LOCK TABLES `act_hi_varinst` WRITE;
/*!40000 ALTER TABLE `act_hi_varinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_hi_varinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_bytearray`
--

DROP TABLE IF EXISTS `act_id_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_bytearray` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_bytearray`
--

LOCK TABLES `act_id_bytearray` WRITE;
/*!40000 ALTER TABLE `act_id_bytearray` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_bytearray` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_group`
--

DROP TABLE IF EXISTS `act_id_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_group` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_group`
--

LOCK TABLES `act_id_group` WRITE;
/*!40000 ALTER TABLE `act_id_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_info`
--

DROP TABLE IF EXISTS `act_id_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_info` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `VALUE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PASSWORD_` longblob,
  `PARENT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_info`
--

LOCK TABLES `act_id_info` WRITE;
/*!40000 ALTER TABLE `act_id_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_membership`
--

DROP TABLE IF EXISTS `act_id_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_membership` (
  `USER_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `GROUP_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`USER_ID_`,`GROUP_ID_`),
  KEY `ACT_FK_MEMB_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_MEMB_GROUP` FOREIGN KEY (`GROUP_ID_`) REFERENCES `act_id_group` (`ID_`),
  CONSTRAINT `ACT_FK_MEMB_USER` FOREIGN KEY (`USER_ID_`) REFERENCES `act_id_user` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_membership`
--

LOCK TABLES `act_id_membership` WRITE;
/*!40000 ALTER TABLE `act_id_membership` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_membership` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_priv`
--

DROP TABLE IF EXISTS `act_id_priv`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_priv` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PRIV_NAME` (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_priv`
--

LOCK TABLES `act_id_priv` WRITE;
/*!40000 ALTER TABLE `act_id_priv` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_priv` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_priv_mapping`
--

DROP TABLE IF EXISTS `act_id_priv_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_priv_mapping` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `PRIV_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_PRIV_MAPPING` (`PRIV_ID_`),
  KEY `ACT_IDX_PRIV_USER` (`USER_ID_`),
  KEY `ACT_IDX_PRIV_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_FK_PRIV_MAPPING` FOREIGN KEY (`PRIV_ID_`) REFERENCES `act_id_priv` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_priv_mapping`
--

LOCK TABLES `act_id_priv_mapping` WRITE;
/*!40000 ALTER TABLE `act_id_priv_mapping` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_priv_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_property`
--

DROP TABLE IF EXISTS `act_id_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_property` (
  `NAME_` varchar(64) COLLATE utf8_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_property`
--

LOCK TABLES `act_id_property` WRITE;
/*!40000 ALTER TABLE `act_id_property` DISABLE KEYS */;
INSERT INTO `act_id_property` VALUES ('schema.version','7.1.0.2',1);
/*!40000 ALTER TABLE `act_id_property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_token`
--

DROP TABLE IF EXISTS `act_id_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_token` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TOKEN_VALUE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TOKEN_DATE_` timestamp(3) NULL DEFAULT NULL,
  `IP_ADDRESS_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `USER_AGENT_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TOKEN_DATA_` varchar(2000) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_token`
--

LOCK TABLES `act_id_token` WRITE;
/*!40000 ALTER TABLE `act_id_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_id_user`
--

DROP TABLE IF EXISTS `act_id_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_id_user` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `FIRST_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `LAST_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DISPLAY_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `EMAIL_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PWD_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PICTURE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_id_user`
--

LOCK TABLES `act_id_user` WRITE;
/*!40000 ALTER TABLE `act_id_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_id_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_procdef_info`
--

DROP TABLE IF EXISTS `act_procdef_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_procdef_info` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `INFO_JSON_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`),
  CONSTRAINT `ACT_FK_INFO_JSON_BA` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_INFO_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_procdef_info`
--

LOCK TABLES `act_procdef_info` WRITE;
/*!40000 ALTER TABLE `act_procdef_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_procdef_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_re_deployment`
--

DROP TABLE IF EXISTS `act_re_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_deployment` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  `DEPLOY_TIME_` timestamp(3) NULL DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ENGINE_VERSION_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_deployment`
--

LOCK TABLES `act_re_deployment` WRITE;
/*!40000 ALTER TABLE `act_re_deployment` DISABLE KEYS */;
INSERT INTO `act_re_deployment` VALUES ('1','SpringAutoDeployment',NULL,NULL,'','2026-09-09 12:09:23.792',NULL,NULL,'1',NULL);
/*!40000 ALTER TABLE `act_re_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_re_model`
--

DROP TABLE IF EXISTS `act_re_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_model` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EDITOR_SOURCE_VALUE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
  KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
  KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_model`
--

LOCK TABLES `act_re_model` WRITE;
/*!40000 ALTER TABLE `act_re_model` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_re_model` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_re_procdef`
--

DROP TABLE IF EXISTS `act_re_procdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_re_procdef` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8_bin NOT NULL,
  `VERSION_` int NOT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  `ENGINE_VERSION_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DERIVED_VERSION_` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_re_procdef`
--

LOCK TABLES `act_re_procdef` WRITE;
/*!40000 ALTER TABLE `act_re_procdef` DISABLE KEYS */;
INSERT INTO `act_re_procdef` VALUES ('simpleApproval:1:3',1,'http://springblade.workflow','Simple Approval','simpleApproval',1,'1','D:\\workproject\\springbladeandreact\\springBlade\\blade-service\\blade-workflow\\target\\classes\\processes\\simple-approval.bpmn20.xml',NULL,NULL,0,0,1,'',NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `act_re_procdef` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_actinst`
--

DROP TABLE IF EXISTS `act_ru_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_actinst` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_ACTI_START` (`START_TIME_`),
  KEY `ACT_IDX_RU_ACTI_END` (`END_TIME_`),
  KEY `ACT_IDX_RU_ACTI_PROC` (`PROC_INST_ID_`),
  KEY `ACT_IDX_RU_ACTI_PROC_ACT` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC` (`EXECUTION_ID_`),
  KEY `ACT_IDX_RU_ACTI_EXEC_ACT` (`EXECUTION_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_RU_ACTI_TASK` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_actinst`
--

LOCK TABLES `act_ru_actinst` WRITE;
/*!40000 ALTER TABLE `act_ru_actinst` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_actinst` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_deadletter_job`
--

DROP TABLE IF EXISTS `act_ru_deadletter_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_deadletter_job` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_DJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_DJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_DEADLETTER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_DEADLETTER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_deadletter_job`
--

LOCK TABLES `act_ru_deadletter_job` WRITE;
/*!40000 ALTER TABLE `act_ru_deadletter_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_deadletter_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_entitylink`
--

DROP TABLE IF EXISTS `act_ru_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_entitylink` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LINK_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`),
  KEY `ACT_IDX_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_entitylink`
--

LOCK TABLES `act_ru_entitylink` WRITE;
/*!40000 ALTER TABLE `act_ru_entitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_entitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_event_subscr`
--

DROP TABLE IF EXISTS `act_ru_event_subscr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_event_subscr` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EVENT_TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `EVENT_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACTIVITY_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CONFIGURATION_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CREATED_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`),
  KEY `ACT_IDX_EVENT_SUBSCR_EXEC_ID` (`EXECUTION_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_PROC_ID` (`PROC_INST_ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_SCOPEREF_` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EVENT_EXEC` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_event_subscr`
--

LOCK TABLES `act_ru_event_subscr` WRITE;
/*!40000 ALTER TABLE `act_ru_event_subscr` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_event_subscr` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_execution`
--

DROP TABLE IF EXISTS `act_ru_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_execution` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PARENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SUPER_EXEC_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ROOT_PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `IS_ACTIVE_` tinyint DEFAULT NULL,
  `IS_CONCURRENT_` tinyint DEFAULT NULL,
  `IS_SCOPE_` tinyint DEFAULT NULL,
  `IS_EVENT_SCOPE_` tinyint DEFAULT NULL,
  `IS_MI_ROOT_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `CACHED_ENT_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `EVT_SUBSCR_COUNT_` int DEFAULT NULL,
  `TASK_COUNT_` int DEFAULT NULL,
  `JOB_COUNT_` int DEFAULT NULL,
  `TIMER_JOB_COUNT_` int DEFAULT NULL,
  `SUSP_JOB_COUNT_` int DEFAULT NULL,
  `DEADLETTER_JOB_COUNT_` int DEFAULT NULL,
  `EXTERNAL_WORKER_JOB_COUNT_` int DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_IDC_EXEC_ROOT` (`ROOT_PROC_INST_ID_`),
  KEY `ACT_IDX_EXEC_REF_ID_` (`REFERENCE_ID_`),
  KEY `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_EXE_PARENT` (`PARENT_ID_`),
  KEY `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`),
  KEY `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_EXE_PARENT` FOREIGN KEY (`PARENT_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE,
  CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_EXE_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `ACT_FK_EXE_SUPER` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_execution`
--

LOCK TABLES `act_ru_execution` WRITE;
/*!40000 ALTER TABLE `act_ru_execution` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_execution` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_external_job`
--

DROP TABLE IF EXISTS `act_ru_external_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_external_job` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_EJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_EJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_external_job`
--

LOCK TABLES `act_ru_external_job` WRITE;
/*!40000 ALTER TABLE `act_ru_external_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_external_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_history_job`
--

DROP TABLE IF EXISTS `act_ru_history_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_history_job` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ADV_HANDLER_CFG_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_history_job`
--

LOCK TABLES `act_ru_history_job` WRITE;
/*!40000 ALTER TABLE `act_ru_history_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_history_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_identitylink`
--

DROP TABLE IF EXISTS `act_ru_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_identitylink` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_TSKASS_TASK` (`TASK_ID_`),
  KEY `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_IDL_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_TSKASS_TASK` FOREIGN KEY (`TASK_ID_`) REFERENCES `act_ru_task` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_identitylink`
--

LOCK TABLES `act_ru_identitylink` WRITE;
/*!40000 ALTER TABLE `act_ru_identitylink` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_identitylink` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_job`
--

DROP TABLE IF EXISTS `act_ru_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_job` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_JOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_JOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_job`
--

LOCK TABLES `act_ru_job` WRITE;
/*!40000 ALTER TABLE `act_ru_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_suspended_job`
--

DROP TABLE IF EXISTS `act_ru_suspended_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_suspended_job` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_SJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_SJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_SUSPENDED_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_SUSPENDED_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_suspended_job`
--

LOCK TABLES `act_ru_suspended_job` WRITE;
/*!40000 ALTER TABLE `act_ru_suspended_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_suspended_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_task`
--

DROP TABLE IF EXISTS `act_ru_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_task` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `STATE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DELEGATION_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IN_PROGRESS_TIME_` datetime(3) DEFAULT NULL,
  `IN_PROGRESS_STARTED_BY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `CLAIMED_BY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUSPENDED_TIME_` datetime(3) DEFAULT NULL,
  `SUSPENDED_BY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `IN_PROGRESS_DUE_DATE_` datetime(3) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  `FORM_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `SUB_TASK_COUNT_` int DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`),
  KEY `ACT_IDX_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TASK_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TASK_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_TASK_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_task`
--

LOCK TABLES `act_ru_task` WRITE;
/*!40000 ALTER TABLE `act_ru_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_timer_job`
--

DROP TABLE IF EXISTS `act_ru_timer_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_timer_job` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`),
  KEY `ACT_IDX_TIMER_JOB_CORRELATION_ID` (`CORRELATION_ID_`),
  KEY `ACT_IDX_TIMER_JOB_DUEDATE` (`DUEDATE_`),
  KEY `ACT_IDX_TJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_TJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_TIMER_JOB_EXECUTION` (`EXECUTION_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`),
  KEY `ACT_FK_TIMER_JOB_PROC_DEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`),
  CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_timer_job`
--

LOCK TABLES `act_ru_timer_job` WRITE;
/*!40000 ALTER TABLE `act_ru_timer_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_timer_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `act_ru_variable`
--

DROP TABLE IF EXISTS `act_ru_variable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `act_ru_variable` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_IDX_RU_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`),
  KEY `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`),
  KEY `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`),
  KEY `ACT_FK_VAR_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_FK_VAR_BYTEARRAY` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`),
  CONSTRAINT `ACT_FK_VAR_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `act_ru_variable`
--

LOCK TABLES `act_ru_variable` WRITE;
/*!40000 ALTER TABLE `act_ru_variable` DISABLE KEYS */;
/*!40000 ALTER TABLE `act_ru_variable` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_channel_definition`
--

DROP TABLE IF EXISTS `flw_channel_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_channel_definition` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `IMPLEMENTATION_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_CHANNEL_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_channel_definition`
--

LOCK TABLES `flw_channel_definition` WRITE;
/*!40000 ALTER TABLE `flw_channel_definition` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_channel_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_event_definition`
--

DROP TABLE IF EXISTS `flw_event_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_definition` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_IDX_EVENT_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_event_definition`
--

LOCK TABLES `flw_event_definition` WRITE;
/*!40000 ALTER TABLE `flw_event_definition` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_event_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_event_deployment`
--

DROP TABLE IF EXISTS `flw_event_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_deployment` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_event_deployment`
--

LOCK TABLES `flw_event_deployment` WRITE;
/*!40000 ALTER TABLE `flw_event_deployment` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_event_deployment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flw_event_resource`
--

DROP TABLE IF EXISTS `flw_event_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flw_event_resource` (
  `ID_` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOURCE_BYTES_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flw_event_resource`
--

LOCK TABLES `flw_event_resource` WRITE;
/*!40000 ALTER TABLE `flw_event_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `flw_event_resource` ENABLE KEYS */;
UNLOCK TABLES;

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
-- Table structure for table `wf_custom_action`
--

DROP TABLE IF EXISTS `wf_custom_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_custom_action` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `action_name` varchar(200) NOT NULL COMMENT '接口动作名称',
  `action_key` varchar(100) NOT NULL COMMENT '接口动作标识（唯一）',
  `class_name` varchar(300) NOT NULL COMMENT '接口动作类文件（类全名）',
  `params_json` json DEFAULT NULL COMMENT '参数设置：[{name,value,isDataSource}]',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_action_key` (`action_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自定义接口动作（注册自定义接口）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_custom_action`
--

LOCK TABLES `wf_custom_action` WRITE;
/*!40000 ALTER TABLE `wf_custom_action` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_custom_action` ENABLE KEYS */;
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
  `scope` varchar(32) NOT NULL COMMENT 'main | dt{idx} | dt{idx}_r{row}（与 data-excelp-scope 对齐）',
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
  `via_gateway` tinyint NOT NULL DEFAULT '0' COMMENT '是否经由网关折叠而来的逻辑连线 1=是',
  PRIMARY KEY (`id`),
  KEY `idx_def_from` (`def_id`,`from_node_key`),
  KEY `idx_def_reject` (`def_id`,`is_reject`)
) ENGINE=InnoDB AUTO_INCREMENT=2100088764643713026 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程出口（连线）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_node_link`
--

LOCK TABLES `wf_node_link` WRITE;
/*!40000 ALTER TABLE `wf_node_link` DISABLE KEYS */;
INSERT INTO `wf_node_link` VALUES (2099477068447293442,2099475845245640706,'StartEvent_mu18738g0','UserTask_mu18738m0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-14 20:35:14',NULL,'2026-09-14 20:35:25',1,1,0),(2099477068447293443,2099475845245640706,'UserTask_mu18738m0','EndEvent_mu18738u0',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-14 20:35:14',NULL,'2026-09-14 20:35:25',1,1,0),(2099477117248020482,2099475845245640706,'StartEvent_mu18738g0','EndEvent_mu18738u0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-14 20:35:25',NULL,'2026-09-15 08:28:28',1,1,0),(2099656559220019203,2099475845245640706,'StartEvent_mu18738g0','UserTask_mu1xob7r0',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-15 08:28:28',NULL,'2026-09-15 08:28:28',1,0,0),(2099656559220019204,2099475845245640706,'UserTask_mu1xob7r0','EndEvent_mu18738u0',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-15 08:28:28',NULL,'2026-09-15 10:35:46',1,1,0),(2099687772060176386,2099475845245640706,'UserTask_mu1xob7r0','Activity_1dw3jwq',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-15 10:32:29',NULL,'2026-09-15 10:33:25',1,1,0),(2099687772060176387,2099475845245640706,'Activity_1dw3jwq','EndEvent_mu18738u0',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 10:32:29',NULL,'2026-09-15 10:33:25',1,1,0),(2099688954895847427,2099475845245640706,'Activity_1m8w40y','EndEvent_mu18738u0',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 10:37:11',NULL,'2026-09-15 10:37:11',1,0,0),(2099688981668089859,2099475845245640706,'Activity_06no0fv','EndEvent_mu18738u0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 10:37:18',NULL,'2026-09-15 10:37:18',1,0,0),(2099757522001948673,2099475845245640706,'UserTask_mu1xob7r0','Activity_1m8w40y',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-15 15:09:39',NULL,'2026-09-15 15:09:39',1,0,0),(2099758002102956034,2099475894742622209,'UserTask_mu2c2p880','EndEvent_mu2c2p8c0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758002102956035,2099475894742622209,'StartEvent_mu2c2p840','UserTask_mu2c2p880',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758024894803973,2099758024894803969,'UserTask_mu2c2p880','EndEvent_mu2c2p8c0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758024961912833,2099758024894803969,'StartEvent_mu2c2p840','UserTask_mu2c2p880',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758062752591877,2099758062693871618,'UserTask_mu2c2p880','EndEvent_mu2c2p8c0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:56:47',1,1,0),(2099758062752591878,2099758062693871618,'StartEvent_mu2c2p840','UserTask_mu2c2p880',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-16 08:37:29',1,1,0),(2099769199200137218,2099758062693871618,'UserTask_mu2c2p880','Activity_0qm5tc6',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-15 15:56:03',NULL,'2026-09-16 08:37:29',1,1,1),(2099769329089343490,2099758062693871618,'UserTask_mu2c2p880','Activity_1mzuxt6',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 15:56:34',NULL,'2026-09-16 08:37:29',1,1,1),(2099769398794481666,2099758062693871618,'Activity_0qm5tc6','EndEvent_mu2c2p8c0',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-15 15:56:51',NULL,'2026-09-15 15:56:51',1,0,0),(2099769425491226626,2099758062693871618,'Activity_1mzuxt6','EndEvent_mu2c2p8c0',0,0,NULL,'',9,'000000',NULL,NULL,'2026-09-15 15:56:57',NULL,'2026-09-15 15:56:57',1,0,0),(2099816461460652034,2099758062693871618,'UserTask_mu2c2p880','EndEvent_mu2c2p8c0',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 19:03:51',NULL,'2026-09-15 19:05:54',1,1,1),(2099816958225629187,2099758062693871618,'UserTask_mu2c2p880','Activity_02klewn',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-15 19:05:50',NULL,'2026-09-15 20:13:18',1,1,1),(2099816958225629188,2099758062693871618,'Activity_02klewn','EndEvent_mu2c2p8c0',0,0,NULL,'',8,'000000',NULL,NULL,'2026-09-15 19:05:50',NULL,'2026-09-15 19:05:50',1,0,0),(2099816975044788227,2099758062693871618,'Activity_1g4p700','EndEvent_mu2c2p8c0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 19:05:54',NULL,'2026-09-15 19:05:58',1,1,1),(2099816975044788228,2099758062693871618,'UserTask_mu2c2p880','Activity_1g4p700',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 19:05:54',NULL,'2026-09-15 20:13:18',1,1,1),(2099816992086245378,2099758062693871618,'Activity_1g4p700','Activity_0ylt45t',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 19:05:58',NULL,'2026-09-15 20:16:23',1,1,0),(2099816992086245379,2099758062693871618,'Activity_0ylt45t','EndEvent_mu2c2p8c0',0,0,NULL,'',11,'000000',NULL,NULL,'2026-09-15 19:05:58',NULL,'2026-09-15 19:05:58',1,0,0),(2099834008419516418,2099758062693871618,'UserTask_mu2c2p880','Activity_1g4p700',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 20:13:35',NULL,'2026-09-15 20:13:55',1,1,1),(2099834008419516419,2099758062693871618,'UserTask_mu2c2p880','Activity_02klewn',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 20:13:35',NULL,'2026-09-16 08:27:19',1,1,1),(2099834144151388161,2099758062693871618,'UserTask_mu2c2p880','Activity_1g4p700',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-15 20:14:07',NULL,'2026-09-16 08:27:19',1,1,1),(2099834773615755265,2099758062693871618,'Activity_1g4p700','Activity_0ylt45t',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 20:16:37',NULL,'2026-09-16 08:27:19',1,1,1),(2100018659545096196,2099758062693871618,'Activity_1g4p700','Event_0pivae0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 13:05:53',1,1,0),(2100018659545096197,2099758062693871618,'UserTask_mu2c2p880','Event_093p7s3',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 08:37:29',1,1,0),(2100018659595427842,2099758062693871618,'Event_093p7s3','Activity_02klewn',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 08:27:19',1,0,1),(2100018659595427843,2099758062693871618,'Event_093p7s3','Event_0ctoqxr',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 08:27:19',1,0,1),(2100018659595427844,2099758062693871618,'Event_0ctoqxr','Activity_1g4p700',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 13:05:53',1,1,0),(2100018659595427845,2099758062693871618,'Event_0pivae0','Activity_0ylt45t',0,0,NULL,'',10,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 08:27:19',1,0,0),(2100021218242502657,2099758062693871618,'StartEvent_mu2c2p840','Event_093p7s3',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-16 08:37:29',NULL,'2026-09-16 08:37:42',1,1,1),(2100021218242502658,2099758062693871618,'StartEvent_mu2c2p840','Activity_0qm5tc6',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-16 08:37:29',NULL,'2026-09-16 08:37:42',1,1,1),(2100021218242502659,2099758062693871618,'StartEvent_mu2c2p840','Activity_1mzuxt6',0,0,NULL,'',6,'000000',NULL,NULL,'2026-09-16 08:37:29',NULL,'2026-09-16 08:37:42',1,1,1),(2100021271568883714,2099758062693871618,'UserTask_mu2c2p880','Event_093p7s3',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-16 08:37:42',NULL,'2026-09-16 08:40:19',1,1,0),(2100021271568883715,2099758062693871618,'UserTask_mu2c2p880','Activity_0qm5tc6',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-16 08:37:42',NULL,'2026-09-16 08:40:19',1,1,1),(2100021271568883716,2099758062693871618,'UserTask_mu2c2p880','Activity_1mzuxt6',0,0,NULL,'',6,'000000',NULL,NULL,'2026-09-16 08:37:42',NULL,'2026-09-16 08:40:19',1,1,1),(2100021271568883717,2099758062693871618,'StartEvent_mu2c2p840','UserTask_mu2c2p880',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-16 08:37:42',NULL,'2026-09-16 08:40:19',1,1,0),(2100021930926055426,2099758062693871618,'StartEvent_mu2c2p840','Event_093p7s3',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-16 08:40:19',NULL,'2026-09-16 08:40:19',1,0,1),(2100021930926055427,2099758062693871618,'StartEvent_mu2c2p840','Activity_0qm5tc6',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-16 08:40:19',NULL,'2026-09-16 08:40:19',1,0,1),(2100021930926055428,2099758062693871618,'StartEvent_mu2c2p840','Activity_1mzuxt6',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-16 08:40:19',NULL,'2026-09-16 08:40:19',1,0,1),(2100085625609822211,2099758062693871618,'StartEvent_mu2c2p840','Event_1fty5wz',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-16 12:53:25',NULL,'2026-09-16 12:53:30',1,1,1),(2100088764643713025,2099758062693871618,'Event_0ctoqxr','Event_0pivae0',0,0,NULL,'',6,'000000',NULL,NULL,'2026-09-16 13:05:53',NULL,'2026-09-16 13:05:53',1,0,1);
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
) ENGINE=InnoDB AUTO_INCREMENT=2100081416176967682 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='节点操作者';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_node_operator`
--

LOCK TABLES `wf_node_operator` WRITE;
/*!40000 ALTER TABLE `wf_node_operator` DISABLE KEYS */;
INSERT INTO `wf_node_operator` VALUES (2099761448268951554,2099758062752591875,1,3,'2036826812715196418,2042170824913412098',0,100,0,0,0,'{\"name\": \"111\"}','000000',NULL,NULL,'2026-09-15 15:25:15',NULL,'2026-09-16 08:37:29',1,1),(2099769766802714625,2099769199200137217,1,1,'1123598813738675202',0,100,0,0,0,'{\"name\": \"2\"}','000000',NULL,NULL,'2026-09-15 15:58:18',NULL,'2026-09-15 15:58:18',1,0),(2099769837103443969,2099769329089343489,1,2,'1123598816738675202,1123598816738675203',0,100,0,0,0,'{\"name\": \"3\"}','000000',NULL,NULL,'2026-09-15 15:58:35',NULL,'2026-09-15 15:58:35',1,0),(2099818846874583042,2099816958225629186,1,3,'2019979771992432642,2044030517948477441',0,100,0,2,0,'{\"name\": \"6\"}','000000',NULL,NULL,'2026-09-15 19:13:20',NULL,'2026-09-15 19:13:20',1,0),(2099818928986472449,2099816975044788226,1,1,'1123598813738675202,1123598813738675203',0,100,0,2,0,'{\"name\": \"4\"}','000000',NULL,NULL,'2026-09-15 19:13:40',NULL,'2026-09-16 13:05:53',1,1),(2099818972590456834,2099816992086245377,1,4,'',0,100,0,2,0,'{\"name\": \"5\"}','000000',NULL,NULL,'2026-09-15 19:13:50',NULL,'2026-09-15 20:01:20',1,1),(2100081416176967681,2099816992086245377,1,1,'1123598813738675202,1123598813738675203',0,100,0,0,0,'{\"name\": \"5\"}','000000',NULL,NULL,'2026-09-16 12:36:41',NULL,'2026-09-16 12:36:41',1,0);
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
  `bpmn_xml` mediumtext COMMENT 'BPMN 2.0 流程定义 XML（bpmn-js 画布产出，部署时下发引擎）',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `active_version_id` bigint unsigned DEFAULT NULL COMMENT '版本组锚点：指向当前激活版本的 defId；首版=自身id，NULL=单版本流程（组=自身）',
  `is_free` tinyint NOT NULL DEFAULT '0' COMMENT '是否自由流程',
  `free_wf_type` tinyint DEFAULT NULL COMMENT '自由流程类型：1简易 2高级',
  `type` varchar(64) DEFAULT NULL COMMENT '路径类型（对齐 ecology path_type 字典 code）',
  `form_type` tinyint DEFAULT NULL COMMENT '对应表单类型：0自定义表单 1系统表单',
  `description` varchar(500) DEFAULT NULL COMMENT '路径描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
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
  KEY `idx_form_status` (`form_id`,`status`),
  KEY `idx_active_version` (`active_version_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2099758062693871619 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_process_definition`
--

LOCK TABLES `wf_process_definition` WRITE;
/*!40000 ALTER TABLE `wf_process_definition` DISABLE KEYS */;
INSERT INTO `wf_process_definition` VALUES (2099475845245640706,'flow_mu180u86md3e',2064530495200337922,'测试914','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"flow_mu180u86md3e\" name=\"测试914\" isExecutable=\"true\">\n    <bpmn:startEvent id=\"StartEvent_mu18738g0\" name=\"开始\">\n      <bpmn:outgoing>Flow_0yenwcd</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:endEvent id=\"EndEvent_mu18738u0\" name=\"结束\">\n      <bpmn:incoming>Flow_1gxy9sv</bpmn:incoming>\n      <bpmn:incoming>Flow_1prutey</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:userTask id=\"UserTask_mu1xob7r0\" name=\"test\">\n      <bpmn:incoming>Flow_0yenwcd</bpmn:incoming>\n      <bpmn:outgoing>Flow_15soamo</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_0yenwcd\" sourceRef=\"StartEvent_mu18738g0\" targetRef=\"UserTask_mu1xob7r0\" />\n    <bpmn:sequenceFlow id=\"Flow_1gxy9sv\" sourceRef=\"Activity_06no0fv\" targetRef=\"EndEvent_mu18738u0\" />\n    <bpmn:sequenceFlow id=\"Flow_1prutey\" sourceRef=\"Activity_1m8w40y\" targetRef=\"EndEvent_mu18738u0\" />\n    <bpmn:userTask id=\"Activity_1m8w40y\" name=\"test2\">\n      <bpmn:incoming>Flow_15soamo</bpmn:incoming>\n      <bpmn:outgoing>Flow_1prutey</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:userTask id=\"Activity_06no0fv\" name=\"test1\">\n      <bpmn:outgoing>Flow_1gxy9sv</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_15soamo\" sourceRef=\"UserTask_mu1xob7r0\" targetRef=\"Activity_1m8w40y\" />\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"flow_mu180u86md3e\">\n      <bpmndi:BPMNShape id=\"StartEvent_mu18738g0_di\" bpmnElement=\"StartEvent_mu18738g0\">\n        <dc:Bounds x=\"142\" y=\"142\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"149\" y=\"185\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"EndEvent_mu18738u0_di\" bpmnElement=\"EndEvent_mu18738u0\">\n        <dc:Bounds x=\"852\" y=\"272\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"859\" y=\"248\" width=\"22\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"UserTask_mu1xob7r0_di\" bpmnElement=\"UserTask_mu1xob7r0\">\n        <dc:Bounds x=\"248\" y=\"102\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1bevfca_di\" bpmnElement=\"Activity_1m8w40y\">\n        <dc:Bounds x=\"460\" y=\"360\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1yvz5ju_di\" bpmnElement=\"Activity_06no0fv\">\n        <dc:Bounds x=\"590\" y=\"60\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_0yenwcd_di\" bpmnElement=\"Flow_0yenwcd\">\n        <di:waypoint x=\"178\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"142\" />\n        <di:waypoint x=\"248\" y=\"142\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1gxy9sv_di\" bpmnElement=\"Flow_1gxy9sv\">\n        <di:waypoint x=\"690\" y=\"100\" />\n        <di:waypoint x=\"771\" y=\"100\" />\n        <di:waypoint x=\"771\" y=\"290\" />\n        <di:waypoint x=\"852\" y=\"290\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1prutey_di\" bpmnElement=\"Flow_1prutey\">\n        <di:waypoint x=\"560\" y=\"400\" />\n        <di:waypoint x=\"706\" y=\"400\" />\n        <di:waypoint x=\"706\" y=\"290\" />\n        <di:waypoint x=\"852\" y=\"290\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_15soamo_di\" bpmnElement=\"Flow_15soamo\">\n        <di:waypoint x=\"348\" y=\"142\" />\n        <di:waypoint x=\"404\" y=\"142\" />\n        <di:waypoint x=\"404\" y=\"400\" />\n        <di:waypoint x=\"460\" y=\"400\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n',1,2099758062693871618,0,NULL,'2',0,NULL,0,'000000',NULL,NULL,'2026-09-14 20:30:22',NULL,'2026-09-14 20:30:22',0,0),(2099475894742622209,'flow_mu180u86md3e',2064530495200337922,'测试914','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"flow_mu180u86md3e\" name=\"测试914\" isExecutable=\"true\">\n    <bpmn:startEvent id=\"StartEvent_mu2c2p840\" name=\"开始\">\n      <bpmn:outgoing>Flow_1g1p7by</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:userTask id=\"UserTask_mu2c2p880\" name=\"1231\">\n      <bpmn:incoming>Flow_1g1p7by</bpmn:incoming>\n      <bpmn:outgoing>Flow_11qj1tr</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\" />\n    <bpmn:endEvent id=\"EndEvent_mu2c2p8c0\" name=\"结束\">\n      <bpmn:incoming>Flow_11qj1tr</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:sequenceFlow id=\"Flow_11qj1tr\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"EndEvent_mu2c2p8c0\" />\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"flow_mu180u86md3e\">\n      <bpmndi:BPMNShape id=\"StartEvent_mu2c2p840_di\" bpmnElement=\"StartEvent_mu2c2p840\">\n        <dc:Bounds x=\"142\" y=\"142\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"149\" y=\"185\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"UserTask_mu2c2p880_di\" bpmnElement=\"UserTask_mu2c2p880\">\n        <dc:Bounds x=\"248\" y=\"102\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"EndEvent_mu2c2p8c0_di\" bpmnElement=\"EndEvent_mu2c2p8c0\">\n        <dc:Bounds x=\"450\" y=\"84\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"457\" y=\"127\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_1g1p7by_di\" bpmnElement=\"Flow_1g1p7by\">\n        <di:waypoint x=\"178\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"142\" />\n        <di:waypoint x=\"248\" y=\"142\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_11qj1tr_di\" bpmnElement=\"Flow_11qj1tr\">\n        <di:waypoint x=\"348\" y=\"142\" />\n        <di:waypoint x=\"400\" y=\"142\" />\n        <di:waypoint x=\"400\" y=\"102\" />\n        <di:waypoint x=\"450\" y=\"102\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n',2,2099758062693871618,0,NULL,'2',0,NULL,0,'000000',NULL,NULL,'2026-09-14 20:30:34',NULL,'2026-09-14 20:30:34',0,0),(2099758024894803969,'flow_mu180u86md3e',2064530495200337922,'测试914','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"flow_mu180u86md3e\" name=\"测试914\" isExecutable=\"true\">\n    <bpmn:startEvent id=\"StartEvent_mu2c2p840\" name=\"开始\">\n      <bpmn:outgoing>Flow_1g1p7by</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:userTask id=\"UserTask_mu2c2p880\" name=\"1231\">\n      <bpmn:incoming>Flow_1g1p7by</bpmn:incoming>\n      <bpmn:outgoing>Flow_11qj1tr</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\" />\n    <bpmn:endEvent id=\"EndEvent_mu2c2p8c0\" name=\"结束\">\n      <bpmn:incoming>Flow_11qj1tr</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:sequenceFlow id=\"Flow_11qj1tr\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"EndEvent_mu2c2p8c0\" />\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"flow_mu180u86md3e\">\n      <bpmndi:BPMNShape id=\"StartEvent_mu2c2p840_di\" bpmnElement=\"StartEvent_mu2c2p840\">\n        <dc:Bounds x=\"142\" y=\"142\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"149\" y=\"185\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"UserTask_mu2c2p880_di\" bpmnElement=\"UserTask_mu2c2p880\">\n        <dc:Bounds x=\"248\" y=\"102\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"EndEvent_mu2c2p8c0_di\" bpmnElement=\"EndEvent_mu2c2p8c0\">\n        <dc:Bounds x=\"450\" y=\"84\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"457\" y=\"127\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_1g1p7by_di\" bpmnElement=\"Flow_1g1p7by\">\n        <di:waypoint x=\"178\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"142\" />\n        <di:waypoint x=\"248\" y=\"142\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_11qj1tr_di\" bpmnElement=\"Flow_11qj1tr\">\n        <di:waypoint x=\"348\" y=\"142\" />\n        <di:waypoint x=\"400\" y=\"142\" />\n        <di:waypoint x=\"400\" y=\"102\" />\n        <di:waypoint x=\"450\" y=\"102\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n',3,2099758062693871618,0,NULL,'2',0,NULL,0,'000000',NULL,NULL,'2026-09-15 15:11:39',NULL,'2026-09-15 15:11:39',0,0),(2099758062693871618,'flow_mu180u86md3e',2064530495200337922,'测试914','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"flow_mu180u86md3e\" name=\"测试914\" isExecutable=\"true\">\n    <bpmn:extensionElements>\n      <camunda:executionListener class=\"\" event=\"start\" />\n    </bpmn:extensionElements>\n    <bpmn:startEvent id=\"StartEvent_mu2c2p840\" name=\"开始\">\n      <bpmn:outgoing>Flow_1g1p7by</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\" />\n    <bpmn:endEvent id=\"EndEvent_mu2c2p8c0\" name=\"结束\">\n      <bpmn:extensionElements>\n        <camunda:properties>\n          <camunda:property />\n        </camunda:properties>\n      </bpmn:extensionElements>\n      <bpmn:incoming>Flow_1aqie0c</bpmn:incoming>\n      <bpmn:incoming>Flow_016it02</bpmn:incoming>\n      <bpmn:incoming>Flow_1kkbmin</bpmn:incoming>\n      <bpmn:incoming>Flow_0t3saos</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:sequenceFlow id=\"Flow_1qxvbp7\" name=\"xiaoyuyibai\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_15cdzo6\">\n      <bpmn:extensionElements>\n        <camunda:properties>\n          <camunda:property />\n        </camunda:properties>\n      </bpmn:extensionElements>\n      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\" />\n    </bpmn:sequenceFlow>\n    <bpmn:sequenceFlow id=\"Flow_1dpq6dk\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_0qm5tc6\" />\n    <bpmn:userTask id=\"Activity_0qm5tc6\" name=\"1\">\n      <bpmn:incoming>Flow_1dpq6dk</bpmn:incoming>\n      <bpmn:outgoing>Flow_1aqie0c</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_0icqanz\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_1mzuxt6\" />\n    <bpmn:parallelGateway id=\"Gateway_15cdzo6\">\n      <bpmn:incoming>Flow_1qxvbp7</bpmn:incoming>\n      <bpmn:outgoing>Flow_1dpq6dk</bpmn:outgoing>\n      <bpmn:outgoing>Flow_0icqanz</bpmn:outgoing>\n    </bpmn:parallelGateway>\n    <bpmn:userTask id=\"Activity_1mzuxt6\" name=\"2\">\n      <bpmn:incoming>Flow_0icqanz</bpmn:incoming>\n      <bpmn:outgoing>Flow_016it02</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_1aqie0c\" sourceRef=\"Activity_0qm5tc6\" targetRef=\"EndEvent_mu2c2p8c0\" />\n    <bpmn:sequenceFlow id=\"Flow_016it02\" sourceRef=\"Activity_1mzuxt6\" targetRef=\"EndEvent_mu2c2p8c0\" />\n    <bpmn:exclusiveGateway id=\"Gateway_0a6n8eh\">\n      <bpmn:incoming>Flow_0t5hbag</bpmn:incoming>\n      <bpmn:outgoing>Flow_13gvmcu</bpmn:outgoing>\n      <bpmn:outgoing>Flow_0e4xs2a</bpmn:outgoing>\n    </bpmn:exclusiveGateway>\n    <bpmn:sequenceFlow id=\"Flow_13gvmcu\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_02klewn\" />\n    <bpmn:sequenceFlow id=\"Flow_1kkbmin\" sourceRef=\"Activity_02klewn\" targetRef=\"EndEvent_mu2c2p8c0\" />\n    <bpmn:sequenceFlow id=\"Flow_0t3saos\" sourceRef=\"Activity_0ylt45t\" targetRef=\"EndEvent_mu2c2p8c0\" />\n    <bpmn:userTask id=\"Activity_02klewn\" name=\"6\">\n      <bpmn:incoming>Flow_13gvmcu</bpmn:incoming>\n      <bpmn:outgoing>Flow_1kkbmin</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:userTask id=\"Activity_0ylt45t\" name=\"5\">\n      <bpmn:incoming>Flow_04ilbrs</bpmn:incoming>\n      <bpmn:outgoing>Flow_0t3saos</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_0dizg8q\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Event_093p7s3\" />\n    <bpmn:intermediateCatchEvent id=\"Event_093p7s3\">\n      <bpmn:incoming>Flow_0dizg8q</bpmn:incoming>\n      <bpmn:outgoing>Flow_0t5hbag</bpmn:outgoing>\n      <bpmn:timerEventDefinition id=\"TimerEventDefinition_0cmm2hq\" />\n    </bpmn:intermediateCatchEvent>\n    <bpmn:sequenceFlow id=\"Flow_0t5hbag\" sourceRef=\"Event_093p7s3\" targetRef=\"Gateway_0a6n8eh\" />\n    <bpmn:sequenceFlow id=\"Flow_0e4xs2a\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Event_0ctoqxr\" />\n    <bpmn:intermediateCatchEvent id=\"Event_0ctoqxr\">\n      <bpmn:incoming>Flow_0e4xs2a</bpmn:incoming>\n      <bpmn:outgoing>Flow_19aofay</bpmn:outgoing>\n      <bpmn:timerEventDefinition id=\"TimerEventDefinition_1pyxikw\">\n        <bpmn:timeDate xsi:type=\"bpmn:tFormalExpression\" />\n      </bpmn:timerEventDefinition>\n    </bpmn:intermediateCatchEvent>\n    <bpmn:sequenceFlow id=\"Flow_19aofay\" sourceRef=\"Event_0ctoqxr\" targetRef=\"Activity_1g4p700\" />\n    <bpmn:sequenceFlow id=\"Flow_04zmffr\" sourceRef=\"Activity_1g4p700\" targetRef=\"Event_0pivae0\" />\n    <bpmn:intermediateThrowEvent id=\"Event_0pivae0\">\n      <bpmn:incoming>Flow_04zmffr</bpmn:incoming>\n      <bpmn:outgoing>Flow_04ilbrs</bpmn:outgoing>\n      <bpmn:messageEventDefinition id=\"MessageEventDefinition_02ydbd1\" camunda:class=\"\" />\n    </bpmn:intermediateThrowEvent>\n    <bpmn:sequenceFlow id=\"Flow_04ilbrs\" sourceRef=\"Event_0pivae0\" targetRef=\"Activity_0ylt45t\" />\n    <bpmn:businessRuleTask id=\"UserTask_mu2c2p880\" name=\"3\">\n      <bpmn:incoming>Flow_1g1p7by</bpmn:incoming>\n      <bpmn:outgoing>Flow_1qxvbp7</bpmn:outgoing>\n      <bpmn:outgoing>Flow_0dizg8q</bpmn:outgoing>\n    </bpmn:businessRuleTask>\n    <bpmn:subProcess id=\"Activity_1g4p700\" name=\"4\">\n      <bpmn:extensionElements>\n        <camunda:properties>\n          <camunda:property />\n        </camunda:properties>\n      </bpmn:extensionElements>\n      <bpmn:incoming>Flow_19aofay</bpmn:incoming>\n      <bpmn:outgoing>Flow_04zmffr</bpmn:outgoing>\n    </bpmn:subProcess>\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"flow_mu180u86md3e\">\n      <bpmndi:BPMNShape id=\"StartEvent_mu2c2p840_di\" bpmnElement=\"StartEvent_mu2c2p840\">\n        <dc:Bounds x=\"142\" y=\"142\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"149\" y=\"185\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"EndEvent_mu2c2p8c0_di\" bpmnElement=\"EndEvent_mu2c2p8c0\">\n        <dc:Bounds x=\"812\" y=\"312\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"857.5\" y=\"323\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_10ime3q_di\" bpmnElement=\"Activity_0qm5tc6\">\n        <dc:Bounds x=\"780\" y=\"90\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Gateway_1u6p3ea_di\" bpmnElement=\"Gateway_15cdzo6\">\n        <dc:Bounds x=\"545\" y=\"75\" width=\"50\" height=\"50\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_0iy2z00_di\" bpmnElement=\"Activity_1mzuxt6\">\n        <dc:Bounds x=\"631\" y=\"170\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Gateway_0pefr9g_di\" bpmnElement=\"Gateway_0a6n8eh\" isMarkerVisible=\"true\">\n        <dc:Bounds x=\"225\" y=\"365\" width=\"50\" height=\"50\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_12i5vwr_di\" bpmnElement=\"Activity_02klewn\">\n        <dc:Bounds x=\"200\" y=\"500\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1262tz2_di\" bpmnElement=\"Activity_0ylt45t\">\n        <dc:Bounds x=\"610\" y=\"340\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Event_14dm5hb_di\" bpmnElement=\"Event_093p7s3\">\n        <dc:Bounds x=\"242\" y=\"242\" width=\"36\" height=\"36\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Event_0at38vf_di\" bpmnElement=\"Event_0ctoqxr\">\n        <dc:Bounds x=\"302\" y=\"372\" width=\"36\" height=\"36\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Event_1w4343j_di\" bpmnElement=\"Event_0pivae0\">\n        <dc:Bounds x=\"502\" y=\"372\" width=\"36\" height=\"36\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1prbjhj_di\" bpmnElement=\"UserTask_mu2c2p880\">\n        <dc:Bounds x=\"248\" y=\"102\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1dhqof2_di\" bpmnElement=\"Activity_1g4p700\">\n        <dc:Bounds x=\"370\" y=\"350\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_1g1p7by_di\" bpmnElement=\"Flow_1g1p7by\">\n        <di:waypoint x=\"178\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"142\" />\n        <di:waypoint x=\"248\" y=\"142\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1qxvbp7_di\" bpmnElement=\"Flow_1qxvbp7\">\n        <di:waypoint x=\"348\" y=\"134\" />\n        <di:waypoint x=\"548\" y=\"103\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"404\" y=\"130\" width=\"54\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1dpq6dk_di\" bpmnElement=\"Flow_1dpq6dk\">\n        <di:waypoint x=\"595\" y=\"100\" />\n        <di:waypoint x=\"688\" y=\"100\" />\n        <di:waypoint x=\"688\" y=\"130\" />\n        <di:waypoint x=\"780\" y=\"130\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0icqanz_di\" bpmnElement=\"Flow_0icqanz\">\n        <di:waypoint x=\"570\" y=\"125\" />\n        <di:waypoint x=\"570\" y=\"210\" />\n        <di:waypoint x=\"631\" y=\"210\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1aqie0c_di\" bpmnElement=\"Flow_1aqie0c\">\n        <di:waypoint x=\"830\" y=\"170\" />\n        <di:waypoint x=\"830\" y=\"312\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_016it02_di\" bpmnElement=\"Flow_016it02\">\n        <di:waypoint x=\"731\" y=\"210\" />\n        <di:waypoint x=\"766\" y=\"210\" />\n        <di:waypoint x=\"766\" y=\"330\" />\n        <di:waypoint x=\"812\" y=\"330\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_13gvmcu_di\" bpmnElement=\"Flow_13gvmcu\">\n        <di:waypoint x=\"250\" y=\"415\" />\n        <di:waypoint x=\"250\" y=\"500\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1kkbmin_di\" bpmnElement=\"Flow_1kkbmin\">\n        <di:waypoint x=\"300\" y=\"540\" />\n        <di:waypoint x=\"830\" y=\"540\" />\n        <di:waypoint x=\"830\" y=\"348\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0t3saos_di\" bpmnElement=\"Flow_0t3saos\">\n        <di:waypoint x=\"710\" y=\"400\" />\n        <di:waypoint x=\"761\" y=\"400\" />\n        <di:waypoint x=\"761\" y=\"330\" />\n        <di:waypoint x=\"812\" y=\"330\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0dizg8q_di\" bpmnElement=\"Flow_0dizg8q\">\n        <di:waypoint x=\"298\" y=\"182\" />\n        <di:waypoint x=\"298\" y=\"210\" />\n        <di:waypoint x=\"260\" y=\"210\" />\n        <di:waypoint x=\"260\" y=\"242\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0t5hbag_di\" bpmnElement=\"Flow_0t5hbag\">\n        <di:waypoint x=\"260\" y=\"278\" />\n        <di:waypoint x=\"260\" y=\"322\" />\n        <di:waypoint x=\"250\" y=\"322\" />\n        <di:waypoint x=\"250\" y=\"365\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0e4xs2a_di\" bpmnElement=\"Flow_0e4xs2a\">\n        <di:waypoint x=\"275\" y=\"390\" />\n        <di:waypoint x=\"302\" y=\"390\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_19aofay_di\" bpmnElement=\"Flow_19aofay\">\n        <di:waypoint x=\"338\" y=\"390\" />\n        <di:waypoint x=\"370\" y=\"390\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_04zmffr_di\" bpmnElement=\"Flow_04zmffr\">\n        <di:waypoint x=\"470\" y=\"390\" />\n        <di:waypoint x=\"502\" y=\"390\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_04ilbrs_di\" bpmnElement=\"Flow_04ilbrs\">\n        <di:waypoint x=\"538\" y=\"390\" />\n        <di:waypoint x=\"574\" y=\"390\" />\n        <di:waypoint x=\"574\" y=\"380\" />\n        <di:waypoint x=\"610\" y=\"380\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1bsf56s\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_0t2zxg5\" bpmnElement=\"Activity_1g4p700\" />\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n',4,2099758062693871618,0,NULL,'2',0,NULL,0,'000000',NULL,NULL,'2026-09-15 15:11:48',NULL,'2026-09-15 15:11:48',0,0);
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
  `test_status` tinyint NOT NULL DEFAULT '0' COMMENT '节点测试状态 0未测试 1测试通过 2测试未通过',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_def_node` (`def_id`,`node_key`),
  KEY `idx_def_sort` (`def_id`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=2100085625609822211 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程节点';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_process_node`
--

LOCK TABLES `wf_process_node` WRITE;
/*!40000 ALTER TABLE `wf_process_node` DISABLE KEYS */;
INSERT INTO `wf_process_node` VALUES (2099477068380184577,2099475845245640706,'StartEvent_mu18738g0','开始',0,0,0,0,1,0,0,1,NULL,'000000',NULL,NULL,'2026-09-14 20:35:14',NULL,'2026-09-14 20:35:14',1,0,1),(2099477068380184578,2099475845245640706,'UserTask_mu18738m0','1213',1,0,0,0,1,0,0,2,'{\"settings\": {}}','000000',NULL,NULL,'2026-09-14 20:35:14',NULL,'2026-09-14 20:35:25',1,1,0),(2099477068380184579,2099475845245640706,'EndEvent_mu18738u0','结束',3,0,0,0,1,0,0,5,NULL,'000000',NULL,NULL,'2026-09-14 20:35:14',NULL,'2026-09-14 20:35:14',1,0,1),(2099656559220019202,2099475845245640706,'UserTask_mu1xob7r0','test',1,0,0,0,1,0,0,2,'{\"settings\": {}}','000000',NULL,NULL,'2026-09-15 08:28:28',NULL,'2026-09-15 08:28:28',1,0,2),(2099687772060176385,2099475845245640706,'Activity_1dw3jwq','Activity_1dw3jwq',1,0,0,0,1,0,0,5,NULL,'000000',NULL,NULL,'2026-09-15 10:32:29',NULL,'2026-09-15 10:33:24',1,1,0),(2099688954895847426,2099475845245640706,'Activity_1m8w40y','test2',1,0,0,0,1,0,0,3,NULL,'000000',NULL,NULL,'2026-09-15 10:37:11',NULL,'2026-09-15 10:37:11',1,0,2),(2099688981668089858,2099475845245640706,'Activity_06no0fv','test1',1,0,0,0,1,0,0,4,NULL,'000000',NULL,NULL,'2026-09-15 10:37:18',NULL,'2026-09-15 10:37:18',1,0,0),(2099758002035847169,2099475894742622209,'StartEvent_mu2c2p840','开始',0,0,0,0,1,0,0,1,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758002035847170,2099475894742622209,'UserTask_mu2c2p880','1231',1,0,0,0,1,0,0,2,'{\"settings\": {}}','000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758002102956033,2099475894742622209,'EndEvent_mu2c2p8c0','结束',3,0,0,0,1,0,0,3,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758024894803970,2099758024894803969,'StartEvent_mu2c2p840','开始',0,0,0,0,1,0,0,1,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758024894803971,2099758024894803969,'UserTask_mu2c2p880','1231',1,0,0,0,1,0,0,2,'{\"settings\": {}}','000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758024894803972,2099758024894803969,'EndEvent_mu2c2p8c0','结束',3,0,0,0,1,0,0,3,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758062752591874,2099758062693871618,'StartEvent_mu2c2p840','开始',0,0,0,0,1,0,0,1,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,1),(2099758062752591875,2099758062693871618,'UserTask_mu2c2p880','3',1,0,0,0,1,0,0,12,'{\"settings\": {}}','000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-16 08:40:19',1,1,1),(2099758062752591876,2099758062693871618,'EndEvent_mu2c2p8c0','结束',3,0,0,0,1,0,0,8,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,1),(2099769199200137217,2099758062693871618,'Activity_0qm5tc6','1',1,0,0,0,1,0,0,3,NULL,'000000',NULL,NULL,'2026-09-15 15:56:03',NULL,'2026-09-15 15:56:03',1,0,1),(2099769329089343489,2099758062693871618,'Activity_1mzuxt6','2',1,0,0,0,1,0,0,4,NULL,'000000',NULL,NULL,'2026-09-15 15:56:34',NULL,'2026-09-15 15:56:34',1,0,1),(2099816958225629186,2099758062693871618,'Activity_02klewn','6',1,0,0,0,1,0,0,5,NULL,'000000',NULL,NULL,'2026-09-15 19:05:50',NULL,'2026-09-15 19:05:50',1,0,1),(2099816975044788226,2099758062693871618,'Activity_1g4p700','4',1,0,0,0,1,0,0,6,NULL,'000000',NULL,NULL,'2026-09-15 19:05:54',NULL,'2026-09-16 13:05:53',1,1,1),(2099816992086245377,2099758062693871618,'Activity_0ylt45t','5',1,0,0,0,1,0,0,7,'{\"sign\": 0, \"remind\": 0, \"settings\": {\"timeout\": {\"hours\": 0, \"remind\": false}}, \"timeoutHours\": 0}','000000',NULL,NULL,'2026-09-15 19:05:58',NULL,'2026-09-15 19:05:58',1,0,1),(2100018659545096193,2099758062693871618,'Event_093p7s3','中间事件',5,0,0,0,1,0,0,9,NULL,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 08:27:19',1,0,1),(2100018659545096194,2099758062693871618,'Event_0ctoqxr','中间事件',5,0,0,0,1,0,0,10,NULL,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 08:27:19',1,0,1),(2100018659545096195,2099758062693871618,'Event_0pivae0','中间事件',5,0,0,0,1,0,0,11,NULL,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 08:27:19',1,0,1),(2100085625609822210,2099758062693871618,'Event_1fty5wz','结束',3,0,0,0,1,0,0,12,NULL,'000000',NULL,NULL,'2026-09-16 12:53:25',NULL,'2026-09-16 12:53:30',1,1,0);
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

--
-- Table structure for table `wf_workflow_type`
--

DROP TABLE IF EXISTS `wf_workflow_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_workflow_type` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type_name` varchar(100) NOT NULL COMMENT '类型名称',
  `type_desc` varchar(500) DEFAULT NULL COMMENT '类型描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '0停用 1启用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除:1已删 0未删',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`status`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程（路径）类型（浏览框 wftype 数据源）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_workflow_type`
--

LOCK TABLES `wf_workflow_type` WRITE;
/*!40000 ALTER TABLE `wf_workflow_type` DISABLE KEYS */;
INSERT INTO `wf_workflow_type` VALUES (1,'行政审批','行政审批类流程',1,'000000',NULL,NULL,'2026-09-10 15:53:50',NULL,'2026-09-10 15:53:50',1,0),(2,'人事流程','招聘、入转调离、考勤等',2,'000000',NULL,NULL,'2026-09-10 15:53:50',NULL,'2026-09-10 15:53:50',1,0),(3,'财务流程','报销、预算、付款等',3,'000000',NULL,NULL,'2026-09-10 15:53:50',NULL,'2026-09-10 15:53:50',1,0),(4,'IT流程','系统权限、资源申请、运维',4,'000000',NULL,NULL,'2026-09-10 15:53:50',NULL,'2026-09-10 15:53:50',1,0),(5,'业务流程','各业务运营审批',5,'000000',NULL,NULL,'2026-09-10 15:53:50',NULL,'2026-09-10 15:53:50',1,0);
/*!40000 ALTER TABLE `wf_workflow_type` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-16 15:34:18
