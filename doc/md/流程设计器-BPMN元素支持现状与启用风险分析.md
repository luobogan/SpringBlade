# 流程设计器 BPMN 元素支持现状与启用风险分析

> **总索引**：[工作流分析文档总索引.md](工作流分析文档总索引.md)（五份递进式分析文档的统一入口）
>
> **触发问题**：设计器"图形编辑 → 更改元素"菜单（见截图）能点出 任务 / 用户任务 / 服务任务 / 发送任务 / 接收任务 / 手工任务 / 脚本任务 / 调用活动 / 子流程（折叠）等元素。这些能力是否都已在 SpringBlade 上开发完成？若要真正用上，会有什么问题？
>
> **分析方法**：前端设计器直接读源码；后端按"定义解析 → 部署校验 → 运行期执行"三层逐元素核对，全部给出代码位置。本文是**当前实现现状 + 启用风险**的实测分析，与《E9与Flowable7流程节点功能对比及改造可行性评估.md》（理论可行性）互补。
>
> **日期**：2026-11-17
>
> **进阶文档**：若要把画布元素**全部真正实现**且**完整保留 SB 原有流程语义**，两套推进模型（业务层 vs 引擎驱动）的冲突与分阶段架构路线见《全量BPMN元素启用与原有流程语义兼容架构分析.md》。

---

## 0. 一句话结论

**画布能画 ≠ 后端能跑。** 真正开发完成的只有 **用户任务 / 创建 / 归档 / 网关 + 那套 OA 扩展（会签、抄送、意见、字段权限、超时）**；服务/脚本/调用活动/子流程/发送/接收/业务规则/中间与边界事件目前只是**被归一化为节点类型或静默降级，好让流程能部署能跑通**，对应的引擎语义基本未实现。

---

## 1. 前端现状：bpmn-js 建模能力全量放开

`BpmnDesigner.tsx` 的设计目标就是"把 bpmn-js 的建模能力**全部放开**"：

- `BpmnDesigner.tsx:44-53` 注释明确：左侧 Palette、元素右键上下文菜单、多选对齐浮动条**由 bpmn-js 原生提供**；右侧属性面板用 `BpmnPropertiesPanelModule`。
- `BpmnDesigner.tsx:367-381` 初始化 `BpmnJS`，挂 `camunda` + `wf` 两个 moddle 扩展及属性面板模块。
- **没有任何元素白名单/黑名单**：截图里的"更改元素"菜单是 bpmn-js 原生 replace menu，列出全部标准 BPMN 任务类型。

因此，画布上能画出来只说明 **bpmn-js 支持该元素**，不代表 SB 后端实现了对应语义。这是本文所有风险的源头。

---

## 2. 后端支持矩阵（实测）

### 2.1 节点类型值域

没有独立枚举类，权威口径散落在实体注释与映射代码中：

- `WfProcessNode.java:13-17,37-47`：`nodeType` = **0创建 / 1审批 / 2提交 / 3归档 / 5等待 / 6自动处理**（缺 4）；`signOrder` = 0或签 / 1会签 / 2依次 / 3抄送不需提交 / 4抄送需提交；`mergeType` = 0普通 / 1分叉起点 / 2分叉中间 / 3按分支数合并 / 4指定分支合并 / 5比例合并。
- `WfNodeSettingsUtil.java:728-776`：`SIGN_OR/AND/SEQ` 映射到引擎多实例完成条件。
- **没有任何 ServiceTask / ScriptTask / CallActivity / SubProcess / SendTask / ReceiveTask / ManualTask 的专属 nodeType**——它们一律被塞进上面的 1/5/6。

### 2.2 逐元素矩阵

