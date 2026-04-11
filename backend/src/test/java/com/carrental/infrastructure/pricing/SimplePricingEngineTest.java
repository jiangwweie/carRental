package com.carrental.infrastructure.pricing;

import com.carrental.domain.holiday.Holiday;
import com.carrental.domain.holiday.HolidayRepository;
import com.carrental.domain.pricing.PricingResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimplePricingEngine 单元测试（Sprint 2 升级）
 *
 * 实现逻辑:
 *   1. 节假日: matchedHoliday.calculatePrice(weekdayPrice, weekendPrice)
 *   2. 周末: weekendPrice (type = "weekend")
 *   3. 工作日: weekdayPrice (type = "weekday")
 */
class SimplePricingEngineTest {

    // ==================== 正常场景 ====================

    @Nested
    @DisplayName("正常价格计算（无节假日配置）")
    class NormalCalculationTests {

        @Test
        @DisplayName("1 天工作日租赁")
        void oneDay_weekday_rental() {
            SimplePricingEngine engine = new SimplePricingEngine(new EmptyHolidayRepository());
            BigDecimal weekdayPrice = new BigDecimal("100");
            BigDecimal weekendPrice = new BigDecimal("150");
            LocalDate start = LocalDate.of(2026, 4, 13); // Monday
            LocalDate end = LocalDate.of(2026, 4, 14);   // Tuesday

            PricingResult result = engine.calculate(weekdayPrice, weekendPrice, null, start, end);

            assertEquals(1, result.getDayPrices().size());
            assertEquals(new BigDecimal("100"), result.getTotalPrice());
            assertEquals("weekday", result.getDayPrices().get(0).getType());
        }

        @Test
        @DisplayName("3 天工作日租赁: 总价 = 3 × weekdayPrice")
        void threeDay_weekday_rental() {
            SimplePricingEngine engine = new SimplePricingEngine(new EmptyHolidayRepository());
            BigDecimal weekdayPrice = new BigDecimal("200");
            BigDecimal weekendPrice = new BigDecimal("300");
            LocalDate start = LocalDate.of(2026, 4, 13); // Monday
            LocalDate end = LocalDate.of(2026, 4, 16);   // Thursday

            PricingResult result = engine.calculate(weekdayPrice, weekendPrice, null, start, end);

            assertEquals(3, result.getDayPrices().size());
            assertEquals(new BigDecimal("600"), result.getTotalPrice());
            result.getDayPrices().forEach(dp -> {
                assertEquals("weekday", dp.getType());
                assertEquals(new BigDecimal("200"), dp.getPrice());
            });
        }

        @Test
        @DisplayName("7 天租赁（跨周末）: 5 工作日 + 2 周末")
        void sevenDay_rental_withWeekend() {
            SimplePricingEngine engine = new SimplePricingEngine(new EmptyHolidayRepository());
            BigDecimal weekdayPrice = new BigDecimal("150");
            BigDecimal weekendPrice = new BigDecimal("200");
            // Mon Apr 13 to Mon Apr 20 = 7 days: Mon-Fri(5) + Sat-Sun(2)
            LocalDate start = LocalDate.of(2026, 4, 13);
            LocalDate end = LocalDate.of(2026, 4, 20);

            PricingResult result = engine.calculate(weekdayPrice, weekendPrice, null, start, end);

            assertEquals(7, result.getDayPrices().size());
            // 5 weekdays * 150 + 2 weekend days * 200 = 750 + 400 = 1150
            assertEquals(new BigDecimal("1150"), result.getTotalPrice());

            long weekdayCount = result.getDayPrices().stream().filter(dp -> "weekday".equals(dp.getType())).count();
            long weekendCount = result.getDayPrices().stream().filter(dp -> "weekend".equals(dp.getType())).count();
            assertEquals(5, weekdayCount);
            assertEquals(2, weekendCount);
        }

        @Test
        @DisplayName("纯周末租赁")
        void pureWeekend_rental() {
            SimplePricingEngine engine = new SimplePricingEngine(new EmptyHolidayRepository());
            BigDecimal weekdayPrice = new BigDecimal("100");
            BigDecimal weekendPrice = new BigDecimal("150");
            // Sat Apr 18 to Mon Apr 20 = 2 days (Sat, Sun)
            LocalDate start = LocalDate.of(2026, 4, 18);
            LocalDate end = LocalDate.of(2026, 4, 20);

            PricingResult result = engine.calculate(weekdayPrice, weekendPrice, null, start, end);

            assertEquals(2, result.getDayPrices().size());
            assertEquals(new BigDecimal("300"), result.getTotalPrice());
            result.getDayPrices().forEach(dp -> {
                assertEquals("weekend", dp.getType());
                assertEquals(new BigDecimal("150"), dp.getPrice());
            });
        }
    }

