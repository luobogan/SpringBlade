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
- [x] ⑥ 双写漂移 = 0 —— ✅ **已达标**（2026-11-08）：根因 `WfStateProjector` 三处漏
      `taskActWriter.sync()` 回写 ACT，已修并加回归（见 §6.2.2）；存量 68 条已用
      `V2026.11.08_002__fix_task_act_status_drift.sql` 修正（修前 68 → 修后 0），
      复跑校验脚本确认 ⑥ = 0。
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
② 读侧切 ACT 单源                     ← ✅ 已完成（2026-11-08，dev 实测通过，见 §7.9）
③ 剩余读点逐个翻源                     ← ✅ 已完成（10 项，**10/10**，清单见 §7.12，完成记录见 §7.13 ~ §7.22）
④ blade 6 态 → ACT 状态映射定案       ← ✅ 已定案（2026-11-08，按「是否引擎任务本体」分流，见 §7.10）
⑤ 停写 wf_task → 删表 → DROP         ← ⏸ 暂缓（2026-11-08 决策，理由与重启条件见 §7.11）
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

### 7.9 端到端实测结论（2026-11-08，dev，开关已切 false）

**开关落点**：改 `application.yml` 默认值 `task-residue-merge.enabled: false`（**不动 Nacos**）——
理由：Nacos 的 `blade.yaml`/`blade-dev.yaml`/`blade-test.yaml`/`blade-prod.yaml` **全部指向同一个物理库 `blade`**，
改配置会全服务广播、波及所有环境；且 `blade-workflow-dev.yaml`（workflow 专属配置）经确认
**不含任何 task- 相关键**，故代码默认值不会被覆盖。
回退 = 改回 `true` 并重启 blade-workflow（秒级）。

**部署**：重新 `install` 并以原参数重启（`java -jar blade-workflow.jar --spring.profiles.active=dev`），
18s 就绪，服务日志 **ERROR = 0**。

**实测结果**（admin / tenant 000000，经网关 `:8000/api`）：

| # | 验证项 | 结果 |
|---|---|---|
| 1 | 待办 `assignee=10`（草稿所属人） | `total=0` —— 草稿已移出待办 ✅（DB 侧该用户本就无 ACT 待办） |
| 2 | 待办 `assignee=676` | `total=11 / rows=11`，全部 `status=0`，与 DB 的 ACT 待办数一致 ✅ |
| 3 | 已办 `assignee=676` | `total=95`，状态标签**既有 2(已办) 也有 4(办结)** ✅ —— 漂移修复生效（修前这批显示「待办」） |
| 4 | 角标 `/monitor/count` | `{"todo":11,"done":95}` —— **与列表条数完全一致，零割裂** ✅ |
| 5 | 草稿实例 `GET /instance/2118968283743944705` | `code=200 status=5 title=综合费用报销全流程审批流 starter=10` → 草稿完好未丢 ✅ |
| 6 | 操作链路前置 `GET /task/{列表里的 id}` | `code=200`，id/status/node 正确 → `requireTodoTask` 按 `wf_task.id` 能查到 ✅（approve/reject/forward 的共同前置） |
| 7 | **深页一致性**（已办 95 条，pageSize=10） | 第 1/5/9 页各 10 条、**第 10 页 5 条**（95 = 9×10+5），每页 total 恒为 95 → **深页漏行彻底消失** ✅ |
| 8 | `GET /instance/mine` | `code=200 total=37`，草稿回退分支正常 ✅ |

> 至此 §7.4 的翻源硬前置全部达标：① 0 / ④ 仅剩草稿（已由「我的请求」承载）/ ⑤ 0 / ⑥ 0 / ⑦ 0 / 覆盖率 101.2%，
> 且「待办/已办」两个列表已是 **100% 来自 ACT_* 的纯单源**。

---

### 7.10 ④ blade 6 态 → ACT 状态映射定案（2026-11-08 已定案）

**结论：blade 6 态不塞进 `ACT_*`。** 按「**是否引擎任务本体**」分流，各走各的权威存储：

| 行类别 | status | engine_task_id | 权威存储 | 列表读取来源 |
|---|---|---|---|---|
| 引擎任务本体·待办 | 0 | 有 | `ACT_RU_TASK` | ACT |
| 引擎任务本体·已办/办结/自动提交 | 2 / 4 / 6 | 有 | `ACT_HI_TASKINST` | ACT |
| **blade 扩展·协办** | 7 | 有（同节点多人） | **`wf_task`** | wf_task |
| **blade 扩展·抄送** | 8 | 有（沿用） | **`wf_task`** | wf_task |
| **blade 扩展·传阅** | 11 | 有 | **`wf_task`** | wf_task |
| 合成待办·草稿 | 0 | 空 | `wf_instance` | 「我的请求」（方案 A 已移出待办） |
| 合成待办·退回发起人重提交 | 0 | 空 | **`wf_task`** | wf_task |

**定案理由**：

1. `ACT_*.BUSINESS_STATUS_` 只是 `wf_task.status` 的**投影**，不是引擎事实 —— 引擎先删 `ACT_RU_TASK`、
   事件再反写 sync，中间存在**漂移窗口**（实测 68 条，见 §6.2.2）。把它当权威会把漂移窗口暴露给业务。
2. 协办(7)/抄送(8)/传阅(11) 是 **blade 业务产物，引擎无原生物**；抄送历史更是以 `wf_task` 行为权威存储
   （`WfTaskServiceImpl#circulate` 自述「identityLink 只是预热」，且 `ACT_RU_IDENTITYLINK` 任务完成后会被清理）。
3. 「进入过」≠「办理过」：`ACT_HI_TASKINST` 有行不代表办理过 —— 协办/抄送/传阅同样会在 HI 表建行
   → 监控回退/归档回退的候选集不能用 HI 表裸算（见 ③ 第 2 项）。

#### 7.10.1 读侧判据：从「兜底」改为「精确」

| 判据 | 含义 | 问题 |
|---|---|---|
| **旧（兜底）** | `id NOT IN (SELECT BIZ_TASK_ID_ FROM ACT_*)` | 把**任何**漏双写的行都补进来 → 掩盖真实覆盖率缺口；开关语义退化为「应急兜底」，无法长期维护 |
| **新（精确）** | `status IN (7,8,11) OR engine_task_id 为空` | 只取**结构性**无法由 ACT 表达的行 → 不掩盖缺口、可长期维护 |

落点：`WfTaskServiceImpl#wfTaskQuery(..., extOnly)`（`extOnly=true` 用于残留合并；`false` 用于**读源=wf 的
回退路径**，回退必须全量否则降级后缺行）、`WfTaskActReader#countFromAct`（角标两段残留 SQL，同口径）。

#### 7.10.2 开关语义随之变更（重要）

`blade.workflow.task-residue-merge.enabled` **不再**表示「要不要兜底 ACT 未覆盖的残留」，
而是表示「**要不要读 blade 扩展任务**」：

- `true`（默认，**生产安全值**）：列表与角标 = ACT（引擎任务本体）+ wf_task（blade 扩展任务 + 合成待办）→ 不丢行；
- `false`（**仅诊断用**）：完全不查 `wf_task` → ⚠️ **生产禁止**：协办/抄送/传阅/退回发起人重提交会全部从列表消失
  （dev 恰好无这些行故看不出丢行，**生产必有** —— 转办/抄送会 insert `status=8` 行）。

#### 7.10.3 顺带修掉的两个既有缺陷

| # | 缺陷 | 症状 | 修法 |
|---|---|---|---|
| 1 | `countFromAct` 的残留裸 SQL **缺 `is_deleted = 0`** | `JdbcTemplate` 裸 SQL 绕过 MyBatis-Plus 的 `@TableLogic` 自动过滤 → 已逻辑删除的合成待办被计入角标，而列表侧（Mapper）自动过滤 → **「角标 12、列表 11」的割裂**（2026-11-08 实测发现；`HEAD` 版本同样存在） | 两段残留 SQL 显式加 `t.is_deleted = 0` |
| 2 | `countFromAct` 的残留裸 SQL **缺排重**（`NOT IN` ACT 的 `BIZ_TASK_ID_`） | 交集行与 ACT 段**双计** → 角标虚高 | 补 `t.id NOT IN (SELECT BIZ_TASK_ID_ FROM ACT_*)`（待办查 RU、已办查 HI） |

> 教训：**凡用 `JdbcTemplate` 裸 SQL 读 `wf_task`，必须显式写 `is_deleted = 0` 与租户条件**
> —— MyBatis-Plus 的逻辑删除与租户插件只作用于 Mapper 查询。

### 7.11 ⑤ 停写 `wf_task` / 删 Entity / DROP 表 —— **暂缓执行**

**2026-11-08 决策：不删表、不停写、不删 Entity/Mapper/Service。**

**理由**（全量盘点 `blade-workflow` 模块所有 `wf_task` 读写点后确认）：

1. **三类业务语义只有 `wf_task` 能承载**：blade 扩展任务（协办/抄送/传阅）、无引擎任务的合成待办（草稿/退回发起人）、
   blade 6 态语义（§7.10）。
2. **写侧无法立即停**：`WfTaskActWriter` 的 N:1 守卫每次双写都要
   `SELECT COUNT(*) FROM wf_task WHERE engine_task_id = ?` → **在被替代前表不能物理删除**。
3. **③ 的剩余读点仍依赖它**（见下表），停写会让这些路径失去数据源。

**重启退役的条件**：① blade 扩展任务迁到独立表或 `ACT_*` 新增承载列；② 合成待办有替代载体；
③ blade 6 态的权威存储按 §7.10 定案落定且切换完成。

**代码留痕**：`WfTask` 实体类注释已加「表退役暂缓 · 2026-11-08 决策」段（含上述理由与重启条件）。

### 7.12 ③ 剩余读点翻源清单（按改造代价排序，2026-11-08 盘点）

