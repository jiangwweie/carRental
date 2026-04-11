# 合同合规修复设计文档

> 基于合同审查发现的 22 个问题（7 P0、9 P1、6 P2），本文档提供分阶段修复方案。
>
> API 版本: v2.0 | 文档日期: 2026-04-11 | 状态: 待评审

---

## 1. 修复策略总览

### 1.1 分阶段执行

修复按照**依赖关系**分为四个阶段，前一个阶段未完成不应进入下一个阶段：

| 阶段 | 内容 | 优先级 | 说明 |
|------|------|--------|------|
| Phase 0 | Jackson snake_case 全局配置 | P0 | **最优先**，影响所有后端 JSON 输出 |
| Phase 1 | 后端缺失端点和 DTO 字段 | P0-P1 | 补全合同定义但缺失的后端能力 |
| Phase 2 | 前端 API 调用和字段映射 | P0-P2 | 适配后端实际输出 |
| Phase 3 | 合同文档对齐 | P2 | 文档层面的修正 |

### 1.2 关键设计决策

**决策 1：Jackson snake_case 采用全局配置而非逐字段 @JsonProperty**

| 方案 | 优点 | 缺点 |
|------|------|------|
| 全局 `SNAKE_CASE` | 一行配置解决 90% 问题，一致性好 | 需验证现有前端代码不会被破坏 |
| 逐字段 @JsonProperty | 精确控制，不影响已有字段 | 改动量大，容易遗漏，维护成本高 |

**推荐：全局配置**。合同中所有字段均为 snake_case（`order_id`、`vehicle_name`、`total_price`、`status_label` 等），Java 代码使用 camelCase。一行全局配置 `spring.jackson.property-naming-strategy: SNAKE_CASE` 即可自动转换，与合同完全对齐。

**决策 2：前端统一使用 snake_case**

配置 Jackson snake_case 后，前端应从 API 返回中直接读取 snake_case 字段（如 `res.order_id`），与合同保持一致。不再在前端做 camelCase 映射。

**决策 3：Mock 数据保留为降级兜底**

保留 mock 数据作为开发环境的最后兜底（后端未启动时的 fallback），但正常路径必须调用真实 API。Mock 数据仅在 catch 块中使用。

### 1.3 字段名称映射表（Jackson snake_case 生效前后）

| Java 字段 (camelCase) | 序列化后 (snake_case) | 前端读取方式 |
|---|---|---|
| `orderId` | `order_id` | `res.order_id` |
| `vehicleName` | `vehicle_name` | `item.vehicle_name` |
| `totalPrice` | `total_price` | `item.total_price` |
| `statusLabel` | `status_label` | `item.status_label` |
| `createdAt` | `created_at` | `item.created_at` |
| `canCancel` | `can_cancel` | `item.can_cancel` |
| `vehicleImage` | `vehicle_image` | `item.vehicle_image` |
| `priceBreakdown` | `price_breakdown` | `res.price_breakdown` |
| `pickupAddress` | `pickup_address` | `res.pickup_address` |
| `rejectReason` | `reject_reason` | `res.reject_reason` |
| `paymentStatus` | `payment_status` | `res.payment_status` |
| `weekdayPrice` | `weekday_price` | `item.weekday_price` |
| `coverImage` | `cover_image` | `item.cover_image` |
| `isNewUser` | `is_new_user` | `res.is_new_user` |
| `mustChangePwd` | `must_change_pwd` | `user.must_change_pwd` |

---

## 2. 文件变更矩阵

| # | 文件路径 | 变更类型 | 优先级 | 涉及问题 | 说明 |
|---|---------|---------|--------|---------|------|
| 1 | `backend/src/main/resources/application.yml` | 修改 | P0 | P0-1 | 添加 Jackson snake_case 配置 |
| 2 | `backend/.../controller/AuthController.java` | 修改 | P0 | P0-2 | 添加 `/auth/refresh` 端点 |
| 3 | `backend/.../controller/OrderController.java` | 修改 | P0 | P0-3 | CreateOrderResult 补全缺失字段 |
| 4 | `backend/.../controller/AdminVehicleController.java` | 修改 | P1 | P1-8 | 车辆列表 status 筛选未生效 |
| 5 | `backend/.../controller/DashboardController.java` | 修改 | P1 | P1-9, P1-10 | 添加 pending_orders，修复 today_revenue |
| 6 | `backend/.../controller/SubscriptionController.java` | 修改 | P1 | P1-14 | 返回 { status: "accepted" } 而非 null |
| 7 | `frontend-mini/src/pages/orders/orders.vue` | 修改 | P0 | P0-4, P1-11 | 替换 mock 为真实 API 调用，修复 tab 值 |
| 8 | `frontend-mini/src/pages/booking/booking.vue` | 修改 | P0 | P0-5, P1-13 | 修复字段映射（total_price/price_breakdown/order_id） |
| 9 | `frontend-mini/src/pages/order-detail/order-detail.vue` | 修改 | P0 | P0-6 | camelCase 改为 snake_case 字段访问 |
| 10 | `frontend-mini/src/pages/index/index.vue` | 修改 | P0 | P0-7, P1-12 | 移除错误参数，修复 hasMore 判断 |
| 11 | `frontend-mini/src/pages/vehicle-detail/vehicle-detail.vue` | 修改 | P2 | P2-21 | 修复 transmission mock 值 |
| 12 | `frontend-mini/src/pages/agreement/agreement.vue` | 修改 | P2 | P2-22 | 调用 GET /api/v1/agreement |
| 13 | `frontend-mini/src/api/auth.js` | 修改 | P1 | P1-16 | refreshToken 改为无 body |
| 14 | `docs/arch/api.md` | 修改 | P2 | P2-17 | admin-login 对齐为 password-only |
| 15 | `docs/contracts/api-spec.yaml` | 修改 | P2 | P2-18, P2-20 | 修复 total_price 类型，status_steps 结构 |

