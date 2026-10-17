-- -----------------------------------------------------------------------------
-- 迁移脚本：密级收发限制规则（对齐 E9 SENDREUNITRESTRICTIONS / CommReceiveGroup）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_080
-- 目标库    : blade_workflow（odoc 业务表所在库）
-- 影响表    : wf_odoc_secret_rule（新建）
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 回滚      : DROP TABLE wf_odoc_secret_rule;
--
-- 背景
--   约束某收文单位/公共组可收发的公文密级范围 [sec_level_from, sec_level_to]（序数：
--   0公开/1秘密/2机密/3绝密，见 OdocSecretLevel）。提交时若公文密级超出范围则由
--   WfNodeMustDoActionExecutor 拒绝；无规则=不限制。规则按需配置，故不灌种子。

CREATE TABLE IF NOT EXISTS `wf_odoc_secret_rule` (
  `id`              BIGINT       NOT NULL COMMENT '主键(雪花)',
  `target_type`     TINYINT      NOT NULL COMMENT '目标类型 1收文单位 2公共组',
  `target_id`       BIGINT       NOT NULL COMMENT '目标ID(单位ID/公共组ID)',
  `sec_level_from`  TINYINT      DEFAULT 0 COMMENT '允许密级起(序数 0..3)',
  `sec_level_to`    TINYINT      DEFAULT 3 COMMENT '允许密级至(序数 0..3)',
  `enabled`         TINYINT      DEFAULT 1 COMMENT '是否启用 1是 0否',
  `remark`          VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `tenant_id`       VARCHAR(12)  DEFAULT NULL,
  `create_user`     BIGINT       DEFAULT NULL,
  `create_time`     DATETIME     DEFAULT NULL,
  `update_user`     BIGINT       DEFAULT NULL,
  `update_time`     DATETIME     DEFAULT NULL,
  `is_deleted`      TINYINT      DEFAULT 0,
  `status`          TINYINT      DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_odoc_secret_rule_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密级收发限制规则';
