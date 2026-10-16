-- =============================================================================
-- 迁移脚本 V2026.10.14_001：门户业务审计表 blade_portal_log
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.14_001
-- 目标库    : blade
-- 影响表    : 新建 blade_portal_log
-- 变更类型  : DDL
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS）
-- 是否丢数据: 否（纯新增表）
-- 设计蓝本  : doc/md/ecology-portal-config-analysis.md §13.4
--              泛微 PortalLogInfo（userid/type/item/sql/ip/opdate）
--              + BizLogContext / LogUtil.removeIntersectionEntry 前后值 diff
-- 回滚脚本  : DROP TABLE IF EXISTS `blade_portal_log`;
-- 说明      : 记录门户页面/元素/个性化的写操作（操作人/类型/业务标识/前后值摘要/IP/时间），
--             供管理端 GET /log/list 查询。审计不软删除；status 表示当次操作结果(1成功 0失败)。
--             大字段（before/after/params）由服务层截断后入库，避免撑爆行宽。
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS `blade_portal_log` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    varchar(12)           DEFAULT '000000' COMMENT '租户ID',
  `log_type`     varchar(32)  NOT NULL COMMENT '操作类型：PAGE_CREATE/PAGE_UPDATE/PAGE_DEFAULT/PAGE_DELETE/ELEMENT_SAVE/ELEMENT_BATCH/ELEMENT_DELETE/ELEMENT_SETTING',
  `biz_type`     varchar(16)           DEFAULT NULL COMMENT '业务对象：page/element/setting',
  `biz_key`      varchar(64)           DEFAULT NULL COMMENT '业务标识：pageId/pageCode/elementId',
  `title`        varchar(255)          DEFAULT NULL COMMENT '操作对象标题（页面名/元素名）',
  `summary`      varchar(512)          DEFAULT NULL COMMENT '变更字段摘要（如 pageName: 旧->新）',
  `before_json`  text                  DEFAULT NULL COMMENT '变更前快照（摘要JSON，截断）',
  `after_json`   text                  DEFAULT NULL COMMENT '变更后快照（摘要JSON，截断）',
  `params_json`  text                  DEFAULT NULL COMMENT '请求参数快照（截断）',
  `status`       int                   DEFAULT '1' COMMENT '操作结果（1成功 0失败）',
  `error_msg`    varchar(512)          DEFAULT NULL COMMENT '失败信息（status=0 时）',
  `create_user`  bigint                DEFAULT NULL COMMENT '操作人ID',
  `create_dept`  bigint                DEFAULT NULL COMMENT '操作部门ID',
  `create_time`  datetime              DEFAULT NULL COMMENT '操作时间',
  `ip`           varchar(64)           DEFAULT NULL COMMENT '操作IP（兼容 X-Forwarded-For）',
  PRIMARY KEY (`id`),
  KEY `idx_log_type` (`log_type`),
  KEY `idx_biz_key` (`biz_key`),
  KEY `idx_create_user` (`create_user`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门户业务审计日志表（对标泛微 PortalLogInfo）';

-- 校验输出 -------------------------------------------------------------------
SELECT COUNT(*) AS `log_rows` FROM `blade_portal_log`;