---

## 3. 详细修复方案

### Phase 0: Jackson snake_case 全局配置

#### P0-1: 添加 Jackson snake_case 配置

**文件**: `/Users/jiangwei/Documents/carRental/backend/src/main/resources/application.yml`

**变更**: 在 `spring:` 下添加：

```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

**注意**: 添加此配置后，所有已有端点的 JSON 响应字段都会从 camelCase 变为 snake_case。前端必须同步修改（Phase 2）。

**风险评估**: 这是一个**破坏性变更**。但考虑到：
- 当前前端大量使用 mock 数据，实际对接 API 的字段名本就不对
- 合同（api.md + api-spec.yaml）定义的字段全是 snake_case
- 这是对齐合同的最佳时机

**迁移策略**: 后端 + 前端在同一批次提交中一起修改，避免中间态不一致。

---

### Phase 1: 后端修复

#### P0-2: 添加 /auth/refresh 端点

**文件**: `AuthController.java`

**问题**: 合同定义了 `POST /api/v1/auth/refresh` 端点但代码未实现。

**变更**: 在 AuthController 中添加 refresh 方法：

```java
/**
 * 刷新 Token（无需请求体，从 Authorization header 读取当前 token）
 */
@PostMapping("/refresh")
public ApiResponse<Map<String, String>> refresh(HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    String role = (String) httpRequest.getAttribute("role");
    String newToken = jwtUtil.generateToken(userId, role);
    Map<String, String> result = new HashMap<>();
    result.put("token", newToken);
    return ApiResponse.success(result);
}
```

需要添加 import: `import java.util.HashMap;` 和 `import java.util.Map;`

---

#### P0-3: CreateOrderResult 补全缺失字段

**文件**: `OrderController.java`，CreateOrderResult 内部类

**问题**: 合同要求返回 `vehicle_name`、`price_breakdown`、`status`，但当前 DTO 只有 `orderId`、`startDate`、`endDate`、`days`、`totalPrice`。

**变更**:

```java
@Data
public static class CreateOrderResult {
    private Long orderId;           // -> order_id (snake_case)
    private String vehicleName;     // -> vehicle_name (新增)
    private LocalDate startDate;    // -> start_date
    private LocalDate endDate;      // -> end_date
    private Integer days;
    private BigDecimal totalPrice;  // -> total_price
    private List<PriceBreakdown> priceBreakdown;  // -> price_breakdown (新增)
    private String status;          // 新增，固定 "pending"
}
```

同时修改 createOrder 方法中的赋值逻辑：

```java
CreateOrderResult result = new CreateOrderResult();
result.setOrderId(order.getId());
result.setVehicleName(vehicle.getName());          // 新增
result.setStartDate(order.getStartDate());
result.setEndDate(order.getEndDate());
result.setDays(order.getDays());
result.setTotalPrice(order.getTotalPrice());
result.setPriceBreakdown(priceBreakdown);          // 新增
result.setStatus("pending");                       // 新增
```

---

#### P1-8: AdminVehicle 列表 status 筛选未生效

**文件**: `AdminVehicleController.java`，`list()` 方法

**问题**: `status` 参数被接收但从未用于过滤查询，`vehicleRepository.findAllVehicles()` 返回全部车辆。

**变更**: 在 `list()` 方法中添加 status 过滤：

```java
@GetMapping
public ApiResponse<Map<String, Object>> list(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize) {
    List<Vehicle> allVehicles;
    long total;
    
    if (status != null && !status.isEmpty()) {
        allVehicles = vehicleRepository.findVehiclesByStatus(status, page, pageSize);
        total = vehicleRepository.countVehiclesByStatus(status);
    } else {
        allVehicles = vehicleRepository.findAllVehicles(page, pageSize);
        total = vehicleRepository.countAllVehicles();
    }
    
    Map<String, Object> result = new HashMap<>();
    result.put("total", total);
    result.put("items", allVehicles);
    return ApiResponse.success(result);
}
```

需要在 `VehicleRepository` 接口及 `VehicleRepositoryImpl` 实现类中补充 `findVehiclesByStatus` 和 `countVehiclesByStatus` 方法。

---

#### P1-9: Dashboard 缺少 pending_orders 字段

**文件**: `DashboardController.java`，`overview()` 方法

**问题**: 合同要求返回 `pending_orders`（待确认订单数），当前代码未查询。

**变更**: 在 `overview()` 方法中添加：

```java
// 待确认订单数（小程序端额外需要）
LambdaQueryWrapper<OrderDO> pendingWrapper = new LambdaQueryWrapper<>();
pendingWrapper.eq(OrderDO::getStatus, "pending");
long pendingOrders = orderMapper.selectCount(pendingWrapper);

