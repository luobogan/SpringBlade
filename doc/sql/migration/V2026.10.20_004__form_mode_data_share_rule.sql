-- =====================================================================
-- V2026.10.20_004  表单建模：行级数据范围共享规则表
-- ---------------------------------------------------------------------
-- 背景（P2-2 Step4）：表单建模此前无任何「行级数据范围」机制——
--   pageFormData / getFormDataById 对所有用户返回该模块的<b>全部数据</b>，
--   存在越权读（看到他人/其他部门数据）。
--
-- 本表对标 ecology 的 modedatasharerule，但做<b>精简够用子集</b>：
--   把模块的「查看/维护」权限共享给 角色 / 部门 / 人员 / 岗位；
--   命中共享规则的用户可见该模块<b>全部数据</b>；
--   未命中的用户默认<b>仅可见自己创建的</b>（creator 自阅，E9 默认行为）。
--   矩阵 / 复杂条件表达式为后续增强（暂按「共享=看全部」处理）。
--
-- 解析引擎与读取路径接入见后端 ModeDataShareRuleServiceImpl / FormDataServiceImpl。
--
-- 执行要求：幂等（CREATE TABLE IF NOT EXISTS），可重复执行。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `modedatasharerule` (
  `id`           bigint       NOT NULL                COMMENT '主键（雪花）',
  `modeid`       bigint       NOT NULL                COMMENT '模块ID（modeinfo.id）',
  `type`         tinyint      NOT NULL DEFAULT 1      COMMENT '权限类型：1查看 2维护',
  `objtype`      tinyint      NOT NULL                COMMENT '授权对象类型：1人员 2部门 3角色 4岗位',
  `objid`        bigint       NOT NULL DEFAULT 0      COMMENT '授权对象ID（人员/部门/角色/岗位）',
  `include_sub`  tinyint      DEFAULT 1               COMMENT '是否含子级（部门/角色生效）：1是 0否',
  `creator_self` tinyint      DEFAULT 0               COMMENT '附加：仅本人创建的数据可见（与共享叠加）',
  `tenant_id`    varchar(32)  DEFAULT '000000'        COMMENT '租户ID',
  `create_time`  datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_time`  datetime     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_modeid` (`modeid`),
  KEY `idx_mode_obj` (`modeid`, `objtype`, `objid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='模块数据范围共享规则（行级数据权限）';


-- =====================================================================
-- 回滚脚本（删除该表）
-- ---------------------------------------------------------------------
-- DROP TABLE IF EXISTS `modedatasharerule`;
-- =====================================================================
