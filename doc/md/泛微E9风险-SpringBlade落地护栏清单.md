# 泛微 E9 风险 → SpringBlade 落地护栏清单

> 桥梁文档：把 12 份《泛微E9-*》分析文档的 **81 条风险**翻译成**可执行的护栏**——每条 = E9 的坑 + SpringBlade 的正确写法（指向你们工程里**已存在**的机制）+ ant-design-pro 对应。
> 用途：**功能落地的 Definition-of-Done 门禁**。E9 的坑在 SpringBlade 里大多「结构性不可发生」，仅少数需要主动护栏；本清单只列需要主动防的。
> 源码根：`D:\project\springbladeandreact\SpringBlade`（后端）、`D:\project\springbladeandreact\ant-design-pro`（前端）。

---

## 1. 机制映射总表（E9 原语 → 你们已有原语）

| E9 缺陷根因 | SpringBlade 正确原语（已存在） | ant-design-pro 对应 |
|---|---|---|
| 每模块自实现分权开关（`isUseXxxManageDetach`） | **`DataScope`**（`org.springblade.core.datascope`，`DataScopeModelHandler`/`DataScopeCache`/`ScopeConfiguration`）+ **多租户**（`blade-core-tenant`、`Tenant` 实体） | 数据范围由后端注入，前端只读过滤结果，不自行过滤 |
| 权限被注释/漏校验（`HrmUserVarify` 散落调用） | **`@PreAuth`**（声明式，集中拦截，无法被「注释掉」） | 路由守卫 `access` + `useAccess()` 服务端下发权限集 |
| SQL 字符串拼接注入 | **`QueryWrapper`/`LambdaQueryWrapper`**（MyBatis-Plus，参数化） | — |
| 写操作滥用 GET | Spring MVC **POST/PUT/DELETE** + 表单/网关 CSRF | 统一请求层：`src/request.ts` 变更必 POST，token 走 `Authorization` header |
| 内部 Feign 漏鉴权 | **`/feign/client` 前缀 + 网关 `InnerFilter` 拒绝经网关访问**（`NoticeClient`/`MessageClient` 约定） | — |

> 关键：`DataScope` 框架 = 我《泛微E9-分权统一收口方案》里设计稿的**现成实现**。直接统一采纳，不要再手写分部/租户过滤。

---

## 2. 护栏清单（Forbidden Patterns）

每条格式：`[风险ID] E9 坑 → ❌禁用写法 → ✅SpringBlade 正确写法 → 🔷前端`。

### 2.1 数据范围 / 分权（FNA-N1 / DET-P0-2 / CRM-N1 / CUBE-N1）
- **[FNA-N1]** E9 财务用 `isUseWfManageDetach`（workflow 开关）门控 → ❌模块里硬编码 `isUseXxxManageDetach` 做分支 → ✅所有分部/租户隔离统一走 `@DataScope`（按 mapperId/code 在 `DataScopeCache` 配置），开关语义集中在 DataScope 配置，不在业务代码。
- **[DET-P0-2]** E9 `DetachHelper` 缓存 key 漏 `rightStr` 致跨权限串泄漏 → ❌任何「用户+维度」缓存 key 不含完整权限维度 → ✅缓存 key 必须含 `uid+roleId+resourceCode`（参考 `DataScopeCache.getDataScopeByMapper` 已含 `mapperId+roleId`；自加维度时一并进 key）。
- **[CUBE-N1]** E9 `GetAppListCmd` 拼接 `subCompanyId` → ❌`" and subCompanyId ="+id` → ✅`QueryWrapper.eq("subCompanyId", id)` 或 DataScope 自动追加 `IN(?)`。
- **[CRM-N1]** E9 CRM 用自有 `CRM_ShareInfo` 共享，漏接即泄露 → ❌列表查询不接共享/数据范围视图 → ✅CRM 类记录级共享统一用 `@DataScope(code="crm:customer")` 或 `@DataScope(mapperId=...)` 强制注入，缺失注解的读端点编译期/启动期拦截。

### 2.2 权限校验（DET-P0-1/2/3/4、DOC-P0-3/4、ODOC-P0-1/2、WF-N9）
- **[DET-P0-1/2/3/4]** E9 批量「注释掉权限判断」→ ❌控制器/Service 里 `// if(!hasRight)...` 或整段删除校验 → ✅端点一律 `@PreAuth(...)` 声明；缺注解即视为未授权（默认 deny）。内部 Feign 走 `/feign/client` + 网关隔离，不加 `@PreAuth`（见 `NoticeClient` 注释约定）。
- **[WF-N9]** E9「条件门禁 ≠ 权限」→ ❌用流程 link 上的 `condition`/`newrule` 当权限拦截 → ✅节点提交权限独立用 `@PreAuth`；数据与「能否读这一条」收口到业务归属（**正面样板：`WfInstanceServiceImpl.java:2304-2306` 已把读权限收口到流程参与人**）。
- **[ODOC-P0-1/2]** E9 公文操作漏权 → ❌公文删除/分发仅前端隐藏按钮 → ✅后端 `@PreAuth` + 资源归属校验（见 2.5）。

