-- =============================================================================
-- 迁移脚本 001：配置驱动门户引擎 —— 建表 + 种子数据
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.13_001
-- 目标库    : blade（dev 多服务共用数据源，blade-portal 直接读 blade_notice）
-- 影响表    : 新建 blade_portal_page / blade_portal_element /
--                    blade_portal_page_share / blade_portal_element_setting
-- 变更类型  : DDL + DML
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS；种子数据按 page_code 判重）
-- 是否丢数据: 否（纯新增表）
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md
--              泛微 E-cology：homepage（页面容器）→ 本表 blade_portal_page
--                        hpelement（元素定义）  → 本表 blade_portal_element
--                        元素共享（sharelevel）  → 本表 blade_portal_page_share
--                        hpElementSettingDetail（用户级个性化）
--                                                    → 本表 blade_portal_element_setting
--            ⚠️ 蓝本第 11.1 节修正结论：元素定义与用户个性化是**两层**，
--               per_page/show_title 属于个性化层，不放在元素定义表里。
-- 回滚脚本  :
--   DELETE FROM `blade_portal_element_setting` WHERE `element_id` IN
--     (SELECT `id` FROM `blade_portal_element`);
--   DELETE FROM `blade_portal_page_share`;
--   DELETE FROM `blade_portal_element`;
--   DELETE FROM `blade_portal_page`;
--   DROP TABLE IF EXISTS `blade_portal_element_setting`;
--   DROP TABLE IF EXISTS `blade_portal_page_share`;
--   DROP TABLE IF EXISTS `blade_portal_element`;
--   DROP TABLE IF EXISTS `blade_portal_page`;
-- =============================================================================

USE `blade`;

-- -----------------------------------------------------------------------------
-- 1) 门户页面表（对标 homepage）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_page` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `page_code`   varchar(64)  NOT NULL                COMMENT '页面编码（唯一，如 default）',
  `page_name`   varchar(128) NOT NULL                COMMENT '页面名称',
  `is_default`  int                   DEFAULT '0'    COMMENT '是否默认门户（0否 1是）',
  `sort`        int                   DEFAULT '1'    COMMENT '显示顺序',
  `theme`       varchar(64)           DEFAULT 'default' COMMENT '主题标识（预留）',
  `remark`      varchar(255)          DEFAULT NULL   COMMENT '备注',
  `is_deleted`  int                   DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`      int                   DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user` bigint                DEFAULT NULL   COMMENT '创建人',
  `create_dept` bigint                DEFAULT NULL   COMMENT '创建部门',
  `create_time` datetime              DEFAULT NULL   COMMENT '创建时间',
  `update_user` bigint                DEFAULT NULL   COMMENT '修改人',
  `update_time` datetime              DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_code` (`page_code`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户页面配置表（对标泛微 homepage）';

-- -----------------------------------------------------------------------------
-- 2) 门户元素表（对标 hpelement —— 元素定义，全局一份）
--    注意：per_page/show_title 不在此表，属 blade_portal_element_setting（用户个性化）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_element` (
  `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    varchar(12)          DEFAULT '000000' COMMENT '租户ID',
  `page_id`      bigint      NOT NULL                COMMENT '所属页面ID（blade_portal_page.id）',
  `element_type` varchar(32) NOT NULL                COMMENT '元素类型：todo待办/notice通知公告/message消息/chart图表/links快捷链接/rich富文本',
  `element_name` varchar(128)         DEFAULT NULL   COMMENT '元素标题（卡片显示名）',
  `show_title`   int                  DEFAULT '1'    COMMENT '是否显示卡片标题（0否 1是）',
  `row_no`       int                  DEFAULT '1'    COMMENT '所在行号（同一行按 col_span 依次排布）',
  `col_span`     int                  DEFAULT '12'   COMMENT '栅格宽度（antd 24 栅格：4/8/12/16/24）',
  `sort_order`   int                  DEFAULT '0'    COMMENT '同页排序号',
  `config_json`  text                 DEFAULT NULL   COMMENT '元素私有配置JSON（条数/链接列表/图表配置/富文本HTML等）',
  `style_json`   text                 DEFAULT NULL   COMMENT '元素样式配置JSON（预留）',
  `is_deleted`   int                  DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`       int                  DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user`  bigint               DEFAULT NULL   COMMENT '创建人',
  `create_dept`  bigint               DEFAULT NULL   COMMENT '创建部门',
  `create_time`  datetime             DEFAULT NULL   COMMENT '创建时间',
  `update_user`  bigint               DEFAULT NULL   COMMENT '修改人',
  `update_time`  datetime             DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_page_id` (`page_id`),
  KEY `idx_type` (`element_type`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户元素配置表（对标泛微 hpelement）';

-- -----------------------------------------------------------------------------
-- 3) 门户页面授权表（对标元素共享范围 sharelevel）
--    share_type：1=全员可见 2=部门可见（share_id=部门ID） 3=个人可见（share_id=用户ID）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_page_share` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   varchar(12)          DEFAULT '000000' COMMENT '租户ID',
  `page_id`     bigint      NOT NULL                COMMENT '页面ID',
  `share_type`  int         NOT NULL                COMMENT '共享范围（1全员 2部门 3个人）',
  `share_id`    bigint               DEFAULT '0'    COMMENT '共享对象ID（部门ID或用户ID；全员为0）',
  `is_deleted`  int                  DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`      int                  DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user` bigint               DEFAULT NULL   COMMENT '创建人',
  `create_time` datetime             DEFAULT NULL   COMMENT '创建时间',
  `update_user` bigint               DEFAULT NULL   COMMENT '修改人',
  `update_time` datetime             DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_page_id` (`page_id`),
  KEY `idx_share` (`share_type`, `share_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户页面授权表（全员/部门/个人）';

