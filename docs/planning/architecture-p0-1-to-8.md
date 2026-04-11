# Sprint 1 P0-1 到 P0-8 架构设计方案

> 生成日期: 2026-04-11
> 目标: 小程序用户端核心流程跑通（浏览 -> 选车 -> 下单 -> 查看订单）
> 状态: 待评审

---

## 1. 当前代码状态分析

### 1.1 后端代码清单与状态

| 文件 | 类型 | 状态 | 说明 |
|------|------|------|------|
| `controller/VehicleController.java` | Controller | **半完成** | 列表+详情已实现，但缺少 coverImage 序列化，Vehicle 的 images 为 null 时 coverImage=null |
| `controller/OrderController.java` | Controller | **空壳** | 创建订单缺少后端算价、缺少 agreed 校验、缺少 vehicleName 返回；列表缺少车辆信息组合；详情缺少车辆信息组合；DTO 不完整 |
| `controller/AuthController.java` | Controller | **完成** | wx-login + admin-login 已实现 |
| `controller/AdminOrderController.java` | Controller | **空壳** | 缺少车辆信息组合 |
| `controller/AdminVehicleController.java` | Controller | **完成** | CRUD 已实现 |
| `controller/DashboardController.java` | Controller | **完成** | 概览已实现 |
| `controller/AgreementController.java` | Controller | **完成** | 协议读取已实现 |
| `controller/SubscriptionController.java` | Controller | **完成** | 记录授权已实现 |
| `controller/PricingController.java` | Controller | **不存在** | 需要新建 |
| `application/auth/AuthService.java` | Service | **完成** | 微信登录+管理员登录已实现 |
| `application/order/OrderAppService.java` | Service | **不存在** | 需要新建 |
| `application/vehicle/VehicleAppService.java` | Service | **不存在** | 需要新建 |
| `application/pricing/PricingAppService.java` | Service | **不存在** | 需要新建 |
| `domain/vehicle/Vehicle.java` | Domain | **完成** | 含 getCoverImage() 方法 |
| `domain/vehicle/VehicleRepository.java` | Interface | **完成** | 基础 CRUD 已定义 |
| `domain/order/Order.java` | Domain | **完成** | 含状态机方法 |
| `domain/order/OrderRepository.java` | Interface | **完成** | 含 hasConflict 方法 |
| `domain/order/OrderStatus.java` | Enum | **完成** | 6 种状态 + getLabel() |
| `domain/order/PriceBreakdown.java` | VO | **完成** | date/type/price |
| `domain/order/service/OrderConflictChecker.java` | Service | **完成** | 冲突检测已实现 |
| `domain/pricing/PricingEngine.java` | Interface | **完成** | calculate 方法已定义 |
| `domain/pricing/PricingResult.java` | VO | **完成** | dayPrices + totalPrice |
| `infrastructure/persistence/repository/VehicleRepositoryImpl.java` | Impl | **完成** | DO <-> Domain 转换完整 |
| `infrastructure/persistence/repository/OrderRepositoryImpl.java` | Impl | **半完成** | toDomain() 缺少 priceBreakdown 转换 |
| `infrastructure/persistence/repository/UserRepositoryImpl.java` | Impl | **完成** | 完整 |
| `infrastructure/persistence/dataobject/VehicleDO.java` | DO | **完成** | JacksonTypeHandler for images/tags |
| `infrastructure/persistence/dataobject/OrderDO.java` | DO | **半完成** | priceBreakdown 类型为 `List<Object>` |
| `infrastructure/persistence/mapper/VehicleMapper.java` | Mapper | **完成** | selectActiveVehicles + countActiveVehicles |
| `infrastructure/persistence/mapper/OrderMapper.java` | Mapper | **完成** | selectByUserId + countConflicts |
| `infrastructure/pricing/SimplePricingEngine.java` | Impl | **不存在** | 需要新建 |
| `common/security/JwtInterceptor.java` | Interceptor | **完成** | token 验证 + userId/role 注入 request |
| `common/security/RoleInterceptor.java` | Interceptor | **完成** | admin 路径拦截 |
| `common/security/JwtUtil.java` | Util | **完成** | generateToken/parseToken |
| `common/result/ApiResponse.java` | Wrapper | **完成** | 统一响应格式 |
| `common/result/ErrorCode.java` | Enum | **完成** | 含 5200/5300 错误码 |
| `common/config/WebMvcConfig.java` | Config | **完成** | 拦截器注册 |

### 1.2 前端代码状态

| 文件 | 状态 | 说明 |
|------|------|------|
| `pages.json` | **半完成** | 缺少 profile 页面注册、缺少 "我的" Tab、页面注册不全 |
| `App.vue` | **空壳** | 只有 console.log，缺少登录态拦截 |
| `api/request.js` | **完成** | 基本请求封装，含 token 注入 |
| `api/vehicle.js` | **完成** | getVehicleList + getVehicleDetail |
| `store/user.js` | **半完成** | 有 login/logout，但依赖 wx.login |
| `pages/index/index.vue` | **半完成** | 基础列表渲染，无取车地址、无空状态 |
| `pages/login/login.vue` | **半完成** | 依赖微信授权，需要 mock 登录入口 |

### 1.3 数据库状态

| 资源 | 状态 | 说明 |
|------|------|------|
| `V1__init_tables.sql` | 完成 | 7 张表已创建 |
| `V2__seed_demo_data.sql` | **P0-1** | 3 用户 + 5 车辆 + 1 协议，但 vehicles.images 全是空数组 `[]` |

### 1.4 关键发现

