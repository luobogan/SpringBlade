-- -----------------------------------------------------------------------------
-- 迁移脚本：文号规则 增加「跨年流水归零开关」字段
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.21_060
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_number
-- 变更类型  : DDL（幂等）
-- 是否幂等  : 是（按 information_schema 守卫，列已存在则跳过）
-- 回滚      : ALTER TABLE wf_odoc_number DROP COLUMN year_reset;
--
-- 背景
--   取号逻辑支持「跨年流水归零」：进入新历年 serial 从 1 重计。部分文号（如档案/累计类）
--   希望跨年不断号，故加 year_reset 开关：1=归零（默认）、0=累计（跨年不重置）。

SET @t = 'wf_odoc_number';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'year_reset');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN year_reset tinyint NOT NULL DEFAULT 1 COMMENT ''跨年流水是否归零 1=归零 0=累计'' AFTER is_deleted'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;

-- 示例值：收文测试文号 设为「累计」(跨年不断号) 演示；其余规则保持默认「归零」。
-- 无对应行时不报错（影响 0 行），幂等可重复执行。
UPDATE wf_odoc_number SET year_reset = 0 WHERE type_name = '收文测试文号';
