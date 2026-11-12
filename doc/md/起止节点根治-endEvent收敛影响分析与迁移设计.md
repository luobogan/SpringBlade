# 起止节点根治 · endEvent（归档节点）收敛 · 影响分析与迁移设计

> 配套文档：
> - [起止节点建模-开始结束事件本质与UserTask根治方案.md](./起止节点建模-开始结束事件本质与UserTask根治方案.md)
> - [起止节点根治-实施设计与存量迁移.md](./起止节点根治-实施设计与存量迁移.md)
>
> 本文是「后续独立排期」第 5 项的**前置设计**：只做影响分析与迁移设计，**不改动任何代码**。改造本身（含前端设计器）待本文设计拍板、并在联调环境验证后再实施。
>
> 探查基线：2026-11-12 对 `blade-workflow` 全模块 `nodeType==3 / archive / EndEvent` 关联代码的 very-thorough 探查。所有行号以探查时为准，落地前需 rebase 复核。

---

## 零、背景与硬约束

### 0.1 当前真实状态（与文档规划的偏差）

阶段二「开始节点根治」已把创建节点从 `startEvent` 改为 `UserTask(nodeType=0)`，**结束节点（归档）却仍是 `EndEvent` 且 `wf:node nodeType==3` 仍挂在 BPMN 上**。也就是说：

- **运行期 endEvent 已经是"纯连接器"**：`WfInstanceServiceImpl.advanceInternal` 靠"引擎无活动任务"（`engineTasks.isEmpty()`，`:3123`）判归档，而非显式判 `nodeType==3`；`WfRejectManager.computeRejectableNodes` 把 `3` 列入"不可退回"集合（`:183`）。endEvent 不生成待办、不产生权限、不退回——这三点与 BPMN 规范一致，**无需改动**。
- **未收敛的是"配置侧"**：
  1. `WfDefinitionServiceImpl.saveBpmn` 仍把 EndEvent(`nodeType==3`) 写入 `wf_process_node`（`:694-724`），作为**归档配置载体**（归档人 / 归档文案从这里读）；
  2. 设计器仍允许给 endEvent 配**操作者 / 表单权限 / 操作菜单**（`NodeDetail.tsx:704` 仅用文字提示"除标题显示外不生效"，`NodeInfoTable.tsx:658` 已标注 `仍待 D 组「结束节点配置收敛」单独排期`）；
  3. 代码里 **4 处各自 `loadNodes 找 nodeType==3`** 散落扫描（`appendArchiveRecord:1388` / `appendArchiveLog:4457` / `nextHandlerNames:1329` / `fillActivityColors:4153`），无统一入口。

### 0.2 硬约束（不可破坏，违反即算回归）

> 依据：[feedback_archive_node_operator（ID 40052367）](../../.codebuddy/memory/project_memory_files.md 同索引) —— 用户 2026-10-05 明确确认："归档节点也要显示操作者的，没问题就不改了"。

- **流转意见的"接收人"必须继续显示归档节点配置的操作者**（典型 `opType=17` 创建人本人），即 `nextHandlerNames:1329-1340` 行为保留。
- 任何收敛方案若让"归档节点操作者"读不到，即破坏约束。
- ⚠️ **现状校验发现（2026-11-12）**：`nextHandlerNames` 的**注释（`:1317-1318`）与实现（`:1329-1338`）自相矛盾**——注释写"归档(3)/结束节点不显示其办理人，改为显示节点名"，但实现是"**优先取该节点操作者作接收人，未配置时才回退节点名**"。实现符合本硬约束，注释是过时反向描述（同根治文档 §2.2(b) 已标注）。**改造时以代码 `:1329-1338` 为准，切勿照过时注释把归档节点改成只显示节点名**。

### 0.3 决策风格对齐

- 用户反感"手工补配置"（创建节点默认办理人 `opType=17` 自动落库、`REQUIRED_MENUS=['submit','save']` 自动默认）——归档人应**默认解析为创建人本人**，UI 可覆盖但默认有值，不要逼用户手工配。
- 优先低风险、小动、可追溯；"重度重构 + schema 变更"作为可选而非默认。

---

## 一、当前架构事实（探查结果，逐位置）

> 以下为 `nodeType==3` / `EndEvent` / 归档 在 9+ 文件中的真实落点。格式 `路径:行号`。

