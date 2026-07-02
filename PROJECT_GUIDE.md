# 校园失物招领系统 · 项目指导文档

> 📖 **重要提示**：本文件是整个项目的核心指导文档。
> 当开发过程中出现偏离、疑问或需要回顾整体方案时，请优先阅读此文件。
> 所有技术选型、架构设计、开发步骤、API 规范均以此文件为准。

---

## 目录

1. [项目概述](#1-项目概述)
2. [系统角色划分](#2-系统角色划分)
3. [功能结构图](#3-功能结构图)
4. [系统架构图](#4-系统架构图)
5. [技术选型](#5-技术选型)
6. [数据库设计](#6-数据库设计)
7. [后端分层目录结构](#7-后端分层目录结构)
8. [开发路线图（分步确认）](#8-开发路线图分步确认)
9. [API 接口规范](#9-api-接口规范)
10. [开发流程与验证标准](#10-开发流程与验证标准)
11. [常见问题与回正轨](#11-常见问题与回正轨)

---

## 1. 项目概述

### 1.1 项目名称
校园失物招领系统（xyswzl-sys）

### 1.2 项目目标
提供一个校园场景下的失物发布/招领发布、信息匹配、留言交流、管理员审核的完整平台。

### 1.3 核心功能
- 用户发布寻物/拾物信息
- 用户查看、搜索、筛选物品信息
- 用户留言交流
- 管理员审核信息、管理用户
- 管理员查看操作日志

### 1.4 项目位置
```
d:\dai\Projectinformation\xyswzl-sys\
├── src/main/java/com/mzy/xyswzlsys/    # Java 后端代码
├── src/main/resources/                  # 配置文件 + SQL
│   ├── application.properties          # Spring Boot 配置
│   └── sql/init_database.sql           # 数据库初始化脚本
├── frontend/                           # Vue 3 前端代码（vite 项目）
├── pom.xml                             # Maven 依赖管理
└── PROJECT_GUIDE.md                    # 本文件（项目指导）
```

---

## 2. 系统角色划分

| 角色 | role 字段值 | 权限范围 |
| :--- | :--- | :--- |
| 学生用户 | 0 | 发布寻物/拾物、查看列表、留言、认领、管理个人信息 |
| 普通管理员 | 1 | 审核物品信息、下架违规内容、管理用户（部分）、查看操作日志 |
| 超级管理员 | 2 | 包含普通管理员所有权限 + 系统配置、分类管理、角色管理、数据备份 |

---

## 3. 功能结构图

```
校园失物招领系统
├── 用户模块
│   ├── 用户注册 / 登录（JWT Token）
│   ├── 个人信息管理（修改头像、联系方式、学院）
│   └── 我的发布（查看、编辑、删除）
│
├── 物品分类模块（管理员）
│   ├── 分类列表
│   ├── 新增分类
│   ├── 编辑分类
│   └── 删除分类
│
├── 物品信息模块（核心）
│   ├── 发布寻物信息
│   ├── 发布拾物信息
│   ├── 物品列表（按类型/分类/状态筛选、关键词搜索）
│   ├── 物品详情查看
│   ├── 编辑物品信息
│   ├── 删除物品信息
│   └── 状态变更（认领成功/找回/下架）
│
├── 审核模块（管理员）
│   ├── 待审核列表
│   ├── 审核通过
│   ├── 审核驳回（填写原因）
│   └── 审核记录查询
│
├── 留言模块
│   ├── 发布留言
│   ├── 查看留言列表
│   ├── 删除留言（作者/管理员）
│   └── 回复留言
│
└── 系统管理（管理员）
    ├── 用户管理（列表、禁用/启用）
    ├── 操作日志（按管理员/时间/模块查询）
    └── 数据统计（发布数量/认领数量等）
```

---

## 4. 系统架构图

### 4.1 整体架构（前后端分离）

```
┌───────────────────────────────────────────────────────────────┐
│                         前端（Vue 3）                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   用户端     │  │   管理端     │  │ Element Plus │         │
│  │ (学生用户)   │  │ (管理员)     │  │   UI 组件    │         │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘         │
│         │                  │                                     │
│  ┌──────┴──────────────────┴──────┐                              │
│  │        Axios HTTP 调用层         │                              │
│  └────────────────┬─────────────────┘                              │
└───────────────────┼───────────────────────────────────────────────┘
                    │ HTTPS + JWT 携带 Authorization Header
┌───────────────────▼───────────────────────────────────────────────┐
│                      后端（Spring Boot 4.1.0）                       │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │  Controller  │  │   Service    │  │    Mapper    │               │
│  │ (REST API)   │  │ (业务逻辑)   │  │ (MyBatis-Plus)│               │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘               │
│         │                  │                  │                       │
│  ┌──────┴──────────────────┴──────────────────┴──────┐                │
│  │            中间件 / 工具层                           │                │
│  │  JWT 认证  │  Spring Security  │  全局异常处理        │                │
│  └───────────────────────────────────────────────────────┘                │
└───────────────────┬───────────────────────────────────────────────┘
                    │ JDBC
┌───────────────────▼───────────────────────────────────────────────┐
│                      MySQL 8.0 数据库                              │
│  数据库名：xyswzl_db                                                │
│  字符集：utf8mb4_unicode_ci                                        │
└───────────────────────────────────────────────────────────────────┘
```

### 4.2 请求处理流程

```
前端请求（带 JWT Token）
       │
       ▼
  JwtAuthenticationFilter（校验 Token）
       │
       ▼
  Spring Security（权限校验）
       │
       ▼
  Controller（接收参数 → 调用 Service）
       │
       ▼
  Service（业务逻辑 → 调用 Mapper）
       │
       ▼
  Mapper（MyBatis-Plus → 数据库 CRUD）
       │
       ▼
  MySQL（返回数据）
       │
       ▼
  返回统一结果 Result<T>（JSON）
```

---

## 5. 技术选型

| 分类 | 技术 | 版本 | 用途 |
| :--- | :--- | :--- | :--- |
| 后端框架 | Spring Boot | 4.1.0 | 核心框架（已在 pom.xml 中） |
| 构建工具 | Maven | 3.x | 依赖管理 |
| Java 版本 | JDK | 17 | 开发语言 |
| 持久层 | MyBatis-Plus | 3.5.7 | 简化 CRUD 操作 |
| 安全认证 | Spring Security + JWT | 6.x | 登录认证与权限控制 |
| 数据库 | MySQL | 8.0+ | 数据持久化 |
| JSON 工具 | Jackson | 内置于 Spring Boot | JSON 序列化 |
| Lombok | Lombok | 最新 | 简化 POJO 写法 |
| 前端框架 | Vue | 3.x | 用户界面 |
| 前端 UI | Element Plus | 最新 | UI 组件库 |
| 前端构建 | Vite | 6.x | 开发与构建 |
| 前端路由 | Vue Router | 4.x | 页面路由 |
| 前端状态 | Pinia | 最新 | 全局状态管理 |
| 前端 HTTP | Axios | 最新 | API 调用 |

---

## 6. 数据库设计

### 6.1 数据库信息
- **数据库名**：`xyswzl_db`
- **字符集**：`utf8mb4`
- **排序规则**：`utf8mb4_unicode_ci`
- **初始化脚本**：`src/main/resources/sql/init_database.sql`

### 6.2 数据表清单

| 表名 | 用途 | 关键字段 |
| :--- | :--- | :--- |
| `sys_user` | 用户表 | id, username, password, real_name, role(0/1/2), status |
| `item_category` | 物品分类表 | id, category_name, category_code, sort_order, status |
| `item_info` | 物品信息表（寻物/拾物统一） | id, type(0/1), user_id, category_id, title, description, images, location, lost_time, status, audit_status, claim_user_id |
| `item_comment` | 留言表 | id, item_id, user_id, content, parent_id, status |
| `admin_operation_log` | 操作日志表 | id, admin_id, admin_name, operation_type, operation_module, target_id, ip_address, result, create_time |

### 6.3 状态枚举规范

**重要！所有状态字段必须遵循以下枚举值，严禁在代码中散落魔法数字！**

| 字段 | 值 | 含义 |
| :--- | :--- | :--- |
| `sys_user.role` | 0 | 学生用户 |
| `sys_user.role` | 1 | 普通管理员 |
| `sys_user.role` | 2 | 超级管理员 |
| `sys_user.status` | 0 | 禁用 |
| `sys_user.status` | 1 | 正常 |
| `item_info.type` | 0 | 寻物（丢失物品） |
| `item_info.type` | 1 | 拾物（捡到物品） |
| `item_info.status` | 0 | 待找回/待认领 |
| `item_info.status` | 1 | 已找回/已认领 |
| `item_info.status` | 2 | 已下架 |
| `item_info.audit_status` | 0 | 待审核 |
| `item_info.audit_status` | 1 | 审核通过 |
| `item_info.audit_status` | 2 | 审核驳回 |
| `item_category.status` | 0 | 禁用 |
| `item_category.status` | 1 | 启用 |
| `item_comment.status` | 0 | 禁用 |
| `item_comment.status` | 1 | 正常 |
| `admin_operation_log.result` | 0 | 操作失败 |
| `admin_operation_log.result` | 1 | 操作成功 |

### 6.4 默认账号
| 用户名 | 密码 | 角色 | 说明 |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | 超级管理员 (role=2) | 开发测试用，生产环境务必修改 |
| `manager` | `admin123` | 普通管理员 (role=1) | 开发测试用 |
| `student01` | `admin123` | 学生用户 (role=0) | 开发测试用 |
| `student02` | `admin123` | 学生用户 (role=0) | 开发测试用 |

> ⚠️ **密码注意**：`init_database.sql` 中的加密密码为示例值。实际使用时需通过 `BCryptPasswordEncoder` 生成正确的加密值写入数据库。
> 生成方式：`new BCryptPasswordEncoder().encode("admin123")`

---

## 7. 后端分层目录结构

```
src/main/java/com/mzy/xyswzlsys/
│
├── XyswzlSysApplication.java           # Spring Boot 启动类（已存在）
│
├── common/                              # 公共组件
│   ├── Result.java                     # 统一返回结果
│   ├── ResultCode.java                 # 状态码枚举
│   └── PageResult.java                 # 分页返回结果
│
├── config/                              # 配置类
│   ├── MyBatisPlusConfig.java          # MyBatis-Plus 配置
│   ├── CorsConfig.java                 # 跨域配置
│   └── GlobalExceptionHandler.java     # 全局异常处理
│
├── security/                            # 安全认证
│   ├── SecurityConfig.java             # Spring Security 配置
│   ├── JwtTokenUtil.java               # JWT 生成/解析工具
│   ├── JwtAuthenticationFilter.java    # JWT 过滤器
│   ├── UserDetailsServiceImpl.java     # 用户详情服务
│   └── SecurityUser.java               # 已认证用户封装
│
├── entity/                              # 数据库实体类（每张表对应一个）
│   ├── SysUser.java
│   ├── ItemCategory.java
│   ├── ItemInfo.java
│   ├── ItemComment.java
│   └── AdminOperationLog.java
│
├── dto/                                 # 数据传输对象
│   ├── request/                        # 请求 DTO
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── SysUserRequest.java
│   │   ├── ItemCategoryRequest.java
│   │   ├── ItemInfoRequest.java
│   │   └── ItemCommentRequest.java
│   └── response/                       # 响应 DTO
│       ├── LoginResponse.java
│       ├── SysUserResponse.java
│       ├── ItemCategoryResponse.java
│       ├── ItemInfoResponse.java
│       └── ItemCommentResponse.java
│
├── mapper/                              # 数据访问层（MyBatis-Plus）
│   ├── SysUserMapper.java
│   ├── ItemCategoryMapper.java
│   ├── ItemInfoMapper.java
│   ├── ItemCommentMapper.java
│   └── AdminOperationLogMapper.java
│
├── service/                             # 业务逻辑层
│   ├── SysUserService.java
│   ├── ItemCategoryService.java
│   ├── ItemInfoService.java
│   ├── ItemCommentService.java
│   ├── AdminOperationLogService.java
│   ├── AuthService.java                # 登录/注册/登出
│   └── impl/                            # Service 实现类
│       ├── SysUserServiceImpl.java
│       ├── ItemCategoryServiceImpl.java
│       ├── ItemInfoServiceImpl.java
│       ├── ItemCommentServiceImpl.java
│       ├── AdminOperationLogServiceImpl.java
│       └── AuthServiceImpl.java
│
└── controller/                          # REST API 控制层
    ├── AuthController.java             # 认证接口（登录/登出）
    ├── SysUserController.java          # 用户管理 CRUD
    ├── ItemCategoryController.java     # 分类管理 CRUD
    ├── ItemInfoController.java         # 物品信息 CRUD
    ├── ItemCommentController.java      # 留言 CRUD
    └── AdminLogController.java         # 操作日志查询
```

---

## 8. 开发路线图（分步确认）

> **开发原则**：每完成一个阶段，由用户确认后再进入下一个阶段。

---

### 🎯 **第一阶段：后端基础框架**

**目标**：让 Spring Boot 能正常连接 MySQL，具备统一的返回格式、基础依赖就绪

**工作内容**：

| 编号 | 任务 | 说明 |
| :--- | :--- | :--- |
| 1.1 | 修改 `pom.xml` | 添加 MyBatis-Plus、Spring Security、JWT、Lombok、FastJSON 等依赖 |
| 1.2 | 创建 `common/` 包 | `Result.java`（统一返回）、`ResultCode.java`（状态码）、`PageResult.java`（分页） |
| 1.3 | 创建 `config/` 包 | `CorsConfig.java`（跨域）、`GlobalExceptionHandler.java`（全局异常）、`MyBatisPlusConfig.java` |
| 1.4 | 创建 `entity/` 包 | 5 个实体类，使用 MyBatis-Plus 注解 `@TableName`、`@TableId`、`@TableField` |
| 1.5 | 创建 `mapper/` 包 | 5 个 Mapper 接口，继承 `BaseMapper<T>` |
| 1.6 | 配置 `application.properties` | 添加 MyBatis-Plus 配置、Mapper XML 扫描路径 |
| 1.7 | 验证启动 | 启动 Spring Boot，确认无报错，数据库连接正常 |

**验证标准**：
- IDEA 中 Run `XyswzlSysApplication`，控制台无错误输出
- 日志中看到 MySQL 连接成功、HikariCP 池初始化成功
- 访问 `http://localhost:8080` 返回正常（404 表示框架正常工作）

---

### 🎯 **第二阶段：用户模块 CRUD + 登录鉴权**

**目标**：实现用户的增删改查、登录认证、JWT Token 机制

**工作内容**：

| 编号 | 任务 | 说明 |
| :--- | :--- | :--- |
| 2.1 | 创建 DTO | `LoginRequest`、`SysUserRequest`、`SysUserResponse`、`LoginResponse` |
| 2.2 | 创建 `SysUserService` + 实现类 | 新增、修改、删除、分页查询、单条查询 |
| 2.3 | 创建 `SysUserController` | REST API：POST / PUT / DELETE / GET |
| 2.4 | 实现 JWT 工具 | `JwtTokenUtil.java`：生成 Token、解析 Token、校验 Token |
| 2.5 | 实现 Spring Security 配置 | `SecurityConfig.java` + `JwtAuthenticationFilter.java` |
| 2.6 | 创建 `AuthService` + `AuthController` | 登录、登出接口 |
| 2.7 | 测试所有接口 | 使用 Postman / APIFox 验证 |

**API 接口清单（第二阶段）**：

| Method | Path | 功能 | 需要 Token | 角色限制 |
| :--- | :--- | :--- | :--- | :--- |
| POST | `/api/auth/login` | 登录 | 否 | - |
| POST | `/api/auth/register` | 注册（可选） | 否 | - |
| GET | `/api/users` | 分页查询用户列表 | 是 | 管理员 |
| GET | `/api/users/{id}` | 查询单个用户 | 是 | 管理员/本人 |
| POST | `/api/users` | 新增用户 | 是 | 管理员 |
| PUT | `/api/users/{id}` | 修改用户 | 是 | 管理员/本人 |
| DELETE | `/api/users/{id}` | 删除用户 | 是 | 超级管理员 |

**验证标准**：
- 使用 Postman POST `/api/auth/login` → 返回 `{"code":200, "data":{"token":"..."}}`
- 使用 GET `/api/users`（不带 Token）→ 返回 401 未认证
- 使用 GET `/api/users`（Header 带 `Authorization: Bearer <token>`）→ 返回用户列表
- 完整测试 6 个用户接口，CRUD 操作数据库数据正确

---

### 🎯 **第三阶段：物品分类 + 物品信息 CRUD**

**目标**：实现物品分类管理、物品信息（寻物/拾物）完整 CRUD + 审核

**工作内容**：

| 编号 | 任务 |
| :--- | :--- |
| 3.1 | 物品分类 DTO + Service + Controller |
| 3.2 | 物品信息 DTO + Service + Controller |
| 3.3 | 物品审核接口（通过/驳回） |
| 3.4 | 物品状态变更接口（认领/找回/下架） |
| 3.5 | 物品列表筛选/搜索功能 |
| 3.6 | 测试所有物品相关接口 |

**API 接口清单（第三阶段）**：

| Method | Path | 功能 | Token |
| :--- | :--- | :--- | :--- |
| GET | `/api/categories` | 分类列表 | 否（公开） |
| POST | `/api/categories` | 新增分类 | 是（管理员） |
| PUT | `/api/categories/{id}` | 修改分类 | 是（管理员） |
| DELETE | `/api/categories/{id}` | 删除分类 | 是（管理员） |
| GET | `/api/items` | 物品列表（支持 type/category/status/keyword 筛选） | 否（公开） |
| GET | `/api/items/{id}` | 物品详情 | 否（公开） |
| POST | `/api/items` | 发布物品信息 | 是（登录用户） |
| PUT | `/api/items/{id}` | 修改物品信息 | 是（作者/管理员） |
| DELETE | `/api/items/{id}` | 删除物品信息 | 是（作者/管理员） |
| PUT | `/api/items/{id}/audit` | 审核物品 | 是（管理员） |
| PUT | `/api/items/{id}/status` | 变更物品状态（认领/找回/下架） | 是（作者/管理员） |
| GET | `/api/items/mine` | 查看我的发布 | 是（本人） |

---

### 🎯 **第四阶段：留言模块 + 操作日志**

**目标**：实现物品详情页下的留言功能、管理员操作日志记录

**工作内容**：

| 编号 | 任务 |
| :--- | :--- |
| 4.1 | 留言 DTO + Service + Controller |
| 4.2 | 新增留言、查询留言列表、删除留言 |
| 4.3 | 留言支持回复（parent_id 机制） |
| 4.4 | 操作日志 Service：管理员关键操作自动记录日志 |
| 4.5 | 操作日志 Controller：分页查询日志（筛选：管理员/模块/类型/时间） |
| 4.6 | 测试留言和日志接口 |

**API 接口清单（第四阶段）**：

| Method | Path | 功能 | Token |
| :--- | :--- | :--- | :--- |
| GET | `/api/comments/item/{itemId}` | 查询某物品的留言列表 | 否（公开） |
| POST | `/api/comments` | 发布留言 | 是（登录用户） |
| DELETE | `/api/comments/{id}` | 删除留言 | 是（作者/管理员） |
| GET | `/api/admin/logs` | 分页查询操作日志 | 是（管理员） |

---

### 🎯 **第五阶段：前端项目初始化**

**目标**：搭建 Vue 3 + Vite 项目，配置路由、状态管理、HTTP 请求层

**工作内容**：

| 编号 | 任务 |
| :--- | :--- |
| 5.1 | 使用 Vite 创建 Vue 3 项目 |
| 5.2 | 安装 Element Plus、Vue Router、Pinia、Axios |
| 5.3 | 配置项目基础结构（views / components / router / stores / utils / api） |
| 5.4 | 配置 Axios 请求拦截器（自动携带 Token）、响应拦截器（统一处理错误） |
| 5.5 | 配置 Vue Router，创建登录页与主页面路由 |
| 5.6 | 配置 Pinia store：用户登录态、Token 管理 |
| 5.7 | 创建主布局（顶部导航 + 侧边栏 + 主内容区） |

**前端目录结构**：

```
frontend/
├── src/
│   ├── views/                      # 页面
│   │   ├── Login.vue              # 登录页
│   │   ├── Layout.vue             # 主布局
│   │   ├── ItemList.vue           # 物品列表
│   │   ├── ItemDetail.vue         # 物品详情
│   │   ├── ItemPublish.vue        # 发布物品
│   │   ├── Profile.vue            # 个人中心
│   │   └── admin/                 # 管理员页面
│   │       ├── UserManage.vue
│   │       ├── ItemAudit.vue
│   │       ├── CategoryManage.vue
│   │       └── OperationLog.vue
│   ├── components/                 # 可复用组件
│   ├── router/                     # 路由
│   │   └── index.js
│   ├── stores/                     # Pinia 状态管理
│   │   └── user.js
│   ├── utils/                      # 工具函数
│   │   └── request.js             # Axios 封装
│   ├── api/                        # API 请求
│   │   ├── auth.js
│   │   ├── user.js
│   │   ├── item.js
│   │   └── comment.js
│   ├── App.vue
│   └── main.js
├── index.html
├── vite.config.js
└── package.json
```

---

### 🎯 **第六阶段：前端页面开发**

**目标**：按优先级逐步实现所有页面

| 阶段 | 页面 | 优先级 |
| :--- | :--- | :--- |
| 6.1 | 登录页面 | 高 |
| 6.2 | 物品列表页（首页） | 高 |
| 6.3 | 物品详情页 + 留言 | 高 |
| 6.4 | 发布物品页 | 高 |
| 6.5 | 注册页面 | 中 |
| 6.6 | 个人中心（我的发布 + 修改资料） | 中 |
| 6.7 | 管理员：用户管理 | 中 |
| 6.8 | 管理员：物品审核 | 中 |
| 6.9 | 管理员：分类管理 | 低 |
| 6.10 | 管理员：操作日志 | 低 |

---

## 9. API 接口规范

### 9.1 统一返回格式 `Result<T>`

所有接口必须返回以下统一 JSON 格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { /* 业务数据 */ }
}
```

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `code` | Integer | 状态码（200=成功，其他=错误） |
| `message` | String | 提示信息 |
| `data` | T / null | 业务数据（对象或数组或 null） |

### 9.2 统一状态码

| code | 含义 | 场景 |
| :--- | :--- | :--- |
| 200 | 成功 | 查询/新增/修改/删除成功 |
| 400 | 请求参数错误 | 参数校验失败 |
| 401 | 未认证 | Token 缺失或无效 |
| 403 | 无权限 | 角色无权操作 |
| 404 | 资源不存在 | 记录不存在 |
| 409 | 业务冲突 | 用户名已存在等 |
| 500 | 服务器错误 | 异常情况 |

### 9.3 分页返回格式 `PageResult<T>`

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [ /* 当前页数据数组 */ ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

### 9.4 Token 使用规范

- 所有需要登录的请求，必须在 HTTP Header 中携带：
  ```
  Authorization: Bearer <token>
  ```
- 登录接口 `/api/auth/login` 返回的 `data.token` 字段即为 JWT Token
- Token 过期后返回 401，前端应自动跳转到登录页

### 9.5 HTTP Method 规范

| Method | 用途 |
| :--- | :--- |
| GET | 查询（列表 / 详情） |
| POST | 新增 |
| PUT | 修改 |
| DELETE | 删除 |

### 9.6 API 路径规范

- 所有后端 API 以 `/api/` 开头
- 模块名使用复数形式：`/api/users`、`/api/items`、`/api/categories`
- ID 使用路径参数：`/api/users/{id}`
- 查询参数放在 URL Query String：`/api/items?type=0&categoryId=1&page=1&size=10`

---

## 10. 开发流程与验证标准

### 10.1 每个功能模块的标准开发流程

1. **设计**：确定 API 路径、请求/响应结构 → 记录在本章节
2. **Entity/DTO**：创建实体类和请求/响应 DTO
3. **Mapper**：创建 Mapper 接口（继承 `BaseMapper<T>`）
4. **Service**：创建 Service 接口 + 实现类（调用 Mapper）
5. **Controller**：创建 Controller（调用 Service，暴露 REST API）
6. **测试**：使用 Postman / APIFox 手动测试所有接口
7. **确认**：通知用户检查功能 → 用户确认后进入下一个模块

### 10.2 测试验证清单

每个功能模块完成后，必须验证以下内容：

- [ ] 接口返回格式符合 `Result<T>` 规范
- [ ] 所有 HTTP Method 正确使用（GET/POST/PUT/DELETE）
- [ ] Token 认证生效（需要登录的接口在无 Token 时返回 401）
- [ ] 权限控制生效（学生用户无法访问管理员接口）
- [ ] 数据库数据正确写入/更新/删除
- [ ] 输入校验生效（非空、长度限制等）
- [ ] 异常情况有友好的错误提示（用户名已存在、记录不存在等）

### 10.3 代码风格规范

| 规范 | 要求 |
| :--- | :--- |
| 命名 | Java 使用小驼峰（`sysUser`），数据库使用下划线（`sys_user`），URL 使用 kebab-case（`/api/sys-users`） |
| 注释 | 每个类、每个公开方法必须有 Javadoc 注释说明用途 |
| 状态值 | 禁止在业务代码中直接使用 `0`、`1` 等魔法数字，必须使用枚举或常量 |
| 异常 | 使用 `GlobalExceptionHandler` 统一处理，禁止在 Controller 层使用 try-catch 吞异常 |
| 日志 | Service 层关键操作添加日志，使用 SLF4J `log.info()` / `log.error()` |

---

## 11. 常见问题与回正轨

### ❓ 问题 1：数据库连接失败
**现象**：启动 Spring Boot 时控制台报数据库连接错误

**排查步骤**：
1. 确认 MySQL 服务已启动
2. 检查 `application.properties` 中的 `url`、`username`、`password`
3. 确认数据库 `xyswzl_db` 已创建（执行 `init_database.sql`）
4. 确认 MySQL 端口是 3306，用户名/密码正确

**修正方式**：修改 `application.properties` 中的连接信息

---

### ❓ 问题 2：登录始终失败 / Token 无效
**现象**：登录接口返回成功，但后续接口仍返回 401

**排查步骤**：
1. 确认密码使用 BCrypt 正确加密后写入数据库
2. 检查请求 Header 中是否正确携带 `Authorization: Bearer <token>`
3. 检查 JWT 密钥（secret）在生成和解析时是否一致
4. 检查 JWT 是否过期

**修正方式**：在 `JwtTokenUtil.java` 中统一设置 secret 和过期时间

---

### ❓ 问题 3：分页查询不生效
**现象**：分页查询返回所有数据，total 正确但 records 未分页

**排查步骤**：
1. 确认 `MyBatisPlusConfig.java` 中正确配置了 `MybatisPlusInterceptor` 和 `PaginationInnerInterceptor`
2. 确认 Service 层使用 `Page<>(current, size)` 对象作为第一个参数

---

### ❓ 问题 4：前端无法调用后端接口（跨域问题）
**现象**：浏览器控制台报 CORS 错误

**排查步骤**：
1. 确认 `CorsConfig.java` 已创建并正确配置
2. 确认后端 `server.port=8080`，前端端口（如 5173）在允许列表中

---

### ❓ 问题 5：状态枚举值不一致
**现象**：数据库中 `status=1` 表示"正常"，但前端代码中按"禁用"处理

**排查步骤**：
1. **回到本文件第 6.3 节**，对照"状态枚举规范"检查
2. 检查 Entity/DTO/前端代码中的状态映射是否与规范一致
3. 严禁在代码中随意定义状态值，必须遵循本文件规范

---

### 🚨 偏离项目方向时如何回正轨

**当你不确定下一步该做什么、或发现技术选型可能有误时，请按以下步骤操作**：

1. **阅读本文件**：先通读整个 `PROJECT_GUIDE.md`，明确当前应在哪个阶段
2. **确认开发阶段**：查看 [第 8 节开发路线图](#8-开发路线图分步确认)，找到当前应进行的阶段
3. **对照架构检查**：查看 [第 7 节目录结构](#7-后端分层目录结构)，确认代码结构是否一致
4. **对照数据库检查**：查看 [第 6 节数据库设计](#6-数据库设计)，确认状态枚举值是否正确
5. **对照 API 规范**：查看 [第 9 节 API 规范](#9-api-接口规范)，确认接口设计是否统一
6. **如仍有疑问**：暂停编码，在需求层面确认清楚后再继续

---

## 14. 📌 项目状态记录

| 阶段 | 内容 | 状态 | 完成日期 | 备注 |
| :--- | :--- | :--- | :--- | :--- |
| 阶段 0 | 数据库模块（xyswzl_db + 5 张表 + 测试数据） | ✅ 已完成 | 2026-07-01 | 密码使用 BCrypt 加密存储 |
| 阶段 1 | 后端基础框架（Spring Boot + MyBatis-Plus + Security + JWT） | ✅ 已完成 | 2026-07-01 | 登录接口可正常返回 Token，MetaObjectHandler 已配置 |
| 阶段 1 | 前端基础框架（Vue 3 + Vite + Element Plus + Axios） | 🟡 进行中 | - | 需要搭建前端项目 |
| 阶段 1 | 前后端连通点（前端调用登录接口 → Token） | ⏳ 待完成 | - | 关键验证：前端能否成功登录 |
| 阶段 2 | 用户模块（后端用户 CRUD + 前端用户管理页） | ⏳ 待开始 | - | - |
| 阶段 3 | 物品分类 + 物品信息 CRUD + 审核 | ⏳ 待开始 | - | - |
| 阶段 4 | 留言模块 + 操作日志 | ⏳ 待开始 | - | - |

> **项目位置**：后端 `d:\dai\Projectinformation\xyswzl-sys\src\main\java\com\mzy\xyswzlsys\`，前端 `d:\dai\Projectinformation\xyswzl-sys\frontend\`
> **测试账号**：admin/123456（超级管理员），manager/123456，student01/123456，student02/123456
> **后端端口**：8080，**前端端口**：5173
>
> 本文件由项目开发者维护，每次阶段完成需更新上表。
> 任何技术变更需先在本文件记录后再修改代码。
