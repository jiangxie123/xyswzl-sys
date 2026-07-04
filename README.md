# 校园失物招领系统

基于 **Spring Boot 4.1.0 + Vue 3 + MySQL 8.0** 的校园失物招领全栈项目，
统一管理失物与拾物信息，支持发布、搜索、审核、认领、留言等完整业务流程。

---

## 1. 技术栈一览

| 层级 | 技术 | 版本 |
| :--- | :--- | :--- |
| 后端框架 | Spring Boot | 4.1.0 |
| ORM 框架 | MyBatis-Plus | 3.5.7 |
| 安全认证 | Spring Security + JWT | 6.x / 0.12.6 |
| Token 存储 | Redis（可选，不启动也能用） | - |
| 数据库 | MySQL | 8.0+ |
| 构建工具 | Maven | 3.x |
| Java | JDK | 17 |
| 前端框架 | Vue | 3.4+ |
| 前端 UI | Element Plus | 2.7+ |
| 前端构建 | Vite | 5.x |
| 前端路由 / 状态 | Vue Router 4 / Pinia 2 | - |

---

## 2. 环境准备（必须完成）

### 2.1 软件安装

| 软件 | 要求 | 检查命令 |
| :--- | :--- | :--- |
| JDK | 17 及以上 | `java -version` |
| Maven | 3.6+（IDEA 自带即可） | `mvn -version` |
| MySQL | 8.0.x，默认端口 3306 | `mysql --version` |
| Redis | 可选，默认端口 6379（未启动时自动降级为纯 JWT） | `redis-cli ping` |
| Node.js | 18+（仅前端开发/构建时需要） | `node -v` |

### 2.2 数据库初始化

1. 启动 MySQL 服务
2. 打开 IDEA 或 MySQL 客户端，连接到 localhost:3306
3. 执行以下脚本：

```
src/main/resources/sql/init_database.sql
```

脚本会完成：
- 创建数据库 `xyswzl_db`（字符集 utf8mb4_unicode_ci）
- 创建 5 张表：`sys_user` / `item_category` / `item_info` / `item_comment` / `admin_operation_log`
- 初始化 9 个分类 + 4 个测试账号 + 3 条测试物品信息 + 2 条测试留言 + 7 条操作日志

### 2.3 修改数据库连接信息（按需）

打开 `src/main/resources/application.properties`，按实际 MySQL 配置修改：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/xyswzl_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=adc20050209
```

Redis 配置（未安装可忽略，项目会自动降级）：

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=   # 无密码留空
spring.data.redis.database=0
```

---

## 3. 启动项目

### 方式 A：IDEA 一键运行（推荐）

1. 用 IDEA 打开项目根目录（`d:\dai\Projectinformation\xyswzl-sys`）
2. 等待 Maven 自动下载依赖（右下角进度条完成）
3. 找到启动类并运行：

```
src/main/java/com/mzy/xyswzlsys/XyswzlSysApplication.java
```

4. 浏览器访问：

```
http://localhost:8080/
```

> 前端已构建到 `src/main/resources/static/`，直接通过后端 8080 端口即可访问完整页面。

### 方式 B：Maven 命令行

在项目根目录执行：

```bash
mvn spring-boot:run
```

然后访问 `http://localhost:8080/`。

### 方式 C：前端单独开发模式（仅开发时用）

如果需要修改前端代码并热更新：

```bash
cd frontend
npm install      # 首次需要
npm run dev      # 启动 Vite 开发服务器，默认 http://localhost:5173
```

Vite 已通过 `vite.config.js` 配置代理：

```
/api  →  http://localhost:8080/api
```

因此前端在 5173 端口访问 `/api/xxx` 会自动转发到后端 8080，无需额外配置跨域。

前端构建并同步到后端静态资源目录（重新打包部署使用）：

```bash
cd frontend
npm run build
# 然后将 frontend/dist/ 内容复制到 src/main/resources/static/
# 项目已提供 build-frontend.bat / start.bat 辅助脚本
```

---

## 4. 默认账号

| 用户名 | 密码 | 角色 | 说明 |
| :--- | :--- | :--- | :--- |
| `admin` | `123456` | 超级管理员 (role=2) | 完整权限，可删除用户、管理分类、清理日志 |
| `manager` | `123456` | 普通管理员 (role=1) | 可审核物品、管理用户（学生）、查看日志 |
| `student01` | `123456` | 学生用户 (role=0) | 发布、查看、留言、认领等普通操作 |
| `student02` | `123456` | 学生用户 (role=0) | 同上 |

> ⚠️ **安全提示**：密码在数据库中以 BCrypt 加密存储；前端提交时会先用 XOR+Base64 加密传输，后端解密后再进行 BCrypt 比较。生产环境请务必修改默认密码。

---

## 5. 核心目录结构

