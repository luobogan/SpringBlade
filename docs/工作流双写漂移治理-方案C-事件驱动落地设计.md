# 工作流双写漂移治理 · 方案 C 落地设计（事件驱动反写）

> 适用范围：`blade-workflow` 模块（SpringBlade + Flowable 7.1.0）
> 前置：方案 A（`WfWriteHelper` 双写收口）**已落地**
> 目标：把「状态」从 **手工两处写** 改为 **引擎单写 + 台账派生**，从结构上消灭状态镜像类漂移

---

## 1. 目标与定位

### 1.1 要解决什么
当前每次生命周期操作都要在**两处**写同一份「状态」：

- 业务代码写 `wf_instance.status` / `wf_task.status`
- 引擎写 `ACT_RU_*` / `ACT_HI_*`

两者是**同一事实的两份镜像**，靠"开发者记得写两边"维持一致 —— 这正是历史 P0（撤销/撤回/终止/暂停/恢复只改 `wf_*`）的成因。

方案 A 已把「两边都写」**收口**成不可跳过的一步（防复发）；方案 C 要更进一步：让**引擎成为状态的唯一写入方**，`wf_*` 的状态由 Flowable 事件**派生**出来。

### 1.2 不解决什么（明确边界）
以下 OA 语义在引擎里**没有对应事件源**，仍由业务代码显式写（它们本来就是单写，不构成漂移源）：

| OA 语义 | 为什么监听覆盖不了 |
|---|---|
| 协办(7) / 抄送(8) / 传阅(11) | 同一 `engine_task_id` 的多行 overlay；引擎只有一个 task 事件 |
| 草稿(5) | 引擎尚未发起，无任何事件 |
| `wf_approval_log` 审批意见 | BPMN 无"意见"概念（引擎有 `ACT_HI_COMMENT`，本模块未采用） |
| `wf_form_snapshot` 表单快照 | 表单数据，非流程事件 |
| 部门数据权限 / 催办 / 已读 | 纯 OA 台账属性 |

> **一句话**：C 消灭的是「状态镜像」的双写，不是全部双写。OA 独占写仍是单写。

---

## 2. 监听选型：用哪种监听

### 2.1 三种候选与结论

| 候选 | 形态 | 本项目结论 |
|---|---|---|
| BPMN 内嵌 `ExecutionListener` / `TaskListener` | 写在 BPMN XML 元素上 | ❌ **不用**。BPMN 由 bpmn-js 画布动态生成、经 `IProcessService.deployProcess` 部署；要给每个节点挂监听就必须改 XML 生成逻辑，成本高且易漏 |
| **全局 `FlowableEventListener`** | 引擎级注册，与流程定义无关 | ✅ **采用**。一处注册覆盖全部流程，与动态 BPMN 完全解耦 |
| `HistoryEventListener` / 异步历史 | 历史落库时触发 | ❌ **不用**。异步历史另起事务，破坏原子性（见 §4） |

### 2.2 注册方式（两种，按依赖取舍）

**方式一（推荐）：只依赖 `wf_*` Mapper 的纯台账监听 → 直接装配进引擎配置**

```java
// FlowableConfig#processEngineConfiguration()
configuration.setEventListeners(List.of(wfLedgerListener));
```
监听只注入 `WfInstanceMapper` / `WfTaskMapper` / `WfApprovalLogMapper`，**不依赖任何引擎 Service** → 无循环依赖。

**方式二：监听需要查引擎（如 `RuntimeService`）→ 引擎就绪后注册**

```java
@Component
@DependsOn("processEngine")   // 避免 config → listener → RuntimeService → processEngine 循环
public class WfListenerRegistrar implements ApplicationRunner {
    public void run(ApplicationArguments args) {
        runtimeService.addEventListener(wfLedgerListener,
            FlowableEngineEventType.PROCESS_CANCELLED, ...);   // 按需指定类型
    }
}
```

> ⚠️ 若监听注入了 `RuntimeService`/`TaskService`，**不能**走方式一的 `setEventListeners` —— 会与 `processEngineConfiguration` 形成构造期循环依赖。

### 2.3 事件类型对照表

实现类：`org.flowable.common.engine.api.delegate.event.FlowableEventListener`
事件常量：`org.flowable.common.engine.api.delegate.event.FlowableEngineEventType`

