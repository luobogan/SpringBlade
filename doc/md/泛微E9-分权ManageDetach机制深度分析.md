# 泛微 E9 分权（ManageDetach / 分部维度权限）机制深度分析

> 分析对象：`E:\project\ecology\ecology\src`（`weaver/hrm/moduledetach`、`weaver/systeminfo/systemright`、`com/engine/hrm/cmd/detachmanagerset`、`com/engine/hrm/cmd/modulemanagedetach`、各模块接入点）。
> **只读分析，未修改任何代码。** 所有路径为绝对路径，行号对应上述仓库根。

---

## 目录

- [1. 分权总体机制](#1-分权总体机制)
- [2. 判定链路](#2-判定链路)
- [3. 各模块接入点全清单](#3-各模块接入点全清单)
- [4. 数据结构](#4-数据结构)
- [5. 分权 vs 功能权限 vs 共享/ACL](#5-分权-vs-功能权限-vs-共享acl)
- [6. 缓存与性能](#6-缓存与性能)
- [7. 已知问题与缺失](#7-已知问题与缺失)

---

## 1. 分权总体机制

### 1.1 开关存储与后台页面

| 项 | 内容 |
|---|---|
| **配置表** | `SystemSet`（单行记录，全局唯一） |
| **读取** | 存储过程 `SystemSet_Select`，`ManageDetachComInfo.setManageDetachComInfo()` L164-165 |
| **写入** | `com/engine/hrm/cmd/detachmanagerset/SaveDetachMSetCmd.java`：主 14 字段走 `SystemDMSet_Update`（L146-148，实现 `com/weaver/procedure/systemdmset/Systemdmset_update.java` L18），其余字段逐个 `update SystemSet set ... where 1=1`（L149-176） |
| **权限校验** | `HrmUserVarify.checkUserRight("SystemSetEdit:Edit", user)`（SaveDetachMSetCmd L66） |
| **后台页面** | `/api/hrm/detachmanagerset/*` → `com/engine/hrm/web/HrmDetachMSetAction.java` |
| **分权管理员** | `/api/hrm/modulemanagedetach/*` → `ModuleManagerDetachAction`（L39-158），Cmd 在 `com/engine/hrm/cmd/modulemanagedetach/` |
| **三员管理员** | `com/engine/hrm/cmd/detachsysadmin/` + `HrmDetachSysadminAction` |
| **应用分权** | `com/engine/hrm/cmd/appdetach/` + `weaver/hrm/appdetach/AppDetachComInfo.java` |
| **缓存清除** | `SaveDetachMSetCmd.java:345` → `ManageDetachComInfo.removeManageDetachCache()` |

### 1.2 `SystemSet` 中 `*detachable` 相关字段全集（19 组）

字段声明 `ManageDetachComInfo.java` L17-76，读取 L167-224，写入 `SaveDetachMSetCmd.java` L71-176。

| 开关字段 | 默认分部字段 | 模块 | `isUseXxxManageDetach()` |
|---|---|---|---|
| `detachable` | `dftsubcomid` | **管理分权总开关** | `isUseManageDetach()` L581 |
| `appdetachable` | (`appDetachDisableAll`) | 应用分权总开关 | `isUseAppDetach()` L538 |
| `hrmdetachable` | `hrmdftsubcomid` | 人力资源 | `isUseHrmManageDetach()` L595 |
| `wfdetachable` | `wfdftsubcomid` | 工作流程 | `isUseWfManageDetach()` L609 |
| **`docdetachable`** | **`docdftsubcomid`** | **知识/文档** | `isUseDocManageDetach()` L623 |
| `portaldetachable` | `portaldftsubcomid` | 门户 | L637 |
| `cptdetachable` | `cptdftsubcomid` | 资产 | L651 |
| `mtidetachable` | `mtidftsubcomid` | 会议 | L665 |
| `wcdetachable` | `wcdftsubcomid` | 微信 | L679 |
| `fmdetachable` | `fmdftsubcomid` | 表单建模 | L693 |
| `mmdetachable` | `mmdftsubcomid` | 移动建模 | L707 |
| `votingdetachable` | `votingdftsubcomid` | 调查 | L721 |
| `odocdetachable` | **`odocdetachsubcomid`（命名例外）** | 公文规范 | L763 |
| `blogdetachable` | `blogdftsubcomid` | 微博 | L749 |
| `executiondetachable` | `executiondftsubcomid` | 执行力 | L735 |
| `integrationdetachable` | `integrationdftsubcomid` | 集成中心 | L777 |
| `smsdetachable` | **`smssubcomid`（命名例外）** | 短信 | **无 isUse 方法** |
| `governdetachable` | `governdftsubcomid` | 督查督办 | L791 |
| `emaildetachable` | `emaildftsubcomid` | 邮件 | L805 |
| `coworkdetachable` | `coworkdftsubcomid` | 协作 | L819 |
| `carsdetachable` | `carsdftsubcomid` | 车辆 | **无 isUse 方法，ManageDetachComInfo 完全未声明** |
| `sanyuanable` | — | 三员分立（联动） | `HrmSanyuanAdminBiz.getSanyuanAble()` |

### 1.3 默认分部 `docdftsubcomid` 的三重作用

1. **开关生效的必要条件**：`isUseDocManageDetach()` L625 要求 `docdftsubcomid != null && !"" && !"0"`。只勾 `docdetachable=1` 而默认分部为 0/空 → **分权实际不生效（静默）**。
2. **新建对象的默认归属分部**：新建文档主目录/模板时作为 `subcompanyid` 默认值（前端取用 `com/engine/doc/cmd/multi/DocDetachableCmd.java` L26-30）。
3. **保存分权设置时历史数据兜底刷库**（`SaveDetachMSetCmd.java` L220-258）：把 `subcompanyid` 为空/0 的历史数据批量刷成默认值，涉及 `DocSecCategory`(L222-225)、`DocMould`(L228)、`DocMouldFile`(L233)、`DocFrontpage`(L238)、`WebMagazineType`(L243)、`OdocPrintMould`(L248)、`odocTypesetMould`(L253)。

---

## 2. 判定链路

### 2.1 三层判定

```
第1层：模块开关   ManageDetachComInfo.isUseXxxManageDetach()
        = "1".equals(detachable)             // 总开关
       && "1".equals(xxxdetachable)          // 模块开关
       && xxxdftsubcomid 非空/非 "0"          // 默认分部已设
                    ↓
第2层：权限串→模块归属  CheckSubCompanyRight.getDetachableByRight(rightStr)  L1573-1625
        SELECT righttype, detachable FROM SystemRights a, SystemRightDetail b
          WHERE a.id=b.rightid AND b.rightdetail='<rightStr>'
        detachable='1' 时按 righttype 分派到对应 isUseXxxManageDetach()
                    ↓
第3层：分部级操作级别  CheckSubCompanyRight.ChkComRightByUserRightCompanyId(uid, rightStr, subcompanyid)  L548
```

### 2.2 `getDetachableByRight()` 的 `righttype` → 模块分派表（L1594-1623）

| `SystemRights.righttype` | 实际调用 | 备注 |
|---|---|---|
| `1` | `isUseDocManageDetach()` | 文档 |
| `3` | `isUseHrmManageDetach()` | 人力资源 |
| `5` | `isUseWfManageDetach()` | 工作流 |
| `8` | `isUseCptManageDetach()` | 资产 |
| `9` | `isUsePortalManageDetach()` | 门户 |
| `16` | `isUseGovernManageDetach()` | **注释错写"执行力"，实为督办** |
| `20` | `isUseExecutionManageDetach()` | 执行力 |
| `21` | `isUseBlogManageDetach()` | 微博 |

> **未覆盖的 righttype**（公文、邮件、协作、短信、集成、会议、建模、移动建模、车辆、调查、微信）在 `tmpdetachable='1'` 时**不会**被改写，保留 L1576-1581 从 `SystemSet.detachable` 读到的**全局总开关值** → **只认总开关、不认模块开关**。

### 2.3 `ChkComRightByUserRightCompanyId` 返回值语义

`CheckSubCompanyRight.java` L36-39：
```java
public static int FORBIT = -1;   // 禁止
public static int READ   =  0;   // 只读
public static int EDIT   =  1;   // 编辑
public static int ALL    =  2;   // 完全操作
```

执行序列（L548-616）：
1. L550-551 `detachable = getDetachableByRight(rightStr)?1:0; if(detachable==0) return -1;` —— **未开分权直接返回 -1（禁止）**
2. L558-560 `userid==1`（sysadmin）或 `HrmSanyuanAdminBiz.canViewAllSubCompany` → 直接 `return 2`
3. L552 缓存 key = `userid + "&" + rightStr + "&" + subcompanyid`
4. L570-574 miss → 存储过程 `HrmRoleSR_SByURCId` 取 `rightlevel` 并写缓存
5. L578-614 `returnValue < 0` 且开启多账号显示 → 遍历子账号重算
6. L615 `staticobj.putObject("mapHrmRoleSR", mapHrmRoleSR)`

### 2.4 完整调用链示例（知识文档目录树）

```
com/api/doc/search/util/CategoryTree.java:272-274
  detach = mdci.isUseDocManageDetach();                    // 第1层
        ↓
CategoryTree.java:277-282  选 rightStr
  = "DocSecCategoryAdd:Add"(默认) / "DocShareRight:all" / "Docs:SetPopUp"
        ↓
CategoryTree.java:286-296
  SubCompanyComInfo.getRightSubCompany(uid, rightStr, -1)  // 有权限分部串
        ↓  SubCompanyComInfo.java:933-…
      L941-942 detachable = CheckSubCompanyRight.getDetachableByRight(rightStr)?1:0   // 第2层
      L947+    HrmRoleSR_SeByURId 取 rightlevel
        ↓
CategoryTree.java:306-316  逐根节点过滤
  companyid = 节点 DocSecCategory.subcompanyid
  if(!compids.contains(","+companyid+",")) continue;
  int operatelevel = ChkComRightByUserRightCompanyId(uid, rightStr, companyid);   // 第3层
  if(operatelevel <= 0) continue;                          // 只读/禁止 → 不渲染
```

另一条（文档回收站）：
```
com/api/doc/docRecycle/service/DocRecycleService.java:368-388
  isUseDocManageDetach()
  → HrmUserVarify.checkUserRight("DocumentRecycle:All", user)      // 功能权限先过
  → SecCategoryComInfo.getSubcompanyIdFQ(secid)                    // 取目录根分部
  → ChkComRightByUserRightCompanyId(uid, "DocumentRecycle:All", subcompanyid)
  → if(operatelevel < 1) return "没有权限"
```

---

## 3. 各模块接入点全清单

| 模块 | 是否支持 | 开关字段 | 判定/接入 | 数据载体 |
|---|---|---|---|---|
| **doc 知识/文档** | ✅ 最完整 | `docdetachable` / `docdftsubcomid` | `CategoryTree.java:39 DOC_RIGHT_STR="DocSecCategoryAdd:Add"`（过滤 L271-319）；`DocRecycleService:368-388`；`SearchSecMouldCmd`（L64-91、复选框 L160-176）；`DocDetachableCmd` | `DocSecCategory.subcompanyid`（`SecCategoryComInfo.java:37 @CacheColumn`；`getSubcompanyIdFQ` L434-474 递归四套 SQL） |
| **workflow 流程** | ✅ 接入最广 | `wfdetachable` / `wfdftsubcomid` | `ChkComRightByUserRightCompanyId` 调用点 **138+ 处**；`cmd/workflowPath/list/*`、`util/AuthorityUtil`、`FormAuthorityUtil`、`reportSetting/*`、`monitorSetting/*`、`customQuerySetting/*`、`codeMaintenance/*`、`workflowBatchSet/GetWfTreeDataCmd`；老代码 `WFMainManager`、`UserWFOperateLevel`、`Monitor`、`CustomQueryManager`、`ReportManager` | `workflow_base/workflow_bill/workflow_custom/Workflow_Report/workflow_monitor_bound.subcompanyid`（刷库 `SaveDetachMSetCmd` L191-219） |
| **odoc 公文规范** | ✅（2019+） | `odocdetachable` / **`odocdetachsubcomid`** | `isUseOdocManageDetach()` L763；`OdocUtil`/`OdocStandardUtil`/`OdocStandardFrontMethodUtil`（30 处）、`OdocNumberManageUtil`（14 处）；`entity/standard/*`；`cmd/standard/**`；`cmd/setTemplate/OdocMould/DocMouldInitCmd`（8 处） | 公文规范表 `subcompanyid`；`OdocPrintMould`/`odocTypesetMould` 刷库 L247-255 |
| **hrm 人力资源** | ✅ | `hrmdetachable` / `hrmdftsubcomid` | `isUseHrmManageDetach()` L595（righttype=3）；`HrmTransMethod`（4 处）；考勤 `GetDetachOperatelevelCmd`、`KQShiftManagementComInfo` | `hrm_att_proc_set.field004`、`hrm_schedule_shifts_set.field002`、`HrmContractTemplet.subcompanyid` |
| **cpt 资产** | ✅ | `cptdetachable`（righttype=8） | `GetTreeNodeDetachableCmd` → `CapitalAssortment.getDetachableTree` | — |
| **cowork 协作** | ✅（2020） | `coworkdetachable`/`coworkdftsubcomid` | `com/api/cowork/util/CoworkDetach.java`（`isDetach` L22、`getSubcompanyids` L27-44） | — |
| **email 邮件** | ✅（2020） | `emaildetachable`/`emaildftsubcomid` | `com/engine/email/util/EmailDetach.java`（L24-27、L36-53、L63-80） | — |
| **portal 门户** | ✅ | `portaldetachable`/`portaldftsubcomid` | `com/engine/portal/cmd/{mainpage,mainportal,mportalpage,portalmenu,floatarea,commonmenu,portalrcmenu}`、`PageUtil`（4 处）、`HomepageUtil` | 分权管理员权限 `{599,644,645,659}` |
| **fm 表单建模** | ✅ | `fmdetachable`/`fmdftsubcomid` | **`com/engine/cube/biz/DetachHelper.java`**（`modeDetachIsOpen` L25、`getUserDeatchOperateLevel` L38-61，rightStr=`FORMMODEAPP:ALL`） | 权限 `{1297,1796,1797}` |
| **mm 移动建模** | ✅ | `mmdetachable`/`mmdftsubcomid` | `SkinManager`（6 处）、`com/api/mobilemode/web/admin/DetachAction`；刷 `mobileAppBaseInfo.subcompanyid`（L266-272） | 权限 `{1727}` |
| **voting 调查** | ✅ | `votingdetachable` | `com/engine/voting/util/VotingDetach.java`（`RIGHT_STR="Voting:Maint"`） | — |
| **blog 微博 / execution 执行力** | ✅ | `blogdetachable`/`executiondetachable` | `weaver/hrm/moduledetach/BlogManageDetach.java`（L38-54、L96-124） | rightStr=`blog:newappSetting`、`Execution:PerformanceSet` |
| **govern 督办** | ✅ | `governdetachable` | `com/engine/govern/cmd/category/GetDetachableCmd`；刷 `govern_category`（L290-295）并联动 `SystemRights.detachable`（L296-300） | — |
| **integration 集成中心** | ✅ | `integrationdetachable` | `cmd/workflowaction/*`、`workflowtrigger/*`、`IntegratedMethod`、`DataShowSplitPageUtil` | — |
| **mti 会议** | ✅ | `mtidetachable` | `com/engine/meeting/cmd/workflow/GetDetachableCmd.java`（**复用 wf 分权**，取所有分部最大 operatelevel L48-108） | — |
| **sms 短信** | ⚠️ 部分 | `smsdetachable`/`smssubcomid` | **无 isUse 方法**；`SmsTemplateUtil`（2 处） | 保存时强制刷 `sms_set`/`sms_reminder_set` 为 **0**（L305-315，注释"默认总部 0"） |
| **wc 微信** | ⚠️ 极少 | `wcdetachable` | `isUseWcManageDetach()` L679，接入点极少 | — |
| **cars 车辆** | ⚠️ 绕过 | `carsdetachable`/`carsdftsubcomid` | **不走 ManageDetachComInfo**：`com/api/car/**` 直接 `select carsdetachable from SystemSet` | 分权管理员权限 `{463}` |
| **crm 客户** | ❌ 不支持 | 无 `crmdetachable` | 仅**日志中心**用总开关 `isUseManageDetach()` 做分部过滤（`CustomerServiceMaintImpl` L2799-2818、L3029-3403） | 客户主数据无分权 |
| **prj 项目** | ❌ 不支持 | 无 `prjdetachable` | — | — |
| **fna 财务** | ❌ 无独立开关 | 无 `fnadetachable` | `FnaDetachControlUtil` 借 `SysRoleSubcomRight` 自行实现 | — |

---

## 4. 数据结构

### 4.1 `SystemSet`
单行全局配置表，分权字段见 §1.2（19 组 + `sanyuanable`）。多处代码直接 `select detachable from SystemSet`（`CheckSubCompanyRight` L246/629/1576/1638、`SubCompanyComInfo` L897/936、`CarBaseService` L75）。

### 4.2 `SysRoleSubcomRight` —— **角色→分部→级别（分权核心授权表）**

| 列 | 含义 |
|---|---|
| `roleid` | 角色 ID（可为 `hrmroles_module` 中的负 ID 虚拟角色） |
| `subcompanyid` | 分部 ID |
| `rightlevel` | `-1` 禁止 / `0` 只读 / `1` 编辑 / `2` 完全控制 |

**写入**：`SaveMAdminCmd.java` L162-169/227-234/552-560/633-640 —— 先 `HrmRoleStrRight_Del` 再 `HrmRoleStrRight_ins`，**`rightlevel` 硬编码 `"2"`**（L101/161/226/551/632）；`AddSubComRightCmd.java:116` 同样硬编码 `"2"`。

**读取**：`ManageDetachComInfo.getDetachableSubcompanyIds(User)` L834-857 —— **4 段 UNION**：
1. `hrmrolemembers.resourcetype in (1,7,8)` 且 `resourceid=uid`（人）
2. `resourcetype=2`（分部）+ 安全级别区间
3. `resourcetype=3`（部门）+ 安全级别区间
4. `resourcetype=5`（岗位）+ `jobtitlelevel` 1/2/3

相关存储过程：`HrmRoleSR_SeByURId`、`HrmRoleSR_SByURCId`、`HrmRoleStrRight_ins/_Del`、`Hrmrsrpath_sebyurid`、`Hrmeditrightpath_sebyurid`。

### 4.3 `SystemRights` / `SystemRightDetail` / `SystemRightRoles`

| 表 | 关键列 | 用途 |
|---|---|---|
| `SystemRights` | `id`、`rightname`、`righttype`（1文档/3人力/5流程/8资产/9门户/16督办/20执行力/21微博）、`detachable` | 权限定义 |
| `SystemRightDetail` | `rightid`、`rightdetail`（权限串如 `DocSecCategoryAdd:Add`） | 权限串 → 权限 ID |
| `SystemRightRoles` | `rightid`、`roleid`、`roleLevel` | 角色→功能权限 |
| `HrmRoleMembers` | `roleid`、`resourceid`、`rolelevel`、`resourcetype`、`seclevelfrom/to`、`jobtitlelevel`、`subdepid` | 角色成员（分权 UNION 的 4 种来源） |

### 4.4 `hrmroles_module`（分权管理员"虚拟角色"）
负 ID 角色表。`SaveMAdminCmd.java` L506-521：`select Min(id) from hrmroles_module` 后 `minID-1`，角色名用标签 129309「模块管理分权虚拟角色」；成员写入 `HrmRoleMembers_Insert`+`HrmRoleMembersShare`，`level="2"`。

### 4.5 `HrmResourceManager`（三员管理员）
`id`、`loginid`、`password`、`sanyuanType`（1系统管理员/2安全保密员/3安全审计员）、`creator`、`subcompanyids`、`salt`。
`HrmSanyuanAdminBiz.java`：`getSanyuanAble()` L23-38（要求 `detachable=1 && sanyuanable=1`）、`canViewAllSubCompany(uid, rightStr)` L263-275（**要求 `creator=1`**）。

### 4.6 `SysSubComRight` —— **不存在**（全仓库 0 命中）。分权的"角色分部权限"只用 `SysRoleSubcomRight` 一张表。

---

## 5. 分权 vs 功能权限 vs 共享/ACL

| 维度 | 分权（ManageDetach） | 功能权限（HrmUserVarify） | 共享/ACL（MultiAclManager / DocShare） |
|---|---|---|---|
| **回答的问题** | "这个管理员能管**哪些分部**的后台设置/数据" | "这个人能不能**用这个功能**" | "这个人能不能**看/操作这条具体数据**" |
| **粒度** | 分部 × 模块 × 权限串 | 权限串（rightdetail） | 单条记录 / 单个目录 |
| **存储** | `SystemSet.*detachable` + `SysRoleSubcomRight` | `SystemRights`/`SystemRightDetail`/`SystemRightRoles` | `DirAccessControlList`、`DocShare`/`ShareinnerDoc` |
| **核心类** | `ManageDetachComInfo`、`CheckSubCompanyRight` | `HrmUserVarify.checkUserRight` → `CheckUserRight` | `MultiAclManager`、`AclManager`、`DocViewer`、`DocShare` |
| **返回值** | `-1/0/1/2` | boolean | boolean |
| **典型顺序** | ②（在功能权限之后） | ① | ③ |

**协作方式是"串联短路"**（`DocRecycleService.java` L371-388）：
```java
if(!HrmUserVarify.checkUserRight("DocumentRecycle:All", user)) return 无权限;   // ①功能权限
else if(isUseDocManageDetach()){                                                  // ②分权开关
    int operatelevel = ChkComRightByUserRightCompanyId(uid,"DocumentRecycle:All",subcompanyid);
    if(operatelevel < 1) return 无权限;                                            // ②分部级别
}
```

另一范式（`DetachHelper.getUserDeatchOperateLevel` L50-57）：
```java
if(modeDetachIsOpen()) operatelevel = CheckSubCompanyRight.ChkComRight...(…);   // 分权口径
else if(HrmUserVarify.checkUserRight(rightStr, user)) operatelevel = 2;         // 未开分权退化为功能权限
```

**关键边界规则**：
- **未开分权时** `ChkComRightByUserRightCompanyId` 返回 `-1`（L551）——**不能**当作"无权限"，必须自行判断"未开分权 → 只看功能权限"。
- **开了分权但角色没配分部**（`SysRoleSubcomRight` 无记录）→ `getRightSubCompany` 返回空 → 各模块 `and 1=2`/`continue`/`-1`，即"管理员什么都看不到"。
- 分权**只约束后台管理与列表可见范围**，**不约束文档正文的查看权限**——正文查看仍由 `MultiAclManager.hasPermission()`（`Doc_DirAcl_CheckPermission`/`Ex2` 存储过程）与 `DocShare` 决定。
- `MultiAclManager` 里 `subcompanyid` 只是 **ACL 授权项的一个维度**（`grantDirPermission6` → `permissiontype=6`），与分权的 `SysRoleSubcomRight` **完全无关**；唯一耦合点是 `getPermittedTree` 的 `hasRightSub` 由调用方从分权获取后注入（L106/680-717）。

---

## 6. 缓存与性能

### 6.1 `ManageDetachComInfo`
```java
// L82-85 构造即加载
public ManageDetachComInfo() { staticobj = StaticObj.getInstance(); getManageDetachComInfo(); }
// L87-153 synchronized(lock) 双重读
// L155-228 rt.executeProc("SystemSet_Select","") 一次全字段读出
// L527-530 removeManageDetachCache()
```
| 项 | 内容 |
|---|---|
| 容器 | `weaver.general.StaticObj` → 单例 + `ConcurrentHashMap`（L43/77），二级 `Hashtable`（L144-168） |
| key | 一级 `"ManageDetachComInfo"`，二级为字段名 |
| 失效 | **仅** `removeManageDetachCache()`（`SaveDetachMSetCmd:345`、`SaveAppDetachSetCmd`） |

**缺陷**：
1. `setManageDetachComInfo()` 只写 StaticObj（L167-224），**不写实例字段**；实例字段在 L91-149 读 → `SystemSet` 无记录时会拿到 null。
2. L150 `//if(ids==null)setManageDetachComInfo();` 被注释。
3. **无 TTL、无集群广播**（对比 `StaticObj` 对 `fmbrowser` 有集群同步注释 L401-402）。
4. 在 `CheckSubCompanyRight` L49 是**实例字段**，每次 `new CheckSubCompanyRight()` 都会 `new ManageDetachComInfo()`。
5. `getDetachableSubcompanyIds()`/`getDetachSubcomAndLevel()` **完全不缓存**，每次 4 段 UNION 全表扫描。

### 6.2 `CheckSubCompanyRight`
| 项 | 内容 |
|---|---|
| 缓存字段 | `public static Map<String,Integer> mapHrmRoleSR`（L547，**static**） |
| 容器 | `StaticObj` key `"mapHrmRoleSR"`，值 `ConcurrentHashMap`（L553-557） |
| key | `userid + "&" + rightStr + "&" + subcompanyid` |
| 值 | `rightlevel`，**包括负值也缓存**（L574 无条件 put） |
| 失效 | `CheckUserRight.removeRoleRightdetailCache()` L332-335 / `removeMemberRoleCache()` L337-340 → `staticobj.removeObject("mapHrmRoleSR")`；调用点 `SaveMAdminCmd` L192/257/391/584、`AddSubComRightCmd` L140-142 |
| 旁路 | `userid==1` 或 `canViewAllSubCompany` → 不查不缓存直接 2 |

### 6.3 性能热点
1. **`getDetachableByRight()` 每次调用 2 条 SQL 且无任何缓存**，而它在 `getSubComByUserRightId`(L251)、`getSubComByUserEditRightId`(L634)、`geDeptPathByUserRightId`(L1644)、`SubCompanyComInfo.getRightSubCompany`(L903/942) 中每次都调用。
2. L246-250/629-634/1638-1644 先 `select detachable from SystemSet` 再被 `getDetachableByRight` 结果**覆盖** → 纯浪费 SQL（死代码）。
3. 列表渲染每行调一次 `ChkComRightByUserRightCompanyId`（`SearchSecMouldCmd.showCheckBox` L160-176、`SplitPageTransmethod` 14 处、`MeetingTransMethod` 14 处）→ N+1。
4. **`SaveDetachMSetCmd` 保存是重量级操作**：L179-334 对十余张表无条件 `update`（`where 1=1`），大表上会长时间锁表。

---

## 7. 已知问题与缺失

### 🔴 P0 — 权限绕过 / 越权

1. **`DocMultiOperaAction.java` L197-203 权限判断被注释**：
   ```java
   boolean canEdit = HrmUserVarify.checkUserRight("DocumentRecycle:All", user);
   //            if(canEdit){
       apidatas = getService(request, response).getRecycleTable(request);
   //            }else { apidatas.put("api_status", false); ... }
   ```
   `/recycle/table` **完全不校验功能权限与分权**，任意登录用户可拉取回收站列表；而同文件 `getRecycleTreeData` 走 `DocRecycleService.getTreehide` 有完整校验 → **前后端校验不一致**。

2. **`DetachHelper.getUserDeatchOperateLevel()` 缓存 key 漏了 `rightStr`**（`com/engine/cube/biz/DetachHelper.java` L38-61）：
   ```java
   String key = user.getUID()+"_"+subCompanyId;   // 无 rightStr
   ```
   同一用户+分部下，先以高权限串判定缓存 `2`，后续用低权限串会直接复用 → **建模模块跨权限串越权**（TTL 60 秒）。

3. **车辆分权绕过 `SysRoleSubcomRight` 校验**：`CarSearchService.java` L65-77、`CarViewService.java` L236-248/857-868 仅 `select carsdetachable from SystemSet`，`detachable==1` 时直接用 `request.getParameter("subCompanyId")` 拼 SQL，**不与用户有权限分部比对** → 改一个请求参数即可跨分部查看车辆。

4. **`carsdetachable` 判定不完整**：`ManageDetachComInfo` 无该字段/getter/判定方法，`com/api/car/**` 直接查表 → **不校验全局 `detachable` 总开关**。

### 🟠 P1 — 逻辑 Bug / 判定不一致

5. **`CheckSubCompanyRight` 局部变量与成员字段 `detachable` 混淆 → 死代码**：L246-251/L629-634/L1638-1644 用局部 `detachable`，但 L520/L676 回退分支用**成员字段** `this.detachable`（默认 0）→ **该回退分支在这三个方法里永远不执行**，分部/部门兜底逻辑失效。

6. **`getDetachableByRight()` righttype 覆盖不全**：只处理 `1/3/5/8/9/16/20/21`；公文、邮件、协作、短信、集成、会议、建模、移动建模、车辆、调查、微信不在表内 → **退化成只看全局总开关，忽略模块开关**（如 `emaildetachable=0` 仍判定分权开启）。

7. **`getDetachableByRight()` 注释错误**：L1616-1618 `righttype=="16"` 注释写「执行力」，实际调用 `isUseGovernManageDetach()`（督办）；`20` 才是执行力。

8. **`BlogManageDetach.getBySubcomId()` 循环覆盖 Bug**（L42-49）：
   ```java
   for (int j = 0; j < result1.length; j++) {
       retstaus = subcompanyid.equals(result1[j]);   // 无 break / 无 ||，被最后元素覆盖
   }
   ```
   除最后一个分部外全部误判为无权限。

9. **`ChkComRightByUserRightCompanyId` 未开分权直接返回 `-1`**（L551）：调用方若不做"未开分权 → 只看功能权限"分支，会**误把正常用户判为无权限**。`MeetingTransMethod`、`SplitPageTransmethod` 等 14 处调用点需逐一核查。

10. **`mapHrmRoleSR` 静态字段并发问题**（L547/553-557/574/615）：`public static Map` 被所有实例共享，并发下 `getObject` 与 `putObject` 之间无同步（与 `getManageDetachComInfo()` 的 `synchronized(lock)` 形成对比）→ 可能丢失写入。

11. **短信分权保存时强制刷 `subcompanyid=0`**（L305-315）：与"默认分部"语义相反，注释「咨询了领导，这里刷数据功能不需要了。默认总部 0」。

### 🟡 P2 — 缺失 / 不一致 / 硬编码

12. **`smsdetachable` 与 `carsdetachable` 缺 `isUseXxxManageDetach()`**：19 个模块有统一判定方法，这两个没有，各自裸查表。
13. **默认分部字段命名不一致**：`odocdetachsubcomid`、`smssubcomid` vs 其余全部 `xxxdftsubcomid`。
14. **`SysRoleSubcomRight.rightlevel` 实际只会有 `2`**：`SaveMAdminCmd` 与 `AddSubComRightCmd` 硬编码 `"2"`，UI 也没有级别选项 → `ChkComRightByUserRightCompanyId` 定义的 `-1/0/1` 三档**没有产生入口**，"只读/编辑"级别形同虚设。
15. **分权管理员模块→权限 ID 全部硬编码**（`SaveMAdminCmd` L296-345、L406-455、L726-763、L811-860）：

    | 模块参数 | 权限 ID 数组 |
    |---|---|
    | `chkHrm` | 18,19,22,25,30,35,37,109,381,395,587,588,657,658,660,661,664,787,788,789,906,907,908,1827,1849,1850,1851,1918,1919 |
    | `chkWf` | 91,300,591,719,771,837,838,1749,1836 |
    | `chkDoc` | 1,4,8,10,11,16,453,690,1535,2086 |
    | `chkPortal` | 599,644,645,659 |
    | `chkCpt` | 439,137 |
    | `chkMti` | 200,350 |
    | `chkfm` | 1297,1796,1797 |
    | `chkmm` | 1727 |
    | `chkcars` | 463 |
    | `chkOdoc` | 2471,2610,2612,2614,2615,2632,2616 |

    （`381` 重复出现 L296；`sql3` 的 `in(...)` 列表与 `rightidList` 不完全一致，含 `2611`/`2613` 但写入列表没有）

16. **`appDetachable` 未纳入 `SystemDMSet_Update`**：保存方法里未见对应 `update SystemSet set appdetachable=...` → **应用分权开关可能保存不落库**（需复核 `SaveAppDetachSetCmd`）。
17. **sysadmin 硬编码口径不一**：`CheckSubCompanyRight` L559/592 用 `userid==1`；`DocRecycleService` L371 用 `loginid.equalsIgnoreCase("sysadmin")`；`BlogManageDetach` L98 用 `isAdmin() && UID==1`。
18. **大量注释掉的代码**：`CategoryTree` L120-130/L837-844、`SaveDetachMSetCmd` L316-320、`SaveMAdminCmd` L699-724/L131-132。
19. **CRM / PRJ / FNA 无模块级分权**：客户/项目/财务主数据无分权；FNA 用 `FnaDetachControlUtil` 自行实现，L55 无匹配时塞 `-1` 兜底。
20. **`SaveDetachMSetCmd` 保存为重量级操作**：事务外对十余张表无条件 `update`，L303 注释显示该段行为已与需求脱节。

---

## 附：核心文件清单

| 文件 | 角色 |
|---|---|
| `weaver/hrm/moduledetach/ManageDetachComInfo.java` | 分权开关读取/缓存、19 个 `isUseXxxManageDetach()`、分部权限 UNION 查询 |
| `weaver/systeminfo/systemright/CheckSubCompanyRight.java` | `getDetachableByRight()` L1573、`ChkComRightByUserRightCompanyId()` L548、`getSubComByUserRightId()` L242 |
| `weaver/systeminfo/systemright/CheckUserRight.java` | 功能权限缓存与失效 L332-340 |
| `weaver/hrm/company/SubCompanyComInfo.java` | `getRightSubCompany(uid,rightStr)` L892/L933 |
| `com/engine/hrm/cmd/detachmanagerset/SaveDetachMSetCmd.java` | 分权设置保存 + 默认分部刷库 |
| `com/engine/hrm/cmd/modulemanagedetach/SaveMAdminCmd.java` | 分权管理员：`SysRoleSubcomRight` + `SystemRightRoles` + `hrmroles_module` |
| `com/engine/hrm/biz/HrmSanyuanAdminBiz.java` | 三员管理员 |
| `com/api/doc/search/util/CategoryTree.java` | 文档目录树分权过滤 |
| `com/api/doc/docRecycle/service/DocRecycleService.java` | 回收站分权 L368-388 |
| `com/engine/cube/biz/DetachHelper.java` | 建模分权（含 60s 缓存缺陷） |
| `weaver/hrm/moduledetach/BlogManageDetach.java` | 微博/执行力分权（含循环覆盖 Bug） |
| `weaver/docs/category/security/MultiAclManager.java` | 文档记录级 ACL（与分权解耦） |
