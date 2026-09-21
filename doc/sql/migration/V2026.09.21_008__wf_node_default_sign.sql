-- ============================================================================
-- 版本    : V2026.09.21_008
-- 目标库  : blade_workflow
-- 影响表  : wf_node_default_sign（按操作类型默认签字意见）
-- 变更类型: 新增表（对齐泛微：不同操作自动带出默认意见）
-- 是否幂等: 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本: DROP TABLE IF EXISTS `wf_node_default_sign`;
-- 决策依据: 泛微节点信息「按操作类型默认签字意见」（唯一键 def_id+node_key+menu_type）。
-- ============================================================================

USE `blade_workflow`;

CREATE TABLE IF NOT EXISTS `wf_node_default_sign` (
    `id`             BIGINT       NOT NULL                COMMENT '主键（雪花 ID）',
    `tenant_id`      VARCHAR(12)  NOT NULL DEFAULT '000000' COMMENT '租户',
    `def_id`         BIGINT       NOT NULL                COMMENT '流程定义 ID',
    `node_key`       VARCHAR(64)  NOT NULL                COMMENT '节点 Key',
    `menu_type`      VARCHAR(64)  NOT NULL                COMMENT '操作类型（submit/reject/forward/... 或自定义按钮 key）',
    `default_opinion` VARCHAR(512) NULL                   COMMENT '该操作类型的默认签字意见',
    `create_time`    DATETIME     NULL                    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_default_sign_def_node_menu` (`def_id`, `node_key`, `menu_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '按操作类型默认签字意见';
