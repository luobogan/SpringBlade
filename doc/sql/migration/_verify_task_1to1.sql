-- =============================================================================
-- 任务源翻源前置校验：会签/或签 1:1 残留 + 双写漂移
-- 作用：量化「同一引擎任务对应 N 条 wf_task」的残留量，并核对 ACT_* 业务列双写是否与
--       wf_task 一致。这是打开「待办/已办读源=act」开关前的验收依据。
--
-- ⚠️ 前提：
--   1. 本脚本 USE blade；只读，不做任何写操作。
--   2. N:1 残留 = 自研会签（multiInstanceEnabled=false）的产物；只有会签下沉引擎多实例
--      （1:1）后，该数才应趋近 0。
--   3. 双写漂移检查仅在「已开启 blade.workflow.task-act-write.enabled 双写」后才有意义；
--      双写未开启时 BUSINESS_STATUS_ 全为 NULL 属预期，不代表故障。
--
-- =============================================================================
-- 【2026-11-08 口径修订】①~⑨ 全部补 is_deleted = 0 过滤（生产态口径）。
--   修订原因：原脚本的 ①/⑦ 缺逻辑删除条件，把已逻辑删除的历史数据算成「N:1 残留」与
--   「孤儿」—— 实测 ①=1 / ⑦=225，而按生产态复算两者均为 0，会误导验收
--   （与脚本 ⑤ 已注明的同类问题一致：若不限定，⑤ 会把 N:1 一并计入而永远到不了 0）。
--   修订方式：主项一律用生产态口径；每项另附一条「全量口径」参考项，不丢历史信息。
--   另新增 ④c（合成待办按 草稿/非草稿 分档）与 ⑥c（漂移形态分布），便于快速定位。
-- =============================================================================

USE blade;

-- ① N:1 残留总量（生产态：>0 即仍有会签/或签共享引擎任务，翻源后这部分在 ACT 侧无表达）
SELECT '① N:1 残留（同一 engineTaskId 多条 wf_task，生产态；达标应=0）' AS item, COUNT(*) AS val
FROM (
    SELECT engine_task_id FROM wf_task
    WHERE is_deleted = 0
      AND engine_task_id IS NOT NULL AND engine_task_id <> ''
    GROUP BY engine_task_id HAVING COUNT(*) > 1
) n1;

-- ①b 参考项：全量口径（含 is_deleted=1 的历史数据）—— 修订前脚本口径，仅供对照
SELECT '①b N:1 残留（全量口径，含 is_deleted=1，仅参考）' AS item, COUNT(*) AS val
FROM (
    SELECT engine_task_id FROM wf_task
    WHERE engine_task_id IS NOT NULL AND engine_task_id <> ''
    GROUP BY engine_task_id HAVING COUNT(*) > 1
) n1b;

-- ② 1:1 任务总量（生产态；这部分可直接翻源）
SELECT '② 1:1 任务数（可直接翻源，生产态）' AS item, COUNT(*) AS val
FROM (
    SELECT engine_task_id FROM wf_task
    WHERE is_deleted = 0
      AND engine_task_id IS NOT NULL AND engine_task_id <> ''
    GROUP BY engine_task_id HAVING COUNT(*) = 1
) o2o;

-- ③ N:1 明细抽样（前 20：引擎任务ID / 条数 / 办理人；生产态）
SELECT '③ N:1 明细抽样（生产态）' AS item;
SELECT engine_task_id, COUNT(*) AS cnt, GROUP_CONCAT(assignee) AS assignees
FROM wf_task
WHERE is_deleted = 0
  AND engine_task_id IS NOT NULL AND engine_task_id <> ''
GROUP BY engine_task_id
HAVING COUNT(*) > 1
LIMIT 20;

-- ④ 无引擎任务的任务数（合成待办，如「退回发起人」重提交；翻源时须走 wf_task，不进 ACT）
--    生产态口径。草稿（所属实例 status=5）的合成待办已于「草稿移出待办 · 方案 A」中
--    移出「待办」列表；其余合成待办（运行中实例）必须留在待办列表，不得误伤。
SELECT '④ 无 engineTaskId 的合成待办（生产态）' AS item, COUNT(*) AS val
FROM wf_task
WHERE is_deleted = 0
  AND (engine_task_id IS NULL OR engine_task_id = '');

