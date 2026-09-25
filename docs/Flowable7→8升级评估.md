# Flowable 7.1.0 → 8.x 升级评估

> 目的：评估升级可行性与破坏面
> 源码基线：`D:\workproject\springbladeandreact\flowable-engine`（**8.1.0-SNAPSHOT**）
> 项目基线：Flowable **7.1.0** / Spring Boot **4.x** / Java **21**
> 关联：`工作流双写漂移治理.md`（`ACT_*` 表 39 张，`databaseSchemaUpdate=none`）

---

## 0. 纠正一个先前判断（重要）

此前曾判断「8.1.0-SNAPSHOT 与 Boot 4 兼容性未知，可能比 7.1.0 风险更高」—— **该判断错误**。

实测 Flowable 8 的依赖基线：

```55:59:flowable-engine/modules/flowable-dependencies/pom.xml
        <spring.boot.version>4.1.0-RC1</spring.boot.version>
        <spring.kafka.version>4.1.0-RC1</spring.kafka.version>
        <spring.ldap.version>4.1.0-RC1</spring.ldap.version>
        <spring.framework.version>7.0.7</spring.framework.version>
        <spring.security.version>7.1.0-RC1</spring.security.version>
```

**Flowable 8 正是为 Boot 4 / Spring Framework 7 构建的**，与本项目 Boot 4.x **对齐**。
而项目 pom 注释写的是「Flowable 7 官方基线为 Boot 3.x / JDK 17，故不使用 starter」。

→ 升级到 8 **反而解决** Boot 4 对齐问题（理论上可启用官方 starter）。

---

## 1. 版本路径（官方 upgrade 脚本已证实）

```
7.1.0 ──▶ 7.2.0 ──▶ 8.0.0
```

`distro/sql/upgrade/all/` 下存在各数据库的 `upgradestep.*.to.*.all.sql`，说明**升级路径是官方支持的**。

---

## 2. API 兼容性核实（项目实际用到的类）

| 项目使用的类 | 8.x 中是否存在 | 位置 |
|---|---|---|
| `org.flowable.spring.SpringProcessEngineConfiguration` | ✅ | `flowable-spring` |
| `org.flowable.spring.ProcessEngineFactoryBean` | ✅ | `flowable-spring` |
| `org.flowable.bpmn.converter.BpmnXMLConverter` | ✅ | `flowable-bpmn-converter` |
| `org.flowable.engine.{ProcessEngine, RepositoryService, RuntimeService, TaskService, HistoryService, ManagementService}` | ✅（核心稳定 API） | `flowable-engine` |
| `org.flowable.engine.history.{HistoricActivityInstance, HistoricProcessInstance}` | ✅ | `flowable-engine` |
| `org.flowable.engine.repository.{Deployment, ProcessDefinition}` | ✅ | `flowable-engine` |
| `org.flowable.engine.runtime.ProcessInstance` | ✅ | `flowable-engine` |
| `org.flowable.task.api.Task` | ✅ | task api |
| `org.flowable.bpmn.model.*`（`BpmnModel`/`UserTask`/`BoundaryEvent`/`TimerEventDefinition`/`Gateway`/…） | ✅ | `flowable-bpmn-model` |

> 上表中标 ✅ 的前三项为**实测文件存在**；其余为核心稳定 API，需在升级时以编译为准复核。

---

## 3. DB 破坏面（实测：很小）

### 3.1 `7.1.0 → 7.2.0`（MySQL，BPMN 相关部分）

```sql
alter table ACT_RU_ACTINST add column COMPLETED_BY_ varchar(255);
alter table ACT_HI_ACTINST add column COMPLETED_BY_ varchar(255);
create index ACT_IDX_BYTEAR_DEPL on ACT_GE_BYTEARRAY(DEPLOYMENT_ID_);
-- + ACT_GE_PROPERTY 版本号更新
```

### 3.2 `7.2.0 → 8.0.0`（MySQL，BPMN 相关部分）

