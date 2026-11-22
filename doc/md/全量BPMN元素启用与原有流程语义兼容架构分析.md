# 全量 BPMN 元素启用与原有流程语义兼容架构分析

> **总索引**：[工作流分析文档总索引.md](工作流分析文档总索引.md)（七份递进式分析文档的统一入口）
>
> **命题**：若要把画布里所有 BPMN 元素（服务/脚本/发送/接收/调用活动/子流程/中间与边界事件等）全部真正实现，同时**完整保留 SB 现有流程能力**（发起即归档语义、业务层推进、去 wf_ 表 direct 模式、方案C 事件反写、自由流程、测试态隔离…），是否可行？代价与冲突在哪？
>
> **分析方法**：以"两套推进模型能否共存"为核心，对 advance 调用链、事件反写、归档判定、事务边界、测试态、自由流程、direct 模式七个维度做源码实证。
>
> **配套文档**：现状与逐元素风险见《流程设计器-BPMN元素支持现状与启用风险分析.md》；理论可行性见《E9与Flowable7流程节点功能对比及改造可行性评估.md》；Flowable 能力盘点与增强蓝图见《Flowable能力挖掘与功能增强蓝图.md》。
>
> **日期**：2026-11-17

---

## 0. 结论先行

**可行，但不是"加实现"，而是"在一条流程里同时跑两套推进内核"——这是本命题的真正难度所在。**

- SB 现有内核是**业务层驱动**：`completeTask/moveActivity → advance()` 全在业务方法里显式调用，状态、待办、归档都靠它。
- BPMN 原生元素靠**引擎驱动**：ServiceTask/Timer/CallActivity 由引擎在自己的事务里自动推进，不经过任何业务方法。
- 两者之间的**正向桥（引擎→SB）目前是断的**：没有任何引擎事件会触发 `advance`，方案C 又明确把"建待办"排除在事件反写之外、且不监听活动级事件。

因此，全量启用的**最大阻塞点**不是某个元素缺实现，而是**缺一条能把引擎自动推进结果同步回 SB 台账的桥**，以及由此衍生的一系列冲突（等待态误判归档、事务断裂、测试态守卫被绕过、自由流程步骤错位）。

**推荐架构：双内核 + 单一事实源（Flowable）+ 事件驱动投影 + 显式状态机守卫**，分阶段落地，绝不一次性全开。

---

## 1. 根本矛盾：两套推进模型

| 维度 | SB 现有内核（业务层驱动） | BPMN 原生内核（引擎驱动） |
|---|---|---|
| 推进者 | 业务方法显式调 `advance()` | 引擎执行活动后自动流转 |
| 状态同步 | `advance` 内写 `wf_instance`/`wf_task`/业务列 | 引擎写 `ACT_RU_*`/`ACT_HI_*` |
| 待办生成 | 业务层 `advance` 建 | 引擎 TaskListener/Task 实体 |
| 归档判定 | `currentTasks 为空 → APPROVED` | ProcessInstance ended 事件 |
| 事务 | 业务 `@Transactional` 一个事务 | 异步 job 自己的独立事务 |

两者对"谁推进、谁建待办、谁写状态、谁判归档"的答案完全不同。全量启用等于让一条流程里同时存在这两种答案——**所有冲突都从这里派生**。

---

## 2. 七个维度的源码实证

### 2.1 advance 调用链：14 处全在业务层，无引擎事件入口

全仓主源码 `.advance(` 共 14 处，全部来自业务/回调层：

- 提交主链 `WfTaskServiceImpl.java:505`（多目标）、`:520`（指定流转）、`:527`（普通 completeTask 后）；`autoApprove`（`:272-305`）与超时 `WfTimeoutServiceImpl.java:215-233` 汇入 `doApprove`→`:527`
- 退回 `:656`（`AdvanceSrc.REJECT`）、`:1741`（退回后重提）
- 发起 `WfInstanceServiceImpl.java:627`；收回/监管退回 `:2389`、`:2895`；异常兜底递归 `:3223`
- 子流程归档回调 `WfSubflowServiceImpl.java:118`
- 自由流程 `freeflow/WfFreeFlowDriver.java:109`（末步）、`:117`（中间步）
- 测试 `WfTestServiceImpl.java:350`、`:1649`；外部签章回调 `OdocSealAdvancer.java:127`

```java
// WfTaskServiceImpl.java:522-528
processService.completeTask(task.getEngineTaskId(), vars);
...
if (!jumpDirectIfMarked(inst, operator)) {
    instanceService.advance(inst.getId(), operator);
}
```

**没有任何一个 listener 调用 advance**。`WfStateProjector.java:29,99` 注释直言："**『建待办』无法下沉为事件驱动，仍由业务侧负责**"。

> 后果：引擎自动跑完 ServiceTask/Timer 后，`wf_instance`/业务状态没有同步入口。

### 2.2 方案C 事件反写：落地了，但覆盖不到自动节点

注册：`FlowableConfig.java:146-178`（四个 listener 聚合一次 `setEventListeners`）。

| 监听器 | 监听事件 | 反写内容 |
|---|---|---|
| `WfEngineEventListener.java:69-91` | PROCESS_STARTED/COMPLETED/CANCELLED、ENTITY_SUSPENDED/ACTIVATED、TASK_CREATED/ASSIGNED/COMPLETED、ENTITY_DELETED | 派发 `WfStateProjector` |
| `WfProcessNoticeListener.java:71-75` | TASK_CREATED、PROCESS_COMPLETED | 消息中心 `:165` |
| `WfBizCallbackListener.java:53-55` | 仅 PROCESS_COMPLETED | Feign 回调 |
| `WfMiEmptyGuardListener.java:81` | MULTI_INSTANCE_ACTIVITY_STARTED | 空集合拦截，不反写 |

致命缺口（`WfEngineEventListener.java:88-90`）：

```java
default -> {
    // 其余事件（变量/作业/活动/序列流等）不参与台账反写
}
```

**无 ACTIVITY_COMPLETED / ACTIVITY_STARTED 分支**。设计文档《工作流双写漂移治理-方案C》`:76-85` 的事件表同样没有活动级事件。

`WfStateProjector.java:20-33` 明确两类不可比事件：

```java
// :26-33
PROCESS_STARTED / TASK_CREATED（业务 advance() 返回后才建行）...
范围结论：「建待办」无法下沉为事件驱动，仍由业务侧负责；
事件驱动只承接状态变更（已办/终态/暂停/恢复）。
// :101-104 onTaskCreated 只 log.info("[skip] ...")
```

实际反写仅：`PROCESS_COMPLETED/CANCELLED`（`:115-135`）、`ENTITY_SUSPENDED/ACTIVATED`（`:137-149`）、`TASK_COMPLETED`（`:152-196`）、`ENTITY_DELETED`（`:212-255`）。

> 后果：即使 ServiceTask 后落到新 UserTask，也没有桥接路径生成 SB 待办——TASK_CREATED 虽派发却被跳过。

### 2.3 归档判定与等待态冲突（最直接、最危险）

```java
// WfInstanceServiceImpl.java:3120-3133
List<TaskVO> engineTasks = processService.currentTasks(inst.getEngineInstId());
if (engineTasks.isEmpty()) {
    if (subflowService.holdForSubflow(instId)) { ... return; }   // 唯一额外信号：子流程
    inst.setStatus(WfInstance.STATUS_APPROVED);
    inst.setEndTime(new Date());
    inst.setCurrentNodeKey("");
    instanceMapper.updateById(inst);
    actWriter.writeLifecycle(...);   // 终态双写进 ACT_HI_PROCINST
```

`ProcessServiceImpl.java:126-130` 的 `currentTasks` 只查 `ACT_RU_TASK`（UserTask），**没有 execution 数量、没有 `runtimeService.createProcessInstanceQuery().active()` 参与判定**（全模块 `createExecutionQuery` 0 匹配）。

> **ReceiveTask / Timer / message catch 等待时 `ACT_RU_TASK` 为空 → 立即被置 APPROVED + endTime，且终态被双写进 ACT_HI_PROCINST，不可逆。** 这是全量启用时第一个会爆的雷。

### 2.4 事务边界断裂

