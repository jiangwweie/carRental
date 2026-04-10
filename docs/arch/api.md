# 租车应用 - API 接口文档

## 基础信息

- **Base URL**: `/api/v1`
- **认证方式**: JWT Bearer Token（Header: `Authorization: Bearer <token>`）
- **响应格式**: 统一 JSON 响应

```json
// 成功响应
{
  "code": 0,
  "data": { ... },
  "message": "success"
}

// 错误响应
{
  "code": 4001,
  "data": null,
  "message": "微信登录失败，请重试"
}
```

---

## 错误码定义

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 4000 | 参数错误 |
| 4001 | 微信登录失败 |
| 4002 | 手机号获取失败 |
| 4003 | 未登录/Token 无效 |
| 4004 | 资源不存在 |
| 4010 | 无权操作（角色不匹配） |
| 5000 | 服务器内部错误 |
| 5100 | 微信支付失败（v1.5） |
| 5101 | 退款失败（v1.5） |
| 5200 | 时间段冲突（车辆已被预订） |
| 5300 | 订单状态不允许此操作 |

---

## 1. 认证模块 (/auth)

### 1.1 微信登录

```
POST /auth/wx-login
```

**请求体**:
```json
{
  "login_code": "081xxx",       // wx.login() 获取的 code
  "phone_code": "612xxx"        // getUserPhoneNumber 获取的 code
}
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": 1,
      "phone": "138****8000",
      "nickname": "用户8000",
      "role": "user"
    },
    "is_new_user": true
  },
  "message": "success"
}
```

**说明**: 
- 首次登录自动创建用户（nickname 默认为"用户+手机后4位"）
- Token 有效期 7 天
- 后端用 `login_code` 换 openid，用 `phone_code` 换手机号
- 如果 openid 已存在，直接登录

**错误**:
- `4001`: 微信登录失败（code 无效/过期）
- `4002`: 获取手机号失败（用户拒绝授权）

---

### 1.2 刷新 Token

```
POST /auth/refresh
```

**认证**: 需要

**说明**: Token 临近过期时刷新，保持登录态。

---

## 2. 车辆模块 (/vehicles)

### 2.1 车辆列表（公开）

```
GET /vehicles
```

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| min_price | number | 否 | 最低日租金 |
| max_price | number | 否 | 最高日租金 |
| page | int | 否 | 页码，默认 1 |
| page_size | int | 否 | 每页数量，默认 20 |

**响应**:
```json
{
  "code": 0,
  "data": {
    "total": 15,
    "items": [
      {
        "id": 1,
        "name": "特斯拉 Model 3",
        "brand": "Tesla",
        "seats": 5,
        "transmission": "auto",
        "cover_image": "https://cos.example.com/vehicle1.jpg",
        "weekday_price": 299,
        "weekend_price": 399,
        "tags": ["新能源"]
      }
    ]
  }
}
```

**说明**: 仅返回 `status=active` 的车辆。`cover_image` 取自 `images` 数组第一个元素。

---

### 2.2 车辆详情（公开）

```
GET /vehicles/{id}
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "name": "特斯拉 Model 3",
    "brand": "Tesla",
    "seats": 5,
    "transmission": "auto",
    "description": "纯电续航556km，适合周边自驾...",
    "images": [
      "https://cos.example.com/vehicle1-1.jpg",
      "https://cos.example.com/vehicle1-2.jpg"
    ],
    "weekday_price": 299,
    "weekend_price": 399,
    "holiday_price": 499,
    "tags": ["新能源"]
  }
}
```

---

## 3. 订单模块 (用户端 /orders)

### 3.1 创建订单

```
POST /orders
```

**认证**: 需要

**请求体**:
```json
{
  "vehicle_id": 1,
  "start_date": "2026-05-01",
  "end_date": "2026-05-05"
}
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "order_id": 1,
    "vehicle_name": "特斯拉 Model 3",
    "start_date": "2026-05-01",
    "end_date": "2026-05-05",
    "days": 4,
    "price_breakdown": [
      { "date": "2026-05-01", "type": "holiday", "price": 499 },
      { "date": "2026-05-02", "type": "holiday", "price": 499 },
      { "date": "2026-05-03", "type": "weekend", "price": 399 },
      { "date": "2026-05-04", "type": "weekend", "price": 399 }
    ],
    "total_price": 1796
  }
}
```

