# 泛微 E9 与 Flowable 7 流程图节点功能对比及改造可行性评估

> **分析方法**：E9 侧结论全部基于本地源码实证（`D:\workproject\ecology\src`，部署件对照 `D:\Weaver2020\ecology`），关键类/行号见文末附录；Flowable 7 侧基于官方文档与 7.0.0 Release 说明（Spring Boot 3 / Java 17 基线）。
> **本文焦点**：流程图节点功能（作用 / 流转机制 / 使用场景）→ 节点分类、执行逻辑、扩展方式、功能覆盖差异 → Flowable 7 改造成 E9 功能一致的可行性。
> **项目背景**：本工作区 `springBlade` 已在 Flowable 上实现 E9 风格的流程设计器（节点/出口/操作者/超时/附加操作等，见《SpringBlade工作流引擎迁移方案.md》），第 5 章可行性评估结合该实践给出。

---

## 1. E9 流程引擎节点体系（源码实证）

### 1.1 节点模型与数据表

E9 的"流程图"由 **节点（node）+ 出口（nodelink，即连线）** 构成，核心表：

| 表 | 作用 | 关键字段 |
|---|---|---|
| `workflow_base` | 流程定义主表 | `isfree`(自由流程)、`isautoapprove/isautocommit`(自动批准/提交)、`version/activeversionid`(版本) |
| `workflow_nodebase` | 节点基础表 | `nodetype`(节点类型)、`nodename`、`isfreenode`(自由节点)、`isstart/isreject/isend/isreopen`、自由流程运行期节点存 `requestid/startnodeid/floworder` |
| `workflow_flownode` | 流程↔节点运行配置 | 节点在各流程中的超时、通过时限（`nodepasstime/nodelefttime`）等 |
| `workflow_nodelink` | **出口表（连线）** | `nodeid→destnodeid`、`condition`(老条件SQL)、`newrule`(新规则引擎)、`ruleRelationship`(1且/2或)、`ismustpass`(必经)、`linkorder`、`isreject`(退回出口)、`isBulidCode`(出口生成编号) |
| `workflow_nodeoperator` / 操作组相关 | 节点操作者配置 | 操作组→操作者明细（见 1.5） |
| `workflow_currentoperator` | **运行期待办表** | `isremark`(0=待办、7=协办…)、`receivedate/receivetime`、`agenttype`(代理) |
| `workflow_robotNodeSet` | 机器人节点（等待/自动处理）设置 | `waitTimeType/waitTimeValue/operateType/exceptionHandleType` |
| `workflow_requestbase/requestlog` | 请求主表/流转日志 | `currentnodeid/currentnodetype/isremark`、全量操作日志 |

**设计范式**：E9 是**数据库状态机**——一个请求（requestid）任一时刻对应"当前节点 + 当前操作者集合"，流转=计算下一个节点与操作者并改写状态行，没有 BPMN 的令牌（Token）/执行树概念。

### 1.2 节点类型（`com.engine.workflow.constant.node.NodeType`，实证枚举）

| 值 | 枚举 | 名称 | 作用与流转机制 | 典型场景 |
|---|---|---|---|---|
| 0 | `CREATE` | **创建节点** | 流程入口，通常 `isstart='1'`；发起人填单提交后进入后续节点。支持 `isreopen` 重新打开回到创建节点（`RequestNodeFlow.getNextNode()` 中 `nextnodetype="0"` 硬编码返回创建节点） | 报销单填报、请假申请发起 |
| 1 | `APPROVE` | **批准/审批节点** | 核心人工节点。操作者提交时评估**出口条件**决定去向；可配置会签关系、签收、退回、转办、代理、加签、超时、节点前/后附加操作 | 部门经理审批、财务复核 |
| 2 | `SUBMIT` | **提交（实现）节点** | 与审批节点同为人工节点，差异主要是操作菜单/交互语义（源码中与 `AUTOPROCESS/WAIT` 共用提交按钮分支 `GetRightMenuCmd:1515`）：强调"完成后进入下一步"而非"批准与否" | 材料整理、录入确认 |
| 3 | `ARCHIVE` | **归档节点** | 终点。到达后请求置办结（`workflow_requestbase.currentstatus`），触发归档后附加操作、归档消息/文档归档 | 流程收尾、数据落定 |
| 4 | `CIRCULATE` | **传阅（自由节点）** | 仅出现在自由节点场景（`WorkflowNodeComInfo` 的 `TABLE_WHERE` 明确过滤 `isfreenode`），传阅人查看后可提交/不需提交（见 `SignOrder.CC_*`） | 通报知会、并行知会 |
| 5 | `WAIT` | **等待节点** | **机器人节点**。不产生人工待办（`workflow_currentoperator.userid = '0'` 系统操作者，`RobotNodeBiz.operatorid="0"`）；由后台线程 `RobotNodeThread` 周期扫描 `workflow_currentoperator` 中节点类型为 5/6 的记录，按 `waitTimeType`（0=永久等待/1=等待时长，`RobotNodeServiceBiz.getRobotNodeTask()`）到期后触发后续处理；永久等待型直接跳过 | 等待子流程回写、等待外部系统回调后再流转 |
| 6 | `AUTOPROCESS` | **自动处理节点** | 机器人节点：到期或条件满足后由系统以机器人身份自动"提交"（写日志时备注按机器人节点处理，`RequestLogBiz:196`），可配置异常处理类型与干预对象 | 自动通过校验节点、系统代批 |

