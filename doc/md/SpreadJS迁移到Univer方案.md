# Ecology 项目 SpreadJS 迁移到 Univer 详细方案

## 一、项目现状分析

根据对 Ecology 项目 (`D:\Weaver2020\ecology`) 的分析，当前 SpreadJS 使用情况如下：

| 模块 | SpreadJS 版本 | 核心功能 |
|------|--------------|---------|
| **excelDesign** | Wijmo Spread | 表单设计器画布、字段绑定 |
| **fnaMulDimensions** | GrapeCity 11.1.0 | 财务多维分析、维度填充 |
| **edc** | GrapeCity 11.1.0/12.2.2 | 数据采集 |
| **cube** | GrapeCity 11.1.0 | 多维分析报表 |
| **formmode** | GrapeCity 11.1.0 | 表单模式 |

---

## 二、迁移策略

### 2.1 总体策略：分阶段渐进式迁移

```
阶段一：基础设施准备
    ↓
阶段二：核心模块迁移（formmode）
    ↓
阶段三：复杂模块迁移（fnaMulDimensions）
    ↓
阶段四：设计器模块迁移（excelDesign）
    ↓
阶段五：全面替换与优化
```

### 2.2 迁移模式选择

| 模式 | 适用场景 | 复杂度 |
|------|---------|--------|
| **Preset 模式** | 快速启动，标准功能 | ⭐ |
| **Plugin 模式** | 精细控制，按需加载 | ⭐⭐⭐ |
| **混合模式** | 逐步迁移，平滑过渡 | ⭐⭐ |

**推荐**：采用**混合模式**，分模块逐步迁移。

---

## 三、迁移步骤详解

### 阶段一：基础设施准备

**步骤 1.1：环境准备**

```bash
# 克隆 Univer 仓库
git clone https://github.com/dream-num/univer.git

# 安装依赖
pnpm install

# 验证环境
pnpm dev
```

**步骤 1.2：依赖安装**

```bash
# 在 Ecology 项目中安装 Univer
pnpm add @univerjs/presets @univerjs/preset-sheets-core

# 或使用 Plugin 模式（推荐）
pnpm add @univerjs/core @univerjs/design @univerjs/docs @univerjs/docs-ui \
  @univerjs/engine-formula @univerjs/engine-render \
  @univerjs/sheets @univerjs/sheets-formula @univerjs/sheets-formula-ui \
  @univerjs/sheets-numfmt @univerjs/sheets-numfmt-ui @univerjs/sheets-ui @univerjs/ui
```

**步骤 1.3：创建适配层**

```
src4js/
├── pc4backstage/
│   ├── spreadAdapter/           # 新增：SpreadJS 适配层
│   │   ├── index.ts             # 统一导出
│   │   ├── SpreadAdapter.ts     # SpreadJS 兼容适配器
│   │   ├── UniverAdapter.ts     # Univer 适配器
│   │   └── adapterFactory.ts    # 适配器工厂
│   └── excelDesign/
│       ├── util/mainFrame/
│       │   └── spreadOperate.js # 原 SpreadJS 操作
│       └── components/          # Excel 设计器组件
```

---

## 四、核心 API 映射

### 4.1 工作簿初始化对比

| SpreadJS | Univer | 说明 |
|----------|--------|------|
| `new GC.Spread.Sheets.Workbook(container)` | `createUniver()` + `UniverSheetsCorePreset()` | 创建工作簿 |
| `wijspread({sheetCount:1})` | `{ sheet: { [sheetId]: {} } }` | 配置工作表 |
| `.getActiveSheet()` | `univerAPI.getActiveWorkbook().getActiveSheet()` | 获取活动表 |
| `.toJSON()` | `univerAPI.getActiveWorkbook().toJSON()` | 序列化 |

### 4.2 迁移示例代码

```typescript
// SpreadJS 旧代码
$("#container").wijspread({sheetCount: 1});
const spread = $("#container").wijspread("spread");
const sheet = spread.getActiveSheet();

// Univer 新代码
import { createUniver, UniverSheetsCorePreset } from '@univerjs/preset-sheets-core';
import '@univerjs/preset-sheets-core/lib/index.css';

const { univerAPI } = createUniver({
  presets: [
    UniverSheetsCorePreset({
      container: 'app',
    }),
  ],
});

const workbook = univerAPI.createWorkbook({});
const sheet = workbook.getActiveSheet();
```

