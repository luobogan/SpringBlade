# 泛微 E9 自由流程在 SpringBlade 的落地分析与实现建议

> **文档性质**：调研 + 落地方案（待评审，非定稿设计）
> **日期**：2026-11-04
> **需求基线**：《泛微E9-收发文与正文套红功能分析及SpringBlade落地方案》及 `doc/md` 下 E9 流程系列分析
> **核查范围**：`D:\workproject\ecology\src`（E9 Java 源码）、`D:\Weaver2020\ecology`（E9 部署态）、`springBlade`（后端 + 前端）、`springBlade\doc\md`（既有分析文档）
> **声明**：本文全部结论来自**只读**检索与代码核查，**未修改任何业务代码**；文中「推断」类结论已单独标注，不与源码事实混同。

---

## 0. 摘要（结论先行）

1. **E9 自由流程的本质是「逐实例运行期动态建节点 + 动态建链」**，不是"预置节点池 + 运行时选路"。它在存储上复用 `workflow_nodelink`，在代码路径上复用 `RequestNodeFlow.getNextNodes()`，但用 `isSelectNextFlow` **短路出口条件求值**，以"人选 + 顺序"替代"条件 + linkorder"。
2. **E9 自身有两套不同的"自由"机制**（老自由流程：正 id / `workflow_nodebase`；新自由节点：负 id / `workflow_freenode`），数据模型不同、能力不同。springBlade 的 `freeWfType` 注释对齐的是**新体系**，而 `doc/md` 专文分析的是**老体系**，**口径不一致，落地前必须先拍板**。
3. **springBlade 现状 = 一个有害的空壳 + 一个被低估的底座**：`isFree/freeWfType` 已建列、已做设计器开关，但**运行期零消费**（勾了开关行为与普通流程完全一样，且提示"不允许关闭"）；而「指定流转」已具备运行期跳节点、指定办理人、多目标并行扇出的完整链路，是最接近自由流程的底座。
4. **`doc/md` 里没有一份 springBlade 侧的自由流程落地设计**——只有 E9 分析专文 + 对比评估里一句 P2 路线 + 技术选型里一个"待业务确认"项。本文即补此缺口。
5. **对比评估中"本项目引擎抽象已支持双态，故推荐免模型"的论据被源码证伪**：全库无 `IWorkflowEngine` / `EngineGateway` / `WfInstanceActReader`，所谓"双态"只是 wf 表↔ACT 表的**读源开关**；叠加 `instance-read-source=act` 与"去 wf_ 表"两项现状，免模型路线成本被明显低估。
6. **推荐分期**：P0 处理开关半成品 → **P1 用「预置节点池 + 指定流转增强」交付可用的伪自由流程**（改动小、复用全部现有配套、可测可回滚）→ P2 仅在业务确有"任意串节点 / 分支合并 / 模板"需求时，再评估免模型通道，并把配套适配清单作为独立工作量核算。
7. **`flowtable` 能"摆"不能"跑"**（见 §7.6）：业务表单表 `formtable_main_*` 可承载自由流程步骤的**录入与展示**，但**不能作为运行期流转真源**——列设计期固定、明细表流程链路写不进、无稳定顺序、引擎不读业务表、删除不清理且写入侧无权限裁剪。执行真源必须新建实例级表；**P1 路线 A 则完全不必碰 flowtable**。
8. **第二轮复查发现更关键的连锁缺口**（见 §10）：springBlade 全链路以 `def_id + node_key` 为索引，而初版建议的 `wf_free_node` 表**未定义 `node_key`**。运行期动态节点若无稳定 key，会连带打断**公文四个钩子（套红 / 签章 / 密级 / 定稿取号静默跳过）**、节点级字段权限与操作菜单（缺配置被兜底解释为"全放行"）、进度图 / 流转意见 / 退回 / 超时 / 表单快照历史。**这进一步佐证 P1 路线 A（预置静态节点池）的稳健性**——它保留真实 activityId，天然规避该缺口；但 P1 仍有「改 BPMN 必须发布才生效」的硬前提（§10.4）。

---

## 1. 资料来源与既有分析盘点

### 1.1 之前整理的自由流程产物

| 资料 | 位置 | 内容 |
|---|---|---|
| 专文 | `doc/md/泛微E9-流程自由流程(路由复用)分析.md` | 回答"自由流程是否复用出口条件选择算法"，结论：**部分复用**（复用数据模型与代码路径，不复用条件求值子算法） |
| 上篇 | `doc/md/泛微E9-流程引擎流转核心深度分析.md` §10 未深入点 #5 | 专文的引子 |
| E9 源码 | `D:\workproject\ecology\src\weaver\workflow\request\WFFreeFlowManager.java` | 老自由流程定义层（动态建节点、建链、复制出口） |
| E9 源码 | `D:\workproject\ecology\src\weaver\workflow\request\RequestNodeFlow.java` | 运行期流转（`FreeWorkflowNextoperator()`、`isSelectNextFlow`） |
| E9 部署态 | `D:\Weaver2020\ecology\workflow\request\FreeWorkflowSet.jsp` / `FreeNodeShow.jsp` | 老设计器（表格 / 图形） |
| E9 部署态 | `D:\Weaver2020\ecology\src4js\pc4backstage\workflow\components\pathSet\workflowDesign\workflowFree\` | 新自由流程 React 设计器（9 个文件） |

### 1.2 `doc/md` 中涉及自由流程的文档

| 文档 | 命中 | 与自由流程的关系 |
|---|---|---|
| `泛微E9-流程自由流程(路由复用)分析.md` | 25 | ★★★ 专文，运行期算法结论 |
| `审批流程方案-现状分析与技术选型.md` | 18 | ★★ E9 表结构源码级；§1157 把"自由流程是否需迁移"列为**待业务确认** |
| `E9与Flowable7流程节点功能对比及改造可行性评估.md` | 14 | ★★★ **唯一给出落地路线**（§4.2 / §4.4 / §4.5） |
| `泛微流程节点信息设置项分析.md` | 14 | ★ 自由节点过滤（`isFree=1` 排除）、`total` 动态计算 |
| `泛微E9工作流引擎分析.md` | 10 | ★ `workflow_base.isFree` 字段与 `WFManager` setter |
| `泛微E9-流程workflow模块架构深度分析.md` | 6 | ★ 模块归属 |
| `SpringBlade工作流引擎迁移方案.md` | 6 | ★ 实体/DTO/DDL 带 `is_free`，**无实现** |
| `泛微流程操作菜单与退回机制分析.md` | 4 | ★ 自由节点退回 `RequestFreeNodeFlowBiz.getFreeRejectNodeids` |
| `审批流程-开发任务说明.md` | 4 | ★ 任务排期提及 |
| `泛微E9-流程引擎流转核心深度分析.md` | 4 | ★ 专文上篇 |
| `泛微E9-跨模块风险登记册.md` | 2 | ★ WF-P1-2：E9 自身"设计器两套独立实现"风险 |
| `泛微E9-收发文与正文套红功能分析及SpringBlade落地方案.md` | 2 | ★ 公文侧 `freeFlowTable` 权限表格 |
| 其余（`泛微E9-流程测试功能分析.md`、`去wf_表-定义主记录翻源设计.md`、`流程版本控制设计与改造说明.md`、`收发文一体化技术方案.md`、`审批流程-数据库结构与迁移记录.md`、`泛微E9表单建模-源码级完整功能分析.md`） | 各 1 | 边缘引用 |

### 1.3 结论

**既有分析全部是"E9 侧分析"或"能力对比"，没有一份落到 springBlade 的实现设计**。唯一的实现指引是对比评估 §4.4 的一行：

> 5. **P2 自由流程**：免模型状态机（复用现有 node/link 存储，跳过 BPMN）；

该指引的两条支撑论据在本次源码核查中均需修正（见 §4）。

---

## 2. E9 自由流程的本质机制

### 2.1 两套"自由"必须分开建模

| 维度 | 老「自由流程」 | 新「自由节点 / 传阅」 |
|---|---|---|
| 开关 | `workflow_base.isfree=1`（+ `freewftype` 1 简易 / 2 高级） | 任意流程可挂；`FreeNodeBiz.isFreeFlow(workflowid)` 仅为其中一种条件 |
| 节点 id | **正整数**，`workflow_nodebase.IsFreeNode='1'` | **负数**（`isFreeNode(nodeid){ return nodeid < -1; }`） |
| 表 | `workflow_nodebase` + `workflow_nodelink` | `workflow_freenode` / `_group` / `_template` / `_sequence` |
| 操作者 | 节点行上 `operators` 字符串 + `Signtype` | `operators/operatorids/signType` + `FreeNodeGroupEntity` 串并行/合并 |
| 引擎 | `WFFreeFlowManager` + `RequestNodeFlow.FreeWorkflowNextoperator()` | `FreeNodeBiz` / `RequestFreeNodeFlowBiz` + `addFreeNodeOperator()` |
| 能力 | 纯串联、逐实例 | 串并行分支、合并策略、模板、传阅、运行期插节点 |
| 设计器 | `FreeWorkflowSet.jsp`（表格）/ `FreeNodeShow.jsp`（图形） | `freeNodeFlowSet.html` + `workflowFree/` React 组件 |

**口径警示**：springBlade `WfProcessDefinition.freeWfType` 注释写的是「对齐 ecology `newFreeWfType`」（**新体系**），而本文 §2.2-§2.5 分析的老自由流程与 `doc/md` 专文对应**新体系之前的实现**。若按 `freeWfType=2 高级模式` 开工，实际要做的是"新自由节点"（分支/合并/模板），工作量远大于老自由流程。

### 2.2 定义层：运行期动态建节点 + 动态建链

`WFFreeFlowManager.SaveFreeFlow(...)` 为**每个实例、每个自定义步骤**插一行节点，再插 `workflow_nodelink` 串成链：

```java
// D:/workproject/ecology/src/weaver/workflow/request/WFFreeFlowManager.java:455-456
sql = "update workflow_nodebase set isstart='0',...,IsFreeNode='1',floworder=" + floworder
    + ",Signtype=" + Signtype + ",operators='" + operators + "',requestid=" + requestid
    + ",startnodeid=" + startnodeid + " where id=" + newnodeid;
```

| 字段 | 含义 |
|---|---|
| `requestid` | 所属流程实例（动态节点**逐实例私有**，删除时按此清理） |
| `startnodeid` | 动态段挂载的定义态节点 id，可递归找祖先 |
| `floworder` | 步骤顺序（`order by floworder,id`） |
| `IsFreeNode='1'` | 运行期节点标记；定义态节点缓存 `WorkflowNodeComInfo.TABLE_WHERE` 显式排除 |
| `operators` / `Signtype` | **直接把人 ID 串和会签类型存在节点行上**，不走 26 种操作组 |

`SaveFreeFlow` 还会：删除该实例旧的动态节点（L275）、为新节点建操作组 `CreateNewGroup`（L167）、写 `workflow_flownode(IsFreeWorkflow='1')`（L461-470）、写权限 `workflow_freeright(isroutedit,istableedit)` 与 `workflow_function_manage`（L429-440）、把同一份自由步骤复制到所有兄弟节点（L1039-1099）。

**是"每实例建节点行"，不是共享**，也不是"预置节点池"：

```java
// WFFreeFlowManager.java:275
sql = "delete from workflow_nodebase where IsFreeNode='1' and startnodeid=" + startnodeid
    + " and requestid=" + requestid + " and id not in (" + getExistsFreeNodeIds(params) + ")";