- `doApprove` 的 `@Transactional` 在 public 方法（`WfTaskServiceImpl.java:266-268`、`:272-305`），私有 `doApprove`（`:314`）在 `:462-545` 同一 try 块内先 `completeTask/moveActivity` 再 `advance`，advance 自身 `@Transactional`（`WfInstanceServiceImpl.java:3056-3101`）传播加入同一事务。
- 当前能同步是因为 `FlowableConfig.java:127-134` 硬编码 `setAsyncExecutorActivate(false)`、`setAsyncHistoryEnabled(false)`；`WfEngineEventListener.java:103-106` `isFireOnTransactionLifecycleEvent()=false`。
- 开异步后：设计文档 `:118-156` 明确 job 在**自己的新事务**派发事件，业务 `@Transactional` 感知不到；要求三项兜底"常驻对账修复 + deadletter 告警 + C1 降级最终一致"，并写"**缺任一 → 宁可不开异步**"。

现有补偿仅限于同步方法内（`WfWriteHelper.java:229-254 ensureApplied`，由 terminate/suspend/activate 调用）与定义期对账（`WfDefinitionBackfillJob.java:174-194 reconcileAll`）；**常驻运行期台账对账 job、ACT_RU_DEADLETTER_JOB 告警均不存在**。

### 2.5 测试态隔离：守卫在调用方，引擎触发会绕过

- `WfDefinitionServiceImpl.java:2221-2296 neutralizeForTest`：businessRuleTask、ThrowEvent、timer 事件降级为 ManualTask 自动通过；`:2255-2257` SendTask 同规则（正式部署也降级 `:2312-2327`）；process id 改 `procKey__test`。
- `is_test` 守卫**全在调用方**：`WfTaskServiceImpl.java:403/405/436/549-552`、`WfInstanceServiceImpl.java:3141-3146/3163-3176`、`WfTimeoutServiceImpl.java:208-213`、`WfProcessNoticeListener.java:105-109/141-145`、`WfFreeFlowDriver.java:70-74`。
- **`WfActionExecutor` 自身零 `is_test` 过滤**（action 目录搜 `isTest|is_test|testMode` 0 匹配）：`:77-79` 直接前缀分派，`:274` 直接 `new JdbcTemplate(dataSource).update(sql)`；`NodeActionExecutor.java:92-135` 同样无判断。

> 后果：现有隔离靠"业务层决定调不调"。一旦 ServiceTask 由引擎/监听器直接触发 http/sql 动作，所有守卫均被绕过，测试流程会真实发外部请求/改库。**必须新建统一测试态闸门**（在动作执行器层而非调用方）。

### 2.6 自由流程：步骤绑定假设池内全是 UserTask

```java
// WfFreeFlowDriver.java:92-96
WfFreeNode current = context.byNodeKey(task.getNodeKey());
WfFreeNode next = context.nextOf(current);
// :105-117
processService.moveActivity(inst.getEngineInstId(), task.getNodeKey(), next.getNodeKey(), flowVars);
instanceService.advance(inst.getId(), operator, next.getNodeKey(), assignees.get(0));
```

`task` 来自 `requireTodoTask(taskId)`（`WfTaskServiceImpl.java:315`），**只有 UserTask 才有可提交待办**；`WfFreeFlowManager.java:138-177 saveSteps` 绑定静态 `Free_<order>`，只存 operatorIds/signType/extJson，**不校验节点类型**。

- 池内含 ServiceTask：`moveActivity` 进入后被引擎立即执行并流走，`advance` 读到的已不是该步骤，`stampTaskExtJson`（`:220-254`）装饰与实际节点错位。
- 池内含 ReceiveTask：`currentTasks` 空 → 触发 2.3 的误归档，自由流程提前结束。

> 需新建：步骤节点类型约束（池内只允许 UserTask）+ 自动节点在自由流程中的独立驱动方式（或明确禁止）。

### 2.7 去 wf_ 表 direct 模式：利弊并存

`application.yml`：`:65 task-ledger-mode: direct`、`:66 task-read-source: act`、`:112 instance-read-source: act`、`:147 free-flow.enabled: true`、`:175 ledger-listener.enabled: true`。

**有利**：`WfStateProjector.java:61-69/157-168/217-228` 在 direct 下按引擎 taskId 直写 ACT 业务列（`markStatusIfTodo`），不依赖 wf_task 锚点；`WfInstanceServiceImpl.java:3269-3272` 明说 direct 下无需 MI 台账对账。

**不利**：ServiceTask/Timer 本身不产生 Task 行，ACT 无写入对象；后续 UserTask 的 TASK_CREATED 被投影器跳过，ACT 的 `BIZ_TASK_ID_`/业务列无人初始化；归档判定仍只查 `ACT_RU_TASK`。

> 总体对"自动节点状态同步"**弊大于利**，除非补齐"事件→ACT 建待办"通道。

---

## 3. 冲突矩阵汇总

| # | 冲突 | 性质 | 阻塞度 |
|---|---|---|---|
| 1 | advance 无引擎事件入口 | 设计冲突 | **最大阻塞点** |
| 2 | 事件反写无 ACTIVITY_COMPLETED、跳过 TASK_CREATED | 设计冲突 | **最大阻塞点** |
| 3 | `currentTasks 空 = 归档` 误判等待态 | 设计冲突 | 高（第一个爆的雷） |
| 4 | 异步 job 独立事务 vs 业务 @Transactional | 需新建兜底 | 高 |
| 5 | 测试态守卫在调用方，引擎触发绕过 | 需新建闸门 | 高（安全） |
| 6 | 自由流程步骤假设全 UserTask | 设计冲突 | 中 |
| 7 | direct 模式自动节点无 ACT 写入对象 | 需新建通道 | 中 |
| 8 | AsyncExecutor 硬关 + 7 项配套缺失 | 环境障碍 | 中（timer/CallActivity） |

**最大阻塞点 = 1+2 构成的"引擎→SB 正向桥缺失"**：引擎自动推进后，新等待点既不生成 SB 待办，也不推进 `wf_instance` 状态；3 是这个缺口最直接危险的爆发点（误置终态且双写 ACT 不可逆）。

---

## 4. 推荐架构：双内核 + 单一事实源 + 事件投影 + 显式状态机守卫

### 4.1 三条铁律

1. **Flowable 是唯一事实源**（沿用现有项目基调）：`wf_instance.status`、`wf_task`/ACT 业务列都是投影，不是独立状态机。
2. **归档判定改信号源**：从"ACT_RU_TASK 为空"改为"流程实例是否 ended（PROCESS_COMPLETED 事件 / `runtimeService` 查询 active）+ 无活跃 execution"，彻底消除等待态误判。
3. **动作执行统一闸门**：所有 `WfActionExecutor` / 脚本 / 消息 / HTTP 调用入口加 `is_test` 与租户上下文校验，不依赖调用方自觉。

### 4.2 关键改造点

| 层 | 改造 | 复用现成机制 |
|---|---|---|
| 引擎→SB 桥 | 新增 listener 监听 ACTIVITY_COMPLETED / TASK_CREATED，投影"新等待点"到 ACT 业务列；补 `advance` 的事件触发版 | `FlowableConfig.java:146-178` 注册范式；`WfStateProjector` 投影框架 |
| 归档判定 | `advanceInternal` 引入 `processInstance ended` + `execution` 信号 | 现有 `holdForSubflow` 守卫位置（`:3122`） |
| 自动节点实现 | ServiceTask 走部署注入 `delegateExpression` → `IWfCustomAction`；timer 用 `WfTimeoutJob` 轮询 | `WfDefinitionServiceImpl.java:490-496` 注入管线；`WfActionExecutor.java:104-163` 反射注册 |
| 子流程 | 明确"业务层 triggerSubflow"与"BPMN callActivity"二选一为主，另一降级 | `NodeActionExecutor.java:147-218` |
| 测试态闸门 | `WfActionExecutor.execute` 入口统一 `is_test` 短路 + 白名单 | 现有调用方守卫可保留为双保险 |
| 自由流程 | 步骤节点类型约束：池内仅 UserTask；自动节点场景走独立流程类型 | `WfFreeFlowManager.saveSteps` 增加类型校验 |
| 异步与对账 | 若开 AsyncExecutor：常驻对账 job + deadletter 告警 + C1 最终一致，三项缺一不可 | 方案C §3.4 设计（未落地） |

### 4.3 阶段路线（必须分阶段，禁止一次性全开）