### 4.3 单元格操作 API 映射

| SpreadJS | Univer | 说明 |
|----------|--------|------|
| `sheet.getCell(r, c)` | `sheet.getRange(r, c, 1, 1)` | 获取单元格 |
| `cell.value()` | `range.getValue()` | 获取值 |
| `cell.value(val)` | `range.setValue(val)` | 设置值 |
| `cell.text()` | `range.getText()` | 获取文本 |
| `cell.backColor(color)` | `range.getCell().setBackgroundColor(color)` | 背景色 |

---

## 五、分模块迁移

### 5.1 formmode 模块迁移

```typescript
// formmode/SpreadManager.ts
import { createUniver, UniverSheetsCorePreset } from '@univerjs/preset-sheets-core';

export class SpreadManager {
  private univerAPI: any;

  async initialize(containerId: string) {
    const { univerAPI } = createUniver({
      presets: [
        UniverSheetsCorePreset({
          container: containerId,
        }),
      ],
    });
    this.univerAPI = univerAPI;
  }

  createSheet(options: any) {
    return this.univerAPI.createWorkbook(options);
  }

  getActiveSheet() {
    return this.univerAPI.getActiveWorkbook().getActiveSheet();
  }

  // 数据验证迁移
  addDataValidation(range: string, options: any) {
    const sheet = this.getActiveSheet();
    sheet.getDataValidations().add(range, {
      type: options.type,
      operator: options.operator,
      formula1: options.formula1,
      formula2: options.formula2,
    });
  }
}
```

### 5.2 fnaMulDimensions 模块迁移

```typescript
// fnaMulDimensions/FnaSpreadManager.ts
export class FnaSpreadManager extends SpreadManager {
  private static instance: FnaSpreadManager;

  static getInstance() {
    if (!FnaSpreadManager.instance) {
      FnaSpreadManager.instance = new FnaSpreadManager();
    }
    return FnaSpreadManager.instance;
  }

  // 财务多维分析特有功能
  setDimensionData(dimType: string, data: any[]) {
    const sheet = this.getActiveSheet();
    // 维度数据设置
    data.forEach((item, index) => {
      sheet.getRange(index, 0, 1, 1).setValues([[item]]);
    });
  }

  // 事件绑定迁移
  bindCellEnterEvent(callback: Function) {
    const workbook = this.univerAPI.getActiveWorkbook();
    workbook.on(FUniver.Events.EnterCell, callback);
  }
}
```

### 5.3 excelDesign 模块迁移

```typescript
// excelDesign/DesignerAdapter.ts
export class DesignerAdapter {
  private spreadManager: SpreadManager;

  constructor() {
    this.spreadManager = new SpreadManager();
  }

  // 表单设计器特有功能
  enableDesignMode(enabled: boolean) {
    const sheet = this.spreadManager.getActiveSheet();
    if (enabled) {
      sheet.setIsProtected(false);
      // 禁用部分快捷键
    } else {
      sheet.setIsProtected(true);
    }
  }

  // 字段绑定
  bindField(fieldId: string, cellRange: string) {
    const sheet = this.spreadManager.getActiveSheet();
    const range = sheet.getRange(cellRange);
    // 设置字段绑定信息
    range.setTag({ fieldId });
  }
}
```

---

## 六、数据验证迁移

### 6.1 SpreadJS 数据验证 → Univer 数据验证

```typescript
// 迁移前 (SpreadJS)
var dv = new GC.Spread.Sheets.CellValidation();
dv.rule({
    type: GC.Spread.Sheets.CellValidationType.list,
    formula1: '["A", "B", "C"]',
    showInputMessage: true,
    inputMessage: "请选择"
});
sheet.setDataValidator(0, 0, 5, 5, dv);

// 迁移后 (Univer)
import { DataValidationType, DataValidationOperator } from '@univerjs/sheets-data-validation';

const dataValidation = {
    type: DataValidationType.LIST,
    operator: DataValidationOperator.EQUALS,
    formula1: '"A,B,C"',
    allowBlank: true,
    showError: true,
    error: '请从列表中选择',
    errorTitle: '输入无效'
};

sheet.getDataValidations().add('A1:F6', dataValidation);
```

