-- =====================================================================
-- 版本      : V2026.11.15_002
-- 目标库    : blade
-- 影响表    : blade_scope_data（列类型扩容：scope_field）
-- 变更类型  : DDL
-- 是否幂等  : 是（判断列类型是否已是 text 家族，是则跳过）
-- 是否丢数据: 否（varchar -> text 为同语义放宽，仅放宽长度上限）
-- 回滚脚本  :
--   ALTER TABLE `blade_scope_data`
--      MODIFY COLUMN `scope_field` varchar(255) DEFAULT NULL
--      COMMENT '数据权限字段';
--
-- 背景：
--   blade_scope_data.scope_field 存的是「数据权限可见字段」的**列名逗号串**
--   （与前端「可见字段」勾选结果同源，同一列同时驱动 SQL 列裁剪与前端动态列）。
--   该串长度随**可见字段数量**线性增长：单列名平均约 10 字符 + 1 个逗号，
--   勾选 58 个字段即约 640 字符，已远超 varchar(255) 的容量。
--   现象：保存数据权限时报
--     Data truncation: Data too long for column 'scope_field' at row 1
--   （报错点 DataScopeMapper.updateById 写整行，scope_field 是唯一超长列）。
--
--   为什么不改前端做长度限制：可见字段数量由业务表单字段数决定、无固定上限，
--   前端截断会静默丢列（勾了却不生效），比报错更难排查。
--   故放宽为 TEXT：长度不设上限，且该列不参与 WHERE/ORDER BY，无需考虑
--   TEXT 索引前缀限制（仅 SELECT 拼接使用）。
--
-- 执行要求：项目未启用 Flyway，请在 blade 库手工执行本文件（仅一次）。
-- =====================================================================

USE `blade`;

SET @dbname = 'blade';
SET @tablename = 'blade_scope_data';
SET @columnname = 'scope_field';

SET @preparedStatement = (
  SELECT IF(
    (SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
       WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname)
      IN ('text', 'mediumtext', 'longtext'),
    'SELECT 1',
    'ALTER TABLE `blade_scope_data`
       MODIFY COLUMN `scope_field` TEXT
       COMMENT ''数据权限字段（可见列名逗号串；长度随可见字段数量增长，故用 TEXT 不设上限）'''
  )
);

PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
