-- -----------------------------------------------------------------------------
-- 迁移脚本：公文流程设置补充「公文属性→表单字段」映射 + 公文信息表列表索引
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.15_006
-- 目标库    : blade_workflow
-- 影响表    : wf_odoc_process_set（加列）、wf_odoc_request（加索引）
-- 变更类型  : DDL（ALTER）
-- 是否丢数据: 否
-- 回滚脚本  :
--   ALTER TABLE `wf_odoc_process_set`
--     DROP COLUMN `issued_unit_field_key`, DROP COLUMN `secret_level_field_key`,
--     DROP COLUMN `urgency_field_key`,     DROP COLUMN `topic_field_key`,
--     DROP COLUMN `receive_num_field_key`, DROP COLUMN `receive_unit_field_key`,
--     DROP COLUMN `written_date_field_key`,DROP COLUMN `issued_date_field_key`;
--   ALTER TABLE `wf_odoc_request` DROP INDEX `idx_odoc_req_ot`;
--
-- 背景
--   公文待办/已办/查询（WfOdocTodo*，对应 E9 OdocReqListCmd）依赖 wf_odoc_request 镜像；
--   镜像由 WfOdocRequestSyncListener 在节点提交后置从流程表单业务表抽取。抽取所需的
--   「公文属性→表单字段」映射（文号复用 number_field_key、标题复用 doc_title_field_key）
--   其余列在此补充到 wf_odoc_process_set（流程级配置，经 ProcessSetModal 维护）。
--   同时在 wf_odoc_request 加 (offical_type, is_deleted) 索引，支撑 officalType 维度子查询。
--
-- 说明：迁移 runner 按版本号只执行一次；以下为普通 ALTER（非 CREATE IF NOT EXISTS）。

ALTER TABLE `wf_odoc_process_set`
    ADD COLUMN `issued_unit_field_key`  VARCHAR(128) DEFAULT NULL COMMENT '印发/来文单位字段',
    ADD COLUMN `secret_level_field_key` VARCHAR(128) DEFAULT NULL COMMENT '密级字段',
    ADD COLUMN `urgency_field_key`     VARCHAR(128) DEFAULT NULL COMMENT '紧急程度字段',
    ADD COLUMN `topic_field_key`       VARCHAR(128) DEFAULT NULL COMMENT '主题词字段',
    ADD COLUMN `receive_num_field_key` VARCHAR(128) DEFAULT NULL COMMENT '收文编号字段',
    ADD COLUMN `receive_unit_field_key`VARCHAR(128) DEFAULT NULL COMMENT '主送/收文单位字段',
    ADD COLUMN `written_date_field_key`VARCHAR(128) DEFAULT NULL COMMENT '成文日期字段',
    ADD COLUMN `issued_date_field_key` VARCHAR(128) DEFAULT NULL COMMENT '印发日期字段';

ALTER TABLE `wf_odoc_request`
    ADD INDEX `idx_odoc_req_ot` (`offical_type`, `is_deleted`);
