# `wf_*` 依赖扫描影响清单（T-1 交付物）

> 生成日期：2026-10-01。来源任务：T-1 依赖扫描（执行顺序中的关键路径起点，⬜→本次产出）。
> 原始扫描底稿：`wf_退役影响清单.md`（code-explorer very thorough，含 22 表逐文件:行引用点、跨模块、高风险项）。
> 分类标尺（唯一依据，不重造）：`去wf_表-D系列决策结论.md`、`去wf_表-读源切换开关说明.md`、`去wf_表-定义主记录翻源设计.md`、`wf_BPMN扩展schema定稿.md`。
> 本文件在其基础上套用标尺，输出「表/字段 → 目标源 → 改造动作 → 归属任务 → 风险等级」结构化矩阵。

---

## 一、分类标尺

### 1.1 目标源四类（取自 D 系列决策 + 翻源设计）

| 目标源 | 含义 | 适用对象 |
|---|---|---|
| **翻源 BPMN** | 语义下沉到 BPMN `extensionElements`（`wf:` 命名空间），引擎=`ACT_GE_BYTEARRAY` 持久化/版本化 | 定义期 7 张语义表 + 自定义操作/动作 + 默认签署 |
| **ACT_\* 原生表** | 运行期台账迁 Flowable 原生表（`ACT_HI_PROCINST`/`ACT_RU_TASK`/`ACT_HI_TASKINST`/`ACT_HI_COMMENT`）+ 原生列（`BUSINESS_STATUS_`/`DUE_DATE_`）+ 加列（`DEF_KEY_`/`TENANT_ID_`） | 运行期 3 表（wf_instance/wf_task/wf_approval_log） |
| **读源切换开关** | 定义期列表/筛选接口通过 `@Value` 开关从 `wf_*` 切到读 BPMN，默认关、可回退，写保留到 P6 | 6 个已落地开关（见 1.2） |
| **退役 / 保留** | P6 随开关全开停写删表；或主记录瘦身为业务态长期保留；或边界表单独决策 | 主记录、灰度、字典、快照、迁移桥、测试日志 |

### 1.2 已落地读源切换开关（默认关，写保留到 P6）

| 开关 | 作用接口 | 归属 |
|---|---|---|
| `perm-from-bpmn.enabled` | `/node/{nodeKey}/field-perm` | T-11 |
| `operator-from-bpmn.enabled` | `/node/{nodeKey}/operator` + 流转操作者 | T-11 |
| `timeout-from-bpmn.enabled` | `/node-timeout/list` + **运行期** `resolveDueTime`/`firstOverdue` | T-8 |
| `detail-perm-from-bpmn.enabled` | `/detail-perm`（读 `wf:detailTablePerm`） | T-11 |
| `detail-filter-from-bpmn.enabled` | `/detail-filter`（读 `wf:detailFilter`） | T-11 |
| `definition-from-bpmn.enabled` | `/nodes`、`/links`、退回可达性、运行期 `loadNode()` | T-5 / T-11 |

> 注：上述 6 开关转换方法均为无 Spring 依赖纯函数，回归测试齐备（见读源切换开关说明 §4）。

### 1.3 风险等级定义

- **P0**：运行期核心数据正确性 / 阻断 T-2/T-5/T-6/T-7 前置；改造失败会丢数据或停服。
- **P1**：定义期语义，失败仅影响设计器/配置，可由开关回退，不影响运行期。
- **P2**：边界/字典/测试/桥接表，影响小或单独决策，不阻塞主干。

---

## 二、主影响矩阵（22 表 + 跨模块）

> 「引用点」列指向 `wf_退役影响清单.md` 对应章节（a/b/c/d/e），避免重复罗列文件:行。

### 2.1 定义期 16 表 → 翻源 BPMN / 保留

