# 用户创建功能改造计划（对齐泛微 Ecology 架构）

> 目标页面：`http://192.168.1.4:8000/system/user`（用户管理 → 新增用户）
> 参考项目：`D:\Weaver2020\ecology`（E9 部署版，前端 JSP/JS） + `E:\project\ecology\ecology\src`（E9 源码，weaver.* / com.engine.*）
> 本项目：`E:\project\springbladeandreact\ant-design-pro`（前端） + `E:\project\springbladeandreact\SpringBlade`（后端）
> 文档版本：v1.4（2026-10-08）
> v1.4 修订：① **新增 §14「P3 实施记录：状态流转工作流化 + 伴生初始化」**（5 步全量）；② 修订 §6 风险表在职判定口径，与 §5-P3-5 统一为「在职 `in (0,1,2,3,5)`，仅 4 解聘拒绝登录」（原表述含"退休"属文档内部矛盾，按裁定统一）
> v1.3 修订：① 新增 §12「P0-8 收尾：Excel 导入导出与新字段对齐」（补齐导出 SQL 缺列 + 导入逐行容错报错到行）；② §11.2 回填 P1 接口级验证实测结果（6 项全过）；③ 新增 §13「P2 实施记录：字段配置驱动 + 自定义字段」
> v1.1 修订（查漏）：① 修正 person_status 枚举口径（PmAction 0-8 为流程场景类型，非字段值），统一两处 DEFAULT=1；② 补「逻辑删除 × 唯一键」对策（删除时置空编号）；③ P1 补直接上级/头像上传步骤；④ P0 补 Excel 导入导出同步；⑤ P3 补离职状态登录联动；⑥ 预检接口补防枚举措施；⑦ 8.5 补部门→分部归属推导规则；⑧ 风险表 +3 行
> v1.2 修订：新增第 10 章「Ecology → SpringBlade 数据迁移方案」（人员/部门/分部），**改造完成后再执行**
> ✅ **实施状态（v1.3 / 2026-10-08）**：P0（用户核心）、第 8 章（部门/分部追加字段）、**P1（表单交互与密码策略）** 均已完成并联调通过（P0/第8章 12 项见 §5-P0/§8.5；**P1 6 项见 §11.2，本次实测全过**）；ALTER SQL 已在 dev 库执行；blade-system 已重打包重启（8106）。
> 🆕 **本次补齐**：Excel 导入导出与 P0 新字段对齐（§12）——`exportUser` SQL 补 `work_code`/`person_status`、导入改为**逐行容错 + 行号级报错**（对齐 P0-8「重复工号报错到行」）。已编译通过，**待重打包重启后回归**。
> 🆕 **P2 已实施（2026-10-08，§13）**：三张配置/扩展表已建（dev 库执行成功）、schema 接口 + 扩展表写入 + 前端配置驱动渲染已完成并编译通过（后端 EXIT=0、前端 tsc 无新增错误），**待重打包重启后联调**。
> 🆕 **P3 已实施（2026-10-08，§14）**：状态流转配置表/记录表 + 信息完善度表已建（dev 库 5/5 成功）、审批发起与回调链路、伴生初始化、主管消息通知、在职性登录拦截（三个 Granter）已完成并编译通过（blade-system / blade-workflow / blade-auth 均 EXIT=0，前端 tsc 无新增错误），**已重打包重启并联调通过**（`doc/tools/verify-p3.cjs` 直连 8100/8106，11 项全过：建档→完善度4项、可用流转、降级直改、流转记录、在职可登录 → 解聘被拒「账号已停用」→ 恢复可登录）。
> **待办**：① Ecology 数据迁移（第 10 章，用户选择暂缓）；② 审批闭环端到端验证（需先启动 Nacos + blade-workflow 并部署 §14.5 的 BPMN）。
> ⚠️ **环境提示**：Nacos（8848）、网关（81）、blade-auth（8100）、blade-system（8106）当前均在线；运行中的 jar 位于 `D:\project\springbladeandreact\SpringBlade\{模块}\target\*.jar`（JDK `D:\project\weaver\jdk\bin\java.exe`）。P3 改动已从 D: 工作副本 `mvn package -DskipTests` 重打包并重启 blade-auth/blade-system 生效（2026-10-08）；验证脚本 `verify-p3.cjs` 仍按**直连 8100/8106** 编写，与网关/Nacos 无关，可独立运行。

---

## 1. 背景与目标

当前 SpringBlade 的用户创建能力非常薄：单页表单（租户/账号/密码/昵称/姓名/手机/邮箱/性别/生日/角色/部门/岗位），后端仅做租户校验、账号查重、密码加密后单表落库。相比泛微 Ecology 的人员创建体系，缺少：工号编码规则、人员状态体系、字段配置驱动、唯一性预检、分步表单、伴生数据初始化、密码策略、分部级数据权限等能力。

本计划目标：**在不破坏 SpringBlade 多租户架构与既有登录/工作流链路的前提下**，按 Ecology 的架构规范补齐用户创建能力，分期实施、每期可独立验证。

**改造原则（与 Ecology 对齐的四个架构特征）**：
1. **配置驱动字段**：表单字段由「字段分组 + 字段配置」表驱动，支持启用/停用/必填/控件类型，而非硬编码。
2. **分层校验**：前端即时预检（隐藏 iframe → 本项目对应异步接口）+ 后端保存时兜底校验 + 存储层唯一键三道防线。
3. **主表 + 伴生写入**：人员主表插入后伴随审计信息、系统信息（登录名/密码）、冗余触发表、自定义字段等伴生写入，有明确的成功/失败语义。
4. **状态机驱动**：人员状态（入职/试用/转正/调动/离职…）由配置表定义流转，而非散落 if-else。

---

## 2. 参考架构：Ecology 用户创建全链路

### 2.1 前端（部署版 `D:\Weaver2020\ecology\hrm`）

| 环节 | 实现 | 关键文件（绝对路径） |
|---|---|---|
| 入口 | 组织维护页「新增人员」链接 / 部门页弹窗 | `D:\Weaver2020\ecology\hrm\HrmMaintenance.jsp`、`D:\Weaver2020\ecology\hrm\company\HrmDepartmentDsp.jsp` |
| 全量新增页（E8） | 单表单 + `wea:layout` 分组，固定字段 + 5 组自由字段，三个按钮（保存/保存并新增/保存并进入下一步） | `D:\Weaver2020\ecology\hrm\resource\HrmResourceAdd.jsp` |
| 部门弹窗简版 | 单 `ViewForm` 平铺：工号/姓名/账号类型/主账号/性别/部门/岗位/职务/状态/联系方式… | `D:\Weaver2020\ecology\hrm\company\HrmResourceAdd.jsp` |
| **配置驱动路由** | `CusFormSetting("hrm","HrmResourceBase")`：status=2 转发到配置驱动页 `HrmResourceAddNew.jsp`，status=3 转发完全自定义页面 | `D:\Weaver2020\ecology\hrm\resource\HrmResourceAdd.jsp:3-18` |
| **字段配置驱动渲染** | `HrmFieldGroupComInfo`（分组）+ `HrmFieldManager("HrmCustomFieldByInfoType", scopeId)`（字段：isUse/isMand/控件反射 `eleclazzname`）动态输出表单；必填清单 `needinputitems` 按 `isMand()` 累加 | `D:\Weaver2020\ecology\hrm\resource\HrmResourceAddNew.jsp:281-403` |
| 分步表单 | 第 2 步个人信息（`addresourcepersonalinfo`）、第 3 步工作信息（`addresourceworkinfo`） | `D:\Weaver2020\ecology\hrm\resource\HrmResourceAddTwo.jsp`、`HrmResourceAddThree.jsp` |
| 提交目标 | `hrm\resource\HrmResourceOperation.jsp`（multipart），按 `operation` 分支分发 | `D:\Weaver2020\ecology\hrm\resource\HrmResourceOperation.jsp:484-762` |
| 前端校验 | `check_form(needinputitems)`（公共 `systeminfo\init_wev8.jsp:300`）、`checkinput*` 系列（`js\weaver_wev8.js`）、邮箱/照片/数字校验 | 同左 |
| **唯一性预检** | 隐藏 iframe 加载 `HrmResourceCheck.jsp?lastname=&workcode=`：工号重复 → 阻断；姓名重复 → confirm 放行；通过后回调 `parent.checkPass()` 才 submit | `D:\Weaver2020\ecology\hrm\resource\HrmResourceCheck.jsp`、`HrmResourceAdd.jsp:910-940` |
| 浏览框选择器 | 部门 `DepartmentBrowser2/3.jsp`（带权限 SQL：`rightLevel` 裁剪 WHERE）、岗位 `JobTitlesBrowser.jsp`、主账号 `ResourceBrowser.jsp`（`accounttype is null or =0`）、职务类别/办公地点浏览器 | `D:\Weaver2020\ecology\hrm\company\DepartmentBrowser3.jsp` 等 |
| ID 预占 | 页面先 `max(id)+1` 预占展示，提交时以 `HrmResourceMaxId_Get` 存储过程取真值 | `HrmResourceAdd.jsp:258-278` |
| 提交后流转 | `cmd=SaveAndNew` → 保留部门继续新增；`cmd=SaveAndNext` → 第 2 步；默认回列表 `isclose=1` 触发父窗刷新 | `HrmResourceOperation.jsp:752-762` |

### 2.2 后端（源码 `E:\project\ecology\ecology\src`）

**两条链路**：

| 链路 | 入口 → 编排 → 落库 | 文件 |
|---|---|---|
| E9 新引擎（REST + Command） | `@Path("/hrm/resource/add")` hasRight / getHrmResourceAddForm / save / saveSimple → `HrmResourceAddService.save`（L809）→ `HrmResourceBaseService.addResourceBase`（L2179） | `com\api\hrm\web\HrmResourceAddAction.java`、`com\api\hrm\service\HrmResourceAddService.java`、`com\api\hrm\service\HrmResourceBaseService.java` |
| 纯 Command 模式 | `@Path("/addResourceBase")` → `ResourceServiceImpl` → `commandExecutor.execute(new AddResourceBaseCmd(params,user))`：必填校验 → `hrmresourceallview` loginid 查重 → `HrmResourceMaxId_Get` → `HrmResourceBasicInfo_Insert` → `HrmResource_CreateInfo` → loginid/password 落库 → 缓存刷新 | `com\api\hrm\web\ResourceAction.java`、`com\api\hrm\service\impl\ResourceServiceImpl.java`、`com\api\hrm\cmd\resource\AddResourceBaseCmd.java` |
| 老架构（工作流驱动人事建档） | `PmAction`（状态常量 0入职…8再入职，`hrm_state_proc_set` 路由）→ `HrmResourceEntrantAction`：`HrmResourceMaxId_Get` → `HrmResourceBasicInfo_Insert` → commit → 定义字段/系统信息/缓存/薪资初始化/触发器/信息状态 | `weaver\hrm\pm\action\PmAction.java`、`weaver\hrm\pm\action\HrmResourceEntrantAction.java` |

