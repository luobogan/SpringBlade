# Flowable 7.1.0 → 8.0.0 升级执行记录

> 状态：**已完成（数据库升级部分）**
> 执行日期：**2026-09-26**
> 关联：`Flowable7→8升级评估.md`（评估与破坏面）、`flowable-engine-upgrade-7.1.0-to-8.0.0.mysql.sql`（执行的脚本）

---

## 1. 环境信息

| 项 | 值 |
|---|---|
| 数据库 | MySQL 8.0.23 |
| 客户端路径 | `D:\project\mysql-8.0.23-winx64\bin\mysql.exe` |
| 目标库 | **`blade_workflow`**（存放 `wf_*` 与 `ACT_*`） |
| 连接 | `mysql -uroot -p*** blade_workflow --default-character-set=utf8mb4` |
| 执行脚本 | `springBlade/docs/flowable-engine-upgrade-7.1.0-to-8.0.0.mysql.sql` |
| 原始输出存档 | `D:\temp\flowable8_upgrade.log` |

---

## 2. 前置检查（执行前）

| 检查项 | 结果 | 判定 |
|---|---|---|
| `ACT_GE_PROPERTY.schema.version` | **7.1.0.2** | ✅ 与脚本要求的起点一致 |
| `ACT_GE_PROPERTY.common.schema.version` | **7.1.0.2** | ✅ |
| `ACT_ID_PROPERTY` 表是否存在 | **存在**（count=1） | ✅ 脚本中两行可安全执行，无需注释 |

---

## 3. 执行内容

按官方路径 `7.1.0 → 7.2.0 → 8.0.0`，**仅 BPMN 部分**（已剔除 CMMN / DMN / event-registry 建表改表语句）。

### 3.1 结构变更

```sql
alter table ACT_RU_ACTINST add column COMPLETED_BY_ varchar(255);
alter table ACT_HI_ACTINST add column COMPLETED_BY_ varchar(255);
alter table ACT_HI_PROCINST add column END_USER_ID_ varchar(255);
alter table ACT_HI_PROCINST add column STATE_ varchar(255);
create index ACT_IDX_BYTEAR_DEPL on ACT_GE_BYTEARRAY(DEPLOYMENT_ID_);
```

### 3.2 版本号变更

`ACT_GE_PROPERTY` 的 `schema.version` / `common.schema.version` / `app` / `cmmn` / `dmn` / `eventregistry.schema.version`，
以及 `ACT_ID_PROPERTY.schema.version`，统一置为 **8.0.0.0**。

---

## 4. 执行后校验（全部通过）

| 校验项 | 期望 | 实际 | 结果 |
|---|---|---|---|
| `schema.version` | 8.0.0.0 | **8.0.0.0** | ✅ |
| `common.schema.version` | 8.0.0.0 | **8.0.0.0** | ✅ |
| `eventregistry.schema.version` | 8.0.0.0 | **8.0.0.0** | ✅ |
| 新增列数量 | 4 | **4** | ✅ |
| `act_hi_actinst.COMPLETED_BY_` | 存在 | 存在 | ✅ |
| `act_ru_actinst.COMPLETED_BY_` | 存在 | 存在 | ✅ |
| `act_hi_procinst.END_USER_ID_` | 存在 | 存在 | ✅ |
| `act_hi_procinst.STATE_` | 存在 | 存在 | ✅ |
| `ACT_IDX_BYTEAR_DEPL`（`act_ge_bytearray`） | 存在 | 存在 | ✅ |

**执行过程零错误。**

---

## 5. 代码侧变更（已完成）

| 文件 | 变更 |
|---|---|
| `springBlade/pom.xml` | `<flowable.version>` `7.1.0` → **`8.0.0`**（含升级说明注释） |

编译验证：

```
mvn -pl blade-service/blade-workflow -am compile -DskipTests -B
→ BUILD SUCCESS
```

