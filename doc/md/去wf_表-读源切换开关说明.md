# 去 wf_ 表 · 读源切换开关说明（P3-5）

> 目标：把定义期语义的「列表/筛选接口」从 `wf_*` 表逐步切换为读 BPMN `extensionElements`（`wf:` 命名空间），
> 让流程引擎成为定义语义的**唯一事实源**。运行期台账（`wf_instance`/`wf_task`/...）切换属 P4/P5，不在此文范围。

## 1. 设计原则

- **默认关、逐个开**：每个读源切换都有 `@Value` 开关，默认 `false`，即仍走原 `wf_*` 表读路径，**零行为变化**。
- **同源、可回退**：开关只切换「数据从哪读」，不改变取数语义（启用过滤、排序、字段映射与原表读保持一致）。
- **单一事实源**：开关开启后，该接口的数据完全来自 BPMN（随 `ACT_GE_BYTEARRAY` 持久化/版本化），`wf_*` 表对应读被旁路（**写仍保留**直到 P6 退役）。
- **回归先行**：每个开关落地时附带纯函数转换的单元测试（见 §4），无需 Spring/DB 即可跑通。

## 2. 已落地开关清单

| 开关配置键 | 作用接口 | 落地状态 | 备注 |
|---|---|---|---|
| `blade.workflow.perm-from-bpmn.enabled` | `/definition/{id}/node/{nodeKey}/field-perm` | ✅ 已完成（默认关） | 字段级权限，三维度 schema 已补全 + 往返测试 |
| `blade.workflow.operator-from-bpmn.enabled` | `/definition/.../node/{nodeKey}/operator` + 流转操作者 | ✅ 已完成（默认关） | 节点操作者解析 |
| `blade.workflow.timeout-from-bpmn.enabled` | `/node-timeout/list` | ✅ 已完成（默认关） | **该开关同时影响运行期 `WfTimeoutServiceImpl.resolveDueTime`/`firstOverdue` 取数**（见 §3） |
| `blade.workflow.detail-perm-from-bpmn.enabled` | `/definition/.../detail-perm` | ✅ 已完成（默认关） | 明细表整表权限，读 BPMN `wf:detailTablePerm` |
| `blade.workflow.detail-filter-from-bpmn.enabled` | `/definition/.../detail-filter` | ✅ 已完成（默认关） | 明细表字段筛选，读 BPMN `wf:detailFilter`（逐字段规则） |
| `blade.workflow.definition-from-bpmn.enabled` | `/definition/{id}/nodes`、`/definition/{id}/links`、退回可达性（`WfRejectManager`：`/task/{id}/reject-nodes` + reject 动作）+ 运行期 `loadNode()`（doApprove/转办/抄送/列表节点名） | ✅ 已完成（默认关） | 一个开关管定义期+运行期。定义期：`loadNodes` 读 BPMN `wf:node`（全部 UserTask→`WfProcessNode`，含 nodeType=0 开始节点）、`loadLinks` 读 `wf:link`+`wf:foldedLink`（折叠连线）；退回可达性：`WfRejectManager` 出口/节点改读 BPMN（算法抽为纯函数核心，BPMN 与 DB 读源同口径经 `WfRejectManagerBpmnTest` 证明）；**锚定 def 自己的部署 `proc_def_id`**，草稿/未部署自动回退 `wf_process_node`/`wf_node_link`。deploy 条件注入/saveBpmn 合并/另存版本复制/simulate/diff 等**内部写路径固定仍读 `wf_*` 表**（BPMN 读源拿不到草稿数据，不可依赖） |

> 历史 BPMN 元素 `detailPerm`（dt 作用域**字段级**权限）与表 `wf_node_detail_perm`（整表权限）是**两个不同概念**，前者已被 `getFieldPerm` 接线，后者走新建的 `detailTablePerm` 元素，命名已区分避免混淆。
> BPMN `detailFilter` 元素原仅作回填死表示（`rowFilter` JSON，无接口读取），现**重定义为逐字段比较规则**（dtIndex/modeType/fieldName/compareType/compareValue/isRequired），与 `wf_node_detail_filter` 表语义对齐。

