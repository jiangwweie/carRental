package com.carrental.domain.holiday;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Holiday 领域对象单元测试
 * 覆盖: calculatePrice() 四个优先级, covers() 边界, overlapsWith() 重叠/不重叠场景
 */
class HolidayTest {

    @Nested
    @DisplayName("calculatePrice() 四档优先级")
    class CalculatePriceTests {

        private final BigDecimal weekdayPrice = new BigDecimal("100");
        private final BigDecimal weekendPrice = new BigDecimal("150");

        @Test
        @DisplayName("优先级 1: fixedPrice 非空，直接返回固定价格")
        void priority1_fixedPrice_returnsFixedPrice() {
            Holiday holiday = new Holiday();
            holiday.setFixedPrice(new BigDecimal("300"));
            holiday.setPriceMultiplier(new BigDecimal("2.0"));

            BigDecimal result = holiday.calculatePrice(weekdayPrice, weekendPrice);

            assertEquals(new BigDecimal("300"), result);
        }

        @Test
        @DisplayName("优先级 2: weekendPrice * multiplier，无 fixedPrice")
        void priority2_weekendPriceMultiplier_returnsMultipliedPrice() {
            Holiday holiday = new Holiday();
            holiday.setPriceMultiplier(new BigDecimal("1.5"));
            // fixedPrice 为 null

            BigDecimal result = holiday.calculatePrice(weekdayPrice, weekendPrice);

            assertEquals(0, new BigDecimal("225.00").compareTo(result));
        }

        @Test
        @DisplayName("优先级 3: weekendPrice 兜底，multiplier 为 null")
        void priority3_weekendPrice_only_returnsWeekendPrice() {
            Holiday holiday = new Holiday();
            holiday.setPriceMultiplier(null);
            // fixedPrice 为 null

            BigDecimal result = holiday.calculatePrice(weekdayPrice, weekendPrice);

            assertEquals(new BigDecimal("150"), result);
        }

        @Test
        @DisplayName("优先级 4: weekdayPrice 兜底，weekendPrice 和 multiplier 都为 null")
        void priority4_weekdayPrice_fallback_returnsWeekdayPrice() {
            Holiday holiday = new Holiday();
            holiday.setPriceMultiplier(null);
            // fixedPrice 为 null

            BigDecimal result = holiday.calculatePrice(weekdayPrice, null);

            assertEquals(new BigDecimal("100"), result);
        }
    }

    @Nested
    @DisplayName("covers() 日期范围覆盖判断")
    class CoversTests {

        @Test
        @DisplayName("开始日期当天 - 返回 true")
        void covers_startDate_returnsTrue() {
            Holiday holiday = new Holiday();
            holiday.setStartDate(LocalDate.of(2026, 1, 1));
            holiday.setEndDate(LocalDate.of(2026, 1, 3));

            assertTrue(holiday.covers(LocalDate.of(2026, 1, 1)));
        }

        @Test
        @DisplayName("结束日期当天 - 返回 true")
        void covers_endDate_returnsTrue() {
            Holiday holiday = new Holiday();
            holiday.setStartDate(LocalDate.of(2026, 1, 1));
            holiday.setEndDate(LocalDate.of(2026, 1, 3));

            assertTrue(holiday.covers(LocalDate.of(2026, 1, 3)));
        }

        @Test
        @DisplayName("中间日期 - 返回 true")
        void covers_middleDate_returnsTrue() {
            Holiday holiday = new Holiday();
            holiday.setStartDate(LocalDate.of(2026, 1, 1));
            holiday.setEndDate(LocalDate.of(2026, 1, 3));

            assertTrue(holiday.covers(LocalDate.of(2026, 1, 2)));
        }

        @Test
        @DisplayName("开始日期前一天 - 返回 false")
        void covers_beforeStartDate_returnsFalse() {
            Holiday holiday = new Holiday();
            holiday.setStartDate(LocalDate.of(2026, 1, 1));
            holiday.setEndDate(LocalDate.of(2026, 1, 3));

            assertFalse(holiday.covers(LocalDate.of(2025, 12, 31)));
        }

        @Test
        @DisplayName("结束日期后一天 - 返回 false")
        void covers_afterEndDate_returnsFalse() {
            Holiday holiday = new Holiday();
            holiday.setStartDate(LocalDate.of(2026, 1, 1));
            holiday.setEndDate(LocalDate.of(2026, 1, 3));

            assertFalse(holiday.covers(LocalDate.of(2026, 1, 4)));
        }

        @Test
        @DisplayName("单日节假日 - 开始和结束同一天")
        void covers_singleDayHoliday() {
            Holiday holiday = new Holiday();
            holiday.setStartDate(LocalDate.of(2026, 5, 1));
            holiday.setEndDate(LocalDate.of(2026, 5, 1));

            assertTrue(holiday.covers(LocalDate.of(2026, 5, 1)));
            assertFalse(holiday.covers(LocalDate.of(2026, 4, 30)));
            assertFalse(holiday.covers(LocalDate.of(2026, 5, 2)));
        }
    }

