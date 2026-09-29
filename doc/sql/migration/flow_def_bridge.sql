-- ============================================================
-- T-2 数据模型定稿 · DEF_KEY_ 桥接表（决策 D6）
-- 作用：业务定义 defId ↔ Flowable 引擎 processDefinition（KEY_ + 版本）的桥接。
--       原 wf_process_definition 将在 P6 退役，此表独立存活，承载 1:N 反查与版本统计。
-- 命名：flow_ 前缀（非 wf_，符合"不保留 wf_ 表"目标）。
-- 运行：必须 USE blade（jeelowcode 也有 ACT_*，跨库危险，见分析文档 §11 警示）。
-- ============================================================
USE blade;

CREATE TABLE IF NOT EXISTS `flow_def_bridge` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `def_key`           VARCHAR(64)    NOT NULL COMMENT '业务定义 key（DEF_KEY_，D6 桥接键）',
  `def_id`            BIGINT         NOT NULL COMMENT '业务侧流程定义ID（原 wf_process_definition.id）',
  `engine_def_key`    VARCHAR(255)   NOT NULL COMMENT 'Flowable process id（KEY_）',
  `engine_def_id`     VARCHAR(255)   NOT NULL COMMENT 'Flowable 流程定义ID（含版本，如 key:ver:rand）',
  `engine_version`    INT            NOT NULL DEFAULT 1 COMMENT '引擎流程定义版本',
  `tenant_id`         VARCHAR(32)    NOT NULL DEFAULT '000000' COMMENT '租户ID（多域隔离，§13）',
  `status`            TINYINT        NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_def_key_tenant` (`def_key`, `tenant_id`),
  KEY `idx_engine_def_id` (`engine_def_id`),
  KEY `idx_def_id_tenant` (`def_id`, `tenant_id`),
  KEY `idx_tenant_engine_key` (`tenant_id`, `engine_def_key`, `engine_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务定义与 Flowable 引擎定义桥接（DEF_KEY_，D6）';

-- 注：ACT_* 加列（ACT_HI_PROCINST.DEF_ID_/DATA_ID_/FORM_ID_/TITLE_/IS_TEST_、ACT_RU_TASK.TIMEOUT_HANDLED_）
--     与租户复合索引、ACT_HI_COMMENT 租户列（决策 D8）归属 T-6（P3），见分析文档 §11。
-- 注：投影索引表（决策 D3）默认不建——分析文档 §8.4 结论"无需投影表，靠 ACT_* 原生列 + 可重建投影"，
--     故方案C 投影设施可直接退役；若后续发现某些筛选无法覆盖，再按需补建非 wf_ 前缀只读投影表。
