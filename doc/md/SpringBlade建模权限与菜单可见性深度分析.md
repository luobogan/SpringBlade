# SpringBlade 建模权限与菜单可见性深度分析

> 分析对象：
>
> - `D:\Weaver2020\ecology`：Ecology 部署运行包，主要看 JSP、运行脚本、表结构 SQL。
> - `E:\project\ecology\ecology`：Ecology Java 源码，主要看后端权限与菜单加载逻辑。
> - `D:\project\springbladeandreact\SpringBlade`：当前 SpringBlade 建模实现。
>
> 核心结论：
>
> ```text
> SpringBlade 建模模块、表单、字段、数据权限是一套；
> 菜单可见性是另一套；
> 人员级建模权限不会自动生成人员菜单；
> 用户必须同时满足建模权限和菜单可见性。
> ```

---

## 1. 为什么要把“建模权限”和“菜单可见性”拆开看

很多问题看起来是“权限没生效”，实际是两个独立系统没有同时满足。

以当前问题为例：

```text
模块ID：2104243330272460802
模块权限：人员 lh，已配置“可见”
结果：lh 登录后在菜单里看不到这个模块
```

这不是建模权限没有写入，而是：

```text
建模权限：已写入 moderightinfo（人员 lh）
菜单权限：lh 没有角色，菜单没有授权给 lh
```

在 Ecology 和当前 SpringBlade 中，都是两条链路：

```text
链路 A：建模模块/字段/数据权限
  → 决定用户进入页面后能看到什么数据、能做什么操作

链路 B：菜单可见性
  → 决定用户能不能在左侧/顶部菜单里看到这个入口
```

两条链路缺一不可：

```text
能看到菜单 + 有建模权限 → 正常使用
能看到菜单 + 无建模权限 → 进去后无数据或无操作
有建模权限 + 看不到菜单 → 用户根本进不去
```

---

## 2. Ecology 是怎么处理的

### 2.1 建模本体：模块、表单、字段、数据权限

Ecology 的建模主要由这些表承载：

| 类型 | 表 | 说明 |
|---|---|---|
| 表单定义 | `workflow_bill` | 表单主定义、物理表名、页面路径 |
| 字段定义 | `workflow_billfield` | 表单字段、`dsporder`、`viewtype`、`detailtable` |
| 模块定义 | `modeinfo` | 建模模块，`formid → workflow_bill.id` |
| 模块权限 | `moderightinfo` | 可见/新建/编辑/删除/详情等模块操作权限 |
| 字段权限 | `ModeFieldAuthorize` | 字段级显示/编辑控制 |
| 数据权限 | `modeDataShare_<modeid>` | 每个模块一张动态数据共享表 |
| 矩阵权限 | `moderightinfo_matrix` | 矩阵主体权限 |

关键证据：

```text
D:\Weaver2020\ecology\data\Oracle\1table.sql:7458-7467        workflow_bill
D:\Weaver2020\ecology\data\Oracle\1table.sql:7470-7479        workflow_billfield
D:\Weaver2020\ecology\data\Oracle\sql2012080803.sql:32-43     modeinfo
D:\Weaver2020\ecology\data\Oracle\sql2012080803.sql:278-286   moderightinfo
D:\Weaver2020\ecology\data\Oracle\sql201502030403.sql:1-9     ModeFieldAuthorize
D:\Weaver2020\ecology\data\Oracle\sql201708030503.sql:1-12    moderightinfo_matrix
```

模块创建流程：

```text
formmode/setup/ModeOperation.jsp
  → ModeSetUtil.addMode()
  → 写 modeinfo
  → 创建 modeDataShare_<modeid>
  → 初始化权限
```

证据：

```text
E:\project\ecology\ecology\src\weaver\formmode\setup\ModeSetUtil.java:393-434
D:\Weaver2020\ecology\formmode\setup\ModeOperation.jsp:96-140
```

### 2.2 Ecology 的建模权限主体

Ecology 建模权限 `moderightinfo.sharetype` 支持：

```text
1 = 人员
2 = 分部
3 = 部门
4 = 角色
5 = 所有人
6 = 岗位
7 = 矩阵
```

证据：

```text
D:\Weaver2020\ecology\formmode\setup\ModeShareAdd.jsp:117-132
E:\project\ecology\ecology\src\weaver\formmode\search\FormModeRightInfo.java:35-37
```

注意：这里的“人员”是建模数据权限主体：

```text
moderightinfo.sharetype = 1
moderightinfo.relatedid = userid
```

它只解决：

```text
这个用户能不能看到/新建/编辑建模数据
```

不解决：

```text
这个用户能不能在菜单里看到入口
```

### 2.3 Ecology 的菜单体系是另一套

Ecology 菜单不是简单一张表，常见包括：

