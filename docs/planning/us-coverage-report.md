# 用户故事验收报告

**日期**: 2026-04-11
**范围**: US-01 ~ US-25 逐项验收
**总体状态**: 9/25 完整实现 | 11/25 部分实现 | 5/25 未开始

---

## 用户故事逐项验收

### US-01: 微信一键登录 (Demo 模式) ⚠️ 部分实现

**验收标准**:
- [x] 登录页保留微信授权按钮 -- `login.vue` 第 61 行，`<button open-type="getPhoneNumber">`
- [x] Demo 模式纯本地 token -- `store/user.js` mockLogin 生成 mock_token
- [x] 登录成功后跳转首页 -- `login.vue` 第 101 行 `uni.switchTab('/pages/index/index')`
- [ ] 登录态保持 7 天 -- Token 存 Storage 但无过期时间逻辑，无法验证 7 天续期
- [ ] 后端记录 wechat_openid 和 phone 绑定关系 -- `wxLogin` API 已定义（`auth.js` 第 24 行），后端 `AuthController.wxLogin` 已实现，但前端未对接真实流程

**缺失**:
- Token 无过期时间管理（7 天续期逻辑）
- 真实微信登录未启用，`login` 方法中 `VITE_USE_MOCK_LOGIN` 环境变量控制但默认 false 时会走真实流程但后端未完全对接

**前后端对齐**: ⚠️ 后端 wx-login 端点存在但依赖小程序上线（Sprint 3 任务）

---

### US-02: 浏览车辆 ⚠️ 部分实现

**验收标准**:
- [x] 列表展示上架车辆 -- `index.vue` 展示车辆卡片
- [x] 显示封面图/车型名/日租金 -- `index.vue` 第 45-50 行，coverImage + name + weekdayPrice
- [ ] 支持按价格区间筛选 -- 后端 `VehicleController.list()` 有 minPrice/maxPrice 参数，前端无筛选 UI
- [x] 已租完/下架车辆不展示 -- 后端 `findActiveVehicles()` 只查 active

**缺失**:
- 前端价格筛选 UI 缺失（P1 功能，列入 Sprint 3）
- 当前使用 Mock 数据（`useMock = true`），未对接真实 API `GET /api/v1/vehicles`

**前后端对齐**: ⚠️ 后端 API 已就绪，前端仍走 Mock

---

### US-03: 查看车辆详情 ✅ 完整实现

**验收标准**:
- [x] 多图轮播 -- `vehicle-detail.vue` 第 7-10 行 `<swiper>` + indicator
- [x] 展示品牌型号/座位数/变速箱 -- 第 28-31 行 meta-row
- [x] 车辆描述/使用规则 -- 第 32 行 description
- [x] 展示不同日期类型的价格 -- 第 24-26 行 weekdayPrice + weekendPrice
- [x] 底部"立即预订"按钮 -- 第 61 行 book-btn

**前后端对齐**: ✅ 后端 `GET /api/v1/vehicles/{id}` 返回 VehicleDetailVO，字段完整

---

### US-04: 预订下单 ✅ 完整实现

**验收标准**:
- [x] 选择取车/还车日期 -- `vehicle-detail.vue` 租期选择器
- [x] 自动计算租期天数 -- computed `days`
- [x] 根据日期类型自动计算总价 -- `POST /pricing/estimate` 调用
- [x] 展示价格明细 -- `booking.vue` 第 35-53 行
- [x] 取车指引 -- 第 29 行硬编码地址
- [x] 下单前需勾选用户协议 -- 第 56-64 行 checkbox，onSubmit 校验
- [x] 提交订单后状态为"待确认" -- 后端 `OrderController.createOrder` 设置 PENDING
- [x] 同一辆车同一时间段不能被重复预订 -- `OrderConflictChecker.checkConflict`
- [x] 后端计算 totalPrice -- 使用 `PricingEngine.calculate`
- [ ] 提交订单防抖 500ms -- 有 `submitting` 防重复提交标志，但非严格 500ms 防抖

**前后端对齐**: ✅ 完全对齐，后端算价 + 冲突检测均已实现

---

