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
| **短信服务** | 阿里云短信 / 腾讯云短信 | - | 备用认证方式（v1.0 暂不用） |
| **支付** | 微信小程序支付 | - | v1.5 接入，v1.0 预留 |
| **图片存储** | 腾讯云 COS / 阿里云 OSS | - | 便宜，CDN 加速 |
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
│  ┌──────────────┐  ┌──────────────┐                     │
│  │  用户端页面   │  │ 管理端简化页  │                     │
│  │  (默认角色)   │  │ (admin 角色) │                     │
│  └──────────────┘  └──────────────┘                     │
└────────────────────────┬────────────────────────────────┘
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
│ Auth 层  │ Vehicle  │ Order    │ Payment  │ Subscription│
│ JWT/微信  │ Service  │ Service  │ (预留)   │ 订阅消息    │
├──────────┴──────────┴──────────┴──────────┴─────────────┤
│                    MyBatis-Plus ORM                       │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                    MySQL 8.0                              │
└─────────────────────────────────────────────────────────┘

外部服务:
  ├── 微信 MiniApp (登录 + 订阅消息 + 支付 v1.5)
  ├── 阿里云/腾讯云短信 (备用验证码)
  └── 腾讯云 COS / 阿里云 OSS (图片存储)

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
infrastructure/     → 基础设施层：Repository 实现、微信 SDK、OSS、外部服务
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

按业务领域拆分为 4 个模块，每个模块内部包含四层：