**落库与伴生写入**（存储过程垫片位于 `com\weaver\procedure\`）：

| 存储过程/写入 | 作用 | 垫片类 |
|---|---|---|
| `HrmResourceMaxId_Get` | `update SequenceIndex set currentid=currentid+1 where indexdesc='resourceid'` 后取值（ID 生成） | `com\weaver\procedure\hrmresourcemaxid\Hrmresourcemaxid_get.java` |
| `HrmResourceBasicInfo_Insert` | 主表 `INSERT INTO HrmResource`（29 列：workcode/lastname/sex/departmentid/jobtitle/joblevel/status/locationid/telephone/mobile/email/subcompanyid1/managerid/assistantid/accounttype/belongto…） | `com\weaver\procedure\hrmresourcebasicinfo\Hrmresourcebasicinfo_insert.java` |
| `HrmResource_CreateInfo` | 审计字段（createrid/createdate/lastmodid/lastmoddate） | `com\weaver\procedure\hrmresourcecreateinfo\Hrmresourcecreateinfo.java` |
| `Hrmresourcesysteminfo_insert` | 登录名/密码/安全级别/密码策略 + **loginid 第三重查重**（`SELECT COUNT(id) FROM HrmResource WHERE id!=? AND loginid=?`） | `com\weaver\procedure\hrmresourcesysteminfo\Hrmresourcesysteminfo_insert.java` |
| `HrmResource_Trigger_Insert` | 冗余触发表 `HrmResource_Trigger`（managerid/departmentid/subcompanyid1） | `com\weaver\procedure\hrmresourcetrigger\Hrmresourcetrigger_insert.java` |
| `HrmResourceDefine_Update` | 自定义字段预留列 `datefield1-5/numberfield1-5/textfield1-5/tinyintfield1-5` | `com\weaver\procedure\hrmresourcedefine\Hrmresourcedefine_update.java` |
| 直写（`HrmResourceBaseService.addResourceBase` L2312-2526） | `RecordSetTrans` 手动事务仅包主表；`SalaryManager.initResourceSalary`、`insert into HrmInfoStatus(itemid,hrmid) values(1/2/3/10,…)`（信息完善状态）、`HrmResourceVirtual`（虚拟组织）、`userprivacysetting`、`CustomFieldTreeManager.editCustomDataE9Add`（写 `cus_fielddata`）、`ResourceComInfo.addResourceInfoCache`（缓存） | `com\api\hrm\service\HrmResourceBaseService.java` |

**校验体系**（三道防线）：
- 前端预检：`HrmResourceCheck.jsp`（工号查重阻断 / 姓名重复 confirm）。
- 保存时：`hrmResourceCheck`（L3237：`select workcode from HrmResource where workcode=?`）；loginid 三重查重（save L865-885 主表 + `HrmResourceManager` 管理员表；`AddResourceBaseCmd` L115 查 `hrmresourceallview` 视图；proc 内部第三次）；证件号唯一（L891）；手机/座机格式 `ValidateFieldManager.validate`。
- 工号自动编码：`CodeRuleManager.generateRuleCode(RuleCodeType.USER, subcompanyid1, departmentid, jobtitle, workcode)`（`com\engine\hrm\util\CodeRuleManager.java`），命中预留号段时删除 `hrm_coderulereserved`。

**密码与安全**：
- 传输层 RSA（`weaver.rsa.security.RSA`，开关 `openRSA/isrsaopen`）；初始密码取自 `ChgPasswdReminder` 的 `defaultPasswordEnable/defaultPassword` 配置；加密 `PasswordUtil.encrypt`（SM3 加盐，返回 `[密文, salt]`，`weaver\general\PasswordUtil.java:23`）。

**权限与约束**：
- 功能权限 `HrmResourceAdd:Add` / 保存需 `HrmResourceEdit:Edit`（`HrmUserVarify.checkUserRight`）；分部级 `CheckSubCompanyRight.ChkComRightByUserRightCompanyId`；分部管理员配额 `HrmResourceManager.noMore`；License 人数 `LN.CkHrmnum()`；验证码（sysadmin 豁免）。

**状态与字段配置**：
- 人员状态枚举（`PmAction.java:26-34`）：0 入职建档 / 1 试用 / 2 正式 / 3 延期 / 4 调动 / 5 离职 / 6 退休 / 7 解聘 / 8 再入职；新建默认 `status=1`（正式）；在职判定 = `status in (0,1,2,3,5)`；流转配置表 `hrm_state_proc_set`（引擎服务 `com\engine\hrm\service\impl\HrmStateSetServiceImpl.java`）。
- 字段配置表：`hrm_fieldgroup`（`grouptype` 区分基本信息/个人信息/工作信息）+ `HrmFieldManager`（`HrmCustomFieldByInfoType` 体系）；保存双路落库：hrmresource 预留列（`HrmResourceDefine_Update`）+ E9 扩展表 `cus_fielddata`（`CustomFieldTreeManager`）。

### 2.3 Ecology 数据模型要点

| 表 | 角色 |
|---|---|
| `HrmResource` | 人员主表（工号/姓名/组织/岗位/状态/联系方式 + 自定义预留列） |
| `HrmResource_Trigger` | 组织/主管冗余触发表（供联动刷新） |
| `HrmInfoStatus` | 人员信息完善状态（itemid 1/2/3/10 对应基本/个人/工作/系统信息） |
| `hrm_fieldgroup` / `HrmCustomFieldByInfoType` 配置 | 字段分组与字段元数据（启用/必填/控件类型） |
| `cus_fielddata` | E9 自定义字段值（scope='HrmCustomFieldByInfoType'） |
| `SequenceIndex` | ID 序列（resourceid） |
| `hrm_coderulereserved` | 工号编码规则预留号段 |
| `hrm_state_proc_set` | 人员状态流转（对应入职/转正/离职等流程）配置 |

---

## 3. 现状：SpringBlade 用户创建

### 3.1 前端（`ant-design-pro/src/pages/System/User/`）

| 文件 | 现状 |
|---|---|
| `User.tsx` | 列表页，维护入口 |
| `UserAdd.tsx` | 单页表单，三组硬编码字段：**基础信息**（tenantId/account/password/confirmPassword）、**详细信息**（name 昵称/realName 姓名/phone/email/sex/birthday）、**职责信息**（roleId/deptId/positionId，`TreeSelect` 多选 → 逗号串）。**无工号、无人员状态、无唯一性预检、无头像、无自定义字段、无分步** |
| `UserEdit.tsx` / `UserView.tsx` | 编辑/查看，同结构 |

提交：`src/services/system/user.ts → submit()` → `POST /blade-system/user/submit`（JSON）。

### 3.2 后端（`SpringBlade/blade-service/blade-system`）

- `controller/UserController.java:132`：`POST /submit`，`@PreAuth(RoleConstant.HAS_ROLE_ADMIN)` + `@Valid @RequestBody User` → `userService.submit(user)`。
- `service/impl/UserServiceImpl.java:84-127`：
  ```java
  submit(user) → TenantGuard.bindTenant(租户归属) → 租户非空校验
              → TenantGuard.verifyBatch(roleService, roleId, ROLE)
              → doSubmit(user)：DigestUtil.encrypt(password)
                            → 租户内 account 查重（cnt>0 → "当前账号已被使用!"）
                            → save / updateById
  ```
- **无**：工号/编码规则、人员状态、伴生数据初始化、保存事务边界声明（`doSubmit` 无 `@Transactional`，目前单表尚可）、字段配置、在途预检接口、分部级数据权限。

### 3.3 数据模型（`blade_user`，实体 `blade-service-api/blade-user-api/.../user/entity/User.java`）

已有：`code`（编号，未启用）、`account`、`password`、`name`(昵称)、`realName`、`avatar`、`email`、`phone`、`openId`、`birthday`、`sex`、`roleId`(CSV)、`deptId`(CSV)、`postId`(CSV)、**`managerId`（直属主管，已对齐 E9 managerid）**、商城会员字段组、`themeSetting`（自定义 JSON 先例）。

缺失（对照 Ecology）：`work_code` 工号、人员状态（`BaseEntity.status` 已被通用状态占用，需独立列）、`account_type` 主/次账号、`certificate_num` 证件号、`salt` 密码盐、自定义字段存储、办公地点/办公室/职务类别/职级等 E9 工作信息字段。

---

## 4. 差异点矩阵（Ecology vs SpringBlade）

| # | 能力 | Ecology | SpringBlade 现状 | 差距等级 |
|---|---|---|---|---|
| 1 | 工号 + 编码规则 | `workcode` + `CodeRuleManager.generateRuleCode` + 预留号段 | 无工号字段 | ★★★ |
| 2 | 人员状态体系 | status 0-8 + `hrm_state_proc_set` 流转 + 流程 Action | 无（BaseEntity.status 为通用标记） | ★★★ |
| 3 | 唯一性三道防线 | iframe 预检 + save 时三重查重（含管理员视图）+ proc 内兜底 | 仅后端租户内 account 查重，无预检 | ★★★ |
| 4 | 字段配置驱动 | `hrm_fieldgroup` + `HrmFieldManager` + `CusFormSetting` 布局路由 + `cus_fielddata` | 表单硬编码 | ★★ |
| 5 | 分步表单（基本/个人/工作） | 3 步 + `SaveAndNext` | 单页 | ★ |
| 6 | 密码策略 | RSA 传输 + 默认密码配置 + SM3 加盐存储 | 明文 HTTPS 提交 + `DigestUtil.encrypt`，默认密码常量 | ★★ |
| 7 | 伴生数据初始化 | HrmInfoStatus / 薪资 / 虚拟组织 / 缓存 / 触发表 | 无 | ★★ |
| 8 | 组织选择器权限裁剪 | Browser + `rightLevel` 权限 SQL | `TreeSelect` 全量树（无数据权限裁剪） | ★★ |
| 9 | 数据权限粒度 | 分部级 `CheckSubCompanyRight` + 配额 | `HAS_ROLE_ADMIN` 单一开关 | ★★ |
| 10 | 姓名重复策略 | 重复仅 confirm 提示不阻断 | 无 | ★ |
| 11 | 次账号机制 | `accounttype` + `belongto`（主账号 + loginid 自动生成） | 无 | ★ |
| 12 | ID 生成 | `SequenceIndex` 连续序列 | MyBatis-Plus 雪花 `ASSIGN_ID` | 保留现状（分布式更优） |
| 13 | 多租户 | 无租户概念，按分部权限隔离 | `tenantId` 强隔离 | **保留现状**（本项目根本差异，不可照搬 Ecology 的分部模型） |
| 14 | 姓名模型 | `lastname`(+firstname) | `name` 昵称 + `realName` 姓名 | 保留现状，映射说明写入接口文档 |

---

## 5. 改造计划

### P0 数据模型与校验对齐（核心，1-2 周）

**目标**：补齐工号、人员状态、唯一性三道防线——这三项是「创建的人」能否被其他模块（工作流/消息/报表）正确消费的地基。

| 步骤 | 涉及文件 | 修改内容 | 依赖 | 验证方式 |
|---|---|---|---|---|
| 1. 表结构 | `SpringBlade/doc/sql/`（新增 `alter_blade_user_hrm.sql`） | `ALTER TABLE blade_user ADD work_code varchar(50) NULL, ADD person_status tinyint NOT NULL DEFAULT 1 COMMENT '人员状态(经典E9):0试用 1正式 2临时 3延期 4解聘 5退休;新建默认正式,以现场字典为准', ADD certificate_num varchar(30) NULL, ADD salt varchar(64) NULL, ADD UNIQUE KEY uk_blade_user_tenant_workcode(tenant_id, work_code)`（唯一键放 DB 层）。⚠️ 两个坑：① `PmAction.java:26-34` 的 0入职…8再入职是**流程场景类型**（`hrm_state_proc_set` 的场景路由），**不是** status 字段值，注释勿混用；② `blade_user` 为逻辑删除（is_deleted），删除用户时须将 `work_code` 置 NULL，否则唯一键使工号永久无法复用 | 无 | SQL 在 dev 库执行成功；旧数据 `work_code` 为 NULL 不受影响；删除用户后原工号可复用 |
| 2. 实体 | `blade-service-api/blade-user-api/.../user/entity/User.java` | 增加 `workCode / personStatus / certificateNum / salt` 字段（`@TableField` 对应列） | 步骤 1 | 编译通过 |
| 3. 编码规则服务 | 新增 `blade-system/service/IWorkCodeRuleService.java` + `impl/WorkCodeRuleServiceImpl.java` | 简化版 `CodeRuleManager`：规则表 `blade_code_rule(rule_code, prefix, date_fmt, seq_len, current_seq)`，`generate(tenantId, deptId)` 产出工号；预留号段（对齐 `hrm_coderulereserved`）可后置 | 步骤 1 | 单测：并发 100 次生成无重复 |
| 4. 后端校验 | `UserServiceImpl.submit/doSubmit` | ① `workCode` 租户内查重（缺省时若启用编码规则自动生成）；② `certificateNum` 唯一校验（`account_type=主账号` 范围内）；③ `doSubmit` 补 `@Transactional(rollbackFor=Exception.class)`（为 P2 伴生写入预留事务边界）；④ 抛错消息与 HTTP 语义对齐前端提示 | 步骤 2 | 接口测试：重复工号/证件号返回业务错误而非 500 |
| 5. 预检接口 | `controller/UserController.java` + `IUserService` | 新增 `POST /user/check`（`{field:'account'|'workCode', value, tenantId}`）→ `{available:boolean}`，对齐 `HrmResourceCheck.jsp` 的提交前预检；**接口置于 `@PreAuth(HAS_ROLE_ADMIN)` 内并限流**（防止未授权账号枚举探测） | 步骤 4 | curl/联调脚本验证 available 语义；未授权调用返回 403 |
| 6. 前端预检 + 工号/状态字段 | `UserAdd.tsx` / `UserEdit.tsx` | ① 增加「工号」（失焦/提交前调 `/user/check`，对齐 Ecology 隐藏 iframe 模式；提供「按规则自动生成」按钮）与「人员状态」（字典下拉，默认"正式"，对齐 E9 status=1）；② `User.tsx` 列表增加工号/状态列与筛选 | 步骤 2/5 | 手工验证：预检红字提示、自动生成不重复 |
| 7. 字典 | 前端字典服务（`src/services/system/dict` 或常量） | 人员状态枚举字典（对齐 E9 经典枚举：试用/正式/临时/延期/解聘/退休，以现场字典为准） | 步骤 6 | 下拉与列表显示一致 |
| 8. 导入导出同步 | `blade-system/excel/UserExcel.java`、`UserController.importUser/exportUser` | `UserExcel` 增加 workCode/personStatus 列；`importUser` 复用 `doSubmit` 校验链（工号/账号/证件号查重对导入同样生效，对齐 Ecology 批量建档同源校验） | 步骤 2/4 | 导入含重复工号的文件 → 明确报错到行 |

**P0 验收**：创建用户时工号可自动生成且全局唯一、状态必选默认正式、重复工号/账号/证件号在三道防线（前端预检/后端校验/DB 唯一键）均被拦截。

### P1 表单交互与密码策略对齐（1 周）

| 步骤 | 涉及文件 | 修改内容 | 依赖 | 验证方式 |
|---|---|---|---|---|
| 1. 保存语义按钮 | `UserAdd.tsx` | 增加「保存并继续新增」（清表单保留租户/部门，对齐 `cmd=SaveAndNew`）与「保存」（回列表刷新，对齐 `isclose=1` 父窗刷新） | P0 | 手工验证连续建档效率 |
| 2. 密码传输加密 | 前端复用 `src/utils/sm2.ts`；后端 `UserController.submit`/`resetPassword` | 前端用 SM2 公钥加密密码再提交（公钥沿用 `config/defaultSettings.auth.publicKey`，与登录链路一致）；后端按 header `encrypt-flag` 解密后 Digest | 登录链路已具备 | 抓包确认无明文密码；登录回归 |
| 3. 默认密码配置 | `ITenantService`/租户表或 Nacos `blade-dev.yaml` | 默认密码可配置（对齐 `ChgPasswdReminder`），`resetPassword` 改读配置 | 无 | 修改配置后重置密码生效 |
| 4. 盐值存储（可选） | `UserServiceImpl` | 如沿用 `DigestUtil.encrypt` 则不加 salt（兼容存量密码）；如引入加盐算法需迁移策略：新用户加盐、存量用户登录时惰性升级 | 步骤 1-3 | 新旧用户均可登录 |
| 5. 组织选择器数据权限 | `UserAdd.tsx` 部门 TreeSelect 数据源；后端 dept tree 接口 | 非管理员按数据权限裁剪部门树（对齐 `DepartmentBrowser3` 的 `rightLevel` SQL），接口按当前用户可见范围过滤 | P0 | 非 admin 账号仅见授权部门 |
| 6. 表单补全（直接上级 + 头像） | `UserAdd.tsx` / `UserEdit.tsx` / `UserView.tsx` | ① 「直接上级」选人字段绑定已有 `managerId`（选人组件对齐 `ResourceBrowser.jsp` 的主账号过滤模式，`account_type=0`）；② 头像上传（对齐 Ecology 照片字段与 `check_photo` 格式校验，走既有 blade-resource 上传接口） | P0 | 创建后头像/直接上级在 `/user/info` 回读正常，消息中心 StaffRoster 头像生效 |

### P2 字段配置驱动 + 自定义字段（2-3 周，中期）

| 步骤 | 涉及文件 | 修改内容 | 依赖 | 验证方式 |
|---|---|---|---|---|
| 1. 配置表 | 新增 SQL：`blade_hrm_field_group(id, code, name, group_type:1基本/2个人/3工作, sort, status)`、`blade_hrm_field(id, group_id, prop_name, label, ele_type:input/select/date/browser…, required, sort, status, ext_json)` | 对齐 `hrm_fieldgroup` + `HrmCustomFieldByInfoType`；管理界面（可先用 SQL/简单 CRUD 页） | P0 | 配置 1 个自定义字段落库 |
| 2. 表单 schema 接口 | 新增 `GET /user/add-form-schema?tenantId=` | 返回分组 + 字段元数据（对齐 `getHrmResourceAddForm`），前端 `UserAdd` 改为 schema 驱动渲染（antd `Form` 动态生成，控件映射 input/select/date/TreeSelect） | 步骤 1 | 修改配置（增/停/必填）后表单即时变化（对齐 `CusFormSetting` 路由思想） |
| 3. 自定义字段存储 | 新表 `blade_user_ext_data(user_id, field_id, field_value)`（对齐 `cus_fielddata`；不建议仿 E9 主表预留列，避免列爆炸） | `doSubmit` 在事务内同步写扩展表；`UserView`/详情接口回显 | 步骤 2 | 自定义字段值保存并回显 |
| 4. 预留列策略决策 | 设计评审 | 明确不仿制 E9 `datefield1-5` 预留列（历史包袱），以扩展表替代——文档记录差异 | 步骤 3 | 评审纪要 |

### P3 状态流转工作流化 + 伴生初始化（远期，结合本项目 Flowable 优势）

| 步骤 | 涉及文件 | 修改内容 | 依赖 | 验证方式 |
|---|---|---|---|---|
| 1. 状态流转配置表 | 新表 `blade_person_status_flow(id, from_status, to_status, flow_key, callback_bean)` | 对齐 `hrm_state_proc_set`：转正/调动/离职等状态变更绑定 Flowable 流程（本项目已有 blade-workflow，替代 Ecology 的工作流驱动 PmAction 链路） | P0 | 配置一条「转正」流程并走通 |
| 2. 状态变更入口 | `UserEdit` 或人员详情页「办理转正/离职」按钮 | 按配置表发起对应流程；流程审批通过回调更新 `person_status`（对齐 `HrmResourceTryAction/FireAction` 的职责，由工作流回调承担） | 步骤 1 | 双账号审批后状态变化 |
| 3. 伴生初始化 | `UserServiceImpl.doSubmit` 事务内 | 新用户伴生初始化（对齐 `HrmInfoStatus` 信息完善状态）：新增 `blade_user_complete_status(user_id, item, done)` 记录基本/个人/工作/系统信息完善度；清 `USER_CACHE`（已有） | P0 | 创建后可查完善状态 |
| 4. 消息通知 | 复用本项目 blade-message 消息中心 | 用户创建成功后向直属主管（`managerId`，字段已备）发送欢迎/待办消息（对齐 Ecology 人员创建后的组织联动） | P3-3 | 双账号验证收到消息 |
| 5. 离职状态登录联动 | blade-auth 登录校验链路（`UserServiceImpl.userInfo(tenantId,account,password)` 侧） | 登录成功后校验 `person_status` 在职性（对齐 `ResourceComInfo` 在职判定 `in (0,1,2,3,5)`），离职/解聘/退休账号拒绝登录并提示「账号已停用」——否则状态机形同虚设 | P0 步骤 1 | 离职状态账号登录被拒；在职状态登录不受影响 |

---

## 6. 兼容性与风险

| 风险 | 说明 | 对策 |
|---|---|---|
| 多租户语义不可照搬 | Ecology 按分部（subcompanyid1）权限隔离；SpringBlade 按租户强隔离 | 保留租户体系；Ecology 的「分部级数据权限」映射为「租户内部门数据权限」（P1-5），接口入参 `tenantId` 仅超管可指定（现有 `TenantGuard` 机制保留） |
| 存量用户数据 | 新增列均为可空；`work_code` 允许 NULL（唯一键对 NULL 不生效，MySQL 语义） | 上线后按部门批量补录工号或启用编码规则批量生成 |
| 密码兼容 | 引入加盐/新哈希会使存量密码失效 | 方案 B（惰性升级）：登录成功且密码为旧格式时重哈希；默认先保留 `DigestUtil.encrypt` |
| 账号唯一范围 | Ecology 三重查重含管理员视图（跨表）；SpringBlade 租户内 `blade_user` 查重 | 保留租户内唯一（多租户正确语义）；若需跨租户登录名唯一加白名单配置开关 |
| `BaseEntity.status` 冲突 | 通用 status 已存在，人员状态必须独立列 | 新增 `person_status`，禁止复用 `status` |
| 字段配置误配风险 | 配置驱动后字段停用可能导致关键信息缺失 | 内置字段（account/password/realName/deptId）标记为系统级不可停用（对齐 Ecology 固定字段 + 自由字段分层） |
| 逻辑删除 × 唯一键 | `is_deleted=1` 的行仍占用 `UK(tenant_id, work_code/dept_code)`，编号无法复用 | 删除时置空编号字段（主方案）；不采用「唯一键加 is_deleted 列」（0/1 二值二次删除仍冲突） |
| 预检接口账号枚举 | `/user/check` 若无权限控制可被未授权方探测已注册账号 | 接口置于 `@PreAuth(HAS_ROLE_ADMIN)` 内 + 限流；不做匿名开放（Ecology 的 `HrmResourceCheck.jsp` 同样在登录态内） |
| 人员状态与登录脱节 | `person_status` 改为离职后若登录链路不校验，状态机形同虚设 | P3-5：blade-auth 登录成功后校验在职性。在职集合 `in (0,1,2,3,5)`，**仅 4（解聘）拒绝登录**；退休(5) 仍算在职可登录（对齐 `ResourceComInfo`，与 §5-P3-5 口径一致；本章原表述含"退休"已按此修订） |

---

## 7. 验证清单（每期完成后执行）

**接口级**（复用 `springblade-dev-login` 联调工具：真实登录拿 token → 调接口）：
1. `POST /user/submit`：正常创建 / 缺租户 / 重复账号 / 重复工号 / 重复证件号 / 非管理员越权（403）
2. `POST /user/check`：account、workCode 可用性语义
3. 创建后 `GET /user/info` 回读：新字段齐全、`person_status` 默认值正确
4. 并发生成工号压测（100 并发无重复）

**前端级**：
1. `/system/user` 新增：预检红字、自动工号、状态默认值、保存并继续新增
2. 编辑回显、查看页自定义字段回显
3. 非 admin 账号部门树裁剪

**回归级**（用户数据消费方）：
1. 登录（blade-auth）→ `/welcome` 头像/昵称正常
2. 工作流发起人填单（`Workflow/Create/Start`）读取 `currentUser.userid` 正常
3. 消息中心人员列表（StaffRoster）显示新用户
4. Excel 导入导出（`import-user`/`export-user`）与新字段兼容（`UserExcel` 需同步增加列）

---

## 附：关键文件索引

**Ecology 前端（只读参考）**
- `D:\Weaver2020\ecology\hrm\resource\HrmResourceAdd.jsp` / `HrmResourceAddNew.jsp` / `HrmResourceAddTwo.jsp` / `HrmResourceAddThree.jsp`
- `D:\Weaver2020\ecology\hrm\company\HrmResourceAdd.jsp`（部门弹窗简版）
- `D:\Weaver2020\ecology\hrm\resource\HrmResourceOperation.jsp`（提交分发）
- `D:\Weaver2020\ecology\hrm\resource\HrmResourceCheck.jsp`（唯一性预检）
- `D:\Weaver2020\ecology\hrm\resource\EditHrmCustomField.jsp`（字段配置维护）
- `D:\Weaver2020\ecology\hrm\company\DepartmentBrowser3.jsp`、`D:\Weaver2020\ecology\hrm\jobtitles\JobTitlesBrowser.jsp`

**Ecology 源码（只读参考）**
- `E:\project\ecology\ecology\src\com\api\hrm\web\HrmResourceAddAction.java`
- `E:\project\ecology\ecology\src\com\api\hrm\service\HrmResourceAddService.java`（save L809 / saveSimple L604）
- `E:\project\ecology\ecology\src\com\api\hrm\service\HrmResourceBaseService.java`（addResourceBase L2179、hrmResourceCheck L3237）
- `E:\project\ecology\ecology\src\com\api\hrm\cmd\resource\AddResourceBaseCmd.java`
- `E:\project\ecology\ecology\src\weaver\hrm\pm\action\PmAction.java`、`HrmResourceEntrantAction.java`（状态机 + 工作流驱动）
- `E:\project\ecology\ecology\src\com\weaver\procedure\hrmresourcebasicinfo\Hrmresourcebasicinfo_insert.java`（主表列清单权威来源）
- `E:\project\ecology\ecology\src\com\engine\hrm\util\CodeRuleManager.java`（工号编码规则）
- `E:\project\ecology\ecology\src\weaver\general\PasswordUtil.java`（SM3 加盐）

**SpringBlade 改造涉及**
- 前端：`ant-design-pro/src/pages/System/User/UserAdd.tsx`、`UserEdit.tsx`、`UserView.tsx`、`User.tsx`；`src/services/system/user.ts`；`src/utils/sm2.ts`
- 后端：`SpringBlade/blade-service/blade-system/src/main/java/org/springblade/system/controller/UserController.java`、`service/impl/UserServiceImpl.java`、`service/IUserService.java`
- 实体：`SpringBlade/blade-service-api/blade-user-api/src/main/java/org/springblade/system/user/entity/User.java`
- 新增（建议）：`blade-system/service/impl/WorkCodeRuleServiceImpl.java`、`blade_hrm_field_group/blade_hrm_field/blade_user_ext_data/blade_person_status_flow` 建表 SQL、`blade_code_rule` 建表 SQL

---

# 8. 追加分析：部门与分部模块（对齐 Ecology）

> 说明：Ecology 中「分部」（子公司/分支机构，表 `hrmsubcompany`）与「部门」（表 `hrmdepartment`）是**两张独立的树表**，部门通过冗余外键 `subcompanyid1` 挂到分部；SpringBlade 的 `blade_dept` 是**单表树**（`parent_id` + `ancestors`），分部与部门共用。本章还原 Ecology 两表结构，给出 Blade 对应表的追加字段方案。
> 表结构来源：ecology 源码中存储过程垫片类（`com\weaver\procedure\*`，proc 已 Java 化）、E9 组织命令类（`com\engine\hrm\cmd\organization\*Cmd`）、缓存类（`weaver\hrm\company\*ComInfo`）交叉推断；`migrationsql` 中无这两张表的 CREATE TABLE（仅有拼音列回填 UPDATE）。

## 8.1 hrmsubcompany（分部）字段清单

| 字段 | 推断类型 | 语义 | 证据（ecology 源码） |
|---|---|---|---|
| id | int PK 自增 | 主键；插入后 `select max(id)` 取回 | `com\weaver\procedure\hrmsubcompany\Hrmsubcompany_insert.java:38-42` |
| subcompanyname | varchar | 分部**简称**；同一上级下唯一（proc 内 count 校验） | `Hrmsubcompany_insert.java:20-24` |
| subcompanydesc | varchar | 分部**全称**；同一上级下唯一 | `Hrmsubcompany_insert.java:28-34` |
| companyid | int | 总部 id（指向 hrmcompany.id，代码固定 =1） | `com\engine\hrm\cmd\organization\AddSubCompanyCmd.java:79` |
| supsubcomid | int | **上级分部**（自引用树，顶级=0）；层级上限 10 级 | `AddSubCompanyCmd.java:80,89-95` |
| subcompanycode | varchar | 分部编号，可由编码规则生成，**代码层全局唯一** | `AddSubCompanyCmd.java:98-109` |
| showorder | float | 显示顺序（缓存排序 `supsubcomid asc, showorder asc`） | `weaver\hrm\company\SubCompanyComInfo.java:59-60` |
| limitUsers | int | 分部人员数上限（仅系统管理员可设） | `AddSubCompanyCmd.java:86-87,121-124` |
| canceled | char(1) | **封存标志**：'1'=封存，'0'/null=正常（≠物理删除，可解封） | `CancelSubCompanyCmd.java:124`、`ISCanceledSubCompanyCmd.java:120` |
| url | varchar | 分部主页 | `HrmSubCompanyEntity.java:17-18` |
| outkey / uuid | varchar / varchar(32) | 外部系统同步外键 / 同步 UUID | `com\engine\common\cmd\hrmsyn\SynSubCompanyCmd.java:43,65` |
| ecology_pinyin_search | varchar | 拼音搜索列（触发器维护） | `Hrmsubcompany_insert.java:37` |
| created/creater/modified/modifier | datetime/int | 审计四件套 | `weaver\hrm\common\DbFunctionUtil.java:50-52` |

## 8.2 hrmdepartment（部门）字段清单

| 字段 | 推断类型 | 语义 | 证据 |
|---|---|---|---|
| id | int PK 自增 | 主键 | `com\weaver\procedure\hrmdepartment\Hrmdepartment_insert.java:40-44` |
| departmentmark | varchar | 部门**简称**；同分部+同上级下唯一 | `Hrmdepartment_insert.java:21-28` |
| departmentname | varchar | 部门**全称**；同分部+同上级下唯一 | `Hrmdepartment_insert.java:30-36` |
| supdepid | int | **上级部门**（自引用树，顶级=0）；层级上限 10 级 | `com\engine\hrm\cmd\organization\EditDepartmentCmd.java:87,96-103` |
| allsupdepid | varchar | 全部上级链（冗余路径） | `EditDepartmentCmd.java:105` |
| **subcompanyid1** | int | **所属分部**（冗余外键 → hrmsubcompany.id）；调整上级部门时级联刷新整棵子树及人员 | `EditDepartmentCmd.java:153-155,189-204` |
| showorder | int/varchar | 显示顺序 | `Hrmdepartment_insert.java:38` |
| coadjutant | int | 部门协管人（人员 id） | `EditDepartmentCmd.java:114,131` |
| departmentcode | varchar | 部门编号；代码层全局唯一 | `EditDepartmentCmd.java:106-112,141-149` |
| canceled | char(1) | 封存标志；封存前提：无在职人员且无未封存子部门 | `CancelDepartmentCmd.java:113-133` |
| outkey / uuid / ecology_pinyin_search | 同上 | 同步外键 / UUID / 拼音列 | `com\engine\common\entity\HrmDepartmentEntity.java:92-98` |
| created/creater/modified/modifier | 审计四件套 | 同上 | `DbFunctionUtil.java:50-52` |

> 注：Ecology 中**两表均无 `depttype` 列**（全库检索确认）；部门/分部自定义字段不在主表，拆在 `hrmdepartmentdefined(deptid,…)` / `hrmsubcompanydefined(subcomid,…)`。

## 8.3 约束与关系

1. **双树 + 冗余外键**：分部自引用 `supsubcomid`；部门自引用 `supdepid`；`hrmdepartment.subcompanyid1 → hrmsubcompany.id`（1:N），人员表双向冗余 `hrmresource.departmentid / subcompanyid1`。
2. **唯一性均为代码级校验**（非 DB 约束）：分部简称/全称同上级内唯一；`subcompanycode`/`departmentcode` 全局唯一。
3. **逻辑封存 vs 物理删除**：日常走 `canceled='1'`（可解封）；物理 delete 仅在「分部下无部门 / 部门下无人员」时允许（`Hrmsubcompany_delete.java:19-26`、`DelDepartmentCmd.java:96-103`），删除人员侧还会触发 `HrmResourceShare` 权限重算。
4. **层级限制**：两棵树均 10 级上限（`HrmOrganizationUtil.ifSubComLevelEquals10 / ifDeptLevelEquals10`）。
5. **缓存**：`SubCompanyComInfo` / `DepartmentComInfo` 声明式列缓存 + `id2ParentIds/id2ChildIds` 映射；所有写操作后统一 `removeCompanyCache()` 刷新，并联动人员缓存与组织架构图标记。

## 8.4 SpringBlade `blade_dept` 现状（`blade-system-api/.../system/entity/Dept.java`）

实体仅映射：`id(ASSIGN_ID 雪花)`、`tenantId`、`parentId`、`ancestors`（祖级链，等价于 Ecology `allsupdepid` 思路）、`deptName`（≈ departmentmark/subcompanyname 简称）、`fullName`（≈ departmentname/subcompanydesc 全称）、`sort`（≈ showorder）、`remark`、`isDeleted`（`@TableLogic` 逻辑删除）。

**已天然对齐**：简称/全称（dept_name/full_name）、排序（sort）、上级链（ancestors ⊇ allsupdepid）。
**缺失**：节点类型（分部 vs 部门）、编号、所属分部冗余外键、封存语义、外部同步键、拼音搜索、部门主管/协管人、人员上限、层级数字。

## 8.5 Blade 库追加字段方案

### 方案 A（推荐）：`blade_dept` 单表扩展，以 `dept_type` 区分分部/部门

保留 Blade 单表树架构，Ecology 的「两张表」映射为「同一棵树中 `dept_type=1` 的分部节点 + `dept_type=2` 的部门节点」。改动集中、前端树组件与工作流申请人填单等既有链路不受影响。

**ALTER 语句（建议追加到 `SpringBlade/doc/sql/alter_blade_dept_org.sql`）**：

```sql
ALTER TABLE blade_dept
  ADD COLUMN dept_type      tinyint      NOT NULL DEFAULT 2 COMMENT '节点类型:1分部 2部门(对齐ecology hrmsubcompany/hrmdepartment两表模型)',
  ADD COLUMN dept_code      varchar(50)  NULL COMMENT '编号(对齐subcompanycode/departmentcode),租户内唯一,可由编码规则生成',
  ADD COLUMN subcompany_id  bigint       NULL COMMENT '所属分部节点id(对齐hrmdepartment.subcompanyid1冗余外键),仅dept_type=2且有归属时填写;自分部节点为NULL',
  ADD COLUMN canceled       tinyint      NOT NULL DEFAULT 0 COMMENT '封存标志:0正常 1封存(对齐ecology canceled char(1),语义≠is_deleted逻辑删除,可解封)',
  ADD COLUMN manager_user_id bigint      NULL COMMENT '部门主管/协管人用户id(对齐coadjutant)',
  ADD COLUMN out_key        varchar(64)  NULL COMMENT '外部系统同步外键(对齐outkey)',
  ADD COLUMN sync_uuid      varchar(32)  NULL COMMENT '同步UUID(对齐uuid)',
  ADD COLUMN pinyin         varchar(100) NULL COMMENT '拼音搜索列(对齐ecology_pinyin_search,由dept_name+full_name生成)',
  ADD COLUMN limit_users    int          NULL COMMENT '分部人员数上限(对齐limitUsers,仅超管可设)',
  ADD COLUMN dept_level     int          NULL COMMENT '层级号(对齐ecology 10级限制的派生层级,由ancestors维护)',
  ADD UNIQUE INDEX uk_blade_dept_tenant_code (tenant_id, dept_code),
  ADD INDEX idx_blade_dept_subcompany (subcompany_id),
  ADD INDEX idx_blade_dept_type (tenant_id, dept_type);
