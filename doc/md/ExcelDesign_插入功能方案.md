# Excel 设计器「插入」功能完善方案（对齐泛微 E9）

> 目标页面：`http://192.168.1.5:8000/formmode/exceldesign/ExcelDesign?formId=2109677533654978561&formName=集团暴跌收文`
> 适用目录：`ant-design-pro/src/pages/FormMode/ExcelDesign/`
> 参考实现：E9 前端 `D:\Weaver2020\ecology\src4js\pc4mobx\workflowForm\components\layout\CellType.js`；E9 后端 `E:\project\ecology\ecology\src\weaver\workflow\exceldesign\ParseExcelLayout.java`（及同类 `excelUploadImg.jsp`/`excelUploadLink.jsp`/`setIframe.jsp`/`setScanCode.jsp`/`tabStyleDesign.jsp`/`setCustomAttr.jsp` 等设计器配置 JSP）。

---

## 1. 背景与目标

Excel 设计器的"插入"工具条需完整支持 12 类元素：**图片、文本、链接、多语言标签、Iframe 区域、公式、清除背景、标签页、说明、二维/条形码、门户元素**（外加"代码块"布局级脚本）。

现状（截至 2026-11-07）：
- 运行期渲染组件 `components/ElementNodes.tsx` 已按 E9 模型实现全部元素；
- 但**插入弹出框**（`ExcelRibbon.tsx` 的 `INSERT_DEFS`）是手写简化版，仅含最小字段；
- 方案要求的"富属性 schema" `elementSchema.ts` 的 `ELEMENT_FIELDS` 已定义却**未被插入弹窗接入**（当前仅 `elementSchema.ts` 自引用）；
- 因此弹出框对大多数元素仍是"改造前"形态，需统一改为 schema 驱动。

目标：让每个元素的**插入交互 / 属性配置项 / 数据存储 / 渲染逻辑**与 E9 前后端保持一致，并明确代码落点。

---

## 2. 参考代码位置总览

| 关注点 | 文件 | 关键位置 |
|---|---|---|
| 插入菜单 + 弹窗 | `components/ExcelRibbon.tsx` | `INSERT_DEFS` :133；弹窗字段渲染 :665-698；`openInsert` :322；`submitInsert` :351 |
| 元素属性 schema（富） | `elementSchema.ts` | `ELEMENT_FIELDS` :215；`E9_ETYPE` :58；`getElementFields` :364；`normalizeElementConfig` :367；`TAB_STYLE_PRESETS` :144；`isSheetBackgroundImage` :408；`normalizeLinkHref` :416；`buildBarcodeUrl` :438 |
| 落格 / 读格 / 自愈 | `components/UniverExcelGrid.tsx` | `insertElement` :4472；`getCellFieldMeta`；`setElementConfig`（已挂载）；`getCellDisplayValue` 的 element 分支（`${}` 已修复）；保存自愈跳过元素格；`onCellDoubleClick` 的 `dblclick` 监听（标签页双击入口） |
| 预览渲染分发 | `components/ExcelPreview.tsx` | `renderElementNode` ~:660；整表背景图 :1222-1234 与 :2021-2027；`TabContentBlock` |
| 运行期渲染组件 | `components/ElementNodes.tsx` | `MultiLangLabel` :68 / `NoteTip` :76 / `TabElement` :95 / `ImageElement` :107 / `LinkElement` :145 / `IframeArea` :156 / `BarcodeImage` :187 / `PortalElement` :257 / `TextElement` :300 |
| 属性面板 | `components/PropertyPanel.tsx` | 当前为字段属性面板 :35-265；**元素配置面板尚未接入 `ELEMENT_FIELDS`** |
| 标签页编辑 | `components/TabEditor.tsx` | 引入 `TAB_STYLE_PRESETS` :14 |
| E9 后端拼 HTML | `E:\project\ecology\...\ParseExcelLayout.java` | 图片 etype6 :1544；链接 etype11 :1593；标签页 etype12 :1596；门户 etype15 :1606；iframe etype16 :1617；条码 etype17 :1622；`parseHtml_iframe` :2030；`parseHtml_portal` :1998；`parseHtml_scancode` :2089 |
| E9 前端 SPA | `D:\Weaver2020\ecology\src4js\pc4mobx\workflowForm\components\layout\CellType.js` | :42-258（各类元素渲染） |
| 条码后端 | `FormLayoutBarcodeController.java`（前端工程） | `GET /form-layout/barcode/png?type=qr\|bar&msg=&width=&height=&hrp=`；依赖 `blade-starter-report` 的 ZXing `core:3.3.1` 传递依赖 |

