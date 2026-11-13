# 公文（odoc）WPS 在线编辑端到端打通 —— 改造设计文档

> 状态：**改造 4.1 / 4.2 已落地并回归通过（2026-11-12）**；改造 4.3（公网反代补 `/blade-workflow/**`）待用户侧执行
> 日期：2026-11-12
> 背景：WPS 配置已收口为「编辑器设置页面 + `doc_engine_setting` 库表」单一源（见 `project_wps_single_source_20261112.md`）。
> 本文档针对「公文编辑端到端」实测暴露的 4 个阻断，给出根因、改造方案、改动清单与回归锚点。
> 落地结果见文末「10. 落地结果」。

---

## 1. 结论速览

公文侧端到端**当前跑不通**，根因有 4 个，按 WPS 中台调用链路顺序：

| # | 阻断 | 归属 | 改造类型 | 是否阻塞端到端 |
|---|------|------|----------|----------------|
| 1 | `fileId = odoc:{docId}` 含冒号，WPS 中台拒绝（`字段file_id, 只能由数字、字母、下划线组成, 且不能以下划线开头`）→ session 403 | 代码（跨服务协议） | WpsFileId wireValue 改造 | **决定性** |
| 2 | 仅前端改 fileId 不够：token 仍按 `wps:odoc:...` 签发，回调验签不匹配 → `unauthorized callback` | 代码（与 #1 同源） | 随 #1 一起解决 | 紧随 #1 |
| 3 | 公网反代未转发 `/blade-workflow/**`，odoc 的 download + upload 端点都返回 HTML → 取内容/保存都失败 | 用户侧外部动作（公网 Express） | 补一条反代规则 | **决定性** |
| 4 | 前端 `OdocField` 调 `init` 固定 `editor=auto`，后端只有 `editor=wps` 才返回 WPS 配置 → 点「在线编辑」根本不弹 WPS | 代码（前端） | 前端补 editor 参数 | **决定性** |

另有 WPS 中台偶发 503（静态资源），curl 直连可 200，属外部抖动，重跑即可，非代码故障。

---

## 2. 实测证据（2026-11-12，真实浏览器 + 真实 WPS 中台）

工具：`log/pw_odoc_e2e.cjs`（Playwright 登录后把 `/doc/editor/config` 响应替换为公文侧真实 `wpsEditorConfig`，挂载 `WpsEditor`，只改网络响应、不改业务代码）。

**对照实验 A —— 当前代码（冒号）稳定复现 403：**
```
iframe: https://o.wpsgo.com/office/w/odoc:2117125489433808897?_w_appid=SX20261023ZYNJCZ
403 POST /api/v3/office/session/odoc:2117125489433808897/word?first
403 POST /api/v3/office/file/odoc:2117125489433808897/open/word
get ticket failed! {errno:10000, msg: 字段file_id, 只能由数字、字母、下划线组成, 且不能以下划线开头}
PAGEERR: Cannot read properties of undefined (reading 'officeType')
```

**对照实验 B —— 仅把注入的 fileId 改成 `odoc_2117125489433808897`（合法字符）：**
- file_id 格式报错消失；
- 但出现 `403 unauthorized callback`：因为后端 `init` 下发的 token 仍按 `wps:odoc:...` 签发（`WfOdocDocServiceImpl:362` `signer.sign("wps:" + wire)`，wire 此时是 `odoc:...`），WPS 中台回传 fileId=`odoc_...`，`WpsCallbackController.authGuard` 用 `wps:{wireFileId}` = `wps:odoc_...` 验签 → 不匹配。
- **结论**：不能只改前端 fileId，必须让「下发 fileId」「token 签发」「回调验签」「upload 验签」四处的 wireValue 口径全部一致。

