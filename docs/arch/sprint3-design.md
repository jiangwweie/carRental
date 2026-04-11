# Sprint 3 体验优化 - 设计文档

> 日期: 2026-04-11
> 范围: 7 项可执行任务（T7-T13，排除 T14/T15 微信登录+订阅消息）

---

## 总览

| 任务 | 模块 | 需要后端变更 | 复杂度 |
|------|------|-------------|--------|
| T7: 节假日配置 UI | PC 管理端 | 需新增 DELETE 端点 | 中 |
| T12: PC 订单详情页 | PC 管理端 | 需新增 GET /{id} 端点 | 低 |
| T11: Dashboard 本月数据 | PC 管理端 | 无（API 已有） | 低 |
| T13: 订单角标 | PC 管理端 | 无（API 已有） | 低 |
| T8: 首页价格筛选 | 小程序端 | 无（API 已有） | 低 |
| T9: 订单筛选补全 | 小程序端 | 无（API 已有） | 低 |
| T10: 取车指引完善 | 小程序端 | 无（纯前端） | 低 |

---

## T7: PC 节假日配置 UI

### 现状

- **后端 API**（`AdminPricingController.java`）已有完整接口：
  - `GET /api/v1/admin/pricing/holidays?year=2026` -- 列表
  - `POST /api/v1/admin/pricing/holidays` -- 创建单个
  - `POST /api/v1/admin/pricing/holidays/batch` -- 批量创建
  - **缺失**：`DELETE /api/v1/admin/pricing/holidays/{id}` 删除端点
- **后端服务层**（`HolidayAdminService`）无删除方法
- **后端仓储层**（`HolidayRepository` 接口）无 `deleteById` 方法
- **前端** `PricingView.vue` 是纯表格内联编辑（基础价格），无 tab 切换，无节假日相关 UI

### 方案：独立新建页面

理由：PricingView 已是完整的车辆价格编辑表格，加入 tab 会让页面过重。独立页面更清晰，也便于未来扩展。

### 组件结构

**新建文件：**
- `frontend-admin/src/views/holiday/HolidayView.vue` -- 节假日管理主页面
- `frontend-admin/src/api/holiday.js` -- 节假日 API 模块

**修改文件：**
- `frontend-admin/src/router/index.js` -- 新增 `/holidays` 路由
- `frontend-admin/src/views/Layout.vue` -- 侧边栏新增"节假日管理"菜单项

### 需要后端新增的 API

```
DELETE /api/v1/admin/pricing/holidays/{id}
Response: ApiResponse<?> { code: 0, message: "已删除" }
```

后端需同步扩展：
1. `HolidayRepository` 接口增加 `void deleteById(Long id)`
2. `HolidayAdminService` 增加 `deleteHoliday(Long id)` 方法
3. `AdminPricingController` 增加 `@DeleteMapping("/{id}")` 方法
4. `HolidayRepositoryImpl` 实现 deleteById（MyBatis-Plus 的 `HolidayConfigMapper` 应已有内置 deleteById，直接使用即可）

### 数据流

```
HolidayView.vue
  onMounted -> loadHolidays()
    GET /api/v1/admin/pricing/holidays?year=2026
    -> el-table 展示
  新增按钮 -> el-dialog 表单
    POST /api/v1/admin/pricing/holidays (单个)
    或 POST /api/v1/admin/pricing/holidays/batch (批量)
    -> reload
  删除按钮
    DELETE /api/v1/admin/pricing/holidays/{id}
    -> reload
  年份选择器 -> 切换 year 参数重新加载
```

### UI 结构

```
+---------------------------------------------------+
|  节假日管理                           [+ 新增]     |
|  [年份选择: 2026 ▼]                               |
+---------------------------------------------------+
|  名称      | 开始日期   | 结束日期   | 倍率 | 操作 |
|  春节      | 2026-02-17 | 2026-02-23 | 2.0x | 删除 |
|  国庆节    | 2026-10-01 | 2026-10-07 | 1.8x | 删除 |
+---------------------------------------------------+
```

- 使用 el-table 展示列表
- 使用 el-dialog + el-form 做新增弹窗
- 使用 el-date-picker 选择日期范围
- 删除用 el-popconfirm 二次确认
- 年份选择器用 el-select（当前年份起 3 年选项）

---

## T12: PC 订单详情页

### 现状

