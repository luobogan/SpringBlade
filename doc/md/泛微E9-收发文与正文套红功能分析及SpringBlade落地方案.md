# 泛微 E9 收文 / 发文与「正文套红」功能分析及 SpringBlade 落地方案

> 分析对象：
> - `D:\Weaver2020\ecology` —— 泛微 Ecology **部署态 webapp**（JSP / JS / classbean / 配置），含 `odoc/`（979 文件，公文/收发文前端）。
> - `D:\workproject\ecology\src` —— 同一产品的 **Java 源码**（29923 个 `.java`），后端服务、Cmd、Util、数据表访问。
> - 对照目标：`D:\workproject\springbladeandreact\springBlade`（SpringBlade 后端）+ `ant-design-pro`（React 前端）。
>
> 方法：静态代码走查（只读），未做运行时联调。所有结论均给出**文件路径 + 关键代码/行号**作为证据。
> 配套阅读：`E9与Flowable7流程节点功能对比及改造可行性评估.md`、`泛微E9表单建模字段属性配置详解.md`、`字段控件体系-PC与手机端组件汇总.md`、`字段权限优先级链.md`、`测试流程与正式流程隔离方案设计.md`。

---

## 一、两个 Ecology 目录的定位与关系（先厘清，避免误对比）

| 目录 | 形态 | 能看到什么 | 看不到什么 |
|---|---|---|---|
| `D:\Weaver2020\ecology` | 部署态 webapp | 前端 JSP/JS/HTML、页面路由、按钮状态机、插件调用、Ajax 端点名、模板资源（`mouldbookmark.json`） | Java 后端实现（只有 `classbean/` 编译产物，无源码） |
| `D:\workproject\ecology\src` | Java 源码 | 后端 Service / Cmd / Util、SQL、数据表、文号生成、套红服务、权限校验 | 前端页面渲染、按钮状态机、插件交互 |

**关键结论：二者不是「两套收发文实现」，而是同一套实现的「前端部署态」与「后端源码态」互补视图。**
因此本文的「对比」不是 A/B 两个产品的差异，而是**同一功能在「业务流转（后端）— 表单渲染（前端）— 数据交互（两端契约）」三个切面上的分工与衔接**；真正的差异体现在后面 §六 与 SpringBlade 的架构对照。

分工速查：

- 收发文「是什么类型」→ 后端 `workflow_base.officaltype`。
- 收发文「页面长什么样、按钮怎么变」→ 前端 `odoc/odoc/index_odoc*.jsp` + `js/odoc/common/DocUtil_e9.js`。
- 收发文「正文如何套红」→ 前端触发 + 后端 `OdocFileAction` / `OfficedService` 合成。

---

## 二、收文 / 发文的业务判定与完整流转

### 2.1 类型判定（一个字段区分收发文）

后端判定「流程是否为公文流程、是收文还是发文」只看两个字段：

```java
// OdocRequestdocUtil.java:98 / :877
rs.executeQuery("select id from workflow_base where isworkflowdoc = '1' and officaltype > -1 ");
```

`workflow_base.officaltype`：**1=发文，2=收文，3=签报**。

类型在初始化时被规范化（`"1"/"30"→发文`、`"10"/"40"→收文`、`"20"/"50"→签报`）：

```java
// OdocRequestdocUtil.java:649-658 saveProcessSetting(...)
if("1".equals(odocWfType)||"30".equals(odocWfType)){ odocWfType="1"; ... }
else if("10".equals(odocWfType)||"40".equals(odocWfType)){ odocWfType = "2"; count += 10; }
else if("20".equals(odocWfType)||"50".equals(odocWfType)){ odocWfType = "3"; count += 16; }
// insert into workflow_process_relative (WORKFLOWID, NODEIDS, OFFICALTYPE, PDID, UUID)
```

前端同一语义（建文设置页下拉）：

```jsp
<!-- CreateDocumentByWorkFlow.jsp:976 -->
<select name="officalType" id="officalType" onchange="initProcessDefine()">
  <option value=1 ...>发文</option>
  <option value=2 ...>收文</option>
  <option value=3 ...>签报</option>
</select>
```

发文/收文入口动作不同（`OdocCenterAction.java:56-68`）：
`officalType="1"` → `createReqDraft`（发文拟稿）；`officalType="2"` → `createReqReceipt`（收文登记）。

### 2.2 环节（过程定义）也是同一份数据、按类型过滤

```jsp
<!-- WorkflowProcessDefine.jsp:21 -->
rs.executeSql("select * from workflow_processdefine where status=1 and linkType="+officalType+" order by sortorder");
```

发文链路：**拟稿 → 审核 → 签发 → 套红 → 印发**
收文链路：**登记 → 拟办 → 批阅 → 承办**
二者共用 `workflow_processdefine`，差异只是勾选了哪些环节（`processDefine.jsp:50-61` 的 `docTypeId` 复选框 1/2/3）。

### 2.3 建文配置：流程如何绑定「正文」

保存建文设置：

```java
// WFDocumentManager.java:89 / :133-136
saveCreateDocByWorkFlow(workFlowID, status, workFlowCoding, createDocument, documentLocation, ...,
                        useTempletNode, documentTitleField, printNodes, signatureNodes, ...)
// INSERT INTO workflow_createdoc(workflowId, status, flowCodeField, flowDocField, flowDocCatField,
//   defaultView, useTempletNode, documentTitleField, printNodes, newTextNodes,
//   iscompellentmark, iscancelcheck, signatureNodes, isWorkflowDraft, defaultDocType, ...)
```

`workflow_createdoc` 是**流程 × 公文**的核心配置表，关键列：

| 列 | 含义 | 本文关联 |
|---|---|---|
| `flowDocField` | 正文字段（表单里存 docid 的字段） | 正文定位 |
| `flowDocCatField` | 存放目录字段 | 发文库/收文库 |
| `defaultView` | 默认正文存放目录（`docseccategory`） | 收发文默认目录不同 |
| `useTempletNode` | **套红节点** | §五 套红触发 |
| `signatureNodes` / `mustSignatureNodes` | 签章节点 | 与套红并列的提交拦截 |
| `printNodes` | 打印/呈批节点 | |
| `documentTitleField` | 标题字段 | 正文标题取表单值 |
| `isPermissionTemp` | 是否按权限选套红模板 | |
| `showmouldfield` / `editMouldField` | 用表单字段动态选模板 | |
| `issuednum` / `receivenum` / `sendunit` / `receiveunit` / `writtendate` / `issueddate` | 文号、来文/发文单位、成文/印发日期 | 收发文字段语义不同 |

只有**发文**模板才初始化套红设置：

```java
// OdocRequestdocUtil.java:449-462
if("1".equals(odocWfType)||"30".equals(odocWfType)){ saveMouldBookMark(); saveTHSetting(workflowId,flowdoccatfield); }
```

### 2.4 运行时链路（打开表单 → 正文 → 流转 → 回写）

**① 打开流程表单，解析公文上下文**

```java
// RequestDocService.java:234 getFlowDocInfo(User user, Map paramMap)
flowDoc.haveDocFiled(workflowid+"", nodeid+"");        // 该节点是否有正文字段
// 读 workflow_createdoc（289-383）：flowdocfield / documentTitleField / useTempletNode / signatureNodes ...
// 取正文档 docid（423-447）：select <正文字段> from <表单主表> where requestid = ?
//   并有版本修正：从 DocDetail 取 doceditionid 最大的 id 回写正文字段
getDocMouldInfo(workflowid, requestid, systemtype, user, editMouldField);   // 模板信息（511）
insert into odoc_docMouldInfo ...                                          // 记录编辑模板（517）
if (useTempletNode == nodeid) { isEdit = 1; }                              // 套红节点强制可编辑（564-567）
```

