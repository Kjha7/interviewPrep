package com.tekion.interview.stockTradingPlatform.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Debezium's outbox event router publishes to a topic named after "aggregateType"
 * by default (outbox.event.<aggregateType>, configurable - see
 * docker/debezium-connector-config.json). These beans are only needed if you want
 * Spring Boot to auto-create the topics on startup in dev; in prod topics are usually
 * managed by the platform/Debezium config directly.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_EVENTS_TOPIC = "outbox.event.Order";

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS_TOPIC)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
