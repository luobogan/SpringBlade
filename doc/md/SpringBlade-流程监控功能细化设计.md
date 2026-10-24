# SpringBlade「流程监控」功能细化设计

> 版本：v1.0（细化版）｜生成日期：2026-10-22
> 背景：基于「泛微 E9 流程监控实现分析」+「SpringBlade 工作流现状盘点（已核实代码）」落地。
> 约束：① 使用新版 ant-design-pro 前端；② **不迁移 E9 监控配置数据**（权限模型从零设计）；③ 尽量复用既有能力。

---

## 0. 背景与现状（已核实）

### 0.1 参考：E9 流程监控实现
- 后端：`weaver/workflow/monitor/Monitor.java`（78KB）、`com/engine/workflow/web/RequestMonitorListAction.java`；权限三表 `workflow_monitor_info`(监控对象+数据范围) / `workflow_monitor_detail`(逐流程细粒度权限位) / `WORKFLOW_MONITOR_DT_WFID`(预计算速查表)；管理员须显式配置总部范围才看全部。
- 前端：旧 JSP `WorkflowMonitorList.jsp`（SQL 分页）+ 新 SPA `src4js/pc4mobx/workflow/stores/monitor*.js`（统一调 `/api/workflow/monitor/*`）。
- 动作：删除 / 强制归档 / 暂停 / 启用 / 撤销 / 强制收回 / 流程干预；转发/督办/流转图复用通用请求页。

### 0.2 SpringBlade 现状（决定方案可行性）
- 实例表 `wf_instance`（`WfInstance.java`）：完整字段 `id/engineInstId/defId/formId/dataId/title/bizKey/currentNodeKey/starter/startTime/endTime/urgency/parentId/isTest`，状态常量 `RUNNING=0 / APPROVED=1 / REJECTED=2 / CANCELED=3 / SUSPENDED=4 / DRAFT=5`，额外含 **L3 自检三列** `businessRowReady / requestIdBound / engineDeploymentMatched` 与 `procDefId / pendingStatus`。
- 配套表：`wf_task`、`wf_process_definition`、`wf_approval_log`（`log_type`：0批准/2提交/3退回/7转发/h转办/s督办/t抄送/y批示/9批注）。
- 控制器：`WfInstanceController(/instance)`（含 `/mine`、`/{id}`、`/{id}/logs`、`/{id}/progress`、`/{id}/snapshot/{nodeKey}`）、`WfTaskController(/task)`（含 `/todo`、`/done`、`/forward`、`/urge`）、`WfDefinitionController(/definition)`（`/list`）、`WfMonitorController(/monitor)`（仅 `/count`、`/health/*`、`/ledger-shadow`）。
- 权限：`WorkflowConstant.HAS_ROLE_WORKFLOW = "hasRole('workflow') or hasRole('administrator')"`；`WfAuthUtil.isAdmin()` + `WfInstanceServiceImpl.canVisible()` 已让管理员可见全部实例。**无独立「监控员」角色**。
- 已有动作（服务层）：`withdraw`(→CANCELED, `requireStarterOrAdmin`)、`stop`(suspendProcessInstanceById→SUSPENDED)、`resume`(activateProcessInstanceById)、`cancel`(→CANCELED, `requireStarterOrAdmin`)、`forward`(转办)、`urge`(催办/督办)。
- **缺口**：① 管理员视角的全量实例**列表/检索**端点（无）；② **强制收回(收回重填)**、**强制归档(强制办结)**、**强制删除实例** 显式端点（无）；③ **批量**动作（无）；④ 前端监控页面/菜单/接口（无）。

---

## 1. 功能范围

### 1.1 MVP（必做）
1. **列表检索**：流程管理员/超管看**全租户**实例；按 流程定义 / 发起人 / 当前节点 / 状态 / 时间范围 / 是否超时 / 紧急度 / 测试态 / 标题 筛选；服务端分页。
2. **详情只读查看**：复用 `InstanceFlow`——表单 + 流程图 + 流转记录时间线。
3. **单条监管动作**：强制收回、强制归档(办结)、暂停、恢复、终止(撤销)、强制删除、转办、催办。
4. **批量监管动作**：批量暂停 / 恢复 / 归档 / 删除（二次确认）。
5. **异常/超时监控**（SB 独有）：超时(`wf_task.dueTime`)、L3 自检异常(`businessRowReady/requestIdBound/engineDeploymentMatched`)、引擎对账差异(`/monitor/ledger-shadow`)。