> 说明：E8 老文档常写"0创建/1审批/2实现/3归档"，E9 源码枚举精确语义为上表；4/5/6 是 E9 新增（4 仅自由节点域，5/6 即"机器人节点"，二者共用 `GetRobotNodeSet` 配置页）。

### 1.3 节点属性与会签体系

- **会签关系 `SignOrder`**（枚举实证）：`0=非会签（或签，一人通过即过）`、`1=会签（全部通过）`、`2=依次逐个处理`、`3=抄送(不需提交)`、`4=抄送(需提交)`。执行逻辑在 `RequestNodeFlow.getNextOperator()`（行 2743 起）与 `getNextOrderOperator()`（行 2182 起）中按关系计算操作者集合与放行条件。
- **协办人**：`isremark==7`，与主办人独立会签（`RequestManager:4026-4532` 多分支处理协办人提交对主办人的影响）。
- **节点属性 `nodeattribute`** 与 **签收 `signtype`**、**重新打开 `isreopen`**、**是否允许退回 `isreject`** 等布尔/枚举属性由 `WorkflowNodeComInfo` 缓存类统一暴露。
- **超时体系 `NodeOverTimeInfo`**（实证字段）：超时前提醒（多通道：工作流提醒/短信/邮件/微信/消息中心，对象可为操作者本人/创建人/经理/指定对象）+ 超时后自动处理（`IsAutoFlow`：自动流转至下一操作者 / 退回 `flowobjectreject` / 提交至目标节点 `flowobjectsubmit` / 交流程干预对象，可携带出口条件与处理意见）。
- **节点前/后附加操作**：`RequestPreAddinoperateManager`（前）、`weaver.workflow.action.BaseAction`（后），动作类型涵盖 Java/SQL/WebService/SAP 等（见已有文档《泛微E9工作流引擎分析.md》§3.4）。

### 1.4 出口（连线）机制 —— E9 的"流转规则"核心

`RequestNodeFlow.getNextNode()`（行 1462 起，约 900 行）是一条**多分支出口选择引擎**，按序处理：

1. **重新打开**（`isreopen=1`）→ 直接回创建节点。
2. **退回到指定节点 / 提交至指定节点**（`RejectToNodeid / SubmitToNodeid`）：
   - 退回细分 `RejectToType`：`0=退回指定节点`、`1=重新分叉`（以目标节点为起点重算所有出口）、`2=取消合并`（回退到分叉各支路的最后节点，`getBrancheLastNode`）。
   - 主干提交至分支中间节点：`getLastOperateNode` 求分支末端，反向寻找出口。
3. **强制归档**（`isForceOver + forceOverNodeId`）→ 直接指定归档节点，忽略常规出口。
4. **常规出口评估**（行 1750 起）：按 `linkorder` 顺序遍历 `workflow_nodelink`，每条出口计算：
   - `condition`：**老条件**，直接拼 SQL 对业务表（`workflow_form` 或单据表）执行 `count(*)>0` 判断；
   - `newrule`：**新规则引擎**（`RuleInterface.compareRuleforWF`，规则设计器产物）；
   - `ruleRelationship`：老条件与新规则 **1=且 / 2=或**；
   - `ismustpass` 必经出口、`occurnodeids` 防环、`passnum` 通过次数、`isBulidCode` 出口生成编号、`newconditioncn` 条件中文日志。
   - **无出口满足时报错拦截**（`ReqFlowFailMsgType.LINK_NOE_SET`，提示出口未设置/仅退回出口）。