**② 正文与流程的关联方式（重要：不在 docdetail 里）**

- 表单主表某字段（= `workflow_createdoc.flowdocfield`）存 `docid`；
- 反查：`OdocTemplateWithWebOfficeUtil.getdocidByRequestid`、`getImagefileidByRequestid`；
- 公文维度冗余表 `odoc_requestdoc(requestid, docid, officaltype, ...)`，每次流转提交后重建。

**③ 流转提交时的公文回写**

```java
// RequestManager.java:5605-5625
rs.executeQuery("select 1 from odoc_requestdoc where requestid = ?",requestid);
if (rs.next()){
    OdocRequestdocUtil.OdocRequestdocInitByReqId(requestid);   // 重建 odoc_requestdoc（含 issued_num 等）
    OdocRequestdocUtil.updateInnerChangeData(requestid);        // exchange_senddocinfo_inner / exchange_receive_doc_inner
}
```

### 2.5 收文 vs 发文的数据语义差异

同一张 `odoc_requestdoc` 表，字段语义不同（`OdocRequestdocUtil.java:109 / 1259-1280 / 1323-1352`）：

| 维度 | 发文 | 收文 |
|---|---|---|
| 编号 | `ISSUED_NUM`（文号） | `RECEIVENUM`（收文编号） |
| 单位 | `ISSUED_UNIT`（印发单位） | `RECEIVEUNIT`（来文单位） / `SENDUNIT` |
| 日期 | `WRITTEN_DATE`（成文日期）、`ISSUED_DATE`（印发日期） | 收文日期 |
| 套红 | **有**（`showMouldId` + 文号书签 + `barcode`） | **通常无**（`isUseTempletNode=false`） |
| 交换 | `exchange_senddocinfo_inner` | `exchange_receive_doc_inner` |
| 前端专属资产 | `odoc/docs/sendDoc/`（文号/种类/紧急程度/接收单位 21 个文件） | `odoc/docs/change/ReceiveDoc.jsp`（收文登记）、`DocReject.jsp`（退文） |

---

## 三、表单渲染：字段区 + 正文区如何组合

### 3.1 三种布局

```js
// js/odoc/common/DocUtil_e9.js:188-195
if(docInfo.docParam.isColumnShow=='1'){
  req_workflow_odoc = document.getElementsByClassName("wf-req-form-odoc-iframe");   // 一文双屏/分栏
}else{
  req_workflow_odoc = document.getElementById("requestdocInForm");                 // 正文嵌在表单内
}
```

- `.req-workflow-odoc` / `.wf-req-odoc`：独立正文区容器
- `#requestdocInForm`：正文内嵌表单
- `.wf-req-form-odoc-iframe`：分栏双屏

### 3.2 渲染分工

| 区域 | 渲染者 | 技术 |
|---|---|---|
| 字段区 | `WfForm`（`spa/workflow/static4form/index.js`） | 流程表单引擎（字段权限/必填/布局） |
| 正文区 | iframe 内 WebOffice 插件 或 无插件编辑器 | `index_odoc_router.jsp` 按浏览器/插件/签出态路由 |

统一入口路由器：

```jsp
<!-- index_odoc_router.jsp:149-210 -->
if(!isEdit || ".ofd".equals(...)){ router="#/main/document/detail"; } else { router="#/main/document/edit"; }
if(requestdocInForm.equals("1")){
   if(isIE=="false" && isWindows && !office2015 && !wpsEditor && ...)   → index_odoc4formShowContent.jsp / 4formShow.jsp
   else if("1".equals(isCheckedOut))                                    → 4formShowContent / 4formShow
   else if(isEdit||office2015)                                           → index_odoc4requestText.jsp   ← 套红/签章主力页
   ...
} else { strBackUrl = "/odoc/odoc/index_odoc.jsp"; }
```

### 3.3 字段区 ↔ 正文区的数据桥梁

公文上下文通过 `WfForm.getOdocConfig() / changeOdocConfig()` 在两端流转，`docParam` 全量序列化进正文 iframe URL：

```js
// DocUtil_e9.js:1437-1441
for (var key in docInfonew.docParam) {
    url += key + "=" + encodeURIComponent(docInfonew.docParam[key]);
}
```

`docParam` 关键键：`requestid / workflowid / nodeid / id(docid) / showMouldId / hasUsedTemplet / isUseTempletNode / documentTitleField / secFieldid / showMouldFieldid / formFieldList`。

**表单字段 → 正文书签（文号写入正文的实现）**：

```js
// index_odoc.jsp:4613-4641 getFormValueByMould()
var needFieldList = docInfo.formFieldList;                 // 后端算好模板需要哪些字段
for(var cnt = 0 ; cnt < needFieldList.length; cnt++){
    var needFieldvalue = encodeURI(parent.WfForm.getFieldValue("field" + needFieldid));
    MOULDURLPARAM["field" + needFieldid] = needFieldvalue;  // ← 文号等字段值
}
```

**正文 → 表单（回写）**：

```js
// index_odoc4requestText.jsp:3436-3491
function getFileNameByRequest(){ ... parent.WfForm.getFieldValue('field' + titileField); }
function saveTHTemplate(){ parent.window.WfForm.doSave(); }   // 保存正文 = 触发表单保存
```

---

## 四、`flowtable` 到底是什么（术语澄清，重要）

调研结论：**在 odoc（收发文）语境下，`flowtable` 并不是「正文表格 / 签批意见表 / 明细表」**。命中分两类，都与收发文运行时表单无直接关系。

### 4.1 前端：两个后台设置页的 CSS 类名

| 命中 | 位置 | 真实含义 |
|---|---|---|
| `freeFlowTable` | `src4js/pc4backstage/odoc/components/createDoc/freeFlowFuntionManager/freeFlowCom.js:92`<br>`className: 'freeFlowTable wea-new-table'` | **自由流程的权限设置可编辑表格**（标题「权限设置」，操作列仅「删除」） |
| `odocWorkflowTableSetting` | `src4js/pc4backstage/odoc/components/systemDefaultTableSet/index.js:33`<br>`<div className="odocWorkflowTableSetting">` | **公文列表默认设置页容器**（公文流程列表 / 公文库列表 / 督办列表 / 跟踪列表） |

二者属于 `src4js/pc4backstage/**`（后台设置引擎），打包进 `spa/odoc/static4engine/engine.js`，**不是收发文运行时页面**。

### 4.2 后端：`getWorkflowTable` = 「工作流程表」数据源

```java
// weaver/workflow/workflow/WfDataSource.java:14-19 / :28 / :345
/** 工作流程非数据库数据源分页处理 */
public class WfDataSource {
  public List<Map<String,String>> getWorkflowTable(User, Map otherparams, ...)  // 「工作流程表」
  public List<...> getWorkflowTableField(...)                                   // 「工作流程表字段」
```

返回 `workflow_formbase` / `workflow_bill`（主表 + `Workflow_billdetailtable` 明细）/ `Sys_tabledict`（人力、客户、项目、资产…）的表字典，供**浏览按钮 / 数据源下拉选表**使用。

> ⚠️ 此前 grep 命中的 `flowtable` 多数是 `getWork<flowTable>` 的方法名子串，不是独立标识符。全仓唯一真正的 `flowtable` 字面量是 `WorkflowServiceUtil.java:1782` 的 properties key `MobileShinkFlowTable.workflowids`（移动端流程缩略开关），与公文无关。

### 4.3 收发文流程表单里真正的「表」是什么

| 概念 | 承载者 |
|---|---|
| 表单字段区 | `WfForm` 渲染的字段（`workflow_billfield` / 明细 `Workflow_billdetailtable`） |
| 明细表 | workflow 自带 detail table（本工程通过 `ExcelPreview` 的 `DetailBlock` 渲染） |
| 正文 | **iframe 内的 WebOffice / 无插件编辑器**，对应 `MOULDURLPARAM` + `bookmark`，不是表格 |
| 签批意见 | 流程流转意见（独立于正文） |

