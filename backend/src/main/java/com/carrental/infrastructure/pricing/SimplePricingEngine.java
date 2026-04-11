package com.carrental.infrastructure.pricing;

import com.carrental.domain.pricing.PricingEngine;
import com.carrental.domain.pricing.PricingResult;
import com.carrental.domain.pricing.PricingResult.DayPrice;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * MVP 简化定价引擎：仅使用 days × weekday_price
 * ADR-009: MVP 阶段所有 dayPrice.type 均为 "weekday"
 * Sprint 4 升级为完整 PricingEngine（支持周末/节假日差异化定价）
 */
@Component
public class SimplePricingEngine implements PricingEngine {

    @Override
    public PricingResult calculate(BigDecimal weekdayPrice, BigDecimal weekendPrice,
                                   BigDecimal holidayPrice, LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        List<DayPrice> dayPrices = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (long i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            // MVP: 所有天数都按 weekday 价格
            DayPrice dp = new DayPrice(date.toString(), "weekday", weekdayPrice);
            dayPrices.add(dp);
            totalPrice = totalPrice.add(weekdayPrice);
        }

        return new PricingResult(dayPrices, totalPrice);
    }
}