```

E9 中可复用的只有**模板**（新引擎 `workflow_freeflow_template`，`SaveFreeNodeCmd` 存/导）和"按表单字段批量生成节点"（`GenerateFreeNodeCmd` 的 `nodeData` JSON）。

### 2.3 运行期：短路条件求值

`RequestManager#flowNextNode()` **没有 `if (isfree)` 大分支**，统一构造 `RequestNodeFlow` 后由内部四条分支处理。自由流程段的两处关键代码：

```java
// RequestNodeFlow.java:1044-1061 —— 命中动态节点后绕过普通 getNextNode/getNextOperator
sql = "select id,Signtype,operators from workflow_nodebase where IsFreeNode='1' and requestid="
      + requestid + " and startnodeid=" + nodeid + " order by floworder,id";
if (rs.next() && isreject != 1) {
    IsFreeNode = "1"; nextnodeid = rs.getInt("id");
    FreeSigntype = ...; Freeoperators = rs.getString("operators");
    return FreeWorkflowNextoperator();
}
```

```java
// RequestNodeFlow.java:1891-1894 —— 指定流转/自由流程跳过出口条件求值
for (linkindex = 0; linkindex < nextnodeids.size(); linkindex++) {
    if (isSelectNextFlow) { break; }   // 直接采用用户所选节点
    // 此后才是旧公式 SQL count + 新规则 RuleInterface 求值（L1919-2006）
}
```

| 维度 | 普通固定流程 | 自由流程 / 指定流转 |
|---|---|---|
| 下一节点来源 | 按 `linkorder,id` 遍历出口、逐条求值条件 | 用户手动选定 `selectNextFlowNodeList` |
| 条件求值 | ✅ 旧公式 + 新规则 + 且/或 | ❌ 被 `isSelectNextFlow` 短路 |
| 首匹配短路 | ✅ | N/A |
| 必经分叉 | ✅ 参与 | 仅固定节点段参与 |
| 数据表 | `workflow_nodelink` | **同左**（由 `WFFreeFlowManager` 写入） |

自由段中若含**固定节点**，进入该段时 `this.isSelectNextFlow=false; freeNodeFlowBiz=null; getNextNodes();`（L1184-1188）恢复自动条件求值——两种模式在同一 `getNextNodes()` 内按标志切换。

### 2.4「路由复用」到底指什么

`doc/md/泛微E9-流程自由流程(路由复用)分析.md` 标题中的"路由复用"有三层含义：

1. **数据模型复用**：动态步骤写成 `workflow_nodelink` 行（`WFFreeFlowManager` L698-848），与主干出口同表、同字段。
2. **取数算法复用**：`WFPathUtil.getNodeLinkrs`（L78）用同一条 SQL 按 `(wfrequestid is null or ...)` + `EXISTS` 过滤实例可见节点，实现"定义态/实例态共用一套取数算法"；`RequestNodeFlow.getNextNodes()` 是同一代码路径。
3. **出口原样复制**：`WFFreeFlowManager` L817-918 把 `startnodeid` 原有的每条出口（含 `condition/conditioncn/newrule/linkorder/ruleRelationship`）整条复制，`nodeid` 换成动态链最后一个节点——**动态段结束后"接回主干"，路由配置不重配**。

> 实例隔离靠**节点行上的 `requestid`**（配合 `workflow_nodelink.wfrequestid`），不是靠给每个实例复制一整套定义。

### 2.5 最小数据模型

| 层 | 表 / 字段 |
|---|---|
| 定义开关 | `workflow_base(isfree, freewftype)` |
| 实例节点 | `workflow_nodebase(id, isfreenode, requestid, startnodeid, floworder, operators, signtype, nodeattribute, isstart/isend)` |
| 实例链路 | `workflow_nodelink(nodeid, destnodeid, linkorder, condition, newrule, ruleRelationship, ismustpass, isreject, wfrequestid)` |
| 节点在流程内的配置 | `workflow_flownode(IsFreeWorkflow, FREEFS)` |
| 权限 | `workflow_freeright(nodeid, isroutedit, istableedit)`、`workflow_function_manage` |
| 运行期状态 | `workflow_currentoperator`、`workflow_requestflowinfo` |
| 新自由节点（如需） | `workflow_freenode`、`workflow_freenode_group`、`workflow_freeflow_template`、`workflow_freenodesequence` |

索引参考：`IX_WORKFLOW_NDBS_IR(ISFREENODE, REQUESTID, STARTNODEID, ID)`、`IX_WF_NODELINK_WNIW(WFREQUESTID, NODEID, ISREJECT, WORKFLOWID)`。

### 2.6 权限与范围控制

- **谁能发起**：`RequestCheckUser` 校验创建权限，与是否自由流程无关。
- **谁能改流程 / 改表单**：`workflow_freeright.isroutedit/istableedit`；新体系由 `LoadFreeNodeBiz.getFlowChartEditRight()` 给出 `0只读 / 1可加后续节点 / 2当前节点可加且未操作节点可编辑`，值来自 `FreeNodeEntity.nextNodeEditPurview`。
- **能选哪些人**：老自由流程用 HR 全局多选浏览器（`ShowMultiResource` / `WeaBrowser type:17`），**无自由流程专属范围表**；运行期过滤离职与密级（`RequestFreeNodeFlowBiz:163-167`）。
- **指定流转（选路）才有范围控制**：`GetSelectFlowNodesCmd` L94-98 查 `workflow_FwLimitSet`、`SelectNextFlowBiz.getNodeScopeSql`；`SelectNextFlowMode.NODE_AND_OPERATOR(1) / NODE_ONLY(2)`。
- **治理提示（专文 WF-N9）**：指定流转/自由流程模式下出口条件**不被求值**，若某 link 配了本应拦截的条件，手动选择会绕过它。属设计预期，但**节点级提交权限必须独立到位——条件≠权限**。

---

## 3. springBlade 现状核查

### 3.1 空壳：`isFree` 已建模、运行期零消费

```java
// blade-service-api/blade-workflow-api/.../entity/WfProcessDefinition.java:105-109
@Schema(description = "是否自由流程")
private Integer isFree;

@Schema(description = "自由流程类型：1简易 2高级（对齐 ecology newFreeWfType）")
private Integer freeWfType;
```

- DDL：`doc/sql/blade/blade.sql:12499-12500` 有 `is_free tinyint NOT NULL DEFAULT '0'`、`free_wf_type tinyint`。
- 设计器基础信息表单：`WfDefinitionServiceImpl:2673-2691` 有 `switch`「是否为自由流程」（tip："自由流程一旦启用，保存后即不允许关闭"）+ `select`「简易模式 / 高级模式」（`visibleWhen isFree==true`）。
- 版本复制：`WfDefinitionServiceImpl:1390-1391` 把两个值带到新版本。
- **全后端搜 `getIsFree/setIsFree/isFree` 仅 4 处命中，全部在 `WfDefinitionServiceImpl`，且全在定义期**；前端 `services/workflow/index.ts` 与 `pages/System/Workflow/Workflow.tsx` 只有类型/列表字段。

> ⚠️ **这是当前最需处理的误导性半成品**：管理员勾选并发布后，流程行为与普通流程**完全一致**，而开关还不允许关闭。

### 3.2 真正的底座：「指定流转」已具备约 80% 运行期能力

```java
// blade-service/blade-workflow/.../service/impl/WfTaskServiceImpl.java:390-446
int appointMode = WfNodeSettingsUtil.appointFlowMode(node);
if (!system && appointMode != 0) {
    if (appointMode == 3) {
        // 多目标：targets = [{nodeKey, operatorIds[], signType}]，并行扇出
        processService.moveActivityToActivities(inst.getEngineInstId(), task.getNodeKey(), toKeys, vars);
        instanceService.advance(inst.getId(), operator, overrideAssignees, AdvanceSrc.SUBMIT);
    } else {
        String nextNodeKey = dto.getNextNodeKey();
        if (nextNodeKey == null || nextNodeKey.isBlank()) { throw ... "请选择下一节点"; }
        if (loadNode(inst.getDefId(), nextNodeKey) == null) { throw ... "目标节点不存在"; }
        Long overrideAssignee = (appointMode == 1 && dto != null) ? dto.getNextAssignee() : null;
        processService.moveActivity(inst.getEngineInstId(), task.getNodeKey(), nextNodeKey, vars);
        instanceService.advance(inst.getId(), operator, nextNodeKey, overrideAssignee);
    }
}
```

| 能力 | 位置 |
|---|---|
| DTO | `ApproveDTO.nextNodeKey`(L27) / `nextAssignee`(L30)；`RejectDTO.targetNodeKey` |
| 设计期配置 | `WfNodeSettingsUtil.appointFlowMode/Target/Targets`（L633-657）；`nodeSettings.ts`；`wfDict.SELECT_NEXT_FLOW_MODES`；`NodeSettingModal.TargetsEditor` |
| 引擎跳转 | `ProcessServiceImpl.moveActivity`(L314-328) / `moveActivityToActivities`(L331-354)，基于 Flowable `createChangeActivityStateBuilder` |
| 台账推进 | `WfInstanceServiceImpl.advance` 三个重载（`overrideNodeKey` / `overrideAssignee` / `overrideAssignees`）；`advanceInternal`(L2984) 让指定办理人覆盖常规操作者解析 |
| 扩展点 | `NodeActionExecutor`（`PHASE_PRE/POST`、`TRIGGER_AFTER_SUBMIT/ARCHIVE`、`executeLink` 出口级动作）；公文 `WfNodeMustDoActionExecutor.checkBeforeSubmit` |
| 测试态 | `WfTestServiceImpl`（真实引擎 + `is_test=1`）、`deployForTest`、`/test/*` 端点、`cleanupTestData` |

**卡点**：`L410/L439` 强制 `loadNode(defId, nextNodeKey) != null`，**目标必须是已部署 BPMN 中真实存在的 activityId**，不能是运行期新造节点。

### 3.3 四个明确缺口

| # | 缺口 | 证据 |
|---|---|---|
| 1 | **无运行期动态节点** | `WfProcessNode` / `WfNodeLink` 无 `inst_id`、无 `is_free_node` 标记，都是设计期静态行；全库 `dynamicNode\|adHoc\|runtimeNode\|动态插` 在 java/tsx 中 0 命中 |
| 2 | **无免模型执行路径** | `ProcessServiceImpl` 发起必须 `startProcessInstanceById`；全库无 `IWorkflowEngine` / `EngineGateway` / `WfInstanceActReader`；"双态"只是读源开关 |
| 3 | **前端办理页无选路/选人 UI** | `pages` 下 `nextNodeKey\|nextAssignee` **0 命中**；`nextAssignee` 仅单人 `Long`；无「下一节点候选」接口 |
| 4 | **加签不是动态插节点** | `WfTaskServiceImpl#addSign/forward/circulate`(L673/713/745) 只复用 `engineTaskId` 造 `wf_task` 行，不建引擎任务、不插 BPMN 节点 |

### 3.4 可借的先例：合成待办

`WfInstanceServiceImpl#saveDraft`(L732-747) 插 `WfTask` 时**不 setEngineTaskId**（库中存空串 `''`），只设 `nodeKey/assignee/status=0`；`WfTaskActReader`(L206/L243-250) 在读源为 act 时兜底合并"无引擎任务的合成待办"；`WfTaskActWriter`(L98-101) 对空 `engineTaskId` 跳过 ACT 双写。

**证明"无引擎任务的待办"能被列表/角标正确呈现**，是免模型自由流程的天然载体。但正式流转 `advanceInternal`(L2991/L3050-3133) 完全靠 `processService.currentTasks()` 生成任务与判定结束，**目前没有"靠合成待办推进流程"的推进器**。

### 3.5 现状能力矩阵

