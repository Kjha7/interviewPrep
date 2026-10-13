package com.tekion.interview.stockTradingPlatform.service;

import com.tekion.interview.stockTradingPlatform.dto.response.PortfolioResponse;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderSide;

import java.util.UUID;

public interface PortfolioService {

    PortfolioResponse getPortfolio(UUID userId, String range);

    /**
     * Applies a fill to the read-projection. Called by the Kafka consumer processing
     * OrderExecuted events - NOT called synchronously from OrderController.
     */
    void applyFill(UUID userId, String ticker, long quantity, OrderSide side, long priceMinorUnits);
}
