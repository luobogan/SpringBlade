-- =====================================================================
-- 用户创建 P0：blade_user 追加字段（对齐 ecology hrmresource）
-- 见 doc/md/用户创建功能改造计划-对齐ecology架构.md §5-P0 / §9.2
-- 说明：
--   1) 唯一键放 DB 层（三道防线之最后一道）；
--   2) ⚠️ blade_user 为逻辑删除（is_deleted），删除用户时代码会将 work_code 置 NULL，
--      否则唯一键会导致工号永久无法复用；
--   3) person_status 枚举：0 试用 / 1 正式 / 2 临时 / 3 延期 / 4 解聘 / 5 退休
--      （新建默认“正式”=1；PmAction 的 0-8 是流程场景类型，勿混用）。
-- =====================================================================

ALTER TABLE blade_user
  ADD COLUMN work_code       varchar(50) NULL COMMENT '工号(对齐ecology workcode),租户内唯一,可由编码规则自动生成' AFTER account,
  ADD COLUMN person_status   tinyint     NOT NULL DEFAULT 1 COMMENT '人员状态:0试用 1正式 2临时 3延期 4解聘 5退休;新建默认正式' AFTER sex,
  ADD COLUMN certificate_num varchar(30) NULL COMMENT '证件号(对齐ecology certificatenum),主账号范围业务唯一(应用层校验)' AFTER birthday,
  ADD COLUMN salt            varchar(64) NULL COMMENT '密码盐值(预留,配合加盐算法惰性升级)' AFTER password;

ALTER TABLE blade_user
  ADD UNIQUE KEY uk_blade_user_tenant_workcode (tenant_id, work_code);