---

## 3. 整体架构与数据流

### 3.1 统一的插入入口
所有"元素格"插入共享同一路径：
1. 工具条按钮 → `openInsert(def)`（`ExcelRibbon.tsx:322`）；
2. 有参数 → 弹窗收集 `insertVals` → `submitInsert` 调 `window.univerExcelGrid.insertElement(type, insertVals)`；
3. `insertElement`（`UniverExcelGrid.tsx:4472`）在选中单元格写入
   `fieldMeta = { cellType:'element', elementType:type, elementConfig:values }`；
4. 无参数元素直接插入，不弹窗。

`INSERT_DEFS` 当前为硬编码，应改为由 `getElementFields(type)`（即 `ELEMENT_FIELDS`）生成，使弹出框自动拥有富属性项（见第 6 节）。

### 3.2 数据存储形态
- **元素格**：单元格 `fieldMeta.elementConfig` 承载全部配置（≈ E9 `jsonparam`）。`E9_ETYPE` 给出 E9 数字编码用于导入导出兼容。
- **整表背景图（图片 srcdtype=2）**：不落单元格图片，而是写入 `layoutJson.backgroundImage`，并镜像到 `sheets.sheet1.backgroundImage`（`elementSchema.ts:403-409`、`ExcelPreview.tsx:2021-2027`），预览由表格容器铺图；保存时在 `result`/`parentData`/`emptyResult` 三处携带，避免 `handleLayoutChange` 整体替换时冲掉。
- **标签页**：内容落在独立子画布 `layout.tabTables[tabId]`（E9 式每页一张 Excel），主表标签页格仅作"配置载体"。
- **公式**：写入 `=` 交由 Univer 公式引擎；**非元素格**，结果不进提交载荷。
- **代码块**：布局级脚本，存 `layout_config`，不占单元格。
- **保存自愈**：保存时跳过 `cellType==='element'` 的单元格，避免被改写成 `${}`（此前 bug 已修）。

### 3.3 渲染分发
`ExcelPreview.renderElementNode` 按 `elementType` 分发到 `ElementNodes` 各组件（全部仅依赖 `elementConfig`，不读写字段值）：

| elementType | 组件 | 运行期要点 |
|---|---|---|
| multilang | `MultiLangLabel` | `useIntl()` 取 locale → `pickMultiLang` 选 zh/en/tw |
| note | `NoteTip` | antd `Tooltip`，`placement` 控制方向 |
| tab | `TabElement` | 仅显示摘要；真实头/内容由外层 `TabContentBlock` 联动 |
| image | `ImageElement` | `srcdtype` 1=单元格背景 / 2=整表背景（容器渲染）/ 3=浮动图 |
| link | `LinkElement` | `normalizeLinkHref` 补全 `protocol` |
| iframe | `IframeArea` | `resolveIframeSrc` 注入 `$requestid$` 等变量；`heightMode==='auto'` 自适应 |
| barcode | `BarcodeImage` | `buildBarcodeUrl` 走 fetch+Blob（带 Blade-Auth），`$字段名$` 变量替换 |
| portal | `PortalElement` | `portalApi.fetchElementDataBatch` + `ElementRenderer` |
| text | `TextElement` | `cssClass`/`style`/`hide` |
| （明细周边） | 直接渲染 | `detailTitle`/`detailTail`/`detailButtons`/`sumName`/`sumValue`/`checkAll`/`checkSingle`/`serialNum` |

---

## 4. 各元素详细设计

> 每项给出：**插入交互 / 属性配置项（改造后，来自 `ELEMENT_FIELDS`）/ 数据存储 / 渲染逻辑 / E9 参考**。当前 `INSERT_DEFS` 缺项会标注 `[待补]`。

