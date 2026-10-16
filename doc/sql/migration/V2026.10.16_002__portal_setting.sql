-- =============================================================================
-- 迁移脚本 V2026.10.16_002：门户应用设置 + 个人通用设置（对标 portalsetting / accountmore）
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.16_002
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_setting / blade_portal_account_setting；
--             blade_menu（设置页菜单 + 按钮 + 权限码）、blade_role_menu（授权）
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子/菜单按 code 判重；授权按 menu_id+role_id 判重）
-- 是否丢数据: 否
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §16.3（portalsetting 门户应用设置）
--             §14.2 I（accountmore 个人通用设置：默认字体/布局类型/个人更多菜单显隐）
--             §13.5（全局单例：首页自动刷新/RSS/记住上次访问/显示无权限元素/隐藏我的门户/
--                    菜单频率自动排序/默认展开左侧菜单/顶部菜单全称/常用菜单 + 同步到所有人）
-- 说明      : blade_portal_setting 按租户单例（对标 SystemSet + PortalThemeConfig +
--             PortalPageConfig 三表合并为一行）；blade_portal_account_setting 按用户单例
--             （对标 ACCOUNTMOREMENUINFO + HrmUserSetting 个人字体/布局）。
-- 回滚脚本  :
--   DELETE rm FROM `blade_role_menu` rm JOIN `blade_menu` m ON m.id=rm.menu_id
--     WHERE m.`code` IN ('portal_setting','portal_setting_manage','portal_setting_maint');
--   DELETE FROM `blade_menu` WHERE `code` IN ('portal_setting','portal_setting_manage','portal_setting_maint');
--   DROP TABLE IF EXISTS `blade_portal_account_setting`;
--   DROP TABLE IF EXISTS `blade_portal_setting`;
-- =============================================================================

USE `blade`;

-- -----------------------------------------------------------------------------
-- 1) 门户应用设置表（租户单例；对标泛微 portalsetting §16.3）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_setting` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`             varchar(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID（租户内唯一）',
  `need_refresh`          int          NOT NULL DEFAULT '0'    COMMENT '首页自动刷新开关（0关 1开）',
  `refresh_scope`         int          NOT NULL DEFAULT '1'    COMMENT '刷新范围（1整页 2指定元素）',
  `refresh_mins`          int          NOT NULL DEFAULT '5'    COMMENT '自动刷新间隔（分钟）',
  `rsstype`               int          NOT NULL DEFAULT '1'    COMMENT 'RSS读取类型（1客户端 2服务器）',
  `show_last_hp`          int          NOT NULL DEFAULT '1'    COMMENT '记住上次访问首页（0否 1是）',
  `show_no_right_element` int          NOT NULL DEFAULT '0'    COMMENT '显示无查看权限元素（0否 1是）',
  `hidden_my_portal`      int          NOT NULL DEFAULT '0'    COMMENT '隐藏"我的门户"入口（0否 1是）',
  `auto_sort_top_menu`    int          NOT NULL DEFAULT '0'    COMMENT '菜单按使用频率自动排序（0否 1是）',
  `default_expand_left`   int          NOT NULL DEFAULT '1'    COMMENT '默认展开左侧菜单（0否 1是）',
  `show_top_menu_full_name` int        NOT NULL DEFAULT '0'    COMMENT '顶部菜单显示全称（0否 1是）',
  `show_freq_menu`        int          NOT NULL DEFAULT '1'    COMMENT '显示常用菜单（0否 1是）',
  `quick_search_enabled`  int          NOT NULL DEFAULT '1'    COMMENT '快捷搜索总开关（0关 1开）',
  `layout_type`           int          NOT NULL DEFAULT '2'    COMMENT '门户布局类型（1舒适 2标准 3紧凑）',
  `font_size`             int          NOT NULL DEFAULT '3'    COMMENT '全局默认字体（1大 2中 3默认）',
  `remark`                varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`            int          NOT NULL DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`                int          NOT NULL DEFAULT '1'    COMMENT '状态（1正常 0禁用）',
  `create_user`           bigint                DEFAULT NULL   COMMENT '创建人',
  `create_time`           datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`           bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`           datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户应用设置（租户单例，对标泛微 portalsetting §16.3）';

-- -----------------------------------------------------------------------------
-- 2) 个人通用设置表（用户单例；对标泛微 accountmore §14.2 I）
--    font_size/layout_type 为空=沿用全局默认（对标 HrmUserSetting 缺省回退）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_account_setting` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      varchar(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `user_id`        bigint       NOT NULL                COMMENT '用户ID（用户内唯一）',
  `font_size`      int                   DEFAULT NULL   COMMENT '个人字体（1大 2中 3默认；NULL=沿用全局）',
  `layout_type`    int                   DEFAULT NULL   COMMENT '个人布局类型（1舒适 2标准 3紧凑；NULL=沿用全局）',
  `menu_config_json` text               DEFAULT NULL   COMMENT '个人"更多"菜单显隐JSON（对标 ACCOUNTMOREMENUINFO，如 {"font":1,"language":1}）',
  `remark`         varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`     int          NOT NULL DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`         int          NOT NULL DEFAULT '1'    COMMENT '状态（1正常 0禁用）',
  `create_user`    bigint                DEFAULT NULL   COMMENT '创建人',
  `create_time`    datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user`    bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time`    datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户个人通用设置（对标泛微 accountmore §14.2）';

-- -----------------------------------------------------------------------------
-- 3) 种子：默认租户全局设置（不设种也行——服务端读不到时按字段默认值兜底；
--    此处显式播种一行，便于管理端"读取-编辑"闭环）
-- -----------------------------------------------------------------------------
INSERT INTO `blade_portal_setting` (`tenant_id`, `create_time`)
SELECT '000000', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_setting` WHERE `tenant_id` = '000000' AND `is_deleted` = 0);

