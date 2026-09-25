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

### 0.1 Maven Central 核实（2026-09-26）：**8.0.0 已是 GA**

| artifact | `<latest>` / `<release>` | 结论 |
|---|---|---|
| `org.flowable:flowable-engine` | **8.0.0** | ✅ GA |
| `org.flowable:flowable-spring` | **8.0.0** | ✅ GA |
| `org.flowable:flowable-spring-boot-starter` | **8.0.0** | ✅ GA，**官方 starter 可用** |

版本列表确认 `7.0.1 → 7.1.0 → 7.2.0 → 8.0.0` 路径真实存在；`lastUpdated` = `20260227161638`（2026-02-27 发布，至今约 7 个月）。

**关键反转 —— 8.0.0 GA 的依赖基线全是正式版**（`flowable-dependencies-8.0.0.pom`）：

```properties
<spring.boot.version>4.0.2</spring.boot.version>          ← GA，非 RC
<spring.framework.version>7.0.3</spring.framework.version> ← GA
<spring.security.version>7.0.2</spring.security.version>   ← GA
```

> 而本地源码 `flowable-engine`（**8.1.0-SNAPSHOT**）用的是 Boot **4.1.0-RC1** / Framework **7.0.7**。
> 说明本地那份是 **8.0.0 GA 之后的下一个开发分支**，依赖尚未锁定为 GA。
> **结论：不要构建本地 SNAPSHOT，直接用 Maven Central 的 8.0.0 GA。**

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
| ✅ 已解除 | ~~预发布栈~~ | **8.0.0 已 GA**（2026-02-27），且依赖基线 Boot **4.0.2** / Framework **7.0.3** / Security **7.0.2** **全为 GA**。→ **直接用 Maven Central 的 8.0.0，不要构建本地 8.1.0-SNAPSHOT** |
| 🟠 中 | **Boot 版本需对齐** | Flowable 8.0.0 编译基线为 Boot **4.0.2**。项目为 SpringBlade **5.0.2**（Boot 4.x），**需确认其具体 Boot 版本**：4.0.x → 完全对齐；4.1.x → 需验证（通常兼容，但应实测） |
| 🟠 中 | **动态数据源** | 项目用 `blade.datasource.workflow.*`（dynamic-datasource）。若改用官方 starter，需验证它能否正确绑定**主数据源**；否则继续保留自定义 `FlowableConfig` |
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

- [x] ~~Maven Central 上 Flowable 8.x 是否已有 GA~~ → **已确认：8.0.0 GA**
- [x] ~~GA 的 Spring Boot 基线是否为 GA 版~~ → **已确认：Boot 4.0.2 / Framework 7.0.3，全 GA**
- [ ] **项目 SpringBlade 5.0.2 的具体 Boot 版本**（4.0.x 对齐 / 4.1.x 需实测）
- [ ] 升级后是否改用官方 `flowable-spring-boot-starter`（可简化 `FlowableConfig`），还是继续显式装配（受 dynamic-datasource 影响）
- [ ] `ACT_HI_PROCINST.STATE_` 能否替代现有 `wf_instance.status` 的部分对账逻辑

---

## 7. 推荐动作（更新版）

1. 根 pom `<flowable.version>7.1.0</flowable.version>` → **`8.0.0`**（两个依赖都不用改）
2. **不要**引入本地 `flowable-engine` 源码 —— 它是 8.1.0-SNAPSHOT 开发分支
3. 手工执行 MySQL 升级脚本（顺序不可颠倒，`databaseSchemaUpdate=none` 不会自动升级）：
   - `flowable.mysql.upgradestep.7.1.0.to.7.2.0.all.sql`
   - `flowable.mysql.upgradestep.7.2.0.to.8.0.0.all.sql`
   - （脚本内 CMMN / DMN / event-registry 部分本项目未使用，可跳过）
4. 编译 + 跑 `WfMysqlUseCaseTest`，重点回归：BPMN 部署解析、发起 / 审批 / 驳回 / 撤销 / 暂停 / 恢复、`moveActivity` 指定流转
5. 重跑 `drift_check.sql`；评估用新增的 `ACT_HI_PROCINST.STATE_` 增强 6a/6b/6c 对账
6. （可选）评估改用官方 `flowable-spring-boot-starter` 以简化 `FlowableConfig`

---

## 8. 实测记录（2026-09-26）

### 8.1 已完成的动作与结果

| 步骤 | 结果 |
|---|---|
| 根 pom `<flowable.version>` `7.1.0` → **`8.0.0`** | ✅ 已改 |
| `mvn -pl blade-service/blade-workflow -am compile -DskipTests -B` | ✅ **BUILD SUCCESS** |
| 从 Maven Central 拉取 `flowable-engine` / `flowable-spring` **8.0.0** | ✅ 已进入本机 m2 |
| 业务代码改动 | **0 处**（`ProcessServiceImpl` / `FlowableConfig` / `WfDefinitionServiceImpl` 的 Flowable import 全部兼容） |

> 首次**不带 `-am`** 的编译失败，原因是兄弟模块（`blade-common` / `blade-workflow-api` 等）未安装到 m2，
> **与 Flowable 无关**；加 `-am` 从源码构建依赖链后通过。

### 8.2 一个重要的澄清（显著降低风险）

`flowable-engine` 与 `flowable-spring` **不依赖 Spring Boot**，只依赖 Spring Framework
（`spring-context` / `spring-jdbc` / `spring-tx` / `spring-orm`）。

→ 「Flowable 8.0.0 基线 Boot **4.0.2** vs 项目 Boot **4.1.x**」的差异，
**只影响 `flowable-spring-boot-starter`**，而本项目**不使用 starter**（自定义 `FlowableConfig`）。
这解释了为何**一次编译即通过、零改动**。

### 8.3 关于本地 `flowable-engine` 源码：**不需要编译**

| 对比 | 本地源码 | Maven Central |
|---|---|---|
| 版本 | **8.1.0-SNAPSHOT**（8.0.0 GA 之后的**开发分支**） | **8.0.0 GA** |
| 依赖 | Boot **4.1.0-RC1** / Security **7.1.0-RC1**（RC 预发布） | Boot 4.0.2 / Framework 7.0.3（全 GA） |
| 可用性 | 未发布，生产不可用 | ✅ 已发布，已实测编译通过 |
| 构建成本 | 全量构建（8000+ java 文件） | 无需构建 |

**结论：编译本地源码只会得到一个更不稳定、更未发布的版本。用 8.0.0 GA。**

### 8.4 ⚠️ 下一步：编译通过 ≠ 可运行

`blade.flowable.database-schema-update=none`，引擎**不会自动升级 schema**。
**不执行升级脚本直接启动会失败** —— `ACT_GE_PROPERTY.schema.version` 仍为 7.1.0，而 8.0.0 引擎要求 `8.0.0.0`。

必须**按顺序**手工执行（MySQL）：

1. `flowable.mysql.upgradestep.7.1.0.to.7.2.0.all.sql`
2. `flowable.mysql.upgradestep.7.2.0.to.8.0.0.all.sql`

> 脚本内 CMMN / DMN / event-registry 部分本项目未使用，可跳过。
> 内容均为 `add column` / `create index`，无删改列、无数据重写，可在线执行、可回滚。
