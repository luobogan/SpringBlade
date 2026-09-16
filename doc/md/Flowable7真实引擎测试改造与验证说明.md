# Flowable7 真实引擎测试 · 改造与验证说明

> 配套分析报告：`Flowable7真实引擎假数据流程测试落地分析报告.md`
> 改造范围：严格限定在报告定义的"真实引擎 + 假数据流程测试"落地范围，未偏离。
> 执行结论：**4 个用例全部通过（BUILD SUCCESS），真实引擎行为与报告预期一致。**

---

## 1. 改造依据

本报告第 2~7 节提出：用真实 Flowable7 引擎 + 假数据跑完流程，验证每个细节。明确指出两个**封装缺口**：

- 报告 §3.4 / §4.1：`IProcessService` 缺少 `HistoryService` 封装（覆盖率采集）。
- 报告 §4.4 / §7：`IProcessService` 缺少 `ManagementService` 封装（定时/异步作业覆盖）。
- 报告 §3.4：测试部署需独立 key + 跑完即卸载（零污染清理）。
- 报告 §7：条件求值器不一致风险——真实引擎 UEL（支持 `||`）与 `WfConditionUtil`（仅 `&&`）存在差异，配置走查会"假阳性"。

---

## 2. 改造内容（代码）

### 2.1 `IProcessService`（引擎适配层接口）
路径：`blade-service/blade-workflow/.../service/IProcessService.java`

新增 4 个方法（对应报告 §3.4 / §4.1 / §7 的封装缺口）：

| 方法 | 用途 | 报告对应 |
|---|---|---|
| `List<HistoricActivityInstance> historicActivities(instId)` | 采集实例经过的所有节点/流转 | §4.1 覆盖率 |
| `HistoricProcessInstance historicProcess(instId)` | 判断是否已归档（endTime） | §3 运行结果 |
| `void deleteDeployment(deploymentId)` | 测试部署级联卸载（零污染） | §3.4 清理 |
| `long pendingJobCount()` | 统计引擎待执行作业（普通 + 定时） | §4.4 / §7 作业覆盖 |

### 2.2 `ProcessServiceImpl`（实现）
路径：`blade-service/blade-workflow/.../service/impl/ProcessServiceImpl.java`

- 注入 `HistoryService` / `ManagementService`（二者在 `FlowableConfig` 中本已声明为 Bean，仅此前未接入适配层）。
- 实现上述 4 个方法；覆盖率采集按 `startTime` 升序返回历史活动，排他网关"走了哪条分支"由下游被激活节点反推（无需 `history=full` 记录 sequenceFlow）。

> 未改动任何生产行为：`FlowableConfig` 的 `databaseSchemaUpdate`、异步执行器开关、自动部署路径均保持不变。

---

## 3. 假数据注入与驱动逻辑

测试类：`blade-service/blade-workflow/src/test/java/.../WfEngineSmokeTest.java`
复用现有隔离式测试底座 `FlowableTestConfig`（内存 H2，无 Nacos/MySQL/Redis）。

### 3.1 假数据注入点（对应报告 §3）
- **表单变量（条件驱动）**：内联一份含排他网关的 BPMN，条件表达式 `${amount > 1000 || type == 'A'}`；通过 `startInstance(procKey, bizKey, vars)` 把 `{amount, type, approver}` 作为流程变量注入真实引擎——这正是出口条件 UEL 求值所读取的变量（报告 §2.2 指出的 `StartProcessDTO.fieldValues` → 引擎变量链路）。
- **操作者模拟**：用户任务 `assignee="${approver}"`，每次运行喂 `"approver":"tester"`。
- **副作用抑制**：本测试聚焦"引擎内核 + 条件注入 + 覆盖率"层，不进入语义层（`WfActionExecutor` 等），故无需关闭 `action.http-enabled`；报告要求的副作用抑制开关（`WfActionProperties` 已具备）将在语义层集成时生效。

### 3.2 驱动逻辑（对应报告 §3.4 测试骨架）
```
每个用例 @BeforeEach：deployProcess(独立 procKey) → 断言定义已部署
runToEnd(vars)：
    startInstance → currentTasks（网关已即时求值，仅 1 个用户任务）
    → completeTask → historicProcess.endTime 非空（已归档）
    → historicActivities 采集节点集合
每个用例 @AfterEach：deleteDeployment（断言定义已卸载，零污染）
```

