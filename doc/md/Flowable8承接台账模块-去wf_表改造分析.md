# 基于 Flowable 承接台账模块并去除 `wf_*` 表的改造分析

> **引擎版本**：本文所述引擎为 **Flowable `8.1.0-SNAPSHOT`**（据 `springBlade/pom.xml`：`<flowable.version>8.1.0-SNAPSHOT</flowable.version>`），且为**本地源码构建**（源码 `D:\workproject\springbladeandreact\flowable-engine`，已安装到 `E:\project\mavenLib`，构建需 `-Dmaven.repo.local=E:\project\mavenLib`）。全文统一以该版本为准。
> - ⚠️ **pom 已标注的既有风险**：`8.1.0-SNAPSHOT` 的 `CURRENT_VERSION=8.1.0.1`，而库内 `schema.version=8.0.0.0`；官方尚无 `8.0.0→8.1.0` 升级脚本，且 `databaseSchemaUpdate=none` 会**跳过版本校验** ⇒ 可启动但存在**静默 schema 不一致风险**（pom 注释原文）。**本文 §11 的加列方案会叠加在这层既有的版本不一致之上，须特别留意。**
> - 回退 GA：pom 注释说明"改回 `8.0.0` 即可 —— 库已升级至 `8.0.0.0`，版本一致"。**建议在执行 §11 加列前先确认该版本一致性问题已处置。**
>
> 🔴 **对"是否改源码"权衡的修正**：本文 §12.4 曾以"需重新构建并发布 `flowable-engine`"作为反对改源码的理由之一。**该理由不成立** —— 工程**已经在用本地源码构建**，构建与安装流程已存在，改源码的发布成本很低。反对改源码的**剩余且仍然成立**的理由是：一致性风险、`getPersistentState()` 等经典坑、多方言 mapper/DDL 维护、以及**回归面**（见 §12.10.1 方案 4）。
>
> 🏢 **多业务域共用约定**：电商（`blade-mall` / `blade-order` / `blade-pay`）等**其它行业模块共用同一套引擎与 `ACT_*` 表**。隔离规范见 **§十三**：`ACT_*` 为 **workflow 专用**、按 **`TENANT_ID_`** 隔离、异步执行器**按需隔离**。

---

## 文档导航（快速入口）

| 想找什么 | 看哪一章 |
|---|---|
| **整体结论 / 一页纸速查** | §1.2 核心结论、**§17.5 一页纸速查** |
| `wf_*` 表全景（22 张） | §二 |
| 能力差异（`wf_*` vs 引擎原生） | §三 |
| 数据表替换方案 / 加列 DDL | §四、**§十一**（实测 + DDL + 索引 + 回填） |
| 代码改造点（按模块） | §五、**§12.10.1**（状态写入治本方案） |
| 迁移步骤 / 风险 | §六、**§15.3**（迁移遗漏） |
| 分期与任务拆解 | §七、**§15.12 / §17.4** |
| "列表化"提升检索效率 | §八、**§12.10.2** |
| 多实例 / 加签 / 跳转 启用路线 | §九、**§12.8.1**（状态写入）、**§12.8.3**（定时器） |
| 源码可改 / 不可改边界 | **§12.0**、§12.4（改源码 vs 扩展点） |
| 去 `wf_*` 的连锁影响 / 隐性依赖 | **§12.3** |
| 多业务域共用与隔离 | **§十三**（含 §13.8 租户列覆盖实测） |
| 高并发与检索性能 | **§十四** |
| 遗漏排查（九维度交叉验证） | **§十五** |
| 前端改造 | **§十六** |
| 待确认 / 验证点 / 风险 登记表 | **§十七**（§17.1 / §17.2 / §17.3） |

> 📌 **评审最短路径**：先看 **§17.5 一页纸速查** → 再按 **§17.1（D 系列）** 拍板 → 按 **§17.4（任务总表）** 派活 → 按 **§17.2（V 系列）** 排验证。

> 目标：以 **Flowable 8.1.0-SNAPSHOT 现有能力（必要时扩展/修改其源码）**重新实现当前"台账模块"的功能，**不再使用任何以 `wf_` 开头的表**。
> 本文覆盖：功能差异、需调整的代码逻辑、数据表替换方案、迁移注意事项。

---

## 一、背景、目标与核心结论

### 1.1 目标
把当前由 `wf_*` 侧表（共 22 张）承载的"流程定义语义 + 运行期台账"能力，迁移到以 Flowable 8.1.0-SNAPSHOT 为核心的承载方式上，且**不保留任何 `wf_` 前缀表**。

### 1.2 核心结论（先给判断，避免误读）

| # | 结论 | 说明 |
|---|---|---|
| 1 | **定义期语义可以真·去表** | 节点 / 出口 / 操作者 / 字段权限 / 超时规则 → 全部写入 BPMN `extensionElements`（随 `ACT_GE_BYTEARRAY` 持久化、版本化）。已验证 Flowable 8.1.0-SNAPSHOT 可完整解析与写回自定义命名空间扩展。 |
| 2 | **运行期台账不可能进 BPMN** | `wf_instance` / `wf_task` / `wf_approval_log` 是**运行态数据**，BPMN 是**定义制品**。运行态只能落 `ACT_RU_*` / `ACT_HI_*` 原生表，或另建**非 `wf_` 前缀**的表。 |
| 3 | **去 `wf_task` ⟹ 会签必须改为引擎多实例** | 当前会签是「单个 userTask + `wf_task` 多条待办」自研实现；`ACT_RU_TASK` **无法**表达"一条引擎任务、N 个办理人各自独立状态/到期时间"。故去掉 `wf_task` 后，会签 / 或签 / 依次**必须**改用 Flowable `multiInstanceLoopCharacteristics`（引擎为每个办理人创建独立任务）。这是本次改造**最硬的依赖与最大风险**。 |
| 4 | **方案C（事件驱动台账投影）随之退役** | 一旦引擎即台账、不存在第二份副本，则"引擎 vs 台账漂移"这个问题**在定义上消失**，`WfStateProjector` / `WfEngineEventListener` / `WfWriteHelper` 及漂移治理设施可整体退役——这是本次改造**最大的收益**。<br>⚠️ **但须区分两类漂移**（详见 §12.9）：本条只消灭**类型 A（引擎 vs 台账）**；**类型 B（异步作业 vs 业务副作用）**与台账是否存在无关，开启异步执行器后依然存在，仍需死信告警与作业监控。 |
| 5 | **纯靠 `ACT_*` 会丢失高效检索能力** | 业务字段若只存流程变量，跨实例按业务字段检索（`ACT_HI_VARINST` 按 name/value 查）**性能差且无有效索引**。建议保留**非 `wf_` 前缀的只读投影索引表**（单向、可重建），它**不是事实源**，故不产生漂移。 |

> ⚠️ **关于"不再使用 `wf_` 表"的诚实提醒**：若只是把 `wf_instance` 改名为 `bpm_instance` 而架构不变（仍是双写事实源），**收益为零**。真正的收益来自"**引擎成为唯一事实源**"。因此本文推荐的是：**引擎为事实源 + 非 `wf_` 只读投影索引**。

---

## 二、现状盘点：`wf_*` 表全景（22 张）

### 2.1 定义期 / 设计期语义（可下沉 BPMN）
| 表 | 作用 |
|---|---|
| `wf_process_definition` | 流程定义（含 bpmnXml、版本、灰度） |
| `wf_process_node` | 节点信息（nodeType / signOrder / mergeType / passNum / allowReject / allowForward / autoApprove / extJson） |
| `wf_node_link` | 出口信息（conditionExpr / isReject / isMustPass / extraOperations / viaGateway） |
| `wf_node_operator` | 节点操作者（规则，多行/节点） |
| `wf_node_field_perm` | 节点级字段权限矩阵 |
| `wf_node_detail_perm` | 明细表权限 |
| `wf_node_detail_filter` | 明细表字段筛选 |
| `wf_node_timeout` | 节点超时规则（多条） |
| `wf_node_default_sign` | 按操作类型默认签字意见 |
| `wf_custom_action` / `wf_custom_operation` / `wf_custom_operation_action` / `wf_custom_operation_right` | 自定义接口动作 / 操作按钮 / 动作明细 / 权限矩阵（4 张） |
| `wf_definition_gray` | 定义灰度规则 |
| `wf_workflow_type` | 流程（路径）分类 |

### 2.2 运行期台账（BPMN 装不下，核心改造对象）
| 表 | 作用 |
|---|---|
| `wf_instance` | 流程实例台账（status / current_node_key / data_id / form_id / title / start_user / is_test） |
| `wf_task` | 任务待办台账（assignee / status / node_key / due_time / timeout_handled / is_test / engine_task_id） |
| `wf_approval_log` | 审批流转记录（log_type / 意见 / 操作人 / 时间） |
| `wf_form_snapshot` | 表单数据快照 |
| `wf_subflow_request` | 主 / 子流程请求关系 |

### 2.3 迁移与测试辅助
| 表 | 作用 |
|---|---|
| `wf_migration_map` | ecology 存量迁移映射（一次性，迁移后可弃） |
| `wf_test_log` | 流程测试日志 |

---

## 三、功能差异分析（当前 `wf_*` 能力 vs Flowable 8.1.0-SNAPSHOT 原生能力）

### 3.1 定义期语义：节点 / 出口 / 操作者

| 能力 | `wf_*` 现状 | Flowable 8.1.0-SNAPSHOT 原生 | 差异 / 处理 |
|---|---|---|---|
| 节点属性 | `wf_process_node` 行 | BPMN `extensionElements` + `<wf:node>` | **无功能差异**，载体变更；且随定义版本化（优于现状） |
| 出口条件 | `wf_node_link.conditionExpr`，部署期 `injectLinkConditions` 注入 | `SequenceFlow.conditionExpression` 原生 | **能力增强**：免注入，消除注入漂移 |
| 出口语义（isReject / isMustPass / extraOperations） | `wf_node_link` 列 | `<wf:link>` 扩展 | 无差异 |
| 网关折叠（A→网关→B） | `via_gateway` 标记 | BPMN 网关结构可推导 | 需设计锚定规则（锚定网关出边） |
| 操作者**规则** | `wf_node_operator` 多行 | `<wf:operator>` 嵌套扩展 | 无差异；**规则**可入 BPMN |
| 操作者**解析结果** | 运行期 `WfOperatorResolver` 计算 | 运行期计算 | **不可入 BPMN**（依赖发起人 / 表单数据 / 组织架构 / 外部接口） |
| 字段权限 / 明细权限 | 独立表 | 嵌套扩展 | 无差异 |

### 3.2 运行期实例台账 `wf_instance`

| `wf_instance` 字段/能力 | Flowable 8.1.0-SNAPSHOT 承载 | 差异 |
|---|---|---|
| 实例存在 / 运行中 | `ACT_RU_EXECUTION` / `ACT_HI_PROCINST` | ✅ 对等 |
| 发起人与发起时间 | `ACT_HI_PROCINST.START_USER_ID_` / `START_TIME_` | ✅ 对等 |
| 结束时间与耗时 | `ACT_HI_PROCINST.END_TIME_` / `DURATION_` | ✅ 对等 |
| **当前节点** | `ACT_RU_TASK.TASK_DEF_KEY_` 或 `ACT_RU_EXECUTION.ACT_ID_` | ✅ 可推导（= nodeKey），**无需冗余字段** |
| `def_id`（业务定义ID） | `ACT_RE_PROCDEF.ID_` | ✅ 对等，但业务侧自定义 defId 需经 `businessKey` 或变量桥接 |
| `data_id` / `form_id`（表单数据ID） | **流程变量** | ⚠️ 需转为变量；跨实例检索能力下降 |
| `title`（标题） | **流程变量** 或 `ACT_HI_PROCINST.NAME_` | ⚠️ 同上 |
| **`status`**（审批中/通过/不通过/撤销/暂停） | ✅ **`ACT_HI_PROCINST.BUSINESS_STATUS_` 原生列**（见 §4.4 修正） | ✅ **已有原生承载**：`BUSINESS_STATUS_` + `SetProcessInstanceBusinessStatusCmd` + `HistoricProcessInstanceQuery.processInstanceBusinessStatus()`；⚠️ 默认无索引，须补索引 |
| `is_test`（测试态隔离） | **流程变量** | ⚠️ 转为变量；测试态隔离逻辑需按变量过滤（影响所有查询） |
| 暂停 / 恢复 | `ACT_RU_EXECUTION.SUSPENSION_STATE_` | ✅ 对等（原生 suspend/activate） |

### 3.3 运行期任务台账 `wf_task`（**差异最大、风险最高**）

| `wf_task` 字段/能力 | Flowable 8.1.0-SNAPSHOT 承载 | 差异 |
|---|---|---|
| 待办存在 / 办理人 | `ACT_RU_TASK.ASSIGNEE_` | ✅ 对等 |
| 任务名称 / 创建时间 | `ACT_RU_TASK.NAME_` / `CREATE_TIME_` | ✅ 对等 |
| 节点 key | `ACT_RU_TASK.TASK_DEF_KEY_` | ✅ 对等 |
| **到期时间** | `ACT_RU_TASK.DUE_DATE_` | ✅ **原生支持**（优于现状） |
| 办结 / 历史 | `ACT_HI_TASKINST` | ✅ 对等 |
| `is_test` | **任务/流程变量** | ⚠️ 转变量 |
| `timeout_handled` | 可用"任务已办结即离开 `ACT_RU_TASK`"自然替代 | ⚠️ 需重新设计超时扫描判据（见 §5.6） |
| 🔴 **会签 / 或签 / 依次：一条引擎任务 vs 一人一条待办** | `ACT_RU_TASK` **只能是一人一任务**；多实例模式下引擎为每个办理人建独立任务 | 🔴 **根本性差异**：当前「单 userTask + `wf_task` 多条待办」**无法**用 `ACT_RU_TASK` 直接表达。**必须**切换到引擎多实例（`multiInstanceLoopCharacteristics`），否则会签能力丧失 |

> **会签改造的连锁影响**：开启多实例后，业务侧自研的运行时计数（`WfTaskServiceImpl#doApprove` 的 `countPending` / `closeSiblings` / `nextPending`）须由引擎的"分叉 / 汇聚 / 放行"接管。这是本次改造中**代码改动量最大、风险最高**的部分。

### 3.4 审批流转日志 `wf_approval_log` → `ACT_HI_COMMENT`

| `wf_approval_log` | Flowable 8.1.0-SNAPSHOT | 差异 |
|---|---|---|
| 意见内容 | `ACT_HI_COMMENT.MESSAGE_` / `FULL_MSG_` | ✅ 对等 |
| 操作人 / 时间 | `USER_ID_` / `TIME_` | ✅ 对等 |
| 关联任务 / 实例 | `TASK_ID_` / `PROC_INST_ID_` | ✅ 对等 |
| `log_type`（提交/审批/退回/撤销/转发/催办） | `TYPE_` + `ACTION_` 自定义取值（如 `TYPE_='wfLog'`, `ACTION_=<logType>`） | ✅ 可承载，需约定取值规范 |
| 长文本 / 富文本 | `FULL_MSG_` (bytes) | ✅ 对等 |

### 3.5 表单快照 `wf_form_snapshot`
- `ACT_HI_VARINST` 可存变量历史，但**表单数据体积大、结构复杂**，不适合塞进变量表。
- **建议**：保留**非 `wf_` 前缀**的快照表（如 `flow_form_snapshot`），或接入外部存储（对象存储 / 文档库）。
- 这是少数**建议保留独立表**的场景之一（见 §4.5）。

### 3.6 其他表
| 表 | 替换建议 | 说明 |
|---|---|---|
| `wf_subflow_request` | `ACT_HI_PROCINST.SUPER_PROCESS_INSTANCE_ID_` + 变量 | ✅ 原生支持父子流程 |
| `wf_workflow_type` | `ACT_RE_PROCDEF.CATEGORY_` 或 非 `wf_` 表 | 分类建议用 `CATEGORY_` |
| `wf_definition_gray` | 非 `wf_` 表（平台级配置，非 BPMN 语义） | 灰度属平台配置，不适合入 BPMN |
| `wf_custom_*`（4 张） | 非 `wf_` 表 | 自定义操作属平台字典级能力，建议独立 |
| `wf_node_default_sign` | BPMN 扩展 或 非 `wf_` 表 | 二选一 |
| `wf_migration_map` | 迁移期临时，迁移完成后弃用 | — |
| `wf_test_log` | 非 `wf_` 表 或 外部日志 | — |

### 3.7 能力差异汇总

| 维度 | 结论 |
|---|---|
| ✅ 可完全替代、且更强 | 出口条件（原生）、到期时间（原生）、暂停/恢复（原生）、父子流程（原生）、当前节点（可推导）、审批意见（`ACT_HI_COMMENT`） |
| ⚠️ 需转为流程变量（有检索代价） | `data_id` / `form_id` / `title` / `is_test` |
| ✅ 原生已提供（本次修正） | **业务终态** → `BUSINESS_STATUS_` 原生列（原判断"必须自建变量"**错误**，经 Flowable 源码验证已修正，见 §4.4） |
| 🔴 架构性变更（硬依赖） | **会签 / 或签 / 依次 → 引擎多实例** |
| ❌ 不适合入 BPMN，建议保留非 `wf_` 表 | 表单快照、自定义操作、灰度、测试日志、默认签字意见 |

---

## 四、数据表替换方案

### 4.1 总体映射

| 原 `wf_*` 表 | 替换承载 | 是否还需建表 |
|---|---|---|
| `wf_process_definition` | `ACT_RE_DEPLOYMENT` / `ACT_RE_PROCDEF` / `ACT_GE_BYTEARRAY` | ❌ 否 |
| `wf_process_node` | BPMN `<wf:node>` 扩展 | ❌ 否 |
| `wf_node_link` | BPMN `<wf:link>` + 原生 `conditionExpression` | ❌ 否 |
| `wf_node_operator` | BPMN 嵌套 `<wf:operator>` | ❌ 否 |
| `wf_node_field_perm` / `wf_node_detail_perm` / `wf_node_detail_filter` | BPMN 嵌套扩展 | ❌ 否 |
| `wf_node_timeout` | BPMN 扩展（规则） | ❌ 否 |
| `wf_instance` | `ACT_HI_PROCINST` / `ACT_RU_EXECUTION` + **流程变量** | ❌ 否（见 §4.5 投影索引） |
| `wf_task` | `ACT_RU_TASK` / `ACT_HI_TASKINST` + **变量** | ❌ 否（见 §4.5） |
| `wf_approval_log` | `ACT_HI_COMMENT` | ❌ 否 |
| `wf_subflow_request` | `ACT_HI_PROCINST.SUPER_PROCESS_INSTANCE_ID_` | ❌ 否 |
| `wf_form_snapshot` | **建议** 非 `wf_` 表（如 `flow_form_snapshot`） | ✅ 建议 |
| `wf_workflow_type` | `ACT_RE_PROCDEF.CATEGORY_` | ❌ 否 |
| `wf_definition_gray` / `wf_custom_*` / `wf_node_default_sign` / `wf_test_log` | 非 `wf_` 表 | ✅ 建议 |
| `wf_migration_map` | 迁移完成后弃用 | — |

### 4.2 定义期：BPMN `extensionElements` 组织方式（无表）

```xml
<bpmn:userTask id="approve1" name="部门经理审批">
  <bpmn:extensionElements>
    <wf:node nodeType="1" signOrder="1" mergeType="0" passNum="0"
             allowReject="1" allowForward="1" autoApprove="0" sortOrder="2">
      <wf:extJson><![CDATA[{...超时/提醒/签章...}]]></wf:extJson>
      <wf:operator groupNo="0" opType="1" objId="10" bhxj="1" levelMin="0" levelMax="99"/>
      <wf:operator groupNo="1" opType="17"/>
      <wf:fieldPerm field="amount" perm="edit"/>
    </wf:node>
  </bpmn:extensionElements>
</bpmn:userTask>

<bpmn:sequenceFlow id="flow1" sourceRef="approve1" targetRef="end1">
  <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${amount &gt; 1000}</bpmn:conditionExpression>
  <bpmn:extensionElements>
    <wf:link isReject="0" isMustPass="1" conditionCn="金额大于1000"/>
  </bpmn:extensionElements>
</bpmn:sequenceFlow>
```

