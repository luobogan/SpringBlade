# blade-workflow-odoc-layout — 厂商 SDK 接入指南（预留窗口）

本模块是公文 **L2 版式 / 电子签章** 的厂商实现扩展点。默认不引入任何厂商 SDK，
未配置时 blade-workflow 判定「版式/签章未启用」，前端隐藏入口、停留 L1（docx/PDF）。
采购/对接厂商 SDK 后，按本指南「一并对接」即可，接口与业务层不变。

## 一、SPI 契约（在 blade-workflow-api，勿改签名）

- `org.springblade.workflow.spi.IOdocFormatConvertor`
  - `boolean available()` / `String name()` / `OdocConvertResult toLayout(byte[] docxBytes, String format, String fileName)`
- `org.springblade.workflow.spi.IOdocSealService`
  - `boolean available()` / `String name()` / `OdocSealResult sign(byte[] layoutBytes, OdocSealRequest req)` / `OdocVerifyResult verify(byte[] layoutBytes)`

 blade-workflow 通过注入 `List<IOdocFormatConvertor>` / `List<IOdocSealService>` 选取第一个
 `available()==true` 的实现；无则降级。

## 二、配置键

| 键 | 作用 | 装配条件 |
|---|---|---|
| `odoc.layout.vendor` | 版式转换厂商标识（shuke/yozo/fuxin/fuxin-gsdk…） | 设置后 `OfdVendorConvertor` 才装配 |
| `odoc.seal.vendor` | CA 签章厂商（qiyuesuo/…） | 设置后 `CaSealService` 才装配 |
| `odoc.file-service-base-url` / `file-upload-path` / `file-download-path` / `file-basic-auth` | 文件服务（blade-resource）下载/上传 | 版式产物存取 |

## 三、实现位（当前为脚手架，未接入 SDK 时返回明确失败）

- 版式：`src/main/java/org/springblade/workflow/odoc/layout/ofd/OfdVendorConvertor.java`
  → 在 `toLayout(...)` 内调用厂商 SDK 完成 `docx → PDF(12) / OFD(13)`，成功返回
  `OdocConvertResult.ok(content, format, fileName, vendor)`。
- 签章：`src/main/java/org/springblade/workflow/odoc/layout/seal/CaSealService.java`
  → 在 `sign(...)` 完成盖章 + RFC3161 时间戳，回填 `vendorSealId/certSn/algorithm/tsaToken`；
  在 `verify(...)` 做验章 + 篡改检测。

## 四、接入步骤

1. 在 `pom.xml` 的「厂商 SDK」占位处加入对应依赖，设 `<optional>true</optional>`。
2. 在上表两个实现类里替换脚手架逻辑为真实 SDK 调用（字节进/字节出，已接好）。
3. 配 `odoc.layout.vendor` / `odoc.seal.vendor`；必要时配 `odoc.file-service-*`。
4. 重启 blade-workflow：`GET /odoc/layout/available`、`/odoc/seal/available` 返回 true 即接入成功，
   前端「转版式 / 签章」入口自动出现。

## 五、说明

- 本模块是**纯库**（无启动类、不注册微服务），bean 包名在 `org.springblade.workflow` 下，
  由 blade-workflow 组件扫描自动拾取（optional 依赖在 classpath 时才生效）。
- OFD-A（归档级）转换在厂商 SDK 支持 `format=13` 的归档档级时一并覆盖；当前契义的 `13` 即 OFD，
  归档档级差异由厂商 SDK 参数控制（在 `toLayout` 内按需扩展 format 细分）。
