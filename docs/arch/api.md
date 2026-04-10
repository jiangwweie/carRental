# 租车应用 - API 接口文档

## 基础信息

| 项目 | 说明 |
|------|------|
| **Base URL** | `/api/v1` |
| **认证方式** | JWT Bearer Token（Header: `Authorization: Bearer <token>`） |
| **响应格式** | 统一 JSON 包装 |
| **时间格式** | `YYYY-MM-DD`（日期），`YYYY-MM-DDTHH:mm:ss`（日期时间） |
| **字符编码** | UTF-8 |

### 统一响应格式

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

// 分页响应
{
  "code": 0,
  "data": {
    "total": 15,
    "items": [ ... ]
  },
  "message": "success"
}
```

---

## 错误码定义

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 0 | 成功 | - |
| 4000 | 参数错误 | 检查请求参数格式和必填项 |
| 4001 | 微信登录失败 | code 无效或过期，重新调用 wx.login |
| 4002 | 手机号获取失败 | 用户拒绝授权，引导重新授权 |
| 4003 | 未登录 / Token 无效 | 跳转登录页 |
| 4004 | 资源不存在 | 检查资源 ID |
| 4010 | 无权操作 | 角色不匹配，检查用户权限 |
| 5000 | 服务器内部错误 | 重试，联系后端排查 |
| 5200 | 时间段冲突 | 车辆已被预订，提示用户更换日期 |
| 5300 | 订单状态不允许此操作 | 当前状态不支持该操作 |

---

## 1. 认证模块 (/auth)

### 1.1 微信登录

```
POST /api/v1/auth/wx-login
```

**认证**: 不需要

**请求体**:

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `login_code` | string | 是 | 非空 | `wx.login()` 获取的临时 code |
| `phone_code` | string | 是 | 非空 | `getUserPhoneNumber` 获取的 code |

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.token` | string | JWT Token，有效期 7 天 |
| `data.user.id` | number | 用户 ID |
| `data.user.phone` | string | 手机号 |
| `data.user.nickname` | string | 昵称（"用户+手机后4位"） |
| `data.user.role` | string | `user` 或 `admin` |
| `data.is_new_user` | boolean | 是否首次注册 |

**响应示例**:
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

**错误**:
- `4001`: 微信登录失败（code 无效/过期）
- `4002`: 获取手机号失败（用户拒绝授权）

---

### 1.2 PC 管理端登录

```
POST /api/v1/auth/admin-login
```

**认证**: 不需要

**请求体**:

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `phone` | string | 是 | 手机号格式 | PC 管理员手机号 |
| `password` | string | 是 | 非空 | 登录密码（BCrypt 校验） |

**响应**: 同 1.1 微信登录响应格式

**说明**: PC 管理端使用手机号+密码登录，小程序端使用微信授权登录。

---

### 1.3 刷新 Token

```
POST /api/v1/auth/refresh
```

**认证**: 需要

**请求体**: 无

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.token` | string | 新的 JWT Token |

**说明**: Token 临近过期时调用，保持登录态。前端应在 token 剩余有效期 < 1 天时主动调用此接口，或在收到 401 响应后尝试刷新。

---

## 2. 车辆模块（用户端 /vehicles）

### 2.1 车辆列表（公开）

```
GET /api/v1/vehicles
```

**认证**: 不需要

**查询参数**:

| 参数 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `min_price` | number | 否 | >= 0 | 最低日租金（按 weekday_price 筛选） |
| `max_price` | number | 否 | >= min_price | 最高日租金 |
| `page` | int | 否 | >= 1 | 页码，默认 1 |
| `page_size` | int | 否 | 1~100 | 每页数量，默认 20 |

> **注意**：MVP 阶段不包含 `seats` 筛选参数。`sort` 排序参数（US-16）延期至 v1.5。

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.total` | number | 符合条件的总记录数 |
| `data.items` | array | 车辆列表 |
| `data.items[].id` | number | 车辆 ID |
| `data.items[].name` | string | 车型名称 |
| `data.items[].brand` | string | 品牌 |
| `data.items[].seats` | number | 座位数 |
| `data.items[].transmission` | string | `auto` 或 `manual` |
| `data.items[].cover_image` | string | 封面图 base64 字符串（images 数组第一项） |
| `data.items[].weekday_price` | number | 工作日日租金（元） |
| `data.items[].weekend_price` | number | 周末日租金（元） |

