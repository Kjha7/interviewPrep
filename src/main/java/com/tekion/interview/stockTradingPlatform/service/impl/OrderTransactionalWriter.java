package com.tekion.interview.stockTradingPlatform.service.impl;

import com.tekion.interview.stockTradingPlatform.dto.event.OrderEventPayload;
import com.tekion.interview.stockTradingPlatform.dto.request.PlaceOrderRequest;
import com.tekion.interview.stockTradingPlatform.dto.response.OrderResponse;
import com.tekion.interview.stockTradingPlatform.entity.Order;
import com.tekion.interview.stockTradingPlatform.entity.OutboxEvent;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderSide;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderStatus;
import com.tekion.interview.stockTradingPlatform.exception.InsufficientHoldingsException;
import com.tekion.interview.stockTradingPlatform.repository.OrderRepository;
import com.tekion.interview.stockTradingPlatform.repository.OutboxEventRepository;
import com.tekion.interview.stockTradingPlatform.service.PortfolioService;
import com.tekion.interview.stockTradingPlatform.service.StockPriceService;
import com.tekion.interview.stockTradingPlatform.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Pulled out of OrderServiceImpl into its own Spring bean deliberately - if
 * createOrderAndPublishEvent lived on OrderServiceImpl and were called as
 * this.createOrderAndPublishEvent(...) from placeOrder() in the same class, Spring's
 * proxy-based @Transactional would never actually apply (self-invocation bypasses the
 * proxy). Being a separate injected bean means OrderServiceImpl calls through the real
 * Spring-managed proxy, so the transaction boundary is real.
 */
@Component
@RequiredArgsConstructor
class OrderTransactionalWriter {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final WalletService walletService;
    private final PortfolioService portfolioService;
    private final StockPriceService stockPriceService;
    private final ObjectMapper objectMapper;

    @Transactional
    OrderResponse createOrderAndPublishEvent(UUID userId, OrderSide side, PlaceOrderRequest request, String idempotencyKey) {
        long estimatedPriceMinorUnits = stockPriceService.getPrice(request.getTicker())
                .getPrice()
                .movePointRight(2)
                .longValueExact();
        long estimatedAmountMinorUnits = estimatedPriceMinorUnits * request.getQuantity();

        Long reservedAmount = null;
        if (side == OrderSide.BUY) {
            walletService.reserveFunds(userId, estimatedAmountMinorUnits); // throws InsufficientFundsException
            reservedAmount = estimatedAmountMinorUnits;
        } else {
            // Best-effort pre-check against the (eventually consistent) Cassandra portfolio
            // projection. This is a UX-level fast-fail, NOT the source of truth for correctness -
            // see design-doc feedback: a real implementation needs a reserved-quantity column on
            // Portfolio (mirroring Wallet.reservedMinorUnits) so two concurrent sells of the same
            // last share can't both pass this check. Flagged as a known gap, not silently fixed,
            // since it changes the Portfolio schema beyond what was asked for here.
            boolean hasEnough = portfolioService.getPortfolio(userId, null).getHoldings().stream()
                    .anyMatch(h -> h.getTicker().equals(request.getTicker()) && h.getQuantity() >= request.getQuantity());
            if (!hasEnough) {
                throw new InsufficientHoldingsException(request.getTicker());
            }
        }

        Order order = Order.builder()
                .userId(userId)
                .stockTicker(request.getTicker())
                .quantity(request.getQuantity())
                .side(side)
                .status(OrderStatus.PENDING)
                .limitPriceMinorUnits(request.getLimitPriceMinorUnits())
                .idempotencyKey(idempotencyKey)
                .build();
        order = orderRepository.save(order);

        OrderEventPayload eventPayload = OrderEventPayload.builder()
                .eventType("OrderAccepted")
                .orderId(order.getOrderId())
                .userId(userId)
                .ticker(request.getTicker())
                .quantity(request.getQuantity())
                .side(side)
                .limitPriceMinorUnits(request.getLimitPriceMinorUnits())
                .reservedAmountMinorUnits(reservedAmount)
                .build();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("Order")
                .aggregateId(order.getOrderId().toString())
                .type("OrderAccepted")
                .payload(writeJson(eventPayload))
                .build();
        outboxEventRepository.save(outboxEvent);

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .ticker(order.getStockTicker())
                .quantity(order.getQuantity())
                .side(order.getSide())
                .status(order.getStatus())
                .acceptedAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    @SneakyThrows
    private String writeJson(Object o) {
        return objectMapper.writeValueAsString(o);
    }
}
