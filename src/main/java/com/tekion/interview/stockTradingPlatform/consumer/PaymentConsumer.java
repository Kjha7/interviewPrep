package com.tekion.interview.stockTradingPlatform.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tekion.interview.stockTradingPlatform.config.KafkaTopicConfig;
import com.tekion.interview.stockTradingPlatform.dto.event.OrderEventPayload;
import com.tekion.interview.stockTradingPlatform.entity.Order;
import com.tekion.interview.stockTradingPlatform.entity.OutboxEvent;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderSide;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderStatus;
import com.tekion.interview.stockTradingPlatform.marketdata.MarketDataService;
import com.tekion.interview.stockTradingPlatform.repository.OrderRepository;
import com.tekion.interview.stockTradingPlatform.repository.OutboxEventRepository;
import com.tekion.interview.stockTradingPlatform.service.PortfolioService;
import com.tekion.interview.stockTradingPlatform.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Consumer group "payment-consumer-group" (see application.properties) - reads OrderAccepted
 * events off the Debezium-published outbox topic, executes against the exchange (stubbed here),
 * settles the wallet, updates the Cassandra portfolio projection, flips the Order row to
 * EXECUTED/FAILED, and writes a follow-up OrderExecuted/OrderFailed outbox event so
 * NotificationConsumer (or anything else downstream) can react without re-deriving state.
 *
 * Idempotency note: Kafka gives at-least-once delivery. This handler is written to be safe
 * to run twice for the same orderId - the Order status check below turns a duplicate delivery
 * into a no-op instead of double-crediting/debiting.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final WalletService walletService;
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC,
            groupId = "payment-consumer-group")
    @Transactional
    public void onOrderEvent(String rawPayload) throws Exception {
        // @Transactional is on THIS method deliberately, not on a private/package helper called
        // from within the class - Spring's proxy-based AOP does not intercept self-invocation
        // (this.someMethod(...) bypasses the proxy entirely), which would silently make the
        // transaction boundary a no-op. Keeping the annotated method as the actual Kafka
        // listener entry point avoids that gotcha.
        OrderEventPayload event = objectMapper.readValue(rawPayload, OrderEventPayload.class);

        if (!"OrderAccepted".equals(event.getEventType())) {
            return; // this consumer only reacts to freshly accepted orders
        }

        Order order = orderRepository.findByUserIdAndOrderId(event.getUserId(), event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found for event: " + event.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order {} already in status {}, skipping duplicate delivery", order.getOrderId(), order.getStatus());
            return;
        }

        try {
            // TODO: real exchange order placement (FIX/REST/whatever the exchange exposes) goes
            // here instead of just reading the cached market price. This stub fills at the
            // current cached price to keep the boilerplate runnable end-to-end.
            BigDecimal fillPrice = marketDataService.getLastPrice(event.getTicker());
            long fillPriceMinorUnits = fillPrice.movePointRight(2).longValueExact();
            long actualAmountMinorUnits = fillPriceMinorUnits * event.getQuantity();

            if (event.getSide() == OrderSide.BUY) {
                walletService.settleReservedFunds(event.getUserId(), event.getReservedAmountMinorUnits(), actualAmountMinorUnits);
            } else {
                walletService.creditFunds(event.getUserId(), actualAmountMinorUnits);
            }
            portfolioService.applyFill(event.getUserId(), event.getTicker(), event.getQuantity(), event.getSide(), fillPriceMinorUnits);

            order.setStatus(OrderStatus.EXECUTED);
            order.setExecutedPriceMinorUnits(fillPriceMinorUnits);
            orderRepository.save(order);

            publishFollowUpEvent(event, "OrderExecuted", fillPriceMinorUnits);
        } catch (Exception e) {
            log.error("Order execution failed for {}", order.getOrderId(), e);
            if (event.getSide() == OrderSide.BUY && event.getReservedAmountMinorUnits() != null) {
                walletService.releaseReservation(event.getUserId(), event.getReservedAmountMinorUnits());
            }
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            publishFollowUpEvent(event, "OrderFailed", null);
        }
    }

    private void publishFollowUpEvent(OrderEventPayload original, String eventType, Long executedPriceMinorUnits) {
        try {
            OrderEventPayload followUp = OrderEventPayload.builder()
                    .eventType(eventType)
                    .orderId(original.getOrderId())
                    .userId(original.getUserId())
                    .ticker(original.getTicker())
                    .quantity(original.getQuantity())
                    .side(original.getSide())
                    .executedPriceMinorUnits(executedPriceMinorUnits)
                    .build();

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(original.getOrderId().toString())
                    .type(eventType)
                    .payload(objectMapper.writeValueAsString(followUp))
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to publish follow-up outbox event for order {}", original.getOrderId(), e);
        }
    }
}
