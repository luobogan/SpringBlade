# 泛微 E-cology「后台配置 → 前端门户」生成机制分析

> 分析对象：
> - 后端 Java 源码：`E:\project\ecology\ecology\src`（约 3 万个 `.java`，包名以 `weaver.*` / `com.engine.*` / `com.api.*` 为主；`out/` 下含 Flex `.mxml/.as` 与少量 Velocity `.vm`）
> - 部署的前端 webapp：`D:\Weaver2020\ecology`（JSP/JS/CSS/HTML；含 `formmode/`、`portal_main/`、`newportal/`、`mobilemode/`、`LeftMenu/`、`classbean/` 等）
>
> 说明：传统页面不存在"独立前端工程构建部署"，就是 webapp 里的 `.jsp` 由容器（Resin，见 `D:\Weaver2020\ecology\resin.xml`）直接渲染；E9 新增的门户/后台是 React SPA（`spa/portal/`、`src4js/pc4backstage/portal4engine/`），由后端 REST 供数。

---

## 0. 总览与阅读导航

> 本文档经历多轮分析（基线 §1–§9、修正 §10–§11、门户引擎深扒 §12–§17）。本节能帮你在「想找某功能 / 某表 / 某接口」时快速定位。所有结论均带 `文件:行` 或 `data/*.sql` 证据。

### 0.1 文档演进与定位
- **§1–§9 基线**：从「后台配置 → 前端门户」的全局生成机制讲起——架构范式、菜单导航、门户/首页组合、表单建模、权限框架、模板加载、接口调用、完整流程图、附录速查。
- **§10–§11 修正轮**：纠偏（手工门户 `portal_main/` 不走配置、E9 入口 `/wui/main.jsp` 主题路由、SPA 模块加载、`/api/portal/*` 清单、移动门户引擎、建模布局复用等）。
- **§12–§17 门户引擎深扒**：把 §12.2 那张「只罗列模块名」的门户引擎功能树，逐个分栏做到「功能 / 接口 / 表 / 前端文件」带证据深扒（含两次关键纠错：原文档曾误假设 `loginportal≈loginpage`、`PortalConfigComInfo` 存在）。

### 0.2 门户引擎功能树 → 深度分析索引
| 分栏 | 深度章节 | 核心结论一句话 |
|---|---|---|
| 门户引擎概述与模块树 | §12、§13 | 门户引擎 = React SPA + `/api/portal/*` REST；≠ 所见即所得拖拽画布 |
| 门户菜单 portalmenu | §14 | 门户菜单 = 系统 LeftMenu/MainMenu 的可视化维护界面（同一套表） |
| 门户页面 portalpage | §15.2（A1–A4） | 治理 `HPINFO`；三套布局 `HPLAYOUT`/`PAGELAYOUT`/`HP_GRID_LAYOUT` |
| 门户元素 portalelement | §15.3（B1–B9）、§17.1 | 治理 `HPELEMENT` 两层模型；53 种 ebase 元素类型全谱系 |
| 素材库 materiallib | §15.4（C1–C2） | 新闻模板 `PAGENEWSTEMPLATE`；图片素材库纯文件系统（无关系表） |
| 门户主题 portaltheme | §15.5（A1–A4） | 定义分散 `ecology8/9theme`+`hp_grid_layout`；个人在 `HrmUserSetting`、组织在 `systemtemplateunifyinfo` |
| 移动门户 mobileportal | §15.6（B1–B4）+ §20 | 独立 REST+表（`hp_mobile_hpinfo`）；与「移动建模」是两套引擎，复用 `modehtmllayout` 的是移动建模，详见 §20 |
| 应用中心+协同 | §15.7（C1–C2） | 协同真以 `hpid` 操作旧门户表（`hpinfo_workflow`/`hpLayout`） |
| 门户维护 portalmaintenance | §16（含 §15.2 的 mainportal/loginpage） | `loginportal≠loginpage`；tsportal 实为网格布局；导出导入全链路 |
| 元素类型谱系 + 权限分权 | §17 | 53 种元素 + `PortalRight` 14 码 + 门户管理分权 |
| 运行时渲染 + 登录门户选择 | §18 | SPA+JSON 渲染；"我的门户"=按用户行 `hplayout`+`hpElementSettingDetail`；landing hpid 判定顺序；对 §1/§8 等 7 处纠偏 |
| 单点登录/身份集成（SSO/IDP） | §19 | 三套独立机制（SSO 过滤器 SP / `weaver_sso_app` IDP / `outter_sys` 凭据代填）；outterSys 元素只对接代填；6 处误读纠偏 |
| 移动门户运行时 + 两套引擎 | §20 | 移动门户(`hp_mobile_hpinfo`)是 PC 门户同源兄弟实现；复用 `modehtmllayout` 的是移动建模(AppHomepage/MEC)；纠正 §11.5/§15.6 |
| 统一勘误 Changelog | §21 | 汇总 §18.3/§19.5/§20 全部误读→实证对照，分渲染/表名/SSO/移动四类 |
| 门户数据权限与组织可见范围 | §22 | 三条轴：管理(`ptaccesscontrollist`)/终端可见(`shareinnerhp`)/元素可见(`elementshareinfo`)+数据按身份过滤 |
| 设计态编辑器 + 个性化写回 | §23 | 设计态 `pc4mobx/portal` 搭页面/配元素；个性化按 `(hpid,userid,usertype)` 写用户专属行(`hplayout`/`hpElementSettingDetail`/`UserAddElementInfo`) |

### 0.3 高频查找速查
- **某功能在哪个 REST 接口？** → §10.4（`/api/portal/*` 清单）、§14/§15/§16/§17 各模块「后端接口」段。
- **某配置落在哪张表？** → §9.1 关键配置表速查、§3.2 / §14 / §15 / §16 / §17 的「表」段。
- **门户菜单/页面与旧系统 LeftMenu 啥关系？** → §14.1、§15.1、§15.8、§16.7。
- **门户有哪些元素类型？** → §17.1 类型清单表（53 种）。
- **门户权限码有哪些？** → §17.2 权限码对照表 + 门户管理分权。
- **门户运行时怎么渲染 / 登录后怎么选门户？** → §18（SPA+JSON 管线、`getBaseHpStr` 合并、`ThemeStore` 的 landing hpid 判定顺序、登录前/后衔接）。
- **单点登录 / 集成登录是怎么回事？** → §19（SSO/IDP 三套机制、USB Key 防伪、`outterSys` 凭据代填与 SSO 无关）。
- **移动端门户怎么渲染 / "移动"是否复用 PC 建模？** → §20（移动门户与移动建模是两套引擎；复用 `modehtmllayout` 的是移动建模，移动门户不读它）。
- **所有被纠正的误读/假设汇总在哪？** → §21（统一勘误 Changelog，分渲染/表名/SSO/移动四类）。
- **门户/元素"谁能看到"怎么算（数据权限/组织范围）？** → §22（三条轴：管理 `ptaccesscontrollist` / 终端可见 `shareinnerhp` / 元素可见 `elementshareinfo`，加数据型元素按身份过滤）。
- **管理员怎么搭门户/用户"我的门户"怎么保存？** → §23（设计态 `pc4mobx/portal` 编辑器；个性化按 `(hpid,userid,usertype)` 写用户专属行，`intiThisHpData` 首次拷贝创建者行）。

---

## 1. 整体架构：两套渲染范式并存

| 范式 | 技术栈 | 代表入口 | 数据来源 |
|---|---|---|---|
| 旧范式（E8 及以前） | JSP + `<jsp:include>` + `<iframe>`，服务端拼 HTML | `/main.jsp`、`/portal/main.jsp` | JSP 内 `<jsp:useBean>` + `RecordSet` 直连 SQL |
| 新范式（E9） | 后端 REST（`com.engine.*` JAX-RS）+ 前端 React SPA；**两套 SPA**：用户运行时门户 `src4js/pc/portal/`（编译 `/spa/portal/static/`）、后台管理门户 `src4js/pc4backstage/portal4engine/` | `/wui/main.jsp` → `/wui/index.html#/main`、REST `/api/portal/*` | REST 返回 **JSON**（如 `/api/portal/homepage/hpdata`），SPA 渲染（运行时管线见 §18） |

**Velocity 澄清**：`.vm` 模板仅 11 个（`E:\project\ecology\ecology\src\configure.vm`、`head.vm`、`idp.vm` 等），且**并非 E-cology 作为 IDP 发令牌的配置页**，而是 **OIOSAML SP 端**接入外部 IdP 的"选择 IdP / SP 自配置"页（详见 §19.2）。**门户、首页、表单建模均不使用 Velocity**——它们的"模板"是 JSP/HTML + JS 配置（JSON）。

### 1.1 登录入口与跳转链路

`login/VerifyLogin*.java`（源码在 `E:\project\ecology\ecology\src\login\`，部署件 `D:\Weaver2020\ecology\login\VerifyLogin*.jsp`）决定登录后落地页：

```
login/Login.jsp
  → login/VerifyLogin.jsp（校验账号）
      → RedirectFile 默认为 /wui/main.jsp
      → 若 logintype=2（门户模式）→ RedirectFile = /portal/main.jsp
      → 若带 gopage 参数（如待办直达）→ /wui/main.jsp?gopage=<url>
```

多个 main 入口并存：`/wui/main.jsp`、`/main.jsp`、`/portal/main.jsp`、`/wui/page/main.jsp`（`VerifyLoginSmall.jsp` 用）。

### 1.2 全局上下文初始化：`/systeminfo/init_wev8.jsp`

几乎所有 JSP 第一行都是 `<%@ include file="/systeminfo/init_wev8.jsp" %>`（如 `formmode/menu/MenuMaintenanceList.jsp:4`）。它做统一的前置处理（源码 `D:\Weaver2020\ecology\systeminfo\init_wev8.jsp`）：

```java
User user = HrmUserVarify.getUser(request, response);          // 登录态校验，未登录→跳登录页
LicenseCheckLogin licenseCheckLogin = new LicenseCheckLogin(); // License 检查、在线状态
licenseCheckLogin.setOutLineDate(user.getUID(), IPUtil.getIp(request));
TestWorkflowCheck twc = new TestWorkflowCheck();               // 流程 URL 白名单检查
if (twc.checkURI(session, request.getRequestURI(), ...)) { ... }
if (session.getAttribute("userOffline").equals("1")) { ... }   // 强制下线检查
```

**即：每个页面的"权限/上下文"都从 include 这个文件开始，它是所有页面共享的横切入口。**

---

## 2. 菜单导航：配置项如何映射为页面 URL

### 2.1 后端核心类（`weaver.systeminfo.menuconfig` 包）

| 类 | 绝对路径 | 核心方法/职责 |
|---|---|---|
| `LeftMenuHandler` | `E:\project\ecology\ecology\src\weaver\systeminfo\menuconfig\LeftMenuHandler.java` | `getTopLevelLeftMenuInfos(userId)`、`getSubLevelLeftMenuInfos(userId,parentId)`、`getMenuInfo(infoId)`、`getLeftMenuMapping(...)` |
| `LeftMenuConfigHandler` | 同包 `LeftMenuConfigHandler.java` | `getConfigWhere(userId)`（按 `resourcetype/resourceid` 生成可见性过滤条件） |
| `LeftMenuInfo` | 同包 `LeftMenuInfo.java` | 实体：`getLinkAddress()`、`getIconUrl()`、`getParentId()`、`getMenuLevel()`、`getLabelId()` |
| `MainMenuHandler` / `MainMenuInfo` / `MainMenuConfigHandler` | 同包 `MainMenu*.java` | 顶部菜单同构处理 |
| `MenuMaint` | 同包 `MenuMaint.java`（78KB） | 菜单维护逻辑；`tblConfig = "leftmenuconfig"/"mainmenuconfig"` 按 menuType 选表 |
| `HrmUserVarify` | `E:\project\ecology\ecology\src\weaver\hrm\HrmUserVarify.java`（或同包） | `getUser(request,response)`、`checkUserRight(权限点, user)` |

### 2.2 真实 SQL（取自 `LeftMenuHandler.java:41-59`）

顶级菜单加载：

```sql
SELECT t1.id, t1.labelId, t1.useCustomName, t1.customName, t2.visible
  FROM LeftMenuInfo t1, LeftMenuConfig t2, SystemModule t3
 WHERE t2.infoId = t1.id
   AND t1.relatedModuleId = t3.id
   AND <LeftMenuConfigHandler.getConfigWhere(userId)>   -- 按资源类型/ID 过滤
   AND t3.moduleReleased = 1                            -- 模块已发布
   AND t1.menuLevel = 1                                 -- 顶级
 ORDER BY t2.viewIndex;
```

子菜单加载（`LeftMenuHandler.java:99-109`）：把 `menuLevel=1` 换成 `menuLevel=2` 并加 `AND t1.parentId = <parentId>`。

单菜单详情（`LeftMenuHandler.java:139-144`）：

```sql
SELECT id, labelId, iconUrl, linkAddress, parentId, defaultIndex, menuLevel, useCustomName, customName, relatedModuleId
  FROM LeftMenuInfo WHERE id = <infoId>;
```

### 2.3 数据库配置表与关键字段

| 表 | 作用 | 关键字段 |
|---|---|---|
| `LeftMenuInfo` | 左侧菜单定义 | `id`、`labelId`（多语言标签）、**`linkAddress`（页面 URL，仅子菜单有值）**、`iconUrl`、`menuLevel`（1 顶级 / 2 子级）、`parentId`、`viewIndex`（显示顺序）、`relatedModuleId`、`useCustomName`/`customName`、`visible` |
| `LeftMenuConfig` | 按组织/人员的可见性配置 | `infoId`、`resourceId`、`resourcetype`（1=全员 / 2=分部 / 3=人员）、`visible`、`viewIndex`、`locked`、`useCustomName`/`customName` |
| `SystemModule` | 模块清单 | `id`、`moduleReleased`（1=已发布；未发布的模块其菜单不显示） |
| `MainMenu` / `MainMenuConfig` | 顶部菜单（同构） | 同上 |
| `SystemMenuInfo` | 系统菜单定义 | — |
| `HtmlLabelInfo` | 多语言标签 | `indexId`（`labelId` 关联它取显示名） |

### 2.4 前端渲染文件

- `D:\Weaver2020\ecology\mainleft.jsp`（根，左侧主框架）
- `D:\Weaver2020\ecology\portal\main.jsp`、`portal\left.jsp`、`portal\top.jsp`、`portal\PortalTabs.jsp`（`PortalTabs.jsp` 内含 `HrmUserVarify.checkUserRight("SystemTemplate:Edit", user)` 权限闸门）
- `D:\Weaver2020\ecology\LeftMenu\LeftMenuTree.jsp`、`LeftMenu\InfoCenterTree.jsp`
- E9 SPA：`D:\Weaver2020\ecology\spa\portal\engine.js`、`src4js\pc4backstage\portal4engine\components\mainportal\`

### 2.5 菜单 → 页面的关联方式

**「菜单项 → 具体页面 URL」由 `LeftMenuInfo.linkAddress` 直接驱动。** 流程：

```
LeftMenuHandler.getTopLevelLeftMenuInfos(userId)
  → 执行 §2.2 的 SQL（LeftMenuInfo ⋈ LeftMenuConfig ⋈ SystemModule + getConfigWhere 过滤）
  → 构造 LeftMenuInfo 对象（linkAddress 即目标页 URL）
  → LeftMenuTree.jsp / SPA 菜单组件渲染菜单树
  → 用户点击 → 浏览器跳转 linkAddress（例：/workflow/request/RequestView.jsp、/formmode/view/ViewMode.jsp?...）