**迁移提示**：若在 SpringBlade 侧沿用「flowtable」一词，建议**重新定义**为「流程表单表字典数据源」（对齐 `WfDataSource.getWorkflowTable`），而不要把正文/明细表误称为 flowtable。

---

## 五、「正文套红」完整机制（核心）

### 5.1 一句话定义

> 套红 = 把**红头模板文件**（含红头版式 + Word 书签）+ **正文文件** + **书签→表单字段值 Map** 交给合成服务，生成一份「套红后正文」，并把它记为**正文的新版本**；同时把 `hasUsedTemplet` 置 1，作为「已套红」状态。

### 5.2 触发条件（四者同时满足才自动套红）

```java
// OdocFileAction.java:967
}else if(docid >0 && isUseTempletNode && showMouldId > 0 && !hasUsedTemplet){
```

| 条件 | 判定 | 位置 |
|---|---|---|
| 正文已存在 | `docid > 0` | `OdocFileAction:967` |
| 当前是套红节点 | `nodeid == workflow_createdoc.useTempletNode`，或 URL 参数 `isUseTempletNode=true` | `index_odocedit.jsp:264`；`index_odoc.jsp:123` |
| 有套红模板 | `showMouldId > 0` | 同上 |
| 尚未套红 | `docdetail.hasUsedTemplet != '1'` | `index_odocedit.jsp:406/503/576` |

权限/环境附加条件：

- 无模板则隐藏「套红确认」按钮：`index_odoc.jsp:2125-2129`（`showmouldCount<=0 && showMouldId<=0` → `isshow=false`）。
- 非 IE + Windows 且未启用 office2015/wpsEditor → 套红类按钮全隐藏：`index_odoc.jsp:2141-2147`。
- 只读页一律禁用：`index_odocdsp.jsp:567-590`。
- 按权限选模板：`OdocTemplateSharedUtil.getisPermissionTemp(workflowid)` → `getCanUseTemplate(...)`（人/分部/部门/角色/安全级别/岗位，`OdocTemplateSharedUtil:40-202`）。

**提交强拦截（最硬的控制）**：

```js
// DocUtil_e9.js:738-748
if(eventKey == "BTN_SUBMIT" || ...){
  if (docInfo.docParam.isUseTempletNode && docInfo.docParam.hasUsedTemplet != "1" && needTip(...)) {
      antd.message.error('请先执行套红操作！');  return false;
  }
  if (docInfo.docParam.isMustSignatureNodes && !docInfo.docParam.hasSignatureSucceed && ...) {
      antd.message.error('该节点为签章节点，请先签章，再提交！'); return false;
```

### 5.3 两套实现（关键：插件路径无法移植到 Web）

#### 路径 A：插件（iWebOffice / Office2015）——客户端 Word 控件内合成

```js
// index_odoc.jsp:4500-4582（插件消息包，非普通 Ajax）
WebOfficeObj.WebSetMsgByName("SHOWDOCMOULDBOOKMARK","1");
WebOfficeObj.WebSetMsgByName("REQUESTID", requestid);
WebOfficeObj.WebSetMsgByName("WORKFLOWID", workflowid);
WebOfficeObj.WebSetMsgByName("SHOWMOULDID", tmpShowMouldId);      // 套红模板 id
WebOfficeObj.WebSetMsgByName("MOULDURLPARAM", JSON.stringify(MOULDURLPARAM));  // 表单字段值集合
WebOfficeObj.WebSetMsgByName("COMMAND","LOADVIEWMOULD");
if(WebOfficeObj.WebLoadTemplate()){          // ① 载入红头模板
    WebOfficeObj.RecordID = tempVersionid + "_" + docid;
    WebOfficeObj.WebLoadBookMarks();          // ② 替换书签（文号/主送/印发日期…）
    WebOfficeObj.WebInsertFile();             // ③ 把正文插入 Content 书签
}
```

顺序即：`清稿(acceptAll) → 保存清稿版本 → 打开模板 → 替换书签 → 插入正文 → 生成条码`。

自动套红（进入套红节点时）见 `index_odocedit.jsp:406-443`、`838-844`；无插件版先 Ajax 取清稿版本：

```jsp
<!-- index_odocedit.jsp:576-614 -->
jQuery.ajax({ url:"/odoc/odoc/iWebOfficeOperation.jsp",
  data:{ id:docid, imagefileId:imagefileId, operation:"generateCleanCopyVersion" }, ...});
// 然后 prop_type="showMould" → useTemplateForTHTime(parameter)
```

#### 路径 B：无插件（WPS / 永中 / 服务端）——服务端合成，前端只换 iframe

```js
// index_odoc.jsp:3808-3831
function taohong(){
    onSave();                                   // 先保存正文
    jQuery.ajax({ url:'/api/odoc/odocFile/taohong',
      data:{ showMouldId: showMouldId, docId: docid }, type:'POST',
      success:function(msg){
          jQuery("#docxContent").val(msg.useTempfilePath);
          window.frames["webOffice"].src = msg.useTempfilePreviewUrl;   // ← 正文区整帧换成套红后预览
          changeButtonStatus("taohong");
```

后端：

```java
// OdocFileAction.java:967-1042（getFile 内自动套红）
officedService.acceptRevisions(paramsMap);                 // 取消痕迹（清稿）
DocUtil.saveDocImagefile(docid, acceptedRevisionImagefileId, ..., "0", "套红前取消痕迹版本");
DocUtil.getBookMarkMap(false, isPermissionTemp, requestid, workflowid, showMouldId, docid, ...);
rtnImagefileId = officedService.taohong(paramsMap);        // ★ 合成，返回新 imagefileId
```

```java
// REST 端点
@POST @Path("/taohong")          → OdocFileAction:447-475   （合成 DOCX + PDF）
@POST @Path("/confirmTemplate")  → OdocFileAction:335-409   （套红确认，落版本）
@POST @Path("/thCancel")         → OdocFileAction:417-439   （置 hasUsedTemplet=null）
@POST @Path("/cancelTaoHong")    → OdocWpsAction:110-124    （版本回滚式取消）
```

底层合成（WPS/金山）：

```java
// weaver/wps/doccenter/impl/DocTemplate.java:43-79 taohong(...)
replaceParams{mouldType, mouldId, imagefileId, fileName} + bookmarkMap
→ OfficeWrapheader.replaceBookmark(params, bookmarkMap)  // :90
→ baseConvert.convert(apiUrl, requestParams, targetFilename, params)
→ 返回 result.get("imagefileid")
```

### 5.4 模板、书签与值传递

**模板来源**

- 套红（显示）模板：`docmould`（`mouldtype=2`），文件在 `imagefile.filerealpath` 或 `docmould.mouldpath`（`DocUtil.getMouldPath:273`）。
- 流程绑定：`workflow_mould(workflowid, mouldid, mouldtype, selectvalue, isdefault, systemtype)`，`mouldtype` 0/1=显示(套红)模板，3/4=编辑模板。
- 文号型套红：`workflow_mould.selectvalue = -numberid-100`（`OdocNumberManageUtil:561-605`）。
- 按权限：`OdocTemplateShared` + `OdocMouldShareSet`。

**书签字典**（`odoc/odoc/resource/mouldbookmark.json`，`mouldid=4022`「发文套红模板」）：

| 书签 | 含义 |
|---|---|
| `fld_Title` | 标题（系统导入） |
| `fld_Mark` | **发文文号** |
| `Content` | **正文插入点**（`FORMTEXT [正文]`，替换时跳过） |
| `fld_yfrq` | 印发日期 |
| `fld_zsdw` | 主送单位 |
| `fld_csdw` | 抄送 |
| `fld_qfrq` | 签发日期 |
| `fld_ngr` | 联系人 |
| `fld_lxdh` | 联系电话 |
| `barcode` | 盖章 / PDF417 二维码 |

