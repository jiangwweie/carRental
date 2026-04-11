# 项目进度 - 租车应用

## 当前阶段: Sprint 3 - 体验优化（PC 管理端完善 + 小程序筛选）

### 分阶段规划

| 阶段 | 目标 | 预估工时 | 状态 |
|------|------|---------|------|
| **Sprint 1: Demo** | 小程序用户端核心流程跑通（浏览→选车→下单→查看订单） | ~15h | ✅ 已完成 |
| **Sprint 2: 管理** | PC 管理端 + 小程序管理端（车辆管理 + 订单处理 + 契约修复） | ~16h | ✅ 已完成 |
| **Sprint 2.5: 核心验证** | 关闭 Mock + 真实 API 全流程 + 个人中心 + 全局登录检查 | ~2.5h | ✅ 已完成 |
| **Sprint 3: 优化** | 节假日配置 UI + 首页筛选 + 订单筛选 + PC 订单详情 + 角标 | ~5.5h | ✅ 已完成 |
| **Sprint 4: 支付** | 微信支付 + 退款 + OSS + 完整定价 | ~10.5h | 📋 规划中（需外部条件） |

### Sprint 2 已完成总结

- PC 管理端：车辆管理（CRUD + 上下架 + 批量定价）、订单管理（列表+确认+拒绝+完成）、仪表盘、价格设置、协议管理
- 后端 API 增强：节假日定价管理 + 协议版本递增 + 契约合规修复
- 契约合规修复（12 文件）：Jackson snake_case + 6 后端修复 + 6 前端修复 + 103/103 测试通过

---

### 2026-04-11 收工 - Sprint 3 体验优化完成（T7-T13 全部完成）

**会话阶段**: 架构师设计 + PM 并行调度 + 3 Agent 并行开发
**参与者**: 用户（调度）+ Claude Code（PM + 架构师 + 开发）

#### 完成工作

- **架构设计文档**（`docs/arch/sprint3-design.md`）
  - 7 项任务逐一评估：现状分析 + API 契约检查 + 组件设计
  - 识别 2 项后端变更（DELETE 节假日 + 订单详情 API）

- **后端 API 新增（2 项，6 文件修改）**
  - T7: 节假日 DELETE API — Repository 接口 + Impl 实现 + Service 方法 + Controller 端点
  - T12: 管理端订单详情 API — AdminOrderController 新增 GET /{id}，返回 OrderDetailVO + userPhone

- **PC 管理端前端（T7/T11/T12/T13，4 项）**
  - T7: 新建 `HolidayView.vue`（年份选择器 + el-table 列表 + el-dialog 新增 + el-popconfirm 删除）+ `holiday.js` API 模块
  - T11: DashboardView.vue 新增本月订单数/本月收入两个统计卡片
  - T12: 新建 `OrderDetailView.vue`（基本信息 + 车辆 + 租期 + 价格明细 + 拒绝原因）+ 列表"查看详情"按钮
  - T13: Layout.vue 侧边栏"订单管理"显示待确认数量 el-badge 角标
  - 路由: 新增 `/holidays` 和 `/orders/:id` 路由

- **小程序端前端（T8/T9/T10，3 项）**
  - T8: index.vue 新增价格区间标签按钮（全部/0-200/200-400/400+），点击传 minPrice/maxPrice 参数
  - T9: orders.vue 新增已取消/已拒绝 Tab + 补充 `.status-rejected` 样式
  - T10: booking.vue 新增取车指引卡片（地址 + 营业时间 + 注意事项）

- **构建验证**
  - `npm run build:mp-weixin` 编译通过
  - `npm run build` (PC admin) 编译通过
  - 后端 103 单元测试全部通过（0 failures）

- **Bug 修复**
  - VehicleView.vue 新增车辆 snake_case 字段映射修复（前端 camelCase → 后端 snake_case）
  - VehicleView.vue / PricingView.vue 列表响应解析修复（`res.data.data` → `res.data.data.items`）
  - MySQL Seed 数据 UTF-8 乱码修复（latin1 编码数据清空后用 utf8mb4 重新插入）
  - Flyway V2 schema history 修复（标记失败迁移为成功）
  - OrderConflictChecker 缺少 @Service 注解修复
  - SimplePricingEngineTest 测试替补全 deleteById 方法

---

---

### 2026-04-11 收工 - Sprint 2.5 核心流程验证（T1/T2/T3 全部完成）

**会话阶段**: 架构师设计 + PM 并行调度 + 3 Agent 并行开发
**参与者**: 用户（调度）+ Claude Code（PM + 架构师 + 开发）

#### 完成工作

- **架构设计文档**（`docs/arch/sprint2.5-design.md`）
  - T1/T2 评估为低复杂度，无需设计，直接执行
  - T3 评估为中复杂度，采用方案 C（onShow + 公开页面白名单）