| 引擎事件 | 派生的台账写入 | 现状对应 |
|---|---|---|
| `PROCESS_STARTED` | `wf_instance` 建行 + 回填 `engine_inst_id` | `start` 显式写 |
| `PROCESS_COMPLETED` | `wf_instance.status=通过1` + `end_time`；关残留待办 | `advance` 归档 |
| `PROCESS_CANCELLED` | `wf_instance.status=撤销3` + `end_time`；关全部待办 | `terminate`（撤销/撤回） |
| `ENTITY_SUSPENDED`（ProcessInstance） | `wf_instance.status=暂停4` | `stop` |
| `ENTITY_ACTIVATED`（ProcessInstance） | `wf_instance.status=运行中0` | `resume` |
| `TASK_CREATED` | `wf_task` 建待办行（关联 `engine_task_id`） | `advance` 生成待办 |
| `TASK_ASSIGNED` | 回填 `wf_task.assignee` | 解析操作者时写 |
| `TASK_COMPLETED` | `wf_task.status=已办2` + `operate_time` | `approve` / `reject` |

> **实现时需核对**：上述常量名以 Flowable **7.1.0** 的 `FlowableEngineEventType` 为准（历史/实体类事件在不同版本间有增改）。`PROCESS_CANCELLED` 是本方案最关键的一个 —— 它对应 `deleteProcessInstance`，而非 `PROCESS_COMPLETED`。

---

## 3. 同步还是异步：**必须同步**

### 3.1 结论
**全部同步**，禁止任何异步化。

### 3.2 为什么
`FlowableEventListener#onEvent` 在引擎 Command 的**同一线程、同一事务**内被 dispatch。因此在监听里直接写 `wf_*`：

```
同一个 Spring 事务
├── 引擎写 ACT_*      （引擎内部）
└── 监听写 wf_*       （同 DataSource，加入同一事务）
     → 一起提交 / 一起回滚
```

这是「原子」的唯一来源。一旦异步，事务就裂成两个，漂移风险反而**比现在更高**。

### 3.3 必须守住的前提

| 前提 | 现状 | 要求 |
|---|---|---|
| `asyncExecutorActivate` | `FlowableConfig` 已设 **false** ✅ | 现状安全；**若要支持定时器边界事件则必须开启** —— 开启后事件改在 job 的独立事务派发，须同时满足 **§3.4 三项兜底** |
| `asyncHistoryEnabled`（异步历史） | 未开启 | **禁止开启**：`HISTORIC_*` 事件改由异步 job 写，跨事务且失败即漏派 |
| BPMN 节点 `flowable:async="true"` | 应无 | 禁止使用异步节点（治理文档 §4 已列为部署侧前提）；确需则见 §3.4 |
| 监听内部再异步 | — | **禁止** `@Async`、新线程、`@Transactional(REQUIRES_NEW)`、发 MQ 后落库 —— 都会脱离引擎事务 |
| `ACT_*` 与 `wf_*` 同 DataSource | 是（治理文档 §4） | 分库则本方案原子性不成立 |

### 3.4 异步 / 定时器路径的专门处理（开启 async executor 时必读）

#### 先纠正一个容易搞错的点
异步 job 执行时，Flowable 在 **job 自己的新事务**里执行命令，但**事件仍在该事务内同步派发** ——
即监听写 `wf_*` 与「这一次引擎写」**仍是同一事务、仍然原子**。
所以问题**不是**"监听脱离了引擎事务"，而是：

| # | 真实问题 | 说明 |
|---|---|---|
| 1 | **时间窗漂移（瞬时）** | 用户请求已提交，job 稍后才跑 → `wf_*` 滞后于 `ACT_*`。UI 短暂显示旧态 |
| 2 | **job 失败 → 永久漂移** ⚠️ | job 重试耗尽进入 `ACT_RU_DEADLETTER_JOB` → 事件**永不补发** → 台账永不更新。**唯一会留下永久伤的一类** |
| 3 | **同步 `advance` 读到旧节点** | `completeTask` 返回时引擎可能尚未真正推进，`advance` 按旧节点生成待办 → 落点错位（治理文档 §4 已列为部署侧前提，**与方案 C 无关的前置问题**） |

#### 必须同时满足的三项兜底（缺一不可）

1. **绝不使用 `HISTORIC_*` 事件**（不变，且更重要）
   异步历史（`asyncHistoryEnabled`）会另起事务写历史，**且失败即不补发**。
   只用 `PROCESS_*` / `TASK_*` / `ENTITY_*` 这类**运行态同步事件**。

