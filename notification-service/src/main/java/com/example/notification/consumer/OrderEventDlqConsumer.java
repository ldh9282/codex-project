package com.example.notification.consumer;

import com.example.common.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrderEventDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventDlqConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.order-created-dlq}",
            groupId = "${spring.kafka.consumer.group-id}-dlq",
            containerFactory = "dlqKafkaListenerContainerFactory"
    )
    public void consumeDlq(
            OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        // 실무에서는 DLQ 컨슈머에서 재처리 워커로 전달하거나 운영 알람(Slack/PagerDuty)을 전송한다.
        log.error(
                "DLQ event received. topic={}, partition={}, offset={}, eventId={}, orderId={}, customerId={}",
                topic, partition, offset, event.eventId(), event.orderId(), event.customerId()
        );
    }
}
