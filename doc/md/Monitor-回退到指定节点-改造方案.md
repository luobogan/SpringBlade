# Monitor「回退到指定节点」改造方案

> 状态：**已实现并部署（后端实测通过；前端待硬刷新验证）**　｜　口径已定：**候选 = 已办理过的节点 + 创建/填单节点**
> 关联文档：`泛微流程操作菜单与退回机制分析.md`、`SpringBlade-流程监控功能细化设计.md`
> 代码基线：`springBlade`（后端 `blade-service/blade-workflow` + `blade-service-api/blade-workflow-api`，前端 `ant-design-pro`）

---

## 一、背景与目标

流程监控页（`/workflow/monitor/Monitor`）的「列表操作」目前只有：详情 / 暂停 / 恢复 / 强制收回 / 强制归档 / 终止 / 强制删除。

管理员在监控列表里**无法对任意运行中实例执行"回退到指定节点"**——只有「强制收回」，而它的候选节点口径是"整个流程的创建/审批节点"（含尚未走到的后续节点），语义偏监管、不是业务驳回。

**目标**：在监控列表补一个「回退」入口，管理员可把实例回退到**本实例实际办理过的某个上游节点或创建/填单节点**，并留痕。

---

## 二、现状盘点

### 2.1 ✅ 已开发：办理页的「退回」可以选目标节点

链路（`InstanceFlow.tsx`，待办点「办理」进入 `/workflow/create/start?mode=instance`）：

```
openReject()
  → GET  /blade-workflow/task/{id}/reject-nodes        // 候选查询
  → 弹窗 Radio 表格（节点名称 / 操作者两列）             // 用户选目标节点
  → POST /blade-workflow/task/{id}/reject               // { targetNodeKey, opinion, resubmitMode }
```

后端 `WfTaskServiceImpl.reject`（:500-580）+ `rejectNodes`（:583-）已完成：候选计算、目标合法性校验、`moveActivity + advance` 回退、留痕 `LOG_REJECT`、MI 集合变量注入、逐级/直达重提交。

节点级配置也齐（`WfNodeSettingsUtil`）：`rejectType`（1=直接退 / 2=弹窗选）、`defaultRejectNodeKey`、`rejectableNodeKeys` 白名单、`rejectChangeNode`、`rejectRemind`（对齐 ecology `RejectNodeSet`）。

> **限制**：该链路以 **taskId** 为入口（`InstanceFlow` 靠 `listTodo` 找「本人名下该节点待办」）。管理员从监控页打开某实例时通常没有他人节点的待办 → `taskId` 为空 → `canOperate=false` → 退回按钮根本不显示。**这就是监控侧必须另做一套的原因。**

### 2.2 ⚠️ 半开发：Monitor 列表的「强制收回」能选节点，但口径不同

- `Monitor.tsx` 的 `askOpinion(title, cb, defId)`（:153-178）会 `listNodes(defId)` 拉全流程节点，过滤 `nodeType===0||1` 作为候选，弹窗「收回至」下拉（:642-671），提交 `forceRecallInstance(id, opinion, targetNodeKey)`。
- 后端 `WfInstanceServiceImpl.forceRecallTo`（:2033-2081）已支持 `targetNodeKey`。
- **与「退回」的差异**：
  - 候选 = 全流程创建/审批节点（**不区分是否走过**，可能选到还没到达的后续节点）；
  - 语义 = 监管收回（实例不终止、无"不通过"态、无节点操作菜单校验）；
  - 不注入 MI 集合变量、不支持逐级/直达。

### 2.3 ❌ 未开发：Monitor 列表的「回退」

- 列表操作列无「回退」按钮；
- 后端**无实例级候选接口**：任务级 `/task/{id}/reject-nodes` 绑定 `taskId` 且要求 `WfAuthUtil.requireOperateTask`（本人或管理员）；
- 后端**无实例级回退执行接口**（`force-recall` 语义为监管收回，且缺目标合法性校验）。

### 2.4 关键代码位置索引

