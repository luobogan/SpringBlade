# `wf:` BPMN 扩展 Schema 定稿（T-3）

> 来源：T-3（方案 C 退役后的定义期语义载体）。配套：`开发计划.md`、技能 `flowable-workflow-builder`。
> 本文件是 **canonical 契约**：后端 `BpmnExtensionUtil`、前端 `wf:` moddle（F-T1）、BPMN 生成模板、往返测试（V7/V13）**共用同一套语义**，禁止另起一套。

## 1. 命名空间（统一）

```xml
xmlns:wf="http://www.springblade.org/workflow"
```

> ⚠️ 历史测试曾用 `http://www.springblade.io/wf`，**已作废**，统一改用上面的官方命名空间（与技能模板、springblade-mapping 一致）。

## 2. 放置规则

- **节点语义** → 挂在 `<userTask>` 的 `<extensionElements>` 下，元素名 `<wf:node>`，**key = userTask 的 `id`**（即 nodeKey）。
- **出口语义** → 挂在 `<sequenceFlow>` 的 `<extensionElements>` 下，元素名 `<wf:link>`，**key = sequenceFlow 的 `id`**。
- **流程级语义**（表单/类型/灰度/DEF_KEY_ 桥接）→ 挂在 `<process>` 的 `<extensionElements>` 下，元素名 `<wf:processMeta>`。
- 每个元素可含多个同名子元素（如多条操作者、多条超时规则、多条字段权限）。

## 3. 保留名防护（不得用作自定义元素 localName）

Flowable 按 `localName` 分发子元素解析（`BpmnXMLUtil#genericChildParserMap`），自定义名冲突会被当原生解析而**语义被吞**。禁用：`condition`、`conditionExpression`、`documentation`、`executionListener`、`taskListener`、`formProperty`、`field`、`timerEventDefinition`、`timeDate`、`timeCycle`、`timeDuration`、`multiInstanceLoopCharacteristics`、`script`、`eventListener`。

**本项目自定义元素名**（均不与保留名冲突）：`node`、`link`、`processMeta`、`operator`、`fieldPerm`、`detailPerm`、`detailFilter`、`timeout`、`customAction`、`operation`、`right`、`extJson`、`extraOperations`。

## 4. 元素 / 属性参考

### 4.1 `wf:node`（userTask 扩展，替代 wf_process_node / wf_node_*）

| 属性 | 含义 | 对应原表 |
|---|---|---|
| `nodeType` | 1创建 2审批 3提交 4归档 5等待 6自动 | wf_process_node.node_type |
| `signOrder` | 会签顺序 | wf_process_node |
| `mergeType` | 0或签 1顺序签 2依次 | wf_process_node |
| `passNum` | 通过数 / 比例 | wf_process_node |
| `allowReject` | 0/1 允许驳回 | wf_process_node |
| `allowForward` | 0/1 允许转办 | wf_process_node |
| `autoApprove` | 0/1 自动通过 | wf_process_node |
| `sortOrder` | 节点排序 | wf_process_node |
| `testStatus` | 0/1 节点测试态（D13：存扩展，不落表） | wf_process_node.test_status |
| `multiInstance` | 0/1 是否多实例会签（P2/P3 关联） | 新语义 |
| `formKey` | 节点表单 key（可选） | wf_process_node |

**子元素**

- `<wf:operator groupNo opType objId bhxj levelMin levelMax/>`（0..n）→ `wf_node_operator`：`opType` 1人员 2部门 3角色 17等；`objId` 逗号分隔 ID。
- `<wf:fieldPerm field perm/>`（0..n）→ `wf_node_field_perm`：`perm` 取值 `hidden`/`readonly`/`edit`/`required`（兼容 0/1/2/3）。
- `<wf:detailPerm dtKey field perm/>`（0..n）→ `wf_node_detail_perm`：`dtKey` 明细表标识。
- `<wf:detailFilter dtKey rowFilter/>`（0..n）→ `wf_node_detail_filter`：`rowFilter` 行过滤表达式。
- `<wf:timeout seq enabled startType startField endType endFixedTime endField durationMin actionWay opinion operatorIds remindBeforeOperator remindTypes remindPersons/>`（0..n）→ `wf_node_timeout`：`actionWay` ∈ `autoApprove`/`forward`/`assign`/`remind`；`startType` 1相对 2字段；`endType` 1时长 2固定时刻 3字段。
- `<wf:customAction actionKey name type url expression/>`（0..n）→ `wf_custom_action`：`type` 1URL 2流程操作 3接口。
- `<wf:operation btnName btnOrder actionType enabled/>` + 子 `<wf:right rightType rightValue/>`（0..n）→ `wf_custom_operation`(+`wf_custom_operation_right`)：`actionType` 1URL 2流程操作 3接口。
- `<wf:extJson><![CDATA[...]]></wf:extJson>`（0..1）→ 未结构化字段的自由 JSON（如 remind 配置）。

### 4.2 `wf:link`（sequenceFlow 扩展，替代 wf_node_link）

| 属性 | 含义 |
|---|---|
| `isReject` | 0/1 是否驳回线 |
| `isMustPass` | 0/1 必经 |
| `conditionCn` | 条件中文描述 |
| `sortOrder` | 排序 |
| `viaGateway` | 0/1 经网关 |

**子元素**：`<wf:extraOperations><![CDATA[...]]></wf:extraOperations>`（0..1，出口附加操作脚本）。

> 条件表达式**仍写原生** `<conditionExpression>`（不进 `wf:link`），与扩展互不干扰。

### 4.3 `wf:processMeta`（process 扩展，替代 wf_process_definition / wf_workflow_type / wf_definition_gray 部分字段）

| 属性 | 含义 | 对应 |
|---|---|---|
| `defKey` | 业务定义 key（**D6 桥接**：业务 defId ↔ 引擎 KEY_+版本） | 新增 DEF_KEY_ |
| `workflowType` | 流程分类 id | wf_workflow_type |
| `formId` | 主表单 id（替代 wf_process_definition.form_id） | wf_process_definition.form_id |
| `layoutId` | 布局 id | wf_process_definition |
| `grayEnabled` | 0/1 灰度 | wf_definition_gray |
| `grayRule` | 灰度规则 | wf_definition_gray |

## 5. 设计约束（评审确认）

- 定义期语义**唯一载体**是 BPMN `extensionElements`，随 `ACT_GE_BYTEARRAY` 持久化/版本化；不再写 `wf_process_node`/`wf_node_*` 等 16 张定义期表。
- 导入器（`WfDefinitionServiceImpl`）必须**解析 `wf:` 扩展**并据此构建运行期所需语义，**不再**自动建 `wf_*` 行（修复技能文档所述"仅文档性"现状）。
- 前端 moddle（F-T1）与后端 `BpmnExtensionUtil` **共用本契约**；元素名/属性名/命名空间任一变更须同步三处（模板、util、moddle）并跑通往返测试。

## 6. 与原技能模板的差异（迁移须知）

技能 `assets/template.bpmn20.xml` 与 `springblade-mapping.md` 当前用 `wf:operators`/`wf:customOperations`/`wf:fieldPerms` 分容器写法，且 `wf:` 仅文档性。本定稿改为 `wf:node` 包装 + 全量语义 + 强制解析。后续更新技能模板与映射文档以对齐本契约。
