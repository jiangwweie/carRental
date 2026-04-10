package com.carrental.controller;

import com.carrental.common.result.ApiResponse;
import com.carrental.domain.order.Order;
import com.carrental.domain.order.OrderRepository;
import com.carrental.domain.order.service.OrderConflictChecker;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderConflictChecker conflictChecker;

    /**
     * 创建订单
     */
    @PostMapping
    public ApiResponse<CreateOrderResult> createOrder(
            @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");

        // 检查时间冲突
        conflictChecker.checkConflict(request.getVehicleId(), request.getStartDate(), request.getEndDate());

        // 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setVehicleId(request.getVehicleId());
        order.setStartDate(request.getStartDate());
        order.setEndDate(request.getEndDate());
        order.setDays((int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()));
        order.setStatus(com.carrental.domain.order.OrderStatus.PENDING);
        order.setPaymentStatus("unpaid");

        order = orderRepository.save(order);

        CreateOrderResult result = new CreateOrderResult();
        result.setOrderId(order.getId());
        result.setStartDate(order.getStartDate());
        result.setEndDate(order.getEndDate());
        result.setDays(order.getDays());
        result.setTotalPrice(order.getTotalPrice());

        return ApiResponse.success(result);
    }

    /**
     * 我的订单列表
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> myOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        List<Order> orders = orderRepository.findByUserId(userId, status, page, pageSize);
        long total = orderRepository.countByUserId(userId, status);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("items", orders.stream().map(this::toListDTO).collect(Collectors.toList()));

        return ApiResponse.success(result);
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Order> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return orderRepository.findById(id)
                .filter(order -> order.getUserId().equals(userId))
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(4004, "订单不存在"));
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancelOrder(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return orderRepository.findById(id)
                .filter(order -> order.getUserId().equals(userId))
                .map(order -> {
                    try {
                        order.cancel();
                        orderRepository.save(order);
                        Map<String, String> result = new HashMap<>();
                        result.put("status", "cancelled");
                        return ApiResponse.success("订单已取消", result);
                    } catch (IllegalStateException e) {
                        return ApiResponse.error(5300, e.getMessage());
                    }
                })
                .orElse(ApiResponse.error(4004, "订单不存在"));
    }

    private OrderListItemDTO toListDTO(Order order) {
        OrderListItemDTO dto = new OrderListItemDTO();
        dto.setId(order.getId());
        dto.setStartDate(order.getStartDate());
        dto.setEndDate(order.getEndDate());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus().name());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }

    @Data
    public static class CreateOrderRequest {
        private Long vehicleId;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class CreateOrderResult {
        private Long orderId;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer days;
        private java.math.BigDecimal totalPrice;
    }

    @Data
    public static class OrderListItemDTO {
        private Long id;
        private LocalDate startDate;
        private LocalDate endDate;
        private java.math.BigDecimal totalPrice;
        private String status;
        private java.time.LocalDateTime createdAt;
    }
}