| 表 | 实体 | 引用点(底稿) | 目标源 | 改造动作 | 归属任务 | 风险 |
|---|---|---|---|---|---|---|
| wf_process_definition | WfProcessDefinition | (a)/(b) | **保留**（职责切分） | 主记录瘦身为业务态+草稿载体；保留桥列 `proc_def_id`/`deployment_id`/`proc_key`+`bpmn_xml`；语义读取已切 BPMN | P3-5(已完成) / T-14(收窄,不退役) | P1 |
| wf_process_node | WfProcessNode | (a)/(b) | **BPMN `wf:node`** | 回填 BPMN；`definition-from-bpmn` 开关已落地(默认关)；全开稳定后停写删表 | T-5(回填) / T-14 | **P0** |
| wf_node_link | WfNodeLink | (a)/(b) | **BPMN `wf:link`+`wf:foldedLink`** | 同 node；折叠连线合并 | T-5 / T-14 | **P0** |
| wf_node_operator | WfNodeOperator | (a)/(b) | **BPMN `wf:operator`** | `operator-from-bpmn` 已落地；回填操作者扩展 | T-5 / T-11 / T-14 | **P0** |
| wf_node_field_perm | WfNodeFieldPerm | (a)/(b) | **BPMN `wf:fieldPerm`** | `perm-from-bpmn` 已落地(三维度 schema 已补全) | T-5 / T-11 / T-14 | **P0** |
| wf_node_detail_perm | WfNodeDetailPerm | (a)/(b) | **BPMN `wf:detailTablePerm`** | `detail-perm-from-bpmn` 已落地 | T-5 / T-11 / T-14 | P1 |
| wf_node_detail_filter | WfNodeDetailFilter | (a)/(b) | **BPMN `wf:detailFilter`** | `detail-filter-from-bpmn` 已落地(逐字段规则) | T-5 / T-11 / T-14 | P1 |
| wf_node_timeout | WfNodeTimeout | (a)/(b) | **BPMN `wf:timeout`** | `timeout-from-bpmn` 已落地，**同时影响运行期**超时计算 | T-5 / T-8 / T-14 | **P0** |
| wf_node_default_sign | WfNodeDefaultSign | (a) | **BPMN 扩展** | 翻 BPMN（默认签署，归 node 扩展或独立元素）；当前无专属开关，需补 | T-5 | P1 |
| wf_custom_action | WfCustomAction | (a)/(b) | **BPMN `wf:customAction`** | 翻 BPMN 扩展；`importDefinition` 自动建逻辑改读 BPMN | T-5 | P1 |
| wf_custom_operation | WfCustomOperation | (a)/(b) | **BPMN `wf:customOperation`** | 翻 BPMN 扩展 | T-5 | P1 |
| wf_custom_operation_action | WfCustomOperationAction | (a) | **BPMN（customOperation 子元素）** | 随 customOperation 翻 BPMN | T-5 | P1 |
| wf_custom_operation_right | WfCustomOperationRight | (a) | **BPMN（customOperation 子元素）** | 随 customOperation 翻 BPMN | T-5 | P1 |
| wf_definition_gray | WfDefinitionGray | (a) | **保留**（业务态） | 灰度孪生，长期保留（processMeta 仅部署期快照） | T-14(收窄) | P2 |
| wf_subflow_request | WfSubflowRequest | (a)/(b) | **待决策** | 子流程请求：可能迁引擎子流程或保留独立表 | 待决策(单独任务) | P2 |
| wf_workflow_type | WfWorkflowType | (a)/(c) | **字典**（迁 `blade_dict_biz` 或保留） | 纯字典无引擎对等物，独立小任务 | T-14 | P2 |

### 2.2 运行期 3 表 → ACT_\* 原生表（P0 主干）

| 表 | 实体 | 引用点(底稿) | 目标源 | 改造动作 | 归属任务 | 风险 |
|---|---|---|---|---|---|---|
| wf_instance | WfInstance | (a)/(b)/(d) | **`ACT_HI_PROCINST` + 运行期 `ACT_RU_EXECUTION` + 原生 `BUSINESS_STATUS_`** | 迁 ACT_*；写侧 `WfWriteHelper`/`WfStateProjector` 改造；`WfInstanceServiceImpl` 反向读定义期表需清理 | T-6(加列) / T-7(实例回填) / T-10(接口改造) | **P0** |
| wf_task | WfTask | (a)/(b)/(d) | **`ACT_RU_TASK`/`ACT_HI_TASKINST` + `DUE_DATE_`(已加索引) + `is_test` 过滤** | 迁 ACT_*；合成待办(rejectToStarter)维持 `wf_task` 占位(D2)或改引擎；`WfTimeoutJob` 改 Flowable 定时器(D9) | T-6 / T-7 / T-8 / T-10 | **P0** |
| wf_approval_log | WfApprovalLog | (a)/(b)/(e) | **`ACT_HI_COMMENT`(已加 `TENANT_ID_`, D8)** | 读切 `ACT_HI_COMMENT`（28+ 读路径全量回归）；写侧 `WfWriteHelper.appendLog` 改引擎评论 | T-9(迁移) / T-10 | **P0** |

