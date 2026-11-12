# 收发文落地 — 编辑器 WPS Web 统一改造方案

> **状态**：**P1–P4 代码与文档侧全部完成**；「后续独立排期」中可代码落地的 4 项（读源直读收口、Feign 补签名、`/save` 断链、auxGuard resource 绑定）**亦已收口并通过全量回归**；P0 决策已定稿为 **WPS Web**。剩余均为人工外部动作或高风险独立改造：P2.7 真实 WPS 中台联调、P3.2 密钥轮换、P3.5 网关路由与回调可达性实测、修订留痕中台能力核验（§7）、结束节点 endEvent 收敛（刻意暂缓），详见 §六 待确认 8 项。
> **创建**：2026-11-12　**目标**：收发文（odoc）在线编辑从 OnlyOffice 切到 WPS WebOffice，并与知识管理（blade-doc）**共用同一套公共能力**，不重复造轮子。
> **关联**：`收发文落地-编辑器选型与套红合成设计.md`、`收发文落地-验收清单.md`、`泛微E9-收发文与正文套红功能分析及SpringBlade落地方案.md`、`收发文落地-后端完成与前端交接.md`、`起止节点根治-实施设计与存量迁移.md`

---

## 〇、决策与技术核验结论

### 0.1 决策（P0，已定稿）

**正文编辑器统一采用 WPS Web（WebOffice SDK），不引入 OnlyOffice Docs Server。**

| 原待决策项 | 结论 | 依据 |
|---|---|---|
| D1 正文编辑器 | **WPS Web** | 知识管理已落地并配置；用户确认（2026-11-12） |
| D2 是否必须 OFD | **仍待答**（与编辑器无关） | OFD 版式输出走 `BuiltinOfdConvertor` 或 L2 厂商 SDK，不随编辑器改变 |
| D3 AGPL 合规 | **自动消解** | 不再引入 OnlyOffice（AGPL），改走 WPS 商业授权 |

选型对比表（`收发文落地-编辑器选型与套红合成设计.md` §1.2）中 WPS 的优势成立：与 E9 对齐度最高（E9 的 WPS 路径）、OFD/国产化支持好、原生修订留痕。

### 0.2 核验结果（2026-11-12，两处更正此前结论）

**① 「知识管理是否定了 WPS Web」——是，此前判断有误。**

- `doc/sql/blade/blade-2026-11-04_164016-dump.sql:5064` 的 `doc_engine_setting` 有 6 行落库配置：`default_engine=wps`、`third_platform=wps`、`wps_app_id`、`wps_app_secret`、`wps_jssdk_url`、`wps_public_base=https://www.zwymding.top`。
- 此前"默认仍 Univer、WPS 只是备选"的结论，源于**当前 dev 库 `doc_engine_setting` 为 0 行（已被清空）**，只看了运行时状态、未看历史配置，属误判。
- 代码侧 `DocOnlineEditorController.java:87` 的 `default_engine` 默认值仍为 `"univer"`，需在 P1 补为 `wps`（库表有值时以库表为准）。

**② WPS 控制台回调地址：单入口假设成立，入口必须留在 `blade-doc`。**

- `wps_public_base=https://www.zwymding.top` 说明 WPS 中台回调按**一个公网 base** 登记。
- `doc/sql/migration/V2026.10.22_004__nacos_blade_doc_config.sql` 的网关路由只配了 `- id: blade-doc` / `Path=/blade-doc/**` / `StripPrefix=1`。
- `WpsCallbackController` 的路径是 `/v3/3rd/**`（**不带服务前缀**）。因此回调入口保留在 `blade-doc`，否则需改 WPS 开放平台控制台登记地址（外部动作，需人工）。
- ✅ P3.5 已补白名单（`AuthProvider.DEFAULT_SKIP_URL` + 网关 dev yml），但**仅加白名单不足以让回调可达**：`/v3/3rd/**` 不带服务前缀，还需网关追加路由或 nginx 改写，见「3.5 部署片段」；实测可达仍待人工（回调不带 `Blade-Auth`）。

**③ SDK 能力核验：修订留痕是硬风险，`WpsSign` 确认死代码。**

`ant-design-pro/public/web-office-sdk-solution.umd.js`（v2.0.7，29,454 B）是 WebOffice **加载器**：

| 关键字 | 命中 |
|---|---|
| `OfficeType` / `Writer` / `Spreadsheet` / `init` / `destroy` / `save` / `export` / `token` | 有 |
| `revision` / `Revision` / `track` | **0** |
| `_w_signature` / `X-WebOffice-Token` | **0**（仅 1 处 `_w_appid`） |

