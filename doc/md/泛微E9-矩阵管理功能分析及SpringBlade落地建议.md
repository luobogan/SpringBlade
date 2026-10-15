# 泛微 E9 矩阵（Matrix）管理功能分析

> 分析范围：`D:\workproject\springbladeandreact\矩阵`（功能说明文档，重点）、`D:\workproject\ecology`（E9 Java 源码）、`D:\Weaver2020\ecology`（E9 部署态：JSP/JS/class）
> 文档目的：识别矩阵模块的核心功能、模块结构、接口设计、前端组件与后端逻辑，为 SpringBlade 侧落地提供依据
> 分析日期：2026-10-14

---

## 一、功能概述

### 1.1 什么是矩阵

矩阵管理是用**二维表**的方式，把企业中「分部 / 部门 × 固定岗位」的人员固化下来。

官方定义（摘自 `矩阵/什么是矩阵.txt`）：

> 矩阵管理是通过矩阵的方式将企业中分部、部门固定岗位的人员给定下来，比如一个集团企业，每个分公司都有财务经理、出纳，那么在流程申请中怎么样才能快速准确得找到我所属分公司的财务经理和出纳呢，通过矩阵就可以办到，我们将岗位做为横行，分公司做为列，那么就形成了一张矩阵表，在每个分公司所对应财务经理和出纳岗位上加上实际的用户就可以让申请人快速找到自己分公司的财务经理了。

> 矩阵管理另外一个好处是能够应对人员流动，当这些岗位的人员发生变动的时候，我们将不需要到每个涉及此岗位的流程中进行调整了，只要在矩阵表中直接调整就可以了。

**核心价值**：

1. **快速定位跨组织办理人** —— 申请人提交流程时，按自己所属组织自动匹配到对应岗位人员
2. **人员变动单点维护** —— 岗位人员调整只改矩阵，无需逐个修改流程节点操作者
3. **批量替换** —— 提供「批量维护」，把某个人员在所有矩阵位置一次性替换/删除

### 1.2 核心概念

| 概念 | 说明 |
|---|---|
| **条件维度**（黄色字段） | 矩阵的「列/查询键」，通常是**浏览按钮**字段（分部 164/194、部门 4/57 等）。多个条件维度之间是 **AND** 关系，必须全部满足才能命中结果 |
| **结果维度**（蓝色字段） | 矩阵的「取值列」，默认 3 个**多人力资源**（浏览按钮 type=17）字段，即各岗位人员。可增列 |
| **矩阵数据表** | 动态建表 `Matrixtable_<matrixid>`，一行 = 一条矩阵数据（一个组织 + 各岗位人员） |
| **默认矩阵** | 系统内置两张：**分部矩阵**（`issystem=1`）、**部门矩阵**（`issystem=2`）。条件维度自动跟随组织机构变动，无需维护 |
| **自定义矩阵** | 用户自建，条件维度/结果维度均手工设计 |
| **矩阵管理员** | 单独授权某人维护某个矩阵。权限分两级：**编辑权限**（受「可维护条件」「可维护列」限制）、**完全控制权限**（含字段维护，不受限制） |
| **批量维护** | 人员变动后，批量将结果维度中的人员「替换为」某人或「删除」 |

### 1.3 典型场景（财务矩阵）

> 一个集团公司，每个分公司都有财务部，财务部中有财务经理、会计、出纳岗位。

```
                财务经理      会计       出纳          ← 结果维度（多人力资源）
北京分公司      张三         李四       王五
上海分公司      赵六         钱七       孙八
广州分公司      周九         吴十       郑十一
   ↑
条件维度（分部）
```

流程节点操作者配置「矩阵」后，申请人选了「上海分公司」，节点办理人自动解析为赵六（财务经理列）。

### 1.4 功能模块清单

| 模块 | 说明 |
|---|---|
| 默认矩阵 | 分部矩阵 / 部门矩阵，只读维护数据，条件维度自动跟随组织 |
| 自定义矩阵 | 新建、编辑名称、删除、矩阵管理员、字段数据维护 |
| 字段维护 | 设计条件维度（黄）与结果维度（蓝），填显示名 + 数据库名称 |
| 数据维护 | 添加行、条件维度至少填一个、列筛选、全路径显示 |
| 导入导出 | 导出 Excel；导入分「新增」「更新」两种模板 |
| 清除无效数据 | 清除离职人员、封存的分部/部门 |
| 批量维护 | 按矩阵 + 条件字段 + 取值字段，批量替换/删除人员 |
| 流程节点操作者 | 节点操作者类型 = 矩阵（type=99），按表单字段动态取人 |

---

## 二、数据模型

> 说明：E9 源码仓库内**没有 `.sql` 脚本**，矩阵相关表 DDL 全部由 Java 动态生成（`AddMatrixDesignCmd` L188、`MatrixUtil.addSql` L973）。

### 2.1 表清单

| 表名 | 用途 | 关键字段 |
|---|---|---|
| `matrixinfo` | 矩阵主表（一个矩阵 = 一张二维表定义） | `id, name, descr, priority, createdate, createtime, createrid, issystem(1=分部系统矩阵, 2=部门系统矩阵), subcompanyid, uuid` |
| `matrixfieldinfo` | 矩阵字段定义 | `id, matrixid, browsertypeid, browservalue, custombrowser, displayname, fieldname, fieldtype(0=条件维度, 1=结果维度), priority, colwidth` |
| `Matrixtable_<matrixid>` | **矩阵数据表**（动态建表，一行 = 一条矩阵数据） | 固定列 `uuid varchar(100)`、`dataorder float`，其余为每个 `matrixfieldinfo.fieldname` 的 `varchar(100)` 列 |
| `MatrixMaintInfo`（亦写作 `matrixmaintinfo`） | 矩阵维护者明细（人/角色） | `id, matrixid, maintid, type(1人力资源/4角色), resourceid, roleid, rolelevel, seclevel, showseclevelend, subcompanyid, departmentid, foralluser` |
| `matrix_maint_info` | 维护者组（一次维护设置） | `id, matrixid, name(uuid), rightlevel(1编辑/2完全控制)` |
| `matrix_maint_condition` | 维护者的**可维护范围（条件）** | `maintid, matrixid, mainttype(1人力/4部门/7客户/8项目/24岗位/161/162自定义/164分部), typevalue(1总部/2同部门/3同分部/4同部门及下级/5同分部及下级/6多分部/7多部门), typebrowservalue` |
| `matrix_maint_col` | 维护者的**可维护列**（结果维度列） | `maintid, matrixid, colid, colname` |
| `matrix_colset` | 数据维护页的列显示名/排序定制 | `matrixid, nameset, orderset, userid` |
| **`workflow_groupdetail_matrix`** | **节点操作者=矩阵 的配置主表** | `groupdetailid, matrix(矩阵id), value_field(结果维度字段id)` |
| **`workflow_matrixdetail`** | **节点操作者=矩阵 的条件明细**（一行一个条件，AND） | `groupdetailid, condition_field(矩阵条件字段id), workflow_field(流程字段id / -1指定分部 / -2指定部门 / -12创建人部门 / -13创建人分部), workflow_objid, workflow_bhxj(0本级/1上级/2下级/3逐级), virtualid` |
| `workflow_urger_matrix` / `workflow_urger_matrix_detail` | 督办人矩阵（同构） | 同上 |
| `workflow_bdf_dr_matrix` | 浏览数据范围矩阵 | — |
| `subcompanyDefineField` / `departmentDefineField` | **分部字段定义 / 部门字段定义**（默认矩阵结果维度来源） | `fieldname, fieldlabel, isopen, dsporder` |
| `hrm_formfield` + `hrm_fieldgroup` | E8 版分部/部门自定义字段（`grouptype=4` 分部，`grouptype=5` 部门） | — |
| `hrmsubcompanydefined` / `hrmdepartmentdefined` | 分部/部门自定义字段**值**表 | — |

### 2.2 动态建表机制

`weaver/matrix/MatrixUtil.java`：

```java
public static String MATRIXPREFIX = "Matrixtable_";   // L31 矩阵数据表名前缀
public static String UUIDCOLUMN   = "uuid";           // L32
public static String DATAORDERCOLUMN = "dataorder";   // L33
```

建表（`AddMatrixDesignCmd.java` L188）：

```java
"create table " + matrixtablename + " ( uuid varchar(100), dataorder float, <字段> varchar(100) ...)"
```

即：**每建一个矩阵，就动态创建一张 `Matrixtable_<id>` 物理表**；字段维护里每个字段对应一个 `varchar(100)` 列。这是矩阵能承载任意条件/结果维度组合的实现基础。

### 2.3 条件维度 / 结果维度的区分

`com/engine/hrm/cmd/matrix/biz/MatrixFieldsComInfo.java` L59-63 注释明确：

```
0 是条件字段（黄色）
1 是取值字段（蓝色）
```

结果维度默认浏览按钮为 **17（多人力资源）**，见 `MatrixDesignList.jsp` L194：

```jsp
<option value='17'>多人力资源</option>
```

---

## 三、关键实现路径

### 3.1 后端核心类

#### 3.1.1 引擎层 `weaver.matrix`（最核心）

**`d:\workproject\ecology\src\weaver\matrix\MatrixUtil.java`**（92KB，矩阵核心工具类）

| 行 | 方法 | 作用 |
|---|---|---|
| L489/522/540/543 | `getMatrixUsers(String matrixid, String selectFieldId, Map<String,String> matchs, [int workflowBhxj], [RecordSet rs])` | **核心：按条件维度取值 → 返回人员 id 串** |
| L675/679 | `getMatrixUsers_4_system(...)` | 系统矩阵（分部/部门矩阵）逐级向上找人的分支 |
| L436 | `getMatrixList()` | 所有矩阵（`select id,name from MatrixInfo`） |
| L455 | `getMatrixSelectList(matrixid)` | **结果维度**：`MatrixFieldInfo where fieldtype='1'` |
| L474 | `getMatrixMatchList(matrixid)` | **条件维度**：`fieldtype='0'` |
| L832/855/877/899 | `getFieldName / getFieldNameNew / getFieldType / getFieldID` | 字段 id ↔ 物理列名 / 浏览框类型互转 |
| L51/131/136 | `getMatrixJsonById(matrixid[, lowerCase])` | 矩阵设计 JSON |
| L171 | `matrixDelMark(matrixid)` | 能否删除（表里有数据则不能删） |
| L949/1010 | `addSql / addSql2(...)` | 建表并灌数据 |
| L1077/1121 | `sysSubcompanyToMatrix / sysDepartToMatrix` | 分部/部门字段定义 → 矩阵字段 |
| L1846/1872 | `createMatrixInfo(...) / createMatrixInfo2(..., isSystem)` | 建矩阵主记录 |
| L2198/2205/2213/2220 | `sysSubcompayData() / sysDepartmentData() / updateDepartmentData(id) / updateSubcompayData(id)` | 分部/部门变更时回写矩阵 |
| L1918 | `isUsed(fieldname, issystem)` | 判断矩阵字段是否在流程中被引用 |

**取人核心逻辑（L543-651）** —— 多条件 AND，多值用 `in`，多选浏览框用 `like '%,v,%'`：

```java
public String getMatrixUsers(String matrixid, String selectFieldId, Map<String,String> matchs, RecordSet rs){
    String selectFieldName = getFieldNameNew(matrixid, selectFieldId, rs);   // L563 结果维度物理列名
    StringBuffer conditions = new StringBuffer();
    for(Map.Entry<String, String> entry: matchs.entrySet()) {
        String value = entry.getValue().trim();
        String selectFieldType = getFieldType(entry.getKey(), rs);
        if("162".equals(selectFieldType)){            // 多选浏览框
            conditions.append(" and ','||").append(getFieldName(entry.getKey(),rs))
                      .append("||',' like ").append("'%,").append(value).append(",%' ");
        }else{
            ...
            conditions.append(" and ").append(conditionField)
                      .append(" in ").append("(").append(valueStr).append(")");   // L612
        }
    }
    String sql = "select "+selectFieldName+" from "+MATRIXPREFIX+matrixid+" where 1=1 "+conditions;  // L622
    rs.executeSql(sql);
    ...
    return CollectionUtil.list2String(users, ",");   // L650
}
```

