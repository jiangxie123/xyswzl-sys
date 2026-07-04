# 校园失物招领系统 - 变更日志

本文件记录项目每次提交的核心功能变更和对应的代码位置，方便快速理解项目演进。

---

## v1.2.0 - 2026-07-03

**提交哈希**：本地开发提交（安全加固 + 前端体验优化）
**Commit 信息**：fix: P0 级安全修复（角色校验、归属校验、登录失败锁定）+ 操作日志页面修复 + 注册表单校验对齐 + 前端构建产物更新

---

### 🔧 核心修复（后端）

#### 1. P0 级安全修复：角色越权访问拦截
**功能说明**：学生（role=0）不能调用任何 `/api/users` 管理接口；普通管理员（role=1）不能删除用户，不能新增/修改其他管理员账号。

**后端文件变更**：
- [SysUserController.java](src/main/java/com/mzy/xyswzlsys/controller/SysUserController.java) — 每个管理接口入口增加 `isAdmin(currentRole)` / `isSuperAdmin(currentRole)` 判断；`delete` 仅超级管理员可用；`add` / `update` 由 Service 层做角色取值检查

#### 2. P0 级安全修复：物品归属校验
**功能说明**：普通用户修改/删除物品时，Service 层强制校验 userId 是否匹配；非发布者操作直接抛出异常。同时 `status=1`（已认领/找回）状态变更时必须提供认领人 ID，防止状态被恶意篡改。

**后端文件变更**：
- [ItemInfoServiceImpl.java](src/main/java/com/mzy/xyswzlsys/service/impl/ItemInfoServiceImpl.java) — `updateItem` 增加 `existing.getUserId().equals(userId)` 校验；`deleteItem` 增加相同逻辑（`isAdmin` 作为例外条件）；`changeItemStatus` 新增 `claimUserId` 非空 + >0 校验，且仅发布者可变更状态

#### 3. P0 级安全修复：登录失败锁定
**功能说明**：同一用户名连续登录失败 5 次后，1 分钟内禁止再次登录，防爆力破解。

**后端文件变更**：
- [AuthServiceImpl.java](src/main/java/com/mzy/xyswzlsys/service/impl/AuthServiceImpl.java) — 引入 `FAILURE_CACHE`（ConcurrentHashMap）；每次登录失败计数累加，达到 MAX_FAILURE 触发 LOCK_DURATION_MS 锁定；登录成功自动重置计数

#### 4. P0 级安全修复：请求参数数值范围校验
**功能说明**：防止前端/工具提交 `role=99` 或 `status=99` 绕过业务逻辑。

**后端文件变更**：
- [SysUserRequest.java](src/main/java/com/mzy/xyswzlsys/dto/request/SysUserRequest.java) — `role` 字段新增 `@Min(0) @Max(2)`；`status` 字段新增 `@Min(0) @Max(1)`

---

### 🛠 核心修复（前端）

#### 5. 操作日志页面修复（无响应 + 缺加载状态）
**功能说明**：原页面 `getAdminLogs().then(...)` 未 catch 错误，接口失败时表格永远处于空状态；也没有 loading 提示。改造为 `async/await + try/catch` 并增加表格 v-loading。

**前端文件变更**：
- [OperationLog.vue](frontend/src/views/admin/OperationLog.vue) — 引入 `loading` 响应状态；`el-table` 增加 `v-loading="loading"`；`loadLogs()` 改为 async，请求包 try/catch，失败时清空 records；ElMessage 自然给出错误提示

#### 6. 注册表单校验与后端对齐
**功能说明**：原注册表单只校验「用户名必填 + 密码必填 + 两次密码一致」，与后端实际规则不一致。现在用户名长度/格式、手机号格式、邮箱格式、学号格式、学院长度等全部与后端一致。

**前端文件变更**：
- [Login.vue](frontend/src/views/Login.vue) — `registerRules` 扩展 `username`（3-30 字符 + 仅字母数字下划线）、`realName`（max 50）、`phone`（`^[0-9-+]+$`）、`email`（email 格式）、`studentId`（仅字母数字 + 横杠）、`college`（max 100）；对应 form-item 补充 `prop` 绑定触发校验

---

### 🧪 自动化接口测试（v1.2.0 新增）
**功能说明**：基于 Java 11 HttpClient（无第三方依赖），对核心业务流程进行全链路接口级回归测试，覆盖认证、用户管理、物品 CRUD、操作日志、权限矩阵等 44 个用例，全部通过。

