-- =============================================================================
-- 迁移脚本 001：wf_process_definition 补 bpmn_xml 列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.10_001
-- 目标库    : blade_workflow
-- 影响表    : wf_process_definition
-- 变更类型  : 加列（DDL）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  : ALTER TABLE `wf_process_definition` DROP COLUMN `bpmn_xml`;
--
-- 背景（故障记录）
--   现象：GET /api/blade-workflow/definition/list 抛出
--         java.sql.SQLSyntaxErrorException: Unknown column 'bpmn_xml' in 'field list'
--   原因：bpmn-js 流程画布需求上线时，实体 WfProcessDefinition 增加了 bpmnXml 字段、
--         建表脚本也已同步该列，但**线上库未执行 ALTER**。
--         本项目无 Flyway/Liquibase，schema 变更长期靠手工执行，导致实体与库表漂移。
--
-- ⚠️ 严禁使用 blade-service/blade-workflow/src/main/resources/sql/workflow-schema.sql
--    来"同步结构"——该脚本第 29~39 行为 DROP TABLE IF EXISTS 全部 9 张 wf_* 表后重建，
--    会清空所有业务数据，仅适用于全新库初始化。
--
-- 字段说明
--   bpmn_xml  MEDIUMTEXT  NULL
--   BPMN 2.0 流程定义 XML，由 bpmn-js 画布产出，部署时下发 Flowable 引擎。
--   放在 name 之后、version 之前，与建表脚本列序一致。
-- =============================================================================

USE `blade_workflow`;

SET @v_db   = DATABASE();
SET @v_tbl  = 'wf_process_definition';
SET @v_col  = 'bpmn_xml';

-- 判定列是否已存在
SET @v_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db
      AND TABLE_NAME   = @v_tbl
      AND COLUMN_NAME  = @v_col
);

SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_process_definition`
         ADD COLUMN `bpmn_xml` MEDIUMTEXT NULL
         COMMENT ''BPMN 2.0 流程定义 XML（bpmn-js 画布产出，部署时下发引擎）''
         AFTER `name`',
    'SELECT ''SKIP: wf_process_definition.bpmn_xml already exists'' AS migration_info'
);

PREPARE _stmt FROM @v_sql;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 校验（应返回 1）
SELECT COUNT(1) AS bpmn_xml_should_be_1
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'wf_process_definition'
  AND COLUMN_NAME  = 'bpmn_xml';
