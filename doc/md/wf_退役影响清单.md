# `wf_*` 退役影响清单（T-1 依赖扫描）

> 来源：T-1 依赖扫描（code-explorer，very thorough）。配套计划见 `doc/md/开发计划.md`。
> 结论先行：**跨模块 Java 引用仅存在于 `blade-formmode`**（其余 7 个模块对 `org.springblade.workflow` 引用数 = 0）。无 Mapper XML 直接写 `wf_*`（MyBatis-Plus 走 `@TableName`，P6 退役对象即实体/Mapper/Service/Controller 类）。

---

## (a) `wf_*` 实体/表全量清单及分类（22 表）

全部实体位于 `blade-service-api/blade-workflow-api/.../org/springblade/workflow/entity/`。

### 定义期元数据类（16 表，应下沉 BPMN 扩展 / Flowable 原生）

| 表名 | 实体类 | 文件 | 语义 |
|---|---|---|---|
| wf_process_definition | **WfProcessDefinition** | `entity/WfProcessDefinition.java:18` | 流程定义主表（form_id/proc_key/version/status/gray 锚点）。⚠️ 无 `WfDefinition.java`，对应 `WfDefinitionController`/`WfDefinitionServiceImpl` |
| wf_process_node | WfProcessNode | `entity/WfProcessNode.java:22` | 节点 |
| wf_node_link | WfNodeLink | `entity/WfNodeLink.java:17` | 出口/连线 |
| wf_node_operator | WfNodeOperator | `entity/WfNodeOperator.java:20` | 节点操作者 |
| wf_node_timeout | WfNodeTimeout | `entity/WfNodeTimeout.java:25` | 节点超时规则 |
| wf_node_field_perm | WfNodeFieldPerm | `entity/WfNodeFieldPerm.java:25` | 节点字段权限 |
| wf_node_detail_perm | WfNodeDetailPerm | `entity/WfNodeDetailPerm.java:18` | 明细字段权限 |
| wf_node_detail_filter | WfNodeDetailFilter | `entity/WfNodeDetailFilter.java:18` | 明细行显示过滤 |
| wf_node_default_sign | WfNodeDefaultSign | `entity/WfNodeDefaultSign.java:19` | 节点默认签署 |
| wf_custom_action | WfCustomAction | `entity/WfCustomAction.java:21` | 自定义接口动作 |
| wf_custom_operation | WfCustomOperation | `entity/WfCustomOperation.java:19` | 自定义操作 |
| wf_custom_operation_action | WfCustomOperationAction | `entity/WfCustomOperationAction.java:16` | 自定义操作-动作映射 |
| wf_custom_operation_right | WfCustomOperationRight | `entity/WfCustomOperationRight.java:16` | 自定义操作-权限 |
| wf_definition_gray | WfDefinitionGray | `entity/WfDefinitionGray.java:24` | 灰度发布配置 |
| wf_subflow_request | WfSubflowRequest | `entity/WfSubflowRequest.java:20` | 子流程请求 |
| wf_workflow_type | WfWorkflowType | `entity/WfWorkflowType.java:17` | 流程分类（浏览框） |

> 定义期主写者：`WfDefinitionServiceImpl.java:126-132`（注入 7 个定义期 Mapper）；`WfPermServiceImpl`、`WfTimeoutServiceImpl`、`WfCustomActionServiceImpl`、`WfCustomOperationServiceImpl`、`WfSubflowServiceImpl` 分管其余。

### 运行期台账类（3 表，必须迁 `ACT_*`）

| 表名 | 实体类 | 文件 | 语义 |
|---|---|---|---|
| wf_instance | WfInstance | `entity/WfInstance.java:31` | 实例台账（status/end_time/engine_inst_id/pending_status/current_node_key/def_id/starter/title…） |
| wf_task | WfTask | `entity/WfTask.java:24` | 待办台账（status/engine_task_id/inst_id/due_time/timeout_handled/is_test/assignee…） |
| wf_approval_log | WfApprovalLog | `entity/WfApprovalLog.java:24` | 审批流转日志（inst_id/task_id/node_key/operator/log_type/opinion） |

> 运行期主写者：`WfInstanceServiceImpl.java`、`WfTaskServiceImpl`、`WfWriteHelper`+`WfStateProjector`。

### 边界/桥接类（3 表，单独决策）

| 表名 | 实体类 | 文件 | 处置建议 |
|---|---|---|---|
| wf_form_snapshot | WfFormSnapshot | `entity/WfFormSnapshot.java:20` | 表单数据快照（inst_id+nodeKey+data_json+layout_id）。建议保留为独立快照表或改引擎历史变量 |
| wf_migration_map | WfMigrationMap | `entity/WfMigrationMap.java:22` | ecology 存量迁移映射（一次性桥接）。迁移验收完成后退役 |
| wf_test_log | WfTestLog | `entity/WfTestLog.java:33` | 流程测试日志，`WfTestServiceImpl` 独占，随测试体系保留/退役 |