| 序 | 读点 | 位置 | 难度 | 说明 |
|---|---|---|---|---|
| 1 | 监控 overdue / taskId / 超时计数（5 处 wf_task 子查） | `WfInstanceServiceImpl`：`computeMonitorIdRestriction` / `enrichMonitor`×2 / `monitorAnomalySummary` / `monitorStats` | ✅ **已完成**（2026-11-08） | 见 §7.13 |
| 2 | 「已办理过节点集」（3 处） | `loadPassedNodeKeys` / `archivedCandidates` / `recallTargetPassed` | ✅ **已完成**（2026-11-08） | 见 §7.14 |
| 3 | 监管动作关待办（2 处） | `forceRecallTo` / `monitorRejectTo` | ✅ **已完成**（2026-11-08） | 见 §7.15 |
| 4 | `canVisible` 参与人判定（半翻源） | `WfInstanceServiceImpl` | ✅ **已完成**（2026-11-08） | 见 §7.16 |
| 5 | 超时 job 回读（读源扫描） | `WfTimeoutJob`（`scanDueFromAct`） | ✅ **已完成**（读源扫描，P3-6 已落地；`autoApprove` 内部 `wf_task.id` 定位属第 9 项） | 见 §7.17 |
| 6 | 公文待办/已办列表 | `WfOdocTodoServiceImpl` | ✅ **已完成**（复用通用列表混合分页 + 公文维度下推） | 见 §7.18 |
| 7 | form-render / 节点操作者的 wf 回退分支 | `WfFormRenderServiceImpl:139`、`WfInstanceServiceImpl:1540/1582/1632`（4199 为 BPMN 可达性辅助，无关 wf_task） | 小 | **✅ 已完成**（2026-11-08 核验：回退分支已正确受 `actRead()` 闸门，wf 兜底为设计内双源；无需改代码，见 §7.19） |
| 8 | 自由流程 / 子流程 | `WfFreeFlowDriver:145/157/184`、`WfSubflowServiceImpl:153`（列表 ACT 路径步骤名） | 中高 | **✅ 已完成**（2026-11-08：写侧读保留 + ACT 列表补步骤名 + 子流程办结人翻源，见 §7.20） |
| 9 | **操作链路的任务定位与 ID 语义** | `WfTaskServiceImpl:1433/1435`（`resolveTask`） | **最高** | **✅ 已完成（设计内覆盖，见 §7.21）**：列表恒返回 `wf_task.id`，`wf_task` 保留为任务 id 权威存储（§7.11），操作链无需改 ID 语义；消息中心 `engineTaskId` 由 `resolveTask` 的 engine_task_id 回退兜住 |
| 10 | 签章流 + 审批意见双写 | `OdocSealAdvancer:170`、`WfWriteHelper:311` | 中 | **✅ 已完成（设计内覆盖，见 §7.22）**：均为写侧读，依赖保留的 `wf_task.engine_task_id` 定位引擎任务；退役后改用 `BIZ_TASK_ID_` 反查（§7.11 重启条件） |

**已在本轮顺手修掉的**（原属第 9 项的一部分，且是**现存 bug**）：
`WfTaskActReader#toWfTaskByBizIdFromAct` 此前**没读 `ORIGINAL_USER_`** →
读源=act 时 `WfAuthUtil#canOperateTask` 的 `me.equals(originalUser)` 恒 false →
**代理代办的人既看不到操作按钮（前端 readonly 判定），后端 `requireOperateTask` 还会直接 403**。已补该列读取。

### 7.13 ③ 第 1 项：监控的 5 处 wf_task 子查 → ACT（2026-11-08 已完成，行为零差异）

**改造点**（`WfInstanceServiceImpl`，5 处，全部按**任务读源** `task-read-source` 分流）：

| # | 位置 | 改造前（读 `wf_task`） | 改造后（读 `ACT_*`） |
|---|---|---|---|
| 1 | `computeMonitorIdRestriction` 超时维度 | `wf_task` where `status=0 AND due_time<now` | `ACT_RU_TASK.DUE_DATE_ < now`（`overdueInstanceIdsFromAct`） |
| 2 | `enrichMonitor` overdue 标记 | 同上（页内实例范围） | 同上（限定页内实例范围） |
| 3 | `enrichMonitor` 当前待办 taskId | `wf_task` where `status=0` 取最小 id | `ACT_RU_TASK.BIZ_TASK_ID_` 取 `MIN()`（`pendingTaskIdsByInstanceFromAct`） |
| 4 | `monitorAnomalySummary` 超时计数 | `wf_task` group by inst_id | `COUNT(DISTINCT BUSINESS_ID_)`（`countOverdueInstancesFromAct`） |
| 5 | `monitorStats` 超时计数 | 同上 | 同上 |

**新增读器方法**（`WfTaskActReader`）：`overdueInstanceIdsFromAct` / `countOverdueInstancesFromAct` /
`pendingTaskIdsByInstanceFromAct`，统一用 `ACT_RU_TASK r JOIN ACT_HI_PROCINST p ON p.ID_=r.PROC_INST_ID_`
关联 blade 实例（`p.BUSINESS_ID_` = 原 `wf_instance.id` 雪花值），并复用既有 `appendActTenant` 加租户。

**两个必须守住的口径约束**（翻源不改行为）：

1. **分流依据用「任务读源」而非「实例读源」**：任务维度的读取一律按 `task-read-source` 判断
   （`taskActReader.actRead()`），因为 `task-read-source` 与 `instance-read-source` 是**两个独立开关**，
   混用会在两者配置不一致时口径分裂。
2. **刻意不加 `IS_TEST_` 过滤**：wf 侧原查询没有过滤测试态，ACT 侧若加了过滤 → 监控列表与看板在测试实例上分裂。
   同理 `pendingTaskIdsByInstanceFromAct` 不排除 `is_test`，与 wf 侧严格一致。

**已知口径边界（生产需注意）**：wf 侧的「当前待办」会包含「退回发起人重提交」的合成待办
（`status=0` 且 `engine_task_id` 为空），而 ACT 侧无此行 → 此类实例的 `taskId` 两边会不同
（ACT 侧给的是真实引擎任务的 `BIZ_TASK_ID_`）。dev 无此类行，故实测零差异；生产若有，
影响面仅限「监控页转办/催办」的目标任务选择，不影响列表条数与超时判定。

**实测对比（dev，改造前 jar 12:27 vs 改造后 jar 13:09，同一 token、同一时刻）**：

| 项 | 改造前（wf_task） | 改造后（ACT） | 一致 |
|---|---|---|---|
| `anomaly-summary.overdue` | 2 | 2 | ✅ |
| `stats.overdue` / `total` / `running` | 2 / 35 / 17 | 2 / 35 / 17 | ✅ |
| `instances.total` | 35 | 35 | ✅ |
| 每行 `overdue`（20 行逐行比对） | 仅 `2117206468097785857`=true | 完全相同 | ✅ |
| 每行 `taskId`（20 行逐行比对） | 全部 | 完全相同 | ✅ |
| 服务 ERROR | 0 | 0 | ✅ |

**回归**：`WfTaskResidueMergeTest` 10/10；全模块 307 用例，2 失败 = 已 stash 双重验证的既有失败。

### 7.14 ③ 第 2 项：「已办理过节点集」3 处 → ACT（2026-11-08 已完成，行为零差异）

**改造点**（`WfInstanceServiceImpl`，3 处，按**任务读源** `task-read-source` 分流）：

| # | 位置 | wf 侧口径 | ACT 侧口径 |
|---|---|---|---|
| 1 | `loadPassedNodeKeys(instId)` | 本实例 `wf_task` **全部** `node_key`（**无 status 过滤**） | `ACT_HI_TASKINST ∪ ACT_RU_TASK` 的 `TASK_DEF_KEY_`（**无 status 过滤**） |
| 2 | `archivedCandidates(defId, instId)` | `status=DONE(2)` 的 `node_key`，按 `id` 倒序、每节点取最近一次 | `BUSINESS_STATUS_='DONE'` + `END_TIME_` 非空，按 `END_TIME_` 倒序 |
| 3 | `recallTargetPassed(inst, targetNode)` | 同 #1（无 status 过滤）；**空集合 → 放行** | 同 #1；**空集合 → 放行**（照搬保守语义） |

**新增读器方法**（`WfTaskActReader`）：`passedNodeKeysFromAct` / `doneNodeKeysFromAct`，
实例维度靠 `ACT_HI_PROCINST.BUSINESS_ID_`（= 原 `wf_instance.id`）关联。

**三个必须守住的口径约束**：

1. **不加 status 过滤**（#1/#3）：`loadPassedNodeKeys` 名为「已办理过」，实际是「本实例**出现过**的节点」
   （含当前待办节点、协办/抄送/传阅节点）→ 若在 ACT 侧加上 `BUSINESS_STATUS_ IN ('DONE','FINISHED')` 过滤，
   会把「进入过但未办理」的节点排除 → **监控回退的落点范围被收窄**（原本可退的节点变得不可退）→ 功能回归。
2. **空集合按「通过」处理**（#3）：wf 侧「台账读不到 → 不阻断监管动作」的保守语义必须照搬，
   否则 ACT 侧读到空集会把越级回退**拦死**。
3. **`DONE` 的映射**：wf `status=2` → ACT `BUSINESS_STATUS_='DONE'`（`WfTaskActWriter#statusStr`），
   且要求 `END_TIME_ IS NOT NULL`（已办结）→ 与 `WfTaskServiceImpl#done()` 的六态口径对齐。

**已知口径差异（不影响判定）**：ACT 侧「出现过」集合比 wf 侧**多一个 `Task_Create`**（创建节点在引擎侧有
HI 行、且 `BUSINESS_STATUS_` 非 DONE；wf 侧创建节点的待办行已被清理）。影响分析：
`Task_Create` 是创建节点，而三处使用点对创建节点都**单独放行**
（`archivedCandidates` 末尾追加创建节点、`recallTargetPassed` 对 `nodeType=0` 直接 `return true`），
故**候选集合与判定结果均不变**（实测已逐条比对确认，见下）。

**实测对比（dev，同一 token；改造前 jar 13:09 vs 改造后 jar 13:22）**：

| 实例（归档态，走 `archivedCandidates`） | 改造前（wf_task） | 改造后（ACT） | 一致 |
|---|---|---|---|
| 2118961249115996161 | n=8，default=Task_Cashier | n=8，default=Task_Cashier | ✅ 节点与**顺序**全同 |
| 2118672243744673793 | n=9 | n=9 | ✅ |
| 2117206426024722433 | n=8 | n=8 | ✅ |
| 2117206378780082177 | n=9 | n=9 | ✅ |

| 实例（运行中，走 `loadPassedNodeKeys`） | 改造后 | 说明 |
|---|---|---|
| 2118670646641790978 / 2118667114429648898 / 2118638326413103105 / 2118609457119289346 | n=3 `[Task_DeptDirector, Task_DeptLeader, Task_Create]` | 结构候选 ∩ (已办理 ∪ 创建节点)，`Task_Create` 来自创建节点分支 |