| 画布元素 | 落表 nodeType | 解析/保存 | 部署 | 运行期实际行为 | 结论 |
|---|---|---|---|---|---|
| 用户任务 / 任务 | 0或1 | ✅ | ✅ | 待办、会签、意见、抄送、权限、超时齐全 | **完整支持** |
| 开始 / 结束（归档） | 创建=UserTask(0)、结束=3 | ✅ | ✅ | 引擎结束、归档人回写 | **完整支持**（按 SB 约定：创建是 UserTask 不是 StartEvent） |
| 排他/并行/包容网关 | 7 | ✅合成 | ✅（UEL 注入） | Flowable 原生分支 | **完整支持**（条件变量缺失会报错卡死） |
| 手工任务 ManualTask | 1 | ✅ | ✅ | 落表当审批，引擎侧却自动通过 | **部分支持/语义错配** |
| 服务任务 ServiceTask | 6 | ✅ | 通过（无校验） | 无 JavaDelegate，引擎空跑放行 | **画了不执行** |
| 脚本任务 ScriptTask | 6 | ✅ | 通过（无校验） | 无脚本引擎，引擎空跑 | **画了不执行** |
| 调用活动 CallActivity | 6 | ✅ | 通过（无校验） | 无 calledElement / 子流程回调 | **完全未实现** |
| 子流程（折叠）SubProcess | 不合成（内部 UserTask 递归扫到） | 部分 | 视内容 | 无展开/折叠/汇总语义 | **完全未实现 + 定义源缺节点** |
| 发送任务 SendTask | 6 | ✅ | **静默降级**为 manualTask | 自动通过，**不发任何通知** | **未实现** |
| 接收任务 ReceiveTask | 5"等待" | ✅ | 通过 | 无 signal/message 唤醒 → **实例永久停留** | **完全未实现** |
| 业务规则任务 BusinessRuleTask | 6 | ✅ | **硬拦**（测试态降级） | 测试绿灯、正式红 | **禁止发布** |
| 中间捕获/边界事件（含 timer） | 5 | 部分（save 落表、read 不合成） | timer 仅 warn | 正式环境真等待、无唤醒 → 卡死 | **部分支持/高风险** |
| 抄送、意见回写、字段权限、超时 | — | ✅ | ✅ | `wf_ext_task` / `ACT_HI_COMMENT` / `wf:fieldPerm` | **完整支持** |
| 分支数/比例合并 mergeType/passNum | — | 仅配置读写 | 无校验 | 无汇聚执行逻辑 | **完全未实现** |

### 2.3 关键代码证据

**解析（`WfBpmnExtensionReader.java`）**
- `:237-264` `toNodes` 三段：① UserTask 读 `wf:node`；② EndEvent→3、Gateway→7；③ `syntheticNodeType`。
- `:255-280` `syntheticNodeType`：ReceiveTask / IntermediateCatchEvent / ThrowEvent→5；ServiceTask / ScriptTask / SendTask / BusinessRuleTask / CallActivity→6；ManualTask→1；其余 null。
- SubProcess 未被任何分支命中（`:247` 仅提 BoundaryEvent 挂宿主不入遍历），但内部 UserTask 因 `findFlowElementsOfType(...,true)` 递归仍会被扫到 → 读写口径不一致。
- `:319-364` `toNodeLinks` 只认 `SequenceFlow` 与流程级 `wf:foldedLink`。

**部署管线与门禁（`WfDefinitionServiceImpl.java`）**
- `:474-524` `deploy`：validateForDeploy → injectLinkConditions → neutralizeForDeploy → applyMultiInstanceIfEnabled。
- `:490-496` 部署管线是 **BpmnModel 对象级改写再序列化回 XML**，不是直接把模型交给引擎 —— 这是后续注入 ServiceTask 实现的最佳切入点。
- `:2454-2532` `validateForDeploy`：BusinessRuleTask 硬拦（`:2493-2495`，缺 Drools）；SendTask / ThrowEvent / timer 仅 warn（`:2474-2500`）；UserTask 缺 `wf:node` 硬拦（`:2505-2509`）；审批类无操作者硬拦（`:2510-2527`）。
- `:2298-2327` `downgradeUnexecutableTasks` 把 SendTask 用 `new ManualTask()` 替换；`:2336-2354` / `:2253-2257` 正式/测试同规则。
- `:2210-2296` `neutralizeForTest`：测试态把 businessRuleTask、ThrowEvent、定时 Catch/Boundary 统统降级为 manualTask 自动通过 → "测试绿、正式红"。
- `:2336-2427` `applyMultiInstanceIfEnabled` 注入 `MultiInstanceLoopCharacteristics`。
- `:2636-2681` `injectLinkConditions` 把出口条件注入为 UEL `conditionExpression`。
- `:2138-2163` `nodeTypeOf`：Service/Script/Send/BusinessRule/CallActivity 一律 6，Receive=5 → 语义层不区分真实行为。

