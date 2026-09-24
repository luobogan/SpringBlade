-- =============================================================================
-- 迁移脚本 001：wf_process_definition.form_id 改为可空
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.24_001
-- 目标库    : blade_workflow
-- 影响表    : wf_process_definition（含可选 wf_instance）
-- 变更类型  : 改列属性（DDL，仅改 NULLABLE）
-- 是否幂等  : 是（已是 NULL 则跳过，不报错）
-- 是否丢数据: 否（仅放宽约束）
-- 回滚脚本  :
--   ALTER TABLE `wf_process_definition` MODIFY COLUMN `form_id` BIGINT NOT NULL
--       COMMENT '关联 workflow_bill.id（表单）';
--   ALTER TABLE `wf_instance`          MODIFY COLUMN `form_id` BIGINT NOT NULL
--       COMMENT '表单ID（workflow_bill.id）';
--
-- 背景
--   BPMN 导入接口 /definition/import（importNewDefinition）在插入 wf_process_definition
--   时，form_id 来自请求体，BPMN 未关联表单时为 NULL。但建表脚本（workflow-schema.sql
--   第 63 行）将 form_id 设为 NOT NULL 且无默认值，导致插入失败（500）。
--   语义上 formId 本就是「可空」（DefinitionImportDTO 注释：关联 workflow_bill.id，可空），
--   故将列放宽为可空。
--
--   注：wf_instance.form_id 同样为 NOT NULL。仅当你们「启动无表单流程」时才会轮到它报错；
--       下方 OPTIONAL 段已一并放宽为可空（无外键约束，安全），如不想动可删除该段。
--
-- ⚠️ 执行顺序：先在目标库 blade_workflow 执行本脚本，再重启/重部署 blade-workflow。
--   执行方式（服务器本机，root@localhost 可连）：
--     mysql -u root -p123456 blade_workflow < V2026.09.24_001__wf_process_definition_form_id_nullable.sql
-- =============================================================================

USE `blade_workflow`;

SET @v_db  = DATABASE();
SET @v_tbl = 'wf_process_definition';
SET @v_col = 'form_id';

-- 仅当当前为 NOT NULL 时才改可空；已是 NULL 则跳过（幂等）----------------------
SET @v_is_nullable = (
    SELECT UPPER(IS_NULLABLE)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);

SET @v_sql = IF(
    @v_is_nullable = 'NO',
    'ALTER TABLE `wf_process_definition` MODIFY COLUMN `form_id` BIGINT NULL '
        'COMMENT ''关联 workflow_bill.id（表单），可空''',
    'SELECT ''SKIP: wf_process_definition.form_id already nullable'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 校验（应返回 IS_NULLABLE = YES）---------------------------------------------
SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_process_definition' AND COLUMN_NAME = 'form_id';

-- =============================================================================
-- OPTIONAL：wf_instance.form_id 一并放宽为可空（启动无表单流程时需要）
--   如确认所有流程都绑定表单，可删除本段。
-- =============================================================================
SET @v_tbl2 = 'wf_instance';
SET @v_is_nullable2 = (
    SELECT UPPER(IS_NULLABLE)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl2 AND COLUMN_NAME = @v_col
);

SET @v_sql2 = IF(
    @v_is_nullable2 = 'NO',
    'ALTER TABLE `wf_instance` MODIFY COLUMN `form_id` BIGINT NULL '
        'COMMENT ''表单ID（workflow_bill.id），可空''',
    'SELECT ''SKIP: wf_instance.form_id already nullable'' AS migration_info'
);
PREPARE _stmt2 FROM @v_sql2; EXECUTE _stmt2; DEALLOCATE PREPARE _stmt2;

SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_instance' AND COLUMN_NAME = 'form_id';