`workflowBhxj=3`（逐级）时在 L491-516 循环上翻分部/部门，逐层调用直到命中。

**`d:\workproject\ecology\src\weaver\matrix\MatrixManager.java`**（维护权限）

```java
public List<String> getUserPermissionMatrixids(User user)                              // L32 可维护的矩阵 id
public boolean isUserPermissionMatrixids(User user)                                    // L44 是否矩阵维护者
public MatrixCondition getMatrixConditionByMaintUser(String resourceid, String matrixid) // L598
public String getConditionRanges(String maintid, String lang)                          // L388 总的可维护范围
public String getColRanges(String maintid, String lang)                                // L479 可维护列
public String getSubcompany(String id) / getDepartment(String id)                      // L209/L252 全路径名
```

其它：`MatrixImportProcess.java`、`MatrixImportProcessE9.java`（Excel 导入）。

#### 3.1.2 缓存层 `com.engine.hrm.cmd.matrix.biz`

| 文件 | 说明 |
|---|---|
| `MatrixinfoComInfo.java` | `TABLE_NAME="matrixinfo"` |
| `MatrixFieldsComInfo.java` | `TABLE_NAME="matrixfieldinfo"`；`getFieldsByMatrixid()`（L227）按 fieldtype 分组 |
| `MatrixMaintComInfo.java` | 维护者缓存，聚合 `MatrixMaintInfo` + `matrix_maint_condition` + `matrix_maint_col` + `matrix_maint_info.rightLevel` |
| `MatrixMaintColComInfo.java` / `MatrixMaintConditionComInfo.java` / `MatrixMaintMemberComInfo.java` | 可维护列 / 可维护条件 / 维护者成员 |
| `biz/chain/MatrixCondition.java` | 维护范围模型：`resids/deptids/subids/crmids/prjids/jobids/colids/colnames/rightLevel(1编辑 2完全控制)` |
| `biz/chain/Condition1Chain/4/7/8/24/164Chain` | 责任链，按 `mainttype`（1人力/4部门/7客户/8项目/24岗位/164分部）解析可维护范围 |

#### 3.1.3 流程侧

| 文件 | 说明 |
|---|---|
| `weaver/workflow/workflow/GroupDetailMatrix.java` | `workflow_groupdetail_matrix` DAO |
| `weaver/workflow/workflow/GroupDetailMatrixDetail.java` | `workflow_matrixdetail` DAO：`readAll(rs, groupdetailid)` |
| `weaver/workflow/request/MatrixConvert.java` | E8→E9 升级：把老的分部/部门自定义字段操作者转成矩阵（`update workflow_groupdetail set type=99`） |
| `com/engine/workflow/util/MatrixUtil.java` | 节点设置**回显**；区分 `operatorMatrix` / `superviseMatrix` / `datarangeMatrix`（L184-190） |
| **`com/engine/workflow/biz/nodeOperator/item/MartrixItem.java`** | **E9 运行时解析矩阵操作者** |
| `com/engine/workflow/cmd/workflowPath/node/operatorSetting/GetMatrixbrowserCmd.java` | 前端「矩阵取值字段/条件字段/流程字段」下拉数据 |

### 3.2 操作者类型常量 —— 矩阵 = **99**

`d:\workproject\ecology\src\com\engine\workflow\constant\node\OperatorDBType.java` L131-132：

```java
//矩阵
MARTRIX(99, 301, 3, "110"),
```

`d:\workproject\ecology\src\com\engine\workflow\constant\node\OperatorBigType.java` L22：

```java
MARTRIX(3, 382790),   // 3 = 矩阵 tab
```

数据库落库即 `workflow_groupdetail.type = 99`。

保存入口 `DoSaveOperatorGroupInfoCmd.java` L361-365：

```java
//矩阵特殊处理
if(dbtype == 99){
    String matrix = Util.null2String(params.get("add_matrix_"+i));
    String value_field = Util.null2String(params.get("add_value_field_"+i));
    rst.executeUpdate("INSERT INTO workflow_groupdetail_matrix(groupdetailid,matrix,value_field) VALUES(?,?,?)", ...);
```

### 3.3 前端实现

E9 存在**两套 UI**。

#### 3.3.1 老版（E8 风格 JSP）—— `D:\Weaver2020\ecology\matrixmanage\pages\`

| 功能 | 路径 |
|---|---|
| 列表页外壳 | `Matrix.jsp` |
| **矩阵列表**（卡片式，含新建/编辑/维护者/字段维护/删除） | `MatrixList.jsp` |
| 新建 / 编辑 | `MatrixAdd.jsp` / `MatrixEdit.jsp?id=` |
| 后端（add/edit/delete/init） | `MatrixOperation.jsp?method=` |
| 字段维护 + 数据维护 双 Tab 外壳 | `matrixdesign.jsp?matrixid=&showtype=0\|1&issystem=` |
| **字段维护**（黄蓝设计器） | `MatrixDesignList.jsp?matrixid=` |
| **数据维护**（矩阵表格，增行/删行/保存/导入/导出） | `matrixtable.jsp?matrixid=&canedit=&issystem=` |
| 字段保存 / 数据保存 | `saveitems.jsp` / `savetablerecords.jsp` |
| 删行 / 校验 / 列数据 | `deletetabledatas.jsp`、`checkdatas.jsp`、`getcolumndata.jsp` |
| 矩阵管理员 | `MatrixMaint.jsp?matrixid=` / `MatrixMaintAdd.jsp` / `MatrixMaintOperation.jsp` |
| 批量维护 | `MassMaint.jsp` / `MassMaintIframe.jsp` / `MassMaintOperation.jsp` |
| 导入 / 导出 | `MatrixImport.jsp?matrixid=&issystem=` / `MatrixImportProcess.jsp` / `MatrixExportToFile.jsp?method=exportData\|exportDataNoSQ` |
| 浏览按钮 | `matrixbrowser.jsp` / `matrixfieldbrowser.jsp?matrixid=&fieldtype=0\|1` |
| JS / CSS | `matrixmanage/js/matrixdesign_wev8.js`、`matrixtable_wev8.js`、`matrix_pageshow_wev8.js`；`css/matrix_wev8.css` 等 |

`MatrixList.jsp` 四类操作入口（L111-140）：

```jsp
dialog.URL = "/matrixmanage/pages/MatrixAdd.jsp";                                  // 新建
dialog.URL = "/matrixmanage/pages/MatrixEdit.jsp?id="+id;                          // 编辑
dialog.URL = "/matrixmanage/pages/matrixdesign.jsp?matrixid="+id+"&showtype="+type+"&issystem="+issystem; // 字段/数据维护
dialog.URL = "/matrixmanage/pages/MatrixMaint.jsp?matrixid="+id;                   // 矩阵管理员
```

系统/自定义矩阵区分（L216-259）：`issystem` 为空 = 自定义（可编辑/字段维护/删除），非空 = 分部/部门默认矩阵。

#### 3.3.2 新版 SPA（E9 主力，React/MobX）

入口：`D:\Weaver2020\ecology\spa\hrm\engine.html`

路由 `D:\Weaver2020\ecology\src4js\pc4backstage\hrmengine\index.js` L186-188：

```js
<Route name="matrix"         path="matrix"           component={Matrix}/>
<Route name="matrixDataView" path="matrix/:matrixid" component={MatrixDataMaintenanceView}/>
<Route name="matrixAdjust"   path="matrixAdjust"     component={MatrixAdjust}/>
```

访问地址：`/spa/hrm/engine.html#/hrmengine/matrix`、`#/hrmengine/matrix/{matrixid}`、`#/hrmengine/matrixAdjust`

组件（均在 `src4js\pc4backstage\hrmengine\components\matrix\`）：

| 组件 | 作用 |
|---|---|
| `index.js` | 模块容器（左侧组织树 + Tab） |
| `Dashboard.js` | 矩阵卡片墙 + 弹出对话框 |
| `MatrixCard.js` | 单卡片：删除 / 字段维护 / **矩阵管理员** / 编辑 / 点名称进数据维护 |
| `MatrixEdit.js` | 新建/编辑矩阵 |
| `MatrixFieldSetting.js` | **字段维护**：`condition.map`（条件维度）+ `val.map`（结果维度） |
| `MatrixDataMaintenance.js` / `MatrixDataMaintenanceView.js` | **数据维护**：`WeaTableEdit`、`DataList`、列筛选 `Screen`、全路径 `SubCompanyDisplaySetting` |
| `matrixAdjust/index.js` + `Dashboard/Content/Result` | **矩阵批量调整** |
| `matrix/maintenance/MatrixMaintenance.js` | 矩阵管理员（三个 Tab：维护者 / 可维护条件 / 可维护列） |

Store：`stores\matirx.js`（注意文件名为 `matirx`）、`matirxDataMaintenance.js`、`matrixMaintenance.js`、`matrixAdjust.js`
样式：`style\matrix.less`、`matrixAdjust.less`、`matrixDataMaintenance.less`、`matrixFieldSetting.less`

#### 3.3.3 流程节点操作者引用矩阵（前端）

- 容器：`src4js\pc4backstage\workflow\components\pathSet\nodeSet\OperatorSet.js`
  ```js
  import MatrixDialog from '../../../public/wf-matrix-form/MatrixDialog';   // L21
  <MatrixDialog isCreate={isCreate} workflowid={workflowId} showSignOrder={true}
                doSave={v => this.doSaveMatrix(v)} />                        // L142
  ```
- 矩阵配置弹窗：`src4js\pc4backstage\workflow\public\wf-matrix-form\MatrixDialog.js`（标题 `getLabel(382790,'矩阵对应')`，900×400；暴露全局 `window.workflowMatrixSetUtil.openMatrix(params)`）
- 表单主体：`...\wf-matrix-form\index.js`
  ```js
  const getMatrixData = params => WeaTools.callApi('/api/workflow/nodeSet/getMatrixbrowser','POST',params);  // L19
  // 返回：baseItems(条件字段行) / operatorItem(操作者=结果维度) / tableItems(矩阵表选择) / conditionTypes / visibleControl
  ```
- 老版 JSP：`D:\Weaver2020\ecology\workflow\workflow\editOperatorGroupMatrix.jsp`（40KB）
  ```jsp
  String[] valueAry = matrixValue.split(",");
  matrix = valueAry[0];  String value_field = valueAry[1];        // 矩阵ID + 结果维度
  for (z=2..) { condition_field = rowValueAry[0]; workflow_field = rowValueAry[1]; }  // 条件字段:表单字段
  String sql = "select id,fieldname,displayname from MatrixFieldInfo where fieldtype='1' and matrixid="+matrix;
  ```

#### 3.3.4 条件字段 ↔ 表单字段 / 系统字段映射

`D:\Weaver2020\ecology\matrixmanage\pages\matrixbrowserOp.jsp`（`operator` 分派）：

```
operator=0 → MatrixFieldInfo where fieldtype='0'（条件维度下拉）
operator=1 → MatrixFieldInfo where fieldtype='1'（结果维度/操作者下拉）
operator=2 → MatrixInfo.issystem（1=分部矩阵, 2=部门矩阵 → 条件维度下拉置灰）
operator=3 → 表单所有 fieldhtmltype=3（浏览按钮）字段
operator=4 → 按条件字段的 browsertypeid 过滤可选表单字段（类型映射）
operator=5/6 → 取条件字段 browsertypeid（显示"等于/包含"）
```

类型映射（L91-117）+ **系统字段**（L162-178）：

```java
if ("1".equals(browsertypeid) || "17"...)  typestr = "1,17,165,166";    // 人力资源
if ("4".equals(browsertypeid) || "57"...)  typestr = "4,57,167,168";    // 部门
if ("164"...||"194"...)                    typestr = "164,194,169,170"; // 分部
// 分部：value=-13  "与创建人同分部（系统字段）"
// 部门：value=-12  "与创建人同部门（系统字段）"
```

---

## 四、模块间依赖关系

### 4.1 依赖全景

```
                        ┌─────────────────────────────┐
                        │  组织权限中心 (HRM)          │
                        │  ├ 分部字段定义 subcompanyDefineField  │
                        │  └ 部门字段定义 departmentDefineField  │
                        └──────────┬──────────────────┘
                                   │ ① 生成结果维度（isopen='1' 的每个人力资源字段 → 一列）
                                   │ ④ 双向同步（改矩阵 ↔ 改分部/部门）
                                   ▼
