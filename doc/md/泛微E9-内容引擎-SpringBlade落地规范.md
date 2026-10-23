# 泛微 E9 内容引擎（DocEngine）→ SpringBlade 落地规范

> 把《泛微E9-内容引擎DocEngine架构与运作机制深度分析.md》的 E9 实现，按《泛微E9风险-SpringBlade落地护栏清单》正确落到 SpringBlade + ant-design-pro。覆盖 **DataScope 配置 + 实体 + 接口 + 前端权限**。
> 目录建议：`blade-service/blade-doc`（后端）、`ant-design-pro/src/pages/DocEngine`（前端）。

---

## 1. 表 / 实体映射（E9 → SpringBlade MyBatis-Plus）

| E9 表 | SpringBlade 实体（`org.springblade.doc.entity`） | 关键字段 |
|---|---|---|
| `DocDetail` | `DocDetail` | `id, secCategoryId, docStatus, docEdition, createUser, tenantId, subCompanyId, isTop, isValid` |
| `DocSecCategory` | `DocSecCategory` | `id, name, parentId, manageRoleId, isOpenWater` |
| `DocImageFile` | `DocImageFile` | `id, docId, versionId, fileId, fileName, fileSize` |
| `DocDetailContent` | `DocDetailContent` | `docId, content` |
| `DocShare` / `shareinnerdoc` | `DocShare` | `id, docId, shareType, targetId, shareLevel` |
| `DocDummyDetail` | `DocDummyDetail` | `catalogId, docId`（虚拟目录挂载） |
| `DocDetailLog` / `DownloadLog` | `DocDetailLog` / `DownloadLog` | `docId, operateType(0读/1增/2改/13发布/22下载), operateUser` |
| `recycle_docdetail` | `DocRecycle` | `docId, deleteUser, deleteTime` |

> 所有实体继承 `TenantEntity`（自带 `tenantId`），分部隔离靠 **DataScope**（见 §2），**禁止**在业务代码里手写 `tenant_id`/`sub_company_id` 过滤。

---

## 2. DataScope 配置（核心：杜绝 FNA-N1 / DET-P0-2 / CUBE-N1 类缺陷）

SpringBlade 的 `DataScope` 框架（`DataScopeModelHandler`/`DataScopeCache`）按 `mapperId` 或 `code` 自动追加 `AND <field> IN (?)`。配置两处：

**① 库表 `blade_data_scope`（或 `@DataScope` 注解）**：
```sql
INSERT INTO blade_data_scope(mapper_id, field, scope_class, role_id)
VALUES ('org.springblade.doc.mapper.DocDetailMapper.selectPage', 'sub_company_id', 1, 0);  -- 1=自定义/按角色分部
```
**② Mapper 标记**（blade-core 的 `@DataScope`）：
```java
@DataScope(mapperId = "org.springblade.doc.mapper.DocDetailMapper.selectPage")
List<DocDetail> selectPage(IPage<DocDetail> page, @Param("ew") QueryWrapper<DocDetail> ew);
```
→ 框架依据 `DataScopeCache.getDataScopeByMapper(mapperId, roleId)` 注入当前用户可见分部，`getDeptAncestors` 处理上下级。开关语义集中在配置，**业务代码零硬编码**（对照 E9 `isUseWfManageDetach` 散落各端点）。

---

## 3. 接口与权限（逐个端点，对应 DOC-P0-2/3/4/5）

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/doc/detail")
public class DocDetailController {
    private final IDocDetailService docService;

    // 列表：DataScope 自动按分部过滤 + 角色门禁
    @GetMapping("/list")
    @PreAuth("hasPermission('doc:view')")
    public R<IPage<DocDetailVO>> list(Query query, BladeUser user) {
        return R.data(docService.page(listQuery(query, user)));   // 内部用 QueryWrapper
    }

    // 详情：DataScope + 文档级归属（DocViewPermission 等价物）
    @GetMapping("/{id}")
    @PreAuth("hasPermission('doc:view')")
    public R<DocDetailVO> detail(@PathVariable Long id, BladeUser user) {
        if (!docService.canRead(id, user)) throw new ServiceException(ResultCode.REQ_REJECT);  // fail-closed
        return R.data(docService.getDetail(id));
    }

    // 保存/提交：编辑权 + QueryWrapper 参数化（修 DOC-P0-2 拼接）
    @PostMapping("/save")
    @PreAuth("hasPermission('doc:edit')")
    public R<Boolean> save(@RequestBody @Valid DocDetailDTO dto, BladeUser user) {
        return R.status(docService.saveDoc(dto, user));   // 内部 QueryWrapper.eq / .in，绝不字符串拼接
    }

