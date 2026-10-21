-- -----------------------------------------------------------------------------
-- 迁移脚本：文号规则 增加「归属机构代字」字段（打通 org_abbr 字典）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.21_020
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_number
-- 变更类型  : DDL（幂等）
-- 是否幂等  : 是（按 information_schema 守卫，列已存在则跳过）
-- 回滚      : ALTER TABLE wf_odoc_number DROP COLUMN org_abbr_id;
--
-- 背景
--   文号机关代字（prefix）此前是自由文本手填。现接入 wf_odoc_dict(type=org_abbr) 机构代字
--   字典，维护页从字典下拉选择（而非手填）。org_abbr_id 仅作追溯，取号合成仍以 prefix 文本为准。

SET @t = 'wf_odoc_number';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'org_abbr_id');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN org_abbr_id BIGINT NULL DEFAULT NULL COMMENT ''归属机构代字(对应wf_odoc_dict org_abbr id)'' AFTER prefix'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;
