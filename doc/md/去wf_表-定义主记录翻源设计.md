# 去 wf_ 表 —— 定义主记录（wf_process_definition）翻源设计

> P3-5「去 wf_ 表」收尾设计。前置成果（本分支已落地）：定义期**语义**读取已全面切 BPMN
> （`/nodes` `/links`、退回可达性、operator/field-perm/detail-perm/detail-filter/timeout 各开关 + 运行期 `loadNode`），
> dev 实测 BPMN 读源与引擎部署模型逐元素一致、草稿自动回退。
> 本文回答最后一个问题：**`wf_process_definition` 主记录本身能不能翻、怎么翻。**

## 1. 结论先行

**主记录不可完全退役**，推荐走「职责切分」路线：

- 引擎侧承载**定义语义**（节点/出口/操作者/权限/超时/折叠连线）——已落地；
- 主记录降级为**草稿暂存 + 业务元数据**（状态、版本组、表单绑定、灰度、路径类型）——长期保留；
- 退役对象收窄为 7 张**语义表**（wf_process_node / wf_node_link / wf_node_operator / wf_node_field_perm /
  wf_node_detail_perm / wf_node_detail_filter / wf_node_timeout），随各开关全量开启后停写（P6）。

## 2. 根本矛盾：主记录的职责在引擎侧没有对等物

逐字段盘点（消费接口 → 引擎侧对等物 → 可翻性）：

| 字段 | 主要消费 | 引擎侧对等物 | 结论 |
|---|---|---|---|
| `proc_key` | 版本组/部署/发起 | `ACT_RE_PROCDEF.KEY_` | 引擎原生（主记录列只是桥） |
| `proc_def_id` / `deployment_id` | 发起绑定、latest 校验 | 引擎原生 | 桥列，保留 |
| `bpmn_xml` | 画布回显、deploy 下发 | 已部署版本在 `ACT_GE_BYTEARRAY` | **草稿无部署 → XML 只能存这里**，不可翻 |
| `version` | 版本组排序/展示 | `ACT_RE_PROCDEF.VERSION_` 是**部署次数**，blade 是「另存 +1 不部署」 | 语义不同，不可翻 |
| `active_version_id` | 版本组激活切换 | 引擎只有版本列表，无「激活」概念 | 业务态，不可翻 |
| `status`(0/1/3) | 发起页收口、列表过滤 | 可由引擎推导（procDefId 空=草稿、挂起=已撤回、`__test` key=测试态）但语义复用脆弱（挂起还承担「停用」语义），测试态横跨两 key | **不建议翻** |
| `name`/`description` | 列表/详情/绑定提示 | BPMN process name（部署期固化） | 改名要重部署，破坏「部署不可变」原则 |
| `form_id`/`type`/`sort_order` | 发起页分组/排序/绑定校验 | `wf:processMeta`（formId/workflowType）仅是**部署期固化副本** | 副本≠事实源，事实源仍需业务侧 |
| `is_free`/`free_wf_type`/`form_type` | 自由流程/表单类型 | 无 | 纯业务 |

关键事实：**草稿定义在引擎侧没有任何存放处**（无 deployment → 无 BPMN 持久化）；且「激活哪个版本」
「是否灰度」「显示排序」都是写态业务数据，硬塞进 BPMN 意味着每次启停/切版本都要改引擎持久化数据，
与 Flowable 部署不可变原则直接冲突。

## 3. 方案对比

| 方案 | 思路 | 问题 |
|---|---|---|
| D1 硬翻 | 已发布定义的 list/detail 读 `ACT_RE_PROCDEF` + `processMeta`，主记录仅存草稿 | status 靠引擎状态推导（挂起语义复用、测试态跨界）脆弱；激活版本/排序/改名无处放或需重部署；收益≈0（主记录行数少、变化慢，不是性能/一致性瓶颈） |
| **D2 职责切分（推荐）** | 语义归 BPMN（已完成），主记录瘦身为业务态 + 草稿暂存 | 无——这是 D1 试图解决的目标，D2 已通过语义下沉达成 |
| D3 维持现状 | 主记录继续承载语义 | 与「引擎=唯一事实源」冲突，且已被本分支推翻 |

## 4. 落地清单（D2）

1. ✅ **语义读取切 BPMN**：`/nodes` `/links`、退回可达性、`loadNode`、operator/field-perm/detail-perm/detail-filter/timeout（各开关 + dev 实测）。
2. **停写语义表（P6，各开关全量开启稳定后）**：`saveBpmn`/`importBpmnXml`/节点出口配置接口停止写 7 张语义表；读侧开关默认置 true 观察一个版本周期后删代码。
3. **主记录瘦身**：`proc_def_id`/`deployment_id`/`proc_key` 保留（发起绑定/latest 校验的桥）；`bpmn_xml` 保留（草稿载体）；其余业务列原样。
4. **`wf_workflow_type`（路径类型字典）**：纯字典无引擎对等物 → 迁 blade-system 字典（`blade_dict_biz`）或长期保留，独立小任务。
5. **`wf_definition_gray`（灰度）**：业务态，长期保留（processMeta 里只是部署期快照）。
6. （可选远期）`wf_process_definition` → `wf_definition_meta` 更名，消除 `wf_` 前缀歧义（退役语义表后它已是纯业务表）。

## 5. 附带说明（2026-09-30 dev 实测修正）

实测发现 `wf_process_node`/`wf_node_link` 存在**画布历史残留**（某已发布定义 DB 11 节点/20 出口，
引擎模型仅 3/2）。经核对代码，`saveBpmn` **已有**画布 diff 删除逻辑（节点级联清理操作者/字段权限/
明细权限/布局，出口同样删除）——残留为该逻辑上线前产生的历史数据或带外写入，**非现行代码缺陷**，
无需新增清理逻辑；如需可出一次性对账脚本。

同批实测暴露并已修复的真问题：**画布产出的 BPMN 可能不带 `wf:` 扩展**（裸模型），此时读源会返回
「骨架节点」（nodeType/signOrder/extJson 全 null）而非回退，导致运行期把 DB 已配置的会签方式/
操作菜单/扩展设置**静默降级为默认值**。已在 `WfBpmnExtensionReader` 增加完整性守卫
（`nodeExtsComplete`：任一 UserTask 缺 `wf:node` 扩展 → 整体回退 `wf_*` 表），与回填作业前置条件对齐。

## 6. 验收口径（调整后）

- 7 张语义表：读全切 BPMN（开关默认 true）→ 停写 → 删表，走既有「上线检查」流程；
- 主记录/灰度/字典表：**不设退役目标**，以「职责切分完成度」验收（语义列无读取方、主记录仅剩业务态与桥列）。