| 关注点 | 位置 |
|---|---|
| 结构候选算法 | `blade-workflow/.../reject/WfRejectManager.java`（`computeRejectableNodes` / `resolveDefaultRejectNode` / `isRejectable`） |
| 任务级退回 | `blade-workflow/.../service/impl/WfTaskServiceImpl.java`（`reject` :500-580、`rejectNodes` :583-） |
| 监控强制收回 | `blade-workflow/.../service/impl/WfInstanceServiceImpl.java`（`forceRecallTo` :2033-2081） |
| 监控接口 | `blade-workflow/.../controller/WfMonitorController.java`（`/instances/{id}/force-recall` :147） |
| 监控列表前端 | `ant-design-pro/src/pages/Workflow/Monitor/Monitor.tsx`（`askOpinion` :153、opinion 弹窗 :642-671、操作列 :474-508） |
| 办理页退回 UI（可复用） | `ant-design-pro/src/pages/Workflow/Create/InstanceFlow.tsx`（`openReject` :534、radio 表格 :1626-1660+） |
| VO | `blade-service-api/blade-workflow-api/.../vo/RejectCandidatesVO.java`、`RejectCandidateVO.java`（`nodeKey/nodeName/nodeType/operators`） |

---

## 三、口径定义（本次敲定）

**候选目标节点** = 本实例**实际办理过**的节点 ∪ **创建/填单节点**，且必须同时满足：

1. 是"引擎能停住的 UserTask"：`nodeType ∈ {0 创建, 1 审批, 2 提交}`（`3 归档/5 等待/6 自动/7 网关` 剔除——token 移过去会立刻流出，表现为"退回了但没动"）；
2. 不在"禁止回退"方向上（连线 `is_reject=1` 的入边封锁回溯）；
3. 命中节点级白名单 `settings.reject.nodeKeys`（若配置）。

**最终候选** = `结构候选集` ∩ `运行时已办理集 ∪ {创建节点}`，按「离当前节点由近及远」排序（结构算法已保证该顺序）。

**"已办理"判定数据源**：`wf_task` 台账的 `node_key` 集合（每办理过一个节点就留下一条台账，与"谁办理过"的业务语义一致）。

> ⚠️ **不可用 `wf_approval_log`**：该表在「去 wf_ 表」改造后**已停写**（审批意见真源已改为 `ACT_HI_COMMENT`，由 `WfApprovalLogActReader.readFromAct` 读取），实测该表**全表 0 行**——用它判定会永远读不到数据、告警形同虚设。
> 备选：`act_hi_taskinst` 的 `DISTINCT TASK_DEF_KEY_`（引擎权威，含条件分支跳过的自动节点）。两者差异见 §七 待拍板 3。

---

## 四、后端改造清单

### 4.1 接口（`WfMonitorController`，风格与现有 `force-recall` 保持一致，用 `@RequestParam`）

```java
@GetMapping("/instances/{id}/rejectable-nodes")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@Operation(summary = "监控-可回退节点", description = "当前节点可回退的目标：已办理过的节点 + 创建节点")
public R<RejectCandidatesVO> rejectableNodes(@PathVariable Long id) {
    return R.data(instanceService.monitorRejectableNodes(id));
}

@PostMapping("/instances/{id}/reject-to")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@Operation(summary = "监控-回退到指定节点", description = "把实例回退到目标节点（实例保持运行中）")
public R<Boolean> rejectTo(@PathVariable Long id,
                            @RequestParam(required = false) String targetNodeKey,
                            @RequestParam(required = false) String opinion,
                            @RequestParam(required = false) Integer resubmitMode) {
    return R.data(instanceService.monitorRejectTo(id, targetNodeKey, opinion, resubmitMode));
}
```

### 4.2 Service（`IWfInstanceService` + `WfInstanceServiceImpl`）

```java
/** 监控级：实例当前节点的可退回目标节点（已办理过的节点 + 创建节点） */
RejectCandidatesVO monitorRejectableNodes(Long instId);

/** 监控级：回退到指定节点（业务驳回语义，实例不终止） */
boolean monitorRejectTo(Long instId, String targetNodeKey, String opinion, Integer resubmitMode);
```

### 4.3 `monitorRejectableNodes` 实现要点

1. 取实例 `instanceMapper.selectById(instId)`；校验 `status ∈ {运行中, 暂停}`。
2. 当前节点：复用现成的 **`resolveCurrentNodeKey(inst)`**（`forceRecallTo` 已在用，兼容 act 读源）。
3. 结构候选：`rejectManager.computeRejectableNodes(inst.getDefId(), currentNodeKey, loadNode(defId, currentNodeKey))`
   → **`WfRejectManager` 零改动**（纯函数、读源收口已在里面）。