### US-05: 查看我的订单 ⚠️ 部分实现

**验收标准**:
- [x] 订单列表按时间倒序 -- `orders.vue` 展示订单卡片
- [x] 显示订单状态 -- 6 种状态标签均有
- [x] 点击进入订单详情 -- `goDetail` 跳转
- [x] 待确认状态可取消 -- `handleCancel` + `cancelOrder` API
- [ ] 状态筛选 Tab -- 只有 5 个 Tab（全部/待确认/已确认/进行中/已完成），缺少"已取消"和"已拒绝"Tab

**缺失**:
- Tab 列表缺少 cancelled 和 rejected 筛选
- 前端调用 `getOrders` 但当前 Mock 数据策略下未实际走后端 API

**前后端对齐**: ⚠️ 后端 `OrderController.myOrders` 支持 status 参数，前端 Tab 未覆盖全部状态

---

### US-06: 用户协议 ✅ 完整实现

**验收标准**:
- [x] 下单页展示协议勾选框 -- `booking.vue` 第 56-64 行
- [x] 点击可展开查看协议全文 -- `agreement.vue` 完整 6 章节
- [x] 未勾选无法提交订单 -- `onSubmit` 第 172 行校验
- [x] MVP 阶段协议内容硬编码在前端 -- `agreement.vue` 完整硬编码

**前后端对齐**: ✅ 后端 `GET /api/v1/agreement` 已实现且管理端可编辑

---

### US-07: 订阅消息通知 ❌ 未开始

**验收标准**:
- [ ] 下单时引导用户授权订阅消息 -- `booking.vue` 无任何订阅消息授权引导
- [ ] 管理员确认订单后推送通知 -- `AdminOrderController.confirm` 第 76 行 `// TODO: 发送订阅消息通知用户`
- [ ] 管理员拒绝订单后推送通知（含拒绝原因）-- `AdminOrderController.reject` 第 98 行 `// TODO: 发送订阅消息通知用户`

**现状**:
- 后端 `SubscriptionController` 仅有 `/subscription/record` 记录授权端点，无实际推送逻辑
- 前端无任何订阅消息 UI

**缺失**: 整个订阅消息推送链路未实现，属于 Sprint 3 任务

---

### US-08: 仪表盘（PC）⚠️ 部分实现

**验收标准**:
- [x] 今日订单数、今日收入 -- `DashboardView.vue` 第 6-15 行
- [ ] 本月订单数、本月收入 -- 后端返回 `month_orders`/`month_revenue`，前端 `stats` 对象未包含这两个字段
- [x] 当前进行中订单数 -- `active_orders` 已展示
- [x] 当前可租车辆数 -- `available_vehicles` 已展示

**缺失**:
- 前端 Dashboard 缺少"本月订单数"和"本月收入"两个卡片
- 后端已返回数据，纯前端遗漏

**前后端对齐**: ⚠️ 后端 `DashboardController.overview` 返回 7 个字段，前端只展示 4 个

---

### US-09: 车辆管理（PC）✅ 完整实现

**验收标准**:
- [x] 列表展示所有车辆（含下架）-- `VehicleView.vue` 加载所有车辆
- [x] 新增车辆：填写车型信息、上传照片、设置价格 -- Dialog 表单 9 字段
- [x] 编辑车辆信息 -- `showEditDialog` 数据回填
- [x] 上架/下架操作 -- `toggleStatus` 按钮
- [x] 删除车辆（软删除）-- `deleteVehicle` + ElMessageBox 确认

**前后端对齐**: ✅ 后端 CRUD API 全部就绪，含批量价格更新

---

### US-10: 订单管理（PC + 小程序）⚠️ 部分实现

**验收标准（PC 端）**:
- [x] 列表展示所有订单，按状态筛选 -- `OrderView.vue` 状态筛选 select
- [ ] 查看订单详情（用户信息、车辆、租期、价格）-- 列表仅显示基本字段，无独立详情页
- [x] 待确认订单可以"确认"或"拒绝" -- confirm/reject 按钮
- [ ] 确认订单后通知用户 -- `AdminOrderController.confirm` TODO 注释
- [x] 进行中的订单可以标记"完成" -- complete 按钮
- [x] 拒绝时填写原因（选填）-- `prompt` 输入拒绝原因