**值的来源与装配**

- 书签定义：`MouldBookMark(mouldid, name, descript, showOrder)`；值：`DocMouldBookMark(docid, mouldId, bookMarkId, bookMarkValue)`。
- 装配入口：`RequestDocDocAddExt.getBookmarkList(Map)`（`:74`）→ `DocUtil.getBookMarkMap(...)`（`:683`）。
- 权限模板：`OdocTemplateSharedUtil.getTemplateSharedBookMark(requestId, workflowId, templateId, languageId)`（`:475`），按「书签名 == 表单字段 labelName」匹配取值。
- **跳过 `Content` 书签**：`DocUtil.java:773`、`OdocTemplateSharedUtil.java:512`（正文占位，不替换）。
- 日期可按 `dateShowType=1` 转中文（二〇二四年十月一日）：`OdocTemplateSharedUtil:575-780`。

**前端取表单值**：`getFormValueByMould()`（`index_odoc.jsp:4613`）→ `MOULDURLPARAM` → 服务端映射书签。

### 5.5 状态、版本与取消套红

**状态存三处**：

| 位置 | 字段 | 写入点 |
|---|---|---|
| `docdetail.hasUsedTemplet` | `'1'`/`'0'`/`null`（主标志） | `DocUtil.sethasTempalte:169`、`cancelTemplate:418`、`DataHandler.saveAsNewVersion:187` |
| `docimagefile.hasUsedTemplet` | 每个版本 | `DocUtil.saveDocImagefile:86/108`、`DataHandler.updateDoc:123-129` |
| `workflow_requestbase.hasusetemplet` | `'1'` | `OdocTemplateWithWebOfficeUtil.getHasUseTemplet:2056` |

**套红 = 新增正文版本**（不是覆盖）：

```java
// OdocWpsSaveCmd.java:61-64
DocUtil.saveDocImagefile(docid, imagefileid, docSubject + extendName, docFileType, hasUsedTemplet);
if("1".equals(hasUsedTemplet)){ DocUtil.sethasTempalte(docid); }
```

**取消套红（版本回滚式，推荐）**：

```java
// OdocWpsCancelTaoHongCmd.java:44-88
rs.executeQuery("select * from docimagefile where docid=? and (hasUsedTemplet is null or hasUsedTemplet='0') order by versionid desc", docId);
DocUtil.copyAsNewVersion(id, docId, oldImagefileId, ...);   // 复制未套红版本为新版本
DocUtil.cancelTemplate(docId);                              // update docdetail set hasUsedTemplet='0'
```

另有简单版 `/thCancel`（仅把两个 `hasUsedTemplet` 置 null，不回滚版本）。

### 5.6 文号（发文专属）：生成 / 占用 / 回写

出口 Action（流程流转到某出口时触发）：

```java
// AutoGenerateOdocNumberAction.java:25 execute(RequestInfo)
select numbername, numberid from odoc_numberRequest where requestid=?   // 已有则跳过
generateOdocNumber(requestid, workflowid, numberid, "add"/"update")
  → currentSerial + 1（跨年单流水时重置为 1）
  → update odoc_numberManage set currentSerial=?, isUsed=1 where id=?   // 占用
  → numberName = prefix [+yearPre+years+yearEnd] + serialPre + (fillZero?%0Nd:serial) + serialEnd
  → SaveRequestNumberCmd.updateOdocRequestInfo(...)   → odoc_numberRequest
  → SaveRequestNumberCmd.updateWorkflowDatas(...)     → 回写表单 odocfield 字段
  → delete from odoc_numberReserved ...               // 释放保留号
```

Action 注册在 `workflowactionset`（`interfaceid='AutoGenerateOdocNumberAction'`，`SaveWorkflowOdocNumberInfoCmd:375-414`）。

相关表：`odoc_numberManage`、`odoc_numberReserved`、`odoc_numberWorkflowBaseSet`、`odoc_numberRequest`。

---

## 六、与 SpringBlade / ant-design-pro 的架构对照

### 6.1 现状映射

| Ecology 概念 | SpringBlade / ant-design-pro 现状 | 差距 |
|---|---|---|
| `workflow_base`（流程定义） | `wf_process_definition`（BPMN 下沉，见「去 wf_ 表」系列文档） | 已有；**无 `officaltype` 公文类型** |
| 流程节点 / 出口 | `wf_process_node` / `wf_node_link` + Flowable BPMN | 已有 |
| 节点字段权限 | `wf_node_field_perm` + `nodePermission` 解析器（`renderForm`/`renderFormPreview` 下发 `fieldPerms`，`visible/editable/required` 三维度 + 派生 `perm`） | 已有，且**比 ecology 更清晰**（三维度） |
| 流程表单布局 | `formmode` Excel 网格布局（`ExcelDesign` 设计 + `ExcelPreview` 渲染） | 已有，且**PC/手机双端**（≤768px 列表化） |
| 字段控件 | `FieldControl`（统一入口，`desktop/mobile/auto` 变体）、`fieldTypes.ts`（编码单一来源）、`fieldTokens` | 已有，对齐 ecology 字段类型体系 |
| 流程表单渲染入口 | `Start.tsx`（发起/测试/实例）→ `InstanceFlow` → `ExcelPreview`（`layoutData` + `nodePermission` + `initialValues`） | 已有，对应 `WfForm` + 正文 iframe |
| 草稿修订（并发） | `draftRevision` 乐观锁（`saveBpmn` 条件递增） | 已有 |
| 测试隔离 | `is_test` 标记（测试流程不外溢，见隔离方案文档） | 已有 |
| 流转意见 | `RichTextEditor`（签字意见） | 已有（单条意见区） |
| **公文类型（收发文/签报）** | **无** | 需新增 |
| **正文（docdetail/docimagefile 版本体系）** | **无** | 需新建模块 |
| **红头模板 + 书签（docmould / MouldBookMark / DocMouldBookMark）** | **无** | 需新建模块 |
| **套红（合成服务 + 节点触发 + 提交拦截 + 版本/取消）** | **无** | 需新建模块 |
| **文号（odoc_numberManage + 出口 Action）** | **无** | 需新建模块 |
| **公文交换（exchange_*_inner）** | 无 | 可选（后期） |

### 6.2 可复用 vs 需新建

**可直接复用（改造成本低）**

1. **表单渲染**：`ExcelPreview` 已具备 ecology `exceldesign` 的能力（Excel 网格 + 字段控件 + PC/手机双端），收发文表单字段区可完全复用。
2. **字段体系**：`fieldTypes.ts` 已对齐 ecology 编码（1文本/2多行/3浏览按钮/4选择框/5附件/6复选/7特殊/8布局），公文字段（文号=文本、主送单位=浏览按钮/文本、印发日期=日期、紧急程度=下拉）可直接建模。
3. **节点字段权限**：`nodePermission` 三维度比 ecology 的 `perm` 单列更清晰；套红节点的「正文可编辑」可用现有权限链表达。
4. **流程骨架**：Flowable + 节点/出口 + 流转意见 + 消息通知（`blade-message`）已具备。
5. **并发与测试隔离**：`draftRevision`、`is_test` 可直接服务公文流程。

**需新建（本文重点设计）**

1. 公文流程类型（发文/收文/签报）与环节模板。
2. 正文模块（文档 + 版本）。
3. 红头模板 + 书签模块。
4. 套红服务（服务端合成）+ 节点触发 + 提交拦截 + 版本/取消。
5. 文号模块（生成/占用/保留/回写）。

---

## 七、SpringBlade 落地方案设计

### 7.1 总体思路

