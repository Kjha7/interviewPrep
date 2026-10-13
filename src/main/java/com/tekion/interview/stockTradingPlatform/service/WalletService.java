package com.tekion.interview.stockTradingPlatform.service;

import java.util.UUID;

public interface WalletService {

    /** Throws InsufficientFundsException if the CAS reserve fails. */
    void reserveFunds(UUID userId, long amountMinorUnits);

    void releaseReservation(UUID userId, long amountMinorUnits);

    /** Moves amount from "reserved" to "spent" - call after a buy order actually executes. */
    void settleReservedFunds(UUID userId, long reservedAmountMinorUnits, long actualAmountMinorUnits);

    void creditFunds(UUID userId, long amountMinorUnits);
}