| 能力 | 状态 | 备注 |
|---|---|---|
| 流程定义 `isFree/freeWfType` | 🟡 空壳 | 列/实体/表单开关齐备，运行期零消费 |
| 指定流转（跳节点 + 指定单人） | ✅ 可用 | `doApprove` L390-446 |
| 多目标并行扇出 | ✅ 可用 | 模式 3，设计期配置 targets |
| 指定多人 / 会签类型 | 🟡 部分 | 引擎多实例集合变量 `wfMiAssignees_<nodeKey>` 已有，但 `nextAssignee` 仅单人 Long |
| 动态节点名 / 顺序 | ❌ | 无 |
| 运行期增删节点 / 任意链 | ❌ | 无 |
| 分支 / 合并 / 模板 | ❌ | 无 |
| 选人 / 选节点范围白名单 | ❌ | 无 |
| 前端配置与办理 UI | ❌ | 只有设计期 `TargetsEditor`，办理页无 UI |
| 免模型执行通道 | ❌ | 写侧必须启 Flowable 实例 |
| 节点前后置 / 出口动作 | ✅ 可复用 | `NodeActionExecutor` |
| 测试态 / 一键清理 | ✅ 可复用 | `is_test` 体系 |

### 3.6 设置界面分层：不是流程设计器那个页面

> 回应一个具体交互问题：**自由流程的设置界面长什么样、是不是"流程设计 / 画流程图"那个页面？**
> 结论：**不是**。自由流程节点是逐实例运行期动态建的，定义期画不出来，所以 E9 把设置拆成 **「定义期只配开关」+「运行期才编排步骤」** 两半。

#### 3.6.1 E9 五类界面一览

| # | 界面 | 时机 / 入口 | 载体 | 可配项 |
|---|---|---|---|---|
| ① **定义期配置** | 新建/编辑流程的「基础信息」表单（**不是 pathSet 画布**） | 老版 `addwf0.jsp:943-956`；新版 `AddCondition.js:64-90` + `BaseInfoCondition.js:100-121`；落库 `DoAddWFCmd.java:105,172-173` / `DoEditWFCmd.java:196,322-327` | 普通表单项，无图 | `isFreeWf` 开关（启用后 `setViewAttr(1)` 只读、不可关闭）、`newFreeWfType` 1 简易 / 2 高级、表单范围 `formRangeType/formRange`、自动创建+归档两节点、`selectnextflowpattern`、节点「流转设定」按钮名 |
| ② **流程设计器画布（画流程图）** | 定义期 `workflowDesign/index.html`（mxGraph） | 画布 | 普通节点 / 连线 / 出口条件；**全量搜 `pathSet/` 无自由流程开关或菜单**；仅节点操作菜单里可配「流转设定」按钮名（`GetOperationMenuInfoCmd.java:984-992`） |
| ③ **运行期步骤编排** ★ | 发起 / 办理页点「**流转设定**」(label 21781) 弹出；创建节点提交时**自动弹** | `window.top.Dialog()`（850×600 / 1000×550 / 550×550）或 SPA `WeaDialog` + iframe；入口 `ManageRequestNoFormIframe.jsp:1787-1793, 3225-3267`、`AddRequestIframe.jsp:581-587, 1827-1863`、`GetRightMenuCmd.java:1777-1780`、`rightBtn.js:680-740` | 见 §3.6.2 |
| ④ **办理页选下一步** | 简易模式提交 / 退回时 | 页内区块 `.wf-req-freeWorkflowSimple`、可拖拽步骤表、canvas 弹窗（`FreeNodeWin.jsp` + `FreeNodeShowSimple.jsp`），含「保存设置 / 保存并提交」 | 选人 / 选步骤、`SubmitToNodeid`、`selectNextFlowNodeList` + `isSelectNextFlow` |
| ⑤ **后台权限设置（odoc）** | 公文后台配置 | `WeaTableEditable` 可编辑表格（`freeFlowCom.js:81-113` 的 `freeFlowTable`） | 类型/对象 + 改签/代录意见/收回/改办理人/归档/调整日志 6 权限；存 `odoc_freeflow_manager`。**与 ③ 步骤编排、`workflow_freeright` 均非同一界面/同一张表** |

> ⚠️ `workflowFree/` 组件虽物理上位于 `pathSet/workflowDesign/` 目录下，但入口参数含 `requestid / isAllowNodeFreeFlow / isReadOnlyModel`，由运行期 `readOnly-index.html` / `freeNodeFlowSet.html` 挂载 —— 它是「**本次请求的自由节点流程图**」，不是定义期路径图。

#### 3.6.2 ③「流转设定」弹窗的三代形态

**a) 老表格版** `FreeWorkflowSet.jsp`（纯 `<TABLE class=ListStyle>`，无画布）：

```text
┌──────────────────────────────────────────────┐
│  [增加步骤]  [删除步骤]              [保存]   │
├────┬────────┬──────────┬──────┬─────────────┤
│ 勾选│ 顺序   │ 步骤/节点名 │ 会签 │ 操作者 [选人]│
├────┼────────┼──────────┼──────┼─────────────┤
│ □  │  1     │ 步骤1     │ 非会签│ 张三,李四    │
│ □  │  2     │ 步骤2     │ 会签  │ 王五        │
└────┴────────┴──────────┴──────┴─────────────┘
```

列为 `check_node / floworder_i / nodename_i / Signtype_i(0非会签/1会签/2依次) / operators_i`（`:74-85, 114-125, 144-161`）；保存调 `WFFreeFlowManager.SaveFreeFlow(request, requestid, nodeid, ...)`（`:34-38`），成功后 `picreload()` 刷新父页流程图（`:410-447`）。

**b) 高级模式** `FreeNodeShow.jsp`（`freewftype=2`）：可拖拽画布（jQuery UI draggable/droppable）+ 节点详情面板，比表格版多 `road`（路径编辑权 0无/1加签/2后续）、`frms`（表单编辑权）、签章 `trust`、`nodeDo`（转发/收回/归档），工具条 `insert / remove / agency`（`:433-438, 590-610`）。

**c) 新 E9 SPA** `WorkflowFreeContainer.js` 三大块：

```text
┌─────────────────────────────────────────────────────┐
│ WfFreeActionTop        [保存设置]                    │  顶部工具条
├──────────────────────────────┬──────────────────────┤
│                              │ WfFreeRightDetail    │
│  WorkflowFreeDrawSvg         │ 节点名/类型/办理人(多选)│
│  （SVG 画布，节点悬浮菜单：   │ 会签类型/串并行/       │
│   删除/添加串行/添加并行）    │ 合并策略(3种)/mustPass │
│                              │ 编辑权限/超时提醒      │
└──────────────────────────────┴──────────────────────┘
```

字段定义 `GetNodeSettingCmd.java:55-135`、实体 `FreeNodeEntity.java:21-69`、合并策略 `WfFreeRightDetail.js:298-419`（按比例 / 按分支数 / 指定通过分支）。

> ⚠️ **E9 自身的坑**：`WfFreeActionTop.js:86-93`「存为模板 / 从模板导入」与 `:98-125`「添加/删除/替换节点」**整段被注释**，实际节点增删只走画布悬浮菜单 `WorkflowFreeNode_v2.js:167-220`；模板功能有独立界面与 API（`SaveTemplateCmd` / `ImportFromTemplateCmd` / `freeflowTemplate/`），但工具条未启用。不要照着注释里的按钮设计。

#### 3.6.3 springBlade 现状对照

现有设计器 `WorkflowDesign.tsx:1254-1261` 是 6 个顶层页签（流程卡片 / **基础设置** / 表单管理 / 流转设置 / 归档与撤回 / 高级设置），「流转设置」下再分「图形编辑 / 节点信息 / 出口信息」三个子页签（`:1104-1115`）。

- **已对齐 ①**：`WfDefinitionServiceImpl:2673-2691` 生成的「是否为自由流程」switch +「简易/高级」下拉落在**「基础设置」页**（不是画布），连 tip「自由流程一旦启用，保存后即不允许关闭」都与 E9 一致 —— 定义期入口位置**正确，无需改动**。
- **完全缺失 ③④**：办理页 `InstanceFlow.tsx` 的弹窗只有退回 / 转办 / 加签 / 传阅 / 附件（`modalType` 那套，`:360-362, 521-528`），**没有「流转设定」步骤编排弹窗，也没有提交时选下一步/选人的 UI**（对应 §3.3 缺口 3）。

#### 3.6.4 界面落点建议

| 阶段 | 落点 | 说明 |
|---|---|---|
| **定义期** | ✅ **现有「流程设计器 → 基础设置」页**，不用改 | 只改开关文案 / P0 隐藏，别去画布加东西 |
| **P1 路线 A** | **办理页 `InstanceFlow.tsx` 新增「下一步设置」弹窗** | 复用现有 `modalType` + `Modal` 机制，内容 = 选节点下拉 + 选多人 + 会签类型；**不新开页面、不碰画布** |
| **P2 真·自由流程** | 新建独立组件 `FreeFlowSetModal`（挂办理页） | 表格版起步（顺序/节点名/办理人/会签/增删行），高级版再上 SVG 编排；可在设计器加「自由流程**模板**」页签，但那是模板管理、不是编排实例 |
| **不要做** | ❌ 在「流转设置 → 图形编辑」画布上加自由流程节点 | 定义期画不出逐实例节点，与 BPMN 静态模型语义冲突 |

> **一句话**：定义期在「基础设置」配开关（已有），运行期在「办理页弹窗」编排步骤（缺）；**流程图设计器画布从头到尾不参与自由流程设置**，它只负责静态 BPMN 骨架。

---

## 4. 与 `doc/md` 方案的偏差（影响可行性判断）

| 方案表述 | 源码事实 | 影响 |
|---|---|---|
| 对比评估 §4.2 row223：**「本项目引擎抽象已支持双态」**，故推荐免模型 | **不成立**。无引擎级抽象，`*ActReader` 只有 `WfTaskActReader` / `WfApprovalLogActReader`；写侧 100% 依赖 Flowable | 免模型路线成本被**明显低估**，不能拿这句话当论据 |
| 对比评估 §4.4：P2「免模型状态机（**复用现有 node/link 存储**）」 | `application.yml` 中 `definition-from-bpmn.enabled=true`，节点/出口读源是 **BPMN**；`wf-table-retirement.enabled=true` 正在"去 wf_ 表" | "复用现有 node/link 存储"前提已弱化，实例级节点必须**独立建表** |
| 对比评估 §4.2：路线 a `DynamicBpmnService` | `DynamicBpmnService` 只能改**已存在元素**的属性（assignee / 表达式），不能新增 UserTask；新增节点需 `ProcessInstanceModificationBuilder` 或重新部署，对在途实例不友好 | 该路线能力受限，文档亦判定"能力受限" |
| 技术选型 §1157：自由流程是否迁移**待业务确认** | 开关已做进产品且不可用 | 需求未拍板先行做了 UI，形成半成品 |
| 专文治理提示 WF-N9「条件≠权限」 | 现有 `moveActivity` 指定流转同样绕过出口条件 | 落地时必须保留**节点级提交权限**校验 + 可选人/节点白名单 |
| 对比评估 §4.5："约 90% 能力可对齐，自由流程唯一建议降级" | 与本次核查一致 | 该判断**成立**，支持"自由流程延后"的优先级安排 |

**另一处全局风险（推断，需实测确认）**：当前 `instance-read-source=act`、`task-read-source=act`。若自由流程实例不启动引擎，按 act 读源会**查不到实例**（列表 / 详情 / 监控 / 进度图全部隐形）。草稿已有"回退查 `wf_instance`"的兜底先例可复用，但必须为自由流程实例补同类分支。这是免模型路线最易漏的一环。

---

## 5. 三条实现路线

