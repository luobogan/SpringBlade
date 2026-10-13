# 收发文落地设计（三）：版式文件 OFD / PDF「真·电子公文」方案

> 上游文档：
> - 《泛微E9-收发文与正文套红功能分析及SpringBlade落地方案.md》（业务与套红机制）
> - 《收发文落地-编辑器选型与套红合成设计.md》（P1 选型 / P3 套红合成 / DDL / 状态机）
>
> 本文定义**套红技术的权威标准方案（以此为准）**：最新的套红技术不再是单纯的「Word 宏」或「图片叠加」，而是**以 GB/T 9704 标准为底座、以 OFD/PDF 版式文件为载体，结合 AI 智能排版与 CA 电子签章的一体化解决方案**——政府 / 国企主流、信创环境标配、最终归档与签发形态。「Word 宏 / VBA」与「图片叠加红头」两条旧路线明确淘汰（见 §1.3）。
>
> 核心证据来源：`D:\workproject\ecology\src\weaver\odoc/ofd/**`（E9 独立 OFD 子系统）、`weaver/workflow/qiyuesuo/**`（契约锁电子签章）、`weaver/wps/officed/**`（带修订转 PDF / 水印）。

---

## 一、为什么需要第三条路线

### 1.1 两条路线的本质差异

| 维度 | 路线一：docx 编辑态套红 | 路线二：**版式文件（OFD/PDF）** |
|---|---|---|
| 载体 | `.docx`（可编辑、流式） | `.ofd` / `.pdf`（**固定版式**） |
| 版面保真 | 依赖字体/版本，跨端可能偏移 | **一次定稿、处处一致**（版式锁定） |
| 红头 | 套红时把红头模板套进 docx | 红头是**版式的组成部分**，定稿即固化 |
| 法律效力 | 弱（可被篡改） | **强**（版式 + 电子签章 + 时间戳 + CA） |
| 归档标准 | 无国标要求 | **OFD-A（GB/T 33190-2016）**，档案级 |
| 信创适配 | Office 依赖 | **国产 OFD 厂商全栈** |
| 适用 | 企业内部流程、企业公文 | **政府 / 国企 / 事业单位** |

### 1.2 结论

> **版式文件不是"更好的套红"，而是电子公文的"定稿与签发形态"。**
> 正确架构是：**docx（编辑态）→ 定稿 → 版式文件（OFD/PDF）→ 电子签章 → 归档**。
> 套红在这个链路里的位置从"在 Word 里换书签"前移/转变为"**套用红头版式模板生成版式文件**"。

### 1.3 套红技术路线演进（以此为准）

> **权威定义：最新的套红技术不再是单纯的「Word 宏」或「图片叠加」，而是以 GB/T 9704 标准为底座、以 OFD/PDF 版式文件为载体，结合 AI 智能排版与 CA 电子签章的一体化解决方案。**

| 代际 | 技术路线 | 实现方式 | 判定 |
|---|---|---|---|
| 一代 | **Word 宏 / VBA 套红** | 在 Word 里用宏打开红头模板、替换书签、插入正文（E9 的 `WebOffice_odoc.js:589 WebUseTemplate`、iWebOffice 控件消息包属此类） | **淘汰**：强依赖 Windows + Office/插件，无法服务化，无法进入版式文件体系 |
| 二代 | **图片叠加红头** | 把红头渲染成图片贴到正文上方 | **淘汰**：红头不可检索、不可复用、放大失真，不符合版式文件结构化要求 |
| 三代 | **版式文件一体化套红** | **GB/T 9704 标准底座 + OFD/PDF 版式载体 + AI 智能排版 + CA 电子签章**，全链路服务端完成 | **标准方案（本文档即按此设计）** |

**因此本项目套红能力的设计基准是三代方案**：
1. **GB/T 9704 是底座**——不是"画个红头"，而是把国标的版头/主体/版记三要素、字体字号、页边距、行数字数、红色分隔线全部规则化（详见 §2.5）；
2. **OFD/PDF 是载体**——套红的产出物必须是版式文件，而非可再编辑的 docx（详见 §2）；
3. **AI 智能排版**承接"正文 → 规范版式"的自动化（详见 §2.6），E9 的 `SmartOfficial` 只是其雏形；
4. **CA 电子签章**是套红后的必然动作，二者共同构成"可信电子公文"（详见 §2.4）。

> 注：docx + 书签替换（路线一）在本项目中**降级为"企业内部轻量公文 / 移动端兜底"的过渡形态**，不再是政务公文的目标形态。设计上保留它，是为了兼容企业场景与存量数据，而非作为标准。

---

## 二、E9 的版式文件子系统解剖