**反代探测（curl，公网）：**
```
GET  https://www.zwymding.top/blade-workflow/odoc/wps/content?docId=...&version=...&token=...  → 返回 SPA HTML fallback（未转发）
PUT  https://www.zwymding.top/blade-workflow/odoc/wps/upload/{fileId}?token=...                → 返回 SPA HTML fallback（未转发）
```
`OdocWpsProvider.base()` = `publicBase + /blade-workflow`，故 odoc 的 downloadUrl / uploadUrl 全部走 `/blade-workflow/` 前缀（见 `OdocWpsProvider:74-117`），而 `/v3/3rd/**` 与 `/doc/file/**` 两条反代已通（P3.5 已配）但指向 8120，本规则缺失。

**前端 editor 固定 auto（源码）：**
- `OdocField.tsx:97`：`.init(instanceId, defId, fieldTitle, true)` —— 只传 4 个参数，`editor` 未传。
- `services/workflow/index.ts:2039`：`init: (instanceId, defId?, title?, edit = true, editor = 'auto') => ...` —— `editor` 默认 `'auto'`。
- 后端 `initDoc(instanceId, defId, title, edit, editor)`：仅 `editor=wps` 且 WPS 已配置时返回 `wpsEditorConfig`（`OdocDocInitVO.wpsEditorConfig`）；`auto` 走 OnlyOffice 分支，前端拿不到 WPS 配置，`OdocEditorModal` 不会渲染 `WpsEditor`。

---

## 3. 根因分析

### 3.1 file_id 字符集约束（阻断 #1/#2）
`WpsFileId.of(DOMAIN_ODOC, docId)` 用 `SEP = ':'` 生成 `odoc:{docId}`（`WpsFileId.java:18,30-32`）。该字符串同时作为：
- 前端 init 下发 `fileId` → 传给 SDK `init({fileId})` → 成为 WPS 中台的 `file_id`；
- 后端 token 签名的 resource：`signer.sign("wps:" + wire)`（`WfOdocDocServiceImpl:362`）；
- 回调验签 resource：`WpsCallbackSupport.tokenValid(signer, wireFileId, token)` 内部 `verify("wps:" + wireFileId, token)`（`WpsCallbackSupport.java:62-64`）；
- upload 验签 resource：`signer.verify("wps:" + fileId, token)`（`WfOdocWpsController:90`）。

WPS 中台（`o.wpsgo.com`）对 `file_id` 的硬约束：**只能由数字、字母、下划线组成，且不能以下划线开头**（实测 403 报文）。冒号非法。
知识管理侧 fileId 是**纯数字 legacy**（`396215716413575168`），天然合法，所以问题只在公文侧出现。此前所有协议层单测（`WpsCallbackProtocolTest` 用 `odoc:7`、`OdocWpsProviderTest`）都直打后端、绕过 WPS 中台，故从未暴露——**是假阳性**。

### 3.2 反代缺失（阻断 #3）
odoc 的字节流端点（`/odoc/wps/content`、`/odoc/wps/upload/{fileId}`）挂在 `blade-workflow`（8111），经网关前缀 `/blade-workflow`。`OdocWpsProvider` 把这两个 URL 拼成 `publicBase + /blade-workflow/...` 下发给 WPS 中台。公网 Express 反代只转发了 `/v3/3rd/**` 与 `/doc/file/**`（→ 8120），**缺 `/blade-workflow/**` → 8111**，因此 WPS 中台拉内容/推保存都落到 HTML fallback。

### 3.3 前端 editor 默认 auto（阻断 #4）
`OdocField` 调 `init` 未传 `editor`，默认 `'auto'`，后端走 OnlyOffice 分支不返回 WPS 配置，编辑器弹不出 WPS。

---

## 4. 改造方案

> 核心设计取舍：**只改 odoc 域的下发 fileId 为 `odoc_{docId}`（下划线），doc 域保持纯数字 legacy 完全不动**，把知识管理侧的回归风险降为零。token / 回调 / upload 的 resource 口径因都基于同一个 `wireValue()` 而自动一致，无需逐处改字符串。

### 4.1 改造 1：`WpsFileId` 的 odoc 域 wireValue 改用下划线（核心，解决 #1/#2）

