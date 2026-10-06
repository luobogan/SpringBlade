# 起止节点建模：开始/结束事件本质与「申请人填单」UserTask 根治方案

> 文档目标：澄清 BPMN 起止事件（StartEvent / EndEvent）的规范本质，剖析本项目当前「把它们当业务节点（逻辑标记）使用」所产生的体验与功能瓶颈，并给出将「申请人填单」建模为真正 UserTask（等待态）的根治方案及其改动范围。
>
> 配套事实：本文所有结论均来自对实例 `2107151967368323074`（定义 `2100918142244032513`，会签节点 `Activity_16wyet2` 业务领导，3 人：admin / dd123 / test7960）及其部署 BPMN、引擎历史轨迹的实测核对。

---

## 一、BPMN 规范中开始/结束事件的本质

### 1.1 规范定义

在 BPMN 2.0 中，`StartEvent` / `EndEvent` 属于 **Event（事件）** 大类，而 `UserTask` 属于 **Activity（活动）**。二者根本区别：

| 维度 | StartEvent / EndEvent | UserTask |
|---|---|---|
| 规范类别 | Event（信号） | Activity（工作） |
| 是否等待态 | **否**，瞬时通过 | **是**，生成工作项 |
| 引擎产物 | 仅 `ACT_HI_ACTINST` 行 | `ACT_RU_TASK` 待办 + 历史行 |
| 办理人 / 表单 / 权限 | 无 | 有 |
| 语义 | 「token 在此产生 / 销毁」 | 「有人要干一件事」 |

具体：

- **StartEvent**：流程或分支的**触发信号**（trigger）。它标记 token 在此产生，可以是 None / Message / Timer / Conditional / Signal 等子类型，但无论哪种，语义都是「**实例或分支的开始**」；引擎执行到它是**瞬时通过（instantaneous pass-through）**。
- **EndEvent**：token 的**终止 / 消费信号**。到达即销毁 token，可能抛出 Error / Message / Terminate / Cancel 等副作用，但同样**不产生工作项、不停留**。

> **结论**：起止节点**只代表流程实例或分支的起止逻辑信号**，是图的**结构锚点**（定义入口 / 出口、分支汇聚 / 终止），**不承载业务动作、页面跳转或数据处理**。

### 1.2 本项目代码对本质的印证

- `WfTaskServiceImpl` 1014–1032（退回发起人注释）明确写道：

  > 创建节点在 BPMN 里是 `startEvent`（对齐泛微「创建节点」＝发起人填单环节），**不是等待态** —— 直接把 token 移过去它不会停住，会立刻沿出口继续流出。

- `WfRejectManager` 138–144（可退回候选剔除）：剔除「引擎停不住」的节点类型 归档(3) / 等待(5) / 自动处理(6) / 网关(7)，因其不是等待态。
- 实测：实例 `2107151967368323074` 的 `ACT_HI_TASKINST` 中 `Event_1wh3dpi`（开始）与 `Event_13rmetl`（结束）**均无任何 task 行**，只有 `ACT_HI_ACTINST` 活动行；`WfInstanceServiceImpl#appendArchiveRecord`（1021–1070）要**读侧手工合成**一条归档流转记录，正是因为 endEvent 引擎不生成任务。

---

## 二、仅当作「逻辑标记」使用时的问题

本项目的实质错配：**把起止节点当成业务节点用了**。`wf_process_node` 里它们有记录（`node_type` 0 / 3），设计器给它们配置**节点信息 / 操作者**（实测归档节点配了 `opType="17"` 创建人本人），流程图、流转意见、节点操作者面板也都展示它们。

### 2.1 开始节点只当标记的问题

#### (a) 引擎停不住 → 退回发起人必须「状态分裂」

`rejectToStarter`（1033–1068）的做法是刻意制造两套真值：

- **引擎侧**：token 移到创建节点 → 自然流出、**停在第一审批节点**（保留引擎活动任务，避免 `advance` 因「无活动任务」误判归档）
- **台账侧**：当前节点记为**开始节点** + 给发起人一条**合成待办**（`engine_task_id` 留空）

两套真值靠「**禁止调用 `advance()`**」（1030–1031 明令）这条**注释约定**维系，而非机制保证——任何一处漏判即崩。

#### (b) 待办读源翻 act 后机制直接失效

`application.yml` 第 63 行 **`task-read-source: act`**（2026-09-30 已达标翻 act，Nacos 无覆盖）。

