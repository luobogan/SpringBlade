# Flowable 与 Spring Boot 集成流程功能系统性分析与遗漏清单

> **总索引**：[工作流分析文档总索引.md](工作流分析文档总索引.md)（五份递进式分析文档的统一入口）
>
> **目标**：对本项目（springBlade + Flowable 8.1.0-SNAPSHOT）已实现的流程功能做系统性深度分析，覆盖 11 个方面；每项说明"已覆盖 / 缺失或不完整"，给出遗漏项优先级与影响评估，最终汇总为一份条理清晰的遗漏清单。
>
> **本文边界**：分析现状与缺口，给出优先级与影响；**不写实现步骤**（实现落在后续方案文档）。证据用于证明现状。
>
> **配套文档**：能力盘点与增强目标见《Flowable能力挖掘与功能增强蓝图.md》；双内核兼容架构见《全量BPMN元素启用与原有流程语义兼容架构分析.md》；事实源边界见《去wf_表-定义主记录翻源设计.md》。
>
> **日期**：2026-11-17

---

## 0. 总体结论

- **已覆盖**：以 UserTask 为核心的审批主链（发起/办理/退回/撤回/终止/归档）、指定流转与多目标扇出、会签（自研 + 引擎多实例两套）、出口 UEL 条件、流程版本钉绑、ACT 读源与状态投影、4 个全局事件监听器、自算四色进度图、自由流程静态节点池、超时扫表、公文 odoc/套红/OFD 等 OA 扩展。
- **整体判断**：项目是"**中深度使用 Flowable、但能力利用高度不均衡**"——执行与 OA 扩展做得深，引擎原生事件/异步/补偿/生态/可观测大面积空白。
- **最高风险发现**：事件反写失败策略 `isFailOnException` **口径不统一**——多数 listener 为 false 且注释/文档仍残留 true 语义，而 `WfMiEmptyGuardListener` 仍为 true（P0/高，见 §10.3）。
- **遗漏清单**：共 **34 项**，P0 3 项、P1 14 项、P2 13 项、P3 4 项（见第 12 章）。

优先级定义：**P0**=正确性/一致性风险，需立即订正；**P1**=高价值功能缺口，影响业务能力；**P2**=平台化/可观测/生态，影响演进效率；**P3**=锦上添花，按需启用。

---

## 1. 流程定义与部署

**已覆盖**
- 手工装配 `SpringProcessEngineConfiguration`，含数据源、事务管理器、schema 策略、自动部署 `classpath*:processes/*.bpmn20.xml`（`FlowableConfig.java:49,93-183,179-181`）。
- 正式部署 `createDeployment().tenantId(...).addString(...).deploy()`；测试部署走 `disableSchemaValidation/disableBpmnValidation` + `procKey__test`（`ProcessServiceImpl.java:194-206,296-309`）。
- 流程定义查询、挂起、删除（`ProcessServiceImpl.java:425-445,434`）；`getBpmnModel` 用于重部署/解析/监听器（`WfDefinitionRedeployServiceImpl.java:120` 等）。
- 部署期 BpmnModel 改写：条件 UEL 注入、多实例注入、SendTask 降级（`WfDefinitionServiceImpl.java:490-496,2370-2427,2298-2327`）。
- 发布门禁 `validateForDeploy` 四类校验（`WfDefinitionServiceImpl.java:2454-2532`）。

**缺失或不完整**
- ❌ 定义级 `activateProcessDefinitionById`：只有实例级激活（P2/中）。
- ❌ 部署资源字节级读取 `getResource` / `getDeploymentResourceNames`：main 0 结果，BPMN 附件导出/下载能力缺失（P3/低）。
- ❌ 流程实例迁移 `ProcessInstanceMigrationManager`：0 结果；版本变更靠 `ChangeActivityState` 单点跳转，无批量迁移（P2/中）。
- ⚠️ `flowable.*` 原生配置前缀在 yml 中 0 结果，手工装配使 `flowable.history-level` 等不生效，配置前缀具有误导性（P2/中，详见 §11）。

---

## 2. 流程实例与任务生命周期