// ... 在 result Map 中添加
result.put("pending_orders", pendingOrders);
```

---

#### P1-10: Dashboard today_revenue 按 created_at 过滤错误

**文件**: `DashboardController.java`，`overview()` 方法

**问题**: 今日收入应统计**今日完成**的订单，当前使用 `created_at` 过滤。应改为按 `completed_at`（完成时间）过滤。

**问题分析**: 当前 OrderDO 可能没有 `completed_at` 字段。MVP 阶段的简化方案：
- 方案 A（推荐）: 添加 `updatedAt` 字段，当订单状态变为 `completed` 时更新 `updatedAt`，用 `updatedAt` 近似 `completed_at`
- 方案 B: 在 OrderDO 中新增 `completed_at` 字段（需要 Flyway 迁移脚本）

**变更（方案 B，更严谨）**:

1. 创建 Flyway 迁移脚本 `V4__add_completed_at_to_orders.sql`:
```sql
ALTER TABLE orders ADD COLUMN completed_at DATETIME NULL;
```

2. 在订单状态变为 `completed` 时设置 `completed_at`（在 AdminOrderController 的 complete 方法中）

3. 修改 DashboardController 中的 today_revenue 查询：
```java
// 今日收入（按 completed_at 过滤）
LambdaQueryWrapper<OrderDO> todayPaidWrapper = new LambdaQueryWrapper<>();
todayPaidWrapper.ge(OrderDO::getCompletedAt, startOfDay)
        .lt(OrderDO::getCompletedAt, endOfDay)
        .eq(OrderDO::getStatus, "completed");
```

**MVP 过渡方案**: 如果暂不想加字段，可改为按 `updated_at` 过滤（前提是订单完成时确实会更新 `updated_at`）。

---

#### P1-14: SubscriptionController.record() 返回 null

**文件**: `SubscriptionController.java`，`record()` 方法

**问题**: 返回 `ApiResponse.success(null)`，body 为 null。合同要求返回 `{ "status": "accepted" }`。

**变更**:

```java
@PostMapping("/record")
public ApiResponse<Map<String, String>> record(
        @RequestBody RecordRequest request, HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    
    LambdaQueryWrapper<MessageSubscriptionDO> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(MessageSubscriptionDO::getUserId, userId)
            .eq(MessageSubscriptionDO::getTemplateId, request.getTemplateId());
    MessageSubscriptionDO existing = messageSubscriptionMapper.selectOne(wrapper);
    
    if (existing == null) {
        MessageSubscriptionDO subscription = new MessageSubscriptionDO();
        subscription.setUserId(userId);
        subscription.setTemplateId(request.getTemplateId());
        subscription.setStatus("accepted");
        messageSubscriptionMapper.insert(subscription);
    }
    
    Map<String, String> result = new HashMap<>();
    result.put("status", "accepted");
    return ApiResponse.success(result);
}
```

需要添加 `import java.util.HashMap;` 和 `import java.util.Map;`。

---

### Phase 2: 前端修复

#### P0-4: orders.vue 未调用真实 API

**文件**: `frontend-mini/src/pages/orders/orders.vue`

**问题**: `onMounted` 中从未调用 `getOrders()` API，始终使用 mock 数据。

**变更**: 重写 `onMounted` 逻辑，调用真实 API + 真实取消：

```javascript
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthGuard } from '../../utils/auth-guard.js'
import { getOrders, cancelOrder } from '../../api/order.js'

// ... 保留 mockOrders 作为兜底