    // ==================== 节假日场景 ====================

    @Nested
    @DisplayName("节假日差异化定价")
    class HolidayPricingTests {

        @Test
        @DisplayName("节假日使用 fixed_price（优先级 1）")
        void holiday_withFixedPrice() {
            Holiday holiday = new Holiday();
            holiday.setName("五一");
            holiday.setStartDate(LocalDate.of(2026, 5, 1));
            holiday.setEndDate(LocalDate.of(2026, 5, 5));
            holiday.setFixedPrice(new BigDecimal("500"));
            holiday.setPriceMultiplier(null);
            holiday.setYear(2026);

            SimplePricingEngine engine = new SimplePricingEngine(new SingleHolidayRepository(holiday));
            BigDecimal weekdayPrice = new BigDecimal("200");
            BigDecimal weekendPrice = new BigDecimal("300");
            LocalDate start = LocalDate.of(2026, 5, 1);
            LocalDate end = LocalDate.of(2026, 5, 3); // 3 days, all within holiday

            PricingResult result = engine.calculate(weekdayPrice, weekendPrice, null, start, end);

            assertEquals(2, result.getDayPrices().size());
            assertEquals(new BigDecimal("1000"), result.getTotalPrice()); // 2 * 500
            result.getDayPrices().forEach(dp -> {
                assertEquals("holiday", dp.getType());
                assertEquals(0, new BigDecimal("500").compareTo(dp.getPrice()));
            });
        }

        @Test
        @DisplayName("节假日使用 multiplier（优先级 2）: weekend_price * multiplier")
        void holiday_withMultiplier() {
            Holiday holiday = new Holiday();
            holiday.setName("国庆");
            holiday.setStartDate(LocalDate.of(2026, 10, 1));
            holiday.setEndDate(LocalDate.of(2026, 10, 7));
            holiday.setPriceMultiplier(new BigDecimal("1.5"));
            holiday.setFixedPrice(null);
            holiday.setYear(2026);

            SimplePricingEngine engine = new SimplePricingEngine(new SingleHolidayRepository(holiday));
            BigDecimal weekdayPrice = new BigDecimal("200");
            BigDecimal weekendPrice = new BigDecimal("300");
            // Oct 1-3 (Thu-Sat): 2 holidays within date range
            LocalDate start = LocalDate.of(2026, 10, 1);
            LocalDate end = LocalDate.of(2026, 10, 3);

            PricingResult result = engine.calculate(weekdayPrice, weekendPrice, null, start, end);

            assertEquals(2, result.getDayPrices().size());
            // Each day: weekendPrice(300) * 1.5 = 450, total = 900
            assertEquals(0, new BigDecimal("900").compareTo(result.getTotalPrice()));
            result.getDayPrices().forEach(dp -> {
                assertEquals("holiday", dp.getType());
                assertEquals(0, new BigDecimal("450").compareTo(dp.getPrice()));
            });
        }

        @Test
        @DisplayName("混合: 工作日 + 周末 + 节假日")
        void mixed_weekdayWeekendHoliday() {
            Holiday holiday = new Holiday();
            holiday.setName("五一");
            holiday.setStartDate(LocalDate.of(2026, 5, 1));
            holiday.setEndDate(LocalDate.of(2026, 5, 1)); // Just May 1st
            holiday.setFixedPrice(new BigDecimal("400"));
            holiday.setPriceMultiplier(null);
            holiday.setYear(2026);

            SimplePricingEngine engine = new SimplePricingEngine(new SingleHolidayRepository(holiday));
            BigDecimal weekdayPrice = new BigDecimal("200");
            BigDecimal weekendPrice = new BigDecimal("300");
            // Apr 30 (Thu) + May 1 (Fri, holiday) + May 2 (Sat) = 3 days
            LocalDate start = LocalDate.of(2026, 4, 30);
            LocalDate end = LocalDate.of(2026, 5, 3);

            PricingResult result = engine.calculate(weekdayPrice, weekendPrice, null, start, end);

            assertEquals(3, result.getDayPrices().size());
            // Apr 30 = weekday(200), May 1 = holiday(400), May 2 = weekend(300)
            assertEquals(0, new BigDecimal("900").compareTo(result.getTotalPrice()));
            assertEquals("weekday", result.getDayPrices().get(0).getType());
            assertEquals("holiday", result.getDayPrices().get(1).getType());
            assertEquals("weekend", result.getDayPrices().get(2).getType());
        }
    }