### 2.3 边界 3 表 → 保留/单独决策（P2）

| 表 | 实体 | 引用点(底稿) | 目标源 | 改造动作 | 归属任务 | 风险 |
|---|---|---|---|---|---|---|
| wf_form_snapshot | WfFormSnapshot | (a)/(e) | **保留独立快照表 或 迁引擎历史变量** | 历史打印/回溯（含 `layout_id` 布局重现）依赖；删除会丢 | 待决策(单独任务) | P2 |
| wf_migration_map | WfMigrationMap | (a)/(e) | **迁移验收后退役** | ecology 存量在途实例接管唯一凭据；保留作对账凭据(H4) | T-14(迁移后) | P2 |
| wf_test_log | WfTestLog | (a)/(e) | **保留**（D13 节点测试态维持存 `wf_*`） | `WfTestServiceImpl` 独占；随测试体系保留/退役 | 单独决策 | P2 |

### 2.4 跨模块引用（唯一跨模块风险在 blade-formmode）

| 模块 | 引用点(底稿) | 目标源 | 改造动作 | 归属任务 | 风险 |
|---|---|---|---|---|---|
| blade-formmode (pom) | (b) `pom.xml:46-49` | Maven 依赖 `blade-workflow-api` | 维持（API 包不退役，仅语义表停写） | T-10 | P1 |
| blade-formmode `ApprovalTriggerServiceImpl` | (b)/(e) `:13-14,36,83-91` | Feign `IWorkflowClient` | **已失效（2026-10-02 D16）**：D2 保留 `wf_process_definition` 主记录不退役，无需改键；新决议以 Flowable 为事实源，绑定改 `procKey + TENANT_ID_`（见改造分析 §十八 / 决策文档 D16），原"必须改键→P0"假设推翻 | T-10 + formmode 单独改造 | 已由 D16 接管 |
| blade-formmode `WorkflowBillServiceImpl` | (b) `:22-23,39-41,61` | Feign `IWorkflowClient.getFormBinding` | `FormBindingVO` 读 `wf_process_definition.form_id` 与 `wf_instance.form_id`；删表单占用校验依赖它——formmode 侧同步改读源 | T-10 | P1 |

> 其余 7 模块（desk/mall/order/pay/demo/system/log）对 `org.springblade.workflow` 引用数 = 0（已逐模块确认）。

---

## 三、P0 高风险汇总（改造失败即丢数据/停服）

取自 `wf_退役影响清单.md §e`，逐条绑定归属任务：

1. **formmode `mode_triggerworkflowset.workflowid` 外键**：退役后含义变为 Flowable `processDefinitionId`，存量映射与触发表需同步改键 → T-10 + formmode。
2. **`WfTimeoutJob` 直接读 `wf_task.due_time/timeout_handled/is_test`**：退役后超时必须由 Flowable 定时器边界事件承载，`is_test` 过滤与 `timeout_handled` 幂等重设计 → T-8。
3. **`WfInstanceServiceImpl` 反向读定义期表**（def/node/gray/link/operator）渲染/校验；`WfDefinitionServiceImpl` 注入 `WfInstanceMapper` 做表单绑定校验 → T-5/T-10 清理跨期引用。
4. **`wf_approval_log` 读侧 28+ 处**：切 `ACT_HI_COMMENT` 前须全量回归读路径 → T-9/T-10。
5. **`wf_form_snapshot` 边界表**：历史打印/回溯（含 `layout_id`）依赖，随 wf_instance 删除会丢 → 单独决策。
6. **`wf_migration_map` 迁移桥接表**：ecology 存量在途实例接管唯一凭据，迁移验收后退役 → T-14。
7. **序列化/雪花 ID 契约**：`IWorkflowClient.startProcess` 与前端 `startInstance` 强制返回**字符串**实例ID；迁 Flowable 后透传复核 → T-10。
8. **`WfDefinition` 命名陷阱**：实体实为 `WfProcessDefinition`；别漏 `WfDefinitionGray`（灰度孪生） → T-5/T-14。
9. **测试体系强耦合**：`WfTestServiceImpl`(126KB) 独占 `wf_test_log`；`WorkflowTestModal`+`WfMultiInstanceGateTest` 断言 `wf_task` 行为 → 连带评估 → D13 维持。
10. **Feign 契约**：`IWorkflowClient` 仅 2 方法；`getFormBinding` 查 `wf_process_definition.form_id`/`wf_instance.form_id` → formmode 占用校验依赖 → T-10。

