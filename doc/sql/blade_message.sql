-- 企业统一消息中心 建表脚本（多租户，前缀 blade_message_）
-- 说明：id 采用雪花主键（MyBatis-Plus ASSIGN_ID），库内建表不设置自增；
--       tenant_id / create_time / create_user / create_dept / update_time / update_user / status / is_deleted 由 TenantEntity 统一提供。

DROP TABLE IF EXISTS `blade_message_session`;
CREATE TABLE IF NOT EXISTS `blade_message_session`
(
    `id`           BIGINT       NOT NULL COMMENT '主键',
    `tenant_id`    VARCHAR(50)  NOT NULL COMMENT '租户ID',
    `name`         VARCHAR(100) NULL     COMMENT '群名称（两人会话可为空）',
    `type`         INT          NOT NULL DEFAULT 1 COMMENT '会话类型 1=两人 2=群',
    `last_message` VARCHAR(500) NULL     COMMENT '最近一条消息摘要',
    `last_time`    DATETIME     NULL     COMMENT '最近消息时间',
    `create_time`  DATETIME     NULL     COMMENT '创建时间',
    `create_user`  BIGINT       NULL     COMMENT '创建人',
    `create_dept`  BIGINT       NULL     COMMENT '创建部门',
    `update_time`  DATETIME     NULL     COMMENT '更新时间',
    `update_user`  BIGINT       NULL     COMMENT '更新人',
    `status`       INT          NULL     COMMENT '状态',
    `is_deleted`   INT          NULL     COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_blade_message_session_tenant` (`tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息会话';

DROP TABLE IF EXISTS `blade_message_session_member`;
CREATE TABLE IF NOT EXISTS `blade_message_session_member`
(
    `id`          BIGINT NOT NULL COMMENT '主键',
    `tenant_id`   VARCHAR(50) NULL COMMENT '租户ID',
    `session_id`  BIGINT NOT NULL COMMENT '会话ID',
    `user_id`     BIGINT NOT NULL COMMENT '参与人（blade_user.id）',
    `unread_count` INT   NOT NULL DEFAULT 0 COMMENT '未读数量',
    `pinned`      INT   NOT NULL DEFAULT 0 COMMENT '置顶 0否 1是',
    `mute`        INT   NOT NULL DEFAULT 0 COMMENT '免打扰 0否 1是',
    `create_time` DATETIME NULL COMMENT '创建时间',
    `create_user` BIGINT   NULL COMMENT '创建人',
    `create_dept` BIGINT   NULL COMMENT '创建部门',
    `update_time` DATETIME NULL COMMENT '更新时间',
    `update_user` BIGINT   NULL COMMENT '更新人',
    `status`      INT      NULL COMMENT '状态',
    `is_deleted`  INT      NULL COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blade_message_member_session_user` (`session_id`, `user_id`),
    KEY `idx_blade_message_member_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会话成员';

DROP TABLE IF EXISTS `blade_message`;
CREATE TABLE IF NOT EXISTS `blade_message`
(
    `id`              BIGINT       NOT NULL COMMENT '主键',
    `tenant_id`       VARCHAR(50)  NOT NULL COMMENT '租户ID',
    `session_id`      BIGINT       NOT NULL COMMENT '会话ID',
    `sender_id`       BIGINT       NOT NULL COMMENT '发送人（blade_user.id）',
    `content_type`    INT          NOT NULL DEFAULT 1 COMMENT '内容类型 1文本 2富文本 3附件 4流程引用',
    `content`         VARCHAR(2000) NULL COMMENT '内容',
    `quote_message_id` BIGINT      NULL COMMENT '引用消息ID',
    `biz_ref_type`    VARCHAR(30)  NULL COMMENT '业务引用类型（WF_INSTANCE/WF_TASK）',
    `biz_ref_id`      VARCHAR(64)  NULL COMMENT '业务引用ID（字符串防精度丢失）',
    `status`          INT          NOT NULL DEFAULT 1 COMMENT '状态 1=SENT',
    `create_time`     DATETIME     NULL COMMENT '创建时间',
    `create_user`     BIGINT       NULL COMMENT '创建人',
    `create_dept`     BIGINT       NULL COMMENT '创建部门',
    `update_time`     DATETIME     NULL COMMENT '更新时间',
    `update_user`     BIGINT       NULL COMMENT '更新人',
    `is_deleted`      INT          NULL COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_blade_message_session` (`session_id`),
    KEY `idx_blade_message_tenant` (`tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息主体';

DROP TABLE IF EXISTS `blade_message_attachment`;
CREATE TABLE IF NOT EXISTS `blade_message_attachment`
(
    `id`          BIGINT NOT NULL COMMENT '主键',
    `tenant_id`   VARCHAR(50) NULL COMMENT '租户ID',
    `message_id`  BIGINT NOT NULL COMMENT '消息ID',
    `file_id`     VARCHAR(64) NULL COMMENT 'blade-resource 文件ID',
    `file_name`   VARCHAR(200) NULL COMMENT '文件名',
    `file_url`    VARCHAR(500) NULL COMMENT '文件访问地址',
    `file_size`   BIGINT NULL COMMENT '文件大小（字节）',
    `file_type`   VARCHAR(50) NULL COMMENT '文件类型',
    `create_time` DATETIME NULL COMMENT '创建时间',
    `create_user` BIGINT   NULL COMMENT '创建人',
    `create_dept` BIGINT   NULL COMMENT '创建部门',
    `update_time` DATETIME NULL COMMENT '更新时间',
    `update_user` BIGINT   NULL COMMENT '更新人',
    `status`      INT      NULL COMMENT '状态',
    `is_deleted`  INT      NULL COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_blade_message_attachment_message` (`message_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息附件';

DROP TABLE IF EXISTS `blade_message_read_log`;
CREATE TABLE IF NOT EXISTS `blade_message_read_log`
(
    `id`          BIGINT NOT NULL COMMENT '主键',
    `tenant_id`   VARCHAR(50) NULL COMMENT '租户ID',
    `message_id`  BIGINT NOT NULL COMMENT '消息ID',
    `user_id`     BIGINT NOT NULL COMMENT '阅读人（blade_user.id）',
    `read_time`   DATETIME NULL COMMENT '阅读时间',
    `create_time` DATETIME NULL COMMENT '创建时间',
    `create_user` BIGINT   NULL COMMENT '创建人',
    `create_dept` BIGINT   NULL COMMENT '创建部门',
    `update_time` DATETIME NULL COMMENT '更新时间',
    `update_user` BIGINT   NULL COMMENT '更新人',
    `status`      INT      NULL COMMENT '状态',
    `is_deleted`  INT      NULL COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blade_message_read_log_msg_user` (`message_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息已读回执';
