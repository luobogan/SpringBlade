# Flowable 7 工作流引擎精讲：从节点语义到企业级流程管控

> 本文围绕一个核心命题展开：如何正确理解 BPMN 节点的本质，并在此基础上设计可测试、可隔离、可长期维护的企业级审批流。适合正在使用或选型 Flowable 7 的架构师与后端开发者。

---

## 目录

1. 节点的本质：为什么开始/结束节点不能乱用
2. 开始节点：填表反模式与正确建模
3. 结束节点：被误解的墓碑标记
4. Flowable 7 带来的真实变化
5. 测试流程 vs 正式流程：分层设计方案
6. 测试流转正：状态机 + 重新部署架构
7. category 与 tenantId：两种标记的本质区别
8. 定时/延时任务如何测试
9. 单库混布：让正式人员看不到测试数据
10. 人员基础数据一致性：关闭 IDM 后的同步方案
11. 落地清单与决策速查表

---

## 一、节点的本质：为什么开始/结束节点"不能乱用"

BPMN 2.0 规范中，**开始事件（Start Event）** 与 **结束事件（End Event）** 都是"事件（Event）"，而不是"任务（Task）"。这是理解一切设计取舍的根基。

| 概念 | 本质 | 关键属性 | 能否承载业务/表单 |
| :--- | :--- | :--- | :--- |
| Start Event | 流程实例创建的触发信号 | 无 assignee、无 UI | 不应承载 |
| User Task | 需要人处理的工作单元 | 有 assignee、formKey、dueDate | 业务/表单主场 |
| Service Task | 自动执行的系统动作 | 有 delegateExpression | 业务逻辑主场 |
| End Event | 流程/分支终止的信号 | 无 assignee、无 UI | 不应承载 |

**一句话纲领：开始节点是"流程出生的产房标记"，结束节点是"流程死亡的墓碑标记"。产房和墓碑本身都不负责养育和治疗，真正的业务逻辑永远在中间的 Task 和 ServiceTask 里。**

把事件当作纯粹的**生命周期边界符号**，把所有业务动作显式建模为独立节点，才是 Flowable 正确且可持续的使用方式。

---

## 二、开始节点：填表反模式与正确建模

### 2.1 一个常见的"看似合理"需求

很多团队第一反应是：把"填写申请单"直接绑在开始节点上，审批驳回后再退回开始节点重新填。理由通常是——"少画一个节点，界面更简洁，发起效率更高"。

这个需求在技术上**可以实现**，但属于**反模式（Anti-Pattern）**，会付出十倍代价。

### 2.2 强行用开始节点填表的六大难点

| 难点 | 具体表现 | 后果 |
| :--- | :--- | :--- |
| 开始事件不可被分配 | StartEvent 没有 assignee/candidateUser | 退回后系统不知道"谁"该重新填写 |
| 表单数据丢失/覆盖 | 退回时原表单变量仍在上下文，重填直接覆盖 | 无法保留修改痕迹，UI 不回显旧值 |
| 历史记录混乱 | ACT_HI_ACTINST 出现多条 StartEvent，END_TIME 异常 | 审计追溯困难，统计报表出错 |
| 监听器重复触发 | executionListener(start) 每次退回重入都执行 | 初始化逻辑（发通知、建关联数据）重复执行产生脏数据 |
| 并行/子流程兼容差 | 退回到 StartEvent 可能导致令牌（Token）状态不一致 | 流程卡死或意外终止 |
| 前端对接复杂 | 开始表单与任务表单是两套 API | 前端需维护"首次发起 vs 退回重填"两套逻辑 |

### 2.3 用户体验的真相：省节点反而体验更差

最致命的不是画图麻烦，而是——**如果把申请单绑在开始节点上，审批人驳回时，引擎无法自动把任务分配回申请人**，因为开始节点没有"办理人"属性。结果是用户被驳回后，在"我的待办"里**根本找不到**被退回的单子，只能去"已发起"里翻，甚至收不到通知。**这才是真正的体验灾难。**

