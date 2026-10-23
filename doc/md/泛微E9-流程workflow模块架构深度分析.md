# 泛微 E9 流程（Workflow）模块架构与运作机制深度分析

> 分析对象：后端 `E:\project\ecology\ecology\src`（`com/engine/workflow`、`com/api/workflow`、`weaver/workflow`）；
> 前端 `D:\Weaver2020\ecology`（`src4js/pc4backstage/workflow`、`spa/workflow/`、`workflow/`）。
> 与《内容引擎 DocEngine 架构分析》同深度、同维度；**只读分析，未修改任何代码**。

---

## 目录

- [1. 模块组成与包结构](#1-模块组成与包结构)
- [2. 前后端职责划分](#2-前后端职责划分)
- [3. 功能入口与路由](#3-功能入口与路由)
- [4. 对外 REST 入口](#4-对外-rest-入口)
- [5. 流程引擎核心机制](#5-流程引擎核心机制)
- [6. 数据模型](#6-数据模型)
- [7. 与内容引擎、公文的依赖关系](#7-与内容引擎公文的依赖关系)
- [8. 已知问题与缺失](#8-已知问题与缺失)

---

## 1. 模块组成与包结构

流程模块采用**三层包结构**，与内容引擎 `com/engine/doc` **完全同构**（`web/service/cmd/util/entity`），体量约为内容引擎的数倍。

### 1.1 `com/engine/workflow` — E9 新版引擎

| 子包 | 职责 | 规模 |
|---|---|---|
| `cmd/` | 命令对象（一个 Cmd = 一个后端接口用例），继承 `AbstractCommonCommand` 或 `CommandUtil` | **≥500 个**；`cmd/workflowPath/` 单目录 ≥241 个 |
| `service/` + `impl/` | 服务接口 + 实现，被 Action 调用，内部再调 Cmd | **≥213 个** |
| `web/` | JAX-RS Action（方法级 `@POST/@GET/@Path`，**类上无 `@Path`**） | **81 个** |
| `biz/` | 重业务逻辑（非 Cmd） | `FormFieldSetBiz`(104 KB)、`RequestQuickSearchBiz`(108.6 KB)、`publicApi/RequestOperateBiz`(80.7 KB)、`systemBill/`、`workflowCore/` |
| `entity/` | 30+ 个 POJO/DTO（`NodeLinkEntity`、`FormFieldSetEntity`、`WorkflowToDocSettingInfoEntity`） | 纯 DTO，非 ORM |
| `constant/` | `node/NodeType`、`PageUidConst`(14 KB) | |
| `util/` | ~70 个工具类 | `WorkflowUtil`、`MatrixUtil`、`WfToDocUtil`、`CommandUtil` |

关键基类 `com/engine/workflow/util/CommandUtil.java:22` — `public abstract class CommandUtil<T> extends AbstractCommonCommand<T>`，统一提供 `writeLogContext(...)` 写 `BizLogType.WORKFLOW_ENGINE` 日志（L80-100）。

### 1.2 `com/api/workflow` — REST 对外暴露层（114 个文件）

| 子包 | 职责 |
|---|---|
| `web/` 及 `web/workflowPath/`、`web/formManage/`、`web/logDebug/` | **URL 映射空壳**：只带类级 `@Path`、继承 `com.engine.workflow.web.**` |
| `service/` | 9 个对外 Service（`RequestLogService` 96.67 KB、`RequestDeleteService` 59.95 KB、`RequestAuthenticationService` 59.96 KB） |
| `util/` | `WorkFlowSPATransMethod`(37.91 KB)、`CreateFormTableMainUtils`、`TransDatabaseTableUtils` |
| `bean/` / `constant/` | `PageTabInfo`、`WfTreeNode`；`RequestAuthenticationConstant` |

### 1.3 `weaver/workflow` — E8 遗留内核 / 真正的流程引擎（≥541 个文件）

| 子包 | 职责 | 代表类 |
|---|---|---|
| `request/` | **引擎核心（运行期）** | `RequestManager.java` **556.54 KB**（`flowNextNode()` 在第 **2711** 行）、`RequestNodeFlow.java` **404.37 KB**、`RequestCheckAddinRules.java`(121 KB)、`RequestAddShareInfo.java`(179 KB) |
| `workflow/` | **流程定义管理（设计期）** | `WFManager`、`WFMainManager`、`WFNodeMainManager`、`WfRightManager`、`WorkflowVersion`、`WorkflowBillComInfo`、`GroupDetailMatrix` |
| `action/` | 节点附加动作执行器 | `BaseAction`(60 KB)、`WorkflowActionManager`(21 KB)、`ESBAction`(74 KB)、`SapAction`(98 KB) |
| `design/` | 设计器后端 + **Flash 遗留前端源码** | 1 个 Java + **94 个 `.as`**（见 §8 M1） |
| `ruleDesign/`、`dmlaction/`、`exceldesign/`、`form/`、`field/`、`layout/`、`search/`、`report/`、`transfer/`、`imports/`、`exports/` | 规则引擎、DML 动作、Excel 表单、表单/字段、布局、查询、报表、权限转移、导入导出 | |

---

## 2. 前后端职责划分

### 前端（流程设计器 / 后台配置 UI）
- 技术栈 **React + MobX + antd + ecCom**（`src4js/pc4backstage/workflow/`，入口 `index.js` 52.77 KB）
- 职责：**只做"配置界面"**——路径列表/详情 Tab、基本信息、节点信息、出口信息、表单字段编辑、报表/查询/监控设置、流程测试、编号维护
- **不做流程运行**：运行期流转由后端 `RequestManager.flowNextNode()` 全权处理
- 产物：`spa/workflow/static4engine/engine.js`（**3.5 MB**，未拆分）

### 后端（流程引擎）
职责：定义持久化 + 实例流转 + 权限计算 + 出口条件判定 + 附加动作 + 消息/日志。

分层调用链（已核实）：
```
com/api/workflow/web/XxxAction   (@Path 类级)              ← URL 入口
      ↓ extends
com/engine/workflow/web/XxxAction(@POST @Path 方法级, 鉴权) ← 参数/权限
      ↓ ServiceUtil.getService(...)
com/engine/workflow/service/impl/XxxServiceImpl
      ↓
com/engine/workflow/cmd/.../XxxCmd (extends AbstractCommonCommand / CommandUtil)
      ↓
weaver.workflow.** (WFManager / RequestManager / RecordSet)
```
实测：`com/api/workflow/web/WorkflowBaseInfoSettingAction.java:10-13` → `com/engine/workflow/web/workflowPath/BaseInfoSettingAction.java:31-52`（`WfRightManager.hasPermission3(...)` + `HrmUserVarify.checkUserRight("WorkflowManage:All", user)`）→ `BaseInfoSettingServiceImpl` → `cmd/workflowPath/baseInfo/GetBaseInfoConditionCmd.java:39`。

### ⚠️ 重大架构事实：图形化设计器**不是 React 实现**

`src4js/pc4backstage/workflow/components/pathSet/workflowDesign/WorkflowDesign.js:42`：
```jsx
<iframe name='workflowDesign_new_name' id='workflowDesign_new_id'
  src={`/workflow/workflowDesign/readOnly.html?workflowId=${...}&backstageReadOnly=true`}/>
```
React 页面只是**包了一个 iframe**，真正的图形编辑器是 `D:\Weaver2020\ecology\workflow\workflowDesign\readOnly.html` —— 基于 **mxGraph** 的独立旧版应用，通过 `window.parent.ecCom` / `window.parent.antd` 反向借用父页组件库（L88-99）。

→ **职责边界是"React 外壳 + iframe 独立编辑器"，两套技术栈通过 `window.parent` 耦合。**

---

## 3. 功能入口与路由

**挂载根**：`src4js/pc4backstage/workflow/index.js:196` `<Route breadcrumbName="流程引擎" path="workflowengine" onEnter={getLocaleLabel} component={Home}>`。
访问形态：`/spa/workflow/static4engine/engine.html#/main/workflowengine/path/pathSet/pathDetail/flowSet/pictureEditNew?...`

| `#/workflowengine/...` | 功能 |
|---|---|
| （IndexRoute） | 流程引擎门户首页 |
| `form/formSet/{_formSet, edidForm, editField, addForm, baseInfo}` | 表单设置/编辑/字段/添加/基本信息 |
| `form/{funcLibrary, fieldTemplateLib, selectList, templateList}` | 函数库 / 字段模板库 / 公共选择框 / 高级模式模板 |
| `path/batchMaintenance/*` | 批量维护（8 个子路由：路径基础、操作者、督办、异常处理、节点功能、超时、反向维护、签字意见、高级设置） |
| `path/{typeSet, workflowChange, ruleManager, signSet}` | 类型设置 / 流程交换 / 规则管理 / 签字意见设置 |
| `path/pathSet/pathList` | 路径列表（IndexRedirect 目标） |
| `path/pathSet/pathDetail/baseSet/*` | 路径基础设置（baseInfo/remindSet/functionSet/annexSet/signOption/customPage） |
| `path/pathSet/pathDetail/formManage/*` | 表单管理（baseInfo/editField/fieldGroup/fieldShowName/rowFieldRule/columnFieldRule） |
| **`path/pathSet/pathDetail/flowSet/pictureEdit(New)`** | **图形编辑（iframe→mxGraph）** |
| `path/pathSet/pathDetail/flowSet/{nodeInfo, nodeLinkInfo}` | 节点信息 / 出口信息 |
| `path/pathSet/pathDetail/advanceSet/*` | 高级设置（14 项：FunctionManager/WorkflowPlan/WorkflowToMode/NumberSet/SuperviseSet/**WorkflowCreateDoc**/SubWorkflow/LinkAgeViewAttr/DataInput/WorkflowToDoc/WfToScheduleList/BrowserData/ReportSet/OnlineSign） |
| `path/codeMaintenance/*`、`path/test/*` | 编号维护 / 流程测试 |
| `path/{wfSetting, reverseControl}` | 应用设置 / 反向维护 |
| `report/*`、`search/*`、`monitor/*` | 报表设置 / 自定义查询 / 监控管理 |
| `userDefault`、`workflowLog/:logType`、`workflowFormula/*` | 个性化 / 日志 / 公式 |
| `interface` | **空路由（死路由）** `index.js:365-366` |

**设计器页面组成**：
- `pathSet/mainFrame/PathSet.js` → `PathList.js` → `PathBaseTab.js` → `PathDetailTab.js`
- 固定流程：`workflowDesign/WorkflowDesign.js`（React 壳）+ iframe 内 `workflow/workflowDesign/{readOnly.html, js/main.js, design/js/workflowDesign_wev8.js}`
- 自由流程（**React 原生，两套完全不同的实现**）：`pathSet/workflowDesign/workflowFree/WorkflowFreeContainer.js`（含 `WorkflowFreeDrawSvg`）

---

## 4. 对外 REST 入口

### ✅ 机制确认：与内容引擎完全一致

标准样板（`com/api/workflow/web/WorkflowBaseInfoSettingAction.java`，全文 13 行）：
```java
package com.api.workflow.web;
import javax.ws.rs.Path;
@Path("/workflow/baseInfoSetting")                                        // ← L10
public class WorkflowBaseInfoSettingAction
    extends com.engine.workflow.web.workflowPath.BaseInfoSettingAction {   // ← L11
}
```
文件普遍只有 **171 B ~ 368 B**（`FormPrintAction.java` 175 B、`WorkflowLabelAction.java` 174 B）。

**流程路径 / 设计器**：`/workflow/baseInfoSetting`、`/workflow/workflowPathSetting/list`、`/workflow/nodeSet`（及 `titleSet/titleCusSet/subWfSet/signSet/exceptionHandleSet/RobotNodeSet`）、`/workflow/pathSet/codeSet`、`/workflow/pathSet/browserDataDefinition`、`/workflow/pathset/advanced/{linkAgeViewAttr, datainput}`、`/workflow/advanceSet/workflowToDoc`、`/workflow/smartProcess`、`/workflow/workflowplan`、`/workflow/signInputSet`、`/workflow/OperationMenuSet`。

**表单**：`/workflow/formSetting/{baseInfo, fieldSet, list}`、`/workflow/formManage/{publicselect, fieldTemplateLib}`、`/workflow/wfexceldesign`、`/workflow/formula`（**非空壳**，13 个端点）、`/workflow/formexport`、`/workflow/formprint`。

**请求运行期**：`/workflow/createreq`、`/workflow/reqform`、`/workflow/reqlist`、`/workflow/share`（非空壳 10.9 KB）、`/workflow/delete`（含 `@Deprecated`）、`/workflow/forward`、`/workflow/freeFlow`、`/workflow/monitor`、`/workflow/supervise`、`/workflow/secondauth`、`/workflow/communication`、`/workflow/requestAttention/`、`/workflow/wfcenter`、`/workflow/mobile`、`/workflow/agent`、`/workflow/transfer`。

**其它**：`/workflow/workflowType`、`/workflow/newRule`、`/workflow/newReport`、`/workflow/reportSet`、`/workflow/monitorSet`、`/workflow/CustomQuerySetting`、`/workflow/codeMaintenance`、`/workflow/workflowImport`、`/workflow/index`、`/workflow/overtimeSet`、`/workflow/batchPrint`、`/workflow/batchToDoc`、`/workflow/logdebug`、`/workflow/wfutil`。

---

## 5. 流程引擎核心机制

### 5.1 流程定义与版本
- **定义表：`workflow_base`**（**`workflow_version` 表不存在** —— 版本通过同表自引用实现）
- `weaver/workflow/workflow/WorkflowVersion.java`：
  - 状态（L61/66/71）：`WORKFLOW_STATUS_VERSION_ACTIVE="1"`、`_TEST="2"`、`_VERSION_INACTIVE="3"`
  - 版本链（L105-110）：`select isvalid, activeVersionID FROM workflow_base where id=` + wfid
  - 版本列表（L123-127）：`SELECT distinct id, version, ... FROM workflow_base WHERE activeVersionID=? OR id=? ORDER BY ...`
  - → **同一条流程的多个版本都是 `workflow_base` 行，用 `activeVersionID` 指向"主版本"，`version` 为版本号**
- 版本实现：`cmd/workflowPath/baseInfo/{DoSaveNewVersionCmd, DoEditWFCmd}(41 KB)`、`biz/WfE8ToE9Task.java`（E8→E9 迁移）

### 5.2 节点与流转
- **节点定义**：`workflow_nodebase` + `workflow_flownode`（`WFNodeMainManager.java:363` 参数化查询）
- **节点类型枚举** `com/engine/workflow/constant/node/NodeType.java:14-21`：

| 值 | 名称 | 含义 |
|---|---|---|
| `0` | CREATE | 创建节点 |
| `1` | APPROVE | 审批节点 |
| `2` | SUBMIT | 提交节点 |
| `3` | ARCHIVE | 归档节点 |
| `4` | CIRCULATE | 自由节点-传阅 |
| `5` | WAIT | 等待节点 |
| `6` | AUTOPROCESS | 自动处理节点 |

> ⚠️ `NodeType.getNodeTypeOptions()`（L57-67）下拉选项**缺少 `4`（传阅）**。

- **流转主入口**：`weaver/workflow/request/RequestManager.java:2711` `public boolean flowNextNode()`
  - 静态判定 `isFlowNextNode(...)`（L8969）
  - 流转核心 `RequestNodeFlow.java`（404 KB，`workflow_nodelink` 32 次）
  - 出口读取 `WFLinkInfo.java`；自由流程 `WFFreeFlowManager.java`
  - 公共 API `com/engine/workflow/biz/publicApi/RequestOperateBiz.java:724` `flowNextNode(RequestInfoEntity, User, ...)`
  - 超时自动流转 `weaver/system/WrokflowOverTimeTimer.java:2057` `autoFLowNextNode(int requestid)`（**类名拼写错误 Wrokflow**）

### 5.3 出口与条件
- 表 **`workflow_nodelink`**（`WFLinkInfo` 32 处、`RequestNodeFlow` 32 处、`WFFreeFlowManager` 36 处）
- 出口 Cmd：`cmd/workflowPath/nodeLink/{GetNodeLinkCmd}(18.45 KB)、{SaveNodeLinkCmd}(11.5 KB)`
- 规则引擎（独立子系统）：`weaver/workflow/ruleDesign/RuleBusiness.java`
- DML 动作子系统：`weaver/workflow/dmlaction/`

### 5.4 操作者与权限
- **当前操作者表 `workflow_currentoperator`** —— `RequestManager.java` 中出现 **240 次**（引擎最核心表）
- 操作者矩阵：`workflow_nodegroup` / `workflow_groupdetail` / **`workflow_groupdetail_matrix`**（`GroupDetailMatrix.java`、`MatrixConvert.java`、`com/engine/workflow/util/MatrixUtil.java`）
- 权限校验：`WfRightManager.hasPermission3(wfid, 0, user, WfRightManager.OPERATION_CREATEDIR)`（见 `BaseInfoSettingAction.java:49`）

### 5.5 节点前后附加动作（Action 接口）
`weaver/interfaces/workflow/action/Action.java:11-20`：
```java
public interface Action {
    public static final String SUCCESS = "1";
    /** 失败信息；节点前附加操作返回此值将阻止流程提交 */
    public static final String FAILURE_AND_CONTINUE = "0";
    public String execute(RequestInfo request);
}
```
- 扩展 `ActionExtends.java:13`；实现类目录 `weaver/interfaces/workflow/action/` **≥75 个**（`WorkflowToDoc`、`OdocSignByKG`、`Fna*New`、`EASDBImport`、`CreateTraceDocument`…）
- 执行器 `weaver/workflow/action/` —— `BaseAction`(60 KB)、`WorkflowActionManager`(21 KB，`checkActionOnNodeOrLink(workflowid, nodeid, nodelinkid, ispreoperator, isnewsap)`）
- 配置表 **`workflowactionset`**；校验 `RequestCheckAddinRules.java`(121 KB)

### 5.6 systembill（系统单据）
- `com/engine/workflow/biz/systemBill/`：`SystemBill.java`、`SystemBillManagerBiz`、`Bill85.java`、`BillActionCfg`、`Demo_PreAction/Demo_AfterAction/Demo_LinkAction`（**疑为示例代码/死代码**）
- 各业务模块各自实现：`com/engine/{doc,hrm,fna,crm,prj,odoc,meeting,car}/systemBill|SystemBill*`
  - 内容引擎：`SystemBill28ServiceImpl`、`SystemBill241ServiceImpl`、`SystemBill200ServiceImpl`；`cmd/systembill/{SystemBill28AfterAction, Systembill241AfterAction, Systembill200AfterAction}`（**命名不一致：SystemBill vs Systembill**）

### 5.7 流程与表单
- 表 **`workflow_bill`**（`BillComInfo`、`WorkflowBillComInfo`；`workflow_bill` 在 `weaver/workflow/` 出现 ≥85 处）
- 动态明细表：`bill_*` / `formtable_main_*`，由 `com/api/workflow/util/{CreateFormTableMainUtils, DropTableUtils, DeleteTableUtils, GetTableNameUtils, TransDatabaseTableUtils, InitFormTableTriggerUtils}` 管理
- 表单子系统：`weaver/workflow/{form, field, layout, exceldesign}/`
- 表单建模（独立模块）：`mode_workflowtomodeset`

### 5.8 流程与文档 / 公文集成
- 表 `workflow_createdoc` / `workflow_createDoc`（**两种大小写写法**）
- `weaver/workflow/request/RequestDoc.java`（84.27 KB）、`RequestDocDocAddExt.java`（40 KB）、`RequestDocPropManager.java`（31 KB）
- `com/engine/workflow/util/WfToDocUtil.java`、`entity/WorkflowToDocSettingInfoEntity.java`
- Action：`weaver/interfaces/workflow/action/{WorkflowToDoc, CreateTraceDocument, CreateTraceDocumentByClear}`
- 端点 `/workflow/advanceSet/workflowToDoc`、`/workflow/batchToDoc`

---

## 6. 数据模型

| 表 | 用途 |
|---|---|
| `workflow_base` | **流程定义（含版本链）**；`id/isvalid/activeVersionID/version/versionDescription/formid/isbill/VersionCreater` |
| `workflow_type` / `workflow_Workflowtype` | 流程类型 |
| `workflow_nodebase` | 节点定义（含 `IsFreeNode`） |
| `workflow_flownode` | 流程-节点关联（`nodetype`/`nodeorder`/`viewnodeids`/`nodeattribute`/`passnum`/`nodetitle`） |
| **`workflow_nodelink`** | 出口/连线及流转条件 |
| `workflow_nodegroup` / `workflow_groupdetail` / **`workflow_groupdetail_matrix`** | 操作者组 / 明细 / 矩阵 |
| **`workflow_requestbase`** | 流程实例主表 |
| **`workflow_currentoperator`** | 当前/历史操作者（待办）—— 引擎最核心表 |
| `workflow_requestflowinfo` | 流转日志 |
| `workflowactionset` | 节点前后附加动作配置 |
| `workflow_bill` | 流程表单定义 |
| `bill_*` / `formtable_main_*` | 动态生成的业务数据表 |
| `workflow_createdoc` | 流程创建文档配置 |
| `int_BrowserbaseInfo` | 浏览按钮接口注册 |
| `mode_workflowtomodeset` | 流程转建模配置 |
| `workflow_codeSeq*` | 流水号 |

**访问方式：`RecordSet`，无 ORM** —— 统一 `weaver.conn.RecordSet`/`RecordSetTrans`/`ConnStatement`；`com/engine/workflow/entity/**` 是纯 DTO。
**两种 SQL 风格并存**：新代码参数化（`RequestBaseBiz.java:28`、`WFNodeMainManager.java:399`），老代码字符串拼接（`GroupDetailMatrix.java`、`WorkflowVersion.java:105,123`）。

---

## 7. 与内容引擎、公文的依赖关系

### 7.1 流程 → 内容引擎（Doc）
- `weaver/workflow/request/RequestDoc.java:27-31` 直接 import 文档内核：
  ```java
  import weaver.docs.docs.DocCoder;  import weaver.docs.docs.DocComInfo;
  import weaver.docs.docs.DocManager; import weaver.docs.docs.DocViewer;
  import weaver.docs.util.DocTriggerUtils;
  ```
  → 流程"存为文档"/"流程创建文档"直接调用 `weaver.docs.*`
- Doc 侧反向依赖工作流审批：`com/engine/doc/entity/DocApproveWfDetailBean.java`
- 文档侧 systembill 挂在流程 Action 机制上

### 7.2 流程 → 公文（odoc）依赖：**插件式反向注入**
公文不修改流程代码，而是通过 **`@CommandDynamicProxy` 命令动态代理**挂到流程 Cmd 上：
```java
// com/engine/plugin/odoc/command/workflowPath/SaveEditWFCmd.java:15-16
@CommandDynamicProxy(target = DoEditWFCmd.class, desc = "编辑路径设置时，保存公文的一些配置")
public class SaveEditWFCmd extends AbstractCommandProxy<Map<String, Object>> {
```
- 插件包 `com/engine/plugin/odoc/` **48 个文件**，其中 `command/` 下 44 个代理 Cmd（路径、表单、签批、右键菜单）
- 触发条件 `SaveEditWFCmd.java:24` `boolean isOdoc = "true".equals(params.get("isOdoc"));` —— **由请求参数决定是否激活公文逻辑**

### 7.3 其它模块对流程的依赖（反向）
`com/engine/{fna,hrm,crm,prj,meeting,car}/systemBill/**` 用流程 Action 机制实现业务回写。

---

## 8. 已知问题与缺失

### 🔴 严重

**S1. SQL 字符串拼接（注入风险）**
- `GroupDetailMatrix.java`：L22 `WHERE value_field='"+fieldId+"' and a.id=b.groupdetailid)`；L33 `DELETE ... WHERE groupdetailid='"+groupdetailid+"'`；**L38 `IN (" + groupdetailids + ")`（无引号拼接，注入面最大）**；L62/67 INSERT/UPDATE 同样拼接
- `WorkflowVersion.java:105-106` `"select isvalid, activeVersionID FROM workflow_base where id=" + wfid`；L123-127 同样拼接
- `WorkflowActionManager.java:284` `"update int_BrowserbaseInfo set w_enable = "+isused+" ,w_actionorder = "+actionorder+" where mark = '"+actionname+"'"`；L291 `set isenable = "+isused+" where actionid="+actionid`
- 反例（正确做法）：`WFNodeMainManager.java:399` 用 `values(?,?,...)`；`RequestBaseBiz.java:28` 用 `?` → **同模块两种风格并存，改造不彻底**

**S2. 图形化设计器 = iframe 嵌套独立旧版应用，技术栈割裂**
- `WorkflowDesign.js:42` iframe → `readOnly.html`（mxGraph）
- 跨栈通信靠 `window.parent.ecCom` / `window.parent.antd`（L88-99），注入全局 `window.wfGetLabel/wfModal/wfMessage` → **全局命名空间污染 + 强耦合**
- 全屏编辑硬编码跳转 URL（L72），含 4 个 query 参数，无法复用路由配置

**S3. 设计器存在两套完全独立实现**：固定流程 mxGraph iframe；自由流程 React 原生（`WorkflowFreeContainer.js`）。且后者 L27 遗留 `console.log(this.props,'this.props')`，L28-45 整段 return 被注释。

### 🟠 中等

**M1. 遗留 Flash（ActionScript）设计器源码仍完整保留**
- `weaver/workflow/design/workflowDesign/src/cn/com/weaver/flowdesign/**` —— **94 个 `.as`**（`MainGraph.as` 58 KB、`BaseNode.as` 64 KB、`BaseLine.as` 60 KB、`GraphUtil.as` 55 KB）
- 运行时 `readOnly.html` 已是 mxGraph JS → **这套 Flash 源码是纯死代码**（Flash 2020 年 EOL）

**M2. 巨型上帝类**
- `RequestManager.java` **556.54 KB**（`flowNextNode()` 在 2711 行）、`RequestNodeFlow.java` **404.37 KB**
- `RequestDetailImport`(185 KB)、`RequestAddShareInfo`(179 KB)、`RequestCheckAddinRules`(121 KB)
- `RequestQuickSearchBiz`(108 KB)、`FormFieldSetBiz`(104 KB)、`cmd/attention/GetSessionkeyCmd`(51.6 KB，单个 Cmd)
- 前端产物 `engine.js` **3.5 MB 未拆分**

**M3. 异常吞没 + `printStackTrace` 泛滥**
- `com/engine/workflow/web/**` 中 `e.printStackTrace()` **≥54 处**：`BaseInfoSettingAction`(34)、`NodeSettingAction`(30)、`FormFieldSettingAction`(26)、`WorkflowSmartProcessAction`(23)、`WorkflowNewRuleAction`(20)
- 只改 `api_status=false` + `api_errormsg`，**无统一日志落盘**

**M4. URL 命名不一致**
- 大小写混用：`/workflow/pathset/advanced/linkAgeViewAttr` vs `/workflow/pathSet/codeSet` vs `/workflow/pathSet/browserDataDefinition`
- 尾部多余斜杠：`/workflow/requestAttention/`、`/workflow/reportShareSetting/`
- 拼写错误：`BwrowseDataDefinitionAction`（应为 Browse）、`WrokflowOverTimeTimer`、`Systembill241AfterAction` vs `SystemBill28AfterAction`；前端 `perationMenuSet`
- **`@Path` 打在 Service 上而非 web 包**：`com/api/workflow/service/{LoadOrgResourceService:32, HrmGroupService:34, RequestForwardService:29, ScriptManagerService:27}` —— 破坏"web 层统一挂 @Path"约定

### 🟡 轻微

**L1. 前端死路由/占位**：`index.js:365-366` `interface` 空路由；L265 `component={<div>字段显示名</div>}`、L289 `component={<div>流程计划任务</div>}` 占位未实现。
**L2. 调试用路由**：L217-226 连续 10 条 `breadcrumbName="1"`~`"10"`（纯数字，明显调试遗留）。
**L3. 属性名 Typo**：L214 `beadcrumbName`（应为 breadcrumbName）→ 面包屑永远不生效。
**L4. 路由重复挂载**：`pathSet`(245/304)、`baseSet`(250/327)、`codeMaintenance`(336/367)、`subWorkflow`(284/301)、`newShowTemplate`(295/317) 各定义两次。
**L5. 死方法**：`WorkflowDesign.js:51-53` `getButtons(){ return []; }` → 右键菜单恒为空。
**L6. 节点类型下拉缺项**：`NodeType.java:57-67` 未包含 `CIRCULATE("4")`。
**L7. 硬编码**：`WorkflowDesign.js:60-61` 图片路径仍是 `/images/ecology8/...`；`readOnly.html:45` 手工版本号 `?v=20200413`；L1/L63-66 IE shim。
**L8. 同表双写法**：`workflow_createdoc` 与 `workflow_createDoc` —— 依赖数据库大小写不敏感，跨库迁移（Oracle→MySQL/达梦）有隐患。
**L9. 权限缺口嫌疑**：`com/api/workflow/web/**` 空壳子类自身无任何鉴权注解，完全依赖父类方法内部手工校验；实测 `RequestDeleteAction.java:45-68`（独立实现）未见显式权限校验分支。

---

## 附：未查到 / 无法确认项

1. **`workflow_version` 表不存在** —— 版本化通过 `workflow_base.activeVersionID` 自引用实现。
2. **未找到独立的流程引擎 ORM 层** —— 全模块 `RecordSet` 直连 SQL。
3. `workflow_requestlog`、`workflow_selectnextflow`、`workflow_nodecustommenu` 等表的建表语句未逐一核实。
4. **systembill 与"节点前后动作"的直接调用关系未确认** —— 二者同为 `Action` 机制使用方，`Demo_*` 疑为示例代码（可能是死代码）。
5. **前端路由前缀 `#/main/workflowengine` 中 `main` 的全局路由挂载点未定位** —— 仅从硬编码 URL 反推。
6. `RequestNodeFlow.java`（404 KB）全文内容检索被工具限制，**流转内部算法细节未深入**。
