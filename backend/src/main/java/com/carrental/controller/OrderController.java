package com.carrental.controller;

import com.carrental.common.exception.BusinessException;
import com.carrental.common.result.ApiResponse;
import com.carrental.common.result.ErrorCode;
import com.carrental.domain.order.Order;
import com.carrental.domain.order.OrderRepository;
import com.carrental.domain.order.OrderStatus;
import com.carrental.domain.order.PriceBreakdown;
import com.carrental.domain.order.service.OrderConflictChecker;
import com.carrental.domain.pricing.PricingEngine;
import com.carrental.domain.pricing.PricingResult;
import com.carrental.domain.vehicle.Vehicle;
import com.carrental.domain.vehicle.VehicleRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderConflictChecker conflictChecker;
    private final VehicleRepository vehicleRepository;
    private final PricingEngine pricingEngine;

    /**
     * 创建订单
     */
    @PostMapping
    public ApiResponse<CreateOrderResult> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");

        // 校验用户已同意租赁协议
        if (request.getAgreed() == null || !request.getAgreed()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先同意租赁协议");
        }

        // 查询车辆信息
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "车辆不存在"));

        // 检查时间冲突
        conflictChecker.checkConflict(request.getVehicleId(), request.getStartDate(), request.getEndDate());

        // 计算天数
        int days = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());

        // 使用定价引擎算价
        PricingResult pricingResult = pricingEngine.calculate(
                vehicle.getWeekdayPrice(),
                vehicle.getWeekendPrice(),
                vehicle.getHolidayPrice(),
                request.getStartDate(),
                request.getEndDate()
        );
        BigDecimal totalPrice = pricingResult.getTotalPrice();
        List<PriceBreakdown> priceBreakdown = pricingResult.getDayPrices().stream()
                .map(dp -> new PriceBreakdown(
                        LocalDate.parse(dp.getDate()),
                        dp.getType(),
                        dp.getPrice()
                ))
                .collect(Collectors.toList());

        // 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setVehicleId(request.getVehicleId());
        order.setStartDate(request.getStartDate());
        order.setEndDate(request.getEndDate());
        order.setDays(days);
        order.setTotalPrice(totalPrice);
        order.setPriceBreakdown(priceBreakdown);
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

        // 批量获取车辆信息（循环 findById + Map 缓存）
        Map<Long, Vehicle> vehicleMap = new HashMap<>();
        for (Order order : orders) {
            if (!vehicleMap.containsKey(order.getVehicleId())) {
                vehicleMap.put(order.getVehicleId(),
                        vehicleRepository.findById(order.getVehicleId()).orElse(null));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("items", orders.stream()
                .map(order -> toListDTO(order, vehicleMap.get(order.getVehicleId())))
                .collect(Collectors.toList()));

        return ApiResponse.success(result);
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderDetailVO> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Order order = orderRepository.findById(id)
                .filter(o -> Objects.equals(o.getUserId(), userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));

        Vehicle vehicle = vehicleRepository.findById(order.getVehicleId()).orElse(null);
        return ApiResponse.success(toDetailVO(order, vehicle));
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancelOrder(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return orderRepository.findById(id)
                .filter(order -> Objects.equals(order.getUserId(), userId))
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

    private OrderListItemDTO toListDTO(Order order, Vehicle vehicle) {
        OrderListItemDTO dto = new OrderListItemDTO();
        dto.setId(order.getId());
        dto.setStartDate(order.getStartDate());
        dto.setEndDate(order.getEndDate());
        dto.setDays(order.getDays());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus().name());
        dto.setStatusLabel(order.getStatus().getLabel());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setCanCancel(order.getStatus() == OrderStatus.PENDING);

        if (vehicle != null) {
            dto.setVehicleName(vehicle.getName());
            dto.setVehicleImage(vehicle.getCoverImage());
        }
        return dto;
    }

    private OrderDetailVO toDetailVO(Order order, Vehicle vehicle) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setStartDate(order.getStartDate());
        vo.setEndDate(order.getEndDate());
        vo.setDays(order.getDays());
        vo.setTotalPrice(order.getTotalPrice());
        vo.setStatus(order.getStatus().name());
        vo.setStatusLabel(order.getStatus().getLabel());
        vo.setStatusSteps(buildStatusSteps(order.getStatus()));
        vo.setPaymentStatus(order.getPaymentStatus());
        vo.setPriceBreakdown(order.getPriceBreakdown());
        vo.setPickupAddress(buildPickupAddress());
        vo.setCanCancel(order.getStatus() == OrderStatus.PENDING);
        vo.setRejectReason(order.getRejectReason());
        vo.setCreatedAt(order.getCreatedAt());

        if (vehicle != null) {
            OrderDetailVO.VehicleInfo vi = new OrderDetailVO.VehicleInfo();
            vi.setId(vehicle.getId());
            vi.setName(vehicle.getName());
            vi.setImages(vehicle.getImages());
            vo.setVehicle(vi);
        }
        return vo;
    }

    private List<Map<String, Object>> buildStatusSteps(OrderStatus currentStatus) {
        // 订单状态流转：待确认 -> 已确认 -> 进行中 -> 已完成
        // 已取消/已拒绝 不走正常流程
        List<Map<String, Object>> steps = new ArrayList<>();

        if (currentStatus == OrderStatus.CANCELLED) {
            addStep(steps, "已取消", true, true);
            return steps;
        }
        if (currentStatus == OrderStatus.REJECTED) {
            addStep(steps, "已拒绝", true, true);
            return steps;
        }

        // 正常流程
        String[] labels = {"待确认", "已确认", "进行中", "已完成"};
        OrderStatus[] statuses = {
                OrderStatus.PENDING, OrderStatus.CONFIRMED,
                OrderStatus.IN_PROGRESS, OrderStatus.COMPLETED
        };

        int currentIdx = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i] == currentStatus) {
                currentIdx = i;
                break;
            }
        }

        for (int i = 0; i < labels.length; i++) {
            boolean completed = i <= currentIdx;
            boolean current = i == currentIdx;
            addStep(steps, labels[i], completed, current);
        }
        return steps;
    }

    private OrderDetailVO.PickupAddress buildPickupAddress() {
        OrderDetailVO.PickupAddress addr = new OrderDetailVO.PickupAddress();
        addr.setAddress("北京市朝阳区XX路XX号");
        addr.setHours("周一至周日 08:00-20:00");
        addr.setNote("下单后请与车主确认取车时间");
        return addr;
    }

    private void addStep(List<Map<String, Object>> steps, String label, boolean completed, boolean current) {
        Map<String, Object> step = new HashMap<>();
        step.put("label", label);
        step.put("completed", completed);
        step.put("current", current);
        steps.add(step);
    }

    @Data
    public static class CreateOrderRequest {
        @NotNull(message = "车辆ID不能为空")
        private Long vehicleId;
        @NotNull(message = "开始日期不能为空")
        private LocalDate startDate;
        @NotNull(message = "结束日期不能为空")
        private LocalDate endDate;
        @NotNull(message = "请同意租赁协议")
        private Boolean agreed;
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
        private String vehicleName;
        private String vehicleImage;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer days;
        private java.math.BigDecimal totalPrice;
        private String status;
        private String statusLabel;
        private java.time.LocalDateTime createdAt;
        private Boolean canCancel;
    }

    @Data
    public static class OrderDetailVO {
        private Long id;
        private VehicleInfo vehicle;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer days;
        private java.math.BigDecimal totalPrice;
        private String status;
        private String statusLabel;
        private List<Map<String, Object>> statusSteps;
        private String paymentStatus;
        private List<PriceBreakdown> priceBreakdown;
        private PickupAddress pickupAddress;
        private Boolean canCancel;
        private String rejectReason;
        private java.time.LocalDateTime createdAt;

        @Data
        public static class PickupAddress {
            private String address;
            private String hours;
            private String note;
        }

        @Data
        public static class VehicleInfo {
            private Long id;
            private String name;
            private List<String> images;
        }
    }
}
