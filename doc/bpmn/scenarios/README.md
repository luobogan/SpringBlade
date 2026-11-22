# flowtable 流程场景 BPMN 样例库

本目录覆盖 SpringBlade 工作流平台（flowtable）的 **21 种流程情况**，每种一个 `.bpmn20.xml`，可用设计器导入、部署到引擎运行。由 `_generate_all.py` 统一生成（改约束改脚本后重跑即可），全部通过 `validate_bpmn.py` 校验。

> 设计依据：`.codebuddy/skills/flowable-workflow-builder/references/engine-capabilities.md` 与 `bpmn-schema.md`。
> 读源前提：本仓库 `application.yml` 已开 `operator-from-bpmn` / `definition-from-bpmn` 且关 `wf_*` 表兜底，即 **BPMN 的 `wf:` 扩展就是权威读源**。导入后必须用设计器「发布 + 自动测试走查」确认节点未被跳过。

## A. 基础主链与节点类型

| 文件 | 覆盖情况 | 部署前提 |
|---|---|---|
| `01_basic_approval` | 开始→创建(0,opType17)→审批(1,opType2)→归档(3) | 默认配置 |
| `02_escalation_chain` | 逐级向上：部门上级(18)→分管领导(角色)→总经理(角色)，多 userTask 串联 | 默认配置 |
| `10_operator_types` | 操作者类型混合：人员3/角色2/岗位58/部门1/字段人员5/创建人17/本部门19 | 默认配置 |
| `11_matrix_operator` | 矩阵操作者 opType=99 + matrixJson | 默认配置 |
| `12_dept_leader` | 创建人上级 opType=18（依赖 starter.manager_id，配角色兜底） | 默认配置 |

## B. 签批方式 signOrder

| 文件 | 覆盖情况 | 部署前提 |
|---|---|---|
| `03_countersign_all` | 会签 signOrder=1（全部同意才推进） | 默认配置 |
| `04_any_one_approve` | 或签 signOrder=0（任一同意即推进） | 默认配置 |
| `05_sequential_approval` | 依次审批 signOrder=2（按 wf_task.id ASC） | 默认配置 |
| `06_cc_circulate` | 抄送 signOrder=3/4 + autoApprove | 默认配置 |

## C. 网关与条件路由

| 文件 | 覆盖情况 | 部署前提 |
|---|---|---|
| `07_exclusive_gateway_amount` | 排他网关金额分级 + default 流不带条件 + 条件只用 `&&` + 各节点退回白名单（按条件回退） | 默认配置 |
| `08_parallel_gateway` | 并行网关 fork/join | 默认配置 |
| `09_inclusive_gateway` | 包容网关条件分流 + 汇聚 | 默认配置（校验脚本对 join 网关的 2 条 warning 是误报：汇聚本就不带条件） |

## D. 流转配置

| 文件 | 覆盖情况 | 部署前提 |
|---|---|---|
| `13_reject_whitelist` | 退回白名单 wf:reject，退发起人/退指定节点（目标必须是 0/1/2 节点） | 默认配置 |
| `14_forward_transfer` | 转发/转办/委托（菜单在 ext_json，需设计器配） | 默认配置 |
| `15_timeout_rules` | 超时四种动作：remind/autoApprove/forward/assign | 默认配置 |
| `16_field_permissions` | 字段权限 0隐藏/1只读/2可编辑/3必填，主表+明细 dt1 | 默认配置 |

## E. 自动节点 / 事件（需开 compat 开关，否则 S0 发布期硬拦或运行期卡住）

| 文件 | 覆盖情况 | 部署前提 |
|---|---|---|
| `17_service_task_e1` | ServiceTask 自动动作（delegateExpression → WfActionExecutor） | 开 `action-gate` + `service-task-exec`（填灰度 defId）+ E7 `engine-auto-todo-bridge`，翻转后**重新发布** |
| `18_script_task_s6` | ScriptTask 沙箱 Groovy（`flowable:resultVariable`） | 开 `s6-script-task-enabled`，重新发布 |
| `19_receive_task_s4` | ReceiveTask 等待外部触发唤醒 | 开 `s4-wakeup-enabled`，重新发布；外部入口还需 HMAC + 网关白名单 |
| `20_timer_boundary_s3` | 边界定时器超时分支（PT5M） | 开 `s3-timer-enabled`（与 AsyncExecutor 互斥），timer 仅 warn 不硬拦 |
| `21_business_subflow` | 业务层子流程（settings.subflow.trigger=afterArchive，非 callActivity） | 默认配置；BPMN callActivity/SubProcess 被 S0 硬拦，业务层为准 |

## 红线速记

- 创建节点必须是 `<userTask>` + `wf:node nodeType="0"`，不是 `<startEvent>`。
- `wf:operator/@opType` 是**数字码**；30/97/98 声明了但不可解析；99 矩阵已支持。
- `wf:operator` / `wf:fieldPerm` / `wf:timeout` / `wf:reject` 必须嵌在 `<wf:node>` 内部。
- 排他/包容网关 `default` 流**不得**带 `conditionExpression`；条件表达式只用 `&&`，`<` `>` 转义为 `&lt;` `&gt;`。
- 逐级向上 = 多个 userTask 串联，**不要** `bhxj="3"`（那是整条主管链并行）。
- 退回目标只能是 nodeType ∈ {0,1,2}；网关/归档/自动节点不能作目标。
- 空办理人结果在 nodeType=1/2（MI）会被静默跳过；用「自动测试走查」看 `nodeTimes` 是否含该节点来发现。

## 重新生成 / 校验

```powershell
cd d:/workproject/springbladeandreact/springBlade/doc/bpmn/scenarios
python _generate_all.py
$v='d:/workproject/springbladeandreact/.codebuddy/skills/flowable-workflow-builder/scripts/validate_bpmn.py'
Get-ChildItem -Filter *.bpmn20.xml | ForEach-Object { python $v $_.FullName | Select-String 'RESULT:' }
```