### 1.1 定义侧（谁产生 nodeType==3 并落库）

| 位置 | 逻辑 | 收敛时处置 |
|---|---|---|
| `WfDefinitionServiceImpl.nodeTypeOf:2125` | `EndEvent → 3`（归档） | **唯一权威源之一**，保留 |
| `WfDefinitionServiceImpl.saveBpmn:694-724` | `nodeTypeOf!=null` 才 upsert `wf_process_node`；EndEvent(3) 由此入库 | 轻量方案保留（作配置载体）；重度方案移除 |
| `WfDefinitionServiceImpl.defaultNodeName:2199` | `nodeType==3 → "结束"` | 保留 |
| `WfDefinitionServiceImpl.simulate:3073` | `nodeType==3 → 模拟到归档即止` | 保留 |
| `WfBpmnExtensionReader.node:96-107` | EndEvent 经 `toProcessNode`，`nodeTypeByElement` 强制覆盖为 3 | 保留（与 toNodes 一致） |
| `WfBpmnExtensionReader.toNodes:249` | `syntheticNode(defId,e,3)` 合成归档节点 | 保留 |
| `WfBpmnExtensionReader.nodeTypeByElement:291` | EndEvent→3 | 保留 |
| `WfBpmnExtensionReader.syntheticNode:301-313` | 读 `wf:node` 扩展（operators/extJson）后强制 nodeType | 保留（归档人来源在此） |

### 1.2 运行侧（谁消费 nodeType==3）

| 位置 | 逻辑 | 收敛时处置 |
|---|---|---|
| `WfInstanceServiceImpl.advanceInternal:3123` | `engineTasks.isEmpty()` → 流程结束（**不显式判 nodeType==3**） | 保留，无需改 |
| `WfInstanceServiceImpl.appendArchiveLog:4452-4493` | 写库"流程归档"行；归档人 = `operatorResolver.resolve(defId, archiveNodeKey, ...)` 取第一个，兜底 `lastDoneOperator → starter → 0(系统)` | 归档人解析来源需统一（见 §三） |
| `WfInstanceServiceImpl.appendArchiveRecord:1381-1419` | 读侧合成"归档"行（**不写库**，operator=最后办理人，name=归档节点名） | 保留合成语义；扫描可收口 |
| `WfInstanceServiceImpl.nextHandlerNames:1329-1340` | nodeType==3 优先取操作者作"接收人"，空则显示节点名"归档" | **硬约束，保留** |
| `WfInstanceServiceImpl.fillActivityColors:4153-4160` | 取消态 `hasEndEvent` 判定（不介入去绿，交前端） | 保留 |

### 1.3 校验侧

| 位置 | 逻辑 | 收敛时处置 |
|---|---|---|
| `WfRejectManager.computeRejectableNodes:183-186` | 剔除 `{3,5,6,7}`（endEvent 非等待态，移过去不停） | **保留**（引擎事实） |
| `WfTestServiceImpl:931-948` | 归档(3) **不再要求操作者**（因 endEvent 不生成任务，操作者仅记归档人，运行期兜底） | 若收敛后强制归档必须有操作者会反向影响，需注意 |
| `WfDefinitionServiceImpl:3221-3226` | 表单内容预校验 `(t>=0 && t<=3)` 必须 `isLayoutMode` | 与"endEvent 不渲染表单"有张力，需一并调口径 |

### 1.4 展示侧（进度图）

| 位置 | 逻辑 | 收敛时处置 |
|---|---|---|
| `FlowDiagram.tsx:231-249` | `instStatus∈{2,3}` 时对 `EndEvent` 元素 `removeMarker('wf-act-success')` + `addMarker('wf-act-cancel')`（去绿改灰） | **保留** |
| `WfProgressView.java:49-53` | `cancelledActivityIds` 不主动填，交给前端按 `instanceStatus` 纠偏 | 保留 |
| `services/workflow/index.ts:1163` `buildActivityColors` | 四色契约映射 | 保留 |

### 1.5 前端设计器（装饰性配置能力所在）