-- ④c 合成待办分档：按「所属实例状态」区分草稿(5) 与非草稿 —— 二者处置路径不同
SELECT '④c 合成待办分档（实例状态 / 条数，5=草稿已移出待办）' AS item;
SELECT CASE WHEN i.id IS NULL THEN 'NULL(无实例)' ELSE CAST(i.status AS CHAR) END AS inst_status,
       COUNT(*) AS cnt
FROM wf_task t
LEFT JOIN wf_instance i ON i.id = t.inst_id
WHERE t.is_deleted = 0
  AND (t.engine_task_id IS NULL OR t.engine_task_id = '')
GROUP BY inst_status
ORDER BY cnt DESC;

-- ⑤ 双写覆盖（达标项）：【必须限定 1:1 口径】—— N:1 按设计无法回填（ACT 单行放不下多人），
--    若不限定，⑤ 会把 N:1 一并计入而永远到不了 0，误导验收。
SELECT '⑤ 双写未覆盖-1:1口径（达标应=0，生产态）' AS item, COUNT(*) AS val
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE t.is_deleted = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NULL
  AND t.engine_task_id IN (
      SELECT engine_task_id FROM wf_task
      WHERE is_deleted = 0
        AND engine_task_id IS NOT NULL AND engine_task_id <> ''
      GROUP BY engine_task_id HAVING COUNT(*) = 1
  );

-- ⑤b 参考项：N:1 且业务列为空（按设计不可回填，需待存量 N:1 自然办结；不计入达标）
SELECT '⑤b N:1未回填（参考，按设计不可回填，生产态）' AS item, COUNT(*) AS val
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE t.is_deleted = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NULL
  AND t.engine_task_id IN (
      SELECT engine_task_id FROM wf_task
      WHERE is_deleted = 0
        AND engine_task_id IS NOT NULL AND engine_task_id <> ''
      GROUP BY engine_task_id HAVING COUNT(*) > 1
  );

-- ⑥ 双写漂移：业务列状态与 wf_task.status 不一致（映射 0/2/4/6/7/8/11）
--    ⚠️ 达标要求 = 0。已知成因（2026-11-08 定位）：WfStateProjector#onTaskCompleted /
--    onTaskDeleted 反写 wf_task 后未调 taskActWriter.sync() 回写 ACT，
--    形成「wf_status=4(办结) vs ACT=TODO」的单一形态漂移（MI 或签删除兄弟任务场景）。
SELECT '⑥ 双写漂移（ACT 子状态 ≠ wf_task.status 映射，生产态；达标应=0）' AS item, COUNT(*) AS val
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE t.is_deleted = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NOT NULL
  AND h.BUSINESS_STATUS_ <> CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE '?' END;

-- ⑥b 参考项：全量口径（含 is_deleted=1）—— 修订前脚本口径，仅供对照
SELECT '⑥b 双写漂移（全量口径，含 is_deleted=1，仅参考）' AS item, COUNT(*) AS val
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NOT NULL
  AND h.BUSINESS_STATUS_ <> CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE '?' END;

-- ⑥c 漂移形态分布：一眼看出是单一形态还是多样（单一形态通常指向单点成因）
SELECT '⑥c 漂移形态分布（wf_status vs ACT 子状态 / 条数）' AS item;
SELECT t.status AS wf_status, h.BUSINESS_STATUS_ AS act_status, COUNT(*) AS cnt
FROM wf_task t
JOIN ACT_HI_TASKINST h ON h.ID_ = t.engine_task_id
WHERE t.is_deleted = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND h.BUSINESS_STATUS_ IS NOT NULL
  AND h.BUSINESS_STATUS_ <> CASE t.status
        WHEN 0 THEN 'TODO' WHEN 2 THEN 'DONE' WHEN 4 THEN 'FINISHED'
        WHEN 6 THEN 'AUTO_SUBMIT' WHEN 7 THEN 'COADJUTANT'
        WHEN 8 THEN 'CIRCULATE' WHEN 11 THEN 'READ' ELSE '?' END
