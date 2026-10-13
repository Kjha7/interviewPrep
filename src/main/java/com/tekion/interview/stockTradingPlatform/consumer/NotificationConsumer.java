package com.tekion.interview.stockTradingPlatform.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tekion.interview.stockTradingPlatform.config.KafkaTopicConfig;
import com.tekion.interview.stockTradingPlatform.dto.event.OrderEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Independent consumer group ("notification-consumer-group") reading the SAME Kafka topic
 * as PaymentConsumer. This is the point of Kafka pub-sub here: PaymentConsumer and
 * NotificationConsumer both see every event, on their own offsets, with no coordination
 * between them - one doing money-moving work, the other doing best-effort UX pushes.
 * A dropped/delayed notification is not a correctness problem the way a dropped
 * payment-side event would be, which is precisely why they're split into separate
 * consumer groups rather than one handler doing both jobs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC,
            groupId = "notification-consumer-group")
    public void onOrderEvent(String rawPayload) throws Exception {
        OrderEventPayload event = objectMapper.readValue(rawPayload, OrderEventPayload.class);

        // Push to the per-user private queue - client subscribes to /user/queue/orders after
        // connecting to /ws/trading (see ClientWebSocketConfig).
        messagingTemplate.convertAndSendToUser(
                event.getUserId().toString(),
                "/queue/orders",
                event);

        // TODO: fan out to email/SMS/push here too, or delegate to a dedicated notification
        // service - this stub only covers the in-app real-time channel.
        log.debug("Notified user {} of {} for order {}", event.getUserId(), event.getEventType(), event.getOrderId());
    }
}
