-- -----------------------------------------------------------------------------
-- 迁移脚本：新增「流程测试日志」表 wf_test_log
-- -----------------------------------------------------------------------------
-- 版本      : V2026.09.17_001
-- 目标库    : blade_workflow（wf_test_log 所在库）
-- 影响表    : wf_test_log（新建）
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `wf_test_log`;
--
-- 背景
--   设计期「流程测试」功能（WfTestController / IWfTestService）首次运行会向
--   wf_test_log 落库测试日志，但建表 DDL 此前只在 workflow-schema.sql 中定义，
--   尚未执行到运行库，导致运行时报
--   "Table 'blade_workflow.wf_test_log' doesn't exist"。
--   本脚本把该建表语句抽成独立迁移，便于手工执行与复现。
--
--   说明：本项目无 Flyway/Liquibase，schema 变更长期靠手工执行，
--   请在目标库 blade_workflow 上直接执行本文件（或用你常用的 DB 工具执行下面语句）。

CREATE TABLE IF NOT EXISTS `wf_test_log` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `def_id`         BIGINT UNSIGNED NOT NULL                COMMENT '流程定义ID',
    `def_version`    INT          NULL                      COMMENT '测试时的流程版本',
    `proc_key`       VARCHAR(100) NULL                      COMMENT '引擎流程Key（冗余，便于检索）',
    `def_name`       VARCHAR(200) NULL                      COMMENT '流程名称（冗余）',
    `test_user_id`   BIGINT       NOT NULL                  COMMENT '测试发起人用户ID',
    `test_user_name` VARCHAR(100) NULL                      COMMENT '测试发起人姓名（冗余）',
    `test_time`      DATETIME     NOT NULL                  COMMENT '测试时间',
    `cost_ms`        BIGINT       NOT NULL DEFAULT 0        COMMENT '耗时（毫秒）',
    `test_status`    INT          NOT NULL DEFAULT 0        COMMENT '测试结论 0未通过 1通过 2异常中断',
    `node_total`     INT          NOT NULL DEFAULT 0        COMMENT '参与校验节点数',
    `node_passed`    INT          NOT NULL DEFAULT 0        COMMENT '走通节点数',
    `reached_end`    INT          NOT NULL DEFAULT 0        COMMENT '是否走到归档节点 1是 0否',
    `summary`        VARCHAR(1000) NULL                     COMMENT '结论摘要',
    `log_content`    LONGTEXT     NULL                      COMMENT '测试日志正文（逐行文本）',
    `result_json`    JSON         NULL                      COMMENT '结构化结果：节点经过次数/路径/操作者',
    `tenant_id`      VARCHAR(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
    `create_user`    BIGINT       NULL COMMENT '创建人',
    `create_dept`    BIGINT       NULL COMMENT '创建部门',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user`    BIGINT       NULL COMMENT '修改人',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`         INT          NOT NULL DEFAULT 1        COMMENT '状态:1正常 0禁用',
    `is_deleted`     INT          NOT NULL DEFAULT 0        COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_def_time` (`def_id`, `test_time`),
    KEY `idx_user_time` (`test_user_id`, `test_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程测试日志';
