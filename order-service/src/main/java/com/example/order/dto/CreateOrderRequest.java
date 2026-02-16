package com.example.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank(message = "customerId is required")
        String customerId,
        @NotBlank(message = "customerEmail is required")
        @Email(message = "customerEmail must be valid")
        String customerEmail,
        @NotNull(message = "totalAmount is required")
        @DecimalMin(value = "0.01", message = "totalAmount must be positive")
        BigDecimal totalAmount,
        @NotBlank(message = "currency is required")
        String currency
) {
}