**已验证的可行性依据**（Flowable 8.1.0-SNAPSHOT 源码 `BpmnXMLUtil`）：
- `parseExtensionElement`：通用解析 name / namespace / prefix / 属性 / 文本 / CDATA / **嵌套子元素递归**；
- `writeExtensionElements`：通用写回，含命名空间声明；
- ⇒ 自定义命名空间扩展**可完整往返**，扛得住部署链路的 `convertToBpmnModel → convertToXML`。

**⚠️ 命名陷阱**：解析分发按 **localName** 判定（`localParserMap.containsKey(xtr.getLocalName())`）。自定义元素名**不得**撞上已注册的 BPMN 子元素名（`condition` / `documentation` / `timerEventDefinition` / `executionListener` / `taskListener` / `formProperty` 等），否则会被当作 Flowable 原生子元素解析而**语义被吞**。

### 4.3 运行期：`ACT_*` 原生表承载

- 运行中：`ACT_RU_EXECUTION` / `ACT_RU_TASK` / `ACT_RU_VARIABLE`
- 历史：`ACT_HI_PROCINST` / `ACT_HI_TASKINST` / `ACT_HI_VARINST` / `ACT_HI_COMMENT`
- 定义：`ACT_RE_PROCDEF`（含 `CATEGORY_` 作分类）、`ACT_GE_BYTEARRAY`（BPMN XML + 扩展）

### 4.4 业务字段：流程变量的组织约定

建议统一命名前缀，避免与引擎变量冲突：

| 业务字段 | 变量命名 | 作用域 |
|---|---|---|
| `data_id` | `wfDataId` | 实例 |
| `form_id` | `wfFormId` | 实例 |
| `title` | `wfTitle` | 实例 |
| `is_test` | `wfIsTest` | 实例 |
| ~~**业务终态**~~ | ❌ **不再用变量**，改用原生列（见下方说明） | 实例 |
| 超时已处理 | 由"任务离开 `ACT_RU_TASK`"自然表达 | 任务 |

### ⚠️ 重要修正：业务终态**无需自建变量**，Flowable 8.1.0-SNAPSHOT 已有原生列

最初本文判断"Flowable 无业务终态概念，必须用 `wfBizStatus` 变量承载"。**经 Flowable 8.1.0-SNAPSHOT 源码验证，该判断错误，现修正如下**：

| 原生能力 | 证据（Flowable 8.1.0-SNAPSHOT 源码 / DDL） |
|---|---|
| `ACT_HI_PROCINST.BUSINESS_STATUS_` 列 | `flowable.mysql.create.history.sql` DDL 实证 |
| `ACT_RU_EXECUTION.BUSINESS_STATUS_` / `DUE_DATE_` / `CLAIM_TIME_` / `CLAIMED_BY_` 列（执行流级） | `flowable.mysql.create.engine.sql` DDL + `blade` 库实测 |
| ⚠️ **`ACT_RU_TASK` 无 `BUSINESS_STATUS_`** | `blade` 库 `act_ru_task` 实测 37 列，末列 `SUB_TASK_COUNT_`，**无该列**（先前误记为任务级，已修正） |
| `HistoricProcessInstanceEntity.businessStatus` 持久化字段 | `HistoricProcessInstanceEntityImpl` |
| 更新命令 `SetProcessInstanceBusinessStatusCmd` | 存在 |
| 查询 API `HistoricProcessInstanceQuery.processInstanceBusinessStatus()`（精确 / Like / LikeIgnoreCase） | `HistoricProcessInstanceQuery` |
| 变更事件 `FlowableProcessBusinessStatusUpdatedEvent` | 存在 |

**⇒ 结论**：业务终态（通过 / 不通过 / 撤销）应直接使用**原生 `BUSINESS_STATUS_` 列**，取值建议 `APPROVED` / `REJECTED` / `CANCELED`，经由 `SetProcessInstanceBusinessStatusCmd` 更新、经 `HistoricProcessInstanceQuery` 查询。**不再需要 `wfBizStatus` 流程变量**。

> ⚠️ **但列化 ≠ 自动变快**：`ACT_HI_PROCINST` **默认没有 `BUSINESS_STATUS_` 索引**（该表默认仅 `END_TIME_` / `BUSINESS_KEY_` / `SUPER_PROCESS_INSTANCE_ID_` 三个索引）。改用原生列后**必须自行补索引**才能真正拿到检索收益——这是最易被忽略的一步。

### 4.5 检索能力缺口与非 `wf_` 投影索引表（**建议保留**）

**问题**：业务字段若只存流程变量，跨实例检索（"查 title 含 X 的实例"、"查某 dataId 的所有流程"）需查 `ACT_HI_VARINST`，**无有效索引、性能差**，且无法与业务表 join。

**建议**：保留**非 `wf_` 前缀的只读投影索引表**，仅作查询加速，**不是事实源**：

```sql
-- 实例投影索引（只读、可由 ACT_* + 变量完全重建）
CREATE TABLE flow_inst (
  id              BIGINT PRIMARY KEY,
  proc_inst_id    VARCHAR(64) NOT NULL UNIQUE,   -- ACT_HI_PROCINST.PROC_INST_ID_
  proc_def_id     VARCHAR(64),
  def_id          BIGINT,                        -- 业务定义ID（桥接）
  title           VARCHAR(500),
  data_id         BIGINT,
  form_id         BIGINT,
  biz_status      VARCHAR(20),                   -- 业务终态
  current_node_key VARCHAR(255),
  start_user      BIGINT,
  start_time      DATETIME,
  end_time        DATETIME,
  is_test         TINYINT NOT NULL DEFAULT 0,
  KEY idx_def (def_id), KEY idx_data (data_id),
  KEY idx_startuser (start_user), KEY idx_istest (is_test)
);

-- 任务投影索引
CREATE TABLE flow_task (
  id           BIGINT PRIMARY KEY,
  task_id      VARCHAR(64) NOT NULL UNIQUE,      -- ACT_RU_TASK.ID_
  proc_inst_id VARCHAR(64),
  node_key     VARCHAR(255),
  assignee     BIGINT,
  status       TINYINT,
  due_time     DATETIME,
  create_time  DATETIME,
  is_test      TINYINT NOT NULL DEFAULT 0,
  KEY idx_assignee (assignee), KEY idx_inst (proc_inst_id), KEY idx_due (due_time)
);
```

**关键约束（决定它不产生漂移）**：
- 单向：只由引擎 / 事件写入，业务代码**不得**作为事实源写入；
- 可重建：任何时刻可从 `ACT_*` + 变量全量重建；
- 不一致时以 `ACT_*` 为准，投影仅作加速。

> 若要求"**完全不建任何侧表**"，则须接受：跨实例业务字段检索退化为变量查询、无 join、报表能力显著下降。

### 4.6 关于"修改 Flowable 8.1.0-SNAPSHOT 源码"的取舍

工作区内有 `flowable-engine` 源码，理论上可改。**但不建议改核心**：
- 升级维护成本高（每次 Flowable 升级需重新合入）；
- 本文所需能力（扩展元素、变量、comment、多实例）**均已由扩展点覆盖**，无需改源码。

**仅在这些场景才考虑改源码**：
- 需要在 `ACT_RU_TASK` / `ACT_HI_*` 上增加**高频检索的自定义列**；
- 需要引擎内部行为定制（如自定义任务分配策略、自定义历史写入）。
即便如此，也优先用"投影索引表"替代改源码。

---

## 五、需要调整的代码逻辑（按模块）

| 模块 | 现状 | 需调整的逻辑 |
|---|---|---|
| **`FlowableConfig`** | `asyncExecutorActivate=false`、`history=audit`、`isFailOnException=true` | 会签改多实例后需复核；历史级别保持 `audit`（确保 `ACT_HI_COMMENT` / `ACT_HI_TASKINST` 落库） |
| **`WfDefinitionServiceImpl`** | 导入 BPMN → upsert `wf_*`；部署期 `injectLinkConditions` 注入条件；`multiInstanceEnabled` 默认关 | ① 导入改为解析 BPMN 扩展（不再写 `wf_*`）；② **移除 `injectLinkConditions`**（条件已在 BPMN）；③ **开启 `multiInstanceEnabled`**（会签硬依赖）；④ 保留 `neutralizeForDeploy` / `validateForDeploy` |
| **`WfInstanceServiceImpl`** | 写 `wf_instance`、读 `wf_instance` | 改为写/读 `ACT_*`；**业务终态用原生 `BUSINESS_STATUS_` 列**（`SetProcessInstanceBusinessStatusCmd`），其余业务字段用变量（`wfDataId` / `wfFormId` / `wfTitle` / `wfIsTest`）；发起时设置 `businessKey` 便于回查 |
| **`WfTaskServiceImpl`** 🔴 | 自研会签计数 `countPending` / `closeSiblings` / `nextPending`；写 `wf_task` 多条 | **改动最大**：移除自研计数，改由引擎多实例分叉/汇聚；待办读写改 `ACT_RU_TASK` / `ACT_HI_TASKINST` |
| **`WfStateProjector` / `WfEngineEventListener` / `WfWriteHelper`** | 方案C：引擎事件 → 反写 `wf_*` 台账 | **整体退役**（引擎即台账，无第二副本即无漂移）；或仅保留用于**刷新投影索引表** |
| **`WfApprovalLog` 相关** | 写 `wf_approval_log` | 改为 `taskService.addComment(...)` / `ACT_HI_COMMENT`；`log_type` 用 `ACTION_` 字段约定 |
| **`WfTimeoutJob` / `WfTimeoutServiceImpl`** | 扫 `wf_task`（`due_time <= now AND timeout_handled=0`） | 改为扫 `ACT_RU_TASK.DUE_DATE_`；`timeout_handled` 判据改为"任务仍在 `ACT_RU_TASK` 且未办结"；**必须保留 `is_test` 过滤**（改为变量过滤） |
| **`WfOperatorResolver`** | 读 `wf_node_operator` 规则 → 运行期解析 | 输入改为读 BPMN 扩展规则；**解析逻辑本身不变**（结果仍运行期算） |
| **权限 / 字段权限** | 读 `wf_node_field_perm` | 改为读 BPMN 扩展 |
| **`WfMonitorController` / 对账脚本** | 漂移检查（6a/6b/6c） | 台账漂移检查**退役**；改为校验"投影索引 vs `ACT_*`"的一致性 |
| **Entity / Mapper 层** | 22 个 `wf_*` Entity + Mapper | 定义期 Entity 删除（改 BPMN 读写）；运行期 Entity 改为投影索引 Entity（`flow_*`）或非 `wf_` 表 |
| **测试态隔离（`is_test`）** | `wf_task.is_test` / `wf_instance.is_test` 列过滤 | 改为**流程变量过滤**，所有查询需加变量条件（影响面广，需逐一排查） |

---

## 六、迁移注意事项

### 6.1 迁移步骤（建议顺序）
1. **前置**：完成会签运行时门禁改造（多实例放行 / 汇聚），否则步骤 4 无法开启。
2. **定义期下沉**：存量 `wf_process_node` / `wf_node_link` / `wf_node_operator` / 权限 / 超时 → 回填为 BPMN 扩展（幂等脚本，可重跑）。
3. **双轨校验**：BPMN 扩展解析结果 vs 存量 `wf_*`，**只告警不改行为**，确认口径一致。
4. **开启多实例** + 运行期切换：`wf_task` → `ACT_RU_TASK`（会签语义切换，**最高风险步骤**）。
5. **实例台账切换**：`wf_instance` → `ACT_*` + 变量；建立投影索引表。
6. **日志切换**：`wf_approval_log` → `ACT_HI_COMMENT`。
7. **存量数据迁移**：历史实例 / 任务 / 日志迁移到 `ACT_HI_*`（**注意**：引擎历史表结构与业务表不同，需字段映射；无法映射的业务字段入变量或投影表）。
8. **退役**：删除 `wf_*` 相关 Entity/Mapper/Service 分支与方案C 投影设施。

### 6.2 数据迁移要点
- **定义期**：可程序化回填（读 `wf_*` → 写 BPMN 扩展 → 重新部署生成新版本）。**必须版本化保留旧定义**，避免存量在途实例找不到定义。
- **运行期在途实例**：**最难点**。`wf_instance` / `wf_task` 的存量行需映射为 `ACT_HI_*` 记录；但引擎历史表由引擎写入，手工插入风险高。
  - 建议：**在途实例不迁移数据，而是迁移"关联关系"**——用 `businessKey` 或投影表保留 `wf_instance.id → procInstId` 映射，历史查询走投影表；新实例一律走引擎。
  - 已结束的历史数据：保留在**只读归档表**（非 `wf_` 命名）或迁移到 `ACT_HI_*`。
- **`wf_approval_log` → `ACT_HI_COMMENT`**：字段可映射（`log_type` → `ACTION_`），但 `ACT_HI_COMMENT` 由引擎写入，手工插入需确保 `PROC_INST_ID_` / `TASK_ID_` 与引擎记录一致，否则关联断裂。

### 6.3 双轨与回退
- 全程**开关控制**（如 `blade.flowable.ledger-native.enabled`），默认走旧链路。
- 双轨期：**新旧并存**，对账只告警；确认一致后再切流量。
- 回退：关开关即回 `wf_*` 链路；因定义期已回填 BPMN 扩展，回退不影响（扩展是新增、不删旧表）。
- **重要**：多实例一旦开启并对存量会签流程生效，**回退成本高**（引擎任务结构变化）。务必先灰度。

### 6.4 风险清单

| 风险 | 等级 | 缓解 |
|---|---|---|
| **会签改多实例**引发运行时失败（缺集合变量等） | 🔴 高 | 先完成运行时门禁；充分测试或签/会签/依次 + 驳回回多实例节点 |
| 存量在途实例与引擎历史表映射不一致 | 🔴 高 | 不迁移在途数据，改用映射关系 + 投影表 |
| `is_test` 改为变量后，查询漏过滤导致测试数据外溢 | 🔴 高 | 逐一排查所有查询；加自动化断言 |
| 业务终态（通过/不通过/撤销）表达不一致 | 🟠 中 | 统一用**原生 `BUSINESS_STATUS_` 列**与取值规范（`APPROVED`/`REJECTED`/`CANCELED`），全口径统一读取 |
| 扩展元素名撞 Flowable 已注册名，语义被吞 | 🟠 中 | schema 定稿时逐一核对；补往返保真测试 |
| BPMN 往返序列化丢失扩展 | 🟠 中 | 补"解析→写回→断言无损"测试（防 Flowable 升级行为变化） |
| 去 `wf_*` 后跨实例检索性能下降 | 🟠 中 | 保留非 `wf_` 投影索引表 |
| 表单快照塞变量表撑大 `ACT_HI_VARINST` | 🟡 低 | 快照保留独立非 `wf_` 表或外部存储 |

### 6.5 前置依赖（必须完成才能开工）
1. **会签运行时门禁改造**（多实例放行 / 汇聚 / 驳回回多实例节点）—— 去 `wf_task` 的硬前提。
2. **超时扫描判据改造**（改扫 `ACT_RU_TASK.DUE_DATE_` + `is_test` 变量过滤）。
3. **BPMN 扩展 schema 定稿** + 往返保真测试。

---

## 七、分期建议

| 期 | 内容 | 风险 | 可独立交付 |
|---|---|---|---|
| **P1** | 定义期下沉（节点/出口/操作者/权限/超时 → BPMN 扩展）+ 回填迁移 + 双轨对账（只读告警） | 低 | ✅ |
| **P2** | 会签运行时门禁改造（多实例放行/汇聚/驳回）—— **P3 的前置** | 中 | ✅ |
| **P3** | 开启多实例 + `wf_task` → `ACT_RU_TASK` 切换（**最高风险**） | 🔴 高 | 需 P2 |
| **P4** | `wf_instance` → `ACT_*` + 变量；建非 `wf_` 投影索引表 | 中 | 需 P3 |
| **P5** | `wf_approval_log` → `ACT_HI_COMMENT`；表单快照独立表 | 中 | ✅ |
| **P6** | 存量迁移 + 退役 `wf_*` Entity/Mapper/Service 与方案C 投影设施 | 中 | 需 P1–P5 |

---

## 八、原生语义的"列表化"改造（提升检索效率）

> 本节回应："Flowable 8.1.0-SNAPSHOT 的原生语义模型（运行时/历史表结构、变量存储、任务查询语义）能否从实体关系设计改为列表（列式/扁平化）形式？"

### 8.1 "列表形式"的三种含义（必须先区分，收益与代价完全不同）

| 形态 | 定义 | 举例 |
|---|---|---|
| **A 宽表化** | 需 JOIN 多张 `ACT_*` 才能拼出的"台账一行" → 预展开为**单表的多个真实列** | `flow_inst` 一行含 `title`/`data_id`/`biz_status`/`current_node_key`… |
| **B 变量列化** | `ACT_RU_VARIABLE` 中**一行一个变量** → 提升为宿主表的**真实列** | 业务状态 → `BUSINESS_STATUS_` |
| **C 定义+实例统一扁平列表** | BPMN 扩展（定义期语义）+ 运行期实例数据 → 合成一张扁平列表 | 查询时无需先解析 BPMN 再关联实例 |

### 8.2 关键发现：Flowable 8.1.0-SNAPSHOT **已原生提供 B 形态**

源码 / DDL 实证（详见 §4.4 修正）：

| 原生能力 | 位置 |
|---|---|
| `BUSINESS_STATUS_`（实例级业务状态） | `ACT_HI_PROCINST` |
| `BUSINESS_STATUS_` / `DUE_DATE_` / `CLAIM_TIME_`（任务级） | `ACT_RU_TASK` |
| `SetProcessInstanceBusinessStatusCmd` | 更新命令 |
| `HistoricProcessInstanceQuery.processInstanceBusinessStatus()` | 精确 / Like / LikeIgnoreCase 查询 |
| `FlowableProcessBusinessStatusUpdatedEvent` | 变更事件 |

⇒ **"业务终态"的列化是现成的，无需自建变量。**

### 8.3 变量为什么慢（实证）

`ACT_RU_VARIABLE` 采用**多态稀疏存储**：一条变量一行，值按 `TYPE_` 落在 `DOUBLE_` / `LONG_` / `TEXT_`(`varchar(4000)`) / `BYTEARRAY_ID_` 之一。

- 一个实例的 N 个业务字段 = **N 行**；
- 跨实例按业务字段检索 = 扫行 + 类型分支；
- `ACT_HI_VARINST` **仅有** `ACT_IDX_HI_PROCVAR_PROC_INST(PROC_INST_ID_)` 索引，**无 name / value 索引**。

⇒ 这就是"行式存储导致检索低效"的根因。

### 8.4 推荐方案：原生列 + 投影宽表，**不要改 `ACT_*` 表结构**

1. **能用原生列的一律用原生列**（B 形态）：业务终态 → `BUSINESS_STATUS_`；到期 → `DUE_DATE_`。零改造成本、完全兼容原生 API。
2. **原生没有的列**（`title` / `data_id` / `form_id` / `is_test` / `def_id`）→ **形态 A 的投影宽表**（非 `wf_` 前缀、单向、可重建），见 §4.5。
3. **不要给 `ACT_*` 加列**：这些表由引擎写入，改结构会破坏 Flowable 升级兼容性。

### 8.5 检索效率提升预期

