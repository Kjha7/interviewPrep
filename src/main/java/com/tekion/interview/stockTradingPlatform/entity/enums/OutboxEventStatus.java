package com.tekion.interview.stockTradingPlatform.entity.enums;

/**
 * NEW_AND_UNPUBLISHED / PUBLISHED are only relevant if you poll the outbox table yourself.
 * With Debezium CDC reading the WAL directly, this column is informational only -
 * Debezium doesn't need it to do its job, but it's useful for manual reconciliation/debugging.
 */
public enum OutboxEventStatus {
    NEW,
    PUBLISHED
}