onMounted(async () => {
  try {
    const res = await getOrders({ page: 1, page_size: 50 })
    // API 返回 { total, items }，字段为 snake_case
    orders.value = (res.items || []).map(item => ({
      id: item.id,
      orderNo: `ORD${item.id}`,
      vehicleName: item.vehicle_name,
      vehicleBrand: item.vehicle_brand || '',
      startDate: item.start_date,
      endDate: item.end_date,
      days: item.days,
      totalAmount: item.total_price,
      status: item.status,
      statusText: item.status_label,
      createdAt: formatDateTime(item.created_at)
    }))
  } catch (err) {
    console.error('加载订单列表失败，使用 Mock 数据', err)
    orders.value = mockOrders
  } finally {
    loading.value = false
  }
})

// 真实取消
function handleCancel(id) {
  uni.showModal({
    title: '确认取消',
    content: '确定要取消此订单吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await cancelOrder(id)
        uni.showToast({ title: '订单已取消', icon: 'success' })
        // 重新加载列表
        const apiRes = await getOrders({ page: 1, page_size: 50 })
        orders.value = (apiRes.items || []).map(item => ({
          id: item.id,
          orderNo: `ORD${item.id}`,
          vehicleName: item.vehicle_name,
          vehicleBrand: item.vehicle_brand || '',
          startDate: item.start_date,
          endDate: item.end_date,
          days: item.days,
          totalAmount: item.total_price,
          status: item.status,
          statusText: item.status_label,
          createdAt: formatDateTime(item.created_at)
        }))
      } catch (err) {
        console.error('取消订单失败', err)
        uni.showToast({ title: '取消失败，请重试', icon: 'none' })
      }
    }
  })
}

// 辅助函数
function formatDateTime(dt) {
  if (!dt) return ''
  return dt.replace('T', ' ').slice(0, 16)
}
```

---

#### P1-11: orders.vue tab 值 'ongoing' 应为 'in_progress'

**文件**: `frontend-mini/src/pages/orders/orders.vue`

**问题**: `tabList` 中"进行中" tab 的 value 为 `'ongoing'`，但合同 OrderStatus 枚举值为 `'in_progress'`。

**变更**:

```javascript
const tabList = [
  { label: '全部', value: 'all' },
  { label: '待确认', value: 'pending' },
  { label: '已确认', value: 'confirmed' },
  { label: '进行中', value: 'in_progress' },  // 修复: ongoing -> in_progress
  { label: '已完成', value: 'completed' }
]
```

同时修改 `.status-ongoing` 的 CSS 类名为 `.status-in_progress`。

---

#### P0-5: booking.vue 字段映射错误

**文件**: `frontend-mini/src/pages/booking/booking.vue`

**问题**: `fetchPrice()` 中读取 `res.daily_rate`、`res.total_amount` 但 API 实际返回 `total_price` 和 `price_breakdown`（无 `daily_rate` 字段）。合同中的定价 API 返回 `vehicle_name`、`total_price`、`price_breakdown` 数组。

**变更**: 修正 `fetchPrice` 中的字段映射：

```javascript
async function fetchPrice() {
  try {
    if (vehicleId.value) {
      const res = await estimatePrice({
        vehicle_id: parseInt(vehicleId.value, 10),
        start_date: startDate.value,
        end_date: endDate.value
      })
      // API 返回: vehicle_name, days, total_price, price_breakdown[]
      const dailyRate = res.price_breakdown && res.price_breakdown.length > 0
        ? res.price_breakdown[0].price  // MVP 阶段每天价格相同
        : (res.total_price / res.days)
      
      priceData.value = {
        vehicleId: res.vehicle_id || vehicleId.value,
        vehicleName: res.vehicle_name || '未知车辆',
        days: res.days || days.value,
        startDate: res.start_date || startDate.value,
        endDate: res.end_date || endDate.value,
        dailyRate: dailyRate,
        totalAmount: res.total_price || 0,
        discount: 0,
        finalAmount: res.total_price || 0
      }
    } else {
      throw new Error('no vehicleId')
    }
  } catch (err) {
    console.warn('API 获取价格失败，使用 Mock 数据', err)
    // 保持现有 mock fallback 逻辑
    priceData.value = {
      ...mockPrice,
      vehicleId: vehicleId.value || mockPrice.vehicleId,
      days: days.value || mockPrice.days,
      startDate: startDate.value || mockPrice.startDate,
      endDate: endDate.value || mockPrice.endDate,
      totalAmount: (days.value || mockPrice.days) * mockPrice.dailyRate,
      finalAmount: (days.value || mockPrice.days) * mockPrice.dailyRate
    }
  }
}
```

---

#### P1-13: booking.vue orderId 读取修复

**文件**: `frontend-mini/src/pages/booking/booking.vue`，`onSubmit` 方法

**问题**: `const orderId = res?.id || res?.orderId || 1`，Jackson snake_case 生效后应为 `res.order_id`。

**变更**:

```javascript
// 在 onSubmit 成功后
const orderId = res?.order_id || res?.id || 1
```

---

#### P0-6: order-detail.vue 使用 camelCase 字段名

**文件**: `frontend-mini/src/pages/order-detail/order-detail.vue`

**问题**: 模板中大量使用 `order.orderNo`、`order.statusText`、`order.vehicleName`、`order.dailyRate` 等 camelCase 字段，但 API 返回 snake_case。

**变更**: 在 `fetchDetail` 中添加字段映射层，将 API 返回的 snake_case 数据转换为前端模板使用的格式：

```javascript
async function fetchDetail() {
  loading.value = true
  try {
    if (orderId.value) {
      const res = await getOrderDetail(orderId.value)
      // 将 API 返回的 snake_case 数据映射为前端模板格式
      order.value = mapOrderDetail(res)
      updateStepIndex()
    } else {
      throw new Error('no orderId')
    }
  } catch (err) {
    console.warn('API 获取订单详情失败，尝试本地数据', err)
    // 保持现有 fallback 逻辑
    const lastOrderStr = uni.getStorageSync('lastOrder')
    if (lastOrderStr && orderId.value && orderId.value.startsWith('mock_order_')) {
      const parsed = JSON.parse(lastOrderStr)
      parsed.priceBreakdown = parsed.priceBreakdown || {
        dailyRate: parsed.totalAmount / parsed.days,
        days: parsed.days,
        subtotal: parsed.totalAmount,
        discount: parsed.discount,
        total: parsed.finalAmount
      }
      order.value = parsed
    } else {
      order.value = mockOrder
    }
    updateStepIndex()
  } finally {
    loading.value = false
  }
}

