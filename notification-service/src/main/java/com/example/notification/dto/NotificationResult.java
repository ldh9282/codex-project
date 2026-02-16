package com.example.notification.dto;

import com.example.notification.domain.NotificationStatus;

import java.time.Instant;

public record NotificationResult(
        String eventId,
        String orderId,
        NotificationStatus status,
        Instant processedAt,
        String detail
) {
}
