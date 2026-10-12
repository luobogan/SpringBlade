# 字段控件体系 —— PC 端与手机端组件汇总

> 适用范围：`ant-design-pro/src/components/FormMode/` 及其页面层消费方。
> 本文是该体系的**总览与速查手册**；每个源文件头部另有更详细的实现注释。

---

## 1. 一句话概括

全项目字段控件**只有一个渲染入口** `FieldControl`。桌面端用 **antd 6**，手机端用
**ant-design-mobile 5**，两端**共用同一份 props 契约**（`FieldControlBaseProps`），
由 `variant` 属性分发。页面层禁止自行 `switch` 字段类型。

---

## 2. 文件清单与职责

### 2.1 共享层（两端都用，不含任何 UI）

| 文件 | 导出 | 职责 |
| --- | --- | --- |
| `fieldTypes.ts` | `ControlType`、`FieldControlBaseProps`、`FieldOption`、`mapFieldToControlType()`、`resolveBrowserType()`、`getFieldTypeLabel()`、`getBrowserTypeLabel()`、`isBrowserTypeMultiple()`、`backfillLayoutFieldMeta()`、`toDayjsValue()` / `fromDayjsValue()` | **字段类型编码的唯一权威**：编码→中文标签、编码→控件类型、两端 props 契约、日期值与 antd/antd-mobile 的互转 |
| `fieldTokens.ts` | `FIELD_COLOR`、`DESKTOP_FIELD_TOKENS`、`MOBILE_FIELD_TOKENS`、`getFieldTokens()`、`useIsMobileViewport()`、`resolveVariant()`、`attrBackground()`、`MOBILE_BREAKPOINT` | **样式与断点的单一来源**：语义色、两端尺寸令牌、响应式 hook |
| `personOrg.ts` | `BROWSER_TYPE_META`、`PersonOrgItem`、`PersonOrgCategory`、`splitIds()` / `joinIds()`、`loadPersonOrgData()`、`queryUsers()`、`resolveItemNames()`、`itemsOfCategory()`、`collectDeptIds()`、`USER_PAGE_SIZE` | 人员/组织**数据与元信息**：类型编号→类别/单多选、id 串解析、服务端查询与名称反查 |
| `useUserNames.ts` | `useUserNames(ids)` | 把 id 数组批量反查为姓名 map 的 hook |

### 2.2 PC 端（antd）

| 文件 | 导出 | 职责 |
| --- | --- | --- |
| `FieldControl.tsx` | `FieldControl`（默认导出）、`FieldControlProps` | **统一入口 + 桌面实现**。接收 `variant`，为 `mobile` 时整体转交 `MobileFieldControl`，否则走本文件内的 antd switch |
| `BrowserControl.tsx` | `BrowserControl` | 浏览按钮壳：单/多选语义、类型 Tag、未接入类型的降级（输入框 + Tooltip） |
| `PersonOrgPicker.tsx` | `PersonOrgPicker` | 桌面选人弹窗：antd `Modal`（780px）+ 左侧 200px `Tree` + 右侧 `Table` + 分页器 |

### 2.3 手机端（antd-mobile）

| 文件 | 导出 | 职责 |
| --- | --- | --- |
| `MobileFieldControl.tsx` | `MobileFieldControl` | 手机端控件集：`Input`/`TextArea`/`DatePicker`/`Picker`/`Switch`/`Radio`/`CheckList` 等；首次渲染时注入一次主题与局部样式（`data-fm-adm-field`） |
| `PersonOrgPicker.tsx`（同一文件内的 `isMobile` 分支） | `MobilePersonOrgPicker` | 手机端选人弹窗：`Popup(position=bottom, 82vh)` + `NavBar` + `SearchBar` + `Selector` + `CheckList` + 「加载更多」 |

> `MobilePersonOrgPicker` 是 `PersonOrgPicker` 的 `variant='mobile'` 薄包装。
> **选中逻辑、数据加载、分页、名称反查两端完全共用同一份 state**，仅 UI 形态不同。

### 2.4 页面层容器（只调 `FieldControl`，不感知控件细节）

| 文件 | 角色 |
| --- | --- |
| `pages/FormMode/ExcelDesign/components/ExcelPreview.tsx` | Excel 预览/办理/发起页。`FieldCell` 负责 `<td>` 布局、Excel 单元格样式、节点权限；`SheetPreviewForm` 走桌面网格，`MobileSheetForm` 走手机列表化布局（内部固定传 `variant='mobile'`） |
| `pages/FormMode/FormView/components/FieldRenderer.tsx` | 表单数据详情/新增/编辑、流程测试页。负责只读降级 + placeholder 文案，**默认 `variant='auto'`** |
| `pages/FormMode/TableDesign/components/BrowserTypePreview.tsx` | 表设计页的字段类型预览 |
| `pages/FormMode/WorkflowDesign/WorkflowTestModal.tsx`、`pages/FormMode/Test/Test.tsx` | 通过 `FieldRenderer` 间接使用 |

