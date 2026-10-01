# 去 wf_ 表改造 —— D 系列决策结论

> 拍板日期：2026-10-01。来源：《Flowable8承接台账模块-去wf_表改造分析.md》§17.1（D1–D15）。
> 本文档为 D 系列的最终结论登记，作为后续实现与验证的唯一依据；与原文冲突处以本文为准。

## 一、决策总表

| 编号 | 事项 | 结论 | 状态 |
|---|---|---|---|
| D1 | 业务终态用原生 `BUSINESS_STATUS_` | ✅ 采纳：原生列 + 取值 `APPROVED/REJECTED/CANCELED` | 已落地 |
| D2 | `rejectToStarter` 合成待办在 MI 模式下的表达 | ✅ 复用「`wf_task(engine_task_id=null)` 业务占位」模型，不引入 MI 专属表达 | 已拍板 |
| D3 | 投影宽表保留与否 | ✅ 不保留投影表（方案 C 真正退役） | 已落地 |
| D4 | `TENANT_ID_` 取值规范 | ✅ 复用 SpringBlade 租户 ID | 已落地 |
| D5 | 各行业共用模板 vs 独立定义 | ✅ 各租户/行业**独立定义**；`KEY_` 按 `tenantId:bizKey` 命名空间 | 已拍板 |
| D6 | 是否新增 `DEF_KEY_` 桥接 | ✅ 新增 `ACT_HI_PROCINST.DEF_KEY_`（可空+默认） | 已落地 |
| D7 | 存量会签明细迁移 | ✅ 放弃 1:1 明细迁移，仅迁实例级；明细留**只读归档表**（非 `wf_` 命名，如 `wf_countersign_archive`） | 已拍板 |
| D8 | `ACT_HI_COMMENT` 租户列 | ✅ **新增 `TENANT_ID_` 列**（支撑"按租户横扫审批意见"高频场景） | 已实施（2026-10-01） |
| D9 | `WfTimeoutJob` 多副本治理 | ✅ MySQL `GET_LOCK` 选主 | 已落地 |
| D10 | 接口筛选能力盘点 | ✅ 已逐接口盘点（见 T-10 交付） | 已落地 |
| D11 | 启用 Flowable 事件日志作审计 | ✅ **暂不启用** `act_evt_log`；审计 = `ACT_HI_COMMENT` + 业务日志 | 已拍板 |
| D12 | 前端 moddle 交付方 | ✅ 后端出 schema（已定稿），前端注册（F-T1，待交付） | 部分落地 |
| D13 | 节点测试态存哪 | ✅ 维持存 `wf_*`（决策 B：表保留为遗留设计/测试存储），不迁移 | 已拍板 |
| D14 | 前端路线 A/B | ✅ **直接路线 B**（前端 bpmn-js + `wf:` moddle 直改 BPMN，统一 `saveBpmn` 保存；接受大改工期） | 已拍板 |
| D15 | 设计期版本策略 | ✅ **草稿态累积 + 显式「部署/发布」才生成引擎版本**；保存带乐观锁版本号防并发冲突（R11） | 已拍板 |

## 二、焦点决策详述

### D2 —— rejectToStarter 合成待办（MI 模式）

- **结论**：合成待办本质是业务层占位（`wf_task.engine_task_id` 为空），与引擎 task 是否存在无关；多实例仅改变「要关闭哪些引擎执行」，不改变「给发起人一条业务待办」的表达。
- **实现要点**：rejectToStarter 命中多实例节点时，先关闭该节点全部 MI 子执行（`deleteMultiInstanceExecution` / 批量 `changeState`），再生成同一条合成待办。避免双轨数据结构。
- **验证**：会签/依次 + 驳回回发起人 场景（§12.6 必测）。

### D5 —— 模板共用 vs 独立定义