2. **常驻对账修复（把"漏派"兜住）**
   把 `HistoricalDriftFixer` 从「一次性脚本」升级为**定时巡检 + 自动修复**任务
   （最小实现：定时跑 `drift_check.sql` 第 2/3/6 段，非零即告警）。
   这样即使 job 死掉导致事件漏派，也能被对账发现并修复 ——
   把强一致降级为 **最终一致 + 可观测 + 可修复**。

3. **作业积压 / 失败告警**
   监控 `ACT_RU_DEADLETTER_JOB`（> 0 即告警）与 `ACT_RU_TIMER_JOB` 积压量。
   deadletter 非空 = 台账可能已漏更新，是需人工介入的信号。

#### 配套建议
- 查询 / 展示一律以 `wf_*` 为准（方案 X），接受异步路径下的短暂延迟，不把中间态暴露给用户。
- 同一实例开启 **exclusive job**（Flowable 默认对同一实例的作业串行执行）→ 事件顺序不乱，避免乱序派生。
- 定时器边界事件（节点类型 5）所在流程，不要同时依赖同步 `advance` 的落点判断（见问题 3）。

#### 结论
- **当前**：`asyncExecutorActivate=false`，且定时器边界事件仅见于 `IProcessService#pendingJobCount` 的
  覆盖规划、主流程并未使用 → **现状安全，无需上述兜底**。
- **一旦要支持定时器边界事件**：必须「开启 async executor」+「C1 强一致下调为最终一致」
  +「常驻对账修复 + deadletter 告警」三者齐全。**缺任一 → 宁可不开异步**。

---

## 4. 事务一致性怎么保

### 4.1 基础（已具备）
`FlowableConfig` 已把项目主 `PlatformTransactionManager` 绑给引擎：

```java
configuration.setTransactionManager(transactionManager);
```

因此引擎写加入 Spring 事务；监听写 `wf_*` 同样在该事务内 → 同库时天然原子。

### 4.2 监听内的写法约束
- **直接**用 `wf_*` Mapper 写，**不要**经过 `@Transactional(REQUIRES_NEW)` 的自调用，也不要走 Feign / 消息。
- 不要把监听逻辑丢进 `WfInstanceServiceImpl`（会引入 Service 层循环依赖）；监听是独立 `@Component`。

### 4.3 异常策略（关键分叉）

| 策略 | 行为 | 评价 |
|---|---|---|
| **C1 强一致（推荐）** | 监听写台账失败 → 抛异常 → **整个引擎操作回滚** | ✅ 台账是权威（方案 X 定位），不允许"引擎动了、台账没动"。**仅承诺同步路径**；异步 / 定时器路径按 §3.4 降级为最终一致 |
| C2 弱一致 | 监听 try-catch 吞异常只记日志 | ❌ 会重新引入漂移（方向变为"引擎动了台账没动"），与治理目标冲突，不采用 |

> C1 的代价：台账写入异常会连带回滚用户操作。故必须先用 §6 阶段 1「影子模式」充分验证映射正确，再开写入。

### 4.4 幂等（必须）
事件可能因重试 / 并发重复投递，监听写台账必须幂等：
- 建行：按 `engine_inst_id` / `engine_task_id` **先查存在再插**，并对该列建唯一索引兜底。
- 更新：用「**目标态覆盖**」（`setStatus(已办)`），不用累加/自增。

### 4.5 一个易踩的坑
`PROCESS_CANCELLED` / `ENTITY_DELETED` 触发时，`ACT_RU_*` 行**可能已被删除**。监听里**不要**再回查 `ACT_RU_TASK` 判断存活 —— 一律使用**事件对象自带**的 `processInstanceId` / `taskId` 去关联 `wf_*`。

---

## 5. 与方案 A 的关系

**不是替代，是递进**：

```
阶段 A（已落地）：WfWriteHelper 把「台账 + 引擎」绑成一步 → 防漏写
阶段 C（本设计）：引擎事件派生台账状态          → 根本不用手写两边
```

C 逐步接管后，`WfWriteHelper` **保留**：继续承载 OA 独占写与兜底，并在 C 出问题时作为回退路径。

---

## 6. 落地步骤（灰度，按序推进）