| 菜单表 | 说明 |
|---|---|
| `LeftMenuInfo` / `MainMenuInfo` | 左菜单/顶部菜单定义，含 `linkAddress` |
| `LeftMenuConfig` / `MainMenuConfig` | 用户/资源维度的菜单配置 |
| `menucenter` / `menucustom` | 自定义菜单 |
| `menushareinfo` | 菜单共享授权，含角色、人员、部门、分部等 |

表结构证据：

```text
D:\Weaver2020\ecology\data\Oracle\sql20050417.sql:2-71
D:\Weaver2020\ecology\data\Oracle\sql2009061903.sql:79-104
D:\Weaver2020\ecology\data\Oracle\sql201410130903.sql:439-449
```

### 2.4 Ecology 建模菜单通常是手工创建

普通建模模块创建时，不会自动写菜单表。

菜单创建流程：

```text
formmode/setup/modelAdd.jsp
  → createMenuNew()
  → modelMenuAdd.jsp
  → CreateMenuNew.jsp
  → MenuMaint.java
```

证据：

```text
D:\Weaver2020\ecology\formmode\setup\modelAdd.jsp:229-230
D:\Weaver2020\ecology\formmode\setup\modelAdd.jsp:894-908
D:\Weaver2020\ecology\formmode\setup\modelMenuAdd.jsp:162-166
D:\Weaver2020\ecology\formmode\menu\CreateMenuNew.jsp:13-20
E:\project\ecology\ecology\src\weaver\systeminfo\menuconfig\MenuMaint.java:85
```

常见建模菜单 URL：

```text
/formmode/view/AddFormMode.jsp?modeId=...&formId=...&type=1
/formmode/search/CustomSearchBySimple.jsp?customid=...
/formmode/search/CustomSearchByAdvanced.jsp?customid=...
/formmode/tree/ViewCustomTree.jsp?id=...
/formmode/report/ReportCondition.jsp?id=...
```

也就是说 Ecology 是：

```text
先建建模模块
再建菜单入口
再授权菜单
```

而不是：

```text
建模块时自动把权限主体变成菜单授权
```

### 2.5 Ecology 用户登录时如何加载菜单

前端菜单加载链路：

```text
GetFrontEndMenuCmd
  → MenuUtil.getAllMenuRsWithShareRight(...)
```

证据：

```text
E:\project\ecology\ecology\src\com\engine\portal\cmd\menu\GetFrontEndMenuCmd.java:52-55
E:\project\ecology\ecology\src\com\engine\portal\cmd\menu\GetFrontEndMenuCmd.java:78-80
E:\project\ecology\ecology\src\weaver\systeminfo\menuconfig\MenuUtil.java:310
```

E9 菜单共享主体编码：

```text
1 = 部门
2 = 角色
3 = 安全级别
5 = 人员
6 = 分部
7 = 岗位
```

证据：

```text
E:\project\ecology\ecology\src\weaver\systeminfo\menuconfig\MenuUtil.java:461-503
```

菜单“人员”判断：

```text
menushareinfo.sharetype = 5
menushareinfo.sharevalue = userid
```

证据：

```text
E:\project\ecology\ecology\src\weaver\systeminfo\menuconfig\MenuUtil.java:475-477
```

### 2.6 Ecology 中两类“人员”编码不同

这是最容易混淆的地方：

| 系统 | 表 | 人员编码 |
|---|---|---|
| 建模权限 | `moderightinfo` | `sharetype=1, relatedid=userid` |
| 菜单授权 | `menushareinfo` | `sharetype=5, sharevalue=userid` |

这两套编码、两张表、两套判断完全不同。

源码中没有发现把建模人员权限自动转换成菜单人员授权的桥接逻辑。

所以 Ecology 的结论是：

```text
建模人员权限 ≠ 菜单人员授权
```

用户要看到建模入口，需要：

```text
1. 有建模权限
2. 菜单对该用户/角色/部门/分部等可见
```

---

## 3. SpringBlade 当前是怎么处理的

### 3.1 建模权限体系

当前 SpringBlade 的建模权限主体定义在：

```text
blade-service-api/blade-formmode-api/.../entity/ModeRightInfo.java
```

关键字段：

```text
modeid      模块ID
righttype   权限类型：1可见 2新建 3编辑 4删除 5详情 6导入 7导出
objtype     授权对象类型：1角色 2部门 3岗位 4人员 5分部
objid       授权对象ID
includesub  是否含子部门
showlevel   密级下限
showlevel2  密级上限
```

证据：

```text
blade-service-api/blade-formmode-api/src/main/java/org/springblade/formmode/entity/ModeRightInfo.java:25-57
```

运行期判断支持人员：

```java
case 4: // 人员
    if (uid != null && uid.equals(objId)) { ... }
```

证据：

```text
blade-service/blade-formmode/.../FormRightServiceImpl.java:270-275
```

所以：