```

**追加字段说明（用途 / 类型 / 关联关系）**：

| 字段 | 数据类型 | 用途 | 关联关系 |
|---|---|---|---|
| `dept_type` | tinyint NOT NULL DEFAULT 2 | 区分分部/部门节点；分部节点承载 Ecology `hrmsubcompany` 语义，部门节点承载 `hrmdepartment` 语义 | 树内自引用：`subcompany_id` 只能指向 `dept_type=1` 的节点；列表/树接口按类型分层渲染 |
| `dept_code` | varchar(50) NULL | 组织编号，工号编码规则（P0 的 `WorkCodeRuleService`）可按部门维度取码 | `UK(tenant_id, dept_code)`；MySQL 唯一键对 NULL 不生效，未编码节点不受影响；`blade_user.work_code` 的生成入参之一 |
| `subcompany_id` | bigint NULL | 部门→分部冗余外键（对齐 `subcompanyid1`）：免跨层级遍历即可取分部，供数据权限/报表按分部聚合 | `→ blade_dept.id (dept_type=1)`；调整上级部门时按 Ecology 语义级联刷新子树；与 `blade_user.dept_id`(CSV) 共同决定用户分部归属 |
| `canceled` | tinyint NOT NULL DEFAULT 0 | 封存/解封（对齐 Ecology 日常封存语义）；与 `is_deleted` 并存：封存=暂时停用可见性，删除=逻辑删除 | 封存前置校验：`blade_user.dept_id` 无在职人员 + 无未封存子节点（对齐 `CancelDepartmentCmd`）；封存节点在用户创建页部门树中不可选 |
| `manager_user_id` | bigint NULL | 部门主管/协管人（对齐 `coadjutant`） | `→ blade_user.id`；供审批流找部门负责人、消息中心按部门推送 |
| `out_key` / `sync_uuid` | varchar(64)/varchar(32) | 外部系统（OA 同步、集团主数据）对接幂等键（对齐 `outkey/uuid`） | 组织同步 Cmd 按 out_key/uuid 定位更新而非重复插入 |
| `pinyin` | varchar(100) NULL | 拼音/首字母搜索 | 由 `dept_name + full_name` 生成，保存时维护 |
| `limit_users` | int NULL | 分部人员配额（对齐 `limitUsers`） | 与 `blade_user.dept_id` 计数比对，超限阻止创建（仅超管可改） |
| `dept_level` | int NULL | 显式层级号 | 由 `ancestors` 推导维护，实现 10 级上限校验（对齐 `ifDeptLevelEquals10`） |

**两条必须写死的规则**：

1. **逻辑删除 × 唯一键**：`blade_dept` 与 `blade_user` 同为逻辑删除（`is_deleted`），`UK(tenant_id, dept_code)` / `UK(tenant_id, work_code)` 会**阻止已删组织/用户的编号复用**——删除时必须将 `dept_code` / `work_code` 置 NULL（或追加 `_del_{id}` 后缀）。不要用「唯一键加 is_deleted 列」的方案：0/1 二值在第二次删除时仍冲突。
2. **部门→分部归属推导**：`blade_user.dept_id` 为 CSV 多部门，用户分部归属 = **首个部门的 `subcompany_id`**（前端须把主部门排在首位）；`blade_dept.subcompany_id` 在部门调整上级时按 Ecology 语义级联重算整棵子树（对齐 `EditDepartmentCmd.java:189-204`）。

### 方案 B（备选）：拆表对齐 Ecology 双表模型

新增 `blade_subcompany`（分部表）并给 `blade_dept` 加 `subcompany_id` 外键。优点：与 Ecology 表模型一一对应、分部可挂独立扩展信息；缺点：`blade_dept` 树、`blade_user.dept_id`、工作流部门取值、前端部门树组件全部需要双表适配，回归面大。**除非后续有「分部需要独立于组织树的大量专属属性」，否则不建议**。

### 两个方案共同的配套改造

| 项 | 内容 | 对齐点 |
|---|---|---|
| 唯一性校验服务化 | `DeptServiceImpl.submit` 内增加：同父下 `deptName` 唯一、`deptCode` 租户内唯一（对齐 Ecology proc 内 flag=2/3 校验） | `Hrmsubcompany_insert.java:20-34` |
| 封存/解封接口 | `POST /dept/cancel`、`POST /dept/is-canceled`（含前置校验：无在职人员、无未封存子节点），物理删除仍走 `/remove` | `CancelDepartmentCmd` / `ISCanceledDepartmentCmd` |
| 层级上限 | 保存时按 `ancestors` 计算 ≤10 级 | `ifSubComLevelEquals10/ifDeptLevelEquals10` |
| 缓存/联动 | 组织变更后清 `blade_user` 相关缓存并刷新前端树（对齐 `removeCompanyCache` + `ResourceComInfo` 联动） | `SubCompanyComInfo/DepartmentComInfo` |
| 前端 | 部门管理页增加类型/编号/封存操作；用户创建页部门树按 `canceled=0` 过滤、分部节点分组展示 | `HrmDepartmentAdd.jsp` 系列 |

## 8.6 分期与验证

| 期 | 内容 | 验证 |
|---|---|---|
| 第 1 期 | 方案 A 的 `ALTER` + 实体 `Dept.java` 字段补齐 + 保存唯一性校验 + 封存/解封接口 | SQL 执行成功；重复编号被拦截；封存后用户创建页部门树不可选、解封恢复 |
| 第 2 期 | 与用户创建 P0 联动：工号编码规则按部门取码；`limit_users` 配额拦截；`manager_user_id` 用于工作流部门负责人寻址 | 创建用户时按部门生成工号；超配额阻止；审批流按部门主管路由 |
| 第 3 期 | 拼音搜索、外部同步键（out_key/uuid）与集团主数据对接 | 拼音模糊搜索生效；同步接口按 uuid 幂等 |

---

# 9. 追加分析：人员表（blade_user）字段清单（对齐 hrmresource）

> 权威字段来源：`E:\project\ecology\ecology\src\com\weaver\procedure\hrmresourcebasicinfo\Hrmresourcebasicinfo_insert.java` 的 INSERT 列清单（29 列）+ 系统信息 proc（loginid/password/salt 相关列）。下列映射均给出 Ecology 侧字段名。

## 9.1 已对齐字段（无需追加）

| Ecology（hrmresource） | blade_user 现有字段 | 说明 |
|---|---|---|
| loginid（登录名） | `account` | 唯一性范围差异见 §4 差异点 13 |
| lastname（姓名） | `realName` | Ecology 另有 firstname，本项目合并为 realName |
| sex / birthday / email / mobile | `sex` / `birthday` / `email` / `phone` | 直对应 |
| departmentid | `deptId`（CSV，多部门） | Blade 特色：一人可属多部门 |
| jobtitle（岗位） | `postId` | 对应岗位表 |
| managerid（直接上级） | `managerId` | **实体已存在**（`@TableField("manager_id")`），仅前端创建表单未暴露（P1 补充） |
| resourceimageid（照片） | `avatar` | 直对应 |
| id | `id`（ASSIGN_ID 雪花） | 保留现状，不仿 SequenceIndex |
| createrid/createdate/lastmodid/lastmoddate | BaseEntity 审计字段 | 直对应（`HrmResource_CreateInfo` 的职责已由 MP 自动填充承担） |

## 9.2 必须追加（P0，随用户创建改造一起上）

| blade_user 新字段 | 数据类型 | 对齐 Ecology | 用途 | 关联关系 | 约束 |
|---|---|---|---|---|---|
| `work_code` | varchar(50) NULL | `workcode`（工号） | 员工工号，人事/报表/考勤对接主键；可由编码规则自动生成 | `UK(tenant_id, work_code)`；与 `blade_dept.dept_code` 共用 `blade_code_rule` 规则体系（按部门取码） | 同租户唯一；允许 NULL（存量数据不强制） |
| `person_status` | tinyint NOT NULL DEFAULT 1 | hrmresource.`status`（经典枚举：0 试用 / 1 正式 / 2 临时 / 3 延期 / 4 解聘 / 5 退休；在职判定 = `in (0,1,2,3,5)`，来自 `ResourceComInfo`） | 人员状态机；驱动工作流转正/离职流程 | 状态流转配置表 `blade_person_status_flow`（P3）；新建默认"正式"（对齐 `AddResourceBaseCmd` 默认 status=1） | 禁止复用 BaseEntity.status（通用标记已占用）；⚠️ `PmAction` 的 0-8 是流程场景类型而非字段值 |
| `certificate_num` | varchar(30) NULL | `certificatenum`（证件号） | 身份证/证件唯一标识 | 业务唯一（仅主账号范围）：应用层查重（对齐 `HrmResourceAddService#save` L891） | 无 DB 唯一键（次账号/空值语义复杂，先应用层校验） |
| `salt` | varchar(64) NULL | 密码加盐（`PasswordUtil.encrypt` 返回 [密文,salt]） | 密码盐值 | 与 `password` 配套；仅当密码算法切换为加盐方案时启用（P1 方案 B 惰性升级） | 可空（存量用户为旧格式） |