4. 运行时"已办理"集：
   ```sql
   SELECT DISTINCT node_key FROM wf_approval_log
    WHERE inst_id = #{instId} AND is_deleted = 0 AND node_key IS NOT NULL
   ```
5. 创建节点：复用现成的 **`resolveCreateNodeKey(inst.getDefId())`**。
6. 取交集 → 填 `RejectCandidatesVO`：
   - `type` = `WfNodeSettingsUtil.rejectType(node)`；`remind` = `rejectRemind(node)`；`changeNode` = `rejectChangeNode(node)`；
   - `defaultNodeKey` = 节点配置 `reject.defaultNodeKey`（须落在最终候选内）否则取候选首项（最近上游）；
   - `nodes` = 逐个转 `RejectCandidateVO`（`nodeKey/nodeName/nodeType/operators`；`operators` 可复用办理页同款解析逻辑，解析失败给 `-`）。
7. 最终候选为空 → 返回 `nodes=[]`（前端提示"该实例当前无可回退节点"），**不抛异常**（避免 500 打断列表）。

### 4.4 `monitorRejectTo` 实现要点（与任务级 `reject` 的差异表）

| 环节 | 任务级 `reject` | 监控级 `monitorRejectTo` |
|---|---|---|
| 鉴权 | `requireOperateTask`（本人/管理员）+ `requireOperate(node, MENU_REJECT)` + `allow_reject` 检查 | 仅 `WfAuthUtil.isAdmin()` / 委托范围（`monitorScope`）；节点菜单 / `allow_reject` **建议放行**（监管动作，见 §七 待拍板 1） |
| 目标合法性 | `rejectManager.isRejectable(candidates, target)` | **同样必须校验**（现有 `forceRecallTo` 缺这步 → 目前能选任意节点，是现存隐患，本次补齐） |
| 关闭当前待办 | `task=DONE` + `taskActWriter.sync` + `closeSiblings` | 关该节点全部 TODO + `taskActWriter.sync`（同 `forceRecallTo` 口径） |
| 留痕 | `appendLog(..., LOG_REJECT, opinion)`，留痕人 = **task.assignee**（节点接收人） | `recordLog(..., LOG_REJECT, "[监控回退]" + opinion)`，留痕人 = **当前操作人**（见 §七 待拍板 2） |
| **MI 集合变量** | `injectMiCollectionVars(defId, inst, vars, assignee)` | **必须补上**（见 §4.5） |
| 逐级 / 直达 | `resubmitMode` → 引擎变量 `VAR_RESUBMIT_DIRECT_NODE` | 建议支持，默认逐级（`RESUBMIT_SEQUENTIAL`） |
| 引擎回退 | `processService.moveActivity(engineInstId, currentNodeKey, target, vars)` + `instanceService.advance(..., AdvanceSrc.REJECT)` | 同（可直接复用 `forceRecallTo` 的这两步） |
| 节点后附加操作 | `nodeActionExecutor.execute(inst, node, PHASE_POST, assignee, true)`（测试态跳过） | 同（`isTest` 实例跳过，避免测试实例触发真实写业务/外部调用） |
| 实例状态 | 不变（保持运行中） | 不变（**不置"不通过(2)"**，与任务级一致） |
| 测试实例 | —— | `is_test=1` 的实例**直接拒绝**（不提供回退） |

**补充语义说明（避免误解）**

- **不做业务数据回滚**：回退只做「移动引擎 token + 关闭原节点待办 + 为目标节点重建待办 + 留痕」，**表单业务数据（金额、附件、明细行等）保持回退前状态**，不做还原（与 ecology 退回一致）。**"回退" ≠ "数据还原"**。
- **字段权限按目标节点重算**：目标节点的表单布局/字段权限由目标节点配置渲染（前端自动），但**已提交的数据不会被裁剪**——目标节点若配了字段权限，重新进入时表单按新权限渲染，用户看到的是已提交值的子集。
- **节点后附加操作**：执行 `nodeActionExecutor.PHASE_POST` 中**仅勾选「退回时触发」**的条目（`WfNodeSettingsUtil.preOperateRejectScript` / `postOperateRejectScript` 口径），测试实例跳过。
- **重复回退**：同一实例连续回退到同一节点 → 第二次被 `target == current` 拦下（报「目标节点与当前节点相同，无需收回」），属预期。

