package com.tekion.interview.stockTradingPlatform.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends ApiException {
    public InsufficientFundsException() {
        super(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_FUNDS", "Wallet balance is insufficient to reserve for this order");
    }
}
