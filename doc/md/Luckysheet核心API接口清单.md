# Luckysheet 核心 API 接口清单

## 概述

Luckysheet 是由字节跳动开发的开源电子表格组件，是 SpreadJS 的开源替代品。它提供了丰富的 API 接口，支持单元格操作、工作表管理、数据导入导出等功能。

---

## 一、初始化与配置

| API | 说明 | 示例 |
|-----|------|------|
| `LuckySheet.buildGridByDataStore(data)` | 从数据结构构建网格 | `LuckySheet.buildGridByDataStore(data)` |
| `LuckySheet.create(options)` | 创建工作簿实例 | `LuckySheet.create({ container: 'luckysheet', showtoolbar: true })` |
| `LuckySheet.destroy()` | 销毁工作簿实例 | `LuckySheet.destroy()` |

---

## 二、获取实例

| API | 说明 |
|-----|------|
| `getluckysheetfile()` | 获取当前工作簿文件对象 |
| `getSheetByIndex(index)` | 根据索引获取工作表 |
| `getSheetByName(name)` | 根据名称获取工作表 |
| `getActiveSheet()` | 获取当前活动工作表 |
| `getconfig()` | 获取工作簿配置 |
| `luckysheet_getGridData()` | 获取网格数据 |

---

## 三、单元格操作

| API | 说明 |
|-----|------|
| `getCellValue(row, col, options)` | 获取单元格值 |
| `setCellValue(row, col, value, options)` | 设置单元格值 |
| `getRangeValue(range)` | 获取范围单元格值 |
| `setRangeValue(range, value)` | 设置范围单元格值 |
| `getCellFormat(row, col)` | 获取单元格格式 |
| `setCellFormat(row, col, format)` | 设置单元格格式 |

---

## 四、工作表操作

| API | 说明 |
|-----|------|
| `sheetAdd(name, options)` | 添加新工作表 |
| `sheetDelete(index)` | 删除工作表 |
| `sheetCopy(index)` | 复制工作表 |
| `sheetMove(index, targetIndex)` | 移动工作表 |
| `sheetRename(name)` | 重命名工作表 |
| `sheetVisible(index, visible)` | 设置工作表可见性 |
| `sheetActiveIndex(index)` | 激活指定工作表 |

---

## 五、选区操作

| API | 说明 |
|-----|------|
| `getSelection()` | 获取当前选区 |
| `setSelection(range)` | 设置选区 |
| `getRangeAxis()` | 获取选区坐标 |
| `getAxisString(row, col)` | 转换行列为坐标字符串 |

---

## 六、行列操作

| API | 说明 |
|-----|------|
| `insertRow(index, count)` | 插入行 |
| `deleteRow(index, count)` | 删除行 |
| `insertColumn(index, count)` | 插入列 |
| `deleteColumn(index, count)` | 删除列 |
| `setRowHeight(index, height)` | 设置行高 |
| `setColumnWidth(index, width)` | 设置列宽 |
| `getRowHeight(index)` | 获取行高 |
| `getColumnWidth(index)` | 获取列宽 |
| `hideRow(index, count)` | 隐藏行 |
| `hideColumn(index, count)` | 隐藏列 |

---

## 七、合并单元格

| API | 说明 |
|-----|------|
| `mergeCells(range, type)` | 合并单元格 |
| `cancelMerge(range)` | 取消合并 |
| `getMerge()` | 获取所有合并单元格信息 |

---

## 八、行列标题

| API | 说明 |
|-----|------|
| `rowHeaderAndColumnHeaderExist()` | 行列标题是否存在 |
| `createRowHeader(data)` | 创建行标题 |
| `createColumnHeader(data)` | 创建列标题 |

---

## 九、筛选操作

| API | 说明 |
|-----|------|
| `filterOptionsExist()` | 筛选选项是否存在 |
| `createFilter(range, options)` | 创建筛选 |
| `clearFilter()` | 清除筛选 |

---

## 十、数据验证

| API | 说明 |
|-----|------|
| `dataVerificationExist()` | 数据验证是否存在 |
| `dataVerificationBindSheet()` | 绑定数据验证到工作表 |
| `clearDataVerification()` | 清除数据验证 |

---

## 十一、条件格式

| API | 说明 |
|-----|------|
| `createHierarchyComposedMap(data)` | 创建条件格式 |
| `getComposedHierarchyData()` | 获取条件格式数据 |

