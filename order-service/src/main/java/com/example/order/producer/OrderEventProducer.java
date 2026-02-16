package com.example.order.producer;

import com.example.common.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final String orderTopic;

    public OrderEventProducer(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.order-created}") String orderTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderTopic = orderTopic;
    }

    public void sendOrderCreated(OrderCreatedEvent event) {
        // key를 orderId로 지정하면 같은 주문 이벤트는 동일 파티션에 기록되어 순서를 보장하기 쉽다.
        CompletableFuture<SendResult<String, OrderCreatedEvent>> future = kafkaTemplate.send(orderTopic, event.orderId(), event);
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Failed to publish order event. orderId={}, eventId={}", event.orderId(), event.eventId(), throwable);
                return;
            }
            log.info(
                    "Published order event. orderId={}, eventId={}, topic={}, partition={}, offset={}",
                    event.orderId(),
                    event.eventId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }
}
