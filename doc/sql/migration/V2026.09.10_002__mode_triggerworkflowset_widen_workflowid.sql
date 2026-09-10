-- =============================================================================
-- 迁移脚本 002：mode_triggerworkflowset.workflowid 由 INT 扩为 BIGINT
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.10_002
-- 目标库    : blade（formmode 业务库，与 workflow_bill / mode_* 同库）
-- 影响表    : mode_triggerworkflowset
-- 变更类型  : 改列类型（DDL，扩大，不丢数据）
-- 是否幂等  : 是（已是 bigint 则跳过）
-- 是否丢数据: 否（INT → BIGINT 为兼容扩展）
-- 回滚脚本  : ALTER TABLE `mode_triggerworkflowset` MODIFY COLUMN `workflowid` INT NULL;
--             （回滚仅在确认无 >2147483647 的值时执行）
--
-- 背景（隐患记录，静默故障）
--   现状：mode_triggerworkflowset.workflowid 为 INT（上限 2147483647）。
--   调用：ApprovalTriggerServiceImpl.startWorkflowInstance() 中
--         dto.setDefId(triggerSet.getWorkflowid().longValue());
--   问题：新流程定义主键 wf_process_definition.id 为 BIGINT UNSIGNED（雪花 ID，
--         典型值如 1123598815738675999，远超 INT 上限）。把新定义 ID 写回该 INT 列
--         会发生**截断/溢出且不抛异常**，导致触发的流程指向错误定义，属静默故障。
--
-- 语义约定（重要）
--   扩列后 workflowid 统一存**新流程定义 ID（wf_process_definition.id）**；
--   ecology 存量流程 ID 不再直接存此列，改为经 wf_migration_map 映射后使用
--   （映射逻辑当前尚未实现，见《审批流程-开发任务说明》B3 / D4）。
-- =============================================================================

-- ⚠️ 注意：请确认 mode_triggerworkflowset 实际所在库后再执行本脚本，
--    若不在默认库请把下面的 USE 改为正确的库名。
-- USE `blade`;

SET @v_db   = DATABASE();
SET @v_tbl  = 'mode_triggerworkflowset';
SET @v_col  = 'workflowid';

-- 读取当前列类型
SET @v_type = (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db
      AND TABLE_NAME   = @v_tbl
      AND COLUMN_NAME  = @v_col
);

SET @v_sql = IF(
    @v_type IS NULL,
    'SELECT ''ERROR: column mode_triggerworkflowset.workflowid not found'' AS migration_info',
    IF(
        LOWER(@v_type) = 'bigint',
        'SELECT ''SKIP: workflowid is already BIGINT'' AS migration_info',
        'ALTER TABLE `mode_triggerworkflowset`
             MODIFY COLUMN `workflowid` BIGINT NULL
             COMMENT ''流程定义ID（wf_process_definition.id，BIGINT）'''
    )
);

PREPARE _stmt FROM @v_sql;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 校验（COLUMN_TYPE 应为 bigint）
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'mode_triggerworkflowset'
  AND COLUMN_NAME  = 'workflowid';
