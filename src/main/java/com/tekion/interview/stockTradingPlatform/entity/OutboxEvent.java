package com.tekion.interview.stockTradingPlatform.entity;

import com.tekion.interview.stockTradingPlatform.entity.enums.OutboxEventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox: OrderService writes an Order row AND an OutboxEvent row in the
 * SAME @Transactional method / same DB commit. Debezium's Postgres connector + the outbox
 * event router SMT tails the WAL, picks up new rows here, and publishes them to Kafka -
 * no dual-write, no "DB committed but Kafka publish failed" gap.
 *
 * Column names (aggregateType/aggregateId/type/payload) follow Debezium's default
 * outbox event router field mapping - see docker/debezium-connector-config.json.
 */
@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, length = 64)
    private String aggregateType; // e.g. "Order"

    @Column(nullable = false)
    private String aggregateId; // e.g. orderId.toString() - also used as the Kafka message key

    @Column(nullable = false, length = 64)
    private String type; // e.g. "OrderAccepted", "OrderExecuted", "OrderFailed"

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OutboxEventStatus status = OutboxEventStatus.NEW;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
