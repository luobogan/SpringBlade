-- =============================================================================
-- 迁移脚本：对象存储配置表 blade_oss + 页面按钮权限
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.23_002
-- 目标库    : blade
-- 影响表    : blade_oss（新建）、blade_menu（新增按钮权限）、blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（表/菜单均按不存在才创建）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `blade_oss`;
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id = rm.menu_id
--     WHERE m.`code` IN ('oss_add','oss_edit','oss_delete','oss_view');
--   DELETE FROM `blade_menu` WHERE `code` IN ('oss_add','oss_edit','oss_delete','oss_view');
--
-- 背景
--   对标 BladeX 商业版「对象存储」页：维护多套 OSS 配置（分类/资源编号/资源地址/
--   外网地址/空间名/访问策略/accessKey/secretKey/region），
--   「启用」其中一条后，blade-resource 管理端点优先使用该配置（OssBuilder 动态构建）。
-- =============================================================================

USE `blade`;

-- 1) 建表 -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_oss` (
  `id`            bigint       NOT NULL                  COMMENT '主键',
  `tenant_id`     varchar(12)  DEFAULT '000000'          COMMENT '租户ID',
  `category`      varchar(32)  NOT NULL                  COMMENT '分类（qiniu/alioss/tencent/huawei/amazon/local/minio/rustfs）',
  `oss_code`      varchar(64)  NOT NULL                  COMMENT '资源编号',
  `endpoint`      varchar(255) DEFAULT NULL              COMMENT '资源地址',
  `domain`        varchar(255) DEFAULT NULL              COMMENT '外网地址',
  `bucket_name`   varchar(128) DEFAULT NULL              COMMENT '空间名',
  `access_policy` tinyint      DEFAULT 1                 COMMENT '访问策略（1公共读 2私有）',
  `access_key`    varchar(128) DEFAULT NULL              COMMENT 'accessKey',
  `secret_key`    varchar(128) DEFAULT NULL              COMMENT 'secretKey',
  `region`        varchar(64)  DEFAULT NULL              COMMENT 'region',
  `remark`        varchar(255) DEFAULT NULL              COMMENT '备注',
  `create_user`   bigint       DEFAULT NULL              COMMENT '创建人',
  `create_dept`   bigint       DEFAULT NULL              COMMENT '创建部门',
  `create_time`   datetime     DEFAULT NULL              COMMENT '创建时间',
  `update_user`   bigint       DEFAULT NULL              COMMENT '修改人',
  `update_time`   datetime     DEFAULT NULL              COMMENT '修改时间',
  `status`        int          DEFAULT 0                 COMMENT '状态（0停用 1启用）',
  `is_deleted`    int          DEFAULT 0                 COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  KEY `idx_oss_tenant` (`tenant_id`),
  KEY `idx_oss_code` (`oss_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='对象存储配置表';

-- 2) 页面菜单下挂按钮权限（查看/新增/编辑/删除）-----------------------------------
SET @m_oss = (SELECT `id` FROM `blade_menu` WHERE `code` = 'resource_oss' AND `is_deleted` = 0 LIMIT 1);

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), @m_oss, t.`code`, t.`name`, t.`alias`, '', '', t.`sort`,
       2, t.`action`, 1, 0, t.`remark`, 0, '000000', 1
FROM (
  SELECT 'oss_add'    AS `code`, '新增' AS `name`, 'add'    AS `alias`, 1 AS `sort`, 1 AS `action`, '对象存储新增按钮' AS `remark` UNION ALL
  SELECT 'oss_edit',         '编辑',        'edit',         2,      2,             '对象存储编辑按钮' UNION ALL
  SELECT 'oss_delete',       '删除',        'delete',       3,      3,             '对象存储删除按钮' UNION ALL
  SELECT 'oss_view',         '查看',        'view',         4,      0,             '对象存储查看按钮'
) t
WHERE @m_oss IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` b WHERE b.`code` = t.`code` AND b.`is_deleted` = 0);

-- 3) 按钮授权给「超级管理员」角色-------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r ON r.`role_alias` = 'administrator' AND r.`is_deleted` = 0
WHERE m.`code` IN ('oss_add', 'oss_edit', 'oss_delete', 'oss_view') AND m.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`);

-- 4) 校验输出 --------------------------------------------------------------------
SELECT `id`, `parent_id`, `code`, `name`, `category`, `action`
FROM `blade_menu`
WHERE (`code` = 'resource_oss' OR `parent_id` = @m_oss) AND `is_deleted` = 0
ORDER BY `category`, `sort`;