**运行期**
- UserTask 完整链路：`WfTaskServiceImpl.java` 待办/会签/意见/抄送、字段明细权限；`ProcessServiceImpl.java:356-373` 意见写 `ACT_HI_COMMENT`；`WfBpmnExtensionReader.java:400-483` 字段权限、`:485-532` 超时。
- 全仓搜索 `JavaDelegate|ScriptEngine|groovy|flowable:class` **无实现**；`WfTaskServiceImpl.java:345-372` 自研计数会签与 `:2356-2427` 引擎多实例两套并存（开关默认关）。
- 全仓无 `signalEventReceived` / `messageEventReceived` / `calledElement` 调用。
- `WfInstanceServiceImpl.java:3120-3152` `advanceInternal`：**引擎无活动任务即置 STATUS_APPROVED 归档** —— 这是等待态元素会被误判归档的根因。

---

## 3. 最需要警惕的五个坑

1. **能画能发布 ≠ 能跑出预期**：ServiceTask / ScriptTask / CallActivity 在画布可画、save 可落表、deploy **不拦不告警**，但后端零执行实现，运行期被引擎空跑放行 —— 流程"悄悄绕过"设计者想要的自动动作，最难排查。
2. **测试绿灯、正式红**：BusinessRuleTask 测试态降级跑通，正式发布硬拦；`validate_bpmn.cjs` 也只校验 UserTask 的 `wf:node`，不覆盖这些元素。
3. **静默语义替换**：SendTask（"发送通知"）一律被降级成自动通过，只写一条日志；用户以为通知发了，其实什么都没发生。
4. **卡死类**：ReceiveTask、timer 中间/边界事件标成"等待(5)"却无唤醒代码，`validateForDeploy` 仅 warn 不拦 → 正式环境实例永久停留。
5. **定义源缺节点**：SubProcess 与 BoundaryEvent 在 `toNodes` 不合成，退回剔除/下一节点计算按 BPMN 读源时会少行（`node()` 单查却能命中），读写口径不一致。

---

## 4. 若要让这些功能真正可用：架构风险清单

### 4.1 分档判断

| 档位 | 元素 | 判断 |
|---|---|---|
| 🟢 低风险（现成机制复用） | ServiceTask、子流程、timer 事件 | 有抓手，改造可控 |
| 🟡 中风险（要开引擎能力+配套兜底） | 网关条件、ReceiveTask | 能动但要补不少配套 |
| 🔴 高风险（近乎全新开发+安全/依赖） | ScriptTask、BusinessRuleTask(DMN) | 需引入依赖、沙箱、发布门禁重构 |

### 4.2 关键架构事实

**依赖与版本**
- `springBlade/pom.xml:24` Flowable **8.1.0-SNAPSHOT（本地源码构建）**；`:26` Java 21；`:157` Spring Boot **4.1.0**。
- `blade-workflow/pom.xml:67-82` 仅 `flowable-engine` + `flowable-spring`，**无** flowable-dmn / drools / groovy / JSR-223；因 Boot 4 兼容性刻意不用 starter，全部由 `FlowableConfig` 手工装配。

**异步执行器**
- `FlowableConfig.java:127-128` **硬编码 `setAsyncExecutorActivate(false)`**，注释明说"一旦要支持定时器须开启"；`:133-134` `setAsyncHistoryEnabled(false)`；`:130-132` 说明 yml 的 `flowable.*` 整体不生效（手工装配）。
- `application.yml:45-47` 仅 `blade.flowable.database-schema-update: true`。
- 文档《Flowable8承接台账模块-去wf_表改造分析.md:962,1086-1096》：timer 改一个开关就能触发，但需 **7 项配套**（最终一致降级、常驻对账、deadletter 告警、`is_test` 测试态隔离、超时动作接 JavaDelegate、线程池、asyncHistory 保持 false）。

