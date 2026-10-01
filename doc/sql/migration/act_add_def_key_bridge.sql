-- =============================================================
-- R1 / D6 / M1：业务定义 ↔ 引擎定义 1:N 的桥接列 DEF_KEY_
-- 出处：《Flowable8承接台账模块-去wf_表改造分析.md》§15.1 M1、§17.1 D6、§17.3 R1
--
-- 背景：wf_process_definition（业务 defId）对应 ACT_RE_PROCDEF 的多个版本
--       （每次部署生成新 ID_，KEY_ 不变）。§11 只加了 DEF_ID_，
--       【无法从引擎 PROC_DEF_ID_ 反查业务 defId / 版本组】。
-- 方案：在 ACT_HI_PROCINST 冗余引擎 KEY_（= ACT_RE_PROCDEF.KEY_），
--       配合既有桥接表 flow_def_bridge（procKey → defId）即可双向反查。
--
-- ⚠️ 硬约束（§11.1.5）：新增列必须 NULL-able 且带 DEFAULT，
--    否则 MySQL 严格模式下引擎 INSERT 会因缺列失败，直接导致流程发起失败。
-- ⚠️ 必须显式 USE blade：jeelowcode 库同样存在 ACT_* 表。
-- =============================================================
USE blade;

-- 1) 加列（INSTANT，秒级；重复执行报 duplicate column 属预期，人工确认即可）
-- 注：MySQL 8 不允许 ALGORITHM=INSTANT 与 LOCK 子句同用（INSTANT 已隐含不加锁）；
--     若环境不支持 INSTANT 会报错，去掉该子句即可（数据量极小，常规 ALTER 同为秒级）。
ALTER TABLE ACT_HI_PROCINST
  ADD COLUMN DEF_KEY_ VARCHAR(255) NULL DEFAULT NULL
  COMMENT '引擎流程定义KEY（ACT_RE_PROCDEF.KEY_）；桥接业务defId与引擎版本组',
  ALGORITHM=INSTANT;

-- 2) 索引：多域共用按 TENANT_ID_ 复合（§13.3 修订口径）
CREATE INDEX IDX_HI_PROC_T_DEFKEY ON ACT_HI_PROCINST(TENANT_ID_, DEF_KEY_);

-- 3) 存量回填（幂等：仅补 DEF_KEY_ 为空且有 PROC_DEF_ID_ 的行）
--    先核对匹配行数，再 UPDATE（防漏回填）
SELECT COUNT(*) AS backfill_candidates
  FROM ACT_HI_PROCINST h
  JOIN ACT_RE_PROCDEF p ON p.ID_ = h.PROC_DEF_ID_
 WHERE h.DEF_KEY_ IS NULL AND h.PROC_DEF_ID_ IS NOT NULL;

UPDATE ACT_HI_PROCINST h
  JOIN ACT_RE_PROCDEF p ON p.ID_ = h.PROC_DEF_ID_
   SET h.DEF_KEY_ = p.KEY_
 WHERE h.DEF_KEY_ IS NULL AND h.PROC_DEF_ID_ IS NOT NULL;

-- 4) 校验：总数 / 已回填 / 仍缺失（缺失应为"PROC_DEF_ID_ 为空的存量孤儿行"）
SELECT COUNT(*) AS total,
       SUM(CASE WHEN DEF_KEY_ IS NOT NULL THEN 1 ELSE 0 END) AS with_key,
       SUM(CASE WHEN DEF_KEY_ IS NULL THEN 1 ELSE 0 END) AS missing
  FROM ACT_HI_PROCINST;

SELECT COUNT(*) AS missing_but_has_procdef
  FROM ACT_HI_PROCINST WHERE DEF_KEY_ IS NULL AND PROC_DEF_ID_ IS NOT NULL;