| 阶段 | 动作 | 验收 |
|---|---|---|
| **0 前置** | 方案 A 收口（已完成）+ P0 存量清理（`HistoricalDriftFixer`） | `drift_check.sql` 第 6 段 = 0 |
| **1 影子（shadow）** | 注册监听，但**只比对不写入**：算出"应有的 wf_* 状态"与库里实际值比对，不一致打日志计数 | 跑一段时间，影子差异计数 = 0；`drift_check.sql` 无新增 |
| **2 双写校验** | 监听开启写入，业务代码原有显式写**先保留**（两者写同值，幂等） | 观察无异常、无差异告警 |
| **3 切单写** | 逐个摘掉业务代码里的状态写：`terminate` → `stop`/`resume` → `advance` 归档 | 每摘一个跑一次回归 |
| **4 复核** | `drift_check.sql` 复核 | 第 2/3/6 段 = 0 |

> 阶段 1 不可跳过 —— 它是 C1 强一致策略下唯一能在"开写之前"证明映射正确的手段。

---

## 7. 开关与回滚

- 监听注册用 `@ConditionalOnProperty(name = "workflow.ledger-listener.enabled", havingValue = "true")` 控制（先例：`HistoricalDriftFixer`）。
- **回滚代价极低**：关闭开关即回到方案 A 的显式双写，无需改代码、无需数据修复。
- 建议默认 **false**，仅在阶段 1 影子模式验证通过后逐环境打开。

---

## 8. 风险清单

| 风险 | 说明 | 缓解 |
|---|---|---|
| 监听异常回滚用户操作 | C1 强一致的固有代价 | 阶段 1 影子模式充分验证；开关一键回退 |
| Flowable 事件语义随版本变化 | 实体/历史事件常量在版本间有增改 | 锁定 7.1.0；升级 Flowable 时必须回归本监听 |
| 异步 job / 定时器边界事件跨事务 | 事件改在 job 的独立事务派发 → 时间窗漂移；job 失败进 deadletter → **永久漏派** | 见 **§3.4**：禁 `HISTORIC_*` 事件 + 常驻对账修复 + deadletter 告警，**三者缺一不可**（缺任一则不开异步） |
| 循环依赖 | 监听若注入引擎 Service 会与 `processEngineConfiguration` 形成环 | 见 §2.2 方式二；优先让监听只依赖 `wf_*` Mapper |
| 事件顺序 / 批量推进 | 一次 `completeTask` 可能连发多个事件 | 幂等（§4.4）+ 只用事件自带 id（§4.5） |

---

## 9. 交付物清单（实现阶段）

1. `WfLedgerListener`（`@Component`，实现 `FlowableEventListener`）—— 事件 → 台账派生。
2. `FlowableConfig` 或 `WfListenerRegistrar` 中的注册代码（§2.2）。
3. `workflow.ledger-listener.enabled` 配置项（默认 false）。
4. 影子模式比对日志（阶段 1）。
5. `wf_instance.engine_inst_id` / `wf_task.engine_task_id` 唯一索引（幂等兜底）。
6. `drift_check.sql` 复核记录。
7. **（开启 async executor / 定时器时必需，§3.4）** 常驻定时对账修复任务 + `ACT_RU_DEADLETTER_JOB` 告警。

---

## 10. 实现进度（阶段 1/2/3 已完成）

> 初版记录于 2026-09-26（阶段 1 骨架，默认关闭，只记日志不写库）。
> **更新于 2026-09-27：阶段 1 影子实测、阶段 2 真实反写、阶段 3 切单写均已落地，并回归通过。**

### 10.1 已落地
- `org.springblade.workflow.listener.WfEngineEventListener`：全局 `FlowableEventListener`，`@ConditionalOnProperty(name="blade.workflow.ledger-listener.enabled", havingValue="true", matchIfMissing=false)`，**当前在 `application.yml` 显式开启**。
  - `isFailOnException()=true`（**C1 强一致**，阶段 2 起由影子期的 `false` 改回）、`isFireOnTransactionLifecycleEvent()=false`、`getOnTransaction()=null`。
  - `onEvent` 内部 try-catch **已移除**：反写失败必须抛异常回滚引擎事务，否则会留下「引擎成/台账丢」的漂移（影子期才吞异常）。