GROUP BY t.status, h.BUSINESS_STATUS_
ORDER BY cnt DESC;

-- ⑦ 孤儿：wf_task 有 engineTaskId 但引擎侧 ACT 两表都查无此任务（数据不一致，需人工核对）
SELECT '⑦ 孤儿（wf_task 有引擎任务ID 但 ACT 无对应行，生产态；达标应=0）' AS item, COUNT(*) AS val
FROM wf_task t
WHERE t.is_deleted = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND NOT EXISTS (SELECT 1 FROM ACT_RU_TASK r WHERE r.ID_ = t.engine_task_id)
  AND NOT EXISTS (SELECT 1 FROM ACT_HI_TASKINST h WHERE h.ID_ = t.engine_task_id);

-- ⑦b 参考项：全量口径（含 is_deleted=1 的历史数据）—— 修订前脚本口径，仅供对照
SELECT '⑦b 孤儿（全量口径，含 is_deleted=1，仅参考）' AS item, COUNT(*) AS val
FROM wf_task t
WHERE t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND NOT EXISTS (SELECT 1 FROM ACT_RU_TASK r WHERE r.ID_ = t.engine_task_id)
  AND NOT EXISTS (SELECT 1 FROM ACT_HI_TASKINST h WHERE h.ID_ = t.engine_task_id);

-- ⑧ 多实例注入率：开关 blade.workflow.engine-multi-instance.enabled=true 并【重新部署】后应 > 0。
--    ⚠️ MI 在部署期注入（applyMultiInstanceIfEnabled），仅改开关不会改写已部署定义，必须重新部署。
SELECT '⑧ 已注入多实例的流程定义数' AS item, COUNT(DISTINCT pd.ID_) AS val
FROM ACT_RE_PROCDEF pd
JOIN ACT_GE_BYTEARRAY ba ON ba.DEPLOYMENT_ID_ = pd.DEPLOYMENT_ID_
WHERE CONVERT(ba.BYTES_ USING utf8mb4) LIKE '%multiInstanceLoopCharacteristics%';

SELECT '⑧b 流程定义总数（对照）' AS item, COUNT(*) AS val FROM ACT_RE_PROCDEF;

-- ⑨ 孤儿分档：按 测试态(is_test) / 任务状态(status) 统计，判断严重性。
--    status：0待办 2已办 4办结 6自动提交 7协办 8抄送 11传阅。
--    若为 is_test=1 的测试数据 → 可整体清理；若含 is_test=0 的待办(0) → 翻源会真实丢单，必须先修复。
SELECT '⑨ 孤儿分档（is_test / status / 条数，生产态）' AS item;
SELECT t.is_test, t.status, COUNT(*) AS cnt
FROM wf_task t
WHERE t.is_deleted = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND NOT EXISTS (SELECT 1 FROM ACT_RU_TASK r WHERE r.ID_ = t.engine_task_id)
  AND NOT EXISTS (SELECT 1 FROM ACT_HI_TASKINST h WHERE h.ID_ = t.engine_task_id)
GROUP BY t.is_test, t.status
ORDER BY t.is_test, t.status;

-- ⑨b 参考项：全量口径（含 is_deleted=1）—— 修订前脚本口径，仅供对照
SELECT '⑨b 孤儿分档（全量口径，含 is_deleted=1，仅参考）' AS item;
SELECT t.is_test, t.status, COUNT(*) AS cnt
FROM wf_task t
WHERE t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND NOT EXISTS (SELECT 1 FROM ACT_RU_TASK r WHERE r.ID_ = t.engine_task_id)
  AND NOT EXISTS (SELECT 1 FROM ACT_HI_TASKINST h WHERE h.ID_ = t.engine_task_id)
GROUP BY t.is_test, t.status
ORDER BY t.is_test, t.status;