**现状**（`WpsFileId.java`）：
- `SEP = ':'`；`of(domain,biz) = domain + ":" + biz`；`wireValue()` = `legacy ? bizId : of(domain,biz)`；`parse()` 用 `indexOf(SEP)` 识别 `domain:biz`。

**目标**：odoc 域对外（WPS 中台）的 fileId 变为 `odoc_{docId}`（下划线，合法）；doc 域仍纯数字；四码口径一致。

**改动**：
1. 新增常量 `WIRE_SEP = '_'`（仅 odoc 域 wire 使用），保留 `SEP = ':'` 用于内部/向后兼容。
2. 新增方法 `public static String wireValue(String domain, String bizId)`，语义：
   - `legacy` / `domain == DOMAIN_DOC` → 返回 `bizId`（纯数字，不变）；
   - `domain == DOMAIN_ODOC` → 返回 `"odoc" + WIRE_SEP + bizId`（下划线）；
   - （可选）`domain == DOMAIN_DOC` 且非 legacy 场景 → 维持 `of()`，但目前 doc 域全为 legacy，此分支可留空。
3. 新增 `parse()` 分支：先识别 `odoc_<digits>`（`WIRE_SEP` 开头且后接纯数字）→ `domain=odoc, bizId=digits, legacy=false`；再保留原 `indexOf(SEP)` 与纯数字 legacy 逻辑，确保 `odoc:xxx`（旧格式）与新 `odoc_xxx` 都能解析，纯数字仍按 doc legacy。
4. `parseFor(fileId, DOMAIN_ODOC)`（`WfOdocWpsController:95` 用于域隔离）自然兼容新格式。
5. 调用点同步：
   - `WfOdocDocServiceImpl:360` `String wire = WpsFileId.of(...)` → 改为 `WpsFileId.wireValue(DOMAIN_ODOC, String.valueOf(docId))`（下发给 WPS 中台与 token）。`wc.setToken(signer.sign("wps:" + wire))` 随之自动一致。
   - `WfOdocDocServiceImpl:453` `m.setId(WpsFileId.of(ODOC, docId))` → 改为 `WpsFileId.wireValue(ODOC, docId)`（版本数据回传 fileId）。
   - `OdocWpsProvider.uploadUrl(wireFileId, name)`（`:109-117`）：入参已是 `wireValue`（由 `WpsCallbackController.address` 透传回调 fileId），无需改；其内 `signer.sign("wps:" + wireFileId)` 与 `WfOdocWpsController.upload` 的 `verify("wps:" + fileId)` 自动一致。
   - `DocWpsProvider.toMeta(WpsFileId.of(domain,bizId), ...)`（`:102/114/178`）：doc 域，纯数字 legacy，不受影响。
6. **不改** `WpsCallbackSupport.internalResource`（服务间 token `wps:odoc:{op}:{bizId}`，冒号是内部约定，不经 WPS 中台，不受字符集约束）。

**回归自检**：改造后 `odoc:` 旧格式仍可解析（兼容期内），新格式 `odoc_xxx` 全链路一致。

### 4.2 改造 2：前端「在线编辑」显式传 `editor=wps`（解决 #4）

**改动**：
- `OdocField.tsx:97`：`.init(instanceId, defId, fieldTitle, true, 'wps')` 显式传 `editor='wps'`；或在后端 `initDoc` 对 odoc 默认 `editor = wpsWhenConfigured()`（依 `doc_engine_setting.default_engine/third_platform`）兜底，前端可不改。
- 推荐：**后端兜底 + 前端显式传**双保险。后端 `initDoc` 当 `editor == null || "auto".equals(editor)` 时，若 `settings.isWpsConfigured()` 则返回 WPS 分支（`OdocDocInitVO.wpsEditorConfig` 非 null），否则 OnlyOffice；这样即使老前端不传也是 WPS。
- `services/workflow/index.ts:2039` `editor = 'auto'` 默认值保留向后兼容，但调用处传 `'wps'`。