---

## 四、既有 migration SQL 冲突核对结论

| 已落地 DDL（doc/sql/migration） | 列/索引 | 影响 |
|---|---|---|
| `V2026.09.30_001__act_ru_task_due_index.sql` | `ACT_RU_TASK.IDX_RU_TASK_DUE (DUE_DATE_)` | T-8 超时改造依赖此索引，新脚本**不得重复建** |
| `act_add_def_key_bridge.sql` | `ACT_HI_PROCINST.DEF_KEY_ VARCHAR(255) NULL` | T-2/T-6 模型终稿若再 ADD 须幂等避让 |
| `act_add_comment_tenant.sql` | `ACT_HI_COMMENT.TENANT_ID_ VARCHAR(64) DEFAULT ''` | D8 已完成（存量 234 条回填）；T-9 读侧直接复用，勿重复加列 |

**结论**：
- 无不可逆冲突；但 **T-2（§11.6 模型终稿）/ T-6（ACT_\* 加列）新 DDL 脚本必须以幂等方式编写**（`IF NOT EXISTS` / 存在性判断），且新建 `V*` 脚本时间戳须避让既有文件，避免 Flyway/Liquibase 版本号碰撞。
- 已加列（`DEF_KEY_`/`TENANT_ID_`/`DUE_DATE_` 索引）在 T-2 终稿中**引用即可、不再 ADD**。
- 新加列严格遵循硬约束：新列必须 `NULL-able + DEFAULT`；DDL 首行 `USE blade`。

---

## 五、与 T-x 任务映射总览

| 归属任务 | 输入（来自本清单） | 当前状态 |
|---|---|---|
| T-1（本任务） | 全量扫描 + 本矩阵 | ✅ 本次产出 |
| T-2 数据模型 DDL | §11.6 终稿；须幂等避让既有 3 份 DDL | 🟡 已出稿待评审 |
| T-3 BPMN schema 定稿 | `wf_BPMN扩展schema定稿.md` | ✅ 已完成 |
| T-5 定义期回填 | 7 语义表 + 自定义操作/动作/默认签署 → 写 BPMN（依赖 T-3） | ⬜ |
| T-6 ACT_\* 加列/索引 | wf_instance/wf_task 迁 ACT_\* 所需加列（避让 §四） | ⬜ |
| T-7 实例级回填 | wf_instance/wf_task 存量迁移 + `TENANT_ID_` 校验 | ⬜ |
| T-8 超时链路改造 | wf_node_timeout/wf_task 超时 → Flowable 定时器；`WfTimeoutJob` 重写（依赖 T-6） | ⬜ |
| T-9 审批日志迁移 | wf_approval_log → `ACT_HI_COMMENT`（复用 D8 列） | 🟡 |
| T-10 接口改造 | 运行期实例/任务接口 + formmode 跨模块 + 雪花 ID 契约 + Feign | ⬜ 仅分析 |
| T-11 权限/操作者改读 BPMN | 6 开关已落地（默认关）；全量开启观察 | 🟡 |
| T-12 压测 | 依赖 T-8/T-10 | ⬜ |
| T-13 多租户验证 | 依赖 T-7/T-10 | ⬜ |
| T-14 wf_\* 退役 | 7 语义表停写删表；主记录/灰度/字典收窄不退役；wf_migration_map 迁移后退役 | ⬜ |

---

## 六、下一步建议

1. **T-1 收口**：本矩阵即 T-1 交付；建议将 `wf_退役影响清单.md`（原始底稿）与本文件一并作为后续任务输入。
2. **立即解锁 T-2**：评审 §11.6 模型终稿，按 §四幂等约束出 `V*` 脚本（建表/加列），解锁 T-6。
3. **并行推进 T-5**：7 语义表回填脚本（写 BPMN 扩展），依赖 T-3（已完成），与 T-2 无强依赖可并行。
4. **跨模块提前通报**：blade-formmode 的 `mode_triggerworkflowset.workflowid` 外键（P0）须尽早与 formmode 负责人对齐改键方案，避免 T-10 末期才发现。