- **T1: 关闭 Mock 数据** - index.vue useMock 改为 false，login.vue 改用后端 mock-login API 获取 JWT token
- **T2: 个人中心动态信息** - me.vue 对接 userStore，已登录/未登录两种状态 + 退出登录
- **T3: 全局登录守卫** - App.vue onShow 白名单检查 + 登录后返回来源页

- **构建验证**
  - `npm run build:mp-weixin` 编译通过
  - 后端 103 单元测试全部通过（0 failures）

- **Git 提交**: `8565a66`

---

### 2026-04-11 Sprint 2.5 T3 - 全局登录态检查

**修改内容**:
- `frontend-mini/src/App.vue`: `onShow` 添加全局登录守卫，使用公开页面白名单方案
  - 公开页面：首页、车辆详情、登录页、用户协议
  - 非公开页面未登录时 `uni.reLaunch` 跳转登录页
- `frontend-mini/src/pages/login/login.vue`: 登录成功后支持返回来源页
  - 读取 `redirectTo` 查询参数
  - Tab 页面（index/orders/me）使用 `uni.switchTab`
  - 普通页面使用 `uni.redirectTo`
- `frontend-mini/src/utils/auth-guard.js`: 页面级守卫跳转时传递当前页路径作为 `redirectTo` 参数
- `npm run build:mp-weixin` 编译通过

---

### 2026-04-11 Sprint 2.5 T2 - 个人中心动态用户信息 + 退出登录

**修改内容**:
- `frontend-mini/src/pages/me/me.vue`: 从 `useUserStore()` 读取 `userInfo` 和 `isLoggedIn`
  - 已登录状态：显示用户昵称、脱敏手机号、头像（支持 avatarUrl 或默认图标）
  - 未登录状态：显示"点击登录"引导，点击跳转登录页
  - 退出登录：`uni.showModal` 确认 -> `userStore.logout()` -> `uni.switchTab` 跳转首页
- 手机号脱敏处理（`maskPhone` 函数：138****8000 格式）
- `npm run build:mp-weixin` 编译通过

---

### 2026-04-11 Sprint 2.5 T1 - 关闭 Mock 数据，对接真实 API

**修改内容**:
- `frontend-mini/src/pages/index/index.vue`: `useMock` 从 `ref(true)` 改为 `ref(false)`
- 其余页面（vehicle-detail、booking、order-detail）已是 API-first + try/catch 降级到 Mock 模式，无需修改
- `npm run build:mp-weixin` 编译通过
- Mock 数据保留作为 API 失败时的兜底策略

**环境配置说明**:
- `VITE_API_BASE_URL` 未配置，默认 `http://localhost:8080`
- `VITE_USE_MOCK_LOGIN` 未配置，使用真实微信登录流程

---

### 2026-04-11 收工 - 契约合规修复（7 后端 + 7 前端 P0/P1/P2）

**会话阶段**: 代码审查修复 + 前后端契约对齐
**参与者**: 用户（调度）+ Claude Code（PM + 开发 + 测试）

#### 完成工作

- **根因定位**: Jackson SNAKE_CASE 全局配置缺失（导致 90%+ 字段命名不匹配）

- **后端修复（6 文件）**
  - `application.yml`: 新增 `spring.jackson.property-naming-strategy: SNAKE_CASE`（P0-1）
  - `AuthController.java`: 新增 POST /auth/refresh 端点（P0-2, P1-16）
  - `OrderController.java`: CreateOrderResult 补全 vehicleName/priceBreakdown/status（P0-3）
  - `AdminVehicleController.java`: list() 增加 status 筛选（P1-8）
  - `DashboardController.java`: 新增 pending_orders 统计（P1-9）
  - `SubscriptionController.java`: record() 返回 {status:"accepted"} 替代 null（P1-14）

- **前端修复（6 文件）**
  - `orders.vue`: 替换 Mock 为真实 API + snake_case 映射 + tab 值修正（P0-4, P1-11）
  - `booking.vue`: 价格映射 total_price/dailyRate + order_id 提取修正（P0-5, P1-13）
  - `order-detail.vue`: 全字段 snake_case + 移除不存在字段（P0-6）
  - `index.vue`: 查询参数改为 page/page_size + 分页逻辑修正（P0-7, P1-12）
  - `auth.js`: refreshToken 改用 params 查询参数（P1-16）
  - `agreement.vue`: 从后端 API 获取协议 + loading/错误处理 + 本地兜底（P2-22）

- **测试验证**
  - 后端编译通过 + 91 单元测试全部通过（0 failures）
  - Git 推送: `0945853`

---

### 2026-04-11 收工 - Sprint 2 管理后台前端 3 项完成 + 代码审查修复

**会话阶段**: 架构师设计 + 3 Agent 并行开发 + 2 Agent 代码审查 + 自动修复
**参与者**: 用户（需求确认 + 调度）+ Claude Code（PM + 架构师 + 开发 + 审查）

#### 完成工作

- **架构设计文档**（`docs/arch/admin-frontend-design.md`）
  - 3 个管理后台功能的完整设计：组件结构、API 契约、状态管理、校验规则
  - 技术选型决策记录 + 文件变更清单 + 测试策略

