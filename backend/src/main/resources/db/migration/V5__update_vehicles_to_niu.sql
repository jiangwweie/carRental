-- 更新车辆数据为小牛电动车系列
-- 注意:图片URL需要在上传后更新

-- 1. NQi GT - 高性能电动摩托车
UPDATE vehicles SET
    name = '小牛 NQi GT 2024 款',
    brand = '小牛',
    seats = 2,
    transmission = '电动',
    description = '小牛旗舰级电动摩托车,双电池续航可达180km,最高时速80km/h,配备智能APP和GPS定位',
    weekday_price = 120.00,
    weekend_price = 150.00
WHERE id = 1;

-- 2. MQi+ - 城市通勤电动车
UPDATE vehicles SET
    name = '小牛 MQi+ 2024 款',
    brand = '小牛',
    seats = 2,
    transmission = '电动',
    description = '城市通勤首选,续航120km,轻量化设计,智能防盗系统,适合日常出行',
    weekday_price = 80.00,
    weekend_price = 100.00
WHERE id = 2;

-- 3. UQi - 时尚轻便电动车
UPDATE vehicles SET
    name = '小牛 UQi 2024 款',
    brand = '小牛',
    seats = 2,
    transmission = '电动',
    description = '时尚轻便电动车,续航100km,智能APP控制,适合年轻人城市出行',
    weekday_price = 60.00,
    weekend_price = 80.00
WHERE id = 3;

-- 4. NQi - 经典电动摩托车
UPDATE vehicles SET
    name = '小牛 NQi 2024 款',
    brand = '小牛',
    seats = 2,
    transmission = '电动',
    description = '经典电动摩托车,续航150km,动力强劲,配备CBS联动刹车系统',
    weekday_price = 100.00,
    weekend_price = 130.00
WHERE id = 4;

-- 5. MQi - 实用电动踏板车
UPDATE vehicles SET
    name = '小牛 MQi 2024 款',
    brand = '小牛',
    seats = 2,
    transmission = '电动',
    description = '实用电动踏板车,续航110km,储物空间大,适合买菜接送孩子',
    weekday_price = 70.00,
    weekend_price = 90.00
WHERE id = 5;