---

## 十二、图表操作

| API | 说明 |
|-----|------|
| `createChart(chartData)` | 创建图表 |
| `deleteChart(chartId)` | 删除图表 |
| `updateChart(chartId, chartData)` | 更新图表 |

---

## 十三、数据透视表

| API | 说明 |
|-----|------|
| `createPivotTable(options)` | 创建数据透视表 |
| `deletePivotTable(id)` | 删除数据透视表 |
| `refreshPivotTable(id)` | 刷新数据透视表 |

---

## 十四、命名管理器

| API | 说明 |
|-----|------|
| `createNamedFunction(name, formula)` | 创建命名公式 |
| `deleteNamedFunction(name)` | 删除命名公式 |
| `getNamedFunctionAll()` | 获取所有命名公式 |

---

## 十五、撤销/重做

| API | 说明 |
|-----|------|
| `undo()` | 撤销 |
| `redo()` | 重做 |
| `hasUndo()` | 是否有撤销操作 |
| `hasRedo()` | 是否有重做操作 |

---

## 十六、数据操作

| API | 说明 |
|-----|------|
| `insertCellValue(value, range)` | 插入单元格值 |
| `deleteCellValue(range)` | 删除单元格值 |
| `clearCell(range)` | 清除单元格内容 |
| `clearCellAll()` | 清除所有单元格 |

---

## 十七、复制/粘贴

| API | 说明 |
|-----|------|
| `copy(range)` | 复制选区 |
| `paste(options)` | 粘贴 |
| `cut(range)` | 剪切选区 |
| `isClipboardEmpty()` | 剪切板是否为空 |

---

## 十八、国际化

| API | 说明 |
|-----|------|
| `changeLang(lang)` | 切换语言 |
| `getLang()` | 获取当前语言 |

---

## 十九、导出功能

| API | 说明 |
|-----|------|
| `luckysheet_toPdf()` | 导出为 PDF |
| `luckysheet_toHtml()` | 导出为 HTML |
| `luckysheet_getExcelData()` | 获取 Excel 数据 |

---

## 二十、事件监听

| API | 说明 |
|-----|------|
| `on(cellSelected, callback)` | 监听单元格选中事件 |
| `on(cellEdited, callback)` | 监听单元格编辑事件 |
| `on(sheetSelected, callback)` | 监听工作表选中事件 |
| `on(sheetChanged, callback)` | 监听工作表变化事件 |
| `on(rangeSelected, callback)` | 监听范围选中事件 |
| `on(editEnded, callback)` | 监听编辑结束事件 |
| `off(event, callback)` | 取消事件监听 |

---

## 二十一、UI 控制

| API | 说明 |
|-----|------|
| `showtoolbar(show)` | 显示/隐藏工具栏 |
| `showtoolbarById(id, show)` | 显示/隐藏指定工具栏按钮 |
| `showFormatBoard(show)` | 显示/隐藏格式面板 |
| `showStatusBar(show)` | 显示/隐藏状态栏 |
| `showinfo(a)` | 显示/隐藏信息栏 |
| `showSheetViewBar(show)` | 显示/隐藏工作表视图栏 |

---

## 二十二、主题设置

| API | 说明 |
|-----|------|
| `changeTheme(theme)` | 切换主题 |
| `getTheme()` | 获取当前主题 |

---

## 二十三、公式相关

| API | 说明 |
|-----|------|
| `getFormulaString(row, col)` | 获取单元格公式 |
| `setFormulaString(formula, range)` | 设置公式 |
| `getAllFunctionFormula()` | 获取所有函数公式 |
| `runCopysheetFunction()` | 执行复制工作表函数 |

---

## 二十四、批注操作

| API | 说明 |
|-----|------|
| `insertComment(row, col, comment)` | 插入批注 |
| `deleteComment(row, col)` | 删除批注 |
| `updateComment(row, col, comment)` | 更新批注 |
| `getComments()` | 获取所有批注 |

---

## 二十五、冻结行列

| API | 说明 |
|-----|------|
| `freezeRow(rowIndex)` | 冻结行 |
| `freezeColumn(colIndex)` | 冻结列 |
| `freezeRowColumn(rowIndex, colIndex)` | 冻结行和列 |
| `cancelFrozen()` | 取消冻结 |

