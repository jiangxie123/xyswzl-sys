# 校园失物招领系统 · 设计说明

> 本文档面向**接手开发的同学**或**答辩评审**，解释「为什么这样设计」「核心结构是什么」「关键决策点在哪里」。
> 不求面面俱到，只求最短路径让别人看懂项目。

---

## 1. 一句话概述

> **一个前后端分离的校园失物招领平台：学生发布失物/拾物信息 → 管理员审核 → 信息公开展示 → 用户留言/认领 → 全流程可审计。**

---

## 2. 为什么选择这套技术栈

### 2.1 后端：Spring Boot 4.1.0 + MyBatis-Plus

| 选型 | 理由 |
| :--- | :--- |
| **Spring Boot** | 业界最成熟的 Java Web 方案，约定大于配置，集成 Security/Redis 只需引入 starter |
| **MyBatis-Plus** | 相比原生 MyBatis 省去手写 `select * from xxx` 这类重复代码；相比 JPA/Hibernate，查询条件更直观、SQL 更可控，适合小团队 |
| **Spring Security + JWT** | 实现 **无状态 Token 认证** —— 前端把 Token 存到 localStorage，后端每次请求拦截器解析即可，无需 session 集群；天然支持 SPA 单页应用 |
| **Redis（可选）** | 存放 Token → 支持「主动登出让 Token 立即失效」；即使 Redis 挂掉也能自动降级到纯 JWT 校验 |
| **MySQL 8.0 + utf8mb4** | 8.0 支持窗口函数、CTE；utf8mb4 确保 emoji（如 🎒📱🔑）和中文混合字段正常 |

### 2.2 前端：Vue 3 + Vite + Element Plus

| 选型 | 理由 |
| :--- | :--- |
| **Vue 3** | Composition API 让复杂页面（如物品详情 + 留言 + 操作按钮）逻辑可拆分，代码量明显小于 Vue 2 options 风格 |
| **Vite** | 冷启动 < 500ms；热更新毫秒级；对课程设计这种频繁修改的项目体验提升明显 |
| **Element Plus** | 组件覆盖全项目需要（表格/表单/上传/弹框/消息）；样式统一、开箱即用，避免重复造轮子 |
| **Pinia + Vue Router** | Pinia 替代 Vuex，API 更简洁；Vue Router 管理「登录 → 首页 → 详情 → 个人中心 → 管理员后台」的路由 |
| **Axios 统一封装** | 请求拦截器自动加 Token；响应拦截器统一处理 401 自动跳登录、业务错误统一弹提示 |

### 2.3 架构图（最简版）

```
              浏览器
                │
         ┌──────▼──────────────┐
         │   Vue 3 SPA          │  http://localhost:8080
         │  (Element Plus UI)   │  登录页 / 列表页 / 详情页 / 个人中心 / 管理员后台
         └──────┬──────────────┘
                │ HTTP + JSON
                │ Header: Authorization: Bearer <jwt>
         ┌──────▼──────────────┐
         │  Spring Boot 4.1.0   │  localhost:8080
         │  ┌────────────────┐ │
         │  │ Security Filter│ │  ← JwtAuthenticationFilter 解析 Token → GrantedAuthority
         │  ├────────────────┤ │
         │  │ Controller 层  │ │  ← 7 个 Controller：Auth / User / Item / Category / Comment / Log / Upload
         │  ├────────────────┤ │
         │  │  Service 层    │ │  ← 业务逻辑 + 权限校验（归属检查、角色检查）
         │  ├────────────────┤ │
         │  │  Mapper 层     │ │  ← MyBatis-Plus BaseMapper<T>，极少手写 XML
         │  └──────┬─────────┘ │
         └──────┬──┴────────────┘
                │
         ┌──────▼──────┐        ┌──────────────┐
         │   MySQL 8.0 │        │   Redis（可  │
         │  xyswzl_db  │        │   选存储）    │
         │  5 张表     │        │  (key: token)│
         └─────────────┘        └──────────────┘
```

---

## 3. 核心结构（让你 5 分钟内知道代码在哪）

### 3.1 后端分层