---

## (b) 后端引用面

### 跨模块（非 blade-workflow → wf_*）

| 模块 | 文件 | 引用对象 | 说明 |
|---|---|---|---|
| **blade-formmode** | `pom.xml:46-49` | `blade-workflow-api` | Maven compile 依赖 |
| **blade-formmode** | `WorkflowBillServiceImpl.java:22-23,39-41,61` | `IWorkflowClient`、`FormBindingVO` | 删表单前 `getFormBinding` 校验；VO 来自 `wf_process_definition.form_id` 与 `wf_instance.form_id` |
| **blade-formmode** | `ApprovalTriggerServiceImpl.java:13-14,36,83-91` | `StartProcessDTO`、`IWorkflowClient` | `startProcess(dto)` 间接写 wf_instance；**高风险**：`triggerSet.getWorkflowid()` 当 `wf_process_definition.id` 存入 `mode_triggerworkflowset.workflowid`，依赖 `wf_migration_map` 映射 ecology→新定义 |

> 其余模块（desk/mall/order/pay/demo/system/log）对 `org.springblade.workflow` 引用数 = 0（已逐模块确认）。

### 模块内（blade-workflow 自身，P6 退役主体）

| 文件 | 关键引用 |
|---|---|
| `WfDefinitionServiceImpl.java:126-146` | 写 7 个定义期 Mapper；注入 `WfInstanceMapper`（146）读 wf_instance 做绑定校验 |
| `WfInstanceServiceImpl.java:87-94,338,441,556,1448,1573` | 写 wf_instance/wf_task/wf_approval_log/wf_form_snapshot |
| `WfTaskServiceImpl.java` | wf_task 读写 |
| `WfPermServiceImpl.java` | wf_node_field_perm / detail_perm / detail_filter |
| `WfTimeoutServiceImpl.java` | wf_node_timeout |
| `WfCustomActionServiceImpl.java` / `WfCustomOperationServiceImpl.java` | wf_custom_action / wf_custom_operation(_action/_right) |
| `WfFormRenderServiceImpl.java` | wf_form_snapshot + 字段权限 |
| `WfSubflowServiceImpl.java` | wf_subflow_request |
| `WfTestServiceImpl.java` | wf_test_log + 节点测试态 |
| `WfWriteHelper.java` / `WfStateProjector.java` | wf_instance/wf_task/wf_approval_log（方案C 投影） |
| `job/WfTimeoutJob.java` | 直接读 wf_task（due_time/timeout_handled/is_test），消费 wf_node_timeout |
| `controller/*` | 各表 HTTP 入口 |

---

## (c) 前端引用面（`ant-design-pro`）

服务入口：`src/services/workflow/index.ts`（87+ 接口，前缀 `/api/blade-workflow`）。

### 定义期写接口（失去承载对象 → 改存 BPMN 扩展）

| 接口（行） | 端点 | 承载表 | 改造性质 |
|---|---|---|---|
| `saveBpmn`(346)/`getBpmn`(364) | `/definition/{id}/bpmn` | BPMN | 画布保存 |
| `importDefinition`(331) | `/definition/import` | 解析 `wf:` 扩展自动建节点/操作者/权限 | **重点**：导入后自动建 wf_* 的逻辑需改 |
| `listNodes`(465)/`updateNode`(499)/`deleteNode`(563) | `/definition/{id}/node…` | wf_process_node | 改存 BPMN 节点扩展 |
| `listLinks`(494)/`createLink`(507)/`updateLink`(548)/`deleteLink`(556) | `/definition/{id}/link…` | wf_node_link | 改存 BPMN 连线 |
| `configOperator`(469)/`getNodeOperators`(479)/`syncOperatorToNodes`(486) | `/definition/{id}/node/{nodeKey}/operator` | wf_node_operator | 改存 BPMN 操作者扩展 |
| `getFieldPerm`(611)/`saveFieldPerm`(617) | `…/field-perm` | wf_node_field_perm | 改存 BPMN |
| `getDetailPerm`(624)/`saveDetailPerm`(630) | `…/detail-perm` | wf_node_detail_perm | 改存 BPMN |
| `getDetailFilter`(637)/`saveDetailFilter`(644) | `…/detail-filter` | wf_node_detail_filter | 改存 BPMN |
| `listNodeTimeouts`(1291)+`NodeTimeoutModal.tsx` | 节点超时 | wf_node_timeout | 改存 BPMN 定时器边界事件 |
| `listCustomActions`(530)/`saveCustomAction`(535)/`deleteCustomAction`(543) | `/custom-action…` | wf_custom_action | 改存 BPMN 扩展 |
| `CustomOperationModal.tsx` 系列 | 自定义操作 | wf_custom_operation(_action/_right) | 改存 BPMN 扩展 |
| `deployDefinition`(339)/`saveAsNewVersion`(368)/`activateVersion`(373)/`listVersions`(380)/`diffVersion`(422)/`testDefinition`(429)/`withdrawDefinition`(433)/`removeDefinition`(459) | `/definition/…` | wf_process_definition 生命周期 | 改走 Flowable 部署/版本 |
| `createWorkflowType`(956) | `/definition/browser/wftype` | wf_workflow_type | 分类字典 |
| `saveNodeTestStatus`(604)/`simulateDefinition`(596) | 节点测试态/模拟 | 节点测试标记 | 测试态 |

