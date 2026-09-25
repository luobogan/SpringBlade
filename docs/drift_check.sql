-- ============================================================================
-- drift_check.sql
-- 工作流「业务台账 wf_*」 vs 「引擎 Flowable ACT_*」待办漂移对账 + 历史清理清单
--
-- 前提：
--   1) wf_task.status : 0待办 2已办 4办结 6自动提交 7协办 8抄送 11传阅
--   2) wf_instance.status : 0运行中 1通过 2不通过 3撤销 4暂停 5草稿
--   3) wf_task.engine_task_id == ACT_RU_TASK.ID_ == ACT_HI_TASKINST.ID_
--      wf_instance.engine_inst_id == ACT_RU_EXECUTION.PROC_INST_ID_（根执行 PARENT_ID_ IS NULL）
--   4) 协办(7)/抄送(8)/传阅(11) 复用同一 engine_task_id → 行数不可直接比，按 ID 关联
--   5) 测试态(is_test=1) 不参与对账（其清理走「测试数据一键清理」独立路径）
--   6) 在 wf_* 与 ACT_* 同库的 MySQL 客户端执行
--
-- 使用顺序：先跑本文件确认漂移范围 → 跑一次性修复脚本(HistoricalDriftFixer) → 再跑本文件复核应为 0
-- ============================================================================


-- ============ 一、数量概览（仅参考，别直接比行数） ============
SELECT
  (SELECT COUNT(*) FROM wf_task WHERE status = 0 AND is_test = 0)                                   AS wf_todo_rows,
  (SELECT COUNT(DISTINCT engine_task_id) FROM wf_task
      WHERE status = 0 AND is_test = 0 AND engine_task_id IS NOT NULL)                               AS wf_todo_distinct_engine,
  (SELECT COUNT(*) FROM ACT_RU_TASK)                                                                AS engine_ru_task,
  (SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE END_TIME_ IS NULL)                                     AS engine_hi_open,
  (SELECT COUNT(*) FROM wf_instance WHERE status IN (1,2,3,4) AND is_test = 0)                      AS wf_terminal_or_suspended;


-- ============ 二、A 类漂移：引擎有任务，业务缺记录（孤儿任务 → 用户看不到待办） ============
SELECT rt.ID_        AS engine_task_id,
       rt.NAME_      AS task_name,
       rt.ASSIGNEE_  AS engine_assignee,
       rt.PROC_INST_ID_,
       rt.TENANT_ID_,
       rt.CREATE_TIME_
FROM ACT_RU_TASK rt
WHERE rt.ID_ NOT IN (SELECT engine_task_id FROM wf_task WHERE engine_task_id IS NOT NULL)
ORDER BY rt.CREATE_TIME_ DESC;


-- ============ 三、B 类漂移：业务显示待办，引擎已无该任务（悬挂/陈旧待办 → 永远待办） ============
SELECT t.id, t.inst_id, t.engine_task_id, t.assignee, t.node_key, t.status, t.is_test, t.create_time
FROM wf_task t
WHERE t.status = 0
  AND t.is_test = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND t.engine_task_id NOT IN (SELECT ID_ FROM ACT_RU_TASK)
ORDER BY t.create_time DESC;


-- ============ 四、漂移按租户/日期分布，定位「全局双写 bug」还是「个别流程」 ============
SELECT t.tenant_id,
       DATE(t.create_time) AS dt,
       COUNT(*)            AS dangling_todo
FROM wf_task t
WHERE t.status = 0 AND t.is_test = 0
  AND t.engine_task_id IS NOT NULL AND t.engine_task_id <> ''
  AND t.engine_task_id NOT IN (SELECT ID_ FROM ACT_RU_TASK)
GROUP BY t.tenant_id, DATE(t.create_time)
ORDER BY dangling_todo DESC;


-- ============ 五、待办去重计数对比（应与 engine_ru_task 接近，差应能被二/三解释） ============
-- 注意：ACT_RU_EXECUTION 无 END_TIME_ 列（运行态结束即删行），"实例运行中"= 该行存在(PARENT_ID_ IS NULL)
SELECT
  (SELECT COUNT(DISTINCT engine_task_id) FROM wf_task
      WHERE status = 0 AND is_test = 0 AND engine_task_id IS NOT NULL) AS wf_todo_distinct_engine,
  (SELECT COUNT(*) FROM ACT_RU_TASK)                                   AS engine_ru_task,
  (SELECT COUNT(*) FROM ACT_RU_EXECUTION
      WHERE PARENT_ID_ IS NULL)                                         AS engine_active_instances;


-- ============ 六、反向漂移（P0 历史漂移：业务已终态/暂停，引擎仍运行）→ 需跑修复脚本 ============
-- 判定依据：流程运行中 ⇔ ACT_RU_EXECUTION 存在根执行(PARENT_ID_ IS NULL)；结束即删行，无需 END_TIME_ 过滤
-- 6a：业务已终态(通过1/不通过2/撤销3) 但引擎实例仍在运行 → 需 deleteProcessInstance
SELECT wi.id AS inst_id, wi.engine_inst_id, wi.status, wi.title, wi.end_time
FROM wf_instance wi
WHERE wi.status IN (1, 2, 3)
  AND wi.is_test = 0
  AND wi.engine_inst_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM ACT_RU_EXECUTION e
             WHERE e.PROC_INST_ID_ = wi.engine_inst_id
               AND e.PARENT_ID_ IS NULL);

-- 6b：业务已暂停(4) 但引擎实例仍「活跃未挂起」(SUSPENSION_STATE_=1) → 需 suspendProcessInstance
SELECT wi.id AS inst_id, wi.engine_inst_id, wi.status
FROM wf_instance wi
WHERE wi.status = 4
  AND wi.is_test = 0
  AND wi.engine_inst_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM ACT_RU_EXECUTION e
             WHERE e.PROC_INST_ID_ = wi.engine_inst_id
               AND e.PARENT_ID_ IS NULL
               AND e.SUSPENSION_STATE_ = 1);

-- 6c：业务运行中(0) 但引擎实例已被挂起(SUSPENSION_STATE_=2) → 需 activateProcessInstance
--     （修复前 stop/resume 不碰引擎，理论上历史不会出现；此处作安全兜底）
SELECT wi.id AS inst_id, wi.engine_inst_id, wi.status
FROM wf_instance wi
WHERE wi.status = 0
  AND wi.is_test = 0
  AND wi.engine_inst_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM ACT_RU_EXECUTION e
             WHERE e.PROC_INST_ID_ = wi.engine_inst_id
               AND e.PARENT_ID_ IS NULL
               AND e.SUSPENSION_STATE_ = 2);