```
com.mzy.xyswzlsys
├── XyswzlSysApplication.java        ← 启动入口（含 @EnableScheduling → 日志定时清理）
│
├── common/                          ← 统一返回三件套
│   ├── Result.java                  ← 统一响应包装 { code, message, data }
│   ├── ResultCode.java              ← 状态码枚举
│   └── PageResult.java              ← 分页返回包装（records, total, size, current, pages）
│
├── config/                          ← 基础设施
│   ├── WebMvcConfig.java            ← 【重要】前端路由转发（通配符 /items/** /admin/** → index.html）
│   │                                   + 静态资源映射（/uploads/** → 本地磁盘；/images/** → classpath）
│   ├── CorsConfig.java              ← 跨域（开发阶段 Vite 5173 访问后端 8080）
│   ├── MyBatisPlusConfig.java       ← 分页插件 PaginationInnerInterceptor
│   ├── MyMetaObjectHandler.java     ← create_time / update_time 自动填充（注解 @TableField(fill)）
│   └── GlobalExceptionHandler.java  ← 全局异常 → 统一 Result 错误返回，Controller 不再写 try-catch
│
├── security/                        ← 【核心】认证与鉴权
│   ├── SecurityConfig.java          ← Spring Security 规则：哪些路径 permitAll，哪些需要 authenticated
│   ├── JwtAuthenticationFilter.java ← 每个请求检查 Authorization Header → 解析 JWT → 注入 SecurityContext
│   ├── JwtTokenUtil.java            ← JWT 生成/解析/过期判断（secret 硬编码在类里，仅课程设计级别）
│   └── CurrentUserDetails.java      ← 已登录用户信息封装（id, username, role, 权限集合）
│
├── controller/                      ← 7 个模块的 REST API（见 README 第 6 节 API 表格）
├── service/ + service/impl/         ← 6 个 Service（Auth / SysUser / ItemInfo / ItemCategory / ItemComment / AdminOperationLog）
├── dto/request/                     ← 7 个请求 DTO（LoginRequest / RegisterRequest / ItemInfoRequest / ...）
├── dto/response/                    ← 2 个响应 DTO（LoginResponse / SysUserResponse）
├── entity/                          ← 5 个数据库实体（SysUser / ItemInfo / ItemCategory / ItemComment / AdminOperationLog）
├── mapper/                          ← 5 个 Mapper，继承 BaseMapper<T>
└── util/
    └── PasswordCrypto.java          ← XOR + Base64 解密（前端 encrypt.js 的反向操作）
```

**关键决策**：没有引入 DDD / 事件驱动等复杂架构——因为项目规模只有 5 张表、几十个接口，经典三层架构（Controller → Service → Mapper）最合适，学习成本最低、维护最直接。

### 3.2 前端结构

```
frontend/src
├── main.js                           ← 入口：挂载 Vue、注册 Element Plus、Pinia、Router
├── App.vue                           ← 根组件（<router-view/>）
│
├── router/index.js                   ← 【关键】路由表 + 登录守卫（meta.requiresAuth + meta.roles）
│                                     │ 路由守卫：未登录跳 /login；角色不够跳 / 403 提示
│
├── stores/user.js                    ← Pinia：保存 token、用户信息、角色、登录态
│
├── utils/
│   ├── request.js                    ← Axios 实例：baseURL='/api'，请求拦截器加 Token，响应拦截器处理 401
│   ├── encrypt.js                    ← XOR + Base64 加密（登录/注册密码提交前调用）
│   └── captcha.js                    ← 前端 Canvas 验证码（4 位随机字符 + 噪点 + 干扰线）
│
├── api/
│   ├── auth.js                       ← login / register / updateProfile
│   ├── user.js                       ← getUserList / addUser / updateUser / deleteUser
│   ├── item.js                       ← getItemList / getItemDetail / publishItem / updateItem / deleteItem / auditItem / changeStatus / uploadImage
│   ├── category.js                   ← getCategories / addCategory / updateCategory / deleteCategory
│   ├── comment.js                    ← getComments / addComment / deleteComment
│   └── log.js                        ← getAdminLogs / cleanLogs
│
└── views/                            ← 10 个页面
    ├── Login.vue                     ← 登录 + 注册（双 Tab，验证码 + 密码加密）
    ├── Layout.vue                    ← 主布局（顶部导航 + 左侧菜单 + 内容区；动态检测 /images/app-bg.jpg 背景）
    ├── ItemList.vue                  ← 物品列表（分类筛选 + 类型切换 + 搜索 + 分页）
    ├── ItemDetail.vue                ← 物品详情（图片轮播 + 详细信息 + 留言区 + 认领按钮）
    ├── ItemPublish.vue               ← 发布物品（类型选择 + 图片上传 + 分类下拉 + 表单校验）
    ├── Profile.vue                   ← 个人中心（我的发布 + 基本信息修改）
    └── admin/
        ├── UserManage.vue            ← 用户管理（列表 + 新增/编辑/删除，根据角色禁用按钮）
        ├── ItemAudit.vue             ← 物品审核（待审核列表 + 通过/驳回操作）
        ├── CategoryManage.vue        ← 分类管理（CRUD）
        └── OperationLog.vue          ← 操作日志（分页查询 + 条件筛选 + 超级管理员清理）
```