配套实体改动：`blade-service-api/blade-user-api/.../user/entity/User.java` 增加上述 4 字段；`UserWrapper`/`UserExcel`（导入导出）同步补列。

## 9.3 建议追加（P1/P2，按需）

| blade_user 新字段 | 数据类型 | 对齐 Ecology | 用途 | 说明 |
|---|---|---|---|---|
| `account_type` | tinyint DEFAULT 0 | `accounttype`（0 主账号，1 次账号） | 主/次账号体系 | 次账号登录名自动生成 `主账号+(id+1)`（对齐 BaseService L2256-2271）；次账号不可登录后台管理 |
| `belong_to` | bigint NULL | `belongto` | 次账号归属的主账号 id | `→ blade_user.id (account_type=0)`；消息中心/工作流按主账号聚合 |
| `job_level` | int NULL | `joblevel`（职级） | 职级 | 与 `post_id` 岗位互补 |
| `job_call` | bigint NULL | `jobcall`（职务类别，关联 jobcall 浏览框） | 职务类别字典 | 可用字典表替代独立外键 |
| `location_id` | bigint NULL | `locationid`（办公地点） | 办公地点 | 关联地点字典 |
| `work_room` | varchar(50) NULL | `workroom`（办公室） | 办公室/工位 | — |
| `telephone` / `fax` | varchar(30) | `telephone` / `fax` | 座机/传真 | 手机已有（`phone`） |
| `sec_level` | int DEFAULT 0 | `seclevel`（安全级别） | 信息安全等级 | 影响可见范围（Ecology 中涉密过滤），按需启用 |
| `mobile_show_type` | tinyint | `mobileshowtype` | 手机号对外展示方式 | 隐私控制，可选 |
| `work_start_date` | date | 入职相关（Ecology 由工作流日期字段承载） | 入职日期 | 状态机（转正/工龄）依赖，建议 P3 随状态流转一并加 |

