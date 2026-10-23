# 泛微 E9 公文（odoc）模块架构与运作机制深度分析

> 分析对象：后端 `E:\project\ecology\ecology\src`（`com/engine/odoc`、`com/api/odoc`、`weaver/odoc`、`com/engine/plugin/odoc`）；
> 前端 `D:\Weaver2020\ecology`（`src4js/pc4backstage/odoc`、`src4js/pc4mobx/odoc`、旧版 `odoc/odoc/`、`spa/odoc/`）。
> 与《内容引擎 DocEngine 架构分析》同深度、同维度；**只读分析，未修改任何代码**。

---

## 目录

- [1. 模块组成与包结构](#1-模块组成与包结构)
- [2. 前后端职责划分](#2-前后端职责划分)
- [3. 功能入口与路由](#3-功能入口与路由)
- [4. 对外 REST 入口（机制已核实）](#4-对外-rest-入口机制已核实)
- [5. 核心能力](#5-核心能力)
- [6. 与流程、内容引擎的依赖关系](#6-与流程内容引擎的依赖关系)
- [7. 数据模型](#7-数据模型)
- [8. 在线编辑 / 版式文件（OFD·PDF·套红）](#8-在线编辑--版式文件ofdpdf套红)
- [9. 已知问题与缺失](#9-已知问题与缺失)

---

## 1. 模块组成与包结构

### 1.1 `com/engine/odoc`（业务实现层，500+ 文件）

| 子包 | 职责 | 证据 |
|---|---|---|
| `cmd/` | Command 层，38 个子包：`odocNumberManage`、`standard`、`setTemplate`、`odocMould`、`officialSettings`、`officalList`、`officalCenter`、`officalReport`、`odocrequest`、`exchange`、`haoqian`、`odocWps`、`docTypeChange`、`OdocFileSendDoc`(归档)、`monitor`、`supervise`、`withdraw`、`updateSign`、`signSetting`、`rightMenu`… | 含基类 `OdocAbstractCommonCommand.java` |
| `web/` | Action 层，**66 个 `*Action.java`**，只有方法级 `@Path`/`@GET`/`@POST`，**无类级 `@Path`** | `OdocFileAction.java`（79.5 KB，18 个方法级 `@Path`） |
| `service/` + `service/impl/` | Service 接口与实现（`OdocNumberManageServiceImpl`、`OfficialSetingsServiceImpl`、`OdocWpsServiceImpl`、`Officedv1/v2ServiceImpl`…） | 由 Action 反射注入 |
| `entity/` | ORM 实体（**自研 `weaver.orm` 注解，非 JPA**），8 个子包 | `entity/createDoc/WorkflowCreateDoc.java:15` `@Table("workflow_createdoc")` |
| `util/` | 55 个工具类，大量巨型类 | `WorkFlowTransMethod.java`（264 KB）、`OdocTypeChangeUtil.java`（158 KB） |
| `biz/` | 业务编排 | `odocrequest/OdocRequestManagerBiz.java`（125 KB）、`OdocFlowTransferBiz.java`（71 KB） |
| `constant/` | `OdocEnum`、`OdocMenuType`、`OdocRequestMenu`、`OdocSignInputConst` | |
| `systemBill/` | **流程系统单据** | `Bill66.java`（收发文流转单）、`Bill67.java` |

**Command 基类** `OdocAbstractCommonCommand.java`：
- 继承 `com.engine.common.biz.AbstractCommonCommand<Map<String,Object>>`
- `execute()`（L174-252）是**权限白名单网关**：按类名片段 + `HrmUserVarify.checkUserRight(...)` 逐个放行，未命中 `hasRight=false`（L250-251）

### 1.2 `com/api/odoc`（REST 暴露层，142 文件）

| 子包 | 职责 |
|---|---|
| `web/` + 11 个子目录 | **空壳子类**，仅类级 `@Path` + `extends com.engine.odoc.web.Xxx` |
| `service/` | 重型遗留 Service：`OfficalSendReceiveDocService`（148 KB）、`RequestDocService`（161 KB）、`SearchOfficalService`（86 KB）、`OfficalListService`（70 KB）、`OfficalCollectionService`（79 KB） |
| `util/` | `OdocTemplateWithWebOfficeUtil`（101 KB，**套红**）、`ODocTHWithOpinionUtil`（51 KB）、`OdocFileUtil`（100 KB）、`OdocPrintMergePDFUtil`、`ExchangeWebserviceUtil`、`RequestIdUtil`（60 KB） |
| `bean/` | 交换/收发文 WebService 报文 Bean |

### 1.3 经典层与插件层

| 层 | 路径 | 作用 |
|---|---|---|
| `weaver.odoc` | `weaver/odoc/`（42 文件） | `docs/MaintextToPDF.java`（正文转 PDF）、`docs/ChangeDocName.java`、`ofd/**`、`workflow/request/ODocRightManager.java` |
| `weaver.docs.change` | `weaver/docs/change/DocChangeManager.java`（150 KB） | **公文交换核心**，`OfficalSendDocAction:125,198,223,267,321` 直接调用 |
| `com.engine.plugin.odoc` | `com/engine/plugin/odoc/`（64 文件） | **公文插件层**，被 workflow 调用：`GetOdocRightMenuCmd`（37 KB）、`command/workflowPath/{SaveAddWFCmd,SaveEditWFCmd,SaveNewVersionCmd}` |

> 关系：`com.api.odoc`（URL 门面） → `com.engine.odoc`（cmd/service/web 主力） → `weaver.odoc` / `weaver.docs.change` / `com.engine.plugin.odoc`。

---

## 2. 前后端职责划分

### 后端负责
- 全部 REST 端点（`/api/odoc/**`、`/api/offical/**`、`/api/odocExchange/**`）
- 权限判定（`HrmUserVarify.checkUserRight` + `OdocAbstractCommonCommand` 白名单）
- 正文服务端处理：套红、痕迹保留、版本、PDF/OFD 转换
- 与流程引擎的建文档/流转/归档；交换协议（FTP / WebService，`docchangesetting.changemode`）

### 前端负责

| 前端 | 路径 | 职责 |
|---|---|---|
| 新版 React/MobX（引擎设置） | `src4js/pc4backstage/odoc/` | 公文引擎后台 `#/odocengine/*`，导出 `com.OdocCreatedoc`、`com.OdocComs4Workflow` 供流程引擎内嵌 |
| 新版 React/MobX（业务端） | `src4js/pc4mobx/odoc/` | 发文/收文/签报/待办/公文库/台账/报表 `#/main/offical/*` |
| **旧版 JSP（正文在线编辑）** | `odoc/odoc/`（241 个 JSP） | `index_odoc.jsp`（259 KB）、`index_odoc4requestText.jsp`（192 KB）、`WebOffice_odoc.js`（32 KB） |
| SPA 副本 | `spa/odoc/` | `index_odoc.jsp`（108 KB）、`static/index.js` |

> **关键**：`odoc/odoc/` **仍是运行时主用路径**（非废弃）——`RequestDocService.java:774/1210/1227/1233` 硬引用 `/odoc/odoc/index_odoc_router.jsp`；新 React 组件也直接跳旧 JSP。

**正文路由分发**（`index_odoc_router.jsp`）：
- L32-41 按开关决定编辑器：`office2015`(DocPlugUtil) / `wpsEditor` / `yozoOffice` / `webwps`
- L84-101 浏览器嗅探（IE/Windows），非 IE+非 2015+非 WPS → 强制只读
- L120-126 查 `workflow_createdoc.istextinformcanedit` 决定正文是否在表单内可编辑
- L164-197 分流到 `index_odoc4requestText.jsp` / `index_odoc4formShow.jsp` / `index_odoc.jsp`

---

## 3. 功能入口与路由

### 3.1 `#/odocengine/*`（公文引擎后台设置，26 个子路由）

定义 `src4js/pc4backstage/odoc/index.js:58-88`；异步注册 `amain/index.async.js:93` `['b_odoc','weaOdocEngine','odocengine']`。

| 路由 | 功能 | 路由 | 功能 |
|---|---|---|---|
| `createdoc` | 流程创建文档设置 | `receiveUtil` | 收发文单位 |
| `DocNumber` | 发文字号 | `SecretLevel` | 秘密等级 |
| `exchangeSetting` | 交换设置 | `setTemplate` | 公文流程模板设定 |
| `exchangeWorkflow` | 交换流程设置 | `TopicType` | 主题分类 |
| `generalSetting` | 通用设置 | `previewReport` | 自定义报表预览 |
| `InstancyLevel` | 紧急程度 | `tableColCustomSet` | 公文列表设置 |
| `OdocType` | 公文种类 | `fieldLibDialogContent` | 字段库设置 |
| `odocMould` | 公文显示模板 | `systemDefaultTableSet` | 系统默认列表 |
| `OdocWfMould` | 公文路径模板维护 | `rightmenttest` | 右键菜单**测试页** |
| `odocWorkflow` | 公文路径 | `odocNoRightPage` | 无权限页 |
| `OrgAbbr` | 机构代字 | `odocSignSetting` | 签字意见分组设置 |
| `processDefine` | 过程定义 | `odocRightMenuSetting` | 公文操作菜单 |
| `publicGroup` | 公共组 | `odocNodeSetting` | 公文节点设置 |
| — | — | `customReport` | **已注释掉**（index.js:77） |

**跨模块复用**：`window.weaOdocEngine.com.OdocCreatedoc` 被 `pc4backstage/workflow/components/pathSet/mainFrame/WorkflowCreateDoc.js:47-48` 引用；`OdocComs4Workflow` 被 `NodeSet.js:142-153`、`OnlineSign.js:31-34` 引用。

### 3.2 `#/main/offical/*`（公文业务端）

定义 `src4js/pc4mobx/odoc/index.js:72-142`：
- 发文 `sendDoc`/`sendDocManage`；收文 `ReceiveDoc`/`receiveDocManage`/`registDoc`；签报 `signDocManage`
- 待办/在办 `todo`/`newTodo`/`odocDone`/`odocFinish`/`trackingWF`/`searchReq`
- 公文库 `odocLibrary`/`odocLibraryNew`/`docVersion`/`accountList`（台账）
- 公文中心（嵌套）、报表 `odocReport/flowChart/:id`、`odocReport/odocCount/:id`
- 门户 `odocPortal/odocWFManage/:id`（1发文/2收文/3签报）
- 其他 `OnlineEdit`、`odocSignApproval`（签批）、`supervise`、`portalElement`

---

## 4. 对外 REST 入口（机制已核实）

**与内容引擎完全一致**：Jersey 只扫 `com.cloudstore;com.api`（`WEB-INF/web.xml:221-232`），`com.engine.odoc.web.*` 不被扫描，必须在 `com.api.odoc.web` 建空壳子类。

```java
// com/api/odoc/web/OdocNumberManageAction.java:13-15
@Path("/odoc/odocNumberManage")
public class OdocNumberManageAction extends com.engine.odoc.web.OdocNumberManageAction{}
```

**A. `/odoc/*` 组（引擎设置类）**

| 类级 @Path | 主要端点 |
|---|---|
| `/odoc/odocNumberManage` | `isUseOdocManageDetach`、`GetOperateLevel`、`getOdocNumberList/Info`、`saveOdocNumberInfo`、`getReservedNumberList`、`generateReservedNumber`、`getWorkflowOdocNumberInfo`、`saveWorkflowOdocNumberInfo`、`saveRequestNumber`、`getCurrentYearSerial`、`getLatestYearSerial` |
| `/odoc/odocDocNumber` | `getFormItems`/`getList`/`getOne`/`save`/`delete`/`isUseOdocManageDetach`/`GetOperateLevel`/… |
| `/odoc/mould` | `init`/`table`/`info`/`save`/`delete`/`labelList`(GET+POST)/`setDefault`/`saveAsNew`/`iwebOffice`/`getSharedTempList`/`saveSharedTemp`… |
| `/odoc/odocFile`（79 KB） | `recordDocId`、`convertFileToHtml`、`convertHtmlToDocx`、`saveFile`、`readFile`、`confirmTemplate`、`thCancel`、`taohong`、`qysDownload`、`acceptRevisions`、`cancelTemplate`、`selectMouldForTemplate`、`smartOfficial`… |
| `/odoc/officialSettings` | 65+ 端点：`getWfBaseInfoSettings`、`saveBaseInfoSettings`、`getTemplateSetting`、`getSignatureNodeSettings`、`getOdocGetTextToPDFSettings`、`isOpenOFD`、`getRedOpinionSeting`、`getSuperviseSetting`… |
| `/odoc/odocWorkflow` | `del`/`getTreeData`/`getList`/`removeWorkflow`/`addWorkflow`/`getOdocWorkflowTemplateList` |
| `/odoc/odocrequest` | `loadConfig`/`loadData`/`saveData`/`flowNext`/`forceArchive`/`loadRejectConfig`/`flowReject` |
| `/odoc/odocReceiveUtil` | 22 个：`getList`/`getTree`/`getOne`/`save`/`delete`/`seal`/`unseal`/`uploadExcel`/`getImportResult`… |
| `/odoc/odocReport`、`/odoc/odocDocReport` | 报表与统计 |

其余 40+ 个：`/odoc/odocCenter`、`/odoc/odocDoclib`、`/odoc/filesenddoc`、`/odoc/openDocVersion`、`/odoc/rightMenu`、`/odoc/signSetting`、`/odoc/withdraw`、`/odoc/odocExchangeSetting`、`/odoc/haoqian`、`/odoc/hq`、`/odoc/printMould`、`/odoc/uploadPDF` 等。

**B. `/offical/*` 组（业务端，自身实现）**：`/offical/sendDoc`、`/offical/receiveDoc`（文件 119 KB/2291 行）、`/offical/monitor`、`/offical/collectionSend`、`/offical/collectionReceive`、`/offical/search`、`/offical/createreq`。

**C. `/odocExchange/*` 组**：`exchangeSend`/`exchangeReceive`/`exchangeLogin`/`exchangeLog`/`exchangePostManagement`/`exchangeField`/`exchangeCompany`/`exchangeModifyPassword`。

---

## 5. 核心能力

| 能力 | 实现类 |
|---|---|
| **发文** | `OfficalSendDocAction` + `OfficalSendReceiveDocService` + `weaver.docs.change.DocChangeManager`（`SendDocManual`/`SendDocByWebservice`） |
| **收文** | `OfficalReceiveDocAction` + 同上；登记 `ReceiveDocRegistComp.js` |
| **签报** | 复用 `officalList`/`odocDocReport`，由 `workflow_createdoc.odoctype` 区分 |
| **正文编辑（套红）** | `OdocTemplateWithWebOfficeUtil`（101 KB）、`ODocTHWithOpinionUtil`（51 KB）、`OdocTemplateWithDcsUtil`；端点 `taohong`/`thCancel`/`confirmTemplate`/`cancelTemplate` |
| **痕迹保留** | `acceptRevisions`/`acceptRevisionsByYozo`；`workflow_createdoc.ishidethetraces`/`issavetracebyclean` |
| **版本管理** | `cmd/OdocOpenDocVersion/OdocOpenDocVersionCmd`；JSP `listVersion.jsp` |
| **文号管理** | `OdocNumberManageAction` + `OdocDocNumberAction` + `OdocNumberManageUtil`（49 KB）+ `cmd/odocNumberManage/*` |
| **公文模板** | `cmd/setTemplate/` 45 个 Cmd：`OdocMould/*`（显示模板）、`OdocMouldFile/*`（编辑模板）、`OdocPrintMould/*`（打印模板）、`OdocTypeSetMould/*`（文种绑定） |
| **签章与印章** | 好签(HQ)：`web/HqSealAction`（`/odoc/hq`）、`service/HqSealService`、`cmd/haoqian/` 18 个 Cmd、`biz/haoqian/`；电子签章 `QysOfficeSignSettingBiz` |
| **流转与流程集成** | `WorkflowCreateDoc`、`systemBill/Bill66/Bill67`、`com/engine/plugin/odoc/command/workflowPath/*`、`cmd/odocrequest/{FlowNextCmd,FlowRejectCmd,ForceArchiveCmd}`、`OdocRequestManagerBiz`（125 KB） |
| **公文交换** | 外部：`DocChangeManager`（150 KB）+ `ExchangeWebserviceUtil`；内部：`cmd/exchange/innerexchange/*`；独立交换台 `/odocExchange/*`；模式开关 `docchangesetting.changemode`（ws=WebService，默认 FTP） |
| **归档与统计** | 归档 `cmd/OdocFileSendDoc/*`（`OdocArchivesCmd`、`PreFileDoc`…）；统计 `cmd/officalReport/*`、`cmd/officalCenter/*` |
| **收发文单位** | `OdocReceiveUtilAction`（22 端点）+ `OdocCommReceiveUtilAction` + `OdocPrivateReceiveUtilAction`；实体 `ReceiveUtil`(@Table `DOCRECEIVEUNIT`) |

---

## 6. 与流程、内容引擎的依赖关系

### 6.1 与流程引擎
1. **建文档配置表** `workflow_createdoc`（`WorkflowCreateDoc.java:15`，70+ 字段），由 `#/odocengine/createdoc` 维护；流程引擎内嵌 `weaOdocEngine.com.OdocCreatedoc`。
2. **路由分发读该表**：`index_odoc_router.jsp:121` → `select istextinformcanedit from workflow_createdoc where workflowid=?`
3. **插件式注入**：`com/engine/plugin/odoc/command/workflowPath/{SaveAddWFCmd,SaveEditWFCmd,SaveNewVersionCmd}` 在流程保存/新建版本时同步公文设置；`GetOdocRightMenuCmd`（37 KB）提供公文右键菜单。
4. **系统单据**：`systemBill/Bill66.java`（收发文流转单）、`Bill67.java`。
5. **权限常量复用流程**：`WorkflowManage:All`、`WorkflowAgent:All`（`OdocWorkflowAction:58,85,112,201`）。
6. **正文文档落库**：`odoc_requestdoc`（requestid ↔ docid）。

### 6.2 与内容引擎（docengine）
1. **直接引用 docengine 工具类**：`OdocFileAction.java:11-12` `import com.api.doc.detail.util.{ImageConvertUtil, PdfConvertUtil}`
2. **文件上传复用**：前端多处 `uploadUrl="/api/doc/upload/uploadFile"`
3. **模板复用**：`cmd/setTemplate/OdocMould/*` 操作的是内容引擎的 `DocMould`/`DocMouldFile`/`DocPrintMould` 体系（书签 `labelList`、标签顺序 `DocMouldLabelOrder*`）→ **公文模板 = docengine 文档模板的公文化封装**
4. **正文文档实体复用** `DocManager`（签出/签入判定）、`DocSeccategoryUtil`（归档目录）
5. **34 个文件引用 `com.engine.doc.*`/`com.api.doc.*`**

---

## 7. 数据模型

**持久化方式：无 JPA**（`javax.persistence` 在 `com/engine/odoc` 下 0 命中）；23 个文件用自研 `weaver.orm` `@Table/@Id`，其余全部 `RecordSet` 原生 SQL。

| 表 | 用途 |
|---|---|
| `workflow_createdoc` | **流程建文档主配置**（文种/密级/紧急度/文号/收发文单位/套红节点/痕迹/节点选择等 70+ 字段） |
| `odoc_numberManage` / `odoc_numberReserved` / `odoc_numberWorkflowBaseSet` / `odoc_numberRequest` | 文号管理 / 保留号 / 流程基本信息 / 流程使用记录 |
| `odoc_odoctype` / `odoc_topictype` | 公文种类 / 主题分类 |
| `DocSecretLevel` / `DocInstancyLevel` / `DocSendDocNumber` | 秘密等级 / 紧急程度 / 发文字号 |
| `DOCRECEIVEUNIT` / `sendReUnitRestrictions` / `receiveunit_group` / `docreceiveunit_source` | 收发文单位及其限制/分组/来源 |
| `workflow_processdefine` | 过程定义（**表名带尾空格** `"workflow_processdefine "`） |
| `odoc_requestdoc` | 流程 ↔ 公文正文文档关系 |
| `odoc_finish` / `odoc_archives` / `odoc_Archives_Set` | 预归档 / 归档主表 / 归档字段设置 |
| `odoc_docDistribute` | 公文分发设置 |
| `odoc_sign_modifylog` / `odocSigncolumnlist` / `odocSignMould` / `odocSignGroup` | 签字意见相关 |
| `docchangesetting` / `DocChangeWorkflow` / `DocChangeWfField` | 交换设置 / 流程 / 字段映射 |
| `workflow_requestbase` / `workflow_currentoperator` / `workflow_requestlog` | 流程基类表（复用） |

> 好签（签章）相关表**未查到**：`(from|into|update)\s+hq\w*` 全模块 0 命中，8 个 `Hq*Entity` 未加 `@Table` → 推测走独立 SDK/HTTP 服务，本地仅存流程-字段配置。**本仓库内未能确认具体表。**

---

## 8. 在线编辑 / 版式文件（OFD·PDF·套红）

### 8.1 OFD（`weaver/odoc/ofd/`，17 个源文件）
- 接口 `ofd/interf/OFDConvertor.java`；**4 家厂商实现**：`FuxinOFDConvertor`、`FuxinGSDKOFDConvertor`（福昕）、`YozoOFDConvertor`（永中）、`ShukeOFDConvertor`（数科）
- 服务 `ConvertOFDService`（正文转 OFD）、`ConvertAttachOFDService`（附件转 OFD）
- 流程 Action `convertOfdAction.java:10` → 节点附加动作自动转 OFD
- 开关 `workflow_createdoc.uploadofd`；属性文件 `WEB-INF/prop/weaver_odoc_convertofd.properties`

### 8.2 PDF
- `weaver/odoc/docs/MaintextToPDF.java`（35 KB）正文转 PDF
- `OdocPrintMergePDFUtil`（36 KB）+ `OdocPrintMergePDFService` 打印合并 PDF
- `OdocPDF417Service` + `OdocPDF417Util` PDF417 条码（公文条码）
- 配置 `workflow_createdoc.uploadpdf`；免登录白名单 `weaver_security_rules_odoc_document_no_login.xml:5-6`

### 8.3 WebOffice / iWebOffice / WPS / 永中
- iWebOffice：`odoc/odoc/iWebOfficeOperation.jsp`、`WebOffice_odoc.js`（32 KB）
- WPS：`index_odocPrintwps.jsp`、`webwps/upload.jsp`；引擎侧 `odocWps/OdocWpsService`、`wpsOfficed/Officedv1/v2ServiceImpl`
- 永中：`yozoOffice/upload.jsp`；`selectMouldForTemplateByYozo`、`acceptRevisionsByYozo`
- 路由开关 `index_odoc_router.jsp:32-41`（`office2015`/`wpsEditor`/`yozoOffice`/`webwps`）

### 8.4 套红合成
- `OdocTemplateWithWebOfficeUtil`（101 KB）WebOffice 套红
- `ODocTHWithOpinionUtil`（51 KB）套红 + 意见合成
- `OdocTemplateWithDcsUtil`（13 KB）DCS 套红
- 端点 `confirmTemplate`(L336)、`thCancel`(L418)、`taohong`(L448)、`cancelTemplate`(L1302)

---

## 9. 已知问题与缺失

### 🔴 严重

1. **文号管理多个端点缺失权限校验** —— `OdocNumberManageAction.java` 中 `isUseOdocManageDetach`(L52)、`getOdocNumberList`(L98)、`getOdocNumberInfo`(L127)、`saveOdocNumberInfo`(L155) 有 `HrmUserVarify.checkUserRight("ODOC:OdocNumber", user)`；但 **L183 `getOdocNumberCondition`、L206 `getReservedNumberList`、L229 `generateReservedNumber`、L252 `getWorkflowOdocNumberInfo`、L275 `saveWorkflowOdocNumberInfo`、L298 `getWorkflowFormOdocNumber`、L322 `saveRequestNumber`、L345 `getCurrentYearSerial`、L389 `getLatestYearSerial` 全部只判 `user != null`**。任何登录用户可读写文号、生成保留号、改流程文号。

2. **`OdocAbstractCommonCommand` 权限是"类名片段匹配"白名单**（L184-251）——靠 `className.contains("OdocType")` 等字符串包含放行。L211 `contains("OdocType")` 会同时放行 `OdocTypeChange*`、`OdocTypeSetMould*`；新增 Cmd 忘记加分支 → 默认 `hasRight=false`（功能不可用）；命名巧合 → 越权。

3. **`getCurrentYearSerial`/`getLatestYearSerial` 在 Action 里裸写 SQL**（L356、L400）——未走 Service/Cmd、无权限、无租户/分部隔离，`numberid` 直接取请求参数。

### 🟠 中等

4. `OfficialSettingsAction` **大量 `@Path` 缺前导 `/`**（L344/367/383/403/419/440/456/470/486/521/536/551/680/1031/1050），与同文件 `/xxx` 风格混用。
5. **`@Path` 加在 Service 类上**（分层混乱）：`SearchOfficalService.java:56` `@Path("/offical/search")`、`CreateOfficalService.java:46` `@Path("/offical/createreq")`。
6. **`com/engine/odocExchange` 模块整体不可达**——该目录 `^@Path` 0 命中，而 `com/api/odocExchange/` 8 个空壳各自 `@Path` 并 `extends` 它 → 若实现内无 `@Path` 方法，这 8 个端点**无任何可调用方法**。
7. **旧版 `odoc/odoc/` 与 `spa/odoc/` 双份 JSP 并存**（`index_odoc.jsp` 259 KB vs 108 KB），运行时指向 `/odoc/odoc/` → 高维护成本与行为不一致风险；残留 `.bak`（134 KB）。
8. **死代码/注释实现**：`OdocAbstractCommonCommand` L35-65、L164-172 注释；`pc4backstage/odoc/index.js:28,77` `customReport` 路由被注释但 `CustomReportStore.js:71` 仍拼该 URL → **点进去 404**；`pc4mobx/odoc/index.js` `CollectionSend`/`CollectionReceive` 全注释但 apis 保留。

### 🟡 轻微

9. **拼写/命名**：包名 `difDepCataloguo`（应为 Catalogue）、类名 `OdocSuperviceAction`、`OdocOPenDocVersionAction`、`odocModelSetAction`（首字母小写）；`util/OdocFileSenDocUtil` 与 `OdocFileSendDocUtil` 并存（前者是残缺副本）；前端 `gerneralSeting`、`ToViedComponent`、`Rightmenttest`。
10. **表名带尾空格**：`ProcessDefine.java:9` `@Table("workflow_processdefine ")`、`OdocNumberManageUtil.java:47` `"workflowactionset "`。
11. **国际化硬编码语种 7**：`OdocFileSenDocUtil.java:16,18,50` `SystemEnv.getHtmlLabelName(251,7)` —— 收了 languageid 却硬编码简体中文。
12. **浏览器嗅探脆弱**：`index_odoc_router.jsp:84-101` 依赖 UA contains；L101 Linux/Mac + 非 2015 + 非 WPS → 直接失去编辑能力。
13. **调试日志污染**：`index_odoc_router.jsp` 10+ 处 `writeLog`（含完整 queryString），每次打开正文都写库。
14. **巨型类**：`WorkFlowTransMethod` 264 KB、`OdocTypeChangeUtil` 158 KB、`OdocFileAction` 79.5 KB、`RequestDocService` 161 KB、`DocChangeManager` 150 KB、JSP 259 KB。
15. **交换硬编码残留**：`ExchangeWebserviceUtil.java:102` 注释里 `http://localhost:8080/services/OdocExchangeWebService?wsdl`。
16. `OdocMouldAction` 同名 `@Path("/labelList")` 两个（L124 GET / L144 POST），语义混淆。
