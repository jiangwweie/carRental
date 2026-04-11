package com.carrental.controller;

import com.carrental.common.result.ApiResponse;
import com.carrental.common.result.ErrorCode;
import com.carrental.domain.vehicle.Vehicle;
import com.carrental.domain.vehicle.VehicleRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/vehicles")
@RequiredArgsConstructor
public class AdminVehicleController {

    private final VehicleRepository vehicleRepository;

    /**
     * 车辆列表（含下架，支持分页）
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<Vehicle> allVehicles = vehicleRepository.findAllVehicles(page, pageSize);
        long total = vehicleRepository.countAllVehicles();

        if (status != null && !status.isEmpty()) {
            allVehicles = allVehicles.stream()
                    .filter(v -> status.equals(v.getStatus()))
                    .collect(Collectors.toList());
            total = allVehicles.size();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("items", allVehicles);
        return ApiResponse.success(result);
    }

    /**
     * 创建车辆
     */
    @PostMapping
    public ApiResponse<Vehicle> create(@RequestBody CreateVehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setName(request.getName());
        vehicle.setBrand(request.getBrand());
        vehicle.setSeats(request.getSeats());
        vehicle.setTransmission(request.getTransmission());
        vehicle.setDescription(request.getDescription());
        vehicle.setImages(request.getImages());
        vehicle.setTags(request.getTags());
        vehicle.setWeekdayPrice(request.getWeekdayPrice());
        vehicle.setWeekendPrice(request.getWeekendPrice());
        vehicle.setHolidayPrice(request.getHolidayPrice());
        vehicle.setStatus("active");

        vehicle = vehicleRepository.save(vehicle);
        return ApiResponse.success(vehicle);
    }

    /**
     * 更新车辆（部分更新，仅更新传入的字段）
     */
    @PutMapping("/{id}")
    public ApiResponse<Vehicle> update(@PathVariable Long id, @RequestBody UpdateVehicleRequest request) {
        return vehicleRepository.findById(id)
                .map(vehicle -> {
                    if (request.getName() != null) vehicle.setName(request.getName());
                    if (request.getBrand() != null) vehicle.setBrand(request.getBrand());
                    if (request.getSeats() != null) vehicle.setSeats(request.getSeats());
                    if (request.getTransmission() != null) vehicle.setTransmission(request.getTransmission());
                    if (request.getDescription() != null) vehicle.setDescription(request.getDescription());
                    if (request.getImages() != null) vehicle.setImages(request.getImages());
                    if (request.getTags() != null) vehicle.setTags(request.getTags());
                    if (request.getWeekdayPrice() != null) vehicle.setWeekdayPrice(request.getWeekdayPrice());
                    if (request.getWeekendPrice() != null) vehicle.setWeekendPrice(request.getWeekendPrice());
                    if (request.getHolidayPrice() != null) vehicle.setHolidayPrice(request.getHolidayPrice());
                    return ApiResponse.success(vehicleRepository.save(vehicle));
                })
                .orElse(ApiResponse.error(ErrorCode.NOT_FOUND));
    }

    /**
     * 上下架
     */
    @PostMapping("/{id}/toggle-status")
    public ApiResponse<Map<String, String>> toggleStatus(@PathVariable Long id) {
        return vehicleRepository.findById(id)
                .map(vehicle -> {
                    String newStatus = "active".equals(vehicle.getStatus()) ? "inactive" : "active";
                    vehicle.setStatus(newStatus);
                    vehicleRepository.save(vehicle);
                    Map<String, String> result = new HashMap<>();
                    result.put("status", newStatus);
                    return ApiResponse.success(result);
                })
                .orElse(ApiResponse.error(ErrorCode.NOT_FOUND));
    }

    /**
     * 删除车辆（软删除）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        vehicleRepository.softDelete(id);
        return ApiResponse.success(null);
    }

    /**
     * 批量更新车辆价格
     */
    @PutMapping("/prices")
    @Transactional
    public ApiResponse<Void> batchUpdatePrices(@RequestBody BatchUpdatePriceRequest request) {
        for (VehiclePriceItem item : request.getItems()) {
            vehicleRepository.findById(item.getId()).ifPresent(vehicle -> {
                if (item.getWeekdayPrice() != null) {
                    vehicle.setWeekdayPrice(item.getWeekdayPrice());
                }
                if (item.getWeekendPrice() != null) {
                    vehicle.setWeekendPrice(item.getWeekendPrice());
                }
                if (item.getHolidayPrice() != null) {
                    vehicle.setHolidayPrice(item.getHolidayPrice());
                }
                vehicleRepository.save(vehicle);
            });
        }
        return ApiResponse.success(null);
    }

    @Data
    public static class CreateVehicleRequest {
        private String name;
        private String brand;
        private Integer seats;
        private String transmission;
        private String description;
        private List<String> images;
        private List<String> tags;
        private BigDecimal weekdayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal holidayPrice;
    }

    @Data
    public static class UpdateVehicleRequest {
        private String name;
        private String brand;
        private Integer seats;
        private String transmission;
        private String description;
        private List<String> images;
        private List<String> tags;
        private BigDecimal weekdayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal holidayPrice;
    }

    @Data
    public static class BatchUpdatePriceRequest {
        private List<VehiclePriceItem> items;
    }

    @Data
    public static class VehiclePriceItem {
        private Long id;
        private BigDecimal weekdayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal holidayPrice;
    }
}
