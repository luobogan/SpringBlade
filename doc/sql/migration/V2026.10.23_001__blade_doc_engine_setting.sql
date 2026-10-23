-- V2026.10.23_001__blade_doc_engine_setting.sql
-- 内容引擎 · 编辑器设置表（默认编辑方案 Univer / WPS）。
-- 幂等；目标库: blade（blade-doc 业务库）。
CREATE TABLE IF NOT EXISTS doc_engine_setting (
  id            BIGINT       NOT NULL COMMENT '主键',
  tenant_id     VARCHAR(50)  DEFAULT '000000' COMMENT '租户ID',
  setting_key   VARCHAR(64)  NOT NULL COMMENT '设置键',
  setting_value VARCHAR(512) DEFAULT NULL COMMENT '设置值',
  remark        VARCHAR(255) DEFAULT NULL COMMENT '说明',
  create_user   BIGINT       DEFAULT 0 COMMENT '创建人',
  create_dept   BIGINT       DEFAULT 0 COMMENT '创建部门',
  create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_user   BIGINT       DEFAULT 0 COMMENT '修改人',
  update_time   DATETIME     DEFAULT NULL COMMENT '修改时间',
  status        INT          DEFAULT 1 COMMENT '状态',
  is_deleted    INT          DEFAULT 0 COMMENT '是否已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_setting_key (setting_key)
) COMMENT='内容引擎-编辑器设置';