### 2.1 策略接口 + 四家信创厂商实现

```java
// weaver/odoc/ofd/interf/OFDConvertor.java:18
public interface OFDConvertor{
    /** 将文档转换为 OFD，返回 oldImagefileId -> newImagefileId 映射 */
    Map<Integer, Integer> convert(Set<Integer> imagefileIdSet) throws Exception;
    Map<Integer, Integer> convert(Set<Integer> imagefileIdSet, int requestid, User user) throws Exception;
}
```

实现类（同一接口、按配置选用）：

| 实现 | 厂商 | 文件 |
|---|---|---|
| `ShukeOFDConvertor` | **数科 OFD** | `weaver/odoc/ofd/impl/ShukeOFDConvertor.java` |
| `YozoOFDConvertor` | **永中 Office** | `weaver/odoc/ofd/impl/YozoOFDConvertor.java` |
| `FuxinOFDConvertor` | **福昕 OFD** | `weaver/odoc/ofd/impl/FuxinOFDConvertor.java` |
| `FuxinGSDKOFDConvertor` | **福昕 GSDK** | `weaver/odoc/ofd/impl/FuxinGSDKOFDConvertor.java` |

**这是最值得复刻的设计**：用策略接口屏蔽厂商差异，项目按客户信创环境（已采购哪家 OFD）切换实现，主流程零改动。

### 2.2 公文元数据注入 OFD（关键，易遗漏）

```java
// weaver/odoc/ofd/impl/ShukeOFDConvertor.java:52
public Map<Integer, Integer> convert(Set<Integer> imagefileIdSet, int requestid, User user){
    Map<String, String> meta = new HashMap<>();
    if(requestid > 0){
        meta = getMetaData(requestid, user);          // ★ 从流程实例取公文元数据
    }
    ...
    if(meta != null && meta.size() > 0){
        result = ConvertTest.officeToOFD(filePath, ofdFilePath, meta);   // ★ 元数据写入 OFD
    } else {
        result = ConvertTest.officeToOFD(filePath, ofdFilePath);
    }
```

**意义**：OFD 支持**结构化文档元数据**（对应 GB/T 33190 的元数据要求：文号、标题、密级、紧急程度、成文日期、发文单位…）。E9 在转换时把流程实例里的公文信息写进 OFD 元数据区，使版式文件**既可读又可检索**（档案系统可按元数据检索）。这是"纸面红头"变"结构化电子公文"的关键一步。

### 2.3 支撑能力矩阵

| 能力 | E9 实现 | 位置 |
|---|---|---|
| docx → OFD | `ConvertTest.officeToOFD(filePath, ofdFilePath, meta)` | `weaver/odoc/ofd/util/` |
| WPS 侧 docx → OFD | `ConvertDocToOfd` / `ConvertFileToOfd` | `weaver/wps/doccenter/impl/`、`weaver/wps/view/linux/convert/` |
| 附件也转 OFD | `ConvertAttachOFDService` | `weaver/odoc/ofd/service/` |
| **带修订 + 批注转 PDF** | `ConvertToPdfWithRevisionAndComent` | `weaver/wps/officed/` |
| 正文转 PDF | `TexttoPDFManager` | `weaver/workflow/request/` |
| 水印 | `OfficedWatermark` | `weaver/wps/officed/` |
| 条码 / PDF417 | `WorkflowBarCodeSetManager` + `weaver.PDF417Manager` | `weaver/workflow/workflow/` |
| OFD 上传 / 下载 Servlet | `UploadServlet` / `DownloadServlet` | `weaver/odoc/ofd/web/` |
| OFD 工具 | `OFDConvertUtil` / `OFDCommonUtil` / `ConvertToPdfForDcs` | `weaver/odoc/ofd/util/` |
| 福昕 GSDK 直连 | `com/foxit/ofd/gsdk/service/GsdkServiceApi` | `com/foxit/ofd/gsdk/` |

### 2.4 电子签章：坐标定位 + 反射解耦

```java
// weaver/workflow/qiyuesuo/external/QYSExternalAction.java:31
public String createSinglePersonalContract(int signerId, String imageFileIds,
                                           double offsetX, double offsetY, int page) {
    // 用反射调用契约锁集成 jar（不在主源码内，解耦）
    Class<?> c = Class.forName("weaver.workflow.qiyuesuo.external.QYSExternalInterface");
    Method m = c.getMethod("createSinglePersonalContract", Integer.class, String.class,
                           Double.class, Double.class, Integer.class);
    response = (String) m.invoke(c.getConstructor(User.class).newInstance(user),
                                 signerId, imageFileIds, offsetX, offsetY, page);
    ...
}
public String personalAutoSign(Long contractId) { ... }   // 个人自动签署
```