```
backend/src/main/java/com/carrental/
│
├── common/                         # 共享内核（不依赖任何业务模块）
│   ├── config/
│   ├── exception/                  # 全局异常处理
│   ├── result/                     # 统一响应包装
│   ├── security/                   # JWT 拦截器
│   └── util/                       # 纯工具类
│
├── domain/                         # 领域层（按模块分包）
│   ├── user/
│   │   ├── User.java               # 聚合根
│   │   ├── UserRepository.java     # 接口
│   │   └── UserRole.java           # 值对象/枚举
│   ├── vehicle/
│   │   ├── Vehicle.java            # 聚合根
│   │   ├── VehicleStatus.java      # 值对象
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
│   └── pricing/
│       ├── PricingEngine.java      # 接口
│       ├── PricingResult.java      # 值对象
│       └── HolidayCalendar.java    # 值对象
│
├── application/                    # 应用层（按用例分包）
│   ├── auth/
│   │   ├── WxLoginCommand.java     # 命令对象
│   │   └── AuthService.java        # 应用服务
│   ├── order/
│   │   ├── CreateOrderCommand.java
│   │   ├── CreateOrderResult.java
│   │   ├── OrderAppService.java
│   │   └── dto/
│   │       ├── OrderListItemDTO.java
│   │       └── OrderDetailDTO.java
│   ├── vehicle/
│   │   ├── VehicleAppService.java
│   │   └── dto/
│   ├── pricing/
│   │   └── PricingAppService.java
│   └── dashboard/
│       └── DashboardAppService.java
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
│   └── storage/
│       └── OssStorageService.java  # 对象存储封装
│
└── controller/                     # 接口适配层
    ├── AuthController.java
    ├── VehicleController.java
    ├── OrderController.java
    ├── AdminOrderController.java
    ├── AdminVehicleController.java
    ├── PricingController.java
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
  └─ pricing 模块: 定价引擎 + 节假日配置

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
│   │       │   │
│   │       │   ├── common/              # 共享内核
│   │       │   │   ├── config/          # 全局配置
│   │       │   │   ├── exception/       # 全局异常
│   │       │   │   ├── result/          # 统一响应
│   │       │   │   ├── security/        # JWT
│   │       │   │   └── util/            # 工具类
│   │       │   │
│   │       │   ├── domain/              # 领域层（无框架依赖）
│   │       │   │   ├── user/
│   │       │   │   │   ├── User.java
│   │       │   │   │   ├── UserRepository.java
│   │       │   │   │   └── UserRole.java
│   │       │   │   ├── vehicle/
│   │       │   │   │   ├── Vehicle.java
│   │       │   │   │   ├── VehicleStatus.java
│   │       │   │   │   └── VehicleRepository.java
│   │       │   │   ├── order/
│   │       │   │   │   ├── Order.java
│   │       │   │   │   ├── OrderStatus.java
│   │       │   │   │   ├── PriceBreakdown.java
│   │       │   │   │   ├── OrderRepository.java
│   │       │   │   │   └── service/
│   │       │   │   │       └── OrderConflictChecker.java
│   │       │   │   └── pricing/
│   │       │   │       ├── PricingEngine.java
│   │       │   │       ├── PricingResult.java
│   │       │   │       └── HolidayCalendar.java
│   │       │   │
│   │       │   ├── application/         # 应用层（用例编排）
│   │       │   │   ├── auth/
│   │       │   │   │   ├── WxLoginCommand.java
│   │       │   │   │   └── AuthService.java
│   │       │   │   ├── order/
│   │       │   │   │   ├── CreateOrderCommand.java
│   │       │   │   │   ├── OrderAppService.java
│   │       │   │   │   └── dto/
│   │       │   │   ├── vehicle/
│   │       │   │   │   ├── VehicleAppService.java
│   │       │   │   │   └── dto/
│   │       │   │   ├── pricing/
│   │       │   │   │   └── PricingAppService.java
│   │       │   │   └── dashboard/
│   │       │   │       └── DashboardAppService.java
│   │       │   │
│   │       │   ├── infrastructure/      # 基础设施层
│   │       │   │   ├── persistence/
│   │       │   │   │   ├── mapper/
│   │       │   │   │   ├── dataobject/   # DO 对象
│   │       │   │   │   └── repository/   # Repository 实现
│   │       │   │   ├── wechat/           # 微信 SDK 封装
│   │       │   │   └── storage/          # OSS/COS 封装
│   │       │   │
│   │       │   └── controller/          # HTTP 适配层
│   │       │       ├── AuthController.java
│   │       │       ├── VehicleController.java
│   │       │       ├── OrderController.java
│   │       │       ├── AdminOrderController.java
│   │       │       ├── AdminVehicleController.java
│   │       │       ├── PricingController.java
│   │       │       ├── DashboardController.java
│   │       │       ├── AgreementController.java
│   │       │       └── SubscriptionController.java
│   │       │
│   │       └── resources/
│   │           ├── application.yml
│   │           ├── application-dev.yml
│   │           └── db/migration/
│   │               └── V1__init_tables.sql
│   │
│   └── Dockerfile
│   │       │   │   ├── WxConfig.java
│   │       │   │   ├── JwtConfig.java
│   │       │   │   ├── MyBatisPlusConfig.java
│   │       │   │   └── WebMvcConfig.java
│   │       │   │
│   │       │   ├── security/
│   │       │   │   ├── JwtInterceptor.java
│   │       │   │   └── RoleInterceptor.java
│   │       │   │
│   │       │   └── utils/
│   │       │       ├── DateUtils.java
│   │       │       └── PriceUtils.java
│   │       │
│   │       └── resources/
│   │           ├── application.yml
│   │           ├── application-dev.yml
│   │           └── db/migration/
│   │               └── V1__init_tables.sql
│   │
│   └── Dockerfile
│
├── frontend-mini/                  # uni-app 小程序
│   ├── src/
│   │   ├── pages/
│   │   │   ├── login/              # 微信登录页
│   │   │   ├── index/              # 首页(车辆列表)
│   │   │   ├── vehicle-detail/     # 车辆详情
│   │   │   ├── booking/            # 预订下单
│   │   │   ├── orders/             # 我的订单
│   │   │   ├── order-detail/       # 订单详情
│   │   │   ├── agreement/          # 用户协议
│   │   │   │
│   │   │   └── admin/              # 管理端简化版
│   │   │       ├── dashboard/      # 营收概览(简化)
│   │   │       ├── orders/         # 订单管理
│   │   │       └── order-detail/   # 订单详情
│   │   │
│   │   ├── components/
│   │   │   ├── common/             # 通用组件
│   │   │   └── vehicle/            # 车辆相关组件
│   │   │
│   │   ├── api/                    # API 请求封装
│   │   ├── store/                  # Pinia 状态
│   │   ├── utils/
│   │   ├── static/
│   │   ├── App.vue
│   │   ├── main.js
│   │   ├── manifest.json           # 小程序配置
│   │   └── pages.json              # 页面路由配置
│   │
│   ├── package.json
│   ├── vite.config.js
│   └── unocss.config.js            # 或使用 uView Plus
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
│   └── tailwind.config.js          # 可选
│
└── docs/
    ├── products/                   # 产品文档
    ├── arch/                       # 架构文档
    └── planning/                   # 进度日志
```

---

## 4. 微信集成方案

### 4.1 登录流程

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
// WxMaService 封装了所有微信 API 调用
WxMaJscode2SessionResult session = wxMaService.getUserService().getSessionInfo(loginCode);
String openid = session.getOpenid();

String phone = wxMaService.getUserService().getUserPhoneNumber(phoneCode);
```

### 4.2 订阅消息流程

```
1. 用户下单时调用 wx.requestSubscribeMessage，获取授权
2. 后端记录授权到 message_subscriptions 表
3. 管理员确认/拒绝订单时，后端调用微信订阅消息 API 推送通知
```

```java
// 发送订阅消息
WxMaSubscribeMessage message = WxMaSubscribeMessage.builder()
    .toUser(openid)
    .templateId(TEMPLATE_ID)
    .data(List.of(
        new WxMaSubscribeMessage.MsgData("thing1", "订单已确认"),
        new WxMaSubscribeMessage.MsgData("date2", "2026-05-01"),
        new WxMaSubscribeMessage.MsgData("amount3", "¥1796.00")
    ))
    .build();
