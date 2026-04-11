# Sprint 1 需求覆盖审查报告

> 审查日期: 2026-04-11
> 审查范围: US-01 ~ US-25 全部用户故事 vs 当前代码实现
> 审查依据: PRD (car-rental-brief.md) + Backlog (backlog.md) + API 契约 (api.md)

---

## 1. 用户故事覆盖矩阵

| 故事 | 优先级 | 状态 | 说明 |
|------|--------|------|------|
| US-01: 模拟登录 | P0 | 🟡 部分实现 | AuthController 有 wx-login，但**缺少 mock-login 端点**；前端 login.vue 依赖微信授权，无 mock 入口 |
| US-02: 浏览车辆 | P0 | 🟡 部分实现 | 后端 VehicleController.list() 已返回 coverImage，但**seed 数据 images 为空数组**，前端 index.vue 有基础渲染框架 |
| US-03: 查看车辆详情 | P0 | 🟡 部分实现 | VehicleController.detail() 已实现，但**直接返回 Domain 对象**而非 VO，缺少 tags 字段控制 |
| US-04: 预订下单 | P0 | ❌ 未实现 | 后端 OrderController.createOrder() **缺少后端算价、agreed 校验、vehicleName 返回**；前端无 booking 页面 |
| US-05: 查看我的订单 | P0 | 🟡 部分实现 | OrderController.myOrders() 有框架但**缺少 vehicleName/vehicleImage/statusLabel/canCancel**；前端无订单列表页 |
| US-06: 用户协议 | P0 | 🟡 部分实现 | 后端 AgreementController 已实现；前端 login.vue 有协议勾选但**无协议全文页** |
| US-15: 首页取车信息 | P0 | ❌ 未实现 | 前端 index.vue **缺少取车地址展示**；后端无取车地址接口 |
| US-17: 车辆图片展示 | P0 | 🟡 部分实现 | 后端 images 字段已定义且有 JacksonTypeHandler，但**seed 数据为空**；前端无图片轮播组件 |
| US-18: 租期选择与价格预览 | P0 | ❌ 未实现 | **PricingController 不存在**，SimplePricingEngine 无实现；前端无日期选择器 |
| US-19: 订单状态可视化 | P0 | ❌ 未实现 | 后端 OrderStatus 枚举已定义 6 种状态 + getLabel()，但**订单详情未返回 statusSteps**；前端无订单详情页 |
| US-20: 个人中心 | P0 | ❌ 未实现 | **profile 页面不存在**，pages.json 未注册 |
| US-21: 登录态管理 | P0 | ❌ 未实现 | App.vue **无 onLaunch 登录态检查**；request.js 无 401 处理；WebMvcConfig 排除路径待确认 |
| US-22: 取车指引 | P1 | ❌ 未实现 | 后端无取车地址字段/接口；前端无取车指引展示 |
| US-25: 空状态引导 | P1 | 🟡 部分实现 | index.vue 有 loading 状态但**无空状态组件**；订单列表页不存在 |

**不在 Sprint 1 范围**：

| 故事 | Sprint | 状态 |
|------|--------|------|
| US-07: 订阅消息通知 | Sprint 3 | 📋 未开始 |
| US-08: 仪表盘（PC） | Sprint 2 | 📋 未开始 |
| US-09: 车辆管理 | Sprint 2 | 📋 未开始 |
| US-10: 订单管理 | Sprint 2 | 📋 未开始 |
| US-11: 价格设置 | Sprint 3 | 📋 未开始 |
| US-12/13: 小程序管理端 | Sprint 2 | 📋 未开始 |
| US-14: 订阅消息 | Sprint 3 | 📋 未开始 |
| US-23: 管理端订单筛选 | Sprint 2 | 📋 未开始 |
| US-24: 管理端仪表盘 | Sprint 2 | 📋 未开始 |

---

## 2. API 契约一致性检查

### 2.1 缺失的 API 端点

