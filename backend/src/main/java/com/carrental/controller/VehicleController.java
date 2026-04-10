package com.carrental.controller;

import com.carrental.common.result.ApiResponse;
import com.carrental.domain.vehicle.Vehicle;
import com.carrental.domain.vehicle.VehicleRepository;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleRepository vehicleRepository;

    /**
     * 车辆列表（公开）
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        List<Vehicle> vehicles = vehicleRepository.findActiveVehicles(page, pageSize);
        long total = vehicleRepository.countActiveVehicles();

        // 价格过滤
        if (minPrice != null || maxPrice != null) {
            vehicles = vehicles.stream()
                    .filter(v -> {
                        BigDecimal weekday = v.getWeekdayPrice();
                        if (minPrice != null && weekday.compareTo(new BigDecimal(minPrice)) < 0) return false;
                        if (maxPrice != null && weekday.compareTo(new BigDecimal(maxPrice)) > 0) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", vehicles.size());
        result.put("items", vehicles.stream().map(this::toListDTO).collect(Collectors.toList()));

        return ApiResponse.success(result);
    }

    /**
     * 车辆详情（公开）
     */
    @GetMapping("/{id}")
    public ApiResponse<Vehicle> detail(@PathVariable Long id) {
        return vehicleRepository.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(4004, "车辆不存在"));
    }

    private VehicleListItemDTO toListDTO(Vehicle vehicle) {
        VehicleListItemDTO dto = new VehicleListItemDTO();
        dto.setId(vehicle.getId());
        dto.setName(vehicle.getName());
        dto.setBrand(vehicle.getBrand());
        dto.setSeats(vehicle.getSeats());
        dto.setTransmission(vehicle.getTransmission());
        dto.setCoverImage(vehicle.getCoverImage());
        dto.setWeekdayPrice(vehicle.getWeekdayPrice());
        dto.setWeekendPrice(vehicle.getWeekendPrice());
        return dto;
    }

    @Data
    public static class VehicleListItemDTO {
        private Long id;
        private String name;
        private String brand;
        private Integer seats;
        private String transmission;
        private String coverImage;
        private java.math.BigDecimal weekdayPrice;
        private java.math.BigDecimal weekendPrice;
    }
}