### 2.3 SQL / 注入（DOC-P0-2/5、WF-P0-1/2、ODOC-P0-3 类）
- ❌`"select ... where name='"+name+"'"`、`${}` 拼接 → ✅`QueryWrapper.like("name", name)` / `#{}` 参数化；动态 SQL 只用 MyBatis `<if>` + `#{}`。
- 批量 ID 入参 → ✅`QueryWrapper.in("id", idList)`（idList 为 `List<Long>`，类型绑定防注入）。

### 2.4 写操作 GET（G1）
- ❌`@GetMapping` 做删除/状态变更、token 或敏感参数进 URL → ✅`@PostMapping/@PutMapping/@DeleteMapping` + CSRF；`ant-design-pro` 侧 token 走 `Authorization` header（`src/request.ts` 统一注入），不拼 URL。

### 2.5 越权 / 参数信任（DOC-P0-4 枚举下载、ODOC-P0-1 越权删除）
- **[DOC-P0-4]** E9 `imagefileid` 顺序枚举下载 → ❌`@PreAuth("permitAll()") @GetMapping("/file/{fileId}/content")` 且 fileId 为可猜测顺序值 → ✅文件下载端点必须 `@PreAuth` + **按当前用户资源归属校验该 fileId 是否可读**（或用不可猜测的 token/签名 URL，限时有效）；`fileId` 不暴露业务顺序。
- **[ODOC-P0-1]** 越权删除他人公文 → ❌仅校验「是否登录」→ ✅删除/修改前查资源 owner/参与人，非归属拒绝。

### 2.6 架构 / 调试残留（G5/G6、WF-N4/5/6）
- ❌`System.out.println` / `e.printStackTrace()` / 单文件数千行 → ✅统一 `log.xxx`；依赖 SpringBlade 已拆分的 `blade-*` 微服务边界；规则/流程求值异常 **fail-closed 并记录告警**（参考 E9 `RuleInterface` 异常静默返回 false 的教训，你们应 fail-closed + 日志 + 可观测面板）。

---

## 3. 示范模块落地：doc 内容引擎「文件下载/权限」

对应 E9 最密 P0 区（DOC-P0-2/3/4/5）。在 SpringBlade 上的正确骨架：

```java
// blade-workflow / odoc 文件下载端点（修正 DOC-P0-4 形态）
@RestController
@RequiredArgsConstructor
public class WfOdocFileController {
    private final IResourceService resourceService;

    @GetMapping("/file/{fileId}/content")
    @PreAuth("hasRole('odoc_reader')")              // 1. 至少角色门禁，禁止 permitAll
    public void fileContent(@PathVariable Long fileId, BladeUser user, HttpServletResponse resp) {
        // 2. 资源归属校验：该 fileId 是否当前用户可读（发布范围/参与人/授权）
        if (!resourceService.canRead(fileId, user)) {
            throw new ServiceException(ResultCode.REQ_REJECT);   // fail-closed
        }
        // 3. fileId 不暴露业务顺序；如需外链用签名 URL（限时、不可猜）
        resourceService.writeContent(fileId, resp);
    }
}
```
> ⚠️ **优先复核**：你们 `WfOdocController.java:232` 现为 `@PreAuth("permitAll()") @GetMapping("/file/{fileId}/content")`。若确为「已发布公文公开」，请加注释说明并保留不可猜测/限时机制；若非，按上面骨架补归属校验——这是 E9 DOC-P0-4 在你们工程里的**同款风险点**。

---

## 4. SpringBlade 自身落地风险（优先复核项）

| 位置 | 现象 | 对应 E9 类 | 建议 |
|---|---|---|---|
| `WfOdocController.java:232` | `@PreAuth("permitAll()") @GetMapping("/file/{fileId}/content")` | DOC-P0-4 枚举下载 | 补资源归属校验或确认公开并加限时/不可猜机制 |
| `WfInstanceServiceImpl.java:2304-2306` | 已正确把读权限收口到参与人 | （正面样板） | 保持，作为其他模块参照 |
| 内部 Feign（`NoticeClient`/`MessageClient`） | 无 `@PreAuth`，靠 `/feign/client`+网关隔离 | DET-P0-3 内部漏权 | 保持约定，CI 校验所有 `/feign/client` 不经网关可达 |

---

## 5. CI / 验证门禁（让护栏自动生效）

1. **静态扫描（正则，接入 CI）**：
   - SQL 拼接：`\+\s*["'].*where.*["']\s*\+` 或 `${` 非参数化 → 阻断。
   - GET 写操作：`@GetMapping` 且方法名含 `delete|remove|update|save|submit` → 告警。
   - 缺 `@PreAuth`：`@(Get|Post|Put|Delete)Mapping` 的公开 Controller 方法无 `@PreAuth`/`@PermitAll` 显式声明 → 阻断（白名单内部 Feign）。
2. **单测矩阵**：E9 的「开关×角色×模块」矩阵 → 映射为 DataScope 配置测试（不同 `roleId`+`mapperId` 下断言注入的 `IN(...)` 范围正确）。
3. **DoD 勾选**：每个功能 PR 必须附「护栏清单 2.1–2.6 已逐项确认」勾选。

---

## 6. 与既有「落地」文档的衔接

你们已有：`收发文落地-*`、`表单建模功能分析报告`、`泛微E9-矩阵管理功能分析及SpringBlade落地建议`、`用户创建功能改造计划-对齐ecology架构` 等。本清单是其**公共安全基线**——各模块落地文档在「权限/分权/SQL」小节直接引用本清单条目即可，避免重复且保持一致。