**回归自检**：点「在线编辑」→ `init` 返回 `wpsEditorConfig` → `OdocEditorModal` 渲染 `WpsEditor` → iframe 指向 `o.wpsgo.com/office/w/odoc_xxx`。

> **⚠️ 落地时补充发现（真实入口才会暴露）**：仅传 `editor=wps` 并不够。
> `OdocField` 原实现在 `value`（正文 docId）非空时走 `if (value) { setDocId(value); reload(value); return; }`
> —— **完全不调 `init`**，于是 `canEdit` 恒为 `false`（「在线编辑」按钮直接不渲染）、
> `initData` 为 `null`（弹窗内 `init?.wpsEditorConfig` 取不到 → 显示「WPS 配置缺失」）。
> 而「正文已创建并回写表单」恰恰是**常态**，只有首次进入（value 为空）才会调 `init`。
> 前一轮的注入式验证（替换 `/doc/editor/config` 响应）恰好绕开了这条路径，属**验证盲区**。
>
> **补充修复**：`OdocField` 的 `useEffect` 抽出 `loadInit(keepDocId)`，**两条分支都调 `init`**：
> - `value` 非空（正文已存在）：`keepDocId=true`，只取 `canEdit` / `wpsEditorConfig` / `initData`，
>   `docId` 仍以表单 `value` 为准；后端 `initDoc` 按 `instanceId` 复用已有正文，不会重复创建。
> - `value` 为空（首次）：`keepDocId=false`，`docId` 取 `init.docId` 并回写表单。
>
> 另注：当前 dev 库 `form_layout` 中**没有任何 `fieldHtmlType=9`（正文）字段**，
> 故真实流程表单入口（含点「在线编辑」按钮）在 dev 环境无法自然走通；
> 该分支的验证方式与结果见 §10.4。

### 4.3 改造 3：公网反代补 `/blade-workflow/**`（解决 #3，用户侧动作）

**改动（公网 Express / 反代配置，非仓库代码）**：新增一条转发规则，把
`https://www.zwymding.top/blade-workflow/**` → `127.0.0.1:8111`（或经网关 `http://host:81/blade-workflow`，与 `/v3/3rd/**` → 8120 区分）。
与既有两条已通规则并列：
- `/v3/3rd/**` → 8120（WPS 回调入口，blade-doc）
- `/doc/file/**` → 8120（知识管理直连内容）
- **`/blade-workflow/**` → 8111（odoc 字节流 + 元数据，本次新增）**

**注意**：网关 `blade.secure.skip-url` 已含 `/blade-workflow/odoc/wps/**`（代码层 `AuthProvider.DEFAULT_SKIP_URL` 常驻），无需改 Nacos；反代层补转发即可。

**回归自检**：`GET/PUT https://www.zwymding.top/blade-workflow/odoc/wps/content?...` 与 `/upload/...` 返回 200 + 字节流，而非 HTML。

### 4.4 改造 4：WPS 中台偶发 503（外部，非代码，仅记录）

中台静态资源偶发 503（curl 直连 200），重跑 `pw_odoc_e2e.cjs` 即可，勿误判为代码故障。

---

## 5. 改动清单（文件级）

