# 门户引擎 blade-portal · 交付文档

> 对标泛微 E-cology 首页门户（homepage / hpelement / elementshareinfo）。设计原则：
> **配置决定「有什么、显示什么数据、什么样式」；代码决定「每种元素怎么渲染/怎么取数」。**
> 设计蓝本：`doc/md/ecology-portal-config-analysis.md`。

---

## 1. 运行环境

| 组件 | 端口 | 说明 |
| --- | --- | --- |
| 基础设施 | MySQL `3306` / Redis `6379` / Nacos `8848`(控制台 `8080`) | 需先就绪 |
| blade-gateway | `81` | 网关；校验 `blade-auth: bearer <token>` + `Authorization: Basic sword:sword_secret` + `Tenant-Id` |
| blade-auth | `8100` | 登录/发 token（验证码已由系统参数 `captcha_mode` 关闭） |
| blade-system | `8106` | 用户/部门/菜单（`/menu/routes` 驱动前端动态路由） |
| blade-portal | `8140` | **本模块** |
| blade-message | `8130` | 消息铃铛（管理台外壳外设，缺失仅消息角标降级） |
| 前端 ant-design-pro | `8000` | `max dev`；代理 `/api` → `http://127.0.0.1:81`（`config/proxy.ts`） |

启动（与 blade-system 同参）：

```bash
"D:\project\weaver\jdk\bin\java.exe" -Xms256m -Xmx1536m -jar <模块>\target\<模块>.jar --spring.profiles.active=dev
```

数据源：`jdbc:mysql://localhost:3306/blade`，`root / 123456`（dev 默认；生产由 Nacos `blade-dev.yaml` 覆盖）。

---

## 2. 数据库迁移执行清单

迁移脚本位于 `doc/sql/migration/`，**按文件名序执行，全部幂等**（`CREATE TABLE IF NOT EXISTS`、`information_schema` 守卫加列、菜单/授权按 code 判重）。门户相关迁移：

```
V2026.10.13_001__portal_schema          门户基础表(页面/元素/菜单/主题/日志/默认)
V2026.10.13_002__portal_menu            门户管理菜单
V2026.10.14_001__portal_log             审计日志表
V2026.10.14_001__portal_theme_menu      主题中心菜单
V2026.10.14_002__portal_permission_menu 维护权限码(PERM_*)
V2026.10.14_003__portal_default         机构默认首页表
V2026.10.14_004__portal_default_org_menu 机构默认首页菜单
V2026.10.14_004__portal_theme           主题表
V2026.10.15_001__portal_user_default    个人默认门户
V2026.10.15_002__portal_menu            门户菜单管理
V2026.10.15_003__portal_log_page        审计日志页菜单
V2026.10.15_004__portal_user_layout     个人布局覆盖
V2026.10.15_005__portal_element_owner   元素归属(页面级共享/用户私有)
V2026.10.15_006__portal_login           登录前门户(page_type)+皮肤
V2026.10.15_007__portal_layout          布局模板库
V2026.10.15_008__portal_element_lib     独立元素库
V2026.10.15_009__portal_theme_timer     主题定时切换(open_timer)
V2026.10.15_010__portal_page_view_count 页面访问热度(view_count)
V2026.10.15_011__portal_page_stat       按日访问统计(blade_portal_page_stat)+访问分析菜单
V2026.10.15_012__portal_page_uv         独立访客(blade_portal_page_visitor)
```

> **排坑提示（实测）**
> 1. 新增页面类菜单，除 `blade_menu` 外**必须**同步 `blade_role_menu` 授权（`role_alias='administrator'`），否则 `/menu/routes` 不返回该路由 → 前端 404。V011 已补此授权。
> 2. 前端组件目录名与菜单 path 末段的 Pascal 推导需一致：`app.tsx` 的 `commonWords` 负责分词还原（已含 `Lib`/`Template`），个别无法还原的复合词登记到 `PAGE_DIR_ALIAS`。
> 3. `blade_portal_page` 等实体新增映射列（如 `view_count`/`page_type`）后，务必先把迁移刷到库再启动服务，否则全表查询 500。

一键应用（Windows，见根目录 `apply_portal_migrations.cmd`）：按序 `mysql ... < 各portal迁移.sql`。