- SDK **不暴露程序化清稿/留痕 API**。对比表 N3 的 ✅ 指 WPS 编辑器内部自带 track changes（随 docx 保存），**非我方可调能力**。`泛微E9-收发文与正文套红功能分析及SpringBlade落地方案.md:821` 列的 `AcceptRevision`/`ClearRevisionAndComent`/`CreateTraceDocument` 属 E9 客户端控件能力，本 SDK 无对应物。
- `WpsSign`（`blade-doc/.../sign/WpsSign.java`，SHA1 `_w_*`）的 `clientSign`/`serverVerify` **全项目零调用点**；`DocEditorConfig.wpsSignature` 从未赋值。实际鉴权走 `DocUrlSigner` 签发的 `token`，由前端 `WebOfficeSDK.init({token})` 透传、WPS 中台回调时经 `X-WebOffice-Token` 回传。
- **结论**：P3 删除或降级 `WpsSign`（不按"已有验签"沿用）；留痕/清稿需在 P0 补一次 WPS 中台服务端 API 能力核验，未确认前不得在方案里宣称"已支持"。

### 0.3 现存两套实现对照（核验事实）

| 维度 | 知识管理 `blade-doc` | 收发文 `blade-workflow/odoc` |
|---|---|---|
| 编辑器 | WPS WebOffice v2.0.7（`WebOfficeSDK.init`） | **WPS WebOffice（默认，`editor=wps`）+ OnlyOffice DOCS（降级，`editor=onlyoffice`）** |
| 前端组件 | `ant-design-pro/src/pages/DocEngine/Editor/WpsEditor.tsx` | `OdocEditorModal.tsx` 按 `editorType` 分流：wps→`WpsEditor`、onlyoffice→`DocsAPI.DocEditor` |
| 配置 | `DocProperties.Wps`（`blade.doc.wps.*` + 库表 `doc_engine_setting`） | `OdocProperties` + 共用 `WpsProperties`（`blade.wps.*`） |
| 配置 VO | `DocEditorConfig`（含 WPS 专属字段） | `OdocEditorConfigVO`（OnlyOffice 降级）/ `WpsEditorConfig`（WPS，来自公共层） |
| 回调协议 | `WpsCallbackController`：`/v3/3rd/files/*` v3，三阶段保存 | 元数据经 `IWpsOdocFileClient`→blade-doc 单入口；字节流直连 `WfOdocWpsController#/odoc/wps/**` |
| 签名 | `DocUrlSigner` → `WpsUrlSigner`（`blade.wps.url-sign-secret`） | `OdocUrlSigner` → `WpsUrlSigner`（同密钥，R2 已统一） |
| 版本模型 | `DocImageFile`（`versionId` = 新 fileId，`docFileType=2`） | `WfOdocDocVersion`（`versionId` 为 doc 内自增序号） |
| 文件存储 | `IDocFileStore#save(byte[])` | `IOdocFileStore#storeFromUrl/store/download` |

---

## 一、目标架构

WPS v3 回调协议（`/v3/3rd/files/*`）**与业务无关**；唯一耦合业务的是「fileId → 文件/元信息/版本链/保存落库」。因此通用点抽在这一层，而非协议层。

```
                  WPS WebOffice 中台（同一 appId，回调 base = https://www.zwymding.top）
                                 │ /v3/3rd/**
                                 ▼
   ┌──── 单一回调入口（blade-doc 承载，路径不变）──────────────────────┐
   │  blade-common :: org.springblade.common.wps                     │
   │   WpsCallbackController（协议骨架，纯编排）                        │
   │   WpsProperties / WpsUrlSigner / WpsFileId / WpsEditorConfig     │
   │   WpsFileMeta / WpsCallbackSupport / WpsFileProvider(SPI)        │
   └──────────┬───────────────────────────────┬─────────────────────┘
              │ SPI: WpsFileProvider           │
     ┌────────┴─────────┐          ┌───────────┴──────────┐
     │ DocWpsProvider   │          │ OdocWpsProvider       │
     │ blade-doc 本地   │          │ Feign → blade-workflow│
     │ DocImageFile     │          │ WfOdocDocVersion      │
     └──────────────────┘          └──────────────────────┘
```

**关键设计：字节流不经过回调入口。** `download` / `versionDownload` / `upload/address` 本就返回绝对 URL，让它们直接签发指向文件归属服务的签名 URL（WPS 中台直连）；入口只做元数据编排，跨服务不传大文件。

**公共层落点**：`blade-common`——`blade-doc/pom.xml:19` 与 `blade-workflow/pom.xml:19` 均已依赖它，零新增模块。公共层**零新增运行时依赖**（纯 JDK + lombok，POJO/工具/SPI，不注册 Spring Bean），避免影响两个服务的依赖树与启动。

---

## 二、公共模块 `blade-common`（P1 已落地）