act 读源读的是**引擎真实任务**。而「退回发起人」后，引擎恰恰在 `Activity_16wyet2` 停着 3 条**真实任务** → 这 3 条**照样出现在 3 个审批人的待办里，照样能提交/退回**；发起人那条合成待办反而要靠 `WfTaskActReader` 的兜底 merge（`BIZ_TASK_ID_ IS NULL` 的 wf_task 行，203–241）才显示。

→ **退回给发起人后审批人待办里照样能看到、照样能操作**。这是与「开始节点状态分裂」设计的结构性冲突，与下述缺陷①相互独立。

#### (c) 待办生成 / 完成语义被特殊化

- 开始节点的「提交」不是 `completeTask`，而是 `resubmitByCreator`（1091–1111）：把 token 移回创建节点重新入流
- 识别靠 `isCreatorResubmitTask`（1076–1082）：「`nodeType==0` **且** `engine_task_id` 为空」——**用数据特征反推语义**，脆弱
- 一旦漏判去 complete，就会 complete 掉停在第一审批节点的引擎任务 → **直接跳过第一个审批节点**（311–315 注释即为此警告）

#### (d) UI 只能靠特例打补丁

`InstanceFlow.tsx` 716–721 对开始节点只保留 `submit/save/print/opinion/attach`，屏蔽退回 / 转发 / 转办 / 加签 / 传阅 / 催办等。这类屏蔽随功能增加持续膨胀。

#### (e) 可视化：开始节点恒为「已完成」

进度图里开始节点永远是绿（已完成），**无法体现「退回后待申请人修改」**这一真实状态——而这是用户心智里最重要的一步。

### 2.2 结束节点只当标记的问题

#### (a) 无任务 → 流转意见要合成

`appendArchiveRecord`（1021–1070）手工合成一条归档记录（**读侧合成、不写库**），否则流转意见里看不到「归档」这一步；且合成记录的操作人 / 时间无真实来源，只能用最后一条记录的操作人与实例 `end_time` 兜底。

#### (b) 配了操作者却没有承载体

实测归档节点配了 `opType="17"`（创建人本人），它会作为「接收人」出现在流转意见里（用户已确认这是期望行为）。但它在引擎里**没有任何任务**，所以这个「操作者」是**纯装饰性配置**：不产生待办、不产生权限、不产生超时。与「节点操作者面板会展示它」「退回候选剔除 nodeType=3」等规则互相矛盾。

> 注：归档节点显示操作者（含 opType=17）是本项目确认的期望口径，相关「归档节点不显示其办理人」的旧注释（971–972 行）已过时，以实现为准，不要照旧注释去改。

#### (c) 进度图要额外纠偏

`FlowDiagram` 175–183 按 `instanceStatus` 对 EndEvent **强制去绿改灰**；`WfProgressView` 还要单独维护 `cancelledActivityIds`。因为引擎会给 endEvent 打 `ACT_HI` 行，但「是否已归档」只能看实例状态而非节点本身。

#### (d) 配置与能力不一致

设计器允许给归档节点配操作者，但 `computeRejectableNodes` 永远剔除它（正确），用户配了却看不到任何效果。

---

## 三、根治方案：把「申请人填单」建模为真正的 UserTask（等待态）

### 3.1 方案形态

```
现状：   startEvent(创建节点·标记) ──► userTask 审批节点1 ──► ... ──► endEvent(归档·标记)

改造：   startEvent(纯信号) ──► userTask「申请人填单」(等待态, assignee=发起人) ──► 审批节点1 ──► ...
```

`wf:node` 的 `nodeType=0` **保留**（业务语义上它仍是「创建节点」），但**底层 BPMN 元素类型从 `startEvent` 变为 `userTask`**，引擎会真正为它生成任务、停住 token。

### 3.2 为什么最干净

| 维度 | 现状（标记 + 合成待办） | UserTask 方案 |
|---|---|---|
| 真值源 | **两套**（ACT 停审批节点 / wf_task 记开始节点） | **一套**：引擎任务即待办 |
| 退回发起人 | 专用路径 + 「禁止 advance」的**约定** | **普通退回**：moveActivity 过去，引擎自然停住 |
| 重新提交 | `resubmitByCreator` 移 token 特例 | **普通 completeTask** |
| 待办 | 合成行（`engine_task_id` 空）+ 兜底 merge | 真引擎任务，**act / wf 读源都自洽** |
| 会签 / 权限 / 表单 / 超时 | 开始节点全部特例 | 与审批节点**同构** |
| 可视化 | 恒绿，看不出「待我修改」 | 正常「进行中(蓝)」 |
| 与 act 读源 | **冲突**（本次故障根因之一） | 天然兼容 |

**核心收益**：消灭「状态分裂」与「合成待办」两个概念。创建节点与审批节点同构后，退回、待办、权限、表单权限、进度图、读源等所有通用机制自动生效，不再需要任何特例分支——把「用约定维系的不变量」变成「由机制保证的不变量」。

