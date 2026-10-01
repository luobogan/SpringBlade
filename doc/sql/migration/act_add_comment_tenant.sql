-- =============================================================
-- D8 / H2 / R7：ACT_HI_COMMENT 新增租户列 TENANT_ID_
-- 出处：《Flowable8承接台账模块-去wf_表改造分析.md》§15.3 H2、§17.1 D8、§17.3 R7
--       决策结论：《去wf_表-D系列决策结论.md》D8（2026-10-01 拍板：加列）
--
-- 背景：act_hi_comment 原生无 TENANT_ID_ 列（§13.8 实测），审批日志按租户检索
--       只能 JOIN ACT_HI_PROCINST（PROC_INST_ID_）继承租户。为支撑
--       「按租户横扫审批意见」高频场景，决策直接加列；既有按 PROC_INST_ID_
--       驱动的查询不受影响，列值与 JOIN 推导值必须完全一致（V22）。
--
-- 写侧：ProcessServiceImpl.addComment 写入意见后，由 WfCommentTenantWriter
--       从 ACT_HI_PROCINST.TENANT_ID_ 反查回填本列
--       （开关 blade.workflow.comment-tenant-write，见 application.yml）。
--
-- ⚠️ 硬约束（§11.1.5）：新增列必须 NULL-able 且带 DEFAULT（''），
--    否则 MySQL 严格模式下引擎 INSERT 缺列直接失败 → 审批意见写入失败。
-- ⚠️ 必须显式 USE blade：jeelowcode 库同样存在 ACT_* 表。
-- =============================================================
USE blade;

-- 1) 加列（INSTANT，秒级；重复执行报 duplicate column 属预期，人工确认即可）
-- 注：MySQL 8 不允许 ALGORITHM=INSTANT 与 LOCK 子句同用（INSTANT 已隐含不加锁）；
--     若环境不支持 INSTANT 会报错，去掉该子句即可（数据量极小，常规 ALTER 同为秒级）。
ALTER TABLE ACT_HI_COMMENT
  ADD COLUMN TENANT_ID_ VARCHAR(64) NULL DEFAULT ''
  COMMENT '租户ID（D8 决策新增）：按租户横扫审批意见；写侧自 ACT_HI_PROCINST 反查回填',
  ALGORITHM=INSTANT;

-- 2) 索引：横扫场景 = 等值租户过滤 + 时间排序（equality first, then range/sort）
CREATE INDEX IDX_HI_COMMENT_TENANT ON ACT_HI_COMMENT(TENANT_ID_, TIME_);

-- 3) 存量回填（幂等：仅补 TENANT_ID_ 为空且能关联到实例的行）
--    先核对匹配行数，再 UPDATE（防漏回填）
SELECT COUNT(*) AS backfill_candidates
  FROM ACT_HI_COMMENT c
  JOIN ACT_HI_PROCINST p ON p.PROC_INST_ID_ = c.PROC_INST_ID_
 WHERE (c.TENANT_ID_ IS NULL OR c.TENANT_ID_ = '')
   AND p.TENANT_ID_ IS NOT NULL AND p.TENANT_ID_ <> '';

UPDATE ACT_HI_COMMENT c
  JOIN ACT_HI_PROCINST p ON p.PROC_INST_ID_ = c.PROC_INST_ID_
   SET c.TENANT_ID_ = p.TENANT_ID_
 WHERE (c.TENANT_ID_ IS NULL OR c.TENANT_ID_ = '')
   AND p.TENANT_ID_ IS NOT NULL AND p.TENANT_ID_ <> '';

-- 4) 校验：总数 / 已有租户 / 仍为空
--    仍为空的行 = 无法关联实例（PROC_INST_ID_ 为空的任务级意见）或实例本身无租户，
--    需人工核对，不阻塞。
SELECT COUNT(*) AS total,
       SUM(CASE WHEN TENANT_ID_ IS NOT NULL AND TENANT_ID_ <> '' THEN 1 ELSE 0 END) AS with_tenant,
       SUM(CASE WHEN TENANT_ID_ IS NULL OR TENANT_ID_ = '' THEN 1 ELSE 0 END) AS missing
  FROM ACT_HI_COMMENT;

-- 5) V22 一致性：列值与 JOIN 实例表推导值必须完全一致（mismatch 应为 0）
SELECT COUNT(*) AS tenant_mismatch
  FROM ACT_HI_COMMENT c
  JOIN ACT_HI_PROCINST p ON p.PROC_INST_ID_ = c.PROC_INST_ID_
 WHERE c.TENANT_ID_ IS NOT NULL AND c.TENANT_ID_ <> ''
   AND p.TENANT_ID_ IS NOT NULL AND p.TENANT_ID_ <> ''
   AND c.TENANT_ID_ <> p.TENANT_ID_;

-- 6) EXPLAIN：横扫场景索引命中验证（V1 方法论；预期 type=ref 命中 IDX_HI_COMMENT_TENANT）
EXPLAIN SELECT TYPE_, TIME_, MESSAGE_
  FROM ACT_HI_COMMENT
 WHERE TENANT_ID_ = '000000'
 ORDER BY TIME_ DESC;