### 2.4 正确建模：用第一个 UserTask 承载申请单

```
[StartEvent] -> [填写申请单(UserTask)] -> [审批] -> ...
                      ^                         |
                      |_______ 驳回 ____________|
```

好处：

- **天然支持退回**：UserTask 有完整 assignee、formKey、dueDate，驳回目标明确；
- **表单回显简单**：taskService.getVariables(taskId) 即可取回上次填写数据回填；
- **历史清晰**：每次修改都是独立 Task 记录，带完整时间戳和处理人（满足合规审计）；
- **监听器可控**：用 taskListener(assignment) 精确控制退回行为，避免重复初始化；
- **API 统一**：首次提交与退回重填都走 taskService.complete()，前端无需分支判断。

### 2.5 折中方案（如确有特殊约束）

| 场景 | 方案 | 说明 |
| :--- | :--- | :--- |
| 可接受新实例 ID + 追求架构干净 | 终止 + 重新启动 | 驳回时 deleteProcessInstance()，以相同 businessKey 重启，业务表记录"版本链" |
| 需保留原实例 ID + 合规允许 | MessageCatch 模拟 | StartEvent 后紧跟消息捕获事件作"重填入口"，驳回时发消息绕过 StartEvent |
| 仅 Demo / 内部玩具项目 | 直接退回 StartEvent | 做好踩坑准备 |

> **沟通要点**：这个需求多源于对"开始节点"概念的误用。多数情况下，向业务方解释"多画一个圆圈换来退回重填、待办提醒、草稿保存、多渠道发起全部零成本实现"，是最高效的说服方式。画图时老老实实 [Start] -> [填写申请单]，给用户看时前端隐藏起始节点即可——**规范是给引擎和开发者看的契约，简洁是给用户的体验，二者并不矛盾。**

---

## 三、结束节点：被误解的"墓碑标记"

结束事件（实心粗线圆圈）仅代表"流程实例或当前分支执行到此终止"这一逻辑信号，本身不含任何业务动作、页面跳转或数据处理。

### 3.1 结束事件的三种形态

| 类型 | 图标 | 本质作用 | 常见误区 |
| :--- | :--- | :--- | :--- |
| None End Event | 空心粗圈 | 仅标记流程/分支正常结束 | 以为流程结束用户就会收到通知 |
| Terminate End Event | 实心粗圈+内环 | 强制终止整个流程实例（含所有并行分支） | 误用于普通审批通过，导致其他并行任务被意外杀掉 |
| Cancel End Event | 粗圈+x | 仅在事务子流程中触发补偿/回滚 | 在普通流程中使用会直接报错 |

> **关键区别**：None End Event 只结束**当前令牌**。若并行网关产生的多分支中只有一个分支到达 None End，**其他分支仍在运行**，流程实例不会真正结束。只有所有分支都到达 None End，或任意分支到达 Terminate End，实例才彻底终结。

### 3.2 结束节点的最佳实践

- **永远不要在结束节点上寄托业务期望**：把它当作纯语法符号；
- **在结束节点前加一个 ServiceTask**（"流程收尾处理"）：集中处理通知、状态更新、数据快照，比全局监听器更直观易调试；
- **区分分支结束与流程结束**：并行汇聚后只用一个 None End；需提前终止全部并行分支（如"一票否决"）才用 Terminate End；
- **给用户明确的终态反馈**：前端检测到流程实例结束后，展示独立"办理完成"页，而不是停留在空白/加载中。

### 3.3 开始与结束节点的对称性对比

| 维度 | 开始节点 | 结束节点 |
| :--- | :--- | :--- |
| BPMN 语义 | 流程创建触发点 | 流程/分支终止信号 |
| 是否有 UI | 无（需额外绑定表单） | 无（需额外实现反馈页） |
| 是否有处理人 | 无 assignee | 无 assignee |
| 能否被退回/重入 | 反模式 | 不可达（已结束） |
| 业务逻辑承载位置 | 应放在首个 UserTask | 应放在最后 ServiceTask 或全局监听器 |
| 用户体验痛点 | 退回后找不到待办 | 流程走完了用户不知道结果 |

