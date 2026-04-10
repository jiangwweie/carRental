package com.carrental.controller;

import com.carrental.common.result.ApiResponse;
import com.carrental.common.result.ErrorCode;
import com.carrental.domain.vehicle.Vehicle;
import com.carrental.domain.vehicle.VehicleRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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
     * 车辆列表（含下架）
     */
    @GetMapping
    public ApiResponse<List<Vehicle>> list() {
        // 简单实现：返回所有车辆
        // 实际应该分页，这里简化处理
        List<Vehicle> vehicles = vehicleRepository.findActiveVehicles(1, 1000);
        return ApiResponse.success(vehicles);
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
        vehicle.setWeekdayPrice(request.getWeekdayPrice());
        vehicle.setWeekendPrice(request.getWeekendPrice());
        vehicle.setHolidayPrice(request.getHolidayPrice());
        vehicle.setStatus("active");

        vehicle = vehicleRepository.save(vehicle);
        return ApiResponse.success(vehicle);
    }

    /**
     * 更新车辆
     */
    @PutMapping("/{id}")
    public ApiResponse<Vehicle> update(@PathVariable Long id, @RequestBody CreateVehicleRequest request) {
        return vehicleRepository.findById(id)
                .map(vehicle -> {
                    vehicle.setName(request.getName());
                    vehicle.setBrand(request.getBrand());
                    vehicle.setSeats(request.getSeats());
                    vehicle.setTransmission(request.getTransmission());
                    vehicle.setDescription(request.getDescription());
                    vehicle.setImages(request.getImages());
                    vehicle.setWeekdayPrice(request.getWeekdayPrice());
                    vehicle.setWeekendPrice(request.getWeekendPrice());
                    vehicle.setHolidayPrice(request.getHolidayPrice());
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

    @Data
    public static class CreateVehicleRequest {
        private String name;
        private String brand;
        private Integer seats;
        private String transmission;
        private String description;
        private List<String> images;
        private BigDecimal weekdayPrice;
        private BigDecimal weekendPrice;
        private BigDecimal holidayPrice;
    }
}
