# 校园失物招领系统 - 变更日志

本文件记录项目每次提交的核心功能变更和对应的代码位置，方便快速理解项目演进。

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