| 位置 | 逻辑 | 收敛时处置 |
|---|---|---|
| `NodeDetail.tsx:704-719` | nodeType==3 显示 Alert：除"标题显示"外设置项不生效，归档人在"操作者"页签配 | 收敛后改为"归档配置在此定义级设置" |
| `WorkflowDesign.tsx buildDefaultBpmn:177` | `<bpmn:endEvent name="结束"/>`，默认 `<wf:operator opType="17"/>`（创建人本人，即归档人默认来源） | 轻量方案保留；重度方案把 endEvent 的 wf:operator 移出 |
| `BpmnDesigner.tsx shapeTypeFor:1192-1195` | `nt===3 && !hasEnd → 'bpmn:EndEvent'`；末尾无 End 自动补 EndEvent | 保留 |
| `NodeInfoTable.tsx:652-665` | nodeType==3 操作菜单列显示"—"；`:658` TODO：归档人/归档文案提级 + endEvent 纯连接器 | **本次收敛的核心 TODO** |
| `bpmnExtension.ts` | 通用 wf:node 读写，**不特判 endEvent**；`:579-612` 仅对 nodeType=0 补默认办理人 | 轻量方案：增加"endEvent 不写/不读操作者·表单·菜单"的收口 |

### 1.6 流转意见硬约束落点

- `nextHandlerNames:1329-1340`（接收人显示归档节点操作者）—— **保留**，且是验证归档人不丢失的回归锚。

### 1.7 回归锚点（改造前后必跑）

- `WfBpmnDefNodesTest:85`（断言 EndEvent→3）
- `WfRejectManagerBpmnTest`（归档剔除 / 网关穿透 / 折叠连线回溯）
- `WfArchiveReviveEngineTest`（归档复活 = `deleteProcessInstance` 后新建运行态）
- `WfProgressColorTest`、`WfEngineSmokeTest:114`（跑到归档）、`WfRuntimeLifecycleVerifyTest`
- 前端：设计器新建流程→发布→发起→退回→归档 全链路（联调环境）

---

## 二、收敛方案选型

### 方案 A · 归档建模成"归档 UserTask"（不推荐）

把归档节点从 `EndEvent` 改回 `UserTask(nodeType=3)`，让它生成真实待办。

- **否决**：会改变"归档"语义——当前归档是引擎到达 endEvent **自动**触发（`advanceInternal` 判无活动任务），不涉及"人去点归档"。做成 UserTask 会引入"归档待办"，全链路（advance/doApprove/待办读源）都要为归档节点加等待态分支，与"归档自动完成"的产品语义冲突，且用户从未要求"归档需人处理"。属过度设计。

### 方案 B · 直接禁用 endEvent 操作者配置、不提级（不推荐）

设计器不再允许配归档人，运行期归档人一律兜底"最后办理人/系统"。

- **否决**：直接违反 §0.2 硬约束（归档节点显示操作者作为接收人是期望行为）。

### 方案 C · 提级到流程定义级 + endEvent 收敛（推荐，分轻重两层）

**核心思想**：保留"归档节点显示操作者"的期望行为，但把"归档人/归档文案"的来源从"散落在 endEvent 的 wf:node 扩展"统一为**单一来源**，并消除设计器的装饰性配置能力。按风险分两层：

#### C1 · 轻量收敛（推荐先做，零 schema 变更）

- **EndEvent 仍是 `nodeType==3`、仍写 `wf_process_node`**，但**明确其语义 = 归档配置载体（不是业务节点）**。
- **设计器收敛**：`bpmnExtension.ts` 对 endEvent 收口——`NodeInfoTable`/`NodeDetail` 已部分收口（操作菜单列"—"、Alert 提示），进一步**屏蔽 endEvent 的"表单权限""操作菜单""字段权限"编辑入口**，只保留"标题显示"与"归档人（操作者页签）"。消除"配了不生效"的认知冲突。
- **代码收口**：新增 `archiveNode(defId)` / `findByNodeType(defId, 3)` 统一方法，替换 4 处 `loadNodes 找 nodeType==3` 散落扫描（`appendArchiveRecord:1388` / `appendArchiveLog:4457` / `nextHandlerNames:1329` / `fillActivityColors:4153`）。单一来源、单一出口。
- **归档人解析保留**：来源仍是 `wf_process_node(3)` 的 `wf:operator`（典型 `opType=17` 创建人本人），兜底链（最后办理人 → 发起人 → 系统）保留。
- **不破坏**：§0.2 硬约束、自动归档、进度图去绿、reject 剔除。
- **收益**：消除即期风险（装饰性配置误导、4 处重复扫描漂移）+ 为重度方案铺路。