### 6.2 支持的验证类型映射

| SpreadJS | Univer |
|----------|--------|
| `CellValidationType.list` | `DataValidationType.LIST` |
| `CellValidationType.number` | `DataValidationType.NUMBER` |
| `CellValidationType.dateTime` | `DataValidationType.DATE` |
| `CellValidationType.textLength` | `DataValidationType.TEXT_LENGTH` |
| `CellValidationType.custom` | `DataValidationType.FORMULA` |
| `CellValidationType.wholeNumber` | `DataValidationType WHOLE_NUMBER` |
| `CellValidationType.decimal` | `DataValidationType.DECIMAL` |

---

## 七、事件系统迁移

### 7.1 事件绑定对比

```typescript
// SpreadJS
workbook.bind(GC.Spread.Sheets.Events.EnterCell, (sender, args) => {
    console.log(args.row, args.col);
});

// Univer
const workbook = univerAPI.getActiveWorkbook();
workbook.onCellValueChange((cell: any, oldValue: any, newValue: any) => {
    console.log(cell.row, cell.col, oldValue, newValue);
});
```

### 7.2 支持的事件

| SpreadJS 事件 | Univer 事件 | 说明 |
|---------------|-------------|------|
| `EnterCell` | `onCellValueChange` | 单元格进入/值变化 |
| `SelectionChanged` | `onSelectionChange` | 选区变化 |
| `EditEnding` | `onBeforeValueChange` | 编辑结束前 |
| `EditEnded` | `onAfterValueChange` | 编辑结束后 |
| `ValueChanged` | `onValueChange` | 值变化 |
| `CellChanged` | `onCellValueChange` | 单元格变化 |
| `ClipboardChanged` | `onClipboardChange` | 剪切板变化 |

---

## 八、样式系统迁移

### 8.1 单元格样式对比

```typescript
// SpreadJS
var style = new GC.Spread.Sheets.Style();
style.backColor = "#FFFF00";
style.font = "bold 12px Arial";
style.hAlign = GC.Spread.Sheets.HorizontalAlign.center;
style.borderTop = new GC.Spread.Sheets.LineBorder("red", GC.Spread.Sheets.LineStyle.thin);
sheet.setStyle(0, 0, style);

// Univer
import { TextStyle, Color, HorizontalAlign, BorderType } from '@univerjs/core';

const style = new TextStyle({
    font: 'bold 12px Arial',
    horizontalAlign: HorizontalAlign.CENTER,
    backgroundColor: Color.YELLOW,
    borderTop: {
        style: BorderType.THIN,
        color: Color.RED
    }
});

sheet.getRange(0, 0, 1, 1).setStyle(style);
```

---

## 九、JSON 序列化迁移

### 9.1 数据结构转换

```typescript
// SpreadJS JSON 格式
{
    "version": "11.1.0",
    "sheets": [{
        "name": "Sheet1",
        "data": {
            "dataTable": [[cellData]]
        }
    }]
}

// Univer JSON 格式
{
    "id": "workbook-id",
    "sheetOrder": ["sheet-id"],
    "sheets": {
        "sheet-id": {
            "id": "sheet-id",
            "name": "Sheet1",
            "cellData": {
                "0": {
                    "0": { "v": "value", "s": styleIndex }
                }
            }
        }
    }
}
```

### 9.2 转换工具

```typescript
// utils/spreadConverter.ts
export class SpreadConverter {
  static toUniverFormat(spreadJson: any): any {
    const sheets: any = {};

    for (const [name, sheetData] of Object.entries(spreadJson.sheets)) {
      const sheetId = generateId();
      sheets[sheetId] = {
        id: sheetId,
        name: name,
        cellData: this.convertCellData(sheetData.data.dataTable),
        // 其他属性转换...
      };
    }

    return {
      id: generateId(),
      sheetOrder: Object.keys(sheets),
      sheets: sheets
    };
  }

  static toSpreadFormat(univerJson: any): any {
    // Univer → SpreadJS 格式转换
  }
}
```