1. **Controller 层直接暴露 Domain 对象**：`VehicleController.detail()` 返回 `ApiResponse<Vehicle>`，`OrderController.detail()` 返回 `ApiResponse<Order>`。这与架构文档的四层架构设计（需要 DTO/VO）不一致。
2. **缺少 Application 层**：所有 Controller 直接调用 Repository 和 Domain，中间没有 Application Service 层。这导致跨领域编排（如订单创建需要 Pricing + Vehicle + Order 三方协作）在 Controller 中耦合。
3. **无 PricingController 和 SimplePricingEngine 实现**：PricingEngine 接口已定义，但无实现类，也无对应的 Controller。
4. **OrderDO.priceBreakdown 类型为 `List<Object>`**：MyBatis Plus 的 JacksonTypeHandler 可以序列化/反序列化，但反序列化后丢失类型信息，需要处理。

---

## 2. P0-2: 完善车辆列表 API（返回 coverImage）

### 2.1 现状分析

`VehicleController.list()` 已调用 `toListDTO()` 映射到 `VehicleListItemDTO`，其中包含 `coverImage` 字段。`Vehicle.getCoverImage()` 已实现（返回 images[0] 或 null）。

**问题**：种子数据中 vehicles.images 全为 `[]`，所以 coverImage 将始终为 null。

### 2.2 实施方案

**无需新建文件**，仅需修改种子数据。

#### 步骤 1: 更新种子数据（P0-1 连带修复）

修改 `V2__seed_demo_data.sql`，将车辆的 images 字段从 `'[]'` 改为实际 base64 数组。

MVP 阶段使用简单的占位 base64 图片（1x1 像素或小型 placeholder），每辆车至少一张图片。格式：
```json
["data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ..."]
```

> **注意**：实际开发时可以使用在线工具将小型占位图转换为 base64 字符串。MVP 阶段可以用纯色方块图片。

#### 步骤 2: 验证 VehicleController 无需代码修改

当前 `VehicleController.toListDTO()` 已正确调用 `vehicle.getCoverImage()`，只要数据中有 images，即可正确返回。

**API 验证**:
```json
GET /api/v1/vehicles

{
  "code": 0,
  "data": {
    "total": 5,
    "items": [
      {
        "id": 1,
        "name": "丰田卡罗拉 2024 款",
        "brand": "丰田",
        "seats": 5,
        "transmission": "自动",
        "cover_image": "data:image/jpeg;base64,...",  // images[0]
        "weekday_price": 150.00,
        "weekend_price": 180.00
      }
    ]
  },
  "message": "success"
}
```

### 2.3 涉及文件

- `/Users/jiangwei/Documents/carRental/backend/src/main/resources/db/migration/V2__seed_demo_data.sql`（修改 images 数据）

---

## 3. P0-3: 完善车辆详情 API（返回 images 数组）

### 3.1 现状分析

`VehicleController.detail()` 直接返回 `ApiResponse<Vehicle>`，将 Domain 对象暴露给 API 层。这有两个问题：

1. Domain 对象的字段与 API 契约不完全匹配（缺少 `tags` 的序列化控制、暴露了 `deletedAt`/`createdAt` 等内部字段）
2. 违反了四层架构原则（应通过 Application 层 + VO 转换）

但考虑到 MVP 阶段和代码现状，**采用渐进式改造**：

### 3.2 实施方案

#### 方案：在 Controller 中添加 VehicleDetailVO，修改 detail 方法

**新建文件**：无
**修改文件**：`VehicleController.java`

在 `VehicleController` 内部新增静态 VO 类：

```java
@Data
public static class VehicleDetailVO {
    private Long id;
    private String name;
    private String brand;
    private Integer seats;
    private String transmission;
    private String description;
    private List<String> images;       // base64 数组
    private BigDecimal weekdayPrice;
    private BigDecimal weekendPrice;
    private BigDecimal holidayPrice;
    private List<String> tags;
}
```

修改 `detail()` 方法，从返回 `ApiResponse<Vehicle>` 改为 `ApiResponse<VehicleDetailVO>`，手动映射字段。

**API 验证**（与 api.md 契约对齐）:
```json
GET /api/v1/vehicles/1

{
  "code": 0,
  "data": {
    "id": 1,
    "name": "丰田卡罗拉 2024 款",
    "brand": "丰田",
    "seats": 5,
    "transmission": "自动",
    "description": "经济实用，省油...",
    "images": ["data:image/jpeg;base64,...", ...],
    "weekday_price": 150.00,
    "weekend_price": 180.00,
    "holiday_price": null,
    "tags": ["经济型", "省油", "热门"]
  },
  "message": "success"
}
```

#### 关键设计决策

- **MVP 阶段直接在 Controller 中定义 VO**，不引入 Application 层转换。等 Sprint 2 管理端功能开发时，再统一抽取到 `application/vehicle/VehicleAppService.java` 中。
- **VehicleDO.images 已通过 JacksonTypeHandler 处理**，从数据库读取后自动转为 `List<String>`，无需额外转换。

### 3.3 涉及文件

- `/Users/jiangwei/Documents/carRental/backend/src/main/java/com/carrental/controller/VehicleController.java`（修改）

---

## 4. P0-4: 实现 Pricing Estimate API

### 4.1 现状分析

- `PricingEngine` 接口已定义（`calculate` 方法签名完整）
- `PricingResult` VO 已定义（含 `DayPrice` 内部类）
- **无实现类**：需要创建 `SimplePricingEngine`
- **无 Controller**：需要创建 `PricingController`

### 4.2 实施方案

#### 4.2.1 新建 `SimplePricingEngine` 实现类

**文件**: `infrastructure/pricing/SimplePricingEngine.java`

