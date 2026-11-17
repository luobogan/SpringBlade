-- =============================================================================
-- 迁移脚本 V2026.11.17_004：单条数据共享表
-- -----------------------------------------------------------------------------
-- 版本      : V2026.11.17_004
-- 目标库    : blade
-- 影响表    : modedatashare_single（新建）
-- 变更类型  : 结构（新表）
-- 是否幂等  : 是（IF NOT EXISTS）
-- 是否丢数据: 否
-- 回滚脚本  : DROP TABLE IF EXISTS modedatashare_single;
--
-- 背景
--   E9 支持「把某一条单据单独共享给某人/角色/部门」（ModeShareIframe.jsp + modeDataShare_<id>）。
--   SB 此前只有模块级共享规则，无法表达单条数据授权。本表补齐该能力：
--   按 (modeId, dataId) 授权给主体，行级过滤时与「本人创建」OR 叠加。
-- =============================================================================

USE `blade`;

CREATE TABLE IF NOT EXISTS modedatashare_single (
  id           bigint       NOT NULL COMMENT '主键',
  modeid       bigint       NOT NULL COMMENT '模块ID',
  data_id      bigint       NOT NULL COMMENT '业务数据行ID',
  objtype      tinyint      NOT NULL COMMENT '授权对象：1角色 2部门 3岗位 4人员',
  objid        bigint       NOT NULL COMMENT '授权对象ID',
  share_level  tinyint      NOT NULL DEFAULT 1 COMMENT '共享级别：1查看 2编辑（对齐 E9 sharelevel）',
  tenant_id    varchar(32)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  create_time  datetime     NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_mode_data (modeid, data_id),
  KEY idx_subject (objtype, objid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='建模单条数据共享授权';

SHOW TABLES LIKE 'modedatashare_single';