---

## 十、关键技术难点与解决方案

| 难点 | SpreadJS 实现 | Univer 解决方案 | 迁移策略 |
|------|--------------|-----------------|---------|
| **公式引擎** | 内置完整 | `@univerjs/engine-formula` | 直接迁移 |
| **数据验证** | 7种类型 | 完整支持 | API 1:1映射 |
| **条件格式** | 内置 | `@univerjs/sheets-conditional-formatting` | 直接迁移 |
| **打印导出** | 原生支持 | `@univerjs/sheets-print` (Pro) | 需商业授权 |
| **图表** | 原生支持 | `@univerjs/sheets-charts` (Pro) | 需商业授权 |
| **快捷键** | 内置 | Command System | 自定义命令 |
| **撤销/重做** | 内置 | UndoRedoService | 自动支持 |

---

## 十一、迁移检查清单

```
□ 环境准备
  □ Node.js >= 22.18
  □ pnpm >= 10
  □ Git

□ 依赖安装
  □ @univerjs/presets
  □ @univerjs/preset-sheets-core
  □ @univerjs/core
  □ @univerjs/sheets
  □ @univerjs/sheets-ui

□ 适配层开发
  □ SpreadAdapter 创建
  □ UniverAdapter 创建
  □ AdapterFactory 创建
  □ 事件桥接开发

□ 模块迁移
  □ formmode 模块
  □ fnaMulDimensions 模块
  □ excelDesign 模块
  □ edc 模块
  □ cube 模块

□ 功能验证
  □ 单元格操作
  □ 数据验证
  □ 样式设置
  □ 公式计算
  □ JSON 序列化

□ 性能测试
  □ 加载速度
  □ 大数据量性能
  □ 内存占用
```

---

## 十二、推荐实施计划

| 阶段 | 时间 | 内容 | 风险等级 |
|------|------|------|---------|
| **POC** | 1-2周 | formmode 模块可行性验证 | 低 |
| **MVP** | 2-4周 | formmode + fnaMulDimensions 完整迁移 | 中 |
| **Beta** | 4-8周 | excelDesign 模块 + 全功能验证 | 中 |
| **Stabilize** | 2-4周 | 性能优化 + Bug 修复 | 低 |
| **GA** | 1-2周 | 全面上线 | 低 |

---

## 十三、Univer 核心模块一览

```
univer
├── @univerjs/base          # 核心基础模块
├── @univerjs/ui             # UI 组件
├── @univerjs/docs           # 文档模块
├── @univerjs/sheets         # 表格核心
├── @univerjs/sheets-ui      # 表格 UI
├── @univerjs/sheets-data-validation   # 数据验证
├── @univerjs/sheets-formula # 公式引擎
├── @univerjs/sheets-facula  # 导入导出
├── @univerjs/engine-render  # 渲染引擎
├── @univerjs/engine-formula # 公式计算引擎
├── @univerjs/design         # 设计系统
└── @univerjs/client         # 客户端
```

---

## 十四、总结

| 项目 | SpreadJS | Univer |
|------|----------|--------|
| **授权** | 商业付费 | Apache 2.0 (开源) |
| **架构** | 组件库 | 插件化架构 |
| **协同** | ❌ 不支持 | ✅ 原生支持 |
| **公式引擎** | 完整 | 完整 |
| **数据验证** | 完整 | 完整 |
| **性能** | 一般 | Canvas 渲染，更优 |
| **移动端** | 有限 | 原生支持 |
| **维护** | 厂商维护 | 社区活跃 |

### 迁移价值

- **成本降低**：无需商业授权费用
- **性能提升**：Canvas 渲染优化
- **未来扩展**：插件架构支持更多功能
- **社区支持**：活跃的开源社区

---

**文档版本**: v1.0
**生成时间**: 2026-06-02
**适用项目**: Ecology (D:\Weaver2020\ecology)
**目标平台**: Univer (https://github.com/dream-num/univer)