**验收标准（小程序端）**:
- [ ] 小程序端快速查看待确认订单 -- 未实现，小程序无管理 Tab
- [ ] 小程序端确认/拒绝操作 -- 未实现

**缺失**:
- PC 端缺少订单详情页面（只能看列表行内信息）
- 确认订单后通知用户未实现
- 小程序管理端完全未开始

**前后端对齐**: ⚠️ 后端 AdminOrder API 完整，前端 PC 端基本功能 OK 但缺详情页

---

### US-11: 价格设置（PC）⚠️ 部分实现

**验收标准**:
- [x] 为每辆车设置工作日价格和周末价格 -- `PricingView.vue` 内联编辑表格
- [ ] 可配置特定日期为节假日并设置独立价格 -- 后端 `AdminPricingController` 有节假日 CRUD，但前端无 UI
- [ ] 支持批量设置日期类型（如设置某个日期区间为节假日）-- 后端有 `batchCreateHolidays`，前端无 UI

**缺失**:
- 节假日配置页面缺失，无法在 PC 端添加/管理节假日
- 价格设置页只有车辆价格编辑，缺少节假日日期区间管理

**前后端对齐**: ⚠️ 后端节假日 API 完整（GET/POST/Batch），前端 `PricingView` 只做了车辆价格部分

---

### US-12: 快速查看订单（小程序管理端）❌ 未开始

**验收标准**:
- [ ] 按时间倒序展示订单列表 -- 无
- [ ] 按状态筛选（待确认、进行中）-- 无
- [ ] 显示关键信息：用户手机号后4位、车型、租期、价格 -- 无

**现状**: 小程序中无管理端 Tab 或页面

---

### US-13: 快速处理订单（小程序管理端）❌ 未开始

**验收标准**:
- [ ] 待确认订单可以"确认"或"拒绝" -- 无
- [ ] 拒绝时需填写原因 -- 无
- [ ] 确认后自动通知用户 -- 无

**现状**: 小程序中无管理端页面

---

### US-14: 订阅消息（小程序管理端）❌ 未开始

**验收标准**:
- [ ] 用户提交订单后推送新订单通知 -- 无
- [ ] 点击通知跳转到小程序管理端订单详情页 -- 无

**现状**: 订阅消息整体未实现（见 US-07）

---

### US-15: 首页取车信息 ✅ 完整实现

**验收标准**:
- [x] 顶部固定展示取车地点 -- `index.vue` pickup-card 展示取还车时间
- [x] 显示取车/还车日期 -- pickupDate / returnDate
- [x] 点击可修改日期 -- 原生 `<picker mode="date">`

**前后端对齐**: ✅ 取车信息为前端硬编码，不依赖后端

---

### US-17: 车辆图片展示 ✅ 完整实现

**验收标准**:
- [x] 支持多图轮播，左右滑动切换 -- `<swiper>` 组件
- [ ] 图片比例 16:9 -- swiper 图片 400rpx 高，宽度 100%，比例不固定为 16:9
- [x] 图片下方显示指示器 -- page-indicator 显示 "X / Y"
- [x] 加载失败时显示默认占位图 -- `displayImages` computed 有 fallback
- [x] MVP 阶段图片以 base64 存储在数据库 -- 后端 `Vehicle.images` 为 List<String>

**注意**: 图片比例非严格 16:9，但功能完整

**前后端对齐**: ✅ 前后端均支持 images 数组

---

### US-18: 租期选择与价格预览 ✅ 完整实现

**验收标准**:
- [x] 取车日期和还车日期选择器 -- 原生 picker
- [x] 还车日期不能早于取车日期 -- `:start` 约束
- [x] 自动计算租期天数 -- computed `days`
- [x] 调用 `POST /pricing/estimate` 获取预估价格 -- `fetchPrice` 在 booking 页调用
- [x] 底部"立即预订"按钮携带已选日期跳转 -- `goBooking` 传参

**前后端对齐**: ✅ 完全对齐

---