**已覆盖**
- 发起：`startProcessInstanceByKey`（测试/回退）+ `startProcessInstanceById`（正式钉版本）（`ProcessServiceImpl.java:55,89`）。
- 发起即完成创建节点 `completeStarterCreateTask`（`WfInstanceServiceImpl.java:646-673`）。
- `taskService.complete` 审批主链、`doApprove` 全流程（`ProcessServiceImpl.java:145`；`WfTaskServiceImpl.java:314-559`）。
- 退回/转发/加签/抄送/催办/标记已读（`WfTaskServiceImpl.java:585,761,791,888,941,964`）。
- 撤回/暂停/恢复/撤销/强制归档/强制删除（`WfInstanceServiceImpl.java:1698,1706,1723,1741,2159,2227-2229`）。
- `ChangeActivityState` 用于退回/跳转/多目标扇出/归档复活（`ProcessServiceImpl.java:115-118,322-350`）。
- 级联删除 `WfWriteHelper.terminate`（状态→关任务→日志→删实例→写 ACT→反写兜底，`WfWriteHelper.java:133-157`）。

**缺失或不完整**
- ❌ `setAssignee` / `claim` / `delegate` / `setOwner` 引擎原生 API 全 0：转办靠业务台账 + 引擎任务重建，与引擎口径不一（P1/高，详见 §5）。
- ❌ `DynamicBpmnService` / `createProcessInstanceModificationBuilder` 0：无法运行期精细订正实例（P2/中）。
- ❌ 按消息启动 `startProcessInstanceByMessage` 0（P2/中，详见 §4）。
- ⚠️ 归档判定只看 `ACT_RU_TASK` 是否为空（`WfInstanceServiceImpl.java:3120-3133`），等待态节点会被误判归档（P0/高，详见 §4、§11 前置）。

---

## 3. 网关与事件机制

**已覆盖**
- 排他/并行/包容网关由 Flowable 原生执行；出口条件 `injectLinkConditions` 注入 UEL（`WfDefinitionServiceImpl.java:2636-2681`），条件变量兜底 `supplyMissingConditionVars`（`WfTaskServiceImpl.java:393-396`），UEL 异常中文转换（`:535-542`）。
- 网关节点无办理人占位处理（`WfInstanceServiceImpl.java:3231-3238`）。
- 全局事件监听 4 个、9 类事件（`FlowableConfig.java:146-178`）。

**缺失或不完整**
- ❌ `signalEventReceived` / `messageEventReceived` 0：无运行期信号/消息唤醒（P1/高）。
- ❌ `startProcessInstanceByMessage` 0：无事件驱动发起（P2/中）。
- ❌ `EventRegistry` 0：无外部事件统一注册/发布订阅（P2/中）。
- ⚠️ 边界事件/事件子流程/信号事件仅静态识别与降级（`WfDefinitionServiceImpl.java:782,2139,2480-2484`），不参与运行（P1/高）。
- ❌ 错误/升级事件（`errorEventDefinition` / `BoundaryErrorEvent` / `escalations`）全仓 0（P1/高，详见 §7）。
- ⚠️ 事件反写缺活动级事件：`WfEngineEventListener.java:88-90` default 分支不处理 ACTIVITY_*，`TASK_CREATED` 被投影器跳过（`WfStateProjector.java:101-104`）——引擎自动推进结果无同步入口（P0/高，全量启用的最大阻塞点）。

---

## 4. 监听器与扩展点

**已覆盖**
- 4 个全局 `FlowableEventListener`：`WfEngineEventListener`（9 类事件）、`WfBizCallbackListener`（PROCESS_COMPLETED）、`WfMiEmptyGuardListener`（MI 空集合拦截）、`WfProcessNoticeListener`（TASK_CREATED/PROCESS_COMPLETED），统一聚合注册（`FlowableConfig.java:146-178`，修复过监听器互相覆盖问题 `:144-145`）。

**缺失或不完整**
- ❌ 内嵌 `ExecutionListener` / `TaskListener` 0 结果（方案 C 明确不用，改 XML 成本问题）——节点级自定义钩子缺失（P2/中）。
- ❌ 引擎扩展点全 0：`ProcessEngineConfigurator` / `BpmnParseHandler` / `ActivityBehaviorFactory` / `DelegateInterceptor` / `ExpressionManager`（P2/中）。
- ❌ Spring bean resolver / `delegateExpression` 0（`WfActionExecutor` 的 `SpringUtil.getBean` 是自定义 Action 通道，非引擎委托解析）——服务任务无法委托 Spring Bean（P1/高）。
- ⚠️ 事件失败策略口径不统一（详见 §10.3，P0/高）。

---

## 5. 表单与变量处理

