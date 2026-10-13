package com.tekion.interview.stockTradingPlatform.exception;

import org.springframework.http.HttpStatus;

public class InsufficientHoldingsException extends ApiException {
    public InsufficientHoldingsException(String ticker) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_HOLDINGS", "Not enough " + ticker + " shares held to place this sell order");
    }
}