## 9.4 明确不迁移的 Ecology 字段（及理由）

| Ecology 字段 | 不迁移理由 |
|---|---|
| `subcompanyid1`（人员上的分部冗余） | SpringBlade 用 `dept_id` → `blade_dept.subcompany_id`（第 8.5 章新增列）链式推导分部，避免双写不一致 |
| `datefield1-5 / numberfield1-5 / textfield1-5 / tinyintfield1-5` 自定义预留列 | 列爆炸反模式；用扩展表 `blade_user_ext_data`（P2，对齐 `cus_fielddata`） |
| `passwordstate / needusb / needdynapass / lloginid / passwdchgdate` | USB-Key/动态口令等安全策略，本项目无对应设施；登录失败锁定已由 blade-redis 计数实现 |
| `systemlanguage` | 前端 i18n 已由 umi-plugin-locale 承载 |
| `managerstr`（主管链冗余） | 可由 `manager_id` 链式推导，避免冗余维护 |
| `canceled`（人员注销走 status=5/6/7） | 不需要独立列，人员状态机已覆盖 |
| `SequenceIndex` 主键序列 | 雪花 ID（分布式友好，保留现状） |

## 9.5 与主计划的对应关系

- 第 9.2 节 4 个字段 = §5 P0 步骤 1 的 ALTER 内容（本节补充了字段级用途/关联说明）；
- 第 9.3 节字段按 P1（account_type/belong_to、默认密码相关）与 P2（职级/办公信息）拆入主计划分期；
- 实体/Wrapper/Excel 同步改动文件与 P0 步骤 2、§7 验证清单（导入导出兼容）一致。

