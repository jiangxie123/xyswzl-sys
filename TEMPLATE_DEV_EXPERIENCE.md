# 校园失物招领系统 · 开发经验模板（可复用）

> 📍 **用途**：把本次开发验证过的**规则/流程/步骤/踩坑**沉淀下来，下次开发同技术栈（Spring Boot + Vue 3 + MySQL 8）的项目时，直接复制这份文件作为起步基线。
>
> 📍 **适用范围**：课程设计、中小型管理系统、展示型 Web 应用（不包含微服务/大数据/高并发场景）
>
> 📍 **在 TRAE 中复用**：本文件与 `user_profile.md` / `project_memory.md` 中的「硬约束 + 工程约定」配合使用。下次开新项目前把本文件放到项目根目录，或让 AI 读取并对齐。

---

## 0. 一句话经验总结

> **先搭数据库 → 再建后端骨架 → 做前端基础工程 → 分模块前后端并行开发 → 每个模块做完立即自检 → 收尾清理测试代码 + 写 README + 提交**
>
> 关键是：**规则先行、文档先行、测试先行**；别边写边改数据库字段和接口定义，否则后期返工很痛。

---

## 1. 技术栈选型（硬约束，不要变）

| 层级 | 技术 | 版本 | 为什么选它 |
| :--- | :--- | :--- | :--- |
| Java | JDK | 17 | LTS，Spring Boot 3.x 起要求；IDEA 原生支持 |
| 后端 | Spring Boot | 4.1.0 或 3.2.x | 生态最成熟，starter 可以少写大量配置代码 |
| ORM | MyBatis-Plus | 3.5.7 | BaseMapper + IService + 分页插件，CRUD 零手写 |
| 权限 | Spring Security | 6.x | 框架自带，过滤器链清晰；配合 JWT 无状态 |
| Token | JWT | 0.12.6 (jjwt) | 轻量、自包含；配合 Redis 做主动失效 |
| Token 存储 | Redis | 可选 | 放 Token；不可用时自动降级为纯 JWT |
| 数据库 | MySQL | 8.0.x | utf8mb4 支持 emoji；课程设计最通用 |
| 前端 | Vue | 3.4.x + script setup | Composition API 代码组织更清爽 |
| 前端构建 | Vite | 5.x | 冷启动快；dev + build 都用它 |
| UI 组件 | Element Plus | 2.7.x | 组件覆盖齐全，中文文档好 |
| 路由 | Vue Router | 4.x | 守卫 + 动态路由支持管理后台 |
| 状态 | Pinia | 2.x | API 简单，可持久化 Token |
| HTTP | Axios | 最新 | 请求拦截器统一加 Token；响应拦截器统一提示 |

> ❌ **不要选**：JSP、jQuery、Vue 2、Angular（学习曲线高）、MyBatis 原生（无插件，手写太多）、PostgreSQL（课设场景 MySQL 文档最多）

---

## 2. 项目目录规范（新项目直接照抄）