**已覆盖**
- 运行期变量：`getVariable/setVariable/setVariables/removeVariable`（`ProcessServiceImpl.java:154,168,181,320,338`）；业务变量与 MI 集合变量注入（`WfTaskServiceImpl.java:387,1892`）。
- 自建表单渲染链：`IWfFormRenderService`（render/preview/validate）+ `formmode` 取数（`WfTimeoutServiceImpl.java:400`）。
- 审批意见写 `ACT_HI_COMMENT`（`ProcessServiceImpl.java:362-368`）。

**缺失或不完整**
- ❌ Flowable Form 引擎全 0（`FormService` / `org.flowable.form`）——表单定义与流程变量绑定完全自建（P3/低：自建方案已够用，明确边界即可）。
- ❌ `HistoricVariableInstanceQuery` / `ACT_HI_VARINST` 0：字段级历史变量轨迹缺失（P1/高，详见 §6）。
- ⚠️ 网关 UEL 仅补变量兜底，不引用 Spring bean（P2/中）。

---

## 6. 历史数据与审计

**已覆盖**
- `historyService`：`createHistoricActivityInstanceQuery`（`ProcessServiceImpl.java:404-408`）、`createHistoricProcessInstanceQuery`（`:413-415`）、`deleteHistoricProcessInstance`（`WfInstanceServiceImpl.java:2233`）。
- 四色进度图：纯读 `ACT_HI_ACTINST/TASKINST/PROCINST` 自算（`WfInstanceServiceImpl.java:4040-4110`，`WfProgressView.java:10-28`）。
- 历史级别固定 `audit`（`FlowableConfig.java:129-134`），保证 `ACT_HI_COMMENT` 落库。
- 审批日志与意见双写（`WfWriteHelper.java:74`）。

**缺失或不完整**
- ❌ `createHistoricTaskInstanceQuery`：main 0 结果，仅测试中出现（P2/中）。
- ❌ 历史变量审计 `ACT_HI_VARINST` 0：字段级变更无法回溯（P1/高）。
- ⚠️ 历史级别硬编码 `audit`，不可按流程/租户配置；full 表单快照不开启（P2/中）。
- ⚠️ 进度图纯自算，未用引擎 Diagram API，与引擎版本升级存在耦合风险（P3/低）。

---

## 7. 异常与补偿处理

**已覆盖**
- 业务级"流程异常处理"：`exceptionMode` continue/stop（`WfNodeSettingsUtil.java:447-454`）、`applyExceptionFallback` 带 visited 防环递归重算（`WfInstanceServiceImpl.java:3221-3242`）。
- MI 空集合守卫（`WfMiEmptyGuardListener.java:48-49,109-117`）。
- 非核心副作用失败降级为 warn：公文取号、镜像同步、版式转换、用印、通知回写（`WfTaskServiceImpl.java:408-442,569-581`）。
- 补偿 job：超时扫描 `WfTimeoutJob`、签章回调补偿 `EsignCompensateJob`、漂移修复 `HistoricalDriftFixer`。

**缺失或不完整**
- ❌ BPMN 原生补偿（`compensationHandler` / `compensation boundary` / `sendCompensationEvent`）全仓 0（P1/高）。
- ❌ 错误/升级事件（`errorEventDefinition` 等）全仓 0（P1/高）。
- ❌ 流转失败重试 / `deadletter` 处理 / 失败重算通用机制 0（`deadletter` 仅注释提及，`FlowableConfig.java:127`；P1/高）。
- ⚠️ `advanceInternal` 无 try-catch，完全靠业务级兜底 + 抛错回滚（`WfInstanceServiceImpl.java:3113-3242`）（P2/中）。

---

## 8. 事务与并发控制

**已覆盖**
- `@Transactional(rollbackFor = Exception.class)` 广泛覆盖 service public 方法（`WfTaskServiceImpl.java:266,272-305,585,761,791,888,941,964`；`WfInstanceServiceImpl.java:388,676,777,1698-1741,2159` 等）。
- 自由流程 version 乐观锁（`WfFreeFlowManager.java:127-131,171`）+ saveToken 幂等（`:61-62,132-136,187-191`）。
- 唯一键防重（`uk_inst_order/uk_inst_node/uk_link`，`V2026.11.04_001__wf_free_flow.sql:52-53,81`；`uk_engine_inst`，方案 C 设计 `:287`）。
- 定时选主 MySQL `GET_LOCK`（`WfTimeoutJob.java:58,82-100`；`EsignCompensateJob.java:33,53-80`）。