### 1.2 明确「不做」（区别于 E9）
- **不迁移** E9 的 `workflow_monitor_info/detail` + 预计算 `DT_WFID` 三表。
- **不做** E9 式「按分部/部门委托授权给任意经理看本部门流程」的精细化模型（SB 监控定位为**运维级全租户可见**）。

### 1.3 可选增强（未来）
- 部门级委托监控：新增 `wf_monitor_scope`(def_id + 组织范围) 配置表 + 配置页，届时按 E9 思路做数据范围与预计算表。

---

## 2. 权限模型（细化）

| 层级 | 设计 |
|---|---|
| 角色门 | 复用 `workflow` / `administrator`（`HAS_ROLE_WORKFLOW`）。监控页与所有监管动作为**管理动作**，角色门统一用 `HAS_ROLE_WORKFLOW`。 |
| 菜单权限码 | 新增 `workflow_monitor`（前端 `usePageButtons('workflow_monitor')` 守门，按钮用 `PermissionButton`）。 |
| 记录级兜底 | `canVisible()`（发起人/参与人/管理员）+ `requireStarterOrAdmin()` 已存在；监管动作调用前仍走记录级校验，避免越权。 |
| 数据范围 | 实体继承 `TenantEntity`，`tenant_id` 自动过滤；监控看**全租户**（不按分部/部门切）。 |
| 独立监控员角色 | **不做**（与流程管理员合并，避免 E9 的过度精细化）。 |

---

## 3. 数据模型（细化：复用为主，不新增业务表）

### 3.1 列表主数据源 = `wf_instance`
监控列表直查 `wf_instance`（已含全部列表所需字段），**不碰 `act_*`**，规避双读复杂度。

| 监控列 | 来源字段 | 说明 |
|---|---|---|
| 实例ID | `id` | 雪花ID，字符串化(`ToStringSerializer`) |
| 流程定义 | `defId` → join `wf_process_definition` 取 `name` | 定义名需关联查询 |
| 标题 | `title` | 关键词模糊 |
| 发起人 | `starter` → `blade_user` 取姓名 | 前端 `useUserNames` 解析或后端 enrich |
| 当前节点 | `currentNodeKey` | 节点名需结合定义 BPMN |
| 状态 | `status` | 0/1/2/3/4/5 |
| 紧急度 | `urgency` | 0/1/2 |
| 发起/结束时间 | `startTime`/`endTime` | 范围筛选 + 排序 |
| 测试态 | `isTest` | 可过滤/单独清理 |
| 异常标记 | `businessRowReady`/`requestIdBound`/`engineDeploymentMatched` | **SB 独有**，见 §6 |

### 3.2 超时维度 = `wf_task`
- 条件：`status = 0(TODO)` 且 `dueTime IS NOT NULL` 且 `dueTime < now()` 且实例仍运行(`wf_instance.status=0`)。
- 列表 `overdue=true` 过滤时，对 `wf_instance` 做 `EXISTS (SELECT 1 FROM wf_task ...)` 子查。

### 3.3 异常维度（SB 独有，E9 无）
- `businessRowReady = 0`：业务表无对应行（流程能跑但查不到单据）。
- `requestIdBound = 0`：业务行 `request_id` 未回填（流程↔单据未闭环）。
- `engineDeploymentMatched = 0`：引擎 latest 部署被测试/手工部署顶替（灰度漂移）。
- 注意：`NULL` 视为「未自检（存量实例）」，**不报异常**。
- 引擎对账差异：复用 `WfMonitorController.ledger-shadow`（内存差异计数，重启清零）。

### 3.4 可选 `wf_monitor_scope`（未来，MVP 不建）
`id / tenant_id / def_id / scope_type(1分部/2部门/3人员) / scope_value / monitor_user_ids`。

---

## 4. 后端接口设计（细化：落到方法）

### 4.1 Controller 归属
**并入 `WfMonitorController`**（`@RequestMapping("/monitor")`），与现有 `/count`、`/health/*`、`/ledger-shadow` 同源；新增实例监控端点统一加 `@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)`。

### 4.2 列表 / 检索
```
GET  /monitor/instances
入参(Long/Query): current, pageSize,
   defId, defName, starter, currentNodeKey,
   statuses(多选, 0/1/2/3/4/5), startTime, endTime,
   overdue(Boolean), urgent(Integer), keyword, isTest(Integer),
   anomalyType(业务行缺失/未回填/部署漂移/超时 多选)
出参: R<IPage<MonitorInstanceVO>>
```
- 实现：`WfInstanceServiceImpl.monitorList(IPage<MonitorInstanceVO> page, MonitorInstanceQuery q)`
  - `wf_instance` lambdaQuery + `tenant_id` 过滤；`defId/defName` join `wf_process_definition`；`starter/title/currentNodeKey` 等值/模糊；`statuses in`；时间范围；`isTest`；`urgent`。
  - `overdue`：`EXISTS` 子查 `wf_task`(TODO + dueTime<now)。
  - `anomalyType`：按 L3 三列 + 超时分别加 `eq`。
  - 排序：`startTime desc`。
