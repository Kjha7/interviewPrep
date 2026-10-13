package com.tekion.interview.stockTradingPlatform.marketdata;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Central, application-scoped (singleton by default Spring bean scope) connector to the
 * stock exchange's market-data feed. There is exactly ONE of these per app instance -
 * it is NOT created per user session/login, and it is NOT tied to any one HTTP request.
 *
 * Responsibilities:
 *  - own the exchange WebSocket connection lifecycle (connect at startup, reconnect w/ backoff)
 *  - cache the last known price per ticker for synchronous reads (GET /stocks/{ticker}/price)
 *  - fan out ticks to interested client STOMP sessions via SimpMessagingTemplate
 *
 * This is boilerplate: ExchangeWebSocketHandler.onMessage() below is a stub where real
 * exchange wire-format parsing would go.
 */
@Service
@Slf4j
public class MarketDataService {

    private final StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
    private final SimpMessagingTemplate messagingTemplate;
    private final String exchangeWebSocketUrl;

    private final Map<String, BigDecimal> lastPriceByTicker = new ConcurrentHashMap<>();
    private final Set<String> subscribedTickers = new CopyOnWriteArraySet<>();

    public MarketDataService(
            SimpMessagingTemplate messagingTemplate,
            @Value("${app.exchange.websocket-url}") String exchangeWebSocketUrl) {
        this.messagingTemplate = messagingTemplate;
        this.exchangeWebSocketUrl = exchangeWebSocketUrl;
    }

    @PostConstruct
    public void connect() {
        try {
            webSocketClient.execute(
                    new ExchangeWebSocketHandler(this),
                    URI.create(exchangeWebSocketUrl).toString());
            log.info("MarketDataService: connecting to exchange feed at {}", exchangeWebSocketUrl);
        } catch (Exception e) {
            // TODO: exponential backoff + scheduled retry, don't fail app startup on a down exchange.
            log.error("MarketDataService: initial connect failed, will need reconnect logic", e);
        }
    }

    @PreDestroy
    public void disconnect() {
        // TODO: close the underlying WebSocketSession gracefully.
        log.info("MarketDataService: shutting down exchange connection");
    }

    /** Called by ExchangeWebSocketHandler when a price tick arrives from the exchange. */
    void onPriceTick(String ticker, BigDecimal price) {
        lastPriceByTicker.put(ticker, price);
        if (subscribedTickers.contains(ticker)) {
            messagingTemplate.convertAndSend("/topic/price/" + ticker, price);
        }
    }

    public void subscribe(String ticker) {
        subscribedTickers.add(ticker);
        // TODO: send an actual subscribe frame to the exchange over exchangeSession if it
        // uses a subscribe-per-ticker protocol rather than a full firehose.
    }

    /** Synchronous read for GET /stocks/{ticker}/price - backed by the cache, not a live round trip. */
    public BigDecimal getLastPrice(String ticker) {
        BigDecimal price = lastPriceByTicker.get(ticker);
        if (price == null) {
            throw new IllegalStateException("No price cached for ticker " + ticker + " - not subscribed or exchange unavailable");
        }
        return price;
    }
}
