package com.example.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "productName is required")
        String productName,
        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be positive")
        BigDecimal price,
        @NotBlank(message = "currency is required")
        String currency,
        @Min(value = 0, message = "stockQuantity must be zero or positive")
        int stockQuantity
) {
}