#### C2 · 重度提级（可选，schema 变更）

在 C1 基础上，把"归档人/归档文案"从 `wf_process_node(3)` 行**提级到 `wf_process_definition` 新列**（如 `archive_assignee_type` / `archive_assignee` / `archive_title_tpl`），EndEvent 完全不进 `wf_process_node`。

- 收益：endEvent 真正"纯连接器"，归档配置与流程定义同生命周期，无残留行。
- 代价：`wf_process_definition` schema 变更 + 设计器归档配置 UI 重构（移到流程级设置面板）+ 存量 `wf_process_definition` 加列迁移 + 发布模板调整。
- **判断**：C2 的"干净"收益有限（C1 已消除即期风险），而 schema 变更 + UI 重构风险显著。建议**先做 C1，C2 作为后续可选**，除非清库重建时顺手做（见 §四）。

---

## 三、方案 C1 详细设计（轻量收敛，落地草图）

### 3.1 数据模型

- **不改 schema**。`wf_process_node(nodeType=3)` 继续作为归档配置载体；其 `wf:operator` = 归档人，`extJson` 可选承载归档文案模板。

### 3.2 读源收口（核心改动）

新增 `WfInstanceServiceImpl` 私有方法：

```java
/** 归档节点（nodeType==3）统一出口：替代 4 处 loadNodes 散落扫描 */
private WfProcessNode archiveNode(Long defId) {
    List<WfProcessNode> nodes = loadNodes(defId);   // 已收口到 BPMN 读源
    return nodes.stream()
        .filter(n -> n.getNodeType() != null && n.getNodeType() == 3)
        .findFirst().orElse(null);
}
```

调用点替换：
- `appendArchiveRecord:1388` → `archiveNode(defId)`
- `appendArchiveLog:4457` → `archiveNode(defId)`（取其 `nodeKey` + 归档人）
- `nextHandlerNames:1329` → `archiveNode(defId)`
- `fillActivityColors:4153` 已用 `hasEndEvent`（元素类型），可不动或统一。

> 注意：`loadNodes`/`loadNodeReadOnly` 已在本轮更早（2026-11-12）收口到 BPMN 读源，故 `archiveNode` 天然走 BPMN，无需再处理 `wf_process_node` 回退（与开始节点根治后的读源口径一致）。

### 3.3 归档人解析（保留硬约束来源）

`appendArchiveLog:4468-4491` 保持：

```java
operatorResolver.resolve(defId, archiveNode.getNodeKey(), instId, starter, starter)  // 取第一个
  → 兜底 lastDoneOperator(instId) → inst.getStarter() → 0(系统)
```

`nextHandlerNames:1329-1340` 保持"优先 nodeType==3 操作者作接收人，空则节点名"。**此两处是 §0.2 硬约束的落点，C1 不改其行为**，只把"找归档节点"替换为 `archiveNode()`。

### 3.4 设计器收口（消除装饰性配置）

- `bpmnExtension.ts`：新增"endEvent 不写/不读 表单权限、操作菜单、字段权限"的收口（保留 `nodeType`、`wf:operator`、标题）。
- `NodeDetail.tsx:704`：Alert 文案改为"归档人请在「操作者」页签配置（默认创建人本人）；其余设置项不适用于归档节点"。
- `NodeInfoTable.tsx:658`：删除"仍待 D 组排期" TODO，改为"已收敛（C1）：endEvent 仅配置归档人"。

### 3.5 运行期 / 进度图 / reject

- `advanceInternal:3123`（引擎无任务判归档）：**不动**。
- `FlowDiagram.tsx:231` / `WfProgressView:49`：不动。
- `WfRejectManager:183`（剔除 3）：不动。

### 3.6 校验口径微调

- `WfDefinitionServiceImpl:3221`：表单内容预校验 `(t>=0 && t<=3) 需 isLayoutMode` 与"endEvent 不渲染表单"张力——C1 下 endEvent 仍不渲染表单，但其 `wf:node` 可能带 `extJson`，预校验可把 `t==3` 从"必须布局"放宽（归档节点不要求布局表单），避免发布被打回。需与前端 NodeDetail 收口口径对齐。
- `WfTestServiceImpl:931`：归档(3) 不要求操作者——C1 下归档人默认 `opType=17` 创建人本人，仍"不强制"，保持现状即可，勿反向要求。