---

## 4. 测试设计与执行结果

| 用例 | 假数据 | 断言（报告预期） | 结果 |
|---|---|---|---|
| `shouldTakeConditionalBranchWhenConditionTrue` | amount=2000, type=B | 走 `highApproval`，不经过 `normalApproval`；无残留作业 | ✅ |
| `shouldTakeDefaultBranchWhenConditionFalse` | amount=50, type=B | 走 `normalApproval`（默认支），不经过 `highApproval` | ✅ |
| `shouldHonorOrOperatorUnlikeConfigWalk` | amount=50, type=A | `50>1000(假) || 'A'=='A'(真)` → UEL 判真 → 走 `highApproval`（验证报告 §7 风险点） | ✅ |
| `coverageShouldCoverAllNodesAcrossRuns` | 三份数据并集 | 全部节点覆盖：startEvent / amountGateway / highApproval / normalApproval / endEvent | ✅ |

**执行命令**（CLI `mvn`，独立 Maven 仓库）：
```
cd SpringBlade
mvn -pl blade-service/blade-workflow test -Dtest=WfEngineSmokeTest -DfailIfNoTests=false "-Dmaven.compiler.proc=full"
```
**输出**：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` → `BUILD SUCCESS`。

---

## 5. 与报告章节对应关系

| 报告章节 | 本章落实 |
|---|---|
| §2.1 引擎服务分层（History/Management 缺口） | §2.1 已封装 4 个方法 |
| §2.2 双层设计 / 条件变量链路 | §3.1 通过 `startInstance` 变量驱动 UEL |
| §3 真引擎 + 假数据三注入点 | §3.1 表单变量 + 操作者；副作用抑制开关就绪 |
| §3.4 测试骨架 + 独立 key + 清理 | §3.2 + `deleteDeployment` 零污染 |
| §4.1 节点/连线覆盖率 | `historicActivities` + `coverageShouldCoverAllNodesAcrossRuns` |
| §4.2 网关分支枚举（多组数据） | 三份数据枚举两条分支 |
| §4.4 / §7 作业覆盖 | `pendingJobCount()` 断言无残留作业 |
| §7 条件求值器不一致风险 | `shouldHonorOrOperatorUnlikeConfigWalk` 实证 |

---

## 6. 验证结论

1. **真实引擎 + 假数据**可完整驱动条件分支流程从发起走到归档，与报告 §3 预期一致。
2. **覆盖率采集机制**（HistoryService）有效：能识别实际走过的节点与未走的分支，支撑报告 §4.1 的"死节点 / 未验证分支"识别。
3. **报告 §7 风险被实证**：真实引擎正确解析 `||`，而 `WfConditionUtil` 无法解析 `||`（仅 `split("&&")`）。若仅靠配置走查，该分支会被误判为"默认支"——**证明真引擎测试不可替代**。
4. 测试部署走独立 key 并 `deleteDeployment` 卸载，确认零污染（报告 §3.4）。

---

## 7. 遗留与下一步（不在本次范围，报告 §3.4 定义）

本次聚焦"引擎内核 + 条件注入 + 覆盖率"层（H2 即可运行）。报告 §3.4 测试骨架中**语义层**部分（`WfInstanceServiceImpl.start` + `WfTaskServiceImpl.autoApprove` 循环驱动会签/或签/依次、前/后附加操作、子流程、指定流转）需 `wf_*` 业务表（MySQL）。落地方式：

- 在 `FlowableTestConfig` 增加 `wf_*` 表 H2 schema（或在测试 Profile 接 MySQL），
- 串起 `WfInstanceServiceImpl.start(defId, formData)` → 循环 `WfTaskServiceImpl.autoApprove` → 覆盖率采集，
- 测试 Profile 关闭 `blade.workflow.action.http-enabled` / `sql-enabled` 抑制副作用（开关已具备）。

另：`pendingJobCount()` 已就位，定时器边界事件（节点类型 5）等高级场景的覆盖将借其驱动 `ManagementService` 执行作业。

---

*生成日期：2026-09-17*