新增包 `org.springblade.common.wps`：

| 类 | 职责 |
|---|---|
| `WpsProperties` | `blade.wps.*` 统一配置：`appId/appSecret/jssdkUrl/publicBase/urlSignSecret/urlSignTtlSeconds/callbackEnabled/callbackAuthStrict`；`isConfigured()` 双凭证判定 |
| `WpsUrlSigner` | HMAC-SHA256 + 时效，纯 JDK 实现；签名串 `exp.hex(hmac(resource:exp))` 与存量两实现**逐字兼容** |
| `WpsFileId` | 结构化 `fileId = {domain}:{bizId}`（`doc`/`odoc`），避免两侧雪花 ID 撞车；`parseFor` 做域隔离防越权；**纯数字按 `doc` 解析**以兼容存量链接 |
| `WpsEditorConfig` | 前端统一配置 VO，收编 `DocEditorConfig` 的 WPS 字段与 `OdocEditorConfigVO` |
| `WpsFileMeta` | v3 协议文件/版本元信息中间态（8 字段） |
| `WpsFileProvider` | 业务存储 SPI：`domain/publicBase/info/downloadUrl/uploadUrl/saveNewVersion/versions/versionDownloadUrl/rename/permission`。**`uploadUrl` 首参是 wireFileId 而非 bizId**——必须原样回带 WPS 中台会话里的 fileId，否则三阶段保存的 PUT 与会话脱节 |
| `WpsCallbackSupport` | 协议工具：`ok/err` 响应包装、`tokenValid` **硬校验**、`userQuery`、`publicBase`、`versionItem` |

**收编（消重，非复制）**：
- `blade-doc/.../sign/DocUrlSigner.java` → 薄委托 `WpsUrlSigner`（按 `blade.doc.*` 装配）
- `blade-workflow/.../odoc/OdocUrlSigner.java` → 薄委托 `WpsUrlSigner`（按 `odoc.*` 装配）
- 两者共 89 行重复 HMAC 实现删除；注入点不变，存量已签发 URL 完全兼容。

**验证**（2026-11-12）：
- `blade-common`：13 例单测 `WpsCommonTest`（签名格式兼容、篡改/过期拒绝、**跨模块密钥必须一致**、fileId 域隔离、legacy 兼容、协议工具、配置判定）→ `13/0 失败`
- `blade-doc` + `blade-workflow`：`BUILD SUCCESS`，全量 `346 / 0 失败 / 0 错误 / 1 skip`

⚠️ **R2 已前移解决（P3.1 提前到 P2 前）**：`odoc` 与 `blade-doc` 的 URL 签名现已共用一把 `blade.wps.url-sign-secret`（doc 侧 `DocUrlSigner` 优先 `wps.getUrlSignSecret()` 回退 `doc` 本地密钥；odoc 侧 `OdocUrlSigner` 同理回退 `odoc`），两侧签发/验签同源，odoc 路径回调不再被公共入口拒签。单测 `verify_rejectsDifferentSecret_acrossModules` 仍固化「跨模块密钥必须一致」约束。P3.2/3.3/3.5 仍待办。

---

## 三、按优先级排期（**按顺序推进**）

### P1 · 公共层抽取 + 知识管理迁移　【进行中】

| # | 任务 | 状态 |
|---|---|---|
| 1.1 | 建 `blade-common` WPS 公共层 7 类 | ✅ 已落地 |
| 1.2 | `WpsCommonTest` 13 例单测 | ✅ 已通过 |
| 1.3 | `DocUrlSigner`/`OdocUrlSigner` 收编为委托 | ✅ 已落地，全量 346 无回归 |
| 1.4 | 装配 `WpsProperties`（`DocWpsConfiguration` / `OdocWpsConfiguration`） | ✅ 已落地；`blade-doc` 另装配 `WpsUrlSigner`，**显式复用 `DocProperties` 密钥**保证签发/验签同源 |
| 1.5 | `DocWpsProvider` + `WpsCallbackController` 瘦身为协议编排层 | ✅ 已落地：控制器 593 行 → 纯编排（不再访问业务表），按 `WpsFileId.domain()` 路由；`odoc` 域未注册时返回 40005，P2 加 Provider 即自动生效 |
| 1.6 | 默认引擎 `univer` → `wps`；恢复 `doc_engine_setting` | ✅ 代码已切；迁移脚本 `V2026.11.12_001__doc_engine_setting_wps_restore.sql` 已落盘（幂等，恢复 4 行非敏感配置；**app_id/app_secret 不落明文**，由环境变量/Nacos 注入） |
| 1.7 | `WpsDocOnlineEditor` 改用公共 `WpsEditorConfig` | ✅ 已落地；`DocEditorConfig` 新增 `wpsConfig` 公共契约字段，旧 `wps*` 保留为**兼容投影**（`WpsEditor.tsx` 仍在读），P2.6 前端统一后删除 |
| 1.8 | 知识管理 WPS 协议层回归 | ✅ `WpsCallbackProtocolTest` 13 例通过（fileId 路由、v3 响应、legacy 兼容、uploadUrl 回带、未知域拒绝）；**真实 WPS 中台全链路未跑**，见下 |