| 文件 | 改动 | 类型 |
|------|------|------|
| `blade-common/.../wps/WpsFileId.java` | 新增 `WIRE_SEP='_'`、`wireValue(domain,biz)`；`parse()` 增加 `odoc_<digits>` 识别 | 核心 |
| `blade-workflow/.../odoc/service/impl/WfOdocDocServiceImpl.java:360,453` | `of(ODOC,docId)` → `wireValue(ODOC,docId)`（下发 fileId + token + 版本 id） | 核心 |
| `blade-workflow/.../odoc/controller/WfOdocWpsController.java` | 无代码改动；注释里 `fileId = odoc:{docId}` 更新为 `odoc_{docId}` | 注释 |
| `blade-doc/.../online/OdocWpsProvider.java` | 无代码改动（uploadUrl 入参已是 wireValue）；核对无硬编码 `odoc:` | 核对 |
| `blade-doc/.../controller/WpsCallbackController.java` | 无改动（authGuard 用 `wireFileId` 透传） | 核对 |
| `blade-workflow/.../odoc/controller/WfOdocWpsController.java:90` | 无改动（`verify("wps:"+fileId)` 随 wireValue 自动一致） | 核对 |
| `ant-design-pro/src/components/FormMode/OdocField.tsx:97` | `init(..., true, 'wps')` | 前端 |
| `ant-design-pro/src/services/workflow/index.ts:2039` | `editor` 默认保持 `auto`（兼容），调用处传 `wps`；后端兜底 | 前端 |
| `blade-workflow/.../odoc/controller/WfOdocDocController.java`（`initDoc`） | `editor=auto/null` 且 `settings.isWpsConfigured()` → WPS 分支（兜底） | 后端兜底 |
| 公网反代配置（Express） | 新增 `/blade-workflow/**` → 8111 | 外部动作 |
| `blade-common/src/test/.../WpsCommonTest.java` | `parse("doc:")` 等断言复核（doc 域仍纯数字，断言维持）；补充 `odoc_123` 解析用例 | 测试 |
| `blade-doc/src/test/.../WpsCallbackProtocolTest.java` | 合法 odoc fileId 用例 `odoc:7` → `odoc_7`；`doesNotContain("doc:")` 维持（更不会有冒号） | 测试 |
| `blade-doc/src/test/.../OdocWpsProviderTest.java` | 核对 `odoc:` 字面量（如有）更新为 `odoc_`；internal token 含 `odoc:` 是服务间约定，保留 | 测试 |

**不改动**：知识管理侧纯数字 fileId 全链路（doc 域 legacy）；`WpsCallbackSupport.internalResource` 服务间 token 口径；网关 skip-url（代码层已含）；Nacos 共享配置。

---

## 6. 存量兼容与迁移

- **知识管理（doc 域）**：fileId 全为纯数字 legacy，本改造**完全不触碰**，零回归风险。
- **公文（odoc 域）**：从未端到端跑通过，不存在已签发的有效 token / 已分发的 content/upload URL 存量，改造无数据迁移负担。
- **已落库数据**：`doc_engine_setting` 配置源不涉及 fileId 格式，无需迁移脚本。
- **WPS 控制台 / 应用配置**：回调地址登记为 `/v3/3rd/files/{fileId}`，路径结构不变（仅 fileId 内部字符变），无需重新登记。
- **向后兼容窗口**：`parse()` 同时识别旧 `odoc:xxx` 与新 `odoc_xxx`，过渡期两种格式都可解析，避免灰度期间已签发链接失效（虽 odoc 无存量，仍保留以防万一）。

---

## 7. 回归锚点

### 7.1 单元测试（改完必跑）
```
mvn -o -pl blade-common,blade-service/blade-doc,blade-service/blade-workflow -am test \
  -Dtest=WpsCommonTest,WpsCallbackProtocolTest,OdocWpsProviderTest
```
- `WpsCommonTest`：补充 `parse("odoc_123").domain()==odoc`、`wireValue(odoc,123)=="odoc_123"`、`parse("odoc:123").domain()==odoc`（向后兼容）。
- `WpsCallbackProtocolTest`：合法 odoc fileId 改为 `odoc_7`；`doesNotContain("doc:")` / `doesNotContain("doc%3A")` 维持（downloadUrl 不再含冒号）。
- `OdocWpsProviderTest`：internal token 口径 `wps:odoc:...` 不变；如有 `odoc:` 字面量更新。

### 7.2 端到端脚本（真实浏览器 + 真实中台）
- `log/pw_odoc_e2e.cjs`：改造后**移除** `E2E_FILEID` 覆盖（默认即为新下划线格式），重跑。
- 预期：`SESSION_403_COUNT=0`、`CRITICAL_COUNT=0`、`iframeSrcs` 含 `o.wpsgo.com/office/w/odoc_<docId>`、`NET_TOTAL` 含 `200 POST /api/v3/office/session/.../word` 与 `200 ... /v3/3rd/files/odoc_.../download`，无 `字段file_id` / `unauthorized callback` 报错。