**架构要点**：
1. **签章目标就是版式文件**（`imageFileIds`），在**指定页（page）+ 指定坐标（offsetX/offsetY）**盖电子章——这正是公文"盖章位置"的业务语义（压在成文日期/单位名称上）。
2. **反射调用第三方集成 jar**：契约锁等 CA 厂商的 SDK 以独立 jar 提供，主系统只依赖接口名，**编译期不耦合、运行期可选装配**。政企项目常按客户已采购的 CA 厂商切换，这个模式非常实用。
3. 配套表 `wf_qiyuesuoSetting(workflowid, documentField)` 记录流程与签章配置。

---

## 2.5 GB/T 9704 公文格式底座（"以此为准"的标准落地）

### 2.5.1 标准定位

- **GB/T 9704-2012《党政机关公文格式》**（替代 1999 版）是党政机关公文排版的国家标准，也是本项目套红能力的**唯一格式基准**。
- 国标规定的 15 个文种：决议、决定、命令（令）、公报、公告、通告、意见、通知、通报、报告、请示、批复、议案、函、纪要。
- **佐证**：E9 的 `odoc/odoc/resource/mouldbookmark.json` 已按文种建立套红模板（通知 5042、函 5025、请示 5039、批复 5035、通报 5041、纪要 5026、令 5521、通告 5040、意见 5034、决议 5032、议案 5038、公告 5024、决定 5027）。我们的工作不是重新设计版式，而是**把每个文种模板升级为"GB/T 9704 合规版式"**。

### 2.5.2 三要素结构（版式文件的分区依据）

| 区 | 要素 | 版式要点 |
|---|---|---|
| **版头** | 份号、密级和保密期限、紧急程度、发文机关标志（红头）、发文字号、签发人 | 发文机关标志红色居中；发文字号仿宋三号居中；上行文须标注签发人 |
| **主体** | 标题、主送机关、正文、附件说明、发文机关署名、成文日期、印章、附注、附件 | 标题 2 号小标宋居中；正文 3 号仿宋；成文日期用阿拉伯数字；印章压成文日期 |
| **版记** | 抄送机关、印发机关和印发日期 | 版记首末条有分隔线 |

### 2.5.3 版面量化规范（可直接作为规则库参数）

| 项 | 规范 |
|---|---|
| 纸型 | A4（210mm × 297mm） |
| 页边距 | 上 37±1mm、下 35±1mm、左 28±1mm、右 26±1mm |
| 版心 | 156mm × 225mm |
| 每面行数 / 每行字数 | 22 行 / 28 字（三号仿宋） |
| 红色分隔线 | 与版心等宽的红色反线（版头与主体之间） |
| 页码 | 4 号半角宋体阿拉伯数字；奇数页居右、偶数页居左；数字左右各一条一字线 |

### 2.5.4 字体字号规范（版式渲染与 AI 排版的规则核心）

| 要素 | 字体 | 字号 |
|---|---|---|
| 发文机关标志（红头） | 小标宋体（红色） | 随机关名长度缩放 |
| 标题 | 小标宋体 | 2 号 |
| 正文 | 仿宋体 | 3 号 |
| 一级标题 | 黑体 | 3 号 |
| 二级标题 | 楷体 | 3 号 |
| 三级 / 四级标题 | 仿宋体加粗 | 3 号 |
| 发文字号 | 仿宋体 | 3 号 |
| 成文日期 / 附注 | 仿宋体 | 3 号 |
| 抄送机关 / 印发机关和印发日期 | 仿宋体 | 4 号 |

### 2.5.5 落地：GB/T 9704 规则库（必须自建，确定性、不依赖 AI）

```java
/** GB/T 9704 版式 profile：规则的唯一载体，可按机关微调 */
public class Gbt9704Profile {
    private PageSpec page;                        // A4、页边距、版心
    private LineSpec line;                        // 每面行数、每行字数
    private Map<ElementType, FontSpec> fonts;     // 要素 → 字体字号
    private boolean redSeparator;                 // 红色分隔线
    private PageNumberSpec pageNumber;            // 页码（奇右偶左 + 一字线）

    public enum ElementType {
        ORG_SIGN,      // 发文机关标志（红头）
        DOC_NUMBER,    // 发文字号
        SIGNER,        // 签发人
        CLASSIFICATION,// 密级和保密期限 / 紧急程度 / 份号
        TITLE,         // 标题
        RECIPIENT,     // 主送机关
        BODY,          // 正文
        H1, H2, H3, H4,
        ATTACHMENT_NOTE, ATTACHMENT,
        ORG_SIGNATURE, // 发文机关署名
        DATE,          // 成文日期
        SEAL,          // 印章
        NOTE,          // 附注
        CC, ISSUER, ISSUE_DATE  // 版记：抄送 / 印发机关 / 印发日期
    }
}
```

