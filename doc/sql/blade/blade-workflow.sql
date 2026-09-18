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
INSERT INTO `act_ge_bytearray` VALUES ('12502',1,'flow_mu180u86md3e.bpmn20.xml','12501',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://bpmn.io/schema/bpmn\" id=\"Definitions_1\">\n  <process id=\"flow_mu180u86md3e\" name=\"æµ‹è¯•914\" isExecutable=\"true\">\n    <extensionElements>\n      <flowable:executionListener event=\"start\"></flowable:executionListener>\n    </extensionElements>\n    <startEvent id=\"StartEvent_mu2c2p840\" name=\"å¼€å§‹\"></startEvent>\n    <sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\"></sequenceFlow>\n    <endEvent id=\"EndEvent_mu2c2p8c0\" name=\"ç»“æŸ\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </endEvent>\n    <sequenceFlow id=\"Flow_1qxvbp7\" name=\"xiaoyuyibai\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_15cdzo6\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_1dpq6dk\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_0qm5tc6\"></sequenceFlow>\n    <userTask id=\"Activity_0qm5tc6\" name=\"1\"></userTask>\n    <sequenceFlow id=\"Flow_0icqanz\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_1mzuxt6\"></sequenceFlow>\n    <parallelGateway id=\"Gateway_15cdzo6\"></parallelGateway>\n    <userTask id=\"Activity_1mzuxt6\" name=\"2\"></userTask>\n    <sequenceFlow id=\"Flow_1aqie0c\" sourceRef=\"Activity_0qm5tc6\" targetRef=\"EndEvent_mu2c2p8c0\">\n      <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${tes &gt; 100}]]></conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_016it02\" sourceRef=\"Activity_1mzuxt6\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <exclusiveGateway id=\"Gateway_0a6n8eh\"></exclusiveGateway>\n    <sequenceFlow id=\"Flow_13gvmcu\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_02klewn\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_1kkbmin\" sourceRef=\"Activity_02klewn\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0t3saos\" sourceRef=\"Activity_0ylt45t\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <userTask id=\"Activity_02klewn\" name=\"6\"></userTask>\n    <userTask id=\"Activity_0ylt45t\" name=\"5\"></userTask>\n    <sequenceFlow id=\"Flow_0dizg8q\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Event_093p7s3\"></sequenceFlow>\n    <intermediateCatchEvent id=\"Event_093p7s3\">\n      <timerEventDefinition></timerEventDefinition>\n    </intermediateCatchEvent>\n    <sequenceFlow id=\"Flow_0t5hbag\" sourceRef=\"Event_093p7s3\" targetRef=\"Gateway_0a6n8eh\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0e4xs2a\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Event_0ctoqxr\"></sequenceFlow>\n    <intermediateCatchEvent id=\"Event_0ctoqxr\">\n      <timerEventDefinition></timerEventDefinition>\n    </intermediateCatchEvent>\n    <intermediateThrowEvent id=\"Event_0pivae0\">\n      <messageEventDefinition></messageEventDefinition>\n    </intermediateThrowEvent>\n    <sequenceFlow id=\"Flow_04ilbrs\" sourceRef=\"Event_0pivae0\" targetRef=\"Activity_0ylt45t\"></sequenceFlow>\n    <manualTask id=\"UserTask_mu2c2p880\" name=\"3\"></manualTask>\n    <sequenceFlow id=\"Flow_0qhinx9\" sourceRef=\"Event_0ctoqxr\" targetRef=\"Activity_1jl42bt\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0q59g41\" sourceRef=\"Activity_1jl42bt\" targetRef=\"Event_0pivae0\"></sequenceFlow>\n    <subProcess id=\"Activity_1jl42bt\" name=\"7\">\n      <startEvent id=\"Event_032ztbm\"></startEvent>\n    </subProcess>\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_flow_mu180u86md3e\">\n    <bpmndi:BPMNPlane bpmnElement=\"flow_mu180u86md3e\" id=\"BPMNPlane_flow_mu180u86md3e\">\n      <bpmndi:BPMNShape bpmnElement=\"StartEvent_mu2c2p840\" id=\"BPMNShape_StartEvent_mu2c2p840\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"142.0\" y=\"142.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"149.0\" y=\"185.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"EndEvent_mu2c2p8c0\" id=\"BPMNShape_EndEvent_mu2c2p8c0\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"812.0\" y=\"382.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"778.0\" y=\"393.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0qm5tc6\" id=\"BPMNShape_Activity_0qm5tc6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"780.0\" y=\"90.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_15cdzo6\" id=\"BPMNShape_Gateway_15cdzo6\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"545.0\" y=\"75.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1mzuxt6\" id=\"BPMNShape_Activity_1mzuxt6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"631.0\" y=\"170.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_0a6n8eh\" id=\"BPMNShape_Gateway_0a6n8eh\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"225.0\" y=\"365.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_02klewn\" id=\"BPMNShape_Activity_02klewn\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"200.0\" y=\"500.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0ylt45t\" id=\"BPMNShape_Activity_0ylt45t\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"610.0\" y=\"340.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_093p7s3\" id=\"BPMNShape_Event_093p7s3\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"242.0\" y=\"242.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_0ctoqxr\" id=\"BPMNShape_Event_0ctoqxr\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"302.0\" y=\"372.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_0pivae0\" id=\"BPMNShape_Event_0pivae0\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"502.0\" y=\"372.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"UserTask_mu2c2p880\" id=\"BPMNShape_UserTask_mu2c2p880\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"248.0\" y=\"102.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1jl42bt\" id=\"BPMNShape_Activity_1jl42bt\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"370.0\" y=\"350.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_032ztbm\" id=\"BPMNShape_Event_032ztbm\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"222.0\" y=\"182.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1g1p7by\" id=\"BPMNEdge_Flow_1g1p7by\">\n        <omgdi:waypoint x=\"178.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"142.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"248.0\" y=\"142.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1qxvbp7\" id=\"BPMNEdge_Flow_1qxvbp7\">\n        <omgdi:waypoint x=\"348.0\" y=\"134.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"548.0\" y=\"103.0\"></omgdi:waypoint>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"54.0\" x=\"404.0\" y=\"130.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1dpq6dk\" id=\"BPMNEdge_Flow_1dpq6dk\">\n        <omgdi:waypoint x=\"591.0\" y=\"96.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"720.0\" y=\"70.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"780.0\" y=\"103.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0icqanz\" id=\"BPMNEdge_Flow_0icqanz\">\n        <omgdi:waypoint x=\"570.0\" y=\"125.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"570.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"631.0\" y=\"210.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1aqie0c\" id=\"BPMNEdge_Flow_1aqie0c\">\n        <omgdi:waypoint x=\"880.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"848.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_016it02\" id=\"BPMNEdge_Flow_016it02\">\n        <omgdi:waypoint x=\"731.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"382.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_13gvmcu\" id=\"BPMNEdge_Flow_13gvmcu\">\n        <omgdi:waypoint x=\"250.0\" y=\"415.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"500.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1kkbmin\" id=\"BPMNEdge_Flow_1kkbmin\">\n        <omgdi:waypoint x=\"300.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"418.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0t3saos\" id=\"BPMNEdge_Flow_0t3saos\">\n        <omgdi:waypoint x=\"710.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"812.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0dizg8q\" id=\"BPMNEdge_Flow_0dizg8q\">\n        <omgdi:waypoint x=\"298.0\" y=\"182.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"298.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"260.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"260.0\" y=\"242.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0t5hbag\" id=\"BPMNEdge_Flow_0t5hbag\">\n        <omgdi:waypoint x=\"260.0\" y=\"278.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"260.0\" y=\"322.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"322.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"365.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0e4xs2a\" id=\"BPMNEdge_Flow_0e4xs2a\">\n        <omgdi:waypoint x=\"275.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"302.0\" y=\"390.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_04ilbrs\" id=\"BPMNEdge_Flow_04ilbrs\">\n        <omgdi:waypoint x=\"538.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"574.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"574.0\" y=\"380.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"610.0\" y=\"380.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0qhinx9\" id=\"BPMNEdge_Flow_0qhinx9\">\n        <omgdi:waypoint x=\"338.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"370.0\" y=\"390.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0q59g41\" id=\"BPMNEdge_Flow_0q59g41\">\n        <omgdi:waypoint x=\"470.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"502.0\" y=\"390.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>',0),('12503',1,'flow_mu180u86md3e.flow_mu180u86md3e.png','12501',_binary '‰PNG\r\n\Z\n\0\0\0\rIHDR\0\0ı\0\0N\0\0\0\åg\0\0c\0IDATx^\ì\İ|T\åÿq¼\ÔK\İ\Ú\Ë\ÖıÿÿjÛ­uw»n\ï\Õv×µ+µ¶Z(Ø­\ÍmB0\İ ‰—­¤¤\nÚ€\ì¶\êv—[Ev\éVÀ\Ú*•í‚¢	TD (\n(‘[ˆ$„@PAÀœÿ\ï7d¦á™™df2—\ç<\ç\ó~½~¯I\æ\\\É9\ó<Ï—3sf\Ğ \0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\à¥¥¥\çš\Ï\0\0\0\0\0Ÿ)((8£¸¸øR©ª¢¢¢ÿ’zY~~·¤¤\äÇ„\0\0\0\0\0|b\ô\è\Ñ\ï‘Pÿ	\õ£¥fI­“zK\ê%©ŸË´›¤.+((ø¸„şÿ–\çZ¥n<x\ğ\é\æº\0\0\0\0\0@hÀ…BŸ“\ğ>J‚ûL©5\Z\ğ%\Ôo\Ç_h˜—úryyùY\æ²²\ìgdş§d¾-\òøs:\0\0\0\0\0\È2½¯]\Âù?I8ÿ™<®–zS\êUy~\Ô?Kı]AAÁ\Ù\æ²Ée¿.\ë]/\ë{V\×cN\0\0\0\0d\Ç{\Í\'\à6	\î§I\0ÿ”\ğr©\éR«zş&©y2\í¶\Â\ÂÂ¿/++;\Ç\\v \ê\ë\ëO•u”mì”š/ûq±9\0\0\0\0 s®”\Ú\Û\óiĞ–p}IOØ*\õœÔ¡’’’×¤’Ÿ«¥¾\"\óü‰¹l¶\è\Çd~(\Ûm—\Çi²6\ç\0\0\0\0L$\ğ\ëy$øûœ|	ĞŸ”\Z!aú?$T¯”Ÿ»\äq‹\Ô\Ã\ò\óX©+‡ş>s\Ù|Ğ°\ß\ó\Zş\Ø×½\0\0\0\0\0\É3¯\ğ›¿\Ã~§HPşK	\Ì!	\Ï?•zF~>(\õ©_Ë´š\Â\ÂÂ¯¼\ß\\\Ğ6ú6\Ù\çùR;\õ	úŸ\æ<\0\0\0\0€\ä$\nø‰Gş¢ÁX±\ä\â\âû¤–Kj–°ÿ¨<\ÖI}-\n}\Ğ\\\ĞO\ô\ò\ïxV\ê%½\ñŸ9\0\0\0\0Ğ·ş‚}Ó‘\Ş/’\Ğ[(!ÿ	À\r\ò\Ø)\Û\å\ñ7\òü\í\Zˆ\n\n>d.\ç\nù·ş£\Ôfù\÷>%ÿ\ÖÏ˜\Ó\0\0\0\0±’\r\ô\ÉÎ‡pû\çRß•pûc©¥\ò\óş\âw·_ ¡w|(ºF?ûn.\ç:ı\nAù\Ü\"\Õ*ÿşÿ.((¸Àœ\0\0\0\0pBªA>\Õù‘	®• ÿ	²ÿ\"µDjŸ\Ôn©\ßJ\İ)Ó¾YVV\ög\ærA¦7\ìù{\é\ß\ê_JKK\Ï5\ç\0\0\0€ K7À§»„„\ÓKJJ¾-A\õn©\'¥Ú¤\öH°_(?’\ZZPP\ğ\Í\åŸ^\é\×+şz\å_\ê}\'€9\0\0\0\0\Í@ƒû@—	¡\çK\r—P:QBı\ò\ó^©7¤\×ç¤†\é<\ærH~\Æ_?\ë/\Ï-ú®	s:\0\0\0\0E2\İ\ëU‰$³ÀĞ«\ó6¿\Õsµ~QÏ•g½Š¿X\ên™v^\å7—Cf\é\Í\åo½^ş\æ\Ï\Ê\ÏgN\0\0\0\0—%Ô“	ı*\Ù\õ9E?_\n…†H°¼S\ê¥Z¤\ö\õ\\i\ÖÏ™ÿ£~N\ß\\¹Q__ª‹‘\Å\'n|8_¿\ÖĞœ\0\0\0\0\\“J@O6\ô«T\Ö\ë;z‡|©k\õùRÿ#!r—T‡\Ô\Ó=wÖ¿^~şss9\ä_yyùYr\Ì~(Ç§]§\ñ\Û\0\0\0\0CªÁ<•Ğ¯R]¿•Fù§¿!\á\ğv	ŠI\í\ß;\åq™<\Ş#U\n….2—ƒ\İ4\ì\Ë1œ\Úş¨ÿ`\Î\0\0\0\0~•N O5\ô«t¶“7\Ş?XXXxµÁ:}¸\Ô\ëR$ 6\Ê\ã}RE=o?\Å\\ş¤Ç³\çX\ïÔ·ÿ\ë\Ç\0\Ìy\0\0\0\0ÀO\Ò\r\â\é„~•\î\ö²J\Â\Şû%\Ì_%A¯F\ß#RÛ¤J­\ç*U\n…şb?\ôrìŸ•zIoügN\0\0\0\0?H\0O7\ô«lwÀJKKÏ•07X\Â\ÜX©_\É\Ï[\å±K›$\ôÿ‡TiAAÁ_\r\"\àœÿ(µYoÀ(\ç\Èg\Ì\é™\ày\Ş\Í\ÍÍ®Zµ\êhCCƒ·t\éR*\Ç%\÷î¦¦¦=×™\Ç\0\0\0\ğ«ï„~5\Ğ\í\'E\ÂûŸşƒ„¶Hxû¥8©C=Wq\õ3\Üe2\í¯y7<x\ğ\ér\Ü\"\Õ*Áÿ¿åœºÀœg $\ğÏ—À\é\íİ»\×;|ø°\÷\Î;\ïP9.ı»\ë\ßÅŠ—.]:\Ô<F\0\0\0€\ßd\"p4\ô«L\ìGTYY\Ù9Ê®\Z#m\Ô\ï¥Ş”z^jº<ƒ\Ô\ßHh;\Í\\\è\Ï\ğ\á\Ã\ßW|\â\ë\÷é£¾cÄœ\'z…_§D©\ÜWkk\ë>	ı/š\Ç\0\0\0\ğ“L\íL„~•\Öş6\ì½¼.—ú¾ù9R{şEEE?“ú„²O\ğ‘iz¥_¯ø\ë•}€¾Àœ\Ç$Ëœa>¡o\é\ç\n¿¥\ÇABÿ\ó\0\0\0~‘VÀN S¡_\õ¹_˜Î–ÿ·R·J\Èú…\Ô+RoI½(5SªB¦}6™\ğdŠ~\Æ_?\ë/\ç\ßyü9½7™\çqı‰ù¼\ÒÏ”›\á“\Ê_\é\ñ0\0\0\0\à}\ë4d2\ô«\ğşu\ÖYW‡B¡/I ºY‚\ÒÏ¥^\î	øk¥fI\İ(\õùÑ£G¿\Ç\\zw	\ô\ë\å¼|V~ş;sºLû¦L\ó\ôw\Ş&ú\ß<\Ğ\ê5¯ù…·qÙ¤p\é\Ïúœ95°\"\ô\0\0À2øU¦Cÿ 	ü_»şúë»¥\ô\Ê\éI€ª…B_\ì\ë­Ñ€\r\ôFrÎ”`¿Sj¾œ³G/>\ñ\Î\rıZ“\Ìe“	ı‡:[¼W—\Ü\é½üD\ÍI¥\Ï\é4s~*ı\"\ô\0\0Ào²øU\ÆC¿\Ò\à?(;ûd]yyùYş(\á¾]§I\İ\Ö+\ğk“úr\ïe’	ı»6.Œ	ü‘Ú½qQ\ÌüTúE\è\0\0€Ÿd+ğ«¬„ş\Ù\Üo \ëJJJ>¬7””\ÇwĞ¯µYoD™7™\Ğÿû\å\÷Ä„ıH\é4s~*ı\"\ô\0\0À\nzãº‚‚‚\÷›\Ï\÷’Lp\î\Ü\óU‰\\9¨ÿı¬%\á~BœÀ©‘ù’	ı¯.­	û‘\Òi\æüTúE\è\0\0€JJJ&JpxGj‰Ô\òûÿ\é5Y¯\"j`\×\ë¹x\Ì\0\ê‹\î¿ş;¢WE?(++û3yMv\Å	û‘\ê.**ú†\ÎKè·«ı\0\0\0°‚„†\"Ş•\ñŒÔ˜P(\ô±A\É])7x>*‘+\õ¿ÿ€•\ä\õxœ R\Éku¯¼V?˜L\è×»\õ›a?R:ÍœŸJ¿ı\0€(\Ï\ó\Îhnn~tÕªUG\Z\ZÂ•Û’¿{wSSÓ\Æ\Æ\Æ\ë\Ì\ã¸®¨¨\èfˆ0j\Í×¿ş\õ\Ù\ç{\î¾\ÓO?ı*sùH&¸§‹À\ß*((8[¿}B^ƒ¿Ğ’p\ß(Ëµ\ä\ç\×\äq»\Ô>©n©\æ\ßş\ö·1ÁÓ¬­\ÏÍˆ	û‘\Òi\æüTú¥\ã\ó˜\0Jÿ|	œ\ŞŞ½{½Ã‡\Çt\ZT\öKÿ\îú\÷_±b\ÅAé¤‡š\ÇpYYY\Ù9=¡Áû½\ë¨\ï~\÷»\İYş\Ù\nı~\Â\ğ\á\Ã\ß\'¯\Ñ©¨¨\ğ:\Ó\Ç\õ®–\rŞ«Oÿ(&\ğ\ës:ÍœŸJ¿ı\0€(½Â¯\Ó\ì,¨\ÜWkk\ë>\é¤_4\à¢ÁƒŸ\n…ş¢¤¤dXQQQgœ ©×‹O|\Öÿ“Yú\Z¼l„~?\ç\É\'ŸŒ\é\×\â\Õ\ëk\çÆ„~}Îœ\ZXú\0Qú–~®\ğ\ÛQz¤“>b#À\Ï\Ê\Ë\Ë? ¡ı\Ë\Úo\Ç•Z µI\ê°\Ô6	üO\È\ã8aÿ¨L_PPpš±\ÊL\êL‡şL\ï\à\Z2\Í~-¦\ñ¶>?3&\ô\ës:-f~*\í\"\ô\0¢’ê¤©œ4ü¨¾¾şT	\ç—şM	\ë\Õ\òø€<®z£\ç\Î\ßk¤\æŸø\ê¯\ï\Ê\ôO}\ó›\ß<3²¼<7\İü[C¡Ğ—zoÃ\É`\ÉĞŸ\Éı|¥¿\ñÄ¡\Îo\Ës\Óc¤tš\Îc.G¥WŒ\'\0\0QıuÒ½\ë\Èá·¼\ök¼?¼\ğŸŞ—3\Zx\ÑI\Ãf\ìÿD\Âø%´—J0¿[ı£\ò\óùù\í\âW\ë\õk\÷¦\Ê\ó7K]%?Ÿo®#™\ï–^ÿ\çºs82°3ú3µ?€/%O9\âµnm\ô^yê˜ o–Î£\ór\Õ\à\Åx\0•°“\îU{·x;^z\Ô{uÉÑy\ó\Ê)1\óQ/:iX\à	\Ş‘\Ğşu©–Pÿ3ù}™\Ôn©7\å\÷\õ\òü¯\ä\ç\É\ÏÅ¡P\èsÃ†\r\Ğ\÷\Ï^-\ë\ë\õ}ÇœÖL\íL„şL\ì\àk\ñ\Æı]\İOT\\\õx1\0\0D\Åë¤µ\Ş>\Ô\éµüş)oS\ãO\îŒ\×z\Ík\ô\Ş<\È\Íÿ²Qt\ÒÈ•¯\ãú¬„\í\"\r\ğRKıNƒ½T‹TCO\àÿ¾<~C\æÿ¨,vŠ¹L\Ğ\ïø–\õ_`>Ÿ¤î†şnpB¼\ñD2W\÷•.k®J¾O\0\0¢\âu\Òouµy¿_~OL¬¥WûušY¯­¸\Ïk\ß\ñbÌº¨ÔŠN\Z™&¡ıü\Â\ÂÂ¯Jp¿I~*OŸø~m}Kş+R\ó‹O¼U„<^ª_¿e®\Ã¼ú²]À)\ñ\Æ\æ\"\Õ2\×G%_Œ\'\0\0Q\ñ:\é×š~\Z\Ó\ñ&S|\Î\àE\'t\èM\ñ\ô\æxÜ¯\×;\ŞŸ¸iŞ‹R‹O\ÜL\ïyœ%U\n…†H]¤7\ß3\×\ãs\é\ğtCº\Ûœo<A\å¯O\0\0¢\âu\Òf˜W\Ík~ş^\İh­{\ÈÛ³y©\×ù\Æ\æ˜\õQ\É4úRVV\ögÜ¿R|\â{\ëZt\â\ë\îşP|\â\ë\ï6\É\ïÿ#\ÏÿX¿O~ş[ıº<sK\'ˆ§ú\Ó\Ù\à´x\ã	*\Åx\0¯“6~*µqé¤˜\õQ\É4Fı	íŸ”\Ğ~ù:©_H=/µ_jŸ\ÔsR?—\é5\ò8\\ÿr\ğ\àÁ§›\ë	°Tyª¡?\Õ\õo<A\å¯O\0\0¢\âu\ÒfWzg\İm/ş\÷Iµıw¿\ô:v¿³>*ù¢“‚‚‚Ih¿\\Bû\÷¤î‘ŸÿW\Âşk=W\í·H-’ºOªB¿B\ê\Ã\æ:P*Á<•ĞŸ\Êz@‰7 \òWŒ\'\0\0Q\ñ:\é\rO\Ş\ò“©\rOş\Ğ\ë\ê\Ø³>*ù¢“v‹ûÓ¤.–Pÿ-	\íc\å\ñ¿$Ä¯”j“: \õ‚<?G\êvùùe\ŞK¤\Î0×ƒ´$Ğ“\rıÉ®¤x\ã	*\Åx\0¯“~}İ¼˜@oÖ¦\Æ¹ƒ¿^ı\ëĞ¾˜\õQ\É´?•––\n…¾$Á½L\ê_¤“ ¿±ø\Ä\ò›¥K\àÿy®R\êJ	\öÿ\×\\²\"™ L\èOf=@ \ÅOPù+\Æ\0€¨xtW\Ç\Î\ğU{3\è\ÇıúU}-/Ç¬ƒJ¯\è¤\í¥w»—\ğş\çÚ¯•\0›ü<Sj¹\Ô©CRk¥~)u§L/ù>S^^~–¹\ä\Ü@û@—!\Şx‚\Ê_1\0\0D%\ê¤;Z6x—\İ\ö\ÍÒt¾\ñû˜\å©\ôŠN:ÿ\Ê\Ê\ÊÎ‘\Ğş	\ï!	\î\å\ñ©—¥Ş’\Ú)\õ´\Ôt©[¤¾VZZz¡¹X\'\İ\à\îr@\à$\ZOPù)\Æ\0€(:i»ŠN:w4¬^-ÿV\r\ñ\ğ—\Êã®pÿ’Ô¯5\ôK•\ÈÏŸ\×ÿ0\×_I5À§:?hŒ\'\ì*\Æ\0€(:i»ŠN:³\n\n\n\ÎÖ·\ÙK\ê\Û\î‹O¼ı~]\ñ‰·\ã\ï‘\ç\å\ñ~}»~(ºF\êc²\Ø)\æz\àŒdƒ|²\ó\èÁxÂ®b<\0ˆ¢“¶«\è¤Ó£7Æ“\ğ>XªJo˜\'OJ½^|\âFz¯\Ês¿)>qƒ½2ùù2½\ñ¹F¾¿\é\0\â`<aW1\0\0D\ÑI\ÛUtÒ‰\éW\Ù\éW\ÚIhÿ„\÷;\ô«\î\äquñ‰¯¾Ó¯Àk’úOy~¬<•ù>¡_™g®”8\Ø\'z@?O\ØUŒ\'\0\0Qt\Òvt8ÜŸ\'Áı\n©Q\ÜÿMü\"©­R‡\å¹\×\ä\ñ·R?‘iß“\ßÿ.\n}\Ğ\\3\à›¿H\ã	»Š\ñ\0 ŠNÚ®\nJ\'=x\ğ\à\Ó%´ÿ¥„\÷\á\Ük\å\ñ\çR\ÏIu\ô\Ô\ó\òü\ë4™ïº‚‚‚¿\Òe\Ì\õ\0	ú\ãz	ü@šO\ØUAO\0\0’@\'mW¹\ÖI\ëUx	\í+!¾¼\ç\êüÿ\È\ã\ï\õª½\Ô¤ï¹š£\ÔW\ô*¿¹ Ë®”sO_w~`\0O\ØU‰\Æo¼\ñ\Æ\Çw\î\Üù\êK/½\ô\æ]w\İ\õ¯ü‡:\0\0´]•¨“¶Y}}ı©úùy	øC\ä\ñ\Å\'>WÿŒ<\î•:(\õ¢\Ô\Ü\âŸÃ¿^\êo¾ù\Íoi®È—\Ğ`\0O\ØU\æx\Â\ó¼Ó¶l\Ù\òGy\×\ë!\ó{\ä‘GZ¾\÷½\ï]\Ş{^\0€c\è¤\í*³“¶\É\ğ\á\Ãß§w¾—\Ğ>Bjr\Ï\ñ_)>q‡ü\×\å\÷§\äù)\òsUaa\áWGŒ\ñÿ\Ìu\06\"\ô\ÇxÂ®\ê=xı\õ×¯Ü»wo[$ì›¶o\ß~\ô{\î™_PP\ğş\Ş\Ç\0\à:i»Ê‚\ĞŠ~W½øoHú¾\ÔıR\rR-RoJıN\êa™~—üByü¬\Î6Wø	¡8\Æv•\ñ¾?ü\áO¼ûn\ô\â~B\İB–9T]]}ƒyl\0>G\'mW\å*\ô6\ì½t>/¡]Š\ë¥~-?¯—Ç·¤vI-“š!\Ï\İ*\áş\ë\ò\óGd±S\Ì\õ\0. \ô\ÇxÂ®Zµj•w\ğ\àÁ\Ãf¸\ïOgg\ç\Ñx`]AAÁ\Ç\Íc\0\ğ):i»*Ó¡_:\í$\Ğ|M‚û\Í\à§\É\ÏOK\í\è	\÷/\Ë\ó\Ê\ã$©ø¢\Ìÿ\'\æ:\0\×úc<aO\íÙ³\Çû·û7oß¾}f¦\ï—.£\Ëj»H¹S\æ\ë@À\ĞI\ÛU\é„ş\ò\ò\ò³JKK?-¡¾@\Z\ö	\ä’Çµ\ò\Ø%­R\Ëe\Ú\òû©k\õ\ï\õ\æ{\æz€ b@\ã	»jÁ‚Ş¨Q£nlll\ï\î\î\îÿııw¼µµ\õ\×ÿ\ôOÿ¤\÷\êYÌ•~w\Ğ\Ç “¶¬ú\nı\ØÿÔ•\Òx–úwí”¥¶I–\ç7\Ê\ã©•€?2\n}\É/7\ä¹\éş•3­±\Ã|>\ÌmWÿlùÅ·Mkü}\ïy1—…1 Î…\ñÄ›o¾\é]y\å•\Ú\ÄL\ó[E\Æzÿ;\ï¼so_7\ò{\÷\İw\×\ëü¤-Ü­\ó‡>GÀ‰NÚ¥Z²d‰\'¡ı¯¥şGyü¡<>(\õ‚ü\Ü)\íR+¥fK“\ç¾%\áş/$ÜŸfW¤Gÿ\÷o›\Úø\ï\æ\óp\"`\àü>\Ğ\ÏÀ_q\Å\áÀ¯eN\÷[\õ¾ˆ JKKg>ü\ğ\Ã?şv¯¼ßµv\í\Úe\Úni\ï\÷\Ë\Å¤†>€\ï;i—\ê\í·\ß\ö\Ê\Ê\Ê\ô³W›¥ş·¤¤\ä^yü\'©\ËGù§\æ±\ó£1\Ó\Z¯\Ö0,ü\ó\ô\å?\Z3µaÜ˜i\ËK%hÿ2:>?­±M\êP$€»o\É9c¦5ü¢zZ\ã~y~\çm\Ó‡\÷Zç¾ºYK\ß?fz\ã\ß\Ë\Ï+o›\ÒøW2\ïV=½1tÒºy\ÍmK\èR[¬Û¹m\Ú\òº\èrq\ö\É\\şÅ€8¿\'\äŸ\à]v\ÙeN†şˆ¢¢¢¿½\í¶\Û6‰7\ö\ïß¿\ò?øA£´/\ë\ó\æ¼p}\0\ßwÒ®•^\é7‘K\"oŸ—ú©‘ú\\Oø¯\Î\äy§\Ü6u\éG«§6F\Ş>/!}š^…ÿ\çi‹Ï•À<¿\'2»\ô\ç~0­\ñ³\ÕS\İ6cù·n›\Öp£şN“Ÿ\'\ô^·9o\ïmWÿû\óg\ËzVO]~\í\÷§6\\.?ˆ,ŸŒı†o1 \Î\ï\ã‰ë®»\Îkmmu:\ô«ÁƒŸ^RRR+\í\ŞV}\Ô\ß\Íy\àú8\0¾\ï¤]«D´K\ôJú˜©·ÿ\ñ\÷Æ‡\ôª¹ş|\"°7n\Õ+\õ\òøº\ÔÜy\ö\Ü2cYø\İcÿmù‡\å\÷\Î^\Ëÿ¼zz\Ãe™\r\Z\Îu™1S—:1­\á\"\ëN0ot\Ûc¦7|\ó¶\é\Ë\è\Ï\Õÿ\ŞpLÛ«?\÷±O\Ñe\áoˆ€se<1\È\ñĞà¡&/\ğ\åRƒ\Í\çM®tÒ®”ë´„ıI\ášÖ¸b\Ì,ÿ@ø¹i««§5|©\ç\çßŸºüS·Miü²üÜ¢o\Ã\ïy¾\ãûS¿~k~ø#\rÿ]\ç\ô\ÆZ™\Ş%Ï—\õÌ»®zÚ²¯\è\Ç\ä\ç\ã‘u\'˜\÷\Û\ÖwLo|\"üŸ\n\òs\ä\İ}\ìStYø\"`\à\\O\"\ô\Ã1\ôq€\Ã\ô\ŞS}†W:iW\Ê\åNºzZ\ã\ÄÛ¦5Ş¥?\ëg\ò%4?\Ü\óüş\ê\êCú³<w¿\Ô[ú\ö{\r\Ûÿ<­\é¼\èü\Ó·\És‡\ä\ñÁ\Ú{}_t½S¯“\çw\Ô\×/¿EQ\Ö7uŒ¾MZ\ã\Ê\ã¾Èº\Ì\Û{\Û[{>»\ß1f\ê\ò§ûÛ§\Ş\Ë\Â\ß\ç\Êxb¡¡\Ö+\ô\÷ş]\é¤]):\é\Ô\ô|ÿ¡\ê\é7›\ÓL©Ì‹`a@œ+\ã‰A„~8†>pXœ\Ğ7ü»\ÒI»RtÒ©\Ñ+ÿ\ÕÓ–?Z__ª9Í”Ê¼DÀÀ1°«O ‚>pXœ°oV8ü\ÓI\ÛUt\Ò@\î1 \ñ„]\Åx\ôq°\ÊÁƒ?´p\á\ÂI3f\Ìxy\âÄ‰û\ê\ê\ê\ßt\ÓM\İz¢VUU7n\\\×ø\ñ\ãwNš4iQMMÍµ²\È)\æ:l\'t[Qt\Òv4{\Úš\ÏH\r\ã	»Š\ñ\"\è\ã`…Õ«WWM™2¥¥²²Ò“ \ïÍŸ?\ß[·n·m\Û6¯££\ÃSú¨¿\ë\ó:½¶¶¶»¢¢\â	ÿKB¡\ĞE\æ:m”\ëœ\î{\Õ\òb\Ş\Şom\ÑI¹—\ë\öp\ã	»Š\ñ\"\è\ãW6lrÿı\÷\ï=z´\÷\Øcy\í\í\íá€Ÿ,_—“\ğ´ººúÑ’’’›Û°I®_pı…ı?w\Ò\Ï<\óŒwé¥—z\çœs\÷\Ù\Ï~\Ö[¾|y\Ì<~+:i \÷r\İ>.\ò\óx\Â\Åb<ú8\ä…\ä\õ\Ó/^¼P\Ãşœ9s¼C‡™y>%º¼®§¼¼ü\í#F|\ÛÜ-rı‚\ë/\ìGø¹“ş\Ú×¾\æ½\ï}\ï\ó–,Y¢[\ï‚.ˆ™\ÇoE\'\r\ä^®\ÛgÀE~O¸XŒ\'A‡œ“Œşy\ó\æm¹\å–[¼\æ\æf3¿ˆ®o\ô\èÑ‡+**&šÛµA®_pı…ı:\é•+W†Cÿ—¾\ô¥˜i~+:i \÷r\İ>.ra<\áR1@}rJrù¦M›\Ö9a\Â¯³³\Ó\Ì\ì¡ë­««{{\äÈ‘s\Í\íç›­/8:\és\Ï=\×;ÿü\ó½-[¶\ÄL\ó[\ÑI¹gkûø‰\ã	—Š\ñ\"\è\ã3’\ÇO›7o\ŞV\rü\Úe“®Ü¸qJKKÇ›û‘O¶¾\à\\\è¤W­Z¾\Òÿ•¯|%fšßŠN\Z\È=[\ÛgÀO\\O¸TŒ\'A‡œ\Ñ\Ï\ğ\ßz\ë­Y»\Âo\Ò\íŒ\Z5ªKN\ò\á\æ¾ä‹­/8:\é·\Şz+ú\õ†~\æ4¿4{¶¶Ï€Ÿ¸0p©O ‚>9±~ıú¡zÓ¾L†¿?º½#Ftœg\îS>\Øú‚\ós\'}\ÅWx\ï}\ï{½„Cÿ7¾\ñ˜yüVt\Ò@\î\Ù\Ú>~\â\ç\ñ„‹\Åx\ôq\È	ıZ>½»~>Ìœ9sœ\èS\Í}\Ê[_p~\î¤\õ+ú>ı\éO‡ƒÿUW]şs¿4{¶¶Ï€Ÿøy<\áb1@}²n\õ\ê\ÕUz•¿««\Ë\Ì\ã9¡\Û-++;\n….2\÷-\×l}Á\ÑI\ÛUt\Ò@\î\Ù\Ú>~\ÂxÂ®b<ú8dİ”)SZ\ô­\×ù4w\î\Ümr²\Ï2\÷-\×l}Á\ÑI\ÛUt\Ò@\î\Ù\Ú>~\ÂxÂ®b<ú8dUWW×ŸVUUymmmf\Ï)\Ùş\ñP(´·¾¾şTss\É\Ö´]E\'\rä­\í3\à\'Œ\'\ì*\Æˆ CV=şø\ã\ë\ê\ê\Ì£F\Ú.\'ü\å\æ>æ’­/8:i»ŠN\Z\È=[\ÛgÀOO\ØUŒ\'A‡¬š>}ú+\ó\ç\Ï7\ów^<\ğÀ/\É	ÿss\É\Ö´]E\'\rä­\í3\à\'Œ\'\ì*\Æˆ CVMœ8qßºu\ë\ÌüMMMŠŠŠšû˜K¶¾\à\è¤\í*:i \÷lmŸ?a<aW1@}²ª¶¶\ö°~…š\r6oŞ¬o\ï_c\îc.\Ùú‚£“¶«è¤Ü³µ}ü„\ñ„]\Åx\ôqÈªªªª\îıû\÷›ù;/:::\É	\ßb\îc.\Ùú‚£“¶«è¤Ü³µ}ü„\ñ„]\Åx\ôqÈª²²2\ïØ±cfş\ÎÙ.=\á\ó]\æ\ß\Èt\Òv4{¶¶Ï€Ÿ0°«O ‚>YUYYyÌ–+ı\í\í\í(\Î\ó•~[\ÑI\ÛUt\Ò@\î1 \ñ„]\Åx\ôqÈª±c\Çv\Ù\ò™şM›6­-\Î\ógúmE\'mW\ÑI¹Ç€8\Æv\ã	D\Ğ\Ç!«&L˜°Ë–»\÷/[¶¬!\ßw\ï·´]E\'\r\ä\"`\àO\ØUŒ\'A‡¬ª¯¯_4ş|3\ç\Å\äÉ“\È	ÿsA\'m[\ÑI¹Ç€8\Æv\ã	D\Ğ\Ç!«\ê\êê®©­­\í6x…Bä„¿\Ü\ÜG\ĞI\ÛVt\Ò@\î1 \ñ„]\Åx\ôqÈªúúúSG\Z\õN[[›\Âsj\÷\î\İ\Ï\É\ÉŞªûc\î#\è¤m+:i \÷\ÇxÂ®b<ú8d]MMÍ’˜9<§\î¾û\î‡\ådŸe\îN “¶«è¤\Üc@\\CCC\÷\áÃ‡cú5*\÷%\ÇaŒ\'˜\ÇÁD‡¬…BUTT\í\ê\ê2³xNtvv¾(\'z›î‡¹o8\ĞoWú\Üc@\\SSÓ½{\÷\Æ\ôkT\îkÇ¿–\ñÄ‹\æ1B0\Ñ\Ç!\'ª««3g™\ÇsB¶ıˆœ\èS\Í}\Âú\í*B?{ˆ€kll¼nÅŠ[[[\÷q\Å??%\÷\Ö\íÛ·?,c‰RC\Íc„`¢CNœW^^şvss³™É³j\íÚµú¶ş6İ¾¹Oø#B¿]E\èr/ˆ\"\é&Ï~ù\ÑU«Vmhh·=TnK\ß¯W\Ç5,›\ÇÇ¯\ä\ß5T\êE©w\Ì/•“Ò¿»şı	üˆ\nb‡<1bÄ·+++wvvš\Ù<+\Ú\ÚÚ‘|‡\Ôps_p2\í$\Ì\àI\å¯\ôx˜\Ç@vq@$¾NOß\ÍU\Ùü”ş\İ\õ\ï¯W\Ç	i\0²%ˆ}ò¨¢¢bb]]\İ\Û\Ú\ÑeÓ‘#G6…B¡5%%%·›û€X„~»Š\Ğ\ä^Dz…Ÿ\Ï_\ÛQúvx½:k#\0È„ \öqÈ³‘#G\Î7nÜl]\ñokk[¡_N\î\Ù\æ¶¡ß®\"\ô¹\Ä‘¾¥Ÿ+üv”‡¥\Üi@–±ƒJKK\ÇWTTteú3ş=Ÿ\á\ß\Éş\Ôú\í*B?{A\Ñ\ö\ÛU´ı\0²%ˆ},¡Ÿµ1bD\çÌ™3\÷:t\È\Ì\ï)9p\àÀê»\ô·\ñş\Ô1\ğ³«ø¹\ÄQ²mÿ›Z½\æ5¿\ğ6.›.ıYŸ3\ç£V´ı\0²%ˆ},RRR\òa9	§–••|\ğÁ·µ··3}µ´´<7q\â\Ä\ğúu=º>s\è_²?*7\ÅÀÈ½ ˆ’iûu¶x¯.¹\Ó{ù‰š“JŸ\Ói\æüTúE\Û [‚\Ø\ÇÁB¡P\è\"9gIho»\ñ\Æw<\ğÀë›šš6lŞ¼y\ç¾}ûŞ”€¨­­­y\ãÆk¥S\\:y\ò\ä²\ÌY\æ\r]N—7×‰\ä%3\ğ£rWü€\Ü\â€(™¶\×Æ…1?R»7.Š™ŸJ¿hûdKû8\Ø\í”\Â\ÂÂ¿—\ğÿã¢¢¢…r‚¾(µKOÔ\Ç\õy®\ó\éü\æ\nºd~TîŠ{A%\Ó\öÿ~ù=1a?R:ÍœŸJ¿hûdKû8\0††††n\î\àlG\ÉqØ³”;89\ÄQ2¡ÿÕ¥\õ1a?R:ÍœŸJ¿ı\0²%ˆ}\0CSS\Ó¾«ÙÚ±cÇ¯—\ò]\Í@\Îq@Dè·«ı\0²%ˆ}\0Ccc\ãu+V¬8\Ø\ÚÚº+şù)ù»·nß¾ıa\ô\í”\Zj#\0\Ù\ÄQ2¡_\ï\Öo†ıH\é4s~*ı\"\ôÈ– \öq€³8\ğ‰E‹5Í˜1\ãp}}ı»uuu\ŞM7İ¤\÷Cğªªª¼±c\Çv\ßq\Ç\Ç\'Mš\ôFMM\ÍY\ä\ÔÈ²\Z4\õ\n³\Ô;:\ğ\ğc\é¿\Ó|\ÎG¥wıûø<\â€H\Û3xšµ\õ¹1a?R:ÍœŸJ¿\ôx˜\Ç\02!ˆ}\àœÕ«W?<eÊ”ã•••ıù\ó\ç{\ëÖ­\ó¶m\Û\æutt„¿\ßP\õw}^§\ë|^mm\í¾’’’+\Íuú\r\Z€t±ıH&\ôw´l\ğ^}úG1_Ÿ\Ói\æüTúE\è-A\ì\ã\0g¼\ò\Ê+?ºÿşû=\Ú{\ì±Ç¼\ö\ö\öpÀO–Î¯\Ëiø3fÌ®¢¢¢¿4·\á\'4h\0\Ò\Ä\ö#™Ğ¯\õúÚ¹1¡_Ÿ3\ç£V„~\0\Ù\Ä>\ğ=\É\ëg,^¼¸U\Ãşœ9s¼C‡™y>%º¼®§¼¼\Ü+++û±¹=¿ A® ¶I…ş#G¼­\ÏÏŒ	ıúœN‹™ŸJ»ı\0²%ˆ}\àk’\Ñ?6w\îÜ£·\Ür‹\×\Ü\Ül\æ\÷\Ñ\õ\é$TTT<cn\×h\Ğ\0¤+ˆ\íG¡ÿPg‹·\å¹\é1?R:M\ç1—£\Ò+B?€l	bø–\ä\òM›6­{Â„	^gg§™\Ù3B×«Ÿ\÷¿\á†¶™Û·\r\Z€t±ıHú\ñZ·6z¯<uGL\Ğ7K\ç\Ñy¹\ê?\ğ\"\ôÈ– \öq€/I?cŞ¼yG5\ğ\ë\à ›tız§ÿ\Ò\ÒÒ¥\æ~ØŒ\r@º‚\Ø~\Äıı]\İOT\\\õxúdKû8À—\ô3ü·\ŞzkÖ®\ğ›t;\İ%%%“\Í}±\r\Z€t±ıˆú“¹ºŸ¨tYs}T\òE\è-A\ì\ã\0\ßY¿~}½~\Ö>ÓŸ\á\ïn¯´´´ûú\ë¯ÿ¤¹O6¢A® ¶\ñB¿\äS-s}T\òE\è-A\ì\ã\0\ßÑ¯\åÓ»\ë\ç\ÃÌ™3KC\ñ’¹O6¢A® ¶\ñB?•¿\"\ôÈ– \öq€¯¬^½úa½\Ê\ß\Õ\Õe\æ\ñœ\Ğí–••\é\Ûü¯4\÷\Í64h\0\Ò\Ä\öƒ\ĞoWúdKû8ÀW¦L™r|Á‚fÏ©9s\æ•\Æb“¹o¶¡A® ¶„~»Š\Ğ [‚\Ø\Ç¾q\ğ\àÁ¿¨ªª\ò\Ú\Ú\Ú\ÌSºıP(\ôn}}ı\é\æ>Ú„\r@º‚\Ø~ú\í*B?€l	bø\ÆO<±¢®®\Î\Ì\àyq\ã7+))©4\÷\Ñ&4h\0\Ò\Ä\öƒ\ĞoWúdKû8À7fÌ˜qdşüùfşÎ‹Y³f–c•¹6¡A® ¶„~»Š\Ğ [‚\Ø\Ç¾Q__ÿ\îºu\ë\ÌüMMM‡‹ŠŠ\Ş0\÷\Ñ&4h\0\Ò\Ä\öƒ\ĞoWúdKû8À7jkk½\æ\æf3\ç\Å\æÍ›Iƒñ¦¹6¡A® ¶„~»Š\Ğ [‚\Ø\Ç¾¡7\ñÛ¿¿™¿ó¢£££[\ZŒ\ã\æ>Ú„\r@º‚\Ø~ú\í*B?€l	bøFYY™w\ì\Ø13\ç…ì‡†~«\Û\÷€½‚\Ø~ú\í*B?€l	bøFee¥5Wú\Û\Û\Û\õ\íı\\\é\à¤ ¶„~»Š\Ğ [‚\Ø\Ç¾\ñƒü Û–\Ï\ôoÚ´\é->\ÓÀUAl?ıv¡@¶±|cüø\ñ\Çm¹{ÿ²e\Ë\ös\÷~\0®\nbûAè·«ı\0²%ˆ}\à?úÑŞ˜?¾™¿\ó\â\î»\ïn‘c•¹6¡A® ¶„~»Š\Ğ [‚\Ø\Ç¾QWWw‡”™¿\ó\"\n.))©4\÷\Ñ&4h\0\Ò\åRû!ÿ–\åRƒ\Í\çM„~»Š\Ğ [\\\ê\ã\0\ç\Ô\××Ÿ>j\Ô(¯­­\Í\Ì\à9\Õ\Ò\Ò\Ò%ÿ]\İsmBƒ ].µúo\é©>\Ã?¡ß®\"\ô\È—ú8ÀIµµµû,X`\æ\ğœºûî»·Kc±\É\Ü7\ÛĞ H—K\íG¯\Ğ\ßgø\'\ô\ÛU„~\0\Ù\âR8©¤¤\äÊŠŠ\n¯««\Ë\Ì\â9±ÿş7\õ*¿î‡¹o¶¡A.—Ú8¡?nø\'\ô\ÛU„~\0\Ù\âR8kÌ˜1»\æÌ™c\æñœ¨®®\Ş)\r\ÅK\æ>Ùˆ\r@º\\j?\â„}³\Â\áŸ\ĞoWúd‹K}\à¬ë¯¿ş“\å\å\å^ss³™É³j\íÚµ\Û\õ*¿n¿\÷ş\Ä@ZS½\÷\0’e¶%A(B¿]e*¿e¶€ŸqN>QVV\ö\ã\Ê\ÊJ¯³³\Ó\Ì\æY\Ñ\Ş\Ş~@\Zˆcú\'›ûB\Ã\0\ö2\ÃK¯Z^\Ì\Ûû­-®\ôÛƒq\\\Ã9\røHEE\Å3ú~:8È¦#¢´´\ôMi –˜û h8\0À^ı…ı?‡ş§z\ÊûÔ§>\åu\ÖY\Ş¿øEoİºu1\óø­ı\ö`œ\×pN>s\Ã\r7l;vlw¶®ø\ëşÀ¿\Ù\Üv\r\0Ø«¿°\á\ç\Ğé¥—zgyf$({_ø\Âb\æ\ñ[ú\íÁ8®\áœ|HBùÒŠŠŠ\îL\Æ_?\Ã/\Â\ñ\âWø#h8\0À^ı…ı?‡şHmß¾=ú\Ï=\÷Ü˜i~+B¿=\çÀ5œÓ€O\ég\í%üwÏœ9\óø¡C‡\Ìü’\Î\Î\Î7\õ.ı=_\Í\ó~\r\0øŸ¡üø\ñ\á\Ğ?dÈ˜i~+B¿=\çÀ5œÓ€ı¥¼ˆ_*++\ë~\ğÁ¶··›y¾O{\ö\ì\éš8qbøıº]Ÿ¹xh8\0Àÿüú\õ«_y§zªwşù\ç{[·n™\î·\"\ôÛƒq\\\Ã9\r8@Bû•\òbŞ¤\áı\Æo<6s\æÌ·›ššoŞ¼ùø¾}ûº%\ßw·µµÛ¸q\ã[2¨\è˜<y\òP(tX–Ñ°¯\Ë]i®³/4\0\à~ı\Ï=\÷œw\ÆgxŸÿü\ç\Ã_gkN\÷cú\íÁ8®\áœ\Ürjaaa•¼°W½!zC>ıŒ¾\Ş\ĞI\ß\ìy~•Î§\ó›+H\r\0øŸŸCÿ5\×\\\ã]{\íµŞb¦ùµı\ö`œ\×pNH\r\0øŸŸCÿyç§ıP´&L˜3ßŠ\Ğo\Æ9p\r\ç4€”\Ñp\0€ÿù9\ô»X„~{0Îk8§¤Œ†\0ü\ĞoWú\íÁ8®\áœ2\Z\0\ğ?B¿]E\è·\ã¸†s\Z@\Êh8\0Àÿıv¡\ßŒs\à\Z\Îi\0)£\á\0\0ÿ#\ô\ÛU„~{0Îk8§¤Œ†\0ü\ĞoWú\íÁ8®\áœ2\Z\0\ğ?B¿]E\è·\ã¸†s\Z@\Êh8\0Àÿıv¡\ßŒs\à\Z\Îi\0)£\á\0\0ÿ#\ô\ÛU„~{0Îk8§¤Œ†\0ü\ĞoWú\íÁ8®\áœ2\Z\0\ğ?B¿]E\è·\ã¸†s\Z@\Êh8\0Àÿıv¡\ßŒs\à\Z\Îi\0}’Fb¹6ı\Ôrs9\0€\İıv¡\ß$¸†s\Z@Ÿ¤‘\'\ä›5\Ø\\\0`7B¿]E\è·	®\áœ\Ğ/½’\'\ès•\0|Œ\ĞoWú\íA@‚k8§\ôK¯\ä\Ç	û\\\å\0#\ô\ÛU„~{\à\Z\Îi\0I\Ñ+úq?WùÀ§ıv¡\ß$¸†s\Z@R\ôŠ~œ\Ğ?Øœ\0\à„~»Š\Ğo\\\Ã9\r i\Æ\Õ~®\ò€ú\í*B¿=Hp\r\ç4€¤Wû›\Ó\0şAè·«ı\ö  Á5œ\Ó\0R¹\Úo>\0\ğB¿]E\è·\ã¸†s\Z@J\"Wû\Í\ç\0şBè·«ı\ö`œ\×pNH\è\àÁƒZ¸p\á¤3f¼<q\â\Ä}uuu‡oº\é¦nm8ªªª7®küø\ñ;\'Mš´¨¦¦\æZY\äs\0\0;ú\í*B¿=Hp\r\ç4€«W¯®š2eJKee¥\'Aß›?¾·n\İ:oÛ¶m^GG‡§\ôQ\×\çuzmmmwEE\Å;ş—„B¡‹\Ìu\0\ìBè·«ı\ö  Á5œ\Ó\0¢6l\Ø0\äşû\ï\ß3z\ôh\ï±\Ç\ó\Ú\ÛÛ½­[·z\÷\Ş{¯7l\Ø0\ï²\Ë.\ó.¼\ğBm4Âú»>¯\Óu>_—“\ğ´ººúÑ’’’›\Û\0\0Ø\ĞoWú\íA@‚k8§\ò<\ï´Å‹/Ô°?g\Î\ïÀ\Ş\ìÙ³½K.¹$î«ªª¼E‹ykÖ¬\ñv\í\Ú¾Ò¯ú»>¯\Óu>_—\Ó\åu=\å\å\åo1\â\Û\æ\ö\0\0ùGè·«ı\ö  Á5œ\Ó@ÀI~ÿÀ¼y\ó¶\Ür\Ë-^ss³\÷øã‡\ÃûUW]\å­\\¹2\ğ“¥\ó\ërº¼®G\×7z\ô\è\Ã\Í\í\0\ò‹\ĞoWú\íA@‚k8§\0“œşiÓ¦uN˜0!üıšš\Z\ï\â‹/\ö|\òI3Ï§D—\×\õ\èút½uuuo9r®¹}\0@ş444t>|8&|R¹/9{$\ô1\òƒ€\×pN%\Ùü´y\ó\æmş!C†xW_}µ·ÿ~3Ã§E×£\ë\Ó\õ\êúÇw ´´t¼¹\0€ühjjÚ³w\ïŞ˜\0J\å¾v\ì\Ø\ñk	ı/š\ÇùA@‚k8§€\Ò\Ï\ğ\ßz\ë­\á@>t\èP½ùwüøq3»ˆ®O×«\ë\×\íŒ\Z5ªK\Z\á\æ¾\0\0r¯±±\ñº+Vlmm\İ\Çÿü”ü\İ[·o\ßş°şRC\Íc„ü  Á5œ\Ó@\0­_¿~¨Ş´O?s¯oÁ\×+\ò™üº^]¿nG·7bÄˆÎ‚‚‚\ó\Ì}\0\äM½\Â,\õ~¦œ\Êy\é\ß]ÿş~‹\à\Z\Îi €\ôkù\ô\îúz³=ı\ì}o\é\×\à¾n\İ:\ï7¿ùMø\ëù\ônıZú\óüù\ó\Ã\ÓúúO]¿nG·7s\æ\Ì=\Ò\ğL5\÷	\0\0À$¸†s\Z˜Õ«WW\éUş\Î\Î\Î\ğ]\ö/^lf\ô“\ìŞ½\Û?~¼w\×]wy,_­\ä‘G¼‡z\È{ù\å—\Ã?\ßy\ç\Şw\Ü7İnO·[VVv0\n]d\î\0\0@¾\à\Z\Îi `¦L™Ò¢\á}\ö\ì\Ù\á¯\×K¤»»;|e^¯\è¯]»\ö¤izu_«7ıº¾\Ê\Ê\Ê\ğ2ºl<º=\İ\îÜ¹s·I\ã3\Ë\Ü7\0\0€|# Á5œ\Ó@€tuuı©†ø¶¶¶\ğU\÷gŸ}\Ö\Ì\åQ\Z\Şo¿ıv¯½½İœ\ämÜ¸1\\&W—\Ñe\ã\Ñ\í\éveû\ÇC¡\Ğ\ŞúúúS\Í}\0\0\È\'\\\Ã9\rˆ„\ñ‰uuuŞ–-[¼.¸ \áy}›¾ş\ç€\Şq?U\Zü\õŠ¼·ú\ë\öt»ºıQ£Fm—\èrs\0\0\ò‰€\×pN2}ú\ôW\ômù\÷\İwŸw\ó\Í7›™<LoÈ§Ÿ\Ï\æ™g\ÌIQ\ñ\Ş\Şß›¾\Õ_\×\ï\æ~º]\İş<\ğ’4@?1\÷\0\0 ŸHp\r\ç4 \'NÜ§w\Ú>|xø¦|\ñ\èt½1__úıJ×¡\ë2\évuûMMMŠŠŠšû\0\0O$¸†s\Z\Ú\Ú\Ú\Ãz\÷ıK/½4\æ\æ|\Z\æ}\ôQ\ó\é“$ú\õ®şú&İ®n\ó\æ\Íú\öş5\æ>\0\0\ä	®\áœ¤ªªª{ÿşı\Şù\çŸïµ´´˜y<\ì\Ş{\ï\õ6l\Ø`>\õê«¯†ß¢\ÓM7y¯¼\òŠ99J¿\ÎO\×e\Ò\í\ê\ö;::I\Ôb\î#\0\0@>\à\Z\Îi @\Ê\ÊÊ¼cÇygy¦\÷\Î;\ï˜y<Lo\à\÷\ĞC…¯\äÇ»CMM§\r‡–\óª¿.£¿\ë:t]&İ®n_\ö£K\Öq\Ä\ÜG\0\0€|\" Á5œ\Ó@€TVV\ë\ïJ¡üø\ñ\ÑĞ¯WüS\rı‘+ı\í\í\í\àJ?\0\0°\r	®\áœd\ìØ±]ı}¦_ß’¯o\ÍO\äµ\×^½\â¯o\õO$\Ñ\Ûû#Ÿ\éß´i\ÓZ>\Ó\0\0lC@‚k8§\0™0aÂ®ş\îŞ¯W\é\õ&|•\èF~‘»\÷/[¶¬»\÷\0\0\Û\à\Z\Îi @\ê\ë\ëi¨¿\ï¾û\ÂoÍ\'™¯\ìKF¢¯\ì\Ó\í\ê\ö\'O¼@\Z Ÿ˜û\0\0O$¸†s\Zºººkjkk»·l\Ù\â]pÁ^ww·™É½\ãÇ{w\Üq‡·r\åJsR\ÒtY]‡®«7İnW¶<\nm\èrs\0\0\ò‰€\×pNR__\ê¨Q£\Şikk\ó.¹\ä\ï\ÙgŸ=)”G\ìŞ½Û«¬¬Ô›í™“ú¥\Ëè²º“nO·+Ó“Æ§U\÷\Ç\ÜG\0\0€|\" Á5œ\Ó@À\Ô\Ô\Ô,\Ñ\Ï\ÕÏ=Û»êª«\Ì\\\õø\ã{·\ß~{\Ü\à¯w\èwgW—\Ñe\ã\Ñ\í\év\ï¾ûî‡¥\ñ™e\î\0\0@¾\à\Z\Îi `B¡\ĞEG;;;\ÃW\İ/^lf\ó0}+¾†w½jo¾\Õ\ßüš>¥\óè¼ºL¼\r\èvt{\í\í\ík¤\ái\Óı0\÷\r\0\0 \ßHp\r\ç4@\Õ\ÕÕÎ™3\'\Ğ/¾øboÿşıfFÒ·\é\ë\ç\ó\õ\Æ|zG~ı*¾‡z(\\ú³>§\Ótxo\éWº~İnO¶ıˆ4<S\Í}\0\0°	®\áœ¨  \à¼\ò\òò·›››½šš\Z\ïê«¯¹\é^o:M\ïÄ¯_Áw\ï½\÷zUUU\áÒŸ\õŠ¿NK´¼>¯\ë\×\í¬]»V\ß\Öß¦\Û7\÷	\0\0À$¸†s\Z¨#F|»²²\òpGG‡7t\èP¯¢¢\"apO—®O×«\ëomm}F\ZœR\Ã\Í}\0\0°	®\áœLùÄººº·5ø2$|E¾¯·ú§B×£\ë\Ó\õ¶µµ½\n…Ö”””\Ün\î\0\0€MHp\r\ç4p#Gœ;nÜ¸\Zü\õ-øú\Ùû\'Ÿ|\Ò\Ì\ğ)\Ñ\åu=º¾\Ö\Ö\Ö\Zø¥±™mn\0\0À6$¸†s\ZÀ \Ò\Ò\Ò\ñ]ú½Ù\Şe_¿^Ï¼kt~]N—\×\õ\ô|†\'Wø\0€_\à\Z\Îi\0aúYû#FtÎœ9sÏ¼Ù³g‡\Ãû…^¾iß¢E‹¼5k\Öx»v\í\n|}\Ô\ß\õy®\ó\éüºÜ¾}û^\ì¹KŸ\á\0\0~B@‚k8§D•””|X\Z…©eee|\ğÁm\í\í\íÇ¶n\İ\Z¾Kÿ°aÃ¼\Ë.»,\îe\Ö\ğ£ş®\Ï\ët™\ïxKK\Ës\'Nß¡_×£\ë3·\0\0`3\\\Ã9\r F(ºH\Z‡Y\Ú\Ûn¼\ñ\Æ<\ğÀú¦¦¦\r›7oŞ¹oß¾7=\Ï;\Ô\Ö\ÖÖ¼q\ãÆµK\Å\äÉ“\È2d™7t9]\Ş\\\'\0\0€\à\Z\Îi\0}9¥°°\ğ\ï%üÿ¸¨¨h¡4/J\íÒ†£\ç\ñE}^§\ë|:¿¹\0\0\0?! Á5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§¤Œ†\0\0¸Šq\\\Ã9\r e4\0\0ÀUŒs\à\Z\Îi\0)£\á\0\0\0®bœ\×pNH\r\0\0p\ã¸†s\Z@\Êh8\0\0€«\çÀ5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§¤Œ†\0\0¸Šq\\\Ã9\r e4\0\0ÀUŒs\à\Z\Îi\0)£\á\0\0\0®bœ\×pNH\r\0\0p\ã¸†s\Z@\Êh8\0\0€«\çÀ5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§¤Œ†\0\0¸Šq\\\Ã9\r e4\0\0ÀUŒs\à\Z\Îi\0)£\á\0\0\0®bœ\×pNH\r\0\0p\ã¸†s\Z@\Êh8\0\0€«\çÀ5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§¤Œ†\0\0¸Šq\\\Ã9\r e4\0\0ÀUŒs\à\Z\Îi\0)£\á\0\0\0®bœ\×pNH\r\0\0p\ã¸†s\Z@\Êh8\0\0€«\çÀ5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§¤Œ†\0\0¸Šq\\\Ã9\r e4\0\0ÀUŒs\à\Z\Îi\0)£\á\0\0\0®bœ\×pNH\r\0\0p\ã¸†s\Z@\Êh8\0\0€«\çÀ5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§Diƒl™\Ë\0\0¸€q\\\Ã9\r Š\0\0\ã!¸†s\Z@\r\0\0:\ÆCp\r\ç4€(\Z\0\0tŒ‡\à\Z\Îi\0Q4\0\0 \èÁ5œ\Ó\0¢h\0\0@\Ğ1‚k8§D\Ñ \0\0€ c<\×pNˆ¢A\0\0\0A\Çx®\áœEƒ\0\0\0‚\ñ\\\Ã9\r Š\0\0\ã!¸†s\Z@\r\0\0:\ÆCp\r\ç4€(\Z\0\0tŒ‡\à\Z\Îi\0Q4\0\0 \èÁ5œ\Ó\0¢h\0\0@\Ğ1‚k8§D\Ñ \0\0€ c<\×pNˆ¢A\0\0\0A\Çx®\áœEƒ\0\0\0‚\ñ\\\Ã9\r Š\0\0\ã!¸†s\Z@\r\0\Ø\É\ó¼3š››]µj\ÕÑ††o\éÒ¥TKş\î\İMMM{\Z¯3\Ü\Âx®\áœEƒ\0\0v’À?_§·w\ï^\ï\ğ\á\Ã\Ş;\ï¼C\å¸\ô\ï®ÿ+V\\ºt\éP\óÁŒ‡\à\Z\Îi\0Q4\0`\'½Â¯\Ó¢Tî«µµuŸ„ş\Ícw0‚k8§D\Ñ \0€\ô-ı\\á·£\ô8H\è?b#¸ƒ\ñ\\\Ã9\r Š\0ì¤Ÿ)7\Ã\'•¿\Ò\ãa#¸ƒ\ñ\\\Ã9\r Š\0\ì”l\è\ó@«×¼\æ\Ş\Æe“Â¥?\ës\æ|\ÔÀŠ\Ğ\ï6\ÆCp\r\ç4€(\Z\0°S2¡ÿPg‹\÷\ê’;½—Ÿ¨9©\ô9f\ÎO¥_„~·1‚k8§D\Ñ \0€’	ı»6.Œ	ü‘Ú½qQ\ÌüTúE\èw\ã!¸†s\Z@\r\0\Ø)™\Ğÿû\å\÷Ä„ıH\é4s~*ı\"\ô»\ñ\\\Ã9\r Š\0\ì”L\èui}LØ”N3\ç§\Ò/B¿\ÛÁ5œ\Ó\0¢hÜ·gÏ\İv\Ûm\ë?\ó™\Ï=\ãŒ3\ôxS9ª\÷¼\ç=İŸø\Ä\'\ö\ä#™,¿Ÿ6H¡ß®\"\ô»\ñ\\\Ã9\r ŠÁm\Zø¯¸âŠ£\ßúÖ·¼5kÖ„®\Èı{\ë\ßı\Úk¯=z\ö\Ùg??ˆ\à$ú\õnıfØ”N3\ç§\Ò/B¿\ÛÁ5œ\Ó\0¢h\Ü\öı\ïıĞ¡C\Í,Š<ø\ò—¿\Ü\"‡¤\Ö<F@\"É„ş­\ÏÍˆ	û‘\Òi\æüTúE\èw\ã!¸†s\Z@\r‚\Û>\÷¹\Ï\Õ+\ÍÈ¿\çŸ¾M\É\Z\ó‰$ú;Z6x¯>ı£˜À¯\Ï\é4s~*ı\"\ô»\ñ\\\Ã9\r ŠÁmgyfx°Šü\Ó\ã ‡\äˆyŒ€D’	ıZ¯¯ú\õ9s>j`E\èw\ã!¸†s\Z(y\ñ/\× ŸZn._3³\'\òH‡y€€D’\nıGx[ŸŸú\õ93?•vú\İF@‚k8§€’ÿ\à8!ß¬Á\ær\ğ53w\"\ôx˜H¤¿\Ğ¨³\Å\Û\ò\Ü\ô˜À)¦\ó˜\ËQ\é¡\ßm$¸†s\Z0½’\'\ès•\ß]f\îŒK\ç3™\×\ó·’’0\ô9\âµnm\ô^yê˜ o–Î£\ór\Õ\àE\èw	®\áœL¯\ä\Ç	û\\\åw—™;û4}ú\ôp06l˜9	 [\ó\0‰\Äıı]\İOT\\\õxú\İF@‚k8§€\Ó+úqÿrs>8ÁÌ	uww{ÿø\Ç\ÃÁ\ôw¿û9 [\ó\0‰\Äı\É\\\İOTº¬¹>*ù\"\ô»€\×pN§W\ô\ã„şÁ\æ|p‚™;z\á…Â¡\ô¯ÿú¯\ÍI\ÈıûšH$^\è7ƒ|ªe®J¾ın# Á5œ\Ó\0Ì«ı\\\åw—™;º\ó\Î;Ã¡tÜ¸q\æ$dˆş}\Í$/\ôSù+B¿\ÛHp\r\ç4\0\ójÿ`s:œa\æÎ„®¹\æšp(}ú\é§\ÍI\ÈıûšH„\ĞoWú\İF@‚k8§„E®\ö›\Ï\Ã)f\îL\è£ıh8”¶´´˜“!ú\÷5¡ß®\"\ô»\ñ\\\Ã9\r ,rµ\ß|N1sgBgŸ}v8”=zÔœ„Ñ¿¯y€€Dıv¡\ßmŒ‡\à\Z\Îi\0W\ö4Wš\à3w\"\ôx˜H„\ĞoWú\İF@‚k8§(\rú{¥\Æ\õ<ü\İd\æN\ä‘\ó\0‰ú\í*B¿\ÛHp\r\ç4€H\à}\ów¸\ÃÌ\È#=\æ!\ô\ÛU„~·\à\Z\Îi \ØüD\Ï\Ã\ß\ÌÜ‰<\Ò\ãa  B¿]E\èw	®\áœ‚«¿`\ß\ßtø™;‘Gz<\Ì$Bè·«ın# Á5œ\Ó@0%è“ş`\æN\ä‘\ó\0‰ú\í*B¿\ÛHp\r\ç4<©ùTç‡½\ÌÜ‰<\Ò\ãa  B¿]E\èw	®\áœ‚%\İ\0Ÿ\îr¾r\ğ\àÁ-\\¸pÒŒ3^8qâ¾ººº\Ã7\İtS·6”UUU\ÇÆ\×5~üø“&MZTSSs­,rŠ¹Ë™¹y¤\Ç\Ã<@6À\ë\Ãj„~»Š\Ğ\ïiÃ–k;\ÖO-7—ü„\Ğ\Ç@ƒû@—·\Ö\êÕ««¦L™\ÒRYY\éI\ñ\æÏŸ\ï­[·\ÎÛ¶m›\×\Ñ\Ñhú¨¿\ë\ó:½¶¶¶»¢¢\â	7KB¡\ĞE\æ:-e\ÄN\ä“\ó\0\Ù(@¯«ú\í*B¿;$\r\ò\Í\Zl.ø	¡†L\öL­\Ç\n6lrÿı\÷\ï=z´\÷\Øcy\í\í\í\ŞÖ­[½{\ï½\×6l˜w\Ùe—y^xa8œ\é£ş®\Ï\ëtO\ç\×\å$\Ü­®®~´¤¤\ä\Ã\æ6,c\ÄN\ä“\ó\0\Ù$€¯«ú\í*B¿[\ôJ~œ \ÏU~8ƒ\Ğ¸/\ÓA=\Ó\ë\Ë9\É[§-^¼x¡†™9s\æx\ğfÏ\í]r\É%\á\ğRUU\å-Z´\È[³f·k×®p@\ÓGı]Ÿ\×\é:ŸÎ¯\Ë\é\òº\ò\ò\ò·GŒ\ñms{99u\"¯\ôx˜\È^p_y\Ñ6›Ï›ıv¡\ß-ú\ZŒ\ö¹\Êgú·e+ gk½Y\'ù\ä\ó\æ\Í\Ûr\Ë-·x\Í\Í\Í\Ş\ã?\'W]u•·r\åÊ“BYt~]N—\×\õ\èú$(®¨¨˜hn\×\æ?y¤\Ç\Ã<@ù\æû\õ‘½¯&\ö.ıv¡\ß==¯A3\ğ/7\çüˆ\Ğ¸+\ÛÁ<\Û\ë\Ï8\É!˜6mZ\ç„	ÂŸA®©©\ñ.¾øb\ï\É\'Ÿ4\óJJty]®O\×[WW\÷\öÈ‘#\çšÛ·€¹\ë\È#=\æ\Ê\'\×G^\Ä\ñ\Â?¡ß®\"\ô»G_wq^ƒ\Íù\0?\"\ôn\ê\È\õ\Ú7\ô<D¼\õø&øK\ö8mŞ¼y[#fÈ!\Ş\ÕW_\í\íß¿\ß\Ì(i\Ñ\õ\èút½ºşq\ã\Æ(--o\îG™»<\Ò\ãa |\ñx}\äMœ7üú\í*B¿›z^w\Ñ× 9\ğ+B?\à3\ğÿ|Ğ‰p¡\éÿ¾\Ö\ã‹à¯ŸQ¾\õ\Ö[Ãc\èĞ¡zs1\ïø\ñ\ãf6]Ÿ®W×¯\Û5jT—4²\Ã\Í}\É#s—‘Gz<\Ì”/¼>\ò\'N\Ø7+ş	ıv¡\ßMúZ\ë\õ\ÚlNüJ\Ïi\ó9\0şe\ğ‘2{2zşH<i\Ø\íZeıú\õC\õ¦dú™b}‹±^q\Ìt ‰\Ğ\õ\êúu;º½#Ftœg\îS˜»‹<\Ò\ãa |\à\õ_œ\ğ\÷2ƒ\'•¿\"\ô»«¸\çj¿ù<\àgœÓ€;\â\ïx=•\àoù\Ù=Ï›\âm\ß\núµcz\÷p½™˜~¶¸¿·,k0\Ñ\ïÿ\Ío~şú1½¹–şù\ò¾B‘®_·£Û›9s\æih§šû”\'\æ®\"\ôx˜(R}}”Å¯“\äj€dû^¥Ácpd>®\ô\ÛU„~7”——@^g\ß-**š&\rR¯\Ë\ÏGz^ƒo\ë\ïú|\Ï\ô\ï\êü\æ:\0?\Ğs\Ú|€ÿ\ô¸\ã\÷d‚¼\åşˆ¾\ö#/V¯^]¥W1;;;\Ãw_¼x±™AN²{\÷noüø\ñ\Ş]w\İ\å-X° |5\ò‘G\ñz\è!\ï\å—_ÿ|\çwzw\ÜqGx\ŞDt;º=\İnYY\ÙÁP(t‘¹oy`\î&\òH‡y€r-\Õ\×G¦Xúú8I®HFĞ	û~ı¯¿şz\ï~$\\\æ<~+B¿¿•””|]^g¤Ş‰\ó\Z\ì«tşº¼¹NÀfzşš\Ï\ğ—d‚v¼\0\ßW\ğ7?\"™ıÉ™)S¦´hx\×\ï×¯K¤»»;|\åQ¯\è¯]»\ö¤izu_«7ı:²\Ê\Ê\Ê\ğ2ºl<º=\İ\îÜ¹s·Ic;\ËÜ·<0wy¤\Ç\Ã<@¹–\ì\ë#,|}œ$W¤^a\"nØ\ğs\è\×ÿ,•‚wı\õ\×\ÇL\ókúıI\Âú\ß\É\ë\ì…8a>zA\×gn°‘³\æs\0ü#•€/\È\Çş\ñ\æK6\ğG¤²_Y\Ó\Õ\Õ\õ§\Z\â\Û\Ú\Ú\ÂWŸ}\öY3wDix¿ı\öÛ½\ö\övs’·q\ã\Æp™t^]F—G·§Û•\í…B{\ë\ë\ëO5\÷1\Ç\Ì]D\é\ñ0P.¥\òú\È_\'\É\Õ\0©¸Ÿ°\á\ç\Ğ?v\ì\Ø\ğùşÓŸş4fš_‹\Ğ\ï/\å\å\ågıL^k\İfx3fŒ\÷\Ë_ş2üÑ½mÛ¶…o4ª\ôQ\×\çuº\Îg.«\ë\Ó\õ\êú\Ím6\ÉUŸ \ó\Ò	\Ö\ñ}\ï\àozª?\"ıKJ²ƒd	\ã\ë\ê\ê¼-[¶x\\pA\Â+\òú6}\r?‘>\Zü\õŠ¼·ú\ë\öt»ºıQ£Fm—}¾\Ü\Ü\ÇLH\ö\ï1ˆ\Ğo=\æÊ„dÏ‡d_Ù’«\×Gºl ù9\ôÿ\Ã?üCø|ÿĞ‡>\äşùŞ²e\Ëb\æ\ñ[=ı\ô\ÓVHL^\Ë)))y¹wX…BŞ¬Y³\â\ö\İ}\Ñùu9]¾\÷ú¤^\Ò\í˜\Ûla[Ÿ 9	\Ô\ñ‚½ş®W\Ù\Ì\ç\Ó\rü\ÙÏ„zu²}†›\éÓ§¿¢oË¿\ï¾û¼›o¾\Ù\ì»\Ã\ô†|úùügyÆœ\ï\íı½\é[ıu\ñn\î§\Û\Õ\í?\ğÀ: ø‰¹™\ì\ßc¡\ß*z<\Ì”	ÉÉ¼>²-¯t\Ù6@\òs\èÿ\Ü\ç>\çM:5\ÜV\Ê?\ÅûÔ§>3Ÿ\ê\ğ\á\Ãú\Íú\Z\Û)\õ¬\Ô\ÃzşJ°¼¹¨¨\è[\òønøf9Ÿ”j\é\Õ.z\÷\Üs·k\×.³9J‰.¯\ë\é½^©]º=s\0\ØÖ§\è_&‚t¼\à¿\Ùø} ?\"û{£“Mn&Nœ¸Oß–7|ø\ğ\ğMù\â\Ñ\ézc¾¾\ôú•®C\×e\Ò\í\ê\ö›šš6\È`p¡¹™\ì\ßc¡\ß*z<\Ì”	ÉÉ¼>²-¯t\é\ß\Î|.Ÿüú#\õ\Ö[o…\Ïû\÷¾\÷½1\ÓüVK–,\ñ\n\n\n>*!\ï\n9wK\å\ñv9gf\Ê\ÏO\È\ã+R\å¹.ı¹ç¹™:\Î+?%\n}l\ğ\àÁ§›\Ç™S|\â\n4\ğ\ë\Õù…š\ÍĞ€\èúŒ«ş»t»\æ¾\0ùf[Ÿ o™\Ğ\ñ‚¦D&\÷;^¨‰njkk\ë\İ\÷/½\ôÒ˜›\óEh˜\ô\ÑGÍ§O’L\è\×U\éWü™t»ºıÍ›7\ëÛ—\×\ôúgdLœ¿CÜ¿\Ç B¿U\ôx\ô:6\ç<ˆ{>$\óúÈ¶\\¼>Ò¥3\ó¹|\òs\è¿ü\òË½³\Ï>\Ûû\ío>\ï¿úÕ¯\Æ\Ì\ã·J\æ3ı\ï/--ı´œKC\õ]\0\òø“\â\ï\n\Ğw\è»\ôN\ğ¼[ \ô3\ö½\ß\Òÿ½\ï}\Ï[¿~½\Ùe„®W\×Ù–\ÔK|\Æ¶±­OXFƒs}K¿y…_\Ï\Æ\rµ2¶ÿ½:\ÖD7UUU\İú\àúÒ––³Ÿ»\÷\Ş{½\r6˜OG½ú\ê«\á· \ßt\ÓM\Ş+¯¼bNÒ¯\ó\Óu™t»ºıC²O-\æ¿%\âüû\Í\nÿ=ú­¢\Ç\Ã8”\çø›>’y}d[.^\éÒ¿•ù\\>ù9\ô744x\ó7\ãs\Î9\áomĞ¯\ğ3\ç\ñ[%úû£Wúy·@v\ôÜ´/\Ü\æ\é•ølş]\ï+şº}sŸ€|²­O\ß{\Ì\ã\Ì	\Ğ×•şxw\õÏ„q‘1UVV\æ;v\Ì;\ó\Ì3Ãƒ´x\ô~=\ôPøJ~¼;\ô\×\Ô\ÔD×§Áß¼\ê¯\Ë\è\ïº]—I·«Û—ıĞ[\Ì>\æ²ú­bŸ|T¯l³\é\õ¯\Ì,Ÿüú]¬L„şd\ğnÔ•œøZ¾w#¯\ãE‹™MOV\èvzµ\ï\ê~˜ûä‹—\æs\0ì”±+\åƒ\â~\óŠ¦ƒ\Æ\ö¿W§j\Ö\ò\â^o_®¬¬<\Öß•\ÌşBÿø\ñ\ã£\ë\×+ş©†şÈ•\Ì\ö\ö\ö?g\éJfœ¿CÜ¿\Ç B¿U\ôx\ô:6\ç<ˆ{>$\óúÈ¶\\¼>\\Aè·«rúû”wÈ¿\ñ4\ó¹D\ä\ß\õB¤\İÓ›\í\å’qs¿\Ì}\òE\ÏI\ó9\0\ö\ÊDpø\õ3ü\ñ\îŞŸ©\àŸ‰ı\êÕ¡\Æ\r3cÇ\í\ê\ï3\Ëú–|}k~\"¯½\öZ8ø\ë}«\"‰\Ş\Şù\Ì\ò¦M›\Ög\é3\Ë\Éş=ú­¢\Ç\Ã<@™\ìù\Ì\ë#\Ûr\ñúp¡ß®²%\ô\'Ã…w\ô\ì\÷tù·ü‰9­·\Â\ÂÂ«#mŸ¾\İ~ w\éO•n¯\÷\Ûüu\Ì}\òA\ÏG\ó9\0vH€Nø#Á>\Ş\ôÿ\ìo\\‘Î´8A˜‰˜0aÂ®ş\îN®W\é\õ&|•\èF~‘»“/[¶¬¡(Kw\'O\ö\ï1ˆ\Ğo=\æÊ„dÏ‡d^Ù–‹×‡+ıv•ŸBü\ğn\Ù\Ö==\í\Ú\ë}i™¾ \ÒÎš5\ËlrrB·Û«^`\î#z>š\Ï°_:A:^ w—şx\ó¥ü\Ó\Ù\Ï~\÷f\"\ê\ë\ë\õ\÷=\ä\É|e_2}e_\ä{\È\'O¬‘¬|y²A„~«\è\ñ0P&${>$\óúÈ¶\\¼>\\Aè·«\\\nı\É\È\÷»d¿*şc\Ö\åı—\îS\ïyB¡\Ğ{\ö#<O®¯\òG\èv{\í\ë;º_½\÷\È=\Í\ç\0øC*:^ø#\âÍŸj\ğOeÿ²¢®®\îš\Ú\Ú\Ú\î-[¶x\\p\×\İ\İm\ö\Í\Ş\ñ\ãÇ½;\î¸\Ã[¹r¥9)iº¬®C\×Õ›nO·+\Û?.şip/7\÷1\ÇN\Ú?\ä—\ó\0\åR2¯l²\ğ\õa5B¿]´\ĞßŸl¿[@\æy®W\Ô.ıO…^\ó|72mÌ˜1f““Sºı^ûù\İ\Şÿ–\Şd\Ú`©\å\æ\ó@¦\é¹h>À?’	\Ö\ñ|_?\"\Şr\Éÿd\ö+\ë\ê\ë\ëO5j\Ô;mmm\Ş%—\\\â=û\ì³f¿¶{\÷n¯²²Ro&fN\ê—.£\Ë\ê:Lº=İ®L\ÓÁJ«î¹9f\î\"\òH‡y€r)\Ù\×G¶Xøú°\Z¡ß®\"\ô§n \ïŸ\ß\Ğ\Ğ¯d‡d\İ’Ÿ§GÓ›\ë\æ“n¿\×>N7ÿ\Å=a?29\È4\Î3Àÿú\n\Ø\ñ‚{2?\"\Ş\òıÿ¾\ö\'\çjjj–\è\ç†gÏş~\èDüq\ï\ö\Ûoü\õı\ñ\î\ì¯\ó\ê2ºl<º=\İ\î\İwß­ƒ—Y\æ¾å¹‹\È#=\æÊµd_\Ù`\á\ë\Ãj„~»ŠĞŸy}¼[`±<v\÷\n\Ñ\ñj¿Ì¿9\ò{¾nN\Z¡Û\ì‹\ìWc\ä\ßXl„ıH\õş;\0\ÙÀy¸!QĞ¾aPú?\"^\ğy\Ò”h?\ò&\n]TQQq´³³3|Uq\ñ\â\Åf\ß¦o5\Ö\ğ®W\íÍ·ú›_Ó§tW—‰\÷¶hİn¯½½}4´mº\æ¾å¹›\È#=\æÊµd_™f\é\ë\Ãj„~»ŠĞŸ;\ÒFü¹’z[j·Ô»‘\ç\ô›I\òI·\ßkÿ\ô\İq\Ã~¤\Ì3iœg€;\â\îŞ=À‘\Ìz\âm\ß\n\Õ\ÕÕÎ™3\'\Ğ/¾øbO¿›<}›¾~>_oÌ§w\ä×¯\âÓ·\éi\é\ÏúœN\Óyâ½¥_\éúu;º=\Ù\ö#\Ò\ĞN5\÷)O\Ì]E\é\ñ0P>¤\òú\È‹_V#\ô\ÛU„ş\Ü),,ü3$\÷\Ü\àR_‹|4¨\ç¹\ğ\ôl·cı\Ñ\í\÷\Ú\ß\ã\æş›eş›L\ã<\Ü/xk@\×+\ó\ñ‚z*úZO¼\íZ£  \à¼\ò\ò\ò·\õ\Şkjj¼«¯¾:\æ¦{½\é4½¿~ß½\÷\Ş\ëUUU…K\Ö+ş:-\Ñ\òú¼®_·³v\íZ}\Ûr›n\ßÜ§<1wy¤\Ç\Ã<@ù\ê\ëc ,}X\ĞoWúsG\Âüˆp|Lo(¿—H»q¶9_\ïp}\ì\Ø1³ù\É)İ¾\ì)*\ße¾f\0ø[®x®·—–#F|»²²\òpGG‡7t\èP¯¢¢\"\ãÁF×§\ë\Õ\õ·¶¶>#\r\ì©\á\æ¾ä‘¹\Ë\È#=\æ\Ê^\ö#\ô\ÛU„şÜ‘ ÿ\Éùÿ\\VV\ög\æ´\Şl½\Òß³_¼½\0q¹\n\â¹\ÚNFH\à˜XWW\÷¶›!C†„¯8fj` \ë\Ñ\õ\éz\Û\Ú\Ú^…Bk\ô&D\æ>üÿ\ö\î/\Ä\Î2¿x\ËJ¨¡/\rHQ\è•WŠHÁ\æ¢\Å±^(\Æ\Ä?DD\ô.¹p½h)]‹e\ñr»-š«Ê‚\Ë\Òb.¢%k³3\ÜH²’\Ö\ZŠ\rf\ógšui&\ÛnŞ¾¿\Ù9\Û\é\óÎ˜“7\ïœ\÷9\ÏûùÀ—q\Îy\Ï{N\ÎÃœ\çùúœ9Ó³\ôaÓ£t€ú\ä\ç#oJ^Qú\ó\ó\ä¯w~©D\ç\ö;ı+\ãª\å\å¿\0®\Çz\ò\õ>ÿºx\ö\Ùg\ğ\ò\Ë/_Šbo1\ß-Ş»wo:__—¸}œ\'Î·°°0…¦\Ä\ã³r“>tz\ã‘P\ßş\ó‘5¥?¯(ıù‰O\É•\è\\?½$-ÿ\é\õ\0p=Ö«˜¯\×y\'â©§ú\ó\çŸş«ø?\ñ\ñab\ñ)\â\ñ\ç\Ã\ÒO\í¿–8>n·\ó,ÿ\ò\ç\ï`¦ÿz\ã‘Pü\ó‘5¥?¯(ıù©__¾?*\Ñ\ñ\á»}Šû=–x\\\éc•ÿ\ôr\0¸^]\ô®\Ï×‹z’ıÓ§Ÿ~ú‹7\Şx\ãÌ¥K—–ş^x”“;\î¸c\éCû\Ş}\÷\İ\êĞ¡C\ÕÉ“\'—&\ğø\Z\ß\Ç\åq}\Ç\Ç\í.\\¸ppùS\È\Ï\Åy\Óû\ÊH²,¡O1\é\0\åb ?YSú\óŠÒŸŸú\õ\å\ñQ\ÑŞ¹sg\òŠ;Y;vì¸º¢\ô?>V\0X]\õ®Î“…­[·\Ş^O\Æ\ß{\æ™g¾|ë­·>;şü=zt\éSúy\ä‘\ê¾û\î[*/\õ¡K_\ãû¸<®¯ûŸÓ§O\ğê«¯.}yœ\'Î—\ŞGf\Òu	=Š\ñH(\'üùÈšÒŸW”şülÛ¶\í\÷\ê×š+£²=úŸ’“\÷»¢\ğ_‰Ç•>V\0X/7Z\Øo\ô\öÙª\'\äß¯\'\æ]u)9\÷\Â/œx\ó\Í7\Ï\Ï\Ïüé§Ÿ~~\áÂ…_\Ôsø\×\çÎ;v\äÈ‘Ÿ\Õ½}¯½\ö\Ú\îú6×·ùÏ¸]\Ü>=g¦Òµ	=Š\ñH(GúùÈšÒŸW”ş<Õ¯9»G…{×®]\é\Ë\îD\Äı®(ı»\Ó\Ç\0\ë­mqo{»i\ó\ÛO<\ñ\Ä\Ö\å\æ»[¶l\ÙSO\Ö\ëœ\\¸\ã\ëÁ¸<®\ã\âø\ô™K\×&\ô(\Æ# Ì•ş\ó‘5¥?¯(ıyª_{şxT¸·m\Û6\ñ\İş¸¿ú5\ğ7o\íÇ“>F\0˜„\ë-\ğ\×{<ùJ\×\'\ô(\Æ# X‹ÒŸW”ş|\Õeû\ÃQ\é~ı\õ\×Ó—\Şu\÷7º\ïx\éc€I\Z·È{\Ó!]ŸĞ£t€`-J^Qú\óµu\ë\Ö\ê\Âı«Qù„¸Ÿ…ÿW\ñ8\Ò\Ç\0“v­B­\ë™>\é\Z…\Åx¤kQú\óŠÒŸ·-[¶ü\İÊ·ù>|8}	\îTœ\å\Ûú\ãş\Ó\Ç\0}Y«Ø¯u9\Ó-]§Ğ£t€`-J^Qú\ó¶}û\öß©\Ë\÷¿ŒJøs\Ï=·n\Å?\Î[\ß\ßo\ŞY\÷\÷Ÿ>&\0\èSZ\ğ\Ó\ï)GºV¡G1\é\0ÁZ”ş¼¢\ô\ç¯.ß›ü¿]\Ú\ñß³gOúR|C\â|+wø—\ïoSúX\0 £¢ÿ\ò\òW…¿L\éz…\Åx¤kQú\óŠ\Ò?\êBş+‹$>l\ïF?\Õ?nŸ|h\ßR\áûK\0\ä\äbÒŠ¯\é#]·Ğ£t€`-J^Qú§G\ì¼?¹\â­ş‘\Ø\õßµkWu\êÔ©\ô¥ù\Å\ñq»dw\é-ıq?\é}@v–K?\åJ\×/\ô(\Æ# X‹ÒŸW”ş\é¿c¿ü\á~iY¯v\î\ÜY½ı\ö\Û\ÕG}T}\ö\Ùg\ÕÅ‹—^£\ãk|—\Ç\õ;v\ìh\Ü6\Î\ç\õ;ü\0L\r¥¿xI\í¤O1\é\0ÁZfff®^¾|¹Q>e\ò©\Ç\áL]ú\Ó1\"\Ë\Î\ï\ÃU\Ê{›|\è\Ï\ò0ubK/£(i\ï¤G1\é\0ÁZ\æ\ç\çÏœ={¶Q@e\ò9q\âÄ\ê\Ò0#¦G]\Öÿ¤^\ó\ì®se•2ÿM‰\ãw\Ç\í\ÓsÀTPú‹—\öNz\ã‘¬evv\öÑ¹¹¹/.\Ø\ñ\ï\'\õ\ó¾püø\ñÖ…ÿ\ó:§c\Ä\ôÙ¾}û\ï\ÖkŸÇ·l\Ù\ò7\õ×™:?¯3ú\ó{ÿµüı\Ì\ò\õ\Ç\ñ\é9\0`ª(ı\ÅK{\'=Š\ñH¾I\Í\Øa®s%~§\\&x\Ş\ãùWøU¯ƒ6¯\Ø\Õßœ^\0SO\é/^\Ú;\éQŒG:@\0\ô§^\í_Qú\÷§\×À\ÔSú‹—\öNz\ã‘\0ıHvù\í\öP&¥¿xi\ï¤G1\é\0Ğd—\ßn?\0eRú‹—\öNz\ã‘\0“·\Æ.¿\İ~\0Ê£\ô/\í\ô(\Æ# \0&o]~»ı\0”G\é/Û†\r®ÆŸ¢‹‹‹g\ê!YL\Ç€ÉŠüUŠ~š\Í\é\í\0`*)ıe»û\î»/:t(\íŸ\ô`\ïŞ½?®‡\äP:F\0LV\ì\ä¯R\ò\Ó\Ø\í J\Ù6m\Ú\ô\×=\ô\Ğ/\Ó\Ê\Ä]Ü¸q\ãO\ê!y%#\0úg=@±Lr\Åû\Ö\Í7\ßü\Óû\ï¿ÿÔ\Î{«ÿd-...\Ä]ø\÷\Õc»F\ßJ€şYP,“\Ü D\Ñüv\ê\\ù­_˜œL&\ñ|\Ç\óÏ¿\Â)\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(B=¡\íI\í\ZÙŸ\Ş\0 dJ?\0E¨\'´Í«”ü4›\Ó\Û\0”L\é ±“¿JÑ·\Ë\0–\Ò@1b\'•²o—\0,¥€¢Äş*…\ß.?\00HJ?\0EYc·sz\0À(ı\0\'\Ù\í·\Ë\0–\Ò@q’\İş\Í\é\õ\0\0C¡\ôP¤\Ñnz9\0ÀXP¤\Ñnz9\0ÀX\ĞPUÕ†cÇıÃ~933S\íÛ·O&œúy¿:??fvv\ö\Ñt|\0\0Æ¥\ô\ĞPş¬gu\ö\ì\Ù\ê\ò\å\ËÕ•+WdÂ‰\ç=ÿ¹¹¹/\÷\í\Û\÷p:F\0\0\ãPúhˆş(œi•\Égaa\áB]ú¦c\00¥€†xK¿ş<\ãP—ş\ÅtŒ\0\0Æ¡\ô\Ğ¿S–O\é/1\é\0ŒC\é a\Ü\Òÿ‹KÕ±C_ù\É_-%ş;.K“‹\Ò\0´¥\ô\Ğ0N\éÿú‹\Ó\Õ\'\ïÿE\õ¯ÿ\ô\íÿ—¸,®K—\öQú€¶”~\0\Z\Æ)ı\'\ìişQNy·q¼´\Ò\0´¥\ô\Ğ0N\éÿ\÷ı¯7\Êş(q]z¼´\Ò\0´¥\ô\Ğ0N\éÿd\ßw\Ze”¸.=^\ÚG\é\0\ÚRúhPú\óŠ\Ò\0´¥\ô\Ğ0N\éO\ëO\Ëş(q]z¼´\Ò\0´¥\ô\Ğ0N\é?úÁ\ß6\Êş(q]z¼´\Ò\0´¥\ô\Ğ0N\é¿xú\ã\ê“ş\ËF\á\Ë\âº\ôxi¥\0hK\é aœ\Òùù\Ï~\Ğ(ıqYzœ\ÜX”~\0 -¥€†±Jÿ\âbu\ô§o4J\\\×5—\ÖQú€¶”~\0\Z®Uú¿ş\ât\õ|¿QøG‰\ë\â˜\ôv\Ò.J?\0Ğ–\Ò@Ãš¥q±Z8:[ıÛÿ¬Q\ô\Ó\Ä1q¬]ÿ\Ò\0´¥\ôĞ°Z\é¿\Ö\îşZ±\ë\ãQú€¶”~\0\ZV+ı\ã\ìî¯•¸mz>?J?\0Ğ–\Ò@\Ãj¥?-\ò×›\ô|2~”~\0 -¥€†\ÕJ¿\ô¥\0hK\é A\é\Ï+J?\0Ğ–\Ò@ƒÒŸW”~\0 -¥€¥?¯(ı\0@[J?\0\rJ^Qú€¶”~\0\Z”ş¼¢\ô\0m)ı\04(ıyE\é\0\ÚRúhPú\óŠ\Ò\0´¥\ôĞ \ô\ç¥\0hK\é A\é\Ï+J?\0Ğ–\Ò@ƒÒŸW”~\0 -¥€¥?¯(ı\0@[J?\0\rJ^Qú€¶”~\0\Z”ş¼¢\ô\0m)ı\04(ıyE\é\0\ÚRúhPú\óŠ\Ò\0´¥\ôĞ \ô\ç¥\0hK\é A\é\Ï+J?\0Ğ–\Ò@ƒÒŸW”~\0 -¥€¥?¯\Äd-\"\"\"\Ò6\éZ€Sú\óŠ~\0\0\0:3Í¥ÿ«¯¾ª^|\ñ\Å\ê¶\Ûn«\î¼\ó\Î\ê½\÷\Şk3mQú\0\0\è\Ì4—şW^y%\nr\õşû\ïW7n¬|\ğÁ\Æ1\Ó¥\0\0€\ÎLs\é¿ë®»ª\Ûo¿½qù4G\é\0\0 3\Ó\\ú7lØ°Tú\ï¹\çjÓ¦M\Õ\Ü\Ü\\\ã˜i‹\Ò\0\0@g¦¹\ô\ßt\ÓMKo\ïß³g\Ï\Ò\×(ÿ\é1\Ó¥\0\0€\ÎLs\é\İıúŸ°\ô~\ñ\õ–[ni3mQú\0\0\è\Ì4—ş—^zi©\ì¿\ó\Î;K_x\à\Æ1\Ó¥\0\0€\ÎLs\é?ş|\õ\ØcU·\Şzku\ï½\÷V‡n3mQú\0\0\è\Ì4—ş£\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0™™™¹zù\ò\åFù”É§‡3u\é_L\Ç\0\0\0Z™ŸŸ?s\ö\ì\ÙF•\É\çÄ‰?ªKÿÁtŒ\0\0\0 •\Ù\Ù\ÙG\ç\æ\æ¾\\XX¸`Ç¿Ÿ\Ô\Ïû\Â\ñ\ã\ÇXş\Ï\ë<œ\0\0\0´E3v˜\ë\\‰\ß)—‰\'\÷xş~\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0€i\ó¿±\õ Eˆ[…\0\0\0\0IEND®B`‚',1),('12516',1,'flow_mu180u86md3e.bpmn20.xml','12515',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://bpmn.io/schema/bpmn\" id=\"Definitions_1\">\n  <process id=\"flow_mu180u86md3e\" name=\"æµ‹è¯•914\" isExecutable=\"true\">\n    <extensionElements>\n      <flowable:executionListener event=\"start\"></flowable:executionListener>\n    </extensionElements>\n    <startEvent id=\"StartEvent_mu2c2p840\" name=\"å¼€å§‹\"></startEvent>\n    <sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\"></sequenceFlow>\n    <endEvent id=\"EndEvent_mu2c2p8c0\" name=\"ç»“æŸ\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </endEvent>\n    <sequenceFlow id=\"Flow_1qxvbp7\" name=\"xiaoyuyibai\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_15cdzo6\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_1dpq6dk\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_0qm5tc6\"></sequenceFlow>\n    <userTask id=\"Activity_0qm5tc6\" name=\"1\"></userTask>\n    <sequenceFlow id=\"Flow_0icqanz\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_1mzuxt6\"></sequenceFlow>\n    <parallelGateway id=\"Gateway_15cdzo6\"></parallelGateway>\n    <userTask id=\"Activity_1mzuxt6\" name=\"2\"></userTask>\n    <sequenceFlow id=\"Flow_1aqie0c\" sourceRef=\"Activity_0qm5tc6\" targetRef=\"EndEvent_mu2c2p8c0\">\n      <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${tes &gt; 100}]]></conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_016it02\" sourceRef=\"Activity_1mzuxt6\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <exclusiveGateway id=\"Gateway_0a6n8eh\"></exclusiveGateway>\n    <sequenceFlow id=\"Flow_13gvmcu\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_02klewn\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_1kkbmin\" sourceRef=\"Activity_02klewn\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0t3saos\" sourceRef=\"Activity_0ylt45t\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <userTask id=\"Activity_02klewn\" name=\"6\"></userTask>\n    <userTask id=\"Activity_0ylt45t\" name=\"5\"></userTask>\n    <sequenceFlow id=\"Flow_0dizg8q\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Event_093p7s3\"></sequenceFlow>\n    <intermediateCatchEvent id=\"Event_093p7s3\">\n      <timerEventDefinition></timerEventDefinition>\n    </intermediateCatchEvent>\n    <sequenceFlow id=\"Flow_0t5hbag\" sourceRef=\"Event_093p7s3\" targetRef=\"Gateway_0a6n8eh\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0e4xs2a\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Event_0ctoqxr\"></sequenceFlow>\n    <intermediateCatchEvent id=\"Event_0ctoqxr\">\n      <timerEventDefinition></timerEventDefinition>\n    </intermediateCatchEvent>\n    <intermediateThrowEvent id=\"Event_0pivae0\">\n      <messageEventDefinition></messageEventDefinition>\n    </intermediateThrowEvent>\n    <sequenceFlow id=\"Flow_04ilbrs\" sourceRef=\"Event_0pivae0\" targetRef=\"Activity_0ylt45t\"></sequenceFlow>\n    <manualTask id=\"UserTask_mu2c2p880\" name=\"3\"></manualTask>\n    <sequenceFlow id=\"Flow_0qhinx9\" sourceRef=\"Event_0ctoqxr\" targetRef=\"Activity_1jl42bt\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0q59g41\" sourceRef=\"Activity_1jl42bt\" targetRef=\"Event_0pivae0\"></sequenceFlow>\n    <subProcess id=\"Activity_1jl42bt\" name=\"7\">\n      <startEvent id=\"Event_032ztbm\"></startEvent>\n    </subProcess>\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_flow_mu180u86md3e\">\n    <bpmndi:BPMNPlane bpmnElement=\"flow_mu180u86md3e\" id=\"BPMNPlane_flow_mu180u86md3e\">\n      <bpmndi:BPMNShape bpmnElement=\"StartEvent_mu2c2p840\" id=\"BPMNShape_StartEvent_mu2c2p840\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"142.0\" y=\"142.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"149.0\" y=\"185.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"EndEvent_mu2c2p8c0\" id=\"BPMNShape_EndEvent_mu2c2p8c0\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"812.0\" y=\"382.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"778.0\" y=\"393.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0qm5tc6\" id=\"BPMNShape_Activity_0qm5tc6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"780.0\" y=\"90.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_15cdzo6\" id=\"BPMNShape_Gateway_15cdzo6\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"545.0\" y=\"75.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1mzuxt6\" id=\"BPMNShape_Activity_1mzuxt6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"631.0\" y=\"170.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_0a6n8eh\" id=\"BPMNShape_Gateway_0a6n8eh\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"225.0\" y=\"365.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_02klewn\" id=\"BPMNShape_Activity_02klewn\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"200.0\" y=\"500.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0ylt45t\" id=\"BPMNShape_Activity_0ylt45t\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"610.0\" y=\"340.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_093p7s3\" id=\"BPMNShape_Event_093p7s3\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"242.0\" y=\"242.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_0ctoqxr\" id=\"BPMNShape_Event_0ctoqxr\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"302.0\" y=\"372.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_0pivae0\" id=\"BPMNShape_Event_0pivae0\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"502.0\" y=\"372.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"UserTask_mu2c2p880\" id=\"BPMNShape_UserTask_mu2c2p880\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"248.0\" y=\"102.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1jl42bt\" id=\"BPMNShape_Activity_1jl42bt\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"370.0\" y=\"350.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_032ztbm\" id=\"BPMNShape_Event_032ztbm\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"222.0\" y=\"182.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1g1p7by\" id=\"BPMNEdge_Flow_1g1p7by\">\n        <omgdi:waypoint x=\"178.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"142.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"248.0\" y=\"142.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1qxvbp7\" id=\"BPMNEdge_Flow_1qxvbp7\">\n        <omgdi:waypoint x=\"348.0\" y=\"134.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"548.0\" y=\"103.0\"></omgdi:waypoint>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"54.0\" x=\"404.0\" y=\"130.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1dpq6dk\" id=\"BPMNEdge_Flow_1dpq6dk\">\n        <omgdi:waypoint x=\"591.0\" y=\"96.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"720.0\" y=\"70.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"780.0\" y=\"103.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0icqanz\" id=\"BPMNEdge_Flow_0icqanz\">\n        <omgdi:waypoint x=\"570.0\" y=\"125.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"570.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"631.0\" y=\"210.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1aqie0c\" id=\"BPMNEdge_Flow_1aqie0c\">\n        <omgdi:waypoint x=\"880.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"848.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_016it02\" id=\"BPMNEdge_Flow_016it02\">\n        <omgdi:waypoint x=\"731.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"382.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_13gvmcu\" id=\"BPMNEdge_Flow_13gvmcu\">\n        <omgdi:waypoint x=\"250.0\" y=\"415.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"500.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1kkbmin\" id=\"BPMNEdge_Flow_1kkbmin\">\n        <omgdi:waypoint x=\"300.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"418.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0t3saos\" id=\"BPMNEdge_Flow_0t3saos\">\n        <omgdi:waypoint x=\"710.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"812.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0dizg8q\" id=\"BPMNEdge_Flow_0dizg8q\">\n        <omgdi:waypoint x=\"298.0\" y=\"182.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"298.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"260.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"260.0\" y=\"242.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0t5hbag\" id=\"BPMNEdge_Flow_0t5hbag\">\n        <omgdi:waypoint x=\"260.0\" y=\"278.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"260.0\" y=\"322.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"322.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"365.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0e4xs2a\" id=\"BPMNEdge_Flow_0e4xs2a\">\n        <omgdi:waypoint x=\"275.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"302.0\" y=\"390.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_04ilbrs\" id=\"BPMNEdge_Flow_04ilbrs\">\n        <omgdi:waypoint x=\"538.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"574.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"574.0\" y=\"380.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"610.0\" y=\"380.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0qhinx9\" id=\"BPMNEdge_Flow_0qhinx9\">\n        <omgdi:waypoint x=\"338.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"370.0\" y=\"390.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0q59g41\" id=\"BPMNEdge_Flow_0q59g41\">\n        <omgdi:waypoint x=\"470.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"502.0\" y=\"390.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>',0),('12517',1,'flow_mu180u86md3e.flow_mu180u86md3e.png','12515',_binary '‰PNG\r\n\Z\n\0\0\0\rIHDR\0\0ı\0\0N\0\0\0\åg\0\0c\0IDATx^\ì\İ|T\åÿq¼\ÔK\İ\Ú\Ë\ÖıÿÿjÛ­uw»n\ï\Õv×µ+µ¶Z(Ø­\ÍmB0\İ ‰—­¤¤\nÚ€\ì¶\êv—[Ev\éVÀ\Ú*•í‚¢	TD (\n(‘[ˆ$„@PAÀœÿ\ï7d¦á™™df2—\ç<\ç\ó~½~¯I\æ\\\É9\ó<Ï—3sf\Ğ \0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\à¥¥¥\çš\Ï\0\0\0\0\0Ÿ)((8£¸¸øR©ª¢¢¢ÿ’zY~~·¤¤\äÇ„\0\0\0\0\0|b\ô\è\Ñ\ï‘Pÿ	\õ£¥fI­“zK\ê%©ŸË´›¤.+((ø¸„şÿ–\çZ¥n<x\ğ\é\æº\0\0\0\0\0@hÀ…BŸ“\ğ>J‚ûL©5\Z\ğ%\Ôo\Ç_h˜—úryyùY\æ²²\ìgdş§d¾-\òøs:\0\0\0\0\0\È2½¯]\Âù?I8ÿ™<®–zS\êUy~\Ô?Kı]AAÁ\Ù\æ²Ée¿.\ë]/\ë{V\×cN\0\0\0\0d\Ç{\Í\'\à6	\î§I\0ÿ”\ğr©\éR«zş&©y2\í¶\Â\ÂÂ¿/++;\Ç\\v \ê\ë\ëO•u”mì”š/ûq±9\0\0\0\0 s®”\Ú\Û\óiĞ–p}IOØ*\õœÔ¡’’’×¤’Ÿ«¥¾\"\óü‰¹l¶\è\Çd~(\Ûm—\Çi²6\ç\0\0\0\0L$\ğ\ëy$øûœ|	ĞŸ”\Z!aú?$T¯”Ÿ»\äq‹\Ô\Ã\ò\óX©+‡ş>s\Ù|Ğ°\ß\ó\Zş\Ø×½\0\0\0\0\0\É3¯\ğ›¿\Ã~§HPşK	\Ì!	\Ï?•zF~>(\õ©_Ë´š\Â\ÂÂ¯¼\ß\\\Ğ6ú6\Ù\çùR;\õ	úŸ\æ<\0\0\0\0€\ä$\nø‰Gş¢ÁX±\ä\â\âû¤–Kj–°ÿ¨<\ÖI}-\n}\Ğ\\\ĞO\ô\ò\ïxV\ê%½\ñŸ9\0\0\0\0Ğ·ş‚}Ó‘\Ş/’\Ğ[(!ÿ	À\r\ò\Ø)\Û\å\ñ7\òü\í\Zˆ\n\n>d.\ç\nù·ş£\Ôfù\÷>%ÿ\ÖÏ˜\Ó\0\0\0\0±’\r\ô\ÉÎ‡pû\çRß•pûc©¥\ò\óş\âw·_ ¡w|(ºF?ûn.\ç:ı\nAù\Ü\"\Õ*ÿşÿ.((¸Àœ\0\0\0\0pBªA>\Õù‘	®• ÿ	²ÿ\"µDjŸ\Ôn©\ßJ\İ)Ó¾YVV\ög\ærA¦7\ìù{\é\ß\ê_JKK\Ï5\ç\0\0\0€ K7À§»„„\ÓKJJ¾-A\õn©\'¥Ú¤\öH°_(?’\ZZPP\ğ\Í\åŸ^\é\×+şz\å_\ê}\'€9\0\0\0\0\Í@ƒû@—	¡\çK\r—P:QBı\ò\ó^©7¤\×ç¤†\é<\ærH~\Æ_?\ë/\Ï-ú®	s:\0\0\0\0E2\İ\ëU‰$³ÀĞ«\ó6¿\Õsµ~QÏ•g½Š¿X\ên™v^\å7—Cf\é\Í\åo½^ş\æ\Ï\Ê\ÏgN\0\0\0\0—%Ô“	ı*\Ù\õ9E?_\n…†H°¼S\ê¥Z¤\ö\õ\\i\ÖÏ™ÿ£~N\ß\\¹Q__ª‹‘\Å\'n|8_¿\ÖĞœ\0\0\0\0\\“J@O6\ô«T\Ö\ë;z‡|©k\õùRÿ#!r—T‡\Ô\Ó=wÖ¿^~şss9\ä_yyùYr\Ì~(Ç§]§\ñ\Û\0\0\0\0CªÁ<•Ğ¯R]¿•Fù§¿!\á\ğv	ŠI\í\ß;\åq™<\Ş#U\n….2—ƒ\İ4\ì\Ë1œ\Úş¨ÿ`\Î\0\0\0\0~•N O5\ô«t¶“7\Ş?XXXxµÁ:}¸\Ô\ëR$ 6\Ê\ã}RE=o?\Å\\ş¤Ç³\çX\ïÔ·ÿ\ë\Ç\0\Ìy\0\0\0\0ÀO\Ò\r\â\é„~•\î\ö²J\Â\Şû%\Ì_%A¯F\ß#RÛ¤J­\ç*U\n…şb?\ôrìŸ•zIoügN\0\0\0\0?H\0O7\ô«lwÀJKKÏ•07X\Â\ÜX©_\É\Ï[\å±K›$\ôÿ‡TiAAÁ_\r\"\àœÿ(µYoÀ(\ç\Èg\Ì\é™\ày\Ş\Í\ÍÍ®Zµ\êhCCƒ·t\éR*\Ç%\÷î¦¦¦=×™\Ç\0\0\0\ğ«ï„~5\Ğ\í\'E\ÂûŸşƒ„¶Hxû¥8©C=Wq\õ3\Üe2\í¯y7<x\ğ\ér\Ü\"\Õ*Áÿ¿åœºÀœg $\ğÏ—À\é\íİ»\×;|ø°\÷\Î;\ïP9.ı»\ë\ßÅŠ—.]:\Ô<F\0\0\0€\ßd\"p4\ô«L\ìGTYY\Ù9Ê®\Z#m\Ô\ï¥Ş”z^jº<ƒ\Ô\ßHh;\Í\\\è\Ï\ğ\á\Ã\ßW|\â\ë\÷é£¾cÄœ\'z…_§D©\ÜWkk\ë>	ı/š\Ç\0\0\0\ğ“L\íL„~•\Öş6\ì½¼.—ú¾ù9R{şEEE?“ú„²O\ğ‘iz¥_¯ø\ë•}€¾Àœ\Ç$Ëœa>¡o\é\ç\n¿¥\ÇABÿ\ó\0\0\0~‘VÀN S¡_\õ¹_˜Î–ÿ·R·J\Èú…\Ô+RoI½(5SªB¦}6™\ğdŠ~\Æ_?\ë/\ç\ßyü9½7™\çqı‰ù¼\ÒÏ”›\á“\Ê_\é\ñ0\0\0\0\à}\ë4d2\ô«\ğşu\ÖYW‡B¡/I ºY‚\ÒÏ¥^\î	øk¥fI\İ(\õùÑ£G¿\Ç\\zw	\ô\ë\å¼|V~ş;sºLû¦L\ó\ôw\Ş&ú\ß<\Ğ\ê5¯ù…·qÙ¤p\é\Ïúœ95°\"\ô\0\0À2øU¦Cÿ 	ü_»şúë»¥\ô\Ê\éI€ª…B_\ì\ë­Ñ€\r\ôFrÎ”`¿Sj¾œ³G/>\ñ\Î\rıZ“\Ìe“	ı‡:[¼W—\Ü\é½üD\ÍI¥\Ï\é4s~*ı\"\ô\0\0Ào²øU\ÆC¿\Ò\à?(;ûd]yyùYş(\á¾]§I\İ\Ö+\ğk“úr\ïe’	ı»6.Œ	ü‘Ú½qQ\ÌüTúE\è\0\0€Ÿd+ğ«¬„ş\Ù\Üo \ëJJJ>¬7””\ÇwĞ¯µYoD™7™\Ğÿû\å\÷Ä„ıH\é4s~*ı\"\ô\0\0À\nzãº‚‚‚\÷›\Ï\÷’Lp\î\Ü\óU‰\\9¨ÿı¬%\á~BœÀ©‘ù’	ı¯.­	û‘\Òi\æüTúE\è\0\0€JJJ&JpxGj‰Ô\òûÿ\é5Y¯\"j`\×\ë¹x\Ì\0\ê‹\î¿ş;¢WE?(++û3yMv\Å	û‘\ê.**ú†\ÎKè·«ı\0\0\0°‚„†\"Ş•\ñŒÔ˜P(\ô±A\É])7x>*‘+\õ¿ÿ€•\ä\õxœ R\Éku¯¼V?˜L\è×»\õ›a?R:ÍœŸJ¿ı\0€(\Ï\ó\Îhnn~tÕªUG\Z\ZÂ•Û’¿{wSSÓ\Æ\Æ\Æ\ë\Ì\ã¸®¨¨\èfˆ0j\Í×¿ş\õ\Ù\ç{\î¾\ÓO?ı*sùH&¸§‹À\ß*((8[¿}B^ƒ¿Ğ’p\ß(Ëµ\ä\ç\×\äq»\Ô>©n©\æ\ßş\ö·1ÁÓ¬­\ÏÍˆ	û‘\Òi\æüTú¥\ã\ó˜\0Jÿ|	œ\ŞŞ½{½Ã‡\Çt\ZT\öKÿ\îú\÷_±b\ÅAé¤‡š\ÇpYYY\Ù9=¡Áû½\ë¨\ï~\÷»\İYş\Ù\nı~\Â\ğ\á\Ã\ß\'¯\Ñ©¨¨\ğ:\Ó\Ç\õ®–\rŞ«Oÿ(&\ğ\ës:ÍœŸJ¿ı\0€(½Â¯\Ó\ì,¨\ÜWkk\ë>\é¤_4\à¢ÁƒŸ\n…ş¢¤¤dXQQQgœ ©×‹O|\Öÿ“Yú\Z¼l„~?\ç\É\'ŸŒ\é\×\â\Õ\ëk\çÆ„~}Îœ\ZXú\0Qú–~®\ğ\ÛQz¤“>b#À\Ï\Ê\Ë\Ë? ¡ı\Ë\Úo\Ç•Z µI\ê°\Ô6	üO\È\ã8aÿ¨L_PPpš±\ÊL\êL‡şL\ï\à\Z2\Í~-¦\ñ¶>?3&\ô\ës:-f~*\í\"\ô\0¢’ê¤©œ4ü¨¾¾şT	\ç—şM	\ë\Õ\òø€<®z£\ç\Î\ßk¤\æŸø\ê¯\ï\Ê\ôO}\ó›\ß<3²¼<7\İü[C¡Ğ—zoÃ\É`\ÉĞŸ\Éı|¥¿\ñÄ¡\Îo\Ës\Óc¤tš\Îc.G¥WŒ\'\0\0QıuÒ½\ë\Èá·¼\ök¼?¼\ğŸŞ—3\Zx\ÑI\Ãf\ìÿD\Âø%´—J0¿[ı£\ò\óùù\í\âW\ë\õk\÷¦\Ê\ó7K]%?Ÿo®#™\ï–^ÿ\çºs82°3ú3µ?€/%O9\âµnm\ô^yê˜ o–Î£\ór\Õ\à\Åx\0•°“\îU{·x;^z\Ô{uÉÑy\ó\Ê)1\óQ/:iX\à	\Ş‘\Ğşu©–Pÿ3ù}™\Ôn©7\å\÷\õ\òü¯\ä\ç\É\ÏÅ¡P\èsÃ†\r\Ğ\÷\Ï^-\ë\ë\õ}ÇœÖL\íL„şL\ì\àk\ñ\Æı]\İOT\\\õx1\0\0D\Åë¤µ\Ş>\Ô\éµüş)oS\ãO\îŒ\×z\Ík\ô\Ş<\È\Íÿ²Qt\ÒÈ•¯\ãú¬„\í\"\r\ğRKıNƒ½T‹TCO\àÿ¾<~C\æÿ¨,vŠ¹L\Ğ\ïø–\õ_`>Ÿ¤î†şnpB¼\ñD2W\÷•.k®J¾O\0\0¢\âu\Òouµy¿_~OL¬¥WûušY¯­¸\Ïk\ß\ñbÌº¨ÔŠN\Z™&¡ıü\Â\ÂÂ¯Jp¿I~*OŸø~m}Kş+R\ó‹O¼U„<^ª_¿e®\Ã¼ú²]À)\ñ\Æ\æ\"\Õ2\×G%_Œ\'\0\0Q\ñ:\é×š~\Z\Ó\ñ&S|\Î\àE\'t\èM\ñ\ô\æxÜ¯\×;\ŞŸ¸iŞ‹R‹O\ÜL\ïyœ%U\n…†H]¤7\ß3\×\ãs\é\ğtCº\Ûœo<A\å¯O\0\0¢\âu\Òf˜W\Ík~ş^\İh­{\ÈÛ³y©\×ù\Æ\æ˜\õQ\É4úRVV\ögÜ¿R|\â{\ëZt\â\ë\îşP|\â\ë\ï6\É\ïÿ#\ÏÿX¿O~ş[ıº<sK\'ˆ§ú\Ó\Ù\à´x\ã	*\Åx\0¯“6~*µqé¤˜\õQ\É4Fı	íŸ”\Ğ~ù:©_H=/µ_jŸ\ÔsR?—\é5\ò8\\ÿr\ğ\àÁ§›\ë	°Tyª¡?\Õ\õo<A\å¯O\0\0¢\âu\ÒfWzg\İm/ş\÷Iµıw¿\ô:v¿³>*ù¢“‚‚‚Ih¿\\Bû\÷¤î‘ŸÿW\Âşk=W\í·H-’ºOªB¿B\ê\Ã\æ:P*Á<•ĞŸ\Êz@‰7 \òWŒ\'\0\0Q\ñ:\é\rO\Ş\ò“©\rOş\Ğ\ë\ê\Ø³>*ù¢“v‹ûÓ¤.–Pÿ-	\íc\å\ñ¿$Ä¯”j“: \õ‚<?G\êvùùe\ŞK¤\Î0×ƒ´$Ğ“\rıÉ®¤x\ã	*\Åx\0¯“~}İ¼˜@oÖ¦\Æ¹ƒ¿^ı\ëĞ¾˜\õQ\É´?•––\n…¾$Á½L\ê_¤“ ¿±ø\Ä\ò›¥K\àÿy®R\êJ	\öÿ\×\\²\"™ L\èOf=@ \ÅOPù+\Æ\0€¨xtW\Ç\Î\ğU{3\è\ÇıúU}-/Ç¬ƒJ¯\è¤\í¥w»—\ğş\çÚ¯•\0›ü<Sj¹\Ô©CRk¥~)u§L/ù>S^^~–¹\ä\Ü@û@—!\Şx‚\Ê_1\0\0D%\ê¤;Z6x—\İ\ö\ÍÒt¾\ñû˜\å©\ôŠN:ÿ\Ê\Ê\ÊÎ‘\Ğş	\ï!	\î\å\ñ©—¥Ş’\Ú)\õ´\Ôt©[¤¾VZZz¡¹X\'\İ\à\îr@\à$\ZOPù)\Æ\0€(:i»ŠN:w4¬^-ÿV\r\ñ\ğ—\Êã®pÿ’Ô¯5\ôK•\ÈÏŸ\×ÿ0\×_I5À§:?hŒ\'\ì*\Æ\0€(:i»ŠN:³\n\n\n\ÎÖ·\ÙK\ê\Û\î‹O¼ı~]\ñ‰·\ã\ï‘\ç\å\ñ~}»~(ºF\êc²\Ø)\æz\àŒdƒ|²\ó\èÁxÂ®b<\0ˆ¢“¶«\è¤Ó£7Æ“\ğ>XªJo˜\'OJ½^|\âFz¯\Ês¿)>qƒ½2ùù2½\ñ¹F¾¿\é\0\â`<aW1\0\0D\ÑI\ÛUtÒ‰\éW\Ù\éW\ÚIhÿ„\÷;\ô«\î\äquñ‰¯¾Ó¯Àk’úOy~¬<•ù>¡_™g®”8\Ø\'z@?O\ØUŒ\'\0\0Qt\Òvt8ÜŸ\'Áı\n©Q\ÜÿMü\"©­R‡\å¹\×\ä\ñ·R?‘iß“\ßÿ.\n}\Ğ\\3\à›¿H\ã	»Š\ñ\0 ŠNÚ®\nJ\'=x\ğ\à\Ó%´ÿ¥„\÷\á\Ük\å\ñ\çR\ÏIu\ô\Ô\ó\òü\ë4™ïº‚‚‚¿\Òe\Ì\õ\0	ú\ãz	ü@šO\ØUAO\0\0’@\'mW¹\ÖI\ëUx	\í+!¾¼\ç\êüÿ\È\ã\ï\õª½\Ô¤ï¹š£\ÔW\ô*¿¹ Ë®”sO_w~`\0O\ØU‰\Æo¼\ñ\Æ\Çw\î\Üù\êK/½\ô\æ]w\İ\õ¯ü‡:\0\0´]•¨“¶Y}}ı©úùy	øC\ä\ñ\Å\'>WÿŒ<\î•:(\õ¢\Ô\Ü\âŸÃ¿^\êo¾ù\Íoi®È—\Ğ`\0O\ØU\æx\Â\ó¼Ó¶l\Ù\òGy\×\ë!\ó{\ä‘GZ¾\÷½\ï]\Ş{^\0€c\è¤\í*³“¶\É\ğ\á\Ãß§w¾—\Ğ>Bjr\Ï\ñ_)>q‡ü\×\å\÷§\äù)\òsUaa\áWGŒ\ñÿ\Ìu\06\"\ô\ÇxÂ®\ê=xı\õ×¯Ü»wo[$ì›¶o\ß~\ô{\î™_PP\ğş\Ş\Ç\0\à:i»Ê‚\ĞŠ~W½øoHú¾\ÔıR\rR-RoJıN\êa™~—üByü¬\Î6Wø	¡8\Æv•\ñ¾?ü\áO¼ûn\ô\â~B\İB–9T]]}ƒyl\0>G\'mW\å*\ô6\ì½t>/¡]Š\ë¥~-?¯—Ç·¤vI-“š!\Ï\İ*\áş\ë\ò\óGd±S\Ì\õ\0. \ô\ÇxÂ®Zµj•w\ğ\àÁ\Ãf¸\ïOgg\ç\Ñx`]AAÁ\Ç\Íc\0\ğ):i»*Ó¡_:\í$\Ğ|M‚û\Í\à§\É\ÏOK\í\è	\÷/\Ë\ó\Ê\ã$©ø¢\Ìÿ\'\æ:\0\×úc<aO\íÙ³\Çû·û7oß¾}f¦\ï—.£\Ëj»H¹S\æ\ë@À\ĞI\ÛU\é„ş\ò\ò\ò³JKK?-¡¾@\Z\ö	\ä’Çµ\ò\Ø%­R\Ëe\Ú\òû©k\õ\ï\õ\æ{\æz€ b@\ã	»jÁ‚Ş¨Q£nlll\ï\î\î\îÿııw¼µµ\õ\×ÿ\ôOÿ¤\÷\êYÌ•~w\Ğ\Ç “¶¬ú\nı\ØÿÔ•\Òx–úwí”¥¶I–\ç7\Ê\ã©•€?2\n}\É/7\ä¹\éş•3­±\Ã|>\ÌmWÿlùÅ·Mkü}\ïy1—…1 Î…\ñÄ›o¾\é]y\å•\Ú\ÄL\ó[E\Æzÿ;\ï¼so_7\ò{\÷\İw\×\ëü¤-Ü­\ó‡>GÀ‰NÚ¥Z²d‰\'¡ı¯¥şGyü¡<>(\õ‚ü\Ü)\íR+¥fK“\ç¾%\áş/$ÜŸfW¤Gÿ\÷o›\Úø\ï\æ\óp\"`\àü>\Ğ\ÏÀ_q\Å\áÀ¯eN\÷[\õ¾ˆ JKKg>ü\ğ\Ã?şv¯¼ßµv\í\Úe\Úni\ï\÷\Ë\Å¤†>€\ï;i—\ê\í·\ß\ö\Ê\Ê\Ê\ô³W›¥ş·¤¤\ä^yü\'©\ËGù§\æ±\ó£1\Ó\Z¯\Ö0,ü\ó\ô\å?\Z3µaÜ˜i\ËK%hÿ2:>?­±M\êP$€»o\É9c¦5ü¢zZ\ã~y~\çm\Ó‡\÷Zç¾ºYK\ß?fz\ã\ß\Ë\Ï+o›\ÒøW2\ïV=½1tÒºy\ÍmK\èR[¬Û¹m\Ú\òº\èrq\ö\É\\şÅ€8¿\'\äŸ\à]v\ÙeN†şˆ¢¢¢¿½\í¶\Û6‰7\ö\ïß¿\ò?øA£´/\ë\ó\æ¼p}\0\ßwÒ®•^\é7‘K\"oŸ—ú©‘ú\\Oø¯\Î\äy§\Ü6u\éG«§6F\Ş>/!}š^…ÿ\çi‹Ï•À<¿\'2»\ô\ç~0­\ñ³\ÕS\İ6cù·n›\Öp£şN“Ÿ\'\ô^·9o\ïmWÿû\óg\ËzVO]~\í\÷§6\\.?ˆ,ŸŒı†o1 \Î\ï\ã‰ë®»\Îkmmu:\ô«ÁƒŸ^RRR+\í\ŞV}\Ô\ß\Íy\àú8\0¾\ï¤]«D´K\ôJú˜©·ÿ\ñ\÷Æ‡\ôª¹ş|\"°7n\Õ+\õ\òøº\ÔÜy\ö\Ü2cYø\İcÿmù‡\å\÷\Î^\Ëÿ¼zz\Ãe™\r\Z\Îu™1S—:1­\á\"\ëN0ot\Ûc¦7|\ó¶\é\Ë\è\Ï\Õÿ\ŞpLÛ«?\÷±O\Ñe\áoˆ€se<1\È\ñĞà¡&/\ğ\åRƒ\Í\çM®tÒ®”ë´„ıI\ášÖ¸b\Ì,ÿ@ø¹i««§5|©\ç\çßŸºüS·Miü²üÜ¢o\Ã\ïy¾\ãûS¿~k~ø#\rÿ]\ç\ô\ÆZ™\Ş%Ï—\õÌ»®zÚ²¯\è\Ç\ä\ç\ã‘u\'˜\÷\Û\ÖwLo|\"üŸ\n\òs\ä\İ}\ìStYø\"`\à\\O\"\ô\Ã1\ôq€\Ã\ô\ŞS}†W:iW\Ê\åNºzZ\ã\ÄÛ¦5Ş¥?\ëg\ò%4?\Ü\óüş\ê\êCú³<w¿\Ô[ú\ö{\r\Ûÿ<­\é¼\èü\Ó·\És‡\ä\ñÁ\Ú{}_t½S¯“\çw\Ô\×/¿EQ\Ö7uŒ¾MZ\ã\Ê\ã¾Èº\Ì\Û{\Û[{>»\ß1f\ê\ò§ûÛ§\Ş\Ë\Â\ß\ç\Êxb¡¡\Ö+\ô\÷ş]\é¤]):\é\Ô\ô|ÿ¡\ê\é7›\ÓL©Ì‹`a@œ+\ã‰A„~8†>pXœ\Ğ7ü»\ÒI»RtÒ©\Ñ+ÿ\ÕÓ–?Z__ª9Í”Ê¼DÀÀ1°«O ‚>pXœ°oV8ü\ÓI\ÛUt\Ò@\î1 \ñ„]\Åx\ôq°\ÊÁƒ?´p\á\ÂI3f\Ìxy\âÄ‰û\ê\ê\ê\ßt\ÓM\İz¢VUU7n\\\×ø\ñ\ãwNš4iQMMÍµ²\È)\æ:l\'t[Qt\Òv4{\Úš\ÏH\r\ã	»Š\ñ\"\è\ã`…Õ«WWM™2¥¥²²Ò“ \ïÍŸ?\ß[·n·m\Û6¯££\ÃSú¨¿\ë\ó:½¶¶¶»¢¢\â	ÿKB¡\ĞE\æ:m”\ëœ\î{\Õ\òb\Ş\Şom\ÑI¹—\ë\öp\ã	»Š\ñ\"\è\ãW6lrÿı\÷\ï=z´\÷\Øcy\í\í\íá€Ÿ,_—“\ğ´ººúÑ’’’›Û°I®_pı…ı?w\Ò\Ï<\óŒwé¥—z\çœs\÷\Ù\Ï~\Ö[¾|y\Ì<~+:i \÷r\İ>.\ò\óx\Â\Åb<ú8\ä…\ä\õ\Ó/^¼P\Ãşœ9s¼C‡™y>%º¼®§¼¼ü\í#F|\ÛÜ-rı‚\ë/\ìGø¹“ş\Ú×¾\æ½\ï}\ï\ó–,Y¢[\ï‚.ˆ™\ÇoE\'\r\ä^®\ÛgÀE~O¸XŒ\'A‡œ“Œşy\ó\æm¹\å–[¼\æ\æf3¿ˆ®o\ô\èÑ‡+**&šÛµA®_pı…ı:\é•+W†Cÿ—¾\ô¥˜i~+:i \÷r\İ>.ra<\áR1@}rJrù¦M›\Ö9a\Â¯³³\Ó\Ì\ì¡ë­««{{\äÈ‘s\Í\íç›­/8:\és\Ï=\×;ÿü\ó½-[¶\ÄL\ó[\ÑI¹gkûø‰\ã	—Š\ñ\"\è\ã3’\ÇO›7o\ŞV\rü\Úe“®Ü¸qJKKÇ›û‘O¶¾\à\\\è¤W­Z¾\Òÿ•¯|%fšßŠN\Z\È=[\ÛgÀO\\O¸TŒ\'A‡œ\Ñ\Ï\ğ\ßz\ë­Y»\Âo\Ò\íŒ\Z5ªKN\ò\á\æ¾ä‹­/8:\é·\Şz+ú\õ†~\æ4¿4{¶¶Ï€Ÿ¸0p©O ‚>9±~ıú¡zÓ¾L†¿?º½#Ftœg\îS>\Øú‚\ós\'}\ÅWx\ï}\ï{½„Cÿ7¾\ñ˜yüVt\Ò@\î\Ù\Ú>~\â\ç\ñ„‹\Åx\ôq\È	ıZ>½»~>Ìœ9sœ\èS\Í}\Ê[_p~\î¤\õ+ú>ı\éO‡ƒÿUW]şs¿4{¶¶Ï€Ÿøy<\áb1@}²n\õ\ê\ÕUz•¿««\Ë\Ì\ã9¡\Û-++;\n….2\÷-\×l}Á\ÑI\ÛUt\Ò@\î\Ù\Ú>~\ÂxÂ®b<ú8dİ”)SZ\ô­\×ù4w\î\Ümr²\Ï2\÷-\×l}Á\ÑI\ÛUt\Ò@\î\Ù\Ú>~\ÂxÂ®b<ú8dUWW×ŸVUUymmmf\Ï)\Ùş\ñP(´·¾¾şTss\É\Ö´]E\'\rä­\í3\à\'Œ\'\ì*\Æˆ CV=şø\ã\ë\ê\ê\Ì£F\Ú.\'ü\å\æ>æ’­/8:i»ŠN\Z\È=[\ÛgÀOO\ØUŒ\'A‡¬š>}ú+\ó\ç\Ï7\ów^<\ğÀ/\É	ÿss\É\Ö´]E\'\rä­\í3\à\'Œ\'\ì*\Æˆ CVMœ8qßºu\ë\ÌüMMMŠŠŠšû˜K¶¾\à\è¤\í*:i \÷lmŸ?a<aW1@}²ª¶¶\ö°~…š\r6oŞ¬o\ï_c\îc.\Ùú‚£“¶«è¤Ü³µ}ü„\ñ„]\Åx\ôqÈªªªª\îıû\÷›ù;/:::\É	\ßb\îc.\Ùú‚£“¶«è¤Ü³µ}ü„\ñ„]\Åx\ôqÈª²²2\ïØ±cfş\ÎÙ.=\á\ó]\æ\ß\Èt\Òv4{¶¶Ï€Ÿ0°«O ‚>YUYYyÌ–+ı\í\í\í(\Î\ó•~[\ÑI\ÛUt\Ò@\î1 \ñ„]\Åx\ôqÈª±c\Çv\Ù\ò™şM›6­-\Î\ógúmE\'mW\ÑI¹Ç€8\Æv\ã	D\Ğ\Ç!«&L˜°Ë–»\÷/[¶¬!\ßw\ï·´]E\'\r\ä\"`\àO\ØUŒ\'A‡¬ª¯¯_4ş|3\ç\Å\äÉ“\È	ÿsA\'m[\ÑI¹Ç€8\Æv\ã	D\Ğ\Ç!«\ê\êê®©­­\í6x…Bä„¿\Ü\ÜG\ĞI\ÛVt\Ò@\î1 \ñ„]\Åx\ôqÈªúúúSG\Z\õN[[›\Âsj\÷\î\İ\Ï\É\ÉŞªûc\î#\è¤m+:i \÷\ÇxÂ®b<ú8d]MMÍ’˜9<§\î¾û\î‡\ådŸe\îN “¶«è¤\Üc@\\CCC\÷\áÃ‡cú5*\÷%\ÇaŒ\'˜\ÇÁD‡¬…BUTT\í\ê\ê2³xNtvv¾(\'z›î‡¹o8\ĞoWú\Üc@\\SSÓ½{\÷\Æ\ôkT\îkÇ¿–\ñÄ‹\æ1B0\Ñ\Ç!\'ª««3g™\ÇsB¶ıˆœ\èS\Í}\Âú\í*B?{ˆ€kll¼nÅŠ[[[\÷q\Å??%\÷\Ö\íÛ·?,c‰RC\Íc„`¢CNœW^^şvss³™É³j\íÚµú¶ş6İ¾¹Oø#B¿]E\èr/ˆ\"\é&Ï~ù\ÑU«Vmhh·=TnK\ß¯W\Ç5,›\ÇÇ¯\ä\ß5T\êE©w\Ì/•“Ò¿»şı	üˆ\nb‡<1bÄ·+++wvvš\Ù<+\Ú\ÚÚ‘|‡\Ôps_p2\í$\Ì\àI\å¯\ôx˜\Ç@vq@$¾NOß\ÍU\Ùü”ş\İ\õ\ï¯W\Ç	i\0²%ˆ}ò¨¢¢bb]]\İ\Û\Ú\ÑeÓ‘#G6…B¡5%%%·›û€X„~»Š\Ğ\ä^Dz…Ÿ\Ï_\ÛQúvx½:k#\0È„ \öqÈ³‘#G\Î7nÜl]\ñokk[¡_N\î\Ù\æ¶¡ß®\"\ô¹\Ä‘¾¥Ÿ+üv”‡¥\Üi@–±ƒJKK\ÇWTTteú3ş=Ÿ\á\ß\Éş\Ôú\í*B?{A\Ñ\ö\ÛU´ı\0²%ˆ},¡Ÿµ1bD\çÌ™3\÷:t\È\Ì\ï)9p\àÀê»\ô·\ñş\Ô1\ğ³«ø¹\ÄQ²mÿ›Z½\æ5¿\ğ6.›.ıYŸ3\ç£V´ı\0²%ˆ},RRR\òa9	§–••|\ğÁ·µ··3}µ´´<7q\â\Ä\ğúu=º>s\è_²?*7\ÅÀÈ½ ˆ’iûu¶x¯.¹\Ó{ù‰š“JŸ\Ói\æüTúE\Û [‚\Ø\ÇÁB¡P\è\"9gIho»\ñ\Æw<\ğÀë›šš6lŞ¼y\ç¾}ûŞ”€¨­­­y\ãÆk¥S\\:y\ò\ä²\ÌY\æ\r]N—7×‰\ä%3\ğ£rWü€\Ü\â€(™¶\×Æ…1?R»7.Š™ŸJ¿hûdKû8\Ø\í”\Â\ÂÂ¿—\ğÿã¢¢¢…r‚¾(µKOÔ\Ç\õy®\ó\éü\æ\nºd~TîŠ{A%\Ó\öÿ~ù=1a?R:ÍœŸJ¿hûdKû8\0††††n\î\àlG\ÉqØ³”;89\ÄQ2¡ÿÕ¥\õ1a?R:ÍœŸJ¿ı\0²%ˆ}\0CSS\Ó¾«ÙÚ±cÇ¯—\ò]\Í@\Îq@Dè·«ı\0²%ˆ}\0Ccc\ãu+V¬8\Ø\ÚÚº+şù)ù»·nß¾ıa\ô\í”\Zj#\0\Ù\ÄQ2¡_\ï\Öo†ıH\é4s~*ı\"\ôÈ– \öq€³8\ğ‰E‹5Í˜1\ãp}}ı»uuu\ŞM7İ¤\÷Cğªªª¼±c\Çv\ßq\Ç\Ç\'Mš\ôFMM\ÍY\ä\ÔÈ²\Z4\õ\n³\Ô;:\ğ\ğc\é¿\Ó|\ÎG¥wıûø<\â€H\Û3xšµ\õ¹1a?R:ÍœŸJ¿\ôx˜\Ç\02!ˆ}\àœÕ«W?<eÊ”ã•••ıù\ó\ç{\ëÖ­\ó¶m\Û\æutt„¿\ßP\õw}^§\ë|^mm\í¾’’’+\Íuú\r\Z€t±ıH&\ôw´l\ğ^}úG1_Ÿ\Ói\æüTúE\è-A\ì\ã\0g¼\ò\Ê+?ºÿşû=\Ú{\ì±Ç¼\ö\ö\öpÀO–Î¯\Ëiø3fÌ®¢¢¢¿4·\á\'4h\0\Ò\Ä\ö#™Ğ¯\õúÚ¹1¡_Ÿ3\ç£V„~\0\Ù\Ä>\ğ=\É\ëg,^¼¸U\Ãşœ9s¼C‡™y>%º¼®§¼¼\Ü+++û±¹=¿ A® ¶I…ş#G¼­\ÏÏŒ	ıúœN‹™ŸJ»ı\0²%ˆ}\àk’\Ñ?6w\îÜ£·\Ür‹\×\Ü\Ül\æ\÷\Ñ\õ\é$TTT<cn\×h\Ğ\0¤+ˆ\íG¡ÿPg‹·\å¹\é1?R:M\ç1—£\Ò+B?€l	bø–\ä\òM›6­{Â„	^gg§™\Ù3B×«Ÿ\÷¿\á†¶™Û·\r\Z€t±ıHú\ñZ·6z¯<uGL\Ğ7K\ç\Ñy¹\ê?\ğ\"\ôÈ– \öq€/I?cŞ¼yG5\ğ\ë\à ›tız§ÿ\Ò\ÒÒ¥\æ~ØŒ\r@º‚\Ø~\Äıı]\İOT\\\õxúdKû8À—\ô3ü·\ŞzkÖ®\ğ›t;\İ%%%“\Í}±\r\Z€t±ıˆú“¹ºŸ¨tYs}T\òE\è-A\ì\ã\0\ßY¿~}½~\Ö>ÓŸ\á\ïn¯´´´ûú\ë¯ÿ¤¹O6¢A® ¶\ñB¿\äS-s}T\òE\è-A\ì\ã\0\ßÑ¯\åÓ»\ë\ç\ÃÌ™3KC\ñ’¹O6¢A® ¶\ñB?•¿\"\ôÈ– \öq€¯¬^½úa½\Ê\ß\Õ\Õe\æ\ñœ\Ğí–••\é\Ûü¯4\÷\Í64h\0\Ò\Ä\öƒ\ĞoWúdKû8ÀW¦L™r|Á‚fÏ©9s\æ•\Æb“¹o¶¡A® ¶„~»Š\Ğ [‚\Ø\Ç¾q\ğ\àÁ¿¨ªª\ò\Ú\Ú\Ú\ÌSºıP(\ôn}}ı\é\æ>Ú„\r@º‚\Ø~ú\í*B?€l	bø\ÆO<±¢®®\Î\Ì\àyq\ã7+))©4\÷\Ñ&4h\0\Ò\Ä\öƒ\ĞoWúdKû8À7fÌ˜qdşüùfşÎ‹Y³f–c•¹6¡A® ¶„~»Š\Ğ [‚\Ø\Ç¾Q__ÿ\îºu\ë\ÌüMMM‡‹ŠŠ\Ş0\÷\Ñ&4h\0\Ò\Ä\öƒ\ĞoWúdKû8À7jkk½\æ\æf3\ç\Å\æÍ›Iƒñ¦¹6¡A® ¶„~»Š\Ğ [‚\Ø\Ç¾¡7\ñÛ¿¿™¿ó¢£££[\ZŒ\ã\æ>Ú„\r@º‚\Ø~ú\í*B?€l	bøFYY™w\ì\Ø13\ç…ì‡†~«\Û\÷€½‚\Ø~ú\í*B?€l	bøFee¥5Wú\Û\Û\Û\õ\íı\\\é\à¤ ¶„~»Š\Ğ [‚\Ø\Ç¾\ñƒü Û–\Ï\ôoÚ´\é->\ÓÀUAl?ıv¡@¶±|cüø\ñ\Çm¹{ÿ²e\Ë\ös\÷~\0®\nbûAè·«ı\0²%ˆ}\à?úÑŞ˜?¾™¿\ó\â\î»\ïn‘c•¹6¡A® ¶„~»Š\Ğ [‚\Ø\Ç¾QWWw‡”™¿\ó\"\n.))©4\÷\Ñ&4h\0\Ò\åRû!ÿ–\åRƒ\Í\çM„~»Š\Ğ [\\\ê\ã\0\ç\Ô\××Ÿ>j\Ô(¯­­\Í\Ì\à9\Õ\Ò\Ò\Ò%ÿ]\İsmBƒ ].µúo\é©>\Ã?¡ß®\"\ô\È—ú8ÀIµµµû,X`\æ\ğœºûî»·Kc±\É\Ü7\ÛĞ H—K\íG¯\Ğ\ßgø\'\ô\ÛU„~\0\Ù\âR8©¤¤\äÊŠŠ\n¯««\Ë\Ì\â9±ÿş7\õ*¿î‡¹o¶¡A.—Ú8¡?nø\'\ô\ÛU„~\0\Ù\âR8kÌ˜1»\æÌ™c\æñœ¨®®\Ş)\r\ÅK\æ>Ùˆ\r@º\\j?\â„}³\Â\áŸ\ĞoWúd‹K}\à¬ë¯¿ş“\å\å\å^ss³™É³j\íÚµ\Û\õ*¿n¿\÷ş\Ä@ZS½\÷\0’e¶%A(B¿]e*¿e¶€ŸqN>QVV\ö\ã\Ê\ÊJ¯³³\Ó\Ì\æY\Ñ\Ş\Ş~@\Zˆcú\'›ûB\Ã\0\ö2\ÃK¯Z^\Ì\Ûû­-®\ôÛƒq\\\Ã9\røHEE\Å3ú~:8È¦#¢´´\ôMi –˜û h8\0À^ı…ı?‡ş§z\ÊûÔ§>\åu\ÖY\Ş¿øEoİºu1\óø­ı\ö`œ\×pN>s\Ã\r7l;vlw¶®ø\ëşÀ¿\Ù\Üv\r\0Ø«¿°\á\ç\Ğé¥—zgyf$({_ø\Âb\æ\ñ[ú\íÁ8®\áœ|HBùÒŠŠŠ\îL\Æ_?\Ã/\Â\ñ\âWø#h8\0À^ı…ı?‡şHmß¾=ú\Ï=\÷Ü˜i~+B¿=\çÀ5œÓ€O\ég\í%üwÏœ9\óø¡C‡\Ìü’\Î\Î\Î7\õ.ı=_\Í\ó~\r\0øŸ¡üø\ñ\á\Ğ?dÈ˜i~+B¿=\çÀ5œÓ€ı¥¼ˆ_*++\ë~\ğÁ¶··›y¾O{\ö\ì\éš8qbøıº]Ÿ¹xh8\0Àÿüú\õ«_y§zªwşù\ç{[·n™\î·\"\ôÛƒq\\\Ã9\r8@Bû•\òbŞ¤\áı\Æo<6s\æÌ·›ššoŞ¼ùø¾}ûº%\ßw·µµÛ¸q\ã[2¨\è˜<y\òP(tX–Ñ°¯\Ë]i®³/4\0\à~ı\Ï=\÷œw\ÆgxŸÿü\ç\Ã_gkN\÷cú\íÁ8®\áœ\Ürjaaa•¼°W½!zC>ıŒ¾\Ş\ĞI\ß\ìy~•Î§\ó›+H\r\0øŸŸCÿ5\×\\\ã]{\íµŞb¦ùµı\ö`œ\×pNH\r\0øŸŸCÿyç§ıP´&L˜3ßŠ\Ğo\Æ9p\r\ç4€”\Ñp\0€ÿù9\ô»X„~{0Îk8§¤Œ†\0ü\ĞoWú\íÁ8®\áœ2\Z\0\ğ?B¿]E\è·\ã¸†s\Z@\Êh8\0Àÿıv¡\ßŒs\à\Z\Îi\0)£\á\0\0ÿ#\ô\ÛU„~{0Îk8§¤Œ†\0ü\ĞoWú\íÁ8®\áœ2\Z\0\ğ?B¿]E\è·\ã¸†s\Z@\Êh8\0Àÿıv¡\ßŒs\à\Z\Îi\0)£\á\0\0ÿ#\ô\ÛU„~{0Îk8§¤Œ†\0ü\ĞoWú\íÁ8®\áœ2\Z\0\ğ?B¿]E\è·\ã¸†s\Z@\Êh8\0Àÿıv¡\ßŒs\à\Z\Îi\0}’Fb¹6ı\Ôrs9\0€\İıv¡\ß$¸†s\Z@Ÿ¤‘\'\ä›5\Ø\\\0`7B¿]E\è·	®\áœ\Ğ/½’\'\ès•\0|Œ\ĞoWú\íA@‚k8§\ôK¯\ä\Ç	û\\\å\0#\ô\ÛU„~{\à\Z\Îi\0I\Ñ+úq?WùÀ§ıv¡\ß$¸†s\Z@R\ôŠ~œ\Ğ?Øœ\0\à„~»Š\Ğo\\\Ã9\r i\Æ\Õ~®\ò€ú\í*B¿=Hp\r\ç4€¤Wû›\Ó\0şAè·«ı\ö  Á5œ\Ó\0R¹\Úo>\0\ğB¿]E\è·\ã¸†s\Z@J\"Wû\Í\ç\0şBè·«ı\ö`œ\×pNH\è\àÁƒZ¸p\á¤3f¼<q\â\Ä}uuu‡oº\é¦nm8ªªª7®küø\ñ;\'Mš´¨¦¦\æZY\äs\0\0;ú\í*B¿=Hp\r\ç4€«W¯®š2eJKee¥\'Aß›?¾·n\İ:oÛ¶m^GG‡§\ôQ\×\çuzmmmwEE\Å;ş—„B¡‹\Ìu\0\ìBè·«ı\ö  Á5œ\Ó\0¢6l\Ø0\äşû\ï\ß3z\ôh\ï±\Ç\ó\Ú\ÛÛ½­[·z\÷\Ş{¯7l\Ø0\ï²\Ë.\ó.¼\ğBm4Âú»>¯\Óu>_—“\ğ´ººúÑ’’’›\Û\0\0Ø\ĞoWú\íA@‚k8§\ò<\ï´Å‹/Ô°?g\Î\ïÀ\Ş\ìÙ³½K.¹$î«ªª¼E‹ykÖ¬\ñv\í\Ú¾Ò¯ú»>¯\Óu>_—\Ó\åu=\å\å\åo1\â\Û\æ\ö\0\0ùGè·«ı\ö  Á5œ\Ó@ÀI~ÿÀ¼y\ó¶\Ür\Ë-^ss³\÷øã‡\ÃûUW]\å­\\¹2\ğ“¥\ó\ërº¼®G\×7z\ô\è\Ã\Í\í\0\ò‹\ĞoWú\íA@‚k8§\0“œşiÓ¦uN˜0!üıšš\Z\ï\â‹/\ö|\òI3Ï§D—\×\õ\èút½uuuo9r®¹}\0@ş444t>|8&|R¹/9{$\ô1\òƒ€\×pN%\Ùü´y\ó\æmş!C†xW_}µ·ÿ~3Ã§E×£\ë\Ó\õ\êúÇw ´´t¼¹\0€ühjjÚ³w\ïŞ˜\0J\å¾v\ì\Ø\ñk	ı/š\ÇùA@‚k8§€\Ò\Ï\ğ\ßz\ë­\á@>t\èP½ùwüøq3»ˆ®O×«\ë\×\íŒ\Z5ªK\Z\á\æ¾\0\0r¯±±\ñº+Vlmm\İ\Çÿü”ü\İ[·o\ßş°şRC\Íc„ü  Á5œ\Ó@\0­_¿~¨Ş´O?s¯oÁ\×+\ò™üº^]¿nG·7bÄˆÎ‚‚‚\ó\Ì}\0\äM½\Â,\õ~¦œ\Êy\é\ß]ÿş~‹\à\Z\Îi €\ôkù\ô\îúz³=ı\ì}o\é\×\à¾n\İ:\ï7¿ùMø\ëù\ônıZú\óüù\ó\Ã\ÓúúO]¿nG·7s\æ\Ì=\Ò\ğL5\÷	\0\0À$¸†s\Z˜Õ«WW\éUş\Î\Î\Î\ğ]\ö/^lf\ô“\ìŞ½\Û?~¼w\×]wy,_­\ä‘G¼‡z\È{ù\å—\Ã?\ßy\ç\Şw\Ü7İnO·[VVv0\n]d\î\0\0@¾\à\Z\Îi `¦L™Ò¢\á}\ö\ì\Ù\á¯\×K¤»»;|e^¯\è¯]»\ö¤izu_«7ıº¾\Ê\Ê\Ê\ğ2ºl<º=\İ\îÜ¹s·I\ã3\Ë\Ü7\0\0€|# Á5œ\Ó@€tuuı©†ø¶¶¶\ğU\÷gŸ}\Ö\Ì\åQ\Z\Şo¿ıv¯½½İœ\ämÜ¸1\\&W—\Ñe\ã\Ñ\í\éveû\ÇC¡\Ğ\ŞúúúS\Í}\0\0\È\'\\\Ã9\rˆ„\ñ‰uuuŞ–-[¼.¸ \áy}›¾ş\ç€\Şq?U\Zü\õŠ¼·ú\ë\öt»ºıQ£Fm—\èrs\0\0\ò‰€\×pN2}ú\ôW\ômù\÷\İwŸw\ó\Í7›™<LoÈ§Ÿ\Ï\æ™g\ÌIQ\ñ\Ş\Şß›¾\Õ_\×\ï\æ~º]\İş<\ğ’4@?1\÷\0\0 ŸHp\r\ç4 \'NÜ§w\Ú>|xø¦|\ñ\èt½1__úıJ×¡\ë2\évuûMMMŠŠŠšû\0\0O$¸†s\Z\Ú\Ú\Ú\Ãz\÷ıK/½4\æ\æ|\Z\æ}\ôQ\ó\é“$ú\õ®şú&İ®n\ó\æ\Íú\öş5\æ>\0\0\ä	®\áœ¤ªªª{ÿşı\Şù\çŸïµ´´˜y<\ì\Ş{\ï\õ6l\Ø`>\õê«¯†ß¢\ÓM7y¯¼\òŠ99J¿\ÎO\×e\Ò\í\ê\ö;::I\Ôb\î#\0\0@>\à\Z\Îi @\Ê\ÊÊ¼cÇygy¦\÷\Î;\ï˜y<Lo\à\÷\ĞC…¯\äÇ»CMM§\r‡–\óª¿.£¿\ë:t]&İ®n_\ö£K\Öq\Ä\ÜG\0\0€|\" Á5œ\Ó@€TVV\ë\ïJ¡üø\ñ\ÑĞ¯WüS\rı‘+ı\í\í\í\àJ?\0\0°\r	®\áœd\ìØ±]ı}¦_ß’¯o\ÍO\äµ\×^½\â¯o\õO$\Ñ\Ûû#Ÿ\éß´i\ÓZ>\Ó\0\0lC@‚k8§\0™0aÂ®ş\îŞ¯W\é\õ&|•\èF~‘»\÷/[¶¬»\÷\0\0\Û\à\Z\Îi @\ê\ë\ëi¨¿\ï¾û\ÂoÍ\'™¯\ìKF¢¯\ì\Ó\í\ê\ö\'O¼@\Z Ÿ˜û\0\0O$¸†s\Zºººkjkk»·l\Ù\â]pÁ^ww·™É½\ãÇ{w\Üq‡·r\åJsR\ÒtY]‡®«7İnW¶<\nm\èrs\0\0\ò‰€\×pNR__\ê¨Q£\Şikk\ó.¹\ä\ï\ÙgŸ=)”G\ìŞ½Û«¬¬Ô›í™“ú¥\Ëè²º“nO·+Ó“Æ§U\÷\Ç\ÜG\0\0€|\" Á5œ\Ó@À\Ô\Ô\Ô,\Ñ\Ï\ÕÏ=Û»êª«\Ì\\\õø\ã{·\ß~{\Ü\à¯w\èwgW—\Ñe\ã\Ñ\í\év\ï¾ûî‡¥\ñ™e\î\0\0@¾\à\Z\Îi `B¡\ĞEG;;;\ÃW\İ/^lf\ó0}+¾†w½jo¾\Õ\ßüš>¥\óè¼ºL¼\r\èvt{\í\í\ík¤\ái\Óı0\÷\r\0\0 \ßHp\r\ç4@\Õ\ÕÕÎ™3\'\Ğ/¾øboÿşıfFÒ·\é\ë\ç\ó\õ\Æ|zG~ı*¾‡z(\\ú³>§\Ótxo\éWº~İnO¶ıˆ4<S\Í}\0\0°	®\áœ¨  \à¼\ò\òò·›››½šš\Z\ïê«¯¹\é^o:M\ïÄ¯_Áw\ï½\÷zUUU\áÒŸ\õŠ¿NK´¼>¯\ë\×\í¬]»V\ß\Öß¦\Û7\÷	\0\0À$¸†s\Z¨#F|»²²\òpGG‡7t\èP¯¢¢\"apO—®O×«\ëomm}F\ZœR\Ã\Í}\0\0°	®\áœLùÄººº·5ø2$|E¾¯·ú§B×£\ë\Ó\õ¶µµ½\n…Ö”””\Ün\î\0\0€MHp\r\ç4p#Gœ;nÜ¸\Zü\õ-øú\Ùû\'Ÿ|\Ò\Ì\ğ)\Ñ\åu=º¾\Ö\Ö\Ö\Zø¥±™mn\0\0À6$¸†s\ZÀ \Ò\Ò\Ò\ñ]ú½Ù\Şe_¿^Ï¼kt~]N—\×\õ\ô|†\'Wø\0€_\à\Z\Îi\0aúYû#FtÎœ9sÏ¼Ù³g‡\Ãû…^¾iß¢E‹¼5k\Öx»v\í\n|}\Ô\ß\õy®\ó\éüºÜ¾}û^\ì¹KŸ\á\0\0~B@‚k8§D•””|X\Z…©eee|\ğÁm\í\í\íÇ¶n\İ\Z¾Kÿ°aÃ¼\Ë.»,\îe\Ö\ğ£ş®\Ï\ët™\ïxKK\Ës\'Nß¡_×£\ë3·\0\0`3\\\Ã9\r F(ºH\Z‡Y\Ú\Ûn¼\ñ\Æ<\ğÀú¦¦¦\r›7oŞ¹oß¾7=\Ï;\Ô\Ö\ÖÖ¼q\ãÆµK\Å\äÉ“\È2d™7t9]\Ş\\\'\0\0€\à\Z\Îi\0}9¥°°\ğ\ï%üÿ¸¨¨h¡4/J\íÒ†£\ç\ñE}^§\ë|:¿¹\0\0\0?! Á5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§¤Œ†\0\0¸Šq\\\Ã9\r e4\0\0ÀUŒs\à\Z\Îi\0)£\á\0\0\0®bœ\×pNH\r\0\0p\ã¸†s\Z@\Êh8\0\0€«\çÀ5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§¤Œ†\0\0¸Šq\\\Ã9\r e4\0\0ÀUŒs\à\Z\Îi\0)£\á\0\0\0®bœ\×pNH\r\0\0p\ã¸†s\Z@\Êh8\0\0€«\çÀ5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§¤Œ†\0\0¸Šq\\\Ã9\r e4\0\0ÀUŒs\à\Z\Îi\0)£\á\0\0\0®bœ\×pNH\r\0\0p\ã¸†s\Z@\Êh8\0\0€«\çÀ5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§¤Œ†\0\0¸Šq\\\Ã9\r e4\0\0ÀUŒs\à\Z\Îi\0)£\á\0\0\0®bœ\×pNH\r\0\0p\ã¸†s\Z@\Êh8\0\0€«\çÀ5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§¤Œ†\0\0¸Šq\\\Ã9\r e4\0\0ÀUŒs\à\Z\Îi\0)£\á\0\0\0®bœ\×pNH\r\0\0p\ã¸†s\Z@\Êh8\0\0€«\çÀ5œ\Ó\0RF\Ã\0\0\\\Å8®\áœ2\Z\0\0\à*\Æ9p\r\ç4€”\Ñp\0\0\0W1Îk8§Diƒl™\Ë\0\0¸€q\\\Ã9\r Š\0\0\ã!¸†s\Z@\r\0\0:\ÆCp\r\ç4€(\Z\0\0tŒ‡\à\Z\Îi\0Q4\0\0 \èÁ5œ\Ó\0¢h\0\0@\Ğ1‚k8§D\Ñ \0\0€ c<\×pNˆ¢A\0\0\0A\Çx®\áœEƒ\0\0\0‚\ñ\\\Ã9\r Š\0\0\ã!¸†s\Z@\r\0\0:\ÆCp\r\ç4€(\Z\0\0tŒ‡\à\Z\Îi\0Q4\0\0 \èÁ5œ\Ó\0¢h\0\0@\Ğ1‚k8§D\Ñ \0\0€ c<\×pNˆ¢A\0\0\0A\Çx®\áœEƒ\0\0\0‚\ñ\\\Ã9\r Š\0\0\ã!¸†s\Z@\r\0\Ø\É\ó¼3š››]µj\ÕÑ††o\éÒ¥TKş\î\İMMM{\Z¯3\Ü\Âx®\áœEƒ\0\0v’À?_§·w\ï^\ï\ğ\á\Ã\Ş;\ï¼C\å¸\ô\ï®ÿ+V\\ºt\éP\óÁŒ‡\à\Z\Îi\0Q4\0`\'½Â¯\Ó¢Tî«µµuŸ„ş\Ícw0‚k8§D\Ñ \0€\ô-ı\\á·£\ô8H\è?b#¸ƒ\ñ\\\Ã9\r Š\0ì¤Ÿ)7\Ã\'•¿\Ò\ãa#¸ƒ\ñ\\\Ã9\r Š\0\ì”l\è\ó@«×¼\æ\Ş\Æe“Â¥?\ës\æ|\ÔÀŠ\Ğ\ï6\ÆCp\r\ç4€(\Z\0°S2¡ÿPg‹\÷\ê’;½—Ÿ¨9©\ô9f\ÎO¥_„~·1‚k8§D\Ñ \0€’	ı»6.Œ	ü‘Ú½qQ\ÌüTúE\èw\ã!¸†s\Z@\r\0\Ø)™\Ğÿû\å\÷Ä„ıH\é4s~*ı\"\ô»\ñ\\\Ã9\r Š\0\ì”L\èui}LØ”N3\ç§\Ò/B¿\ÛÁ5œ\Ó\0¢hÜ·gÏ\İv\Ûm\ë?\ó™\Ï=\ãŒ3\ôxS9ª\÷¼\ç=İŸø\Ä\'\ö\ä#™,¿Ÿ6H¡ß®\"\ô»\ñ\\\Ã9\r ŠÁm\Zø¯¸âŠ£\ßúÖ·¼5kÖ„®\Èı{\ë\ßı\Úk¯=z\ö\Ùg??ˆ\à$ú\õnıfØ”N3\ç§\Ò/B¿\ÛÁ5œ\Ó\0¢h\Ü\öı\ïıĞ¡C\Í,Š<ø\ò—¿\Ü\"‡¤\Ö<F@\"É„ş­\ÏÍˆ	û‘\Òi\æüTúE\èw\ã!¸†s\Z@\r‚\Û>\÷¹\Ï\Õ+\ÍÈ¿\çŸ¾M\É\Z\ó‰$ú;Z6x¯>ı£˜À¯\Ï\é4s~*ı\"\ô»\ñ\\\Ã9\r ŠÁmgyfx°Šü\Ó\ã ‡\äˆyŒ€D’	ıZ¯¯ú\õ9s>j`E\èw\ã!¸†s\Z(y\ñ/\× ŸZn._3³\'\òH‡y€€D’\nıGx[ŸŸú\õ93?•vú\İF@‚k8§€’ÿ\à8!ß¬Á\ær\ğ53w\"\ôx˜H¤¿\Ğ¨³\Å\Û\ò\Ü\ô˜À)¦\ó˜\ËQ\é¡\ßm$¸†s\Z0½’\'\ès•\ß]f\îŒK\ç3™\×\ó·’’0\ô9\âµnm\ô^yê˜ o–Î£\ór\Õ\àE\èw	®\áœL¯\ä\Ç	û\\\åw—™;û4}ú\ôp06l˜9	 [\ó\0‰\Äıı]\İOT\\\õxú\İF@‚k8§€\Ó+úqÿrs>8ÁÌ	uww{ÿø\Ç\ÃÁ\ôw¿û9 [\ó\0‰\Äı\É\\\İOTº¬¹>*ù\"\ô»€\×pN§W\ô\ã„şÁ\æ|p‚™;z\á…Â¡\ô¯ÿú¯\ÍI\ÈıûšH$^\è7ƒ|ªe®J¾ın# Á5œ\Ó\0Ì«ı\\\åw—™;º\ó\Î;Ã¡tÜ¸q\æ$dˆş}\Í$/\ôSù+B¿\ÛHp\r\ç4\0\ójÿ`s:œa\æÎ„®¹\æšp(}ú\é§\ÍI\ÈıûšH„\ĞoWú\İF@‚k8§„E®\ö›\Ï\Ã)f\îL\è£ıh8”¶´´˜“!ú\÷5¡ß®\"\ô»\ñ\\\Ã9\r ,rµ\ß|N1sgBgŸ}v8”=zÔœ„Ñ¿¯y€€Dıv¡\ßmŒ‡\à\Z\Îi\0W\ö4Wš\à3w\"\ôx˜H„\ĞoWú\İF@‚k8§(\rú{¥\Æ\õ<ü\İd\æN\ä‘\ó\0‰ú\í*B¿\ÛHp\r\ç4€H\à}\ów¸\ÃÌ\È#=\æ!\ô\ÛU„~·\à\Z\Îi \ØüD\Ï\Ã\ß\ÌÜ‰<\Ò\ãa  B¿]E\èw	®\áœ‚«¿`\ß\ßtø™;‘Gz<\Ì$Bè·«ın# Á5œ\Ó@0%è“ş`\æN\ä‘\ó\0‰ú\í*B¿\ÛHp\r\ç4<©ùTç‡½\ÌÜ‰<\Ò\ãa  B¿]E\èw	®\áœ‚%\İ\0Ÿ\îr¾r\ğ\àÁ-\\¸pÒŒ3^8qâ¾ººº\Ã7\İtS·6”UUU\ÇÆ\×5~üø“&MZTSSs­,rŠ¹Ë™¹y¤\Ç\Ã<@6À\ë\Ãj„~»Š\Ğ\ïiÃ–k;\ÖO-7—ü„\Ğ\Ç@ƒû@—·\Ö\êÕ««¦L™\ÒRYY\éI\ñ\æÏŸ\ï­[·\ÎÛ¶m›\×\Ñ\Ñhú¨¿\ë\ó:½¶¶¶»¢¢\â	7KB¡\ĞE\æ:-e\ÄN\ä“\ó\0\Ù(@¯«ú\í*B¿;$\r\ò\Í\Zl.ø	¡†L\öL­\Ç\n6lrÿı\÷\ï=z´\÷\Øcy\í\í\í\ŞÖ­[½{\ï½\×6l˜w\Ùe—y^xa8œ\é£ş®\Ï\ëtO\ç\×\å$\Ü­®®~´¤¤\ä\Ã\æ6,c\ÄN\ä“\ó\0\Ù$€¯«ú\í*B¿[\ôJ~œ \ÏU~8ƒ\Ğ¸/\ÓA=\Ó\ë\Ë9\É[§-^¼x¡†™9s\æx\ğfÏ\í]r\É%\á\ğRUU\å-Z´\È[³f·k×®p@\ÓGı]Ÿ\×\é:ŸÎ¯\Ë\é\òº\ò\ò\ò·GŒ\ñms{99u\"¯\ôx˜\È^p_y\Ñ6›Ï›ıv¡\ß-ú\ZŒ\ö¹\Êgú·e+ gk½Y\'ù\ä\ó\æ\Í\Ûr\Ë-·x\Í\Í\Í\Ş\ã?\'W]u•·r\åÊ“BYt~]N—\×\õ\èú$(®¨¨˜hn\×\æ?y¤\Ç\Ã<@ù\æû\õ‘½¯&\ö.ıv¡\ß==¯A3\ğ/7\çüˆ\Ğ¸+\ÛÁ<\Û\ë\Ï8\É!˜6mZ\ç„	ÂŸA®©©\ñ.¾øb\ï\É\'Ÿ4\óJJty]®O\×[WW\÷\öÈ‘#\çšÛ·€¹\ë\È#=\æ\Ê\'\×G^\Ä\ñ\Â?¡ß®\"\ô»G_wq^ƒ\Íù\0?\"\ôn\ê\È\õ\Ú7\ô<D¼\õø&øK\ö8mŞ¼y[#fÈ!\Ş\ÕW_\í\íß¿\ß\Ì(i\Ñ\õ\èút½ºşq\ã\Æ(--o\îG™»<\Ò\ãa |\ñx}\äMœ7üú\í*B¿›z^w\Ñ× 9\ğ+B?\à3\ğÿ|Ğ‰p¡\éÿ¾\Ö\ã‹à¯ŸQ¾\õ\Ö[Ãc\èĞ¡zs1\ïø\ñ\ãf6]Ÿ®W×¯\Û5jT—4²\Ã\Í}\É#s—‘Gz<\Ì”/¼>\ò\'N\Ø7+ş	ıv¡\ßMúZ\ë\õ\ÚlNüJ\Ïi\ó9\0şe\ğ‘2{2zşH<i\Ø\íZeıú\õC\õ¦dú™b}‹±^q\Ìt ‰\Ğ\õ\êúu;º½#Ftœg\îS˜»‹<\Ò\ãa |\à\õ_œ\ğ\÷2ƒ\'•¿\"\ô»«¸\çj¿ù<\àgœÓ€;\â\ïx=•\àoù\Ù=Ï›\âm\ß\núµcz\÷p½™˜~¶¸¿·,k0\Ñ\ïÿ\Ío~şú1½¹–şù\ò¾B‘®_·£Û›9s\æih§šû”\'\æ®\"\ôx˜(R}}”Å¯“\äj€dû^¥Ácpd>®\ô\ÛU„~7”——@^g\ß-**š&\rR¯\Ë\ÏGz^ƒo\ë\ïú|\Ï\ô\ï\êü\æ:\0?\Ğs\Ú|€ÿ\ô¸\ã\÷d‚¼\åşˆ¾\ö#/V¯^]¥W1;;;\Ãw_¼x±™AN²{\÷noüø\ñ\Ş]w\İ\å-X° |5\ò‘G\ñz\è!\ï\å—_ÿ|\çwzw\ÜqGx\ŞDt;º=\İnYY\ÙÁP(t‘¹oy`\î&\òH‡y€r-\Õ\×G¦Xúú8I®HFĞ	û~ı¯¿şz\ï~$\\\æ<~+B¿¿•””|]^g¤Ş‰\ó\Z\ì«tşº¼¹NÀfzşš\Ï\ğ—d‚v¼\0\ßW\ğ7?\"™ıÉ™)S¦´hx\×\ï×¯K¤»»;|\åQ¯\è¯]»\ö¤izu_«7ı:²\Ê\Ê\Ê\ğ2ºl<º=\İ\îÜ¹s·Ic;\ËÜ·<0wy¤\Ç\Ã<@¹–\ì\ë#,|}œ$W¤^a\"nØ\ğs\è\×ÿ,•‚wı\õ\×\ÇL\ókúıI\Âú\ß\É\ë\ì…8a>zA\×gn°‘³\æs\0ü#•€/\È\Çş\ñ\æK6\ğG¤²_Y\Ó\Õ\Õ\õ§\Z\â\Û\Ú\Ú\ÂWŸ}\öY3wDix¿ı\öÛ½\ö\övs’·q\ã\Æp™t^]F—G·§Û•\í…B{\ë\ë\ëO5\÷1\Ç\Ì]D\é\ñ0P.¥\òú\È_\'\É\Õ\0©¸Ÿ°\á\ç\Ğ?v\ì\Ø\ğùşÓŸş4fš_‹\Ğ\ï/\å\å\ågıL^k\İfx3fŒ\÷\Ë_ş2üÑ½mÛ¶…o4ª\ôQ\×\çuº\Îg.«\ë\Ó\õ\êú\Ím6\ÉUŸ \ó\Ò	\Ö\ñ}\ï\àozª?\"ıKJ²ƒd	\ã\ë\ê\ê¼-[¶x\\pA\Â+\òú6}\r?‘>\Zü\õŠ¼·ú\ë\öt»ºıQ£Fm—}¾\Ü\Ü\ÇLH\ö\ï1ˆ\Ğo=\æÊ„dÏ‡d_Ù’«\×Gºl ù9\ôÿ\Ã?üCø|ÿĞ‡>\äşùŞ²e\Ëb\æ\ñ[=ı\ô\ÓVHL^\Ë)))y¹wX…BŞ¬Y³\â\ö\İ}\Ñùu9]¾\÷ú¤^\Ò\í˜\Ûla[Ÿ 9	\Ô\ñ‚½ş®W\Ù\Ì\ç\Ó\rü\ÙÏ„zu²}†›\éÓ§¿¢oË¿\ï¾û¼›o¾\Ù\ì»\Ã\ô†|úùügyÆœ\ï\íı½\é[ıu\ñn\î§\Û\Õ\í?\ğÀ: ø‰¹™\ì\ßc¡\ß*z<\Ì”	ÉÉ¼>²-¯t\Ù6@\òs\èÿ\Ü\ç>\çM:5\ÜV\Ê?\ÅûÔ§>3Ÿ\ê\ğ\á\Ãú\Íú\Z\Û)\õ¬\Ô\ÃzşJ°¼¹¨¨\è[\òønøf9Ÿ”j\é\Õ.z\÷\Üs·k\×.³9J‰.¯\ë\é½^©]º=s\0\ØÖ§\è_&‚t¼\à¿\Ùø} ?\"û{£“Mn&Nœ¸Oß–7|ø\ğ\ğMù\â\Ñ\ézc¾¾\ôú•®C\×e\Ò\í\ê\ö›šš6\È`p¡¹™\ì\ßc¡\ß*z<\Ì”	ÉÉ¼>²-¯t\é\ß\Î|.Ÿüú#\õ\Ö[o…\Ïû\÷¾\÷½1\ÓüVK–,\ñ\n\n\n>*!\ï\n9wK\å\ñv9gf\Ê\ÏO\È\ã+R\å¹.ı¹ç¹™:\Î+?%\n}l\ğ\àÁ§›\Ç™S|\â\n4\ğ\ë\Õù…š\ÍĞ€\èúŒ«ş»t»\æ¾\0ùf[Ÿ o™\Ğ\ñ‚¦D&\÷;^¨‰njkk\ë\İ\÷/½\ôÒ˜›\óEh˜\ô\ÑGÍ§O’L\è\×U\éWü™t»ºıÍ›7\ëÛ—\×\ôúgdLœ¿CÜ¿\Ç B¿U\ôx\ô:6\ç<ˆ{>$\óúÈ¶\\¼>Ò¥3\ó¹|\òs\è¿ü\òË½³\Ï>\Ûû\ío>\ï¿úÕ¯\Æ\Ì\ã·J\æ3ı\ï/--ı´œKC\õ]\0\òø“\â\ï\n\Ğw\è»\ôN\ğ¼[ \ô3\ö½\ß\Òÿ½\ï}\Ï[¿~½\Ùe„®W\×Ù–\ÔK|\Æ¶±­OXFƒs}K¿y…_\Ï\Æ\rµ2¶ÿ½:\ÖD7UUU\İú\àúÒ––³Ÿ»\÷\Ş{½\r6˜OG½ú\ê«\á· \ßt\ÓM\Ş+¯¼bNÒ¯\ó\Óu™t»ºıC²O-\æ¿%\âüû\Í\nÿ=ú­¢\Ç\Ã8”\çø›>’y}d[.^\éÒ¿•ù\\>ù9\ô744x\ó7\ãs\Î9\áomĞ¯\ğ3\ç\ñ[%úû£Wúy·@v\ôÜ´/\Ü\æ\é•ølş]\ï+şº}sŸ€|²­O\ß{\Ì\ã\Ì	\Ğ×•şxw\õÏ„q‘1UVV\æ;v\Ì;\ó\Ì3Ãƒ´x\ô~=\ôPøJ~¼;\ô\×\Ô\ÔD×§Áß¼\ê¯\Ë\è\ïº]—I·«Û—ıĞ[\Ì>\æ²ú­bŸ|T¯l³\é\õ¯\Ì,Ÿüú]¬L„şd\ğnÔ•œøZ¾w#¯\ãE‹™MOV\èvzµ\ï\ê~˜ûä‹—\æs\0ì”±+\åƒ\â~\óŠ¦ƒ\Æ\ö¿W§j\Ö\ò\â^o_®¬¬<\Öß•\ÌşBÿø\ñ\ã£\ë\×+ş©†şÈ•\Ì\ö\ö\ö?g\éJfœ¿CÜ¿\Ç B¿U\ôx\ô:6\ç<ˆ{>$\óúÈ¶\\¼>\\Aè·«rúû”wÈ¿\ñ4\ó¹D\ä\ß\õB¤\İÓ›\í\å’qs¿\Ì}\òE\ÏI\ó9\0\ö\ÊDpø\õ3ü\ñ\îŞŸ©\àŸ‰ı\êÕ¡\Æ\r3cÇ\í\ê\ï3\Ëú–|}k~\"¯½\öZ8ø\ë}«\"‰\Ş\Şù\Ì\ò¦M›\Ög\é3\Ë\Éş=ú­¢\Ç\Ã<@™\ìù\Ì\ë#\Ûr\ñúp¡ß®²%\ô\'Ã…w\ô\ì\÷tù·ü‰9­·\Â\ÂÂ«#mŸ¾\İ~ w\éO•n¯\÷\Ûüu\Ì}\òA\ÏG\ó9\0vH€Nø#Á>\Ş\ôÿ\ìo\\‘Î´8A˜‰˜0aÂ®ş\îN®W\é\õ&|•\èF~‘»“/[¶¬¡(Kw\'O\ö\ï1ˆ\Ğo=\æÊ„dÏ‡d^Ù–‹×‡+ıv•ŸBü\ğn\Ù\Ö==\í\Ú\ë}i™¾ \ÒÎš5\ËlrrB·Û«^`\î#z>š\Ï°_:A:^ w—şx\ó¥ü\Ó\Ù\Ï~\÷f\"\ê\ë\ë\õ\÷=\ä\É|e_2}e_\ä{\È\'O¬‘¬|y²A„~«\è\ñ0P&${>$\óúÈ¶\\¼>\\Aè·«\\\nı\É\È\÷»d¿*şc\Ö\åı—\îS\ïyB¡\Ğ{\ö#<O®¯\òG\èv{\í\ë;º_½\÷\È=\Í\ç\0øC*:^ø#\âÍŸj\ğOeÿ²¢®®\îš\Ú\Ú\Ú\î-[¶x\\p\×\İ\İm\ö\Í\Ş\ñ\ãÇ½;\î¸\Ã[¹r¥9)iº¬®C\×Õ›nO·+\Û?.şip/7\÷1\ÇN\Ú?\ä—\ó\0\åR2¯l²\ğ\õa5B¿]´\ĞßŸl¿[@\æy®W\Ô.ıO…^\ó|72mÌ˜1f““Sºı^ûù\İ\Şÿ–\Şd\Ú`©\å\æ\ó@¦\é¹h>À?’	\Ö\ñ|_?\"\Şr\Éÿd\ö+\ë\ê\ë\ëO5j\Ô;mmm\Ş%—\\\â=û\ì³f¿¶{\÷n¯²²Ro&fN\ê—.£\Ë\ê:Lº=İ®L\ÓÁJ«î¹9f\î\"\òH‡y€r)\Ù\×G¶Xøú°\Z¡ß®\"\ô§n \ïŸ\ß\Ğ\Ğ¯d‡d\İ’Ÿ§GÓ›\ë\æ“n¿\×>N7ÿ\Å=a?29\È4\Î3Àÿú\n\Ø\ñ‚{2?\"\Ş\òıÿ¾\ö\'\çjjj–\è\ç†gÏş~\èDüq\ï\ö\Ûoü\õı\ñ\î\ì¯\ó\ê2ºl<º=\İ\î\İwß­ƒ—Y\æ¾å¹‹\È#=\æÊµd_\Ù`\á\ë\Ãj„~»ŠĞŸy}¼[`±<v\÷\n\Ñ\ñj¿Ì¿9\ò{¾nN\Z¡Û\ì‹\ìWc\ä\ßXl„ıH\õş;\0\ÙÀy¸!QĞ¾aPú?\"^\ğy\Ò”h?\ò&\n]TQQq´³³3|Uq\ñ\â\Åf\ß¦o5\Ö\ğ®W\íÍ·ú›_Ó§tW—‰\÷¶hİn¯½½}4´mº\æ¾å¹›\È#=\æÊµd_™f\é\ë\Ãj„~»ŠĞŸ;\ÒFü¹’z[j·Ô»‘\ç\ô›I\òI·\ßkÿ\ô\İq\Ã~¤\Ì3iœg€;\â\îŞ=À‘\Ìz\âm\ß\n\Õ\ÕÕÎ™3\'\Ğ/¾øbO¿›<}›¾~>_oÌ§w\ä×¯\âÓ·\éi\é\ÏúœN\Óyâ½¥_\éúu;º=\Ù\ö#\Ò\ĞN5\÷)O\Ì]E\é\ñ0P>¤\òú\È‹_V#\ô\ÛU„ş\Ü),,ü3$\÷\Ü\àR_‹|4¨\ç¹\ğ\ôl·cı\Ñ\í\÷\Ú\ß\ã\æş›eş›L\ã<\Ü/xk@\×+\ó\ñ‚z*úZO¼\íZ£  \à¼\ò\ò\ò·\õ\Şkjj¼«¯¾:\æ¦{½\é4½¿~ß½\÷\Ş\ëUUU…K\Ö+ş:-\Ñ\òú¼®_·³v\íZ}\Ûr›n\ßÜ§<1wy¤\Ç\Ã<@ù\ê\ëc ,}X\ĞoWúsG\Âüˆp|Lo(¿—H»q¶9_\ïp}\ì\Ø1³ù\É)İ¾\ì)*\ße¾f\0ø[®x®·—–#F|»²²\òpGG‡7t\èP¯¢¢\"\ãÁF×§\ë\Õ\õ·¶¶>#\r\ì©\á\æ¾ä‘¹\Ë\È#=\æ\Ê^\ö#\ô\ÛU„şÜ‘ ÿ\Éùÿ\\VV\ög\æ´\Şl½\Òß³_¼½\0q¹\n\â¹\ÚNFH\à˜XWW\÷¶›!C†„¯8fj` \ë\Ñ\õ\éz\Û\Ú\Ú^…Bk\ô&D\æ>üÿ\ö\î/\Ä\Î2¿x\ËJ¨¡/\rHQ\è•WŠHÁ\æ¢\Å±^(\Æ\Ä?DD\ô.¹p½h)]‹e\ñr»-š«Ê‚\Ë\Òb.¢%k³3\ÜH²’\Ö\ZŠ\rf\ógšui&\ÛnŞ¾¿\Ù9\Û\é\óÎ˜“7\ïœ\÷9\ÏûùÀ—q\Îy\Ï{N\ÎÃœ\çùúœ9Ó³\ôaÓ£t€ú\ä\ç#oJ^Qú\ó\ó\ä¯w~©D\ç\ö;ı+\ãª\å\å¿\0®\Çz\ò\õ>ÿºx\ö\Ùg\ğ\ò\Ë/_Šbo1\ß-Ş»wo:__—¸}œ\'Î·°°0…¦\Ä\ã³r“>tz\ã‘P\ßş\ó‘5¥?¯(ıù‰O\É•\è\\?½$-ÿ\é\õ\0p=Ö«˜¯\×y\'â©§ú\ó\çŸş«ø?\ñ\ñab\ñ)\â\ñ\ç\Ã\ÒO\í¿–8>n·\ó,ÿ\ò\ç\ï`¦ÿz\ã‘Pü\ó‘5¥?¯(ıù©__¾?*\Ñ\ñ\á»}Šû=–x\\\éc•ÿ\ôr\0¸^]\ô®\Ï×‹z’ıÓ§Ÿ~ú‹7\Şx\ãÌ¥K—–ş^x”“;\î¸c\éCû\Ş}\÷\İ\êĞ¡C\ÕÉ“\'—&\ğø\Z\ß\Ç\åq}\Ç\Ç\í.\\¸ppùS\È\Ï\Åy\Óû\ÊH²,¡O1\é\0\åb ?YSú\óŠÒŸŸú\õ\å\ñQ\ÑŞ¹sg\òŠ;Y;vì¸º¢\ô?>V\0X]\õ®Î“…­[·\Ş^O\Æ\ß{\æ™g¾|ë­·>;şü=zt\éSúy\ä‘\ê¾û\î[*/\õ¡K_\ãû¸<®¯ûŸÓ§O\ğê«¯.}yœ\'Î—\ŞGf\Òu	=Š\ñH(\'üùÈšÒŸW”şülÛ¶\í\÷\ê×š+£²=úŸ’“\÷»¢\ğ_‰Ç•>V\0X/7Z\Øo\ô\öÙª\'\äß¯\'\æ]u)9\÷\Â/œx\ó\Í7\Ï\Ï\Ïüé§Ÿ~~\áÂ…_\Ôsø\×\çÎ;v\äÈ‘Ÿ\Õ½}¯½\ö\Ú\îú6×·ùÏ¸]\Ü>=g¦Òµ	=Š\ñH(GúùÈšÒŸW”ş<Õ¯9»G…{×®]\é\Ë\îD\Äı®(ı»\Ó\Ç\0\ë­mqo{»i\ó\ÛO<\ñ\Ä\Ö\å\æ»[¶l\ÙSO\Ö\ëœ\\¸\ã\ëÁ¸<®\ã\âø\ô™K\×&\ô(\Æ# Ì•ş\ó‘5¥?¯(ıyª_{şxT¸·m\Û6\ñ\İş¸¿ú5\ğ7o\íÇ“>F\0˜„\ë-\ğ\×{<ùJ\×\'\ô(\Æ# X‹ÒŸW”ş|\Õeû\ÃQ\é~ı\õ\×Ó—\Şu\÷7º\ïx\éc€I\Z·È{\Ó!]ŸĞ£t€`-J^Qú\óµu\ë\Ö\ê\Âı«Qù„¸Ÿ…ÿW\ñ8\Ò\Ç\0“v­B­\ë™>\é\Z…\Åx¤kQú\óŠÒŸ·-[¶ü\İÊ·ù>|8}	\îTœ\å\Ûú\ãş\Ó\Ç\0}Y«Ø¯u9\Ó-]§Ğ£t€`-J^Qú\ó¶}û\öß©\Ë\÷¿ŒJøs\Ï=·n\Å?\Î[\ß\ßo\ŞY\÷\÷Ÿ>&\0\èSZ\ğ\Ó\ï)GºV¡G1\é\0ÁZ”ş¼¢\ô\ç¯.ß›ü¿]\Ú\ñß³gOúR|C\â|+wø—\ïoSúX\0 £¢ÿ\ò\òW…¿L\éz…\Åx¤kQú\óŠ\Ò?\êBş+‹$>l\ïF?\Õ?nŸ|h\ßR\áûK\0\ä\äbÒŠ¯\é#]·Ğ£t€`-J^Qú§G\ì¼?¹\â­ş‘\Ø\õßµkWu\êÔ©\ô¥ù\Å\ñq»dw\é-ıq?\é}@v–K?\åJ\×/\ô(\Æ# X‹ÒŸW”ş\é¿c¿ü\á~iY¯v\î\ÜY½ı\ö\Û\ÕG}T}\ö\Ùg\ÕÅ‹—^£\ãk|—\Ç\õ;v\ìh\Ü6\Î\ç\õ;ü\0L\r¥¿xI\í¤O1\é\0ÁZfff®^¾|¹Q>e\ò©\Ç\áL]ú\Ó1\"\Ë\Î\ï\ÃU\Ê{›|\è\Ï\ò0ubK/£(i\ï¤G1\é\0ÁZ\æ\ç\çÏœ={¶Q@e\ò9q\âÄ\ê\Ò0#¦G]\Öÿ¤^\ó\ì®se•2ÿM‰\ãw\Ç\í\ÓsÀTPú‹—\öNz\ã‘¬evv\öÑ¹¹¹/.\Ø\ñ\ï\'\õ\ó¾püø\ñÖ…ÿ\ó:§c\Ä\ôÙ¾}û\ï\ÖkŸÇ·l\Ù\ò7\õ×™:?¯3ú\ó{ÿµüı\Ì\ò\õ\Ç\ñ\é9\0`ª(ı\ÅK{\'=Š\ñH¾I\Í\Øa®s%~§\\&x\Ş\ãùWøU¯ƒ6¯\Ø\Õßœ^\0SO\é/^\Ú;\éQŒG:@\0\ô§^\í_Qú\÷§\×À\ÔSú‹—\öNz\ã‘\0ıHvù\í\öP&¥¿xi\ï¤G1\é\0Ğd—\ßn?\0eRú‹—\öNz\ã‘\0“·\Æ.¿\İ~\0Ê£\ô/\í\ô(\Æ# \0&o]~»ı\0”G\é/Û†\r®ÆŸ¢‹‹‹g\ê!YL\Ç€ÉŠüUŠ~š\Í\é\í\0`*)ıe»û\î»/:t(\íŸ\ô`\ïŞ½?®‡\äP:F\0LV\ì\ä¯R\ò\Ó\Ø\í J\Ù6m\Ú\ô\×=\ô\Ğ/\Ó\Ê\Ä]Ü¸q\ãO\ê!y%#\0úg=@±Lr\Åû\Ö\Í7\ßü\Óû\ï¿ÿÔ\Î{«ÿd-...\Ä]ø\÷\Õc»F\ßJ€şYP,“\Ü D\Ñüv\ê\\ù­_˜œL&\ñ|\Ç\óÏ¿\Â)\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(–I\0:\ë!\0Še’\0†\Îz€b™\ä\0€¡³ X&9\0`è¬‡\0(B=¡\íI\í\ZÙŸ\Ş\0 dJ?\0E¨\'´Í«”ü4›\Ó\Û\0”L\é ±“¿JÑ·\Ë\0–\Ò@1b\'•²o—\0,¥€¢Äş*…\ß.?\00HJ?\0EYc·sz\0À(ı\0\'\Ù\í·\Ë\0–\Ò@q’\İş\Í\é\õ\0\0C¡\ôP¤\Ñnz9\0ÀXP¤\Ñnz9\0ÀX\ĞPUÕ†cÇıÃ~933S\íÛ·O&œúy¿:??fvv\ö\Ñt|\0\0Æ¥\ô\ĞPş¬gu\ö\ì\Ù\ê\ò\å\ËÕ•+WdÂ‰\ç=ÿ¹¹¹/\÷\í\Û\÷p:F\0\0\ãPúhˆş(œi•\Égaa\áB]ú¦c\00¥€†xK¿ş<\ãP—ş\ÅtŒ\0\0Æ¡\ô\Ğ¿S–O\é/1\é\0ŒC\é a\Ü\Òÿ‹KÕ±C_ù\É_-%ş;.K“‹\Ò\0´¥\ô\Ğ0N\éÿú‹\Ó\Õ\'\ïÿE\õ¯ÿ\ô\íÿ—¸,®K—\öQú€¶”~\0\Z\Æ)ı\'\ìişQNy·q¼´\Ò\0´¥\ô\Ğ0N\éÿ\÷ı¯7\Êş(q]z¼´\Ò\0´¥\ô\Ğ0N\éÿd\ßw\Ze”¸.=^\ÚG\é\0\ÚRúhPú\óŠ\Ò\0´¥\ô\Ğ0N\éO\ëO\Ëş(q]z¼´\Ò\0´¥\ô\Ğ0N\é?úÁ\ß6\Êş(q]z¼´\Ò\0´¥\ô\Ğ0N\é¿xú\ã\ê“ş\ËF\á\Ë\âº\ôxi¥\0hK\é aœ\Òùù\Ï~\Ğ(ıqYzœ\ÜX”~\0 -¥€†±Jÿ\âbu\ô§o4J\\\×5—\ÖQú€¶”~\0\Z®Uú¿ş\ât\õ|¿QøG‰\ë\â˜\ôv\Ò.J?\0Ğ–\Ò@Ãš¥q±Z8:[ıÛÿ¬Q\ô\Ó\Ä1q¬]ÿ\Ò\0´¥\ôĞ°Z\é¿\Ö\îşZ±\ë\ãQú€¶”~\0\ZV+ı\ã\ìî¯•¸mz>?J?\0Ğ–\Ò@\Ãj¥?-\ò×›\ô|2~”~\0 -¥€†\ÕJ¿\ô¥\0hK\é A\é\Ï+J?\0Ğ–\Ò@ƒÒŸW”~\0 -¥€¥?¯(ı\0@[J?\0\rJ^Qú€¶”~\0\Z”ş¼¢\ô\0m)ı\04(ıyE\é\0\ÚRúhPú\óŠ\Ò\0´¥\ôĞ \ô\ç¥\0hK\é A\é\Ï+J?\0Ğ–\Ò@ƒÒŸW”~\0 -¥€¥?¯(ı\0@[J?\0\rJ^Qú€¶”~\0\Z”ş¼¢\ô\0m)ı\04(ıyE\é\0\ÚRúhPú\óŠ\Ò\0´¥\ôĞ \ô\ç¥\0hK\é A\é\Ï+J?\0Ğ–\Ò@ƒÒŸW”~\0 -¥€¥?¯\Äd-\"\"\"\Ò6\éZ€Sú\óŠ~\0\0\0:3Í¥ÿ«¯¾ª^|\ñ\Å\ê¶\Ûn«\î¼\ó\Î\ê½\÷\Şk3mQú\0\0\è\Ì4—şW^y%\nr\õşû\ïW7n¬|\ğÁ\Æ1\Ó¥\0\0€\ÎLs\é¿ë®»ª\Ûo¿½qù4G\é\0\0 3\Ó\\ú7lØ°Tú\ï¹\çjÓ¦M\Õ\Ü\Ü\\\ã˜i‹\Ò\0\0@g¦¹\ô\ßt\ÓMKo\ïß³g\Ï\Ò\×(ÿ\é1\Ó¥\0\0€\ÎLs\é\İıúŸ°\ô~\ñ\õ–[ni3mQú\0\0\è\Ì4—ş—^zi©\ì¿\ó\Î;K_x\à\Æ1\Ó¥\0\0€\ÎLs\é?ş|\õ\ØcU·\Şzku\ï½\÷V‡n3mQú\0\0\è\Ì4—ş£\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0Qú\óŠ\Ò\0\0@g”ş¼¢\ô\0\0\Ğ¥?¯(ı\0\0\0tF\é\Ï+J?\0\0\0™™™¹zù\ò\åFù”É§‡3u\é_L\Ç\0\0\0Z™ŸŸ?s\ö\ì\ÙF•\É\çÄ‰?ªKÿÁtŒ\0\0\0 •\Ù\Ù\ÙG\ç\æ\æ¾\\XX¸`Ç¿Ÿ\Ô\Ïû\Â\ñ\ã\ÇXş\Ï\ë<œ\0\0\0´E3v˜\ë\\‰\ß)—‰\'\÷xş~\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0€i\ó¿±\õ Eˆ[…\0\0\0\0IEND®B`‚',1),('12530',1,'flow_mu180u86md3e.bpmn20.xml','12529',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://bpmn.io/schema/bpmn\" id=\"Definitions_1\">\n  <process id=\"flow_mu180u86md3e\" name=\"æµ‹è¯•914\" isExecutable=\"true\">\n    <extensionElements>\n      <flowable:executionListener event=\"start\"></flowable:executionListener>\n    </extensionElements>\n    <startEvent id=\"StartEvent_mu2c2p840\" name=\"å¼€å§‹\"></startEvent>\n    <sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\"></sequenceFlow>\n    <endEvent id=\"EndEvent_mu2c2p8c0\" name=\"ç»“æŸ\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </endEvent>\n    <sequenceFlow id=\"Flow_1qxvbp7\" name=\"xiaoyuyibai\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_15cdzo6\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_1dpq6dk\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_0qm5tc6\"></sequenceFlow>\n    <userTask id=\"Activity_0qm5tc6\" name=\"1\"></userTask>\n    <sequenceFlow id=\"Flow_0icqanz\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_1mzuxt6\"></sequenceFlow>\n    <parallelGateway id=\"Gateway_15cdzo6\"></parallelGateway>\n    <userTask id=\"Activity_1mzuxt6\" name=\"2\"></userTask>\n    <sequenceFlow id=\"Flow_1aqie0c\" sourceRef=\"Activity_0qm5tc6\" targetRef=\"EndEvent_mu2c2p8c0\">\n      <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${tes &gt; 100}]]></conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_016it02\" sourceRef=\"Activity_1mzuxt6\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <exclusiveGateway id=\"Gateway_0a6n8eh\"></exclusiveGateway>\n    <sequenceFlow id=\"Flow_13gvmcu\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_02klewn\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_1kkbmin\" sourceRef=\"Activity_02klewn\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0t3saos\" sourceRef=\"Activity_0ylt45t\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <userTask id=\"Activity_02klewn\" name=\"6\"></userTask>\n    <userTask id=\"Activity_0ylt45t\" name=\"5\"></userTask>\n    <sequenceFlow id=\"Flow_0dizg8q\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_0a6n8eh\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0e4xs2a\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_1jl42bt\"></sequenceFlow>\n    <manualTask id=\"UserTask_mu2c2p880\" name=\"3\"></manualTask>\n    <sequenceFlow id=\"Flow_0q59g41\" sourceRef=\"Activity_1jl42bt\" targetRef=\"Activity_0ylt45t\"></sequenceFlow>\n    <subProcess id=\"Activity_1jl42bt\" name=\"7\">\n      <startEvent id=\"Event_032ztbm\"></startEvent>\n    </subProcess>\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_flow_mu180u86md3e\">\n    <bpmndi:BPMNPlane bpmnElement=\"flow_mu180u86md3e\" id=\"BPMNPlane_flow_mu180u86md3e\">\n      <bpmndi:BPMNShape bpmnElement=\"StartEvent_mu2c2p840\" id=\"BPMNShape_StartEvent_mu2c2p840\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"142.0\" y=\"142.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"149.0\" y=\"185.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"EndEvent_mu2c2p8c0\" id=\"BPMNShape_EndEvent_mu2c2p8c0\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"812.0\" y=\"382.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"778.0\" y=\"393.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0qm5tc6\" id=\"BPMNShape_Activity_0qm5tc6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"780.0\" y=\"90.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_15cdzo6\" id=\"BPMNShape_Gateway_15cdzo6\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"545.0\" y=\"75.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1mzuxt6\" id=\"BPMNShape_Activity_1mzuxt6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"631.0\" y=\"170.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_0a6n8eh\" id=\"BPMNShape_Gateway_0a6n8eh\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"225.0\" y=\"365.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_02klewn\" id=\"BPMNShape_Activity_02klewn\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"200.0\" y=\"500.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0ylt45t\" id=\"BPMNShape_Activity_0ylt45t\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"610.0\" y=\"340.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"UserTask_mu2c2p880\" id=\"BPMNShape_UserTask_mu2c2p880\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"248.0\" y=\"102.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1jl42bt\" id=\"BPMNShape_Activity_1jl42bt\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"370.0\" y=\"350.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_032ztbm\" id=\"BPMNShape_Event_032ztbm\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"222.0\" y=\"182.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1g1p7by\" id=\"BPMNEdge_Flow_1g1p7by\">\n        <omgdi:waypoint x=\"178.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"142.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"248.0\" y=\"142.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1qxvbp7\" id=\"BPMNEdge_Flow_1qxvbp7\">\n        <omgdi:waypoint x=\"348.0\" y=\"134.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"548.0\" y=\"103.0\"></omgdi:waypoint>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"54.0\" x=\"404.0\" y=\"130.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1dpq6dk\" id=\"BPMNEdge_Flow_1dpq6dk\">\n        <omgdi:waypoint x=\"591.0\" y=\"96.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"720.0\" y=\"70.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"780.0\" y=\"103.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0icqanz\" id=\"BPMNEdge_Flow_0icqanz\">\n        <omgdi:waypoint x=\"570.0\" y=\"125.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"570.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"631.0\" y=\"210.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1aqie0c\" id=\"BPMNEdge_Flow_1aqie0c\">\n        <omgdi:waypoint x=\"880.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"848.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_016it02\" id=\"BPMNEdge_Flow_016it02\">\n        <omgdi:waypoint x=\"731.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"382.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_13gvmcu\" id=\"BPMNEdge_Flow_13gvmcu\">\n        <omgdi:waypoint x=\"250.0\" y=\"415.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"500.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1kkbmin\" id=\"BPMNEdge_Flow_1kkbmin\">\n        <omgdi:waypoint x=\"300.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"418.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0t3saos\" id=\"BPMNEdge_Flow_0t3saos\">\n        <omgdi:waypoint x=\"710.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"812.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0dizg8q\" id=\"BPMNEdge_Flow_0dizg8q\">\n        <omgdi:waypoint x=\"298.0\" y=\"182.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"298.0\" y=\"274.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"274.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"365.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0e4xs2a\" id=\"BPMNEdge_Flow_0e4xs2a\">\n        <omgdi:waypoint x=\"275.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"370.0\" y=\"390.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0q59g41\" id=\"BPMNEdge_Flow_0q59g41\">\n        <omgdi:waypoint x=\"470.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"540.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"540.0\" y=\"380.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"610.0\" y=\"380.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>',0),('12531',1,'flow_mu180u86md3e.flow_mu180u86md3e.png','12529',_binary '‰PNG\r\n\Z\n\0\0\0\rIHDR\0\0ı\0\0N\0\0\0\åg\0\0Q\ÎIDATx^\ì\İ\rœ\ÜU}/~D‹\"•Vo\í½½\Újk¯½µj[«­WKª\õ\Û\Âf³cƒ€\ÕJ\"\ñj@[Ë¿µ…„Š´µŠ(UsM½\à„lb\ÃS\n\"`	I\ÈHx\Ê\Üs6;\Ó\õ\ìL2;³3\ó›\ó{¿_¯\ïkv\ç\÷›\ßü²\ç\ì9\ç“\ß\Ì\ì~û\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0ıc\ö\ì\Ù§\÷\0\0\0}f``\à€Y³f½&\Ô	ƒƒƒÿ\ê‡\áë§†††>%ü\0\0@Ÿ8\î¸\ã~&„ú\ß¡ş¸P\ç‡Z\ê‘P7„ú\\\Ø\ö¾P¿;00\ğ’úÿ%Ü·!\ÔûgÌ˜\ñŒ\ôX\0\0\0@Ä€?<<ü\êŞ\rÁı¼P\×Æ€Bı\á\ö\ó1Ì‡zİœ9s•>¶*<\ö•aÿ\ï†ın·’n\0\0\0:,^‰=„\ó?\r\áü\Â\íÕ¡uS¸ÿ‚P\ê\÷LÛŒ\ğØ·„\ã^\÷ıxœt;\0\0\0\ñ\ì\ô\ò‚û\ÓC\0E\àsB-µz<\à\ß\êÂ°\íÄ™3gş¯‘‘‘ƒ\ÒÇ¶c\áÂ…û‡c\ãPK\Âyüjº\0\0\0\Ó\çP\Ço\ÉP\Ú!\\ÿ\Ï\ñ°}N¨+C\í\Z\ZúP_\n_\Ï\õÆ°\ÏÏ¦\í”øv€p>Ï»9\Ü.\n\ç\ñ\é>\0\0\0´§\ZøO¿üû\\ø!@¿<\Ô\Ñ!Lÿ}\ÕW„¯·‡\Û\ÛB]¾>)\Ô!‡~øs\Ò\Ç\öBû\ãÿ\Ãÿ‡\÷\ö\Ù\0\0\0\04/½ÂŸ~O\ñ=-\å_y8„\çO‡ú^øú¡P?	\õ•°í”™3gş\áÀÀÀÏ¥,šø2ÿp\ÎKB\İ_‘ÿ\ó\"\İ\0\0€\æ4\nø\î§\÷ƒq\Ä!\Ïú›P+B=jmû_·B½yxxø¹\éûIü€¿\ğ\ïø~¨\âÿ¥\Û\0\0Ø»}û}m§Bxi½3C\È?+\à\å\áv[¸½+\ÜşŸpÿGb x^ú¸\\„\ë‡º5ü{¿ş­¯L·\0\00Y³¾\Ùı˜!\Üşr¨£B¸ıT¨e\á\ëf\íùtû¥!\ô~lxxøm\ñ½\ï\é\ãrÿ„`ø¼?Ô†\ğ\ïÿ—¦û\0\0\0°\ÇTƒüT\÷§	!¸¾8ù?	A\ö/C]\ZjK¨ûB}#\Ô\éa\Û;FFF~1}\\™\ÅÿyÅŸ\Õ_Î=û\àt\0\0€2k5À·ú8‚Nÿû\Ğ\Ğ\ĞÿA\õ¡¾jS¨\õ!\Ø_n?\êĞÿ–>ú\â•şx\Å?^ù\õşøJ€t\0\0€²i7¸·ûøR!\ô¡¡\ôŒ\ê¿¾\Ş\êşPßŒ\÷…:,\î“>©‹\ï\ñ\ï\õ?\Ï\Û\â«&\Ò\í\0\0\0e\ÑL`¯L¨Fš9NiÄ«\ó!l¾küjı%\ãW\ãUüo‡úD\ØvD¼ÊŸ>\é?\Ì0ü¬¯?\óï‡¯?\İ\0\0³fƒz3¡?j\öxY‰\ï¯~g–§‡ú¿¡Ö…\Ú2~¥9¾\Ïü\ãû\ô\Ó\Ç\Ñ.\Ü?´\Å1³\ö|\ğ\á’øg\r\Ó}\0\0\0r3•€\Şlè¦rÜ¾?!?\Ô\Û\ã\'\æ‡ú·\"\ï\rµ5\ÔeãŸ¬døú—\Ó\Ç\Ñ{s\æ\ÌyVh³‡\ö\Ùn•\ñ¯\0\0\0\å0\Õ`>•\ĞM\õø…t\Ì1\Çü—\ß\Z\Â\áGBPüz¨»\Ã\÷\Û\Â\í\å\á\ö¬P\Ã\Ã\Ã/MG±Å°\Ú\ğœ\ñ\ğÿ\áøŸ\é>\0\0\0ıª•@>\Õ\Ğµ\ò<=\ÂûsgÎœùG!.ˆ/ug¨C@\r·jpüe\áOKKŠ\í9\Ş\Ö\÷Ä—ÿÇ·¤û\0\0\0\ô“Vƒx+¡?j\õù:*„½ŸaşM!\è\ßWC\İ\ê¡P+\ÃıŸ54<<ü²ıüRˆ\ğ\Úşû¡nˆü—n\0\0\è\í\ğVC\Ô\Î\ó¶m\ö\ì\Ù‡07#„¹“Bıkøú\öp»=Ü®\n¡ÿ\ïC\Íøû	ø¥ú\Ä‡º5~\0c\è#¯L·O‡J¥rÀÚµk¿¶z\õ\êÇ—/_^Y¶l™\êr…Ÿû\îU«V­=\"m\0\0\èW\í\ïvB\Ô\î\ó7%„\÷Ÿ9s\æ„\Ğ\ö¡Ş¾\\¨\ãWq\ã{¸GÂ¶_\÷2n\Z™1c\Æ3B?y¨\r!øÿK\èS/L\÷iGüKB\à¬lÜ¸±²s\ç\Î\Êc=¦º\\\ñ\çş+W®|hÙ²e‡¦m\0\0ıf:w»¡?š\ó¨9(„²7„šÚ…¡~\ê\áP?µ8\Üÿ\îP¿B\Û\Ó\Ó\ÇÂ¾~ø\áÏ™µ\ç\Ï-n‰·\ñ#\é>­ˆWøc\àLƒ¨\ê~mØ°aKı×¤m\0\0ıdº‚\öt„ş¨¥\ó9\ì°Ã‚\×\ëC}0ùB\İ<\ğ¯\Zü‡P\ï	¡\ì7|¦[¼\Ò¯ø\Ç+ÿ\ñ\0\ñ•\0\é>©\ğ˜\Òûª\âKú]\á/F\Åv¡W\ÚF\0\0\Ğ/Z\n\Ø\rLW\è\öz^!0Bü\ï…ú@YŸ\õ£P„º&\Ôy¡\æ†m¯j&|Át‰\ï\ñ\ï\õı\ï¶pû\'\é\ö‰\Â>ßŒo#I\ï\â{\Ê\Ó\ğ©zW±=\Ò6\0€~°\×`İ‚\éı\Ñ\Øù=\ëY\Ïú£\á\á\á×†@\õg!(}.\Ô\Çşu¡\Î\õ\ŞP¿u\Üq\ÇıLz\0\è…ø\éş!\Ğ_ú\å\÷\Ã×¿Ÿn\Û\Ş¶Ub®\×o›\rı?¸¡²\ö\Ú\ÏWn¾üÌ±Š_\Çû\ÒıT{%\ô\0Ğ¦;\ğG\Óú\÷ÿ\ÍGy\ä\îP\ñ\Ê\é?…\0uü\ğ\ğ\ğ\ï\ì\í¥\ÑP\ñƒ CŸ=&û{B-	}\öW«\÷\Ï\Ú\óÊ”úc™>¶™Ğ¿cÛº\ÊM—^ù\á·Nù©Š\÷\Åm\éşª\õú\0\è7üÑ´‡ş(ÿı:s¾\Ğqs\æ\ÌyVÿ\á~s¸]\ê\Ä	?\Ö¡^7\ñ1Í„ş{o¾xR\à¯\Ö}7_2i\Õz	ı\0\0\ô“Nş¨#¡\\\'\Ï:nhh\è\âJ†Û§’\Ğ\ë\ÖøA”\Õ}›	ı?^qÖ¤°_­¸-\İ_µ^B?\0\0…?¸n``\à\ç\Òû\'h&8Oî½ªF\Ùo\ß\ç…\ÂıiuµÎ­\î\×L\è¿i\Ù\ÂIa¿Zq[º¿j½„~\0\0\nahh\èŒui¨\÷†\ïÿ\ë„\Í\ñ*b\Ì\'O¸¯4€\÷¢\ö&üwÔ®ŠB?ù\Å\ğ;¹½NØ¯\Ö\îÁÁÁ·\Æ}…şb•\Ğ\0@!„\Ğ0œ„ˆ§B\Èø^¨y\Ã\ÃÃ¿´_sW\Ê\Ó\0Ş‹j\äı\ö}şPH\á\÷\ñ3u‚şOUø]\İ~WŸ\ÛLèŸÖŸ†ıj\Åm\éşª\õú¨©T*¬]»\ök«W¯~|ù\ò\åc“„\ên…Ÿû\îU«V­=\"m\È\İ\à\à\ào§!\"©k\ß\ò–·ü\óÁ¼\å\ÏxÆ›\Ò\ÇOƒf‚{«~ú\ÖÀÀÀ\ñ¯O„\ßÁ\Ï\Ç\n\á~4Ü®ˆ¾şp{W¨-¡v‡Zûo|cR\ğL\ë\ö+Ï\ö«·¥û«\Ö+®/\Ò6 ¤B\à_ge\ãÆ•;wNš4T\ç+ş\Ü\ã\Ï\åÊ•…IúĞ´ g###‡†4\ìO¬\ÇC\0\Ùz\ÔQG\í\î@\ğ\ïT\èø)…\Ã?ü9\áw\ô/\çÎ[Ù±cÇ¤9nbm]wc\å¦\Ë>>)\ğ\Çû\â¶t\Õz	ı\0\Ô\Ä+ü1p¦“…\ê~mØ°aK˜¤¯I\Ûr4cÆŒg¿lhh\è°ÁÁÁmu‚~µîœµ\ç½ş/\ïĞŸÁ\ëD\èø)\ï|\ç;“\æµzu\çu_œú\ã}\é~ª½ú¨‰/\éw…¿\Û!LÒ»\Ò6‚~6gÎœŸ¡ıu!´¿;\ÜşU¨¥¡n	µ3\Ô!\ğ+\Ü\Ş]\'\ì?¶}l``\à\é\É!§;POw\èŸ\î\óƒ¾Cf:¯Mª]»*·ÿ\à¼I¡?\Ş·M\Ú_µ\\B?\05MMÒªke’¦-\\¸pÿ\Î_Bú;BXŸn?nW†ºü“¿¯\ru\á¬=úë¨°ı\ïx\Ç;Y}|¸oqøo~\í\Ä\çHLg°\Î\Ğ?\ç}e_\ë‰\Û\ÖUn»r\ñ¤À_­¸-\î“>NµV\Ö\0\Ô\ìk’X»v>R\Ù|\÷µ•Ÿ\\\õ•»ø&mW\í—Iš\"ÁşgCÿ\Úg‡`ş‰\è¿¾¾1|ı\è¬=W\ë\ãŸ\İ;\'\Üÿg¡\Ş¾~AzŒz\Â~\ïŸø?Ÿ\'İ§\é\n\Ø\Óú§\ë| /5\\O\ì\ÚU\Ùpûh\åG\ßıè¤ ŸV\Ü\'\î\ëªûe=@M\ÃIzBm\Ûx[\å\î¾V¹\é\Ò\Ók\ó­Wœ=i?\Õ~™¤)€§…\àı¢\Ú\ß\ê\ÏC¨ÿ‡\ğı\å¡\î\õpøşúpÿ¿†¯?¾5<<ü\ê\Ã;¬­¿??s\æ\Ì?\n\Ç\Û\Z\÷\'\é¶}˜ =¡:\ÎúZ½\õÄ¾®\î7*Wı\Û/\ë	\0j\êMÒ±İ±­²\î\Çß­\Ü2ú©ŸŒ¿}je\íµ_¨<üÿ\ëD™¤\é–\ñ?\Ç\õª¶c€uQ¨Á>ÔºP\Ë\Çÿ\Ã\í[\Ãş/{Zzœ\éÿ\Æw8ş\Óû›\Ôn\àn7\ô·ûü…z\ë‰f®\î7ªø\Ø\ôxªù²\0 ¦\Ş$ı\È\öM•¯8k\Ò+^\í\Û\Òú•S\Ù|\÷5“¥¦V&i¦[\í/˜9s\æ†\àş¾\ğ\õ9\á\ö»³\öü}\íø’ü…Z2k\ÏK\õ·¯‰~+=Fh\'x·ú\Ûy^\ÈJ½\õDº†˜j¥\ÇSÍ—\õ\05\õ&\éÿX\õ\éIo3\å}ş\í—IšV\ÄÅ‹‚û‘\ñ\ïg\íùĞ¼kB=4kÏ‡\é}/Üjş\ğ\ğ\ğ;C½4~ø^zœ>\×j\0o5\ô·ú|¥z\ë	Õ»²\0 ¦\Ş$†ùzµ\ö\ÚÏı]\İZ­ùReı­\Ë*\Û\î¿u\Ò\ñT\óe’foFFF~1\÷7\Î\Ú\ów\ë?=¸\ç\Ï\İıdÖ?wKøş\ß\ÂıŸŠ/|ı{\ñ\Ï\å¥\Ç\È\\+A¼•\Ğ\ß\Ê\ó@\Ö\ê­\'T\ï\Êz€šz“t\Z\ğ§R7/;s\Ò\ñT\óe’\æ¸\ãû™\Ú_Bû!\È/\õùP?\õ@¨-¡®\õ¹°ı”p{x¸ıµ3f<#=N‰M5O5\ôO\õøP\n\õ\Öªwe=@M½I:\r\ò\õ*~²\î\×ü\ËO\Õ]ÿş\å\Ê\Öûn˜t<\Õ|™¤\Ëc``\ày!´¿>„\ö\÷„:+|ıC\Øÿ\ñ«\ö·…º$\Ôß„š\îC¨_HACS	\æS	ıS9.”J½\õ„\ê]YO\0PSo’¾\ñ;™\ò›©¿\ó\á\Ê\ö­wO:j¾L\Òy	Áş\é¡~5„úw…\Ğ~R¸ı§\â¯µ)Ôƒ¡®\n\÷_\ê#\á\ë?ûş\ÏP¤Ç¡%\Í\ôfC³ÇƒRª·P½+\ë	\0j\êM\Òw®¹pR O\ë–Ñ¿š\ô	ş\ñ\êÿ#;¶L:j¾L\Òıi\ö\ì\Ù¿6\÷‘P\ê\ë!\È\ß<k\Ï\'\ä¯\r\õ\íøÿ>\Üw|¨CB°ÿo\é1\èˆf‚z3¡¿™\ã@©\Õ[O¨Ş•\õ\05\õ&\é\í[\ï»jŸız¡?ş©¾­\ë~8\éªµ2IWü´û\Ş9„\ö·‡\0bøú¼P+B­µ#\Ôu¡¾\ê\ô°} \ì\÷\Ê9s\æ<+=]\×n`o\÷\ñP\n\õ\Öªwe=@M£Iz\ëº+7_ş‰Ia?­øV€m\÷ÿx\Ò\ãUke’î½‘‘‘ƒBhÿ\íŞ‡Cp?#\Ü~5\ÔC=\êP—…Z\êı¡\Ş<{\ö\ìÿƒ\Âi5¸·ú8(F\ë	Õ›²\0 \Æ$]¬2IwO\ë3g\Îü£\ğ?C|ø\Ë\Â\í½\ã\áş†P_‰¡?\ÔPøú·\â¤Ç ¯L5ÀOu(5\ë‰b•\õ\05&\éb•Izz\r_fjf|\Ùı¬=/¿_3k\Ï\Ë\ñ×‡ûG\Ã\íg\â\Ë\õ‡‡‡\ß\ê—\ÂÃ–‡l4\ä›\İg=Q¬²\0 \Æ$]¬2I·&~0^\ï3B?0/\Ü~\'Ô³\ö|\ŞM\á¾ÿ3k\Ïì„¯7~\ğ^zJc_~_Û:¬\'ŠU\Ö\0Ô˜¤‹U&\é\ÆâŸ²‹\Ò.„\ö?	\áı£\ñOİ…Û«g\íù\Ów\ñO\à­\n\õ\áş“\Â\í¡a¿_‰2/=\ì\×8\Ø7º\Ø\ë‰b•\õ\05&\éb•Iz,\Ü??\÷7„:6\÷¿\rş’P·‡\Ú\îûpûP¶½\'|ÿû\Ã\Ã\Ã\ÏMMH~ú=0\Ö\Å*\ë	\0jL\ÒÅª²L\Ò3f\ÌxF\í¿\Âû\á!¸Ÿ\Zn?\ê\ÊP[\Ç\ë\áş‰\Û\Â~Güø˜\ô8Ğ¦j\Ğ?yüV\à‡YO«Ê²\0 	&\ébUn“t¼\nBû\ï…?gü\êü¿…\ÛÇ«\ö¡~\ê›\ãW\ó\ß\ê\ñ*z\è°CBß‹¿w?´Áz¢X\Õh=qÿı\÷¿\ä{\î¹\é†nxø/ş\â/ş\Ê¨”€IºX\Õh’.²…\î\ß?ş;\Ã\í‡f\íy_ı\÷\Â\í\ÆP…º&\Ôg\íyş‘¡~\ã\ïx\Ç3\Ó\ã@¯Œ‡~ \r\ÖÅªt=Q©T~\Ûm·ıı®]»ªŒû=\ñÕ¯~u\İ{\Ş\ó\×O\Ü€Ì˜¤‹U\é$]$‡~øs\â\'ß‡\Ğ~t¨O\"şf\íù„ü;\Ã\÷\ß\r\÷Ÿ¾>a\æÌ™x\ô\ÑGÿ\é1 ˆ„~hŸ\õD±j\âz\â\Î;\ï<d\ãÆ›ªa?u\×]w=~\ÖYg-ø¹‰m\n@&L\ÒÅª„ş§Å¿Uü[Cú`¨Ï„Zj]¨‡Cı{¨‹\Â\ö¿f¸}UX$˜ú‰\Ğ\í³(V\Å\ö\ó“Ÿü\ä[O=U»¸\ß\Ğ\î <f\Çüù\óß¶-\0}\Î$]¬\êV\è?\ì°Ã‚\Îo…\Ğnf-\õ•\ğ\õ\õ\á\ö‘P\÷†º<Ô¹\á¾„pÿ–\ğ\õ‹\ÂÃ–r \ôCû¬\'ŠU«W¯®<\ô\ĞC;\Óp¿/Û¶m{ü³Ÿıìš—¤m@Ÿ2I«¦;\ô‡Iû…!Ğ¼9\÷?~Qøú²Pw‡û†û¿n\Ï5üN\Øÿg\Óc@\î„~hŸ\õDqjıú\õ•¿ıÛ¿­lÙ²%\Í\ôû\ÇE•O¥¿¯@É˜¤‹U­„ş9s\æ<k\ö\ìÙ¿Bı@\ØOAşK\á\öºp»=\Ünµ\"lûlø~^¨·\Çÿ½¾—\ÊÊ‚\Úg=Q¬Zºti\å\Øc½lttt\ó\îİ»\÷ıúşJ\å\É\r6|\åOÿ\ôO\ãg\õ|Û•ş|˜\ã\0“tÁjo¡?\öÿ\Z\ê0x\ê\ï\â¤\êP;\Ãı7‡Û¥¡ş*üc†‡‡_\Û/\È\ó¾\Ï\\\ñ\Üy‹F·¦\÷wCú\Ü\óÿaÅ¯¸h\ô\Ç\÷i$},ıË‚Ú—\Ãz\â\á‡®r\È!q<˜´­ßªºˆŸ¿sú\é§o\Ü\Ûù=\õ\ÔS\×\Ç\òc\á}qÿ¤i\és\æ8 ‹I:§º\ô\ÒK+!´ÿz ÿ8\Ü~8\Ü~!\ÔU\á\ëm\ávs¨+Bıs¨“\Ã}\ï\n\áşe!\Ü?=mWZÿO<g\ô\ï\ÒûÉ›´¯\ß\×\ñ=\ğox\Ã\Æ¬t{¿\ÕÄ‹\ñ\"À\ìÙ³Ï»è¢‹|\ò\É\'\÷·_w\İu_\Û\î\ã\àgú\åbScú~’Î©}\ô\Ñ\Ê\È\ÈH|\ïÕ­¡ş\ï\Ğ\Ğ\Ğÿnÿ4\Ô\ë9\æ˜ÿ’¶]?š·h\ô›\ó-?l\ì\ë\Å+>>\ïœ\å\'\Ï[´bv\Ú_®\í\ï_4º)Ôj\0?ùo.=hŞ¢åŸŸ¿h\ôpÿ=\'.=|\Â1·,8\Ù\Ï\Í[<ú¿\Â\×Wœx\ö\èÿû\Ş·\Í_<:üS\ÇN\öMŸ;„ş\ï„û¾Ÿ\ç\ÄE+\ÔW\çœ\Ò\ÇÒ¿,ˆ }ı¾ÿ„\Ê\ïş\î\ïfú«\ï\ÄO¼%¸ÿ¸\âCú\Ğhÿ~\ïO\÷%\æ8 \ï\'\é\Ü*^\éO\Û(\'Õ—Ï‡ú\ËP\Ç\Äû\Æ\Ãÿ\Â\ÚN•\Ê\ÓN<gÙ‹\çŸ3:³ú\òù\ÒÅ«\ğ¾\è\Û‡Àÿ¶pÿú\ê\î! _ù¡E£¯š\Î\òKN<wÅ»N\\´ü½\ñ?\â¶\ğ\õi\î;\ñ¹\çÿ\İ\Ç}hş9+\ŞşÁs–¿>|ı`\õq\õ\Ï)9oú–´¯\ß\×GqDeÃ†\rY‡şhÆŒ\Ï\Z\Z:5Œ{·\Ç\Ûø}ºy1\Ç}?I\çV&\éœ\Ä+\é\ó\Îı\È~?ú¥x\Õ<~½\'°\Ş¯Ô‡\Û;C}q|Ÿ\õ\ï?\÷\ò±W;œ\ô·+~!|¿m\Â\ã?7\ñ\ò‡\Ç\Ü\Ãy|Ì¼sV»g\Û\ò«»Á¾µç·xù;N\\¼biüzş\ß-aØ¶1~½—sª=–şfA\í\Ëe=±_æ¡Ÿ\ò1\ÇA\Æ\Â/øŠP3\ÒûS¹LÒ¹T\î“tûgÕ¢Ñ•\óş~\ÅÏİ·h\ô\êù‹–¿vü\ë?xÎŠWœx\ö\è\ë\Â\×\ë\â\Ë\ğ\Ç\ï\ßúÁsF\ß<\ö\Òü±·,ÿ§\Ú1\Z¶o\÷Œ\ï»fş¢\Ë\ß\ßF¾~²z\ìûş\çs\ÇW,ı\Ö\Ø*„¯«¯\Ø\Ë9\ÕK³ ‚\öå²\ØO\è\'3\æ8\ÈXü¯½†ÿ\\&\é\\*\çIzş¢\Ñ3N\\4ú\ñ\ëøüš/\Z¿ÿù\÷\İ\çÅ¯\Ã}Ÿ	\õH|ù}\Û¾h\Õ\ókû/½#Ü·#\Ü~\áÔ³¾ÿœ\Úq\Ï=\"\Ü\÷Â…+\Æ^¢wÎ¼ø2ıE£ÿn·T\İ`ß‰\Ï}ûø{\÷·\Î;g\Åeû:§‰¥¿YAûrYO\ì\'\ô“sdlB\è\ßkø\Ïe’Î¥L\ÒS3ş>ü/\Í_<úg\é¶\ÔT\ö¥\\,ˆ }¹¬\'\öúÉŒ92V\'\ô\×\rÿ¹LÒ¹”Izj\â•ÿù‹V|m\áÂ…û§\ÛRSÙ—r± ‚\öYO«¬\'¨2\ÇA\Æ\ê„ı´\ÆÂ¿IºXe’†\î³ ‚\öYO«¬\'¨2\ÇQ(=\ô\Ğ\ó.¾ø\â3\Ï=\÷\Üq\Æ[,X°\ó}\ï{\ß\î\ØQO8\á„\'N>ù\ä\íû\Ø\Ç\î9\ó\Ì3/9\å”S\Ş\ò´\ôEV\'t¢L\Ò\Å*“4t_\Óû€©±(VYOPe£®¾ú\ê\Î>û\ìu\Ç|%ıÊ’%K*kÖ¬©\Üq\Ç•­[·V¢x¿\÷\Ç\í§z\ê\î¹s\ç>\Âÿ¥\Ã\Ã\Ã/MYD\İş…K\Ãı„Z1\Ë\Ëû[&i\è¾nÏ#\ë‰b•\õU\æ8z\ê\Æo|\çg>\ó™\õ\Çw\\\å\ë_ÿze\ó\æ\Íc¿Yqÿø¸şŸ?ş×†††~!}\"\é\ö/Ü¾\Â~U?O\Ò\ßû\Ş\÷*¯y\Ík*tP\åU¯zUeÅŠ“\ö\é·2IC\÷u{|†\õ\óz\"Ç² \ÊGO„¼ş\ôoû\ÛÇ°ÁTv\ìØ‘\æù)‰Ç™3gÎ£G}\ôÿNŸ¯(ºı·¯°_\ÕÏ“\ô›\ßü\æ\Ês\óœÊ¥—^\Z¶•¾\ğ…“\ö\é·2IC\÷u{|†\õ\óz\"Ç² \ÊG×…Œş\ó^x\ám\ïÿû+k×®M\ó{[\â\ñ;î¸s\ç\Î=#}\Ş\"\è\ö/Ü¾\Â~U“\ôW\\1ú_û\Ú\×N\Ú\Öoe’†\î\ë\öø9\Êa=‘SYOPe£«B.ÿùE‹m;\í´\Ó*Û¶mK3û´ˆ\Ç]°`Á£\Çs\Ì\Ó\çïµ¢ş\Â\å0I|\ğÁ•¼\à•\Ûn»mÒ¶~+“4t_Q\Çg\è\'9¬\'r*\ë	ª\ÌqtM\È\ãO¿\ğ\Âo?D\ò\É\'?8{\öì¥\ç\ÑKEı…\Ëa’^½z\õØ•ş7¾ñ“¶\õ[™¤¡ûŠ:>C?\Éa=‘SYOPe£k\â{ø?\ğt\ì\n*>Ï±\Ç»=t\ò\Ã\Ósé•¢ş\Â\å0I?\ò\È#c¡?~ _º­\ß\Ê$\r\İW\Ô\ñúIë‰œ\Êz‚*s]qı\õ\×\Z?´oº\ßÃ¿/\ñù>ú\èm\ÏOÏ©Šú\×Ï“\ô\Ş\ğ†Ê³Ÿı\ì\ÊÒ¥K\ÇBÿ[\ßú\ÖIû\ô[™¤¡ûŠ:>C?\é\ç\õDe=A•9®ˆ–/~º~/œw\Şy\ëCG?\'=§^(\ê/\\?O\Ò\ñO\ôı\æoş\æX\ğÓ›\Ş4\ö=\é>ıV&iè¾¢\Ï\ĞOúy=‘cYOPe£ã®¾ú\ê\âUş\íÛ·§y¼+\âóŒŒ<4<<ü\Ò\ôÜº­¨¿p&\éb•I\Zº¯¨\ã3\ô\ë‰b•\õU\æ8:\î\ì³\Ï^_z\İK_ü\â\ïıü\ôÜº­¨¿p&\éb•I\Zº¯¨\ã3\ô\ë‰b•\õU\æ8:jû\ö\íÿ\å„N¨lÚ´)\Í\á]ÿ\É\á\á\á.\Ü?=\Çn*\ê/œIºXe’†\î+\êøı\Äz¢Xe=A•9ú\æ7¿yÆ‚\Ò\Ş\Ç{\ì]¡Ã¿>=\Çn*\ê/œIºXe’†\î+\êøı\Äz¢Xe=A•9Z¼x\ñ–,Y’\æ\ïø\ìg?{C\è\ğc7\õ\Î$]¬2IC\÷u|†~b=Q¬² \ÊGGq\Æ[Ö¬Y“\æ\ïXµjÕƒƒƒ§\ç\ØMEı…3I«L\Ò\Ğ}EŸ¡ŸXO«¬\'¨2\Ç\ÑQ§z\ê\Îø\'ÔŠ\à\Ö[o/\ï¿6=\Çn*\ê/œIºXe’†\î+\êøı\Äz¢Xe=A•9:\á„v?\ğÀişî‰­[·\î~]z\İT\Ô_8“t±\Ê$\r\İW\Ô\ñú‰\õD±\Êz‚*s522Ry\â‰\'\Òü\İ\á<¶\Ç\ß\ëJFE`’.V™¤¡ûŠ:>C?±(VYOPe££?şø\'Šr¥\ó\æ\Í?™\Õ\ã+ıEe’.V™¤¡û,ˆ }\Ö\Å*\ë	ª\Ìqt\ÔI\'´½(\ï\é¿\å–[®›\Õ\ã\÷\ô•IºXe’†\î³ ‚\öYO«¬\'¨2\Ç\ÑQ§vÚ½Eù\ôş\Ë/¿|y¯?½¿¨L\Ò\Å*“4tŸ´\Ïz¢Xe=A•9Z¸p\á%K–,I\ówO|\ò“Ÿ\\\Z:ü_§\çˆIºhe’†\î³ ‚\öYO«¬\'¨2\Ç\ÑQ,xÛ©§º;\r\à=\ğ\Ä\ğ\ğ\ğ¡Ã¿>=GL\ÒE+“4tŸ´\Ïz¢Xe=A•9Z¸p\áş\Ç{\ìc›6mJCxW\İw\ß}W†Î¾!Oz˜¤‹V&i\è>\"hŸ\õD±\Êz‚*sw\Ê)§\\ºt\é\Ò4‡w\Õ\'>\ñ‰‹Bg??=7\ö0I«L\Ò\Ğ}DĞ¾\åË—\ïŞ¹s\ç¤yMu¿B;¬\ë‰]iQN\æ8:nxxø¥s\ç\Î}|û\ö\íi\ïŠmÛ¶]:ú¦xé¹±‡\Ğ_¬ú¡û,ˆ }«V­Z¿q\ã\ÆI\óš\ê~\İ}\÷\İ_	\ë‰k\Ò6¢œ\Ìqt\Åüù\ó¿vÁ¤y¼+\Âs5t\ôs\Òs\â?	ı\Å*¡ºÏ‚\Ú7::z\ÄÊ•+Ú°a\ÃWü{S\á\ç¾á®»\îº(¬%\î	uh\ÚF”“9®xşœ9s]»vmš\É;\êºë®‹/\ë\ßŸ?=\'ş“\Ğ_¬ú¡ûÊ¸ \n\Ó\äa^ş\Ú\êÕ«_¾|ù\ØØ£º[\ñ\å\ğ\ñ\êx\Ëiû\ô«\ğ\ï:4\Ô5¡Kÿ½ª+\î\ñ\ç/\ğSS\Æ99ú\è£ÿ\÷\ñ\Ç¿sÛ¶mi6\ïˆM›6}/t\ğ»C?-Ni\ğT½«\ØiU\ÆQüKB\à¬Ä—c»*Û›Š?\÷ø\óWÇ…4 S\Ê8\Ç\ÑCs\ç\Î=cÁ‚Æ‰®“v\í\Úu\Ë\ğ\ğ\ğµCCCIÏÉ„şb•\Ğ\İW\ÆQ¼\Â\ïı\×Å¨ørøxu6m#€\éP\Æ9;\æ˜c¾x\ò\É\'?Ø©+ş›6mZ\è\Üÿœ>7\õ	ı\Å*¡º¯Œ¢ø’~Wø‹Q±–ù¤u C\Ê8\ÇQ\0³g\Ïş\ØÜ¹s·O\÷{ü\Ç\ß\Ã+üS#\ô«„~\è¾2.ˆŒı\Å*c?\Ğ)eœ\ã(ˆø^û£>z\Ûyç·~Çi~Ÿ’|\ğ\ê\ñO\é\ß\ä=üSg\áW¬²\ğƒ\î+ã‚¨Ù±ÿ\á7T\Ö^ûù\ÊÍ—Ÿ9V\ñ\ëx_ºŸj¯Œı@§”q£@†††~!t\ÂsFFFú\Â¾p\Ç\æÍ›ŸHı^<±nİº+\Ï8ãŒ±O\èÇ‰\ÇKŸƒ}kvá§ºS~\Ğ}e\\53\ö\ïØ¶®rÓ¥§W~ø­S~ª\â}q[º¿j½Œı@§”q£€†‡‡_\Z:\ãù!´oz\ï{\ß{\÷g?û\Ù\ëW­Zuã­·\ŞzÏ–-[Ç¦M›\Ö\Ş|\ó\Í×…Iq\Ù\'?ùÉ¥\á17†\Ç\ÜŸ“\æ5³\ğS\İ+?\è¾2.ˆšû\ï½ù\âI¿Z\÷\İ|É¤ıU\ëe\ì:¥Œs\Å\ö´™3gş¯ş?588xq\è ×„º7v\Ô\ñ\Ûk\âıq{\Ü/\îŸ€©kfá§ºW~\Ğ}e\\53\öÿx\ÅY“\Â~µ\â¶t\ÕzûN)\ã$–/_¾\Û\'8£B;¬_\æœ¡\ëÊ¸ j&\ôß´lá¤°_­¸-\İ_µ^B?\Ğ)eœ\ã€ÄªU«\Öû[\ÍÅ¨»\ï¾û+\Ëü­f\èº2.ˆ„şb•\ĞtJ\ç8 1::z\ÄÊ•+Ú°a\ÃWü{S\á\ç¾á®»\îº(,ú\î	uh\ÚF@g•qA\ÔLèŸÖŸ†ıj\Åm\éşª\õúN)\ã\Ùz\ğÁ\å’K.Yu\î¹\ç\î\\¸p\áS,¨¼\ï}ï‹Ÿ‡P9\á„*\'t\Ò\î~\ô£Oy\æ™\÷Ÿr\Ê)§…‡\ì_}lš\ñ\ns¨\Ç\âÂ£+ş;\Óûú¨\â\Ï=şü~\è2.ˆ\âØ“Ï´n¿\ò\ÜIa¿Zq[º¿j½b{¤m0\Ê8\ÇAv®¾ú\ê‹\Î>û\ì\'?şøJúK–,©¬Y³¦r\ÇwT¶n\İ:\ö\÷\r\ãmü>\Ş·\Çı\æÎ[9\õ\ÔS·\r\r’³Ğ€V•qüh&\ôo]wc\å¦\Ë>>)\ğ\Çû\â¶t\Õz	ı@§”qƒlü\èG?úøg>\ó™\';\î¸\Ê×¿ş\õ\Ê\æÍ›\Ç~³\âş\ñq1üÏ›7\ï\ŞÁÁÁ_KŸ£ŸĞ€V•qüh&\ôÇº\óº/N\nı\ñ¾t?\Õ^	ı@§”qƒ¾\òú\ßş\ö·7Ä°ÁTv\ìØ‘\æù)‰Ç™3gNedd\äS\é\ó\õ\ZĞª2M…ş]»*·ÿ\à¼I¡?\Ş·M\Ú_µ\\B?\Ğ)eœã ¯…ŒşK_ü\âÿû\ß_Y»vmš\ß\Ûÿ#a\îÜ¹\ßKŸ·Ğ€V•qü\ØW\èß±m]\å¶+O\nüÕŠ\Û\â>\é\ãTk%\ôR\Æ9úV\Èå¿´hÑ¢İ§vZeÛ¶mifŸ\ñ¸\ñış\ï~\÷»\ïHŸ¿\èh@«\Ê8~4ı»vU6\Ü>Zù\Ñw?:)\è§\÷‰ûº\ê\ß~	ı@§”qƒ¾\òø^x\á\ã1\ğ\Ç\ÅA\'\Å\ã\ÇOúŸ={\ö²\ô<ŠÌ€´ªŒ\ãG½Ğ¿¯«û\ÊUÿ\öK\è:¥Œs\ô¥øş|\à»ÂŸŠ\Ï3w\î\Ü\İCCCŸLÏ¥¨h@«\Ê8~\Ôı\Í\\\İoT\ñ±\é\ñT\ó%\ôR\Æ9ú\Î\õ\×_¿0¾\×~º\ßÃ¿/\ñùfÏ½û\È#|yzNEd@ZU\Æ\ñ£^\èOƒüT+=j¾„~ S\Ê8\ÇAß‰–/~º~/œw\ŞyO†\â†\ôœŠÈ€´ªŒ\ãG½Ğ¯zWB?\Ğ)eœã ¯\\}\õ\ÕÅ«üÛ·oO\óxW\Ä\ç‰/\ó?$=·¢1 ­*\ãø!\ô«„~ S\Ê8\ÇA_9ûì³Ÿ\\ºtišÅ»\ê‚.x<·¤\çV44 Ue?„şb•\ĞtJ\ç8\è=\ô\Ğ\ËN8\á„Ê¦M›\Ò\ŞU\ñù‡‡‡ŸZ¸p\á3\Òs,\ZĞª2B±J\è:¥Œs\ôo}\ë[+,Xf\ğx\ï{\ßû\Ä\Ğ\Ğ\Ğ\ñ\é9‰\rhU\Ç¡¿X%\ôR\Æ9úÆ¹ç»kÉ’%iş\î‰\ó\Ï?g0V§\çX$4 Ue?„şb•\ĞtJ\ç8\è.|jÍš5iş\î‰U«V\í¼?=\Ç\"1 ­*\ãø!\ô«„~ S\Ê8\ÇA\ß8\õ\ÔS+k×®M\ówO\Üz\ë­O„\ã\á\ô‹Ä€´ªŒ\ã‡\Ğ_¬úN)\ã}#~ˆ\ß<\æ\ïØºu\ë\î0`<™c‘Ğ€V•qüú‹UB?\Ğ)eœ\ã oŒŒŒTx\â‰4\÷D8ú=`ıü€\â*\ãø!\ô«„~ S\Ê8\ÇA\ß8şø\ãs¥\ó\æ\Í\ñ\åı®\ôY*\ãø!\ô«„~ S\Ê8\ÇA\ßøĞ‡>´»(\ï\é¿\å–[\ñ~ We?„şb•\ĞtJ\ç8\èû\ØÇ,Ê§\÷_~ù\åø\ô~ We?„şb•\ĞtJ\ç8\èÿø\Ç\ï_²dIš¿{\âŸøÄº0`¬NÏ±Hh@«\Ê8~ı\Å*¡\è”2\Îq\Ğ7,X\ğ\ÑPiş\î‰\á\á\áCCCÇ§\çX$4 U9\áß²\"ÔŒ\ôş”\Ğ_¬úN\Éiƒ\ì,\\¸\ğ\Ç{leÓ¦Mi\ïªu\ë\Ömÿ©x>\é9‰\rhUN\ãGü·Œ\×^Ã¿\Ğ_¬úN\Éiƒ,z\ê©[–.]š\æ\ğ®ú\Ä\'>qW,nIÏ­hh@«r\Z?&„ş½†¡¿X%\ô’\ÓY\Z\Z\Z:d\îÜ¹•\íÛ·§Y¼+x\à‡\ãUşx\é¹\rhUN\ãG\Ğ_7üı\Å*¡è”œ\æ8\ÈÖ¼y\ó\î½\à‚\Ò<\Ş\ó\çÏ¿\'7¤\çTD4 U9u\Â~Zc\á_\è/V	ı@§\ä4\ÇA¶<\òÈ—Ï™3§²v\í\Ú4“w\Ôu\×]wW¼\ÊŸ?=§\"2 ­ªŒ³/¡¿X•¶\êm¥c\ô3}\Zú\Ä\È\ÈÈ§?şøÊ¶m\Û\Òl\Ş›7o~0O„\Ğÿ\É\ô\\ŠÊ€°\×ÿÀX1\Ë\Ëû[®\ô‡\õ¹Ñ§¡Ì;\÷{\ñOø\Å\ÅA\'\í\nfÏıp .MÏ¡\Èh\0uCÿO…ıª~ı\ßı\îw+¯x\Å+*\ÏzÖ³*¿\ó;¿SY³fÍ¤}ú­„şâ° 7ú4\ô™w¿û\İwœt\ÒI»;u\Å?^\áü·¦\Ï]t4€Ÿ\nıu\Ã~U?‡ş×¼\æ5•g>\ó™Õ \\ù\í\ßş\íIû\ô[	ı\Åa=An\ôi\èC!”/›;w\î\î\é~|œ\ÕgWø«h\0cc\á^\Ã~U?‡şj\İu\×]c¡ÿ\àƒ´­\ßJ\è/\ë	r£OCŸŠ\ïµ\á\÷y\ç\÷\ä;\Òü>%Û¶m{8~JÿøŸ\æ\ë›\÷\ğ§h\0\Í\Ë!\ô\ìcı\ï|\ç;\'m\ë·ú‹\Ãz‚\Ü\è\Ó\Ğ\Ç-ü\ß022²û_ø\Â\ã›7oN\óü^­_¿~ûgœ1\ö	ı\ñ8\ñx\és\ô\Z@\óú=\ôÿ\ë¿şkeÿı\÷¯¼\à/¨\Ü~ûí“¶\÷[	ı\Åa=An\ôi\È@í‡„_\æ[bx\ï{\ßû\Äy\ç\÷\èªU«v\Şz\ë­OnÙ²ew\È\÷»7m\Ú\ô\Ä\Í7\ßüHXTlı\ä\'?¹~xxxgxLû\ñq‡¤\Ç\ìG4€\æ\õs\è¿\ò\Ê++p@\å·~\ë·\Æşœmº½K\è/\ë	r£OC^\öŸ9s\æ	\á{\õ\à\à\àı\á6~ _|~ü@§xû\ğøı«\ã~qÿ\ô\0ıÌ€Ğ¼~ıo{\Û\Û*oû\Û+>ø\à¤mıZBqXO}\ZÈ†\r yıúŸÿü\ç\Ç\ñ¾V§vÚ¤}ú­„şâ° 7ú4\r\Z@\óú9\ô\çXBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ	ı\Å*¡¿8¬\'È>\rdÃ€\Ğ<¡¿X%\ô‡\õ¹Ñ§l\Ğ\0š\'\ô«„şâ° 7ú4\r\Z@\ó„şb•\Ğ_\Ö\äFŸ²a@h\Ğ_¬ú‹\Ãz‚\Ü\è\Ó@6h\0\Íú‹UBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ	ı\Å*¡¿8¬\'È>\rdÃ€\Ğ<¡¿X%\ô‡\õ¹Ñ§l\Ğ\0š\'\ô«„şâ° 7ú4\r\Z@\ó„şb•\Ğ_\Ö\äFŸ²a@h\Ğ_¬ú‹\Ãz‚\Ü\è\Ó@6h\0\Íú‹UBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ	ı\Å*¡¿8¬\'È>\rdÃ€\Ğ<¡¿X%\ô‡\õ¹Ñ§l\Ğ\0š\'\ô«„şâ° 7ú4\r\Z@\ó„şb•\Ğ_\Ö\äFŸ²a@h\Ğ_¬ú‹\Ãz‚\Ü\è\Ó@6h\0\Íú‹UBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ-_¾|\÷Î;\'…O\Õı\n\í°>„ş]i\Ñ\Ö\äFŸ²a@hŞªU«\ÖoÜ¸qR\0Uİ¯»\ï¾û+!\ô_“¶½a=An\ôi 4€æ±r\åÊ‡6lØ°\Åÿ\ŞTø¹o¸ë®».\nÿP‡¦mDoXO}\ZÈ†\r`jbĞŒW˜C=\ßS®º^\ñ\çşXO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§š8 \ô{¥ÿ&\0€©° 7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@ ˜*•\Êk×®ı\Ú\êÕ«_¾|yeÙ²eª\Ë~\î»W­Zµ~tt\ôˆ´}È‹\õ¹Ñ§\Z@1…À¿$\Î\ÊÆ+;w\î¬<\ö\Øcª\Ë\î\ñ\ç¿r\åÊ‡–-[vh\ÚF\ä\Ãzˆ\Ü\è\Ó@ ˜\âş8\Ó ªº_6l\ØBÿ5i‘\ë!r£O5€bŠ/\éw…¿\Û!„ş]i‘\ë!r£O5€bŠ\ï)OÃ§\ê]\Å\öHÛˆ|X‘}\Z¨1 \0S³¡ÿ\á7T\Ö^ûù\ÊÍ—Ÿ9V\ñ\ëx_ºŸj¯„ş¼Y‘}\Z¨1 \0S3¡Ç¶u•›.=½\ò\Ão\òS\ï‹\Û\ÒıU\ë%\ô\ç\Ízˆ\Ü\è\Ó@ ˜š	ı\÷\Ş|\ñ¤À_­ûn¾d\Òşª\õú\óf=Dn\ôi Æ€\0PLÍ„ş¯8kRØ¯VÜ–\î¯Z/¡?o\ÖC\äFŸj\0\Å\ÔL\è¿i\Ù\ÂIa¿Zq[º¿j½„ş¼Y‘}\Z¨1 \äoıú\õ¿t\â‰\'^ÿ\ÊW¾\ò\ñ8 ¶·\êRı\Ì\Ïü\Ì\î_ù•_y\àE/z\Ñ\'\Ã\÷O\ß¦@\è/V	ıy³\"7ú4Pc@\È[üox\Ã×»\ŞU¹\ö\Úk\Ç®tOüyÇŸû\Û\ßş\ö\Ç<\ğÀ\ì\'ø3Í„şøiıiØ¯VÜ–\î¯Z/¡?o\ÖC\äFŸjyû\à?xı¡‡šfQz\àu¯{İº\Ğ$§¦m4úo¿\ò\ÜIa¿Zq[º¿j½„ş¼Y‘}\Z¨1 \ä\íÕ¯~\õ\ã\ñJ3½\÷ƒü`Sh’k\Ó6‚Fš	ı[\×\İX¹é²O\nü\ñ¾¸-\İ_µ^BŞ¬‡È>\r\Ô\ò\ö\Ìg>sl±J\ï\ÅvM²+m#h¤™\Ğ\ë\Î\ë¾8)\ô\Çû\ÒıT{%\ô\ç\Ízˆ\Ü\è\ÓPR\á—E\0\öQ+\Ò\Ç\Ñ\×\Ò\ìI\Å\öH\Zi*\ô\ï\ÚU¹ı\çM\nı\ñ¾¸m\Òşª\åú\ó& ‘}\ZJ*ü\òÏ¨\òÓš‘>¾–\æNz(¶G\Ú@\ĞÈ¾Bÿm\ë*·]¹xR\à¯V\Ü\÷I§Z+¡?o¹Ñ§¡\Ä\â•ü:A\ßUş|¥¹³®¸_ZL¿\ñŸ-4¥a\èßµ«²\á\ö\ÑÊ¾û\ÑIA?­¸O\Ü\×Uÿ\öK\èÏ›€Dn\ôi(±x%¿N\Øw•?_i\îÜ«Å‹\Ó\Ã;,\İ\Ä4ˆ?Û´ ‘z¡_W\÷•«ş\í—ĞŸ7‰\Ü\è\ÓPr\ñŠ~À¿\"İ,¤¹³¡İ»wW^ò’—Œ\Óÿ\÷O73\r\â\Ï6m h¤^\èo\æ\ê~£ŠM§š/¡?o¹Ñ§¡\ä\âı:¡FºYHsgCW]u\ÕX(ı\õ_ÿ\õt\Ó$ş|\Ó‚F\ê…ş4\ÈOµ\Ò\ã©\æK\èÏ›€Dn\ôi ½\Ú\ï*¾\Ò\Ü\Ù\Ğé§Ÿ>JO>ù\ät\Ó$ş|\Ó‚F\ê…~Õ»ú\ó& ‘}\ZH¯\ö\ÏH·“4w6\ô¶·½m,”^v\Ùe\é&¦Iüù¦\rı\Å*¡?o¹Ñ§1Õ«ı\éıd%Í\r½ø\Å/¥\ëÖ­K71M\â\Ï7m hD\è/V	ıy³\"7ú40¦zµ?½Ÿ¬¤¹³¡<p,”>şø\ã\é&¦Iüù¦\rı\Å*¡?o\ÖC\äFŸªI74w\ÒC±=\Ò‚F„şb•ĞŸ7‰\Ü\è\Ó@ƒş\ÆP\'\ß\nşyJs\'=\Û#m hD\è/V	ıyÈ>\rT5\è§ß“4w\ÒC±=\Ò‚F„şb•ĞŸ7‰\Ü\è\ÓPn~£û\éoiî¤‡b{¤\rı\Å*¡?o¹Ñ§¡¼\ö\ì\÷µş“\æNz(¶G\Ú@Ğˆ\Ğ_¬ú\ó& ‘}\ZÊ©\Ù@\ß\ì~\ô‡4w\ÒC±=\Ò‚F„şb•ĞŸ7‰\Ü\è\ÓP>S\r\òSİŸ\âJs\'=\Û#m hD\è/V	ıyÈ>\r\å\Òj€o\õqKš;\é¡\ØiA#B±J\è\ÏGC+b \ÚG­HıD\è‡\òh7¸·ûxz/Í\ôPl´ ¡¿X%\ô\ç#„¡uB~Z3\Ò\ÇA?ú¡¦+°O\×q\è4w\ÒC±=\Ò‚F„şb•ĞŸ—x%¿N\Ğw•Ÿlı¿\é\ê\Ó}<º\'Í\ôPl´(Ÿ\ñ°1#½?%\ô«„ş¼\Ä\ßÁ:a\ßU~²!\ôC\Ş:\Ğ;u\\:+Í\ôPl´(Ÿ‰W\÷.„şb•ĞŸŸ\ñ\ßÁ4\ğ¯H\÷ƒ~$\ôC¾:\Ì;}|¦_š;\é¡\ØiQ>\õBF½\ğ/\ô«„şü\Äß»:¿3\Òı 	ı§‰üi¡\Ş=~Ûz\ÇüûKš;\é¡\ØiQ>uBF\İ\ğ/\ô«„ş<ÿ\Ş\Õ~\Ó\íĞ¯„~\ÈO\Zø?·ßpo[\rş{;\à\ß?\Ò\ÜI\Å\öHˆ\ò©\ö\Ó\ZÿB±J\è\ÏSü]›\ğ»7#\İıJè‡¼¤ü\İû\í	\ÕJ{3&şj\óS{L~^Š)Í\ôPl´h*—¶\Ò\à©zWB¾f_\íO\ï‡~¦OC>\ê\ïz}*Á¿\Ş\ãÿyüşT½\ç§X\Ò\ÜI\Å\öHˆ\ò-L\Ò`?¡b\ğ˜Q\İÏ•şb•ĞŸ‡9s\æü|ø=;jpppQ¸]\ê\Î\ğ\õ®\ñ\ßÁG\ã\÷\ñş\ñ\íG\Åı\Óc@?ˆ}:½\è?{\Ü\õ‚{3Á¿\Ş\ã\Zşª½½—\æNz(¶G\Ú@”oa’ıIa¿ªŸCÿw\Ş9q«tŸ~+¡¿¿\r\r\r½%ü-\r\õX\ßÁ½U\Üi||zL(²\Ø\Óû€ş\ÒLĞ®\à\÷ü\ë\í¿¯À_\Õ\Ìù\Ğiî¤‡b{¤\rDù&\ÂDİ°_\ÕÏ¡ÿ«_ı\êX?\ò\È#\'m\ë\×úûS\ë¿~Ï®ª\æ[©«\â\ñ\Ò\ç€\"Š}6½\èS	\Ø\õ‚|½\à_o¿f\ÕTÎ‹\îIs\'=\Û#m Ê·0™µ°_\ÕÏ¡ÿ¤“N\Z\ë\ïŸş\ô§\'m\ë\×úûËœ9s588ø\áwmw\Z\Ş\çÍ›Wù\ò—¿\\Y³fM\å;\î¨lİºulŒ·\ñûx\Ü\÷KŸ>\'I\Ù\æV\ÈI+Áº^ Ÿü\ëmŸj\à¯j\åühA³¡a?¡¿Pb{¤\r„…I#ıúÿ\àş`¬¿?\ïyÏ«¼\à/¨\\~ù\å“\öé·º\ì²\Ë\ô\Ó>Æ”\r\r\rıpbX®œşù•û\î»/š\÷*\îŸ„ÿ\â\ó¤\Ï\rEan…ş\ÔN ®\ì\ã\÷û×¹¿\ÕÀ_\Õ\ÎyÒ¤	‹}…ÿtıB\Å\öH“Fú9\ô¿úÕ¯®œs\Î9•+®¸b¬ß¿\â¯˜´O?\ÕÎ;+G}ts\ï	\õıP…ú\ë,ÿlpp\ğ]\á\ö•>\ğ­B[¼<Ôº‰ı¬³Îª\Ü{\ï½\é<%\ñ\ñ\ñ8\ê\Şø|\é9@˜[¡ÿLG®üoM¾o7\ğWM\Çù²É¢co\á?]·\ĞC±=\Ò\ÂÂ¤‘~ı\Õz\ä‘G\Æúı³Ÿı\ìI\Ûú­.½\ô\Ò\ÊÀÀÀ‹C\È{Cú³\Ã\íGB\ß=/|ı­pû£P…û¶Ç¯\Ç\ï;/\î\÷\r_¿qxxø—fÌ˜ñŒ´™>³\ö\\\á¯şxuş\â‹/N‡\â¶\Ä\ã%Wı\ïÏ›\ôš¹ú\Ët\èzÁº\Õt7‰	‹´\Ò\ğŸ®W\è¡\ØÚ†q&\õ\õs\èı\ë__9\ğÀ+\ßø\Æ7\Æúış\áNÚ§ßª™\÷\ôü\Ü\ìÙ³3\ô\éC\ã«\0\Â\í_\Ï\Ú\óª€ø\ê€ø*øI\ğ^-\Ğ\ñ=\ö_\Òÿ\÷¼§rı\õ×§\Ã\ğ´ˆÇÇ¯>W¨¼ÇŸ¢1·Bÿ\èDp/\éO¯\ğ\Ç\ï\ãıÓ­\ç\Ï~{\rıiøO\×*\ôPl¤)\Ù\ÏÂ¤‘~ıË—/¯ü\ÆoüFå ƒª¼\éMo\Zû~\é>ıVÍ„ş}‰Wú½Z 3\Æ?´olŒW\â;ø«\â\ñ\'^\ñÏŸ\ô’¹úÃ³\÷\Û˜ON7´aoWú\ë}ªÿt8yBU]®ı„şBI\ÛGıg¥ıús¬\éı\Í\ğj©\Ú\ógùª\'—\\rI:üvD|	\ã\ØS\ñ<\Òsƒ^‰ı2½(¦\é¼R^/\ğ§Wü§;øO\çù3Á„EFZ+fyya\Å\ö˜\Ğ6°WB±ª[¡_\Ê\òj\ğo|zz_#\á\ßuUuŒ¶\×MÉ‡û]•\ôJ\ì“\é}@qMGp®ø\ã{ø\ë}zÿtÿ\é8o\Z˜°Àh\ö«\Ò\õ	=\Û#m hD\è/V%\ô7#‡WŒŸ\÷\â\ğoù\Ùt\ÛD3g\Îü£\ê\\_n\ß\î§\ôOU|¾‰/\óç“#\ôB\ì\é}@±µ şj°¯·½\İ\à\ß\ÎùÒ„\ê\âbV\ã°_•®O\è¡\ØiA#B±ªŸBÿ¾\ôÃ«\Âs5>\Ïİ¹· ¶/­Î‰\çŸ~:\ìvE|\Ş	\ó\ò\Ò\ô¡bL\ïŠ¯• ]/\Ğ\×û”şzûµ\Zü[9O¦hÖ¾\Ã~Uº6¡‡b{¤\rıÅªœB3zıjpŒ¤\ã\åıS<§‰û?wü<\Æ\ö\é\öUşªø¼\Î\õ±x^\Ïz!\ö\Ç\ô> ?L%P\×\ò\õU½ı§\Zü§r~tGº6¡‡b{¤\rıÅª²…ş}\é\ô«\Â>WN\ÒÕº7ş§Â„}ªn›7o^:\ävU|ş	\çy\Ô\Ä\ËDaÛŒP+\ÒûaºÅ¾˜\Ş\ôf‚u½\0¿·À_U\ïq\ÍÿfÎ‹\îK\×%\ôPl´ ¡¿X%\ôO];¯_\ß? \'U\Ø\çK\á\Ø\Ï_/®\Ş\÷¥/})r»*>ÿ„s\\œş,f‡ı\ê>\év˜nú\ô¿½\ìzÁ½™À_U\ï\ñû\nş{;z+]—\ĞC±=\Ò‚F„şb•\Ğ?ı\ö\òjo‡\Û\İƒ~z \ìk\õûë®».r»*>\õ\\\ÂyVÿ³’°_­‰?\èı\ò\Ğ(h¿{¿\ÖU½\à\ÌO\í\ñŸ\ZÅ®K\è¡\ØiA#B±J\è\ïV~9\r\ÉI=\Z\ê¾POU\ï[»vm:\ävU|ş	\ç_½P7\ìW+ı7\Ãt\Ó\Ï \õ\÷\ÄÀ\ŞJ\à¯j\æ8\õŸbI\×%\ôPl´ ¡¿X%\ôw\ÏÌ™3ÿ \r\É\ãŸ\ğùPo^¸paü“\Ã\ñ\Ãş\â}c\Ûx\àt\È\íªøü\Î\÷\É\ôü\ÓJÿ\Í0\İ\ô3\ÈK½\àz¼2_/¨O\ÅŞS\ïy)t]B\Å\öH\Zú‹UB\÷„0\ôx8~\"~`ø~h``\àÀt¿‰\áú‰\'H‡Ü®ŠÏŸ{¥z]\é\ï\ĞßºÀ»ı|´.]—\ĞC±=\Ò‚F„şb•\Ğ\ß=!\èÿI\Èù>22\ò‹é¶‰Šz¥ü¼¼¼€i×­ Ş­\çaz¤\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹gÖ\÷Î…è¢½§\Â9\Ö\rÿÿ\00\ä>>\Ó/]—\ĞC±=\Ò‚F„şb•\Ğ_<\ñS\ò«!º¨Ÿ\Ş_•†ÿt;\0LE§‚y§Kg¥\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹\'„\ç\Å\Õı¥/})r»*>ÿ„+ù‹\Ós­ª†ÿ\ô~\0˜ª\é\è\Ó}<º\']—\ĞC±=\Ò‚F„şb•\Ğ_<!<U\r\Ú\ó\æ\ÍK‡Ü®:\ñ\ÄwOıG¥\ç\n\00]A}ºCo¤\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹gxxø¹!`?V\r\Û\÷\Ş{o:\ìvE|\Ş	ÿ±x^\é¹@§´\Ø\Û}<½—®M\è¡\ØiA#B±J\è/¦²—V\÷ùçŸŸ»]ŸwB\è_š#\0tZ«Á½\Õ\ÇQ,\éÚ„Š\í‘64\"\ô«„şbš9s\æU\÷\ğ\ğp×¯\ö\Ç\ç\Z\Zª½´?Oz\0\Ğ\rS\r\ğSİŸ\âJ\×\'\ôPl´ ¡¿X%\ôW\ÛWUC\÷Yg•½Ÿo\ÂUş«\Òs€nj6\È7»ı!]Ÿ\ĞC±=\Ò‚F„şb•\Ğ_\\CCC¿\÷S\Õ\ğ}\É%—¤\ÃoG\Ä\ç™øŸŠç‘\0tÛ¾ı¾¶\Ó\Ò5\n=\Û#m hD\è/V	ı\Å688ø_\æı\õ×§C\ğ´ŠÇŸø²şøü\é9@¯4\n\öî§¿¥\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹mÎœ9\Ï\n\áû†j\Ï{\ŞÓ±\à¯\öÊ‚ø¼\ñù\Ós€^J~ú=ùH\×*\ôPl´ ¡¿X%\ô_\ß/\nu\ï\Ä+ş_|q:·%o\âş\ñ\ç{Qz.\0PÕ \òø­ÀŸ§t½B\Å\öH\Zú‹UBü\åƒ¬øa{\í~ª||\ò¡}c?>_z\0P$‡\ÄI+Ş¦\ÈFºn¡‡b{¤\rı\Å*¡¿\Ä+\ï³&¼\Ô?V¼\êşù\çW\î»\ï¾thŞ«¸|\\ru\ì%ı\ñy\Ò\ç€\Âı\ä+]¿\ĞC±=\Ò‚F„şb•\Ğ\ß_\â{\ì\Ç?\Ü/\r\ë•y\ó\æU¾ü\å/WÖ¬YS¹\ã;*[·n£\ãmü>\Ş·Ÿxâ‰“\ë=ü\0\ô\r¡?{Iì¤—b{¤\r,_¾|\÷Î;\'…O\Õı\n\í°>„ş]iQ|\ã\Î\ïª:á½•ºÊŸ\å \ï\ÄI,½¬¤¹“Š\í‘64²jÕª\õ7nœ@U\÷\ë\î»\ïşJı×¤mDÿaı-aÍ³4\Ôcu\Âü\Ş*\î¿4>>=&\0\ô¡?{iî¤‡b{¤\rŒ±r\åÊ‡6lØ°\Åÿ\ŞTø¹o¸ë®».\nÿP‡¦mDÿ™3g\ÎÏ‡µ\ÏQƒƒƒ‹\Â\í\òPw†ªşù½GÇ¿_>¾ı¨¸z\0\è+B\ö\Ò\ÜI\Å\öH\ö&\Íx…9\Ôc\ñ=\åª\ë\î\ñ\ç/\ğg*¬ƒfL¸ª?#\İ\0}O\è\Ï^š;\é¡\Øi\Ğ;a´bB\è_‘n€¾\'\ôg/Í\ôPl´\0\è\ä*¿«ı\0\äI\è\Ï^š;\é¡\Øi\Ğ\ÉU~WûÈ“ĞŸ½4w\ÒC±=\Ò û\Z\\\åwµ€üı\ÙKs\'=\Û#m \0º¯ÁU~WûÈĞŸ·8`wü³S\ôŞ®]»Ö‡&Ù•¶\0\İ¯\ä\×	úi\ÍH\0}I\è\Ï\Û\Ë^\ö²­\×^{mš?\é\ï|\ç;\ß\rMrm\ÚF\0tW¼’_\'\ä§\åj?\0yú\ó\ö¢½\è/\ß\ö¶·=Pºn\ëAtyh’S\Ó6 \÷¬‡\0È–I.{O?\ğÀ\ğº×½\î¾Õ«Wo\öRÿ\îÚµk×†x…?şe¡-\âU£§§\r@\ïY-“\\)Ä yJ¨5¡\ÛoÏ‡É©\îTüyÇŸ{üùü\0e=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0YÚŠ8©\í£V¤\0È™\Ğ@Â„6£N\ÈOkFú8\0€œ	ı\0d#^É¯\ô]\å\0JK\è \ñJ~°\ï*?\0PZB?\0Y‰W\ô\ë~Wù€Rú\ÈJƒ«ı3\Òı\0\0\Ê@\è ;\É\Õ~Wù€\Òú\ÈNrµFº\0 ,„~\0²T½ÚŸ\Ş\0P&\ÖC\0d©zµ?½\0 L¬‡\0˜¤R©°v\íÚ¯­^½ú\ñ\åË—W–-[¦º\\\á\ç¾{ÕªU\ëGGGH\Û\0 YB?\0“„À¿$\Î\ÊÆ+;w\î¬<\ö\Øcª\Ë\î\ñ\ç¿r\åÊ‡–-[vh\ÚF\0\0\Íú˜$^á3\r¢ªûµaÃ†-!\ô_“¶\0@3„~\0&‰/\éw…¿\Û!„ş]i\04C\è`’ø\ò4|ª\ŞUl´\0\0š!\ô0I³¡ÿ\á7T\Ö^ûù\ÊÍ—Ÿ9V\ñ\ëx_ºŸj¯„~\0 UB?\0“4úwl[W¹\é\Ò\Ó+?ü\Ö)?U\ñ¾¸-\İ_µ^B?\0\Ğ*¡€Iš	ı\÷\Ş|\ñ¤À_­ûn¾d\Òşª\õú€V	ı\0L\ÒL\èÿ\ñŠ³&…ıj\Åm\éşª\õú€V	ı\0L\ÒL\è¿i\Ù\ÂIa¿Zq[º¿j½„~\0 UB?\0“ı\Å*¡\0h•\ĞÀ$Í„şøiıiØ¯VÜ–\î¯Z/¡\0h•\ĞÀ$Í„şÛ¯<wRØ¯VÜ–\î¯Z/¡\0ø\íÜ±J¤W\Ç\áF,±,¬­ll¼)¼½‹m\ö\"R¤O\ÒØˆE(ƒ! d·\Ê\ÑÀ\î &„˜\Â\Ù&_\æ¤ù^e\'Çƒû~\É\óÀŸ™3\Îú\ãSk‰~\0‚y¢ÿ··?v¯¿y‚¿\ÜWëŸ·ú‰~\0 –\è ˜\'ú\Ë~ş\á‹ı\å¾ş9{\ÚD?\0PK\ô\Ìı“I\÷\æ»\ÏC\ô—û\Êc\á¼UO\ô\0µD?\0Á‡¢ÿ\Ï\ß\ßv?}ûYşV+gúÏ³º‰~\0 –\è x4ú\'“nü\æ¼{\õ\õ‹úı•3å¬«şOŸ\è\0j‰~\0‚‡¢ÿCW\÷›«şOŸ\è\0j‰~\0‚‡¢«û­<·ÿz6ÿD?\0PK\ô<ııÿ·ë¿\Í?\Ñ\0\Ôı\0E¿}¼‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸkå›µ™™™Y\íú?\ë\ğ?\'úsÍ•~\0\0\0šr\ô\ß\İ\İuûûû\İ\ê\êj·±±Ñœœ„3C›\è\0\0 ™!Gÿ\á\áa	\ä\î\ô\ô´[^^\îvwwÃ™¡M\ô\0\0\ĞÌ£ss³[[[\÷y¢\0\0€f†ı‹‹‹³\è\ß\Ú\Ú\ê\Ö\××»\Ñh\Îm¢\0\0€f†ı³_\ï?>>İ–ø\ïŸ\ÚD?\0\0\0\Í9ú\Ë\Õı\é—0û‡~\åvii)œ\ÚD?\0\0\0\Í9úf±tt4»\İ\Ù\Ù	g†6\Ñ\0\0@3Cş\Û\Û\Ûnoo¯[YYé¶··»\Ë\Ë\Ëpfhı\0\0\043\ä\èÿ/N\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íœıu\âÓ\Ó\Ï\á\İ4ú\'ı\Ï\0\0\0ª\\\\\\¼»¹¹	jÏ¿\ë\ëë¯¦\Ñÿ}ÿ3\0\0€*\ç\ççŸF£?\Æ\ã\ñ¯®øœM\ß\÷\ñ\Õ\ÕÕ—\Ó\àÿeºOúŸ\0\0\0T+¡Y®0O\÷¾üM¹=û\Êû^\ŞÁ\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\004&\Ì\Ç\ï\ò\÷B\Ñ\0\0\0\0IEND®B`‚',1),('12554',1,'flow_mu180u86md3e.bpmn20.xml','12553',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://bpmn.io/schema/bpmn\" id=\"Definitions_1\">\n  <process id=\"flow_mu180u86md3e\" name=\"æµ‹è¯•914\" isExecutable=\"true\">\n    <extensionElements>\n      <flowable:executionListener event=\"start\"></flowable:executionListener>\n    </extensionElements>\n    <startEvent id=\"StartEvent_mu2c2p840\" name=\"å¼€å§‹\"></startEvent>\n    <sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\"></sequenceFlow>\n    <endEvent id=\"EndEvent_mu2c2p8c0\" name=\"ç»“æŸ\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </endEvent>\n    <sequenceFlow id=\"Flow_1qxvbp7\" name=\"xiaoyuyibai\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_15cdzo6\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_1dpq6dk\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_0qm5tc6\"></sequenceFlow>\n    <userTask id=\"Activity_0qm5tc6\" name=\"1\"></userTask>\n    <sequenceFlow id=\"Flow_0icqanz\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_1mzuxt6\"></sequenceFlow>\n    <parallelGateway id=\"Gateway_15cdzo6\"></parallelGateway>\n    <userTask id=\"Activity_1mzuxt6\" name=\"2\"></userTask>\n    <sequenceFlow id=\"Flow_1aqie0c\" sourceRef=\"Activity_0qm5tc6\" targetRef=\"EndEvent_mu2c2p8c0\">\n      <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${tes &gt; 100}]]></conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_016it02\" sourceRef=\"Activity_1mzuxt6\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <exclusiveGateway id=\"Gateway_0a6n8eh\"></exclusiveGateway>\n    <sequenceFlow id=\"Flow_13gvmcu\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_02klewn\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_1kkbmin\" sourceRef=\"Activity_02klewn\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0t3saos\" sourceRef=\"Activity_0ylt45t\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <userTask id=\"Activity_02klewn\" name=\"6\"></userTask>\n    <userTask id=\"Activity_0ylt45t\" name=\"5\"></userTask>\n    <sequenceFlow id=\"Flow_0dizg8q\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_0a6n8eh\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0e4xs2a\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_1jl42bt\"></sequenceFlow>\n    <manualTask id=\"UserTask_mu2c2p880\" name=\"3\"></manualTask>\n    <sequenceFlow id=\"Flow_0q59g41\" sourceRef=\"Activity_1jl42bt\" targetRef=\"Activity_0ylt45t\"></sequenceFlow>\n    <subProcess id=\"Activity_1jl42bt\" name=\"7\">\n      <startEvent id=\"Event_032ztbm\"></startEvent>\n    </subProcess>\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_flow_mu180u86md3e\">\n    <bpmndi:BPMNPlane bpmnElement=\"flow_mu180u86md3e\" id=\"BPMNPlane_flow_mu180u86md3e\">\n      <bpmndi:BPMNShape bpmnElement=\"StartEvent_mu2c2p840\" id=\"BPMNShape_StartEvent_mu2c2p840\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"142.0\" y=\"142.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"149.0\" y=\"185.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"EndEvent_mu2c2p8c0\" id=\"BPMNShape_EndEvent_mu2c2p8c0\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"812.0\" y=\"382.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"778.0\" y=\"393.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0qm5tc6\" id=\"BPMNShape_Activity_0qm5tc6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"780.0\" y=\"90.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_15cdzo6\" id=\"BPMNShape_Gateway_15cdzo6\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"545.0\" y=\"75.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1mzuxt6\" id=\"BPMNShape_Activity_1mzuxt6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"631.0\" y=\"170.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_0a6n8eh\" id=\"BPMNShape_Gateway_0a6n8eh\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"225.0\" y=\"365.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_02klewn\" id=\"BPMNShape_Activity_02klewn\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"200.0\" y=\"500.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0ylt45t\" id=\"BPMNShape_Activity_0ylt45t\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"610.0\" y=\"340.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"UserTask_mu2c2p880\" id=\"BPMNShape_UserTask_mu2c2p880\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"248.0\" y=\"102.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1jl42bt\" id=\"BPMNShape_Activity_1jl42bt\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"370.0\" y=\"350.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_032ztbm\" id=\"BPMNShape_Event_032ztbm\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"222.0\" y=\"182.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1g1p7by\" id=\"BPMNEdge_Flow_1g1p7by\">\n        <omgdi:waypoint x=\"178.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"142.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"248.0\" y=\"142.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1qxvbp7\" id=\"BPMNEdge_Flow_1qxvbp7\">\n        <omgdi:waypoint x=\"348.0\" y=\"134.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"548.0\" y=\"103.0\"></omgdi:waypoint>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"54.0\" x=\"404.0\" y=\"130.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1dpq6dk\" id=\"BPMNEdge_Flow_1dpq6dk\">\n        <omgdi:waypoint x=\"591.0\" y=\"96.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"720.0\" y=\"70.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"780.0\" y=\"103.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0icqanz\" id=\"BPMNEdge_Flow_0icqanz\">\n        <omgdi:waypoint x=\"570.0\" y=\"125.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"570.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"631.0\" y=\"210.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1aqie0c\" id=\"BPMNEdge_Flow_1aqie0c\">\n        <omgdi:waypoint x=\"880.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"848.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_016it02\" id=\"BPMNEdge_Flow_016it02\">\n        <omgdi:waypoint x=\"731.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"382.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_13gvmcu\" id=\"BPMNEdge_Flow_13gvmcu\">\n        <omgdi:waypoint x=\"250.0\" y=\"415.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"500.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1kkbmin\" id=\"BPMNEdge_Flow_1kkbmin\">\n        <omgdi:waypoint x=\"300.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"418.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0t3saos\" id=\"BPMNEdge_Flow_0t3saos\">\n        <omgdi:waypoint x=\"710.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"812.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0dizg8q\" id=\"BPMNEdge_Flow_0dizg8q\">\n        <omgdi:waypoint x=\"298.0\" y=\"182.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"298.0\" y=\"274.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"274.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"365.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0e4xs2a\" id=\"BPMNEdge_Flow_0e4xs2a\">\n        <omgdi:waypoint x=\"275.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"370.0\" y=\"390.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0q59g41\" id=\"BPMNEdge_Flow_0q59g41\">\n        <omgdi:waypoint x=\"470.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"540.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"540.0\" y=\"380.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"610.0\" y=\"380.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>',0),('12555',1,'flow_mu180u86md3e.flow_mu180u86md3e.png','12553',_binary '‰PNG\r\n\Z\n\0\0\0\rIHDR\0\0ı\0\0N\0\0\0\åg\0\0Q\ÎIDATx^\ì\İ\rœ\ÜU}/~D‹\"•Vo\í½½\Újk¯½µj[«­WKª\õ\Û\Âf³cƒ€\ÕJ\"\ñj@[Ë¿µ…„Š´µŠ(UsM½\à„lb\ÃS\n\"`	I\ÈHx\Ê\Üs6;\Ó\õ\ìL2;³3\ó›\ó{¿_¯\ïkv\ç\÷›\ßü²\ç\ì9\ç“\ß\Ì\ì~û\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0ıc\ö\ì\Ù§\÷\0\0\0}f``\à€Y³f½&\Ô	ƒƒƒÿ\ê‡\áë§†††>%ü\0\0@Ÿ8\î¸\ã~&„ú\ß¡ş¸P\ç‡Z\ê‘P7„ú\\\Ø\ö¾P¿;00\ğ’úÿ%Ü·!\ÔûgÌ˜\ñŒ\ôX\0\0\0@Ä€?<<ü\êŞ\rÁı¼P\×Æ€Bı\á\ö\ó1Ì‡zİœ9s•>¶*<\ö•aÿ\ï†ın·’n\0\0\0:,^‰=„\ó?\r\áü\Â\íÕ¡uS¸ÿ‚P\ê\÷LÛŒ\ğØ·„\ã^\÷ıxœt;\0\0\0\ñ\ì\ô\ò‚û\ÓC\0E\àsB-µz<\à\ß\êÂ°\íÄ™3gş¯‘‘‘ƒ\ÒÇ¶c\áÂ…û‡c\ãPK\Âyüjº\0\0\0\Ó\çP\Ço\ÉP\Ú!\\ÿ\Ï\ñ°}N¨+C\í\Z\ZúP_\n_\Ï\õÆ°\ÏÏ¦\í”øv€p>Ï»9\Ü.\n\ç\ñ\é>\0\0\0´§\ZøO¿üû\\ø!@¿<\Ô\Ñ!Lÿ}\ÕW„¯·‡\Û\ÛB]¾>)\Ô!‡~øs\Ò\Ç\öBû\ãÿ\Ãÿ‡\÷\ö\Ù\0\0\0\04/½ÂŸ~O\ñ=-\å_y8„\çO‡ú^øú¡P?	\õ•°í”™3gş\áÀÀÀÏ¥,šø2ÿp\ÎKB\İ_‘ÿ\ó\"\İ\0\0€\æ4\nø\î§\÷ƒq\Ä!\Ïú›P+B=jmû_·B½yxxø¹\éûIü€¿\ğ\ïø~¨\âÿ¥\Û\0\0Ø»}û}m§Bxi½3C\È?+\à\å\áv[¸½+\ÜşŸpÿGb x^ú¸\\„\ë‡º5ü{¿ş­¯L·\0\00Y³¾\Ùı˜!\Üşr¨£B¸ıT¨e\á\ëf\íùtû¥!\ô~lxxøm\ñ½\ï\é\ãrÿ„`ø¼?Ô†\ğ\ïÿ—¦û\0\0\0°\ÇTƒüT\÷§	!¸¾8ù?	A\ö/C]\ZjK¨ûB}#\Ô\éa\Û;FFF~1}\\™\ÅÿyÅŸ\Õ_Î=û\àt\0\0€2k5À·ú8‚Nÿû\Ğ\Ğ\ĞÿA\õ¡¾jS¨\õ!\Ø_n?\êĞÿ–>ú\â•şx\Å?^ù\õşøJ€t\0\0€²i7¸·ûøR!\ô¡¡\ôŒ\ê¿¾\Ş\êşPßŒ\÷…:,\î“>©‹\ï\ñ\ï\õ?\Ï\Û\â«&\Ò\í\0\0\0e\ÑL`¯L¨Fš9NiÄ«\ó!l¾küjı%\ãW\ãUüo‡úD\ØvD¼ÊŸ>\é?\Ì0ü¬¯?\óï‡¯?\İ\0\0³fƒz3¡?j\öxY‰\ï¯~g–§‡ú¿¡Ö…\Ú2~¥9¾\Ïü\ãû\ô\Ó\Ç\Ñ.\Ü?´\Å1³\ö|\ğ\á’øg\r\Ó}\0\0\0r3•€\Şlè¦rÜ¾?!?\Ô\Û\ã\'\æ‡ú·\"\ï\rµ5\ÔeãŸ¬døú—\Ó\Ç\Ñ{s\æ\ÌyVh³‡\ö\Ùn•\ñ¯\0\0\0\å0\Õ`>•\ĞM\õø…t\Ì1\Çü—\ß\Z\Â\áGBPüz¨»\Ã\÷\Û\Â\í\å\á\ö¬P\Ã\Ã\Ã/MG±Å°\Ú\ğœ\ñ\ğÿ\áøŸ\é>\0\0\0ıª•@>\Õ\Ğµ\ò<=\ÂûsgÎœùG!.ˆ/ug¨C@\r·jpüe\áOKKŠ\í9\Ş\Ö\÷Ä—ÿÇ·¤û\0\0\0\ô“Vƒx+¡?j\õù:*„½ŸaşM!\è\ßWC\İ\ê¡P+\ÃıŸ54<<ü²ıüRˆ\ğ\Úşû¡nˆü—n\0\0\è\í\ğVC\Ô\Î\ó¶m\ö\ì\Ù‡07#„¹“Bıkøú\öp»=Ü®\n¡ÿ\ïC\Íøû	ø¥ú\Ä‡º5~\0c\è#¯L·O‡J¥rÀÚµk¿¶z\õ\êÇ—/_^Y¶l™\êr…Ÿû\îU«V­=\"m\0\0\èW\í\ïvB\Ô\î\ó7%„\÷Ÿ9s\æ„\Ğ\ö¡Ş¾\\¨\ãWq\ã{¸GÂ¶_\÷2n\Z™1c\Æ3B?y¨\r!øÿK\èS/L\÷iGüKB\à¬lÜ¸±²s\ç\Î\Êc=¦º\\\ñ\çş+W®|hÙ²e‡¦m\0\0ıf:w»¡?š\ó¨9(„²7„šÚ…¡~\ê\áP?µ8\Üÿ\îP¿B\Û\Ó\Ó\ÇÂ¾~ø\áÏ™µ\ç\Ï-n‰·\ñ#\é>­ˆWøc\àLƒ¨\ê~mØ°aKı×¤m\0\0ıdº‚\öt„ş¨¥\ó9\ì°Ã‚\×\ëC}0ùB\İ<\ğ¯\Zü‡P\ï	¡\ì7|¦[¼\Ò¯ø\Ç+ÿ\ñ\0\ñ•\0\é>©\ğ˜\Òûª\âKú]\á/F\Åv¡W\ÚF\0\0\Ğ/Z\n\Ø\rLW\è\öz^!0Bü\ï…ú@YŸ\õ£P„º&\Ôy¡\æ†m¯j&|Át‰\ï\ñ\ï\õı\ï¶pû\'\é\ö‰\Â>ßŒo#I\ï\â{\Ê\Ó\ğ©zW±=\Ò6\0€~°\×`İ‚\éı\Ñ\Øù=\ëY\Ïú£\á\á\á×†@\õg!(}.\Ô\Çşu¡\Î\õ\ŞP¿u\Üq\ÇıLz\0\è…ø\éş!\Ğ_ú\å\÷\Ã×¿Ÿn\Û\Ş¶Ub®\×o›\rı?¸¡²\ö\Ú\ÏWn¾üÌ±Š_\Çû\ÒıT{%\ô\0Ğ¦;\ğG\Óú\÷ÿ\ÍGy\ä\îP\ñ\Ê\é?…\0uü\ğ\ğ\ğ\ï\ì\í¥\ÑP\ñƒ CŸ=&û{B-	}\öW«\÷\Ï\Ú\óÊ”úc™>¶™Ğ¿cÛº\ÊM—^ù\á·Nù©Š\÷\Åm\éşª\õú\0\è7üÑ´‡ş(ÿı:s¾\Ğqs\æ\ÌyVÿ\á~s¸]\ê\Ä	?\Ö¡^7\ñ1Í„ş{o¾xR\à¯\Ö}7_2i\Õz	ı\0\0\ô“Nş¨#¡\\\'\Ï:nhh\è\âJ†Û§’\Ğ\ë\ÖøA”\Õ}›	ı?^qÖ¤°_­¸-\İ_µ^B?\0\0…?¸n``\à\ç\Òû\'h&8Oî½ªF\Ùo\ß\ç…\ÂıiuµÎ­\î\×L\è¿i\Ù\ÂIa¿Zq[º¿j½„~\0\0\nahh\èŒui¨\÷†\ïÿ\ë„\Í\ñ*b\Ì\'O¸¯4€\÷¢\ö&üwÔ®ŠB?ù\Å\ğ;¹½NØ¯\Ö\îÁÁÁ·\Æ}…şb•\Ğ\0@!„\Ğ0œ„ˆ§B\Èø^¨y\Ã\ÃÃ¿´_sW\Ê\Ó\0Ş‹j\äı\ö}şPH\á\÷\ñ3u‚şOUø]\İ~WŸ\ÛLèŸÖŸ†ıj\Åm\éşª\õú¨©T*¬]»\ök«W¯~|ù\ò\åc“„\ên…Ÿû\îU«V­=\"m\È\İ\à\à\ào§!\"©k\ß\ò–·ü\óÁ¼\å\ÏxÆ›\Ò\ÇOƒf‚{«~ú\ÖÀÀÀ\ñ¯O„\ßÁ\Ï\Ç\n\á~4Ü®ˆ¾şp{W¨-¡v‡Zûo|cR\ğL\ë\ö+Ï\ö«·¥û«\Ö+®/\Ò6 ¤B\à_ge\ãÆ•;wNš4T\ç+ş\Ü\ã\Ï\åÊ•…IúĞ´ g###‡†4\ìO¬\ÇC\0\Ùz\ÔQG\í\î@\ğ\ïT\èø)…\Ã?ü9\áw\ô/\çÎ[Ù±cÇ¤9nbm]wc\å¦\Ë>>)\ğ\Çû\â¶t\Õz	ı\0\Ô\Ä+ü1p¦“…\ê~mØ°aK˜¤¯I\Ûr4cÆŒg¿lhh\è°ÁÁÁmu‚~µîœµ\ç½ş/\ïĞŸÁ\ëD\èø)\ï|\ç;“\æµzu\çu_œú\ã}\é~ª½ú¨‰/\éw…¿\Û!LÒ»\Ò6‚~6gÎœŸ¡ıu!´¿;\ÜşU¨¥¡n	µ3\Ô!\ğ+\Ü\Ş]\'\ì?¶}l``\à\é\É!§;POw\èŸ\î\óƒ¾Cf:¯Mª]»*·ÿ\à¼I¡?\Ş·M\Ú_µ\\B?\05MMÒªke’¦-\\¸pÿ\Î_Bú;BXŸn?nW†ºü“¿¯\ru\á¬=úë¨°ı\ïx\Ç;Y}|¸oqøo~\í\Ä\çHLg°\Î\Ğ?\ç}e_\ë‰\Û\ÖUn»r\ñ¤À_­¸-\î“>NµV\Ö\0\Ô\ìk’X»v>R\Ù|\÷µ•Ÿ\\\õ•»ø&mW\í—Iš\"ÁşgCÿ\Úg‡`ş‰\è¿¾¾1|ı\è¬=W\ë\ãŸ\İ;\'\Üÿg¡\Ş¾~AzŒz\Â~\ïŸø?Ÿ\'İ§\é\n\Ø\Óú§\ë| /5\\O\ì\ÚU\Ùpûh\åG\ßıè¤ ŸV\Ü\'\î\ëªûe=@M\ÃIzBm\Ûx[\å\î¾V¹\é\Ò\Ók\ó­Wœ=i?\Õ~™¤)€§…\àı¢\Ú\ß\ê\ÏC¨ÿ‡\ğı\å¡\î\õpøşúpÿ¿†¯?¾5<<ü\ê\Ã;¬­¿??s\æ\Ì?\n\Ç\Û\Z\÷\'\é¶}˜ =¡:\ÎúZ½\õÄ¾®\î7*Wı\Û/\ë	\0j\êMÒ±İ±­²\î\Çß­\Ü2ú©ŸŒ¿}je\íµ_¨<üÿ\ëD™¤\é–\ñ?\Ç\õª¶c€uQ¨Á>ÔºP\Ë\Çÿ\Ã\í[\Ãş/{Zzœ\éÿ\Æw8ş\Óû›\Ôn\àn7\ô·ûü…z\ë‰f®\î7ªø\Ø\ôxªù²\0 ¦\Ş$ı\È\öM•¯8k\Ò+^\í\Û\Òú•S\Ù|\÷5“¥¦V&i¦[\í/˜9s\æ†\àş¾\ğ\õ9\á\ö»³\öü}\íø’ü…Z2k\ÏK\õ·¯‰~+=Fh\'x·ú\Ûy^\ÈJ½\õDº†˜j¥\ÇSÍ—\õ\05\õ&\éÿX\õ\éIo3\å}ş\í—IšV\ÄÅ‹‚û‘\ñ\ïg\íùĞ¼kB=4kÏ‡\é}/Üjş\ğ\ğ\ğ;C½4~ø^zœ>\×j\0o5\ô·ú|¥z\ë	Õ»²\0 ¦\Ş$†ùzµ\ö\ÚÏı]\İZ­ùReı­\Ë*\Û\î¿u\Ò\ñT\óe’foFFF~1\÷7\Î\Ú\ów\ë?=¸\ç\Ï\İıdÖ?wKøş\ß\ÂıŸŠ/|ı{\ñ\Ï\å¥\Ç\È\\+A¼•\Ğ\ß\Ê\ó@\Ö\ê­\'T\ï\Êz€šz“t\Z\ğ§R7/;s\Ò\ñT\óe’\æ¸\ãû™\Ú_Bû!\È/\õùP?\õ@¨-¡®\õ¹°ı”p{x¸ıµ3f<#=N‰M5O5\ôO\õøP\n\õ\Öªwe=@M½I:\r\ò\õ*~²\î\×ü\ËO\Õ]ÿş\å\Ê\Öûn˜t<\Õ|™¤\Ëc``\ày!´¿>„\ö\÷„:+|ıC\Øÿ\ñ«\ö·…º$\Ôß„š\îC¨_HACS	\æS	ıS9.”J½\õ„\ê]YO\0PSo’¾\ñ;™\ò›©¿\ó\á\Ê\ö­wO:j¾L\Òy	Áş\é¡~5„úw…\Ğ~R¸ı§\â¯µ)Ôƒ¡®\n\÷_\ê#\á\ë?ûş\ÏP¤Ç¡%\Í\ôfC³ÇƒRª·P½+\ë	\0j\êM\Òw®¹pR O\ë–Ñ¿š\ô	ş\ñ\êÿ#;¶L:j¾L\Òıi\ö\ì\Ù¿6\÷‘P\ê\ë!\È\ß<k\Ï\'\ä¯\r\õ\íøÿ>\Üw|¨CB°ÿo\é1\èˆf‚z3¡¿™\ã@©\Õ[O¨Ş•\õ\05\õ&\é\í[\ï»jŸız¡?ş©¾­\ë~8\éªµ2IWü´û\Ş9„\ö·‡\0bøú¼P+B­µ#\Ôu¡¾\ê\ô°} \ì\÷\Ê9s\æ<+=]\×n`o\÷\ñP\n\õ\Öªwe=@M£Iz\ëº+7_ş‰Ia?­øV€m\÷ÿx\Ò\ãUke’î½‘‘‘ƒBhÿ\íŞ‡Cp?#\Ü~5\ÔC=\êP—…Z\êı¡\Ş<{\ö\ìÿƒ\Âi5¸·ú8(F\ë	Õ›²\0 \Æ$]¬2IwO\ë3g\Îü£\ğ?C|ø\Ë\Â\í½\ã\áş†P_‰¡?\ÔPøú·\â¤Ç ¯L5ÀOu(5\ë‰b•\õ\05&\éb•Izz\r_fjf|\Ùı¬=/¿_3k\Ï\Ë\ñ×‡ûG\Ã\íg\â\Ë\õ‡‡‡\ß\ê—\ÂÃ–‡l4\ä›\İg=Q¬²\0 \Æ$]¬2I·&~0^\ï3B?0/\Ü~\'Ô³\ö|\ŞM\á¾ÿ3k\Ïì„¯7~\ğ^zJc_~_Û:¬\'ŠU\Ö\0Ô˜¤‹U&\é\ÆâŸ²‹\Ò.„\ö?	\áı£\ñOİ…Û«g\íù\Ów\ñO\à­\n\õ\áş“\Â\í¡a¿_‰2/=\ì\×8\Ø7º\Ø\ë‰b•\õ\05&\éb•Iz,\Ü??\÷7„:6\÷¿\rş’P·‡\Ú\îûpûP¶½\'|ÿû\Ã\Ã\Ã\ÏMMH~ú=0\Ö\Å*\ë	\0jL\ÒÅª²L\Ò3f\ÌxF\í¿\Âû\á!¸Ÿ\Zn?\ê\ÊP[\Ç\ë\áş‰\Û\Â~Güø˜\ô8Ğ¦j\Ğ?yüV\à‡YO«Ê²\0 	&\ébUn“t¼\nBû\ï…?gü\êü¿…\ÛÇ«\ö¡~\ê›\ãW\ó\ß\ê\ñ*z\è°CBß‹¿w?´Áz¢X\Õh=qÿı\÷¿\ä{\î¹\é†nxø/ş\â/ş\Ê¨”€IºX\Õh’.²…\î\ß?ş;\Ã\í‡f\íy_ı\÷\Â\í\ÆP…º&\Ôg\íyş‘¡~\ã\ïx\Ç3\Ó\ã@¯Œ‡~ \r\ÖÅªt=Q©T~\Ûm·ıı®]»ªŒû=\ñÕ¯~u\İ{\Ş\ó\×O\Ü€Ì˜¤‹U\é$]$‡~øs\â\'ß‡\Ğ~t¨O\"şf\íù„ü;\Ã\÷\ß\r\÷Ÿ¾>a\æÌ™x\ô\ÑGÿ\é1 ˆ„~hŸ\õD±j\âz\â\Î;\ï<d\ãÆ›ªa?u\×]w=~\ÖYg-ø¹‰m\n@&L\ÒÅª„ş§Å¿Uü[Cú`¨Ï„Zj]¨‡Cı{¨‹\Â\ö¿f¸}UX$˜ú‰\Ğ\í³(V\Å\ö\ó“Ÿü\ä[O=U»¸\ß\Ğ\î <f\Çüù\óß¶-\0}\Î$]¬\êV\è?\ì°Ã‚\Îo…\Ğnf-\õ•\ğ\õ\õ\á\ö‘P\÷†º<Ô¹\á¾„pÿ–\ğ\õ‹\ÂÃ–r \ôCû¬\'ŠU«W¯®<\ô\ĞC;\Óp¿/Û¶m{ü³Ÿıìš—¤m@Ÿ2I«¦;\ô‡Iû…!Ğ¼9\÷?~Qøú²Pw‡û†û¿n\Ï5üN\Øÿg\Óc@\î„~hŸ\õDqjıú\õ•¿ıÛ¿­lÙ²%\Í\ôû\ÇE•O¥¿¯@É˜¤‹U­„ş9s\æ<k\ö\ìÙ¿Bı@\ØOAşK\á\öºp»=\Ünµ\"lûlø~^¨·\Çÿ½¾—\ÊÊ‚\Úg=Q¬Zºti\å\Øc½lttt\ó\îİ»\÷ıúşJ\å\É\r6|\åOÿ\ôO\ãg\õ|Û•ş|˜\ã\0“tÁjo¡?\öÿ\Z\ê0x\ê\ï\â¤\êP;\Ãı7‡Û¥¡ş*üc†‡‡_\Û/\È\ó¾\Ï\\\ñ\Üy‹F·¦\÷wCú\Ü\óÿaÅ¯¸h\ô\Ç\÷i$},ıË‚Ú—\Ãz\â\á‡®r\È!q<˜´­ßªºˆŸ¿sú\é§o\Ü\Ûù=\õ\ÔS\×\Ç\òc\á}qÿ¤i\és\æ8 ‹I:§º\ô\ÒK+!´ÿz ÿ8\Ü~8\Ü~!\ÔU\á\ëm\ávs¨+Bıs¨“\Ã}\ï\n\áşe!\Ü?=mWZÿO<g\ô\ï\ÒûÉ›´¯\ß\×\ñ=\ğox\Ã\Æ¬t{¿\ÕÄ‹\ñ\"À\ìÙ³Ï»è¢‹|\ò\É\'\÷·_w\İu_\Û\î\ã\àgú\åbScú~’Î©}\ô\Ñ\Ê\È\ÈH|\ïÕ­¡ş\ï\Ğ\Ğ\Ğÿnÿ4\Ô\ë9\æ˜ÿ’¶]?š·h\ô›\ó-?l\ì\ë\Å+>>\ïœ\å\'\Ï[´bv\Ú_®\í\ï_4º)Ôj\0?ùo.=hŞ¢åŸŸ¿h\ôpÿ=\'.=|\Â1·,8\Ù\Ï\Í[<ú¿\Â\×Wœx\ö\èÿû\Ş·\Í_<:üS\ÇN\öMŸ;„ş\ï„û¾Ÿ\ç\ÄE+\ÔW\çœ\Ò\ÇÒ¿,ˆ }ı¾ÿ„\Ê\ïş\î\ïfú«\ï\ÄO¼%¸ÿ¸\âCú\Ğhÿ~\ïO\÷%\æ8 \ï\'\é\Ü*^\éO\Û(\'Õ—Ï‡ú\ËP\Ç\Äû\Æ\Ãÿ\Â\ÚN•\Ê\ÓN<gÙ‹\çŸ3:³ú\òù\ÒÅ«\ğ¾\è\Û‡Àÿ¶pÿú\ê\î! _ù¡E£¯š\Î\òKN<wÅ»N\\´ü½\ñ?\â¶\ğ\õi\î;\ñ¹\çÿ\İ\Ç}hş9+\ŞşÁs–¿>|ı`\õq\õ\Ï)9oú–´¯\ß\×GqDeÃ†\rY‡şhÆŒ\Ï\Z\Z:5Œ{·\Ç\Ûø}ºy1\Ç}?I\çV&\éœ\Ä+\é\ó\Îı\È~?ú¥x\Õ<~½\'°\Ş¯Ô‡\Û;C}q|Ÿ\õ\ï?\÷\ò±W;œ\ô·+~!|¿m\Â\ã?7\ñ\ò‡\Ç\Ü\Ãy|Ì¼sV»g\Û\ò«»Á¾µç·xù;N\\¼biüzş\ß-aØ¶1~½—sª=–şfA\í\Ëe=±_æ¡Ÿ\ò1\ÇA\Æ\Â/øŠP3\ÒûS¹LÒ¹T\î“tûgÕ¢Ñ•\óş~\ÅÏİ·h\ô\êù‹–¿vü\ë?xÎŠWœx\ö\è\ë\Â\×\ë\â\Ë\ğ\Ç\ï\ßúÁsF\ß<\ö\Òü±·,ÿ§\Ú1\Z¶o\÷Œ\ï»fş¢\Ë\ß\ßF¾~²z\ìûş\çs\ÇW,ı\Ö\Ø*„¯«¯\Ø\Ë9\ÕK³ ‚\öå²\ØO\è\'3\æ8\ÈXü¯½†ÿ\\&\é\\*\çIzş¢\Ñ3N\\4ú\ñ\ëøüš/\Z¿ÿù\÷\İ\çÅ¯\Ã}Ÿ	\õH|ù}\Û¾h\Õ\ókû/½#Ü·#\Ü~\áÔ³¾ÿœ\Úq\Ï=\"\Ü\÷Â…+\Æ^¢wÎ¼ø2ıE£ÿn·T\İ`ß‰\Ï}ûø{\÷·\Î;g\Åeû:§‰¥¿YAûrYO\ì\'\ô“sdlB\è\ßkø\Ïe’Î¥L\ÒS3ş>ü/\Í_<úg\é¶\ÔT\ö¥\\,ˆ }¹¬\'\öúÉŒ92V\'\ô\×\rÿ¹LÒ¹”Izj\â•ÿù‹V|m\áÂ…û§\ÛRSÙ—r± ‚\öYO«¬\'¨2\ÇA\Æ\ê„ı´\ÆÂ¿IºXe’†\î³ ‚\öYO«¬\'¨2\ÇQ(=\ô\Ğ\ó.¾ø\â3\Ï=\÷\Üq\Æ[,X°\ó}\ï{\ß\î\ØQO8\á„\'N>ù\ä\íû\Ø\Ç\î9\ó\Ì3/9\å”S\Ş\ò´\ôEV\'t¢L\Ò\Å*“4t_\Óû€©±(VYOPe£®¾ú\ê\Î>û\ìu\Ç|%ıÊ’%K*kÖ¬©\Üq\Ç•­[·V¢x¿\÷\Ç\í§z\ê\î¹s\ç>\Âÿ¥\Ã\Ã\Ã/MYD\İş…K\Ãı„Z1\Ë\Ëû[&i\è¾nÏ#\ë‰b•\õU\æ8z\ê\Æo|\çg>\ó™\õ\Çw\\\å\ë_ÿze\ó\æ\Íc¿Yqÿø¸şŸ?ş×†††~!}\"\é\ö/Ü¾\Â~U?O\Ò\ßû\Ş\÷*¯y\Ík*tP\åU¯zUeÅŠ“\ö\é·2IC\÷u{|†\õ\óz\"Ç² \ÊGO„¼ş\ôoû\ÛÇ°ÁTv\ìØ‘\æù)‰Ç™3gÎ£G}\ôÿNŸ¯(ºı·¯°_\ÕÏ“\ô›\ßü\æ\Ês\óœÊ¥—^\Z¶•¾\ğ…“\ö\é·2IC\÷u{|†\õ\óz\"Ç² \ÊG×…Œş\ó^x\ám\ïÿû+k×®M\ó{[\â\ñ;î¸s\ç\Î=#}\Ş\"\è\ö/Ü¾\Â~U“\ôW\\1ú_û\Ú\×N\Ú\Öoe’†\î\ë\öø9\Êa=‘SYOPe£«B.ÿùE‹m;\í´\Ó*Û¶mK3û´ˆ\Ç]°`Á£\Çs\Ì\Ó\çïµ¢ş\Â\å0I|\ğÁ•¼\à•\Ûn»mÒ¶~+“4t_Q\Çg\è\'9¬\'r*\ë	ª\ÌqtM\È\ãO¿\ğ\Âo?D\ò\É\'?8{\öì¥\ç\ÑKEı…\Ëa’^½z\õØ•ş7¾ñ“¶\õ[™¤¡ûŠ:>C?\Éa=‘SYOPe£k\â{ø?\ğt\ì\n*>Ï±\Ç»=t\ò\Ã\Ósé•¢ş\Â\å0I?\ò\È#c¡?~ _º­\ß\Ê$\r\İW\Ô\ñúIë‰œ\Êz‚*s]qı\õ\×\Z?´oº\ßÃ¿/\ñù>ú\èm\ÏOÏ©Šú\×Ï“\ô\Ş\ğ†Ê³Ÿı\ì\ÊÒ¥K\ÇBÿ[\ßú\ÖIû\ô[™¤¡ûŠ:>C?\é\ç\õDe=A•9®ˆ–/~º~/œw\Şy\ëCG?\'=§^(\ê/\\?O\Ò\ñO\ôı\æoş\æX\ğÓ›\Ş4\ö=\é>ıV&iè¾¢\Ï\ĞOúy=‘cYOPe£ã®¾ú\ê\âUş\íÛ·§y¼+\âóŒŒ<4<<ü\Ò\ôÜº­¨¿p&\éb•I\Zº¯¨\ã3\ô\ë‰b•\õU\æ8:\î\ì³\Ï^_z\İK_ü\â\ïıü\ôÜº­¨¿p&\éb•I\Zº¯¨\ã3\ô\ë‰b•\õU\æ8:jû\ö\íÿ\å„N¨lÚ´)\Í\á]ÿ\É\á\á\á.\Ü?=\Çn*\ê/œIºXe’†\î+\êøı\Äz¢Xe=A•9ú\æ7¿yÆ‚\Ò\Ş\Ç{\ì]¡Ã¿>=\Çn*\ê/œIºXe’†\î+\êøı\Äz¢Xe=A•9Z¼x\ñ–,Y’\æ\ïø\ìg?{C\è\ğc7\õ\Î$]¬2IC\÷u|†~b=Q¬² \ÊGGq\Æ[Ö¬Y“\æ\ïXµjÕƒƒƒ§\ç\ØMEı…3I«L\Ò\Ğ}EŸ¡ŸXO«¬\'¨2\Ç\ÑQ§z\ê\Îø\'ÔŠ\à\Ö[o/\ï¿6=\Çn*\ê/œIºXe’†\î+\êøı\Äz¢Xe=A•9:\á„v?\ğÀişî‰­[·\î~]z\İT\Ô_8“t±\Ê$\r\İW\Ô\ñú‰\õD±\Êz‚*s522Ry\â‰\'\Òü\İ\á<¶\Ç\ß\ëJFE`’.V™¤¡ûŠ:>C?±(VYOPe££?şø\'Šr¥\ó\æ\Í?™\Õ\ã+ıEe’.V™¤¡û,ˆ }\Ö\Å*\ë	ª\Ìqt\ÔI\'´½(\ï\é¿\å–[®›\Õ\ã\÷\ô•IºXe’†\î³ ‚\öYO«¬\'¨2\Ç\ÑQ§vÚ½Eù\ôş\Ë/¿|y¯?½¿¨L\Ò\Å*“4tŸ´\Ïz¢Xe=A•9Z¸p\á%K–,I\ówO|\ò“Ÿ\\\Z:ü_§\çˆIºhe’†\î³ ‚\öYO«¬\'¨2\Ç\ÑQ,xÛ©§º;\r\à=\ğ\Ä\ğ\ğ\ğ¡Ã¿>=GL\ÒE+“4tŸ´\Ïz¢Xe=A•9Z¸p\áş\Ç{\ìc›6mJCxW\İw\ß}W†Î¾!Oz˜¤‹V&i\è>\"hŸ\õD±\Êz‚*sw\Ê)§\\ºt\é\Ò4‡w\Õ\'>\ñ‰‹Bg??=7\ö0I«L\Ò\Ğ}DĞ¾\åË—\ïŞ¹s\ç¤yMu¿B;¬\ë‰]iQN\æ8:nxxø¥s\ç\Î}|û\ö\íi\ïŠmÛ¶]:ú¦xé¹±‡\Ğ_¬ú¡û,ˆ }«V­Z¿q\ã\ÆI\óš\ê~\İ}\÷\İ_	\ë‰k\Ò6¢œ\Ìqt\Åüù\ó¿vÁ¤y¼+\Âs5t\ôs\Òs\â?	ı\Å*¡ºÏ‚\Ú7::z\ÄÊ•+Ú°a\ÃWü{S\á\ç¾á®»\îº(¬%\î	uh\ÚF”“9®xşœ9s]»vmš\É;\êºë®‹/\ë\ßŸ?=\'ş“\Ğ_¬ú¡ûÊ¸ \n\Ó\äa^ş\Ú\êÕ«_¾|ù\ØØ£º[\ñ\å\ğ\ñ\êx\Ëiû\ô«\ğ\ï:4\Ô5¡Kÿ½ª+\î\ñ\ç/\ğSS\Æ99ú\è£ÿ\÷\ñ\Ç¿sÛ¶mi6\ïˆM›6}/t\ğ»C?-Ni\ğT½«\ØiU\ÆQüKB\à¬Ä—c»*Û›Š?\÷ø\óWÇ…4 S\Ê8\Ç\ÑCs\ç\Î=cÁ‚Æ‰®“v\í\Úu\Ë\ğ\ğ\ğµCCCIÏÉ„şb•\Ğ\İW\ÆQ¼\Â\ïı\×Å¨ørøxu6m#€\éP\Æ9;\æ˜c¾x\ò\É\'?Ø©+ş›6mZ\è\Üÿœ>7\õ	ı\Å*¡º¯Œ¢ø’~Wø‹Q±–ù¤u C\Ê8\ÇQ\0³g\Ïş\ØÜ¹s·O\÷{ü\Ç\ß\Ã+üS#\ô«„~\è¾2.ˆŒı\Å*c?\Ğ)eœ\ã(ˆø^û£>z\Ûyç·~Çi~Ÿ’|\ğ\ê\ñO\é\ß\ä=üSg\áW¬²\ğƒ\î+ã‚¨Ù±ÿ\á7T\Ö^ûù\ÊÍ—Ÿ9V\ñ\ëx_ºŸj¯Œı@§”q£@†††~!t\ÂsFFFú\Â¾p\Ç\æÍ›ŸHı^<±nİº+\Ï8ãŒ±O\èÇ‰\ÇKŸƒ}kvá§ºS~\Ğ}e\\53\ö\ïØ¶®rÓ¥§W~ø­S~ª\â}q[º¿j½Œı@§”q£€†‡‡_\Z:\ãù!´oz\ï{\ß{\÷g?û\Ù\ëW­Zuã­·\ŞzÏ–-[Ç¦M›\Ö\Ş|\ó\Í×…Iq\Ù\'?ùÉ¥\á17†\Ç\ÜŸ“\æ5³\ğS\İ+?\è¾2.ˆšû\ï½ù\âI¿Z\÷\İ|É¤ıU\ëe\ì:¥Œs\Å\ö´™3gş¯ş?588xq\è ×„º7v\Ô\ñ\Ûk\âıq{\Ü/\îŸ€©kfá§ºW~\Ğ}e\\53\öÿx\ÅY“\Â~µ\â¶t\ÕzûN)\ã$–/_¾\Û\'8£B;¬_\æœ¡\ëÊ¸ j&\ôß´lá¤°_­¸-\İ_µ^B?\Ğ)eœ\ã€ÄªU«\Öû[\ÍÅ¨»\ï¾û+\Ëü­f\èº2.ˆ„şb•\ĞtJ\ç8 1::z\ÄÊ•+Ú°a\ÃWü{S\á\ç¾á®»\îº(,ú\î	uh\ÚF@g•qA\ÔLèŸÖŸ†ıj\Åm\éşª\õúN)\ã\Ùz\ğÁ\å’K.Yu\î¹\ç\î\\¸p\áS,¨¼\ï}ï‹Ÿ‡P9\á„*\'t\Ò\î~\ô£Oy\æ™\÷Ÿr\Ê)§…‡\ì_}lš\ñ\ns¨\Ç\âÂ£+ş;\Óûú¨\â\Ï=şü~\è2.ˆ\âØ“Ï´n¿\ò\ÜIa¿Zq[º¿j½b{¤m0\Ê8\ÇAv®¾ú\ê‹\Î>û\ì\'?şøJúK–,©¬Y³¦r\ÇwT¶n\İ:\ö\÷\r\ãmü>\Ş·\Çı\æÎ[9\õ\ÔS·\r\r’³Ğ€V•qüh&\ôo]wc\å¦\Ë>>)\ğ\Çû\â¶t\Õz	ı@§”qƒlü\èG?úøg>\ó™\';\î¸\Ê×¿ş\õ\Ê\æÍ›\Ç~³\âş\ñq1üÏ›7\ï\ŞÁÁÁ_KŸ£ŸĞ€V•qüh&\ôÇº\óº/N\nı\ñ¾t?\Õ^	ı@§”qƒ¾\òú\ßş\ö·7Ä°ÁTv\ìØ‘\æù)‰Ç™3gNedd\äS\é\ó\õ\ZĞª2M…ş]»*·ÿ\à¼I¡?\Ş·M\Ú_µ\\B?\Ğ)eœã ¯…ŒşK_ü\âÿû\ß_Y»vmš\ß\Ûÿ#a\îÜ¹\ßKŸ·Ğ€V•qü\ØW\èß±m]\å¶+O\nüÕŠ\Û\â>\é\ãTk%\ôR\Æ9úV\Èå¿´hÑ¢İ§vZeÛ¶mifŸ\ñ¸\ñış\ï~\÷»\ïHŸ¿\èh@«\Ê8~4ı»vU6\Ü>Zù\Ñw?:)\è§\÷‰ûº\ê\ß~	ı@§”qƒ¾\òø^x\á\ã1\ğ\Ç\ÅA\'\Å\ã\ÇOúŸ={\ö²\ô<ŠÌ€´ªŒ\ãG½Ğ¿¯«û\ÊUÿ\öK\è:¥Œs\ô¥øş|\à»ÂŸŠ\Ï3w\î\Ü\İCCCŸLÏ¥¨h@«\Ê8~\Ôı\Í\\\İoT\ñ±\é\ñT\ó%\ôR\Æ9ú\Î\õ\×_¿0¾\×~º\ßÃ¿/\ñùfÏ½û\È#|yzNEd@ZU\Æ\ñ£^\èOƒüT+=j¾„~ S\Ê8\ÇAß‰–/~º~/œw\ŞyO†\â†\ôœŠÈ€´ªŒ\ãG½Ğ¯zWB?\Ğ)eœã ¯\\}\õ\ÕÅ«üÛ·oO\óxW\Ä\ç‰/\ó?$=·¢1 ­*\ãø!\ô«„~ S\Ê8\ÇA_9ûì³Ÿ\\ºtišÅ»\ê‚.x<·¤\çV44 Ue?„şb•\ĞtJ\ç8\è=\ô\Ğ\ËN8\á„Ê¦M›\Ò\ŞU\ñù‡‡‡ŸZ¸p\á3\Òs,\ZĞª2B±J\è:¥Œs\ôo}\ë[+,Xf\ğx\ï{\ßû\Ä\Ğ\Ğ\Ğ\ñ\é9‰\rhU\Ç¡¿X%\ôR\Æ9úÆ¹ç»kÉ’%iş\î‰\ó\Ï?g0V§\çX$4 Ue?„şb•\ĞtJ\ç8\è.|jÍš5iş\î‰U«V\í¼?=\Ç\"1 ­*\ãø!\ô«„~ S\Ê8\ÇA\ß8\õ\ÔS+k×®M\ówO\Üz\ë­O„\ã\á\ô‹Ä€´ªŒ\ã‡\Ğ_¬úN)\ã}#~ˆ\ß<\æ\ïØºu\ë\î0`<™c‘Ğ€V•qüú‹UB?\Ğ)eœ\ã oŒŒŒTx\â‰4\÷D8ú=`ıü€\â*\ãø!\ô«„~ S\Ê8\ÇA\ß8şø\ãs¥\ó\æ\Í\ñ\åı®\ôY*\ãø!\ô«„~ S\Ê8\ÇA\ßøĞ‡>´»(\ï\é¿\å–[\ñ~ We?„şb•\ĞtJ\ç8\èû\ØÇ,Ê§\÷_~ù\åø\ô~ We?„şb•\ĞtJ\ç8\èÿø\Ç\ï_²dIš¿{\âŸøÄº0`¬NÏ±Hh@«\Ê8~ı\Å*¡\è”2\Îq\Ğ7,X\ğ\ÑPiş\î‰\á\á\áCCCÇ§\çX$4 U9\áß²\"ÔŒ\ôş”\Ğ_¬úN\Éiƒ\ì,\\¸\ğ\Ç{leÓ¦Mi\ïªu\ë\Ömÿ©x>\é9‰\rhUN\ãGü·Œ\×^Ã¿\Ğ_¬úN\Éiƒ,z\ê©[–.]š\æ\ğ®ú\Ä\'>qW,nIÏ­hh@«r\Z?&„ş½†¡¿X%\ô’\ÓY\Z\Z\Z:d\îÜ¹•\íÛ·§Y¼+x\à‡\ãUşx\é¹\rhUN\ãG\Ğ_7üı\Å*¡è”œ\æ8\ÈÖ¼y\ó\î½\à‚\Ò<\Ş\ó\çÏ¿\'7¤\çTD4 U9u\Â~Zc\á_\è/V	ı@§\ä4\ÇA¶<\òÈ—Ï™3§²v\í\Ú4“w\Ôu\×]wW¼\ÊŸ?=§\"2 ­ªŒ³/¡¿X•¶\êm¥c\ô3}\Zú\Ä\È\ÈÈ§?şøÊ¶m\Û\Òl\Ş›7o~0O„\Ğÿ\É\ô\\ŠÊ€°\×ÿÀX1\Ë\Ëû[®\ô‡\õ¹Ñ§¡Ì;\÷{\ñOø\Å\ÅA\'\í\nfÏıp .MÏ¡\Èh\0uCÿO…ıª~ı\ßı\îw+¯x\Å+*\ÏzÖ³*¿\ó;¿SY³fÍ¤}ú­„şâ° 7ú4\ô™w¿û\İwœt\ÒI»;u\Å?^\áü·¦\Ï]t4€Ÿ\nıu\Ã~U?‡ş×¼\æ5•g>\ó™Õ \\ù\í\ßş\íIû\ô[	ı\Åa=An\ôi\èC!”/›;w\î\î\é~|œ\ÕgWø«h\0cc\á^\Ã~U?‡şj\İu\×]c¡ÿ\àƒ´­\ßJ\è/\ë	r£OCŸŠ\ïµ\á\÷y\ç\÷\ä;\Òü>%Û¶m{8~JÿøŸ\æ\ë›\÷\ğ§h\0\Í\Ë!\ô\ìcı\ï|\ç;\'m\ë·ú‹\Ãz‚\Ü\è\Ó\Ğ\Ç-ü\ß022²û_ø\Â\ã›7oN\óü^­_¿~ûgœ1\ö	ı\ñ8\ñx\és\ô\Z@\óú=\ôÿ\ë¿şkeÿı\÷¯¼\à/¨\Ü~ûí“¶\÷[	ı\Åa=An\ôi\È@í‡„_\æ[bx\ï{\ßû\Äy\ç\÷\èªU«v\Şz\ë­OnÙ²ew\È\÷»7m\Ú\ô\Ä\Í7\ßüHXTlı\ä\'?¹~xxxgxLû\ñq‡¤\Ç\ìG4€\æ\õs\è¿\ò\Ê++p@\å·~\ë·\Æşœmº½K\è/\ë	r£OC^\öŸ9s\æ	\á{\õ\à\à\àı\á6~ _|~ü@§xû\ğøı«\ã~qÿ\ô\0ıÌ€Ğ¼~ıo{\Û\Û*oû\Û+>ø\à¤mıZBqXO}\ZÈ†\r yıúŸÿü\ç\Ç\ñ¾V§vÚ¤}ú­„şâ° 7ú4\r\Z@\óú9\ô\çXBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ	ı\Å*¡¿8¬\'È>\rdÃ€\Ğ<¡¿X%\ô‡\õ¹Ñ§l\Ğ\0š\'\ô«„şâ° 7ú4\r\Z@\ó„şb•\Ğ_\Ö\äFŸ²a@h\Ğ_¬ú‹\Ãz‚\Ü\è\Ó@6h\0\Íú‹UBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ	ı\Å*¡¿8¬\'È>\rdÃ€\Ğ<¡¿X%\ô‡\õ¹Ñ§l\Ğ\0š\'\ô«„şâ° 7ú4\r\Z@\ó„şb•\Ğ_\Ö\äFŸ²a@h\Ğ_¬ú‹\Ãz‚\Ü\è\Ó@6h\0\Íú‹UBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ	ı\Å*¡¿8¬\'È>\rdÃ€\Ğ<¡¿X%\ô‡\õ¹Ñ§l\Ğ\0š\'\ô«„şâ° 7ú4\r\Z@\ó„şb•\Ğ_\Ö\äFŸ²a@h\Ğ_¬ú‹\Ãz‚\Ü\è\Ó@6h\0\Íú‹UBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ-_¾|\÷Î;\'…O\Õı\n\í°>„ş]i\Ñ\Ö\äFŸ²a@hŞªU«\ÖoÜ¸qR\0Uİ¯»\ï¾û+!\ô_“¶½a=An\ôi 4€æ±r\åÊ‡6lØ°\Åÿ\ŞTø¹o¸ë®».\nÿP‡¦mDoXO}\ZÈ†\r`jbĞŒW˜C=\ßS®º^\ñ\çşXO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§š8 \ô{¥ÿ&\0€©° 7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@ ˜*•\Êk×®ı\Ú\êÕ«_¾|yeÙ²eª\Ë~\î»W­Zµ~tt\ôˆ´}È‹\õ¹Ñ§\Z@1…À¿$\Î\ÊÆ+;w\î¬<\ö\Øcª\Ë\î\ñ\ç¿r\åÊ‡–-[vh\ÚF\ä\Ãzˆ\Ü\è\Ó@ ˜\âş8\Ó ªº_6l\ØBÿ5i‘\ë!r£O5€bŠ/\éw…¿\Û!„ş]i‘\ë!r£O5€bŠ\ï)OÃ§\ê]\Å\öHÛˆ|X‘}\Z¨1 \0S³¡ÿ\á7T\Ö^ûù\ÊÍ—Ÿ9V\ñ\ëx_ºŸj¯„ş¼Y‘}\Z¨1 \0S3¡Ç¶u•›.=½\ò\Ão\òS\ï‹\Û\ÒıU\ë%\ô\ç\Ízˆ\Ü\è\Ó@ ˜š	ı\÷\Ş|\ñ¤À_­ûn¾d\Òşª\õú\óf=Dn\ôi Æ€\0PLÍ„ş¯8kRØ¯VÜ–\î¯Z/¡?o\ÖC\äFŸj\0\Å\ÔL\è¿i\Ù\ÂIa¿Zq[º¿j½„ş¼Y‘}\Z¨1 \äoıú\õ¿t\â‰\'^ÿ\ÊW¾\ò\ñ8 ¶·\êRı\Ì\Ïü\Ì\î_ù•_y\àE/z\Ñ\'\Ã\÷O\ß¦@\è/V	ıy³\"7ú4Pc@\È[üox\Ã×»\ŞU¹\ö\Úk\Ç®tOüyÇŸû\Û\ßş\ö\Ç<\ğÀ\ì\'ø3Í„şøiıiØ¯VÜ–\î¯Z/¡?o\ÖC\äFŸjyû\à?xı¡‡šfQz\àu¯{İº\Ğ$§¦m4úo¿\ò\ÜIa¿Zq[º¿j½„ş¼Y‘}\Z¨1 \ä\íÕ¯~\õ\ã\ñJ3½\÷ƒü`Sh’k\Ó6‚Fš	ı[\×\İX¹é²O\nü\ñ¾¸-\İ_µ^BŞ¬‡È>\r\Ô\ò\ö\Ìg>sl±J\ï\ÅvM²+m#h¤™\Ğ\ë\Î\ë¾8)\ô\Çû\ÒıT{%\ô\ç\Ízˆ\Ü\è\ÓPR\á—E\0\öQ+\Ò\Ç\Ñ\×\Ò\ìI\Å\öH\Zi*\ô\ï\ÚU¹ı\çM\nı\ñ¾¸m\Òşª\åú\ó& ‘}\ZJ*ü\òÏ¨\òÓš‘>¾–\æNz(¶G\Ú@\ĞÈ¾Bÿm\ë*·]¹xR\à¯V\Ü\÷I§Z+¡?o¹Ñ§¡\Ä\â•ü:A\ßUş|¥¹³®¸_ZL¿\ñŸ-4¥a\èßµ«²\á\ö\ÑÊ¾û\ÑIA?­¸O\Ü\×Uÿ\öK\èÏ›€Dn\ôi(±x%¿N\Øw•?_i\îÜ«Å‹\Ó\Ã;,\İ\Ä4ˆ?Û´ ‘z¡_W\÷•«ş\í—ĞŸ7‰\Ü\è\ÓPr\ñŠ~À¿\"İ,¤¹³¡İ»wW^ò’—Œ\Óÿ\÷O73\r\â\Ï6m h¤^\èo\æ\ê~£ŠM§š/¡?o¹Ñ§¡\ä\âı:¡FºYHsgCW]u\ÕX(ı\õ_ÿ\õt\Ó$ş|\Ó‚F\ê…ş4\ÈOµ\Ò\ã©\æK\èÏ›€Dn\ôi ½\Ú\ï*¾\Ò\Ü\Ù\Ğé§Ÿ>JO>ù\ät\Ó$ş|\Ó‚F\ê…~Õ»ú\ó& ‘}\ZH¯\ö\ÏH·“4w6\ô¶·½m,”^v\Ùe\é&¦Iüù¦\rı\Å*¡?o¹Ñ§1Õ«ı\éıd%Í\r½ø\Å/¥\ëÖ­K71M\â\Ï7m hD\è/V	ıy³\"7ú40¦zµ?½Ÿ¬¤¹³¡<p,”>şø\ã\é&¦Iüù¦\rı\Å*¡?o\ÖC\äFŸªI74w\ÒC±=\Ò‚F„şb•ĞŸ7‰\Ü\è\Ó@ƒş\ÆP\'\ß\nşyJs\'=\Û#m hD\è/V	ıyÈ>\rT5\è§ß“4w\ÒC±=\Ò‚F„şb•ĞŸ7‰\Ü\è\ÓPn~£û\éoiî¤‡b{¤\rı\Å*¡?o¹Ñ§¡¼\ö\ì\÷µş“\æNz(¶G\Ú@Ğˆ\Ğ_¬ú\ó& ‘}\ZÊ©\Ù@\ß\ì~\ô‡4w\ÒC±=\Ò‚F„şb•ĞŸ7‰\Ü\è\ÓP>S\r\òSİŸ\âJs\'=\Û#m hD\è/V	ıyÈ>\r\å\Òj€o\õqKš;\é¡\ØiA#B±J\è\ÏGC+b \ÚG­HıD\è‡\òh7¸·ûxz/Í\ôPl´ ¡¿X%\ô\ç#„¡uB~Z3\Ò\ÇA?ú¡¦+°O\×q\è4w\ÒC±=\Ò‚F„şb•ĞŸ—x%¿N\Ğw•Ÿlı¿\é\ê\Ó}<º\'Í\ôPl´(Ÿ\ñ°1#½?%\ô«„ş¼\Ä\ßÁ:a\ßU~²!\ôC\Ş:\Ğ;u\\:+Í\ôPl´(Ÿ‰W\÷.„şb•ĞŸŸ\ñ\ßÁ4\ğ¯H\÷ƒ~$\ôC¾:\Ì;}|¦_š;\é¡\ØiQ>\õBF½\ğ/\ô«„şü\Äß»:¿3\Òı 	ı§‰üi¡\Ş=~Ûz\ÇüûKš;\é¡\ØiQ>uBF\İ\ğ/\ô«„ş<ÿ\Ş\Õ~\Ó\íĞ¯„~\ÈO\Zø?·ßpo[\rş{;\à\ß?\Ò\ÜI\Å\öHˆ\ò©\ö\Ó\ZÿB±J\è\ÏSü]›\ğ»7#\İıJè‡¼¤ü\İû\í	\ÕJ{3&şj\óS{L~^Š)Í\ôPl´h*—¶\Ò\à©zWB¾f_\íO\ï‡~¦OC>\ê\ïz}*Á¿\Ş\ãÿyüşT½\ç§X\Ò\ÜI\Å\öHˆ\ò-L\Ò`?¡b\ğ˜Q\İÏ•şb•ĞŸ‡9s\æü|ø=;jpppQ¸]\ê\Î\ğ\õ®\ñ\ßÁG\ã\÷\ñş\ñ\íG\Åı\Óc@?ˆ}:½\è?{\Ü\õ‚{3Á¿\Ş\ã\Zşª½½—\æNz(¶G\Ú@”oa’ıIa¿ªŸCÿw\Ş9q«tŸ~+¡¿¿\r\r\r½%ü-\r\õX\ßÁ½U\Üi||zL(²\Ø\Óû€ş\ÒLĞ®\à\÷ü\ë\í¿¯À_\Õ\Ìù\Ğiî¤‡b{¤\rDù&\ÂDİ°_\ÕÏ¡ÿ«_ı\êX?\ò\È#\'m\ë\×úûS\ë¿~Ï®ª\æ[©«\â\ñ\Ò\ç€\"Š}6½\èS	\Ø\õ‚|½\à_o¿f\ÕTÎ‹\îIs\'=\Û#m Ê·0™µ°_\ÕÏ¡ÿ¤“N\Z\ë\ïŸş\ô§\'m\ë\×úûËœ9s588ø\áwmw\Z\Ş\çÍ›Wù\ò—¿\\Y³fM\å;\î¨lİºulŒ·\ñûx\Ü\÷KŸ>\'I\Ù\æV\ÈI+Áº^ Ÿü\ëmŸj\à¯j\åühA³¡a?¡¿Pb{¤\r„…I#ıúÿ\àş`¬¿?\ïyÏ«¼\à/¨\\~ù\å“\öé·º\ì²\Ë\ô\Ó>Æ”\r\r\rıpbX®œşù•û\î»/š\÷*\îŸ„ÿ\â\ó¤\Ï\rEan…ş\ÔN ®\ì\ã\÷û×¹¿\ÕÀ_\Õ\ÎyÒ¤	‹}…ÿtıB\Å\öH“Fú9\ô¿úÕ¯®œs\Î9•+®¸b¬ß¿\â¯˜´O?\ÕÎ;+G}ts\ï	\õıP…ú\ë,ÿlpp\ğ]\á\ö•>\ğ­B[¼<Ôº‰ı¬³Îª\Ü{\ï½\é<%\ñ\ñ\ñ8\ê\Şø|\é9@˜[¡ÿLG®üoM¾o7\ğWM\Çù²É¢co\á?]·\ĞC±=\Ò\ÂÂ¤‘~ı\Õz\ä‘G\Æúı³Ÿı\ìI\Ûú­.½\ô\Ò\ÊÀÀÀ‹C\È{Cú³\Ã\íGB\ß=/|ı­pû£P…û¶Ç¯\Ç\ï;/\î\÷\r_¿qxxø—fÌ˜ñŒ´™>³\ö\\\á¯şxuş\â‹/N‡\â¶\Ä\ã%Wı\ïÏ›\ôš¹ú\Ët\èzÁº\Õt7‰	‹´\Ò\ğŸ®W\è¡\ØÚ†q&\õ\õs\èı\ë__9\ğÀ+\ßø\Æ7\Æúış\áNÚ§ßª™\÷\ôü\Ü\ìÙ³3\ô\éC\ã«\0\Â\í_\Ï\Ú\óª€ø\ê€ø*øI\ğ^-\Ğ\ñ=\ö_\Òÿ\÷¼§rı\õ×§\Ã\ğ´ˆÇÇ¯>W¨¼ÇŸ¢1·Bÿ\èDp/\éO¯\ğ\Ç\ï\ãıÓ­\ç\Ï~{\rıiøO\×*\ôPl¤)\Ù\ÏÂ¤‘~ıË—/¯ü\ÆoüFå ƒª¼\éMo\Zû~\é>ıVÍ„ş}‰Wú½Z 3\Æ?´olŒW\â;ø«\â\ñ\'^\ñÏŸ\ô’¹úÃ³\÷\Û˜ON7´aoWú\ë}ªÿt8yBU]®ı„şBI\ÛGıg¥ıús¬\éı\Í\ğj©\Ú\ógùª\'—\\rI:üvD|	\ã\ØS\ñ<\Òsƒ^‰ı2½(¦\é¼R^/\ğ§Wü§;øO\çù3Á„EFZ+fyya\Å\ö˜\Ğ6°WB±ª[¡_\Ê\òj\ğo|zz_#\á\ßuUuŒ¶\×MÉ‡û]•\ôJ\ì“\é}@qMGp®ø\ã{ø\ë}zÿtÿ\é8o\Z˜°Àh\ö«\Ò\õ	=\Û#m hD\è/V%\ô7#‡WŒŸ\÷\â\ğoù\Ùt\ÛD3g\Îü£\ê\\_n\ß\î§\ôOU|¾‰/\óç“#\ôB\ì\é}@±µ şj°¯·½\İ\à\ß\ÎùÒ„\ê\âbV\ã°_•®O\è¡\ØiA#B±ªŸBÿ¾\ôÃ«\Âs5>\Ïİ¹· ¶/­Î‰\çŸ~:\ìvE|\Ş	\ó\ò\Ò\ô¡bL\ïŠ¯• ]/\Ğ\×û”şzûµ\Zü[9O¦hÖ¾\Ã~Uº6¡‡b{¤\rıÅªœB3zıjpŒ¤\ã\åıS<§‰û?wü<\Æ\ö\é\öUşªø¼\Î\õ±x^\Ïz!\ö\Ç\ô> ?L%P\×\ò\õU½ı§\Zü§r~tGº6¡‡b{¤\rıÅª²…ş}\é\ô«\Â>WN\ÒÕº7ş§Â„}ªn›7o^:\ävU|ş	\çy\Ô\Ä\ËDaÛŒP+\ÒûaºÅ¾˜\Ş\ôf‚u½\0¿·À_U\ïq\ÍÿfÎ‹\îK\×%\ôPl´ ¡¿X%\ôO];¯_\ß? \'U\Ø\çK\á\Ø\Ï_/®\Ş\÷¥/})r»*>ÿ„s\\œş,f‡ı\ê>\év˜nú\ô¿½\ìzÁ½™À_U\ï\ñû\nş{;z+]—\ĞC±=\Ò‚F„şb•\Ğ?ı\ö\òjo‡\Û\İƒ~z \ìk\õûë®».r»*>\õ\\\ÂyVÿ³’°_­‰?\èı\ò\Ğ(h¿{¿\ÖU½\à\ÌO\í\ñŸ\ZÅ®K\è¡\ØiA#B±J\è\ïV~9\r\ÉI=\Z\ê¾POU\ï[»vm:\ävU|ş	\ç_½P7\ìW+ı7\Ãt\Ó\Ï \õ\÷\ÄÀ\ŞJ\à¯j\æ8\õŸbI\×%\ôPl´ ¡¿X%\ôw\ÏÌ™3ÿ \r\É\ãŸ\ğùPo^¸paü“\Ã\ñ\Ãş\â}c\Ûx\àt\È\íªøü\Î\÷\É\ôü\ÓJÿ\Í0\İ\ô3\ÈK½\àz¼2_/¨O\ÅŞS\ïy)t]B\Å\öH\Zú‹UB\÷„0\ôx8~\"~`ø~h``\àÀt¿‰\áú‰\'H‡Ü®ŠÏŸ{¥z]\é\ï\ĞßºÀ»ı|´.]—\ĞC±=\Ò‚F„şb•\Ğ\ß=!\èÿI\Èù>22\ò‹é¶‰Šz¥ü¼¼¼€i×­ Ş­\çaz¤\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹gÖ\÷Î…è¢½§\Â9\Ö\rÿÿ\00\ä>>\Ó/]—\ĞC±=\Ò‚F„şb•\Ğ_<\ñS\ò«!º¨Ÿ\Ş_•†ÿt;\0LE§‚y§Kg¥\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹\'„\ç\Å\Õı¥/})r»*>ÿ„+ù‹\Ós­ª†ÿ\ô~\0˜ª\é\è\Ó}<º\']—\ĞC±=\Ò‚F„şb•\Ğ_<!<U\r\Ú\ó\æ\ÍK‡Ü®:\ñ\ÄwOıG¥\ç\n\00]A}ºCo¤\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹gxxø¹!`?V\r\Û\÷\Ş{o:\ìvE|\Ş	ÿ±x^\é¹@§´\Ø\Û}<½—®M\è¡\ØiA#B±J\è/¦²—V\÷ùçŸŸ»]ŸwB\è_š#\0tZ«Á½\Õ\ÇQ,\éÚ„Š\í‘64\"\ô«„şbš9s\æU\÷\ğ\ğp×¯\ö\Ç\ç\Z\Zª½´?Oz\0\Ğ\rS\r\ğSİŸ\âJ\×\'\ôPl´ ¡¿X%\ôW\ÛWUC\÷Yg•½Ÿo\ÂUş«\Òs€nj6\È7»ı!]Ÿ\ĞC±=\Ò‚F„şb•\Ğ_\\CCC¿\÷S\Õ\ğ}\É%—¤\ÃoG\Ä\ç™øŸŠç‘\0tÛ¾ı¾¶\Ó\Ò5\n=\Û#m hD\è/V	ı\Å688ø_\æı\õ×§C\ğ´ŠÇŸø²şøü\é9@¯4\n\öî§¿¥\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹mÎœ9\Ï\n\áû†j\Ï{\ŞÓ±\à¯\öÊ‚ø¼\ñù\Ós€^J~ú=ùH\×*\ôPl´ ¡¿X%\ô_\ß/\nu\ï\Ä+ş_|q:·%o\âş\ñ\ç{Qz.\0PÕ \òø­ÀŸ§t½B\Å\öH\Zú‹UBü\åƒ¬øa{\í~ª||\ò¡}c?>_z\0P$‡\ÄI+Ş¦\ÈFºn¡‡b{¤\rı\Å*¡¿\Ä+\ï³&¼\Ô?V¼\êşù\çW\î»\ï¾thŞ«¸|\\ru\ì%ı\ñy\Ò\ç€\Âı\ä+]¿\ĞC±=\Ò‚F„şb•\Ğ\ß_\â{\ì\Ç?\Ü/\r\ë•y\ó\æU¾ü\å/WÖ¬YS¹\ã;*[·n£\ãmü>\Ş·Ÿxâ‰“\ë=ü\0\ô\r¡?{Iì¤—b{¤\r,_¾|\÷Î;\'…O\Õı\n\í°>„ş]iQ|\ã\Î\ïª:á½•ºÊŸ\å \ï\ÄI,½¬¤¹“Š\í‘64²jÕª\õ7nœ@U\÷\ë\î»\ïşJı×¤mDÿaı-aÍ³4\Ôcu\Âü\Ş*\î¿4>>=&\0\ô¡?{iî¤‡b{¤\rŒ±r\åÊ‡6lØ°\Åÿ\ŞTø¹o¸ë®».\nÿP‡¦mDÿ™3g\ÎÏ‡µ\ÏQƒƒƒ‹\Â\í\òPw†ªşù½GÇ¿_>¾ı¨¸z\0\è+B\ö\Ò\ÜI\Å\öH\ö&\Íx…9\Ôc\ñ=\åª\ë\î\ñ\ç/\ğg*¬ƒfL¸ª?#\İ\0}O\è\Ï^š;\é¡\Øi\Ğ;a´bB\è_‘n€¾\'\ôg/Í\ôPl´\0\è\ä*¿«ı\0\äI\è\Ï^š;\é¡\Øi\Ğ\ÉU~WûÈ“ĞŸ½4w\ÒC±=\Ò û\Z\\\åwµ€üı\ÙKs\'=\Û#m \0º¯ÁU~WûÈĞŸ·8`wü³S\ôŞ®]»Ö‡&Ù•¶\0\İ¯\ä\×	úi\ÍH\0}I\è\Ï\Û\Ë^\ö²­\×^{mš?\é\ï|\ç;\ß\rMrm\ÚF\0tW¼’_\'\ä§\åj?\0yú\ó\ö¢½\è/\ß\ö¶·=Pºn\ëAtyh’S\Ó6 \÷¬‡\0È–I.{O?\ğÀ\ğº×½\î¾Õ«Wo\öRÿ\îÚµk×†x…?şe¡-\âU£§§\r@\ïY-“\\)Ä yJ¨5¡\ÛoÏ‡É©\îTüyÇŸ{üùü\0e=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0YÚŠ8©\í£V¤\0È™\Ğ@Â„6£N\ÈOkFú8\0€œ	ı\0d#^É¯\ô]\å\0JK\è \ñJ~°\ï*?\0PZB?\0Y‰W\ô\ë~Wù€Rú\ÈJƒ«ı3\Òı\0\0\Ê@\è ;\É\Õ~Wù€\Òú\ÈNrµFº\0 ,„~\0²T½ÚŸ\Ş\0P&\ÖC\0d©zµ?½\0 L¬‡\0˜¤R©°v\íÚ¯­^½ú\ñ\åË—W–-[¦º\\\á\ç¾{ÕªU\ëGGGH\Û\0 YB?\0“„À¿$\Î\ÊÆ+;w\î¬<\ö\Øcª\Ë\î\ñ\ç¿r\åÊ‡–-[vh\ÚF\0\0\Íú˜$^á3\r¢ªûµaÃ†-!\ô_“¶\0@3„~\0&‰/\éw…¿\Û!„ş]i\04C\è`’ø\ò4|ª\ŞUl´\0\0š!\ô0I³¡ÿ\á7T\Ö^ûù\ÊÍ—Ÿ9V\ñ\ëx_ºŸj¯„~\0 UB?\0“4úwl[W¹\é\Ò\Ó+?ü\Ö)?U\ñ¾¸-\İ_µ^B?\0\Ğ*¡€Iš	ı\÷\Ş|\ñ¤À_­ûn¾d\Òşª\õú€V	ı\0L\ÒL\èÿ\ñŠ³&…ıj\Åm\éşª\õú€V	ı\0L\ÒL\è¿i\Ù\ÂIa¿Zq[º¿j½„~\0 UB?\0“ı\Å*¡\0h•\ĞÀ$Í„şøiıiØ¯VÜ–\î¯Z/¡\0h•\ĞÀ$Í„şÛ¯<wRØ¯VÜ–\î¯Z/¡\0ø\íÜ±J¤W\Ç\áF,±,¬­ll¼)¼½‹m\ö\"R¤O\ÒØˆE(ƒ! d·\Ê\ÑÀ\î &„˜\Â\Ù&_\æ¤ù^e\'Çƒû~\É\óÀŸ™3\Îú\ãSk‰~\0‚y¢ÿ··?v¯¿y‚¿\ÜWëŸ·ú‰~\0 –\è ˜\'ú\Ë~ş\á‹ı\å¾ş9{\ÚD?\0PK\ô\Ìı“I\÷\æ»\ÏC\ô—û\Êc\á¼UO\ô\0µD?\0Á‡¢ÿ\Ï\ß\ßv?}ûYşV+gúÏ³º‰~\0 –\è x4ú\'“nü\æ¼{\õ\õ‹úı•3å¬«şOŸ\è\0j‰~\0‚‡¢ÿCW\÷›«şOŸ\è\0j‰~\0‚‡¢«û­<·ÿz6ÿD?\0PK\ô<ııÿ·ë¿\Í?\Ñ\0\Ôı\0E¿}¼‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸkå›µ™™™Y\íú?\ë\ğ?\'úsÍ•~\0\0\0šr\ô\ß\İ\İuûûû\İ\ê\êj·±±Ñœœ„3C›\è\0\0 ™!Gÿ\á\áa	\ä\î\ô\ô´[^^\îvwwÃ™¡M\ô\0\0\ĞÌ£ss³[[[\÷y¢\0\0€f†ı‹‹‹³\è\ß\Ú\Ú\ê\Ö\××»\Ñh\Îm¢\0\0€f†ı³_\ï?>>İ–ø\ïŸ\ÚD?\0\0\0\Í9ú\Ë\Õı\é—0û‡~\åvii)œ\ÚD?\0\0\0\Í9úf±tt4»\İ\Ù\Ù	g†6\Ñ\0\0@3Cş\Û\Û\Ûnoo¯[YYé¶··»\Ë\Ë\Ëpfhı\0\0\043\ä\èÿ/N\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íœıu\âÓ\Ó\Ï\á\İ4ú\'ı\Ï\0\0\0ª\\\\\\¼»¹¹	jÏ¿\ë\ëë¯¦\Ñÿ}ÿ3\0\0€*\ç\ççŸF£?\Æ\ã\ñ¯®øœM\ß\÷\ñ\Õ\ÕÕ—\Ó\àÿeºOúŸ\0\0\0T+¡Y®0O\÷¾üM¹=û\Êû^\ŞÁ\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\004&\Ì\Ç\ï\ò\÷B\Ñ\0\0\0\0IEND®B`‚',1),('12578',1,'flow_mu180u86md3e.bpmn20.xml','12577',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://bpmn.io/schema/bpmn\" id=\"Definitions_1\">\n  <process id=\"flow_mu180u86md3e\" name=\"æµ‹è¯•914\" isExecutable=\"true\">\n    <extensionElements>\n      <flowable:executionListener event=\"start\"></flowable:executionListener>\n    </extensionElements>\n    <startEvent id=\"StartEvent_mu2c2p840\" name=\"å¼€å§‹\"></startEvent>\n    <sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\"></sequenceFlow>\n    <endEvent id=\"EndEvent_mu2c2p8c0\" name=\"ç»“æŸ\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </endEvent>\n    <sequenceFlow id=\"Flow_1qxvbp7\" name=\"xiaoyuyibai\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_15cdzo6\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_1dpq6dk\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_0qm5tc6\"></sequenceFlow>\n    <userTask id=\"Activity_0qm5tc6\" name=\"1\"></userTask>\n    <sequenceFlow id=\"Flow_0icqanz\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_1mzuxt6\"></sequenceFlow>\n    <parallelGateway id=\"Gateway_15cdzo6\"></parallelGateway>\n    <userTask id=\"Activity_1mzuxt6\" name=\"2\"></userTask>\n    <sequenceFlow id=\"Flow_1aqie0c\" sourceRef=\"Activity_0qm5tc6\" targetRef=\"EndEvent_mu2c2p8c0\">\n      <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${tes &gt; 100}]]></conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_016it02\" sourceRef=\"Activity_1mzuxt6\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <exclusiveGateway id=\"Gateway_0a6n8eh\"></exclusiveGateway>\n    <sequenceFlow id=\"Flow_13gvmcu\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_02klewn\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_1kkbmin\" sourceRef=\"Activity_02klewn\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0t3saos\" sourceRef=\"Activity_0ylt45t\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <userTask id=\"Activity_02klewn\" name=\"6\"></userTask>\n    <userTask id=\"Activity_0ylt45t\" name=\"5\"></userTask>\n    <sequenceFlow id=\"Flow_0dizg8q\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_0a6n8eh\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0e4xs2a\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_1jl42bt\"></sequenceFlow>\n    <manualTask id=\"UserTask_mu2c2p880\" name=\"3\"></manualTask>\n    <sequenceFlow id=\"Flow_0q59g41\" sourceRef=\"Activity_1jl42bt\" targetRef=\"Activity_0ylt45t\"></sequenceFlow>\n    <subProcess id=\"Activity_1jl42bt\" name=\"7\">\n      <startEvent id=\"Event_032ztbm\"></startEvent>\n    </subProcess>\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_flow_mu180u86md3e\">\n    <bpmndi:BPMNPlane bpmnElement=\"flow_mu180u86md3e\" id=\"BPMNPlane_flow_mu180u86md3e\">\n      <bpmndi:BPMNShape bpmnElement=\"StartEvent_mu2c2p840\" id=\"BPMNShape_StartEvent_mu2c2p840\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"142.0\" y=\"142.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"149.0\" y=\"185.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"EndEvent_mu2c2p8c0\" id=\"BPMNShape_EndEvent_mu2c2p8c0\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"812.0\" y=\"382.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"778.0\" y=\"393.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0qm5tc6\" id=\"BPMNShape_Activity_0qm5tc6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"780.0\" y=\"90.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_15cdzo6\" id=\"BPMNShape_Gateway_15cdzo6\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"545.0\" y=\"75.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1mzuxt6\" id=\"BPMNShape_Activity_1mzuxt6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"631.0\" y=\"170.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_0a6n8eh\" id=\"BPMNShape_Gateway_0a6n8eh\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"225.0\" y=\"365.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_02klewn\" id=\"BPMNShape_Activity_02klewn\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"200.0\" y=\"500.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0ylt45t\" id=\"BPMNShape_Activity_0ylt45t\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"610.0\" y=\"340.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"UserTask_mu2c2p880\" id=\"BPMNShape_UserTask_mu2c2p880\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"248.0\" y=\"102.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1jl42bt\" id=\"BPMNShape_Activity_1jl42bt\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"370.0\" y=\"350.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Event_032ztbm\" id=\"BPMNShape_Event_032ztbm\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"222.0\" y=\"182.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1g1p7by\" id=\"BPMNEdge_Flow_1g1p7by\">\n        <omgdi:waypoint x=\"178.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"142.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"248.0\" y=\"142.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1qxvbp7\" id=\"BPMNEdge_Flow_1qxvbp7\">\n        <omgdi:waypoint x=\"348.0\" y=\"134.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"548.0\" y=\"103.0\"></omgdi:waypoint>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"54.0\" x=\"404.0\" y=\"130.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1dpq6dk\" id=\"BPMNEdge_Flow_1dpq6dk\">\n        <omgdi:waypoint x=\"591.0\" y=\"96.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"720.0\" y=\"70.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"780.0\" y=\"103.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0icqanz\" id=\"BPMNEdge_Flow_0icqanz\">\n        <omgdi:waypoint x=\"570.0\" y=\"125.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"570.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"631.0\" y=\"210.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1aqie0c\" id=\"BPMNEdge_Flow_1aqie0c\">\n        <omgdi:waypoint x=\"880.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"848.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_016it02\" id=\"BPMNEdge_Flow_016it02\">\n        <omgdi:waypoint x=\"731.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"382.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_13gvmcu\" id=\"BPMNEdge_Flow_13gvmcu\">\n        <omgdi:waypoint x=\"250.0\" y=\"415.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"500.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1kkbmin\" id=\"BPMNEdge_Flow_1kkbmin\">\n        <omgdi:waypoint x=\"300.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"418.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0t3saos\" id=\"BPMNEdge_Flow_0t3saos\">\n        <omgdi:waypoint x=\"710.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"812.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0dizg8q\" id=\"BPMNEdge_Flow_0dizg8q\">\n        <omgdi:waypoint x=\"298.0\" y=\"182.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"298.0\" y=\"274.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"274.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"365.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0e4xs2a\" id=\"BPMNEdge_Flow_0e4xs2a\">\n        <omgdi:waypoint x=\"275.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"370.0\" y=\"390.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0q59g41\" id=\"BPMNEdge_Flow_0q59g41\">\n        <omgdi:waypoint x=\"470.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"540.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"540.0\" y=\"380.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"610.0\" y=\"380.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>',0),('12579',1,'flow_mu180u86md3e.flow_mu180u86md3e.png','12577',_binary '‰PNG\r\n\Z\n\0\0\0\rIHDR\0\0ı\0\0N\0\0\0\åg\0\0Q\ÎIDATx^\ì\İ\rœ\ÜU}/~D‹\"•Vo\í½½\Újk¯½µj[«­WKª\õ\Û\Âf³cƒ€\ÕJ\"\ñj@[Ë¿µ…„Š´µŠ(UsM½\à„lb\ÃS\n\"`	I\ÈHx\Ê\Üs6;\Ó\õ\ìL2;³3\ó›\ó{¿_¯\ïkv\ç\÷›\ßü²\ç\ì9\ç“\ß\Ì\ì~û\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0ıc\ö\ì\Ù§\÷\0\0\0}f``\à€Y³f½&\Ô	ƒƒƒÿ\ê‡\áë§†††>%ü\0\0@Ÿ8\î¸\ã~&„ú\ß¡ş¸P\ç‡Z\ê‘P7„ú\\\Ø\ö¾P¿;00\ğ’úÿ%Ü·!\ÔûgÌ˜\ñŒ\ôX\0\0\0@Ä€?<<ü\êŞ\rÁı¼P\×Æ€Bı\á\ö\ó1Ì‡zİœ9s•>¶*<\ö•aÿ\ï†ın·’n\0\0\0:,^‰=„\ó?\r\áü\Â\íÕ¡uS¸ÿ‚P\ê\÷LÛŒ\ğØ·„\ã^\÷ıxœt;\0\0\0\ñ\ì\ô\ò‚û\ÓC\0E\àsB-µz<\à\ß\êÂ°\íÄ™3gş¯‘‘‘ƒ\ÒÇ¶c\áÂ…û‡c\ãPK\Âyüjº\0\0\0\Ó\çP\Ço\ÉP\Ú!\\ÿ\Ï\ñ°}N¨+C\í\Z\ZúP_\n_\Ï\õÆ°\ÏÏ¦\í”øv€p>Ï»9\Ü.\n\ç\ñ\é>\0\0\0´§\ZøO¿üû\\ø!@¿<\Ô\Ñ!Lÿ}\ÕW„¯·‡\Û\ÛB]¾>)\Ô!‡~øs\Ò\Ç\öBû\ãÿ\Ãÿ‡\÷\ö\Ù\0\0\0\04/½ÂŸ~O\ñ=-\å_y8„\çO‡ú^øú¡P?	\õ•°í”™3gş\áÀÀÀÏ¥,šø2ÿp\ÎKB\İ_‘ÿ\ó\"\İ\0\0€\æ4\nø\î§\÷ƒq\Ä!\Ïú›P+B=jmû_·B½yxxø¹\éûIü€¿\ğ\ïø~¨\âÿ¥\Û\0\0Ø»}û}m§Bxi½3C\È?+\à\å\áv[¸½+\ÜşŸpÿGb x^ú¸\\„\ë‡º5ü{¿ş­¯L·\0\00Y³¾\Ùı˜!\Üşr¨£B¸ıT¨e\á\ëf\íùtû¥!\ô~lxxøm\ñ½\ï\é\ãrÿ„`ø¼?Ô†\ğ\ïÿ—¦û\0\0\0°\ÇTƒüT\÷§	!¸¾8ù?	A\ö/C]\ZjK¨ûB}#\Ô\éa\Û;FFF~1}\\™\ÅÿyÅŸ\Õ_Î=û\àt\0\0€2k5À·ú8‚Nÿû\Ğ\Ğ\ĞÿA\õ¡¾jS¨\õ!\Ø_n?\êĞÿ–>ú\â•şx\Å?^ù\õşøJ€t\0\0€²i7¸·ûøR!\ô¡¡\ôŒ\ê¿¾\Ş\êşPßŒ\÷…:,\î“>©‹\ï\ñ\ï\õ?\Ï\Û\â«&\Ò\í\0\0\0e\ÑL`¯L¨Fš9NiÄ«\ó!l¾küjı%\ãW\ãUüo‡úD\ØvD¼ÊŸ>\é?\Ì0ü¬¯?\óï‡¯?\İ\0\0³fƒz3¡?j\öxY‰\ï¯~g–§‡ú¿¡Ö…\Ú2~¥9¾\Ïü\ãû\ô\Ó\Ç\Ñ.\Ü?´\Å1³\ö|\ğ\á’øg\r\Ó}\0\0\0r3•€\Şlè¦rÜ¾?!?\Ô\Û\ã\'\æ‡ú·\"\ï\rµ5\ÔeãŸ¬døú—\Ó\Ç\Ñ{s\æ\ÌyVh³‡\ö\Ùn•\ñ¯\0\0\0\å0\Õ`>•\ĞM\õø…t\Ì1\Çü—\ß\Z\Â\áGBPüz¨»\Ã\÷\Û\Â\í\å\á\ö¬P\Ã\Ã\Ã/MG±Å°\Ú\ğœ\ñ\ğÿ\áøŸ\é>\0\0\0ıª•@>\Õ\Ğµ\ò<=\ÂûsgÎœùG!.ˆ/ug¨C@\r·jpüe\áOKKŠ\í9\Ş\Ö\÷Ä—ÿÇ·¤û\0\0\0\ô“Vƒx+¡?j\õù:*„½ŸaşM!\è\ßWC\İ\ê¡P+\ÃıŸ54<<ü²ıüRˆ\ğ\Úşû¡nˆü—n\0\0\è\í\ğVC\Ô\Î\ó¶m\ö\ì\Ù‡07#„¹“Bıkøú\öp»=Ü®\n¡ÿ\ïC\Íøû	ø¥ú\Ä‡º5~\0c\è#¯L·O‡J¥rÀÚµk¿¶z\õ\êÇ—/_^Y¶l™\êr…Ÿû\îU«V­=\"m\0\0\èW\í\ïvB\Ô\î\ó7%„\÷Ÿ9s\æ„\Ğ\ö¡Ş¾\\¨\ãWq\ã{¸GÂ¶_\÷2n\Z™1c\Æ3B?y¨\r!øÿK\èS/L\÷iGüKB\à¬lÜ¸±²s\ç\Î\Êc=¦º\\\ñ\çş+W®|hÙ²e‡¦m\0\0ıf:w»¡?š\ó¨9(„²7„šÚ…¡~\ê\áP?µ8\Üÿ\îP¿B\Û\Ó\Ó\ÇÂ¾~ø\áÏ™µ\ç\Ï-n‰·\ñ#\é>­ˆWøc\àLƒ¨\ê~mØ°aKı×¤m\0\0ıdº‚\öt„ş¨¥\ó9\ì°Ã‚\×\ëC}0ùB\İ<\ğ¯\Zü‡P\ï	¡\ì7|¦[¼\Ò¯ø\Ç+ÿ\ñ\0\ñ•\0\é>©\ğ˜\Òûª\âKú]\á/F\Åv¡W\ÚF\0\0\Ğ/Z\n\Ø\rLW\è\öz^!0Bü\ï…ú@YŸ\õ£P„º&\Ôy¡\æ†m¯j&|Át‰\ï\ñ\ï\õı\ï¶pû\'\é\ö‰\Â>ßŒo#I\ï\â{\Ê\Ó\ğ©zW±=\Ò6\0€~°\×`İ‚\éı\Ñ\Øù=\ëY\Ïú£\á\á\á×†@\õg!(}.\Ô\Çşu¡\Î\õ\ŞP¿u\Üq\ÇıLz\0\è…ø\éş!\Ğ_ú\å\÷\Ã×¿Ÿn\Û\Ş¶Ub®\×o›\rı?¸¡²\ö\Ú\ÏWn¾üÌ±Š_\Çû\ÒıT{%\ô\0Ğ¦;\ğG\Óú\÷ÿ\ÍGy\ä\îP\ñ\Ê\é?…\0uü\ğ\ğ\ğ\ï\ì\í¥\ÑP\ñƒ CŸ=&û{B-	}\öW«\÷\Ï\Ú\óÊ”úc™>¶™Ğ¿cÛº\ÊM—^ù\á·Nù©Š\÷\Åm\éşª\õú\0\è7üÑ´‡ş(ÿı:s¾\Ğqs\æ\ÌyVÿ\á~s¸]\ê\Ä	?\Ö¡^7\ñ1Í„ş{o¾xR\à¯\Ö}7_2i\Õz	ı\0\0\ô“Nş¨#¡\\\'\Ï:nhh\è\âJ†Û§’\Ğ\ë\ÖøA”\Õ}›	ı?^qÖ¤°_­¸-\İ_µ^B?\0\0…?¸n``\à\ç\Òû\'h&8Oî½ªF\Ùo\ß\ç…\ÂıiuµÎ­\î\×L\è¿i\Ù\ÂIa¿Zq[º¿j½„~\0\0\nahh\èŒui¨\÷†\ïÿ\ë„\Í\ñ*b\Ì\'O¸¯4€\÷¢\ö&üwÔ®ŠB?ù\Å\ğ;¹½NØ¯\Ö\îÁÁÁ·\Æ}…şb•\Ğ\0@!„\Ğ0œ„ˆ§B\Èø^¨y\Ã\ÃÃ¿´_sW\Ê\Ó\0Ş‹j\äı\ö}şPH\á\÷\ñ3u‚şOUø]\İ~WŸ\ÛLèŸÖŸ†ıj\Åm\éşª\õú¨©T*¬]»\ök«W¯~|ù\ò\åc“„\ên…Ÿû\îU«V­=\"m\È\İ\à\à\ào§!\"©k\ß\ò–·ü\óÁ¼\å\ÏxÆ›\Ò\ÇOƒf‚{«~ú\ÖÀÀÀ\ñ¯O„\ßÁ\Ï\Ç\n\á~4Ü®ˆ¾şp{W¨-¡v‡Zûo|cR\ğL\ë\ö+Ï\ö«·¥û«\Ö+®/\Ò6 ¤B\à_ge\ãÆ•;wNš4T\ç+ş\Ü\ã\Ï\åÊ•…IúĞ´ g###‡†4\ìO¬\ÇC\0\Ùz\ÔQG\í\î@\ğ\ïT\èø)…\Ã?ü9\áw\ô/\çÎ[Ù±cÇ¤9nbm]wc\å¦\Ë>>)\ğ\Çû\â¶t\Õz	ı\0\Ô\Ä+ü1p¦“…\ê~mØ°aK˜¤¯I\Ûr4cÆŒg¿lhh\è°ÁÁÁmu‚~µîœµ\ç½ş/\ïĞŸÁ\ëD\èø)\ï|\ç;“\æµzu\çu_œú\ã}\é~ª½ú¨‰/\éw…¿\Û!LÒ»\Ò6‚~6gÎœŸ¡ıu!´¿;\ÜşU¨¥¡n	µ3\Ô!\ğ+\Ü\Ş]\'\ì?¶}l``\à\é\É!§;POw\èŸ\î\óƒ¾Cf:¯Mª]»*·ÿ\à¼I¡?\Ş·M\Ú_µ\\B?\05MMÒªke’¦-\\¸pÿ\Î_Bú;BXŸn?nW†ºü“¿¯\ru\á¬=úë¨°ı\ïx\Ç;Y}|¸oqøo~\í\Ä\çHLg°\Î\Ğ?\ç}e_\ë‰\Û\ÖUn»r\ñ¤À_­¸-\î“>NµV\Ö\0\Ô\ìk’X»v>R\Ù|\÷µ•Ÿ\\\õ•»ø&mW\í—Iš\"ÁşgCÿ\Úg‡`ş‰\è¿¾¾1|ı\è¬=W\ë\ãŸ\İ;\'\Üÿg¡\Ş¾~AzŒz\Â~\ïŸø?Ÿ\'İ§\é\n\Ø\Óú§\ë| /5\\O\ì\ÚU\Ùpûh\åG\ßıè¤ ŸV\Ü\'\î\ëªûe=@M\ÃIzBm\Ûx[\å\î¾V¹\é\Ò\Ók\ó­Wœ=i?\Õ~™¤)€§…\àı¢\Ú\ß\ê\ÏC¨ÿ‡\ğı\å¡\î\õpøşúpÿ¿†¯?¾5<<ü\ê\Ã;¬­¿??s\æ\Ì?\n\Ç\Û\Z\÷\'\é¶}˜ =¡:\ÎúZ½\õÄ¾®\î7*Wı\Û/\ë	\0j\êMÒ±İ±­²\î\Çß­\Ü2ú©ŸŒ¿}je\íµ_¨<üÿ\ëD™¤\é–\ñ?\Ç\õª¶c€uQ¨Á>ÔºP\Ë\Çÿ\Ã\í[\Ãş/{Zzœ\éÿ\Æw8ş\Óû›\Ôn\àn7\ô·ûü…z\ë‰f®\î7ªø\Ø\ôxªù²\0 ¦\Ş$ı\È\öM•¯8k\Ò+^\í\Û\Òú•S\Ù|\÷5“¥¦V&i¦[\í/˜9s\æ†\àş¾\ğ\õ9\á\ö»³\öü}\íø’ü…Z2k\ÏK\õ·¯‰~+=Fh\'x·ú\Ûy^\ÈJ½\õDº†˜j¥\ÇSÍ—\õ\05\õ&\éÿX\õ\éIo3\å}ş\í—IšV\ÄÅ‹‚û‘\ñ\ïg\íùĞ¼kB=4kÏ‡\é}/Üjş\ğ\ğ\ğ;C½4~ø^zœ>\×j\0o5\ô·ú|¥z\ë	Õ»²\0 ¦\Ş$†ùzµ\ö\ÚÏı]\İZ­ùReı­\Ë*\Û\î¿u\Ò\ñT\óe’foFFF~1\÷7\Î\Ú\ów\ë?=¸\ç\Ï\İıdÖ?wKøş\ß\ÂıŸŠ/|ı{\ñ\Ï\å¥\Ç\È\\+A¼•\Ğ\ß\Ê\ó@\Ö\ê­\'T\ï\Êz€šz“t\Z\ğ§R7/;s\Ò\ñT\óe’\æ¸\ãû™\Ú_Bû!\È/\õùP?\õ@¨-¡®\õ¹°ı”p{x¸ıµ3f<#=N‰M5O5\ôO\õøP\n\õ\Öªwe=@M½I:\r\ò\õ*~²\î\×ü\ËO\Õ]ÿş\å\Ê\Öûn˜t<\Õ|™¤\Ëc``\ày!´¿>„\ö\÷„:+|ıC\Øÿ\ñ«\ö·…º$\Ôß„š\îC¨_HACS	\æS	ıS9.”J½\õ„\ê]YO\0PSo’¾\ñ;™\ò›©¿\ó\á\Ê\ö­wO:j¾L\Òy	Áş\é¡~5„úw…\Ğ~R¸ı§\â¯µ)Ôƒ¡®\n\÷_\ê#\á\ë?ûş\ÏP¤Ç¡%\Í\ôfC³ÇƒRª·P½+\ë	\0j\êM\Òw®¹pR O\ë–Ñ¿š\ô	ş\ñ\êÿ#;¶L:j¾L\Òıi\ö\ì\Ù¿6\÷‘P\ê\ë!\È\ß<k\Ï\'\ä¯\r\õ\íøÿ>\Üw|¨CB°ÿo\é1\èˆf‚z3¡¿™\ã@©\Õ[O¨Ş•\õ\05\õ&\é\í[\ï»jŸız¡?ş©¾­\ë~8\éªµ2IWü´û\Ş9„\ö·‡\0bøú¼P+B­µ#\Ôu¡¾\ê\ô°} \ì\÷\Ê9s\æ<+=]\×n`o\÷\ñP\n\õ\Öªwe=@M£Iz\ëº+7_ş‰Ia?­øV€m\÷ÿx\Ò\ãUke’î½‘‘‘ƒBhÿ\íŞ‡Cp?#\Ü~5\ÔC=\êP—…Z\êı¡\Ş<{\ö\ìÿƒ\Âi5¸·ú8(F\ë	Õ›²\0 \Æ$]¬2IwO\ë3g\Îü£\ğ?C|ø\Ë\Â\í½\ã\áş†P_‰¡?\ÔPøú·\â¤Ç ¯L5ÀOu(5\ë‰b•\õ\05&\éb•Izz\r_fjf|\Ùı¬=/¿_3k\Ï\Ë\ñ×‡ûG\Ã\íg\â\Ë\õ‡‡‡\ß\ê—\ÂÃ–‡l4\ä›\İg=Q¬²\0 \Æ$]¬2I·&~0^\ï3B?0/\Ü~\'Ô³\ö|\ŞM\á¾ÿ3k\Ïì„¯7~\ğ^zJc_~_Û:¬\'ŠU\Ö\0Ô˜¤‹U&\é\ÆâŸ²‹\Ò.„\ö?	\áı£\ñOİ…Û«g\íù\Ów\ñO\à­\n\õ\áş“\Â\í¡a¿_‰2/=\ì\×8\Ø7º\Ø\ë‰b•\õ\05&\éb•Iz,\Ü??\÷7„:6\÷¿\rş’P·‡\Ú\îûpûP¶½\'|ÿû\Ã\Ã\Ã\ÏMMH~ú=0\Ö\Å*\ë	\0jL\ÒÅª²L\Ò3f\ÌxF\í¿\Âû\á!¸Ÿ\Zn?\ê\ÊP[\Ç\ë\áş‰\Û\Â~Güø˜\ô8Ğ¦j\Ğ?yüV\à‡YO«Ê²\0 	&\ébUn“t¼\nBû\ï…?gü\êü¿…\ÛÇ«\ö¡~\ê›\ãW\ó\ß\ê\ñ*z\è°CBß‹¿w?´Áz¢X\Õh=qÿı\÷¿\ä{\î¹\é†nxø/ş\â/ş\Ê¨”€IºX\Õh’.²…\î\ß?ş;\Ã\í‡f\íy_ı\÷\Â\í\ÆP…º&\Ôg\íyş‘¡~\ã\ïx\Ç3\Ó\ã@¯Œ‡~ \r\ÖÅªt=Q©T~\Ûm·ıı®]»ªŒû=\ñÕ¯~u\İ{\Ş\ó\×O\Ü€Ì˜¤‹U\é$]$‡~øs\â\'ß‡\Ğ~t¨O\"şf\íù„ü;\Ã\÷\ß\r\÷Ÿ¾>a\æÌ™x\ô\ÑGÿ\é1 ˆ„~hŸ\õD±j\âz\â\Î;\ï<d\ãÆ›ªa?u\×]w=~\ÖYg-ø¹‰m\n@&L\ÒÅª„ş§Å¿Uü[Cú`¨Ï„Zj]¨‡Cı{¨‹\Â\ö¿f¸}UX$˜ú‰\Ğ\í³(V\Å\ö\ó“Ÿü\ä[O=U»¸\ß\Ğ\î <f\Çüù\óß¶-\0}\Î$]¬\êV\è?\ì°Ã‚\Îo…\Ğnf-\õ•\ğ\õ\õ\á\ö‘P\÷†º<Ô¹\á¾„pÿ–\ğ\õ‹\ÂÃ–r \ôCû¬\'ŠU«W¯®<\ô\ĞC;\Óp¿/Û¶m{ü³Ÿıìš—¤m@Ÿ2I«¦;\ô‡Iû…!Ğ¼9\÷?~Qøú²Pw‡û†û¿n\Ï5üN\Øÿg\Óc@\î„~hŸ\õDqjıú\õ•¿ıÛ¿­lÙ²%\Í\ôû\ÇE•O¥¿¯@É˜¤‹U­„ş9s\æ<k\ö\ìÙ¿Bı@\ØOAşK\á\öºp»=\Ünµ\"lûlø~^¨·\Çÿ½¾—\ÊÊ‚\Úg=Q¬Zºti\å\Øc½lttt\ó\îİ»\÷ıúşJ\å\É\r6|\åOÿ\ôO\ãg\õ|Û•ş|˜\ã\0“tÁjo¡?\öÿ\Z\ê0x\ê\ï\â¤\êP;\Ãı7‡Û¥¡ş*üc†‡‡_\Û/\È\ó¾\Ï\\\ñ\Üy‹F·¦\÷wCú\Ü\óÿaÅ¯¸h\ô\Ç\÷i$},ıË‚Ú—\Ãz\â\á‡®r\È!q<˜´­ßªºˆŸ¿sú\é§o\Ü\Ûù=\õ\ÔS\×\Ç\òc\á}qÿ¤i\és\æ8 ‹I:§º\ô\ÒK+!´ÿz ÿ8\Ü~8\Ü~!\ÔU\á\ëm\ávs¨+Bıs¨“\Ã}\ï\n\áşe!\Ü?=mWZÿO<g\ô\ï\ÒûÉ›´¯\ß\×\ñ=\ğox\Ã\Æ¬t{¿\ÕÄ‹\ñ\"À\ìÙ³Ï»è¢‹|\ò\É\'\÷·_w\İu_\Û\î\ã\àgú\åbScú~’Î©}\ô\Ñ\Ê\È\ÈH|\ïÕ­¡ş\ï\Ğ\Ğ\Ğÿnÿ4\Ô\ë9\æ˜ÿ’¶]?š·h\ô›\ó-?l\ì\ë\Å+>>\ïœ\å\'\Ï[´bv\Ú_®\í\ï_4º)Ôj\0?ùo.=hŞ¢åŸŸ¿h\ôpÿ=\'.=|\Â1·,8\Ù\Ï\Í[<ú¿\Â\×Wœx\ö\èÿû\Ş·\Í_<:üS\ÇN\öMŸ;„ş\ï„û¾Ÿ\ç\ÄE+\ÔW\çœ\Ò\ÇÒ¿,ˆ }ı¾ÿ„\Ê\ïş\î\ïfú«\ï\ÄO¼%¸ÿ¸\âCú\Ğhÿ~\ïO\÷%\æ8 \ï\'\é\Ü*^\éO\Û(\'Õ—Ï‡ú\ËP\Ç\Äû\Æ\Ãÿ\Â\ÚN•\Ê\ÓN<gÙ‹\çŸ3:³ú\òù\ÒÅ«\ğ¾\è\Û‡Àÿ¶pÿú\ê\î! _ù¡E£¯š\Î\òKN<wÅ»N\\´ü½\ñ?\â¶\ğ\õi\î;\ñ¹\çÿ\İ\Ç}hş9+\ŞşÁs–¿>|ı`\õq\õ\Ï)9oú–´¯\ß\×GqDeÃ†\rY‡şhÆŒ\Ï\Z\Z:5Œ{·\Ç\Ûø}ºy1\Ç}?I\çV&\éœ\Ä+\é\ó\Îı\È~?ú¥x\Õ<~½\'°\Ş¯Ô‡\Û;C}q|Ÿ\õ\ï?\÷\ò±W;œ\ô·+~!|¿m\Â\ã?7\ñ\ò‡\Ç\Ü\Ãy|Ì¼sV»g\Û\ò«»Á¾µç·xù;N\\¼biüzş\ß-aØ¶1~½—sª=–şfA\í\Ëe=±_æ¡Ÿ\ò1\ÇA\Æ\Â/øŠP3\ÒûS¹LÒ¹T\î“tûgÕ¢Ñ•\óş~\ÅÏİ·h\ô\êù‹–¿vü\ë?xÎŠWœx\ö\è\ë\Â\×\ë\â\Ë\ğ\Ç\ï\ßúÁsF\ß<\ö\Òü±·,ÿ§\Ú1\Z¶o\÷Œ\ï»fş¢\Ë\ß\ßF¾~²z\ìûş\çs\ÇW,ı\Ö\Ø*„¯«¯\Ø\Ë9\ÕK³ ‚\öå²\ØO\è\'3\æ8\ÈXü¯½†ÿ\\&\é\\*\çIzş¢\Ñ3N\\4ú\ñ\ëøüš/\Z¿ÿù\÷\İ\çÅ¯\Ã}Ÿ	\õH|ù}\Û¾h\Õ\ókû/½#Ü·#\Ü~\áÔ³¾ÿœ\Úq\Ï=\"\Ü\÷Â…+\Æ^¢wÎ¼ø2ıE£ÿn·T\İ`ß‰\Ï}ûø{\÷·\Î;g\Åeû:§‰¥¿YAûrYO\ì\'\ô“sdlB\è\ßkø\Ïe’Î¥L\ÒS3ş>ü/\Í_<úg\é¶\ÔT\ö¥\\,ˆ }¹¬\'\öúÉŒ92V\'\ô\×\rÿ¹LÒ¹”Izj\â•ÿù‹V|m\áÂ…û§\ÛRSÙ—r± ‚\öYO«¬\'¨2\ÇA\Æ\ê„ı´\ÆÂ¿IºXe’†\î³ ‚\öYO«¬\'¨2\ÇQ(=\ô\Ğ\ó.¾ø\â3\Ï=\÷\Üq\Æ[,X°\ó}\ï{\ß\î\ØQO8\á„\'N>ù\ä\íû\Ø\Ç\î9\ó\Ì3/9\å”S\Ş\ò´\ôEV\'t¢L\Ò\Å*“4t_\Óû€©±(VYOPe£®¾ú\ê\Î>û\ìu\Ç|%ıÊ’%K*kÖ¬©\Üq\Ç•­[·V¢x¿\÷\Ç\í§z\ê\î¹s\ç>\Âÿ¥\Ã\Ã\Ã/MYD\İş…K\Ãı„Z1\Ë\Ëû[&i\è¾nÏ#\ë‰b•\õU\æ8z\ê\Æo|\çg>\ó™\õ\Çw\\\å\ë_ÿze\ó\æ\Íc¿Yqÿø¸şŸ?ş×†††~!}\"\é\ö/Ü¾\Â~U?O\Ò\ßû\Ş\÷*¯y\Ík*tP\åU¯zUeÅŠ“\ö\é·2IC\÷u{|†\õ\óz\"Ç² \ÊGO„¼ş\ôoû\ÛÇ°ÁTv\ìØ‘\æù)‰Ç™3gÎ£G}\ôÿNŸ¯(ºı·¯°_\ÕÏ“\ô›\ßü\æ\Ês\óœÊ¥—^\Z¶•¾\ğ…“\ö\é·2IC\÷u{|†\õ\óz\"Ç² \ÊG×…Œş\ó^x\ám\ïÿû+k×®M\ó{[\â\ñ;î¸s\ç\Î=#}\Ş\"\è\ö/Ü¾\Â~U“\ôW\\1ú_û\Ú\×N\Ú\Öoe’†\î\ë\öø9\Êa=‘SYOPe£«B.ÿùE‹m;\í´\Ó*Û¶mK3û´ˆ\Ç]°`Á£\Çs\Ì\Ó\çïµ¢ş\Â\å0I|\ğÁ•¼\à•\Ûn»mÒ¶~+“4t_Q\Çg\è\'9¬\'r*\ë	ª\ÌqtM\È\ãO¿\ğ\Âo?D\ò\É\'?8{\öì¥\ç\ÑKEı…\Ëa’^½z\õØ•ş7¾ñ“¶\õ[™¤¡ûŠ:>C?\Éa=‘SYOPe£k\â{ø?\ğt\ì\n*>Ï±\Ç»=t\ò\Ã\Ósé•¢ş\Â\å0I?\ò\È#c¡?~ _º­\ß\Ê$\r\İW\Ô\ñúIë‰œ\Êz‚*s]qı\õ\×\Z?´oº\ßÃ¿/\ñù>ú\èm\ÏOÏ©Šú\×Ï“\ô\Ş\ğ†Ê³Ÿı\ì\ÊÒ¥K\ÇBÿ[\ßú\ÖIû\ô[™¤¡ûŠ:>C?\é\ç\õDe=A•9®ˆ–/~º~/œw\Şy\ëCG?\'=§^(\ê/\\?O\Ò\ñO\ôı\æoş\æX\ğÓ›\Ş4\ö=\é>ıV&iè¾¢\Ï\ĞOúy=‘cYOPe£ã®¾ú\ê\âUş\íÛ·§y¼+\âóŒŒ<4<<ü\Ò\ôÜº­¨¿p&\éb•I\Zº¯¨\ã3\ô\ë‰b•\õU\æ8:\î\ì³\Ï^_z\İK_ü\â\ïıü\ôÜº­¨¿p&\éb•I\Zº¯¨\ã3\ô\ë‰b•\õU\æ8:jû\ö\íÿ\å„N¨lÚ´)\Í\á]ÿ\É\á\á\á.\Ü?=\Çn*\ê/œIºXe’†\î+\êøı\Äz¢Xe=A•9ú\æ7¿yÆ‚\Ò\Ş\Ç{\ì]¡Ã¿>=\Çn*\ê/œIºXe’†\î+\êøı\Äz¢Xe=A•9Z¼x\ñ–,Y’\æ\ïø\ìg?{C\è\ğc7\õ\Î$]¬2IC\÷u|†~b=Q¬² \ÊGGq\Æ[Ö¬Y“\æ\ïXµjÕƒƒƒ§\ç\ØMEı…3I«L\Ò\Ğ}EŸ¡ŸXO«¬\'¨2\Ç\ÑQ§z\ê\Îø\'ÔŠ\à\Ö[o/\ï¿6=\Çn*\ê/œIºXe’†\î+\êøı\Äz¢Xe=A•9:\á„v?\ğÀişî‰­[·\î~]z\İT\Ô_8“t±\Ê$\r\İW\Ô\ñú‰\õD±\Êz‚*s522Ry\â‰\'\Òü\İ\á<¶\Ç\ß\ëJFE`’.V™¤¡ûŠ:>C?±(VYOPe££?şø\'Šr¥\ó\æ\Í?™\Õ\ã+ıEe’.V™¤¡û,ˆ }\Ö\Å*\ë	ª\Ìqt\ÔI\'´½(\ï\é¿\å–[®›\Õ\ã\÷\ô•IºXe’†\î³ ‚\öYO«¬\'¨2\Ç\ÑQ§vÚ½Eù\ôş\Ë/¿|y¯?½¿¨L\Ò\Å*“4tŸ´\Ïz¢Xe=A•9Z¸p\á%K–,I\ówO|\ò“Ÿ\\\Z:ü_§\çˆIºhe’†\î³ ‚\öYO«¬\'¨2\Ç\ÑQ,xÛ©§º;\r\à=\ğ\Ä\ğ\ğ\ğ¡Ã¿>=GL\ÒE+“4tŸ´\Ïz¢Xe=A•9Z¸p\áş\Ç{\ìc›6mJCxW\İw\ß}W†Î¾!Oz˜¤‹V&i\è>\"hŸ\õD±\Êz‚*sw\Ê)§\\ºt\é\Ò4‡w\Õ\'>\ñ‰‹Bg??=7\ö0I«L\Ò\Ğ}DĞ¾\åË—\ïŞ¹s\ç¤yMu¿B;¬\ë‰]iQN\æ8:nxxø¥s\ç\Î}|û\ö\íi\ïŠmÛ¶]:ú¦xé¹±‡\Ğ_¬ú¡û,ˆ }«V­Z¿q\ã\ÆI\óš\ê~\İ}\÷\İ_	\ë‰k\Ò6¢œ\Ìqt\Åüù\ó¿vÁ¤y¼+\Âs5t\ôs\Òs\â?	ı\Å*¡ºÏ‚\Ú7::z\ÄÊ•+Ú°a\ÃWü{S\á\ç¾á®»\îº(¬%\î	uh\ÚF”“9®xşœ9s]»vmš\É;\êºë®‹/\ë\ßŸ?=\'ş“\Ğ_¬ú¡ûÊ¸ \n\Ó\äa^ş\Ú\êÕ«_¾|ù\ØØ£º[\ñ\å\ğ\ñ\êx\Ëiû\ô«\ğ\ï:4\Ô5¡Kÿ½ª+\î\ñ\ç/\ğSS\Æ99ú\è£ÿ\÷\ñ\Ç¿sÛ¶mi6\ïˆM›6}/t\ğ»C?-Ni\ğT½«\ØiU\ÆQüKB\à¬Ä—c»*Û›Š?\÷ø\óWÇ…4 S\Ê8\Ç\ÑCs\ç\Î=cÁ‚Æ‰®“v\í\Úu\Ë\ğ\ğ\ğµCCCIÏÉ„şb•\Ğ\İW\ÆQ¼\Â\ïı\×Å¨ørøxu6m#€\éP\Æ9;\æ˜c¾x\ò\É\'?Ø©+ş›6mZ\è\Üÿœ>7\õ	ı\Å*¡º¯Œ¢ø’~Wø‹Q±–ù¤u C\Ê8\ÇQ\0³g\Ïş\ØÜ¹s·O\÷{ü\Ç\ß\Ã+üS#\ô«„~\è¾2.ˆŒı\Å*c?\Ğ)eœ\ã(ˆø^û£>z\Ûyç·~Çi~Ÿ’|\ğ\ê\ñO\é\ß\ä=üSg\áW¬²\ğƒ\î+ã‚¨Ù±ÿ\á7T\Ö^ûù\ÊÍ—Ÿ9V\ñ\ëx_ºŸj¯Œı@§”q£@†††~!t\ÂsFFFú\Â¾p\Ç\æÍ›ŸHı^<±nİº+\Ï8ãŒ±O\èÇ‰\ÇKŸƒ}kvá§ºS~\Ğ}e\\53\ö\ïØ¶®rÓ¥§W~ø­S~ª\â}q[º¿j½Œı@§”q£€†‡‡_\Z:\ãù!´oz\ï{\ß{\÷g?û\Ù\ëW­Zuã­·\ŞzÏ–-[Ç¦M›\Ö\Ş|\ó\Í×…Iq\Ù\'?ùÉ¥\á17†\Ç\ÜŸ“\æ5³\ğS\İ+?\è¾2.ˆšû\ï½ù\âI¿Z\÷\İ|É¤ıU\ëe\ì:¥Œs\Å\ö´™3gş¯ş?588xq\è ×„º7v\Ô\ñ\Ûk\âıq{\Ü/\îŸ€©kfá§ºW~\Ğ}e\\53\öÿx\ÅY“\Â~µ\â¶t\ÕzûN)\ã$–/_¾\Û\'8£B;¬_\æœ¡\ëÊ¸ j&\ôß´lá¤°_­¸-\İ_µ^B?\Ğ)eœ\ã€ÄªU«\Öû[\ÍÅ¨»\ï¾û+\Ëü­f\èº2.ˆ„şb•\ĞtJ\ç8 1::z\ÄÊ•+Ú°a\ÃWü{S\á\ç¾á®»\îº(,ú\î	uh\ÚF@g•qA\ÔLèŸÖŸ†ıj\Åm\éşª\õúN)\ã\Ùz\ğÁ\å’K.Yu\î¹\ç\î\\¸p\áS,¨¼\ï}ï‹Ÿ‡P9\á„*\'t\Ò\î~\ô£Oy\æ™\÷Ÿr\Ê)§…‡\ì_}lš\ñ\ns¨\Ç\âÂ£+ş;\Óûú¨\â\Ï=şü~\è2.ˆ\âØ“Ï´n¿\ò\ÜIa¿Zq[º¿j½b{¤m0\Ê8\ÇAv®¾ú\ê‹\Î>û\ì\'?şøJúK–,©¬Y³¦r\ÇwT¶n\İ:\ö\÷\r\ãmü>\Ş·\Çı\æÎ[9\õ\ÔS·\r\r’³Ğ€V•qüh&\ôo]wc\å¦\Ë>>)\ğ\Çû\â¶t\Õz	ı@§”qƒlü\èG?úøg>\ó™\';\î¸\Ê×¿ş\õ\Ê\æÍ›\Ç~³\âş\ñq1üÏ›7\ï\ŞÁÁÁ_KŸ£ŸĞ€V•qüh&\ôÇº\óº/N\nı\ñ¾t?\Õ^	ı@§”qƒ¾\òú\ßş\ö·7Ä°ÁTv\ìØ‘\æù)‰Ç™3gNedd\äS\é\ó\õ\ZĞª2M…ş]»*·ÿ\à¼I¡?\Ş·M\Ú_µ\\B?\Ğ)eœã ¯…ŒşK_ü\âÿû\ß_Y»vmš\ß\Ûÿ#a\îÜ¹\ßKŸ·Ğ€V•qü\ØW\èß±m]\å¶+O\nüÕŠ\Û\â>\é\ãTk%\ôR\Æ9úV\Èå¿´hÑ¢İ§vZeÛ¶mifŸ\ñ¸\ñış\ï~\÷»\ïHŸ¿\èh@«\Ê8~4ı»vU6\Ü>Zù\Ñw?:)\è§\÷‰ûº\ê\ß~	ı@§”qƒ¾\òø^x\á\ã1\ğ\Ç\ÅA\'\Å\ã\ÇOúŸ={\ö²\ô<ŠÌ€´ªŒ\ãG½Ğ¿¯«û\ÊUÿ\öK\è:¥Œs\ô¥øş|\à»ÂŸŠ\Ï3w\î\Ü\İCCCŸLÏ¥¨h@«\Ê8~\Ôı\Í\\\İoT\ñ±\é\ñT\ó%\ôR\Æ9ú\Î\õ\×_¿0¾\×~º\ßÃ¿/\ñùfÏ½û\È#|yzNEd@ZU\Æ\ñ£^\èOƒüT+=j¾„~ S\Ê8\ÇAß‰–/~º~/œw\ŞyO†\â†\ôœŠÈ€´ªŒ\ãG½Ğ¯zWB?\Ğ)eœã ¯\\}\õ\ÕÅ«üÛ·oO\óxW\Ä\ç‰/\ó?$=·¢1 ­*\ãø!\ô«„~ S\Ê8\ÇA_9ûì³Ÿ\\ºtišÅ»\ê‚.x<·¤\çV44 Ue?„şb•\ĞtJ\ç8\è=\ô\Ğ\ËN8\á„Ê¦M›\Ò\ŞU\ñù‡‡‡ŸZ¸p\á3\Òs,\ZĞª2B±J\è:¥Œs\ôo}\ë[+,Xf\ğx\ï{\ßû\Ä\Ğ\Ğ\Ğ\ñ\é9‰\rhU\Ç¡¿X%\ôR\Æ9úÆ¹ç»kÉ’%iş\î‰\ó\Ï?g0V§\çX$4 Ue?„şb•\ĞtJ\ç8\è.|jÍš5iş\î‰U«V\í¼?=\Ç\"1 ­*\ãø!\ô«„~ S\Ê8\ÇA\ß8\õ\ÔS+k×®M\ówO\Üz\ë­O„\ã\á\ô‹Ä€´ªŒ\ã‡\Ğ_¬úN)\ã}#~ˆ\ß<\æ\ïØºu\ë\î0`<™c‘Ğ€V•qüú‹UB?\Ğ)eœ\ã oŒŒŒTx\â‰4\÷D8ú=`ıü€\â*\ãø!\ô«„~ S\Ê8\ÇA\ß8şø\ãs¥\ó\æ\Í\ñ\åı®\ôY*\ãø!\ô«„~ S\Ê8\ÇA\ßøĞ‡>´»(\ï\é¿\å–[\ñ~ We?„şb•\ĞtJ\ç8\èû\ØÇ,Ê§\÷_~ù\åø\ô~ We?„şb•\ĞtJ\ç8\èÿø\Ç\ï_²dIš¿{\âŸøÄº0`¬NÏ±Hh@«\Ê8~ı\Å*¡\è”2\Îq\Ğ7,X\ğ\ÑPiş\î‰\á\á\áCCCÇ§\çX$4 U9\áß²\"ÔŒ\ôş”\Ğ_¬úN\Éiƒ\ì,\\¸\ğ\Ç{leÓ¦Mi\ïªu\ë\Ömÿ©x>\é9‰\rhUN\ãGü·Œ\×^Ã¿\Ğ_¬úN\Éiƒ,z\ê©[–.]š\æ\ğ®ú\Ä\'>qW,nIÏ­hh@«r\Z?&„ş½†¡¿X%\ô’\ÓY\Z\Z\Z:d\îÜ¹•\íÛ·§Y¼+x\à‡\ãUşx\é¹\rhUN\ãG\Ğ_7üı\Å*¡è”œ\æ8\ÈÖ¼y\ó\î½\à‚\Ò<\Ş\ó\çÏ¿\'7¤\çTD4 U9u\Â~Zc\á_\è/V	ı@§\ä4\ÇA¶<\òÈ—Ï™3§²v\í\Ú4“w\Ôu\×]wW¼\ÊŸ?=§\"2 ­ªŒ³/¡¿X•¶\êm¥c\ô3}\Zú\Ä\È\ÈÈ§?şøÊ¶m\Û\Òl\Ş›7o~0O„\Ğÿ\É\ô\\ŠÊ€°\×ÿÀX1\Ë\Ëû[®\ô‡\õ¹Ñ§¡Ì;\÷{\ñOø\Å\ÅA\'\í\nfÏıp .MÏ¡\Èh\0uCÿO…ıª~ı\ßı\îw+¯x\Å+*\ÏzÖ³*¿\ó;¿SY³fÍ¤}ú­„şâ° 7ú4\ô™w¿û\İwœt\ÒI»;u\Å?^\áü·¦\Ï]t4€Ÿ\nıu\Ã~U?‡ş×¼\æ5•g>\ó™Õ \\ù\í\ßş\íIû\ô[	ı\Åa=An\ôi\èC!”/›;w\î\î\é~|œ\ÕgWø«h\0cc\á^\Ã~U?‡şj\İu\×]c¡ÿ\àƒ´­\ßJ\è/\ë	r£OCŸŠ\ïµ\á\÷y\ç\÷\ä;\Òü>%Û¶m{8~JÿøŸ\æ\ë›\÷\ğ§h\0\Í\Ë!\ô\ìcı\ï|\ç;\'m\ë·ú‹\Ãz‚\Ü\è\Ó\Ğ\Ç-ü\ß022²û_ø\Â\ã›7oN\óü^­_¿~ûgœ1\ö	ı\ñ8\ñx\és\ô\Z@\óú=\ôÿ\ë¿şkeÿı\÷¯¼\à/¨\Ü~ûí“¶\÷[	ı\Åa=An\ôi\È@í‡„_\æ[bx\ï{\ßû\Äy\ç\÷\èªU«v\Şz\ë­OnÙ²ew\È\÷»7m\Ú\ô\Ä\Í7\ßüHXTlı\ä\'?¹~xxxgxLû\ñq‡¤\Ç\ìG4€\æ\õs\è¿\ò\Ê++p@\å·~\ë·\Æşœmº½K\è/\ë	r£OC^\öŸ9s\æ	\á{\õ\à\à\àı\á6~ _|~ü@§xû\ğøı«\ã~qÿ\ô\0ıÌ€Ğ¼~ıo{\Û\Û*oû\Û+>ø\à¤mıZBqXO}\ZÈ†\r yıúŸÿü\ç\Ç\ñ¾V§vÚ¤}ú­„şâ° 7ú4\r\Z@\óú9\ô\çXBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ	ı\Å*¡¿8¬\'È>\rdÃ€\Ğ<¡¿X%\ô‡\õ¹Ñ§l\Ğ\0š\'\ô«„şâ° 7ú4\r\Z@\ó„şb•\Ğ_\Ö\äFŸ²a@h\Ğ_¬ú‹\Ãz‚\Ü\è\Ó@6h\0\Íú‹UBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ	ı\Å*¡¿8¬\'È>\rdÃ€\Ğ<¡¿X%\ô‡\õ¹Ñ§l\Ğ\0š\'\ô«„şâ° 7ú4\r\Z@\ó„şb•\Ğ_\Ö\äFŸ²a@h\Ğ_¬ú‹\Ãz‚\Ü\è\Ó@6h\0\Íú‹UBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ	ı\Å*¡¿8¬\'È>\rdÃ€\Ğ<¡¿X%\ô‡\õ¹Ñ§l\Ğ\0š\'\ô«„şâ° 7ú4\r\Z@\ó„şb•\Ğ_\Ö\äFŸ²a@h\Ğ_¬ú‹\Ãz‚\Ü\è\Ó@6h\0\Íú‹UBqXO}\ZÈ†\r yB±J\è/\ë	r£O\Ù0 4O\è/V	ı\Åa=An\ôi 4€\æ-_¾|\÷Î;\'…O\Õı\n\í°>„ş]i\Ñ\Ö\äFŸ²a@hŞªU«\ÖoÜ¸qR\0Uİ¯»\ï¾û+!\ô_“¶½a=An\ôi 4€æ±r\åÊ‡6lØ°\Åÿ\ŞTø¹o¸ë®».\nÿP‡¦mDoXO}\ZÈ†\r`jbĞŒW˜C=\ßS®º^\ñ\çşXO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§l\Ğ\0€vYO}\ZÈ†\r\0h—\õ¹Ñ§š8 \ô{¥ÿ&\0€©° 7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@\0(;\ë!r£O5\0 ì¬‡È>\r\Ô\0€²³\"7ú4Pc@\0\0\Ê\Îzˆ\Ü\è\Ó@ ˜*•\Êk×®ı\Ú\êÕ«_¾|yeÙ²eª\Ë~\î»W­Zµ~tt\ôˆ´}È‹\õ¹Ñ§\Z@1…À¿$\Î\ÊÆ+;w\î¬<\ö\Øcª\Ë\î\ñ\ç¿r\åÊ‡–-[vh\ÚF\ä\Ãzˆ\Ü\è\Ó@ ˜\âş8\Ó ªº_6l\ØBÿ5i‘\ë!r£O5€bŠ/\éw…¿\Û!„ş]i‘\ë!r£O5€bŠ\ï)OÃ§\ê]\Å\öHÛˆ|X‘}\Z¨1 \0S³¡ÿ\á7T\Ö^ûù\ÊÍ—Ÿ9V\ñ\ëx_ºŸj¯„ş¼Y‘}\Z¨1 \0S3¡Ç¶u•›.=½\ò\Ão\òS\ï‹\Û\ÒıU\ë%\ô\ç\Ízˆ\Ü\è\Ó@ ˜š	ı\÷\Ş|\ñ¤À_­ûn¾d\Òşª\õú\óf=Dn\ôi Æ€\0PLÍ„ş¯8kRØ¯VÜ–\î¯Z/¡?o\ÖC\äFŸj\0\Å\ÔL\è¿i\Ù\ÂIa¿Zq[º¿j½„ş¼Y‘}\Z¨1 \äoıú\õ¿t\â‰\'^ÿ\ÊW¾\ò\ñ8 ¶·\êRı\Ì\Ïü\Ì\î_ù•_y\àE/z\Ñ\'\Ã\÷O\ß¦@\è/V	ıy³\"7ú4Pc@\È[üox\Ã×»\ŞU¹\ö\Úk\Ç®tOüyÇŸû\Û\ßş\ö\Ç<\ğÀ\ì\'ø3Í„şøiıiØ¯VÜ–\î¯Z/¡?o\ÖC\äFŸjyû\à?xı¡‡šfQz\àu¯{İº\Ğ$§¦m4úo¿\ò\ÜIa¿Zq[º¿j½„ş¼Y‘}\Z¨1 \ä\íÕ¯~\õ\ã\ñJ3½\÷ƒü`Sh’k\Ó6‚Fš	ı[\×\İX¹é²O\nü\ñ¾¸-\İ_µ^BŞ¬‡È>\r\Ô\ò\ö\Ìg>sl±J\ï\ÅvM²+m#h¤™\Ğ\ë\Î\ë¾8)\ô\Çû\ÒıT{%\ô\ç\Ízˆ\Ü\è\ÓPR\á—E\0\öQ+\Ò\Ç\Ñ\×\Ò\ìI\Å\öH\Zi*\ô\ï\ÚU¹ı\çM\nı\ñ¾¸m\Òşª\åú\ó& ‘}\ZJ*ü\òÏ¨\òÓš‘>¾–\æNz(¶G\Ú@\ĞÈ¾Bÿm\ë*·]¹xR\à¯V\Ü\÷I§Z+¡?o¹Ñ§¡\Ä\â•ü:A\ßUş|¥¹³®¸_ZL¿\ñŸ-4¥a\èßµ«²\á\ö\ÑÊ¾û\ÑIA?­¸O\Ü\×Uÿ\öK\èÏ›€Dn\ôi(±x%¿N\Øw•?_i\îÜ«Å‹\Ó\Ã;,\İ\Ä4ˆ?Û´ ‘z¡_W\÷•«ş\í—ĞŸ7‰\Ü\è\ÓPr\ñŠ~À¿\"İ,¤¹³¡İ»wW^ò’—Œ\Óÿ\÷O73\r\â\Ï6m h¤^\èo\æ\ê~£ŠM§š/¡?o¹Ñ§¡\ä\âı:¡FºYHsgCW]u\ÕX(ı\õ_ÿ\õt\Ó$ş|\Ó‚F\ê…ş4\ÈOµ\Ò\ã©\æK\èÏ›€Dn\ôi ½\Ú\ï*¾\Ò\Ü\Ù\Ğé§Ÿ>JO>ù\ät\Ó$ş|\Ó‚F\ê…~Õ»ú\ó& ‘}\ZH¯\ö\ÏH·“4w6\ô¶·½m,”^v\Ùe\é&¦Iüù¦\rı\Å*¡?o¹Ñ§1Õ«ı\éıd%Í\r½ø\Å/¥\ëÖ­K71M\â\Ï7m hD\è/V	ıy³\"7ú40¦zµ?½Ÿ¬¤¹³¡<p,”>şø\ã\é&¦Iüù¦\rı\Å*¡?o\ÖC\äFŸªI74w\ÒC±=\Ò‚F„şb•ĞŸ7‰\Ü\è\Ó@ƒş\ÆP\'\ß\nşyJs\'=\Û#m hD\è/V	ıyÈ>\rT5\è§ß“4w\ÒC±=\Ò‚F„şb•ĞŸ7‰\Ü\è\ÓPn~£û\éoiî¤‡b{¤\rı\Å*¡?o¹Ñ§¡¼\ö\ì\÷µş“\æNz(¶G\Ú@Ğˆ\Ğ_¬ú\ó& ‘}\ZÊ©\Ù@\ß\ì~\ô‡4w\ÒC±=\Ò‚F„şb•ĞŸ7‰\Ü\è\ÓP>S\r\òSİŸ\âJs\'=\Û#m hD\è/V	ıyÈ>\r\å\Òj€o\õqKš;\é¡\ØiA#B±J\è\ÏGC+b \ÚG­HıD\è‡\òh7¸·ûxz/Í\ôPl´ ¡¿X%\ô\ç#„¡uB~Z3\Ò\ÇA?ú¡¦+°O\×q\è4w\ÒC±=\Ò‚F„şb•ĞŸ—x%¿N\Ğw•Ÿlı¿\é\ê\Ó}<º\'Í\ôPl´(Ÿ\ñ°1#½?%\ô«„ş¼\Ä\ßÁ:a\ßU~²!\ôC\Ş:\Ğ;u\\:+Í\ôPl´(Ÿ‰W\÷.„şb•ĞŸŸ\ñ\ßÁ4\ğ¯H\÷ƒ~$\ôC¾:\Ì;}|¦_š;\é¡\ØiQ>\õBF½\ğ/\ô«„şü\Äß»:¿3\Òı 	ı§‰üi¡\Ş=~Ûz\ÇüûKš;\é¡\ØiQ>uBF\İ\ğ/\ô«„ş<ÿ\Ş\Õ~\Ó\íĞ¯„~\ÈO\Zø?·ßpo[\rş{;\à\ß?\Ò\ÜI\Å\öHˆ\ò©\ö\Ó\ZÿB±J\è\ÏSü]›\ğ»7#\İıJè‡¼¤ü\İû\í	\ÕJ{3&şj\óS{L~^Š)Í\ôPl´h*—¶\Ò\à©zWB¾f_\íO\ï‡~¦OC>\ê\ïz}*Á¿\Ş\ãÿyüşT½\ç§X\Ò\ÜI\Å\öHˆ\ò-L\Ò`?¡b\ğ˜Q\İÏ•şb•ĞŸ‡9s\æü|ø=;jpppQ¸]\ê\Î\ğ\õ®\ñ\ßÁG\ã\÷\ñş\ñ\íG\Åı\Óc@?ˆ}:½\è?{\Ü\õ‚{3Á¿\Ş\ã\Zşª½½—\æNz(¶G\Ú@”oa’ıIa¿ªŸCÿw\Ş9q«tŸ~+¡¿¿\r\r\r½%ü-\r\õX\ßÁ½U\Üi||zL(²\Ø\Óû€ş\ÒLĞ®\à\÷ü\ë\í¿¯À_\Õ\Ìù\Ğiî¤‡b{¤\rDù&\ÂDİ°_\ÕÏ¡ÿ«_ı\êX?\ò\È#\'m\ë\×úûS\ë¿~Ï®ª\æ[©«\â\ñ\Ò\ç€\"Š}6½\èS	\Ø\õ‚|½\à_o¿f\ÕTÎ‹\îIs\'=\Û#m Ê·0™µ°_\ÕÏ¡ÿ¤“N\Z\ë\ïŸş\ô§\'m\ë\×úûËœ9s588ø\áwmw\Z\Ş\çÍ›Wù\ò—¿\\Y³fM\å;\î¨lİºulŒ·\ñûx\Ü\÷KŸ>\'I\Ù\æV\ÈI+Áº^ Ÿü\ëmŸj\à¯j\åühA³¡a?¡¿Pb{¤\r„…I#ıúÿ\àş`¬¿?\ïyÏ«¼\à/¨\\~ù\å“\öé·º\ì²\Ë\ô\Ó>Æ”\r\r\rıpbX®œşù•û\î»/š\÷*\îŸ„ÿ\â\ó¤\Ï\rEan…ş\ÔN ®\ì\ã\÷û×¹¿\ÕÀ_\Õ\ÎyÒ¤	‹}…ÿtıB\Å\öH“Fú9\ô¿úÕ¯®œs\Î9•+®¸b¬ß¿\â¯˜´O?\ÕÎ;+G}ts\ï	\õıP…ú\ë,ÿlpp\ğ]\á\ö•>\ğ­B[¼<Ôº‰ı¬³Îª\Ü{\ï½\é<%\ñ\ñ\ñ8\ê\Şø|\é9@˜[¡ÿLG®üoM¾o7\ğWM\Çù²É¢co\á?]·\ĞC±=\Ò\ÂÂ¤‘~ı\Õz\ä‘G\Æúı³Ÿı\ìI\Ûú­.½\ô\Ò\ÊÀÀÀ‹C\È{Cú³\Ã\íGB\ß=/|ı­pû£P…û¶Ç¯\Ç\ï;/\î\÷\r_¿qxxø—fÌ˜ñŒ´™>³\ö\\\á¯şxuş\â‹/N‡\â¶\Ä\ã%Wı\ïÏ›\ôš¹ú\Ët\èzÁº\Õt7‰	‹´\Ò\ğŸ®W\è¡\ØÚ†q&\õ\õs\èı\ë__9\ğÀ+\ßø\Æ7\Æúış\áNÚ§ßª™\÷\ôü\Ü\ìÙ³3\ô\éC\ã«\0\Â\í_\Ï\Ú\óª€ø\ê€ø*øI\ğ^-\Ğ\ñ=\ö_\Òÿ\÷¼§rı\õ×§\Ã\ğ´ˆÇÇ¯>W¨¼ÇŸ¢1·Bÿ\èDp/\éO¯\ğ\Ç\ï\ãıÓ­\ç\Ï~{\rıiøO\×*\ôPl¤)\Ù\ÏÂ¤‘~ıË—/¯ü\ÆoüFå ƒª¼\éMo\Zû~\é>ıVÍ„ş}‰Wú½Z 3\Æ?´olŒW\â;ø«\â\ñ\'^\ñÏŸ\ô’¹úÃ³\÷\Û˜ON7´aoWú\ë}ªÿt8yBU]®ı„şBI\ÛGıg¥ıús¬\éı\Í\ğj©\Ú\ógùª\'—\\rI:üvD|	\ã\ØS\ñ<\Òsƒ^‰ı2½(¦\é¼R^/\ğ§Wü§;øO\çù3Á„EFZ+fyya\Å\ö˜\Ğ6°WB±ª[¡_\Ê\òj\ğo|zz_#\á\ßuUuŒ¶\×MÉ‡û]•\ôJ\ì“\é}@qMGp®ø\ã{ø\ë}zÿtÿ\é8o\Z˜°Àh\ö«\Ò\õ	=\Û#m hD\è/V%\ô7#‡WŒŸ\÷\â\ğoù\Ùt\ÛD3g\Îü£\ê\\_n\ß\î§\ôOU|¾‰/\óç“#\ôB\ì\é}@±µ şj°¯·½\İ\à\ß\ÎùÒ„\ê\âbV\ã°_•®O\è¡\ØiA#B±ªŸBÿ¾\ôÃ«\Âs5>\Ïİ¹· ¶/­Î‰\çŸ~:\ìvE|\Ş	\ó\ò\Ò\ô¡bL\ïŠ¯• ]/\Ğ\×û”şzûµ\Zü[9O¦hÖ¾\Ã~Uº6¡‡b{¤\rıÅªœB3zıjpŒ¤\ã\åıS<§‰û?wü<\Æ\ö\é\öUşªø¼\Î\õ±x^\Ïz!\ö\Ç\ô> ?L%P\×\ò\õU½ı§\Zü§r~tGº6¡‡b{¤\rıÅª²…ş}\é\ô«\Â>WN\ÒÕº7ş§Â„}ªn›7o^:\ävU|ş	\çy\Ô\Ä\ËDaÛŒP+\ÒûaºÅ¾˜\Ş\ôf‚u½\0¿·À_U\ïq\ÍÿfÎ‹\îK\×%\ôPl´ ¡¿X%\ôO];¯_\ß? \'U\Ø\çK\á\Ø\Ï_/®\Ş\÷¥/})r»*>ÿ„s\\œş,f‡ı\ê>\év˜nú\ô¿½\ìzÁ½™À_U\ï\ñû\nş{;z+]—\ĞC±=\Ò‚F„şb•\Ğ?ı\ö\òjo‡\Û\İƒ~z \ìk\õûë®».r»*>\õ\\\ÂyVÿ³’°_­‰?\èı\ò\Ğ(h¿{¿\ÖU½\à\ÌO\í\ñŸ\ZÅ®K\è¡\ØiA#B±J\è\ïV~9\r\ÉI=\Z\ê¾POU\ï[»vm:\ävU|ş	\ç_½P7\ìW+ı7\Ãt\Ó\Ï \õ\÷\ÄÀ\ŞJ\à¯j\æ8\õŸbI\×%\ôPl´ ¡¿X%\ôw\ÏÌ™3ÿ \r\É\ãŸ\ğùPo^¸paü“\Ã\ñ\Ãş\â}c\Ûx\àt\È\íªøü\Î\÷\É\ôü\ÓJÿ\Í0\İ\ô3\ÈK½\àz¼2_/¨O\ÅŞS\ïy)t]B\Å\öH\Zú‹UB\÷„0\ôx8~\"~`ø~h``\àÀt¿‰\áú‰\'H‡Ü®ŠÏŸ{¥z]\é\ï\ĞßºÀ»ı|´.]—\ĞC±=\Ò‚F„şb•\Ğ\ß=!\èÿI\Èù>22\ò‹é¶‰Šz¥ü¼¼¼€i×­ Ş­\çaz¤\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹gÖ\÷Î…è¢½§\Â9\Ö\rÿÿ\00\ä>>\Ó/]—\ĞC±=\Ò‚F„şb•\Ğ_<\ñS\ò«!º¨Ÿ\Ş_•†ÿt;\0LE§‚y§Kg¥\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹\'„\ç\Å\Õı¥/})r»*>ÿ„+ù‹\Ós­ª†ÿ\ô~\0˜ª\é\è\Ó}<º\']—\ĞC±=\Ò‚F„şb•\Ğ_<!<U\r\Ú\ó\æ\ÍK‡Ü®:\ñ\ÄwOıG¥\ç\n\00]A}ºCo¤\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹gxxø¹!`?V\r\Û\÷\Ş{o:\ìvE|\Ş	ÿ±x^\é¹@§´\Ø\Û}<½—®M\è¡\ØiA#B±J\è/¦²—V\÷ùçŸŸ»]ŸwB\è_š#\0tZ«Á½\Õ\ÇQ,\éÚ„Š\í‘64\"\ô«„şbš9s\æU\÷\ğ\ğp×¯\ö\Ç\ç\Z\Zª½´?Oz\0\Ğ\rS\r\ğSİŸ\âJ\×\'\ôPl´ ¡¿X%\ôW\ÛWUC\÷Yg•½Ÿo\ÂUş«\Òs€nj6\È7»ı!]Ÿ\ĞC±=\Ò‚F„şb•\Ğ_\\CCC¿\÷S\Õ\ğ}\É%—¤\ÃoG\Ä\ç™øŸŠç‘\0tÛ¾ı¾¶\Ó\Ò5\n=\Û#m hD\è/V	ı\Å688ø_\æı\õ×§C\ğ´ŠÇŸø²şøü\é9@¯4\n\öî§¿¥\ëz(¶G\Ú@Ğˆ\Ğ_¬ú‹mÎœ9\Ï\n\áû†j\Ï{\ŞÓ±\à¯\öÊ‚ø¼\ñù\Ós€^J~ú=ùH\×*\ôPl´ ¡¿X%\ô_\ß/\nu\ï\Ä+ş_|q:·%o\âş\ñ\ç{Qz.\0PÕ \òø­ÀŸ§t½B\Å\öH\Zú‹UBü\åƒ¬øa{\í~ª||\ò¡}c?>_z\0P$‡\ÄI+Ş¦\ÈFºn¡‡b{¤\rı\Å*¡¿\Ä+\ï³&¼\Ô?V¼\êşù\çW\î»\ï¾thŞ«¸|\\ru\ì%ı\ñy\Ò\ç€\Âı\ä+]¿\ĞC±=\Ò‚F„şb•\Ğ\ß_\â{\ì\Ç?\Ü/\r\ë•y\ó\æU¾ü\å/WÖ¬YS¹\ã;*[·n£\ãmü>\Ş·Ÿxâ‰“\ë=ü\0\ô\r¡?{Iì¤—b{¤\r,_¾|\÷Î;\'…O\Õı\n\í°>„ş]iQ|\ã\Î\ïª:á½•ºÊŸ\å \ï\ÄI,½¬¤¹“Š\í‘64²jÕª\õ7nœ@U\÷\ë\î»\ïşJı×¤mDÿaı-aÍ³4\Ôcu\Âü\Ş*\î¿4>>=&\0\ô¡?{iî¤‡b{¤\rŒ±r\åÊ‡6lØ°\Åÿ\ŞTø¹o¸ë®».\nÿP‡¦mDÿ™3g\ÎÏ‡µ\ÏQƒƒƒ‹\Â\í\òPw†ªşù½GÇ¿_>¾ı¨¸z\0\è+B\ö\Ò\ÜI\Å\öH\ö&\Íx…9\Ôc\ñ=\åª\ë\î\ñ\ç/\ğg*¬ƒfL¸ª?#\İ\0}O\è\Ï^š;\é¡\Øi\Ğ;a´bB\è_‘n€¾\'\ôg/Í\ôPl´\0\è\ä*¿«ı\0\äI\è\Ï^š;\é¡\Øi\Ğ\ÉU~WûÈ“ĞŸ½4w\ÒC±=\Ò û\Z\\\åwµ€üı\ÙKs\'=\Û#m \0º¯ÁU~WûÈĞŸ·8`wü³S\ôŞ®]»Ö‡&Ù•¶\0\İ¯\ä\×	úi\ÍH\0}I\è\Ï\Û\Ë^\ö²­\×^{mš?\é\ï|\ç;\ß\rMrm\ÚF\0tW¼’_\'\ä§\åj?\0yú\ó\ö¢½\è/\ß\ö¶·=Pºn\ëAtyh’S\Ó6 \÷¬‡\0È–I.{O?\ğÀ\ğº×½\î¾Õ«Wo\öRÿ\îÚµk×†x…?şe¡-\âU£§§\r@\ïY-“\\)Ä yJ¨5¡\ÛoÏ‡É©\îTüyÇŸ{üùü\0e=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0\Ù2\É\0eg=@¶Lr\0@\ÙY-“\0Pv\ÖC\0d\Ë$\0”\õ\0YÚŠ8©\í£V¤\0È™\Ğ@Â„6£N\ÈOkFú8\0€œ	ı\0d#^É¯\ô]\å\0JK\è \ñJ~°\ï*?\0PZB?\0Y‰W\ô\ë~Wù€Rú\ÈJƒ«ı3\Òı\0\0\Ê@\è ;\É\Õ~Wù€\Òú\ÈNrµFº\0 ,„~\0²T½ÚŸ\Ş\0P&\ÖC\0d©zµ?½\0 L¬‡\0˜¤R©°v\íÚ¯­^½ú\ñ\åË—W–-[¦º\\\á\ç¾{ÕªU\ëGGGH\Û\0 YB?\0“„À¿$\Î\ÊÆ+;w\î¬<\ö\Øcª\Ë\î\ñ\ç¿r\åÊ‡–-[vh\ÚF\0\0\Íú˜$^á3\r¢ªûµaÃ†-!\ô_“¶\0@3„~\0&‰/\éw…¿\Û!„ş]i\04C\è`’ø\ò4|ª\ŞUl´\0\0š!\ô0I³¡ÿ\á7T\Ö^ûù\ÊÍ—Ÿ9V\ñ\ëx_ºŸj¯„~\0 UB?\0“4úwl[W¹\é\Ò\Ó+?ü\Ö)?U\ñ¾¸-\İ_µ^B?\0\Ğ*¡€Iš	ı\÷\Ş|\ñ¤À_­ûn¾d\Òşª\õú€V	ı\0L\ÒL\èÿ\ñŠ³&…ıj\Åm\éşª\õú€V	ı\0L\ÒL\è¿i\Ù\ÂIa¿Zq[º¿j½„~\0 UB?\0“ı\Å*¡\0h•\ĞÀ$Í„şøiıiØ¯VÜ–\î¯Z/¡\0h•\ĞÀ$Í„şÛ¯<wRØ¯VÜ–\î¯Z/¡\0ø\íÜ±J¤W\Ç\áF,±,¬­ll¼)¼½‹m\ö\"R¤O\ÒØˆE(ƒ! d·\Ê\ÑÀ\î &„˜\Â\Ù&_\æ¤ù^e\'Çƒû~\É\óÀŸ™3\Îú\ãSk‰~\0‚y¢ÿ··?v¯¿y‚¿\ÜWëŸ·ú‰~\0 –\è ˜\'ú\Ë~ş\á‹ı\å¾ş9{\ÚD?\0PK\ô\Ìı“I\÷\æ»\ÏC\ô—û\Êc\á¼UO\ô\0µD?\0Á‡¢ÿ\Ï\ß\ßv?}ûYşV+gúÏ³º‰~\0 –\è x4ú\'“nü\æ¼{\õ\õ‹úı•3å¬«şOŸ\è\0j‰~\0‚‡¢ÿCW\÷›«şOŸ\è\0j‰~\0‚‡¢«û­<·ÿz6ÿD?\0PK\ô<ııÿ·ë¿\Í?\Ñ\0\Ôı\0E¿}¼‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸk¢\0¨%úD®‰~\0 –\è ı¹&ú€Z¢€@\ô\çš\è\0j‰~\0ÑŸkå›µ™™™Y\íú?\ë\ğ?\'úsÍ•~\0\0\0šr\ô\ß\İ\İuûûû\İ\ê\êj·±±Ñœœ„3C›\è\0\0 ™!Gÿ\á\áa	\ä\î\ô\ô´[^^\îvwwÃ™¡M\ô\0\0\ĞÌ£ss³[[[\÷y¢\0\0€f†ı‹‹‹³\è\ß\Ú\Ú\ê\Ö\××»\Ñh\Îm¢\0\0€f†ı³_\ï?>>İ–ø\ïŸ\ÚD?\0\0\0\Í9ú\Ë\Õı\é—0û‡~\åvii)œ\ÚD?\0\0\0\Í9úf±tt4»\İ\Ù\Ù	g†6\Ñ\0\0@3Cş\Û\Û\Ûnoo¯[YYé¶··»\Ë\Ë\Ëpfhı\0\0\043\ä\èÿ/N\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íˆş\\ı\0\0\04#úsM\ô\0\0ĞŒ\è\Ï5\Ñ\0\0@3¢?\×D?\0\0\0Íœıu\âÓ\Ó\Ï\á\İ4ú\'ı\Ï\0\0\0ª\\\\\\¼»¹¹	jÏ¿\ë\ëë¯¦\Ñÿ}ÿ3\0\0€*\ç\ççŸF£?\Æ\ã\ñ¯®øœM\ß\÷\ñ\Õ\ÕÕ—\Ó\àÿeºOúŸ\0\0\0T+¡Y®0O\÷¾üM¹=û\Êû^\ŞÁ\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\004&\Ì\Ç\ï\ò\÷B\Ñ\0\0\0\0IEND®B`‚',1),('15002',1,'flow_mu180u86md3e.bpmn20.xml','15001',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" typeLanguage=\"http://www.w3.org/2001/XMLSchema\" expressionLanguage=\"http://www.w3.org/1999/XPath\" targetNamespace=\"http://bpmn.io/schema/bpmn\" id=\"Definitions_1\">\n  <process id=\"flow_mu180u86md3e\" name=\"æµ‹è¯•914\" isExecutable=\"true\">\n    <extensionElements>\n      <flowable:executionListener event=\"start\"></flowable:executionListener>\n    </extensionElements>\n    <startEvent id=\"StartEvent_mu2c2p840\" name=\"å¼€å§‹\"></startEvent>\n    <sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\"></sequenceFlow>\n    <endEvent id=\"EndEvent_mu2c2p8c0\" name=\"ç»“æŸ\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </endEvent>\n    <sequenceFlow id=\"Flow_1qxvbp7\" name=\"xiaoyuyibai\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_15cdzo6\">\n      <extensionElements>\n        <camunda:properties>\n          <camunda:property></camunda:property>\n        </camunda:properties>\n      </extensionElements>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_1dpq6dk\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_0qm5tc6\"></sequenceFlow>\n    <userTask id=\"Activity_0qm5tc6\" name=\"1\"></userTask>\n    <sequenceFlow id=\"Flow_0icqanz\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_1mzuxt6\"></sequenceFlow>\n    <parallelGateway id=\"Gateway_15cdzo6\"></parallelGateway>\n    <userTask id=\"Activity_1mzuxt6\" name=\"2\"></userTask>\n    <sequenceFlow id=\"Flow_1aqie0c\" sourceRef=\"Activity_0qm5tc6\" targetRef=\"EndEvent_mu2c2p8c0\">\n      <conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[${tes &gt; 100}]]></conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"Flow_016it02\" sourceRef=\"Activity_1mzuxt6\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <exclusiveGateway id=\"Gateway_0a6n8eh\"></exclusiveGateway>\n    <sequenceFlow id=\"Flow_13gvmcu\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_02klewn\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_1kkbmin\" sourceRef=\"Activity_02klewn\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0t3saos\" sourceRef=\"Activity_0ylt45t\" targetRef=\"EndEvent_mu2c2p8c0\"></sequenceFlow>\n    <userTask id=\"Activity_02klewn\" name=\"6\"></userTask>\n    <userTask id=\"Activity_0ylt45t\" name=\"5\"></userTask>\n    <sequenceFlow id=\"Flow_0dizg8q\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_0a6n8eh\"></sequenceFlow>\n    <sequenceFlow id=\"Flow_0e4xs2a\" sourceRef=\"Gateway_0a6n8eh\" targetRef=\"Activity_0ylt45t\"></sequenceFlow>\n    <manualTask id=\"UserTask_mu2c2p880\" name=\"3\"></manualTask>\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_flow_mu180u86md3e\">\n    <bpmndi:BPMNPlane bpmnElement=\"flow_mu180u86md3e\" id=\"BPMNPlane_flow_mu180u86md3e\">\n      <bpmndi:BPMNShape bpmnElement=\"StartEvent_mu2c2p840\" id=\"BPMNShape_StartEvent_mu2c2p840\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"142.0\" y=\"142.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"149.0\" y=\"185.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"EndEvent_mu2c2p8c0\" id=\"BPMNShape_EndEvent_mu2c2p8c0\">\n        <omgdc:Bounds height=\"36.0\" width=\"36.0\" x=\"812.0\" y=\"382.0\"></omgdc:Bounds>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"23.0\" x=\"778.0\" y=\"393.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0qm5tc6\" id=\"BPMNShape_Activity_0qm5tc6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"780.0\" y=\"90.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_15cdzo6\" id=\"BPMNShape_Gateway_15cdzo6\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"545.0\" y=\"75.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_1mzuxt6\" id=\"BPMNShape_Activity_1mzuxt6\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"631.0\" y=\"170.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Gateway_0a6n8eh\" id=\"BPMNShape_Gateway_0a6n8eh\">\n        <omgdc:Bounds height=\"50.0\" width=\"50.0\" x=\"225.0\" y=\"365.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_02klewn\" id=\"BPMNShape_Activity_02klewn\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"200.0\" y=\"500.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"Activity_0ylt45t\" id=\"BPMNShape_Activity_0ylt45t\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"610.0\" y=\"340.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape bpmnElement=\"UserTask_mu2c2p880\" id=\"BPMNShape_UserTask_mu2c2p880\">\n        <omgdc:Bounds height=\"80.0\" width=\"100.0\" x=\"248.0\" y=\"102.0\"></omgdc:Bounds>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1g1p7by\" id=\"BPMNEdge_Flow_1g1p7by\">\n        <omgdi:waypoint x=\"178.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"160.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"210.0\" y=\"142.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"248.0\" y=\"142.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1qxvbp7\" id=\"BPMNEdge_Flow_1qxvbp7\">\n        <omgdi:waypoint x=\"348.0\" y=\"134.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"548.0\" y=\"103.0\"></omgdi:waypoint>\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds height=\"14.0\" width=\"54.0\" x=\"404.0\" y=\"130.0\"></omgdc:Bounds>\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1dpq6dk\" id=\"BPMNEdge_Flow_1dpq6dk\">\n        <omgdi:waypoint x=\"591.0\" y=\"96.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"720.0\" y=\"70.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"780.0\" y=\"103.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0icqanz\" id=\"BPMNEdge_Flow_0icqanz\">\n        <omgdi:waypoint x=\"570.0\" y=\"125.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"570.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"631.0\" y=\"210.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1aqie0c\" id=\"BPMNEdge_Flow_1aqie0c\">\n        <omgdi:waypoint x=\"880.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"130.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"1011.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"848.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_016it02\" id=\"BPMNEdge_Flow_016it02\">\n        <omgdi:waypoint x=\"731.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"210.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"382.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_13gvmcu\" id=\"BPMNEdge_Flow_13gvmcu\">\n        <omgdi:waypoint x=\"250.0\" y=\"415.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"500.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_1kkbmin\" id=\"BPMNEdge_Flow_1kkbmin\">\n        <omgdi:waypoint x=\"300.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"540.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"830.0\" y=\"418.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0t3saos\" id=\"BPMNEdge_Flow_0t3saos\">\n        <omgdi:waypoint x=\"710.0\" y=\"400.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"812.0\" y=\"400.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0dizg8q\" id=\"BPMNEdge_Flow_0dizg8q\">\n        <omgdi:waypoint x=\"298.0\" y=\"182.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"298.0\" y=\"274.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"274.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"250.0\" y=\"365.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge bpmnElement=\"Flow_0e4xs2a\" id=\"BPMNEdge_Flow_0e4xs2a\">\n        <omgdi:waypoint x=\"275.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"443.0\" y=\"390.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"443.0\" y=\"380.0\"></omgdi:waypoint>\n        <omgdi:waypoint x=\"610.0\" y=\"380.0\"></omgdi:waypoint>\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>',0),('15003',1,'flow_mu180u86md3e.flow_mu180u86md3e.png','15001',_binary '‰PNG\r\n\Z\n\0\0\0\rIHDR\0\0ı\0\0N\0\0\0\åg\0\0H\ôIDATx^\ì\İœœUa7~PJ*^ª}ß¿¾¶Ö¿½¼o½\ÔKm\õU‰\÷Za“\ì†\ĞX°ZI*­X\Úÿ\ÖVik½!*TjÁvƒˆ\"”‹ ®!	 \Âü\Ï\Ù\ìL—3“dvn{\æ<\ß\ï\ç\óû\Ìfg.\ì<{\Îù\ñ\Ì\Î\î¶\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\00<,X°oz\0\0\00dFFF\öœ?ş‹C7oŞ¿„\\¾~xtt\ôo•\0\0\0Gy\ä/„Rÿ\ÂP\ê9=\ä\òûC®ùd\Ø\ö\ö\ßù\õPúÿ-\\·.\äs\æ\Ì\Ù#½/\0\0\0`–Ä‚?66\ö»¡¼Šûi!ß?”ú«\Ã\å§b™ùıE‹\í•Ş¶.\Ü\öyaÿ\ó\Ã~×‡Ë·¤\Û\0\0€>‹g\âcA\åüOC9ÿX¸ü^\È}!×„\ë?\ò\ç!/y\\z\Ûv„Û¾.\Ü\ï\áş¾\ï\'\İ\0\0@\ì^A\ÙBql(\à\Ï	|Q\È\òK¦\nşC>¶3w\î\Üÿ»p\á\Â}\Ò\Ûvc\éÒ¥	\÷}XxŒ[C\Î\Ï\ã\Ù\é>\0\0\0\ô\Î~!\ë§.)P,Ú¡\\ÿŸ©²}j\È\Å![FGGrFøzI\È+\Â>¿”Ş¶_\â¯„\ç\ó\ğ¸w…\Ëe\áy<%\İ\0\0€\î\Ôÿ±S—Šÿ‹?\è\ß94”\é\n¥ú¢\ğ\õ\æpy}È™\á\ëw‡\ìw\à>>½\ílˆe\êD\Ä\òÿ}6\0\0\0\0\íK\Ï\ğ§ÿ&»‡¢ü›¡0…\òü\áo…¯\ï\rùI\ÈÃ¶\ã\æÎûª‘‘‘\'¤7\ÌM|›x\Îg‡\Ü\Zß‘ÿ\çEº\0\0\0\í\ÙQÁ\ß\Ñ\õÌ¾\İc1…8\ô\âù²2ä5¡\ìŸ.Oy\Í\Ø\ØØ“\Ò“ø\á¿\ã\Û!W\ÆşK·\0\0°s»*\ö»\Ú\Î\0„\òş¬Pzç†’J(À\ã\árS¸¼9\\ş{¸ş½±ŒŒ<9½])\Â\ë‡\\ş{\Ïÿ­\ÏK·\0\0Ğ¬\İB\ß\î~\ô@(·\Ï9$”Û¿\rY¾¾{ş\öO·?\'”\Ş\÷½!ş\î{z»\Ò\Å?!¾\ïYşûÿmdd\ä\é\é>\0\0\0l7\Ó\"?\ÓıiC(®¿\ZŠü[B‘ı\ëB6„\Ü\ò•\÷‡moZ¸pá¯¤·«²ø¡ƒSß¯ø½ú\ë\ì›\î\0\0PeøNoG\Ê\éÿ\Zı£PT?\ò;C\î\Åş\ÜpùıGFFşgz;Z‹gú\ãÿx\æ?\ä\ñ\0\é>\0\0\0U\Ómq\ï\ö\ö•J\è\ÓB¥\ô¤P\ê¿¾^\òÓ¯\Æ\ëBˆû¤·c\æ\â\ïø\Ç\ß\õ\ß\Ï\ë\ã»&\Ò\í\0\0\0U\ÑNa¯MË´s?•\ÏÎ‡²ù‡Sg\ëÏ›:\ó\Ï\â=\äƒa\ÛA\ñ,z;z+~˜aø^_¾\ç\ß_¿4\İ\0\0P²v‹z;¥?j\÷şŠ¿~ll\ìÍ¡X¾?\ä?CÖ†l˜:\Ó\Ïü\ã\ïé§·c0–.]ú˜\ğZ6ûÿ¬aº\0\0@ifR\Ğ\Û-ı\ÑL\îw\è\ÄO\Èycü\Äüÿ%ò¶!ßœúdıƒ\Ã\×\ÏLo\Ç\ì[´h\Ñ^\á5{Ox}\î\n—Ëªø\×\0\0€j˜i1ŸI\éfzÿY:\ì°\Ã~9”\Ã×‡rø\ŞP¿rKø\÷¦pya¸<%ddll\ìY\é\í\È[,û\á5<uªü¿\'şÏ€t\0\0€a\ÕI!Ÿi\é:yœY\Êû“\æÎû\ÚPOˆo¹)\äP\'\Â\åß‡Ì›z[ø\î\émN\ñ\õœz­ooÿ¿\î\0\00L:-â”ş¨\Ó\Ç\ë«P\ö\Êü«C\Ñ;.¾/…\ÜroÈªpı‡CF\Ç\Æ\Æ~c7¿\âü…\×ş\Û!W\ÆşK·\0\0ƒn\nx§¥?\ê\æq»¶`Á‚}C™›\ÊÜ»C¾¾¾!\\n—«C\éÿ§###¿µ›‚_y\á˜ø\ã\ë\â0†c\äy\é\ö^¨\Õj{®Y³\æ¬K.¹\äg\ã\ã\ãµ+VÈ€¾ï¬^½ú‰‰‰ƒ\Ò\×\0\0†U·Å»›\Òuûøm	\åı—\æÎû\ÊP\Úş\"”·\Ï\Ç²e\ê,nü\î…a\Ûÿ\ö6nvdÎœ9{„\ã\ä!\ëB\ñÿ·pL==İ§¡\ğŸ\ngmıú\õµ­[·\Ö|\ğAp\â\÷=~ÿW­Zu\ïŠ+\öO_#\0\06½(\Üİ–ş¨Ï£a\áÂ…û„R\ö\òÅ¡ }.\äG!\÷…|\'dy¸şOB~\'”¶Ç¦·…]9\ğÀ?ûŸ[\Ü/\ã;F\Ò}:\Ï\ğ\ÇÂ™Q|Ö­[·!”şK\Ó\×\0\0†I¯Šv/J\Ô\Ñ\ó9\à€\ö\Å\ëe!\ï\nEş3!\×Nü\ïÎ›7\ïc!o\r¥\ì¹\n>½\Ï\ô\Ç3ş\ñ\Ì|@|\'@ºO*\Üf\Ï\ôººø–~gø\óH|B\éß–¾F\0\00,:*\Ø;Ğ«\Ò\í\ôy…\Â\ô¸P\âÿ ä¡d}*\ä!\÷‡\\\ZrZ\È\áa\Û\ó\Û)_\Ğ+\ñwü\ã\ïú‡\ã\ïúpù–tûtaŸ¯\Æ_#I¯\âï”§\åSf/\ñ\õH_#\0\0;-\Ö\èe\é&Ÿ\ß^{\í\õÚ±±±—„B\õg¡(}2äª©‚Y\È\é!oyÁ‘Gù\éÀlˆŸ\î\nı\á¸üvøú¥\é\ö°\íMa[-Ã­\ÛvKÿ}\÷¬«­ùş§j\×^x\òd\â\×\ñºt?\é.J?\0\0Ã¨×…?\êu\é\ß-ş\×|\ğÁ„\Ä3§ÿ\n\ÔQccc/\Ú\Ù[£!\ñƒ \Ã1{X(\ö·†œ\Ùg×¯Ÿ¿ı)±\ôÇœœŞ¶Ò¿e\Ó\Ú\Ú5¼¿v\Õ×{T\âuq[º¿t¥\0€aÓ\Â\õ¼\ôG±ø\ïÖŸ\ç}·hÑ¢½BùO(\÷w…\Ëe!\ÇL+ü1…üş\ôÛ´Súo»\öÜ¦\Â_\Ï\í××´¿t¥\0€aÒ¯\Â\õ¥\ôO\é\ç\ó†¾}Jü@\ÉpùpRúc®‹DYß·\Òÿ£•§4•ız\â¶t\é<J?\0\0Yˆ\\722\ò„\ôúi\Ú)\ÎÓ‹ûleG\ö\Ûm\×\Ï²\Êı‰-\n=­\ï\×N\é¿f\ÅÒ¦²_OÜ–\î/G\é\0 £££\'…\â\ğ`\È!oÿş\Ó6Ç³ˆ±0;\íºV\Ò>Ù™øü\ãG\ã¬(ƒ…şJø™\ÜÜ¢\ì\×\óÈ¼y\ó^\÷Uú\óŠ\Ò\0@BiKJ\ÄÃ¡d|+d\ñ\Ø\ØØ¯\í\ÖŞ™\ò´€\ÏFvd¿\İvıü!K\á\ç\ñ\ã-Šş£~V×‡Ÿ\Õ\'µSú\ã§\õ§e¿¸-\İ_:\Ò@C­V\ÛsÍš5g]r\É%?Ÿœ$d°	\ß\÷GV¯^}\Ç\Ä\Ä\ÄA\é\ë¥›7o\Ş\Ó‘\äû¯{\İ\ëşu\ß}\÷İ°\Ç{¼:½}´S\Ü;¥\ğ3´FFFÿúDøüTL(\÷\áreLøú\Ç\á\ò\æ\r!„¬ù\ÊW¾\ÒT<\Ó\Üp\ñG›\Ê~=q[º¿t¸¾H_S\0**ş³Cá¬­_¿¾¶u\ëÖ¦ICúŸø}\ßÿU«V\İ&\éı\Ó\×J¶p\á\Â}¦JCZ\ö§\çg¡€l<\äC\éC\ñ\ïW\éWø©„<\ğ\ñ\ág\ô¯?ü\ğÚ–-[š\æ¸\éÙ¸\ö\ê\Ú5\ßü@S\á\×\Åm\éş\Òy”~\0\Z\âşX8\Ó\ÉBŸu\ë\Öm“\ô¥\ék%š3g\Îccc¿1::zÀ¼y\ó6µ(ú\õ\Ü4û\ïúÿvŸş^?J¿\ÂO\å|\ã\ßhš\×Z\å¦\Ë>\ÛTú\ãu\é~\Ò]”~\0\Z\â[ú\á\Ï#\ñu“\ô¶\ô5‚a¶hÑ¢\'†\Òşû¡´ÿI¸ü›sB~²5\ä\ÆPø¿.oiQ\ö¶½odd\ä±\É]\öºP\÷º\ô\÷úùÁPˆ%3×š²m[\í†\ïœ\ÖTú\ãuq[\Óş\Òq”~\0\ZÚš¤e`1I3Œ–.]ú˜P\Î=”\ô7…²¾$\\~\"\\®\nù\é\Ô\'?\äs\ó·ÿ\é¯C\Â\ö\ç¼\éMoú\Åú\í\ÃuË“\Â\Ã\Ø\Ø\ØK¦?F¢—Åº—¥¿—\Ï†Ê®\Ö[6­­]\ñ\ò¦\Â_O\Ü\÷Io\'\Åz€†]M\ÒÓ³m\ëıµ»nù~\í\'\ßı\ç\Ú-Wı{\Óv\é>&irŠı/…2ş¢P\Ú„bşÁP\è\Ï\n__¾~`ş\ö³\õ\ñ\Ï\î\Z®ÿ³W‡¯Ÿ–\ŞG+a¿wL+üŸŒ“\î\ÓB¯\nv¯J¯¥®\'¶m«­»a¢\öƒ\óÿ²©è§‰û\Ä}\õ\ï>\Ö\04\ìp’–Më¯¯\İr\åYµk.xcb¾î¢4\í\'\İ\Ç$Mv\Åû¡´¿.\ä\ÏC©ÿXø\÷…!·‡\Üş}E¸ş\á\ë„¯çı\î\Ğ\ÕßŸŸ;w\îk\Ãım\÷\÷–t\Û.\ô¢h\÷¢\ô\÷\âyÀPkµ\Ø\Õ\Ùı\ÅYÿ\îc=@C«I:\æ-›jkt~\í‡û\è\Éø\ë\Ç\×\Ö|ÿÓµû\î\õ\áıˆIšA™ús\\\Ïe{^,\ğ!g†üW,\ö!kCÆ§\nÿ»\Â\å\ë\Ãş¿\Zn¶{z?½ÿ\Æw¸ÿ§§×·©\Û\Â\İm\é\ï\ö\ñ¡­\Ö\íœ\İ\ßQ\âm\Óû“\öc=@C«Iúş\Íw\Ö~´\ò”¦	8&\í\Û\Òüx\Õ\ß\×\îº\åÒ¦û’™\Å$M¯…\Òş´¹s\ç¾*\÷·‡¯O\r—\ç\Ï\ßş\÷µ\ã[\òr\öü\ío\Õ?4\\¾8şù­\ô>†@7Å»›\Ò\ß\Í\ãBQZ­\'\Ò5\ÄL“ŞŸ´\ë	\0\ZZM\Ò?^ıá¦‰·ø=ÿ\îc’¦\ñC\ñ\â‡\ã…\â~pü\Äûù\Û?4\ïÒ{\çoÿ0½o…\Ë\ÓC–Œ½9\äY\ñ\Ã\÷\Òûr\ğNK§EjµÙ‹\õ\0\r­&\é´Ì·Êš\ïj\ò\ï\ê6rùµ;®[Q\Û\ô\Ó\ëš\îOÚIšY¸pá¯„\âşŠù\Ûÿnı‡\çmÿsw?™¿ı\Ï\ßı0üû?\Â\õÿ<^øú\âŸ\ËK\ï£p\ñNJ\'EkµÙ‹\õ\0\r­&\é´\à\Ï$×®8¹\éş¤ı˜¤9\ò\È#!”\ö\ß¥ı P\äOùT\ÈwB\î\Ùrq\È\'\Ã\ö\ã\Â\å\á\ò7\çÌ™³Gz?6\ÓB>\Ó\Ò?\Óû‡JhµÙ‹\õ\0\r­&\é´È·Jüd\İ/ı·G\å\æÿú|m\ã\íW6İŸ´“tuŒŒŒ<9”\ö—…\ÒşÖS\Â\×ÿ\Êş§\Î\Ú_r^\Èß‡®y\ÈS\Òû`‡fR\ÌgRúgr¿P)­\Ö2{±\0 ¡\Õ$}\õ7\Ş\ÛT\ò\Û\É\Õ\ßxOm\ó\Æ[š\îOÚIº,¡\Ø?6\äÙ¡\Ôÿa(\í\ï—ÿJüE!w†\Ü\ò\İpıgB\Ş¾ş\ã°\ïÿ	\Ù3½:\ÒnAo·\ô·{PI­\Ö2{±\0 ¡\Õ$}\Ó\åŸk*\ôi~8\ñ7MŸ\à\Ïşß¿eC\ÓıIû1I§\ì;66\ö’P\Ü†üuÈ—C‘¿vş\öO\È_\ò\õPøÿ)\\wT\È~¡\Øÿ\Ï\ô>\è‹vŠz;¥¿ûJkµÙ‹\õ\0\r­&\é\Ío<kŸıV¥?ş©¾k¯jº\é,&\é|\ÅO»\åı™¡´¿1øc\Â×§…¬¹#dK\Èe!Ÿy\Ø>\ö{Ş¢E‹\öJï‡ë¶°w{{¨„V\ë	™½XO\0Ğ°£Iz\ãÚ«k\×^øÁ¦²Ÿ&ş*À¦Ÿş¨\é\ö\ÒYLÒ³o\áÂ…û„\Òş\ÂP\Ş\ÇBq?)\\~)äªûCn\rùf\È\òw„¼fÁ‚ÿ+½²\Óiq\ï\ôvP9;ZO\È\ì\Äz€“t^1IN,\ës\ç\Î}m(ø\ïŒ%>ü\á\ò¶©re\Èc\é\r_¿ şÏ€\ô>*3-\ğ3\İ*\Íz\"¯XO\0\Ğ`’\Î+&\é\Ş\Zy\\|›}\È\Üø¶ûù\Û\ß~ùü\íoÇ¿#\\?.?ß®?66\ö†_7\Û=½Š\Ñn‘ow?`Š\õD^±\0 Á$WLÒ‰Œ\Êûœ£\ã\æ…\Ëo„\Ü4û\é]®û\÷ù\Û?`oaøú\÷\â\ï¥\÷Ae\ìª\Ğ\ïj;Ğ‚\õD^±\0 Á$WL\Ò;ÿ”]ü“v¡´¿%”\÷¿Œ\ê.\\~oş\ö?}ÿ\Ş\ê×¿;\\\î\öû\ãŸ\ÌK\ïv\Ûq±\ß\Ñ\õÀ.XO\ä\ë	\0\ZL\Òy\Å$=Y\îŸ\ZŠû\ËC\ÅıB?/ä†­\áº‡Ë¯„ü]\Ø\ö\Ö\ğï—=)½hCZ\ğ\Ó3`=‘W¬\'\0h0Iç•ªL\Òs\æ\Ì\Ù#”\ö\ß\åıÀPÜ—Ÿ¹8d\ãT¾®ÿ·¸-\ìw\Ğ\È\È\Èo\ÅÛ¤\÷]ªıc§.~\è\õD^©\Êz€6˜¤\óJi“t<Jû„¿h\ê\ìü„\ËÅ³\ö!?	ù\ê\Ô\Ùü·…¼\"\åO\ïúl¿p\ìÅŸ;…º`=‘Wv´ø\éOú\ë·\Şz\ë5W^y\å}\õW\õ7ş‡:@˜¤\óÊ&\éœ-]º\ô1\ñ\÷\çCÁs¸ü‹ù\Û¯ş[\ár}È½!—†|vş\ö\ß\Ã?8\äw\Ş\ô¦7ıbz?0[¦J?\Ğë‰¼’®\'jµ\Úc¯¿şúÚ¶m\ÛÃµ)a¿‡¾\ô¥/­}\ë[\ßú²\éûP“t^I\'\éœx\àŸ|Jû¡!šúDü\Ì\ßş	ù7…Ÿ®ÿHøú\è¹s\ç¾\ê\ĞCı\Òû€)ı\Ğ=ë‰¼2}=q\ÓM7\í·~ıú;\ëe?u\ó\Í7ÿ\ì”SN9{dd\ä	\Ó_S\0\na’\Î+”ş\İ\ãßªş\õ¡½+\ä\ã!\ã!kC\îù¯3\Ã\ö¿\nn¸|~X$<.½&J?t\Ïz\"¯\Ä\×#xüO~\ò“¯=üp\ã\äş=„\ÛlY²dÉŸ¤¯-\0C\Î$WUú8\à€½C\ÑyA(\í\ábşÒ/†¯¯—\÷‡\Üra\ÈG\Ãu\ï\åşu\á\ëg„›\í\Ş”@\é‡\îYO\ä•K.¹¤v\ï½\÷nM\Ëı®lÚ´\égŸø\Ä\'.ù\õ\ô5`H™¤\óJ¯K˜´Ÿ\n\ÍkBqÿ³P\à—…¯¿r\ËT¹¿*\\V¸<9d,xQ\Øÿ—\Òû€\Ò)ı\Ğ=\ë‰|r\Çw\Ôş\áş¡¶aÃ†´\Ó\ïR¼M¼m¥œ¤?¯@Å˜¤\óJ\'¥Ñ¢E{-X°à¹¡Ô„ı\ÄP\ä\Ï——…\Ë\Í\ár]\ÈÊ°\í\áß‹C\Şÿ\ï}ü\ğ½\ô~ ª,ˆ {\Öy\åœsÎ©q\Äßœ˜˜¸\ë‘G\Ù\õûûkµŸ¯[·\î‹ú§\Z?«\ç\ë\Î\ô—\Ã˜¤3\Ë\ÎJ(\ìÿ#d¿0x\òqR¹1dk¸ş\ÚpyN\Èß„‚\Ø\Ø\Ø\ØK†\åy\Şşñ‹´x\Ù\Ä\Æ\ôúAH{\É\ÇV>û˜e?š¾Ï¤·exYA\÷JXO\Üw\ß}µı\ö\Û/MÛ†-\õ\õDüü\÷¿ÿı\ëw\öA~?ü\ğ\ñƒü\ÂXx{\Ü?yir\æ8 ˆIº¤\\pÁµP\Úÿw ÿ8\\¾\'\\~:\ä»\á\ëM\áò®‹Bş5\ä\Øp\İ†rÿ¡\Ü?6}]\éL(ü\ï:\æÔ‰L¯§lDĞ½a_O\Äßù\Ë_>Yøc\Ò\íÃ–\é\'\âI€œv\æ™g\Ş\ó\óŸÿüi}\óe—]\ö\é°\í\ö0~|XN03\æ8`\è\'\é’\òÀ\Ô.\\\÷êºÿıÿ\ÂåŸ†¼\ì°\Ãû\å\ôµF‹—M|uÉ²\ñ&¿^¾\ò‹O?v\ñ²•B\Ñş|cŸxı²‰;C¶\Ôø±Á>‹—jÉ²‰»\Ã\õ·³|\âÀi\÷¹\á„\ÓW<a\ñ\ò‰ÿ¾¾è˜LüV\Ø\÷ú¸m\É\ò‰±G\İw²oúØ¡\ô#\\\÷\õø8\Ç,[yB\ãv-Sz[†—to\Ø\×\á?¡\ö{¿\÷{E–şºy\ó\æıÁ1\Ç\ó\Ã\à§w\ß}\÷E\ñ1Æ¿«\â\õé¾”\Ãı$]Z\â™ş\ô5*Iı\í\ó!rX¼nªü/m\ìT«\í~Ì©+~uÉ©s\ëoŸ%}Y<ÿ\çË¾¾o(üo\×\ßQ\ß=\ô‹ÿb\Ù\Ä\ó—œ:~\Ş1]ù‡\Ç,[üq[øú\Ä\é\÷\î;ı±—ü\ãw\î\÷\Ş%§®|\ã»NYøúú\íZ?§\äy3´,ˆ {Ã¾8è ƒj\ëÖ­+º\ôGs\æ\Ì\Ùctt\ôø0\î\İ/\ã¿\Ó}(‹9úIº´\ìh’.I<“¾øÔ‰\÷ş\÷¿\'Îˆg\Í\ã\×\Ûû\Ä\r\ñL}¸¼)\ä³Sû\Ü\ñ^8ùn‡wÿ\ÃÊ§„ošvûO.Y>şp›«c9·Y|\ê\Ê#¶oÿú}\ï`\ß\Æc/^>ş¦c–¯<\'~½\äÇŸ¶­_\ï\ä95n\Ëp³ ‚î•²Ø­\ğ\ÒO\õ˜\ã `\á|eÈœ\ôúT)“t))}’eÿ\ä\É,›XµøŸV>q\òºe\ß[²lü%S_\ß\ó®SW>ç˜Lü~øzm|ş\Ô\õ\ßu\ê\Äk&ßš?ù+\ãÿÒ¸\Ï\åÇ‡\í›\Ã\õ§\ö½|É²_ |ı\óú}\ï`\ßÿ~\ìøn‚\å_›üŸ\n\á\ëú»v\òœ\Z·e¸YA\÷JYO\ì¦\ôSs,ş€Oe§å¿”Iº””<I/Y6q\Ò1\Ë&ş*~\'?”\æ3§®¿{\É?ÿ\äøu¸\î\ã!\÷Ç·\ßÇ²ı\ç\ËV?µ±ÿ\ò‰\Ãu[\Âå§?\åÛo\Ü\ï©…\ëoYºt\å\ä[\Ãıº8¾M\Ù\Ä?‡\Ë\r\õû\ŞÁ¾\Óû†©\ß\İß¸øÔ•\ß\Ü\Õsš~[†›t¯”\õ\ÄnJ?…1\ÇAÁ¦•ş–ÿR&\éRb’™©\ß\Ã?c\É\ò‰?K·¥f²/\ÕbA\İ+e=±›\ÒOa\ÌqP°¥¿eù/e’.%&é™‰gş—,[y\ÖÒ¥K“nK\Íd_ªÅ‚ºg=‘W¬\'¨3\ÇAÁZ”ı4“\å\ß$WL\Ò0xD\Ğ=ë‰¼b=A9¬\Ü{\ï½O>\÷\ÜsOş\èG?z\ÕI\'´\á„N\Øú\ö·¿ı‘x }\ô\Ñ{ì±›\ß\÷¾\÷\İz\ò\É\'Ÿw\ÜqÇ½1\Üd\÷\ô>rÖ¢tg“t^1I\Ã\àÅ±0½˜ë‰¼b=A9,|\ï{\ß;ú#ù\ÈÚ£:ªŠ~\í\ì³Ï®]~ù\åµo¼±¶q\ã\ÆZ/\ã¿\ã\õqû\ñ\Çÿ\È\á‡ş`(ÿŒ=+½\Ï\rú.-\÷Ó²r¾·\÷g“4Ş \Çg(‘\õD^± \ÎÇ¬ºú\ê«\ßü\ñü#<²\ö\å/¹v\×]wMüv\Åı\ã\íBùÿÙ’%K\Î\Z}Jú9\ôÜ®\Ê~\İ0O\Ò\ßúÖ·j/~\ñ‹kû\ì³O\íù\Ï~m\åÊ•Mû[L\Ò0xƒŸ¡DÃ¼(1\ÖÔ™\ã˜¡¯?\ö\ë_ÿú¹±\ì\æ3Ÿ©mÙ²%\í\ó3o\ïgÑ¢Ez\è¡”>^.ı·«²_7Ì“\ôk^\óš\Ú\ãÿø\Ú\\¿·µ§?ı\éMû[L\Ò0xƒŸ¡DÃ¼(1\ÖÔ™\ã¸\ĞÑŸø¹\Ï}\îúw¼\ãµ5kÖ¤ı½+\ñş<\òÈ­‡~øI\é\ã\æ`\Ğ?p»*ûu%L\Ò]t\Ñd\é\ÉK^Ò´m\Øb’†Á\ôø%*a=QR¬\'¨3\Ç1P¡—?qÙ²e›N<\ñ\ÄÚ¦M›\Ò\Î\Ş\ñ~O8\á„;\ì°Ï¦?\Ûrı+a’\Şw\ß}kO{\Ú\Ój\×_}Ó¶a‹I\Z/\×\ñ†I	ë‰’b=A9	}ü±Ÿû\Ü\çnˆ…?Dı\ïÿ\Øc½gÁ‚\ïKŸ\Çl\Ê\õ®„Iú’K.™<\ÓÿŠW¼¢iÛ°\Å$\rƒ—\ëøÃ¤„\õDI± \Î\ÇÀ\Ä\ß\á\ç;\ßÙ·3ü©ø8Gq\Ä\æp˜>—Ù’\ë\\	“\ôı\÷\ß?Yú\ãú¥Û†-&i¼\\\Çg&%¬\'JŠ\õu\æ8\âŠ+®\Ø?~h_¯‡W\â\ãzè¡›FFFš>§Ù\ë\Ü0O\Ò/ù\Ëk{\ï½w\íœsÎ™,ı¯ı\ë›\ö¶˜¤a\ğrŸa˜\óz¢\ÄXOPgc \âŸå‹Ÿ®?N;\í´;Â~júœfC®?p\Ã<I\Ç?\Ñ\÷\Ü\ç>w²ø¿úÕ¯ü=\é>\Ã“4^®\ã3“a^O”\ë	\ê\Ìq\ô\İ\÷¾\÷½£\ãYşÍ›7§}| \â\ã.\\¸\ğŞ±±±g¥\Ïm\Ğrı3I\ç“4^®\ã3ë‰¼b=A9¾û\ÈG>²6¾\õz6}\ö³Ÿ½1ì§§\Ïm\Ğrı3I\ç“4^®\ã3ë‰¼b=A9¾Ú¼y\ó/}\ôÑµ;\ï¼3\í\áÿ\çccc\ë—.]ú˜\ô9R®?p&\é¼b’†Á\Ëu|†ab=‘W¬\'¨3\Ç\ÑW_ı\êWO:\á„\Ò>+8âˆ›\Ãÿ²\ô9R®?p&\é¼b’†Á\Ëu|†ab=‘W¬\'¨3\Ç\ÑWË—/ÿÁ\ÙgŸ\ö\ïY\ñ‰O|\â\ÊpÀÿ]ú)\×8“t^1I\Ã\à\å:>\Ã0±\È+\ÖÔ™\ãè«“N:i\Ã\å—_\ö\ïY±z\õ\ê«\çÍ›wnú)\×8“t^1I\Ã\à\å:>\Ã0±\È+\ÖÔ™\ã\è«\ã?~küj9¸\îº\ë\â\Ûû¿Ÿ>\ÇA\Ê\õ\Î$WL\Ò0x¹\Ï0L¬\'\òŠ\õu\æ8ú\êè£~\ä\î»\ïNû\÷¬Ø¸q\ã–pÀ¯MŸ\ã \åúg’\Î+&i¼\\\Çg&\Öy\Åz‚:s}µp\á\Â\ÚC=”\ö\ïY\Ç\æxÀ\Ïv\Ò\ïQL\Òy\Å$\rƒ—\ëø\Ã\Äz\"¯XOPg£¯:ê¨‡r9\Ó\×]wıdş,Ÿ\éÏ•I:¯˜¤a\ğ,ˆ {\Öy\Åz‚:s}\õ\îw¿{s.¿\Óÿ\Ãş\ğ²ù³ü;ı¹2I\ç“4t\Ïz\"¯XOPg£¯N<\ñ\Ä\Ûrù\ôş/¼p|¶?½?W&\é¼b’†Á³ ‚\îYO\ä\ë	\ê\Ìq\ô\ÕÒ¥K\Ï;û\ì³\Óş=+>\ô¡ø¿KŸ#&\é\Üb’†Á³ ‚\îYO\ä\ë	\ê\Ìq\ô\Õ	\'œ\ğ†\ã?ş‘´€Ï‚‡\Æ\ÆÆ®ü\Ë\Ò\çˆI:·˜¤a\ğ,ˆ {\Öy\Åz‚:s}µt\é\Ò\Çq\Ä\Şy\çi	¨\Ûo¿ı\âp°¯‹\Ï\'}˜¤s‹I\ZÏ‚ºg=‘W¬\'¨3\Ç\Ñw\Çw\Ü\çœsN\Ú\Ã\êƒü\à™\á`?=}nlg’\Î+&i<\"\è\Şøøø#[·nmš\×d\ğ	¯\Ãa=±-}¨&s}766\ö¬\Ã?üg›7oN»ø@lÚ´\é\Òp \ßŸGú\Ü\ØN\é\Ï+J?to\õ\ê\Õw¬_¿¾i^“Á\ç–[nùbXO\\š¾FT“9X²d\ÉYŸù\Ìg\Ò>>á±¿\ôS\Ó\ç\ÄSú\óŠ\ÒƒgAİ›˜˜8hÕªU\÷®[·nƒ3ş³“\ğ}_w\ó\Í7Ÿ\Ö·†ìŸ¾FT“9y\ê¢E‹X³fM\Ú\Éû\ê²\Ë.‹o\ë¿3>~úœøoJ^Qúa\ğª¸ \n\Ó\äa^>\ë’K.ù\Ùøøø\ä\Ø#ƒM|;|<;\Ërúú«\ğßµÈ¥!¦ÿ½2\Ä\ï{üş+ü4Tqc–z\è¡t\ÔQGmİ´iS\Ú\Íû\â\Î;\ïüV8Ào	90}.<Zœ$\Ò\â)³—øz¤¯\Ğ_U\\…\Âv(œµøvlgeg\'\ñû¿ÿ\ñì¸’\ôK\ç8f\Ñ\á‡~\Ò	\'œ\ğ@œ\èúiÛ¶m?ûş\è\è\è{\Ó\ç@3¥?¯(ı0xU\\\Å3ü~ÿ:Ä·\ÃÇ³³\ék\ĞUœ\ã˜e‡v\Øg=\ö\Ø{úu\Æÿ\Î;\ï\\8¸ÿ5}lZSú\óŠ\ÒƒW\ÅQ|K¿3üy$¾+|\Ò:\Ğ\'Uœ\ã\ÈÀ‚\Şwø\á‡o\î\õ\ïøOıÿ­\Î\ğÏŒÒŸW”~¼*.ˆŒıy\Å\Ø\ôK\ç82\×ş\ĞC\İt\Úi§İ±eË–´¿\Ï\È=\÷\Ü\ó½©O\é¿\Ó\ï\ğÏœ…_^±\ğƒÁ«â‚¨İ±ÿ¾{\Ö\Õ\Ö|ÿSµk/<y2\ñ\ëx]ºŸtc?\Ğ/Uœ\ã\È\È\è\è\èS\ÂAx\êÂ…\ïı\ô§?}\ã]w\İ\õPZ\èwâ¡µk\×^|\ÒI\'M~B¼Ÿx\éc°k\í.üd0±\ğƒÁ«â‚¨±Ë¦µµk.x\íª¯\÷¨\Ä\ë\â¶t\é<\Æ~ _ª8Ç‘¡±±±g…ƒ\ñ\ôP\Ú\ï|\Û\Û\Şv\Ë\'>\ñ‰+V¯^}\õu\×]w\ë†\r\îËwŞ¹\æ\Úk¯½,LŠ+>\ô¡nsu¸\ÍO\ã\í\â\í\Óû¤}\í,üdp±\ğƒÁ«â‚¨±ÿ¶k\Ïm*ü\õ\Ü~\íyMûK\ç1\öıR\Å9¼\í>w\î\Üÿ\Êÿ\ßÎ›7\ï\Üp€^\Zr[<P§./\×\Ç\íq¿¸z\Ì\\;?\\,ü`\ğª¸ jg\ìÿ\Ñ\ÊSš\Ê~=q[º¿tc?\Ğ/Uœ\ã€\Äøøø#>Á9„\×\á>Á®Š¢vJÿ5+–6•ız\â¶t\é<J?\Ğ/Uœ\ã€\Ä\êÕ«\ï\ğ·š\ó\È-·\Ü\ò\ÅşV3\\DJ^Qú~©\â$&&&ZµjÕ½\ëÖ­\Û\àŒÿ\ì$|\ß\×\İ|\ó\Íg†Eß­!û§¯\Ğ_U\\µSú\ã§\õ§e¿¸-\İ_:\Ò\ôK\ç8 …X4\ã\æ\ã\Âc´\ôº!Jü¾\Ç\ï¿\Â³ Š¢8\ö¤\Å3\Í\r´©\ì\×·¥ûKç‰¯Gú\Z\ôB\ç8 P4 SU?\Ú)ı\×^]»\æ›h*ü\ñº¸-\İ_:\Ò\ôK\ç8 P4 SU?\Ú)ı17]\öÙ¦\Ò¯K\÷“\î¢\ôıR\Å9(”\r\èTÇ¶Jÿ¶mµ¾sZS\é\×\ÅmMûK\ÇQú~©\âÊ€tªŠ\ãÇ®Jÿ–Mkk\×_¼¼©\ğ\×·\Å}\Ò\ÛIgQú~©\âÊ€tªŠ\ã\ÇKÿ¶mµu7L\Ô~pş_6ı4qŸ¸¯³ş\İG\éú¥ŠsP(\ZĞ©*­Jÿ®\Î\î\ï(\Îúw¥\è—*\Îq@¡h@§ª8~´*ı\íœ\İ\ßQ\âm\Óû“\ö£\ôıR\Å9(”\r\èTÇV¥?-\ò3Mz\Ò~”~ _ª8\Ç…2 ª\âøÑª\ô\Ë\ìE\éú¥ŠsP(\ZĞ©*J^Qú~©\âÊ€tªŠ\ã‡ÒŸW”~ _ª8\Ç…2 ª\âø¡\ô\ç¥\è—*\Îq@¡h@§ª8~(ıyE\éú¥ŠsP(\ZĞ©*J^Qú~©\âÊ€tªŠ\ã‡ÒŸW”~ _ª8\Ç…2 ª\âø¡\ô\ç¥\è—*\Îq@¡h@§ª8~(ıyE\éú¥ŠsP(\ZĞ©*J^Qú~©\âÊ€tªŠ\ã‡ÒŸW”~ _ª8\Ç…2 ª\âø¡\ô\ç¥\è—*\Îq@¡h@§ª8~(ıyE\éú¥ŠsP(\ZĞ©*J^Qú~©\âÊ€tª¤\ñ#ü·¬™“^ŸRú\óŠ\Ò\ôKIsPq4 S%\ñ¿e*;-ÿJ^Qú~)i*Î€tª¤\ñcZ\é\ßiùWú\óŠ\Ò\ôKIsPq4 S%-J\Ë\ò¯\ô\ç¥è—’\æ8 \âh@§J\Z?Z”ı4“\å_\é\Ï+J?\Ğ/%\Íq@\ÅĞ€Nµ(\Æ\ÅG\é\Ï+\é\ë#³›tŒ€a\æ˜Ša@\Ø\éÿÀX9\ß\Ûû³3ıù° 4i 4€–¥ÿQe¿n˜Kÿù\çŸ_{\ÎsS\Ûk¯½j/zÑ‹j—_~y\Ó>\Ã¥?\Ö”\Æ1\rÃ€\ğ¨\Òß²\ì\×\rs\é\ñ‹_\\û\Å_ü\ÅzQ®½\ğ…/l\ÚgØ¢\ô\ç\Ãz‚\Ò8¦b\Ğ\0&\ÇÂ–ıºa.ı\õ\Ü|\óÍ“¥\ß}\÷m\Ú6lQú\óa=Ai\Ó@1h\0\í+¡\ô¿\ï}\ï›,ıo~ó››¶\r[”ş|XOP\Z\Ç4P\Z@û†½\ô\á_¨=\æ1©=\íiO«\İp\Ã\rMÛ‡-J>¬\'(c\Z(†\r }\Ã\\ú/¾ø\âÚ{\îY{Á^P[³fM\Ó\öaŒÒŸ\ë	J\ã˜Ša@h\ß0—ş7¼\á\rµ7¾\ñµ{î¹§iÛ°F\éÏ‡\õ¥qL\Å0 ´o˜KÿSŸú\Ô8\Ş7r\â‰\'6\í3lQú\óa=Ai\Ó@1h\0\í\æ\Ò_b”ş|XOP\Z\Ç4P\Z@û”ş¼¢\ô\ç\Ãz‚\Ò8¦b\Ğ\0Ú§\ô\ç¥?\Ö”\Æ1\rÃ€\Ğ>¥?¯(ıù° 4i 4€\ö)ıyE\éÏ‡\õ¥qL\Å0 ´O\é\Ï+J>¬\'(c\Z(†\r }J^Qú\óa=Ai\Ó@1h\0\íSú\óŠÒŸ\ë	J\ã˜Ša@hŸÒŸW”ş|XOP\Z\Ç4P\Z@û”ş¼¢\ô\ç\Ãz‚\Ò8¦b\Ğ\0Ú§\ô\ç¥?\Ö”\Æ1\rÃ€\Ğ>¥?¯(ıù° 4i 4€\ö)ıyE\éÏ‡\õ¥qL\Å0 ´O\é\Ï+J>¬\'(c\Z(†\r }J^Qú\óa=Ai\Ó@1h\0\íSú\óŠÒŸ\ë	J\ã˜Ša@hŸÒŸW”ş|XOP\Z\Ç4P\Z@û”ş¼¢\ô\ç\Ãz‚\Ò8¦b\Ğ\0Ú§\ô\ç¥?\Ö”\Æ1\rÃ€\Ğ>¥?¯(ıù° 4i 4€\ö)ıyE\éÏ‡\õ¥qL\Å0 ´O\é\Ï+J>¬\'(c\Z(†\r }J^Qú\óa=Ai\Ó@1h\0\íSú\óŠÒŸ\ë	J\ã˜Ša@hŸÒŸW”ş|XOP\Z\Ç4P\Z@û”ş¼¢\ô\ç\Ãz‚\Ò8¦b\Ğ\0Ú§\ô\ç¥?\Ö”\Æ1\rÃ€Ğ¾\ñ\ñ\ñG¶n\İ\ÚT>e\ğ	¯\Ã¡\ôoK_#f‡\õ¥qL\Å0 ´o\õ\ê\Õw¬_¿¾©€\Ê\às\Ë-·|1”şK\Ó×ˆ\Ùa=Ai\Ó@1h\0í›˜˜8hÕªU\÷®[·nƒ3ş³“\ğ}_w\ó\Í7Ÿ\nÿ­!û§¯³\Ãz‚\Ò8¦b\Ğ\0f&\Íx†9\äÁø;\å2\ğ\Ä\ï{üş+ü± 4i 4\0 [\Ö”\Æ1\rÃ€\0t\Ëz‚\Ò8¦b\Ğ\0€nYOP\Z\Ç4P\Z\0\Ğ-\ë	J\ã˜Ša@\0ºe=Ai\Ó@1h\0@·¬\'(c\Z(†\r\0\è–\õ¥qL\Å0 \0İ² 4i 4\0 [\Ö”\Æ1\rÃ€\0t\Ëz‚\Ò8¦b\Ğ\0€nYOP\Z\Ç4P\Z\0\Ğ-\ë	J\ã˜Ša@\0ºe=Ai\Ó@1h\0@·¬\'(c\Z(†\r\0\è–\õ¥qL\Å0 \0İ² 4i 4\0 [\Ö”\Æ1\rÃ€\0t\Ëz‚\Ò8¦b\Ğ\0€nYOP\Z\Ç4P\Z\0\Ğ-\ë	J\ã˜Ša@\0ºe=Ai\Ó@1h\0@·¬\'(c\Z(†\r\0\è–\õ¥qL\rq@\ö¤ÿM\0\03a=Ai\Ó@ƒ\0¨:\ë!J\ã˜\Z\0@\ÕYQ\Z\Ç4\Ğ`@\0\0ª\Îzˆ\Ò8¦\0Pu\ÖC”\Æ1\r4\0€ª³¢4i Á€\0\0T\õ¥qL\r\0 ê¬‡(c\Zh0 \0\0Ug=Di\Ó@ƒ\0¨:\ë!J\ã˜\Z\0@\ÕYQ\Z\Ç4\Ğ`@\0\0ª\Îzˆ\Ò8¦\0Pu\ÖC”\Æ1\r4\0€ª³¢4i Á€\0\0T\õ¥qL\r\0 ê¬‡(c\Zh0 \0\0Ug=Di\Ó@ƒ\0¨:\ë!J\ã˜\Z\0@\ÕYQ\Z\Ç4\Ğ`@\0\0ª\Îzˆ\Ò8¦\0Pu\ÖC”\Æ1\r4\0€ª³¢4i Á€\0\0T\õ¥qL\r\0 ê¬‡(c\Zh0 \0\0Ug=Di\Ó@ƒ\0¨:\ë!J\ã˜\Z\0@\ÕYQ\Z\Ç4\Ğ`@\0\0ª\Îzˆ\Ò8¦\0Pu\ÖC”\Æ1\r4\0€ª³¢4i Á€\0\0T\õ¥qL\r\0 ê¬‡(c\Zh0 \0\0Ug=Di\Ó@ƒ\0¨:\ë!J\ã˜\Z\0@\ÕYQ\Z\Ç4\Ğ`@\0\0ª\Îzˆ\Ò8¦\0Pu\ÖC”\Æ1\r4\0€ª³¢4i Á€\0\0T\õ¥qL\r\0 ê¬‡(c\Zh0 \0\0Ug=Di\Ó@ƒ\0¨:\ë!J\ã˜\Z\0@\ÕYQ\Z\Ç4\Ğ`@\0\0ª\Îzˆ\Ò8¦\0Pu\ÖC”\Æ1\r4\0€ª³¢4i Á€\0\0T\õ¥qL\r\0 ê¬‡(c\Zh0 \0\0Ug=Di\Ó@ƒ\0¨:\ë!J\ã˜\Z\0@\ÕYQ\Z\Ç4\Ğ`@\0\0ª\Îzˆ\Ò8¦\0Pu\ÖC”\Æ1\r4\0€ª³¢4i Á€\0\0T\õ¥qL\r\0 ê¬‡(c\Zh0 \0\0Ug=Di\Ó@ƒ\0¨:\ë!J\ã˜\Z\0@\ÕYQ\Z\Ç4\Ğ`@\0\0ª\Îzˆ\Ò8¦\0Pu\ÖC”\Æ1\r4\0€ª³¢4i Á€\0\0T\õ¥qL\r\0 ê¬‡(c\Zh0 \0\0Ug=Di\Ó@ƒ\0¨:\ë!J\ã˜\Z\0yª\Õj{®Y³\æ¬K.¹\äg\ã\ã\ãµ+VÈ€¾ï¬^½ú‰‰‰ƒ\Ò×‡²XQ\Z\Ç4\Ğ`@\0\ÈS(üg‡\ÂY[¿~}m\ëÖ­µ|Pœø}\ßÿU«Vİ»bÅŠı\Ó×ˆrXQ\Z\Ç4\Ğ`@\0\ÈS<\ÃgZDe\ğY·nİ†Pú/M_#\Êa=Di\Ó@ƒ O\ñ-ı\Î\ğ\ç‘ø:„Ò¿-}(‡\õ¥qL\r€<\Å\ß)OË§\Ì^\âë‘¾F”\Ãzˆ\Ò8¦@\Ú-ı\÷İ³®¶\æûŸª]{\áÉ“‰_\Ç\ë\Òı¤»(ıe³¢4i Á€\0§vJÿ–Mkk\×\\\ğş\ÚU_;\îQ‰\×\Åm\éş\Òy”ş²YQ\Z\Ç4\Ğ`@\0\ÈS;¥ÿ¶k\Ïm*ü\õ\Ü~\íyMûK\çQú\Ëf=Di\Ó@ƒ O\í”ş­<¥©\ì\×·¥ûK\çQú\Ëf=Di\Ó@ƒ O\í”şkV,m*û\õ\Äm\éş\Òy”ş²YQ\Z\Ç4\Ğ`@\0È“ÒŸW”ş²YQ\Z\Ç4\Ğ`@\0\ÈS;¥?~ZZ\ö\ë‰\Û\Òı¥\ó(ıe³¢4i Á€\0§vJÿ\r´©\ì\×·¥ûK\çQú\Ëf=Di\Ó@ƒ O\í”şk¯®]\ó\Í4şx]Ü–\î/G\é/›\õ¥qL\r€<µSúcnº\ì³M¥?^—\î\'\İE\é/›\õ¥qLCE…ş•q\0\ØEV¦·`\ğ\Ú*ıÛ¶\Õnø\ÎiM¥?^·5\í/G\é/›‚Di\ÓPQ\á‡N‹’ŸfNz;\0oW¥Ë¦µµ\ë/^\ŞTø\ë‰\Û\â>\é\í¤³(ıeS(c\Z*,\ÉoQ\ô\å\È\ÌKÿ¶mµu7L\Ô~pş_6ı4qŸ¸¯³ş\İG\é/›‚Di\ÓPa\ñL~‹²\ï,?@fZ•ş]\İ\ßQœ\õ\ï>J\Ù$Jã˜†Š‹g\ô[~gù2Òª\ô·svG‰·M\ïOÚ\Ò_6‰\Ò8¦¡\â\âı¥Nº\0³§U\éO‹üL“ŞŸ´¥¿l\n¥qL\é\Ù~gù2Óª\ô\Ë\ìE\é/›‚Di\Ó@z¶Nº€Ù¥\ô\ç¥¿l\n¥qL“\êgû\Ó\ë˜}J^Qú\Ëf=Di\ÓÀ¤ú\Ùş\ôz\0fŸÒŸW”ş²YQ\Z\Ç4P·\ßÔ€°_º€Ù¥\ô\ç¥¿l\n¥qLQ,ú\ëCºTü2¢\ô\ç¥¿l\n¥qL\õ\Â_/ú\é¿˜eJ^QúË¦ Q\Z\Ç4TÛ\nş®`(ıyE\é/›‚Di\ÓP]»*\ö»\ÚÀ€(ıyE\é/›‚Di\ÓPM\íúv\÷ ”ş¼¢\ô—MA¢4i¨™ù™\î@)ıyE\é/›‚Di\ÓP-øNo@(ıyE\é/G(C+c!\ÚEV¦·ƒa¢\ôCut[Ü»½=\0Rú\óŠ\Ò_P†\æ´(ùiæ¤·ƒa¢\ôC5\ôª°\÷\ê~\0˜¥?¯(ıe‰g\ò[}gù)†\Ò\å\ëuQ\ï\õıT\ÖTÙ˜“^ŸRú\óŠ\Ò_–ø3Ø¢\ì;\ËO1”~([¿\nz¿\î R¦ŸM\ÜY¹Pú\óŠ\Ò_©ŸÁ´\ğ;\ËO”~(W¿‹y¿\ï x­JF«\ò¯\ô\ç¥¿<\ñ\ç®\Å\Ï\ãœt?FJ?”iz!\ß=\äO¦.»\Ñ\ê~€.´(-Ë¿ÒŸW”ş2Mı\Ü5~\Ó\í0¬”~(OZø?\Ğ\ãe§\Åg\÷£øt¨E\ÙO3Yş•ş¼¢\ô—)ş¬MûÙ›“n‡a¥\ôCY\Ò\Ï\Ì\Ç\òz\Ò\ÂŞé…¿\ÃµG\ó\ã\ÂPjQ¸D²ˆÒŸW”şrÍŸ:ÛŸ^\Ã\Ì1\r\åhU¼[\ö™ÿV·ÿ×©\ëS­†ŠI‘AK\Ëı´\Ä\â1§¾ŸÒŸW”ş2,Z´\è‰\á\ç\ìy\ó\æ-—\ã!7…¯·Mı>ÿ¯Ÿ\Ú~H\Ü?½\ñ˜N¯†\Ï\Î\nw«\â\ŞN\ñou»şº=ÈI‘AKŠ~SÙ¯\æ\Ò\ÓM7MŸG&“\î3lQú‡\Û\è\è\è\ë\Â\Ï\Ù9!¶ø\ÜY\âş\ç\ÄÛ§\÷	9‹\Çoz0\\\Ú)Ú­\nüÎŠ«ıwUø\ë\Úy>%“\"ƒ6­L´,ûu\Ã\\ú¿\ô¥/M\Î#|pÓ¶a\Ò?œBYiø9ûn‹2\ßI¾\ï/}\ÈQ<f\Ó\ë€\á1“‚İªÈ·*ş­\ök·\ğ\×\Í\äyA6LŠ\Úü]”ıºa.ı\ï~\÷»\'\ç’ø\ÃMÛ†5JÿpY´h\Ñ^\ó\æ\ÍûXøY{$-\ï‹/®}ş\óŸ¯]~ù\åµo¼±¶q\ã\ÆZ/\ã¿\ã\õq{\Ü/½m¼¿x¿\ñş\ÓÇ„œX\ßÀ\ğ\ê¤X·*\ôÓ‹«\í3-üu<?˜U&Er5Ì¥ÿ•¯|\å\ä|\ò\ä\'?¹\ö´§=­v\á…6\í3lù\æ7¿i¬a\\\Æ\è\è\èU\Ó\Ëú\Ø\ØX\í\ô\ÓO¯\İ~û\í“¿]qÿx»xû¤ü_\'}lÈ…\õ\r§n\nu«bÿı˜\×wZø\ëºy0p&Er5Ì¥ÿw\÷wk§zj\í¢‹.šœ[\óœ\ç4\í3LÙºuk\í\ĞCE\ïÖo‡œ\òw¡XşÙ¼y\óş0\\>\Ï¾\å!¼¿²vzA?\å”Sj·\İv[\Ú\çg$\Ş>\ŞORüo‹—>È\õ\rŸ^\éV\Åÿº\ä\ß\İşº^<_“\"¹\Z\æ\Ò_\Ïı\÷\ß?9¿\ì½\÷\ŞMÛ†-\\pAmdd\äWC\É{y(ú\Â\å{\ÃøqZøúk\á\ò!\÷†\ë6Ç¯§®;-\î\÷\r_¿bll\ì\×\æÌ™³Gú:\Ó;\ó·Ÿ\áoşxvş\Üs\ÏMû{W\âı%gıo‹›>˜m\Ö70\\zY [ÿ^şº^>o\è“\"¹\Z\æ\Òÿ²—½¬\ö¸\Ç=®\ö•¯|eryÕ«^Õ´Ï°¥\ß\éyÂ‚Æ•ı\ã»\0\Â\å\ß\Í\ßş®€ø\î€ø.øI\ğ\Ş-\Ğ\ñwì§¿¥ÿ­o}k\íŠ+®H;{O\Äû\÷_¬+ı?¹±¾\áÑ\â\ßÒŸ\áÿ\×\÷Z??\ô”I‘\\\rs\é¯ı\Î\ïüNmŸ}\ö©½úÕ¯ü~\é>Ã–vJÿ®\Ä3ı\Ş-\ĞS\Ú\×8\Ãß¯\Â_\ïúÿøø\és‚\Ùd}\Ãa\ïİ¶\æc\Ó\r]\ØÙ™şVŸ\ê\ß\ÇNû?\á\"Y&=h!\Ã\\úKL/J;¼[`\æF·ÿY¾‡\ëcúyç—v\ô¾ˆ3m.y8>\ô¹Ál‰\Çez§^)oUø\Ó3ş½.ş½|ş\0•¢\ô\ç•A•ş]©Ê»\Â\ãc\Ó\ëv$üw}·^¾\ã‡\í\rR\ò\á~\ßMŸ\Ì¥†K/Šs«\Â‡¿Õ§\÷\÷ªø\÷\âyT–ÒŸWr)ı\í(\á\İS\Ï{yøoù¥t\Ûts\ç\Î}m½tÇ·\Ûwû)ı3oú\Ûü\ã\óIŸ#\Ì¥†O7zG…¿^\ì[m\ï¶øw\ó|\ØM\é\Ï-\ÃTúwe\Ş-ë”©\"}\ÓÎŠt\Ø~N½pŸ~ú\éi\'ˆø¸\õ\çŸOúa6(ı0œ:)Ò­\n}«O\éoµ_§Å¿“\ç	@B\é\Ï+%•şv\Ì\ö»\Â}|aZ‘”\÷/\ñ9M\ßgll\ìIS\ÏcrŸAŸå¯‹;\í¹>Ÿ\×\ô\ç	³!\éuÀp˜I¡nU\ä[şºVûÏ´ø\Ï\äù°J^©Z\éß•~¿[ \ìs\ñ´\"]\Ïm\ñ*L\Û\çú¶Å‹§]| \â\ãO{‡Lÿo™.l›²2½z-‹\éuÀ\ğh§X·*\ğ;+üu­n\×n\ño\çy\Ğ&¥?¯(ı3\×Í»\Â\×?*\ĞM	ûœ\îû\É\á\ë\å\õ\ë\Î8ãŒ´‡T|üi\Ïqyú½˜?U\ö\ëû¤Û¡\×g0üvV°[\÷v\n]«\Û\ïªø\ï\ìù\0\Ğ¥?¯(ı½·“w|=\\>2½\è·\È\İaÿ\ë\êÿ¾\ì²\Ë\Ò>P\ñ\ñ\ë\Ï%<¯‰ú\ãü¤\ì\×3ıû\0ı\à8ƒ2\ì¨hÿ\ÉnşºV\Åÿ°G\í\ñ\ßv\ô<\0\è‚ÒŸW”şÁ	e\å™iIN\ò@\È\í!×¯[³fM\Ú\Ã*>ş´\çß½Ğ²\ì×“ş7C¯9Î ­\n\÷\ô\Â\ŞI\á¯k\ç~Z=>\0= \ô\ç¥p\æÎûÊ´$O}>À§B^³t\é\Òø\'‡\ã‡ı\Å\ë&·\ß}\÷\İi¨øøÓ\ï\Ï\Ó\çŸ&ıo†^sœAYZ\ïX\Ğ\ã™ùVE}&vv?­€Qú\óŠ\Ò?8¡\Ì:UŠş=:22\ò¸t¿\é\åú¡‡J{ø@\Å\ÇO‹½\Èl\'ı™†Û ø  r”ş¼¢\ôN(úo	=ÿ\Ï.\\ø+\é¶\ér=\Ó?\õ¼¼½€T\Ô\ã\0TšÒŸW”şü\Ì\ßş»\ó“%:·\ß\éŸ\ö[–ÿ\éÿ\00ı.\äı¾\0¦(ıyE\é\ÏOü”üz‰\Î\õ\Óû\ë\Ò\òŸn€™\èW1\ï\×ıĞ‚ÒŸW”şü„\ò¼¼^¢\Ï8ãŒ´‡T|üig\ò—§Ïµ®^ş\Ó\ë`¦z]\Ğ{}\0\ì‚ÒŸW”şü„\ò|H½h/^¼8\í\áu\Ì1\Ç<2­\ô’>W\0\è‡^\õ^\İ\03 \ô\ç¥??cccO\nûÁzÙ¾\í¶\Û\Ò.>\ñq§ş\ã\óJŸ+\0\ôK·…½\Û\Û\Ğ!¥?¯(ıy\n%ûœz\á>ı\ô\Ó\Ó>>\ñq§•şs\Ò\ç\0ı\Öiq\ï\ôv\0\ô€ÒŸW”ş<Í;\÷µ\õ\Â=666\ğ³ı\ñ\ñFGGo\í\Ï\'}\003-\ğ3\İ€Sú\óŠÒŸ¯P¶¿[/İ§œrJ\Ú\Ëû*>Ş´³ü\ßMŸ\0R»E¾\İı\0\è#¥?¯(ıù\Z}i(\Ü\×\Ë\÷yç—vó¾ˆ3­\ğ?ŸGú\Ü\0`\ĞvU\èwµ€Qú\óŠÒŸ·y\ó\æ}lú\Ûü¯¸âŠ´£\÷T¼ÿ\éoëŸ>\'\0˜-;*\ö;º€Y \ô\ç¥?o‹-\Ú+”\ï+\ë%ü­o}kßŠ¼\ß\ğxw\ÄÇŸ>\'\0˜MiÁOÿ\rÀ,Sú\óŠÒŸ¿P¾Ÿr\Û\ô3ş\ç{n\ÚÙ»\ïoúş©\Ç{Fú\\\0 \õ¢\ìÔ¥\Â¥?¯(ı\Ã!\òß^üc\â‡\íuû©ş\ñ\öÉ‡\öMşøx\és\0€œ\ì\'­x™n\0`v)ıyE\é\ñ\Ìûüio\õ‰gıO?ı\ô\Ú\í·ß\öùŠû\Ç\Û%g\÷\'\ß\Ò\'}l\0\È\ÎT\é 3J^Qú‡Küû©\÷K\Ëzm\ñ\âÅµ\Ïş\óµ\Ë/¿¼v\ã7\Ö6n\Ü8Y\ğ\ãeüw¼>n?\æ˜cšn\ï/Ş¯\ß\á`h(ı\0y\Zd\ëÖ­M\åSŸ\ğ:\ÜJÿ¶\ô5\"S\Î\ï»-\Ê{\'ù®?\ËÀĞ‰“Xz\0³o\õ\ê\Õw¬_¿¾©€\Ê\às\Ë-·|1”şK\Ó×ˆ\á\Êú\ëÂšçœ[”ù%\îN¼}zŸ\00”~€<MLL´jÕª{×­[·Áÿ\ÙIø¾¯»ù\æ›\Ï…ÿÖı\Ó×ˆ\á³hÑ¢\'†µ\Ï!\ó\æ\Í[.\ÇCn\n©ÿù½¦ş=>µı¸z\00T”~€|Å¢\Ï0‡<§\\ø}\ß…¿Pa4g\ÚYı9\év\0zJ?\0PUa´rZ\é_™n€¡§\ô\0U”œ\åw¶€2)ı\0@%gù\í LJ?\0P5;8\Ë\ïl?\0\åQú€ª\ÙÁY~gû(\Ò\0TI<“ß¢è§™“\Ş\0†’\Ò\0TI<“ß¢\ä§q¶€2(ı\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0\Ë$\0T\õ\0\Å2\É\0Ug=@±Lr\0@\ÕYP,“\0Pu\ÖC\0!Lh+ã¤¶‹¬Lo\0P2¥€\"„	mN‹’ŸfNz;\0€’)ı\0#\ÉoQ\ô\å\0*K\é \ñL~‹²\ï,?\0PYJ?\0E‰g\ô[~gù€JRú(\Ê\Î\ö\ÏI\÷\0¨¥€\â$gû\å\0*K\é 8\É\Ùş9\év\0€ªPú(Rılz=\0@•XP¤ú\Ùş\ôz\0€*± I­V\ÛsÍš5g]r\É%?¯­X±Bœ\ğ}d\õ\ê\ÕwLLL”¾>\0\0\íRúh\nÿÙ¡p\ÖÖ¯__Ûºuk\íÁ”\'~\ß\ã\÷ÕªU\÷®X±bÿ\ô5\0h‡\Ò@“x†?Î´ˆ\Ê\à³nİº\r¡\ô_š¾F\0\0\íPúh\ß\Ò\ï‰¯C(ı\Û\Ò\×\0 J?\0M\âï”§\åSf/\ñ\õH_#\0€v(ı\04i·\ô\ßwÏºÚš\ïªv\í…\'O&~¯K\÷“\î¢\ô\0Rúh\ÒN\éß²im\íš\Ş_»\êk\Ç=*\ñº¸-\İ_:\Ò\0tJ\é I;¥ÿ¶k\Ïm*ü\õ\Ü~\íyMûK\çQú€N)ı\04i§\ôÿh\å)Me¿¸-\İ_:\Ò\0tJ\é I;¥ÿšK›\Ê~=q[º¿t¥\0\è”\Ò@¥?¯(ı\0@§”~\0š´Sú\ã§\õ§e¿¸-\İ_:\Ò\0tJ\é I;¥ÿ†‹?\ÚT\ö\ë‰\Û\Òı¥\ó(ı\0@§”~\0š´Sú7®½ºv\Í7?\ĞTø\ãuq[º¿t¥\0\è”\Ò@“vJ\ÌM—}¶©\ô\Ç\ë\Òı¤»(ı\0@§”~\0š´Uú·m«\İ\ğÓšJ¼.nk\Ú_:\Ò\0tJ\é É®Jÿ–Mkk\×_¼¼©\ğ\×·\Å}\Ò\ÛIgQú€N)ı\04\Ùa\éß¶­¶î†‰\Ú\ÎÿË¦¢Ÿ&\î\÷uÖ¿û(ı\0@§”~\0š´*ı»:»¿£8\ë\ß}”~\0 SJ?\0MZ•şv\Î\î\ï(\ñ¶\éıIûQú€N)ı\04iUú\Ó\"?Ó¤\÷\'\íG\é\0:¥\ôĞ¤U\é—Ù‹\Ò\0tJ\é ‰ÒŸW”~\0 SJ?\0M”ş¼¢\ô\0Rúh¢\ô\ç¥\0\è”\Ò@¥?¯(ı\0@§”~\0š(ıyE\é\0:¥\ô\ĞD\é\Ï+J?\0\Ğ)¥€&J^Qú€N)ı\04Qú\óŠ\Ò\0tJ\é ‰ÒŸW”~\0 SJ?\0M”ş¼¢\ô\0Rúh¢\ô\ç¥\0\è”\Ò@¥?¯(ı\0@§”~\0š(ıyE\é\0:¥\ô\ĞD\é\Ï+J?\0\Ğ)¥€&J^Qú€N)ı\04Qú\óŠ\Ò\0tJ\é ‰ÒŸW”~\0 SJ?\0M”ş¼¢\ô\0Rúh¢\ô\ç•8Y‹ˆˆˆtšt­@\Å)ıyÅ™~\0\0\0zf˜Kÿ\æÍ›kGuT\í\ÉO~r\í™\Ï|f\í«_ıj\Ó>\Ã¥\0\0€\æ\Òü\ñ\ÇÇ‚\\»\à‚jû\ì³Oí•¯|e\Ó>\Ã¥\0\0€\æ\Òÿ\ìg?»\ö”§<¥\éúa\Ò\0\0@\Ïs\é\ßs\Ï=\'Kÿ\ó\÷¼\Ú3\ñŒÚªU«š\ö¶(ı\0\0\0\ô\Ì0—ş=\ö\Øc\ò\íı\ç{\î\äe,ÿ\é>\Ã¥\0\0€\æ\Ò\Ï\î‡ÿ„\É\ô‹—{\ï½w\Ó>\Ã¥\0\0€\æ\Ò\ô\ÑGO–ı/ùË“—/}\éK›\ö¶(ı\0\0\0\ô\Ì0—ş»îº«v\ğÁ\×ÿø\Ç\×^ü\â×®¸âŠ¦}†-J?\0\0\0=3Ì¥¿\Ä(ı\0\0\0\ôŒÒŸW”~\0\0\0zF\é\Ï+J?\0\0\0=£\ô\ç¥\0\0€Qú\óŠ\Ò\0\0@\Ï(ıyE\é\0\0 g”ş¼¢\ô\0\0\Ğ3J^Qú\0\0\è¥?¯(ı\0\0\0\ôŒÒŸW”~\0\0\0zF\é\Ï+J?\0\0\0=£\ô\ç¥\0\0€Qú\óŠ\Ò\0\0@\Ï(ıyE\é\0\0 g”ş¼¢\ô\0\0\Ğ3J^Qú\0\0\è¥?¯(ı\0\0\0\ôŒÒŸW”~\0\0\0zF\é\Ï+J?\0\0\0=£\ô\ç¥\0\0€Qú\óŠ\Ò\0\0@\Ï(ıyE\é\0\0 g”ş¼¢\ô\0\0\Ğ3J^Qú\0\0\è¥?¯(ı\0\0\0\ôŒÒŸW”~\0\0\0zF\é\Ï+J?\0üÿ\í\Û1\n\Â@\ĞZxK\ñ	KJO±±\ÈHcmÄ¿m\Ò-Şƒ¹ÀŸjX\0øš®\ë\Ş\ëº\îÆ§Ÿ\èaŠ\Ñÿ\Üv\0\0\0M†a˜\æy\Ş\rP9>\ã8^c\ôß¶\0\0@“¾\ï\Ï)¥G)e\ñ\âÿ›\Ä\İK\Îùƒÿ9m;\0\0€fuh\Ö\æÈ«ş)—\ÃS\ï^\ïo\ğ\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0ü›1@k¿9²ı\0\0\0\0IEND®B`‚',1),('2',1,'D:\\workproject\\springbladeandreact\\springBlade\\blade-service\\blade-workflow\\target\\classes\\processes\\simple-approval.bpmn20.xml','1',_binary '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\r\n             xmlns:flowable=\"http://flowable.org/bpmn\"\r\n             targetNamespace=\"http://springblade.workflow\"\r\n             id=\"definitions_simpleApproval\">\r\n\r\n    <process id=\"simpleApproval\" name=\"Simple Approval\" isExecutable=\"true\">\r\n        <startEvent id=\"startEvent\" name=\"å‘èµ·\"/>\r\n        <userTask id=\"approveTask\" name=\"å®¡æ‰¹\" flowable:assignee=\"${approver}\"/>\r\n        <endEvent id=\"endEvent\" name=\"ç»“æŸ\"/>\r\n\r\n        <sequenceFlow id=\"flow_start_approve\" sourceRef=\"startEvent\" targetRef=\"approveTask\"/>\r\n        <sequenceFlow id=\"flow_approve_end\" sourceRef=\"approveTask\" targetRef=\"endEvent\"/>\r\n    </process>\r\n\r\n</definitions>\r\n',0);
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
INSERT INTO `act_ge_property` VALUES ('cfg.execution-related-entities-count','true',1),('cfg.task-related-entities-count','true',1),('common.schema.version','7.1.0.2',1),('eventregistry.schema.version','7.1.0.2',1),('next.dbid','17501',8),('schema.history','create(7.1.0.2)',1),('schema.version','7.1.0.2',1);
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
INSERT INTO `act_hi_actinst` VALUES ('15007',1,'flow_mu180u86md3e:6:15004','15005','15006','StartEvent_mu2c2p840',NULL,NULL,'å¼€å§‹','startEvent',NULL,'2026-09-17 16:47:20.465','2026-09-17 16:47:20.467',1,2,NULL,''),('15008',1,'flow_mu180u86md3e:6:15004','15005','15006','Flow_1g1p7by',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-09-17 16:47:20.468','2026-09-17 16:47:20.468',2,0,NULL,''),('15009',1,'flow_mu180u86md3e:6:15004','15005','15006','UserTask_mu2c2p880',NULL,NULL,'3','manualTask',NULL,'2026-09-17 16:47:20.468','2026-09-17 16:47:20.468',3,0,NULL,''),('15011',1,'flow_mu180u86md3e:6:15004','15005','15006','Flow_1qxvbp7',NULL,NULL,'xiaoyuyibai','sequenceFlow',NULL,'2026-09-17 16:47:20.468','2026-09-17 16:47:20.468',4,0,NULL,''),('15012',1,'flow_mu180u86md3e:6:15004','15005','15010','Flow_0dizg8q',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-09-17 16:47:20.468','2026-09-17 16:47:20.468',5,0,NULL,''),('15013',1,'flow_mu180u86md3e:6:15004','15005','15006','Gateway_15cdzo6',NULL,NULL,NULL,'parallelGateway',NULL,'2026-09-17 16:47:20.468','2026-09-17 16:47:20.470',6,2,NULL,''),('15014',1,'flow_mu180u86md3e:6:15004','15005','15010','Gateway_0a6n8eh',NULL,NULL,NULL,'exclusiveGateway',NULL,'2026-09-17 16:47:20.470','2026-09-17 16:47:20.470',7,0,NULL,''),('15016',1,'flow_mu180u86md3e:6:15004','15005','15006','Flow_1dpq6dk',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-09-17 16:47:20.470','2026-09-17 16:47:20.470',8,0,NULL,''),('15017',1,'flow_mu180u86md3e:6:15004','15005','15015','Flow_0icqanz',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-09-17 16:47:20.470','2026-09-17 16:47:20.470',9,0,NULL,''),('15018',1,'flow_mu180u86md3e:6:15004','15005','15010','Flow_13gvmcu',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-09-17 16:47:20.470','2026-09-17 16:47:20.470',10,0,NULL,''),('15019',1,'flow_mu180u86md3e:6:15004','15005','15006','Activity_0qm5tc6','15020',NULL,'1','userTask',NULL,'2026-09-17 16:47:20.470',NULL,11,NULL,NULL,''),('15021',1,'flow_mu180u86md3e:6:15004','15005','15015','Activity_1mzuxt6','15022',NULL,'2','userTask',NULL,'2026-09-17 16:47:20.483',NULL,12,NULL,NULL,''),('15023',1,'flow_mu180u86md3e:6:15004','15005','15010','Activity_02klewn','15024',NULL,'6','userTask',NULL,'2026-09-17 16:47:20.483',NULL,13,NULL,NULL,'');
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
INSERT INTO `act_hi_procinst` VALUES ('15005',1,'15005','2064530495200337922:2100506880313933826','flow_mu180u86md3e:6:15004','2026-09-17 16:47:20.464',NULL,NULL,NULL,'StartEvent_mu2c2p840',NULL,NULL,NULL,'',NULL,NULL,NULL,NULL,NULL,NULL,NULL);
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
INSERT INTO `act_hi_taskinst` VALUES ('15020',1,'flow_mu180u86md3e:6:15004',NULL,'Activity_0qm5tc6','15005','15006',NULL,NULL,NULL,NULL,NULL,'created','1',NULL,NULL,NULL,NULL,'2026-09-17 16:47:20.470',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,50,NULL,NULL,NULL,NULL,'','2026-09-17 16:47:20.483'),('15022',1,'flow_mu180u86md3e:6:15004',NULL,'Activity_1mzuxt6','15005','15015',NULL,NULL,NULL,NULL,NULL,'created','2',NULL,NULL,NULL,NULL,'2026-09-17 16:47:20.483',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,50,NULL,NULL,NULL,NULL,'','2026-09-17 16:47:20.483'),('15024',1,'flow_mu180u86md3e:6:15004',NULL,'Activity_02klewn','15005','15010',NULL,NULL,NULL,NULL,NULL,'created','6',NULL,NULL,NULL,NULL,'2026-09-17 16:47:20.483',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,50,NULL,NULL,NULL,NULL,'','2026-09-17 16:47:20.483');
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
INSERT INTO `act_re_deployment` VALUES ('1','SpringAutoDeployment',NULL,NULL,'','2026-09-09 12:09:23.792',NULL,NULL,'1',NULL),('12501','flow_mu180u86md3e',NULL,'flow_mu180u86md3e','','2026-09-17 08:30:33.167',NULL,NULL,'12501',NULL),('12515','flow_mu180u86md3e',NULL,'flow_mu180u86md3e','','2026-09-17 08:32:51.288',NULL,NULL,'12515',NULL),('12529','flow_mu180u86md3e',NULL,'flow_mu180u86md3e','','2026-09-17 08:34:16.407',NULL,NULL,'12529',NULL),('12553','flow_mu180u86md3e',NULL,'flow_mu180u86md3e','','2026-09-17 08:34:23.972',NULL,NULL,'12553',NULL),('12577','flow_mu180u86md3e',NULL,'flow_mu180u86md3e','','2026-09-17 08:45:45.316',NULL,NULL,'12577',NULL),('15001','flow_mu180u86md3e',NULL,'flow_mu180u86md3e','','2026-09-17 08:47:20.125',NULL,NULL,'15001',NULL);
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
INSERT INTO `act_re_procdef` VALUES ('flow_mu180u86md3e:1:12504',1,'http://bpmn.io/schema/bpmn','æµ‹è¯•914','flow_mu180u86md3e',1,'12501','flow_mu180u86md3e.bpmn20.xml','flow_mu180u86md3e.flow_mu180u86md3e.png',NULL,0,1,1,'',NULL,NULL,NULL,0),('flow_mu180u86md3e:2:12518',1,'http://bpmn.io/schema/bpmn','æµ‹è¯•914','flow_mu180u86md3e',2,'12515','flow_mu180u86md3e.bpmn20.xml','flow_mu180u86md3e.flow_mu180u86md3e.png',NULL,0,1,1,'',NULL,NULL,NULL,0),('flow_mu180u86md3e:3:12532',1,'http://bpmn.io/schema/bpmn','æµ‹è¯•914','flow_mu180u86md3e',3,'12529','flow_mu180u86md3e.bpmn20.xml','flow_mu180u86md3e.flow_mu180u86md3e.png',NULL,0,1,1,'',NULL,NULL,NULL,0),('flow_mu180u86md3e:4:12556',1,'http://bpmn.io/schema/bpmn','æµ‹è¯•914','flow_mu180u86md3e',4,'12553','flow_mu180u86md3e.bpmn20.xml','flow_mu180u86md3e.flow_mu180u86md3e.png',NULL,0,1,1,'',NULL,NULL,NULL,0),('flow_mu180u86md3e:5:12580',1,'http://bpmn.io/schema/bpmn','æµ‹è¯•914','flow_mu180u86md3e',5,'12577','flow_mu180u86md3e.bpmn20.xml','flow_mu180u86md3e.flow_mu180u86md3e.png',NULL,0,1,1,'',NULL,NULL,NULL,0),('flow_mu180u86md3e:6:15004',1,'http://bpmn.io/schema/bpmn','æµ‹è¯•914','flow_mu180u86md3e',6,'15001','flow_mu180u86md3e.bpmn20.xml','flow_mu180u86md3e.flow_mu180u86md3e.png',NULL,0,1,1,'',NULL,NULL,NULL,0),('simpleApproval:1:3',1,'http://springblade.workflow','Simple Approval','simpleApproval',1,'1','D:\\workproject\\springbladeandreact\\springBlade\\blade-service\\blade-workflow\\target\\classes\\processes\\simple-approval.bpmn20.xml',NULL,NULL,0,0,1,'',NULL,NULL,NULL,0);
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
INSERT INTO `act_ru_actinst` VALUES ('15007',1,'flow_mu180u86md3e:6:15004','15005','15006','StartEvent_mu2c2p840',NULL,NULL,'å¼€å§‹','startEvent',NULL,'2026-09-17 16:47:20.465','2026-09-17 16:47:20.467',2,1,NULL,''),('15008',1,'flow_mu180u86md3e:6:15004','15005','15006','Flow_1g1p7by',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-09-17 16:47:20.468','2026-09-17 16:47:20.468',0,2,NULL,''),('15009',1,'flow_mu180u86md3e:6:15004','15005','15006','UserTask_mu2c2p880',NULL,NULL,'3','manualTask',NULL,'2026-09-17 16:47:20.468','2026-09-17 16:47:20.468',0,3,NULL,''),('15011',1,'flow_mu180u86md3e:6:15004','15005','15006','Flow_1qxvbp7',NULL,NULL,'xiaoyuyibai','sequenceFlow',NULL,'2026-09-17 16:47:20.468','2026-09-17 16:47:20.468',0,4,NULL,''),('15012',1,'flow_mu180u86md3e:6:15004','15005','15010','Flow_0dizg8q',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-09-17 16:47:20.468','2026-09-17 16:47:20.468',0,5,NULL,''),('15013',1,'flow_mu180u86md3e:6:15004','15005','15006','Gateway_15cdzo6',NULL,NULL,NULL,'parallelGateway',NULL,'2026-09-17 16:47:20.468','2026-09-17 16:47:20.470',2,6,NULL,''),('15014',1,'flow_mu180u86md3e:6:15004','15005','15010','Gateway_0a6n8eh',NULL,NULL,NULL,'exclusiveGateway',NULL,'2026-09-17 16:47:20.470','2026-09-17 16:47:20.470',0,7,NULL,''),('15016',1,'flow_mu180u86md3e:6:15004','15005','15006','Flow_1dpq6dk',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-09-17 16:47:20.470','2026-09-17 16:47:20.470',0,8,NULL,''),('15017',1,'flow_mu180u86md3e:6:15004','15005','15015','Flow_0icqanz',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-09-17 16:47:20.470','2026-09-17 16:47:20.470',0,9,NULL,''),('15018',1,'flow_mu180u86md3e:6:15004','15005','15010','Flow_13gvmcu',NULL,NULL,NULL,'sequenceFlow',NULL,'2026-09-17 16:47:20.470','2026-09-17 16:47:20.470',0,10,NULL,''),('15019',1,'flow_mu180u86md3e:6:15004','15005','15006','Activity_0qm5tc6','15020',NULL,'1','userTask',NULL,'2026-09-17 16:47:20.470',NULL,NULL,11,NULL,''),('15021',1,'flow_mu180u86md3e:6:15004','15005','15015','Activity_1mzuxt6','15022',NULL,'2','userTask',NULL,'2026-09-17 16:47:20.483',NULL,NULL,12,NULL,''),('15023',1,'flow_mu180u86md3e:6:15004','15005','15010','Activity_02klewn','15024',NULL,'6','userTask',NULL,'2026-09-17 16:47:20.483',NULL,NULL,13,NULL,'');
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
INSERT INTO `act_ru_execution` VALUES ('15005',1,'15005','2064530495200337922:2100506880313933826',NULL,'flow_mu180u86md3e:6:15004',NULL,'15005',NULL,1,0,1,0,0,1,NULL,'',NULL,'StartEvent_mu2c2p840','2026-09-17 16:47:20.464',NULL,NULL,NULL,1,0,0,0,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL),('15006',1,'15005',NULL,'15005','flow_mu180u86md3e:6:15004',NULL,'15005','Activity_0qm5tc6',1,0,0,0,0,1,NULL,'',NULL,NULL,'2026-09-17 16:47:20.465',NULL,NULL,NULL,1,0,1,0,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL),('15010',1,'15005',NULL,'15005','flow_mu180u86md3e:6:15004',NULL,'15005','Activity_02klewn',1,0,0,0,0,1,NULL,'',NULL,NULL,'2026-09-17 16:47:20.468',NULL,NULL,NULL,1,0,1,0,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL),('15015',1,'15005',NULL,'15005','flow_mu180u86md3e:6:15004',NULL,'15005','Activity_1mzuxt6',1,0,0,0,0,1,NULL,'',NULL,NULL,'2026-09-17 16:47:20.470',NULL,NULL,NULL,1,0,1,0,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL);
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
INSERT INTO `act_ru_task` VALUES ('15020',1,'15006','15005','flow_mu180u86md3e:6:15004',NULL,NULL,NULL,NULL,NULL,NULL,'created','1',NULL,NULL,'Activity_0qm5tc6',NULL,NULL,NULL,50,'2026-09-17 08:47:20.470',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,'',NULL,1,0,0,0),('15022',1,'15015','15005','flow_mu180u86md3e:6:15004',NULL,NULL,NULL,NULL,NULL,NULL,'created','2',NULL,NULL,'Activity_1mzuxt6',NULL,NULL,NULL,50,'2026-09-17 08:47:20.483',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,'',NULL,1,0,0,0),('15024',1,'15010','15005','flow_mu180u86md3e:6:15004',NULL,NULL,NULL,NULL,NULL,NULL,'created','6',NULL,NULL,'Activity_02klewn',NULL,NULL,NULL,50,'2026-09-17 08:47:20.483',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,'',NULL,1,0,0,0);
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
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `inst_id` bigint unsigned NOT NULL COMMENT 'æµç¨‹å®ä¾‹ID',
  `task_id` bigint unsigned DEFAULT NULL COMMENT 'ä»»åŠ¡ID',
  `node_key` varchar(64) NOT NULL DEFAULT '' COMMENT 'èŠ‚ç‚¹Key',
  `operator` bigint NOT NULL COMMENT 'æ“ä½œäºº',
  `log_type` varchar(2) NOT NULL COMMENT '0æ‰¹å‡† 2æäº¤ 3é€€å› 7è½¬å‘ 9æ‰¹æ³¨ hè½¬åŠ sç£åŠ tæŠ„é€ yæ‰¹ç¤ºï¼ˆå¯¹é½ RequestLogTypeï¼‰',
  `opinion` varchar(2000) NOT NULL DEFAULT '' COMMENT 'å®¡æ‰¹æ„è§',
  `operate_time` datetime NOT NULL COMMENT 'æ“ä½œæ—¶é—´ï¼ˆåˆ†åŒºåˆ—ï¼‰',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€:1æ­£å¸¸ 0ç¦ç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`,`operate_time`),
  KEY `idx_inst_time` (`inst_id`,`operate_time`),
  KEY `idx_operator_time` (`operator`,`operate_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2100506882624995330 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å®¡æ‰¹æµè½¬è®°å½•'
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
INSERT INTO `wf_approval_log` VALUES (2100506880666255361,2100506880599146497,NULL,'StartEvent_mu2c2p840',2020463343744380930,'2','','2026-09-17 16:47:21','000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',1,0);
/*!40000 ALTER TABLE `wf_approval_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_custom_action`
--

DROP TABLE IF EXISTS `wf_custom_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_custom_action` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `action_name` varchar(200) NOT NULL COMMENT 'æ¥å£åŠ¨ä½œåç§°',
  `action_key` varchar(100) NOT NULL COMMENT 'æ¥å£åŠ¨ä½œæ ‡è¯†ï¼ˆå”¯ä¸€ï¼‰',
  `class_name` varchar(300) NOT NULL COMMENT 'æ¥å£åŠ¨ä½œç±»æ–‡ä»¶ï¼ˆç±»å…¨åï¼‰',
  `params_json` json DEFAULT NULL COMMENT 'å‚æ•°è®¾ç½®ï¼š[{name,value,isDataSource}]',
  `remark` varchar(500) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€:1æ­£å¸¸ 0ç¦ç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_action_key` (`action_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='è‡ªå®šä¹‰æ¥å£åŠ¨ä½œï¼ˆæ³¨å†Œè‡ªå®šä¹‰æ¥å£ï¼‰';
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
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `inst_id` bigint unsigned NOT NULL COMMENT 'æµç¨‹å®ä¾‹ID',
  `node_key` varchar(64) NOT NULL DEFAULT '' COMMENT 'èŠ‚ç‚¹Key',
  `layout_id` bigint DEFAULT NULL COMMENT 'å½“æ—¶çš„ form_layout.idï¼ˆå¸ƒå±€å¿«ç…§ï¼‰',
  `data_json` json NOT NULL COMMENT 'ä¸»è¡¨+æ˜ç»†å…¨é‡å€¼',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€:1æ­£å¸¸ 0ç¦ç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_inst_node` (`inst_id`,`node_key`),
  KEY `idx_inst_time` (`inst_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2100506880599146499 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='è¡¨å•æ•°æ®å¿«ç…§';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_form_snapshot`
--

LOCK TABLES `wf_form_snapshot` WRITE;
/*!40000 ALTER TABLE `wf_form_snapshot` DISABLE KEYS */;
INSERT INTO `wf_form_snapshot` VALUES (2100506880599146498,2100506880599146497,'StartEvent_mu2c2p840',NULL,'{}','000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',1,0);
/*!40000 ALTER TABLE `wf_form_snapshot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_instance`
--

DROP TABLE IF EXISTS `wf_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_instance` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `engine_inst_id` varchar(64) NOT NULL DEFAULT '' COMMENT 'å¼•æ“å®ä¾‹IDï¼ˆFlowable PROC_INST_ID_ï¼‰ï¼Œå¼±å…³è”ï¼Œä¸ä¾èµ– ACT_* è¡¨',
  `def_id` bigint unsigned NOT NULL COMMENT 'æµç¨‹å®šä¹‰ID',
  `form_id` bigint NOT NULL COMMENT 'è¡¨å•IDï¼ˆworkflow_bill.idï¼‰',
  `data_id` bigint NOT NULL COMMENT 'ä¸šåŠ¡æ•°æ®IDï¼ˆformtable_main_{id}.idï¼‰',
  `title` varchar(400) NOT NULL DEFAULT '' COMMENT 'æµç¨‹æ ‡é¢˜',
  `biz_key` varchar(128) NOT NULL COMMENT 'ä¸šåŠ¡ä¸»é”® formId:dataIdï¼Œä¾¿äºåæŸ¥',
  `current_node_key` varchar(64) NOT NULL DEFAULT '' COMMENT 'å½“å‰èŠ‚ç‚¹',
  `starter` bigint NOT NULL COMMENT 'å‘èµ·äºº',
  `start_time` datetime NOT NULL COMMENT 'å‘èµ·æ—¶é—´',
  `end_time` datetime DEFAULT NULL COMMENT 'ç»“æŸæ—¶é—´',
  `urgency` tinyint NOT NULL DEFAULT '0' COMMENT 'ç´§æ€¥ç¨‹åº¦ 0/1/2ï¼ˆå¯¹é½ requestlevelï¼‰',
  `parent_id` bigint unsigned DEFAULT NULL COMMENT 'çˆ¶æµç¨‹å®ä¾‹ï¼ˆå­æµç¨‹ï¼‰',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '0' COMMENT '0è¿è¡Œä¸­ 1é€šè¿‡ 2ä¸é€šè¿‡ 3æ’¤é”€ 4æš‚åœï¼ˆå¯¹é½ ecology currentstatus è¯­ä¹‰ï¼‰',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  `is_test` tinyint NOT NULL DEFAULT '0' COMMENT 'æµ‹è¯•æ€æ ‡è®° 1=æµ‹è¯•äº§ç”Ÿçš„å®ä¾‹/æ•°æ®',
  `test_deployment_id` varchar(64) DEFAULT NULL COMMENT 'æµ‹è¯•ä¸´æ—¶éƒ¨ç½²IDï¼ˆæ¸…ç†æ—¶çº§è”å¸è½½ï¼‰',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_engine_inst` (`engine_inst_id`),
  UNIQUE KEY `uk_biz_key` (`biz_key`),
  KEY `idx_status_start` (`status`,`start_time`),
  KEY `idx_starter_status` (`starter`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2100506880599146498 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æµç¨‹å®ä¾‹';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_instance`
--

LOCK TABLES `wf_instance` WRITE;
/*!40000 ALTER TABLE `wf_instance` DISABLE KEYS */;
INSERT INTO `wf_instance` VALUES (2100506880599146497,'15005',2099758062693871618,2064530495200337922,2100506880313933826,'ã€æµ‹è¯•ã€‘æµ‹è¯•914','2064530495200337922:2100506880313933826','Activity_0qm5tc6',2020463343744380930,'2026-09-17 16:47:21',NULL,0,NULL,'000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',0,0,1,'15001');
/*!40000 ALTER TABLE `wf_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_migration_map`
--

DROP TABLE IF EXISTS `wf_migration_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_migration_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `ec_wf_id` bigint NOT NULL COMMENT 'ecology workflow_base.id',
  `ec_request_id` bigint DEFAULT NULL COMMENT 'ecology workflow_requestbase.requestidï¼ˆåœ¨é€”å®ä¾‹ï¼‰',
  `ec_node_id` bigint DEFAULT NULL COMMENT 'ecology èŠ‚ç‚¹ID',
  `new_def_id` bigint unsigned DEFAULT NULL COMMENT 'æ–°æµç¨‹å®šä¹‰ID',
  `new_inst_id` bigint unsigned DEFAULT NULL COMMENT 'æ–°æµç¨‹å®ä¾‹ID',
  `new_node_key` varchar(64) DEFAULT NULL COMMENT 'æ–°èŠ‚ç‚¹Key',
  `migrate_time` datetime NOT NULL COMMENT 'è¿ç§»æ—¶é—´',
  `err_msg` varchar(1000) NOT NULL DEFAULT '' COMMENT 'é”™è¯¯ä¿¡æ¯',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '0' COMMENT '0å¾…è¿ç§» 1æˆåŠŸ 2å¤±è´¥',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ec_wf` (`ec_wf_id`,`ec_node_id`),
  KEY `idx_ec_request` (`ec_request_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ecology å­˜é‡è¿ç§»æ˜ å°„';
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
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `def_id` bigint unsigned NOT NULL COMMENT 'æµç¨‹å®šä¹‰ID',
  `node_key` varchar(64) NOT NULL COMMENT 'èŠ‚ç‚¹Key',
  `dt_index` int NOT NULL COMMENT 'æ˜ç»†è¡¨åºå·',
  `can_add` tinyint NOT NULL DEFAULT '1' COMMENT 'å¯æ–°å¢è¡Œ',
  `can_edit` tinyint NOT NULL DEFAULT '1' COMMENT 'å¯ç¼–è¾‘è¡Œ',
  `can_delete` tinyint NOT NULL DEFAULT '1' COMMENT 'å¯åˆ é™¤è¡Œ',
  `hide_empty` tinyint NOT NULL DEFAULT '0' COMMENT 'éšè—ç©ºè¡Œ',
  `default_rows` int NOT NULL DEFAULT '1' COMMENT 'é»˜è®¤è¡Œæ•°',
  `required` tinyint NOT NULL DEFAULT '0' COMMENT 'å¿…é¡»è‡³å°‘ä¸€æ¡',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€:1æ­£å¸¸ 0ç¦ç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_dt` (`def_id`,`node_key`,`dt_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='èŠ‚ç‚¹çº§æ˜ç»†è¡¨æƒé™';
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
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `def_id` bigint unsigned NOT NULL COMMENT 'æµç¨‹å®šä¹‰ID',
  `node_key` varchar(64) NOT NULL COMMENT 'èŠ‚ç‚¹Key',
  `scope` varchar(32) NOT NULL COMMENT 'main | dt{idx} | dt{idx}_r{row}ï¼ˆä¸ data-excelp-scope å¯¹é½ï¼‰',
  `field_name` varchar(128) NOT NULL COMMENT 'å­—æ®µåï¼ˆä¸ data-excelp-field å¯¹é½ï¼‰',
  `perm` tinyint NOT NULL DEFAULT '2' COMMENT '0éšè— 1åªè¯» 2å¯ç¼–è¾‘ 3å¿…å¡«ï¼ˆå¯¹é½ ecology fieldattrï¼‰',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€:1æ­£å¸¸ 0ç¦ç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_scope_field` (`def_id`,`node_key`,`scope`,`field_name`),
  KEY `idx_def_node` (`def_id`,`node_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='èŠ‚ç‚¹çº§å­—æ®µæƒé™çŸ©é˜µ';
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
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `def_id` bigint unsigned NOT NULL COMMENT 'æµç¨‹å®šä¹‰ID',
  `from_node_key` varchar(64) NOT NULL COMMENT 'æºèŠ‚ç‚¹',
  `to_node_key` varchar(64) NOT NULL COMMENT 'ç›®æ ‡èŠ‚ç‚¹',
  `is_reject` tinyint NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦é€€å›çº¿ï¼ˆå¯¹é½ ISREJECTï¼‰',
  `is_must_pass` tinyint NOT NULL DEFAULT '0' COMMENT 'åˆ†å‰å¿…ç»åˆ†æ”¯ï¼ˆå¯¹é½ ISMUSTPASSï¼‰',
  `condition_expr` text COMMENT 'æ¡ä»¶è¡¨è¾¾å¼',
  `condition_cn` varchar(1000) NOT NULL DEFAULT '' COMMENT 'æ¡ä»¶ä¸­æ–‡æè¿°',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT 'æ’åº',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€:1æ­£å¸¸ 0ç¦ç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  `via_gateway` tinyint NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦ç»ç”±ç½‘å…³æŠ˜å è€Œæ¥çš„é€»è¾‘è¿çº¿ 1=æ˜¯',
  `via_gateway_key` varchar(64) DEFAULT NULL COMMENT 'ç»ç”±çš„ç½‘å…³èŠ‚ç‚¹ keyï¼ˆAâ†’ç½‘å…³â†’B æŠ˜å ä¸º Aâ†’B æ—¶è®°å½•è¯¥ç½‘å…³ï¼Œä¾›ç½‘å…³èŠ‚ç‚¹å‘ˆç°å…¶ä¸‹æ¸¸åˆ†æ”¯ï¼‰',
  PRIMARY KEY (`id`),
  KEY `idx_def_from` (`def_id`,`from_node_key`),
  KEY `idx_def_reject` (`def_id`,`is_reject`)
) ENGINE=InnoDB AUTO_INCREMENT=2100741581804990466 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æµç¨‹å‡ºå£ï¼ˆè¿çº¿ï¼‰';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_node_link`
--

LOCK TABLES `wf_node_link` WRITE;
/*!40000 ALTER TABLE `wf_node_link` DISABLE KEYS */;
INSERT INTO `wf_node_link` VALUES (2099477068447293442,2099475845245640706,'StartEvent_mu18738g0','UserTask_mu18738m0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-14 20:35:14',NULL,'2026-09-14 20:35:25',1,1,0,NULL),(2099477068447293443,2099475845245640706,'UserTask_mu18738m0','EndEvent_mu18738u0',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-14 20:35:14',NULL,'2026-09-14 20:35:25',1,1,0,NULL),(2099477117248020482,2099475845245640706,'StartEvent_mu18738g0','EndEvent_mu18738u0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-14 20:35:25',NULL,'2026-09-15 08:28:28',1,1,0,NULL),(2099656559220019203,2099475845245640706,'StartEvent_mu18738g0','UserTask_mu1xob7r0',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-15 08:28:28',NULL,'2026-09-15 08:28:28',1,0,0,NULL),(2099656559220019204,2099475845245640706,'UserTask_mu1xob7r0','EndEvent_mu18738u0',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-15 08:28:28',NULL,'2026-09-15 10:35:46',1,1,0,NULL),(2099687772060176386,2099475845245640706,'UserTask_mu1xob7r0','Activity_1dw3jwq',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-15 10:32:29',NULL,'2026-09-15 10:33:25',1,1,0,NULL),(2099687772060176387,2099475845245640706,'Activity_1dw3jwq','EndEvent_mu18738u0',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 10:32:29',NULL,'2026-09-15 10:33:25',1,1,0,NULL),(2099688954895847427,2099475845245640706,'Activity_1m8w40y','EndEvent_mu18738u0',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 10:37:11',NULL,'2026-09-15 10:37:11',1,0,0,NULL),(2099688981668089859,2099475845245640706,'Activity_06no0fv','EndEvent_mu18738u0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 10:37:18',NULL,'2026-09-15 10:37:18',1,0,0,NULL),(2099757522001948673,2099475845245640706,'UserTask_mu1xob7r0','Activity_1m8w40y',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-15 15:09:39',NULL,'2026-09-15 15:09:39',1,0,0,NULL),(2099758002102956034,2099475894742622209,'UserTask_mu2c2p880','EndEvent_mu2c2p8c0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0,NULL),(2099758002102956035,2099475894742622209,'StartEvent_mu2c2p840','UserTask_mu2c2p880',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0,NULL),(2099758024894803973,2099758024894803969,'UserTask_mu2c2p880','EndEvent_mu2c2p8c0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0,NULL),(2099758024961912833,2099758024894803969,'StartEvent_mu2c2p840','UserTask_mu2c2p880',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0,NULL),(2099758062752591877,2099758062693871618,'UserTask_mu2c2p880','EndEvent_mu2c2p8c0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:56:47',1,1,0,NULL),(2099758062752591878,2099758062693871618,'StartEvent_mu2c2p840','UserTask_mu2c2p880',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-16 08:37:29',1,1,0,NULL),(2099769199200137218,2099758062693871618,'UserTask_mu2c2p880','Activity_0qm5tc6',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-15 15:56:03',NULL,'2026-09-16 08:37:29',1,1,1,NULL),(2099769329089343490,2099758062693871618,'UserTask_mu2c2p880','Activity_1mzuxt6',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 15:56:34',NULL,'2026-09-16 08:37:29',1,1,1,NULL),(2099769398794481666,2099758062693871618,'Activity_0qm5tc6','EndEvent_mu2c2p8c0',0,0,'${tes &gt; 100}','å½“ tes å¤§äº 100',8,'000000',NULL,NULL,'2026-09-15 15:56:51',NULL,'2026-09-16 19:54:00',1,0,0,NULL),(2099769425491226626,2099758062693871618,'Activity_1mzuxt6','EndEvent_mu2c2p8c0',0,0,NULL,'',12,'000000',NULL,NULL,'2026-09-15 15:56:57',NULL,'2026-09-15 15:56:57',1,0,0,NULL),(2099816461460652034,2099758062693871618,'UserTask_mu2c2p880','EndEvent_mu2c2p8c0',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 19:03:51',NULL,'2026-09-15 19:05:54',1,1,1,NULL),(2099816958225629187,2099758062693871618,'UserTask_mu2c2p880','Activity_02klewn',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-15 19:05:50',NULL,'2026-09-15 20:13:18',1,1,1,NULL),(2099816958225629188,2099758062693871618,'Activity_02klewn','EndEvent_mu2c2p8c0',0,0,NULL,'',9,'000000',NULL,NULL,'2026-09-15 19:05:50',NULL,'2026-09-15 19:05:50',1,0,0,NULL),(2099816975044788227,2099758062693871618,'Activity_1g4p700','EndEvent_mu2c2p8c0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 19:05:54',NULL,'2026-09-15 19:05:58',1,1,1,NULL),(2099816975044788228,2099758062693871618,'UserTask_mu2c2p880','Activity_1g4p700',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 19:05:54',NULL,'2026-09-15 20:13:18',1,1,1,NULL),(2099816992086245378,2099758062693871618,'Activity_1g4p700','Activity_0ylt45t',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 19:05:58',NULL,'2026-09-15 20:16:23',1,1,0,NULL),(2099816992086245379,2099758062693871618,'Activity_0ylt45t','EndEvent_mu2c2p8c0',0,0,NULL,'',13,'000000',NULL,NULL,'2026-09-15 19:05:58',NULL,'2026-09-15 19:05:58',1,0,0,NULL),(2099834008419516418,2099758062693871618,'UserTask_mu2c2p880','Activity_1g4p700',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 20:13:35',NULL,'2026-09-15 20:13:55',1,1,1,NULL),(2099834008419516419,2099758062693871618,'UserTask_mu2c2p880','Activity_02klewn',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-15 20:13:35',NULL,'2026-09-16 08:27:19',1,1,1,NULL),(2099834144151388161,2099758062693871618,'UserTask_mu2c2p880','Activity_1g4p700',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-15 20:14:07',NULL,'2026-09-16 08:27:19',1,1,1,NULL),(2099834773615755265,2099758062693871618,'Activity_1g4p700','Activity_0ylt45t',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-15 20:16:37',NULL,'2026-09-16 08:27:19',1,1,1,NULL),(2100018659545096196,2099758062693871618,'Activity_1g4p700','Event_0pivae0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 13:05:53',1,1,0,NULL),(2100018659545096197,2099758062693871618,'UserTask_mu2c2p880','Event_093p7s3',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 08:37:29',1,1,0,NULL),(2100018659595427842,2099758062693871618,'Event_093p7s3','Activity_02klewn',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-17 16:34:02',1,1,1,NULL),(2100018659595427843,2099758062693871618,'Event_093p7s3','Event_0ctoqxr',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-17 16:34:02',1,1,1,NULL),(2100018659595427844,2099758062693871618,'Event_0ctoqxr','Activity_1g4p700',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-16 13:05:53',1,1,0,NULL),(2100018659595427845,2099758062693871618,'Event_0pivae0','Activity_0ylt45t',0,0,NULL,'',9,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-17 16:34:06',1,1,0,NULL),(2100021218242502657,2099758062693871618,'StartEvent_mu2c2p840','Event_093p7s3',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-16 08:37:29',NULL,'2026-09-16 08:37:42',1,1,1,NULL),(2100021218242502658,2099758062693871618,'StartEvent_mu2c2p840','Activity_0qm5tc6',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-16 08:37:29',NULL,'2026-09-16 08:37:42',1,1,1,NULL),(2100021218242502659,2099758062693871618,'StartEvent_mu2c2p840','Activity_1mzuxt6',0,0,NULL,'',6,'000000',NULL,NULL,'2026-09-16 08:37:29',NULL,'2026-09-16 08:37:42',1,1,1,NULL),(2100021271568883714,2099758062693871618,'UserTask_mu2c2p880','Event_093p7s3',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-16 08:37:42',NULL,'2026-09-16 08:40:19',1,1,0,NULL),(2100021271568883715,2099758062693871618,'UserTask_mu2c2p880','Activity_0qm5tc6',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-16 08:37:42',NULL,'2026-09-16 08:40:19',1,1,1,NULL),(2100021271568883716,2099758062693871618,'UserTask_mu2c2p880','Activity_1mzuxt6',0,0,NULL,'',6,'000000',NULL,NULL,'2026-09-16 08:37:42',NULL,'2026-09-16 08:40:19',1,1,1,NULL),(2100021271568883717,2099758062693871618,'StartEvent_mu2c2p840','UserTask_mu2c2p880',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-16 08:37:42',NULL,'2026-09-16 08:40:19',1,1,0,NULL),(2100021930926055426,2099758062693871618,'StartEvent_mu2c2p840','Event_093p7s3',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-16 08:40:19',NULL,'2026-09-16 15:55:50',1,1,1,NULL),(2100021930926055427,2099758062693871618,'StartEvent_mu2c2p840','Activity_0qm5tc6',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-16 08:40:19',NULL,'2026-09-16 15:55:50',1,1,1,NULL),(2100021930926055428,2099758062693871618,'StartEvent_mu2c2p840','Activity_1mzuxt6',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-16 08:40:19',NULL,'2026-09-16 15:55:50',1,1,1,NULL),(2100085625609822211,2099758062693871618,'StartEvent_mu2c2p840','Event_1fty5wz',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-16 12:53:25',NULL,'2026-09-16 12:53:30',1,1,1,NULL),(2100088764643713025,2099758062693871618,'Event_0ctoqxr','Event_0pivae0',0,0,NULL,'',6,'000000',NULL,NULL,'2026-09-16 13:05:53',NULL,'2026-09-16 15:55:50',1,1,1,NULL),(2100131533609304065,2099758062693871618,'Activity_1g4p700','Event_0pivae0',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-16 15:55:50',NULL,'2026-09-16 19:34:46',1,1,0,NULL),(2100131533609304066,2099758062693871618,'UserTask_mu2c2p880','Event_093p7s3',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-16 15:55:50',NULL,'2026-09-17 16:34:02',1,1,0,NULL),(2100131533609304067,2099758062693871618,'UserTask_mu2c2p880','Activity_0qm5tc6',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-16 15:55:50',NULL,'2026-09-17 19:35:02',1,1,1,NULL),(2100131533609304068,2099758062693871618,'UserTask_mu2c2p880','Activity_1mzuxt6',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-16 15:55:50',NULL,'2026-09-17 19:35:02',1,1,1,NULL),(2100131533609304069,2099758062693871618,'StartEvent_mu2c2p840','UserTask_mu2c2p880',0,0,NULL,'',6,'000000',NULL,NULL,'2026-09-16 15:55:50',NULL,'2026-09-16 15:55:50',1,0,0,NULL),(2100131533663830018,2099758062693871618,'Event_0ctoqxr','Activity_1g4p700',0,0,NULL,'',8,'000000',NULL,NULL,'2026-09-16 15:55:50',NULL,'2026-09-16 19:34:46',1,1,0,NULL),(2100186629982453762,2099758062693871618,'Event_0ctoqxr','Event_0pivae0',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-16 19:34:46',NULL,'2026-09-16 19:55:27',1,1,0,NULL),(2100191851668635651,2099758062693871618,'Event_0ctoqxr','Activity_1jl42bt',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-16 19:55:31',NULL,'2026-09-16 20:33:04',1,1,0,NULL),(2100191863119085570,2099758062693871618,'Activity_1jl42bt','Event_0pivae0',0,0,NULL,'',13,'000000',NULL,NULL,'2026-09-16 19:55:34',NULL,'2026-09-16 20:33:04',1,1,0,NULL),(2100201301154529282,2099758062693871618,'Event_0ctoqxr','Event_0pivae0',0,0,NULL,'',6,'000000',NULL,NULL,'2026-09-16 20:33:04',NULL,'2026-09-17 16:34:05',1,1,1,NULL),(2100503535302004737,2099758062693871618,'UserTask_mu2c2p880','Activity_02klewn',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-17 16:34:02',NULL,'2026-09-17 20:14:43',1,1,1,NULL),(2100503535310393346,2099758062693871618,'UserTask_mu2c2p880','Event_0ctoqxr',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-17 16:34:02',NULL,'2026-09-17 16:34:05',1,1,1,NULL),(2100503545263476737,2099758062693871618,'UserTask_mu2c2p880','Event_0pivae0',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-17 16:34:05',NULL,'2026-09-17 16:34:06',1,1,1,NULL),(2100503549986263041,2099758062693871618,'UserTask_mu2c2p880','Activity_0ylt45t',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-17 16:34:06',NULL,'2026-09-17 20:13:55',1,1,1,NULL),(2100549103856439298,2099758062693871618,'UserTask_mu2c2p880','Activity_0qm5tc6',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-17 19:35:07',NULL,'2026-09-17 20:12:36',1,1,1,NULL),(2100549103856439299,2099758062693871618,'UserTask_mu2c2p880','Activity_1mzuxt6',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-17 19:35:07',NULL,'2026-09-17 20:12:36',1,1,1,NULL),(2100549293887770627,2099758062693871618,'Activity_02klewn','Activity_0vforb1',0,0,NULL,'',10,'000000',NULL,NULL,'2026-09-17 19:35:52',NULL,'2026-09-17 19:47:21',1,1,1,NULL),(2100549306437128193,2099758062693871618,'Activity_0vforb1','EndEvent_mu2c2p8c0',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-17 19:35:55',NULL,'2026-09-17 19:35:55',1,0,0,NULL),(2100549423093305346,2099758062693871618,'Activity_02klewn','Activity_154cox6',0,0,NULL,'',10,'000000',NULL,NULL,'2026-09-17 19:36:23',NULL,'2026-09-17 19:36:23',1,0,1,'Gateway_12o2q4i'),(2100549483243819009,2099758062693871618,'Activity_154cox6','EndEvent_mu2c2p8c0',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-17 19:36:37',NULL,'2026-09-17 19:36:37',1,0,0,NULL),(2100552194882338817,2099758062693871618,'Activity_02klewn','Activity_0vforb1',0,0,NULL,'',11,'000000',NULL,NULL,'2026-09-17 19:47:24',NULL,'2026-09-17 19:47:24',1,0,1,'Gateway_12o2q4i'),(2100558555330445313,2099758062693871618,'UserTask_mu2c2p880','Activity_0qm5tc6',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-17 20:12:40',NULL,'2026-09-17 20:12:40',1,0,1,'Gateway_15cdzo6'),(2100558555397554178,2099758062693871618,'UserTask_mu2c2p880','Activity_1mzuxt6',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-17 20:12:40',NULL,'2026-09-17 20:12:40',1,0,1,'Gateway_15cdzo6'),(2100558894666416129,2099758062693871618,'UserTask_mu2c2p880','Activity_0ylt45t',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-17 20:14:01',NULL,'2026-09-17 20:14:43',1,1,1,NULL),(2100559101441409026,2099758062693871618,'UserTask_mu2c2p880','Activity_0ylt45t',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-17 20:14:50',NULL,'2026-09-17 21:13:08',1,1,1,NULL),(2100559122123522050,2099758062693871618,'UserTask_mu2c2p880','Activity_02klewn',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-17 20:14:55',NULL,'2026-09-17 21:13:08',1,1,1,NULL),(2100573783094984706,2099758062693871618,'UserTask_mu2c2p880','Activity_0ylt45t',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-17 21:13:11',NULL,'2026-09-18 07:34:59',1,1,1,'Gateway_10vfybo'),(2100573783094984707,2099758062693871618,'UserTask_mu2c2p880','Activity_02klewn',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-17 21:13:11',NULL,'2026-09-18 07:34:59',1,1,1,'Gateway_10vfybo'),(2100730280407990274,2099758062693871618,'UserTask_mu2c2p880','Activity_0ylt45t',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-18 07:35:03',NULL,'2026-09-18 07:35:31',1,1,1,'Gateway_10vfybo'),(2100730280407990275,2099758062693871618,'UserTask_mu2c2p880','Activity_02klewn',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-18 07:35:03',NULL,'2026-09-18 07:35:31',1,1,1,'Gateway_10vfybo'),(2100730481256431617,2099758062693871618,'UserTask_mu2c2p880','Activity_0ylt45t',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-18 07:35:51',NULL,'2026-09-18 07:35:51',1,0,1,'Gateway_1qcbf1n'),(2100730502836125698,2099758062693871618,'UserTask_mu2c2p880','Activity_02klewn',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-18 07:35:56',NULL,'2026-09-18 07:35:56',1,0,1,'Gateway_1qcbf1n'),(2100741041675104259,2100740952047022081,'Event_1lb88j7','Activity_0rpzmot',0,0,NULL,'',1,'000000',NULL,NULL,'2026-09-18 08:17:48',NULL,'2026-09-18 08:17:48',1,0,0,NULL),(2100741054891356163,2100740952047022081,'Activity_0rpzmot','Gateway_00dz885',0,0,NULL,'',2,'000000',NULL,NULL,'2026-09-18 08:17:52',NULL,'2026-09-18 08:17:52',1,0,0,NULL),(2100741060624969729,2100740952047022081,'Gateway_00dz885','Activity_065wlfl',0,0,NULL,'',3,'000000',NULL,NULL,'2026-09-18 08:17:53',NULL,'2026-09-18 08:17:53',1,0,0,NULL),(2100741079688081411,2100740952047022081,'Gateway_00dz885','Activity_01qzg7i',0,0,NULL,'',4,'000000',NULL,NULL,'2026-09-18 08:17:57',NULL,'2026-09-18 08:17:57',1,0,0,NULL),(2100741270747017219,2100740952047022081,'Activity_065wlfl','Gateway_1qw9z2c',0,0,NULL,'',5,'000000',NULL,NULL,'2026-09-18 08:18:43',NULL,'2026-09-18 08:18:43',1,0,0,NULL),(2100741328838127618,2100740952047022081,'Gateway_1qw9z2c','Activity_0j79ocf',0,0,NULL,'',6,'000000',NULL,NULL,'2026-09-18 08:18:57',NULL,'2026-09-18 08:18:57',1,0,0,NULL),(2100741346408067074,2100740952047022081,'Activity_0j79ocf','Event_0urcupa',0,0,NULL,'',8,'000000',NULL,NULL,'2026-09-18 08:19:01',NULL,'2026-09-18 08:19:01',1,0,0,NULL),(2100741407393247234,2100740952047022081,'Activity_01qzg7i','Event_0urcupa',0,0,NULL,'',9,'000000',NULL,NULL,'2026-09-18 08:19:16',NULL,'2026-09-18 08:19:16',1,0,0,NULL),(2100741536376483842,2100740952047022081,'Gateway_1qw9z2c','Activity_0gfw7j5',0,0,NULL,'',7,'000000',NULL,NULL,'2026-09-18 08:19:46',NULL,'2026-09-18 08:19:46',1,0,0,NULL),(2100741581804990465,2100740952047022081,'Activity_0gfw7j5','Event_0urcupa',0,0,NULL,'',10,'000000',NULL,NULL,'2026-09-18 08:19:57',NULL,'2026-09-18 08:19:57',1,0,0,NULL);
/*!40000 ALTER TABLE `wf_node_link` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_node_operator`
--

DROP TABLE IF EXISTS `wf_node_operator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_node_operator` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `node_id` bigint unsigned NOT NULL COMMENT 'æµç¨‹èŠ‚ç‚¹ID',
  `group_no` int NOT NULL DEFAULT '1' COMMENT 'æ“ä½œç»„åºå·',
  `op_type` int NOT NULL COMMENT 'æ“ä½œè€…ç±»å‹ï¼ˆå¯¹é½ OperatorDBTypeï¼š3äººå‘˜ 1éƒ¨é—¨ 30åˆ†éƒ¨ 2è§’è‰² 58å²—ä½ 4æ‰€æœ‰äºº / 17åˆ›å»ºäººæœ¬äºº 18åˆ›å»ºäººä¸Šçº§ 19æœ¬éƒ¨é—¨ / 40æœ¬äºº 41ä¸Šçº§ / 5å­—æ®µ-äººå‘˜ 6å­—æ®µ-äººå‘˜ä¸Šçº§ 42å­—æ®µ-éƒ¨é—¨ 43å­—æ®µ-è§’è‰² / 99çŸ©é˜µ 97æ¥å£ 98SQLï¼‰',
  `obj_id` varchar(64) NOT NULL DEFAULT '' COMMENT 'å¯¹è±¡IDæˆ–è¡¨å•å­—æ®µå',
  `level_min` int DEFAULT NULL COMMENT 'å®‰å…¨çº§åˆ«ä¸‹é™ï¼ˆå¯¹é½ LEVEL_Nï¼‰',
  `level_max` int DEFAULT NULL COMMENT 'å®‰å…¨çº§åˆ«ä¸Šé™ï¼ˆå¯¹é½ LEVEL2_Nï¼‰',
  `bhxj` tinyint NOT NULL DEFAULT '0' COMMENT '0æœ¬éƒ¨ 1å«ä¸‹çº§ 2å«ä¸Šçº§ 3é€çº§å‘ä¸Šï¼ˆå¯¹é½ BHXJï¼‰',
  `sign_order` tinyint NOT NULL DEFAULT '0' COMMENT 'ä¼šç­¾å…³ç³»ï¼ˆå¯¹é½ SIGNORDERï¼‰',
  `batch_no` int NOT NULL DEFAULT '0' COMMENT 'æ‰¹æ¬¡ï¼ˆä¾æ¬¡å®¡æ‰¹é¡ºåºï¼Œå¯¹é½ ORDERSï¼‰',
  `condition_json` json DEFAULT NULL COMMENT 'æ“ä½œè€…ç”Ÿæ•ˆæ¡ä»¶',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€:1æ­£å¸¸ 0ç¦ç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_node_group` (`node_id`,`group_no`),
  KEY `idx_node_type` (`node_id`,`op_type`)
) ENGINE=InnoDB AUTO_INCREMENT=2100081416176967682 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='èŠ‚ç‚¹æ“ä½œè€…';
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
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `proc_key` varchar(64) NOT NULL COMMENT 'å¼•æ“æµç¨‹Keyï¼ˆ= BPMN process idï¼‰',
  `form_id` bigint NOT NULL COMMENT 'å…³è” workflow_bill.idï¼ˆè¡¨å•ï¼‰',
  `name` varchar(200) NOT NULL COMMENT 'æµç¨‹åç§°',
  `bpmn_xml` mediumtext COMMENT 'BPMN 2.0 æµç¨‹å®šä¹‰ XMLï¼ˆbpmn-js ç”»å¸ƒäº§å‡ºï¼Œéƒ¨ç½²æ—¶ä¸‹å‘å¼•æ“ï¼‰',
  `version` int NOT NULL DEFAULT '1' COMMENT 'ç‰ˆæœ¬å·',
  `active_version_id` bigint unsigned DEFAULT NULL COMMENT 'ç‰ˆæœ¬ç»„é”šç‚¹ï¼šæŒ‡å‘å½“å‰æ¿€æ´»ç‰ˆæœ¬çš„ defIdï¼›é¦–ç‰ˆ=è‡ªèº«idï¼ŒNULL=å•ç‰ˆæœ¬æµç¨‹ï¼ˆç»„=è‡ªèº«ï¼‰',
  `is_free` tinyint NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦è‡ªç”±æµç¨‹',
  `free_wf_type` tinyint DEFAULT NULL COMMENT 'è‡ªç”±æµç¨‹ç±»å‹ï¼š1ç®€æ˜“ 2é«˜çº§',
  `type` varchar(64) DEFAULT NULL COMMENT 'è·¯å¾„ç±»å‹ï¼ˆå¯¹é½ ecology path_type å­—å…¸ codeï¼‰',
  `form_type` tinyint DEFAULT NULL COMMENT 'å¯¹åº”è¡¨å•ç±»å‹ï¼š0è‡ªå®šä¹‰è¡¨å• 1ç³»ç»Ÿè¡¨å•',
  `description` varchar(500) DEFAULT NULL COMMENT 'è·¯å¾„æè¿°',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT 'æ˜¾ç¤ºé¡ºåº',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '0' COMMENT '0è‰ç¨¿ 1å·²å‘å¸ƒ 2åœç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤:1å·²åˆ  0æœªåˆ ',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_proc_key_version` (`proc_key`,`version`),
  KEY `idx_form_status` (`form_id`,`status`),
  KEY `idx_active_version` (`active_version_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2100740952047022082 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æµç¨‹å®šä¹‰';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_process_definition`
--

LOCK TABLES `wf_process_definition` WRITE;
/*!40000 ALTER TABLE `wf_process_definition` DISABLE KEYS */;
INSERT INTO `wf_process_definition` VALUES (2099475845245640706,'flow_mu180u86md3e',2064530495200337922,'æµ‹è¯•914','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"flow_mu180u86md3e\" name=\"æµ‹è¯•914\" isExecutable=\"true\">\n    <bpmn:startEvent id=\"StartEvent_mu18738g0\" name=\"å¼€å§‹\">\n      <bpmn:outgoing>Flow_0yenwcd</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:endEvent id=\"EndEvent_mu18738u0\" name=\"ç»“æŸ\">\n      <bpmn:incoming>Flow_1gxy9sv</bpmn:incoming>\n      <bpmn:incoming>Flow_1prutey</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:userTask id=\"UserTask_mu1xob7r0\" name=\"test\">\n      <bpmn:incoming>Flow_0yenwcd</bpmn:incoming>\n      <bpmn:outgoing>Flow_15soamo</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_0yenwcd\" sourceRef=\"StartEvent_mu18738g0\" targetRef=\"UserTask_mu1xob7r0\" />\n    <bpmn:sequenceFlow id=\"Flow_1gxy9sv\" sourceRef=\"Activity_06no0fv\" targetRef=\"EndEvent_mu18738u0\" />\n    <bpmn:sequenceFlow id=\"Flow_1prutey\" sourceRef=\"Activity_1m8w40y\" targetRef=\"EndEvent_mu18738u0\" />\n    <bpmn:userTask id=\"Activity_1m8w40y\" name=\"test2\">\n      <bpmn:incoming>Flow_15soamo</bpmn:incoming>\n      <bpmn:outgoing>Flow_1prutey</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:userTask id=\"Activity_06no0fv\" name=\"test1\">\n      <bpmn:outgoing>Flow_1gxy9sv</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_15soamo\" sourceRef=\"UserTask_mu1xob7r0\" targetRef=\"Activity_1m8w40y\" />\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"flow_mu180u86md3e\">\n      <bpmndi:BPMNShape id=\"StartEvent_mu18738g0_di\" bpmnElement=\"StartEvent_mu18738g0\">\n        <dc:Bounds x=\"142\" y=\"142\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"149\" y=\"185\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"EndEvent_mu18738u0_di\" bpmnElement=\"EndEvent_mu18738u0\">\n        <dc:Bounds x=\"852\" y=\"272\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"859\" y=\"248\" width=\"22\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"UserTask_mu1xob7r0_di\" bpmnElement=\"UserTask_mu1xob7r0\">\n        <dc:Bounds x=\"248\" y=\"102\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1bevfca_di\" bpmnElement=\"Activity_1m8w40y\">\n        <dc:Bounds x=\"460\" y=\"360\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1yvz5ju_di\" bpmnElement=\"Activity_06no0fv\">\n        <dc:Bounds x=\"590\" y=\"60\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_0yenwcd_di\" bpmnElement=\"Flow_0yenwcd\">\n        <di:waypoint x=\"178\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"142\" />\n        <di:waypoint x=\"248\" y=\"142\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1gxy9sv_di\" bpmnElement=\"Flow_1gxy9sv\">\n        <di:waypoint x=\"690\" y=\"100\" />\n        <di:waypoint x=\"771\" y=\"100\" />\n        <di:waypoint x=\"771\" y=\"290\" />\n        <di:waypoint x=\"852\" y=\"290\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1prutey_di\" bpmnElement=\"Flow_1prutey\">\n        <di:waypoint x=\"560\" y=\"400\" />\n        <di:waypoint x=\"706\" y=\"400\" />\n        <di:waypoint x=\"706\" y=\"290\" />\n        <di:waypoint x=\"852\" y=\"290\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_15soamo_di\" bpmnElement=\"Flow_15soamo\">\n        <di:waypoint x=\"348\" y=\"142\" />\n        <di:waypoint x=\"404\" y=\"142\" />\n        <di:waypoint x=\"404\" y=\"400\" />\n        <di:waypoint x=\"460\" y=\"400\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n',1,2099758062693871618,0,NULL,'2',0,NULL,0,'000000',NULL,NULL,'2026-09-14 20:30:22',NULL,'2026-09-14 20:30:22',0,0),(2099475894742622209,'flow_mu180u86md3e',2064530495200337922,'æµ‹è¯•914','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"flow_mu180u86md3e\" name=\"æµ‹è¯•914\" isExecutable=\"true\">\n    <bpmn:startEvent id=\"StartEvent_mu2c2p840\" name=\"å¼€å§‹\">\n      <bpmn:outgoing>Flow_1g1p7by</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:userTask id=\"UserTask_mu2c2p880\" name=\"1231\">\n      <bpmn:incoming>Flow_1g1p7by</bpmn:incoming>\n      <bpmn:outgoing>Flow_11qj1tr</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\" />\n    <bpmn:endEvent id=\"EndEvent_mu2c2p8c0\" name=\"ç»“æŸ\">\n      <bpmn:incoming>Flow_11qj1tr</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:sequenceFlow id=\"Flow_11qj1tr\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"EndEvent_mu2c2p8c0\" />\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"flow_mu180u86md3e\">\n      <bpmndi:BPMNShape id=\"StartEvent_mu2c2p840_di\" bpmnElement=\"StartEvent_mu2c2p840\">\n        <dc:Bounds x=\"142\" y=\"142\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"149\" y=\"185\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"UserTask_mu2c2p880_di\" bpmnElement=\"UserTask_mu2c2p880\">\n        <dc:Bounds x=\"248\" y=\"102\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"EndEvent_mu2c2p8c0_di\" bpmnElement=\"EndEvent_mu2c2p8c0\">\n        <dc:Bounds x=\"450\" y=\"84\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"457\" y=\"127\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_1g1p7by_di\" bpmnElement=\"Flow_1g1p7by\">\n        <di:waypoint x=\"178\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"142\" />\n        <di:waypoint x=\"248\" y=\"142\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_11qj1tr_di\" bpmnElement=\"Flow_11qj1tr\">\n        <di:waypoint x=\"348\" y=\"142\" />\n        <di:waypoint x=\"400\" y=\"142\" />\n        <di:waypoint x=\"400\" y=\"102\" />\n        <di:waypoint x=\"450\" y=\"102\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n',2,2099758062693871618,0,NULL,'2',0,NULL,0,'000000',NULL,NULL,'2026-09-14 20:30:34',NULL,'2026-09-14 20:30:34',0,0),(2099758024894803969,'flow_mu180u86md3e',2064530495200337922,'æµ‹è¯•914','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"flow_mu180u86md3e\" name=\"æµ‹è¯•914\" isExecutable=\"true\">\n    <bpmn:startEvent id=\"StartEvent_mu2c2p840\" name=\"å¼€å§‹\">\n      <bpmn:outgoing>Flow_1g1p7by</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:userTask id=\"UserTask_mu2c2p880\" name=\"1231\">\n      <bpmn:incoming>Flow_1g1p7by</bpmn:incoming>\n      <bpmn:outgoing>Flow_11qj1tr</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\" />\n    <bpmn:endEvent id=\"EndEvent_mu2c2p8c0\" name=\"ç»“æŸ\">\n      <bpmn:incoming>Flow_11qj1tr</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:sequenceFlow id=\"Flow_11qj1tr\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"EndEvent_mu2c2p8c0\" />\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"flow_mu180u86md3e\">\n      <bpmndi:BPMNShape id=\"StartEvent_mu2c2p840_di\" bpmnElement=\"StartEvent_mu2c2p840\">\n        <dc:Bounds x=\"142\" y=\"142\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"149\" y=\"185\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"UserTask_mu2c2p880_di\" bpmnElement=\"UserTask_mu2c2p880\">\n        <dc:Bounds x=\"248\" y=\"102\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"EndEvent_mu2c2p8c0_di\" bpmnElement=\"EndEvent_mu2c2p8c0\">\n        <dc:Bounds x=\"450\" y=\"84\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"457\" y=\"127\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_1g1p7by_di\" bpmnElement=\"Flow_1g1p7by\">\n        <di:waypoint x=\"178\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"142\" />\n        <di:waypoint x=\"248\" y=\"142\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_11qj1tr_di\" bpmnElement=\"Flow_11qj1tr\">\n        <di:waypoint x=\"348\" y=\"142\" />\n        <di:waypoint x=\"400\" y=\"142\" />\n        <di:waypoint x=\"400\" y=\"102\" />\n        <di:waypoint x=\"450\" y=\"102\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n',3,2099758062693871618,0,NULL,'2',0,NULL,0,'000000',NULL,NULL,'2026-09-15 15:11:39',NULL,'2026-09-15 15:11:39',0,0),(2099758062693871618,'flow_mu180u86md3e',2064530495200337922,'æµ‹è¯•914','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"flow_mu180u86md3e\" name=\"æµ‹è¯•914\" isExecutable=\"true\">\n    <bpmn:extensionElements>\n      <camunda:executionListener class=\"\" event=\"start\" />\n    </bpmn:extensionElements>\n    <bpmn:startEvent id=\"StartEvent_mu2c2p840\" name=\"å¼€å§‹\">\n      <bpmn:outgoing>Flow_1g1p7by</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:sequenceFlow id=\"Flow_1g1p7by\" sourceRef=\"StartEvent_mu2c2p840\" targetRef=\"UserTask_mu2c2p880\" />\n    <bpmn:endEvent id=\"EndEvent_mu2c2p8c0\" name=\"ç»“æŸ\">\n      <bpmn:extensionElements>\n        <camunda:properties>\n          <camunda:property />\n        </camunda:properties>\n      </bpmn:extensionElements>\n      <bpmn:incoming>Flow_1aqie0c</bpmn:incoming>\n      <bpmn:incoming>Flow_016it02</bpmn:incoming>\n      <bpmn:incoming>Flow_1kkbmin</bpmn:incoming>\n      <bpmn:incoming>Flow_0t3saos</bpmn:incoming>\n      <bpmn:incoming>Flow_1q30sui</bpmn:incoming>\n      <bpmn:incoming>Flow_132zhl0</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:sequenceFlow id=\"Flow_1dpq6dk\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_0qm5tc6\" />\n    <bpmn:userTask id=\"Activity_0qm5tc6\" name=\"1\">\n      <bpmn:incoming>Flow_1dpq6dk</bpmn:incoming>\n      <bpmn:outgoing>Flow_1aqie0c</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_0icqanz\" sourceRef=\"Gateway_15cdzo6\" targetRef=\"Activity_1mzuxt6\" />\n    <bpmn:parallelGateway id=\"Gateway_15cdzo6\">\n      <bpmn:incoming>Flow_1vki9ls</bpmn:incoming>\n      <bpmn:outgoing>Flow_1dpq6dk</bpmn:outgoing>\n      <bpmn:outgoing>Flow_0icqanz</bpmn:outgoing>\n    </bpmn:parallelGateway>\n    <bpmn:userTask id=\"Activity_1mzuxt6\" name=\"2\">\n      <bpmn:incoming>Flow_0icqanz</bpmn:incoming>\n      <bpmn:outgoing>Flow_016it02</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_1aqie0c\" sourceRef=\"Activity_0qm5tc6\" targetRef=\"EndEvent_mu2c2p8c0\" />\n    <bpmn:sequenceFlow id=\"Flow_016it02\" sourceRef=\"Activity_1mzuxt6\" targetRef=\"EndEvent_mu2c2p8c0\" />\n    <bpmn:sequenceFlow id=\"Flow_1kkbmin\" sourceRef=\"Activity_02klewn\" targetRef=\"EndEvent_mu2c2p8c0\" />\n    <bpmn:sequenceFlow id=\"Flow_0t3saos\" sourceRef=\"Activity_0ylt45t\" targetRef=\"EndEvent_mu2c2p8c0\" />\n    <bpmn:userTask id=\"Activity_02klewn\" name=\"6\">\n      <bpmn:incoming>Flow_1y16bhc</bpmn:incoming>\n      <bpmn:outgoing>Flow_1kkbmin</bpmn:outgoing>\n      <bpmn:outgoing>Flow_0oded29</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:userTask id=\"Activity_0ylt45t\" name=\"5\">\n      <bpmn:incoming>Flow_0uooqhg</bpmn:incoming>\n      <bpmn:outgoing>Flow_0t3saos</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:exclusiveGateway id=\"Gateway_12o2q4i\">\n      <bpmn:incoming>Flow_0oded29</bpmn:incoming>\n      <bpmn:outgoing>Flow_1a0pu4d</bpmn:outgoing>\n      <bpmn:outgoing>Flow_105ma25</bpmn:outgoing>\n    </bpmn:exclusiveGateway>\n    <bpmn:sequenceFlow id=\"Flow_0oded29\" sourceRef=\"Activity_02klewn\" targetRef=\"Gateway_12o2q4i\" />\n    <bpmn:sequenceFlow id=\"Flow_1q30sui\" sourceRef=\"Activity_0vforb1\" targetRef=\"EndEvent_mu2c2p8c0\" />\n    <bpmn:userTask id=\"Activity_0vforb1\" name=\"9\">\n      <bpmn:incoming>Flow_105ma25</bpmn:incoming>\n      <bpmn:outgoing>Flow_1q30sui</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_1a0pu4d\" sourceRef=\"Gateway_12o2q4i\" targetRef=\"Activity_154cox6\" />\n    <bpmn:userTask id=\"Activity_154cox6\" name=\"10\">\n      <bpmn:incoming>Flow_1a0pu4d</bpmn:incoming>\n      <bpmn:outgoing>Flow_132zhl0</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_132zhl0\" sourceRef=\"Activity_154cox6\" targetRef=\"EndEvent_mu2c2p8c0\" />\n    <bpmn:sequenceFlow id=\"Flow_105ma25\" sourceRef=\"Gateway_12o2q4i\" targetRef=\"Activity_0vforb1\" />\n    <bpmn:sequenceFlow id=\"Flow_1vki9ls\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_15cdzo6\" />\n    <bpmn:userTask id=\"UserTask_mu2c2p880\" name=\"3\">\n      <bpmn:incoming>Flow_1g1p7by</bpmn:incoming>\n      <bpmn:outgoing>Flow_1vki9ls</bpmn:outgoing>\n      <bpmn:outgoing>Flow_0qw546z</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:exclusiveGateway id=\"Gateway_1qcbf1n\">\n      <bpmn:incoming>Flow_0qw546z</bpmn:incoming>\n      <bpmn:outgoing>Flow_0uooqhg</bpmn:outgoing>\n      <bpmn:outgoing>Flow_1y16bhc</bpmn:outgoing>\n    </bpmn:exclusiveGateway>\n    <bpmn:sequenceFlow id=\"Flow_0qw546z\" sourceRef=\"UserTask_mu2c2p880\" targetRef=\"Gateway_1qcbf1n\" />\n    <bpmn:sequenceFlow id=\"Flow_0uooqhg\" sourceRef=\"Gateway_1qcbf1n\" targetRef=\"Activity_0ylt45t\" />\n    <bpmn:sequenceFlow id=\"Flow_1y16bhc\" sourceRef=\"Gateway_1qcbf1n\" targetRef=\"Activity_02klewn\" />\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"flow_mu180u86md3e\">\n      <bpmndi:BPMNShape id=\"StartEvent_mu2c2p840_di\" bpmnElement=\"StartEvent_mu2c2p840\">\n        <dc:Bounds x=\"142\" y=\"142\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"149\" y=\"185\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"EndEvent_mu2c2p8c0_di\" bpmnElement=\"EndEvent_mu2c2p8c0\">\n        <dc:Bounds x=\"812\" y=\"382\" width=\"36\" height=\"36\" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x=\"778.5\" y=\"393\" width=\"23\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_10ime3q_di\" bpmnElement=\"Activity_0qm5tc6\">\n        <dc:Bounds x=\"780\" y=\"90\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Gateway_1u6p3ea_di\" bpmnElement=\"Gateway_15cdzo6\">\n        <dc:Bounds x=\"545\" y=\"75\" width=\"50\" height=\"50\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_0iy2z00_di\" bpmnElement=\"Activity_1mzuxt6\">\n        <dc:Bounds x=\"631\" y=\"170\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_12i5vwr_di\" bpmnElement=\"Activity_02klewn\">\n        <dc:Bounds x=\"200\" y=\"500\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1262tz2_di\" bpmnElement=\"Activity_0ylt45t\">\n        <dc:Bounds x=\"610\" y=\"340\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Gateway_12o2q4i_di\" bpmnElement=\"Gateway_12o2q4i\" isMarkerVisible=\"true\">\n        <dc:Bounds x=\"215\" y=\"625\" width=\"50\" height=\"50\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_08cisdy_di\" bpmnElement=\"Activity_0vforb1\">\n        <dc:Bounds x=\"320\" y=\"610\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_0x1ckta_di\" bpmnElement=\"Activity_154cox6\">\n        <dc:Bounds x=\"320\" y=\"720\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1pfo8j7_di\" bpmnElement=\"UserTask_mu2c2p880\">\n        <dc:Bounds x=\"248\" y=\"102\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Gateway_1qcbf1n_di\" bpmnElement=\"Gateway_1qcbf1n\" isMarkerVisible=\"true\">\n        <dc:Bounds x=\"265\" y=\"255\" width=\"50\" height=\"50\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_1g1p7by_di\" bpmnElement=\"Flow_1g1p7by\">\n        <di:waypoint x=\"178\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"160\" />\n        <di:waypoint x=\"210\" y=\"142\" />\n        <di:waypoint x=\"248\" y=\"142\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1dpq6dk_di\" bpmnElement=\"Flow_1dpq6dk\">\n        <di:waypoint x=\"592\" y=\"97\" />\n        <di:waypoint x=\"720\" y=\"80\" />\n        <di:waypoint x=\"780\" y=\"108\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0icqanz_di\" bpmnElement=\"Flow_0icqanz\">\n        <di:waypoint x=\"570\" y=\"125\" />\n        <di:waypoint x=\"570\" y=\"210\" />\n        <di:waypoint x=\"631\" y=\"210\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1aqie0c_di\" bpmnElement=\"Flow_1aqie0c\">\n        <di:waypoint x=\"880\" y=\"130\" />\n        <di:waypoint x=\"1011\" y=\"130\" />\n        <di:waypoint x=\"1011\" y=\"400\" />\n        <di:waypoint x=\"848\" y=\"400\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_016it02_di\" bpmnElement=\"Flow_016it02\">\n        <di:waypoint x=\"731\" y=\"210\" />\n        <di:waypoint x=\"830\" y=\"210\" />\n        <di:waypoint x=\"830\" y=\"382\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1kkbmin_di\" bpmnElement=\"Flow_1kkbmin\">\n        <di:waypoint x=\"300\" y=\"540\" />\n        <di:waypoint x=\"830\" y=\"540\" />\n        <di:waypoint x=\"830\" y=\"418\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0t3saos_di\" bpmnElement=\"Flow_0t3saos\">\n        <di:waypoint x=\"710\" y=\"400\" />\n        <di:waypoint x=\"812\" y=\"400\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0oded29_di\" bpmnElement=\"Flow_0oded29\">\n        <di:waypoint x=\"250\" y=\"580\" />\n        <di:waypoint x=\"250\" y=\"603\" />\n        <di:waypoint x=\"240\" y=\"603\" />\n        <di:waypoint x=\"240\" y=\"625\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1q30sui_di\" bpmnElement=\"Flow_1q30sui\">\n        <di:waypoint x=\"420\" y=\"650\" />\n        <di:waypoint x=\"830\" y=\"650\" />\n        <di:waypoint x=\"830\" y=\"418\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1a0pu4d_di\" bpmnElement=\"Flow_1a0pu4d\">\n        <di:waypoint x=\"240\" y=\"675\" />\n        <di:waypoint x=\"240\" y=\"760\" />\n        <di:waypoint x=\"320\" y=\"760\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_132zhl0_di\" bpmnElement=\"Flow_132zhl0\">\n        <di:waypoint x=\"420\" y=\"760\" />\n        <di:waypoint x=\"830\" y=\"760\" />\n        <di:waypoint x=\"830\" y=\"418\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_105ma25_di\" bpmnElement=\"Flow_105ma25\">\n        <di:waypoint x=\"265\" y=\"650\" />\n        <di:waypoint x=\"320\" y=\"650\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1vki9ls_di\" bpmnElement=\"Flow_1vki9ls\">\n        <di:waypoint x=\"348\" y=\"142\" />\n        <di:waypoint x=\"447\" y=\"142\" />\n        <di:waypoint x=\"447\" y=\"100\" />\n        <di:waypoint x=\"545\" y=\"100\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0qw546z_di\" bpmnElement=\"Flow_0qw546z\">\n        <di:waypoint x=\"298\" y=\"182\" />\n        <di:waypoint x=\"298\" y=\"219\" />\n        <di:waypoint x=\"290\" y=\"219\" />\n        <di:waypoint x=\"290\" y=\"255\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0uooqhg_di\" bpmnElement=\"Flow_0uooqhg\">\n        <di:waypoint x=\"290\" y=\"305\" />\n        <di:waypoint x=\"290\" y=\"380\" />\n        <di:waypoint x=\"610\" y=\"380\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1y16bhc_di\" bpmnElement=\"Flow_1y16bhc\">\n        <di:waypoint x=\"290\" y=\"305\" />\n        <di:waypoint x=\"290\" y=\"403\" />\n        <di:waypoint x=\"250\" y=\"403\" />\n        <di:waypoint x=\"250\" y=\"500\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n',4,2099758062693871618,0,NULL,'2',0,NULL,0,'000000',NULL,NULL,'2026-09-15 15:11:48',NULL,'2026-09-15 15:11:48',0,0),(2100740952047022081,'flow_mu67lpidihle',2061742319431884801,'æµ‹è¯•918','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"flow_mu67lpidihle\" name=\"æµ‹è¯•918\" isExecutable=\"true\">\n    <bpmn:startEvent id=\"Event_1lb88j7\">\n      <bpmn:outgoing>Flow_1fcnjpa</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:sequenceFlow id=\"Flow_1fcnjpa\" sourceRef=\"Event_1lb88j7\" targetRef=\"Activity_0rpzmot\" />\n    <bpmn:sequenceFlow id=\"Flow_1qdcx8e\" sourceRef=\"Activity_0rpzmot\" targetRef=\"Gateway_00dz885\" />\n    <bpmn:sequenceFlow id=\"Flow_1kuqor9\" sourceRef=\"Gateway_00dz885\" targetRef=\"Activity_065wlfl\" />\n    <bpmn:sequenceFlow id=\"Flow_165hphx\" sourceRef=\"Gateway_00dz885\" targetRef=\"Activity_01qzg7i\" />\n    <bpmn:parallelGateway id=\"Gateway_00dz885\">\n      <bpmn:incoming>Flow_1qdcx8e</bpmn:incoming>\n      <bpmn:outgoing>Flow_1kuqor9</bpmn:outgoing>\n      <bpmn:outgoing>Flow_165hphx</bpmn:outgoing>\n    </bpmn:parallelGateway>\n    <bpmn:userTask id=\"Activity_0rpzmot\" name=\"1\">\n      <bpmn:incoming>Flow_1fcnjpa</bpmn:incoming>\n      <bpmn:outgoing>Flow_1qdcx8e</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:userTask id=\"Activity_065wlfl\" name=\"2\">\n      <bpmn:incoming>Flow_1kuqor9</bpmn:incoming>\n      <bpmn:outgoing>Flow_1s9bfd1</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:userTask id=\"Activity_01qzg7i\" name=\"3\">\n      <bpmn:incoming>Flow_165hphx</bpmn:incoming>\n      <bpmn:outgoing>Flow_1g4en21</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_1s9bfd1\" sourceRef=\"Activity_065wlfl\" targetRef=\"Gateway_1qw9z2c\" />\n    <bpmn:complexGateway id=\"Gateway_1qw9z2c\">\n      <bpmn:incoming>Flow_1s9bfd1</bpmn:incoming>\n      <bpmn:outgoing>Flow_0qvcunp</bpmn:outgoing>\n      <bpmn:outgoing>Flow_197fn3b</bpmn:outgoing>\n    </bpmn:complexGateway>\n    <bpmn:sequenceFlow id=\"Flow_0qvcunp\" sourceRef=\"Gateway_1qw9z2c\" targetRef=\"Activity_0j79ocf\" />\n    <bpmn:endEvent id=\"Event_0urcupa\">\n      <bpmn:incoming>Flow_0ia0vzl</bpmn:incoming>\n      <bpmn:incoming>Flow_1g4en21</bpmn:incoming>\n      <bpmn:incoming>Flow_1w4jgy0</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:sequenceFlow id=\"Flow_0ia0vzl\" sourceRef=\"Activity_0j79ocf\" targetRef=\"Event_0urcupa\" />\n    <bpmn:userTask id=\"Activity_0j79ocf\" name=\"4\">\n      <bpmn:incoming>Flow_0qvcunp</bpmn:incoming>\n      <bpmn:outgoing>Flow_0ia0vzl</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id=\"Flow_1g4en21\" sourceRef=\"Activity_01qzg7i\" targetRef=\"Event_0urcupa\" />\n    <bpmn:sequenceFlow id=\"Flow_197fn3b\" sourceRef=\"Gateway_1qw9z2c\" targetRef=\"Activity_0gfw7j5\" />\n    <bpmn:sequenceFlow id=\"Flow_1w4jgy0\" sourceRef=\"Activity_0gfw7j5\" targetRef=\"Event_0urcupa\" />\n    <bpmn:userTask id=\"Activity_0gfw7j5\" name=\"5\">\n      <bpmn:incoming>Flow_197fn3b</bpmn:incoming>\n      <bpmn:outgoing>Flow_1w4jgy0</bpmn:outgoing>\n    </bpmn:userTask>\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"flow_mu67lpidihle\">\n      <bpmndi:BPMNShape id=\"Event_1lb88j7_di\" bpmnElement=\"Event_1lb88j7\">\n        <dc:Bounds x=\"212\" y=\"172\" width=\"36\" height=\"36\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Gateway_0va1wek_di\" bpmnElement=\"Gateway_00dz885\">\n        <dc:Bounds x=\"455\" y=\"165\" width=\"50\" height=\"50\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_0m8ebmc_di\" bpmnElement=\"Activity_0rpzmot\">\n        <dc:Bounds x=\"300\" y=\"150\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1s6bk10_di\" bpmnElement=\"Activity_065wlfl\">\n        <dc:Bounds x=\"560\" y=\"150\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_18apcwa_di\" bpmnElement=\"Activity_01qzg7i\">\n        <dc:Bounds x=\"560\" y=\"260\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Gateway_081rsqq_di\" bpmnElement=\"Gateway_1qw9z2c\">\n        <dc:Bounds x=\"715\" y=\"165\" width=\"50\" height=\"50\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Event_0urcupa_di\" bpmnElement=\"Event_0urcupa\">\n        <dc:Bounds x=\"982\" y=\"172\" width=\"36\" height=\"36\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1jc4nam_di\" bpmnElement=\"Activity_0j79ocf\">\n        <dc:Bounds x=\"820\" y=\"150\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"Activity_1r0tfaz_di\" bpmnElement=\"Activity_0gfw7j5\">\n        <dc:Bounds x=\"820\" y=\"40\" width=\"100\" height=\"80\" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"Flow_1fcnjpa_di\" bpmnElement=\"Flow_1fcnjpa\">\n        <di:waypoint x=\"248\" y=\"190\" />\n        <di:waypoint x=\"300\" y=\"190\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1qdcx8e_di\" bpmnElement=\"Flow_1qdcx8e\">\n        <di:waypoint x=\"400\" y=\"190\" />\n        <di:waypoint x=\"455\" y=\"190\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1kuqor9_di\" bpmnElement=\"Flow_1kuqor9\">\n        <di:waypoint x=\"505\" y=\"190\" />\n        <di:waypoint x=\"560\" y=\"190\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_165hphx_di\" bpmnElement=\"Flow_165hphx\">\n        <di:waypoint x=\"480\" y=\"215\" />\n        <di:waypoint x=\"480\" y=\"300\" />\n        <di:waypoint x=\"560\" y=\"300\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1s9bfd1_di\" bpmnElement=\"Flow_1s9bfd1\">\n        <di:waypoint x=\"660\" y=\"190\" />\n        <di:waypoint x=\"715\" y=\"190\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0qvcunp_di\" bpmnElement=\"Flow_0qvcunp\">\n        <di:waypoint x=\"765\" y=\"190\" />\n        <di:waypoint x=\"820\" y=\"190\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_0ia0vzl_di\" bpmnElement=\"Flow_0ia0vzl\">\n        <di:waypoint x=\"920\" y=\"190\" />\n        <di:waypoint x=\"982\" y=\"190\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1g4en21_di\" bpmnElement=\"Flow_1g4en21\">\n        <di:waypoint x=\"660\" y=\"300\" />\n        <di:waypoint x=\"1000\" y=\"300\" />\n        <di:waypoint x=\"1000\" y=\"208\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_197fn3b_di\" bpmnElement=\"Flow_197fn3b\">\n        <di:waypoint x=\"740\" y=\"165\" />\n        <di:waypoint x=\"740\" y=\"80\" />\n        <di:waypoint x=\"820\" y=\"80\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1w4jgy0_di\" bpmnElement=\"Flow_1w4jgy0\">\n        <di:waypoint x=\"920\" y=\"80\" />\n        <di:waypoint x=\"951\" y=\"80\" />\n        <di:waypoint x=\"951\" y=\"190\" />\n        <di:waypoint x=\"982\" y=\"190\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n',1,2100740952047022081,0,NULL,'1',0,NULL,0,'000000',NULL,NULL,'2026-09-18 08:17:27',NULL,'2026-09-18 08:17:27',0,0);
/*!40000 ALTER TABLE `wf_process_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_process_node`
--

DROP TABLE IF EXISTS `wf_process_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_process_node` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `def_id` bigint unsigned NOT NULL COMMENT 'æµç¨‹å®šä¹‰ID',
  `node_key` varchar(64) NOT NULL COMMENT 'å¼•æ“èŠ‚ç‚¹ID',
  `node_name` varchar(200) NOT NULL COMMENT 'èŠ‚ç‚¹åç§°',
  `node_type` tinyint NOT NULL COMMENT '0åˆ›å»º 1å®¡æ‰¹ 2æäº¤ 3å½’æ¡£ 5ç­‰å¾… 6è‡ªåŠ¨å¤„ç†ï¼ˆå¯¹é½ ecology NodeTypeï¼‰',
  `sign_order` tinyint NOT NULL DEFAULT '0' COMMENT '0æˆ–ç­¾ 1ä¼šç­¾ 2ä¾æ¬¡ 3æŠ„é€ä¸éœ€æäº¤ 4æŠ„é€éœ€æäº¤ï¼ˆå¯¹é½ SignOrderï¼‰',
  `merge_type` tinyint NOT NULL DEFAULT '0' COMMENT '0æ™®é€š 1åˆ†å‰èµ·ç‚¹ 2åˆ†å‰ä¸­é—´ 3æŒ‰åˆ†æ”¯æ•°åˆå¹¶ 4æŒ‡å®šåˆ†æ”¯åˆå¹¶ 5æ¯”ä¾‹åˆå¹¶ï¼ˆå¯¹é½ nodeattributeï¼‰',
  `pass_num` int NOT NULL DEFAULT '0' COMMENT 'åˆå¹¶é˜ˆå€¼ï¼šåˆ†æ”¯æ•°æˆ–ç™¾åˆ†æ¯”ï¼ˆå¯¹é½ passnumï¼‰',
  `allow_reject` tinyint NOT NULL DEFAULT '1' COMMENT 'å…è®¸é€€å›',
  `allow_forward` tinyint NOT NULL DEFAULT '0' COMMENT 'å…è®¸è½¬å‘/è½¬åŠ',
  `auto_approve` tinyint NOT NULL DEFAULT '0' COMMENT 'è‡ªåŠ¨æ‰¹å‡†',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT 'æ’åº',
  `ext_json` json DEFAULT NULL COMMENT 'æ‰©å±•å±æ€§ï¼ˆè¶…æ—¶ã€æé†’ã€ç­¾ç« ç­‰ï¼‰',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€:1æ­£å¸¸ 0ç¦ç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  `test_status` tinyint NOT NULL DEFAULT '0' COMMENT 'èŠ‚ç‚¹æµ‹è¯•çŠ¶æ€ 0æœªæµ‹è¯• 1æµ‹è¯•é€šè¿‡ 2æµ‹è¯•æœªé€šè¿‡',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_def_node` (`def_id`,`node_key`),
  KEY `idx_def_sort` (`def_id`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=2100741536309374979 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æµç¨‹èŠ‚ç‚¹';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_process_node`
--

LOCK TABLES `wf_process_node` WRITE;
/*!40000 ALTER TABLE `wf_process_node` DISABLE KEYS */;
INSERT INTO `wf_process_node` VALUES (2099477068380184577,2099475845245640706,'StartEvent_mu18738g0','å¼€å§‹',0,0,0,0,1,0,0,1,NULL,'000000',NULL,NULL,'2026-09-14 20:35:14',NULL,'2026-09-14 20:35:14',1,0,1),(2099477068380184578,2099475845245640706,'UserTask_mu18738m0','1213',1,0,0,0,1,0,0,2,'{\"settings\": {}}','000000',NULL,NULL,'2026-09-14 20:35:14',NULL,'2026-09-14 20:35:25',1,1,0),(2099477068380184579,2099475845245640706,'EndEvent_mu18738u0','ç»“æŸ',3,0,0,0,1,0,0,5,NULL,'000000',NULL,NULL,'2026-09-14 20:35:14',NULL,'2026-09-14 20:35:14',1,0,1),(2099656559220019202,2099475845245640706,'UserTask_mu1xob7r0','test',1,0,0,0,1,0,0,2,'{\"settings\": {}}','000000',NULL,NULL,'2026-09-15 08:28:28',NULL,'2026-09-15 08:28:28',1,0,2),(2099687772060176385,2099475845245640706,'Activity_1dw3jwq','Activity_1dw3jwq',1,0,0,0,1,0,0,5,NULL,'000000',NULL,NULL,'2026-09-15 10:32:29',NULL,'2026-09-15 10:33:24',1,1,0),(2099688954895847426,2099475845245640706,'Activity_1m8w40y','test2',1,0,0,0,1,0,0,3,NULL,'000000',NULL,NULL,'2026-09-15 10:37:11',NULL,'2026-09-15 10:37:11',1,0,2),(2099688981668089858,2099475845245640706,'Activity_06no0fv','test1',1,0,0,0,1,0,0,4,NULL,'000000',NULL,NULL,'2026-09-15 10:37:18',NULL,'2026-09-15 10:37:18',1,0,0),(2099758002035847169,2099475894742622209,'StartEvent_mu2c2p840','å¼€å§‹',0,0,0,0,1,0,0,1,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758002035847170,2099475894742622209,'UserTask_mu2c2p880','1231',1,0,0,0,1,0,0,2,'{\"settings\": {}}','000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758002102956033,2099475894742622209,'EndEvent_mu2c2p8c0','ç»“æŸ',3,0,0,0,1,0,0,3,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758024894803970,2099758024894803969,'StartEvent_mu2c2p840','å¼€å§‹',0,0,0,0,1,0,0,1,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758024894803971,2099758024894803969,'UserTask_mu2c2p880','1231',1,0,0,0,1,0,0,2,'{\"settings\": {}}','000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758024894803972,2099758024894803969,'EndEvent_mu2c2p8c0','ç»“æŸ',3,0,0,0,1,0,0,3,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,0),(2099758062752591874,2099758062693871618,'StartEvent_mu2c2p840','å¼€å§‹',0,0,0,0,1,0,0,1,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,1),(2099758062752591875,2099758062693871618,'UserTask_mu2c2p880','3',6,0,0,0,1,0,0,12,'{\"settings\": {}}','000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-16 15:55:50',1,0,1),(2099758062752591876,2099758062693871618,'EndEvent_mu2c2p8c0','ç»“æŸ',3,0,0,0,1,0,0,8,NULL,'000000',NULL,NULL,'2026-09-15 15:11:33',NULL,'2026-09-15 15:11:33',1,0,1),(2099769199200137217,2099758062693871618,'Activity_0qm5tc6','1',1,0,0,0,1,0,0,3,NULL,'000000',NULL,NULL,'2026-09-15 15:56:03',NULL,'2026-09-15 15:56:03',1,0,1),(2099769329089343489,2099758062693871618,'Activity_1mzuxt6','2',1,0,0,0,1,0,0,4,NULL,'000000',NULL,NULL,'2026-09-15 15:56:34',NULL,'2026-09-15 15:56:34',1,0,1),(2099816958225629186,2099758062693871618,'Activity_02klewn','6',1,0,0,0,1,0,0,5,NULL,'000000',NULL,NULL,'2026-09-15 19:05:50',NULL,'2026-09-15 19:05:50',1,0,1),(2099816975044788226,2099758062693871618,'Activity_1g4p700','4',6,0,0,0,1,0,0,13,NULL,'000000',NULL,NULL,'2026-09-15 19:05:54',NULL,'2026-09-16 19:34:46',1,1,1),(2099816992086245377,2099758062693871618,'Activity_0ylt45t','5',1,0,0,0,1,0,0,7,'{\"sign\": 0, \"remind\": 0, \"settings\": {\"timeout\": {\"hours\": 0, \"remind\": false}}, \"timeoutHours\": 0}','000000',NULL,NULL,'2026-09-15 19:05:58',NULL,'2026-09-15 19:05:58',1,0,1),(2100018659545096193,2099758062693871618,'Event_093p7s3','ä¸­é—´äº‹ä»¶',5,0,0,0,1,0,0,9,NULL,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-17 16:34:02',1,1,1),(2100018659545096194,2099758062693871618,'Event_0ctoqxr','ä¸­é—´äº‹ä»¶',5,0,0,0,1,0,0,10,NULL,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-17 16:34:05',1,1,1),(2100018659545096195,2099758062693871618,'Event_0pivae0','ä¸­é—´äº‹ä»¶',5,0,0,0,1,0,0,11,NULL,'000000',NULL,NULL,'2026-09-16 08:27:19',NULL,'2026-09-17 16:34:06',1,1,1),(2100085625609822210,2099758062693871618,'Event_1fty5wz','ç»“æŸ',3,0,0,0,1,0,0,12,NULL,'000000',NULL,NULL,'2026-09-16 12:53:25',NULL,'2026-09-16 12:53:30',1,1,0),(2100191851668635650,2099758062693871618,'Activity_1jl42bt','7',1,0,0,0,1,0,0,13,NULL,'000000',NULL,NULL,'2026-09-16 19:55:31',NULL,'2026-09-16 20:33:04',1,1,0),(2100549293887770626,2099758062693871618,'Activity_0vforb1','9',1,0,0,0,1,0,0,13,NULL,'000000',NULL,NULL,'2026-09-17 19:35:52',NULL,'2026-09-17 19:35:52',1,0,2),(2100549423093305345,2099758062693871618,'Activity_154cox6','10',1,0,0,0,1,0,0,14,NULL,'000000',NULL,NULL,'2026-09-17 19:36:23',NULL,'2026-09-17 19:36:23',1,0,2),(2100741005457289217,2100740952047022081,'Event_1lb88j7','å¼€å§‹',0,0,0,0,1,0,0,1,NULL,'000000',NULL,NULL,'2026-09-18 08:17:40',NULL,'2026-09-18 08:17:40',1,0,0),(2100741041675104258,2100740952047022081,'Activity_0rpzmot','1',1,0,0,0,1,0,0,2,NULL,'000000',NULL,NULL,'2026-09-18 08:17:48',NULL,'2026-09-18 08:17:48',1,0,0),(2100741054891356162,2100740952047022081,'Gateway_00dz885','ç½‘å…³',7,0,0,0,1,0,0,3,NULL,'000000',NULL,NULL,'2026-09-18 08:17:52',NULL,'2026-09-18 08:17:52',1,0,0),(2100741060557860865,2100740952047022081,'Activity_065wlfl','2',1,0,0,0,1,0,0,4,NULL,'000000',NULL,NULL,'2026-09-18 08:17:53',NULL,'2026-09-18 08:17:53',1,0,0),(2100741079688081410,2100740952047022081,'Activity_01qzg7i','3',1,0,0,0,1,0,0,5,NULL,'000000',NULL,NULL,'2026-09-18 08:17:57',NULL,'2026-09-18 08:17:57',1,0,0),(2100741270747017218,2100740952047022081,'Gateway_1qw9z2c','ç½‘å…³',7,0,0,0,1,0,0,6,NULL,'000000',NULL,NULL,'2026-09-18 08:18:43',NULL,'2026-09-18 08:18:43',1,0,0),(2100741328838127617,2100740952047022081,'Activity_0j79ocf','4',1,0,0,0,1,0,0,7,NULL,'000000',NULL,NULL,'2026-09-18 08:18:57',NULL,'2026-09-18 08:18:57',1,0,0),(2100741346408067073,2100740952047022081,'Event_0urcupa','ç»“æŸ',3,0,0,0,1,0,0,8,NULL,'000000',NULL,NULL,'2026-09-18 08:19:01',NULL,'2026-09-18 08:19:01',1,0,0),(2100741536309374978,2100740952047022081,'Activity_0gfw7j5','5',1,0,0,0,1,0,0,9,NULL,'000000',NULL,NULL,'2026-09-18 08:19:46',NULL,'2026-09-18 08:19:46',1,0,0);
/*!40000 ALTER TABLE `wf_process_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_task`
--

DROP TABLE IF EXISTS `wf_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `inst_id` bigint unsigned NOT NULL COMMENT 'æµç¨‹å®ä¾‹ID',
  `engine_task_id` varchar(64) NOT NULL DEFAULT '' COMMENT 'å¼•æ“ä»»åŠ¡IDï¼ˆFlowableï¼‰ï¼Œå¼±å…³è”',
  `node_key` varchar(64) NOT NULL COMMENT 'èŠ‚ç‚¹Key',
  `assignee` bigint NOT NULL COMMENT 'åŠç†äºº',
  `original_user` bigint DEFAULT NULL COMMENT 'ä»£ç†äººä»£åŠæ—¶çš„åŸå¤„ç†äºº',
  `sign_order` tinyint NOT NULL DEFAULT '0' COMMENT 'ä¼šç­¾å…³ç³»',
  `receive_time` datetime DEFAULT NULL COMMENT 'æ¥æ”¶æ—¶é—´',
  `operate_time` datetime DEFAULT NULL COMMENT 'å¤„ç†æ—¶é—´',
  `due_time` datetime DEFAULT NULL COMMENT 'æˆªæ­¢æ—¶é—´',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '0' COMMENT '0å¾…åŠ 2å·²åŠ 4åŠç»“ 6è‡ªåŠ¨æäº¤ 7ååŠ 8æŠ„é€ 11ä¼ é˜…ï¼ˆå¯¹é½ isremarkï¼‰',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  `is_test` tinyint NOT NULL DEFAULT '0' COMMENT 'æµ‹è¯•æ€æ ‡è®° 1=æµ‹è¯•äº§ç”Ÿçš„å¾…åŠ',
  PRIMARY KEY (`id`),
  KEY `idx_assignee_status` (`assignee`,`status`),
  KEY `idx_inst_node` (`inst_id`,`node_key`),
  KEY `idx_engine_task` (`engine_task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2100506881907769347 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æµç¨‹ä»»åŠ¡ï¼ˆå¾…åŠ/å·²åŠï¼‰';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_task`
--

LOCK TABLES `wf_task` WRITE;
/*!40000 ALTER TABLE `wf_task` DISABLE KEYS */;
INSERT INTO `wf_task` VALUES (2100506881450590209,2100506880599146497,'15020','Activity_0qm5tc6',2019979771992432642,NULL,0,'2026-09-17 16:47:21',NULL,NULL,'000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',0,0,1),(2100506881450590210,2100506880599146497,'15020','Activity_0qm5tc6',2044030517948477441,NULL,0,'2026-09-17 16:47:21',NULL,NULL,'000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',0,0,1),(2100506881647722497,2100506880599146497,'15022','Activity_1mzuxt6',2036826812715196418,NULL,0,'2026-09-17 16:47:21',NULL,NULL,'000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',0,0,1),(2100506881647722498,2100506880599146497,'15022','Activity_1mzuxt6',2042170824913412098,NULL,0,'2026-09-17 16:47:21',NULL,NULL,'000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',0,0,1),(2100506881714831361,2100506880599146497,'15022','Activity_1mzuxt6',2045299268400508929,NULL,0,'2026-09-17 16:47:21',NULL,NULL,'000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',0,0,1),(2100506881714831362,2100506880599146497,'15022','Activity_1mzuxt6',2049417585025458177,NULL,0,'2026-09-17 16:47:21',NULL,NULL,'000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',0,0,1),(2100506881781940226,2100506880599146497,'15022','Activity_1mzuxt6',2051983659671547905,NULL,0,'2026-09-17 16:47:21',NULL,NULL,'000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',0,0,1),(2100506881844854786,2100506880599146497,'15024','Activity_02klewn',2019979771992432642,NULL,0,'2026-09-17 16:47:21',NULL,NULL,'000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',0,0,1),(2100506881907769346,2100506880599146497,'15024','Activity_02klewn',2044030517948477441,NULL,0,'2026-09-17 16:47:21',NULL,NULL,'000000',NULL,NULL,'2026-09-17 16:47:20',NULL,'2026-09-17 16:47:20',0,0,1);
/*!40000 ALTER TABLE `wf_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_test_log`
--

DROP TABLE IF EXISTS `wf_test_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_test_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `def_id` bigint unsigned NOT NULL COMMENT 'æµç¨‹å®šä¹‰ID',
  `def_version` int DEFAULT NULL COMMENT 'æµ‹è¯•æ—¶çš„æµç¨‹ç‰ˆæœ¬',
  `proc_key` varchar(100) DEFAULT NULL COMMENT 'å¼•æ“æµç¨‹Keyï¼ˆå†—ä½™ï¼Œä¾¿äºæ£€ç´¢ï¼‰',
  `def_name` varchar(200) DEFAULT NULL COMMENT 'æµç¨‹åç§°ï¼ˆå†—ä½™ï¼‰',
  `test_user_id` bigint NOT NULL COMMENT 'æµ‹è¯•å‘èµ·äººç”¨æˆ·ID',
  `test_user_name` varchar(100) DEFAULT NULL COMMENT 'æµ‹è¯•å‘èµ·äººå§“åï¼ˆå†—ä½™ï¼‰',
  `test_time` datetime NOT NULL COMMENT 'æµ‹è¯•æ—¶é—´',
  `cost_ms` bigint NOT NULL DEFAULT '0' COMMENT 'è€—æ—¶ï¼ˆæ¯«ç§’ï¼‰',
  `test_status` int NOT NULL DEFAULT '0' COMMENT 'æµ‹è¯•ç»“è®º 0æœªé€šè¿‡ 1é€šè¿‡ 2å¼‚å¸¸ä¸­æ–­',
  `node_total` int NOT NULL DEFAULT '0' COMMENT 'å‚ä¸æ ¡éªŒèŠ‚ç‚¹æ•°',
  `node_passed` int NOT NULL DEFAULT '0' COMMENT 'èµ°é€šèŠ‚ç‚¹æ•°',
  `reached_end` int NOT NULL DEFAULT '0' COMMENT 'æ˜¯å¦èµ°åˆ°å½’æ¡£èŠ‚ç‚¹ 1æ˜¯ 0å¦',
  `summary` varchar(1000) DEFAULT NULL COMMENT 'ç»“è®ºæ‘˜è¦',
  `log_content` longtext COMMENT 'æµ‹è¯•æ—¥å¿—æ­£æ–‡ï¼ˆé€è¡Œæ–‡æœ¬ï¼‰',
  `result_json` json DEFAULT NULL COMMENT 'ç»“æ„åŒ–ç»“æœï¼šèŠ‚ç‚¹ç»è¿‡æ¬¡æ•°/è·¯å¾„/æ“ä½œè€…',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '1' COMMENT 'çŠ¶æ€:1æ­£å¸¸ 0ç¦ç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_def_time` (`def_id`,`test_time`),
  KEY `idx_user_time` (`test_user_id`,`test_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2100541210537021443 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æµç¨‹æµ‹è¯•æ—¥å¿—';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_test_log`
--

LOCK TABLES `wf_test_log` WRITE;
/*!40000 ALTER TABLE `wf_test_log` DISABLE KEYS */;
INSERT INTO `wf_test_log` VALUES (2100399131932545025,2099758062693871618,4,'flow_mu180u86md3e','æµ‹è¯•914',2019979771992432642,'test1','2026-09-17 09:39:11',327,1,10,10,1,'æµ‹è¯•é€šè¿‡ï¼šæµç¨‹ä»åˆ›å»ºèŠ‚ç‚¹èµ°é€šåˆ°å½’æ¡£èŠ‚ç‚¹ï¼Œå…± 10 ä¸ªèŠ‚ç‚¹ï¼Œå„èŠ‚ç‚¹æ“ä½œè€…å‡èƒ½è§£æã€‚','å‘èµ·äººèº«ä»½ï¼štest1ï¼ˆ2019979771992432642ï¼‰\næµç¨‹ï¼šæµ‹è¯•914ï¼ˆV4ï¼‰\n-----------------------------------------\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"å¼€å§‹\"ï¼Œæ“ä½œè€…\"æ— \"ï¼›\n2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\"å¼€å§‹\"ï¼›\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"3\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"3\"ï¼Œæ“ä½œè€…\"æ— \"ï¼›\n2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\"3\"ï¼›\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"ä¸­é—´äº‹ä»¶\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"ä¸­é—´äº‹ä»¶\"ï¼Œæ“ä½œè€…\"æ— \"ï¼›\n2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\"ä¸­é—´äº‹ä»¶\"ï¼›\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"6\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"6\"ï¼Œæ“ä½œè€…\"test1ï¼ˆ2019979771992432642ï¼‰ï¼ŒtestFindï¼ˆ2044030517948477441ï¼‰\"ï¼›\n2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\"6\"ï¼›\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"ç»“æŸ\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"ç»“æŸ\"ï¼Œæ“ä½œè€…\"æ— \"ï¼›\n2026-09-17 09:39:10 æµç¨‹å·²åˆ°è¾¾å½’æ¡£èŠ‚ç‚¹\"ç»“æŸ\"ï¼Œæµ‹è¯•å®Œæˆ\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"ä¸­é—´äº‹ä»¶\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"ä¸­é—´äº‹ä»¶\"ï¼Œæ“ä½œè€…\"æ— \"ï¼›\n2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\"ä¸­é—´äº‹ä»¶\"ï¼›\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"ä¸­é—´äº‹ä»¶\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"ä¸­é—´äº‹ä»¶\"ï¼Œæ“ä½œè€…\"æ— \"ï¼›\n2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\"ä¸­é—´äº‹ä»¶\"ï¼›\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"5\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"5\"ï¼Œæ“ä½œè€…\"test1ï¼ˆ2019979771992432642ï¼‰ï¼ŒtestFindï¼ˆ2044030517948477441ï¼‰ï¼Œtest2ï¼ˆ2020463343744380930ï¼‰\"ï¼›\n2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\"5\"ï¼›\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"ç»“æŸ\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"ç»“æŸ\"ï¼Œæ“ä½œè€…\"æ— \"ï¼›\n2026-09-17 09:39:10 æµç¨‹å·²åˆ°è¾¾å½’æ¡£èŠ‚ç‚¹\"ç»“æŸ\"ï¼Œæµ‹è¯•å®Œæˆ\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"1\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"1\"ï¼Œæ“ä½œè€…\"test1ï¼ˆ2019979771992432642ï¼‰ï¼ŒtestFindï¼ˆ2044030517948477441ï¼‰\"ï¼›\n2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\"1\"ï¼›\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"å½“ tes å¤§äº 100\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"ç»“æŸ\"ï¼Œæ“ä½œè€…\"æ— \"ï¼›\n2026-09-17 09:39:10 æµç¨‹å·²åˆ°è¾¾å½’æ¡£èŠ‚ç‚¹\"ç»“æŸ\"ï¼Œæµ‹è¯•å®Œæˆ\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"2\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"2\"ï¼Œæ“ä½œè€…\"2036826812715196418ï¼ˆ2036826812715196418ï¼‰ï¼Œ2042170824913412098ï¼ˆ2042170824913412098ï¼‰ï¼Œ2045299268400508929ï¼ˆ2045299268400508929ï¼‰ï¼Œå¾®ä¿¡ç”¨æˆ·ï¼ˆ2049417585025458177ï¼‰ï¼Œç”¨æˆ·7960ï¼ˆ2051983659671547905ï¼‰\"ï¼›\n2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\"2\"ï¼›\n2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\"ç»“æŸ\"ï¼›\n2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\"ç»“æŸ\"ï¼Œæ“ä½œè€…\"æ— \"ï¼›\n2026-09-17 09:39:10 æµç¨‹å·²åˆ°è¾¾å½’æ¡£èŠ‚ç‚¹\"ç»“æŸ\"ï¼Œæµ‹è¯•å®Œæˆ','{\"log\": [\"å‘èµ·äººèº«ä»½ï¼štest1ï¼ˆ2019979771992432642ï¼‰\", \"æµç¨‹ï¼šæµ‹è¯•914ï¼ˆV4ï¼‰\", \"-----------------------------------------\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"å¼€å§‹\\\"ï¼Œæ“ä½œè€…\\\"æ— \\\"ï¼›\", \"2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\\\"å¼€å§‹\\\"ï¼›\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"3\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"3\\\"ï¼Œæ“ä½œè€…\\\"æ— \\\"ï¼›\", \"2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\\\"3\\\"ï¼›\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"ä¸­é—´äº‹ä»¶\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"ä¸­é—´äº‹ä»¶\\\"ï¼Œæ“ä½œè€…\\\"æ— \\\"ï¼›\", \"2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\\\"ä¸­é—´äº‹ä»¶\\\"ï¼›\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"6\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"6\\\"ï¼Œæ“ä½œè€…\\\"test1ï¼ˆ2019979771992432642ï¼‰ï¼ŒtestFindï¼ˆ2044030517948477441ï¼‰\\\"ï¼›\", \"2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\\\"6\\\"ï¼›\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"ç»“æŸ\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"ç»“æŸ\\\"ï¼Œæ“ä½œè€…\\\"æ— \\\"ï¼›\", \"2026-09-17 09:39:10 æµç¨‹å·²åˆ°è¾¾å½’æ¡£èŠ‚ç‚¹\\\"ç»“æŸ\\\"ï¼Œæµ‹è¯•å®Œæˆ\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"ä¸­é—´äº‹ä»¶\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"ä¸­é—´äº‹ä»¶\\\"ï¼Œæ“ä½œè€…\\\"æ— \\\"ï¼›\", \"2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\\\"ä¸­é—´äº‹ä»¶\\\"ï¼›\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"ä¸­é—´äº‹ä»¶\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"ä¸­é—´äº‹ä»¶\\\"ï¼Œæ“ä½œè€…\\\"æ— \\\"ï¼›\", \"2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\\\"ä¸­é—´äº‹ä»¶\\\"ï¼›\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"5\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"5\\\"ï¼Œæ“ä½œè€…\\\"test1ï¼ˆ2019979771992432642ï¼‰ï¼ŒtestFindï¼ˆ2044030517948477441ï¼‰ï¼Œtest2ï¼ˆ2020463343744380930ï¼‰\\\"ï¼›\", \"2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\\\"5\\\"ï¼›\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"ç»“æŸ\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"ç»“æŸ\\\"ï¼Œæ“ä½œè€…\\\"æ— \\\"ï¼›\", \"2026-09-17 09:39:10 æµç¨‹å·²åˆ°è¾¾å½’æ¡£èŠ‚ç‚¹\\\"ç»“æŸ\\\"ï¼Œæµ‹è¯•å®Œæˆ\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"1\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"1\\\"ï¼Œæ“ä½œè€…\\\"test1ï¼ˆ2019979771992432642ï¼‰ï¼ŒtestFindï¼ˆ2044030517948477441ï¼‰\\\"ï¼›\", \"2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\\\"1\\\"ï¼›\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"å½“ tes å¤§äº 100\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"ç»“æŸ\\\"ï¼Œæ“ä½œè€…\\\"æ— \\\"ï¼›\", \"2026-09-17 09:39:10 æµç¨‹å·²åˆ°è¾¾å½’æ¡£èŠ‚ç‚¹\\\"ç»“æŸ\\\"ï¼Œæµ‹è¯•å®Œæˆ\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"2\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"2\\\"ï¼Œæ“ä½œè€…\\\"2036826812715196418ï¼ˆ2036826812715196418ï¼‰ï¼Œ2042170824913412098ï¼ˆ2042170824913412098ï¼‰ï¼Œ2045299268400508929ï¼ˆ2045299268400508929ï¼‰ï¼Œå¾®ä¿¡ç”¨æˆ·ï¼ˆ2049417585025458177ï¼‰ï¼Œç”¨æˆ·7960ï¼ˆ2051983659671547905ï¼‰\\\"ï¼›\", \"2026-09-17 09:39:10 é€šè¿‡èŠ‚ç‚¹\\\"2\\\"ï¼›\", \"2026-09-17 09:39:10 æ‰§è¡Œå‡ºå£\\\"ç»“æŸ\\\"ï¼›\", \"2026-09-17 09:39:10 åˆ°è¾¾èŠ‚ç‚¹\\\"ç»“æŸ\\\"ï¼Œæ“ä½œè€…\\\"æ— \\\"ï¼›\", \"2026-09-17 09:39:10 æµç¨‹å·²åˆ°è¾¾å½’æ¡£èŠ‚ç‚¹\\\"ç»“æŸ\\\"ï¼Œæµ‹è¯•å®Œæˆ\"], \"path\": [{\"toNodeKey\": \"UserTask_mu2c2p880\", \"conditionCn\": \"\", \"fromNodeKey\": \"StartEvent_mu2c2p840\"}, {\"toNodeKey\": \"Event_093p7s3\", \"conditionCn\": \"\", \"fromNodeKey\": \"UserTask_mu2c2p880\"}, {\"toNodeKey\": \"Activity_02klewn\", \"conditionCn\": \"\", \"fromNodeKey\": \"Event_093p7s3\"}, {\"toNodeKey\": \"EndEvent_mu2c2p8c0\", \"conditionCn\": \"\", \"fromNodeKey\": \"Activity_02klewn\"}, {\"toNodeKey\": \"Event_0ctoqxr\", \"conditionCn\": \"\", \"fromNodeKey\": \"Event_093p7s3\"}, {\"toNodeKey\": \"Event_0pivae0\", \"conditionCn\": \"\", \"fromNodeKey\": \"Event_0ctoqxr\"}, {\"toNodeKey\": \"Activity_0ylt45t\", \"conditionCn\": \"\", \"fromNodeKey\": \"Event_0pivae0\"}, {\"toNodeKey\": \"EndEvent_mu2c2p8c0\", \"conditionCn\": \"\", \"fromNodeKey\": \"Activity_0ylt45t\"}, {\"toNodeKey\": \"Activity_0qm5tc6\", \"conditionCn\": \"\", \"fromNodeKey\": \"UserTask_mu2c2p880\"}, {\"toNodeKey\": \"EndEvent_mu2c2p8c0\", \"conditionCn\": \"å½“ tes å¤§äº 100\", \"fromNodeKey\": \"Activity_0qm5tc6\"}, {\"toNodeKey\": \"Activity_1mzuxt6\", \"conditionCn\": \"\", \"fromNodeKey\": \"UserTask_mu2c2p880\"}, {\"toNodeKey\": \"EndEvent_mu2c2p8c0\", \"conditionCn\": \"\", \"fromNodeKey\": \"Activity_1mzuxt6\"}], \"logId\": null, \"nodes\": [{\"status\": 1, \"message\": \"èµ°é€š\", \"nodeKey\": \"Activity_02klewn\", \"nodeName\": \"6\", \"nodeType\": 1, \"operators\": [{\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2019979771992432642, \"userName\": \"test1\"}, {\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2044030517948477441, \"userName\": \"testFind\"}], \"passTimes\": 1}, {\"status\": 1, \"message\": \"èµ°é€š\", \"nodeKey\": \"Activity_0qm5tc6\", \"nodeName\": \"1\", \"nodeType\": 1, \"operators\": [{\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2019979771992432642, \"userName\": \"test1\"}, {\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2044030517948477441, \"userName\": \"testFind\"}], \"passTimes\": 1}, {\"status\": 1, \"message\": \"èµ°é€š\", \"nodeKey\": \"Activity_0ylt45t\", \"nodeName\": \"5\", \"nodeType\": 1, \"operators\": [{\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2019979771992432642, \"userName\": \"test1\"}, {\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2044030517948477441, \"userName\": \"testFind\"}, {\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2020463343744380930, \"userName\": \"test2\"}], \"passTimes\": 1}, {\"status\": 1, \"message\": \"èµ°é€š\", \"nodeKey\": \"Activity_1mzuxt6\", \"nodeName\": \"2\", \"nodeType\": 1, \"operators\": [{\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2036826812715196418, \"userName\": \"2036826812715196418\"}, {\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2042170824913412098, \"userName\": \"2042170824913412098\"}, {\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2045299268400508929, \"userName\": \"2045299268400508929\"}, {\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2049417585025458177, \"userName\": \"å¾®ä¿¡ç”¨æˆ·\"}, {\"source\": \"æ“ä½œè€…é…ç½®è§£æ\", \"userId\": 2051983659671547905, \"userName\": \"ç”¨æˆ·7960\"}], \"passTimes\": 1}, {\"status\": 1, \"message\": \"èµ°é€š\", \"nodeKey\": \"EndEvent_mu2c2p8c0\", \"nodeName\": \"ç»“æŸ\", \"nodeType\": 3, \"operators\": [], \"passTimes\": 4}, {\"status\": 1, \"message\": \"èµ°é€š\", \"nodeKey\": \"Event_093p7s3\", \"nodeName\": \"ä¸­é—´äº‹ä»¶\", \"nodeType\": 5, \"operators\": [], \"passTimes\": 1}, {\"status\": 1, \"message\": \"èµ°é€š\", \"nodeKey\": \"Event_0ctoqxr\", \"nodeName\": \"ä¸­é—´äº‹ä»¶\", \"nodeType\": 5, \"operators\": [], \"passTimes\": 1}, {\"status\": 1, \"message\": \"èµ°é€š\", \"nodeKey\": \"Event_0pivae0\", \"nodeName\": \"ä¸­é—´äº‹ä»¶\", \"nodeType\": 5, \"operators\": [], \"passTimes\": 1}, {\"status\": 1, \"message\": \"èµ°é€š\", \"nodeKey\": \"StartEvent_mu2c2p840\", \"nodeName\": \"å¼€å§‹\", \"nodeType\": 0, \"operators\": [], \"passTimes\": 1}, {\"status\": 1, \"message\": \"èµ°é€š\", \"nodeKey\": \"UserTask_mu2c2p880\", \"nodeName\": \"3\", \"nodeType\": 6, \"operators\": [], \"passTimes\": 1}], \"costMs\": 327, \"summary\": \"æµ‹è¯•é€šè¿‡ï¼šæµç¨‹ä»åˆ›å»ºèŠ‚ç‚¹èµ°é€šåˆ°å½’æ¡£èŠ‚ç‚¹ï¼Œå…± 10 ä¸ªèŠ‚ç‚¹ï¼Œå„èŠ‚ç‚¹æ“ä½œè€…å‡èƒ½è§£æã€‚\", \"nodeTimes\": {\"Event_093p7s3\": 1, \"Event_0ctoqxr\": 1, \"Event_0pivae0\": 1, \"Activity_02klewn\": 1, \"Activity_0qm5tc6\": 1, \"Activity_0ylt45t\": 1, \"Activity_1mzuxt6\": 1, \"EndEvent_mu2c2p8c0\": 4, \"UserTask_mu2c2p880\": 1, \"StartEvent_mu2c2p840\": 1}, \"nodeTotal\": 10, \"nodePassed\": 10, \"reachedEnd\": true, \"testStatus\": 1}','000000',1123598821738675201,NULL,'2026-09-17 09:39:11',NULL,'2026-09-17 09:39:11',1,0),(2100515081595965441,2099758062693871618,4,'flow_mu180u86md3e','æµ‹è¯•914',2019979771992432642,'test1','2026-09-17 17:19:56',13,0,7,0,0,'æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚','2026-09-17 17:19:55 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\n    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•','{\"log\": [\"2026-09-17 17:19:55 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\", \"    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\"], \"path\": null, \"logId\": null, \"nodes\": [{\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_02klewn\", \"nodeName\": \"6\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0qm5tc6\", \"nodeName\": \"1\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0ylt45t\", \"nodeName\": \"5\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_1mzuxt6\", \"nodeName\": \"2\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 2, \"message\": \"å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\", \"nodeKey\": \"EndEvent_mu2c2p8c0\", \"nodeName\": \"ç»“æŸ\", \"nodeType\": 3, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"StartEvent_mu2c2p840\", \"nodeName\": \"å¼€å§‹\", \"nodeType\": 0, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"UserTask_mu2c2p880\", \"nodeName\": \"3\", \"nodeType\": 6, \"operators\": [], \"passTimes\": 0}], \"costMs\": 13, \"summary\": \"æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚\", \"nodeTimes\": {}, \"nodeTotal\": 7, \"nodePassed\": 0, \"reachedEnd\": false, \"testStatus\": 0}','000000',1123598821738675201,NULL,'2026-09-17 17:19:55',NULL,'2026-09-17 17:19:55',1,0),(2100515614146744321,2099758062693871618,4,'flow_mu180u86md3e','æµ‹è¯•914',2019979771992432642,'test1','2026-09-17 17:22:03',6,0,7,0,0,'æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚','2026-09-17 17:22:02 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\n    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•','{\"log\": [\"2026-09-17 17:22:02 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\", \"    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\"], \"path\": null, \"logId\": null, \"nodes\": [{\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_02klewn\", \"nodeName\": \"6\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0qm5tc6\", \"nodeName\": \"1\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0ylt45t\", \"nodeName\": \"5\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_1mzuxt6\", \"nodeName\": \"2\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 2, \"message\": \"å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\", \"nodeKey\": \"EndEvent_mu2c2p8c0\", \"nodeName\": \"ç»“æŸ\", \"nodeType\": 3, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"StartEvent_mu2c2p840\", \"nodeName\": \"å¼€å§‹\", \"nodeType\": 0, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"UserTask_mu2c2p880\", \"nodeName\": \"3\", \"nodeType\": 6, \"operators\": [], \"passTimes\": 0}], \"costMs\": 6, \"summary\": \"æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚\", \"nodeTimes\": {}, \"nodeTotal\": 7, \"nodePassed\": 0, \"reachedEnd\": false, \"testStatus\": 0}','000000',1123598821738675201,NULL,'2026-09-17 17:22:02',NULL,'2026-09-17 17:22:02',1,0),(2100531761239871490,2099758062693871618,4,'flow_mu180u86md3e','æµ‹è¯•914',2036826812715196418,'2036826812715196418','2026-09-17 18:26:13',4,0,7,0,0,'æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚','2026-09-17 18:26:12 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\n    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•','{\"log\": [\"2026-09-17 18:26:12 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\", \"    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\"], \"path\": null, \"logId\": null, \"nodes\": [{\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_02klewn\", \"nodeName\": \"6\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0qm5tc6\", \"nodeName\": \"1\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0ylt45t\", \"nodeName\": \"5\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_1mzuxt6\", \"nodeName\": \"2\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 2, \"message\": \"å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\", \"nodeKey\": \"EndEvent_mu2c2p8c0\", \"nodeName\": \"ç»“æŸ\", \"nodeType\": 3, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"StartEvent_mu2c2p840\", \"nodeName\": \"å¼€å§‹\", \"nodeType\": 0, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"UserTask_mu2c2p880\", \"nodeName\": \"3\", \"nodeType\": 6, \"operators\": [], \"passTimes\": 0}], \"costMs\": 4, \"summary\": \"æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚\", \"nodeTimes\": {}, \"nodeTotal\": 7, \"nodePassed\": 0, \"reachedEnd\": false, \"testStatus\": 0}','000000',1123598821738675201,NULL,'2026-09-17 18:26:12',NULL,'2026-09-17 18:26:12',1,0),(2100531963275300866,2099758062693871618,4,'flow_mu180u86md3e','æµ‹è¯•914',2036826812715196418,'2036826812715196418','2026-09-17 18:27:01',4,0,7,0,0,'æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚','2026-09-17 18:27:00 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\n    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•','{\"log\": [\"2026-09-17 18:27:00 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\", \"    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\"], \"path\": null, \"logId\": null, \"nodes\": [{\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_02klewn\", \"nodeName\": \"6\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0qm5tc6\", \"nodeName\": \"1\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0ylt45t\", \"nodeName\": \"5\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_1mzuxt6\", \"nodeName\": \"2\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 2, \"message\": \"å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\", \"nodeKey\": \"EndEvent_mu2c2p8c0\", \"nodeName\": \"ç»“æŸ\", \"nodeType\": 3, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"StartEvent_mu2c2p840\", \"nodeName\": \"å¼€å§‹\", \"nodeType\": 0, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"UserTask_mu2c2p880\", \"nodeName\": \"3\", \"nodeType\": 6, \"operators\": [], \"passTimes\": 0}], \"costMs\": 4, \"summary\": \"æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚\", \"nodeTimes\": {}, \"nodeTotal\": 7, \"nodePassed\": 0, \"reachedEnd\": false, \"testStatus\": 0}','000000',1123598821738675201,NULL,'2026-09-17 18:27:00',NULL,'2026-09-17 18:27:00',1,0),(2100532418596360193,2099758062693871618,4,'flow_mu180u86md3e','æµ‹è¯•914',2036826812715196418,'2036826812715196418','2026-09-17 18:28:49',3,0,7,0,0,'æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚','2026-09-17 18:28:49 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\n    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•','{\"log\": [\"2026-09-17 18:28:49 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\", \"    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\"], \"path\": null, \"logId\": null, \"nodes\": [{\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_02klewn\", \"nodeName\": \"6\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0qm5tc6\", \"nodeName\": \"1\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0ylt45t\", \"nodeName\": \"5\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_1mzuxt6\", \"nodeName\": \"2\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 2, \"message\": \"å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\", \"nodeKey\": \"EndEvent_mu2c2p8c0\", \"nodeName\": \"ç»“æŸ\", \"nodeType\": 3, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"StartEvent_mu2c2p840\", \"nodeName\": \"å¼€å§‹\", \"nodeType\": 0, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"UserTask_mu2c2p880\", \"nodeName\": \"3\", \"nodeType\": 6, \"operators\": [], \"passTimes\": 0}], \"costMs\": 3, \"summary\": \"æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚\", \"nodeTimes\": {}, \"nodeTotal\": 7, \"nodePassed\": 0, \"reachedEnd\": false, \"testStatus\": 0}','000000',1123598821738675201,NULL,'2026-09-17 18:28:49',NULL,'2026-09-17 18:28:49',1,0),(2100541210537021442,2099758062693871618,4,'flow_mu180u86md3e','æµ‹è¯•914',2036826812715196418,'2036826812715196418','2026-09-17 19:03:45',3,0,7,0,0,'æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚','2026-09-17 19:03:45 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\n    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•','{\"log\": [\"2026-09-17 19:03:45 é¢„æ ¡éªŒå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼Œå·²ç»ˆæ­¢æµ‹è¯•ï¼š\", \"    - èŠ‚ç‚¹ã€ç»“æŸã€‘å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\"], \"path\": null, \"logId\": null, \"nodes\": [{\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_02klewn\", \"nodeName\": \"6\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0qm5tc6\", \"nodeName\": \"1\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_0ylt45t\", \"nodeName\": \"5\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"Activity_1mzuxt6\", \"nodeName\": \"2\", \"nodeType\": 1, \"operators\": [], \"passTimes\": 0}, {\"status\": 2, \"message\": \"å‡ºå£æ¡ä»¶è¡¨è¾¾å¼éæ³•ï¼šå«è¢«è½¬ä¹‰çš„è¿ç®—ç¬¦ï¼ˆ&gt; &lt; &amp;ï¼‰ï¼Œè¯·åœ¨ã€Œæµè½¬è®¾ç½®ã€é‡æ–°ä¿å­˜æ¡ä»¶åå†æµ‹è¯•\", \"nodeKey\": \"EndEvent_mu2c2p8c0\", \"nodeName\": \"ç»“æŸ\", \"nodeType\": 3, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"StartEvent_mu2c2p840\", \"nodeName\": \"å¼€å§‹\", \"nodeType\": 0, \"operators\": [], \"passTimes\": 0}, {\"status\": 0, \"message\": \"æœªèµ°åˆ°è¯¥èŠ‚ç‚¹ï¼ˆä¸èµ·ç‚¹ä¸è¿é€šæˆ–ç½‘å…³æ¡ä»¶æœªå‘½ä¸­ï¼‰\", \"nodeKey\": \"UserTask_mu2c2p880\", \"nodeName\": \"3\", \"nodeType\": 6, \"operators\": [], \"passTimes\": 0}], \"costMs\": 3, \"summary\": \"æµ‹è¯•æœªé€šè¿‡ï¼šå‘ç° 1 ä¸ªèŠ‚ç‚¹é…ç½®é—®é¢˜ï¼ˆè¯¦è§èŠ‚ç‚¹åˆ—è¡¨ä¸æ—¥å¿—ï¼‰ï¼Œè¯·ä¿®æ­£åé‡è¯•ã€‚\", \"nodeTimes\": {}, \"nodeTotal\": 7, \"nodePassed\": 0, \"reachedEnd\": false, \"testStatus\": 0}','000000',1123598821738675201,NULL,'2026-09-17 19:03:45',NULL,'2026-09-17 19:03:45',1,0);
/*!40000 ALTER TABLE `wf_test_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_workflow_type`
--

DROP TABLE IF EXISTS `wf_workflow_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_workflow_type` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `type_name` varchar(100) NOT NULL COMMENT 'ç±»å‹åç§°',
  `type_desc` varchar(500) DEFAULT NULL COMMENT 'ç±»å‹æè¿°',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT 'æ˜¾ç¤ºé¡ºåº',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT 'ç§Ÿæˆ·ID',
  `create_user` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `create_dept` bigint DEFAULT NULL COMMENT 'åˆ›å»ºéƒ¨é—¨',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_user` bigint DEFAULT NULL COMMENT 'ä¿®æ”¹äºº',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'ä¿®æ”¹æ—¶é—´',
  `status` int NOT NULL DEFAULT '1' COMMENT '0åœç”¨ 1å¯ç”¨',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT 'é€»è¾‘åˆ é™¤:1å·²åˆ  0æœªåˆ ',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`status`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æµç¨‹ï¼ˆè·¯å¾„ï¼‰ç±»å‹ï¼ˆæµè§ˆæ¡† wftype æ•°æ®æºï¼‰';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_workflow_type`
--

LOCK TABLES `wf_workflow_type` WRITE;
/*!40000 ALTER TABLE `wf_workflow_type` DISABLE KEYS */;
INSERT INTO `wf_workflow_type` VALUES (1,'è¡Œæ”¿å®¡æ‰¹','è¡Œæ”¿å®¡æ‰¹ç±»æµç¨‹',1,'000000',NULL,NULL,'2026-09-10 15:53:50',NULL,'2026-09-10 15:53:50',1,0),(2,'äººäº‹æµç¨‹','æ‹›è˜ã€å…¥è½¬è°ƒç¦»ã€è€ƒå‹¤ç­‰',2,'000000',NULL,NULL,'2026-09-10 15:53:50',NULL,'2026-09-10 15:53:50',1,0),(3,'è´¢åŠ¡æµç¨‹','æŠ¥é”€ã€é¢„ç®—ã€ä»˜æ¬¾ç­‰',3,'000000',NULL,NULL,'2026-09-10 15:53:50',NULL,'2026-09-10 15:53:50',1,0),(4,'ITæµç¨‹','ç³»ç»Ÿæƒé™ã€èµ„æºç”³è¯·ã€è¿ç»´',4,'000000',NULL,NULL,'2026-09-10 15:53:50',NULL,'2026-09-10 15:53:50',1,0),(5,'ä¸šåŠ¡æµç¨‹','å„ä¸šåŠ¡è¿è¥å®¡æ‰¹',5,'000000',NULL,NULL,'2026-09-10 15:53:50',NULL,'2026-09-10 15:53:50',1,0);
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

-- Dump completed on 2026-09-18  8:25:43