```
项目根目录/
├── src/main/java/com/你的包名/
│   ├── common/                      ← 公共类
│   │   ├── Result.java              ← 统一返回 {code, message, data}
│   │   ├── ResultCode.java          ← 状态码常量/枚举
│   │   └── PageResult.java          ← 分页返回（records/total/size/current/pages）
│   ├── config/                      ← 配置类
│   │   ├── CorsConfig.java          ← 跨域（前端 5173 → 后端 8080）
│   │   ├── MyBatisPlusConfig.java   ← 分页插件 + MetaObjectHandler 配置
│   │   ├── GlobalExceptionHandler.java ← 全局异常捕获 → Result
│   │   └── WebMvcConfig.java        ← 静态资源 + 前端路由转发（通配符 /**）
│   ├── controller/                  ← REST API（按模块拆）
│   ├── service/ + service/impl/     ← 业务逻辑（接口 + 实现分离）
│   ├── mapper/                      ← MyBatis-Plus Mapper
│   ├── entity/                      ← 数据库实体（每张表一个）
│   ├── dto/request/                 ← 请求 DTO（含参数校验 @NotBlank/@Min/@Max）
│   ├── dto/response/                ← 响应 DTO（裁剪字段，不暴露 entity）
│   ├── security/                    ← 认证鉴权
│   │   ├── SecurityConfig.java      ← 权限规则（permitAll/authenticated/hasRole）
│   │   ├── JwtTokenUtil.java        ← JWT 生成/解析/校验
│   │   ├── JwtAuthenticationFilter.java ← 每个请求解析 Token → SecurityContext
│   │   └── CurrentUserDetails.java  ← 当前登录用户信息封装（id/username/role）
│   ├── util/                        ← 工具类（密码加密、文件处理…）
│   └── 项目Application.java         ← 启动类（含 @EnableScheduling）
│
├── src/main/resources/
│   ├── application.properties       ← 数据库/Redis/Token 过期/上传目录
│   ├── sql/init_database.sql        ← 建表 + 初始数据（含 BCrypt 密码）
│   └── static/                      ← 前端构建产物（npm run build 后复制到这里）
│       ├── index.html
│       ├── assets/*.js / *.css
│       └── images/login-bg.jpg / app-bg.jpg（可选）
│
├── frontend/                        ← Vue 3 前端
│   ├── src/
│   │   ├── views/                   ← 页面（登录 + 业务页 + 管理员页）
│   │   ├── router/index.js          ← 路由 + 登录守卫 + 角色守卫
│   │   ├── stores/user.js           ← Pinia 保存 token/用户/角色
│   │   ├── utils/
│   │   │   ├── request.js           ← Axios 封装（拦截器 + baseURL='/api'）
│   │   │   ├── encrypt.js           ← 密码 XOR+Base64 加密
│   │   │   └── captcha.js           ← 前端 Canvas 验证码
│   │   ├── api/                     ← 按模块拆 API 请求（auth/user/item/category...）
│   │   ├── App.vue
│   │   └── main.js
│   ├── index.html
│   ├── vite.config.js               ← 代理 /api → http://localhost:8080
│   └── package.json
│
├── pom.xml
├── .gitignore                       ← 必须忽略 target/、node_modules/、uploads/、.idea/
├── CHANGELOG.md                     ← 每次提交的核心功能变更记录（语义化版本）
├── PROJECT_GUIDE.md                 ← 设计文档（字段/接口规范）
├── README.md                        ← 启动说明 + 默认账号
├── DESIGN.md                        ← 架构设计说明（交付用）
└── DEMO_GUIDE.md                    ← 演示路线（交付用）
```

---

## 3. 数据库设计规则（必须遵守）

### 3.1 字段硬约束

**每张表必须有**：
```sql
create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP        ← 创建时间（自动填充）
update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  ← 更新时间
create_by    BIGINT                                              ← 创建人 id（可选）
update_by    BIGINT                                              ← 更新人 id（可选）
```

**字符集必须是**：
```sql
CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
```

### 3.2 状态枚举硬约束（核心之一！）

| 含义 | 0 | 1 | 2 |
| :--- | :--- | :--- | :--- |
| 角色 role | 学生/普通用户 | 管理员 | 超级管理员 |
| 用户状态 status | 禁用 | 正常 | —— |
| 物品审核 audit_status | 待审核 | 通过 | 驳回 |
| 物品状态 status | 待处理/待认领 | 已认领/找回 | 已下架 |
| 分类/留言 status | 禁用 | 正常 | —— |
| 操作日志 result | 失败 | 成功 | —— |

> ✅ **代码里用数字、文档里写含义**。枚举/常量放一份在文档里，代码里用 `0/1/2` 即可，不强行引入 enum 类（避免 mybatis-plus 额外配置）。

### 3.3 表设计推荐顺序

1. **用户表** `sys_user`（username 唯一、password BCrypt、role、status）
2. **核心业务主表** 如 `item_info`（包含类型、归属人、状态、审核状态）
3. **字典/分类表** 如 `item_category`（category_code 唯一 + 排序）
4. **明细表/留言表** 如 `item_comment`（parent_id 支持层级）
5. **操作日志表** `admin_operation_log`（operation_type + operation_module + target_id + ip）

### 3.4 常见坑

| 坑 | 解决办法 |
| :--- | :--- |
| 数据库里中文变 `???` | JDBC URL 必须加 `useUnicode=true&characterEncoding=utf8`，数据库/表/字段都是 utf8mb4 |
| `username` 不区分大小写导致重复注册 | 加 `UNIQUE KEY uk_username(username)` 唯一索引 |
| 连表查询忘记加索引导致慢 | 外键字段（user_id / category_id / item_id / audit_status + type 组合）建普通索引 |
| 发布时间显示有时区差 | MySQL `serverTimezone=Asia/Shanghai`，Java 实体用 `LocalDateTime`，前端直接 `new Date(iso)` 展示 |

