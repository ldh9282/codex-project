package com.example.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
        String eventId,
        String orderId,
        String customerId,
        String customerEmail,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt
) {
}
