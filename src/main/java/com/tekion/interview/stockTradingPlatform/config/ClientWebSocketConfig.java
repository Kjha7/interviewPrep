package com.tekion.interview.stockTradingPlatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * This is the client <-> TradingApp channel: order fill notifications and subscribed
 * price ticks pushed to a logged-in user's browser/app session.
 *
 * It is intentionally separate from MarketDataService, which owns the single long-lived
 * connection to the stock exchange itself (see marketdata package). Do not conflate the two -
 * one exchange connection fans out to many client sessions here, it does not get re-opened
 * per user.
 */
@Configuration
@EnableWebSocketMessageBroker
public class ClientWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // swap for a Redis/RabbitMQ broker relay if scaling beyond one instance
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/trading")
                .setAllowedOriginPatterns("*"); // tighten in production
    }
}