**SQL 层集合比对（5 个实例）**：`done` 口径 wf 与 ACT **完全相同**（8/7/8/4/7 个节点）；
`all` 口径 ACT 为 wf 的**超集**（多 `Task_Create`）→ 与上文分析一致，不改变判定。

**回归**：全模块 307 用例（2 失败 = 既有）；待办 11 / 已办 95 / 角标 `{todo:11, done:95}` /
异常卡片 overdue=2 / 看板 overdue=2 —— 与改造前**完全一致**；服务 ERROR=0。

> ⚠️ **踩坑记录**：`appendActTenant` 是「末尾追加」语义，若 SQL 片段里已拼好 `ORDER BY`，
> 租户条件会被追加到 `ORDER BY` 之后 → `bad SQL grammar`；`UNION ALL` 的第二段也不能沿用第一段的表别名。
> 本次修法：`ORDER BY` 放在 `appendActTenant` **之后**追加；`UNION ALL` 拆成两段独立查询（各自带自己的租户条件）。

### 7.15 ③ 第 3 项：监管动作关待办 2 处 → ACT（2026-11-08 已完成，端到端验证）

**改造点**（`WfInstanceServiceImpl`，2 处监管动作 `forceRecallTo` / `monitorRejectTo` 的「① 关闭当前节点未办任务」读源点，按**任务读源** `task-read-source` 分流）：

| # | 位置 | 原读源 | ACT 读源 |
|---|---|---|---|
| 1 | `forceRecallTo` 关待办（仅非归档态） | `wf_task.status=TODO` 全量 | `ACT_RU_TASK.BIZ_TASK_ID_`（经 `ACT_HI_PROCINST.BUSINESS_ID_` 关联） |
| 2 | `monitorRejectTo` 关待办（含归档态） | 同上 | 同上 |

**新增**：私有方法 `collectPendingCloseTasks(instId)`（统一两处收口）+ 读器方法 `WfTaskActReader.pendingCloseBizIdsFromAct(instId)`
（返回本实例当前活跃 RU 任务的 `BIZ_TASK_ID_` 集合；`BIZ_TASK_ID_` 经核实为 **bigint** 列，存 `wf_task.id` 数字形式）。

**为什么「关待办 + sync」这一步不可省**：`advance()` 只读 `ACT_RU_TASK` 当前活动任务并 `insertTask`，**不删除**旧 `wf_task` 行。
随后的 `moveActivity` 会删除当前节点的 RU 任务，但 `ACT_HI_TASKINST` 对应行的 `BUSINESS_STATUS_`（blade 自定义列）引擎不会改
→ 若不先在删除前把 `wf_task` 收尾为 DONE 并 `sync`，HI 行 `BUSINESS_STATUS_` 会停在 `TODO` **漂移**。
故该步只收尾"即将被引擎删除的活跃任务对应的台账行"，翻源后仍以引擎 `ACT_RU_TASK` 为权威取这些行。

**回退策略（行为不变）**：act 读源下，若 `ACT_RU_TASK` 取到活跃任务则**仅**关这些（精确，对齐引擎权威）；
若取不到（`BUSINESS_ID_` 无运行态，典型 = 归档态无运行态 / 纯台账残留），回退到 `wf_task.status=TODO`
（保证归档残留待办仍被收尾，与改造前一致）。运行中态下 `wf_task` 的「幽灵待办」不在此关 —— act 读源下用户侧读 ACT、不可见，
其清理属漂移修复职责而非监管动作。

**口径一致性（只读 SQL 比对，dev 4 个运行中实例）**：`wf_task.status=TODO` 的 id 集合 与 `ACT_RU_TASK.BIZ_TASK_ID_` 集合 **逐行完全一致**
→ act 分支取到的 `wf_task` 行集合与 wf 分支完全相同 → **零行为差异**。

**端到端实测**（dev 实例 2118670646641790978，`monitorRejectTo` → `Task_DeptDirector`）：
- 实例 `current_node_key`：Task_FinAudit → **Task_DeptDirector** ✅（流程成功回退）
- 目标节点新待办（advance 正常）：2119...497 / 2119...617（Task_DeptDirector, status=0, 有 engine_task_id）✅
- 旧节点 Task_FinAudit 的 HI 行：`BUSINESS_STATUS_=TODO, END_TIME_=NULL` → **`BUSINESS_STATUS_=DONE, END_TIME_=13:48:39`** ✅（关待办 + sync 正确收尾，无漂移）
- 接口 `code=200`；服务 ERROR=0

> ⚠️ 实测为**写操作**，真实回退该实例一步（dev 测试数据，可重新流转恢复）。

**回归**：全模块 307 用例（2 失败 = 既有）；`clean install` 一度因插入辅助方法时 `@Override/@Transactional` 注解错位（残留于 `forceRecallTo` 前、
误挂到 private 方法上）编译失败，已修（注解移回 `forceRecallTo`，辅助方法 private 无需 `@Transactional`）；服务 ERROR=0。

### 7.16 ③ 第 4 项：记录级鉴权 `canVisible` 参与人判定 → ACT（2026-11-08 已完成，**半翻源**）

**改造点**（`WfInstanceServiceImpl#canVisible`，按**任务读源** `task-read-source` 分流）：

```java
if (isInMonitorScope(...)) return true;
// 【③ 读点翻源】act 读源：办理人先查 ACT（RU + HI 的 ASSIGNEE_/BIZ_ASSIGNEE_），未命中回退 wf_task 全 status 兜底
if (taskActReader.actRead() && taskActReader.isParticipantFromAct(inst.getId(), me)) return true;
Long count = taskMapper.selectCount(... .eq(WfTask::getInstId, inst.getId()).eq(WfTask::getAssignee, me));
return count != null && count > 0;
```

**新增读器方法**（`WfTaskActReader#isParticipantFromAct(bladeInstId, assignee)`）：查 `ACT_RU_TASK ∪ ACT_HI_TASKINST` 的
`ASSIGNEE_` 或 blade 扩展列 `BIZ_ASSIGNEE_` 是否命中（实例维度靠 `ACT_HI_PROCINST.BUSINESS_ID_` 关联）。

**为什么是「半翻源」而非纯翻源**：`canVisible` 的「参与人」= 办理人 + 抄送人(status 8) + 传阅人(status 11) + 退回发起人重提交等**纯台账行**。
而抄送/传阅在 ACT 侧**无权威源**（`application.yml` 注释明示「抄送历史以 `wf_task` 行为权威存储，`ACT_RU_IDENTITYLINK` 任务完成后会被清理」；
且 `WfTaskActWriter#sync` 对 `engineTaskId` 为空的纯台账行**直接跳过**）。故这部分**必须保留 `wf_task` 兜底**。

**正确性保证（零回归）**：act 分支 = `isParticipantFromAct(ACT) OR wf_task(全 status)` ——
act 分支结果 **⊇** 原 wf 分支（wf 全 status 是 OR 的一边），故**不会误判「不可见」**（鉴权宁宽勿严）；
办理人部分走 ACT（引擎权威、与任务列表读源一致），反而更准。

**实测（dev，SQL 层等价验证；沙箱全员 admin 致 `isSelfOrAdmin` 短路、参与人分支无法端到端触发）**：
对 4 个实例（2 运行 + 2 归档）的 `wf_task` 参与人（status≠8/11）逐人核对 `isParticipantFromAct` 的 ACT 查询命中：
- 归档实例：全部 `in_hi=1`（HI 有记录）、`in_ru=NULL`（已无运行态）✅
- 运行实例：全部 `in_ru=1` 或 `in_hi=1` ✅
- **结论**：`isParticipantFromAct` 对办理人 100% 覆盖 wf 分支判定；抄送/传阅（dev 无数据）由 wf_task 兜底 → act 分支与 wf 分支**零行为差异**。

**回归**：服务 ERROR=0；健康 `monitor/count` todo=11 / done=96（done 多 1 条 = 前次回退实测产生的历史）；全模块 307 用例（2 失败 = 既有）。

> ⚠️ **生产注意**：本项半翻源，act 读源下 `canVisible` 仍会读 `wf_task`（抄送/传阅/退回发起人兜底）。这**不违反**「去 wf_ 表」目标——
> 目标是用户侧任务列表读 ACT 不漂移；鉴权点的抄送/传阅兜底是为正确性（ACT 侧本就不可靠）。待抄送/传阅的 ACT 侧权威源方案
> （如 `ACT_HI_IDENTITYLINK` 永久化或自建关系表）落地后，可进一步收口为纯 ACT。

### 7.17 ③ 第 5 项：超时 job 扫描读源 → ACT（2026-11-08 确认已完成，零差异）

**状态**：本项**读源翻源已于 P3-6 改造落地**（非本轮新写），本次为确认 + 文档化。

**已实现**（`WfTimeoutJob`）：
- `doScan` → `scanDueFromAct(now)`（:136）：主源已从 `wf_task` 切到 `ACT_RU_TASK`
  —— 条件 `{ SUSPENSION_STATE_=1（活跃） AND DUE_DATE_ <= now AND (TIMEOUT_HANDLED_ IS NULL OR 0) AND COALESCE(IS_TEST_,0)=0 }`，
  经 `BIZ_TASK_ID_`（回退 `engine_task_id`）解析回 `wf_task`，再 `handle/fire` 在 wf_task 维度执行。
- 双保险：`scanDueFromAct` 命中后仍以 wf_task 复核（status=TODO / due_time 非空且<=now / timeout_handled≠1 / 非测试 / 未删），避免误触发。
- 防重：`markHandled` 置 `wf_task.timeout_handled=1` 并 `taskActWriter.sync`（双写 `TIMEOUT_HANDLED_` 到 `ACT_HI_TASKINST`/`ACT_RU_TASK`）。

**为何"被第 9 项阻塞"仅指 autoApprove 内部定位（非读源翻源）**：
`handle → timeoutService.fire → autoApprove(taskId)` 仍按 `wf_task.id` 定位具体任务去驱动引擎办理（第 9 项"操作链路 ID 语义"未动）。
该定位依赖 wf_task 双写台账仍在（写入退役暂缓），故读取侧翻源不影响它 —— **读源翻源范畴已闭环**，autoApprove 内部定位属操作链路收口（第 9 项），另行处理。

**零差异验证（dev，SQL 层）**：wf_task 待办候选（status=0 + due_time 非空 + 非测试 + 未删）= **19**，
经 `ACT_RU_TASK.BIZ_TASK_ID_`（活跃态）能覆盖 = **19**，`wf_not_covered = 0`
→ ACT 扫描读源 100% 覆盖 wf_task 真实待办，**零漏扫**。dev 当前 `wf_overdue_now=0`（无已逾期任务），但候选映射完整，
一旦有超时 ACT 必能扫到；命中路径与 wf 分支逐任务等价（双保险复核条件 ⊇ 旧 wf 扫描条件）。