**缺失或不完整**
- ❌ `TransactionTemplate` / `TransactionSynchronization` / propagation 精细控制 0：粗粒度 service 层事务，无只读事务/提交后回调（P2/中）。
- ❌ 行锁 `SELECT FOR UPDATE` / RedisLock / `synchronized` / `ReentrantLock` 业务并发控制 0（P1/高）。
- ⚠️ 会签并发靠自研 `countPending/nextPending/closeSiblings`（`WfTaskServiceImpl.java:346-372,2026,2051`），无行锁/CAS/提交级幂等键（P1/高）。
- ⚠️ 进程级 token 重启失效（注释"重启后由 version 乐观锁兜底"，`WfFreeFlowManager.java:61-62`）（P2/中）。
- ⚠️ 事件反写失败无通用补偿 job（详见 §10.3，P0/高）。

---

## 9. 权限与多租户

**已覆盖**
- 记录级鉴权 `WfAuthUtil.requireOperateTask/requireStarterOrAdmin`（"条件 ≠ 权限"口径）。
- 部署传 `tenantId`（`ProcessServiceImpl.java:202`），定义查询带租户过滤（`:80-81`）。
- `blade-starter-tenant` 依赖（`pom.xml:29-32`），数据权限 mapper 豁免配置（`application.yml:232-241`）。
- 字段/明细权限（`WfPermServiceImpl.java:193,260,323`）。

**缺失或不完整**
- ⚠️ 启动实例未显式传 tenant（`ProcessServiceImpl.java:53-58,87-92` 靠 processDefinitionId 隐式携带）（P2/中）。
- ❌ 多租户执行器 `ExecutorPerTenantAsyncExecutor` 0：无租户级作业隔离与按租户调度（P2/中）。
- ❌ 动作执行器无权限校验：`WfActionExecutor` 仅 `instanceof IWfCustomAction` 约束（`WfActionExecutor.java:135-137`），http/sql 有白名单/开关但无租户级动作白名单（P1/高）。

---

## 10. 定时任务与异步执行

**已覆盖**
- Spring 调度 `WfTimeoutJob`（`@Scheduled` + `GET_LOCK` 选主 + 扫 `ACT_RU_TASK.DUE_DATE_`，`WfTimeoutJob.java:76,82-100,144-152`）。
- 其他 job：`WfDefinitionBackfillJob`、`EsignCompensateJob`、`HistoricalDriftFixer`。
- `ManagementService` bean 与作业计数监控（`FlowableConfig.java:223-226`；`ProcessServiceImpl.java:514-516`）。

**缺失或不完整**
- ❌ AsyncExecutor 硬编码关闭（`FlowableConfig.java:127-128`），`jobService` 0：timer 边界事件/异步节点不可用（P1/高）。
- ❌ 死信作业（deadletter）可见/重试/告警 0（P1/高）。
- ⚠️ timer 依赖异步执行器且需 7 项配套（最终一致、常驻对账、deadletter 告警、`is_test` 隔离、超时动作接线、线程池、asyncHistory=false），文档《Flowable8承接台账模块-去wf_表改造分析.md:1086-1096》列明，均未落地（P1/高）。
- ❌ `@Async` 0：无 Spring 异步执行（P3/低）。

### 10.3 事务一致性口径不统一（P0/高）
- `WfEngineEventListener.java:94-100` `isFailOnException=false`；`:40-42` Javadoc 仍写 true；`FlowableConfig.java:135-136`、`application.yml:167` 注释仍残留 true 语义。
- `WfBizCallbackListener.java:88-92`、`WfProcessNoticeListener.java:211-215` 同为 false；
- `WfMiEmptyGuardListener.java:130-134` 仍为 **true**（空集合拦截需回滚）。
- 结论：同类 listener 失败策略不一、文档与代码脱节，是最应优先订正的一致性风险。

---

## 11. Spring Boot 自动配置与生态集成完整性

**已覆盖**
- `WorkflowApplication`：`@BladeCloudApplication` + `@EnableScheduling` + `@EnableFeignClients` + `@ComponentScan`（`WorkflowApplication.java:13-19`）。
- Feign 生态：`blade-formmode/user/system/message-api`（`pom.xml:42-61`），消息中心 `INoticeClient` 在多处使用。
- Nacos：动态数据源主库 URL 由 Nacos blade.yaml 提供（`pom.xml:33` 注释、`application.yml:172`）。
- 手工装配完整可用且可回退（所有开关 `@Value("${blade.workflow...}")` 默认 false，yml 现值多为 true，见证据索引）。