| 维度 | 改造前 | 改造后 |
|---|---|---|
| JOIN | 多表 JOIN 拼一行 | 单表查询，JOIN 数 → 0 |
| 索引命中 | 扫变量行 + 类型分支，无索引 | 真实列可建 B+Tree，命中率高 |
| 查询复杂度 | 扫行 + 内存过滤 | 索引定位 |
| 解析开销 | 每次解析 BPMN 取定义语义 | 语义已快照进宽表，免解析 |

**⚠️ 最易忽略的一步**：`ACT_HI_PROCINST` **默认无 `BUSINESS_STATUS_` 索引**（默认仅 `END_TIME_` / `BUSINESS_KEY_` / `SUPER_PROCESS_INSTANCE_ID_` 三个索引）。

⇒ **列化 ≠ 自动变快**，改用原生列后**必须补索引**才能拿到收益：

```sql
CREATE INDEX ACT_IDX_HI_PRO_BIZSTATUS ON ACT_HI_PROCINST(BUSINESS_STATUS_);
```

### 8.6 权衡

| 维度 | 影响 |
|---|---|
| 写入性能 | ⚠️ 略降（投影表需同步写）；原生列由引擎写，无额外成本 |
| 存储冗余 | ⚠️ 增加，但只冗余检索必需字段，可控 |
| 扩展性 | ⚠️ 宽表加字段需 DDL；原生列方案扩展性好（`BUSINESS_STATUS_` 是 varchar，可承载枚举） |
| 原生 API 兼容性 | ✅ 原生列完全兼容；⚠️ 投影表绕过引擎 API ⇒ **只能作索引，不能作事实源** |
| 一致性 | ⚠️ 投影表有同步延迟 ⇒ 需单向同步 + 巡检，以 `ACT_*` 为准 |

---

## 九、原生能力启用路线（多实例 + 加签 + 跳转）

> 本节回应：这些 Flowable 8.1.0-SNAPSHOT 原生能力如何启用、适用边界，以及如何逐步替代现有自研逻辑，并与 `WfRejectManager` 对照。

### 9.1 多实例（并行 / 串行）

**配置方式**（BPMN `multiInstanceLoopCharacteristics`）：

| 属性 | 作用 |
|---|---|
| `isSequential` | 依次（串行）审批 |
| `flowable:collection` | 办理人集合（引擎据此分叉） |
| `elementVariable` | 集合元素变量 |
| `completionCondition` | 放行条件 —— **或签** `${nrOfCompletedInstances >= 1}`、**会签** `${nrOfCompletedInstances == nrOfInstances}` |

**仓库现状**：`WfDefinitionServiceImpl#applyMultiInstanceIfEnabled` **已实现注入**（`assignee=${wfMiAssignee}`、`collection=wfMiAssignees_<nodeKey>`、`elementVariable=wfMiAssignee`），但 `multiInstanceEnabled` 开关**默认关闭**。

**动态调整**：
- 进入节点**前**：业务层把解析出的办理人集合写入变量 `wfMiAssignees_<nodeKey>`，引擎按集合分叉；
- 运行**中**：用 `addMultiInstanceExecution` / 对应 delete 增减。
- **`completionCondition` 是"或签/会签/比例通过"的原生表达**，可替代自研计数。

**边界**：只对**已建模为多实例**的任务生效。当前自研会签（单 userTask + `wf_task` 多条）**不会自动变多实例** ⇒ 必须开开关 + 重新部署。

### 9.2 加签（动态增加审批人）

**原生方式**（`RuntimeService` 源码实证）：

```java
Execution addMultiInstanceExecution(String activityId, String parentExecutionId,
                                    Map<String, Object> executionVariables);
// 配对：Deletes a multi-instance execution（减签）
```

**硬前置**：目标活动**必须已是多实例**。非多实例任务无法用此 API 加签（需先转多实例，或改用跳转/新建任务）。

**与自研对照**：当前加签由业务层向 `wf_task` 插行实现；改原生后由引擎创建真实任务 ⇒ 待办、到期、权限统一，业务侧不再维护多条 `wf_task`。

### 9.3 跳转（自由流 / 回退 / 任意节点流转）

`ChangeActivityStateBuilder` 能力全集（源码实证）：

| 方法 | 用途 |
|---|---|
| `moveExecutionToActivityId(execId, actId)` | 单执行流跳转 |
| `moveExecutionsToSingleActivityId(List, actId)` | 多执行流**汇聚**（并行 / 多实例） |
| `moveSingleExecutionToActivityIds(execId, List)` | 单执行流**分裂**为多 |
| `moveActivityIdTo(cur, new)` / 两个 List 变体 | 按当前活动 id 跳转 |
| `moveActivityIdToParentActivityId` | 跳到**父流程** |
| `moveActivityIdToSubProcessInstanceActivityId` | 跳到**子流程**（可指定定义版本） |
| `enableEventSubProcessStartEvent` | 激活事件子流程 |
| `processVariable(s)` / `localVariable(s)` | 跳转时附带变量 |
| `changeState()` | 执行 |

### 9.4 ⚠️ 三个必须保留自研的边界（原生不覆盖）

1. **跳转不校验目标是否为等待态**。
   `WfRejectManager` 注释原文已记录该坑：*"归档(3)/等待(5)/自动处理(6)/网关(7) 在 BPMN 里不是等待态，token 移过去不会停住 —— 会立刻沿出口继续流出，表现为「退回了但没动」"*。
   **Flowable 跳转有完全相同的坑** ⇒ **节点类型过滤必须保留**，不能删。
2. **不提供"可退回到哪些节点"的计算**（反向 BFS + `is_reject` 封锁 + 白名单裁剪）——这是**业务规则**，引擎没有。
3. **不提供 ecology 式"驳回后原路返回"语义**，仍需业务表达。

### 9.5 与 `WfRejectManager` 对照分析

`WfRejectManager` 现状：沿 `wf_node_link` **反向 BFS** 回溯到创建节点，跳过 `is_reject=1` 的入边，按 `settings.reject.nodeKeys` 白名单裁剪，剔除节点类型 3/5/6/7（引擎停不住），输出"由近及远"的可退回候选集；另有 `resolveDefaultRejectNode`（配置优先，否则最近上游）与 `isRejectable`（合法性校验）。

| 维度 | 自研 `WfRejectManager` | Flowable 原生 | 结论 |
|---|---|---|---|
| **功能覆盖度** | 候选集计算 + is_reject 封锁 + 白名单 + 类型过滤 + 默认节点 | **仅"执行跳转"**，候选集计算完全没有 | 🔴 原生只覆盖**最后一步** |
| **驳回语义** | 沿 `wf_node_link` 反向回溯，`is_reject=1` 不可退 | 引擎无"驳回"概念，只是移动 token | 语义仍在业务侧 |
| **驳回后流转规则** | 退回到目标节点后按出口条件重新流转；`rejectToStarter` 特殊路径 | 同样按 BPMN 出口条件流转 | ✅ 行为一致（均由出口条件决定） |
| **"退回了但没动"** | 已用节点类型过滤规避 | ❌ **原生无防护** | 🔴 过滤必须搬到 `changeState()` 之前 |
| **性能** | 每次退回：查 `wf_node_link` 全表 + BFS + 查节点 | 执行阶段一次引擎调用（更快）；候选集计算仍需保留 | 小幅提升 |
| **可维护性** | 自研图算法，与 BPMN 拓扑**双源**，可能不一致 | 执行交给引擎；候选集仍读 `wf_node_link` | 🟠 **只有出口下沉 BPMN 后才彻底消除双源** |

### ⇒ 核心结论：`WfRejectManager` 不能被"替换"，只能被"拆分"

- **保留**：候选节点计算（反向 BFS / `is_reject` 封锁 / 白名单裁剪 / 节点类型过滤 / 默认节点）—— 业务规则，引擎没有；
- **替换**：真正"移动 token"的那一步 → `changeState()`；
- **前置**：候选集计算依赖 `wf_node_link` ⇒ 若出口已下沉进 BPMN，则改为从 BPMN 读图，此时双源问题才真正消除。

### 9.6 替换顺序与依赖

```
P1 节点 / 出口下沉 BPMN（wf_node_link → <wf:link>）
   ↓ 为 WfRejectManager 提供单一图源（消除双源）
P2 WfRejectManager 改为从 BPMN 读图（保留算法，只换数据源）
   ↓
P3 跳转执行换为 changeState()（保留候选集计算 + 节点类型过滤）
   ↓
P4 会签改多实例（开启 multiInstanceEnabled + 运行时门禁改造）
   ↓ 加签的前置
P5 加签换为 addMultiInstanceExecution
```

依赖要点：
- **跳转（P3）不依赖多实例**，可先做；但"彻底消除双源"依赖 P1 / P2；
- **加签（P5）强依赖 P4**（活动必须已是多实例）；
- **多实例（P4）依赖运行时门禁改造**（移除 `WfTaskServiceImpl#doApprove` 中自研的 `countPending` / `closeSiblings` / `nextPending`）。

### 9.7 数据迁移与兼容处理

- **多实例开关只对重新部署后的新实例生效**：存量在途实例仍是"单 userTask + `wf_task`"结构 ⇒ **必须共存期**。建议按定义版本区分处理，或用 `ProcessInstanceMigrationService` 迁移。
- **跳转改造无数据结构变化**：风险低，可灰度。
- **加签只对多实例节点生效**：非多实例节点须保留自研加签路径，直至全部转换完成。
- **驳回回多实例节点**：需重建集合变量 `wfMiAssignees_<nodeKey>`（边界场景，见 V6）。

---

# 十、需进一步验证 / 决策的关键点

| 编号 | 类型 | 内容 |
|---|---|---|
| **D1** | 决策 | 业务终态**改用原生 `BUSINESS_STATUS_`**（而非流程变量）—— 需定稿取值规范（`APPROVED` / `REJECTED` / `CANCELED`） |
| **D2** | 决策 | `rejectToStarter` 的"合成待办"（`engine_task_id` 为空的 `wf_task`）在多实例模式下如何表达 |
| **D3** | 决策 | 投影宽表是长期保留还是仅过渡？保留则需明确同步时机与失效策略 |
| **V1** | 验证 | `BUSINESS_STATUS_` **默认无索引** ⇒ 实测补索引后的查询计划与提升幅度 |
| **V2** | 验证 | ~~`ACT_RU_TASK.BUSINESS_STATUS_` 是否有 `TaskQuery` API 暴露~~ → **该列不存在**（`blade` 库实测 `act_ru_task` 共 37 列，末列 `SUB_TASK_COUNT_`，无 `BUSINESS_STATUS_`）。问题改为：**任务级业务状态如何承载** —— 建议**不新增列**，通过 `PROC_INST_ID_` JOIN 实例表获取（见 §十一 决策 2） |
| **V3** | 验证 | `addMultiInstanceExecution` 新增后，`completionCondition` 是否**重新求值**并把新增者计入 `nrOfInstances`（否则"会签到齐"判定会错） |
| **V4** | 验证 | `changeState()` 跳回**已执行过的活动**时，`ACT_HI_ACTINST` 是否产生重复 / 异常记录 |
| **V5** | 验证 | `moveExecutionsToSingleActivityId` 用于并行汇聚退回时，是否取消其余分支任务 |
| **V6** | 验证 | 加签后驳回回多实例节点，集合变量如何重建 |
| **V7** | 验证 | BPMN 扩展元素的**往返保真**（解析 → 写回 → 断言无损），防 Flowable 升级后行为变化 |

---

## 十一、为 `ACT_*` 表新增列与调整结构的具体方案

> 前提：本节在"**暂不考虑 Flowable 升级兼容性**"的约定下给出方案。所有结论均基于 **`blade` 库实测** + Flowable 8.1.0-SNAPSHOT 官方 DDL 交叉验证。

### 11.1 现状实测（`blade` 库）

#### 11.1.1 数据量：极小，ALTER 无风险

| 表 | 行数 | 大小 |
|---|---|---|
| `act_hi_actinst` | 165 | 0.11 MB |
| `act_ru_actinst` | 120 | 0.16 MB |
| `act_ge_bytearray` | 99 | **1.50 MB** |
| `act_re_procdef` | 64 | 0.03 MB |
| `act_hi_varinst` | 64 | 0.11 MB |
| `act_re_deployment` | 63 | 0.02 MB |
| `act_ru_variable` | 59 | 0.11 MB |
| `act_ru_execution` | 52 | 0.13 MB |
| `act_hi_taskinst` | 48 | 0.08 MB |
| `act_hi_procinst` | 34 | 0.08 MB |
| `act_ru_task` | 25 | 0.13 MB |
| `act_hi_comment` / `act_hi_detail` / `act_hi_identitylink` / job 系列 / `act_id_*` | **0** | — |

**总计约 750 行 / 4.5 MB** ⇒ 任何 `ALTER` / 建索引均为秒级，无需 `pt-online-schema-change`。

> ⚠️ **`jeelowcode` 库同样存在 `ACT_*` 表**（正是此前"跨库误命中 `ACT_GE_PROPERTY`"的根源）。所有 DDL **必须显式 `USE blade`**，否则会误改其它库。

#### 11.1.2 表结构：已有列 vs 缺失列

| 表 | 已有可用列 | 缺失（需新增） |
|---|---|---|
| `ACT_HI_PROCINST` | `BUSINESS_STATUS_`、`BUSINESS_KEY_`、`START_USER_ID_`、`NAME_`、`STATE_`、`END_TIME_`、`DUE_DATE_`、`CLAIM_TIME_`、`CLAIMED_BY_`、`END_USER_ID_` | `DEF_ID_`、`DATA_ID_`、`FORM_ID_`、`TITLE_`、`IS_TEST_` |
| `ACT_RU_EXECUTION` | `BUSINESS_STATUS_`（**执行流级**）、`ACT_ID_`、`START_USER_ID_`、`DUE_DATE_`、`CLAIM_TIME_`、`CLAIMED_BY_` | 不冗余（见决策 1） |
| `ACT_RU_TASK`（实测 **37 列**） | **`TASK_DEF_KEY_`(= nodeKey)**、`ASSIGNEE_`、`DUE_DATE_`、`CATEGORY_`、`FORM_KEY_`、`STATE_`、`CREATE_TIME_`、`SUSPENSION_STATE_` | `TIMEOUT_HANDLED_`；**无** `BUSINESS_STATUS_` |
| `ACT_HI_TASKINST` | `TASK_DEF_KEY_`、`ASSIGNEE_`、`DUE_DATE_`、`START_TIME_`、`END_TIME_`、`COMPLETED_BY_`、`DELETE_REASON_` | 不冗余（见决策 2） |

**⇒ 关键收益**：`nodeKey`（`TASK_DEF_KEY_`）、`assignee`（`ASSIGNEE_`）、`due_time`（`DUE_DATE_`）**原生均已具备**，无需新增，改动面大幅缩小。

#### 11.1.3 索引（实测）

| 表 | 现有索引 | **缺失** |
|---|---|---|
| `act_hi_procinst` | PK(`ID_`)、UQ(`PROC_INST_ID_`)、`BUSINESS_KEY_`、`END_TIME_`、`SUPER_PROCESS_INSTANCE_ID_` | 🔴 `BUSINESS_STATUS_`、`IS_TEST_`、`DEF_ID_`、`DATA_ID_` |
| `act_ru_task` | PK、3 个 FK、`CREATE_TIME_`、3 个 SCOPE 复合 | 🔴 **`DUE_DATE_`**（超时扫描将全表扫）、`ASSIGNEE_`、`TASK_DEF_KEY_` |
| `act_hi_taskinst` | PK、`PROC_INST_ID_`、3 个 SCOPE 复合 | `ASSIGNEE_` |
| `act_hi_varinst` | 含 `ACT_IDX_HI_PROCVAR_NAME_TYPE`(`NAME_`,`VAR_TYPE_`) | 按 **value** 检索无索引 |

> 修正前文说法：`ACT_HI_VARINST` **确有** `NAME_` 复合索引（按变量名检索历史变量有索引），但按**值**检索仍无索引。

#### 11.1.4 约束

- **`ACT_RU_*` 有外键**：`act_ru_task` 3 个（→ `execution` / `procdef` / `procinst`）、`act_ru_execution` 4 个、`act_ru_variable` 3 个。
- **`ACT_HI_*` 无外键**（历史表）。
- ⇒ 仅加列不影响外键；但 `ACT_RU_*` 的删除/更新受 FK 校验，回填 SQL 须避免破坏引用。

#### 11.1.5 写入逻辑 —— **最关键的一条约束**

Flowable 的 MyBatis `INSERT` / `UPDATE` 使用**显式列列表**，因此：

> **新增列引擎永远不会写入**，引擎插入后这些列保持 `NULL` / 默认值。

由此推出两条**硬约束**：

1. **新增列必须 `NULL`-able 且带 `DEFAULT`** —— 否则 MySQL 严格模式下引擎 `INSERT` 会因缺列报 `Field 'xxx' doesn't have a default value`，**直接导致流程发起失败**。
2. **必须有"补写机制"**（引擎写行 → 业务侧补列）+ 兜底巡检。

#### 11.1.6 查询逻辑

- **现有直接命中 `ACT_*` 的 SQL**：`HistoricalDriftFixer` 的 `notInSql(..., "SELECT ID_ FROM ACT_RU_TASK")`（不受本次变更影响）。
- **改造后将命中**：`WfTimeoutJob` 超时扫描（→ `ACT_RU_TASK.DUE_DATE_`）、待办 / 已办列表（→ `ACT_RU_TASK` / `ACT_HI_TASKINST`）、实例列表（→ `ACT_HI_PROCINST`）。

### 11.2 设计决策

| # | 决策 | 理由 |
|---|---|---|
| 1 | 业务字段**单点落在 `ACT_HI_PROCINST`**，不在 `ACT_RU_EXECUTION` 冗余 | `ACT_RU_*` 行在流程结束后**会被删除**，只存 RU 会丢失；两处都存则双份维护、必然不一致。运行时通过 `PROC_INST_ID_`（**唯一索引**）JOIN 取业务字段，成本可接受 |
| 2 | 任务级 `IS_TEST_` **不新增**，通过 `PROC_INST_ID_` JOIN 实例表获取 | 避免又一份冗余与不一致 |
| 3 | `TIMEOUT_HANDLED_` **加在 `ACT_RU_TASK`** | 超时动作为"提醒"时任务不推进、仍留在 `ACT_RU_TASK`，会被**重复提醒**，必须标记；`ACT_RU_TASK` 行在任务结束后即删，无需历史留存 |

### 11.3 具体变更方案

#### 11.3.1 DDL：新增列

```sql
USE blade;   -- ⚠️ 必须显式指定：jeelowcode 库也有 ACT_* 表

-- A. ACT_HI_PROCINST：业务字段主存（启动时即插行、永久保留）
ALTER TABLE ACT_HI_PROCINST
  ADD COLUMN DEF_ID_  BIGINT       NULL     COMMENT '业务流程定义ID(wf_process_definition.id)',
  ADD COLUMN DATA_ID_ BIGINT       NULL     COMMENT '表单数据ID',
  ADD COLUMN FORM_ID_ BIGINT       NULL     COMMENT '表单定义ID',
  ADD COLUMN TITLE_   VARCHAR(500) NULL     COMMENT '流程标题',
  ADD COLUMN IS_TEST_ TINYINT      NOT NULL DEFAULT 0 COMMENT '测试态 1=是',
  ALGORITHM=INSTANT, LOCK=NONE;

-- B. ACT_RU_TASK：超时已处理标记
ALTER TABLE ACT_RU_TASK
  ADD COLUMN TIMEOUT_HANDLED_ TINYINT NOT NULL DEFAULT 0 COMMENT '超时已处理 1=是',
  ALGORITHM=INSTANT, LOCK=NONE;
```

> `ALGORITHM=INSTANT` 在 MySQL 8.0 对"表尾追加可空 / 有默认列"生效（秒级、不重建表）。若环境不支持会报错，去掉该子句即可——数据量仅 750 行，常规 `ALTER` 同样是秒级。

#### 11.3.2 索引变更