要点：
1. **规则库是确定性的**，AI 只负责"把正文识别成哪个要素类型"，格式本身永远由规则库决定——保证国标合规可审计。
2. 版式模板 = **GB/T 9704 规则库 + 文种模板 + 机关信息**（红头名称、机关代字）三者合成，支持按机关定制。
3. 校验能力（§2.6 L3）同样以规则库为依据，输出"国标符合性问题清单"。

---

## 2.6 AI 智能排版

### 2.6.1 E9 的雏形与其局限

```java
// weaver/wps/view/linux/convert/SmartOfficial.java:34
public int smartOfficial(int mouldImagefileId, int imageFileId, String imagefileName){
    ...
    requestParams.put("id", documentWpsId);
    requestParams.put("templateId", templateWpsId);
    requestParams.put("fieldConfig", "none");     // ★ 只套版式模板，不理解正文语义
    httpUtil.post(propUtil.getSmartOfficialApi(), requestParams, null);
```

E9 的做法是"选一个版式模板，把文档整体重排"，**不理解正文结构**——标题层级、主送机关、落款仍需用户自行保证。这正是三代方案要用 AI 补齐的部分。

### 2.6.2 三代方案的 AI 智能排版三层模型

| 层 | 能力 | 技术 | 必要性 |
|---|---|---|---|
| **L1 规则引擎** | 按 GB/T 9704 规则库应用字体字号、行距、页边距、页码、红色分隔线 | 自建规则库 + 版式渲染 | **必须（可独立交付）** |
| **L2 结构识别** | 识别标题层级、层级序号（一、/（一）/1./（1））、主送机关、落款、成文日期、附件标记，映射到 `ElementType` | 规则 + LLM | 建议（先规则后 AI） |
| **L3 智能校验** | 要素完整性（缺成文日期/主送机关/印章）、文号格式、国标符合性、敏感词 | 规则 + LLM + 复用现有敏感词模块 | 建议 |

### 2.6.3 落地接口

```java
public interface IOdocTypesettingService {

    /** L1+L2：正文结构化 + 按 GB/T 9704 渲染为规范版式 */
    TypesetResult typeset(Long docId, Long mouldId, Gbt9704Profile profile);

    /** L2：正文结构识别（可独立使用：识别出的标题可回填表单字段） */
    DocStructure analyze(String content);

    /** L3：成文要素完整性与国标符合性校验 */
    List<ComplianceIssue> checkCompliance(Long docId);
}
```

- **输入**：docx / HTML 正文（或结构化表单数据）+ 文种 + 机关信息
- **输出**：版式文件（OFD/PDF）+ 结构化工件 `DocStructure`（回填表单、写入 OFD 元数据）
- **与套红的职责边界**：AI 排版负责"**自由正文 → 规范版式**"；套红负责"**套红头 + 填公文要素（文号/主送/抄送/印发日期）**"；二者串行后统一转为版式文件。

### 2.6.4 分步落地（避免一次性 AI 化）

| 步骤 | 内容 | 交付价值 |
|---|---|---|
| 第一步（无 AI） | L1 规则引擎 + 文种版式模板 | 已满足国标合规，可上线 |
| 第二步 | L2 规则式结构识别（正则 / 段落特征 / 序号推断） | 大幅减少手工排版 |
| 第三步 | 接入 LLM 做结构识别与 L3 合规校验（私有化部署） | 智能化增强，可选 |

---

## 2.7 一体化能力拼图

```
                ┌──────────────── GB/T 9704 规则库（底座）────────────────┐
                │  纸型/页边距/行数/字数/字体字号/红头/红线/页码            │
                └───────────────────────┬────────────────────────────────┘
                                        │
   正文(docx/HTML) ──► AI 智能排版 ──► 规范版式 ──► 套红(红头+公文要素) ──► OFD/PDF 版式文件
                                        │                                      │
                                  结构化工件                             CA 电子签章 + 时间戳
                                        │                                      │
                                  表单回填 / OFD 元数据                        │
                                        └──────────────► 归档（OFD-A）◄────────┘
```

四者职责：
- **GB/T 9704**：定义"什么是合规的公文版式"——规则来源、审计依据；
- **AI 智能排版**：把自由正文变成合规版式——自动化手段；
- **OFD/PDF**：承载最终版式——保真、可签章、可归档；
- **CA 电子签章**：赋予法律效力——可信。

> **任一环节缺失，都不构成"真·电子公文"。**