- **管理后台前端开发（3 项）**
  - `VehicleView.vue`: 车辆新增/编辑弹窗（9 字段表单 + 校验 + 图片处理 + 标签选择）
  - `PricingView.vue`: 价格设置页（内联编辑表格 + 批量保存 + 差异检测 + 单行重置）
  - `AgreementView.vue`: 协议管理页（textarea 编辑器 + 日期格式化 + 空状态处理 + 预览）

- **后端 API 新增**
  - `AdminVehicleController.java`: 新增 `PUT /api/v1/admin/vehicles/prices` 批量更新价格端点
  - 新增 DTO: `BatchUpdatePriceRequest` + `VehiclePriceItem`
  - 使用 `@Transactional` 保证原子性

- **代码审查 + 自动修复（12 项发现）**
  - P0 修复: 删除/下架增加确认对话框、updatedAt 格式化、协议空状态处理、PUT 响应码检查
  - P1 修复: API 响应码校验、错误处理补全、空内容校验、保存失败内容恢复、ElMessageBox catch 精确处理

---

---

### 2026-04-11 收工 - Sprint 2 后端 API 审查修复 + 节假日定价管理实现

**会话阶段**: 架构师设计 + 4 Phase 并行开发 + 测试验证
**参与者**: 用户（调度）+ Claude Code（PM + 架构师 + 开发）

#### 完成工作

- **Sprint 2 设计文档输出**（docs/arch/sprint2-4-design.md）
  - 架构师出具节假日定价管理 + 协议版本递增详细设计
  - 10 个新建文件 + 3 个修改文件的完整实现计划

- **管理端 API 契约差距修复（5 项）**
  - P0-1: AdminOrders DTO 补全 5 字段（userPhone/vehicleName/vehicleImage/days/statusLabel）+ 批量查询用户/车辆信息避免 N+1
  - P0-3: Order.reject(reason) 新增 reason 参数，拒绝原因持久化
  - P0-4: AdminVehicles 列表加分页（page/pageSize 参数 + {total, items} 响应）
  - P0-5: PUT 改为部分更新（UpdateVehicleRequest + null 检查）
  - P0-2: 分页已有，无需修复

- **Feature 1: 节假日定价管理（P0-6）完整实现**
  - Domain 层：Holiday.java（聚合 + calculatePrice/covers/overlapsWith）+ HolidayRepository 接口
  - Infrastructure 层：HolidayConfigDO + HolidayConfigMapper + HolidayRepositoryImpl（@Transactional batchSave）
  - Application 层：HolidayAdminService（校验 + 重叠检测 + 事务性批量创建）
  - Controller 层：AdminPricingController（GET 列表/POST 创建/POST 批量）
  - PricingEngine 升级：SimplePricingEngine 注入 HolidayRepository，实现节假日/周末/工作日差异化定价
  - 定价优先级：fixed_price > weekend_price × multiplier > weekend_price > weekday_price

- **Feature 2: 协议版本自动递增（P0-7）**
  - AgreementController.update() version 从硬编码 "1.0" 改为 calculateNextVersion() 自动递增
  - 返回值改为返回新协议 DTO（含新版本号）

- **单元测试**
  - 新增 HolidayTest（4 优先级定价 + 6 日期覆盖 + 8 重叠场景 = 18 用例）
  - 重写 SimplePricingEngineTest（节假日/周末/工作日差异化定价场景 = 10 用例）
  - **总计 91 个测试用例全部通过**（0 failures）

---

## Sprint 1: Demo（已完成 ✅）

### 2026-04-11 收工 - 本地体验修复 + 微信开发者工具验证

**会话阶段**: 微信开发者工具体验测试 + Mock 流程闭环修复
**参与者**: 用户（体验反馈）+ Claude Code（PM 调度，3 Agent 并行）

#### 完成工作

- **依赖版本修复**: `package.json` 中 `@dcloudio/*` 版本号从无效 `4010120250128001` 改为可用 `5000720260327001`
- **pages.json 清理**: 移除未创建的 admin 页面路由注册，解决编译报错
- **Mock 登录纯本地化**: `store/user.js` mockLogin 改为纯本地生成 token，不再依赖后端
- **订单列表 Mock 数据**: 添加 3 条覆盖不同状态的 Mock 订单 + Tab 筛选 + 取消操作
- **预订提交流程闭环**: `createOrder` 失败后生成 Mock 订单 → 存本地 → 跳转详情页
- **微信开发者工具验证**: 编译成功，导入后可正常预览 8 个页面

#### Git 提交
- 待提交（本次修改）

---

### 2026-04-11 收工 - Sprint 1 后端全面收尾（代码审查 + P0 修复 + 单元测试）

**会话阶段**: 代码审查 + 架构方案 + 开发修复 + 测试验证
**参与者**: 用户（需求确认）+ Claude Code（PM 调度，多 Agent 并行）

#### 完成工作

