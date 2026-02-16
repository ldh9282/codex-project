package com.example.common.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;

public final class KafkaEventSerDe {

    private KafkaEventSerDe() {
    }

    public static String toJson(ObjectMapper objectMapper, Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new SerializationException("Failed to serialize Kafka payload", exception);
        }
    }
}