---

## 3. 分层架构

```
页面 / 容器层（负责布局、权限、校验外壳）
  ExcelPreview.FieldCell         Excel 网格 <td> + 单元格样式 + 节点权限
  FormView.FieldRenderer         数据详情/新增/编辑页：只读降级 + placeholder（默认 auto）
  TableDesign.BrowserTypePreview 表设计页：类型预览
        │  都只调用 FieldControl，不感知 antd / antd-mobile
        ▼
统一入口 FieldControl（按 variant 分发）
  ├── variant='desktop'（默认）→ 本文件内的 antd 实现（size=small，适配 120px 窄单元格）
  ├── variant='mobile'         → MobileFieldControl（antd-mobile）
  └── variant='auto'           → useIsMobileViewport() 视口 ≤768px 自动选 mobile
        │
        ▼
BrowserControl（浏览按钮：单/多选语义 + 类型 Tag + 降级）
        │  variant 透传
        ▼
PersonOrgField（表单字段壳）→ PersonOrgPicker（isMobile 分支切手机 UI）
```

---

## 4. 统一 props 契约

**唯一定义处：`fieldTypes.ts` 的 `FieldControlBaseProps`**（`FieldControl.tsx` 与
`MobileFieldControl.tsx` 都 import 它）。这样新增属性必须两端都实现，从编译期强制对齐，
同时避免 `FieldControl ↔ MobileFieldControl` 循环依赖。

| 属性 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `controlType` | `ControlType` | 必填 | 控件类型，由 `mapFieldToControlType(field)` 得出 |
| `value` | `any` | - | 当前值 |
| `onChange` | `(v:any)=>void` | - | 值变化回调 |
| `options` | `FieldOption[]` | `[]` | 下拉/单选选项，`{label,value}` |
| `disabled` | `boolean` | - | 禁用 |
| `readOnly` | `boolean` | - | 只读（预览/打印态），与 `disabled` 等价 |
| `placeholder` | `string` | - | 占位符 |
| `status` | `'' \| 'error'` | - | 校验失败红框 |
| `browserType` | `number` | - | 仅 `browser`：E9 浏览按钮编号 |
| `multiple` | `boolean` | - | 仅 `browser`；不传按 `isBrowserTypeMultiple()` 推导 |
| `maxLength` | `number` | `200` | 文本最大长度 |
| `rows` | `number` | `3`(PC)/`4`(手机) | textarea 行数 |
| `inputId` | `string` | - | 挂到真实 input 的 DOM id，供布局级代码块取/设值 |
| `style` | `CSSProperties` | - | 外层样式（承载 Excel 单元格底色/字体），**优先级最高** |
| `variant` | `'desktop'\|'mobile'\|'auto'` | `'desktop'` | 外观变体 |

### 4.1 命名规范

- 只读属性统一驼峰 **`readOnly`**（禁用态用 `disabled`，二者合并生效）。
  `PersonOrgField` / `FieldRenderer` 上的历史 `readonly`（小写 o）已标 `@deprecated` 并做兼容映射。
- 字段类型枚举统一为 `ControlType`，不存在第二套编码解释。
- 日期值统一以**字符串**存取：`YYYY-MM-DD` / `YYYY-MM-DD HH:mm:ss`，
  由 `fieldTypes.ts` 的 `toDayjsValue` / `fromDayjsValue` 与组件互转。
  ⚠️ 不可把 dayjs 对象塞进表单值（会在 rc-picker 内部抛错并被 ErrorBoundary 接管成白屏）。
- 颜色/字号/尺寸只取自 `fieldTokens.ts`，禁止在页面或控件里写魔法值。

---

## 5. 双端控件映射对照