**结论**：第 5 项读源翻源**已完成**，不阻塞其它项。

### 7.18 ③ 第 6 项：公文待办/已办列表 → ACT（2026-11-08 已完成，零差异）

**改造点**（`WfOdocTodoServiceImpl.pageTasks`，按**任务读源**分流复用通用列表混合分页）：
- 不再自己 `taskMapper.selectPage(wf_task)`，改为复用 `WfTaskServiceImpl.page()` 的混合分页（`wfTaskService.todoPage`/`donePage` 返回 `IPage<WfTaskVO>`）。
  - act 读源：ACT 主表（`ACT_RU_TASK`/`ACT_HI_TASKINST`）计数+分页 + 受 `task-residue-merge` 开关控制的 `wf_task` 扩展任务（协办7/抄送8/传阅11）兜底合并 —— 与通用列表同口径。
  - 降级 wf 单源分支不变（wf 读源下完全等价原逻辑）。
- **公文维度下推**：新增 `WfTaskListFilter.instIds`（`pAlias.BUSINESS_ID_ IN (...)`），`WfTaskActReader.appendInstanceFilter` + `WfTaskServiceImpl.wfTaskQuery` 均应用；`WfOdocTodoServiceImpl.requestScopeIds` 从 `wf_odoc_request` 取 `instance_id` 集合（offical_type/secret_level 过滤）填入。标题/时间过滤经 `filter.title`/`beginTime`/`endTime` 下推（ACT 路径 `appendInstanceFilter` like `p.TITLE_` + wf 路径 `hasInstanceFilter` 查 `wf_instance`）。
- 批量回查 `wf_instance` + `wf_odoc_request` 组装 `OdocTodoVO`（taskId=WfTaskVO.id=wf_task.id，操作链路不变）。

**为何不复写独立 ACT SQL 而复用通用列表**：通用列表已验证"ACT 主表 + 扩展任务兜底"的混合分页，公文列表只多了公文维度（instance_id 范围），复用即 DRY 且保证与通用列表零差异。

**与原 wf 口径的差异（预期，非回归）**：通用列表的混合分页待办/已办口径包含协办7/抄送8/传阅11（原公文 wf 口径只 status=0/2/4/6）；这是与通用列表统一的**正确**行为（原公文列表遗漏了协办/抄送/传阅）。

**零差异验证（dev，SQL 层，assignee=676 公文任务）**：
- wf 口径待办（status=0）id 集合 = **4** 条；act 口径（`ACT_RU_TASK.BIZ_ASSIGNEE_='676'` 在公文维度内）= 同 4 条 ✓
- wf 口径已办（status∈{2,4}）id 集合 = **42** 条；act 口径（`ACT_HI_TASKINST.BIZ_ASSIGNEE_='676'` 在公文维度内）= 同 42 条 ✓
- **逐任务 id 完全一致** → 翻源零差异。

**接口实测说明**：dev 沙箱全员 admin 致 `resolveSelfIfNotAdmin(676)` 把指定 assignee 当 admin 重定向，接口（`/odoc/todo?assignee=676`）返回 0（dev 限制，非代码 bug）；生产有非 admin 用户即正常。零差异已用 SQL 直接比对 act/wf 两口径 id 集合证明。

**回归**：服务 ERROR=0；全模块 307 用例（2 失败 = 既有）。

### 7.19 ③ 第 7 项：form-render / 节点操作者 wf 回退分支 → 已正确闸门（2026-11-08 核验，无需改造）

**核验结论**：第 7 项所列 wf_task 回退分支**均已受 `actRead()`（任务读源开关）正确闸门**，开关=act 时走 `taskActReader.*FromAct`，
开关=wf 时回退 `taskMapper`，与全模块既有分流范式一致。配合 §7.11「wf_task 不退役」决策，wf 兜底分支是设计内的双源/降级路径，
**不应删除**（删除即丢开关=wf 与降级能力）。故本项**无需改代码**。

**逐点核验**：

| # | 位置 | 读源分流 | 是否裸读 wf_task |
|---|---|---|---|
| 1 | `WfFormRenderServiceImpl.render:132-144`（`taskId` 定位节点 + `canOperate`） | `if (actRead()) taskActReader.toWfTaskByBizIdFromAct(taskId)`；`else taskMapper.selectById` | 否（受开关闸门） |
| 2 | `WfInstanceServiceImpl.fresh:1538-1547`（界面任务所属节点） | `if (actRead()) findNodeKeyByBizIdFromAct`；`else taskMapper` | 否 |
| 3 | `WfInstanceServiceImpl.fresh:1579-1596`（界面任务已办结判定） | `if (actRead()) isTaskDoneFromAct`；`else taskMapper` | 否 |
| 4 | `WfInstanceServiceImpl.isNodeStillActive:1630-1638`（并行分支活动判定） | `if (actRead() && engineInstId!=null) countActiveNodeTasksFromAct`；`else taskMapper.selectCount` | 否 |
| 5 | `WfInstanceServiceImpl:4199` `reachable` | （BPMN 可达性图遍历辅助方法，不读 wf_task） | 不涉及 |

**act 合成 `WfTask` 完整性核验**（`WfTaskActReader.toWfTaskByBizIdFromAct:921`）：act 路径合成的 `WfTask` 已含
`nodeKey` / `assignee` / `originalUser` / `status` —— 满足 `render` 的 `canOperate(task)`（含代理代办 `me.equals(originalUser)` 判定，
§7.12 已修 `ORIGINAL_USER_` 读取）与节点定位需求，act 路径下渲染包 readonly 判定与 wf 路径等价。

**为何"无需翻源"而非"已翻源"**：本项本质是「读源开关的 wf 兜底分支」，而非「裸 wf 直读」。它的存在正符合 §7.10/§7.11 的双源设计——
抄送/传阅/合成待办等 blade 扩展任务在 ACT 无权威源，wf_task 兜底必须保留。故第 7 项以「确认闸门正确 + 保留兜底」收口，
与第 4 项（canVisible 半翻源，同样保留 wf 兜底）同属「正确性优先于零残留」一类。

**回归**：无代码改动，不引入新回归；全文检索 `WfFormRenderServiceImpl` / `WfInstanceServiceImpl` 的 `taskMapper` 直读调用，
确认全部处于 `actRead()` 的 `else` 分支，无裸读。

### 7.20 ③ 第 8 项：自由流程 / 子流程 → ACT（2026-11-08 已完成，行为零差异）

**改造点**（自由流程与子流程的任务读源翻源，按**任务读源** `task-read-source` 分流）：

| # | 位置 | 原读源 | ACT 读源 / 处理 | 性质 |
|---|---|---|---|---|
| 1 | `WfTaskServiceImpl.page` / `list` 的 ACT 分支（待办/已办 VO 组装） | 列表 `nodeName` 取 `ACT.NAME_`（引擎节点名） | 新增 `decorateFreeFlowNames(candidates)`：按候选 id 批量取 `wf_task.ext_json.nodeName` 覆盖步骤名，与 wf 回退路径 `toVO` 的 `freeDisplayName` 同口径 | **列表显示名回填**（ACT 无该列） |
| 2 | `WfSubflowServiceImpl.resolveMainNodeOperator:153` | `wf_task` 取节点最近办结人（提醒接收人） | `if (actRead()) taskActReader.nodeOperatorFromAct(mainInstId, nodeKey)`；否则回退 `taskMapper` | 任务读翻源 |
| 3 | `WfFreeFlowDriver:145/157/184`（`appendExtraAssignees` / `stampTaskExtJson`） | `wf_task` 查首人样例 / 查重 / stamp 步骤名 | **保留 wf_task**（写侧） | 非列表读源，不翻 |

**为何 `WfFreeFlowDriver` 的 3 处保留**：它们是自由流程**推进写操作**的支撑读（补并列待办、查重、把步骤显示名写入
`wf_task.ext_json` 与 `wf_free_node`），读的正是由自由流程自己创建的 `wf_task` 行；这些行的 `ext_json` / 并列待办是 blade 特有、
ACT 无对应列（§7.10 已定案：blade 扩展任务以 `wf_task` 为权威存储）。翻到 ACT 无意义，且 §7.11 已决策 `wf_task` 不退役，
故保留作写侧数据源。

**为何 `decorateFreeFlowNames` 不是「又把 wf_task 当列表源」**：它只读 `wf_task.ext_json` 的 `nodeName`（blade 特有的显示名扩展列，
按候选 id 一次 `IN` 批量取），用于覆盖 VO 的显示名；列表的**行来源 / 计数 / 分页**仍是 ACT。这与 §7.10/§7.11「blade 扩展列允许回读」
一致，且对非自由流程任务 `ext_json` 为空、覆盖不生效 → **对默认路径零行为变化**（`blade.workflow.free-flow.enabled` 默认 `false`）。

**新增读器方法**（`WfTaskActReader#nodeOperatorFromAct(Long bladeInstId, String nodeKey)`）：经
`ACT_HI_TASKINST h JOIN ACT_HI_PROCINST p ON p.ID_=h.PROC_INST_ID_`、`p.BUSINESS_ID_=bladeInstId`、
`h.TASK_DEF_KEY_=nodeKey`、`h.BUSINESS_STATUS_='DONE' AND h.END_TIME_ IS NOT NULL`，按 `END_TIME_` 倒序取首条
`COALESCE(BIZ_ASSIGNEE_, ASSIGNEE_)`，等价于 wf 口径「同实例同节点 status=DONE、operate_time 倒序首条 assignee」。

**口径一致性**：`nodeOperatorFromAct` 与 wf 分支同一实例同一节点同一「最近办结」语义；`decorateFreeFlowNames` 与 `toVO` 的
`freeDisplayName` 同源（`freeDisplayName` 解析逻辑完全相同）。开关=wf 或 ACT 取不到时回退 wf_task → 与改造前完全一致。

**编译验证**：`mvn -o -pl blade-service/blade-workflow compile -DskipTests` → BUILD SUCCESS（exitCode 0，无 ERROR）。

**验证限制（务必知悉）**：自由流程与子流程提醒均为**特性开关默认关闭**（`free-flow.enabled=false`；子流程提醒随节点高级设置开启），
dev 实测环境未编排自由流程实例、无子流程提醒触发，故本次为**静态/编译级验证 + 口径同源论证**，未能端到端跑通自由流程待办/已办与子流程提醒。
生产环境若启用这两类特性，须按本记录口径造数回归：① 自由流程实例的待办/已办列表 `nodeName` 是否显示步骤名（非引擎节点名）；
② 子流程全部归档后「节点操作者本人」提醒是否命中正确接收人。