| 缺失端点 | 对应需求 | 影响故事 |
|----------|---------|---------|
| `POST /api/v1/auth/mock-login` | Sprint 1 模拟登录 | US-01 |
| `POST /api/v1/auth/refresh` | Token 刷新 | US-21 |
| `POST /api/v1/pricing/estimate` | 价格预估 | US-04, US-18 |
| `GET /api/v1/admin/mini/orders` | 小程序管理端订单列表 | Sprint 2 |
| `GET/POST /api/v1/admin/pricing/holidays` | 节假日价格配置 | Sprint 3 |

### 2.2 已实现但与契约不一致的 API

| API | 契约要求 | 当前实现 | 偏差 |
|-----|---------|---------|------|
| `GET /api/v1/vehicles/{id}` | 返回 VehicleDetailVO（含 tags, holiday_price, images 数组） | 返回 `ApiResponse<Vehicle>` Domain 对象 | 暴露内部字段（deletedAt, createdAt），缺少 tags |
| `POST /api/v1/orders` | 请求含 agreed，响应含 vehicleName, priceBreakdown, status | 请求无 agreed 校验，响应无 vehicleName, priceBreakdown | **严重偏差** |
| `GET /api/v1/orders` | 返回含 vehicleName, vehicleImage, statusLabel, canCancel | 返回原始 Order 列表，缺少车辆信息 | 缺少后端算价、车辆组合 |
| `GET /api/v1/orders/{id}` | 返回含 VehicleInfo, statusSteps, canCancel, pickupAddress | 返回原始 Order 对象 | 缺少状态进度条数据 |
| `POST /api/v1/admin/orders/{id}/confirm` | 管理员确认后返回订单详情 | 框架存在但缺少车辆信息组合 | 待完善 |
| `GET /api/v1/admin/vehicles` | 返回所有车辆（含下架） | 调用 `findActiveVehicles()` 仅返回 active | **逻辑错误** |
| `PUT /api/v1/admin/vehicles/{id}` | 部分更新 | 实现为全量替换 | PUT vs PATCH 语义不一致 |
| `POST /api/v1/auth/admin-login` | 请求含 phone + password | 实现仅含 password（通过 openid 硬编码查找） | 契约不一致 |
| `GET /api/v1/dashboard/overview` | 返回 todayOrders, todayRevenue, monthOrders, monthRevenue, activeOrders, availableVehicles | 缺少 monthRevenue 计算 | 数据不完整 |

---

## 3. 数据模型一致性检查

### 3.1 已对齐的实体

| 实体 | PRD 定义 | 当前 DO/Domain | 状态 |
|------|---------|---------------|------|
| User | phone, wechat_openid, nickname, role, status | ✅ 字段完整 | ✅ 一致 |
| Vehicle | name, brand, seats, transmission, description, images, prices, status | ✅ 字段完整 + 额外 tags | ✅ 一致 |
| Order | user_id, vehicle_id, dates, totalPrice, status, paymentStatus | ✅ 核心字段完整 | ✅ 一致 |
| UserAgreement | content, version, is_active | ✅ 完整 | ✅ 一致 |
| MessageSubscription | user_id, template_id, status | ✅ 完整 | ✅ 一致 |

### 3.2 数据模型偏差

| 实体 | 偏差 | 影响 |
|------|------|------|
| OrderDO | `priceBreakdown` 类型为 `List<Object>` 而非 `List<PriceBreakdown>` | Jackson 反序列化丢失类型信息，导致读取后无法正确转换为 PriceBreakdown |
| OrderRepositoryImpl | `toDomain()` 和 `toDO()` 均**未处理 priceBreakdown 字段** | 订单保存时价格明细丢失，读取时为 null |
| OrderDO | 缺少 `rejectReason` 字段 | 管理员拒绝订单时无法存储原因（PRD US-10 要求拒绝时填写原因） |
| OrderDO | 缺少 `agreed` 字段 | 无法追溯用户是否同意协议 |
| VehicleDO.images | 已配置 JacksonTypeHandler ✅ | 无问题 |
| Vehicle | 多了 `tags` 字段（PRD 未要求） | 属于 PRD v2.0+ 预留字段，可接受 |

