package com.carrental.domain.vehicle;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Vehicle {

    private Long id;
    private String name;
    private String brand;
    private Integer seats;
    private String transmission;
    private String description;
    private List<String> images;
    private List<String> tags;
    private BigDecimal weekdayPrice;
    private BigDecimal weekendPrice;
    private BigDecimal holidayPrice;
    private String status;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return "active".equals(this.status) && this.deletedAt == null;
    }

    public String getCoverImage() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.get(0);
    }
}
