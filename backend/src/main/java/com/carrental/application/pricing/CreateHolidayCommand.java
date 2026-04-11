package com.carrental.application.pricing;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建节假日配置命令对象
 */
@Data
public class CreateHolidayCommand {
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private BigDecimal priceMultiplier;
    private BigDecimal fixedPrice;
    private Integer year;
}