---

## 四、Flowable 7 带来的真实变化

**Flowable 7 并没有把 StartEvent 变成 Task，但它让"用 Task 代替 StartEvent 填表"变得足够简单，以至于你不再纠结"少画一个图标"。**

- **原生表单能力增强**：Form Engine 支持更丰富字段类型、校验规则和动态表达式，把申请单放在第一个 UserTask 上时，原生表单往往就够用；
- **REST/OpenAPI 更统一**：Task 与 ProcessInstance 操作更一致，前端处理"首次提交"和"退回重填"都用相似的 completeTask 模式；
- **变量与历史查询优化**：historyService 查询更快，变量序列化更灵活，表单回显更可靠；
- **官方工具链封装**：Flowable Work / Design 已内置"发起即填写"体验封装，用户看到的就是一个申请单页面，感知不到背后的 StartEvent，而后台仍遵循正确建模规范。

> 最佳实践从 v5 到 v7 始终没变：**永远用第一个 UserTask 来承载申请单。**

---

## 五、测试流程 vs 正式流程：分层设计方案

核心原则：**代码/定义复用，数据/环境隔离。** 千万不要为测试复制一份独立的 BPMN 文件（如 leave-request-test.bpmn20.xml），那会导致维护灾难。

### 5.1 方案一：同一流程定义 + 运行时变量路由（推荐）

测试和正式使用**同一个 BPMN 文件**，通过流程变量控制行为差异（审批人、通知方式、超时时间、外部接口地址）。

```xml
<!-- 动态指定处理人 -->
<userTask id="approve" name="部门审批"
          flowable:assignee="${testMode == true ? 'test_user_01' : realApprover}" />

<!-- 切换通知服务实现 -->
<serviceTask id="notify" name="发送通知"
             flowable:delegateExpression="${testMode == true ? @mockNotifyService : @realNotifyService}" />

<!-- 控制超时 -->
<timerEventDefinition>
    <timeDuration>${testMode == true ? 'PT5M' : 'P3D'}</timeDuration>
</timerEventDefinition>
```

```java
runtimeService.startProcessInstanceByKey("leaveRequest",
    Map.of("testMode", true, "realApprover", "zhangsan"));
```

优点：零维护成本，测试的就是真正要上线的流程；Flowable 7 表达式引擎对 SpEL/MVEL 支持完善，性能无损。
注意：testMode 务必有默认值（通常 false），防止正式环境遗漏传参走测试逻辑。

### 5.2 方案二：Spring Profile + Bean 别名隔离

当测试与正式差异是整个服务实现类时，用 Spring Profile 配合 Delegate Expression：

```java
@Service("notifyService") @Profile("prod")
public class RealNotifyService implements JavaDelegate { ... }

@Service("notifyService") @Profile({"dev","test"})
public class MockNotifyService implements JavaDelegate { ... }
```

BPMN 中始终写 ${@notifyService}，容器按 Profile 自动注入。**流程图完全无需感知环境差异。**

### 5.3 基础设施层隔离（无论用哪种逻辑方案都必须做）

| 维度 | 测试环境 | 正式环境 |
| :--- | :--- | :--- |
| 数据库 | 独立 Schema / 实例 | 生产库，严格权限 |
| 历史级别 | FULL / AUDIT（便于调试） | ACTIVITY / NONE（保障性能） |
| 异步执行器 | 可关闭/单线程 | 多线程集群 |
| 流程部署 | 本地/CI 自动部署 | K8s ConfigMap / 制品库版本化 |

### 5.4 Flowable 7 专属测试能力

- @FlowableTest：自动初始化内存引擎，测试完自动清理；
- ProcessEngineRule / JUnit5 Extension：断言流程状态、变量、历史；
- Mock 框架集成：直接 Mock JavaDelegate、TaskListener，验证调用次数与参数；
- CMMN/DMN 联合测试。