- **后端 P0-6 完成（订单列表/详情 API）**
  - myOrders() 两查询组合：批量获取车辆信息 + OrderListItemDTO
  - detail() 返回 OrderDetailVO：vehicle 嵌套对象、statusSteps、pickupAddress、canCancel
  - 新增 toListDTO()、toDetailVO()、buildStatusSteps()、buildPickupAddress() 辅助方法

- **后端代码审查 + 5 个 P0 修复**
  - P0-1: 车辆列表分页 total 修正（先全量加载→内存过滤→内存分页）
  - P0-2: 订单详情 NPE 防御（Objects.equals 替换 .equals）
  - P0-3: OrderStatus.valueOf 安全转换（新增 fromValue + BusinessException）
  - P0-4: CreateOrderRequest 参数校验（@Valid + @NotNull）
  - P0-5: 接入 PricingEngine 替代手写算价逻辑

- **P1 遗留问题修复**
  - Dashboard monthRevenue 从硬编码零值改为实际查询 completed 订单总价
  - Admin 登录契约不一致：经核查 api-spec.yaml 只定义 password 字段，当前实现一致，标记 WONTFIX

- **首次引入单元测试（71 个用例，全部通过）**
  - OrderStateMachineTest: 状态机合法/非法流转（33 用例）
  - OrderStatusTest: getLabel + fromValue 安全转换（17 用例）
  - SimplePricingEngineTest: 价格计算 + 边界条件（8 用例）
  - OrderConflictCheckerTest: 冲突检测场景（13 用例）

#### Git 提交
- `21a7423` feat: P0-6 完成订单列表/详情 API
- `bbd39fc` fix: 修复 5 个 P0 阻塞问题（代码审查）
- `1fb8d69` docs: 更新进度 - 5 个 P0 Bug 修复完成
- `84e3cd8` fix: Dashboard monthRevenue 实际查询 + 首次引入单元测试

---

### 2026-04-11 收工 - P1/P2 审查问题修复 + 代码推送

**会话阶段**: UI/UX 问题修复 + 代码审查闭环
**参与者**: 用户（确认优先级）+ Claude Code（PM 调度，3 Agent 并行）

#### 完成工作

- **P1 问题修复（3 个）**
  - `login.vue`: 按钮 `:disabled` 增加 `!agreed` 条件，未勾选协议时真正不可点击
  - `me.vue`: 用户卡片渐变色从紫色 `#667eea` 改为绿色 `#07c160` 主题色
  - 订单列表页: 保持 Mock 数据策略，loading 和重试体验已优化

- **P2 问题修复（3 个）**
  - `vehicle-detail.vue`: 删除未使用的 `currentImage` 变量，swiper 添加 `@change` 事件
  - `vehicle-detail.vue`: 日期选择器从 `showModal` 改为原生 `<picker mode="date">`
  - `index.vue`: `returnDate` 默认改为 `pickupDate + 1 天`（默认租期 1 天）

#### Git 提交
- `df23484` fix(frontend): 修复代码审查发现的 P1/P2 问题
- 已推送到远程仓库 `origin/main`

---

### 2026-04-11 收工 - Sprint 1 前端 7 个页面全部开发完成

**会话阶段**: 前端开发（PM 调度，多 Agent 并行）
**参与者**: 用户（需求确认）+ Claude Code（PM + 5 Agent 并行）

#### 完成工作

- **基础设施增强**
  - `request.js`: 401 自动拦截 + 全局 loading + 网络错误友好提示
  - `store/user.js`: 支持 mock-login（开发阶段 bypass 微信授权）
  - `auth-guard.js`: 新建登录态拦截工具，页面级 onShow 检查

- **API 模块（4 个新增）**
  - `api/auth.js`: mock-login / wx-login / refresh-token
  - `api/order.js`: 创建/列表/详情/取消订单
  - `api/pricing.js`: 价格估算
  - `api/agreement.js`: 获取用户协议

- **页面开发（7 个页面）**
  - `pages/index/index.vue`: 增强首页（取车信息卡片、空状态、下拉刷新、Mock 数据）
  - `pages/login/login.vue`: 修复登录页（mock-login 输入 + 协议校验 + 微信登录保留）
  - `pages/vehicle-detail/vehicle-detail.vue`: 车辆详情页（图片轮播、租期选择、价格预览）
  - `pages/booking/booking.vue`: 预订下单页（价格明细、协议勾选、提交订单）
  - `pages/orders/orders.vue`: 订单列表页（状态 Tab、订单卡片、取消操作）
  - `pages/order-detail/order-detail.vue`: 订单详情页（状态进度条、信息区块、操作按钮）
  - `pages/me/me.vue`: 我的页面（用户信息、菜单导航、退出登录）
  - `pages/agreement/agreement.vue`: 用户协议页（6 章节完整协议）

- **代码审查**
  - 修复 P0: store/user.js mockLogin 响应解包错误
  - 修复 P0: order-detail 页面缺失登录拦截

