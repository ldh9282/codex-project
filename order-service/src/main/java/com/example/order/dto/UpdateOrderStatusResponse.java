package com.example.order.dto;

import java.time.Instant;

public record UpdateOrderStatusResponse(
        String orderId,
        String eventId,
        int previousStatus,
        int currentStatus,
        String status,
        Instant changedAt
) {
}