**现成扩展点（可复用）**
- 全局事件监听：`FlowableConfig.java:146-178` `setEventListeners` + `ObjectProvider` 汇入多个 Listener；`WfMiEmptyGuardListener.java:65-96` 是范例。注意方案 C 明确**不用 BPMN 内嵌 ExecutionListener/TaskListener**，避免改 XML 生成逻辑。
- **节点前后置动作**：`NodeActionExecutor.java:35-45,92-136,147-218`（PHASE_PRE/POST、afterSubmit/afterArchive、`triggerSubflow` 防自引用+5 级深度+parentId 回填）；执行体 `WfActionExecutor.java:38-61` 前缀分派 `http(s)://` / `sql:` / `dml:` / `field:` / `set:` / `action:`，`:104-163` `Class.forName` + `SpringUtil.getBean` 反射调 `IWfCustomAction`。触发点在 `WfTaskServiceImpl.java:549-551,665-666`、`WfInstanceServiceImpl.java:3143-3144,3167-3175`。**这是最接近 ServiceTask 的抓手**。
- 无自定义 BPMN parser / ActivityBehaviorFactory / ProcessEngineConfigurator（全仓搜索 0 结果）。

**多租户与安全**
- 部署传 tenantId（`ProcessServiceImpl.java:199-204,300-306`）；**启动未显式传 tenant**（`:53-58,87-92` 靠 processDefinitionId 隐式携带）。
- 没有任何脚本沙箱：无 ScriptEngine/GroovyClassLoader；安全仅 HTTP URL 白名单 + sql 开关（`WfActionExecutor.java:236-265,590-598`）；反射调用要求 `instanceof IWfCustomAction` 但**无权限校验**。
- 条件表达式直接 `setConditionExpression(en.getValue())`（`WfDefinitionServiceImpl.java:2672`），无白名单/校验。

**在途实例版本绑定（有利条件）**
- `WfInstanceServiceImpl.java:506-539` 用 `startProcessInstanceById(procDefId)` 把实例钉在具体部署版本；`application.yml:152-153` 明确"在途实例不受影响"。**新功能只需对新发起流程生效，无需迁移旧实例**。

### 4.3 逐元素问题

**🟢 ServiceTask（最值得做）**
- 问题不在做不到，而在**触发时机对不上**：现成 `WfActionExecutor` / `IWfCustomAction` 由业务层 `completeTask`/`advance` 在节点提交后触发，不是引擎进入 ServiceTask 时触发。
- 真 ServiceTask 要走部署注入管线给 ServiceTask 注入 `delegateExpression`，但 `FlowableConfig` 未配 Spring bean resolver，Flowable 8 的 `flowable-spring` 依赖虽在，手工装配下能否解析需实测。
- 同步执行慢 HTTP 会拖垮审批提交；走异步又受 AsyncExecutor 关闭限制。

**🟢 子流程（CallActivity / 折叠）**
- 已有业务层子流程（`NodeActionExecutor.triggerSubflow`），但与 BPMN 原生 `callActivity` 是**两套语义**（后者支持变量传递/返回、引擎级父子实例）。要支持原生需补变量绑定与结果回写，并决定两套并存时以谁为准。

**🟢 timer 事件**
- 硬障碍是 AsyncExecutor 关闭；开了还要补 7 项配套。
- 最小阻力替代：不开异步，继续用 `WfTimeoutJob` DB 轮询 + `WfActionExecutor` 执行动作 —— 但这不是"用上 BPMN timer 元素"，而是绕开它。

**🟡 网关条件**
- 已实现（`injectLinkConditions` 注入 UEL，引擎原生跑排他/并行/包容），但条件表达式无白名单校验，缺变量抛 `Unknown property used in expression`（`WfTaskServiceImpl.java:534-536` 中文包装 + `supplyMissingConditionVars` 兜底），发布期只做非阻断提示。
- `mergeType/passNum` 的按分支数/比例合并**只有配置读写、无执行逻辑**，是纯缺口。

**🟡 ReceiveTask / 消息事件**
- 标 nodeType=5"等待"但无 `messageEventReceived`/`signalEventReceived`；要支持需新建消息接收通道 + 外部唤醒入口设计（消息名→实例路由）。设计不好就是实例永久停留。

**🔴 ScriptTask**
- 零脚本引擎依赖；更麻烦的是**安全**：允许用户在画布写脚本 = 任意代码执行入口。脚本类型注册、租户上下文注入、超时/资源限制、沙箱都要从零设计。

**🔴 BusinessRuleTask / DMN**
- 发布硬拦（缺 org.kie/Drools）；要支持需引入 `flowable-dmn` + Drools，处理决策表部署/版本/测试态一致性，并同步拆掉"测试态降级"陷阱。

### 4.4 跨元素共性问题（真正的麻烦）