**关键决策**：前端路由由 **WebMvcConfig** 的 `ViewControllerRegistry` 做通配符转发 —— 这样用户按 F5 刷新 `/items/1` 不会 404，Spring Boot 会把它交给 Vue Router 去匹配。

### 3.3 数据库核心表关系

```
┌───────────────┐          ┌───────────────┐
│   sys_user    │1        n│   item_info   │
│ (用户表)       │──────────│ (物品信息表)    │
│ id (PK)       │          │ id (PK)       │
│ username      │          │ type(0/1)     │  0=失物 1=拾物
│ password(BCry)│          │ user_id(FK)   │  发布者
│ role(0/1/2)   │          │ category_id   │  → item_category
│ status        │          │ title         │
└───────────────┘          │ description   │
     │ 1                    │ images(JSON)  │
     │                      │ location      │
     │                      │ lost_time     │
     │                      │ status        │  0待 1已认领/找回 2下架
     │ n                    │ audit_status  │  0待审 1通过 2驳回
     ▼                      │ claim_user_id │  → sys_user(认领人)
┌───────────────┐          │ claim_time    │
│ admin_operation│          └───────────────┘
│      _log      │1                   │ n
│ (操作日志)      │                    ▼
│ id (PK)       │          ┌───────────────┐
│ admin_id      │          │ item_comment  │
│ operation_type│          │ (留言表)       │
│ operation_mod │          │ id (PK)       │
│ target_id     │          │ item_id       │  → item_info
│ create_time   │          │ user_id       │  → sys_user
└───────────────┘          │ content       │
                            │ parent_id     │  0=顶层, >0=回复某条
                            └───────────────┘
                                    │ 1
                                    ▼
                            ┌───────────────┐
                            │ item_category │
                            │ (物品分类表)   │
                            │ id (PK)       │
                            │ category_name │  如：证件类、电子产品
                            │ category_code │  如：ID_CARD
                            │ sort_order    │
                            │ status        │
                            └───────────────┘
```

---

## 4. 关键设计决策（为什么这样，不是那样）

### 4.1 物品的「失物」和「招领」为什么不拆两张表？

- **答案**：两者字段几乎一致（标题、描述、图片、地点、联系方式……），唯一差别是语义。用一个 `type` 字段（0=失物，1=拾物）区分即可，减少 JOIN 和重复代码。
- **代码体现**：`ItemInfo.java` 中 `type`；前端 `ItemList.vue` 顶部 tab 切换 type；后端 `ItemInfoServiceImpl` 按 type 查询。

### 4.2 为什么要「管理员审核」？

- **答案**：校园场景涉及个人信息（学生证、宿舍号、手机号），必须防止恶意发布。审核流程让信息先到管理员那再公开。
- **代码体现**：`item_info.audit_status` 字段（0 待审核 / 1 通过 / 2 驳回）；`GET /api/items` 公共列表接口默认只返回 `audit_status=1` 的数据；`PUT /api/items/{id}/audit` 仅管理员可调用。

### 4.3 为什么用三种角色而不是两种？

