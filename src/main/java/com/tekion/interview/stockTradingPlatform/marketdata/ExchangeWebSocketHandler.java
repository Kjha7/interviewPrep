package com.tekion.interview.stockTradingPlatform.marketdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Stub parser for the exchange's tick format. Real implementation depends entirely on
 * the exchange's actual protocol (FIX-over-WS, a proprietary JSON schema, etc.) - wire
 * that parsing in here and call back into MarketDataService.onPriceTick(...).
 */
@Slf4j
public class ExchangeWebSocketHandler extends TextWebSocketHandler {

    private final MarketDataService marketDataService;

    public ExchangeWebSocketHandler(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Connected to exchange market data feed: session={}", session.getId());
        // TODO: send auth/subscribe handshake frame(s) per the exchange's protocol.
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // TODO: replace with real parsing, e.g.:
        // TickMessage tick = objectMapper.readValue(message.getPayload(), TickMessage.class);
        // marketDataService.onPriceTick(tick.ticker(), tick.price());
        log.debug("Raw exchange frame received: {}", message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Exchange WebSocket transport error", exception);
        // TODO: trigger reconnect-with-backoff from MarketDataService.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("Exchange WebSocket connection closed: {}", status);
        // TODO: trigger reconnect-with-backoff from MarketDataService.
    }
}
