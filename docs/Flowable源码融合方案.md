# Flowable 源码融合到 springBlade 的方案

> 目标：让 `D:\workproject\springbladeandreact\flowable-engine`（Flowable **8.1.0-SNAPSHOT** 源码）
> 与 `D:\workproject\springbladeandreact\springBlade`（SpringBlade 5.0.2）协同构建，
> 便于后续直接修改 Flowable 源码实现定制功能。
> 关联：`Flowable8升级执行记录.md`（当前已切到 8.1.0-SNAPSHOT，编译 + 测试通过）
> 关联：`Flowable7→8升级评估.md`

---

## 0. 关键前提（已核实）

两个工程**都是独立根 POM，无 `<parent>`** —— 这决定了可行的融合方式。

| 工程 | 坐标 | 证据 |
|---|---|---|
| springBlade | `org.springblade:SpringBlade:${revision}`（5.0.2），`packaging=pom`，**无 parent** | `springBlade/pom.xml:6-9` |
| flowable-engine | `org.flowable:flowable-root:8.1.0-SNAPSHOT`，`packaging=pom`，**无 parent** | `flowable-engine/pom.xml:9-13` |

其它已核实事实：

- SpringBlade 根 pom 含 **enforcer 规则 `ban-jackson3-autoconfigure`** 与 `pluginManagement`、`flatten` 插件
- Flowable 的 `flowable-spring` **不在默认 reactor**，而在 **`deploy` profile** 下，且该 profile 启用 **GPG 签名**
- SNAPSHOT 装在 **`E:\project\mavenLib`**（本机无 `settings.xml`，默认仓库 `C:\Users\Administrator\.m2\repository` 里没有它）
- Flowable 全量构建实测 **3 分 48 秒**

---

## 1. 结论先行

**推荐方案 B：顶层聚合 POM**（建在 `D:\workproject\springbladeandreact`）——
一条命令按序构建 Flowable → SpringBlade，两侧 reactor 各自独立、互不污染。

**不要选「直接并入 SpringBlade reactor」**（方案 C），理由见 §3。

---

## 2. 四个方案对比

| 方案 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| **A 子模块 + 构建脚本** | flowable-engine 作为 git submodule 放进 springBlade，脚本两步构建 | 代码物理在一起，改动可 git 追踪 | 仍是两步；submodule 有使用成本 |
| **B 顶层聚合 POM ⭐** | 在 `springbladeandreact` 建 aggregator，`modules=[flowable-engine, springBlade]` | **一条命令**、顺序由 Maven 保证、两侧 reactor 独立 | 多一个 pom；构建 Flowable 约 4 分钟 |
| **C 直接并入 reactor** | SpringBlade 根 pom 加 `<module>` 指向 Flowable 各模块 | 真正「一个 reactor」 | **高风险**，见 §3 |
| **D 不合并，只固化工作流** | 维持现状，把重建命令写成脚本 | 零改造、最快 | 仍是两步手工 |

---

## 3. 为什么不选 C（直接并入 SpringBlade reactor）

1. **enforcer 污染** —— SpringBlade 根 pom 的 `ban-jackson3-autoconfigure` 规则与 `pluginManagement`
   会作用于 Flowable 模块，可能直接破坏其构建
2. **profile 污染** —— Flowable 需要 `-Pdeploy -Dgpg.skip=true` 才产出 `flowable-spring`；
   并入后这些参数会连带作用于 SpringBlade 侧
3. **构建量爆炸** —— Flowable 51+ 个模块，每次 `mvn compile` 都要拖着跑
4. **JDK 基线不同** —— Flowable 17 / 项目 21

---

## 4. 方案 B 落地步骤

### 4.1 新建聚合器 `D:\workproject\springbladeandreact\pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.springblade</groupId>
  <artifactId>springblade-and-flowable</artifactId>
  <version>1.0.0</version>
  <packaging>pom</packaging>
  <modules>
    <module>flowable-engine</module>
    <module>springBlade</module>
  </modules>