-- -----------------------------------------------------------------------------
-- 4) 门户元素用户个性化表（对标 hpElementSettingDetail）
--    以 blade_portal_element 为基准，本表按 user_id 覆盖 per_page/show_title
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blade_portal_element_setting` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   varchar(12)          DEFAULT '000000' COMMENT '租户ID',
  `element_id`  bigint      NOT NULL                COMMENT '元素ID（blade_portal_element.id）',
  `user_id`     bigint      NOT NULL                COMMENT '用户ID',
  `per_page`    int                  DEFAULT '5'    COMMENT '该用户下此元素显示条数',
  `show_title`  int                  DEFAULT NULL   COMMENT '该用户下是否显示标题（NULL=沿用元素定义）',
  `style_json`  text                 DEFAULT NULL   COMMENT '该用户下样式覆盖JSON',
  `is_deleted`  int                  DEFAULT '0'    COMMENT '是否删除（0未删除 1已删除）',
  `status`      int                  DEFAULT '1'    COMMENT '状态（1正常 0禁用 -1已删除）',
  `create_user` bigint               DEFAULT NULL   COMMENT '创建人',
  `create_time` datetime             DEFAULT NULL   COMMENT '创建时间',
  `update_user` bigint               DEFAULT NULL   COMMENT '修改人',
  `update_time` datetime             DEFAULT NULL   COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_element_user` (`element_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户元素用户个性化表（对标泛微 hpElementSettingDetail）';

-- -----------------------------------------------------------------------------
-- 5) 种子数据：默认门户 + 4 个示例元素 + 全员可见
-- -----------------------------------------------------------------------------
INSERT INTO `blade_portal_page`
  (`tenant_id`, `page_code`, `page_name`, `is_default`, `sort`, `remark`, `is_deleted`, `status`, `create_time`)
SELECT '000000', 'default', '集团门户', 1, 1, '系统默认门户页', 0, 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `blade_portal_page` WHERE `page_code` = 'default');

SET @page_id = (SELECT `id` FROM `blade_portal_page` WHERE `page_code` = 'default' AND `is_deleted` = 0 LIMIT 1);

-- 5.1 元素（仅在该页尚无元素时插入，避免重复播种）
INSERT INTO `blade_portal_element`
  (`tenant_id`, `page_id`, `element_type`, `element_name`, `show_title`, `row_no`, `col_span`, `sort_order`, `config_json`, `is_deleted`, `status`, `create_time`)
SELECT '000000', @page_id, 'notice', '通知公告', 1, 1, 12, 1, '{"perPage":5,"showContent":false}', 0, 1, NOW()
FROM DUAL
WHERE @page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_portal_element` WHERE `page_id` = @page_id AND `element_type` = 'notice' AND `is_deleted` = 0);

