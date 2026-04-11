package com.carrental.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.domain.order.Order;
import com.carrental.domain.order.OrderRepository;
import com.carrental.domain.order.OrderStatus;
import com.carrental.infrastructure.persistence.dataobject.OrderDO;
import com.carrental.infrastructure.persistence.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper orderMapper;

    @Override
    public Optional<Order> findById(Long id) {
        OrderDO orderDO = orderMapper.selectById(id);
        return Optional.ofNullable(orderDO).map(this::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderDO orderDO = toDO(order);
        if (order.getId() == null) {
            orderMapper.insert(orderDO);
            order.setId(orderDO.getId());
        } else {
            orderMapper.updateById(orderDO);
        }
        return order;
    }

    @Override
    public List<Order> findByUserId(Long userId, String status, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<OrderDO> orderDOs = orderMapper.selectByUserId(userId, status, offset, pageSize);
        return orderDOs.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId, String status) {
        return orderMapper.countByUserId(userId, status);
    }

    @Override
    public List<Order> findAdminOrders(String status, Long vehicleId, int page, int pageSize) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(OrderDO::getStatus, status);
        }
        if (vehicleId != null) {
            wrapper.eq(OrderDO::getVehicleId, vehicleId);
        }
        wrapper.orderByDesc(OrderDO::getCreatedAt);
        wrapper.last("LIMIT " + pageSize + " OFFSET " + ((page - 1) * pageSize));
        List<OrderDO> orderDOs = orderMapper.selectList(wrapper);
        return orderDOs.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countAdminOrders(String status, Long vehicleId) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(OrderDO::getStatus, status);
        }
        if (vehicleId != null) {
            wrapper.eq(OrderDO::getVehicleId, vehicleId);
        }
        return orderMapper.selectCount(wrapper);
    }

    @Override
    public boolean hasConflict(Long vehicleId, LocalDate startDate, LocalDate endDate, Long excludeOrderId) {
        long count = orderMapper.countConflicts(vehicleId, startDate, endDate);
        if (excludeOrderId != null && count > 0) {
            // 如果排除后没有冲突，则不算冲突
            LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDO::getVehicleId, vehicleId)
                    .in(OrderDO::getStatus, "pending", "confirmed", "in_progress")
                    .lt(OrderDO::getStartDate, endDate)
                    .gt(OrderDO::getEndDate, startDate)
                    .eq(OrderDO::getId, excludeOrderId);
            return orderMapper.selectCount(wrapper) < count;
        }
        return count > 0;
    }

    private Order toDomain(OrderDO orderDO) {
        Order order = new Order();
        order.setId(orderDO.getId());
        order.setUserId(orderDO.getUserId());
        order.setVehicleId(orderDO.getVehicleId());
        order.setStartDate(orderDO.getStartDate());
        order.setEndDate(orderDO.getEndDate());
        order.setDays(orderDO.getDays());
        order.setTotalPrice(orderDO.getTotalPrice());
        order.setPriceBreakdown(orderDO.getPriceBreakdown());
        order.setStatus(OrderStatus.valueOf(orderDO.getStatus()));
        order.setPaymentStatus(orderDO.getPaymentStatus());
        order.setPaymentId(orderDO.getPaymentId());
        order.setPaidAt(orderDO.getPaidAt());
        order.setRejectReason(orderDO.getRejectReason());
        order.setCreatedAt(orderDO.getCreatedAt());
        order.setUpdatedAt(orderDO.getUpdatedAt());
        return order;
    }

    private OrderDO toDO(Order order) {
        OrderDO orderDO = new OrderDO();
        orderDO.setId(order.getId());
        orderDO.setUserId(order.getUserId());
        orderDO.setVehicleId(order.getVehicleId());
        orderDO.setStartDate(order.getStartDate());
        orderDO.setEndDate(order.getEndDate());
        orderDO.setDays(order.getDays());
        orderDO.setTotalPrice(order.getTotalPrice());
        orderDO.setPriceBreakdown(order.getPriceBreakdown());
        orderDO.setStatus(order.getStatus().name());
        orderDO.setPaymentStatus(order.getPaymentStatus());
        orderDO.setPaymentId(order.getPaymentId());
        orderDO.setPaidAt(order.getPaidAt());
        orderDO.setRejectReason(order.getRejectReason());
        return orderDO;
    }
}
