-- =====================================================================
-- V2026.10.20_003  表单建模：模块/权限外键列 int → bigint
-- ---------------------------------------------------------------------
-- 背景（数据模型缺陷）：
--   modeinfo.id / moderightinfo.id / modefieldauthorize.id 均为 bigint（雪花），
--   但下列「指向模块/表单」的外键列是 int，导致：
--     1) 自建模块：modeinfo.id 是 19 位雪花 → 存不进 moderightinfo.modeid /
--        modefieldauthorize.modeid → 模块权限 / 字段权限对自建模块完全失效；
--     2) ecology 迁移表单：workflow_bill.id 是 19 位雪花 → 存不进 modeinfo.billid
--        → 「按表单反查模块」（resolveModeIdByFormId）对迁移表单永远查不到；
--     3) 代码侧只能「超 int 范围即跳过校验并告警」（降级），权限/唯一性形同虚设。
--
-- 本脚本范围（仅改 3 列，最小必要）：
--   modeinfo.billid            int → bigint   （表单反查模块链）
--   moderightinfo.modeid       int → bigint   （模块权限）
--   modefieldauthorize.modeid  int → bigint   （字段权限）
--
-- 为什么只改这 3 列：
--   blade.sql 中 `modeid` int 共 14 处（涉及 14 张表），但其余 11 张表
--   （mode_customsearch / mode_customtree / modeformfield / modefieldattr /
--    modeformgroup / moderightinfo_matrix 等）对应的功能（自定义查询、树、
--    明细组、字段属性）本项目尚未建设、当前无实际数据与运行期消费，
--   提前迁移属于「无收益 DDL 变更」。待对应功能建设时同批迁移，见文末清单。
--
-- 执行要求：
--   1) 低峰执行（int→bigint 会重建表，MySQL 8.0 默认 ALGORITHM=INPLACE，
--      允许并发 DML，但大表仍需窗口）；
--   2) 幂等：仅当列当前为 int 时才执行 ALTER（可重复执行）；
--   3) 类型拓宽（int→bigint）值域兼容，存量数据不丢、不需转换。
-- =====================================================================


-- ---------- 0. 迁移前快照（用于迁移后比对，确认数据零丢失） ----------
SELECT 'modeinfo'           AS tbl, COUNT(*) AS rows_before FROM modeinfo
UNION ALL SELECT 'moderightinfo',        COUNT(*) FROM moderightinfo
UNION ALL SELECT 'modefieldauthorize',   COUNT(*) FROM modefieldauthorize;


-- ---------- 1. modeinfo.billid  int → bigint ----------
SET @s := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'modeinfo'
      AND column_name = 'billid' AND data_type = 'bigint') > 0,
  'SELECT ''modeinfo.billid 已为 bigint，跳过'' AS msg',
  'ALTER TABLE `modeinfo` MODIFY COLUMN `billid` bigint DEFAULT NULL COMMENT ''关联表单ID'''
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ---------- 2. moderightinfo.modeid  int → bigint ----------
SET @s := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'moderightinfo'
      AND column_name = 'modeid' AND data_type = 'bigint') > 0,
  'SELECT ''moderightinfo.modeid 已为 bigint，跳过'' AS msg',
  'ALTER TABLE `moderightinfo` MODIFY COLUMN `modeid` bigint DEFAULT NULL COMMENT ''模块ID'''
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ---------- 3. modefieldauthorize.modeid  int → bigint ----------
SET @s := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'modefieldauthorize'
      AND column_name = 'modeid' AND data_type = 'bigint') > 0,
  'SELECT ''modefieldauthorize.modeid 已为 bigint，跳过'' AS msg',
  'ALTER TABLE `modefieldauthorize` MODIFY COLUMN `modeid` bigint DEFAULT NULL COMMENT ''模块ID'''
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ---------- 4. 迁移后校验（类型 + 数据量一致） ----------
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'modeinfo'          AND column_name = 'billid')
    OR (table_name = 'moderightinfo'     AND column_name = 'modeid')
    OR (table_name = 'modefieldauthorize' AND column_name = 'modeid'))
ORDER BY table_name;

-- 与第 0 步的 rows_before 逐行比对，必须完全一致
SELECT 'modeinfo'           AS tbl, COUNT(*) AS rows_after FROM modeinfo
UNION ALL SELECT 'moderightinfo',        COUNT(*) FROM moderightinfo
UNION ALL SELECT 'modefieldauthorize',   COUNT(*) FROM modefieldauthorize;

-- 越界值检查（回滚前必查）：以下查询应返回 0 行，否则不可回滚
-- SELECT id, modeid FROM moderightinfo     WHERE modeid > 2147483647 OR modeid < -2147483648;
-- SELECT id, modeid FROM modefieldauthorize WHERE modeid > 2147483647 OR modeid < -2147483648;
-- SELECT id, billid FROM modeinfo          WHERE billid > 2147483647 OR billid < -2147483648;


-- =====================================================================
-- 回滚脚本（bigint → int；执行前必须先跑上方「越界值检查」确认 0 行）
-- ---------------------------------------------------------------------
-- SET @s := (SELECT IF(
--   (SELECT COUNT(*) FROM information_schema.columns
--     WHERE table_schema = DATABASE() AND table_name = 'modeinfo'
--       AND column_name = 'billid' AND data_type = 'int') > 0,
--   'SELECT ''already int'' AS msg',
--   'ALTER TABLE `modeinfo` MODIFY COLUMN `billid` int DEFAULT NULL COMMENT ''关联表单ID'''
-- ));
-- PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
--
-- SET @s := (SELECT IF(
--   (SELECT COUNT(*) FROM information_schema.columns
--     WHERE table_schema = DATABASE() AND table_name = 'moderightinfo'
--       AND column_name = 'modeid' AND data_type = 'int') > 0,
--   'SELECT ''already int'' AS msg',
--   'ALTER TABLE `moderightinfo` MODIFY COLUMN `modeid` int DEFAULT NULL COMMENT ''模块ID'''
-- ));
-- PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
--
-- SET @s := (SELECT IF(
--   (SELECT COUNT(*) FROM information_schema.columns
--     WHERE table_schema = DATABASE() AND table_name = 'modefieldauthorize'
--       AND column_name = 'modeid' AND data_type = 'int') > 0,
--   'SELECT ''already int'' AS msg',
--   'ALTER TABLE `modefieldauthorize` MODIFY COLUMN `modeid` int DEFAULT NULL COMMENT ''模块ID'''
-- ));
-- PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- =====================================================================


-- =====================================================================
-- 附：blade.sql 中其余 `modeid` int 列清单（本批**不迁移**，待功能建设时同批）
--   已知位置（行号为 blade.sql）：4787 / 8788(mode_customsearch) /
--   8847(mode_customtree) / 8909 / 9034 / 9060 / 9092 / 9184 / 9216 /
--   9245 / 9332(modeformfield) / 9364(modefieldattr) / 9439(本批已改)
--   说明：这些表对应「自定义查询 / 目录树 / 布局字段 / 字段属性 / 明细组」等
--   尚未建设的能力，当前无数据、无运行期消费；提前改属无收益 DDL 变更。
-- =====================================================================