**验收结果**（2026-11-12）：
- `blade-common` 13 例 + `blade-doc` 15 例（协议 13 + 原有 2）+ `blade-workflow` 346 例，`0 失败 / 0 错误 / 1 skip`
- 三模块 reactor 打包 `BUILD SUCCESS`（`blade-common → blade-doc → blade-workflow`）
- ⚠️ **未覆盖**：WPS 中台是外部依赖，真实「发起编辑 → 三阶段保存 → 历史版本」全链路需在联调环境人工验收；`/v3/3rd/**` 网关白名单（P3.5）未实测回调可达。此项不因自动化测试通过而视为完成。

### P2 · 收发文接入（新功能）　【基本完成，2.7 联调待人工】

| # | 任务 | 状态 |
|---|---|---|
| 2.1 | `blade-workflow-api` 新增 `IWpsOdocFileClient`（Feign）：取元数据/版本链、触发保存 | ✅ 已落地 |
| 2.2 | `OdocWpsProvider` 实现 SPI：`WfOdocDocVersion` 版本链、`IOdocFileStore.store(bytes,name)`、`rename` 对不可变版本为空操作、字节流直连 workflow `/odoc/wps/**` | ✅ 已落地，含 7 例协议测试 |
| 2.3 | `WfOdocDocServiceImpl#initDoc(...,editor)` 由 OnlyOffice 结构改 `WpsEditorConfig`（`buildWpsEditorConfig`：`domain=odoc`、wireFileId=`odoc:{docId}`、token 共用 `blade.wps.url-sign-secret` 签发、`contentUrl`→`/odoc/wps/content`）；OnlyOffice 仍作 `editor=onlyoffice` 降级分支 | ✅ 已落地 |
| 2.4 | `WfOdocWpsController`：新增 `/odoc/wps/content`(下载)/`/odoc/wps/upload/{fileId}`(三阶段 PUT)/`/meta`/`/versions`/`/version-meta`（供 blade-doc Feign 取）；全 permitAll 靠 URL 签名+内网隔离 | ✅ 已落地 |
| 2.5 | `OdocDocInitVO`：`editorType`/`wpsEditorConfig` 字段、`canEdit` 判定；`OdocEditorConfigVO` OnlyOffice 字段保留降级 | ✅ 已落地 |
| 2.6 | 前端统一：`WpsEditor.tsx` 兼容 `WpsEditorConfig` 标准字段名（回退 DocEngine 旧命名）；`OdocEditorModal.tsx` 按 `editorType` 分流（wps→`WpsEditor`，onlyoffice→原 DOCS）；`odocApi.init` 加 `editor` 参数（默认 auto）；`OdocDocInit` 增 `wpsEditorConfig` 类型 | ✅ 已落地（TS 无 lint 错误） |
| 2.7 | 验收清单第 5/11/12 项改写并跑通：编辑→保存→新版本→套红→取消套红 | ⏳ 待人工联调（WPS 中台外部依赖，见 §0.2 ③/§六） |

**验证**（2026-11-12）：
- `blade-common` 13 例 + `blade-doc` 22 例（协议 13 + `OdocWpsProviderTest` 7 + 原有 2）+ `blade-workflow` 346 例，`0 失败 / 0 错误 / 1 skip`
- 三模块 reactor 打包 `BUILD SUCCESS`

⚠️ **未覆盖**：WPS 中台是外部依赖，odoc 真实「发起编辑 → 三阶段保存 → 历史版本」全链路需在联调环境人工验收（即 2.7）；`/v3/3rd/**` 网关白名单（P3.5）未实测回调可达。此项不因自动化测试通过而视为完成。

**验收**：`收发文落地-验收清单.md` 15 项中在线编辑相关项在 WPS 下全通过；OnlyOffice 降级路径可用。

### P3 · 安全加固与配置收口　【代码侧完成；3.2 轮换 / 3.5 实测待人工】

