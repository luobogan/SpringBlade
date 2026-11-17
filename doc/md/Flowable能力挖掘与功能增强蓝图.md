# Flowable 能力挖掘与功能增强蓝图

> **总索引**：[工作流分析文档总索引.md](工作流分析文档总索引.md)（五份递进式分析文档的统一入口）
>
> **命题**：在现有 Flowable 8.1.0-SNAPSHOT 基础上进行功能扩展与完善，充分挖掘并利用 Flowable 尚未被充分利用的能力，弥补当前功能短板。
>
> **本文视角**：明确"已用 / 未用"特性 → 指出功能缺口 → 围绕**流程设计、流程执行、流程监控、扩展能力**四环节给出**功能点与目标**。
>
> **本文边界**：只讲"要补什么能力、达到什么目标"，**不写实现步骤与代码改动**（实现落在后续方案文档）。证据只用于证明"现状是什么"。
>
> **配套文档**：现状与逐元素风险见《流程设计器-BPMN元素支持现状与启用风险分析.md》；双内核兼容架构见《全量BPMN元素启用与原有流程语义兼容架构分析.md》；事实源边界见《去wf_表-定义主记录翻源设计.md》。
>
> **日期**：2026-11-17

---

## 0. 总体判断

本项目已把 Flowable 用作"**动态 BPMN 执行 + 多实例会签 + 全局事件反写 + ACT_HI 自算进度**"的**中深度引擎**，但能力利用是**不均衡**的：

- **用得深**：BPMN 模型级改写（条件 UEL 注入、多实例注入）、`ChangeActivityState` 跳转、全局事件监听、ACT 读源与状态投影、流程版本钉绑。
- **基本空白**：消息/信号驱动、定时异步与死信、引擎内嵌监听器、候选组/认领委派、Flowable Form / DMN / CMMN、JavaDelegate 自动节点、原生 `callActivity`、流程实例迁移、引擎扩展点、历史变量审计。

**增强目标**：在**不推翻现有"业务层推进 + 单一事实源"架构**的前提下，把 Flowable 已从"流程执行内核"升级为"**流程执行 + 自动化 + 事件协同 + 可观测 + 可定制**"的完整平台能力。

---

## 1. Flowable 特性使用矩阵

图例：✅已用（深/中/浅） · ⚠️部分使用或仅静态识别 · ❌未用（全仓 0 结果）

### 1.1 流程执行类

| 能力 | 状态 | 现状要点 |
|---|---|---|
| `startProcessInstanceByKey` / `ById` | ✅ 深 | 正式发起走 ById 钉版本（`ProcessServiceImpl.java:55,89`） |
| `startProcessInstanceByMessage` | ❌ | 无消息驱动发起入口 |
| `signalEventReceived` / `messageEventReceived` | ❌ | 无运行期信号/消息唤醒 |
| `taskService.complete` | ✅ 深 | 审批主链（`ProcessServiceImpl.java:145`） |
| `setAssignee` / `claim` / `delegate` / `setOwner` | ❌ | 转办靠业务台账 + 引擎任务重建，非原生 API |
| `createChangeActivityStateBuilder` | ✅ 深 | 退回/跳转/多目标扇出/归档复活（`:115-118,322-350`） |
| `DynamicBpmnService` / `ModificationBuilder` | ❌ | 无运行期模型级精细订正 |
| 多实例（MI） | ✅ 中（默认关） | 会签下沉引擎 + 加签 + 空集合守卫 |
| ExecutionListener / TaskListener（内嵌） | ❌ | 明确不用内嵌，走全局事件 |
| 条件表达式 UEL | ✅ 深 | `injectLinkConditions` 注入 + 自研条件变量工具 |

### 1.2 任务与身份类

| 能力 | 状态 | 现状要点 |
|---|---|---|
| `candidateUsers` / `candidateGroups` / 组任务查询 | ⚠️ 极浅 | 仅置空占位；`taskCandidateUser/Group` 0 结果 |
| `IdentityLink` | ⚠️ 浅 | 仅 `addUserIdentityLink` 做抄送，且旧路径已停用 |
| 认领/委派/转办原生 API | ❌ | 见 1.1 |
| Flowable Form 引擎 | ❌ | 表单渲染完全自建（`formmode` + `IWfFormRenderService`） |

### 1.3 事件与定时器类

| 能力 | 状态 | 现状要点 |
|---|---|---|
| AsyncExecutor / Job 执行 | ❌ | `setAsyncExecutorActivate(false)` 硬关（`FlowableConfig.java:127-128`） |
| Job 计数（ManagementService） | ⚠️ | 仅 `pendingJobCount()` 监控展示（`:514-516`） |
| Timer 能力 | ⚠️ 替代实现 | 超时靠 `WfTimeoutJob` Spring 调度扫 `ACT_RU_TASK.DUE_DATE_` |
| 边界事件 / 事件子流程 / 信号事件 | ⚠️/❌ | 仅发布前静态校验识别，不参与运行 |
| EventRegistry | ❌ | 0 结果 |
| 全局 FlowableEventListener | ✅ 深 | 4 个 listener、9 类事件（`FlowableConfig.java:146-178`） |