**测试文件**：
- [AbstractApiTest.java](src/test/java/com/mzy/xyswzlsys/AbstractApiTest.java) — 测试基类：HTTP 请求发送、JSON 解析、登录辅助、密码加密（XOR + Base64）
- [AuthApiTest.java](src/test/java/com/mzy/xyswzlsys/AuthApiTest.java) — 认证测试（12 用例）：注册、登录、登出、非加密密码被拒绝、连续失败锁定、未登录访问受限
- [UserManagementApiTest.java](src/test/java/com/mzy/xyswzlsys/UserManagementApiTest.java) — 用户管理测试（7 用例）：角色越权拦截、CRUD、分页查询
- [ItemApiTest.java](src/test/java/com/mzy/xyswzlsys/ItemApiTest.java) — 物品操作测试（14 用例）：创建/修改/删除归属校验、claimUserId 必填、状态变更、管理员审核、分类查询
- [OperationLogApiTest.java](src/test/java/com/mzy/xyswzlsys/OperationLogApiTest.java) — 操作日志测试（10 用例）：权限拦截、分页查询、条件筛选、超级管理员清理功能

**测试执行方式**：
```bash
mvn test           # 运行全部 44 个测试
mvn test -Dtest=ItemApiTest   # 单独运行某测试类
```
**当前状态**：✅ Tests run: 44, Failures: 0, Errors: 0

---

### 🧹 操作日志清理功能（v1.2.0 新增）
**功能说明**：为防止操作日志无限增长，提供双重清理机制：
- **定期清理**：每天凌晨 02:00 自动清理 30 天前的操作日志（基于 `@Scheduled` cron `0 0 2 * * ?`）
- **主动清理**：超级管理员可通过前端按钮随时清理指定天数前的日志（仅 role=2 有权）

**后端文件变更**：
- [XyswzlSysApplication.java](src/main/java/com/mzy/xyswzlsys/XyswzlSysApplication.java) — 新增 `@EnableScheduling` 启用定时任务
- [AdminOperationLogService.java](src/main/java/com/mzy/xyswzlsys/service/AdminOperationLogService.java) — 新增 `cleanOldLogs(int daysBefore)` 接口定义与 `scheduledCleanOldLogs()` 定时任务
- [AdminOperationLogServiceImpl.java](src/main/java/com/mzy/xyswzlsys/service/impl/AdminOperationLogServiceImpl.java) — 实现清理逻辑（按 create_time < 当前时间 - 天数删除）
- [AdminOperationLogController.java](src/main/java/com/mzy/xyswzlsys/controller/AdminOperationLogController.java) — 新增 `DELETE /api/admin/logs/clean?daysBefore=30` 接口，role=0/1 均返回 403，仅 role=2 可执行

**前端文件变更**：
- [log.js](frontend/src/api/log.js) — 新增 `cleanLogs(params)` API 方法
- [OperationLog.vue](frontend/src/views/admin/OperationLog.vue) — 页面右上角新增「清理历史日志」按钮，点击后弹出确认框输入天数，二次确认后执行清理并刷新列表；筛选下拉框新增「CLEAN=清理」类型

**权限控制**：
- 学生 (role=0)：❌ 403 无权操作
- 管理员 (role=1)：❌ 403 无权操作  
- 超级管理员 (role=2)：✅ 200 清理成功

---

### 🔐 权限矩阵（v1.2.0 与 v1.1.0 一致，仅校验更严格）

| 功能 | 学生 (role=0) | 管理员 (role=1) | 超级管理员 (role=2) |
|------|----------------|-------------------|------------------------|
| 登录 | ✅ | ✅ | ✅ |
| 修改自己的基本信息 | ✅ | ✅ | ✅ |
| 查看用户列表 | ❌ | ✅ | ✅ |
| 新增学生 | ❌ | ✅ | ✅ |
| 新增管理员/超级管理员 | ❌ | ❌ | ✅ |
| 修改学生信息 | ❌ | ✅ | ✅ |
| 修改管理员/超级管理员信息 | ❌ | ❌ | ✅ |
| 删除用户 | ❌ | ❌ | ✅ |
| 物品审核 | ❌ | ✅ | ✅ |
| 分类管理 | ❌ | ✅ | ✅ |
| 操作日志查看 | ❌ | ✅ | ✅ |
| 修改他人物品 | ❌ | ❌ | ❌ |
| 删除他人物品（非管理员） | ❌ | ❌ | ❌ |

