package com.carrental.domain.order;

public enum OrderStatus {
    PENDING("待确认"),
    CONFIRMED("已确认"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REJECTED("已拒绝");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