| `controlType` | PC 端（antd） | 手机端（antd-mobile） |
| --- | --- | --- |
| `text` | `Input` | `Input`（16px、带边框圆角、`clearable`） |
| `password` | `Input.Password` | `Input type="password"` |
| `textarea` | `Input.TextArea` | `TextArea`（`showCount`） |
| `number` / `wholeNumber` | `InputNumber` | `Input inputMode="decimal"/"numeric"` |
| `date` | `DatePicker` 下拉 | `DatePicker` 底部滚轮（`precision="day"`） |
| `datetime` | `DatePicker showTime` | `DatePicker` 底部滚轮（`precision="second"`） |
| `select` | `Select` 下拉 | `Picker` 底部滚轮单选 |
| `radio` | `Radio.Group` 横排 | `Radio` **纵排**（adm 无 `Radio.Group`，手动管理选中） |
| `checkbox` | `Checkbox`（"同意"） | **`Switch` 开关** |
| `browser` | `BrowserControl` + antd `Modal` 弹窗 | `BrowserControl` + antd-mobile `Popup` 弹层 |
| `attachment` | `Upload` + 小按钮 | `Upload` + 整行虚线上传区 |
| `richtext` / `group` / `custom` / `label` | 展示型，无交互 | 同左（字号放大） |
| 未知类型 | 兜底 `Input` | 兜底 `Input` |

**值语义双端完全一致**（换组件库不会导致已存数据读取不到）：
`checkbox` 单布尔；`browser` 为 id 逗号串；`date`/`datetime` 为上述字符串格式。

---

## 6. 手机端专项说明

### 6.1 为什么用 antd-mobile 而不是 antd

antd 的控件按桌面鼠标交互设计（24px 高、13px 字号、下拉浮层按 PC 视口计算），
在手机上会出现：触控目标过小、聚焦时 iOS Safari 自动放大页面、下拉框溢出屏幕。
antd-mobile 专为 H5 触屏设计：48px 级触控高度、滚轮式底部弹层、`Switch` 开关、
自带 `:active` 按压反馈与惯性滚动。

### 6.2 主题对齐

antd-mobile 通过 CSS 变量定制主题。`MobileFieldControl` 首次渲染时注入一次：

```css
:root {
  --adm-color-primary: #1677ff;   /* 对齐 antd 主色 */
  --adm-color-text: #333333;
  --adm-color-border: #d9d9d9;
  --adm-font-size-main: 16px;
}
```

### 6.3 必须补回输入框边框

antd-mobile 的 `Input` 默认**无边框 + 透明背景**（它的设计依赖外层 `List`/`Form` 分隔线界定
输入区）。本项目手机端是白底卡片 + 无分隔线的流式布局，裸输入框会**完全看不见边界**
（实测 `.adm-input` 计算样式为 `border: 0px solid rgba(0,0,0,0.88)`、`background: transparent`）。

故在作用域内补回：`border:1px solid #d9d9d9; border-radius:6px; padding:8px 12px; background:#fff`，
高度约 **44px**。同理：`.adm-switch` 放大到 36px（默认 31px）、`.adm-radio` 字号 16px（默认 14px）。

### 6.4 错误态

antd-mobile 的 `Input` **没有** antd 的 `status` 属性，只能 CSS 覆盖：

```css
.fm-adm-field--error .adm-input,
.fm-adm-field--error .adm-text-area,
.fm-adm-field--error .fm-adm-trigger { border-color: #ff4d4f !important; }
```

### 6.5 日期/下拉触发器

`DatePicker` / `Picker` 是弹层组件（不渲染输入框），通过 `children` render-prop 拿
`actions.open()` 自制触发器，并做成与输入框一致的外观（`.fm-adm-trigger`：40px、边框、圆角、▼ 箭头）。

⚠️ `inputId` 在该场景挂在**触发器 div** 上（Picker 无原生 input）。布局级代码块用
`getElementById` 能定位元素，但读不到 `.value` —— 需改用 `window.ExcelPreview.get/set`（按字段名）。

### 6.6 选人/选组织弹窗双端对照

| | PC 端（antd） | 手机端（antd-mobile） |
| --- | --- | --- |
| 容器 | `Modal` 780px 居中 | `Popup position="bottom"`（82vh）+ `NavBar` |
| 部门分类 | 左侧 200px `Tree` | `Selector` 横向胶囊（全部 + 一级部门），深层部门用搜索定位 |
| 列表 | `Table` + 分页器 | `CheckList` + 「加载更多」 |
| 搜索 | `Input` + prefix 图标 | `SearchBar` |
| 已选区 | 底部 `Tag` 列表 | 固定底部、可逐项移除 |
| 空提示 | antd `Empty` | antd-mobile `Empty` |
| 校验提示 | antd `message` | antd-mobile `Toast` |

**variant 透传链**（新增 browser 相关属性时必须走通）：

```
FieldControl(variant)
  └─ MobileFieldControl            → 手机端实现，browser 分支显式传 variant="mobile"
       └─ BrowserControl(variant)  → 透传给 PersonOrgField
            └─ PersonOrgField(variant) → 按端调整触发器尺寸/按钮文案
                 └─ PersonOrgPicker(variant) → isMobile 分支返回手机 UI
```

