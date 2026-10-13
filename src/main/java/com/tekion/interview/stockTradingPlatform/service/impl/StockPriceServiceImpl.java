package com.tekion.interview.stockTradingPlatform.service.impl;

import com.tekion.interview.stockTradingPlatform.dto.response.StockPriceResponse;
import com.tekion.interview.stockTradingPlatform.exception.ApiException;
import com.tekion.interview.stockTradingPlatform.marketdata.MarketDataService;
import com.tekion.interview.stockTradingPlatform.service.StockPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StockPriceServiceImpl implements StockPriceService {

    private final MarketDataService marketDataService;

    @Override
    public StockPriceResponse getPrice(String ticker) {
        marketDataService.subscribe(ticker); // no-op if already subscribed

        BigDecimal price;
        try {
            price = marketDataService.getLastPrice(ticker);
        } catch (IllegalStateException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EXCHANGE_UNAVAILABLE", e.getMessage());
        }

        return StockPriceResponse.builder()
                .ticker(ticker)
                .price(price)
                .currency("INR")
                .asOf(Instant.now())
                .source("NSE")
                .build();
    }
}
