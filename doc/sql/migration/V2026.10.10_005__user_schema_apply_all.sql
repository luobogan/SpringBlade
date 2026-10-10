-- =============================================================================
-- 合并幂等迁移：补齐「工号 / 人员状态 / Hrm / 盐值」所需表结构与字段
-- 可重复执行（列按需添加、表 IF NOT EXISTS、唯一键与 salt 做存在性判断）
-- 目标库：blade
-- =============================================================================
USE `blade`;

-- 临时存储过程：列不存在才加（MySQL 8.0 支持 CREATE PROCEDURE IF NOT EXISTS）
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

CALL add_col_if_missing('blade', 'blade_user', 'work_code',
  "varchar(50) DEFAULT NULL COMMENT '工号(租户内唯一，可由编码规则自动生成)'");
CALL add_col_if_missing('blade', 'blade_user', 'person_status',
  "int DEFAULT NULL COMMENT '人员状态:0试用 1正式 2临时 3延期 4解聘 5退休'");
CALL add_col_if_missing('blade', 'blade_user', 'certificate_num',
  "varchar(50) DEFAULT NULL COMMENT '证件号(主账号范围业务唯一，应用层校验)'");
CALL add_col_if_missing('blade', 'blade_user', 'salt',
  "varchar(255) DEFAULT NULL COMMENT '密码盐值(预留，配合加盐算法惰性升级)'");

DROP PROCEDURE add_col_if_missing;

-- 工号租户内唯一键（存在性判断）
SET @uk = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA='blade' AND TABLE_NAME='blade_user' AND INDEX_NAME='uk_blade_user_tenant_workcode');
SET @sql = IF(@uk = 0,
  'ALTER TABLE blade_user ADD UNIQUE KEY uk_blade_user_tenant_workcode (tenant_id, work_code)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 四张依赖表
CREATE TABLE IF NOT EXISTS `blade_code_rule` (
  `id` bigint NOT NULL COMMENT '主键', `tenant_id` varchar(50) DEFAULT NULL COMMENT '租户ID',
  `rule_code` varchar(50) DEFAULT NULL COMMENT '规则编码:USER=工号 / DEPT=部门编号',
  `prefix` varchar(50) DEFAULT NULL COMMENT '前缀', `date_fmt` varchar(50) DEFAULT NULL COMMENT '日期段格式',
  `seq_len` int DEFAULT NULL COMMENT '序列位数', `current_seq` bigint DEFAULT NULL COMMENT '当前序列值',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_user` bigint DEFAULT NULL, `create_dept` bigint DEFAULT NULL, `create_time` datetime DEFAULT NULL,
  `update_user` bigint DEFAULT NULL, `update_time` datetime DEFAULT NULL, `status` int DEFAULT NULL, `is_deleted` int DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织编码规则';

CREATE TABLE IF NOT EXISTS `blade_user_ext_data` (
  `id` bigint NOT NULL COMMENT '主键', `tenant_id` varchar(50) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL COMMENT '用户id', `field_id` bigint DEFAULT NULL COMMENT '字段id',
  `field_value` varchar(2000) DEFAULT NULL COMMENT '字段值',
  `create_user` bigint DEFAULT NULL, `create_dept` bigint DEFAULT NULL, `create_time` datetime DEFAULT NULL,
  `update_user` bigint DEFAULT NULL, `update_time` datetime DEFAULT NULL, `status` int DEFAULT NULL, `is_deleted` int DEFAULT 0,
  PRIMARY KEY (`id`), KEY `idx_user_ext_user` (`user_id`), KEY `idx_user_ext_field` (`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户自定义字段值(对齐 ecology cus_fielddata)';

CREATE TABLE IF NOT EXISTS `blade_hrm_field` (
  `id` bigint NOT NULL COMMENT '主键', `tenant_id` varchar(50) DEFAULT NULL,
  `group_id` bigint DEFAULT NULL COMMENT '所属分组id', `prop_name` varchar(100) DEFAULT NULL COMMENT '字段属性名',
  `label` varchar(100) DEFAULT NULL COMMENT '字段显示名', `ele_type` varchar(50) DEFAULT NULL COMMENT '控件类型',
  `required` int DEFAULT NULL COMMENT '是否必填:0否 1是', `sort` int DEFAULT NULL COMMENT '排序',
  `ext_json` varchar(2000) DEFAULT NULL COMMENT '控件扩展配置JSON', `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_user` bigint DEFAULT NULL, `create_dept` bigint DEFAULT NULL, `create_time` datetime DEFAULT NULL,
  `update_user` bigint DEFAULT NULL, `update_time` datetime DEFAULT NULL, `status` int DEFAULT NULL, `is_deleted` int DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='人员自定义字段配置(对齐 ecology HrmCustomFieldByInfoType)';

CREATE TABLE IF NOT EXISTS `blade_user_complete_status` (
  `id` bigint NOT NULL COMMENT '主键', `tenant_id` varchar(50) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `item` varchar(50) DEFAULT NULL COMMENT '完善项:BASE/PERSON/WORK/SYSTEM', `done` int DEFAULT NULL COMMENT '是否完善:0否 1是',
  `create_user` bigint DEFAULT NULL, `create_dept` bigint DEFAULT NULL, `create_time` datetime DEFAULT NULL,
  `update_user` bigint DEFAULT NULL, `update_time` datetime DEFAULT NULL, `status` int DEFAULT NULL, `is_deleted` int DEFAULT 0,
  PRIMARY KEY (`id`), KEY `idx_complete_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息完善度(对齐 ecology HrmInfoStatus)';

SELECT 'APPLY_DONE' AS result;
