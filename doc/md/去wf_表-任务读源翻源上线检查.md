# 去 wf_ 表 · 任务读源（待办/已办）翻源上线检查清单

> 适用：把 `blade.workflow.task-read-source` 由 `wf` 翻到 `act`（待办读 `ACT_RU_TASK`、已办读 `ACT_HI_TASKINST`）。
> 现状：读源代码已就绪且**默认 `wf`（零行为变化）**；`dev` 实测覆盖率仅约 30%，**不可直接翻**，必须先按本清单评估达标。
> 关联：`doc/md/去wf_表-读源切换开关说明.md`（读源开关总览）、`doc/sql/migration/_verify_task_1to1.sql`（校验脚本）。

---

## 0. 一句话结论

**代码就绪 ≠ 可以翻源**。能否翻取决于**数据对齐度**：存量会签 N:1 与孤儿任务在 ACT 侧都无法表达，翻了会丢单。务必先跑校验、达标再翻。

---

## 1. 前置依赖（缺一不可）

| # | 依赖 | 落地物 | 如何确认 |
|---|---|---|---|
| 1 | 任务业务列已加到 ACT | `act_add_task_biz_columns.sql`（每表 8 列：`BUSINESS_STATUS_`/`IS_TEST_`/`ORIGINAL_USER_`/`SIGN_ORDER_`/`VIEW_TIME_`/`TIMEOUT_HANDLED_`/`BIZ_TASK_ID_`/`BIZ_ASSIGNEE_`） | 脚本自带校验：两表 `col_cnt = 8` |
| 2 | 双写已开启 | `blade.workflow.task-act-write.enabled=true` | 接口日志 / 新增任务回查 ACT 有业务列 |
| 3 | 会签已下沉多实例（1:1） | `blade.workflow.engine-multi-instance.enabled=true` + **重新部署全部定义**（`POST /definition/admin/redeploy-mi`） | 校验项 ⑧ > 0；`POST /test/mi-scenario` 返回 `allPassed=true` |
| 4 | 存量业务列已回填 | `act_backfill_task_biz_columns.sql` | 校验项 ⑤ → 0 |
| 5 | 业务 ID / 办理人已回填 | `act_backfill_task_biz_id.sql` | 校验项「残留：1:1 但 BIZ_TASK_ID_ 仍为空」→ 0 |

> ⚠️ ③ 的关键认知：MI **只在部署期注入**，仅改开关不会改写已部署定义，**必须重新部署**（在途实例不受影响，仍绑旧版本）。

---

## 2. 翻源前置校验（在目标环境执行）

```bash
cmd /c D:\project\mysql-8.0.23-winx64\bin\mysql.exe --host=<host> --user=<u> --password=<p> --table \
  < doc/sql/migration/_verify_task_1to1.sql
```

### 达标阈值

| 项 | 含义 | **达标要求** | 不达标处理 |
|---|---|---|---|
| ① N:1 残留 | 同一引擎任务对应多条 `wf_task`（自研会签） | **= 0** | 等存量实例自然办结；新会签已是 1:1。**ACT 单行放不下多人，无法补偿** |
| ② 1:1 任务数 | 可直接翻源的任务 | ≥ 预期业务量 | 结合 ⑦ 一起看 |
| ④ 合成待办 | 无 `engineTaskId`（退回发起人重提交） | 记录数量 | **永远走 `wf_task`**，翻源不受影响（设计如此） |
| ⑤ 双写未覆盖（**1:1 口径**） | 1:1 但 ACT 业务列为空 | **= 0** | 跑 `act_backfill_task_biz_columns.sql` |
| ⑤b N:1 未回填（参考） | N:1 且业务列为空 | 不设阈值 | **按设计不可回填**（ACT 单行放不下多人），随存量 N:1 办结自然消失 |
| ⑥ 双写漂移 | ACT 子状态 ≠ `wf_task.status` 映射 | **= 0** | 排查双写；漂移期禁止翻 |
| ⑦ 孤儿 | `wf_task` 有引擎任务ID 但 ACT 查无此任务 | **= 0**，或经 ⑨ 确认**全为 `is_test=1` 测试数据** | 若含 `is_test=0` 的真实数据 → 引擎数据已丢失，**必须先修复，否则翻源丢单** |
| ⑧ MI 注入 | 已注入多实例的定义数 | **> 0** 且覆盖会签定义 | 重新部署 |
| ⑨ 孤儿分档 | 孤儿的 `is_test`/`status` 分布 | 全 `is_test=1` | 同上 |

### 额外的覆盖率硬指标（最关键）