### 3.3 缺失的数据库字段

| 表 | 缺失字段 | 用途 | 影响故事 |
|----|---------|------|---------|
| orders | `reject_reason` | 拒绝原因 | US-10 |
| orders | `agreed_version` | 用户同意的协议版本 | US-06 |

---

## 4. Sprint 1 验收标准缺口分析

### US-01: 模拟登录

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 登录页保留微信授权按钮（UI 占位） | ✅ 已有 login.vue 按钮 | 无 |
| Demo 模式：点击直接获取模拟 token | ❌ | **缺少 mock-login 端点**，前端无 mock 登录入口 |
| 登录成功后跳转到首页 | 🟡 | 登录成功后有 switchTab 跳转，但 mock 登录未实现 |
| 真实微信登录接口预留 | ✅ | WxLoginCommand 已定义 |

### US-02: 浏览车辆

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 列表展示上架车辆 | ✅ | VehicleController.list() 已实现 |
| 显示封面图、车型名称、日租金 | ❌ | **seed 数据 images 为空**，coverImage 为 null |
| 点击进入车辆详情 | 🟡 | 前端有 goDetail 路由但详情页未实现 |
| 空状态引导 | ❌ | index.vue 无空状态组件 |

### US-03: 查看车辆详情

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 多图轮播（base64 图片数组） | ❌ | seed 数据为空；**无 VehicleDetailVO** |
| 车型参数（座位、变速箱、品牌） | ✅ | Domain 实体已含这些字段 |
| 车辆描述/使用规则 | ✅ | description 字段已存在 |
| 底部"立即预订"按钮 | ❌ | 前端详情页不存在 |

### US-04: 预订下单

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 选择取车/还车日期 | ❌ | 无 booking 页面 |
| 调用后端 pricing/estimate 获取价格 | ❌ | **PricingController 不存在** |
| 价格明细展示 | ❌ | SimplePricingEngine 未实现 |
| 取车指引（硬编码地址 + 营业时间） | ❌ | 无取车地址数据/接口 |
| 勾选用户协议 | ❌ | 无 booking 页面 |
| 提交订单（防抖 500ms） | ❌ | 前端未实现 |
| 订单创建成功，状态为"待确认" | 🟡 | Order 状态机有 PENDING 状态 |
| 后端计算 totalPrice（防止前端篡改） | ❌ | **OrderController.createOrder() 无后端算价** |

### US-05: 查看我的订单

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 订单列表按时间倒序 | 🟡 | 已有查询但排序待确认 |
| 状态筛选 Tab | ❌ | 前端订单列表页不存在 |
| 订单卡片显示车型名、租期、总价、状态标签 | ❌ | **Order 列表缺少 vehicleName/vehicleImage/statusLabel** |
| 待确认状态可取消 | 🟡 | Order.cancel() 状态机方法已定义 |
| 点击进入订单详情 | ❌ | 订单详情页不存在 |

### US-06: 用户协议

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 下单页展示协议勾选框 | 🟡 | login.vue 有勾选，但 booking 页不存在 |
| 点击跳转协议全文页 | ❌ | **agreement 页面不存在** |
| 未勾选无法提交订单 | ❌ | booking 页不存在 |
| MVP 阶段协议内容硬编码在前端 | ❌ | 无协议展示页面 |

### US-15: 首页取车信息

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 顶部固定展示取车地点 | ❌ | index.vue 无取车地址展示 |
| 显示取车/还车日期 | ❌ | 无日期展示 |
| 点击可修改日期 | ❌ | 无日期选择器 |

### US-17: 车辆图片展示

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 支持多图轮播 | ❌ | 无轮播组件 |
| 图片比例 16:9 | ❌ | 前端未实现 |
| 图片下方显示指示器 | ❌ | 前端未实现 |
| 加载失败时显示占位图 | ❌ | 前端未实现 |
| base64 存储在数据库 | ✅ | VehicleDO.images 已配置 JacksonTypeHandler |

