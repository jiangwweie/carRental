package com.carrental.controller;

import com.carrental.common.exception.BusinessException;
import com.carrental.common.result.ApiResponse;
import com.carrental.common.result.ErrorCode;
import com.carrental.domain.pricing.PricingEngine;
import com.carrental.domain.pricing.PricingResult;
import com.carrental.domain.pricing.PricingResult.DayPrice;
import com.carrental.domain.vehicle.Vehicle;
import com.carrental.domain.vehicle.VehicleRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 价格预估接口（Sprint 1）
 * 此接口不需要登录（WebMvcConfig 需排除此路径）
 */
@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingEngine pricingEngine;
    private final VehicleRepository vehicleRepository;

    @PostMapping("/estimate")
    public ApiResponse<EstimateResult> estimate(@Valid @RequestBody EstimateRequest request) {
        // 校验日期
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "取车日期不能早于今天");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "还车日期必须晚于取车日期");
        }

        // 查询车辆
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .filter(Vehicle::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "车辆不存在或已下架"));

        // 计算价格
        PricingResult result = pricingEngine.calculate(
                vehicle.getWeekdayPrice(),
                vehicle.getWeekendPrice(),
                vehicle.getHolidayPrice(),
                request.getStartDate(), request.getEndDate()
        );

        long days = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());

        // 组装响应
        EstimateResult response = new EstimateResult();
        response.setVehicleId(vehicle.getId());
        response.setVehicleName(vehicle.getName());
        response.setStartDate(request.getStartDate());
        response.setEndDate(request.getEndDate());
        response.setDays((int) days);
        response.setTotalPrice(result.getTotalPrice());
        response.setPriceBreakdown(result.getDayPrices().stream()
                .map(dp -> new PriceBreakdownItem(dp.getDate(), dp.getType(), dp.getPrice()))
                .toList());

        return ApiResponse.success(response);
    }

    @Data
    public static class EstimateRequest {
        @NotNull(message = "车辆ID不能为空")
        private Long vehicleId;
        @NotNull(message = "取车日期不能为空")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;
        @NotNull(message = "还车日期不能为空")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
    }

    @Data
    public static class EstimateResult {
        private Long vehicleId;
        private String vehicleName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer days;
        private BigDecimal totalPrice;
        private List<PriceBreakdownItem> priceBreakdown;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class PriceBreakdownItem {
        private String date;
        private String type;
        private BigDecimal price;
    }
}
