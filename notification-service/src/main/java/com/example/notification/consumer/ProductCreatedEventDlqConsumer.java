package com.example.notification.consumer;

import com.example.common.event.ProductCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ProductCreatedEventDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductCreatedEventDlqConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.product-created-dlq}",
            groupId = "${spring.kafka.consumer.group-id}-dlq",
            containerFactory = "productCreatedDlqKafkaListenerContainerFactory"
    )
    public void consumeDlq(
            ProductCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.error(
                "Product created DLQ event received. topic={}, partition={}, offset={}, eventId={}, productId={}, productName={}",
                topic, partition, offset, event.eventId(), event.productId(), event.productName()
        );
    }
}
