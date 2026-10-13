package com.tekion.interview.stockTradingPlatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceResponse {
    private String ticker;
    private BigDecimal price;
    private String currency;
    private Instant asOf;
    private String source;
}
