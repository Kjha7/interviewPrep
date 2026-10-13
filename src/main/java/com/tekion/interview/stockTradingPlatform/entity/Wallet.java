package com.tekion.interview.stockTradingPlatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Balances are stored as integer minor units (paise) - never double/float - to avoid
 * floating point drift in money math. 100000 = INR 1,000.00.
 */
@Entity
@Table(name = "wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    private UUID userId;

    @Column(nullable = false)
    private long balanceMinorUnits;

    // Funds earmarked against PENDING buy orders - not yet debited, not spendable again.
    @Column(nullable = false)
    private long reservedMinorUnits;

    @Column(nullable = false, length = 3)
    private String currency;

    private Instant updatedAt;

    // Backs the compare-and-set style update: WHERE user_id=? AND version=?
    @Version
    private long version;

    @PreUpdate
    @PrePersist
    void touch() {
        this.updatedAt = Instant.now();
    }
}
