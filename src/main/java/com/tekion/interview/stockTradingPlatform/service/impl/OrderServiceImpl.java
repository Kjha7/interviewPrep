package com.tekion.interview.stockTradingPlatform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tekion.interview.stockTradingPlatform.dto.request.PlaceOrderRequest;
import com.tekion.interview.stockTradingPlatform.dto.response.OrderResponse;
import com.tekion.interview.stockTradingPlatform.entity.Order;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderSide;
import com.tekion.interview.stockTradingPlatform.exception.ApiException;
import com.tekion.interview.stockTradingPlatform.repository.OrderRepository;
import com.tekion.interview.stockTradingPlatform.service.IdempotencyService;
import com.tekion.interview.stockTradingPlatform.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;
    private final OrderTransactionalWriter orderTransactionalWriter;
    private final ObjectMapper objectMapper;

    @Override
    public OrderResponse placeOrder(UUID userId, OrderSide side, PlaceOrderRequest request, String idempotencyKey) {
        String requestHash = hashRequest(userId, side, request);

        IdempotencyService.Result idem = idempotencyService.checkAndReserve(idempotencyKey, requestHash);
        switch (idem.outcome()) {
            case CONFLICT -> throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT",
                    "This Idempotency-Key was already used with a different request body");
            case REPLAY_COMPLETE -> {
                return deserializeResponse(idem.storedResponseJson());
            }
            case REPLAY_IN_PROGRESS -> {
                // Simplest correct boilerplate behavior: tell the client to retry the read shortly
                // rather than block the request thread. A production version might poll with a
                // short bounded wait before giving up.
                throw new ApiException(HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS",
                        "An identical request is already being processed - retry GET /orders/{orderId} shortly");
            }
            case PROCEED -> { /* fall through to normal processing */ }
        }

        // Delegated to a separate bean so @Transactional actually applies through the Spring
        // proxy (see OrderTransactionalWriter's javadoc) - the Redis idempotency check above is
        // intentionally OUTSIDE that DB transaction.
        OrderResponse response = orderTransactionalWriter.createOrderAndPublishEvent(userId, side, request, idempotencyKey);
        idempotencyService.complete(idempotencyKey, serializeResponse(response));
        return response;
    }

    @Override
    public OrderResponse getStatus(UUID userId, UUID orderId) {
        Order order = orderRepository.findByUserIdAndOrderId(userId, orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "No such order for this user"));

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .ticker(order.getStockTicker())
                .quantity(order.getQuantity())
                .side(order.getSide())
                .status(order.getStatus())
                .executedPrice(order.getExecutedPriceMinorUnits() == null
                        ? null : BigDecimal.valueOf(order.getExecutedPriceMinorUnits(), 2))
                .acceptedAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private String hashRequest(UUID userId, OrderSide side, PlaceOrderRequest request) {
        String raw = userId + "|" + side + "|" + request.getTicker() + "|" + request.getQuantity()
                + "|" + request.getOrderType() + "|" + request.getLimitPriceMinorUnits();
        return sha256(raw);
    }

    @SneakyThrows
    private String sha256(String input) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(input.getBytes()));
    }

    @SneakyThrows
    private String serializeResponse(OrderResponse response) {
        return objectMapper.writeValueAsString(response);
    }

    @SneakyThrows
    private OrderResponse deserializeResponse(String json) {
        return objectMapper.readValue(json, OrderResponse.class);
    }
}
