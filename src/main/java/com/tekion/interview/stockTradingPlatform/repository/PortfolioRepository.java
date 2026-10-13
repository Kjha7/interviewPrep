package com.tekion.interview.stockTradingPlatform.repository;

import com.tekion.interview.stockTradingPlatform.entity.PortfolioEntry;
import com.tekion.interview.stockTradingPlatform.entity.PortfolioEntryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PortfolioRepository extends CassandraRepository<PortfolioEntry, PortfolioEntryKey> {

    // Partition-scoped query - reads all clustering rows (tickers) under one user_id partition.
    // No AllowFiltering needed since user_id is the partition key.
    @Query("SELECT * FROM portfolio WHERE user_id = ?0")
    List<PortfolioEntry> findByUserId(UUID userId);
}