| 阶段 | 内容 | 为什么这个顺序 |
|---|---|---|
| **S0 防呆** | 设计器元素白名单 + 发布期对未实现元素硬拦 | 零风险，先止血 |
| **S1 桥与判定** | 补 ACTIVITY_COMPLETED listener + 归档判定改信号源 + 测试态闸门 | 这是所有自动节点的前置依赖 |
| **S2 ServiceTask** | 部署注入 + IWfCustomAction 复用，同步执行、不碰 AsyncExecutor | 有现成抓手，验证桥是否管用 |
| **S3 子流程/timer** | 子流程语义归一；timer 用 WfTimeoutJob 轮询绕开 AsyncExecutor | 避开 7 项配套 |
| **S4 ReceiveTask/消息** | 新建消息通道 + 外部唤醒入口设计 | 需新设计，风险中等 |
| **S5 AsyncExecutor** | 开异步 + 三项兜底全部落地 | 只有前面验证过才开 |
| **S6 ScriptTask/DMN** | 先定沙箱与依赖，最后做 | 安全与依赖双重高风险 |

---

## 5. 为什么"保留原有功能"是可能的

三条有利条件：

1. **在途实例钉版本**：`WfInstanceServiceImpl.java:506-539` 用 `startProcessInstanceById(procDefId)`，`application.yml:152-153` 明确"在途实例不受影响（Flowable 绑定创建时的流程定义版本）"。**新语义只对新发起流程生效，无需迁移旧实例**。
2. **特性开关体系已成型**：`blade.workflow.free-flow.enabled`、`mi-empty-guard.enabled`、`multi-instance-enabled`、`task-ledger-mode: direct` 等，新能力可沿用"开关默认关、灰度开"的模式（`application.yml:147,175`、`WfFreeFlowDriver.java:61-63`）。
3. **单一事实源基调已定**：项目已是"以 Flowable 为唯一事实源"，全量启用只是把更多引擎事件纳入投影，不是推翻重来。

**但要注意**：保留 ≠ 不变。至少要动三处既有语义——归档判定信号源（2.3）、测试态闸门位置（2.5）、自由流程池约束（2.6）。这三处是"全量启用"与"保留原有"必须交叠的地方，无法绕开。

---

## 6. 最终判断

- **技术可行性**：可行。没有原理性障碍，Flowable 原生支持这些元素；SB 的扩展点（部署注入、事件监听、动作注册表）足以承载。
- **真正成本**：不在"实现元素"，而在**补引擎→SB 正向桥 + 消除两套推进语义的冲突**。这是系统性改造，不是单点开发。
- **最大风险**：仓促全开会导致等待态流程被误判归档且终态双写不可逆（2.3）、测试流程绕过守卫触发真实外部动作（2.5）。
- **推荐决策**：按 S0→S6 分阶段，S1（桥+判定+闸门）完成前不启用任何自动节点；AsyncExecutor 在三项兜底落地前保持关闭。
- **一句话**：能全做、能保留，但必须"先修桥、再通车"，且旧功能的三处语义要同步调整——这不是加功能，是一次内核级演进。

---

## 7. 证据索引

| 结论 | 代码位置 |
|---|---|
| advance 14 处调用全在业务层 | `WfTaskServiceImpl.java:272-305,505,520,527,656,1741`；`WfInstanceServiceImpl.java:627,2389,2895,3223`；`WfSubflowServiceImpl.java:118`；`WfFreeFlowDriver.java:109,117`；`WfTestServiceImpl.java:350,1649`；`OdocSealAdvancer.java:127` |
| 建待办无法下沉事件驱动 | `service/helper/WfStateProjector.java:20-33,99-104` |
| 事件监听无活动级事件 | `listener/WfEngineEventListener.java:69-91`（default 分支 `:88-90`） |
| 归档只看 ACT_RU_TASK | `WfInstanceServiceImpl.java:3120-3133`；`ProcessServiceImpl.java:126-130` |
| 事务同步前提 / AsyncExecutor 关闭 | `config/FlowableConfig.java:103-106,127-139` |
| 异步三项兜底要求 | `docs/工作流双写漂移治理-方案C-事件驱动落地设计.md:91-106,118-156` |
| 测试态降级与调用方守卫 | `WfDefinitionServiceImpl.java:2221-2296`；`WfTaskServiceImpl.java:403-552`；`WfInstanceServiceImpl.java:3141-3176` |
| 动作执行器零 is_test 过滤 | `action/WfActionExecutor.java:38-43,77-79,274`；`NodeActionExecutor.java:92-135` |
| 自由流程步骤假设 UserTask | `freeflow/WfFreeFlowDriver.java:92-117`；`WfFreeFlowManager.java:138-177` |
| direct 模式配置与投影 | `application.yml:65-66,112,147,175`；`WfStateProjector.java:61-69,157-168,217-228` |
| 在途实例钉版本 | `WfInstanceServiceImpl.java:506-539`；`application.yml:152-153` |
| 部署注入管线 | `WfDefinitionServiceImpl.java:490-496` |

---

## 8. 实施记录：E7 引擎→业务状态桥（2026-11-18 已落地）

> 本文 §3"冲突矩阵"标注的两处最大阻塞点之一（"引擎→SB 正向桥缺失"）已于 2026-11-18 完成**首版实施**。本章固化落地结果、实现取舍与明确边界。

### 8.1 落地的代码改动

| 文件 | 改动 |
|---|---|
| `config/WfFlowtableCompatProperties.java` | 新增 `engineAutoTodoBridge=false` + `engineAutoTodoGrayDefIds=[]`（默认关） |
| `resources/application.yml` | compat 段加 E7 双开关 + 前置说明 |
| **新增** `listener/WfEngineAutoTodoBridgeListener.java` | 监听 `ACTIVITY_COMPLETED`，只处理自动节点类型（`serviceTask`/`scriptTask`/`businessRuleTask`/`callActivity`），ThreadLocal 防重入 + defId 灰度 + 失败捕获告警不回滚；调公开 `advance(...)` 复用完整建待办链路 |
| `config/FlowableConfig.java` | 第 5 个 listener 并入统一 `globalListeners` 一次注册（未另起 setEventListeners，规避历史覆盖事故） |

### 8.2 实现取舍（与 §4 架构方案的差异）

- 原方案建议"从 `advanceInternal` 抽出公开的建待办方法"。实际采用 **listener 直接调已公开的 `advance(instId, starter, null, null, SUBMIT)`**。
- 理由：`advance()` 已内置判重、`insertTask`（生成 `BIZ_TASK_ID_`、写 ACT 10 列、超时）、实例状态同步；抽新方法等于复制这套复杂逻辑，反而更易产生状态漂移。"复用既有收口"优于"另起一套实现"。
- 未单独监听 `TASK_CREATED` 建待办：`advance()` 在自动节点完成后读引擎当前任务并同步，已覆盖"下一等待点建待办"；单独再监听会导致待办重复与 `BIZ_TASK_ID_` 漂移。

### 8.3 验证结果

| 项 | 结果 |
|---|---|
| 单测 | **46/46 PASS**（E7 listener 6 例 + E7 H2 集成 2 例 + 既有 38 例） |
| E2E | **26/26 PASS**（开关全关 = 现有行为零回归） |
| 编译 | 通过（`mvn -o -pl blade-service/blade-workflow -am test-compile -q`，必须带 `-am`） |

- `WfEngineAutoTodoBridgeListenerTest`（6 例）：activityType 过滤、灰度白名单内/外、防重入、失败策略恒 false。
- `WfEngineAutoTodoBridgeIntegrationTest`（2 例，H2 内存库 + 真实 Flowable 引擎）：部署 `start → serviceTask(NoOpServiceTask) → userTask` BPMN，验证**真实引擎链路**——ServiceTask 自动完成后派发 `ACTIVITY_COMPLETED` → E7 listener 捕获 → 调 `advance(instId, starter, null, null, SUBMIT)`；断言 NoOpServiceTask 已执行 1 次、token 停在 userTask、advance 参数正确；灰度名单外不触发。这补上了"只有 mock 单测、无真实引擎验证"的缺口。

测试实例已清理。

### 8.4 明确边界（首版未覆盖，避免误用）

1. **MI 父节点完成事件不处理**：MI 走 `MULTI_INSTANCE_ACTIVITY_*`，普通 `ACTIVITY_COMPLETED` 不覆盖；首版显式不处理，避免或签首人过后重复建待办。
2. **Timer 不可用**：AsyncExecutor 仍关闭（`FlowableConfig.java:127-128`），ServiceTask 同步路径可用，Timer 需先开异步 + 三项兜底。
3. **失败策略独立**：E7 自身 catch 全部异常并告警，不受 `strict-event-fail` 影响，绝不回滚引擎。
4. **测试态**：建待办通道不跳过测试实例（否则测试流程跑不了），但 `IS_TEST_` 写 ACT、不发通知（已有 listener 过滤）、不触发外部动作（action-gate 守卫）。