    @Nested
    @DisplayName("overlapsWith() 日期范围重叠判断")
    class OverlapsWithTests {

        @Test
        @DisplayName("完全重叠 - 返回 true")
        void overlaps_completeOverlap_returnsTrue() {
            Holiday h1 = new Holiday();
            h1.setStartDate(LocalDate.of(2026, 1, 1));
            h1.setEndDate(LocalDate.of(2026, 1, 7));

            Holiday h2 = new Holiday();
            h2.setStartDate(LocalDate.of(2026, 1, 1));
            h2.setEndDate(LocalDate.of(2026, 1, 7));

            assertTrue(h1.overlapsWith(h2));
        }

        @Test
        @DisplayName("部分重叠（h2 在 h1 内部）- 返回 true")
        void overlaps_partialInside_returnsTrue() {
            Holiday h1 = new Holiday();
            h1.setStartDate(LocalDate.of(2026, 1, 1));
            h1.setEndDate(LocalDate.of(2026, 1, 10));

            Holiday h2 = new Holiday();
            h2.setStartDate(LocalDate.of(2026, 1, 5));
            h2.setEndDate(LocalDate.of(2026, 1, 7));

            assertTrue(h1.overlapsWith(h2));
        }

        @Test
        @DisplayName("部分重叠（h2 跨越 h1 尾部）- 返回 true")
        void overlaps_partialTail_returnsTrue() {
            Holiday h1 = new Holiday();
            h1.setStartDate(LocalDate.of(2026, 1, 1));
            h1.setEndDate(LocalDate.of(2026, 1, 5));

            Holiday h2 = new Holiday();
            h2.setStartDate(LocalDate.of(2026, 1, 3));
            h2.setEndDate(LocalDate.of(2026, 1, 8));

            assertTrue(h1.overlapsWith(h2));
        }

        @Test
        @DisplayName("部分重叠（h2 跨越 h1 头部）- 返回 true")
        void overlaps_partialHead_returnsTrue() {
            Holiday h1 = new Holiday();
            h1.setStartDate(LocalDate.of(2026, 1, 5));
            h1.setEndDate(LocalDate.of(2026, 1, 10));

            Holiday h2 = new Holiday();
            h2.setStartDate(LocalDate.of(2026, 1, 1));
            h2.setEndDate(LocalDate.of(2026, 1, 6));

            assertTrue(h1.overlapsWith(h2));
        }

        @Test
        @DisplayName("边界接触（h2 开始 = h1 结束）- 返回 true")
        void overlaps_boundaryTouch_returnsTrue() {
            Holiday h1 = new Holiday();
            h1.setStartDate(LocalDate.of(2026, 1, 1));
            h1.setEndDate(LocalDate.of(2026, 1, 5));

            Holiday h2 = new Holiday();
            h2.setStartDate(LocalDate.of(2026, 1, 5));
            h2.setEndDate(LocalDate.of(2026, 1, 10));

            assertTrue(h1.overlapsWith(h2));
        }

        @Test
        @DisplayName("完全不重叠（h2 在 h1 之前）- 返回 false")
        void noOverlap_before_returnsFalse() {
            Holiday h1 = new Holiday();
            h1.setStartDate(LocalDate.of(2026, 1, 10));
            h1.setEndDate(LocalDate.of(2026, 1, 15));

            Holiday h2 = new Holiday();
            h2.setStartDate(LocalDate.of(2026, 1, 1));
            h2.setEndDate(LocalDate.of(2026, 1, 5));

            assertFalse(h1.overlapsWith(h2));
        }

        @Test
        @DisplayName("完全不重叠（h2 在 h1 之后）- 返回 false")
        void noOverlap_after_returnsFalse() {
            Holiday h1 = new Holiday();
            h1.setStartDate(LocalDate.of(2026, 1, 1));
            h1.setEndDate(LocalDate.of(2026, 1, 5));

            Holiday h2 = new Holiday();
            h2.setStartDate(LocalDate.of(2026, 1, 10));
            h2.setEndDate(LocalDate.of(2026, 1, 15));

            assertFalse(h1.overlapsWith(h2));
        }

        @Test
        @DisplayName("边界不接触（h2 开始 = h1 结束 + 1 天）- 返回 false")
        void noOverlap_boundaryAdjacent_returnsFalse() {
            Holiday h1 = new Holiday();
            h1.setStartDate(LocalDate.of(2026, 1, 1));
            h1.setEndDate(LocalDate.of(2026, 1, 5));

            Holiday h2 = new Holiday();
            h2.setStartDate(LocalDate.of(2026, 1, 6));
            h2.setEndDate(LocalDate.of(2026, 1, 10));

            assertFalse(h1.overlapsWith(h2));
        }
    }
}