### 1.4 历史与监控类

| 能力 | 状态 | 现状要点 |
|---|---|---|
| `HistoricActivityInstanceQuery` / `HistoricProcessInstanceQuery` | ✅ 中 | `ProcessServiceImpl.java:404-415` |
| `HistoricTaskInstanceQuery` | ❌ main | 仅测试中出现 |
| `HistoricVariableInstanceQuery` / `ACT_HI_VARINST` | ❌ | 历史变量审计空白 |
| 流程图进度 | ✅ 中 | 纯读 `ACT_HI_*` 自算四色（`WfInstanceServiceImpl.java:4040-4110`） |
| 运行期变量 / 审批意见 | ✅ 中 | `runtimeService` 变量 + `ACT_HI_COMMENT` |
| 历史级别 | ✅ 中 | 固定 `audit`（`FlowableConfig.java:129-134`） |
| deadletter 作业 | ❌ | 无死信可见/重试/告警 |

### 1.5 扩展与部署类

| 能力 | 状态 | 现状要点 |
|---|---|---|
| `repositoryService` 部署/查询/挂起 | ✅ 深 | 正式/测试双部署通道（`ProcessServiceImpl.java:199-204,300-307`） |
| `getBpmnModel` | ✅ 中 | 重部署/解析/监听器均用 |
| 定义级 `activateProcessDefinitionById` | ❌ | 只有实例级激活 |
| 流程实例迁移 `ProcessInstanceMigrationManager` | ❌ | 版本变更靠 `ChangeActivityState` 单点跳转 |
| `ProcessEngineConfigurator` / `BpmnParseHandler` / `ActivityBehaviorFactory` / `DelegateInterceptor` / `ExpressionManager` | ❌ | 引擎配置零定制 |
| Spring bean resolver / `delegateExpression` | ❌ | 服务任务无法委托 Spring Bean |
| DMN / CMMN 引擎 | ❌ | 0 结果 |
| ServiceTask / ScriptTask / BusinessRuleTask 执行 | ⚠️ 仅识别 | 无 JavaDelegate / 脚本执行 |
| 原生 `callActivity`（`calledElement`） | ⚠️ 仅识别 | 子流程走业务自建 `WfSubflow` |
| 多租户执行器 | ❌ | 仅启动/部署传 tenantId |

---

## 2. 功能缺口归纳

把上面的"未用/浅用"按**业务影响**归为六类缺口：

| 缺口 | 表现 | 影响 |
|---|---|---|
| **G1 自动化节点缺口** | 服务/脚本/规则任务只识别不执行；无 JavaDelegate / bean 委托 / 沙箱 | 画布能画自动节点但引擎空跑，业务动作落空 |
| **G2 事件协同缺口** | 无 message/signal、`startByMessage`、EventRegistry、事件子流程 | 无法"外部回调驱动流程""流程间协同""事件驱动发起" |
| **G3 定时与异步缺口** | AsyncExecutor 关闭、无 Job 执行/死信处理；超时靠扫表替代 | timer 边界事件不可用、异步节点无、失败作业无感 |
| **G4 任务协作缺口** | 候选组/共享任务、认领委派、抢单、原生转办缺失（业务层有替代但口径不一） | 组任务池、代办委托、标准化任务审计能力弱 |
| **G5 可观测缺口** | 无历史变量审计、无历史任务查询、无死信告警、进度图纯自算 | 字段级变更无法回溯，运行健康度与失败闭环不可见 |
| **G6 可定制缺口** | 引擎扩展点全未用（解析/行为/表达式/拦截）、无实例迁移、无原生 callActivity/DMN/CMMN | 新节点语义只能靠"改 XML 字符串"，无法优雅注入自定义行为；跨版本升级无批量迁移 |

---

## 3. 功能增强蓝图（按环节）

> 以下每项都是"**功能点 + 目标**"，不含实现步骤。

### 3.1 流程设计环节

**D1. 设计期元素能力分层与防呆**
- 目标：设计器提供的元素要么完整可用、要么明确标注不可用，**杜绝"画了跑不通"**。
- 范围：元素白名单/能力标记；发布前能力完备性校验；对未启用能力给出可理解的设计期提示。

**D2. 自动化节点的可配置化**
- 目标：服务任务/规则任务从"画了不执行"升级为"配置即生效"。
- 范围：节点上可声明要执行的动作（Java 委托 / Spring Bean / 脚本 / 规则），并纳入设计期校验与权限约束；补齐 Spring bean resolver 与脚本沙箱能力。

