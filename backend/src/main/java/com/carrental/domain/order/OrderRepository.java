package com.carrental.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Optional<Order> findById(Long id);

    Order save(Order order);

    List<Order> findByUserId(Long userId, String status, int page, int pageSize);

    long countByUserId(Long userId, String status);

    List<Order> findAdminOrders(String status, Long vehicleId, int page, int pageSize);

    long countAdminOrders(String status, Long vehicleId);

    boolean hasConflict(Long vehicleId, java.time.LocalDate startDate, java.time.LocalDate endDate, Long excludeOrderId);
}
