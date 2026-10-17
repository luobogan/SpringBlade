-- -----------------------------------------------------------------------------
-- 迁移脚本：公文列表列设置（对齐 E9 OdocCustomListSet）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_100
-- 目标库    : blade_workflow（odoc 业务表所在库）
-- 影响表    : wf_odoc_list_set（新建）
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 回滚      : DROP TABLE wf_odoc_list_set;
--
-- 背景
--   按列表编码存默认隐藏列（CSV），前端据此过滤列显隐（对应 E9 列表设置）。
--   当前为「按列表」的管理员级默认配置（非按用户）。后端 /odoc/list-set 读写。

CREATE TABLE IF NOT EXISTS `wf_odoc_list_set` (
  `id`          BIGINT        NOT NULL COMMENT '主键(雪花)',
  `list_code`   VARCHAR(64)   NOT NULL COMMENT '列表编码(odoc_todo/odoc_supervise/odoc_report/odoc_exchange)',
  `hidden_keys` VARCHAR(2000) DEFAULT NULL COMMENT '隐藏列key(逗号分隔)',
  `enabled`     TINYINT       DEFAULT 1 COMMENT '是否启用 1是 0否',
  `tenant_id`   VARCHAR(12)   DEFAULT NULL,
  `create_user` BIGINT        DEFAULT NULL,
  `create_time` DATETIME      DEFAULT NULL,
  `update_user` BIGINT        DEFAULT NULL,
  `update_time` DATETIME      DEFAULT NULL,
  `is_deleted`  TINYINT       DEFAULT 0,
  `status`      TINYINT       DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_odoc_list_set_code` (`list_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公文列表列设置';