### 7.21 ③ 第 9 项：操作链路任务定位与 ID 语义 → 设计内覆盖（2026-11-08 核验，无需改造）

**核验结论**：第 9 项所担心的「翻源后操作链 ID 语义崩溃」在现行架构下**不会发生**，原因如下（与 §7.11「wf_task 不退役」决策自洽）：

1. **列表恒回 `wf_task.id`**：`WfTaskActReader.todoPageFromAct/donePageFromAct` 组装 `WfTaskVO` 时 `vo.setId(bizTaskId)`，
   其中 `bizTaskId = ACT.BIZ_TASK_ID_ = wf_task.id`（见 `WfTaskActReader` 类注释「id 语义」段）。即**无论读源=act 还是 wf，
   列表吐出的任务 id 永远是 `wf_task.id`**。
2. **操作链按 `wf_task.id` 解析**：`WfTaskServiceImpl.resolveTask:1510` 先 `taskMapper.selectById(taskId)`（把入参当 wf_task.id），
   命中即返回；仅当 wf_task.id 查不到（典型=消息中心存的是 `engineTaskId`）才回退 `engine_task_id` 反查（:1516）。
   所有 `/task/{id}/approve|reject|forward|add-sign|circulate|urge|view|detail` 入口都经 `requireTodoTask`/`resolveTask` 走此解析。
3. **`wf_task` 保留为任务 id 权威存储**（§7.11）：行不会被删、id 不会失效 → 第 1/2 点的 `wf_task.id` 解析永远成立。

**结论**：翻源（用户侧待办/已办列表读 ACT）**不改变操作链的 ID 语义**——它本来就以 `wf_task.id` 为契约，而 `wf_task` 不退役。
文档 §7.12 原先标注的「改全链路 ID 语义 / 保留薄映射表」是**假设 wf_task 会被物理退役**时的方案；该退役已在 §7.11 暂缓，
故本项**无需改造**。

**消息中心 `engineTaskId` 坑**：消息中心若存的是 `engineTaskId`（而非 `wf_task.id`），`resolveTask` 的 `engine_task_id` 回退（:1516）仍能兜住，
与改造前完全一致 → 零行为差异。

**重启退役时才需处理**：若未来执行 §7.11 的「停写 wf_task / 删表」，则第 9 项必须落地（届时列表需改吐 ACT 任务 id、全链路 ID 语义切换、
或建 `BIZ_TASK_ID_↔ACT_TASK_ID_` 映射表）。当前不退役 → 本项封闭。

### 7.22 ③ 第 10 项：签章流 + 审批意见双写 → 设计内覆盖（2026-11-08 核验，无需改造）

**核验结论**：第 10 项两处均为**写侧读**，依赖保留的 `wf_task` 取 `engineTaskId` 去定位引擎任务，翻源（用户侧列表读 ACT）**不影响**它们：

| # | 位置 | 读源 | 翻源影响 |
|---|---|---|---|
| 1 | `WfWriteHelper.syncCommentToEngine:311` | `taskMapper.selectById(wfTaskId)` 取 `engineTaskId`，再 `processService.addComment(engineTaskId, procInstId, ...)` 双写 `ACT_HI_COMMENT` | 无（写侧，wf_task 保留；`wfTaskId` 入参即 wf_task.id） |
| 2 | `OdocSealAdvancer.terminate:170` | `taskMapper.selectById(flow.getTaskId())` 取业务待办恢复为 TODO | 无（写侧，`flow.getTaskId()` 即 wf_task.id） |

**一致性**：两处入参都是 `wf_task.id`（与第 9 项同源），经保留的 `wf_task` 解析 `engineTaskId` 去驱动引擎写操作。
翻源只改变了「用户侧列表从哪读」，不改变「写侧如何定位引擎任务」——二者解耦（这正是 §7.5「开关不管写」与 §7.11 的设计意图）。

**重启退役时才需处理**：若未来执行 §7.11 的 wf_task 退役，则这两处须改为用 `BIZ_TASK_ID_` 反查 ACT 取 `engineTaskId`
（`WfTaskActReader` 已有 `toWfTaskByBizIdFromAct` 可复用其思路；或直接按 `ACT_RU_TASK/ACT_HI_TASKINST.BIZ_TASK_ID_=wfTaskId` 取 `ID_`）。
当前不退役 → 本项封闭。

---

## 8. ③ 收尾结论（2026-11-08）

**任务读源翻源清单 10/10 全部达成**：

- **真正改代码翻源（用户侧读点）**：① 监控超时（§7.13）、② 已办节点集（§7.14）、③ 监管关待办（§7.15）、④ canVisible 半翻源（§7.16）、
  ⑤ 超时 job 扫描（§7.17，P3-6 已落地）、⑥ 公文待办/已办（§7.18）、⑧ 自由流程/子流程（§7.20，含 ACT 列表补步骤名 + 子流程办结人翻源）。
- **核验无需改造（设计内覆盖，wf_task 保留为任务 id 权威存储）**：⑦ form-render/节点操作者回退分支（§7.19）、⑨ 操作链 ID 语义（§7.21）、⑩ 签章/意见双写（§7.22）。

**读源翻源的总目标**——用户侧待办/已办列表与角标从 ACT 读取、与 wf_task 零漂移——已由 ①~⑥ 与 ⑧ 完整实现，
并经 dev 端到端实测（§7.9：待办/已办/角标 100% 来自 ACT，深页零漏行，状态标签正确）。⑦⑨⑩ 因 §7.11 决策「wf_task 不退役」而无需翻源，
与双源设计自洽。

**剩余工程（非本次范畴）**：⑤(§7.11) 停写 wf_task / 删表 / DROP —— 重启条件未达（blade 扩展任务迁独立表、合成待办替代载体、6 态权威存储切换），
届时 ⑨⑩ 与 §7.11 的 N:1 守卫需一并收口。

### 7.23 ③ 端到端实测（dev，2026-11-08，act 读源）

**环境**：gateway=81、blade-workflow=8111 在线；登录 `admin/ant.design/000000`（user_id=676，dept=2000000121），
经 `springblade-dev-login` 技能拿 `sword-token` 调网关鉴权接口。`task-read-source` 当前=act（翻源生效）。

**实测接口（经网关 81，blade-workflow）**：

| 接口 | 返回 total | 数据来源核对 |
|---|---|---|
| `/task/todo` | 11（data 数组） | ids 集合 **==** `wf_task(assignee=676,status=0,is_test=0,is_deleted=0)` 的 11 个 id → **零漂移** |
| `/task/done` | 96 | ids 集合 = `wf_task(status∈{2,4})` 的 94 个 **+ 2 个 ACT-only 已办**（见下） |
| `/odoc/todo` | 4 | ids 集合 **==** 公文维度 `wf_task(status=0)` 的 4 个 → **零漂移** |
| `/odoc/done` | 42 | ids 集合 **==** 公文维度 `wf_task(status∈{2,4})` 的 42 个 → **零漂移** |

- 列表 `nodeName` 正常（`部门主管审批`/`财务审核会签`），`isTest=0`（测试态已过滤），`title` 正常。
- `/odoc/todo`/`/odoc/done` 的 id 集合与 §7.18 的 SQL 层 act/wf 口径（4/42）**逐 id 一致**，确认第 6 项公文翻源零漂移。

**DONE 的 2 个差异（正面修正，非回归）**：接口 DONE 比 `wf_task(assignee=676)` 多 2 个 id
`2117991653013786626`、`2118125414175019009`。核查：

- `wf_task` 中**无这两行**（台账缺失）；`ACT_HI_TASKINST` 中 `BIZ_ASSIGNEE_=676`、`BUSINESS_STATUS_='DONE'`、`END_TIME_` 正常。
- 对应引擎实例 `440005`/`440128` 的 `wf_instance` 也无记录（`BUSINESS_STATUS_='APPROVED'`，2026-11-04/05 的已归档流程）。

即这 2 个任务是**引擎有已办、但 blade 双写台账（wf_task/wf_instance）历史上漏建**的旧流程。翻源（ACT 读源）让这 2 个已办「失而复得」——
属于用 ACT 权威源**修正台账缺失**，恰是翻源目标之一；开关=wf 时它们因台账无行而不可见。结论：读源翻源**未引入漂移**，反而补全了 2 条历史台账缺口。

**后续清理（2026-11-08）**：经确认这 2 个为孤儿脏数据（无 blade 台账、纯引擎残留），已从 ACT 引擎历史表删除 46 行
（`act_hi_identitylink:2 / act_hi_comment:6 / act_hi_varinst:20 / act_hi_taskinst:4 / act_hi_actinst:12 / act_hi_procinst:2`，
删除前已备份至 `cleanup_backup.json`）。删除后复测 `/task/done` → **94 条**，与 `wf_task(assignee=676,status∈{2,4})` 的 94 个 id **逐 id 一致**，
读源翻源至此**零漂移闭环**（TODO/ODOC_TODO/ODOC_DONE/DONE 全部对齐 wf_task 台账）。

**未覆盖项（已知限制）**：
- 第 8 项自由流程/子流程：特性默认关闭（`free-flow.enabled=false`；子流程提醒随节点设置），dev 无实例，未端到端跑通（静态/编译级验证见 §7.20）。
- 角标：前端取 `/task/todo` 的 `total`（已验证从 ACT 读），无需单测。
- `/task/mine`（我的请求，含草稿）：路径应为 `/instance/mine`，本次未纳入核对（草稿合并逻辑见 memory `52355684`，与读源翻源正交）。

---

## 9. ⑤ 重启：wf_task 退役 + 扩展任务独立承载（具体方案 · 2026-11-08 起草）

> 背景：`③ 读源翻源` 已让**普通审批任务**的列表/角标/计数 100% 来自 `ACT_*`，但仍有 3 类读点依赖 `wf_task`
> （混合分页扩展任务补齐 / `countFromAct` 残留 / `decorateFreeFlowNames` 步骤名）。要使"连扩展任务也彻底不读 wf_task"，
> 必须先把 §7.11 的退役重启条件（① 扩展任务迁独立表或 `ACT_*` 承载列 ② 合成待办替代载体 ③ 6 态权威存储切换）落地。
> 本章给出**可分步执行、每步可独立回滚**的具体方案。

### 9.1 目标与边界