```sql
USE blade;

-- 业务终态检索（现状无索引，必补）
CREATE INDEX IDX_HI_PRO_BIZSTATUS ON ACT_HI_PROCINST(BUSINESS_STATUS_);
-- 测试态隔离：所有查询都要过滤，必补
CREATE INDEX IDX_HI_PRO_ISTEST    ON ACT_HI_PROCINST(IS_TEST_);
-- 业务字段检索
CREATE INDEX IDX_HI_PRO_DEFID     ON ACT_HI_PROCINST(DEF_ID_);
CREATE INDEX IDX_HI_PRO_DATAID    ON ACT_HI_PROCINST(DATA_ID_);
-- 超时扫描（现状 act_ru_task 无 DUE_DATE_ 索引 → 全表扫，必补）
CREATE INDEX IDX_RU_TASK_DUE      ON ACT_RU_TASK(DUE_DATE_);
CREATE INDEX IDX_RU_TASK_TIMEOUT  ON ACT_RU_TASK(TIMEOUT_HANDLED_, DUE_DATE_);
-- 已办按人查询
CREATE INDEX IDX_HI_TASK_ASSIGNEE ON ACT_HI_TASKINST(ASSIGNEE_);
```

> ⚠️ **克制加索引**：`ACT_RU_*` 是高频写入表，每多一个索引都增加写成本。上表只加"查询必需"项；`TASK_DEF_KEY_`、RU 侧 `ASSIGNEE_` 暂不加，待压测后再定。

#### 11.3.3 写入逻辑改造（补写点）

| 时机 | 动作 | 方式 |
|---|---|---|
| 流程发起后 | 写 `ACT_HI_PROCINST.DEF_ID_ / DATA_ID_ / FORM_ID_ / TITLE_ / IS_TEST_` | 同事务 `UPDATE ... WHERE PROC_INST_ID_ = ?` |
| 业务终态变更 | 写 `BUSINESS_STATUS_` | 优先 `SetProcessInstanceBusinessStatusCmd`（原生），或 `UPDATE` |
| 超时动作执行后 | 置 `ACT_RU_TASK.TIMEOUT_HANDLED_ = 1` | `UPDATE ... WHERE ID_ = ?` |
| 兜底 | 定期巡检：列缺失则从 `wf_*` / 变量回填并告警 | 常驻任务 |

> ⚠️ 补写失败即列缺失 ⇒ **必须有兜底巡检**；双轨期内业务口径不应把新列当作唯一真相。

#### 11.3.4 存量回填

```sql
USE blade;

-- 回填前先跑 SELECT COUNT(*) 验证匹配行数（防止 engine_inst_id 为空导致漏回填）
SELECT COUNT(*) FROM ACT_HI_PROCINST h JOIN wf_instance w ON w.engine_inst_id = h.PROC_INST_ID_;

UPDATE ACT_HI_PROCINST h
  JOIN wf_instance w ON w.engine_inst_id = h.PROC_INST_ID_
   SET h.DEF_ID_  = w.def_id,
       h.DATA_ID_ = w.data_id,
       h.FORM_ID_ = w.form_id,
       h.TITLE_   = w.title,
       h.IS_TEST_ = IFNULL(w.is_test, 0);

-- 业务终态回填（按 wf_instance.status 映射）
UPDATE ACT_HI_PROCINST h
  JOIN wf_instance w ON w.engine_inst_id = h.PROC_INST_ID_
   SET h.BUSINESS_STATUS_ = CASE w.status
        WHEN 1 THEN 'APPROVED' WHEN 2 THEN 'REJECTED' WHEN 3 THEN 'CANCELED' END
 WHERE w.status IN (1, 2, 3);
```

#### 11.3.5 执行步骤与回滚

1. **停服** `blade-workflow`（或选低峰；数据量小，但保险起见）
2. **备份**：`mysqldump blade ACT_HI_PROCINST ACT_RU_TASK > backup.sql`
3. 执行 §11.3.1 DDL
4. 执行 §11.3.2 建索引
5. 执行 §11.3.4 回填（**先用 SELECT 验证匹配数**）
6. 启动并验证：新列有值、`BUSINESS_STATUS_` / `DUE_DATE_` 索引被命中（`EXPLAIN` 确认）
7. **回滚**：`ALTER TABLE ... DROP COLUMN ...`（数据量小，秒级）

### 11.4 风险与注意事项

| 风险 | 等级 | 说明 / 缓解 |
|---|---|---|
| 新增列若 `NOT NULL` 无默认 ⇒ 引擎 `INSERT` 失败 | 🔴 高 | **已按"可空 + DEFAULT"设计**，不可违反 |
| 引擎不写新列 ⇒ 列缺失 | 🔴 高 | 必须补写 + 兜底巡检 + 双轨期 |
| 误改 `jeelowcode` 库 | 🟠 中 | DDL 首行 `USE blade` 并复核 |
| `databaseSchemaUpdate=create` / `create-drop` 会**重建表、丢失新列** | 🟠 中 | DDL 必须在 Flowable 建表**之后**执行；禁止 `create-drop` |
| `ACT_RU_*` 索引过多拖慢写入 | 🟡 低 | 只加必需索引，压测后再增 |
| 回填时 `engine_inst_id` 不匹配 ⇒ 漏回填 | 🟡 低 | 先 `SELECT COUNT(*)` 验证，再 `UPDATE` |
| 与 `ACT_RU_*` 外键冲突 | 🟡 低 | 仅加列、不动外键，无冲突 |

### 11.5 本节对前文结论的修正

| 前文结论 | 修正后 |
|---|---|
| `ACT_RU_TASK` 有 `BUSINESS_STATUS_`（§4.4 / §8.2 曾误记） | ❌ **不存在**。该列属于 `ACT_RU_EXECUTION`；`blade` 库 `act_ru_task` 实测 37 列、末列 `SUB_TASK_COUNT_` |
| `ACT_HI_VARINST` 无 name 索引 | ⚠️ 部分修正：有 `ACT_IDX_HI_PROCVAR_NAME_TYPE`(`NAME_`,`VAR_TYPE_`)，但按**值**检索仍无索引 |
| 任务级业务状态需新增列 | 改为**不新增**，通过 `PROC_INST_ID_` JOIN 实例表获取（决策 2） |

---

## 十二、源码可修改性完整评估（不考虑升级兼容前提）

> 围绕"状态追加""节点停留""去 `wf_` 表连锁影响""改源码 vs 扩展点""遗漏项排查"五个方面，明确**可改 / 不可改边界**、**潜在遗漏**与**建议验证方式**。
> 本节结论均基于 Flowable 8.1.0-SNAPSHOT 源码实证 + `blade` 库实测。

### 12.0 结论先行：可改 / 不可改边界总表

| 类别 | 项 | 判定 | 理由 |
|---|---|---|---|
| 🟢 **可改（且推荐用扩展点，不必改源码）** | 自定义业务状态 | 扩展点 | `BUSINESS_STATUS_` 是**自由 varchar**，引擎不校验取值 ⇒ 追加"通过 / 不通过 / 撤销"**无需改源码** |
| 🟢 | 自定义节点行为（停留 / 超时 / 自动流转） | 扩展点 | `setActivityBehaviorFactory(...)`（源码实证 `ProcessEngineConfigurationImpl:3734`） |
| 🟢 | 自定义 BPMN 解析（注入 `wf:` 扩展语义） | 扩展点 | `preBpmnParseHandlers` / `postBpmnParseHandlers` / `customDefaultBpmnParseHandlers` 均已提供 |
| 🟢 | 引擎事务内写入自定义列 | 扩展点 | `managementService.executeCommand(Command<T>)`（源码实证 `ManagementService:467`）⇒ **同事务，消除两阶段不一致** |
| 🟡 **可改但需改多处（中风险）** | 让引擎原生 INSERT 写自定义列 | 改源码 | 需改：Entity + `getPersistentState()` + MyBatis XML + 历史管理 + QueryImpl + DDL（多方言）+ 升级脚本 |
| 🟡 | 改变状态机流转 / 历史写入时机 | 改源码 | 写入路径分散（start / end / delete / suspend / migration / async），漏一处即不一致 |
| 🔴 **不可改 / 改了也没用** | 草稿态（未进引擎） | 业务层 | 引擎无"未发起实例"概念 |
| 🔴 | `rejectToStarter` 合成待办 | 业务层 | 业务 `current_node_key` 与引擎 `ACT_ID_` **故意不一致**，属业务语义分歧 |
| 🔴 | 并行 / 多实例下"单一当前节点"收敛 | 业务层 | 并行分支下当前活动是**集合**，引擎不会替你选一个 |
| 🔴 | 部门 / 角色 / 岗位数据权限 | 业务层 | 引擎仅 `TENANT_ID_`（租户级），无组织维度 |
| 🟡 | 定时器超时自动流转 | **配置可开（非源码问题）** | `setAsyncExecutorActivate(true)` 即可启用 ⇒ 定时器可触发。**但"必要不充分"**：须配套 §12.8.3 的七项（尤其防双轨重复触发、测试态外溢）；若仅需超时自动流转，维持 `WfTimeoutJob` 成本更低 |

### 12.1 状态追加分析

**现有状态字段（实测）**

| 层 | 字段 | 说明 |
|---|---|---|
| 实例 | `ACT_HI_PROCINST.STATE_` | 引擎生命周期态 |
| 实例 | `ACT_HI_PROCINST.BUSINESS_STATUS_` | **自由业务状态**，varchar，配 `SetProcessInstanceBusinessStatusCmd` + `HistoricProcessInstanceQuery` + `FlowableProcessBusinessStatusUpdatedEvent` |
| 执行流 | `ACT_RU_EXECUTION.SUSPENSION_STATE_` / `BUSINESS_STATUS_` | 暂停 / 激活 |
| 任务 | `ACT_RU_TASK.STATE_` / `SUSPENSION_STATE_`、`ACT_HI_TASKINST.STATE_` | 任务态 |
| 定义 | `ACT_RE_PROCDEF.SUSPENSION_STATE_` | 定义级挂起 |

**⇒ 关键结论**：自定义业务状态**已被原生支持**（`BUSINESS_STATUS_` 引擎不校验取值），追加"通过 / 不通过 / 撤销"**不需要改源码**，只需定稿取值规范（D1）。

**若仍强行改源码新增状态字段，风险如下**

| 风险 | 说明 |
|---|---|
| **状态不一致** | 引擎写入路径分散：start / end / delete / suspend / activate / migration / async 多处。漏改一处 ⇒ 该路径下新字段缺失 |
| **并发覆盖** | `ACT_RU_EXECUTION` 与 `ACT_HI_PROCINST` 均有 `REV_` 乐观锁（实测有 `REV_` 列）。自定义字段更新须与 `REV_` 协同，否则并发下相互覆盖 |
| **历史数据无法映射** | ① 存量 `wf_instance.status` 需回填；② **引擎不知道的业务态**（草稿、合成待办、暂停待重提交）无法映射 ⇒ 仍须业务侧兜底 |
| **脏检查失效（经典坑）** | 已实证 `HistoricProcessInstanceEntityImpl.getPersistentState()` 中逐字段登记（`persistentState.put("businessStatus", ...)`）。新增字段**必须**同步加入该方法，否则修改不会 flush 到库 |

### 12.2 节点停留分析

**引擎已原生提供的能力**

| 需求 | 原生承载 | 实测 |
|---|---|---|
| 当前活动 | `ACT_RU_EXECUTION.ACT_ID_` | 有列 |
| 当前任务节点 | `ACT_RU_TASK.TASK_DEF_KEY_` | 有列（= nodeKey） |
| **停留时长** | `ACT_HI_ACTINST`（`START_TIME_` / `END_TIME_` / `DURATION_` / `ACT_ID_`） | **原生已记录每个活动起止**（实测 165 行） |
| 到期时间 | `ACT_RU_TASK.DUE_DATE_` | 有列 |
| 提醒 | ❌ 引擎无原生提醒能力 | 需业务层 |

**⇒ 停留时长与到期时间原生已有**，无需改源码；**提醒**是唯一缺口，属业务能力。

**若改源码改写节点停留逻辑的影响面**

| 影响面 | 风险 |
|---|---|
| 任务调度 | userTask 的 `ActivityBehavior` 与 `TaskEntity` 创建 / 完成强耦合，改写易破坏任务生命周期 |
| 异步作业 | 若节点 `async=true`，behavior 在 job 中执行，自定义逻辑需处理 async 上下文与重试 |
| **锁机制** | `ACT_RU_EXECUTION.LOCK_OWNER_` / `LOCK_TIME_` 被 job executor 用于加锁；自定义逻辑若在锁内做长耗时操作会阻塞作业调度 |
| 定时器 | ⚠️ **改源码无法绕过"定时器依赖异步执行器"** —— `asyncExecutorActivate=false` 时定时器永不触发 |

> 🔴 **易遗漏点**：超时自动流转当前**必须**维持 `WfTimeoutJob` 的 DB 轮询（或按 §3.4 开启异步执行器）。这不是"改源码"能解决的，而是运行时配置约束。

### 12.3 去 `wf_` 表后对各模块的连锁影响（含隐性依赖）

| 模块 | 引擎载体 | 连锁影响 / 隐性依赖 |
|---|---|---|
| 运行时 | `ACT_RU_EXECUTION` / `TASK` / `VARIABLE` / `IDENTITYLINK` | 运行时行**结束后删除** ⇒ 业务字段必须落历史；`IDENTITYLINK` 只表达 candidate/assignee，**操作者矩阵的多行语义丢失** |
| 历史 | `ACT_HI_*` | 依赖 history level（当前 `audit`）；`act_hi_comment` **实测 0 行** ⇒ 需确认 `addComment` 是否已启用 |
| 身份 | `ACT_ID_*` | **实测全部 0 行** ⇒ 项目未使用 Flowable 身份模块（人员来自业务侧）；去 `wf_` 无影响 |
| 变量 | `ACT_RU_VARIABLE` / `ACT_HI_VARINST` | 多态稀疏存储、按**值**无索引（§11.1.3） |
| 事件订阅 | `ACT_RU_EVENT_SUBSCR` | 实测 0 行；将来若用消息事件会引入 |
| 定时器 | `ACT_RU_TIMER_JOB` | 实测 0 行；**异步执行器关闭 ⇒ 不会触发** |
| 作业 | `ACT_RU_JOB` / `DEADLETTER` / `SUSPENDED` / `EXTERNAL` / `HISTORY_JOB` | 全部 0 行；去 `wf_` 不影响，但开异步后增长 |
| 字节数组 | `ACT_GE_BYTEARRAY`（**1.50 MB，最大表**） | 存 BPMN XML + 变量大对象 |
| 属性 | `ACT_GE_PROPERTY` | `schema.version` 校验；加列不影响，但 `create-drop` 会重建并丢失新列 |

**⚠️ 三条隐性依赖（最易遗漏）**

1. **`ACT_GE_BYTEARRAY` 成为语义唯一载体**：BPMN 扩展下沉后，节点 / 出口语义只存在于该表的 XML 中。若该表被清理 ⇒ 语义全丢，**历史实例将无法还原节点语义**。
2. **流程定义版本不可随意清理**：历史实例引用旧版本 `ACT_RE_PROCDEF` / `ACT_GE_BYTEARRAY`；删除旧版本 ⇒ 历史实例的节点 / 出口语义无法还原。
3. **`wf_migration_map` 删除后失去存量对账凭据**：ecology 存量迁移的 ID 映射若随 `wf_` 一起删除，存量数据将无法对账与回滚。

### 12.4 改源码 vs 扩展点 / 监听器 / 自定义行为类

| 方式 | 能力 | 成本与风险 |
|---|---|---|
| **扩展点（推荐）** | `setActivityBehaviorFactory` 自定义节点行为；`pre/postBpmnParseHandlers` 自定义解析；自定义 `Command` 同事务写自定义列；事件监听器；`customMybatisMappers` 追加 mapper | **不改 Flowable 源码**；升级友好；团队可控；改动的回归面局限于业务侧 |
| **改源码** | 让引擎原生 `INSERT` 写入自定义列；改变引擎状态机 / 调度语义 | 需改：Entity + `getPersistentState()` + MyBatis XML（多方言）+ `DefaultHistoryManager` + `*QueryImpl`（MyBatis 与内部查询**双路径**）+ DDL（7 种方言）+ 升级 step SQL；**还需重新构建并发布 `flowable-engine`**；且 repo 内存在 **flowable5 兼容层**（`flowable5*` 配置项源码实证），形成第二套路径；回归面巨大 |

> 🔴 **对 §11 方案的重要修正（先前遗漏的优化点）**：§11 判定"引擎不写新列 ⇒ 必须业务侧补写 ⇒ 有不一致风险"。但 `managementService.executeCommand(Command<T>)`（源码实证 `ManagementService:467`）可在**引擎同一事务内**执行自定义 SQL ⇒ **补写可做到同事务、零不一致，无需 fork 源码**。这是比"改源码"更优的解法，应作为 §11 的首选写入方式。

### 12.5 遗漏项排查

| 维度 | 现状 / 风险 | 建议 |
|---|---|---|
| **权限与租户隔离** | 引擎仅 `TENANT_ID_`（租户级），**无部门 / 角色 / 岗位维度** | 不能靠改源码解决；保留业务权限层 |
| **事务边界** | 引擎 Command 为单事务；业务侧若用独立事务写自定义列 ⇒ 不一致 | 用**同事务 Command** 或事务内监听器，**禁止独立事务** |
| **缓存一致性** | Flowable 有 `EntityCache`（CommandContext 内）与流程定义缓存；改源码加字段若不同步缓存，读到旧对象 | 新增列读取优先走直接 SQL；若改源码须验证缓存失效路径 |
| **分布式多节点** | 多实例部署：异步作业靠 DB 锁竞争；流程定义缓存各节点独立；启动时 `databaseSchemaUpdate` 可能并发冲突 | DDL 由单节点执行；投影表同步须幂等、可重放 |
| **审计日志** | `ACT_HI_COMMENT`、`ACT_EVT_LOG` **实测均 0 行**；业务 `wf_approval_log` 有 `log_type` 细分 | 确认 `addComment` 是否启用；定稿 `ACTION_` 取值规范 |
| **异常补偿** | 引擎失败即回滚；业务侧补写失败无重试 ⇒ 静默丢失 | 需自建死信 + 告警（§3.4 已规划） |
| **数据迁移与校验** | 存量 `wf_*` → `ACT_*` 映射；`engine_inst_id` 为空会漏回填 | 先 `SELECT COUNT(*)` 校验匹配数；双轨对账 |
| **性能退化** | BPMN 扩展下沉后每次读语义需解析 XML；`ACT_RU_*` 加索引增加写成本 | 按 `procDefId` 缓存解析结果（§8）；克制加索引（§11.3.2） |
| **边界：驳回** | `WfRejectManager` 图算法（反向 BFS + `is_reject` 封锁 + 白名单 + 节点类型过滤） | **必须保留**，只替换"移动 token"一步（§9.5） |
| **边界：撤回** | 引擎无原生撤回，需 delete / suspend / jump 组合 | 业务语义，不能靠改源码 |
| **边界：跳转 / 自由流** | `ChangeActivityStateBuilder` 已足够（§9.3） | ⚠️ **不校验目标是否等待态** ⇒ 必须保留节点类型过滤 |
| **边界：并行网关** | `moveExecutionsToSingleActivityId` 可汇聚 | ⚠️ **并行下"当前节点"是集合** ⇒ 单列 `current_node` 表达不足，须业务口径收敛（**易遗漏**） |
| **边界：会签中间态** | 多实例部分完成（`nrOfCompletedInstances`） | 业务展示口径（"第几人会签中"）属业务层 |

### 12.6 建议验证方式（扩展 §十 的 V 系列）

