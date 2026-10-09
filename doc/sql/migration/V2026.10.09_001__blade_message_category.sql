-- V2026.10.09_001__blade_message_category.sql
-- 流程消息集成消息中心（消息中心设计文档 §10，D5 分类模型）：
--   1) blade_message 增加 category 列：1=聊天 2=流程通知；
--   2) 增加 (biz_ref_type, biz_ref_id) 索引：为二期「审批完成后回写历史通知状态」
--      （按业务引用反查消息，对齐 ecology updateBizState）预留查询路径。
-- 幂等：列/索引已存在则跳过，可重复执行。

SET @db = DATABASE();

-- ---------- 1. category 列 ----------
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'blade_message'
      AND COLUMN_NAME = 'category'
);
SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE blade_message ADD COLUMN category INT NOT NULL DEFAULT 1 COMMENT ''消息分类 1=聊天 2=流程通知'' AFTER content_type',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 2. 业务引用索引（二期状态回写用） ----------
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'blade_message'
      AND INDEX_NAME = 'idx_blade_message_biz_ref'
);
SET @sql = IF(
    @idx_exists = 0,
    'ALTER TABLE blade_message ADD INDEX idx_blade_message_biz_ref (biz_ref_type, biz_ref_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 3. 存量数据兜底：分类置为聊天 ----------
UPDATE blade_message SET category = 1 WHERE category IS NULL OR category = 0;
