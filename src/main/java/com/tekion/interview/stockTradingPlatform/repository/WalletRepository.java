package com.tekion.interview.stockTradingPlatform.repository;

import com.tekion.interview.stockTradingPlatform.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Atomic reserve: only succeeds if there's enough unreserved balance.
     * The WHERE clause on balance is the compare-and-set guard - the row lock taken by
     * this UPDATE serializes concurrent buy attempts for the same user, so two concurrent
     * requests can't both pass the balance check against a stale read.
     *
     * @return number of rows updated - 0 means insufficient funds (service layer treats
     *         that as a business rejection, not a system error).
     */
    @Modifying
    @Query("""
        UPDATE Wallet w
        SET w.balanceMinorUnits = w.balanceMinorUnits - :amount,
            w.reservedMinorUnits = w.reservedMinorUnits + :amount
        WHERE w.userId = :userId
          AND w.balanceMinorUnits >= :amount
        """)
    int reserveFunds(@Param("userId") UUID userId, @Param("amount") long amount);

    @Modifying
    @Query("""
        UPDATE Wallet w
        SET w.reservedMinorUnits = w.reservedMinorUnits - :amount
        WHERE w.userId = :userId
          AND w.reservedMinorUnits >= :amount
        """)
    int releaseReservation(@Param("userId") UUID userId, @Param("amount") long amount);

    // userId IS the @Id here, so findById(UUID) already covers the single-wallet lookup.
}