┌──────────────────────────────────────────────────────────────┐
│                     矩阵模块 (Matrix)                          │
│  ┌────────────┐  ┌──────────────┐  ┌────────────────────┐   │
│  │ matrixinfo │  │matrixfieldinfo│  │ Matrixtable_<id>   │   │
│  │ (矩阵主表)  │  │(条件/结果维度) │  │  (动态数据表)       │   │
│  └────────────┘  └──────────────┘  └────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ 权限：MatrixMaintInfo / matrix_maint_info             │   │
│  │       matrix_maint_condition (可维护条件)              │   │
│  │       matrix_maint_col       (可维护列)                │   │
│  └──────────────────────────────────────────────────────┘   │
│  核心 API：MatrixUtil.getMatrixUsers(matrixid, valueField,   │
│            matchs, workflowBhxj)                             │
└───────┬──────────────────────────────────┬───────────────────┘
        │ ② 配置引用（type=99）              │ ⑤ 同样被引用
        ▼                                   ▼
┌────────────────────────────┐   ┌──────────────────────────┐
│  流程引擎 (Workflow)        │   │  工作关系 (WorkRelate)    │
│  workflow_groupdetail.type=99│  │  GoalUtil / RiskUtil      │
│  workflow_groupdetail_matrix│   │  用 matrixtable_ 取目标   │
│  workflow_matrixdetail      │   └──────────────────────────┘
│  MartrixItem.getOperator()  │
└────────────────────────────┘
        │ ③ 运行时取人
        ▼
   节点办理人 (OperatorInfoEntity)
```

### 4.2 依赖关系说明

| 依赖方 | 被依赖方 | 关系 |
|---|---|---|
| 矩阵模块 | 组织权限中心（分部/部门字段定义） | ① 默认矩阵（分部/部门）的**结果维度**由 `subcompanyDefineField`/`departmentDefineField` 中 `isopen='1'` 的自定义字段生成；④ 双向同步 |
| 矩阵模块 | HR 组织架构 | 分部/部门变更后回写系统矩阵（`sysSubcompayData()` / `sysDepartmentData()`） |
| 流程引擎 | 矩阵模块 | ② 节点操作者 type=99 引用矩阵；③ 运行时调 `MatrixUtil.getMatrixUsers` 取人 |
| 工作关系 | 矩阵模块 | ⑤ `GoalUtil.getMatrixSql` / `RiskUtil` 直接用 `matrixtable_` 前缀取矩阵数据 |
| 外部接口 | 矩阵模块 | `HrmOutInterface.initMatrixInfo()` / `initMatrixDataOutManager()` —— 外部用户/外部经理走矩阵 |
| 建模（表单建模） | 矩阵模块 | `formmode/setup/ModeShareMatrixbrowserOp.jsp` 复用同一套矩阵浏览按钮 |
| 浏览按钮体系 | 矩阵模块 | `MatrixBrowserService` / `MatrixColBrowserService` / `MatrixFieldInfoBrowserService` |

### 4.3 权限依赖

- `Matrix:Maint` —— 矩阵维护权限（老版 `MatrixMaint.jsp:29` 校验）
- `Matrix:MassMaint` —— 批量维护权限（老版 `MassMaint.jsp:17` 校验）
- 新版走 `MatrixManager.getUserPermissionMatrixids(user)` + `matrix_maint_condition` / `matrix_maint_col` 细粒度控制

---

## 五、主要业务流程

### 5.1 矩阵建模流程

```
新建矩阵（填名称）
      │  MatrixAdd.jsp → MatrixOperation.jsp?method=add
      ▼
字段维护（条件维度黄色 + 结果维度蓝色）
      │  选浏览按钮 → 填显示名 + 数据库名称 → 可加多个
      │  MatrixDesignList.jsp → saveitems.jsp → AddMatrixDesignCmd
      │  ⚠ 初次创建必须"保存并进入详细设置页面"才会出现数据维护页
      ▼
动态建表 Matrixtable_<matrixid>
      │  MatrixUtil.addSql / AddMatrixDesignCmd L188
      ▼
数据维护（添加行，每行条件维度至少填一个）
      │  matrixtable.jsp → savetablerecords.jsp → AddMatrixTableCmd
      ▼
完成（可被流程节点引用）
```

关键约束：
- 条件维度之间**是 AND 关系**，必须都满足才能找到结果
- 结果维度默认 3 个多人力资源，**至少需要设置 3 个**；岗位不够可设为相同名称
- 每行的条件维度**至少有一个有值**，否则无法根据维度找到对应人
- 一行条件维度若设置了多个选项，必须**全部满足**才选中

### 5.2 流程节点引用矩阵（配置流程）

```
节点操作者设置 → 操作者类型选「矩阵」(type=99)
      │
      ├─ 引用矩阵表字段 → 选择矩阵（如"集团总部二维矩阵"）
      │     → workflow_groupdetail_matrix.matrix
      │
      ├─ 操作者（结果维度）→ 选矩阵的一个取值字段
      │     → workflow_groupdetail_matrix.value_field
      │
      └─ 条件字段（可加多条，AND）
            ├─ 矩阵条件（condition_field）
            └─ 对应流程字段（workflow_field）
                  ├─ 表单浏览按钮字段（类型须匹配：人力/部门/分部）
                  ├─ -12  与创建人同部门（系统字段）
                  └─ -13  与创建人同分部（系统字段）
            → workflow_matrixdetail（一行一个条件）