- **后端** `AdminOrderController` 仅有 `GET /api/v1/admin/orders` 列表端点，无详情端点
- **后端** `OrderController`（小程序端）已有 `GET /api/v1/orders/{id}` 返回 `OrderDetailVO`，包含 statusSteps、priceBreakdown、pickupAddress、rejectReason 等完整字段
- `OrderAdminDTO` 已有 userPhone、vehicleName、vehicleImage 等管理端字段
- **前端** `OrderView.vue` 仅有列表+操作按钮，无查看详情的入口

### 方案：新增管理端订单详情 API + 详情页

后端在 `AdminOrderController` 新增：

```java
@GetMapping("/{id}")
public ApiResponse<OrderAdminDetailVO> detail(@PathVariable Long id)
```

管理端直接复用 `OrderController.toDetailVO` 的返回结构，额外追加 userPhone 字段。

**前端新建：** `frontend-admin/src/views/orders/OrderDetailView.vue`

### 组件结构

**新建文件：**
- `frontend-admin/src/views/orders/OrderDetailView.vue`

**修改文件：**
- `frontend-admin/src/router/index.js` -- 新增 `/orders/:id` 路由
- `frontend-admin/src/views/orders/OrderView.vue` -- 列表添加"查看详情"按钮

### 数据流

```
OrderDetailView.vue (mounted)
  读取路由 params.id
  GET /api/v1/admin/orders/{id}
  渲染详情卡片（基本信息/车辆信息/租期信息/价格明细）
```

---

## T11: Dashboard 本月数据卡片

### 现状

- **后端** `DashboardController.overview()` 已返回全部所需字段：
  - `month_orders` -- 本月订单数
  - `month_revenue` -- 本月收入
  - `pending_orders` -- 待确认订单数
  - `today_orders`, `today_revenue`, `active_orders`, `available_vehicles`
- **前端** `DashboardView.vue` 当前只显示 4 个卡片，未使用 month_orders / month_revenue / pending_orders

### 方案：在现有 el-row 下方新增一行

在现有 4 个卡片下方新增一行，展示本月订单数和本月收入两个卡片。

### 修改文件

仅修改 `/Users/jiangwei/Documents/carRental/frontend-admin/src/views/dashboard/DashboardView.vue` -- 新增两个 el-col 卡片 + stats 中增加 `month_orders` / `month_revenue` 初始值。

### 无需后端变更

---

## T13: PC 待确认订单角标

### 现状

- **后端** `DashboardController` 已返回 `pending_orders` 数量
- **前端** `Layout.vue` 侧边栏使用 el-menu，"订单管理"项无角标
- Dashboard API 仅在 `/dashboard` 页面加载时调用

### 方案：Layout 组件 onMounted 时调用 Dashboard API

在 `Layout.vue` 的 `onMounted` 中调用 Dashboard overview API，取 `pending_orders` 显示为 el-badge。

### 修改文件

仅修改 `/Users/jiangwei/Documents/carRental/frontend-admin/src/views/Layout.vue`：
- 在 `onMounted` 中调用 `GET /api/v1/admin/dashboard/overview`
- 获取 `pending_orders` 存入 ref
- 订单管理菜单项使用 `el-badge` 包裹

```vue
<el-badge :value="pendingCount" :hidden="pendingCount === 0" :max="99">
  <el-menu-item index="/orders">
    ...
  </el-menu-item>
</el-badge>
```

### 无需新增后端 API

---

## T8: 首页价格区间筛选

### 现状

- **后端** `VehicleController.list()` 已支持 `minPrice` 和 `maxPrice` 查询参数（按 weekdayPrice 过滤）
- **前端** `index.vue` 首页有取车时间选择器和车辆列表，无价格筛选控件
- `getVehicleList` API 函数已支持 params 透传

### 方案：价格区间标签按钮

在取车卡片下方增加预设价格区间标签（如 0-200, 200-400, 400+），点击后触发列表重新加载。比 slider 更直观。

### 修改文件

仅修改 `/Users/jiangwei/Documents/carRental/frontend-mini/src/pages/index/index.vue`：
- 新增 `priceRange` ref（预设选项）
- 新增价格区间标签行（横向 scroll-view 或 flex 布局）
- `loadVehicles` 调用时传入 minPrice/maxPrice 参数

### 无需后端变更

---

## T9: 订单筛选补全已取消/已拒绝

### 现状

