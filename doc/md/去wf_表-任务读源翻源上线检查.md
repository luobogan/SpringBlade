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
  → 已落地**第一个可回退台阶**：读侧残留合并开关 `blade.workflow.task-residue-merge.enabled`（见 §7），关闭后列表与角标 100% 来自 ACT_*。
- **会签必须保持引擎多实例开启**：一旦关掉，新建会签又变 N:1，读源会重新丢单。

---

## 6. dev 环境实测基线（供对照）

### 6.1 历史基线（2026-09-30，翻源前）

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

### 6.2 当前基线（2026-11-08 实测，dev 主库 `blade`）

> 复算脚本：`log/wf_residue_dist.py`（只读）、`log/wf_residue_owner.py`（只读）；
> 输出存档 `log/wf_residue_dist.out.txt`。库连接：本机 MySQL `127.0.0.1:3306`，库 `blade`。

| 项 | 值 | 判定 |
|---|---|---|
| ① 活跃 N:1 残留 | **0** | ✅ 达标 |
| ② 孤儿（wf_task 有 engineTaskId、ACT 查无） | **0** | ✅ 达标 |
| ④ 合成待办（无 engineTaskId） | **1 条，生产态 `is_test=0`** | ❌ **唯一阻塞项**（见 §6.3） |
| 待办侧残留（`status=0` 未被 `ACT_RU_TASK` 覆盖） | **1 条，生产态** | ❌ = 那条草稿 |
| 已办侧残留（`status IN (2,4,6,7,8,11)` 未被 `ACT_HI_TASKINST` 覆盖） | **0** | ✅ 达标 |
| ⑤ `ACT_RU_TASK` 业务列回填 | 37 / 37 行 `BIZ_TASK_ID_` 非空 | ✅ 达标 |
| ⑤ `ACT_HI_TASKINST` 业务列回填 | 286 / 326 行非空（空 40 行全为已办、且 `wf_task` 侧无对应行 → 非回归） | ✅ 可接受 |
| 覆盖率硬指标 | `wf_task` 列表行 **240** vs ACT 可读行 **243** = **101.2%** | ✅ 达标（要求 ≥99%） |

### 6.2.1 官方校验脚本 `_verify_task_1to1.sql` 实测（2026-11-08）

| 项 | 脚本口径 | 未删除/生产态口径 | 判定 |
|---|---|---|---|
| ① N:1 残留 | 1 | **0**（2 条均 `is_deleted=1`） | ✅ |
| ② 1:1 任务数 | 504 | — | — |
| ④ 合成待办 | 2 | 1（草稿） | ✅ 方案 A 已处理 |
| ⑤ 双写未覆盖（1:1） | 0 | **0** | ✅ 达标 |
| ⑤b N:1 未回填 | 0 | 0 | ✅ |
| **⑥ 双写漂移** | **72** | **57**（`is_deleted=0`/`is_test=0`） | ❌ **不达标（要求 = 0）** |
| ⑦ 孤儿 | 225 | **0**（全部 `is_deleted=1`，2026-10-11~11-04 已清理） | ✅ |
| ⑧ MI 注入 / 定义总数 | 62 / 150 | — | ✅ |

> ⚠️ **脚本口径缺陷（建议修）**：① 与 ⑦ 缺 `is_deleted=0` 条件，把已逻辑删除的历史数据算成「N:1 残留」与「孤儿」，
> **会误导验收**（脚本自己在 ⑤ 已注明同类问题：「若不限定，⑤ 会把 N:1 一并计入而永远到不了 0，误导验收」）。
> 已用 `is_deleted`/`is_test` 维度复算确认生产态口径为 0。

### 6.2.2 ⑥ 漂移 57 条：根因已定位（真实缺陷）

**形态单一**：`wf_task.status=4（办结）` vs `ACT.BUSINESS_STATUS_='TODO'`（57 条全部同形态，无其他组合）。

