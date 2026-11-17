-- =============================================================================
-- 迁移脚本 V2026.11.17_002：数据范围规则增加「条件化授权」列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.11.17_002
-- 目标库    : blade
-- 影响表    : modedatasharerule
-- 变更类型  : 结构（新增 4 列，均 nullable，存量数据不受影响）
-- 是否幂等  : 否（MySQL 无 ADD COLUMN IF NOT EXISTS；重复执行会报 Duplicate column）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE modedatasharerule
--     DROP COLUMN condition_field, DROP COLUMN condition_op,
--     DROP COLUMN condition_value, DROP COLUMN condition_enabled;
--
-- 背景
--   E9 的 modedatasharerule 通过 conditionsql 做条件化授权（可见金额>1万的单据等），
--   SB 此前仅支持「命中共享规则=全量，否则仅本人」二分。
--   本脚本采用「结构化条件」而非裸 SQL 片段：字段名走白名单、操作符枚举、值参数化，
--   从根源杜绝 SQL 注入。命中条件的行才可见（与主体授权 AND 叠加）。
-- =============================================================================

USE `blade`;

ALTER TABLE modedatasharerule
  ADD COLUMN condition_field varchar(64)   NULL COMMENT '条件字段（workflow_billfield.fielddbname，白名单校验）' AFTER creator_self,
  ADD COLUMN condition_op    varchar(16)   NULL COMMENT '操作符：eq/ne/gt/ge/lt/le/like/in' AFTER condition_field,
  ADD COLUMN condition_value varchar(500)  NULL COMMENT '条件值（in 用逗号分隔；参数化拼接）' AFTER condition_op,
  ADD COLUMN condition_enabled tinyint     NOT NULL DEFAULT 0 COMMENT '是否启用条件化授权：1是 0否' AFTER condition_value;

-- 校验输出
SHOW COLUMNS FROM modedatasharerule;