### US-18: 租期选择与价格预览

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 取车/还车日期选择器 | ❌ | 无日期选择组件 |
| 还车日期不能早于取车日期 | ❌ | 无前端校验 |
| 自动计算天数 | ❌ | 无前端计算逻辑 |
| 调用 POST /pricing/estimate | ❌ | **PricingController 不存在** |
| 底部"立即预订"按钮 | ❌ | 详情页不存在 |

### US-19: 订单状态可视化

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 顶部进度条 | ❌ | 无前端组件 |
| 已完成步骤高亮 | ❌ | 无前端组件 |
| 已取消/已拒绝特殊样式 | ❌ | 无前端组件 |
| 不同状态展示不同操作按钮 | ❌ | 无前端组件 |

### US-20: 个人中心

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 展示用户昵称、手机号（脱敏） | ❌ | **profile 页面不存在** |
| 入口：全部订单、用户协议 | ❌ | 无入口组件 |
| 退出登录 | ❌ | 无退出按钮 |
| 未登录时显示"点击登录" | ❌ | 无引导逻辑 |

### US-21: 登录态管理

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 小程序启动时检查 token | ❌ | **App.vue 无 onLaunch 登录检查** |
| 无 token → 跳转登录页 | ❌ | 无跳转逻辑 |
| Token 过期 → 清除 → 跳转登录 | ❌ | request.js 无过期处理 |
| HTTP 401 → 清除 token → 跳转登录 | ❌ | **request.js 无 401 处理** |
| 登录成功后回到原页面 | ❌ | 无 redirect 逻辑 |

### US-22: 取车指引

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 下单页展示取车地址 | ❌ | 无 booking 页面 |
| 展示营业时间 | ❌ | 无数据 |
| 提示"下单后请与车主确认" | ❌ | 无提示文案 |
| MVP 阶段硬编码文字 | ❌ | 未实现 |

### US-25: 空状态引导

| 验收标准 | 满足 | 缺口 |
|---------|------|------|
| 订单列表为空时引导 | ❌ | 订单列表页不存在 |
| 车辆列表为空时引导 | ❌ | index.vue 无空状态 |
| 图标 + 文案 + 操作按钮 | ❌ | 无空状态组件 |

---

## 5. 风险与 Bug 报告

### 5.1 业务逻辑缺陷（P0 - 必须修复）

| # | 缺陷 | 影响 | 涉及文件 |
|---|------|------|---------|
| BUG-01 | **AdminVehicleController 使用 findActiveVehicles()** | 管理端无法查看下架车辆，违反 US-09 验收标准 | `AdminVehicleController.java` |
| BUG-02 | **OrderDO.priceBreakdown 类型擦除** | 订单保存和读取时价格明细丢失 | `OrderDO.java`, `OrderRepositoryImpl.java` |
| BUG-03 | **缺少 reject_reason 字段** | 管理员拒绝订单时无法存储原因 | `OrderDO.java`, V1 建表脚本 |
| BUG-04 | **OrderController.createOrder 无 agreed 校验** | 用户可绕过协议同意直接下单 | `OrderController.java` |
| BUG-05 | **OrderController.createOrder 无后端算价** | totalPrice 为 null，前端可篡改价格 | `OrderController.java` |

### 5.2 边界条件缺失（P1 - 应修复）

| # | 边界条件 | 场景 | 涉及 |
|---|---------|------|------|
| EDGE-01 | **日期校验：startDate < today** | 用户可选择过去的日期 | PricingController, OrderController |
| EDGE-02 | **日期校验：endDate <= startDate** | 还车日期不晚于取车日期 | OrderController |
| EDGE-03 | **vehicleId 不存在或已下架** | 预订不存在的车辆 | OrderController |
| EDGE-04 | **用户越权访问他人订单** | A 用户可查看 B 用户的订单 | OrderController（已有 userId 过滤但待确认） |
| EDGE-05 | **订单取消后重复取消** | 已取消的订单再次取消 | Order.cancel() 状态机（需确认是否已处理） |
| EDGE-06 | **并发预订同一车辆同一时段** | 两个用户同时提交相同时间段的同一辆车 | OrderConflictChecker 已实现，但需确认是否在事务中 |

