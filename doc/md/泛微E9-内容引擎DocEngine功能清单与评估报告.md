# 泛微 E9 内容引擎（DocEngine）功能清单与评估报告

> 配套文档：《泛微E9-内容引擎DocEngine架构与运作机制深度分析.md》（架构、机制、调用链）
> 本文档定位：**已实现功能点的完整盘点 + 依赖关系 + 已知问题 + 改造/治理优先级**，供后续评估。
>
> 分析对象：前端 `D:\Weaver2020\ecology`；后端 `E:\project\ecology\ecology\src`
> 覆盖：后台配置域 21、内容前台域 20、文档业务域 15+ web 类、移动端 81 java、在线编辑 4 载体、检索统计 14 类、横切机制 7 项。
> **全程只读分析，未修改任何代码。**

---

## 目录

- [一、功能全景](#一功能全景)
- [二、后台配置域（21 模块）](#二后台配置域docengine-21-模块)
- [三、内容前台域（20 路由）](#三内容前台域document-20-路由)
- [四、文档业务域](#四文档业务域comapidoc--15-web-类)
- [五、移动端域](#五移动端域)
- [六、在线编辑 / 版本 / 附件 / 转换](#六在线编辑--版本--附件--转换)
- [七、检索 / 统计 / 知识中心](#七检索--统计--知识中心)
- [八、横切机制](#八横切机制)
- [九、依赖关系矩阵](#九依赖关系矩阵)
- [十、全局问题清单](#十全局问题清单按严重度)
- [十一、评估与改造优先级](#十一评估与改造优先级)

---

## 一、功能全景

| 域 | 规模 | 入口 |
|---|---|---|
| ① 后台配置域 | 21 模块 | `#/docengine/*`，`/api/doc/console/*` |
| ② 内容前台域 | 20 路由 | `#/document/*`，`/api/doc/{searchlist,doccenter,monitor,recycle,subscription,movecopy,docbatchshare}/*` |
| ③ 文档业务域 | 15+ web 类 | `#/main/document/*`，`/api/doc/{detail,save,acc,share,reply,read,praise,score,coope,out}/*` |
| ④ 移动端域 | 81 java / 7 路由 | `/api/doc/mobile/*`，`spa/document/static4mobile/` |
| ⑤ 在线编辑 / 版本 / 附件 | 4 载体 + 3 套版本 | `/api/wps/*`、`/api/doc/yozo/*`、`/api/doc/acc/*` |
| ⑥ 检索 / 统计 / 知识中心 | 14 web 类 | `/api/doc/searchlist/*`、`/api/doc/doccenter/*`、5 个统计包 |
| ⑦ 横切机制 | 7 | 缓存 / 分权 / 上传 / 敏感词 / 下载鉴权 / 索引 / 多语言 |

后端 `com/api/doc` 共 **26 个一级子包**：`call, category, center, console, departmentDocNum, detail, docCategoryStatement, docMonitoring, docRecycle, docStatement, edit, magazine, mobile, multi, news, publicMethod, readingStatement, relate, search, statementUtil, subsectionDocStatement, tab, upload, watermark, wps, yozo`。

---

## 二、后台配置域（docengine，21 模块）

> 统一机制：`com/api/doc/console/**/web/XxxAction` 是**只带 `@Path` 的空壳**，继承 `com/engine/doc/web/XxxAction`（含业务逻辑）；URL = `/api/` + 类级路径 + 方法路径。

| # | 模块 | 作用 | 输入 | 输出 / 写表 | 核心逻辑 | 依赖 | 已知问题 |
|---|---|---|---|---|---|---|---|
| 1 | 目录-基本信息 | 目录属性编辑（名/编码/上级/分部/开关/水印/加密/认证/提醒/推送图） | `id,parentid,categoryname,coder,subcompanyid,orderable,pushoperation,wmimportset,isopenwater,sendmessageimg,minuploadfilesize,replyremind,allowoutshare,isencryptshare,encryptrange,isenablesecondauth…` | `DocSecCategory`(存储过程 `Doc_Sec_Update_for_safe`)、`docwatermarksecsetting`、`CategorySendMessahgimg`、`remindfornewDoc`、`docdetail`(orderable) | 权限→重名校验→主更新→各子项分写→清 4 类缓存 | `CheckPermission`/`RightUtil`/`WaterMarkUtil`/4 个 ComInfo/`DocTriggerUtils` | `updateMainOrSubCategory()` **方法体被注释**；FTP 分支被注释；**无事务** |
| 2 | 目录-下级目录 | 下级目录列表 + 新建 | `{parentid, subCompanyId}` | `DocMainCategory/DocSubCategory/DocSecCategory` + `DocSecCategoryApproveWfDetail`/`docwatermarksecsetting`/`DocSecCategoryShare`/`DocSecCatFTPConfig` | 权限（parentid>0 走父，否则按分部）→重名校验→`SecCategoryManager` 建目录 | `SecCategoryManager`/`MultiAclManager` | 无用 import；失败无回滚 |
| 3 | 目录-编号规则 | 编码成员、子文档/子目录/日期单独流水 | `id,codeid,memberList,secDocMemberList,userUse,secDocCodeAlone,secCategorySeqAlone,dateSeqAlone` | `codemain`、`codedetail`(先删后插)、`DocSecCategoryCoderSeq` | 权限→更新 codemain→清 codedetail→插成员→序列行（`-1` 为全局行） | `SecCategoryComInfo` | 先删后插异常丢规则；全局行并发竞争；`getLogContext` **语义写反** |
| 4 | 目录-版本管理 | 版本开关/前缀/读者看历史/保留附件 | `id,editionisopen,editionprefix,readercanviewhistoryedition,isnotdelhisatt` | `DocSecCategory` | 权限→update→开启时强制 `updatedocsend=1` | `SecCategoryComInfo`/`MultiAclManager` | **只有 POST 无 GET**，前端只能复用 basicInfo |
| 5 | 目录-文档属性 | 属性可见性/列宽/必填/自定义字段挂载 | `secCategoryId,datas(JSON)` | `DocSecCategoryDocProperty`；必填同步 `cus_formfield` | 权限→逐条 update/insert（viewindex 受密级影响）→清 3 类缓存 | `SecCategoryDocPropertiesComInfo` | 保存**不处理删除行**；`"NULL"` 字符串兜底 |
| 6 | 目录-自定义列表 | 列表显示哪些属性、是否作查询条件 | `id,useCustomSearch,datas(JSON)` | `DocSecCategory.useCustomSearch`、`DocSecCategoryCusSearch` | 按 `(secCategoryId, docPropertyId)` 存在性决定写 | `SecCategoryCustomSearchComInfo` | 与 #5 viewindex 逻辑重复维护 |
| 7 | 目录-文档审批 | 生效/失效审批流、批准工作流、字段映射 | `id,isOpenApproveWf(1/2),validityWfId,invalidityWfId,approveWorkflowId`；明细 `secCategoryId,approveType,workflowId,workflowFieldId,docPropertyFieldId` | `DocSecCategory`、`DocSecCategoryApproveWfDetail` | 取历史流程→变更时清旧明细→更新主表 | `SecCategoryComInfo`、`DocApproveWfDetailBean` | 明细**字符串拼 SQL**（注入风险）；无事务 |
| 8 | 目录导入导出 | Excel 批量导入、历史/失败明细、导出 | 导入 multipart + `subcompanyid`；导出 `ids,isinclude` | `docseccategoryimporthistory`、`docseccategoryimportfaildetail`；导出 `/docs/category/tempfile/{ts}.xls` | 权限 `ADD_RIGHT`→分权分部→`DocSeccategoryUtil` | `weaver.docs.docs.DocSeccategoryUtil` | 4 处 `importExcel>>>>` **调试日志未清理**；导出文件无清理 |
| 9 | 批量维护（5 子页） | 多目录批量改 基本信息/权限/编号/版本/模板 | `ids`(逗号)、`isHaveChild`、`oper`(1追加/2覆盖)、`data`(JSON)、`code` | `docseccategory`、`remindfornewDoc`、`docwatermarksecsetting`、`CategorySendMessahgimg`、`DirAccessControlList(+Detail)`、`DocSecCategoryShare`、`codemain/codedetail`、`DocSecCategoryMould(+BookMark)`、`workflow_docshow` | `takeChildIds` 展开→字段白名单拼 SQL→`getSubINClause`→执行→触发器→清缓存→逐目录写提醒 | `DocTriggerUtils`/`WaterMarkUtil`/`MultiAclManager` | 白名单需改两处；`IN` ids 多时超长；**无事务**；5 个 store 口径易不一致 |
| 10 | 虚拟目录 | 树状文档字段维护 | `id,superiorFieldId,treeDocFieldName,showOrder,treeDocFieldDesc` | `DocTreeDocField` | 自引用校验→权限 `DummyCata:Maint`→重名/超 10 层校验→算 `allSuperiorFieldId/fieldLevel`→新增或更新→拼音触发器 | `DocTreeDocFieldComInfo`/`DocTriggerUtils` | SQL 全拼串；`AddVirtualFolderCmd` **疑似死代码** |
| 11 | 目录模板 | 模板列表 + 删除（存为/套用在基本信息页） | `subCompanyId`、`mouldId` | `DocSecCategoryTemplate`；删除级联 12 张表 | 权限 `DocSecCategoryMould:all`→分权分部→SplitTable | `KnowledgeTransMethod`/`DocTriggerUtils` | 删除 `id in(` **无数字校验**；**不清理** `docwatermarksecsetting`/`CategorySendMessahgimg`（残留） |
| 12 | 应用设置（6 组） | 附加信息/操作菜单/回收站/历史共享时间/水印/Tab 角色/下载限制 | 各组参数 | `docapplysetting`、`DOC_PROP`、`HRMDOCROLE`、水印表 | 6 个 Action 各自读写 | `DocApplySettingUtil`/`WaterMarkUtil` | `saveDocTabRole` **保存用 GET**；`HRMDOCROLE` delete **无 where**；`DocTabInfoRoleShowCmd` 丢弃 params |
| 13 | 网盘设置 | 个人网盘配额 | `userid,disksize` | `DocNetdiskSizeSet`；白名单读 `doc_netdisk_setting.properties` | `getlist/addlist/deletelist` 需 `user.isAdmin()` | `cmd/NetdiskSetting/*` | `deletelist` ids 拼串；`getbasicinfo` **缺 admin 校验**；白名单只读 |
| 14 | 字段管理 | 自定义字段维护（含选项） | `fieldhtmltype`(1-5)、`type`、`fieldname`、选项 | `cus_formfield`、`cus_selectitem` | 生成 fieldid→写定义+选项 | `DocCustomFieldServiceImpl` | 选项拼串；**删字段不校验引用**→悬挂引用 |
| 15 | 内容日志（3） | 下载/阅读/修改日志 | `creater,department,imagename,fromdate,todate,resourceId,subCompanyId` | 只读：`DownloadLog`、`DocDetailLog` | 分权→权限 `LogView:View`→日期换算→SplitTable | `CheckSubCompanyRight`/`HrmSanyuanAdminBiz` | 三个 cmd 各 15KB **高度重复**；三元管理员语义不一致 |
| 16 | multi 批量上传 | 批量上传文档 | multipart + 配置 | 经 `DocManager.UploadFileToDoc` 写 `DocDetail`/附件表 | 权限 `MultiDocUpload:maint` | `DocMultiInitCmd`(25.9KB) | 单文件 25.9KB 难维护 |
| 17 | multi 调整共享 | 批量改共享 | `ids`、`oper` | `DocShare`(delete 后重建) | 权限 `DocShareRight:all` | `DocBatchShareEntireCmd` | `successIds` 拼 `in(...)` |
| 18 | multi 弹窗设置 | 弹窗提醒配置 | `docid` 等 | `DocPopUpInfo`、`DocPopUpUser` | 权限 `Docs:SetPopUp` | `DocPopUpSetCmd` | SQL 全拼串 |
| 19 | multi 回收站 | 回收站列表/恢复/彻底删除 | `operation,docid` | 回收站表 | **权限 `DocumentRecycle:All` 已被注释** | `DocRecycleService` | 🔴 **任何人可调用** |
| 20 | 新闻三件套 | 类型/新闻页/图库 | 类型 `name/desc/dspNum`；新闻页 `frontpagename,isactive,newsperpage,defnewspicid,importdocid,publishtype,newclause,subcompanyid…` | `newstype`、`DocFrontpage`、`DocPicUpload` | 权限 + 分权复核 | `weaver.docs.news.DocNewsManager` | 权限串 `Add` vs `add` **大小写不一致**；图片删除未清理 `backgroundpicid` |
| 21 | 期刊 | 分类→刊号→明细 CRUD | 各层 id/name/docID | `WebMagazineType`/`WebMagazine`/`WebMagazineDetail` | 14 端点，service→15 cmd | `DocMagazineServiceImpl` | 🔴 **预览接口不可达**（`DocMagazineWebAction./preview` 无 api 子类） |

**补充 cmd 包**：`doctabinfo/`(Tab 角色)、`docApplySetting/`(附加信息/菜单)、`maintaince/`(批量维护全部)、`mobileSetting/`(移动端 Tab/默认目录)、`transfer/`(权限移交浏览，只读)、`updateHisDocShareTime/`(历史共享时间)、`systembill/`(流程节点 Action：`SystemBill28` 文档审批、`Systembill241` 投票共享、**`Systembill200` 方法体为空占位**)。

**首页概览**：只读统计（`docseccategory/DocMould/DocMouldFile/WebMagazinetype/DocFrontPage/DocDetail/SysMaintenanceLog`）；`maindir/subdir` 声明未赋值。
**external**：3 个设置弹窗经 `window.weaDocEngine.com.*` 暴露，被 `smallApp/ecAndEM/appmanager` 引用；`SystemDocSetting` 渲染被注释。

---

## 三、内容前台域（document，20 路由）

**公共底座**：`ComsStore`（`stores/tables/common.js`）→ `doSearch/getTreeDatas/getTreeNums/getRightMenu/getConditionInfo`；条件字段、右键菜单、表格列**全部后端下发**（`RightMenuType.java` 约 80 个 `BTN_*`）；统一 `WeaRightMenu + WeaTop + WeaTab + WeaTable`。

| 路由 | 作用 | 输入 | 输出 / 写表 | 接口前缀 | 已知问题 |
|---|---|---|---|---|---|
| `add` | 目录选择器（跳新建） | `?isFav/isCommon/openNewWindow/secid` | 跳 `linkUrl?domKey=secid+params+linkRout` | `/api/doc/category/*`、`/api/doc/save/*` | `getEditSet/getParams` 声明未调用 |
| `center` | 知识中心首页 | Tab、`docsubject`、分页 | 统计 3 + 排行 3 + 最近读者 + 列表 + 上传 | `/api/doc/doccenter/*`(12) | localStorage 缓存**无失效** |
| `count` | 阅读量统计 | `doccreatedateselect`(7/6)、`deptType`(2/3/4)、`statusType` | 3 KPI + 柱状图 | `/api/doc/readstatement/*` | 误置 `loadingChart`；任一空则整块不显示 |
| `catalog` | 目录数量 | 同 count | 3 数字 + 图 | `/api/doc/categoryReport/*` | 右键菜单**写死假菜单**；className 遗留 |
| `docofauthor` | 著者文档数 | `?deptType/branch_id/dept_id` | 著者榜 + 创建数 + 最多目录 + 图 | `/api/doc/report/*` | `reportPeopleData` 取了未渲染 |
| `docofdepartment` | 部门文档数 | 同上 | 同上 | `/api/doc/deptDocReport/*` | — |
| `docofsubdepartment` | 分部文档数 | 同上 | 同上 | `/api/doc/subDocStatement/*` | `reportDate` **笔误**（应 reportData） |
| `monitor` | 文档监控（8 操作） | 组织树 + 目录树 + 条件 + 多选 | `deleteDoc/publishDoc/archiveDoc/invalidDoc/cancelDoc/reopenFromArchiveDoc/reopenFromCancelDoc/强制签入` | `/api/doc/monitor/*` + `/api/doc/operate/*` | `componentWillReceiveProps` **无 location.key**；`setLoading` 写成 `setTitle`；失败无提示 |
| `dummy` | 虚拟目录维护 | 虚拟目录树 + 搜索 + 多选 | 复制/移动/移出/导入 | `/api/doc/dummydoc/*`、`/api/doc/operate/*OfDummy` | `removeOfDummy` `.then` **空函数** |
| `search` | 查阅文档（核心） | `?secid/getMost/jsonstr/menuidforportal/viewcondition`；条件 100% 后端下发 | sessionkey→`WeaTable`；缩略图视图 | `/api/doc/searchlist/*`(13) + `/api/doc/operate/*` | 🔴 `myDoc/latestDoc/allDoc.js` **不存在**却被 import；`Search.js:721` **未定义变量**；`viewcondition>2` 串台；**导出完全未实现** |
| `move` | 移动/复制 | 源/目标目录 + 字段映射 + 多选 | `{sourceId,targetId,docids,operate,refids}` | `/api/doc/movecopy/*` | 第一参是"用哪个接口"非"移动/复制"，易误读 |
| `share` | 批量共享 | 条件→搜索→多选→弹窗 | 写 `DocShare`+`ShareinnerDoc/ShareouterDoc` | `/api/doc/docbatchshare/*`、`/api/doc/share/*` | 🔴 `eval(params.shareDatas)`；`dropMenuProps` 传函数 |
| `docRecycle` | 回收站 | `hasRight`、树、条件、多选 | 恢复/彻底删除 | `/api/doc/recycle/*` | `Synergy` import 被注释 |
| `subscribe` | 知识订阅 | 4 Tab（3/0/1/2）+ 多选 | 写 `DocSubscribe` + 权限明细 | `/api/doc/subscription/*` | 用 `api_status===true`，**协议不一致** |
| `news` | 新闻中心 | `?publishType=&id=` | 只读 outer/list/content | `/api/doc/news/*` | 导出 `com:{News}`；未挂 Synergy |
| `more` | 门户元素/登录前 | `?outDoc=1`、`?jsonstr=`、`?tabid/eid`、`?isNew=1` | 只读列表 + 删除 | `/api/doc/more/*`、`/api/doc/out/more/*` | 把 **`setIsNew` 函数当请求参数传** |
| `knowledgeEdit` | 知识点编辑 | `knowledgeId` | 写 `KT_*` | `/api/knowledge/knowledge/*` | 路由**缺 onEnter** |
| `knowledgeList` | 知识点维护 | — | 列表 | 同上 | 🔴 `mobx.toJS` **未 import** |
| `KnowledgeMapList` | 知识地图维护 | — | 列表 | `/api/knowledge/knowledgeMap/*` | `KnowledgeMapEdit.js`(41KB) **无路由** |
| `netdisk` | 个人网盘 | `?type=`、`?viewType=` | 表格/图标视图 | `/api/doc/netdisk/*` | 🔴 **基本是 Mock**（假数据 + 写死 fileid） |

**可见性裁剪（统一）** — `DocShareUtil.getOnlyHasRight()`：
```
usertype==1 → ShareinnerDoc；否则 ShareouterDoc
→ ShareManager.getSqlWhereList("doc", …) 多条条件
→ union all SELECT sourceid … group by sourceid      ← 我有权的 docid 集合
→ select t1.* from DocDetail t1, (子查询) s where t1.id=s.sourceid <业务条件> group by …
```
目录树用 `getHasRightOfCategory`（只查 `sec_category`）。写侧 `DocShareUtil.docSave(..., syncToDetail)` 同步物化表。

**三套 SPA 代码级耦合**：后台 `stores/multi/*` **直接 import 并继承前台 store**（`ShareOperationStore extends DocShareStore`、`RecycleStore`、`MultiShareStore`、`PopupSetStore`）；`documentDetail` 复用前台 `Portal/Portal.js` 的 `Synergy`。

**门户集成**：`Synergy` 已挂 8 页，未挂 12 页；`portal/constants/Synergy.js` 维护路由→老版 JSP 映射；门户元素主要落 `more`（Base64 `jsonstr`）；`search` 被门户快捷入口引用（`viewcondition=0/1/2`）。

---

## 四、文档业务域（`com/api/doc/**`，15+ web 类）

| Web 类（`@Path`） | 端点 | 作用 | 依赖 | 已知问题 |
|---|---|---|---|---|
| `DocDetailAction` `/doc/detail` | 13 | `basicInfo/htmlContent/tabInfo/docParamInfo/docAutoExtendInfo/docChilds/docVersion/rightMenu/getTopSet/setTop/invalidate/publishDoc/reOpen/reLoad/updateBizStateForDoc` | `DocDetailService`/`WaterMarkUtil`/`DocSecretLevelUtil` | 🔴 **`setTop/invalidate/publishDoc/reOpen/reLoad` 全是 GET 写操作**；3 个参数读后硬编码传 1；`user` 未判空 |
| `DocSaveAction` `/doc/save` | 13 | `save/filterResource/submit/afterSave/getEditSet/getParams/getMouldContent/isExists/getAccListForEdit/fileTmp/basicInfo/isCheckOut/checkIn/accForDoc/addshare` | `DocSaveService`/`EditConfigUtil` | 🔴 **L74-85、L167-178 权限校验被整段注释**（最高优先级） |
| `DocAcction` `/doc/acc`（类名拼错） | 22 | 附件列表/版本/批量下载/替换/签入/保存/`convertFile`/`getAccFileConvert`/删除/重命名/`loadFromWps`/`loadFromYoZo`/FTP×3/`importToHtml`/base64 | `DocAccService`/`ImageConvertUtil`/`ConvertPDFTools` | 🔴 永中端点 4 处 **取错 Map**（错误恒 null）；`docAccSearch` 只回搜索框；6 个端点 `@Deprecated` 且与 `/doc/yozo/*` 重复 |
| `DocReplyAction` `/doc/reply` | 6 | 回复数/列表/子回复/更多/保存/删除 | `DocReplyService` | `deleteReply` GET 写 |
| `DocReadAction` `/doc/read` | 3 | 阅读数/写日志/协同日志 | `DocLogService` | GET 写 |
| `DocPraiseAction` `/doc/praise` | 3 | 点赞信息/点赞/取消 | `DocPraiseService` | GET 写 |
| `DocScoreAction` `/doc/score` | 3 | 评分/打分/详情 | `DocScoreService` | GET 写 |
| `DocCoopeEditAction` `/doc/coope` | 13 | 协同设置/分享增删查/保存/上传/judge/send(EM)/mobile judge/改标题/复制 | `DocCoopereateUtil`(47KB)/`DocCoopeEM7Util` | **PC 无入口**（仅移动端）；`modify/docsubject` 拼 docid |
| `SensitiveWordAction` `/doc/sensitiveword` | 4 | 日志/WPS 校验/**预览校验**/按文件校验 | `WordProcessUtil` | 返回 `CommonResult`，**协议不一致** |
| `DocOutDetailAction` `/doc/out/detail` | 4 | 外发详情（免登录） | `DocSptm`(DES) | 🔴 **全端点无 user 校验 + 无有效期** |
| `DocSearchAction` `/doc/docsearch` | 4 | 列表（**微搜分支已注释**）/条件/树/右键 | `DocSearchService` | 微搜通道注释 |
| `DocShareAction` `/doc/share` | 7 | 共享 CRUD + 批量 + 历史处理 | `ShareDocServiceImpl` | `addShare/deleteShare` GET 写 |
| `SearchListAction` `/doc/searchlist` | 11 | 列表（双通道）/条件/树/树计数/右键/`tabInfo`/**保存·删除条件模板**/默认树/新建链接 | `ConditionUtil`/`CategoryTree` | 🔴 `tabInfo` 每次 `insert doc_user_introstep` **无去重** |
| `SubscriptionAction` `/doc/subscription` | 6 | 订阅列表/条件/4 Tab/右键/批准·退回·收回·申请/树 | `DocSearchService` | `optionType` GET 写 |
| `DocMonitoringAction` `/doc/monitor` | 7 | 列表/执行 8 操作/条件/操作类型/右键/树/分权 | `DocMonitoringService` | `/list` **无 try**，异常返回 `"null"`；ids 拼 `IN` |
| `DocRecycleAction` `/doc/recycle` | 6 | 列表/开关/恢复·删除/条件/右键/树 | `DocRecycleService` | 未判 `rs.next()`；批量失败被吞 |
| `FileUploadAction` `/doc/upload` | 12 | 上传/Base64/替换正文/转文档/移动端/EM/按URL/共享/Word导入/挂docid/图片类型/后缀/密级 | `EditConfigUtil`/`DocSecretLevelUtil` | 🔴 **L849 `where id=` 缺 `?`、L884 `ishistory` 缺 `=?`**；敏感词检测 4 处复制 |
| `WaterMarkAction` `/doc/watermark` | 3 | 水印文本参数/白名单/黑名单 | `WaterMarkServiceImpl` | 异常日志三处同一字符串 |
| `DocCenterAction` `/doc/doccenter` | 13 | 个人信息/文档数(6)/评论点赞/排行榜(5)/最近读者/推荐阅读/伙伴在看/点赞/上传配置 | `dbsearch`(19)/`fullsearch`(4) | 🔴 `getPraise` **praiseId 误传 docid**；`case "3"` **缺 break** |
| `CreateDocCategoryAction` `/doc/category` | 4 | 新建目录树/有权限账号(主次账号)/全部目录/收藏 | `CategoryService`/`MultiAclManager` | 🔴 `getTreeNodeById` **未 add** → `treeNodeAll` 恒空 |
| 列表四件套 | — | `MyDocs/LatestDoc/DocRank/DummyDoc/DocCategory/DocMore/DocOutMore/DocLog/DocTableOperate` | — | 全 GET 写；`exprot` 拼写错 |
| 统计 5 类 | 各 5 | `readingStatement`/`docStatement`/`docCategoryStatement`/`departmentDocNum`/`subsectionDocStatement` | 各自 Service | 5 类**结构完全重复**；分部 service 类级 new（非线程安全）；签名不一致 |
| `PublicMethodAction` | 1 | 给指定人加共享（供他模块复用） | `AddShareForUserCmd` | GET 写；类名小写开头 |
| `WpsController` `/doc/wps/sign` | 7 | WPS 开放平台：下载×2/签名/wxConfig/上传/订阅校验/保存回调 | `GraphService`/`SignService`/`AppSdkUtil` | 🔴 **L65/L93 硬编码域名替换**（项目残留）；`download/download1` 重复 |
| `DocCenterAction` `/wps/doccenter/outter` | 10 | WPS 文档中心 info/user/online/save/version/rename/history/onnotify/callback/ping | 11 Cmd | 🔴 **`online/onnotify/cps/callback` 被 `if(Boolean.TRUE)` 短路** |
| `WebOfficeFileAction` `/v1/3rd/file` 等 | 7 | WPS 旧协议 + user/oauth | `WebOfficeServiceImpl` | `online` **方法体为空**；路由重复 |
| `YozoWebOfficeAction` `/doc/yozo` | 6 | 永中：下载/上传/打开/保存/关闭/状态 | `YozoWebOfficeUtil` | 与 `DocAcction` 重复端点 |
| `DocBatchShareAction`/`DocCopyMoveAction` | 11 | 批量共享(强制 sharelevel=3)/移动复制 | — | 移动复制 GET |

---

## 五、移动端域

`SystemDocAction`（`@Path("/doc/mobile/systemDoc")`）**34 端点**，统一 `getUser → request2Map → SystemDocServiceImpl(commandExecutor) → JSON`，异常统一 `{api_status:false,msg:"error"}（未国际化）`。

| 分组 | 端点 | 输出 / 表 |
|---|---|---|
| 目录树/列表（8） | `getAllDocList/getMyDocList/getCollectDocList/getCategoryList/getMyCategoryList/getCategoryDocList/categoryBrowser/searchListCondition` | `DocDetail`+权限；收藏 `SysFavourite` |
| 详情（4） | `getDocDetail`(Cmd 40KB)/`getParams`/`userAndCategory`/`filetodocdetail` | 版本/附件/权限 |
| 新建/编辑/提交（~6） | `getCategoryParams/saveDoc/submitDoc/afterSave/editPermission/getEditPermission` | `DocDetail` |
| 附件/版本（~5） | `getAccList/getDocVersionInfo/saveOffice/downloadParams` | `DocImageFile` |
| 回复/点赞（4） | `getReply/replyDoc/deleteReply/praiseDoc` | `doc_reply`/`PRAISE_INFO` |
| 外发分享（13） | `DocOutShareAction` 全套 | `DocoutShareManager`(87KB) |
| 自定义 Tab（2） | `getUserChooseTab/setUserChooseTab` | — |

前端**独立 SPA**：`spa/document/static4mobile/`（PWA，含 service-worker），源码 `src4js/mobile4mobx/document/`，7 路由、54 js，依赖 `weaver-mobile` 而非 ecCom。
> 已知：`userAndCategory` 默认目录**写死 `int secid = 16`**。

---

## 六、在线编辑 / 版本 / 附件 / 转换

| 载体 | 入口 | 输入 → 输出 | 状态 |
|---|---|---|---|
| WPS 文档中心 | `/api/wps/doccenter/outter/v1/3rd/*` | `file_id, sence(edit/view/format)` → `{file:{…,user_acl}, user}`；`save` 写 `imagefile`(comefrom=`wps_doccenter_edit`)+`wps_doccenter_fileinfo` | `online/onnotify/callback` **被短路** |
| WPS 旧协议 | `/api/v1/3rd/file/*` | `_w_fileid/_w_userid` | `online` 空实现 |
| 永中 YoZo | `/api/doc/yozo/*` | `fileid/filePath/saveFlag`；配置 `doc_yozo_for_weaver` | 与 DocAcction 重复 |
| 金格 iWebOffice | OCX（非 REST 主链） | `IWebOfficeConf` 读 `iweboffice_config.clientType` | 仅 IE/国产，落点模板编辑 |

**三套版本体系**：① 文档级 `DocDetail.doceditionid/docedition/isHistory`；② 附件级 `DocImageFile.versionid`；③ WPS 级 `wps_doccenter_fileinfo.versionid`。**`docversion` 表不存在**。
**签出/签入**：`DocDetail` 自身 5 字段，**无独立锁表、无超时释放**。
**转换**：`/api/doc/acc/convertFile` → 敏感词高亮 → `canViewForWps()` ? `WebOfficeUtil.convert` : 本系统（`pdf_imagefile`/`DocPreviewHtmlManager`）。

---

## 七、检索 / 统计 / 知识中心

- **开关**：`DocSearchService.useFullSearch()`（读 `doc_full_search.use_full_search` + 微搜探活）。
- **双通道**：`dbsearch` 19 个（SQL + `DocShareUtil` 子查询）；`fullsearch` 4 个（微搜）。
- **微搜**：**非 ES/Lucene**，独立 **RMI 服务**（`SearchRmi`/`SearchRmiService`）。
- **条件模板**：`DocConditionMould` + `docConditionDetail`（事务批量），仅创建人可改。
- **统计 5 类**：阅读量 / 著作 / 目录 / 部门 / 分部（结构完全重复的 5 套）。

---

## 八、横切机制

| 机制 | 实现 | 关键结论 |
|---|---|---|
| 缓存 | `CacheBase` + `DocComInfo`(key=docid) + `SecCategoryComInfo` | **目录改配置未刷缓存 → 权限/下载/大小限制用旧值** |
| 分权 | `SystemSet`(docdetachable/docdftsubcomid) + `SysRoleSubcomRight` + `DocSecCategory.subcompanyid` | 影响目录树/回收站/权限列表/分部下拉 |
| 上传 | `FileUploadAction` + `FileSuffixCheckUtil`（全局 ∩ 目录） | 默认 5MB；**无病毒扫描**；`imagefiletemp` 超 4h 清理 |
| 敏感词 | 上传（命中即删）/预览/WPS编辑/日志 | **下载不校验**（`FileDownload` 在白名单） |
| 下载鉴权 | `DocDownloadCheckUtil` 只是 **DES 混淆**（`file_download_permission` uuid）；真鉴权在 `FileDownload.getWhetherHasRight` | 类名误导 |
| 全文索引 | 异步写 `indexupdatelog`(DONEFLAG=0) | **无失败重试**，仅微搜未开时 truncate |
| 多语言 | `HtmlLabelInfo`+`HtmlLabelIndex`；`WeaLocaleProvider.getLabel(id,兜底)` | **文档内容级多语言不存在** |

---

## 九、依赖关系矩阵

```
                    ┌──────────────┐
                    │  DocDetail   │ 文档主表（一切的中心）
                    └──────┬───────┘
        ┌──────────────┬───┴────┬──────────────┬──────────────┐
   DocSecCategory   DocImageFile  DocShare   DocDetailLog  cus_fielddata
   (目录/属性/权限)  (附件/版本)  (共享)     (日志)         (自定义字段值)
        │               │           │
   DirAccessControl  imagefile   ShareinnerDoc ← 物化权限（查询侧裁剪）
   DocSecCategoryShare          ↑
        │                    DocShareUtil.getOnlyHasRight()
   DocSecCategoryTemplate       （所有前台列表统一注入）
   (目录模板，级联 12 张表)
```

**跨模块**：`com.engine.doc`（配置）← `com.api.doc.console`（URL 壳）；`weaver.docs.*`（经典层：`DocManager`/`DocImageManager`/`MouldManager`/`DocNewsManager`）；`com.weaver.procedure.*`（存储过程）；流程（`bill_Approve`/`workflow_requestbase`/`workflow_createdoc`/`systembill` Action）；公文 odoc **复用** docengine 的模板与字段页。

---

## 十、全局问题清单（按严重度）

### 🔴 严重（安全 / 断链 / 数据正确性）

1. **`DocSaveAction` L74-85、L167-178 权限校验被整段注释** → 保存文档无鉴权。
2. **字段加密静默失效**：写入 scope 传 `HrmCustomFieldByInfoType`，配置按 `DocCustomFieldBySecCategory` → 加密被跳过、**明文入库**（读取恰好返回明文故无表象）。
3. `stores/tables/myDoc.js`、`latestDoc.js`、`allDoc.js` **文件不存在**却被 import 实例化。
4. **`eval(params.shareDatas)`**（`stores/tables/docShare.js:84`）。
5. 外发**无 token、无有效期**；`DocOutDetailAction` 全端点无 user 校验。
6. 回收站权限 `DocumentRecycle:All` **被注释**，任何人可调用。
7. `FileUploadAction` L849/L884 **UPDATE 缺 `=?`**。
8. `netdisk` 全模块**硬编码 Mock**。
9. `WpsController` L65/L93 **硬编码项目域名替换**。

### 🟠 功能缺失 / 不可达

10. **导出完全未实现**（后端有 `BTN_EXPORT`(28343)/`BTN_EXPORT_LOG`，前端无分支 → 僵尸按钮）。
11. **期刊预览不可达**（`DocMagazineWebAction./preview` 无 api 子类）。
12. `CategoryService.getTreeNodeById` 未 add → `treeNodeAll` 恒空。
13. WPS 文档中心 `online/onnotify/cps/callback` 被短路。
14. `ref`/`children` Tab 被 `if(false)` 关闭；`relate/*` 名不副实（实为组织画像）。
15. `KnowledgeMapEdit.js`(41KB) 已实现**无路由**；3 个 knowledge 路由缺 `onEnter`。
16. `DocAcction.docAccSearch` 只回搜索框定义。
17. 死代码：`updateMainOrSubCategory`、`AddVirtualFolderCmd`、`Systembill200AfterAction`、`SecCategoryAddAction` 空壳。

### 🟠 协议 / 规范

18. **写操作滥用 GET**：`setTop/invalidate/publishDoc/reOpen/reLoad/checkIn/accForDoc/fileTmp/docAccReplace/delete/doPraise/doMarkDoc/addShare/deleteShare/saveConditionMould/doOperate/recoveryDoc/collute/moveOrCopy/…`
19. SQL 注入面广（批量维护 IN、虚拟目录、弹窗、模板删除、字段选项、网盘删除均拼串）。
20. 返回协议不统一（`CommonResult` vs `api_status`；`subscribe` 用 `api_status===true`；`multi/*` 用字符串 `"-1"`）。
21. `@Path` 前导斜杠缺失（console 下 18 个类），依赖容器容错。
22. 拼写：`DocAcction`、`exprot`、`reportDate`、`publicMethodService`。

### 🟡 质量 / 维护

23. 无事务的多表写（基本信息、批量维护、模板存为/导入）。
24. 重复代码（3 个日志 cmd 各 15KB、5 个统计 Action、mobileSetting 两对 cmd、永中端点双份、WPS 3rd 双份）。
25. 调试日志未清理（`importExcel>>>>`、`window.develop`）。
26. 运行时隐患（`Search.js:721` 未定义变量、`KnowledgeList` mobx 未 import、`netdisk` 无 query 崩溃、monitor/move 的 `componentWillReceiveProps` 无 `location.key`）。

---

## 十一、评估与改造优先级

| 优先级 | 事项 | 说明 |
|---|---|---|
| **P0** | ① 恢复 `DocSaveAction` 权限校验 | 最高安全缺口 |
| | ② 修字段加密 scope（1 行） | 需评估存量明文回填 |
| | ③ 修 `FileUploadAction` L849/L884 SQL | 明确 bug |
| | ④ 去掉 `eval` | 安全 |
| | ⑤ 恢复回收站权限校验 | 越权 |
| **P1** | ⑥ 外发加有效期/签名 | 安全边界 |
| | ⑦ 期刊预览补 api 子类 | 功能不可达 |
| | ⑧ `getTreeNodeById` 补 add | 功能失效 |
| | ⑨ 补齐 myDoc/latestDoc/allDoc（或删 import） | 前台断链 |
| | ⑩ 实现导出（前后端对齐 `BTN_EXPORT`） | 避免僵尸按钮 |
| **P2** | ⑪ GET 写操作改 POST | CSRF |
| | ⑫ 拼串改参数化 | 注入 |
| | ⑬ 统一返回协议 | 可维护性 |
| | ⑭ 拆分重复代码 | 维护成本 |
| | ⑮ 清理死代码与调试日志 | 整洁 |
| | ⑯ netdisk 真正对接后端 | 当前为 Mock |
