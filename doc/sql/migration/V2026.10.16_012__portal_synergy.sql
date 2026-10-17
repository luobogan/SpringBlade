-- =============================================================================
-- 迁移脚本 V2026.10.16_012：门户协同区（synergy）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_012
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_synergy_area；blade_menu（协同区页菜单 1 + 权限按钮 2）、blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §15.7 C2（synergy 协同区）
--             E-cology：知识/流程/流程表单/流程节点四类协同，以 hpid 操作 hpinfo_workflow/synergy_base；
--             宽度设置/启用禁用/参数/新建页与查看处理页两种页面。
-- 本地化说明:
--             1) 协同区=把「门户上一块绑定流程域数据的面板」做成可治理配置：本模块实现
--                【流程(workflow)/流程表单(form)/流程节点(node)】三类协同（知识型延后）；
--                深度对接 blade-workflow：绑定流程定义/节点/表单字段均经 blade-workflow 内部
--                Feign 端点（definitionOptions/definitionNodes/definitionFields）拉取真实选项；
--             2) 复用 blade_portal_* 表族与既有 CRUD/审计/权限横切；不建独立 synergy_base 旧表；
--             3) 每个协同区含：绑定对象(bind_id/name + 可选 node_id/name、form_id)、启用(enabled)、
--                宽度(width,1-24 栅格)、扩展参数(params_json)；运行时由首页按 area_type 拉取
--                blade-workflow 数据渲染（本期完成配置中心，首页渲染为后续消费点）；
--             4) 唯一键(tenant_id,area_type,bind_id,node_id) 防止同租户同流程同节点重复登记
--                （node_id/form_id 不允许 NULL，统一空串 '' 以保证唯一键语义正确）。
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；菜单按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_synergy','portal_synergy_manage','portal_synergy_maint');
--   DELETE FROM `blade_menu`
--     WHERE `code` IN ('portal_synergy','portal_synergy_manage','portal_synergy_maint');
--   DROP TABLE IF EXISTS `blade_portal_synergy_area`;
-- =============================================================================

USE `blade`;

-- 1) 协同区配置表 ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_synergy_area` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `area_type`   varchar(20)  NOT NULL                COMMENT '协同区类型：workflow流程/form流程表单/node流程节点',
  `title`       varchar(128)          DEFAULT NULL   COMMENT '显示标题',
  `bind_id`     varchar(64)  NOT NULL DEFAULT ''     COMMENT '绑定流程定义ID（字符串，兼容雪花ID）',
  `bind_name`   varchar(128)          DEFAULT NULL   COMMENT '绑定流程名称（保存时快照）',
  `node_id`     varchar(64)  NOT NULL DEFAULT ''     COMMENT '绑定节点ID（仅 node 类型；空串表示无）',
  `node_name`   varchar(128)          DEFAULT NULL   COMMENT '绑定节点名称',
  `form_id`     varchar(64)  NOT NULL DEFAULT ''     COMMENT '绑定表单ID（仅 form 类型；空串表示无）',
  `enabled`     int                   DEFAULT '1'    COMMENT '是否启用（1启用 0禁用）',
  `width`       int                   DEFAULT '12'   COMMENT '显示宽度（栅格 1-24）',
  `params_json` text                  DEFAULT NULL   COMMENT '扩展参数（如展示字段键列表/排序/最大条数）',
  `is_deleted`  int                   DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`      int                   DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user` bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept` bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time` datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user` bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time` datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_area` (`tenant_id`, `area_type`, `bind_id`, `node_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_status` (`status`),
  KEY `idx_area_type` (`area_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户协同区配置（对标泛微 synergy §15.7 C2；知识型延后）';

-- 2) 管理页菜单（普通子菜单，两段连写路径 → ./pages/Portal/Synergy/index.tsx）------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_synergy', '协同区', 'portal_synergy', '/portal/synergy', 'api', 20,
       1, 1, 0, 0,
       '协同区（普通子菜单 → 组件 ./pages/Portal/Synergy/index.tsx）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_synergy' AND m.`is_deleted` = 0);

-- 3) 权限按钮（category=2）：前端按钮门禁 + 后端权限码 ---------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_synergy_manage' AS `code`, '协同区管理按钮' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_synergy_maint',   '协同区维护权限', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 4) 授权：仅超级管理员 -------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r
  ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0
  AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_synergy','portal_synergy_manage','portal_synergy_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 5) 校验输出 ---------------------------------------------------------------------
SELECT `area_type`, `bind_id`, `bind_name`, `node_id`, `node_name`
FROM `blade_portal_synergy_area` WHERE `is_deleted` = 0;
