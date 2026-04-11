package com.carrental.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderStatus 枚举单元测试
 * 覆盖: fromValue() 安全转换 (正常值, null, 非法值, 大小写)
 */
class OrderStatusTest {

    @Nested
    @DisplayName("fromValue() 安全转换")
    class FromValueTests {

        @Test
        @DisplayName("正常值 - PENDING")
        void fromValue_validPending_returnsPending() {
            assertEquals(OrderStatus.PENDING, OrderStatus.fromValue("PENDING"));
        }

        @Test
        @DisplayName("正常值 - CONFIRMED")
        void fromValue_validConfirmed_returnsConfirmed() {
            assertEquals(OrderStatus.CONFIRMED, OrderStatus.fromValue("CONFIRMED"));
        }

        @Test
        @DisplayName("正常值 - IN_PROGRESS")
        void fromValue_validInProgress_returnsInProgress() {
            assertEquals(OrderStatus.IN_PROGRESS, OrderStatus.fromValue("IN_PROGRESS"));
        }

        @Test
        @DisplayName("正常值 - COMPLETED")
        void fromValue_validCompleted_returnsCompleted() {
            assertEquals(OrderStatus.COMPLETED, OrderStatus.fromValue("COMPLETED"));
        }

        @Test
        @DisplayName("正常值 - CANCELLED")
        void fromValue_validCancelled_returnsCancelled() {
            assertEquals(OrderStatus.CANCELLED, OrderStatus.fromValue("CANCELLED"));
        }

        @Test
        @DisplayName("正常值 - REJECTED")
        void fromValue_validRejected_returnsRejected() {
            assertEquals(OrderStatus.REJECTED, OrderStatus.fromValue("REJECTED"));
        }

        @Test
        @DisplayName("null 输入 - 返回 null 不抛异常")
        void fromValue_nullInput_returnsNull() {
            assertNull(OrderStatus.fromValue(null));
        }

        @Test
        @DisplayName("非法值 - 返回 null 不抛异常")
        void fromValue_invalidValue_returnsNull() {
            assertNull(OrderStatus.fromValue("UNKNOWN_STATUS"));
        }

        @Test
        @DisplayName("空字符串 - 返回 null")
        void fromValue_emptyString_returnsNull() {
            assertNull(OrderStatus.fromValue(""));
        }

        @Test
        @DisplayName("小写输入 - 忽略大小写匹配")
        void fromValue_lowercase_returnsMatchedStatus() {
            assertEquals(OrderStatus.PENDING, OrderStatus.fromValue("pending"));
            assertEquals(OrderStatus.CONFIRMED, OrderStatus.fromValue("confirmed"));
            assertEquals(OrderStatus.IN_PROGRESS, OrderStatus.fromValue("in_progress"));
        }

        @Test
        @DisplayName("混合大小写输入 - 忽略大小写匹配")
        void fromValue_mixedCase_returnsMatchedStatus() {
            assertEquals(OrderStatus.PENDING, OrderStatus.fromValue("Pending"));
            assertEquals(OrderStatus.COMPLETED, OrderStatus.fromValue("Completed"));
        }
    }

    @Nested
    @DisplayName("getLabel() 标签")
    class GetLabelTests {

        @Test
        @DisplayName("PENDING 标签为 待确认")
        void getLabel_pending() {
            assertEquals("待确认", OrderStatus.PENDING.getLabel());
        }

        @Test
        @DisplayName("CONFIRMED 标签为 已确认")
        void getLabel_confirmed() {
            assertEquals("已确认", OrderStatus.CONFIRMED.getLabel());
        }

        @Test
        @DisplayName("IN_PROGRESS 标签为 进行中")
        void getLabel_inProgress() {
            assertEquals("进行中", OrderStatus.IN_PROGRESS.getLabel());
        }

        @Test
        @DisplayName("COMPLETED 标签为 已完成")
        void getLabel_completed() {
            assertEquals("已完成", OrderStatus.COMPLETED.getLabel());
        }

        @Test
        @DisplayName("CANCELLED 标签为 已取消")
        void getLabel_cancelled() {
            assertEquals("已取消", OrderStatus.CANCELLED.getLabel());
        }

        @Test
        @DisplayName("REJECTED 标签为 已拒绝")
        void getLabel_rejected() {
            assertEquals("已拒绝", OrderStatus.REJECTED.getLabel());
        }
    }
}