5. **指定流转**（`selectNextFlowNodeList`，提交人手工选路）→ 跳过条件计算，直接命中 `SelectNextFlowBiz.findSelectFlowLinkInfo` 找到的出口。

### 1.5 操作者体系（`OperatorBigType` + `OperatorItemType`，枚举实证）

**5 大类**：`1 通用`、`2 表单字段`、`3 矩阵对应`、`4 客户门户`、`5 外部接口`；细项 26 种，代表性的：

| 大类 | 细项 |
|---|---|
| 通用 | 指定人员/部门/分部/角色/岗位/所有人/**流程创建人**/**节点操作者**/当前操作者/单位类型 |
| 表单字段 | 人力资源/部门/分部/角色/角色人员/岗位 + 业务浏览框（收发文单位/会议室/文档/项目/资产/客户）|
| 外部接口 | 自定义 Java 接口（501）、自定义 SQL（502） |

操作者确定后还可叠加**代理**（`WorkflowAgentOp`/`WFAgentManager`）、**按序取值**（`getNextOrderOperator` 逐个处理时按 orderbyorder 取值）等策略。

### 1.6 特殊流转能力一览（源码类名实证）

| 能力 | 实现类 |
|---|---|
| 自由流程（运行期由操作者手工串节点） | `WFFreeFlowManager`、`FreeWorkflowNextoperator()`（节点动态写入 `workflow_nodebase`，`requestid/startnodeid/floworder` 标识链） |
| 自由节点 | `FreeNodeBiz`、`RequestFreeNodeFlowBiz`（`RequestNodeFlow.setFreeNodeFlowBiz`） |
| 指定流转（提交时选路） | `SelectNextFlowBiz`、`SelectNextFlowMode` |
| 子流程 | `SubWorkflowManager`、`SubWorkflowTriggerService`（主流程节点后触发/随主流程参数） |
| 强制归档 / 强制撤回 | `WfForceOver`、`WfForceDrawBack`、`RequestForceDrawBack` |
| 转办/协办/代理/传阅 | `WFCoadjutantManager`、`WFForwardManager`、`WorkflowAgentOp`、`SignOrder.CC_*` |
| 流程版本/模板 | `WorkflowVersion`、`workflow_base.version/activeversionid/istemplate` |
| 流转异常处理 | `FlowExceptionHandle`（`eh_callAgainNextNodes/eh_unAppendNextNodes/eh_original_nodeinfo`，支持重算下一节点/追加节点/还原节点信息） |

---

## 2. Flowable 7 节点体系

### 2.1 节点分类（BPMN 2.0 规范原语）

Flowable 是 **BPMN 2.0 标准引擎**，节点分四大家族：

**① 事件（Event）**
- 启动：None / 定时 / 消息 / 信号 / 条件 / 错误；流程级/事件子流程级。
- 边界：定时 / 消息 / 信号 / 错误 / 升级 / 补偿 / 中断与非中断。
- 中间：捕获（定时/消息/信号/条件）与抛出（None/信号/补偿/升级）。
- 结束：None / 错误 / 升级 / 终止（Terminate）/ 取消。

**② 活动（Activity）**
- `UserTask` 人工任务、`ServiceTask`（JavaDelegate/表达式）、`ScriptTask`、`ReceiveTask`、`MailTask`、`HttpTask`、`ShellTask`、`BusinessRuleTask(DMN)`、`ManualTask`。
- 子流程：嵌入子流程、`Call Activity` 调用活动、事件子流程、事务子流程、特别（AdHoc）子流程。
- **多实例（Multi-Instance）**：串行/并行 + `completionCondition` —— 这是对应 E9 会签/依次逐个的标准机制。

**③ 网关（Gateway）**
- 排他（XOR，`conditionExpression`）、并行（AND）、包容（OR）、事件网关、复杂网关。

**④ 连线（SequenceFlow）**
- 条件流 + 默认流；条件是**连线上的 `conditionExpression`（UEL）**，通常配合排他网关使用。

### 2.2 执行逻辑

