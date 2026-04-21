# SpringBlade 项目规则文档

## 1. 项目创建流程

### 1.1 环境准备
- **JDK**: 17+
- **Maven**: 3.8+
- **Node.js**: 18+ (前端开发)
- **IDE**: IntelliJ IDEA 或 Eclipse
- **Git**: 2.0+

### 1.2 项目初始化
1. **克隆代码库**
   ```bash
   git clone https://gitee.com/smallc/SpringBlade.git
   cd SpringBlade
   ```

2. **构建项目**
   ```bash
   mvn clean package -DskipTests
   ```

3. **启动依赖服务**
   - Nacos Server 3.1.1+
   - Redis Server 7.0+
   - MySQL 8.0+

4. **初始化数据库**
   - 执行 `doc/sql` 目录下的数据库脚本

5. **配置修改**
   - 修改各模块的 `application-dev.yml` 配置文件
   - 配置数据库连接信息
   - 配置 Nacos 服务地址

### 1.3 模块创建流程
1. **创建服务 API 模块**
   - 在 `blade-service-api` 目录下创建新的 API 模块
   - 定义实体类、DTO、VO 和 Feign 客户端

2. **创建服务实现模块**
   - 在 `blade-service` 目录下创建对应的实现模块
   - 实现业务逻辑

3. **配置依赖关系**
   - 在父模块的 `pom.xml` 中添加新模块
   - 在相关模块中添加依赖

## 2. 技术栈限制

### 2.1 核心技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17+ | 主要开发语言 |
| Spring Boot | 3.5.9 | 应用框架 |
| Spring Cloud | 2025.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2025.0.0.0 | 微服务生态 |
| Nacos | 3.1.1 | 服务注册与配置中心 |
| MyBatis Plus | 3.5.19 | ORM 框架 |
| Redis | 7.0+ | 缓存 |
| MySQL | 8.0+ | 数据库 |
| JWT | - | 认证令牌 |
| Sentinel | - | 服务保护 |

### 2.2 前端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Node.js | 18+ | 运行环境 |
| Vue 3 | - | 前端框架 (Saber3) |
| React | - | 前端框架 (Sword) |
| Ant Design | - | UI 组件库 |
| Element UI | - | UI 组件库 |

### 2.3 工具与插件
| 工具 | 版本 | 用途 |
|------|------|------|
| Maven | 3.8+ | 项目构建 |
| Docker | 20.0+ | 容器化 |
| Jenkins | - | CI/CD |
| Git | 2.0+ | 版本控制 |

### 2.4 依赖管理
- 使用 Maven BOM 统一管理依赖版本
- 所有依赖必须在 `blade-core-bom` 中定义
- 新增依赖需要评估安全性和稳定性

## 3. 命名规范

### 3.1 包命名
- **格式**: `org.springblade.{模块名}.{功能模块}`
- **示例**:
  - `org.springblade.auth.controller`
  - `org.springblade.system.service`

### 3.2 类命名
- **控制器**: `{业务名称}Controller.java`
  - 示例: `AuthController.java`
- **服务接口**: `I{业务名称}Service.java`
  - 示例: `IUserService.java`
- **服务实现**: `{业务名称}ServiceImpl.java`
  - 示例: `UserServiceImpl.java`
- **实体类**: `{业务名称}.java`
  - 示例: `User.java`
- **数据传输对象**: `{业务名称}DTO.java`
  - 示例: `UserDTO.java`
- **视图对象**: `{业务名称}VO.java`
  - 示例: `UserVO.java`
- **映射器**: `{业务名称}Mapper.java`
  - 示例: `UserMapper.java`
- **工具类**: `{功能}Util.java`
  - 示例: `TokenUtil.java`

### 3.3 方法命名
- **查询方法**: `get{业务名称}`、`find{业务名称}`、`list{业务名称}`
  - 示例: `getUserInfo()`
- **新增方法**: `save{业务名称}`、`add{业务名称}`
  - 示例: `saveUser()`
- **更新方法**: `update{业务名称}`
  - 示例: `updateUser()`
- **删除方法**: `remove{业务名称}`、`delete{业务名称}`
  - 示例: `removeUser()`
- **批量操作**: `batch{操作}{业务名称}`
  - 示例: `batchSaveUser()`

### 3.4 变量命名
- **驼峰命名法**
- **常量**: 全大写，下划线分隔
  - 示例: `USER_NOT_FOUND`
- **成员变量**: 小驼峰
  - 示例: `userId`
- **局部变量**: 小驼峰
  - 示例: `userName`

### 3.5 数据库表命名
- **格式**: `blade_{模块名}_{表名}`
- **示例**:
  - `blade_user`
  - `blade_dept`
- **字段命名**: 下划线分隔
  - 示例: `user_id`、`dept_name`

### 3.6 API 端点命名
- **格式**: `/api/{模块名}/{资源}/{操作}`
- **示例**:
  - `/api/auth/token`
  - `/api/system/user/list`
- **HTTP 方法**:
  - GET: 查询
  - POST: 新增
  - PUT: 更新
  - DELETE: 删除

## 4. 项目结构