| # | 任务 | 状态 |
|---|---|---|
| 3.1 | 统一 `blade.wps.url-sign-secret`（见 §二），密钥迁 Nacos/密钥管理 | ✅ 已前移解决：doc/odoc signer 均优先共享密钥、回退本地密钥；"密钥迁 Nacos"属部署动作，见 3.2 |
| 3.2 | 明文 `app-id/app-secret` 收口；历史 dump 中的明文密钥按泄露处理（轮换） | ⏳ **待人工决策/执行**，见下方「3.2 结论与操作步骤」 |
| 3.3 | `softCheck` 临时放行 → `WpsCallbackSupport.tokenValid` 硬校验 | ✅ 已落地：`authGuard`（12 个 fileId 端点，HMAC 硬校验、拒绝发生在 Provider 路由前）+ `auxGuard`（`/users`、`/object/**` 4 个原本**完全无鉴权**的端点，形状+时效校验）；由 `blade.wps.callback-auth-strict` 控制，默认 `true` |
| 3.4 | 删除/降级死代码 `WpsSign`；明确 v2.0.7 签名口径 | ✅ 降级保留：`WpsSign` 与 `DocEditorConfig.wpsSignature` 标 `@Deprecated` 并写明真实鉴权链路；修正 `WpsDocOnlineEditor` 中"签名规则见 WpsSign"的误导性注释。**v2.0.7 口径仍待 P2.7 联调确认**（SDK 加载器内无 `_w_signature`） |
| 3.5 | 网关 `secure.skip-url` 加 `/v3/3rd/**` 并实测回调可达 | ✅ 配置已补（`AuthProvider.DEFAULT_SKIP_URL` + `blade-gateway/application-dev.yml`，含带前缀形态）；⏳ **网关路由/nginx 改写与实测可达待人工**，见下方「3.5 部署片段」 |
| 3.6 | 越权回调用例：伪造/过期 token、`doc` 域 fileId 打 `odoc` Provider 均须拒绝 | ✅ 已落地：`WpsCallbackProtocolTest` 新增 6 例（无 token 全端点拒绝且不触达 Provider、伪造/过期/错 resource、`strict=false` 放行、辅助端点拒绝/放行、跨域 token 与 Provider 隔离）；另修 `WfOdocWpsController#upload` 的 `parse`→`parseFor(fileId,"odoc")` 域校验缺口 |

**验证**（2026-11-12，含后续开发项收口后复跑）：`blade-common` 15 + `blade-doc` 33 + `blade-workflow` 346（1 skip，既有基线），`0 失败 / 0 错误`；`blade-workflow-api` install、`blade-gateway` 编译均 `BUILD SUCCESS`。

#### 3.2 结论与操作步骤（需你决策，本轮**未改** dev 配置）

**判断**：不做"只删明文默认值"的半截修复。原因：① 密钥已在 git 历史与 `doc/sql/blade/blade-2026-11-04_164016-dump.sql:5064` 中，删默认值不等于消除泄露，真正修复是**在 WPS 控制台轮换**；② 删掉默认值会让 dev 的 `WpsDocOnlineEditor.isEnabled()` / `WpsProperties.isConfigured()` 立刻变 false，WPS 编辑在联调未完成前直接不可用。这属于项目级决策，不由本轮代码改动驱动。

已确认的明文落点（3 处）：
- `blade-service/blade-doc/src/main/resources/application.yml:16-17`
- `blade-service/blade-workflow/src/main/resources/application-dev.yml:39-40`
- `doc/sql/blade/blade-2026-11-04_164016-dump.sql:5064`（`doc_engine_setting` 的 `wps_app_id`/`wps_app_secret`）

建议顺序（轮换完成后一次性切换，可回滚）：
1. WPS 开放平台控制台轮换 `appSecret`（人工），同步核对 `appId` 是否变化。
2. 生产/Nacos 下发 `WPS_APP_ID` / `WPS_APP_SECRET` 环境变量或 `blade.wps.app-id/app-secret`，确保两服务取到同一套。
3. 把两个 yml 的 `${WPS_APP_ID:明文}` / `${WPS_APP_SECRET:明文}` 改为**无默认值**的 `${WPS_APP_ID}` / `${WPS_APP_SECRET}`，并让 `isConfigured()` 拒绝空值（现状已拒绝）。
4. 历史 dump 按泄露凭据处理：不要试图"改掉"已提交的历史文件，以轮换为准；如需保留该文件用于初始化，改写成占位值 + 注释。
5. `blade.wps.url-sign-secret` 一并迁 Nacos 高熵值，dev 与生产使用不同密钥。

⚠️ 步骤 3 落地后，dev 若未注入环境变量，WPS 编辑会不可用（`isConfigured()=false`）。回滚 = 恢复原默认值，故请在轮换完成后再执行。

#### 3.5 部署片段（人工按现场拓扑二选一，本轮**未发布 Nacos**）

`/v3/3rd/**` 不带服务前缀，而现有网关路由只有 `Path=/blade-doc/**` + `StripPrefix=1`，因此**仅加白名单不足以让回调可达**，还需让请求落到 blade-doc：