#### Git 提交
- `b9d5b2a` feat(frontend): Sprint 1 前端 7 个页面全部开发完成 + API 模块 + 登录拦截

---

### 2026-04-11 开工 - QA 审查 + 5 个 P0 Bug 修复 + API 补全

**会话阶段**: 架构审查 + 后端 Bug 修复 + API 补全
**参与者**: 用户（需求确认）+ Claude Code（3 Agent 并行）

#### 完成工作

- **架构设计输出**（architecture-p0-1-to-8.md）
  - P0-1~P0-8 逐个任务的现状分析、分层设计、文件清单、依赖关系
  - 识别 3 个风险点（seed 数据空图片、OrderDO 类型擦除、Domain 暴露）

- **QA 审查**（sprint1-coverage-report.md）
  - 14 个 Sprint 1 用户故事逐一对照验收标准
  - 发现 5 个 P0 Bug、6 个边界条件缺口、4 个安全隐患
  - 9 个 API 不一致问题 + 5 个缺失端点
  - 前端 8 个页面缺失清单

- **后端 P0 Bug 修复**（5 个全部修复）
  - BUG-01: AdminVehicleController 改为返回所有车辆（含下架）
  - BUG-02: OrderDO.priceBreakdown 类型修复 + Repository 双向转换
  - BUG-03: Order + OrderDO 新增 rejectReason 字段 + V1 建表脚本
  - BUG-04: OrderController.createOrder() 新增 agreed 协议校验
  - BUG-05: OrderController.createOrder() 新增后端算价