**缺失或不完整**
- ❌ 不使用 `flowable-spring-boot-starter`（Boot 4 兼容性，`pom.xml:67-82`），无 `@AutoConfiguration` 类：生态自动配置能力缺失（P2/中）。
- ❌ Actuator 健康检查/metrics 0（pom 无依赖、yml 无 management.endpoint）：流程引擎运行指标不可观测（P2/中）。
- ❌ Spring Event（`ApplicationEventPublisher` / `@EventListener`）0：流程事件未与 Spring 事件体系打通（P2/中）。
- ❌ Redis/Caffeine 0：本地缓存用 `ConcurrentHashMap`（`WfBpmnExtensionReader.java:67`、`WfStateProjector.java:75`），无分布式缓存/失效机制（P2/中）。
- ⚠️ 配置前缀误导：`flowable.*` 原生前缀在 workflow yml 中 0 结果，手工装配使 `flowable.history-level` 不生效（`FlowableConfig.java:129-132` 明确声明）（P2/中）。
- ⚠️ 开关默认值与 yml 现值大面积相反（代码默认 false、yml 开 true，共 20+ 项），回退语义依赖 yml，配置漂移风险（P2/中）。

---

## 12. 遗漏清单（汇总）

> 优先级：P0 正确性/一致性风险 · P1 高价值功能缺口 · P2 平台化/可观测/生态 · P3 按需启用

### P0 — 立即订正（3 项）

| # | 遗漏/风险 | 方面 | 影响 |
|---|---|---|---|
| P0-1 | 事件反写失败策略 `isFailOnException` 口径不统一，代码/注释/文档脱节 | 监听器、事务、定时 | 高：事务一致性语义不可信，排障困难 |
| P0-2 | 引擎→SB 正向桥缺失：无 ACTIVITY_COMPLETED、TASK_CREATED 被跳过，自动节点结果无同步入口 | 网关事件、监听器 | 高：全量启用自动节点的最大阻塞点 |
| P0-3 | 归档判定只看 `ACT_RU_TASK` 空，等待态节点被误判终态且双写 ACT 不可逆 | 实例生命周期、事件 | 高：流程数据错误且不可逆 |

### P1 — 高价值功能缺口（14 项）

| # | 遗漏 | 方面 | 影响 |
|---|---|---|---|
| P1-1 | BPMN 原生补偿机制缺失 | 异常补偿 | 高：无法声明式回滚已执行活动 |
| P1-2 | 错误/升级事件缺失 | 网关事件、异常补偿 | 高：异常路径只能靠 Java catch |
| P1-3 | 流转失败重试 / deadletter 处理缺失 | 异常补偿、定时异步 | 高：失败无感、无恢复闭环 |
| P1-4 | 行锁/分布式锁/CAS 缺失，会签并发靠自研计数 | 事务并发 | 高：并发审批一致性风险 |
| P1-5 | 引擎原生认领/委派/转办缺失 | 实例任务生命周期 | 高：与业务层替代方案口径不一 |
| P1-6 | 消息/信号唤醒缺失（`signal/messageEventReceived`） | 网关事件 | 高：无法事件驱动流转 |
| P1-7 | 边界/事件子流程/信号事件仅识别不运行 | 网关事件 | 高：画了跑不通 |
| P1-8 | Spring bean resolver / delegateExpression 缺失 | 监听器扩展点 | 高：服务任务无法委托 Bean |
| P1-9 | 历史变量审计 `ACT_HI_VARINST` 缺失 | 表单变量、历史审计 | 高：字段级变更不可回溯 |
| P1-10 | 动作执行器无租户级权限校验 | 权限多租户 | 高：自动动作越权风险 |
| P1-11 | AsyncExecutor 关闭、timer/异步节点不可用 | 定时异步 | 高：引擎原生定时能力空白 |
| P1-12 | 作业死信可见/重试/告警缺失 | 定时异步、监控 | 高：失败作业无闭环 |
| P1-13 | ServiceTask/ScriptTask/DMN 无真正执行能力 | 扩展点（能力盘点） | 高：自动化节点空跑 |
| P1-14 | 在途实例批量迁移缺失 | 定义部署 | 中：版本升级只能单点跳转 |

### P2 — 平台化与可观测（13 项）

