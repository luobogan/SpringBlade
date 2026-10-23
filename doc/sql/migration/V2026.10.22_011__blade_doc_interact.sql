-- V2026.10.22_011__blade_doc_interact.sql
-- 交互三件套：回复 / 点赞 / 评分。幂等、库无关。
CREATE TABLE IF NOT EXISTS `doc_reply` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `doc_id` BIGINT DEFAULT NULL COMMENT '文档ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父回复ID(0=顶层)',
  `user_id` BIGINT DEFAULT NULL COMMENT '回复人',
  `content` TEXT COMMENT '回复内容',
  `tenant_id` VARCHAR(20) DEFAULT '' COMMENT '租户ID',
  `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_dept` BIGINT DEFAULT NULL COMMENT '创建部门',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME DEFAULT NULL COMMENT '修改时间',
  `status` INT DEFAULT 1 COMMENT '状态',
  `is_deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档回复';

CREATE TABLE IF NOT EXISTS `doc_praise` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `doc_id` BIGINT DEFAULT NULL COMMENT '文档ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '点赞人',
  `tenant_id` VARCHAR(20) DEFAULT '' COMMENT '租户ID',
  `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_dept` BIGINT DEFAULT NULL COMMENT '创建部门',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME DEFAULT NULL COMMENT '修改时间',
  `status` INT DEFAULT 1 COMMENT '状态',
  `is_deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档点赞';

CREATE TABLE IF NOT EXISTS `doc_score` (
  `id` BIGINT NOT NULL COMMENT '主键',
  `doc_id` BIGINT DEFAULT NULL COMMENT '文档ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '评分人',
  `score` INT DEFAULT NULL COMMENT '分值(1-5)',
  `tenant_id` VARCHAR(20) DEFAULT '' COMMENT '租户ID',
  `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_dept` BIGINT DEFAULT NULL COMMENT '创建部门',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
  `update_time` DATETIME DEFAULT NULL COMMENT '修改时间',
  `status` INT DEFAULT 1 COMMENT '状态',
  `is_deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档评分';