```java
package com.carrental.infrastructure.pricing;

import com.carrental.domain.pricing.PricingEngine;
import com.carrental.domain.pricing.PricingResult;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class SimplePricingEngine implements PricingEngine {

    @Override
    public PricingResult calculate(BigDecimal weekdayPrice, BigDecimal weekendPrice,
                                   BigDecimal holidayPrice, LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        List<PricingResult.DayPrice> dayPrices = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            // MVP: 所有日期均使用 weekday_price
            BigDecimal price = weekdayPrice;
            totalPrice = totalPrice.add(price);
            dayPrices.add(new PricingResult.DayPrice(
                date.toString(), "weekday", price));
        }

        return new PricingResult(dayPrices, totalPrice);
    }
}
```

#### 4.2.2 新建 `PricingController`

**文件**: `controller/PricingController.java`

```java
@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final VehicleRepository vehicleRepository;
    private final PricingEngine pricingEngine;

    @PostMapping("/estimate")
    public ApiResponse<PricingEstimateVO> estimate(@Valid @RequestBody EstimateRequest request) {
        // 1. 校验车辆存在且 active
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
            .filter(Vehicle::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "车辆不存在或已下架"));

        // 2. 调用定价引擎
        PricingResult result = pricingEngine.calculate(
            vehicle.getWeekdayPrice(),
            vehicle.getWeekendPrice(),
            vehicle.getHolidayPrice(),
            request.getStartDate(),
            request.getEndDate());

        // 3. 组装 VO
        PricingEstimateVO vo = new PricingEstimateVO();
        vo.setVehicleId(vehicle.getId());
        vo.setVehicleName(vehicle.getName());
        vo.setStartDate(request.getStartDate());
        vo.setEndDate(request.getEndDate());
        vo.setDays((int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()));
        vo.setTotalPrice(result.getTotalPrice());
        vo.setPriceBreakdown(result.getDayPrices().stream()
            .map(dp -> new PriceBreakdownItem(dp.getDate(), dp.getType(), dp.getPrice()))
            .collect(Collectors.toList()));

        return ApiResponse.success(vo);
    }

    // --- 内部类 ---
    @Data
    public static class EstimateRequest {
        @NotNull private Long vehicleId;
        @NotNull private LocalDate startDate;
        @NotNull private LocalDate endDate;
    }

    @Data
    public static class PricingEstimateVO {
        private Long vehicleId;
        private String vehicleName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer days;
        private BigDecimal totalPrice;
        private List<PriceBreakdownItem> priceBreakdown;
    }

    @Data
    @AllArgsConstructor
    public static class PriceBreakdownItem {
        private String date;
        private String type;
        private BigDecimal price;
    }
}
```

#### 4.2.3 WebMvcConfig 排除路径

`PricingController` 的路径 `/api/v1/pricing/**` 需要在 `WebMvcConfig` 中添加到 `excludePathPatterns`（因为价格估算不需要登录，用户可在未登录状态下预览价格）。

修改 `WebMvcConfig.java`：
```java
.excludePathPatterns(
    "/api/v1/auth/**",
    "/api/v1/vehicles/**",
    "/api/v1/agreement",
    "/api/v1/pricing/**"     // 新增
);
```

### 4.3 涉及文件（新建）

- `/Users/jiangwei/Documents/carRental/backend/src/main/java/com/carrental/infrastructure/pricing/SimplePricingEngine.java`
- `/Users/jiangwei/Documents/carRental/backend/src/main/java/com/carrental/controller/PricingController.java`

### 4.4 涉及文件（修改）

- `/Users/jiangwei/Documents/carRental/backend/src/main/java/com/carrental/common/config/WebMvcConfig.java`

---

## 5. P0-5: 完善订单创建 API（后端算价 + 冲突检测）

### 5.1 现状分析

`OrderController.createOrder()` 当前实现：
1. 获取 userId（从 JWT）
2. 调用 `conflictChecker.checkConflict()` -- **已有**
3. 创建 Order 对象，手动设置字段
4. `order.setTotalPrice(null)` -- **BUG**：没有设置价格
5. 缺少 `agreed=true` 校验
6. 缺少车辆存在性校验
7. 缺少 `priceBreakdown` 计算
8. 返回结果缺少 `vehicleName` 和 `priceBreakdown`

### 5.2 实施方案

#### 5.2.1 修改 OrderController.createOrder

