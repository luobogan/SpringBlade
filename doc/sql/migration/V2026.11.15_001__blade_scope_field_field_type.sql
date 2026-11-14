-- =====================================================================
-- 版本      : V2026.11.15_001
-- 目标库    : blade
-- 影响表    : blade_scope_field（加列）
-- 变更类型  : DDL
-- 是否幂等  : 是（存储过程判断列是否存在，不存在才 ALTER）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `blade_scope_field` DROP COLUMN `field_type`;
--
-- 背景：
--   blade_scope_field 原仅存 field_html_type（展示/控件类型编号），
--   无法区分浏览按钮子类（人力资源 / 部门 / 日期 …）。列表预览与字段元
--   数据表格按 getFieldTypeLabel(fieldHtmlType, fieldType) 展示组件字段类型，
--   缺失 fieldType 时只能降级显示「浏览按钮」大类。本迁移补该列，
--   使已保存配置回显时也能精确展示子类。
--
-- 执行要求：项目未启用 Flyway，请在 blade 库手工执行本文件（仅一次）。
-- =====================================================================

USE `blade`;

SET @dbname = 'blade';
SET @tablename = 'blade_scope_field';
SET @columnname = 'field_type';

SET @preparedStatement = (
  SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
       WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
    'SELECT 1',
    'ALTER TABLE `blade_scope_field`
       ADD COLUMN `field_type` varchar(32) DEFAULT NULL
       COMMENT ''字段子类型（人力资源/部门/日期等浏览按钮子类；与 field_html_type 组合确定表单控件类型）'''
  )
);

PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
