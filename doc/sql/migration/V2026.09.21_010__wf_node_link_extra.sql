-- =============================================================================
-- 版本    ：V2026.09.21_010
-- 目标库  ：blade_workflow
-- 影响表  ：wf_node_link
-- 变更类型：加字段
-- 是否幂等：是（information_schema 存在性守卫 + 跳过提示）
-- 是否丢数：否
-- 回滚脚本：ALTER TABLE `wf_node_link` DROP COLUMN `extra_operations`;
-- 决策依据：流程节点信息设置项补齐（项8 出口级附加操作）。出口（连线）上的附加操作脚本，
--           离开该出口时执行（http/sql/field/action 前缀分派，复用 WfActionExecutor），对齐 E9 连线附加操作。
-- =============================================================================

USE `blade_workflow`;

SET @v_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'wf_node_link'
    AND COLUMN_NAME = 'extra_operations'
);

SET @v_sql = IF(
  @v_exists = 0,
  'ALTER TABLE `wf_node_link`
     ADD COLUMN `extra_operations` TEXT NULL COMMENT ''出口级附加操作脚本（多行，前缀分派；离开本出口时执行）''',
  'SELECT ''skip: column extra_operations already exists'' AS migration_info'
);

PREPARE _stmt FROM @v_sql;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 尾部校验
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'wf_node_link'
  AND COLUMN_NAME = 'extra_operations';
