package com.tekion.interview.stockTradingPlatform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite PK (userId, orderId). Postgres native declarative partitioning
 * (PARTITION BY HASH (user_id)) requires the partition key to be part of the
 * primary key - see docker/schema-partitioning.sql for the DDL this maps to.
 * This is what "shard by user_id" becomes on Postgres: hash partitioning,
 * not a separate physical shard per user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderId implements Serializable {
    private UUID userId;
    private UUID orderId;
}