// 新增：API snake_case -> 前端 camelCase 映射
function mapOrderDetail(api) {
  const pickupAddr = api.pickup_address || {}
  const priceBreakdown = api.price_breakdown || []
  const totalFromBreakdown = priceBreakdown.reduce((sum, item) => sum + (item.price || 0), 0)
  
  return {
    id: api.id,
    orderNo: `ORD${api.id}`,
    vehicleName: api.vehicle?.name || '',
    vehicleBrand: '',  // 后端不返回 brand，可设为空
    startDate: api.start_date,
    endDate: api.end_date,
    days: api.days,
    dailyRate: priceBreakdown.length > 0 ? priceBreakdown[0].price : (api.total_price / api.days),
    totalAmount: api.total_price,
    discount: 0,
    finalAmount: api.total_price,
    status: api.status,
    statusText: api.status_label,
    createdAt: api.created_at ? api.created_at.replace('T', ' ') : '',
    pickupAddress: typeof pickupAddr === 'string' ? pickupAddr : pickupAddr.address || '',
    paymentStatus: api.payment_status,
    rejectReason: api.reject_reason,
    priceBreakdown: {
      dailyRate: priceBreakdown.length > 0 ? priceBreakdown[0].price : 0,
      days: api.days,
      subtotal: totalFromBreakdown,
      discount: 0,
      total: api.total_price
    }
  }
}
```

**注意**: 模板中 `.status-in_progress` 的 CSS 类名与 snake_case `in_progress` 匹配，无需修改。

---

#### P0-7: index.vue 发送错误参数

**文件**: `frontend-mini/src/pages/index/index.vue`

**问题**: `loadVehicles` 中发送 `pickupDate` 和 `returnDate` 参数给 `/api/v1/vehicles`，但合同定义该端点只接受 `min_price`、`max_price`、`page`、`page_size`。同时 `useMock` 默认为 `true`，从不走真实 API。

**变更**:

```javascript
async function loadVehicles(isRefresh = false, isLoadMore = false) {
  if (isLoadMore) loadingMore.value = true
  if (isRefresh) refreshing.value = true
  if (!isRefresh && !isLoadMore) loading.value = true

  try {
    // 关闭 mock，使用真实 API
    const res = await getVehicleList({
      page: isLoadMore ? currentPage + 1 : 1,
      page_size: PAGE_SIZE
    })
    
    if (isLoadMore) {
      vehicles.value = [...vehicles.value, ...(res.items || [])]
    } else {
      vehicles.value = res.items || []
    }
    
    // 修复: 检查 items.length < page_size 判断是否有更多
    noMore.value = (res.items || []).length < PAGE_SIZE
    if (isLoadMore && res.items) currentPage.value++
  } catch (err) {
    console.error('加载车辆列表失败，使用Mock数据', err)
    useMock.value = true
    vehicles.value = [...MOCK_VEHICLES]
    noMore.value = true
  } finally {
    loading.value = false
    refreshing.value = false
    loadingMore.value = false
  }
}
```

需要新增 `const PAGE_SIZE = 20` 和 `const currentPage = ref(1)`。

同时需要更新 `getVehicleList` API 调用中的参数名，确保使用正确的 query params：

`/api/v1/vehicles` 只接受 `min_price`、`max_price`、`page`、`page_size`，不应发送 `pickupDate` / `returnDate`。

---

#### P1-12: index.vue hasMore 判断错误

**文件**: `frontend-mini/src/pages/index/index.vue`

**问题**: `noMore.value = !res.hasMore`，但 API 返回 `{ total, items }`，没有 `hasMore` 字段。

**变更**: 已在 P0-7 中一并修复。判断逻辑改为：`(res.items || []).length < page_size` 即无更多数据。

---

#### P2-21: vehicle-detail.vue transmission mock 值

**文件**: `frontend-mini/src/pages/vehicle-detail/vehicle-detail.vue`

**问题**: mock 数据中 `transmission: '自动'`。合同 api-spec.yaml 中 `transmission` 的 enum 定义为 `["自动", "手动"]`（中文值），但 api.md 中写的是 `auto` / `manual`。

**决策**: 查看 api-spec.yaml，后端存储的是中文值（`"自动"` / `"手动"`），因此 mock 数据使用 `'自动'` 是**正确的**。但如果后端改为存储英文值，则需要同步修改。

**变更**: 此问题确认为文档不一致（见 P2-18 修复），代码本身无需变更。建议在 api-spec.yaml 和 api.md 中统一 transmission 的枚举值定义。MVP 阶段保持中文值不变。

---

#### P2-22: agreement.vue 调用真实 API

**文件**: `frontend-mini/src/pages/agreement/agreement.vue`

**问题**: 协议内容硬编码，未调用 `GET /api/v1/agreement`。

**变更**:

```javascript
import { ref } from 'vue'
import { getAgreement } from '../../api/agreement.js'