- **令牌驱动 + 持久化执行树**：`ACT_RU_EXECUTION` 记录执行分支，节点进入即创建 Task/Job，历史走 `ACT_HI_*`；与 E9 的"当前节点+待办行改写"本质不同——Flowable 里"并行"是一等公民（多个 execution 分支），E9 的并行分叉/合并是靠 `ismustpass` 必经 + 分叉节点属性 + `passnum` 模拟的。
- 异步执行器（`async executor`）+ Job 表支撑定时/异步节点；`ChangeActivityState`（运行期把令牌迁到任意活动）支撑退回/跳转/补发。
- **Flowable 7.0 要点**（官方 Release）：基线 Java 17 + Spring Boot 3.x；应用拆分整合（引擎+REST 单应用，UI 应用独立）；清理废弃 API；事件注册（Event Registry）、CMMN/DMN 引擎与 BPMN 并列；JTA/事务与可变数据模型等微调。**节点语义相对 6.x 无破坏性变化**，升级关注点在 API/配置而非节点能力。

### 2.3 扩展方式（与 E9 的根本差异点）

| 扩展点 | 机制 |
|---|---|
| 节点业务逻辑 | `JavaDelegate` / `SpringBean` 表达式 / `DelegateExpression` |
| 生命周期钩子 | `ExecutionListener`（start/end/transition take）、`TaskListener`（create/assignment/complete/delete） |
| 人员分配 | `assignee/candidateUsers/candidateGroups` 表达式、`TaskIdentityLink`、自定义 `IdentityLinkInterceptor` |
| 全局事件 | `FlowableEventListener`（引擎事件总线）+ `eventListeners` 装配 |
| 解析期定制 | `BpmnParseHandler` / `ProcessEngineConfigurator`（改模型、注入行为） |
| 运行期改流 | `ChangeActivityState`、动态 `ProcessInstanceModificationBuilder`、`DynamicBpmnService` |
| 定时/超时 | 边界定时器、中间定时事件、自定义 JobHandler |
| 输出映射 | 变量作用域（execution/local）、`IoMapping` |

---

## 3. 节点分类与功能对比

### 3.1 节点类型映射表（E9 → BPMN/Flowable 7）

| E9 节点 | BPMN/Flowable 对应物 | 等价度 | 差异要点 |
|---|---|---|---|
| 创建节点(0) | `StartEvent` + `UserTask`（发起人待办） | ★★★★☆ | E9 创建节点本身是一个可退回、可重新打开的待办节点；BPMN 起点不可"重新打开"，需 `ChangeActivityState` 回迁 |
| 审批节点(1) | `UserTask` | ★★★★★ | 语义最接近；E9 的会签/或签/依次 = 多实例模式差异 |
| 提交节点(2) | `UserTask`（同 1，仅菜单语义差） | ★★★★★ | E9 用类型区分交互文案，BPMN 无此概念，靠自定义属性（`<flowable:attribute>`）承载 |
| 归档节点(3) | `EndEvent` + 归档 `ExecutionListener` | ★★★★☆ | E9 归档是"节点"（可配操作者做最后确认），BPMN 是事件；若要"归档节点有人工动作"，需保留一个尾端 UserTask |
| 传阅(4) | 无标准对应（近似只读任务/知会） | ★★☆☆☆ | 需自建"抄送任务"体系（不产生流转，仅留痕） |
| 等待节点(5) | 中间定时捕获事件 / `ReceiveTask` + 边界定时器 | ★★★☆☆ | E9"永久等待"= `ReceiveTask`；"等待时长"= 定时事件；但 E9 等待期间可被干预（跳过/加速），Flowable 需自行实现 |
| 自动处理节点(6) | `ServiceTask` 或 定时 Job + 机器人提交 | ★★★☆☆ | E9 机器人以"系统操作者"身份走完整提交链路（留痕一致）；Flowable ServiceTask 不产生"系统待办→自动办结"的日志序列，需模拟 |

### 3.2 流转机制核心差异