### 3.3 改动范围

#### ① BPMN 建模 / 设计器
- 画布：`BpmnDesigner.tsx` / `bpmnExtension.ts` —— 创建 / 保存 / 解析节点时区分 `startEvent`（纯入口信号）与「申请人填单 `userTask`」；`wf:node` 扩展写法随之调整
- 节点信息面板：`NodeInfoTable.tsx` / `NodeDetail.tsx` —— 开始节点按 userTask 口径开放操作者、表单权限、超时、操作菜单（现在这些对它是部分无效的装饰配置）
- **存量定义**：需要迁移脚本把既有 `startEvent` 改造成 userTask，否则新旧模型混跑

#### ② 部署
- `WfDefinitionServiceImpl` 部署路径、MI 注入（`engine-multi-instance` 开关）、`POST /definition/admin/redeploy-mi`
- 存量已部署定义的**在途实例不受影响**（Flowable 绑定定义版本），但**新旧语义并存期必须兼容**
- 需要一次**全量 redeploy**

#### ③ 发起页 / 发起接口
- `Start.tsx` / `startInstance`：发起语义从「创建实例即进入第一审批节点」变为「创建实例 → 停在填单 userTask（草稿态）→ 提交才推进」
- **草稿机制可一并收敛**：现在草稿（`draftInstId` / `saveDraft` / `getSnapshot`）是**另一套合成机制**，与「退回发起人的合成待办」同源同构，改造后可统一为一个真实的填单 userTask
- `startInstance` 返回后需走正常 `advance` 生成填单 userTask 的引擎任务与 wf_task 待办

#### ④ 后端运行期（删特例）
- **删除 / 简化**：`rejectToStarter`、`isCreatorResubmitTask`、`resubmitByCreator`、合成待办分支
- `WfTaskServiceImpl` 469–479：删掉 `nodeType==0` 特判，退回目标统一走 `moveActivity + advance`
- 开始节点「操作者 = 发起人」（当前 533 行特判）改为真正配置，由 `WfOperatorResolver` 的 `OP_CREATOR=17` 解析（这本就是通用能力）
- `WfRejectManager`：开始节点变 userTask 后自然可退，无需再保留 0 型特例
- **结束节点同步收敛**：要么也建模成「归档 userTask」（若归档确需人处理），要么在设计器里**明确标为不可配置操作者**，消除装饰性配置

#### ⑤ 读源 / 进度图
- `task-read-source=act` 与合成待办的冲突自然消失，兜底 merge 逻辑可简化
- 进度图能真实反映「退回后待申请人修改」

#### ⑥ 数据迁移
- 存量 `wf_task` 中 `nodeType=0` 且 `engine_task_id` 为空的合成待办行需识别处理
- 存量实例 `current_node_key` 指向创建节点的，需按新语义修正
- 建议**灰度**：新定义走新模型，存量走兼容分支（`rejectToStarter` 保留一段时间）

### 3.4 建议节奏

1. **先止血**（小、快、独立，详见第四章）
2. **再立项** UserTask 改造：设计器 → 部署 → 发起页 / 接口 → 运行期删特例 → 存量迁移脚本，按流程定义灰度
3. **结束节点同步收敛**：先明确「归档是否为人步骤」，再决定建模成 userTask 还是禁用其操作者配置

> ⚠️ 提醒：根治是**建模范式变更**，不是小重构。建议先在测试环境用 1–2 个流程跑通全链路（发起 → 退回发起人 → 重新提交 → 归档）再全量。

---

## 四、当前已定位的两个止血缺陷（实测证据）

> 止血修复独立于根治，可先单独上线，为根治争取时间并作为兼容期安全网。

### 缺陷① BPMN 读源自身不一致 → 退回落错分支

**现象**：实例 `2107151967368323074` 的「退回」等于「没退」——退回同一秒即重建 3 条审批待办，流程仍停在原审批节点。

**证据链**：

- `ACT_HI_COMMENT` 仅 3 条：00:52:32 提交（test1）/ 00:53:43 退回#1（admin）/ 01:03:36 退回#2（admin）——**两次退回之间无任何重新提交**
- 每次退回**同一秒**即重建 3 条待办；`ACT_HI_TASKINST` 删除原因为 `Change parent activity to Event_1wh3dpi`
- `wf_task` 9 行：6 条已闭（status 2/4）+ 3 条 status=0（engine_task_id 293090/293094/293098）；`ACT_RU_TASK` 恰 3 条匹配 → 无幽灵待办
- 部署 BPMN 实测：`<startEvent id="Event_1wh3dpi">` 的 `<wf:node>` **无 nodeType 属性**；`<userTask id="Activity_16wyet2">` 的 `wf:node` 有 `nodeType="1"`