**错误**:
- `5200`: 该时间段已被预订
- `4004`: 车辆不存在或已下架

**说明**: v1.0 创建订单后状态直接为 `pending`（待确认），无需支付。v1.5 接入支付后需要调用支付接口。前端需对提交按钮做防抖处理（500ms），防止重复提交。

---

### 3.2 发起支付（v1.5）

```
POST /orders/{id}/pay
```

**认证**: 需要

**说明**: v1.0 暂不实现。v1.5 返回小程序支付所需参数。

---

### 3.3 我的订单列表

```
GET /orders
```

**认证**: 需要

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | 状态筛选 |
| page | int | 否 | 页码 |

**响应**:
```json
{
  "code": 0,
  "data": {
    "total": 5,
    "items": [
      {
        "id": 1,
        "vehicle_name": "特斯拉 Model 3",
        "vehicle_image": "https://cos.example.com/v1.jpg",
        "start_date": "2026-05-01",
        "end_date": "2026-05-05",
        "total_price": 1796,
        "status": "pending",
        "created_at": "2026-04-09T10:00:00"
      }
    ]
  }
}
```

---

### 3.4 订单详情

```
GET /orders/{id}
```

**认证**: 需要

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "vehicle": {
      "id": 1,
      "name": "特斯拉 Model 3",
      "images": ["..."]
    },
    "start_date": "2026-05-01",
    "end_date": "2026-05-05",
    "days": 4,
    "total_price": 1796,
    "status": "pending",
    "payment_status": "unpaid",
    "price_breakdown": [...],
    "created_at": "2026-04-09T10:00:00"
  }
}
```

---

### 3.5 取消订单

```
POST /orders/{id}/cancel
```

**认证**: 需要

**限制**: 仅 `pending` 状态可取消。

**响应**:
```json
{
  "code": 0,
  "data": { "status": "cancelled" },
  "message": "订单已取消"
}
```

---

## 4. 订阅消息 (/subscription)

### 4.1 记录订阅授权

```
POST /subscription/record
```

**认证**: 需要

**请求体**:
```json
{
  "template_id": "xxx"
}
```

**说明**: 小程序端用户授权订阅消息后调用，记录授权状态。

---

## 5. 订单模块 (管理端 /admin/orders)

### 5.1 订单列表

```
GET /admin/orders
```

**认证**: 需要（管理员权限）

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | 状态筛选 |
| vehicle_id | int | 否 | 车辆筛选 |
| start_date | string | 否 | 开始日期 |
| end_date | string | 否 | 结束日期 |
| page | int | 否 | 页码 |

**响应**: 同用户端列表 + 额外字段 `user_phone`。`vehicle_image` 通过 JOIN vehicles 表取 `images[0]`。

---

### 5.2 确认订单

```
POST /admin/orders/{id}/confirm
```

**认证**: 需要（管理员权限）

**说明**: 订单状态从 `pending` → `confirmed`，同时推送订阅消息通知用户。

---

### 5.3 拒绝订单

```
POST /admin/orders/{id}/reject
```

**认证**: 需要（管理员权限）

**请求体**:
```json
{
  "reason": "车辆临时故障"
}
```

**说明**: 订单状态从 `pending` → `rejected`，推送订阅消息通知用户。v1.5 接入支付后自动触发退款。

---

### 5.4 标记进行中

```
POST /admin/orders/{id}/start
```

**认证**: 需要（管理员权限）

**说明**: 订单状态从 `confirmed` → `in_progress`。

---

### 5.5 标记完成

```
POST /admin/orders/{id}/complete
```

**认证**: 需要（管理员权限）

**说明**: 订单状态从 `in_progress` → `completed`。

---

## 6. 车辆管理 (管理端 /admin/vehicles)

### 6.1 车辆列表

```
GET /admin/vehicles
```

**认证**: 需要（管理员权限）

**说明**: 返回所有车辆（含下架），支持按状态筛选。

---

### 6.2 创建车辆

```
POST /admin/vehicles
```

**认证**: 需要（管理员权限）

**请求体**:
```json
{
  "name": "特斯拉 Model 3",
  "brand": "Tesla",
  "seats": 5,
  "transmission": "auto",
  "description": "纯电续航556km...",
  "images": ["https://cos.example.com/v1.jpg"],
  "weekday_price": 299,
  "weekend_price": 399,
  "holiday_price": 499
}
```

**说明**: `holiday_price` 可选，不填时默认使用 `weekend_price`。

---

### 6.3 更新车辆

```
PUT /admin/vehicles/{id}
```

**认证**: 需要（管理员权限）

**请求体**: 同创建，所有字段可选。

---

### 6.4 上下架

```
POST /admin/vehicles/{id}/toggle-status
```

**认证**: 需要（管理员权限）

**说明**: 切换 `active` / `inactive` 状态。下架车辆不在用户端展示。

---

### 6.5 删除车辆

```
DELETE /admin/vehicles/{id}
```

**认证**: 需要（管理员权限）

**说明**: 软删除，数据库标记 `deleted_at`。已有订单关联的车辆不可删除。

---

## 7. 价格设置 (管理端 /admin/pricing)

### 7.1 节假日配置列表

```
GET /admin/pricing/holidays
```

**认证**: 需要（管理员权限）

**响应**:
```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "name": "国庆节",
      "start_date": "2026-10-01",
      "end_date": "2026-10-07",
      "price_multiplier": 1.5,
      "year": 2026
    }
  ]
}
```

---

### 7.2 创建节假日

```
POST /admin/pricing/holidays
```

**认证**: 需要（管理员权限）

**请求体**:
```json
{
  "name": "国庆节",
  "start_date": "2026-10-01",
  "end_date": "2026-10-07",
  "price_multiplier": 1.5,
  "year": 2026
}
```

**说明**: 节假日价格 = 周末价格 × multiplier。也可直接设 fixed_price 覆盖。

---

### 7.3 批量设置节假日

```
POST /admin/pricing/holidays/batch
```

**认证**: 需要（管理员权限）

**请求体**:
```json
{
  "holidays": [
    {
      "name": "五一",
      "start_date": "2026-05-01",
      "end_date": "2026-05-05",
      "price_multiplier": 1.3,
      "year": 2026
    },
    {
      "name": "国庆",
      "start_date": "2026-10-01",
      "end_date": "2026-10-07",
      "price_multiplier": 1.5,
      "year": 2026
    }
  ]
}
```

---

## 8. 仪表盘 (管理端 /admin/dashboard)

### 8.1 获取概览

```
GET /admin/dashboard/overview
```

**认证**: 需要（管理员权限）

**响应**:
```json
{
  "code": 0,
  "data": {
    "today_orders": 3,
    "today_revenue": 1500.00,
    "month_orders": 45,
    "month_revenue": 28000.00,
    "active_orders": 2,
    "available_vehicles": 8
  }
}
```

---

## 9. 小程序管理端 (/admin/mini)

### 9.1 订单列表（简化）

```
GET /admin/mini/orders
```

**认证**: 需要（管理员权限）

**说明**: 仅返回待确认和进行中的订单，信息简化（用户手机号后4位、车型、租期）。

---

### 9.2 确认订单

同 `5.2 确认订单`。

### 9.3 拒绝订单

同 `5.3 拒绝订单`。

---

## 10. 用户协议

### 10.1 获取当前协议

```
GET /agreement
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "content": "本租车服务协议...",
    "version": "1.0",
    "updated_at": "2026-04-10"
  }
}
```

---

### 10.2 更新协议 (管理端)

```
PUT /admin/agreement
```

**认证**: 需要（管理员权限）

**请求体**:
```json
{
  "content": "更新后的协议内容..."
}
```

**说明**: 创建新版本，旧版本保留历史记录。

---

## 订单状态枚举

| 值 | 中文名 | 说明 |
|----|--------|------|
| `pending` | 待确认 | 用户已下单，等待管理员确认 |
| `confirmed` | 已确认 | 管理员已确认，等待到店取车 |
| `in_progress` | 进行中 | 用户已取车，租赁进行中 |
| `completed` | 已完成 | 用户已还车，订单完成 |
| `cancelled` | 已取消 | 用户主动取消 |
| `rejected` | 已拒绝 | 管理员拒绝 |

## 支付状态枚举

| 值 | 中文名 |
|----|--------|
| `unpaid` | 未支付 |
| `paid` | 已支付 |
| `refunded` | 已退款 |

---

*API 版本: v2.0 | 最后更新: 2026-04-10 | 终端变更: H5 → 微信小程序*