**根因**：`WfStateProjector#onTaskDeleted`（引擎任务删除事件 → 置 wf_task 办结 4）
**只 `taskMapper.updateById(patch)` 更新了 `wf_task`，从未调 `taskActWriter.sync()` 回写 ACT**
（全类搜 `taskActWriter|sync(` = 0 命中）。

**触发场景**：MI（会签/或签）下沉引擎多实例后，**或签节点首个办理人同意 → 引擎按完成条件删除其余 MI 子任务**
→ 引擎派发 `ENTITY_DELETED` → `onTaskDeleted` 置办结 → ACT 的 `BUSINESS_STATUS_` 停在 `TODO`。
数据吻合：57 条集中在 `Task_DeptLeader`/`Task_DeptDirector`/`Task_ViceLeader`/`Task_DeptReview`/
`Task_LeaderApprove`/`Task_Draft`，时间 2026-11-02~11-07（MI 下沉后新建的实例），实例状态 0（运行中）/1（通过）均有。

**对比为何 `status=2`（已办）不漂移**：业务路径（`doApprove`）是「业务先写 wf_task → `taskActWriter.sync()` → 再 completeTask」，
ACT 已同步；引擎事件到达时 `onTaskCompleted` 幂等跳过。**只有「引擎事件驱动」的路径漏了 ACT 回写。**

**影响面**（数量不受影响，标签与筛选受影响）：

| 场景 | 影响 |
|---|---|
| 待办列表（读 `ACT_RU_TASK`） | ✅ 不受影响（这些任务已被引擎删除，不在 RU 表） |
| 已办列表（读 `ACT_HI_TASKINST`，`END_TIME_` 非空） | ⚠️ 57 条**仍在列表里**（数量对），但 `doneFromAct` 用 `taskStatus('TODO')=0` → 状态标签显示「待办」而非「办结」 |
| 已办列表的「状态」筛选 | ⚠️ `appendStatusFilter` 按 `BUSINESS_STATUS_` 筛 → 前端按「办结」筛**漏掉这 57 条** |
| 角标 | ✅ 不受影响（已办计数按 `END_TIME_`） |

**修复方向**：给 `WfStateProjector` 注入 `WfTaskActWriter`，在 `onTaskCompleted` / `onTaskDeleted`
反写 `wf_task` 后调 `sync(task)` 回写 ACT 业务列。
`sync()` 是 UPDATE（不是回查 `ACT_RU_*`），不违反该类「绝不回查 ACT_RU_*」的约束；
N:1 守卫对 MI 1:1 场景不误伤。

**✅ 已修复（2026-11-08）**：

| # | 位置 | 改动 |
|---|---|---|
| 1 | `WfStateProjector#onTaskCompleted` | 置已办(2) 后 `taskActWriter.sync(t)` 回写 ACT |
| 2 | `WfStateProjector#onTaskDeleted` | 置办结(4) 后 `taskActWriter.sync(t)` 回写 ACT（漂移主体：MI 或签删除兄弟任务） |
| 3 | `WfStateProjector#closeResidualTodos` | 终态批量关残留待办：先查出行 → 批量 UPDATE → **逐行 sync**（批量 UPDATE 无法带业务列，此前是第三个漏回写的点） |

回归：`WfStateProjectorActSyncTest`（H2 + Mockito，2 用例：置已办→`DONE`、置办结→`FINISHED`）。

**存量修正**：`doc/sql/migration/V2026.11.08_002__fix_task_act_status_drift.sql`
（幂等，只改 `ACT_HI_TASKINST.BUSINESS_STATUS_` 一列；含前后置校验）。
已在 dev 库**事务内验证**：`UPDATE` 影响 **68 行**（与漂移数一致，不多不少），`ROLLBACK` 后漂移仍 = 68
→ **验证过程未改动数据**，正式执行需另行确认（dev 为共享库）。



### 6.3 唯一阻塞项：草稿合成待办（`status=5` 草稿实例）

