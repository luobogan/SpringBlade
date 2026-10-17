-- -----------------------------------------------------------------------------
-- 迁移脚本：公文字典 + 意见模板（对齐泛微 E9 公文规范基础数据）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_020
-- 目标库    : blade_workflow（odoc 业务表所在库）
-- 影响表    : wf_odoc_dict（新建）、wf_odoc_opinion（新建）
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子 INSERT IGNORE 依赖 uk_odoc_dict_type_code）
-- 回滚      : DROP TABLE wf_odoc_opinion; DROP TABLE wf_odoc_dict;
--
-- 背景
--   对齐泛微 E9 公文规范基础数据：秘密等级(DocSecretLevel)/紧急程度(DocInstancyLevel)/
--   种类(odoc_odoctype)/主题分类(odoc_topictype)/机构代字(odocOrgAbbr) 合并为统一字典
--   wf_odoc_dict + dict_type 区分；意见模板(常用批示语)落 wf_odoc_opinion。
--   doc_type 的 code 复用 GBT9704 文种码，与 wf_odoc_mould.doc_type_code 对齐。
--   运行时另有 OdocBaseDataSeeder 兜底灌同值（幂等），二者以 (dict_type, code) 唯一保证不重复。

CREATE TABLE IF NOT EXISTS `wf_odoc_dict` (
  `id`          BIGINT       NOT NULL COMMENT '主键(雪花)',
  `dict_type`   VARCHAR(32)  NOT NULL COMMENT '字典类型 secret_level/urgency/doc_type/topic_type/org_abbr',
  `code`        VARCHAR(64)  NOT NULL COMMENT '编码(类型内唯一)',
  `name`        VARCHAR(128) NOT NULL COMMENT '显示名',
  `alias`       VARCHAR(128) DEFAULT NULL COMMENT '别名/英文名',
  `show_order`  INT          DEFAULT 0 COMMENT '排序(升序)',
  `enabled`     TINYINT      DEFAULT 1 COMMENT '是否启用 1是 0否',
  `is_default`  TINYINT      DEFAULT 0 COMMENT '是否默认项 1是 0否',
  `ext_color`   VARCHAR(32)  DEFAULT NULL COMMENT '扩展色(紧急程度/密级着色)',
  `ext_note`    VARCHAR(255) DEFAULT NULL COMMENT '备注(doc_type 存 GBT profileKey)',
  `tenant_id`   VARCHAR(12)  DEFAULT NULL,
  `create_user` BIGINT       DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_user` BIGINT       DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  `is_deleted`  TINYINT      DEFAULT 0,
  `status`      TINYINT      DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_odoc_dict_type_code` (`dict_type`, `code`),
  KEY `idx_odoc_dict_type` (`dict_type`, `show_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公文字典';

CREATE TABLE IF NOT EXISTS `wf_odoc_opinion` (
  `id`           BIGINT        NOT NULL COMMENT '主键(雪花)',
  `def_id`       BIGINT        DEFAULT NULL COMMENT '流程定义ID(空=全局公共模板)',
  `phrase_short` VARCHAR(128)  DEFAULT NULL COMMENT '标题/常用语名',
  `phrase`       VARCHAR(1000) NOT NULL COMMENT '意见内容',
  `is_default`   TINYINT       DEFAULT 0 COMMENT '是否默认 1是 0否',
  `show_order`   INT           DEFAULT 0 COMMENT '排序(升序)',
  `enabled`      TINYINT       DEFAULT 1 COMMENT '是否启用 1是 0否',
  `tenant_id`    VARCHAR(12)   DEFAULT NULL,
  `create_user`  BIGINT        DEFAULT NULL,
  `create_time`  DATETIME      DEFAULT NULL,
  `update_user`  BIGINT        DEFAULT NULL,
  `update_time`  DATETIME      DEFAULT NULL,
  `is_deleted`   TINYINT       DEFAULT 0,
  `status`       TINYINT       DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_odoc_opinion_def` (`def_id`, `show_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公文意见模板';

-- 秘密等级（code=中文显示名，与存量表单自由录入值一致）
INSERT IGNORE INTO `wf_odoc_dict` (`id`,`dict_type`,`code`,`name`,`alias`,`show_order`,`enabled`,`is_default`,`create_time`,`update_time`) VALUES
(2108000000010101,'secret_level','公开','公开','PUBLIC',1,1,0,NOW(),NOW()),
(2108000000010102,'secret_level','秘密','秘密','SECRET',2,1,0,NOW(),NOW()),
(2108000000010103,'secret_level','机密','机密','CONFIDENTIAL',3,1,0,NOW(),NOW()),
(2108000000010104,'secret_level','绝密','绝密','TOP_SECRET',4,1,0,NOW(),NOW());

-- 紧急程度
INSERT IGNORE INTO `wf_odoc_dict` (`id`,`dict_type`,`code`,`name`,`alias`,`show_order`,`enabled`,`is_default`,`create_time`,`update_time`) VALUES
(2108000000010201,'urgency','普通','普通','NORMAL',1,1,0,NOW(),NOW()),
(2108000000010202,'urgency','加急','加急','EXPRESS',2,1,0,NOW(),NOW()),
(2108000000010203,'urgency','特急','特急','URGENT',3,1,0,NOW(),NOW());

-- 种类：15 文种（code=GBT 码，ext_note=版式 profileKey）
INSERT IGNORE INTO `wf_odoc_dict` (`id`,`dict_type`,`code`,`name`,`alias`,`show_order`,`enabled`,`is_default`,`ext_note`,`create_time`,`update_time`) VALUES
(2108000000010301,'doc_type','JUEYI','决议','JUEYI',1,1,0,'standard',NOW(),NOW()),
(2108000000010302,'doc_type','JUEDING','决定','JUEDING',2,1,0,'standard',NOW(),NOW()),
(2108000000010303,'doc_type','MINGLING','命令','MINGLING',3,1,0,'command',NOW(),NOW()),
(2108000000010304,'doc_type','GONGBAO','公报','GONGBAO',4,1,0,'standard',NOW(),NOW()),
(2108000000010305,'doc_type','GONGGAO','公告','GONGGAO',5,1,0,'standard',NOW(),NOW()),
(2108000000010306,'doc_type','TONGGAO','通告','TONGGAO',6,1,0,'standard',NOW(),NOW()),
(2108000000010307,'doc_type','YIJIAN','意见','YIJIAN',7,1,0,'standard',NOW(),NOW()),
(2108000000010308,'doc_type','TONGZHI','通知','TONGZHI',8,1,0,'standard',NOW(),NOW()),
(2108000000010309,'doc_type','TONGBAO','通报','TONGBAO',9,1,0,'standard',NOW(),NOW()),
(2108000000010310,'doc_type','BAOGAO','报告','BAOGAO',10,1,0,'standard',NOW(),NOW()),
(2108000000010311,'doc_type','QINGSHI','请示','QINGSHI',11,1,0,'standard',NOW(),NOW()),
(2108000000010312,'doc_type','PIFU','批复','PIFU',12,1,0,'standard',NOW(),NOW()),
(2108000000010313,'doc_type','YIAN','议案','YIAN',13,1,0,'standard',NOW(),NOW()),
(2108000000010314,'doc_type','HAN','函','HAN',14,1,0,'standard',NOW(),NOW()),
(2108000000010315,'doc_type','JIYAO','纪要','JIYAO',15,1,0,'minutes',NOW(),NOW());

-- 全局常用批示语（意见模板）
INSERT IGNORE INTO `wf_odoc_opinion` (`id`,`def_id`,`phrase_short`,`phrase`,`is_default`,`show_order`,`enabled`,`create_time`,`update_time`) VALUES
(2108000000020101,NULL,'同意','同意。',0,1,1,NOW(),NOW()),
(2108000000020102,NULL,'拟同意','拟同意，请审阅。',0,2,1,NOW(),NOW()),
(2108000000020103,NULL,'已阅','已阅。',0,3,1,NOW(),NOW()),
(2108000000020104,NULL,'请办理','请相关部门办理并回复。',0,4,1,NOW(),NOW()),
(2108000000020105,NULL,'同意执行','同意，请据此执行。',0,5,1,NOW(),NOW());