---

## 4. 后端开发规则（必须遵守）

### 4.1 统一返回格式

```java
// Result.java（线程安全）
{
  "code": 200,         // 200 成功；400 参数错误；401 未认证；403 无权；404 不存在；500 异常
  "message": "操作成功",
  "data": { ... }       // 对象 / 数组 / null
}
```

分页必须返回：`records`、`total`、`size`、`current`、`pages`（5 个字段，便于前端 Element Plus 分页组件通用）。

### 4.2 HTTP 方法与路径

| 动作 | HTTP 方法 | 路径示例 | 备注 |
| :--- | :--- | :--- | :--- |
| 查询列表 | `GET` | `/api/items?page=1&size=10&keyword=xx` | 无 Token 也可查的公开数据 |
| 查询单条 | `GET` | `/api/items/{id}` | |
| 新增 | `POST` | `/api/items` | Body 是 JSON 对象 |
| 修改 | `PUT` | `/api/items/{id}` | Body 是 JSON 对象 |
| 删除 | `DELETE` | `/api/items/{id}` | 逻辑删除或物理删除都可以 |
| 状态变更（非 CRUD） | `PUT` | `/api/items/{id}/audit` 或 `.../status` | 用子路径，语义清晰 |
| 登录/退出 | `POST` | `/api/auth/login` / `/api/auth/logout` | 不用 Token（登录）/需要 Token（退出） |

**路径规则**：所有 API 必须以 `/api/` 开头；模块名使用复数（users / items / categories）。

### 4.3 权限校验（写在哪里很重要）

- **公开接口**（列表/详情/分类/登录/注册）：`SecurityConfig` 里 `permitAll()`
- **需要登录的接口**（发布/我的信息）：`authenticated()`，在 Controller 里从 `SecurityContextHolder` 拿 `userId`
- **管理员接口**（审核/用户管理/日志）：Controller 层或 Service 层 `if (currentRole < 1) throw new BusinessException(403, "无权")`
- **超级管理员接口**（删除用户/清理日志）：同上，判断 `role == 2`
- **归属校验**（只能改自己发布的物品）：Service 层 `if (!existing.getUserId().equals(currentUserId) && !isAdmin) throw 403`

> ✅ **建议**：角色越权逻辑放 Service 层（易复用），认证拦截放 Filter/Controller 层（统一入口）。

### 4.4 Service 层标准方法模板（照抄即可）

```
getById(id) → 查单条
getPage(page, size, keyword, ...) → 分页列表
create(request, userId) → 新增（参数校验、填充默认值、记录操作日志）
update(id, request, userId) → 修改（先查存在性 → 校验归属/权限 → 更新）
delete(id, userId) → 删除（先查存在性 → 校验权限 → 删除）
changeStatus(id, status, userId) → 状态变更（如认领/下架）
audit(id, status, remark, adminId) → 管理员审核（仅 role>=1 可执行）
```

### 4.5 参数校验

- 在 `@RequestBody` 的 DTO 字段上用 `@NotBlank` / `@Size(max=100)` / `@Min(0) @Max(2)`
- Controller 方法参数加 `@Valid` 注解
- `GlobalExceptionHandler` 捕获 `MethodArgumentNotValidException` → 提取第一条错误消息 → 返回 `Result.error(400, 消息)`

### 4.6 密码安全（三层防护）

```
前端提交          encrypt.js: XOR 加密 + Base64 编码 → payload: {username, password: "ENC:xxx"}
                   ↓
后端接收          PasswordCrypto.decrypt(password) → 明文
                   ↓
后端存储          BCryptPasswordEncoder.encode(明文) → 哈希存入 sys_user.password
                   ↓
登录验证          BCryptPasswordEncoder.matches(用户输入解密后, DB中的哈希) → true/false
```

- 关键密钥放在 `PasswordCrypto.java` 里（和 `encrypt.js` 必须一致）
- `init_database.sql` 里的密码是**示例 BCrypt**；生成真实密码用代码跑 `new BCryptPasswordEncoder().encode("你的密码")`

### 4.7 JWT 与 Redis

