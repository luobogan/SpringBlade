-- ============================================================================
-- 版本    : V2026.09.21_007
-- 目标库  : blade_workflow
-- 影响表  : wf_custom_operation（按钮）、wf_custom_operation_action（动作明细）、
--           wf_custom_operation_right（权限矩阵）、wf_node_default_sign（按操作类型默认签字意见）
-- 变更类型: 节点「自定义操作」（按钮 + 动作明细 URL/流程操作/接口 + 权限矩阵）与「按操作类型默认签字意见」
-- 是否幂等: 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本: 见各表 DROP TABLE IF EXISTS
-- 决策依据: 泛微节点信息「自定义操作」（自定义按钮 + 动作明细 + 权限 + $field$ 占位符）与「按操作类型默认签字意见」。
-- ============================================================================

USE `blade_workflow`;

CREATE TABLE IF NOT EXISTS `wf_custom_operation` (
    `id`          BIGINT       NOT NULL                COMMENT '主键（雪花 ID）',
    `tenant_id`   VARCHAR(12)  NOT NULL DEFAULT '000000' COMMENT '租户',
    `def_id`      BIGINT       NOT NULL                COMMENT '流程定义 ID',
    `node_key`    VARCHAR(64)  NOT NULL                COMMENT '节点 Key',
    `btn_name`    VARCHAR(64)  NOT NULL                COMMENT '按钮名称',
    `btn_order`   INT          NOT NULL DEFAULT 0      COMMENT '按钮顺序',
    `enabled`     TINYINT      NOT NULL DEFAULT 1      COMMENT '是否启用 1=是 0=否',
    `action_type` TINYINT      NOT NULL DEFAULT 1      COMMENT '动作类型 1=URL 2=流程操作 3=接口',
    `create_time` DATETIME     NULL                    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_custom_op_def_node` (`def_id`, `node_key`, `btn_order`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '节点自定义操作按钮';

CREATE TABLE IF NOT EXISTS `wf_custom_operation_action` (
    `id`             BIGINT       NOT NULL                COMMENT '主键（雪花 ID）',
    `tenant_id`      VARCHAR(12)  NOT NULL DEFAULT '000000' COMMENT '租户',
    `op_id`          BIGINT       NOT NULL                COMMENT '关联 wf_custom_operation.id',
    `url`            VARCHAR(512) NULL                    COMMENT '动作类型=URL 时的地址（支持 $field$ 占位符）',
    `http_method`    VARCHAR(8)   NULL DEFAULT 'POST'     COMMENT 'URL 请求方法',
    `flow_operation` VARCHAR(64)  NULL                    COMMENT '动作类型=流程操作时的动作标识（对齐 WfCustomAction 注册 key）',
    `interface_name` VARCHAR(128) NULL                    COMMENT '动作类型=接口时的接口名（扩展位）',
    `param_expr`     TEXT         NULL                    COMMENT '参数表达式（支持 $field$ 占位符，JSON 或表单串）',
    `opinion`        VARCHAR(255) NULL                    COMMENT '执行后写入的签字意见',
    `create_time`    DATETIME     NULL                    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_custom_op_action_op` (`op_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义操作动作明细';

CREATE TABLE IF NOT EXISTS `wf_custom_operation_right` (
    `id`          BIGINT       NOT NULL                COMMENT '主键（雪花 ID）',
    `tenant_id`   VARCHAR(12)  NOT NULL DEFAULT '000000' COMMENT '租户',
    `op_id`       BIGINT       NOT NULL                COMMENT '关联 wf_custom_operation.id',
    `right_type`  VARCHAR(16)  NOT NULL                COMMENT '权限类型 role=角色 dept=部门 person=人员',
    `right_value` VARCHAR(255) NOT NULL                COMMENT '权限值（角色/部门/人员 ID，多个逗号分隔）',
    `create_time` DATETIME     NULL                    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_custom_op_right_op` (`op_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义操作权限矩阵';

CREATE TABLE IF NOT EXISTS `wf_node_default_sign` (
    `id`            BIGINT       NOT NULL                COMMENT '主键（雪花 ID）',
    `tenant_id`     VARCHAR(12)  NOT NULL DEFAULT '000000' COMMENT '租户',
    `def_id`        BIGINT       NOT NULL                COMMENT '流程定义 ID',
    `node_key`      VARCHAR(64)  NOT NULL                COMMENT '节点 Key',
    `menu_type`     VARCHAR(32)  NOT NULL                COMMENT '操作类型（submit/reject/forward/... 或自定义按钮 key）',
    `default_opinion` VARCHAR(255) NULL                  COMMENT '该操作类型的默认签字意见',
    `create_time`   DATETIME     NULL                    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_default_sign_def_node_menu` (`def_id`, `node_key`, `menu_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '按操作类型默认签字意见';