**根因**：

- `WfBpmnExtensionReader.nodes()`（122–129）对 StartEvent 用 `syntheticNode`（224–236）**强制 nodeType=0** → 开始节点能成为退回候选
- `WfBpmnExtensionReader.node()`（88–94）走 `toProcessNode`（296–315），nodeType **只取自 `wf:node` 扩展属性** → 开始节点取到 **null**
- `WfTaskServiceImpl` 469–479 判定 `targetNode.getNodeType() == 0` 失败 → 落 else 分支 `moveActivity + advance()` → 引擎从 startEvent 流出停第一审批节点、advance 生成待办并把 `current_node_key` 改回 `Activity_16wyet2`（即注释 1030–1031 警告的「等于没退回」）

**修复**：统一 `node()` 与 `nodes()` 口径——非 UserTask 元素按**元素类型**权威映射 nodeType（StartEvent=0 / EndEvent=3 / Gateway=7 / ReceiveTask·IntermediateCatchEvent·ThrowEvent=5 / Service·Script·Send·BusinessRule·CallActivity=6 / ManualTask=1），复用 `syntheticNode`。UserTask 行为零变化；非 UserTask 从「可能 null」变「恒有值」，与 `nodes()` 及 `wf_process_node` 口径收敛。

### 缺陷② 与 `task-read-source: act` 的结构性冲突

**现象**：即使修好缺陷①，使 `rejectToStarter` 正确执行，「退回给发起人」后审批人待办里照样能看到、照样能提交/退回。

**根因**：`rejectToStarter` 让引擎**停在第一审批节点**（真实 `ACT_RU_TASK` 行），台账记开始节点 + 合成待办（engine_task_id 空）。读源 = act 读引擎真实任务 → 那 3 条停靠任务照样进审批人待办；合成待办反靠 `WfTaskActReader` 兜底 merge 才显示。**这是状态分裂设计与 act 读源的结构性冲突，与缺陷①相互独立。**

**修复**：在 act 读源侧屏蔽「退回发起人」期间停在第一审批节点的**停靠任务**——当 `wf_instance.current_node_key` 指向创建节点（node_type=0）时，该实例的引擎停靠任务不计入待办，只认合成待办。须同时作用于「待办列表」与「角标计数」（countOne 两处），否则列表为空但红点有数。

> 备选：将 `task-read-source` 回退 `wf`。但属共享配置变更（Nacos），本方案不擅自改动，仅在文档记录。

---

## 五、实施计划

### 阶段一 · 止血（可独立上线）

| # | 任务 | 改动文件 |
|---|---|---|
| 1 | 落成本设计文档 | `springBlade/doc/md/起止节点建模-开始结束事件本质与UserTask根治方案.md` |
| 2 | 止血①：统一 `WfBpmnExtensionReader.node()` 与 `nodes()` 口径 | `resolver/WfBpmnExtensionReader.java` |
| 3 | 止血②：act 读源按实例语义屏蔽创建节点的引擎停靠任务（列表 + 角标） | `service/helper/WfTaskActReader.java` |
| 4 | 回归验证：发起 / 退回发起人 / 重新提交 / 归档 全链路（覆盖会签 3 人） | 接口级（`springblade-dev-login` 技能） |

### 阶段二 · 根治（建模范式变更，建议灰度）

| # | 任务 | 改动文件 |
|---|---|---|
| 5 | 设计器与 BPMN 建模：申请人填单改为 UserTask + 存量定义迁移方案 | `pages/FormMode/WorkflowDesign/*`、`resolver/WfBpmnExtensionReader.java` |
| 6 | 部署路径与发起页 / 发起接口：发起后停在填单 UserTask，草稿机制并入 | `WfDefinitionServiceImpl`、`Start.tsx`、`startInstance` |
| 7 | 删除运行期特例（`rejectToStarter` / `isCreatorResubmitTask` / `resubmitByCreator` / 合成待办）+ 结束节点收敛 | `WfTaskServiceImpl`、`WfRejectManager`、`WfInstanceServiceImpl`、`ProcessServiceImpl`、`InstanceFlow.tsx` |

### 约束

- 不得为跑通测试擅自修改共享 dev 环境配置 / 开关（含 Nacos）；`blade-workflow-dev.yaml` 在 Nacos 不存在（404），相关开关取 `application.yml` 本地值。
- 根治须灰度：新定义走新模型，存量定义与在途实例保留兼容分支。
