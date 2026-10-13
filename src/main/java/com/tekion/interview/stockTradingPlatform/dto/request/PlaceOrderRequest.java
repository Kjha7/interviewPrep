package com.tekion.interview.stockTradingPlatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Shared shape for both POST /orders/buy and POST /orders/sell -
 * side is determined by which endpoint was called, not a field in the body.
 */
@Data
public class PlaceOrderRequest {

    @NotBlank
    private String ticker;

    @Positive
    private long quantity;

    // MARKET (default) or LIMIT
    private String orderType = "MARKET";

    // required only when orderType = LIMIT; validated in the service layer
    private Long limitPriceMinorUnits;
}
