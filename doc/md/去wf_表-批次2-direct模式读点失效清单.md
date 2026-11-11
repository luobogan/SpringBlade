# 去 wf_ 表 · 批次 2：direct 模式下 `wf_task` 空表导致的读点失效清单

> **背景**：`blade.workflow.task-ledger-mode=direct`（批次 1，默认值）时，任务创建**不再落 `wf_task` 行**，
> 业务列直写 `ACT_RU_TASK`/`ACT_HI_TASKINST`（见 `WfLedgerModeProperties` / `WfTaskActDirectWriter`）。
> dev 实测 `wf_task` 为**空表（0 行）**。
>
> **问题**：批次 1 只做了**写侧单路径化**，读侧/补偿侧仍有若干处把 `wf_task` 当权威源 →
> 该表为空即**静默失效**（只打 log、不抛异常、不影响主流程），是最隐蔽的半迁移形态。
>
> **本文档**：① 已修复并验证的缺陷；② 尚未翻源的读点清单（待办）；③ 防再犯建议。
>
> 关联：`doc/md/去wf_表-任务读源翻源上线检查.md`（列表读源 `wf→act` 切换）、
> `doc/md/Flowable8承接台账模块-去wf_表改造分析.md`（总体改造分析）。

---

## 一、已修复并验证（批次 2）

| ID | 缺陷 | 症状（direct 模式实测） | 修复 | 验证 |
|---|---|---|---|---|
| **D2** | 引擎事件驱动的状态反写整链失效 | `WfStateProjector` 以 `wf_task` 行作锚点 → 表空 → 一行都找不到 → `ACT_HI_TASKINST.BUSINESS_STATUS_` 永久停在 `'TODO'`。**已办列表把「办结」显示成「待办」；按「已办」状态筛选漏行** | 改为按引擎 taskId **条件反写 ACT 业务列**（`WfTaskActDirectWriter.markStatusIfTodo`：仅当当前为待办态才改，协办/抄送语义不受影响，天然幂等） | dev 实测：被引擎删除的 MI 子任务 `TODO → FINISHED`；自动完成的创建节点 `None → DONE`；本次实例「已结束但仍 TODO」= **0 条** |
| **D1** | 超时任务扫描 100% 失效 | `WfTimeoutJob` 先从 `ACT_RU_TASK` 捞到期行，再回 `wf_task` 反查业务任务 → 表空 → 逐条跳过 → **超时自动通过/转办/催办完全不触发**，`TIMEOUT_HANDLED_` 永不置位（每轮重复扫描 + 重复告警） | direct 模式改为**用扫描 SQL 已取到的 ACT 列在内存组装 `WfTask`**（不再回读 `wf_task`） | `WfTimeoutDirectModeTest`（H2）3/3：direct 扫得到、legacy 行为不变、过滤条件正确 |
| **D4** | 待办幂等判重恒 false → **待办 id 漂移** | `WfInstanceServiceImpl.existsTask` 查 `wf_task` → 恒 false → 每轮 `advanceInternal` 都重新 `insertTask` 生成**新的 `BIZ_TASK_ID_`** 并覆盖 ACT 行 → 用户手上待办 id 漂移，提交报「任务不存在（可能该流程已被删除）」 | 改为按 `ACT_RU_TASK` 判重（`WfTaskActReader.existsTaskFromAct`）。⚠️ **判据必须是「业务待办已生成」**（`BIZ_TASK_ID_` 非空），不能只按引擎行判重——引擎行在业务建待办之前就存在，只按它判重会让首轮 advance 跳过 `insertTask`，业务列永不写入 | dev 实测：676 的业务待办 id 与其引擎任务 `BIZ_TASK_ID_` **完全一致**，另一人审批触发 advance 后**未漂移** |
| **D10** | A 类漂移告警 100% 误报 | `warnIfEngineTaskMismatch` 用 `wf_task` 的 `engine_task_id` 集合比对 → 恒空 → 每个引擎任务都被误报「A类漂移，用户看不到该待办」 | 改为按 `ACT_RU_TASK.BIZ_TASK_ID_` 是否为空判定（`WfTaskActReader.boundEngineTaskIdsFromAct`） | dev 重启后日志**无 A 类漂移误报** |

> **D3（自研会签门禁）已确认无需改**：`WfTaskServiceImpl.doApprove` 的 `countPending`/`nextPending`/`closeSiblings`
> 被 `if (!isEngineMultiInstance(...))` 包裹（`WfTaskServiceImpl:341`）——引擎 MI 化后放行由引擎完成条件驱动，
> 自研门禁本就应当退役。仅 `reject()` 里的 `closeSiblings`（`:629`）未加该守卫，但其效果已被 **D2** 覆盖
> （被引擎删除的 MI 子任务由 `onTaskDeleted` 收口）。

