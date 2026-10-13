package com.tekion.interview.stockTradingPlatform.dto.response;

import com.tekion.interview.stockTradingPlatform.entity.enums.OrderSide;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID orderId;
    private String ticker;
    private long quantity;
    private OrderSide side;
    private OrderStatus status;
    private BigDecimal executedPrice; // null until EXECUTED
    private Instant acceptedAt;
    private Instant updatedAt;
}