| 维度 | E9 | Flowable 7 |
|---|---|---|
| 流转模型 | 状态机：改写 `workflow_currentoperator` | 令牌：execution 树推进 |
| 出口条件 | 表驱动：`condition`(SQL 拼接) + `newrule`(规则引擎) + 且/或，**连线级** | `conditionExpression`(UEL) 在排他/包容网关或连线上，**表达式级** |
| 并行 | 分叉/合并节点属性 + `ismustpass` 必经出口 + `passnum`（弱并行） | 并行/包容网关 + execution 分支（强并行） |
| 会签 | 操作组 `SignOrder` + 操作者集合放行逻辑 | 多实例任务（串/并行 + 完成条件） |
| 退回 | `RejectToType` 3 模式 + 退回出口 `isreject` | `ChangeActivityState` 迁移（无"退回出口"概念，需自行在出口上加 `flowable:reject` 扩展属性） |
| 超时 | 节点超时表 + 后台扫描（提醒/自动流转/退回/干预一体） | 边界定时器（中断/非中断）+ Job；提醒需自建 |
| 等待 | 独立节点类型(5)，后台线程扫描 | 定时中间事件 / ReceiveTask；无"机器人扫描"范式 |
| 操作者解析 | 26 种细项的操作组引擎（表驱动，含 SQL/Java 接口） | assignee/candidate 表达式 + 自定义 `IdentityLink` 逻辑（需自建操作组 DSL） |
| 附加操作 | 节点前/后/出口附加规则（界面可配，多动作类型） | ExecutionListener/TaskListener（编码或自定义属性驱动） |
| 自由流程 | 运行期动态插节点入 `workflow_nodebase` | 无原生等价；可 `DynamicBpmnService` 动态改模型，或走"免模型引擎"路线 |
| 指定流转 | 提交时人工选路，命中指定出口 | `ChangeActivityState` 可达，但"选路 UI+出口合法性"需自建 |
| 版本 | `workflow_base.version` 双表版本切换 | 流程定义 key+version 原生支持（更完整） |

### 3.3 扩展方式与架构差异

- **E9**：JSP + `RequestManager` 巨类（约万行）+ 表驱动配置；扩展=改代码/加动作类，无依赖注入，缓存类全内存（`CacheBase` 家族）；强绑定自家表单（`isbill` 双态：表单/单据）。
- **Flowable**：Spring 生态一等公民；扩展=配置化装配（Listener/Delegate/Configurator/EventRegistry）；模型与表单解耦（表单可任意）；社区与文档成熟。

### 3.4 功能覆盖矩阵（节点功能视角）

| 能力 | E9 | Flowable 原生 | 结论 |
|---|---|---|---|
| 创建/审批/提交/归档 | ✔ | ✔(映射) | 可对齐 |
| 会签/或签/依次/抄送 | ✔(操作组内建) | ✔(多实例+自定义) | 需包装 |
| 出口条件(SQL/规则引擎/且或) | ✔(内建) | 部分(UEL 表达式) | **需扩展** |
| 必经出口/防环/passnum | ✔(内建) | ✘(靠网关语义) | **需扩展** |
| 退回三模式/强制归档/强制撤回 | ✔(内建) | 部分(ChangeActivityState) | **需扩展** |
| 指定流转/自由流程/自由节点 | ✔(内建) | ✘ | **需扩展/重构** |
| 机器人节点(等待/自动处理) | ✔(内建) | 部分(Timer/ServiceTask) | **需扩展** |
| 超时提醒+自动处理 | ✔(内建) | 部分(边界 Timer) | **需扩展** |
| 附加操作(界面可配) | ✔(内建) | 部分(Listener 需编码) | **需扩展** |
| 操作者 26 细项引擎 | ✔(内建) | ✘ | **需扩展** |
| 代理/转办/协办/传阅 | ✔(内建) | 部分(Task 操作) | 需包装 |
| 并行/包容/事件网关 | 弱(模拟) | ✔(原生强项) | Flowable 更强 |
| 边界事件/补偿/事务子流程 | ✘ | ✔(原生强项) | Flowable 更强 |
| 历史与审计 | ✔(requestlog 全链路) | ✔(ACT_HI_*) | 可对齐 |
| 流程版本 | ✔ | ✔(更规范) | 可对齐 |

---

## 4. Flowable 7 改造成 E9 功能一致的可行性评估

**总体判断：可行，且本项目（springBlade）已在实践该路线。** 推荐采用**混合架构**：BPMN 引擎作为执行内核，E9 式配置模型（节点/出口/操作者组/附加操作）作为外层 DSL，运行期由"翻译层"把 E9 配置落到 BPMN 原语与扩展属性上——而非把 Flowable 的 BPMN 语义硬掰成 E9。

### 4.1 可直接等价实现（无侵入或低侵入）

