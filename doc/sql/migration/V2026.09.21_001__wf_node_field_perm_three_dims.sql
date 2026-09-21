-- -----------------------------------------------------------------------------
-- 迁移脚本：wf_node_field_perm 由「perm 单列」升级为「三维度 + 兼容列」
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.21_001
-- 目标库    : blade_workflow
-- 影响表    : wf_node_field_perm
-- 变更类型  : 加列（DDL）+ 存量回填（DML）
-- 是否幂等  : 是（列已存在则跳过；回填只更新 is_* 为 NULL 的行）
-- 是否丢数据: 否（保留 perm 列，不回填不删除）
-- 回滚脚本  : ALTER TABLE `wf_node_field_perm`
--                 DROP COLUMN `is_visible`,
--                 DROP COLUMN `is_editable`,
--                 DROP COLUMN `is_required`;
--             （perm 列始终保留，回滚后旧读取路径立即恢复）
--
-- 背景（对齐 ecology workflow_nodeform 的三列 isview / iseditable / ismandatory）
--   ecology 用三列独立表示字段属性，每列 0/1/2/3（0 不选 / 1 选 / 2 禁用不选 / 3 禁用选）；
--   本项目此前压成单列 `perm`（0 隐藏 / 1 只读 / 2 可编辑 / 3 必填），
--   由此**丢失了组合维度**——例如「显示但非必填且不可编辑」与
--   「显示 + 可编辑 + 必填」无法分别表达，且无法表达 ecology 的独立勾选。
--
--   本迁移改为三个独立列（Boolean 语义），仍保留 `perm` 作为**兼容派生列**：
--     is_visible  字段是否显示  1=显示 0=隐藏
--     is_editable 字段是否可编辑 1=可编辑 0=只读
--     is_required 字段是否必填  1=必填 0=非必填
--
--   派生规则（双写，保证老消费方不受影响）：
--     perm = is_required=1 ? 3 : is_editable=1 ? 2 : is_visible=1 ? 1 : 0
--
--   存量回填规则（perm → 三维度）：
--     perm=0 → (0,0,0) 隐藏
--     perm=1 → (1,0,0) 只读
--     perm=2 → (1,1,0) 可编辑
--     perm=3 → (1,1,1) 必填
--     perm IS NULL → 按前端历史默认「可编辑」处理 (1,1,0)
--
--   读取规则（服务层）：三列全为 NULL 视为存量行，按 perm 现推；否则以三列为准。
--
-- ⚠️ 执行顺序：先执行本脚本，再重启 blade-workflow。
-- ⚠️ 严禁使用 workflow-schema.sql 同步结构（会 DROP TABLE 清空数据）。
-- =============================================================================

USE `blade_workflow`;

SET @v_db  = DATABASE();
SET @v_tbl = 'wf_node_field_perm';

-- is_visible ------------------------------------------------------------------
SET @v_col = 'is_visible';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_node_field_perm`
         ADD COLUMN `is_visible` TINYINT NULL
         COMMENT ''字段是否显示 1=显示 0=隐藏 NULL=存量未迁移（按 perm 推导）''
         AFTER `perm`',
    'SELECT ''SKIP: wf_node_field_perm.is_visible already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- is_editable -----------------------------------------------------------------
SET @v_col = 'is_editable';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_node_field_perm`
         ADD COLUMN `is_editable` TINYINT NULL
         COMMENT ''字段是否可编辑 1=可编辑 0=只读 NULL=存量未迁移（按 perm 推导）''
         AFTER `is_visible`',
    'SELECT ''SKIP: wf_node_field_perm.is_editable already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- is_required -----------------------------------------------------------------
SET @v_col = 'is_required';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `wf_node_field_perm`
         ADD COLUMN `is_required` TINYINT NULL
         COMMENT ''字段是否必填 1=必填 0=非必填 NULL=存量未迁移（按 perm 推导）''
         AFTER `is_editable`',
    'SELECT ''SKIP: wf_node_field_perm.is_required already exists'' AS migration_info'
);
PREPARE _stmt FROM @v_sql; EXECUTE _stmt; DEALLOCATE PREPARE _stmt;

-- 存量回填（只更新 is_* 尚为 NULL 的行 → 重复执行无副作用）------------------------
UPDATE `wf_node_field_perm`
SET
    `is_visible`  = CASE WHEN `perm` IS NULL THEN 1 WHEN `perm` >= 1 THEN 1 ELSE 0 END,
    `is_editable` = CASE WHEN `perm` IS NULL THEN 1 WHEN `perm` >= 2 THEN 1 ELSE 0 END,
    `is_required` = CASE WHEN `perm` = 3 THEN 1 ELSE 0 END
WHERE `is_visible` IS NULL
   OR `is_editable` IS NULL
   OR `is_required` IS NULL;

-- 校验 ------------------------------------------------------------------------
-- ① 三列应各返回 1（存在）
-- ② 待迁移行数应返回 0（全部已回填）
SELECT
    (SELECT COUNT(1) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_field_perm'
        AND COLUMN_NAME = 'is_visible')                       AS is_visible_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_field_perm'
        AND COLUMN_NAME = 'is_editable')                      AS is_editable_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_node_field_perm'
        AND COLUMN_NAME = 'is_required')                      AS is_required_should_be_1,
    (SELECT COUNT(1) FROM `wf_node_field_perm`
      WHERE `is_visible` IS NULL
         OR `is_editable` IS NULL
         OR `is_required` IS NULL)                            AS rows_pending_backfill_should_be_0;
