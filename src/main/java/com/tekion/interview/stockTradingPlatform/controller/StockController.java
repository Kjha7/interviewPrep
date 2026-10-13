package com.tekion.interview.stockTradingPlatform.controller;

import com.tekion.interview.stockTradingPlatform.dto.response.StockPriceResponse;
import com.tekion.interview.stockTradingPlatform.service.StockPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockPriceService stockPriceService;

    @GetMapping("/{ticker}/price")
    public ResponseEntity<StockPriceResponse> getPrice(@PathVariable String ticker) {
        return ResponseEntity.ok(stockPriceService.getPrice(ticker.toUpperCase()));
    }
}
