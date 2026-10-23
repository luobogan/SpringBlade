-- V2026.10.22_002__blade_doc_custom_field.sql
-- 内容引擎 · 自定义字段体系（C-2）：doc_custom_field(定义) + doc_custom_field_value(值)。
-- 对齐 E9 cus_formdict / cus_fielddata；加密字段值落密文（is_encrypted=1，见 DocFieldCrypto）。
-- 幂等：CREATE TABLE IF NOT EXISTS；库无关（作用于当前 DATABASE()）。

CREATE TABLE IF NOT EXISTS `doc_custom_field` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `field_name` VARCHAR(128) NULL COMMENT '字段名(存储键)',
  `field_label` VARCHAR(255) NULL COMMENT '字段标签',
  `field_type` INT NULL COMMENT '字段类型 1单行 2多行 3浏览按钮 4复选 5选择',
  `is_encrypt` INT NULL DEFAULT 0 COMMENT '是否加密存储',
  `scope` VARCHAR(128) NULL COMMENT '加密/权限作用域(读写须一致)',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_custom_field_name` (`field_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义字段定义';

CREATE TABLE IF NOT EXISTS `doc_custom_field_value` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `doc_id` BIGINT NULL COMMENT '文档ID',
  `field_id` BIGINT NULL COMMENT '字段ID',
  `field_value` LONGTEXT NULL COMMENT '字段值(加密字段为密文)',
  `is_encrypted` INT NULL DEFAULT 0 COMMENT '是否密文 0明文 1密文',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_custom_field_value_doc_field` (`doc_id`, `field_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义字段值';