> **表单字段区沿用现有 `ExcelPreview` 体系；正文作为「独立文档对象」新建模块；套红一律走「服务端合成」（不移植 WebOffice 插件）；合成结果记为正文新版本，套红状态与节点校验复用现有权限/拦截链。**

### 7.2 数据模型建议（新建表，命名沿用本项目 `wf_` 前缀）

| 建议表 | 对应 Ecology | 关键列 |
|---|---|---|
| `wf_odoc_doc` | `docdetail` | `id, def_id, instance_id, title, sec_category, doc_status, doc_edition_id, has_used_templet, edit_mould_id, is_deleted` |
| `wf_odoc_doc_version` | `docimagefile` | `id, doc_id, file_id, file_name, doc_file_type, version_id, has_used_templet, version_detail` |
| `wf_odoc_mould` | `docmould` | `id, mould_name, file_id, mould_type(1编辑/2套红/3打印), system_type, def_id` |
| `wf_odoc_mould_bookmark` | `MouldBookMark` | `id, mould_id, name, descript, show_order` |
| `wf_odoc_doc_bookmark` | `DocMouldBookMark` | `doc_id, mould_id, bookmark_id, bookmark_value` |
| `wf_odoc_mould_wf` | `workflow_mould` | `def_id, mould_id, mould_type, select_value, is_default` |
| `wf_odoc_number` | `odoc_numberManage` | `id, type_name, prefix, year_pre, year_end, show_year, serial_pre, serial, digit, fill_zero, def_ids, mould_ids, is_open, current_serial, is_used` |
| `wf_odoc_number_request` | `odoc_numberRequest` | `def_id, instance_id, number_id, number_name, years, serial` |
| `wf_odoc_request` | `odoc_requestdoc` | `instance_id, doc_id, offical_type, issued_num, receive_num, issued_unit, receive_unit, written_date, issued_date, secret_level, urgency` |

**流程侧扩展**（建议加在 `wf_process_definition` 或独立配置表）：

```
offical_type           -- 0非公文 / 1发文 / 2收文 / 3签报
taohong_node_key       -- 套红节点（对应 workflow_createdoc.useTempletNode）
signature_node_keys    -- 签章节点
print_node_keys        -- 打印节点
doc_field_key          -- 正文字段（表单里存 doc_id 的字段）
doc_title_field_key    -- 标题字段
number_field_key       -- 文号字段
```

### 7.3 正文模块

- **定位**：正文是**独立于表单**的文档对象，通过表单某字段（`doc_field_key`）存 `doc_id` 与流程实例关联——与 ecology 一致（关联不在正文表内）。
- **创建**：发起/到新建文节点时，用上传的 docx 或由编辑模板生成首个版本（`wf_odoc_doc_version` version=1，`has_used_templet=0`）。
- **编辑**：正文区用 iframe 承载 Web 编辑器（现有 `RichTextEditor` 可承载简单正文；正式场景建议引入 OnlyOffice / 永中 / WPS Web 办公套件）。
- **版本**：每次保存/套红/清稿都新增版本行，不覆盖——这是「取消套红可回滚」的前提。

### 7.4 套红：服务端合成（推荐实现）

**为什么不做插件路径**：ecology 的 iWebOffice/Office2015 依赖 Windows + IE/ActiveX 客户端控件，属于浏览器插件方案，在 Web 端不可移植。SpringBlade 应只实现 ecology 的**路径 B（服务端合成）**。

**合成流程**

```
① 触发：进入 taohong_node_key 节点 且 doc_id>0 且 mould_id>0 且 has_used_templet!=1
② 清稿（可选）：生成「套红前取消痕迹版本」（has_used_templet=0）
③ 取书签值：instanceId + defId → 表单值（复用 renderForm 的 dataJson） → bookmark Map
   【映射来源：wf_odoc_mould_bookmark + wf_odoc_mould_wf.select_value】
④ 合成：红头模板文件 + 正文文件 + bookmark Map → 合成服务
        （推荐 docx4j / Apache POI + 书签，或 LibreOffice headless / WPS 转换服务）
        规则：替换除 Content 外的所有书签；把正文插入 Content 书签
⑤ 落库：新 imagefile → wf_odoc_doc_version(has_used_templet=1) + wf_odoc_doc.has_used_templet=1
⑥ 渲染：生成 PDF 预览 URL → 后端下发给前端 → 正文 iframe 换 src（对齐 ecology 无插件路径）
```

**接口建议**（对齐 ecology 命名，便于对照）

```
POST /api/blade-workflow/odoc/{defId}/doc/{docId}/taohong        body:{ mouldId }
POST /api/blade-workflow/odoc/{defId}/doc/{docId}/confirmTemplate
POST /api/blade-workflow/odoc/{defId}/doc/{docId}/cancelTaoHong   -- 版本回滚
GET  /api/blade-workflow/odoc/{defId}/doc/{docId}/bookmarks
```

返回 `{ useTempfilePath, useTempfilePreviewUrl, versionId }`，前端直接换 iframe src。

**模板与书签引擎**

- 模板：管理员上传 .docx 红头模板（预置 Word 书签：`fld_Mark` 文号、`fld_zsdw` 主送、`Content` 正文占位…），存文件服务，`wf_odoc_mould` 记录。
- 书签→字段映射：在流程建文设置里配置（对应 `workflow_docshow`），存 `wf_odoc_mould_bookmark` + 映射表；值为 `WfForm.getFieldValue` 等价物（SpringBlade 侧取 `renderForm` 的 `dataJson`）。
- **退路**：若不想依赖 Word 书签，可用「占位符模板 + docx 模板引擎」（`${wenhao}` 等），但会失去 ecology 模板文件（真实 .docx 书签）的兼容性，导入存量模板时需转换。

**触发与拦截（前端）**

- 节点属性里配置「套红节点」，后端 `renderForm` 在返回包里下发 `isUseTempletNode` / `hasUsedTemplet` / `showMouldId`（对齐 ecology `docParam`）。
- 提交拦截：在 `InstanceFlow`/`Start.tsx` 的提交/流转前校验——若 `isUseTempletNode && hasUsedTemplet != '1'` 则阻断并提示「请先执行套红操作」（对齐 `DocUtil_e9.js:738`）。
- 按钮状态机：`套红 / 套红确认 / 套红取消 / 选择其他模板` 的显隐由 `hasUsedTemplet` 驱动（对齐 `changeButtonStatus`）。

**取消套红**：找 `has_used_templet=0` 的最新版本 → 复制为新版本 → `has_used_templet='0'`（对齐 `OdocWpsCancelTaoHongCmd`）。

### 7.5 文号模块

- 配置：`wf_odoc_number`（前缀/年份/流水位数/补零/绑定流程与模板）。
- 生成时机：对齐 ecology 的**出口 Action**——在 Flowable 侧用 **ExecutionListener / JavaDelegate** 挂在指定流转线上（等价 `AutoGenerateOdocNumberAction`）。
- 占用与并发：`current_serial + 1` 需**数据库乐观锁或行锁**（`UPDATE ... SET current_serial=current_serial+1 WHERE id=?`），避免并发重号。
- 回写：把生成的文号写入表单 `number_field_key` 字段（等价 `updateWorkflowDatas`），并写入 `wf_odoc_request.issued_num`。
- 保留号：`wf_odoc_number_reserved`（可选，后期）。

### 7.6 收文差异点落地

- 收文**默认不配套红节点**（`taohong_node_key` 为空）→ 正文页走普通编辑分支，无套红按钮。
- 收文编号用 `receive_num`，字段语义走 `wf_odoc_request` 的收文列。
- 收文链路（登记 → 拟办 → 批阅 → 承办）用同一套节点/出口建模，只是环节配置不同（对齐 ecology 的 `linkType` 过滤）。

---

