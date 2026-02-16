package com.example.order.dto;

import java.time.Instant;

public record CreateOrderResponse(
        String orderId,
        String eventId,
        String status,
        Instant createdAt
) {
}
