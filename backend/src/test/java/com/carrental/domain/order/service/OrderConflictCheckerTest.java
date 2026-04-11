package com.carrental.domain.order.service;

import com.carrental.common.exception.BusinessException;
import com.carrental.common.result.ErrorCode;
import com.carrental.domain.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderConflictChecker 单元测试
 *
 * 由于约束"不使用 Mockito"，此处使用内嵌的 InMemoryOrderRepositoryStub
 * 模拟 OrderRepository 的 hasConflict() 行为。
 *
 * 冲突检测逻辑: 同一辆车在时间段重叠时返回 true
 */
class OrderConflictCheckerTest {

    private InMemoryOrderRepositoryStub repository;
    private OrderConflictChecker checker;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepositoryStub();
        checker = new OrderConflictChecker(repository);
    }

    // ==================== 不冲突场景 ====================

    @Nested
    @DisplayName("时间段不重叠 - 通过")
    class NoConflictTests {

        @Test
        @DisplayName("新订单在已有订单之前 - 不冲突")
        void newOrderBeforeExisting_noConflict() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 25));

            // 新订单: 4/10 - 4/15，在已有订单之前
            assertDoesNotThrow(() ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 15))
            );
        }

        @Test
        @DisplayName("新订单在已有订单之后 - 不冲突")
        void newOrderAfterExisting_noConflict() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 15));

            // 新订单: 4/20 - 4/25，在已有订单之后
            assertDoesNotThrow(() ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 25))
            );
        }

        @Test
        @DisplayName("新订单紧接已有订单 (end == start) - 不冲突")
        void newOrderAdjacent_noConflict() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 15));

            // 新订单: 4/15 - 4/20，起始日等于已有订单结束日（边界不重叠）
            assertDoesNotThrow(() ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 20))
            );
        }

        @Test
        @DisplayName("不同车辆 - 不冲突")
        void differentVehicle_noConflict() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20));

            // 不同车辆，同一时间段
            assertDoesNotThrow(() ->
                checker.checkConflict(300L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20))
            );
        }

        @Test
        @DisplayName("无已有订单 - 不冲突")
        void noExistingOrders_noConflict() {
            assertDoesNotThrow(() ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 15))
            );
        }
    }

    // ==================== 冲突场景 ====================

    @Nested
    @DisplayName("时间段完全重叠 - 冲突")
    class FullOverlapTests {

        @Test
        @DisplayName("完全相同的时间段 - 冲突")
        void exactSamePeriod_conflict() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20))
            );
            assertEquals(ErrorCode.TIME_SLOT_CONFLICT.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("时间段部分重叠 - 冲突")
    class PartialOverlapTests {

        @Test
        @DisplayName("新订单起始重叠 (新订单在已有订单内部开始)")
        void newOrderStartsInsideExisting_conflict() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20));

            // 新订单: 4/15 - 4/25，与 4/10-4/20 部分重叠
            assertThrows(BusinessException.class, () ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 25))
            );
        }

        @Test
        @DisplayName("新订单结束重叠 (新订单在已有订单内部结束)")
        void newOrderEndsInsideExisting_conflict() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 25));

            // 新订单: 4/10 - 4/20，与 4/15-4/25 部分重叠
            assertThrows(BusinessException.class, () ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20))
            );
        }

        @Test
        @DisplayName("新订单完全包含已有订单 - 冲突")
        void newOrderContainsExisting_conflict() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 17));

            // 新订单: 4/10 - 4/20，完全包含已有订单
            assertThrows(BusinessException.class, () ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20))
            );
        }

        @Test
        @DisplayName("新订单被已有订单完全包含 - 冲突")
        void newOrderInsideExisting_conflict() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20));

            // 新订单: 4/13 - 4/17，被已有订单完全包含
            assertThrows(BusinessException.class, () ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 13), LocalDate.of(2026, 4, 17))
            );
        }
    }

    // ==================== 用户无关性 ====================

    @Nested
    @DisplayName("冲突检测与用户无关")
    class UserIndependenceTests {

        @Test
        @DisplayName("同一车辆不同用户 - 仍应冲突")
        void sameVehicleDifferentUser_stillConflicts() {
            // 模拟: 用户 100 已预订车辆 200
            repository.addBooking(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20));

            // 用户 999 尝试预订同一车辆同一时间段 → 仍冲突
            assertThrows(BusinessException.class, () ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 25))
            );
        }
    }

    // ==================== 异常类型和错误码 ====================

    @Nested
    @DisplayName("异常类型和错误码")
    class ExceptionTypeTests {

        @Test
        @DisplayName("冲突时抛出 BusinessException")
        void conflict_throwsBusinessException() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20));

            Throwable thrown = assertThrows(Throwable.class, () ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20))
            );
            assertInstanceOf(BusinessException.class, thrown);
        }

        @Test
        @DisplayName("错误码为 TIME_SLOT_CONFLICT (5200)")
        void conflict_errorCodeIsTimeSlotConflict() {
            repository.addBooking(200L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                checker.checkConflict(200L, LocalDate.of(2026, 4, 12), LocalDate.of(2026, 4, 18))
            );
            assertEquals(5200, ex.getCode());
            assertEquals("该时间段已被预订", ex.getMessage());
        }
    }

    /**
     * 内存中的 OrderRepository 测试替
     * 仅实现 hasConflict() 方法，其他方法返回空值
     * 时间重叠判断逻辑: newStart < existingEnd && newEnd > existingStart
     */
    private static class InMemoryOrderRepositoryStub implements OrderRepository {

        private record Booking(Long vehicleId, LocalDate startDate, LocalDate endDate) {}

        private final List<Booking> bookings = new ArrayList<>();

        void addBooking(Long vehicleId, LocalDate startDate, LocalDate endDate) {
            bookings.add(new Booking(vehicleId, startDate, endDate));
        }

        @Override
        public boolean hasConflict(Long vehicleId, LocalDate startDate, LocalDate endDate, Long excludeOrderId) {
            return bookings.stream()
                    .filter(b -> b.vehicleId().equals(vehicleId))
                    .anyMatch(b ->
                            startDate.isBefore(b.endDate()) && endDate.isAfter(b.startDate())
                    );
        }

        // ===== 以下方法在冲突检测测试中不需要实现 =====
        @Override public java.util.Optional<com.carrental.domain.order.Order> findById(Long id) { return java.util.Optional.empty(); }
        @Override public com.carrental.domain.order.Order save(com.carrental.domain.order.Order order) { return order; }
        @Override public java.util.List<com.carrental.domain.order.Order> findByUserId(Long userId, String status, int page, int pageSize) { return java.util.List.of(); }
        @Override public long countByUserId(Long userId, String status) { return 0; }
        @Override public java.util.List<com.carrental.domain.order.Order> findAdminOrders(String status, Long vehicleId, int page, int pageSize) { return java.util.List.of(); }
        @Override public long countAdminOrders(String status, Long vehicleId) { return 0; }
    }
}
