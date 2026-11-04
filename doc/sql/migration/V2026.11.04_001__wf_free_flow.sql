-- =============================================================================
-- 迁移：自由流程（P0+P1+P2）实例级步骤 / 链路 与 预置节点池容量
--
-- 背景
--   见 doc/md/自由流程-SpringBlade落地分析与实现建议.md。
--   采用「静态节点池绑定 + 实例级步骤编排 + 影子节点装饰」方案：每个自由步骤绑定到
--   BPMN 内已部署的真实静态 activityId（Free_k），因此 wf_task.node_key 恒为设计期
--   已存在的 key，loadNode(defId,nodeKey) 天然命中，避免 §10.1 的连锁断点
--   （公文四钩子静默跳过 / 字段权限全放行 / 快照不留痕 / 进度图意见退回超时失效）。
--
-- 边界（遵循迁移规范：加列一律可空 + 默认值；建表用 IF NOT EXISTS 幂等）
--   · 不改既有表结构、不删列、不回填存量数据。
--   · wf_process_definition.free_node_pool_size 全部可空：NULL = 未配置，
--     代码侧按默认 5 处理；存量流程 is_free=0，完全不进入新逻辑（零行为变更）。
--   · 用 DATABASE() 而非硬编码库名，各环境通用。
--
-- 索引说明（对齐 mysql skill：等值列在前、范围覆盖排序）
--   · uk_inst_order(inst_id, flow_order)：整包读取 / 按序推进的主访问路径，
--     同时覆盖 "WHERE inst_id=?" 的左前缀，无需额外单列索引。
--   · uk_inst_node(inst_id, node_key)：Resolver 按 (实例,当前节点) 反查步骤。
--   · uk_link(inst_id, from_node_key, to_node_key)：退回 BFS 取入边防重。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 实例级自由步骤
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wf_free_node` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键（雪花ID由应用写入）',
  `inst_id` bigint unsigned NOT NULL COMMENT '流程实例ID',
  `def_id` bigint unsigned NOT NULL COMMENT '流程定义ID（发起时快照，不随版本变更）',
  `node_key` varchar(64) NOT NULL COMMENT '绑定的预置池静态 activityId（如 Free_3）；写入后永不改变',
  `start_node_key` varchar(64) DEFAULT NULL COMMENT '动态段挂载的定义态节点Key',
  `flow_order` int NOT NULL COMMENT '步骤顺序（从1开始）',
  `node_name` varchar(200) NOT NULL DEFAULT '' COMMENT '步骤显示名（运行时可自由命名）',
  `operator_ids` varchar(2000) NOT NULL DEFAULT '' COMMENT '办理人ID逗号串（对齐 ecology operators）',
  `sign_type` tinyint NOT NULL DEFAULT '0' COMMENT '会签关系：0非会签 1会签 2依次逐个（运行期仅支持 0/1/2）',
  `ext_json` text COMMENT '影子节点配置（菜单/字段权限/超时等装饰用 JSON）',
  `version` int NOT NULL DEFAULT '0' COMMENT '整包替换乐观锁版本号',
  `is_finalize` tinyint NOT NULL DEFAULT '0' COMMENT '是否定稿取号点（替代按静态 finalizeNodeKey 判定）',
  `must_taohong` tinyint NOT NULL DEFAULT '0' COMMENT '是否套红必做点',
  `parallel_group` varchar(64) DEFAULT NULL COMMENT '并行分支组标识；NULL=串行',
  `node_status` tinyint NOT NULL DEFAULT '0' COMMENT '0未流转 1已流转（已流转步骤属性只读）',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inst_order` (`inst_id`,`flow_order`),
  UNIQUE KEY `uk_inst_node` (`inst_id`,`node_key`),
  KEY `idx_def` (`def_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自由流程实例级步骤';

-- -----------------------------------------------------------------------------
-- 2. 实例级步骤链路（供退回 BFS / 分支合并 / 出口复制）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wf_free_link` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inst_id` bigint unsigned NOT NULL COMMENT '流程实例ID',
  `def_id` bigint unsigned NOT NULL COMMENT '流程定义ID',
  `from_node_key` varchar(64) NOT NULL COMMENT '源节点（池内静态 key）',
  `to_node_key` varchar(64) NOT NULL COMMENT '目标节点（池内静态 key）',
  `is_reject` tinyint NOT NULL DEFAULT '0' COMMENT '是否退回线',
  `is_must_pass` tinyint NOT NULL DEFAULT '0' COMMENT '分叉必经分支',
  `merge_type` tinyint DEFAULT NULL COMMENT '合并策略：1按比例 2按分支数 3指定通过分支',
  `merge_value` varchar(200) DEFAULT NULL COMMENT '合并阈值 / 指定分支',
  `condition_copy` text COMMENT '从定义态出口复制的条件（留痕，不参与求值）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `tenant_id` varchar(32) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `is_deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_link` (`inst_id`,`from_node_key`,`to_node_key`),
  KEY `idx_inst_to` (`inst_id`,`to_node_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自由流程实例级步骤链路';

-- -----------------------------------------------------------------------------
-- 3. 流程定义：预置节点池容量（独立幂等，可重复执行）
-- -----------------------------------------------------------------------------
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_process_definition'
      AND COLUMN_NAME = 'free_node_pool_size') = 0,
  "ALTER TABLE wf_process_definition ADD COLUMN free_node_pool_size int DEFAULT NULL COMMENT '自由流程预置通用审批节点数量；NULL=未配置（代码按默认5处理）；调整后须重新发布刷新 proc_def_id'",
  "SELECT 'skip: wf_process_definition.free_node_pool_size already exists'"
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 4. 校验（期望：两表 1、新列 1）
-- -----------------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_free_node') AS has_free_node,
  (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wf_free_link') AS has_free_link,
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_process_definition'
      AND COLUMN_NAME = 'free_node_pool_size') AS has_free_node_pool_size;

-- -----------------------------------------------------------------------------
-- 5. wf_task.ext_json：自由流程步骤动态显示名 / 步序（独立幂等）
--    影子节点装饰让 loadNode 命中静态配置，但待办列表需要展示用户自定义的
--    「步骤N / 自定义名称」，故在任务行上冗余一份轻量上下文。
-- -----------------------------------------------------------------------------
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_task'
      AND COLUMN_NAME = 'ext_json') = 0,
  "ALTER TABLE wf_task ADD COLUMN ext_json varchar(1000) DEFAULT NULL COMMENT '任务级扩展上下文JSON：自由流程步骤显示名/步序；NULL=普通任务'",
  "SELECT 'skip: wf_task.ext_json already exists'"
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wf_task'
      AND COLUMN_NAME = 'ext_json') AS has_task_ext_json;

-- =============================================================================
-- 回滚脚本（如需撤销，取消注释执行）
--   DROP TABLE IF EXISTS wf_free_link;
--   DROP TABLE IF EXISTS wf_free_node;
--   ALTER TABLE wf_process_definition DROP COLUMN free_node_pool_size;
--   ALTER TABLE wf_task DROP COLUMN ext_json;
-- 行为回滚：不加列亦可 —— 存量行 NULL = 未配置，且 is_free=0 的流程完全不进入新逻辑，
--           与本次迁移前完全一致。
-- =============================================================================