| E9 功能 | Flowable 7 实现方案 |
|---|---|
| 审批/提交节点、会签/或签/依次 | `UserTask` + 多实例（并行=会签、`nrOfCompletedInstances>=1`=或签、串行=依次）；会签关系作为节点扩展属性 |
| 归档 | 尾端 `UserTask`(可选)+`EndEvent`，归档监听器写办结状态 |
| 出口条件 | 保留 E9 式出口表（自建 `wf_node_link`），提交时先跑自研条件求值器（SQL/规则/且或），把结果翻译为"下一节点"，用 `ChangeActivityState`/`TaskService.complete(变量)` 命中对应连线（变量驱动排他网关） |
| 节点前/后附加操作 | `ExecutionListener(start/end)` + 自研动作注册器（对齐 E9 的 Java/SQL/WS 动作类型），配置存节点 `ext_json` |
| 超时 | 边界定时器（中断型=超时自动流转，非中断型=仅提醒）+ 自研提醒发送器；或自建调度扫描（对齐 E9 `NodeOverTimeInfo` 全功能） |
| 退回/指定流转/强制归档 | `runtimeService.createChangeActivityStateBuilder()` 目标迁移；退回三模式在外层封装（记录分叉树信息以支持"重新分叉/取消合并"） |
| 代理/转办 | `TaskService.setAssignee/setOwner` + `IdentityLink`；代理用 `IdentityLinkInterceptor` 或提交前换人 |
| 流程版本 | 原生 definition version；建模发布时生成新版本 |
| 历史审计 | `ACT_HI_TASKINST/ACTIVTY/IDENTITYLINK` + 自研操作日志表（补 E9 requestlog 的富客户端信息字段） |

### 4.2 需要额外扩展开发（有工作量但可落地）

| E9 功能 | 缺口 | 扩展方案 | 工作量级 |
|---|---|---|---|
| 操作者组引擎（26 细项） | Flowable 只有表达式 | 自建 `wf_node_operator` 配置 + `OperatorResolver` SPI（人员/部门/角色/岗位/表单字段/矩阵/SQL/Java 接口…），提交前解析出 assignee 集合再下发任务 | 中（本项目已实现大头：人员与组织选择组件 + NodeOperator 配置） |
| 机器人节点（等待/自动处理） | 无"系统待办→自动办结"范式 | 自建 `wf_robot_job` 扫描器（对齐 `RobotNodeThread`）：等待节点落一条系统"虚拟待办"，到期后由机器人身份调用完整提交流程（保证日志一致性）；自动处理节点同理 | 中 |
| 传阅/抄送（不需/需提交） | 无原生 | 自建 `wf_cc_task`（不走 BPMN），或用"只读 UserTask + TaskListener 旁路" | 小-中 |
| 重新打开（回创建节点） | BPMN 起点不可回迁 | `ChangeActivityState` 迁回第一个 UserTask + 状态标记 | 小 |
| 出口生成编号（isBulidCode）/出口级操作 | 出口无 Listener | 出口命中后由外层执行出口附加动作（本项目"出口信息"页签已承载出口级配置） | 小 |
| 自由流程/自由节点 | 无等价物 | 两条路线：a) `DynamicBpmnService` 运行期改 BPMN 模型（能力受限）；b) **免模型**：自由流程不走 BPMN，直接用 E9 式 node/link 状态机执行（推荐，本项目引擎抽象已支持双态） | 大（可降级为后期特性） |
| 流转异常处理（重算/追加/还原） | — | 在流转失败的 catch 层重建上下文重试（对齐 `FlowExceptionHandle`） | 中 |

### 4.3 存在限制 / 不建议硬对齐的功能

1. **E9"出口条件直拼 SQL"的安全与表达力双输**：`condition` 直接 `select count(*) ... and <用户配置SQL>`，E9 靠权限内控；Flowable 化改造建议收敛为结构化规则（字段+操作符+值/表达式），而不是保留裸 SQL——这是**有意不兼容**项。
2. **E9 弱并行的"必经出口/分叉取消合并"**：在 BPMN 强并行语义下没有一一对应；强行模拟会出现 execution 泄漏。建议以 BPMN 网关重建并行场景，仅在展示层提供 E9 风格的"必经"视觉标记。
3. **创建节点作为"可退回待办"**：BPMN 的流程发起与人工待办是两个概念；回迁可做，但"E9 那种发起节点超时/代理/会签"等配置应视为反模式，建议裁剪。
4. **机器人节点日志形态**：E9 机器人复用人工提交全链路（含待办行）；Flowable 若用 ServiceTask 则无待办痕迹。若审计要求与 E9 逐字对齐，必须走"虚拟待办+机器人提交"，成本高于收益，建议按需降级（仅保留自动流转结果与日志）。
5. **性能模型差异**：E9 每步一次状态行改写；Flowable 有 execution 树/历史/Job 多表写放大。超大并发待办场景需做异步执行器调优与历史级别裁剪（`ACT_HI_*` level）。

