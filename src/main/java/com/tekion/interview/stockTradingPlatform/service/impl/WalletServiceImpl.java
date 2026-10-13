package com.tekion.interview.stockTradingPlatform.service.impl;

import com.tekion.interview.stockTradingPlatform.entity.Wallet;
import com.tekion.interview.stockTradingPlatform.exception.InsufficientFundsException;
import com.tekion.interview.stockTradingPlatform.repository.WalletRepository;
import com.tekion.interview.stockTradingPlatform.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public void reserveFunds(UUID userId, long amountMinorUnits) {
        // The row-level lock taken by this UPDATE is what actually prevents the race between
        // two concurrent buy requests - not application-level checks against a stale read.
        int rowsUpdated = walletRepository.reserveFunds(userId, amountMinorUnits);
        if (rowsUpdated == 0) {
            throw new InsufficientFundsException();
        }
    }

    @Override
    @Transactional
    public void releaseReservation(UUID userId, long amountMinorUnits) {
        walletRepository.releaseReservation(userId, amountMinorUnits);
    }

    @Override
    @Transactional
    public void settleReservedFunds(UUID userId, long reservedAmountMinorUnits, long actualAmountMinorUnits) {
        // Balance was already debited by reservedAmountMinorUnits at reserve time (see reserveFunds).
        // For a MARKET order the actual fill price can differ from that estimate, so:
        //   1. release the full reservation (frees the "reserved" bucket)
        //   2. refund the difference between what was reserved and what was actually spent back
        //      to balance (refund is negative - i.e. an extra debit - if the fill cost MORE than
        //      the original reservation estimate)
        walletRepository.releaseReservation(userId, reservedAmountMinorUnits);

        long refund = reservedAmountMinorUnits - actualAmountMinorUnits;
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet missing for user " + userId));
        wallet.setBalanceMinorUnits(wallet.getBalanceMinorUnits() + refund);
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public void creditFunds(UUID userId, long amountMinorUnits) {
        // Used on SELL settlement (proceeds credited) - a plain additive update, no CAS needed
        // since it never fails a business precondition the way a debit/reserve can.
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet missing for user " + userId));
        wallet.setBalanceMinorUnits(wallet.getBalanceMinorUnits() + amountMinorUnits);
        walletRepository.save(wallet);
    }
}
