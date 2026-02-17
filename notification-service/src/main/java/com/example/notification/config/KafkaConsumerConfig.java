package com.example.notification.config;

import com.example.common.event.OrderCreatedEvent;
import com.example.common.event.OrderShippedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId
    ) {
        JsonDeserializer<OrderCreatedEvent> jsonDeserializer = new JsonDeserializer<>(OrderCreatedEvent.class);
        jsonDeserializer.addTrustedPackages("com.example.common.event");
        jsonDeserializer.setUseTypeMapperForKey(false);
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(bootstrapServers, groupId), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConsumerFactory<String, OrderShippedEvent> orderShippedConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId
    ) {
        JsonDeserializer<OrderShippedEvent> jsonDeserializer = new JsonDeserializer<>(OrderShippedEvent.class);
        jsonDeserializer.addTrustedPackages("com.example.common.event");
        jsonDeserializer.setUseTypeMapperForKey(false);
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(bootstrapServers, groupId), new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> orderKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.order-created-dlq}") String dlqTopic
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderCreatedConsumerFactory);
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate, dlqTopic));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderShippedEvent> orderShippedKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderShippedEvent> orderShippedConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.order-shipped-dlq}") String dlqTopic
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderShippedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderShippedConsumerFactory);
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate, dlqTopic));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> dlqKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderCreatedConsumerFactory);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderShippedEvent> orderShippedDlqKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderShippedEvent> orderShippedConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderShippedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderShippedConsumerFactory);
        return factory;
    }

    @Bean
    public NewTopic orderCreatedTopic(
            @Value("${app.kafka.topics.order-created}") String topic,
            @Value("${app.kafka.partitions}") int partitions,
            @Value("${app.kafka.replication-factor}") short replicationFactor
    ) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicationFactor).build();
    }

    @Bean
    public NewTopic orderCreatedDlqTopic(
            @Value("${app.kafka.topics.order-created-dlq}") String topic,
            @Value("${app.kafka.partitions}") int partitions,
            @Value("${app.kafka.replication-factor}") short replicationFactor
    ) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicationFactor).build();
    }

    @Bean
    public NewTopic orderShippedTopic(
            @Value("${app.kafka.topics.order-shipped}") String topic,
            @Value("${app.kafka.partitions}") int partitions,
            @Value("${app.kafka.replication-factor}") short replicationFactor
    ) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicationFactor).build();
    }

    @Bean
    public NewTopic orderShippedDlqTopic(
            @Value("${app.kafka.topics.order-shipped-dlq}") String topic,
            @Value("${app.kafka.partitions}") int partitions,
            @Value("${app.kafka.replication-factor}") short replicationFactor
    ) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicationFactor).build();
    }

    private Map<String, Object> consumerConfigs(String bootstrapServers, String groupId) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return configs;
    }

    private DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate, String dlqTopic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    log.error(
                            "Message moved to DLQ. topic={}, partition={}, offset={}, key={}",
                            record.topic(), record.partition(), record.offset(), record.key(), exception
                    );
                    return new TopicPartition(dlqTopic, record.partition());
                }
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3L));
    }
}