- `MonitorInstanceVO`：投影 `wf_instance` + `defName`(join) + 派生 `overdue`/`anomalyTags`；`starterName` 由前端 `useUserNames` 解析（或后端 enrich）。

### 4.3 详情（复用，不新增端点）
| 用途 | 端点 | 说明 |
|---|---|---|
| 实例详情 | `GET /instance/{id}` | 返回 `InstanceVO` |
| 流转记录 | `GET /instance/{id}/logs` | `ApprovalLogVO` 时间线 |
| 流转图 | `GET /instance/{id}/progress` | `WfProgressView`(bpmnXml + 四色 activityId) |
| 表单快照 | `GET /instance/{id}/snapshot/{nodeKey}` | 节点表单数据 |

> 监控详情**零新增**，直接复用上述 `/instance/*` 端点（管理员经 `canVisible` 可见任意实例）。

### 4.4 监管动作（细化：逐动作语义 + 引擎操作 + 状态流转）

| 动作 | E9 语义 | SB 落地 | 端点 | 实现要点 |
|---|---|---|---|---|
| **强制收回** | 退回发起人重填（保留业务数据） | **新增** `forceRecall` | `POST /monitor/instances/{id}/force-recall` | 与现有 `withdraw`(→CANCELED 撤销) **区分**：收回不删业务数据，把实例重置回 `DRAFT` 态（或退到首节点）让发起人重办；引擎侧 `suspendProcessInstanceById` + 标记；写 `wf_approval_log`(log_type=退回/收回)。 |
| **强制归档(办结)** | 强制结束为「通过/归档」 | **新增** `forceArchive` | `POST /monitor/instances/{id}/force-archive` | 置 `wf_instance.status=APPROVED(1)`、`endTime=now`；引擎 `runtimeService.deleteProcessInstance(id,"强制归档")`；写 `wf_approval_log`(办结)。注意与 BPMN `archiveEnd` 终态语义对齐。 |
| 暂停 | 挂起 | 复用 `stop` | `POST /instance/{id}/stop` | `runtimeService.suspendProcessInstanceById` → `SUSPENDED(4)`。 |
| 恢复 | 取消挂起 | 复用 `resume` | `POST /instance/{id}/resume` | `activateProcessInstanceById` → `RUNNING(0)`。 |
| 终止/撤销 | 作废 | 复用 `cancel` | `POST /instance/{id}/cancel` | `terminate` → `CANCELED(3)`（`requireStarterOrAdmin` 已含 admin）。 |
| **强制删除** | 物理清理 | **新增** `forceDelete` | `DELETE /monitor/instances/{id}` | 删 `wf_instance` + 关联 `wf_task` + `wf_approval_log` + 引擎 `runtimeService.deleteProcessInstance`；记系统审计日志。**破坏性操作，必须 workflow 角色 + 二次确认**。 |
| 转办/改派 | 改当前节点办理人 | 复用 `forward` | `POST /task/{id}/forward` | 任务级改派，管理员可操作当前节点。 |
| 催办/督办 | 提醒当前办理人 | 复用 `urge` | `POST /task/{id}/urge` | 写 `wf_approval_log`(log_type=s 督办)。 |

- **新方法伪实现**：
  - `forceRecall(Long instId)`：`requireStarterOrAdmin` 或仅 admin；`runtimeService.suspendProcessInstanceById(engineInstId)`；`inst.setStatus(DRAFT)`；`inst.setCurrentNodeKey(firstNode)`；写审批日志；返回。
  - `forceArchive(Long instId)`：`requireStarterOrAdmin`；`inst.setStatus(APPROVED)`；`inst.setEndTime(now)`；`runtimeService.deleteProcessInstance(engineInstId,"强制归档")`；写审批日志。
  - `forceDelete(Long instId)`：admin only；`taskMapper.deleteByInstId`；`approvalLogMapper.deleteByInstId`；`runtimeService.deleteProcessInstance(engineInstId)`；`instanceMapper.deleteById`；系统审计。

