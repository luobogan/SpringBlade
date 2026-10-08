-- =====================================================================
-- 部门/分部改造：blade_dept 追加字段（对齐 ecology hrmsubcompany/hrmdepartment）
-- 见 doc/md/用户创建功能改造计划-对齐ecology架构.md 第 8 章 §8.5 方案 A
-- 说明：
--   1) 单表树以 dept_type 区分分部(1)/部门(2)，替代 Ecology 双表模型；
--   2) ⚠️ blade_dept 为逻辑删除（is_deleted），删除组织时代码会将 dept_code 置 NULL，
--      否则唯一键会导致编号永久无法复用；
--   3) canceled=封存（可解封），语义 ≠ is_deleted（逻辑删除）。
-- =====================================================================

ALTER TABLE blade_dept
  ADD COLUMN dept_type       tinyint     NOT NULL DEFAULT 2 COMMENT '节点类型:1分部 2部门(对齐ecology hrmsubcompany/hrmdepartment双表模型)' AFTER parent_id,
  ADD COLUMN dept_code       varchar(50) NULL COMMENT '编号(对齐subcompanycode/departmentcode),租户内唯一' AFTER dept_name,
  ADD COLUMN subcompany_id   bigint      NULL COMMENT '所属分部节点id(对齐hrmdepartment.subcompanyid1冗余外键),仅部门节点填写' AFTER dept_code,
  ADD COLUMN canceled        tinyint     NOT NULL DEFAULT 0 COMMENT '封存标志:0正常 1封存(可解封,语义≠is_deleted)' AFTER sort,
  ADD COLUMN manager_user_id bigint      NULL COMMENT '部门主管/协管人用户id(对齐coadjutant)' AFTER canceled,
  ADD COLUMN out_key         varchar(64) NULL COMMENT '外部系统同步外键(对齐outkey)',
  ADD COLUMN sync_uuid       varchar(32) NULL COMMENT '同步UUID(对齐uuid)',
  ADD COLUMN pinyin          varchar(100) NULL COMMENT '拼音搜索列(由dept_name+full_name生成)',
  ADD COLUMN limit_users     int         NULL COMMENT '分部人员数上限(对齐limitUsers,仅超管可设)',
  ADD COLUMN dept_level      int         NULL COMMENT '层级号(由ancestors推导,10级上限)';

ALTER TABLE blade_dept
  ADD UNIQUE KEY uk_blade_dept_tenant_code (tenant_id, dept_code),
  ADD INDEX idx_blade_dept_subcompany (subcompany_id),
  ADD INDEX idx_blade_dept_type (tenant_id, dept_type);

-- =====================================================================
-- 组织编码规则表（对齐 ecology CodeRuleManager / hrm_coderulereserved 简化版）
-- 供工号(work_code)/部门编号(dept_code) 自动生成：prefix + yyyyMMdd + 序列
-- =====================================================================
CREATE TABLE IF NOT EXISTS blade_code_rule (
  id          bigint      NOT NULL COMMENT '主键',
  rule_code   varchar(50) NOT NULL COMMENT '规则编码:USER=工号 / DEPT=部门编号',
  tenant_id   varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  prefix      varchar(20) NOT NULL DEFAULT '' COMMENT '前缀',
  date_fmt    varchar(20) NULL COMMENT '日期段格式(如 yyyyMMdd),NULL 表示无日期段',
  seq_len     int         NOT NULL DEFAULT 4 COMMENT '序列位数(左补零)',
  current_seq bigint      NOT NULL DEFAULT 0 COMMENT '当前序列值(全局递增)',
  remark      varchar(255) NULL COMMENT '备注',
  create_user bigint      NULL COMMENT '创建人',
  create_dept bigint      NULL COMMENT '创建部门',
  create_time datetime    NULL COMMENT '创建时间',
  update_user bigint      NULL COMMENT '更新人',
  update_time datetime    NULL COMMENT '更新时间',
  status      int         NOT NULL DEFAULT 1 COMMENT '状态',
  is_deleted  int         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_blade_code_rule (tenant_id, rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织编码规则表';

-- 已建表补列（TenantEntity 审计列；幂等需按列判断，此处为一次性修复语句）
ALTER TABLE blade_code_rule
  ADD COLUMN create_user bigint   NULL COMMENT '创建人',
  ADD COLUMN create_dept bigint   NULL COMMENT '创建部门',
  ADD COLUMN update_user bigint   NULL COMMENT '更新人';

-- 初始化工号规则：前缀 EMP + yyyyMMdd + 4 位序列（000000 租户）
INSERT INTO blade_code_rule (id, rule_code, tenant_id, prefix, date_fmt, seq_len, current_seq, remark, status, is_deleted)
SELECT 1948000000000000001, 'USER', '000000', 'EMP', 'yyyyMMdd', 4, 0,
       '工号编码规则:EMP+日期+4位序列', 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM blade_code_rule WHERE rule_code = 'USER' AND tenant_id = '000000');
