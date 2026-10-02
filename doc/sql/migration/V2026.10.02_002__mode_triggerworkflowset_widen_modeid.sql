-- =============================================================================
-- 迁移脚本：mode_triggerworkflowset.modeid 由 INT 扩为 BIGINT
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.02_002
-- 目标库    : blade（formmode 业务库，与 workflow_bill / mode_* 同库）
-- 影响表    : mode_triggerworkflowset
-- 变更类型  : 改列类型（DDL，扩大，不丢数据）
-- 是否幂等  : 是（已是 bigint 则跳过）
-- 是否丢数据: 否（INT → BIGINT 为兼容扩展）
-- 回滚脚本  : ALTER TABLE `mode_triggerworkflowset` MODIFY COLUMN `modeid` INT NULL;
--             （回滚仅在确认无 >2147483647 的值时执行）
--
-- 背景（隐患记录，静默故障）
--   现状：mode_triggerworkflowset.modeid 为 INT（上限 2147483647）。
--   语义：modeid 存 modeinfo.id，而 modeinfo.id 为 BIGINT 雪花 ID
--         （典型值如 1123598815738675999，远超 INT 上限）。
--   问题：把雪花 modeinfo.id 写入该 INT 列会发生**截断/溢出且不抛异常**，
--         导致触发器关联到错误模块（甚至永远匹配不上），属静默故障。
--   配套：本脚本与实体字段 Integer→Long（ModeTriggerWorkflowSet.modeid）
--         及 ApprovalTriggerServiceImpl 去除 .intValue() 配套落地。
-- =============================================================================

SET @v_db   = DATABASE();
SET @v_tbl  = 'mode_triggerworkflowset';
SET @v_col  = 'modeid';

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
    'SELECT ''ERROR: column mode_triggerworkflowset.modeid not found'' AS migration_info',
    IF(
        LOWER(@v_type) = 'bigint',
        'SELECT ''SKIP: modeid is already BIGINT'' AS migration_info',
        'ALTER TABLE `mode_triggerworkflowset`
             MODIFY COLUMN `modeid` BIGINT NULL
             COMMENT ''模块ID（modeinfo.id，BIGINT 雪花）'''
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
  AND COLUMN_NAME  = 'modeid';
