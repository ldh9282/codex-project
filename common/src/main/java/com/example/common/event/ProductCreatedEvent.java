package com.example.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductCreatedEvent(
        String eventId,
        String productId,
        String productName,
        BigDecimal price,
        String currency,
        int stockQuantity,
        Instant createdAt
) {
}