---

## 二、读点失效清单（**已全部翻源完成**）

> ✅ **状态（2026-11-11 批次 3）：下表 10 项已全部翻源**。统一复用 `WfTaskActReader` 新增的 6 个通用读源方法：
> `activeTasksFromAct` / `activeTaskFromAct` / `countDoneByInstanceFromAct` /
> `allTasksByInstanceFromAct` / `residualTasksFromAct` / `lastDoneOperatorFromAct`；
> 每个调用点以 `ledgerMode.isDirect()` 分支路由，legacy 模式行为不变。
>
> 全模块复核（47 处 `taskMapper.select*`）：**42 处有 direct/actRead 守卫，5 处无守卫但均可接受**
> （3 处自研会签门禁被 `!isEngineMultiInstance` 守卫、1 处 ACT 主路径已覆盖的死兜底、
> 1 处默认禁用的历史漂移脚本）。`countPending`/`nextPending`/`closeSiblings` 的 4 个调用点中
> 3 个在 MI 守卫内，`reject()` 那一个由 `WfStateProjector.onTaskDeleted`（D2）覆盖。
>
> **dev 实测**：`/test/todo?instId` 从「恒空」变为返回 1 条；`/test/step` 从「一步都走不了」
> 变为成功推进 `Task_Department → Task_FinReview`（新待办 assignee=608）。
>
> 以下为翻源前的失效形态（留作回归参考）：均为 direct 模式下
> **查 `wf_task` → 空集 → 静默降级**，行为是「不报错、结果为空或退化」。

| 影响 | 位置 | 功能 | direct 下的表现 |
|---|---|---|---|
| 高 | `WfTestServiceImpl:1719` | 流程测试单步提交前「重读待办」 | `fresh == null` → `continue` → **自动测试一步都走不了**（静默跳过，无报错） |
| 高 | `WfTestServiceImpl:2623,2632` | MI 场景自测（逐人驱动门禁 + 断言） | `todos` 恒空 → 循环立即 break → **自测必然判失败**（`failReason` 误导） |
| 中 | `WfInstanceServiceImpl:3260` | MI 对账：关闭被完成条件取消的兄弟待办 | `pendings` 空 → 不收口（与 D2 同源，D2 修好后多数场景已覆盖） |
| 中 | `WfInstanceServiceImpl:2269,2273` | 监管动作（强制归档/回退）前关闭待办 + sync ACT | `selectBatchIds` 返回空 → **监管动作前的待办收尾完全不发生**（漂移） |
| 中 | `WfInstanceServiceImpl:2971` | 当前节点定位（强制归档范围 / 监控回退落点） | 回退到 `inst.getCurrentNodeKey()` 单一值 → 并行分支下可能落错节点 |
| 中 | `WfFreeFlowDriver:156,168` | 自由流程：为首人以外的办理人补并列待办 | `sample` 恒 null → **非 MI 自由流程步骤只有第一个人收得到待办** |
| 中 | `WfFreeFlowDriver:207` | 自由流程：步骤名装饰 | 不写装饰行 → 待办列表显示引擎默认节点名而非编排步骤名 |
| 低 | `WfInstanceServiceImpl:4472` | 归档「归档人」兜底 | 返回 null → 回退发起人 → 归档意见的归档人变成发起人 |
| 低 | `WfWriteHelper:257` | `terminate` 的非事件驱动兜底关待办 | 空 → 无操作（与 `WfStateProjector.closeResidualTodos` 同源） |
| 低 | `WfTestServiceImpl:1764,1817,2448,2459` | 创建节点必填校验 / 我的测试待办 / 测试实例待办 / 操作者面板 | 恒空或恒 0 → 误判「首次提交」、面板全空 |

**共用修复素材**（已具备，可直接复用）：
`WfTaskActReader`（ACT 读源：`todoFromAct`/`doneFromAct`/`existsTaskFromAct`/`boundEngineTaskIdsFromAct`）、
`WfTaskBizResolver`（ACT → `WfTask` 还原）、
`WfTaskActDirectWriter`（条件反写 ACT 业务列）。

---

## 三、防再犯收尾（**已全部落地**，2026-11-11 批次 4）

> ✅ 本轮把 §二 翻源后残留的设计债一次性收口，全模块 346 例单测 0 失败，dev 实测核心路径无回归。

