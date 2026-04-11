package com.carrental.controller;

import com.carrental.application.pricing.CreateHolidayCommand;
import com.carrental.application.pricing.HolidayAdminService;
import com.carrental.common.result.ApiResponse;
import com.carrental.domain.holiday.Holiday;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端定价相关接口
 */
@RestController
@RequestMapping("/api/v1/admin/pricing")
@RequiredArgsConstructor
public class AdminPricingController {

    private final HolidayAdminService holidayAdminService;

    /**
     * 节假日配置列表
     * GET /api/v1/admin/pricing/holidays?year=2026
     */
    @GetMapping("/holidays")
    public ApiResponse<List<HolidayDTO>> listHolidays(
            @RequestParam(required = false) Integer year) {
        List<Holiday> holidays = holidayAdminService.listHolidays(year);
        return ApiResponse.success(holidays.stream()
            .map(this::toDTO)
            .toList());
    }

    /**
     * 创建单个节假日
     * POST /api/v1/admin/pricing/holidays
     */
    @PostMapping("/holidays")
    public ApiResponse<HolidayDTO> createHoliday(@Valid @RequestBody CreateHolidayCommand cmd) {
        Holiday holiday = holidayAdminService.createHoliday(cmd);
        return ApiResponse.success(toDTO(holiday));
    }

    /**
     * 批量创建节假日
     * POST /api/v1/admin/pricing/holidays/batch
     */
    @PostMapping("/holidays/batch")
    public ApiResponse<Map<String, Integer>> batchCreateHolidays(
            @Valid @RequestBody BatchHolidayRequest request) {
        int count = holidayAdminService.batchCreateHolidays(request.getHolidays());
        Map<String, Integer> result = new HashMap<>();
        result.put("created", count);
        return ApiResponse.success(result);
    }

    // ====== DTO 转换 ======

    private HolidayDTO toDTO(Holiday holiday) {
        HolidayDTO dto = new HolidayDTO();
        dto.setId(holiday.getId());
        dto.setName(holiday.getName());
        dto.setStartDate(holiday.getStartDate());
        dto.setEndDate(holiday.getEndDate());
        dto.setPriceMultiplier(holiday.getPriceMultiplier());
        dto.setFixedPrice(holiday.getFixedPrice());
        dto.setYear(holiday.getYear());
        return dto;
    }

    @Data
    public static class HolidayDTO {
        private Long id;
        private String name;
        private java.time.LocalDate startDate;
        private java.time.LocalDate endDate;
        private java.math.BigDecimal priceMultiplier;
        private java.math.BigDecimal fixedPrice;
        private Integer year;
    }

    @Data
    public static class BatchHolidayRequest {
        @NotNull(message = "节假日配置列表不能为空")
        private List<CreateHolidayCommand> holidays;
    }
}