- **结论**：各租户/行业**独立定义**自己的流程，不支持跨租户共享同一 BPMN 实例化。
- **理由**：操作者/字段权限/超时高度租户相关，共享模板会把这些语义绑死；BPMN 扩展已按 defId 承载租户专属配置。
- **实现要点**：`KEY_` 命名空间 `tenantId:bizKey`，避免共用 `ACT_RE_PROCDEF` 碰撞；部署时绑定 `TENant_ID_`。确有「集团统一制度模板」需求时走「模板导出 → 各租户独立导入」，不做运行时共享。

### D7 —— 存量会签明细

- **结论**：放弃 1:1 明细迁移（H1：存量「单 userTask + wf_task 多条」在 `ACT_*` 仅一条 task，无法还原），仅迁实例级；明细保留为**只读归档表**。
- **实现要点**：归档表非 `wf_` 命名（如 `wf_countersign_archive`），满足审计/查证；`wf_migration_map` 同样不删（H4，对账凭据）。

### D8 —— `ACT_HI_COMMENT` 加租户列（待实施）

- **结论**：新增 `TENANT_ID_` 列，支撑「按租户横扫审批意见」高频场景；不再依赖 JOIN 实例表继承租户。
- **实施清单**（遵循 §11.1.5 硬约束：新列必须 NULL-able + DEFAULT；DDL 首行 `USE blade`）：
  1. **DDL**：`ALTER TABLE ACT_HI_COMMENT ADD COLUMN TENANT_ID_ varchar(64) DEFAULT '' COMMENT '租户ID（D8 决策新增）';`（幂等脚本落 `doc/sql/migration/`）
  2. **回填**：`UPDATE ACT_HI_COMMENT c JOIN ACT_HI_PROCINST p ON c.PROC_INST_ID_ = p.PROC_INST_ID_ SET c.TENANT_ID_ = p.TENANT_ID_ WHERE c.TENANT_ID_ IS NULL OR c.TENANT_ID_ = '';`（幂等可重跑，回填前后计数校验）
  3. **写侧**：所有写 `ACT_HI_COMMENT` 的路径（审批评论写入口/监听器）落租户值。
  4. **读侧**：跨实例按租户扫审批意见直接过滤 `TENANT_ID_`；既有按 `PROC_INST_ID_` 的查询不受影响。如出现高频扫描再评估 `(TENANT_ID_, TIME_)` 复合索引（先 EXPLAIN 验证，参照 V1 方法）。
- **风险**：需回归 V22（无租户列表 JOIN 隔离）确认两种路径结果一致。

### D11 —— 事件日志

- **结论**：暂不启用原生 `act_evt_log`。审计口径 = `ACT_HI_COMMENT`（审批意见）+ 业务日志；流程轨迹用 `ACT_HI_ACTINST` / `ACT_HI_TASKINST`（已有）。
- **重启条件**：合规要求「引擎级不可篡改轨迹」时，再单独评估启用 + 落盘/清理策略。

### D13 —— 节点测试态

- **结论**：维持存 `wf_*`（决策 B 已明确表保留为遗留设计/测试存储）。`saveNodeTestStatus` 接口无需重设计；迁 BPMN 扩展/引擎变量反而污染生产定义。
- **重启条件**：未来若真执行方案 A（全迁 BPMN 后 DROP），测试态随之迁独立表。

### D14 —— 前端直接路线 B

- **结论**：不走后端兼容层过渡，前端用 bpmn-js + `wf:` moddle **直接编辑扩展**，统一 `saveBpmn` 保存；接受近十个面板重写的工期与回归成本（R10）。
- **影响**：
  - F-T1（moddle 交付）成为**关键路径**，须最先落地；
  - F-T2~F-T6 按 §16.4 全量改造（`NodeInfoPanel`/`LinkInfoPanel`/`NodeOperatorModal`/`NodeTimeoutModal`/`NodeOperateMenuModal`/`NodeExtraOperateModal`/`CustomOperationModal`/`ConditionBuilder`/`useLinkActions`/`nodeSettings.ts`）；
  - 条件写入**原生 `conditionExpression`**（非库字段）；
  - 雪花 ID 仍按字符串透传（F10 约定不变）。