## 八、差异汇总对照表

| 维度 | 泛微 E9 | SpringBlade / ant-design-pro | 迁移动作 |
|---|---|---|---|
| 公文类型 | `workflow_base.officaltype` + `isworkflowdoc` | 无 | 新增流程类型字段 + 环节模板 |
| 正文存储 | `docdetail` + `docimagefile` 版本体系 | 无 | 新建 `wf_odoc_doc` / `wf_odoc_doc_version` |
| 正文与流程关联 | 表单字段存 docid + `odoc_requestdoc` 冗余 | 无 | 表单字段存 doc_id + `wf_odoc_request` |
| 表单渲染 | `WfForm` 字段区 + 正文 iframe | `ExcelPreview`（Excel 网格，PC/手机双端） | **可直接复用** |
| 字段控件 | ecology 原生控件 | `FieldControl`（统一入口 + 双端变体） | **可直接复用** |
| 节点字段权限 | `workflow_nodeform` / `wf_node_field_perm`（`perm` 单列） | `wf_node_field_perm` + 三维度 `nodePermission` | **复用（更优）** |
| 套红（客户端） | iWebOffice/Office2015 插件消息包 | **不可移植** | 放弃 |
| 套红（服务端） | `/api/odoc/odocFile/taohong` + WPS/金山合成 | 无 | **新建服务端合成**（推荐路径） |
| 红头模板/书签 | `docmould` + `MouldBookMark` + `DocMouldBookMark` | 无 | 新建 `wf_odoc_mould*` |
| 套红状态 | `docdetail/docimagefile/workflow_requestbase` 三处 | 无 | 落到正文表与版本表 |
| 取消套红 | 版本回滚 | 无 | 按版本回滚实现 |
| 文号 | `odoc_numberManage` + 出口 Action | 无 | 新建 + Flowable 监听器 |
| 提交拦截 | `DocUtil_e9.js` 前端强拦截 | 无 | 在提交/流转前加校验 |
| 并发（定义） | — | `draftRevision` 乐观锁（已有） | 复用 |
| 测试隔离 | — | `is_test` 标记（已有） | 复用 |

---

## 九、实施建议（分阶段）

| 阶段 | 目标 | 产出 |
|---|---|---|
| P0 | 公文流程建模 | 流程类型（发文/收文/签报）+ 环节模板 + 表单字段（文号/主送/抄送/印发日期/紧急程度/密级） |
| P1 | 正文模块 | `wf_odoc_doc` / `wf_odoc_doc_version` + 正文上传/编辑/版本 + 正文 iframe 集成到 `ExcelPreview` 布局 |
| P2 | 模板与书签 | `wf_odoc_mould` + `wf_odoc_mould_bookmark` + 流程建文设置（绑定模板/书签映射/套红节点） |
| P3 | 套红（服务端合成） | 合成服务（docx4j/POI 或办公转换服务）+ 套红/确认/取消接口 + 状态与版本 + 提交拦截 |
| P4 | 文号 | `wf_odoc_number` + Flowable 出口监听器 + 占用/回写/保留号 |
| P5 | 收文/交换/移动端 | 收文链路、内部交换（可选）、手机端正文与套红适配（复用 `ExcelPreview` 已有的 ≤768px 列表化） |

**风险提示**

1. **插件依赖不可移植**：iWebOffice/Office2015 方案必须替换为服务端合成；若必须保留痕迹/留痕（清稿）能力，需在合成前加「接受修订」步骤（对齐 `acceptRevisions`）。
2. **模板兼容性**：存量 ecology 红头模板是带 Word 书签的 .docx，SpringBlade 若用占位符方案需做一次转换。
3. **并发取号**：文号生成必须加锁，否则重号。
4. **版本膨胀**：每次保存/套红/清稿都新增版本，需配套清理策略（可复用现有 `is_test` 清理思路）。
5. **合成性能**：服务端合成（docx→PDF）较重，建议异步化 + 结果缓存。

---

## 十、结论

1. 两个 Ecology 目录是**同一产品的部署态与源码态互补视图**，不是两套收发文实现；差异体现在「后端业务流转 / 前端表单渲染 / 两端数据契约」三个切面上。
2. 收文与发文在 E9 中**共用同一套页面与渲染引擎**，差异由 `workflow_base.officaltype`（1发文/2收文/3签报）与环节配置（`linkType`）驱动；核心差异是**发文有套红节点、文号、印发单位**，收文有登记/来文单位/交换。
3. **`flowtable` 并非收发文正文/明细表**：前端是后台设置页的两个 CSS 类（自由流程权限表、公文列表设置），后端是「工作流程表」数据源（`WfDataSource.getWorkflowTable`）。迁移时需重新定义该术语，避免误用。
4. **套红**本质是「红头模板 + 书签替换 + 正文插入 + 生成新版本」，E9 有插件（客户端）与服务端合成两条路径；**SpringBlade 只能走服务端合成**，且需新建正文/模板/书签/文号四个模块，表单渲染与字段控件体系可直接复用现有 `ExcelPreview` + `FieldControl`。
5. 最大复用点是**表单渲染与字段权限**（`ExcelPreview` + `FieldControl` + `nodePermission` 三维度已优于 E9 的 `perm` 单列）；最大工作量是**正文文档体系与服务端套红合成**。

---

## 十一、补充：初版遗漏点与深化分析（复查后新增）

> 本章为二次复查结论，补入初版未覆盖或过于简化的能力面，并给出 SpringBlade 落地建议。

### 11.1 正文编辑器：最大的能力缺口（初版低估）

**Ecology 的正文编辑器矩阵**（三层并存，按环境降级）：

| 层 | 实现 | 位置 |
|---|---|---|
| 插件 | weboffice2015 / iWebOffice（Windows + IE/ActiveX） | `odoc/odoc/weboffice2015/`、`odoc/odoc/iwebofficechina/` |
| 无插件（在线） | CKEditor4 定制编辑器、永中 Office、WPS Web | `odoc/onlineEdit/`（`Ckeditor4Odoc/src/js/odocEditor.js`）、`odoc/odoc/yozoOffice/`、`odoc/odoc/webwps/` |
| 服务端转换套件（WPS Linux） | 书签替换 / 智能排版 / 接受修订 / 清稿 / 转 PDF / 转 OFD | `weaver/wps/view/linux/convert/`：`ReplaceBookmark`、`SmartOfficial`、`AcceptRevision`、`ClearRevisionAndComent`、`ConvertToPdf`、`ConvertDocToOfd` |

**SpringBlade 现状**：仅 `RichTextEditor`（`@opentiny/fluent-editor`，Quill 内核，纯 HTML 字符串）。

**差距清单**：无 docx 打开/另存、无 Word 书签读写、无修订留痕（track changes）、无 PDF/OFD 输出、无正文版本 diff。

**落地选项（三选一或组合）**：

| 方案 | 做法 | 适配度 |
|---|---|---|
| A. OnlyOffice Docs Server | 引入开源 OnlyOffice，回调保存 docx；原生支持书签、修订、转换 API | **最贴合**（书签/留痕/转 PDF 一站式） |
| B. WPS Web 开放平台 / 永中 Office Web | 商用办公套件 Web 化，与 E9 的 WPS 路径最接近 | 贴合，但有授权成本 |
| C. 自研「HTML 正文 + 服务端 docx 合成」 | 复用 `RichTextEditor`/FluentEditor 采集 HTML → 服务端 `convertHtmlToDocx` → 再套红 | **这正是 E9 已有路径**：`onlineEditStore.previewWithTempl()`（`src4js/pc4mobx/odoc/stores/onlineEditStore.js:128-147`）调 `/api/odoc/odocFile/convertHtmlToDocx`，多传 `docId + showMouldId` 即由服务端完成套红 |