在现有 `OrderController` 中注入依赖并增强方法：

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderConflictChecker conflictChecker;
    private final VehicleRepository vehicleRepository;    // 新增注入
    private final PricingEngine pricingEngine;            // 新增注入

    @PostMapping
    public ApiResponse<CreateOrderResult> createOrder(
            @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");

        // 1. 校验 agreed=true
        if (!Boolean.TRUE.equals(request.getAgreed())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先同意用户协议");
        }

        // 2. 校验车辆存在且 active
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
            .filter(Vehicle::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "车辆不存在或已下架"));

        // 3. 检查时间冲突
        conflictChecker.checkConflict(
            request.getVehicleId(),
            request.getStartDate(),
            request.getEndDate());

        // 4. 后端算价
        PricingResult pricingResult = pricingEngine.calculate(
            vehicle.getWeekdayPrice(),
            vehicle.getWeekendPrice(),
            vehicle.getHolidayPrice(),
            request.getStartDate(),
            request.getEndDate());

        int days = (int) ChronoUnit.DAYS.between(
            request.getStartDate(), request.getEndDate());

        // 5. 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setVehicleId(request.getVehicleId());
        order.setStartDate(request.getStartDate());
        order.setEndDate(request.getEndDate());
        order.setDays(days);
        order.setTotalPrice(pricingResult.getTotalPrice());
        // 将 PricingResult.DayPrice 转为 PriceBreakdown
        order.setPriceBreakdown(pricingResult.getDayPrices().stream()
            .map(dp -> new PriceBreakdown(
                LocalDate.parse(dp.getDate()),
                dp.getType(),
                dp.getPrice()))
            .collect(Collectors.toList()));
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus("unpaid");

        order = orderRepository.save(order);

        // 6. 组装结果
        CreateOrderResult result = new CreateOrderResult();
        result.setOrderId(order.getId());
        result.setVehicleName(vehicle.getName());
        result.setStartDate(order.getStartDate());
        result.setEndDate(order.getEndDate());
        result.setDays(order.getDays());
        result.setTotalPrice(order.getTotalPrice());
        result.setPriceBreakdown(order.getPriceBreakdown().stream()
            .map(pb -> new PriceBreakdownItem(
                pb.getDate().toString(), pb.getType(), pb.getPrice()))
            .collect(Collectors.toList()));
        result.setStatus("pending");

        return ApiResponse.success(result);
    }

    // --- 内部类更新 ---
    @Data
    public static class CreateOrderRequest {
        private Long vehicleId;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean agreed;   // 新增
    }

    @Data
    public static class CreateOrderResult {
        private Long orderId;
        private String vehicleName;      // 新增
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer days;
        private BigDecimal totalPrice;
        private List<PriceBreakdownItem> priceBreakdown;  // 新增
        private String status;           // 新增
    }

    @Data
    @AllArgsConstructor
    public static class PriceBreakdownItem {
        private String date;
        private String type;
        private BigDecimal price;
    }
}
```

#### 5.2.2 OrderRepositoryImpl 修复：priceBreakdown 转换

当前 `OrderRepositoryImpl.toDomain()` 不处理 `priceBreakdown`，`toDO()` 也不处理。需要添加转换逻辑：

```java
// toDomain 中添加：
if (orderDO.getPriceBreakdown() != null) {
    order.setPriceBreakdown(orderDO.getPriceBreakdown().stream()
        .map(obj -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return new PriceBreakdown(
                LocalDate.parse((String) map.get("date")),
                (String) map.get("type"),
                new BigDecimal(map.get("price").toString()));
        })
        .collect(Collectors.toList()));
}

// toDO 中添加：
if (order.getPriceBreakdown() != null) {
    orderDO.setPriceBreakdown(order.getPriceBreakdown().stream()
        .map(pb -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", pb.getDate().toString());
            map.put("type", pb.getType());
            map.put("price", pb.getPrice());
            return map;
        })
        .collect(Collectors.toList()));
}
```

### 5.3 涉及文件（修改）

- `/Users/jiangwei/Documents/carRental/backend/src/main/java/com/carrental/controller/OrderController.java`
- `/Users/jiangwei/Documents/carRental/backend/src/main/java/com/carrental/infrastructure/persistence/repository/OrderRepositoryImpl.java`

---

## 6. P0-6: 完善订单列表/详情 API（两查询组合）

### 6.1 方案设计

按照架构文档 ADR 中定义的"两查询 + 应用层组合"方案：

```
步骤 1: SELECT * FROM orders WHERE user_id = ? LIMIT ...
步骤 2: SELECT id, name, images FROM vehicles WHERE id IN (?, ?, ...)
步骤 3: 在应用层用 Map<Long, Vehicle> 做 join
```

### 6.2 实施方案

#### 6.2.1 修改 `OrderController.myOrders()`

```java
@GetMapping
public ApiResponse<Map<String, Object>> myOrders(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        HttpServletRequest httpRequest) {

    Long userId = (Long) httpRequest.getAttribute("userId");
    List<Order> orders = orderRepository.findByUserId(userId, status, page, pageSize);
    long total = orderRepository.countByUserId(userId, status);

    // 两查询组合
    List<Long> vehicleIds = orders.stream()
        .map(Order::getVehicleId)
        .distinct()
        .collect(Collectors.toList());

    Map<Long, Vehicle> vehicleMap = new HashMap<>();
    if (!vehicleIds.isEmpty()) {
        for (Long vid : vehicleIds) {
            vehicleRepository.findById(vid).ifPresent(v -> vehicleMap.put(vid, v));
        }
    }

    Map<String, Object> result = new HashMap<>();
    result.put("total", total);
    result.put("items", orders.stream()
        .map(order -> toListDTO(order, vehicleMap.get(order.getVehicleId())))
        .collect(Collectors.toList()));

    return ApiResponse.success(result);
}

// 完善后的 OrderListItemDTO
@Data
public static class OrderListItemDTO {
    private Long id;
    private String vehicleName;       // 新增
    private String vehicleImage;      // 新增（coverImage）
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;             // 新增
    private BigDecimal totalPrice;
    private String status;
    private String statusLabel;       // 新增
    private LocalDateTime createdAt;
    private Boolean canCancel;        // 新增
}

