-- =====================================================================
-- 版本      : V2026.11.13_001
-- 目标库    : blade
-- 影响表    : blade_scope_field（新建）
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  :
--   DROP TABLE IF EXISTS `blade_scope_field`;
--
-- 背景：
--   blade_scope_data.scope_field 只是「SQL SELECT 列清单字符串」，由
--   BladeDataScopeHandler 直接拼进 select {} from (...)，无转义、无元数据。
--   它无法承载字段中文名、类型、说明、列顺序、是否可编辑，因此业务列表
--   无法据此动态生成表头。本表补充「可见字段展示配置」，用于列表动态列。
--
-- 与现有数据权限的关联：
--   1) scope_id -> blade_scope_data.id（主关联，精确到某条数据权限规则）
--   2) menu_id  -> blade_menu.id（冗余；业务列表按当前菜单直接查列配置，
--      避免「一个菜单有多条数据权限规则」时无法定位）
--   3) resource_code 冗余，便于按资源编号查询
--   4) scope_id = 0 表示「菜单级默认配置」，未绑定具体数据权限规则
--   5) field_name 与 blade_scope_data.scope_field 中的列名保持一致：
--      可见字段集合即 SQL SELECT 列集合；保存时前端同步两者
--   6) 保存采用「按 (scope_id, menu_id) 做差集合并」：同名字段更新、
--      新增字段插入、本次未提交的字段逻辑删除；因 BaseEntity 带
--      @TableLogic（逻辑删除而非物理删除），不能建唯一索引，
--      否则重新插入同名字段会与已逻辑删除的旧行冲突。
--
-- 安全：field_name 为 SQL 标识符，后端写入时按 ^[A-Za-z_][A-Za-z0-9_$]*$
--       白名单校验，禁止分号/引号/注释等注入字符。
--
-- 执行要求：项目未启用 Flyway，请在 blade 库手工执行本文件。
-- =====================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_scope_field` (
  `id`              bigint       NOT NULL                COMMENT '主键（雪花）',
  `scope_id`        bigint       NOT NULL DEFAULT 0      COMMENT '数据权限ID（blade_scope_data.id）；0=菜单级默认配置',
  `menu_id`         bigint       DEFAULT NULL            COMMENT '菜单ID（blade_menu.id）；业务列表按此维度查询',
  `resource_code`   varchar(255) DEFAULT NULL           COMMENT '资源编号（冗余，便于按编码查询）',
  `field_id`        bigint       DEFAULT NULL            COMMENT '源字段定义ID（workflow_billfield.id）；自定义字段为空',
  `field_name`      varchar(128) NOT NULL                COMMENT '字段名（数据库列名，对应 SQL SELECT 项）',
  `field_label`     varchar(255) DEFAULT NULL            COMMENT '字段名称（列表表头显示名，中文）',
  `field_db_type`   varchar(64)  DEFAULT NULL            COMMENT '数据库字段类型（varchar/datetime/decimal...），决定列表渲染',
  `field_html_type` varchar(32)  DEFAULT NULL            COMMENT '展示/控件类型',
  `description`     varchar(500) DEFAULT NULL            COMMENT '字段说明',
  `is_visible`      tinyint      NOT NULL DEFAULT 1      COMMENT '是否可见：1是 0否',
  `is_editable`     tinyint      NOT NULL DEFAULT 0      COMMENT '是否可编辑：1是 0否',
  `sort`            int          NOT NULL DEFAULT 0      COMMENT '列展示顺序（升序）',
  `width`           int          DEFAULT NULL            COMMENT '列宽（px）',
  `align`           varchar(16)  DEFAULT NULL            COMMENT '对齐方式：left/center/right',
  `tenant_id`       varchar(64)  DEFAULT '000000'        COMMENT '租户ID',
  `create_user`     bigint       DEFAULT NULL            COMMENT '创建人',
  `create_dept`     bigint       DEFAULT NULL            COMMENT '创建部门',
  `create_time`     datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_user`     bigint       DEFAULT NULL            COMMENT '修改人',
  `update_time`     datetime     DEFAULT NULL            COMMENT '修改时间',
  `status`          int          NOT NULL DEFAULT 1      COMMENT '状态：1正常 0禁用',
  `is_deleted`      int          NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_scope_menu_field` (`scope_id`, `menu_id`, `field_name`),
  KEY `idx_scope_id` (`scope_id`),
  KEY `idx_menu_id` (`menu_id`),
  KEY `idx_resource_code` (`resource_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='数据权限可见字段配置表（列表动态列）';