---

### 📦 构建产物
- `src/main/resources/static/index.html`
- `src/main/resources/static/assets/*.js`
- `src/main/resources/static/assets/*.css`
- 构建状态：✅ `vite build` 成功（6.6s）

---

## v1.1.0 - 2026-07-02

**提交哈希**：`56beeb9` | 作者：mzy
**Commit 信息**：功能完善：超级管理员和管理员角色权限体系 + 图片上传 + 注册/登录背景图

---

### 🔥 核心新增功能

#### 1. 超级管理员 / 管理员 角色权限体系
**功能说明**：区分「超级管理员（role=2）」和「普通管理员（role=1）」，管理员只能新增/修改「学生用户」，不能创建其他管理员或修改其他管理员信息。只有超级管理员才能删除用户。

**后端文件变更**：
- [SecurityConfig.java](src/main/java/com/mzy/xyswzlsys/security/SecurityConfig.java) — 新增 `/api/users/me` 路由权限；区分管理员与超级管理员接口；用户管理相关接口（新增/修改/删除）分层控制权限
- [SysUserController.java](src/main/java/com/mzy/xyswzlsys/controller/SysUserController.java) — 新增 `PUT /api/users/me` 修改个人信息；从 JWT 解析当前用户角色传递给 Service
- [SysUserService.java](src/main/java/com/mzy/xyswzlsys/service/SysUserService.java) — 接口签名扩展：`add(request, currentRole)`、`update(id, request, currentRole)`、`updateProfile(userId, request)`
- [SysUserServiceImpl.java](src/main/java/com/mzy/xyswzlsys/service/impl/SysUserServiceImpl.java) — 新增业务逻辑：管理员只能创建学生；管理员不能修改其他管理员；updateProfile 强制忽略角色字段

**前端文件变更**：
- [UserManage.vue](frontend/src/views/admin/UserManage.vue) — 根据角色限制按钮状态；角色下拉框普通管理员禁用，只显示学生选项；删除按钮仅超级管理员可见
- [auth.js](frontend/src/api/auth.js) — 新增 `updateProfile(data)` 接口方法

#### 2. 物品图片上传功能
**功能说明**：用户在发布物品时可上传多张图片，图片保存到项目运行目录的 `uploads/item-images/`，通过 `/uploads/xxx.jpg` URL 访问。

**后端文件变更**：
- [FileUploadController.java](src/main/java/com/mzy/xyswzlsys/controller/FileUploadController.java) — 全新文件：`POST /api/upload/image` 接口，XOR/Base64 解密上传，UUID 重命名保存
- [WebMvcConfig.java](src/main/java/com/mzy/xyswzlsys/config/WebMvcConfig.java) — 新增 `/uploads/**` 静态资源映射；将 uploads 路径解析为绝对路径，避免 Tomcat 临时目录问题；启动时自动创建上传目录
- [application.properties](src/main/resources/application.properties) — 显式声明 `upload.dir=uploads`

**前端文件变更**：
- [ItemPublish.vue](frontend/src/views/ItemPublish.vue) — 新增 `<el-upload>` 图片上传组件与预览
- [ItemList.vue](frontend/src/views/ItemList.vue) — 卡片显示物品图片，无图片时展示占位图
- [ItemDetail.vue](frontend/src/views/ItemDetail.vue) — 详情页图片展示
- [item.js](frontend/src/api/item.js) — 新增 `uploadImage(formData)` 接口方法

#### 3. 登录 / 注册页 + 主界面背景图
**功能说明**：在 `/images/` 目录下放置 `login-bg.jpg`（登录页背景）和 `app-bg.jpg`（主界面背景）即可自定义背景图；缺省时默认使用紫色渐变。

**前端文件变更**：
- [Login.vue](frontend/src/views/Login.vue) — 动态检测 `/images/login-bg.jpg` 是否存在，存在则使用图片背景，否则使用渐变
- [Layout.vue](frontend/src/views/Layout.vue) — 同样的动态检测逻辑用于主界面背景 `/images/app-bg.jpg`

**后端文件变更**：
- [WebMvcConfig.java](src/main/java/com/mzy/xyswzlsys/config/WebMvcConfig.java) — 新增 `/images/**` 静态资源映射（同时支持 `classpath:static/images/` 和 `frontend/public/images/`）