**建议**：以 A（或 B）为主编辑器；C 作为"轻量公文/移动端"兜底。选型的关键约束见 11.3（留痕）。

### 11.2 打印 / 呈批表（初版仅一句带过，实为独立子系统）

**配置与数据**

- 打印设置：`workflow_printset`（`type in (1,3)` + `modeid`）、`workflow_flownode`
  - `printflowcomment`（是否打印流转意见）：**2=始终打印 / 0=始终不打印 / 其它=放入模板但不打印**（`PrintInfoBiz.java:35-56`）
  - 签字意见默认值：`PrintInfoBiz.getNodeDefaultSign`（`:58`）
- 模板：`odocPrintMould` + `workflow_mould_print` + `workflow_docshow_print` + `MouldBookMarkPrint`
- 命令集：`com/engine/workflow/cmd/workflowPath/node/form/print/`（`SavePrintSettingsCmd`、`SyncPrintModeCmd`、`SavePrintSignInfoCmd` 打印签章、`GetPrintTableInfoCmd`、`GetPrintSignInfoCmd`）
- 其它：`requestForm/LoadPrintTemplatesCmd`、`batchPrint/SplitPrintModeInfoCmd`（批量打印 / 分套打印）、`biz/requestForm/WfToDocBiz`（流程转文档）

**生成链路**

```
OdocTemplateWithWebOfficeUtil.replaceBookmarkValue(requestid, workflowid, user, printMouldid, printNodeId)
  → getMarkIdAndFieldIdMap(...) + getFieldValue(...)
  → workflowContentType=="2" 时取流转意见：ODocTHWithOpinionUtil.getWorkflowPrintOpinionInfo(...)
      并按 remarkAlign/otherAlign 包成 <p class="odoc_zhengwen"> 等 HTML
  → ConvertToDocxTestbak.toDocx(...) → savePdfImageFile(...) → createDoc(...)
  → 返回 printImagefileid / printDocid / printVersionid / mouldName
```

**关键认知**：呈批表的书签值可能来自**流转意见**——"审批意见进入公文"是打印链路的一部分，不是表单字段。

**SpringBlade 落地**：需新增呈批表模板 + 打印设置（按节点）+ 服务端合成 + PDF 输出；意见 HTML 可复用现有 `RichTextView`。批量/分套打印可后期。

### 11.3 清稿 / 留痕（痕迹管理）—— 影响编辑器选型的关键约束

**Ecology 配置**：`workflow_createdoc.isCompellentMark`（必须留痕）、`isCancelCheck`、`isHideTheTraces`、`cleanCopyNodes`（清稿节点）；表 `odoc_cleantracedoc(workflowId, ISCLEARTRACEDISABLE)`；命令 `OdocSaveTracePropSettingsCmd` / `OdocGetTraceDocumentSettingsCmd`。

**Ecology 实现**：WPS `AcceptRevision` / `ClearRevisionAndComent`；流程 Action `weaver/interfaces/workflow/action/CreateTraceDocument.java`、`CreateTraceDocumentByClear.java`。

**语义冲突点（重要）**：套红前**必须清稿**（`acceptRevisions` 生成"套红前取消痕迹版本"，`hasUsedTemplet=0`），但业务又要求保留修改痕迹——所以清稿结果本身要**存为一个版本**，而非丢弃。

**SpringBlade 落地**：
- 选 OnlyOffice/WPS → 有原生修订（track changes），可同时满足清稿与留痕；
- 选自研 HTML→docx → **无原生修订**，需自行设计痕迹（如"意见式修订记录"或保存前后 diff），否则只能放弃留痕能力。**这是选型的硬约束，建议在 P1 前定。**

### 11.4 智能排版 / 范文 / 敏感词（初版完全遗漏）

**智能排版**（与套红是两件事）：

```java
// weaver/wps/view/linux/convert/SmartOfficial.java:34
public int smartOfficial(int mouldImagefileId, int imageFileId, String imagefileName){
    // 上传模板 + 文档到 WPS → 拿到 wpsFileId
    requestParams.put("id", documentWpsId);
    requestParams.put("templateId", templateWpsId);
    requestParams.put("fieldConfig", "none");        // ★ 不做字段替换
    httpUtil.post(propUtil.getSmartOfficialApi(), requestParams, null);
```

> **套红 = 替换书签 + 插入正文；智能排版 = 按版式模板重排全文，不做字段替换（`fieldConfig=none`）。**
> 配置：`odocTypesetMould`、`OdocSmartOfficeSetAction`、`OdocSaveSmartOfficeSetCmd` / `OdocGetSmartOfficeSetCmd`。

**范文（模板正文）**：`com/engine/odoc/modelOrSenWordsSetting/`（`OdocModelSetService`、`ModelManagement`、`OdocModelShared`），配置表 `odoc_modelAndSenWordsSet`：

| 列 | 含义 |
|---|---|
| `modelAttachView` | 范文附件查看 |
| `istexttomodel` | 正文转范文 |
| `accordSenWords` | 按敏感词处理 |
| `isTextSearch` | 正文检索 |

**敏感词**：`weaver/security/sensitive/CheckSensitiveWord.java`、`com/engine/sensitive/`（`SensitiveWordComInfo`、`CheckSensitiveFilter`）、`ProcessSensitiveWordLogCmd` / `ProcessSensitiveWordApproveCmd`（送审流程）。

**SpringBlade 落地**：均为独立子系统，建议排在 P4 之后按需启用；敏感词可先用"文本过滤器 + 待审队列"的简化版。

### 11.5 国产化 / OFD / 版式（初版仅列了 `docfiletype`）

- `weaver/wps/doccenter/impl/ConvertDocToOfd.java`（docx→OFD）、`ConvertDocToPdf.java`
- `docfiletype=13` 即 `.ofd`；`index_odoc_router.jsp:149` 对 `.ofd` **强制走只读详情页**
- `com/engine/odoc/cmd/standard/uploadPDF/OdocUploadPDFCmd.java`

**意义**：政务公文国标（GB/T 9704）要求 OFD 版式文件；信创/国产化环境必须支持。

**SpringBlade 落地**：服务端合成需同时输出 **PDF（在线预览）+ OFD（归档/国标）**；可引入 ofd 转换组件或复用办公套件的转换能力。

### 11.6 移动端套红

- `weaver/mobile/webservices/workflow/WorkflowServiceImpl.java:4189-4201 / 4682-4694 / 5718-5730` → 返回 `templetStatus`（0/1/2：未套红 / 已套红 / 异常）
- 移动端（`mobilemode/`）复用**同一套后端接口**，套红仍由服务端合成完成，移动端只做状态展示与触发。

**SpringBlade**：`ExcelPreview` 已具备 ≤768px 列表化 + `MobileFieldControl`；移动端只需复用同一 `isUseTempletNode / hasUsedTemplet / showMouldId` 契约渲染触发按钮与状态徽标，无需额外后端。

### 11.7 电子签章与契约锁（与套红并列的"节点强制动作"）

- `workflow_createdoc.signatureNodes` / `mustSignatureNodes`
- 前端提交拦截：`该节点为签章节点，请先签章，再提交！`（`DocUtil_e9.js:745-748`）
- 契约锁集成：`wf_qiyuesuoSetting(workflowid, documentField)`（`OdocFileAction.java:386`）

**建议**：套红、签章、清稿本质都是"**节点必做动作（node mandatory action）**"，SpringBlade 应抽象为统一框架（配置节点 + 前置校验 + 提交拦截 + 状态回写），而不是三个独立 if。

### 11.8 转文（收文 → 发文）

- `js/odoc/common/OdocRightBtn.js:463 changeOdocType()`；`docInfo.docParam.odocTypeField`
- 语义：以当前公文为来源生成另一类型公文（如来文转发文）。

