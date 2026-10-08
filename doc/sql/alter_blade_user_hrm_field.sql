-- =====================================================================
-- P2：字段配置驱动 + 自定义字段存储（对齐 ecology hrm_fieldgroup / HrmCustomFieldByInfoType / cus_fielddata）
-- 见 doc/md/用户创建功能改造计划-对齐ecology架构.md §5-P2 / §12
-- 说明：
--   1) 三张表：
--        blade_hrm_field_group  字段分组（对齐 hrm_fieldgroup，group_type 区分基本/个人/工作信息）
--        blade_hrm_field        字段元数据（对齐 HrmCustomFieldByInfoType：启用/必填/控件类型）
--        blade_user_ext_data    自定义字段值（对齐 cus_fielddata）
--   2) 决策（P2-4）：不仿制 E9 主表预留列（datefield1-5/numberfield1-5...），
--      一律走扩展表，避免列爆炸；
--   3) 内置字段（account/password/realName/deptId 等）不进配置表，等价于"系统级不可停用"，
--      配置表只承载自由字段（对齐 Ecology 固定字段 + 自由字段分层，见 §6 风险表）。
-- =====================================================================

CREATE TABLE IF NOT EXISTS blade_hrm_field_group (
  id           bigint      NOT NULL COMMENT '主键',
  tenant_id    varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  group_code   varchar(50) NOT NULL COMMENT '分组编码',
  group_name   varchar(100) NOT NULL COMMENT '分组名称',
  group_type   tinyint     NOT NULL DEFAULT 1 COMMENT '分组类型:1基本信息 2个人信息 3工作信息(对齐 ecology hrm_fieldgroup.grouptype)',
  sort         int         NOT NULL DEFAULT 0 COMMENT '排序',
  create_user  bigint      NULL COMMENT '创建人',
  create_dept  bigint      NULL COMMENT '创建部门',
  create_time  datetime    NULL COMMENT '创建时间',
  update_user  bigint      NULL COMMENT '修改人',
  update_time  datetime    NULL COMMENT '修改时间',
  status       int         NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  is_deleted   int         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_hrm_field_group (tenant_id, group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员字段分组(对齐 ecology hrm_fieldgroup)';

CREATE TABLE IF NOT EXISTS blade_hrm_field (
  id           bigint      NOT NULL COMMENT '主键',
  tenant_id    varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  group_id     bigint      NOT NULL COMMENT '所属分组id(blade_hrm_field_group.id)',
  prop_name    varchar(50) NOT NULL COMMENT '字段属性名(表单字段名,租户内唯一)',
  label        varchar(100) NOT NULL COMMENT '字段显示名',
  ele_type     varchar(20) NOT NULL DEFAULT 'input' COMMENT '控件类型:input/textarea/number/date/select(对齐 ecology eleclazzname)',
  required     tinyint     NOT NULL DEFAULT 0 COMMENT '是否必填:0否 1是(对齐 ecology isMand)',
  sort         int         NOT NULL DEFAULT 0 COMMENT '排序',
  ext_json     varchar(1000) NULL COMMENT '控件扩展配置JSON(如 select 的 options: [{"label":"","value":""}])',
  remark       varchar(255) NULL COMMENT '备注',
  create_user  bigint      NULL COMMENT '创建人',
  create_dept  bigint      NULL COMMENT '创建部门',
  create_time  datetime    NULL COMMENT '创建时间',
  update_user  bigint      NULL COMMENT '修改人',
  update_time  datetime    NULL COMMENT '修改时间',
  status       int         NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用(对齐 ecology isUse)',
  is_deleted   int         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_hrm_field_prop (tenant_id, prop_name),
  KEY idx_hrm_field_group (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员自定义字段配置(对齐 ecology HrmCustomFieldByInfoType)';

CREATE TABLE IF NOT EXISTS blade_user_ext_data (
  id           bigint      NOT NULL COMMENT '主键',
  tenant_id    varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  user_id      bigint      NOT NULL COMMENT '用户id(blade_user.id)',
  field_id     bigint      NOT NULL COMMENT '字段id(blade_hrm_field.id)',
  field_value  varchar(1000) NULL COMMENT '字段值',
  create_user  bigint      NULL COMMENT '创建人',
  create_dept  bigint      NULL COMMENT '创建部门',
  create_time  datetime    NULL COMMENT '创建时间',
  update_user  bigint      NULL COMMENT '修改人',
  update_time  datetime    NULL COMMENT '修改时间',
  status       int         NOT NULL DEFAULT 1 COMMENT '状态',
  is_deleted   int         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_ext_data (tenant_id, user_id, field_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义字段值(对齐 ecology cus_fielddata)';

-- =====================================================================
-- 示例数据（000000 租户，可选）：工作信息分组 + 一个"办公地点"自由字段
-- 展示"改配置即改表单"的效果；如不需要可跳过本段
-- =====================================================================
INSERT INTO blade_hrm_field_group (id, tenant_id, group_code, group_name, group_type, sort, status, is_deleted)
SELECT 1948000000000000011, '000000', 'WORK_INFO', '工作信息', 3, 30, 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM blade_hrm_field_group WHERE tenant_id = '000000' AND group_code = 'WORK_INFO');

INSERT INTO blade_hrm_field (id, tenant_id, group_id, prop_name, label, ele_type, required, sort, ext_json, remark, status, is_deleted)
SELECT 1948000000000000012, '000000', 1948000000000000011, 'workLocation', '办公地点', 'select', 0, 10,
       '{"options":[{"label":"总部A座","value":"总部A座"},{"label":"总部B座","value":"总部B座"},{"label":"驻外","value":"驻外"}]}',
       '示例自由字段：办公地点(对齐 ecology locationid 的简化版)', 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM blade_hrm_field WHERE tenant_id = '000000' AND prop_name = 'workLocation');
