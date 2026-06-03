# SpreadJS 在 Ecology 项目中的核心 API 接口清单

## 概述

Ecology 项目使用了两种版本的 SpreadJS 电子表格组件：

| 版本类型 | 命名空间 | 主要模块 |
|---------|---------|---------|
| **旧版 Wijmo Spread** | `$.wijmo.wijspread` | excelDesign（表单设计器） |
| **新版 GrapeCity SpreadJS** | `GC.Spread.Sheets` | fnaMulDimensions、edc、cube、formmode |

---

## 一、旧版 Wijmo Spread API（Excel 设计器模块）

### 1.1 工作簿初始化与配置

| API | 方法签名 | 用途 | 示例 |
|-----|---------|------|------|
| 创建工作簿 | `container.wijspread(options)` | 初始化电子表格 | `$("#wfed_excelDiv").wijspread({sheetCount:1})` |
| 获取工作簿 | `.wijspread("spread")` | 获取工作簿实例 | `container.wijspread("spread")` |
| 设置撤销 | `.allowUndo(boolean)` | 启用/禁用撤销功能 | `spread.allowUndo(false)` |
| 获取活动表 | `.getActiveSheet()` | 返回当前活动工作表 | `spread.getActiveSheet()` |
| JSON 序列化 | `.toJSON()` | 将工作簿转为 JSON | `ss.toJSON()` |
| JSON 反序列化 | `.fromJSON(json)` | 从 JSON 恢复工作簿 | `ss.fromJSON(json)` |

### 1.2 工作表操作

| API | 方法签名 | 用途 |
|-----|---------|------|
| 获取单元格 | `sheet.getCell(row, col, area)` | 返回指定位置单元格对象 |
| 设置保护 | `sheet.setIsProtected(boolean)` | 设置工作表保护状态 |
| 获取选区 | `sheet.getSelections()` | 获取当前选区数组 |

### 1.3 单元格操作

| API | 方法签名 | 用途 |
|-----|---------|------|
| 设置值 | `.value(val)` | 设置单元格值 |
| 获取值 | `.value()` | 获取单元格值 |
| 获取文本 | `.text()` | 获取单元格显示文本 |
| 设置背景图 | `.backgroundImage(url)` | 设置单元格背景图片 |
| 设置图片布局 | `.backgroundImageLayout(layout)` | 背景图布局方式 |
| 设置边框 | `.borderTop(border)` | 设置上边框 |
| 设置字体 | `.font(font)` | 设置字体样式 |
| 水平对齐 | `.hAlign(align)` | 设置水平对齐 |
| 垂直对齐 | `.vAlign(align)` | 设置垂直对齐 |

### 1.4 工作表区域常量

```javascript
$.wijmo.wijspread.SheetArea.viewport   // 数据区域
$.wijmo.wijspread.SheetArea.colHeader  // 列标题区域
$.wijmo.wijspread.SheetArea.rowHeader  // 行标题区域
```

### 1.5 核心事件

| 事件名 | 触发时机 |
|-------|---------|
| `CellClick` | 单元格单击 |
| `CellDoubleClick` | 单元格双击 |
| `SelectionChanged` | 选区变化完成 |
| `EditStarting` | 编辑开始 |
| `EditEnd` | 编辑结束 |
| `EditChange` | 编辑内容变化 |
| `ClipboardChanged` | 剪切板内容变化 |
| `ClipboardPasted` | 粘贴完成 |

---

## 二、新版 GrapeCity SpreadJS API

### 2.1 核心对象构造

| 对象 | 构造方法 | 用途 |
|------|---------|------|
| **Workbook** | `new GC.Spread.Sheets.Workbook(container, options)` | 创建工作簿实例 |
| **Style** | `new GC.Spread.Sheets.Style()` | 创建单元格样式对象 |
| **FormulaTextBox** | `new GC.Spread.Sheets.FormulaTextBox.FormulaTextBox(el)` | 创建公式输入框 |

### 2.2 工作簿级别 API

