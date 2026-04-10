# 租车应用 - 接口契约表

## 基本信息

| 项目 | 值 |
|------|-----|
| API 版本 | v1 |
| Base URL | `/api/v1` |
| 认证方式 | JWT Bearer Token (`Authorization: Bearer <token>`) |
| 响应格式 | `{ code, data, message }` |
| OpenAPI Spec | `docs/contracts/api-spec.yaml` |

---

## 验证清单

- [x] 所有端点已定义
- [x] 请求/响应模型已完整
- [x] 错误码已完整（F/C/W 系列）
- [x] 枚举值已完整
- [x] 数据类型已明确（Decimal 用 string）
- [x] 必填/可选字段已标注

---

## 端点清单

| 序号 | 方法 | 路径 | 认证 | 权限 | 说明 |
|------|------|------|------|------|------|
| 1 | POST | `/auth/wx-login` | 否 | 无 | 微信小程序登录 |
| 2 | POST | `/auth/admin-login` | 否 | 无 | PC 管理端登录 |
| 3 | POST | `/auth/refresh` | 是 | 无 | 刷新 Token |
| 4 | GET | `/vehicles` | 否 | 无 | 车辆列表（公开） |
| 5 | GET | `/vehicles/{id}` | 否 | 无 | 车辆详情（公开） |
| 6 | POST | `/orders` | 是 | user | 创建订单 |
| 7 | POST | `/orders/{id}/pay` | 是 | user | 发起支付（v1.5） |
| 8 | GET | `/orders` | 是 | user | 我的订单列表 |
| 9 | GET | `/orders/{id}` | 是 | user | 订单详情 |
| 10 | POST | `/orders/{id}/cancel` | 是 | user | 取消订单 |
| 11 | POST | `/subscription/record` | 是 | user | 记录订阅授权 |
| 12 | GET | `/admin/orders` | 是 | admin | 订单列表（管理端） |
| 13 | POST | `/admin/orders/{id}/confirm` | 是 | admin | 确认订单 |
| 14 | POST | `/admin/orders/{id}/reject` | 是 | admin | 拒绝订单 |
| 15 | POST | `/admin/orders/{id}/start` | 是 | admin | 标记进行中 |
| 16 | POST | `/admin/orders/{id}/complete` | 是 | admin | 标记完成 |
| 17 | GET | `/admin/vehicles` | 是 | admin | 车辆列表（含下架） |
| 18 | POST | `/admin/vehicles` | 是 | admin | 创建车辆 |
| 19 | PUT | `/admin/vehicles/{id}` | 是 | admin | 更新车辆 |
| 20 | DELETE | `/admin/vehicles/{id}` | 是 | admin | 删除车辆（软删） |
| 21 | POST | `/admin/vehicles/{id}/toggle-status` | 是 | admin | 上下架切换 |
| 22 | GET | `/admin/pricing/holidays` | 是 | admin | 节假日列表 |
| 23 | POST | `/admin/pricing/holidays` | 是 | admin | 创建节假日 |
| 24 | POST | `/admin/pricing/holidays/batch` | 是 | admin | 批量设置节假日 |
| 25 | GET | `/admin/dashboard/overview` | 是 | admin | 仪表盘概览 |
| 26 | GET | `/admin/mini/orders` | 是 | admin | 小程序管理端订单列表 |
| 27 | GET | `/agreement` | 否 | 无 | 获取当前协议 |
| 28 | PUT | `/admin/agreement` | 是 | admin | 更新协议 |

---

## 请求/响应模型

### 认证模块

#### WxLoginRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| login_code | string | 是 | wx.login() 获取的 code |
| phone_code | string | 是 | getUserPhoneNumber 获取的 code |

#### AdminLoginRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| password | string | 是 | 管理端登录密码 |

#### UserDTO

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int64 | 是 | 用户 ID |
| phone | string | 是 | 脱敏手机号 |
| nickname | string | 是 | 昵称 |
| role | string | 是 | 角色：`user` / `admin` |
| must_change_pwd | boolean | 是 | 首次登录需修改密码 |