- Token 结构：`{sub=userId, role=0/1/2, username=xx, exp=now+24h}`
- 传输：前端每次请求 Header 带 `Authorization: Bearer <token>`
- Redis 存一份 `token:<userId> -> value`，带 TTL；登出时删除
- 没启动 Redis 时：`try { redisTemplate.opsForValue().get(...) } catch (Exception e) { /* 降级到纯 JWT */ }`

### 4.8 操作日志记录时机

只在**管理员关键操作**上记录：新增用户、删除用户、审核物品、管理分类、清理日志。学生日常发布/留言**不记**，避免冗余。记录字段：admin_id / admin_name / operation_type / operation_module / operation_desc / target_id / ip_address / result / create_time。

### 4.9 定时任务

```java
@EnableScheduling               // 加在启动类
@Scheduled(cron = "0 0 2 * * ?") // 每天 02:00
public void scheduledCleanOldLogs() { cleanOldLogs(30); }
```

---

## 5. 前端开发规则（必须遵守）

### 5.1 项目初始化

```bash
npm create vite@latest frontend -- --template vue
cd frontend
npm install
npm install vue-router@4 pinia element-plus @element-plus/icons-vue axios
npm run dev
```

### 5.2 Vite 配置（`vite.config.js`）

```js
server: {
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true }
  }
}
```

这样开发期 `http://localhost:5173/api/xxx` → 转发到后端 `8080`；生产期构建产物放在 `src/main/resources/static/`，直接通过 `http://localhost:8080/` 访问。

### 5.3 Axios 封装（`utils/request.js`）

```js
import axios from 'axios'
const request = axios.create({ baseURL: '/api', timeout: 10000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token') || userStore.token
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code === 401) {
      ElMessage.warning('登录已过期，请重新登录')
      userStore.clearToken()
      router.push('/login')
      return Promise.reject(data)
    }
    if (data.code !== 200) {
      ElMessage.error(data.message || '操作失败')
      return Promise.reject(data)
    }
    return data.data          // 前端直接拿到 data，不用写 .data.data
  },
  err => { ElMessage.error(err.message || '网络异常'); return Promise.reject(err) }
)
```

### 5.4 路由与守卫（`router/index.js`）

```js
routes = [
  { path: '/login', component: Login, meta: { requiresAuth: false } },
  { path: '/items', component: ItemList, meta: { requiresAuth: false } },
  { path: '/items/:id', component: ItemDetail, meta: { requiresAuth: false } },
  { path: '/publish', component: ItemPublish, meta: { requiresAuth: true } },
  { path: '/profile', component: Profile, meta: { requiresAuth: true } },
  { path: '/admin/users', component: UserManage, meta: { requiresAuth: true, roles: [1,2] } },
  { path: '/admin/items-audit', component: ItemAudit, meta: { requiresAuth: true, roles: [1,2] } },
  { path: '/admin/logs', component: OperationLog, meta: { requiresAuth: true, roles: [1,2] } },
]
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !userStore.isLoggedIn) next('/login')
  else if (to.meta.roles && !to.meta.roles.includes(userStore.role)) {
    ElMessage.warning('无权访问'); next('/items')
  }
  else next()
})
```

### 5.5 WebMvcConfig 通配符路由转发

**前端路由刷新不丢页的关键**：后端必须把 `/items/**`、`/admin/**`、`/login`、`/register` 等所有前端路由转发到 `index.html`。

```java
registry.addViewController("/login").setViewName("forward:/index.html");
registry.addViewController("/items/**").setViewName("forward:/index.html");
registry.addViewController("/admin/**").setViewName("forward:/index.html");
registry.addViewController("/profile").setViewName("forward:/index.html");
```

### 5.6 前端构建与部署

```bash
cd frontend
npm install    # 首次或 package.json 变更时
npm run build  # 构建输出到 frontend/dist/
```

然后把 `frontend/dist/` 的 `index.html` 和 `assets/` 整个目录复制到 `src/main/resources/static/`。

> 常见坑：`index.html` 引用路径必须是**相对路径**（`./assets/xxx.js`），否则在非根路径（如 `http://xxx/demo/`）部署时资源 404。

---

## 6. 开发流程标准步骤（新项目照抄）

### 第 0 步：立项与设计（必须做，代码量为 0）