---

## 四、存量迁移设计

### 4.1 沿用既有决策：dev 清库重建，不做迁移

依据[起止节点根治-实施设计与存量迁移.md §四](./起止节点根治-实施设计与存量迁移.md)（用户 2026-10-06 决策）：旧工作流数据直接清空，用新设计器重建，不写迁移脚本。

- C1（轻量收敛）**不引入 schema 变更**，与"清库重建"完全兼容：清库后用新设计器（endEvent 收口配置）重建流程即可。
- 若选 C2（重度提级），则需在清库重建**之前**先执行 `wf_process_definition` 加列 DDL，并在设计器发布模板里把归档配置写入新列而非 `wf_process_node(3)`。

### 4.2 落地顺序（建议）

1. 后端：`archiveNode()` 收口 4 处扫描 + 设计器 `bpmnExtension.ts` 收口 endEvent 扩展。
2. 前端：NodeDetail / NodeInfoTable 提示与菜单收口。
3. 单测：跑 §1.7 回归锚点 + 新增 `archiveNode()` 单测（找归档节点、无归档节点返回 null）。
4. 联调：设计器新建流程→发布→发起→退回发起人→重新提交→归档 全链路，重点核对：
   - 流转意见"接收人"仍显示归档人（§0.2 硬约束）；
   - 进度图归档节点去绿改灰正常；
   - 归档自动触发（advanceInternal 无任务判归档）正常。
5. 数据：dev 清库（按 `docs/工作流清库-新模型验证.md`）+ 新设计器重建流程。

### 4.3 回滚

- 代码：git revert 前后端。
- 数据：清库不可逆，回滚后旧定义需在旧代码下重建（同开始节点根治 §4.4）。

---

## 五、风险与待用户拍板决策点

1. **方案层级**：先做 C1（轻量，零 schema）还是直接上 C2（重度提级，schema 变更 + UI 重构）？
   - 建议：C1 先行，C2 待清库重建时顺手（若团队认为"endEvent 纯连接器"值得为干净付 schema 代价）。
2. **归档人默认形态**：保留"默认 `opType=17` 创建人本人、UI 可覆盖"（符合用户反感手工补配置），还是改为"流程级单一归档人配置"？
3. **表单预校验口径**：`WfDefinitionServiceImpl:3221` 对 `t==3` 是否从"必须布局"放宽？需与 NodeDetail 收口联动。
4. **与开始节点根治的对称关系**：开始节点已改为 UserTask(0) 并硬切清库；endEvent 收敛（C1）保持 endEvent 元素不变，仅收口配置侧——**两者独立**，不要求同批次。

---

## 六、结论

endEvent 在**运行期已是纯连接器**（自动归档、不退回、不生成任务），本次"收敛"实质是**配置侧收口**：消除设计器给 endEvent 配装饰性能力 + 消除 4 处散落扫描。推荐 **方案 C1（轻量、零 schema、保留 §0.2 硬约束）**，重度提级 C2 作为可选。改造前需 rebase 复核行号、跑 §1.7 回归锚点，并在联调环境验收"归档节点显示操作者"这一硬约束不被破坏。

---

## 七、联调验收清单（现状确认，先于改造）

> **目的**：动手改造前，先确认当前归档行为与硬约束在测试/联调环境真实成立，避免把"配置问题"与"结构改造问题"混在一起无法定位。
>
> **范围澄清（重要）**：本项验收**不依赖 WPS 中台**。归档是工作流内部逻辑（`advanceInternal` 判引擎无任务），WPS 编辑器只是表单填写界面，与归档判定 / 进度图 / 流转意见解耦。用一个已发布的收文/发文流程在测试环境跑完"发起 → 逐级审批 → 归档"即可；WPS 中台联调（P2.7）是独立事项，不必前置。

### 7.1 后端可验项（接口 / 数据库 / 单测）