**D3. 事件与消息元素的可用性**
- 目标：消息启动、消息/信号捕获、事件子流程从"静态识别"升级为"可设计可运行"。
- 范围：事件定义与业务事件的绑定关系可配置；外部事件统一注册（EventRegistry）能被调用。

**D4. 表单能力的两条路线选择**
- 目标：明确"继续自建 formmode"还是"引入 Flowable Form"，避免两套表单语义并存。
- 范围：若引入，目标是**定义期表单与流程节点绑定、变量自动映射**；若维持自建，目标是**把 formmode 表单能力与流程变量打通**（现状已部分具备，需明确边界）。

**D5. 决策与协作范式补齐**
- 目标：规则外置（DMN）与案例型协作（CMMN）具备**按需启用**的能力，而不是全仓空白。
- 范围：决策表与流程节点的关联；案例管理作为非流程型协作的补充形态。

### 3.2 流程执行环节

**E1. 引擎级自动化执行能力**
- 目标：ServiceTask 真正能执行业务动作，且**同步/异步可控、失败可重试、可审计**。
- 范围：JavaDelegate / delegateExpression / 脚本三类执行体；与现有 `WfActionExecutor` 动作注册表对齐，避免两套执行语义。

**E2. 事件驱动执行与跨流程协同**
- 目标：流程可被"消息/信号/业务事件"驱动发起与推进，具备**事件驱动的流程间协同**。
- 范围：`startByMessage`、消息/信号接收、事件子流程、EventRegistry 统一入口；与现有"业务层调 advance"形成互补而非冲突。

**E3. 定时与异步执行能力**
- 目标：timer 边界事件、异步节点、超时自动处理由**引擎原生**承担，扫表方案作为兼容降级保留。
- 范围：AsyncExecutor 启用所需的配套能力（最终一致、对账、死信告警、测试态隔离）作为**整体能力**交付，缺一不启用。

**E4. 任务协作标准化**
- 目标：候选组/共享任务池、认领/委派/转办具备引擎原生语义，补齐现有业务层替代方案的口径差异。
- 范围：组任务查询、认领抢单、委派转办的标准化与审计留痕；与 SB 现有转办/加签能力收敛为一套口径。

**E5. 动态实例订正能力**
- 目标：运行期实例可按模型定义做**精细订正**（增删节点、改分支），超越当前 `ChangeActivityState` 的单点跳转。
- 范围：`DynamicBpmnService` / `ModificationBuilder` 能力；与"在途实例钉版本"原则协调，明确哪些订正允许。

**E6. 原生子流程与版本联动**
- 目标：`callActivity` 原生调用具备与业务自建子流程**可共存、可切换**的能力，支持变量传递与结果回写。
- 范围：明确两套子流程的适用边界（引擎级 vs 业务动作级）。

**E7. 引擎→业务状态桥（前置能力）**
- 目标：引擎自动节点跑完后，SB 台账（实例状态、待办、业务列）能**自动同步**，不再依赖业务方法显式调 `advance`。
- 范围：活动级事件纳入事件投影、新等待点的待办生成通道；这是 E1/E2/E3 能否安全启用的**共同前置**。

### 3.3 流程监控环节

**M1. 全链路历史审计**
- 目标：从"ACT_HI 自算进度"升级为**活动级 + 任务级 + 变量级**的完整历史轨迹。
- 范围：历史任务查询、历史变量查询与字段级变更轨迹；与 `ACT_HI_COMMENT` 审批意见整合。

**M2. 运行健康度与失败闭环**
- 目标：作业失败/死信**可见、可重试、可告警**，形成闭环。
- 范围：deadletter 作业查询与恢复、失败作业告警；与 E3 异步能力绑定交付。

**M3. 引擎原生监控与自建监控的融合**
- 目标：把 `ManagementService` 的作业/定义/实例运行指标纳入统一监控视图，减少"自建监控只覆盖 wf_*/ACT 表"的盲区。
- 范围：作业队列、挂起实例、多实例状态等引擎指标的可见性；与现有 `WfMonitorController` 形成互补。

**M4. 历史级别策略化**
- 目标：历史级别从硬编码 `audit` 升级为**按流程/租户可配置**，在审计完整性与存储成本间可权衡。
- 范围：full/audit/activity/none 的策略化；字段级快照（表单快照）按需开启。

### 3.4 扩展能力环节

**X1. 引擎扩展点开放**
- 目标：具备对**解析、行为、表达式、委托**的深度定制能力，替代当前"改 XML 字符串"的单一注入方式。
- 范围：`ProcessEngineConfigurator` / `BpmnParseHandler` / `ActivityBehaviorFactory` / `DelegateInterceptor` / `ExpressionManager` 的受控开放。

