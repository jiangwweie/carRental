# 租车应用 - 数据库设计

## 数据库: MySQL 8.0+

所有表使用 `BIGINT AUTO_INCREMENT` 作为主键，时间字段使用 `DATETIME`。

---

## 1. users - 用户表

```sql
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone           VARCHAR(20) NOT NULL UNIQUE,
    wechat_openid   VARCHAR(100) NOT NULL UNIQUE,
    nickname        VARCHAR(100) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'user',     -- user / admin
    status          VARCHAR(20) NOT NULL DEFAULT 'active',   -- active / disabled

    password_hash   VARCHAR(100) DEFAULT NULL,               -- PC管理端登录密码(BCrypt)，小程序用户为空
    must_change_pwd TINYINT(1) NOT NULL DEFAULT 0,          -- 首次登录强制修改密码

    CONSTRAINT chk_user_role CHECK (role IN ('user', 'admin')),

    -- === 预留字段 v2.0+ ===
    id_card         VARCHAR(255) DEFAULT NULL,               -- 身份证号 (身份验证)
    driver_license  VARCHAR(255) DEFAULT NULL,               -- 驾驶证号 (身份验证)
    credit_score    INT DEFAULT NULL,                        -- 信用分 (信用免押)
    member_level    INT DEFAULT 0,                           -- 会员等级 (会员体系)

    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_openid ON users(wechat_openid);
CREATE INDEX idx_users_role ON users(role);
```

**变化说明**（相对 PG 版本）:
- 主键从 UUID 改为 BIGINT 自增
- 新增 `wechat_openid`（必填，唯一索引）
- 新增 `role` 字段（user / admin）

---

## 2. vehicles - 车辆表

```sql
CREATE TABLE vehicles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,

    name            VARCHAR(200) NOT NULL,                   -- 车型名称
    brand           VARCHAR(100) NOT NULL,                   -- 品牌
    seats           INT NOT NULL,                            -- 座位数
    transmission    VARCHAR(20) NOT NULL,                    -- auto / manual
    description     TEXT DEFAULT '',                         -- 车辆描述/使用规则

    images          JSON NOT NULL DEFAULT '[]',             -- 图片URL数组

    weekday_price   DECIMAL(10,2) NOT NULL,                  -- 工作日日租金
    weekend_price   DECIMAL(10,2) NOT NULL,                  -- 周末日租金
    holiday_price   DECIMAL(10,2) DEFAULT NULL,              -- 节假日日租金(空则取weekend)

    status          VARCHAR(20) NOT NULL DEFAULT 'active',   -- active / inactive

    -- === 预留字段 v2.0+ ===
    store_id        BIGINT DEFAULT NULL,                     -- 所属门店 (多门店)
    gps_device_id   VARCHAR(100) DEFAULT NULL,               -- GPS设备ID (定位)
    tags            JSON DEFAULT '[]',                      -- 标签数组

    deleted_at      DATETIME DEFAULT NULL,                   -- 软删除
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vehicles_status ON vehicles(status, deleted_at);
CREATE INDEX idx_vehicles_brand ON vehicles(brand);
```

**变化说明**: `JSONB` → `JSON`，`TIMESTAMPTZ` → `DATETIME`，`uuid` → `BIGINT`。

---

## 3. orders - 订单表

```sql
CREATE TABLE orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id         BIGINT NOT NULL,
    vehicle_id      BIGINT NOT NULL,

    start_date      DATE NOT NULL,                           -- 取车日期
    end_date        DATE NOT NULL,                           -- 还车日期
    days            INT NOT NULL,                            -- 租期天数

    total_price     DECIMAL(10,2) NOT NULL,                  -- 总价
    price_breakdown JSON NOT NULL,                           -- 每日价格明细

    status          VARCHAR(20) NOT NULL DEFAULT 'pending',  -- 订单状态
    payment_status  VARCHAR(20) NOT NULL DEFAULT 'unpaid',   -- 支付状态

    payment_id      VARCHAR(200) DEFAULT NULL,               -- 微信支付流水号
    paid_at         DATETIME DEFAULT NULL,                   -- 支付时间

    -- === 预留字段 v2.0+ ===
    deposit_amount  DECIMAL(10,2) DEFAULT NULL,              -- 押金金额
    deposit_status  VARCHAR(20) DEFAULT NULL,                -- unpaid/paid/refunded
    insurance_id    BIGINT DEFAULT NULL,                     -- 保险产品ID
    coupon_id       BIGINT DEFAULT NULL,                     -- 优惠券ID

    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_vehicle ON orders(vehicle_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_dates ON orders(start_date, end_date);
CREATE INDEX idx_orders_created ON orders(created_at DESC);
```

