package com.carrental.infrastructure.pricing;

import com.carrental.domain.holiday.Holiday;
import com.carrental.domain.holiday.HolidayRepository;
import com.carrental.domain.pricing.PricingEngine;
import com.carrental.domain.pricing.PricingResult;
import com.carrental.domain.pricing.PricingResult.DayPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 定价引擎实现（Sprint 2 升级）
 *
 * 价格计算优先级:
 *   1. 节假日 fixed_price（如有配置）
 *   2. 节假日 weekend_price * multiplier（如有配置）
 *   3. 周末 weekend_price
 *   4. 工作日 weekday_price（兜底）
 */
@Component
@RequiredArgsConstructor
public class SimplePricingEngine implements PricingEngine {

    private final HolidayRepository holidayRepository;

    @Override
    public PricingResult calculate(BigDecimal weekdayPrice, BigDecimal weekendPrice,
                                   BigDecimal holidayPrice, LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        List<DayPrice> dayPrices = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        // 预加载与日期范围重叠的所有节假日（一次性查询，避免 N+1）
        List<Holiday> holidays = holidayRepository.findOverlappingWith(startDate, endDate);

        for (long i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            BigDecimal price;
            String type;

            // 查找当前日期命中的节假日
            Holiday matchedHoliday = holidays.stream()
                    .filter(h -> h.covers(date))
                    .findFirst()
                    .orElse(null);

            if (matchedHoliday != null) {
                // 节假日：使用配置的定价规则
                price = matchedHoliday.calculatePrice(weekdayPrice, weekendPrice);
                type = "holiday";
            } else if (isWeekend(date)) {
                // 周末
                price = weekendPrice != null ? weekendPrice : weekdayPrice;
                type = "weekend";
            } else {
                // 工作日
                price = weekdayPrice;
                type = "weekday";
            }

            DayPrice dp = new DayPrice(date.toString(), type, price);
            dayPrices.add(dp);
            totalPrice = totalPrice.add(price);
        }

        return new PricingResult(dayPrices, totalPrice);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}