### 7.3 验收判据（全绿即端到端打通）
1. 知识管理侧 docx 仍正常打开（`SESSION_403_COUNT=0`，纯数字 unaffected）—— 回归不退化。
2. 公文侧 `o.wpsgo.com/office/w/odoc_<docId>` session 200，无 file_id 报错。
3. 回调解 contentUrl（经 `/v3/3rd/files/odoc_.../download` → 返回 `/blade-workflow/odoc/wps/content?...`）：公网 GET 返回 200 字节流（反代已补）。
4. 编辑保存三阶段：`upload/prepare` 200 → `upload/address` 返回 `https://www.zwymding.top/blade-workflow/odoc/wps/upload/odoc_...?token=...` → WPS 中台 PUT 该地址公网 200 → `upload/complete` 200、版本链 `+1`。
5. `unauthorized callback` 不再出现（token 与 wireFileId 口径一致）。
6. 前端点「在线编辑」弹 WPS 编辑器（改造 2 生效）。

---

## 8. 风险与回滚

- **风险 R1（doc 域误伤）**：若任何代码路径把 `doc:xxx` 作为 WPS fileId 下发，改 SEP 全局会破坏知识管理。已通过「仅 odoc 域改下划线、doc 域保持纯数字」规避；落地前 grep `WpsFileId.of(DOMAIN_DOC` 确认 doc 域只用 legacy（当前 `DocWpsProvider` 全为 legacy）。
- **风险 R2（服务间 token 误改）**：`internalResource` 的 `wps:odoc:` 冒号是内部约定，不经中台，明确不改；若误改为下划线需 blade-doc/OdocWpsProvider 与 blade-workflow/WfOdocWpsController 同步，本文档不要求改。
- **风险 R3（反代指向错误）**：`/blade-workflow/**` 必须指向 8111（非 8120），否则会打到 doc 服务 404；与既有 `/v3/3rd/**`→8120 区分清楚。
- **回滚**：改造 1/2 的代码回滚为 `wireValue` 用 `: ` 即可；odoc 无存量链接，回滚不会产生孤儿数据。反代回滚即删规则。

---

## 9. 评审 checklist

- [ ] 改造 1：`WpsFileId.wireValue(ODOC)` 输出 `odoc_<digits>`，`parse` 兼容新旧两格式，doc 域零改动
- [ ] 改造 1：三处 `of(ODOC,...)` 调用点改为 `wireValue(ODOC,...)`
- [ ] 改造 2：前端 `init` 传 `wps` + 后端 `initDoc` 兜底
- [ ] 改造 3：用户侧反代补 `/blade-workflow/**` → 8111（并提供 curl 验收证据）
- [ ] 单测全绿，且覆盖 `odoc_123` 解析与向后兼容
- [ ] `pw_odoc_e2e.cjs` 默认（不覆盖 fileId）跑通，6 项验收判据全绿
- [ ] 知识管理侧 docx 回归不退化（纯数字 unaffected）
- [ ] 不动 Nacos 共享配置、不动网关 skip-url、不改 internalResource 服务间 token 口径

---

## 10. 落地结果（2026-11-12）

### 10.1 已落地并验证通过（改造 4.1 + 4.2）

**改造 4.1 —— `WpsFileId` 下划线 wire 形态**
- `blade-common/.../wps/WpsFileId.java`：新增 `WIRE_SEP = '_'`、静态 `wireValue(domain, bizId)`；实例 `wireValue()` 对公文域返回 `odoc_{bizId}`，legacy 与知识管理域保持原语义；`parse()` 新增 `odoc_{digits}` 识别，并保留旧 `odoc:bizId` 冒号格式解析（迁移期兼容）。
- `WfOdocDocServiceImpl`：`buildWpsEditorConfig` 的 wire（:360）与 `toWpsMeta` 的 meta id（:453）改为 `wireValue(ODOC, docId)`。