产出物：`PROJECT_GUIDE.md` 草稿，包含：
- 项目概述（做什么、给谁用）
- 角色划分（几种身份、各自能做什么）
- 功能结构图（至少 5 个主要模块）
- 技术选型表（对照上面第 1 节）
- 数据库初步设计（表清单 + 字段 + 状态枚举）
- API 接口清单（至少 10 个核心接口）

> ⚠️ **不要跳过**：数据库字段和 API 路径在这一步确定后，后面别再大改；否则前后端双方都要返工。

### 第 1 步：初始化后端骨架

1. 用 IDEA 创建 Spring Boot 项目（Spring Initializr），选依赖：Spring Web、Spring Security、Validation、Lombok
2. 手动在 `pom.xml` 补加：MyBatis-Plus、MySQL connector、jjwt（JWT）、Spring Data Redis（可选）
3. 写 `application.properties`（数据库/Redis/Token 过期/JDBC URL 注意 utf8mb4 + Asia/Shanghai）
4. 写 `common/` 三件套（Result / ResultCode / PageResult）
5. 写 `config/` 三件套（CorsConfig / MyBatisPlusConfig + MetaObjectHandler / GlobalExceptionHandler）
6. 手动建一张最简单的表（用户表），连上数据库测试 → 启动 Spring Boot，确认无报错
7. 写一个测试 Controller（如 `GET /api/health`）测试返回 `{code:200, message:"ok", data:null}`

### 第 2 步：编写完整数据库脚本 `init_database.sql`

按顺序：建库（`DROP IF EXISTS + CREATE`）→ 建表（每张表都带 `create_time / update_time / create_by / update_by`）→ 初始化数据（管理员账号 + 分类等）。

**密码注意**：`sys_user.password` 必须是 BCrypt 哈希值。生成方式：在 Java 里临时执行 `System.out.println(new BCryptPasswordEncoder().encode("admin123"))` 拿值贴到 SQL。

### 第 3 步：后端认证模块（JWT + Security）

**必须做的文件**：
- `security/JwtTokenUtil.java`（生成/解析 token，secret 写死在代码里）
- `security/JwtAuthenticationFilter.java`（每个请求解析 Header 中的 `Bearer xxx`）
- `security/SecurityConfig.java`（禁用 CSRF、设置过滤器顺序、放行 `/api/auth/**`、`/api/health`、静态资源等）
- `security/CurrentUserDetails.java`（简单 POJO：id/username/role）
- `service/AuthService.java` + `impl/AuthServiceImpl.java`（登录：解密密码 → BCrypt matches → 生成 token；注册：校验唯一性 → 加密 → 插入）
- `controller/AuthController.java`（`POST /api/auth/login` / `POST /api/auth/register`）

**此时用 Postman 测试**：POST `/api/auth/login` body 填 `{username:"admin", password:"ENC:xxx加密后"}` → 得到 `{code:200, data:{token:"..."}}`。

### 第 4 步：后端 CRUD 模块（按业务表依次写）

**每张表标准 6 个文件**：
- `entity/Xxx.java`（加 `@TableName("xxx")` + 字段 `@TableId(type=AUTO)`）
- `mapper/XxxMapper.java`（extends `BaseMapper<Xxx>`）
- `dto/request/XxxRequest.java`（字段 + 校验注解）
- `service/XxxService.java`（接口定义方法）
- `service/impl/XxxServiceImpl.java`（extends `ServiceImpl<XxxMapper, Xxx>` implements `XxxService`）
- `controller/XxxController.java`（REST + `@Valid` + 权限判断）

写一张表 → 用 Postman 测试 5 个接口（列表/单条/新增/修改/删除）→ 确认通过 → 进入下一张表。

### 第 5 步：前端基础工程

1. `npm create vite` → 装 router/pinia/element-plus/axios
2. 写 `main.js`（注册 Element Plus + Pinia + Router）
3. 写 `utils/request.js`（axios 封装 + 拦截器 + 401 跳登录）
4. 写 `stores/user.js`（Pinia：token / username / role / 登录态）
5. 写 `router/index.js`（基本路由结构 + 登录守卫）
6. 写一个最简 `views/Login.vue`（用户名 + 密码 + 验证码 + 登录按钮）
7. 测试：前端登录 → 拿到 token → 调用一个需要登录的接口

### 第 6 步：前端业务页面（和后端第 4 步并行）