- **方案 A（网关加路由）**：在 Nacos `blade.yaml` 的 `spring.cloud.gateway.routes` 追加（勿整文件覆盖）：

  ```yaml
  - id: blade-wps-callback
    uri: lb://blade-doc
    predicates:
      - Path=/v3/3rd/**
  ```

  不要 `StripPrefix`，控制器路径本身就是 `/v3/3rd/**`。
- **方案 B（nginx 改写）**：把 `/v3/3rd/(.*)` rewrite 成 `/blade-doc/v3/3rd/$1`，复用既有 `blade-doc` 路由的 `StripPrefix=1`。

白名单两处已落地（代码层 `AuthProvider.DEFAULT_SKIP_URL` 不受 Nacos 覆盖影响；`blade-gateway/application-dev.yml` 同步）。**⚠️ 安全耦合**：白名单只在 `callback-auth-strict=true` 时可开；若联调临时设为 `false`，必须同步移除白名单，否则回调整体裸奔。

**实测待办**：不带 `Blade-Auth` 打 `https://www.zwymding.top/v3/3rd/files/doc:1`，预期返回 `40101`（token 缺失被硬校验拒绝）而非 401/404 —— 能拿到 40101 即证明「网关可达 + 白名单生效 + 硬校验生效」三者同时成立。

#### P3 残留风险（2026-11-12 续：3 项已收口，2 项待人工/保留）

| 项 | 状态 / 说明 |
|---|---|
| 辅助端点 resource 未绑定 | ✅ **代码已就绪，默认关闭**：`WpsProperties.callbackAuthBindResource` 开启后，`auxGuard` 从 `X-WebOffice-File-Id`（或 query `file_id`）取 fileId 并按 `wps:{fileId}` 做完整 HMAC 验签，缺 fileId 一律拒绝；单测覆盖匹配/不匹配/缺 fileId/query 兜底。**默认 false 仍是形状校验**——WPS 中台回调头形态未核验，开启前须由 P2.7 联调确认 fileId 与前端 init 签发 token 时的 wireFileId 逐字一致（尤其知识管理用纯数字 legacy fileId，中台若回传 `doc:123` 会误拒） |
| Feign 内部元数据端点无签名 | ✅ **已修**：4 个方法统一带 `X-Wps-Internal-Token`，resource 由 `WpsCallbackSupport.internalResource(op,bizId)` = `wps:odoc:{op}:{bizId}` 约定，两侧共享密钥；`/meta`/`/versions`/`/version-meta` 与新增 `/save` 均 `requireInternal` 硬校验。**顺带修掉更隐蔽的契约错误**：Feign 原声明裸 `WpsFileMeta`，而服务端返回 `R<WpsFileMeta>`，反序列化会得到字段全默认值的空对象（不报错但数据全丢）；现改为 `R<T>` + `getData()`，并加 null/空列表兜底 |
| `/odoc/wps/save` 断链 | ✅ **已补齐**：`WfOdocWpsController` 新增 `POST /odoc/wps/save`（octet-stream，内部签名 + 空内容校验），与 `IWpsOdocFileClient#save` 契约对齐；正常三阶段仍走直连 PUT |
| `softCheck` 应急开关 | ⚠️ **保留**：`callback-auth-strict=false` 是联调排障口子，必须与网关白名单互斥使用（已在代码注释与本文标注） |
| 服务间 token 时效 | ⚠️ 沿用 `WpsUrlSigner` 默认 300s；Feign 调用为同步即时请求，超时风险低；若将来加异步重试需在重试时重新签发 |

### P4 · 文档与验收回写　【已完成】

统一采用「**保留历史选型过程记录 + 顶部/关键段落醒目标注定稿口径**」的方式，不把合并前的设计改得失真、保持可追溯。每份文档均指向本文为唯一权威口径。

| 文档 | 回写内容 | 状态 |
|---|---|---|
| `收发文落地-编辑器选型与套红合成设计.md` | 顶部定稿声明；§1.3 主编辑器改 WPS Web（公共层 + 单入口 + 留痕硬约束）；§1.4 前端改 `OdocEditorModal` 分流 + v3 单入口 + 字节流直连；§九 D1 标已定稿、D3 标消解、**D2 保留待答** | ✅ |
| `收发文一体化技术方案.md` | 顶部定稿声明；§2.7.3 决策改 WPS Web；§2.8 D1/D3；另同步 §十三 P1 前置决策、实施步骤 3、验收第 1 条、P0 决策清单的 OnlyOffice 残留描述 | ✅ |
| `泛微E9-收发文与正文套红功能分析及SpringBlade落地方案.md` | §11.1 选型建议后加定稿声明；§十三 P1 决策列；§十五 结论 5 | ✅ |
| `收发文落地-后端完成与前端交接.md` | 组件清单补 `WpsEditor.tsx`；新增 §2.1「WPS 统一改造后的前端组件与契约」（4 个前端文件变更表 + 后端契约要点 + token 说明） | ✅ |
| `收发文落地-验收清单.md` | 第 5 项改 WPS 在线编辑（含 OnlyOffice 降级）；§附 L2 补「编辑器与 OFD 解耦」；新增 W1–W9 人工联调验收表与联调禁忌 | ✅ |
| `odoc-联调验收-runbook.md` | 新增 §7 WPS 联调：控制台前置、配置检查、路由二选一、重启顺序、`40101` 冒烟、全链路 W2–W8、回滚 | ✅ |

