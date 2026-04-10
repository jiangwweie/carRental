package com.carrental.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("vehicles")
public class VehicleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String brand;
    private Integer seats;
    private String transmission;
    private String description;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private java.util.List<String> images;

    private BigDecimal weekdayPrice;
    private BigDecimal weekendPrice;
    private BigDecimal holidayPrice;

    private String status;

    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