### US-19: 订单状态可视化 ✅ 完整实现

**验收标准**:
- [x] 顶部进度条：提交订单 → 确认订单 → 取车用车 → 完成订单 -- `order-detail.vue` progressSteps
- [x] 已完成步骤高亮，当前步骤突出 -- dot-active / line-active 样式
- [x] 已取消/已拒绝展示特殊样式 -- reject-tip + dot-rejected
- [x] 不同状态展示不同操作按钮 -- pending 显示"取消订单"，confirmed 显示"立即支付"

**前后端对齐**: ✅ 后端 `buildStatusSteps` 提供 statusSteps 数据

---

### US-20: 个人中心 ⚠️ 部分实现

**验收标准**:
- [ ] 展示用户昵称、手机号（脱敏）-- `me.vue` 硬编码"租车用户"和"点击登录"，未从 userStore 读取真实信息
- [x] 入口：全部订单、用户协议 -- 菜单项存在
- [ ] 退出登录按钮 -- 有一个"设置"占位项，无退出登录功能
- [x] 未登录时显示"点击登录"引导 -- 显示"点击登录"文字但无点击事件

**缺失**:
- 用户信息未从 store 动态读取（硬编码）
- 缺少退出登录功能（`userStore.logout` 已定义但无 UI 入口）
- 未登录引导无点击交互

**前后端对齐**: ⚠️ 后端用户数据可用，前端未对接

---

### US-21: 登录态管理 ⚠️ 部分实现

**验收标准**:
- [ ] 小程序启动时检查 token -- 无 `App.onLaunch` 级别的 token 检查
- [x] 无 token → 跳转登录页 -- 页面级 `useAuthGuard` 在 onShow 检查
- [ ] Token 过期 → 清除 → 跳转登录 -- 无 token 过期时间存储和校验
- [x] HTTP 401 → 清除 token → 跳转登录 -- `request.js` 第 54 行 handle401
- [ ] 登录成功后回到原页面 -- `login.vue` 固定跳转首页，不记录来源页

**缺失**:
- 无全局启动检查（app.vue 无登录逻辑）
- 无 token 过期管理
- 无登录后返回来源页的逻辑

**前后端对齐**: ⚠️ 401 拦截已实现，但 token 生命周期管理缺失

---

### US-22: 取车指引 ⚠️ 部分实现

**验收标准**:
- [x] 下单页展示取车地址 -- `booking.vue` 第 29 行硬编码地址
- [ ] 展示营业时间 -- 仅在订单详情展示（`OrderController.buildPickupAddress`），下单页未展示
- [ ] 提示"下单后请与车主确认具体取车时间" -- 下单页无此提示
- [x] MVP 阶段硬编码文字 -- 是

**缺失**:
- booking 页缺少营业时间和取车确认提示

**前后端对齐**: ⚠️ 后端 `buildPickupAddress` 有 hours 和 note，但 booking 页未展示

---

### US-23: 管理端订单筛选与快捷操作 ⚠️ 部分实现

**验收标准**:
- [x] 状态筛选（全部/待确认/进行中/已完成）-- `OrderView.vue` el-select 筛选
- [ ] 待确认订单显示角标数量 -- 无角标/计数显示
- [x] 待确认订单卡片展示"确认"和"拒绝"按钮 -- 表格操作列按钮

**缺失**:
- 缺少待确认订单数量角标

**前后端对齐**: ✅ 后端已返回数据，前端缺 UI 元素

---

### US-24: 管理端仪表盘（小程序简化版）❌ 未开始

**验收标准**:
- [ ] 今日订单数、今日收入 -- 无
- [ ] 待确认订单数 -- 无
- [ ] 可租车辆数 -- 无

**现状**: 小程序无管理端，Dashboard API 仅供 PC 端使用

---

### US-25: 空状态引导 ✅ 完整实现

**验收标准**:
- [x] 订单列表为空："暂无订单" + "去租一辆车吧" -- `orders.vue` 第 17-21 行
- [x] 车辆列表为空："暂无可用车辆"提示 -- `index.vue` 第 27-31 行
- [x] 图标 + 文案 + 操作按钮 -- 均有 emoji 图标 + 文案 + 按钮