### 5.3 安全隐患（P1 - 应修复）

| # | 隐患 | 风险 | 建议 |
|---|------|------|------|
| SEC-01 | **Admin 登录端点仅校验 password** | 任何人知道密码即可登录，无法区分不同管理员 | admin-login 应同时校验 phone + password |
| SEC-02 | **JWT 拦截器排除路径过多** | `/api/v1/auth/**` 全部排除，包括已登录才能访问的端点 | 确认排除路径仅包含 wx-login 和 mock-login |
| SEC-03 | **前端 request.js 无 token 刷新机制** | Token 过期后无法自动续期，用户体验差 | Sprint 3 增加 refresh token |
| SEC-04 | **base64 图片直接存数据库** | 大图片导致数据库膨胀，影响查询性能 | MVP 可接受，但需限制图片大小 |

### 5.4 前端页面缺口

| 缺失页面 | 对应故事 | 是否已注册 pages.json |
|---------|---------|---------------------|
| pages/vehicle-detail/vehicle-detail | US-03, US-17, US-18 | ✅ 已注册，但 .vue 文件不存在 |
| pages/booking/booking | US-04, US-22 | ✅ 已注册，但 .vue 文件不存在 |
| pages/order-list/order-list | US-05, US-25 | ✅ 已注册，但 .vue 文件不存在 |
| pages/order-detail/order-detail | US-05, US-19 | ✅ 已注册，但 .vue 文件不存在 |
| pages/agreement/agreement | US-06 | ✅ 已注册，但 .vue 文件不存在 |
| pages/profile/profile | US-20 | ❌ 未注册，.vue 文件也不存在 |
| pages/admin/orders | US-12, US-23 | ❌ 未注册 |
| pages/admin/order-detail | US-13 | ❌ 未注册 |

---

## 6. 总结

### 6.1 Sprint 1 完成度

| 维度 | 完成度 | 说明 |
|------|--------|------|
| 后端 API | ~40% | 骨架代码齐全，核心逻辑（算价、DTO 转换、车辆组合）缺失 |
| 前端页面 | ~15% | pages.json 已注册页面，但 .vue 文件大部分不存在 |
| 数据库 | ~95% | V1 建表脚本完整，V2 演示数据需补充 images |
| 安全体系 | ~80% | JWT 拦截器已实现，但排除路径和 admin 登录需完善 |

### 6.2 阻塞开发的前置问题

1. **P0-1 连带**：V2 seed 数据的 images 必须补充 base64，否则所有图片相关 API 返回 null
2. **OrderDO.priceBreakdown**：类型修复是 P0-4/P0-5/P0-6 的前提
3. **PricingController + SimplePricingEngine**：P0-4 是 P0-5 的前置依赖
4. **Mock Login**：P0-7 是前端联调的前提

### 6.3 建议优先级调整

按照依赖关系重新排序执行批次：

```
批次 1（基础，可并行）:
├── P0-1: 补充 seed 数据 images（纯 SQL）
├── P0-7 后端: mock-login 端点（前端联调前提）
└── P0-8: TabBar + 页面注册（前端路由基础）

批次 2（读接口，可并行）:
├── P0-2: 验证车辆列表 API（依赖 P0-1 数据）
├── P0-3: 车辆详情 VO（独立）
└── P0-4: Pricing Estimate API（独立新建）

批次 3（写接口，可并行）:
├── P0-5: 订单创建 API（依赖 P0-4 PricingEngine）
└── P0-6: 订单列表/详情 API（依赖 P0-4 priceBreakdown）

批次 4（前端页面，可并行）:
├── P0-7 前端: mockLogin + 登录态检查
├── P0-9: 首页
├── P0-10: 车辆详情页
├── P0-11: 预订下单页
├── P0-12: 订单列表页
├── P0-13: 订单详情页
├── P0-14: 我的页面
└── P0-15: 用户协议页
```

---

*审查人: QA Agent | 审查日期: 2026-04-11*
*下一步: 请 PM 根据此报告安排开发任务*