```text
模块权限里配置人员 lh
→ checkRight / listUserRightTypes 是能识别的
→ 用户进入页面后的操作权限可以生效
```

### 3.2 字段权限体系

字段权限当前有三部分：

```text
modefieldauthorize                   字段权限明细
mode_field_auth_group                授权对象组
mode_field_auth_subject              组成员
```

运行期：

```text
listFieldPerm(modeId)
  → 解析当前用户主体
  → 命中授权对象组
  → 返回字段 visible/editable
```

相关证据：

```text
FormRightServiceImpl.java:571-605    resolveMatchedFieldGroupIds
FormRightServiceImpl.java:689-765    listFieldPerm
```

### 3.3 数据权限体系

数据范围当前主要落在：

```text
modedatasharerule
```

由 `DataScopeService` 和 `ModeDataShareRuleServiceImpl` 处理。

这条链路解决：

```text
用户能看到哪些数据行
```

### 3.4 菜单体系

SpringBlade 系统菜单仍是标准 Blade 体系：

```text
blade_menu
blade_role_menu
blade_role_menu_name
```

菜单路由加载入口：

```java
public List<MenuVO> routes(String roleId, Long topMenuId) {
    ...
    List<Menu> roleIdMenus = baseMapper.roleMenuByRoleId(Func.toLongList(roleId));
    ...
}
```

证据：

```text
blade-service/blade-system/.../MenuServiceImpl.java:87-148
```

这说明当前菜单可见性主要由：

```text
用户 token 中的 roleId → blade_role_menu → blade_menu
```

决定，而不是由：

```text
moderightinfo.objtype/objid
```

决定。

### 3.5 当前 SpringBlade 的“一表单一菜单”逻辑

保存权限时，`saveAll()` 会调用：

```java
ensureFormMenuAndGrant(modeId, batch);
```

证据：

```text
FormRightServiceImpl.java:1171-1180
```

该方法会：

```text
1. 创建/更新菜单
   code = formdata_view_{formId}
   path = /formmode/formview/{formId}

2. 从模块权限中提取角色
   objtype == 1

3. 重置 blade_role_menu

4. 写角色级菜单显示名
```

证据：

```text
FormRightServiceImpl.java:1200-1291
```

关键代码语义：

```java
// 3) 目标角色（objtype=1）
if (r.getObjtype() != 1) continue;
...
// 4) 重置该菜单的角色授权
DELETE FROM blade_role_menu WHERE menu_id=?
INSERT INTO blade_role_menu ...
```

也就是说：

```text
当前自动菜单授权只识别 objtype=1（角色）
```

不会识别：

```text
objtype=4（人员）
objtype=2（部门）
objtype=3（岗位）
objtype=5（分部）
```

### 3.6 前端操作按钮又是第三层判断

`FormView.tsx` 中操作按钮是否显示：

```ts
if (!fieldConfigured) return false;
if (buttons.some(btn => btn.code === code)) return true;
return userRightTypes.includes(rightType);
```

证据：

```text
ant-design-pro/src/pages/FormMode/FormView/FormView.tsx:65-82
```

这里有两层条件：

```text
字段配置完成
+
菜单按钮码 OR 建模权限码
```

所以完整链路是：

```text
菜单能看到页面
  → FormView 加载
  → 字段配置完成
  → hasPermission 判断按钮
  → userRightTypes 来自建模权限
```

---

## 4. 用当前 lh 问题还原整条链路

当前数据库情况：

```text
lh:
  id = 6
  role_id = NULL

模块权限:
  modeid = 2104243330272460802
  righttype = 1
  objtype = 4
  objid = 6

菜单:
  code = formdata_view_2104243330272460802

角色菜单授权:
  只有流程管理员、超级管理员授权了通用表单数据菜单
```

推导：

### 第一层：建模权限

```text
objtype=4, objid=6
命中 lh
```

所以：

```text
checkRight(... uid=6) = true
listUserRightTypes(...) 包含 1（可见）
```

这层没问题。

### 第二层：菜单可见性

```text
lh.role_id = NULL
MenuServiceImpl.routes(roleId) 只按角色查 blade_role_menu
```

所以：

```text
lh 没有角色
→ 查不到该菜单
→ 侧边栏没有入口
```

### 第三层：前端按钮

即使直接输入 URL 进入：

```text
hasPermission 还可能因为按钮码/权限加载链路不完整而表现异常
```

但根因仍然是：

```text
没有菜单入口
```

---

## 5. SpringBlade 与 Ecology 的本质差异

