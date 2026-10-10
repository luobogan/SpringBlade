-- =============================================================================
-- 迁移脚本 006：补齐消息模块（blade-message）全部表结构
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.10_006
-- 目标库    : blade
-- 影响表    : blade_message_session / blade_message_session_member / blade_message
--             blade_message_attachment / blade_message_read_log / blade_message_notice_config
-- 变更类型  : DDL
-- 是否幂等  : 是（表均用 IF NOT EXISTS）
-- 背景
--   blade-message 模块实体的 @TableName 对应 6 张表，但库里从未建过，
--   导致查询 blade_message_session_member 报 Table doesn't exist。
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_message_session` (
  `id`          bigint       NOT NULL COMMENT '主键',
  `tenant_id`   varchar(50)  DEFAULT NULL COMMENT '租户ID',
  `name`        varchar(255) DEFAULT NULL COMMENT '群名称(两人会话可为空)',
  `type`        int          DEFAULT NULL COMMENT '会话类型 1=两人 2=群',
  `last_message` varchar(2000) DEFAULT NULL COMMENT '最近一条消息摘要',
  `last_time`   datetime     DEFAULT NULL COMMENT '最近消息时间',
  `create_user` bigint       DEFAULT NULL,
  `create_dept` bigint       DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_user` bigint       DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  `status`      int          DEFAULT NULL,
  `is_deleted`  int          DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_session_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息会话';

CREATE TABLE IF NOT EXISTS `blade_message_session_member` (
  `id`          bigint       NOT NULL COMMENT '主键',
  `tenant_id`   varchar(50)  DEFAULT NULL COMMENT '租户ID',
  `session_id`  bigint       DEFAULT NULL COMMENT '会话ID',
  `user_id`     bigint       DEFAULT NULL COMMENT '参与人(blade_user.id)',
  `unread_count` int         DEFAULT NULL COMMENT '未读数量',
  `pinned`      int          DEFAULT NULL COMMENT '置顶 0否 1是',
  `mute`        int          DEFAULT NULL COMMENT '免打扰 0否 1是',
  `create_user` bigint       DEFAULT NULL,
  `create_dept` bigint       DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_user` bigint       DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  `status`      int          DEFAULT NULL,
  `is_deleted`  int          DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_session_member_session` (`session_id`),
  KEY `idx_session_member_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话成员';

CREATE TABLE IF NOT EXISTS `blade_message` (
  `id`             bigint       NOT NULL COMMENT '主键',
  `tenant_id`      varchar(50)  DEFAULT NULL COMMENT '租户ID',
  `session_id`     bigint       DEFAULT NULL COMMENT '会话ID',
  `sender_id`      bigint       DEFAULT NULL COMMENT '发送人(blade_user.id)',
  `content_type`   int          DEFAULT NULL COMMENT '内容类型 1文本 2富文本 3附件 4流程引用',
  `category`       int          DEFAULT NULL COMMENT '消息分类 1=聊天 2=流程通知',
  `biz_state`      int          DEFAULT NULL COMMENT '通知业务状态 NULL=待处理 1=已处理 2=已办结',
  `content`        text         DEFAULT NULL COMMENT '内容',
  `quote_message_id` bigint     DEFAULT NULL COMMENT '引用消息ID',
  `biz_ref_type`   varchar(50)  DEFAULT NULL COMMENT '业务引用类型(WF_INSTANCE/WF_TASK)',
  `biz_ref_id`     varchar(100) DEFAULT NULL COMMENT '业务引用ID(字符串防精度丢失)',
  `status`         int          DEFAULT NULL COMMENT '状态 1=SENT',
  `create_user`    bigint       DEFAULT NULL,
  `create_dept`    bigint       DEFAULT NULL,
  `create_time`    datetime     DEFAULT NULL,
  `update_user`    bigint       DEFAULT NULL,
  `update_time`    datetime     DEFAULT NULL,
  `is_deleted`     int          DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_message_session` (`session_id`),
  KEY `idx_message_bizref` (`biz_ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息主体';

CREATE TABLE IF NOT EXISTS `blade_message_attachment` (
  `id`          bigint       NOT NULL COMMENT '主键',
  `tenant_id`   varchar(50)  DEFAULT NULL COMMENT '租户ID',
  `message_id`  bigint       DEFAULT NULL COMMENT '消息ID',
  `file_id`     varchar(100) DEFAULT NULL COMMENT 'blade-resource 文件ID',
  `file_name`   varchar(255) DEFAULT NULL COMMENT '文件名',
  `file_url`    varchar(1000) DEFAULT NULL COMMENT '文件访问地址',
  `file_size`   bigint       DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type`   varchar(100) DEFAULT NULL COMMENT '文件类型',
  `create_user` bigint       DEFAULT NULL,
  `create_dept` bigint       DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_user` bigint       DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  `status`      int          DEFAULT NULL,
  `is_deleted`  int          DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_attachment_message` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息附件(引用 blade-resource)';

CREATE TABLE IF NOT EXISTS `blade_message_read_log` (
  `id`          bigint       NOT NULL COMMENT '主键',
  `tenant_id`   varchar(50)  DEFAULT NULL COMMENT '租户ID',
  `message_id`  bigint       DEFAULT NULL COMMENT '消息ID',
  `user_id`     bigint       DEFAULT NULL COMMENT '阅读人(blade_user.id)',
  `read_time`   datetime     DEFAULT NULL COMMENT '阅读时间',
  `create_user` bigint       DEFAULT NULL,
  `create_dept` bigint       DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_user` bigint       DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  `status`      int          DEFAULT NULL,
  `is_deleted`  int          DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_read_log_message` (`message_id`),
  KEY `idx_read_log_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息已读回执';

CREATE TABLE IF NOT EXISTS `blade_message_notice_config` (
  `id`          bigint       NOT NULL COMMENT '主键',
  `tenant_id`   varchar(50)  DEFAULT NULL COMMENT '租户ID',
  `user_id`     bigint       DEFAULT NULL COMMENT '用户ID(blade_user.id)',
  `flow_key`    varchar(100) DEFAULT NULL COMMENT '流程定义key(proc_key);* = 全部流程',
  `enabled`     int          DEFAULT NULL COMMENT '是否接收 1=接收 0=屏蔽',
  `create_user` bigint       DEFAULT NULL,
  `create_dept` bigint       DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_user` bigint       DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  `status`      int          DEFAULT NULL,
  `is_deleted`  int          DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blade_msg_notice_cfg` (`tenant_id`, `user_id`, `flow_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程通知用户级接收配置';

SELECT 'MESSAGE_SCHEMA_DONE' AS result;