### 运行期写接口（承载对象迁 `ACT_*`）

| 接口（行） | 端点 | 承载表 | 改造性质 |
|---|---|---|---|
| `startInstance`(662) | `/instance/start` | 建 wf_instance+wf_task | 改由 Flowable 发起 |
| `saveDraft`(920)/`deleteDraft`(928) | `/instance/save-draft` `/instance/{id}/draft` | wf_instance 草稿态 | 草稿语义重新设计 |
| `saveFormData`(912) | `/form/save` | wf_form_snapshot | 快照边界表 |
| `approveTask`(767)/`rejectTask`(771)/`forwardTask`(779)/`addSignTask`(783)/`circulateTask`(787)/`urgeTask`(791)/`markTaskViewed`(737) | `/task/{id}/…` | wf_task 状态 | 改走 Flowable TaskService |
| `withdrawInstance`(800)/`stopInstance`(804)/`resumeInstance`(808)/`cancelInstance`(812) | `/instance/{id}/…` | wf_instance 生命周期 | 改走引擎 |
| `getLogs`(715) | `/instance/{id}/logs` | 读 wf_approval_log | **改读 `ACT_HI_COMMENT`** |
| `getSnapshot`(741) | `/instance/{id}/snapshot/{nodeKey}` | 读 wf_form_snapshot | 读边界快照表 |
| `getInstanceNodeOperators`(729) | `/instance/{id}/node-operators` | 运行期操作者 | 改读引擎指派 |
| `getInstance`(666)/`freshInstance`(699)/`getInstanceByBiz`(711)/`listTodo`(745)/`listDone`(749)/`listMyRequests`(759) | 实例/任务查询 | 读 wf_instance/wf_task | 改读 Flowable 历史/运行 |

### 前端页面引用
- `src/pages/FormMode/WorkflowDesign/`（26 文件）：`BpmnDesigner.tsx`、`NodeDetail.tsx`、`NodeInfoPanel.tsx`、`NodeInfoTable.tsx`、`NodeOperatorModal.tsx`、`NodeTimeoutModal.tsx`、`LinkInfoPanel.tsx`、`LinkDetail.tsx`、`CustomActionRegisterModal.tsx`、`CustomOperationModal.tsx`、`FormContentDesignModal.tsx`、`NodeExtraOperateModal.tsx`、`NodeOperateMenuModal.tsx`、`SimulateModal.tsx`、`VersionDiffModal.tsx`、`WorkflowTestModal.tsx` 等。
- `src/pages/Workflow/`（运行期消费）：`Todo/Todo.tsx`、`Done/Done.tsx`、`Request/Request.tsx`、`Create/Start.tsx`、`Create/Create.tsx`、`Create/InstanceFlow.tsx`。

---

## (d) 方案C 投影设施（去 `wf_*` 后退役）

| 设施 | 路径 | 职责 | 退役触发 |
|---|---|---|---|
| **WfStateProjector** | `blade-workflow/.../service/helper/WfStateProjector.java` | 引擎事件→台账反写（onProcessCompleted/Cancelled/EntitySuspended/Activated/TaskCompleted），按 `engine_inst_id`/`engine_task_id` 关联 wf_* | wf_instance/wf_task 退役即整体删除 |
| **WfEngineEventListener** | `blade-workflow/.../listener/WfEngineEventListener.java` | Flowable 全局 `FlowableEventListener`，派发事件给 Projector；`@ConditionalOnProperty("blade.workflow.ledger-listener.enabled")` | 监听注册一并移除 |
| **WfWriteHelper** | `blade-workflow/.../service/helper/WfWriteHelper.java` | 双写收口器 `terminate/suspend/activate` 同时写 wf_* + 引擎；`appendLog` 写 wf_approval_log（231）；含 `intent`(`pending_status`) 与 `approvalCommentEnabled` 双写开关 | 运行期台账退役后生命周期写只留引擎侧；`appendLog` 改走引擎评论 |