    // ==================== 边界条件 ====================

    @Nested
    @DisplayName("边界条件")
    class EdgeCaseTests {

        @Test
        @DisplayName("startDate == endDate (0 天): 空明细, 总价为 0")
        void zeroDays_sameDate() {
            SimplePricingEngine engine = new SimplePricingEngine(new EmptyHolidayRepository());
            BigDecimal weekdayPrice = new BigDecimal("100");
            BigDecimal weekendPrice = new BigDecimal("150");
            LocalDate date = LocalDate.of(2026, 4, 13);

            PricingResult result = engine.calculate(weekdayPrice, weekendPrice, null, date, date);

            assertTrue(result.getDayPrices().isEmpty());
            assertEquals(BigDecimal.ZERO, result.getTotalPrice());
        }

        @Test
        @DisplayName("startDate > endDate (负天数): 空循环, 总价为 0")
        void negativeDays_startAfterEnd() {
            SimplePricingEngine engine = new SimplePricingEngine(new EmptyHolidayRepository());
            BigDecimal weekdayPrice = new BigDecimal("100");
            BigDecimal weekendPrice = new BigDecimal("150");
            LocalDate start = LocalDate.of(2026, 4, 15);
            LocalDate end = LocalDate.of(2026, 4, 13);

            PricingResult result = engine.calculate(weekdayPrice, weekendPrice, null, start, end);

            assertTrue(result.getDayPrices().isEmpty());
            assertEquals(BigDecimal.ZERO, result.getTotalPrice());
        }

        @Test
        @DisplayName("无 weekendPrice 时周末回退到 weekdayPrice")
        void noWeekendPrice_fallbackToWeekday() {
            SimplePricingEngine engine = new SimplePricingEngine(new EmptyHolidayRepository());
            BigDecimal weekdayPrice = new BigDecimal("100");
            // Sat to Sun
            LocalDate start = LocalDate.of(2026, 4, 18);
            LocalDate end = LocalDate.of(2026, 4, 20);

            PricingResult result = engine.calculate(weekdayPrice, null, null, start, end);

            assertEquals(2, result.getDayPrices().size());
            // Without weekendPrice, weekends should fall back to weekdayPrice
            result.getDayPrices().forEach(dp ->
                assertEquals(new BigDecimal("100"), dp.getPrice())
            );
        }
    }

    // ==================== 测试替实现 ====================

    /**
     * 空节假日仓储 — 不返回任何节假日配置
     */
    static class EmptyHolidayRepository implements HolidayRepository {
        @Override public Optional<Holiday> findById(Long id) { return Optional.empty(); }
        @Override public List<Holiday> findByYear(Integer year) { return new ArrayList<>(); }
        @Override public List<Holiday> findAll() { return new ArrayList<>(); }
        @Override public List<Holiday> findOverlappingWith(LocalDate startDate, LocalDate endDate) { return new ArrayList<>(); }
        @Override public Holiday save(Holiday holiday) { return holiday; }
        @Override public List<Holiday> batchSave(List<Holiday> holidays) { return holidays; }
    }

    /**
     * 单节假日仓储 — 返回指定的单个节假日
     */
    static class SingleHolidayRepository implements HolidayRepository {
        private final Holiday holiday;

        SingleHolidayRepository(Holiday holiday) {
            this.holiday = holiday;
        }

        @Override public Optional<Holiday> findById(Long id) { return Optional.empty(); }
        @Override public List<Holiday> findByYear(Integer year) { return List.of(holiday); }
        @Override public List<Holiday> findAll() { return List.of(holiday); }
        @Override public List<Holiday> findOverlappingWith(LocalDate startDate, LocalDate endDate) {
            if (!holiday.getEndDate().isBefore(startDate) && !holiday.getStartDate().isAfter(endDate)) {
                return List.of(holiday);
            }
            return new ArrayList<>();
        }
        @Override public Holiday save(Holiday holiday) { return holiday; }
        @Override public List<Holiday> batchSave(List<Holiday> holidays) { return holidays; }
    }
}
