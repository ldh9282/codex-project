package com.example.product.service;

import com.example.common.event.ProductCreatedEvent;
import com.example.product.dto.CreateProductRequest;
import com.example.product.dto.CreateProductResponse;
import com.example.product.producer.ProductEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductEventProducer productEventProducer;

    public ProductService(ProductEventProducer productEventProducer) {
        this.productEventProducer = productEventProducer;
    }

    public CreateProductResponse createProduct(CreateProductRequest request) {
        String productId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        ProductCreatedEvent event = new ProductCreatedEvent(
                eventId,
                productId,
                request.productName(),
                request.price(),
                request.currency(),
                request.stockQuantity(),
                now
        );

        productEventProducer.sendProductCreated(event);

        log.info("Product created and event published. productId={}, eventId={}", productId, eventId);

        return new CreateProductResponse(productId, eventId, "PUBLISHED", now);
    }
}