const agreementContent = ref('')
const agreementVersion = ref('')
const updateTime = ref('')

onMounted(async () => {
  try {
    const res = await getAgreement()
    agreementContent.value = res.content
    agreementVersion.value = res.version
    updateTime.value = res.updated_at
  } catch (err) {
    console.warn('获取协议失败，使用硬编码内容', err)
    // 保留现有的硬编码内容作为兜底
  }
})
```

模板中 `<view class="update-time">` 改为动态显示 `updateTime`，内容区域改为展示 `agreementContent`。如果 API 返回 HTML 内容，需要使用 `<rich-text>` 组件：

```html
<rich-text :nodes="agreementContent"></rich-text>
```

---

#### P1-16: refreshToken 发送 body 但合同定义无 body

**文件**: `frontend-mini/src/api/auth.js`，`refreshToken` 函数

**问题**: 当前发送 `{ refreshToken }` 作为 body，但合同 `POST /auth/refresh` 定义为无请求体，Token 从 Authorization header 读取。

**变更**:

```javascript
export function refreshToken() {
  return request({
    url: '/api/v1/auth/refresh',
    method: 'POST'
    // 不发送 body，token 由 request.js 自动从 Authorization header 读取
  })
}

---

### Phase 3: 合同文档对齐

#### P2-17: api.md admin-login 请求体不一致

**文件**: `docs/arch/api.md`，1.2 PC 管理端登录

**问题**: api.md 说 admin-login 需要 `phone` + `password`，但 api-spec.yaml 和代码实现都只需要 `password`。

**变更**: 修改 api.md 中 1.2 的请求体表格，移除 `phone` 字段：

```markdown
**请求体**:

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `password` | string | 是 | 非空 | 登录密码（固定密码，SHA-256 校验） |
```

同时更新请求示例：

```json
{
  "password": "admin123"
}
```

---

#### P2-18: total_price 类型不一致

**文件**: `docs/contracts/api-spec.yaml`

**问题**: api-spec.yaml 中 `total_price` 在不同 schema 中类型不一致 — `CreateOrderResult` 中定义为 `string` (format: decimal)，但在 api.md 的响应示例中是 `number`。

**决策**: Java 后端使用 `BigDecimal`，Jackson 序列化 BigDecimal 默认输出为 JSON number（如 `1196.00`）。

**变更**: 在 api-spec.yaml 中将所有 `total_price` 的类型从 `string` + `format: decimal` 改为 `number` + `format: double`：

```yaml
total_price:
  type: number
  format: double
  description: 总价（元）
```

注意：如果后端配置了 `WRITE_BIGDECIMAL_AS_PLAIN` 或其他序列化行为导致输出为 string，则需额外配置。当前默认行为输出为 number。

---

#### P2-19: 拒绝原因长度验证

**文件**: `docs/arch/api.md` + 后端代码

**问题**: api.md 中 reject reason 缺少长度验证说明。

**状态**: 数据库 VARCHAR(500) 已覆盖。仅需在 api.md 中添加 `最大 500 字` 的验证说明：

```markdown
| `reason` | string | 否 | 最大 500 字 | 拒绝原因（建议填写，用于通知用户） |
```

后端建议在 `AdminOrderController.reject()` 中添加 `@Size(max = 500)` 校验。