### 4.1 图片（etype 6）
- **插入交互**：工具条「插入 → 图片」→ 用途选「整表背景图」(srcdtype=2) 或普通图片 → 上传/填 URL → 插入。元素格作配置载体，右侧属性面板可改地址/用途。
- **属性配置项**（`ELEMENT_FIELDS.image`）：`src`(type=`upload` 双通道) / `srcdtype`(1 单元格背景 / 2 整表背景 / 3 浮动) / `width` / `height` / `alt`。
- **数据存储**：`elementConfig={src, srcdtype, width, height, alt}`；srcdtype=2 时额外写 `layoutJson.backgroundImage`。
- **渲染**：`ImageElement`——srcdtype=2 仅显示"由表格容器渲染"标记；=1 用 `background-image` 铺单元格；=3 浮动 `<img>`。
- **E9 参考**：`ParseExcelLayout.java:1544`（etype 6 单元格背景图）；`excelUploadImg.jsp:180-200`（type=2 addBgImg）。
- **图片上传决策（见第 5 节）**：接 `uploadFile()` → `/api/blade-resource/file/upload`，同时保留 URL 手填兜底（服务未启动时仍可用）。

### 4.2 文本（setCustomAttr）
- **插入交互**：「插入 → 文本」→ 直接插入或弹窗填内容。
- **属性配置项**（`ELEMENT_FIELDS.text`）：`content` / `cssClass` / `style` / `hide`(运行期隐藏)。
- **数据存储**：`elementConfig={content, cssClass, style, hide}`。
- **渲染**：`TextElement`——`hide` 时返回 `null`；`style` 走 `parseInlineStyle`(`k:v;k:v`)。
- **E9 参考**：`setCustomAttr.jsp:76-86`（`class`/`style`/`hide`）；`CellType.js` 文本节点。

### 4.3 链接（etype 11）
- **插入交互**：「插入 → 链接」→ 选协议 + 填地址 + 显示文字 + 打开方式。
- **属性配置项**（`ELEMENT_FIELDS.link`）：`protocol`(http/https/ftp/news) / `href` / `text` / `target`(`_blank`/`_self`)。
- **数据存储**：`elementConfig={protocol, href, text, target}`。
- **渲染**：`LinkElement`——`normalizeLinkHref` 把 `protocol`+`href` 拼成完整 URL。
- **E9 参考**：`ParseExcelLayout.java:1593`（`parseHref(fieldtype, field, evalue)`）；`excelUploadLink.jsp:36-43`。

### 4.4 多语言标签 ✅（已按决策 a 实现）
- **决策**：**(a) 沿用现有三语文本 zh/en/tw + 前端 locale 切换（零后端改动）**。项目无后端多语言表，仅 `src/locales/` 8 个 UI 语言包 + `blade-starter-i18n` 请求级工具。
- **插入交互**：「插入 → 多语言标签」→ 填 zh/en/tw 三语。
- **属性配置项**（`ELEMENT_FIELDS.multilang`）：`zh` / `en` / `tw`。
- **数据存储**：`elementConfig={zh, en, tw}`。
- **渲染**：`MultiLangLabel`——`useIntl().locale` → `pickMultiLang` 选对应语种。
- **E9 参考**：E9 多语言由后端语言包；本项目以"三语字段 + 前端 locale"等价替代，无后端依赖。

### 4.5 Iframe 区域（etype 16）✅（已补齐）
- **插入交互**：「插入 → Iframe 区域」→ 填地址(+变量) / 高度模式 / 高度 / ID / Name / 样式。
- **属性配置项**（`ELEMENT_FIELDS.iframe`）：`setId` / `setName` / `src`(支持 `$requestid$`/`$workflowid$`/`$nodeid$`/`$formid$`) / `heightMode`(fixed/auto) / `height` / `style`。
- **数据存储**：`elementConfig={setId, setName, src, heightMode, height, style}`。
- **渲染**：`IframeArea`——`resolveIframeSrc` 注入流程变量；`heightMode==='auto'` 时 `onLoad` 读 `contentDocument` 自适应高度。
- **E9 参考**：`ParseExcelLayout.java:2030` `parseHtml_iframe`（`set_id`/`set_name`/`set_src`/`set_height`(`auto`=自适应)/`set_style`）。

### 4.6 公式
- **插入交互**：「插入 → 公式」→ 直接进入单元格公式编辑（写入 `=`），由 Univer 公式引擎求值；非元素格。
- **属性配置项**：无（mode=`formula`）。
- **数据存储**：单元格值/表达式；预览/审批态按表达式完整计算。
- **与 E9 差距**：E9 有公式表/触发动作/后端重算；本项目公式结果**不进提交载荷**（`collectFieldValues` 只收 field 格）。补齐需动 `collectFieldValues.ts` 与 `blade-workflow` 提交校验（**当前明确未覆盖项**）。