| | **A. 预置节点池 + 指定流转增强**（伪自由流程） | **B. 实例级 node/link + 免模型推进器**（真·E9 对齐） | C. `DynamicBpmnService` |
|---|---|---|---|
| 本质 | BPMN 静态放 N 个通用 UserTask，提交时 `moveActivity` 逐个跳、动态指定办理人 | 每实例建节点/链路行，台账驱动推进，不启引擎 | 运行期改 BPMN 模型 |
| 任意增删节点 | ❌ 有上限 | ✅ | ❌ |
| 分支 / 合并 / 模板 | ❌ | ✅ | ❌ |
| 复用现有配套（监控 / 进度图 / 意见 / 超时 / 强制收回 / 测试态 / ACT 双写 / 归档） | ✅ **全部复用** | ❌ 每项都要加 `isFree` 分支或降级 | ✅ |
| 与 act 读源 / 去 wf_ 表冲突 | 无 | **高** | 无 |
| 改动量 | 小 | 很大（≈ 半套新引擎） | 中但能力受限 |
| 结论 | **推荐 P1** | **P2，需重新评估** | 不推荐 |

### 5.1 路线 A 说明

在 BPMN 中静态放置 `Free_1..Free_N` 通用审批节点（**无需连线**，`moveActivity` 可硬跳），每次提交由办理人选择"下一步节点 + 办理人 + 会签类型"，引擎 token 依次迁移，节点显示名/办理人存实例级数据。节点数上限即"自由"的上限，但覆盖"临时加签/串签/自选审批人"这类**绝大多数真实诉求**。

> **实施前提（第二轮复查补充，见 §10.4）**：预置节点池属于**改 BPMN**。本项目 `definition-from-bpmn.enabled=true`，节点/出口/退回读源锚定 `wf_process_definition.proc_def_id` 指向的**那次部署**；`WfDefinitionServiceImpl:500-503` 只有 `deploy` 才会刷新 `deploymentId/procDefId`，`saveBpmn`（`:542-553, 567-576`）只更新草稿 `bpmn_xml`。因此设计器保存画布后**必须点「发布」**（`POST /definition/{id}/deploy`）才会被 `loadNode` / `WfRejectManager` / `moveActivity` 读到；若同时改了会签多实例，还需 `redeploy-mi`（`WfDefinitionController:121-128`）。**"保存画布 ≠ 生效"**，否则表现为"预置节点不存在 / 指定流转目标节点不存在"。

### 5.2 路线 B 说明

对齐 §2.5 最小数据模型：新增 `wf_free_node(node_key, inst_id, def_id, start_node_key, flow_order, node_name, operator_ids, sign_type, ext_json, version)` 与 `wf_free_link(inst_id, from_key, to_key, is_reject, condition_copy)`；`WfFreeFlowManager` 负责保存/删步骤与复制出口；`WfFreeFlowDriver` 负责推进、结束判定、生成合成待办；`doApprove` 在 L390 之前按 `isFreeInstance(inst)` 分叉。

> ⚠️ **初版遗漏已修正（见 §10.1）**：初稿此处的表定义**缺 `node_key`**，而 springBlade 全链路以 `def_id + node_key` 为索引。动态节点必须有**建节点时一次性生成、写入后永不改变**的稳定 key，建议 `node_key = F_<instId>_<flowOrder>`（`F_` 前缀避免与 BPMN 静态 activityId 冲突，长度 ≤64，内嵌实例与步序可回溯），并把 `node_name/operator_ids/sign_type` 冗余进 `wf_task.ext_json`。补 `def_id`（发起时快照绑定）、`version`（§7.7 并发控制）、`(inst_id, flow_order)` / `(inst_id, node_key)` 唯一键。

**必须同时解决 §7.3 的配套适配清单与 §10 的连锁断点**，否则只是"能提交，不能看/不能管"。

---

## 6. 推荐实施路径

### P0：处理半成品（必做，成本极低）

二选一：

- **① 隐藏/禁用**基础信息表单里的 `isFree` 开关（在真正实现前不给用户错觉）；或
- **② 明确定义为路线 A 入口**，并把 tip 改为"启用后，办理人可在提交时自选下一节点与办理人（可选范围限设计器内预置节点）"，去掉"不允许关闭"的限制。

同时把 `freeWfType` 口径钉死：现有注释对齐的是**新体系** `newFreeWfType`，建议**先只保留简易模式**，高级模式（分支/模板）留到 P2 再启用。

### P1：路线 A —— 交付可用的伪自由流程

| # | 改造点 | 具体位置 |
|---|---|---|
| 1 | 契约扩展：`nextAssignee: Long` → 增 `nextAssigneeIds: List<Long>`（保留旧字段兼容）、`nextNodeName`、`signType` | `ApproveDTO`、`services/workflow/index.ts` |
| 2 | 后端多人化：`overrideAssignee` → 多人，复用已有引擎多实例集合变量 `wfMiAssignees_<nodeKey>` 实现会签 | `WfTaskServiceImpl#doApprove` L390-446、L1290-1311 |
| 3 | 动态节点名：`wf_task.ext_json` 存显示名，供待办 / 进度图 / 流转意见展示 | `WfTask`、`advanceInternal` |
| 4 | 新增 `GET /task/{id}/next-nodes`：返回候选节点（设计期白名单）+ 每节点可选人范围，**必须做权限过滤** | `WfTaskController` |
| 5 | 前端办理页"下一步设置"弹窗（选节点 / 选多人 / 会签类型） | `pages/Workflow/Create/InstanceFlow.tsx` |
| 6 | 设计器把 `appointFlow` 从专家配置项提升为「自由流程模式」一等配置 | `NodeSettingModal.tsx`、`nodeSettings.ts`、`wfDict.ts` |
| 7 | **发布时序**：预置节点池写进 BPMN 后，设计器保存必须再点「发布」刷新 `proc_def_id`；MI 改动补 `redeploy-mi`。实施与验收手册里必须写明（见 §10.4） | `WfDefinitionController` `/{id}/deploy`、`/admin/redeploy-mi` |
| 8 | **会签口径**：`signOrder` 注释写 0-4，运行期只实现 0/1/2；P1 明确只支持「非会签/会签/依次逐个」，若暴露传阅需提交（3/4）须另做门禁，不要按注释误判已支持（见 §10.5） | `WfTaskServiceImpl:1318-1366`、`WfNodeSettingsUtil:727-779` |

**路线 A 下无需改动**：`NodeActionExecutor`（pre/post/出口）、`WfNodeMustDoActionExecutor`、`is_test` 自动化测试、`WfTestServiceImpl` 逐节点自动推进、一键清理、监控列表、进度图、流转意见、超时 Job、强制收回原地回退 —— **前提是预置节点保留静态 key 且已正确发布**（这正是路线 A 相对路线 B 稳的根本原因，见 §10.1 结论）。

### P2：真·自由流程（仅在业务确认需要"任意串节点 / 分支合并 / 模板"时启动）

- 新增两张实例级表（**独立于 BPMN 读源，勿挂靠 `wf_process_node`**）。
- 新增 `WfFreeFlowManager`（保存/删步骤、复制出口）与 `WfFreeFlowDriver`（推进、结束判定、合成待办）。
- `WfTaskServiceImpl#doApprove` L390 前加 `if (isFreeInstance(inst)) return freeFlowDriver.submit(...)`；`advanceInternal` 增加"无引擎实例"模式。
- 逐项完成 §7.3 配套适配清单后再验收。

### 分期总览

| 阶段 | 目标 | 改动面 | 可回滚 |
|---|---|---|---|
| P0 | 消除误导性开关 | 1 个表单配置 | ✅ |
| P1 | 可用的"自选下一步+选人" | DTO + 1 个 service 分支 + 1 个接口 + 前端弹窗 | ✅ |
| P2 | 任意串节点 / 分支 / 模板 | 2 张表 + 2 个新类 + 全链路配套适配 | 需专门设计 |

---

## 7. 风险与护栏

### 7.1 act 读源 / 去 wf_ 表

`application.yml` 中 `task-read-source: act`(L63)、`instance-read-source: act`(L75)、`definition-from-bpmn.enabled: true`(L140)、`wf-table-retirement.enabled: true`(L161)。路线 B 的实例若不启引擎，须为**每一条按 id 读实例/任务的路径**补 `wf_instance` / `wf_task` 回退（草稿已有先例），否则实例在列表与详情中隐形。

### 7.2 条件 ≠ 权限（WF-N9）

指定流转与自由流程都会绕过出口条件求值。**节点级提交权限、可选节点白名单、可选人范围必须独立校验**，不得依赖出口条件做门禁。参照 E9 `workflow_FwLimitSet` / `SelectNextFlowBiz.getNodeScopeSql` / `GetSelectFlowNodesCmd`，以及运行期离职与密级过滤（`RequestFreeNodeFlowBiz:163-167`）。

### 7.3 路线 B 配套适配清单（不可漏）

| 模块 | 现状依赖 | 需处理 |
|---|---|---|
| 实例 / 任务读源 | ACT 表 | 补 act → wf 回退分支 |
| 进度图 | 四色纯 `ACT_HI_*` | 无引擎实例时改用台账渲染 |
| 流转意见 | 真源 `ACT_HI_COMMENT` | 补台账意见写入/读取 |
| 监控列表 / 异常聚合 | `wf_instance` + 引擎对账 | 排除自由流程实例的对账差异，或单列口径 |
| 超时 / 自动处理 | `WfTimeoutJob` 依赖引擎任务 | 改为扫描合成待办 |
| 强制收回原地回退 `forceRecallTo` | `moveActivity` | 无 token 时改为台账回退 |
| 测试态自动推进 | `WfTestServiceImpl` 走真实引擎 | 加自由流程分支 |
| 归档 / `is_test` 清理 | 引擎实例 + 标记 | 动态节点/链路随实例级联清理 |

### 7.4 实例数据清理

E9 有明确先例：`RequestManager:4979-4981` 删除实例时 `delete from workflow_nodelink where ... wfrequestid=requestid` + `delete from workflow_nodebase where IsFreeNode='1' and requestid=requestid`。springBlade 若引入实例级表，**必须同步实现按 `inst_id` 级联删除**，否则形成孤儿数据（现有 `/test/cleanup` 与实例删除逻辑都要覆盖）。

### 7.5 版本与在途实例

`WfDefinitionServiceImpl:1390-1391` 已让新版本继承 `isFree/freeWfType`。但**实例级自由步骤应绑定发起时的定义**，不随版本变更；重新发布定义不得影响在途实例的自由链。此点需在 P2 数据模型上明确（建议 `wf_free_node.def_id` + 发起时快照）。

### 7.6 载体选择：`flowtable` 能否承载自由流程步骤

> 本节回应一个具体架构追问：**能否不新建实例级表，直接把自由流程的运行期步骤放进业务表单表 `formtable_main_{formId}`？**
> 结论：**能"摆"，不能"跑"**——可承载步骤的录入与展示，**不能作为流转真源**。全部结论来自只读代码核查。

#### 7.6.1 术语澄清（三种 `flowtable`）

| 语境 | 实际所指 | 与自由流程的关系 |
|---|---|---|
| E9 `freeFlowTable` | `D:/Weaver2020/ecology/src4js/pc4backstage/odoc/components/createDoc/freeFlowFuntionManager/freeFlowCom.js:92` 的 CSS 类，后台**「权限设置」可编辑表格**（操作列仅"删除"） | **是权限配置表，不是步骤设计器**；名字易误导，勿当成"串节点的表格"照搬 |
| E9 `getWorkflowTable` | `D:/workproject/ecology/src/weaver/workflow/workflow/WfDataSource.java:28 / :345`「工作流程表」数据源，返回 `workflow_formbase` / `workflow_bill` / `Sys_tabledict` 表字典供浏览按钮选表 | 无关 |
| **springBlade 文档里的 flowtable** | `doc/md/归档管理与撤回管理-设计说明.md:41` 定义：`formtable_main_{formId}` = **实例级表单业务数据表**（由 formmode 动态生成，每实例一行字段值） | **本节讨论对象** |