---

## 二十六、打印设置

| API | 说明 |
|-----|------|
| `setPrintBooleans(options)` | 设置打印选项 |
| `getPrintInfo()` | 获取打印信息 |

---

## 二十七、数据导入

| API | 说明 |
|-----|------|
| `luckysheetImportExcel(file, callback)` | 导入 Excel 文件 |
| `luckysheetImportExcelById(id, file)` | 通过 ID 导入 Excel |
| `importExcelUrl(url, callback)` | 通过 URL 导入 Excel |

---

## 二十八、API 分类速查表

### 初始化类
```
LuckySheet.create()
LuckySheet.destroy()
LuckySheet.buildGridByDataStore()
```

### 单元格操作类
```
getCellValue()
setCellValue()
getRangeValue()
setRangeValue()
getCellFormat()
setCellFormat()
```

### 工作表管理类
```
sheetAdd()
sheetDelete()
sheetCopy()
sheetMove()
sheetRename()
sheetVisible()
sheetActiveIndex()
```

### 数据操作类
```
insertRow()
deleteRow()
insertColumn()
deleteColumn()
insertCellValue()
deleteCellValue()
clearCell()
```

### 选区操作类
```
getSelection()
setSelection()
getRangeAxis()
getAxisString()
```

### 撤销重做类
```
undo()
redo()
hasUndo()
hasRedo()
```

### 导入导出类
```
luckysheet_toPdf()
luckysheet_toHtml()
luckysheet_getExcelData()
luckysheetImportExcel()
```

### 事件监听类
```
on()
off()
```

---

## 二十九、典型使用示例

### 示例 1：初始化 Luckysheet

```javascript
LuckySheet.create({
    container: 'luckysheet',
    showtoolbar: true,
    showinfobar: true,
    showsheetviewbar: true,
    showstatisticBar: true,
    lang: 'zh'
});
```

### 示例 2：获取和设置单元格值

```javascript
// 获取单元格值
const value = getCellValue(0, 0);

// 设置单元格值
setCellValue(0, 0, 'Hello World');

// 获取范围值
const rangeValues = getRangeValue('A1:C3');

// 设置范围值
setRangeValue('A1:C3', [[1, 2, 3], [4, 5, 6]]);
```

### 示例 3：监听选区变化

```javascript
$('#luckysheet').on('cellSelected', function (data) {
    console.log('选中的单元格:', data);
    console.log('行:', data.row);
    console.log('列:', data.col);
});
```

### 示例 4：工作表操作

```javascript
// 添加新工作表
sheetAdd('新工作表');

// 激活工作表
sheetActiveIndex(1);

// 重命名工作表
sheetRename('销售报表');

// 删除工作表
sheetDelete(2);
```

### 示例 5：行列操作

```javascript
// 插入行
insertRow(0, 2);

// 删除列
deleteColumn(1, 3);

// 设置行高
setRowHeight(0, 50);

// 隐藏列
hideColumn(2, 1);
```

### 示例 6：合并单元格

```javascript
// 合并单元格
mergeCells('A1:C3', 'merge-all');

// 取消合并
cancelMerge('A1:C3');
```

### 示例 7：导入 Excel

```javascript
luckysheetImportExcel(file, function(data) {
    console.log('导入成功:', data);
});
```

### 示例 8：导出为 PDF

```javascript
luckysheet_toPdf();
```

---

## 三十、与 SpreadJS API 对比

| 功能 | SpreadJS | Luckysheet |
|------|----------|------------|
| 初始化 | `new GC.Spread.Sheets.Workbook()` | `LuckySheet.create()` |
| 获取活动表 | `workbook.getActiveSheet()` | `getActiveSheet()` |
| 获取单元格 | `sheet.getCell(row, col)` | `getCellValue(row, col)` |
| 设置单元格值 | `cell.value(val)` | `setCellValue(row, col, val)` |
| 绑定事件 | `workbook.bind(event, handler)` | `on(event, callback)` |
| JSON 序列化 | `workbook.toJSON()` | `getluckysheetfile()` |
| 导出 Excel | `GC.Spread.Excel.IO.save()` | `luckysheet_toHtml()` |

---

**文档版本**: v1.0
**生成时间**: 2026-06-02
**适用项目**: 通用
**开源地址**: https://github.com/mengshukeji/Luckysheet