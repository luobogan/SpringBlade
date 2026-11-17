-- =============================================================================
-- 迁移脚本 V2026.11.17_006：安全级别（密级）轴能力框架
-- -----------------------------------------------------------------------------
-- 版本      : V2026.11.17_006
-- 目标库    : blade
-- 影响表    : blade_user、moderightinfo、modedatasharerule、modedatashare_single
-- 变更类型  : 结构（新增列，均有默认值，存量行为不变）
-- 是否幂等  : 否（MySQL 无 ADD COLUMN IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  : ALTER TABLE blade_user DROP COLUMN security_level;
--             ALTER TABLE moderightinfo DROP COLUMN showlevel, DROP COLUMN showlevel2;
--             ALTER TABLE modedatasharerule DROP COLUMN showlevel, DROP COLUMN showlevel2;
--             ALTER TABLE modedatashare_single DROP COLUMN showlevel, DROP COLUMN showlevel2;
--
-- 背景
--   E9 存在独立于 righttype/sharelevel 的「第三权限轴」：showlevel ≤ 用户seclevel ≤ showlevel2。
--   SB 此前完全没有该维度。本脚本搭建能力框架，但：
--     ① 全局开关 blade.formmode.seclevel.enforce 默认 false —— 未配置密级时零影响；
--     ② blade_user.security_level 默认 0（= 最低密级），不自动给存量用户赋值；
--     ③ 需业务方定义密级标准（0~100 分几档、谁有权限赋密级）后再启用。
--   注意：不复用 member_level（那是商城会员等级，语义不同）。
-- =============================================================================

USE `blade`;

ALTER TABLE blade_user
  ADD COLUMN security_level int NOT NULL DEFAULT 0 COMMENT '数据密级（0~100，E9 seclevel 语义；默认0=最低，不自动赋值）';

ALTER TABLE moderightinfo
  ADD COLUMN showlevel int NULL COMMENT '密级下限（E9 showlevel，0~100）' AFTER includesub,
  ADD COLUMN showlevel2 int NULL COMMENT '密级上限（E9 showlevel2，NULL=9999不封顶）' AFTER showlevel;

ALTER TABLE modedatasharerule
  ADD COLUMN showlevel int NULL COMMENT '密级下限（E9 showlevel，0~100）' AFTER condition_enabled,
  ADD COLUMN showlevel2 int NULL COMMENT '密级上限（E9 showlevel2，NULL=9999不封顶）' AFTER showlevel;

ALTER TABLE modedatashare_single
  ADD COLUMN showlevel int NULL COMMENT '密级下限（E9 showlevel，0~100）' AFTER matrix_value_field,
  ADD COLUMN showlevel2 int NULL COMMENT '密级上限（E9 showlevel2，NULL=9999不封顶）' AFTER showlevel;

-- 校验输出
SHOW COLUMNS FROM blade_user LIKE 'security_level';
SHOW COLUMNS FROM modedatasharerule LIKE 'showlevel%';