### 4.5 批量端点（新增）
```
POST /monitor/instances/batch-stop     { ids: [] }   → 循环 stop
POST /monitor/instances/batch-resume   { ids: [] }   → 循环 resume
POST /monitor/instances/batch-archive  { ids: [] }   → 循环 forceArchive
POST /monitor/instances/batch-recall   { ids: [] }   → 循环 forceRecall
POST /monitor/instances/batch-delete   { ids: [] }   → 循环 forceDelete（事务内）
```

### 4.6 鉴权小结
- 列表/动作端点：`@PreAuth(HAS_ROLE_WORKFLOW)`。
- 记录级：`requireStarterOrAdmin` / `canVisible` 兜底。
- 强制删除/归档：`workflow` 角色（含 admin），前端 + 后端双重二次确认。

---

## 5. 前端设计（细化：ant-design-pro）

### 5.1 页面与菜单
- 新增 `src/pages/Workflow/Monitor/Monitor.tsx`（列表）+ `MonitorDetailDrawer.tsx`（只读详情）。
- 菜单：`blade_menu` 在「工作流」一级下加一行：`code=workflow_monitor`、`path=/workflow/monitor`、`is_open=1`；前端 `app.tsx` 经 `/menu/routes` 自动注册，**无需改静态路由**。
- 权限：`usePageButtons('workflow_monitor')` + `PermissionButton`。

### 5.2 列表（`StandardTable` + ProTable）
- **列**：标题(链接打开详情)、流程定义(defName)、发起人(starterName)、当前节点、状态(`valueEnum` + `Tag`)、紧急度、发起时间、超时(`Badge`)、异常标记、测试态、操作。
- **搜索表单**（`search`）：流程定义(defId 下拉，复用 `definition/list`)、发起人(用户下拉)、状态(多选)、时间范围(`RangePicker`)、是否超时(`Switch`)、紧急度、测试态、异常类型(多选)、标题关键词。
- **分页**：服务端分页，`listMonitorInstances` 返回 `{records,total}`。
- **批量**：`rowSelection` + `PermissionButton` 批量按钮（批量暂停/恢复/归档/删除），`Modal.confirm` 二次确认 → 调 batch 接口 → `reload()`。

### 5.3 详情抽屉（只读监控视图）
- 复用 `InstanceFlow` 组件（已含 表单预览 `ExcelPreview` / 流程图 `FlowDiagram` / 流转记录 `Timeline`），数据来自 `getLogs` / `getInstanceProgress` / `getBpmn`。
- 以 `mode='monitor'` 只读打开（隐藏普通办理按钮，仅展示 + 监管动作按钮区）。
- 监管动作按钮（强制收回/暂停/恢复/归档/删除/转办/催办）放抽屉底部，调对应接口。

### 5.4 接口封装（`src/services/workflow/index.ts` 新增 monitor 分组）
```ts
const WORKFLOW = '/api/blade-workflow';
listMonitorInstances: (params) => request(`${WORKFLOW}/monitor/instances`, { method:'GET', params }),
forceRecallInstance: (id) => request(`${WORKFLOW}/monitor/instances/${id}/force-recall`, { method:'POST' }),
forceArchiveInstance: (id) => request(`${WORKFLOW}/monitor/instances/${id}/force-archive`, { method:'POST' }),
deleteMonitorInstance: (id) => request(`${WORKFLOW}/monitor/instances/${id}`, { method:'DELETE' }),
batchStopInstances:   (ids) => request(`${WORKFLOW}/monitor/instances/batch-stop`,   { method:'POST', data:{ids} }),
batchResumeInstances: (ids) => request(`${WORKFLOW}/monitor/instances/batch-resume`, { method:'POST', data:{ids} }),
batchArchiveInstances:(ids) => request(`${WORKFLOW}/monitor/instances/batch-archive`,{ method:'POST', data:{ids} }),
batchRecallInstances: (ids) => request(`${WORKFLOW}/monitor/instances/batch-recall`, { method:'POST', data:{ids} }),
batchDeleteInstances: (ids) => request(`${WORKFLOW}/monitor/instances/batch-delete`, { method:'POST', data:{ids} }),
// 单条 暂停/恢复/终止/转办/催办 直接复用已有 stopInstance/resumeInstance/cancelInstance/forwardTask/urgeTask
```
- 19 位雪花 ID 已靠 `ToStringSerializer` 字符串化，前端无需额外处理。

---

## 6. 异常 / 超时监控（SB 独有，细化）

