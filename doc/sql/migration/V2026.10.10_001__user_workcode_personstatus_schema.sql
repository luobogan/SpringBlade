-- =============================================================================
-- 迁移脚本 001：补齐「工号 / 人员状态 / Hrm」改造所需的表结构与字段（幂等版）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.10_001
-- 目标库    : blade
-- 影响表    : blade_user（新增 work_code / person_status / certificate_num 三列 + 工号唯一键）
--             blade_code_rule / blade_user_ext_data / blade_hrm_field / blade_user_complete_status（新建）
-- 变更类型  : DDL
-- 是否幂等  : 是（列/唯一键按需添加；表用 IF NOT EXISTS；可重复执行）
-- 背景
--   User 实体（blade-user-api）新增 workCode / personStatus / certificateNum 字段，
--   blade-system 新增 WorkCodeRule / UserExtData / HrmField / UserCompleteStatus 四个实体，
--   但对应表结构与字段从未建过，导致查询 blade_user 报 Unknown column 'work_code'。
-- =============================================================================

USE `blade`;

-- 0) 临时存储过程：列不存在才加 ------------------------------------------------
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_col_if_missing(
  IN p_db VARCHAR(64), IN p_tbl VARCHAR(64), IN p_col VARCHAR(64), IN p_def VARCHAR(512))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = p_db AND TABLE_NAME = p_tbl AND COLUMN_NAME = p_col
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_db, '`.`', p_tbl, '` ADD COLUMN `', p_col, '` ', p_def);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

-- 1) blade_user 新增三列 -------------------------------------------------------
CALL add_col_if_missing('blade', 'blade_user', 'work_code',
  "varchar(50) DEFAULT NULL COMMENT '工号(租户内唯一，可由编码规则自动生成)'");
CALL add_col_if_missing('blade', 'blade_user', 'person_status',
  "int DEFAULT NULL COMMENT '人员状态:0试用 1正式 2临时 3延期 4解聘 5退休'");
CALL add_col_if_missing('blade', 'blade_user', 'certificate_num',
  "varchar(50) DEFAULT NULL COMMENT '证件号(主账号范围业务唯一，应用层校验)'");

DROP PROCEDURE add_col_if_missing;

-- 2) 工号租户内唯一键（对齐 ecology hrmResourceCheck；逻辑删除时置空工号以复用）----
SET @uk = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='blade_user' AND INDEX_NAME='uk_blade_user_tenant_workcode');
SET @sql = IF(@uk = 0,
  'ALTER TABLE blade_user ADD UNIQUE KEY uk_blade_user_tenant_workcode (tenant_id, work_code)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 组织编码规则（WorkCodeRule 实体，@TableName=blade_code_rule）-----------------
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

-- 4) 用户自定义字段值（UserExtData 实体）---------------------------------------
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

-- 5) 人员自定义字段配置（HrmField 实体）---------------------------------------
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

-- 6) 用户信息完善度（UserCompleteStatus 实体）----------------------------------
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