### 4.7 清除背景
- **插入交互**：「插入 → 清除背景」→ 立即执行（mode=`action`），React 工具条与原生 ribbon 已同步。
- **行为**：`clearBackground()` 一次清 ① 选区单元格背景色 ② sheet 级背景图 ③ 该背景图对应的配置载体格。
- **E9 参考**：E9 `clearBgImg`（`baseOperate_wev8.js:2054-2062`）。

### 4.8 标签页（etype 12）
- **插入交互**：「插入 → 标签页」→ 填名称（逗号分隔多个）→ 插入标签格；**双击标签格** → 弹出页签编辑面（`TabEditor`）→「新增标签页」→「编辑当前页签内容」打开独立子 Excel 画布（`tabTables[tabId]`）。双击入口已在 `UniverExcelGrid` 的 `dblclick` 监听接上。
- **属性配置项**（`ELEMENT_FIELDS.tab`）：`name` / `style`(预设 -1~-6 + 自定义 0) / `defshow` / `areaheight` / 自定义 `selBg`/`selColor`/`unselBg`/`unselColor`/`tabFontSize`。
- **数据存储**：`elementConfig` 存容器配置；内容存 `layout.tabTables[tabId]`。
- **渲染**：`TabElement` 仅摘要；真实头/内容由 `ExcelPreview.TabContentBlock` 受控渲染 + 按页行区间过滤。
- **E9 参考**：`ParseExcelLayout.java:1596` `analyzeTabArea`；`tabpage_wev8.js` / `tabStyleDesign.jsp:9-60`（`TAB_STYLE_PRESETS` 已对齐 styleid -1~-6）。

### 4.9 说明（etype 25）
- **插入交互**：「插入 → 说明」→ 填说明内容 + 提示位置。
- **属性配置项**（`ELEMENT_FIELDS.note`）：`content` / `placement`(top/topLeft/.../left)。
- **数据存储**：`elementConfig={content, placement}`。
- **渲染**：`NoteTip`——antd `Tooltip` 问号提示。
- **E9 参考**：`CellType.js:238-246`（`WeaHelpfulTip`）。

### 4.10 二维/条形码（etype 17）✅（后端已具备）
- **决策**：接 **`createQRCode` 风格接口**（`blade-starter-report` 已有 ZXing `core:3.3.1` 传递依赖），新增 `FormLayoutBarcodeController.java` → `GET /form-layout/barcode/png?type=qr|bar&msg=&width=&height=&hrp=`。
- **插入交互**：「插入 → 二维/条形码」→ 选类型 + 编码内容(+`$字段名$`变量) + 关联文本字段 + 是否隐藏文本。
- **属性配置项**（`ELEMENT_FIELDS.barcode`）：`type`(qrcode/barcode) / `content` / `relatefield`(multiselect) / `hidetext`(switch) / `width` / `height`。
- **数据存储**：`elementConfig={type, content, relatefield, hidetext, width, height}`。
- **渲染**：`BarcodeImage`——fetch+Blob 取图（带 Blade-Auth，免改网关白名单）；`$字段名$` 用运行期 `formValues` 替换；`hidetext` 时不显示文本。
- **E9 参考**：`ParseExcelLayout.java:2089` `parseHtml_scancode`（codetype 1=QR→`/workflow/createQRCode`；2=bar→`/workflow/createWfBarCode`，`hrp=none` 隐藏文本，`relatefield` 联动）；`setScanCode.jsp:10-12,70-102`。

### 4.11 门户元素（etype 15）✅（已按决策复用）
- **决策**：**(a) 复用 `Portal/components/ElementRegistry` 的 `ElementRenderer` 按 `elementType` 渲染**。
- **插入交互**：「插入 → 门户元素」→ 选门户元素类型 + 填 hpid/elementId + 每页条数 + 高度。
- **属性配置项**（`ELEMENT_FIELDS.portal`）：`elementType`(下拉复用 `REGISTERED_ELEMENT_TYPES`) / `hpid` / `elementId` / `perPage` / `height`。
- **数据存储**：`elementConfig={elementType, hpid, elementId, perPage, height}`。
- **渲染**：`PortalElement`——`portalApi.fetchElementDataBatch` 取数 → `ElementRenderer` 渲染。
- **E9 参考**：`ParseExcelLayout.java:1998` `parseHtml_portal`（hpid + `$requestid$`/`$wfid$`/`$nodeid$`/`$formid$` 变量透传）。