- **后端配套**：细粒度写接口（updateNode/createLink/configOperator/saveFieldPerm 等）在路线 B 下最终废弃或转只读兼容（F6）。

### D15 —— 草稿态 + 显式部署

- **结论**：设计期修改累积在**草稿态**（不生成引擎版本），显式「部署/发布」才落新 `ACT_RE_PROCDEF` 版本。
- **实现要点**：
  - 保存链路 = 写草稿（BPMN + 扩展 + 业务主记录）；deploy 一次性部署为引擎版本；
  - 草稿/未部署定义不进引擎（与现状「草稿读 wf_* 回退」兼容，决策 B 下运行期读源行为不变）；
  - **乐观锁**：保存携带版本号，冲突提示重拉（R11 并发编辑防护）；
  - 节点测试（D13）基于草稿态进行，不影响生产。

## 三、关联行动项

| 来源 | 行动 | 优先级 |
|---|---|---|
| D8 | `ACT_HI_COMMENT` 加列 DDL + 回填 + 写侧落租户 + 回归 V22 | ✅ **已完成（2026-10-01）**：`act_add_comment_tenant.sql` 执行成功（存量 234 条全部回填，missing=0，V22 mismatch=0，`FORCE INDEX` 确认 `IDX_HI_COMMENT_TENANT` 计划有效 type=ref 无 filesort）；写侧 `WfCommentTenantWriter`（`ProcessServiceImpl.addComment` 唯一写入口接线，ObjectProvider 可选依赖兼容精简测试上下文）+ 开关 `blade.workflow.comment-tenant-write.enabled=true` + 单测 `WfCommentTenantWriterTest`（5 场景全绿）；全模块 104 用例 103 通过（1 失败为存量 `WfCompApprovalBpmnReproTest` BPMN DI `strokeWidth` 校验问题，与 D8 无关） |
| D14 | F-T1 前端 moddle 交付（关键路径）→ F-T2~F-T6 全量改造 | ✅ **F-T1 已交付（2026-10-01）**：`wfModdle.json` 与后端 `BpmnExtensionUtil` 冻结契约逐字段对账 PASS（15 类型全量，含 `detailTablePerm`/`foldedLink`/`viaGatewayKey`/P3-5 三维度；`detailFilter` 修正为定稿契约；全类型补 `superClass: ["Element"]`）；`bpmnExtension.ts` 共享读写层同步对齐，并修复 `extJson`/`extraOperations` 误经 extensionElements 查找导致恒取不到的静默丢失 bug；新增折叠连线 `getWfFoldedLinks`/`setWfFoldedLinks`。F-T2~F-T6 面板改造待做 |
| D15 | 保存链路收敛 `saveBpmn` + 草稿乐观锁（配合 F-T5/F-T6） | ✅ **已完成（2026-10-01）**：① 收敛核实——细粒度写接口（updateNode/createLink/updateLink/configOperator/saveFieldPerm/saveDetailPerm/saveDetailFilter/saveNodeTestStatus/syncOperatorToNodes）前端已**零调用**且全部标 `@deprecated 路线B`，保存主路径 = 画布自动保存（commandStack.changed → saveBpmn）；② 乐观锁——`wf_process_definition` 加 `draft_revision BIGINT NOT NULL DEFAULT 0`（dev 已执行，26 行存量全部 0），`saveBpmn(defId, bpmnXml, baseRevision)` 前置快速失败 + 文末**原子条件递增 gate**（`WHERE draft_revision = base`，不命中抛异常整体回滚，竞态由 MySQL 行锁串行化兜底；`baseRevision=NULL` 兼容旧调用仅无条件递增），前端 `BpmnDesigner.handleSave` 懒加载修订号、成功本地 +1、**冲突阻断后续自动保存**防覆盖他人修改；后端 103 测试全绿，前端 Lints 零诊断。**dev 实测通过（2026-10-01，API 全序列 10/10）**：rev0=0 保存成功→rev1=1；旧基线保存被拒（提示「定义已被他人修改（草稿修订 0 → 1），请刷新页面后重试」）；冲突后修订号不变（事务回滚验证）；最新基线保存成功；无基线兼容调用成功且递增；`detail-filter` 读路径 200 可达 |
| D5 | `KEY_` 命名空间规范落地（部署侧） | P2 |
| D7 | 会签明细归档表设计与存量归档脚本（P4 迁移启动时） | P3 |
| F-T6（部分） | **字段权限三维度 + 明细筛选接线（2026-10-01）**：① `NodeDetail` 保存 `wf:fieldPerm` 补写 P3-5 三维度（visible/editable/required，权威值此前被压缩成 perm 派生列丢失），读取三维度齐备时优先于 perm；② `FormContentDesignModal` 明细筛选双写 `wf:detailFilter`（modeType 1=显示 2=打印，与后端 `WfPermController` 口径一致），extJson 侧留 `detailFilterSynced` 迁移标记，旧数据回退无感升级；③ `detailTablePerm` 共享层已透传（前端暂无编辑 UI，后端回填数据可无损往返），UI 待表单设计页批次。**UI 读回闭环已于 2026-10-01 Playwright 实测打通**：保存→BPMN 落 `wf:detailFilter`（dtIndex=1, fieldName=amount, compareValue=999）→reload 重拉→重开弹窗读回无误，且重开不再破坏服务端数据（此前因下方 moddle 根因 bug 表现为「读回为空 + 误覆盖」，已随该 bug 修复一并解决） | 已完成（①②，③ UI 待做） |
| **BUG-根因（2026-10-01，P0）** | **`wfModdle.json` 类型名全小写导致 `importXML` 丢弃全部 `wf:*` 扩展**：bpmn-moddle 的解析器按 XML 标签 `wf:node` 推导类型时会对首字母大写（`wf:Node`），而 `wfModdle.json` 把 15 个类型名全写成小写（`node`/`link`/`operator`/`detailFilter`…）并配 `tagAlias:"lowerCase"`，致使 `importXML` 报 `unknown type <wf:Node>`、把整段 `wf:*` 扩展静默丢弃——表现为「画布重加载后节点配置（操作者/字段权限/明细筛选/超时/自定义操作…）全部读空」，且 `FormContentDesignModal` 打开时把空读数写回 → 破坏服务端 BPMN（与路线 B「BPMN 为唯一真相源」直接冲突）。**修复**：① 15 个类型名首字母大写（`Node`/`Link`/`ProcessMeta`/`FoldedLink`/`Operator`/`FieldPerm`/`DetailPerm`/`DetailTablePerm`/`DetailFilter`/`Timeout`/`CustomAction`/`Operation`/`Right`/`ExtJson`/`ExtraOperations`），同步把属性 `type` 引用一并大写；② `moddle.create('wf:xxx')` 调用同步改大写标签（`moddle.create` 不解析 tagAlias，须用类型名）；③ `bpmnExtension.isWf` 改为 `$type` 大小写不敏感。写路径（`moddle.create`）与读路径（`importXML`）现已统一，`detailFilter` 读写闭环实测通过。**教训**：bpmn-moddle 自定义扩展类型名必须首字母大写（对齐 camunda 约定），否则 import 静默丢扩展、且会在保存时反向破坏数据 | 已修复 |
| F-T8 | **版本对比扩展差异（2026-10-01）**：后端 `diff()` 数据源本已随 `definition-from-bpmn` 读 BPMN 扩展，但比较范围窄；本轮扩展——节点变更判定 + 操作者构成摘要（`opType#objId@groupNo` 升序串，经 `nodeOperators` 读取）、extJson（键序/空白归一化后比较）、mergeType/passNum/allowReject/allowForward/autoApprove；出口 + viaGatewayKey/extraOperations；`VersionDiffVO` 增 `sourceOperators/targetOperators`、`sourceIsMustPass/targetIsMustPass`、`sourceViaGatewayKey/targetViaGatewayKey`；前端 `VersionDiffModal` 展示操作者数量变化与必经/折叠网关标记。后端 103 测试全绿 | 已完成 |
