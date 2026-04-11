package com.carrental.controller;

import com.carrental.common.result.ApiResponse;
import com.carrental.domain.order.Order;
import com.carrental.domain.order.OrderRepository;
import com.carrental.domain.order.OrderStatus;
import com.carrental.domain.user.User;
import com.carrental.domain.user.UserRepository;
import com.carrental.domain.vehicle.Vehicle;
import com.carrental.domain.vehicle.VehicleRepository;
import lombok.Data;
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
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    /**
     * 订单列表（含分页 + 用户/车辆信息）
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        List<Order> orders = orderRepository.findAdminOrders(status, vehicleId, page, pageSize);
        long total = orderRepository.countAdminOrders(status, vehicleId);

        // 批量查询关联数据，避免 N+1
        List<Long> userIds = orders.stream().map(Order::getUserId).distinct().collect(Collectors.toList());
        List<Long> vehicleIds = orders.stream().map(Order::getVehicleId).distinct().collect(Collectors.toList());

        Map<Long, User> userMap = new HashMap<>();
        for (Long uid : userIds) {
            userRepository.findById(uid).ifPresent(u -> userMap.put(u.getId(), u));
        }

        Map<Long, Vehicle> vehicleMap = new HashMap<>();
        for (Long vid : vehicleIds) {
            vehicleRepository.findById(vid).ifPresent(v -> vehicleMap.put(v.getId(), v));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("items", orders.stream()
                .map(o -> toAdminDTO(o, userMap.get(o.getUserId()), vehicleMap.get(o.getVehicleId())))
                .collect(Collectors.toList()));

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
                        order.reject(reason);
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

    private OrderAdminDTO toAdminDTO(Order order, User user, Vehicle vehicle) {
        OrderAdminDTO dto = new OrderAdminDTO();
        dto.setId(order.getId());
        dto.setStartDate(order.getStartDate());
        dto.setEndDate(order.getEndDate());
        dto.setDays(order.getDays());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus().name());
        dto.setStatusLabel(order.getStatus().getLabel());
        dto.setCreatedAt(order.getCreatedAt());

        // 用户手机号（脱敏）
        if (user != null && user.getPhone() != null) {
            String phone = user.getPhone();
            if (phone.length() >= 7) {
                dto.setUserPhone(phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4));
            } else {
                dto.setUserPhone(phone);
            }
        }

        // 车辆信息
        if (vehicle != null) {
            dto.setVehicleName(vehicle.getName());
            dto.setVehicleImage(vehicle.getCoverImage());
        }

        return dto;
    }

    @Data
    public static class OrderAdminDTO {
        private Long id;
        private String userPhone;
        private String vehicleName;
        private String vehicleImage;
        private java.time.LocalDate startDate;
        private java.time.LocalDate endDate;
        private Integer days;
        private java.math.BigDecimal totalPrice;
        private String status;
        private String statusLabel;
        private java.time.LocalDateTime createdAt;
    }
}