**响应示例**:
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
        "cover_image": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
        "weekday_price": 299.00,
        "weekend_price": 399.00
      }
    ]
  },
  "message": "success"
}
```

**说明**: 仅返回 `status=active` 且未软删除的车辆。`cover_image` 为 base64 字符串，可直接用于 `<image src>` 或 `style="background-image: url(...)"`。

---

### 2.2 车辆详情（公开）

```
GET /api/v1/vehicles/{id}
```

**认证**: 不需要

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | number | 车辆 ID |

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.id` | number | 车辆 ID |
| `data.name` | string | 车型名称 |
| `data.brand` | string | 品牌 |
| `data.seats` | number | 座位数 |
| `data.transmission` | string | `auto` 或 `manual` |
| `data.description` | string | 车辆描述/使用规则 |
| `data.images` | array\<string\> | 图片 base64 字符串数组（US-17 图片轮播数据源） |
| `data.weekday_price` | number | 工作日日租金（元） |
| `data.weekend_price` | number | 周末日租金（元） |
| `data.holiday_price` | number \| null | 节假日日租金（MVP 阶段可能为 null） |
| `data.tags` | array\<string\> | 标签数组（如 ["新能源"]） |

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "name": "特斯拉 Model 3",
    "brand": "Tesla",
    "seats": 5,
    "transmission": "auto",
    "description": "纯电续航556km，适合周边自驾。注意：满电取车，满电归还。",
    "images": [
      "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
      "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
      "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
    ],
    "weekday_price": 299.00,
    "weekend_price": 399.00,
    "holiday_price": null,
    "tags": ["新能源"]
  },
  "message": "success"
}
```

**错误**:
- `4004`: 车辆不存在或已下架

---

## 3. 定价模块 (/pricing)

### 3.1 价格估算

```
POST /api/v1/pricing/estimate
```

**认证**: 不需要（用户可在未登录状态下预览价格）

**请求体**:

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `vehicle_id` | number | 是 | 正整数 | 车辆 ID |
| `start_date` | string | 是 | `YYYY-MM-DD`，>= 今天 | 取车日期 |
| `end_date` | string | 是 | `YYYY-MM-DD`，> start_date | 还车日期 |

**请求示例**:
```json
{
  "vehicle_id": 1,
  "start_date": "2026-05-01",
  "end_date": "2026-05-05"
}
```

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.vehicle_id` | number | 车辆 ID |
| `data.vehicle_name` | string | 车型名称 |
| `data.start_date` | string | 取车日期 |
| `data.end_date` | string | 还车日期 |
| `data.days` | number | 租期天数 |
| `data.total_price` | number | 预估总价（元） |
| `data.price_breakdown` | array | 每日价格明细 |
| `data.price_breakdown[].date` | string | 日期 |
| `data.price_breakdown[].type` | string | 价格类型：`weekday` / `weekend` / `holiday` |
| `data.price_breakdown[].price` | number | 当日价格（元） |

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "vehicle_id": 1,
    "vehicle_name": "特斯拉 Model 3",
    "start_date": "2026-05-01",
    "end_date": "2026-05-05",
    "days": 4,
    "total_price": 1196.00,
    "price_breakdown": [
      { "date": "2026-05-01", "type": "weekday", "price": 299.00 },
      { "date": "2026-05-02", "type": "weekday", "price": 299.00 },
      { "date": "2026-05-03", "type": "weekday", "price": 299.00 },
      { "date": "2026-05-04", "type": "weekday", "price": 299.00 }
    ]
  },
  "message": "success"
}
```

> **MVP 说明**：当前版本仅使用 `weekday_price` 计算（`days × weekday_price`），所有 `price_breakdown[].type` 均为 `weekday`，`price` 均为 `weekday_price`。v1.5 将接入完整 PricingEngine，支持周末/节假日差异化定价。

**错误**:
- `4000`: 参数错误（日期格式不对、end_date <= start_date 等）
- `4004`: 车辆不存在或已下架

---

## 4. 订单模块（用户端 /orders）

### 4.1 创建订单

```
POST /api/v1/orders
```

**认证**: 需要

**请求体**:

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `vehicle_id` | number | 是 | 正整数 | 车辆 ID |
| `start_date` | string | 是 | `YYYY-MM-DD`，>= 今天 | 取车日期 |
| `end_date` | string | 是 | `YYYY-MM-DD`，> start_date | 还车日期 |
| `agreed` | boolean | 是 | 必须为 true | 用户协议勾选 |

**请求示例**:
```json
{
  "vehicle_id": 1,
  "start_date": "2026-05-01",
  "end_date": "2026-05-05",
  "agreed": true
}
```

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.order_id` | number | 订单 ID |
| `data.vehicle_name` | string | 车型名称 |
| `data.start_date` | string | 取车日期 |
| `data.end_date` | string | 还车日期 |
| `data.days` | number | 租期天数 |
| `data.total_price` | number | **后端计算的**总价（元） |
| `data.price_breakdown` | array | 每日价格明细（同 3.1 格式） |
| `data.status` | string | 订单状态，固定 `pending` |

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "order_id": 1,
    "vehicle_name": "特斯拉 Model 3",
    "start_date": "2026-05-01",
    "end_date": "2026-05-05",
    "days": 4,
    "total_price": 1196.00,
    "price_breakdown": [
      { "date": "2026-05-01", "type": "weekday", "price": 299.00 },
      { "date": "2026-05-02", "type": "weekday", "price": 299.00 },
      { "date": "2026-05-03", "type": "weekday", "price": 299.00 },
      { "date": "2026-05-04", "type": "weekday", "price": 299.00 }
    ],
    "status": "pending"
  },
  "message": "success"
}
```

**错误**:
- `4000`: 参数错误（agreed 不为 true、日期无效等）
- `4004`: 车辆不存在或已下架
- `5200`: 该时间段已被预订（时间段冲突）

**说明**:
- `totalPrice` 由后端计算，前端传入的价格仅用于展示预览，后端会忽略
- 下单前需检查 `agreed=true`，否则拒绝
- 前端需对提交按钮做防抖处理（500ms），防止重复提交
- 创建成功后订单状态为 `pending`（待确认）

---

### 4.2 我的订单列表

```
GET /api/v1/orders
```

**认证**: 需要

**查询参数**:

| 参数 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `status` | string | 否 | 见订单状态枚举 | 按状态筛选 |
| `page` | int | 否 | >= 1 | 页码，默认 1 |
| `page_size` | int | 否 | 1~100 | 每页数量，默认 20 |

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.total` | number | 总记录数 |
| `data.items` | array | 订单列表 |
| `data.items[].id` | number | 订单 ID |
| `data.items[].vehicle_name` | string | 车型名称 |
| `data.items[].vehicle_image` | string | 车辆封面 base64（images[0]） |
| `data.items[].start_date` | string | 取车日期 |
| `data.items[].end_date` | string | 还车日期 |
| `data.items[].days` | number | 租期天数 |
| `data.items[].total_price` | number | 总价（元） |
| `data.items[].status` | string | 订单状态 |
| `data.items[].status_label` | string | 状态中文名（前端直接展示） |
| `data.items[].created_at` | string | 创建时间 `YYYY-MM-DDTHH:mm:ss` |
| `data.items[].can_cancel` | boolean | 是否可以取消（仅 pending 状态为 true） |

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "total": 5,
    "items": [
      {
        "id": 1,
        "vehicle_name": "特斯拉 Model 3",
        "vehicle_image": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
        "start_date": "2026-05-01",
        "end_date": "2026-05-05",
        "days": 4,
        "total_price": 1196.00,
        "status": "pending",
        "status_label": "待确认",
        "created_at": "2026-04-09T10:00:00",
        "can_cancel": true
      }
    ]
  },
  "message": "success"
}
```

**说明**: 车辆信息通过两查询方式获取——先查订单获取 vehicle_id 列表，再批量查询车辆信息，在应用层组合。

---

### 4.3 订单详情

```
GET /api/v1/orders/{id}
```

**认证**: 需要

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | number | 订单 ID |

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.id` | number | 订单 ID |
| `data.vehicle` | object | 车辆信息 |
| `data.vehicle.id` | number | 车辆 ID |
| `data.vehicle.name` | string | 车型名称 |
| `data.vehicle.images` | array\<string\> | 图片 base64 数组 |
| `data.start_date` | string | 取车日期 |
| `data.end_date` | string | 还车日期 |
| `data.days` | number | 租期天数 |
| `data.total_price` | number | 总价（元） |
| `data.price_breakdown` | array | 每日价格明细 |
| `data.status` | string | 订单状态 |
| `data.status_label` | string | 状态中文名 |
| `data.status_steps` | array | 状态步骤条数据（用于前端 Steps 组件） |
| `data.payment_status` | string | 支付状态（MVP 固定 `unpaid`） |
| `data.pickup_address` | string | 取车地址（MVP 硬编码） |
| `data.pickup_instructions` | string | 取车指引说明 |
| `data.created_at` | string | 创建时间 |
| `data.can_cancel` | boolean | 是否可以取消 |

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "vehicle": {
      "id": 1,
      "name": "特斯拉 Model 3",
      "images": [
        "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
        "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
      ]
    },
    "start_date": "2026-05-01",
    "end_date": "2026-05-05",
    "days": 4,
    "total_price": 1196.00,
    "price_breakdown": [
      { "date": "2026-05-01", "type": "weekday", "price": 299.00 },
      { "date": "2026-05-02", "type": "weekday", "price": 299.00 },
      { "date": "2026-05-03", "type": "weekday", "price": 299.00 },
      { "date": "2026-05-04", "type": "weekday", "price": 299.00 }
    ],
    "status": "pending",
    "status_label": "待确认",
    "status_steps": [
      { "status": "pending", "label": "待确认", "active": true },
      { "status": "confirmed", "label": "已确认", "active": false },
      { "status": "in_progress", "label": "进行中", "active": false },
      { "status": "completed", "label": "已完成", "active": false }
    ],
    "payment_status": "unpaid",
    "pickup_address": "北京市朝阳区XX路XX号",
    "pickup_instructions": "到达后联系管理员取车",
    "created_at": "2026-04-09T10:00:00",
    "can_cancel": true
  },
  "message": "success"
}
```

**错误**:
- `4004`: 订单不存在或无权查看

---

### 4.4 取消订单

```
POST /api/v1/orders/{id}/cancel
```

**认证**: 需要

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | number | 订单 ID |

**请求体**: 无

**响应**:
```json
{
  "code": 0,
  "data": { "status": "cancelled" },
  "message": "订单已取消"
}
```

**错误**:
- `4004`: 订单不存在
- `5300`: 订单状态不允许取消（仅 `pending` 状态可取消）

---

### 4.5 发起支付（v1.5）

```
POST /api/v1/orders/{id}/pay
```

**认证**: 需要

**说明**: v1.0 暂不实现。v1.5 返回小程序支付所需参数（`timeStamp`, `nonceStr`, `package`, `signType`, `paySign`）。

---

## 5. 订阅消息 (/subscription)

### 5.1 记录订阅授权

```
POST /api/v1/subscription/record
```

**认证**: 需要

**请求体**:

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `template_id` | string | 是 | 非空 | 微信订阅消息模板 ID |

**请求示例**:
```json
{
  "template_id": "xxx-order-confirm-template-id"
}
```

**响应**:
```json
{
  "code": 0,
  "data": { "status": "accepted" },
  "message": "success"
}
```

**说明**: 小程序端用户授权订阅消息后调用，记录授权状态。管理员确认/拒绝订单时，后端查询此表并推送通知。

---

## 6. 订单模块（管理端 /admin/orders）

### 6.1 订单列表

```
GET /api/v1/admin/orders
```

**认证**: 需要（管理员权限 `admin`）

**查询参数**:

| 参数 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `status` | string | 否 | 见订单状态枚举 | 按状态筛选 |
| `vehicle_id` | number | 否 | 正整数 | 按车辆筛选 |
| `start_date` | string | 否 | `YYYY-MM-DD` | 按租期起始日期筛选 |
| `end_date` | string | 否 | `YYYY-MM-DD` | 按租期结束日期筛选 |
| `page` | int | 否 | >= 1 | 页码，默认 1 |
| `page_size` | int | 否 | 1~100 | 每页数量，默认 20 |

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.total` | number | 总记录数 |
| `data.items` | array | 订单列表 |
| `data.items[].id` | number | 订单 ID |
| `data.items[].user_phone` | string | 用户手机号（脱敏：`138****8000`） |
| `data.items[].vehicle_name` | string | 车型名称 |
| `data.items[].vehicle_image` | string | 车辆封面 base64 |
| `data.items[].start_date` | string | 取车日期 |
| `data.items[].end_date` | string | 还车日期 |
| `data.items[].days` | number | 租期天数 |
| `data.items[].total_price` | number | 总价（元） |
| `data.items[].status` | string | 订单状态 |
| `data.items[].status_label` | string | 状态中文名 |
| `data.items[].created_at` | string | 创建时间 |

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "total": 10,
    "items": [
      {
        "id": 1,
        "user_phone": "138****8000",
        "vehicle_name": "特斯拉 Model 3",
        "vehicle_image": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
        "start_date": "2026-05-01",
        "end_date": "2026-05-05",
        "days": 4,
        "total_price": 1196.00,
        "status": "pending",
        "status_label": "待确认",
        "created_at": "2026-04-09T10:00:00"
      }
    ]
  },
  "message": "success"
}
```

**说明**（US-23）：支持按状态和车辆筛选，管理端可用于快速定位待确认订单。

---

### 6.2 确认订单

```
POST /api/v1/admin/orders/{id}/confirm
```

**认证**: 需要（管理员权限）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | number | 订单 ID |

**请求体**: 无

**响应**:
```json
{
  "code": 0,
  "data": { "status": "confirmed" },
  "message": "订单已确认"
}
```

**说明**: 订单状态从 `pending` → `confirmed`，同时推送订阅消息通知用户。

**错误**:
- `4004`: 订单不存在
- `5300`: 订单不是 pending 状态，无法确认

---

### 6.3 拒绝订单

```
POST /api/v1/admin/orders/{id}/reject
```

**认证**: 需要（管理员权限）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | number | 订单 ID |

**请求体**:

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `reason` | string | 否 | 最大 500 字 | 拒绝原因（建议填写，用于通知用户） |

**请求示例**:
```json
{
  "reason": "车辆临时故障，无法按时交付"
}
```

**响应**:
```json
{
  "code": 0,
  "data": { "status": "rejected" },
  "message": "订单已拒绝"
}
```

**说明**: 订单状态从 `pending` → `rejected`，推送订阅消息通知用户（含拒绝原因）。v1.5 接入支付后自动触发退款。

**错误**:
- `4004`: 订单不存在
- `5300`: 订单不是 pending 状态

---

### 6.4 标记进行中

```
POST /api/v1/admin/orders/{id}/start
```

**认证**: 需要（管理员权限）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | number | 订单 ID |

**请求体**: 无

**响应**:
```json
{
  "code": 0,
  "data": { "status": "in_progress" },
  "message": "success"
}
```

**说明**: 订单状态从 `confirmed` → `in_progress`。

---

### 6.5 标记完成

```
POST /api/v1/admin/orders/{id}/complete
```

**认证**: 需要（管理员权限）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | number | 订单 ID |

**请求体**: 无

**响应**:
```json
{
  "code": 0,
  "data": { "status": "completed" },
  "message": "success"
}
```

**说明**: 订单状态从 `in_progress` → `completed`。

---

## 7. 车辆管理（管理端 /admin/vehicles）

### 7.1 车辆列表

```
GET /api/v1/admin/vehicles
```

**认证**: 需要（管理员权限）

**查询参数**:

| 参数 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `status` | string | 否 | `active` / `inactive` | 按状态筛选 |
| `page` | int | 否 | >= 1 | 页码，默认 1 |
| `page_size` | int | 否 | 1~100 | 每页数量，默认 20 |

**响应**: 同 2.1 车辆列表格式，但包含所有状态的车辆（不限制 active）。

---

### 7.2 创建车辆

```
POST /api/v1/admin/vehicles
```

**认证**: 需要（管理员权限）

**请求体**:

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `name` | string | 是 | 1~200 字 | 车型名称 |
| `brand` | string | 是 | 1~100 字 | 品牌 |
| `seats` | number | 是 | 1~20 | 座位数 |
| `transmission` | string | 是 | `auto` / `manual` | 变速箱类型 |
| `description` | string | 否 | 最大 2000 字 | 车辆描述/使用规则 |
| `images` | array\<string\> | 是 | 非空数组 | 图片 base64 字符串数组 |
| `weekday_price` | number | 是 | > 0 | 工作日日租金（元） |
| `weekend_price` | number | 是 | > 0 | 周末日租金（元） |
| `holiday_price` | number | 否 | > 0 | 节假日日租金，不填时默认使用 weekend_price |

**请求示例**:
```json
{
  "name": "特斯拉 Model 3",
  "brand": "Tesla",
  "seats": 5,
  "transmission": "auto",
  "description": "纯电续航556km，满电取车满电归还。",
  "images": [
    "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
    "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
  ],
  "weekday_price": 299.00,
  "weekend_price": 399.00,
  "holiday_price": 499.00
}
```

**响应**: 返回创建后的车辆对象（同 2.2 车辆详情格式）。

---

### 7.3 更新车辆

```
PUT /api/v1/admin/vehicles/{id}
```

**认证**: 需要（管理员权限）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | number | 车辆 ID |

**请求体**: 同 7.2 创建车辆，所有字段均为可选（仅更新传入的字段）。

---

### 7.4 上下架

```
POST /api/v1/admin/vehicles/{id}/toggle-status
```

**认证**: 需要（管理员权限）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | number | 车辆 ID |

**请求体**: 无

**响应**:
```json
{
  "code": 0,
  "data": { "status": "active" },
  "message": "success"
}
```

**说明**: 切换 `active` / `inactive` 状态。下架车辆不再在用户端展示。

---

### 7.5 删除车辆

```
DELETE /api/v1/admin/vehicles/{id}
```

**认证**: 需要（管理员权限）

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | number | 车辆 ID |

**响应**:
```json
{
  "code": 0,
  "data": null,
  "message": "删除成功"
}
```

**说明**: 软删除，数据库标记 `deleted_at`。已有订单关联的车辆不可删除。

---

## 8. 价格设置（管理端 /admin/pricing）

### 8.1 节假日配置列表

```
GET /api/v1/admin/pricing/holidays
```

**认证**: 需要（管理员权限）

**查询参数**:

| 参数 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `year` | number | 否 | 4 位年份 | 按年份筛选，默认当前年 |

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | array | 节假日配置列表 |
| `data[].id` | number | 配置 ID |
| `data[].name` | string | 节假日名称 |
| `data[].start_date` | string | 开始日期 |
| `data[].end_date` | string | 结束日期 |
| `data[].price_multiplier` | number | 价格倍率 |
| `data[].fixed_price` | number \| null | 固定价格（优先级高于倍率） |
| `data[].year` | number | 适用年份 |

**响应示例**:
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
      "fixed_price": null,
      "year": 2026
    }
  ],
  "message": "success"
}
```