### 5.5 绝对要避免的反模式

复制 BPMN 加 -test 后缀、代码里硬编码 if(env.equals("test"))、测试与正式共用数据库、用正式账号做测试、测试环境开启全量异步+集群。

---

## 六、测试流转正：状态机 + 重新部署架构

"在沙箱里测，测完一键转正"是企业级流程管控中心的标配诉求，**完全可以实现**。但需明确：**Flowable 中流程定义一旦部署就不可变**，因此"测试转正式"本质是**将测试版定义以正式身份重新部署一次**。

### 6.1 业务管控表设计

```sql
CREATE TABLE biz_process_manage (
    id VARCHAR(64) PRIMARY KEY,
    process_key VARCHAR(64) NOT NULL,    -- 流程标识（如 leave_request）
    process_name VARCHAR(128),
    current_status VARCHAR(20),          -- DRAFT / TESTING / PUBLISHED
    test_proc_def_id VARCHAR(64),
    prod_proc_def_id VARCHAR(64),
    bpmn_xml LONGTEXT,                   -- BPMN XML 源码
    update_time DATETIME
);
```

### 6.2 部署测试版（打标记）

```java
Deployment deployment = repositoryService.createDeployment()
    .name("请假流程-测试版")
    .addString("leave_request.bpmn20.xml", bpmnXml)
    .category("TEST")
    .tenantId("test_env")
    .deploy();

ProcessDefinition testProcDef = repositoryService.createProcessDefinitionQuery()
    .deploymentId(deployment.getId()).singleResult();
```

### 6.3 一键发布为正式（重新部署 + 挂起旧测试版）

```java
Deployment prodDeployment = repositoryService.createDeployment()
    .name("请假流程-正式版")
    .addString("leave_request.bpmn20.xml", bpmnXml)
    .category("PROD")
    .tenantId("prod_env")
    .deploy();

ProcessDefinition prodProcDef = repositoryService.createProcessDefinitionQuery()
    .deploymentId(prodDeployment.getId()).singleResult();

// 挂起旧测试版，防止继续发起
repositoryService.suspendProcessDefinitionById(testProcDefId, true, null);
```

### 6.4 三大优势

1. **版本快照与秒级回滚**：转正自动 version=N，上线出 Bug 可一键切回上一 PUBLISHED 版本；
2. **测试与正式数据天然隔离**：通过 category/tenantId 过滤，正式用户绝对看不到测试流程；
3. **解决"带病上线"合规风险**：可在管控表加"测试覆盖率/审批卡点"，仅当测试用例全通过才点亮发布按钮。

### 6.5 必须注意的坑

1. **测试实例不能转正**：转的是流程定义（规则），不是流程实例（数据）；
2. **表单与外部接口要同步切环境**：formKey、接口地址需结合 Profile/配置中心一起切换；
3. **流程定义 ID 会变**：业务表关联**一定要用 process_key 作主键**，而非 Flowable 自动生成的 ID。

---

## 七、category 与 tenantId：两种标记的本质

这是最容易混淆的一点。**二者解决的是完全不同层面的问题，应组合使用。**

| 用途 | 正确选择 | 为什么 |
| :--- | :--- | :--- |
| 区分流程定义（列表展示、状态） | category（TEST/PROD） | category 适合做元数据分类 |
| 隔离运行时数据（待办/实例/历史） | tenantId（TEST_TENANT/PROD_TENANT） | tenantId 是底层**行级隔离**，所有关联表自动继承，查询性能最高、最安全 |
| 防止定时任务串台 | 隔离 Job Executor 或手动触发测试 Job | 执行器默认不认 tenantId |

> **结论**：用 category 管理流程定义的状态（草稿/测试/正式），用 tenantId 隔离运行时的数据。两者结合，才能在单库环境下实现既灵活又安全的流程管控中心。

---

## 八、定时/延时任务如何测试

**关键认知**：category/tenantId 标记只解决"数据归属与展示隔离"，**不会**改变定时器触发时机。只要时间一到，Job Executor 就无差别触发。测试定时任务必须配合"时间控制机制"。