**SpringBlade**：实现为"以源实例为模板新建目标实例"，需记录来源关系（`source_instance_id`），并继承表单数据与正文。

### 11.9 布局级代码块：SpringBlade 已有等价物（可承载公文联动）

- ant-design-pro：`ExcelDesign/utils/runLayoutScript.ts`（`LAYOUT_SCRIPT_HOST_ID` / `getLayoutScript` / `runLayoutScript`），`ExcelPreview.tsx` 在内容末尾挂 `<div id={LAYOUT_SCRIPT_HOST_ID} />`
- 这正是 E9「布局级代码块」的等价物，可直接承载公文联动逻辑：文号联动、标题带出、主送/抄送单位联动、正文标题同步、按钮显隐。

### 11.10 初版遗漏的数据表（补全清单）

| 表 | 用途 |
|---|---|
| `odoc_requestdoc_hasinit` | 公文信息是否已初始化 |
| `odoc_docMouldInfo` | 实例使用的编辑模板记录 |
| `odoc_usercheck` | 用户查阅记录 |
| `odoc_topictype` | 主题词 |
| `odoc_odoctype` | 公文种类 |
| `odoc_field_lib` + `odoc_fieldlib_billfield` | 公文字段库 ↔ 表单字段映射 |
| `odoc_cleantracedoc` | 清稿开关（按流程） |
| `odocTypesetMould` | 智能排版版式模板 |
| `odocPrintMould` / `workflow_mould_print` / `workflow_docshow_print` / `MouldBookMarkPrint` | 打印/呈批表 |
| `odoc_modelAndSenWordsSet` / `odoc_modelshared` / `odoc_modelManagement` | 范文 / 敏感词设置 |
| `DocChangeWorkflow` / `DocChangeSend` / `exchange_senddocinfo_inner` / `exchange_receive_doc_inner` | 公文交换 / 转发 |
| `wf_qiyuesuoSetting` | 契约锁（电子签章） |
| `workflow_printset` / `workflow_flownode` | 打印设置（按节点） |

---

## 十二、修订后的差异总表（增补行）

| 维度 | 泛微 E9 | SpringBlade / ant-design-pro | 迁移动作 |
|---|---|---|---|
| 表单字段区 / 字段控件 | `WfForm` + ecology 原生控件 | `ExcelPreview` + `FieldControl`（双端变体） | **复用** |
| 节点字段权限 | `perm` 单列 | 三维度 `nodePermission` | **复用（更优）** |
| 正文编辑器 | 插件 / 无插件(CKEditor/永中/WPS Web) / WPS 服务端套件 | 仅 `RichTextEditor`（Quill，HTML） | **引入 OnlyOffice/WPS Web 或自研 HTML→docx** |
| 修订留痕 | WPS `AcceptRevision`/`ClearRevisionAndComent` + `CreateTraceDocument` | 无 | 随编辑器选型落地（硬约束） |
| 套红 | 插件 + 服务端双路径 | 无 | **服务端合成（唯一可移植路径）** |
| 智能排版 | `SmartOfficial`（`fieldConfig=none`） | 无 | 新建（P4+） |
| 范文 | `OdocModelSetService` + `odoc_modelAndSenWordsSet` | 无 | 新建（P4+） |
| 敏感词 | `CheckSensitiveWord` + `com/engine/sensitive/*` | 无 | 新建（可先简化版） |
| 打印 / 呈批表 | `workflow_printset` + `odocPrintMould` + `replaceBookmarkValue` + 批量/分套 + 打印签章 | 无 | 新建（P5） |
| OFD / 国标版式 | `ConvertDocToOfd`，`.ofd` 只读 | 无 | 服务端合成增加 OFD 输出（信创必做） |
| 移动端 | `mobilemode/` + `templetStatus` | `ExcelPreview` ≤768px 已就绪 | 复用同一契约即可 |
| 电子签章 / 契约锁 | `signatureNodes` + `wf_qiyuesuoSetting` | 无 | 与套红合并为"节点必做动作"框架 |
| 转文 | `changeOdocType()` | 无 | 新建（记录来源实例） |
| 布局级代码块 | 布局级可执行脚本 | `runLayoutScript` 已有 | **复用**（承载公文联动） |

---

## 十三、修订后的实施路线（P0–P6）

| 阶段 | 目标 | 关键交付 | 前置决策 |
|---|---|---|---|
| P0 | 公文流程建模 | 流程类型（发文/收文/签报）+ 环节模板 + 表单字段（文号/主送/抄送/印发日期/紧急程度/密级/主题词） | — |
| P1 | 正文模块 + **编辑器选型** | `wf_odoc_doc` / `wf_odoc_doc_version` + 正文上传/编辑/版本 + 编辑器集成 | **A OnlyOffice / B WPS Web / C 自研 HTML→docx（决定留痕能力）** |
| P2 | 模板与书签 | `wf_odoc_mould` / `wf_odoc_mould_bookmark` / `wf_odoc_mould_wf` + 流程建文设置（绑定模板/书签映射/套红节点） | 书签 vs 占位符 |
| P3 | 套红（服务端合成） | 合成服务 + 套红/确认/取消接口 + 状态与版本 + 提交拦截 + 清稿版本 | 合成引擎（docx4j/POI/LibreOffice/办公套件） |
| P4 | 文号 + 智能排版/范文/敏感词 | `wf_odoc_number` + Flowable 出口监听器 + 占用/回写/保留号；排版/范文/敏感词 | — |
| P5 | 打印呈批表 + 电子签章 + 转文 + OFD | 呈批表模板与设置 + 服务端合成 PDF/OFD + "节点必做动作"框架 + 转文 | — |
| P6 | 收文链路 + 交换 + 移动端 | 收文登记/拟办/批阅/承办、内部交换、移动端套红与状态 | — |

---

## 十四、修订后的关键风险（补充）

1. **编辑器选型是一票否决项**：留痕（track changes）能力完全取决于编辑器，必须在 P1 前定稿；自研 HTML→docx 路线意味着放弃原生修订。
2. **插件不可移植**：iWebOffice/Office2015 必须替换为服务端合成。
3. **清稿与留痕的语义冲突**：套红前必须清稿，但清稿结果必须留版本，否则痕迹丢失（E9 用"套红前取消痕迹版本"解决）。
4. **模板兼容性**：存量 E9 红头模板依赖 Word 书签，改用占位符需一次转换。
5. **并发取号**：文号生成必须加锁。
6. **版本膨胀**：保存/套红/清稿/打印均产生版本，需清理策略。
7. **合成性能**：docx→PDF/OFD 较重，建议异步 + 缓存。
8. **信创要求**：若面向政务，OFD 输出与国产化编辑/阅读组件是硬性项，需提前验证。

---

## 十五、修订后的结论

1. 两个 Ecology 目录是同一产品的部署态/源码态互补视图，非两套实现。
2. 收文/发文共用同一套页面与渲染引擎，差异由 `officaltype` 与环节配置驱动；发文独有套红、文号、印发，收文独有登记、来文单位、交换。
3. `flowtable` 并非正文/明细表，而是后台设置页 CSS 类与「工作流程表」数据源，迁移时需重新定义术语。
4. 套红本质是「红头模板 + 书签替换 + 正文插入 + 生成新版本」；SpringBlade 只能走服务端合成路径。
5. **正文编辑器选型（OnlyOffice / WPS Web / 自研）是整个收发文落地的核心决策**，直接决定留痕、清稿、OFD 等能力上限。
6. 除套红外，还必须纳入：**打印呈批表、清稿留痕、智能排版、范文、敏感词、OFD 国标、电子签章、转文、移动端**九个面；其中打印呈批表与清稿留痕对公文场景是刚需。
7. 表单渲染与字段权限可完全复用现有体系；主要工作量集中在正文文档体系、服务端合成引擎与上述增值子系统。