| API | 方法签名 | 用途 |
|-----|---------|------|
| 获取活动表 | `workbook.getActiveSheet()` | 返回当前活动工作表 |
| 绑定事件 | `workbook.bind(event, handler)` | 绑定事件监听器 |
| 命令管理器 | `workbook.commandManager()` | 返回命令管理器实例 |
| 设置快捷键 | `.setShortcutKey(cmd, key, ctrl, shift, alt, meta)` | 自定义快捷键 |
| JSON 序列化 | `workbook.toJSON()` | 序列化工作簿 |
| JSON 反序列化 | `workbook.fromJSON(json)` | 反序列化工作簿 |

### 2.3 工作表级别 API

| API | 方法签名 | 用途 |
|-----|---------|------|
| 获取单元格 | `sheet.getCell(row, col)` | 返回指定单元格对象 |
| 获取范围 | `sheet.getRange(row, col, rowCount, colCount)` | 返回单元格范围 |
| 获取选区 | `sheet.getSelections()` | 获取当前选区数组 |
| 设置样式 | `sheet.setStyle(row, col, style, area)` | 设置单元格样式 |
| 获取样式 | `sheet.getStyle(row, col, area)` | 获取单元格样式 |
| 获取文本 | `sheet.getText(row, col, area)` | 获取单元格文本 |

### 2.4 单元格级别 API

| API | 方法签名 | 用途 |
|-----|---------|------|
| 设置值 | `cell.value(val)` | 设置单元格值 |
| 获取值 | `cell.value()` | 获取单元格值 |
| 设置背景色 | `cell.backColor(color)` | 设置单元格背景色 |
| 设置字体 | `cell.font(font)` | 设置字体样式 |
| 水平对齐 | `cell.hAlign(align)` | 设置水平对齐方式 |
| 垂直对齐 | `cell.vAlign(align)` | 设置垂直对齐方式 |
| 设置边框 | `cell.borderTop(border)` | 设置单元格边框 |
| 设置背景图 | `cell.backgroundImage(url)` | 设置背景图片 |
| 设置图片布局 | `cell.backgroundImageLayout(layout)` | 背景图布局 |
| 设置锁定 | `cell.locked(boolean)` | 设置单元格锁定状态 |

### 2.5 核心事件

| 事件常量 | 触发时机 |
|---------|---------|
| `GC.Spread.Sheets.Events.EnterCell` | 进入单元格 |
| `GC.Spread.Sheets.Events.LeaveCell` | 离开单元格 |
| `GC.Spread.Sheets.Events.SelectionChanging` | 选区变化中 |
| `GC.Spread.Sheets.Events.SelectionChanged` | 选区变化完成 |
| `GC.Spread.Sheets.Events.EditEnding` | 编辑结束中 |
| `GC.Spread.Sheets.Events.EditEnded` | 编辑结束完成 |
| `GC.Spread.Sheets.Events.ValueChanged` | 单元格值变化 |
| `GC.Spread.Sheets.Events.CellChanged` | 单元格变化 |
| `GC.Spread.Sheets.Events.ClipboardChanged` | 剪切板变化 |
| `GC.Spread.Sheets.Events.ClipboardPasted` | 粘贴完成 |
| `GC.Spread.Sheets.Events.UserFormulaEntered` | 公式输入完成 |

### 2.6 常量枚举

| 枚举类型 | 枚举值 | 说明 |
|---------|-------|------|
| **SheetArea** | `viewport` / `colHeader` / `rowHeader` | 工作表区域 |
| **HorizontalAlign** | `left` / `center` / `right` | 水平对齐方式 |
| **VerticalAlign** | `top` / `center` / `bottom` | 垂直对齐方式 |
| **TextDecorationType** | `underline` / `lineThrough` | 文本装饰类型 |
| **ImageLayout** | `none` / `stretch` / `zoom` | 图片布局方式 |
| **Commands.Key** | `c` / `v` / `x` / `del` / `s` | 快捷键键码 |

---

## 三、Excel 导入导出 API

使用 `gc.spread.excelio` 模块实现 Excel 文件的读写：