**图片资源**：
- [frontend/public/images/app-bg.jpg](frontend/public/images/app-bg.jpg)
- [src/main/resources/static/images/app-bg.jpg](src/main/resources/static/images/app-bg.jpg)
- [src/main/resources/static/images/login-bg.jpg](src/main/resources/static/images/login-bg.jpg)

#### 4. 前端验证码（Canvas 绘制）
**功能说明**：登录页绘制随机验证码，纯前端生成，无需后端接口。

**前端文件**：
- [captcha.js](frontend/src/utils/captcha.js) — 全新文件：Canvas 绘制随机字符验证码 + 背景噪点

#### 5. 密码加密传输（XOR + Base64）
**功能说明**：登录/注册密码在前端加密后提交，后端解密后再进行 BCrypt 哈希比较。

**前端文件**：
- [encrypt.js](frontend/src/utils/encrypt.js) — 全新文件：XOR 加密 + Base64 编码
- [Login.vue](frontend/src/views/Login.vue) — 提交时调用加密

**后端文件**：
- [PasswordCrypto.java](src/main/java/com/mzy/xyswzlsys/util/PasswordCrypto.java) — 全新文件：XOR 解密 + Base64 解码
- [AuthServiceImpl.java](src/main/java/com/mzy/xyswzlsys/service/impl/AuthServiceImpl.java) — 注册和登录时调用解密
- [JwtAuthenticationFilter.java](src/main/java/com/mzy/xyswzlsys/security/JwtAuthenticationFilter.java) — Token 解析和权限验证

#### 6. JWT Token Redis 存储
**功能说明**：Token 存储在 Redis 中，支持过期自动失效；Redis 不可用时自动降级为纯 JWT 验证。

**后端文件**：
- [TokenStoreService.java](src/main/java/com/mzy/xyswzlsys/service/TokenStoreService.java) — 全新文件：Token 存储接口定义
- [RedisTokenStoreServiceImpl.java](src/main/java/com/mzy/xyswzlsys/service/impl/RedisTokenStoreServiceImpl.java) — 全新文件：Redis 存储实现，带降级策略
- [pom.xml](pom.xml) — 新增 Spring Data Redis 依赖

#### 7. 健康检查接口
**功能说明**：`GET /api/health` 用于验证服务健康状态和数据库连通性。

**后端文件**：
- [HealthController.java](src/main/java/com/mzy/xyswzlsys/controller/HealthController.java) — 新增健康检查端点

#### 8. 前端静态资源补充
**功能说明**：将 Vue 构建产物复制到 Spring Boot 的 `resources/static/`，确保项目打包后可直接运行。

**新增前端构建产物**（`src/main/resources/static/assets/` 目录下）：
- 所有页面的 JS / CSS 压缩产物
- [index.html](src/main/resources/static/index.html)

---

### 🔐 权限矩阵（v1.1.0 最新）

| 功能 | 学生 (role=0) | 管理员 (role=1) | 超级管理员 (role=2) |
|------|----------------|-------------------|------------------------|
| 登录 | ✅ | ✅ | ✅ |
| 修改自己的基本信息 | ✅ | ✅ | ✅ |
| 查看用户列表 | ❌ | ✅ | ✅ |
| 新增学生 | ❌ | ✅ | ✅ |
| 新增管理员/超级管理员 | ❌ | ❌ | ✅ |
| 修改学生信息 | ❌ | ✅ | ✅ |
| 修改管理员/超级管理员信息 | ❌ | ❌ | ✅ |
| 删除用户 | ❌ | ❌ | ✅ |
| 物品审核 | ❌ | ✅ | ✅ |
| 分类管理 | ❌ | ✅ | ✅ |
| 操作日志查看 | ❌ | ✅ | ✅ |

---

## v1.0.0 - 2026-07-02

**提交哈希**：`8b90ce3` | 作者：mzy
**Commit 信息**：feat: 校园失物招领系统 - 初始版本提交

---

### 🎉 项目初版（完整功能）

#### 后端核心功能
- **用户认证**：Spring Security + JWT，支持 3 种角色（学生/管理员/超级管理员）
- **用户管理**：CRUD 用户，支持密码 BCrypt 加密存储
- **物品管理**：失物/招领物品发布、查询、详情、审核（管理员审核后才能公开展示）
- **分类管理**：物品分类的增删改查
- **留言评论**：用户可对物品留言
- **操作日志**：管理员操作审计记录