```sql
-- 当前列表行（wf_task 口径）
SELECT COUNT(*) FROM wf_task WHERE is_test=0 AND assignee IS NOT NULL AND status IN (0,2,4,6,7,8,11);
-- ACT 可读行（翻源后口径）
SELECT COUNT(*) FROM ACT_HI_TASKINST WHERE COALESCE(IS_TEST_,0)=0 AND COALESCE(BIZ_ASSIGNEE_,ASSIGNEE_) IS NOT NULL AND END_TIME_ IS NOT NULL;
SELECT COUNT(*) FROM ACT_RU_TASK    WHERE COALESCE(IS_TEST_,0)=0 AND COALESCE(BIZ_ASSIGNEE_,ASSIGNEE_) IS NOT NULL;
```

**达标要求：ACT 可读行 ≥ wf_task 列表行的 99%**（dev 当前仅约 30%，故不可翻）。

---

## 3. 灰度步骤

1. **单用户灰度**：先在测试/预发，用 1~2 个真实办理人账号，置 `task-read-source=act`，人工比对「待办/已办列表」与 `wf_task` 口径逐条一致（条数、标题、节点名、状态、时间）。
2. **只读灰度**：确认列表正常后，重点验证**操作链路**：随便点一条待办执行「同意 / 转办 / 退回 / 查看」——必须成功。
   > 这是 `BIZ_TASK_ID_` 的意义：列表吐的 id 必须是 `wf_task.id`，否则 `requireTodoTask` 查不到任务。
3. **小流量**：按租户/角色放量，持续观察 1~2 个业务日。
4. **全量**：置 `blade.workflow.task-read-source=act`。

每步之间复跑校验脚本，确认 ⑤/⑥ 保持 0。

---

## 4. 回退步骤（随时可回，秒级）

| 场景 | 动作 |
|---|---|
| 列表异常 / 操作失败 | 把 `task-read-source` 改回 `wf` 并重启 → **立即回到 `wf_task` 读源**（代码内置失败自动降级，双保险） |
| 双写异常 | 把 `task-act-write.enabled` 改回 `false` 并重启 → 停止写 ACT（已写数据保留，不影响 `wf_task` 权威） |
| 会签异常 | 把 `engine-multi-instance.enabled` 改回 `false` 并重启 → 回到自研会签（在途实例不受影响） |

> 所有开关均为 `@Value`，**改完必须重启**才生效。回退不影响已落库数据，`wf_task` 全程保持可读写。

---

## 4.5 关键前提：引擎任务生命周期必须 == blade 任务状态（否则会「多算」）

实测（2026-09-30，dev）：读源=act 时**待办多算了 7 条**（ACT 24 + 兜底 1 = 25，而 `wf_task` 基线 18）。

**根因**：blade 自研会签会**脱离引擎**改变任务状态——典型是 `closeSiblings`（或签办结同节点其余待办）只把
`wf_task.status` 置 `FINISHED`，**并不完成/推进引擎任务**。于是：

- 引擎侧：任务仍活跃 → `ACT_RU_TASK` 仍有该行 → 按 ACT 数被算成「待办」；
- blade 侧：该任务已办结 → 不计入待办。

即 **`ACT_RU_TASK` ≠ blade 待办**。兜底合并只能补「少算」（ACT 表达不了的 N:1/孤儿），
**补不了「多算」**；要消除多算只能用 `wf_task` 交叉过滤，那等于没翻源。

**结论**：翻源成立的充要前提是——**会签/或签/依次 100% 由引擎多实例驱动**
（`engine-multi-instance.enabled=true` + 全部定义重新部署），使引擎任务与 blade 任务 **1:1 且生命周期一致**；
并且**存量 N:1 已清零**。二者缺一，读源=act 就会出现偏差（实测待办多算 7 条）。

> 已办口径受影响较小：以 `END_TIME_` 为准（实测 28 = 基线 28）。

---

## 5. 已知限制（翻源后仍存在，非缺陷）

- **合成待办**（无 `engineTaskId`，如「退回发起人重新提交」）永远走 `wf_task`，不进 ACT。
- **`wf_task` 暂不退役**：只有等读源稳定 + `reject-nodes`/`monitor`/`count`/`form/render` 等接口全部翻源后，才进入 P6 退役。
- **会签必须保持引擎多实例开启**：一旦关掉，新建会签又变 N:1，读源会重新丢单。

---

## 6. dev 环境实测基线（2026-09-30，供对照）

| 项 | 值 |
|---|---|
| ① N:1 残留 | 137 |
| ② 1:1 | 56 |
| ④ 合成待办 | 31 |
| ⑤ 双写未覆盖（1:1 口径） | **0**（达标） |
| ⑤b N:1 未回填（参考） | 108（按设计不可回填） |
| ⑥ 漂移 | 0 |
| ⑦ 孤儿 | 344（⑨ 显示**全为 `is_test=1`**） |
| ⑧ MI 注入 / 总数 | 1 / 65 |
| 覆盖率 | **42 / 139 ≈ 30%** → **不可翻** |

dev 不可翻的原因：存量 N:1（137）+ 孤儿（344，全测试态）导致数据不对齐；属环境问题，非读源实现问题。
