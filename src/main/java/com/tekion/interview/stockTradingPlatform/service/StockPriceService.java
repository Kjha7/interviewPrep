package com.tekion.interview.stockTradingPlatform.service;

import com.tekion.interview.stockTradingPlatform.dto.response.StockPriceResponse;

public interface StockPriceService {
    StockPriceResponse getPrice(String ticker);
}