| 角色 | 权限范围 | 设计目的 |
| :--- | :--- | :--- |
| **超级管理员 (role=2)** | 完整权限 + 删除用户 + 清理日志 | 项目维护者，掌握最高权力 |
| **普通管理员 (role=1)** | 审核物品、管理学生用户、查看日志 | 日常运营角色，不会误伤核心数据 |
| **学生用户 (role=0)** | 发布、查看、留言、认领 | 终端用户 |

关键逻辑在 `SysUserServiceImpl`：普通管理员**不能**修改其他管理员、**不能**删除任何用户；物品 `status=1` 变更时必须有 `claimUserId`，防止随意篡改。

### 4.4 密码传输与存储的三层防护

```
用户输入 123456
      ↓ 【前端 encrypt.js】 XOR + Base64  → 防中间人抓包看到明文
    ENC:xxxxxxx
      ↓ 【后端 PasswordCrypto.java】 XOR 解密 + Base64 解码
      123456
      ↓ 【后端 BCrypt】 哈希（加盐）  → 数据库只存 $2a$10$...，即使 DB 泄露也不可逆
    存入 sys_user.password
```

### 4.5 Token 的「可主动失效」设计

- JWT 本身是**自包含、不可撤销**的 —— 一旦签发，在过期前始终有效。
- 为了支持「管理员强制踢人下线」「用户主动登出」，引入 Redis 作为 Token 白名单（或黑名单）。
- 但 Redis 并非必需：`RedisTokenStoreServiceImpl` 里做了降级 —— 连不上 Redis 时直接忽略，退化为纯 JWT 校验。
- 课程设计演示场景可以不装 Redis；如果要演示「登出后 Token 立即失效」，启动 Redis 即可。

### 4.6 前端路由刷新不会 404 的关键

`WebMvcConfig` 中：

```java
String[] frontRoutes = { "/login", "/register", "/items/**", "/admin/**", ... };
for (String route : frontRoutes) {
    registry.addViewController(route).setViewName("forward:/index.html");
}
```

Spring Boot 收到 `GET /items/1` 时，会把它**内部转发**到 `/index.html` —— 由 Vue Router 去解析 URL 并展示详情页，浏览器不会 404。

### 4.7 操作日志的「自动清理」

- `AdminOperationLogServiceImpl.scheduledCleanOldLogs()` 配合 `@Scheduled(cron = "0 0 2 * * ?")`：每天凌晨 2 点清理 30 天前的日志。
- 超级管理员也可以手动调用 `DELETE /api/admin/logs/clean?daysBefore=30` 立即清理。
- 目的：防止日志无限增长占用磁盘。

---

## 5. 一个请求的完整生命周期（以「发布物品」为例）

```
① 前端: 用户在 ItemPublish.vue 填完表单 → 点击「发布」
   item.js: publishItem({ type: 0, categoryId: 1, title: "...", ... })

② request.js 请求拦截器: 自动加 Header Authorization: Bearer <token>

③ 后端 Spring Security:
   JwtAuthenticationFilter 解析 Token → 获取 userId + role → 写入 SecurityContext
   SecurityConfig: /api/items 需要登录 → 通过

④ ItemInfoController.createItem(@RequestBody ItemInfoRequest request, Authentication auth):
   → 从 auth 里拿到 currentUserId
   → 调用 itemInfoService.create(request, userId)

⑤ ItemInfoServiceImpl.create():
   → Entity 转换：request → ItemInfo
   → 设置 audit_status = 0（待审核）
   → 设置 user_id, create_time, update_time（MetaObjectHandler 自动填充）
   → itemInfoMapper.insert(item)
   → 记录操作日志（仅管理员操作会写日志；学生发布物品暂不记）
   → 返回 ItemInfo

⑥ ItemInfoController: Result.success(item) → JSON

⑦ 前端 Axios 响应拦截器: code=200 → Element Plus ElMessage.success("发布成功，等待管理员审核")
   → 路由跳转到「我的发布」页面
```

---

## 6. 数据模型核心字段（摘选）

### sys_user（用户）
| 字段 | 类型 | 含义 | 取值示例 |
| :--- | :--- | :--- | :--- |
| id | BIGINT | 主键 | 1 |
| username | VARCHAR(50) | 用户名（唯一） | admin |
| password | VARCHAR(200) | BCrypt 哈希 | `$2a$10$N9qo8uLO...` |
| role | TINYINT | 角色 | 0 学生 / 1 管理员 / 2 超级管理员 |
| status | TINYINT | 状态 | 0 禁用 / 1 正常 |
| create_time | DATETIME | 创建时间（自动填充） | NOW() |