```
taskId=2118968283811053570  inst=2118968283743944705  status=0  is_test=0
node_key=Task_Create  engine_task_id=''  assignee=10（王海燕）
实例：status=5(DRAFT)  engine_inst_id=None  title=综合费用报销全流程审批流  create_time=2026-11-07 15:26
```

- **`wf_instance.status=5` = `STATUS_DRAFT`**（`ActHiProcinst.STATUS_DRAFT = 5`；`WfInstanceServiceImpl:726` 落库）；
  `engine_inst_id=None` → **引擎从未启动**，草稿态没有引擎任务。
- 该行的作用（`WfInstanceServiceImpl:732-734` 原注释）：
  > "④ 草稿合成任务（区别于引擎任务）：**保证草稿出现在发起人待办**。"

**影响面精确到两条路径**：

| 路径 | 关闭开关后 | 原因 |
|---|---|---|
| 「我的请求」列表的草稿 | ✅ **不受影响** | `WfInstanceServiceImpl:861-869` 对 `STATUS_DRAFT` 单独回退查 `wf_instance`，不依赖 `wf_task` 待办行 |
| 「待办」列表的草稿条目 | ❌ **会消失** | 草稿进入待办的唯一载体就是这条 `wf_task` 合成待办 |

→ **草稿天然只能存 `wf_task`**：要让草稿进 ACT 就必须启动引擎实例，与「草稿不启动引擎」的设计直接冲突。

### 6.4 Nacos 配置分布（改动前必读）

`nacos.config_info` 中 `blade.yaml` / `blade-dev.yaml` / `blade-test.yaml` / `blade-prod.yaml`
**全部指向同一个物理库 `blade`**。改任一环境的配置都可能影响其它环境 —— 动配置前须确认 namespace 归属，
并优先改**代码默认值 + 单环境 dataId**，避免全环境广播（参见 `doc/nacos/blade.yaml` 的历史事故记录）。


---

## 7. 读侧残留合并开关（P6 退役的第一台阶 · 2026-11-08 落地）

> 落地物：`config/WfTaskResidueProperties`（配置键 `blade.workflow.task-residue-merge.enabled`，默认 `true`）
> 回归：`WfTaskResidueMergeTest`（H2，5 用例：默认合并 / 关闭纯 ACT / 交集不重复计数 / 已办口径 / 默认值兜底）

### 7.1 为什么先做这个开关

`wf_task` 的退役（P6）无法一步到位——**写侧**还卡在三处硬阻塞：

1. **合成待办永不进 ACT**：`WfTaskActWriter#sync` 对 `engineTaskId == null` 直接跳过（草稿待办、退回发起人重提交），
   删表即死循环（发起人被退回却看不到待办）；
2. **blade 6 态无 ACT 对应**：待办/已办/撤销/办结/协办/传阅/已读，ACT 只有 `END_TIME_` 能区分办/未办；
3. **读点远未翻完**：reject-nodes / monitor / count / form-render / 草稿 / 我的请求 / 超时扫描 / 子流程 /
   自由流程 / **自动测试**（`WfTestServiceImpl` 全靠 `wf_task`）。

但**读侧**已具备条件：读源=act 已生效、双写已开、dev 实测真实残留 = 0
（`wf_task` 已办非删除行 212 条**全部被 ACT 覆盖**，未覆盖的 178 条**全是逻辑删除**）。
故先加**读侧残留合并开关**作为 P6 的第一个可回退台阶。

### 7.2 开关语义

| 值 | 行为 |
|---|---|
| `true`（默认，零行为变化） | 待办/已办**列表**与顶栏**角标**都从 `wf_task` 补进「ACT 表达不了的行」，保证零丢单 |
| `false`（纯 ACT 单源） | 列表与角标 100% 来自 `ACT_*`，**完全不查 `wf_task`** |

> ⚠️ **列表与角标必须同开关**：角标走 `WfTaskActReader#countFromAct`，该方法自带残留兜底计数。
> 只关列表不关角标 → 「角标 25、列表只有 24」的割裂。故两处由同一开关统一门控
> （`WfTaskServiceImpl#list` / `#page` + `WfTaskActReader#countFromAct`）。