- `flowable-engine` / `flowable-spring` **8.0.0** 已从 Maven Central 拉取进本机 m2
- **业务代码 0 处改动**（`ProcessServiceImpl` / `FlowableConfig` / `WfDefinitionServiceImpl` 的 Flowable import 全部兼容）

> 关键澄清：`flowable-engine` + `flowable-spring` **不依赖 Spring Boot**，只依赖 Spring Framework。
> 故「Flowable 8.0.0 基线 Boot 4.0.2 vs 项目 Boot 4.1.x」的差异**只影响 starter**，
> 本项目不用 starter（自定义 `FlowableConfig`）—— 这是零改动编译通过的原因。

---

## 5.1 测试验证（2026-09-26）

```
mvn -pl blade-service/blade-workflow -am test -B
→ Tests run: 8, Failures: 0, Errors: 0, Skipped: 1
→ BUILD SUCCESS
```

| 测试类 | 结果 |
|---|---|
| `WfEngineSmokeTest` | ✅ 4 个用例全通过（BPMN 部署 → 发起 → 条件分支 → 历史覆盖率采集 → 测试部署卸载） |
| `FlowableIntegrationTest` | ✅ 通过 |
| `CreateActTablesTest` | ⏭ 跳过（`@Disabled`，连真实 MySQL 建表，常规构建不执行） |

日志实测（真实 Flowable 8.0.0 引擎）：
- `BPMN 部署成功. procKey=conditionalApproval, deploymentId=...`
- `发起流程实例. bizKey=1:1, engineInstId=...`（多组假数据枚举分支）
- `卸载测试部署. deploymentId=...`（零污染）

> ⚠️ **注意**：这两个测试用的是 `FlowableTestConfig` 的**内存 H2 引擎**，
> 验证的是「Flowable 8.0.0 引擎内核可用」，**并未验证 MySQL `blade_workflow` 的运行时启动**。
> MySQL 侧目前只验证了 schema 版本号已正确置为 `8.0.0.0`（见 §4），
> 真正的运行时校验仍需**启动 blade-workflow 服务**。

---

## 6. 回滚方式

结构变更均为 `ADD COLUMN` / `CREATE INDEX`，可回滚：

```sql
-- 仅在需要回退时执行，执行前请先把 pom 改回 7.1.0
alter table ACT_HI_PROCINST drop column END_USER_ID_;
alter table ACT_HI_PROCINST drop column STATE_;
alter table ACT_RU_ACTINST  drop column COMPLETED_BY_;
alter table ACT_HI_ACTINST  drop column COMPLETED_BY_;
drop index ACT_IDX_BYTEAR_DEPL on ACT_GE_BYTEARRAY;

update ACT_GE_PROPERTY set VALUE_ = '7.1.0.2' where NAME_ = 'schema.version';
update ACT_GE_PROPERTY set VALUE_ = '7.1.0.2' where NAME_ = 'common.schema.version';
update ACT_ID_PROPERTY set VALUE_ = '7.1.0.2' where NAME_ = 'schema.version';
```

> 更稳妥的方式：用执行前做的库备份恢复。

---

## 7. 后续待办

- [x] 编译验证（BUILD SUCCESS，0 改动）
- [x] 数据库升级 + 结构校验（8.0.0.0，4 列 1 索引）
- [x] 引擎测试（8 用例全通过）
- [ ] **启动 blade-workflow 验证 MySQL 运行时**（schema 版本校验、FlowableConfig 装配、BPMN 部署解析）
- [ ] 回归：发起 / 审批 / 驳回 / 撤销 / 暂停 / 恢复、`moveActivity` 指定流转
- [ ] 重跑 `drift_check.sql`（第 2/3/6 段应为 0）
- [ ] 评估新增的 `ACT_HI_PROCINST.STATE_` 能否增强 6a/6b/6c 对账
- [ ] 执行那条历史漂移实例（`engine_inst_id=102501`）的存量清理
- [ ] （可选）评估改用官方 `flowable-spring-boot-starter` 简化 `FlowableConfig`