### item_info（物品）
| 字段 | 类型 | 含义 | 取值示例 |
| :--- | :--- | :--- | :--- |
| type | TINYINT | 类型 | 0 失物 / 1 拾物 |
| category_id | BIGINT | 分类 | 1 → 证件类 |
| audit_status | TINYINT | 审核状态 | 0 待审核 / 1 通过 / 2 驳回 |
| status | TINYINT | 业务状态 | 0 待认领/找回 / 1 已认领/找回 / 2 下架 |
| claim_user_id | BIGINT | 认领人 | 仅 status=1 时非空 |

### item_category（分类）
- 9 个预设分类（证件类、电子产品、衣物、书籍、生活用品、钥匙、卡券类、箱包类、其他）
- 字段：`category_name` + `category_code` + `sort_order` + `status`

### admin_operation_log（操作日志）
- 字段：`admin_id` / `operation_type(CREATE/UPDATE/DELETE/AUDIT/LOGIN/...)` / `operation_module` / `target_id` / `ip_address` / `result` / `create_time`

---

## 7. 可以改进的地方（留给下一个接手人）

| 方向 | 当前状态 | 改进建议 |
| :--- | :--- | :--- |
| **JWT 密钥** | 硬编码在 `JwtTokenUtil.java` | 改到 `application.properties`，生产环境用环境变量注入 |
| **密码加密密钥** | 硬编码在 `PasswordCrypto.java` | 同上，或替换为 HTTPS 传输后无需这一层 |
| **Redis Token 存储** | 仅基础支持 | 可以加「单用户同时只允许 N 个 Token 在线」 |
| **文件上传** | 本地磁盘 `uploads/` 目录 | 生产环境建议接入 OSS/S3 对象存储 |
| **前端构建** | 需手动执行 `npm run build` 并复制到 `static/` | 可用 Maven 插件在 `mvn package` 阶段自动执行 |
| **操作日志** | 仅管理员操作记日志 | 可扩展记录学生用户的关键操作（如发布、认领）便于追溯 |
| **数据统计** | 暂无仪表盘页面 | 可加一个首页数据看板（今日发布数、累计认领数、热门分类等） |
| **单元测试** | 当前是接口级自动化测试示例 | 可以补充 JUnit + Mockito 单元测试覆盖 Service 层 |

---

## 8. 文件索引（快速查找关键实现）

| 功能点 | 后端核心文件 | 前端核心文件 |
| :--- | :--- | :--- |
| 登录认证 | `security/SecurityConfig.java`, `security/JwtTokenUtil.java`, `service/impl/AuthServiceImpl.java` | `views/Login.vue`, `utils/encrypt.js`, `utils/request.js` |
| 物品发布/列表 | `controller/ItemInfoController.java`, `service/impl/ItemInfoServiceImpl.java` | `views/ItemPublish.vue`, `views/ItemList.vue`, `api/item.js` |
| 物品审核 | `ItemInfoServiceImpl.auditItem()` | `views/admin/ItemAudit.vue` |
| 物品认领/状态变更 | `ItemInfoServiceImpl.changeItemStatus()` | `views/ItemDetail.vue` |
| 用户管理/角色限制 | `service/impl/SysUserServiceImpl.java` (角色越权拦截) | `views/admin/UserManage.vue` |
| 分类管理 | `service/impl/ItemCategoryServiceImpl.java` | `views/admin/CategoryManage.vue` |
| 留言 | `service/impl/ItemCommentServiceImpl.java` | `views/ItemDetail.vue` (底部留言区) |
| 操作日志 + 清理 | `service/impl/AdminOperationLogServiceImpl.java` (含 @Scheduled) | `views/admin/OperationLog.vue` |
| 图片上传 | `controller/FileUploadController.java`, `config/WebMvcConfig.java` | `views/ItemPublish.vue` (el-upload 组件) |
| 前端路由转发 | `config/WebMvcConfig.java` (addViewControllers) | `router/index.js` |
| 数据库初始化 | `resources/sql/init_database.sql` | - |