| 项 | 目标 |
|---|---|
| 读侧 | 列表（待办/已办/公文）、角标、监管关待办、监管已办节点集 —— **零 `wf_task` 读取** |
| 写侧 | `WfTaskActWriter` 不再双写 `wf_task`；`forward/addSign/circulate/patch` 不再 `INSERT wf_task` |
| 操作链 | `resolveTask` 不再以 `wf_task.id` 为唯一契约；`WfWriteHelper`/`OdocSealAdvancer` 用 `BIZ_TASK_ID_` 反查 |
| 终态 | 删除 `WfTask` Entity/Mapper/Service 中退役相关引用，最终 `DROP wf_task`（保留历史备份） |

**非目标**：不动引擎事实源（`ACT_*` 仍是引擎任务本体权威）。6 态仍按 §7.10 已定案分流，不塞进 `ACT_*`。

### 9.2 现状精确盘点（四类语义 + 依赖点）

`wf_task` 当前实际承载 4 类行（对应 §7.10/§7.11）：

| 类 | 行特征 | 引擎事实 | 写入点 | 退役后归属 |
|---|---|---|---|---|
| **A 引擎任务台账** | `engine_task_id` 有值、`status∈{0,2,4,6}` | 有对应 `ACT_RU_TASK`/`ACT_HI_TASKINST` | `WfTaskActWriter.sync`（doApprove/reject/advance 经它） | ACT 已是权威 → **停写 wf_task 行** |
| **B1 协办/加签协办式** | 多人共享同一 `engine_task_id`（`addSign`/`forward` 沿用原 `engineTaskId`） | N:1，**ACT 单行表达不了** | `addSign`(796)/`forward`(766) | 改为 ACT 原生多实例/候选人，或 `wf_ext_task` 承载协办办理态 |
| **B2 抄送/传阅/已读** | `status∈{8,11,已读}`、`engine_task_id` 沿用 | 无（纯 blade 知会） | `circulate`(831) / `patch`(896) | `wf_ext_task`（或 `ACT_*_IDENTITYLINK` + 已读标记） |
| **C 合成待办** | `engine_task_id` 空 | 无 | 草稿（已迁 `wf_instance`）、退回发起人重提交（新模型走真实 ACT 任务，**仅存量老数据**） | `wf_instance`（草稿）+ 真实 ACT 任务（退回） |

**依赖 `wf_task` 的代码点（须逐个改造）**：
- 读：`WfTaskActReader` 残留 SQL(445/477)、`countFromAct` 残留段、`decorateFreeFlowNames`(1310)、混合分页 `wfTaskQuery(extOnly)`。
- 写：`WfTaskActWriter` N:1 守卫(110 `SELECT COUNT(*) FROM wf_task WHERE engine_task_id=?`)、`forward/addSign/circulate/patch` 的 `taskMapper.insert`。
- 操作链：`resolveTask`(1514)、`WfWriteHelper.syncCommentToEngine`(314)、`OdocSealAdvancer.terminate`(170)、`WfFreeFlowDriver`(145/157/184)。

### 9.3 载体设计

**核心载体：新建 `wf_ext_task` 表**（承载 B1/B2 类 + C 类残量），字段建议：

```
wf_ext_task
  id              BIGINT PK (雪花，沿用 wf_task.id 语义，避免 ID 契约大改)
  inst_id         BIGINT      关联 wf_instance.id
  engine_task_id  VARCHAR     关联原审批引擎任务(BIZ_TASK_ID_ 对应的 ACT 任务)，B1 共享、B2 沿用
  node_key        VARCHAR     节点
  type            TINYINT     1=协办 2=加签协办 3=抄送 4=传阅 5=已读 6=退回重提交
  assignee        BIGINT      接收人
  original_user   BIGINT      原办理人(协办/转办回溯用)
  title           VARCHAR
  opinion         VARCHAR     抄送/传阅意见
  status          TINYINT     0=待办/待阅 1=已办/已读 2=已拒 (按 type 解释)
  ext_json        VARCHAR     自由流程步骤名等 blade 扩展显示列(替代 wf_task.ext_json)
  is_test         TINYINT
  is_deleted      TINYINT
  tenant_id       BIGINT
  create_time / operate_time / receive_time
```

**为何独立表而非复用 `ACT_*_IDENTITYLINK`**：
- 抄送/传阅历史需持久：`ACT_RU_IDENTITYLINK` 任务完成后被清理（§7.10 已记），抄送历史权威只能落 `ACT_HI_IDENTITYLINK` 或 blade 侧。
- 协办/加签**有办理态**（协办人可办理并回流），identityLink 只表达"关联"不表达"办理"，`wf_ext_task.status` 才能承载协办办理态。
- 与引擎解耦：blade 扩展语义演进不受 Flowable 身份模型约束。

### 9.4 N:1 硬门槛的消除（B1 类最难，决定退役可行性）

`WfTaskActWriter:110` 的 N:1 守卫存在，是因为 `addSign`/`forward` 用"同一 `engine_task_id` + 多条 `wf_task` 行"表达"同一节点多人办"。
**退役前提 = 让 N 人办同一节点在 ACT 原生表达**，二选一：

- **方案 X（推荐）：改用 Flowable 多实例/候选人。** 协办、加签协办式统一建模为节点的 candidate/participant 或 MI 集合，
  ACT 原生支持 N 人、N 行 `ACT_RU_TASK`/`ACT_HI_TASKINST` → 守卫自然消除（新会签已是 1:1，见 §7.10 注释）。代价：改 `addSign`/`forward` 的引擎调用（改 `processService` 加候选人/启 MI），非纯数据迁移。
- **方案 Y（最小改造）：B1 迁 `wf_ext_task` 承载"协办办理态"。** 加签/协办仍由 blade 创建 `wf_ext_task` 行（type=1/2），
  引擎侧保持单列（或仍用现有 N:1 守卫但改查 `wf_ext_task`）。代价：N:1 守卫从 `wf_task` 挪到 `wf_ext_task`，未根治但解耦了退役表。

> 推荐 X：它把"N 人同节点"回归引擎事实源，与 §7.10「引擎任务本体→ACT」定案自洽，且让 `WfTaskActWriter` 的 N:1 守卫可整体删除（A 类停写后无需该守卫）。
> 若工期紧，可先用 Y 让 wf_task 退役、N:1 守卫迁 `wf_ext_task`，X 作为后续优化。
>
> ⚠️ **【§10.7 修正】** 本节"**A 类停写后**无需该守卫"的前提**不成立**：A 类台账必须保留写入
> （是 ACT 业务列与 `BIZ_TASK_ID_` 的唯一来源，见 §10.7 决策 1）。2026-11-09 实际落地为
> **方案 Y 的保守变体**：N:1 守卫保留，但统计源改为 `wf_task` + `wf_ext_task` 两表合并
> （排除 type=7 装饰行），避免扩展任务迁出后守卫失效。本次不改引擎调用方式。

### 9.5 6 态权威存储切换（落地 §7.10 定案）

| 行类别 | 权威存储（退役后） | 列表读取 |
|---|---|---|
| 引擎任务本体（含 forward 改派、正常审批、加签产生的真实子任务） | `ACT_RU_TASK`/`ACT_HI_TASKINST` | ACT（已落地） |
| 协办/加签协办式 | `wf_ext_task`（方案 X 下部分回归 ACT MI） | `wf_ext_task` |
| 抄送/传阅/已读 | `wf_ext_task` | `wf_ext_task` |
| 草稿 | `wf_instance` | 「我的请求」（方案 A 已移出待办） |
| 退回发起人重提交 | 真实 ACT 任务（新模型） | ACT |

### 9.6 分阶段迁移（每阶段可独立灰度/回滚）

- **阶段 0 · 双写校验**：① 新建 `wf_ext_task` 表 + Entity/Mapper；② `circulate/addSign/forward/patch` 在写 `wf_task` 的同时写 `wf_ext_task`（兼容双写）；
  ③ 开启已预埋的 `ccIdentityLinkEnabled`（`circulate` 双写 ACT identityLink，仅预热）；④ 跑校验任务比对两表 B 类行一致。读仍走 `wf_task`。
- **阶段 1 · 读切换**：列表/角标/混合分页残留/`countFromAct` 残留从 `wf_task` 切到 `wf_ext_task`（精确判据 `type IN (协办,抄送,传阅,已读)`）；`decorateFreeFlowNames` 改读 `wf_ext_task.ext_json`。ACT 段不变。灰度验证零漂移。
- **阶段 2 · 写切换（A 类停写）**：`WfTaskActWriter.sync` 对 A 类不再写 `wf_task` 行（ACT 已是权威）；`forward` 改调 `processService` 改 ACT assignee（方案 X）。B 类只写 `wf_ext_task`（停写 `wf_task`）。
- **阶段 3 · N:1 根治（可选，对应 §9.4 X）**：`addSign`/协办改为 ACT 多实例/候选人，`wf_ext_task` 中 B1 类逐步清零，N:1 守卫删除。
- **阶段 4 · 操作链/ID 语义（第 9 项重启）**：`resolveTask` 按 `wf_ext_task.id` 或 `engineTaskId/BIZ_TASK_ID_` 解析；列表改吐统一任务 id 契约；
  `WfWriteHelper`/`OdocSealAdvancer` 用 `BIZ_TASK_ID_` 反查 ACT 取 `engineTaskId`（§7.22 已规划）。
- **阶段 5 · 历史数据迁移 + 收口**：存量 `wf_task` 的 B/C 类行迁 `wf_ext_task`（A 类行标记废弃不读）；删除 `WfTask` Entity/Mapper/Service 退役相关；最终 `DROP wf_task`（备份留存）。
  开关 `task-residue-merge.enabled` 语义随之变为"是否读 `wf_ext_task` 扩展任务"（等价于现在控制 wf_task 扩展）。

### 9.7 关键改造点清单（具体到方法）

