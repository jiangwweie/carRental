package com.carrental.domain.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Order 领域模型状态机单元测试
 *
 * 合法流转: PENDING → CONFIRMED → IN_PROGRESS → COMPLETED
 *          PENDING → CANCELLED
 *          PENDING → REJECTED
 *
 * 非法流转: 任何非相邻状态的跳转均应抛 IllegalStateException
 * 重复操作: 同一终态上的重复操作应抛异常
 */
class OrderStateMachineTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setUserId(100L);
        order.setVehicleId(200L);
        order.setStatus(OrderStatus.PENDING);
    }

    // ==================== 合法路径测试 ====================

    @Nested
    @DisplayName("合法状态流转")
    class HappyPathTests {

        @Test
        @DisplayName("完整流程: PENDING → CONFIRMED → IN_PROGRESS → COMPLETED")
        void fullHappyPath() {
            order.confirm();
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());

            order.start();
            assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());

            order.complete();
            assertEquals(OrderStatus.COMPLETED, order.getStatus());
        }

        @Test
        @DisplayName("PENDING → CANCELLED")
        void pendingToCancelled() {
            order.cancel();
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }

        @Test
        @DisplayName("PENDING → REJECTED")
        void pendingToRejected() {
            order.reject(null);
            assertEquals(OrderStatus.REJECTED, order.getStatus());
        }
    }

    // ==================== confirm() 方法 ====================

    @Nested
    @DisplayName("confirm() 行为")
    class ConfirmTests {

        @Test
        @DisplayName("PENDING 状态可以确认")
        void confirm_fromPending_succeeds() {
            order.confirm();
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        }

        @Test
        @DisplayName("CONFIRMED 状态再次确认 - 抛异常")
        void confirm_fromConfirmed_throws() {
            order.confirm(); // 先确认
            assertThrows(IllegalStateException.class, () -> order.confirm());
        }

        @Test
        @DisplayName("CANCELLED 状态确认 - 抛异常")
        void confirm_fromCancelled_throws() {
            order.cancel();
            assertThrows(IllegalStateException.class, () -> order.confirm());
        }

        @Test
        @DisplayName("REJECTED 状态确认 - 抛异常")
        void confirm_fromRejected_throws() {
            order.reject(null);
            assertThrows(IllegalStateException.class, () -> order.confirm());
        }

        @Test
        @DisplayName("IN_PROGRESS 状态确认 - 抛异常")
        void confirm_fromInProgress_throws() {
            order.confirm();
            order.start();
            assertThrows(IllegalStateException.class, () -> order.confirm());
        }

        @Test
        @DisplayName("COMPLETED 状态确认 - 抛异常")
        void confirm_fromCompleted_throws() {
            order.confirm();
            order.start();
            order.complete();
            assertThrows(IllegalStateException.class, () -> order.confirm());
        }
    }

    // ==================== cancel() 方法 ====================

    @Nested
    @DisplayName("cancel() 行为")
    class CancelTests {

        @Test
        @DisplayName("PENDING 状态可以取消")
        void cancel_fromPending_succeeds() {
            order.cancel();
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }

        @Test
        @DisplayName("CANCELLED 状态再次取消 - 抛异常")
        void cancel_fromCancelled_throws() {
            order.cancel();
            assertThrows(IllegalStateException.class, () -> order.cancel());
        }

        @Test
        @DisplayName("CONFIRMED 状态取消 - 抛异常")
        void cancel_fromConfirmed_throws() {
            order.confirm();
            assertThrows(IllegalStateException.class, () -> order.cancel());
        }

        @Test
        @DisplayName("IN_PROGRESS 状态取消 - 抛异常")
        void cancel_fromInProgress_throws() {
            order.confirm();
            order.start();
            assertThrows(IllegalStateException.class, () -> order.cancel());
        }

        @Test
        @DisplayName("REJECTED 状态取消 - 抛异常")
        void cancel_fromRejected_throws() {
            order.reject(null);
            assertThrows(IllegalStateException.class, () -> order.cancel());
        }
    }

    // ==================== reject() 方法 ====================

    @Nested
    @DisplayName("reject() 行为")
    class RejectTests {

        @Test
        @DisplayName("PENDING 状态可以拒绝")
        void reject_fromPending_succeeds() {
            order.reject(null);
            assertEquals(OrderStatus.REJECTED, order.getStatus());
        }

        @Test
        @DisplayName("REJECTED 状态再次拒绝 - 抛异常")
        void reject_fromRejected_throws() {
            order.reject(null);
            assertThrows(IllegalStateException.class, () -> order.reject(null));
        }

        @Test
        @DisplayName("CANCELLED 状态拒绝 - 抛异常")
        void reject_fromCancelled_throws() {
            order.cancel();
            assertThrows(IllegalStateException.class, () -> order.reject(null));
        }

        @Test
        @DisplayName("CONFIRMED 状态拒绝 - 抛异常")
        void reject_fromConfirmed_throws() {
            order.confirm();
            assertThrows(IllegalStateException.class, () -> order.reject(null));
        }
    }

    // ==================== start() 方法 ====================

    @Nested
    @DisplayName("start() 行为")
    class StartTests {

        @Test
        @DisplayName("CONFIRMED 状态可以开始")
        void start_fromConfirmed_succeeds() {
            order.confirm();
            order.start();
            assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
        }

        @Test
        @DisplayName("PENDING 状态直接开始 - 抛异常 (非法跳转)")
        void start_fromPending_throws() {
            assertThrows(IllegalStateException.class, () -> order.start());
        }

        @Test
        @DisplayName("IN_PROGRESS 状态再次开始 - 抛异常")
        void start_fromInProgress_throws() {
            order.confirm();
            order.start();
            assertThrows(IllegalStateException.class, () -> order.start());
        }

        @Test
        @DisplayName("COMPLETED 状态开始 - 抛异常")
        void start_fromCompleted_throws() {
            order.confirm();
            order.start();
            order.complete();
            assertThrows(IllegalStateException.class, () -> order.start());
        }

        @Test
        @DisplayName("CANCELLED 状态开始 - 抛异常")
        void start_fromCancelled_throws() {
            order.cancel();
            assertThrows(IllegalStateException.class, () -> order.start());
        }
    }

    // ==================== complete() 方法 ====================

    @Nested
    @DisplayName("complete() 行为")
    class CompleteTests {

        @Test
        @DisplayName("IN_PROGRESS 状态可以完成")
        void complete_fromInProgress_succeeds() {
            order.confirm();
            order.start();
            order.complete();
            assertEquals(OrderStatus.COMPLETED, order.getStatus());
        }

        @Test
        @DisplayName("PENDING 状态直接完成 - 抛异常 (跳过多个状态)")
        void complete_fromPending_throws() {
            assertThrows(IllegalStateException.class, () -> order.complete());
        }

        @Test
        @DisplayName("CONFIRMED 状态直接完成 - 抛异常 (跳过 IN_PROGRESS)")
        void complete_fromConfirmed_throws() {
            order.confirm();
            assertThrows(IllegalStateException.class, () -> order.complete());
        }

        @Test
        @DisplayName("COMPLETED 状态再次完成 - 抛异常")
        void complete_fromCompleted_throws() {
            order.confirm();
            order.start();
            order.complete();
            assertThrows(IllegalStateException.class, () -> order.complete());
        }

        @Test
        @DisplayName("CANCELLED 状态完成 - 抛异常")
        void complete_fromCancelled_throws() {
            order.cancel();
            assertThrows(IllegalStateException.class, () -> order.complete());
        }
    }

    // ==================== 跨状态非法跳转 ====================

    @Nested
    @DisplayName("跨状态非法跳转")
    class IllegalTransitionTests {

        @Test
        @DisplayName("PENDING → IN_PROGRESS (跳过 CONFIRMED)")
        void pendingToInProgress_illegal() {
            assertThrows(IllegalStateException.class, () -> order.start());
        }

        @Test
        @DisplayName("PENDING → COMPLETED (跳过多个状态)")
        void pendingToCompleted_illegal() {
            assertThrows(IllegalStateException.class, () -> order.complete());
        }

        @Test
        @DisplayName("CONFIRMED → COMPLETED (跳过 IN_PROGRESS)")
        void confirmedToCompleted_illegal() {
            order.confirm();
            assertThrows(IllegalStateException.class, () -> order.complete());
        }

        @Test
        @DisplayName("REJECTED 状态不能进行任何后续操作")
        void rejected_isTerminalState() {
            order.reject(null);
            assertThrows(IllegalStateException.class, () -> order.confirm());
            assertThrows(IllegalStateException.class, () -> order.cancel());
            assertThrows(IllegalStateException.class, () -> order.reject(null));
            assertThrows(IllegalStateException.class, () -> order.start());
            assertThrows(IllegalStateException.class, () -> order.complete());
        }

        @Test
        @DisplayName("CANCELLED 状态不能进行任何后续操作")
        void cancelled_isTerminalState() {
            order.cancel();
            assertThrows(IllegalStateException.class, () -> order.confirm());
            assertThrows(IllegalStateException.class, () -> order.cancel());
            assertThrows(IllegalStateException.class, () -> order.reject(null));
            assertThrows(IllegalStateException.class, () -> order.start());
            assertThrows(IllegalStateException.class, () -> order.complete());
        }
    }
}
