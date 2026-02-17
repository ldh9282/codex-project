package com.example.notification.consumer;

import com.example.common.event.OrderShippedEvent;
import com.example.notification.dto.NotificationResult;
import com.example.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrderShippedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderShippedEventConsumer.class);

    private final NotificationService notificationService;

    public OrderShippedEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-shipped}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderShippedKafkaListenerContainerFactory",
            concurrency = "${app.kafka.consumer-concurrency}"
    )
    public void consumeOrderShipped(
            OrderShippedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        NotificationResult result = notificationService.processOrderShipped(event);
        log.info(
                "Order shipped event consumed. topic={}, partition={}, offset={}, eventId={}, status={}, detail={}",
                topic, partition, offset, result.eventId(), result.status(), result.detail()
        );
    }
}
