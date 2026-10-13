package com.tekion.interview.stockTradingPlatform.controller;

import com.tekion.interview.stockTradingPlatform.dto.request.PlaceOrderRequest;
import com.tekion.interview.stockTradingPlatform.dto.response.OrderResponse;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderSide;
import com.tekion.interview.stockTradingPlatform.exception.ApiException;
import com.tekion.interview.stockTradingPlatform.security.AuthTokenFilter;
import com.tekion.interview.stockTradingPlatform.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public ResponseEntity<OrderResponse> buy(
            @Valid @RequestBody PlaceOrderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestAttribute(AuthTokenFilter.AUTHENTICATED_USER_ID_ATTR) UUID userId) {
        return place(userId, OrderSide.BUY, request, idempotencyKey);
    }

    @PostMapping("/sell")
    public ResponseEntity<OrderResponse> sell(
            @Valid @RequestBody PlaceOrderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestAttribute(AuthTokenFilter.AUTHENTICATED_USER_ID_ATTR) UUID userId) {
        return place(userId, OrderSide.SELL, request, idempotencyKey);
    }

    private ResponseEntity<OrderResponse> place(UUID userId, OrderSide side, PlaceOrderRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required");
        }
        OrderResponse response = orderService.placeOrder(userId, side, request, idempotencyKey);
        // 202, not 200 - execution against the exchange is asynchronous (see PaymentConsumer).
        // Client polls GET /orders/{orderId} or listens on /user/queue/orders over STOMP.
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getStatus(
            @PathVariable UUID orderId,
            @RequestAttribute(AuthTokenFilter.AUTHENTICATED_USER_ID_ATTR) UUID userId) {
        return ResponseEntity.ok(orderService.getStatus(userId, orderId));
    }
}