⚠️ `MobileFieldControl` 的 browser 分支**必须显式传 `variant="mobile"`**：该组件只在手机端被渲染，
若不传，`BrowserControl` 用默认 `'desktop'`，会在手机上弹出 780px 宽的 antd Modal。

### 6.7 响应式

- `useIsMobileViewport()`：`matchMedia` + `useSyncExternalStore`，跨断点**实时重渲染**
  （替换了原先挂载时算一次 `window.innerWidth` 的做法，窗口缩放/旋屏都能响应）。
- 手机列表行：`flexWrap` + `minWidth`，窄屏（如 360px）时标签与控件自动上下堆叠。
- `ExcelPreview` 保留「自动/PC/手机」`Segmented` 切换器，便于在桌面上手动预览手机布局。

---

## 7. 使用示例

### 7.1 桌面端（默认）

```tsx
import FieldControl from '@/components/FormMode/FieldControl';
import { mapFieldToControlType, resolveBrowserType } from '@/components/FormMode/fieldTypes';

<FieldControl
  controlType={mapFieldToControlType(field)}
  browserType={resolveBrowserType(field)}
  value={value}
  onChange={setValue}
  options={field.options?.map(o => ({ label: o.label, value: o.value }))}
  maxLength={field.fieldLen || 200}
  readOnly={isPreview}
  status={error ? 'error' : ''}
/>
```

### 7.2 强制手机端

```tsx
<FieldControl controlType="text" value={v} onChange={setV} variant="mobile" />
```

### 7.3 响应式（推荐，同一份代码适配两端）

```tsx
<FieldControl controlType="select" options={opts} value={v} onChange={setV} variant="auto" />
```

### 7.4 页面级容器（`FieldRenderer`，已默认 auto）

```tsx
<FieldRenderer field={field} value={v} onChange={setV} readOnly />
```

### 7.5 选人字段

```tsx
// browserType：1 人力资源 / 161 多人力资源 / 2 部门 / 17 多部门
//             18 分部 / 3 角色 / 4 岗位
<FieldControl controlType="browser" browserType={1} value={v} onChange={setV} variant="auto" />
```

---

## 8. 如何新增一种字段类型

1. **`fieldTypes.ts`**：加进 `ControlType` 联合类型 + `mapFieldToControlType()` 的映射分支；
2. **`FieldControl.tsx`（antd）与 `MobileFieldControl.tsx`（antd-mobile）**：两端都要加 `case`
   （props 类型共用 `FieldControlBaseProps`，只改一端时 TS 会提示）；
3. 若是新浏览按钮类型：改 `personOrg.ts` 的 `BROWSER_TYPE_META` + `fieldTypes.ts` 的
   `BROWSER_TYPE_LABEL_MAP`，UI 自动支持；
4. 需要新尺寸/配色：只在 `fieldTokens.ts` 增加令牌；
5. antd-mobile 特有主题色：改 `MobileFieldControl.tsx` 的 `ensureAdmStyles()`。

---

## 9. 已知限制与注意事项

1. **`inputId` 对弹层类控件**（`DatePicker`/`Picker`/选人）只能定位到宿主元素，读不到 `.value`。
2. **窄列控件拥挤**：Excel 列宽 120px 时，`browser` 字段的输入框 + 按钮会被压缩，
   这是 Excel 布局的忠实还原（设计器里显示的是占位文本所以看不出）；如需宽松，应在设计器调宽字段列。
3. **Excel 单元格样式通过 `style` 下发**：AntD 控件**不继承** `<td>` 的 font，
   必须在容器层显式下发字体/字号/字重（`FieldCell` 已处理）。
4. **`ExcelPreview` 的 isMobile 仍用自研 `detectMobile()` + Segmented**：这是有意的 ——
   它需要让人在桌面上手动切到手机布局做验证；`variant='auto'` 能力已开放给其他消费方。
5. **多语言字段值未清洗**：E9 导入的 `~7中文~8英文~9繁体` 标记语法目前按原样显示，
   涉及取值清洗，改动会同时影响两端，需单独评估。

---

## 10. 相关文档

- 组件实现内的详细注释：`ant-design-pro/src/components/FormMode/README.md`
- 字段属性配置（对标泛微 E9）：`doc/md/泛微E9表单建模字段属性配置详解.md`
- 表单建模整体分析：`doc/md/表单建模功能分析报告.md`
- 字段权限优先级：`doc/md/字段权限优先级链.md`