-- -----------------------------------------------------------------------------
-- 4) 管理菜单：门户应用设置（普通子菜单，两段连写路径 → ./pages/Portal/Setting/index.tsx）
-- -----------------------------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(), p.`id`,
       'portal_setting', '门户应用设置', 'portal_setting', '/portal/setting', 'api', 12,
       1, 1, 0, 0,
       '门户应用设置（普通子菜单 → 组件 ./pages/Portal/Setting/index.tsx；path 末段须连写 setting）',
       0, '000000', 1
FROM `blade_menu` p
WHERE p.`code` = 'portal' AND p.`is_deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `blade_menu` m WHERE m.`code` = 'portal_setting' AND m.`is_deleted` = 0);

-- 5) 权限按钮 + 权限码 -----------------------------------------------------------------
INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`,
   `category`, `action`, `is_open`, `is_component`, `remark`, `is_deleted`, `tenant_id`, `status`)
SELECT UUID_SHORT(),
       (SELECT `id` FROM `blade_menu` WHERE `code` = 'portal' AND `is_deleted` = 0 LIMIT 1),
       v.`code`, v.`name`, v.`code`, '', 'api', v.`sort`,
       2, 1, 0, 0, NULL, 0, '000000', 1
FROM (
        SELECT 'portal_setting_manage' AS `code`, '门户应用设置按钮' AS `name`, 1 AS `sort`
  UNION ALL SELECT 'portal_setting_maint',   '门户应用设置维护权限', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = v.`code` AND `is_deleted` = 0);

-- 6) 授权：仅超级管理员 -----------------------------------------------------------------
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT UUID_SHORT(), m.`id`, r.`id`
FROM `blade_menu` m
JOIN `blade_role` r ON r.`tenant_id` = m.`tenant_id` AND r.`is_deleted` = 0
WHERE m.`is_deleted` = 0 AND m.`tenant_id` = '000000'
  AND r.`role_alias` = 'administrator'
  AND m.`code` IN ('portal_setting', 'portal_setting_manage', 'portal_setting_maint')
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` rm WHERE rm.`menu_id` = m.`id` AND rm.`role_id` = r.`id`
  );

-- 7) 校验输出 ---------------------------------------------------------------------------
SELECT `tenant_id`, `need_refresh`, `refresh_mins`, `quick_search_enabled`, `layout_type`, `font_size`
  FROM `blade_portal_setting` WHERE `is_deleted` = 0;
SELECT m.`code`, m.`name`, m.`path`, m.`category`
  FROM `blade_menu` m
 WHERE m.`code` IN ('portal_setting','portal_setting_manage','portal_setting_maint') AND m.`is_deleted` = 0;
