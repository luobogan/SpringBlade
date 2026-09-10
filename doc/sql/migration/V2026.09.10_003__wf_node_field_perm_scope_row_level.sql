-- =============================================================================
-- 迁移脚本 003：wf_node_field_perm.scope 支持行级（dt{idx}_r{row}）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.10_003
-- 目标库    : blade_workflow
-- 影响表    : wf_node_field_perm
-- 变更类型  : 扩列长度 + 更新注释（**不改动数据结构语义，不影响现有数据**）
-- 是否幂等  : 是
-- 是否丢数据: 否（VARCHAR 32 → 64 为兼容扩展）
-- 回滚脚本  : ALTER TABLE `wf_node_field_perm`
--             MODIFY COLUMN `scope` VARCHAR(32) NOT NULL COMMENT 'main 或 dt{idx}';
--
-- 决策依据（用户 2026-09-10 拍板）
--   节点字段权限的 scope **要支持行级** dt{idx}_r{row}，对齐《审批流程方案》
--   §3.2 决策 2 与前端 data-excelp-scope（现已支持到行级）。
--
-- 现状核对（无需改结构的原因）
--   1. 列 `scope` 已是 VARCHAR(32)，容纳 "dt12_r999" 这类值本就够用；
--      本脚本扩到 VARCHAR(64) 仅为主表多行/大序号预留余量。
--   2. 唯一键 uk_node_scope_field(def_id, node_key, scope, field_name) **已兼容行级**，
--      行级授权按 (行, 字段) 独立成行存储，不冲突。
--   → 因此本迁移**只改注释与长度**，真正的改动在应用层：
--      · 渲染端匹配回退规则：精确行级 dt{idx}_r{row} → 明细表级 dt{idx} → 主表级 main → fieldAttr
--      · 前端权限矩阵 UI 支持按明细行展开设置
--
-- ⚠️ 已知风险（待确认，见《审批流程-开发任务说明》D1）
--   明细行可动态增/删/排序 → 行号会漂移，行级授权可能错位。
--   一期折中：行级授权仅作用于"模板行/固定行"，新增行回退到 dt{idx} 级。
-- =============================================================================

USE `blade_workflow`;

SET @v_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'wf_node_field_perm'
      AND COLUMN_NAME  = 'scope'
);

SET @v_sql = IF(
    @v_exists = 0,
    'SELECT ''ERROR: column wf_node_field_perm.scope not found'' AS migration_info',
    'ALTER TABLE `wf_node_field_perm`
         MODIFY COLUMN `scope` VARCHAR(64) NOT NULL
         COMMENT ''权限作用域：main（主表）| dt{idx}（明细表整表）| dt{idx}_r{row}（明细行级）'''
);

PREPARE _stmt FROM @v_sql;
EXECUTE _stmt;
DEALLOCATE PREPARE _stmt;

-- 校验
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'wf_node_field_perm'
  AND COLUMN_NAME  = 'scope';

-- 存量数据说明：脚本不改写任何已有 scope 值。
-- 历史数据仅为 'main' 与 'dt{idx}' 两种粒度，按回退规则仍可正常命中。
SELECT scope, COUNT(1) AS cnt
FROM `wf_node_field_perm`
WHERE is_deleted = 0
GROUP BY scope;
