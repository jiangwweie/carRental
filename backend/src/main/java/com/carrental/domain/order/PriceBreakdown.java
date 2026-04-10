package com.carrental.domain.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceBreakdown {

    private LocalDate date;
    private String type;      // weekday / weekend / holiday
    private BigDecimal price;
}