- `org.springblade.workflow.service.helper.WfStateProjector`：注入 `WfInstanceMapper`/`WfTaskMapper`，按 `engine_inst_id`/`engine_task_id` **先查后写**幂等反写（§4.4）。
  - `PROCESS_STARTED` **不比对**：`engine_inst_id` 是业务在 `startProcessInstanceByKey` **返回后**才回填的，事件在引擎调用内部派发，此刻该列仍为 NULL，比对必产生 100% 假差异。
- `FlowableConfig#processEngineConfiguration`：`ObjectProvider<WfEngineEventListener>` 注册（§2.2 方式一：监听仅依赖 wf_* Mapper，无引擎 Service 依赖，避免循环依赖）；`asyncExecutorActivate=false` + `asyncHistoryEnabled=false` 已就位（§3.4 前提）。
- `WfWriteHelper`：阶段 3 后 **不再显式写 `wf_instance.status` / 关闭 `wf_task`**，改为「写 intent → 调引擎 → 事件反写 → 兜底补齐」。
- 步骤 2（会签多实例）运行时门禁**已完成**（见《下沉迁移方案》§6.2）：`doApprove` 在引擎多实例节点跳过自研发散计数、`advance` 按「每条引擎任务=一人」生成 wf_task，方案C 真实反写的前置已打通。

### 10.2 阶段 3 摘写范围（重要边界）
| 位置 | 处理 |
|---|---|
| `terminate` 的 `setStatus+updateById`、关闭 wf_task 循环 | **已摘除**，改由 `PROCESS_CANCELLED` 反写 |
| `suspend` / `activate` 的 `setStatus+updateById` | **已摘除**，改由 `ENTITY_SUSPENDED/ACTIVATED` 反写 |
| `advance` 归档的状态写 | **已摘除**，改由事件反写 |
| `appendLog`（`wf_approval_log`） | **保留** —— OA 独占写，引擎无对应物，本就不存在两次写 |
| `processService.delete/suspend/activate` | **保留** —— 这是**触发事件的引擎动作**，删了事件不会发生、反写也就没了 |

### 10.3 「不通过(2)」语义缺口与 intent 方案
引擎只有 `PROCESS_COMPLETED`→通过(1)、`PROCESS_CANCELLED`→撤销(3)，**没有「不通过(2)」的独立事件**。解决方案：
- `wf_instance` 新增 `pending_status` 列，业务在调引擎**之前**写入目标终态意图（intent）；
- `PROCESS_CANCELLED` 反写时读 `pending_status` 决定终态（1/2/3），缺省回落撤销(3)，写完即清空；
- 业务侧 `ensureApplied` 兜底：事件万一未派发，仍按 intent 补齐并清空，不留下悬挂状态。
- ⚠️ **受限说明**：当前业务**没有任何入口会传 `STATUS_REJECTED(2)`**（撤回/撤销恒传 3），故值 2 的分支只有代码正确性保证，**未做端到端验证**（与库内 `status=2` 历史 0 条一致）。

### 10.4 DDL 结论（修正 §9.5）
| 表 | §9.5 要求 | 实际结论 |
|---|---|---|
| `wf_instance.engine_inst_id` | 唯一索引 | **已存在** `UNIQUE KEY uk_engine_inst`，无需 DDL |
| `wf_task.engine_task_id` | 唯一索引 | **不能建**：协办(7)/抄送(8)/传阅(11) 复用同一 `engine_task_id`（数据实证：同一 id 对应 5 个不同办理人）；该列 `NOT NULL DEFAULT ''`，建唯一索引会重现 `Duplicate entry ''` 事故。幂等**纯靠应用层先查后写** |

### 10.5 踩坑记录（真实事故）
1. **`updateById` 置 NULL 会生成无 SET 子句的 UPDATE**：MyBatis-Plus 只更新非 null 字段，传全 null 实体拼出 `UPDATE wf_instance WHERE id=?` → SQL 语法错误 → C1 下抛异常 → **整个引擎操作回滚**（撤回返回 500、实例停在运行中）。置 NULL 必须用 `UpdateWrapper.setSql("pending_status = NULL")`。
   > 反面印证 C1 确实生效：台账写失败真的回滚了引擎操作，没有留下半截状态。