### 4.1 模块结构
```
SpringBlade
├── blade-auth/            # 认证授权模块
├── blade-common/          # 公共模块
├── blade-gateway/         # 网关模块
├── blade-ops/             # 运维模块
│   ├── blade-admin/       # 管理后台
│   ├── blade-develop/     # 代码生成
│   ├── blade-report/      # 报表模块
│   └── blade-resource/    # 资源管理
├── blade-service/         # 业务服务模块
├── blade-service-api/     # 服务 API 模块
├── .editorconfig          # 编辑器配置
├── .gitignore             # Git 忽略配置
├── LICENSE                # 许可证
├── README.md              # 项目说明
└── pom.xml                # Maven 配置
```

### 4.2 模块内部结构
```
{模块名}
├── src/main/java/org/springblade/{模块名}
│   ├── controller/        # 控制器
│   ├── entity/            # 实体类
│   ├── dto/               # 数据传输对象
│   ├── vo/                # 视图对象
│   ├── mapper/            # 数据访问
│   ├── service/           # 服务接口
│   │   └── impl/          # 服务实现
│   ├── config/            # 配置类
│   ├── utils/             # 工具类
│   ├── enums/             # 枚举类
│   └── {模块名}Application.java  # 启动类
├── src/main/resources
│   ├── application.yml        # 主配置文件
│   ├── application-dev.yml    # 开发环境配置
│   ├── application-test.yml   # 测试环境配置
│   └── application-prod.yml   # 生产环境配置
└── pom.xml                # 模块依赖配置
```

## 5. 代码格式标准

### 5.1 代码风格
- 使用 4 个空格进行缩进
- 每行不超过 120 个字符
- 大括号使用 K&R 风格
- 方法之间空一行
- 逻辑块之间空一行

### 5.2 注释规范
- **类注释**: 包含类的功能描述、作者、创建日期
- **方法注释**: 包含方法功能、参数、返回值
- **字段注释**: 包含字段含义
- **TODO 注释**: 标记待完成的功能

### 5.3 异常处理
- 使用 `ServiceException` 处理业务异常
- 使用 `GlobalExceptionHandler` 统一处理异常
- 异常信息应该清晰明确

### 5.4 日志规范
- 使用 `@Slf4j` 注解
- 日志级别使用规范:
  - `debug`: 调试信息
  - `info`: 一般信息
  - `warn`: 警告信息
  - `error`: 错误信息
- 日志内容应该包含必要的上下文信息

## 6. 文档要求

### 6.1 项目文档
- **README.md**: 项目概述、技术栈、快速开始
- **CHANGELOG.md**: 版本变更记录
- **API 文档**: 使用 Swagger 生成

### 6.2 代码文档
- **类文档**: 描述类的功能和使用方法
- **方法文档**: 描述方法的功能、参数和返回值
- **接口文档**: 描述接口的用途和实现要求

### 6.3 数据库文档
- **数据库设计文档**: 表结构、字段含义、索引设计
- **数据字典**: 字段枚举值说明

## 7. 质量保证流程

### 7.1 代码审查
- 提交代码前进行代码审查
- 审查内容包括代码风格、逻辑正确性、安全性
- 使用 SonarQube 进行代码质量检测

### 7.2 测试规范
- **单元测试**: 覆盖核心业务逻辑
- **集成测试**: 测试模块间的交互
- **端到端测试**: 测试完整的业务流程
- 测试覆盖率不低于 80%

### 7.3 安全检查
- 定期进行安全扫描
- 检查 SQL 注入、XSS 攻击等安全漏洞
- 遵循安全最佳实践

### 7.4 性能优化
- 定期进行性能测试
- 优化 SQL 查询
- 合理使用缓存
- 监控系统性能指标

## 8. 部署规范

### 8.1 环境配置
- **开发环境**: 本地开发
- **测试环境**: 模拟生产环境
- **预生产环境**: 与生产环境一致
- **生产环境**: 正式运行环境

### 8.2 容器化部署
- 使用 Docker 容器化
- 配置 Dockerfile
- 使用 Kubernetes 进行编排

### 8.3 CI/CD 流程
- 使用 Jenkins 进行持续集成
- 自动化测试
- 自动化部署

## 9. 版本管理

### 9.1 版本号规范
- **格式**: `{主版本}.{次版本}.{修订版本}`
- **示例**: `4.8.0`

### 9.2 分支管理
- **master**: 主分支，稳定版本
- **develop**: 开发分支
- **feature/{功能}**: 功能分支
- **bugfix/{问题}**: 修复分支

### 9.3 提交规范
- **格式**: `{类型}({范围}): {简短描述}`
- **类型**: feat, fix, docs, style, refactor, test, chore
- **示例**: `feat(auth): 添加社交登录功能`

## 10. 最佳实践

### 10.1 编码建议
- 遵循 SOLID 原则
- 使用设计模式解决常见问题
- 保持代码简洁明了
- 避免重复代码

### 10.2 性能优化
- 合理使用缓存
- 优化数据库查询
- 使用异步处理
- 避免内存泄漏

### 10.3 安全建议
- 使用 HTTPS
- 加密敏感数据
- 防止 SQL 注入
- 防止 XSS 攻击
- 合理设置权限

### 10.4 可维护性
- 编写清晰的文档
- 使用有意义的命名
- 模块化设计
- 易于测试

## 11. 违规处理

- 违反代码规范的提交将被拒绝
- 严重违规将影响代码审查通过率
- 多次违规将进行团队内通报

## 12. 附则

- 本规则自发布之日起生效
- 规则的解释权归项目团队所有
- 规则将根据项目发展进行适时调整

---

**版本**: 1.0.0
**发布日期**: 2026-04-19
**适用范围**: SpringBlade 4.8.0 及以上版本