---

# 10. 数据迁移方案：Ecology → SpringBlade（人员/部门/分部）

> ⚠️ **执行时点：本方案在 P0 与第 8 章改造（`blade_user`/`blade_dept` 追加列）完成并上线后执行**。
> 现状勘察（2026-10-07）：① 项目内已有 ecology 只读数据源先例 `blade-formmode/.../EcologyDataSourceConfig.java`（SQL Server `192.168.1.5:1433/ecology2020_demo`，sa；该文件注释提醒不可注册为 DataSource Bean，避免抢占 MyBatis 主库绑定）；② 本机已具备 JDK 21（`D:\project\weaver\jdk`，支持单文件源码运行）与驱动 `mssql-jdbc-12.8.1.jre11.jar`、`mysql-connector-j-9.7.0.jar`（本地 Maven 仓库）；③ blade 主库（MySQL）连接信息存于 Nacos `blade-dev.yaml`，Nacos 为 3.x（v1/v2 API 404、v3 admin 403，控制台 8080），**执行时**改由服务本地配置或运维提供凭据。

## 10.1 迁移范围与总体映射

| 源表（ecology, SQL Server） | 目标表（blade, MySQL） | 目标 ID 规则 | 备注 |
|---|---|---|---|
| `hrmsubcompany`（分部） | `blade_dept`（`dept_type=1`） | `dst_id = 1_000_000_000 + src_id` | `supsubcomid=0 → parent_id=0` |
| `hrmdepartment`（部门） | `blade_dept`（`dept_type=2`） | `dst_id = 2_000_000_000 + src_id` | 顶级部门（`supdepid=0`）挂到其分部节点下；其余挂 `2e9+supdepid`；`subcompany_id = 1e9+subcompanyid1` |
| `hrmresource`（人员） | `blade_user` | **保留源 id 原值**（小整数，与雪花 ID 无冲突；`manager_id` 引用天然自洽） | 租户统一 `'000000'` |

偏移量常量（`1e9` / `2e9`）远离雪花 ID 量级（~1e18）与既有业务数据，且分部/部门分命名空间避免主键互撞。

## 10.2 字段映射与转换规则

**分部 → blade_dept**：

| 源字段 | 目标字段 | 转换 |
|---|---|---|
| id | id | `1e9 + id` |
| subcompanyname / subcompanydesc | dept_name / full_name | 简称/全称直拷 |
| subcompanycode | dept_code | 空则 NULL；库内重复时首个保留、后续置 NULL（迁移日志记录） |
| supsubcomid | parent_id | `0 → 0`；否则 `1e9 + supsubcomid` |
| showorder | sort | 直拷 |
| canceled | canceled | `'1'→1，否则 0`（封存节点在用户创建页不可选） |
| — | ancestors | 二次遍历按父链计算（根为 `0`） |
| — | tenant_id / is_deleted / status | `'000000'` / 0 / 1 |

**部门 → blade_dept**：同上，另有 `subcompanyid1 → subcompany_id(1e9+`值`)`、`supdepid → parent_id`、`departmentmark → dept_name`、`departmentname → full_name`、`departmentcode → dept_code`、`canceled → canceled`。

**人员 → blade_user**：

| 源字段（hrmresource） | 目标字段 | 转换 |
|---|---|---|
| id | id | 原值保留 |
| loginid | account | **预检**：与既有 `blade_user.account`（如 admin）或本批已迁移账号重复 → 该行跳过并记日志 |
| lastname | real_name / name | 姓名同填昵称 |
| workcode | work_code | 空则 NULL；库内重复首个保留、后续置 NULL（迁移日志） |
| status | person_status | 经典枚举直拷（0试用/1正式/2临时/3延期/4解聘/5退休） |
| certificatenum | certificate_num | 直拷；空忽略 |
| departmentid | dept_id | `2e9 + departmentid`（单值；Blade CSV 语义下即"主部门"） |
| managerid | manager_id | `→ blade_user.id`（本批映射后自洽；找不到映射则 NULL） |
| mobile / email / sex / birthday | phone / email / sex / birthday | birthday 取日期部分；sex NULL→0 |
| password | password | **不可迁移**（Ecology 哈希算法/盐未知）：统一取 admin 当前密码哈希（`SELECT password FROM blade_user WHERE account='admin' LIMIT 1`），迁移完成后由管理员重置；此决策写入迁移报告 |
| — | tenant_id / status / is_deleted | `'000000'` / 1 / 0 |
| — | work_code 特例 | `accounttype=1`（次账号）行跳过账号迁移或 work_code 置 NULL（次账号无独立工号） |

## 10.3 迁移工具设计

- **形态**：单文件 JDBC 程序 `SpringBlade/doc/tools/EcologyOrgMigration.java`，零框架依赖，JDK 21 源码模式运行：
  `java -cp "mssql-jdbc-12.8.1.jre11.jar;mysql-connector-j-9.7.0.jar" EcologyOrgMigration.java [--dry-run]`
- **连接**：ecology 侧默认复用 `EcologyDataSourceConfig` 的参数；blade 侧 URL/账号/密码从命令行参数或环境变量读取（改造后可从服务配置导出）。
- **执行步骤**：
  1. `--dry-run` 仅打印统计不写库；
  2. 幂等 DDL：查 `information_schema.columns`，逐列补齐 §5-P0 与 §8.5 的 `blade_user`/`blade_dept` 追加列与索引（已存在则跳过）——即使改造 SQL 未手工执行也能自愈；
  3. 迁移分部 → 部门 → 人员（顺序保证父节点先落库）；
  4. 二次遍历计算 `ancestors`；
  5. 输出迁移报告：各表 insert/update/skip 计数 + 跳过明细（重复 loginid/workcode/缺失引用）。
- **幂等**：`blade_dept`/`blade_user` 均带 `ecology_id bigint NULL` 追溯列（DDL 随步骤 2 创建）+ `ON DUPLICATE KEY UPDATE`；重跑=覆盖刷新，不会重复插入。
- **回滚**：迁移前 `mysqldump` 备份 `blade_user`/`blade_dept` 两表；回滚 = 还原备份 + 可选 `DELETE WHERE ecology_id IS NOT NULL`。

## 10.4 执行前置检查清单（改造完成后逐项打勾）

1. ☐ P0 的 `blade_user` 追加列已生效（或确认由迁移工具自愈 DDL）
2. ☐ 第 8 章 `blade_dept` 追加列已生效
3. ☐ `mysqldump` 备份 `blade_user`、`blade_dept`
4. ☐ ecology 库可达（`192.168.1.5:1433`，注意 `weaver.properties` 写的 127.0.0.1 是部署机本机视角）
5. ☐ blade MySQL 连接信息（URL/账号/密码）已获取
6. ☐ 已与业务确认：ecology 人员密码不作迁移、统一重置
7. ☐ `--dry-run` 报告审阅通过（尤其 skip 清单）

## 10.5 迁移后验证清单

| 验证项 | 方式 |
|---|---|
| 计数核对 | 源 `select count(*) from hrmsubcompany/hrmdepartment/hrmresource` vs 目标 `select count(*) from blade_dept where ecology_id is not null`（分部/部门分开数）/ `blade_user where ecology_id is not null` |
| 树完整性 | 抽查 3 个分部+部门：`parent_id`、`ancestors`、`subcompany_id` 链正确；无孤儿节点（parent 指向不存在的 id） |
| 用户抽测 | 任取 3 个迁移用户：真实登录（重置后密码）、`/user/info` 回读 work_code/person_status/dept_id、消息中心 StaffRoster 可见 |
| 组织页抽测 | `/system` 部门树分部/部门分层渲染；封存节点不可选（`canceled=1`） |
| 幂等抽测 | 重跑迁移程序：计数不变、无重复行 |
| 既有数据回归 | admin 等既有用户（`ecology_id IS NULL`）不受影响；登录/工作流/消息中心回归 §7 清单 |

---

## 11. P1 实施记录（2026-10-07）

