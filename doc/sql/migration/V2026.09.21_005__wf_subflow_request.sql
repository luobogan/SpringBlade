-- ============================================================================
-- 版本    : V2026.09.21_005
-- 目标库  : blade_workflow
-- 影响表  : wf_subflow_request（新建）
-- 变更类型: 新增「主/子流程请求关系表」，支撑子流程「全部归档才能提交 / 数据汇总 / 提醒 / 归档后自动流转」
-- 是否幂等: 是（CREATE TABLE IF NOT EXISTS + 列存在性守卫）
-- 是否丢数据: 否
-- 回滚脚本: DROP TABLE IF EXISTS `wf_subflow_request`;
-- 决策依据: 泛微 workflow_subwfrequest；SpringBlade 子流程已触发但未登记关系，无法做「全部归档阻塞/汇总/提醒/自动流转」
-- ============================================================================

USE `blade_workflow`;

CREATE TABLE IF NOT EXISTS `wf_subflow_request` (
    `id`                    BIGINT           NOT NULL                COMMENT '主键（雪花 ID）',
    `tenant_id`             VARCHAR(12)      NOT NULL DEFAULT '000000' COMMENT '租户',
    `main_inst_id`          BIGINT           NOT NULL                COMMENT '主流程实例 ID',
    `sub_inst_id`           BIGINT           NOT NULL                COMMENT '子流程实例 ID（唯一）',
    `sub_def_id`            BIGINT           NULL                    COMMENT '子流程定义 ID',
    `main_node_key`         VARCHAR(64)      NULL                    COMMENT '触发子流程的主流程节点 Key',
    `is_same`               TINYINT          NOT NULL DEFAULT 1      COMMENT '是否「相同子流程」范围（1=是 0=否）',
    `all_end_before_submit` TINYINT          NOT NULL DEFAULT 0      COMMENT '快照：全部归档才能提交（阻塞主流程归档）',
    `data_summary`          TINYINT          NOT NULL DEFAULT 0      COMMENT '快照：子流程归档后汇总数据到主流程',
    `auto_forward`          TINYINT          NOT NULL DEFAULT 0      COMMENT '快照：全部归档后自动流转主流程',
    `remind_enabled`        TINYINT          NOT NULL DEFAULT 0      COMMENT '快照：全部归档后提醒',
    `remind_types`          VARCHAR(32)      NULL                    COMMENT '提醒方式：含 sys=流程提醒 ml=短信 sm=邮件',
    `remind_before_operator`TINYINT          NOT NULL DEFAULT 0      COMMENT '提醒对象：节点操作者本人',
    `remind_persons`        VARCHAR(255)     NULL                    COMMENT '提醒对象：指定人员（人力资源 ID 逗号分隔）',
    `status`                TINYINT          NOT NULL DEFAULT 0      COMMENT '0=进行中（未归档） 1=已归档',
    `create_time`           DATETIME         NULL                    COMMENT '创建时间',
    `archive_time`          DATETIME         NULL                    COMMENT '子流程归档时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subflow_sub_inst` (`sub_inst_id`),
    KEY `idx_subflow_main_node_status` (`main_inst_id`, `main_node_key`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '主/子流程请求关系表（子流程高级设置）';

-- 尾部校验
SELECT 'wf_subflow_request' AS `table`, COUNT(1) AS `exists_flag`
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_subflow_request';
