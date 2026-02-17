package com.example.product.producer;

import com.example.common.event.ProductCreatedEvent;
import com.example.product.exception.EventPublishException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ProductEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventProducer.class);

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
    private final String productCreatedTopic;

    public ProductEventProducer(
            KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.product-created}") String productCreatedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.productCreatedTopic = productCreatedTopic;
    }

    public void sendProductCreated(ProductCreatedEvent event) {
        CompletableFuture<SendResult<String, ProductCreatedEvent>> future;
        try {
            future = kafkaTemplate.send(productCreatedTopic, event.productId(), event);
        } catch (Exception exception) {
            throw new EventPublishException("Product event publish failed. Verify Kafka bootstrap server/topic availability.", exception);
        }

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Failed to publish product event. productId={}, eventId={}", event.productId(), event.eventId(), throwable);
                return;
            }
            log.info(
                    "Published product event. productId={}, eventId={}, topic={}, partition={}, offset={}",
                    event.productId(),
                    event.eventId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }
}