### 8.1 策略一：表达式动态控制时间刻度（最实用）

```xml
<timerEventDefinition>
    <timeDuration>${testMode == true ? 'PT3M' : 'P3D'}</timeDuration>
</timerEventDefinition>
<timerEventDefinition>
    <timeCycle>${testMode == true ? '0 0/5 * * * ?' : '0 0 8 ? * MON'}</timeCycle>
</timerEventDefinition>
```

测试时传 testMode=true，喝杯咖啡就测完；转正后恢复真实时间。

### 8.2 策略二：时间旅行 API（自动化测试神器）

```java
Date realNow = new Date();
ProcessInstance pi = runtimeService.startProcessInstanceByKey("leave", Map.of("testMode", true));

Calendar cal = Calendar.getInstance();
cal.setTime(realNow);
cal.add(Calendar.DAY_OF_MONTH, 4);
processEngineConfiguration.getClock().setCurrentTime(cal.getTime()); // 引擎时钟拨快4天

managementService.executeJob(managementService.createJobQuery()
    .processInstanceId(pi.getId()).singleResult().getId());

// 断言流程已过定时器节点
processEngineConfiguration.getClock().reset(); // 务必拨回，否则影响其他流程
```

### 8.3 策略三：API 手动触发 Timer Job（调试级）

```java
TimerJob timerJob = managementService.createTimerJobQuery()
    .processInstanceId(testProcessInstanceId).singleResult();
managementService.setTimerJobDueDate(timerJob.getId(), new Date());
```

### 8.4 架构强制要求

- **Job Executor 必须物理隔离**：禁止测试与正式共用同库且同时开启轮询，否则会互相抢任务，引发灾难；
- **定时开始事件（Timer Start Event）特殊处理**：流程处于 TESTING 时**必须挂起该定义**或禁用对应 category 的定时启动，否则引擎会不断自动创建测试实例产生脏数据。

| 测试场景 | 推荐方案 | 依赖标记吗 |
| :--- | :--- | :--- |
| 验证延时逻辑对不对 | 表达式 testMode ? 'PT5M':'P3D' | 否，依赖流程变量 |
| 自动化测长周期定时 | Clock API 时间旅行 | 否，依赖引擎 API |
| 开发时手动跳过等待 | ManagementService 改 Job 到期时间 | 否，依赖引擎 API |
| 防测试定时污染正式 | 独立库/独立 Job Executor | 是，环境隔离是基础 |

---

## 九、单库混布：让正式人员看不到测试数据

**如果同库不加干预，正式人员绝对会看到测试数据。** 仅靠流程定义 category 不够（它主要区分定义，对运行时数据过滤不够底层）。**最原生方案是启用多租户 tenantId，把"测试/正式"伪装成两个租户。**

### 9.1 部署阶段打租户标记

```java
repositoryService.createDeployment()
    .name("请假流程-测试版").addString("leave.bpmn20.xml", bpmnXml)
    .tenantId("TEST_TENANT").deploy();

repositoryService.createDeployment()
    .name("请假流程-正式版").addString("leave.bpmn20.xml", bpmnXml)
    .tenantId("PROD_TENANT").deploy();
```

### 9.2 运行阶段数据自动继承隔离标记

引擎会自动把 TEST_TENANT 写入所有相关底层表：运行表 ACT_RU_EXECUTION / ACT_RU_TASK / ACT_RU_VARIABLE，历史表 ACT_HI_PROCINST / ACT_HI_TASKINST / ACT_HI_ACTINST。

### 9.3 查询阶段正式人员自动"隐身"

```java
List<Task> myTasks = taskService.createTaskQuery()
    .taskAssignee("zhangsan")
    .taskTenantId("PROD_TENANT")  // 底层 SQL 自动加 WHERE TENANT_ID_='PROD_TENANT'
    .list();
```

### 9.4 无感隔离：避免忘记写 TenantId

