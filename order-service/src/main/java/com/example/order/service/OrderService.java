package com.example.order.service;

import com.example.common.event.OrderCreatedEvent;
import com.example.common.event.OrderShippedEvent;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.CreateOrderResponse;
import com.example.order.dto.UpdateOrderStatusRequest;
import com.example.order.dto.UpdateOrderStatusResponse;
import com.example.order.exception.InvalidOrderStatusTransitionException;
import com.example.order.producer.OrderEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int STATUS_ORDER_COMPLETED = 20;
    private static final int STATUS_SHIPPING = 25;
    private static final int STATUS_DELIVERED = 80;
    private static final Set<String> ALLOWED_TRANSITIONS = Set.of("20->25", "25->80");

    private final OrderEventProducer orderEventProducer;

    public OrderService(OrderEventProducer orderEventProducer) {
        this.orderEventProducer = orderEventProducer;
    }

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                orderId,
                request.customerId(),
                request.customerEmail(),
                request.totalAmount(),
                request.currency(),
                now
        );

        orderEventProducer.sendOrderCreated(event);
        log.info("Order created and event published. orderId={}, eventId={}", orderId, eventId);

        return new CreateOrderResponse(orderId, eventId, "PUBLISHED", now);
    }

    public UpdateOrderStatusResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request) {
        String transition = request.previousStatus() + "->" + request.currentStatus();
        if (!ALLOWED_TRANSITIONS.contains(transition)) {
            throw new InvalidOrderStatusTransitionException(
                    "Only 20->25(shipping) and 25->80(delivered) transitions are supported. requested=" + transition
            );
        }

        if (request.previousStatus() == STATUS_ORDER_COMPLETED && request.currentStatus() == STATUS_SHIPPING) {
            return publishOrderShipped(orderId, request);
        }

        if (request.previousStatus() == STATUS_SHIPPING && request.currentStatus() == STATUS_DELIVERED) {
            return publishOrderShipped(orderId, request);
        }

        throw new InvalidOrderStatusTransitionException("Unsupported transition=" + transition);
    }

    private UpdateOrderStatusResponse publishOrderShipped(String orderId, UpdateOrderStatusRequest request) {
        String eventId = UUID.randomUUID().toString();
        Instant changedAt = Instant.now();

        OrderShippedEvent event = new OrderShippedEvent(
                eventId,
                orderId,
                request.customerId(),
                request.customerEmail(),
                request.previousStatus(),
                request.currentStatus(),
                changedAt
        );

        orderEventProducer.sendOrderShipped(event);
        log.info(
                "Order status changed and shipped event published. orderId={}, eventId={}, previousStatus={}, currentStatus={}",
                orderId,
                eventId,
                request.previousStatus(),
                request.currentStatus()
        );

        return new UpdateOrderStatusResponse(
                orderId,
                eventId,
                request.previousStatus(),
                request.currentStatus(),
                "PUBLISHED",
                changedAt
        );
    }
}