#### LoginResult

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | string | 是 | JWT Token |
| user | UserDTO | 是 | 用户信息 |
| is_new_user | boolean | 是 | 是否首次登录 |

---

### 车辆模块（公开）

#### VehicleListItem

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int64 | 是 | 车辆 ID |
| name | string | 是 | 车型名称 |
| brand | string | 是 | 品牌 |
| seats | int | 是 | 座位数 |
| transmission | string | 是 | 变速箱：`auto` / `manual` |
| cover_image | string | 是 | 封面图（取自 images[0]） |
| weekday_price | string(decimal) | 是 | 工作日日租金 |
| weekend_price | string(decimal) | 是 | 周末日租金 |

#### VehicleDetail

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int64 | 是 | 车辆 ID |
| name | string | 是 | 车型名称 |
| brand | string | 是 | 品牌 |
| seats | int | 是 | 座位数 |
| transmission | string | 是 | 变速箱：`auto` / `manual` |
| description | string | 是 | 车辆描述 |
| images | string[] | 是 | 图片 URL 数组 |
| weekday_price | string(decimal) | 是 | 工作日日租金 |
| weekend_price | string(decimal) | 是 | 周末日租金 |
| holiday_price | string(decimal) | 否 | 节假日日租金（null 时取周末价） |
| tags | string[] | 是 | 标签数组 |

---

### 订单模块（用户端）

#### CreateOrderRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| vehicle_id | int64 | 是 | 车辆 ID |
| start_date | date | 是 | 取车日期 |
| end_date | date | 是 | 还车日期 |

#### CreateOrderResult

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| order_id | int64 | 是 | 订单 ID |
| start_date | date | 是 | 取车日期 |
| end_date | date | 是 | 还车日期 |
| days | int | 是 | 租期天数 |
| total_price | string(decimal) | 是 | 总价 |

#### PriceBreakdownItem

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| date | date | 是 | 日期 |
| type | string | 是 | 日期类型：`weekday` / `weekend` / `holiday` |
| price | string(decimal) | 是 | 当日价格 |

#### OrderListItem

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int64 | 是 | 订单 ID |
| vehicle_name | string | 是 | 车型名称 |
| vehicle_image | string | 是 | 车辆封面（取自 vehicles.images[0]） |
| start_date | date | 是 | 取车日期 |
| end_date | date | 是 | 还车日期 |
| total_price | string(decimal) | 是 | 总价 |
| status | OrderStatus | 是 | 订单状态 |
| created_at | datetime | 是 | 创建时间 |

#### OrderDetail

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int64 | 是 | 订单 ID |
| vehicle | VehicleRef | 是 | 车辆引用 |
| start_date | date | 是 | 取车日期 |
| end_date | date | 是 | 还车日期 |
| days | int | 是 | 租期天数 |
| total_price | string(decimal) | 是 | 总价 |
| status | OrderStatus | 是 | 订单状态 |
| payment_status | PaymentStatus | 是 | 支付状态 |
| price_breakdown | PriceBreakdownItem[] | 是 | 每日价格明细 |
| created_at | datetime | 是 | 创建时间 |

#### VehicleRef（嵌套）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int64 | 是 | 车辆 ID |
| name | string | 是 | 车型名称 |
| images | string[] | 是 | 图片 URL 数组 |

#### RejectOrderRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | string | 否 | 拒绝原因（可选） |

---

### 订阅消息

#### SubscriptionRecordRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| template_id | string | 是 | 微信订阅消息模板 ID |

---

### 管理端 - 车辆

#### CreateVehicleRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 车型名称 |
| brand | string | 是 | 品牌 |
| seats | int | 是 | 座位数 |
| transmission | string | 是 | 变速箱：`auto` / `manual` |
| description | string | 否 | 车辆描述 |
| images | string[] | 是 | 图片 URL 数组 |
| weekday_price | string(decimal) | 是 | 工作日日租金 |
| weekend_price | string(decimal) | 是 | 周末日租金 |
| holiday_price | string(decimal) | 否 | 节假日日租金（不填取 weekend_price） |

---

### 管理端 - 价格设置

#### CreateHolidayRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 节假日名称 |
| start_date | date | 是 | 开始日期 |
| end_date | date | 是 | 结束日期 |
| price_multiplier | string(decimal) | 否 | 价格倍率，默认 1.5 |
| fixed_price | string(decimal) | 否 | 固定价格（优先级高于 multiplier） |
| year | int | 是 | 适用年份 |

---

### 管理端 - 仪表盘

#### DashboardOverview

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| today_orders | int | 是 | 今日订单数 |
| today_revenue | string(decimal) | 是 | 今日收入 |
| month_orders | int | 是 | 本月订单数 |
| month_revenue | string(decimal) | 是 | 本月收入 |
| active_orders | int | 是 | 进行中订单数 |
| available_vehicles | int | 是 | 可租车辆数 |

---

### 用户协议

#### UpdateAgreementRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | string | 是 | 协议内容 |

---

## 枚举值定义

### OrderStatus（订单状态）

| 值 | 中文名 | 说明 |
|----|--------|------|
| `pending` | 待确认 | 用户已下单，等待管理员确认 |
| `confirmed` | 已确认 | 管理员已确认，等待到店取车 |
| `in_progress` | 进行中 | 用户已取车，租赁进行中 |
| `completed` | 已完成 | 用户已还车，订单完成 |
| `cancelled` | 已取消 | 用户主动取消 |
| `rejected` | 已拒绝 | 管理员拒绝 |

### PaymentStatus（支付状态）

| 值 | 中文名 | 说明 |
|----|--------|------|
| `unpaid` | 未支付 | |
| `paid` | 已支付 | |
| `refunded` | 已退款 | |

### Transmission（变速箱类型）

| 值 | 中文名 | 说明 |
|----|--------|------|
| `auto` | 自动挡 | |
| `manual` | 手动挡 | |

### PriceType（价格类型）

| 值 | 中文名 | 说明 |
|----|--------|------|
| `weekday` | 工作日 | 周一至周五 |
| `weekend` | 周末 | 周六、周日 |
| `holiday` | 节假日 | 节假日配置区间 |

### Role（用户角色）

| 值 | 中文名 | 说明 |
|----|--------|------|
| `user` | 普通用户 | 小程序默认角色 |
| `admin` | 管理员 | 管理端权限 |

---

## 错误码定义

| 错误码 | 级别 | 说明 | 触发场景 |
|--------|------|------|---------|
| 0 | F | 成功 | 所有成功响应 |
| 4000 | F | 参数错误 | 请求体校验失败、缺少必填字段 |
| 4001 | F | 微信登录失败 | login_code 无效/过期 |
| 4002 | F | 手机号获取失败 | phone_code 无效或用户拒绝授权 |
| 4003 | F | 未登录/Token 无效 | 缺少 Authorization header 或 Token 过期 |
| 4004 | F | 资源不存在 | 车辆/订单 ID 不存在 |
| 4010 | C | 无权操作 | 非 admin 角色访问管理端接口 |
| 5000 | F | 服务器内部错误 | 未预期异常 |
| 5100 | F | 微信支付失败（v1.5） | 支付接口调用失败 |
| 5101 | F | 退款失败（v1.5） | 退款接口调用失败 |
| 5200 | C | 时间段冲突 | 同一辆车在相同时间段已被预订 |
| 5300 | C | 订单状态不允许此操作 | 状态机非法跳转 |

> 错误码分类：F = Fatal（系统/外部依赖错误），C = Client Error（客户端可重试或需修正）

---

## 数据类型约定

| 后端类型 | OpenAPI 类型 | 说明 |
|---------|-------------|------|
| BigDecimal/Decimal | string(decimal) | **统一用 string**，避免前端精度丢失 |
| Long/int64 | integer(int64) | 数据库主键类型 |
| Date | string(date) | 格式：`yyyy-MM-dd` |
| DateTime | string(date-time) | 格式：`yyyy-MM-ddTHH:mm:ss` |
| JSON 数组 | array | 如 `images: string[]` |

---

*契约版本: v2.0 | 最后更新: 2026-04-10*