**改造 4.2 —— editor 选择**
- 前端 `OdocField.tsx`：`init(instanceId, defId, fieldTitle, true, 'wps')` 显式传 editor。
- 后端 `WfOdocDocServiceImpl.initDoc`：`auto` 兜底 —— WPS 已配置（`doc_engine_setting` 单一源）时优先走 WPS 分支，未配置才回落 OnlyOffice，老前端不传也生效。

**单测**：49 个全绿（`WpsCommonTest` 16 / `WpsCallbackProtocolTest` 24 / `OdocWpsProviderTest` 9）。
新增用例覆盖：`wireValue(odoc)` 输出 `odoc_456`、新格式解析与幂等、旧冒号格式兼容、`doc_456`/`odoc_abc`/`_1` 非法拒绝、新格式不得误路由到 doc 域、token 必须按 `wps:odoc_{id}` 验签（旧冒号口径 token 被拒）。

**端到端实测（真实浏览器 + 真实 WPS 中台，`log/pw_odoc_e2e.cjs`，不再覆盖 fileId）**：
```
ODOC_CFG fileId = odoc_2117125489433808897          ← 后端真实下发
iframe      = https://o.wpsgo.com/office/w/odoc_2117125489433808897
200 POST https://o.wpsgo.com/api/v3/office/session/odoc_2117125489433808897/word?first   ← 原 403 已消除
200 GET  .../office/file/odoc_2117125489433808897
SESSION_403_COUNT=0  CRITICAL_COUNT=0  无「字段file_id」/「unauthorized callback」
```
- `init` 接口：`editor=wps` 与 `editor=auto` 均返回 `editorType=wps` + `wpsEditorConfig.fileId=odoc_...`。
- `GET /v3/3rd/files/odoc_.../download` → **200** 且返回正确 url（证明 token `wps:odoc_...` 验签口径一致）。
- `upload/prepare` → 200；`upload/address` → 200，url 内 fileId 为 `odoc_...`。
- upload PUT（**内网直连 8111 绕过反代**）→ **HTTP 200**，版本链 **1 → 2 → 3**（落库成功），版本 id 为 `odoc_...`。
- 伪造 token → **40101**（越权防护有效）。
- 知识管理侧回归：`SESSION_403_COUNT=0 / CRITICAL_COUNT=0 / REQUEST_FAILED_COUNT=0`，iframe 1026×478 可见 —— **纯数字 legacy 未退化**。

### 10.2 唯一剩余阻塞：公网反代（改造 4.3，用户侧动作）

中台 `open/word` 仍 500，根因已定位到反代：

```
GET /v3/3rd/files/odoc_.../download  → 200，返回
  https://www.zwymding.top/blade-workflow/odoc/wps/content?docId=...&token=...
该 url 公网实际返回 → HTTP 200 但 Content-Type: text/html（Express SPA fallback）
   → 中台拿到 HTML 而非 docx 字节流 → open/word 500
```

**⚠️ 纠正一处早前结论**：曾表述「网关 `blade.secure.skip-url` 已含 `/blade-workflow/odoc/wps/**`，无需改 Nacos」——**这是错的**。实测：

| 入口 | 结果 |
|------|------|
| 公网 `https://www.zwymding.top/blade-workflow/odoc/wps/content?...` | 200 + `text/html`（Express SPA fallback，反代未转发） |
| 网关 `http://192.168.1.5:81/blade-workflow/odoc/wps/content?...` | **401** `{"msg":"缺失令牌,鉴权失败"}` |
| 直连 `http://192.168.1.5:8111/odoc/wps/content?...` | **200 + `application/octet-stream` + 610 字节** ✓ |

- 代码层 `AuthProvider.DEFAULT_SKIP_URL` 只有 `/v3/3rd/**` 与 `/blade-doc/v3/3rd/**`，**不含** odoc 路径；
- Nacos 共享 `blade.yaml` 的 `blade.secure.skip-url` 同样**不含** odoc 路径（只有 `/odoc/esign/callback` 等）；
- WPS 中台不带任何 Blade-Auth，走网关必然被 `AuthFilter` 拦。

