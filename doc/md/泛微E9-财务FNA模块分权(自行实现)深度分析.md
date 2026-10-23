# 泛微 E9 财务 FNA 模块分权（自行实现）深度分析

> 续《泛微E9-分权ManageDetach机制深度分析》中 DET-P2-6 的「FNA 自行实现分权」。这是未深模块（cube / CRM / FNA）中风险最集中者——**财务属高敏感数据且自实现权限**。
> 源码根：`e:/project/ecology/ecology/src`。**只读分析，未修改任何代码。**

---

## 1. 结论（一句话）

FNA 的「分部数据隔离」**自实现**于 `FnaDetachControlUtil.getSubCompany(resourceid)`，且**以 `ManageDetachComInfo.isUseWfManageDetach()`（workflow 模块的开关）作为闸门**——即财务数据可见性被绑定到「流程是否开启分权」，而非财务自身/全局分权开关。这是一处**跨模块耦合的一致性缺陷**，配合「每端点手工重复 detach 样板」的维护模式，存在跨分部财务数据暴露与漏加过滤的风险。

> 重要纠正：登记册 DET-P2-6 称「`FnaDetachControlUtil L55 塞 -1`」为风险，**实际 `-1` 是良性空守卫**（见 §3），真正问题在开关耦合与样板重复（见 §5）。

---

## 2. 实现结构：两份文件，一基一壳

| 文件 | 角色 |
|---|---|
| `weaver/fna/general/FnaDetachControlUtil.java` | **真正实现**（`getSubCompany` 方法，L26-58） |
| `com/engine/fna/util/FnaDetachControlUtil.java` | **空壳子类**：`public class FnaDetachControlUtil extends weaver.fna.general.FnaDetachControlUtil {}`（L18-20），仅为新引擎包路径提供同名类型，逻辑全在父类 |

→ 两份并非「重复实现/分叉」，而是父类+兼容子类，无逻辑分歧。

---

## 3. `getSubCompany(resourceid)` 逻辑（L26-58）

```java
public static String getSubCompany(int resourceid) {
    boolean viewAllSubCompany = false;
    boolean sanyuanAble = HrmSanyuanAdminBiz.getSanyuanAble();
    if(sanyuanAble) viewAllSubCompany = HrmSanyuanAdminBiz.canViewAllSubCompany(resourceid);
    List<String> subCompanyList = new ArrayList<String>();
    if(viewAllSubCompany){
        rs.executeQuery("select * from hrmsubcompany");           // 三员总管：全分部
    }else{
        rs.executeQuery("select b.subcompanyid from hrmrolemembers a
                         join SysRoleSubcomRight b on a.roleid=b.roleid and a.resourceid=?", resourceid);
    }
    subCompanyList.add("-1");   // ← 空列表时 in(-1) 匹配不到真实分部，保证 SQL 不报错
    return subCompanyList.toString().replaceAll("\\[|\\]|\\s", "");  // 直接拼入 in(...)
}
```

**`-1` 语义澄清（纠正 DET-P2-6）**：
- 无论是否有授权分部，`-1` 都会被无条件追加。
- 有授权时：`in(1,2,-1)`，`-1` 为无害多余值；
- 无授权时：`in(-1)`，匹配不到任何真实分部 → **用户看不到任何数据**（安全空结果，非越权）。
- 值来自 DB（`hrmsubcompany.id` / `SysRoleSubcomRight.subcompanyid`，均为数值），**无用户可控注入**。
- 因此 `-1` 是**防御性空守卫，不是权限绕过**。

**真正的问题在「开关缺位」**：本方法**完全不读全局/财务分权总开关**，只按「角色→分部」授权过滤。是否生效，取决于调用方是否先判断开关（见 §4）。

---

## 4. 消费端模式（16+ 调用点）

所有消费端（如 `GetFnaVoucherListCmd`、`BankEnterpriseUtil`、`GetConnectListCmd`…）统一为：

```java
ManageDetachComInfo manageDetachComInfo = new ManageDetachComInfo();
boolean isUseWfManageDetach = manageDetachComInfo.isUseWfManageDetach();   // ← workflow 模块的开关！
if (isUseWfManageDetach && user.getUID() != 1) {                          // 管理员(UID=1)绕过
    String subCompanyListStr = FnaDetachControlUtil.getSubCompany(user.getUID());
    // 拼入 sqlWhere: b.subcompanyId in(...)   （多数用 StringEscapeUtils.escapeSql 处理）
}
```