| # | 遗漏 | 方面 | 影响 |
|---|---|---|---|
| P2-1 | DynamicBpmnService / ModificationBuilder 缺失 | 实例生命周期 | 中：无运行期精细订正 |
| P2-2 | 按消息启动缺失 | 网关事件 | 中：无事件驱动发起 |
| P2-3 | EventRegistry 缺失 | 网关事件 | 中：外部事件无统一入口 |
| P2-4 | 内嵌 Execution/TaskListener 缺失 | 监听器扩展点 | 中：节点级钩子缺失 |
| P2-5 | 引擎扩展点（Configurator/ParseHandler/BehaviorFactory/Interceptor/ExpressionManager）全缺 | 监听器扩展点 | 中：无法优雅定制节点语义 |
| P2-6 | `HistoricTaskInstanceQuery` main 0 | 历史审计 | 中：任务级历史查询缺失 |
| P2-7 | 历史级别硬编码 audit，不可配置 | 历史审计 | 中：审计/存储不可权衡 |
| P2-8 | TransactionTemplate / Synchronization / propagation 缺失 | 事务并发 | 中：无只读事务/提交后回调 |
| P2-9 | saveToken 进程级、重启失效 | 事务并发 | 中：幂等需乐观锁兜底 |
| P2-10 | 启动实例未显式传 tenant | 权限多租户 | 中：租户靠隐式携带 |
| P2-11 | 多租户执行器缺失 | 权限多租户 | 中：无租户级作业隔离 |
| P2-12 | 定义级 activate 缺失 | 定义部署 | 中：生命周期不对称 |
| P2-13 | Spring 生态：Actuator / Spring Event / Redis-Caffeine 缺失；flowable.* 配置前缀误导；开关默认值与 yml 现值大面积相反 | Boot 集成 | 中：可观测/缓存/配置治理弱 |

### P3 — 按需启用（4 项）

| # | 遗漏 | 方面 | 影响 |
|---|---|---|---|
| P3-1 | Flowable Form 引擎缺失（自建 formmode 已够用） | 表单变量 | 低：需明确边界 |
| P3-2 | 部署资源 `getResource` 缺失 | 定义部署 | 低：BPMN 附件导出能力 |
| P3-3 | `@Async` 缺失 | 定时异步 | 低：异步靠调度与引擎 job |
| P3-4 | 进度图纯自算，未用引擎 Diagram API | 历史审计 | 低：升级耦合风险 |

---

## 13. 证据索引

| 结论 | 代码位置 |
|---|---|
| FlowableConfig 手工装配 / 自动部署 / 历史 audit / AsyncExecutor 关闭 | `config/FlowableConfig.java:49,93-183,179-181,127-138` |
| 发起/钉版本/complete/删除作业 | `ProcessServiceImpl.java:55,89,145,194-206,296-309,404-445,452-465,514-516` |
| 审批主链/副作用降级/UEL 异常转换/会签并发 | `WfTaskServiceImpl.java:266-559,346-372,393-396,408-442,531-545,569-581,2026,2051` |
| 发起即完成创建/归档判定/异常兜底/生命周期 | `WfInstanceServiceImpl.java:388,646-673,1698-1741,2159,2227-2229,3113-3242` |
| 自由流程版本乐观锁/saveToken/物理删除 | `freeflow/WfFreeFlowManager.java:40-41,61-62,106-191,269-278` |
| 事件监听 4 个及失败策略 | `listener/WfEngineEventListener.java:40-42,69-111`；`WfBizCallbackListener.java:88-92`；`WfMiEmptyGuardListener.java:130-134`；`WfProcessNoticeListener.java:211-215` |
| 事件投影不建待办/ACT 直写 | `service/helper/WfStateProjector.java:20-33,61-69,101-104,157-168,217-228` |
| 级联终止与反写兜底 | `service/helper/WfWriteHelper.java:74,88,111,133-157` |
| 补偿与漂移修复 job | `job/WfTimeoutJob.java:58,76,82-100,144-152`；`odoc/esign/job/EsignCompensateJob.java:33,43,53-80`；`job/HistoricalDriftFixer.java:41,50,63-79` |
| Spring Boot / Feign / 生态 | `WorkflowApplication.java:13-19`；`pom.xml:29-32,33,42-61,67-82`；`application.yml:45-47,167,172,232-241` |
| 开关全集（20+ 项默认值与现值） | 各 `@Value("${blade.workflow...}")`，详见第 11 章与源码 |
| 能力 0 结果清单 | blade-workflow 全仓 grep（详见第 1-10 章各表） |

---

**文档版本**：1.0.0
**发布日期**：2026-11-17
**作者**：CodeBuddy AI
**适用范围**：Flowable+Spring Boot 集成现状审计、功能遗漏治理、平台演进排期参考