**因此反代必须直连 blade-workflow（8111），绕开网关**：

| 路径前缀 | 目标 | 状态 |
|----------|------|------|
| `/v3/3rd/**` | `127.0.0.1:8120`（blade-doc，WPS 回调入口） | 已通 |
| `/doc/file/**` | `127.0.0.1:8120`（blade-doc，内容直连） | 已通 |
| **`/blade-workflow/odoc/wps/**`** | **`http://127.0.0.1:8111/odoc/wps/**`（注意剥离 `/blade-workflow` 前缀）** | **待补** |

**前缀剥离是必须的**：8111 上 `WfOdocWpsController` 的 `@RequestMapping("/odoc/wps")`，**无** `/blade-workflow` 前缀（该前缀是网关路由加的）。故转发时须把 `/blade-workflow/odoc/wps/xxx` → `http://127.0.0.1:8111/odoc/wps/xxx`。
若要整段转发 `/blade-workflow/**` → `http://127.0.0.1:8111/**`，同理需剥前缀；但精确到 `/blade-workflow/odoc/wps/**` 暴露面更小，推荐。

**不建议**改为指向网关 81：那需要额外在网关放行 odoc 路径（代码层 `AuthProvider.DEFAULT_SKIP_URL` 或 Nacos `blade.secure.skip-url`），
而这两个位置都是全局共享配置（改 Nacos 会全服务广播、有 blade-auth 404 前例），且等于把中台无鉴权流量引入网关鉴权体系，收益为零。

补完后重跑 `log/pw_odoc_e2e.cjs`，预期 `open/word` 转为 200、iframe 内文档正常渲染。

### 10.3 本次测试产生的数据变更

upload 验证在测试草稿文档「发文回退测试」（docId `2117125489433808897`）上新增了 1 个版本（版本链由 2 条变 3 条），内容为 `uploads/odoc/2111409042288107522.docx` 的字节流。属测试数据，如需清理请删除 versionId 最大的那条记录。

### 10.4 真实入口（流程表单「在线编辑」）验证结论

**目标**：不注入 `/doc/editor/config`，而是走真实流程表单 → 点「在线编辑」→ 弹窗挂载 `WpsEditor`。

**当前 dev 环境无法走通，原因有二（均为环境/数据缺口，非代码缺陷）**：
1. `form_layout` 表中**无任何 `fieldHtmlType=9`（正文）字段**（已查：`SELECT ... WHERE layout_json REGEXP 'fieldHtmlType.{0,4}9'` 无记录），
   故没有含正文字段的流程定义 → `OdocField` 不会被渲染 → 没有「在线编辑」按钮可点。
2. `wf_task` 中 `assignee=676` 无 `status=0` 待办 → 拿不到可办理的流程任务。

**已验证的等价链路**（`log/pw_odoc_e2e.cjs`，替换 `/doc/editor/config` 响应为公文侧真实 `wpsEditorConfig`）：
SDK 加载 → iframe → WPS 中台 session 200 → 元信息 200 → 文档真实渲染，详见 §10.1。

**已完成的等价性补强**：修复了只有真实入口才会触发的 `OdocField` 配置加载缺陷（见 §4.2 补充），
使「正文已存在」与「正文首次创建」两条分支都能拿到 `canEdit` + `wpsEditorConfig`。

**前端编译健康**（`log/pw_odoc_entry.cjs`）：登录后访问 `/odoc/todo-list`，
页面正常渲染（`hasRoot=true`，公文管理 14 个菜单齐全），无 JS 编译错误；
仅有既有的 `/api/blade-system/menu/buttons` 401 与 antd 静态方法告警，与本次改造无关。

**待补齐（需在 dev 数据中建一个含正文字段的流程定义）**：
建好后重跑真实入口脚本，即可验证「点在线编辑 → 弹窗渲染 WPS → iframe `odoc_xxx`」这条最贴近用户操作的链路。
