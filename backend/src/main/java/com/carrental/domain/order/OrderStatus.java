package com.carrental.domain.order;

import java.util.Arrays;

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

    /**
     * 安全查找：未知值返回 null，不抛异常
     */
    public static OrderStatus fromValue(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}