private OrderListItemDTO toListDTO(Order order, Vehicle vehicle) {
    OrderListItemDTO dto = new OrderListItemDTO();
    dto.setId(order.getId());
    dto.setStartDate(order.getStartDate());
    dto.setEndDate(order.getEndDate());
    dto.setDays(order.getDays());
    dto.setTotalPrice(order.getTotalPrice());
    dto.setStatus(order.getStatus().name());
    dto.setStatusLabel(order.getStatus().getLabel());
    dto.setCreatedAt(order.getCreatedAt());
    dto.setCanCancel(order.getStatus() == OrderStatus.PENDING);

    if (vehicle != null) {
        dto.setVehicleName(vehicle.getName());
        dto.setVehicleImage(vehicle.getCoverImage());
    }
    return dto;
}
```

#### 6.2.2 修改 `OrderController.detail()`

```java
@GetMapping("/{id}")
public ApiResponse<OrderDetailVO> detail(
        @PathVariable Long id,
        HttpServletRequest httpRequest) {

    Long userId = (Long) httpRequest.getAttribute("userId");
    Order order = orderRepository.findById(id)
        .filter(o -> o.getUserId().equals(userId))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));

    Vehicle vehicle = vehicleRepository.findById(order.getVehicleId())
        .orElse(null);

    OrderDetailVO vo = new OrderDetailVO();
    vo.setId(order.getId());
    // 车辆信息（嵌套对象）
    if (vehicle != null) {
        OrderDetailVO.VehicleInfo vi = new OrderDetailVO.VehicleInfo();
        vi.setId(vehicle.getId());
        vi.setName(vehicle.getName());
        vi.setImages(vehicle.getImages());
        vo.setVehicle(vi);
    }
    vo.setStartDate(order.getStartDate());
    vo.setEndDate(order.getEndDate());
    vo.setDays(order.getDays());
    vo.setTotalPrice(order.getTotalPrice());
    vo.setStatus(order.getStatus().name());
    vo.setStatusLabel(order.getStatus().getLabel());
    vo.setStatusSteps(buildStatusSteps(order.getStatus()));
    vo.setPaymentStatus(order.getPaymentStatus());
    vo.setPickupAddress("北京市朝阳区XX路XX号");        // MVP 硬编码
    vo.setPickupInstructions("到达后联系管理员取车");    // MVP 硬编码
    vo.setCreatedAt(order.getCreatedAt());
    vo.setCanCancel(order.getStatus() == OrderStatus.PENDING);

    // priceBreakdown
    if (order.getPriceBreakdown() != null) {
        vo.setPriceBreakdown(order.getPriceBreakdown().stream()
            .map(pb -> new OrderDetailVO.PriceBreakdownItem(
                pb.getDate().toString(), pb.getType(), pb.getPrice()))
            .collect(Collectors.toList()));
    }

    return ApiResponse.success(vo);
}

private List<OrderDetailVO.StatusStep> buildStatusSteps(OrderStatus current) {
    List<OrderDetailVO.StatusStep> steps = new ArrayList<>();
    List<OrderStatus> flow = List.of(
        OrderStatus.PENDING, OrderStatus.CONFIRMED,
        OrderStatus.IN_PROGRESS, OrderStatus.COMPLETED);
    boolean passed = false;
    for (OrderStatus s : flow) {
        OrderDetailVO.StatusStep step = new OrderDetailVO.StatusStep();
        step.setStatus(s.name());
        step.setLabel(s.getLabel());
        step.setActive(s == current);
        steps.add(step);
    }
    return steps;
}

@Data
public static class OrderDetailVO {
    private Long id;
    private VehicleInfo vehicle;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;
    private BigDecimal totalPrice;
    private List<PriceBreakdownItem> priceBreakdown;
    private String status;
    private String statusLabel;
    private List<StatusStep> statusSteps;
    private String paymentStatus;
    private String pickupAddress;
    private String pickupInstructions;
    private LocalDateTime createdAt;
    private Boolean canCancel;

    @Data
    public static class VehicleInfo {
        private Long id;
        private String name;
        private List<String> images;
    }

    @Data
    @AllArgsConstructor
    public static class PriceBreakdownItem {
        private String date;
        private String type;
        private BigDecimal price;
    }

    @Data
    public static class StatusStep {
        private String status;
        private String label;
        private Boolean active;
    }
}
```

### 6.3 订单列表 API 验证

```json
GET /api/v1/orders

{
  "code": 0,
  "data": {
    "total": 1,
    "items": [
      {
        "id": 1,
        "vehicle_name": "丰田卡罗拉 2024 款",
        "vehicle_image": "data:image/jpeg;base64,...",
        "start_date": "2026-05-01",
        "end_date": "2026-05-05",
        "days": 4,
        "total_price": 600.00,
        "status": "pending",
        "status_label": "待确认",
        "created_at": "2026-04-11T10:00:00",
        "can_cancel": true
      }
    ]
  }
}
```

### 6.4 涉及文件（修改）

- `/Users/jiangwei/Documents/carRental/backend/src/main/java/com/carrental/controller/OrderController.java`（大幅修改）

---

## 7. P0-7: 登录简化方案（模拟登录 bypass 微信）

### 7.1 现状分析

`AuthController` 当前有两个接口：
1. `POST /api/v1/auth/wx-login` -- 依赖 `WxMaService`（需要真实微信 AppID/Secret）
2. `POST /api/v1/auth/admin-login` -- 用固定密码登录管理员

`WxConfig` 中 `appid` 和 `secret` 配置为 `placeholder`，Sprint 1 阶段无法使用真实微信登录。

### 7.2 方案设计

新增一个 `POST /api/v1/auth/mock-login` 接口，绕过微信，直接返回 JWT Token。

### 7.3 实施方案

#### 7.3.1 修改 `AuthController`

添加 mock 登录方法：

```java
/**
 * 模拟登录（Sprint 1 开发用，bypass 微信）
 * 选择 demo 用户之一直接返回 token
 */
@PostMapping("/mock-login")
public ApiResponse<LoginResult> mockLogin(@RequestBody MockLoginRequest request) {
    String openid = "wx_demo_user_" + (request.getUserIndex() != null ? request.getUserIndex() : "001");

    User user = userRepository.findByOpenid(openid).orElse(null);
    if (user == null) {
        // 首次使用，自动创建
        String phone = "1380000000" + (request.getUserIndex() != null ? request.getUserIndex() : "2");
        String nickname = "用户" + phone.substring(phone.length() - 4);
        user = new User(phone, openid, nickname);
        user = userRepository.save(user);
    }

    String token = jwtUtil.generateToken(user.getId(), user.getRole());
    return ApiResponse.success(new LoginResult(token, toUserDTO(user), false));
}