---

## 三、版式文件路线的完整链路设计

### 3.1 公文生命周期（五态）

```
① 编辑态      docx（OnlyOffice/富文本）          可编辑、可留痕
      │  提交定稿
② 定稿态      docx 锁定（不可再改）              版本冻结
      │  套红/排版 + 转版式
③ 版式态      OFD / PDF（固定版式 + 元数据）      一次定稿处处一致
      │  电子签章 + 时间戳
④ 签发态      已签章版式文件                     具有法律效力
      │  归档
⑤ 归档态      OFD-A（GB/T 33190-2016）           档案级、长期可读
```

### 3.2 与套红的职责重划

| 环节 | 路线一（docx） | 路线二（版式） |
|---|---|---|
| 红头来源 | Word 红头模板 + 书签替换 | **版式模板**（红头 + 版式 + 元数据区） |
| 字段填充 | Word 书签（`fld_Mark` 等） | 版式渲染时填充（或写入 OFD 元数据） |
| 正文插入 | `Content` 书签 | 版式正文区 |
| 输出 | docx（+PDF 预览） | **OFD（+PDF 预览）** |
| 签章 | 需先转 PDF/OFD 再签 | **直接签** |
| 归档 | 不适用 | **OFD-A** |

> 实践建议：**两套可共存**。同一流程内，草稿/审核用 docx（便于批注修订），到"签发/套红"节点产出版式文件并签章；也可按 `offical_type` 分流（企业发文走 docx，政务发文走版式）。

---

## 四、SpringBlade 落地设计

### 4.1 核心抽象：版式转换策略接口

```java
public interface IOdocFormatConvertor {
    /** 厂商标识，对应配置 blade.workflow.odoc.ofd.vendor */
    String vendor();
    /** docx/imagefileId → OFD/imagefileId；meta 为公文元数据（写入 OFD 元数据区） */
    Map<Long, Long> convertToOfd(Set<Long> imagefileIds, Long instanceId, Long userId, Map<String,String> meta);
    /** docx → PDF（预览用） */
    Map<Long, Long> convertToPdf(Set<Long> imagefileIds);
    /** 是否可用（厂商 SDK 是否装配） */
    boolean available();
}

// 实现：ShukeOdocFormatConvertor / YozoOdocFormatConvertor / FuxinOdocFormatConvertor / FuxinGsdkOdocFormatConvertor
// 选用：配置切换 + @ConditionalOnProperty，或按 available() 自动降级
```

配置示例：

```yaml
blade:
  workflow:
    odoc:
      ofd:
        vendor: shuke            # shuke | yozo | fuxin | fuxin-gsdk | none
        shuke:
          api-url: http://ofd-shuke:8080/api
          app-id: xxx
          app-secret: xxx
        meta-enabled: true       # 是否注入公文元数据到 OFD
```

### 4.2 电子签章抽象（对齐契约锁的反射解耦模式）

```java
public interface IOdocSealService {
    /** 在版式文件指定页指定坐标盖章 */
    SealResult signAt(Long docId, Long versionId, Long signerId, int page, double offsetX, double offsetY);
    /** 批量/自动签署 */
    SealResult autoSign(Long docId, Long versionId, Long signerId);
    /** 验章（验证签章有效性与文档完整性） */
    SealVerifyResult verify(Long docId, Long versionId);
}
```

**装配方式**（对齐 E9 反射解耦）：CA 厂商 SDK 以独立 starter/jar 提供，接口由本项目定义，实现由厂商包注册；未装配时 `available()=false`，前端隐藏签章按钮。这样**按客户已采购的 CA 厂商切换，主流程零改动**。

### 4.3 DDL 补充（在《收发文落地-编辑器选型与套红合成设计.md》§2 基础上新增）