### 4.4 改造路线建议（结合本工作区现状）

现状：`springBlade` 已落地 E9 式节点/出口/操作者/超时/附加操作/指定流转/子流程（`wf_process_node.ext_json` 驱动，`WfTimeoutJob`、`WfActionExecutor` 已运行）。后续补齐优先级：

1. **P0 会签语义**：多实例改造 `UserTask`，操作组 `SignOrder` → 多实例完成条件翻译；
2. **P0 退回三模式**：`ChangeActivityState` 封装 + 分叉树快照表；
3. **P1 机器人节点**：`wf_robot` 扫描 Job + 虚拟待办；
4. **P1 传阅/抄送**：独立抄送任务表 + 站内信；
5. **P2 自由流程**：免模型状态机（复用现有 node/link 存储，跳过 BPMN）；
6. **持续**：出口条件求值器结构化（去裸 SQL）。

### 4.5 结论

- **功能覆盖**：E9 的节点功能中约 **90% 属"业务流程审批"范畴**，可在 Flowable 7 上以「BPMN 原语 + 自研配置层 + Listener/ChangeActivityState 扩展」完整对齐；E9 相对 Flowable 的独有项集中在"操作者组引擎、机器人节点、自由流程、E9 式出口条件"四块，均为**可开发的扩展**而非原理性障碍。
- **反向价值**：Flowable 原生的强并行、边界事件、补偿/事务子流程、版本管理与监控生态是 E9 不具备的，改造后能力**超出**而非仅追平 E9。
- **风险**：主要成本不在引擎改造，而在**语义翻译层**（E9 配置→BPMN 原语）的完备性与审计日志的逐字对齐；自由流程是唯一建议降级或延后的特性。
- **最终结论**：**改造可行，推荐混合架构路线**（BPMN 内核 + E9 式 DSL 外层），本项目即该路线的实证案例；不建议追求"用 BPMN 原语逐字复刻 E9 状态机"的强对齐方案。

---

## 附录：E9 源码证据索引

| 结论 | 源码位置 |
|---|---|
| 节点类型 7 种枚举 | `src/com/engine/workflow/constant/node/NodeType.java` |
| 会签关系 5 值 | `src/com/engine/workflow/constant/node/SignOrder.java` |
| 操作者 5 大类/26 细项 | `src/com/engine/workflow/constant/node/OperatorBigType.java`、`OperatorItemType.java` |
| 出口选择引擎（退回三模式/强制归档/必经/防环/条件评估/指定流转） | `src/weaver/workflow/request/RequestNodeFlow.java`（getNextNode，行 1462 起） |
| 操作者计算/会签/依次/转办模拟 | `RequestNodeFlow.java`（getNextOperator 行 2743、getNextOrderOperator 行 2182、getNextOperatorByAssignUser 行 3480、FreeWorkflowNextoperator 行 3690） |
| 机器人节点扫描与等待类型 | `src/com/engine/workflow/biz/RobotNode/RobotNodeServiceBiz.java`、`RobotNodeBiz.java`（operatorid="0"） |
| 超时提醒与自动处理 | `src/weaver/workflow/node/NodeOverTimeInfo.java` |
| 机器人节点无需操作菜单 | `src/com/engine/workflow/cmd/workflowPath/node/GetNodeSessionkeyCmd.java`（行 497） |
| 自由流程 | `src/weaver/workflow/request/WFFreeFlowManager.java` |
| 协办人会签逻辑 | `src/weaver/workflow/request/RequestManager.java`（行 4026-4532） |
| 节点缓存与自由节点过滤 | `src/weaver/workflow/workflow/WorkflowNodeComInfo.java`（TABLE_WHERE 过滤 isfreenode） |
| Flowable 7.0 基线与变更 | 官方 7.0.0 Release（Java 17 / Spring Boot 3 / 引擎整合），节点语义与 6.x 兼容 |

---

**文档版本**：1.0.0
**发布日期**：2026-09-13
**作者**：CodeBuddy AI
**适用范围**：springBlade 工作流模块（Flowable 内核）与 E9 引擎对齐改造决策参考
