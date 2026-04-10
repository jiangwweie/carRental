# 租车应用 - 架构设计文档

## 1. 技术选型

| 层级 | 技术 | 版本 | 理由 |
|------|------|------|------|
| **小程序框架** | uni-app + Vue 3 | Vue 3.4+ | 一套代码输出微信小程序，国内生态好 |
| **小程序 UI** | uView Plus / 原生组件 | - | 移动端组件库，按需引入 |
| **PC 管理端** | Vue 3 + Element Plus | Element Plus 2.x | 成熟的后台组件库 |
| **构建工具** | Vite | 5.x | 极速 HMR |
| **状态管理** | Pinia | 2.x | Vue 3 官方推荐，TypeScript 友好 |
| **路由** | uni-app 路由 / Vue Router | 4.x | 各自内置 |
| **HTTP 客户端** | Axios | 1.x | 拦截器统一处理 token |
| **后端框架** | Java Spring Boot 3 | 3.2+ | 团队熟悉，生态成熟 |
| **ORM** | MyBatis-Plus | 3.5+ | 轻量，CRUD 自动生成，XML 映射灵活 |
| **微信 SDK** | WxJava (weixin-java-miniapp) | 4.6+ | 微信登录、订阅消息、支付全覆盖 |
| **数据库** | MySQL 8.0 | 8.0+ | 团队熟悉，JSON 字段满足扩展 |
| **迁移工具** | Flyway | - | Java 生态标准方案 |
| **认证** | JWT (jjwt) | 0.12+ | 无状态，小程序友好 |
| **图片存储** | 数据库 base64（MVP） | - | MVP 阶段简化，v1.5 迁移至 OSS |
| **部署** | Docker + Docker Compose | - | 一键部署，单机 |
| **反向代理** | Nginx | - | HTTPS 终止，PC 管理端静态文件 |

### 为什么选这套

- **Java 后端**：10 年 Java 经验，WxJava 是微信生态最成熟的 Java SDK
- **uni-app 小程序**：开发效率高，Vue 3 语法与 PC 管理端统一技术栈
- **Element Plus 管理端**：后台系统用成熟组件库，开发速度最快
- **小项目优先**：不引入 Redis、消息队列等复杂基础设施
- **单一服务器**：所有服务跑在一台 2C4G 云服务器上

---

## 2. 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    微信小程序 (用户端+管理简化版)           │
│                    uni-app + Vue 3                       │
│                                                          │
│  ┌──────────────────┐  ┌──────────────────────┐         │
│  │  用户端页面        │  │ 管理端简化页面         │         │
│  │  (首页/车辆/订单)  │  │ (仪表盘/订单管理)      │         │
│  │                  │  │                      │         │
│  │  底部 TabBar:     │  │  底部 TabBar:         │         │
│  │  首页 | 订单 | 我的│  │  订单 | 仪表盘         │         │
│  └────────┬─────────┘  └──────────┬───────────┘         │
│           │                       │                      │
│  (MVP: 通过 role 字段控制 Tab 可见性，    │              │
│   uni-app 限制待后续解决)               │              │
└────────────────────────┬────────────┼──────────────────┘
                         │ HTTPS
                         ▼