> 范围：P1 表单交互与密码策略（§5 表内 6 步骤）。不含第 10 章 Ecology 数据迁移（保持另行安排）。

### 11.1 已实现改动

**后端（blade-system，已 `mvn -pl blade-service/blade-system -am compile` 通过，MVN-EXIT=0）**

| 文件 | 改动 |
|---|---|
| `service/impl/UserServiceImpl.java` | ① 去掉 `@AllArgsConstructor`，改为显式 `@Autowired` 构造器（避免 `@Value` 配置字段被 Lombok 拉入构造器）；② 新增 `@Value("${blade.auth.private-key:}") sm2PrivateKey` 与 `@Value("${blade.default-password:123456}") defaultPassword`；③ 新增 `resolvePassword(raw)`：完全对齐 blade-auth `TokenUtil.decryptPassword`——SM2 密文（前端 sm-crypto 产出，**不带 `04` 前缀**）解密前补齐 `04` 再 `SM2Util.decrypt`，解密失败回退明文（兼容 Excel 导入/旧链路）；④ `doSubmit`/`updatePassword` 的 password 经 `resolvePassword` 后再 `DigestUtil.encrypt` 存储；⑤ `resetPassword`、`importUser` 改用可配置 `defaultPassword` |
| `service/impl/DeptServiceImpl.java` + `IDeptService.java` | 新增 `treeScope(tenantId)`：超管返回全量；非超管裁剪为「当前用户所在部门（取 `dept_id` 首段）+ 其下属 + 祖先链」子树（对齐 Ecology `DepartmentBrowser3` 的 `rightLevel` 数据权限裁剪） |
| `controller/DeptController.java` | 新增 `GET /blade-system/dept/tree-scope`（供用户表单部门选择器按可见范围裁剪） |
| `doc/nacos/blade.yaml` | `blade:` 下新增 `default-password: "123456"`（可配置默认密码，重置/导入使用） |

**前端（ant-design-pro，lint 0 错误）**

| 文件 | 改动 |
|---|---|
| `src/pages/System/User/UserAdd.tsx` | ① 密码提交经 `Crypto.encryptPassword`（SM2，复用登录加密链路，`@/utils/crypto`）；② 新增「保存并继续新增」按钮（`doSubmit(true)`：提交后清空密码、保留租户/部门、停留页面，`onSaved` 刷新列表不关闭）；③ 新增「直接上级」远程搜索选人（`userApi.list`，对齐 Ecology `ResourceBrowser` 主账号过滤）；④ 新增「头像」上传（`ImageUploader`，`/api/blade-resource/oss/endpoint/put-file`）；⑤ 部门树改用 `deptApi.treeScope({})` 按数据权限裁剪；⑥ 提交体带上 `managerId`、`avatar` |
| `src/pages/System/User/UserEdit.tsx` | 同步补全：直接上级选人、头像上传、部门树按权限裁剪、`managerId`/`avatar` 回显与提交 |
| `src/pages/System/User/User.tsx` | `UserAdd` 传入 `onSaved={() => refresh()}`，支撑「保存并继续新增」刷新 |
| `src/services/system/dept.ts` | 新增 `treeScope()` → `/blade-system/dept/tree-scope` |

### 11.2 验证状态

- ✅ 后端编译通过；前端 lint 0 错误。
- ✅ **接口级验证已完成（2026-10-08，`doc/tools/verify-p1.cjs`，实测输出）**：

  | # | 验证项 | 实测结果 |
  |---|---|---|
  | 0 | admin 登录（SM2） | `LOGIN-OK` |
  | 1 | `GET /dept/tree-scope`（部门树数据权限裁剪） | `200`，返回 2 个节点 ✅ 接口已部署 |
  | 2 | `POST /user/submit` 密码经 SM2 加密提交 | `200 操作成功`；回读 `personStatus=1`、`workCode=EMP202610080013`（自动生成） |
  | 3 | **用明文密码登录新建账号**（证明后端解密 + Digest 与登录链路一致） | `200 LOGIN-OK` ✅ 关键回归通过 |
  | 4 | `POST /user/reset-password` → 用 `blade.default-password`(123456) 登录 | `200 LOGIN-OK` ✅ 可配置默认密码生效 |
  | 5 | 清理测试用户 | `200` |

- ⏳ **仍需浏览器验证（前端交互，接口层无法覆盖）**：
  1. 「保存并继续新增」按钮：提交后停留在新增页、列表刷新；
  2. 直接上级下拉可搜索选人；头像可上传并在查看/编辑回显；
  3. 非 admin 账号打开新增：部门树仅见授权部门子树。

### 11.3 遗留与说明

- **MSG-DIAG 诊断日志**：属此前消息中心改造（app.tsx / FloatingMessageBox），不在本 P1 范围内，暂不清理，避免跨功能回归。
- **P2（字段配置驱动）/ P3（状态流转工作流化）/ 第 10 章迁移**：未执行，保持原计划分期。
- **代理/Feign 惰性升级**：P0 已留 `person_status` 等字段；本 P1 的盐值惰性升级因 `salt` 字段已在 P0 入库但当前 `DigestUtil.encrypt` 未实际消费，本着"先不破坏存量登录"的保守原则，本期仅完成"加密传输 + 可配置默认密码 + 部门树权限"三块；若需真正启用 salt 散列，需单独评估存量密码重哈希，列入 P2/P3 或专项。

---

## 12. P0-8 收尾：Excel 导入导出与新字段对齐（2026-10-08）

> 背景：§9 状态行遗留提醒「Excel 导入导出列兼容回归（§7）」。核查发现 P0 只改了 `UserExcel` 实体，**导出 SQL 与导入链路未同步**，实际存在两处缺陷，本次一并补齐。

### 12.1 缺陷与修复

| # | 缺陷 | 修复 | 文件 |
|---|---|---|---|
| 1 | `exportUser` 查询未选 `work_code`/`person_status` → 导出的 Excel 中工号/人员状态**恒为空**（实体列已加但无数据来源） | SQL 补两列；**证件号属敏感信息，明确不导出** | `blade-system/.../mapper/UserMapper.xml:102-105` |
| 2 | 导入为「整批一条事务、首错即炸」：单行工号/账号重复会导致整批失败，且**无法定位到行**（不符合 P0-8「明确报错到行」） | ① `UserImportListener` 回填 Excel 物理行号（`readRowHolder().getRowIndex()+1`，`@ExcelIgnore` 不入列）；② `importUser` 改为**逐行独立提交 + try/catch**，失败收集为「第 N 行（账号：x）：原因」；③ 接口返回 `R.data(errors, "导入完成，失败 N 行")`，全成功仍返回「操作成功」 | `excel/UserExcel.java`、`excel/UserImportListener.java`、`service/IUserService.java`、`service/impl/UserServiceImpl.java`、`controller/UserController.java` |
| 3 | 导入时 `personStatus` 缺省依赖 DB 默认值，口径隐式 | 显式兜底为「正式」=1（对齐 `AddResourceBaseCmd` 默认 status=1） | `service/impl/UserServiceImpl.java` |

### 12.2 关键设计说明

- **导入复用 `submit` 校验链**：工号/账号/证件号查重对导入同样生效（对齐 Ecology 批量建档同源校验），逐行失败不影响其余行——语义从「全或无」改为「部分成功 + 行级明细」。
- **`rowNum` 用 `@ExcelIgnore` 标记**：仅用于报错定位，不参与导入/导出列映射，避免污染模板。
- **不导出证件号**：导出文件易外泄，`certificate_num` 不进 `UserExcel`（导入如需支持可后续按权限评估）。
- 编译状态：`mvn -o -pl blade-service/blade-system -am compile` **通过（EXIT=0）**。

### 12.3 待回归（需从工作副本重打包重启 blade-system 后）

1. `GET /user/export-user`：导出的 xlsx 中 `workCode`、`personStatus` 列有值；
2. `POST /user/import-user`：导入含**重复工号**的文件 → 返回 `data` 中含「第 N 行…当前工号已被使用!」，且其余行正常入库；
3. 导入 `personStatus` 留空的行 → 落库为 1（正式）。

---

## 13. P2 实施记录：字段配置驱动 + 自定义字段（2026-10-08）

> 范围：§5-P2 四步。不含 P3 与第 10 章迁移。

### 13.1 数据模型（P2-1）

建表脚本 `doc/sql/alter_blade_user_hrm_field.sql`，**已在 dev 库执行成功（5/5）**（执行器 `doc/tools/RunSql.java`）：

| 表 | 对齐 Ecology | 作用 |
|---|---|---|
| `blade_hrm_field_group` | `hrm_fieldgroup` | 字段分组（group_type：1 基本 / 2 个人 / 3 工作） |
| `blade_hrm_field` | `HrmCustomFieldByInfoType` | 字段元数据（prop_name/label/ele_type/required/status/ext_json） |
| `blade_user_ext_data` | `cus_fielddata` | 自定义字段值（UK(tenant_id,user_id,field_id)） |

### 13.2 后端改动

| 文件 | 改动 |
|---|---|
| `entity/HrmFieldGroup.java`、`entity/HrmField.java`、`entity/UserExtData.java` | 新增三实体（继承 `TenantEntity`，雪花 ID） |
| `mapper/HrmFieldGroupMapper.java`、`HrmFieldMapper.java`、`UserExtDataMapper.java` | 新增 Mapper |
| `service/IHrmFieldService.java` + `impl/HrmFieldServiceImpl.java` | `formSchema(tenantId)`：按分组返回「启用中」的字段元数据（对齐 `getHrmResourceAddForm`）；租户口径与 `selectPage` 一致（仅超管可指定） |
| `service/IUserExtDataService.java` + `impl/UserExtDataServiceImpl.java` | `getExtData(userId)`；`saveExtData(userId, values)` 采用 **upsert（存在更新 / 不存在插入）**，不做删除——规避 `UK(tenant_id,user_id,field_id)` 与逻辑删除冲突（§6 风险表第 7 行同源问题） |
| `vo/UserFormSchemaVO.java` | schema 出参（分组 + FieldItem） |
| `controller/UserController.java` | 新增 `GET /user/add-form-schema`、`GET /user/ext-data`（均在 `@PreAuth(HAS_ROLE_ADMIN)` 内） |
| `service/IUserService.java` + `impl/UserServiceImpl.java` | 新增 `formSchema` / `getExtData`；**`doSubmit` 内主表保存后同步写扩展表**（与主表同事务，对齐 Ecology 主表 + `cus_fielddata` 双路落库） |
| `blade-user-api/.../user/entity/User.java` | 新增 `@TableField(exist=false) Map<Long,String> extData`（非持久化列，提交用） |

### 13.3 前端改动

| 文件 | 改动 |
|---|---|
| `services/system/user.ts` | 新增 `addFormSchema()`、`extData()`、`normalizeFormSchema()` 与 `FormSchemaGroup/Field` 类型 |
| `pages/System/User/UserExtFields.tsx`（新增） | 配置驱动渲染组件：按 `eleType` 映射 input/textarea/number/date/select（select 的 options 取自 `extJson`），字段名为 `['extData', fieldId]` 嵌套路径；导出 `normalizeExtData()`（moment → `YYYY-MM-DD`，空值剔除） |
| `UserAdd.tsx` / `UserEdit.tsx` | 拉取 schema 并在「职责信息」后渲染自定义字段分组；提交体带 `extData`；编辑态用 `/user/ext-data` 回显（日期字段转 moment）；「保存并继续新增」清空自定义字段 |
| `UserView.tsx` | 追加自定义字段的 `Descriptions.Item` 回显 |