术语依据：`泛微E9-收发文与正文套红功能分析及SpringBlade落地方案.md` §四、`收发文一体化技术方案.md` §1.4。全仓唯一真正的 `flowtable` 字面量是 `WorkflowServiceUtil.java:1782` 的 properties key `MobileShinkFlowTable.workflowids`，与公文无关。

#### 7.6.2 可行部分：录入 / 展示层

明细表 `{主表}_dt{N}` 物理上是「主表 1 行 + N 行、`main_id` 关联」（`DynamicTableServiceImpl.java:76-85`；`FormDataServiceImpl.java:337-442`），`ModeFormGroup.grouptype=2` 提供设计期明细行增删改开关（`blade.sql:10124-10143`）。前端 `ExcelPreview` / Luckysheet 明细区天然可画出「逐行填：步骤名 / 办理人 / 会签类型」的表格——**正是 E9 `FreeWorkflowSet.jsp:146-160` 老设计器的形态**（顺序 / 节点名 / Signtype / 操作者四列）。

明细数据以扁平键进入 `fieldValues`：

```java
// blade-service-api/blade-workflow-api/.../dto/FormSaveDTO.java:46-47
@Schema(description = "表单字段值（主表 + 明细表，key 沿用布局坐标/字段命名）")
private Map<String, Object> fieldValues;
```

发起时整包注入引擎变量：

```java
// blade-service/blade-workflow/.../service/impl/WfInstanceServiceImpl.java:445-451
Map<String, Object> vars = new HashMap<>(16);
if (dto.getFieldValues() != null) { vars.putAll(dto.getFieldValues()); }
if (dto.getVariables() != null)   { vars.putAll(dto.getVariables()); }
```

因此"**发起时一次性把步骤填进明细表 → 驱动前几个节点**"理论上能通。

#### 7.6.3 五条硬障碍（作为真源不可行）

| # | 障碍 | 证据 |
|---|---|---|
| 1 | **主表列设计期固定、表级共享**：列由 `workflow_billfield.fielddbname` 驱动、`ALTER TABLE` 加列（`FieldDefinitionServiceImpl.createFieldWithColumn`）；`syncTableStructure` 在有数据时直接拒绝重建（`:175-182`）。为"步骤1人/步骤2人"加列会污染该表单**所有实例** | `DynamicTableServiceImpl.java:51-67, 94-99, 175-182`；`blade.sql:5773-5862`（`formtable_main_10` 仅 `PRIMARY KEY(id)`，无步骤/序号/办理人/会签列） |
| 2 | **明细表在流程链路上写不进去**：流程发起/办理保存只走 `FormDataSaveDTO`（仅 `fieldValues`）；支持明细的 `FormDataDTO.detailTables` 只存在于 `POST /form/data/save`，且要求 `modeid` 模块存在 | `FormDataSaveDTO.java:47-48` vs `FormDataDTO.java:39-43`；`FormDataController.java:95-100, 116-121`；`WfFormRenderServiceImpl.java:350-354` |
| 3 | **无稳定顺序**：明细表无 `flow_order` 列，读取 `SELECT * ... WHERE main_id=?` **无 `ORDER BY`**，只靠 `List` 遍历 + 雪花 id；`saveDetailData` 是「先 `DELETE` 后逐行 `INSERT`」 | `FormDataServiceImpl.java:379, 385-390, 783`；`blade.sql:6058-6064` |
| 4 | **引擎不读业务表**：网关求值与流转只看 Flowable process variables；`WfTaskServiceImpl` 全文 **0 处**调用 `IFormmodeClient`；办理态提交只 `vars.putAll(dto.getVariables())`，**不读 `fieldValues`** | `ProcessServiceImpl.java:53-55, 87-90, 144-145`；`WfTaskServiceImpl.java:321-325` |
| 5 | **生命周期 + 权限 / 审计缺失**：`deleteBusinessData` 只 `DELETE FROM 主表`、不删任何 `_dt{N}`（草稿/测试删除后必留孤儿）；`saveBusinessData` **零权限裁剪**、端点只需 `@PreAuth(HAS_AUTH)`（已登录即可）；全模块无 `FOR UPDATE` / `@Version` / 历史行，审计仅 `modedata*` 最后修改列；`saveDetailData` 单表异常还被 catch 吞掉不回滚 | `FormDataServiceImpl.java:437-440, 711-734, 827-954`；`FormDataController.java:116-121`；`DynamicTableServiceImpl.java:56-61` |

> 第 5 条与 §7.2 **WF-N9「条件 ≠ 权限」直接冲突**：把「办理人 ID 列表 / 会签类型」放进业务表列，任何已登录用户构造 `formId + dataId + fieldValues` 即可改写流程走向，且改前值无留痕。

补充：`formtable_main_10`（发文表单）在 `blade.sql` 中**没有任何 `_dt` 明细表**（全库 `_dt` 表只有 1/2/3/5/9 系列），因此公文场景连"复用现成明细表"都要先建表。

#### 7.6.4 易误判的坑：扁平键并没有落进 flowtable

把步骤以 `dt0__r0__operator` 这类扁平键塞进 `fieldValues`，看起来"数据进去了"，但 `WfFormRenderServiceImpl.save`（`:344-366`）是两条独立写入：

```java
// ① 写业务数据行（跨服务 formmode）
saveDto.setFieldValues(dto.getFieldValues() == null ? Map.of() : dto.getFieldValues());
R<Long> r = formmodeClient.saveBusinessData(saveDto);
// ② 办理态：同步当前节点快照（不改任务状态、不推进引擎）
if (dto.getInstanceId() != null) { ... upsertSnapshot(dto.getInstanceId(), nodeKey, dto.getFieldValues()); }
```

- `saveBusinessData` **按真实列名匹配，不存在的列静默忽略**（`FormDataServiceImpl.java:892-909`）→ 扁平键**根本没落进 `formtable_main_*` 业务行**；
- `upsertSnapshot` 才把整包写进 `wf_form_snapshot.data_json`（按 `(instId,nodeKey)` 覆盖写，无版本/历史）。

即：这**不是"在 flowtable 上实现"**，而是**"借表单提交通道旁路注入快照与发起时的引擎变量"**。且办理态再提交时 `doApprove` 不读 `fieldValues`，**运行期增删步骤不会同步到 variables**，动态性当场失效。

> 另注：`getBusinessData`（`getFormDataById`）只返回主表 `fieldValues`，`FormDataVO.detailTables` 是**死字段**从未 set；读明细须另调 `GET /form-data/by-form/detail`（`FormDataController:260-265`）。

#### 7.6.5 三种载体对比

| | `formtable_main_*` 主表列 | `_dt{N}` 明细表行 | 新建 `wf_free_node` 实例级表 |
|---|---|---|---|
| N 行变长步骤 | ❌ 列固定 | ✅ 物理支持 | ✅ |
| 稳定顺序 | ❌ | ❌ 无 ORDER BY | ✅ `flow_order` |
| 参与流转决策 | ❌ | ❌ 引擎不读 | ✅ `doApprove` 分叉消费 |
| 运行期增删 | ❌ | 🟡 需绕 `/form/data/save` | ✅ |
| 实例隔离 | 🟡 `data_id` 弱逻辑隔离（主表无 `request_id` 唯一索引） | 🟡 仅 `main_id`，无 `inst_id` | ✅ 强 |
| 权限 / 审计 | ❌ 写入侧无裁剪、无版本 | ❌ | ✅ 可自建 |
| 实例删除清理 | 🟡 只删主表 | ❌ 留孤儿行 | ✅ 级联 |
| 改动量 | 小 | 中 | 中（但闭环） |

#### 7.6.6 建议用法

**可复用 flowtable 做「步骤录入 / 展示 / 归档留痕」，但执行真源必须独立**：

```text
前端明细表 UI（对齐 E9 FreeWorkflowSet 形态）
        │ 提交时一份数据两写
        ├─→ formtable 明细 / wf_form_snapshot   ← 给人看、归档留痕（可读性）
        └─→ wf_free_node(inst_id, flow_order, node_name,
                        operator_ids, sign_type, ext_json)   ← 给引擎用（唯一真源）
                              │
                              ▼
        WfTaskServiceImpl#doApprove 分叉读取
        → 决定 nextNodeKey / overrideAssignees / 结束判定
```

**但 P1 路线 A 完全不必碰 flowtable**：「预置节点池 + 指定流转增强」里"下一步设置"弹窗的数据直接进 `ApproveDTO`、落 `wf_task.ext_json`，走现有 `moveActivity` + `advance`，不依赖业务表、不依赖快照、不引入两写一致性问题。只有 P2 真·自由流程才需要实例级表，届时 flowtable 也只承担 UI 层。

> **一句话**：flowtable 能当「自由流程的步骤表格界面」，不能当「自由流程的引擎」——它是**单据数据表，不是流程状态表**；把流程状态塞进去，会同时丢掉顺序、权限、审计与实例生命周期，而且引擎压根不会去读它。

### 7.7 并发与幂等（第二轮复查新增）

E9 侧：`WFFreeFlowManager.java:109-124` 用 `private static Object syncObject` + `synchronized` 只保护「取新节点 id」（`getNodeNewId` / `getNodeNewIdBase` 的 `insert` + `select max(id)`），`CreateNewGroup` 同样同步（L167-168）；而 `SaveFreeFlow` 的「删旧链 + 重建」（L269-277 先删 `workflow_flownode` / `workflow_nodelink` / 按 `id not in (getExistsFreeNodeIds(params))` 删节点，L396-405 再逐步建）**不是事务级互斥**。两人同时编辑同一实例时，A 的 `id not in (A 的参数)` 会把 B 刚插的节点删掉、A 已删的链路让 B 断链；`select max(id)` 在多 JVM 集群下也不是全局唯一。源码未见实例级版本号或行锁（此为源码推断，未做并发实测）。

springBlade 侧：目前无实例级自由步骤表，故既无并发问题也无并发控制；全模块 `FOR UPDATE` / `@Version` **0 命中**。可复用先例只有 `WfDefinitionServiceImpl:567-576, 649-660` 的 `draftRevision` 乐观锁（`BpmnSaveDTO.baseRevision` 条件递增）与 `WfTimeoutJob:73-98` 的 MySQL `GET_LOCK/RELEASE_LOCK` 选主。

**若走路线 B，必须补 5 项**（雪花 id 只保证主键不冲突，防不了"后写覆盖前写 / 删掉别人刚插的步骤"）：

| # | 控制 | 建议 |
|---|---|---|
| 1 | 实例级乐观锁 | `wf_free_node.version`，`UPDATE ... WHERE inst_id=? AND version=?`；或 `SELECT ... FOR UPDATE` 锁 `wf_instance` 单行作互斥点 |
| 2 | 整包替换单事务 | 「删旧 + 建新 + 复制出口」放同一 `@Transactional`，避免中间态被另一请求读到 |
| 3 | 唯一键 | `(inst_id, flow_order)` 与 `(inst_id, node_key)` 防重，配合 `node_key` 稳定生成（§10.1） |
| 4 | 幂等键 | 保存请求带 saveToken / requestId，防重复提交与页面重放 |
| 5 | 审计与清理 | 变更历史列 + 按 `inst_id` 级联删除（§7.4）；`modedata*` 式"最后修改列"不足以审计 |

---

## 8. 待决事项（需业务 / 架构拍板）