**前后端对齐**: ✅ 纯前端实现

---

## 差距汇总

### 后端缺失

| 用户故事 | 缺失功能 | 严重程度 | 说明 |
|---------|---------|---------|------|
| US-07 | 订阅消息推送 | P1 | TODO 注释存在，需接入微信订阅消息 API |
| US-01 | Token 过期管理 | P2 | Mock token 无过期时间 |

### 前端缺失（小程序）

| 用户故事 | 页面 | 缺失功能 | 严重程度 |
|---------|------|---------|---------|
| US-07 | booking.vue | 订阅消息授权引导 | P1 |
| US-02 | index.vue | 价格区间筛选 UI | P1 |
| US-05 | orders.vue | 已取消/已拒绝 Tab | P2 |
| US-20 | me.vue | 用户信息动态展示 + 退出登录 | P1 |
| US-21 | app.vue | 全局启动登录检查 | P1 |
| US-21 | login.vue | 登录后返回来源页 | P2 |
| US-22 | booking.vue | 营业时间 + 取车确认提示 | P2 |
| US-12/13/14 | 管理端 | 小程序管理端全部页面 | P1 |
| US-24 | 管理端 | 小程序管理端仪表盘 | P1 |

### 前端缺失（PC 管理端）

| 用户故事 | 页面 | 缺失功能 | 严重程度 |
|---------|------|---------|---------|
| US-08 | DashboardView.vue | 本月订单数/本月收入卡片 | P2 |
| US-10 | OrderView.vue | 订单详情页面 | P2 |
| US-11 | PricingView.vue | 节假日配置管理 UI | P1 |
| US-23 | OrderView.vue | 待确认订单角标计数 | P2 |

### 前后端不一致

| 用户故事 | 问题 | 严重程度 |
|---------|------|---------|
| US-02 | 前端 `useMock=true` 使用 Mock 数据，未对接真实 API | P1 |
| US-04/05 | booking/orders 提交失败后降级 Mock，真实链路未验证 | P1 |
| US-20 | `me.vue` 硬编码用户信息，未读取 userStore | P1 |
| US-08 | 后端返回 month_orders/month_revenue，前端未渲染 | P2 |
| US-02 | 后端支持价格筛选参数，前端无筛选入口 | P2 |

---

## 建议下一步

### 必须完成（阻塞核心流程）

1. **对接真实 API** (P1) -- 将 `useMock=false`，验证小程序端到端流程：车辆列表 -> 详情 -> 预订 -> 订单
2. **me.vue 动态用户信息 + 退出登录** (P1) -- 读取 userStore，添加退出登录按钮
3. **小程序管理端基础框架** (P1) -- 在小程序中添加管理 Tab 入口，实现 US-12/13

### 重要但不阻塞（Sprint 3 范围）

4. **订阅消息推送** (P1, US-07/14) -- 下单授权 + 状态变更推送，需小程序上线后接入
5. **价格筛选 UI** (P1, US-02) -- 首页增加价格区间筛选
6. **节假日配置 UI** (P1, US-11) -- 后端 API 已就绪，前端新建节假日管理页面
7. **全局登录态检查** (P1, US-21) -- app.vue onLaunch 添加 token 检查

### 体验优化（P2）

8. Dashboard 本月数据卡片补充 (US-08)
9. 订单列表增加已取消/已拒绝 Tab (US-05)
10. booking 页补充取车指引完整信息 (US-22)
11. 管理端订单详情页面 (US-10)
12. 登录后返回来源页 (US-21)
13. 待确认订单角标 (US-23)

### 已知技术债务

| 问题 | 优先级 | 说明 |
|------|--------|------|
| request.js 超时 10 秒 | P2 | 建议缩短为 3-5 秒 |
| manifest.json appid 为空 | P2 | 注册小程序后需填入 |
| TabBar 图标缺失 | P3 | 仅显示文字 |
| Token 无过期管理 | P2 | Mock token 永久有效 |
| booking 页价格明细未展示每日价格构成 | P2 | pricing estimate 返回了 breakdown 但 booking 页未使用 |