**结论**：P1–P4 文档与代码侧全部落地；剩余均为**人工外部动作** —— P2.7 真实 WPS 中台联调、P3.2 密钥轮换、P3.5 网关路由与可达性实测，以及 §六 待确认 8 项。

### 后续（独立排期，不阻塞 P1–P4）

**本轮（2026-11-12 续）已推进**：

| 项 | 状态 |
|---|---|
| 读源切换补录隐患（§5.3.3） | ✅ **已修**。核查发现文档记的 `:2557`/`:2582` 两处早已走 gated `loadNodes`（行号漂移），真正残留是 `WfInstanceServiceImpl#loadNode` 直查 `nodeMapper`，被 8 处运行期调用（advance / reject / 归档 / 异常兜底 / 子流程附加操作）。现改为委托 `loadNodeReadOnly(defId,nodeKey)`，与 `loadNodes`/`findStartNodeKey` 统一读源。8 处调用均只读 `nodeType`/`nodeName`/`extJson` 或传给 `rejectManager`/`nodeActionExecutor`，不依赖 BPMN 节点没有的 DB 主键 id；默认配置（开关关闭且未退役）下仍走同一条 `nodeMapper` 查询，**行为完全等价、零回归** |
| Feign 内部端点补签名 | ✅ 见上方「P3 残留风险」表 |
| `/odoc/wps/save` 断链 | ✅ 同上 |
| `auxGuard` 按 resource 绑定 | ✅ 代码就绪，开关默认关闭，待联调确认后开启 |

**仍需单独排期**：

- **修订留痕/清稿**（**硬约束，P0 遗留**）：SDK 无 API，需先做 WPS 中台服务端能力核验，见下方 §7 核验清单。未确认前不得宣称支持。
- **结束节点 endEvent 收敛**（`起止节点根治-实施设计与存量迁移.md` §5.3.2）：**仍未动，刻意暂缓**。归档人/结束节点结构要从 `wf:node` 收敛到 `endEvent`，牵涉设计器建模、BPMN 生成、发布/测试态部署、存量定义迁移与流转意见解析全链路，属高风险结构改造；在 P2.7 全链路联调跑通前动手，会把「配置问题」和「结构改造问题」混在一起无法定位。建议独立排期并配套清库/重建验证。
- 前端设计器联调验收、odoc 完整业务 E2E。

#### §7 修订留痕 / 清稿 —— WPS 中台服务端能力核验清单（待人工）

> 前端加载器 `web-office-sdk-solution.umd.js`（v2.0.7）中 `revision` / `track` / `AcceptRevision` 关键字 0 命中，
> 说明**我方无法从 WebOfficeSDK 程序化触发**清稿/接受修订；对比表 N3 的 ✅ 仅指 WPS 编辑器内部随 docx 保存的原生 track changes。
> E9 的 `AcceptRevision` / `ClearRevisionAndComent` / `CreateTraceDocument` 属客户端控件能力，本 SDK 无对应物。

需向 WPS 中台/开放平台确认（按顺序，任一项为「否」即走降级）：

| # | 核验项 | 若为「是」的落地点 | 若为「否」的降级口径 |
|---|---|---|---|
| 1 | 中台服务端是否提供「接受所有修订 / 清稿」API（入参 fileId + 版本，输出新 docx） | 在 `IOdocComposeEngine` 侧新增 `acceptRevisions(byte[])`，套红前调用并落「清稿前版本」 | 套红前不清稿，明确告知用户「红头模板会带修订标记」，由人工在 WPS 内先接受修订 |
| 2 | 是否能导出/生成「痕迹文档」（类似 E9 `CreateTraceDocument`） | 新增 `WfOdocTraceService`，作为独立版本类型或关联资源存档 | **只做版本留痕**：每次保存产新版本 + 流转意见记录，不宣称「痕迹文档」能力 |
| 3 | 保存回调里能否拿到修订统计（修订数/作者/时间） | 落 `wf_odoc_doc_version` 扩展字段，供套红节点「是否允许带修订提交」校验 | 无统计，套红拦截仅依据 `hasUsedTemplet` |
| 4 | 清稿操作是否需要额外授权/计费、是否影响并发 | 计入授权评估与限流 | 维持现状 |
| 5 | 服务端 API 是否为私有化部署可用（非仅公有云） | 走 `OdocProperties` 配置端点 + 独立签名 | 走降级 |

