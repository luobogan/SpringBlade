-- -----------------------------------------------------------------------------
-- 迁移脚本：新增「公文督办设置」与「公文督办记录」表
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_007
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_supervise_setting（新建）、wf_odoc_supervise（新建）
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `wf_odoc_supervise`;
--   DROP TABLE IF EXISTS `wf_odoc_supervise_setting`;
--
-- 背景
--   公文督办（对应 E9 odoc_supervisesetting / odoc_superviseinfo）。设置按流程定义（谁督办/超期天数/
--   提醒方式）；记录按实例（督办人督促发起人尽快办理）。统计/监控复用 wf_instance + wf_odoc_request，不新建表。

CREATE TABLE IF NOT EXISTS `wf_odoc_supervise_setting` (
    `id`             BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `def_id`         BIGINT       NOT NULL COMMENT '流程定义ID',
    `offical_type`   TINYINT      DEFAULT NULL COMMENT '公文类型 1发文 2收文 3签报',
    `supervisor_ids` VARCHAR(500) DEFAULT NULL COMMENT '督办人ID,逗号分隔',
    `delay_days`     INT          DEFAULT NULL COMMENT '超期督办天数',
    `remind_way`     VARCHAR(64)  DEFAULT NULL COMMENT '提醒方式(站内/邮件/短信)',
    `enabled`        TINYINT      DEFAULT 1 COMMENT '是否启用 0否 1是',
    `tenant_id`      VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`    BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`    BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`    BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`         INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_odoc_sup_set_def` (`def_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公文督办设置(流程级)';

CREATE TABLE IF NOT EXISTS `wf_odoc_supervise` (
    `id`               BIGINT       NOT NULL COMMENT 'ID(雪花)',
    `instance_id`      BIGINT       NOT NULL COMMENT '流程实例ID',
    `def_id`           BIGINT       DEFAULT NULL COMMENT '流程定义ID',
    `offical_type`     TINYINT      DEFAULT NULL COMMENT '公文类型 1发文 2收文 3签报',
    `supervisor_id`    BIGINT       DEFAULT NULL COMMENT '督办人ID',
    `starter_id`       BIGINT       DEFAULT NULL COMMENT '被督办对象(发起人)ID',
    `supervise_status` TINYINT     DEFAULT 0 COMMENT '督办状态 0督办中 1已办结 2已取消',
    `deadline`         DATETIME     DEFAULT NULL COMMENT '督办期限',
    `remark`           VARCHAR(500) DEFAULT NULL COMMENT '督办说明',
    `last_remind_time` DATETIME     DEFAULT NULL COMMENT '最近提醒时间',
    `is_test`          TINYINT      DEFAULT 0 COMMENT '是否测试流程产生的数据(可一键清理)',
    `tenant_id`        VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`      BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_dept`      BIGINT       DEFAULT NULL COMMENT '创建部门',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`      BIGINT       DEFAULT NULL COMMENT '修改人',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`           INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
    `is_deleted`       INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_odoc_sup_supervisor` (`supervisor_id`, `status`, `is_deleted`),
    KEY `idx_odoc_sup_starter` (`starter_id`, `status`, `is_deleted`),
    KEY `idx_odoc_sup_inst` (`instance_id`, `is_deleted`),
    KEY `idx_odoc_sup_test` (`is_test`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公文督办记录(按实例)';