```
xyswzl-sys/
├── src/main/java/com/mzy/xyswzlsys/
│   ├── XyswzlSysApplication.java          # Spring Boot 启动类（含 @EnableScheduling）
│   ├── common/                             # 统一返回
│   │   ├── Result.java / ResultCode.java / PageResult.java
│   ├── config/                             # 配置类
│   │   ├── WebMvcConfig.java              # 前端路由转发 + 静态资源映射（/uploads, /images）
│   │   ├── CorsConfig.java                # 跨域
│   │   ├── MyBatisPlusConfig.java         # 分页插件
│   │   ├── MyMetaObjectHandler.java       # 自动填充 create_time / update_time
│   │   └── GlobalExceptionHandler.java    # 全局异常处理
│   ├── controller/                         # REST API（7 个模块）
│   │   ├── AuthController.java            # 登录 / 注册
│   │   ├── SysUserController.java         # 用户 CRUD + /users/me 个人信息
│   │   ├── ItemInfoController.java        # 物品 CRUD + 审核 + 状态变更 + 我的发布
│   │   ├── ItemCategoryController.java    # 分类 CRUD
│   │   ├── ItemCommentController.java     # 留言 CRUD
│   │   ├── AdminOperationLogController.java # 操作日志 + 清理接口
│   │   ├── FileUploadController.java      # 图片上传 /api/upload/image
│   │   └── HealthController.java          # 健康检查 /api/health
│   ├── security/
│   │   ├── SecurityConfig.java            # Spring Security 规则（permitAll / authenticated）
│   │   ├── JwtAuthenticationFilter.java   # Token 解析 + 权限注入
│   │   ├── JwtTokenUtil.java              # JWT 生成 / 解析 / 校验
│   │   └── CurrentUserDetails.java        # 当前登录用户封装
│   ├── service/ + service/impl/            # 业务逻辑层（6 个 Service 接口 + 实现类）
│   │   ├── AuthServiceImpl.java           # 登录/注册（含失败锁定 5 次 → 1 分钟）
│   │   ├── SysUserServiceImpl.java        # 用户管理（角色越权拦截）
│   │   ├── ItemInfoServiceImpl.java       # 物品 CRUD（归属校验 + 状态变更校验）
│   │   ├── ItemCategoryServiceImpl.java   # 分类管理
│   │   ├── ItemCommentServiceImpl.java    # 留言管理
│   │   └── AdminOperationLogServiceImpl.java # 日志 CRUD + 定时清理（每天 02:00 清理 30 天前）
│   ├── dto/request/                        # 请求 DTO（7 个）
│   ├── dto/response/                       # 响应 DTO（2 个）
│   ├── entity/                             # 数据库实体（5 个表对应）
│   ├── mapper/                             # MyBatis-Plus Mapper（5 个）
│   └── util/
│       └── PasswordCrypto.java             # XOR+Base64 解密（前端加密的反向操作）
│
├── src/main/resources/
│   ├── application.properties              # Spring Boot 配置
│   ├── sql/init_database.sql               # 完整数据库初始化脚本
│   └── static/                             # 前端构建产物（Vue 打包输出）
│       ├── index.html
│       ├── assets/*.js / *.css
│       └── images/login-bg.jpg / app-bg.jpg
│
├── frontend/                               # Vue 3 前端源码
│   ├── src/
│   │   ├── views/                          # 页面（10 个页面：登录 + 学生 4 个 + 管理员 4 个）
│   │   ├── router/index.js                 # 路由配置（含 /items/** /admin/** 通配符）
│   │   ├── stores/user.js                  # Pinia 用户状态（登录态 + Token 管理）
│   │   ├── utils/request.js                # Axios 封装（请求拦截自动携带 Token）
│   │   ├── utils/encrypt.js                # 密码加密（XOR + Base64）
│   │   ├── utils/captcha.js                # 前端 Canvas 验证码
│   │   └── api/                            # API 请求层（auth / user / item / category / comment / log）
│   ├── vite.config.js                      # 代理 /api → 8080
│   └── package.json
│
├── pom.xml                                 # Maven 依赖配置
├── PROJECT_GUIDE.md                        # 项目设计与开发指南
├── CHANGELOG.md                            # 变更日志
├── DESIGN.md                               # 设计说明
└── DEMO_GUIDE.md                           # 演示顺序说明
```

---

## 6. 核心 API 速查

所有 API 统一使用 `/api/` 前缀，统一返回格式：

```json
{ "code": 200, "message": "操作成功", "data": ... }
```