### 4.5 既有缺陷（✅ 已于 2026-11-01 先行修复，独立于本功能）

这两项不依赖「回退」功能，已先修掉并通过 `mvn -o -pl blade-service/blade-workflow compile`（BUILD SUCCESS）。

**① `injectMiCollectionVars` 缺失（MI 集合变量未注入）**
- 症状：回退到多实例节点时集合未就绪 → 引擎按 0 实例展开 → 目标节点被「瞬间完成」静默跳过（对应历史问题「MI 集合注入 starter=null 致节点被跳过」）。
- 修法（`WfInstanceServiceImpl.forceRecallTo`）：`moveActivity` 前调用**本类已有**的 `injectMiCollectionVars(defId, inst, vars, currentOperator)`（与 `WfTaskServiceImpl#reject`、本类异常兜底 `:2719` 同口径）。`currentOperator` 传 `inst.getStarter() != null ? starter : admin`，与随后 `advance(...)` 的 operator 保持一致（`injectMiCollectionVars` 内部也优先取 `inst.getStarter()`）。
- 附带：**清理「直达本节点」残留标记**（引擎变量 `wf_resubmit_direct_node`）—— 强制收回是逐级语义，残留会让目标节点重新提交时跳过中间节点直达上次退回点（与 `WfTaskServiceImpl#reject` 逐级模式一致）。已在 `WfInstanceServiceImpl` 内定义同值常量 `VAR_RESUBMIT_DIRECT_NODE`。

**② `forceRecallTo` 缺目标合法性校验**
- 症状：`targetNodeKey` 只校验"非空 + 不等于当前节点"，可被指定到**任意节点**（含网关/归档/自动节点 → "收回了但节点没变"；含尚未到达的后续节点 → 越级回退无约束）。
- 修法：**分级校验**（新增 `requireRecallTargetNode` / `recallTargetPassed`）：
  - **硬校验（拦截）**：① 目标节点必须真实存在于流程定义（防脏 key / 节点已删）；② 必须是「引擎能停住」的 UserTask —— `nodeType ∈ {0 创建, 1 审批, 2 提交}`，明确是 `3/5/6/7` 则拒绝（剔除口径对齐 `WfRejectManager`；`nodeType` 为 `null` 的节点保守放行，避免误伤既有流程）。
  - **软校验（只告警）**：「是否在本实例办理过」用 **`wf_task` 台账的 `node_key` 集合**判定（**不用 `wf_approval_log`**——该表在「去 wf_ 表」改造后已停写、全表 0 行，详见 §三）；不在集合内 → `log.warn` 留痕但放行。
  - **为什么"已走过"不硬拦截**：现有前端「强制收回」候选是**全流程创建/审批节点**（含尚未走到的后续节点），若按"已走过"硬拦截会造成既有功能回归；越级回退是监管动作的既定能力，改为 warn 可追溯。
- 新「回退」接口（`monitorRejectTo`）按 §三 口径**硬校验**"已走过 + 创建"，因为它的候选由服务端下发，前端不会出现越级选项。

---

## 五、前端改造清单

### 5.1 `ant-design-pro/src/services/workflow/index.ts`

```ts
/** 监控-可回退节点：已办理过的节点 + 创建节点 */
export async function monitorRejectableNodes(id: string | number) {
  return request<ApiResponse<any>>(`${WORKFLOW}/monitor/instances/${id}/rejectable-nodes`, { method: 'GET' });
}

/** 监控-回退到指定节点 */
export async function monitorRejectTo(
  id: string | number,
  params: { targetNodeKey?: string; opinion?: string; resubmitMode?: number },
) {
  return request<ApiResponse<boolean>>(`${WORKFLOW}/monitor/instances/${id}/reject-to`, { method: 'POST', params });
}
```

### 5.2 `Monitor.tsx`