</project>
```

> ⚠️ 必须放在**两者共同父目录**。Maven 不支持在 springBlade 内用 `../flowable-engine` 反向引用。

### 4.2 一条命令构建

```powershell
cd D:\workproject\springbladeandreact
mvn install -Pdeploy "-Dgpg.skip=true" "-Dmaven.test.skip=true" -B "-Dmaven.repo.local=E:\project\mavenLib"
```

| 参数 | 作用 |
|---|---|
| `-Pdeploy` | 让 Flowable 产出 `flowable-spring`（它在 deploy profile 下） |
| `-Dgpg.skip=true` | 否则 `maven-gpg-plugin` 报 `Could not determine gpg version` |
| `-Dmaven.repo.local=E:\project\mavenLib` | **必需**，SNAPSHOT 装在该仓库 |
| SpringBlade 侧 | 无 deploy profile，不受 `-Pdeploy` 影响 |

### 4.3 日常开发加速（只构建 SpringBlade，跳过 Flowable）

```powershell
mvn -pl springBlade -am install -DskipTests -B "-Dmaven.repo.local=E:\project\mavenLib"
```

### 4.4 PowerShell 注意

`-Dxxx.yyy=zzz` 形式的参数**必须加引号**，否则 PowerShell 会拆成 `.yyy=zzz`
（已实测触发 `Unknown lifecycle phase` 错误）。

---

## 5. 必须先知道的约束

| 约束 | 说明 |
|---|---|
| **改源码必须 reinstall** | SpringBlade 从**仓库**取 jar，不直接引用源码目录。改完 Flowable 不 install **不生效** |
| **构建耗时** | Flowable 全量实测 3 分 48 秒；日常可增量 |
| **聚合器位置** | 只能是 `springbladeandreact`，不能反向引用 |
| **git 追踪** | 建议把 `flowable-engine` 加为 **git submodule**，否则 fork 的改动无法版本化管理 |
| **fork 成本** | 使用源码即等于 fork，Flowable 升级需自行 rebase 改动 |

---

## 6. 进阶：精准 vendoring（若要「改源码即时生效」）

如果后续**频繁修改 Flowable 源码**、嫌每次 install 麻烦，**不建议全量并入**，
而是只把**要改的那一个模块**以源码形式放进 SpringBlade reactor，其余 40+ 个模块仍从仓库取。

| 对比 | 全量并入 | 精准 vendoring |
|---|---|---|
| 拖入模块数 | 51+ | 1（如 `flowable-engine`） |
| 改源码即时生效 | ✅ | ✅ |
| 构建耗时 | 很慢 | 可控 |
| 依赖处理 | 自动 | 需手动确认（`<parent>`、bom 从仓库解析，实测可行） |

> 前置：需先明确**要改哪个模块**（由要实现的定制功能决定）。

---

## 7. 建议路径

1. **先落地 B**（低风险，立即解决「一条命令构建」）
2. 把 `flowable-engine` 加为 **git submodule**，让 fork 改动可追踪
3. 明确**要改哪个功能/模块**后，再决定是否做精准 vendoring

---

## 8. 待确认

- [ ] 是否采用方案 B（或选 A / D）
- [ ] `flowable-engine` 是否加为 git submodule
- [ ] **要定制的功能是什么**（决定是否需要精准 vendoring）
  - 候选 1：组织 / 数据权限下沉到引擎
  - 候选 2：审批轨迹 / 抄送传阅改用引擎原生能力（`ACT_HI_COMMENT` / identity link）
  - 候选 3：实例 / 任务状态由引擎事件派生（**此项无需改源码**，用 `FlowableEventListener` 即可）
  - 候选 4：其它

> 关于候选 1 的既有结论见 `组织数据取数路径核实.md`：现状是应用侧 Feign 解析组织、
> 引擎不参与可见性判定 → **本就不构成双写**。若目标是「去掉 Feign、让引擎本地解析」，则改源码是正当理由。
