# 泛微 E9 内容引擎（DocEngine）架构与运作机制深度分析

> 分析对象
> - 前端：`D:\Weaver2020\ecology`
> - 后端：`E:\project\ecology\ecology\src`
> - 目标页面：`http://<host>/wui/engine.html#/docengine/index`
>
> 版本说明：**v4（查实修正 + 覆盖度补充 + 缺陷定论版）**。v1→v2：6 处推断被源码证伪并修正（见 §0.2）；v2→v3：经覆盖度排查新增 §13–§18（模板与字段、其它功能域、移动端、横切机制、前端工程、排查清单）；v3→v4：定向验证并**确认**「自定义字段加密静默失效」缺陷（新增 §18.4）。
> 所有结论均附可复核的文件路径 / 类名 / 行号 / URL / 表名证据；无法查实的项统一列入 §12 与 §18。

---

## 目录

- [0. 阅读指引与结论摘要](#0-阅读指引与结论摘要)
- [1. 总体架构](#1-总体架构)
- [2. 前端架构](#2-前端架构)
- [3. 后端架构（含 REST 映射机制·已查实）](#3-后端架构含-rest-映射机制已查实)
- [4. 文档生命周期](#4-文档生命周期)
- [5. 权限与共享模型](#5-权限与共享模型)
- [6. 在线编辑 / 版本 / 附件 / 转换](#6-在线编辑--版本--附件--转换)
- [7. 检索与搜索](#7-检索与搜索)
- [8. 数据模型](#8-数据模型)
- [9. 前后端职责划分](#9-前后端职责划分)
- [10. 与公文模块（odoc）的关系](#10-与公文模块odoc的关系)
- [11. 关键路径清单](#11-关键路径清单)
- [12. 已核实结论与已知边界](#12-已核实结论与已知边界)
- [13. 模板体系与自定义字段（v3）](#13-模板体系与自定义字段v3)
- [14. 其它功能域（v3）](#14-其它功能域v3)
- [15. 移动端（v3）](#15-移动端v3)
- [16. 横切机制（v3）](#16-横切机制v3)
- [17. 前端工程与部署（v3）](#17-前端工程与部署v3)
- [18. 覆盖度排查清单与边界风险（v3）](#18-覆盖度排查清单与边界风险v3)

---

## 0. 阅读指引与结论摘要

### 0.1 三条最关键的认知

1. **`docengine` 是「内容引擎」的后台管理端**，不是终端用户的文档操作界面。
   源码中模块标签即「内容引擎」（`getLabel(33638,'内容引擎')`，`src4js/pc4backstage/document/components/docEngineIndex/index.js`）。
2. **文档的「创建 / 编辑 / 发布 / 检索 / 评论 / 在线编辑」都不在 `#/docengine/` 下**，而在另外两套 SPA：内容前台 `#/document/*`（`src4js/pc4mobx/document`）与文档详情 `#/main/document/*`（`src4js/pc4mobx/documentDetail`）。
3. 内容引擎 = **3 套前端 SPA + 2 个后端包**，共享同一套 REST 与数据模型；后端采用 **「REST Action → Service → 命令模式 Cmd → RecordSet 原生 SQL」** 四层结构。

### 0.2 v2 修正（v1 的推断被证伪的部分）

| # | v1 推断 | v2 查实结论 | 证据 |
|---|---|---|---|
| 1 | 后端 URL 前缀「通常 `/api/engine/doc/...`」 | **错误**。不存在 `/api/engine/...`。真实机制：Jersey 1.x servlet 映射 `/api/*`，仅扫描 `com.cloudstore;com.api`；`com.engine.doc.web.*Action` **不被扫描**，靠 `com/api/doc/**/web/` 下的**同名空壳子类**（带类级 `@Path`）桥接 | `d:\Weaver2020\ecology\WEB-INF\web.xml:221-232`；`com/api/doc/console/category/web/DocSecCategoryAction.java:1-7` |
| 2 | `CheckPermission` 是「文档/目录权限判定」 | **错误**。它只有 2 个常量（`DocSecCategoryAdd:Add` / `DocSecCategoryEdit:Edit`），只判**目录后台维护权限**，无 docid 入参。文档级权限在 `DocViewPermission` | `com/engine/doc/util/CheckPermission.java:22-23`；`com/api/doc/detail/service/DocViewPermission.java:48-56` |
| 3 | 版本表 `docversion` | **该物理表不存在**（`(into\|from\|update) docversion` 全仓 0 命中）。版本实际存于 `DocImageFile.versionid`、`DocDetail.doceditionid/docedition`、`wps_doccenter_fileinfo.versionid` | `weaver/docs/docs/DocImageManager.java:523/543/558`；`DocDetailService.java:1714/1770` |
| 4 | 通用文档发布写 `docfrontpage` | **错误**。`DocMonitorManager.publishDoc` 全文无 `docfrontpage`（只 `update DocDetail set docstatus=2...`）。`docfrontpage` 是**新闻/主页 frontpage** 链路 | `weaver/system/systemmonitor/docs/DocMonitorManager.java:289-305`；`weaver/docs/news/DocNewsManager.java:804/806` |
| 5 | 文档详情源码在 `src4js/pc/document`（Redux 版） | 该目录是**旧 Redux 版且快照不完整**（`actions/`、`reducers/` 目录缺失，import 悬空）。**当前生效的是 MobX 版** `src4js/pc4mobx/documentDetail` | `pc4mobx/documentDetail/components/Detail.js`(70.68KB)、`DetailForm.js`(40.71KB) |
| 6 | 在线编辑含 OnlyOffice（或插件可配） | **确认没有 OnlyOffice**（全仓 `onlyoffice` 0 命中，`DocPlugUtil` 枚举中亦无）。实际 4 类载体：WPS(3 链路)、永中 YoZo、金格 iWebOffice(OCX)、OFD(数科/福昕) | `com/engine/doc/util/DocPlugUtil.java:22-43` |

---

## 1. 总体架构

### 1.1 三大前端

| 维度 | 内容引擎后台（目标页面） | 内容前台 | 文档详情 / 编辑 / 发布 |
|---|---|---|---|
| 源码目录 | `src4js/pc4backstage/document` | `src4js/pc4mobx/document` | `src4js/pc4mobx/documentDetail` |
| 路由前缀 | `#/docengine/*` | `#/document/*` | `#/main/document/*` |
| 模块全局名 | `weaDocEngine` | `f_document` | `weaDocDetail` |
| 编译产物 | `spa/document/engine.js` | `spa/document/index_mobx.js` | `spa/document/index.jsp` 等 |
| 职责 | 目录 / 模板 / 权限 / 回收站 / 批量维护 / 新闻期刊 / 日志 / 插件 | 目录选择器、检索、共享、订阅、移动、网盘、知识、监控、统计 | 详情 Tab、新建编辑表单、附件查看、在线编辑、发布 |

### 1.2 两大后端包

| 包 | 职责 |
|---|---|
| `com/engine/doc` | 后台能力：目录、模板、权限、回收站、期刊、维护设置、插件、审批流设置、加密、水印、虚拟目录、网盘 |
| `com/api/doc` | 文档详情、创建保存、检索、共享、在线编辑（WPS/YoZo）、评论、阅读点赞、评分、日志、移动端 |

### 1.3 分层架构

```
前端 SPA（engine.html / index.jsp，hash 路由，React + MobX）
   │  WeaTools.callApi（fetch 封装）
   ▼
REST 入口层   com/api/doc/**/web/*Action（JAX-RS @GET/@POST + 类级 @Path）
   │          ↑ 继承 com/engine/doc/web/*Action（只写方法级 @Path）
   ▼
服务层        *ServiceImpl（extends com.engine.core.impl.Service）
   │  commandExecutor.execute(new XxxCmd(params, user))
   ▼
命令层        com/engine/doc/cmd/**/*Cmd（implements Command）
   ▼
数据访问      weaver.conn.RecordSet 原生 SQL + com.weaver.procedure.* 存储过程
   ▼
数据表        DocDetail / DocSecCategory / DocImageFile / DocShare / shareinnerdoc …
```

---

## 2. 前端架构

### 2.1 技术栈

React 16 + **MobX 3.1.16** + react-router（**HashHistory**）+ antd + 泛微自建组件库 **ecCom**。请求统一走 **`WeaTools.callApi(url, method, params)`**（底层 fetch，非 axios）。

### 2.2 外壳与路由聚合

- 外壳：`D:\Weaver2020\ecology\wui\engine.html`，容器 `<div id="container">`，最后加载 `/spa/main/engine.js`。
- 聚合源码：`src4js/pc4backstage/amain/index.js`
  - `libs` 数组（L23-60）列出模块全局名：L30 `'weaDocEngine' // 文档引擎`、L46 `'weaOdocEngine' // 公文`
  - `getModules(names)`（L62-76）遍历 `window[name]` 合并 `.store` 与 `.Route`：
    ```js
    const module = window[name];
    if (module && module.store && module.Route) {
      store = { ...store, ...module.store };
      route.push(module.Route);
    }
    ```
  - L9-17：`createHashHistory` + `RouterStore`（`mobx-react-router`）
- **`wui/` 目录不含 docengine 业务代码**；真实源码在 `src4js/pc4backstage/document/`，产物在 `spa/document/`。

### 2.3 docengine 后台模块组成

`src4js/pc4backstage/document/`：`index.js`（路由总表）、`apis/`（8 个）、`components/`（17 个一级组件）、`stores/`（26 个 MobX store）、`util/`、`style/`、`public/`。

| 组件 | 职责 |
|---|---|
| `Home.js` | 路由容器 |
| `docCategory/` | **文档目录管理（核心页面，9 个 Tab）** |
| `docEngineIndex/` | 内容引擎首页 / 概览 |
| `docViewMould/` | 显示模板（列表/编辑/单页/书签/另存为/导入） |
| `docEditMould/` | 编辑模板（含 `DocContractMould.js` 合同模板） |
| `batchMaintenance/` | 批量维护（基本信息/权限/编号/版本/模板） |
| `dummyCategory/` | 虚拟目录 |
| `CategoryTemp.js` | 目录模板 |
| `ApplySetting.js` + `applySetting/` | 应用设置（含 `NetdiskSetting.js` 云盘） |
| `FieldManagement.js` | 字段管理 |
| `external/` | 对外暴露组件（`DocAppSetting`/`CreateDocAppSetting`/`SystemDocSetting`） |
| `integration/plugManager` | 第三方插件管理 |
| `log/` | `downloadLog` / `readLog` / `editLog` |
| `magazine/` | 期刊 + 预览 |
| `multi/` | `Upload` / `Share` / `PopupSet` / `Recycle` |
| `news/` | `Type` / `Info` / `Pic` |

### 2.4 docengine 路由树

来源 `src4js/pc4backstage/document/index.js:49-104`，顶层 `<Route path='docengine' component={Home}>`。

| 路由 | 组件 | 功能 |
|---|---|---|
| `index` | `DocEngineIndex` | 概览（目录/模板/期刊/新闻统计 + 图表） |
| `sec/doccategory` | `DocCategory` | 目录 / 分类管理 |
| `sec/applysetting` | `ApplySetting` | 应用设置（回收站/水印/下载限制） |
| `sec/categorytemp` | `CategoryTemp` | 目录模板 |
| `sec/dummycategory` | `DummyCategory` | 虚拟目录 |
| `sec/batchMaintenance/{base,auth,codeRule,edition,template}` | `BatchBaseInfo`/`BatchAuthority`/`BatchCodeRule`/`BatchEdition`/`BatchTemplate` | 批量维护 |
| `sec/fieldManagement` | `FieldManagement` | 字段管理 |
| `sec/netdiskSet` | `NetdiskSetting` | 云盘空间 |
| `mould/view` `mould/edit` `mould/contract`（+ `/:id`） | `DocViewMould`/`DocEditMould`/`DocContractMould` | 显示/编辑/合同模板 |
| `news/type` `news/info` `news/pic` | `NewsType`/`NewsInfo`/`NewsPic` | 新闻 |
| `magazine/index` `magazine/preview/:id` | `Magazine`/`MagazinePreview` | 期刊 |
| `multi/upload` `multi/share` `multi/popupset` `multi/recycle` | `Upload`/`Share`/`PopupSet`/`Recycle` | 批量上传/共享/弹窗/回收站 |
| `log/download` `log/read` `log/edit` | `DownloadLog`/`ReadLog`/`EditLog` | 内容日志 |
| `external` | `External` | 外部调用示例 |
| `integration/plugmanager` | `PulgManager` | 第三方插件（在线编辑集成点） |

### 2.5 目录管理页 `docCategory` 的 Tab → 组件 → 接口

Tab 定义：`util/DocCategoryConstant.js:7-55`；切换：`index.js:180-194` `getCom()`。

| Tab（labelId） | 组件 | 后端接口 |
|---|---|---|
| 0 基本信息(1361) | `basicInfo/index.js` | `/api/doc/console/category/info/base`、`/info/remind/email`、`/info/remind/message`、`/info/getsendimg`、`/info/savesendimg` |
| 1 下级目录(33112) | `SubDirectory.js` | `/api/doc/console/category/table`（`{parentid, subCompanyId}`） |
| 2 权限管理(19174) | `authority/index.js` | `/category/defaultRight`、`/defaultRight/delete`、`/defaultRight/syncHistory`、`/category/right`、`/category/right/delete` |
| 3 版本管理(19450) | `Edtion.js` | `/api/doc/console/category/info/edition` |
| 4 文档编号(19542) | `codeRule/index.js` | `/api/doc/console/category/info/codeRule` |
| 5 模板设置(16367) | `Template.js` | `/info/template`、`/info/changeTmpl`、`/info/saveAsTmpl` |
| 6 文档属性(19451) | `Properties.js` | `/info/properties`、`/properties/add`、`/properties/delete` |
| 7 自定义列表(20237) | `CustomList.js` | `/info/custom` |
| 8 文档审批(384897) | `approve/index.js` + `ApproveDetail.js` | `/info/approve`、`/info/approve/detail` |

非 Tab 常挂载：`NewCategory.js`（`/category/add`）、`excelImport/`（`/import/history`、`/import/result`）、树/列表/删除/导出（`/tree`、`/table`、`/baseInfo`、`/isasync`、`/del`、`/export`）。

### 2.6 内容前台（`#/document/*`）

路由表见 `src4js/pc4mobx/document/index.js`（`path="document"`）：`add`、`center`、`count`、`catalog`、`docofauthor`、`docofdepartment`、`docofsubdepartment`、`monitor`、`dummy`、`search`、`move`、`share`、`docRecycle`、`subscribe`、`news`、`more`、`knowledgeEdit`、`knowledgeList`、`KnowledgeMapList`、`netdisk`。

- **`Add.js` 不是表单，是「目录选择器」**：左树 + 面包屑 + 目录卡片网格；接口 `/api/doc/category/treeNode`、`/api/doc/save/getEditSet`、`/api/doc/save/getParams`、`/api/doc/category/getAccount`。
- **真正的新建表单**在 `pc4mobx/documentDetail/components/DetailForm.js`（40.71KB），路由 `#/main/document/edit`。
- **`Search.js`（45.85KB）**：条件区（`WeaForm` + `WeaSearchGroup`）+ 结果区（`WeaTable`）+ 检索条件模板 Tab（`/api/doc/searchlist/tabInfo`）；按钮「搜索/保存/另存为/重置/取消」。

### 2.7 文档详情（`#/main/document/*`）

路由（`pc4mobx/documentDetail/index.js`）：`detail`→`Detail.js`、`outDetail`→`OutDetail.js`（登录前）、`fileView`→`FileView.js`、`edit`→`DetailForm.js`。

Tab 由后端下发：`GET /api/doc/detail/tabInfo`；key 全集（后端 `DocDetailService.java:78-87`）：`content / param / share / acc / version / reply / log / score / ref / children`。

| Tab | 接口 |
|---|---|
| `content` 正文 | `/api/doc/detail/basicInfo`、`/api/doc/detail/htmlContent` |
| `acc` 附件 | `/api/doc/acc/docAcc`、`/api/doc/acc/docAccVersion`、`/api/doc/acc/docAccReplace` |
| `version` 版本 | `/api/doc/detail/docVersion` |
| `share` 共享 | `/api/doc/share/list`、`/canEdit`、`/addShare`、`/saveShare`、`/deleteShare`；确认 `/api/doc/save/afterSave` |
| `param` 属性 | `/api/doc/detail/docParamInfo` |
| `score` 打分 | `/api/doc/score/docScore`、`/doMarkDoc`、`/docMarkDetail` |
| `reply` 回复 | `/api/doc/reply/replyCount`、`/replyList`、`/saveReply`、`/deleteReply` |
| `log` 日志 | `/api/doc/log/list` |
| — 阅读/点赞 | `/api/doc/read/addReadLog`、`/readCount`、`/api/doc/praise/*` |
| — 其他 | `/api/doc/operate/delete`、`/api/doc/detail/invalidate`、`/setTop`、`/reOpen`、`/reLoad`、`POST /api/doc/save/submit` |

### 2.8 三套 SPA 的联动

**路由串由后端常量定义**（`com/api/doc/search/util/DocLink.java`）：

```java
DOC_DETAIL_LINK = "/spa/document/index.jsp";      DOC_DETAIL_ROUT = "#/main/document/detail";
DOC_EDIT_LINK   = DOC_DETAIL_LINK;                DOC_EDIT_ROUT   = "#/main/document/edit";
ACC_DETAIL_LINK = "/spa/document/index2file.jsp"; ACC_DETAIL_ROUT = "#/main/document/fileView";
DOC_MOULD_LINK  = ".../spa/document/static4engine/engine.html"; DOC_MOULD_ROUT = "#/docengine/mould/view/";
DOC_ROOT_FLAG = "router"; DOC_ROOT_FLAG_VALUE = "&router=1";
```

**新建完整链路**：

```
#/document/add（选目录，读 /api/doc/category/treeNode，后端回 {linkUrl, linkRout, domKey, params, openNewWindow}）
  CategoryService.java:24-28  LINK_URL="/spa/document/index.jsp" LINK_ROUT="#/main/document/edit" DOM_KEY="secid" PARAMS="&isEdit=1"
  → AddContent.js:239-245 拼 url = `${linkUrl}?${domKey}=${key[0]}${params}&f_weaver_belongto_userid=..${linkRout}`
  → /spa/document/index.jsp?secid=<id>&isEdit=1&router=1#/main/document/edit → DetailForm.js
  → POST /api/doc/save/save
  → DetailFormStore.js:300  data.status===1 && data.link → window.location.href = data.link
  → /spa/document/index.jsp?id=<docid>&router=1#/main/document/detail
```

**后台 → 前台/详情跳转证据**：`news/Info.js:257` `window.open('/spa/document/static/index.html#/main/document/news?...')`；`magazine/Preview.js:49/67` `<a href="/spa/document/index.jsp?id=${item.id}" target="_blank">`。

**后台反向复用前台 store**（代码级耦合）：`stores/multi/ShareOperationStore.js:1` → `../../../../pc4mobx/document/stores/tables/docShare`；`RecycleStore.js:10` → `pc4mobx/document/stores/DocRecycle`；等 4 处。

### 2.9 两个入口的区别

| | `wui/engine.html` | `spa/document/static4engine/engine.html` |
|---|---|---|
| 定位 | **全量后台引擎总壳**（所有引擎共用） | **文档模块单页壳**（只挂 docengine/docDetail） |
| 末尾 JS | `/spa/main/engine.js`（amain） | `static4engine/engine.js` + `engine4single.js` |
| 依赖 | 门户、主题、zDialog、登录等全量 | 仅 react/mobx/ecCom |
| 典型路由 | `#/docengine/**` 全部后台页 | `#/docengine/mould/view/:id`、`#/docengine/magazine/preview/:id` |
| 何时用 | 管理员在后台菜单操作（需左侧导航/分权树/主题/SSO） | 独立/新窗口打开单个页面、外部系统嵌入 |

---

## 3. 后端架构（含 REST 映射机制·已查实）

### 3.1 REST 网关注册点（决定性证据）

`d:\Weaver2020\ecology\WEB-INF\web.xml:221-232`：

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

**结论**：
- 引擎是 **Jersey 1.x**（非 RestEasy）；全系统仅此一个 REST servlet。
- 扫描范围仅 `com.cloudstore` 与 `com.api`；**`com.engine.*` 不被扫描**。
- URL 公式：`ContextPath + "/api" + 类级@Path + 方法级@Path`。

### 3.2 桥接机制：同名空壳子类

`com/engine/doc` 全目录 grep `^@Path\(` → **0 命中**（只有方法级 `@Path`）；`com/api/doc` → 76+ 命中（全是类级 `@Path`）。

桥接方式（`com/api/doc/console/category/web/DocSecCategoryAction.java:1-7`）：

```java
package com.api.doc.console.category.web;
import javax.ws.rs.Path;

@Path("doc/console/category")
public class DocSecCategoryAction extends com.engine.doc.web.DocSecCategoryAction {
}
```

**因此：`/api/engine/doc/...` 不存在**，真实 URL 是 `/api/doc/console/category/tree`。

### 3.3 console 后台壳映射（24 个，均 extends `com.engine.doc.web.*`）

| 壳类（`com/api/doc/console/**/web/`） | 类级 @Path | 最终前缀 |
|---|---|---|
| DocSecCategoryAction | `doc/console/category` | `/api/doc/console/category` |
| DocSecCategoryInfoAction | `doc/console/category/info` | `/api/doc/console/category/info` |
| DocCustomFieldAction | `doc/console/custom` | `/api/doc/console/custom` |
| DocLogAction | `doc/console/log` | `/api/doc/console/log` |
| DocNewsAction / DocMagazineAction | `doc/console/news` / `doc/console/magazine` | … |
| DocMouldAction / DocMouldFileAction / ImportMouldAction | `doc/console/mould` / `mouldfile` / `importmould` | … |
| SecMouldAction | `/doc/console/categoryMould` | `/api/doc/console/categoryMould` |
| RecycleConfigurationAction | `/doc/console/recycle` | `/api/doc/console/recycle` |
| NetDiskSettingAction | `doc/console/netdiskset` | `/api/doc/console/netdiskset` |
| DocMobileSettingAction | `doc/console/mobilesetting` | … |
| DocVirtualCategoryAction | `doc/console/virtual` | `/api/doc/console/virtual` |
| DocWaterMarkSettingAction | `doc/console/wmsystemsetting` | … |
| DocPlugManagerAction | `doc/console/plugmanager` | `/api/doc/console/plugmanager` |
| DocApplySettingAction / DocTabInfoRoleSaveAction | `doc/console/docapplysetting` / `doc/console/applysetting` | … |
| DocMultiOperaAction / DocMultiMaintainAction | `doc/console/multi` / `doc/console/multi/maintain` | … |
| UpdateHisDocShareTimeAction | `/doc/console/application` | … |
| DocumentEncryptFieldSetAction / DocumentEncryptShareSetAction / DocEncryptSecondAuthSettingAction | `/doc/encrypt/fieldset` / `/doc/encrypt/shareset` / `/doc/encrypt/secondAuth` | … |

其余：`com/api/doc/news/DocNewsWebAction.java`(`@Path("doc/news")`)、`com/api/doc/magazine/DocMagazineWebAction.java`(`@Path("doc/magazine")`)、`com/api/doc/relate/DocRelateWebAction.java`(`@Path("/doc/relate")`)。`com/engine/doc/web/SecCategoryAddAction.java` 是空类。

### 3.4 `com/api/doc/**` 主要前缀

| 类 | 类级 @Path | 前缀 |
|---|---|---|
| `DocDetailAction` | `/doc/detail` | `/api/doc/detail` |
| `DocSaveAction` | `/doc/save` | `/api/doc/save` |
| `DocAcction` | `/doc/acc` | `/api/doc/acc` |
| `DocReplyAction` / `DocReadAction` / `DocPraiseAction` / `DocScoreAction` | `/doc/reply` `/doc/read` `/doc/praise` `/doc/score` | … |
| `DocCoopeEditAction` | `/doc/coope` | `/api/doc/coope` |
| `SensitiveWordAction` | `/doc/sensitiveword` | … |
| `DocOutDetailAction` | `/doc/out/detail` | … |
| `DocMonitoringAction` | `/doc/monitor` | `/api/doc/monitor` |
| `DocRecycleAction` | `/doc/recycle` | … |
| `DocSearchAction` / `DocShareAction` | `/doc/docsearch` / `/doc/share` | … |
| `FileUploadAction` | `/doc/upload` | … |
| `WaterMarkAction` | `/doc/watermark` | … |

**运行时 URL 反证**：`com/engine/sensitive/biz/CheckSensitiveFilter.java:23-26` 白名单含 `/api/doc/save/isExists`、`/api/doc/acc/loadFromWps`、`/api/doc/acc/loadFromYoZo`。

### 3.5 包结构与命令模式

| 子包 | 职责 |
|---|---|
| `com/engine/doc/web/` | 对外 REST 入口（约 30 个 `*Action`，只写方法级 `@Path`） |
| `com/engine/doc/cmd/` | 命令模式业务单元，按功能分子包 |
| `com/engine/doc/service/`+`impl/` | 服务层（`extends com.engine.core.impl.Service`，持 `commandExecutor`） |
| `com/engine/doc/entity/`+`bean/` | 引擎自有实体 / Bean |
| `com/engine/doc/util/` | `CheckPermission`、`DocConstant`、`DocPlugUtil`、`IWebOfficeConf`、`WaterMarkUtil`、`RightUtil`、`SqlUtil`、`DocEncryptUtil` |
| `com/engine/doc/dao/docPlugin/` | 插件 DAO |

`cmd/` 功能域：`secCategoryList`（目录树/列表/权限/导入导出）、`secCategoryInfo`（基础/属性/模板/审批）、`docMould`、`docMouldFile`、`SecMould`、`customField`、`docApplySetting`、`doctabinfo`、`magazine`、`news`、`newsWeb`、`multi`（批量/回收站/打开）、`maintaince`、`mobileSetting`、`multi`、`NetdiskSetting`、`virtualFolder`、`WaterMarkSetting`、`encryptFieldSet`、`encryptSecondAuthSet`、`encryptShareSet`、`DocPlugManager`、`log`、`relate`、`systembill`、`transfer`、`updateHisDocShareTime`。

**Cmd 不由前端 `cmd=xxx` 直调**（搜索 `CommandAction|registerCommand` 为 0），由 Service 内 `commandExecutor.execute(new XxxCmd(params, user))` 调度。

### 3.6 典型调用链

```
GET /api/doc/console/category/tree
 → com/api/doc/console/category/web/DocSecCategoryAction（@Path("doc/console/category")）
   → extends com/engine/doc/web/DocSecCategoryAction.getDocSecCategoryTree()  (:56，@Path("/tree"))
   → DocSecCategoryServiceImpl.getDocSecCategoryTree(map)  (:16)
   → commandExecutor.execute(new DocSecCategoryTreeCmd(params, user))
   → DocSecCategoryTreeCmd.execute()  (:41) → MultiAclManager.getPermittedTree + MultiCategoryTree
   → 异步分支 getAsyncTreeDatas(:204) → RecordSet "select ... from docseccategory where parentid=?" (:208)
```

---

## 4. 文档生命周期

### 4.1 保存 `doSave`

入口 `POST /api/doc/save/save` → `DocSaveAction.java:60-95` → `DocSaveService.doSave(user, paramMap, clientip, request)`（`:1364`）。

**入参**：`docid`/`id`、`seccategory`、`operate`（`"draft"` 存草稿）、`isflowdoc`、`requestid`、`fileids`、`mainimagefile`、`doccontent`、`deleteaccessory`、`isOffice`、`saveBJCA`、`off_docid`、`off_imagefileid`、`f_weaver_belongto_userid`、`clientip`。

**写表流程**：

| 步骤 | 行号 | 动作 / 表 |
|---|---|---|
| 权限预检 | 1370-1379 | `DocShareUtil.docRightFilter(user,docid)`；无权限 → `status=-1` |
| 目录校验 | 1388-1392 | 新建且 `seccategory<=0` → 报错「目录不能为空!」 |
| 身份切换 | 1394-1405 | `f_weaver_belongto_userid` → `new User(...)` |
| 版本分裂 | 1509-1538 | `DocIdUpdate.getDocNewId()`；旧版本 `docstatus=7, ishistory=1` |
| 附件 | 1583/1652/1660/1699 | `DocImagefile` / `docimagefile` / `imagefiletemp` / `ImageFile` |
| 状态计算 | 1756-1813 | 见下表 |
| 正文 | 1964-1967 | `insert/update DocDetailContent(doccontent,docid)` |
| 主表 | 1982-1988 | `insert/update DocDetail` |
| 自定义字段 | 2069/2073 | `cus_fielddata` |
| 虚拟目录 | 2094/2100 | `DocDummyDetail(catelogid,docid,importdate,importtime)` |
| 主日志 | 2132-2152 | `DocDetailLog` `operateType="1"`(新增)/`"2"`(修改) |
| 触发审批 | 2254-2265 | `docStatus==3` → `approveWorkflow(docid, oldstatus, "1", user)` |
| 推送 | 2258-2259 | `changeSend(docid)` + `senMessage(...)` |
| 公文同步 | 2262-2265 | `isflowdoc=1` → `OdocRequestdocUtil.changeStatusByWorkflowId` |
| **全文索引** | 2267 | `SearchUpdateUtil.updateIndexLog(SearchUpdateType.DOC, docid)` |

**状态计算（:1756-1813）**：

```
operate=="draft"               → docStatus="-1"
isflowdoc=="1" && requestid>0  → getDocStatusByRequestid(requestid)   // 9 或 1
isflowdoc=="1" && requestid<=0 → "9"
scc.needApprove(secid)         → blnOsp ? "-3" : "3"
scc.needPubOperation(secid)    → blnOsp ? "-6" : "6"
否则                            → blnOsp ? "-1" : "1"（并写 DOCEDITIONID/DOCEDITION）
```

### 4.2 提交 `doSubmit`（`:2307`）

`POST /api/doc/save/submit` → `DocSaveAction.java:122-151`。
校验编辑权（`:2329-2340`）→ 读状态（`:2342-2373`）→ 重算状态（`:2376-2389`，规则同上）→ `update DocDetail set docstatus=?`（`:2391-2419`）→ `docStatus==3` 时 `approveWorkflow(...)`（`:2463-2465`）→ `changeSend` + `senMessage` → 返回 `link = DocSptm.DOC_DETAIL_LINK + "?id=" + docid + param + DOC_DETAIL_ROUT`。

> 定时发布分支被 `if (false && ...)` 短路 → **死代码，不执行**（`:2420-2438`）。

### 4.3 审批 `approveWorkflow`（`:2485`）

`approveType`：1-生效，2-失效（注释 `:2482`）。

1. `:2488` 读 `select c.approveWorkflowId, c.isOpenApproveWf, d.docsubject, d.docstatus, c.id secid from DocSecCategory c, DocDetail d where d.id=? and d.seccategory=c.id`
2. `isOpenApproveWf=="1"`（审批工作流，`:2512-2541`）：`DocApproveWfManager` → `approveWf(docid, approveType, user)` 或 `updateApprovedWf`；失败 → `docStatus=0`（生效）或 `2`（失效）
3. `isOpenApproveWf=="2"` 且 `workflowId>0`（批准工作流，`:2542-2608`）：
   - `WorkflowVersion.getActiveVersionWFID`（`:2543`）
   - 构造 `RequestInfo`（`:2547-2563`）
   - **`:2575` `requestid1 = new RequestService().createRequest(ri)`** ← 流程实例在此创建
   - `:2582` `update bill_Approve set approveid=<docid>, docid=<docid>, approvetype=9, gopage='/docs/docs/DocApprove.jsp?id=', status=0 where REQUESTID=?`
   - `:2584` `update workflow_requestbase set docids='<docid>' where REQUESTID=?`
   - `:2586-2600` 从 `workflow_currentoperator` 取操作者 → `DocShareUtil.addDocShare(...)`
4. 收尾 `:2610-2614` `update DocDetail set docStatus=3, approveType=?`

### 4.4 `afterSave`（`:2862`）

`GET /api/doc/save/afterSave` → `DocSaveAction.java:157-190`。
只做三件事：**状态重算**（`:2897-2910`）+ **触发审批**（`:2967-2969`）+ **推送**（`:2971-2973`）。
**明确不做**：权限同步、附件处理、索引更新（索引只在 `doSave:2267`）。定时发布分支同样是 `if(false && ...)` 死代码（`:2914`）。

### 4.5 文档状态机

权威定义（注释级，无 Java 枚举）——`weaver/docs/docs/DocManager.java:176`：

```
/* 文档状态:0:草稿,1:生效/正常(不需要审批，归档重新打开),2:生效/正常(审批后,发布后),
   3:审批,4:退回(草稿),5:归档,6:待发布,7:失效,8:作废 */
```

| 值 | 含义 | 写入点 |
|---|---|---|
| 0 | 草稿 | `DocManager.java:4017`；`approveWorkflow:2535/2604`（审批失败回退）；`SystemBill28AfterAction.java:219` |
| 1 | 生效（不需审批 / 归档重开） | `doSave:1784`、`doSubmit:2388`、`afterSave:2904`、`getDocStatusByRequestid:3965` |
| 2 | 生效（审批后 / 发布后） | `DocMonitorManager.java:260,291`；`approveWorkflow:2538` |
| 3 | 审批中 | `doSave:1780`、`doSubmit:2384`、`approveWorkflow:2611` |
| 4 | 退回（草稿） | 注释定义；`doSave:1486` |
| 5 | 归档 | `DocManager.ArchiveDocInfo():3341` |
| 6 | 待发布 | `doSave:1782`、`doSubmit:2386`、`afterSave:2902` |
| 7 | 失效 | `DocManager.java:4046`；旧版本作废 `doSave:1523-1537` |
| 8 | 作废 | 注释定义（未定位到写入点） |
| **9** | **流程草稿**（公文/流程建稿） | `DocSaveService.java:1770`、`:3955-3969`；`OdocSignByKG.java:57` |
| **-1 / -3 / -6** | 弹出默认共享(blnOsp)未确认中间态，对应 1/3/6 | `doSave:1764-1784` |

```
新建草稿 → -1(blnOsp)/0
   ↓ doSubmit
提交 → needApprove→3 | needPubOperation→6 | 否则→1
   ↓ approveWorkflow（=1 审批流 / =2 批准流 createRequest）
审批中 3（bill_Approve.approvetype=9, workflow_requestbase.docids）
   ↓ 通过
生效 2；待发布 6 --publishDoc--> 生效 2
生效 1/2 --invalidate--> 失效 7；--archiveDoc--> 归档 5
版本升级：旧版本 → docstatus=7, ishistory=1
```

### 4.6 发布 / 失效 / 置顶

**发布**：`GET /api/doc/detail/publishDoc` → `com/api/doc/detail/web/DocDetailAction.java:402-444`（类级 `@Path("/doc/detail")` :41）。
- `isPublic=="1"` 且 `publicData > 今天` → `update docdetail set scheduledreleasedate=?`（定时发布）
- 否则 `DocDetailService.publishDoc(docid, ipAddress, user)`（`:2888-2901`）→ `DocMonitorManager.executeDocMonitor(docid, "publishDoc", user)`（`:2895`）
- **执行体** `weaver/system/systemmonitor/docs/DocMonitorManager.java:207-321`：
  - `:258` **仅 `docstatus==6`（待发布）才允许发布**
  - `:260` → `:289-305` `UPDATE DocDetail SET docstatus=2, docEdition, docEditionId, selectedpubmouldid, docpubuserid, docPubUserType, docpubdate, docpubtime, docvaliduserid, docValidUserType, docvaliddate, docvalidtime, scheduledreleasedate='' WHERE ID=?`
  - `:307-316` `DocDetailLog` `operateType="13"`（发布）
  - `:318-319` `senMessage(docid, "2", user)`

> **`docfrontpage` 不被通用发布写入**。写它的是新闻/主页 frontpage 链路：`weaver/docs/news/DocNewsManager.java:804/806/851/853` → `DocFrontpage_Insert_New`/`Insert`/`Update_New`/`Update`（`com/weaver/procedure/docfrontpage/*`）。

**失效**：`GET /api/doc/detail/invalidate` → `DocDetailAction.java:367-395`（先 `DocDetailUtil.getPermission(...).get("canInvalidate")` 校验）→ `DocDetailService.invalidate(:2695)`。

**置顶**：`GET /api/doc/detail/setTop` → `DocDetailAction.java:336-361` → `DocDetailService.java:2643-2686`；`update docdetail set istop=1, topdate=?, toptime=?, topstartdate=?, topenddate=?`（:2658）。

**监控台批量**：`GET /api/doc/monitor/doOperate`（`DocMonitoringAction.java:83-86`）→ 按 `operation` 分派 `publishDoc`/`archiveDoc`/`invalidDoc`/`cancelDoc`/`reopenFromArchiveDoc`/`reopenFromCancellationDoc`/`checkInCompellablyDoc`/`deleteDoc`。

### 4.7 由流程创建文档

`com/engine/odoc/entity/createDoc/WorkflowCreateDoc.java:15-18` `@Table("workflow_createdoc")` —— **按流程的配置表**：`flowcodefield`(文号字段)、`flowdocfield`(正文字段)、`flowdoccatfield`(存放目录字段)、`defaultview`、`printnodes`、`newtextnodes`、**`isworkflowdraft`**、`defaultdoctype`、`odoctype` 等。

`DocSaveService.getDocStatusByRequestid(int requestid)`（`:3955-3969`）：

```java
rs.executeQuery("select isWorkFlowDraft from workflow_createdoc where workflowid=(select workflowid from workflow_requestbase where requestid=?)", requestid);
if (rs.next()) {
    if ("1".equals(isWorkFlowDraft)) docStatus = "9";   // 流程草稿
    else                             docStatus = "1";   // 直接生效
}
```

`com/engine/doc/cmd/systembill/SystemBill28AfterAction.java`（节点后动作）：
- 审批通过回写（`:60-80`）：`select approveid from bill_Approve where requestid=?` → `update DocDetail set docstatus=3`（:67）→ `update bill_Approve set status=1`（:68）→ `dm.approveDocFromWF("approve", docid, ...)`（:77）
- 发起审批（`:95-291`）：`ApproveParameter.setApprovetype("9")`、`:205 setBilltablename("bill_Approve")`、`:291 rm.flowNextNode()`

**流程 ⇄ 文档关联表**：`bill_Approve`（`approveid/docid/approvetype=9/gopage/status`）、`workflow_requestbase.docids`、`workflow_currentoperator`、`workflow_createdoc`、`DocSecCategory.approveWorkflowId/isOpenApproveWf`。

---

## 5. 权限与共享模型

### 5.1 两套权限体系（关键区分）

| | 目录维护权限 | 文档级权限 |
|---|---|---|
| 类 | `com/engine/doc/util/CheckPermission.java` | `com/api/doc/detail/service/DocViewPermission.java` |
| 常量 | `ADD_RIGHT="DocSecCategoryAdd:Add"`、`EDIT_RIGHT="DocSecCategoryEdit:Edit"`、`DOC_POWSER=ADD_RIGHT` | `DOC_PERMISSION="doc_permission_"`、`READ/EDIT/DELETE/SHARE/LOG/DOWNLOAD/CONTROL` |
| 入参 | User + 目录id/父目录id/分部id/虚拟目录id（**无 docid**） | `getShareLevel(int docid, User, boolean useNew)` → `Map<String,Boolean>` |
| 说明 | 只判「能否建/改目录」 | 新权限开关 `useNew=="1"`；旧路径 `SpopForDoc.getDocOpratePopedom` |

`CheckPermission` 主要方法：`checkEditPermissionByParentId`（L43-86，`HrmUserVarify.checkUserRight(ADD_RIGHT)` 或 `MultiAclManager.hasPermission(..., OPERATION_CREATEDIR)`）、`hasDummyPermissionFromParent`（L113-155）、`checkRecycleConfigPermission`（L31-34，**硬编码 return true**）。

### 5.2 目录 ACL：`DirAccessControlList` + `MultiAclManager`

`weaver/docs/category/security/MultiAclManager.java`：

- **操作码（只有 5 种，不含查看/编辑/删除）** L27-37：
  `OPERATION_CREATEDOC=0`（创建文档）、`CREATEDIR=1`（目录管理）、`MOVEDOC=2`（移动）、`COPYDOC=3`（复制）、`TREEFIELDDIR=99`（虚拟目录管理员）
- **对象类型 `permissiontype`** L39-47：`1`部门+安全级别、`2`角色+角色级别、`3`安全级别、`4`用户类型、`5`人力资源、`6`分部、`8`岗位（由 `DocRightAddCmd` 的 `permissiontype=7` 触发 `grantDirPermission8`）
- **授权**：`grantDirPermission1/6/2/3/4/5/8` → 对父目录执行 `Doc_DirAcl_Insert_TypeN` → `insert into DirAccessControlList(...)` → `DocTriggerUtils.Tri_I_DirAccessControlList` 展开明细
- **收权**：`depriveDirPermission`（L499-511）走 `Doc_DirAcl_Delete`
- **判定**：`hasPermission(...)`（L575-602）
  - `operationcode != OPERATION_CREATEDIR` → `Doc_DirAcl_CheckPermission`（**非继承**）
  - `operationcode == OPERATION_CREATEDIR` → `Doc_DirAcl_CheckPermissionEx2`（**继承性，检查各上级目录**）
- 展开表：`DirAccessControlDetail`（`DocTriggerUtils.java:711-774` INSERT；`:1008-1011` DELETE）

### 5.3 文档共享三张表（关系明确）

| 表 | 定位 | 关键证据 |
|---|---|---|
| `DocSecCategoryShare` | **目录默认共享（模板）**：`seccategoryid+sharetype+对象id+seclevel+sharelevel+downloadlevel+operategroup+orgid+includesub` | `com/weaver/procedure/docseccategoryshare/Docseccategoryshare_ins_g.java`（23 字段 insert）；`DocDefaultRightAddCmd` L135/177/204/232/266 |
| `DocShare` | **文档共享主表**：一行 = 一条规则；`isSecDefaultShare='1'` 表示继承自目录默认共享，`sharesource` 记来源 | `Docshare_fromdocseccategoryi`；`DocSaveService:2599` |
| `DocShareDetail` | **历史遗留明细**（`docid,userid,usertype,sharelevel`）。**新版内容引擎不写此表**（全库仅 15 处引用，集中在旧版存储过程） | `Docsharedetail_insert` |
| `shareinnerdoc` / `shareouterdoc` | **物化权限表**（查询侧真正被 `ShareManager` 使用） | `DocViewer.setDocShareByDoc`；`DocShareUtil.getOnlyHasRight`（L60-124） |

**sharetype（对象类型）**：`1`人员 `2`分部 `3`部门 `4`角色 `5`所有人 `6`群组 `7`客户 `8`客户类型 `10`岗位；以及 `80`创建人本人、`81`直接上级、`82`所有上级、`84`本分部、`85`本部门。
**sharelevel（权限级别）**：`1`查看 `2`编辑 `3`完全控制（`DocShareService.java` `SEC_LEVEL_VIEW/EDIT/ALL`）。
安全级别默认 `seclevel=10`、`seclevelmax=100`。

> **两套位不一致**：目录 ACL 的 `permissiontype`（1/2/3/4/5/6/8）与共享的 `sharetype`（1/2/3/4/5/6/7/8/10+80系）是不同编码——「岗位」目录侧是 8、共享侧是 10。

### 5.4 权限继承链（三层）

1. **父目录 → 子目录（维护权限继承）**：校验侧 `OPERATION_CREATEDIR` 走 `Doc_DirAcl_CheckPermissionEx2`（继承）；其余操作码非继承。写入侧先对父目录授权再插子目录并记 `relatedid`。虚拟目录用 `DocTreeDocField.allSuperiorFieldId` 逐级上溯。
2. **目录 → 文档（默认共享继承）**：**不走 `DirAccessControlList`**，走共享三表——`Docshare_fromdocseccategoryi` 把 `DocSecCategoryShare` 复制成 `DocShare`，再由 `DocViewer.setDocShareByDoc` 展开到 `shareinnerdoc`。
3. **文档 → 共享（显式共享）**：`DocShare` + `isSecDefaultShare` 标记区分默认/非默认。

### 5.5 三个关键类的角色

| 类 | 角色 |
|---|---|
| `weaver.docs.category.SecCategoryComInfo` | `DocSecCategory` 的**缓存元数据提供者**（`extends CacheBase`）。提供 `parentid/dirid/dirtype/subcompanyid/nodownload/maxuploadfilesize/replyable/coder/secorder`，是权限继承链的「目录层级/归属」数据源 |
| `weaver.docs.category.security.MultiAclManager` | **目录 ACL 的读/写/判定引擎**（`DirAccessControlList` + `DirAccessControlDetail` + 存储过程）。只管 5 类操作码 |
| `weaver.docs.category.MultiCategoryTree` | **目录树结构封装**，配合 `getPermittedTreeSql` 做 UI 可见性裁剪，不参与判定 |

### 5.6 安全与合规

| 能力 | 包 | 配置存储 |
|---|---|---|
| **字段级加密 / 脱敏 / 二次校验** | `cmd/encryptFieldSet` + `DocumentEncryptFieldSetService` | `enc_field_config_info`、`cus_formdict`；加密数据落 `cus_fielddata_doc_enc`；权限 `EncryptConfigBiz.hasRight` |
| **二次身份认证 / 双人认证** | `cmd/encryptSecondAuthSet` + `DocEncryptSecondAuthSettingService` | `docseccategory.isEnableSecondAuth/isEnableDoubleAuth/authverifier`；全局 `enc_secondauth_config_info`、`enc_mould_config_info` |
| **加密分享** | `cmd/encryptShareSet` + `DocEncryptShareSetService` | `docseccategory.isencryptshare/encryptrange`；`EncryptShareSettingEntity`、`enc_mould_config_info` |
| **水印** | `util/WaterMarkUtil` + `cmd/WaterMarkSetting` | `SystemWaterMarkSetting`（全局：`watermarktype`1文字/2图片、`opacity/rotate/imageid/wmcontent/fontsize/color`）、`docwatermarksecsetting`（目录级：`wmforview/wmfordownload/wmforprint/setpostion`）、`docseccategory.isopenwater`（总开关）。应用场景：阅读/下载/打印（值 1/2/3）。权限点 `DocManageSet:all` |
| **回收站** | `cmd/multi/DocRecycleTableCmd` + `com/api/doc/docRecycle/*` | 数据来自 **`recycle_docdetail`**；配置在 **`DOC_PROP`**（`PROPKEY='docsrecycle'/'docsautoclean'/'autodeletedays'`）；恢复/彻底删除 `DocRecycleManager.recoverDocFromRecycle/deleteDocFromRecycle`；权限点 **`DocumentRecycle:All`** |
| **虚拟目录** | `cmd/virtualFolder` | 表 **`DocTreeDocField`**（`superiorFieldId/allSuperiorFieldId/fieldLevel/isLast/mangerids`）；文档经 **`DocDummyDetail(catelogid,docid)`** 挂载；**独立于 DocSecCategory 的另一棵树**；权限点 `DummyCata:Maint` + `OPERATION_TREEFIELDDIR=99` |
| **网盘** | `cmd/NetdiskSetting` | 表 **`DocNetdiskSizeSet(id,userid,username,disksize)`** |
| **日志** | `cmd/log/DocLogReadTable/DocLogEditTable/DocLogDownloadTable` | 统一写 **`DocDetailLog`**（`docid/docsubject/doccreater/operatetype/operatedesc/operateuserid/operatedate/operatetime/clientaddress/usertype`）；下载日志另写 **`DownloadLog`**。`operatetype`：`0`阅读、`1`新增、`2`修改、`13`发布、`14`版本、`18`签出、`20`签入、`21`打印、`22`下载、`23`共享 |

---

## 6. 在线编辑 / 版本 / 附件 / 转换

### 6.1 在线编辑：4 类载体（确认无 OnlyOffice）

全仓检索 `onlyoffice|only_office|onlyOffice` → **0 命中**；`DocPlugUtil` 插件枚举中亦无。

| 载体 | 包 / 入口 | 形态 |
|---|---|---|
| **WPS 文档中心**（新版） | `com/api/doc/wps/web/DocCenterAction` `@Path("/wps/doccenter/outter")` | 服务端 Web 编辑/协作 |
| **WPS WebOffice 标准协议**（旧版） | `WebOfficeFileAction` `@Path("/v1/3rd/file")`、`WebOfficeUserAction`、`WebOfficeOauthAction` `@Path("/weboffice")` | 旧协议 |
| **WPS 另存为附件** | `DocCenterSaveFileAction` `@Path("/wps/doccenter/inner")` | `/api/wps/doccenter/inner/file/saveToAccFile` |
| **永中 YoZo** | `com/api/doc/yozo/weboffice/YozoWebOfficeAction` `@Path("/doc/yozo")` | 服务端（配置 `doc_yozo_for_weaver`：hostname/client_path/edit_online…） |
| **金格 iWebOffice** | `com/engine/doc/util/IWebOfficeConf` + `cmd/docMould/IwebOfficeCmd` + `weaver/wps/WebOfficeUtil`(59KB) | **IE/OCX 客户端控件**（2003/2006/2009/2015/PDF2018），落点在**模板编辑**与旧版正文/附件 |
| **OFD** | `DocPlugUtil` 枚举 `SHUKEOFD`(数科) / `FUXINOFD`(福昕) | 版式预览 |

**WPS 文档中心接口**（`DocCenterAction`）：

| 方法 | HTTP | URL | Cmd |
|---|---|---|---|
| `fileInfo` | GET | `/api/wps/doccenter/outter/v1/3rd/file/info` | `DocCenterGetFileInfo` |
| `userInfo` | POST | `.../v1/3rd/user/info` | `DocCenterGetUsetInfo` |
| `online` | POST | `.../v1/3rd/file/online` | `DocCenterFileOnline` |
| `save` | POST | `.../v1/3rd/file/save` | `DocCenterFileSave` |
| `fileVersionInfo` | GET | `.../v1/3rd/file/version/{version}` | `DocCenterGetFileVersionInfo` |
| `rename` | PUT | `.../v1/3rd/file/rename` | `DocCenterFileRename` |
| `history` | POST | `.../v1/3rd/file/history` | `DocCenterGetFileHistory` |
| `onnotify` | POST | `.../v1/3rd/onnotify` | `DocCenterReceiveNotify` |
| `doFileCallback` | POST | `.../cps/callback` | `DocCenterDoFileCallback` |
| `doPing` | GET | `.../ping` | `DocCenterDoPing` |

> `online`/`onnotify`/`doFileCallback` 三处入口在函数开头有 `if(Boolean.TRUE){ return {code:0,status:200}; }` **短路**，真实业务已被屏蔽。

**WPS 开放平台回调**（另一条链）：`com/api/doc/call/wps/controller/WpsController` `@Path("/doc/wps/sign")` —— `download`、`sign`（JS-SDK 签名）、`wxConfig`、`file`（`AppSdkUtil.upload`）、`subscriptions/receive`（WPS 云订阅回调）。区别于 `com/api/doc/wps/**`（文档中心私有协议）。

**YoZo 接口**：`/api/doc/yozo/{loadFromYoZo, uploadFromYoZo, openFileFromYozo, saveFileFromYozo, closeFileFromYozo, isOpenFromYozo}`。

### 6.2 选型开关（插件注册与生效）

`com/engine/doc/util/DocPlugUtil.java:22-43` 枚举：操作类型 `readonline`/`editonline`；插件类型 `YONGZOSOFT/BJCAPDFOCX/JINSHANWPS/WPSREADFORWIN/WPSREADFORLINUX/WPSEDITFORLINUX/IWEBOFFICECHINA/WEBOFFICEYOZO/WEBOFFICEWPS/OAWORK/JINGESOFT/ZHUOZHSOFT/IWEBOFFICE2015/IWEBPDF2018/SHUKEOFD/FUXINOFD`；应用类型 `pdf`/`office`。

**两张配置表**：`docPlugManagerSetting`（`plugtype/operatetype/isopen/visionnum/fileid/title/functiondes/format/browser/phone/applytype/licenseCode/syncdata`）、`docplugindetail`（字段级配置）。

**接口**（`DocPlugManagerAction` `@Path("doc/console/plugmanager")`）：`/api/doc/console/plugmanager/{getreadinfo,geteditinfo,savereadinfo,saveeditinfo,getreadlist,geteditlist,saveReadList,saveEditList}`；权限点 `DOC:DocplugManager`。

**运行时三处判定点**（不是单一开关）：
1. 国产化控件类型：表 **`iweboffice_config.clientType`**（`default/webwps/yozoOffice/iwebofficeChina`）
2. iWebOffice2015 / iWebPDF2018 / 北京CA：`DocPlugUtil.getoffice215Set()/getiWebPDF2018Set()/isopenbjca()` 读 `docPlugManagerSetting`
3. WPS / 永中：`ImageConvertUtil.canViewForWps()`、`YozoWebOfficeUtil.canEditForYozo()`（读 `doc_yozo_for_weaver`）

### 6.3 签出 / 签入（并发控制）

| 方法 | 行号 | 行为 |
|---|---|---|
| `isCheckOut(docid,user)` | :3835 | 读 `DocDetail.checkOutStatus/checkOutUserId/checkOutUserType/checkOutDate/checkOutTime`；`status∈{"1","2"}` 且非本人 → `api_status=0` + 提示 |
| `checkOut(...)` | :3879 | `update DocDetail set checkOutStatus='1', checkOutUserId=?, ...`；写 `DocDetailLog` `operateType="18"` |
| `checkIn(docid,ip,user)` | :3913 | `update DocDetail set checkOutStatus='0', checkOutUserId=0, ...`；写 `DocDetailLog` `operateType="20"` |

**关键结论**：
- **锁存在 `DocDetail` 自身 5 个字段，无独立锁表**。
- **没有超时/自动释放**——`isCheckOut` 不做任何时间差判断，属「乐观提示 + 人工释放」。
- 附件级：`DocAccService.isCheckOut(docid, fileid, user)`（`:322`），复用同一套 `DocDetail` 判定。
- 前端入口：`GET /api/doc/save/isCheckOut`（`DocSaveAction.java:573-576`）；签入 `/api/doc/acc/fileCheckIn`。
- 前端跳转（`stores/DetailStore.js:481-518`）：`resource==='detail'` → `/spa/document/index.jsp?id=${docid}&isEdit=1#/main/document/edit`；`resource==='attach'` → `/spa/document/index2file.jsp?${params}#/main/document/fileView`。

### 6.4 版本管理（三套体系并存）

1. **文档级**：`DocDetail.doceditionid`（版本族 ID）+ `docedition`（版次）+ `isHistory`；`DocSecCategory.readerCanViewHistoryEdition/editionPrefix`。接口 `/api/doc/detail/docVersion`（`DocDetailAction.java:255`），实现 `DocDetailService.getDocVersion(:1714)`、`getDocVerList(:1770)`。
2. **附件/正文级（通用）**：**`DocImageFile.versionid`**（`weaver/docs/docs/DocImageManager.java:523` `AddDocImageInfo()`，SQL :558；版本号 `:543 versionIdUpdate.getVersionNewId()`，来自 `SequenceIndex.indexdesc='versionid'`）。`docfiletype`：`1`正文图片、`2`附件、`3`Word、`4`Excel、`5`PPT、`6`WPS、`7`docx、`8`xlsx、`9`pptx、`10`et、`11`html视频。
3. **WPS 在线编辑级**：表 **`wps_doccenter_fileinfo`**（`wpsfileid/ecfileid/mould/versionid/fromecfileid/frommould/filename/iscollaborativedoc`）。

> **`docversion` 物理表不存在**（v2 修正）。`copyForVersion` 也不是方法，而是 `updateDocImageFileOfPic(docid, doccontent, boolean copyForVersion)` 的布尔入参（`:3605`，`copyForVersion==true` 时 `ImageFileManager.copyImageFile` 复制新 imagefile 并替换正文里的 fileid）。

### 6.5 附件与转换

**`/api/doc/acc/convertFile`**（`DocAcction.java:575` GET / `:766` POST 移动端）：做的是 **Office → PDF / HTML 的在线预览转换**。
决策链：敏感词高亮 → `ConvertPDFUtil.isUsePDFViewer()` 为真时 `ImageConvertUtil.canViewForWps()` → `WebOfficeUtil.convert`（`convert="client"`）→ 否则本系统转换（`convertType=="pdf"`：查 **`pdf_imagefile`** 表，无则 `ConvertPDFTools.conertToPdf`；`=="html"`：`DocPreviewHtmlManager.doFileConvert`）。

**`/api/doc/acc/docAccReplace`**（`DocAcction.java:260`）：先 `DocAccService.isCheckOut` 判定，被签出则回滚已传文件并返回；否则 `accService.replaceAcc(...)`。**既能替换附件也能替换正文**（按 `oldfileid` 的 `docfiletype`），产生新版本（`versionDetail` 默认 517712）。

**`docimagefile` 表**：文档 ↔ 文件关联（`docid, imagefileid, imagefilename, docfiletype, versionId, versionDetail, isextfile, hasUsedTemplet, operateuserid...`）；真实字节在 **`imagefile`** 表（由 `ImageFileManager` 落盘）。正文 HTML 存 `DocDetail.doccontent`，正文图片以 `docfiletype='1'` 关联，URL 形如 `/weaver/weaver.file.FileDownload?fileid=<imagefileid>`。

---

## 7. 检索与搜索

### 7.1 微搜 vs 数据库（运行时二选一）

开关（`com/api/doc/search/service/DocSearchService.java:2027`）：

```java
public static boolean useFullSearch(){
    String flag = new BaseBean().getPropValue("doc_full_search","use_full_search");
    if(!"1".equals(flag)) return false;
    return SearchBrowserUtils.quickSearchValidate("DOCSEARCH","7");  // 微搜服务可用性探测
}
```

| 包 | 数量 | 说明 |
|---|---|---|
| `com/api/doc/center/cmd/dbsearch` | 19 个 | SQL 直查 `DocDetail` + `DocShareUtil` 权限子查询。`DocCountCmd`、`MyDocCountCmd`、`RecommendedReadDocsCmd`、`RecentReadersCmd`、`ReplyAndPraiseCountCmd/DetailCmd`、`MyColleaguesReaded*`、`AllRank/DeptRant/ComRant/SubRant`、`Top5Latest/MostRead/MostDownload/MostComments/HighestScore` |
| `com/api/doc/center/cmd/fullsearch` | 4 个 | 走微搜：`DocCountCmd`、`MyDocCountCmd`、`RecommendedReadDocsCmd`、`MyColleaguesReadedTableFullsearchCmd` |

**搜索引擎：不是 ES，也不是进程内 Lucene**（全仓 `elasticsearch` 0 命中）。微搜是**独立部署服务**，通过 **RMI** 调用：`weaver/fullsearch/interfaces/rmi/SearchRmi.java`（`search/creatIndex/getSearchInfo`）+ `service/SearchRmiService.java`。

### 7.2 检索条件模板

`GET /api/doc/searchlist/saveConditionMould`（`SearchListAction.java:326-329`）→ `ConditionUtil.saveConditionMould(:989)`。
两张表：**`DocConditionMould`**（`id`UUID/`name`/`userid`/`usertype`/`opdate`/`optime`）+ **`docConditionDetail`**（`mouldid`/`name`/`value`，`RecordSetTrans` 事务批量插入）。仅创建人可改（否则 500533）。条件合法性按 `DocCondition.values()` 枚举过滤。

---

## 8. 数据模型

### 8.1 访问方式

核心表**没有 JPA/Hibernate 实体**，统一用 `weaver.conn.RecordSet` **原生 SQL**，并复用 `weaver.docs.*` 的 `ComInfo`/`Manager` 与 `com.weaver.procedure.*` 存储过程包装类。

### 8.2 核心表

| 表 | 用途 |
|---|---|
| `DocDetail` | 文档主表（含 `docstatus`、`doceditionid/docedition`、`isHistory`、签出 5 字段、`doccontent` 关联、`istop/topdate`） |
| `DocDetailContent` | 正文大字段 |
| `DocSecCategory` | 子目录（含 `parentid`、`approveWorkflowId`、`isOpenApproveWf`、`isopenwater`、`iseEncryptshare`、`isEnableSecondAuth`、`nodownload`、`readerCanViewHistoryEdition`） |
| `DocMainCategory` | 主目录（层级经 `docseccategory.parentid`） |
| `DocImageFile` | 文档 ↔ 文件关联（含 `versionid`、`docfiletype`） |
| `imagefile` / `pdf_imagefile` | 真实文件字节 / PDF 转换结果 |
| `DocShare` | 文档共享主表 |
| `DocSecCategoryShare` | 目录默认共享（模板） |
| `shareinnerdoc` / `shareouterdoc` | 物化权限表（查询侧） |
| `DirAccessControlList` / `DirAccessControlDetail` | 目录 ACL 规则 / 展开明细 |
| `DocTreeDocField` | 虚拟目录树 |
| `DocDummyDetail` | 文档 ↔ 虚拟目录挂载 |
| `DocDetailLog` | 操作日志（阅读/编辑/发布/版本/签出签入/打印/下载/共享） |
| `DownloadLog` | 下载日志 |
| `recycle_docdetail` | 回收站 |
| `DOC_PROP` | 回收站等配置（PROPKEY/PROPVALUE） |
| `wps_doccenter_fileinfo` | WPS 在线编辑版本 |
| `DocNetdiskSizeSet` | 网盘配额 |
| `SystemWaterMarkSetting` / `docwatermarksecsetting` | 水印全局 / 目录级配置 |
| `DocConditionMould` / `docConditionDetail` | 检索条件模板 |
| `cus_fielddata` / `cus_fielddata_doc_enc` | 自定义字段数据 / 加密字段数据 |
| `bill_Approve`、`workflow_requestbase.docids`、`workflow_createdoc` | 流程 ⇄ 文档关联 |

> 注：`docfrontpage` / `newdocfrontpage` 属**新闻/主页 frontpage** 链路，非通用文档发布。

---

## 9. 前后端职责划分

| 能力 | 前端 | 后端 |
|---|---|---|
| 外壳/路由/状态 | `wui/engine.html` + `amain` 聚合 + MobX store | REST 网关分发 |
| 目录管理 | `#/docengine/sec/doccategory`（9 Tab） | `DocSecCategoryAction`/`InfoAction` → `cmd/secCategoryList|secCategoryInfo` |
| 模板 | `#/docengine/mould/*` | `DocMouldAction` → `cmd/docMould|docMouldFile|SecMould` |
| 权限/共享 | Tab「权限管理」+ 详情「共享」Tab | `DocRight*Cmd`/`DocDefaultRight*Cmd`；`com.api.doc.search.cmd.*ShareCmd` |
| 新建文档 | `#/document/add`（选目录）→ `#/main/document/edit`（表单） | `DocSaveService.doSave` |
| 提交/审批 | 详情提交按钮 | `doSubmit` → `approveWorkflow` → `RequestService.createRequest` |
| 发布 | 详情 `BTN_PUBLISH` | `DocDetailAction.publishDoc` → `DocMonitorManager.publishDoc` |
| 在线编辑 | `FileView.js`（iframe + `WebOffice.js`） | WPS / YoZo / iWebOffice 三条链 |
| 检索 | `#/document/search` | `dbsearch` / `fullsearch`（微搜 RMI） |
| 评论/点赞/评分 | 详情 Tab | `DocReplyAction`/`DocPraiseAction`/`DocScoreAction` |
| 回收站 | `#/docengine/multi/recycle` | `DocRecycleTableCmd` → `recycle_docdetail` |
| 日志 | `#/docengine/log/*` | `cmd/log/*Table` → `DocDetailLog`/`DownloadLog` |

---

## 10. 与公文模块（odoc）的关系

| | 旧版公文 | 新版公文 SPA |
|---|---|---|
| 路径 | `D:\Weaver2020\ecology\odoc\` | `spa/odoc/`（源码 `src4js/pc4backstage/odoc/`） |
| 形态 | 传统 JSP/WebOffice（`index_odoc.jsp` 259KB、`iWebOfficeOperation.jsp`、`yozoOffice/`） | React/MobX，路由前缀 **`odocengine`**，模块名 `weaOdocEngine`（`amain/index.js:46`） |

**交叉复用**（odoc 直接 link 到 docengine 路由）：
- `odoc/components/odocMould/index.js:158` → `/wui/engine.html#/docengine/mould/edit`
- `odoc/components/odocMould/docViewMould/index.js:92` → `#/docengine/mould/view`
- `odoc/public/createField/index.js:243` → `#/docengine/sec/fieldManagement`

即：**公文建立在内容引擎的模板与字段基础设施之上**，但两者路由前缀不同（`odocengine` vs `docengine`），属不同模块。

---

## 11. 关键路径清单

**前端**
- 外壳：`D:\Weaver2020\ecology\wui\engine.html`；框架 `spa/main/engine.js`；聚合源码 `src4js/pc4backstage/amain/index.js`
- 后台源码：`src4js/pc4backstage/document/`（路由 `index.js`、`components/`、`apis/`、`stores/`、`util/`）
- 内容前台：`src4js/pc4mobx/document/`（路由 `index.js`，`Add.js`/`Search.js`）
- 文档详情（**生效版**）：`src4js/pc4mobx/documentDetail/`（`Detail.js`、`DetailForm.js`、`FileView.js`、`OutDetail.js`、`DocLog.js`）
- 旧版详情（快照不全）：`src4js/pc/document/`
- 公文：`odoc/`（旧）、`spa/odoc/` + `src4js/pc4backstage/odoc/`（新）

**后端**
- `E:\project\ecology\ecology\src\com\engine\doc\`（web / cmd / service / entity / util / dao）
- `E:\project\ecology\ecology\src\com\api\doc\`（detail / search / center / wps / yozo / mobile / upload / docRecycle / watermark …）
- REST 桥接壳：`com\api\doc\console\**\web\*Action`
- 路由常量：`com\api\doc\search\util\DocLink.java`、`DocSptm.java`
- 经典文档层：`weaver\docs\docs\DocManager.java`、`DocImageManager.java`；`weaver\docs\category\security\MultiAclManager.java`
- 存储过程：`com\weaver\procedure\{docshare,docsharedetail,docseccategoryshare,docimagefile,docfrontpage}`
- Jersey 配置：**`d:\Weaver2020\ecology\WEB-INF\web.xml:221-232`**（不在 src 树内）

---

## 12. 已核实结论与已知边界

### 12.1 已核实（确定结论）

1. REST 映射：Jersey 1.x `/api/*`，扫描 `com.cloudstore;com.api`；`com.engine.doc.web` 经 `com.api.doc.**.web` 同名空壳子类桥接 → `/api/doc/console/...`。
2. 后台分层：REST Action → Service（`extends com.engine.core.impl.Service`）→ Cmd（命令模式）→ `RecordSet` 原生 SQL。
3. 文档状态机：0–8 语义 + 9（流程草稿）+ -1/-3/-6（blnOsp 中间态）；无 Java 枚举，仅 `DocManager.java:176` 注释 + 代码字面量。
4. 发布：`docstatus==6` 才可发布 → 置 2；`docfrontpage` 属新闻链路，与通用发布无关。
5. 权限两套：`MultiAclManager`（目录 5 操作码）与 `DocViewPermission`（文档 read/edit/delete/share/log/download/control）互不相同。
6. 共享三表关系：`DocSecCategoryShare`（模板）→ `DocShare`（文档规则）→ `shareinnerdoc`（物化）；`DocShareDetail` 已弃用。
7. 在线编辑 4 类载体，**无 OnlyOffice**；签出锁在 `DocDetail` 自身字段，**无超时释放**。
8. 版本三套并存；**`docversion` 表不存在**。
9. 微搜为独立 RMI 服务，**非 ES/Lucene**。

### 12.2 已知边界（未查到 / 快照缺失）

1. `docstatus=8`（作废）的 SQL 写入点未定位（走 `DocMonitoringService.cancelDoc` 分支，未展开）。
2. `/api/*` 上的 8 个 Filter 类名未逐一核对（`web.xml` 行 407/443/495/543/575/627/697/726）。
3. `src4js/pc4backstage/document/apis/docEditMould.js`、`apis/batchMaintenance.js` **文件不存在**（import 悬空）；对应 URL 为推断。
4. `src4js/pc/document/` 的 `actions/`、`reducers/`、`css/`、`util/` 目录不在源码树中。
5. `Search.js` 的导出接口无硬编码 URL（由后端 `rightMenu` 下发）。
6. `Doc_DirAcl_*`、`DocSecCategoryShare_Ins_G` 等**存储过程本体 SQL 不在 Java 源码中**，继承细节需查数据库端。
7. 微搜建索引（docdetail → 索引）代码未在本仓找到（仅见消费侧）。
8. 后台 `docCategory` 页面无「查看文档 → 详情」跳转（该页只管目录，不列文档）。

---

## 13. 模板体系与自定义字段（v3）

### 13.1 模板四种类与两张主表（两个同名 MouldManager，极易混淆）

| | 显示模板 | 编辑模板 |
|---|---|---|
| 类 | `weaver/docs/mould/MouldManager.java` | `weaver/docs/mouldfile/MouldManager.java` |
| 表 | **`DocMould`** | **`DocMouldfile`** |
| 缓存 | `weaver.docs.mould.DocMouldComInfo` | `weaver.docs.mouldfile.DocMouldComInfo` |
| INSERT | L414 | L385（多 `templateId`，回指 `DocMould.id`） |
| 默认 | L348-349 `isuserdefault='1' and mouldType=0`(HTML) / `=2`(Word) | 无独立默认，靠目录绑定 |

`DocMouldSaveCmd.java:94-101`：`mouldType==2` 时同步一份到编辑模板表（`OdocFileUtil.addDocMouldFileByDocMould`）。

**模板内容存 HTML，非 JSON**（`weaver/docs/mould/MouldManager.java:35-41` 类注释）：`mouldname` / `mouldtext`（HTML 正文）/ `mouldType` / `mouldPath` / `imagefileid`。
`mouldType` 语义（`KnowledgeTransMethod.java:145-155`）：`0=HTML`、`2=WORD`、`3=EXCEL`、`4=WPS`。

| 种类 | 主表 | 后端 | URL |
|---|---|---|---|
| 显示模板 | `DocMould` | `cmd/docMould/*` | `/api/doc/console/mould/{init,table,info,save,delete,labelList,setDefault,saveAsNew,iwebOffice}` |
| 编辑模板 | `DocMouldfile` | `cmd/docMouldFile/*` | `/api/doc/console/mouldfile/{table,info,save,delete,labelList,saveAsNew}` |
| 合同模板 | `HrmContractTemplet` + `DocMouldfile` | `weaver/docs/mouldfile/MouldManager.java:408`；`DocMouldInitCmd:53` 权限 `HrmContractTypeAdd:Add` | 同编辑模板 |
| 目录模板（= 分目录模板 SecMould） | **`DocSecCategoryTemplate`** | `cmd/SecMould/{SearchSecMouldCmd,DeleteSecMouldCmd}` | `/api/doc/console/categoryMould/{getSecMouldList.do,isOpenDetachable.do,rightMenu,delSecMould.do}` |

> **澄清**：`CategoryTemp`（目录模板）与 `SecMould`（分目录模板）**是同一张表 `DocSecCategoryTemplate` 的两种叫法**，不是两个功能——`SecMouldAction.java:33` 注释「目录模板Action」；`SearchSecMouldCmd.java:109` `sqlform="DocSecCategoryTemplate"`。

### 13.2 目录 ↔ 模板绑定表 `DocSecCategoryMould`

`DocSecCategoryMould(secCategoryId, mouldType, mouldId, isDefault, mouldBind, DocSecCategoryTemplateId)`

**奇数 mouldType = 显示模板族（`DocMould`），偶数 = 编辑模板族（`DocMouldfile`）**——四条证据：
- `DocMouldTableCmd.java:165` `mouldType in(1,3,5,7)`
- `DocMouldFileTableCmd.java:44` `mouldtype in(2,4,6,8)`
- `DocTemplateEditCmd.java:128-132` 按奇偶选不同 `DocMouldComInfo`
- `weaver/docs/mould/MouldManager.java:549-560` `isEditMould → in(4,8)` 否则 `in(3,7)`

`mouldBind` 语义（`DocTemplateEditCmd.createMouldBindCom` L96-106）：`1=可选(166)`、`2=绑定/强制(19478)`、`3=不显示(19479)`。
> `mouldType` 5/6 = 移动端为**推断**（由 `in(1,3,5,7)`/`in(2,4,6,8)` 反推），代码无直接注释。

### 13.3 模板如何真正生效

**显示模板 → 详情页**：`DocDetailService.getDocMouldId(docid,filetype)`（L426）→ 读 `docdetail.selectedpubmouldid`（L438/L442）→ `<=0` 用目录默认 `selectDefaultMould`（L506-511）→ 兜底 `MouldManager.getDefaultWordMouldId()`（L514-520）。

**编辑模板 → 新建页**：`GET /api/doc/save/getMouldContent`（`DocSaveAction.java:344`）→ `DocSaveService.getMouldContent(user,mouldid)`（L1290，用 `weaver.docs.mouldfile.MouldManager`）→ 取 `mouldtext` 剥 `<title>` → `PatternUtil.formatJson2Js`（L1318）。
目录默认解析 `getHtmlMouldIdBySec(secid)`（L1328）：`mouldBind==2`（强制）> `isDefault==1`。

**覆盖顺序**（`DocSaveService.java:520-526`）：目录默认 → `SELECTEDPUBMOULDID>0` 时用户显式选择覆盖。

**前端联动**（`pc4mobx/documentDetail/stores/DetailFormStore.js`）：
- 切**目录** `changeSecCategory`（L208-222）→ `getProperty` + `getEditSet` → **字段集合 + 模板整体刷新**（提示 81565「会清除文档属性和附件」）
- 切**模板** `changeSelectedPubMouldId`（L183-201）→ `getHtmlContent({mouldid})` → **只刷正文**，字段集合不动（提示 81566）；且**仅新建态生效**（URL 无 `?id=`）

### 13.4 书签（模板与属性的桥）

| 模板类型 | 书签表 |
|---|---|
| 显示模板 | **`MouldBookMark`**（`DocMouldLabelOrderListCmd.java:36`） |
| 编辑模板 | **`MouldBookMarkEdit`**（`DocMouldFileLabelOrderSaveCmd.java:53`） |
| 公文打印模板 | `MouldBookMarkPrint` |

候选来源 `DocMouldInitCmd.createLabelList()`（L148-192）：30+ 系统内置（`$DOC_DocId`/`$DOC_Content`/`$DOC_Subject`…）+ 自定义字段书签：
```java
rs.executeSql("select fieldlabel,fieldname from cus_formdict where scope='DocCustomFieldBySecCategory'");
// → 生成 "$CUS_" + fieldname.toUpperCase()
```
书签 → 属性映射表：**`DocSecCategoryMouldBookMark(DocSecCategoryMouldId, bookMarkId, DocSecCategoryDocPropertyId)`**（`DocSecCategoryChangeTmplCmd.java:415/456-461`）。

### 13.5 模板导入导出
`ImportMouldAction`（`@Path("doc/console/importmould")`）：`uploadCloudstore` / `downloadMould` / `uploadLocal` / `downloadLocal` / `getPath` / `deletefolder`。
实现 `com/engine/doc/util/ImportHtmlViewMould.java`：仅接受 `.zip` → `ZipUtils.unZip`（L107）→ 正文图片逐个 `ImageFileManager.saveImageFile()`（L250/304-334）→ **写 `DocMould` 且 `mouldType` 恒为 0（HTML 显示模板），不同步 `DocMouldfile`**（L255-259）。
导出：`ExportHtmlViewMould.java`（18.36KB）。
> `/api/ecode/demo/{moduleImport,bindToken}` 是 **E-code 外部接口**，后端 Java 源码 0 命中，**不是本模块实现**。

### 13.6 自定义字段体系

**scope 常量**：`DocConstant.java:7` `CUSTOM_SCOPE = "DocCustomFieldBySecCategory"`；目录模板专用 `"DocCustomFieldBySecCategoryTemplate"`。

| 用途 | 表 |
|---|---|
| 字段字典定义 | **`cus_formdict`** |
| 目录级启用 / 排序 / 必填 | **`cus_formfield`** |
| 下拉选项 | **`cus_selectitem`** |
| 字段值 | **`cus_fielddata`**（`scope, scopeid=目录id, id=docid`） |
| 加密值 | **`cus_fielddata_doc_enc`**（`DocEncryptUtil.java:29`） |
| 属性展示位 | **`DocSecCategoryDocProperty`**（`isCustom=1` 指向自定义字段；`isCustom=0` 用 `labelid` 指向系统内置属性） |

> `DocSecCategoryDocProperty` **只有表、无同名 Java 类**；Java 侧是 `weaver/docs/category/SecCategoryDocPropertiesComInfo.java`。

**字段类型（仅 5 种）**：`DocCustomFieldAddCmd.java:90-107` —— `1`单行文本、`2`多行文本、`3`浏览按钮、`4`复选框、`5`选择框；**其它一律拒绝**（L104-107）。
- type=1 细分：`1=text / 2=int / 3=float`
- 浏览按钮日期变体（`DocSaveService.java:988-1026`）：`2→DATEPICKER`、`290→datetime`、`402→年`、`403→年月`、其它 `TIMEPICKER`

> **无明细表 / 无主子表**：全 `com/engine/doc` 搜索 `detailTable|明细` 仅命中期刊明细与审批流明细，与自定义字段无关。

**渲染与保存**：表单 key = `customfield<fieldid>`（`DocSaveService.java:98` `CUSTOM_FIELD`）；渲染分支 L851-1048（PC/mobile 双分支），读取时解密（L857/L885）；保存 L2001-2079（insert/update `cus_fielddata`，L2069/L2073）。

> ⚠️ **字段加密存在已确认缺陷**：保存时 L2043 加密调用的 scope 误传为 `"HrmCustomFieldByInfoType"`（读取用 `DocCustomFieldBySecCategory`），导致**配置的加密被静默跳过、明文入库**。完整根因链与修复见 **§18.4**。

**接口**：`DocCustomFieldAction` → 实际 **`/api/doc/console/custom/{table,info,add,update,delete}`**（注意是 **`custom`** 不是 `customfield`）。

---

## 14. 其它功能域（v3）

### 14.1 协同编辑（coope）
`DocCoopeEditAction` `@Path("/doc/coope")`，**13 端点**：`info` / `insert` / `myshare/list` / `allshare/list` / `group/sharelist` / `save` / `mycreate/list` / `upload2doc` / `upload` / `judge` / `send` / `mobile/judge` / `modify/docsubject` / `copy`。
Service + **9 cmd**（`com/api/doc/detail/cmd/CooperateEdit/`）；工具类 **`DocCoopereateUtil`**（47KB，注意拼写多一个 e，**不存在** `DocCooperateUtil`）；`DocCoopeEM7Util`（12KB，EM7 消息）。
协同分支在 `DocShareService`：L137-138 `iscooper`、L191 读 `doc_cooperate_edit_set.shareEdit`、L193-214 `takePermission`、L474-510 协同态强制 `noDownload=1; sharelevel=2`。
> **PC 端无入口**（`pc4mobx/documentDetail` 搜 coope 零命中），仅移动端 `/api/doc/coope/mobile/judge`（返回 `coopelink`）。`DocViewPermission` 无协同分支。

### 14.2 订阅
`SubscriptionAction` **`@Path("/doc/subscription")`**（**不是** `/api/doc/subscribe`，该路径不存在），6 端点：`list` / `condition` / `tabInfo` / `rightMenu` / `optionType` / `treeNode`。
表 **`docsubscribe`**（`docid, subscribedate, approvedate, state, ownerid, othersubscribe, subscribedesc, ownerType`）+ 10 个 `Docsubscribe_*` 存储过程。
4 Tab：可订阅 / 订阅历史 / 订阅批准 / 订阅收回；操作 `approve` / `reject` / `getback` / `add`（`DocSearchService.java:61-64`）。前端 `Subscribe.js`，路由 `#/document/subscribe`。

### 14.3 知识管理（KT_*，独立表）
`com/api/knowledge/` **仅 4 个 java**：`KnowledgeAction`（`@Path("/knowledge/knowledge")` 3 端点）、`KnowledgeMapAction`（`@Path("/knowledge/knowledgeMap")` 6 端点）。
表：`KT_Knowledge`（知识点主表）、`KT_KnowledgeDetail`（含 **docIds** 引用文档）、`KT_KnowledgeHrm`、`KT_Map`、`KT_MapDetail`、`KT_ThemeKnowledge`。
> **不复用 docdetail**，仅以 `KT_KnowledgeDetail.docIds` 引用。前端 3 路由：`knowledgeEdit` / `knowledgeList` / `KnowledgeMapList`（另有 `KnowledgeMapEdit.js` 40KB **未注册路由**）。
> 前端调 `/api/knowledge/knowledge/saveKnowledgeType`，但后端 `KnowledgeAction` **无此端点**（死调用）。

### 14.4 资讯：新闻 / 期刊
**新闻 = 文档 + 发布类型**：
- `docdetail` 中 `docpublishtype='2'` = 新闻，`'3'` = 标题新闻（`DocNewsWebTableCmd.java:107/197`，且 `docstatus in('1','2','5')`）
- **`DocFrontpage` 只是新闻页配置**（`DocNewsTableCmd.java:145`）：`frontpagename / newstypeid / publishtype / isactive / newsclause / importdocid / defnewspicid / newsperpage`
- 新闻类型存 **`newstype`** 表
- `publishtype`：`1`=内部，`0`=外部，负值 `-{customerTypeid}` = 指定外部客户门户
- 权限点：`DocFrontpageEdit:Edit`、`DocFrontpageAdd:add`、`DocFrontpage:log`
- 外部访问 `/api/doc/news/{list,outer,content}`（`DocNewsWebAction` @Path("doc/news")，**允许无 User 访问**）；`outer` 仅返回 `publishtype='0' and status='1'`

**期刊三层结构**：`WebMagazineType`（分类）→ `WebMagazine`（刊号，`docID` 指向标题文档）→ `WebMagazineDetail`（条目，`docID` 指向具体文档）。
后台 `/api/doc/console/magazine/**`（15 cmd）；前台预览 `/api/doc/magazine/preview`（`DocMagazineWebAction` @Path("doc/magazine")，仅 1 端点）。权限 `WebMagazine:Main`。
> **期刊通过 `docID` 引用 docdetail，不复制文档内容**。

### 14.5 监控台与统计
**监控**：`DocMonitoringAction` `@Path("/doc/monitor")`，7 端点；**8 个 operation**：`deleteDoc` / `publishDoc` / `archiveDoc` / `invalidDoc` / `cancelDoc` / `reopenFromArchiveDoc` / `reopenFromCancellationDoc` / `checkInCompellablyDoc`。
数据流：`doOperate` → 权限校验（`DocEdit:Delete` 或 `MultiAclManager.hasPermission(...,OPERATION_CREATEDIR)`）→ `DocMonitorManager.executeDocMonitor(...)`。
**统计**：`Count.js` **不走 `/doc/monitor`**，走 **`/api/doc/readstatement/*`**（`ReadingStatementAction`，5 端点）；同族另有 docStatement / docCategoryStatement / departmentDocNum / subsectionDocStatement 四个 service。

### 14.6 文档外发 / 免登录
`DocOutDetailAction` `@Path("/doc/out/detail")`，4 端点（`basicInfo` / `docContent` / `tabInfo` / `docAcc`）；前端在 **documentDetail 模块内**（`documentDetail/index.js:36` `outDetail`），**不是** document 模块。
**鉴权（关键边界）**：`DocSptm.getOutDocId(idStr)` 用 `DesUtils` **DES 解密** docid（明文数字则 return 0），SQL 强制 `docpublishtype='2'` 且 `docstatus in(1,2,5)` → **仅新闻类型可外发**。
附件：`OUT_FILE_DOWNLOAD` = `/weaver/weaver.file.FileDownloadForOutDoc`（`FileDownloadForOutDoc.java`，DES 解 `fileid-docid`，同样校验新闻类型；`downloadfrom=wechat` 可跳过）。
> **无 token、无有效期、链接永久有效**——安全边界必须写清。
移动端另有一条更大的外发链：`DocOutShareAction` `@Path("/doc/mobile/outshare")` 13 端点 + 7 cmd + `DocoutShareManager`(87KB)。

---

## 15. 移动端（v3）

**规模**：后端 `com/api/doc/mobile/**` **81 个 java 文件**。
- `systemDoc/`：web `SystemDocAction.java`(36.83KB)、`DocOutShareAction.java`(16.84KB)、`ImageFileAction`；**cmd 约 33 个**（`GetDocDetailCmd` 40.5KB 最大、`SaveOfficeCmd` 12.8KB、`MyCategoryListCmd`、`GetCategoryListCmd`、`GetCategoryDocListCmd`、`GetDocVersionInfoCmd`、`GetAllDocListCmd`、`GetMyDocListCmd`、回复/点赞/删除/提交/保存/权限/分享/收藏/下载参数/模板内容/自定义 Tab …）；util `SystemDocReplyUtil`(80KB)、`SystemDocUtil`(51KB)、`CategoryUtil`(20KB)、`DocDetailUtil`(33KB)；bean `DocoutShareManager`(87KB)
- `news/`：9 cmd + `SearchDataUtil`(51KB)
- `fileview/`：4 个文件

**前端是独立 SPA，不复用 PC**：
- 产物 `spa/document/static4mobile/`（含 `service-worker.js`、`asset-manifest.json`、`precache-manifest.js` → **PWA 形态**），与 PC 的 `static4pc` 并列
- 源码 `src4js/mobile4mobx/document/index.js`（HashRouter + mobx Provider + react-dnd TouchBackend），**54 个 js**
- 7 条路由：`/systemDoc`、`/netdisk`、`/netdisk/search/history`、`/news/:scope`、`/doc/:id`、`/attach/:id`、`/add/:scope`（全部 asyncComponent 懒加载）
- 依赖 `weaver-mobile` 的 `Tools`（PC 端依赖 ecCom）→ **独立技术栈**

**主要能力**：移动端目录树、分类/全部/我的/收藏列表、文档详情（版本/附件/权限/office 保存）、新建/编辑/提交、回复点赞、下载参数、外发分享、用户自定义 Tab。

---

## 16. 横切机制（v3）

### 16.1 缓存体系
基类 `weaver/cache/CacheBase.java`：`extends BaseBean`；缓存名 `CACHE_NAME_ + 类全名`（L126）；删除标记 `cacheRemovedFlag`；观察者后缀 `_@cachemap_observer`（集群同步）；列定义靠 `@PKColumn`/`@CacheColumn` 反射（L136-186）；`initCacheSafe()` 并发安全（L188-210）。全平台 380+ 个 `extends CacheBase`。
- **`DocComInfo`**：`TABLE_NAME="DocDetail"`、`PK_NAME="id"` → cache key = docid；缓存列含 `docsubject / doccreaterid / seccategory / docstatus / ishistory / maindoc / accessoryCount / ...`；`updateDocInfoCache(key)`（L207-209）
- **`SecCategoryComInfo`**：`TABLE_NAME="DocSecCategory"`、`TABLE_ORDER="secorder,id"`；缓存列含 `maxuploadfilesize`（缺省 **5**MB，L119-121）、`nodownload`、`subcompanyid`、`parentid`、`coder`；`getSubcompanyIdFQ()`（L434）**走 DB 递归，非纯缓存**

**失效时机**：写完 DB 后手动刷（`DocManager.java:4910/6688`、`DocAcction.java:301-302`、`RequestDoc.java:1275`、正文转 PDF 等）。
> **风险**：目录改了配置但未刷 `SecCategoryComInfo`，权限/下载/大小限制会用旧值（`RightUtil.java:60-62` 从缓存读分部）。

### 16.2 分权（ManageDetach）
`weaver/hrm/moduledetach/ManageDetachComInfo.java`：`isUseDocManageDetach()`（L623-629，`detachable==1 && docdetachable==1 && docdftsubcomid 非空非 0`）；配置**无独立表**，走 `SystemSet_Select` 存储过程读 **`SystemSet`** 表（L165/L170/L179-180）；角色分部范围表 **`SysRoleSubcomRight`**（L834-848）。
`CheckSubCompanyRight.ChkComRightByUserRightCompanyId`（L548）返回 `-1禁止 / 0只读 / 1编辑 / 2完全操作`；结果缓存 key = `userid&rightStr&subcompanyid`（ConcurrentHashMap）；`userid==1` 直接返回 2。
影响面：目录树（`CategoryTree.java:39` `DOC_RIGHT_STR="DocSecCategoryAdd:Add"`）、回收站（`DocRecycleService.java:373-388`）、权限列表分部下拉、目录归属 `DocSecCategory.subcompanyid`。
> **纠正**：`com/engine/doc/util/RightUtil.java` **未直接调** `ChkComRightByUserRightCompanyId`，而是委托 `CheckPermission.checkEditPermission...`。

### 16.3 附件上传
`FileUploadAction` `@Path("/doc/upload")`（L76），`@POST @Path("/uploadFile")`（L87-90）。上传前校验目录创建权 `CategoryService.getAccount(user,secCategory).get("canCreate")`（L100-109）。
- **大小限制**：`DocSecCategory.maxUploadFileSize/minUploadFileSize`，由 `EditConfigUtil.java:31-33/86-88` 下发 `maxUploadSize/mixUploadSize/limitType`；缓存缺省 **5MB**
- **格式白名单**：`weaver/file/util/FileSuffixCheckUtil.java` —— 全局读 `doc_upload_suffix_limit.properties` 的 `upload_suffix`（缺省 `*.*` 不限制）**与**目录级 `uploadext` **取交集**（L157-167）；`getFileNameSuffix` 防 `../`、`\0` 绕过（L119-121）
- **无病毒扫描**（`FileUploadAction` 全文 `杀毒|virus|scan` 仅命中 import，无扫描逻辑）
- **临时表 `imagefiletemp`**：`DocAccService.java:1280-1281` 写入；保存后 `UploadFileUtil.java:55` 删除；**定时兜底 `DocTempAttachmentTimer`**（`implements ThreadWork`）：超 **4 小时**（L41）且无 `docimagefile` 关联的，删文件 + 清表

### 16.4 敏感词
`SensitiveWordAction` `@Path("/doc/sensitiveword")`（L32）：`/log`(POST)、`/wps/validate`、`/fileview/validate`(预览)、`/file/validate`。
开关 `DocSensitiveWordsUtil`：`enablePreviewValidateSensitiveWordHighlight()`（L65-67）、`enableOfficeSensitiveWordValidate()`（L25-34，`SensitiveSettingComInfo.getStatus()=="1"`）、`enableUploadAttachValidateSensitiveWord()`（L51-60）。
`WordProcessUtil.searchAndHighlightSensitiveWords(fileId)`（L545），结果缓存表 **`cache_sensitive_result_words`**（L540）。
**生效环节**：① 上传附件（`FileUploadAction:135-148`，命中即 `deleteImagefile` **直接删除**）；② 预览；③ WPS/本地插件在线编辑；④ 日志留痕。
> **⑤ 下载不校验**——`CheckSensitiveFilter` 白名单（L23-26）含 `/weaver/weaver.file.FileDownload`、`/api/doc/save/isExists`、`/api/doc/acc/loadFromWps`、`/api/doc/acc/loadFromYoZo`。

### 16.5 下载与预览鉴权
> **纠正**：`DocDownloadCheckUtil.checkPermission` **不是权限校验**，只做 **DES 混淆编码**——为每个 imagefileid 在表 **`file_download_permission(imagefileid, uuid)`** 生成 uuid（L139-142/L177-178），拼 `userid_uuid_yyyyMMddHHmm` DES 加密加前缀 `"a"`（L120-209）。URL 传加密串，`FileDownload` 反解真实 id（L1013/1043/2144）。
**真鉴权**：`weaver/file/FileDownload.java` `getWhetherHasRight(...)` → 无权限跳 `noright.jsp`（L1286-1297）；协作/WPS 分支用 `DocViewPermission.getShareLevel` + `DocCoopereateUtil.jugeUserCoopeRight`（L2200-2206）。下载日志 `downloadLog(...)`（L404-406），开关 `doc_writelog_config.doc_download_log`。
**水印**：按目录开关、非强制——下载需 `SECCATEGORYDOWNLOAD=1 && SECCATEGORYWMISOPEN=1 && download=="1" && frompdfview!="1"`（**PDF 预览器内下载不加水印**）；预览用 `SECCATEGORYVIEW`。
**`nodownload` / `downloadlevel`**：换算规律统一 `downloadlevel = (nodownload==0) ? 1 : 0`（`DocShareUtil.java:181-183/433-435`）。

### 16.6 全文索引同步（异步、无重试）
`SearchUpdateUtil.updateIndexLog(SearchUpdateType.DOC, docid)`（L64-66）→ `writeIndexLog`（L148-186）**线程池异步**（`ThreadPoolUtil.fixedPoolExecute`，L149）insert 表 **`indexupdatelog`**（`ID, DOCID, CTYPE, MODTYPE, CREATETIME, DOCCREATEDATE, DONEFLAG`，初始 **0**）。
`DOC` 类型展开为 `{DOC, DOCSEARCH}` 两条（L193-205）。
开关：`needInsertUpdateLog`（L213-227，查 `fullsearch_schemaInfo.canuse`）、`isOpenFullsearch()`（L233-273）。
`FullSearchIndexLogJob`（`extends BaseCronJob`，「微搜增量表清除定时任务」）：微搜未开启时 **`truncate table indexupdatelog`**（L47/L66）——**唯一的清理手段，无失败重试补偿**。
> `DONEFLAG` 的消费/置 1 端**不在本仓库**，由 `SearchRmiService`（RMI）在微搜侧完成。批量补偿见 `OperateService.java:311/319-332`（历史文档批量灌索引）。

### 16.7 多语言 / 国际化
**UI 层**：后端 `SystemEnv.getHtmlLabelName(labelid,languageid)`（L130-131）→ `LabelComInfo.getLabelname`（L140-141）；`LabelComInfo extends CacheBase`，`TABLE_NAME="HtmlLabelInfo"`、`PK_NAME="indexid"`，**缓存 key = `labelid + "_" + languageid`**（L57/L99）。表 **`HtmlLabelInfo(indexid,labelname,languageid)`** + **`HtmlLabelIndex(id,indexdesc)`**；`languageid==8`(繁体) 回退 7。
**前端**：`const getLabel = WeaLocaleProvider.getLabel;`（`docEngineIndex/index.js:13`），用法 `getLabel(33638,'内容引擎')`——第二参数是**缺省兜底文案**。`WeaLocaleProvider` 定义文件**不在本仓库**（运行时注入的全局对象）。
> **文档内容级多语言：不存在**——未检索到 `doccontent` 按 `languageid` 存储的多语言表/字段；标题正文单份存储（`DocComInfo` 缓存列无语言维度）。

---

## 17. 前端工程与部署（v3）

### 17.1 模块注册 `spa/moduleConfig.js`
`window.e9ModuleConfig = [{title, name, page, lib[]}]`——**无 `id`、无 `isBackend` 字段**，前后台靠命名约定 `f_*`/`b_*` + `page` 是否指向 `static4engine/engine.html` 区分。
文档相关 **3 条**：

| name | title | page | lib |
|---|---|---|---|
| `f_document` | 知识前台 | `/spa/document/static/index.html` | `/spa/document/static/index.css\|js?v=1605474670071`（L308-315） |
| `b_document` | 知识后台 | `/spa/document/static4engine/engine.html` | `/spa/document/static4engine/engine.css\|js?v=1605475388402`（L316-323） |
| `docDetail` | 文档详情页 | `/spa/document/index.jsp` | `/spa/document/static4Detail/index.css\|js?v=1605474680430`（L324-332） |

> 新闻/期刊在 moduleConfig 中**无独立条目**，是 `b_document` 的内部子路由。

### 17.2 路由懒加载
`src4js/pc4backstage/amain/index.async.js:75` **`['b_document','weaDocEngine','docengine']`**；元素格式 `[moduleConfig.name, window全局变量名, hash前缀]`。
映射规则（`routes` L116-143）：hash 变化 → `getChildRoutes` 遍历 `libs`；若 `window[lib[1]]` 已存在则同步挂载，否则 `pathname.indexOf('/'+libPath)===0`（数组则 `some`）→ `getLibRouteAsync` 按 `name` 取 `e9ModuleConfig.lib[]` 动态插 `<script>`。
即 **`#/docengine/...` → 加载 `b_document` → `spa/document/static4engine/engine.js` → 挂载 `docengine` 路由树**。
前台：`pc4mobx/amain/index.async.js:95` `['f_document','weaDoc','document']` → `#/document/...`。

### 17.3 `spa/document/` 多入口

| 入口 | 用途 |
|---|---|
| `static/index.html` | **现网前台**（`f_document` 的 page） |
| `static4engine/engine.html` | **现网后台**（`b_document` 的 page） |
| `static4Detail/index.html` | 文档详情（`docDetail` 的 lib） |
| `static4mobile/index.html` | 移动端（含 `em_ssoInit`、水印、主题） |
| `index.html` / `index_mobx.html` / `index_redux.html` | **旧 redux 版遗留** |
| `index.jsp` / `index2file.jsp` / `index2mobile.jsp` | JSP 包装，`<%@ include>` 到 `docs/e9/*.jsp` |
| `office.html` / `OfficeEditMould.html` / `OfficeViewMould.html` | WebOffice 控件宿主页 |
| `engine.html`（根目录） | 旧版后台入口，**moduleConfig 未引用**，勿与 `static4engine/engine.html` 混用 |

### 17.4 构建链路（**配置未随检出提供**）
> `src4js/package.json`、`.babelrc`、`build/`、`src4js/weaconfig/`（`weaconfig.js`、`weapath4e9.js`）**均不存在**；`src4js/webpack.config.js` 存在（6.95KB）但其 `require('./weaconfig/...')` 依赖缺失，无法解析 `document`/`document_single`/`document_redux` 的 entry 映射。
> **因此无法给出可执行构建命令**。产物关系由产物侧反推：`src4js/pc4backstage/document/*` --(webpack app 名 `document`)--> `spa/document/static4engine/engine.js`（版本号与 moduleConfig 完全一致，可确认配套）；`src4js/pc4mobx/document/*` --> `spa/document/static/index.js`。

### 17.5 ecCom 组件库
位置 `cloudstore/resource/pc/com/v1/`：`index.min.js`(737KB)、`ecCom.min.js`(1.49MB)。
> **ecCom 源码不在本仓库**（`src4js` 搜 `ecCom*`/`WeaTable*` 均 0），是预构建产物。
文档后台重度依赖：`WeaLocaleProvider`、`WeaTop`、`WeaRightMenu`、`WeaTab`、`WeaTable`、`WeaDialog`、`WeaOrgTree`、`WeaTree`、`WeaLeftRightLayout`、`WeaFormItem`、`WeaSearchGroup`、`WeaSelect`、`WeaUpload`、`WeaAuth`、`WeaTools` 等。
> 更正：`WeaTop2` 本仓不存在；`WeaForm` / `WeaTableNew` 来自 **`comsMobx`**（`/spa/coms/index.mobx.js`）而非 ecCom。

---

## 18. 覆盖度排查清单与边界风险（v3）

### 18.1 已覆盖要点核对

| # | 已覆盖项 | 状态 |
|---|---|---|
| 1–6 | 总体架构、前端技术栈/外壳/路由聚合、docengine 模块与路由树、docCategory 9 Tab、内容前台/详情、三套 SPA 联动与两入口 | ✅ |
| 7–9 | REST 映射机制（已查实）、包结构/24 壳映射/前缀清单、分层与命令模式/调用链 | ✅ |
| 10–12 | 文档生命周期、状态机、流程⇄文档关联 | ✅ |
| 13–14 | 权限两套体系/目录 ACL/共享三表/继承链、加密/水印/回收站/虚拟目录/网盘/日志 | ✅ |
| 15–18 | 在线编辑 4 类载体/选型开关、签出签入/三套版本、检索（微搜 vs 数据库）/条件模板、数据模型/职责划分/odoc 关系/路径清单 | ✅ |

### 18.2 v3 补充的遗漏项（含优先级）

| 优先级 | 补充项 | 位置 |
|---|---|---|
| **P0** | 移动端整套（后端 81 java + 独立 PWA SPA） | §15 |
| **P0** | 模板体系内部（两张表 / 两个同名 MouldManager / 绑定表 / 书签） | §13.1–13.5 |
| **P0** | 自定义字段体系（5 表 + 5 类型 + 无明细表） | §13.6 |
| **P0** | 缓存体系与失效（目录配置未刷缓存 → 权限/下载用旧值） | §16.1 |
| **P0** | 下载/预览鉴权真相（`DocDownloadCheckUtil` 只是 DES 混淆） | §16.5 |
| **P0** | 文档外发安全边界（DES + 仅新闻类型 + **无有效期**） | §14.6 |
| **P1** | 协同编辑（PC 无入口、仅移动端） | §14.1 |
| **P1** | 订阅（`/api/doc/subscription` + `docsubscribe`） | §14.2 |
| **P1** | 知识管理（独立 `KT_*`，仅引用文档） | §14.3 |
| **P1** | 新闻/期刊（新闻即 `docpublishtype='2'` 的文档；期刊三层表） | §14.4 |
| **P1** | 监控台与统计（8 operation；Count 走 readstatement） | §14.5 |
| **P1** | 分权（`SystemSet` + `SysRoleSubcomRight`） | §16.2 |
| **P1** | 附件上传（5MB 默认、白名单交集、**无病毒扫描**、4 小时清理） | §16.3 |
| **P1** | 敏感词（上传命中即删；**下载不校验**） | §16.4 |
| **P1** | 全文索引（异步、**无重试**、仅 truncate 清理） | §16.6 |
| **P2** | 多语言（仅 UI label；内容级无） | §16.7 |
| **P2** | 前端工程与部署（moduleConfig、懒加载、多入口、ecCom 无源码） | §17 |

### 18.3 边界情况与潜在风险

| # | 项 | 结论 |
|---|---|---|
| 1 | ref / children Tab | **死代码**：`DocDetailService` L729 `&& false`、L736 `if(false)`；前端无 store/api 绑定 |
| 2 | `com/api/doc/relate/*` + `cmd/relate/` | **名不副实**：`DocRelateWebAction` 仅 7 行空壳；3 个 cmd 内容是**组织画像-文档-影响力**（查 `docdetaillog`/`PRAISE_INFO`/`doc_reply`），**与文档关联无关**；无关联表 |
| 3 | 自定义字段加密 scope | **已确认缺陷（v4 查实，详见 §18.4）**：`DocSaveService` L2043 写入用 `"HrmCustomFieldByInfoType"`，L857/L885 读取用 `"DocCustomFieldBySecCategory"` → **字段加密静默失效，明文入库** |
| 4 | 模板导入 | 只写 `DocMould`（`mouldType` 恒 0），**不同步 `DocMouldfile`** |
| 5 | 协同编辑 PC 入口 | **不存在**，仅移动端 |
| 6 | 外发有效期 | **无 token、无有效期**，链接永久有效 |
| 7 | 附件病毒扫描 | **不存在** |
| 8 | 敏感词下载环节 | **不校验**（白名单豁免） |
| 9 | 全文索引失败 | **无重试补偿**，仅微搜未开时 truncate |
| 10 | 源码树悬空 import | `apis/fieldManagement.js`、`apis/magazine.js`、`apis/docEditMould.js` **不存在**；`src4js/pc/document/` 的 `actions/`、`reducers/` 缺失 |
| 11 | `/api/ecode/demo/{moduleImport,bindToken}` | 是 **E-code 外部接口**，后端 Java 0 命中，非本模块实现 |
| 12 | `mouldType` 5/6 = 移动端 | **推断**（由 `in(1,3,5,7)`/`in(2,4,6,8)` 反推），代码无直接注释 |
| 13 | 构建配置 | `package.json` / `weaconfig` **不在检出**，无法给出构建命令 |
| 14 | `WeaLocaleProvider` | 源码不在本仓，是运行时注入的全局对象 |

### 18.4 已确认缺陷：文档自定义字段加密静默失效（v4 查实）

**现象**：在目录「加密字段设置」中把某自定义字段设为加密（`isencrypt=1`）后，保存文档时该字段**并未落密文，而是明文存入 `cus_fielddata`**；前端读取仍显示明文，**界面无任何异常**。

**根因链**（逐跳证据）：

```
写：DocSaveService L2039  用 SCOPE_DOCUMENTCUSTOM("DocCustomFieldBySecCategory") 查配置 → 命中 isEncrypt=1 ✅
    DocSaveService L2043  调 encryt(..., "HrmCustomFieldByInfoType", secid, ...)   ← scope 传错 ❌
      → EncryptUtil.encryt L341  EncryptConfigBiz.getFieldEncryptConfig(tablename, fieldname, scope, scopeid)
        → EncryptConfigBiz L98-100  委派 new EncryptFieldConfigComInfo().getFieldEncryptConfig(...)
          → EncryptFieldConfigComInfo L235  key = (tablename+"|"+fieldname+"|"+scope+"|"+scopeid).toLowerCase()
          → 实际 key = "cus_fielddata|<列>|hrmcustomfieldbyinfotype|<secid>"
             ≠ 注册时的 "cus_fielddata|<列>|doccustomfieldbyseccategory|<secid>"
          → 返回 null
      → EncryptUtil.encryt L345  if (encryptFieldEntity != null) 为 false → 加密被静默跳过 → 明文入库

读：DocSaveService L857 / L885  用 SCOPE_DOCUMENTCUSTOM 查配置 → 命中 isEncrypt=1
    → EncryptUtil.decrypt L209  select * from <密文表> where id = <明文值> → 无记录
    → EncryptUtil.decrypt L233  return id → 原样返回明文
```

**关键判据**：`scope` 直接参与缓存 key 拼接（`EncryptFieldConfigComInfo.java:235`），故 `HrmCustomFieldByInfoType` 与 `DocCustomFieldBySecCategory` **必然查出不同结果**——不是命名风格问题，而是功能失效。

**为何隐蔽**：读取路径在「查不到密文记录」时原样返回入参（`decrypt` L233），所以界面既不报错也不乱码；但**数据在静止状态未加密**，安全目标未达成。

**触发条件**：① 全局加密总开关开启（`enableStatus != 0`）；② 该目录自定义字段在加密字段设置中 `isencrypt=1`；③ 保存时该字段有值。

**修复建议（一行）**：
```java
// DocSaveService.java:2043  将
value = Util.null2String(encryptUtil.encryt("cus_fielddata", column, "HrmCustomFieldByInfoType", secid+"", value, value));
// 改为
value = Util.null2String(encryptUtil.encryt("cus_fielddata", column, DocEncryptUtil.SCOPE_DOCUMENTCUSTOM, secid+"", value, value));
```
`"HrmCustomFieldByInfoType"` 是 **HRM 模块**的 scope，属从 HRM 代码复制过来的遗留。

> ⚠️ 修复后**存量已按明文写入的数据不会自动补加密**，需评估是否做一次性回填（用正确 scope 重跑加密）。
