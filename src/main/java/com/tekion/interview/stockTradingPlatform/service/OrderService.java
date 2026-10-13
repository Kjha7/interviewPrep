package com.tekion.interview.stockTradingPlatform.service;

import com.tekion.interview.stockTradingPlatform.dto.request.PlaceOrderRequest;
import com.tekion.interview.stockTradingPlatform.dto.response.OrderResponse;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderSide;

import java.util.UUID;

public interface OrderService {

    OrderResponse placeOrder(UUID userId, OrderSide side, PlaceOrderRequest request, String idempotencyKey);

    OrderResponse getStatus(UUID userId, UUID orderId);
}