2. **阶段 3 后差异计数语义失效**：业务不再预写状态，事件到达时「实际值≠目标态」是**正常流程**而非漂移（实测 `ENTITY_SUSPENDED/ACTIVATED` 各计 1 条但状态写对了）。已改为只在实际值**既非目标态、也非合法前置态**时才计差异（暂停前允许 0、恢复前允许 4、终态前允许 0/4）。
3. **`PROCESS_STARTED` 的时点约束**：见 10.1，该事件无法用于比对/反写 `engine_inst_id`。

### 10.6 阶段 3 回归结果（2026-09-27）
| 检查项 | 结果 |
|---|---|
| 暂停 → `status=4` | ✅ |
| 恢复 → `status=0` | ✅ |
| 撤回 → `status=3` + `end_time` 有值 | ✅ |
| 撤回后待办（含协办 7）全部关闭 | ✅ 3/3 |
| `pending_status` 清空为 NULL、无残留 | ✅ |
| 影子差异计数 `totalDiff` | ✅ 0 |
| `drift_check` 6a / 6b / 6c | ✅ 全 0 |
| 待办悬挂（业务待办 / 引擎已无） | ✅ 0 |

> **关键证据**：阶段 3 后 `suspend`/`activate` 业务已**完全不写状态**，而实测状态正确变为 4 / 0 —— 证明状态确由**事件反写**产生，而非兜底逻辑补写。

### 10.7 开关与回滚
`blade.workflow.ledger-listener.enabled=false` 即完全回到方案 A 显式双写：业务侧 `eventDriven()` 返回 false，恢复 `setStatus+updateById` 与关闭待办循环。**无需改代码、无需修数据**，回滚代价极低。

> **回退路径已实测（2026-09-27）**：开关置 false 重启后，`listenerRegistered=false`，暂停/恢复/撤回仍得到正确状态（4/0/3）、待办（含协办 7）全部关闭、`drift_check` 6a/6b/6c 与待办悬挂均为 0 —— 证明 `WfWriteHelper` 兜底分支可独立撑住流程状态，方案C 出问题时可一键回退且不伤业务。验证通过后已恢复 `enabled=true`。

### 10.8 「不通过(2)」定位结论（2026-09-27 排查）
**业务语义确认**：本系统的「不同意」= **退回上游节点**（`WfRejectManager` 计算可退回候选、`WfTaskServiceImpl#reject` → `rejectToStarter`），**不是终态**。
因此 `STATUS_REJECTED(2)` 属于**设计上预留、业务上未启用**的状态，无入口产生它是**符合业务现状的**，不是缺陷：
- `terminate` 的调用方只有 `withdraw`（撤回）与 `cancel`（撤销），两者均传 `STATUS_CANCELED(3)`；
- 全库 `wf_instance` 状态分布：运行中(0) 76 / 通过(1) 85 / 撤销(3) 7 / 草稿(5) 6 / **不通过(2) 0 条**。

**对应决策**：不新增业务动作，`pending_status` intent 机制**保留为预留**。待业务侧真正需要「不同意并结束流程」时，只需让该入口调用 `terminate(instId, STATUS_REJECTED, ...)`，反写链路无需改动（已支持派生 2）。

### 10.9 反向漏洞排查结论（PROCESS_COMPLETED 恒派生成「通过(1)」）
风险假设：若某流程定义存在「不同意」分支走到结束事件，实例会被错标为「已通过」。排查结果：

| 流程定义 | 归档节点 | 指向归档的连线 | 结论 |
|---|---|---|---|
| 测试-918（两个版本） | 1 个「结束」 | 4 条，来源均为审批节点(2/5/3/6) | 语义都是审批通过 → 标 1 **正确** |
| test-922 | **3 个**（正常结束 / 错误结束 / 终止结束） | 4 条，含 `UserTask_Reject`(驳回处理) → `ErrorEndEvent`(错误结束) | **存在错标风险** |

**但风险尚未兑现**：test-922 的 3 个实例全部为**草稿(5)**、`engine_inst_id` 为 NULL —— **从未真正发起过**，故从未触发 `PROCESS_COMPLETED`。

**结论与建议**：
- 当前生产/在用流程**无此风险**，暂不改造（改造需给 `PROCESS_COMPLETED` 也加 outcome 变量派生，改动面大于收益）。
- 记录为**已知项**：若将来有流程用「分支走到不同结束事件」表达不同审批结果，必须同步给 `PROCESS_COMPLETED` 加终态派生（业务完成时写流程变量 `outcome`，事件据此派生 1 通过 / 2 不通过），否则会被一律标成「已通过」。