1. **对齐哪套 E9 机制**：老自由流程（纯串联、逐实例动态节点）还是新自由节点（负 id、分支/合并/模板）？决定 `freeWfType=2` 的真实含义与工作量。
2. **是否真的需要"任意增删节点"**：若真实诉求只是"临时自选审批人/加签/串签"，路线 A 即可覆盖，不必付路线 B 的成本。
3. **`isFree` 开关 P0 处置**：隐藏还是重新定义为路线 A 入口？
4. **节点数上限**（路线 A）：预置多少个通用节点，是否允许运行时动态扩展 BPMN（会引入重部署）。
5. **可选人范围口径**：全局 HR、按部门/角色、还是按节点白名单？是否复用现有 26 种操作者解析器 `WfNodeOperator`。
6. **免模型与"去 wf_ 表"路线是否冲突**：若团队坚定推进去 wf_ 表 / act 读源，路线 B 需要架构层决策，不应作为功能开发顺带完成。
7. **`flowtable` 的定位**（见 §7.6）：P2 时是否让业务表单明细表承担"步骤录入 / 展示 / 归档留痕"（需接受两写一致性），还是步骤数据完全不进业务表、只落实例级表？P1 路线 A 建议**完全不碰 flowtable**。
8. **公文侧自由流程六权限是否纳入范围**（见 §10.6）：`odoc_freeflow_manager` 的改签/代录意见/收回/改办理人/归档/调整日志，属公文主线能力，springBlade odoc 目录**无表无端点**。是随 P2 一并做、单列一期，还是明确不做？
9. **设置界面形态**（见 §3.6）：P1 只做办理页「下一步设置」弹窗，还是 P2 直接上 SVG 步骤编排器（含分支/合并/模板）？需结合第 1、2 项一并拍板。
10. **周边能力裁剪**（见 §10.5）：传阅需提交（signOrder 3/4）、超时循环提醒、自由步骤模板、代理、移动端办理页——哪些是本项目必须，哪些明确裁剪？建议 P1 全部裁剪并在产品文档写明。

---

## 9. 证据索引

### 9.1 E9 源码

| 结论 | 文件 | 行 |
|---|---|---|
| 动态建节点（表结构） | `D:\workproject\ecology\src\weaver\workflow\request\WFFreeFlowManager.java` | 124-141 |
| 每步骤建节点 | 同上 | 396-405 |
| 实例归属 / 顺序 / 直存人 | 同上 | 455-456 |
| 按实例清理旧节点 | 同上 | 275 |
| 建链 `insert workflow_nodelink` | 同上 | 698-703 |
| 复制原节点出口（含条件） | 同上 | 817-918 |
| 读已定义步骤 `getFreeWFStep` | 同上 | 84-87 |
| 兄弟节点批量套用 | 同上 | 1039-1099 |
| 自由流程权限 `workflow_freeright` | 同上 | 429-440, 1827-1838 |
| 命中动态节点 → `FreeWorkflowNextoperator()` | `D:\workproject\ecology\src\weaver\workflow\request\RequestNodeFlow.java` | 1044-1061 |
| `FreeWorkflowNextoperator()` 按 operators 生成操作者 | 同上 | 3690-3694, 3736 |
| 指定流转装入所选 link | 同上 | 1832-1861 |
| **`if (isSelectNextFlow) break;`** | 同上 | 1891-1894 |
| 固定节点段恢复条件求值 | 同上 | 1184-1188 |
| 新自由节点 `getNextNodeOperators` | `D:\workproject\ecology\src\com\engine\workflow\biz\freeNode\RequestFreeNodeFlowBiz.java` | 145-151, 1019-1025 |
| 负 id 判定 / `isFreeFlow` | `D:\workproject\ecology\src\com\engine\workflow\biz\freeNode\FreeNodeBiz.java` | 72-75, 1241-1249 |
| `SignOrder.CC_*` 传阅常量 | `D:\workproject\ecology\src\com\engine\workflow\constant\node\SignOrder.java` | 23-27 |
| 实例可见节点过滤 SQL | `D:\workproject\ecology\src\weaver\workflow\request\WFPathUtil.java` | 78 |
| 实例清理动态节点/链路 | `D:\workproject\ecology\src\weaver\workflow\request\RequestManager.java` | 4979-4981 |
| 统一构造 `RequestNodeFlow`（无 isfree 大分支） | 同上 | 3199-3239 |
| 定义态节点缓存排除 `isfreenode` | `D:\workproject\ecology\src\weaver\workflow\workflow\WorkflowNodeComInfo.java` | 31 |
| 选路范围控制 | `D:\workproject\ecology\src\com\engine\workflow\cmd\requestForm\GetSelectFlowNodesCmd.java` | 94-98, 130-132 |
| 老设计器表单（顺序/节点名/会签/操作者） | `D:\Weaver2020\ecology\workflow\request\FreeWorkflowSet.jsp` | 146-160 |
| 新自由流程设计器入口 | `D:\Weaver2020\ecology\workflow\workflowDesign\freeNodeFlowSet.html` | 79-105 |
| `workflow_nodebase` DDL | `D:\Weaver2020\ecology\data\ST\1table.sql` | 22180-22201 |

### 9.2 springBlade 源码

| 结论 | 文件（相对 `springBlade/`） | 行 |
|---|---|---|
| `isFree/freeWfType` 实体 | `blade-service-api/blade-workflow-api/.../entity/WfProcessDefinition.java` | 105-109 |
| `is_free` DDL | `doc/sql/blade/blade.sql` | 12490-12502 |
| 基础信息表单开关 | `blade-service/blade-workflow/.../service/impl/WfDefinitionServiceImpl.java` | 2673-2691 |
| 版本复制继承 | 同上 | 1390-1391 |
| 节点 / 连线 / 操作者实体 | `blade-service-api/blade-workflow-api/.../entity/WfProcessNode.java`、`WfNodeLink.java`、`WfNodeOperator.java` | — |
| `wf_node_link` DDL | `doc/sql/blade/blade.sql` | 11023-11043 |
| `ApproveDTO` 字段 | `blade-service-api/blade-workflow-api/.../dto/ApproveDTO.java` | 27, 30 |
| **指定流转三模式主逻辑** | `blade-service/blade-workflow/.../service/impl/WfTaskServiceImpl.java` | 390-446 |
| 目标节点必须存在（卡点） | 同上 | 410, 439 |
| `appointFlow*` 解析 | `.../utils/WfNodeSettingsUtil.java` | 633-657 |
| 引擎跳转 `moveActivity*` | `.../service/impl/ProcessServiceImpl.java` | 314-354 |
| 台账推进 / 生成 | `.../service/impl/WfInstanceServiceImpl.java` | 2944-2973, 2984, 2991, 3050-3133 |
| 草稿合成待办（空 engineTaskId） | 同上 | 732-747 |
| 加签 / 转发 / 传阅只造 wf_task 行 | `WfTaskServiceImpl.java` | 673, 713, 745 |
| 引擎多实例集合变量 | 同上 | 1290-1311 |
| 读源开关 | `blade-service/blade-workflow/src/main/resources/application.yml` | 63, 75, 140, 161 |
| act 读源兜底 / 跳过双写 | `.../service/helper/WfTaskActReader.java`、`WfTaskActWriter.java` | 206, 243-250 / 98-101 |
| 动作扩展点 | `.../action/NodeActionExecutor.java` | 36-42, 226-249 |
| 公文必做动作 | `.../odoc/action/WfNodeMustDoActionExecutor.java` | 60-77 |
| 测试态 | `.../service/impl/WfTestServiceImpl.java`、`WfTestController.java`、`WfDefinitionServiceImpl#deployForTest` | — |
| 前端设计器 / 节点配置 | `ant-design-pro/src/pages/FormMode/WorkflowDesign/`（`nodeSettings.ts`、`NodeSettingModal.tsx`、`wfDict.ts`） | — |
| 前端办理页（无选路 UI） | `ant-design-pro/src/pages/Workflow/Create/InstanceFlow.tsx` | 635, 691, 1261 |
| **§7.6 flowtable 承载能力** | 见下表 | — |

**§7.6 补充证据（formmode 数据层）**

| 结论 | 文件（相对 `springBlade/`） | 行 |
|---|---|---|
| 主表 / 明细表 / 历史表命名规则 | `blade-service/blade-formmode/.../utils/TableNameUtil.java` | 16-42 |
| 主表 DDL（`id`/`request_id`/`modedata*`） | `.../service/impl/DynamicTableServiceImpl.java` | 51-67, 313-328 |
| 明细表 DDL（`main_id` + 索引，无外键） | 同上 | 76-85, 329-333, 374-377 |
| 有数据时拒绝重建表 | 同上（`syncTableStructure`） | 138-304, 175-182 |
| 设计期加列 `ALTER TABLE ADD COLUMN` | 同上 / `.../service/impl/FieldDefinitionServiceImpl.java` | 94-99 / 124-165 |
| 主表 / 明细分表依据 `isMain` / `detailTable` | `.../service/impl/DynamicTableServiceImpl.java` | 197-253 |
| `FormDataDTO.detailTables`（唯一明细写入口） | `blade-service-api/blade-formmode-api/.../dto/FormDataDTO.java` | 39-43 |
| `FormDataSaveDTO` 仅 `fieldValues`（流程链路） | 同目录 `FormDataSaveDTO.java` | 47-48 |
| 四前缀映射 / `save` / `saveByForm` / 明细读端点 | `blade-service/blade-formmode/.../controller/FormDataController.java` | 39, 95-100, 116-121, 242-265 |
| `saveFormData` 第 283 行才写明细 | `.../service/impl/FormDataServiceImpl.java` | 61-286, 283 |
| `saveDetailData` 先删后插、异常被吞 | 同上 | 337-442（379, 385-390, 437-440） |
| `saveBusinessData` 按列名匹配、无权限裁剪 | 同上 | 827-954（842-849, 892-920, 940-950） |
| `getFormDataById` 只返回主表、`detailTables` 死字段 | 同上 / `FormDataVO.java` | 549-599 / 36-37 |
| 明细读取无 `ORDER BY` | 同上 | 737-790（783） |
| `deleteBusinessData` 只删主表 | 同上 | 711-734（731） |
| `resolveMainTableName` / `workflow_bill.table_name` | 同上 | 508-546, 831-840 |
| 模块级字段权限（fail-open） | `.../service/impl/FormRightServiceImpl.java` | 274-313 |
| 节点级字段权限只用于渲染/必填 | `blade-service/blade-workflow/.../WfFormRenderServiceImpl.java` | 153-155, 273-275, 498-536 |
| `FormSaveDTO.fieldValues` 含明细扁平键 | `blade-service-api/blade-workflow-api/.../dto/FormSaveDTO.java` | 46-47 |
| 发起时 fieldValues 注入引擎变量 | `.../service/impl/WfInstanceServiceImpl.java` | 445-451, 522-524 |
| 保存时两写（业务行 + 快照） | `.../service/impl/WfFormRenderServiceImpl.java` | 340-366 |
| 快照实体 `data_json`「主表+明细全量值」 | `blade-service-api/blade-workflow-api/.../entity/WfFormSnapshot.java`；`blade.sql` | 20-37 / 10580-10597 |
| 引擎只读 process variables | `.../service/impl/ProcessServiceImpl.java`；`WfTaskServiceImpl.java` | 53-55, 87-90, 144-145 / 321-325 |
| `formtable_main_10` DDL（无步骤列、无 `_dt` 表） | `doc/sql/blade/blade.sql` | 5773-5862 |
| 明细表 DDL 样例（无序号/实例列） | 同上 | 6058-6064, 6333-6340 |
| `wf_instance.data_id` / `uk_biz_key` | 同上 | 10617-10648 |
| 迁移说明「仅主表带 request_id，明细用 main_id」 | `doc/sql/migration/V2026.09.20_004__formtable_request_id_bigint.sql` | 21-25 |
| E9 `freeFlowTable` CSS 类 / 「工作流程表」数据源 | `D:/Weaver2020/ecology/src4js/.../freeFlowCom.js`；`D:/workproject/ecology/src/.../WfDataSource.java` | 92 / 28, 345 |