推荐的实现顺序（由易到难）：
1. 列表页（`ItemList.vue`）：表格 + 搜索 + 分页
2. 详情页（`ItemDetail.vue`）：展示信息 + 留言区（留言作为第二个模块）
3. 发布页（`ItemPublish.vue`）：表单 + 图片上传
4. 个人中心（`Profile.vue`）：我的发布列表
5. 管理员：用户管理（`UserManage.vue`）
6. 管理员：物品审核（`ItemAudit.vue`）
7. 管理员：分类管理（`CategoryManage.vue`）
8. 管理员：操作日志（`OperationLog.vue`）

### 第 7 步：自检（每个模块做完立即做）

做完一个模块后按此清单检查：

- [ ] 接口返回格式符合 `{code, message, data}`
- [ ] HTTP 方法使用正确（GET/POST/PUT/DELETE）
- [ ] 未登录访问受限接口 → 401
- [ ] 学生访问管理员接口 → 403
- [ ] 参数为必填项但不传 → 400（有友好提示）
- [ ] 数据库写入的数据正确（真实进 MySQL 看）
- [ ] 前端页面正常展示（列表/分页/搜索/提交都试过）
- [ ] 控制台无红色报错、无 `console.log` 残留调试信息
- [ ] 本次模块的测试代码/临时文件已删除（不要留在项目里）

### 第 8 步：收尾与交付

1. 前端 `npm run build` 并把 `dist/` 复制到 `src/main/resources/static/`
2. 在 IDEA 启动后端，直接访问 `http://localhost:8080/` 验证全流程
3. 写 `README.md`（启动说明 + 默认账号 + API 速查）
4. 写 `DESIGN.md`（架构设计说明，给评审看）
5. 写 `DEMO_GUIDE.md`（演示路线）
6. 检查 `CHANGELOG.md` 是否最新
7. `git status` 确认没有漏提交文件 → `git add -A && git commit && git push`
8. GitHub 仓库页检查：README 渲染正常、文件都在
9. 交付一句话：「项目已完成，访问 http://localhost:8080/ 用 admin / 123456 登录可查看完整功能」

---

## 7. 关键文件模板片段（可直接复制）

### 7.1 `Result.java`

```java
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    public static <T> Result<T> success() { return success(null); }
    public static <T> Result<T> success(T data) { Result<T> r = new Result<>(); r.code=200; r.message="操作成功"; r.data=data; return r; }
    public static <T> Result<T> error(int code, String message) { Result<T> r = new Result<>(); r.code=code; r.message=message; return r; }
}
```

### 7.2 `MetaObjectHandler`（自动填充时间/操作人）

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

实体类字段加注解：
```java
@TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
@TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
```