手动加 .taskTenantId() 容易遗忘导致泄露。推荐用 **ThreadLocal 上下文 + 统一查询封装**：

```java
UserContext.setCurrentTenant(isTestUser ? "TEST_TENANT" : "PROD_TENANT");

public List<Task> getMyTasks(String userId) {
    return taskService.createTaskQuery()
            .taskAssignee(userId)
            .taskTenantId(UserContext.getCurrentTenant())
            .list();
}
```

更极致可自定义 Flowable CommandInterceptor 拦截所有 Query 自动注入（适合有中大型团队/架构师）。

### 9.5 单库混用的三个致命风险与对策

1. **Job 互相抢占**：执行器轮询 ACT_RU_TIMER_JOB / ACT_RU_JOB 默认不区分 tenantId → 不同环境用不同 Job Executor 实例，或测试环境 async-executor-activate=false 改手动触发；
2. **流程定义 Key 冲突**：同 key 不同 tenantId 下，startProcessInstanceByKey 不传 tenantId 会报错/找错版本 → 启动时用 ProcessInstanceBuilder.processDefinitionKey(...).tenantId(...) 显式指定；
3. **历史数据无限膨胀**：测试频繁发起 → 定期清理脚本：

```sql
DELETE FROM ACT_HI_PROCINST
WHERE TENANT_ID_='TEST_TENANT' AND END_TIME_ < DATE_SUB(NOW(), INTERVAL 7 DAY);
-- 需同步清理 ACT_HI_TASKINST、ACT_HI_VARINST 等关联表
```

---

## 十、人员基础数据一致性：关闭 IDM 后的同步方案

核心答案：**基础数据不要拆成测试/正式两套，也不要往 Flowable 的 ACT_ID_* 表同步两套。** 让测试与正式共用同一套企业统一身份源，Flowable 只引用用户 ID、部门 ID、岗位 ID，隔离只发生在流程实例/任务/历史层面。

### 10.1 禁用 Flowable 内置 IDM

生产环境不建议维护 Flowable 自带的 ACT_ID_USER、ACT_ID_GROUP、ACT_ID_MEMBERSHIP、ACT_ID_PRIV、ACT_ID_PRIV_MAPPING。

```yaml
flowable:
  idm:
    enabled: false
```

BPMN 里的审批人配置仍生效——assignee、candidateUsers、candidateGroups 最终都是字符串 ID，不必来自 Flowable 内置表：

```xml
<userTask id="deptApprove" name="部门负责人审批"
          flowable:candidateGroups="${deptLeaderGroup}" />
```

### 10.2 同步方式一：TaskListener 动态分配（最推荐）

```java
@Component("dynamicAssigneeListener")
public class DynamicAssigneeListener implements TaskListener {
    @Autowired private OrganizationService organizationService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String deptId = (String) delegateTask.getVariable("deptId");
        String managerId = organizationService.findDeptManager(deptId);
        if (managerId != null) delegateTask.setAssignee(managerId);

        String groupId = organizationService.findDeptRoleId(deptId);
        if (groupId != null) delegateTask.addCandidateGroup(groupId);
    }
}
```

```xml
<userTask id="deptApprove" name="部门负责人审批">
    <extensionElements>
        <flowable:taskListener event="create"
                               delegateExpression="${dynamicAssigneeListener}" />
    </extensionElements>
</userTask>
```

> 必须用 delegateExpression，不能用 class，否则 Spring 注入失效。

### 10.3 同步方式二：自定义 UserGroupManager（候选组查询）

```java
@Component
public class CustomUserGroupManager implements UserGroupManager {
    @Autowired private OrganizationService organizationService;

    @Override public List<String> findGroupIdsByUser(String userId) {
        return organizationService.findGroupIdsByUser(userId);
    }
    @Override public List<String> findUsersByGroup(String groupId) {
        return organizationService.findUserIdsByGroup(groupId);
    }
}

@Bean
EngineConfigurationConfigurer<SpringProcessEngineConfiguration> userGroupManagerConfigurer(
        UserGroupManager m) {
    return cfg -> cfg.setUserGroupManager(m);
}
```