> 三者耦合：Listener→Projector，WriteHelper 显式双写兜底（同一开关 `ledger-listener.enabled` 与 Listener 互斥）。`WfMonitorController.java:82` 暴露 `diffSnapshot()`（`GET /monitor/ledger-shadow`）随设施一并退役。

---

## (e) 高风险 / 易遗漏点

1. **💥 formmode `mode_triggerworkflowset.workflowid` 硬绑 `wf_process_definition.id`**（`ApprovalTriggerServiceImpl.java:83-86`）：退役后外键含义变为 Flowable `processDefinitionId`/部署ID，存量 ecology 映射（`wf_migration_map`）与 formmode 触发表需同步改键，否则"保存表单自动发起流程"失效。
   > ⚠️ **已失效（2026-10-02，D16）**：D2 已保留 `wf_process_definition` 主记录不退役，其 `id` 长期合法，无需按原断言改键。新决议为"以 Flowable 为唯一事实源"：触发绑定改 `procKey + TENANT_ID_`（见 `Flowable8承接台账模块-去wf_表改造分析.md` §十八 / 决策文档 **D16**），而非裸 `processDefinitionId`。此处"必须改键"的前提已被推翻。
2. **💥 定时任务 `WfTimeoutJob`**：直接读 `wf_task.due_time/timeout_handled/is_test` 并消费 `wf_node_timeout`。退役后超时必须由 Flowable 定时器边界事件承载，`is_test` 过滤与 `timeout_handled` 幂等需重设计。
3. **⚠️ `WfInstanceServiceImpl` 反向读定义期表**（def/node/gray/link/operator Mapper）用于渲染/校验；`WfDefinitionServiceImpl` 注入 `WfInstanceMapper`（146）做表单绑定校验。退役时两处跨定义期↔运行期引用需清理。
4. **⚠️ `wf_approval_log` 读侧 28+ 处**：`getLogs` 前端接口依赖它，切 `ACT_HI_COMMENT` 前须全量回归读路径。
5. **⚠️ `wf_form_snapshot` 边界表**：历史打印/回溯（含 layout_id 布局重现）依赖，若随 wf_instance 删除会丢——建议保留独立快照表或迁引擎历史变量。
6. **⚠️ `wf_migration_map` 迁移桥接表**：ecology 存量在途实例接管唯一凭据，迁移验收通过后退役。
7. **⚠️ 序列化/雪花 ID 契约**：`IWorkflowClient.startProcess` 与前端 `startInstance` 强制返回**字符串**实例ID（避免 JS 精度丢失）。迁 Flowable 后若改用引擎 `processInstanceId`，所有 `instanceId` 透传需复核。
8. **⚠️ `WfDefinition` 命名陷阱**：实体实为 `WfProcessDefinition`（`WfDefinitionController`/`WfDefinitionServiceImpl` 无对应实体）；别漏 `WfDefinitionGray`（灰度孪生表）。
9. **⚠️ 测试体系强耦合**：`WfTestServiceImpl`(126KB) 独占 `wf_test_log`，`saveNodeTestStatus`/`simulateDefinition` 写测试态；`WorkflowTestModal.tsx` + `WfMultiInstanceGateTest` 断言 `wf_task` 行为。退役需连带评估测试开关与 `is_test`。
10. **⚠️ Feign 契约**：`IWorkflowClient` 仅 2 方法（`startProcess`/`getFormBinding`），`getFormBinding` 的 `FormBindingVO` 后端查 `wf_process_definition.form_id` 与 `wf_instance.form_id`——formmode 删表单"占用校验"依赖它。

---

## 一句话退役边界
- **定义期 16 表**：`WfDefinitionServiceImpl` + Controller 主写者 → 改由 BPMN 扩展承载（前端 WorkflowDesign 全部面板联动）。
- **运行期 3 表**：`WfInstanceServiceImpl`/`WfTaskServiceImpl`/`WfWriteHelper`/`WfStateProjector`/`WfEngineEventListener` 主写者 → 迁 Flowable `ACT_*`。
- **3 边界表**：`wf_form_snapshot`（保留/历史变量）、`wf_migration_map`（迁移后退役）、`wf_test_log`（测试体系）单独决策。
- **唯一跨模块风险在 blade-formmode**：Feign + `mode_triggerworkflowset.workflowid` 外键 + Maven 依赖，是最易被遗漏的数据耦合点。
