-- =====================================================================
-- P3：状态流转工作流化 + 伴生初始化
-- 对齐 ecology hrm_state_proc_set（状态变更绑定流程）与 HrmInfoStatus（信息完善度）
-- 见 doc/md/用户创建功能改造计划-对齐ecology架构.md §5-P3 / §14
--
-- 三张表：
--   blade_person_status_flow         状态流转配置（from_status -> to_status 绑定 Flowable 流程）
--   blade_person_status_flow_record  流转记录（含 instance_id，审批回调按此反查，天然幂等）
--   blade_user_complete_status       信息完善度（对齐 HrmInfoStatus）
--
-- 设计约定：
--   1) 记录表 UK(instance_id)：MySQL 唯一索引对 NULL 不生效，直改模式（instance_id 为空）可多行；
--   2) 唯一键 × 逻辑删除沿用 §6 既定对策：不做「唯一键加 is_deleted」；
--   3) 在职判定口径：person_status in (0,1,2,3,5) 放行，仅 4（解聘）拒绝登录。
-- =====================================================================

CREATE TABLE IF NOT EXISTS blade_person_status_flow (
  id            bigint      NOT NULL COMMENT '主键',
  tenant_id     varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  from_status   int         NOT NULL COMMENT '源人员状态(0试用 1正式 2临时 3延期 4解聘 5退休)',
  to_status     int         NOT NULL COMMENT '目标人员状态',
  flow_name     varchar(100) NULL COMMENT '流转名称(如 转正/离职)',
  flow_key      varchar(100) NULL COMMENT '绑定的 Flowable 流程 procKey；为空表示直改不走审批',
  callback_bean varchar(100) NULL COMMENT '流程完成回调 Bean 名(对齐 ecology hrm_state_proc_set)',
  remark        varchar(255) NULL COMMENT '备注',
  create_user   bigint      NULL COMMENT '创建人',
  create_dept   bigint      NULL COMMENT '创建部门',
  create_time   datetime    NULL COMMENT '创建时间',
  update_user   bigint      NULL COMMENT '修改人',
  update_time   datetime    NULL COMMENT '修改时间',
  status        int         NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  is_deleted    int         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_person_status_flow (tenant_id, from_status, to_status),
  KEY idx_person_status_flow_from (tenant_id, from_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员状态流转配置(对齐 ecology hrm_state_proc_set)';

CREATE TABLE IF NOT EXISTS blade_person_status_flow_record (
  id           bigint      NOT NULL COMMENT '主键',
  tenant_id    varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  user_id      bigint      NOT NULL COMMENT '用户id(blade_user.id)',
  from_status  int         NULL COMMENT '变更前人员状态',
  to_status    int         NOT NULL COMMENT '变更后人员状态',
  flow_key     varchar(100) NULL COMMENT '发起时使用的流程 procKey',
  instance_id  varchar(64) NULL COMMENT '流程实例id(wf_instance.id)；直改模式为空',
  mode         tinyint     NOT NULL DEFAULT 1 COMMENT '办理方式:1流程审批 2直接变更',
  flow_status  tinyint     NOT NULL DEFAULT 0 COMMENT '流转状态:0审批中 1通过 2驳回',
  opinion      varchar(1000) NULL COMMENT '办理/审批意见',
  starter      bigint      NULL COMMENT '发起人',
  finish_time  datetime    NULL COMMENT '办结时间',
  create_user  bigint      NULL COMMENT '创建人',
  create_dept  bigint      NULL COMMENT '创建部门',
  create_time  datetime    NULL COMMENT '创建时间',
  update_user  bigint      NULL COMMENT '修改人',
  update_time  datetime    NULL COMMENT '修改时间',
  status       int         NOT NULL DEFAULT 1 COMMENT '状态',
  is_deleted   int         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_psf_record_instance (instance_id),
  KEY idx_psf_record_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员状态流转记录';

CREATE TABLE IF NOT EXISTS blade_user_complete_status (
  id          bigint      NOT NULL COMMENT '主键',
  tenant_id   varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  user_id     bigint      NOT NULL COMMENT '用户id(blade_user.id)',
  item        varchar(20) NOT NULL COMMENT '完善项:BASE基本信息 PERSON个人信息 WORK工作信息 SYSTEM系统信息(对齐 ecology HrmInfoStatus)',
  done        tinyint     NOT NULL DEFAULT 0 COMMENT '是否完善:0否 1是',
  create_user bigint      NULL COMMENT '创建人',
  create_dept bigint      NULL COMMENT '创建部门',
  create_time datetime    NULL COMMENT '创建时间',
  update_user bigint      NULL COMMENT '修改人',
  update_time datetime    NULL COMMENT '修改时间',
  status      int         NOT NULL DEFAULT 1 COMMENT '状态',
  is_deleted  int         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_complete_status (tenant_id, user_id, item)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息完善度(对齐 ecology HrmInfoStatus)';

-- =====================================================================
-- 初始化配置（000000 租户）
-- 流转 1：转正（试用 0 -> 正式 1）走审批
-- 流转 2：离职（正式 1 -> 解聘 4）走审批
-- flow_key 统一指向同一份 BPMN「person_status_change」，
-- 具体目标状态通过流程变量 toStatus 传递（见 §14.4 决策 2）。
-- 若流程尚未部署，发起侧自动降级为「直接变更」(mode=2)，功能不受影响。
-- =====================================================================
INSERT INTO blade_person_status_flow (id, tenant_id, from_status, to_status, flow_name, flow_key, callback_bean, remark, status, is_deleted)
SELECT 1948000000000000021, '000000', 0, 1, '转正', 'person_status_change', 'personStatusFlowCallback',
       '试用转正式，需审批(对齐 ecology HrmResourceTryAction)', 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM blade_person_status_flow WHERE tenant_id = '000000' AND from_status = 0 AND to_status = 1);

INSERT INTO blade_person_status_flow (id, tenant_id, from_status, to_status, flow_name, flow_key, callback_bean, remark, status, is_deleted)
SELECT 1948000000000000022, '000000', 1, 4, '离职', 'person_status_change', 'personStatusFlowCallback',
       '正式转解聘，需审批；通过后登录将被拦截(对齐 ecology HrmResourceFireAction)', 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM blade_person_status_flow WHERE tenant_id = '000000' AND from_status = 1 AND to_status = 4);