| Method | Path | 功能 | 需 Token | 角色限制 |
| :--- | :--- | :--- | :--- | :--- |
| POST | `/api/auth/login` | 登录（密码需 XOR+Base64 加密） | 否 | - |
| POST | `/api/auth/register` | 注册新用户（默认 role=0） | 否 | - |
| GET | `/api/health` | 健康检查 | 否 | - |
| GET | `/api/categories` | 物品分类列表 | 否 | - |
| GET | `/api/items` | 物品列表（type/category/status/keyword 筛选） | 否 | - |
| GET | `/api/items/{id}` | 物品详情 | 否 | - |
| POST | `/api/upload/image` | 上传图片 | 是 | 登录用户 |
| POST | `/api/items` | 发布物品信息 | 是 | 登录用户 |
| PUT | `/api/items/{id}` | 修改物品信息 | 是 | 发布者 / 管理员 |
| DELETE | `/api/items/{id}` | 删除物品信息 | 是 | 发布者 / 管理员 |
| PUT | `/api/items/{id}/audit` | 审核物品（通过 / 驳回） | 是 | 管理员 |
| PUT | `/api/items/{id}/status` | 变更物品状态（认领/找回/下架） | 是 | 发布者 |
| GET | `/api/items/mine` | 查看我的发布 | 是 | 本人 |
| GET | `/api/comments/item/{itemId}` | 查询某物品的留言列表 | 否 | - |
| POST | `/api/comments` | 发布留言 | 是 | 登录用户 |
| DELETE | `/api/comments/{id}` | 删除留言 | 是 | 作者 / 管理员 |
| GET | `/api/users` | 分页查询用户列表 | 是 | 管理员 |
| GET | `/api/users/{id}` | 查询单个用户 | 是 | 管理员 |
| POST | `/api/users` | 新增用户 | 是 | 管理员（超级管理员才可新增其他管理员） |
| PUT | `/api/users/{id}` | 修改用户 | 是 | 管理员 |
| PUT | `/api/users/me` | 修改个人信息 | 是 | 本人 |
| DELETE | `/api/users/{id}` | 删除用户 | 是 | 超级管理员 |
| POST | `/api/categories` | 新增分类 | 是 | 管理员 |
| PUT | `/api/categories/{id}` | 修改分类 | 是 | 管理员 |
| DELETE | `/api/categories/{id}` | 删除分类 | 是 | 管理员 |
| GET | `/api/admin/logs` | 分页查询操作日志 | 是 | 管理员 |
| DELETE | `/api/admin/logs/clean?daysBefore=30` | 清理历史日志 | 是 | 超级管理员 |

Token 使用方式：在 HTTP Header 中携带

```
Authorization: Bearer <jwt_token>
```

---

## 7. 常见问题速查

| 问题 | 排查方向 |
| :--- | :--- |
| 启动报数据库连接失败 | 1) MySQL 是否启动？2) `xyswzl_db` 是否已创建？3) `application.properties` 中的 username/password 是否正确？ |
| 登录始终失败，前端提示"密码错误" | 1) 数据库中 `sys_user.password` 是否为 BCrypt 哈希（`$2a$...`）？2) 前端是否正确调用 `encrypt.js` 加密密码？ |
| 登录成功但其他接口 401 | 1) 浏览器 DevTools → Network，检查请求是否带 `Authorization: Bearer xxx`？2) Token 是否过期（默认 24 小时）？ |
| 访问 `/items/1` 刷新后 404 | 前端已通过 `WebMvcConfig` 配置通配符路由转发到 `index.html`，正常应自动生效；如未生效请确认是否以 `mvn spring-boot:run` 或 IDEA Run 启动（而非 `java -jar` 后前端未打包） |
| 上传图片看不到 | 图片保存在 `{项目运行目录}/uploads/item-images/`，URL 为 `/uploads/item-images/xxx.jpg`；`WebMvcConfig` 已配置静态资源映射 |
| Redis 未启动影响使用吗？ | 不影响。`RedisTokenStoreServiceImpl` 已实现自动降级逻辑：Redis 不可用时自动回退为纯 JWT 校验 |
| 分页查询不生效 | 检查 `MyBatisPlusConfig.java` 中是否配置了 `MybatisPlusInterceptor + PaginationInnerInterceptor`；Service 层第一个参数是否为 `Page<>(current, size)` |

---

## 8. 相关文件索引

| 类型 | 文件 | 说明 |
| :--- | :--- | :--- |
| 项目指南 | `PROJECT_GUIDE.md` | 详细设计文档 + 开发路线图 + 状态枚举规范 |
| 设计说明 | `DESIGN.md` | 架构设计思路 + 核心结构解析 |
| 演示指南 | `DEMO_GUIDE.md` | 5 分钟/10 分钟演示流程 |
| 变更日志 | `CHANGELOG.md` | 每次提交的核心功能变更记录 |
| 数据库脚本 | `src/main/resources/sql/init_database.sql` | 完整表结构 + 初始数据 |
| 后端配置 | `src/main/resources/application.properties` | 数据库、Redis、上传目录等配置 |
| 前端配置 | `frontend/vite.config.js` | Vite + 后端代理配置 |