### 7.3 `GlobalExceptionHandler`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBiz(BusinessException e) { return Result.error(e.getCode(), e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.error(400, msg);
    }
    @ExceptionHandler(Exception.class)
    public Result<?> handleAll(Exception e) { e.printStackTrace(); return Result.error(500, "服务器内部错误"); }
}
```

### 7.4 Security 中从 JWT 拿当前用户 ID

```java
// 在 Controller 里
Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof CurrentUserDetails user) {
        return user.getId();
    }
    return null;
}
```

---

## 8. 常见问题与解决方案（踩坑记录）

| 问题 | 原因 | 解决办法 |
| :--- | :--- | :--- |
| 启动 Spring Boot 报数据库连接失败 | MySQL 未启动 / 用户名密码错 / URL 缺 `allowPublicKeyRetrieval=true` | 先 cmd `netstat -ano | findstr 3306`；再检查 `application.properties` 的 jdbc URL 和 username/password |
| 登录始终返回"密码错误" | 数据库存的是明文不是 BCrypt 哈希 | 用 `new BCryptPasswordEncoder().encode("你的密码")` 生成正确值写进 `init_database.sql` 并重新执行；或前端没调用 encrypt.js |
| 其他接口 401（虽然已登录） | 请求 Header 没带 `Authorization: Bearer <token>`，或 Token 过期/格式错 | Axios 请求拦截器里自动加；检查 Token 有效期（建议 24 小时） |
| 刷新页面 404（前端路由） | Spring Boot 不认识 Vue Router 的路径 | 在 `WebMvcConfig.addViewControllers()` 里加通配符转发 `/**` → `forward:/index.html` |
| 图片上传后看不到 | 静态资源未映射；或上传目录写入失败 | 在 `WebMvcConfig.addResourceHandlers()` 里把 `/uploads/**` 映射到本地绝对路径，上传目录用 `Files.createDirectories` 启动时创建 |
| 前端 `npm run build` 后刷新空白或资源 404 | Vite 默认 `base: '/'`，部署到子路径时要改 `base` | 课设一般根路径部署，不改 base；关键是 `index.html` 里 `./assets/xxx.js` 是相对路径 |
| `git commit` 时出现大量 LF/CRLF 警告 | Windows 默认 CRLF，Linux/仓库默认 LF | 忽略即可；或加 `.gitattributes` 统一 `* text=auto eol=lf` |
| MyBatis-Plus 分页查询 total 永远是 0 | 未配置 `MybatisPlusInterceptor + PaginationInnerInterceptor` | 在 `MyBatisPlusConfig.java` 里注册这个 Bean |
| 角色判断失效（学生可以访问管理员页） | Security 的 GrantedAuthority 没正确设置；或前端路由守卫没判断 role | `JwtAuthenticationFilter` 解析 Token 后注入 `SimpleGrantedAuthority("ROLE_ADMIN")` 等；前端 `to.meta.roles` 判断 |
| 操作日志表无限增长 | 没做清理 | 加 `@Scheduled(cron="0 0 2 * * ?")` 每天 02:00 清理 30 天前的数据；超级管理员可手动触发 |

---

## 9. 可复用的 git 命令速查

```bash
# 本地提交
git status                    # 看当前状态（哪些文件改了）
git add -A                    # 暂存所有变更（也可用 git add 文件名）
git commit -m "feat: 新增 xxx 模块"
git log -5 --oneline          # 看最近 5 个提交

# 推送到远程
git push -u origin master     # 第一次推送 master 并建立跟踪
git push                      # 后续直接 push

# 拉取别人的修改
git pull

# 回退/撤销
git reset --soft HEAD~1       # 撤销最后一次提交（文件改动保留）
git checkout -- 文件名         # 丢弃某个文件的修改
```

---

## 10. 交付文档清单（验收必须有）

| 文件名 | 给谁看 | 内容要点 |
| :--- | :--- | :--- |
| `README.md` | 接手开发的同学 + 老师评审 | 如何启动、默认账号、目录结构、API 速查 |
| `DESIGN.md` | 老师评审 + 同学理解架构 | 架构图、设计决策、关键实现位置索引 |
| `DEMO_GUIDE.md` | 自己演示用 + 答辩展示 | 演示路线、注意事项、屏幕切换顺序 |
| `PROJECT_GUIDE.md` | 自己开发时参考（**不要**删除） | 字段枚举规范、API 设计规范、开发路线图 |
| `CHANGELOG.md` | 任何关心历史变更的人 | 语义化版本号 + 每次提交核心功能 + 文件链接 |
| `init_database.sql` | 新环境部署时执行 | 建库建表 + 初始数据（**必须**可独立运行） |

---

## 11. 一句话金句（核心经验浓缩）

1. **数据库设计决定整个项目上限** —— 表结构和字段越早定越好，中间别动。
2. **三层架构（Controller → Service → Mapper）是骨架** —— 别跳过，哪怕是小项目。
3. **统一返回格式 `Result<T>` + 统一异常处理** —— 前端对接成本会降一半。
4. **Security + JWT 是权限基石** —— 但 Filter 和 GrantedAuthority 一定要调试通。
5. **前端路由刷新不 404 是必做项** —— WebMvcConfig 通配符转发是最小成本方案。
6. **测试代码/调试信息一定要在交付前清理** —— 否则答辩现场会翻车。
7. **先写后端接口 + Postman 测 → 再写前端页面** —— 两边并行开发，接口契约先行。
8. **不要边学边改技术选型** —— 选好 Spring Boot + Vue 3 就一路走到底。
9. **每个独立模块做完立即做自检清单** —— 别等到最后一起修。
10. **写文档/注释/提交信息** —— 代码写给三个月后的自己看，别偷懒。

---

> **模板文件结束**。下次开新项目时：复制此文件 → 把 `xyswzl` / 项目名替换为新项目代号 → 在 `PROJECT_GUIDE.md` 中按本模板的第 6 节执行。
