-- =============================================================================
-- 迁移脚本 004：wf_process_definition 补自由流程类型字段
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.10_004
-- 目标库    : blade_workflow
-- 影响表    : wf_process_definition
-- 变更类型  : 加列（DDL）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `wf_process_definition` DROP COLUMN `free_wf_type`;
--
-- 背景
--   对齐 ecology「添加路径」基本设置：启用自由流程（is_free=1）后，需选择
--   「自由流程类型（简易/高级）」newFreeWfType。本列对应此字段。
--   实体 WfProcessDefinition 已同步增加 freeWfType（前端 condition 驱动表单按
--   visibleWhen(isFree==true) 条件显示该下拉）。
--
-- ⚠️ 执行顺序：先执行本脚本，再重启/重部署 blade-workflow。
-- =============================================================================

USE `blade_workflow`;

SET @v_db  = DATABASE();
SET @v_tbl = 'wf_process_definition';

-- free_wf_type ---------------------------------------------------------------
SET @v_col = 'free_wf_type';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_process_definition`
         ADD COLUMN `free_wf_type` TINYINT NULL DEFAULT 1
         COMMENT ''自由流程类型：1简易 2高级（对齐 ecology newFreeWfType）''
         AFTER `is_free`',
    'SELECT ''SKIP: wf_process_definition.free_wf_type already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 校验（应返回 1）--------------------------------------------------------------
SELECT COUNT(1) AS free_wf_type_should_be_1
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'wf_process_definition'
  AND COLUMN_NAME  = 'free_wf_type';
