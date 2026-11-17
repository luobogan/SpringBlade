# 全量 BPMN 元素启用与原有流程语义兼容架构分析

> **总索引**：[工作流分析文档总索引.md](工作流分析文档总索引.md)（五份递进式分析文档的统一入口）
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

**文档版本**：1.0.0
**发布日期**：2026-11-17
**作者**：CodeBuddy AI
**适用范围**：BPMN 全量元素启用决策、内核级改造排期、与原有流程语义兼容设计
