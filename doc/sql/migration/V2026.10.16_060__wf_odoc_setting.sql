-- -----------------------------------------------------------------------------
-- 迁移脚本：公文通用设置（单例，模块级开关 + 新建流程默认值）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_060
-- 目标库    : blade_workflow（odoc 业务表所在库）
-- 影响表    : wf_odoc_setting（新建）
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；默认行 INSERT IGNORE 固定主键）
-- 回滚      : DROP TABLE wf_odoc_setting;
--
-- 背景
--   对齐 E9 应用设置-通用设置里 odoc 专属开关（避免与 blade 菜单/权限重复）：
--   is_open_ofd 是否开放 OFD 版式（前端据此显隐 OFD 选项）；default_* 为新建流程
--   建文设置的默认值（版式格式/强制套红/必须签章）。后端 /odoc/setting 读写，前端消费。

CREATE TABLE IF NOT EXISTS `wf_odoc_setting` (
  `id`                    BIGINT       NOT NULL COMMENT '主键(雪花)',
  `is_open_ofd`           TINYINT      DEFAULT 0 COMMENT '是否开放 OFD 版式 1是 0否',
  `default_layout_format` TINYINT      DEFAULT 0 COMMENT '新建流程默认版式格式 0不启用/12PDF/13OFD',
  `default_must_taohong`  TINYINT      DEFAULT 1 COMMENT '新建流程默认是否强制套红 1是 0否',
  `default_seal_required` TINYINT      DEFAULT 0 COMMENT '新建流程默认是否必须电子签章 1是 0否',
  `remark`                VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `tenant_id`             VARCHAR(12)  DEFAULT NULL,
  `create_user`           BIGINT       DEFAULT NULL,
  `create_time`           DATETIME     DEFAULT NULL,
  `update_user`           BIGINT       DEFAULT NULL,
  `update_time`           DATETIME     DEFAULT NULL,
  `is_deleted`            TINYINT      DEFAULT 0,
  `status`                TINYINT      DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公文通用设置(单例)';

-- 默认行（与 WfOdocSettingServiceImpl.defaults 一致；运行时 OdocBaseDataSeeder 亦幂等兜底）
INSERT IGNORE INTO `wf_odoc_setting` (`id`,`is_open_ofd`,`default_layout_format`,`default_must_taohong`,`default_seal_required`,`create_time`,`update_time`) VALUES
(2108000000040101,0,0,1,0,NOW(),NOW());
