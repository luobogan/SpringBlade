-- ============================================================================
-- Flowable 引擎 7.1.0 -> 8.0.0 数据库升级脚本（MySQL，仅 BPMN 部分）
-- ============================================================================
-- 来源：flowable-engine 官方 distro/sql/upgrade/all/
--        flowable.mysql.upgradestep.7.1.0.to.7.2.0.all.sql
--        flowable.mysql.upgradestep.7.2.0.to.8.0.0.all.sql
-- 已剔除：CMMN / DMN / event-registry 相关语句（本项目未使用这些引擎）
--
-- 执行库：blade_workflow（存放 wf_* 与 ACT_* 的库，与 blade 库不同）
-- 前提：已把项目 pom 的 flowable.version 改为 8.0.0
--       （blade.flowable.database-schema-update=none，引擎不会自动升级，必须手工执行）
--
-- 安全性：全部为 ADD COLUMN / CREATE INDEX / UPDATE 版本号
--         无删列、无改列、无数据重写 -> 可在线执行、可回滚（见文末回滚段）
-- ============================================================================


-- ---------------------------------------------------------------------------
-- 步骤 0：执行前先记录当前版本（用于回滚 / 核对）
-- ---------------------------------------------------------------------------
SELECT NAME_, VALUE_
FROM ACT_GE_PROPERTY
WHERE NAME_ IN ('schema.version', 'common.schema.version');
-- 期望看到：schema.version = 7.1.0.x（若为 6.x 或其它，说明不是从 7.1.0 起步，需先补中间脚本）


-- ---------------------------------------------------------------------------
-- 步骤 1：7.1.0 -> 7.2.0（BPMN 部分）
-- ---------------------------------------------------------------------------
alter table ACT_RU_ACTINST add column COMPLETED_BY_ varchar(255);
alter table ACT_HI_ACTINST add column COMPLETED_BY_ varchar(255);

create index ACT_IDX_BYTEAR_DEPL on ACT_GE_BYTEARRAY(DEPLOYMENT_ID_);

update ACT_GE_PROPERTY set VALUE_ = '7.2.0.2' where NAME_ = 'common.schema.version';
update ACT_GE_PROPERTY set VALUE_ = '7.2.0.2' where NAME_ = 'schema.version';
update ACT_GE_PROPERTY set VALUE_ = '7.2.0.2' where NAME_ = 'app.schema.version';
update ACT_GE_PROPERTY set VALUE_ = '7.2.0.2' where NAME_ = 'cmmn.schema.version';
update ACT_GE_PROPERTY set VALUE_ = '7.2.0.2' where NAME_ = 'dmn.schema.version';
update ACT_GE_PROPERTY set VALUE_ = '7.2.0.2' where NAME_ = 'eventregistry.schema.version';

-- 可选：IDM 引擎属性表。若库中没有 ACT_ID_PROPERTY 表，此行会报错，可删除本行
update ACT_ID_PROPERTY set VALUE_ = '7.2.0.2' where NAME_ = 'schema.version';


-- ---------------------------------------------------------------------------
-- 步骤 2：7.2.0 -> 8.0.0（BPMN 部分）
-- ---------------------------------------------------------------------------
alter table ACT_HI_PROCINST add column END_USER_ID_ varchar(255);
alter table ACT_HI_PROCINST add column STATE_ varchar(255);

update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'common.schema.version';
update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'schema.version';
update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'app.schema.version';
update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'cmmn.schema.version';
update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'dmn.schema.version';
update ACT_GE_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'eventregistry.schema.version';

-- 可选：同上，无 ACT_ID_PROPERTY 表时删除本行
update ACT_ID_PROPERTY set VALUE_ = '8.0.0.0' where NAME_ = 'schema.version';


-- ---------------------------------------------------------------------------
-- 步骤 3：验证
-- ---------------------------------------------------------------------------
SELECT NAME_, VALUE_
FROM ACT_GE_PROPERTY
WHERE NAME_ IN ('schema.version', 'common.schema.version');
-- 期望：均为 8.0.0.0

-- 验证新列已生效
SELECT COUNT(*) AS has_column
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND ((TABLE_NAME = 'ACT_HI_PROCINST' AND COLUMN_NAME IN ('END_USER_ID_', 'STATE_'))
    OR (TABLE_NAME IN ('ACT_RU_ACTINST', 'ACT_HI_ACTINST') AND COLUMN_NAME = 'COMPLETED_BY_'));
-- 期望：4（ACT_HI_PROCINST 两列 + ACT_RU_ACTINST 一列 + ACT_HI_ACTINST 一列）


-- ============================================================================
-- 回滚段（仅在需要回退时执行；执行前请确认已把 pom 改回 7.1.0）
-- ============================================================================
-- alter table ACT_HI_PROCINST drop column END_USER_ID_;
-- alter table ACT_HI_PROCINST drop column STATE_;
-- alter table ACT_RU_ACTINST  drop column COMPLETED_BY_;
-- alter table ACT_HI_ACTINST  drop column COMPLETED_BY_;
-- drop index ACT_IDX_BYTEAR_DEPL on ACT_GE_BYTEARRAY;
--
-- update ACT_GE_PROPERTY set VALUE_ = '<步骤0记录的原值>' where NAME_ = 'schema.version';
-- update ACT_GE_PROPERTY set VALUE_ = '<步骤0记录的原值>' where NAME_ = 'common.schema.version';
-- （app / cmmn / dmn / eventregistry 同理改回原值）
--
-- 更稳妥的回滚方式：直接用执行前做的库备份恢复。
-- ============================================================================