### 13.4 设计决策（P2-4）

1. **不仿制 E9 主表预留列**（`datefield1-5/numberfield1-5/...`）：列爆炸反模式，一律走扩展表 `blade_user_ext_data`。
2. **内置字段不进配置表**：account/password/realName/deptId 等保持前端固定渲染，等价于「系统级不可停用」，规避 §6「字段配置误配导致关键信息缺失」的风险（对齐 Ecology 固定字段 + 自由字段分层）。
3. **扩展表 upsert 而非 delete+insert**：避免逻辑删除行与唯一键冲突。

### 13.5 验证状态

- ✅ 后端 `mvn -o -pl blade-service/blade-system -am compile` **EXIT=0**；前端 `tsc --noEmit` 本次改动路径**无错误**。
- ✅ 建表脚本 dev 库执行成功（含 1 条示例分组「工作信息」+ 示例字段「办公地点」）。
- ⏳ **待重打包重启 blade-system 后联调**（Nacos 未启动期间直连 8106 验证）：
  1. `GET /user/add-form-schema?tenantId=000000` 返回 1 个分组、1 个字段（办公地点，select 带 3 个 options）；
  2. `POST /user/submit` 带 `extData:{"<fieldId>":"总部A座"}` → `GET /user/ext-data?userId=` 回读一致；
  3. 把字段配置 `status` 改为 0 → 新增页该字段消失（验证「改配置即改表单」）。

---

## 14. P3 实施记录：状态流转工作流化 + 伴生初始化（2026-10-08）

> 范围：§5-P3 五步全量。**不含**第 10 章数据迁移（用户选择暂缓）。

### 14.1 数据模型（P3-1）

`doc/sql/alter_blade_user_person_status_flow.sql`，**已在 dev 库执行成功（5/5）**：

| 表 | 对齐 Ecology | 关键点 |
|---|---|---|
| `blade_person_status_flow` | `hrm_state_proc_set` | `UK(tenant_id, from_status, to_status)`；`flow_key` 为空即直改；已初始化「转正 0→1」「离职 1→4」两条 |
| `blade_person_status_flow_record` | — | `UK(instance_id)`（MySQL 对 NULL 不生效，直改可多行）；`mode` 1审批/2直改；`flow_status` 0审批中/1通过/2驳回 |
| `blade_user_complete_status` | `HrmInfoStatus` | `UK(tenant_id, user_id, item)`；四类 BASE/PERSON/WORK/SYSTEM |

### 14.2 后端改动

| 模块 | 文件 | 改动 |
|---|---|---|
| blade-system | `enums/PersonStatusEnum.java`（新增） | 状态枚举 + `loginAllowed()`：**仅 4 解聘拒绝**，null 按在职放行 |
| | `entity/PersonStatusFlow.java`、`PersonStatusFlowRecord.java`、`UserCompleteStatus.java`（新增） | 三实体 |
| | `mapper/*Mapper.java`（新增 3 个） | BaseMapper |
| | `dto/StatusFlowStartDTO.java`、`dto/StatusFlowCallbackDTO.java`（新增） | 发起/回调入参 |
| | `service/IPersonStatusFlowService.java` + impl（新增） | `start`（命中配置走审批、未配置降级直改）、`callback`（按 instanceId 反查、**幂等**）、`availableFlows`、`records` |
| | `service/IUserCompleteStatusService.java` + impl（新增） | 伴生初始化（幂等）、查询、标记已完善 |
| | `service/impl/UserServiceImpl.java` | `doSubmit` 内：新建时写完善度（同事务）+ 注册 **afterCommit** 回调发主管消息（事务外、失败仅记日志） |
| | `controller/UserStatusFlowController.java`（新增） | `POST /user/status-flow/start`、`GET /user/status-flow/available`、`GET /user/status-flow/records`，`@PreAuth(HAS_ROLE_ADMIN)` |
| | `controller/UserController.java` | 新增 `GET /user/complete-status` |
| | `feign/UserStatusFlowClient.java`（新增） | 内部回调端点 `POST /user/status-flow/callback`，沿用 `UserClient` 的既有约定（内部 Feign 端点不加 `@PreAuth`） |
| | `pom.xml` | 新增 `blade-workflow-api`、`blade-message-api` 依赖 |
| blade-user-api | `feign/IUserStatusFlowClient.java` + Fallback（新增） | 供 blade-workflow 回调的 Feign 契约 |
| blade-message-api | `feign/IMessageClient.java` + Fallback | 新增 `createSession(SessionCreateDTO)`（`MessageSendDTO.sessionId` 事实必填，且服务端「两人会话幂等复用」） |
| blade-workflow | `listener/WfBizCallbackListener.java`（新增） | 全局 `FlowableEventListener`，`PROCESS_COMPLETED` → 按 `wf_instance.title` 前缀 `PSF:` 粗筛 → Feign 回调 blade-system |
| | `config/FlowableConfig.java` | 与台账监听**并列**注册（独立开关 `blade.workflow.biz-callback.enabled`，默认 true） |
| blade-auth | `utils/PersonStatusGuard.java`（新增） | 在职判定单点收敛：`loginAllowed()` + `check()`（抛 `ServiceException("账号已停用")` → 经 `BladeRestExceptionTranslator` 转 400） |
| | `granter/PasswordTokenGranter.java`、`CaptchaTokenGranter.java`、`RefreshTokenGranter.java` | 三处统一调用 `PersonStatusGuard.check()` |

### 14.3 前端改动

| 文件 | 改动 |
|---|---|
| `services/system/user.ts` | 新增 `startStatusFlow` / `statusFlowAvailable` / `statusFlowRecords` / `completeStatus` + `PersonStatusFlow`、`PersonStatusFlowRecord` 类型 + `COMPLETE_STATUS_TEXT` |
| `pages/System/User/UserStatusFlowModal.tsx`（新增） | 办理状态变更弹窗：当前状态（只读）→ 变更为（单选卡片，带「需审批/直接变更」角标）→ 办理说明（必填）→ 流转进度 Timeline |
| `pages/System/User/User.tsx` | 工具栏「办理状态变更」（仅选中单行可用）+ 行内「办理」按钮；人员状态 Tag 配色按在职口径调整（解聘 4 红色） |
| `pages/System/User/UserView.tsx` | 详情追加「信息完善度」区块（环形进度 + 四类 Tag） |

### 14.4 设计决策

1. **审批完成回调：新增独立监听器，不复用既有影子监听器。** 勘察确认 `WfEngineEventListener → WfStateProjector` 是「方案C 影子模式」：`@ConditionalOnProperty(ledger-listener.enabled, matchIfMissing=false)` **默认关闭**，且注释明确「不注入任何引擎 Service」（避免与 `processEngineConfiguration` 循环依赖），只投影 `wf_*` 台账，**不是通用业务回调钩子**。新监听与其同构（只依赖 `WfInstanceMapper`、走 `setEventListeners` 直接装配），但用独立开关、零侵入既有台账链路。
2. **业务标识以「流转记录表 + instance_id」为准，不依赖 BPMN 变量。** 监听器拿不到引擎变量（决策 1 约束），故由 blade-system 发起时写记录表、回调时按 `instance_id` 反查——可追溯、可审计、天然幂等。监听器侧用发起时写入的 `title` 前缀 `PSF:` 做粗筛，避免每条流程完成都跨服务调用。
3. **无流程配置时降级为「直接变更」（`mode=2`）**，保证功能不因流程未配置/流程服务不可用而瘫痪。
4. **在职判定单点收敛 + 覆盖 refresh_token。** 少了 refresh 一环，已登录的离职账号可无限续期，拦截形同虚设；`RefreshTokenGranter` 返回 null / 抛异常均被 `AuthController:78` 与异常翻译器妥善处理。
5. **消息通知在事务外（afterCommit）**，失败仅记日志不回滚建档——避免消息中心抖动导致建档失败。

### 14.5 流程定义（需部署后才能走审批闭环）

新增 `doc/bpmn/person_status_change.bpmn20.xml`（`process id=person_status_change`，2 个 UserTask 均带 `wf:node` 扩展以通过发布门禁；`doc/tools/validate_bpmn.cjs` 校验 0 error）。

语义：发起节点（引擎自动完成）→ 主管审批（`assignee=${approver}`）→ 排他网关，默认出口「审批通过」→ 结束；`${wfOutcome == 'reject'}` 退回发起节点重新提交。**故只有走到结束事件才派发 PROCESS_COMPLETED，回调即视为审批通过。**

部署（需 Nacos + blade-workflow 在线，走 HTTP 管理端；Feign 无法部署）：
```
POST /api/blade-workflow/definition/import   (body: {bpmnXml, name, formId, type})
POST /api/blade-workflow/definition/{id}/deploy
```
部署后把 `procKey=person_status_change` 配到 `blade_person_status_flow.flow_key`（初始化数据已预置）。

### 14.6 验证状态

- ✅ 后端 `mvn -o -pl blade-system / blade-workflow / blade-auth -am compile` **均 EXIT=0**；前端 `tsc --noEmit` 改动路径**无错误**。
- ✅ 建表脚本 dev 库执行成功（5/5，含 2 条流转配置初始化）。
- ✅ **接口级联调已通过（2026-10-08，`doc/tools/verify-p3.cjs` 直连 8100/8106，11 项全过）**：
>
> | # | 验证项 | 实测结果 |
> |---|---|---|
> | 0 | admin 登录（SM2） | `LOGIN-OK` |
> | 1 | `POST /user/submit` 建档（P3-3 伴生初始化） | `200 操作成功` |
> | 2 | `GET /user/complete-status` 信息完善度 | `200`，四项 `BASE/PERSON/WORK/SYSTEM` 均初始化 ✅ 伴生初始化生效 |
> | 3 | `GET /user/status-flow/available` 可用流转 | `200`，`1->4(离职,审批)` ✅ P3-2 入口可用 |
> | 4 | `POST /user/status-flow/start` 降级直改 1→2 | `200 未配置审批流程，按直接变更办理` ✅ P3-1 降级路径 |
> | 5 | `GET /user/status-flow/records` 流转记录 | `200`，1 条 `1->2 mode=2 flowStatus=1` ✅ 记录落库 |
> | 6 | 在职(临时)账号登录 | `200 LOGIN-OK` ✅ P3-5 在职放行 |
> | 7 | 直改 2→4（解聘） | `200 按直接变更办理` |
> | 8 | **解聘账号登录拦截** | `400 LOGIN-FAIL(已拦截) 账号已停用` ✅ **P3-5 核心拦截生效** |
> | 9 | 恢复正式后登录 | `200 LOGIN-OK` ✅ 拦截可解除 |
> | 10 | 清理测试用户 | `200` |
>
> ⏳ **审批闭环端到端待验证**：需先启动 blade-workflow（当前未运行），并部署 §14.5 的 BPMN（`person_status_change`）；届时 `1->4` 这类「审批」流转会真实走 Flowable，经 `WfBizCallbackListener` 回调 blade-system 更新 `person_status`。本轮已验证「降级直改」路径与登录拦截，BPMN 审批路径待 workflow 服务上线后补验。
> ⚠️ 已知限制：驳回在本流程中是退回发起节点重新提交，不会产生「驳回」终态记录；如需记录驳回需另接 `PROCESS_CANCELLED`。

