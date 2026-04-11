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

        // 1. 取全量 active 车辆（MVP 阶段数据量小，全量加载到内存过滤）
        long allTotal = vehicleRepository.countActiveVehicles();
        List<Vehicle> allVehicles = vehicleRepository.findActiveVehicles(1, (int) Math.min(allTotal, 1000));

        // 2. 价格过滤
        List<Vehicle> filtered = allVehicles;
        if (minPrice != null || maxPrice != null) {
            final Integer fpMin = minPrice;
            final Integer fpMax = maxPrice;
            filtered = allVehicles.stream()
                    .filter(v -> {
                        BigDecimal weekday = v.getWeekdayPrice();
                        if (fpMin != null && weekday.compareTo(new BigDecimal(fpMin)) < 0) return false;
                        if (fpMax != null && weekday.compareTo(new BigDecimal(fpMax)) > 0) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        // 3. 内存分页
        long total = filtered.size();
        int fromIndex = Math.min((page - 1) * pageSize, (int) total);
        int toIndex = Math.min(fromIndex + pageSize, (int) total);
        List<Vehicle> pageVehicles = filtered.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("items", pageVehicles.stream().map(this::toListDTO).collect(Collectors.toList()));

        return ApiResponse.success(result);
    }

    /**
     * 车辆详情（公开）
     */
    @GetMapping("/{id}")
    public ApiResponse<VehicleDetailVO> detail(@PathVariable Long id) {
        return vehicleRepository.findById(id)
                .filter(Vehicle::isActive)
                .map(this::toDetailVO)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(4004, "车辆不存在或已下架"));
    }

    private VehicleDetailVO toDetailVO(Vehicle v) {
        VehicleDetailVO vo = new VehicleDetailVO();
        vo.setId(v.getId());
        vo.setName(v.getName());
        vo.setBrand(v.getBrand());
        vo.setSeats(v.getSeats());
        vo.setTransmission(v.getTransmission());
        vo.setDescription(v.getDescription());
        vo.setImages(v.getImages());
        vo.setWeekdayPrice(v.getWeekdayPrice());
        vo.setWeekendPrice(v.getWeekendPrice());
        vo.setHolidayPrice(v.getHolidayPrice());
        vo.setTags(v.getTags());
        return vo;
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

    @Data
    public static class VehicleDetailVO {
        private Long id;
        private String name;
        private String brand;
        private Integer seats;
        private String transmission;
        private String description;
        private List<String> images;       // base64 数组
        private java.math.BigDecimal weekdayPrice;
        private java.math.BigDecimal weekendPrice;
        private java.math.BigDecimal holidayPrice;
        private List<String> tags;
    }
}