| API | 方法签名 | 用途 |
|-----|---------|------|
| 创建 IO 对象 | `new GC.Spread.Excel.IO()` | 创建 Excel IO 实例 |
| 打开文件 | `.open(file, callback)` | 读取 Excel 文件 |
| 保存文件 | `.save(workbook, callback)` | 导出为 Excel 文件 |

---

## 四、模块使用分布

| 模块名称 | SpreadJS 版本 | 版本号 | 主要用途 |
|---------|--------------|--------|---------|
| **excelDesign** | Wijmo Spread | - | 表单设计器画布 |
| **fnaMulDimensions** | GrapeCity | 11.1.0 | 财务多维分析 |
| **edc** | GrapeCity | 11.1.0 / 12.2.2 | EDC 数据采集 |
| **cube** | GrapeCity | 11.1.0 | 多维分析报表 |
| **formmode** | GrapeCity | 11.1.0 | 表单模式 |

---

## 五、典型使用场景

### 场景 1：初始化工作簿

```javascript
// 新版 GrapeCity
const workbook = new GC.Spread.Sheets.Workbook(container, { sheetCount: 1 });
const sheet = workbook.getActiveSheet();

// 旧版 Wijmo
$("#container").wijspread({sheetCount:1});
const spread = $("#container").wijspread("spread");
const sheet = spread.getActiveSheet();
```

### 场景 2：绑定单元格事件

```javascript
// 监听单元格进入事件
workbook.bind(GC.Spread.Sheets.Events.EnterCell, (sender, args) => {
    const sheet = sender.getActiveSheet();
    const row = args.row;
    const col = args.col;
    // 处理逻辑
});
```

### 场景 3：设置单元格样式

```javascript
const style = new GC.Spread.Sheets.Style();
style.backColor = "#FFFF00";
style.font = "12px Arial";
style.hAlign = GC.Spread.Sheets.HorizontalAlign.center;
sheet.setStyle(0, 0, style, GC.Spread.Sheets.SheetArea.colHeader);
```

### 场景 4：快捷键自定义

```javascript
// 禁用默认复制粘贴快捷键
workbook.commandManager().setShortcutKey(undefined, GC.Spread.Commands.Key.c, true, false, false, false);
workbook.commandManager().setShortcutKey(undefined, GC.Spread.Commands.Key.v, true, false, false, false);
workbook.commandManager().setShortcutKey(undefined, GC.Spread.Commands.Key.s, true, false, false, false);
```

### 场景 5：JSON 序列化与恢复

```javascript
// 序列化工作簿
const json = workbook.toJSON();

// 恢复工作簿
workbook.fromJSON(json);
```

---

## 六、授权配置

项目通过以下方式配置 SpreadJS 许可证：

```javascript
// 优先使用客户手动配置的授权码
if (window.SpreadjsLicenseKey) {
    window.GC.Spread.Sheets.LicenseKey = window.SpreadjsLicenseKey;
} else {
    // 使用内置默认授权码
    window.GC.Spread.Sheets.LicenseKey = license;
}

// 设置中文语言
window.GC.Spread.Common.CultureManager.culture("zh-cn");
```

---

## 七、资源文件路径

| 文件类型 | 路径 |
|---------|------|
| 核心样式 | `/fna/js/spreadjs/gc.spread.sheets.11.1.0.css` |
| 主题样式 | `/fna/js/spreadjs/gc.spread.sheets.excel2013white.11.1.0.css` |
| 核心库 | `/fna/js/spreadjs/gc.spread.sheets.all.11.1.0.min.js` |
| Excel IO | `/fna/js/spreadjs/gc.spread.excelio.11.1.0.min.js` |
| 打印模块 | `/fna/js/spreadjs/gc.spread.sheets.print.11.1.0.min.js` |
| 中文资源 | `/fna/js/spreadjs/gc.spread.sheets.resources.zh.11.1.0.min.js` |

---

**文档版本**: v1.0  
**生成时间**: 2026-06-02  
**适用项目**: Ecology (D:\Weaver2020\ecology)