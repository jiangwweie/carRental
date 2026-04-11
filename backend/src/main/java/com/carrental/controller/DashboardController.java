package com.carrental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.common.result.ApiResponse;
import com.carrental.domain.order.OrderRepository;
import com.carrental.domain.vehicle.VehicleRepository;
import com.carrental.infrastructure.persistence.dataobject.OrderDO;
import com.carrental.infrastructure.persistence.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OrderMapper orderMapper;
    private final VehicleRepository vehicleRepository;

    /**
     * 仪表盘概览
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        // 今日订单
        LambdaQueryWrapper<OrderDO> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(OrderDO::getCreatedAt, startOfDay).lt(OrderDO::getCreatedAt, endOfDay);
        long todayOrders = orderMapper.selectCount(todayWrapper);

        // 今日收入
        LambdaQueryWrapper<OrderDO> todayPaidWrapper = new LambdaQueryWrapper<>();
        todayPaidWrapper.ge(OrderDO::getCreatedAt, startOfDay)
                .lt(OrderDO::getCreatedAt, endOfDay)
                .eq(OrderDO::getStatus, "completed");
        BigDecimal todayRevenue = orderMapper.selectList(todayPaidWrapper).stream()
                .map(OrderDO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 本月订单
        LambdaQueryWrapper<OrderDO> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.ge(OrderDO::getCreatedAt, startOfMonth);
        long monthOrders = orderMapper.selectCount(monthWrapper);

        // 本月收入（已完成订单的总收入）
        LambdaQueryWrapper<OrderDO> monthPaidWrapper = new LambdaQueryWrapper<>();
        monthPaidWrapper.ge(OrderDO::getCreatedAt, startOfMonth)
                .eq(OrderDO::getStatus, "completed");
        BigDecimal monthRevenue = orderMapper.selectList(monthPaidWrapper).stream()
                .map(OrderDO::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 进行中订单
        LambdaQueryWrapper<OrderDO> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(OrderDO::getStatus, "in_progress");
        long activeOrders = orderMapper.selectCount(activeWrapper);

        // 可租车辆
        long availableVehicles = vehicleRepository.countActiveVehicles();

        Map<String, Object> result = new HashMap<>();
        result.put("today_orders", todayOrders);
        result.put("today_revenue", todayRevenue);
        result.put("month_orders", monthOrders);
        result.put("month_revenue", monthRevenue);
        result.put("active_orders", activeOrders);
        result.put("available_vehicles", availableVehicles);

        return ApiResponse.success(result);
    }
}
