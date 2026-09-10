-- =============================================================================
-- 迁移脚本 003：wf_process_definition 补路径设置字段
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.10_003
-- 目标库    : blade_workflow
-- 影响表    : wf_process_definition
-- 变更类型  : 加列（DDL）
-- 是否幂等  : 是（逐列判定，已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `wf_process_definition` DROP COLUMN `type`;
--   ALTER TABLE `wf_process_definition` DROP COLUMN `form_type`;
--   ALTER TABLE `wf_process_definition` DROP COLUMN `description`;
--   ALTER TABLE `wf_process_definition` DROP COLUMN `sort_order`;
--
-- 背景
--   「流程设计（路径设置）」的新增/编辑弹窗需对齐 ecology「添加路径」界面，新增以下字段：
--     路径类型、对应表单类型、路径描述、显示顺序。
--   实体 WfProcessDefinition 已同步增加 type / formType / description / sortOrder，
--   但线上库若未执行本 ALTER，MyBatis-Plus 生成的 SELECT 会命中未知列，
--   导致 GET /api/blade-workflow/definition/list 报
--   「Unknown column 'type' in 'field list'」。
--
-- ⚠️ 执行顺序：先执行本脚本，再重启/重部署 blade-workflow，否则列表接口会报未知列。
-- ⚠️ 严禁使用 blade-service/blade-workflow/src/main/resources/sql/workflow-schema.sql
--    来"同步结构"——该脚本会 DROP TABLE 后重建，清空全部业务数据，仅适用于全新库初始化。
--
-- 字段说明（列序与建表脚本保持一致：is_free 之后）
--   type        VARCHAR(64)  NULL  路径类型（对齐 ecology path_type 字典 code）
--   form_type   TINYINT      NULL  对应表单类型：0自定义表单 1系统表单
--   description VARCHAR(500) NULL  路径描述
--   sort_order  INT          NOT NULL DEFAULT 0  显示顺序
-- =============================================================================

USE `blade_workflow`;

SET @v_db  = DATABASE();
SET @v_tbl = 'wf_process_definition';

-- 1) type ---------------------------------------------------------------------
SET @v_col = 'type';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_process_definition`
         ADD COLUMN `type` VARCHAR(64) NULL
         COMMENT ''路径类型（对齐 ecology path_type 字典 code）''
         AFTER `is_free`',
    'SELECT ''SKIP: wf_process_definition.type already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 2) form_type ----------------------------------------------------------------
SET @v_col = 'form_type';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_process_definition`
         ADD COLUMN `form_type` TINYINT NULL
         COMMENT ''对应表单类型：0自定义表单 1系统表单''
         AFTER `type`',
    'SELECT ''SKIP: wf_process_definition.form_type already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 3) description --------------------------------------------------------------
SET @v_col = 'description';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_process_definition`
         ADD COLUMN `description` VARCHAR(500) NULL
         COMMENT ''路径描述''
         AFTER `form_type`',
    'SELECT ''SKIP: wf_process_definition.description already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 4) sort_order ---------------------------------------------------------------
SET @v_col = 'sort_order';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_process_definition`
         ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0
         COMMENT ''显示顺序''
         AFTER `description`',
    'SELECT ''SKIP: wf_process_definition.sort_order already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 校验（应返回 4）--------------------------------------------------------------
SELECT COUNT(1) AS path_fields_should_be_4
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'wf_process_definition'
  AND COLUMN_NAME IN ('type', 'form_type', 'description', 'sort_order');