### 8.5 放量前提（当前全关，对生产零影响）

- 先开 `action-gate`（自动动作受闸门）
- 用 `engine-auto-todo-gray-def-ids` 填 1 个测试 defId 灰度，验证无事件循环/待办重复
- 通过后再清空名单转全量
- ⚠️ Nacos 若配 `blade.workflow.compat` 以 Nacos 为准，需同步

## 9. 实施记录：E1 ServiceTask 自动执行能力（2026-11-18 已落地）

> §3 标注的最大阻塞点其二「ServiceTask 等自动节点画了不执行」的首版能力已落地。E7 解决了「自动节点跑完后台账同步」，E1 解决「自动节点本身可执行」；两者互补，本轮默认全关。

### 9.1 落地的代码改动

| 文件 | 改动 |
|---|---|
| `config/WfFlowtableCompatProperties.java` | 新增 `serviceTaskExec=false`（默认关） |
| `resources/application.yml` | compat 段加 `service-task-exec: false` + 前置说明（须先开 action-gate、翻转后重新发布） |
| `service/impl/WfDefinitionServiceImpl.java` | 新增 `injectServiceTaskDelegate(String)`；在正式部署 `deploy()` 的 MI 注入之后、`deployProcess` 之前，以及测试部署 `deployForTest()` 同位置各调一次；仅对带 `wf:node/customAction` 的 ServiceTask 注入 `implementationType=delegateExpression` + `implementation=${wfServiceTaskDelegate}`，失败 warn 回退原 XML |
| **新增** `action/WfServiceTaskDelegate.java` | `@Component("wfServiceTaskDelegate")` 统一 JavaDelegate：从 `getCurrentFlowElement()` 反读 actionKey → 由 `engineInstId` 反查 `WfInstance` → 调带 gate 的执行器入口 |
| `action/WfActionExecutor.java` | 新增 public `executeCustomAction(inst, node, phase, actionKey, overrideJson)`，内部拼 `action:<key>[\|<json>]` 后**走 `execute()`**（保留 test/tenant 闸门），杜绝 delegate 直接调 `doCustomAction` 绕过守卫 |

### 9.2 实现取舍

- **注入只碰"声明了 customAction"的 ServiceTask**：导入流程里已有 `flowable:class`/`expression` 实现的 ServiceTask 保持原样，不覆盖（避免误伤存量）。
- **delegate 不做 advance/建待办**：只负责执行自定义动作；完成后的台账同步由 E7 桥监听 `ACTIVITY_COMPLETED` 负责。若 E1 也做推进，会与 E7 重复建待办。二者灰度时应配套开、职责单一。
- **必须走 `WfActionExecutor.execute()`**：`action-gate` 的测试态拦截、租户校验都收口在该方法 `execute()` 内；直接调 `doCustomAction` 会整段绕过。新增入口只是拼命令的薄封装，不放任何新守卫逻辑。
- **失败即回滚**：delegate 不 catch 动作异常。本项目 AsyncExecutor 关闭、无 deadletter，同步链路抛 `ServiceException` → 事务回滚、token 停在 ServiceTask。语义与人工节点附加动作一致：动作没成功不能让流程继续。
- **配置来源沿用既有 schema**：读 `wf:node/customAction@actionKey`，不改前端 wfModdle.json、不新增字段，规避 `WfModdleSchemaDriftTest` 双端漂移。

### 9.3 验证结果

| 项 | 结果 |
|---|---|
| E1 新增单测 | **11/11 PASS**（delegate 分支 6 + 部署注入 3 + H2 真实引擎集成 2） |
| 全量单测 | **381 例，0 失败 / 0 错误 / 1 跳过**（`mvn -o -pl blade-service/blade-workflow -am test`） |
| E2E | 本轮未跑（开关默认关、旧轨零命中）；此前 E7 阶段 26/26 通过 |

- `WfServiceTaskDelegateTest`（6 例）：开关关/非 ServiceTask/无 actionKey/反查不到实例均跳过；正常场景断言调用 `executeCustomAction(inst, node, "serviceTask", "demoAction", null)` 且 nodeKey/nodeType/defId 正确。
- `WfServiceTaskDelegateInjectTest`（3 例）：开关关原样返回；开时带 customAction 的 ServiceTask 被注入 `delegateExpression`；无 customAction 不注入。
- `WfServiceTaskDelegateIntegrationTest`（2 例，H2 + 真实引擎）：部署 `start → serviceTask(delegateExpression) → userTask`，真实解析 `${wfServiceTaskDelegate}` Spring Bean → 反读 actionKey → 调执行器 → token 停在 userTask；运行时关开关则 delegate 短路不执行。

### 9.4 明确边界（首版未覆盖）

1. **E1 不含推进语义**：ServiceTask 完成后建待办/实例状态同步完全依赖 E7 桥；只开 E1 不开 E7，token 会流到下一节点但 SB 台账不更新。
2. **翻转后必须重新发布**：注入发生在 `deploy`/`deployForTest` 时，开关翻转不会影响已部署的旧工件；含 ServiceTask 的流程须在设计器重新点「发布」（正式）/「测试」（测试）才会带上 delegate。
3. **跨租户风险**：`WfCustomActionServiceImpl.getByKey` 无租户过滤，自动动作按 actionKey 取注册记录时需依赖 action-gate 的租户校验兜底，务必先开 `action-gate`。
4. **取第一个 actionKey**：一个 ServiceTask 挂多个 customAction 时只取首个有效项（与"一个自动节点一个动作"语义一致）。
5. **无重试**：同步链路无 deadletter、无重试；动作本身须幂等或自行处理失败。

### 9.5 放量前提（当前全关，对生产零影响）

- 先开 `action-gate`（测试态自动动作受闸门 + 租户校验）
- 开 `service-task-exec`，并填 `service-task-exec-gray-def-ids` 灰度 1 个 defId，对含 ServiceTask 的流程**重新发布**
- 开 `engine-auto-todo-bridge` 并填 `engine-auto-todo-gray-def-ids` 灰度同一 defId，验证动作执行 + E7 待办同步 + 无事件循环/待办重复
- 通过后再清空两个名单转全量
- ⚠️ Nacos 若配了 `blade.workflow.compat`，以 Nacos 为准，需同步

### 9.6 dev 真实放量验证（2026-11-18 已实测通过并回滚）

> 在共享 dev 环境用**命令行参数覆盖三开关**（`action-gate` / `service-task-exec` / `engine-auto-todo-bridge` 全 true，不改任何配置文件，回滚=去参数重启），构造含 ServiceTask 的正式流程端到端跑通。**验证后已清理全部测试数据、去掉参数重启恢复全关**。

**（1）实测发现的阻断缺陷并已修复**：开启 `engine-auto-todo-bridge` 后服务**启动失败**——`WfEngineAutoTodoBridgeListener` 构造期强依赖 `IWfInstanceService`，而后者依赖 `ProcessService→processEngine`，`FlowableConfig#processEngineConfiguration` 又在装配期经 `ObjectProvider.getIfAvailable()` 解析该 listener，形成构造器循环依赖（E7 默认关时 listener bean 不存在，从未暴露）。
**修法**：listener 对 `IWfInstanceService` 的依赖改为 `ObjectProvider<IWfInstanceService>`，事件回调时（运行期、上下文就绪）才 `getObject()`，打断构造期环。同步更新 E7 单测/H2 集成测试的构造入参（`lenient` stub 规避早退分支 unnecessary stubbing）。相关测试 6+2+6+3+2=19 例全过。

**（2）端到端三层证据**（测试流程：`start → 创建UserTask → ServiceTask(customAction=demo_log_field) → 审批UserTask → end`，正式实例 is_test=0）：

| 层 | 证据 |
|---|---|
| 部署期注入 | `[e1-service-task] ServiceTask delegateExpression 注入完成. count=1`；deploymentId=517501 |
| 运行期 delegate | `[e1-service-task] 执行自定义动作 ... nodeKey=ServiceTask_Auto, actionKey=demo_log_field` → `[示例自定义接口动作] ... value=E1放量验证-表单字段值`（真实从 wf_form_snapshot 取到值）→ `自定义接口动作执行完成 result=ok: fwlcbt=...` |
| E7 待办同步 | `[e7-bridge] 自动节点完成后同步成功 ... activity=ServiceTask_Auto`；`ACT_RU_TASK` 的 `Task_Approve` 带 `BIZ_TASK_ID_`/`BIZ_ASSIGNEE_=676`；待办通知已投递；实例 current_node_key 流转到 `Task_Approve` |