```sql
alter table ACT_HI_PROCINST add column END_USER_ID_ varchar(255);
alter table ACT_HI_PROCINST add column STATE_ varchar(255);
-- + ACT_GE_PROPERTY 版本号更新
```

### 3.3 评估

- 全部为 **`add column` / `create index`**，**无删列、无改列、无数据重写**
- → 可在线执行；回滚只需 `drop column`
- 脚本中的 CMMN / DMN / event-registry 部分本项目**未使用，可跳过**
- ⚠️ **关键**：项目 `blade.flowable.database-schema-update=none`，引擎**不会自动升级**，
  **必须手工执行**上述两个脚本，否则引擎启动会因 schema version 校验失败

---

## 4. 风险清单

| 级别 | 风险 | 说明 |
|---|---|---|
| 🔴 高 | **预发布栈** | 本地源码为 `8.1.0-SNAPSHOT`（开发分支）；其依赖 Boot **4.1.0-RC1**、Security **7.1.0-RC1** 均为 RC。**生产不可用** |
| 🔴 高 | **需先确认 GA** | 本地源码**不能证明** 8.x 已发布 GA。必须先查 Maven Central；若有 8.0.x GA，优先用 GA，**不要**自行构建 SNAPSHOT |
| 🟠 中 | 自定义装配需重测 | `FlowableConfig` 是项目自写（类都还在，但默认值 / 行为可能变化） |
| 🟠 中 | 回归面 | BPMN 部署与解析（`WfDefinitionServiceImpl` 大量使用 bpmn-model）、历史查询、`moveActivity` 跳转 |
| 🟡 低 | 无官方迁移指南 | docs 中只有 `ch08-ProcessInstanceMigration.md`（**实例**迁移，非版本升级指南） |
| 🟡 低 | JDK 差异 | 源码基线 JDK 17，项目 21 —— 可行 |

---

## 5. 结论与建议

| 如果你的目标是… | 结论 |
|---|---|
| **解决 Boot 4 对齐** | 升 8 有**真实收益**（Flowable 8 本就面向 Boot 4）。但**等 GA**，且不要用 SNAPSHOT |
| **解决组织 / 数据权限** | ❌ **升级帮不上**。该问题现状已解决 —— 见 `组织数据取数路径核实.md`：引擎不参与可见性判定 |
| **解决 `wf_*` / `ACT_*` 双写漂移** | ❌ **升级帮不上**。漂移是代码路径问题，已由方案 A（收口）+ 方案 C（事件派生）解决 |
| **用 Flowable 8 新能力** | 需先明确是哪个能力，再评估能否在 7.1.0 上用扩展点替代 |

### 建议动作顺序

1. 查 Maven Central 确认 Flowable 8 的 **GA 版本**（本评估不能替代这一步）
2. 若有 GA → 在**独立分支**改 `flowable.version`，编译并跑 `WfMysqlUseCaseTest`
3. 手工执行 7.1.0→7.2.0→8.0.0 两个 MySQL 升级脚本（跳过 CMMN/DMN 部分）
4. 回归：BPMN 部署解析、发起 / 审批 / 驳回 / 撤销 / 暂停 / 恢复、`moveActivity` 指定流转
5. 重跑 `drift_check.sql`（新增的 `ACT_HI_PROCINST.STATE_` 可用于**增强** 6a/6b/6c 对账）

---

## 6. 待确认项

- [ ] Maven Central 上 Flowable 8.x 是否已有 **GA**（非 SNAPSHOT / RC）
- [ ] 若用 GA，其 Spring Boot 基线是否为 **GA 版**（而非 4.1.0-RC1）
- [ ] 升级后是否改用官方 `flowable-spring-boot-starter`（可简化 `FlowableConfig`），还是继续显式装配
- [ ] `ACT_HI_PROCINST.STATE_` 能否替代现有 `wf_instance.status` 的部分对账逻辑