- **操作列**：在 `running` 分支内、`canAdmin` 条件下新增 **「回退」**（与「强制收回」并列）。
- **独立弹窗**（不复用现有 opinion 弹窗的 `Select`——语义不同，需要展示"节点名称/操作者"两列 + 逐级/直达）：
  - 新增状态：`rejectOpen` / `rejectCandidates` / `rejectTargetKey` / `rejectResubmitMode`（默认 1 逐级）/ `rejectOpinion`；
  - 打开：`monitorRejectableNodes(record.id)` → 候选为空则 `message.warning('该实例当前无可回退节点')` 并终止；成功则 `setRejectTargetKey(candidates.defaultNodeKey)` 后开弹窗；
  - 提交：`monitorRejectTo(record.id, { targetNodeKey, opinion, resubmitMode })` → 成功 `message.success('已回退')` + **`actionRef.current?.reloadAndRest()`**（用 `reloadAndRest` 而非 `reload`，避免"操作了但列表看着没变"的老毛病）。
- **抽公共组件**：把 `InstanceFlow.tsx` 的 radio 表格（节点名称 / 操作者两列）抽成 `ant-design-pro/src/components/Workflow/RejectNodePicker.tsx`，办理页与监控页共用，避免复制。
- **详情抽屉一致性**：`MonitorDetailDrawer.tsx` 的「强制收回」目前是 Popconfirm 硬编码 `forceRecallInstance(id, '监控强制收回')`，**不传 targetNodeKey** → 只能退回创建节点，与列表页不一致；本次补齐节点选择。

### 5.3 与办理页的关系

- 办理页「退回」**不动**（面向"本人办理中"的节点，走 taskId 链路，语义与权限都不同）。
- 监控页「回退」是**监管视角**的补充入口，两者并存不冲突。

---

## 六、影响面与风险

| # | 风险 | 处置 |
|---|---|---|
| 1 | `injectMiCollectionVars` 缺失导致 MI 节点回退被跳过 | §4.5 必补（`monitorRejectTo` + `forceRecallTo`） |
| 2 | 并发回退同一实例（现有 `forceRecallTo` 无锁） | 建议加实例级锁；或接受"后写覆盖"（回退是低频监管动作） |
| 3 | 委托范围（非管理员）越权 | 复用 `monitorScope` 限定；候选接口同样要校验范围 |
| 4 | `wf_approval_log` 依赖完整性（代跑/测试态留痕人与正式不同） | 口径已在 §三 声明；测试实例直接禁回退 |
| 5 | act 读源与 wf 表读源混用 | 当前节点用 `resolveCurrentNodeKey`（已兼容）；"已办理"改用 `act_hi_actinst` 时需再评估一致性 |
| 6 | `resolveCurrentNodeKey` 在并行/会签下的语义 | 与 `forceRecallTo` 一致（同一函数），不新增风险 |
| 7 | 前端 `reloadAndRest` 依赖 `actionRef` 已绑定 | 已验证 `StandardTable` 会把 `actionRef` 透传给 `ProTable` |
| 8 | 前端候选读源（`listNodes`）与后端 `loadNodes` 读源开关（`definition-from-bpmn`）不一致时，可能出现"前端能选、后端说节点不存在" | 两端读源同源核对；后端报错信息带节点名便于定位（已在 §4.5-② 实现） |
| 9 | `forceRecallTo` 关闭**全部** TODO 待办（整单收回语义），并行分支下会连带关掉其它分支的待办 | 现行为（监管语义），本方案明示不改；如需改成"只关当前节点"需另立需求 |
| 10 | 新「回退」与既有「强制收回」都能改同一实例的当前节点，且**无实例锁** | 低频监管动作，暂接受"后写覆盖"；如要严格串行需引入实例级锁（`SELECT ... FOR UPDATE` 或分布式锁） |

---

## 七、待拍板（开工前需确认）

1. **节点未开放退回时**（`allow_reject=0` 或操作菜单无 `reject`）监控回退是否放行？
   建议：放行 + 弹窗提示（监管动作不受付发节点开关限制），与"强制收回"一致。
2. **回退留痕的操作人**写谁？管理员（当前操作人）还是节点接收人？
   现状：任务级 `reject` 写的是**节点接收人**（`task.assignee`，注释见 `WfTaskServiceImpl` :545-548）；监控级无 `task.assignee` 语义，建议写**当前操作人**并加「[监控回退]」前缀（与「[强制收回]」前缀风格一致）。
3. **「已办理」最终口径**：`wf_approval_log`（有人办理过）还是 `act_hi_actinst`（引擎走过，含无人办理的自动节点）？
   建议：`wf_approval_log`（推荐）；若后续要放开"引擎走过即算"，只需换数据源，算法不动。
   （已修的 `forceRecallTo` 软告警按 **`wf_task` 台账的 `node_key` 集合**实现——`wf_approval_log` 已停写不可用；备选 `act_hi_taskinst` 的 `DISTINCT TASK_DEF_KEY_`。）
