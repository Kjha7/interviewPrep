package com.tekion.interview.stockTradingPlatform.repository;

import com.tekion.interview.stockTradingPlatform.entity.Order;
import com.tekion.interview.stockTradingPlatform.entity.OrderId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, OrderId> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    // Always scoped by userId - the partition key - to keep every query single-partition.
    List<Order> findByUserId(UUID userId);

    Optional<Order> findByUserIdAndOrderId(UUID userId, UUID orderId);
}
