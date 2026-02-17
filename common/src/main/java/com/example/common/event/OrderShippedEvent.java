package com.example.common.event;

import java.time.Instant;

public record OrderShippedEvent(
        String eventId,
        String orderId,
        String customerId,
        String customerEmail,
        int previousStatus,
        int currentStatus,
        Instant changedAt
) {
}