**订单状态枚举**: `pending` | `confirmed` | `in_progress` | `completed` | `cancelled` | `rejected`
**支付状态枚举**: `unpaid` | `paid` | `refunded`

### price_breakdown JSON 示例

```json
[
  { "date": "2026-05-01", "type": "holiday", "price": 499 },
  { "date": "2026-05-02", "type": "holiday", "price": 499 },
  { "date": "2026-05-03", "type": "weekend", "price": 399 },
  { "date": "2026-05-04", "type": "weekend", "price": 399 }
]
```

---

## 4. holiday_configs - 节假日配置表

```sql
CREATE TABLE holiday_configs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,

    name            VARCHAR(100) NOT NULL,                   -- 节假日名称
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,

    price_multiplier DECIMAL(5,2) NOT NULL DEFAULT 1.5,      -- 价格倍率
    fixed_price     DECIMAL(10,2) DEFAULT NULL,              -- 固定价格(优先级高于倍率)

    year            INT NOT NULL,                            -- 适用年份

    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_holidays_dates ON holiday_configs(start_date, end_date);
CREATE INDEX idx_holidays_year ON holiday_configs(year);
```

**定价优先级**: `fixed_price` > `weekend_price × multiplier` > `weekend_price` > `weekday_price`

---

## 5. user_agreements - 用户协议表

```sql
CREATE TABLE user_agreements (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,

    content         TEXT NOT NULL,                           -- 协议内容
    version         VARCHAR(20) NOT NULL,                    -- 版本号
    is_active       TINYINT(1) NOT NULL DEFAULT 0,           -- 是否当前生效版本

    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_agreements_active ON user_agreements(is_active);
```

**变化说明**: `BOOLEAN` → `TINYINT(1)`（MySQL 无原生 BOOLEAN 类型）。

---

## 6. message_subscriptions - 消息订阅记录表（新增）

```sql
CREATE TABLE message_subscriptions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id         BIGINT NOT NULL,
    template_id     VARCHAR(100) NOT NULL,                   -- 微信订阅消息模板ID
    status          VARCHAR(20) NOT NULL DEFAULT 'accepted', -- accepted / revoked

    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_template (user_id, template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_subscriptions_user ON message_subscriptions(user_id);
```

**说明**: 记录用户授权的订阅消息模板，用于后端主动推送通知。

---

## 7. payments - 支付记录表（v1.0 创建但暂不使用）

```sql
CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,

    order_id        BIGINT NOT NULL,
    payment_no      VARCHAR(50) NOT NULL UNIQUE,             -- 内部支付流水号
    payment_id      VARCHAR(200) DEFAULT NULL,               -- 微信支付流水号

    amount          DECIMAL(10,2) NOT NULL,
    type            VARCHAR(20) NOT NULL,                    -- pay / refund
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',  -- pending / success / failed

    raw_request     JSON DEFAULT NULL,                       -- 微信支付请求记录
    raw_response    JSON DEFAULT NULL,                       -- 微信支付响应/回调

    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_no ON payments(payment_no);
```

---

## 8. v2.0+ 预留表（暂不创建）

```sql
-- 优惠券表
CREATE TABLE coupons ( ... );

-- 用户评价表
CREATE TABLE reviews ( ... );

-- 保险产品表
CREATE TABLE insurances ( ... );

-- 门店表
CREATE TABLE stores ( ... );

-- 会员方案表
CREATE TABLE member_plans ( ... );

-- GPS 轨迹表
CREATE TABLE gps_locations ( ... );
```

---

## ER 关系图

```
users 1 ──< orders >── 1 vehicles
     │         │
     │         └──> 1 payments
     │
     └──< message_subscriptions

holiday_configs (独立，定价时查询)
user_agreements (独立)
```

---

## 数据库迁移

使用 Flyway 管理迁移：

```bash
# Spring Boot 自动执行
# 迁移脚本放在 src/main/resources/db/migration/

# V1__init_tables.sql      - 初始建表
# V2__add_xxx_column.sql   - 后续变更
```

v1.0 初始迁移包含前 7 张表。v2.0 时新增预留表。

---

## MySQL vs PostgreSQL 变更说明

| 维度 | PG 版本 | MySQL 版本 |
|------|---------|-----------|
| 主键 | UUID | BIGINT AUTO_INCREMENT |
| 时间类型 | TIMESTAMPTZ | DATETIME |
| JSON 类型 | JSONB | JSON |
| 布尔类型 | BOOLEAN | TINYINT(1) |
| 外键 | REFERENCES 简写 | 显式 CONSTRAINT fk_xxx FOREIGN KEY |
| 部分索引 | 支持（WHERE 条件） | 不支持，改为普通索引 |
| 迁移工具 | Alembic | Flyway |
