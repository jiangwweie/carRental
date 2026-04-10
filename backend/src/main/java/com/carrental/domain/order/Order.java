package com.carrental.domain.order;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Getter
public class Order {

    private Long id;
    private Long userId;
    private Long vehicleId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;
    private BigDecimal totalPrice;
    private List<PriceBreakdown> priceBreakdown;
    private OrderStatus status;
    private String paymentStatus;
    private String paymentId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("只有待确认的订单可以确认");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void reject() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("只有待确认的订单可以拒绝");
        }
        this.status = OrderStatus.REJECTED;
    }

    public void cancel() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("只有待确认的订单可以取消");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void start() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("只有已确认的订单可以开始");
        }
        this.status = OrderStatus.IN_PROGRESS;
    }

    public void complete() {
        if (this.status != OrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("只有进行中的订单可以完成");
        }
        this.status = OrderStatus.COMPLETED;
    }
}
