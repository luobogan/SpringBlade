-- -----------------------------------------------------------------------------
-- 迁移脚本：公文电子签章（e签宝 SaaS API V3）基础表
-- -----------------------------------------------------------------------------
-- 版本      : V2026.10.25_001
-- 目标库    : blade（本 dev 环境各服务共用 blade 库，wf_* 表均在其中；
--              生产若按服务拆库则为 blade_workflow，执行前请先确认 wf_odoc_doc 实际所在库）
-- 影响表    : wf_odoc_sign_flow、wf_odoc_sign_signer、wf_odoc_sign_notify
-- 变更类型  : DDL（新建表）
-- 是否幂等  : 是（CREATE TABLE IF NOT EXISTS，可重复执行）
-- 回滚      : DROP TABLE wf_odoc_sign_notify; DROP TABLE wf_odoc_sign_signer; DROP TABLE wf_odoc_sign_flow;
-- 设计文档  : doc/esign/电子签章集成技术方案.md
--
-- 背景
--   将 e签宝电子签章能力接入公文流程：套红 + 版式转换(PDF/OFD) 之后发起签署，
--   签署完成后归档回写为公文新版本。不改动任何现有表。
--
-- 已确认决策（见设计文档 2.1）
--   D1：测试环境先用公有云 SaaS API V3；架构保持可切换（IEsignService 隔离 + host 可配）。
--       ⚠️ 涉密公文进入生产前必须复核是否需切混合云「天印」/私有化。
--   D2：印章两种方案并存，按 blade.workflow.odoc.esign.seal.mode（local|esign）启用。
--
-- 公共列口径（重要）
--   三张表的实体均继承 org.springblade.core.mp.base.TenantEntity，其父类 BaseEntity 含
--   id / create_user / create_dept / create_time / update_user / update_time / status / is_deleted，
--   MyBatis-Plus 生成的基础 SQL 会引用这些列，缺列即报 Unknown column。
--   同理参见 V2026.09.21_013__wf_base_entity_missing_cols.sql。
--
--   ⚠️ status 是 BaseEntity 的「启用/禁用」列（1正常 0禁用），不是业务状态位；
--   本模块的业务状态一律另起列名，避免语义冲突：
--     wf_odoc_sign_flow.sign_status   : 0待发起 1进行中 2已完成 3已撤销 4已拒签 5已过期 9失败
--     wf_odoc_sign_signer.sign_status : 0待签 1已签 2拒签
--     wf_odoc_sign_notify.processed   : 0待处理 1已处理 2处理失败
--   已执行过旧版脚本的环境请再执行 V2026.10.25_002__wf_odoc_esign_base_cols.sql 修正。
--
-- 状态字典
--   wf_odoc_sign_flow.seal_mode : 0自研印章(本地) 1e签宝印章
--   wf_odoc_sign_signer.signer_type : 0个人 1企业
-- -----------------------------------------------------------------------------

