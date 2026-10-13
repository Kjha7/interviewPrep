package com.tekion.interview.stockTradingPlatform.repository;

import com.tekion.interview.stockTradingPlatform.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Application code only ever INSERTs here (inside the same transaction as the Order write).
 * Reading/publishing is Debezium's job via CDC on the WAL - do not add a poll-and-publish
 * loop against this table, that reintroduces the dual-write problem the outbox pattern exists
 * to avoid.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
