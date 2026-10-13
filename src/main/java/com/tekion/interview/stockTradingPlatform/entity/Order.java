package com.tekion.interview.stockTradingPlatform.entity;

import com.tekion.interview.stockTradingPlatform.entity.enums.OrderSide;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trade_order", indexes = {
        @Index(name = "idx_order_idempotency_key", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_order_user_status", columnList = "userId, status")
})
@IdClass(OrderId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    private UUID userId; // partition key - every query MUST include userId to stay single-partition

    @Id
    @Builder.Default
    private UUID orderId = UUID.randomUUID();

    @Column(nullable = false, length = 16)
    private String stockTicker;

    @Column(nullable = false)
    private long quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;

    // Minor units (paise). For MARKET orders this is filled in on execution, null/0 until then.
    private Long executedPriceMinorUnits;

    private Long limitPriceMinorUnits; // null for MARKET orders

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