### 7.3 关闭的收益

1. **`total` 严格等于 ACT count**：消除「`actTotal + residueTotal`」与实际可翻行数的错位
   —— 即「残留只在第一页并入、深页取 ACT 连续切片」这一口径 a 固有缺陷导致的**深页漏行**；
2. **真实暴露 ACT 覆盖率缺口**：否则名义上读 ACT、实际绕回 `wf_task`，既没真正翻源、又掩盖缺口
   （这正是 2026-09-30 dev 当初回退到 `wf` 的原因）。

### 7.4 关闭前的硬前置

按 2026-11-08 dev 实测（§6.2）标注状态：

- [x] ① 活跃 N:1 残留 = **0**（生产态口径；脚本口径 1 条为 `is_deleted=1` 历史数据）
- [x] ⑦ 孤儿 = **0**（生产态口径；脚本口径 225 条全为 `is_deleted=1` 历史数据）
- [x] 覆盖率硬指标：ACT 可读行 243 / `wf_task` 列表行 240 = **101.2%** ≥ 99%
- [x] `engine-multi-instance.enabled=true` 且定义已重新部署（MI 1:1 已生效）
- [ ] ⑥ 双写漂移 = 0 —— ⚠️ **代码已修（防新增），存量待清**：根因 `WfStateProjector` 三处漏
      `taskActWriter.sync()` 回写 ACT，已修并加回归（见 §6.2.2）；存量 68 条用
      `V2026.11.08_002__fix_task_act_status_drift.sql` 修正（已在事务内验证影响 68 行、未留痕）。
      清完复跑 ⑥ 应为 0，方可翻源。原漂移形态：已办列表状态标签错（「待办」冒充「办结」）、
      且按「办结」筛选会漏行。
- [x] **草稿合成待办的处置**（§6.3）—— ✅ 已解决（2026-11-08，方案 A 落地，见 §7.8）：
      草稿（`wf_instance.status=5`）的待办条目已从「待办」列表移出，改由「我的请求」承载；
      「我的请求」的草稿**零改动**（后端 act 读源下已并入 records+total，前端已有续填/删除）。
      dev 实测：待办侧残留 1 → **0**，生产态被移出的仅 1 条草稿，草稿本身未丢失。

### 7.5 开关不管的事（避免误解）

- **不控制 `wf_task` 的写入**：写入退役属 P6，须先解决合成待办归属与 6 态映射（见 7.1）。
- **不影响 `blade.workflow.wf-table-retirement.enabled`（T-14）**：后者只闸控「读空回退 `wf_*` 分支」，
  **不覆盖** `wf_task` 的写入，也不覆盖本开关门控的残留合并 —— 两者互补，不重叠。
- **不改读源**：`task-read-source` 仍须为 `act`；`task-act-write` 仍是硬前置（关它 ACT 业务列全空 → 列表全空）。

### 7.6 推进顺序（P6 正确路径）

```
① 处置草稿合成待办（§7.7）           ← ✅ 已完成（2026-11-08，方案 A 落地，见 §7.8）
② 本开关关闭（读侧纯 ACT）            ← 代码已落地，dev 可直接置 false（待端到端实测）
③ 剩余读点逐个翻源（reject-nodes / monitor / count / form-render / 自动测试 …）
④ blade 6 态 → ACT 状态映射定案
⑤ 停写 wf_task → 删 Entity/Mapper/Service → DROP 表（P6 收尾）
```

每步都可回退；**顺序不可跳**。跳过 ①④ 直接停写，会造成「草稿/被退回的发起人看不到待办」的线上死循环。

### 7.7 草稿合成待办的三条处置路径（已拍板 A）