> **待办真源在 ACT 表**：新模型下 `wf_task`/`wf_ext_task` 均为 0 行，业务待办列（`BIZ_TASK_ID_`/`BIZ_ASSIGNEE_`/`BUSINESS_ID_`）直接写在 `ACT_RU_TASK`。核验待办不要只查 wf_task。

**（3）清理与回滚**：已删除测试造的 1 条流程定义 + 1 条注册动作 + 1 个实例 + 1 条业务行 + 对应引擎部署（按外键依赖顺序：ACT_RU→ACT_HI→ACT_RE→业务表）；`e1_check_clean.py` 核验 6 类表残留均为 0；去参数重启后 `Started: True` / E7 listener 未注册 / 无启动失败。dev 恢复默认全关。

### 9.7 零回归 E2E 重建与复跑（2026-11-18）

> 原 `log/e2e_freeflow_full.py`（26 项）已不在磁盘，**重建为等效脚本 `log/e2e_freeflow_regression.py`（31 项）**并复跑通过，用于在 E1+E7 代码已进 dev（开关全关）的状态下证明既有自由流程行为未被破坏。

**覆盖场景与断言（31/31 PASS）**：发起不秒归档（status=0）、首步绑定池内节点（Free_1）、步骤编排保存/回显/版本号、下一步候选（池容量 5、排除当前=4 个）、多步推进（Free_1→Free_2→Free_3）、自选办理人（`nextAssigneeIds` 生效）、退回重提（退回 Free_1 → 重提继续 Free_2）、单步归档（status=1 / end_time 非空 / current_node_key 置空 / ACT_HI_PROCINST 终态）、超容量拒绝（6 步 > 池 5 → 400「超过预置节点池容量」且实例未破坏）。脚本自带清理，复跑后实例/业务行/引擎实例残留均为 0。

**重建中踩到并已固化进脚本的三个事实**（避免后续重复踩坑）：

1. **业务表名以 `workflow_bill.table_name` 为准**，不是 `formtable_main_{formId}` 拼接（该 formId=2109677294030196738 实际对应 `formtable_main_10`）。
2. **自由流程的首步直接绑定池内节点（`Free_1`）**，创建节点不单独停留；故发起后 `current_node_key` 已是 `Free_1`。
3. **下一步候选排除当前节点**（当前在 `Free_1` → 候选 4 个而非 5 个）；「自选下一步」由 `POST /free-flow/save` 编排的步骤顺序决定，`ApproveDTO.nextNodeKey` 在自由流程分支**不被读取**。

### 9.8 E1 灰度能力与引擎变量透传（2026-11-18 补强）

> 复核 §9.5 放量纪律时发现两处**实现与文档不一致 / 能力缺口**，本轮补齐。

**（1）E1 缺灰度白名单（实现与文档不一致，已修）**
§9.5 原文要求「先填灰度 defId 验证」，但那是 E7 的 `engine-auto-todo-gray-def-ids`；**E1 的 `service-task-exec` 原本是纯全局开关**——一旦打开，所有含 ServiceTask 的流程在重新发布后都会被注入 delegate，**无法按流程灰度**，与「先灰度验证再转全量」的放量纪律冲突。

补 `service-task-exec-gray-def-ids`（与 E7 对称），判定点两处（双保险）：
- **部署期**（`injectServiceTaskDelegate(bpmnXml, defId)`）：名单外的定义**不注入** delegate，保持引擎原生 ServiceTask 语义；
- **运行期**（`WfServiceTaskDelegate`）：名单外直接跳过——覆盖「工件在开关打开前已按旧配置部署、之后才打开开关」的场景（那批工件的 ServiceTask 里已带 delegateExpression，只靠部署期过滤会照常触发动作）。

**（2）引擎变量未透传给动作（勘察阶段已列风险，本轮补齐）**
原 `WfActionRequest` 只有 `formData`（表单数据）与 `params`（注册参数），动作拿不到流程变量。现 `WfActionRequest` 新增 `engineVariables`，ServiceTask delegate 把 `DelegateExecution.getVariables()` 快照透传下去；与 `formData` 分开不混用（表单按字段名取、引擎变量按变量名取）。人工节点场景该字段为 null，**既有动作行为完全不变**。

**改动清单**：
| 文件 | 改动 |
|---|---|
| `config/WfFlowtableCompatProperties.java` | 新增 `serviceTaskExecGrayDefIds=[]`（显式初始化，避免 null） |
| `resources/application.yml` | compat 段加 `service-task-exec-gray-def-ids: []` + 灰度说明 |
| `service/impl/WfDefinitionServiceImpl.java` | `injectServiceTaskDelegate` 增 defId 形参 + 部署期灰度判定；两处调用点传 defId |
| `action/WfServiceTaskDelegate.java` | 运行期灰度判定 `inGray()`；`engineVariables()` 读取引擎变量并透传 |
| `action/WfActionExecutor.java` | `execute` / `doCustomAction` 增 `engineVars` 形参（**参数传递，非实例字段**，避免 singleton 并发串味）；新增 `executeCustomAction(..., engineVars)` 入口 |
| `action/WfActionRequest.java` | 新增 `engineVariables` 字段 |

**（3）注入失败从静默回退改为 fail-fast（健壮性补强，复核时发现）**
原 `injectServiceTaskDelegate` 在注入异常时 `warn` 后回退原 XML → 部署**显示成功**，但运行期 token 卡在无实现的 ServiceTask（正是 `validateForDeploy` 要杜绝的"测试绿灯、正式跑不了"）。现改为：**BPMN 声明了 customAction 却注入失败 → 抛 `ServiceException` 中止发布**（事务回滚，迫使修复 BPMN 后再发布）；无 customAction 声明的原生 ServiceTask 仍 warn 回退（零影响）。

**验证**：全量单测 **387 例 0 失败 / 0 错误 / 1 跳过**（较上轮 +6：灰度名单外/名单内、引擎变量透传、注入期灰度内外、注入失败 fail-fast）。

### 9.9 S0 防呆：发布期未实现元素硬拦（2026-11-18 落地；前端白名单经确认暂缓）

> 按 S0→S6 顺序补齐 S0。S0 原定义 = 设计器元素白名单 + 发布期对未实现元素硬拦。本轮先落地**后端硬拦**（真正的安全边界），前端白名单作为体验层单独决策（见下）。

**（1）发布期硬拦清单**（`WfDefinitionServiceImpl.validateForDeploy`，遍历含子流程内全部元素）：

| 元素 | 处理 | 理由 |
|---|---|---|
| BusinessRuleTask | 硬拦（既有） | 正式环境无 Drools |
| ScriptTask | **新增硬拦** | S6 未实现，安全沙箱未就绪 |
| CallActivity | **新增硬拦** | S3 子流程语义未归一 |
| SubProcess | **新增硬拦** | S3 未实现 BPMN 子流程运行 |
| ReceiveTask | **新增硬拦** | S4 无消息唤醒，发起后永久停留 |
| SendTask | **warn → 硬拦** | 原静默降级为"自动通过"，语义欺骗 |
| ServiceTask | **条件硬拦** | `service-task-exec=false` 时拦（画了空跑）；开关打开后 E1 注入 delegate，放行 |
| ThrowEvent / timer 事件 / ManualTask | 保持 warn | 部分支持、有存量流程，避免误伤 |

**（2）影响面审计**（`log/s0_element_audit2.py`，dev 库 41 条定义）：40 条仅含 userTask + 排他网关；唯一含未实现元素的是 1 条 E2E 验证定义（businessRuleTask，本就是现有硬拦的测试对象）。**硬拦不影响任何存量可用流程**。

**（3）验证**：新增 `S0DeployGuardTest` 8 例（合法放行 + 6 类未实现元素硬拦 + ServiceTask 条件开/关）全过；全量单测 **395 例 0 失败 / 0 错误 / 1 跳过**。

