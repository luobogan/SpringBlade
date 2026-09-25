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