| 维度 | Ecology | SpringBlade 当前 |
|---|---|---|
| 建模模块 | `modeinfo` | `modeinfo` |
| 表单 | `workflow_bill` | `workflow_bill` |
| 字段 | `workflow_billfield` | `workflow_billfield` |
| 模块权限 | `moderightinfo` | `moderightinfo` |
| 人员编码 | `sharetype=1` | `objtype=4` |
| 数据权限 | `modeDataShare_<modeid>` | `modedatasharerule` |
| 菜单定义 | `LeftMenuInfo/MainMenuInfo` | `blade_menu` |
| 菜单授权 | `menushareinfo` | `blade_role_menu` |
| 菜单主体 | 角色/人员/部门/分部/岗位/安全级别 | 当前只有角色 |
| 建模人员权限是否自动授权菜单 | 否 | 否 |
| 用户是否必须同时满足两层 | 是 | 是 |

结论：

```text
当前 SpringBlade 比 Ecology 更简化：
菜单授权只保留角色维度；
但建模权限仍支持多主体；
因此人员级建模权限天然无法转化为菜单入口。
```

---

## 6. 可选改造方案

### 方案 A：不改系统，按角色授权菜单

这是当前最小成本方案。

步骤：

```text
1. 给 lh 配角色
2. 给该角色授权表单数据菜单
3. 模块权限可继续配置人员 lh
```

优点：

```text
不改代码
不改表
立即生效
```

缺点：

```text
人员级权限仍然不能单独控制菜单
每个人都要有角色
```

适合当前快速解决问题。

---

### 方案 B：Ecosystem-like 独立菜单共享表

这是最接近 Ecology 的完整方案。

新增或复用一张菜单共享表：

```text
form_menu_share
  menu_id
  share_type
  share_id
```

其中：

```text
share_type = 1 角色
share_type = 2 人员
share_type = 3 部门
share_type = 4 分部
share_type = 5 岗位
share_type = 6 所有人
```

保存建模权限时：

```text
objtype=1 → share_type=角色
objtype=4 → share_type=人员
objtype=2 → share_type=部门
objtype=3 → share_type=岗位
objtype=5 → share_type=分部
```

加载路由时：

```text
roleId 命中的菜单
+
userId 命中的菜单
+
deptId 命中的菜单
+
subcompanyId 命中的菜单
+
postId 命中的菜单
```

全部 union。

优点：

```text
完整对齐 Ecology 思路
菜单和建模权限可以保持主体一致
人员、部门、岗位、分部都能单独控制菜单
```

缺点：

```text
要改 MenuServiceImpl.routes
影响系统菜单主链路，需要谨慎回归
```

---

### 方案 C：复用现有 PortalMenuShare

SpringBlade 门户模块已经有类似能力：

```text
PortalMenuShare
  share_type: 全员 / 部门 / 个人 / 角色 / 分部
```

证据：

```text
blade-service/blade-portal/.../entity/PortalMenuShare.java:21-46
blade-service/blade-portal/constant/PortalConstant.java:72-90
blade-service/blade-portal/.../PortalMenuServiceImpl.java:654-688
```

`PortalMenuServiceImpl.hitShare()` 已经支持：

```text
SHARE_DEPT
SHARE_USER
SHARE_ROLE
SHARE_BRANCH
```

可以考虑让建模菜单走门户菜单共享体系，而不是再新建一套表。

优点：

```text
复用已有通用能力
符合用户之前强调的“通用功能统一调用，不要无脑自写”
```

缺点：

```text
需要确认 portal 菜单与主系统 routes 是否同一套展示链路
如果不是同一套，仍要改主菜单加载逻辑
```

---

## 7. 推荐结论

### 对当前问题

lh 看不到模块的直接原因：

```text
lh 没有角色；
表单数据菜单只授权给角色；
人员级建模权限不会自动授权菜单。
```

当前最稳的处理：

```text
给 lh 配角色
给该角色授权菜单
```

### 对产品化改造

建议按 Ecology 的两层模型补齐：

```text
建模权限层：继续用 moderightinfo / modefieldauthorize / modedatasharerule
菜单可见层：新增或复用多主体菜单共享表
```

不要继续在 `ensureFormMenuAndGrant()` 里只处理 `objtype=1` 后再硬塞其它主体到 `blade_role_menu`。

原因：

```text
blade_role_menu 只能表达角色 → 菜单
无法表达人员/部门/岗位/分部 → 菜单
硬转会造成权限语义混乱
```

### 最终模型

```text
用户看到菜单
  = 菜单存在
  AND 菜单可见性命中（角色/人员/部门/分部/岗位等）

用户能操作
  = 字段配置完成
  AND 建模模块权限命中

用户能看到数据
  = 数据范围规则命中
```

这三层必须分开判断，不能互相替代。

---

## 8. 一句话总结

Ecology 和当前 SpringBlade 都遵循同一个本质：

```text
建模权限决定“进去能做什么”，
菜单权限决定“看不看得见入口”。
```

人员级建模权限不会自动生成菜单。  
要让人级权限用户看到菜单，必须把菜单可见性也扩展到人员维度，或先给用户配角色。