| 编号 | 验证项 | 方式 |
|---|---|---|
| **V8** | `SetProcessInstanceBusinessStatusCmd` 是否**同时**更新 `ACT_RU_EXECUTION.BUSINESS_STATUS_` 与 `ACT_HI_PROCINST.BUSINESS_STATUS_` | 发起 → 设状态 → 分别查两表 |
| **V9** | `managementService.executeCommand` 内写自定义列是否**与引擎同事务**（回滚一致性） | 命令内写列后抛异常，验证列是否回滚 |
| **V10** | 并行网关 + 多实例下"当前活动集合"的表达 | 起并行流程，查 `ACT_RU_EXECUTION WHERE IS_ACTIVE_=1` 的数量与 `ACT_ID_` |
| **V11** | 自定义 `ActivityBehaviorFactory` 替换后，既有 userTask 生命周期是否正常 | 回归：创建 / 认领 / 完成 / 转办 / 到期 |
| **V12** | `getPersistentState()` 未登记新字段时的脏检查失效 | 改源码前必测：新增字段后不加入 persistentState，验证 UPDATE 是否落库 |
| **V13** | BPMN 扩展往返保真（同 V7）+ 定义版本回滚后语义是否可还原 | 部署 v1 → 回滚 v1 → 解析扩展 |
| **V14** | 历史实例在**旧定义版本被清理后**能否还原节点 / 出口语义 | 模拟删除旧 `ACT_GE_BYTEARRAY`，验证历史实例语义读取 |
| **V15** | 分布式多节点下 DDL 与投影表同步的幂等性 | 双节点并发执行同一同步任务 |

**测试场景清单（必测）**：或签 / 会签 / 依次 + 驳回回多实例节点；驳回 → 撤回；跳转（含跳到非等待态，验证防护）；并行网关汇聚；自由流；超时（提醒 与 自动通过 两类）；暂停 / 恢复 / 终止；测试态 `is_test` 全流程隔离。

### 12.7 本节对前文方案的修正与补强

| 前文 | 修正 / 补强 |
|---|---|
| §11 写入方式："业务侧补写，有不一致风险" | 🔴 **补强**：改用 `managementService.executeCommand` 在**引擎同事务内**写入 ⇒ 消除不一致，**且无需改源码**（§12.4） |
| 业务状态需新增字段 | 维持"复用 `BUSINESS_STATUS_`"结论，进一步确认**无需改源码**（§12.1） |
| 超时自动流转 | 补充：受 `asyncExecutorActivate=false` 约束，**改源码无法绕过**，须维持 DB 轮询或开异步执行器（§12.2） |
| 去 `wf_` 影响 | 补三条隐性依赖：`ACT_GE_BYTEARRAY` 为语义唯一载体、旧定义版本不可清理、`wf_migration_map` 为对账凭据（§12.3） |

### 12.8 三项待办问题的解决方案

#### 12.8.1 「状态机流转 / 历史写入时机 —— 写入路径分散，漏一处即不一致」的解决方案

**核心思路：不要去改分散的写入路径，而是把它收敛到一个集中扩展点，并优先"推导"而非"存储"。**

| 档位 | 方案 | 做法 | 适用 |
|---|---|---|---|
| **L1（首选）** | **状态改为"推导"，不存冗余列** | 业务终态在查询时从 `ACT_*` 事实推导（结束事件 / `DELETE_REASON_` / `BUSINESS_STATUS_` / `STATE_` 组合判定），不落冗余字段 | 状态可由引擎事实唯一确定时 |
| **L2** | **用 `HistoryManager` 集中补写（一处而非多处）** | 源码实证：`ProcessEngineConfigurationImpl.setHistoryManager(...)`（:4449）+ **`CompositeHistoryManager implements HistoryManager`**（:38，官方组合式扩展点）。自定义 `HistoryManager` 装饰 `DefaultHistoryManager`（或用 Composite 追加），在统一回调里补写自定义字段 | 必须落列时 |
| **L3（兜底，必配）** | **常驻对账修复** | 即使 L1/L2 仍有遗漏 ⇒ 由 §3.4 已规划的常驻对账自动补齐 | 所有情况 |

- **运行时状态**同理：用 `FlowableEventListener`（`ENTITY_CREATED` / `ENTITY_UPDATED` / `ENTITY_DELETED`）或生命周期监听**集中**处理，而非逐路径改。
- ⚠️ L2 仍须配合 `getPersistentState()` 登记新字段（§12.1 经典坑），否则更新不落库。
- ⇒ **结论：三层组合（推导优先 + 集中扩展点 + 对账兜底）即可解决，无需 fork 源码。** "漏一处"在 L3 下只是短暂不一致，会被对账修复，不会永久错误。

#### 12.8.2 「部门 / 角色 / 岗位 —— 按部门流转怎么办」的解决方案

**必须先区分两件事，二者方案不同：**

**(a) 按部门流转 = 操作者解析（引擎不该懂"部门"）**

现状已具备：`WfNodeOperator.opType` 已覆盖 `1部门 / 30分部 / 2角色 / 58岗位 / 19本部门 / 42字段-部门 / 43字段-角色 / 99矩阵 / 97接口 / 98SQL`，由 `WfOperatorResolver` 运行期解析。

标准做法（**引擎只接受"人"，不关心"部门"语义**）：

```
业务层 WfOperatorResolver：部门/角色/岗位 → 解析为具体人员 ID 集合
        ↓
写入流程变量 wfMiAssignees_<nodeKey>
        ↓
引擎按集合多实例分叉（userTask.assignee = ${wfMiAssignee}）
```

⇒ 这样既满足"按部门/角色/岗位流转"，**又不把组织语义污染进引擎或 BPMN**。已论证：操作者**结果**不可下沉 BPMN（依赖发起人 / 表单数据 / 组织架构，§7.1）。

**(b) 数据权限（谁能看哪些数据）**

引擎仅有 `TENANT_ID_`（租户级），**不要指望引擎做组织维度行级权限**。方案：

1. 租户隔离 → 用 `TENANT_ID_`；
2. 部门 / 角色维度权限 → **留在业务层**（SpringBlade `DataScope`）；
3. 若需"按部门检索流程数据" → 在**投影索引表**扩展 `dept_id` / `start_dept_id` 列（§11 已加 `DEF_ID_` / `DATA_ID_`，可扩展），查询走投影表；
4. **不要**把部门字段塞进 `ACT_*` 让引擎过滤。

⇒ **结论：部门 / 角色 / 岗位是业务层职责**；交给引擎的只有"解析后的人员集合"。

#### 12.8.3 「定时器 —— 把 `asyncExecutorActivate` 设为 true 不就可以了？」的完整回答

**可以。`setAsyncExecutorActivate(true)` 正是那个开关（配置问题，非源码问题）。但它是"必要不充分"——开了会引入一组新问题，必须配套。**

| # | 开启后的必办项 | 说明 |
|---|---|---|
| 1 | **一致性降级** | 异步作业在独立事务 ⇒ C1 强一致失效（`isFailOnException=true` 无法回滚已提交的主操作）→ 台账与引擎变为**最终一致** |
| 2 | **兜底设施（§3.4）** | ⚠️ **对账的对象已变化**（见 §12.9）：台账不存在时，无需"引擎 vs 台账"对账；真正必需的是 **①DEADLETTER 作业告警**（超时作业重试耗尽会静默丢失）与 **②异步业务副作用完整性校验**（审批日志 / 提醒 / 表单状态是否落地）。**目前均未建设**，开启前必须先有 |
| 3 | 🔴 **防双轨重复触发** | `WfTimeoutJob`（DB 轮询）与引擎定时器**并存** ⇒ 同一任务可能超时触发**两次**（重复自动通过 / 重复提醒）。必须按"该节点是否建模了定时器"做互斥 |
| 4 | 🟡 **测试态外溢防护（可解决）** | 引擎定时器**不认识 `is_test`**，但**可过滤**，两种做法（详见 §12.9.2）：**①推荐**测试态部署期即不注入定时器（复用现有 `neutralizeForTest` 消毒机制，从源头不产生定时器作业）；**②**定时器触发后必经我们的 `JavaDelegate`，在入口第一行按 `wfIsTest` 变量过滤后直接 return（纵深防御）。二者叠加最稳妥 |
| 5 | **超时业务动作需重新接线** | 定时器触发后要调用 `WfTimeoutServiceImpl` 的 4 类动作（autoApprove / assign / remind / forward）⇒ 需 `JavaDelegate` 或信号桥接，**不是开了就自动具备** |
| 6 | **线程池与连接池** | 异步线程池占用额外连接（合并到 `blade` 后本就承压）；须显式配置 `asyncExecutor` 线程池参数（默认 core=2 / max=10 偏小） |
| 7 | **`asyncHistoryEnabled` 保持 false** | 否则历史写入也异步 ⇒ 漂移面进一步扩大 |

**⇒ 决策建议**

- **若只是要"超时自动流转"** → **不开**，维持 `WfTimeoutJob`。零额外成本，且已有 `is_test` 防护与 4 类动作，风险最低。
- **若确需 BPMN 定时器边界事件** → 按 §3.4 完整方案开启，并**先完成上表 7 项**（尤其 2 / 3 / 4）。

> 一句话：**`true` 是"开启一组新问题"的开关，不是"解决问题"的开关。**

### 12.9 两类漂移的区分、投影表的内在张力、定时器测试态过滤方案

#### 12.9.1 两类漂移（澄清 §1.2 第 4 条与 §12.8.3 第 2 项的表面矛盾）

| | **类型 A：引擎 vs 台账** | **类型 B：异步作业 vs 业务副作用** |
|---|---|---|
| 产生条件 | 存在 `wf_*` 第二副本 | **异步执行器开启**（作业在独立事务） |
| 去 `wf_*` 后 | ✅ **消失** | ❌ **依然存在** |
| 典型表现 | 引擎动了、台账没动 | 定时器触发了，但审批日志没写 / 提醒没发 / 动作抛异常 / 作业进 `DEADLETTER` 无人知晓 |
| 治理手段 | 方案C（事件驱动投影） | **DEADLETTER 告警 + 业务副作用完整性校验** |

⇒ **结论：§1.2 第 4 条"方案C 退役"仅针对类型 A，成立；§12.8.3 第 2 项的兜底需求属类型 B，与台账是否存在无关。二者不矛盾，是两类问题。**

#### 12.9.2 内在张力：§11 的投影索引表是"新的第二副本"

这是我先前**未充分说明**的一处张力：

- §11 推荐保留 `flow_inst` / `flow_task` 投影索引表 ⇒ 它本身就是一份**新的第二副本** ⇒ **引擎 vs 投影**仍会漂移 ⇒ 仍需同步 / 对账。
- 这实质上是**方案C 的"变体重生"**：对账对象从 `wf_*` 换成 `flow_*`，机制没有真正消失。

| 选择 | 后果 |
|---|---|
| **彻底不保留投影表**（引擎为唯一数据） | ✅ **推荐**。方案C **真正退役**。<br>⚠️ 修正先前"检索退化"的表述：**列表化（§八 / §11）已把业务字段变成 `ACT_HI_PROCINST` 上的真实列 + 索引**，故跨实例按业务字段检索**不退化**，可直接查引擎表。<br>真实代价是**查询方式改变 + 复杂报表成本上升**，见 §12.9.4 |
| **保留投影表**（§11 推荐） | 方案C 的同步 / 对账机制**必须保留**，仅对象改为 `flow_*`；且必须严格"单向、可重建、**以引擎为准**" |

⇒ **需决策（并入 D3）**：投影表是长期保留还是仅过渡？保留则须承认"方案C 未退役，只是换了对象"，并配套同步与巡检。

#### 12.9.3 定时器测试态过滤的两种做法（回应"加过滤掉测试流程不可以吗"）

**可以。** 引擎定时器本身不认识 `is_test`，但定时器触发后**必经我们的代码**，因此可过滤。两种做法：

| 做法 | 机制 | 优点 | 缺点 |
|---|---|---|---|
| **① 推荐：测试态部署期不注入定时器** | 复用现有 `neutralizeForTest` 消毒机制，在测试态 BPMN 中移除 / 禁用 `boundaryEvent` 定时器 | **从源头不产生定时器作业**；与既有测试态消毒同机制，一致性好 | 需扩展 `neutralizeForTest` |
| **② 纵深防御：`JavaDelegate` 入口过滤** | 定时器触发后执行我们的 Delegate，**第一行**读 `wfIsTest` 变量，为测试态则直接 return | 实现简单；即使定时器意外注入也能兜住 | 作业仍会创建与触发（少量开销）；依赖 `wfIsTest` 变量已正确写入 |

⇒ **建议二者叠加**（① 为主、② 为辅）。注意：② 生效的前提是发起时已把 `is_test` 写入流程变量（§11.3.3 补写点之一），须一并验证（并入 V9）。

> 补充：这与 `WfTimeoutJob` 的 `is_test` 过滤是同一防护目标，只是落点不同——前者在扫描阶段（`wf_task.is_test` 列），后者在触发阶段（流程变量）。若两条超时路径并存（§12.8.3 第 3 项），**两处过滤都必须生效**。

#### 12.9.4 列表化后"不保留投影表"的真实代价（修正 §8.5 的夸大表述）

既然 §八 / §11 的列表化已把业务字段固化为 `ACT_HI_PROCINST` 的**真实列 + 索引**（`BUSINESS_STATUS_` 原生、`DEF_ID_` / `DATA_ID_` / `FORM_ID_` / `TITLE_` / `IS_TEST_` 新增），则"不保留投影表"**并不会导致跨实例业务字段检索退化**。真实代价如下：

| 代价 | 说明 | 应对 |
|---|---|---|
| **不能用 Flowable 公开 API 按自定义列过滤** | `HistoricProcessInstanceQuery` 不认识 `DEF_ID_` / `DATA_ID_` / `IS_TEST_` | 直接对 `ACT_HI_PROCINST` 写**只读自定义 SQL / 自定义 Mapper**（引擎仍是唯一事实源，不产生漂移） |
| **任务级过滤需 JOIN 实例表** | 任务级 `IS_TEST_` 未新增（§11 决策 2），按测试态过滤待办需 JOIN `ACT_HI_PROCINST` | `PROC_INST_ID_` 有唯一索引，JOIN 成本可接受 |
| **复杂跨表报表成本上升** | 实例 + 任务 + 活动 + 节点语义的联合统计需 JOIN `ACT_HI_PROCINST` / `ACT_HI_TASKINST` / `ACT_HI_ACTINST`（+ 解析 BPMN 取节点语义） | 按 `procDefId` 缓存 BPMN 解析结果（§8）；必要时**只读**物化报表（非事实源） |
| **无 Home 的字段** | 如"发起人部门""流程分类名称""自定义标签"在 `ACT_*` 无对应列 | 需要则**继续列表化**（加到 §11 的 DDL），而非回退到投影表 |

⇒ **结论：列表化 + 不保留投影表 是自洽且推荐的组合。** §8.5 / §12.9.2 中"检索退化"的表述应理解为"查询方式与复杂报表成本改变"，而非能力退化。

#### 12.9.5 超时流转的必需性及其在"去 `wf_*`"后的落地（回应"超时流转必须要有，否则流程永远卡住"）

**认同：超时自动流转是刚需**，否则无人办理的流程会永久停滞。

**好消息：该能力已具备，且不需要开启异步执行器。**

现状（`asyncExecutorActivate=false` 下即可工作）：

```
wf_node_timeout（超时规则）+ wf_task.due_time（到期时间）
        ↓  WfTimeoutJob 定时扫描（默认 10 分钟）
        ↓  WfTimeoutServiceImpl.fire()
   → autoApprove 自动通过 / assign 转办 / remind 提醒 / forward
```

**去 `wf_*` 后的对应落地（三处替换，均在 §八 / §11 已覆盖）**

| 现状依赖 | 去 `wf_*` 后 | 是否已覆盖 |
|---|---|---|
| `wf_task.due_time` | **`ACT_RU_TASK.DUE_DATE_`**（原生列） | ✅ 已有 |
| `wf_node_timeout` 规则 | 下沉为 BPMN 扩展 `<wf:timeout>`（定义期语义） | ✅ §八 已规划 |
| `WfTimeoutJob` 扫描源 | 改为扫描 `ACT_RU_TASK`（`DUE_DATE_ <= now`） | ✅ §11 已规划 |
| 扫描用索引 | `IDX_RU_TASK_DUE`（`DUE_DATE_`） | ✅ §11.3.2 已列 |
| `is_test` 过滤 | JOIN `ACT_HI_PROCINST.IS_TEST_` 或流程变量 | ⚠️ 需确认口径（并入 V9） |

⇒ **去掉 `wf_*` 后超时流转依然完整可用，且仍然不需要 `asyncExecutorActivate=true`。**

**那么"开定时器"到底换来什么？**（即不开会损失什么）

| 维度 | `WfTimeoutJob` 轮询（现状） | BPMN 定时器（需开异步） |
|---|---|---|
| 超时流转能力 | ✅ 具备 | ✅ 具备 |
| **触发精度** | 受扫描周期限制（默认 10 分钟，可配置调小） | 精确到时刻 |
| **BPMN 可视化** | ❌ 流程图中看不到超时 | ✅ 定时器边界事件可见 |
| 依赖 | 扫描任务存活 | 异步执行器 + 作业表 |

⇒ **决策建议**：若仅要"流程不卡住"，**维持 `WfTimeoutJob`，把扫描周期调小即可满足精度需求，零新增风险**。只有当"超时必须在 BPMN 图中表达"或"需要精确时刻触发"时，才值得按 §12.8.3 开启定时器。

> ⚠️ 另需注意：若某节点**未配置任何超时规则**，则无论哪种方案都不会自动流转——这是配置问题（业务需为关键节点配置规则），不是引擎能力问题。

### 12.10 「状态机流转 / 历史写入时机」的改进方案 与 去 `wf_*` 后的检索效率优化

#### 12.10.1 写入路径分散的治本方案：先把"状态"重新分类

**核心洞察：所谓"start / end / delete / suspend / migration / async 路径分散"这一难题，多半源于我们把需求都当成了"每次状态转换都要写"。重新分类后，绝大多数需求根本不落在转换路径上。**

| 类别 | 例子 | 写入时机 | **写入点数量** | 方案 |
|---|---|---|---|---|
| **A 一次性实例属性** | `DEF_ID_` / `DATA_ID_` / `FORM_ID_` / `TITLE_` / `IS_TEST_` | 发起时一次 | **1 处** | 同事务 Command（§12.4 / §12.8.1） |
| **B 业务终态** | 通过 / 不通过 / 撤销 | 结束时一次 | **1 处** | `SetProcessInstanceBusinessStatusCmd`（**原生已有**） |
| **C 派生量** | 当前节点、节点停留时长 | **不存** | **0 处** | 查询时推导（`ACT_RU_EXECUTION.ACT_ID_` / `ACT_HI_ACTINST.START_TIME_~DURATION_`） |
| **D 引擎原生已有** | 暂停 / 恢复、任务生命周期、到期 | 引擎自管 | **0 处** | `SUSPENSION_STATE_` / `DUE_DATE_` 等 |
| **E 确需每次转换都写** | （罕见，如自定义转换审计） | 每次转换 | 多处 | 见下方方案 2 |

**⇒ A/B/C/D 覆盖了台账的绝大多数需求，真正需要"改分散写入路径"的 E 极少。**

**改进方案（按优先级）**

| # | 方案 | 做法 | 是否改源码 |
|---|---|---|---|
| **1（治本，首选）** | **需求分类降级** | 把"状态"尽量归到 A/B/C/D ⇒ 写入点收敛到 1 处或 0 处，**分散问题从根上消失** | ❌ 不需要 |
| **2** | **集中扩展点** | 对确属 E 的需求，用 `CompositeHistoryManager` / 自定义 `HistoryManager`（`setHistoryManager`，`ProcessEngineConfigurationImpl:4449`）**集中补写一处**，而非逐路径改 | ❌ 不需要 |
| **3（兜底）** | **同事务 Command + 常驻对账** | `managementService.executeCommand` 同事务补写；遗漏由对账修复 | ❌ 不需要 |
| **4（最后手段）** | **改源码** | 且必须**集中改 `DefaultHistoryManager` 一处**，并同步 `getPersistentState()`、多方言 mapper、DDL | ✅ 需要 |