INSERT INTO `blade_portal_element`
  (`tenant_id`, `page_id`, `element_type`, `element_name`, `show_title`, `row_no`, `col_span`, `sort_order`, `config_json`, `is_deleted`, `status`, `create_time`)
SELECT '000000', @page_id, 'todo', '我的待办', 1, 1, 12, 2, '{"perPage":5}', 0, 1, NOW()
FROM DUAL
WHERE @page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_portal_element` WHERE `page_id` = @page_id AND `element_type` = 'todo' AND `is_deleted` = 0);

INSERT INTO `blade_portal_element`
  (`tenant_id`, `page_id`, `element_type`, `element_name`, `show_title`, `row_no`, `col_span`, `sort_order`, `config_json`, `is_deleted`, `status`, `create_time`)
SELECT '000000', @page_id, 'message', '消息提醒', 1, 2, 12, 3, '{"perPage":8}', 0, 1, NOW()
FROM DUAL
WHERE @page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_portal_element` WHERE `page_id` = @page_id AND `element_type` = 'message' AND `is_deleted` = 0);

INSERT INTO `blade_portal_element`
  (`tenant_id`, `page_id`, `element_type`, `element_name`, `show_title`, `row_no`, `col_span`, `sort_order`, `config_json`, `is_deleted`, `status`, `create_time`)
SELECT '000000', @page_id, 'links', '快捷入口', 1, 2, 12, 4,
       '{"links":[{"name":"新建流程","icon":"file-add","url":"/workflow/create"},{"name":"消息中心","icon":"message","url":"/message"},{"name":"账号设置","icon":"user","url":"/account/settings"}]}',
       0, 1, NOW()
FROM DUAL
WHERE @page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_portal_element` WHERE `page_id` = @page_id AND `element_type` = 'links' AND `is_deleted` = 0);

-- 5.2 全员可见
INSERT INTO `blade_portal_page_share`
  (`tenant_id`, `page_id`, `share_type`, `share_id`, `is_deleted`, `status`, `create_time`)
SELECT '000000', @page_id, 1, 0, 0, 1, NOW()
FROM DUAL
WHERE @page_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `blade_portal_page_share` WHERE `page_id` = @page_id AND `share_type` = 1 AND `is_deleted` = 0);

-- -----------------------------------------------------------------------------
-- 6) 校验输出
-- -----------------------------------------------------------------------------
SELECT p.`id`, p.`page_code`, p.`page_name`, p.`is_default`,
       (SELECT COUNT(*) FROM `blade_portal_element` e
         WHERE e.`page_id` = p.`id` AND e.`is_deleted` = 0) AS element_count,
       (SELECT COUNT(*) FROM `blade_portal_page_share` s
         WHERE s.`page_id` = p.`id` AND s.`is_deleted` = 0) AS share_count
  FROM `blade_portal_page` p
 WHERE p.`is_deleted` = 0;

SELECT e.`id`, e.`page_id`, e.`element_type`, e.`element_name`, e.`row_no`, e.`col_span`, e.`sort_order`
  FROM `blade_portal_element` e
 WHERE e.`is_deleted` = 0
 ORDER BY e.`page_id`, e.`row_no`, e.`sort_order`;