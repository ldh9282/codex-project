package com.example.notification.consumer;

import com.example.common.event.OrderShippedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrderShippedEventDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderShippedEventDlqConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.order-shipped-dlq}",
            groupId = "${spring.kafka.consumer.group-id}-dlq",
            containerFactory = "orderShippedDlqKafkaListenerContainerFactory"
    )
    public void consumeDlq(
            OrderShippedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.error(
                "Order shipped DLQ event received. topic={}, partition={}, offset={}, eventId={}, orderId={}, currentStatus={}",
                topic, partition, offset, event.eventId(), event.orderId(), event.currentStatus()
        );
    }
}