⇒ **结论：有方案，推荐 1 + 2 + 3，不需要改源码。** 方案 4 仅在 E 类需求确实无法用扩展点满足时才考虑。

#### 12.10.2 去 `wf_*` 改用引擎原生后，检索效率的可改进点

**① 索引策略（分层对待）**

| 表类型 | 特点 | 策略 |
|---|---|---|
| `ACT_HI_*`（历史） | 写入后基本**只读** | ✅ **可放心加索引**（`BUSINESS_STATUS_`、`IS_TEST_`、`DEF_ID_`、`DATA_ID_`、`ASSIGNEE_`） |
| `ACT_RU_*`（运行时） | **高频写入** | ⚠️ 克制，只加必需（`DUE_DATE_`、`TIMEOUT_HANDLED_`） |

- 列表页"筛选 + 排序"用**复合索引**：如 `(IS_TEST_, BUSINESS_STATUS_, START_TIME_)`
- 列表页只取少量列时考虑**覆盖索引**，避免回表

**② BPMN 解析缓存 —— 引擎已内置，无需自建（修正 §8 的过度建议）**

源码实证：`ProcessDefinitionUtil.getBpmnModel()` → `deploymentManager.findDeployedProcessDefinitionById` + `resolveProcessDefinition`，**源码注释明确"This will check the cache"**；另有 `getBpmnModelFromCache()` 直接读 `ProcessDefinitionCacheEntry.getBpmnModel()`。

⇒ **优先用 `repositoryService.getBpmnModel()` 即可命中引擎缓存。** 仅在"流程定义数量大、缓存命中率低"时，才考虑调大 `processDefinitionCacheLimit`。§8 中"必须自建按 `procDefId` 缓存"的说法**应删除**。

**③ 查询分工：按实例状态分表查**

- **运行中** → 查 `ACT_RU_*`（小表，随在途量）
- **已结束** → 查 `ACT_HI_*`
- ⇒ 避免列表页一律扫描大历史表

**④ 其他可改进项**

| 项 | 说明 |
|---|---|
| **避免变量查询** | 高频检索字段已列化（§11），不要再走 `ACT_HI_VARINST` 按值扫描（按值无索引） |
| **历史表分区 / 归档** | `ACT_HI_*` 随时间线性增长 ⇒ 按时间 RANGE 分区或冷热分离，**长期最有效** |
| **读写分离** | 历史查询走只读实例 |
| **深分页优化** | 避免大 `OFFSET`，改用游标（按 `START_TIME_` + `ID_` 翻页） |
| **统计类查询** | 总数 / 看板用缓存或异步物化，不做实时 `COUNT(*)` |
| **`ACT_GE_BYTEARRAY` 隔离** | BPMN XML 与变量大对象（当前 1.50 MB，扩展下沉后增大）⇒ 建议独立表空间，避免挤占 buffer pool |

**⑤ 建议验证（并入 V 系列）**

- **V16**：`EXPLAIN` 验证新增索引是否被命中（尤其 `BUSINESS_STATUS_` 与 `DUE_DATE_`）
- **V17**：实测 `getBpmnModel()` 的缓存命中率（若定义数增长，评估是否需要调大 `processDefinitionCacheLimit`）
- **V18**：历史表增长到万级 / 十万级后的列表查询耗时，据此决定分区策略启动时点 |

---

## 十三、多业务域共用引擎的隔离规范

> **前提**：电商（`blade-mall` / `blade-order` / `blade-pay`）等**其它行业模块共用同一套引擎与 `ACT_*` 表**。本章明确共用边界与隔离方式。

### 13.1 实测现状

| 项 | 实测结果 |
|---|---|
| 电商模块 | `blade-mall`（143 个 java）/ `blade-order` / `blade-pay` **已存在** |
| 电商是否依赖 Flowable | ❌ **否**（`flowable` / `workflow` 搜索 0 命中） |
| Flowable 依赖位置 | **仅在 `blade-workflow`**（`flowable-engine` + `flowable-spring`） |
| 版本 | `8.1.0-SNAPSHOT`（本地源码构建，装于 `E:\project\mavenLib`） |

⇒ 其它行业目前**不直接使用引擎**，须经 workflow 服务接入（见 §13.2）。

### 13.2 核心约定：`ACT_*` 为 workflow 专用

| 规则 | 说明 |
|---|---|
| **唯一读写方** | `ACT_*` 只允许 **`blade-workflow`** 服务读写 |
| **其它行业的接入方式** | **只能通过 workflow 服务暴露的 API / Feign**，**禁止**直连 `ACT_*` 表 |
| **禁止** | 其它服务引入 `flowable-engine` 依赖、自建引擎实例、直接 SQL 读写 `ACT_*` |
| **理由** | 引擎配置（历史级别、异步执行器、BPMN 解析缓存、**§11 自定义列口径**）必须单一；多套引擎并行会导致行为与数据口径不一致 |

### 13.3 `TENANT_ID_` 隔离

- **部署**：`repositoryService.createDeployment().tenantId("<行业/租户>")...`
- **查询**：所有流程定义 / 实例 / 任务 / 历史查询**必须带 `TENANT_ID_` 条件**
- **覆盖范围**：`ACT_RE_DEPLOYMENT`、`ACT_RE_PROCDEF`、`ACT_RU_EXECUTION`、`ACT_RU_TASK`、`ACT_HI_*` 均有 `TENANT_ID_` 列

⚠️ **两个正交维度，切勿混淆**

| 维度 | 字段 | 含义 |
|---|---|---|
| 行业 / 租户隔离 | `TENANT_ID_` | 区分电商、OA 等不同业务域 |
| 测试态隔离 | `IS_TEST_`（§11 新增） | 区分测试流程与生产流程 |

⇒ 查询须同时携带：`WHERE TENANT_ID_ = ? AND IS_TEST_ = 0`

⚠️ **索引须相应改为复合索引**（修订 §11.3.2）：

```sql
CREATE INDEX IDX_HI_PRO_T_BIZSTATUS ON ACT_HI_PROCINST(TENANT_ID_, BUSINESS_STATUS_);
CREATE INDEX IDX_HI_PRO_T_ISTEST    ON ACT_HI_PROCINST(TENANT_ID_, IS_TEST_);
CREATE INDEX IDX_HI_PRO_T_DEFID     ON ACT_HI_PROCINST(TENANT_ID_, DEF_ID_);
CREATE INDEX IDX_HI_PRO_T_DATAID    ON ACT_HI_PROCINST(TENANT_ID_, DATA_ID_);
-- 列表页「筛选 + 排序」复合
CREATE INDEX IDX_HI_PRO_T_LIST      ON ACT_HI_PROCINST(TENANT_ID_, IS_TEST_, BUSINESS_STATUS_, START_TIME_);
```

### 13.4 异步执行器按需隔离（源码实证：原生支持）

Flowable 8 的 `flowable-job-service/.../asyncexecutor/multitenant/` 已原生提供多租户执行器：

| 类 | 用途 |
|---|---|
| `ExecutorPerTenantAsyncExecutor` | **每租户一个执行器**（隔离最强） |
| `SharedExecutorServiceAsyncExecutor` | 共享线程池，按租户获取作业 |
| `TenantAwareAcquireAsyncJobsDueRunnable` / `TenantAwareAcquireTimerJobsRunnable` | 按租户获取异步 / 定时作业 |
| `TenantAwareExecuteAsyncRunnable` / `TenantAwareResetExpiredJobsRunnable` | 按租户执行 / 重置过期作业 |

**分档方案**

| 场景 | 方案 | 说明 |
|---|---|---|
| **默认（推荐）** | 仅 `blade-workflow` 运行引擎与执行器 | 单执行器即可，**无需额外隔离**——因 `ACT_*` 为 workflow 专用，不存在跨应用争抢作业 |
| 将来多服务各自运行引擎 | `ExecutorPerTenantAsyncExecutor`（每行业独立）或 `SharedExecutorServiceAsyncExecutor`（共享线程池 + 按租户获取） | 原生支持，无需改源码 |
| 不开异步执行器（**当前 `asyncExecutorActivate=false`**） | 无作业表竞争 | 最安全，见 §12.9.5 |

⚠️ **若开启异步且共用库而不做租户隔离**：不同行业的 job executor 会**互相消费对方的作业**，执行方可能没有对应流程定义或委托类 ⇒ 作业失败 / 语义错乱。

### 13.5 共享 vs 隔离清单（做好区分）

| 资源 | 共享 / 隔离 | 说明 |
|---|---|---|
| `ACT_*` 表（同一 schema） | **共享**（workflow 专用） | 其它行业经 API 访问，禁止直连 |
| 引擎实例与配置 | **共享**（仅 workflow 服务） | 历史级别、解析器、自定义列口径统一 |
| BPMN 流程定义 | **按 `TENANT_ID_` 隔离** | 部署与查询带 tenantId |
| 流程实例 / 任务 / 历史 | **按 `TENANT_ID_` 隔离** | |
| 异步作业 | **按需隔离**（默认单执行器；多引擎时用 TenantAware） | §13.4 |
| `wf:` BPMN 扩展语义 | **per-定义** | 天然隔离，互不影响 |
| §11 自定义列 | **共享表结构，按值区分** | 列定义共享；数据靠 `TENANT_ID_` + 业务值区分 |
| `WfTimeoutJob` 超时扫描 | **共享单实例** | 扫描须带 `TENANT_ID_` 与 `IS_TEST_` 条件 |

### 13.6 对前文方案的影响（须同步修订）

| 前文 | 修订 |
|---|---|
| §11.3.2 索引方案 | 自定义列索引改为**带 `TENANT_ID_` 的复合索引**（见 §13.3） |
| §11.3.3 写入改造 | 发起流程时**必须写入 `TENANT_ID_`** |
| §11.3.4 存量回填 | 回填 SQL 须补 `TENANT_ID_` 赋值（按业务归属回填） |
| §12.10.2 检索优化 | 所有查询须带 `TENANT_ID_`；分区策略可按租户再切分 |

### 13.7 新增验证点与决策点

| 编号 | 类型 | 内容 |
|---|---|---|
| **V19** | 验证 | 多租户下**跨租户越权**验证：A 租户查询不得返回 B 租户数据（实例 / 任务 / 历史） |
| **V20** | 验证 | 异步执行器租户隔离验证（若采用 TenantAware）：A 租户作业不被 B 租户执行器消费 |
| **V21** | 验证 | `IS_TEST_` 与 `TENANT_ID_` 组合过滤的正确性（测试流程不得出现在任何租户的生产列表） |
| **D4** | 决策 | `TENANT_ID_` 取值规范：行业编码？还是复用 SpringBlade 租户 ID？ |
| **D5** | 决策 | 各行业是**共用同一套流程定义模板**还是**各自独立定义**？（影响 `KEY_` 命名与部署策略） |

### 13.8 `TENANT_ID_` 覆盖实测：并非所有 `ACT_*` 表都有租户列

> 多业务域共用引擎表时，"区分"是**逻辑区分（靠列值过滤），不是物理分表**——所有域的数据交错存放在同一张表里。更关键的是：**并非每张 `ACT_*` 表都有 `TENANT_ID_`**。

**`blade` 库实测结果**

| 类别 | 表 | 隔离方式 |
|---|---|---|
| ✅ **有 `TENANT_ID_`（可直接隔离）** | `act_re_deployment`、`act_re_procdef`、`act_re_model` | 直接按租户过滤 |
| | `act_ru_execution`、`act_ru_task`、`act_ru_actinst`、`act_ru_event_subscr`、`act_ru_identitylink` | 直接 |
| | **全部作业表**：`act_ru_job`、`act_ru_timer_job`、`act_ru_deadletter_job`、`act_ru_suspended_job`、`act_ru_external_job`、`act_ru_history_job` | 直接（⇒ 执行器租户隔离可行） |
| | `act_hi_procinst`、`act_hi_taskinst`、`act_hi_actinst`、`act_hi_tsk_log` | 直接 |
| | `act_id_user` | 直接 |
| ❌ **无 `TENANT_ID_`（须间接隔离）** | **`act_hi_comment`**（审批意见） | 仅有 `PROC_INST_ID_` ⇒ **须 JOIN 实例表** |
| | `act_ru_variable`、`act_hi_varinst`（变量） | 仅有 `PROC_INST_ID_` ⇒ 须 JOIN |
| | `act_hi_detail`、`act_hi_attachment`、`act_hi_identitylink` | 须 JOIN |
| | **`act_ge_bytearray`**（BPMN XML / 大对象） | ❌ 既无 `TENANT_ID_` **也无 `PROC_INST_ID_`** ⇒ 只能经 `ACT_RE_DEPLOYMENT` 间接隔离 |
| | `act_hi_entitylink`、`act_ru_entitylink`、`act_procdef_info`、`act_evt_log`、`act_ge_property`、`act_id_*` | 间接或无需隔离 |

**⇒ 三条重要影响**

1. 🔴 **审批日志无法直接按租户过滤**：`wf_approval_log` 迁移到 `ACT_HI_COMMENT`（§3.4）后，因该表**无 `TENANT_ID_`**，任何"某行业的审批意见"查询**必须 JOIN `ACT_HI_PROCINST`（借其 `TENANT_ID_`）**。这是此前未识别的遗漏点。
2. **变量查询同理须 JOIN**：按租户检索流程变量不能直接过滤。
3. **`ACT_GE_BYTEARRAY` 隔离最弱**：既无租户也无实例列，只能靠 deployment 关联 ⇒ 进一步印证 §12.3 "该表为语义唯一载体、不可随意清理"，且清理时须按租户对应的 deployment 精确操作。

**⇒ 补充验证点**

| 编号 | 内容 |
|---|---|
| **V22** | 验证 `ACT_HI_COMMENT` / `ACT_RU_VARIABLE` 等**无租户列表**的查询是否已通过 JOIN 实例表正确带上租户条件（防跨域泄漏） |

**⇒ 风险提示**：由于是逻辑区分，**任何一条查询漏带 `TENANT_ID_` 即造成跨业务域数据泄漏**。建议：
- 在数据访问层**统一封装租户条件**（而非散落在各业务方法）；
- 以 **V19 / V22** 作为上线前的强制校验项。

---

## 十四、改引擎源码后的性能评估（高并发与检索）

> 前提：本文档已确定**会改动 `flowable-engine` 源码**（本地源码构建，`8.1.0-SNAPSHOT`）。本节评估其性能影响。

### 14.1 总体判断

**"改源码"这个动作本身对性能影响有限；真正的高并发风险来自"改在哪里"和"数据库层"。**

| 影响来源 | 对性能的影响 | 等级 |
|---|---|---|
| 增加实体字段 + `getPersistentState()` | 轻微（脏检查开销随字段数线性增加） | 🟡 低 |
| MyBatis mapper 列变多 | 轻微（语句变长、bulkInsert 单行变大） | 🟡 低 |
| **在 `ActivityBehavior` / Command 热路径加逻辑** | 🔴 **可能致命**（每个节点流转都执行） | 🔴 高 |
| **`ACT_RU_EXECUTION` 更新次数增加 → `REV_` 乐观锁冲突** | 🔴 并发重试增多、吞吐下降 | 🔴 高 |
| `ACT_RU_*` 索引增加 | 写入变慢（B-tree 维护） | 🟠 中 |
| 连接池（Druid 默认 `maxActive=8`） | 🔴 高并发直接成为瓶颈 | 🔴 高 |

### 14.2 高并发下的具体风险

**① 热路径禁止同步 I/O（最重要）**

`ActivityBehavior` 位于引擎**热路径**——每个节点进入/离开都会执行。若在其中做**同步远程调用、复杂查询、大结果集读取**：
- 直接拉长 Command 执行时间 → 同一 CommandContext 事务持锁时间变长
- 并发下排队 → 吞吐断崖式下降

⇒ **红线**：热路径内**只允许**内存计算或已缓存数据；任何 I/O 必须异步化或预加载。

**② 乐观锁 `REV_` 冲突加剧**

- `ACT_RU_EXECUTION` / `ACT_RU_TASK` 带 `REV_` 乐观锁；并发更新同一 execution（如会签多人同时审批）会抛 `FlowableOptimisticLockingException` 并重试。
- **改源码若增加对 `ACT_RU_EXECUTION` 的写次数**，冲突概率上升 ⇒ 高并发下重试风暴、吞吐下降。
- ⇒ **红线**：不要在每次流转都更新实例级自定义列；自定义列尽量**一次性写入**（§12.10.1 的 A/B 类）。

**③ 数据库行锁与持锁时间**

- 同一流程实例的并发操作（会签、并行分支）竞争 `ACT_RU_EXECUTION` 行锁。
- 自定义列更新虽不改变锁粒度，但**更新变慢会延长持锁时间**。

**④ `ACT_RU_*` 索引的写入代价**

- `ACT_RU_TASK` 为高频写表；我们计划加 `DUE_DATE_`、`TIMEOUT_HANDLED_` 索引（§11.3.2）。
- 每个索引增加 INSERT / UPDATE / DELETE 的 B-tree 维护成本。
- ⇒ **克制**：只加查询必需的；`ASSIGNEE_`(RU)、`TASK_DEF_KEY_` 暂不加，压测后再定。

**⑤ 异步执行器（若开启）**

- 默认线程池偏小（core=2 / max=10）⇒ 高并发下作业积压。
- 必须显式调参；多域共用时按租户隔离（§13.4）。

**⑥ 连接池**

- ⚠️ 此前已发现**全仓库零池参数配置**，Druid 默认 `maxActive=8`；且去 `wf_*` 后 workflow 连接全部压到 `blade` 库。
- ⇒ 高并发下**连接池会先于数据库成为瓶颈**，必须显式配置并压测。

### 14.3 检索性能评估

| 场景 | 现状 / 预期 | 优化 |
|---|---|---|
| 当前数据量 | 约 750 行 / 4.5 MB（dev） ⇒ 任何查询都很快 | 不代表生产 |
| **`ACT_HI_*` 线性增长** | 🔴 长期最大风险 | 按时间 RANGE **分区** + 冷热分离 |
| 带 `TENANT_ID_` 的列表查询 | 有复合索引则快 | 复合索引（§13.3） |
| **无租户列表**（`act_hi_comment` / 变量） | 须 JOIN 实例表 ⇒ JOIN 成本 | 尽量按 `PROC_INST_ID_` 驱动（该列有索引） |
| 按**变量值**检索 | ❌ 无索引，全扫 | 高频字段列化（§11），勿走变量 |
| BPMN 节点语义 | 引擎有 `BpmnModel` 缓存（§12.10.2 已验证） | 关注缓存命中率，必要时调 `processDefinitionCacheLimit` |
| 深分页 / `COUNT(*)` | 大 OFFSET 与实时统计慢 | 游标翻页；统计走缓存/异步物化 |
| `ACT_GE_BYTEARRAY` | 1.50 MB 且随 BPMN 扩展增大 | 独立表空间，避免挤占 buffer pool |

### 14.4 改源码的"红线"清单

| # | 红线 | 理由 |
|---|---|---|
| 1 | 热路径（`ActivityBehavior` / Command）内**禁止同步 I/O** | 拉长事务、并发排队 |
| 2 | **禁止**在热路径加自定义锁 / 长事务 | 死锁与吞吐下降 |
| 3 | **禁止**每次流转都更新实例级自定义列 | 加剧 `REV_` 冲突 |
| 4 | `ACT_RU_*` 索引**克制** | 写入成本 |
| 5 | 自定义字段数量**克制** | `getPersistentState()` 脏检查成本 |
| 6 | 不得破坏 `getPersistentState()` 与 mapper 的一致性 | 漏登记 ⇒ 更新不落库（§12.1 经典坑） |

### 14.5 建议的压测与监控（必做）

