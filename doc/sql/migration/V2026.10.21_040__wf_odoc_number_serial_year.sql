-- -----------------------------------------------------------------------------
-- 迁移脚本：文号规则 增加「流水所属年份」字段（跨年 serial 归零基准）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.21_040
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_number
-- 变更类型  : DDL（幂等）
-- 是否幂等  : 是（按 information_schema 守卫，列已存在则跳过）
-- 回滚      : ALTER TABLE wf_odoc_number DROP COLUMN serial_year;
--
-- 背景
--   发文/收文取号需支持「跨年流水归零」：进入新的一年后 serial 从 1 重新计数。
--   用 serial_year 记录当前 serial 计数器所属的年份，取号时若与当前年份不同则先归零再自增。

SET @t = 'wf_odoc_number';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'serial_year');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN serial_year INT DEFAULT NULL COMMENT ''流水所属年份(跨年归零基准)'' AFTER serial'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;
