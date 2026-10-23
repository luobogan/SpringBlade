-- V2026.10.22_008__blade_doc_category_config.sql
-- 目录配置子页：编号规则 / 文档属性可见性 / 审批工作流明细。幂等、库无关。

CREATE TABLE IF NOT EXISTS `doc_sec_category_coder` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `category_id` BIGINT DEFAULT NULL COMMENT '目录ID',
  `member_type` INT DEFAULT NULL COMMENT '成员类型 1固定文本 2日期 3流水号 4自定义字段',
  `member_value` VARCHAR(255) DEFAULT NULL COMMENT '成员值',
  `seq_length` INT DEFAULT NULL COMMENT '流水号长度',
  `order_num` INT DEFAULT NULL COMMENT '排序号',
  `tenant_id` VARCHAR(20) DEFAULT '' COMMENT '租户ID',
  `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_dept` BIGINT DEFAULT NULL COMMENT '创建部门',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME DEFAULT NULL COMMENT '修改时间',
  `status` INT DEFAULT 1 COMMENT '状态',
  `is_deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目录编号规则成员';

CREATE TABLE IF NOT EXISTS `doc_sec_category_doc_property` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `category_id` BIGINT DEFAULT NULL COMMENT '目录ID',
  `field_name` VARCHAR(100) DEFAULT NULL COMMENT '属性字段名',
  `is_view` INT DEFAULT 1 COMMENT '是否可见 0否 1是',
  `is_required` INT DEFAULT 0 COMMENT '是否必填 0否 1是',
  `view_index` INT DEFAULT NULL COMMENT '显示顺序(列序)',
  `tenant_id` VARCHAR(20) DEFAULT '' COMMENT '租户ID',
  `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_dept` BIGINT DEFAULT NULL COMMENT '创建部门',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME DEFAULT NULL COMMENT '修改时间',
  `status` INT DEFAULT 1 COMMENT '状态',
  `is_deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目录文档属性配置';

CREATE TABLE IF NOT EXISTS `doc_sec_category_approve_wf_detail` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `category_id` BIGINT DEFAULT NULL COMMENT '目录ID',
  `approve_type` INT DEFAULT NULL COMMENT '审批类型 1生效审批 2失效审批 3批准',
  `workflow_id` BIGINT DEFAULT NULL COMMENT '工作流ID',
  `workflow_field_id` BIGINT DEFAULT NULL COMMENT '流程字段ID',
  `doc_property_field_id` BIGINT DEFAULT NULL COMMENT '文档属性字段ID',
  `tenant_id` VARCHAR(20) DEFAULT '' COMMENT '租户ID',
  `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_dept` BIGINT DEFAULT NULL COMMENT '创建部门',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME DEFAULT NULL COMMENT '修改时间',
  `status` INT DEFAULT 1 COMMENT '状态',
  `is_deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目录审批工作流明细';