**压测场景（至少覆盖）**
1. 会签/或签多人**并发审批同一实例**（测 `REV_` 冲突率）
2. 高并发发起流程（测 `ACT_RU_EXECUTION` INSERT 吞吐）
3. 并行网关多分支同时流转
4. 超时扫描与业务操作并发（测 `ACT_RU_TASK.DUE_DATE_` 索引下的扫描开销）
5. 多租户混合负载（测复合索引有效性与跨域隔离正确性）

**监控指标**
- `FlowableOptimisticLockingException` **发生频率**（关键：并发健康度）
- 引擎 Command 平均耗时 / P99
- 连接池活跃数、等待数（Druid）
- `ACT_RU_*` 写入 TPS 与慢 SQL
- 异步作业积压数、`ACT_RU_DEADLETTER_JOB` 数量
- `BpmnModel` 缓存命中率

### 14.6 新增验证点

| 编号 | 内容 |
|---|---|
| **V23** | 压测：`REV_` 乐观锁冲突率与重试风暴阈值（会签并发场景） |
| **V24** | 压测：连接池参数（`maxActive` 等）调优前后的吞吐对比 |
| **V25** | 压测：`ACT_RU_TASK` 新增索引后的写入 TPS 衰减幅度 |
| **V26** | 检索：历史表到万 / 十万级后带租户复合索引的查询耗时，据此确定分区启动时点 |

> ⚠️ **说明**：本节为**定性评估**，不含实测数据（当前库仅 750 行，不具代表性）。所有结论需经 §14.5 压测验证后方可作为容量与参数依据。

---

## 十五、开发前交叉验证与遗漏排查（评审 / 任务拆解用）

> 本节对照文档与 `D:\workproject\springbladeandreact\flowable-engine` 源码做交叉验证，按九个维度排查遗漏、边界场景与未考虑的改造点，并给出开发前必补的待确认事项与风险清单。

### 15.0 本次交叉验证已确认的关键结论

| 项 | 结论 | 源码证据 |
|---|---|---|
| `executeCommand` 是否同事务 | ✅ **是**（在已有 Command 上下文中**复用**上下文） | `CommandContextInterceptor`：L60 取 `Context.getCommandContext()`；L80 仅当 `!config.isContextReusePossible() \|\| commandContext == null \|\| commandContext.getException() != null` 才新建；L89-94 **else 复用**（日志"Valid context found. Reusing it"） |
| ⚠️ 复用失效的边界 | ① 使用 `contextReusePossible=false` 的自定义 `CommandConfig`；② **当前上下文已处于异常 / 回滚态**（L80 `commandContext.getException() != null`）时会新建上下文 | 同上 |
| 引擎多租户执行器 | ✅ 原生支持 | `flowable-job-service/.../asyncexecutor/multitenant/`：`ExecutorPerTenantAsyncExecutor`、`SharedExecutorServiceAsyncExecutor`、`TenantAware*` |
| 异步执行器是否有租户字段 | ❌ `DefaultAsyncJobExecutor` 本身**无** `tenantId`（租户隔离靠 multitenant 包的实现类，非在 Default 上加配置） | 源码核查 |

### 15.1 数据模型映射 —— 遗漏项

| # | 遗漏 | 说明与建议 |
|---|---|---|
| **M1** 🔴 | **业务定义与引擎定义是 1:N（版本）关系，缺桥接** | `wf_process_definition`（业务 `defId`）对应 `ACT_RE_PROCDEF` 的**多个版本**（每次部署生成新 `ID_`，`KEY_` 不变）。§11 只加了 `DEF_ID_`，**无法从引擎 `PROC_DEF_ID_` 反查业务 defId / 版本**。建议：在 `ACT_HI_PROCINST` **同时冗余 `DEF_KEY_`**（= `ACT_RE_PROCDEF.KEY_`），或业务侧维护 `defId ↔ procDefKey` 映射 |
| **M2** | **`DEF_ID_` 无法区分定义版本** | 版本相关统计须用 `PROC_DEF_ID_`（已存在于 `ACT_HI_PROCINST`），需在查询层明确分工 |
| **M3** | **`ACT_HI_PROCINST.ID_` 与 `PROC_INST_ID_` 是否相等未验证** | 回填 SQL 用 `w.engine_inst_id = h.PROC_INST_ID_`；需验证两者在历史表中是否恒等（并入 V27） |
| **M4** | **非任务节点无 `ACT_*` 记录** | 网关 / 事件节点无 task，其"节点信息"仅存在于 BPMN 扩展与 `ACT_RU_EXECUTION.ACT_ID_`；节点维度统计（当前节点分布）对非任务节点无表可查 |
| **M5** | **`wf_form_snapshot` 无 `ACT_*` 对应** | 必须保留独立**非 `wf_`** 表（§2.2 已列），须明确其与 `DATA_ID_` 的关联口径 |
| **M6** | 存量回填的**空值 / 类型**处理 | `DEF_ID_` 等用 BIGINT（业务雪花 ID）；存量 `engine_inst_id` 为空的行会**漏回填**，需单独统计与人工核对 |

### 15.2 流程定义与实例的关联 —— 遗漏项

| # | 遗漏 | 说明与建议 |
|---|---|---|
| **P1** 🔴 | **定义删除会连带丢失节点语义** | Flowable 删除 deployment 会级联删除 `ACT_RE_PROCDEF` 与 `ACT_GE_BYTEARRAY` ⇒ **历史实例的节点 / 出口语义无法还原**。须在运维规范中**禁止删除仍有历史实例引用的旧版本定义**（§12.3 已提，此处补充为**具体禁令**） |
| **P2** | `CATEGORY_` 与 `TENANT_ID_` 职责可能重叠 | 多域共用下需明确：`TENANT_ID_` = 行业/租户隔离；`CATEGORY_` = 业务分类（或不用）。避免两套分类并行造成查询歧义 |
| **P3** | 定义灰度 `wf_definition_gray` 的新家 | 属平台配置，保留非 `wf_` 表；需明确灰度选择的是 `ACT_RE_PROCDEF` 版本 |
| **P4** | 在途实例与旧版本并存 | 存量在途实例仍指向旧 `PROC_DEF_ID_`，其节点语义在旧 BYTEARRAY ⇒ 新版本部署后**两套语义并存**，查询须按实例的 `PROC_DEF_ID_` 解析对应版本 |

### 15.3 历史数据迁移 —— 遗漏项

| # | 遗漏 | 说明与建议 |
|---|---|---|
| **H1** 🔴 | **存量会签任务无法 1:1 迁移** | 现状是"单 userTask + `wf_task` 多条待办"自研会签；`ACT_RU_TASK` / `ACT_HI_TASKINST` 中**只有一条任务** ⇒ 存量会签明细**无法还原**。建议：**存量仅迁移实例级**，任务明细保留归档（非 `wf_` 命名只读归档表） |
| **H2** 🔴 | **`wf_approval_log` → `ACT_HI_COMMENT` 无法冗余租户** | `act_hi_comment` **无 `TENANT_ID_` 列**（§13.8 实测）⇒ 迁移时租户只能靠 `PROC_INST_ID_` 关联实例继承，**无法在 comment 上落租户列**（除非再加列，需决策） |
| **H3** | 迁移顺序与幂等 | 建议顺序：定义期语义回填 → 实例级回填 → 审批日志 → 任务（仅实例级）/归档。每步须可重跑、可校验计数 |
| **H4** | `wf_migration_map` 不可随 `wf_*` 一起删 | ecology 存量对账凭据（§12.3 已列） |

### 15.4 事务与并发 —— 遗漏项

| # | 遗漏 | 说明与建议 |
|---|---|---|
| **T1** ✅ | `executeCommand` 同事务**已确认成立**（§15.0），但须注意两个失效边界 | 使用 `contextReusePossible=false` 的配置，或当前上下文已异常 ⇒ 会新建上下文/事务 |
| **T2** 🔴 | **`WfTimeoutJob` 多副本部署会重复执行** | 现为 Spring `@Scheduled`，**无分布式锁**；服务多副本时每个副本都会扫描并触发超时 ⇒ **重复自动通过 / 重复提醒**。去 `wf_*` 后改为扫 `ACT_RU_TASK` 同样存在此问题。⇒ **必须引入分布式锁或改由单实例调度**（如 XXL-Job / ShedLock / 选主） |
| **T3** 🔴 | **超时触发与用户手动办理并发** | 扫描到任务并 `autoApprove` 的同时用户手动审批 ⇒ 竞争同一 execution / task ⇒ 一方乐观锁失败。需幂等 + 失败重试/忽略策略（不能静默丢失，见 E1） |
| **T4** | 会签并发审批 | `REV_` 乐观锁冲突（§14.2 ②），压测验证（V23） |

### 15.5 接口兼容性 —— 遗漏项

| # | 遗漏 | 说明与建议 |
|---|---|---|
| **I1** 🔴 | **"查询契约不变"的承诺需重新审视** | §12.9.4 已决定**不保留投影表** ⇒ 接口返回 JSON 虽可保持，但**实现全变**，且复杂筛选 / 排序 / 分页受 `ACT_*` 索引与 JOIN 能力限制 ⇒ 需**逐个接口**梳理可行性（可能有部分筛选需降级或改走只读物化） |
| **I2** | `blade-workflow-api` 的实体被上下游依赖 | 22 张 `wf_*` 实体若删除，需确认是否被其它模块 / Feign DTO 引用 ⇒ 需先做依赖扫描 |
| **I3** | `WfMonitorController` 指标改造 | `listenerRegistered` 等指标需改为反映新架构（异步执行器状态、deadletter 数、BpmnModel 缓存命中率等，§14.5） |

### 15.6 权限与审计 —— 遗漏项

| # | 遗漏 | 说明与建议 |
|---|---|---|
| **A1** | `ACT_ID_*` 全为 0 行 | 项目**不使用 Flowable 身份模块**（人员来自 SpringBlade）⇒ 若将来用引擎原生 `candidateGroups` / identitylink 需先做身份集成 |
| **A2** | `act_evt_log` 为 0 行 | 引擎事件日志未启用 ⇒ 是否启用 Flowable 事件日志作为审计留痕，需决策（否则审计仅有 `ACT_HI_COMMENT` + 业务日志） |
| **A3** | 审批日志无租户列 | 见 H2 / §13.8 |
| **A4** | 字段权限 / 操作者改读 BPMN 扩展 | `WfAuthUtil`、`WfNodeFieldPerm` 等去 `wf_*` 后须改读 BPMN 扩展，需一并改造（此前方案对此着墨不足） |

### 15.7 异常回滚 —— 遗漏项（含现存缺陷）

| # | 遗漏 | 说明与建议 |
|---|---|---|
| **E1** 🔴 | **超时动作失败却仍标记"已处理"（现存缺陷）** | `WfTimeoutServiceImpl.fire()` 中：动作执行 `try/catch` 仅 `log.warn`，随后**无论成败都执行** `task.setTimeoutHandled(1)` ⇒ **超时动作失败后不会重试，静默丢失**。迁移到 `ACT_RU_TASK.TIMEOUT_HANDLED_` 时**必须修正**：仅成功才置位，失败走重试 / 死信 |
| **E2** | 监听器 `isFailOnException` 语义需重定义 | 当前 `true`（强一致）；去 `wf_*` 后监听器若仍用于写 `ACT_HI_COMMENT` 或只读物化，其失败是否回滚引擎操作需重新约定 |
| **E3** | 异步开启后强一致失效 | 已 §12.8.3 / §3.4；须配套死信告警 |
| **E4** | 补写失败无补偿 | §11 自定义列补写（即便同事务）仍可能因上下文复用失效（T1）而落到独立事务 ⇒ 需兜底巡检 + 对账 |

### 15.8 测试覆盖 —— 需新增清单

| 类别 | 需覆盖 |
|---|---|
| 迁移 | 定义期语义回填对账、实例级回填对账、审批日志迁移、漏回填（`engine_inst_id` 为空）处理 |
| 保真 | BPMN 扩展往返保真（V7/V13）；定义版本回滚后语义可还原（V14） |
| 隔离 | 多租户越权（V19）、无租户列表 JOIN 隔离（V22）、`IS_TEST_`+`TENANT_ID_` 组合（V21）、执行器租户隔离（V20） |
| 并发 | 会签并发审批（V23）、超时与手动办理并发（T3）、连接池（V24）、写入 TPS 衰减（V25） |
| 语义 | 或签/会签/依次 + 驳回回多实例、驳回、撤回、跳转（含跳到非等待态）、自由流、并行网关汇聚、暂停/恢复/终止 |
| 超时 | 两类动作（提醒 / 自动通过）各自幂等；动作失败**不**误标记（E1 回归） |
| 检索 | 历史表万/十万级带租户复合索引耗时（V26） |

### 15.9 上下游模块影响

| 模块 | 影响 | 需做 |
|---|---|---|
| **`blade-formmode`** | `WfTimeoutServiceImpl.formDate` 经 Feign 取表单数据 | 不受影响 |
| **`blade-system`** | 操作者解析依赖组织 / 用户接口 | 不受影响 |
| **前端设计器（bpmn-js）** 🔴 | 属性面板须改为编辑 BPMN 扩展 ⇒ **必须新增 `wf:` 的 moddle 扩展定义文件**，否则设计器无法解析/保存扩展 | **明确为交付物**（详见 §十六） |
| **`blade-mall` / `order` / `pay`** | 共用引擎，经 workflow API 接入 | 遵守 §13（`ACT_*` workflow 专用、`TENANT_ID_` 隔离） |
| **`blade-workflow-api`** | 22 个 `wf_*` 实体可能被引用 | 依赖扫描（I2） |
| **`ecology-to-blade-migrator`** | 存量迁移工具可能依赖 `wf_*` | 确认并同步改造 |

### 15.10 开发前必补的待确认事项

| 编号 | 事项 | 影响 |
|---|---|---|
| **D6** | 是否新增 `DEF_KEY_`（引擎 `KEY_`）以桥接业务 defId 与引擎定义版本？（M1） | 数据模型定稿 |
| **D7** | 存量会签任务明细是否放弃迁移、改为归档？（H1） | 迁移范围 |
| **D8** | `ACT_HI_COMMENT` 是否**新增租户列**以支撑审批日志按租户检索？（H2） | 是否再次加列 |
| **D9** | `WfTimeoutJob` 多副本调度如何治理：分布式锁 / XXL-Job / 选主？（T2） | 并发正确性 |
| **D10** | 接口筛选能力盘点：哪些筛选在去投影表后**无法实现或需降级**？（I1） | 接口改造范围 |
| **D11** | 是否启用 Flowable 事件日志作审计？（A2） | 审计方案 |
| **D12** | 前端 `wf:` moddle 扩展由谁交付、何时交付？（§16） | 设计器改造前置 |

### 15.11 风险清单（开发前）

| 编号 | 风险 | 等级 |
|---|---|---|
| **R1** | 业务定义 ↔ 引擎定义 1:N 缺桥接，导致反查 / 版本统计无解 | 🔴 高 |
| **R2** | 存量会签任务明细无法 1:1 迁移 | 🔴 高 |
| **R3** | `WfTimeoutJob` 多副本重复触发超时 | 🔴 高 |
| **R4** | 超时动作失败被误标记"已处理"（现存缺陷，须修） | 🔴 高 |
| **R5** | 去投影表后接口筛选能力下降，部分接口需降级 | 🟠 中 |
| **R6** | 定义删除导致历史实例节点语义丢失（须立运维禁令） | 🟠 中 |
| **R7** | 审批日志（`ACT_HI_COMMENT`）无租户列，隔离须 JOIN | 🟠 中 |
| **R8** | 前端 `wf:` moddle 扩展未交付 ⇒ 设计器无法编辑扩展 | 🟠 中 |

### 15.12 建议的任务拆解（可评审）

| # | 任务 | 依赖 | 产出 |
|---|---|---|---|
| **T-1** | 依赖扫描：`wf_*` 实体在上下游的引用面 | — | 影响清单 |
| **T-2** | 数据模型定稿（含 D6 `DEF_KEY_`、D8 租户列决策） | T-1 | DDL 终稿 |
| **T-3** | BPMN 扩展 schema 定稿 + **往返保真测试** | — | schema + 测试 |
| **T-4** | 前端 `wf:` **moddle 扩展定义**（D12） | T-3 | moddle 文件 + 注册 |
| **T-5** | 定义期语义回填（节点/出口/操作者/权限/超时 → BPMN）+ 双轨对账 | T-3 | 迁移脚本 + 对账 |
| **T-6** | `ACT_*` 加列 + 复合（租户）索引 | T-2 | DDL 脚本 |
| **T-7** | 实例级回填（含 `TENANT_ID_` 赋值）+ 校验 | T-6 | 回填脚本 |
| **T-8** | 超时链路改造：扫 `ACT_RU_TASK.DUE_DATE_` + **修 E1 缺陷** + **多副本治理（D9）** | T-6 | 代码 + 调度方案 |
| **T-9** | 审批日志迁移到 `ACT_HI_COMMENT`（含 D8） | T-7 | 迁移脚本 |
| **T-10** | 接口改造：逐个盘点筛选能力（D10） | T-6/T-7 | 接口实现 |
| **T-11** | 权限/操作者改读 BPMN 扩展（A4） | T-5 | 代码 |
| **T-12** | 并发与性能压测（V23–V26） | T-8/T-10 | 压测报告 |
| **T-13** | 多租户隔离验证（V19–V22） | T-7/T-10 | 验证报告 |
| **T-14** | 存量 `wf_*` 退役（保留归档与 `wf_migration_map`） | 全部 | 退役清单 |

---

## 十六、前端（`ant-design-pro`）改造分析

> 目标：评估去 `wf_*`、改用 Flowable 原生承载后，前端是否需要同步改造、改哪些、怎么改。

### 16.1 现状盘点（`ant-design-pro/src` 实测）

**流程业务页面**

| 页面 | 作用 |
|---|---|
| `pages/Workflow/Todo/Todo.tsx` | 待办 |
| `pages/Workflow/Done/Done.tsx` | 已办 |
| `pages/Workflow/Request/Request.tsx` | 我的请求 |
| `pages/Workflow/Create/{Create,Start,InstanceFlow}.tsx` | 发起 / 实例流转 |
| `pages/System/Workflow/Workflow.tsx` | 流程管理 |
| `pages/FormMode/Approval/ApprovalPage.tsx` | 办理页 |
| `pages/FormMode/ExcelDesign/components/ApprovalFormRender.tsx` | 审批表单渲染 |
| `pages/FormMode/Test/*` | 测试相关 |

**流程设计器 `pages/FormMode/WorkflowDesign/`（改造重灾区）**

`BpmnDesigner.tsx`、`NodeInfoPanel.tsx`、`NodeInfoTable.tsx`、`NodeSettingModal.tsx`、`NodeDetail.tsx`、`LinkInfoPanel.tsx`、`LinkDetail.tsx`、`useLinkActions.tsx`、`NodeOperatorModal.tsx`、`NodeTimeoutModal.tsx`、`NodeOperateMenuModal.tsx`、`NodeExtraOperateModal.tsx`、`CustomOperationModal.tsx`、`CustomActionRegisterModal.tsx`、`FormContentDesignModal.tsx`、`ConditionBuilder.tsx`、`SimulateModal.tsx`、`WorkflowTestModal.tsx`、`VersionDiffModal.tsx`，以及 `wfDict.ts`、`nodeSettings.ts`、`bpmnZh.ts`

**API 契约层**：`src/services/workflow/index.ts`（实测 **87+ 个接口**）

### 16.2 🔴 核心判断：这批"细粒度 CRUD 接口"在去 `wf_*` 后**失去承载对象**

实测到的、以 `wf_*` 表为中心的接口：