## 3. timeout 开关的运行时影响（重要）

`blade.workflow.timeout-from-bpmn.enabled` 不是「仅接口读」的开关——它改的是 `WfTimeoutServiceImpl.listEnabled` 的取数源，
而 `listEnabled` 同时被以下调用方使用：

- 定义期接口：`WfTimeoutController.list`（前端超时规则列表）；
- **运行期**：`resolveDueTime`（计算待办 `due_time`）、`firstOverdue`（超时扫描命中规则）。

因此开启该开关后，**运行期超时计算也会改读 BPMN**。由于 BPMN `timeout` schema 已与 `wf_node_timeout` 字段对齐
（回填已结构化写入全部字段），语义等价，可放心切换；但切换前须确保：

1. 目标流程定义的 BPMN 已携带完整超时规则——经 `WfDefinitionBackfillJob` 回填，或前端 `saveBpmn` 收敛写回。
2. 已跑通回归（§4 的 `WfBpmnTimeoutSwitchTest` + BPMN 往返测试）。

## 4. 回归测试

| 测试类 | 类型 | 覆盖 |
|---|---|---|
| `WfBpmnExtensionRoundTripTest` | 纯单元（无 Spring） | `wf:` 全部扩展元素（`detailTablePerm`/`detailFilter` 新结构、`timeout` 等）解析与往返无损 |
| `WfBpmnTimeoutSwitchTest` | 纯单元（无 Spring） | `WfBpmnExtensionReader.toNodeTimeouts` 的启用过滤、seq 升序、全字段映射、空/脏数据容错 |
| `WfBpmnDetailPermSwitchTest` | 纯单元（无 Spring） | `toDetailPermVOs`/`toDetailFilterVOs` 的全字段映射、dtIndex 升序、modeType 过滤、空/脏容错 |
| `WfBpmnDefNodesTest` | 纯单元（无 Spring） | `toNodes`（节点列表：开始节点 nodeType=0 覆盖、sortOrder 排序、无扩展任务降级）+ `toNodeLinks`（真实连线与 foldedLink 合并、sortOrder 排序） |

> 上述转换方法均被设计为**无 Spring 依赖的静态纯函数**，使开关分支可在 H2/MySQL 之外的环境直接单测回归。

运行（沙箱本地 flowable 仓库）：

```powershell
Set-Location D:\workproject\springbladeandreact\springBlade
mvn -pl blade-service/blade-workflow -am test "-Dtest=WfBpmnExtensionRoundTripTest,WfBpmnTimeoutSwitchTest,WfBpmnDetailPermSwitchTest" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.repo.local=E:\project\mavenLib"
```

运行（沙箱本地 flowable 仓库）：

```powershell
Set-Location D:\workproject\springbladeandreact\springBlade
mvn -pl blade-service/blade-workflow test "-Dtest=WfBpmnExtensionRoundTripTest,WfBpmnTimeoutSwitchTest" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.repo.local=E:\project\mavenLib"
```

## 5. 开启流程（Checklist）

1. 确认目标 defId 的 BPMN 已含该语义的 `wf:` 扩展（回填作业已执行 / 前端已 `saveBpmn`）。
2. 跑通 §4 回归测试。
3. 在对应环境（`application.yml` / Nacos）将开关置 `true`。
4. 灰度观察：对比开关开/关下接口返回与运行期行为一致。
5. 全量开启后，待 P6 再移除 `wf_*` 表对应写路径。

## 6. 已知缺口（待办）

- 无（定义期读源切换：`field-perm` / `operator` / `timeout` / `detail-perm` / `detail-filter` 五个开关均已落地，默认关，回归测试齐备）。
- 其余未切换接口见 `project_wf_table_interface_inventory.md`：运行期实例/任务接口需等 P4/P5 `ACT_*` schema 与回填就绪后切。