---

#### P2-20: status_steps 结构不一致

**问题**: 代码中 `buildStatusSteps` 返回 `[{ label, completed, current }]`，api-spec.yaml 中定义为 `[{ label, completed }]`（无 `current` 字段），而 api.md 中定义为 `[{ status, label, active }]`。三者结构均不同。

**决策**: 以**代码实际实现**为准，更新 api-spec.yaml 和 api.md：

代码实际输出：
```json
{
  "status_steps": [
    { "label": "待确认", "completed": true, "current": true },
    { "label": "已确认", "completed": false, "current": false },
    { "label": "进行中", "completed": false, "current": false },
    { "label": "已完成", "completed": false, "current": false }
  ]
}
```

**变更**:

1. api-spec.yaml 中 `OrderDetail.status_steps` 添加 `current` 字段：
```yaml
status_steps:
  type: array
  items:
    type: object
    properties:
      label:
        type: string
        description: 步骤名称
      completed:
        type: boolean
        description: 是否已完成
      current:
        type: boolean
        description: 是否为当前步骤
```

2. api.md 中更新响应示例以匹配实际输出。

---

## 4. 风险评估

### 4.1 Jackson snake_case 变更风险

**风险等级**: 高（破坏性变更）

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 前端已有代码依赖 camelCase 字段 | 字段读取为 undefined | 本文档 Phase 2 已覆盖所有前端页面的字段映射修复 |
| 第三方工具/测试依赖旧字段名 | 测试失败 | 同步更新测试用例中的字段名 |
| 其他未知消费者受影响 | 数据解析错误 | 当前项目仅有小程序前端消费，已全覆盖 |

**缓解措施**:
1. Phase 0（Jackson 配置）和 Phase 2（前端字段修复）**必须同批次提交**
2. 部署前在本地完整运行前后端联调验证
3. 保留 mock fallback 作为安全网

### 4.2 Dashboard today_revenue 修复风险

**风险等级**: 中

如果选择方案 B（添加 `completed_at` 字段），需要 Flyway 迁移脚本。在已有数据的数据库上执行 ALTER TABLE 是安全的（新增 nullable 列）。

### 4.3 orders.vue 从 mock 切换为真实 API

**风险等级**: 中

- 如果后端服务不可用，catch 块会 fallback 到 mock 数据
- 需要确保后端 `/api/v1/orders` 端点正常工作
- 取消操作从前端局部状态修改改为调用真实 API

---

## 5. 测试策略

### 5.1 后端测试

| 测试项 | 方法 | 验证点 |
|--------|------|--------|
| Jackson snake_case | 启动应用，curl 任意 GET 端点 | 所有 JSON 字段为 snake_case |
| `/auth/refresh` | POST 携带有效 token | 返回 `{ "code": 0, "data": { "token": "..." } }` |
| CreateOrderResult 字段 | POST `/api/v1/orders` 创建订单 | 响应包含 `vehicle_name`、`price_breakdown`、`status` |
| Admin vehicles status 筛选 | GET `/api/v1/admin/vehicles?status=active` | 仅返回 active 状态车辆 |
| Dashboard pending_orders | GET `/api/v1/admin/dashboard/overview` | 返回 `pending_orders` 字段 |
| Dashboard today_revenue | 完成一个订单后查询 | 收入按 completed_at 统计 |
| Subscription record | POST `/api/v1/subscription/record` | 返回 `{ "status": "accepted" }` |
| 订单详情 status_steps | GET `/api/v1/orders/{id}` | `status_steps` 数组包含 `label`、`completed`、`current` |

### 5.2 前端测试

| 测试项 | 页面 | 验证点 |
|--------|------|--------|
| 订单列表真实加载 | orders.vue | 显示后端订单数据，非 mock |
| 订单 tab 筛选 | orders.vue | "进行中" tab 使用 `in_progress` 值 |
| 订单取消 | orders.vue | 调用真实 API，列表刷新 |
| 预订价格展示 | booking.vue | 正确显示 `total_price` 和 `price_breakdown` |
| 提交订单跳转 | booking.vue | 跳转后使用 `order_id` 正确进入详情页 |
| 订单详情字段 | order-detail.vue | 所有字段正确展示（状态、价格、日期等） |
| 车辆列表加载 | index.vue | 不发送 `pickupDate`/`returnDate`，正确判断分页 |
| 协议页面 | agreement.vue | 从 API 加载协议内容 |

### 5.3 集成测试

1. 完整流程：首页选车 -> 详情 -> 预订 -> 提交 -> 查看订单列表 -> 查看订单详情
2. 管理端流程：登录 -> 仪表盘（验证 pending_orders）-> 车辆列表（验证 status 筛选）
3. Token 刷新流程：模拟 token 过期 -> 调用 refresh -> 获取新 token