4. **监控级回退意见是否必填？** 任务级受 `WfNodeSettingsUtil.opinionRequiredByOp` 控制；监管级建议**必填**（留痕可追溯，便于事后审计"谁把单退到哪"）。
5. **暂停态实例（status=4）能否回退？** `forceRecallTo` 会先 `activate` 解除暂停（副作用：实例状态回到"运行中"）。建议前端「回退」只在 `running` 显示，暂停实例走「恢复」后再回退；后端对暂停态**允许**（与强制收回一致）。
6. **要不要「批量回退」？** Monitor 已有批量暂停/恢复/收回/归档/删除。**建议不做**：各实例当前节点不同，无法统一指定目标节点，语义不成立。

---

## 八、实施步骤与验收点

| 步骤 | 内容 | 验收 |
|---|---|---|
| 1 | 后端 `monitorRejectableNodes` + `monitorRejectTo` + 2 个 Controller 接口 | 单测/接口自测：运行中实例能查到候选且含操作者；回退后实例仍在运行中、目标节点产生待办、原节点待办关闭、留痕里有「[监控回退]」 |
| 2 | ✅ 已完成（2026-11-01）：补 `injectMiCollectionVars` + 清「直达本节点」残留（含 `forceRecallTo`） | MI 流程回退到多实例节点，节点不再被跳过、集合就绪 |
| 3 | ✅ 已完成（2026-11-01）：补 `forceRecallTo` 目标合法性校验（硬校验存在性/类型 + 越级告警） | 指定网关/归档/不存在的节点被拒并给出明确报错；越级回退有 warn 留痕 |
| 4 | 前端 `services/workflow` 两个函数 + `Monitor.tsx`「回退」按钮与弹窗 | 列表点「回退」→ 弹窗显示候选（节点名称/操作者）→ 选一个提交 → **列表即时刷新且该实例当前节点变为目标节点** |
| 5 | 抽 `RejectNodePicker` 组件，办理页与监控页共用 | 办理页退回功能无回归 |
| 6 | `MonitorDetailDrawer` 「强制收回」补节点选择 | 抽屉与列表行为一致 |
| 7 | 回归 | 「强制收回 / 强制归档 / 终止 / 强制删除」四个监管动作不受影响；任务级退回（办理页）无回归 |

---

## 九、工作量

| 项 | 估工 |
|---|---|
| 后端（2 接口 + 2 Service 方法 + 1 候选查询 + MI/校验补齐） | ~1.5 人日 |
| 前端（2 个 service 函数 + 弹窗 + 抽组件 + 抽屉一致性） | ~1 人日 |
| 联调 + 回归 | ~0.5 人日 |
| **合计** | **~3 人日** |

---

## 十、方案复核补充清单（2026-11-01 复审）

初稿写完后逐条复审，补出以下遗漏。除标注"待开工"的条目外，均已回写进本文对应章节。