| 类别 | 接口 |
|---|---|
| 节点 | `listNodes(id)`、`updateNode(id, nodeKey, node)`、`deleteNode(id, nodeKey)`、`saveNodeTestStatus(id, nodeKey, status)` |
| 出口 | `listLinks(id)`、`createLink(id, link)`、`updateLink(id, linkId, link)`、`deleteLink(id, linkId)` |
| 操作者 | `getNodeOperators(id, nodeKey)`、`configOperator(id, nodeKey, operators)`、`syncOperatorToNodes(...)` |
| 权限 | `getFieldPerm` / `saveFieldPerm`、`getDetailPerm` / `saveDetailPerm`、`getDetailFilter` / `saveDetailFilter` |
| 自定义操作 | `listCustomActions` / `saveCustomAction` / `deleteCustomAction` |
| BPMN | `saveBpmn(id, bpmnXml)`、`getBpmn(id)`、`importDefinition({name,bpmnXml})`、`deployDefinition(id)` |
| 版本 | `saveAsNewVersion`、`activateVersion`、`listVersions`、`diffVersion` |
| 实例 | `startInstance`、`getInstance`、`getInstanceByBiz`、`getLogs`、`getSnapshot`、`getInstanceNodeOperators`、`markTaskViewed` |
| 其它 | `simulateDefinition`、`testDefinition`、`withdrawDefinition`、`getDefinitionFormCondition` |

⇒ 去 `wf_*` 后，**节点 / 出口 / 操作者 / 权限 / 超时 / 自定义操作的写入目标从"表"变成"BPMN 扩展"**，上述细粒度写接口不再有表可写。

### 16.3 两条改造路线

| | **路线 A：后端兼容层** | **路线 B：前端直改 BPMN（推荐）** |
|---|---|---|
| 做法 | 保留接口签名，后端改为"收请求 → 改 BPMN 扩展 → 存 bpmnXml" | 前端用 bpmn-js + `wf:` moddle **直接编辑扩展**，统一 `saveBpmn` 保存 |
| 前端改动 | **小**（面板几乎不动） | **大**（`NodeInfoPanel`/`LinkInfoPanel`/`NodeOperatorModal`/`NodeTimeoutModal`/`NodeOperateMenuModal`/`NodeExtraOperateModal`/`CustomOperationModal`/`ConditionBuilder`/`useLinkActions`/`nodeSettings.ts` 等几乎重写） |
| 后端改动 | 大（需做"BPMN 扩展读写 + 版本管理"适配层） | 中（只需扩展 schema + 导入解析） |
| 写放大 | ⚠️ 每次改一个节点属性都要读写整份 XML | ✅ 前端批量编辑，一次保存 |
| 并发编辑 | ⚠️ 多人改同一定义不同节点易冲突，需版本/乐观锁 | ✅ 与设计器天然一致，仍需版本控制 |
| 单一事实源 | ⚠️ 仍有一层适配，语义间接 | ✅ 真正落地 |

⇒ **推荐路线 B**（与 §八 "BPMN 为唯一事实源"一致），但**工期与风险需充分评估**；若前端资源紧张，可先走 A 过渡。

### 16.4 前端具体改造清单

| # | 改造点 | 说明 |
|---|---|---|
| **F1** 🔴 | **新增 `wf:` moddle 扩展定义**（D12，§15.9 已列、此处细化为交付物） | JSON 描述 `wf:node` / `wf:link` / `wf:operator` 及嵌套结构，注册到 bpmn-js，否则设计器无法解析/序列化扩展 |
| **F2** | 属性面板改为**读写 BPMN 元素扩展属性** | `NodeInfoPanel`、`NodeSettingModal`、`NodeDetail`、`NodeInfoTable` |
| **F3** | 出口面板改为**读写 `SequenceFlow`** | `LinkInfoPanel`、`LinkDetail`、`useLinkActions`；条件写入**原生 `conditionExpression`**（而非库字段 `conditionExpr`） |
| **F4** | `ConditionBuilder` 输出改为 BPMN 条件表达式 | 与 F3 联动 |
| **F5** | 操作者 / 超时 / 操作菜单 / 附加操作面板改为写扩展 | `NodeOperatorModal`、`NodeTimeoutModal`、`NodeOperateMenuModal`、`NodeExtraOperateModal` |
| **F6** | 保存链路收敛到 `saveBpmn` | 废弃 `updateNode`/`createLink`/`updateLink`/`configOperator`/`saveFieldPerm`/`saveDetailPerm` 等细粒度写接口（或保留为只读兼容） |
| **F7** | 字典与配置与 `wf:` schema 对齐 | `wfDict.ts`、`nodeSettings.ts`、`bpmnZh.ts` |
| **F8** | `VersionDiffModal` 支持**对比 BPMN 扩展差异** | 新增能力 |
| **F9** | 列表 / 办理页 | 若后端保持返回契约（I1），`Todo`/`Done`/`Request`/`InstanceFlow`/`ApprovalPage`/`ApprovalFormRender` 改动小；否则同步改 |
| **F10** | 雪花 ID 仍按**字符串**透传 | 既有约定（`rowKey` 用 `String(r.id)`、绝不做 `Number()`），新接口须沿用 |
| **F11** | 审批日志 / 快照 | `getLogs`（→ `ACT_HI_COMMENT`）、`getSnapshot`（保留独立表）若返回结构变化则同步 |

### 16.5 与后端方案的联动

| 后端决策 | 对前端的影响 |
|---|---|
| **I1（是否保留投影表）** | 直接决定 F9：不保留则列表接口实现全变，需逐个盘点筛选能力 |
| **T-3（BPMN 扩展 schema 定稿）** | F1/F7 的**前置**，schema 不定则前端无法开工 |
| **D13（节点测试态 `saveNodeTestStatus` 存哪）** | 若改存 BPMN 扩展或引擎变量，该接口需重新设计 |
| **版本管理策略** | 设计期修改存草稿 vs 每次生成新版本 ⇒ 影响 F6 保存逻辑 |

### 16.6 新增待确认与风险

| 编号 | 内容 |
|---|---|
| **D13** | 节点测试态（`saveNodeTestStatus`）去 `wf_*` 后存哪里？BPMN 扩展 / 引擎变量 / 独立表？ |
| **D14** | 走路线 A 还是路线 B？（决定前端工作量量级） |
| **D15** | 设计期每次修改是否生成新版本？还是草稿累积后统一部署？ |
| **R9** | 🔴 未交付 `wf:` moddle 扩展 ⇒ 设计器**无法**编辑扩展，前端改造阻塞 |
| **R10** | 🔴 路线 B 下前端改动面很大（近十个面板重写），工期与回归风险需评估 |
| **R11** | 🟠 多人并发编辑同一流程定义的不同节点 ⇒ 需版本/乐观锁策略（D15） |

### 16.7 任务拆解补充（前端）

| # | 任务 | 依赖 | 产出 |
|---|---|---|---|
| **F-T1** | `wf:` **moddle 扩展定义** | T-3（schema 定稿） | moddle JSON + 注册代码 |
| **F-T2** | 设计器画布支持扩展读写 | F-T1 | `BpmnDesigner` 改造 |
| **F-T3** | 节点 / 出口面板改造（F2/F3/F4） | F-T1 | 面板代码 |
| **F-T4** | 操作者 / 超时 / 菜单 / 附加操作面板改造（F5） | F-T1 | 面板代码 |
| **F-T5** | 保存链路收敛 + 废弃细粒度写接口（F6） | F-T3/F-T4 | API 层改造 |
| **F-T6** | 字典 / 版本对比 / 测试态对齐（F7/F8/D13） | F-T1 | 配置与组件 |
| **F-T7** | 列表 / 办理页契约核对（F9/F10/F11） | I1 决策 | 页面核对报告 |

---

## 十七、附录：评审用登记表（汇总）

> 将散落各章的 **D（待确认事项）/ V（验证点）/ R（风险）** 与任务拆解汇总于此，供开发评审与任务派发直接取用。
> 整理时修正两处：① **原 R8 与 R9 内容重复**（均为"`wf:` moddle 扩展未交付"）⇒ 合并为 **R8**，原 R9 编号**作废**；② **V27** 被 §15.1（M3）引用但未正式定义 ⇒ 此处补齐。

### 17.1 待确认事项登记表（D1–D15）

| 编号 | 事项 | 出处 | 阻塞对象 |
|---|---|---|---|
| **D1** | 业务终态改用原生 `BUSINESS_STATUS_`，需定稿取值规范（`APPROVED`/`REJECTED`/`CANCELED`） | §4.4 / §12.1 | 实例状态口径 |
| **D2** | `rejectToStarter` 的"合成待办"在多实例模式下如何表达 | §9.5 | 会签 + 驳回 |
| **D3** | 投影宽表长期保留还是仅过渡？保留则须明确同步时机与失效策略 | §12.9.2 | §11/§12.9.4 方案 |
| **D4** | `TENANT_ID_` 取值规范：行业编码？还是复用 SpringBlade 租户 ID？ | §13.7 | 多域隔离 |
| **D5** | 各行业共用同一套流程定义模板，还是各自独立定义？ | §13.7 | `KEY_` 命名与部署 |
| **D6** | 是否新增 `DEF_KEY_` 以桥接业务 defId 与引擎定义版本？ | §15.1（M1） | **数据模型定稿** |
| **D7** | 存量会签任务明细是否放弃迁移、改为归档？ | §15.3（H1） | 迁移范围 |
| **D8** | `ACT_HI_COMMENT` 是否新增租户列以支撑审批日志按租户检索？ | §15.3（H2） | 是否再次加列 |
| **D9** | `WfTimeoutJob` 多副本调度如何治理：分布式锁 / XXL-Job / 选主？ | §15.4（T2） | **并发正确性** |
| **D10** | 接口筛选能力盘点：去投影表后哪些筛选无法实现或需降级？ | §15.5（I1） | 接口改造范围 |
| **D11** | 是否启用 Flowable 事件日志作审计？ | §15.6（A2） | 审计方案 |
| **D12** | 前端 `wf:` moddle 扩展由谁交付、何时交付？ | §15.9 / §16.4（F1） | **前端开工前置** |
| **D13** | 节点测试态（`saveNodeTestStatus`）去 `wf_*` 后存哪里？ | §16.5 | 测试态接口 |
| **D14** | 前端走路线 A（后端兼容层）还是路线 B（直改 BPMN）？ | §16.3 | 前端工作量量级 |
| **D15** | 设计期每次修改是否生成新版本？还是草稿累积后统一部署？ | §16.6 | 保存链路（F6） |

**优先级建议**：**D6 / D12 / D14** 为开工前必须拍板（分别卡住数据模型、前端开工、前端工作量）；**D3 / D10** 决定接口改造范围；其余可并行推进。

### 17.2 验证点登记表（V1–V27）

| 编号 | 类型 | 内容 |
|---|---|---|
| **V1** | 性能 | `BUSINESS_STATUS_` 默认无索引 ⇒ 实测补索引后的查询计划与提升幅度 |
| **V2** | 结构 | 任务级业务状态如何承载（原 `ACT_RU_TASK.BUSINESS_STATUS_` 经实测**不存在**）⇒ 建议 JOIN 实例表，不新增列 |
| **V3** | 功能 | `addMultiInstanceExecution` 新增后 `completionCondition` 是否重新求值并把新增者计入 `nrOfInstances` |
| **V4** | 功能 | `changeState()` 跳回**已执行过**的活动时 `ACT_HI_ACTINST` 是否产生重复/异常记录 |
| **V5** | 功能 | `moveExecutionsToSingleActivityId` 并行汇聚退回时是否取消其余分支任务 |
| **V6** | 功能 | 加签后驳回回多实例节点，集合变量如何重建 |
| **V7** | 保真 | BPMN 扩展往返保真（解析 → 写回 → 断言无损） |
| **V8** | 功能 | `SetProcessInstanceBusinessStatusCmd` 是否**同时**更新 `ACT_RU_EXECUTION` 与 `ACT_HI_PROCINST` 的 `BUSINESS_STATUS_` |
| **V9** | 事务 | `executeCommand` 内写自定义列是否与引擎同事务（命令内写列后抛异常，验证是否回滚） |
| **V10** | 功能 | 并行网关 + 多实例下"当前活动集合"的表达 |
| **V11** | 回归 | 自定义 `ActivityBehaviorFactory` 替换后 userTask 生命周期是否正常 |
| **V12** | 回归 | `getPersistentState()` 未登记新字段时的脏检查失效（改源码前必测） |
| **V13** | 保真 | 往返保真 + 定义版本回滚后语义是否可还原 |
| **V14** | 保真 | 旧定义版本被清理后，历史实例能否还原节点/出口语义 |
| **V15** | 并发 | 多节点下 DDL 与投影表同步的幂等性 |
| **V16** | 性能 | `EXPLAIN` 验证新增索引是否被命中（`BUSINESS_STATUS_`、`DUE_DATE_`） |
| **V17** | 性能 | `getBpmnModel()` 缓存命中率（评估是否调大 `processDefinitionCacheLimit`） |
| **V18** | 性能 | 历史表万/十万级后的列表查询耗时（决定分区启动时点） |
| **V19** | 隔离 | 跨租户越权：A 租户查询不得返回 B 租户数据 |
| **V20** | 隔离 | 异步执行器租户隔离：A 租户作业不被 B 租户执行器消费 |
| **V21** | 隔离 | `IS_TEST_` + `TENANT_ID_` 组合过滤正确性 |
| **V22** | 隔离 | `ACT_HI_COMMENT` / `ACT_RU_VARIABLE` 等**无租户列表**是否通过 JOIN 正确带上租户条件 |
| **V23** | 并发 | `REV_` 乐观锁冲突率与重试风暴阈值（会签并发） |
| **V24** | 并发 | 连接池参数调优前后吞吐对比 |
| **V25** | 性能 | `ACT_RU_TASK` 新增索引后的写入 TPS 衰减幅度 |
| **V26** | 检索 | 历史表万/十万级带租户复合索引的查询耗时 |
| **V27** | 数据 | **`ACT_HI_PROCINST.ID_` 与 `PROC_INST_ID_` 是否恒等**（回填 SQL 依赖 `engine_inst_id = PROC_INST_ID_`） |

### 17.3 风险登记表（R1–R10，已去重）

| 编号 | 风险 | 等级 | 缓解 |
|---|---|---|---|
| **R1** | 业务定义 ↔ 引擎定义 1:N 缺桥接，反查 / 版本统计无解 | 🔴 高 | D6：新增 `DEF_KEY_` 或维护映射 |
| **R2** | 存量会签任务明细无法 1:1 迁移 | 🔴 高 | D7：仅迁实例级 + 明细归档 |
| **R3** | `WfTimeoutJob` 多副本重复触发超时 | 🔴 高 | D9：分布式锁 / 单实例调度 |
| **R4** | 超时动作失败被误标记"已处理"（**现存缺陷**） | 🔴 高 | 改为仅成功才置位 + 重试/死信 |
| **R5** | 去投影表后接口筛选能力下降，部分接口需降级 | 🟠 中 | D10：逐个盘点，必要时只读物化 |
| **R6** | 定义删除导致历史实例节点语义丢失 | 🟠 中 | 立运维禁令：禁止删除仍被引用的旧版本 |
| **R7** | 审批日志（`ACT_HI_COMMENT`）无租户列，隔离须 JOIN | 🟠 中 | D8：评估是否新增租户列 |
| **R8** | 🔴 前端 `wf:` moddle 扩展未交付 ⇒ 设计器无法编辑扩展，**前端改造阻塞**（**原 R8 与 R9 合并**） | 🔴 高 | D12：明确交付方与时间 |
| **R9** | ~~（作废，已并入 R8）~~ | — | — |
| **R10** | 路线 B 下前端改动面很大（近十个面板重写） | 🔴 高 | D14：评估工期与回归，可先走路线 A |
| **R11** | 多人并发编辑同一流程定义的不同节点 | 🟠 中 | D15：版本 / 乐观锁策略 |

### 17.4 任务总表（后端 T-1–T-14 + 前端 F-T1–F-T7）

| # | 任务 | 依赖 | 产出 |
|---|---|---|---|
| **T-1** | 依赖扫描：`wf_*` 实体在前后端/上下游的引用面 | — | 影响清单 |
| **T-2** | 数据模型定稿（含 D6、D8） | T-1 | DDL 终稿 |
| **T-3** | BPMN 扩展 schema 定稿 + 往返保真测试（V7/V13） | — | schema + 测试 |
| **T-4** | 前端 `wf:` moddle 扩展定义（D12） | T-3 | moddle + 注册 |
| **T-5** | 定义期语义回填 + 双轨对账 | T-3 | 迁移脚本 + 对账 |
| **T-6** | `ACT_*` 加列 + 租户复合索引 | T-2 | DDL 脚本 |
| **T-7** | 实例级回填（含 `TENANT_ID_`）+ 校验（V27） | T-6 | 回填脚本 |
| **T-8** | 超时链路改造：扫 `DUE_DATE_` + 修 R4 + 多副本治理（D9） | T-6 | 代码 + 调度方案 |
| **T-9** | 审批日志迁移到 `ACT_HI_COMMENT`（含 D8） | T-7 | 迁移脚本 |
| **T-10** | 接口改造：逐个盘点筛选能力（D10） | T-6 / T-7 | 接口实现 |
| **T-11** | 权限/操作者改读 BPMN 扩展（A4） | T-5 | 代码 |
| **T-12** | 并发与性能压测（V23–V26） | T-8 / T-10 | 压测报告 |
| **T-13** | 多租户隔离验证（V19–V22） | T-7 / T-10 | 验证报告 |
| **T-14** | 存量 `wf_*` 退役（保留归档与 `wf_migration_map`） | 全部 | 退役清单 |
| **F-T1** | `wf:` moddle 扩展定义 | T-3 | moddle JSON + 注册 |
| **F-T2** | 设计器画布支持扩展读写 | F-T1 | `BpmnDesigner` 改造 |
| **F-T3** | 节点 / 出口面板改造（F2/F3/F4） | F-T1 | 面板代码 |
| **F-T4** | 操作者/超时/菜单/附加操作面板改造（F5） | F-T1 | 面板代码 |
| **F-T5** | 保存链路收敛 + 废弃细粒度写接口（F6） | F-T3 / F-T4 | API 层改造 |
| **F-T6** | 字典 / 版本对比 / 测试态对齐（F7/F8/D13） | F-T1 | 配置与组件 |
| **F-T7** | 列表 / 办理页契约核对（F9/F10/F11） | I1 决策 | 页面核对报告 |

**关键路径**：`T-3 → T-4/F-T1 → F-T2~F-T5`（前端） 与 `T-2 → T-6 → T-7 → T-10`（后端主链）并行；`T-12/T-13` 收口验证；`T-14` 最后退役。

### 17.5 一页纸速查（核心结论）

1. **不改源码也能达成目标**：用 `BUSINESS_STATUS_`（原生）+ BPMN `extensionElements`（定义期语义）+ `executeCommand` 同事务写入 + `ActivityBehaviorFactory`（节点行为）+ 多租户执行器（原生）。
2. **业务字段已可列表化**：`ACT_HI_PROCINST` 加 `DEF_ID_/DATA_ID_/FORM_ID_/TITLE_/IS_TEST_` ⇒ **无需投影表**，方案C 真正退役。
3. **超时流转不用开定时器**：`WfTimeoutJob` + 原生 `DUE_DATE_` 即可（开 `asyncExecutorActivate=true` 会引入一致性降级、双轨重复、测试态外溢等一组新问题）。
4. **多域共用靠 `TENANT_ID_`**（逻辑隔离）；注意审批意见/变量表**无租户列**，须 JOIN。
5. **`ACT_*` 为 workflow 专用**，其它行业经 API 接入。
6. **四个必须先修/先定的硬问题**：R4（超时误标记缺陷）、R3（多副本重复触发）、R1（定义 1:N 缺桥接）、R8（moddle 未交付）。
7. **实际版本是 `8.1.0-SNAPSHOT`（本地源码构建）**，且 pom 已标注 `schema.version` 与 `CURRENT_VERSION` 不一致风险 ⇒ 建议加列前先确认。
