package com.carrental.infrastructure.pricing;

import com.carrental.domain.pricing.PricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimplePricingEngine 单元测试
 *
 * MVP 实现逻辑:
 *   - days = ChronoUnit.DAYS.between(startDate, endDate)
 *   - 每天均使用 weekdayPrice
 *   - totalPrice = days × weekdayPrice
 *   - 所有 dayPrice.type 均为 "weekday"
 */
class SimplePricingEngineTest {

    private SimplePricingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SimplePricingEngine();
    }

    // ==================== 正常场景 ====================

    @Nested
    @DisplayName("正常价格计算")
    class NormalCalculationTests {

        @Test
        @DisplayName("1 天租赁: 总价 = 1 × weekdayPrice")
        void oneDay_rental() {
            BigDecimal weekdayPrice = new BigDecimal("100");
            LocalDate start = LocalDate.of(2026, 4, 13);
            LocalDate end = LocalDate.of(2026, 4, 14);

            PricingResult result = engine.calculate(weekdayPrice, BigDecimal.ZERO, BigDecimal.ZERO, start, end);

            assertEquals(1, result.getDayPrices().size());
            assertEquals(new BigDecimal("100"), result.getTotalPrice());
            assertEquals("weekday", result.getDayPrices().get(0).getType());
        }

        @Test
        @DisplayName("3 天工作日租赁: 总价 = 3 × weekdayPrice")
        void threeDay_weekday_rental() {
            BigDecimal weekdayPrice = new BigDecimal("200");
            LocalDate start = LocalDate.of(2026, 4, 13); // Monday
            LocalDate end = LocalDate.of(2026, 4, 16);   // Thursday

            PricingResult result = engine.calculate(weekdayPrice, BigDecimal.ZERO, BigDecimal.ZERO, start, end);

            assertEquals(3, result.getDayPrices().size());
            assertEquals(new BigDecimal("600"), result.getTotalPrice());
            result.getDayPrices().forEach(dp -> {
                assertEquals("weekday", dp.getType());
                assertEquals(new BigDecimal("200"), dp.getPrice());
            });
        }

        @Test
        @DisplayName("7 天租赁: 总价 = 7 × weekdayPrice")
        void sevenDay_rental() {
            BigDecimal weekdayPrice = new BigDecimal("150");
            LocalDate start = LocalDate.of(2026, 4, 13);
            LocalDate end = LocalDate.of(2026, 4, 20);

            PricingResult result = engine.calculate(weekdayPrice, BigDecimal.ZERO, BigDecimal.ZERO, start, end);

            assertEquals(7, result.getDayPrices().size());
            assertEquals(new BigDecimal("1050"), result.getTotalPrice());
        }

        @Test
        @DisplayName("价格明细中的日期连续递增")
        void dates_areSequential() {
            BigDecimal weekdayPrice = new BigDecimal("100");
            LocalDate start = LocalDate.of(2026, 4, 13);
            LocalDate end = LocalDate.of(2026, 4, 16);

            PricingResult result = engine.calculate(weekdayPrice, BigDecimal.ZERO, BigDecimal.ZERO, start, end);

            assertEquals("2026-04-13", result.getDayPrices().get(0).getDate());
            assertEquals("2026-04-14", result.getDayPrices().get(1).getDate());
            assertEquals("2026-04-15", result.getDayPrices().get(2).getDate());
        }
    }

    // ==================== 边界条件 ====================

    @Nested
    @DisplayName("边界条件")
    class EdgeCaseTests {

        @Test
        @DisplayName("startDate == endDate (0 天): 空明细, 总价为 0")
        void zeroDays_sameDate() {
            BigDecimal weekdayPrice = new BigDecimal("100");
            LocalDate date = LocalDate.of(2026, 4, 13);

            PricingResult result = engine.calculate(weekdayPrice, BigDecimal.ZERO, BigDecimal.ZERO, date, date);

            assertTrue(result.getDayPrices().isEmpty());
            assertEquals(BigDecimal.ZERO, result.getTotalPrice());
        }

        @Test
        @DisplayName("startDate > endDate (负天数): 空循环, 总价为 0")
        void negativeDays_startAfterEnd() {
            BigDecimal weekdayPrice = new BigDecimal("100");
            LocalDate start = LocalDate.of(2026, 4, 15);
            LocalDate end = LocalDate.of(2026, 4, 13);

            PricingResult result = engine.calculate(weekdayPrice, BigDecimal.ZERO, BigDecimal.ZERO, start, end);

            assertTrue(result.getDayPrices().isEmpty());
            assertEquals(BigDecimal.ZERO, result.getTotalPrice());
        }
    }

    // ==================== MVP 特性验证 ====================

    @Nested
    @DisplayName("MVP 特性: 不区分周末/节假日")
    class MvpBehaviorTests {

        @Test
        @DisplayName("跨周末租赁: 所有天仍标记为 weekday (MVP 行为)")
        void weekendDays_stillWeekdayType() {
            BigDecimal weekdayPrice = new BigDecimal("100");
            // Saturday to Monday: covers weekend
            LocalDate start = LocalDate.of(2026, 4, 17); // Friday
            LocalDate end = LocalDate.of(2026, 4, 20);   // Monday

            PricingResult result = engine.calculate(weekdayPrice, BigDecimal.ZERO, BigDecimal.ZERO, start, end);

            assertEquals(3, result.getDayPrices().size());
            // MVP: 即使是周末, type 也是 weekday
            result.getDayPrices().forEach(dp -> assertEquals("weekday", dp.getType()));
            assertEquals(new BigDecimal("300"), result.getTotalPrice());
        }

        @Test
        @DisplayName("weekendPrice 和 holidayPrice 参数被忽略")
        void weekendAndHolidayPrices_areIgnored() {
            BigDecimal weekdayPrice = new BigDecimal("100");
            BigDecimal weekendPrice = new BigDecimal("999");
            BigDecimal holidayPrice = new BigDecimal("888");
            LocalDate start = LocalDate.of(2026, 4, 13);
            LocalDate end = LocalDate.of(2026, 4, 16);

            PricingResult result = engine.calculate(weekdayPrice, weekendPrice, holidayPrice, start, end);

            // 结果只应受 weekdayPrice 影响
            assertEquals(new BigDecimal("300"), result.getTotalPrice());
            result.getDayPrices().forEach(dp ->
                assertEquals(new BigDecimal("100"), dp.getPrice())
            );
        }
    }
}