```

保存：`DoSaveOperatorGroupInfoCmd` L361-365（`dbtype == 99` 分支）。

### 5.3 运行时解析操作者（核心流程）

**E9 新引擎** —— `com/engine/workflow/biz/nodeOperator/item/MartrixItem.java`：

```java
public OperatorEntity getOperator(OperatorEntity operatorInfo, Map<String,Object> otherInfo) {
    if(isNodeOperator){
        this.matrixTableName      = "workflow_groupdetail_matrix";   // L95
        this.matrixDetailTableName= "workflow_matrixdetail";         // L96
    } else {
        this.matrixTableName      = "workflow_urger_matrix";         // L98 督办
        this.matrixDetailTableName= "workflow_urger_matrix_detail";
    }
    Map<String,String> ugerMatrix = this.getUrgerMatrix(groupdetailid);           // matrix + value_field
    List<Map<String,String>> list = this.getUrgerMatrixDetailInfo(groupdetailid); // 条件明细

    Map<String, String> matchs = new HashMap<String, String>();
    for (Map<String,String> matrixDetail : list) {
        String conditionField = matrixDetail.get("condition_field");
        String fieldId        = matrixDetail.get("workflow_field");
        workflowBhxj = Util.getIntValue(matrixDetail.get("workflow_bhxj"));
        switch (fieldId){
            case "-1":  ... 指定分部  getAllSubcompanyids(fieldValue, workflowBhxj, false); break;  // L197
            case "-13": ... 创建人分部（支持虚拟组织 virtualid < -1）;                              // L201
            case "-2":  ... 指定部门  getAllDepartmentids(...);                                    // L211
            case "-12": ... 创建人部门;                                                            // L215
            default:    // 表单字段
                List<String> fieldValue_list = FieldInfoBiz.conversionDetailData(requestInfo, fieldId+""); // L234
                if(",4,57,167,168,".indexOf(","+type+",")>=0)   // 部门字段
                    fieldValue = getAllDepartmentids(fieldValue, workflowBhxj, false);                    // L245
                if(",164,194,169,170,".indexOf(","+type+",")>=0) // 分部字段
                    fieldValue = getAllSubcompanyids(fieldValue, workflowBhxj, false);                    // L248
        }
        matchs.put(conditionField, fieldValue);                     // L259 key=矩阵条件字段id, value=表单实际值
    }
    MatrixUtil matrixUtil = new MatrixUtil();
    String useridsAll = matrixUtil.getMatrixUsers(matrix, valueField, matchs, workflowBhxj);  // L268
    for(String userId : useridsAll.split(",")){
        operatorInfo.getOperatorInfos().add(new OperatorInfoEntity(userId, 0));   // L276 最终办理人
    }
```

**取值流程总结**：

1. 读 `workflow_groupdetail_matrix` → 得 `matrix`（矩阵 id）+ `value_field`（结果维度列）
2. 读 `workflow_matrixdetail` → 得 N 条条件（**AND**）
3. 每条条件按 `workflow_field` 取值：`-1` 指定分部 / `-2` 指定部门 / `-12` 创建人部门 / `-13` 创建人分部 / 其它 = 表单字段 id（走 `FieldInfoBiz.conversionDetailData`），并按 `workflow_bhxj` 做「本级/上级/下级/逐级」展开
4. 组成 `matchs{矩阵条件字段id : 实际值}`
5. 调 `MatrixUtil.getMatrixUsers(matrixid, valueField, matchs, workflowBhxj)`
   → 拼 `select <结果维度列> from Matrixtable_<matrixid> where 1=1 and <条件列1> in (...) and <条件列2> like '%,v,%'`
   → 返回人员 id 逗号串
6. 逐个封装为 `OperatorInfoEntity(userId, 0)` 加入节点操作者

**E8 老引擎兼容路径** —— `weaver/workflow/request/RequestNodeFlow.java`：

```java
case 99:                                                          // L5173 矩阵
    String matrixUserids = getUseridsByMatrix(rs2, groupdetailid); // L5175
    if ("".equals(matrixUserids)) matrixUserids = "-1";
    String matrixUseridsin = Util.getSubINClause(matrixUserids, "id", "in");
    sql = "SELECT 0 as customerid,HrmResource.* FROM HrmResource WHERE ( "+matrixUseridsin+") and status in(0,1,2,3,5)";
    break;
```

另有 `checkUseridsByMatrix(groupdetailid)`（L5336）用于「矩阵无符合条件的操作者」报错提示。

### 5.4 批量维护流程

```
批量维护入口（权限 Matrix:MassMaint）
      │
      ├─ 选择矩阵
      │     ├─ 系统分部矩阵 → 条件字段可选：全部条件字段 / 分部
      │     ├─ 系统部门矩阵 → 条件字段可选：全部条件字段 / 部门
      │     └─ 自定义矩阵   → 条件字段可选：该矩阵所有条件字段 / 全部条件字段
      │
      ├─ 选择条件字段 + 取值字段（结果维度）
      │
      ├─ 选择被调整对象 → 显示"可调整对象数量"和"可调整矩阵信息"
      │     MassMaintIframe.jsp?method=queryrecord
      │
      └─ 操作方式：替换为（选新用户）/ 删除
            → MassMaintOperation.jsp?method=replacerecord
            → 遍历 Matrixtable_<id> 全部结果维度列做替换/删除
            → issystem=1/2 时再回写分部/部门（sysMatrixDataToSubOrDep）
```

后端核心 `addMatrixListMassMaintFormCmd.java`：
- L157 `select * from Matrixtable_<matrixid>`
- L180-204 逗号包裹的精确替换（`olduserid` → `newuserid`）
- L236 `update Matrixtable_<matrixid> set ... where uuid=...`
- L246 `MatrixUtil.sysMatrixDataToSubOrDep(matrixid, issystem)`

### 5.5 导入 / 导出流程

| 操作 | 说明 |
|---|---|
| 导出 | 将矩阵内容导出到 Excel，主要供查看。老版 `MatrixExportToFile.jsp?method=exportDataNoSQ`；新版 `/api/hrm/matrix/pages/matrixList/matrixExport` |
| 导入-新增 | 模板只有条件，没有内容。老版 `MatrixImport.jsp` 的 `importType=add` |
| 导入-更新 | 模板会连同现有内容一并导出，便于调整后再导入更新。老版 `importType=update` |
| **默认矩阵导入限制** | **只能更新，不能新增**——条件维度跟随组织自动维护。必须先下载更新模板，模板中带「条件维度代码」且**不可修改**，确保导入时能正确关联组织 |
| 清除无效数据 | 清除离职人员、封存的分部/部门。新版按钮 `clearDimission` → `/api/hrm/matrix/pages/matrixList/clearDimission` |

### 5.6 组织结构联动同步（默认矩阵）

```
【组织权限中心】-【自定义设置】-【分部字段定义 / 部门字段定义】
      │  添加"自定义人力资源"或"多人力资源"字段（isopen='1'）
      ▼
默认矩阵自动生成对应的结果维度列（多人力资源 type=17）
      │  MatrixUtil.sysSubcompanyToMatrix()  L1077（分部，issystem=1）
      │  MatrixUtil.sysDepartToMatrix()      L1121（部门，issystem=2）
      ▼
管理员调整组织结构 → 矩阵中分部/部门自动跟随变更
      │  MatrixUtil.updateSubcompayData(id) / updateDepartmentData(id)  L2213/2220
      ▼
（反向）矩阵中修改结果维度人员 → 回写 hrmsubcompanydefined / hrmdepartmentdefined
      MatrixUtil.getUpdateSql(..., isSystem, sys_sqls)  L273-330
```

E8 版走 `hrm_formfield` + `hrm_fieldgroup`（`grouptype=4` 分部 / `5` 部门）+ `hrmsubcompanydefined` / `hrmdepartmentdefined`。

---

## 六、接口清单

### 6.1 REST API（`/api/...`，除标注外均 POST）

**矩阵设置（matrixList）**

```
/api/hrm/matrix/pages/matrixList/getMatrixListSearchList      // 矩阵列表
/api/hrm/matrix/pages/matrixList/getMatrixListSetForm         // 新建/编辑表单
/api/hrm/matrix/pages/matrixList/addMatrixList                // 保存矩阵
/api/hrm/matrix/pages/matrixList/delMatrixList                // 删除矩阵
/api/hrm/matrix/pages/matrixList/getMatrixDesignSetForm       // 字段维护表单
/api/hrm/matrix/pages/matrixList/addMatrixDesign              // 保存字段（条件/结果维度）
/api/hrm/matrix/pages/matrixList/getMatrixTableSetForm        // 数据维护表格数据
/api/hrm/matrix/pages/matrixList/addMatrixTable               // 保存矩阵数据行
/api/hrm/matrix/pages/matrixList/delMatrixTable               // 删除矩阵数据行
/api/hrm/matrix/pages/matrixList/getMatrixShowNameSetForm     // 分部/部门显示名（全路径）设置
/api/hrm/matrix/pages/matrixList/saveMatrixShowNameSetForm
/api/hrm/matrix/pages/matrixList/matrixExport                 // 导出 Excel
/api/hrm/matrix/pages/matrixList/getMatrixFile                // 取导入模板 (type=add/update)
/api/hrm/matrix/pages/matrixList/clearDimission               // 清除无效/离职数据
```

**矩阵管理员**

```
/api/hrm/matrix/pages/matrixList/getMatrixMaintSetForm
/api/hrm/matrix/pages/matrixList/getMatrixMaintSearchList
/api/hrm/matrix/pages/matrixList/addMatrixMaint
/api/hrm/matrix/pages/matrixList/delMatrixMaint
/api/hrm/matrix/pages/matrixList/getMatrixMaintInfoList
/api/hrm/matrix/pages/matrixList/getMatrixMaintTab
/api/hrm/matrix/pages/matrixList/getMatrixMaintConditionForm  // 可维护条件
/api/hrm/matrix/pages/matrixList/saveMatrixMaintCondition
/api/hrm/matrix/pages/matrixList/getMatrixMaintColList        // 可维护列（取值字段）
/api/hrm/matrix/pages/matrixList/saveMatrixMaintCol
/api/hrm/matrix/pages/matrixList/delMatrixMaintCondition
/api/hrm/matrix/pages/matrixList/delMatrixMaintCol
/api/hrm/matrix/pages/matrixList/delMatrixMaintInfo
/api/hrm/matrix/pages/matrixList/getMatrixMaintRightLevelForm
/api/hrm/matrix/pages/matrixList/saveMatrixMaintRightLevel
```

**矩阵批量维护**

```
GET  /api/hrm/matrix/pages/matrixMassMaint/getMatrixListMassMaintForm
POST /api/hrm/matrix/pages/matrixMassMaint/addMatrixListMassMaintForm
```

**流程节点操作者引用矩阵**

```
POST /api/workflow/nodeSet/getMatrixbrowser
```

> 以上 URL 在 `D:\Weaver2020\ecology\WEB-INF\securityRule\Rule\weaver_security_rules_for_E9.xml` 中逐条登记（如 L119913 `clearDimission`、L150934 `getMatrixListMassMaintForm`、L342013 `addMatrixListMassMaintForm`）。

### 6.2 Service 层对外方法

`com/engine/hrm/service/MatrixListService.java`（实现 `MatrixListImpl.java`，全部 `Map<String,Object> xxx(Map<String,Object> params, User user)`）：

```
getRightMenu / gettMatrixTabs / getMatrixListSearchCondition / getMatrixListSearchList
getMatrixInfoList / getMatrixListConditionList / getMatrixListSetForm
addMatrixList / delMatrixList
getMatrixMaintSearchList / getMatrixMaintSetForm / addMatrixMaint / delMatrixMaint
getMatrixDesignSetForm / addMatrixDesign
getMatrixTableSetForm / addMatrixTable / delMatrixTable
getMatrixExport / getHasRight / getMatrixFile
getMatrixShowNameSetForm / saveMatrixShowNameSetForm / getMatrixTableStringForm
clearDimission
getMatrixMaintTab / getMatrixMaintInfoList / getMatrixMaintConditionForm
getMatrixMaintConditionList / saveMatrixMaintCondition
getMatrixMaintColList / saveMatrixMaintCol
getMatrixMaintSearchCondition / getMatrixMaintInfoListCondition
delMatrixMaintCondition / delMatrixMaintCol / delMatrixMaintInfo
getMatrixMaintRightLevelForm / saveMatrixMaintRightLevel
```

**最底层、供各模块直接调用的 API**（最重要）：

```java
weaver.matrix.MatrixUtil.getMatrixUsers(matrixid, selectFieldId, matchs [, workflowBhxj] [, rs])
weaver.matrix.MatrixManager.getUserPermissionMatrixids(User)
weaver.matrix.MatrixManager.isUserPermissionMatrixids(User)
weaver.matrix.MatrixManager.getMatrixConditionByMaintUser(resourceid, matrixid)
```

调用方举例：
- `com/engine/workrelate/util/GoalUtil.java` L2423-2470（`getMatrixSql` / `getMatrixTableData`）
- `com/engine/workrelate/util/RiskUtil.java` L477
- `com/engine/workflow/biz/nodeOperator/item/MartrixItem.java`

### 6.3 配置项

`D:\Weaver2020\ecology\WEB-INF\prop\Matrix.properties`：

```
browserlist=179,124,141,136,101,6086,21002,21003     # 条件维度可选择的浏览按钮类型
```

即字段维护里「选浏览按钮」下拉的数据来源。

---

## 七、SpringBlade 侧现状与落地建议

> ⚠️ 更新（2026-10-15）：本章 7.1/7.2/8 中「opType=99 未实现」的描述**已过时**——矩阵操作者已实现并落地。最新进度、后端缺口核查与开发任务见 **第十一章**；本章保留作历史与方案评估参考。

### 7.1 现状：已预留、未实现

`blade-service-api/blade-workflow-api/.../entity/WfNodeOperator.java` 的类注释**已把 opType=99 纳入对齐契约**：

```java
/**
 * op_type 对齐 OperatorDBType：3人员 1部门 30分部 2角色 58岗位 4所有人 /
 * 17创建人本人 18创建人上级 19本部门 / 40本人 41上级 /
 * 5字段-人员 6字段-人员上级 42字段-部门 43字段-角色 / 99矩阵 97接口 98SQL。
 */
```

但 `blade-service/blade-workflow/.../resolver/WfOperatorResolver.java` 的 `switch` **没有 opType=99 的分支**，仅有：

```java
case OP_USER(3) / OP_CREATOR(17) / OP_SELF(40) / OP_FIELD_USER(5)
case OP_DEPT(1) / OP_FIELD_DEPT(42)
case OP_ROLE(2) / OP_FIELD_ROLE(43)
case OP_POST(58) / OP_ALL(4)
case OP_CREATOR_LEADER / OP_LEADER(41) / OP_FIELD_USER_LEADER(6)
case OP_DEPT_SELF(19)
default:
    log.warn("[blade-workflow] 操作者类型暂不支持解析，已跳过 ... opType={}, objId={}");
    return List.of();
```

即：矩阵操作者配置即使写入，运行时也会落 default → 返回空集合 → 触发本项目已有的「MI 空集合守卫」→ 节点被拦截/跳过。

### 7.2 落地建议

**① 数据建模**（`blade-workflow`）

| 建议表 | 对齐 E9 |
|---|---|
| `wf_matrix` | `matrixinfo`（`id, name, descr, issystem(0自定义/1分部/2部门), ...`） |
| `wf_matrix_field` | `matrixfieldinfo`（`matrixid, browsertypeid, displayname, fieldname, fieldtype(0条件/1结果), priority, colwidth`） |
| `wf_matrix_data` | `Matrixtable_<id>` —— **建议改为统一表** `wf_matrix_data(matrix_id, dataorder, uuid, data_json)`，用 JSON 存各维度值，避免 E9 的动态建表（多租户/迁移成本高） |
| `wf_node_operator_matrix` | `workflow_groupdetail_matrix`（`def_id, node_key, matrix_id, value_field_id`） |
| `wf_node_operator_matrix_detail` | `workflow_matrixdetail`（`node_operator_id, condition_field_id, workflow_field, workflow_bhxj, objid`） |
| `wf_matrix_maint*` | 维护者/可维护条件/可维护列（可延后） |

**② 解析器扩展**（`WfOperatorResolver`）

新增常量与分支：

```java
/** 矩阵 */
public static final int OP_MATRIX = 99;

case OP_MATRIX:
    base = byMatrix(inst, node, op);   // 按 matrixId + valueFieldId + 条件映射解析
    break;
```

解析逻辑复用 SpringBlade 已有能力：
- 条件值取数复用 `OP_FIELD_USER/DEPT` 的表单取数路径（`formData`），`-12/-13` 对应 `OP_DEPT_SELF` / 创建人分部
- `bhxj` 语义已存在（0本部 1含下级 2含上级 3逐级），可复用 `escalate()`

**③ 与现有机制的衔接**

- 本项目已有「MI 空集合守卫」：矩阵解析为空时会被拦截并给出明确报错，与 E9 的 `checkUseridsByMatrix` 行为一致，**无需额外处理**，但建议把报错文案带上矩阵名与条件值，方便排查
- 字段权限 / 渲染：`/form/render` 已返回字段权限矩阵，矩阵取数只需表单值，不额外依赖布局

**④ 落地优先级建议**

| 优先级 | 内容 |
|---|---|
| P0 | `opType=99` 解析 + 矩阵基础表 + 节点配置（运行时取人跑通） |
| P1 | 矩阵维护页面（列表/字段维护/数据维护）+ 导入导出 |
| P2 | 矩阵管理员（编辑/完全控制 + 可维护条件/列） |
| P3 | 批量维护、清除无效数据、默认矩阵与分部/部门字段定义联动 |

---

## 八、关键结论速查

| 项 | 结论 |
|---|---|
| 操作者类型常量 | `OperatorDBType.MARTRIX = 99`（`dbType=99, itemType=301, bigType=3`）；落库 `workflow_groupdetail.type=99` |
| 条件/结果区分 | `matrixfieldinfo.fieldtype`：`0=条件字段（黄）`，`1=取值/结果字段（蓝，多为浏览按钮 17 多人力资源）` |
| 条件关系 | 多个条件维度之间 **AND**；一行条件维度多值需**全部满足** |
| 矩阵数据表 | `Matrixtable_<matrixid>`，固定 `uuid/dataorder` + 各 `matrixfieldinfo.fieldname` 列（动态建表） |
| 结果维度来源 | `subcompanyDefineField` / `departmentDefineField`（`isopen='1'`）或 E8 的 `hrm_formfield` + `hrm_fieldgroup.grouptype=4/5` |
| 系统字段编号 | `-1` 指定分部 / `-2` 指定部门 / `-12` 创建人部门 / `-13` 创建人分部 |
| 级别 `bhxj` | `0` 本级 / `1` 上级 / `2` 下级 / `3` 逐级向上 |
| 取人入口（E9） | `MartrixItem.getOperator()` → `MatrixUtil.getMatrixUsers(matrix, valueField, matchs, workflowBhxj)` |
| 取人入口（E8） | `RequestNodeFlow` `case 99:` → `getUseridsByMatrix()` |
| 前端（新） | `/spa/hrm/engine.html#/hrmengine/matrix`、`/matrix/{id}`、`/matrixAdjust` |
| 前端（老） | `/matrixmanage/pages/*.jsp` |
| SpringBlade 现状 | `opType=99` 已在 `WfNodeOperator` 注释契约中预留，`WfOperatorResolver` **未实现**（落 default 告警 → 空集合 → 被 MI 守卫拦截） |

---

## 九、矩阵数据存储方案评估：统一表 + JSON vs 动态建表

### 9.1 结论先行

**在 SpringBlade 场景下，统一表（`wf_matrix_data` + `data_json`）是更优选择，长期维护负担明显低于 E9 的「每矩阵动态建表 `Matrixtable_<id>`」。** 但它把「DB 层约束」换成了「应用层约束」，因此**必须配套 6 项缓解措施**（见 9.6），否则会引入 E9 同款的数据一致性问题。

### 9.2 两种方案对照

| 维度 | 统一表 + JSON（建议） | 动态建表（E9 现状） |
|---|---|---|
| 表数量 | 固定 3 张 | 随矩阵数**线性增长**（1 矩阵 = 1 张表） |
| 加/删维度 | 改 `wf_matrix_field` 一行，**零 DDL** | 每个矩阵 `ALTER TABLE` / `DROP COLUMN` |
| 跨矩阵统计 | 单表 `WHERE matrix_id IN (...)` | 需 UNION 所有 `Matrixtable_*` |
| 多租户 | 天然带 `tenant_id` | 表名含租户则表爆炸；不含则隔离靠应用层 |
| DB 层约束 | JSON 内部字段无约束 | 列级 `NOT NULL`/长度/类型可控 |
| 备份迁移 | 3 张表，成本恒定 | 成本随矩阵数增长 |

### 9.3 逐维度评估

**① 查询性能** —— 差异可忽略，统一表足够

- 矩阵数据量级：单矩阵通常几十~几百行，项目总量在万行以内，属**极小数据集**。
- 统一表：`WHERE matrix_id = ?` 走索引取出该矩阵全部行（几十~几百行），**Java 侧做条件 AND 过滤**。单次流转成本 = 1 次索引查询 + 内存过滤，微秒级。
- 动态表：SQL 侧 `where 1=1 and <列> in (...)` 过滤，小表下性能相当。
- **本项目已有同款实践佐证**：`wf_form_snapshot.data_json`、`wf_process_node.ext_json`、`condition_json` 全部是「整列取出 → Jackson 解析 → 内存使用」，全项目**无一处 `JSON_EXTRACT` 或函数索引**，说明该模式已被验证。
- ⚠️ 退化边界：若单矩阵行数涨到**万级**且条件维度多，全量取 + 内存过滤会退化。缓解见 9.6。

**② Schema 演进** —— 统一表**显著更优**

- 统一表：增删维度 = 改 `wf_matrix_field` 记录 + `data_json` 内容，**无 DDL、可回滚**。
- 动态表：每个维度变更都要 DDL，多矩阵时逐个执行；DDL 在 MySQL 8 下仍可能锁表，且**不可回滚**。E9 已暴露该风险：`AddMatrixDesignCmd` 删字段时既不校验也不级联清理，导致 `workflow_matrixdetail.condition_field` 悬空（详见 10.1-⑥）。

**③ 索引策略**

| 方案 | 索引 |
|---|---|
| 统一表（建议） | 必建 `(matrix_id)`；**建议 `(matrix_id, data_order)` 复合索引**保证行序稳定；`tenant_id` 纳入联合索引 |
| 可选加速 | MySQL 8 生成列 `JSON_EXTRACT(data_json,'$.<fieldKey>')` + 二级索引（仅在确认需要按条件值检索时启用，不要预先设计） |
| 动态表 | 每条件列可单独建索引，理论上限高；但 N 表 × M 索引 = 索引数量爆炸，维护成本高 |

**④ 数据一致性** —— 这是统一表**唯一明显劣势**，必须靠应用层补

- 动态表可用 DB 列约束（`NOT NULL`、`varchar(100)` 长度、类型），一致性更强；
- 统一表的 `data_json` 内部字段**无任何 DB 约束**（无 NOT NULL、无外键、无唯一、无类型）。
- **但 E9 的教训恰恰是：动态表有列约束也没用** —— 它在应用层两端（前端、后端）都不做校验，照样写入非法数据（字段名可注入、结果维度可删到 0 个、行条件可全空）。
- → 结论：**一致性靠的是校验代码，不是存储形态**。统一表 + 应用层强校验，优于动态表 + 无校验。

**⑤ 后续扩展** —— 统一表显著更优

- 支持任意维度组合与条件维度数量变化；
- 导入导出、批量替换、跨矩阵统计统一处理（E9 批量维护要逐表遍历，见 10.1-⑥）；
- 软删除、审计、多租户可用 `TenantEntity` 标准列统一承载。

### 9.4 风险与缓解措施（落地必做）

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| JSON 内字段无 DB 约束 | 脏数据 | **应用层强校验**：字段定义校验（首字符字母 + `^[a-zA-Z][a-zA-Z0-9_]*$` + 保留字黑名单 + 长度 ≤ 25 + 同矩阵内重名拒绝）；保存时校验「至少一个条件维度有值」 |
| 全量取 + 内存过滤在万级行退化 | 性能 | `(matrix_id, data_order)` 索引；热矩阵本地缓存（矩阵变更时失效）；必要时再上生成列索引 |
| `data_json` 膨胀 | 存储/传输 | 维度值只存 id 或短字符串；单矩阵行数上限保护（建议 ≤ 5000 行并提示） |
| **条件匹配语义歧义** | **正确性** | **显式定义并写单测**：空条件集合 = 不匹配（不要像 E9 退化为全表匹配）；多值 = 包含任一/全部 需明确；空值不参与匹配而非命中空行 |
| 行序不稳定 | 结果不确定 | `data_order` 非空 + `(matrix_id, data_order)` 唯一约束 + 查询 `ORDER BY data_order` |
| 重复人员 | 待办重复 | 结果用 `LinkedHashSet` 去重并保持顺序（E9 未做，会重复派单） |

### 9.5 需要修正的一处计划假设

计划中写的 DDL 路径 `doc/sql/migration/V2026.10.14_001__wf_matrix.sql`（Flyway 风格）**与实际约定不符**：实际 `D:\workproject\springbladeandreact\springBlade\doc\sql\migration\` 下 94 个文件均为**描述性命名**（如 `act_add_columns.sql`、`act_backfill_task_biz_id.sql`、`_verify_tenant.sql`），无 `V{版本号}__` 前缀。

→ 建议改为 `doc/sql/migration/wf_matrix.sql`（或 `add_wf_matrix.sql`），保持与既有 94 个文件一致。

---

## 十、E9 矩阵功能遗漏点清单（后端 + 前端）

> 排查范围：`D:\workproject\ecology`（Java 源码）与 `D:\Weaver2020\ecology`（部署态 JSP/JS/SPA）。每条均带文件路径与行号证据。
> 严重度：**严重**（数据损坏/越权/流程中断）、**高**（结果错误/性能/注入）、**中**（体验/边界）、**低**（可读性/小概率）。

### 10.1 后端遗漏（`D:\workproject\ecology`）

#### A. 边界情况未覆盖

| # | 严重度 | 问题 | 证据 |
|---|---|---|---|
| ① | **严重** | **条件维度全空 → 退化为全表匹配**。空集合判断写错类型：`"".equals(matchs)` 中 `matchs` 是 `Map`，恒为 false → 条件为空 → SQL 变成 `where 1=1` → 返回矩阵**全部行**人员 | `weaver/matrix/MatrixUtil.java:544`（`:680` 同样错误） |
| ② | **严重** | **多条件 AND 中某条件无值 → 变成"该列为空"的等值匹配**，不报错不提示。`value=""` → `values=[""]` → `valueStr="''"` → 走 `else` 分支拼 `and (col is null or col in (''))` | `MatrixUtil.java:590-616` |
| ③ | **严重** | **矩阵被删除但流程仍引用 → 异常被吞，静默无操作者**。`getFieldNameNew` 返回空串 → 拼出 `select  from Matrixtable_x` 语法错 → `catch` 仅 `e.printStackTrace()`，无日志无告警 | `MartrixItem.java:262-280` |
| ④ | **严重** | **E8 路径 NPE**：`GroupDetailMatrix.read` 无记录时返回 null，调用方直接 `matrix.getMatrix()`，且无 try/catch → 流程中断 | `RequestNodeFlow.java:5268,5317`；`GroupDetailMatrix.java:58` |
| ⑤ | **高** | **多行同时命中：无 `ORDER BY dataorder`、无去重** → 行序由 DB 决定（不稳定）+ 加入**重复操作者** | `MatrixUtil.java:622,631-650` |
| ⑥ | **中** | **矩阵字段被删除但流程条件仍引用** → 悬空 id → 条件拼出 `and  in (...)` 语法错；`AddMatrixDesignCmd` 删字段不校验、不级联清理 | `AddMatrixDesignCmd.java:203-222` |
| ⑦ | **高** | **162 多选 `like '%,v,%'` 三重误判**：空值命中空行；多值拼成 `"1,2"` 要求**连续匹配**（存 `"2,1"` 漏匹配）；`varchar(100)` 静默截断 | `MatrixUtil.java:577-589`（`:708-720` 复制一份） |
| ⑧ | **中** | **结果维度人员离职/封存运行时不过滤**：E8 有 `status in(0,1,2,3,5)` 过滤，E9 路径完全无；且 `ClearDimissionCmd` 只保留 `{0,1,2,3}`（把返聘 `5` 清掉），与运行时口径矛盾 | `MartrixItem.java:273-277` vs `RequestNodeFlow.java:5184`；`ClearDimissionCmd.java:140-144` |
| ⑨ | **中** | **`bhxj=3` 逐级向上不可靠**：`HashMap` 无序取值；上游 `AbstractItem` 把 3 降级为 2 且未做「由近及远」排序；全程找不到时静默返回空、无日志 | `MatrixUtil.java:489-521`；`AbstractItem.java:470-478` |
| ⑩ | **中** | **`sysSubcompanyToMatrix`/`sysDepartToMatrix` 在 `rs.next()` 之前取值** → 默认列的 `priority` 丢失 | `MatrixUtil.java:1082-1096`、`:1137` |
| ⑪ | **低** | `addSql` 在字段为空时建表 SQL **末尾残留逗号** → 语法错 | `MatrixUtil.java:973-978` |

#### B. 数据校验缺失

| # | 严重度 | 问题 | 证据 |
|---|---|---|---|
| ⑫ | **严重** | **字段保存：显示名/数据库名完全无校验**（非空、合法性、长度、保留字、重名全无），且直接拼进 `create table`/`alter table` → **SQL 注入 + 保留字冲突 + 同名列覆盖** | `AddMatrixDesignCmd.java:124-148,188-194,236-265` |
| ⑬ | **严重** | **矩阵新增：名称无校验 + 字符串拼接**（无空值/重名/长度/引号转义） | `AddMatrixListCmd.java:80-116` |
| ⑭ | **中** | **「结果维度至少 3 个」「每行至少一个条件维度有值」后端均未校验**（前端校验也缺失，见 10.2） | `AddMatrixDesignCmd` 全文；`AddMatrixTableCmd.java:145-176` |
| ⑮ | **高** | **导入校验基本被注释掉**：不校验未知列、列顺序、重复行、类型；固定取第 2 个 sheet（单 sheet 模板直接越界）；控制流写反（无错误时反而删文件返回）；`flag` 失败标记不复位（一行失败→后续全记失败）；更新模式 UUID 缺失 NPE | `MatrixImportProcessE9.java:315-372, 298, 191-194, 1520/1629/1637, 1560` |

#### C. 权限校验缺失（越权）

| # | 严重度 | 问题 | 证据 |
|---|---|---|---|
| ⑯ | **严重** | **四个入口完全无鉴权**：矩阵列表 `GetMatrixInfoListCmd`、导出 `GetMatrixExportCmd`、下载模板 `GetMatrixFileCmd`、导入写库 `SaveImportMatrixCmd` → 任意登录用户可导出全部矩阵数据/写入任意 `Matrixtable_x` | 见 10.1 表（各 Cmd 入口行） |
| ⑰ | **严重** | **矩阵管理员的「可维护条件/可维护列」只在界面生效，保存接口不校验** → 前端过滤即安全，拿到 uuid 即可改删任意行任意列 | `AddMatrixTableCmd.java:91-94,145-210`；`delMatrixTableCmd.java:78-95`（对照 `ClearDimissionCmd.java:120-127` 反而正确应用了） |
| ⑱ | **高** | **批量维护权限过宽**：只要维护了"任意一个"矩阵（`isUserPermissionMatrixids` 仅判列表非空），即可批量替换**其他矩阵**的人员（`matrixid` 由前端任意指定） | `addMatrixListMassMaintFormCmd.java:83-89`；`MatrixManager.java:44-53` |
| ⑲ | **中** | **维护者判定丢失角色级别/安全级别校验**（原 SQL 被注释简化）→ 权限放大；且方法形参 `matrixid` 完全未使用 | `MatrixUtilToolCmd.java:729-751` |
| ⑳ | **低** | 未开启分权时矩阵列表 `where 1=1` 裸奔 | `GetMatrixInfoListCmd.java:50-85` |

#### D. 事务与并发

| # | 严重度 | 问题 | 证据 |
|---|---|---|---|
| ㉑ | **高** | **数据行保存无事务**：创建了 `RecordSetTrans` 但从未 `setAutoCommit(false)`，默认自动提交 | `AddMatrixTableCmd.java:99-101,205-210` |
| ㉒ | **高** | **批量替换 / 导入 / 矩阵删除均无事务**（逐条执行）；字段保存有事务但 `catch` 中**无 rollback**（异常时事务悬挂） | `addMatrixListMassMaintFormCmd.java:92,241`；`MatrixImportProcessE9.java:1626-1630`；`DelMatrixListCmd.java:74-78`；`AddMatrixDesignCmd.java:281-297` |
| ㉓ | **高** | **主键与排序并发冲突**：`MatrixFieldInfo` 主键与 `dataorder` 均用 `max()+1` 生成 → 并发新增必然冲突/重复；全模块**无行锁、无乐观锁** | `AddMatrixDesignCmd.java:256-270`；`AddMatrixTableCmd.java:128-134` |

#### E. 其它缺陷

| # | 严重度 | 问题 | 证据 |
|---|---|---|---|
| ㉔ | **严重** | **`ClearDimissionCmd` SQL 的 OR 缺括号** → `AND ... AND fieldtype=0 OR id in(...)`，前面条件全失效 → 非管理员可能清到范围外字段；且 `matrixCondition` 可能 null → NPE | `ClearDimissionCmd.java:98-102` |
| ㉕ | **严重** | **删除矩阵不彻底、不校验引用**：不 drop `Matrixtable_<id>`（孤儿表）、不清理 `workflow_groupdetail_matrix`/`workflow_matrixdetail`（流程悬空）、**不调用 `matrixDelMark`**（前端置灰、后端不校验） | `DelMatrixListCmd.java:67-86` |
| ㉖ | **中** | **批量替换子串误替换**：`!s.contains(olduserid)` 未用逗号包裹 → `olduserid="1"` 会误删 `"11"/"21"/"100"`（同方法 L183 的 `","+olduserid+","` 才是正确写法，两者口径不一致） | `addMatrixListMassMaintFormCmd.java:187-190` vs `:183` |
| ㉗ | **中** | 批量替换字段选择逻辑反直觉（uuid 不在 map 时该行静默跳过）；生产代码用 `System.out.println` 打 SQL | `addMatrixListMassMaintFormCmd.java:167-174, 240` |
| ㉘ | **中** | 导入目录不存在 → `files.length` **NPE** → 每次导入返回 `status=-1` | `SaveImportMatrixCmd.java:154-164` |
| ㉙ | **中** | **`XSSFWorkbook`/`InputStream` 未关闭**（资源泄漏） | `MatrixImportProcessE9.java:293-306` |
| ㉚ | **中** | `MatrixBrowserService` 执行**空串 SQL** | `MatrixBrowserService.java:140-143` |
| ㉛ | **中** | **N+1 查询**：字段列表每列查一次"是否有数据"；运行时解析 `getFieldName/getFieldType` 每条件 2 次 SQL（缓存默认 null）→ 每次流转 2×N 次往返 | `MatrixUtil.java:108-119, 832-892, 576-601` |
| ㉜ | **低** | 用 `Thread.sleep(2000)` 掩盖缓存刷新竞态；`InterruptedException` 未恢复中断标志；`initCache` 角色成员 N+1 | `MatrixMaintComInfo.java:143-152, 172-190` |
| ㉝ | **低** | `MatrixConvert` 未校验入参 `size`；`Float.parseFloat` 未防异常；导入异常详情直接回显前端 | `MatrixConvert.java:19-27`；`AddMatrixTableCmd.java:149-153`；`SaveImportMatrixCmd.java:142-146` |

### 10.2 前端遗漏（`D:\Weaver2020\ecology`）

#### A. 源码树级别硬伤

| # | 严重度 | 问题 | 证据 |
|---|---|---|---|
| ㉞ | **严重** | **SPA 大量调用 `apis/matrix.js` 中根本不存在的导出**（`getMatrixListConditionList`、`getMatrixMaintInfoList`、`initMaintenanceSettingData`、`saveMatrixMaintCondition`、`delMatrixMaintCol` 等）→ 位于 `initData()` 且在 `getMatrixList()` 之前调用，一旦抛 `TypeError` **矩阵列表根本不加载**，矩阵管理员三个 Tab 全废 | `apis/matrix.js` 全文仅 34 行；`stores/matirx.js:238`；`stores/matrixMaintenance.js:265,287,381,495,521,558,576,598` |
| ㉟ | **严重** | **SPA 字段维护 store 源文件缺失**：`stores/index.js:30-31` import `matirxFieldSetting`，但该目录下**不存在**此文件 → `matirx.js:432 saveField` 是空壳 → "结果维度至少 3 个""重名校验"在 SPA 侧无法核实 | `stores/index.js:30-31`；`matirx.js:432` |

#### B. 校验缺失

| # | 严重度 | 问题 | 证据 |
|---|---|---|---|
| ㊱ | **严重** | **「结果维度至少 3 个」约束形同虚设且可删到 0 个**：删除保护 `if(selectitems.length===1) return` 永不成立——因为删除时遍历的是 `matchitems`（复制粘贴 bug，应为 `selectitems`），`splice(i,1)` 中 `i` 恒等于 length → 实际不删；长度不减 → 保护条件永不触发 → **3 个结果维度可全部删光并保存成功** | `matrixdesign_wev8.js:240-253`（对比条件维度 `:171-178` 用的是正确的 `matchitems[i]`） |
| ㊲ | **严重** | **数据维护新增行无任何校验**（老版 + SPA 均无）：没有"条件维度至少一个有值"的提示或阻断，新增全空行可直接提交 | `matrixtable_wev8.js:118-149,475-516`；`matirxDataMaintenance.js:597-613,660-695` |
| ㊳ | **中** | 字段维护：**显示名重名未校验**（只对数据库名去重）、**输入框无长度限制** | `matrixdesign_wev8.js:382-385`（仅 fieldname 去重）；`MatrixDesignList.jsp:157,160` 无 maxLength |
| ㊴ | **中** | SPA 字段维护：命中数据库保留字时**静默清空输入**而非阻塞提交，用户可能误以为已填 | `hrmengine/util/index.js:122-134`（`validDBKeys` 返回 `value:''`） |
| ㊵ | **中** | **导入无文件类型 `accept`、无大小限制、导入前无合法性校验** | `MatrixImport.jsp:105,212-220`；`stores/importresource.js:151-159` |

#### C. 前后端字段不一致

| # | 严重度 | 问题 | 证据 |
|---|---|---|---|
| ㊶ | **严重** | **`savetablerecords.jsp` 对参数 `toLowerCase()`，前端原样发送** → 字段名含大写字母（创建时正则允许 `[a-zA-Z]`）时，前端发 `DeptID_0`、后端读 `deptid_0` → 取到 null → **该列被静默清空** | `savetablerecords.jsp:71-73` vs `matrixtable_wev8.js:505-512`；允许大写见 `matrixdesign_wev8.js:44-48` |
| ㊷ | **中** | `MatrixAdd.jsp` 把 `method` 传成**矩阵名称**而非 `"add"`；后端只判 `"edit".equals(method)`，若矩阵名恰好叫 `edit` → 走进 `id != matrixid` 分支导致**重名校验失效** | `MatrixAdd.jsp:117`；`checkdatas.jsp:10-12` |
| ㊸ | **中** | 参数名语义反直觉：名为 `fieldlable` 的参数装的其实是**数据库字段名**（且 `fieldlable` 为拼写错误），`namelabel` 才是显示名 → 后续维护极易改错 | `saveitems.jsp:66-74,81,171` |
| ㊹ | **中** | 可维护列名不一致：落库 `colid`/`colname`，入参却是 `id`/`displayname` | `SaveMatrixMaintColCmd.java:38-43` vs `GetMatrixMaintColListCmd.java:63-64` |
| — | ✅ | `matrixid`/`value_field`/`condition_field`/`workflow_field`、`issystem`（空/1/2）、`fieldtype`（0条件/1结果）前后端**一致，未发现写反** | 多处核对 |

#### D. 边界与流程侧

| # | 严重度 | 问题 | 证据 |
|---|---|---|---|
| ㊺ | **严重** | **系统矩阵（issystem=1/2）前端只是"不渲染入口"，后端写接口全部不拦** → `saveitems.jsp`/`savetablerecords.jsp`/`deletetabledatas.jsp`/`MatrixOperation.jsp(edit)` 均无 `issystem` 与权限校验 → 可直接改删系统矩阵 | `MatrixList.jsp:251-259`（前端隐藏）；`saveitems.jsp:16`、`savetablerecords.jsp:17`、`deletetabledatas.jsp:14`、`MatrixOperation.jsp:49-53`（后端无校验） |
| ㊻ | **严重** | **删除矩阵后端不校验"被流程引用"**：前端 `matrixDelMark` 只决定图标是否渲染，后端 `delete` 直接执行 → 绕过 UI 即可删除被引用矩阵 | `MatrixList.jsp:257` vs `MatrixOperation.jsp:56-70` |
| ㊼ | **严重** | **批量维护选"删除"时后端仍执行替换** → `newuserid=""` → `replaceAll(","+old+",", ","+new+",")` 把人员引用**替换成空串**，数据损坏；且 `MassMaintOperation.jsp` 无权限校验 | `MassMaint.jsp:150`（恒传 `replacerecord`）；`MassMaintOperation.jsp:38-39,73,14-18` |
| ㊽ | **严重** | **流程侧：条件字段与表单字段类型匹配完全无校验** —— `conditionTypes` 被接收存入 state 但**从未使用**，无"矩阵条件是部门却选了人力资源字段"的比较或提示 | `wf-matrix-form/index.js:34,86,147,357,376,504`（仅赋值） |
| ㊾ | **严重** | **删除条件按钮索引错位**：用 `lastIndexNum`（上一条的 index）而非 `indexNum` → 点第 N 条的删除会**删掉上一条** | `wf-matrix-form/index.js:634-658` |
| ㊿ | **中** | 流程侧保存校验不完整（矩阵本身未单独校验，靠 `num==-1` 巧合兜底）；错误文案**固定为"请先选择操作对象的值"**，缺矩阵/缺条件/缺结果维度都弹同一句 → 指引错误 | `wf-matrix-form/index.js:226-281`；`MatrixDialog.js:110` |
| ㊱ | **中** | **多条条件 AND 在配置弹窗内无表达**（只纵向堆叠），"AND" 仅出现在列表页汇总串 | `wf-matrix-form/index.js:646-658, 253` |
| ㊲ | **中** | 两套"是否系统矩阵"判据可能不一致（`getDatas()` 用后端 `systems` ids，`getItems()` 用 `slaveLabel`）→ 界面允许加多条条件但保存只取 `num=1` → **用户配 3 条只存 1 条** | `wf-matrix-form/index.js:172,192,551,637,652` |
| ㊳ | **中** | 清除无效数据：**无二次确认、无"将清除 N 条"提示**，且先 `saveMatrixData` 会把未保存编辑一起写库 | `matirxDataMaintenance.js:743-752` |
| ㊴ | **中** | SPA 删除数据行：**先执行 `removeItem()` 再弹确认**，用户点"取消"行也已经没了 | `matirxDataMaintenance.js:638-641` |
| ㊵ | **中** | **SPA 批量调整全量拉取、无分页**（数据维护页反而有分页）→ 大矩阵卡顿 | `matrixAdjust.js:414-421,181-203` |
| ㊶ | **低** | 右键菜单字符串多余单引号 `exportData()'`；循环变量用错（`i<len`）；`MatrixMaintOperation.jsp` 多值时把整串 id 塞进单个 `roleid` | `matrixtable.jsp:80`；`matrixtable_wev8.js:556`；`MatrixMaintOperation.jsp:47` |

#### E. 权限绕过（前端）

| # | 严重度 | 问题 | 证据 |
|---|---|---|---|
| ㊷ | **严重** | **老版所有写接口均无鉴权**（仅 `getUser`）：`saveitems.jsp`、`savetablerecords.jsp`、`deletetabledatas.jsp`、`getcolumndata.jsp`、`checkdatas.jsp`（且 `name` 可 SQL 注入）、`MatrixImport.jsp`、`MatrixImportProcess.jsp`（**权限校验被注释掉**）、`MassMaintOperation.jsp` → 绕过 UI 直接 POST 即可越权改删任意矩阵（含系统矩阵）。相反 `MatrixOperation/MatrixMaintOperation/MassMaint/MatrixAdd` 有校验 ✅ | 各文件入口行，见上表 |
| ㊸ | **中** | SPA 权限位由服务端返回 ✅，但 `matirxDataMaintenance.js:401-415` 存在**纯前端 `hasRight` 判断**（`findIndex(...) > -1`）→ 可绕过 | `matirxDataMaintenance.js:401-415`；`MatrixDataMaintenanceView.js:116-122` |

### 10.3 SpringBlade 落地「应避免」清单（把 E9 教训转成设计要求）

| E9 教训 | SpringBlade 设计要求 |
|---|---|
| 空条件集合退化为全表匹配（①） | **显式定义：条件为空 = 不匹配（返回空，交 MI 守卫拦截并报明确错误）**，写单测锁定 |
| 空值条件命中空行（②） | 条件值为空 → 该条件不参与匹配并**记录 warn 日志**，禁止拼 `is null` |
| 多行命中无排序无去重（⑤） | `ORDER BY data_order` + `LinkedHashSet` 去重 |
| 离职人员运行时不过滤（⑧） | 取到人后统一按「在职」状态过滤，口径与「清除无效数据」保持一致 |
| 字段/矩阵删除不校验引用（⑥㉕） | 删除前**强校验引用**（被流程引用则拒绝）；删除矩阵级联清理节点配置 |
| 字段名可注入（⑫） | 字段 key 走**正则白名单 + 保留字黑名单 + 长度限制 + 同矩阵重名拒绝**，绝不拼 DDL |
| 可维护范围只在界面生效（⑰） | **保存接口必须校验可维护条件/列**，不能只靠前端过滤 |
| 写接口无鉴权（㊷⑯） | 所有 `/matrix/**` 写接口统一鉴权入口，禁止"前端隐藏即安全" |
| 无事务（㉑㉒） | 数据行保存、批量替换、导入、删除全部 `@Transactional` |
| 异常被吞（③） | 禁止 `printStackTrace` 了事，必须 `log.error` 带矩阵名 + 条件值 |
| 前端拼错索引/遍历错数组（㊱㊾） | 矩阵条件增删用**稳定 id** 而非数组下标 |
| 条件 AND 无界面表达（㊱） | 配置界面显式展示 "AND" 连接关系 |

---

## 十一、SpringBlade 落地进度与「矩阵对应条件」后端缺口核查（2026-10-15）

> 本章为最新核查结论，**取代 7.1 / 7.2 / 8 中「未实现」的旧描述**。依据：E9 运行时源码 `D:\workproject\ecology\src`（`MartrixItem` / `AbstractItem` / `weaver.matrix.MatrixUtil` / `GetMatrixbrowserCmd` / `DoSaveOperatorGroupInfoCmd`）逐行比对 SpringBlade 当前代码。

### 11.0 存储路线说明

未采用 7.2① 建议的独立 `wf_node_operator_matrix*` 表，而是走**路线 B 的 BPMN `wf:operator` 扩展**：`opType=99` + `objId=矩阵ID` + `matrixJson`（与 `WfOperatorResolver.byMatrix` 读取一致）。`matrixJson` 已同步进：前端 `wfModdle.json`（Operator 属性）、`bpmnExtension.ts`（读写）、`services/workflow` 的 `WfNodeOperator`、`nodeOperatorIO.toLocalOp`。引擎侧 `blade.workflow.operator-from-bpmn.enabled=true`（`application.yml`），运行时从 BPMN 读操作者，`matrixJson` 只在 BPMN 承载（表结构放不下），链路自洽。

### 11.1 已落地能力（截至 2026-10-15）

| E9 能力（依据） | SpringBlade 现状（代码位置） | 状态 |
|---|---|---|
| opType=99 分支 | `WfOperatorResolver.byMatrix`（L569-616） | ✅ |
| 来源：表单字段 | `resolveConditionValue` else（L636-638，`formData.get(wfField)`） | ✅ |
| 来源：`-12` 创建人部门 / `-13` 创建人分部 | L625-631（取 **starter**，与 E9 一致） | ✅ |
| 来源：`-1`/`-2` 指定分部/部门（固定 objId） | L632-635 | ✅（编码见 11.2-5） |
| 多条件 AND | `WfMatrixServiceImpl.rowMatches` 全满足（L449） | ✅ |
| 单条件「输入集合 ∈ 行值集合」多值 | L448 双方 `split` 后任一命中 | ✅ |
| 结果维度须为 `fieldtype=1` | `matchUsers` L370-377 校验 | ✅ |
| 结果值=逗号用户ID、拆分 | L409-416 | ✅ |
| 用户去重 | `LinkedHashSet`（L402/421） | ✅（优于 E9，E9 不去重） |
| 离职/无效过滤 | `resolveWithForm.filterActive`（L183-185） | ✅ |
| 矩阵维护页 / 数据行 / 匹配测试 / 操作者「矩阵对应」页签 | `pages/System/Matrix/*` + `NodeOperatorModal` 矩阵对应 | ✅ |
| 唯一键软删除陷阱（字段/数据行覆盖保存撞 `uk_*`） | `physicalDeleteByMatrixId` / `physicalDeleteByOrder`+`physicalDeleteById` | ✅ 已修 |

### 11.2 后端缺口清单（按严重度）

#### A. 必缺（导致「范围」类矩阵条件不生效）

| # | 缺口 | 与矩阵对应条件的关联 | 位置 |
|---|---|---|---|
| 1 | **`bhxj` 范围（0本部/1含下级/2含上级/3逐级向上）整链缺失**：前端 `condRowToCond` 不写 `bhxj`；后端 `byMatrix` L597 只传 `wfField/objId`、**不读 `c.bhxj`**、无展开逻辑 | E9 `workflow_matrixdetail.WORKFLOW_BHXJ` 是矩阵条件的核心列；缺它 → 部门/分部条件退化为「本部精确匹配」，与 E9「含下级/含上级」不符 | 前端 `NodeOperatorModal`；后端 `WfOperatorResolver` |
| 2 | **部门/分部集合展开无数据来源**：`IDeptClient` 目前仅 `divisionIdByDept`，无「取子孙部门/分部ID集合」「取上级链」接口 | `bhxj=1` 需子孙集合、`bhxj=2/3` 需祖先链；无接口则范围无法落地 | `blade-system` `IDeptService/IDeptClient`（`Dept.ancestors` 列可支撑上级链；含下级需新增遍历） |
| 3 | **逐级向上 `bhxj=3` 独立算法**：E9 逐级替换输入值、由近及远取**最近一级命中**（`MatrixUtil:489-521`），非一次性展开 | 决定「逐级向上」语义 | 后端 `byMatrix`/`matchUsers` |

#### B. 建议对齐（语义差异）

| # | 缺口 | 说明 | 位置 |
|---|---|---|---|
| 4 | **空条件值语义相反**：E9 空值**不跳过**、去匹配该列为空/null 的行（`MatrixUtil:611-615`）；我们空值**跳过该条件**（`byMatrix` L598、`matchUsers` L383）→ 创建人无部门等场景可能**忽略条件、多匹配错派** | 建议改 E9 语义：空值→该行此维度需为空才满足（通常无命中→节点被拦截，更安全） | `WfMatrixServiceImpl.matchUsers` |
| 5 | **`-1/-2` 编码与 E9 相反**：E9 `-1`=指定**分部**、`-2`=指定**部门**（`MartrixItem:197,211`）；我们前端 `fixedDept→-1`、`fixedBranch→-2`。后端对二者一视同仁返回 `objId`，**当前无功能影响**，但文档/后续按类型分叉会踩坑 | 校正前端映射与 E9 一致 | 前端 `NodeOperatorModal.condRowToCond` |

#### C. 低优先 / 可选

| # | 缺口 | 说明 |
|---|---|---|
| 6 | 指定部门/分部支持**多选**（`objId` 逗号多值）：E9 `WORKFLOW_OBJID` 可多；我们固定源单选 | 前端 `PersonOrgField` 开 `multiple` |
| 7 | 条件维度浏览类型覆盖不全：编辑器仅 1/17/4/164；E9 支持角色/岗位/日期/自定义(162) 等。匹配按字符串、不依赖类型，但「是否按 org 展开」需读条件维度 `browserType` 判定 | 前端编辑器 + 后端按 `browserType` 分叉 |
| 8 | 自定义多选(162) `,值,` LIKE：E9 特殊处理；我们未暴露 162 | 低 |
| 9 | 系统矩阵(issystem=1/2) + 父部门向上兜底 `getMatrixUsers_4_system`：E9 默认矩阵自动跟随组织并向上兜底一级；我们无默认矩阵 | 属矩阵维护模块，非操作者条件核心，P3 |

#### D. 明确不做（记录）

- **虚拟维度 `virtualid`**：SpringBlade 无 `HrmResourceVirtualView` 虚拟组织模型，不实现。
- **当前办理人部门来源**：E9 `-12/-13` 仅取**创建人**，无「当前办理人」选项；截图中「本部门」实为 `bhxj=0` 的标签、非独立来源 → **不需要**新增当前办理人分支。

### 11.3 深度分析补充遗漏点（本轮新增）

1. **`bhxj` 需按条件维度 `browserType` 决定是否展开**：只有部门(4/57)/分部(164/194) 型条件才做组织展开；人力(1/17)/其它类型不展开。实现时从 `wf_matrix_field.browserType` 取类型，不要假设全是部门。
2. **表单字段来源也要按类型展开**：E9 `default` 分支对「部门型/分部型表单字段」同样按 `bhxj` 展开（`MartrixItem:245,248`）。计划须覆盖表单字段来源，而非只 `-1/-2/-12/-13`。
3. **`removeMatrix`「被流程引用时拒绝删除」需核实**：引用记录在 BPMN `matrixJson`（`objId=matrixId`）。删除校验是否真正扫描了 BPMN/定义需确认，否则会出现「删了矩阵但流程仍引用 → 运行时静默无操作者」（E9 教训③）。
4. **`matchUsers` 全量取行 + 内存过滤**：小矩阵可接受，已建 `(matrix_id, data_order)` 唯一索引；万级行需评估（缓解见 9.6）。
5. **覆盖保存（字段/数据行）为「整组物理删+插」且 `@Transactional`**：前端全量回填，并发/传不全才会丢数据，风险低，记录即可。

### 11.4 开发任务分解（关闭必缺项，端到端）

**后端 `blade-system`**
- **T1** `IDeptService/IDeptClient` 增 `descendantIds(deptId)`、`ancestorIds(deptId)`（含分部 subcompany 对应；上级链直接用 `Dept.ancestors`，含下级用 children 遍历或 `ancestors LIKE`）

**后端 `blade-workflow`**
- **T2** `MatrixCfg.Cond` 确认含 `bhxj`；`byMatrix` 读取 `bhxj`，对 `resolveConditionValue` 取到的组织值按 `bhxj` 展开（0 原值 / 1 含下级 / 2 含上级 / 3 逐级向上），展开集作为条件多值传入 `matchUsers`；**按条件维度 `browserType` 判定是否展开**（部门/分部才展开）
- **T3** 空值语义对齐（并入 T2）：空值→该行此维度为空才满足，消除「跳过导致多匹配」
- **T4**（可选）逐级向上独立算法

**前端**
- **T5** `NodeOperatorModal` 条件行加「范围」下拉（本部/含下级/含上级/逐级向上）写入 `matrixJson.conditions[].bhxj`；结果维度标签改「操作者」、条件行加「属于」；`-1/-2` 编码对回 E9
- **T6** 若采 E9 空值语义，`matchUsers`/`rowMatches` 配合支持「行内该维度为空」匹配

**验证**
- **T7** 单测：`bhxj` 0/1/2/3、空值、AND、多值、去重；E2E：节点配矩阵(含范围)→提交流转→命中预期办理人

**依赖顺序**：T1 → T2 → T3/T4 → T5 → T6 → T7

### 11.5 开发进度（2026-10-15）

| 任务 | 状态 | 说明 |
|---|---|---|
| T1 blade-system `child-ids`/`ancestor-ids` | ✅ 已部署并实测 | `IDeptClient`+`IDeptService`+`DeptServiceImpl`+`DeptController`；复用已有 `getDeptChildIds`，新增 `getDeptAncestorIds`（`ancestors` 列优先，沿父兜底）。实测：`ancestor-ids(行政部)=[1000000005,2000000004]`、`child-ids(1000000005)=自身+全部子孙` |
| T2 blade-workflow `byMatrix` 按 `bhxj` 展开 | ✅ 已编译并重启 | 读 `bhxj`；按条件维度 `browserType` 判定是否组织展开；`0` 原值 / `1` 含下级(`childIds`) / `2` 含上级(`ancestorIds`)；`3` 逐级向上走 `matchEscalate`（祖先链由近及远取最近命中） |
| T3 空值语义对齐 | ✅ 已部署并实测 | `matchUsers` 不再丢弃空值条件；`rowMatches` 空条件值→要求行内该维度也为空；`byMatrix`/`matchEscalate` 空值按空串传入。实测：空值匹配空行命中 `[3]`、非空不匹配空行 `[]`、单条件回归 `[3]` |
| T5 前端范围选择器 | ✅ 已写（lint 通过） | 条件行加「属于」+ 范围下拉（本部/含下级/含上级/逐级向上，仅组织型维度显示）；`-1/-2` 编码对回 E9；结果维度改「操作者」；`matrixJson` 落 `bhxj` |
| T6 配合空值语义 | ✅ 随 T3 完成 | `rowMatches` 已支持空值匹配空行 |
| T7 E2E（节点配矩阵+范围→提交流转→命中办理人） | ✅ 已通过 | 脚本 `doc/bpmn/test_matrix_e2e.cjs`：导入含两矩阵节点（bhxj0 精确 / bhxj2 含上级）的 BPMN → deploy → `/test/run`；实测 bhxj0→`[3]`、bhxj2→`[3,4]`（展开到上级行），真实实例走到归档，`testStatus=1` |

> 部署：本次改动了 `blade-system-api`、`blade-system`、`blade-workflow` 三个模块，已 `clean package` 并重启 blade-system、blade-workflow（均 `Started`）。前端为 umi dev，硬刷新即可。

---

## 十二、E9 矩阵数据迁移（ecology → SpringBlade）

### 12.1 背景与工具

E9 矩阵存于 SQL Server（`ecology2020_demo`）：`matrixinfo`（矩阵主表）、`matrixfieldinfo`（字段/维度定义）、`Matrixtable_<id>`（数据行，列名即 `fieldname`）。需同步到 SpringBlade 的 `wf_matrix` / `wf_matrix_field` / `wf_matrix_data`。

**工具**：`doc/tools/EcologyMatrixMigration.java`（单文件 JDBC，零框架依赖，复用 `EcologyOrgMigration.java` 的写法与 `EcologyDataSourceConfig.java` 的 JDBC 常量：`jdbc:sqlserver://192.168.1.5:1433;databaseName=ecology2020_demo`，账号 `sa`/`1`）。直连 E9 读、直连 blade 写，无需打包进应用。

### 12.2 映射规则（已实测）

| 维度 | E9 来源 | SpringBlade 目标 | 处理 |
|---|---|---|---|
| 矩阵定义/字段 | `matrixinfo`/`matrixfieldinfo` | `wf_matrix`/`wf_matrix_field` | 直接搬；系统矩阵 2 额外补 `departmentDefineField`（billid=0）的 6 个结果维度（空值，待 UI 填） |
| 结果维度（人力 1/17） | `HrmResource.id` | `blade_user.id` | **E9 用户 id == blade 用户 id（账号一致）**，原值直搬；若某 id 在 blade 不存在则保留原值并计入报告 |
| 条件维度（部门 4/57） | `HrmDepartment.id` | `blade_dept.id` | 正值 `+ 2_000_000_000`；经 `blade_dept` 存在性校验，通过才改写 |
| 条件维度（分部 164/194） | `HrmSubCompany.id` | `blade_dept.id` | 正值 `+ 1_000_000_000`；同上校验 |
| 负值 / 非正整 / 非部门分部类型（如项目 8） | — | 保留原值 | 保留原值并计入「待人工核对清单」 |

> 组织 id 偏移量与 `EcologyOrgMigration` 一致（分部 +1e9、部门 +2e9，维森→鼎泰仅公司前缀不同，名称一致）。

### 12.3 运行

```bash
java -cp "mssql-jdbc-12.8.1.jre11.jar;mysql-connector-java-8.0.29.jar" EcologyMatrixMigration.java [--dry-run] \
     [--eco-url ... --eco-user ... --eco-pass ...] [--blade-url ... --blade-user ... --blade-pass ...]
```

- `--dry-run`：只计算并输出预估 + 待核清单，不落库。
- **幂等**：每次对目标 `matrix_id` 先物理 `DELETE` `wf_matrix_field` / `wf_matrix_data` / `wf_matrix` 再 `INSERT`（绕过 `@TableLogic`），可重复运行覆盖。
- 字段/数据 id 用 `9e18` 命名空间，与 blade 既有雪花 id 不冲突。
- 回滚：执行前请先备份 `wf_matrix` / `wf_matrix_field` / `wf_matrix_data`。

### 12.4 实测结果（2026-10-15）

| 项 | 数量 |
|---|---|
| 矩阵 `wf_matrix` | 12（含系统矩阵 1 分部矩阵 / 2 部门矩阵） |
| 字段 `wf_matrix_field` | 37 |
| 数据 `wf_matrix_data` | 95 |
| 待人工核对 | 35 条 |

**待核清单**（保留原值，需人工在 UI 改）：
- 负值组织条件（E9 内部编码，无法反推）：矩阵 7 `bgs/dzb`(-13~-21)、矩阵 8 `gongh/bm`(-13~-30)、矩阵 9 `dq`(-27~-32)、矩阵 11 `yfzx`(-33~-35)、矩阵 3 `subcompany`(-5)。
- 项目类型条件（`browserType=8`，SpringBlade 无项目模块）：矩阵 10 `xm`(76/75/73…) 共 7 条。

**系统矩阵 1/2**：E9 源 `matrix_code` 结果列全空，如实同步；矩阵 2 已补 6 个结果维度字段（空值）。

> 注：早期验证用的 `doc/bpmn/e9export/migrate_matrix.cjs`（node 版）与本 Java 工具映射逻辑一致，结果相同；后续以 `EcologyMatrixMigration.java` 为准（可重复运行）。