**（4）前端白名单：用户决定暂缓（2026-11-18）**：勘察确认 `BpmnDesigner.tsx:367-382` 用 bpmn-js 17.11.1 原生 palette/replace 全量放开、无任何白名单。后端硬拦已能覆盖所有 BPMN 来源（画布/导入/API），是有效安全边界；前端白名单仅属**体验层**（让用户画不出来），非安全边界，且无法在本沙箱浏览器验证。经确认**暂缓不做**。后续若要做：用自定义 module 覆盖 `paletteProvider.getPaletteEntries` / `replaceMenuProvider.getPopupMenuEntries` 按 type 过滤，或 `eventBus.on('commandStack.*.preExecute')` 拦截，需在浏览器联调验证。

> 后续若要做前端白名单：推荐用自定义 module 覆盖 `paletteProvider.getPaletteEntries` 与 `replaceMenuProvider.getPopupMenuEntries`，按元素 type 过滤；或退一步用 `eventBus.on('commandStack.*.preExecute')` 拦截创建/替换并提示。需在前端 dev server 验证。

### 9.10 S3 timer：业务侧轮询绕开 AsyncExecutor（2026-11-18 落地）

> 按 S0→S6 顺序进入 S3，用户拍板「先做 timer，子流程后置」。勘察确认 timer 此前是**设计预留、实现缺失**：warn 文案与测试态降级清单都写"timer 走 WfTimeoutJob 轮询（S3）"，但 WfTimeoutJob 实际只扫 `ACT_RU_TASK.DUE_DATE_`，与 `timerEventDefinition` 零关联。

**（1）技术前提实测**（`S3TimerJobProbeTest`，H2 + 真实 Flowable，AsyncExecutor 关闭）：
- ✅ 流程进入 timer 节点时，引擎**仍创建 timer job**（写入 `ACT_RU_TIMER_JOB`）——只是没有执行器消费；
- ❌ `managementService.executeJob(timerJobId)` 报 `JobNotFound`（executeJob 只查 `ACT_RU_JOB`）；
- ✅ 正确入口：`moveTimerToExecutableJob(timerJobId)` → `executeJob(jobId)`，实测 token 流转到 timer 出口。

**（2）实现**：新增 `job/WfTimerJobScanner.java`，独立开关 `s3-timer-enabled`（默认 false，bean 都不存在）：
- 扫描 `ACT_RU_TIMER_JOB` 中 `DUEDATE_ <= now` 且未锁定的 job，逐个 `moveTimerToExecutableJob` + `executeJob`；
- **与 AsyncExecutor 互斥**（必须保持 `setAsyncExecutorActivate(false)`，否则重复消费）；
- D9 选主（MySQL 命名锁 `blade_wf_timer_scan`）；is_test 过滤（生产库 `ACT_HI_PROCINST.IS_TEST_`，纯净引擎库无此列时回退无过滤——测试态 timer 已被 neutralizeForTest 降级为 manualTask，不产生 timer job）；
- 单条失败不影响其余；不主动重试（Flowable `RETRIES_` 递减由引擎管理）。

**（3）配套**：`validateForDeploy` 的 timer warn 文案从"走 WfTimeoutJob 轮询"更正为"需开启 s3-timer-enabled，否则 timer 到期无人消费将真等待"。测试态降级为 manualTask 的既有行为**保持不变**（兼容现有测试流程）。

**（4）验证**：`WfTimerJobScannerIntegrationTest`（H2 端到端：部署 timer 边界流程 → 引擎建 job → doScan 驱动 → token 到 timer 出口）通过；全量单测 **397 例 0 失败 / 0 错误 / 1 跳过**。

**（5）启动互斥自检（2026-11-18 补，避免运维误配）**：
- 原 `FlowableConfig.setAsyncExecutorActivate(false)` 是硬编码，无法表达"两个开关同时 true"；现显式化为配置项 `blade.flowable.async-executor-enabled`（默认 false，**零行为变化**）；
- `FlowableConfig` 启动时检查 `isTimerConflict()`（`asyncExecutorEnabled && s3TimerEnabled`），冲突则 **`log.error("[启动自检][互斥冲突] ...")` 强提示**（不抛异常阻断启动），点名"两者都会消费 ACT_RU_TIMER_JOB，将导致 timer 节点被重复触发，请关闭其中一个"；
- `application.yml` 的 `blade.flowable` 段加 `async-executor-enabled: false` + 互斥/三项兜底注释；s3-timer 段注释同步指向该自检。
- 单测 `S3TimerMutualExclusionTest` 4 例覆盖四组开关组合；全量 **432 例 0 失败 / 0 错误 / 1 跳过**。

**（6）dev 真机验证（2026-11-20，单独构造流程跑一轮）**：
- 构造正式流程 `start → 创建UserTask → 审批UserTask（挂 PT5S 边界 timer）→ end；timer 出口 → 超时处理UserTask → end`；
- 用命令行参数 `s3-timer-enabled=true` + 扫描周期 20s 重启（**不改任何配置文件、AsyncExecutor 仍关**）；jar 为 11/19 构建，已含 S3 scanner；
- 部署正式版：timer 边界**保留**（仅 warn 不降级），deploymentId=530001；
- 发起实例后：
  - `t+0s`/`t+5s`：实例停 `Task_Approve`，timer job 存在（ID=530028，due=+5s），`Timer_Boundary` 在等待；
  - `t+10s`：**timer job 消失，`ACT_RU_TASK` 变为 `Task_Timeout`**，引擎历史 `Timer_Boundary` END_TIME_ 置位、`Task_Approve` 完成、`F4` 流转、`Task_Timeout` 激活；
  - 日志实证：`[scheduling-1] ... WfTimerJobScanner : [s3-timer] timer job 已执行. timerJobId=530028, procInstId=530004`；
- 测试数据已清理（6 类表残留 0），已去参数重启恢复全关（pid 30056，无 `[s3-timer]` 执行）。

**（7）遗留**：timer 的 `timeCycle`（重复触发）未做特殊处理（当前按到期执行一次，重复周期由 Flowable 自身重排）；子流程见 §9.11。

### 9.11 S3 子流程：业务层为准、补齐验证（2026-11-18）

> 用户拍板「业务层为主、callActivity 保持硬拦」。勘察确认二选一**实际已由代码选定**：业务层 `triggerSubflow` 链路完整（含归档回调 / autoForward / 防递归 / holdForSubflow 守卫），而 BPMN `callActivity` 代码侧零实现且已被 S0 硬拦。真正的缺口是**这条已上线链路无任何单测覆盖**。

**新增单测（14 例，纯 Mockito 无外部依赖）**：

| 文件 | 覆盖 |
|---|---|
| `WfSubflowServiceTest`（11 例） | 归档回调（无记录 / 幂等 / 置位归档时间 / 未全归档等待 / 全归档汇总日志）、autoForward 唤醒（运行中 advance / 已归档不重复推进）、测试态不下发提醒、holdForSubflow 守卫（null / 有 pending 阻塞 / 无 pending 不阻塞） |
| `WfSubflowRecursionTest`（3 例） | 防自引用（flowKey == 本流程 procKey 跳过）、防深递归（嵌套达 MAX_SUBFLOW_DEPTH=5 跳过）、正常场景发起并 record 主/子关系 |

**验证**：全量单测 **411 例 0 失败 / 0 错误 / 1 跳过**（较上轮 +14）。

**结论**：业务层子流程链路**代码无需改动**，仅补验证；callActivity/SubProcess 继续由 S0 发布期硬拦（见 §9.9 `S0DeployGuardTest`）。S3 至此完成（timer + 子流程验证）。

### 9.12 S4 ReceiveTask/消息唤醒（2026-11-18 落地，用户拍板"两者都做"）

> 勘察确认 S4 此前是**设计预留、实现缺失**：全仓 `messageEventReceived`/`trigger` 零调用，ReceiveTask 在 S0 被硬拦。本轮同时实现 ReceiveTask.trigger + messageEventReceived + 通用外部唤醒入口。

**（1）核心服务** `IWfWakeupService` / `WfWakeupServiceImpl`：
- `triggerReceive(engineInstId, nodeKey, vars)` → 查 `ACT_RU_EXECUTION` 定位等待 execution → `runtimeService.trigger(executionId, vars)`；
- `signalMessage(engineInstId, messageName, vars)` → 查 execution → `runtimeService.messageEventReceived(messageName, executionId, vars)`（**Flowable 8.1 签名 messageName 在前**，已实测确认）；
- 独立开关 `s4-wakeup-enabled=false` 默认关，未开启时直接抛 ServiceException；
- 幂等由引擎保证：重复唤醒找不到等待 execution 时返回 false，无副作用。

