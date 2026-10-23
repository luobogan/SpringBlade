-- V2026.10.22_003__blade_doc_mould.sql
-- 内容引擎 · 显示模板（C-9）：doc_mould。对齐 E9 DocMould。
-- 幂等：CREATE TABLE IF NOT EXISTS；库无关。
CREATE TABLE IF NOT EXISTS `doc_mould` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `tenant_id` VARCHAR(50) NULL COMMENT '租户ID',
  `mould_name` VARCHAR(255) NULL COMMENT '模板名',
  `mould_type` INT NULL COMMENT '模板类型 0HTML 2WORD 3EXCEL 4WPS',
  `is_default` INT NULL DEFAULT 0 COMMENT '是否默认',
  `image_file_id` BIGINT NULL COMMENT '模板内容文件ID',
  `mould_path` VARCHAR(500) NULL COMMENT '模板路径',
  `create_user` BIGINT NULL, `create_dept` BIGINT NULL, `create_time` DATETIME NULL,
  `update_user` BIGINT NULL, `update_time` DATETIME NULL,
  `status` INT NULL DEFAULT 1, `is_deleted` INT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '显示模板';
