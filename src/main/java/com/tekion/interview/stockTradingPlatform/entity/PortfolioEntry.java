package com.tekion.interview.stockTradingPlatform.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

/**
 * Kept on Cassandra deliberately - Order is now the Postgres source of truth, and Portfolio
 * is a read-optimized projection rebuilt from OrderExecuted events (via a Kafka consumer),
 * not written synchronously in the request path. Eventual consistency here is fine: a
 * portfolio view lagging by milliseconds after a fill is a UX detail, not a money-safety
 * problem the way a double-spent wallet balance would be.
 */
@Table("portfolio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioEntry {

    @PrimaryKey
    private PortfolioEntryKey key;

    @Column("quantity")
    private long quantity;

    @Column("avg_price_minor_units")
    private long avgPriceMinorUnits;

    @Column("version")
    private int version; // application-managed CAS counter (see PortfolioServiceImpl LWT usage)

    @Column("updated_at")
    private Instant updatedAt;
}
