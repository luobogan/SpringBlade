-- =============================================================================
-- 迁移 016：清理「流程测试」在业务表留下的无主孤儿行
--
-- 背景（见 doc/md/流程测试与生产上线隔离方案.md §6.3 V13 / C17）
--   流程测试（/test/**）发起时会在业务表 formtable_main_N 建行，但：
--     ① 测试态不回填 request_id（WfInstanceServiceImpl.java:355-357）；
--     ② cleanupTestData 只删 4 张 wf_* 表，从不删业务行（WfTestServiceImpl.java:740-754）。
--   于是每次测试都在业务表留下一条「有业务行、无 request_id」的脏行；
--   被清理过的测试实例更是连反查线索都没有，成为永久孤儿行。
--
-- 删除判据（保守，三重条件同时满足才删）
--   ① request_id IS NULL            —— 无主：没有任何流程单据通过 request_id 绑定它
--   ② 不存在 wf_instance.data_id = 该行 id —— 没有任何流程实例（含草稿 status=5）引用它
--   ③ 已备份                          —— 本脚本先 CREATE TABLE ... AS SELECT 留退路
--
-- ⚠️ 风险与适用
--   - 业务表若存在「用户在 formmode 手工新建、尚未发起流程」的正常单据，也会命中判据 ①②。
--     故执行前必须人工过一遍待删清单（见下方「第 0 步 核对」）。
--   - 本次核对结果：10 行，创建人均为 2064998402、创建日期均为 2026-09-21（测试日），
--     业务字段为 1/2、334/444 之类的随手填值，确认为测试残留，同意删除。
--
-- 回滚：从备份表插回（见文末「回滚脚本」）
-- 幂等：重复执行时待删集为空，删除 0 行
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 第 0 步 核对（只读，先看清楚要删什么）
-- -----------------------------------------------------------------------------
SELECT b.id, b.request_id, b.modedatacreator, b.modedatacreatedate, b.modedatacreatetime
FROM blade.formtable_main_5 b
WHERE b.request_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM blade_workflow.wf_instance x WHERE x.data_id = b.id)
ORDER BY b.id;

-- 顺便确认：这些行也没有被「测试实例」直接关联（data_id 命中 is_test=1 实例）
SELECT COUNT(*) AS matched_test_inst
FROM blade.formtable_main_5 b
JOIN blade_workflow.wf_instance i ON i.data_id = b.id AND i.is_test = 1;

-- -----------------------------------------------------------------------------
-- 第 1 步 备份（幂等：备份表已存在则跳过插入；结构用 CTAS 一次建成）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blade.formtable_main_5__bak_20260921 AS
SELECT * FROM blade.formtable_main_5 WHERE 1 = 0;

INSERT INTO blade.formtable_main_5__bak_20260921
SELECT b.* FROM blade.formtable_main_5 b
WHERE b.request_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM blade_workflow.wf_instance x WHERE x.data_id = b.id)
  AND NOT EXISTS (SELECT 1 FROM blade.formtable_main_5__bak_20260921 k WHERE k.id = b.id);

-- -----------------------------------------------------------------------------
-- 第 2 步 删除
-- -----------------------------------------------------------------------------
DELETE b FROM blade.formtable_main_5 b
WHERE b.request_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM blade_workflow.wf_instance x WHERE x.data_id = b.id);

-- -----------------------------------------------------------------------------
-- 第 3 步 校验（期望：orphan_unbound = 0，matched_test_inst = 0）
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS orphan_unbound
FROM blade.formtable_main_5 b
WHERE b.request_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM blade_workflow.wf_instance x WHERE x.data_id = b.id);

SELECT COUNT(*) AS remain_total,
       SUM(request_id IS NULL) AS remain_unbound
FROM blade.formtable_main_5;

SELECT COUNT(*) AS backup_rows FROM blade.formtable_main_5__bak_20260921;

-- =============================================================================
-- 回滚脚本（如需恢复，取消注释执行；注意主键冲突时先清理同名 id）
--   DELETE t FROM blade.formtable_main_5 t
--     JOIN blade.formtable_main_5__bak_20260921 k ON k.id = t.id;
--   INSERT INTO blade.formtable_main_5 SELECT * FROM blade.formtable_main_5__bak_20260921;
-- =============================================================================