### 9.3 `doc/md` 既有分析

| 文档 | 关键位置 |
|---|---|
| `泛微E9-流程自由流程(路由复用)分析.md` | §1 结论、§3 定义层、§4 运行时、§5 固定节点段、§7 WF-N9、§8 证据索引 |
| `E9与Flowable7流程节点功能对比及改造可行性评估.md` | §1.2 表模型、§3.2 差异表 L165/183、§4.2 row223、§4.4 L242、§4.5 L249 |
| `审批流程方案-现状分析与技术选型.md` | §2.3 表结构 L98/L324/L343-352/L405-412、§2.7.9 L538、§待确认 L1157 |
| `泛微流程节点信息设置项分析.md` | L388, L676-698, L770, L974 |
| `泛微流程操作菜单与退回机制分析.md` | L71, L155-157, L423 |
| `泛微E9-跨模块风险登记册.md` | WF-P1-2（L81） |
| `泛微E9-收发文与正文套红功能分析及SpringBlade落地方案.md` | §四 `flowtable` 术语澄清（L232-268）、结论 3（L655） |
| `收发文一体化技术方案.md` | §1.4 `flowtable` 术语澄清（L213-226）、`freeFlowTable` 权限表格（L219） |
| `归档管理与撤回管理-设计说明.md` | L41「flowtable = 实例级表单业务数据表」定义、§七 flowtable 回退可行性核查（L147-166） |

### 9.4 第二轮复查补证（§3.6 界面分层 / §10 遗漏项）

| 主题 | 文件 | 行 |
|---|---|---|
| 定义期开关（老） | `D:/Weaver2020/ecology/workflow/workflow/addwf0.jsp`、`addwf0_script.jsp`、`wf_operation.jsp` | 943-956 / 369-372 / 357,367,483,492,936 |
| 定义期开关（新） | `D:/Weaver2020/ecology/src4js/.../pathSet/mainFrame/AddCondition.js`、`baseSet/BaseInfoCondition.js`；`D:/workproject/ecology/src/.../cmd/workflowPath/baseInfo/GetBaseInfoConditionCmd.java`、`DoAddWFCmd.java`、`DoEditWFCmd.java` | 64-90 / 100-121 / 326-347 / 105,172-173 / 196,322-327 |
| 运行期「流转设定」入口 | `D:/Weaver2020/ecology/workflow/request/ManageRequestNoFormIframe.jsp`、`AddRequestIframe.jsp`；`D:/workproject/ecology/src/.../cmd/requestForm/GetRightMenuCmd.java` | 1787-1793, 3225-3267 / 581-587, 1827-1863 / 1777-1780, 2376-2379 |
| SPA 弹窗 | `D:/Weaver2020/ecology/src4js/pc4mobx/workflowForm/util/rightBtn.js`、`.../workflow/workflowDesign/readOnly-index.html`、`freeNodeFlowSet.html` | 680-740 / 78 / 79-105 |
| 表格 / 图形 / 简易三代设计器 | `D:/Weaver2020/ecology/workflow/request/FreeWorkflowSet.jsp`、`FreeNodeShow.jsp`、`FreeNodeWin.jsp`、`FreeNodeShowSimple.jsp` | 74-198, 34-38 / 255-283, 590-610 / 28-60 / 8-26, 69-88 |
| 新 SVG 三大块 | `D:/Weaver2020/ecology/src4js/.../workflowFree/WorkflowFreeContainer.js`、`WfFreeActionTop.js`、`WfFreeRightDetail.js`、`WorkflowFreeNode_v2.js`；`GetNodeSettingCmd.java` | 57-67 / 86-93, 98-125 / 298-419 / 167-220 / 55-135 |
| 编辑权与后台权限表 | `D:/workproject/ecology/src/.../biz/freeNode/LoadFreeNodeBiz.java`、`.../odoc/util/updateSign/OdocFreeFlowUtil.java`、`.../odoc/constant/FreeFlowBtnEnum.java`；`D:/Weaver2020/ecology/data/ST/sql202005271203.sql` | 116-151 / 41-88 / 5-12 / 1-13 |
| 模板 / 共享 Cmd | `D:/workproject/ecology/src/.../cmd/freeNode/SaveTemplateCmd.java`、`ImportFromTemplateCmd.java`、`GetTemplateListCmd.java`、`DelTemplateCmd.java` | 33-69 / 40-60 / 54-58 / 40-61 |
| 移动端 | `D:/workproject/ecology/src/weaver/mobile/webservices/workflow/WorkflowServiceImpl.java` | 3135-3165, 4063-4075, 7557-7630 |
| **springBlade：nodeKey 全链路索引** | `WfTask.java` / `WfTaskVO.java` / `WfFormSnapshot.java` / `WfNodeFieldPerm.java` / `WfNodeTimeout.java` / `WfNodeOperator.java` | 51-52 / 54-58 / 26-36 / 40-69 / 32-36 / 27-32 |
| 快照空 key 静默丢弃 / 覆盖 | `WfFormRenderServiceImpl.java` | 207-210, 402-425（404-406 return） |
| 节点名回查 / 操作菜单兜底 | `WfTaskServiceImpl.java:996-1028, 256-268, 1082-1089`；`WfNodeSettingsUtil.java:206-220` | — |
| 公文四钩子（node==null 静默 return） | `WfNodeMustDoActionExecutor.java:60-77, 79-93, 105-130`；`WfOdocNumberAssignListener.java:42-87`；`WfTaskServiceImpl.java:340,354,362,369` | — |
| 退回 BFS / 意见 / 进度图 | `WfRejectManager.java:83-152`；`WfInstanceServiceImpl.java:1181-1333, 3837-3890` | — |
| 超时 / MI 变量硬编码 | `WfTimeoutJob.java:136-163, 189-230`；`WfTimeoutServiceImpl.java:74-95`；`WfDefinitionServiceImpl.java:2348-2400` | — |
| 发布时序（保存≠生效） | `WfDefinitionServiceImpl.java:500-503, 542-553, 567-576`；`WfDefinitionController.java:110-113, 121-128`；`application.yml:132-140` | — |
| 并发先例（乐观锁 / 选主锁） | `WfDefinitionServiceImpl.java:567-576, 649-660`；`WfTimeoutJob.java:73-98`；E9 `WFFreeFlowManager.java:109-124, 269-277` | — |

---

## 10. 遗漏项复查（第二轮，2026-11-04）

> **复查方法**：在 §1-§9 基础上，再派两轮只读源码核查，分别针对「**动态节点 key 的连锁影响**」与「**E9 自由流程周边能力 vs springBlade 覆盖**」，逐项比对 E9 实现、springBlade 对应物与本文档已有覆盖度。
> **总结论**：初版覆盖了「机制本身」（动态建节点、路由复用、flowtable 载体、三条路线），但**低估了运行期动态节点对全链路的连锁影响**，并遗漏了 8 类周边能力。其中 **10.1 是最严重的一项，且是初版方案自身的设计缺陷**。

### 10.1 【最严重】`node_key` 缺口及其连锁断点

**问题**：初版 §5.2 给出的 `wf_free_node(inst_id, start_node_key, flow_order, node_name, operator_ids, sign_type, ext_json)` **没有 `node_key` 列**（已在本次修订中补上）。而 springBlade 全链路以 `def_id + node_key` 为索引：`wf_task.node_key`、`wf_form_snapshot(inst_id,node_key)`、`wf_node_field_perm` 唯一键 `(def_id,node_key,scope,field_name)`、`wf_node_timeout(def_id,node_key)`、`wfMiAssignees_<nodeKey>`（发布期硬编码进 BPMN，`WfDefinitionServiceImpl:2394`）、ACT `TASK_DEF_KEY_`。全库 `node_key IN` / `JOIN wf_process_node` **0 命中**——都是 Java 侧 `loadNode(defId, nodeKey)` 逐条回查。

**动态节点无稳定 key 的连锁后果**：

| 链路 | 后果 | 证据 |
|---|---|---|
| **公文四钩子** ⚠️ | `loadNode` 返回 null 时 `checkBeforeSubmit` / `onNodeSubmit` **首行直接 return** → **套红、签章、密级必做校验与定稿取号全部静默跳过（不报错、无文号、无版式）**；`process_set` 存的 `taohongNodeKey/finalizeNodeKey` 是设计期静态 key，动态 key 永不命中 | `WfNodeMustDoActionExecutor:61-63,85,109`；`WfOdocNumberAssignListener:43-45,57`；`WfTaskServiceImpl:340,354,362,369` |
| **字段权限 / 操作菜单** ⚠️ | `getFieldPerm` 空 → 前端 `nodePermission` 恒 undefined → **全字段可编辑/不隐藏/不必填**；`operateMenus(null)=null` → `allowMenusUnset=true` → **显示系统默认全量按钮**且后端 `requireOperate` 放行。**"缺配置"被统一解释成"全放行"，比报错更危险** | `WfPermServiceImpl:83-86`；`WfFormRenderServiceImpl:153-155`；`WfNodeSettingsUtil:206-220`；`InstanceFlow.tsx:762-764` |
| **表单快照** | 空 key → `upsertSnapshot:404-406` **直接 return，每步表单数据都不留痕**；若统一退回 `start_node_key` → 每步覆盖同一行，**逐节点历史回溯全丢**；而 `render` 按 instId 取最新一条，表面"能存能读"，只在历史查看/打印时暴露，极易漏测 | `WfFormRenderServiceImpl:207-210, 402-425` |
| 待办 / 列表 | 不按 key 过滤故仍能列出，但 `toVO:1022` `loadNode` 返回 null → **节点名空白**；前端 `rowKey = instId-nodeKey` 退化为 `instId-undefined`，同实例多条动态待办 **React key 冲突** | `WfTaskServiceImpl:996-1028`；`Todo.tsx:182` |
| 进度图 / 意见 / 退回 | 进度图按 BPMN activityId 四色 → 动态节点**不上色**；`nextHandlerIds` 靠静态 `fromTo` → **接收人恒空**；退回 BFS 以静态入边为准 → 空集合 → **抛"当前节点没有可退回的节点"** | `WfInstanceServiceImpl:1181-1333, 3837-3890`；`WfRejectManager:91-129` |
| 超时 / 会签 | `listEnabled(defId,nodeKey)` 空 → `:207-209` 直接标记已处理，**超时动作永不触发**；`wfMiAssignees_<nodeKey>` 在 BPMN 里无对应 `multiInstanceLoopCharacteristics`，注入变量**无人消费** | `WfTimeoutJob:189-230`；`WfTimeoutServiceImpl:74-95`；`WfDefinitionServiceImpl:2394` |
| 前后置 / 出口动作 | `NodeActionExecutor` 与 `WfNodeSettingsUtil.*` 的形参都是 `WfProcessNode`，动态节点不在静态表 → node=null → pre/post/出口动作全跳过、`appointFlow mode=0`、签字意见/表单日志/打印全默认；`executeLink` 查不到 `wf_node_link` 行直接 return | `NodeActionExecutor:92-119, 226-249`；`WfNodeSettingsUtil:54-59, 634-657` |