| 维度 | 判定 | 数据来源 | 监控页呈现 |
|---|---|---|---|
| 超时 | 运行中实例存在 `wf_task`(TODO, dueTime<now) | `wf_task` | 列表 `overdue` 过滤 + `Badge` |
| 业务行缺失 | `businessRowReady = 0` | `wf_instance` | 异常标记 |
| 单据未闭环 | `requestIdBound = 0` | `wf_instance` | 异常标记 |
| 部署漂移 | `engineDeploymentMatched = 0` | `wf_instance` | 异常标记 |
| 引擎对账差异 | `totalDiff > 0` | `/monitor/ledger-shadow` | 「异常」Tab 快照 |

- 监控页可加「异常」Tab：聚合上述维度，点击进入对应实例详情处理。
- 测试实例：`isTest=1` 提供「一键清理」入口（复用现有测试清理能力），避免污染正常监控数据。

---

## 7. 与 E9 实现的关键差异

| 维度 | 泛微 E9 | SpringBlade 方案 |
|---|---|---|
| 权限粒度 | 任意经理按分部/部门授权看流程（三表 + 预计算速查） | 运维级：流程管理员/超管看全租户；MVP 不引入委托授权 |
| 列表数据源 | `workflow_requestbase` + 预计算速查表 join | 直查 `wf_instance`（全字段），不碰 `act_*` |
| 监管动作 | delete/archiving/repossessed/intervention 命令类 | 复用 stop/resume/withdraw/cancel/forward/urge + 补 forceRecall/forceArchive/forceDelete/batch |
| 异常能力 | 无 | L3 自检三列 + `ledger-shadow` 引擎对账（原生独有） |
| 数据 | 需迁移 monitor 配置 | **不迁移**，从零设计 |
| 前端 | 旧 JSP + 新 SPA | 统一 ant-design-pro `StandardTable` + 复用 `InstanceFlow` |

---

## 8. 实施步骤（细化）

1. **后端**
   - `WfInstanceServiceImpl` 新增 `monitorList()`、`forceRecall()`、`forceArchive()`、`forceDelete()`、batch 五件套。
   - `WfMonitorController` 扩充 `/monitor/instances`、`/monitor/instances/{id}/*`、`/monitor/instances/batch-*`（`HAS_ROLE_WORKFLOW`）。
   - 新增 `MonitorInstanceQuery`、`MonitorInstanceVO`（或在 `blade-workflow-api` 补 DTO）。
2. **前端**
   - `src/services/workflow/index.ts` 加 monitor 分组。
   - 新增 `src/pages/Workflow/Monitor/Monitor.tsx` + `MonitorDetailDrawer.tsx`（复用 `InstanceFlow`/`FlowDiagram`/`getLogs`）。
   - `blade_menu` 加 `workflow_monitor` 菜单行。
3. **验证**
   - 起若干实例（含 1 个超时、1 个 L3 异常、1 个测试态）。
   - 监控页：筛选/分页/异常标记正确；强制收回（回 DRAFT）、强制归档（→APPROVED）、强制删除（库+引擎清干净）、批量动作生效。
   - 详情：流转图 + 流转记录时间线正常。
   - 用 `/monitor/ledger-shadow` 校验无孤儿数据（`totalDiff=0`）。
4. **可选**：异常 Tab 聚合展示。

---

## 9. 风险与注意

- **破坏性操作审计**：强制归档/删除必须 `workflow` 角色 + 前端 `Modal.confirm` + 后端记录级校验 + 写 `wf_approval_log` 与系统日志。
- **删实例联动清理**：`wf_task`/`wf_approval_log`/引擎运行时必须一并清理，避免孤儿数据（用 `ledger-shadow` 校验）。
- **强制收回 vs 撤销语义**：`forceRecall` 回 `DRAFT` 保留业务数据；`withdraw/cancel` 是 `CANCELED` 终结。二者不可混淆，避免误删业务单据。
- **超时依赖**：「超时」依赖 `wf_task.dueTime` 是否配置；流程未设时限则超时列无数据，需先明确产品定义。
- **雪花 ID**：19 位字符串化已处理，跨前端/后端传参勿用 number。
- **租户隔离**：`tenant_id` 自动过滤，监控看全租户；若未来做部门级委托，须显式加 scope 过滤避免越租户。

---

## 10. 接口契约汇总（快速查阅）

