-- -----------------------------------------------------------------------------
-- 迁移脚本：收发文单位 + 公共组（对齐泛微 E9 DOCRECEIVEUNIT / receiveunit_group）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_030
-- 目标库    : blade_workflow（odoc 业务表所在库）
-- 影响表    : wf_odoc_receive_unit（新建）、wf_odoc_receive_group（新建）
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子 INSERT IGNORE 依赖固定主键）
-- 回滚      : DROP TABLE wf_odoc_receive_group; DROP TABLE wf_odoc_receive_unit;
--
-- 背景
--   收发文单位从自由文本升级为单位档案（层级/内部外部），并补齐 E9 完全缺失的公共组。
--   密级收发限制（E9 SENDREUNITRESTRICTIONS / CommReceiveGroup）暂不建模，留后续。

CREATE TABLE IF NOT EXISTS `wf_odoc_receive_unit` (
  `id`                     BIGINT       NOT NULL COMMENT '主键(雪花)',
  `unit_name`              VARCHAR(255) NOT NULL COMMENT '单位名称',
  `unit_code`              VARCHAR(64)  DEFAULT NULL COMMENT '单位代字/代码',
  `superior_id`            BIGINT       DEFAULT NULL COMMENT '上级单位ID(空=根)',
  `unit_level`             INT          DEFAULT 1 COMMENT '层级深度(根=1)',
  `company_type`           TINYINT      DEFAULT 0 COMMENT '单位类型 0内部 1外部',
  `is_main`                TINYINT      DEFAULT 0 COMMENT '是否主送单位 1是 0否',
  `can_start_child_request` TINYINT     DEFAULT 0 COMMENT '是否可发起子收文 1是 0否',
  `receiver_ids`           VARCHAR(500) DEFAULT NULL COMMENT '收文员ID(逗号分隔)',
  `show_order`             INT          DEFAULT 0 COMMENT '排序(升序)',
  `enabled`                TINYINT      DEFAULT 1 COMMENT '是否启用 1是 0否',
  `ext_note`               VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `tenant_id`              VARCHAR(12)  DEFAULT NULL,
  `create_user`            BIGINT       DEFAULT NULL,
  `create_time`            DATETIME     DEFAULT NULL,
  `update_user`            BIGINT       DEFAULT NULL,
  `update_time`            DATETIME     DEFAULT NULL,
  `is_deleted`             TINYINT      DEFAULT 0,
  `status`                 TINYINT      DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_odoc_ru_superior` (`superior_id`, `show_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收发文单位';

CREATE TABLE IF NOT EXISTS `wf_odoc_receive_group` (
  `id`          BIGINT        NOT NULL COMMENT '主键(雪花)',
  `group_name`  VARCHAR(255)  NOT NULL COMMENT '公共组名称',
  `unit_ids`    VARCHAR(2000) DEFAULT NULL COMMENT '成员单位ID(逗号分隔)',
  `show_order`  INT           DEFAULT 0 COMMENT '排序(升序)',
  `enabled`     TINYINT       DEFAULT 1 COMMENT '是否启用 1是 0否',
  `ext_note`    VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `tenant_id`   VARCHAR(12)   DEFAULT NULL,
  `create_user` BIGINT        DEFAULT NULL,
  `create_time` DATETIME      DEFAULT NULL,
  `update_user` BIGINT        DEFAULT NULL,
  `update_time` DATETIME      DEFAULT NULL,
  `is_deleted`  TINYINT       DEFAULT 0,
  `status`      TINYINT       DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_odoc_rg_name` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收发文公共组';

-- 根单位 + 示例外部单位
INSERT IGNORE INTO `wf_odoc_receive_unit` (`id`,`unit_name`,`superior_id`,`unit_level`,`company_type`,`is_main`,`show_order`,`enabled`,`create_time`,`update_time`) VALUES
(2108000000030101,'本单位',NULL,1,0,1,1,1,NOW(),NOW()),
(2108000000030102,'示例外部单位',NULL,1,1,0,2,1,NOW(),NOW());

-- 默认公共组（成员=本单位）
INSERT IGNORE INTO `wf_odoc_receive_group` (`id`,`group_name`,`unit_ids`,`show_order`,`enabled`,`ext_note`,`create_time`,`update_time`) VALUES
(2108000000030201,'默认公共组','2108000000030101',1,1,'示例：把常用收文单位加入本组，批量选择',NOW(),NOW());
