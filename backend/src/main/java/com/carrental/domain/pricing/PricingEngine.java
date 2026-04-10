package com.carrental.domain.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 定价引擎接口
 * 根据日期类型（工作日/周末/节假日）计算每日价格和总价
 */
public interface PricingEngine {

    /**
     * 计算租期价格
     * @param weekdayPrice 工作日日租金
     * @param weekendPrice 周末日租金
     * @param holidayPrice 节假日日租金（可为null，默认取周末价）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 定价结果（含每日明细和总价）
     */
    PricingResult calculate(BigDecimal weekdayPrice, BigDecimal weekendPrice,
                            BigDecimal holidayPrice, LocalDate startDate, LocalDate endDate);
}
