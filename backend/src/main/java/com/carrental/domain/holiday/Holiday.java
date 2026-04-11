package com.carrental.domain.holiday;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 节假日配置领域对象
 *
 * 业务规则（ADR-009）:
 *   1. 定价优先级: fixed_price > weekend_price * multiplier > weekend_price > weekday_price
 *   2. fixed_price 非空时，直接使用固定价格
 *   3. fixed_price 为空时，使用 weekend_price * price_multiplier
 */
@Data
public class Holiday {

    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal priceMultiplier;  // DECIMAL(5,2), 默认 1.5
    private BigDecimal fixedPrice;       // DECIMAL(10,2), 可为 null
    private Integer year;

    /**
     * 计算节假日当天的实际价格
     * @param weekdayPrice 车辆工作日价格
     * @param weekendPrice 车辆周末价格
     * @return 计算后的节假日价格
     */
    public BigDecimal calculatePrice(BigDecimal weekdayPrice, BigDecimal weekendPrice) {
        // 优先级 1: fixed_price
        if (fixedPrice != null) {
            return fixedPrice;
        }
        // 优先级 2: weekend_price * multiplier
        if (weekendPrice != null && priceMultiplier != null) {
            return weekendPrice.multiply(priceMultiplier);
        }
        // 优先级 3: weekend_price
        if (weekendPrice != null) {
            return weekendPrice;
        }
        // 优先级 4: weekday_price (兜底)
        return weekdayPrice;
    }

    /**
     * 判断指定日期是否在当前节假日范围内
     */
    public boolean covers(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 判断是否与另一个节假日日期范围重叠
     */
    public boolean overlapsWith(Holiday other) {
        return !endDate.isBefore(other.startDate) && !startDate.isAfter(other.endDate);
    }
}