---

### 8.2 创建节假日

```
POST /api/v1/admin/pricing/holidays
```

**认证**: 需要（管理员权限）

**请求体**:

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `name` | string | 是 | 1~100 字 | 节假日名称 |
| `start_date` | string | 是 | `YYYY-MM-DD` | 开始日期 |
| `end_date` | string | 是 | `YYYY-MM-DD`，>= start_date | 结束日期 |
| `price_multiplier` | number | 否 | > 0 | 价格倍率，默认 1.5 |
| `fixed_price` | number | 否 | > 0 | 固定价格（优先级高于倍率） |
| `year` | number | 是 | 4 位整数 | 适用年份 |

**说明**: 节假日价格计算优先级：`fixed_price` > `weekend_price × multiplier` > `weekend_price` > `weekday_price`。

---

### 8.3 批量设置节假日

```
POST /api/v1/admin/pricing/holidays/batch
```

**认证**: 需要（管理员权限）

**请求体**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `holidays` | array | 是 | 节假日配置数组（字段同 8.2） |

**请求示例**:
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

**响应**:
```json
{
  "code": 0,
  "data": { "created": 2 },
  "message": "success"
}
```

**说明**: 事务性操作，任一节假日配置无效则全部回滚。

---

## 9. 仪表盘（管理端 /admin/dashboard）

