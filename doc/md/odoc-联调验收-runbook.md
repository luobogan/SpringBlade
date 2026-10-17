# odoc 收发文一体化 — 联调验收 Runbook

> 本清单覆盖本轮（2026-10-16/17）全部新增：公文规范字典、意见模板、收发文单位+公共组、
> 发文字号、通用设置、列表设置、密级收发限制、节点动作接入（套红/签章/密级拦截、定稿取号、自动版式）。
> 全部为 blade-workflow 后端 + ant-design-pro 前端增量，**无新增三方依赖**。

---

## 0. 前置确认
- 本机即 dev（192.168.1.5）；Nacos / MySQL / Redis 本地；端口：gateway=81、blade-workflow=8111、前端 dev=8000。
- 本次改动的表都在 **workflow 业务库**（与既有 `wf_odoc_*` / `wf_*` 同库）；菜单在 **blade 库**（`blade_menu` 所在库）。
  > 若你的 dev 把二者合在同一个 `blade` 库，则全部脚本对该库执行即可（脚本已按 `blade.` 前缀写菜单、表不写前缀）。

---

## 1. 应用迁移 SQL（按序，全部幂等，可重跑）

**A. 表 DDL + 种子 → workflow 业务库**
```
doc/sql/migration/V2026.10.16_020__wf_odoc_dict_opinion.sql     # 字典 + 意见模板 表&种子
doc/sql/migration/V2026.10.16_030__wf_odoc_receive_unit_group.sql # 收发文单位 + 公共组 表&种子
doc/sql/migration/V2026.10.16_060__wf_odoc_setting.sql           # 通用设置 表&默认行
doc/sql/migration/V2026.10.16_080__wf_odoc_secret_rule.sql       # 密级收发限制 表(无种子)
doc/sql/migration/V2026.10.16_100__wf_odoc_list_set.sql          # 列表列设置 表
doc/sql/migration/V2026.10.16_110__wf_odoc_add_create_dept.sql   # 修正:补 create_dept 列(幂等,必跑)
```

**B. 菜单 DML → blade 库**
```
doc/sql/migration/V2026.10.16_040__wf_odoc_basedata_menu.sql     # 公文规范/意见模板/收文单位/公共组
doc/sql/migration/V2026.10.16_050__wf_odoc_number_menu.sql       # 发文字号
doc/sql/migration/V2026.10.16_070__wf_odoc_setting_menu.sql      # 公文设置
doc/sql/migration/V2026.10.16_090__wf_odoc_secret_rule_menu.sql  # 密级收发限制
```

一键示例（mysql 客户端，按实际库名/账号调整）：
```bat
set WFDB=blade
set BLADEDB=blade
for %f in (020 030 060 080 100 110) do mysql -uroot -p123456 %WFDB% < doc\sql\migration\V2026.10.16_%f__*.sql
for %f in (040 050 070 090)          do mysql -uroot -p123456 %BLADEDB% < doc\sql\migration\V2026.10.16_%f__*.sql
```
> dev 实际库名是 `blade`（wf_odoc_* 与 blade_menu 同库，无独立 blade_workflow 库）；账号 root/123456、mysql 客户端在 `D:\project\mysql-8.0.23-winx64\bin\`。
> **必跑 V110**：补 `create_dept` 列（TenantEntity 基类字段，初版 CREATE TABLE 漏写；不补则运行期查询报 `Unknown column 'create_dept'`）。
> 每个文件头部均注明「是否幂等 / 回滚 SQL」。种子另由 `OdocBaseDataSeeder` 启动兜底（幂等）。

---

## 2. 重建 + 重启 blade-workflow
```bat
cd d:\workproject\springbladeandreact\springBlade
mvn -o -pl blade-service-api/blade-workflow-api,blade-service/blade-workflow install -DskipTests
mvn -o -pl blade-service/blade-workflow spring-boot:run   # 后台启动，日志重定向到 %TEMP%
```
- 在线判定：`netstat -ano | findstr :8111` 有监听；或 `/api/blade-workflow/odoc/setting` 不再返回 404「No static resource」。
- ⚠️ 停旧进程再起（端口占用）；IDEA 调试进程带 `-agentlib jdwp suspend=y` 的不要复用其 argfile。

---

## 3. 刷新菜单
重新登录（前端菜单有 localStorage 缓存 `sword-routes`/`sword-buttons`；重登即刷新）。「公文管理」下应出现：
公文规范 / 意见模板 / 收发文单位 / 收文公共组 / 发文字号 / 公文设置 / 密级收发限制。

---

## 4. 冒烟验收清单（逐项打钩）

| # | 验证点 | 操作 | 预期 |
|---|---|---|---|
| 1 | 通用设置 | 公文设置页：关「开放 OFD」→ 保存 | 保存成功；流程建文设置的「版式格式」不再出现 OFD(13) 选项 |
| 2 | 公文规范 | 公文规范页 5 个 Tab 增删改 | 秘密/紧急/种类(15文种已种子)/主题/机构代字 可维护 |
| 3 | 意见模板 | 意见模板页新增一条 | 列表可见、可编辑删除 |
| 4 | 收发文单位 | 新增单位（选上级/内外部） | 列表+层级正常 |
| 5 | 公共组 | 新增公共组选成员单位 | 成员显示为名称 |
| 6 | 发文字号 | 新增文号规则（代字/年份/位数/补零） | 保存成功 |
| 7 | 密级收发限制 | 新增规则：单位甲 仅收 机密~绝密 | 保存成功 |
| 8 | 列表设置 | 待办列表右上角「列设置」取消某列 | 该列隐藏，刷新后仍隐藏（落库） |
| 9 | 套红拦截 | 流程建文设置配套红节点+强制套红；该节点未套红提交 | 拒：「该节点提交前必须完成公文套红」 |
| 10 | 定稿取号 | 配文号字段+定稿节点；走到定稿 | 表单文号字段自动回写生成文号（幂等） |
| 11 | 自动版式 | 配转版式节点+版式格式(12/13) | 走过该节点自动产 PDF/OFD，`/odoc/layout/list` 可见 |
| 12 | 密级拦截 | 建文设置配密级字段+收文单位字段；收文单位=甲、密级=公开 提交 | 拒：「收文单位密级不符…」 |
| 13 | 单位 picker | 表单设计器把主送/抄送字段配 `fieldHtmlType=3`+`browType=259` |  picker 弹出，可选单位/公共组；待办「单位」列显示名称（非 id） |

> 第 9/10/11/12/13 依赖**后端表单设计器**把相应字段/节点配置好（数据配置，非代码）。

---

## 5. 回滚
- 表：各 DDL 文件头 `DROP TABLE wf_odoc_*;`
- 菜单：各菜单文件头 `DELETE FROM blade_menu WHERE id=...; DELETE FROM blade_role_menu WHERE menu_id=...;`
- 后端：`git checkout` 本轮改动文件即可（纯增量，无破坏性变更）。

---

## 6. 已知约束 / 后续（非阻塞）
- **功能菜单 / 报表权限**：未重造，建议复用 blade 既有菜单/权限体系。
- **机构代字**：已并入院文号 `prefix` 与字典 `org_abbr`，基本可用，未独立建表。
- **列设置**：当前按列表（管理员级默认），非按用户。
- **套红/版式像素级正确性**：需在真实 OFD/PDF 阅读器（数科/福昕/WPS）核验；CA 签章待接厂商 SDK。
