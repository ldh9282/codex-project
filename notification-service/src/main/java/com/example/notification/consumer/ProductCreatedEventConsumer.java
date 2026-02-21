package com.example.notification.consumer;

import com.example.common.event.ProductCreatedEvent;
import com.example.notification.dto.NotificationResult;
import com.example.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ProductCreatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductCreatedEventConsumer.class);

    private final NotificationService notificationService;

    public ProductCreatedEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.product-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "productCreatedKafkaListenerContainerFactory",
            concurrency = "${app.kafka.consumer-concurrency}"
    )
    public void consumeProductCreated(
            ProductCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        NotificationResult result = notificationService.processProductCreated(event);
        log.info(
                "Product created event consumed. topic={}, partition={}, offset={}, eventId={}, status={}, detail={}",
                topic, partition, offset, result.eventId(), result.status(), result.detail()
        );
    }
}
