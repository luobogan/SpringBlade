-- =============================================================================
-- 迁移脚本 V2026.11.17_005：数据范围/单条共享支持矩阵主体（objtype=7）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.11.17_005
-- 目标库    : blade
-- 影响表    : modedatasharerule、modedatashare_single
-- 变更类型  : 结构（各新增 1 列，nullable，存量行为不变）
-- 是否幂等  : 否（MySQL 无 ADD COLUMN IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  : ALTER TABLE modedatasharerule DROP COLUMN matrix_value_field;
--             ALTER TABLE modedatashare_single DROP COLUMN matrix_value_field;
--
-- 背景
--   E9 sharetype=7（矩阵）：按当前数据行的条件字段值，从矩阵表解析出有权人员。
--   SB 复用已有 condition_field 列承载「业务表条件字段名」，新增 matrix_value_field
--   承载「矩阵结果字段名」（wf_matrix_field.field_type=1 的 field_key）。
--   objid 承载 matrixId。其余 condition_op/value 与矩阵主体无关。
-- =============================================================================

USE `blade`;

ALTER TABLE modedatasharerule
  ADD COLUMN matrix_value_field varchar(64) NULL COMMENT '矩阵主体结果字段名（objtype=7 时有效，wf_matrix_field.field_type=1 的 field_key）' AFTER condition_enabled;

ALTER TABLE modedatashare_single
  ADD COLUMN condition_field varchar(64) NULL COMMENT '矩阵主体业务条件字段名（objtype=7 时有效；复用 modedatasharerule 同名字段语义）' AFTER objid,
  ADD COLUMN matrix_value_field varchar(64) NULL COMMENT '矩阵主体结果字段名（objtype=7 时有效）' AFTER condition_field;

SHOW COLUMNS FROM modedatasharerule LIKE 'matrix%';
SHOW COLUMNS FROM modedatashare_single LIKE 'matrix%';