1. **「direct 下无 `wf_task` 读执行」已实质达成**（等价于「物理不可用」的目标）：
   - 全模块 47 处 `WfTaskMapper` 读点中，42 处翻源点均走 `if (ledgerMode.isDirect()) ACT else wf_task` 分支，
     direct 下不碰 `wf_task`；
   - 5 处无守卫读点中，3 处自研会签（`WfTaskServiceImpl#countPending/nextPending/closeSiblings`）+ `reject` 的
     `closeSiblings` 已加 `ledgerMode.isDirect()` 守卫（direct 下返回安全值，因 Mi 化后自研会签已退役）；
     1 处 ACT 主路径已覆盖的死兜底、1 处默认禁用的历史脚本本来就不读 `wf_task`。
   - **结论**：direct 配置下**没有任何** `wf_task` 读会被执行，「漏翻源」已无漏网之鱼。
   - ⏳ *可选的硬保险*：把 `WfTaskMapper` 包成「direct 下抛错」的装饰器能让**未来新增代码**的漏翻源立刻 500，
     但需改所有注入点类型 + 全部测试 mock 类型（改动面大），且当前已无漏网，故列为可选后续，未强制实施。
   - 新增 `WfTaskServiceImpl` 注入 `WfLedgerModeProperties ledgerMode`（`@Autowired`，不破坏测试构造器）。
2. **降级反模式已消灭**：`WfTaskServiceImpl` 的 `count`/`page`/`list` 在 ACT 查询异常时，
   **direct 模式下直接抛 `IllegalStateException`**（带上下文），不再静默降级走空 `wf_task`；
   legacy 模式保持降级（wf_task 有数据，安全）。
3. **读源开关已统一**：`WfInstanceServiceImpl` 读**任务**的 4 处（1558/1600/1649/4343 的①任务段）与
   `WfFormRenderServiceImpl:133` 由 `actLogReader.actRead()`（实例读源）改为 `taskActReader.actRead()`（任务读源）；
   读**实例/审批日志**的门控（831/845/931/1099/1143/1170/1209/1767/4311/4388 等）保持 `instance-read-source`，未动。
4. **死字段已清理**：`WfTimeoutServiceImpl` / `WfFreeFlowController` / `OdocSealAdvancer` 三处 0 引用的
   `WfTaskMapper` 声明 + import 已删除，同步修掉两个直构测试的构造参数与失效的 `verify(taskMapper, never())`。
5. **`WfTaskActWriter` javadoc 已修正**：从「双写并存」改为「direct 单路径直写 / legacy 才落台账」。

---

## 四、验证方式

```bash
cd springBlade
mvn -o -pl blade-service/blade-workflow -am test "-Dtest=WfLedgerDirectModeTest,WfTimeoutDirectModeTest,WfStateProjectorActSyncTest,WfTaskServiceImplMiMergeTest"
```

- `WfLedgerDirectModeTest`（H2，15 例）：条件反写 / 残留收尾 / 待办判重 / 漂移判据 /
  **批次 3 的 6 个通用读源方法**（活跃待办过滤、按 id 重读、已办计数、操作者面板、残留收尾、最后办理人）
- `WfTimeoutDirectModeTest`（H2，3 例）：超时扫描 direct 模式
- `WfStateProjectorActSyncTest`（H2 + Mockito，4 例）：事件反写 legacy + direct 双路径
- `WfTaskServiceImplMiMergeTest`（5 例）：D17 加签集合变量合并语义

全模块回归：`mvn -o -pl blade-service/blade-workflow -am test`（当前 **346 例 / 0 失败 / 1 skipped**）。

**dev 端到端**（服务需以 `direct` 模式运行）：

| 场景 | 修复前 | 修复后 |
|---|---|---|
| 加签 → 驳回 → 重提 | 加签人 676 丢失 | 676 仍在 `Task_Department` |
| 引擎删除的 MI 子任务状态 | `TODO` | `FINISHED` |
| 待办 `BIZ_TASK_ID_` | 每轮 advance 重新生成 | 与用户手上的 id 一致，不漂移 |
| `/test/todo?instId` | 恒空 | 返回真实待办 |
| `/test/step`（自动测试） | 一步都走不了 | 成功推进 `Task_Department → Task_FinReview` |

> ⚠️ `/test/mi-scenario` 当前仍报「`miNode` 未解析到任何办理人」→ 事务 rollback-only（500）。
> 该错发生在 `advanceInternal` 的**操作者解析阶段**，早于任何 `wf_task` 读点，属场景造数缺陷
> （造出的 BPMN 只写 `flowable:assignee="placeholder"`、无 `wf:operator` 扩展），与本次翻源无关，需单独修。