```sql
-- 版式文件记录（正文的定稿/版式形态，与 wf_odoc_doc_version 平行）
CREATE TABLE `wf_odoc_layout_file` (
  `id`             BIGINT       NOT NULL,
  `doc_id`         BIGINT       NOT NULL COMMENT '正文ID',
  `src_version_id` BIGINT       DEFAULT NULL COMMENT '来源docx版本ID',
  `format`         TINYINT      NOT NULL COMMENT '版式格式 12PDF 13OFD',
  `file_id`        BIGINT       NOT NULL COMMENT '版式文件ID',
  `vendor`         VARCHAR(32)  DEFAULT NULL COMMENT '转换厂商 shuke/yozo/fuxin/fuxin-gsdk',
  `layout_stage`   TINYINT      DEFAULT 3 COMMENT '3版式态 4签发态 5归档态',
  `is_final`       TINYINT      DEFAULT 0 COMMENT '是否定稿(定稿后不可再编辑docx)',
  `create_time`    DATETIME     DEFAULT NULL,
  `is_deleted`     TINYINT      DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_layout_doc` (`doc_id`, `format`, `layout_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公文版式文件';

-- 公文元数据（写入 OFD 元数据区，档案检索用；对应 E9 getMetaData）
CREATE TABLE `wf_odoc_meta` (
  `id`             BIGINT       NOT NULL,
  `doc_id`         BIGINT       NOT NULL,
  `instance_id`    BIGINT       DEFAULT NULL,
  `meta_key`       VARCHAR(64)  NOT NULL COMMENT '如 issuedNum/title/secretLevel/urgency/writtenDate/issuedUnit',
  `meta_value`     VARCHAR(500) DEFAULT NULL,
  `create_time`    DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_meta_doc` (`doc_id`, `meta_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公文元数据(OFD结构化)';

-- 电子签章记录
CREATE TABLE `wf_odoc_seal` (
  `id`             BIGINT       NOT NULL,
  `doc_id`         BIGINT       NOT NULL,
  `layout_file_id` BIGINT       NOT NULL COMMENT '被签章版式文件',
  `signer_id`      BIGINT       NOT NULL COMMENT '签章人',
  `seal_type`      TINYINT      DEFAULT 1 COMMENT '1个人签字 2单位公章 3时间戳',
  `page`           INT          DEFAULT 1,
  `offset_x`       DOUBLE       DEFAULT 0,
  `offset_y`       DOUBLE       DEFAULT 0,
  `vendor`         VARCHAR(32)  DEFAULT NULL COMMENT 'CA厂商 qiyuesuo/...',
  `vendor_seal_id` VARCHAR(128) DEFAULT NULL COMMENT '厂商侧签章/合同ID',
  `seal_file_id`   BIGINT       DEFAULT NULL COMMENT '签章后文件',
  `status`         TINYINT      DEFAULT 0 COMMENT '0待签 1已签 2已验',
  `sign_time`      DATETIME     DEFAULT NULL,
  `verify_time`    DATETIME     DEFAULT NULL,
  `verify_result`  VARCHAR(500) DEFAULT NULL,
  `create_time`    DATETIME     DEFAULT NULL,
  `is_deleted`     TINYINT      DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_seal_doc` (`doc_id`, `layout_file_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子签章记录';
```

**流程侧扩展**（`wf_odoc_process_set` 增加列）：

```sql
ALTER TABLE `wf_odoc_process_set`
  ADD `finalize_node_key`   VARCHAR(128) DEFAULT NULL COMMENT '定稿节点',
  ADD `layout_node_key`     VARCHAR(128) DEFAULT NULL COMMENT '转版式节点',
  ADD `sign_node_key`       VARCHAR(128) DEFAULT NULL COMMENT '签章节点',
  ADD `layout_format`       TINYINT      DEFAULT 13  COMMENT '版式格式 12PDF 13OFD',
  ADD `ofd_vendor`          VARCHAR(32)  DEFAULT NULL COMMENT 'OFD厂商(空=取全局配置)',
  ADD `seal_required`       TINYINT      DEFAULT 0   COMMENT '是否必须电子签章',
  ADD `seal_config`         JSON         DEFAULT NULL COMMENT '签章位置配置(页/坐标/章型)';
```

### 4.4 接口契约（新增）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/odoc/doc/{docId}/finalize` | 定稿（冻结 docx，进入版式阶段） |
| POST | `/odoc/doc/{docId}/to-layout` | 转版式（docx → OFD/PDF + 元数据注入），返回 `layoutFileId` |
| GET | `/odoc/doc/{docId}/layout?format=ofd\|pdf` | 取版式文件与预览 URL |
| POST | `/odoc/doc/{docId}/seal` | 电子签章 `{ layoutFileId, sealType, page, offsetX, offsetY }` |
| POST | `/odoc/doc/{docId}/seal/auto` | 自动/批量签署 |
| POST | `/odoc/doc/{docId}/seal/verify` | 验章 |
| POST | `/odoc/doc/{docId}/archive` | 归档（生成 OFD-A） |
| GET | `/odoc/vendors` | 当前可用 OFD / CA 厂商（前端据此显隐按钮） |
| GET | `/odoc/gbt9704/profiles` | GB/T 9704 版式 profile 列表（可按机关定制） |
| POST | `/odoc/doc/{docId}/typeset` | **AI 智能排版**（L1 规则 + L2 结构识别），body `{ mouldId, profileId? }`，返回 `{ layoutFileId, structure }` |
| GET | `/odoc/doc/{docId}/typeset/analyze` | 仅结构识别（L2），返回 `DocStructure`（可回填表单字段） |
| POST | `/odoc/doc/{docId}/compliance-check` | 国标符合性与成文要素完整性校验（L3），返回问题清单 |

### 4.5 前端：版式文件阅读器（当前完全缺失）

ant-design-pro 现有前端**没有任何 PDF/OFD 阅读器**（检索仅命中 univer 内部常量与一个地图 mock）。需新增：

| 场景 | 方案 |
|---|---|
| PDF 预览 | `pdfjs-dist`（`react-pdf`）自托管，或后端转图片流 |
| OFD 预览 | **数科 OFD Web 控件** / **福昕 OFD Web** / 永中 OFD Web（按客户采购选型，与后端 `ofd.vendor` 对齐） |
| 移动端 | 版式文件转图片分页 / 厂商 H5 SDK |
| 签章交互 | 在阅读器上点击定位盖章坐标 → 回传 `page/offsetX/offsetY` |
| 降级 | 无 OFD 控件时降级为 PDF 预览 + 「签章需 PC 端」提示 |

建议封装为统一组件 `LayoutFileViewer`（props: `fileUrl / format / onSealPointPick / readonly`），内部按 `format` 与配置分发到具体厂商控件——与后端 `IOdocFormatConvertor` 的策略模式对应。

### 4.6 元数据注入（对齐 E9 `getMetaData`）

```java
// 定稿/转版式时，从流程实例与表单快照提取公文元数据
Map<String,String> meta = OdocMetaExtractor.extract(instanceId, defId);
//   meta.put("issuedNum",     文号);        // wf_odoc_request.issued_num
//   meta.put("title",         标题);        // doc_title_field_key
//   meta.put("secretLevel",   密级);
//   meta.put("urgency",       紧急程度);
//   meta.put("writtenDate",   成文日期);
//   meta.put("issuedUnit",    发文单位);
//   meta.put("receiveUnit",   主送单位);
//   → 落 wf_odoc_meta，并作为 convertToOfd 的 meta 参数写入 OFD 元数据区
```

---

## 五、政府 / 国企 vs 一般企业：选型建议

| 场景 | 推荐路线 | 关键配置 |
|---|---|---|
| **政府 / 国企 / 事业单位** | **路线二为主**（版式文件 + 电子签章 + OFD-A 归档） | `ofd.vendor` 按客户采购；`seal_required=1`；归档开 OFD-A |
| 大型集团（有信创要求） | 混合：内部审批 docx，对外报送/归档转版式 | 定稿节点触发 `to-layout` |
| 一般企业 | 路线一（docx 套红 + OnlyOffice） | 无需 OFD 厂商；PDF 仅作预览 |
| 移动优先 / 轻量公文 | 路线一 + PDF 预览 | 无需签章 |

**混合架构推荐（最实用）**：

```
节点链：拟稿(docx) → 审核(docx,留痕) → 定稿 → [套红] → 转版式(OFD) → 电子签章 → 归档(OFD-A)
                                                              ↑ 仅政务/信创场景启用
```

`wf_odoc_process_set.layout_format = 0` 表示该流程不启用版式文件（企业场景），链路自动止于 docx + PDF 预览。

---

## 六、信创适配清单

| 层 | 需确认/适配 | 说明 |
|---|---|---|
| OS | 麒麟 / 统信 UOS / 中标麒麟 | OFD 厂商 SDK 通常提供 Linux 版（E9 的 WPS 转换即在 `wps/view/linux/`） |
| DB | 达梦 / 人大金仓 / 南大通用 | 版式文件存文件服务或 BLOB；元数据表注意字符集与 JSON 列兼容 |
| 中间件 | 东方通 / 宝兰德 | 无特殊要求 |
| OFD 厂商 | 数科 / 永中 / 福昕 / 福昕GSDK | 按客户采购，策略接口切换 |
| CA / 电子签章 | 契约锁 / 法大大 / 各地方 CA | 反射/SPI 解耦装配 |
| 阅读器 | 数科 OFD Web / 福昕 OFD Web | 前端 `LayoutFileViewer` 分发 |
| 国标 | GB/T 33190-2016（OFD）、GB/T 9704（公文格式）、OFD-A 归档 | 版式模板需按国标制作 |
| 时间戳 | RFC 3161 TSA | 签章时同步盖时间戳 |

---

## 七、分阶段建议（在原有 P0–P6 上叠加）

| 阶段 | 新增交付 |
|---|---|
| **P3 之前（前置）** | **GB/T 9704 规则库 `Gbt9704Profile`**（纸型/页边距/行数/字数/字体字号/红色分隔线/页码）+ 15 文种版式模板库 —— 一切版式能力的地基，必须先做 |
| P3（套红）之后 | `IOdocFormatConvertor` + 首家厂商实现（建议数科或福昕）+ `to-layout` 接口 + `wf_odoc_layout_file` |
| P5 | 元数据注入（`wf_odoc_meta` + `OdocMetaExtractor`）+ 定稿节点 + docx 冻结 |
| P5 | **AI 智能排版 L1**（规则引擎渲染，**无 AI 即可上线**，满足国标合规） |
| P5 | 电子签章（`IOdocSealService` + 厂商 SPI + `wf_odoc_seal` + 签章交互） |
| P5 | 前端 `LayoutFileViewer`（PDF.js + OFD 厂商控件 + 坐标选点） |
| P6 | **AI 智能排版 L2/L3**（结构识别 + 国标合规校验，可选 LLM，私有化部署） |
| P6 | 归档（OFD-A）+ 验章 + 信创全栈验证 |

---

## 八、风险与注意事项

1. **厂商 SDK 的部署与授权**：OFD 转换与电子签章均依赖厂商服务/SDK，需在项目早期确认采购与部署方式（本地 SDK / 厂商云服务 / 私有化转换服务）。
2. **版式模板制作成本**：红头版式模板需按 GB/T 9704 与国标 OFD 制作，且不同文种（通知/函/请示…）各一套，工作量集中在前置资产制作。
3. **docx 与版式的双源一致性**：定稿后 docx 冻结，任何修改必须重新走定稿→转版式→签章，需在交互上明确提示（避免"改了 docx 但版式没变"）。
4. **签章位置精度**：坐标盖章需按文种/模板预设位置，建议做成模板级配置（`seal_config` JSON）而非硬编码。
5. **验章与长期有效性**：电子签章涉及 CA 证书有效期与时间戳，归档时应确保证书链与时间戳完整，否则数年后验章失败。
6. **降级路径必须设计**：无 OFD 厂商或无 CA 的环境，系统应能降级为 PDF 预览 + 线下盖章，不能阻断流程。
7. **与 `is_test` 隔离的兼容**：测试流程若走版式文件，转换产物与签章记录也必须带 `is_test`，随 `/test/cleanup` 清理（签章若走了真实 CA，需支持"测试模式"跳过真实签章）。

---

## 九、一页总结

1. **套红技术的权威定义（以此为准）**：最新的套红技术不再是单纯的「Word 宏」或「图片叠加」，而是**以 GB/T 9704 标准为底座、以 OFD/PDF 版式文件为载体，结合 AI 智能排版与 CA 电子签章的一体化解决方案**。Word 宏/VBA 套红与图片叠加红头两条路线**明确淘汰**。
2. **版式文件（OFD/PDF）是电子公文的定稿与签发形态**，不是套红的替代品；正确链路是 `docx 编辑 → AI 智能排版 → 套红(GB/T 9704) → 版式 → CA 电子签章 → OFD-A 归档`。
3. **一体化四要素**：GB/T 9704 规则库（格式基准，必须自建且确定性）＋ AI 智能排版（L1 规则引擎 → L2 结构识别 → L3 合规校验，可分步落地）＋ OFD/PDF 版式载体 ＋ CA 电子签章（指定页 + 坐标盖章 + 时间戳）。任一缺失即不构成"真·电子公文"。
4. **E9 的最佳可复刻设计是三个"策略 + 解耦"**：
   - `OFDConvertor` 接口 + 四家厂商实现（数科/永中/福昕/福昕GSDK）→ 按客户信创环境切换；
   - 电子签章用**反射调用厂商集成 jar**（契约锁）→ 编译期不耦合、运行期可选装配；
   - `SmartOfficial` 的"模板重排"是 AI 排版雏形，需升级为"规则库 + 结构识别"两层。
5. **易遗漏的关键能力**：转换时把**公文元数据注入 OFD 元数据区**（`getMetaData`），使版式文件可检索、符合档案要求。
6. **SpringBlade 需要新增**：三个策略接口（`IOdocFormatConvertor` / `IOdocSealService` / `IOdocTypesettingService`）+ `Gbt9704Profile` 规则库、4 张表（`wf_odoc_layout_file` / `wf_odoc_meta` / `wf_odoc_seal` / 版式模板库）、前端 `LayoutFileViewer`（当前前端**无任何 PDF/OFD 阅读器**）。
7. **选型原则**：政府/国企走版式文件一体化路线（信创标配，本方案即为此设计）；一般企业可降级为 docx 套红 + PDF 预览；二者用 `layout_format=0` 在同一套代码内共存，按流程分流。
