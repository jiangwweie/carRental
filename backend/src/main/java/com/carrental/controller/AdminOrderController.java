package com.carrental.controller;

import com.carrental.common.result.ApiResponse;
import com.carrental.domain.order.Order;
import com.carrental.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;

    /**
     * 订单列表
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        List<Order> orders = orderRepository.findAdminOrders(status, vehicleId, page, pageSize);
        long total = orderRepository.countAdminOrders(status, vehicleId);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("items", orders.stream().map(this::toAdminDTO).collect(Collectors.toList()));

        return ApiResponse.success(result);
    }

    /**
     * 确认订单
     */
    @PostMapping("/{id}/confirm")
    public ApiResponse<?> confirm(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> {
                    try {
                        order.confirm();
                        orderRepository.save(order);
                        // TODO: 发送订阅消息通知用户
                        Map<String, String> result = new HashMap<>();
                        result.put("status", order.getStatus().name());
                        return ApiResponse.success("订单已确认", result);
                    } catch (IllegalStateException e) {
                        return ApiResponse.error(5300, e.getMessage());
                    }
                })
                .orElse(ApiResponse.error(4004, "订单不存在"));
    }

    /**
     * 拒绝订单
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<?> reject(@PathVariable Long id,
                                                    @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        return orderRepository.findById(id)
                .map(order -> {
                    try {
                        order.reject();
                        orderRepository.save(order);
                        // TODO: 发送订阅消息通知用户
                        Map<String, String> result = new HashMap<>();
                        result.put("status", order.getStatus().name());
                        return ApiResponse.success("订单已拒绝", result);
                    } catch (IllegalStateException e) {
                        return ApiResponse.error(5300, e.getMessage());
                    }
                })
                .orElse(ApiResponse.error(4004, "订单不存在"));
    }

    /**
     * 标记进行中
     */
    @PostMapping("/{id}/start")
    public ApiResponse<?> start(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> {
                    try {
                        order.start();
                        orderRepository.save(order);
                        Map<String, String> result = new HashMap<>();
                        result.put("status", order.getStatus().name());
                        return ApiResponse.success(result);
                    } catch (IllegalStateException e) {
                        return ApiResponse.error(5300, e.getMessage());
                    }
                })
                .orElse(ApiResponse.error(4004, "订单不存在"));
    }

    /**
     * 标记完成
     */
    @PostMapping("/{id}/complete")
    public ApiResponse<?> complete(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> {
                    try {
                        order.complete();
                        orderRepository.save(order);
                        Map<String, String> result = new HashMap<>();
                        result.put("status", order.getStatus().name());
                        return ApiResponse.success(result);
                    } catch (IllegalStateException e) {
                        return ApiResponse.error(5300, e.getMessage());
                    }
                })
                .orElse(ApiResponse.error(4004, "订单不存在"));
    }

    private OrderAdminDTO toAdminDTO(Order order) {
        OrderAdminDTO dto = new OrderAdminDTO();
        dto.setId(order.getId());
        dto.setStartDate(order.getStartDate());
        dto.setEndDate(order.getEndDate());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus().name());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }

    @lombok.Data
    public static class OrderAdminDTO {
        private Long id;
        private java.time.LocalDate startDate;
        private java.time.LocalDate endDate;
        private java.math.BigDecimal totalPrice;
        private String status;
        private java.time.LocalDateTime createdAt;
    }
}