### 4.12 代码块（布局级脚本）
- **插入交互**：「插入 → 代码块」→ 弹窗编辑 HTML/JS（mode=`script`），不占单元格。
- **数据存储**：整份表单一份脚本，存 `layout_config`，加载时注入执行（`utils/runLayoutScript.ts`）。
- **E9 参考**：E9「按 layoutid 取整表一份 script」。

---

## 5. 关键决策点（用户已确认）

| 元素 | 决策 | 说明 |
|---|---|---|
| 多语言标签 | **(a)** 三语文本 zh/en/tw + 前端 locale 切换 | 零后端改动；无后端多语言表 |
| 二维/条形码 | 接 `createQRCode` 风格接口 | `blade-starter-report` 已有 ZXing `core:3.3.1` 传递依赖；前端 `fetch+Blob` 取图 |
| 门户元素 | 复用 `ElementRegistry.ElementRenderer` | 按 `elementType` 渲染 + `portalApi.fetchElementDataBatch` |
| 图片上传 | 双通道：`uploadFile()` → `/api/blade-resource/file/upload` **+ URL 手填兜底** | 依赖 `blade-resource` 服务；**当前沙箱该服务未启动**，故必须保留 URL 兜底，保证设计态可用 |

---

## 6. 需补齐的工作清单（让所有弹出框成为"改造后"）

按方案原话"**ExcelRibbon 插入菜单改为 schema 生成**"，当前 `INSERT_DEFS` 是手写简化版，应改为由 `ELEMENT_FIELDS` 驱动：

1. **`INSERT_DEFS` 元素项 schema 化**：元素类 `fields` 直接取 `getElementFields(type)`，一次性补齐 图片 `srcdtype`+`upload`、链接 `protocol`、文本 `cssClass/style/hide`、条码 `relatefield/hidetext`、标签页 `style` 等富属性。
2. **弹窗 Modal 新增三种字段渲染**（`ExcelRibbon.tsx` 当前仅 `text/textarea/number/select`）：
   - `upload`：双通道——`Upload` 组件调 `uploadFile()` 上传 + 文本框手填 URL 兜底；
   - `multiselect`：antd `Select mode="multiple"`（条码 `relatefield`、明细按钮集）；
   - `switch`：antd `Switch`（文本 `hide`、条码 `hidetext`）。
3. **`PropertyPanel` 接入元素属性面板**：选中元素格时按 `ELEMENT_FIELDS` 渲染专属表单，回写 `setElementConfig`（与插入弹窗同一 schema），实现"插入弹窗配置"与"选中后改属性"一致。
4. **iframe 自适应时隐藏高度**：把当前手写的 `showIf` 一并移入 `ELEMENT_FIELDS.iframe.height`（保持体验）。
5. **导入导出兼容**：`E9_ETYPE` 已具备数字编码；`set_id/set_name/set_src/set_height/set_style` 等 E9 `jsonparam` 键在导入时映射到本项目 `setId/setName/src/height/style`（运行期 `IframeArea` 用 camelCase）。

---

## 7. 实现要点与已知坑

- **`${}` 显示 bug（已修）**：`getCellDisplayValue` 须对 `cellType==='element'` 走元素分支，否则元素格被写成 `${...}`。
- **保存自愈**：保存时跳过元素格，避免被公式/值覆盖成 `${}`；`elementConfig` 三处（`result`/`parentData`/`emptyResult`）携带，防 `handleLayoutChange` 替换冲掉。
- **标签页联动**：单元格内不再渲染第二套 antd Tabs，避免与外层 `TabContentBlock` 选中态冲突（历史"切换不联动"根因）。
- **整表背景图无 canvas API**：Univer 无 `ss.backgroundImage()`，画布内无法直接铺图，以状态标记替代；运行期由预览/表单容器真实渲染（`ExcelPreview` 表格容器 `backgroundImage: url(...)` + `no-repeat/center/contain`，单元格 `s.bg` 仍盖其上）。
- **条码中文回退**：`parseHtml_scancode` 对条形码含中文返回 `illegalBarCode_wev8.png`；前端对 `bar` 类型做 ASCII 校验并提示。
- **iframe 跨域自适应**：`onLoad` 读 `contentDocument.scrollHeight` 仅在同源可用，跨域保留配置高度（已 `try/catch`）。
- **公式不入提交**：`collectFieldValues` 只收 field 格；公式结果入库/服务端复核为明确未覆盖项，需后续单独评估。