| 模块 | 方法/位置 | 改动 |
|---|---|---|
| `WfTaskServiceImpl` | `forward`(766)/`addSign`(796) | 停写 `wf_task`；改 ACT assignee/MI（方案 X）或写 `wf_ext_task`（方案 Y） |
| | `circulate`(831)/`patch`(896) | 停写 `wf_task`，写 `wf_ext_task`（+ 已预埋 identityLink） |
| | `decorateFreeFlowNames`(1310) | `taskMapper` → `extTaskMapper` 读 `ext_json` |
| `WfTaskActReader` | 残留 SQL(445/477)、`countFromAct`(445/477) | `FROM wf_task` → `FROM wf_ext_task`，精确 `type` 判据 |
| | 混合分页 `wfTaskQuery(extOnly)` | `status IN(7,8,11)` → `wf_ext_task.type IN(...)` |
| `WfTaskActWriter` | N:1 守卫(110) | A 类停写后删除；或改查 `wf_ext_task`（方案 Y） |
| `WfTaskServiceImpl` | `resolveTask`(1514) | 解析契约改 `wf_ext_task.id` / `BIZ_TASK_ID_` |
| `WfWriteHelper` | `syncCommentToEngine`(314) | `BIZ_TASK_ID_` 反查 ACT 取 `engineTaskId` |
| `OdocSealAdvancer` | `terminate`(170) | 同上 |
| `WfFreeFlowDriver` | (145/157/184) | 自由流程步骤名/并列待办写 `wf_ext_task` |

### 9.8 风险与核对点

1. **协办办理态回流**：协办人"办理"后如何推进原任务？需 `wf_ext_task` 承载协办完成 → 触发原 ACT 任务流转；方案 X 下协办即 ACT 候选人，天然回流。
2. **抄送/传阅"已读"**：需 `wf_ext_task.status=已读` 或独立 `wf_read_log`；前端"未读红点"依赖此状态，迁移不能丢未读标记。
3. **历史数据迁移完整性**：存量 `wf_task` 行分类迁 `wf_ext_task`，须与"零漂移"原则一致（迁后列表 id 集合不变）；阶段 0/1 双写校验 + 阶段 5 迁后比对。
4. **N:1 守卫时序**：阶段 2 A 类停写后守卫(110) 必须先于 `DROP` 处理，否则存量 N:1 加签数据在阶段 0 双写时漏迁 `wf_ext_task` → 阶段 1 读切换丢行。
5. **回归面**：协办/抄送/传阅/转办/加签/退回重提交 6 条国产 OA 语义全要造数回归；角标与列表数字必须再次一致（§7.10 教训：裸 SQL 必带 `is_deleted=0`/租户）。

### 9.9 与 §7.10/§7.11 的关系

- §7.11 重启条件 ①③ 在本章给出落地路径；② 合成待办替代载体（草稿→`wf_instance`、退回→真实 ACT 任务）在新模型下已大半具备，仅存量老数据需阶段 5 迁移。
- 完成本章即达成"连扩展任务也彻底不读 wf_task"，与 `③ 读源翻源` 的"纯 ACT 普通任务"形成完整闭环。

**回归**：服务 ERROR=0；接口层 200；todo/odoc 列表 id 集合与 wf_task 台账逐 id 一致（除 2 个历史台账缺口被 ACT 补全）。

---

## 10. ⑤ 重启落地（一次性切换版 · 2026-11-08 按用户指令调整）

> 用户指令：① 无需双写，一次性完成；② 删除现有数据迁移功能，不再保留；③ 清空旧数据并重新创建，生成大量新测试数据验证；先出方案。
> 相对 §9（渐进式：双写校验 + 存量迁移）的**调整**：跳过 §9 阶段0 双写校验、阶段5 存量迁移，改为"清空旧数据 + 脚本造大量新数据"直接验证。

### 10.1 迁移功能标记弃用（不删除，统一 `@Deprecated`）

> 按用户指令：**代码不物理删除，保留可追溯**，仅加 `@Deprecated` 注解 + 注释说明弃用原因/日期。

| 目标 | 位置 | 处理 |
|---|---|---|
| 迁移端点 | `WfMigrationController`（`/migration` backfill/reconcile） | 加 `@Deprecated` + 注释「已弃用：wf_task 退役(2026-11-08)，双写/回填/对账不再需要；保留仅供追溯」；端点保留但标注不可用 |
| 迁移实现 | `WfDefinitionBackfillJob`（backfillAll/reconcileAll） | 加 `@Deprecated`；若含「流程定义初始化」依赖则保留该方法、仅标记迁移方法弃用 |
| 双写校验开关 | `circulate` 里的 `ccIdentityLinkEnabled` 阶段3 双写校验（§9.4 X 路径预留） | 标记 `@Deprecated`，默认不触发（保留开关位，不再启用） |

### 10.2 代码改造清单（一次性，不双写；旧路径 `@Deprecated` 不删）

> **弃用标记总原则**：所有被新逻辑替代的 `wf_task` 读写路径**保留代码、加 `@Deprecated` 注解 + 注释（弃用原因/日期）**，不物理删除。新逻辑走 `wf_ext_task`；旧 `wf_task` 分支标注后不再被调用（或短路为 no-op 并注释）。

1. **建表**：`doc/sql/blade/blade-2026-11-09-wf-ext-task.sql` 建 `wf_ext_task`（含 `biz_task_id` 宿主关联列 +
   `idx_assignee_type_status`/`idx_inst_node`/`idx_engine_task`/`idx_biz_task_type` 四个索引）；
   新增 `WfExtTask` 实体（extends `TenantEntity`）与 `WfExtTaskMapper`。已在 dev 执行。
2. **扩展任务写**：`circulate`(抄送) 与 `WfInstanceServiceImpl#createCoadjutantTasks/closeCoadjutantTasks`(协办)
   改为写 `wf_ext_task`；旧 `wf_task` 抄送写入抽成 `@Deprecated` 方法 `insertCirculateToWfTaskDeprecated` 保留。
   ⚠️ `forward`/`addSign`/`markViewed` **不迁**（A 类可办理待办，见 §10.7 决策 3）。
3. **读源切换**：`WfTaskActReader#countFromAct` 两段残留 SQL 与 `WfTaskServiceImpl` 的
   `extTaskQuery`/`countResidue`/列表兜底合并统一改读 `wf_ext_task`，判据为 `type` 集合；
   两侧严格同口径（角标=列表）。`decorateFreeFlowNames` 改按 `biz_task_id` 读 `wf_ext_task.ext_json`。
   `wfTaskQuery(extOnly=true)` 分支与 `EXT_TASK_STATUSES` 标 `@Deprecated` 保留。
4. **写侧收口**：`WfTaskActWriter` 的 N:1 守卫保留（A 类台账必须写），统计源改为
   `wf_task` + `wf_ext_task` 两表合并（排除 type=7 装饰行）；`detail`/`markViewed` 新增
   `resolveExtTask` 分流支持扩展任务 id；`WfWriteHelper#syncCommentToEngine` 改用
   `WfTaskActReader#findEngineTaskIdByBizId` 按 `BIZ_TASK_ID_` 反查 ACT（wf_task 台账兜底）。
   ⚠️ `OdocSealAdvancer:170` **未改**（见 §10.7 决策 4）。
5. **自由流程**：`WfFreeFlowDriver#stampTaskExtJson` 把步骤显示名写入 `wf_ext_task` 的
   `type=7` 装饰行（`biz_task_id` = 宿主 `wf_task.id`）；并列待办本身仍写 `wf_task`（A 类可办理）。
6. **弃用标记**：`WfMigrationController`、`WfDefinitionBackfillJob`、`ccIdentityLinkEnabled`
   加 `@Deprecated`；`WfTaskMapper`/`WfTask`/`WfTaskResidueProperties` 注释更新职责边界。

### 10.3 数据重置（清空 + 造大量新测试数据）

- **清空（破坏性，dev `127.0.0.1:3306` 库 `blade`）**：`ACT_*`（ACT_RU_*/ACT_HI_*/ACT_RE_*/ACT_GE_* 全清）+ `wf_instance` + `wf_task`；`wf_ext_task` 新建后为空。
- **造数（Python/SQL 脚本直接 INSERT）**：生成**大量**流程，覆盖场景——
  - 普通待办 / 普通已办 / 办结；
  - **会签 N:1**（同一 `engine_task_id` 多人，验证 N:1 守卫删除后无碍）；
  - 子流程；
  - **扩展任务**：协办 / 抄送 / 传阅 / 已读 / 退回重提交（写入 `wf_ext_task`）；
  - 须保证 `ACT_RU_TASK`/`ACT_HI_TASKINST`/`ACT_HI_PROCINST`/`ACT_HI_IDENTITYLINK` 多表一致性（BIZ_TASK_ID_/BUSINESS_ID_ 关联正确、IS_TEST_ 标记继承）。
- **自由流程场景**：临时开启 `free-flow.enabled=true` 造数，验证 `ext_json` 迁 `wf_ext_task` 后 `nodeName` 装饰正常。

### 10.4 验证计划

| 项 | 预期 |
|---|---|
| 待办/已办/公文列表 | 从 ACT 读，与造数一致，零漂移 |
| 角标 | 与列表数字一致（§7.10 教训：裸 SQL 必带 `is_deleted=0`/租户） |
| 扩展任务 | 协办/抄送/传阅/已读正确出现在列表与角标 |
| 6 条国产 OA 语义 | 协办/抄送/传阅/转办/加签/退回重提交造数全回归 |
| 自由流程 | `nodeName` 正常（ext_json 迁 wf_ext_task 生效） |

### 10.5 风险与执行顺序

1. **破坏性清空**：dev 共享库，执行前**二次确认范围**（是否含 `ACT_*` 全清、是否影响他人数据）。
2. **造数保真**：脚本 INSERT 须手动保证 ACT 多表一致性（尤其 N:1 会签、`identityLink`），否则读源翻源会漏行/双计。
3. **执行顺序**：建 `wf_ext_task` 表 → 改代码（旧路径 `@Deprecated`）并编译通过 → 清空旧数据 → 跑造数脚本 → 启动服务 → 调接口验证（§7.23 同法：登录 admin 经网关调 `/task/todo`/`/task/done`/`/odoc/*` + 角标）。
4. **N:1 守卫保留**：`WfTaskActWriter` 守卫(110) **不弃用**，统计源改为 `wf_task` + `wf_ext_task` 合并
   （见 §10.7 决策 1/2）；A 类双写继续生效。
5. **弃用标记可溯**：所有 `@Deprecated` 旧路径保留，便于回滚/追溯；确认长期稳定后（P6 终态）再考虑物理删除。

### 10.6 ext_json 影响（见前述分析）

- **任务级** `wf_task.ext_json`（仅自由流程写自定义步骤名）迁 `wf_ext_task.ext_json`，`decorateFreeFlowNames` 改查 `wf_ext_task`，查询逻辑不变、不受影响。
- **`WfProcessNode.ext_json`**（节点定义：operateMenu/reject/timeout 等，大量使用）与本次无关、**不受影响**——勿混淆两张表。
- 自由流程默认关闭，短期零实际影响；造数时开启验证即可。