**X2. 统一可执行动作框架**
- 目标：服务任务、脚本、规则、附加操作共用**一个可审计、可限权、可测试隔离**的执行入口。
- 范围：统一动作注册表、权限与 `is_test` 闸门、租户上下文注入、执行超时与资源约束。

**X3. 在途实例批量迁移**
- 目标：流程定义升级时，在途实例可**批量、可控**迁移到新版本，而非只能单点跳转。
- 范围：`ProcessInstanceMigrationManager` 能力；与"在途实例钉版本"原则协调，明确迁移的授权与回滚。

**X4. 多租户执行隔离**
- 目标：租户从"启动/部署传 ID"升级为**执行期隔离**（作业调度、脚本上下文、动作调用）。
- 范围：多租户异步执行器；租户级资源与动作白名单。

**X5. 定义级生命周期闭环**
- 目标：流程定义从"挂起/删除"补齐"**重新激活**"闭环，避免只有单向操作。
- 范围：定义级 suspend/activate 的完整对称能力（现状只有实例级激活）。

---

## 4. 目标价值与优先级建议

### 4.1 价值映射

| 环节 | 增强后的核心价值 |
|---|---|
| 流程设计 | 所见即所得——画出来的元素都能跑，配置即生效 |
| 流程执行 | 从"人工审批引擎"升级为"人工 + 自动 + 事件 + 定时"混合编排引擎 |
| 流程监控 | 从"看得见进度"升级为"看得见全链路、看得见失败、可回溯" |
| 扩展能力 | 从"改 XML 字符串"升级为"引擎级受控定制"，支撑长期演进 |

### 4.2 建议优先级（按依赖关系，非实现排期）

1. **第一优先——前置能力**：E7 引擎→业务状态桥、X2 统一动作闸门。它们是所有自动化/事件/定时能力的安全底座。
2. **第二优先——自动化与事件**：E1 自动执行、E2 事件驱动、D1/D2 设计防呆与自动化配置。
3. **第三优先——可观测**：M1 全链路审计、M2 失败闭环（与 E3 绑定）。
4. **第四优先——定时与协作**：E3 定时异步、E4 任务协作、E6 原生子流程。
5. **第五优先——深度扩展**：X1 扩展点、X3 实例迁移、X4 多租户执行、D5 DMN/CMMN、M4 历史策略化。

**硬约束**：E3（AsyncExecutor 启用）必须在"最终一致 + 常驻对账 + 死信告警 + 测试态隔离"四项配套能力**全部具备**后才纳入启用范围；在此之前 timer/异步能力不开放，扫表方案保留为兼容路径。

---

## 5. 边界与 invariant（增强过程中必须守住）

1. **单一事实源不倒退**：Flowable 仍是流程执行语义的唯一事实源；SB 表是投影与业务态，不另起一套流程状态机。
2. **在途实例钉版本不放宽**：除非 X3 实例迁移能力显式授权，否则不对在途实例改模型/改定义。
3. **测试态隔离不依赖调用方自觉**：所有自动动作统一在 X2 闸门处拦，避免引擎触发绕过 `is_test` 守卫。
4. **旧功能零回归**：UserTask 主链、会签、自由流程、抄送、权限、超时、ACT 读源等既有能力必须保持现状；新能力以开关默认关闭、灰度启用。

---

## 6. 证据索引

| 结论 | 代码位置 |
|---|---|
| ByKey/ById 发起、`complete`、ChangeActivityState、repositoryService | `ProcessServiceImpl.java:55,89,115-118,145,199-204,300-307,322-350` |
| 多实例注入/完成条件/加签 | `WfTaskServiceImpl.java:242-243,822,1892`；`WfNodeSettingsUtil.java:770-785` |
| 条件 UEL | `WfDefinitionServiceImpl.java:2636-2681`；`WfConditionUtil.java:30` |
| 全局事件监听 4 个 listener | `FlowableConfig.java:146-178`；`WfEngineEventListener.java:69-91` |
| AsyncExecutor 关闭 / 历史 audit | `FlowableConfig.java:127-134` |
| 超时扫表替代 | `WfTimeoutJob.java:76,144-152` |
| 进度图自算 | `WfInstanceServiceImpl.java:4040-4110`；`WfProgressView.java:10-28` |
| 作业计数 / ManagementService | `ProcessServiceImpl.java:514-516`；`FlowableConfig.java:223-226` |
| 消息/信号/认领/委派/Form/DMN/CMMN/扩展点/实例迁移 0 结果 | blade-workflow 全仓搜索（详见第 1 章各表） |
| 事实源分域原则 | `去wf_表-定义主记录翻源设计.md:30-31,42`；`去wf_表-读源切换开关说明.md:8-10` |

---

**文档版本**：1.0.0
**发布日期**：2026-11-17
**作者**：CodeBuddy AI
**适用范围**：Flowable 能力挖掘决策、功能增强规划、流程平台演进路线参考
