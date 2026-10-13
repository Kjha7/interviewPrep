package com.tekion.interview.stockTradingPlatform.dto.event;

import com.tekion.interview.stockTradingPlatform.entity.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Serialized into OutboxEvent.payload, and what PaymentConsumer / NotificationConsumer
 * deserialize back off the Kafka topic. Keep this stable - it's effectively a public
 * contract between the producer (OrderServiceImpl) and every consumer group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventPayload {
    private String eventType; // "OrderAccepted" | "OrderExecuted" | "OrderFailed"
    private UUID orderId;
    private UUID userId;
    private String ticker;
    private long quantity;
    private OrderSide side;
    private Long limitPriceMinorUnits;   // null for MARKET
    private Long executedPriceMinorUnits; // null until OrderExecuted
    private Long reservedAmountMinorUnits; // BUY only - what WalletService reserved at accept time
}