- **后端** `OrderController.myOrders()` 已支持 status 参数，OrderStatus 枚举包含 CANCELLED 和 REJECTED
- **前端** `orders.vue` 的 `tabList` 当前只有：全部、待确认、已确认、进行中、已完成
- 前端已有 `statusLabelMap` 包含 cancelled/rejected 的中文映射
- 前端已有 CSS `.status-cancelled` 和 `.status-rejected` 样式

### 方案：tabList 追加两项

```js
const tabList = [
  { label: '全部', value: 'all' },
  { label: '待确认', value: 'pending' },
  { label: '已确认', value: 'confirmed' },
  { label: '进行中', value: 'in_progress' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' },   // 新增
  { label: '已拒绝', value: 'rejected' }     // 新增
]
```

当前切换 tab 是前端 computed 过滤，cancelled/rejected 数据已经在 orders 数组中（后端 myOrders 不加 status 参数时返回全部状态），所以直接加 tab 项即可生效。

### 修改文件

仅修改 `/Users/jiangwei/Documents/carRental/frontend-mini/src/pages/orders/orders.vue` -- tabList 增加两项。

### 无需后端变更

---

## T10: booking 页完善取车指引

### 现状

- **前端** `booking.vue` 取车地址是硬编码 `"XX市XX区XX路XX号"`，一行文本
- **后端** `OrderController.buildPickupAddress()` 返回了完整结构：address、hours、note
- booking 页在提交订单前，此时无法获取订单的 pickupAddress

### 方案：前端常量定义取车信息

在 booking.vue 中定义 pickupInfo 常量（与后端 `buildPickupAddress()` 保持一致），新增"取车指引"卡片展示：
- 取车地址
- 营业时间（`hours`）
- 注意事项（`note`，如"下单后请与车主确认取车时间"）

### 修改文件

仅修改 `/Users/jiangwei/Documents/carRental/frontend-mini/src/pages/booking/booking.vue`：
- 定义 `pickupInfo` 常量
- 在租期信息卡片后新增取车指引卡片

### 无需后端变更

---

## 变更文件清单

### 后端（4 个文件修改）

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `backend/.../controller/AdminPricingController.java` | 新增 DELETE /holidays/{id} |
| 修改 | `backend/.../pricing/HolidayAdminService.java` | 新增 deleteHoliday 方法 |
| 修改 | `backend/.../domain/holiday/HolidayRepository.java` | 新增 deleteById 方法 |
| 修改 | `backend/.../repository/HolidayRepositoryImpl.java` | 实现 deleteById |
| 修改 | `backend/.../controller/AdminOrderController.java` | 新增 GET /orders/{id} 详情端点 |

### PC 管理端前端（7 个文件）

| 操作 | 文件 | 说明 |
|------|------|------|
| 新建 | `frontend-admin/src/views/holiday/HolidayView.vue` | 节假日管理页面 |
| 新建 | `frontend-admin/src/api/holiday.js` | 节假日 API 模块 |
| 新建 | `frontend-admin/src/views/orders/OrderDetailView.vue` | 订单详情页面 |
| 修改 | `frontend-admin/src/router/index.js` | 新增 /holidays 和 /orders/:id 路由 |
| 修改 | `frontend-admin/src/views/Layout.vue` | 新增节假日菜单 + 订单角标 |
| 修改 | `frontend-admin/src/views/dashboard/DashboardView.vue` | 本月数据卡片 |
| 修改 | `frontend-admin/src/views/orders/OrderView.vue` | 列表加"查看详情"按钮 |

### 小程序端（3 个文件）

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `frontend-mini/src/pages/index/index.vue` | 价格区间筛选 |
| 修改 | `frontend-mini/src/pages/orders/orders.vue` | cancelled/rejected Tab |
| 修改 | `frontend-mini/src/pages/booking/booking.vue` | 取车指引卡片 |

---

## 执行顺序建议

**第一批（后端先行，阻塞 T7、T12）：**
- 新增节假日 DELETE API（Repository + Service + Controller）
- 新增管理端订单详情 API（AdminOrderController）

**第二批（并行开发，后端完成后）：**
- PC 端：T7（节假日 UI）+ T12（订单详情）+ T11（Dashboard）+ T13（角标）
- 小程序端：T8（价格筛选）+ T9（订单筛选）+ T10（取车指引）

小程序端 3 项 + PC 端 T11/T13 完全不依赖后端变更，可立即开始。
