-- =============================================================================
-- 迁移脚本 008：form_layout 支持「按布局类型 / 流程节点」取布局
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.10_008
-- 目标库    : blade（formmode 所有表统一在 blade 库）
-- 影响表    : form_layout
-- 变更类型  : 加列 + 加索引（DDL）
-- 是否幂等  : 是（已存在则跳过，不报错）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `form_layout` DROP INDEX `idx_form_node_type`;
--   ALTER TABLE `form_layout` DROP COLUMN `node_key`;
--   ALTER TABLE `form_layout` DROP COLUMN `layout_type`;
--
-- 背景
--   对齐 ecology 的 layouttype 语义：同一表单可有多套布局（0编辑/1显示/3监控/4打印），
--   并可按流程节点（node_key）绑定不同布局。blade-workflow 组装「审批态渲染包」时
--   经 Feign 调 formmode 的 /form-layout/{formId}?layouttype=&nodeKey= 取布局，回退顺序：
--   节点级(指定类型) → 节点级(默认0) → 表单级(指定类型) → 表单级(默认0) → 任意最新一条。
--
-- ⚠️ 执行顺序：先执行本脚本，再重启/重部署 blade-formmode 与 blade-workflow。
-- =============================================================================

USE `blade`;

SET @v_db  = DATABASE();
SET @v_tbl = 'form_layout';

-- layout_type ----------------------------------------------------------------
SET @v_col = 'layout_type';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `form_layout`
         ADD COLUMN `layout_type` INT NOT NULL DEFAULT 0
         COMMENT ''布局类型：0编辑(默认) 1显示 3监控 4打印（对齐 ecology layouttype）''
         AFTER `layout_config`',
    'SELECT ''SKIP: form_layout.layout_type already exists'' AS migration_info'
);
PREPARE _stmt_layout_type FROM @v_sql; EXECUTE _stmt_layout_type; DEALLOCATE PREPARE _stmt_layout_type;

-- node_key -------------------------------------------------------------------
SET @v_col = 'node_key';
SET @v_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND COLUMN_NAME = @v_col
);
SET @v_sql = IF(
    @v_exists = 0,
    'ALTER TABLE `form_layout`
         ADD COLUMN `node_key` VARCHAR(64) DEFAULT NULL
         COMMENT ''绑定流程节点Key（空=通用，适用所有节点）''
         AFTER `layout_type`',
    'SELECT ''SKIP: form_layout.node_key already exists'' AS migration_info'
);
PREPARE _stmt_node_key FROM @v_sql; EXECUTE _stmt_node_key; DEALLOCATE PREPARE _stmt_node_key;

-- 索引 idx_form_node_type -----------------------------------------------------
SET @v_idx = 'idx_form_node_type';
SET @v_idx_exists = (
    SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @v_db AND TABLE_NAME = @v_tbl AND INDEX_NAME = @v_idx
);
SET @v_sql = IF(
    @v_idx_exists = 0,
    'ALTER TABLE `form_layout` ADD INDEX `idx_form_node_type` (`form_id`, `node_key`, `layout_type`)',
    'SELECT ''SKIP: form_layout.idx_form_node_type already exists'' AS migration_info'
);
PREPARE _stmt_idx FROM @v_sql; EXECUTE _stmt_idx; DEALLOCATE PREPARE _stmt_idx;

-- 校验（两列应各返回 1）------------------------------------------------------
SELECT
    (SELECT COUNT(1) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'form_layout' AND COLUMN_NAME = 'layout_type') AS layout_type_should_be_1,
    (SELECT COUNT(1) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'form_layout' AND COLUMN_NAME = 'node_key') AS node_key_should_be_1;
