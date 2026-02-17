package com.example.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotBlank(message = "customerId is required")
        String customerId,
        @NotBlank(message = "customerEmail is required")
        @Email(message = "customerEmail must be valid")
        String customerEmail,
        @NotNull(message = "previousStatus is required")
        Integer previousStatus,
        @NotNull(message = "currentStatus is required")
        Integer currentStatus
) {
}