| 方案 | 做法 | 代价 | 风险 |
|---|---|---|---|
| **A. 草稿移出「待办」列表** ✅ **已落地** | 草稿不再进「待办」，由「我的请求」承载（后端 `WfInstanceServiceImpl:863-886` 在 act 读源下已把草稿并入 records+total，前端 `Request.tsx` 已有 `INSTANCE_STATUS[5]`/`openDraft`/`deleteDraft`） | 后端 4 处过滤（§7.8），**前端零改动** | 用户习惯变化：原来在「待办」看到草稿，改后去「我的请求」找（「我的请求」是发起人自己的列表，可发现性可接受） |
| B. 草稿定向补齐 | 关闭通用残留合并，但保留一条**定向**查询：只补 `engine_task_id` 为空的草稿行 | 后端加一路定向查询 + `total` 单独加草稿数 | 草稿排位靠后时深页仍看不到（老问题重现）；且「残留合并」从"含糊的兜底"变成"显式例外"，长期仍需退役 |
| C. 草稿也进引擎 | 保存草稿时启动引擎实例，建真实引擎任务 | 架构改动大 | ❌ **与「草稿不启动引擎、零副作用」的设计直接冲突**，且草稿要能被删除/重开，引擎生命周期成本高。不推荐 |

### 7.8 方案 A 落地记录（2026-11-08）

**改动点（4 处，全部只影响「读侧」，不改写侧、不动表结构）**：

| # | 位置 | 改动 |
|---|---|---|
| 1 | `WfTaskServiceImpl#wfTaskQuery` | 加 `NOT EXISTS(草稿实例)` 排除条件 —— 一次覆盖**残留合并**（list/page）、**wf 回退路径**、`countResidue` 共 5 个调用点 |
| 2 | `WfTaskActReader#countFromAct` | 角标残留兜底 SQL 加同一排除条件（否则「角标含草稿、待办列表不含」→ 割裂） |
| 3 | `WfInstanceServiceImpl:2050` | 「我的请求」台账取 pending `taskId` 时排除草稿待办 —— 草稿无引擎任务，转办/催办对它无意义，不应挂 `taskId` |
| 4 | 前端 | **零改动** —— 「我的请求」已完整承载草稿（`Request.tsx:20` 状态列、`openDraft` 续填、`deleteDraft` 删除；后端 `WfInstanceServiceImpl:863-886` act 读源下已并入草稿） |

**判据设计（关键）**：用「**所属实例 `status=5`（草稿）**」而非「`engine_task_id` 为空」——
后者会误伤「退回发起人重提交」的合成待办（那是**运行中**实例的正常待办，必须留在待办列表）。
已由单测 `WfTaskResidueMergeTest` 两例锁定：
`excludesDraftSyntheticTodo`（草稿被排除）+ `keepsRunningSyntheticTodo`（运行中合成待办不误伤）。

**SQL 写法**：`NOT EXISTS` 而非 `NOT IN (子查询)` —— 后者遇 `inst_id IS NULL` 会因三值逻辑（UNKNOWN）把该行**误排**；
且子查询里的 `inst_id` 故意**不限定外层表名**，以兼容 MyBatis-Plus 生成的 `FROM wf_task`（无别名）与将来可能带别名的形态。

**dev 实测（`log/wf_draft_exclude_verify.py` / `log/wf_draft_exclude_detail.py`，只读）**：

| 项 | 改动前 | 改动后 |
|---|---|---|
| 待办侧残留（`status=0` 未被 ACT 覆盖） | 1 | **0** |
| 已办侧残留 | 0 | 0 |
| 合成待办按实例 status 分组 | `status=5` 1 条（被排除） | 无其它 status，无误伤 |
| 「我的请求」草稿数（`starter=10`） | 1 | **1（草稿没丢）** |
| 角标一致性（全用户） | — | ACT 待办 = 残留(0) = 列表条数 = 角标，**无割裂** |

**净效果**：生产态被移出「待办」的仅 **1 条**（王海燕「综合费用报销全流程审批流」草稿，`inst=2118968283743944705`），
该草稿在「我的请求」仍可见、可「继续填写」/「删除」，**未丢失**。