| # | 验收项 | 通过判据 | 代码落点 |
|---|---|---|---|
| V1 | 归档自动触发 | 末节点提交后实例 `status=APPROVED`、`current_node_key=""`、`end_time` 非空；无需人工点"归档" | `advanceInternal:3123`（`engineTasks.isEmpty()`） |
| V2 | **硬约束·归档节点显示操作者** | 流转意见"接收人"展示归档节点配置的操作者（默认 `opType=17` 创建人本人）；未配时显示节点名"归档" | `nextHandlerNames:1329` |
| V3 | 归档记录写库 | `wf_approval_log` 出现 `log_type=审批(通过)`、`log_type_name="流程归档"` 行；归档人 = `operatorResolver` 解析首操作者，兜底 最后办理人 → 发起人 → 系统 | `appendArchiveLog:4452` |
| V4 | 归档节点不可退回 | 任意审批节点的"退回"候选列表**不含** `nodeType==3` | `WfRejectManager:184`（剔除 `{3,5,6,7}`） |
| V5 | 进度图去绿改灰 | 实例取消/终止态下，EndEvent 元素在进度图呈灰（非绿） | `FlowDiagram.tsx:231` |

### 7.2 前端 / 设计器需联调环境项

| # | 验收项 | 通过判据 | 代码落点 |
|---|---|---|---|
| V6 | 设计器归档节点提示 | 选中 endEvent 节点，`NodeDetail` 显示"除标题显示外不生效，归档人在操作者页签配" | `NodeDetail.tsx:704` |
| V7 | 设计器操作菜单收口 | 节点信息表中归档节点(3)操作菜单列显示"—" | `NodeInfoTable.tsx:652` |
| V8 | 流转意见 UI 接收人 | 实例详情流转意见中，归档步骤"接收人"显示归档节点操作者（对应 V2 的 UI 呈现） | 同 V2 |
| V9 | 进度图 UI 灰 | 对应 V5 的 UI 呈现 | 同 V5 |

### 7.3 自动化回归锚点（改造前后必跑，单测级）

- `WfBpmnDefNodesTest:85`（断言 EndEvent→3）
- `WfRejectManagerBpmnTest`（归档剔除 / 网关穿透 / 折叠连线回溯）
- `WfArchiveReviveEngineTest`（归档复活 = `deleteProcessInstance` 后新建运行态）
- `WfProgressColorTest`（进度图四色契约）
- `WfEngineSmokeTest:114`（跑到归档）
- `WfRuntimeLifecycleVerifyTest`（生命周期验证）

### 7.4 验收结论分支

- **全部通过** → 进入改造决策：选 **C1**（轻量，零 schema）或 **C2**（重度提级）。建议 C1 先行。
- **某项不通过**（尤其 V2 硬约束）→ 先在本环境修现状 bug，再谈收敛；本设计文档据实更新。
- **通过但发现新散落点**（文档未列的 `nodeType==3` 逻辑）→ 补入 §一，再改造。

### 7.5 现状基线（2026-11-12 本地跑出，改造前快照）

- **回归锚点全部通过**：§7.3 六个测试类共 **28 个测试 0 失败**（`WfProgressColorTest` 5 / `WfArchiveReviveEngineTest` 7 / `WfBpmnDefNodesTest` 4 / `WfEngineSmokeTest` 4 / `WfRejectManagerBpmnTest` 3 / `WfRuntimeLifecycleVerifyTest` 5）。其中 V4（归档剔除，`WfRejectManagerBpmnTest:137` 断言候选 `taskA,taskB`，`taskE` 归档被剔）、V定义侧（EndEvent→3，`WfBpmnDefNodesTest:85`）已硬锚定。
- **V2 硬约束现状结论**：代码 `nextHandlerNames:1329-1338` 确为"归档节点优先取操作者作接收人、空则节点名"，**符合 §0.2 硬约束**（详见 §0.2 注释/实现矛盾提示）。但该行为**当前无自动化锚定**（全仓库无断言 `nextHandlerNames` 对 `nodeType==3` 分支），仅赖代码存在 + 联调 V8 手动验。
- **V2 自动化缺口处置建议**：`nextHandlerNames` 为私有方法、依赖较重，单独补反射级单测性价比低。**建议并入 C1 改造**：在抽 `archiveNode()` 收口 4 处散落扫描时，一并将"归档节点接收人解析"提取为可测方法并补单测，把硬约束在改造前钉死、改造后守住。

> 注：本清单为验收导航。后端 V1–V5 中 V1/V3/V4 亦可在本地用 §7.3 单测覆盖，无需登录联调环境；V2/V5 及全部前端项（V6–V9）需在联调环境走真实流程页面确认。