### 5.4 回归检查清单

- [ ] 所有后端 API 响应字段为 snake_case
- [ ] 前端页面无 `undefined` 字段展示
- [ ] 车辆列表分页正常工作
- [ ] 订单状态流转正确（pending -> confirmed -> in_progress -> completed）
- [ ] 管理端仪表盘所有 7 个字段均有值
- [ ] Mock fallback 在 API 不可用时仍然生效

---

## 6. 执行顺序与依赖关系

```
Phase 0: Jackson snake_case (P0-1)
  │
  ├── 必须与 Phase 2 同批次提交 ──┐
  │                                │
  ▼                                ▼
Phase 1: 后端修复               Phase 2: 前端修复
  │                                │
  ├─ P0-2: /auth/refresh        ├─ P0-4: orders.vue API 调用
  ├─ P0-3: CreateOrderResult    ├─ P0-5: booking.vue 字段映射
  ├─ P1-8: AdminVehicle filter  ├─ P0-6: order-detail.vue 字段
  ├─ P1-9: Dashboard pending    ├─ P0-7: index.vue 参数修复
  ├─ P1-10: Dashboard revenue   ├─ P1-11: tab 值修正
  ├─ P1-14: Subscription record ├─ P1-12: hasMore 判断
  │                             ├─ P1-13: order_id 读取
  │                             ├─ P1-16: refreshToken 无 body
  │                             ├─ P2-21: transmission mock
  │                             └─ P2-22: agreement API
  │                                │
  ▼                                ▼
Phase 3: 合同文档对齐 (P2-17 ~ P2-20)
  （可独立执行，不依赖代码变更）
```

### 推荐的提交批次

| 批次 | 包含内容 | 说明 |
|------|---------|------|
| **Batch 1** | Phase 0 + Phase 1 + Phase 2 | 后端配置 + 后端修复 + 前端修复，**原子提交** |
| **Batch 2** | P1-10 的 Flyway 迁移（如选择方案 B） | 数据库结构变更，可单独部署 |
| **Batch 3** | Phase 3 | 纯文档更新，随时可合并 |

### 执行步骤

1. **创建 feature 分支**: `git checkout -b fix/contract-compliance`
2. **Batch 1**:
   - 修改 `application.yml` 添加 snake_case
   - 修改所有后端控制器（P0-2, P0-3, P1-8, P1-9, P1-10, P1-14）
   - 修改所有前端页面和 API 文件（P0-4 ~ P0-7, P1-11 ~ P1-13, P1-16, P2-21, P2-22）
   - 本地运行后端 + 前端，验证完整流程
   - 提交并推送到远程
3. **Batch 2**（可选）: 创建 Flyway 迁移 + 修改 DashboardController
4. **Batch 3**: 修改 api.md 和 api-spec.yaml

---

## 7. 附录

### 7.1 问题编号索引

| 编号 | 标题 | 优先级 | 状态 |
|------|------|--------|------|
| P0-1 | Jackson snake_case 全局配置 | P0 | 待修复 |
| P0-2 | /auth/refresh 端点缺失 | P0 | 待修复 |
| P0-3 | CreateOrderResult 缺少字段 | P0 | 待修复 |
| P0-4 | orders.vue 未调用真实 API | P0 | 待修复 |
| P0-5 | booking.vue 字段映射错误 | P0 | 待修复 |
| P0-6 | order-detail.vue 使用 camelCase | P0 | 待修复 |
| P0-7 | index.vue 发送错误参数 | P0 | 待修复 |
| P1-8 | AdminVehicle status 筛选未生效 | P1 | 待修复 |
| P1-9 | Dashboard 缺少 pending_orders | P1 | 待修复 |
| P1-10 | Dashboard today_revenue 过滤错误 | P1 | 待修复 |
| P1-11 | orders.vue tab 值错误 | P1 | 待修复 |
| P1-12 | index.vue hasMore 判断错误 | P1 | 待修复 |
| P1-13 | booking.vue orderId 读取错误 | P1 | 待修复 |
| P1-14 | SubscriptionController 返回 null | P1 | 待修复 |
| P1-16 | refreshToken 发送多余 body | P1 | 待修复 |
| P2-17 | api.md admin-login 不一致 | P2 | 待修复 |
| P2-18 | total_price 类型不一致 | P2 | 待修复 |
| P2-19 | 拒绝原因缺少长度验证 | P2 | 待修复 |
| P2-20 | status_steps 结构不一致 | P2 | 待修复 |
| P2-21 | vehicle-detail transmission mock | P2 | 待确认 |
| P2-22 | agreement.vue 硬编码内容 | P2 | 待修复 |

### 7.2 合同字段完整参考

所有后端响应字段应以 api.md 和 api-spec.yaml 为准，统一使用 snake_case。详见本文档第 1.3 节字段名称映射表。
