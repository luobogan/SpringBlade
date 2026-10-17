-- -----------------------------------------------------------------------------
-- 修正迁移：为 odoc 新表补 create_dept 列（TenantEntity 基类字段，初版 CREATE TABLE 漏写）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_110
-- 目标库    : blade（dev 实际库；wf_odoc_* 所在库）
-- 影响表    : wf_odoc_dict / wf_odoc_opinion / wf_odoc_receive_unit / wf_odoc_receive_group
--             wf_odoc_setting / wf_odoc_secret_rule / wf_odoc_list_set
-- 变更类型  : DDL（幂等）
-- 是否幂等  : 是（按 information_schema 守卫，列已存在则跳过）
-- 回滚      : 可选 ALTER TABLE <t> DROP COLUMN create_dept;（一般无需回滚）
--
-- 背景
--   WfOdoc* 实体继承 TenantEntity，MP 生成 SELECT 含 create_dept；初版 CREATE TABLE
--   (V020/030/060/080/100) 漏写该列，运行期查询报 "Unknown column 'create_dept'"。
--   本脚本对 7 张表统一补齐（缺则补、在则跳过），保证全新库与存量库一致。

SET @t = 'wf_odoc_dict';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'create_dept');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN create_dept bigint NULL DEFAULT NULL COMMENT ''创建部门'' AFTER create_user'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;

SET @t = 'wf_odoc_opinion';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'create_dept');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN create_dept bigint NULL DEFAULT NULL COMMENT ''创建部门'' AFTER create_user'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;

SET @t = 'wf_odoc_receive_unit';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'create_dept');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN create_dept bigint NULL DEFAULT NULL COMMENT ''创建部门'' AFTER create_user'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;

SET @t = 'wf_odoc_receive_group';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'create_dept');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN create_dept bigint NULL DEFAULT NULL COMMENT ''创建部门'' AFTER create_user'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;

SET @t = 'wf_odoc_setting';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'create_dept');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN create_dept bigint NULL DEFAULT NULL COMMENT ''创建部门'' AFTER create_user'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;

SET @t = 'wf_odoc_secret_rule';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'create_dept');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN create_dept bigint NULL DEFAULT NULL COMMENT ''创建部门'' AFTER create_user'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;

SET @t = 'wf_odoc_list_set';
SET @h = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @t AND COLUMN_NAME = 'create_dept');
SET @d = IF(@h = 0, CONCAT('ALTER TABLE ', @t, ' ADD COLUMN create_dept bigint NULL DEFAULT NULL COMMENT ''创建部门'' AFTER create_user'), 'SELECT 1');
PREPARE s FROM @d; EXECUTE s; DEALLOCATE PREPARE s;