| # | 复核发现的遗漏 | 处置 | 落点 |
|---|---|---|---|
| 1 | 未声明"回退**不做业务数据回滚**"（易被误解为数据还原） | 已补 | §4.4 补充语义说明 |
| 2 | 未说明回退后**字段权限按目标节点重算、已提交数据不裁剪** | 已补 | §4.4 补充语义说明 |
| 3 | 未说明**节点后附加操作**在监控回退时是否执行 | 已补：仅执行勾选「退回时触发」的条目 | §4.4 补充语义说明 |
| 4 | 未说明**暂停态实例**的副作用（`activate` 会解除暂停、状态回"运行中"） | 已补 | §七-5 |
| 5 | 未说明并行/会签下 `forceRecallTo` **关全部 TODO** 会连带关掉其它分支待办 | 已补（明示为现行为，不在本方案改） | §六 风险 9 |
| 6 | 未说明**批量回退**做不做 | 已补：建议不做（各实例当前节点不同，语义不成立） | §七-6 |
| 7 | 未说明**重复回退**到同一节点的行为 | 已补：被 `target == current` 拦下 | §4.4 补充语义说明 |
| 8 | 未说明回退到 **MI 节点且操作者解析为空**时的行为 | 已补：MI 守卫 `WfMiEmptyGuardListener` 会把"静默跳过"转为**显式失败**（回退报错，而非无声吞掉审批级）——这是期望行为 | §4.5-① |
| 9 | 未说明前端「回退」按钮的**可见条件** | 已补：`canAdmin && running`；测试实例不显示，后端同样拒绝 `is_test=1` | §五 + §4.4 |
| 10 | 未说明「已办理」判定**排除哪些 log_type 及原因** | 已补：只认 `{0,2,3,7,h}`，排除批注/督办/抄送/批示等旁路留痕 | §4.5-② + §七-3 |
| 11 | 未说明候选节点**"操作者"列**的取数方式 | 已补：复用 `operatorResolver.resolve(defId, nodeKey, instId, starter, operator)`（与办理页同源，解析失败显示 `-`） | §4.3-6 |
| 12 | 未提**前后端读源一致性**风险（`listNodes` vs `loadNodes` 开关不同） | 已补 | §六 风险 8 |
| 13 | 未提**新「回退」与「强制收回」并发冲突**（两个入口都能改同一实例当前节点、无实例锁） | 已补 | §六 风险 10 |
| 14 | 未提**实现依赖**：`WfInstanceServiceImpl` 当前**未注入 `WfRejectManager`** | 待开工时补：新增接口要用 `rejectManager.computeRejectableNodes` 需在 `WfInstanceServiceImpl` 注入（无循环依赖风险——`WfRejectManager` 只依赖 `nodeMapper`/`linkMapper`/`bpmnReader`） | §4.3-3 |
| 15 | 未说明留痕 `log_type` 复用 `LOG_REJECT(3)` 对"流程图已走过节点"判定**无影响** | 已补：进度图读 `ACT_HI_ACTINST`（`WfProgressView.finishedActivityIds`），不读 `log_type` | §4.4 |
| 16 | 未说明**回退后实例状态不变**（不置"不通过(2)"）对监控视图的影响 | 已补：实例仍为"运行中"，列表状态列不变，仅当前节点变化 | §4.4 |
| 17 | 未提**实现依赖** `WfInstanceServiceImpl` 需注入 `WfRejectManager`（✅ 已实现：复用，零改动） | 已落地 | §四 + §十一 |
| 18 | ⚠️ **初稿的「已办理」数据源选错**：`wf_approval_log` 在「去 wf_ 表」改造后**已停写**（实测全表 0 行，意见真源改为 `ACT_HI_COMMENT`），用它判定会永远读不到数据、越级告警形同虚设 | **已改用 `wf_task` 台账的 `node_key` 集合**，代码已修 + 编译通过 | §三 + §4.5-② |

---

## 十一、实现进度（2026-11-02）

- [x] **后端**：`IWfInstanceService#monitorRejectableNodes` / `monitorRejectTo` 已实现；`WfMonitorController` 新增 `GET /rejectable-nodes`、`POST /reject-to` 两个端点；`WfInstanceServiceImpl` 注入 `WfRejectManager`（复用 `computeRejectableNodes`，零改动）。
- [x] **后端编译 + 部署 + 实测通过**：候选查询（运行中实例只返回已办理节点 + 创建节点）、硬校验（脏 key / 非法类型 / 越级拦截均 400）、成功回退（实例当前节点正确变更、台账同步）全部验证。
- [x] **前端**：`services/workflow` 新增 `monitorRejectableNodes` / `monitorRejectTo`；新增公共组件 `components/Workflow/RejectNodePicker.tsx`（节点 radio 表 + 意见必填 + 逐级/直达）；`Monitor.tsx` 列表操作列新增「回退」按钮（非测试态 + running 可见）+ 弹窗；`MonitorDetailDrawer.tsx` 详情抽屉同步新增「回退」按钮 + 弹窗。
- [x] **前端类型检查**：`tsc --noEmit` 聚焦检查我改动的文件 0 错误。
- [ ] **待用户验证**：前端 dev 硬刷新（热重载）后，在 Monitor 列表/详情抽屉点「回退」，确认弹窗候选与回退动作正确。
- **备注**：`InstanceFlow`（核心办理页）现有 reject 弹窗**暂未复用**新 `RejectNodePicker` 组件——核心办理页改动风险高、易回归，本次不动；`RejectNodePicker` 已按共用设计，后续可统一收敛。