    // 发布/失效/置顶：后台维护权限点（E9 DocManageSet:all）
    @PostMapping("/publish")
    @PreAuth("hasPermission('doc:manage')")
    public R<Boolean> publish(@RequestBody DocOpDTO dto, BladeUser user) { ... }

    // 删除：归属校验（修 ODOC-P0-1 越权删除）
    @DeleteMapping("/{id}")
    @PreAuth("hasPermission('doc:delete')")
    public R<Boolean> delete(@PathVariable Long id, BladeUser user) {
        if (!docService.canManage(id, user)) throw new ServiceException(ResultCode.REQ_REJECT);
        return R.status(docService.logicDelete(id, user));
    }

    // 下载：【修正 WfOdocController.java:232 的 permitAll 同款洞】必须归属校验
    @GetMapping("/file/{fileId}/content")
    @PreAuth("hasPermission('doc:download')")
    public void fileContent(@PathVariable Long fileId, BladeUser user, HttpServletResponse resp) {
        if (!docService.canDownload(fileId, user)) throw new ServiceException(ResultCode.REQ_REJECT);
        docService.writeFile(fileId, resp);   // 如需外链用限时签名 URL，fileId 不暴露业务顺序
    }
}
```
> `canRead/canManage/canDownload` 统一查 `DocShare` + `DocSecCategory.manageRoleId` + 创建人，等价于 E9 `DocViewPermission`（`docid` 入参判定），但**声明式 `@PreAuth` + 归属双重把关**，杜绝「注释掉权限」(DOC-P0-3) 与「条件≠权限」(WF-N9)。

---

## 4. 前端权限（ant-design-pro）

### 4.1 权限集（对齐服务端）
`src/access.ts`：
```ts
export default function access(initialState) {
  const { permissions } = initialState ?? {};
  return {
    'doc:view': permissions.includes('doc:view'),
    'doc:edit': permissions.includes('doc:edit'),
    'doc:publish': permissions.includes('doc:publish'),
    'doc:download': permissions.includes('doc:download'),
    'doc:delete': permissions.includes('doc:delete'),
    'doc:manage': permissions.includes('doc:manage'),
  };
}
```
`src/app.tsx` 的 `getInitialState` 从 `/blade-auth` 拉取权限集注入；`config/routes.ts` 路由加 `access: 'doc:view'` 做菜单级守卫。

### 4.2 按钮级（复用既有 `usePageButtons` / `usePermission`）
```tsx
const buttons = usePageButtons(['doc:edit','doc:publish','doc:download','doc:delete']);
// buttons 内无权限的按钮自动隐藏，不靠前端隐藏当安全
```

### 4.3 请求层（修 G1 写操作 GET / token 进 URL）
`src/request.ts`（或既有封装）：变更一律 `POST/PUT/DELETE`；`Authorization` header 带 token；**下载走 `@PreAuth` 授权端点**，敏感 `fileId` 不拼 URL 查询串，外链用后端签名 URL。

---

## 5. 落地校验清单（DoD，引用护栏清单 §2）

- [ ] 2.1 所有分部/租户过滤走 `@DataScope`，无手写 `sub_company_id`（FNA-N1/DET-P0-2/CUBE-N1）
- [ ] 2.2 每个写/读端点有 `@PreAuth`；内部 Feign 走 `/feign/client`+网关（DET-P0-1/2/3/4）
- [ ] 2.3 全部 `QueryWrapper`/`#{}`，无 `${}`/拼接（DOC-P0-2/5）
- [ ] 2.4 变更 `POST/PUT/DELETE`+CSRF；token 不进 URL（G1）
- [ ] 2.5 下载端点 `@PreAuth`+`canDownload` 归属校验；`fileId` 不可猜（DOC-P0-4，含 WfOdocController:232 复核）
- [ ] 2.6 异常 fail-closed + 日志；无 `System.out`（G5/G6）

---

## 6. 优先级

1. **先修 `WfOdocController.java:232`**（`@PreAuth("permitAll()") @GetMapping("/file/{fileId}/content")`）——确认是否「已发布公文公开」：若是，补注释+限时签名 URL；若否，按 §3 下载端点补 `canDownload`。这是 E9 DOC-P0-4 在你们工程里的同款点。
2. 再落地 `blade-doc` 模块（实体/DataScope/接口），复用本规范。
3. 前端 `DocEngine` 页面接 `access.ts` + `usePageButtons`。