**确认前禁止**：在方案/验收文档中写「已支持修订留痕/清稿」；在 `WpsEditor` 前端加相关按钮；把 `WpsSign`（已 `@Deprecated`）当作已有验签能力接入。

---

## 四、OnlyOffice 去留策略

**保留为可选降级（`editor=onlyoffice`），默认切 WPS，不删除代码**：

- `IOdocFileStore#storeFromUrl` 等仍被其它路径使用；保留一条回退能力。
- `OdocProperties.docServerUrl` 为空即隐藏在线编辑（现有行为），改造后由 `WpsProperties.isConfigured()` 接管"是否可编辑"判定。
- 不部署 DOCS 服务器 → 省一套组件、规避 AGPL 合规风险。

---

## 五、风险登记

| # | 风险 | 等级 | 处置 |
|---|---|---|---|
| R1 | **SDK 无程序化留痕/清稿 API** | 高 | P0 补 WPS 中台服务端能力核验；未确认前不宣称支持 |
| R2 | 两侧签名密钥不同，跨服务 URL 不互验 | 高 | ✅ 已消解：共享 `blade.wps.url-sign-secret`（单测固化） |
| R3 | `softCheck` 临时放行 + 明文密钥 | 高 | ✅ 硬校验已落地（P3.3）/ ⏳ 明文密钥轮换待人工（P3.2） |
| R4 | `/v3/3rd/**` 网关白名单缺失、回调不可达 | 中 | ✅ 白名单已补（P3.5）/ ⏳ 路由与实测可达待人工；白名单必须与硬校验同时开启 |
| R9 | 辅助端点 `/users`、`/object/**` 曾完全无鉴权（含写库） | 高 | ✅ P3.3 补 `auxGuard`；⚠️ 仅形状+时效校验，resource 绑定待 P2.7 确认后收紧 |
| R5 | `doc_engine_setting` 当前 dev 库 0 行，WPS 配置丢失 | 中 | P1.6 恢复；确认是否需重新登记 WPS 控制台 |
| R6 | 收发文 `hasUsedTemplet` 套红态在 WPS 保存后的语义 | 中 | P2.2 明确：编辑正文后是否重置套红标记，需产品确认 |
| R7 | fileId 撞车（两侧均为雪花 ID） | 中 | 结构化 `WpsFileId` + 域隔离（已落地） |
| R8 | OFD 输出与编辑器选型被混淆 | 低 | 明确 OFD 走 `BuiltinOfdConvertor`/L2 SDK，与编辑器无关 |

---

## 六、待确认（阻塞 P2.7 联调 / P3.2 轮换 / P3.5 实测）

1. **WPS 开放平台控制台登记的实际回调地址**：项目内只查到 `wps_public_base=https://www.zwymding.top` 与网关 `blade-doc` 路由，需人工登控制台确认登记路径、appId 是否同一套、是否允许回调路径调整。
2. **WPS 中台是否原样回传 `X-WebOffice-Token`**：P3.3 硬校验依赖此假设。联调时若不回传，回调会全部返回 `40101`；此时**不要**直接把 `callback-auth-strict` 设为 `false`（会与已开的网关白名单组合成裸奔），应先定位中台回调头再决定。
3. **辅助端点回调头是否带 `file_id`**：代码已就绪（`callbackAuthBindResource` 开关，默认关闭），单测已覆盖。联调需确认中台回传的是 `X-WebOffice-File-Id` 头还是 query `file_id`，且其值须与前端 init 签发 token 时的 wireFileId 逐字一致，再决定开启开关（P3 残留风险）。
4. **网关/nginx 拓扑**：`/v3/3rd/**` 到 blade-doc 由谁转发，决定 3.5 选方案 A（网关加路由）还是 B（nginx rewrite）。
5. **密钥轮换窗口**（P3.2）：appSecret 轮换后 dev 环境变量/Nacos 何时同步，以便一次性移除 yml 明文默认值。
6. **修订留痕/清稿**：WPS 中台服务端是否提供 `AcceptRevision`/`CreateTraceDocument` 类能力？若无，「修订留痕」需降级为"随 docx 保存的原生修订 + 我方只做版本留痕"。
7. **套红态语义**（R6）：WPS 编辑保存后 `hasUsedTemplet` 是否重置。
8. **WPS 商业授权**：appId/secret 是否为生产授权、并发数与有效期。