| 方法 | 路径 | 新增/复用 | 鉴权 |
|---|---|---|---|
| GET | `/api/blade-workflow/monitor/instances` | 新增 | HAS_ROLE_WORKFLOW |
| GET | `/api/blade-workflow/instance/{id}` | 复用 | canVisible |
| GET | `/api/blade-workflow/instance/{id}/logs` | 复用 | canVisible |
| GET | `/api/blade-workflow/instance/{id}/progress` | 复用 | canVisible |
| GET | `/api/blade-workflow/instance/{id}/snapshot/{nodeKey}` | 复用 | canVisible |
| POST | `/api/blade-workflow/monitor/instances/{id}/force-recall` | 新增 | HAS_ROLE_WORKFLOW |
| POST | `/api/blade-workflow/monitor/instances/{id}/force-archive` | 新增 | HAS_ROLE_WORKFLOW |
| DELETE | `/api/blade-workflow/monitor/instances/{id}` | 新增 | HAS_ROLE_WORKFLOW |
| POST | `/api/blade-workflow/monitor/instances/batch-{stop\|resume\|archive\|recall\|delete}` | 新增 | HAS_ROLE_WORKFLOW |
| POST | `/api/blade-workflow/instance/{id}/stop` | 复用 | HAS_ROLE_WORKFLOW |
| POST | `/api/blade-workflow/instance/{id}/resume` | 复用 | HAS_ROLE_WORKFLOW |
| POST | `/api/blade-workflow/instance/{id}/cancel` | 复用 | HAS_ROLE_WORKFLOW |
| POST | `/api/blade-workflow/task/{id}/forward` | 复用 | HAS_ROLE_WORKFLOW |
| POST | `/api/blade-workflow/task/{id}/urge` | 复用 | HAS_ROLE_WORKFLOW |

---

## 11. 深度展开：监管动作的引擎语义与风险（方案C 已核实）

> 本章所有结论均来自实测代码，非推测。

### 11.1 必须先理解：终态由引擎事件反写，不是业务直写
- `WfWriteHelper.terminate:129-153`：`eventDriven` 时**先**写 `wf_instance.pending_status`(intent)，**再** `processService.deleteProcessInstance(...)` 触发 `PROCESS_CANCELLED`；`WfStateProjector` 监听后读 intent 写终态并 `closeResidualTodos`。
- `WfStateProjector.resolveCancelTarget:240-248` 已原生支持 intent ∈ `{1通过,2不通过,3撤销}`，空则默认 3。
- **推论**：`forceArchive/forceRecall/forceDelete` 绝不能「手写 status + 调 deleteProcessInstance」，否则与投影器竞态。必须走 `writeHelper.terminate` 通道。

### 11.2 forceArchive（强制归档/办结）—— 几乎零新代码
直接调 `writeHelper.terminate(inst, WfInstance.STATUS_APPROVED, opinion, "强制归档")`。intent=1 投影器已支持 → 置 APPROVED(1)+endTime+关待办，与 E9 `WfForceOver.doForceOver` 等价。**修正 §4.4 原「手动 set APPROVED」写法。**

### 11.3 forceRecall（强制收回）—— 两种语义，必须二选一
- **方案 A（同实例回退首节点，理想但高风险）**：Flowable `ProcessInstanceModificationBuilder` 取消当前活动并 `startBeforeActivity(firstNode)`，**保留同一实例ID**。风险：全代码库零使用此 API；依赖「首节点是发起人可编辑 userTask」建模约定；并行/网关节点 token 移动复杂。→ 未来增强。
- **方案 B（终止原实例 + 同 bizKey 重开草稿，低风险 MVP）**：`writeHelper.terminate(inst, STATUS_CANCELED, opinion, "强制收回")` 终态原实例（留 CANCELED 历史），再在**同一 `bizKey`(formId:dataId)** 下新建 DRAFT 让发起人重提。零新引擎能力，业务单据保留，仅流程实例重开（代价：新实例ID）。
- **推荐 MVP 用 B**；产品要求"原地回退保留ID"再评估 A。

### 11.4 forceDelete（强制删除）—— "先逻辑删业务行，再删引擎"，规避投影器回写
顺序（同一事务）：① 先逻辑删 `wf_instance`(@TableLogic→is_deleted=1)、`wf_task`、`wf_approval_log`（按 instId）。MP `@TableLogic` 让投影器 `findInstance`(selectById) 自动带 `is_deleted=0`，**逻辑删后投影器查不到 → 回写 no-op**，天然解耦。② `processService.deleteProcessInstance(engineInstId, "强制删除")` 清运行态。③ `historyService.deleteHistoricProcessInstance(engineInstId)` 清历史（见 §12.3）。列表 `monitorList` 需带 `is_deleted=0` 过滤。

### 11.5 超时判定（overdue）
- 到期时间由 `resolveDueTime(defId, nodeKey, task, inst)` 在**任务创建时**写入 `wf_task.dueTime`（仅节点配了时限才非空）。
- 过滤：`EXISTS (SELECT 1 FROM wf_task WHERE inst_id=? AND status=0(TODO) AND due_time<NOW())` 且 `wf_instance.status=0`。仅统计 TODO(0)，不含 COADJUTANT(7)/CIRCULATE(8)/READ(11)。

