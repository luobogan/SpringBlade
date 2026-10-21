-- -----------------------------------------------------------------------------
-- 迁移脚本：公文应用设置 增加「默认红头模板」「默认流程」字段
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.21_030
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_setting
-- 变更类型  : DDL（幂等）
-- 是否幂等  : 是（按 information_schema 守卫，列已存在则跳过）
-- 回滚      : ALTER TABLE wf_odoc_setting DROP COLUMN default_taohong_mould_id;
--             ALTER TABLE wf_odoc_setting DROP COLUMN default_process_def_id;
--
-- 背景
--   应用设置页原为模块开关 + 新建流程默认值（版式/套红/签章）。现补充「默认红头模板」与
--   「默认流程」，使新建公文流程时自动带入，减少重复配置（红头模板默认此前只在流程级
--   wf_odoc_process_set.taohong_mould_id，本列作为新建默认值）。

SET @t = 'wf_odoc_setting';
SET @h1 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'default_taohong_mould_id');
SET @d1 = IF(@h1 = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN default_taohong_mould_id BIGINT NULL DEFAULT NULL COMMENT ''新建流程默认红头模板ID(关联wf_odoc_mould)'''), 'SELECT 1');
PREPARE s1 FROM @d1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @h2 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'default_process_def_id');
SET @d2 = IF(@h2 = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN default_process_def_id VARCHAR(128) NULL DEFAULT NULL COMMENT ''默认流程定义ID或流程key(新建公文默认带入)'''), 'SELECT 1');
PREPARE s2 FROM @d2; EXECUTE s2; DEALLOCATE PREPARE s2;