**建议的 key 规则**（已写入 §5.2）：`node_key = F_<instId>_<flowOrder>`，由 `inst_id + start_node_key + flow_order` 拼出、**插入后永不改变**；`F_` 前缀避免与 BPMN 静态 activityId 冲突（`WfTaskServiceImpl:439` 指定流转要求 `loadNode(defId,nextNodeKey)!=null`，同名会误命中）；长度 ≤64（`blade.sql:10583`）；不要用 UUID/时间戳（全库 workflow 内无 key 生成器，且历史索引不可重算）。

**对两条路线的影响（重要）**：
- **P1 路线 A 基本不受影响** —— 预置 `Free_1..Free_N` 是 BPMN 静态 UserTask，天然有真实 activityId / 静态 key，**这正是它稳健的根本原因**，也再次说明 §7.3 那张配套清单对 P1 不适用。
- **P2 路线 B 必须整体重做** —— 仅补 `node_key` 还不够，还得把公文取号点从"按静态 key"改成实例级角色标记（如 `wf_free_node.is_finalize / must_taohong` 或 flow_order 最后一步），并为字段权限、操作菜单、超时、意见、进度图、退回逐项加自由节点上下文或影子 `WfProcessNode`。**"能提交，不能看/不能管"的完整成因即在此**。

### 10.2 退回 / 强制收回在无静态出口下失效

E9：简易模式 `FreeWorkflowSimple.getRejectToNodeId/getRejectUserId`（`:545-675`，`signtype=2` 时退给**前一个操作者**、还原被代理人 `:644-646`）；自由节点 `RequestFreeNodeFlowBiz.getFreeRejectNodeids` / `getRejectRangeNodes`（`:707-710, 1109-1182`）；`FreeNodeEntity.isselectrejectnode / isSubmitDirectNode`（`:88-91`）。

springBlade：`WfRejectManager:91-121` 完全靠 `WfNodeLink.toNodeKey` **反向入边 BFS**，无入边即 `order` 空 → `:127-129` 返回空集合 → `WfTaskServiceImpl:516-520` 抛「当前节点没有可退回的节点」；目标还必须在候选内（`:536-537`）；`forceRecallTo:2219-2220` 要求引擎运行态、`:2236-2239` 目标必须是定义中真实 UserTask。**「按 operators 顺序退给前一人」语义全库无对应**。
（已有可复用物：`RejectDTO.targetNodeKey/remindNodeKeys/resubmitMode`，且 `resubmitMode` 有运行期消费 `WfTaskServiceImpl:557-563`、`WfInstanceServiceImpl:2758`、`WfMonitorController:169-176`，对齐 E9 `isSubmitDirectNode`。）

### 10.3 并发与幂等

详见新增 **§7.7**。要点：E9 `synchronized` 只保护取 id，`select max(id)` 集群不唯一，删旧建新非事务互斥；springBlade 全模块 `FOR UPDATE`/`@Version` 0 命中，路线 B 需补乐观锁 / 单事务整包替换 / 唯一键 / 幂等键 / 审计清理 5 项。

### 10.4 发布 / 部署时序是 P1 的硬前提

`application.yml:132-140` `definition-from-bpmn.enabled=true`；`WfDefinitionServiceImpl:500-503` **只有 `deploy`** 才刷新 `deploymentId/procDefId`，`saveBpmn:542-553, 567-576` 只更新草稿 `bpmn_xml`（注释明写「保存只更新草稿、不刷新部署」）。预置节点池属于改 BPMN → 保存后**必须发布**（`POST /definition/{id}/deploy`），MI 改动还需 `redeploy-mi`（`WfDefinitionController:121-128`）。否则表现为"预置节点不存在 / 指定流转目标节点不存在"。已补入 §5.1 与 §6 P1 改造点 7。

### 10.5 周边能力落差清单

| # | 能力 | E9 | springBlade | 初版覆盖 |
|---|---|---|---|---|
| 1 | **会签 5 值** | `SignOrder` 0/1/2/3/4 + `nodetype=4 CIRCULATE` | `signOrder` 注释写 0-4，**运行期只实现 0/1/2**（`resolveSignOrder:1362-1365` 把 3/4 落到或签）；`circulateTask` 只插一条 `STATUS_CIRCULATE` 行、无提交门禁。**「依次逐个」已具备，勿误判为缺** | 仅一笔带过 |
| 2 | **超时字段** | `advanceRemind`、`before/afterLoopRemind`、`*RemindInterval`、`overtimeToNextOperator`、`overflowtimetype` 多形态，且是**实例级节点行** | 有 `WfNodeTimeout`（durationMin/actionWay/targetNodeKey/remindTypes）与 `WfTimeoutJob`（扫 ACT_RU_TASK、`GET_LOCK` 选主、`timeout_handled` 幂等），但缺上述提前/循环提醒与自动流转下一人；规则是**定义期静态** `wf_node_timeout(def_id,node_key)` | 仅一笔带过 |
| 3 | **路径编辑权** | `workflow_freeright.isroutedit/istableedit`（`LoadParamCmd:1150-1157` 把 `istableedit=0` 转成 `isviewonly=1`）、`nextNodeEditPurview(0/1/2)`、`allowModify/Forward/Transfer/Comments/mustPass` | **概念缺失**；`wf_node_field_perm` 是字段级、定义期静态矩阵（`perm 0隐藏/1只读/2可编辑/3必填`），**表达不了"能否改流程路径/加后续节点"** | 仅提及 |
| 4 | **自由步骤模板** | `workflow_freeflow_template` + `freeflow_template_share` + `SaveTemplate/ImportFromTemplate/GetTemplateList/DelTemplate/GetTemplateShareListCmd` + `freeflowTemplate/` 前端 + 共享范围（HR/分部/部门/角色/岗位 + 密级） | **缺失**。`WfDefinitionController:177-180` 的"另存为新版本"复制的是整份 BPMN 定义，不是运行期步骤模板；全库 Java/TS `freeFlow` 0 命中 | 仅一笔带过 |
| 5 | **自动批准/提交/代理/加签特殊分支** | `RequestManager:2677-2681` 自由流程下加签强制 `isFlowToNext="1"`；`JudgeAutoApproveCmd:177-189` 用 `allowModify` 判定；`workflow_agent` 代理；`FreeWorkflowSimple:644-646` 逐级退还原被代理人 | **全缺**，且**代理设置本身也缺**（全库无 `workflow_agent`/`WfAgent` 表与端点）；`autoApprove` 四个重载无自由流程分支 | 完全没提 |
| 6 | **移动端** | `WorkflowServiceImpl:3135-3165, 4063-4075, 7557-7630` + `WorkflowBaseInfo.isFreeWorkflow/freewftype` + `MobileShinkFlowTable` | **无移动端办理页**（`pages/Workflow` 下 mobile/H5 0 命中，只有 `MobileFieldControl.tsx` 字段控件） | 完全没提 |
| 7 | **节点路径编辑权 vs 字段权限** | 见 #3 | 见 #3 | 仅提及 |
| 8 | **退回 / 强制收回** | 见 §10.2 | 见 §10.2 | 仅一笔带过 |

### 10.6 公文（odoc）侧自由流程六权限 —— 与项目主线直接相关

E9：`sql202005271203.sql:1-13` 建 `odoc_freeflow_manager(id, workflowid, type, typeobjectid, updateSign, appendSign, withDraw, updateOperator, archiving, adjustlog)`；`FreeFlowBtnEnum:5-12` 六权限（**改签 / 代录意见 / 收回 / 改办理人 / 归档 / 调整日志**）；`OdocFreeFlowUtil:41-88, 108-148` 生成可编辑列与条件；前端 `freeFlowCom.js:81-113` 的 `WeaTableEditable` + 添加权限弹窗（`freeFlowAddCom.js:18-19`）。

springBlade：`blade-workflow/.../odoc/` 精确检索 `freeflow|updateSign|appendSign|adjustlog|withDraw|updateOperator` **仅命中 `EsignSigner.appendSignData`（HTTP 签章签名，无关）**；`doc/sql` 无对应 DDL；odoc 端点只有 `/odoc/doc/*`、`/odoc/todo|done|my-request`、`/odoc/supervise/*`、`/odoc/dict/*`。

> **判断**：§7.6.1 已正确把 `freeFlowTable` 定性为「权限设置可编辑表格」，但初版只把它当术语澄清，**没有把六权限作为独立能力纳入范围**。项目主线是收发文对齐 E9，这块应明确决策（已加入 §8 待决第 8 项）：随 P2 做 / 单列一期 / 明确不做。

### 10.7 本次修订对初版结论的影响

| 初版结论 | 复查后 |
|---|---|
| 推荐 P1 路线 A（改动小、配套全复用） | ✅ **维持，且更稳** —— 静态节点池天然规避 §10.1 的 nodeKey 连锁断点 |
| P1 六处改造点 | ➕ 补 2 项：发布时序（硬前提）、会签 3/4 口径澄清 |
| 路线 B「新增两张实例级表 + 推进器」即可 | ❌ **成本被进一步低估** —— 表要补 `node_key/def_id/version` 与唯一键，还要为公文钩子、字段权限、菜单、快照、意见、进度图、退回、超时逐项加自由上下文，另加 §7.7 并发控制 |
| §7.3 配套适配清单 | ➕ 补「强制收回」展开、「自动动作/代理」行、「移动端」行 |
| `flowtable` 能摆不能跑 | ✅ 维持（与 §10 无冲突） |
| 「自由流程是唯一建议降级或延后的特性」 | ✅ 维持，理由更充分 |

---

## 11. 变更记录

| 日期 | 内容 |
|---|---|
| 2026-11-04 | 首次整理。整合 E9 源码核查、springBlade 现状核查、`doc/md` 既有分析盘点，给出偏差、三路线对比与 P0/P1/P2 实施建议。**未修改任何业务代码**。 |
| 2026-11-04 | 新增 **§7.6「载体选择：flowtable 能否承载自由流程步骤」**：澄清三种 `flowtable` 术语，核查 formmode 主表/明细表链路，给出「能摆不能跑」结论、五条硬障碍、扁平键易误判坑、三种载体对比与两写建议；同步补充摘要第 7 条、待决事项第 7 项、§9.2/§9.3 证据索引。仍为只读核查，未改业务代码。 |
| 2026-11-04 | 第二轮复查：新增 **§3.6「设置界面分层」（E9 五类界面 + 三代弹窗形态 + springBlade 落点）** 与 **§10「遗漏项复查」**（node_key 连锁断点、退回、并发、发布时序、8 类周边能力、公文六权限）；新增 §7.7 并发与幂等、§9.4 补证；**修正初版 §5.2 表设计缺 `node_key` 的缺陷**，补 `def_id/version` 与 node_key 生成规则；§5.1 / §6 P1 补发布时序与会签口径；摘要补第 8 条、待决补 8-10 项。**全程只读核查，未修改任何业务代码**。 |
| 2026-11-04 | **技术路线定稿 + 全量实施完成**：放弃初版「免模型 + `F_<instId>_<flowOrder>` 动态 key」，改采 **「静态节点池绑定 + 实例级步骤编排 + 影子节点装饰」**——`wf_task.node_key` 恒为池内静态 activityId，`WfFreeNodeResolver` 装饰成带步骤名/会签/角色标记的 `WfProcessNode`，使公文四钩子、字段权限、菜单、超时、快照、进度图等以 `def_id+node_key` 为索引的链路无需重写；末步 `completeTask` 走 EndEvent 复用既有归档。P0/P1/P2 全部落地，含节点池默认生成与数量可配、办理页两个新弹窗、四处级联清理、退回实例链路、测试态短路、并发乐观锁。验证：编译 BUILD SUCCESS、单测 **22/22**、BPMN 校验 PASS、真机端到端 **17/17**，演示数据全量回滚（含引擎运行态/部署/procdef）。实施细节、逐项适配结论、已知限制与回退方式见 **`自由流程-实施与验收手册.md`**。 |