### 10.7 关键决策修正（实施期核对代码后定案 · 2026-11-09）

> 计划（§9/§10 早期草稿）有三处与代码现实不符，实施时按**不破坏功能 + 零漂移**原则修正，记录如下。

#### 决策 1：A 类台账**不可停写**（修正 §9.2/§10.2 的「A 类停写」表述）

`WfTaskActWriter.sync` 把 `wf_task` 行的 `status/is_test/sign_order/view_time/timeout_handled/
original_user/assignee/due_time` 投影到 `ACT_RU_TASK`/`ACT_HI_TASKINST` 自定义列，
并把 `wf_task.id` 写成 `BIZ_TASK_ID_`——后者是**列表 VO 的 id** 与**操作链**
（`approve`/`forward`/`reject` → `requireTodoTask` → `taskMapper.selectById`）的唯一契约。

若停写 A 类：ACT 自定义列断供 → 列表 `status`/`assignee` 为空、角标为 0、办理报「任务不存在」。
故本次 `wf_task` **职责收敛为「仅引擎任务本体台账」，继续写入**；达成的是
「**扩展任务**不再读/写 wf_task」，而非 `wf_task` 物理退役。彻底退役需另立
「创建引擎任务时直写 ACT 业务列 + 全链路 ID 语义切换」的 P6 后续改造。

#### 决策 2：`type` 分类 + 独立 `status`，VO 映射回原 6 态

`wf_ext_task.type` 是不可变语义类别，`status` 是行自身状态（0 待处理/未读、1 已处理/已读、2 已拒绝）；
`WfExtTask#toWfTaskStatus()` 负责映射回原 `WfTask.status` 语义，**前端零改动**：

| type | 含义 | 原 `WfTask.status` | 列表段 | 写入点 |
|---|---|---|---|---|
| 1 | 协办 / 征询意见 | 7 | **已办** | `WfInstanceServiceImpl#createCoadjutantTasks` |
| 2 | 加签协办（可办理） | 0 待办 / 2 已办 | 待办 | 预留（当前 `addSign` 仍写 A 类） |
| 3 | 抄送 | 8 | **已办** | `WfTaskServiceImpl#circulate` |
| 4 | 传阅 | 11 | **已办** | 预留 |
| 5 | 已读 | 11 | **已办** | `markViewed` 扩展任务分支 |
| 6 | 退回发起人重提交 | 0 待办 / 2 已办 | 待办 | 预留（新模型走真实 ACT 任务） |
| 7 | 自由流程步骤名**装饰行** | — | **不进列表** | `WfFreeFlowDriver#stampTaskExtJson` |

> ⚠️ 协办是 `status=7`，落在 `done()` 的 statuses（2,4,6,7,8,11）→ **已办段**。
> 早期草稿误把待处理协办映射到 0（待办段），已修正，否则会漂移。
> `type=7` 装饰行不是列表条目，`isTodoType/isDoneType`、两段残留 SQL、`extTaskQuery`
> **全部显式排除**，避免多算行。

**读源两段判据（角标 `countFromAct` 与列表 `extTaskQuery` 严格同口径）**：
- 待办段：`type IN (2,6) AND status = 0`；
- 已办段：`type IN (1,3,4,5)`（未读也进已办侧，与原 status=7/8/11 行为一致）；
- 两段都带 `is_test=0` + `is_deleted=0` + 租户条件 + 草稿实例排除。

#### 决策 3：`forward`/`addSign`/`markViewed`/自由流程并列待办**不迁**（A 类）

这四类产生的都是 `status=0` 且 `engine_task_id` 非空的**可办理引擎任务本体**：
- `approve` → `requireTodoTask` → `resolveTask` 按 `wf_task.id` 定位，迁走会断办理链；
- `WfTaskActWriter.sync` 需要它们把业务列投影到 ACT。

按 §7.10.1 精确判据（`status IN (7,8,11) OR engine_task_id 为空`），它们本就**不在扩展段**，
从 ACT 段读取。故保留在 `wf_task`。真正迁出的是 `circulate`(8)、协办(7) 与步骤显示名列。

#### 决策 4：`OdocSealAdvancer:170` 无需改

该处 `taskMapper.selectById(flow.getTaskId())` 只是把 A 类台账状态恢复为待办（`status=TODO`），
**不取 engineTaskId**；签章流的引擎任务ID来自 `wf_odoc_sign_flow.engine_task_id` 快照
（创建时由 `flow.setEngineTaskId(task.getEngineTaskId())` 写入）。计划中「改用 `BIZ_TASK_ID_`
反查 ACT」的表述不适用，保持原样。仅 `WfWriteHelper#syncCommentToEngine` 真正需要
`engineTaskId`，已改为 `findEngineTaskIdByBizId` 反查（wf_task 台账兜底）。

#### 决策 5：`biz_task_id` 关联列（新增）

自由流程步骤显示名附着在**可办理 A 类待办**上，而 `decorateFreeFlowNames` 只有
`WfTaskVO.id`（= `BIZ_TASK_ID_` = `wf_task.id`）。为让 ext_json 完全迁出 `wf_task`
且装饰查询不失效，`wf_ext_task` 增加 `biz_task_id` 列：`type=7` 装饰行以该列关联宿主待办，
装饰查询按 `biz_task_id IN (VO ids) AND type=7` 批量回读 `ext_json`。
配套索引 `idx_biz_task_type(biz_task_id, type)`。

#### 决策 6：裸 SQL 纪律

`countFromAct` 走 JdbcTemplate，绕过 MyBatis-Plus 的 `@TableLogic` 与租户插件 →
读 `wf_ext_task` 的两段 SQL 显式带 `is_deleted = 0`，并由 `appendWfTenant(...,"t")`
追加 `tenant_id = ?`。列表侧 `extTaskQuery` 走 Mapper，由插件自动追加，两侧等价
（§7.10.3「角标 12、列表 11」教训）。

#### 决策 7：分页两源分段取数（造数验证发现并修复的生产缺陷）

原 `page()` 把扩展任务**只在第一页并入**、深页 offset 仍按 ACT 主表计算。造数实测暴露两处丢单：
`done total=280` 但 `pageSize=200` 时 80 条扩展任务**一条都翻不到**（第一页 ACT 占满即被
`subList(0, pageSize)` 截掉）；`page3` 直接返回 0 条。

**修复**：把全量结果视为「ACT 段 `[0, actTotal)` + 扩展段 `[actTotal, total)`」，
按全局 `offset` 计算本页各段应取的行数（`actLimit` / `extOffset` / `extLimit`），
新增 `selectExtPage` 复用 `extTaskQuery` 全部筛选条件并追加物理分页。
代价：跨段不保证严格全局时间序（两源各自有序），但**不丢行、不重复、深页可达**。

#### 决策 8：`ORDER BY` 必须带稳定二级排序（造数验证发现并修复）

造数把 135 条 `ACT_RU_TASK.CREATE_TIME_` 设为同一时刻，
`ORDER BY CREATE_TIME_ DESC LIMIT/OFFSET` 无确定次序 → p1/p2 重叠 35 条
（`collected=165, unique=130`）。生产中同毫秒创建的任务同样会触发。

**修复**：ACT 待办/已办列表与分页 SQL 追加 `, r.ID_ DESC` / `, h.ID_ DESC`；
扩展段分页 `selectExtPage` 追加 `orderByDesc(WfExtTask::getId)`。
（排序只加在分页查询，`countResidue` 复用 `extTaskQuery` 不带额外排序列，避免 count SQL 冗余。）

### 10.8 端到端验证结论（dev，2026-11-09，act 读源）

**数据重置**：备份 `seed_backup_20261109.json` 后清空 20 张实例/任务表
（`ACT_RE_*`/`ACT_GE_*` 流程定义与部署资源**保留**，否则系统无法发起流程），
用 `seed_wf_ext_task.py` 批量造数；`verify_seed.py` 精确复现角标/列表判据。

| 项 | 结果 |
|---|---|
| 造数规模 | 135 ACT 待办 / 200 ACT 已办 / 110 扩展任务 / 20 装饰行 / 15 会签N:1 / 10 子流程 / 40 公文维度 |
| 过滤验证 | 草稿 8、测试态 8、逻辑删除 8、装饰行 20 全部正确排除 |
| `/task/todo` | **165**（135 ACT + 30 扩展）✅ 与 SQL 预期一致 |
| `/task/done` | **280**（200 ACT + 80 扩展）✅ 与 SQL 预期一致 |
| 分页 `todo` | 逐页 `[100,65]`，`ZERO_LOSS=True`、`NO_DUP=True`、id 集合 == 全量列表 ✅ |
| 分页 `done` | 逐页 `[100,100,80]`，`ZERO_LOSS=True`、`NO_DUP=True`、id 集合 == 全量列表 ✅ |
| DONE 状态分布 | `2:200, 7:20, 8:20, 11:40`（协办/抄送/传阅映射回原 6 态，前端零改动）✅ |
| TODO 状态分布 | `0:165`（加签协办/退回重提交映射为待办）✅ |
| `/odoc/todo` / `/odoc/done` | 20 / 20 ✅ |
| 自由流程 `nodeName` | 20 条 `wf_ext_task.ext_json` 装饰生效 ✅ |
| 服务日志 | ERROR = 0 ✅ |

**编译/单测**：`mvn -am compile` 全量通过；相关 30 个单测全通过；
全模块 313 个测试中 311 通过，2 个失败（`WfBpmnDefNodesTest`、
`WfOperatorResolverMatrixTest`）为**既有失败**，被测类不在本次改动范围。

**脚本与备份**（`springbladeandreact/` 根目录）：
`seed_wf_ext_task.py`（幂等，备份存在即跳过）、`verify_seed.py`、
`e2e_final.py`、`seed_backup_20261109.json`（旧数据可恢复）、
`seed_verified.json` / `seed_expected.json`（预期口径）。

#### 遗留（P6 后续项，非本次范畴）

- A 类台账物理退役：需「创建引擎任务时直写 ACT 业务列」+ 全链路 ID 语义切换。
- 会签 N:1 原生化（§9.4 方案 X）：本次不改引擎调用方式，仅保证 N:1 守卫两表合并统计不失效。
- `wf_ext_task` 的 `type=2/4/6` 当前无业务写入点（仅造数构造验证），
  待对应 OA 语义落地后接入 `circulate`/`addSign`/退回重提交路径。
- 跨段严格全局时间序：两源分段取数下不保证，如需严格按时间混排，
  需改造成「两源各自取前 offset+size 后在内存归并」或统一落到单表查询。








