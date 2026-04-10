-- V1__init_tables.sql
-- 初始化数据库表结构

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone           VARCHAR(20) NOT NULL UNIQUE,
    wechat_openid   VARCHAR(100) NOT NULL UNIQUE,
    nickname        VARCHAR(100) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'user',
    status          VARCHAR(20) NOT NULL DEFAULT 'active',
    password_hash   VARCHAR(100) DEFAULT NULL,
    must_change_pwd TINYINT(1) NOT NULL DEFAULT 0,
    id_card         VARCHAR(255) DEFAULT NULL,
    driver_license  VARCHAR(255) DEFAULT NULL,
    credit_score    INT DEFAULT NULL,
    member_level    INT DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_role CHECK (role IN ('user', 'admin'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_openid ON users(wechat_openid);
CREATE INDEX idx_users_role ON users(role);

-- 车辆表
CREATE TABLE IF NOT EXISTS vehicles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    brand           VARCHAR(100) NOT NULL,
    seats           INT NOT NULL,
    transmission    VARCHAR(20) NOT NULL,
    description     TEXT DEFAULT NULL,
    images          JSON,
    weekday_price   DECIMAL(10,2) NOT NULL,
    weekend_price   DECIMAL(10,2) NOT NULL,
    holiday_price   DECIMAL(10,2) DEFAULT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'active',
    store_id        BIGINT DEFAULT NULL,
    gps_device_id   VARCHAR(100) DEFAULT NULL,
    tags            JSON,
    deleted_at      DATETIME DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vehicles_status ON vehicles(status, deleted_at);
CREATE INDEX idx_vehicles_brand ON vehicles(brand);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    vehicle_id      BIGINT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    days            INT NOT NULL,
    total_price     DECIMAL(10,2) NOT NULL,
    price_breakdown JSON NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',
    payment_status  VARCHAR(20) NOT NULL DEFAULT 'unpaid',
    payment_id      VARCHAR(200) DEFAULT NULL,
    paid_at         DATETIME DEFAULT NULL,
    deposit_amount  DECIMAL(10,2) DEFAULT NULL,
    deposit_status  VARCHAR(20) DEFAULT NULL,
    insurance_id    BIGINT DEFAULT NULL,
    coupon_id       BIGINT DEFAULT NULL,
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

-- 节假日配置表
CREATE TABLE IF NOT EXISTS holiday_configs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    price_multiplier DECIMAL(5,2) NOT NULL DEFAULT 1.5,
    fixed_price     DECIMAL(10,2) DEFAULT NULL,
    year            INT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_holidays_dates ON holiday_configs(start_date, end_date);
CREATE INDEX idx_holidays_year ON holiday_configs(year);

-- 用户协议表
CREATE TABLE IF NOT EXISTS user_agreements (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    content         TEXT NOT NULL,
    version         VARCHAR(20) NOT NULL,
    is_active       TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_agreements_active ON user_agreements(is_active);

-- 消息订阅记录表
CREATE TABLE IF NOT EXISTS message_subscriptions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    template_id     VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'accepted',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_template (user_id, template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_subscriptions_user ON message_subscriptions(user_id);

-- 支付记录表 (v1.0 创建，v1.5 使用)
CREATE TABLE IF NOT EXISTS payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    payment_no      VARCHAR(50) NOT NULL UNIQUE,
    payment_id      VARCHAR(200) DEFAULT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',
    raw_request     JSON DEFAULT NULL,
    raw_response    JSON DEFAULT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_no ON payments(payment_no);
