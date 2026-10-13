package com.tekion.interview.stockTradingPlatform.service.impl;

import com.tekion.interview.stockTradingPlatform.dto.response.HoldingDto;
import com.tekion.interview.stockTradingPlatform.dto.response.PortfolioResponse;
import com.tekion.interview.stockTradingPlatform.dto.response.StockPriceResponse;
import com.tekion.interview.stockTradingPlatform.entity.PortfolioEntry;
import com.tekion.interview.stockTradingPlatform.entity.PortfolioEntryKey;
import com.tekion.interview.stockTradingPlatform.entity.enums.OrderSide;
import com.tekion.interview.stockTradingPlatform.repository.PortfolioRepository;
import com.tekion.interview.stockTradingPlatform.service.PortfolioService;
import com.tekion.interview.stockTradingPlatform.service.StockPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.cql.WriteResult;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioServiceImpl implements PortfolioService {

    private static final int MAX_CAS_RETRIES = 5;

    private final PortfolioRepository portfolioRepository;
    private final CassandraTemplate cassandraTemplate;
    private final StockPriceService stockPriceService;

    @Override
    public PortfolioResponse getPortfolio(UUID userId, String range) {
        List<PortfolioEntry> entries = portfolioRepository.findByUserId(userId);

        List<HoldingDto> holdings = entries.stream()
                .filter(e -> e.getQuantity() > 0)
                .map(this::toHoldingDto)
                .toList();

        return PortfolioResponse.builder()
                .userId(userId)
                .range(range)
                .holdings(holdings)
                .asOf(Instant.now())
                .build();
    }

    private HoldingDto toHoldingDto(PortfolioEntry entry) {
        BigDecimal avgPrice = toRupees(entry.getAvgPriceMinorUnits());
        BigDecimal currentPrice;
        try {
            StockPriceResponse priceResponse = stockPriceService.getPrice(entry.getKey().getStockTicker());
            currentPrice = priceResponse.getPrice();
        } catch (Exception e) {
            log.warn("Price lookup failed for {}, falling back to avg price", entry.getKey().getStockTicker(), e);
            currentPrice = avgPrice;
        }
        BigDecimal unrealizedPnl = currentPrice.subtract(avgPrice).multiply(BigDecimal.valueOf(entry.getQuantity()));

        return HoldingDto.builder()
                .ticker(entry.getKey().getStockTicker())
                .quantity(entry.getQuantity())
                .avgPrice(avgPrice)
                .currentPrice(currentPrice)
                .unrealizedPnl(unrealizedPnl)
                .build();
    }

    /**
     * Cassandra has no cross-partition transaction, but this update is single-partition
     * (partition key = userId), so a lightweight transaction (IF version = ?) gives us a
     * real compare-and-set here. Retries on CAS contention with a small bounded loop -
     * two fills for the same user+ticker landing at the exact same moment is the case
     * this protects against.
     */
    @Override
    public void applyFill(UUID userId, String ticker, long quantity, OrderSide side, long priceMinorUnits) {
        PortfolioEntryKey key = new PortfolioEntryKey(userId, ticker);

        for (int attempt = 0; attempt < MAX_CAS_RETRIES; attempt++) {
            PortfolioEntry existing = cassandraTemplate.selectOne(
                    Query.query(Criteria.where("user_id").is(userId).and("stock_ticker").is(ticker)),
                    PortfolioEntry.class);

            if (existing == null) {
                if (side == OrderSide.SELL) {
                    throw new IllegalStateException("Fill received for SELL with no existing position: " + userId + "/" + ticker);
                }
                PortfolioEntry fresh = PortfolioEntry.builder()
                        .key(key)
                        .quantity(quantity)
                        .avgPriceMinorUnits(priceMinorUnits)
                        .version(1)
                        .updatedAt(Instant.now())
                        .build();
                // LWT insert - IF NOT EXISTS - guards the same race for the very first fill on a ticker.
                WriteResult result = cassandraTemplate.insert(fresh, InsertOptions.builder().withIfNotExists().build());
                if (result.wasApplied()) return;
                continue; // someone else inserted first - retry as an update
            }

            long newQuantity;
            long newAvgPrice;
            if (side == OrderSide.BUY) {
                newQuantity = existing.getQuantity() + quantity;
                // weighted average price
                newAvgPrice = Math.round(
                        (existing.getAvgPriceMinorUnits() * (double) existing.getQuantity()
                                + priceMinorUnits * (double) quantity) / newQuantity);
            } else {
                newQuantity = existing.getQuantity() - quantity;
                if (newQuantity < 0) {
                    throw new IllegalStateException("Fill would drive negative holdings: " + userId + "/" + ticker);
                }
                newAvgPrice = existing.getAvgPriceMinorUnits(); // avg cost basis unchanged on a sell
            }

            // Spring Data Cassandra's fluent Query/Update API has no clean way to express an
            // "IF version = ?" lightweight-transaction clause (CQL's IF is a distinct clause
            // after WHERE, not a WHERE predicate) - raw CQL is clearer and correct here.
            String cql = "UPDATE portfolio SET quantity = ?, avg_price_minor_units = ?, version = ?, "
                    + "updated_at = ? WHERE user_id = ? AND stock_ticker = ? IF version = ?";
            com.datastax.oss.driver.api.core.cql.ResultSet rs = cassandraTemplate.getCqlOperations().queryForResultSet(
                    cql,
                    newQuantity, newAvgPrice, existing.getVersion() + 1, Instant.now(),
                    userId, ticker, existing.getVersion());

            boolean applied = rs.one().getBoolean("[applied]");
            if (applied) {
                return;
            }
            log.debug("CAS contention updating portfolio {}/{}, retrying (attempt {})", userId, ticker, attempt);
        }

        throw new IllegalStateException("Failed to apply fill after " + MAX_CAS_RETRIES + " CAS retries: " + userId + "/" + ticker);
    }

    private BigDecimal toRupees(long minorUnits) {
        return BigDecimal.valueOf(minorUnits).divide(BigDecimal.valueOf(100), new MathContext(10));
    }
}