**核心实体类**：
- [SysUser.java](src/main/java/com/mzy/xyswzlsys/entity/SysUser.java) — 用户
- [ItemInfo.java](src/main/java/com/mzy/xyswzlsys/entity/ItemInfo.java) — 物品
- [ItemCategory.java](src/main/java/com/mzy/xyswzlsys/entity/ItemCategory.java) — 分类
- [ItemComment.java](src/main/java/com/mzy/xyswzlsys/entity/ItemComment.java) — 评论
- [AdminOperationLog.java](src/main/java/com/mzy/xyswzlsys/entity/AdminOperationLog.java) — 操作日志

**核心控制器**：
- [AuthController.java](src/main/java/com/mzy/xyswzlsys/controller/AuthController.java) — 登录/注册
- [SysUserController.java](src/main/java/com/mzy/xyswzlsys/controller/SysUserController.java) — 用户管理
- [ItemInfoController.java](src/main/java/com/mzy/xyswzlsys/controller/ItemInfoController.java) — 物品管理
- [ItemCategoryController.java](src/main/java/com/mzy/xyswzlsys/controller/ItemCategoryController.java) — 分类管理
- [ItemCommentController.java](src/main/java/com/mzy/xyswzlsys/controller/ItemCommentController.java) — 评论
- [AdminOperationLogController.java](src/main/java/com/mzy/xyswzlsys/controller/AdminOperationLogController.java) — 操作日志

**技术栈**：Spring Boot 4.1.0 + MyBatis-Plus + Spring Security + JWT + MySQL 8.0

#### 前端核心功能
- **登录/注册**：用户名 + 密码
- **物品列表**：分页展示、分类筛选、关键词搜索
- **物品详情**：显示信息、发布者、联系方式、留言
- **发布物品**：选择类型（失物/招领）、分类、标题、描述、联系方式
- **个人中心**：个人信息查看
- **管理员后台**：用户管理、物品审核、分类管理、操作日志查看

**技术栈**：Vue 3 + Vite + Element Plus + Vue Router + Pinia + Axios

**前端页面文件**：
- [Login.vue](frontend/src/views/Login.vue) — 登录/注册页
- [Layout.vue](frontend/src/views/Layout.vue) — 主布局
- [ItemList.vue](frontend/src/views/ItemList.vue) — 物品列表
- [ItemDetail.vue](frontend/src/views/ItemDetail.vue) — 物品详情
- [ItemPublish.vue](frontend/src/views/ItemPublish.vue) — 发布物品
- [Profile.vue](frontend/src/views/Profile.vue) — 个人中心
- [admin/UserManage.vue](frontend/src/views/admin/UserManage.vue) — 用户管理
- [admin/ItemAudit.vue](frontend/src/views/admin/ItemAudit.vue) — 物品审核
- [admin/CategoryManage.vue](frontend/src/views/admin/CategoryManage.vue) — 分类管理
- [admin/OperationLog.vue](frontend/src/views/admin/OperationLog.vue) — 操作日志

**数据库初始化**：
- [init_database.sql](src/main/resources/sql/init_database.sql) — 数据库表结构 + 测试账号（admin/123456 超级管理员，manager/123456 管理员）

**项目配置**：
- [pom.xml](pom.xml) — Maven 依赖
- [application.properties](src/main/resources/application.properties) — Spring Boot 配置
- [vite.config.js](frontend/vite.config.js) — 前端构建配置（代理 `/api` 到后端 `8080` 端口）
- [PROJECT_GUIDE.md](PROJECT_GUIDE.md) — 项目使用说明

---

## 格式说明

- **版本号**：遵循 [语义化版本号 (Semantic Versioning)](https://semver.org/lang/zh-CN/)
  - 主版本号（X.0.0）：架构级变更，可能不兼容
  - 次版本号（1.Y.0）：新增功能，向后兼容
  - 修订号（1.0.Z）：修复问题，向后兼容

- **每个版本包含**：
  1. 版本号 + 发布日期
  2. 提交哈希（便于在 GitHub 查看具体 diff）
  3. 核心功能列表（每项包含：功能说明 + 后端文件 + 前端文件 + 资源文件）
  4. 如有权限变更，附权限矩阵