---

## 3. 核心能力

- **配置**：页面（CRUD/复制/导入导出/默认/机构默认/可见范围/访问热度）、元素、独立元素库、菜单、主题（含定时）、素材、布局模板库。
- **登录前**：皮肤（定时切换）+ 登录前门户页 + 公开落地页 `/prelogin`（免鉴权取数）。
- **运行**：`GET /my-portal`（五粒度可见 + 用户个性化合并 + 多门户切换）；元素取数**线程池并行 + 单元素 3s 超时隔离**（`PortalExecutorConfig.elementDataExecutor`，CallerRunsPolicy 背压）；图表；运行时菜单树。
- **终端个性化**：布局覆盖、私有元素、个人默认门户。
- **治理**：审计日志；访问分析（`/page/stats/daily` 每日 PV+UV、`/page/stats/top` 热门页 TopN）。
- **数据型元素按组织过滤**：取数上下文带 `orgScope`(1租户/2部门/3分部) 与 `deptId/branchId`，Provider 据此过滤（租户始终隔离；`notice` 已按 `tenant_id`/`create_dept` 真实过滤）。
- **权限**：管理端 `HAS_AUTH + PortalRightUtil.check(PERM_*)` 双层门禁；终端五粒度共享（全员1/部门2/个人3/角色4/分部5，分部以「部门树最浅非零祖先」为口径）。

## 4. 关键接口（经网关 `/api/blade-portal/**`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/my-portal` | 我的门户（页面+元素，已合并个性化） |
| GET | `/pre-login/my-portal` | 登录前门户（免鉴权） |
| POST | `/element-data/batch` | 批量取数（元素维度，并行+超时隔离） |
| POST | `/pre-login/element-data/batch` | 登录前批量取数（免鉴权） |
| GET | `/page/stats/daily?days=30` | 访问趋势（PV/UV，缺日补0） |
| GET | `/page/stats/top?limit=10` | 热门门户页 |
| GET/POST | `/page/list` `/page/submit` `/page/remove` `/page/duplicate` `/page/export` `/page/import` | 页面管理 |
| GET/POST | `/page/share/list` `/page/share/set` | 页面可见范围 |
| GET/POST | `/my-portal/layout` `/my-portal/layout/save` `/my-portal/layout/reset` | 个人布局 |

## 5. 扩展指南

### 5.1 新增一种元素类型（引擎零改动）
1. 后端：新增 `IPortalElementDataProvider` 实现（声明 `type()`，`load(ElementDataContext)` 内部**必须自捕获异常**，降级返回 `success=false`，不得外抛）；Spring 自动收集进 `ElementDataProviderRegistry`。
2. 前端：`src/pages/Portal/components/ElementRegistry.tsx` 注册对应 React 组件；元素组件从 `ElementDataVO.data` 取数渲染。
3. 配置：`blade_portal_element.element_type` 使用该类型值；元素私有配置存 `config_json`（`perPage`/`chartType`/`dataOrgScope`/`links`/`html` 等），取数时经 `ElementDataContext.config` 透传。
4. 取数上下文可用：`userId/deptId/tenantId/branchId/orgScope/filterDeptId/branchDeptIds/limit`——**按组织过滤**即据此在 Provider 内拼查询条件。

### 5.2 新增一个管理页面
1. 建组件 `src/pages/Portal/<Page>/index.tsx`（目录名与菜单 path 末段 Pascal 一致，必要时补 `app.tsx` `commonWords`）。
2. 迁移插 `blade_menu`（`parent_id` 指向 `code='portal'`，path 两段连写如 `/portal/xxx`）**并** `blade_role_menu` 授权 `role_alias='administrator'`。
3. 需要维护权限时配 `PERM_*` 权限码，控制器用 `requireRight(PERM_*)` 门禁。

---

## 6. 验证

- 后端：`mvn -o -DskipTests compile` → BUILD SUCCESS。
- 前端：`read_lints` 0 error；`tsc --noEmit` 无本次相关错误。
- 端到端：Playwright 冒烟（真实登录 + 逐页巡检控制台错误/失败请求/关键 UI/图表）**11/11 通过**。脚本：`SpringBlade/run-logs/pw_smoke.cjs`，截图：`run-logs/shots/*.png`。
