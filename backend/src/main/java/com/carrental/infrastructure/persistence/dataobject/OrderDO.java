package com.carrental.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "orders", autoResultMap = true)
public class OrderDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long vehicleId;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;

    private BigDecimal totalPrice;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<com.carrental.domain.order.PriceBreakdown> priceBreakdown;

    private String status;
    private String paymentStatus;
    private String paymentId;
    private LocalDateTime paidAt;
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