**（2）入口分层**：
- 内部 `WfWakeupController`（`/wakeup/trigger`、`/wakeup/message`）：`@PreAuth(HAS_ROLE_WORKFLOW)`，仅流程管理员——**不挂 `/task/**` 的 HAS_AUTH**，避免任何登录用户唤醒任意流程；
- 外部 `WfWakeupExternalController`（`/wakeup/external`）：`@ConditionalOnProperty(wakeup.external-enabled=true)` 默认**bean 都不存在**（404 而非运行时拒绝），开启后 `permitAll` + HMAC-SHA256 验签（`X-WF-Nonce` / `X-WF-Signature`）。

**（3）S0 联动**：ReceiveTask 硬拦改为**条件硬拦**（`s4-wakeup-enabled=false` 时拦，开后放行），与 ServiceTask/E1 同范式。

**（4）⚠️ 外部入口启用前置（必须同时满足，否则验签请求被网关拦截）**：
1. 配 `blade.workflow.wakeup.secret`；
2. 网关 `blade.secure.skip-url` **与** `blade.xss.skip-url` 同时放行 `/blade-workflow/wakeup/external`；
3. 开 `blade.workflow.compat.s4-wakeup-enabled=true` + `blade.workflow.wakeup.external-enabled=true`。
> 本轮**未改网关配置**（共享配置，需用户决策）。

**（5）验证**：`WfWakeupServiceIntegrationTest`（H2 真实引擎，两种唤醒均推进到 `afterWake`）2/2；`S0DeployGuardTest` 9/9（含 ReceiveTask 条件放行）；全量单测 **414 例 0 失败 / 0 错误 / 1 跳过**。

**（6）遗留**：message 事件按 messageName 模糊匹配 ACT_ID_ 定位 execution（保守口径）；E7 listener 白名单仍不含 receiveTask/message——唤醒后 token 流转到下一 UserTask 的台账同步由 `advance()` 现有链路覆盖，未新增 listener 类型。

### 9.13 S5 前置兜底：deadletter 告警三件套（2026-11-18 落地）

> 用户拍板先补 S5（AsyncExecutor）前置三项兜底中的「作业积压/失败告警」。方案C §3.4 明确三项缺一不可，死信非空 = 作业重试耗尽 = 台账可能漏更新，需人工介入。

**（1）三件套实现**：

| 件 | 落点 |
|---|---|
| 死信查询入口 | `IProcessService` 新增 `listDeadLetterJobs()` / `retryDeadLetterJob(jobId, retries)` / `deleteDeadLetterJob(jobId)`；`ProcessServiceImpl` 基于 `managementService.createDeadLetterJobQuery()` / `moveDeadLetterJobToExecutableJob(id, retries)` / `deleteDeadLetterJob(id)` 实现；新增 `DeadLetterJobVO`（id/流程实例/定义/元素/重试数/异常消息） |
| 告警投递 | 新增 `job/WfDeadLetterJobScanner`（开关 `deadletter-alarm-enabled` 默认 false）：扫描死信非空即 `log.error` + 经 `INoticeClient` 投递管理员（接收人 `blade.workflow.deadletter-alarm-user-ids` 逗号分隔，为空则只记日志、零外部依赖）；**只告警不自动重投**（避免掩盖真实故障） |
| 重试机制 | 人工处置入口 `retryDeadLetterJob`（重投并重置重试次数）+ `deleteDeadLetterJob`（确认修复后清理）；扫描器 D9 选主 + 单条容错 + 通知失败不影响主流程 |

**（2）关键设计**：与 `WfTimerJobScanner` 同范式（ManagementService + 选主锁 + package-private `doScan()` 可测）；开关关闭时 bean 都不存在；告警接收人配置非法只 warn 不投递；查询失败本轮跳过。

**（3）验证**：`WfDeadLetterAlarmTest` 6/6（空/非空/未配接收人/查询失败/投递失败/配置非法）+ `WfDeadLetterJobAccessTest` 3/3（查询空/重投/删除委托）；全量单测 **423 例 0 失败 / 0 错误 / 1 跳过**（`forkCount=0` 规避 JVM fork 偶发崩溃——环境问题非代码）。

**（4）S5 前置现状**：三项兜底中「deadletter 告警」已补；「C1 最终一致」（`strict-event-fail=false` 已是最终一致口径）、「常驻对账修复」（`HistoricalDriftFixer` 当前仍是一次性脚本，未升级为定时巡检）**尚未补齐**——按纪律 S5 AsyncExecutor 仍不得开启。

### 9.14 S5 前置兜底收官：常驻漂移对账（2026-11-18）

> 补齐方案C §3.4 三项兜底的最后一项「常驻对账修复」。此前 `HistoricalDriftFixer` 是一次性 CommandLineRunner 脚本，不满足「常驻」要求。

**（1）实现** `job/WfDriftReconciler.java`，开关 `drift-reconcile-enabled`（默认 false，bean 都不存在）：
- 定时扫描**最近 30 分钟**进入终态（通过/不通过/撤销）的业务实例（增量时间窗，非全量）；
- 逐个用 `IProcessService.isProcessInstanceEnded` 确认引擎确实仍活跃才 `deleteProcessInstance` 修复——避免对已清理实例做无谓调用；
- is_test=0 隔离；单条失败不影响其余；查询失败本轮跳过。
- 与一次性脚本 `HistoricalDriftFixer` 互补：后者用于存量全量清理，本类用于增量常驻兜底。

**（2）测试笔记**：MyBatis-Plus `LambdaQueryWrapper` 在纯 Mockito 单测中因 TableInfo 缓存未初始化会在构造时抛 `MybatisPlusException`（生产 Spring 容器无此问题）。故把查询抽成 package-private `queryTerminal(Date)`，测试用 spy 替换，**不为单测改生产查询逻辑**。

**（3）验证**：`WfDriftReconcileTest` 5/5（空/已结束不修/漂移修复/单条失败容错/查询失败）；全量单测 **428 例 0 失败 / 0 错误 / 1 跳过**。

**（4）S5 开启条件评估**：三项兜底现已齐备——C1 最终一致（`strict-event-fail=false` 已是该口径）、常驻对账（本类）、deadletter 告警（§9.13）。**按纪律 S5 AsyncExecutor 已具备开启前提**，但开启会改变全平台 timer/异步行为，且 S3 timer 已用 `WfTimerJobScanner` 轮询方案绕开 AsyncExecutor——是否真要开 S5 属产品决策，本轮不擅自翻转 `setAsyncExecutorActivate(false)`。

### 9.15 S6 ScriptTask：Groovy + 沙箱加固（2026-11-18 落地）

> 用户拍板脚本引擎选型：**Groovy + 沙箱加固**（能力最强，引入 groovy-jsr223 显式依赖 + SecureASTCustomizer 白名单）。这是 S0→S6 路线的最后一步。

**（1）依赖**：`blade-workflow/pom.xml` 新增 `org.apache.groovy:groovy-jsr223:4.0.28` + `groovy:4.0.28`（本地离线仓库已有，可 `-o` 构建）。

**（2）实现**：
- 新增 `script/S6SandboxedGroovyEngineFactory.java`：产出的 JSR-223 ScriptEngine 用带 `SecureASTCustomizer` 白名单的 `GroovyClassLoader` 编译脚本（Groovy 4 只有 `GroovyScriptEngineImpl(GroovyClassLoader)` 单参构造器，故走 loader 注入）。
- 沙箱白名单：禁方法定义/闭包/包声明/静态导入；导入限 `java.lang/math/util/time`；接收者类限 String/Number/BigDecimal/List/Map/Date/时间类等纯值类（无 IO/网络/反射）。**重要**：`setIndirectImportCheckEnabled(true)` 在 Groovy 4 下对纯表达式（`1+2*3`）也误拦，实测后关闭——危险类仍由 receivers/imports 白名单在编译期拦截。
- `FlowableConfig`：开关 `s6-script-task-enabled=true` 时，构造 `JSR223FlowableScriptEngine`，对其 `ScriptEngineManager` 按 `groovy`（Flowable 小写常量）与 `Groovy`（原生工厂名）**两个名字**注册沙箱工厂，再 `setScriptEngine`——确保 `scriptFormat` 两种大小写写法都命中沙箱，不退化到无沙箱原生引擎。
- `validateForDeploy`：ScriptTask 硬拦改为**条件化**——S6 开关关闭时拦（默认零行为变化），打开后放行交沙箱引擎执行。
- 配置：`application.yml` + compat 属性类新增 `s6-script-task-enabled: false`（默认关）。