### 11.6 L3 自检异常（SB 独有，E9 无）
三列 `businessRowReady/requestIdBound/engineDeploymentMatched`：`=1` 正常、`=0` 异常、**`NULL`=存量未自检不算异常**（过滤务必用 `=0` 而非 `IS NULL`）。

### 11.7 ledger-shadow 是系统级健康指标，非逐实例异常
`/monitor/ledger-shadow` 暴露 `WfStateProjector` 的**内存** diff 计数（引擎事件↔期望 `wf_*` 状态不一致），重启清零、需监听开启。适合运维健康看板，不适合逐实例异常定位（逐实例用 §11.6 三列 + §11.5 超时）。

### 11.8 批量：逐条隔离 + 失败汇总，非全有全无
循环每条**独立 try/catch**，返回 `{success:[ids], failed:{id:reason}}`，避免一条坏数据（如已终态）打断整批。竞态：强制删除/归档前置 `status ∈ {0,4}` 守卫；建议乐观锁或 `select for update` 防双删。

### 11.9 权限越权边界
`canVisible` 的 admin 分支 + `tenant_id` 自动过滤 → 监控员只能看**本租户**全部；`forceArchive/forceDelete` 额外要求 `workflow` 角色（含 administrator）；`forceRecall` 走 terminate 时也要显式 `HAS_ROLE_WORKFLOW`，不要退化成"任意参与人可强收"。

### 11.10 前端只读复用
`InstanceFlow` 传 `mode='monitor'` → 隐藏提交/同意/退回，仅渲染 表单预览 + `FlowDiagram` + 流转记录 `Timeline`；数据用已有 `getLogs/getInstanceProgress/getBpmn`，详情零新增接口。监管动作按钮放抽屉底部调 monitor 端点。

### 11.11 与 E9 差异补充
E9 强制收回含**资产解冻**(`CptWfUtil`)；SB 无资产模块 → 无需。E9 管理员监控权须显式配"总部范围"；SB 的 `workflow` 角色天然看全租户（但缺"部门经理只看本部门"细粒度，要则回到 §1.3 `wf_monitor_scope`）。E9 force archive 用 `WfForceOver`；SB 用 `terminate(intent=APPROVED)` 语义等价、实现更轻。

---

## 12. 实现级补充：列表读源 / 历史清理 / 可见性（已核实）

### 12.1 监控列表不能手搓 wf_instance 查询，必须复用 InstanceView + actRead() 开关
- `toInstanceVO:2498-2523` **已做** `defName` 关联（`defMapper.selectById`）和 L3 三列搬运、`isTest` 透出 → 监控 VO 直接复用，**不必自己 join `wf_process_definition`**。
- `countByForm:1071-1079`、`logs:1137` 都按 `actRead()` 分流（读源 = `ACT_HI_PROCINST` 时用引擎表，否则 `wf_instance`）。**修正 §4.2「直查 wf_instance + join def」写法**：`monitorList` 应产出 `InstanceView → toInstanceVO`，并尊重 `actRead()`，否则监控列表与「我的请求/已办」口径不一致。
- 模板：`picker:856-880` 是同构参考——它**不按 starter 收口**（全部可见实例），正是监控列表想要的；`mine` 则收口为当前用户。直接照 `picker` 加筛选条件即可。

### 12.2 L3 三列确实被写入（异常 Tab 站得住）
- `businessRowReady`/`engineDeploymentMatched` 发起/草稿提升即 `set`（494-495、525-526），取值可靠。
- `requestIdBound` 在 `bindRequestId` 后 `patch`（556-559，`bound?1:0`）并双写 `ACT_HI_PROCINST`（`WfInstanceActWriter:98-100,108-112`）。
- 三列对**本版本之后**实例非空，`=0` 即真异常；`NULL` 仅老版本存量。

### 12.3 deleteProcessInstance 只清运行态，不清历史
`ProcessServiceImpl.deleteProcessInstance:423-435` 仅 `runtimeService.deleteProcessInstance`，且 `catch (FlowableException)` 忽略（实例已不在时 best-effort，安全）。→ 归档/撤回/终止保留历史正确（审计）；**forceDelete 必须额外 `historyService.deleteHistoricProcessInstance` 清 `ACT_HI_*`（含 `STARTER_`/`TITLE_` 自定义列）**，否则残留。