wxMaService.getMsgService().sendSubscribeMsg(message);
```

---

## 5. 扩展性设计

### 5.1 领域层扩展

**当前 v1.0 聚合根**：
- `User` — 简单聚合根，只含基本信息
- `Vehicle` — 含价格和状态
- `Order` — 含订单状态机和价格明细

**v2.0 扩展方式**：
```
domain/order/
├── Order.java                    # v1.0 基础聚合根
├── service/
│   ├── OrderConflictChecker.java # v1.0
│   └── OrderPaymentService.java  # v1.5 新增（支付）
├── event/
│   ├── OrderCreatedEvent.java    # v1.5 新增
│   └── OrderPaidEvent.java       # v1.5 新增
└── repository/
    └── OrderRepository.java      # v1.0 接口，v1.5 扩展方法
```

新增领域对象只需在对应 domain 包下添加，不影响现有模块。

### 5.2 基础设施层扩展

**当前 v1.0 实现**：
- `infrastructure/wechat/WxLoginService` — 微信登录
- `infrastructure/wechat/WxSubscribeService` — 订阅消息
- `infrastructure/persistence/repository/` — MyBatis-Plus 实现

**v1.5/v2.0 扩展**：
- `infrastructure/wechat/WxPayService` — 微信支付（新增）
- `infrastructure/sms/SmsService` — 短信通知（新增）
- `infrastructure/cache/` — Redis 缓存（可选，性能瓶颈时引入）

扩展原则：`infrastructure/` 层添加新包，`domain/` 层定义新接口，保持依赖方向不变。

---

## 6. 部署方案

### 6.1 基础设施

| 资源 | 规格 | 用途 |
|------|------|------|
| 云服务器 | 2C4G 40GB | 运行后端 + Nginx |
| 域名 | 1 个 | API + PC 管理端 |
| SSL 证书 | Let's Encrypt (免费) | HTTPS |
| 对象存储 | 腾讯云 COS 标准存储包 | 车辆图片 |
| 微信小程序 | 企业/个体户主体 | 用户端 + 管理简化端 |

### 6.2 部署架构

```
用户 ──HTTPS──→ Nginx (80/443)
                    ├── /api/*        → Spring Boot (8080)
                    ├── /admin/*      → PC 管理端静态文件
                    └── 静态资源缓存

小程序端 ──HTTPS──→ 后端 API（直接请求 /api/*）
```

### 6.3 Docker Compose

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

### 6.4 部署步骤

1. 购买云服务器（推荐腾讯云 / 阿里云）
2. 安装 Docker + Docker Compose
3. 配置 `.env` 文件（数据库密码、微信 AppID/Secret 等）
4. 申请 SSL 证书
5. `docker compose up -d`
6. 配置域名 DNS 解析
7. 微信小程序提交审核发布

---

## 7. 安全设计

| 风险 | 防护措施 |
|------|----------|
| XSS | Vue 自动转义 + CSP Header |
| CSRF | SameSite Cookie + Token |
| SQL 注入 | MyBatis-Plus 参数化查询 |
| 敏感数据 | session_key 加密存储，JWT 短有效期 |
| 支付安全 | 微信支付签名验证 + 回调验签 (v1.5) |
| API 限流 | Spring Boot 拦截器（简单限流） |
| 图片防盗链 | COS Referer 白名单 |
| 越权访问 | 角色拦截器（user/admin 权限隔离） |

---

## 8. 关键设计决策

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

### ADR-004: 图片存储在对象存储
- **决策**: 车辆图片上传到 COS/OSS，数据库只存 URL
- **理由**: 节省服务器带宽和存储，CDN 加速加载

### ADR-005: 同一小程序区分用户端和管理端
- **决策**: 通过用户 `role` 字段（user / admin），同一套代码通过路由和权限区分
- **理由**: 减少小程序维护成本，审核只需提交一个小程序
- **UI**: 管理员底部 Tab 多一个"管理"入口

### ADR-006: 三项目结构
- **决策**: backend + frontend-mini + frontend-admin 三个独立项目
- **理由**: 技术栈各自最优，PC 管理端用 Element Plus 开发效率最高

### ADR-007: MySQL 替代 PostgreSQL
- **决策**: 使用 MySQL 8.0
- **理由**: 团队 10 年 Java + MySQL 经验，MyBatis-Plus 对 MySQL 支持最佳
- **影响**: 项目规模不需要 PG 的高级特性（JSONB、部分索引等）

---

## 9. Maven 核心依赖

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
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

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-mysql</artifactId>
    </dependency>
</dependencies>
```
