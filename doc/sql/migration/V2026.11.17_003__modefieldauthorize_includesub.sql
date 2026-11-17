-- =============================================================================
-- 迁移脚本 V2026.11.17_003：字段授权表增加「含子部门」列
-- -----------------------------------------------------------------------------
-- 版本      : V2026.11.17_003
-- 目标库    : blade
-- 影响表    : modefieldauthorize
-- 变更类型  : 结构（新增 1 列，nullable，默认 0，存量行为不变）
-- 是否幂等  : 否（MySQL 无 ADD COLUMN IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  : ALTER TABLE modefieldauthorize DROP COLUMN includesub;
--
-- 背景
--   E9 的部门授权支持「含子部门」（orgrelation 展开），SB 的模块权限表 moderightinfo 已有
--   includesub，但字段授权表 modefieldauthorize 缺失该列，导致字段权限的部门授权只能精确匹配。
--   本列补齐后，字段权限部门授权可展开子孙部门（与模块权限同口径）。
-- =============================================================================

USE `blade`;

ALTER TABLE modefieldauthorize
  ADD COLUMN includesub tinyint NOT NULL DEFAULT 0 COMMENT '授权部门是否含子部门：1是 0否（默认否，存量行为不变）' AFTER objid;

SHOW COLUMNS FROM modefieldauthorize;