### 9.1 获取概览

```
GET /api/v1/admin/dashboard/overview
```

**认证**: 需要（管理员权限）

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.today_orders` | number | 今日新增订单数 |
| `data.today_revenue` | number | 今日收入（元，已完成订单） |
| `data.month_orders` | number | 本月新增订单数 |
| `data.month_revenue` | number | 本月收入（元） |
| `data.active_orders` | number | 当前进行中订单数 |
| `data.available_vehicles` | number | 当前可租车辆数（active 状态） |

**响应示例**:
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
  },
  "message": "success"
}
```

### 9.2 管理端简化仪表盘（小程序用，US-24）

小程序管理端的仪表盘复用此接口。前端根据屏幕尺寸调整展示布局即可。

**额外字段**（仅小程序端显示）:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.pending_orders` | number | 待确认订单数（便于管理员快速处理） |

> **说明**：MVP 阶段小程序端仪表盘仅展示核心指标（待确认订单数、进行中订单数、今日收入），完整仪表盘在 PC 端使用。

---

## 10. 用户协议

### 10.1 获取当前协议

```
GET /api/v1/agreement
```

**认证**: 不需要

**响应**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.id` | number | 协议 ID |
| `data.content` | string | 协议内容（HTML 或纯文本） |
| `data.version` | string | 版本号 |
| `data.updated_at` | string | 更新时间 |

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "content": "本租车服务协议...",
    "version": "1.0",
    "updated_at": "2026-04-10T00:00:00"
  },
  "message": "success"
}
```

---

### 10.2 更新协议（管理端）

```
PUT /api/v1/admin/agreement
```

**认证**: 需要（管理员权限）

**请求体**:

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `content` | string | 是 | 非空 | 更新后的协议内容 |

**响应**: 返回新版本的协议信息（version 自动递增）。

**说明**: 创建新版本协议，旧版本保留在历史记录中。

---

## 11. 小程序管理端快捷接口 (/admin/mini)

以下接口用于小程序管理端的快捷操作，复用管理端接口，仅返回简化数据。

### 11.1 快捷订单列表

```
GET /api/v1/admin/mini/orders
```

**认证**: 需要（管理员权限 `admin`）

**查询参数**:

| 参数 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `status` | string | 否 | 见订单状态枚举 | 按状态筛选（默认仅 pending + in_progress） |
| `page` | int | 否 | >= 1 | 页码，默认 1 |

**响应**: 同 6.1 订单列表格式，但每项信息更精简。

**说明**（US-23）: 小程序管理端主要用于快速查看待确认订单和进行中的订单，筛选条件更少，信息更紧凑。

### 11.2 确认/拒绝订单

复用 6.2 确认订单 和 6.3 拒绝订单 接口。

---

## 订单状态枚举

| 值 | 中文名 | 说明 | 可执行操作 |
|----|--------|------|-----------|
| `pending` | 待确认 | 用户已下单，等待管理员确认 | 用户可取消，管理员可确认/拒绝 |
| `confirmed` | 已确认 | 管理员已确认，等待到店取车 | 管理员可标记进行中 |
| `in_progress` | 进行中 | 用户已取车，租赁进行中 | 管理员可标记完成 |
| `completed` | 已完成 | 用户已还车，订单完成 | 无 |
| `cancelled` | 已取消 | 用户主动取消 | 无 |
| `rejected` | 已拒绝 | 管理员拒绝 | 无 |

### 状态流转图

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

## 支付状态枚举

| 值 | 中文名 | 说明 |
|----|--------|------|
| `unpaid` | 未支付 | MVP 默认状态 |
| `paid` | 已支付 | v1.5 支付后状态 |
| `refunded` | 已退款 | v1.5 退款后状态 |

---

## 取车地址配置

MVP 阶段取车地址硬编码，通过以下常量提供：

| 配置项 | 值 | 位置 |
|--------|-----|------|
| `PICKUP_ADDRESS` | `北京市朝阳区XX路XX号` | 后端常量 / 前端 store/config.js |
| `PICKUP_INSTRUCTIONS` | `到达后联系管理员取车，电话: 138xxxx8000` | 同上 |

前端在预订页和订单详情页直接展示。v1.5 迁移至可配置方案。

---

*API 版本: v2.0 | 最后更新: 2026-04-10 | 终端: 微信小程序*
