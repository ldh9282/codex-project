package com.example.order.producer;

import com.example.common.event.OrderCreatedEvent;
import com.example.common.event.OrderShippedEvent;
import com.example.order.exception.EventPublishException;
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

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String orderCreatedTopic;
    private final String orderShippedTopic;

    public OrderEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.order-created}") String orderCreatedTopic,
            @Value("${app.kafka.topics.order-shipped}") String orderShippedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderCreatedTopic = orderCreatedTopic;
        this.orderShippedTopic = orderShippedTopic;
    }

    public void sendOrderCreated(OrderCreatedEvent event) {
        send(orderCreatedTopic, event.orderId(), event, "order-created");
    }

    public void sendOrderShipped(OrderShippedEvent event) {
        send(orderShippedTopic, event.orderId(), event, "order-shipped");
    }

    private void send(String topic, String key, Object event, String eventType) {
        CompletableFuture<SendResult<String, Object>> future;
        try {
            future = kafkaTemplate.send(topic, key, event);
        // key를 orderId로 지정하면 같은 주문 이벤트는 동일 파티션에 기록되어 순서를 보장하기 쉽다.
        CompletableFuture<SendResult<String, OrderCreatedEvent>> future;
        try {
            future = kafkaTemplate.send(orderTopic, event.orderId(), event);
        } catch (Exception exception) {
            throw new EventPublishException("Order event publish failed. Verify Kafka bootstrap server/topic availability.", exception);
        }

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Failed to publish {} event. key={}", eventType, key, throwable);
                return;
            }
            log.info(
                    "Published {} event. key={}, topic={}, partition={}, offset={}",
                    eventType,
                    key,
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }
}