@Data
public static class MockLoginRequest {
    // 1=张三(user_001), 2=李四(user_002), null=默认张三
    private Integer userIndex;
}
```

#### 7.3.2 WebMvcConfig 无需修改

`/api/v1/auth/**` 已经在 `excludePathPatterns` 中，mock-login 自然不受 JWT 拦截。

#### 7.3.3 前端修改：store/user.js

新增 mockLogin 方法：

```javascript
async function mockLogin(userIndex) {
  const res = await request({
    url: '/api/v1/auth/mock-login',
    method: 'POST',
    data: { userIndex }
  })

  token.value = res.token
  userInfo.value = res.user
  isLoggedIn.value = true

  uni.setStorageSync('token', res.token)
  uni.setStorageSync('userInfo', res.user)

  return res
}
```

#### 7.3.4 前端修改：App.vue 登录态拦截

在 `App.vue` 的 `onLaunch` 中增加：

```javascript
onLaunch(() => {
  const token = uni.getStorageSync('token')
  if (!token) {
    // 未登录，跳转到登录页
    uni.reLaunch({ url: '/pages/login/login' })
  }
})
```

#### 7.3.5 前端修改：login.vue 增加"模拟登录"按钮

在登录页添加一个开发用按钮：

```vue
<!-- 开发用模拟登录 -->
<button class="mock-btn" @click="onMockLogin">模拟登录（开发用）</button>
```

```javascript
async function onMockLogin() {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }
  try {
    await userStore.mockLogin()
    uni.reLaunch({ url: '/pages/index/index' })
  } catch (err) {
    uni.showToast({ title: '模拟登录失败', icon: 'none' })
  }
}
```

### 7.4 Token 验证流程

```
小程序启动
  |
  v
检查 uni.getStorageSync('token')
  |
  +-- 无 token -> 跳转 /pages/login/login
  |
  +-- 有 token -> 直接请求任意需要认证的 API
         |
         +-- 401 -> 清除 token -> 跳转登录
         +-- 200 -> 正常进入
```

> **Sprint 1 不实现 token 刷新逻辑**。JWT 有效期 7 天，MVP 期间足够。`/auth/refresh` 接口留到 Sprint 3 实现。

### 7.5 涉及文件

**后端修改**:
- `/Users/jiangwei/Documents/carRental/backend/src/main/java/com/carrental/controller/AuthController.java`

**前端修改**:
- `/Users/jiangwei/Documents/carRental/frontend-mini/src/store/user.js`
- `/Users/jiangwei/Documents/carRental/frontend-mini/src/App.vue`
- `/Users/jiangwei/Documents/carRental/frontend-mini/src/pages/login/login.vue`

---

## 8. P0-8: TabBar 补全

### 8.1 现状分析

`pages.json` 当前 tabBar 只有两个 Tab：
- 首页 -> `pages/index/index`
- 订单 -> `pages/orders/orders`

**缺少**: "我的" Tab -> `/pages/profile/index`

此外，`pages.json` 中缺少以下页面注册：
- `pages/profile/index` （"我的"页面）

### 8.2 实施方案

#### 8.2.1 修改 `pages.json`

```json
{
  "pages": [
    { "path": "pages/index/index", "style": { "navigationBarTitleText": "租车" } },
    { "path": "pages/login/login", "style": { "navigationBarTitleText": "登录" } },
    { "path": "pages/vehicle-detail/vehicle-detail", "style": { "navigationBarTitleText": "车辆详情" } },
    { "path": "pages/booking/booking", "style": { "navigationBarTitleText": "预订下单" } },
    { "path": "pages/orders/orders", "style": { "navigationBarTitleText": "我的订单" } },
    { "path": "pages/order-detail/order-detail", "style": { "navigationBarTitleText": "订单详情" } },
    { "path": "pages/agreement/agreement", "style": { "navigationBarTitleText": "用户协议" } },
    { "path": "pages/profile/index", "style": { "navigationBarTitleText": "我的" } },
    { "path": "pages/admin/orders/orders", "style": { "navigationBarTitleText": "订单管理" } },
    { "path": "pages/admin/order-detail/order-detail", "style": { "navigationBarTitleText": "订单详情" } }
  ],
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#333333",
    "backgroundColor": "#FFFFFF",
    "borderStyle": "black",
    "list": [
      {
        "pagePath": "pages/index/index",
        "text": "首页",
        "iconPath": "static/tab-home.png",
        "selectedIconPath": "static/tab-home-active.png"
      },
      {
        "pagePath": "pages/orders/orders",
        "text": "订单",
        "iconPath": "static/tab-order.png",
        "selectedIconPath": "static/tab-order-active.png"
      },
      {
        "pagePath": "pages/profile/index",
        "text": "我的",
        "iconPath": "static/tab-profile.png",
        "selectedIconPath": "static/tab-profile-active.png"
      }
    ]
  }
}
```

#### 8.2.2 新建 Profile 页面骨架

**文件**: `frontend-mini/src/pages/profile/index.vue`

Sprint 1 MVP 仅实现基本骨架：

```vue
<template>
  <view class="profile-container">
    <view class="user-info">
      <text class="nickname">{{ userInfo?.nickname || '未登录' }}</text>
      <text class="phone">{{ userInfo?.phone || '' }}</text>
    </view>
    <view class="menu-list">
      <view class="menu-item" @click="goOrders">
        <text>我的订单</text>
      </view>
      <view class="menu-item" @click="goAgreement">
        <text>用户协议</text>
      </view>
      <view class="menu-item danger" @click="onLogout">
        <text>退出登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '../../store/user.js'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

function goOrders() {
  uni.switchTab({ url: '/pages/orders/orders' })
}

function goAgreement() {
  uni.navigateTo({ url: '/pages/agreement/agreement' })
}

function onLogout() {
  uni.showModal({
    title: '确认退出',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.reLaunch({ url: '/pages/login/login' })
      }
    }
  })
}
</script>
```

#### 8.2.3 TabBar 图标

MVP 阶段可使用 uni-app 自带的文字图标，或使用简单的 SVG 转 PNG 生成 3 组 Tab 图标（普通/选中），放在 `frontend-mini/src/static/` 下。

> **注意**：微信小程序要求 tabBar 图标必须是本地路径（不能是网络图片），且推荐尺寸 81x81px。MVP 阶段可以先使用单色小图标占位。

#### 8.2.4 登录态拦截

在 `App.vue` 中，当检测到无 token 时，如果当前页面不是登录页，则跳转登录：

```javascript
onLaunch(() => {
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.reLaunch({ url: '/pages/login/login' })
  }
})

// 补充：请求 401 时清除 token 并跳转
// 在 api/request.js 中添加
if (res.statusCode === 401) {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.reLaunch({ url: '/pages/login/login' })
  return
}
```

### 8.3 涉及文件

**前端修改**:
- `/Users/jiangwei/Documents/carRental/frontend-mini/src/pages.json`
- `/Users/jiangwei/Documents/carRental/frontend-mini/src/App.vue`
- `/Users/jiangwei/Documents/carRental/frontend-mini/src/api/request.js`

**前端新建**:
- `/Users/jiangwei/Documents/carRental/frontend-mini/src/pages/profile/index.vue`
- `/Users/jiangwei/Documents/carRental/frontend-mini/src/static/tab-home.png`（及 active 版本）
- `/Users/jiangwei/Documents/carRental/frontend-mini/src/static/tab-order.png`（及 active 版本）
- `/Users/jiangwei/Documents/carRental/frontend-mini/src/static/tab-profile.png`（及 active 版本）

---

## 9. 依赖关系图

```
                    任务依赖关系
                    =============

P0-1: 数据初始化 SQL
  │
  ├────> P0-2: 车辆列表 API（依赖有图片数据）
  │        │
  │        ├────> P0-9: 首页前端（依赖 coverImage 返回）
  │        │
  │        └────> P0-3: 车辆详情 API（可并行）
  │                 │
  │                 └────> P0-10: 车辆详情前端（依赖 images 数组）
  │
  ├────> P0-4: Pricing Estimate API（需要 Vehicle 存在）
  │        │
  │        └────> P0-5: 订单创建 API（复用 PricingEngine）
  │                 │
  │                 └────> P0-11: 预订下单前端
  │
  ├────> P0-7: 模拟登录（独立，可最早开始）
  │        │
  │        └────> P0-16: 登录态拦截
  │
  P0-6: 订单列表/详情（可独立开发，但前端需要 P0-7 先有登录态）
  │
  └────> P0-12/P0-13: 订单前端页面
  │
  P0-8: TabBar 补全（独立，可最早开始）
```

### 并行策略

| 并行组 | 任务 | 说明 |
|--------|------|------|
| **组 1** | P0-1 + P0-7 + P0-8 | 数据初始化、模拟登录、TabBar 无依赖，可并行 |
| **组 2** | P0-2 + P0-3 + P0-4 | 三个读接口，互不依赖，可并行 |
| **组 3** | P0-5 + P0-6 | 订单创建和列表/详情，可并行（共用 VehicleRepository 注入） |

### 推荐执行顺序

```
Day 1 上午:
  1. P0-1 种子数据更新（改 images）
  2. P0-7 模拟登录（AuthController 加接口 + 前端改）
  3. P0-8 TabBar + Profile 页面

Day 1 下午:
  4. P0-2 车辆列表 API（验证 coverImage）
  5. P0-3 车辆详情 API（加 VehicleDetailVO）
  6. P0-4 Pricing Estimate API（新建 SimplePricingEngine + PricingController）

Day 2 上午:
  7. P0-5 订单创建 API（后端算价 + 冲突检测 + agreed 校验）
  8. P0-6 订单列表/详情 API（两查询组合）

Day 2 下午:
  9. 全链路联调测试
```

---

## 10. 风险点

### 10.1 高风险

| # | 风险 | 影响 | 缓解措施 |
|---|------|------|---------|
| R1 | **种子数据 images 为空** | P0-2/P0-3 返回 coverImage=null 和 images=[]，前端渲染异常 | P0-1 必须更新 images 字段为有效 base64 字符串 |
| R2 | **OrderDO.priceBreakdown 类型擦除** | `List<Object>` 反序列化后失去类型信息，转换易出错 | 在 `OrderRepositoryImpl` 中使用 `Map<String, Object>` 安全转换，添加 try-catch |
| R3 | **Controller 直接暴露 Domain 对象** | 架构不一致，后续重构成本高 | MVP 阶段接受此不一致，在 Sprint 2 引入 Application 层时统一重构 |

### 10.2 中风险

| # | 风险 | 影响 | 缓解措施 |
|---|------|------|---------|
| R4 | **WxConfig 中 appid/secret 为 placeholder** | 微信 SDK 初始化可能报错 | `WxConfig` 使用 `@ConditionalOnProperty` 或在 placeholder 时使用 mock 配置 |
| R5 | **小程序 TabBar 图标缺失** | 小程序编译报错（tabBar.list 中 iconPath 为必填） | 使用简单的 81x81px 占位 PNG 图标 |
| R6 | **OrderRepositoryImpl.hasConflict 排除订单逻辑复杂** | 当前实现用两次查询判断排除，可能有边界问题 | MVP 阶段创建订单不涉及排除逻辑（只在编辑时用到），暂不影响 |
| R7 | **JWT secret 使用默认值** | 安全风险 | 开发环境可接受，上线前必须修改 |

### 10.3 低风险

| # | 风险 | 影响 | 缓解措施 |
|---|------|------|---------|
| R8 | **base64 图片体积大** | API 响应体积膨胀 | MVP 阶段仅 5 辆车，每车 2-3 张压缩图，可接受 |
| R9 | **价格过滤在内存中进行** | VehicleController.list() 先查全量再内存过滤 | MVP 阶段数据量小，不影响性能 |
| R10 | **未实现 Spring Validation 日期校验** | 非法日期可能传入 Service 层 | 在 `EstimateRequest` 中使用 `@DateTimeFormat` + 自定义校验 |

---

## 11. 完整文件变更清单

### 11.1 新建文件（6 个）

| 文件 | 任务 | 说明 |
|------|------|------|
| `backend/.../infrastructure/pricing/SimplePricingEngine.java` | P0-4 | MVP 定价引擎实现 |
| `backend/.../controller/PricingController.java` | P0-4 | 价格估算接口 |
| `frontend-mini/src/pages/profile/index.vue` | P0-8 | "我的"页面 |
| `frontend-mini/src/static/tab-home.png` | P0-8 | Tab 图标 |
| `frontend-mini/src/static/tab-order.png` | P0-8 | Tab 图标 |
| `frontend-mini/src/static/tab-profile.png` | P0-8 | Tab 图标 |

### 11.2 修改文件（9 个）

| 文件 | 任务 | 变更内容 |
|------|------|---------|
| `backend/.../db/migration/V2__seed_demo_data.sql` | P0-1 | vehicles.images 从 `[]` 改为 base64 数组 |
| `backend/.../controller/VehicleController.java` | P0-3 | detail() 返回 VehicleDetailVO 而非 Vehicle |
| `backend/.../controller/OrderController.java` | P0-5,P0-6 | 大幅修改：加后端算价、agreed 校验、两查询组合、完善 DTO |
| `backend/.../controller/AuthController.java` | P0-7 | 新增 mockLogin() 方法 |
| `backend/.../common/config/WebMvcConfig.java` | P0-4 | 添加 `/api/v1/pricing/**` 排除路径 |
| `backend/.../infrastructure/persistence/repository/OrderRepositoryImpl.java` | P0-5 | 添加 priceBreakdown DO <-> Domain 转换 |
| `frontend-mini/src/pages.json` | P0-8 | 添加 profile 页面注册、补充 TabBar 第三项 |
| `frontend-mini/src/App.vue` | P0-7 | 添加 onLaunch 登录态检查 |
| `frontend-mini/src/store/user.js` | P0-7 | 添加 mockLogin 方法 |
| `frontend-mini/src/api/request.js` | P0-7 | 401 响应处理（清 token + 跳登录） |
| `frontend-mini/src/pages/login/login.vue` | P0-7 | 添加模拟登录按钮 |

### 11.3 不修改文件（已满足需求）

| 文件 | 说明 |
|------|------|
| `domain/vehicle/Vehicle.java` | getCoverImage() 已实现 |
| `domain/order/Order.java` | 状态机完整 |
| `domain/order/OrderStatus.java` | 含 getLabel() |
| `domain/order/PriceBreakdown.java` | 结构完整 |
| `domain/order/service/OrderConflictChecker.java` | 冲突检测已实现 |
| `domain/pricing/PricingEngine.java` | 接口定义完整 |
| `domain/pricing/PricingResult.java` | VO 结构完整 |
| `infrastructure/.../VehicleRepositoryImpl.java` | 转换完整 |
| `common/security/JwtInterceptor.java` | 功能完整 |
| `common/security/JwtUtil.java` | 功能完整 |
| `common/result/ApiResponse.java` | 功能完整 |

---

## 12. 关键技术决策记录

### DEC-001: MVP 阶段不使用 Application Service 层

**决策**: Controller 直接注入 Repository 和 Domain Service，不创建独立的 Application Service。

**理由**:
1. 当前项目规模小（5 辆车），领域逻辑简单
2. 所有 Controller 已有基本骨架，引入 Application 层需要大量重构
3. MVP 目标是快速跑通流程，不是架构完美
4. Sprint 2 管理端功能开发时，再评估是否需要抽取 Application Service

**后果**: Controller 层耦合了用例编排和 HTTP 适配逻辑，但可通过良好的方法拆分缓解。

### DEC-002: VehicleDetailVO 定义在 Controller 内部类

**决策**: 将 VO 定义为 Controller 的静态内部类，不创建独立的 VO 文件。

**理由**:
1. 减少文件数量
2. VO 仅在一个 Controller 中使用
3. 符合 Spring Boot 常见实践

### DEC-003: 模拟登录使用固定 userIndex 选择 demo 用户

**决策**: `mock-login` 通过 `userIndex` 参数选择种子数据中的用户，而非创建新用户。

**理由**:
1. 与种子数据一致
2. 可测试不同角色（userIndex=1 可选管理员）
3. 不需要真实微信环境

### DEC-004: priceBreakdown 在 OrderDO 中保持 `List<Object>`

**决策**: 不在 OrderDO 中使用强类型的 `List<PriceBreakdown>`，继续使用 `List<Object>`。

**理由**:
1. MyBatis Plus 的 JacksonTypeHandler 将 JSON 反序列化为 `LinkedHashMap`
2. 强类型转换容易因类型擦除失败
3. 在 RepositoryImpl.toDomain() 中手动从 Map 提取字段，更安全

**v1.5 改进**: 使用自定义 TypeHandler 实现强类型序列化/反序列化。

---

*文档结束*
*作者: Claude Code 架构师*
*日期: 2026-04-11*
