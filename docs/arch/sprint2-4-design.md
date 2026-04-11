# Sprint 2/3/4 架构设计文档

> 状态：Draft | 日期：2026-04-11 | 作者：Architect
>
> 本文档涵盖 Sprint 2 (P1 管理功能)、Sprint 3 (P2 体验优化)、Sprint 4 (P3 支付集成) 的完整架构设计与 API 契约。
>
> Sprint 1 已完成 16 个 P0 任务。本文档基于现有代码库现状编写。

---

## 目录

1. [Sprint 1 现状总结](#1-sprint-1-现状总结)
2. [Sprint 2 - P1 管理功能](#2-sprint-2---p1-管理功能)
3. [Sprint 3 - P2 体验优化](#3-sprint-3---p2-体验优化)
4. [Sprint 4 - P3 支付集成](#4-sprint-4---p3-支付集成)
5. [数据库迁移脚本](#5-数据库迁移脚本)
6. [风险评估](#6-风险评估)
7. [并行执行计划](#7-并行执行计划)

---

## 1. Sprint 1 现状总结

### 1.1 后端已实现

| 模块 | 状态 | 文件 |
|------|------|------|
| 认证 | 完成（含 mock 登录 + 真实 wx-login） | `AuthController.java`, `AuthService.java` |
| 车辆 CRUD（用户端） | 完成 | `VehicleController.java` |
| 车辆 CRUD（管理端） | 骨架存在，需完善分页/校验 | `AdminVehicleController.java` |
| 订单创建/列表（用户端） | 完成 | `OrderController.java` |
| 订单管理（管理端） | 骨架存在，DTO 缺字段 | `AdminOrderController.java` |
| 仪表盘 | 基本实现，缺 pending_orders | `DashboardController.java` |
| 定价估算 | 简化版（仅 weekday） | `PricingController.java`, `SimplePricingEngine.java` |
| 协议管理 | 完成，version 硬编码 bug | `AgreementController.java` |
| 订阅记录 | 完成 | `SubscriptionController.java` |

### 1.2 前端已实现

| 模块 | 小程序 | PC 管理端 |
|------|--------|-----------|
| 登录页 | 完成 | 完成 |
| 首页（车辆列表） | 完成 | - |
| 车辆详情 | 完成 | - |
| 预订页 | 完成 | - |
| 订单列表/详情 | 完成 | 骨架 |
| 个人中心 | 完成 | - |
| 仪表盘 | - | 骨架 |
| 车辆管理 | - | 骨架 |
| 订单管理 | - | 骨架 |
| 价格设置 | - | 骨架 |
| 协议管理 | - | 骨架 |
| 管理端 admin 页面 | 不存在 | - |

### 1.3 领域层已完成

- `Order.java`：含状态机（confirm/reject/cancel/start/complete）
- `OrderStatus.java`：枚举 + fromValue 安全查找
- `Vehicle.java`：聚合根
- `User.java`：聚合根
- `PricingEngine.java`：接口
- `PricingResult.java`：值对象
- `PriceBreakdown.java`：值对象

### 1.4 已知差距（Sprint 2 需要修复）

1. `AdminOrderController.toAdminDTO` 缺少 `user_phone`, `vehicle_name`, `vehicle_image`, `status_label`, `days` 字段
2. `AdminOrderController.list` 缺少 `start_date`, `end_date` 查询参数
3. `AdminVehicleController.list` 无分页，返回原始 `Vehicle` 而非 DTO
4. `AdminVehicleController.update` 是全量更新而非 PATCH
5. `AdminVehicleController` 缺少参数校验
6. `DashboardController` 缺少 `pending_orders` 字段
7. `AgreementController.update` version 硬编码为 "1.0"
8. 缺少节假日管理 API 和前端页面
9. 缺少 WeChat 订阅消息推送实现
10. 小程序端无 admin 页面

---

## 2. Sprint 2 - P1 管理功能（~13h）

### 2.1 P1-1: 管理端订单列表 API 增强

**现状**：`AdminOrderController.list` 已实现基础分页+筛选，但 DTO 缺少关联字段。

**需要修改的文件**：
- `controller/AdminOrderController.java` — 增强 DTO + 添加 start_date/end_date 参数 + 关联查询

**数据流**：
```
GET /api/v1/admin/orders?status=pending&vehicle_id=1&start_date=2026-05-01&end_date=2026-05-31&page=1&page_size=20
  → AdminOrderController.list(status, vehicleId, startDate, endDate, page, pageSize)
  → OrderRepository.findAdminOrders(...) — 已有的方法，需添加日期范围筛选
  → 对每个 Order: 查 User 获取 phone, 查 Vehicle 获取 name+image
  → 组装 OrderAdminDTO 返回
```

**DTO 增强**（替换现有 `OrderAdminDTO`）：
```java
@Data
public static class OrderAdminDTO {
    private Long id;
    private String userPhone;          // 脱敏: 138****8000
    private String vehicleName;        // 关联查询
    private String vehicleImage;       // 关联查询, images[0]
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;
    private BigDecimal totalPrice;
    private String status;
    private String statusLabel;        // 中文标签
    private LocalDateTime createdAt;
}
```

**OrderRepository 新增方法**：
```java
// 接口新增
List<Order> findAdminOrders(String status, Long vehicleId,
    LocalDate startDate, LocalDate endDate, int page, int pageSize);
long countAdminOrders(String status, Long vehicleId,
    LocalDate startDate, LocalDate endDate);
```

**实现策略**：
- 在 `OrderRepositoryImpl` 中扩展 `findAdminOrders` 和 `countAdminOrders`，添加日期范围条件
- 在 `AdminOrderController` 中注入 `UserRepository` 和 `VehicleRepository`
- 查询后应用层组合：先查订单列表，批量查用户和车辆，在内存中 join

### 2.2 P1-2: 订单状态变更 API（已完成骨架）

**现状**：`AdminOrderController` 已有 confirm/reject/start/complete 四个端点，状态机在 `Order.java` 中已实现。

**需要修改的文件**：
- `controller/AdminOrderController.java` — 基本可用，需补充订阅消息推送（Sprint 3）

**状态机验证**（已在 domain 层实现）：
```
Order.confirm()  → 检查 status == PENDING, 否则抛 IllegalStateException
Order.reject()   → 检查 status == PENDING, 否则抛 IllegalStateException
Order.start()    → 检查 status == CONFIRMED, 否则抛 IllegalStateException
Order.complete() → 检查 status == IN_PROGRESS, 否则抛 IllegalStateException
```

Controller 已正确捕获 `IllegalStateException` 并返回 5300 错误码。

**无需修改**：状态机逻辑已完备。

### 2.3 P1-3: 车辆管理 API 完善

**现状**：`AdminVehicleController` 有基本 CRUD，但缺少分页、校验、PATCH 语义。

**需要修改的文件**：
- `controller/AdminVehicleController.java`

**具体变更**：

1. **列表分页**：添加 page/page_size 参数，返回分页结果
```java
@GetMapping
public ApiResponse<Map<String, Object>> list(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize) {
```

2. **创建校验**：添加 `@Valid` + 校验注解或手动校验
   - `images` 非空
   - `weekdayPrice > 0`, `weekendPrice > 0`
   - `transmission` 在 ["auto", "manual"] 中

3. **更新改为 PATCH 语义**：仅更新传入的非 null 字段
```java
@PutMapping("/{id}")
public ApiResponse<Vehicle> update(@PathVariable Long id, @RequestBody UpdateVehicleRequest request) {
    return vehicleRepository.findById(id)
        .map(vehicle -> {
            if (request.getName() != null) vehicle.setName(request.getName());
            if (request.getBrand() != null) vehicle.setBrand(request.getBrand());
            if (request.getSeats() != null) vehicle.setSeats(request.getSeats());
            // ... 其他字段同理，仅非 null 才更新
            return ApiResponse.success(vehicleRepository.save(vehicle));
        })
        .orElse(ApiResponse.error(ErrorCode.NOT_FOUND));
}
```

4. **新增独立请求类**：
   - `CreateVehicleRequest`：所有必填字段
   - `UpdateVehicleRequest`：所有字段可选（PATCH 语义）

5. **删除前检查订单关联**：已有订单的车辆不可删除
```java
@DeleteMapping("/{id}")
public ApiResponse<Void> delete(@PathVariable Long id) {
    // 检查是否有关联订单
    if (orderRepository.hasOrdersForVehicle(id)) {
        return ApiResponse.error(4000, "该车辆有关联订单，无法删除");
    }
    vehicleRepository.softDelete(id);
    return ApiResponse.success(null);
}
```

**OrderRepository 新增方法**：
```java
boolean hasOrdersForVehicle(Long vehicleId);
```

### 2.4 P1-7: 仪表盘 API 增强

**现状**：`DashboardController.overview` 已实现 6 个指标，缺少 `pending_orders`。

**需要修改的文件**：
- `controller/DashboardController.java`

**新增字段**：
```java
// 待确认订单数（小程序管理端用）
LambdaQueryWrapper<OrderDO> pendingWrapper = new LambdaQueryWrapper<>();
pendingWrapper.eq(OrderDO::getStatus, "pending");
long pendingOrders = orderMapper.selectCount(pendingWrapper);
result.put("pending_orders", pendingOrders);
```

**响应扩展**：
```json
{
  "code": 0,
  "data": {
    "today_orders": 3,
    "today_revenue": 1500.00,
    "month_orders": 45,
    "month_revenue": 28000.00,
    "active_orders": 2,
    "available_vehicles": 8,
    "pending_orders": 5
  },
  "message": "success"
}
```

### 2.5 P1-4/5/6: PC 管理端前端

#### 2.5.1 需要新建的文件

```
frontend-admin/src/
├── api/
│   ├── request.js              # Axios 封装 + token 拦截器
│   ├── auth.js                 # 登录 API
│   ├── vehicle.js              # 车辆管理 API
│   ├── order.js                # 订单管理 API
│   ├── dashboard.js            # 仪表盘 API
│   ├── pricing.js              # 节假日管理 API
│   └── agreement.js            # 协议管理 API
├── router/
│   └── index.js                # Vue Router 路由配置
├── store/
│   └── auth.js                 # Pinia 登录态
└── utils/
    └── auth.js                 # 路由守卫
```

#### 2.5.2 需要完善的视图文件

| 文件 | 需要完成的内容 |
|------|--------------|
| `views/dashboard/DashboardView.vue` | 统计卡片 + 简单图表（推荐 ECharts 或 Element Plus 内置组件） |
| `views/vehicles/VehicleView.vue` | el-table + 创建/编辑对话框 + 上下架按钮 + 图片上传(base64) |
| `views/orders/OrderView.vue` | el-table + 状态筛选 + 确认/拒绝/开始/完成按钮 |

#### 2.5.3 关键组件设计

**VehicleView.vue** 核心结构：
```vue
<template>
  <div>
    <!-- 操作栏 -->
    <el-button @click="showCreateDialog">新增车辆</el-button>

    <!-- 数据表格 -->
    <el-table :data="vehicles" v-loading="loading">
      <el-table-column prop="name" label="车型" />
      <el-table-column prop="brand" label="品牌" />
      <el-table-column prop="seats" label="座位" />
      <el-table-column prop="transmission" label="变速箱" />
      <el-table-column label="封面" width="80">
        <template #default="{ row }">
          <el-image :src="row.images[0]" style="width: 60px" />
        </template>
      </el-table-column>
      <el-table-column prop="weekday_price" label="工作日价" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button @click="editVehicle(row)">编辑</el-button>
          <el-button @click="toggleStatus(row)">
            {{ row.status === 'active' ? '下架' : '上架' }}
          </el-button>
          <el-popconfirm title="确认删除?" @confirm="deleteVehicle(row.id)">
            <template #reference>
              <el-button type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination v-model:current-page="page" :total="total" @current-change="fetchVehicles" />

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑车辆' : '新增车辆'">
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item label="车型" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="品牌" prop="brand"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="座位" prop="seats"><el-input-number v-model="form.seats" /></el-form-item>
        <el-form-item label="变速箱" prop="transmission">
          <el-select v-model="form.transmission">
            <el-option label="自动" value="auto" />
            <el-option label="手动" value="manual" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片" prop="images">
          <!-- 多图上传，转 base64 -->
        </el-form-item>
        <el-form-item label="工作日价" prop="weekday_price"><el-input-number v-model="form.weekday_price" /></el-form-item>
        <el-form-item label="周末价" prop="weekend_price"><el-input-number v-model="form.weekend_price" /></el-form-item>
        <el-form-item label="节假日价" prop="holiday_price"><el-input-number v-model="form.holiday_price" /></el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>
```

**OrderView.vue** 核心结构：
```vue
<template>
  <div>
    <!-- 筛选栏 -->
    <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="fetchOrders">
      <el-option label="待确认" value="pending" />
      <el-option label="已确认" value="confirmed" />
      <el-option label="进行中" value="in_progress" />
      <el-option label="已完成" value="completed" />
      <el-option label="已拒绝" value="rejected" />
      <el-option label="已取消" value="cancelled" />
    </el-select>

    <!-- 数据表格 -->
    <el-table :data="orders" v-loading="loading">
      <el-table-column prop="id" label="订单号" width="80" />
      <el-table-column prop="userPhone" label="用户" />
      <el-table-column prop="vehicleName" label="车型" />
      <el-table-column prop="startDate" label="取车日期" />
      <el-table-column prop="endDate" label="还车日期" />
      <el-table-column prop="totalPrice" label="总价" />
      <el-table-column prop="statusLabel" label="状态" />
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending'" @click="confirmOrder(row.id)" type="success">确认</el-button>
          <el-button v-if="row.status === 'pending'" @click="rejectOrder(row.id)" type="danger">拒绝</el-button>
          <el-button v-if="row.status === 'confirmed'" @click="startOrder(row.id)">开始</el-button>
          <el-button v-if="row.status === 'in_progress'" @click="completeOrder(row.id)">完成</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
```

### 2.6 P1-8/9: 小程序管理端

#### 2.6.1 需要新建的文件

```
frontend-mini/src/pages/admin/
├── dashboard/
│   └── index.vue             # 管理端仪表盘（简化版）
└── orders/
    └── index.vue             # 管理端订单列表（含快捷操作）
```

#### 2.6.2 TabBar 策略

MVP 方案：不动态切换 TabBar，而是：
1. 在 `pages.json` 中配置所有页面（包括 admin 页面）
2. admin 角色用户在首页或"我的"页面看到"管理入口"按钮
3. 点击后 `uni.navigateTo` 进入 admin 页面
4. 非 admin 角色访问 admin 页面时，路由守卫跳转到首页

#### 2.6.3 仪表盘页面 (admin/dashboard/index.vue)

展示 3 个核心指标：
- 待确认订单数（`pending_orders`）— 点击可跳转到订单列表
- 进行中订单数（`active_orders`）
- 今日收入（`today_revenue`）

调用 `GET /api/v1/admin/dashboard/overview`

#### 2.6.4 订单管理页面 (admin/orders/index.vue)

- 简化列表：显示订单号、车型、日期、状态
- 默认仅显示 `pending` + `in_progress` 状态的订单
- 待确认订单：显示"确认"/"拒绝"按钮
- 进行中订单：显示"完成"按钮
- 支持下拉刷新

#### 2.6.5 角色守卫

在 `frontend-mini/src/utils/auth-guard.js` 中新增：
```javascript
export function requireAdmin() {
  const user = uni.getStorageSync('user');
  if (!user || user.role !== 'admin') {
    uni.showToast({ title: '无权访问', icon: 'none' });
    setTimeout(() => uni.navigateBack(), 1000);
    return false;
  }
  return true;
}
```

每个 admin 页面的 `onLoad` 中调用：
```javascript
import { requireAdmin } from '@/utils/auth-guard';

export default {
  onLoad() {
    if (!requireAdmin()) return;
    // 正常加载页面数据
  }
};
```

### 2.7 P1-10: 节假日定价管理 + 协议版本修复

这部分已有详细设计在现有文件中（原 sprint2 设计文档），这里简要记录：

**节假日定价管理**：
- 新建 `domain/holiday/Holiday.java` + `HolidayRepository.java`
- 新建 `infrastructure/persistence/` 对应的 DO/Mapper/RepositoryImpl
- 新建 `application/pricing/HolidayAdminService.java`
- 新建 `controller/AdminPricingController.java`
- 升级 `SimplePricingEngine` 支持周末+节假日差异化定价

**协议版本修复**：
- 修改 `AgreementController.java` 的 `update()` 方法，version 从硬编码改为自动递增

**详细设计**见本文档下方的 Sprint 2 附录，或参考已有的 `docs/arch/sprint2-4-design.md` 中的 Feature 1 和 Feature 2 部分。

### 2.8 Sprint 2 文件变更汇总

#### 新建文件（后端）

| # | 文件路径 | 说明 |
|---|---------|------|
| 1 | `backend/.../domain/holiday/Holiday.java` | 节假日聚合根 |
| 2 | `backend/.../domain/holiday/HolidayRepository.java` | 节假日仓储接口 |
| 3 | `backend/.../infrastructure/persistence/dataobject/HolidayConfigDO.java` | 节假日 DO |
| 4 | `backend/.../infrastructure/persistence/mapper/HolidayConfigMapper.java` | MyBatis Mapper |
| 5 | `backend/.../infrastructure/persistence/repository/HolidayRepositoryImpl.java` | 仓储实现 |
| 6 | `backend/.../application/pricing/CreateHolidayCommand.java` | 命令对象 |
| 7 | `backend/.../application/pricing/HolidayAdminService.java` | 节假日应用服务 |
| 8 | `backend/.../controller/AdminPricingController.java` | 节假日管理 API |

#### 新建文件（前端-PC 管理端）

| # | 文件路径 | 说明 |
|---|---------|------|
| 9 | `frontend-admin/src/api/request.js` | Axios 封装 |
| 10 | `frontend-admin/src/api/auth.js` | 认证 API |
| 11 | `frontend-admin/src/api/vehicle.js` | 车辆 API |
| 12 | `frontend-admin/src/api/order.js` | 订单 API |
| 13 | `frontend-admin/src/api/dashboard.js` | 仪表盘 API |
| 14 | `frontend-admin/src/api/pricing.js` | 节假日 API |
| 15 | `frontend-admin/src/api/agreement.js` | 协议 API |
| 16 | `frontend-admin/src/router/index.js` | 路由配置 |
| 17 | `frontend-admin/src/store/auth.js` | 登录态管理 |

#### 新建文件（前端-小程序）

| # | 文件路径 | 说明 |
|---|---------|------|
| 18 | `frontend-mini/src/pages/admin/dashboard/index.vue` | 管理端仪表盘 |
| 19 | `frontend-mini/src/pages/admin/orders/index.vue` | 管理端订单 |
| 20 | `frontend-mini/src/api/admin.js` | 管理端 API 封装 |

#### 修改文件

| 文件 | 变更说明 |
|------|---------|
| `controller/AdminOrderController.java` | DTO 增强 + 日期范围筛选 |
| `controller/AdminVehicleController.java` | 分页 + PATCH + 校验 + 删除检查 |
| `controller/DashboardController.java` | 新增 pending_orders |
| `controller/AgreementController.java` | version 自动递增 |
| `infrastructure/pricing/SimplePricingEngine.java` | 接入节假日+周末定价 |
| `domain/order/OrderRepository.java` | 新增 hasOrdersForVehicle + 日期筛选 |
| `infrastructure/.../OrderRepositoryImpl.java` | 实现新增方法 |
| `frontend-admin/src/views/dashboard/DashboardView.vue` | 完善仪表盘 UI |
| `frontend-admin/src/views/vehicles/VehicleView.vue` | 完善车辆管理 UI |
| `frontend-admin/src/views/orders/OrderView.vue` | 完善订单管理 UI |
| `frontend-admin/src/views/pricing/PricingView.vue` | 完善节假日管理 UI |
| `frontend-admin/src/views/agreement/AgreementView.vue` | 完善协议管理 UI |
| `frontend-mini/src/utils/auth-guard.js` | 新增 requireAdmin 守卫 |

---

## 3. Sprint 3 - P2 体验优化（~9.5h）

### 3.1 P2-1: 真实微信登录

**现状**：`AuthController` 已有 `wxLogin` 端点，`AuthService.wxLogin` 已实现真实的微信登录流程（code2Session + getPhoneNumber）。但 `AuthController` 可能仍有 mock 登录端点共存。

**需要确认的文件**：
- `controller/AuthController.java` — 确认 wx-login 端点已正确暴露
- `application/auth/AuthService.java` — 已实现，无需修改

**流程**（已实现）：
```
小程序 wx.login() → loginCode
小程序 getUserPhoneNumber() → phoneCode
POST /api/v1/auth/wx-login { loginCode, phoneCode }
  → AuthService.wxLogin()
    → WxMaService.code2Session(loginCode) → openid
    → WxMaService.getNewPhoneNoInfo(phoneCode) → phone
    → userRepository.findByOpenid(openid) → 不存在则创建
    → jwtUtil.generateToken(userId, role)
    → 返回 { token, user, isNewUser }
```

**Sprint 3 任务**：
1. 移除或保护 mock 登录端点（仅 dev profile 可用）
2. 小程序端实现真实登录流程，替换 mock 登录
3. 确认 `WxConfig` 正确配置 appId/secret

**需要修改的文件**：
- `controller/AuthController.java` — mock 登录端点添加 `@Profile("dev")` 注解
- `frontend-mini/src/pages/login/index.vue` — 接入真实 wx.login
- `frontend-mini/src/api/auth.js` — 确认 wx-login API 调用正确

### 3.2 P2-2: 订阅消息推送

**现状**：
- `message_subscriptions` 表已存在
- `POST /api/v1/subscription/record` API 已实现
- 缺少发送订阅消息的逻辑

**需要新建的文件**：
- `infrastructure/wechat/WxSubscribeService.java` — 封装微信订阅消息发送

**需要修改的文件**：
- `controller/AdminOrderController.java` — confirm/reject 后调用推送

**WxSubscribeService 设计**：
```java
@Service
@RequiredArgsConstructor
public class WxSubscribeService {

    private final WxMaService wxMaService;
    private final MessageSubscriptionMapper subscriptionMapper;
    private final UserMapper userMapper;

    /**
     * 发送订单确认通知
     */
    public void sendOrderConfirmed(Long orderId) {
        OrderDO order = orderMapper.selectById(orderId);
        UserDO user = userMapper.selectById(order.getUserId());

        // 查询用户是否授权了该模板
        MessageSubscriptionDO sub = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<MessageSubscriptionDO>()
                .eq(MessageSubscriptionDO::getUserId, user.getId())
                .eq(MessageSubscriptionDO::getTemplateId, getTemplateId("order_confirmed"))
        );
        if (sub == null) return; // 用户未授权，跳过

        // 构建消息
        WxMaSubscribeMessage msg = WxMaSubscribeMessage.builder()
            .toUser(user.getWechatOpenid())
            .templateId(getTemplateId("order_confirmed"))
            .page("/pages/order-detail/index?id=" + order.getId())
            .data(List.of(
                new WxMaSubscribeMessage.MsgData("thing1", "订单已确认"),
                new WxMaSubscribeMessage.MsgData("thing2", order.getVehicleName()),
                new WxMaSubscribeMessage.MsgData("time3", order.getStartDate().toString()),
                new WxMaSubscribeMessage.MsgData("amount4", order.getTotalPrice() + "元")
            ))
            .build();

        wxMaService.getMsgService().sendSubscribeMsg(msg);
    }

    /**
     * 发送订单拒绝通知
     */
    public void sendOrderRejected(Long orderId, String reason) {
        // 类似实现，模板不同
    }
}
```

**集成点**（AdminOrderController）：
```java
@PostMapping("/{id}/confirm")
public ApiResponse<?> confirm(@PathVariable Long id) {
    return orderRepository.findById(id)
        .map(order -> {
            order.confirm();
            orderRepository.save(order);
            // Sprint 3 新增: 发送订阅消息
            wxSubscribeService.sendOrderConfirmed(order.getId());
            // ...
        });
}
```

**配置**（application.yml）：
```yaml
wx:
  miniapp:
    appid: ${WX_APPID}
    secret: ${WX_SECRET}
    subscribe-message:
      template-ids:
        order_confirmed: ${WX_TEMPLATE_ORDER_CONFIRMED}
        order_rejected: ${WX_TEMPLATE_ORDER_REJECTED}
        order_ready: ${WX_TEMPLATE_ORDER_READY}
```

### 3.3 P2-3: 全局 Loading + 错误处理

**小程序端需要修改的文件**：
- `frontend-mini/src/api/request.js` — timeout 10s → 5s + 网络错误友好提示
- `frontend-mini/src/utils/loading.js` — 新建 loading 工具

**request.js 变更**：
```javascript
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 5000,  // Sprint 3: 10s → 5s
});

service.interceptors.response.use(
  response => {
    const { code, data, message } = response.data;
    if (code === 0) return data;
    // 业务错误
    uni.showToast({ title: message || '请求失败', icon: 'none' });
    return Promise.reject(new Error(message));
  },
  error => {
    let msg = '网络异常，请检查网络';
    if (error.message?.includes('timeout')) msg = '请求超时，请稍后重试';
    if (error.message?.includes('Network Error')) msg = '无法连接服务器';
    uni.showToast({ title: msg, icon: 'none' });
    return Promise.reject(error);
  }
);
```

**loading 工具**：
```javascript
// frontend-mini/src/utils/loading.js
let loadingCount = 0;

export function showLoading() {
  if (loadingCount === 0) {
    uni.showLoading({ title: '加载中...', mask: true });
  }
  loadingCount++;
}

export function hideLoading() {
  loadingCount--;
  if (loadingCount <= 0) {
    loadingCount = 0;
    uni.hideLoading();
  }
}
```

**使用方式**（在 request 拦截器中自动调用）：
```javascript
service.interceptors.request.use(config => {
  if (config.showLoading !== false) {
    showLoading();
  }
  return config;
});

service.interceptors.response.use(
  data => { hideLoading(); return data; },
  error => { hideLoading(); return Promise.reject(error); }
);
```

### 3.4 P2-4: 首页价格筛选

**现状**：后端 `GET /api/v1/vehicles` 已支持 `min_price` / `max_price` 参数。

**需要修改的文件**：
- `frontend-mini/src/pages/index/index.vue` — 添加价格区间滑块

**实现**：
```vue
<template>
  <!-- 价格筛选区域 -->
  <view class="price-filter">
    <text>价格范围</text>
    <slider
      :value="priceRange"
      :min="0"
      :max="1000"
      :step="50"
      range
      @change="onPriceChange"
    />
    <text>{{ priceRange[0] }} - {{ priceRange[1] }} 元/天</text>
  </view>

  <!-- 车辆列表 -->
  <view v-for="v in vehicles" :key="v.id">
    <!-- ... -->
  </view>
</template>

<script setup>
const priceRange = ref([0, 1000]);

const fetchVehicles = async () => {
  const res = await getVehicles({
    page: currentPage.value,
    min_price: priceRange.value[0],
    max_price: priceRange.value[1],
  });
  vehicles.value = res.items;
};

const onPriceChange = (e) => {
  priceRange.value = e.detail.value;
  currentPage.value = 1;
  fetchVehicles();
};
</script>
```

### 3.5 P2-5: 可配置取车地址

**现状**：取车地址硬编码。

**方案**：MVP 阶段保持简单，在 PC 管理端通过环境变量或常量配置，暂不引入 system_configs 表。

**需要修改的文件**：
- `backend/src/main/resources/application.yml` — 新增配置
```yaml
car-rental:
  pickup:
    address: 北京市朝阳区XX路XX号
    phone: "138xxxx8000"
    instructions: 到达后联系管理员取车
```
- `controller/OrderController.java` 或新建 `ConfigController.java` — 暴露取车地址
- `frontend-mini/src/store/config.js` — 从 API 获取而非硬编码

**新增 API**（可选，MVP 阶段可跳过）：
```
GET /api/v1/config/pickup
Response: { "address": "...", "phone": "...", "instructions": "..." }
```

**MVP 简化方案**：在小程序端通过环境变量配置，不新增后端 API：
```javascript
// frontend-mini/src/store/config.js
export const PICKUP_ADDRESS = import.meta.env.VITE_PICKUP_ADDRESS || '北京市朝阳区XX路XX号';
export const PICKUP_INSTRUCTIONS = import.meta.env.VITE_PICKUP_INSTRUCTIONS || '到达后联系管理员取车';
```

### 3.6 P2-6: PC 管理端 - 价格设置页面

**需要修改的文件**：
- `frontend-admin/src/views/pricing/PricingView.vue`

**功能**：
- el-table 展示节假日列表（名称、日期范围、倍率、固定价格、年份）
- 年份筛选器
- 新增节假日对话框（el-dialog + el-date-picker + el-form）
- 批量导入功能（可选，手动逐条添加即可 MVP）
- 删除节假日按钮

**数据流**：
```
页面加载 → GET /api/v1/admin/pricing/holidays?year=2026 → 渲染表格
新增 → 填写表单 → POST /api/v1/admin/pricing/holidays → 刷新列表
批量 → 填写多条 → POST /api/v1/admin/pricing/holidays/batch → 刷新列表
删除 → 前端暂无删除 API，可在 Sprint 3 补充 DELETE 端点
```

### 3.7 P2-7: PC 管理端 - 协议管理

**需要修改的文件**：
- `frontend-admin/src/views/agreement/AgreementView.vue`

**功能**：
- el-table 展示协议历史版本（版本号、创建时间、是否生效）
- 编辑当前协议（el-input textarea）
- 保存后 version 自动递增（后端已修复）
- 查看历史版本（只读）

### 3.8 Sprint 3 文件变更汇总

#### 新建文件

| # | 文件路径 | 说明 |
|---|---------|------|
| 1 | `backend/.../infrastructure/wechat/WxSubscribeService.java` | 订阅消息封装 |
| 2 | `frontend-mini/src/utils/loading.js` | Loading 管理 |

#### 修改文件

| 文件 | 变更说明 |
|------|---------|
| `controller/AuthController.java` | mock 登录限制 dev profile |
| `controller/AdminOrderController.java` | confirm/reject 后调用订阅消息 |
| `controller/AdminPricingController.java` | 可选: 新增 DELETE 端点 |
| `frontend-mini/src/api/request.js` | timeout 5s + 网络错误处理 + loading |
| `frontend-mini/src/pages/login/index.vue` | 真实微信登录 |
| `frontend-mini/src/pages/index/index.vue` | 价格筛选滑块 |
| `frontend-mini/src/store/config.js` | 环境变量配置取车地址 |
| `frontend-admin/src/views/pricing/PricingView.vue` | 完善节假日管理 UI |
| `frontend-admin/src/views/agreement/AgreementView.vue` | 完善协议管理 UI |

---

## 4. Sprint 4 - P3 支付集成（~10.5h）

### 4.1 P3-1: 微信支付集成

**需要新建的文件**：
- `infrastructure/wechat/WxPayService.java` — 封装微信支付（统一下单、退款、回调验签）
- `controller/PayController.java` — 支付相关 API
- `application/payment/PaymentAppService.java` — 支付应用服务

**支付流程**：
```
小程序: 订单详情页 → 点击"去支付"
  → POST /api/v1/orders/{id}/pay
  → PaymentAppService.initiatePayment(orderId, openid)
    → 检查订单状态 == confirmed
    → 生成 payment_no（内部流水号）
    → 创建 PaymentDO（type=pay, status=pending）
    → 调用 WxPayService.createJsapiPayment(order)
      → WxPay JSAPI 统一下单
      → 返回 { timeStamp, nonceStr, package, signType, paySign }
  → 小程序调用 wx.requestPayment(params)
  → 微信服务器处理支付
  → 微信回调 /api/v1/pay/callback
    → 验签 + 解析回调
    → 更新 PaymentDO（status=success）
    → 更新 OrderDO（payment_status=paid, paid_at=now）
    → 返回 success XML 给微信
```

**支付结果 DTO**：
```java
@Data
public class PayResult {
    private String timeStamp;
    private String nonceStr;
    private String packageValue;  // "prepay_id=wx..."
    private String signType;      // "RSA"
    private String paySign;
}
```

**数据库利用**：`payments` 表已在 V1 迁移中创建，无需新增迁移脚本。

### 4.2 P3-2: 小程序支付流程

**需要修改的文件**：
- `frontend-mini/src/pages/order-detail/index.vue` — 添加支付按钮
- `frontend-mini/src/api/order.js` — 添加 pay 方法

支付按钮仅在 `payment_status === 'unpaid' && status === 'confirmed'` 时显示。点击后调用 `wx.requestPayment`，根据结果刷新订单状态。

### 4.3 P3-3: 退款流程

**API 端点**：`POST /api/v1/admin/orders/{id}/refund`（管理员权限）

退款流程：查询原支付记录 → 创建退款记录 → 调用微信退款 API → 更新订单 `payment_status=refunded`。

### 4.4 P3-4: 图片迁移到 OSS（可选）

MVP 阶段保持 base64。若接入腾讯云 COS：
- 新增 `OssService` 上传服务
- `vehicles.images` 内容从 base64 改为 URL
- 前端兼容两种格式（判断是否以 `data:` 开头）

### 4.5 P3-5: 完整 PricingEngine 验证

Sprint 2 已升级定价引擎支持周末+节假日差异化定价。Sprint 4 需补充：
1. 确认定价优先级：holiday > weekend > weekday
2. 新增单元测试覆盖边界场景（节假日与周末重叠、跨月租期等）
3. 确认 `price_breakdown` 中 `type` 字段正确标注

### 4.6 Sprint 4 文件变更汇总

**新建文件**：
| 文件路径 | 说明 |
|---------|------|
| `backend/.../infrastructure/wechat/WxPayService.java` | 微信支付封装 |
| `backend/.../application/payment/PaymentAppService.java` | 支付应用服务 |
| `backend/.../controller/PayController.java` | 支付 API |

**修改文件**：
| 文件 | 变更说明 |
|------|---------|
| `controller/AdminOrderController.java` | 新增 refund 端点 |
| `frontend-mini/src/pages/order-detail/index.vue` | 添加支付按钮 |
| `frontend-mini/src/api/order.js` | 添加 pay 方法 |

---

## 5. 数据库迁移脚本

### 5.1 不需要新增迁移脚本

Sprint 2/3/4 所需的所有表已在 `V1__init_tables.sql` 中创建：
- `holiday_configs` — 节假日配置（Sprint 2 使用）
- `message_subscriptions` — 订阅消息记录（Sprint 3 使用）
- `payments` — 支付记录（Sprint 4 使用）
- `user_agreements` — 用户协议（Sprint 2 修复 version bug）

### 5.2 可能需要的增量迁移（V2）

如果后续需要扩展字段，创建 `V2__sprint2_3_4_enhancements.sql`：

```sql
-- V2__sprint2_3_4_enhancements.sql
-- Sprint 2/3/4 增量变更

-- 1. 系统配置表（可选，用于可配置取车地址）
-- CREATE TABLE IF NOT EXISTS system_configs (
--     id              BIGINT AUTO_INCREMENT PRIMARY KEY,
--     config_key      VARCHAR(100) NOT NULL UNIQUE,
--     config_value    TEXT NOT NULL,
--     description     VARCHAR(200) DEFAULT NULL,
--     created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 6. 风险评估

### 6.1 Sprint 2 风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| AdminOrderController DTO 关联查询性能差 | 中 | 中 | 数据量小（MVP），批量查询在应用层组合即可 |
| 车辆图片 base64 导致 API 响应过大 | 低 | 低 | 车辆有限（<20辆），MVP 可接受 |
| PC 管理端前端缺少 API 层封装 | 中 | 中 | Sprint 2 优先创建 api/ 目录封装文件 |
| 节假日定价引擎逻辑复杂 | 中 | 中 | 已有详细设计，优先级规则清晰，需充分测试 |

### 6.2 Sprint 3 风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 微信订阅消息需要真实 appId，本地无法测试 | 高 | 中 | 使用微信测试号，或 Mock 发送逻辑 |
| 订阅消息推送失败影响订单流程 | 低 | 中 | 推送异常不阻断订单流程，仅记录日志 |
| 真实微信登录依赖外网回调 | 高 | 中 | 使用 ngrok 内网穿透，或保持 dev mock 登录 |

### 6.3 Sprint 4 风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 微信支付需要商户号，审批周期长 | 高 | 高 | 提前申请商户号，Sprint 4 前完成 |
| 支付回调验签复杂 | 中 | 高 | 使用 WxJava SDK 封装的验签逻辑 |
| 支付幂等性（重复回调） | 中 | 高 | payment_no 唯一索引 + 状态检查 |
| OSS 迁移影响现有功能 | 低 | 中 | 前端兼容 base64 和 URL 两种格式 |

### 6.4 通用风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 2C4G 服务器资源不足 | 低 | 中 | MVP 阶段用户量少，后期监控 |
| 微信 API 变更 | 低 | 高 | 使用 WxJava SDK（社区维护） |

---

## 7. 并行执行计划

### 7.1 Sprint 2 执行顺序

```
Week 1: 后端完善（可并行）
  ├── P1-1: AdminOrderController DTO 增强     [2h] ── 后端开发 A
  ├── P1-3: AdminVehicleController 完善       [2h] ── 后端开发 B
  ├── P1-7: Dashboard 增强                    [0.5h] ── 后端开发 A（与 P1-1 串行）
  ├── P1-10: 节假日定价管理                   [3h] ── 后端开发 C（独立）
  └── P1-10: 协议版本修复                     [0.5h] ── 后端开发 A（简单修复）

Week 1: PC 管理端前端（可并行）
  ├── API 层封装 (request.js + 各模块 api)    [2h] ── 前端开发 A
  ├── DashboardView 完善                      [1.5h] ── 前端开发 B
  ├── VehicleView 完善                        [2h] ── 前端开发 B
  ├── OrderView 完善                          [1.5h] ── 前端开发 A
  ├── PricingView 完善                        [1h] ── 前端开发 A
  └── AgreementView 完善                      [0.5h] ── 前端开发 B

Week 2: 小程序管理端
  ├── admin/dashboard/index.vue               [1h]
  ├── admin/orders/index.vue                  [1.5h]
  ├── auth-guard 守卫                         [0.5h]
  └── pages.json 路由配置                     [0.5h]
```

**依赖关系**：
- 后端 API 必须先完成，前端才能联调
- PC 前端和小程序前端可以并行开发（共用同一套后端 API）
- 节假日定价管理是独立模块，可与其他后端任务并行

### 7.2 Sprint 3 执行顺序

```
Week 3:
  ├── P2-1: 真实微信登录                      [1.5h] ── 前后端联调
  ├── P2-2: 订阅消息推送                      [2h] ── 后端
  ├── P2-3: 全局 loading + 错误处理            [1.5h] ── 前端
  ├── P2-4: 首页价格筛选                      [1h] ── 前端（后端已支持）
  ├── P2-5: 可配置取车地址                    [0.5h] ── 前端配置
  ├── P2-6: PC 价格设置页面                   [1.5h] ── 前端（依赖 Sprint 2）
  └── P2-7: PC 协议管理页面                   [1h] ── 前端（依赖 Sprint 2）
```

**依赖关系**：
- P2-6 依赖 Sprint 2 的节假日管理 API
- P2-7 依赖 Sprint 2 的协议 version 修复
- 其余任务可并行

### 7.3 Sprint 4 执行顺序

```
Week 4:
  ├── P3-1: 微信支付集成                      [3h] ── 后端
  ├── P3-2: 小程序支付流程                    [2h] ── 前端（依赖 P3-1）
  ├── P3-3: 退款流程                          [1.5h] ── 后端 + 前端
  ├── P3-4: 图片迁移 OSS（可选）              [2h] ── 后端 + 前端
  └── P3-5: PricingEngine 完整测试            [1h] ── 测试
```

**依赖关系**：
- P3-2 严格依赖 P3-1（支付 API）
- P3-3 依赖 P3-1（支付基础设施）
- P3-4 相对独立，可与 P3-1 并行
- P3-5 依赖 Sprint 2 的定价引擎升级

### 7.4 跨 Sprint 依赖

```
Sprint 2 ──必选完成──> Sprint 3
  ├── 节假日管理 API ──> P2-6 价格设置页面
  └── 协议 version 修复 ──> P2-7 协议管理页面

Sprint 2 ──必选完成──> Sprint 4
  └── 完整 PricingEngine ──> P3-5 测试验证

Sprint 3 ──可选依赖──> Sprint 4
  └── 订阅消息推送 ──> 支付成功通知（可选增强）
```

---

## 附录: 订单状态流转图

```
                  用户取消
     ┌──────────────────────────────┐
     │                              │
     ▼                              │
  ┌───────┐   确认    ┌───────────┐  拒绝
  │pending├─────────>│ confirmed  │<────┐
  └───┬───┘          └─────┬─────┘     │
      │                    │           │
      │ 拒绝               │ start     │
      ▼                    ▼           │
  ┌─────────┐        ┌───────────┐    │
  │rejected │        │in_progress│    │
  └─────────┘        └─────┬─────┘    │
                           │          │
                           │ complete │
                           ▼          │
                      ┌───────────┐   │
                      │ completed │   │
                      └───────────┘   │
                                      │
                           ┌──────────┘
                           │
                     创建订单
```

## 附录: 错误码总览

| 错误码 | 说明 | 使用场景 |
|--------|------|---------|
| 0 | 成功 | 所有成功响应 |
| 4000 | 参数错误 | 校验失败 |
| 4001 | 微信登录失败 | wx-login |
| 4002 | 手机号获取失败 | wx-login |
| 4003 | 未登录/Token 无效 | JWT 过期或无效 |
| 4004 | 资源不存在 | ID 不存在 |
| 4010 | 无权操作 | 角色不匹配 |
| 5000 | 服务器内部错误 | 未预期异常 |
| 5100 | 微信支付失败 | 支付异常 |
| 5101 | 退款失败 | 退款异常 |
| 5200 | 时间段冲突 | 车辆已被预订 |
| 5300 | 订单状态不允许 | 非法状态转换 |
| 5400 | 节假日日期重叠 | 节假日配置（Sprint 2） |