```

> **自定义菜单**：`LeftMenuInfo.isCustom` + `useCustomName/customName` 支持用户自定义菜单名；`getLeftMenuMapping`（`LeftMenuHandler.java:273+`）按 `resourceid/resourcetype` 取个人化后的菜单顺序与可见性。

---

## 3. 门户 / 首页：配置项如何组合成页面

### 3.1 后端核心类（`weaver.homepage` 包 + `com.engine.portal` 包）

| 类 | 绝对路径 | 核心方法/职责 |
|---|---|---|
| `HomepageExtShow` | `E:\project\ecology\ecology\src\weaver\homepage\HomepageExtShow.java`（70KB） | `getNewsContent()`、`getEmailContent()`、`getImgPlay()`、`getRssUrlStr()` —— **各元素内容 HTML 生成** |
| `HomepageUtil` | 同包 `HomepageUtil.java`（48KB） | `getElementShareLevel()`（读 `hpElementSettingDetail.sharelevel`）、`getLimitStr()` |
| `HomepageWhere` / `HomepageSql` / `HomepageFiled` | 同包 | 元素数据源 SQL 的 where/字段解析 |
| `HomepageStyleBean` / `HomepageStyleUtil` | `weaver\homepage\style\` | 布局样式解析 |
| `HomepageElementCominfo` 等 cominfo | `weaver\homepage\cominfo\` | 元素/样式缓存 |
| `HomepageMaintOperate` | `E:\project\ecology\ecology\src\com\api\portal\backend\service\util\HomepageMaintOperate.java`（60KB） | 门户维护操作 |
| `WeaverPortal` / `PortalElements` / `PortalDataSource` | `E:\project\ecology\ecology\src\weaver\admincenter\homepage\` | 新门户（portal_main）容器与元素 |
| `MainPortalAction` | `E:\project\ecology\ecology\src\com\engine\portal\web\MainPortalAction.java` | `@Path("/portal/mainPortal")` 登录后门户 REST；`checkUserRight` 用 `PortalRightUtil` |
| `PortalElementsAction` / `MPortalPageAction` | `com\engine\portal\web\` | 元素/页面配置 REST |
| `HomepageCommonBiz` / `Homepage2CommonBiz` | `com\engine\portal\biz\homepage\` | 首页元素业务 |

### 3.2 数据库配置表与关键字段

| 表 | 作用 | 关键字段 |
|---|---|---|
| `homepage` | 门户/首页页容器 | `id`(hpid)、名称、`styleid` |
| **`hpelement`** | **门户元素（一个首页 = N 行元素）** | `id`(eid)、`hpid`、**`strsqlwhere`（数据源 SQL）**、`frommodule`（来源模块：文档/流程/日程/邮件…）、`styleid`（布局样式）、`perpage`、`showfield`、`linkmode`、`isremind`、`margintop`、`isUse` |
| `hpElementSetting` | 元素配置 | `eid`、`name`、`value` |
| `hpElementSettingDetail` | 元素按用户配置 | `eid`、`userid`、`usertype`、`perpage`、`linkmode`、`showfield`、**`sharelevel`**、`currentTab`、`isremind` |
| `hpElementImg` / `hpElement_timeline` / `hpElement_slidesetting` / `hpElement_quickentry*` | 图片/时间线/轮播/快捷入口元素 | `eid`、`imagefileid` 等 |
| `HomePageDesign` / `PersonalHomePageDesign` | 旧式 iframe 个人首页 | `url` 等 |

### 3.3 前端文件

- 旧式 iframe 首页：`D:\Weaver2020\ecology\system\HomePage.jsp`（读 `HomePageDesign`，按 `url` 拼 iframe）、`system\homepage\DesignHomePage.jsp`（元素选择器）
- 门户元素 JS：`D:\Weaver2020\ecology\js\homepage\Homepage_js.jsp`、`LoginHomepage_js.jsp`
- E9 门户：`D:\Weaver2020\ecology\portal_main\index.html`（+ `portal_main/` 360 个文件，含按场景的页面模板 `page_qy.html`、`page_jt.html`、`page_bs.html`…）、`newportal\`、`homepage\`
- E9 SPA：`spa\portal\engine.js`、`src4js\pc4backstage\portal4engine\`（含 `stores/mainportal/MainPortalStore.js`、`components/mainportal/*`）

### 3.4 门户页面的组装方式

**「一个门户页 = N 条 `hpelement` 行（类型 + 数据源 SQL + 样式）的组合」。**

```
homepage(hpid) ──1:N──> hpelement(eid)
    每条 hpelement：
      frommodule   → 决定元素类型（新闻/待办/日程/邮件）
      strsqlwhere  → 决定数据源（对 docs/workflow/email 等执行 SQL）
      styleid      → 决定布局（hpElementStyle）
        │
        ▼
HomepageExtShow.getNewsContent(eid, strsqlwhere, styleBean)
        │  执行数据源 SQL → 生成 HTML 片段
        ▼
按 hpelement 顺序/分栏组装成整页
        │
        ├─ 旧：HomePage.jsp 用 iframe 装载各元素 url
        └─ 新：REST(/portal/mainPortal 等) 返回 HTML 片段，SPA 注入对应容器
```

> **延伸**：门户引擎各分栏（页面/元素/菜单/主题/移动/协同/维护）的逐模块深度分析、运行时渲染管线、个人化与门户选择、单点登录/集成登录，分别见 §14–§19；门户元素类型全谱系与权限分权见 §17。

---

## 4. 表单建模（FormMode）：配置项如何动态生成列表页/明细页

这是"配置驱动"最彻底的部分：**后台配一个建模应用，前后端零代码动态生成列表页与明细页**。

### 4.1 前端目录结构（`D:\Weaver2020\ecology\formmode\`）

| 子目录 | 作用 |
|---|---|
| `view/`（87 文件） | 列表页/明细页渲染（`ViewMode.jsp`、`formModeViewComMore.jsp`） |
| `setup/`（213 jsp） | 建模后台设置（`FormModeHtmlList.jsp` 布局列表、`LayoutPreview.jsp` 预览） |
| `search/`（36 jsp） | 建模搜索页 |
| `browser/`（19 jsp） | 浏览框（选择控件） |
| `custompage/` + `cuspage/` | 自定义页面 |
| `tree/`（56 文件） | 树形建模 |
| `charts/` | 建模图表 |
| `exceldesign/`（416 文件） | Excel 导入导出设计 |
| `menu/`（54 jsp） | **建模菜单维护**（`MenuMaintenanceList.jsp`） |
| `import/`（1512 文件，含 832 个 xml） | 建模导入（模块/模板） |
| `js/`（3086 文件） | 前端 JS 资源 |
| `apps/`（308 文件） | 建模应用相关前端 |
| `form/`、`request/`、`data/`、`template/`、`interfaces/`、`webservice/` | 表单/请求/数据/模板/接口 |

### 4.2 后端核心类

| 类 | 绝对路径 | 职责 |
|---|---|---|
| `FormmodeListAction` | `E:\project\ecology\ecology\src\com\api\formmode\web\FormmodeListAction.java` | 列表页：组装列（`modeformfield`）、`rightMenuArray`、取数据 |
| `FormmodeFormAction` | 同包 `FormmodeFormAction.java`（148KB） | 表单/明细页 |
| `ResolveFormMode` | `E:\project\ecology\ecology\src\com\api\formmode\view\ResolveFormMode.java`（131KB） | **核心：列表/表单 HTML 解析渲染** |
| `ModeShareManager` | `E:\project\ecology\ecology\src\weaver\formmode\view\ModeShareManager.java` | 数据分权（动态表 `modeDataShare_<modeId>`） |
| `ModeRightInfo` / `FormModeRightInfo` | `weaver\formmode\setup\ModeRightInfo.java`、`weaver\formmode\search\FormModeRightInfo.java`（63KB） | 建模功能/数据权限校验 |
| `ModeDataManager` | `com\api\formmode\data\ModeDataManager.java` | 数据 CRUD |
| `FormmodeService` | `com\api\formmode\service\FormmodeService.java`（354KB） | 大服务：读 `modeformfield`/`modehtmllayout`/`workflow_billfield` |
| `com.api.formmode.page.*`（adapter） | `com\api\formmode\page\adapter\...` | 新 Cube 页面框架（卡片/搜索/表单适配器） |

**MyBatis 映射**（编译后位于 `D:\Weaver2020\ecology\classbean\com\api\formmode\mybatis\mapper\`）：
`ModeMapper.xml`、`FormMapper.xml`、`ModeTreeFieldMapper.xml`、`CustomSearchMapper.xml`、`CustomPageMapper.xml`、`CardMapper.xml`、`SplitPageMapper.xml`、`TreeMapper.xml`、`SimpleTableMapper.xml`、`ModeCodeMapper.xml`、`ModeRightInfoMapper.xml`。

### 4.3 数据库配置表与关键字段

> 注意：实际表名与常见猜测不同（已按源码校正）。

| 表 | 作用 | 关键字段 |
|---|---|---|
| `modeinfo` | 建模应用 | `id`、`modename`、`formid`（→ `workflow_bill.billid`）、`DefaultShared`、`NonDefaultShared` |
| `workflow_bill` | 表单底层物理表定义（与流程引擎共用） | `id`(billid)、表名等 |
| `workflow_billfield` | 字段定义 | `id`、`billid`、`fieldname`、`fieldlabel`、`fieldhtmltype`、`type`、`detailtable`、`viewtype`、`dsporder` |
| `workflow_billdetailtable` | 明细表 | `billid`、`tablename`、`orderid` |
| **`modeformfield`** | 字段在建模中的权限/布局 | `fieldid`、`modeid`、`formid`、`isview`、`isedit`、`layoutid`、`type`、`placeholder`、`expendattr`（关联 `workflow_billfield`） |
| **`modeHtmlLayout`** | 布局模板（即常称的 mode_layout） | `id`、`modeid`、`formid`、`type`、`layoutName`、**`sysPath`（模板路径）**、`isDefault`、`dataJSON`、`pluginJSON`、`scripts`、`cssFile`、`colsPerRow` |
| `workflow_selectItem` | 下拉选项 | `fieldid`、`selectvalue`、`selectname` |
| `modeDataShare_<modeId>` / `_set` | 按建模分权的数据（**动态表**） | 由 `ModeShareManager` 维护 |
| `modeformverify` | 表单校验 | `formid`、`id`、`color` |

### 4.4 建模菜单如何挂进左侧菜单

`formmode/menu/MenuMaintenanceList.jsp`（源码 `D:\Weaver2020\ecology\formmode\menu\MenuMaintenanceList.jsp`）是建模菜单管理页：

```jsp
<%@ include file="/systeminfo/init_wev8.jsp" %>
String menuflag = Util.null2String(request.getParameter("menuflag")); // 表单建模新增菜单地址
...
if(!HrmUserVarify.checkUserRight("HeadMenu:Maint", user)) { ... }  // 总部菜单维护权限
if(!HrmUserVarify.checkUserRight("SubMenu:Maint", user))  { ... }  // 分部菜单维护权限
```

**建模菜单最终写入与系统菜单相同的 `LeftMenuInfo`/`LeftMenuConfig` 表**（`linkAddress` 指向建模的 `formmode/view/ViewMode.jsp?modeId=...&formId=...`），因此建模菜单与系统菜单走同一套渲染/权限链路。

### 4.5 建模页面动态生成流程

```
后台配置建模 = modeinfo(formid→workflow_bill) + 一组 modeformfield + 若干 modeHtmlLayout
   │
   ├─ 列表页: 前端访问 formmode/view/ViewMode.jsp?modeId=..&formId=..
   │     → FormmodeListAction
   │     → modeformfield LEFT JOIN workflow_billfield 取可见列/类型/权限(rightMenuArray)
   │     → ResolveFormMode 生成列表 HTML（新框架由 com.api.formmode.page.adapter 渲染）
   │     → 前端 view/ViewMode.jsp + formModeViewComMore.jsp 呈现
   │
   └─ 明细/表单页: 点击某行 → FormmodeFormAction
         → 读 modeHtmlLayout.sysPath(模板) + modeformfield 按 layoutid 取字段
         → ResolveFormMode 套模板生成表单；存盘走 ModeDataManager
```

即"后台配一个建模应用 = modeinfo + 一组 modeformfield + 若干 modeHtmlLayout"，列表页与明细页全部由这些配置行动态拼出，**无需改代码**。

---

## 5. 权限体系

### 5.1 权限框架类

| 类/机制 | 位置 | 说明 |
|---|---|---|
| `SystemRight` / `Systemrightdetail` / `Systemrightroles` / `Systemrightgroup` | `E:\project\ecology\ecology\src\com\weaver\procedure\systemright*`、`com\engine\hrm\cmd\systemright*` | 系统权限点框架 |
| `RightMenu` | 同体系 | 把菜单绑定到权限点 |
| `HrmUserVarify.checkUserRight(权限点, user)` | `weaver\hrm\HrmUserVarify.java` | JSP/后端通用权限校验入口（例：`HeadMenu:Maint`、`SystemTemplate:Edit`） |
| `PortalRightUtil` + `PortalRight` | `com\engine\portal\util\PortalRightUtil.java`、`constant\PortalRight.java` | 门户权限（`MAIN_PORTAL`/`COMPANY_MAIN_PORTAL`）；`MainPortalAction.checkUserRight` |
| `ModeShareManager` | `weaver\formmode\view\ModeShareManager.java` | 建模数据分权 → 动态表 `modeDataShare_<modeId>` |
| `FormModeRightInfo` / `ModeRightInfo` | `weaver\formmode\{setup,search}\` | 建模字段级/功能级权限，最终并入 `SystemRight` 体系 |
| `shareright` | DB 表 | CRM/文档等模块共享基础 |

### 5.2 三类"可见性"的区别（易混淆）

1. **菜单可见性**：不是功能权限，而是 `LeftMenuConfig`(resourcetype/resourceid/visible) + `SystemModule.moduleReleased` 过滤（见 §2.2 SQL）。
2. **门户元素可见性**：`hpElementSettingDetail.sharelevel`（元素共享级别）。
3. **功能/数据权限**：`SystemRight` 权限点 + 建模分权动态表。

---

## 6. 模板与组件加载机制

| 机制 | 载体 | 说明 |
|---|---|---|
| JSP include（旧） | `portal/main.jsp` → `<jsp:include page="top.jsp">` + `mainres.jsp` iframe | 服务端拼装；各页 include `/systeminfo/init_wev8.jsp` 做登录/上下文初始化 |
| Velocity（仅 IDP） | `E:\project\ecology\ecology\src\configure.vm` 等 11 个 | 只用于单点登录/IDP 配置 |
| 前端 JS 懒加载（新） | `D:\Weaver2020\ecology\src4js\pc4backstage\portal4engine\` → 打包 `spa\portal\static4engine\engine.js` | React/MobX；组件/主题按配置（hpelement、门户主题）按需加载 |
| 服务端片段注入 | `HomepageExtShow` 生成 HTML → 门户 REST 返回 → SPA 注入容器 | 首页元素/建模列表均可服务端生成片段 |
| 建模 Cube 适配器 | `com.api.formmode.page.adapter.*` | 浏览器端按 `modeHtmlLayout.dataJSON` 渲染卡片/搜索/表单 |

---

## 7. 数据接口调用

| 接口/URL | 后端类 | 用途 |
|---|---|---|
| `POST /login/VerifyLogin.jsp` | `login\VerifyLogin.jsp` | 登录、决定落地页（`/wui/main.jsp` 或 `/portal/main.jsp`） |
| `/main.jsp`、`/portal/main.jsp` | JSP 框架页 | 旧框架（top.jsp + mainres.jsp iframe，初始页 `/system/HomePage.jsp`） |
| `/wui/main.jsp` | E9 SPA 入口 | React 门户 |
| `@Path("/portal/mainPortal")` | `com.engine.portal.web.MainPortalAction` | 登录后门户 REST（checkUserRight） |
| `@Path("/portal/...")` | `PortalElementsAction` / `MPortalPageAction` | 元素/页面配置 REST |
| `com.api.formmode.web.FormmodeListAction` | 建模列表 | JSP Action（列/权限/数据） |
| `com.api.formmode.web.FormmodeFormAction` | 建模表单 | JSP Action |
| `com.api.formmode.data.ModeDataManager` | 建模数据 CRUD | — |
| `weaver.interfaces.*` / `webservice/` | `formmode/webservice/` | 建模对外接口 |

**数据链路**：前端 JSP → `init_wev8.jsp`（上下文/权限）→ `<jsp:useBean>` 或 Action 类 → `RecordSet`/MyBatis（`classbean/*.xml` 映射）→ 业务表（`LeftMenuInfo`/`hpelement`/`mode*`/`workflow_bill*`）→ 渲染 JSP 或 REST 返回。

---

## 8. 完整「配置 → 展示」流程图

```
┌─────────────────────────────────────────────────────────────┐
│ [后台管理 JSP 保存配置]                                        │
│  formmode/menu/MenuMaintenanceList.jsp（建模菜单维护）          │
│  system/homepage/DesignHomePage.jsp（门户元素选择）             │
│  formmode/setup/FormModeHtmlList.jsp（建模布局）                │
└───────────────────────┬─────────────────────────────────────┘
                        │ 管理员操作
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ [配置落库]                                                    │
│  菜单:  LeftMenuInfo / LeftMenuConfig / SystemModule          │
│  门户:  homepage / hpelement / hpElementSetting*              │
│  建模:  modeinfo / modeformfield / modeHtmlLayout             │
│        / workflow_billfield / workflow_bill                   │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ [登录] login/VerifyLogin.jsp                                  │
│   → RedirectFile = /wui/main.jsp 或 /portal/main.jsp          │
└───────────────────────┬─────────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ [上下文初始化] 每个 JSP include /systeminfo/init_wev8.jsp      │
│   HrmUserVarify.getUser + LicenseCheckLogin + TestWorkflowCheck│
└───────────────────────┬─────────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ [菜单渲染] LeftMenuHandler.getTopLevelLeftMenuInfos(userId)   │
│   SQL: LeftMenuInfo ⋈ LeftMenuConfig ⋈ SystemModule           │
│        + getConfigWhere(resourcetype/resourceid)              │
│   → LeftMenuInfo.linkAddress = 目标页 URL                     │
│   → LeftMenuTree.jsp / SPA 菜单组件                           │
│   → 点击 → 跳转 linkAddress                                    │
└───────────────────────┬─────────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ [首页/门户渲染]                                                │
│   homepage(hpid) → N× hpelement(frommodule+strsqlwhere+styleid)│
│   HomepageExtShow.getNewsContent(eid,strsqlwhere,style)       │
│     → 执行数据源 SQL → HTML 片段                              │
│   → HomePage.jsp(iframe) / SPA 门户容器注入                    │
└───────────────────────┬─────────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ [建模页面渲染]                                                 │
│   列表: FormmodeListAction → modeformfield ⋈ workflow_billfield│
│         → ResolveFormMode → view/ViewMode.jsp                 │
│   表单: FormmodeFormAction → modeHtmlLayout.sysPath            │
│         + modeformfield(layoutid)                             │
│         → ResolveFormMode / page adapter → formModeComView.jsp │
└───────────────────────┬─────────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ [权限闸门（贯穿全程）]                                          │
│   PortalRightUtil(PortalRight) · HrmUserVarify.checkUserRight  │
│   ModeShareManager → modeDataShare_<modeId> · FormModeRightInfo│
│   SystemRight / RightMenu / Systemrightdetail                  │
└─────────────────────────────────────────────────────────────┘
```

> **运行时渲染修正（详见 §18）**：上图"首页/门户渲染"框的 E9 路径需更新——E9 用户门户由 `HomepageAction./hpdata` → `GetHpAllElementJsonCmd` → `HomepageCommonBiz.getBaseHpStr` 把 `hplayout`+`hpElementSettingDetail` 合并成 `hpinfo` JSON，各元素再按 `ebaseid` 经 `ElementsAction.{edata,tabdata,news…}` 异步取数（`HomepageExtShow` 等旧 E8 取数类仍用于部分元素，但入口不再是 `/portal/mainPortal` 返回 HTML 片段，而是 `/hpdata` 返回 JSON）。文中 `GetFrontHpCmd`/`GetFrontElementCmd`/`GetPortalElementCmd`/`GetHpsCmd` 在源码中均不存在。

---

## 9. 附录

### 9.1 关键配置表速查

| 域 | 表 |
|---|---|
| 菜单 | `LeftMenuInfo`、`LeftMenuConfig`、`SystemModule`、`MainMenu`、`MainMenuConfig`、`SystemMenuInfo`、`HtmlLabelInfo` |
| 门户/首页 | `homepage`、`hpelement`、`hpElementSetting`、`hpElementSettingDetail`、`hpElementImg`、`hpElement_timeline`、`hpElement_slidesetting`、`hpElement_quickentry*`、`HomePageDesign`、`PersonalHomePageDesign` |
| 建模 | `modeinfo`、`workflow_bill`、`workflow_billfield`、`workflow_billdetailtable`、`modeformfield`、`modeHtmlLayout`、`workflow_selectItem`、`modeDataShare_<modeId>`、`modeformverify` |
| 权限 | `SystemRight`、`Systemrightdetail`、`Systemrightroles`、`Systemrightgroup`、`RightMenu`、`shareright` |

### 9.2 关键源码目录速查

| 域 | 后端源码（`E:\project\ecology\ecology\src`） | 前端（`D:\Weaver2020\ecology`） |
|---|---|---|
| 菜单 | `weaver\systeminfo\menuconfig\` | `mainleft.jsp`、`portal\`、`LeftMenu\` |
| 门户/首页 | `weaver\homepage\`、`weaver\admincenter\homepage\`、`com\engine\portal\`、`com\api\portal\` | `system\HomePage.jsp`、`portal_main\`、`newportal\`、`homepage\`、`spa\portal\` |
| 建模 | `com\api\formmode\`、`weaver\formmode\` | `formmode\`（view/setup/menu/js/...） |
| 权限 | `com\weaver\procedure\systemright*`、`com\engine\portal\util\PortalRightUtil`、`weaver\formmode\view\ModeShareManager` | JSP 内 `HrmUserVarify.checkUserRight` |
| MyBatis 映射 | — | `classbean\com\api\formmode\mybatis\mapper\*.xml` |

### 9.3 注意事项与易错点

1. **配置即页面**：菜单靠 `LeftMenuInfo.linkAddress`、门户靠 `hpelement.strsqlwhere+styleid`、建模靠 `modeformfield+modeHtmlLayout`——三者都不是硬编码页面，而是 DB 配置行驱动。
2. **表名校正**：建模布局表是 `modeHtmlLayout`（非 `mode_layout`）、字段权限表是 `modeformfield`（非 `modefield`）；字段底层定义是 `workflow_billfield`（与流程引擎共用）。
3. **`.vm` 是陷阱**：门户不用 Velocity；"模板"在 E-cology 里是 JSP/HTML + JS 配置(JSON)，`modeHtmlLayout.sysPath` 指向的才是真正的页面模板文件。
4. **分权是动态表**：建模数据权限生成 `modeDataShare_<modeId>` 这类按 modeId 分的动态表。
5. **E9 双栈**：同一套后台配置，旧客户端走 JSP/iframe，新门户走 `/api/portal/*` REST + React SPA（产物 `spa\portal\`），二者共用 `HomepageExtShow` / `Formmode*` 等后端引擎。
6. **上下文初始化**：任何新页面务必 include `/systeminfo/init_wev8.jsp`，否则没有登录态/权限/许可证检查。
7. **多语言**：菜单显示名不直接存中文，而是 `labelId → HtmlLabelInfo.indexId`。
8. **建模菜单与系统菜单同表**：建模菜单通过 `MenuMaintenanceList.jsp`（`menuflag` 参数）写入 `LeftMenuInfo`/`LeftMenuConfig`，渲染/权限与系统菜单完全一致。

---

## 10. 补充排查（第二轮）：发现的遗漏与修正

### 10.1 【重要修正】`portal_main/` 是手工定制的静态门户，不走后台配置

实际读取 `D:\Weaver2020\ecology\portal_main\index.html` 与 `page_jt.html` 证实：

- 标题"维森集团演示"（上海实业/Visen 演示门户）；
- 菜单**硬编码**在 HTML 里（信息门户/工作中心/流程管理/公文管理/督办管理/会议管理/文档管理/更多应用），点击仅切换 `#headFrame` iframe 的 `src` 与高度；
- 新闻条目为**写死的 2018 年外链**（`https://www.siic.com/...`），头像/用户名"王吴 人力资源部"也是写死的；
- 技术栈为 Bootstrap + Swiper + jQuery + iframe，**完全不涉及** `LeftMenuInfo`/`hpelement` 等任何配置表与后端接口。

**结论**：这套 `portal_main/` 是**纯静态定制演示门户**，不属于"配置驱动"体系；若项目实际使用它做门户，则门户的"配置"就是直接改这些 HTML。它可作为"绕过 E-cology 配置体系、纯前端定制门户"的参照样本。

> 同理注意：`newportal/` 仅是通讯录等少量 JSP（`contactslist.jsp`、`simplehrminfo.jsp`），并非门户引擎。

### 10.2 【补全】E9 真正入口 `/wui/main.jsp`：主题路由（此前误判为 `/main.jsp` 直达）

`D:\Weaver2020\ecology\wui\main.jsp`（100 行内已读全）是**登录后的主题分发器**：

```java
User user = HrmUserVarify.getUser(request, response);        // 登录校验
<%@ include file="/wui/common/page/initWui.jsp" %>           // wui 专用初始化（区别于 init_wev8.jsp）
String curTheme = getCurrWuiConfig(session, user, "theme");  // 读用户主题
if ("ecology9".equals(curTheme)) {
    response.sendRedirect("/wui/index.html#/main");          // → E9 React SPA
} else {
    // e8 主题：校验角色（配置项 e8themerole.role.id）后转发到主题框架页
    request.getRequestDispatcher("/wui/theme/" + curTheme + "/page/main.jsp?...").forward(request, response);
}
```

补充要点：
- 首次登录自动从 `SystemSet` 初始化 `PageUserDefault`（每用户分页设置）；
- **"主题"不是皮肤而是整套框架**：e8 主题对应 `D:\Weaver2020\ecology\wui\theme\`（2937 个文件、62 个 JSP，`wui/theme/<theme>/page/main.jsp` 为各主题框架页），e9 主题对应 React SPA；
- 修正登录链路：`VerifyLogin.jsp → /wui/main.jsp（主题路由）→ /wui/index.html#/main（E9）或 /wui/theme/<t>/page/main.jsp（E8）`；`/portal/main.jsp` 仅是 `logintype=2` 旧门户模式落地页。

### 10.3 【补全】E9 SPA 的模块加载机制（`/spa/` + `/cloudstore/`）

`D:\Weaver2020\ecology\wui\index.html` 是 E9 SPA 的引导页（bootstrap），按序加载：

| 资源 | 作用 |
|---|---|
| `/cloudstore/resource/pc/com/v1/index.min.js`、`ecCom.min.js` | ecCloudstore 组件库（React 组件基础） |
| `/cloudstore/resource/pc/react16/*` + `mobx-3.1.16` + `react-router` | 框架依赖 |
| `/spa/moduleConfig.js` | **SPA 模块注册表**（路由→模块映射） |
| `/spa/coms/index.mobx.js`、`/spa/coms/ssoConfig/config.js` | 公共组件/SSO 配置 |
| `/spa/portal/public/index.js`、`/spa/portal/static/index.js` | 门户公共 + 门户页面 |
| `/spa/theme/static/index.js` | 主题 |
| `/spa/hrm/staticLoginNew/loginNew.js` | 登录接口 |
| `/spa/main/index-mobx.js` | **主应用入口** |

`D:\Weaver2020\ecology\spa\` 下有 70+ 模块目录（workflow 1002 文件、hrm 621、portal 508、cube 97、formmode 39……），每个模块是独立打包的 React 产物（`.js + .map + .css + .html`），**按 `moduleConfig.js` 注册、按路由懒加载**——这是 E9 前端的"模块化配置"机制（补充原第 6 章的"前端 JS 懒加载"）。

### 10.4 【补全】E9 SPA 门户实际调用的 REST 接口（`/api/portal/*` 清单）

从 React 源码 `D:\Weaver2020\ecology\src4js\pc4backstage\portal4engine\stores\*.js` 提取（`WeaTools.callApi` 调用，共 300+ 处）：

| Store | 接口 | 用途 |
|---|---|---|
| `tsportal/TsPortalStore.js` | `GET /api/portal/portalGridLayout/getPortalGridLayouts` / `getRightStatus`；`POST .../editPortalGridLayout`、`usePortalGridLayout`、`defaultEntrancePortalGridLayout`、`deletePortalGridLayout` | 门户布局网格的增删改/启用/默认入口 |
| `toolbarmore/ToolbarMoreStore.js` | `GET /api/portal/toolbarMoreMaintenance/getMenus`、`getShares`；`POST .../saveMenus`、`deleteMenus`、`setShares` | 顶栏"更多"菜单维护及共享 |
| `themestyle/ThemeStylePortalStore.js` | `GET/POST /api/portal/themeStyle/getThemeStyle|setThemeStyle|deleteThemeStyle`；`POST /api/portal/portalinfo/stylepreview` | 主题样式配置与预览 |
| `synergy/SynergyStore.js` | `GET /api/portal/synergy/getTreeData|getHpInfo`；`POST .../setIsUse` | 协同门户——**参数仍是 `hpid`，底层仍操作 `homepage/hpelement` 表**，证实 E9 REST 与旧表共用 |
| `quicksearchmanage/QuickSearchManageStore.js` | `/api/portal/quickSearchMaintenance/*` | 快捷搜索维护 |

**规律**：每个后台维护 Store 的第一个方法都是 `getRightStatus()`（先查维护权限），接口普遍带 `getShares/setShares`（按组织/人员共享配置）——即 E9 后台的每个配置功能 = "REST CRUD + 权限 + 共享"三件套，配置最终仍落库到与旧版相同的表。

### 10.5 【补全】`wui/common/page/initWui.jsp`：E9 页面初始化与 `init_wev8.jsp` 的区别

`wui/main.jsp` 及 wui 体系页面用的是 `initWui.jsp`（已读）：做 `TestWorkflowCheck.checkURI` 白名单校验（失败跳 `/login/Login.jsp`），并加载 jQuery/zDialog/IM 工具脚本——比 `init_wev8.jsp` 轻量，**没有** `HrmUserVarify.getUser`（登录校验已由 `wui/main.jsp` 自己做了）。

### 10.6 修正后的 E9 完整链路

```
login/VerifyLogin.jsp
  → /wui/main.jsp（读 SystemSet/PageUserDefault 主题配置）
      ├─ theme=ecology9 → /wui/index.html（SPA 引导）
      │     → /spa/moduleConfig.js 注册模块
      │     → /spa/main/index-mobx.js 主应用
      │     → 路由 #/main → 门户（/spa/portal/*）
      │         → WeaTools.callApi('/api/portal/mainPortal' 等数百个 /api/portal/* REST)
      │         → com.engine.portal.* 落库/读 homepage、hpelement 等旧表
      └─ 其他主题 → /wui/theme/<theme>/page/main.jsp（e8 主题 JSP 框架）
                    → portal/main.jsp / main.jsp（旧 iframe 框架）
```

### 10.7 第二轮排查结论汇总

| 发现 | 影响 |
|---|---|
| `portal_main/` 为静态定制门户（维森演示），零配置驱动 | 若需改其内容，直接改 HTML，查配置表无用 |
| E9 入口是 `/wui/main.jsp` 主题路由，非直达 `/main.jsp` | 排查门户加载问题从 `wui/main.jsp` 的 `getCurrWuiConfig` 开始 |
| E9 前端按 `spa/moduleConfig.js` + `/spa/<模块>/` 懒加载 | 新增/替换 E9 前端功能以模块为单位 |
| E9 后台维护接口集中在 `/api/portal/*`，且都带 `getRightStatus`/Shares | 权限排查先查对应 Action 的 `checkUserRight`/`PortalRightUtil` |
| E9 REST 底层仍读写旧配置表（`hpid`/`hpelement`） | 两代门户配置同源，改造可只做一层 |

---

## 11. 复核（第三轮）：问题清单与修正

对前文所有结论做代码级复核后，发现以下问题（均已修正依据如下）：

### 11.1 【已修正】§3.2 `hpelement` 字段清单不准确

原文把 `perpage/showfield/linkmode` 列在 `hpelement` 表里——**错误**。实证（`MobilePageDataUtil.java:275` 的完整 insert 语句列全了字段）：

`hpelement` 真实字段：`id, title, logo, islocked, strsqlwhere, ebaseid, isSysElement, hpid, isFixationRowHeight, background, styleid, picStyleid, marginTop, shareuser, scrolltype, isRemind, fromModule, isuse, newstemplate, marginleft, marginright, marginbottom, height`。

`perpage/linkmode/showfield/sharelevel/currentTab/isremind` 实际都在 **`hpElementSettingDetail`**（`MobilePageUtil.java:363`、`:763` 的 SQL 实证：`select sharelevel from hpElementSettingDetail where eid=... and userid=... and usertype=...`）。

即：**`hpelement` = 元素定义（全局一份），`hpElementSettingDetail` = 用户级个性化**，两层分离比原文描述更清晰。

### 11.2 【已补全】`/api/*` 路由机制（原文 §7 只列了接口，没讲路由）

实证 `D:\Weaver2020\ecology\WEB-INF\web.xml:223-232`：

```xml
<servlet>
    <servlet-name>restservlet</servlet-name>
    <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
    <init-param>
        <param-name>com.sun.jersey.config.property.packages</param-name>
        <param-value>com.cloudstore;com.api</param-value>
    </init-param>
</servlet>
<servlet-mapping>
    <servlet-name>restservlet</servlet-name>
    <url-pattern>/api/*</url-pattern>
</servlet-mapping>
```

**结论**：`/api/*` 由 **Jersey（JAX-RS）** 处理，只扫描 `com.api` 和 `com.cloudstore` 两个包——所以 REST 门面类必须在 `com.api.*` 下（`com.engine.*` 是业务实现层，不直接暴露）。另发现挂在 `/api/*` 上的过滤链：`SessionCloudFilter`（会话）、`WeaSsoIocComponentFilter`（`com.weaverboot.frame.ioc`，**WeaverBoot 自研 IOC/二开框架**，原文档未提）。`PortalGridLayoutAction` 等出现在 `com.engine.portal.web` 但带 JAX-RS 注解，是引擎层内部接口，对外经 com.api 门面/WeaverBoot 暴露。

### 11.3 【已修正】§10.4 "E9 REST 底层仍读写旧配置表"过于绝对

- **确证同源的部分**：`synergy` 接口参数即 `hpid`，直接操作 `homepage/hpelement` 旧表。
- **不能断言同源的部分**：E9 另有**独立布局体系** `com.engine.portal.cmd.portalgridlayout.*`（10 个 Cmd：布局 CRUD/启用/默认入口/元素级增删），底层表名在 service/cmd 源码中未直接出现 SQL（DAO 封装），**本次未验证**。

修正为："两代配置**部分**同源（协同门户/元素读旧表）；E9 的网格布局（portalGridLayout）与主题样式是**新增的独立配置体系**，底层表名待查库核实"。

### 11.4 【已实证】§9.3 第 3 条 `modeHtmlLayout.sysPath`

原为推断，现补代码实证：

- PC 端：`ResolveFormMode.java:777` → `modeLayoutUtil.readHtmlFile(syspath)` —— sysPath 指向 webapp 内 **HTML 模板文件**，读取后做占位符/组件替换渲染（还有 `cssfile` 关联样式文件）。
- 移动端复用：`FormmodelAdapter.java:111` 直接 `select * from modehtmllayout where modeid=... and isdefault=1` —— **移动端建模页面复用 PC 的 `modeHtmlLayout` 布局表**。

### 11.5 【重要遗漏】移动门户引擎（mobilemode）未覆盖

`com.engine.mobilemode.*` 是完整的**移动端配置驱动引擎**（`D:\Weaver2020\ecology\mobilemode\` 16732 个文件）：

- 组件体系：`ComponentType`/`component/SearchBoxAction`、`FAPIBrowserAction` 等；
- 应用/页面配置表（`FormmodelAdapter.java:202`、`PageAdapter.java:324`、`:402` 实证）：`mobileAppBaseInfo`、`AppHomepage`（isdelete/ishomepage）、`AppHomepage_Model`、`MobileAppModelInfo`、`MobileExtendComponent`（objid/mectype/mecparam）、`mode_visual_component`（layoutid/fieldid/showorder）、`modeTreeField`、`mode_pageexpand`；
- 配套：版本管理 `MVersion`、图片库 `PicLibrary`、全局变量 `AppGlobalVars`。

**移动端门户同样是"配置行→动态渲染"**；但需注意：**复用 PC 建模 `modehtmllayout` 的是"移动建模/移动模式"（AppHomepage/MEC），不是"移动门户"（hp_mobile_hpinfo）**——两者是 E9 中两套独立的移动引擎（详见 §20）。若自研门户引擎规划含移动端，此块机制可一并对标。

### 11.6 【澄清】`data/` 目录性质

`D:\Weaver2020\ecology\data\` 下 10 万+ SQL **不是业务数据**，是**分数据库的安装/升级脚本库**：`Mysql/`(17353)、`Oracle/`(23562)、`SQLServer/`(23442)、`DM/`(达梦 14172)、`GS/JC/ST/`、`init/`，外加按日期命名的增量升级脚本（`sql201904183501.sql`…）。

**用途**：它是验证任何表结构的权威字典——想确认某张表的真实建表语句/字段，来这里搜即可（比读 Java 代码推断可靠）。

### 11.7 复核后确认无误的部分

| 结论 | 验证方式 |
|---|---|
| 菜单 SQL 与三表关联（§2.2） | `LeftMenuHandler.java` 原文已读 |
| `portal_main/` 为静态定制门户（§10.1） | index.html/page_jt.html 已读 |
| E9 主题路由 `/wui/main.jsp`（§10.2） | 源码已读 |
| SPA 模块加载与 `/api/portal/*` 清单（§10.3/10.4） | `wui/index.html` + `portal4engine/stores` 已读 |
| 登录链路 logintype 分支 | `VerifyLogin*.jsp` 已读 |
| 建模菜单与系统菜单同表（§4.4） | `MenuMaintenanceList.jsp` 已读 |
| `modeformfield`/`modeHtmlLayout` 表名 | `ModeMapper.xml` 已读 |

### 11.8 复核方法备注

本轮全部结论来自：源码 SQL 原文、`WEB-INF/web.xml`、`MobilePageDataUtil`/`MobilePageUtil`/`ResolveFormMode`/`FormmodelAdapter`/`PageAdapter` 具体行号。**唯一未验证项**：portalGridLayout 的底层物理表名（建议在 `data/SQLServer/` 脚本库中搜 `portalgridlayout` 建表语句核实）。

---

## 12. 补充分析（第四轮）：门户引擎（portal4engine）功能

> 探查对象：`D:\Weaver2020\ecology\src4js\pc4backstage\portal4engine\`（前端源码）、`src\com\engine\portal\web\PortalEngineAction.java`（后端）、`spa\middlecenter\portalengine\`（打包产物）、`admincenter\portalEngine\`（jsp 管理壳）。

### 12.1 定位
- `/wui/engine.html#/portalengine` 是泛微 E-cology 的**门户 PaaS 引擎（管理端设计/运营平台）**，不是前端展示页；它运行在 `wui` 的 SPA 容器 `engine.html` 内，`#/portalengine` 路由挂载 `portal4engine` 源码（React + mobx + antd，约 380 个 js / 81 个 store）。
- 后端 `PortalEngineAction.java` 极薄：**仅 2 个统计接口**（`getUseCount` 使用统计、`getMaintenanceCount` 维护统计，二者均 `sysadmin` 限定）。所有设计能力都在前端 SPA，经 `/api/portal/portalEngine/*` 调后端。
- 与文档 §3 的 `homepage/hpelement` DB 模型的关系：门户引擎是把那些配置表（homepage、hpelement、sharelevel…）包装成的**可视化操作界面**。

### 12.2 功能模块树（取自 `index.js` 路由注册）
| 分栏 | 子模块 |
| --- | --- |
| 门户维护 portalmaintenance | loginportal（登录前门户）、mainportal（登录后门户★）、tsportal（特色门户）、portalsetting、portalexport、portalimport、themestyle、themeappoint、themesearch、portalconfig |
| 门户主题 portaltheme | portalthemelib（主题库） |
| 门户菜单 portalmenu | leftmenu / topmenu / custommenu / reportMenu、menustylelib、toolbarmore、pluginmanage、quicksearchmanage、accountmore（仅罗列；逐模块功能深扒见 §14） |
| 门户页面 portalpage | loginpage、mainpage、defaultportal（默认首页设置）、portallayout（布局库） |
| 门户元素 portalelement | developguide、portalelements（元素库★）、elementtemplate、elementstylelib、headlinestyle、elementregister |
| 素材库 materiallib | newstemplate、materiallib |
| 移动门户 mobileportal | mloginpage、melementstylelib、mnavstylelib、mportalpage |
| 应用中心+协同 | appcenter、synergy4doc/wf/wfform/wfnode |

### 12.3 代码印证的核心机制
1. **门户 = 模板，按机构分配**（`components/mainportal`）
   - 每套门户是一个 `template`，含 `templatetype`（ecology8 / ecology9 / custom / ecologyBasic / ecology7 / office）——多代技术栈并存。
   - 模板属性编辑（`Edit.js`）含 `orgType`（所有机构公用 / 指定机构）+ 分部浏览器（`companyId`）；列表行有 `isOpen`（启用）/`appoint`（指定）开关；左侧 `WeaOrgTree`（参数 `SystemTemplate:Edit`）按组织架构树授权。**即"不同机构的人进不同门户"，对标我们 `PortalPageShare`（全员/部门/个人），但 E-cology 是模板×机构的可视化树形指派。**
2. **门户维护 = 外观/主题设置**（`components/mainportal/Setting.js` + `setting/Theme.js`、`CustomTheme.js`、`SetLogo.js`、`ThemeList.js`）
   - 对话框为左 190px（主题列表 `ThemeList`）+ 右内容（主题 / Logo 设置）。按钮：预览、保存并启用（`isOpen=='0'` 时）、保存、另存为、删除（`companyId!='0'` 且 `isOpen=='0'` 时）。
3. **门户预览**（`Preview.js`）
   - e8/e9：用 `<E8Theme>` / `<E9Theme>` 组件（`opt="preview"`）在对话框内直接渲染，主题样式以 `styleConfig` JSON 存储（含 `&quot;` 转义需 `JSON.parse` 还原）。
   - 旧版（custom/ecologyBasic/ecology7/office）：iframe 加载 `/systeminfo/template/templatePreview.jsp?id=xxx`。
4. **元素库独立管理 + 引用追踪**（`components/portalelements`）
   - 元素有 `isuse`（使用中 / 未使用 / 封存），支持封存/解封、**引用关系查询**（`getReferences`：哪些门户用了该元素）、设置、日志。
   - 元素新增（`Add.js`）为**向导式**（原设计 4 步：基本信息→设置信息→内容信息→完成，`Add4Step1~4`），实际由 `weaPortal` 模块的 `<PortalSetting>` 承载；印证 §3 的"元素两层模型（hpelement 基础 + hpElementSettingDetail 详细设置）"。
5. **页面布局库**（`components/portallayout`）：布局分 `design/sys/cus` 三类，支持新建、**导入/下载 zip**、缩略图预览。
6. **主题库**（`components/portalthemelib`）：custom / ecology7 / ecology8 / ecology9 四套，支持导入主题 zip。
7. **门户导入/导出**（`portalimport` / `portalexport`）：整门户迁移能力。

### 12.4 重要澄清：门户引擎 ≠ 所见即所得拖拽画布
`portal4engine` 这套是**门户模板治理 + 外观/主题设置 + 元素库/布局库/主题库/菜单/移动端运维平台**；真正的"拖拽元素、排布布局"的画布**不在此目录**，而是：
- 由 `weaPortal` 模块的 `<PortalSetting>` 承载（元素/门户的深层编辑）；
- 或旧版 JSP（`/systeminfo/template/templatePreview.jsp`、`/portal/main.jsp` 等）承载。
即 `portal4engine` 偏"治理/配置/外观"，画布能力在 `weaPortal` 与旧门户引擎。

### 12.5 与 blade-portal 的对标差距 / 可借鉴项
| 优先级 | E-cology 能力 | blade-portal 现状 | 建议 |
| --- | --- | --- | --- |
| P1 | 按组织结构树可视化分配门户（模板×机构 + 启用/指定开关） | 仅 `PortalPageShare`（全员/部门/个人）+ `is_default` | 增加"组织机构树指派 + 启用/指定" |
| P1 | 门户导入/导出（迁移） | 无 | 新增导出/导入 JSON 能力 |
| P2 | 页面布局模板库（design/sys/cus，可导入导出） | 设计器自由拖拽，无布局模板库 | 增加"常用布局模板"复用 |
| P2 | 主题库 + 样式库（custom/e7/e8/e9，可导入） | 仅预留 `theme` 字段 | 落地主题库 + 配色/Logo |
| P3 | 独立元素库 + 引用追踪 + 封存 | 元素挂在页面下，无独立库 | 抽离元素库 + 引用/封存 |
| P3 | 登录前/后门户、特色门户、移动门户 | 单一门户（登录后） | 按需扩展多门户形态 |

### 12.6 blade-portal 已实现（对照）
- 门户页 `PortalPage` + 元素 `PortalElement`（含用户级个性化 `ElementSetting`）+ 共享 `PortalPageShare`（1 全员/2 部门/3 个人）+ `page_code`（对标 `portal-9-5`）；
- 后端 `/my-portal?portalCode=xxx` 已按 `userId`/`deptId` 从 `PortalPageShare` 解析可见门户，优先默认页；
- 前端查看页 `/portal/home` 已支持 `?code=` 精确访问指定门户（对标 `portal-9-5`），欢迎区显示"门户标识"标签；
- 拖拽设计器 `/portal/design`（dnd-kit）。

---

## 13. 补充分析（第五轮）：门户引擎功能再排查与遗漏补全

> 本轮对后端 `E:\project\ecology\ecology\src\com\engine\portal\`（cmd 478 / service 210 / web 115 / entity 35 / biz 62）与前端 `D:\Weaver2020\ecology\src4js\pc4backstage\portal4engine\components\`（33 个模块）做了全量排查，并核实了 §11.8 遗留的未验证项。以下为**对 §12 的修正 + 遗漏功能补全**。

### 13.1 【回填未验证项】§11.8：portalGridLayout 真实表名 = `hp_grid_layout`
- 门户网格布局 portalGridLayout 的物理表是 **`hp_grid_layout`**（元素子表 `hp_grid_layout_element`）。二者**不在 entity 目录、无 `@Table` 实体**，由 `com/engine/portal/cmd/portalgridlayout/*.Cmd` 以原生 SQL 读写，故此前无法从实体反推。
- `hp_grid_layout` 关键字段：`layout_type` / `layout_name` / `layout_title` / `layout_use` / `layout_default_entrance` / `layout_custom` / `layout_uuid` / `layout_version` / `layout_config` / `layout_background` / `layout_top`·`layout_bottom`·`layout_left`·`layout_right`·`layout_middle`（五区）/ `layout_setting` / `layout_delete`（软删）。
- `hp_grid_layout_element`：`layout_id`(→hp_grid_layout.id)、`layout_position`(top/bottom/left/right/middle)、`layout_element`(JSON)、`layout_delete`。
- 证据：`com\engine\portal\cmd\portalgridlayout\{GetPortalGridLayoutsCmd,EditPortalGridLayoutCmd,SetPortalGridLayoutCmd,GetPortalGridLayoutCmd}.java`。

### 13.2 【修正 §12.1】门户引擎后端是全量 REST API，不是"薄统计层"
- §12.1"后端极薄、仅 2 个统计接口"是以 `PortalEngineAction` 单类得出的**片面结论**。实际 `web/` 有 115 个类，**全部是 JAX-RS 资源**，绝大多数声明类级 `@Path("/portal/xxx")`，即**整套门户引擎以 `/api/portal/*` REST 暴露**（`PortalEngineAction` 只是未写类级前缀的特例）。
- 证据：`web\PortalMenuMaintenanceAction.java:38 @Path("/portal/portalMenuMaintenance")`；`weaver\portal\cache\MenuCache.java:20-21` 注释直书 `/api/portal/portalMenuMaintenance/setCustomMenu`、`/api/portal/portalMenuShareMaintenance/setMenuShares`。
- 核心大 Action：`ElementsAction`(66KB, 72 个 `@Path`)、`MPortalPageAction`(18KB, 25 处权限检查)、`EmailMenuAction`(24KB)、`FloatAreaAction`(21KB)、`SynergyAction`、`ThemeCenterAction`、`PortalGridLayoutAction`、`PortalImportAction`/`PortalExportAction`、`SystemInfoAction`(16KB, 多语言 label + ActiveX)。**未发现 Spring MVC `@RestController`，纯 JAX-RS。**
- 结论修正：门户引擎 = 前端 SPA + 后端 `/api/portal/*` REST，是标准的**前后端分离 PaaS**。

### 13.3 【修正 §3/§12】数据表关系：新旧表并存，非二选一
- 旧 `hp*` 表（hpinfo/hpelement/hplayout/pagelayout/hpelementsetting/hpstyle/hpbaseelement…）**仍是主体数据底座**：`entity/core/` 21 个 ORM 实体几乎全部 `@Table` 到老表；`weaver/homepage/cominfo/` 的 CacheBase 也缓存同一批老表。**新引擎把旧表用 ORM 实体重新包装，并未抛弃**（§3 依旧成立）。
- 新增两类新表：① `portal_*` 系列（E9 定制模块：portal_quicksearch/portal_workflow/portal_user/portal_logo/portal_doc/portal_copyright/portal_submoduleinfo/portal_modulerecord/portal_mloginpage 等，`from/into portal_*` 命中 73+ 处）；② `hp_grid_layout(+_element)`（新版门户布局，与旧 hplayout 实例布局 + pagelayout 布局模板**并列**）。
- `weaver/homepage/` = 旧版门户后台核心：`HomepageUtil`(渲染/管理)、`HomepageExtShow`(跨模块数据装配, 70KB)、`HomepageSql`(数据源 SQL)、`HomepageWhere`(where 装配)、`HomepageSetting`(版本迁移 wfCenterUpgrade)、`HomepageCreateImage`(图片 Servlet)、`RssUtil`(RSS 抓取)、`cominfo/*`(内存缓存)。

### 13.4 后端遗漏功能清单（§12 未覆盖）

**框架/横切**
- 统一事务框架：`cmd\PortalTransCommand`（封装 `RecordSetTrans` 手动事务 + 执行 >4000ms 打 error 日志）。
- 业务审计双轨：`BizLogContext` + `LogUtil.removeIntersectionEntry`（新旧值 diff）；`PortalLogInfo` 表落库（userid/type/item/sql/ip/opdate，`weaver\admincenter\homepage\PortalMaintenanceLog`）。
- 细粒度权限（反射策略模式）：`constant\PortalRight` 13 种权限码（homepage:Maint / Portal:MobliePage / HeadMenu:Maint / SubMenu:Maint / MaterialLibrary:Maint …）+ `biz\right\` 18 个策略类 + `util\PortalRightUtil` 反射调用，返回 `right_status:1/-1`。
- 菜单缓存：`weaver\portal\cache\MenuCache` + StaticObj 进程内缓存，`isOpenMenuCache()` 开关（默认关）。
- 国际化 i18n：菜单全链路带 `languageId`，`customName_e/customName_t`（英/繁），`HpInfo.hplanuageid`，`SystemInfoAction` 按 `user.getLanguage()`(7/8/9) 输出多语言 label。
- 多分支机构/分权：`HpInfo.subcompanyid`、CompanyComInfo/SubCompanyComInfo/ManageDetachComInfo；`PortalRight` 的 COMPANY_MAIN_PORTAL/COMPANY_MENU/SUB_COMPANY_MENU/COMPANY_FLOATAREA。

**门户对象与分配**
- 默认门户按机构：`cmd\defaultportal\{SaveDefaultPortalCmd,GetTreeDataCmd}`。
- 页面维护者/共享：`cmd\mainpage\{AddMaintToMainPageCmd,AddShareToMainPageCmd,DeleteMaintFromMainPageCmd}`。
- 登录门户定时启用：`systemlogintemplate.openTimer` + `timerStartDate/timerEndDate` + `CheckUniqueTimerCmd` 时段冲突校验。
- 桌面/浮动栏：`cmd\desktop\*`(桌面背景图/桌面菜单)、`cmd\floatarea\*`。
- 特色门户/登录门户/登录前后页面（对应前端 tsportal/loginportal/loginpage/mainpage）。

**主题管控**
- 主题个性化中心：`cmd\themecenter\{GetAllThemesOfUserIdCmd,GetMyThemeCmd,SetMyThemeColorCmd,SetMyThemeLayoutCmd,SetMyThemeTsCmd}` + `PortalThemeConfig` 表。
- 统一指定/迁移主题：`mainportal\UnifyAppointThemeCmd`、`portalmenu\maintenance\UnifySettingCmd`(27KB)。

**菜单全族**
- 前端/后端菜单：`cmd\menu\{GetFrontEndMenuCmd,GetBackEndMenuCmd,SetTopMenuStaticticsCmd}`（按点击次数排序）。
- 自定义/父子/排序/同步/共享：`portalmenu\maintenance\{GetMenuTreeDataCmd,SetSystemMenuCmd,SetCustomMenuCmd,SetParentMenuCmd,SortMenuTreeDataCmd,SynchronizeMenuCmd}` + `share\{Set,Get,Delete}MenuSharesCmd`。
- 常用菜单：`cmd\freqmenu\{GetDatasCmd(37KB),SaveDatasCmd,SyncCommonMenuCmd}`。
- 工具栏/右键/邮件菜单：`cmd\toolbar`、`toolbarmore\maintenance`、`rightclickmenu`、`portalrcmenu`、`web\EmailMenuAction`。

**元素生态**
- 元素工具栏拖拽/锁定：`elementstoolbar\{DragElementCmd,LockElementCmd,UnlockElementCmd,DeleteElementCmd}`。
- 自定义元素命令族（8 类 × 6 操作）：`cmd\customelement\{commonAbstract,companyNewsPerfect,customCompanyNews,customPage,img,rss,workflowPerfect}\{Find,Save,Update,Delete,Order,SaveOrUpdate}ItemCmd`。
- SQL 取数元素 + HD 面板/时间轴：`elementecodeaddon\GetSqlDataCmd`、`elementindie\GetElementIndieViewDataCmd`、`hdpanelelement\{GetEditDataSourceCmd,GetCalendarBaseDataCmd,GetAllManagerDataCmd}`、`timelineelementstyle`。
- 元素模板保存/预览：`elementtemplate\{SaveElementTemplateCmd(31KB),PreviewElementTemplateCmd(18KB)}`。

**样式/模板/素材库**
- 样式库：`elementstylelib` / `melementstylelib` / `menustylelib` / `mnavstylelib` / `headlinestyle`。
- 报表/图表模板与配色：`reportform\*`(ReportFormTemplateService/ReportFormColorSchemeService)、`echarts\*`(EchartTemplate)、`e7form\*`。
- 新闻模板/素材库：`newstemplate`、`materiallib`（UploadImageToMaterialLibCmd + MaterialLibService + MaterialLibRight）。

**协同/业务集成**
- `biz\synergy\` 7 个 Biz（Synergy4Workflow/Doc/Report/MyCalendar/Common/SysParams/WfParams）+ `cmd\synergy\` 13 命令（流程字段/下拉项/版本/报表参数/宽度/启停）。

**导入导出全链路**
- `portalimport`：UploadFile → CheckData → CheckPageDuplication(查重) → Preview → `PortalImportCmd`(90KB) → SaveData；`portalexport`：`PortalExportDataBiz`(28.6KB)。

**同步能力族**
- `portalsetting\{SynchronizeCmd,LayoutSynchronizeCmd}`、`accountmore\{SynchronizeFontSizeCmd,SynchronizeLayoutCmd}`、`freqmenu\SyncCommonMenuCmd`、`mportalpage\PublishToMobileCmd`。

**其他**
- 快速搜索维护+共享：`quicksearch\maintenance\*` + `portal_quicksearch`。
- 插件管理：`plugin\maintenance\*`。
- 非标功能挂载+升级：`unstandardfunction\*`（hp_nonstandard_function_info/hp_server_info）+ `DoUpgradeCmd`。
- 应用/模块使用统计：`appcenter\{GetAppUseCountCmd,GetModuleMaintenanceCountCmd}`、`menu\SetTopMenuStaticticsCmd`。
- 移动门户整套 M*：mportalpage / mloginpage / melementstylelib / mnavstylelib + MobilePortalRight。

**明确不是后端能力**：主题 less/scss 编译（无 compile/less/scss 命中）、门户静态化 HTML 生成（无 createHtml 命中）、Redis/分布式缓存（仅 StaticObj 进程内，无 CacheUtil/Redis 引用）。

### 13.5 前端遗漏功能清单（§12 未覆盖）

**全局设置/配置**
- `portalsetting` 门户应用设置（全局单例，不绑定 hpid）：首页自动刷屏（整页/指定元素 + 间隔分钟）、RSS 读取类型（客户端/服务器）、记住上次访问首页、显示无查看权限元素、隐藏"我的门户"；菜单频率自动排序（同步到所有人）、默认展开左侧菜单、顶部菜单显示全称、常用菜单（白/黑名单 WeaPortalShare）。
- `portalconfig` 配置项管理（ccode/vvalue/note 只读名称 + 可改值）。
- `accountmore` 个人通用设置菜单（默认字体 大/中/默认、布局类型）。
- `quicksearchmanage` 快捷搜索管理（总开关 白/黑名单 + 类型表 module/name/url/图标）。
- `toolbarmore` 工具栏"更多"菜单（可拖拽排序表 `WeaTableEditable`）。
- `pluginmanage` 插件管理（pid/文件路径可拖拽插件登记表）。

**主题管控三件套**
- `themestyle` 样式指定：themeType(ecology9/ecology8/ts)/themeId/styleId/hpbgcolor(门户背景色)/iscolor/istitle。
- `themeappoint` 批量设置：按人员范围指定主题(setAppointThemeOfPersonnelScope)、按主题整体迁移(setAppointThemeOfThemeId)。
- `themesearch` 使用查询：查人员当前主题 / 查主题使用人员(getAllHrmOfTheme) —— 构成"指定 → 批量 → 反查"闭环。

**门户对象族**
- `tsportal` 特色门户（专题门户 + WeaPortalShare 共享 + 云商店导入）。
- `loginportal` 登录门户模板库（E9/E8/E7/Ecology8/H2/H/site 多版本；ECustom 跳 static4layout 布局编辑 `layoutType=login`；preview=`/wui/index.html?preview=true&loginTemplateId=`）。
- `loginpage` 登录前页面库 / `mainpage` 登录后页面库（mainpage 按机构树维度 + 排序 + 共享 + 预览）。
- `defaultportal` 默认首页设置：左侧 OrgTree → 点机构打开门户选择 → 指定该机构登录后默认首页（**区别于"门户按机构分配"**）。

**门户菜单全族**
- `portalmenu`：LeftMenu(前端)/TopMenu(后端)/CustomMenu(自定义)/**ReportMenu(报表菜单)** 四类。
- `menustylelib` 菜单样式库：横向 menuh / 纵向 menuv 两套。

**元素生态闭环**
- `developguide` 开发指南：下载元素开发模板 `hpElTemplate.zip` → 跳元素注册。
- `elementregister` 元素注册：上传 `.zip` 元素包注册自定义元素（命名/范围校验）。
- `elementtemplate` 元素模板库（另存为模板 + 预览 + 批量删除）。
- `elementstylelib` 元素样式库（卡片网格 + 增删改 + 导入）。
- `headlinestyle` 头条样式库（头条/大图类元素展示样式）。

**素材/模板库**
- `newstemplate` 新闻模板库（内部/外部两类排版模板，新建/导入/预览）。
- `materiallib` 图片素材库（多选，复用 `weaPortalPublic/MaterialLib`）。

**协同区（深描）**
- `synergy`：知识协同(Synergy4Doc)/流程协同(Synergy4Wf)/流程表单协同(Synergy4WfForm，按 wfid/modeid/formid 定位并读流程字段)/流程节点协同(Synergy4WfNode)；宽度设置(getWidth/setWidth)、启用/禁用(setIsUse)、新建页 vs 查看处理页两种页面。

**应用中心补遗**
- `appcenter`：应用宫格 + 非标功能管理(WeaNonStandard, 仅 sysadmin) + 通用设置入口(infoId 10078) + 使用统计(前五应用排名/今日维护操作 Echart 柱状图，受 `ThemeConfig.properties` 的 `backend.appcentershow` 开关控制)。

**移动门户细分**
- `mloginpage` 移动登录页面（预览/设置/编辑/另存为/保存）。
- `mobileportal/mportalpage` 移动门户页面：元素类型含快捷入口、排行榜(含 SQL 模板管理)、时间线、报表表单、待办数、图片；可设样式/共享/从主门户导入。
- `melementstylelib` / `mnavstylelib` 移动元素/导航样式库。

### 13.6 对 §12 的修正汇总
| §12 原表述 | 修正 |
| --- | --- |
| 后端极薄，仅 2 个统计接口 | 后端是全量 `/api/portal/*` REST（115 个 JAX-RS 资源），PortalEngineAction 只是特例 |
| 门户引擎 = 把配置表包装成可视化界面 | 成立；且后端用 ORM 实体重新包装旧 `hp*` 表，并外挂 `portal_*` 与 `hp_grid_layout` 新表 |
| §11.8 portalGridLayout 表名待确认 | 已确认 = `hp_grid_layout`(+`hp_grid_layout_element`) |
| 协同仅"二字" | 知识/流程/流程表单/流程节点四类协同 + 宽度/启停/页面类型 |
| 应用中心仅名词 | 含非标功能管理 + 使用统计图表 + 开关 |
| 门户菜单 | 实为前端/后端/自定义/报表四类 + 样式库 + 工具栏/插件/快捷搜索 |
| 移动门户 | 含移动登录页 + 移动门户页面元素内容体系 + 移动元素/导航样式库 |
| 缺：默认首页/登录门户/特色门户/主题三件套/全局设置/元素注册/素材模板库 | 均为遗漏，见 §13.5 |

### 13.7 修正后的门户引擎完整功能版图
门户引擎 = **前端 SPA(portal4engine, 33 模块) + 后端 REST(`/api/portal/*`, 115 资源)**；横切框架含：统一事务(PortalTransCommand) / 业务审计(PortalLogInfo) / 细粒度权限(PortalRight 13 码 + 反射策略) / 菜单缓存(MenuCache) / i18n / 多分支机构。功能覆盖：
- **门户对象**：主门户 / 登录门户 / 特色门户 / 登录前后页面 / 移动门户（含移动登录页）；
- **分配**：按机构 + 默认首页(按机构指定) + 维护者 + 共享 + 登录门户定时启用；
- **外观**：主题库 / 主题个性化中心 / 主题三件套(指定/批量/反查) / 元素·菜单·头条样式库；
- **布局**：hplayout(实例) / pagelayout(模板) / hp_grid_layout(新版网格)；
- **元素**：库 / 模板 / 样式 / 头条 / 注册(zip) / 自定义元素族(8类) / SQL 取数 / HD 面板；
- **菜单**：前端/后端/自定义/报表 + 常用/工具栏/右键/邮件 + 样式库；
- **素材**：新闻模板 / 图片素材库；
- **协同**：知识/流程/表单/节点；
- **导入导出**：全链路(上传/查重/预览/落地)；
- **同步族** / **全局设置**(portalsetting/portalconfig/accountmore/quicksearch/plugin) / **桌面浮动栏** / **统计·日志·非标功能**。

> 一句话：门户引擎是一个高度完整的门户 PaaS；**blade-portal 当前只实现了其中"主门户 + 元素 + 共享 + 拖拽设计"的最小闭环**，其余（默认首页、登录门户、主题体系、菜单族、元素注册、素材模板库、协同、导入导出、REST 化、审计/权限横切）均为可对标补齐的方向。

### 13.8 专题：登录前门户 vs 登录后门户

E-cology 的"登录前/后门户"横跨**模板层（portalmaintenance）**与**页面层（portalpage）**两组对象（§12.2 已列、§13.4/§13.5 已分散提及），此处集中对比：

| 维度 | 登录前门户 | 登录后门户 |
|---|---|---|
| 模板（portalmaintenance） | `loginportal` 登录门户模板库 | `mainportal` 主门户模板库（★核心） |
| 页面（portalpage） | `loginpage` 登录前页面库 | `mainpage` 登录后页面库 + `defaultportal` 默认首页设置 |
| 用途 | 未登录 / 登录界面展示的门户 | 登录后工作台首页 |
| 模板类型 | E9/E8/E7/Ecology8/H2/H/site 多版本；`ECustom` 跳 `static4layout` 布局编辑（`layoutType=login`） | ecology8 / ecology9 / custom / ecologyBasic / ecology7 / office |
| 预览 | `/wui/index.html?preview=true&loginTemplateId=` | 对话框内 `E8Theme`/`E9Theme` 或 iframe `templatePreview.jsp` |
| 特色能力 | **定时启用**（`systemlogintemplate.openTimer` + `timerStartDate/timerEndDate` + `CheckUniqueTimerCmd` 时段冲突校验）；图片上传素材库 | 按机构分配 + 默认首页（OrgTree 指定）+ 维护者 + 共享 + 外观/主题设置 |
| 后端 REST | loginportal 相关 Action + openTimer 校验 | `MainPortalAction` `/portal/mainPortal`（`checkUserRight`） |
| 移动对应 | `mloginpage` 移动登录页面 | `mportalpage` 移动门户页面 |

**关键区分（易混淆）**
- **登录后门户 = `mainportal`(模板) + `mainpage`(页面) + `defaultportal`(默认首页)** 三件套：`mainportal` 管模板/外观，`mainpage` 才是登录后按机构真正渲染的页面实例，`defaultportal` 决定"进哪个首页"。
- **登录前门户 = `loginportal`(模板) + `loginpage`(页面)**，且支持**按时间段自动切换**（`openTimer`）——这是登录后门户没有的能力。
- `defaultportal`（按机构指定默认首页）≠"门户按机构分配"（`mainportal` 的 `orgType`）：前者决定"登录后进哪个首页"，后者决定"这个门户给哪些机构用"。

**blade-portal 对照**：当前只有"登录后"单一形态（`/portal/home` 单默认页 + `blade_portal_page`）；登录前门户、多版本模板、登录门户定时启用、登录前/后页面库均未实现（见《blade-portal-门户引擎设计说明.md》§10.3 路线图 P2/P3）。

> blade-portal 相对「旧渲染核心 / 新 REST 核心」的**取舍结论与能力补齐路线图（P1/P2/P3）**见《blade-portal-门户引擎设计说明.md》§10。

---

## 14. 补充分析（第六轮）：门户菜单（portalmenu）逐模块深度分析

> 探查对象：`D:\Weaver2020\ecology\src4js\pc4backstage\portal4engine\`（前端）、`e:/project/ecology/ecology/src/com/engine/portal/`（后端 `web/`+`cmd/`）、`D:\Weaver2020\ecology\data\`（表结构脚本）、`D:\Weaver2020\ecology\portal\` 与 `admincenter\portalEngine\`（JSP 壳）。
> 背景：§12.2 / §13.5 只把 `portalmenu` 当「一行模块名罗列」，没有分析每个子模块的实际功能、REST、表、文件。本轮逐模块深扒补齐。所有结论均带 `文件:行` 或 `data/*.sql` 证据。

### 14.1 核心结论：门户菜单 = 系统 LeftMenu/MainMenu 的可视化维护界面（非独立库）

铁证：
- `cmd/portalmenu/maintenance/GetMenuTreeDataCmd.java:51-52` 写明 `menuType：前端菜单（left）、后端菜单（top）`；`:92` 直接用系统核心类 `weaver.systeminfo.menuconfig.MenuUtil(menuType, resourceType, resourceId, languageId)`——这正是 §2「系统 LeftMenu 导航菜单」的同一套引擎。
- `cmd/menu/GetFrontEndMenuCmd.java:78`、`GetBackEndMenuCmd.java:56` 同样用 `MenuUtil`，返回 `infoId/labelId/customName/parentId` 与 `LEFTMENUINFO/MAINMENUINFO` 字段一一对应（见 §14.2 表定义）。
- 升级脚本 `data/.../sql201808270303.sql:15-25` 把 `MainMenuInfo.id 807/797/806/798/10334/11044` 的 `fullrouteurl` 直接指向 `portalengine/portalmenu/leftmenu|topmenu|custommenu|menustylelib|toolbarmore|pluginmanage`——证明这些门户子页就是系统菜单表的维护前端。

**分级机制**：同一套表靠 `menuType(left/top)` + `resourceType/resourceId`（总部=1 / 分部=2 / 个人=3）+ `*MENUCONFIG` 表的 `RESOURCEID/RESOURCETYPE` 列实现总部/分部/个人三级定制（`LEFTMENUCONFIG:29452-29453`、`MAINMENUCONFIG:30928-30929`）。

**门户专属新增的只是辅助表**（非菜单主数据）：`TOOLBARMOREMENUINFO`、`ACCOUNTMOREMENUINFO`、`hpMenuStyle`、`hppluginsetting`、`quick_search_types`、`PortalThemeConfig(showquicksearch)`、遗留 `menucustom`、共享维表 `commonshareinfo`。报表菜单（reportMenu）与后端菜单（top）共用 `MAINMENUINFO`，仅 `menutype=report`、根节点 id=10。

---

### 14.2 子模块逐功能深扒（功能 / 后端接口 / 表 / 前端文件）

#### A. leftmenu（前端菜单维护）
- **功能**：管理员按「总部 / 分部 / 个人」三级维护系统左侧导航菜单：菜单树查询、增删自定义项、排序、显隐、改父级、统一设置下级分部、同步；`isCustom=true` 转个人（`resourceType=3`）个性化。
- **后端接口**：`web/PortalMenuMaintenanceAction.java`（`@Path("/portal/portalMenuMaintenance")` 类:38）；`getRightStatus:112`、`getMenuTreeData:137`、`setMenuTreeData:204`、`sortMenuTreeData:238`、`getSystemMenu:272/setSystemMenu:305`、`getCustomMenu:383/setCustomMenu:417/deleteCustomMenu:451`、`unifySetting:485`、`synchronizeMenu:518`、`setParentMenu:551`、`GetMenuNameByMenuid:584`。命令类：`cmd/portalmenu/maintenance/{Get,Set,Sort}MenuTreeDataCmd`、`Get/SetSystemMenuCmd`、`Get/Set/DeleteCustomMenuCmd`、`SetParentMenuCmd`、`SynchronizeMenuCmd`、`GetMenuNameByMenuidCmd`。
- **表（系统表）**：`LEFTMENUINFO`（`data/init/Oracle/2.tables.sql:29467`：`ID,LABELID,ICONURL,LINKADDRESS,MENULEVEL,PARENTID,USECUSTOMNAME,CUSTOMNAME,RELATEDMODULEID,ISCUSTOM,FROMMODULE,MENUTYPE,DISPLAYUSAGE,SELECTEDCONTENT,ROUTEURL,FULLROUTEURL,MOBXROUTEURL,ICONCLASSNAME`）、`LEFTMENUCONFIG`（`:29445`：`ID,USERID,INFOID,VISIBLE,VIEWINDEX,RESOURCEID,RESOURCETYPE,LOCKED,LOCKEDBYID,USECUSTOMNAME,CUSTOMNAME(_E/_T),TOPMENUNAME`）；辅助 `menuunifyinfo`（`GetMenuTreeDataCmd.java:220`）、`menuparams`（`GetCustomMenuCmd.java:133`）。
- **前端**：`components/portalmenu/PortalMenu.js`、`PortalMenuTree.js`(33KB)、`PortalMenuEdit4System.js`、`PortalMenuEdit4Custom.js`、`PortalMenuSync.js`、`PortalMenuBatchEditParent.js` + `stores/portalmenu/PortalMenuStore.js`（调 `/api/portal/portalMenuMaintenance/*` 与 `/api/portal/portalMenuShareMaintenance/*` 的 `getMenuShares/setMenuShares/deleteMenuShares`）。

#### B. topmenu（后端菜单维护）
- **功能**：与 leftmenu 完全对称，仅 `menuType="top"`，维护顶部/后端菜单树（报表、系统设置等），同样支持总部/分部/个人分级与统一设置。
- **后端接口**：复用 `PortalMenuMaintenanceAction`，靠传入 `menuType=top` 区分；`GetMenuTreeDataCmd.java:51` 明确左右菜单类型，`:92` 用 `new MenuUtil(menuType, resourceType, resourceId, languageId)`。
- **表（系统表）**：`MAINMENUINFO`（`2.tables.sql:30943`：`ID,LABELID,MENUNAME,LINKADDRESS,PARENTFRAME,DEFAULTPARENTID,DEFAULTLEVEL,DEFAULTINDEX,NEEDRIGHTTOVISIBLE,RIGHTDETAILTOVISIBLE,NEEDRIGHTTOVIEW,USECUSTOMNAME,CUSTOMNAME,RELATEDMODULEID,ICONURL,ISCUSTOM,PARENTID,MENULEVEL,BASETARGET,MODULE,TOPICONURL,TOPMENUNAME,MENUCSS,MENUTYPE,DISPLAYUSAGE,ROUTEURL,FULLROUTEURL`）、`MAINMENUCONFIG`（`:30921`，结构同 LEFTMENUCONFIG）；另有遗留 `MAINMENUINFO70`（`:30996`）。
- **前端**：复用 leftmenu 的 `PortalMenuTree.js`/`PortalMenuStore.js`，靠 `menuType` 切换。升级脚本 `sql201808270303.sql:17` 把 `MainMenuInfo id=797` 指向 `/portalengine/portalmenu/topmenu`。

#### C. custommenu（自定义菜单）
- **功能**：在系统菜单体系内针对某组织/个人维护自定义项（增删改名称/图标/链接/父级）。两套含义：① 门户引擎自定义项（`ISCUSTOM=1` 条目 + config 的 per-resource 自定义名）；② E8 遗留 `menucustom` 个人自定义表。
- **后端接口**：`PortalMenuMaintenanceAction.getCustomMenu:383/setCustomMenu:417/deleteCustomMenu:451`；命令 `cmd/portalmenu/maintenance/GetCustomMenuCmd.java:53` 用 `new MenuMaint(menuType, resourceType, resourceId, languageId)`，`:67` 注释「parentId 为 10 时父菜单为报表菜单」，`:76` 直接读 `leftmenuconfig/mainmenuconfig`；另有 `cmd/custommenu/maintenance/{Get,Set,Delete}CustomMenusCmd` 处理遗留自定义菜单。
- **表**：主数据仍是 `LEFTMENUINFO/MAINMENUCONFIG` 或 `MAINMENUINFO/MAINMENUCONFIG`，自定义项由 `ISCUSTOM` 标志（`2.tables.sql:29479`/`:30968`）与 config 表 `USECUSTOMNAME/CUSTOMNAME` 区分；遗留 `menucustom`（`sql2009061903.sql:95` Oracle / `:41` SQLServer：`id,menuname,menuicon,menuhref,menutarget,menuparentid,menuindex,menutype`）。
- **前端**：`components/portalmenu/PortalMenuEdit4Custom.js`(22.97KB) + `PortalMenuStore.js`（`getCustomMenu 649/setCustomMenu 875/deleteCustomMenu 910`）。`MainMenuInfo id=806 → /portalengine/portalmenu/custommenu`（`sql201808270303.sql:19`）。

#### D. reportMenu（报表菜单）
- **功能**：维护「我的报表」报表类菜单（本质是 `menutype=report` 的子集，根节点 id=10），门户提供勾选/树维护界面。
- **后端接口**：勾选树 `GetMenuTreeDataCmd.java:52` `isReportFormsMenu` → `:59` `mdb.getAllReportMenu(user,"10",params)`；数据源 `com/engine/portal/biz/menu/MenuDataBiz.java` `getReportMenu:106`/`getAllReportMenu:202`（均 `new MenuUtil("top","report",3,user.getUID(),...)`，根 `infoId="10"`）。
- **表**：无独立报表菜单表，复用 `MAINMENUINFO/MAINMENUCONFIG`（`menutype=report`）。
- **前端**：`components/portalmenu/ReportMenu.js`；`PortalMenuTree.js` 内按 `isReportFormsMenu` 分支处理。

#### E. menustylelib（菜单样式库）
- **功能**：管理横向 `menuh` / 纵向 `menuv` 菜单样式：新增、编辑（H/V 两种编辑器）、删除、导入、导出、预览。
- **后端接口**：`web/MenuStyleLibAction.java`（`@Path("/menuStyleLib")`）；`getMenuStyles:103`、`getMenuStyleList:129`、`getMenuStyle:154`、`addMenuStyle:179`、`updateMenuStyle:205`、`deleteMenuStyle:231`、`uploadMenuStyle:261`、`downloadMenuStyle:378`；命令 `cmd/menustylelib/*`。
- **表**：`hpMenuStyle`（`GetMenuStylesCmd.java:38` `select styleid,menustylename,menustyledesc,menustyletype,menustylecreater,menustylelastdate,menustylelasttime from hpMenuStyle`；`MenuStyleLibAction.java:349` 含 `menustylemodifyid,menustylecite`；`menustyletype` 区分 `menuh/menuv`）。
- **前端**：`components/menustylelib/{Add,EditH,EditV,Preview,Upload,index}.js` + `stores/menustylelib/MenuStyleLibStore.js`（`/api/portal/menuStyleLib/*`：`getRightStatus 50/getMenuStyles 67/getMenuStyle 100/updateMenuStyle 180/getMenuStyleList 211/addMenuStyle 262/deleteMenuStyle 306/downloadMenuStyle 383`）。

#### F. toolbarmore（工具栏「更多」菜单）
- **功能**：维护工具栏「更多」下拉项（系统项 + 自定义项）：增删、可拖拽排序（`WeaTableEditable`）、显示位置（上/下）、使用限制（共享）。
- **后端接口**：`/api/portal/toolbarMoreMaintenance/*`（`getRightStatus,getMenus,saveMenus,deleteMenus,getShares,setShares,deleteShares`）；命令 `cmd/toolbarmore/maintenance/{Get,Save,Delete}MenusCmd`、`cmd/toolbarmore/GetToolbarMoreMenuCmd.java`；权限 `PortalRight.TOOLBAR_MENU → "Portal:toolBarMenu"`（`PortalRight.java:20`）。
- **表**：`TOOLBARMOREMENUINFO`（`2.tables.sql:43697`：`id,labelid,customname,icon,isshow,url,linkmode,type,shoposition,sortnum,isshowtoolbar,stype`；初始 `type='sys'`，`:28-30`）；使用限制共享走 `commonshareinfo`。
- **前端**：`stores/toolbarmore/ToolbarMoreStore.js`（`getRightStatus 31/getMenus 45/saveMenus 74/deleteMenus 110/getShares 125/setShares 136/deleteShares 154`）+ `components/toolbarmore/`、`components/portalthemelib/e9theme/E9ToolBarMore.js`。

#### G. pluginmanage（插件管理）
- **功能**：登记 / 启用门户插件：`pid`（插件标识）、描述、文件路径、是否启用、排序、类型 `pType`。
- **后端接口**：`web/PluginMaintenanceAction.java`（`@Path("/pluginMaintenance")`）：`getPlugins/savePlugins/deletePlugins`；前端 `PluginManageStore.js`（`getPlugins 35/savePlugins 64/deletePlugins 100`）；命令 `cmd/plugin/maintenance/*` + `cmd/plugin/GetPluginsCmd.java`。
- **表**：`hppluginsetting`（`GetPluginsCmd.java:38` `select pid,plugindesc,filepath,isuse,ordernum,pType from hppluginsetting order by ordernum asc`）。
- **前端**：`components/pluginmanage/index.js`、`stores/pluginmanage/`。`MainMenuInfo id=11044 → /portalengine/portalmenu/pluginmanage`（`sql201808270303.sql:25`）。

#### H. quicksearchmanage（快捷搜索管理）
- **功能**：快捷搜索总开关 + 搜索类型（白/黑名单）维护 + 各类型使用限制。
- **后端接口**：`web/QuickSearchMaintenanceAction.java`（`@Path("/quickSearchMaintenance")`）；`getRightStatus:62`、`getQuickSearchTypes:87`、`saveQuickSearchTypes:117`、`deleteQuickSearchTypes:147`、`getQuickSearchTypesShares:177`、`saveQuickSearchTypesShares:207`、`deleteQuickSearchTypesShares:237`；权限 `MAIN_PORTAL → "SystemTemplate:Edit"`（`:42`）；命令 `cmd/quicksearch/maintenance/*`。
- **表**：总开关 `PortalThemeConfig.showquicksearch`（`GetQuickSearchTypesCmd.java:42`；建字段 `sql201907313003.sql:3`）；类型表 `quick_search_types`（`GetQuickSearchTypesCmd.java:48`：`id,module,type,label_id,custom_name,iconType,iconFrom,icon,iconImgSrc,url,linkmode,is_use,is_sys,order_num`）；类型使用限制 `commonshareinfo(module='qs')`（`GetQuickSearchTypesCmd.java:81`）；早期外观 `portal_quicksearch`（`sql201812120503.sql:59`：`id,defaultcon,radius,bgColor`）。
- **前端**：`components/quicksearchmanage/`、`stores/quicksearchmanage/QuickSearchManageStore.js`（`getRightStatus 21/getQuickSearchTypes 33/save 57/delete 75`）+ `components/portalthemelib/e9theme/E9QuickSearch.js`。

#### I. accountmore（个人通用设置）
- **功能**：个人头像「更多」菜单设置：菜单项显隐（`ACCOUNTMOREMENUINFO`）+ 全局默认字体、布局类型（`layouttype`），并支持把字体/布局「同步到所有人」。
- **后端接口**：`/api/portal/accountmore/*`（`getRightStatus 20/getAccountMoreSetting 32/synchronizeFontSize 56/synchronizeLayout 77/setAccountMoreSetting 100`）。
- **表**：`ACCOUNTMOREMENUINFO`（`2.tables.sql:1` / `sql201903182403.sql:1`：`id,labelid,customname,icon,isshow,url,linkmode,type,shoposition,sortnum,stype`；初始 `sType` 含 `font,language,custom,password,theme,layout`，`:15-25`）；全局默认字体/布局同步多写入用户级设置表。
- **前端**：`components/accountmore/`、`stores/accountmore/AccountMoreStore.js`。

#### J. freqmenu（常用菜单）
- **功能**：① 按点击次数排序（点击统计表驱动）；② 常用菜单同步——把管理员默认常用菜单（`userid=1`）同步给指定分部的用户。
- **后端接口**：展示 `cmd/commonmenu/GetCommonMenuJsonCmd.java`（用 `MenuUtil("left",3,...).getCommonMenuRs`，`:40-43`）、`GetCommonMenuListCmd`、`SaveCommonMenuCmd`；同步 `cmd/freqmenu/SyncCommonMenuCmd.java`（`:70-106` 把 `hpCommonMenu where userid=1` 同步给 `subcompanyid1 in(ids)` 用户，并同步 `menuparams`）；统计 `GetFrontEndMenuCmd.byClickCntSort` 读 `HrmUserMenuStatictics`。
- **表**：`UserCommonMenu`（`GetCommonMenuJsonCmd.java:129,135`）、`UserCommonMenuInit`（`:132,136`）、`UserCommonMenuDefault`（`:135`）、`hpCommonMenu`（`SyncCommonMenuCmd.java:76,89,90`，`syspagemenuid`）、`HrmUserMenuStatictics`（`2.tables.sql:24791`：`USERID,MENUID,CLICKCNT`；`GetFrontEndMenuCmd.java:336`）、`menuparams`（`menutype='freqmenu'`，`SyncCommonMenuCmd.java:79,104`）。
- **前端**：顶部菜单运行时调用 `GetCommonMenuJsonCmd` 渲染常用菜单（无独立 portalmenu 子页组件）。

#### K. 右键菜单 / 邮件菜单 / 前端后端菜单统计
- **K1 rightclickmenu（门户元素右键菜单）**：命令 `cmd/rightclickmenu/GetRCMenuListCmd.java:28`、`GetHpSetingMenuCmd`；服务 `RightClickMenuServiceImpl`；动态拼装门户元素右键菜单（收缩全部/设置元素/隐藏元素库等），无独立表，依据 `RightClickMenu.java` + 系统配置 `systemmenu.userightmenu` 构建（`:191` `RightClickMenu.getIconPath`）。
- **K2 portalrcmenu（门户右键菜单·同步）**：`web/PortalRCMenuAction.java` `@Path("/portal/portalrcmenu")`（门面 `com/api/portal/engine/PortalRCMenuAction.java:16`）；命令 `cmd/portalrcmenu/UpdateSyniHp.java`、`CompleteSyniHp.java`；接口 `updatesynihp POST`（`PortalRCMenuAction.java:40`）、`completesynihp POST`（`:60`）——门户右键「更新同步/完成同步」HP 自定义菜单同步，无独立表。
- **K3 EmailMenuAction（邮件菜单）**：构造「我的邮件」左侧菜单 JSON（写信/内部邮件/外部邮件/接收等）+ 未读邮件计数；运行时数据，无专属维护表（依据 `Mailconfigureinfo` 内外邮件开关动态构造）。接口 `@Path("/portal/emailmenu")`；`getEmailMenuJson POST /emailmenu`（`:35`）、`unreadCountBymenu GET /unreadCount`（`:401`）。证据 `EmailMenuAction.java:48` 邮件菜单 id `536`，`:57` 写邮件 SPA 路由 `#/main/email/new`。
- **K4 GetFrontEndMenuCmd / GetBackEndMenuCmd（前端/后端菜单构建与统计）**：`cmd/menu/GetFrontEndMenuCmd.java` `menuType="left"`（`:52-54`），`:78` `MenuUtil("left",3,user.getUID(),...)`，`:195-197` `byClickCntSort` 按 `HrmUserMenuStatictics` 排序，`:256-260` 注入「我的门户/我的报表/新闻公告」子项；`cmd/menu/GetBackEndMenuCmd.java:56` `MenuUtil("top",3,...)`、`:58` `getAllMenuRsWithShareRight`；门面 `cmd/portalmenu/GetFrontEndMenuCmd.java`、`cmd/portalmenu/GetBackEndMenuCmd.java`。表：前端用 `LEFTMENUINFO/LEFTMENUCONFIG`，后端用 `MAINMENUINFO/MAINMENUCONFIG` + `HrmUserMenuStatictics`（排序）。

---

### 14.3 共享 / 权限机制（门户菜单族通用）

- **权限码**（`PortalRight.java`）：`COMPANY_MENU → "HeadMenu:Maint"`（总部菜单，`:18`）、`SUB_COMPANY_MENU → "SubMenu:Maint"`（分部菜单，`:19`）、`TOOLBAR_MENU → "Portal:toolBarMenu"`（工具栏，`:20`）、`MAIN_PORTAL → "SystemTemplate:Edit"`（快捷搜索）。`PortalMenuMaintenanceAction.checkUserRight`（`:47-92`）区分总部/分部，并支持「门户管理分权」（`ManageDetachComInfo.isUsePortalManageDetach`，`:68/81`）。
- **共享 / 使用限制**：菜单、工具栏、快捷搜索的 `getShares/setShares/deleteShares` 统一落到 `commonshareinfo` 表，按 `module` 区分（快捷搜索 `module='qs'`，`GetQuickSearchTypesCmd.java:81`；门户菜单共享由 `PortalMenuShareMaintenanceAction` 调用）；支持按分部/角色/`sharetype` 等粒度授权。
- **缓存**：`weaver/portal/cache/MenuCache.java` 按用户缓存 `FrontMenu`/`BackMenu`（`CACHE_KEY_FRONTMENU/BACKMENU`，`:27-30`）；`clearMenuCache`（`:108`）按 `isCustom + menuType` 决定清前端/后端侧；`isOpenMenuCache()` 默认返回 `false`（`:143`，特殊客户才开）。

### 14.4 对原文档 §2 / §12 / §13 的修正与打通

| 原文档表述 | 修正/打通 |
|---|---|
| §2 把 LeftMenu 当「系统导航菜单」单独讲，未与门户引擎关联 | 门户引擎的 `leftmenu/topmenu/custommenu/reportMenu` 维护的**正是**同一套 `LEFTMENUINFO/MAINMENUINFO` + `*MENUCONFIG` 表，只是换了一套 React 维护界面（§14.1 铁证） |
| §12.2 / §13.5 仅罗列 portalmenu 子模块名 | 本轮 §14.2 逐模块补齐「功能/接口/表/前端文件」 |
| §13.5 称 portalmenu「四类」 | 实为 9+ 个子模块（leftmenu/topmenu/custommenu/reportMenu/menustylelib/toolbarmore/pluginmanage/quicksearchmanage/accountmore + freqmenu/rightclickmenu/portalrcmenu/emailmenu 等配套） |
| 未说明门户专属菜单辅助表 | 补：`TOOLBARMOREMENUINFO`、`ACCOUNTMOREMENUINFO`、`hpMenuStyle`、`hppluginsetting`、`quick_search_types`、`PortalThemeConfig(showquicksearch)`、遗留 `menucustom`、共享维表 `commonshareinfo` |

> 一句话：门户菜单是**系统导航菜单（LeftMenu/MainMenu）的可视化治理层**，主数据仍是 §2 那套表；门户引擎只是补了一套 React 维护界面 + 一批门户专属的辅助表（工具栏更多、个人更多、样式库、插件、快捷搜索）。这解释了为何文档 §2 的 LeftMenu SQL 与门户引擎维护接口操作的是同一批数据。

---

## 15. 补充分析（第七轮）：门户页面/元素/素材库/主题/移动门户/应用中心+协同 逐模块深度分析

> 探查对象：`src4js/pc4backstage/portal4engine/`（前端）、`com/engine/portal/`（后端）、`com/engine/portalTs/`（主题独立包）、`com/engine/mobilemode/`（移动端）、`data/`（表结构）。背景：§12.2 / §13.4 / §13.5 仅罗列这些分栏名，未深扒。本轮逐模块补齐（带 `文件:行` 证据）。
> 路由说明：后端类级 `@Path("/portal/xxx")` → 前端 `/api/portal/xxx/...`；**无类级 @Path 的类按类名（去 Action、小写）路由**（如 `ThemeStyleAction`→`themeStyle`、`SynergyAction`→`synergy`、`MLoginPageAction`→`mloginpage`）；主题指定/反查在独立包 `com.engine.portalTs`，前端走 `/api/portalTsLogin/utils/*`。

### 15.1 核心结论：各分栏都是对 §3 模型的治理层（极少新建主表）

1. **portalpage / portalelement 治理的是 §3 核心表**：门户页主表实际是 `HPINFO`（§3 写作 `homepage(hpid)`，`hpid`=hpinfo.id，证据 `GetTreeDataCmd.java:54` `hpinfo c on b.hpid=c.id`）；元素主表 `HPELEMENT` + 用户级 `HPELEMENTSETTINGDETAIL`。
2. **新增独立表只服务新能力**：模板布局库 `PAGELAYOUT`、网格布局 `HP_GRID_LAYOUT(+_ELEMENT)`、新闻模板 `PAGENEWSTEMPLATE(+LAYOUT)`、元素模板 `HPELEMENTTEMPLATE`、默认首页映射 `HPDEFAULSETTING`。
3. **三套布局体系**：`HPLAYOUT`(实例布局) / `PAGELAYOUT`(模板布局库，可 zip 导入导出/缩略图) / `HP_GRID_LAYOUT(+_ELEMENT)`(新增网格布局，CLOB 配置、可启用/默认入口)。
4. **素材库图片部分不走 DB**：`materiallib` 完全基于文件系统（目录/文件），无关系表。
5. **移动门户是独立 REST+表**（不混用 PC 的 hpinfo/hpelement），且**与「移动建模」是两套独立引擎**——复用 PC 建模 `modehtmllayout` 的是移动建模（AppHomepage/MEC），而非移动门户（详见 §20）。
6. **协同 synergy 真以 `hpid` 操作旧门户表**（`hpinfo_workflow`、`hpLayout` 等同源），证实 §10.4/§13.4；协同专用表是 `synergy_base`/`synergyconfig`/`sypara_*`。

### 15.2 门户页面 portalpage 族（治理 HPINFO）

- **A1 loginpage（登录前页面库）**：维护 `HPINFO` 中 loginpage 维度的门户页。后端 `web/LoginPageAction.java`（`@Path /portal/loginPage`，`getLoginPages`）+ 统一保存入口 `web/PortalInfoAction`（`/api/portal/portalinfo/operate`，`LoginPageStore.js:79/120`）；表 `HPINFO`（`2.tables.sql:19854`，无独立表）；前端 `components/loginpage/`。
- **A2 mainpage（登录后页面库）**：按机构树维度维护，启用/默认/排序/维护者/共享/预览。后端两套并行：`web/MainPortalAction.java`（`@Path /portal/mainPortal`，`getMps:131/saveMps:223/deleteMps:253/saveMainPortal:283/saveAsMainPortal:313/setMainPortal:373/unifyAppointTheme:397`，`:39`）+ 兼容 `web/MainPageAction.java`（`share:276/addShare:313/delShare:352`）；cmd `cmd/mainpage/*`（`AddMaint/AddShare/DeleteMaint/GetPortalTreeData/SavePortalTreeSort...`）；表仍是 `HPINFO`；前端 `stores/mainportal/MainPortalStore.js`、`stores/mainpage/MainPageStore.js`。
- **A3 defaultportal（默认首页设置）**：左侧 OrgTree→点机构→选门户→指定该机构登录后默认首页（区别于"门户按机构分配"）。后端 `web/DefaultPortalAction.java`（`@Path /portal/defaultportal`，`treedata:57/portallist:84/save:107`）+ `cmd/defaultportal/{GetTreeDataCmd,SaveDefaultPortalCmd}`；表 `hpdefualtsetting`（`2.tables.sql:19493`，仅 `SUBCOMPANYID,HPID`；表名实际拼写为 `hpdefualtsetting`，见 §18.3③；`SaveDefaultPortalCmd.java:50-54` 写）；前端 `stores/defaultportal/DefaultPortalStore.js`。
- **A4 portallayout（布局库：design/sys/cus）**：三套布局对应三个 Action：实例布局 `web/PortalLayoutAction.java`（`@Path /portal/portallayout`，`HPLAYOUT`）；模板布局库 `web/PageLayoutLibAction.java`（`@Path /portal/pageLayoutLib`，`PAGELAYOUT`，可 zip 导入导出/缩略图 `LAYOUTIMAGE`）；网格布局 `web/PortalGridLayoutAction.java`（`HP_GRID_LAYOUT(+_ELEMENT)`，CLOB 布局配置，可启用/默认入口）。表：`HPLAYOUT`（`2.tables.sql:19903`）、`PAGELAYOUT`（`:37257`）、`HP_GRID_LAYOUT`（`:20392`）+`HP_GRID_LAYOUT_ELEMENT`（`:20421`）。前端 `stores/portallayout/*`。

### 15.3 门户元素 portalelement 族（治理 HPELEMENT 两层模型）

- **B1 portalelements（元素库★）**：按 `ISUSE` 区分 使用中(1)/未使用(0)/封存(2)，向导式新增(Add4Step1~4)、引用关系查询(getReferences)、设置、日志、删除。后端 `web/PortalElementsAction.java`（`@Path /portal/portalelements`，`list:49/deletePortalElement:70/save:112/operate:140/portallist(引用):167/preview:194/getConfigData:263/setConfigData:289`）；表 `HPELEMENT`（`2.tables.sql:19510`：EID,HPID,EBASEID,STYLEID,STRSQLWHERE,TITLE,FROMMODULE,ISUSE,ISSYSELEMENT,ISLOCKED…）+ `HPELEMENTSETTING` + `HPELEMENTSETTINGDETAIL`（`2.tables.sql:19572`）；前端 `stores/portalelements/PortalElementsStore.js`（`isuse` 三态 `:23/56/297/328/450`；`getReferences :146-200`）。
- **B2 elementtemplate（元素模板库）**：另存为模板/预览/批量删除。后端 `web/ElementTemplateAction.java`（`@Path /portal/elementtemplate`，`list:48/save:75/preview:129/delete:155`）；表 `HPELEMENTTEMPLATE`（`2.tables.sql:19654`）；前端 `stores/elementtemplate/ElementTemplateStore.js`。
- **B3 elementstylelib（元素样式库）**：卡片/网格样式增删改/导入/引用查询。后端 `web/ElementStyleLibAction.java`（`@Path /portal/elementStyleLib`，`getElementStyles:102/addElementStyle:179/updateElementStyle:205/deleteElementStyle:231/upload:288/download:397`）；**复用 `HPMENUSTYLE` 且 `menustyletype='element'`**（`ElementStyleLibServiceImpl.java:52,70` `select … from hpMenuStyle where menustyletype='element'`），与系统 `HPSTYLE`（`2.tables.sql:20234`）不是同一张表。
- **B4 headlinestyle（头条样式库）**：头条/大图类元素展示样式。后端 `web/HeadLineStyleAction.java`（`@Path /portal/headlinestyle`，`list:42/operate:73/editinfo:107/saveedit:139`）；持久化表本趟未在 `data/` 确认（多为配置/XML 驱动），前端 `stores/headlinestyle/HeadLineStyleStore.js`。
- **B5 elementregister（元素注册）**：上传 `.zip` 元素开发包注册自定义元素（扩展名/唯一名/范围校验）。后端 `com/api/portal/backend/web/ElementRegisterAction.java`（`@Path /portal/elementregister`，`upload:28/list:63`）；注册后元素定义进 `HPELEMENT(EBASEID)`；前端 `components/elementregister/index.js`。
- **B6 developguide（开发指南）**：下载 `hpElTemplate.zip`→开发→跳元素注册。纯前端引导（`components/developguide/index.js:52/55` 下载 `/admincenter/portalEngine/hpElTemplate.zip` 并 push 到 `/portalengine/portalelement/elementregister`）。
- **B7 自定义元素命令族（8 类×操作）**：`cmd/customelement/{commonAbstract,companyNewsPerfect,customCompanyNews,customPage,img,rss,workflowPerfect}\{Find,Save,Update,Delete,Order,SaveOrUpdate}ItemCmd`；条目落各自定义元素配置表（`HPEXTELEMENT`/`HPCOMPANYNEWS*` 等），主定义归 `HPELEMENT`。
- **B8 SQL 取数 + HD 面板/时间轴**：`elementecodeaddon/GetSqlDataCmd`（`ElementEcodeAddonAction.getSqlData:38`）、`elementindie/GetElementIndieViewDataCmd`（`ElementIndieAction.elementinfo:45`）、`hdpanelelement/*`（`HDPanelElementAction`：`getEditDataSource:189/getCalendarBaseData:300/getAllManagerData:272`）、`timelineelementstyle/*`。
- **B9 元素工具栏拖拽/锁定**：`web/ElementToolbarAction.java`（`@Path /portal/elementtoolbar`，`delete:43/lock:78/unlock:104/drag:129`）+ `cmd/elementstoolbar/*`；`drag` 改写 `HPLAYOUT.AREAELEMENTS`，`lock/unlock` 改写 `HPELEMENT.ISLOCKED`。

### 15.4 素材库 materiallib 族

- **C1 newstemplate（新闻模板库）**：内部/外部两类排版模板新建/导入/预览/另存为/删除。后端 `web/NewsTemplateAction.java`（`@Path /portal/newsTemplate`，`getNewsTemplates:100/saveNewsTemplate:178/saveAsNewsTemplate:208/deleteNewsTemplates:126/setNewsTemplate:333/upload:260/download:444`）；表 `PAGENEWSTEMPLATE`（`2.tables.sql:37275`）+`PAGENEWSTEMPLATELAYOUT`（`:37288`）；前端 `stores/newstemplate/NewsTemplateStore.js`。
- **C2 materiallib（图片素材库）**：目录/文件管理、多选复用（`WeaMaterialLib.js` 被元素向导、移动端、登录页、菜单样式多处复用）。后端 `web/MaterialLibAction.java`（`@Path /portal/materialLib`，`getDirs:88/addDir:135/deleteDir:161/getFiles:187/uploadFile:282/downloadFile:422`）；**完全基于文件系统，无关系表**（`MaterialLibAction.java:87-97,185-197` 未见 insert）；前端 `components/materiallib/index.js`、`components/common/WeaMaterialLib.js`。

### 15.5 门户主题 portaltheme 族

- **A1 portalthemelib（主题库：custom/e7/e8/e9 + 导入 zip）**：后端 `web/PortalThemeLibAction.java`（`@Path /portal/portalThemeLib`，`getAllThemes:117`）、`CustomThemeLibAction`（`/portal/customThemeLib`）、`E7/E8/E9ThemeLibAction`；自定义主题存 `hp_grid_layout(layout_type='theme')`，E8/E9 标准主题存 `ecology8theme`/`ecology9theme`；前端 `stores/portalthemelib/*`。
- **A2 themestyle（样式指定：themeType/themeId/styleId/hpbgcolor/iscolor/istitle）**：后端 `web/ThemeStyleAction.java`（→`/api/portal/themeStyle`，`getThemeStyle:76/setThemeStyle:108/deleteThemeStyle:158`）；表 `themestyle`（`data/Mysql/sql201912190203.sql:1-11`）。
- **A3 themeappoint（批量/组织统一指定主题）**：后端在独立包 `com.engine.portalTs`：`portalTs/web/UtilsLoginAction.java`（→`/api/portalTsLogin/utils`，`setAppointThemeOfPersonnelScope:179/setAppointThemeOfThemeId:202`）+ `cmd/mainportal/UnifyAppointThemeCmd.java`（组织统一，`insert into systemtemplateunifyinfo`/`SystemTemplateSubComp`）；写 `HrmUserSetting.appoint_*` 与 `systemtemplateunifyinfo`/`SystemTemplateSubComp`。
- **A4 themesearch（使用查询：查人员主题/反查使用人员）**：`portalTs/web/UtilsLoginAction.java:225`（`getAllHrmOfTheme`）读 `hrmusersetting`；`web/ThemeCenterAction.java:76`（`getAllThemesOfUserId`）；前端 `stores/themesearch/*`。
- **配套 themecenter（我的主题）**：`web/ThemeCenterAction.java`（→`/api/portal/themeCenter`，`getAllThemes:51/setMyTheme:180/setMyThemeColor:228/setMyThemeLayout:253`）；`GetMyThemeCmd` 读 `PortalThemeConfig`/`SystemTemplate`/`hp_grid_layout`/`HrmUserSetting`/`systemtemplateunifyinfo`。
- **澄清**：**不存在 `portal_theme`/`hpTheme` 表**；主题定义分散 `ecology8theme`/`ecology9theme`/`hp_grid_layout(layout_type='theme')`/`SystemTemplate`；`PortalThemeConfig` 只存全局默认（fontsize/layouttype/showfrequsemenu/hiddenmyportal），不是主题分配表；个人当前/指定主题在 `HrmUserSetting`，组织级在 `systemtemplateunifyinfo`+`SystemTemplateSubComp`。

### 15.6 移动门户 mobileportal 族（独立 REST+表；注意：「移动门户」不读 modehtmllayout，复用它的是移动建模）

> 无独立 `cmd/mobileportal/` 目录，由 4 子模块组成；后端 `com.engine.portal.web`，移动端引擎 `com.engine.mobilemode`。

- **B1 mloginpage（移动登录页面）**：后端 `web/MLoginPageAction.java`（→`/api/portal/mloginpage`，`getMLoginPages:81/updateMLoginPage:166/saveAsMLoginPage:188/useMLoginPage:210/deleteMLoginPage:232/checkUniqueTimer:251`）；表 `portal_mloginpage`（`2.tables.sql:37612`）；前端 `stores/mloginpage/MLoginPageStore.js`。
- **B2 mportalpage（移动门户页面：快捷入口/排行榜/时间线/报表表单/待办数/图片，可样式/共享/从主门户导入/发布到移动）**：后端 `web/MPortalPageAction.java`（→`/api/portal/mPortalPage`，`getMPortalPages:123/saveMPortalPages:151/publishToMobile:205/getShares:231/setShares:257/getMainPageElements:467/importMainPage:493`）+ `cmd/mportalpage/{PublishToMobileCmd,...}`；表 `AppHomepage`(`isdelete/ishomepage`)/`AppHomepage_Model`/`mobileAppBaseInfo`/`mobileAppModelInfo`（`2.tables.sql:255/303/32316/32372`）；前端 `stores/mobileportal/MPortalPageStore.js`。
- **B3 melementstylelib（移动元素样式库）**：后端 `web/MElementStyleLibAction.java`（→`/api/portal/mElementStyleLib`）；表 `hpMobileStyle`（`menustyletype='melement'`，`2.tables.sql:19958`）。
- **B4 mnavstylelib（移动导航样式库）**：后端 `web/MNavStyleLibAction.java`（→`/api/portal/mNavStyleLib`）；**与 B3 共用 `hpMobileStyle`**，靠 `menustyletype='mnav'` 区分。
- **澄清**：移动门户**不混用 PC 的 homepage/hpelement**（独立 `portal_mloginpage`/`hpMobileStyle`/`hp_mobile_hpinfo` 等）；**真正复用 PC 建模 `modehtmllayout` 的是「移动建模」（AppHomepage/MEC，`PageAdapter.java:111,118`），不是「移动门户」**（`hp_mobile_hpinfo` 从头到尾不读 `modehtmllayout`）；`MobileExtendComponent` 属移动建模引擎。移动门户与移动建模是 E9 两套独立移动引擎（详见 §20）。

### 15.7 应用中心 + 协同 族

- **C1 appcenter（应用宫格 + 非标功能 + 通用设置 + 使用统计）**：后端 `web/AppCenterAction.java`（→`/api/portal/appCenter`，`getApps:39/getAppUseCount:59/getModuleMaintenanceCount:81`）、`web/PortalUnStandardFunctionAction.java`（`@Path /portal/unStandardFuncInfo`）；表 `AppUseInfo`（前五应用）、`SysMaintenanceLog`+`SystemLogItem`（维护统计）、`MainMenuInfo`（应用名）、`HP_NONSTANDARD_FUNCTION_INFO`（非标功能，`2.tables.sql:20544`）；ECharts 柱状图受 `ThemeConfig.properties` 的 `backend.appcentershow` 开关控制；前端 `stores/appcenter/AppCenterStore.js`。
- **C2 synergy 协同区（知识/流程/流程表单/流程节点协同；宽度/启用/参数；新建页与查看处理页）**：后端 `web/SynergyAction.java`（→`/api/portal/synergy`，`getTreeData:66/getHpInfo:89/setIsUse:135/setWidth:181/setParams:204/getWfFields:315`）+ 13 命令 `cmd/synergy/*` + 7 Biz `biz/synergy/{Synergy4Doc,Synergy4MyCalendar,Synergy4Report,Synergy4Workflow,SynergyCommon,SynergySysParams,SynergyWfParams}Biz`；表 `synergy_base`（`2.tables.sql:41764`）、`synergyconfig`（`:41754`）、`hpinfo_workflow`（`:19885`）、`hpLayout`、`sypara_*`；每个协同区有**两种页面**（hpid 新建页 / hpid2 查看处理页，`SynergyStore.js:90-103`）；前端 `components/synergy/*` + `stores/synergy/SynergyStore.js`。
- **澄清**：协同**真以 `hpid` 操作旧门户表**（`SynergyCommonBiz.java:141` 写 `hpinfo_workflow.hpid`、`:195-197` 写 `hpLayout(hpid,...)`+`synergyconfig(hpid,isuse)`、`:368` `getWfidByHpid` 用 `Math.abs(hpid)` 查 `synergy_base`），证实 §10.4/§13.4；协同专用表 `synergy_base`/`synergyconfig`/`sypara_*`，`Synergy4*` 是 Biz 类名不是表名。

### 15.8 对原 §3 / §12 / §13 的修正

| 原文档表述 | 修正/打通 |
|---|---|
| §3 称门户主表为 `homepage(hpid)` | 实际代码表名是 `HPINFO`（`hpid`=hpinfo.id），门户页/元素模块治理的就是它 |
| §12.2/§13.4/§13.5 仅罗列 portalpage/portalelement/portaltheme/mobileportal/appcenter/synergy | 本轮 §15.2–§15.7 逐模块补齐功能/接口/表/前端 |
| §3 未讲布局三套体系 | 补：`HPLAYOUT`(实例)/`PAGELAYOUT`(模板库)/`HP_GRID_LAYOUT`(网格) 三者并存 |
| §10.4/§13.4 说协同"参数仍是 hpid，底层仍操作 homepage/hpelement" | 经实证：协同以 `hpid` 操作 `hpinfo_workflow`/`hpLayout`/`synergyconfig` 等同源表，专用表 `synergy_base`/`synergyconfig` |
| 未说明移动门户独立性 | 移动门户是独立 REST+表（`hp_mobile_hpinfo` 等）；但"复用 modehtmllayout"属移动建模（AppHomepage/MEC），非移动门户（§20） |
| 未说明素材库不走 DB | 图片素材库 `materiallib` 完全基于文件系统，无关系表 |

> 一句话：门户引擎的页面/元素/主题/移动/协同分栏，本质都是把 §3 的 `HPINFO/HPELEMENT/hpElementSettingDetail` 等核心表（外加少量新增表与文件系统素材库）包装成可视化治理界面；其中协同与 PC 门户同源（hpid），移动门户自成体系但复用建模布局。

---

## 16. 补充分析（第八轮）：门户维护 portalmaintenance 剩余模块深度分析

> 探查对象：`com/engine/portal/`(web/cmd)、`weaver/portal/util/PortalConfigUtil`、`data/`。背景：§12.2 把 portalmaintenance 罗列为 `loginportal/mainportal/tsportal/portalsetting/portalexport/portalimport/themestyle/themeappoint/themesearch/portalconfig`，其中 mainportal/themestyle/themeappoint/themesearch 已在 §15 覆盖；本轮补齐其余 6 个并厘清关系。
> 路由复核（代码实证）：`PortalConfigAction`/`PortalImportAction` 无类级 @Path → 路由 `/api/portal/portalConfig/*`、`/api/portal/portalImport/*`；**tsportal 后端真实类是 `PortalGridLayoutAction`**（非 `TsPortalAction`，全工程零命中）→ `/api/portal/portalGridLayout/*`。另：原文档假设的 `PortalConfigComInfo` 不存在，真实工具类是 `weaver.portal.util.PortalConfigUtil`。

### 16.1 loginportal（登录门户 / 登录模板）
- **功能**：维护「登录前门户模板（皮肤）」——背景图、Logo、登录框、二维码、登录信息；可设当前启用、另存为、删除、上传图片、保存登录页设置，并支持**按时间定时切换模板**。与 §15.2 A1 `loginpage`（登录前页面内容库）是「壳/皮肤 vs 页面内容」关系（见 §16.7①）。
- **后端**：`web/LoginPortalAction.java`（`@Path /portal/loginportal`，`:34`）：`upload:45/ list:76/ operate:102(delete|update)/ saveas:139/ save:166(设 iscurrent)/ get:192/ saveLoginSetting:218/ getLoginSetting:245/ defaultdisplaypage:292/ checkUniqueTimer:308`；cmd `cmd/loginportal/{GetListDataCmd,EditLoginPortalCmd,SaveAsLoginPortalCmd,...,DeleteLoginPortalCmd}`。
- **表**：`SystemLoginTemplate`（`1.tables.sql:42193`：LOGINTEMPLATEID,LOGINTEMPLATENAME,TEMPLATETYPE,ISCURRENT,DEFAULTSHOW,LEFTMENUID,OPENWINDOWLINK,SETTINGVERSION）+ `SystemLoginTemplateElement`（`:42226`：ELEMENTTYPE,SHOWTYPE,CONTENT,ORDERSORT,LOGINTEMPLATEID）；定时字段 `openTimer/timerStartDate/timerEndDate`（`GetListDataCmd.java:54`）。证据：`UpdateLoginPortalCmd.java:69-71` `UPDATE SystemLoginTemplate SET isCurrent='1'`、`LoginPortalBiz.java:34-35,48,57,89`。
- **前端**：`components/loginportal/`、`stores/loginportal/{LoginPortalStore,LoginSettingStore}.js`（`/api/portal/loginportal/{list,operate,get,save,saveas,saveLoginSetting,getLoginSetting}`）。

### 16.2 tsportal（特色门户 → 实为「门户网格布局」）
- **功能**：管理门户**响应式网格布局**，按 `layout_type` 分「登录前门户(login) / 登录后门户(theme)」（`tsportal/index.js:148-150`）。支持新建/编辑/删除/启用(`layout_use`)/设为默认入口(`layout_default_entrance`)/共享/预览。⚠️ 命名澄清：前端标签确为「特色门户」，但后端真实是 `PortalGridLayoutAction`，且**代码里没有 startdate/enddate 的"定时门户"逻辑**（定时切换落在 §16.1 loginportal 的 `SystemLoginTemplate`）。
- **后端**：`web/PortalGridLayoutAction.java`（无类级 @Path → `/api/portal/portalGridLayout`，`:27`）：`getRightStatus:41/ getPortalGridLayouts:60/ editPortalGridLayout:82/ usePortalGridLayout:104/ defaultEntrancePortalGridLayout:126/ deletePortalGridLayout:148/ setPortalGridLayout:170/ getPortalGridLayout:193/ editPortalGridLayoutElement:221/ deletePortalGridLayoutElement:239`；cmd `cmd/portalgridlayout/*`。
- **表**：`hp_grid_layout`（`1.tables.sql:20392` / `Mysql/1tables.sql:22083`：layout_type,layout_name,layout_use,layout_default_entrance,layout_config CLOB,...）+ `hp_grid_layout_element`（ID,layout_id,layout_position,layout_element）。证据：`EditPortalGridLayoutCmd.java:50,63,70`、`UsePortalGridLayoutCmd.java:35`、`GetPortalGridLayoutCmd.java:43,89`。
- **前端**：`components/tsportal/{index,Edit,edit4config}.js`、`stores/tsportal/{TsPortalStore,index}.js`（`/api/portal/portalGridLayout/*`）。

### 16.3 portalsetting（门户设置 / 门户应用设置）
- **功能**：门户基础应用设置——首页自动刷新开关与间隔、RSS 类型、是否显示上次门户、是否按使用频率排序顶部菜单、是否显示顶部菜单名、是否开启左侧菜单、无权限元素是否显示、主题布局类型、是否显示常用菜单、是否隐藏我的门户、首页刷新指定元素；含「同步到所有人」(`synchronize`/`layoutSynchronize`) 与刷新数据(`getHpRefreshData`)。
- **后端**：`web/PortalSettingAction.java`（`@Path /portal/portalSetting`，`:33`）：`getRightStatus:60/ getSettingData:81/ setSettingData:107/ synchronize:132/ layoutSynchronize:159/ getHpRefreshData:185`；cmd `cmd/portalsetting/{GetSettingDataCmd,SetSettingDataCmd,SynchronizeCmd,LayoutSynchronizeCmd,GetHpRefreshDataCmd}`。
- **表（非 portalconfig 表，而是三张系统表）**：`SystemSet`（`GetSettingDataCmd.java:36` `needRefresh,refreshMins,rsstype,...`；`SetSettingDataCmd.java:54` update）、`PortalThemeConfig`（`:74` 读 `fontsize,layouttype,showfrequsemenu,hiddenmyportal`；`:59` update）、`PortalPageConfig`（`:58,62-64` `delete/insert refreshType,refreshEbaseid`）。
- **前端**：`components/portalsetting/`、`stores/portalsetting/PortalSettingStore.js`（`/api/portal/portalSetting/{getRightStatus,getSettingData,setSettingData,synchronize,layoutSynchronize,getHpRefreshData}`）。

### 16.4 portalexport（门户导出）
- **功能**：把门户**页面**（登录前 `pageType=0` / 登录后 `pageType=1`）及其所选元素、元素样式、外部数据、图表等连同资源 xml/css **序列化打包成 ZIP** 以便迁移/备份。前端向导：先选门户（preList=登录前 / aftList=登录后）→ 再选元素 → 导出下载。
- **后端**：`web/PortalExportAction.java`（`@Path /portal/portalexport`，`:38`）：`getRightStatus:63/ elements:86/ exportdata:109/ deleteTempZipFile:126`；实现 `biz/portalExport/PortalExportDataBiz.java`。
- **落地为文件（ZIP，不落表）**，读取源表：`hpinfo`（`PortalExportDataBiz.java:143` `infoname,Subcompanyid,layoutid,styleid`）、`pagelayout`（`:154`）、`hpelement`/`hpelementsetting`（`:231`）；资源 `copyResourceFile("/page/style/.../conf/<styleid>.xml")`；每个门户写 `Page_<hpid>.json`（`exportPagesData :82-95`）；路径常量 `ImportExport.java:7-8`（`.../page/exportImport/temp/`、`.../page/exportImport/`）。
- **前端**：`components/portalexport/`、`stores/portalexport/PortalExportStore.js`（`/api/portal/portalexport/{getRightStatus,elements,exportdata,deleteTempZipFile}`，下载 `window.location.href=data.fileName`）。

### 16.5 portalimport（门户导入）
- **功能**：从 ZIP 导入门户（页面+布局+元素+样式+资源）。支持登录前(`preHpids`)/登录后(`aftHpids`)分别导入、覆盖/重命名冲突处理(`coverStyle`/`sameNameDefaultDealMode`/`sameNameHomepageData`)、可选资源共享、预览；ZIP 上传仅限超级管理员（`uid==1`）。
- **后端**：`web/PortalImportAction.java`（无类级 @Path → `/api/portal/portalImport`，`:50`）：`getRightStatus:76/ datas:100/ check:122/ save:144/ doImport:160/ getPreViewData:179/ uploadFile:195(仅 zip、uid==1)/ getPageDuplicationList:254`；cmd `cmd/portalimport/{PortalImportCmd(~90KB),CheckPortalImportDataCmd,GetPortalImportDataCmd,SavePortalImportDataCmd}`。
- **写回门户表**：`HpInfo`（`PortalImportCmd.java:99,136` insert）、元素样式 `addElementStyle:108`、页面布局 `addPageLayout:114`、`HpLayout`/`HpElement` 全套设置子表（`:151-294`：`HpElementSetting`/`SettingDetail`/`ShowFieldUse(/Detail)`/`OrderColUse`/`PictureSize`、`HpElementSlideSetting`(IMGSLIDE:245)、`HpElementVideoSetting`(VIDEO:264)、`HpElementQuickEntrySetting`(QUICKEENTRY:282)）；上传目录 `ImportExport.EIPORT_SET_PATH=.../page/exportImport/_TransferStation/`（`ImportExport.java:11`）。
- **前端**：`components/portalimport/`、`stores/portalimport/PortalImportStore.js`（`/api/portal/portalimport/{getRightStatus,check,doImport,getPreViewData,getPageDuplicationList}`）。

### 16.6 portalconfig（配置项）
- **功能**：门户级系统开关/配置项的通用键值管理（列出 `ccode/vvalue/name/note` 并编辑单条取值）。已知配置项：`isOpenMenuCache`（是否开启菜单缓存）、`canPageMaintainerSettingInViewpage`（是否允许页面维护者前台设置）。权限 `MAIN_PORTAL`。
- **后端**：`web/PortalConfigAction.java`（无类级 @Path → `/api/portal/portalConfig`，`:35`）：`getRightStatus:50/ getPortalConfigData:74/ setPortalConfigData:101/ GetEitdPortalConfigData:139/ getConfigValueByCode:167`；cmd `cmd/portalconfig/*`；底层 `weaver.portal.util.PortalConfigUtil`（`get/set/refresh` + `StaticObj` 缓存 `PortalConfigs`）。
- **表（真实表 `portalconfig`，非 properties 文件）**：建表 `data/Mysql/sql202005144503.sql:1-4`（`ccode,vvalue,name,note`）+ 唯一约束 `sql202005190903.sql:1`；列表 `GetPortalConfigDataCmd.java:39-40` `sqlForm="portalconfig"`；更新 `SetPortalConfigDataCmd.java:51` `update portalconfig set vvalue=? where ccode=?` + `:57` `PortalConfigUtil.refresh()`；缓存 `PortalConfigUtil.java:16,31-43`。种子示例 `insert into portalconfig(ccode,vvalue,name,note)values('isOpenMenuCache','0',...)`、被 `MenuCache.isOpenMenuCache()`/`HomepageCommonBiz.canPageMaintainerSettingInViewpage()` 消费。
- **前端**：`components/portalconfig/`、`stores/portalconfig/PortalConfigStore.js`（`/api/portal/portalconfig/{getPortalConfigData,GetEitdPortalConfigData,setPortalConfigData}`）。

### 16.7 三个关系澄清（代码实证）

**① loginportal vs §15.2 A1 loginpage**：二者都"登录前"但**操作不同表、不同对象**，并非都操作 HPINFO。
- `loginportal`（`LoginPortalAction`）= 登录模板/皮肤+分配+定时切换，主表 `SystemLoginTemplate`+`SystemLoginTemplateElement`，负责背景/Logo/登录框/二维码/当前启用(`iscurrent`)/定时(`openTimer,timerStartDate,timerEndDate`)，并据 `GetLoginPortalEditInfoByIdCmd.java:103-111,134` 读 `hpinfo` 取默认显示页名（注：`DEFAULTSHOW` 该列名在 Java 源码中 0 命中，需以 DB 字典核对，见 §18.3⑦）。
- `loginpage`（`LoginPageAction`，注释"登录前页面"）权限 `PortalRight.HOMEPAGE`，直接管理 **HPINFO 登录前页面内容库**（`pageType=0`）。
- 结论：**loginportal 是登录前"页面外壳/皮肤+分配+定时"，loginpage 是登录前展示的"门户页面内容库(HPINFO)"**，互补而非同一物。

**② portalmaintenance 整体 vs §15.2 portalpage 族**：二者是**同一批底层数据（HPINFO/HPELEMENT/HPLAYOUT 等）的不同职责切面**。portalmaintenance 是后台"运维/部署/分配/设置/导入导出/配置"总入口；portalpage 族是"门户页面库样式设计器"入口（直接设计页面内容）。`mainportal ≈ mainpage`（都是登录后门户，mainportal 偏部署/权限/发布，mainpage 偏页面设计）；**但 `loginportal ≠ loginpage`**（见①）；导出导入模块同时覆盖 `pageType=0/1` 两类页面，正是两类入口共享底层表的佐证。

**③ portalexport/portalimport 全链路**：
- **对象范围**：门户页面（登录前 `pageType=0` / 登录后 `pageType=1` = `HPINFO`），逐门户含：页面布局 `pagelayout`、元素 `hpelement` 及其全套设置子表（`hpelementsetting`/`settingdetail`/`showfielduse(/detail)`/`ordercoluse`/`picturesize`、`hpelementslidesetting`(IMGSLIDE)、`hpelementvideosetting`(VIDEO)、`hpelementquickentrysetting`(QUICKEENTRY)）、元素样式 conf xml/css 资源。**菜单以"元素(ebaseid)"形式随 `hpelement` 一并导入，无独立菜单表迁移步骤**。
- **文件格式**：每个门户序列化为 `Page_<hpid>.json`（JSON），非系统样式/资源 xml/css 复制到临时目录，最终打包 **.zip**；临时 `page/exportImport/temp/`，成品 `page/exportImport/`（`ImportExport.java:7-11`）。
- **导入链路**：上传/读 zip（`uploadFile` 限 uid==1）→ `check` 解析返回 preList/aftList → 选页面/元素 → `getPageDuplicationList` 处理同名冲突（`coverStyle`/`sameNameDefaultDealMode`）→ `doImport` 反序列化 `Page_*.json` 按 dealMode 写回 `HPINFO/HPELEMENT/HPLAYOUT` 及元素样式表（`PortalImportCmd.java:99-294`）。

---

## 17. 补充分析（第九轮）：门户元素类型全谱系 + 门户权限/分权体系

> 探查对象：`weaver/page/interfaces/commons/ConstantUtil`、`WeaverBaseElementCominfo`、`com/engine/portal/web/ElementsAction.java`、`com/engine/portal/constant/PortalRight.java`、`weaver/hrm/moduledetach/ManageDetachComInfo`、`data/init/SQLServer/3.tables.sql`。背景：此前 §3 讲了 `hpelement` 两层模型、§15.3 讲了元素库模块，但**没系统列出门户到底支持哪些元素类型、各自配置面板与渲染链路**；权限只零散提及 `PortalRight` 几个码。本轮补齐。

### 17.1 门户元素类型全谱系（ebase）

**核心结论**：元素类型由 **`hpBaseElement`（=ebase 基础类型表，主键 `ebaseid`）** 定义；官方枚举 `ConstantUtil.elementType`（`ConstantUtil.java:53-139`）给出 **53 种元素类型**的权威 ebaseid↔名称↔分类映射。元素分两类存储：**系统/标准元素** 走 `/page/element/<ebaseid>/` 通用配置（由 `elementsetting`/`elementsettingitem` 表驱动），**自定义/集成元素** 走 `hpBaseElementCustom`（指定前端 `viewcompname`/`settingcompname` + 后端 `dynamicClass`/`savemethod`）。

**表（SQLServer 建表）**：`hpBaseElement`（`3.tables.sql:27760`：`id`=ebaseid,`elementtype`(3=自定义),`title`,`logo`,`perpage`,`linkmode`,`moreurl`,`isuse`(封存),`loginview`(0登录后/1登录前/4废弃),`issystemelement`；扩展 `dynamicClass` `data/ST/sql202006223003.sql`）；`hpBaseElementCustom`（`:4313`：`ebaseid,modulename,packagename,viewcompname,settingcompname,savemethod` 仅自定义/集成）；`hpElement`（`:27974`，`ebaseid` 外键 `:27980`）；配置字段表 `elementsetting`(`:17733`)/`elementsettingitem`(`:17752`)。
**权威缓存**：`WeaverBaseElementCominfo`（`TABLE_NAME="hpBaseElement"`，`:36/:53` 缓存全类型；`:99-103` 揭示标准子路径 `/weather/ /video/ /reportForm/ /picture/ /menu/ /favourite/ /audio/` 与 `elementtype=3→/page/elementCustom/<id>/`）。列表遍历 `GetHpBaseElementListCmd.java:50-81`（过滤 isuse!=1 封存、`loginview=4` 废弃、`reportForm` 需权限、`fnaBudgetAssistant*` 仅协同区）。

**门户元素类型清单（53 种，取自 `ConstantUtil.java:53-139`）**

| 分类 | ebaseid | 名称 | 分类 | ebaseid | 名称 |
|---|---|---|---|---|---|
| 可变tab | `1` RSS阅读器 | 其它 | `17` 多新闻中心 | | |
| 可变tab | `7` 文档元素(`7_DY`) | 其它 | `18` 期刊 | | |
| 可变tab | `8` 流程元素 | 其它 | `19` 股票 | | |
| 可变tab | `29` 自定义页面 | 其它 | `25` 文档内容 | | |
| 可变tab | `reportForm` 图表 | 其它 | `audio` 音频 | | |
| 可变tab | `OutData` 外部数据 | 其它 | `favourite` 收藏 | | |
| 可变tab | `FormModeCustomSearch` 建模查询 | 其它 | `Flash` Flash | | |
| 可变tab | `news` 公司新闻 | 其它 | `picture` 图片 | | |
| 可变tab | `RankingList` 排行榜 | 其它 | `MyCalendar` 日历日程 | | |
| 可变tab | `timeLine` 时间轴 | 其它 | `imgSlide` 多图 | | |
| 不可变tab | `16` 我的邮件 | 其它 | `newNotice` 最新通告 | | |
| 不可变tab | `addwf` 新建流程 | 其它 | `outterSys` 集成登录 | | |
| 不可变tab | `Task` 任务 | 其它 | `scratchpad` 便签 | | |
| 不可变tab | `blogStatus` 微博 | 其它 | `weather` 天气 | | |
| 不可变tab | `contacts` 通讯录 | 其它 | `video` 视频 | | |
| 不可变tab | `processTask` 我的任务 | 其它 | `login` 登录系统(列表排除) | | |
| view.jsp | `6` 未读文档 | 其它 | `Slide` 幻灯片 | | |
| view.jsp | `9` 消息提醒 | 其它 | `DataCenter` 个人中心 | | |
| view.jsp | `10` 我的项目 | 其它 | `jobsinfo` 多岗位事项 | | |
| view.jsp | `11` 最新客户 | 其它 | `searchengine` 微搜 | | |
| view.jsp | `12` 最新会议 | 其它 | `notice` 通告栏 | | |
| view.jsp | `13` 未读协作 | 其它 | `plan` 计划 | | |
| view.jsp | `14` 当月目标 | 其它 | `menu` 菜单 | | |
| view.jsp | `15` 当日计划 | 其它 | `32` 任务计划 | | |
| view.jsp | `34` 知识订阅 | 其它 | `mobilePicture` 手机图片 | | |
| 其它 | `CoreMail` / `TencentMail` 邮箱 | 其它 | `MeetingCalendar` 会议日历 | | |
| 其它 | `quickEntry` 快捷入口 | 其它 | `CalendarEle` 日历 | | |
| 其它 | `ecodeElement` ecode自定义 | 其它 | `bookshelf` 书柜 | | |
| 其它 | `odocCenter` 公文中心 | 其它 | `newMobilePicture` 移动图片 | | |
| 其它 | `fnaBudgetAssistant` 费用助手 | 其它 | `mobileQuickEntry` 移动快捷入口 | | |
| 其它 | `fnaBudgetAssistant1` 审批助手 | | | | |

**各类型配置面板（后端）**：聚合 `web/ElementsAction.java`（类级 `@Path /portal/element`，**71 个 @Path**）：通用 `/edata:51`/`/tabdata:72`；流程 `/workflow:93`/`/workflowtab:115`；新闻 `/news:244`/`/newstab:266`；图表 `/reportform:287`；排行榜 `/rankinglist:330`；外部数据 `/outdata:374`；建模 `/formmodecustomsearch:453`；邮件 `/mail:638`/`/coremail:701`/`/tencentmail:723`；菜单 `/custommenu:953`；图片 `/picture:974`；Flash `/flash:933`；视频 `/video:1037`；天气 `/weather:1058`；通讯录 `/contacts:1205`；收藏 `/favourite:912`；搜索 `/searchengine:995`；协作 `/cooperation:537`… 全部经 `getElementJson→GetElementInfoCmd→ElementsServiceImpl`。元素库管理 `web/PortalElementsAction.java`（无类级@Path→`/api/portal/portalelements/*`，`getConfigData:262/setConfigData:288` 读写 `elementsetting`/`elementsettingitem` 通用配置）。
**前端**：`stores/portalelements/PortalElementsStore.js`（`/list:64/operate:76/getConfigData:233/setConfigData:255/preview:213`）、`components/portalelements/ElementSetting.js:28`（按 `ebaseid` 复用同一通用面板）；移动端 `components/mobileportal/mportalpage/setting/content/Content_<ebaseid>.js` 按类型一对一。

**渲染链路（hpelement.EBASEID→运行时组件）**：① `hpElement.ebaseid`→`hpBaseElement.id` 取配置/路径；② `WeaverBaseElementCominfo.initCache:99-103,148-152`：`elementtype=3→/page/elementCustom/<id>/`，否则 `/page/element/<id>/`（weather/video/searchengine/reportForm/picture/menu/favourite/audio 大小写映射，`Custom_` 前缀交由 SPA 渲染）；③ 前端调 `/api/portal/element/edata` → `ElementInfoCommonBiz.getElementMap(eid,ebaseid)`（`ElementInfoCommonBiz.java:160`）按 `ConstantUtil.elementType` 分支差异化取数（NEWS:216,350 / WORKFLOW:303 / TIMELINE:404 / OUTDATA:226,442 / FORMMODECUSTOMSEARCH:234 / 文档7:161,378）；④ 自定义元素由 `HomepageCommonBiz.java:567` 从 `hpBaseElementCustom` 取 `viewcompname`/`settingcompname` 交对应 SPA 包（`weaIntegrationElement`/`weaGovernElement`/`weaFnaEngine`）渲染。

### 17.2 门户权限 / 分权体系

**核心结论**：权限码集中在枚举 `PortalRight.java`（14 常量，各绑定一个 `*Right` 实现类 + `HrmUserVarify` 权限串）。"门户管理分权"由 `ManageDetachComInfo.isUsePortalManageDetach()` 控制，开启后总部菜单(`HeadMenu:Maint`)与分部菜单(`SubMenu:Maint`)管理员分离。共享统一落 `commonshareinfo`（按 `module` 区分），元素可见范围另用 `elementshareinfo`，元素个人/维护级别用 `hpElementSettingDetail.sharelevel`（实测仅 1/2，非 1/2/3）。

**权限码–保护对象对照表（`PortalRight.java:9-25`）**

| 枚举常量 | 权限串 | 保护对象 |
|---|---|---|
| `HOMEPAGE` | `homepage:Maint` | 首页/门户页面维护 |
| `HOMEPAGE_ORDER` | `homepage:Maint` | 首页元素排序 |
| `LOGIN_PORTAL` | `LoginPageMaint` | 登录页维护 |
| `MAIN_PORTAL` | `SystemTemplate:Edit` | 主页（顶部）模板维护 |
| `COMPANY_MAIN_PORTAL` | `HeadSystemTemplate:Edit` | 总部主页模板 |
| `MOBILE_PORTAL` | `Portal:MobliePage` | 移动门户 |
| `MOBILE_PORTAL_STYLE` | `Portal:MobliePageStyle` | 移动门户样式 |
| `COMPANY_MENU` | `HeadMenu:Maint` | 总部菜单维护 |
| `SUB_COMPANY_MENU` | `SubMenu:Maint` | 分部菜单维护 |
| `TOOLBAR_MENU` | `Portal:toolBarMenu` | 工具栏更多菜单 |
| `MATERIAL_LIB` | `MaterialLibrary:Maint` | 素材库 |
| `SYNERGY` | `Synergy:Maint` | 协同区 |
| `FLOATAREA` | `FloatArea:Edit` | 浮动区 |
| `COMPANY_FLOATAREA` | `HeadFloatArea:Edit` | 总部浮动区 |

**校验机制**：`PortalRightUtil.checkUserRight(className,user)`（`util/PortalRightUtil.java:24-38`）反射调各 `*Right.checkUserRight`，内部统一 `HrmUserVarify.checkUserRight(rightCode,user)`（如 `HomepageRight.java:27`、`CompanyMenuRight.java:21`）。`getUserRightStatus` 区分 0/1/2/-1（无权限/有权限/无总权限但有可维护首页/无权限，`HomepageRight.java:36-53`，依据 `ptAccessControlList.permissiontype` 1部门/2角色/3所有人/5人员/6分部/7岗位 `:77-123`）。

**门户管理分权**（`ManageDetachComInfo.java`）：`portaldetachable` 总开关 + `portaldftsubcomid` 默认分部（`:33-34,105-106`）；`isUsePortalManageDetach():637-643` 须 `detachable=="1"` 且 `portaldetachable=="1"` 且 `portaldftsubcomid` 非空非"0"。分权后（`PortalMenuMaintenanceAction.checkUserRight:47-92`）：总部(`resourceId==1&&resourceType==1`)需 `HeadMenu:Maint`；分部(`resourceType==2`)需 `SubMenu:Maint`；分权二次限制 `:81-87` 用 `SubCompanyComInfo.getRightSubCompany(uid,"SubMenu:Maint",0)` 限定分部范围，系统管理员 `uid==1` 豁免。

**共享 / 使用限制（统一 `commonshareinfo` `3.tables.sql:3687`）**：`id,module,resourceid,resourcetype,infoid,sharetype,sharevalue,seclevel,rolelevel,jobtitlelevel,jobtitlesharevalue`，按 `module` 区分业务——`toolbarm`(工具栏更多，`SetSharesCmd.java:48,67`)、`qs`(快捷搜索类型，`SaveQuickSearchTypesSharesCmd.java:41,45`)。通用读取器 `CommonShareCominfo.java:150` 把 `module` 当 `type` 统一加载。元素可见范围另表 **`elementshareinfo`**（`:17770`，按 `eid` 维度，带 `seclevel` 区间与 `includeSub`/`jobtitlelevel`）；自定义快捷入口另表 **`hpCustomShare`**（`SetCustomShareInfoCmd.java:90`）。三者共享同一套 `sharetype/sharevalue/seclevel` 维度。

> **修正（详见 §22）**：上句"三表共享同一套编码"需谨慎——`commonshareinfo`/`elementshareinfo`/`hpCustomShare` 属"使用/可见"层，但**首页"可维护"共享另表 `ptaccesscontrollist`**、**首页"终端可见"共享另表 `shareinnerhp`**，且各表 `type` 编码并不一致（如 `shareinnerhp` 1=部门/2=角色/6=分部，而 `elementshareinfo` 1=人员/2=分部/3=部门/5=所有人/6=角色/7=安全级别）。门户"谁能看到"的完整机制与三条轴划分见 §22。

**元素级权限（`hpElementSettingDetail.sharelevel` `3.tables.sql:28313`，`:28321`）**：实测仅两值——`1`=**个人查看设置**（缺省，`GetNewNoticeMoreDatasCmd.java:97-121` `// 1:为查看`、`LockElementCmd.java:68` 复制行写'1'）；`2`=**可维护/管理员共享设置**（可添加内容，`GetNewNoticeMoreDatasCmd.java:98-100,123-124` `canAddNotice=true`、`UpdateSyniHp.java:152,172` 重置时 `delete...where sharelevel!=2`）。**未发现 3**；"私人/分部/全部"的三元语义实际对应 `elementshareinfo.sharetype`(1部门/2分部/3人员/4角色/5所有人…) 或 `ptAccessControlList.permissiontype`。元素可见/可维护 = `elementshareinfo`(按 eid 匹配身份+seclevel+includeSub) ∪ `hpElementSettingDetail`(命中且 sharelevel 决定仅查看/可维护)，写入受 `ElementShareRight`（`biz/right/ElementShareRight.java:7`）校验。

---

## 18. 补充分析（第十轮）：门户运行时渲染管线 + 登录入口与门户选择

> 探查对象：`weaver/portal/HomepageCommonBiz`、`com/engine/portal/web/HomepageAction.java`、`cmd/homepage/GetHpAllElementJsonCmd.java`、`com/engine/portal/web/ElementsAction.java`、`src4js/pc/portal/components/Homepage.js`、`wui/main.jsp`、`src4js/pc4mobx/portal4theme/stores/ThemeStore.js`、`data/`。背景：§1 讲了"两套渲染范式"、§8 画了"配置→展示"流程图、§14–§17 深扒了各分栏的配置/管理接口，但**从未追运行时到底怎么把配置渲染成页面、用户登录后怎么被路由到正确门户**。本轮补齐，并纠偏旧章节 7 处偏差。

### 18.1 门户运行时渲染管线

**核心结论**：E9 用户的门户是 **React SPA（编译到 `/spa/portal/static/`，源码 `src4js/pc/portal/`，注意**非**后台管理 SPA `pc4backstage/portal4engine/`） + 后端 JSON 接口**，而非 jsp/velocity。页面 JSON 由 `HomepageAction./hpdata` → `GetHpAllElementJsonCmd` → `HomepageCommonBiz.getBaseHpStr` 把 `hplayout`+`hpElementSettingDetail` 合并成 `hpinfo` JSON，每个元素再经 `ElementsAction./edata|/tabdata|/news…` 按 `ebaseid` 异步取数。"我的门户"个人化**不是独立表**，而是 `hplayout`+`hpElementSettingDetail` 的**按用户行**。

- **① 入口与容器**：E9 登录后 `wui/main.jsp:72` `sendRedirect("/wui/index.html#/main")`（`getCurrWuiConfig(session,user,"theme")` 取主题，ecology9 走 SPA、e8 主题走 `wui/theme/{theme}/page/main.jsp`）；`wui/index.html` 加载 `/spa/portal/{public,static}/index.js`、`/spa/theme/static/index.js`；运行时门户 SPA 入口 `src4js/pc/portal/components/Homepage.js:53` `WeaTools.callApi('/api/portal/homepage/hpdata','POST',params)`。
- **② 取数与合并**：`web/HomepageAction.java:33-45` `@Path("/portal/homepage")`+`/hpdata` → `GetHpAllElementJsonCmd.java:42-147`（`hpid` 校验 `:53-62`→权限判定 `:64-97`→`commonBiz.getBaseHpStr` `:118/124`→`map.put("hpinfo",...)` `:130`）；核心 `HomepageCommonBiz.getBaseHpStr:58-239` 读 `select areaflag,areasize,areaElements from hplayout where hpid=? and userid=? and usertype=?`（`:123`），逐元素读 `select sharelevel from hpElementSettingDetail where eid=? and userid=? and usertype=?`（`:215-218`），`getContainer(ebaseid,eid,hpid,...)`（`:229/428+`）产出 `itemMap`。
- **③ 元素运行时渲染**：`web/ElementsAction.java:50-86` `@Path("/portal/element")`+`/edata`(`getElementsDataJson`)/`/tabdata`；外加 **70+ 按 ebaseid 命名的专用端点**（`/news`/`/workflow`/`/slide`/`/doccontent`…，`:93-1226+`，与 §17 配置保存端点一一呼应）；路由链 `getElementJson→ElementsServiceImpl:16-17→GetElementInfoCmd:59→ElementInfoCommonBiz.getElementMap(eid,ebaseid,...)`，`ebaseid` 作派发键落 `weaver/page/element/*`。
- **④ 个人化合并（"我的门户"）**：**源码检索不到 `hppersonalize`/`portal_user_setting`/`userhp`（0 命中）**；个人化走按用户行的 `hplayout`+`hpElementSettingDetail`。`HomepageCommonBiz.intiThisHpData:314-408`：若 `(hpid,userid,usertype)` 在 `hplayout` 无记录，则从**创建者** `creatorid/creatortype` 的 hplayout 复制一份到该用户（`:337-339`），再读 `UserAddElementInfo`（`select eid,areaflag FROM UserAddElementInfo WHERE hpid=? AND userid=?`，`:373`）把用户自添元素插入对应区域（`:384-403`）；用户增删/排序/隐藏/锁定最终改写**本人**的 `hplayout.areaElements` 与 `hpElementSettingDetail`（见 §15.3 B9 的 `DragElementCmd`/`DeleteElementCmd`/`LockElementCmd`）。
- **⑤ 门户刷新**：`SetSettingDataCmd.java:41-64` 把 `needRefresh/refreshMins` 写 `SystemSet`，`refreshType=2` 时 `insert into PortalPageConfig(refreshType,refreshEbaseid)`；`GetHpRefreshDataCmd.java:33-59` 读回，`/api/portal/portalSetting/getHpRefreshData`；前端 `Homepage.js:101-110` `setInterval(()=>this.getPortalData(params), refreshMins*60*1000)` 定时整页重拉 `hpdata`；每个元素 `itemMap` 带 `needRefresh:"true"`（`HomepageCommonBiz.java:510`）各自异步拉 `/edata`/`/tabdata`。
- **⑥ 缓存**：前端 `Homepage.js:48,59,68,96` 用 `ls.set('portal-'+hpid+'-'+isSetting, data)` 存 **localStorage**（设置态/查看态分开，`Toolbar.js:29-32` 同步两份）；后端 hpinfo/hpelement 经 `PageCominfo`/`HomepageElementCominfo`/`ElementStyleCominfo`/`ElementBaseCominfo`（`HomepageCommonBiz.java:38-43`）；**无 `PortalCache`/`PageCache` 类**。`MenuCache`（`weaver/portal/cache/MenuCache.java:25`）仅缓存**菜单**（§14.3），与门户 body 缓存无关。

### 18.2 登录入口与门户选择

**核心结论**：E9 登录后走 `/wui/index.html#/main`，首选门户 hpid 由前端 `ThemeStore.js:435-438` 按 **上次访问(lasthpid) > 机构默认(defaulthpid) > 全局默认** 解析；机构默认来自表 `hpdefualtsetting`（即 §15.2 A3，拼写为 `hpdefualtsetting`），上次访问来自 `hpcurrentuse`。"登录后门户/mainPortal" 以 `systemtemplate` 记录实现（含 `openTimer/timerStartDate/timerEndDate`）。**定时切换不存在 Quartz Job**，是渲染时按当前时间比对（拉模式）。

- **① E9 入口路由**：登录后 `wui/main.jsp:72` redirect `/wui/index.html#/main`（旧 E8 入口 `portal/main.jsp:136-137` 是 top.jsp+iframe 范式，即 §10 已纠偏）；登录前 E9 登录页 `/spa/hrm/staticLoginNew/loginNew.js`。
- **② 登录门户选择（SystemLoginTemplate）**：维护 `web/LoginPortalAction.java:34` `@Path("/portal/loginportal")`（含 `/save` 切当前、`/list`、`/get`）；E9 默认初始化 `weaver/portal/init/PortalLoginPageDataInit.java`（templateType='E9'，loginTemplateId=1/2）；当前模板 `isCurrent`（单选 radio，`SptmForLoginTemplate.getTemplateSelectStr:58-61`）；**`DEFAULTSHOW` 列名 Java 源码 0 命中**（§16.1 所述需以 DB 字典核对，不宜当运行字段）；**定时切换无 Quartz Job**（`SystemTemplate*Job`/`LoginTemplateTimer`/调度器均 0 命中），切换为拉模式：`SptmForLoginTemplate.getTimerActive:76-93`（`openTimer==1 && timerStartDate<=now<=timerEndDate` 激活）、`GetMyThemeCmd.getUserTimerThemeInfo:195-209` 直接 SQL 按当前时间比对（机构级覆盖全局级）。
- **③ 登录后门户分配（判定顺序实证）**：`src4js/pc4mobx/portal4theme/stores/ThemeStore.js:435-438`：`leftMenuId = showlasthp=='1' && lasthpid ? lasthpid : defaulthpid ? defaulthpid : null` → **上次访问(lasthpid) > 机构默认(defaulthpid) > 全局默认(null)**。来源：`lasthpid`=`homepageUtil.getUserNewHpid` 读 `hpcurrentuse`（`HomepageUtil.java:553-555` 必须 `in getShareHomapage` 且 isuse=1，无则回退 `hpinfo where showtype=1 and isuse=1 order by ordernum1,id` 取第一 `:565-582`，写入 `saveUserHpStat:834-846`）；`defaulthpid`=`PortalMenuImplE8.getDefaulthpid:67-77` 读 `hpdefualtsetting where subcompanyid=?`（用户主分部）；`showlasthp`=`SystemSet.showlasthp`。可访问范围由 `HomepageUtil.getShareHomapage` 返回（user/role/dept/subcompany 共享）。mainPortal（§15.2 A2）以 `systemtemplate` 记录实现（`GetMainPortalCmd:50` 读 `SystemTemplate`+`SystemTemplateSubComp` 分部 `appoint`，`/api/portal/mainPortal/setMainPortal`，`MainPortalAction.java:372-393`）。
- **④ 登录前页面（LoginPageAction / HPINFO）**：`LoginPageAction.java:31` `@Path("/portal/loginpage")`，`/getLoginPages`→`GetLoginPagesCmd.java:38-40` 查 `hpinfo where creatortype=0 and subcompanyid=-1 and infoname is not null`——以 **`subcompanyid=-1 & creatortype=0`** 标记"登录前页面"（印证 §16.7①：loginportal=SystemLoginTemplate 登录前门户模板、loginpage=HPINFO 登录前页面，不同表/概念）。运行时由登录 SPA 拉取渲染，二者经"登录前"场景衔接但存储/渲染相互独立。

### 18.3 完整闭环 + 对 §1/§8/§15.2/§16.1 的 7 处纠偏

**闭环**：配置(`hpinfo`+`hpelement`/`hpElementSettingDetail`+`hplayout`+`SystemLoginTemplate`/`systemtemplate`/`HPINFO`) → 运行时(`GetHpAllElementJsonCmd→getBaseHpStr` 合并出 `hpinfo` JSON，元素按 `ebaseid` 异步取数，前端 `Homepage.js`+localStorage) → 个人化(`intiThisHpData` 复制创建者布局成用户专属 `hplayout`，改写本人行+`UserAddElementInfo`) → 门户选择(`ThemeStore` 解析 landing hpid = lasthpid→defaulthpid→全局默认；主题/mainPortal 的 `openTimer` 请求时比对)。

| # | 旧章节表述 | 修正（实证） |
|---|---|---|
| 1 | §1「两套渲染范式」默认 jsp/velocity | E9 实际是 **SPA(React)+后端 JSON**（`/api/portal/homepage/hpdata`）；`pc4backstage/portal4engine/` 是**后台管理 SPA**，运行时用户门户在 `src4js/pc/portal/`（→`/spa/portal/static/`） |
| 2 | §8「配置→展示」流程图列 `GetFrontHpCmd`/`GetFrontElementCmd`/`GetPortalElementCmd`/`GetHpsCmd` | 源码 **0 命中**；实际取数是 `HomepageAction./hpdata`+`GetHpAllElementJsonCmd` 与 `ElementsAction.{edata,tabdata,news,…}` |
| 3 | §15.2 A3 `HPDEFAULSETTING` | 实际表名是 **`hpdefualtsetting`**（拼写如此）；读取点在 `PortalMenuImplE8.getDefaulthpid` 与 `ThemeStore.js` |
| 4 | §16.1「定时切换调度器/定时任务」 | **不存在** Quartz Job；`openTimer` 切换是渲染时按当前时间比对（拉模式，`SptmFor*getTimerActive`/`GetMyThemeCmd` SQL） |
| 5 | 个人化"我的门户"被假设有 `hppersonalize`/`portal_user_setting`/`userhp` | **0 命中**；"我的门户"= `hplayout`+`hpElementSettingDetail`（按用户行）+ `UserAddElementInfo` |
| 6 | 缓存被假设有 `PortalCache`/`PageCache` | 无此类；门户页缓存是前端 localStorage(`portal-{hpid}-{isSetting}`)+Cominfo；`MenuCache` 仅缓存菜单（§14.3） |
| 7 | §16.1 称 `SystemLoginTemplate.DEFAULTSHOW` 引用 `hpinfo.id` | Java 源码 **0 命中**该列名；需以 DB 字典核对，不宜当运行字段 |

（以上 7 处已在正文 §1/§8/§15.2/§16.1 同步修正；全轮"误读→实证"统一汇总见 §21。）

---

## 19. 补充分析（第十一轮）：单点登录 / 身份集成（SSO/IDP）与门户衔接

> 探查对象：`weaver/interfaces/sso/*`、`weaver/weaversso`、`weaver/login/VerifyLogin.java`、`weaver/login/AuthenticUtil.java`、`com/engine/integration/*`、`com.api.integration.*`、`src/configure.vm`/`idp.vm`/`head.vm`、`interface/outter/*`、`data/`。背景：原文档仅说"`.vm` 模板仅 11 个用于 IDP/SSO"，从未系统深扒；门户元素 `outterSys`(集成登录) 也仅列入 §17 清单。本轮补齐，并纠偏 6 处常见误读。
> **前提纠偏**：文档曾假设 SSO 核心包 `weaver/security/authentic/`——**实测该包不存在**；防伪/可信登录核心是 `weaver.login.AuthenticUtil`(USB Key)+`weaver.login.VerifyLogin`，SSO 逻辑分散在 `weaver.interfaces.sso.*`、`weaver.weaversso`、`com.api.integration`、`com.engine.integration`。

### 19.1 SSO/IDP 配置面：三套相互独立的机制

**核心结论**：E-cology 的"单点登录/身份集成"由 **三套互不隶属** 的机制组成，代码实测支持的方式：

| 角色 | 方式 | 过滤器/类 | 后台配置入口 | 落库表（证据） |
|---|---|---|---|---|
| **SP（接外部 IdP）** | CAS | `CasSSOLoginFilter` | `/integration/sso/cas/casSetting.jsp` | `int_cas_setting`(`SQLServer/sql201609271103.sql:1`)+`int_cas_exclueurl(_sys)` |
| SP | OAuth2 | `OAuth2SSOLoginFilter` | `com/engine/integration/cmd/oauth2/*` | `Int_OAuth2_Cfg`(`Mysql/sql202911261703.sql:1`) |
| SP | SAML | `SAMLSSOLoginFilter`(OIOSAML) | `configure.vm`/`head.vm`(SP 自配置) | `Int_SAML_Cfg`(`Mysql/sql202003132703.sql:1`) |
| SP | WebSeal | `WebSealSSOLoginFilter` | `com/engine/integration/cmd/webSeal/*` | `int_webseal_setting`(`Oracle/sql201609181503.sql:1`)+`int_webseal_exclueurl` |
| SP | 微信企业号/Ebridge/AD 域(NTLM) | `WxworkDelegate_SSOLoginFilter`/`EbridgeSSOLoginFilter`/`ADSSOFilter`(jcifs) | — | — |
| **IDP（E-cology 发令牌）** | 统一认证中心 | `VerifyWeaverSSO`/`SimpleSSOAction`(`/integration/simplesso`)/`WeaverSSOLogin` servlet | `AuthenticationCenterAction`(`com/engine/integration/web/AuthenticationCenterAction.java:32,97` 注册应用列表) | `weaver_sso`(`ST/1table.sql:18619`)、`weaver_sso_app`(`:18629`)、`weaver_sso_user_mapping`(`:18653`)、`WEAVER_SSO_login_log`(`:18640`) |
| **凭据代填** | 集成登录(外部系统) | `OutterSysUtil`→`/interface/Entrance.jsp` | `/interface/outter/OutterSys*.jsp` | `outter_sys`/`outter_account`/`outter_sysparam`/`outter_params`(`Mysql/3table,record.sql:34975`) |
| 防伪/可信 | USB Key 硬件加密狗 | `AuthenticUtil`(WibuKey)+`VerifyLogin` | 系统→防伪设置 | `HrmResource.SERIAL`/`TOKENKEY`(`Mysql/1table,record.sql:23921`) |

IDP 支持的票据类型由 `weaver_sso` 多次 ALTER 扩展（`token_isuse`/`oauth2_isuse`/`spnego_isuse`/`dlgt_ec/em_isuse`/`oauth2_client_isuse`，`SQLServer/sql201910081303,202005072003,202006182503,202007021503`），`VerifyWeaverSSO.java:27-30` 把 CAS/TOKEN 映射到开关字段。

### 19.2 单点登录登录流程（运行时）

- **本地登录 + 防伪串(serial)**：`VerifyLogin.getUserCheck(..., String serial, ...)`（`weaver/login/VerifyLogin.java:49`）；serial 来自 USB Key（WibuKey）：`AuthenticUtil.decrypt(...)` `:405`，合法后 `user.setSerial(serialNo)` `:413`，可免/绑定密码。
- **SSO 过滤器链（SP 侧，在到达应用前拦截，不在 VerifyLogin 内判定）**：CAS 取 session assertion（`CasSSOLoginFilter.java:59-73`，移动端成功 redirect `/spa/hrm/static4sso/index.html` `:93`）；SAML 取 `UserAssertionHolder`（`SAMLSSOLoginFilter.java:64-70`，白名单 `:54`、账号映射 `SessionUtil.getUserIdByRule`）；OAuth2 校验 `code/access_token`（`OAuth2SSOLoginFilter.java:53-89`）；WebSeal 读 `int_webseal_setting`（`:75`）；AD 域 jcifs NTLM 取 Windows 用户（`ADSSOFilter.java:253`）。
- **E-cology 作为 IDP 签发/校验令牌**：TOKEN 由 `SimpleSSOAction.getToken` `AES.encrypt(loginid+"|"+ts+"|lzgaj","yjcust")` 生成并存 `sso_login_oa_lzgaj`（`com/api/integration/web/SimpleSSOAction.java:113,123`），外部系统用 token 换 session（`getUser:179`）；标准 `VerifyWeaverSSO.getRealLoginid` 校验 `appid`+`allow_ip`(`CheckIP.checkIPs:51-60`)+`account_rules`(loginid/workcode/email/mobile/certificatenum `:159-176`)+`weaver_sso_user_mapping`(`:84`)。
- **IDP 角色与模板职责（纠偏）**：E-cology 既当 SP（接外部 IdP）又当 IDP（签发票据）。`idp.vm`(`src/idp.vm:4` "Select Login IdP"、`:17-21` 遍历 `$entityIds`)/`configure.vm`(`:11` "Configure OIOSaml.java"、`:38` "Identity provider metadata")/`head.vm`(`:5` "OIOSaml.Java Configuration") **不是** E-cology 作为 IDP 发令牌给外部的配置页，而是 **OIOSAML SP 端**的"选择外部 IdP / SP 自配置"页。
- **与门户衔接**：SSO 过滤器构造好 `weaver_user@bean` 会话后，CAS 重定向 `/spa/hrm/static4sso/index.html` → 最终落到 §18 的 `/wui/main.jsp`→`/wui/index.html#/main` 门户主框架。

### 19.3 门户"集成登录"元素（outterSys）

**核心结论**：`outterSys` 是**凭据代填（form-fill）型**集成登录元素，点击跳 E-cology 自有 `/interface/Entrance.jsp?id=sysid`，由 `Entrance_*.jsp` 用该用户存于 `outter_account` 的（加密）账号密码**代填** POST 到外部系统（NC/K3/邮件等）。**它既不是 E-cology 作为 IDP 签发令牌，也不走 `weaver_sso_app`**——与 SSO/IDP 是两套独立体系。

- **分派/端点**：`ElementInfoCommonBiz.java:918` `ConstantUtil.elementType.OUTTERSYS → new OutterSysImplE8()`；`ElementsAction.@Path("/outtersys")`(`web/ElementsAction.java:1184` `getOutterSysDataJson`)→`IntegrationLoginService.getOutterSys`(`OutterSysImplE9.java:60-63`)。
- **取数/跳转**：`OutterSysUtil.dealMap` 读 `outter_sys` 按 `OutterDisplayHelper.getShareOutterSql` 过滤(`com/api/integration/util/OutterSysUtil.java:44-46`)；每项 `linkUrl=.../interface/Entrance.jsp?id=sysid`(`:124`，常量 `ConstantsUtil.java:45`)，未配置账号则 `routeUrl=/integration/accountSetting/`(`:151-152`)。代填页：`interface/Entrance.jsp`、`NcEntrance.jsp`、`K3CloudEntrance.jsp`、`Entrance_email163.jsp`、`Entrance_CoreMail.jsp` 等。
- **表**：`outter_sys`(`Mysql/3table,record.sql:34975`)/`outter_account`(`:34875` 每用户 account/password 加密)/`outter_sysparam`/`outter_params`/`outter_type`/`outter_encryptclass`。
- **前端**：`src4js/pc4mobx/portal/elements/components/outtersys/OutterSys.js`；后台配置 `components/common/setting/content/Content_outterSys.js`。

### 19.4 外部系统注册 / 可信关系

- **两套注册表**：① 集成登录凭据库（`outter_sys` 系列，供 outterSys 代填）；② IDP 应用注册库（`weaver_sso_app` 系列，供 E-cology 作为 IDP 互信）。
- **可信 IP 白名单 = `weaver_sso_app.allow_ip` 列**（`VerifyWeaverSSO.java:51-60` `CheckIP.checkIPs`）；**防伪/可信登录 = USB Key 硬件加密狗**（`AuthenticUtil` 基于 `com.wibu.WkJavaApi`，`ReadSerial():96-132`；`HrmResource.SERIAL/TOKENKEY`）。
- **无 `TrustedIps` 表**（全仓搜 `TrustedIps|trustedip|trusted_ip|TrustedLogin|TrustIP` 为 0）；SAML/WebSeal 另有 URL 白名单（`int_webseal_exclueurl`）。

### 19.5 关系结论 + 常见误读纠偏（6 处）

**E-cology 单点登录体系 vs 门户集成登录元素**：三套机制互不隶属——(a) SSO 过滤器(SP，接外部 IdP)、(b) 统一认证中心 `weaver_sso_app`(IDP，发令牌)、(c) 集成登录 `outter_sys`(凭据代填)。门户 `outterSys` 元素只对接 (c)，数据链是 `outter_sys→Entrance.jsp→外部系统`，与 (a)/(b) 无代码关联；门户"免密进入"靠 (a) 的 SSO 过滤器在 web 层构造会话后重定向，与 (c) 无关。

| # | 常见误读 | 实测（实证） |
|---|---|---|
| 1 | 有 `TrustedIps` 表做可信 IP | **无此表**；可信 IP = `weaver_sso_app.allow_ip` + `CheckIP` |
| 2 | `idp.vm/configure.vm/head.vm` 是 E-cology 作为 IDP 发令牌页 | 实为 **OIOSAML SP 端**"选择外部 IdP / SP 自配置"页 |
| 3 | `outterSys` 集成登录 = E-cology 作为 IDP 统一签发令牌 | 实为**凭据代填**（`outter_account` 加密账号密码 + Entrance.jsp），与 `weaver_sso_app` IDP 是两套独立体系 |
| 4 | SSO 过滤器在 `web.xml` 静态配置 | 主 `WEB-INF/web.xml` 未静态声明；CAS 等由 `/integration/sso/cas/resetCasWebxml.jsp` **动态改写 web.xml**，受安全规则保护 |
| 5 | LDAP 是 SSO 登录方式 | 实测 `com/engine/integration/ldap` 主要是**目录/人员同步**；域登录走 **ADSSOFilter(jcifs NTLM)**，非 LDAP 绑定登录 |
| 6 | 认证/SSO 核心包 `weaver.security.authentic` 存在 | **该包不存在**；防伪核心是 `weaver.login.AuthenticUtil`+`VerifyLogin`，SSO 分散在 `weaver.interfaces.sso.*`/`weaver.weaversso`/`com.api.integration`/`com.engine.integration` |

（以上 6 处已在 §1/§19.2–§19.4 修正；全轮"误读→实证"统一汇总见 §21。）

---

## 20. 补充分析（第十二轮）：移动门户运行时渲染 +「移动」两套独立引擎纠偏

> 探查对象：`com/engine/portal/web/{MPortalPageAction,MLoginPageAction}.java`、`cmd/mportalpage/GetMPortalPagesCmd.java`、`cmd/mloginpage/GetMLoginPageByUsingCmd.java`、`com/engine/mobilemode/biz/adapter/formmodel/PageAdapter.java`、`weaver/page/interfaces/impl/MobileHomepageImplE9.java`、`com/api/portal/web/MobileElementsAction.java`、`weaver/page/style/mobile/StyleUtil.java`、`weaver/homepage/mobile/cominfo/MobileElementStyleCominfo.java`、`com/weaver/formmodel/mobile/mec/servlet/MECAction.java`、`mobilemode/appHomepageView.jsp`、`src4js/mobile4mobx/portal/`。背景：§11.5 提"移动门户引擎（mobilemode）复用 PC 建模 `modehtmllayout`"、§15.6 称移动门户"复用 PC 建模 `modehtmllayout`"——本轮深扒移动运行时，发现**这两处把"移动门户"与"移动建模"两套系统混淆了**。

### 20.0 总览：E9 的"移动"其实是两套独立运行时引擎

| 维度 | ① 移动门户（Mobile Portal） | ② 移动建模 / 移动模式（Mobile Mode） |
|---|---|---|
| 管理入口 | `com.engine.portal.web.MPortalPageAction` 等（§15.6） | `com.engine.mobilemode` + `com.weaver.formmodel.mobile` |
| 首页/页面表 | **`hp_mobile_hpinfo`**（`GetMPortalPagesCmd.java:54`） | **`AppHomepage`**（`MobileAppHomepageManager`） |
| 布局/元素表 | `hp_mobile_hplayout`(`areaelement`)、`hp_mobile_Element`、`hpNewsTabInfo` | `MobileExtendComponent`（MEC） |
| 运行时数据 REST | `/api/mobile/portal/elements/*`（`MobileElementsAction`，`@Path("/mobile/portal/elements")`） | `MECAction`（按 `mec_id` 取数） |
| 运行时容器 | SPA `spa/portal`（源 `src4js/mobile4mobx/portal`） | JSP `/mobilemode/appHomepageView.jsp` + SPA `spa/mobilemode/engine.js` |
| 复用 PC 的什么 | 复用 **PC 门户**的"接口工厂 + ebaseid 组件"模式（§18 同源，兄弟实现） | 复用 **PC 建模**的 `modehtmllayout`（与门户无关） |

**三处关键纠偏**：
1. §11.5/§15.6 称"移动门户复用 PC 建模 `modehtmllayout`"——**不准确**：`modehtmllayout` 只被 `PageAdapter.java:111,118` 读取，产出的是 `AppHomepage`（移动建模页），**真正的移动门户 `hp_mobile_hpinfo` 从不读 `modehtmllayout`**。
2. `MPortalPageAction`/`MLoginPageAction`/`MElementStyleLibAction`/`MNavStyleLibAction` **全是管理侧**（带 `checkUserRight`/`getRightStatus`）；移动门户**运行时**是 `MobileElementsAction`+`MobileHomepageImplE9`+`ElementsTabContentData.jsp`。
3. `hpMobileStyle`(`menustyletype='melement'/'mnav'`) 是**自定义样式目录登记**，运行时真正套用的是配置目录里的 **XML 样式文件**（`MobileElementStyleCominfo.java:59,176,893`），不是直接查 `hpMobileStyle` 行。

### 20.1 移动端入口与 SPA 装配
- **移动门户 SPA（①）**：源 `src4js/mobile4mobx/portal/`，产物 `spa/portal/`；加载时拉 `apis/homepage.js:5,13` `getHomepageData`→`/api/mobile/portal/elements/elementJson`、`getElementData`→`/elements/tab`、`getNavbarMenus`→`/elements/portalMenuJson`（`apis/navbar.js:13`）。
- **移动建模 SPA（②）**：运行时壳 `mobilemode/appHomepageView.jsp`（登录校验 `MobileUserInit.getUser:26`、按 `appid/appHomepageId` 取 `AppHomepage:47-51`、权限 `:77-82`、`MECManager.parseUIContent:119`、输出 `<%=pageContent%>:211`）；引入 `mobilemode/js/mobile_homepage_wev8.js:138` 与按需 MEC 资源；独立引擎 `spa/mobilemode/engine.js`（`weaMobilemodeEngine`）。

### 20.2 移动门户页面运行时渲染
**核心结论**：移动门户运行时**不**走 PC 的 `HomepageCommonBiz.getBaseHpStr`，而是移动专属**接口工厂**（`MobileHomepageInterface`/`MobileElementsTabInterface`/`MobilePortalMenuInterface`，`MobileElementsAction.java:33-35`）→ `MobileHomepageImplE9`。
- 运行时 REST：`com.api.portal.web.MobileElementsAction` `@Path("/mobile/portal/elements")`（`MobileElementsAction.java:30`）：`/elementJson`→`MobileHomepageImplE9.getHpAllElementJson`（`:67-79`→`:39,48`）、`/tab`→`MobileElementsTabInterface.getElementTabContentDataJson`（`:44-57`，经 `ElementsTabContentData.jsp:` `ELEMENTTABCONTENTDATA_URL` `:37`）、`/portalMenuJson`→`MobilePortalMenuInterface`（`:88-113`）。
- 数据组装 `MobileHomepageImplE9.getHpAllElementMap:48`：读布局 `select areaelement from hp_mobile_hplayout where hpid=? and userid=? and usertype=3`（`:63`），按 `ebaseid` 调 `getElement(...)`（`:80,99`）产出 `item{ eid,ebaseid,showType,styleid }`+`header`+`tabs[]`；元素按 `ebaseid` 一对一渲染（对应 `Content_<ebaseid>.js`，§17 已列；运行时 SPA `mobile4mobx/portal` 按 ebaseid 注册）。
- **表**：`hp_mobile_hpinfo`（`GetMPortalPagesCmd.java:54`）、`hp_mobile_hplayout`（`MobileHomepageImplE9.java:63`）、`hp_mobile_Element`（`:164` `styleid,isuse`）、`hpNewsTabInfo`（ebaseid=8 改 `hpsetting_wfcenter`，`:113-117`）。

### 20.3 复用 `modehtmllayout`（属于"移动建模"而非"移动门户"）
`PageAdapter.java:110-116` `select * from modehtmllayout where modeid=? and isdefault=1 and type=?`（默认布局）、`:118` `where id=?`（指定布局）→ `createPageContent(...)` 转成 `AppHomepage.pageContent`（`:144-146`）→ `getFormmodelComponents` 读 `mode_visual_component`（`:250-252`）映射成 MEC 组件 → `MECAction.saveOrUpdate` 落 `MobileExtendComponent`、`:230` `insert into AppHomepage_Model`。**`hp_mobile_hpinfo` 链路从头到尾不碰 `modehtmllayout`。** 准确表述应是"**移动建模（移动模式）引擎复用 PC 建模 `modehtmllayout`**"。

### 20.4 移动登录页渲染
**核心结论**：移动登录页用**独立表 `portal_mloginpage`**（结构化 `*config` JSON：背景/Logo/表单/按钮/文本按钮/版权/二维码），**非复用 PC `SystemLoginTemplate`**。
- 运行时接口：`MLoginPageAction.@GET /getMLoginPageByUsing`（`MLoginPageAction.java:140-160`，**无需用户权限**，传 `cookies/Referer/Host/basePath/intactUrl`）→ `GetMLoginPageByUsingCmd.java:23`：定时生效 `select ... from portal_mloginpage where openTimer=1 and timerStartDate<=? and ?<=timerEndDate`（`:40`）、域名绑定 `where isdeleted=0 and domainname is not null` 按 `headerReferer/basePath/host` 匹配（`:65-76`）、默认启用 `where isusing=1`（`:81`）；输出 `backgroundconfig/logoconfig/formconfig/loginbuttonconfig/textbuttonconfig/copyrightconfig/qrcodeconfig`（`JSONObject.parse`，`:84-100`）。
- 表：`portal_mloginpage`（独立表，与 PC `SystemLoginTemplate` 无代码交叉）。

### 20.5 移动主题/样式
- **① 移动门户**：`hpMobileStyle` 由 `MElementStyleLibAction.uploadMElementStyle:339-340`(`menustyletype='melement'`)、`MNavStyleLibAction:388-389`(`mnav`) 写入——是**目录登记**，非渲染时直接取 CSS。`StyleUtil.getElementStyleCssByHpId:146`：门户级 `select styleid from hp_mobile_hpinfo`（`:149`）→`MobileElementStyleCominfo.getCss`（`:155,225`），元素级 `select styleid,id from hp_mobile_Element where hpid=? and isuse=1 and styleid is not null`（`:164`）；**样式内容来自配置目录 XML**（`MobileElementStyleCominfo.java:59,176,893` `mobile.style.conf`+`filename+".xml"`），默认导航样式 `hpmobilenavsetting`→`navstyleid`（`:133-138`）。
- **② 移动建模**：`appHomepageView.jsp:99-103` `SkinManager.getSkinCssPathWithApp(appid)`（可被 `isDisabledSkin` 关，`:95`），皮肤目录 `/mobilemode/skin`（`SkinManager.java:65`）。
- E9 主题：`spa/portal`/`mobile4mobx` 走 E9 主题变量注入（门户 SPA 层），底层移动门户样式仍以上述 `MobileElementStyleCominfo`/`hpMobileStyle` 为准。

### 20.6 与 PC 运行时（§18）的异同 + 纠偏

**相同（移动门户 ① ≈ PC 门户，同源兄弟）**：都"接口工厂 + 版本实现"（`MobileHomepageInterface` 对应 PC `HomepageInterface`）、都按 `ebaseid→Content_<ebaseid>.js` 渲染、都是"SPA 拉 JSON→后端取数"（`/mobile/portal/elements/elementJson` 对应 PC `/hpdata`+`getBaseHpStr`）。

**不同（重要）**：
1. **实现独立**：移动门户用 `MobileHomepageImplE9`（依赖 `MobilePageUtil/ElementUtil/StyleUtil/MobileElementCominfo` 等移动专属类，`:30-35`），**不调用 PC `HomepageCommonBiz.getBaseHpStr`**；是兄弟实现，不是移动复用 PC 方法。
2. **移动建模（②）与 PC 门户无关**：`AppHomepage`+`MECAction`+`appHomepageView.jsp` 是第三套引擎，复用的是 **PC 建模 `modehtmllayout`**（§20.3），非 PC 门户。
3. **取数接口不共享 PC `ElementsAction.{edata,...}`**：移动门户 Tab 取数走 `MobileElementsTabInterface`（经 `ElementsTabContentData.jsp`）；移动建模走 `MECAction`（`getMecHtml/getListDataWithPage/...`，`MECAction.java:93,971,1140,839,1196`），PC 完全没有。
4. **登录页**：移动 `portal_mloginpage` vs PC `SystemLoginTemplate`，互不复用。
5. **样式**：移动门户 `hpMobileStyle`+XML 目录+`hpmobilenavsetting` vs PC `hpec`/主题样式表；移动建模另有 `SkinManager`。

> **一句话**：所谓"移动门户运行时"有两条链路——**移动门户(`hp_mobile_hpinfo`)是 PC 门户运行时在移动端的同源兄弟实现**；**真正复用 PC 建模 `modehtmllayout` 的是移动建模(`AppHomepage`/MEC)**。§11.5/§15.6 将"移动门户引擎"与"`com.engine.mobilemode`+`modehtmllayout`"画等号，是把两套系统混淆了（已在本节与 §11.5/§15.6 一并修正）。

---

## 21. 统一勘误 Changelog（全轮"误读→实证"汇总）

> 本文档在逐轮深扒中，对早期基线章节（§1/§3/§8/§11.5/§15/§16）的假设性描述作了多轮代码实证纠偏。为避免散落，本节**统一汇总**全部"误读→实证"对照，并标注其在正文何处已修正。详细证据可回查对应原纠偏表（§18.3 渲染类、§19.5 SSO 类、§20 移动类）；本节为权威汇总。

### 21.1 渲染范式 / 运行时
| # | 原误读（早期章节） | 实证修正 | 正文修正处 |
|---|---|---|---|
| R1 | §1 默认 E9 门户是 jsp/velocity 双范式；后台门户 SPA 在 `pc4backstage/portal4engine/` | E9 用户门户是 React SPA（`src4js/pc/portal/`→`/spa/portal/static/`），后台管理 SPA 独立；运行时非 jsp 渲染 | §1、§18.1 |
| R2 | §8 流程图列 `GetFrontHpCmd`/`GetFrontElementCmd`/`GetPortalElementCmd`/`GetHpsCmd` 为渲染命令 | 源码 0 命中；真实入口 `HomepageAction./hpdata`→`GetHpAllElementJsonCmd`→`HomepageCommonBiz.getBaseHpStr` | §8、§18.1/§18.3① |
| R3 | 假设门户有前端缓存层 `PortalCache`/`PageCache` | 不存在；缓存即前端 localStorage + Cominfo | §18.3⑥ |
| R4 | §16.1 称登录门户有"定时切换调度器"(Quartz Job) | 无 Quartz Job；为渲染时按当前时间比对的拉模式 | §16.1、§18.3④ |
| R5 | §16.1 称 `SystemLoginTemplate.DEFAULTSHOW` 列 | 该列名在 Java 源码 0 命中，需以 DB 字典核对 | §16.1、§18.3⑦ |

### 21.2 表名 / 数据模型
| # | 原误读 | 实证修正 | 正文修正处 |
|---|---|---|---|
| D1 | §15.2 A3 默认首页映射表 `HPDEFAULSETTING` | 实际表名 `hpdefualtsetting`（如此拼写）；`SaveDefaultPortalCmd.java:50-54` 写 | §15.2、§18.3③ |
| D2 | §3/§18 假设"我的门户"靠 `hppersonalize`/`portal_user_setting`/`userhp` 等个人化表 | 无此类表；个人化是创建者布局复制成用户专属 `hplayout`+`hpElementSettingDetail`+`UserAddElementInfo` | §18.2/§18.3⑤ |
| D3 | §16 假设登录门户配置存 properties 或由 `PortalConfigComInfo` 管理 | 真实表 `portalconfig` + `weaver.portal.util.PortalConfigUtil`；`PortalConfigComInfo` 不存在 | §16.6 |
| D4 | §16 假设 tsportal 是"特色/定时门户"有定时逻辑 | 实为 `PortalGridLayoutAction` 管理网格布局 `hp_grid_layout`，无定时 | §16.2 |

### 21.3 SSO / IDP / 集成登录（§19）
| # | 原误读 | 实证修正 | 正文修正处 |
|---|---|---|---|
| S1 | 有 `TrustedIps` 表做可信 IP | 无此表；可信 IP = `weaver_sso_app.allow_ip` + `CheckIP.checkIPs` | §19.4/§19.5① |
| S2 | `idp.vm`/`configure.vm`/`head.vm` 是 E-cology 作为 IDP 发令牌页 | 实为 OIOSAML **SP 端**"选择外部 IdP / SP 自配置"页 | §1、§19.2/§19.5② |
| S3 | `outterSys` 集成登录 = E-cology 作为 IDP 签发令牌 | 实为**凭据代填**（`outter_account` 加密账号 + `/interface/Entrance.jsp`），与 `weaver_sso_app` IDP 独立 | §19.3/§19.5③ |
| S4 | SSO 过滤器静态配置于 `web.xml` | 主 web.xml 未静态声明；CAS 等由 `resetCasWebxml.jsp` 动态改写 web.xml | §19.2/§19.5④ |
| S5 | LDAP 是 SSO 登录方式 | 实测 LDAP 主要是目录/人员同步；域登录走 `ADSSOFilter`(jcifs NTLM) | §19.5⑤ |
| S6 | 认证/SSO 核心包 `weaver.security.authentic` 存在 | 该包不存在；防伪核心 `weaver.login.AuthenticUtil`+`VerifyLogin`，SSO 分散 `weaver.interfaces.sso.*`/`weaver.weaversso`/`com.api.integration` | §19.5⑥ |

### 21.4 移动端（§20）
| # | 原误读 | 实证修正 | 正文修正处 |
|---|---|---|---|
| M1 | §11.5/§15.6 称"移动门户复用 PC 建模 `modehtmllayout`" | 复用 `modehtmllayout` 的是**移动建模**(AppHomepage/MEC，`PageAdapter.java:111,118`)；移动门户 `hp_mobile_hpinfo` 从不读它 | §11.5、§15.6、§20.3 |
| M2 | `MPortalPageAction`/`MLoginPageAction` 是移动运行时接口 | 全是**管理侧**(带 `checkUserRight`)；运行时是 `MobileElementsAction`+`MobileHomepageImplE9`+`ElementsTabContentData.jsp` | §20.1/§20.2 |
| M3 | `hpMobileStyle` 直接提供移动样式 CSS | 是目录登记；运行时取配置目录 XML（`MobileElementStyleCominfo`） | §20.5 |
| M4 | 移动登录页复用 PC `SystemLoginTemplate` | 独立表 `portal_mloginpage`(结构化 config JSON)，无代码交叉 | §20.4 |

---

## 22. 补充分析（第十三轮）：门户数据权限与组织可见范围（三条轴）

> 探查对象：`com/engine/portal/web/{ElementsAction,MainPortalAction}.java`、`weaver/page/interfaces/impl/HomepageImplE9.java`、`weaver/homepage/{HomepageCommonBiz,HomepageUtil,HomepageExtShow}.java`、`weaver/homepage/style/ElementUtil.java`、`com/engine/portal/util/ManageDetachComInfo.java`、`weaver/portal/util/PageUtil.java`、`weaver/doc/DocManager.java`。背景：§17.2 已覆盖门户**管理权限**（`PortalRight` 14 码 + `ManageDetachComInfo` 分权 + 共享表 `commonshareinfo`/`elementshareinfo`/`hpCustomShare`），但只解决了"谁能设计"；**"终端用户最终看到什么"（门户可见 + 元素可见 + 数据型元素按身份过滤）从未系统深扒**。本轮补齐，并纠偏 §17.2 把三套共享表混为一谈的问题。

### 22.0 总览：门户数据权限是"三条独立轴"
| 轴 | 决定"谁" | 核心表 / 接口 | 关键编码（type 语义，各表不同） |
|---|---|---|---|
| **(A) 管理权限**（谁能设计/维护） | 管理员/子管理员 | `PortalRight`(`homepage:Maint` 等) + `PageUtil.getUserMaintHpidList` → **`ptaccesscontrollist`**(permissiontype×operationcode=1,dirtype=0) + `ManageDetachComInfo` | 5人员/6分部/1部门/3安全级别/2角色/7岗位 |
| **(B) 终端可见**（用户看到哪些门户） | 终端用户 | `HomepageUtil.getShareHomapage` → **`shareinnerhp`**(type+content) + `hpinfo.isuse=1,showtype=1` + `hpdefualtsetting`/`hpcurrentuse` | 1部门/2角色/6分部（与 C 轴编码不同！） |
| **(C) 元素可见**（用户看到哪些元素） | 终端用户 | `ElementUtil.isHasRightSingle` → **`elementshareinfo`**(+`hpelement.shareuser`)；个性化级别 `hpElementSettingDetail.sharelevel` | 1人员/2分部/3部门/5所有人/6角色/7安全级别 |
| (D) 数据型元素业务数据过滤 | 当前登录人身份/权限 | `DocManager`(文档共享表)/`workflow_currentoperator.userid`/`WeavermailUtil.getUserMailInfo(uid)` | 文档阅读权限、流程在办人、个人邮箱 |

> **核心纠偏**：§17.2 将 `commonshareinfo`/`elementshareinfo`/`hpCustomShare` 统称为"共享统一"——实际它们是**使用/可见共享轴**（工具栏/快捷搜索、元素可见、自定义组件可见）；而**首页"可维护"轴是 `ptaccesscontrollist`**、**首页"终端可见"轴是 `shareinnerhp`**。三套表、三种 `sharetype` 编码，不能混为一谈（见 §22.5④⑤）。

### 22.1 门户级终端用户可见性（B 轴）
- **分配即 `shareinnerhp`**：门户按组织/角色/人员"分配"**没有专门的门户-人员清单表**，而是 `shareinnerhp`(hpid + type + content) 的行——如 `type=6 and content=分部id` 表示对该分部可见、`type=5 and content=人员id` 表示对具体人可见。
- **可见判定**：`HomepageUtil.getShareHomapage` 读取 `shareinnerhp` 拼接"该用户可见门户" SQL；叠加 `hpinfo.isuse=1`(启用开关) 与 `showtype=1`(类型判别)；登录后实际落地再由 `hpdefualtsetting`(机构默认)/`hpcurrentuse`(个人上次) 决定首选（详见 §18.2）。`SystemTemplate`(登录后门户模板) 用 `companyId`(按机构) + `isOpen`(启用)，属 B 轴外层入口，与 `shareinnerhp` 非同表。
- **启用/下线/类型开关**：`HomepageCominfo` 缓存列（`:37-69`）只有 `isUse/isLocked/creatortype/creatorid/subcompanyid/showtype/...`；**启用= `isUse`，类型=`showtype=1`，个性化锁=`isLocked`**。源码**无 `isclose`/`useright` 字段**（§22.5③）。

### 22.2 元素级终端用户可见性（C 轴）
- **能否看到由 `elementshareinfo` 经 `ElementUtil.isHasRightSingle` 计算**（`hpelement.shareuser` 单列也参与）；**无共享记录 = 默认全员可见**。
- **`hpElementSettingDetail.sharelevel` 不是可见性字段**：它只是**每用户对每元素的个性化级别**（1=锁定/2=解锁），控制用户能否拖拽/编辑该元素，**不决定谁能看到**（§22.5②）。
- 元素共享 `type` 编码（`elementshareinfo`）：1人员/2分部/3部门/5所有人/6角色/7安全级别——**与 `shareinnerhp`(B轴:1部门/2角色/6分部) 编码不一致**，定位共享逻辑必须先看对表（§22.5⑤）。

### 22.3 数据型元素按当前用户过滤业务数据（D 轴）
选典型数据型元素实证其取数如何注入当前身份：
- **新闻/文档**（ebaseid 文档类）：自定义 `newsclause`(用户范围) 与系统文档阅读权限表 **以 AND 并存**——`DocManager.java:2234-2235` `getShareDetailTableByUserNew("doc",user)` 注入阅读权限过滤，自定义 where 不能绕过（§22.5⑥）。
- **待办/流程**（ebaseid 流程类）：直接以当前 `userid` 硬过滤（如 `workflow_currentoperator.userid = 当前用户`），共享机制在此层不介入。
- **邮件**（ebaseid 邮件类）：`WeavermailUtil.getUserMailInfo(uid)` 按当前用户取个人邮箱。
- **结论**：元素 `hpelement.strsqlwhere`(用户自定义) 与系统安全过滤**并存**（AND），系统层以当前 `user.getUID()`/`userid` 强过滤，自定义 SQL 无法越权（§22.5⑥）。

### 22.4 管理权限轴（A）与分权
- **可维护共享 = `ptaccesscontrollist`**：`PageUtil.getUserMaintHpidList` 据此返回某管理员可维护的 hpid 列表（`permissiontype`×`operationcode=1,dirtype=0`）；`PortalRight` 14 码是更粗的菜单级权限闸门（§17.2）。
- **`ManageDetachComInfo` 只影响管理/维护轴**：限制子管理员机构范围（`GetMainPortalsCmd`、维护身份上下文 `getHpUserId/getHpUserType`）；**终端可见轴 `getShareHomapageSql` 不引用 `ManageDetachComInfo`**——即分权不改变终端用户能看到哪些门户（§22.5⑦）。

### 22.5 全景结论 + 7 处纠偏
**三条轴 + 数据过滤轴必须分开**：(A) 管理权限 `ptaccesscontrollist`+`PortalRight`+分权；(B) 终端可见 `shareinnerhp`；(C) 元素可见 `elementshareinfo`；(D) 业务数据按身份 `DocManager`/`workflow_currentoperator`/`WeavermailUtil`。§17.2 的 `commonshareinfo`/`elementshareinfo`/`hpCustomShare` 属"使用/可见"层，需补充 `ptaccesscontrollist`(维护) 与 `shareinnerhp`(门户终端可见)。

| # | 常见误读 | 实证修正 |
|---|---|---|
| 1 | 有专门的"门户-人员分配表" | 无；门户分配 = `shareinnerhp`(hpid+type+content) 行表达 |
| 2 | 元素可见性靠 `hpElementSettingDetail.sharelevel` | `sharelevel` 仅个性化级别(1锁/2解)，**不决定可见**；可见由 `elementshareinfo` 经 `isHasRightSingle` 算，**无记录=默认全员可见** |
| 3 | `HPINFO` 有 `isclose`/`useright` 字段 | 无；`HomepageCominfo` 缓存列只有 `isUse/isLocked/creatortype/...`；启用=`isUse`、类型=`showtype`、锁=`isLocked` |
| 4 | §17.2 的 `elementshareinfo`/`hpCustomShare`/`commonshareinfo` 就是"门户共享全部" | 它们是**使用/可见轴**；首页**可维护**= `ptaccesscontrollist`、**终端可见**= `shareinnerhp`——三表三编码 |
| 5 | 元素共享 `type` 编码全局一致 | 各表不同：`shareinnerhp` 1部门/2角色/6分部；`elementshareinfo` 1人员/2分部/3部门/5所有人/6角色/7安全级别；`hpCustomShare` 1人员/2分部/3部门/6角色/7所有人/8岗位 |
| 6 | 用户自定义 `strsqlwhere` 能绕过系统安全过滤 | 不能；新闻范围与文档阅读权限 AND 并存(`DocManager.java:2234-2235`)；流程/邮件直接按当前 `userid` 硬过滤 |
| 7 | `ManageDetachComInfo` 影响终端用户可见门户 | 不影响；分权只限管理/维护轴，终端可见 `getShareHomapageSql` 不引用它 |

---

## 23. 补充分析（第十四轮）：门户设计态编辑器 + "我的门户"个性化保存写回

> 探查对象：`src4js/pc4backstage/portal4engine/`（引擎级管理向导）、`src4js/pc4mobx/portal/`（页面内容编辑器：`Homepage.js`/`ESetting.js`/`util/drag.js`/`components/HpBaseElement.js`）、`com/engine/portal/web/{MainPortalAction,PortalEditInfoAction,HpBaseElementsAction,ElementToolbarAction,PageLayoutAction}.java`、`cmd/portaledit/SavePortalEditDataCmd.java`、`cmd/esettingupdate/AddElementToPortalCmd.java`、`cmd/elementsetting/SaveElementSettingCmd.java`、`weaver/page/interfaces/esetting/save/ElementsSettingSaveOprationE8.java`、`biz/homepage/HomepageCommonBiz.java`(intiThisHpData)。背景：§13/§14 已覆盖页面/元素**后端 REST 与表**、§17 列了 `Content_<ebaseid>.js` 组件、§18 深扒**运行态**，但**设计态"怎么搭/怎么存"与"终端用户个性化怎么写回 hplayout/hpElementSettingDetail/UserAddElementInfo"从未系统深扒**。本轮补齐，并与 §18 运行态呼应。

### 23.0 先纠偏两点源路径假设
- **"页面设计器在 portal4engine/mainportal"——只中一半**：`MainPortalAction`/`MainPortalStore` 只写 **`SystemTemplate`/`SystemTemplateSubComp`**（顶层"登录后门户"模板选择），**不写 hplayout/HPINFO**。真正的"页面内容设计器（拖拽/加元素/配置）"是 **`pc4mobx/portal/`**；`portal4engine` 负责引擎级管理（向导 `mainpage`、布局模板 `pagelayout`、元素库）。
- **命令类名纠偏**：不存在 `PortalPageAction`/`SavePortalPageCmd`/`SavePageLayoutCmd`；真实为 `MainPageAction`+`PortalEditInfoAction`(REST)、`SavePortalEditDataCmd`(封装 `HomepageMaintOperate`)、`SavePortalLayoutCmd`；元素配置命令是 **`SaveElementSettingCmd`**(`cmd/elementsetting/`)。

### 23.1 设计态：管理员搭建门户页面并保存
- **向导**：`portal4engine/components/mainpage/Add.js`（三步：布局 `Add4Step1`→样式 `Add4Step2`→信息 `Add4Step3`）；保存 `stores/mainpage/MainPageAddStore.js:84-110` → `POST /api/portal/portalinfo/operate`（module=mainpage, method=ref/savebase/saveNew）；编辑 `MainPageStore.js:107-110`(save)、`:155`(delhp)。
- **后端**：`web/PortalEditInfoAction.java:33` `@Path("/portal/portalinfo")` + `:87` `@Path("/operate")` → `savePortalInfo`；命令 `cmd/portaledit/SavePortalEditDataCmd.java:167-169` 按 module 分发：`mainpage`→`new HomepageMaintOperate().operate()`。
- **写表**（`com/api/portal/backend/service/util/HomepageMaintOperate.java`）：`method=ref` 新建→`insert hpinfo`(`:208`)+逐区 `insert hplayout(hpid,layoutbaseid,areaflag,areasize,areaElements,userid,usertype)`(`:243`)+`insert shareinnerhp`(`:266`)；`method=savebase` 编辑→`update hpinfo`(`:787`)+增删改 `hplayout`(`:828/:831/:842`)；`method=saveNew` 另存为/复制模板→把源 `hpinfo` 的 `hpelement/hplayout/hpElementImg/hpElementSetting/hpElementSettingDetail/hpNewsTabInfo` 整段按新 hpid 复制(`:321-719`)。写表清单：`HPINFO`/`HPLAYOUT`/`HPELEMENT`/`hpElementSetting`/`hpElementSettingDetail`/`shareinnerhp`/`hpFieldLength`。

### 23.2 元素配置（`Content_<ebaseid>` 组件）保存
- **前端**：统一配置弹窗 `pc4mobx/portal/elements/components/common/setting_new/ESetting.js:68-125` → `POST /api/portal/setting/saveSetting`（各元素内容/样式/共享来自 `elements/components/<ebaseid>/`）。
- **后端**：命令 `cmd/elementsetting/SaveElementSettingCmd.java:130` → `esso.saveElementSetting`；实际写表 `weaver/page/interfaces/esetting/save/ElementsSettingSaveOprationE8.java`：`update hpElementSettingDetail set perpage,linkmode,showfield,... where eid=? and userid=? and usertype=?`(`:208-209`)；`esharelevel='2'` 时可编辑时 `update hpElement set title,strsqlwhere,styleid,height,... where id=?`(`:254-255`，全局元素配置)；`hpElementSetting`(eid+name+value) 与共享表一并写。**即元素配置同时写多张表**（§23.5②）。

### 23.3 "我的门户"个性化：终端用户保存写回
与设计态**共用同一批后端**，仅靠 `userid/usertype` 区分（普通用户 `usertype=1` vs 创建者/管理员 `usertype=3`）。
- **加元素**（含用户新增自定义元素→`UserAddElementInfo`）：`pc4mobx/portal/components/HpBaseElement.js:46` → `POST /api/portal/hpbaseelement/addElement` → `web/HpBaseElementsAction.java:31` `@Path("/portal/hpbaseelement")`+`:68` `@Path("/addElement")` → `cmd/esettingupdate/AddElementToPortalCmd.java`：写 `insert hpElement`(`:137`)、按用户行 `update/insert hplayout where hpid=? and areaflag=? and userid=? and usertype=?`(`:156-180`)、`insert hpareaelement`(`:206`)、`insert hpElementSettingDetail(hpid,eid,userid,usertype,...)`(`:209`)、**`insert UserAddElementInfo(eid,userid,hpid,areaflag)` 当且仅当 `usertype==1 && hpid>0 && subCompanyId>0`**(`:213-215`)。
- **拖拽/删除**（写回用户行 hplayout）：拖拽 `pc4mobx/portal/elements/util/drag.js:222` → `POST /api/portal/elementtoolbar/drag` → `web/ElementToolbarAction.java:121` `@Path("/drag")`；删除 `:40` `@Path("/delete")`——均按查看者用户行改 `hplayout`/`UserAddElementInfo`。
- **元素个性化配置**：同 §23.2 `ElementsSettingSaveOprationE8:208-209`，WHERE 即 `eid+userid+usertype`（用户行优先）。

### 23.4 `intiThisHpData`（首次进入自动初始化，与手动保存区别）
- 位置 `biz/homepage/HomepageCommonBiz.java:305-399`（同 `Homepage2CommonBiz.java:306-399`），运行态由 `getBaseHpStr` 调用(`:100`)。
- 职责：被锁定页跳过(`:306`)；**用户已存在 hplayout 行则跳过**(`:321-324`)；否则把创建者行 `insert...select...from hplayout where userid=hpCreatorId and usertype=hpCreatorType` 复制成用户专属行(`:328-330`)；复制 `hpFieldLength`(`:357-360`)、把 `UserAddElementInfo` 该用户新增元素合并进用户行 areaElements(`:364-394`)。
- **区别**：自动初始化=读时拷贝创建者→用户行（首次无个性化数据）；手动保存=用户在编辑器拖拽/加元素/配置后即时写用户行。两者落同一 `(hpid,userid,usertype)` 行互不覆盖——一旦用户手动改过，`intiThisHpData` 因已存在行直接 return。

### 23.5 设计态↔运行态呼应 + 6 处纠偏
**读取点与优先级**（`HomepageCommonBiz.getBaseHpStr`，`cmd/homepage/GetHpAllElementJsonCmd.java:111/117` 调用）：先 `intiThisHpData`(`:100`)；取查看者 `userid/usertype`(`:103-104`)；**页面 `isLocked=1` 则改取创建者行**(`:117-120`，锁定页强制显示创建者布局)；**优先读用户行 hplayout**(`select areaflag,areasize,areaElements from hplayout where hpid=? and userid=? and usertype=?`，`:121`)；逐元素优先读用户行 `hpElementSettingDetail`(`:206`)。即 hpinfo+hplayout(用户行优先)+hpElementSettingDetail(用户行优先) 合并产出运行态 JSON（呼应 §18 的 `getBaseHpStr`）。

| # | 常见误读 | 实证修正 |
|---|---|---|
| 1 | 个性化"直接改原 hplayout 行" | 实为写**按 `(hpid,userid,usertype)` 的用户专属行**，原创建者行不动；运行态按用户行优先读，`intiThisHpData` 才在无数据时拷贝创建者行 |
| 2 | 元素配置"只写一张表" | 实际同时写 **hpElementSettingDetail(用户/创建者行) + hpElementSetting(eid+name+value) +（可编辑时）hpElement** |
| 3 | 存在 `SavePortalPageCmd`/`SavePageLayoutCmd`/`PortalPageAction` | 真实为 `SavePortalEditDataCmd`(封装 `HomepageMaintOperate`)、`SavePortalLayoutCmd`、`MainPageAction`/`PortalEditInfoAction` |
| 4 | 页面内容设计器在 `portal4engine/mainportal` | `mainportal` 只管 `SystemTemplate`；页面内容设计器是 `pc4mobx/portal/` |
| 5 | "我的门户"有单一"保存"按钮一次性提交 | 拖拽/加元素/删除/配置均为**即时写回用户行**；"重置/同步我的门户"是管理员经 `portalrcmenu/{updatesynihp\|completesynihp}`(`UpdateSyniHp.java:147-159`/`CompleteSyniHp.java:131-135`，DELETE 用户个性化行+`UserAddElementInfo`)触发的反向操作 |
| 6 | 设计态/个性化实现均可从 .java 逐行取证 | `ESettingUpdateServiceImpl`/`ElementToolbarServiceImpl`/`MainPageServiceImpl` 仅以 .class 存在（源码未分发）；但 `AddElementToPortalCmd`（命令类源码可读）已完整证明加元素/个性化写回三张表 |

---

*本文档基于对 `E:\project\ecology\ecology\src` 与 `D:\Weaver2020\ecology` 的源码/配置/表结构实际探查整理，所有路径与表名均经代码验证。*
