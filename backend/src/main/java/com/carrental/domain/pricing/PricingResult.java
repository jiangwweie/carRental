package com.carrental.domain.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingResult {

    private List<DayPrice> dayPrices;
    private BigDecimal totalPrice;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPrice {
        private String date;
        private String type;      // weekday / weekend / holiday
        private BigDecimal price;
    }
}
