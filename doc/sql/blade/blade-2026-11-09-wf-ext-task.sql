-- =============================================================================
-- 扩展任务表 wf_ext_task（去 wf_ 表 · P6 退役第一台阶 · 2026-11-09）
--
-- 用途：承载「引擎侧无原生形态」的 blade 扩展任务，使 wf_task 收敛为
--       「仅引擎任务本体台账」。列表/角标的扩展任务段由本表读取。
--
-- 设计要点（对齐 wf_task 实际 DDL）：
--   * tenant_id varchar(32) / is_deleted int / create_user|dept|time / update_user|time
--     与 TenantEntity 一致，保证租户插件与 @TableLogic 生效。
--   * engine_task_id 空串（非 NULL）表示「无引擎任务的合成待办」（退回发起人重提交）。
--   * status 为扩展任务自身状态（0 待处理/未读、1 已处理/已读、2 已拒绝），
--     列表 VO 通过 WfExtTask#toWfTaskStatus() 映射回原 WfTask status 语义。
--   * 复合索引 idx_assignee_type_status 服务「assignee 等值 + type/status IN」查询。
-- =============================================================================

CREATE TABLE IF NOT EXISTS `wf_ext_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键（雪花ID由应用写入）',
  `inst_id` bigint unsigned NOT NULL COMMENT '流程实例ID',
  `engine_task_id` varchar(64) NOT NULL DEFAULT '' COMMENT '引擎任务ID（Flowable）；空串=无引擎任务的合成待办（退回发起人重提交）',
  `biz_task_id` bigint unsigned DEFAULT NULL COMMENT '宿主业务任务ID（wf_task.id / ACT BIZ_TASK_ID_）；type=7 自由流程步骤名装饰行据此回查',
  `node_key` varchar(64) NOT NULL DEFAULT '' COMMENT '节点Key',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '扩展任务类型：1协办/转办 2加签协办 3抄送 4传阅 5已读 6退回重提交 7自由流程步骤',
  `assignee` bigint NOT NULL COMMENT '接收人/办理人',
  `original_user` bigint DEFAULT NULL COMMENT '原处理人（转办/协办回溯）',
  `sign_order` tinyint NOT NULL DEFAULT '0' COMMENT '会签关系',
  `title` varchar(400) DEFAULT NULL COMMENT '任务标题（冗余，避免列表回查实例）',
  `opinion` varchar(1000) DEFAULT NULL COMMENT '意见/备注',
  `status` int NOT NULL DEFAULT '0' COMMENT '扩展任务自身状态：0待处理/未读 1已处理/已读 2已拒绝',
  `ext_json` varchar(1000) DEFAULT NULL COMMENT '任务级扩展上下文JSON（自由流程步骤显示名/步序；NULL=普通扩展任务）',
  `is_test` tinyint NOT NULL DEFAULT '0' COMMENT '测试态标记 1=测试产生的任务',
  `receive_time` datetime DEFAULT NULL COMMENT '接收时间',
  `operate_time` datetime DEFAULT NULL COMMENT '处理时间',
  `view_time` datetime DEFAULT NULL COMMENT '首次查看时间',
  `due_time` datetime DEFAULT NULL COMMENT '截止时间',
  `timeout_handled` tinyint NOT NULL DEFAULT '0' COMMENT '超时动作是否已执行（防重复触发）1=已执行 0=未执行',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_assignee_type_status` (`assignee`,`type`,`status`),
  KEY `idx_inst_node` (`inst_id`,`node_key`),
  KEY `idx_engine_task` (`engine_task_id`),
  KEY `idx_biz_task_type` (`biz_task_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='扩展任务（协办/加签协办/抄送/传阅/已读/退回重提交/自由流程步骤）';