### 12.4 管理员可见性的租户细节（跨租户特性）
`canVisible:2333-2348` 对 admin 走 `WfAuthUtil.isSelfOrAdmin(...)`，**无条件 true**。→ `workflow` 管理员可看**任意租户**实例详情（跨租户，既有行为）。监控列表靠 SQL `tenant_id` 过滤限本租户；若需严格隔离，monitor 详情要单独补 tenant 校验。MVP 沿用既有即可，但需标注此跨租户特性。

### 12.5 读源开关对监控整体一致性的约束
`actRead()` 决定实例/任务/日志读 `wf_*` 还是 `ACT_*`：监控超时子查在 `actRead()` 为真时待办在 `ACT_RU_TASK` 而非 `wf_task`，需随读源切换；详情流转记录走 `readApprovalLogsFromAct`(1137)。监控不能只认 `wf_*`。

---

## 13. 引擎可行性：Flowable 能否支撑监控（已核实）

### 13.1 直接结论
**能。** Flowable（及任何 BPM 引擎）天然是流程监控的底座；E9 也是在自己的引擎上盖 `workflow_requestbase` 镜像，SpringBlade 的 `wf_instance`/`wf_task` 镜像同理。问题不在引擎能否，而在 SpringBlade 这层是否暴露——而镜像表、`stop/resume/forward/urge`、方案C 事件反写已就位，只缺 monitor 列表端点与 `forceArchive/forceDelete/batch`。

### 13.2 关键前提已核实通过：历史级别 = audit
`FlowableConfig:129-132` 显式 `configuration.setHistory("audit")`（注释强调手动装配必须代码声明、yml 不生效）。→ `ACT_HI_PROCINST`/`ACT_HI_TASKINST`/`ACT_HI_COMMENT` 都会落库，**已结束/已归档实例可查**。注意非 `full`（变量级历史关），但表单快照 SB 自己存（`snapshot/{nodeKey}`），不依赖 `ACT_HI_VARINST`，无缺口。若历史级别是 `none` 则监控看不到历史实例——本项目已排除此风险。

### 13.3 能力映射表

| 监控需求 | Flowable 原语 | SB 现状 |
|---|---|---|
| 列全部实例（运行+结束） | `RuntimeService` 运行态 / `HistoryService.createHistoricProcessInstanceQuery` | `wf_instance` + `actRead()` 双源镜像 ✓ |
| 暂停/恢复 | `suspendProcessInstanceById` / `activateProcessInstanceById` | `stop()`/`resume()` ✓ |
| 强制归档(办结) | 无原生"强制完成"，靠 `deleteProcessInstance`+终态标记 | `terminate(intent=APPROVED)` ✓ |
| 强制删除 | `deleteProcessInstance` + `HistoryService.deleteHistoricProcessInstance` | 运行时删除已有，历史需补 ✓ |
| 超时 | `Task.dueDate` / `wf_task.dueTime` | `resolveDueTime` 已写 ✓ |
| 流程图/进度 | `ProcessDiagramGenerator` + 活动状态 | `getInstanceProgress`(bpmnXml+四色) ✓ |
| 流转记录 | `ACT_HI_COMMENT`(audit 级落库) | `wf_approval_log` + `logs()` ✓ |
| 转办/改派 | `TaskService.setAssignee`/`delegateTask` | `forward()` ✓ |
| 催办/督办 | 引擎无原生，靠通知+日志 | `urge()`(LOG_SUPERVISE) ✓ |
| 强制收回(收回重填) | **无原生"退回可编辑"**，需 `ProcessInstanceModification` 移动 token | **唯一非平凡项** → 终止+同 bizKey 重开草稿(MVP) 或后续上修改 API |

### 13.4 Flowable 不给的（必须由应用层补，正是本方案内容）
权限模型（`workflow` 角色 + `tenant_id`）、业务友好镜像（`wf_instance` 中文状态/defName/紧急度/L3）、强制收回 token 移动、催办。

### 13.5 一致性副作用（对监控友好）
`FlowableConfig` 关了**异步执行器**与**异步历史**(127,133-134，方案C 一致性前提) → 历史**同步随引擎事务写入**，监控读到的历史与引擎实时态**无延迟/漂移**，比开异步历史的系统更适合准实时监控。

### 13.6 结论
引擎侧零障碍；应用侧要做的（monitor 列表、`forceArchive`/`forceDelete`/batch、前端页面）都是在已暴露的引擎能力上封装。**唯一要谨慎**：强制收回（需自实现 token 回退或终止重开）、forceDelete 补 `deleteHistoricProcessInstance`。
