package com.example.product.dto;

import java.time.Instant;

public record CreateProductResponse(
        String productId,
        String eventId,
        String status,
        Instant createdAt
) {
}