1. **"能画能发布 ≠ 能跑对"会继续存在**：就算补了实现，也必须先补发布期校验，否则用户画了没配实现的服务任务照样静默放行。
2. **两套触发语义打架**：SB 是"业务层 completeTask/advance 驱动 + 以 Flowable 为唯一事实源"的混合架构；BPMN 原生自动节点是**引擎驱动**。典型冲突：ReceiveTask 不产生 UserTask，而 `advanceInternal` 用"currentTasks 为空 = 归档"判定 → **等待态节点被误判为已归档**。
3. **测试态与正式态不一致**：`neutralizeForTest` 把一堆元素降级为自动通过，测试绿不代表正式能跑；补功能必须同步改测试态策略。
4. **多租户**：启动未显式传 tenant，ServiceTask 调业务代码时的执行上下文租户隔离要补。
5. **在途实例版本绑定是有利条件**：新功能只对新流程生效。

---

## 5. 建议优先级

1. **P0 防呆**（成本最低、收益最大）：设计器加元素白名单 + 发布期对未实现元素硬拦，杜绝"画了跑不通"。
2. **P1 ServiceTask + 子流程**：复用 `IWfCustomAction` 和 `triggerSubflow`，走部署注入管线，不碰引擎配置。
3. **P1 timer**：用现成 `WfTimeoutJob` 轮询替代开 AsyncExecutor，避开 7 项配套。
4. **P2 ReceiveTask/消息**：单独设计消息通道后再做。
5. **P3 ScriptTask/DMN**：最后做，且必须先定安全沙箱方案与依赖引入。

---

## 6. 证据索引

| 结论 | 代码位置 |
|---|---|
| bpmn-js 建模能力全量放开、无元素白名单 | `ant-design-pro/src/pages/FormMode/WorkflowDesign/BpmnDesigner.tsx:44-53,367-381` |
| nodeType 值域、signOrder、mergeType | `blade-workflow-api/.../entity/WfProcessNode.java:13-17,37-47` |
| 会签常量映射 | `blade-workflow/.../utils/WfNodeSettingsUtil.java:728-776` |
| toNodes 合成规则 / SubProcess 缺失 | `blade-workflow/.../resolver/WfBpmnExtensionReader.java:237-280` |
| toNodeLinks 仅认 SequenceFlow | 同上 `:319-364` |
| 部署管线（BpmnModel→XML） | `blade-workflow/.../service/impl/WfDefinitionServiceImpl.java:490-496` |
| 发布门禁硬拦/告警 | 同上 `:2454-2532` |
| SendTask 静默降级 | 同上 `:2298-2327,2336-2354` |
| 测试态降级 | 同上 `:2210-2296` |
| 多实例注入 | 同上 `:2370-2427` |
| UEL 条件注入 | 同上 `:2636-2681` |
| AdvanceInternal 空任务即归档 | `blade-workflow/.../service/impl/WfInstanceServiceImpl.java:3120-3152` |
| Flowable 8.1.0-SNAPSHOT / Boot 4.1.0 / Java 21 | `springBlade/pom.xml:24,26,157` |
| 仅 flowable-engine + flowable-spring | `blade-workflow/pom.xml:67-82` |
| AsyncExecutor 硬编码关闭 | `blade-workflow/.../config/FlowableConfig.java:127-134` |
| 全局事件监听机制 | 同上 `:146-178` |
| 节点前后置动作 + 子流程 | `blade-workflow/.../action/NodeActionExecutor.java:35-45,92-136,147-218` |
| 可执行动作前缀分派 / 反射调用 | `blade-workflow/.../action/WfActionExecutor.java:38-61,104-163,236-265,590-598` |
| URL 白名单与 sql 开关 | 同上 `:236-265,590-598` |
| 在途实例钉 procDefId 版本 | `blade-workflow/.../service/impl/WfInstanceServiceImpl.java:506-539`；`application.yml:152-153` |
| timer 依赖异步执行器与 7 项配套 | `doc/md/Flowable8承接台账模块-去wf_表改造分析.md:962,1086-1096` |

---

**文档版本**：1.0.0
**发布日期**：2026-11-17
**作者**：CodeBuddy AI
**适用范围**：springBlade 流程设计器元素开放策略、BPMN 元素启用决策与改造排期参考
