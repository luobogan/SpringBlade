-- 仅建四张依赖表（列已由 001 补齐；IF NOT EXISTS 幂等）
USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_code_rule` (
  `id`          bigint       NOT NULL COMMENT '主键',
  `tenant_id`   varchar(50)  DEFAULT NULL COMMENT '租户ID',
  `rule_code`   varchar(50)  DEFAULT NULL COMMENT '规则编码:USER=工号 / DEPT=部门编号',
  `prefix`      varchar(50)  DEFAULT NULL COMMENT '前缀',
  `date_fmt`    varchar(50)  DEFAULT NULL COMMENT '日期段格式(如 yyyyMMdd),NULL 表示无日期段',
  `seq_len`     int          DEFAULT NULL COMMENT '序列位数(左补零)',
  `current_seq` bigint       DEFAULT NULL COMMENT '当前序列值',
  `remark`      varchar(255) DEFAULT NULL COMMENT '备注',
  `create_user` bigint       DEFAULT NULL,
  `create_dept` bigint       DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_user` bigint       DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  `status`      int          DEFAULT NULL,
  `is_deleted`  int          DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织编码规则';

CREATE TABLE IF NOT EXISTS `blade_user_ext_data` (
  `id`          bigint        NOT NULL COMMENT '主键',
  `tenant_id`   varchar(50)   DEFAULT NULL,
  `user_id`     bigint        DEFAULT NULL COMMENT '用户id',
  `field_id`    bigint        DEFAULT NULL COMMENT '字段id',
  `field_value` varchar(2000) DEFAULT NULL COMMENT '字段值',
  `create_user` bigint        DEFAULT NULL,
  `create_dept` bigint        DEFAULT NULL,
  `create_time` datetime      DEFAULT NULL,
  `update_user` bigint        DEFAULT NULL,
  `update_time` datetime      DEFAULT NULL,
  `status`      int           DEFAULT NULL,
  `is_deleted`  int           DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_ext_user` (`user_id`),
  KEY `idx_user_ext_field` (`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户自定义字段值(对齐 ecology cus_fielddata)';

CREATE TABLE IF NOT EXISTS `blade_hrm_field` (
  `id`          bigint        NOT NULL COMMENT '主键',
  `tenant_id`   varchar(50)   DEFAULT NULL,
  `group_id`    bigint        DEFAULT NULL COMMENT '所属分组id',
  `prop_name`   varchar(100)  DEFAULT NULL COMMENT '字段属性名(表单字段名)',
  `label`       varchar(100)  DEFAULT NULL COMMENT '字段显示名',
  `ele_type`    varchar(50)   DEFAULT NULL COMMENT '控件类型:input/textarea/number/date/select',
  `required`    int           DEFAULT NULL COMMENT '是否必填:0否 1是',
  `sort`        int           DEFAULT NULL COMMENT '排序',
  `ext_json`    varchar(2000) DEFAULT NULL COMMENT '控件扩展配置JSON',
  `remark`      varchar(255)  DEFAULT NULL COMMENT '备注',
  `create_user` bigint        DEFAULT NULL,
  `create_dept` bigint        DEFAULT NULL,
  `create_time` datetime      DEFAULT NULL,
  `update_user` bigint        DEFAULT NULL,
  `update_time` datetime      DEFAULT NULL,
  `status`      int           DEFAULT NULL,
  `is_deleted`  int           DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='人员自定义字段配置(对齐 ecology HrmCustomFieldByInfoType)';

CREATE TABLE IF NOT EXISTS `blade_user_complete_status` (
  `id`          bigint       NOT NULL COMMENT '主键',
  `tenant_id`   varchar(50)  DEFAULT NULL,
  `user_id`     bigint       DEFAULT NULL COMMENT '用户id',
  `item`        varchar(50)  DEFAULT NULL COMMENT '完善项:BASE基本信息 PERSON个人信息 WORK工作信息 SYSTEM系统信息',
  `done`        int          DEFAULT NULL COMMENT '是否完善:0否 1是',
  `create_user` bigint       DEFAULT NULL,
  `create_dept` bigint       DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_user` bigint       DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  `status`      int          DEFAULT NULL,
  `is_deleted`  int          DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_complete_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息完善度(对齐 ecology HrmInfoStatus)';