┌─────────────────────────────────────────────────────────┐
│                        Nginx (HTTPS)                     │
│  ┌────────────────────────┬──────────────────────────┐  │
│  │ /api/*                 │ /admin/*                  │  │
│  │ → Spring Boot (8080)   │ → PC 管理端静态文件       │  │
│  └────────────────────────┴──────────────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│              Spring Boot 3 (Backend)                      │
├──────────┬──────────┬──────────┬──────────┬─────────────┤
│ Auth 层  │ Vehicle  │ Order    │ Pricing  │ Agreement   │
│ JWT/微信  │ Service  │ Service  │ Engine   │ Service     │
├──────────┴──────────┴──────────┴──────────┴─────────────┤
│                    MyBatis-Plus ORM                       │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                    MySQL 8.0                              │
│  (车辆图片以 base64 存储在 vehicles 表)                    │
└─────────────────────────────────────────────────────────┘

外部服务:
  └── 微信 MiniApp (登录 + 订阅消息 + 支付 v1.5)

独立部署:
  ┌─────────────────────────────────────────┐
  │  PC 管理端 (Vue 3 + Element Plus)        │
  │  通过 Nginx 静态文件部署                  │
  │  调用 /api/* 接口                        │
  └─────────────────────────────────────────┘
```

### 分层架构（后端）— 领域驱动 + 模块化单体

采用四层架构，严格单向依赖：

```
controller/         → 接口适配层：REST 接口、参数校验、响应包装
application/        → 应用层：用例编排、DTO 转换、事务边界
domain/             → 领域层：聚合根、值对象、领域服务、Repository 接口
infrastructure/     → 基础设施层：Repository 实现、微信 SDK、外部服务
```

**依赖方向**（严格禁止反向依赖）：

```
controller → application → domain ← infrastructure
                          ↑
                    (定义接口)     (实现接口)
```

**各层职责**：

| 层 | 职责 | 示例 | 依赖 |
|---|------|------|------|
| **controller** | HTTP 适配、参数校验、权限注解 | `@RestController`, `@Valid` | → application |
| **application** | 用例编排、DTO↔Domain 转换、事务 | `@Transactional`, DTO 转换 | → domain |
| **domain** | 纯业务逻辑、聚合根、值对象、Repository 接口 | 订单状态机、定价引擎 | 无外部依赖 |
| **infrastructure** | 技术实现、持久化、外部服务封装 | MyBatis Mapper, WxJava 封装 | → domain |

**关键设计原则**：
- `domain/` 层 **不依赖任何框架**（无 Spring、无 MyBatis、无 Lombok 注解到聚合根）
- `domain/` 定义 `Repository` 接口，`infrastructure/` 实现该接口
- `domain/` 定义 `DomainService` 接口（如定价引擎），`infrastructure/` 提供具体实现
- 用例执行路径：controller → applicationService → domainService/aggregate → repository

---

### 领域模块划分

按业务领域拆分为 5 个模块，每个模块内部包含四层：

```
backend/src/main/java/com/carrental/
│
├── common/                         # 共享内核（不依赖任何业务模块）
│   ├── config/                     # 全局配置
│   ├── exception/                  # 全局异常处理
│   ├── result/                     # 统一响应包装
│   ├── security/                   # JWT 拦截器 + 角色拦截器
│   └── util/                       # 纯工具类
│
├── domain/                         # 领域层（按模块分包）
│   ├── user/
│   │   ├── User.java               # 聚合根
│   │   ├── UserRepository.java     # 接口
│   │   └── UserRole.java           # 值对象/枚举 (user/admin)
│   ├── vehicle/
│   │   ├── Vehicle.java            # 聚合根
│   │   ├── VehicleStatus.java      # 枚举 (active/inactive)
│   │   ├── VehicleRepository.java  # 接口
│   │   └── spec/
│   │       └── VehicleSpecs.java   # 查询规格
│   ├── order/
│   │   ├── Order.java              # 聚合根（含状态机）
│   │   ├── OrderStatus.java        # 枚举
│   │   ├── PriceBreakdown.java     # 值对象
│   │   ├── OrderRepository.java    # 接口
│   │   └── service/
│   │       └── OrderConflictChecker.java  # 领域服务
│   ├── pricing/
│   │   ├── PricingEngine.java      # 接口（v1.5 完整实现）
│   │   ├── PricingResult.java      # 值对象
│   │   └── HolidayCalendar.java    # 值对象
│   └── agreement/
│       ├── Agreement.java          # 聚合根
│       └── AgreementRepository.java # 接口
│
├── application/                    # 应用层（按用例分包）
│   ├── auth/
│   │   ├── WxLoginCommand.java     # 命令对象
│   │   └── AuthService.java        # 应用服务
│   ├── order/
│   │   ├── CreateOrderCommand.java
│   │   ├── CreateOrderResult.java
│   │   ├── OrderAppService.java    # 订单创建（含后端算价）
│   │   └── dto/
│   │       ├── OrderListItemDTO.java
│   │       └── OrderDetailDTO.java
│   ├── vehicle/
│   │   ├── VehicleAppService.java
│   │   └── dto/
│   │       ├── VehicleListItemDTO.java
│   │       └── VehicleDetailDTO.java
│   ├── pricing/
│   │   └── PricingAppService.java  # 价格估算服务
│   ├── dashboard/
│   │   └── DashboardAppService.java
│   └── agreement/
│       └── AgreementAppService.java
│
├── infrastructure/                 # 基础设施层（按领域分包）
│   ├── persistence/
│   │   ├── mapper/
│   │   │   ├── UserMapper.java
│   │   │   ├── VehicleMapper.java
│   │   │   ├── OrderMapper.java
│   │   │   └── ...
│   │   ├── dataobject/             # DO（数据库映射对象）
│   │   │   ├── UserDO.java
│   │   │   ├── VehicleDO.java
│   │   │   ├── OrderDO.java
│   │   │   └── ...
│   │   └── repository/             # Repository 实现
│   │       ├── UserRepositoryImpl.java
│   │       ├── VehicleRepositoryImpl.java
│   │       └── OrderRepositoryImpl.java
│   ├── wechat/
│   │   ├── WxMiniAppConfig.java    # 微信配置
│   │   ├── WxLoginService.java     # 微信登录封装
│   │   └── WxSubscribeService.java # 订阅消息封装
│   └── pricing/
│       └── SimplePricingEngine.java # MVP 简化定价（days × weekday_price）
│
└── controller/                     # 接口适配层
    ├── AuthController.java
    ├── VehicleController.java
    ├── OrderController.java
    ├── AdminOrderController.java
    ├── AdminVehicleController.java
    ├── PricingController.java      # 价格估算
    ├── DashboardController.java
    ├── AgreementController.java
    └── SubscriptionController.java
```

**模块并行开发策略**：

```
Phase 1: 基础设施搭建（串行，1-2 天）
  └─ common/ + domain 接口定义 + infrastructure 持久化框架

Phase 2: 领域模块并行（2-3 天）
  ├─ user 模块: 登录 + 用户管理
  ├─ vehicle 模块: CRUD + 上下架
  ├─ order 模块: 创建 + 状态机 + 冲突检测
  └─ pricing 模块: 简化定价引擎 (MVP)

Phase 3: 前后端联调（2-3 天）
  ├─ 小程序端: 登录 → 车辆列表 → 详情 → 下单 → 订单
  └─ PC 管理端: 仪表盘 → 车辆管理 → 订单管理
```

---

## 3. 项目目录结构

```
car-rental/
├── docker-compose.yml              # 一键部署
├── nginx.conf                      # Nginx 配置
│
├── backend/
│   ├── pom.xml                     # Maven 依赖
│   ├── src/
│   │   └── main/
│   │       ├── java/com/carrental/
│   │       │   ├── CarRentalApplication.java
│   │       │   ├── common/              # 共享内核
│   │       │   ├── domain/              # 领域层（无框架依赖）
│   │       │   ├── application/         # 应用层（用例编排）
│   │       │   ├── infrastructure/      # 基础设施层
│   │       │   └── controller/          # HTTP 适配层
│   │       └── resources/
│   │           ├── application.yml
│   │           ├── application-dev.yml
│   │           └── db/migration/
│   │               └── V1__init_tables.sql
│   └── Dockerfile
│
├── frontend-mini/                  # uni-app 小程序
│   ├── src/
│   │   ├── pages/
│   │   │   ├── login/              # 微信登录页
│   │   │   ├── index/              # 首页(车辆列表+取车点)
│   │   │   ├── vehicle-detail/     # 车辆详情(图片轮播)
│   │   │   ├── booking/            # 预订下单(日期选择+价格预览)
│   │   │   ├── orders/             # 我的订单列表
│   │   │   ├── order-detail/       # 订单详情(状态可视化)
│   │   │   ├── agreement/          # 用户协议
│   │   │   ├── profile/            # 个人中心 (US-20)
│   │   │   │
│   │   │   └── admin/              # 管理端简化版
│   │   │       ├── dashboard/      # 营收概览(简化) (US-24)
│   │   │       ├── orders/         # 订单管理 (US-23)
│   │   │       └── order-detail/   # 订单详情
│   │   │
│   │   ├── components/
│   │   │   ├── common/             # 通用组件
│   │   │   │   ├── EmptyState.vue  # 空状态引导 (US-25)
│   │   │   │   └── LoadingSkeleton.vue
│   │   │   └── vehicle/            # 车辆相关组件
│   │   │       ├── ImageCarousel.vue  # 图片轮播 (US-17)
│   │   │       └── VehicleCard.vue
│   │   │
│   │   ├── api/                    # API 请求封装
│   │   ├── store/                  # Pinia 状态
│   │   │   ├── auth.js             # 登录态管理 (US-21)
│   │   │   └── config.js           # 全局配置(取车地址等)
│   │   ├── utils/
│   │   │   ├── request.js          # Axios 封装(拦截器/token 刷新)
│   │   │   └── date.js             # 日期工具
│   │   ├── static/
│   │   ├── App.vue
│   │   ├── main.js
│   │   ├── manifest.json           # 小程序配置
│   │   └── pages.json              # 页面路由配置
│   │
│   ├── package.json
│   ├── vite.config.js
│   └── unocss.config.js
│
├── frontend-admin/                 # PC 管理端
│   ├── src/
│   │   ├── views/
│   │   │   ├── login/              # 管理员登录
│   │   │   ├── dashboard/          # 仪表盘
│   │   │   ├── vehicles/           # 车辆管理
│   │   │   ├── orders/             # 订单管理
│   │   │   ├── pricing/            # 价格设置
│   │   │   └── agreement/          # 协议管理
│   │   │
│   │   ├── components/             # 业务组件
│   │   ├── api/                    # API 请求
│   │   ├── store/                  # Pinia 状态
│   │   ├── router/                 # Vue Router
│   │   ├── utils/
│   │   ├── App.vue
│   │   └── main.js
│   │
│   ├── index.html
│   ├── vite.config.js
│   ├── package.json
│   └── tailwind.config.js
│
└── docs/
    ├── products/                   # 产品文档
    ├── arch/                       # 架构文档
    └── planning/                   # 进度日志
```

---

## 4. 前端页面架构（小程序端）

### 4.1 页面路由表

| 页面路径 | 文件路径 | 角色 | 说明 | 关联用户故事 |
|---------|---------|------|------|-------------|
| `/pages/login/index` | `pages/login/` | 公开 | 微信授权登录页，未登录自动跳转 | US-01, US-21 |
| `/pages/index/index` | `pages/index/` | user/admin | 首页：固定取车地址 + 车辆列表 | US-02, US-15 |
| `/pages/vehicle-detail/index` | `pages/vehicle-detail/` | user/admin | 车辆详情：图片轮播 + 价格 + 立即预订 | US-03, US-17 |
| `/pages/booking/index` | `pages/booking/` | user | 预订页：日期选择 + 取车指引 + 价格预览 + 协议勾选 | US-04, US-18, US-22 |
| `/pages/orders/index` | `pages/orders/` | user/admin | 我的订单列表：状态筛选 + 空状态引导 | US-05, US-25 |
| `/pages/order-detail/index` | `pages/order-detail/` | user/admin | 订单详情：状态可视化 + 价格明细 | US-05, US-19 |
| `/pages/agreement/index` | `pages/agreement/` | user/admin | 用户协议全文 | US-06 |
| `/pages/profile/index` | `pages/profile/` | user/admin | 个人中心：用户信息 + 退出登录 | US-20 |
| `/pages/admin/dashboard/index` | `pages/admin/dashboard/` | admin | 管理仪表盘：简化版营收概览 | US-08, US-24 |
| `/pages/admin/orders/index` | `pages/admin/orders/` | admin | 管理订单：筛选 + 快速操作 | US-12, US-23 |
| `/pages/admin/order-detail/index` | `pages/admin/order-detail/` | admin | 管理订单详情 | US-12 |

### 4.2 底部 TabBar 配置

**用户端 TabBar（默认）**：
| Tab | 路径 | 图标 | 说明 |
|-----|------|------|------|
| 首页 | `/pages/index/index` | home | 车辆列表 + 取车地址 |
| 订单 | `/pages/orders/index` | orders | 我的订单 |
| 我的 | `/pages/profile/index` | profile | 个人中心 |

**管理端 TabBar（admin 角色）**：
| Tab | 路径 | 图标 | 说明 |
|-----|------|------|------|
| 订单 | `/pages/admin/orders/index` | list | 订单管理（筛选+快捷操作） |
| 仪表盘 | `/pages/admin/dashboard/index` | chart | 营收概览 |

> **MVP 策略**：由于 uni-app 的 tabBar 不支持运行时动态切换，MVP 阶段采用以下方案：
> 1. 在 `pages.json` 中配置所有 Tab 页面
> 2. 登录时根据 `role` 字段，在 `App.vue` 的 `onLaunch` 中通过 `uni.setTabBarItem` 动态隐藏/显示对应 tab
> 3. 或者：不动态切换，用户端和管理端各自有独立的 tabBar，通过角色权限拦截页面访问
> 4. **待解决**：uni-app tabBar 动态切换的优雅方案留到 v1.5 优化

### 4.3 登录态管理（US-21）

```
小程序启动 (App.onLaunch)
  │
  ├─ 检查本地 token 是否存在
  │   ├─ 不存在 → 跳转到 /pages/login/index
  │   └─ 存在 → 解析 JWT 检查过期时间
  │       ├─ 已过期 → 清除本地 token → 跳转登录
  │       ├─ 临近过期（剩余 < 1 天）→ 调用 /auth/refresh 刷新 token
  │       └─ 有效 → 进入首页
  │
  └─ HTTP 拦截器：
      ├─ 请求头添加 Authorization: Bearer {token}
      ├─ 响应 401 → 清除 token → 跳转登录页
      └─ 响应 403 → 提示"无权访问"
```

**token 存储**：
- 使用 `uni.setStorageSync('token')` 持久化
- 使用 `uni.setStorageSync('user')` 存储用户基本信息（id, role, phone）
- 登录页实现：未登录时自动拦截到登录页，登录成功后 `uni.reLaunch` 到首页

### 4.4 关键页面说明

**首页（US-15, US-02）**：
- 顶部固定展示取车地址（硬编码，MVP 阶段写死在 `store/config.js`）
- 下方展示车辆列表（卡片式），显示封面图、车型、日租金
- 无 seats 筛选器，保持简单
- 车辆图片：列表页显示第一张图片（base64）

**车辆详情（US-03, US-17）**：
- 顶部图片轮播组件（`<swiper>`），展示多张 base64 图片
- 车型信息、参数、描述
- 底部"立即预订"按钮

**预订页（US-04, US-18, US-22）**：
- 日期选择器（取车日期 + 还车日期）
- 选择日期后自动调用 `POST /api/v1/pricing/estimate` 获取价格预览
- 展示价格明细（每天的价格构成）
- 取车指引区域（硬编码地址 + 简单的取车说明）
- 用户协议勾选
- 提交订单按钮（防抖 500ms）

**订单详情（US-19）**：
- 状态可视化：使用步骤条（Steps）组件展示订单状态流转
  ```
  待确认 → 已确认 → 进行中 → 已完成
                  ↓
               已取消 / 已拒绝
  ```
- 当前状态高亮显示，已完成/已拒绝/已取消状态显示灰色

**个人中心（US-20）**：
- 用户头像/昵称/手机号
- 快捷入口：我的订单、用户协议
- 退出登录按钮

**空状态（US-25）**：
- 订单列表为空时：展示空状态图 + 引导文案"还没有订单，快去租辆车吧" + "去看看车辆"按钮
- 管理端订单为空时：展示"暂无订单"提示

---

## 5. 微信集成方案

### 5.1 登录流程

```
小程序端                          后端                          微信服务器
  │                                │                                │
  │ 1. wx.login()                  │                                │
  │──────────────> 获取 loginCode   │                                │
  │                                │                                │
  │ 2. getUserPhoneNumber()        │                                │
  │──────────────> 获取 phoneCode   │                                │
  │                                │                                │
  │ 3. POST /api/v1/auth/wx-login  │                                │
  │    { loginCode, phoneCode }    │                                │
  │───────────────────────────────>│                                │
  │                                │ 4. code2Session(loginCode)     │
  │                                │───────────────────────────────>│
  │                                │     返回 openid, session_key    │
  │                                │<───────────────────────────────│
  │                                │                                │
  │                                │ 5. getPhoneNumber(phoneCode)   │
  │                                │───────────────────────────────>│
  │                                │     返回 phoneNumber            │
  │                                │<───────────────────────────────│
  │                                │                                │
  │                                │ 6. 查询/创建用户 (openid+phone)│
  │                                │                                │
  │ 7. 返回 JWT token + 用户信息    │                                │
  │<───────────────────────────────│                                │
```

使用 WxJava SDK 简化步骤 4 和 5：
```java
WxMaJscode2SessionResult session = wxMaService.getUserService().getSessionInfo(loginCode);
String openid = session.getOpenid();
String phone = wxMaService.getUserService().getUserPhoneNumber(phoneCode);
```

### 5.2 订阅消息流程

```
1. 用户下单时调用 wx.requestSubscribeMessage，获取授权
2. 后端记录授权到 message_subscriptions 表
3. 管理员确认/拒绝订单时，后端调用微信订阅消息 API 推送通知
```

---

## 6. 关键设计决策详解

### 6.1 图片存储方案（base64 in DB for MVP）

**MVP 方案（当前）**：
- 车辆图片以 base64 字符串存储在 `vehicles.images` JSON 字段中
- 数据库 `images` 字段类型：`JSON`，存储 base64 字符串数组
- 前端直接使用 base64 data URI 显示：`<image :src="'data:image/jpeg;base64,' + image" />`
- 优点：无需额外 OSS 服务，部署简单，单服务器搞定一切
- 缺点：数据库体积增大，带宽消耗高（每次 API 响应包含图片数据）

**v1.5 迁移方案（OSS）**：
- 将 base64 图片上传至阿里云 OSS / 腾讯云 COS
- 更新 `vehicles.images` 字段为 URL 数组
- 数据库字段名不变，仅内容从 base64 改为 URL
- 前端代码通过判断字符串是否以 `data:` 前缀来兼容两种格式

### 6.2 定价方案（简化 estimate → 完整 PricingEngine）

**MVP 方案（当前）**：
- `POST /api/v1/pricing/estimate` 接收 `vehicleId + startDate + endDate`
- 简化计算：租期天数 × `weekday_price`（不考虑周末/节假日差异）
- 订单创建时，后端同样方式计算 `totalPrice`，防止前端篡改
- `price_breakdown` 返回每日明细，但 MVP 阶段每日价格相同

**v1.5 完整方案（PricingEngine）**：
- 引入完整的 `PricingEngine` 接口实现
- 支持：工作日价格、周末价格、节假日价格（查 `holiday_configs` 表）
- 支持：优惠券折扣、会员折扣、保险加价
- 价格优先级：`fixed_price` > `weekend_price × multiplier` > `weekend_price` > `weekday_price`

### 6.3 订单列表/详情中的车辆信息获取

**两查询 + 应用层组合方案**：
1. 查询订单列表，获取 `vehicle_id` 列表
2. 批量查询车辆信息 `SELECT * FROM vehicles WHERE id IN (vehicle_ids)`
3. 在应用层（`OrderAppService`）将车辆信息 join 到订单 DTO 中
4. 优点：避免 N+1 查询，不依赖数据库 JOIN，保持领域层解耦
5. 订单详情同理：先查订单，再查车辆，组合后返回

### 6.4 取车地址

**MVP 方案**：
- 取车地址硬编码在前端 `store/config.js` 和后端配置中
- 预订页展示取车地址和简单指引（US-22）
- 管理端订单详情中也展示取车地址

**v1.5 可配置方案**：
- 在 `system_configs` 表中存储取车地址
- 提供管理端配置接口

---

## 7. 部署方案

### 7.1 基础设施

| 资源 | 规格 | 用途 |
|------|------|------|
| 云服务器 | 2C4G 40GB | 运行后端 + Nginx |
| 域名 | 1 个 | API + PC 管理端 |
| SSL 证书 | Let's Encrypt (免费) | HTTPS |
| 图片存储 | 数据库 base64（MVP） | 车辆图片（v1.5 迁至 OSS） |
| 微信小程序 | 企业/个体户主体 | 用户端 + 管理简化端 |

### 7.2 部署架构

```
用户 ──HTTPS──→ Nginx (80/443)
                    ├── /api/*        → Spring Boot (8080)
                    ├── /admin/*      → PC 管理端静态文件
                    └── 静态资源缓存

小程序端 ──HTTPS──→ 后端 API（直接请求 /api/*）
```

### 7.3 Docker Compose

```yaml
services:
  mysql:
    image: mysql:8.0
    volumes:
      - mysql_data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: car_rental

  backend:
    build: ./backend
    depends_on:
      - mysql
    environment:
      SPRING_PROFILES_ACTIVE: prod

  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./frontend-admin/dist:/usr/share/nginx/html/admin
    ports:
      - "80:80"
      - "443:443"
```

### 7.4 部署步骤

1. 购买云服务器（推荐腾讯云 / 阿里云）
2. 安装 Docker + Docker Compose
3. 配置 `.env` 文件（数据库密码、微信 AppID/Secret 等）
4. 申请 SSL 证书
5. `docker compose up -d`
6. 配置域名 DNS 解析
7. 微信小程序提交审核发布

---

## 8. 安全设计

| 风险 | 防护措施 |
|------|----------|
| XSS | Vue 自动转义 + CSP Header |
| CSRF | SameSite Cookie + Token |
| SQL 注入 | MyBatis-Plus 参数化查询 |
| 敏感数据 | session_key 加密存储，JWT 短有效期 |
| 支付安全 | 微信支付签名验证 + 回调验签 (v1.5) |
| API 限流 | Spring Boot 拦截器（简单限流） |
| 越权访问 | 角色拦截器（user/admin 权限隔离） |
| 价格篡改 | 订单总价由后端计算，前端无法篡改 |
| 重复提交 | 订单创建接口幂等性 + 前端防抖 500ms |

---

## 9. 关键设计决策（ADRs）

### ADR-001: 微信一键获取手机号登录
- **决策**: `wx.getUserPhoneNumber` 获取手机号 + `wx.login` 获取 openid
- **理由**: 体验最顺滑，零输入，同时获取手机号和 openid
- **后果**: 依赖微信小程序，需要企业/个体户主体

### ADR-002: 订单状态机
- **决策**: 使用明确的枚举状态 + 状态转换方法
- **理由**: 防止非法状态跳转，业务逻辑清晰
- **状态流转**: `待确认 → 已确认/已拒绝`, `已确认 → 进行中 → 已完成`, `待确认 → 已取消`

### ADR-003: 小程序微信支付（v1.5 接入）
- **决策**: 小程序支付（`wx.requestPayment`）
- **理由**: 小程序内完成支付闭环
- **注意**: 需要微信商户号 + 小程序 AppID 绑定

### ADR-004: 车辆图片存储方案
- **MVP 决策**: 图片以 base64 存储在数据库 `vehicles.images` JSON 字段中
- **理由**: 简化 MVP 部署，无需额外 OSS 服务配置
- **v1.5 计划**: 迁移至阿里云 OSS / 腾讯云 COS，数据库改存 URL
- **影响**: MVP 阶段 API 响应体积较大，但个人租车场景车辆数量有限，可接受

### ADR-005: 同一小程序区分用户端和管理端
- **决策**: 通过用户 `role` 字段（user / admin），同一套代码通过路由和权限区分
- **理由**: 减少小程序维护成本，审核只需提交一个小程序
- **UI**: 管理员底部 Tab 多一个"管理"入口
- **限制**: uni-app tabBar 不支持运行时动态切换，MVP 阶段采用页面级权限控制（在页面 onShow 中检查角色，不匹配则跳转），TabBar 切换方案留到 v1.5 优化

### ADR-006: 三项目结构
- **决策**: backend + frontend-mini + frontend-admin 三个独立项目
- **理由**: 技术栈各自最优，PC 管理端用 Element Plus 开发效率最高

### ADR-007: MySQL 替代 PostgreSQL
- **决策**: 使用 MySQL 8.0
- **理由**: 团队 10 年 Java + MySQL 经验，MyBatis-Plus 对 MySQL 支持最佳
- **影响**: 项目规模不需要 PG 的高级特性（JSONB、部分索引等）

### ADR-008: 后端计算订单总价（防止篡改）
- **决策**: 订单创建时，后端根据 vehicleId + 租期计算 totalPrice，忽略前端传入的价格
- **理由**: 前端价格仅用于展示预览，后端重新计算确保安全
- **影响**: 前端价格预览可能因并发修改产生不一致，但 MVP 阶段车辆价格变更频率低，可接受

### ADR-009: MVP 简化定价（仅工作日价格）
- **决策**: MVP 阶段价格估算仅使用 `days × weekday_price`，不考虑周末/节假日
- **理由**: 简化 MVP 开发，快速验证核心流程
- **v1.5**: 引入完整 PricingEngine，支持周末/节假日差异化定价

### ADR-010: 取车地址硬编码
- **决策**: MVP 阶段取车地址硬编码在前端配置和后端常量中
- **理由**: 单取车点场景无需动态配置
- **v1.5**: 迁移至 `system_configs` 表，支持管理端配置

---

## 10. MVP vs 延期功能矩阵

| 功能 | 状态 | 说明 |
|------|------|------|
| 微信登录 | MVP | 完整实现 |
| 车辆列表 | MVP | 仅价格区间筛选，无 seats 筛选 |
| 车辆详情 | MVP | 多图轮播 (base64) |
| 价格估算 | MVP | `days × weekday_price`，v1.5 完整 PricingEngine |
| 订单创建 | MVP | 后端算价，防止篡改 |
| 订单列表/详情 | MVP | 两查询 + 应用层组合 |
| 订单状态可视化 | MVP | Steps 组件 |
| 个人中心 | MVP | 基本信息 + 退出 |
| 登录态管理 | MVP | token 检查 + 自动刷新 + 401 拦截 |
| 取车地址 | MVP | 硬编码 |
| 取车指引 | MVP | 硬编码文字 |
| 空状态引导 | MVP | EmptyState 组件 |
| 管理端筛选+快捷操作 | MVP | 订单状态筛选 + 确认/拒绝 |
| 管理端简化仪表盘 | MVP | 基础指标 |
| TabBar 动态切换 | **延期 v1.5** | uni-app 限制，待优化 |
| 微信支付 | **延期 v1.5** | 已预留数据模型 |
| OSS 图片存储 | **延期 v1.5** | MVP 用 base64 |
| 完整定价引擎 | **延期 v1.5** | MVP 简化版 |
| 可配置取车地址 | **延期 v1.5** | MVP 硬编码 |
| 优惠券/评价/保险 | **延期 v2.0** | 已预留字段 |
| 多门店/GPS/会员 | **延期 v2.0+** | 已预留字段 |

---

## 11. Maven 核心依赖

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL Driver -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>

    <!-- WxJava 小程序 SDK -->
    <dependency>
        <groupId>com.github.binarywang</groupId>
        <artifactId>weixin-java-miniapp</artifactId>
        <version>4.6.0</version>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-mysql</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```