-- ① 签署流程主表
CREATE TABLE IF NOT EXISTS wf_odoc_sign_flow (
  id                  BIGINT       NOT NULL COMMENT '主键',
  tenant_id           VARCHAR(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  inst_id             BIGINT       NOT NULL COMMENT '流程实例ID',
  doc_id              BIGINT       NOT NULL COMMENT '公文正文ID',
  src_version_id      BIGINT       COMMENT '发起签署时的正文版本ID（原文版本，归档后仍保留）',
  esign_file_id       VARCHAR(64)  COMMENT 'e签宝文件ID',
  flow_id             VARCHAR(64)  COMMENT 'e签宝签署流程ID',
  biz_num             VARCHAR(64)  COMMENT '业务流水号(customBizNum)，发起幂等键',
  title               VARCHAR(500) COMMENT '签署标题',
  seal_mode           INT          NOT NULL DEFAULT 0 COMMENT '用印方式 0自研印章(本地) 1e签宝印章（D2 并存）',
  sign_status         INT          NOT NULL DEFAULT 0 COMMENT '签署状态 0待发起 1进行中 2已完成 3已撤销 4已拒签 5已过期 9失败',
  sign_result         INT          COMMENT 'e签宝 signResult（2=签署完成）',
  archive_version_id  BIGINT       COMMENT '归档后回写的新版本ID',
  antchain_info       VARCHAR(1000) COMMENT '区块链存证信息(JSON)',
  error_msg           VARCHAR(1000) COMMENT '失败原因',
  notify_count        INT          NOT NULL DEFAULT 0 COMMENT '回调次数',
  last_notify_time    DATETIME     COMMENT '最近回调时间',
  last_query_time     DATETIME     COMMENT '最近补偿查询时间',
  create_user         BIGINT       COMMENT '创建人',
  create_dept         BIGINT       COMMENT '创建部门',
  create_time         DATETIME     COMMENT '创建时间',
  update_user         BIGINT       COMMENT '更新人',
  update_time         DATETIME     COMMENT '更新时间',
  status              INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
  is_deleted          INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wosf_flow_id (flow_id),
  UNIQUE KEY uk_wosf_biz_num (biz_num),
  KEY idx_wosf_inst (inst_id),
  KEY idx_wosf_doc (doc_id),
  KEY idx_wosf_sign_status (sign_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公文-e签宝签署流程';

-- ② 签署方
CREATE TABLE IF NOT EXISTS wf_odoc_sign_signer (
  id            BIGINT       NOT NULL COMMENT '主键',
  tenant_id     VARCHAR(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  flow_id       BIGINT       NOT NULL COMMENT '关联 wf_odoc_sign_flow.id',
  signer_type   INT          NOT NULL COMMENT '签署方类型 0个人 1企业',
  sign_order    INT          NOT NULL DEFAULT 1 COMMENT '签署顺序',
  account_id    VARCHAR(64)  COMMENT 'e签宝账号 psnId / orgId',
  account_name  VARCHAR(200) COMMENT '签署方名称',
  psn_account   VARCHAR(64)  COMMENT '个人账号（手机号）',
  org_id_card   VARCHAR(64)  COMMENT '企业统一社会信用代码',
  auto_sign     INT          NOT NULL DEFAULT 0 COMMENT '0签署页手动 1后台自动落章',
  seal_id       VARCHAR(64)  COMMENT '印章ID：esign=e签宝 sealId；local=自研印章ID',
  sign_url      VARCHAR(1000) COMMENT '签署链接',
  sign_status   INT          NOT NULL DEFAULT 0 COMMENT '签署状态 0待签 1已签 2拒签',
  sign_time     DATETIME     COMMENT '签署时间',
  create_user   BIGINT       COMMENT '创建人',
  create_dept   BIGINT       COMMENT '创建部门',
  create_time   DATETIME     COMMENT '创建时间',
  update_user   BIGINT       COMMENT '更新人',
  update_time   DATETIME     COMMENT '更新时间',
  status        INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
  is_deleted    INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (id),
  KEY idx_woss_flow (flow_id),
  KEY idx_woss_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公文-e签宝签署方';

-- ③ 回调记录（幂等去重 + 审计）
CREATE TABLE IF NOT EXISTS wf_odoc_sign_notify (
  id            BIGINT       NOT NULL COMMENT '主键',
  tenant_id     VARCHAR(12)  NOT NULL DEFAULT '000000' COMMENT '租户ID',
  flow_id       BIGINT       COMMENT '本地主表ID（未匹配到时留空，待人工处理）',
  esign_flow_id VARCHAR(64)  COMMENT 'e签宝签署流程ID',
  action        VARCHAR(64)  COMMENT '回调事件，如 SIGN_FLOW_UPDATE',
  sign_result   INT          COMMENT '签署结果',
  body          TEXT         COMMENT '原始报文',
  sig           VARCHAR(200) COMMENT '回调签名',
  ts            VARCHAR(32)  COMMENT 'e签宝回调时间戳',
  processed     INT          NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2处理失败',
  process_msg   VARCHAR(1000) COMMENT '处理说明/失败原因',
  receive_time  DATETIME     COMMENT '接收时间',
  process_time  DATETIME     COMMENT '处理时间',
  create_user   BIGINT       COMMENT '创建人',
  create_dept   BIGINT       COMMENT '创建部门',
  create_time   DATETIME     COMMENT '创建时间',
  update_user   BIGINT       COMMENT '更新人',
  update_time   DATETIME     COMMENT '更新时间',
  status        INT          NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
  is_deleted    INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wosn_dedup (esign_flow_id, action, ts),
  KEY idx_wosn_flow (esign_flow_id),
  KEY idx_wosn_processed (processed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公文-e签宝回调记录';