特征：
1. **闸门是 `isUseWfManageDetach`（workflow 分权开关）**，不是财务/全局开关。
2. **每端点手工重复**同一段样板（约 16 处），靠开发者「记得加」。
3. 过滤值经 `StringEscapeUtils.escapeSql` / `FnaCommon.initData1` 处理，注入面主要在 `getSubCompany` 的 DB 派生值上（安全）。

---

## 5. 关键风险

| ID | 严重度 | 问题 | 证据 |
|---|---|---|---|
| **FNA-N1** | P1/SEC | **跨模块开关耦合**：FNA 财务数据可见性由 `isUseWfManageDetach`（workflow 开关）控制。若部署中 workflow 分权**关闭**（默认常关），所有非管理员用户可**跨分部查看全部财务数据**——高敏感暴露。配置依赖型，若关闭则实际为 P0 级。 | 各 Cmd L96-98 / `BankEnterpriseUtil.java:8598-8600` |
| **FNA-N2** | P1/DATA | **注入面**：`BankEnterpriseUtil.java:8601` 把 `workflowId` **裸拼**进 SQL（`" ... and id = " + workflowId`，无 escapeSql）；`GetAddConnectPageCmd.java:332` 用 `xssUtil.put(sqlWhere)` 处理本应走 SQL 转义的片段 | `BankEnterpriseUtil.java:8601`、`GetAddConnectPageCmd.java:332` |
| **FNA-N3** | P2/CONS | **手工样板重复**（16+ 处）：任一新端点漏加 `if(isUseWfManageDetach...)` 即造成跨分部暴露；应统一收口到框架/拦截器 | 各 `com/engine/fna/cmd/**/*Cmd.java` |
| （纠正） | — | **DET-P2-6 的 `-1` 误读**：`-1` 为良性空守卫，非权限绕过 | `FnaDetachControlUtil.java:55` |

---

## 6. 整改建议

1. **FNA-N1（优先）**：将闸门从 `isUseWfManageDetach` 改为**财务专属/全局分权开关**（如 `ManageDetachComInfo` 对应的财务方法，或全局 `isUseManageDetach()`）；并在「开关关闭」时明确 FNA 的可见性语义（全量 vs 受限），避免静默暴露。补单测覆盖「workflow 分权关 / 财务分权开」组合。
2. **FNA-N2**：`workflowId` 改 `escapeSql` 或参数化；`xssUtil` 不应用于 SQL 上下文，改用 SQL 转义/参数化。
3. **FNA-N3**：抽公共 `FnaSubcompanyFilter` 或 JAX-RS `ContainerRequestFilter`，自动对 FNA 列表查询注入分部过滤，消除逐端点样板与漏加风险。
4. **DET-P2-6 修正**：将「FNA 自行实现（塞 -1）」改为上述开关耦合 + 样板重复的描述（见本文）。

---

## 7. 证据索引

| 文件 | 行 | 内容 |
|---|---|---|
| `weaver/fna/general/FnaDetachControlUtil.java` | 26-58 | `getSubCompany`：三员/角色→分部，L55 追加 `-1` |
| `com/engine/fna/util/FnaDetachControlUtil.java` | 18-20 | 空壳子类 `extends weaver.fna.general.FnaDetachControlUtil` |
| `com/engine/fna/cmd/financialVoucher/GetFnaVoucherListCmd.java` | 96-113 | `isUseWfManageDetach` 闸门 + `getSubCompany` + `escapeSql` |
| `com/engine/fna/util/BankEnterpriseUtil.java` | 8597-8603 | `isUseWfManageDetach` 闸门；L8601 `workflowId` 裸拼 |
| `com/engine/fna/cmd/bankEnterpriseConnect/GetAddConnectPageCmd.java` | 328-336 | `getSubCompany`；L332 `xssUtil.put(sqlWhere)` |
| `com/engine/fna/cmd/invoiceWorkflowSetting/DoInterfaceWorkflowSaveBaseCmd.java` | 64-70 | `getSubCompany` + `escapeSql` 校验 workflow 归属 |