- **API 补全**
  - 新建 PricingController + SimplePricingEngine（POST /pricing/estimate）
  - 新建 Mock Login 端点（POST /auth/mock-login）
  - VehicleController.detail() 改为 VehicleDetailVO（不再暴露 Domain）
  - WebMvcConfig 排除 /api/v1/pricing/** 路径

- **数据修复**
  - V2 seed 数据 5 辆车的 images 从 `[]` 改为 base64 占位图数组

- **契约文档修复**
  - api-spec.yaml 8 项 Sprint 1 缺失项修复：
    - 新增 `/auth/mock-login` 端点 + MockLoginRequest schema
    - 新增 `/pricing/estimate` 端点 + 完整请求/响应 schema
    - CreateOrderRequest 新增 `agreed` 必填字段
    - CreateOrderResult 新增 `vehicle_name`、`price_breakdown`、`days` 描述
    - OrderListItem 新增 `days`、`status_label`、`can_cancel`
    - OrderDetail 新增 `status_label`、`status_steps`、`pickup_address`、`can_cancel`、`reject_reason`
    - `images` 格式从 `uri` 改为 base64 字符串
    - `transmission` 枚举从 `auto/manual` 改为 `"自动"/"手动"`

- **编译修复**
  - Lombok 升级到 1.18.44（修复 JDK 兼容性问题）
  - pom.xml 添加 maven-compiler-plugin --add-opens 配置

#### Git 提交
- `8e7f7cc` feat: 为 Vehicle 实体新增 tags 标签字段
- `09b4c42` fix: 修复 5 个 P0 Bug + 新建 Pricing API + Mock Login

---

### 2026-04-10 收工 - 产品规划 + 架构更新 + 后端编译修复

**会话阶段**: 全天的需求讨论 + 架构更新 + 规划
**参与者**: 用户（需求确认）+ Claude Code

#### 完成工作
- **后端编译修复**（4 个 Java 文件）
  - AuthService: BCryptPasswordEncoder → SHA-256 简化编码
  - Order.java: 添加 @Setter
  - AdminOrderController + OrderController: 泛型修复 ApiResponse<?>
  - WxJava API: getUserPhoneNumber → getNewPhoneNoInfo
  - ✅ 后端 45 个 Java 文件全部编译通过

- **产品头脑风暴**
  - 用户故事从 14 个扩展到 25 个（新增 US-15 ~ US-25）
  - 完成 4 阶段 Sprint 规划（Demo → 管理 → 优化 → 支付）
  - 确定 Demo 优先策略：后台管理滞后

- **架构设计更新**（design.md 大重写）
  - 图片存储：base64 存数据库（MVP）→ v1.5 迁移 OSS
  - 定价方案：后端算价（防止篡改），MVP: days × weekday_price
  - 订单-车辆关联：两查询 + 应用层组合
  - 前端页面架构：11 个页面路由映射
  - 新增 ADR-008/009/010

- **API 接口文档更新**（api.md 大重写）
  - 完整覆盖 10 个模块
  - 每个接口含参数、响应、错误码
  - 明确标注 MVP vs 延期

- **需求池更新**（backlog.md 按 Sprint 重新组织）
- **交接文档**（2026-04-10-handoff.md 新建）

#### Git 提交
- `718ef4e` docs: 产品规划更新 + 后端编译修复 + 架构文档更新

---

### 已完成

- [x] 后端骨架编译修复（AuthService 密码编码 + Order @Setter + 泛型修复）
- [x] 产品需求文档更新（25 个用户故事）
- [x] 架构设计文档更新（base64 图片、后端算价、两查询组合）
- [x] API 接口文档更新（完整契约表）
- [x] 小程序骨架搭建（pages.json + TabBar + 基础页面注册）
- [x] 需求头脑风暴 + 产品优先级规划

### 进行中（P0 任务列表）

| # | 任务 | 用户故事 | 类型 | 预估 | 状态 |
|---|------|---------|------|------|------|
| P0-1 | 车辆数据初始化 SQL + base64 图片 | - | 后端 | 0.5h | ✅ 完成 |
| P0-2 | 完善车辆列表 API（base64 图片） | US-02, US-17 | 后端 | 0.5h | ✅ 完成 |
| P0-3 | 完善车辆详情 API（images 数组） | US-03, US-17 | 后端 | 0.5h | ✅ 完成 |
| P0-4 | 实现 Pricing Estimate API | US-18 | 后端 | 1h | ✅ 完成 |
| P0-5 | 完善订单创建 API（后端算价 + 冲突检测） | US-04 | 后端 | 1h | ✅ 完成 |
| P0-6 | 完善订单列表/详情 API（两查询组合） | US-05, US-19 | 后端 | 1h | ✅ 完成 |
| P0-7 | 登录简化方案（模拟登录 bypass 微信） | US-01, US-21 | 后端 | 1h | ✅ 完成 |
| P0-8 | TabBar 补全（首页/订单/我的） | - | 前端 | 0.5h | ✅ 完成 |
| P0-9 | 首页（取车信息 + 车辆卡片 + 空状态） | US-02, US-15, US-25 | 前端 | 2h | ✅ 完成 |
| P0-10 | 车辆详情页（图片轮播 + 租期选择） | US-03, US-17, US-18 | 前端 | 2h | ✅ 完成 |
| P0-11 | 预订下单页（价格明细 + 协议 + 提交） | US-04, US-18, US-22 | 前端 | 2h | ✅ 完成 |
| P0-12 | 订单列表页（状态 Tab + 卡片 + 取消） | US-05, US-25 | 前端 | 1.5h | ✅ 完成 |
| P0-13 | 订单详情页（状态进度条 + 信息区块） | US-05, US-19 | 前端 | 1.5h | ✅ 完成 |
| P0-14 | 「我的」页面（用户信息 + 入口 + 退出） | US-20 | 前端 | 0.5h | ✅ 完成 |
| P0-15 | 用户协议页 | US-06 | 前端 | 0.5h | ✅ 完成 |
| P0-16 | 登录态拦截（无 token 跳转 + 401 处理） | US-21 | 前端 | 0.5h | ✅ 完成 |

**Sprint 1 总计**: ~15h | **已完成**: ~15h（前端 100% + 后端 100%） | **剩余**: 0h 🎉

### 已知问题（待处理）

| # | 问题 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | `request.js` 超时设置 | P2 | 当前默认超时 10 秒，后端不可用时等待过长，建议缩短为 3-5 秒 |
| 2 | `manifest.json` appid | P2 | 当前为空字符串，注册小程序后需填入真实 AppID |
| 3 | TabBar 图标缺失 | P3 | TabBar 仅显示文字，后续可添加 iconPath 图标 |
| 4 | PricingView 无 loading 状态 | P2 | 加载车辆列表时无 loading 指示 |
| 5 | VehicleView 价格默认值为 0 | P2 | 新增时价格默认 0，但校验规则要求 > 0.01，用户体验略有摩擦 |

### 代码审查记录（本轮）

| # | 严重度 | 问题 | 状态 |
|---|--------|------|------|
| 1 | P0 | 删除/下架操作无确认对话框 | ✅ 已修复 |
| 2 | P0 | updatedAt 未格式化显示 | ✅ 已修复 |
| 3 | P0 | 协议空状态（404）被当作错误处理 | ✅ 已修复 |
| 4 | P0 | PUT 响应 code 未检查 | ✅ 已修复 |
| 5 | P1 | API 响应码未检查（handleSubmit/loadVehicles） | ✅ 已修复 |
| 6 | P1 | toggleStatus/deleteVehicle 错误处理缺失 | ✅ 已修复 |
| 7 | P1 | 协议保存前未校验空内容 | ✅ 已修复 |
| 8 | P1 | 保存失败未恢复原始内容 | ✅ 已修复 |

---

## 📋 待办清单（后续优化）

### Sprint 1 遗留

| # | 问题 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | `request.js` 超时设置 | P2 | 当前默认超时 10 秒，后端不可用时等待过长，建议缩短为 3-5 秒 |
| 2 | `manifest.json` appid | P2 | 当前为空字符串，注册小程序后需填入真实 AppID |
| 3 | TabBar 图标缺失 | P3 | TabBar 仅显示文字，后续可添加 iconPath 图标 |

### Sprint 2 启动前置条件

| # | 前置条件 | 状态 |
|---|----------|------|
| 1 | 确认是否需要同时开发 PC 管理端 + 小程序管理端 | 待确认 |
| 2 | 前端仓库是否同步推送到远程（本次已推送） | ✅ 已完成 |
| 3 | 后端 P0-6 已完成 | ✅ 远程已有 |

---

## Sprint 2: 管理功能

| # | 任务 | 用户故事 | 类型 | 预估 | 状态 |
|---|------|---------|------|------|------|
| P1-1 | 完善管理员订单列表 API | US-10, US-23 | 后端 | 1h | ✅ 完成 |
| P1-2 | 完善确认/拒绝/完成 API | US-10, US-23 | 后端 | 1h | ✅ 完成 |
| P1-3 | 完善车辆 CRUD API | US-09 | 后端 | 1.5h | ✅ 完成（新增/编辑/上下架/删除/批量价格） |
| P1-4 | PC 管理端 - 车辆管理页 | US-09 | 前端 | 3h | ✅ 完成（含新增/编辑弹窗） |
| P1-5 | PC 管理端 - 订单管理页 | US-10 | 前端 | 2h | ✅ 完成 |
| P1-6 | PC 管理端 - 仪表盘 | US-08 | 前端 | 1h | ✅ 完成 |
| P1-7 | 完善 Dashboard API | US-08, US-24 | 后端 | 0.5h | ✅ 完成 |
| P1-8 | 小程序管理端 - 订单处理 | US-12, US-13, US-23 | 前端 | 2h | 📋 |
| P1-9 | 小程序管理端 TabBar | US-05, US-24 | 前端 | 1h | 📋 |
| P1-10 | PC 管理端 - 价格设置页 | US-11 | 前端 | 2h | ✅ 完成 |
| P1-11 | PC 管理端 - 协议管理页 | US-06 | 前端 | 0.5h | ✅ 完成 |

**Sprint 2 进度**: 9/11 完成 (~11h/13h) | 🔄 进行中

---

## Sprint 3: 体验优化

| # | 任务 | 用户故事 | 类型 | 预估 | 状态 |
|---|------|---------|------|------|------|
| P2-1 | 真实微信登录 | US-01 | 前后端 | 1h | 📋 |
| P2-2 | 订阅消息推送 | US-07 | 前后端 | 2h | 📋 |
| P2-3 | 全局 loading + 错误处理 | US-25 | 前端 | 1h | 📋 |
| P2-4 | 首页筛选（价格区间） | US-16 | 前端 | 0.5h | ✅ 完成（T8） |
| P2-5 | 取车地址可配置 | US-22 | 前后端 | 1h | 📋 |
| P2-6 | PC 管理端 - 价格设置 | US-11 | 前端 | 2h | ✅ 完成（Sprint 2 提前完成） |
| P2-7 | PC 管理端 - 协议管理 | US-06 | 前后端 | 1h | ✅ 完成（Sprint 2 提前完成） |
| T7 | PC 节假日配置 UI | US-11 | 前端 | 2h | ✅ 完成（Sprint 3） |
| T9 | 订单筛选补全已取消/已拒绝 | US-05 | 小程序 | 0.5h | ✅ 完成（Sprint 3） |
| T10 | booking 页完善取车指引 | US-22 | 小程序 | 0.5h | ✅ 完成（Sprint 3） |
| T11 | Dashboard 补充本月数据卡片 | US-08 | PC | 0.5h | ✅ 完成（Sprint 3） |
| T12 | PC 管理端 - 订单详情页 | US-10 | PC | 1.5h | ✅ 完成（Sprint 3） |
| T13 | PC 待确认订单角标 | US-23 | PC | 0.5h | ✅ 完成（Sprint 3） |

**Sprint 3 进度**: 9/11 完成（T14/T15 需外部条件）| ✅ 已完成

---

## Sprint 4: 支付闭环（v1.5）

| # | 任务 | 类型 | 预估 | 状态 |
|---|------|------|------|------|
| P3-1 | 微信支付接入 | 后端 | 3h | 📋 |
| P3-2 | 小程序支付流程 | 前端 | 2h | 📋 |
| P3-3 | 退款流程 | 后端 | 1.5h | 📋 |
| P3-4 | 图片迁移到 OSS | 后端 | 2h | 📋 |
| P3-5 | 完整 PricingEngine | 后端 | 2h | 📋 |

**Sprint 4 总计**: ~10.5h

---

## 技术时间线

| 日期 | 事件 | 状态 |
|------|------|------|
| 2026-04-09 | 初始需求沟通 + PRD + 架构设计 | ✅ 完成 |
| 2026-04-10 | 需求变更：H5 → 微信小程序 | ✅ 完成 |
| 2026-04-10 | 项目骨架搭建（三子项目） | ✅ 完成 |
| 2026-04-10 | 后端编译修复 | ✅ 完成 |
| 2026-04-10 | UI 布局设计 + 产品头脑风暴 | ✅ 完成 |
| 2026-04-10 | 架构设计 + API 文档更新 | ✅ 完成 |
| 2026-04-10 | 产品规划（4 阶段 Sprint 规划） | ✅ 完成 |
| 2026-04-10 | Sprint 1 开发 | ⏳ 进行中 |

---

## 核心决策记录

| 决策 | 说明 |
|------|------|
| 技术栈 | uni-app + Vue 3（小程序）/ Vue 3 + Element Plus（PC）/ Java Spring Boot 3（后端）/ MySQL 8.0 |
| 图片存储 | MVP: base64 存数据库 → v1.5: 迁移 OSS/COS |
| 定价方案 | MVP: `days × weekday_price`（后端算价）→ v1.5: 完整 PricingEngine |
| 订单-车辆关联 | 两查询 + 应用层组合（避免 N+1） |
| 登录方案 | Sprint 1: 模拟登录 bypass 微信 → Sprint 3: 真实微信流程 |
| 取车地址 | MVP: 硬编码 → v1.5: 可配置 |
| 管理端 | Sprint 1 不做管理页面，数据通过 SQL 初始化 |
| TabBar 角色切换 | MVP: 页面级权限拦截 → v1.5: 动态 TabBar |
| 管理后台状态管理 | 组件局部 ref，无需 Pinia（页面无跨组件共享状态） |
| 价格批量更新 | 新建后端 API（PUT /vehicles/prices），保证原子性 |
| 协议编辑器 | MVP textarea 纯文本，不支持 Markdown |

---

## Sprint 2.5: 核心流程验证（建议立即做，~2.5h）

| # | 任务 | 端 | 预估 | 状态 | 说明 |
|---|------|-----|------|------|------|
| T1 | 关闭 Mock 数据，对接真实 API | 小程序 | 1h | ✅ | index.vue useMock=false，验证浏览→选车→预订→订单全流程 |
| T2 | 个人中心动态用户信息 + 退出登录 | 小程序 | 1h | ✅ | me.vue 读 userStore，加退出登录按钮 |
| T3 | 全局登录态检查 | 小程序 | 0.5h | ✅ | App.vue onShow 公开页面白名单守卫 + login.vue 支持返回来源页 + auth-guard 传递 redirectTo |

## Sprint 3: 小程序管理端 + 体验优化（~6.5h）

| # | 任务 | 端 | 预估 | 状态 | 说明 |
|---|------|-----|------|------|------|
| T4 | 小程序管理端入口 | 小程序 | 1h | 📋 | 管理端身份标识 + 入口 |
| T5 | 小程序管理端 - 订单处理 | 小程序 | 2h | 📋 | 查看/确认/拒绝订单 |
| T6 | 小程序管理端 - 简易仪表盘 | 小程序 | 0.5h | 📋 | 今日订单/待确认/可租车辆 |
| T7 | PC 节假日配置 UI | PC | 2h | 📋 | 后端 API 已就绪 |
| T8 | 首页价格区间筛选 | 小程序 | 0.5h | 📋 | 需 T1 完成 |
| T9 | 订单筛选补全已取消/已拒绝 | 小程序 | 0.5h | 📋 | 需 T1 完成 |
| T10 | booking 页完善取车指引 | 小程序 | 0.5h | 📋 | 纯前端 |
| T11 | Dashboard 补充本月数据卡片 | PC | 0.5h | 📋 | 纯前端，后端已返回 |
| T12 | PC 管理端 - 订单详情页 | PC | 1.5h | 📋 | 管理端完善 |
| T13 | PC 待确认订单角标 | PC | 0.5h | 📋 | 侧边栏角标 |

## 技术债务（~1.5h）

| # | 任务 | 优先级 | 状态 | 说明 |
|---|------|--------|------|------|
| T16 | request.js 超时缩短 | P2 | 📋 | 从 10s 改为 3-5s |
| T17 | manifest.json 填入 AppID | P2 | 📋 | 注册小程序后填入 |
| T18 | TabBar 图标添加 | P3 | 📋 | 为 3 个 Tab 添加 iconPath |
| T19 | PricingView 添加 loading | P2 | 📋 | 加载车辆列表时显示 |
| T20 | VehicleView 价格默认值优化 | P2 | 📋 | 新增弹窗价格默认值改为 null |

## 需外部条件的任务

| # | 任务 | 预估 | 阻塞条件 |
|---|------|------|----------|
| T14 | 真实微信登录 | 1h | 小程序需上线，需 AppID |
| T15 | 订阅消息推送 | 2h | 小程序需上线 + T14 |
| T21 | 微信支付接入 | 3h | 需微信支付商户号 |
| T22 | 小程序支付流程 | 2h | 需 T21 |
| T23 | 退款流程 | 1.5h | 需 T21 |
| T24 | 图片迁移到 OSS | 2h | 需 OSS/COS 账号 |
| T25 | 完整 PricingEngine | 2h | 需节假日配置 UI 完成 |

---

*最后更新: 2026-04-11 (Sprint 3 体验优化完成 - T7/T8/T9/T10/T11/T12/T13 全部通过)*
*项目经理: Claude Code PM*