**（3）与 E1 的边界**：ScriptTask 走 Flowable 原生脚本执行（`scriptFormat` + `<script>` + `resultVariable`），**不能复用** E1 的 `delegateExpression` 注入（ScriptTask 模型无 implementation 字段），也**不并入** IWfCustomAction 注册表（注册表是字符串命令前缀分派，与任意 Groovy 源码不兼容）。

**（4）验证**：
- `S6SandboxedGroovyEngineTest` 8/8：合法脚本（四则/字符串/BigDecimal/变量绑定）可执行；危险脚本（System.exit/方法定义/闭包/文件 IO）均被 `ScriptException` 拦截；引擎名大小写两种写法都走沙箱。
- `S6ScriptTaskIntegrationTest` 2/2（H2 + 真实 Flowable）：合法脚本 `6*7=42` 写入 `calculated` 变量并流转到下一节点；`System.exit(1)` 被引擎拒绝、流程未启动。
- 全量单测 **442 例 0 失败 / 0 错误 / 1 跳过**。

**（5）遗留/边界**：沙箱是**编译期 AST 白名单**，非 JVM SecurityManager（后者已废弃）；白名单外的类/方法需按需扩展 `ALLOWED_RECEIVERS`/`ALLOWED_STAR_IMPORTS` 并回归。`timeCycle` 重复脚本、DMN（决策表）未做。

### 9.16 启动阻断修复：application.yml S4 配置段缩进错误（2026-11-18）

> 用户重启全套 dev 服务后发现 **blade-workflow 未启动**（无进程、8111 无监听），其它服务（auth/gateway 等）正常。单独启动报 `while parsing a block mapping` / `expected <block end>, but found '<block mapping start>'`（application.yml line 244）。

**根因**：S4 外部唤醒配置合入 `application.yml` 时缩进错乱——
- `deadletter-alarm-user-ids: ''` 写成 4 空格（compat 段同级），提前结束了 `blade.workflow.compat` 段；
- `external-enabled`/`secret`（属 `blade.workflow.wakeup`）写成 6 空格且**缺 `wakeup:` 父键**，挂在已结束的 compat 段后。

spring 启动期加载 yml 即抛 snakeyaml 异常，**任何开关组合下都无法启动**（与 compat 开关无关）。

**修法**：`deadletter-alarm-user-ids` 归位为 6 空格（compat 段内、注释前置）；新增 `wakeup:` 父键（4 空格）并把 `external-enabled`/`secret` 归其下（6 空格）。重新 `clean install` 打包 + 无参启动验证：

- `Started WorkflowApplication` = true
- YAML 解析错误消失、E7 listener 未注册（全关默认）、无互斥冲突 ERROR
- 8111 监听正常（pid 38436）

**教训与守卫**：多阶段配置合入时，`application.yml` 的缩进/父键必须由构建或单测校验——YAML 语法错误在启动期才暴露，且会让整个服务不可用。已补 `ApplicationYamlParseTest`（1/1 PASS）：用 Spring `YamlPropertySourceLoader` 在单测阶段 load 一遍主配置，把阻断前移到 CI。

### 9.17 S5 AsyncExecutor 开启实测（2026-11-19，dev 已开启）

> 用户拍板开启 S5（三项兜底已齐、与 S3 timer 互斥已自检）。**用命令行参数覆盖翻转，不改任何配置文件**；回滚=去参数重启（`log/s5_rollback.ps1`）。

**（1）翻参数**（`log/s5_start.ps1`）：
```
--blade.flowable.async-executor-enabled=true
--blade.workflow.compat.deadletter-alarm-enabled=true
--blade.workflow.compat.drift-reconcile-enabled=true
```
`strict-event-fail` 保持默认 false（C1 最终一致口径）、`s3-timer-enabled` 保持默认 false（互斥）。虽然 yml 里两个兜底开关默认 false，但命令行参数优先级更高，实测确认两个扫描器 bean 已随开关激活——**满足"三项兜底真正运行"而不仅是"代码存在"**。

**（2）启动实测**：日志确认 AsyncExecutor 真正激活——`Starting up the async job executor [DefaultAsyncJobExecutor]`、`AcquireTimerJobsRunnable/AcquireAsyncJobsDueRunnable starting`；互斥自检无冲突（s3-timer 保持关）；无 APPLICATION FAILED。

**（3）开启即暴露的存量问题（已处理）**：AsyncExecutor 首次扫描捞起 1 个**存量孤儿 job**（`ID=470219`，`HANDLER_TYPE_=parallel-multi-instance-complete`，`EXECUTION_ID_=470196` 已不存在、父流程历史也被清理）→ 执行时 `executionEntity=null` 触发 NPE。判据（execution 不存在 + 无活动任务 + 历史实例已结束或已被清理）确认彻底孤儿后删除；清理后 ACT_RU_JOB/DEADLETTER 均 0、无新失败。
> 这验证了 deadletter 兜底的必要性：此类残留 job 在 AsyncExecutor 关闭时长期潜伏，一旦开启立刻暴露；有兜底则重试耗尽进死信并告警，无兜底则静默失败。

**（4）零回归**：E2E 31/31 PASS（S5 开启状态）；4 类引擎作业（JOB/TIMER/DEADLETTER/SUSPENDED）流转后全 0，无残留、无死信；测试数据 0 残留。

**（5）当前状态**：dev workflow（pid 23816）**保持 S5 开启**。回滚脚本 `log/s5_rollback.ps1` 已备好，去参数重启即恢复默认全关。

> **状态更正（2026-11-19 11:10 观察）**：dev 服务随后被**无参重启**（pid 42288，`application.yml` 各开关默认 false），当前 S5 实际已关闭、AsyncExecutor 未激活、所有 compat 开关全关。§9.17 记载的"保持 S5 开启"是 01:06 那次（pid 23816）的当时状态。需要 S5 时重新执行 `log/s5_start.ps1` 翻参数即可。

### 9.18 真机完整流程验证：全开关共存（2026-11-20）

> 用户授权「真机跑通完整流程（含新能力）」，验证**画布既有功能 + 全部新增机制共存**不破坏既有报销流程。

**（1）开关翻转**（`log/full_start.ps1`，命令行参数覆盖，不改配置文件）：一次性开 8 个——`action-gate` / `service-task-exec` / `engine-auto-todo-bridge` / `s4-wakeup-enabled` / `s6-script-task-enabled` / `async-executor-enabled` / `deadletter-alarm-enabled` / `drift-reconcile-enabled`；`s3-timer` 与 AsyncExecutor 互斥保持关、`strict-event-fail` 保持默认 false。启动健康：AsyncExecutor 激活、E7 listener 注册、无互斥冲突、无 APPLICATION FAILED。

**（2）验证对象**：现有 14 节点报销流程 `2117206127369306114`（实为 10 UserTask + 3 排他网关，无 ServiceTask/timer/ScriptTask——故 E1/S6 对它无直接影响，验证的是**新机制共存下既有画布流程零回归**）。

**（3）结果：9/9 PASS**（`log/full_flow_test.py`）：重新发布成功（S0 门禁通过）→ 发起实例 → 走完 9 步审批（DeptLeader→DeptDirector→FinAudit×3→FinManager→Supplement→Compliance×2）→ 实例 status=1 / end_time 非空 / 引擎实例 END_TIME_ 写入 / 无残留待办。

**（4）残留核验**（`log/full_cleanup_check.py`）：ACT_RU_JOB / TIMER_JOB / DEADLETTER_JOB / SUSPENDED_JOB **全 0**（AsyncExecutor 开着也无死信）；测试实例 / 业务行 **0 残留**。

**（5）回滚与副作用**：`log/full_rollback.ps1` 去参数重启恢复全关（pid 6584，E7 未注册 / AsyncExecutor 未激活 / 无失败）。唯一遗留副作用：报销流程被重新发布为 v6（`proc_def_id=comprehensive_expense_approval_full:6:527504`）——发布动作本身，BPMN 内容未变、运行中实例原本为 0，在途按 procDefId 隔离不受影响。

**文档版本**：1.17.0（§9.18 真机完整流程全开关共存验证 9/9 PASS 并回滚）
**发布日期**：2026-11-17
**作者**：CodeBuddy AI
**适用范围**：BPMN 全量元素启用决策、内核级改造排期、与原有流程语义兼容设计
