# 泛微 E9 · CRM 与建模 cube 模块分权分析（补齐 C）

> 续《跨模块风险登记册》C 线（FNA 已分析）。补齐 **CRM** 与 **cube（表单建模）** 的分权（数据隔离）现状，对照 FNA 的开关耦合问题。
> 源码根：`e:/project/ecology/ecology/src`。**只读分析，未修改任何代码。**

---

## 1. 结论速览

| 模块 | 是否模块分权 | 开关/机制 | 主要风险 |
|---|---|---|---|
| **cube（建模）** | ✅ 是 | `ManageDetachComInfo.isUseFmManageDetach()` + `CheckSubCompanyRight` | `DetachHelper` 缓存 key 漏 `rightStr`（DET-P0-2）；列表 SQL 拼接 parsed-int（CUBE-N1） |
| **CRM** | ❌ 否 | 自有**记录级共享模型**（`CRM_ShareInfo` + `crmShareBase`），分部为共享维度之一 | 隔离依赖每端点接入共享视图，漏接即泄露（CRM-N1） |
| FNA（前篇） | ⚠️ 自实现 | **错用 `isUseWfManageDetach`（workflow 开关）** | 跨模块开关耦合（FNA-N1） |

→ cube 的分权**实现正确**（与 FNA 形成鲜明对照）；CRM 走完全不同的共享体系，需专项审计。

---

## 2. cube（表单建模）分权

### 2.1 开关与判定（正确）
`com/engine/cube/biz/DetachHelper.java`：
- `modeDetachIsOpen()` → `mdc.isUseFmManageDetach()`（**建模专属开关**，非 workflow）。
- `getUserDeatchOperateLevel(user, subCompanyId, userRightStr)`：
  - 开：`CheckSubCompanyRight.ChkComRightByUserRightCompanyId(uid, rightStr, subCompanyId)`；
  - 关：`HrmUserVarify.checkUserRight(rightStr, user)` → operatelevel=2。
- `getUserDetachInfo` 用 `FORMMODEAPP:ALL` 权限 + `isUseFmManageDetach`。

### 2.2 列表查询真实应用（正确）
`com/engine/cube/cmd/app/GetAppListCmd.java:34,46,51,116,195`：
```java
this.isUseFmManageDetach = manageDetachComInfo.isUseFmManageDetach();   // L34 用建模开关
if(!RightHelper.checkBackRight("FORMMODEAPP:ALL", user, retmap)) return; // L46 功能权限
if(subCompanyId!=-1){
    sql += " and (subCompanyId =" + subCompanyId + " or ( id=1 ... ))";   // L116/L195 分部过滤
}
```
→ 先功能权限、再建模分权开关、再 SQL 分部过滤，**链路完整**。

### 2.3 风险
- **DET-P0-2（已登记，P0）**：`getUserDeatchOperateLevel` 缓存 key = `uid+"_"+subCompanyId`（L39），**不含 `userRightStr`** → 同一用户+分部、不同权限串命中同一缓存 → 跨权限串泄漏（60s）。修复：key 追加 `rightStr`。
- **CUBE-N1（P2/NAME）**：`GetAppListCmd:116/195` 直接拼接 `subCompanyId`，仅经 `Util.getIntValue` 解析为整数防注入（非参数化）。建议改用参数化 / 统一 `SubcompanyAwareSql` 构造器（见《泛微E9-分权统一收口方案.md》）。

---

## 3. CRM 分权

### 3.1 无模块分权（确认）
- 全仓搜索 `isUseCrmManageDetach` / `CrmDetach` → **0 命中**，CM 不存在 CRM 专属分权开关。
- `ManageDetachComInfo` 在 CRM 仅 2 处使用，且都是 `appDetachDisableAll(user)`——仅用于**隐藏「所有人」共享范围 UI 选项**（`AuthorityFormUtil.java:44/72`、`GetCustomerShareConditionsCmd.java:56`），**不参与数据过滤**。

### 3.2 自有记录级共享模型（替代分权）
`com/engine/crm/cmd/customer/CrmMutiShareCmd.java:119-189` 揭示 CRM 数据隔离机制：
- 写 `CRM_ShareInfo` 表：`sharetype` 1=人员/2=部门/3=角色/4=所有人/5=**分部**/6=岗位，`sharelevel`、`seclevel`、`scopeid`、`subcompanyid`。
- 分部是共享维度之一（`sharetype=5`，L128/L150）。
- 写完调用 `baseService.crmShareBase.setCRM_WPShare_newCRMShare(crmid, shareobjid)` —— 由 `crmShareBase` 维护共享派生的权限视图。
- 全仓 23 个 CRM 文件引用 `subcompanyid`，均围绕共享/团队/公海，证明这是**每记录 ACL**，而非模块级分部隔离。

### 3.3 风险
- **CRM-N1（P2/CONS）**：CRM 数据可见性取决于**每个读端点是否接入 `CRM_ShareInfo`/`crmShareBase` 共享视图**。任一列表/详情查询漏接共享过滤 → 跨分部/跨记录泄露。这是与「分权模块」不同的攻击面，需**专项共享强制审计**（建议引入 `@CrmShareScope` 拦截器统一强制，见统一收口方案）。

---

## 4. 跨模块分权对照（核心洞察）

| 维度 | workflow | cube | FNA | CRM |
|---|---|---|---|---|
| 分权开关 | `isUseWfManageDetach` | `isUseFmManageDetach` | **`isUseWfManageDetach`（错用）** | 无（共享模型） |
| 隔离机制 | `CheckSubCompanyRight` | `CheckSubCompanyRight` | 角色→分部白名单 | `CRM_ShareInfo` ACL |
| 应用方式 | 框架/手动 | 手动+SQL | **16+ 手动样板** | 共享视图 |
| 主要缺陷 | — | 缓存 key 漏 rightStr | 开关耦合 + 样板 | 共享强制审计缺失 |

→ **根因一致**：各模块自实现、无统一收口。整改方向见《泛微E9-分权统一收口方案.md》。

---

## 5. 证据索引

| 文件 | 行 | 内容 |
|---|---|---|
| `com/engine/cube/biz/DetachHelper.java` | 25-91 | `isUseFmManageDetach` 开关 + `CheckSubCompanyRight`；L39 key 漏 rightStr |
| `com/engine/cube/cmd/app/GetAppListCmd.java` | 34,46,51,116,195 | 功能权限 + 建模开关 + `subCompanyId` SQL 过滤（拼接整数） |
| `com/engine/crm/util/AuthorityFormUtil.java` | 44,72 | `appDetachDisableAll` 仅 UI 约束 |
| `com/engine/crm/cmd/customer/GetCustomerShareConditionsCmd.java` | 56 | 同上 |
| `com/engine/crm/cmd/customer/CrmMutiShareCmd.java` | 119-189 | `CRM_ShareInfo` 共享（含分部 sharetype=5）+ `crmShareBase` |