适合：Flowable 只需知道"谁属于哪个组"，不需要完整用户档案。

### 10.4 同步方式三：自定义 IdentityService/UserManager（强依赖场景）

若代码、设计器、Admin 控制台仍调用 identityService.createUserQuery()，可替换 IDM 查询实现，让它查自己的用户表。好处是 Flowable 原生查询接口仍可用，缺点是改造量大，仅在必须兼容 Flowable UI/Admin/老代码时才用。

### 10.5 测试/正式人员一致性原则

| 维度 | 做法 |
| :--- | :--- |
| 用户主数据 | 只维护一套，来自 HR / 统一用户中心 |
| 测试流程 | 流程实例带 TEST_TENANT |
| 正式流程 | 流程实例带 PROD_TENANT |
| 审批人解析 | 都走同一套组织服务 |
| 测试账号 | 用 is_test_user、test_tag 标记，而非复制一套用户表 |
| 发布校验 | 发布正式前检查流程里没有测试部门/角色/账号 |

数据流建议（单向同步）：

```
HR / 主数据系统 -> 消息队列/定时同步 -> 统一用户中心/组织中心 -> 按需读取 -> Flowable 流程引擎
```

### 10.6 发布正式前的引用校验

```java
void validateProcessBeforePublish(String bpmnXml) {
    // 解析 BPMN，提取所有 assignee / candidateUsers / candidateGroups
    // 检查是否存在 test_ / mock_ / dev_ 等测试前缀
    // 检查引用的部门、岗位、角色在正式组织中心是否仍存在
}
```

发现 test_dept_leader、mock_finance_group、dev_user_01 即禁止发布。

---

## 十一、落地清单与决策速查表

### 落地清单（按优先级）

1. 申请单永远用**第一个 UserTask** 承载，不要绑在 StartEvent 上；
2. 业务逻辑放中间 Task/ServiceTask，结束节点前加"流程收尾"ServiceTask；
3. 测试/正式**同一 BPMN**，靠 testMode 变量 + Spring Profile 隔离行为；
4. 用 **category** 管理流程定义状态（草稿/测试/正式），用 **tenantId** 隔离运行时数据；
5. 定时任务用**表达式缩短时间** + **Clock 时间旅行** + **手动触发 Job** 三种手段测试，且 Job Executor 物理隔离；
6. 单库混布时强制带 tenantId 过滤（ThreadLocal 自动注入），并定期清理测试历史；
7. **关闭 Flowable IDM**，人员/部门/岗位统一走企业用户中心，Flowable 只存 ID 引用；
8. **发布正式前校验流程引用**，杜绝测试账号/部门/通知渠道进入生产。

### 决策速查表

| 你的情况 | 推荐方案 |
| :--- | :--- |
| 刚开始用 Flowable，团队小 | 变量路由 + 独立测试库 |
| 已有成熟 Spring Boot 体系 | 变量路由 + Profile 隔离 |
| 金融/政务等强合规行业 | 变量路由 + Profile + 完全独立基础设施 + 自动化回归 + 发布卡点 |
| 单库混布，怕正式看到测试数据 | tenantId 行级隔离 + ThreadLocal 注入 + 历史清理 |
| 想"测完一键转正" | category 管状态 + tenantId 隔离数据 + 重新部署跃迁 |
| 仅需验证流程图语法 | Flowable 7 单元测试 + 内存引擎 |

---

## 全文一句话总结

> **事件是边界，任务是舞台。** 把开始/结束节点当作纯粹的生命周期标记，把业务、表单、审批人全部显式建模到中间节点；用 category 管流程定义的状态，用 tenantId 隔离运行时数据；测试靠变量路由与引擎时间控制，人员靠统一身份源与适配器按需读取。如此，才能在 Flowable 7 上构建出一个"画得规范、测得彻底、数据干净、长期可维护"的企业级流程管控中心。

---

内容由 AI 生成
