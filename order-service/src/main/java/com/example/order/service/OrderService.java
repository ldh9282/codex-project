package com.example.order.service;

import com.example.common.event.OrderCreatedEvent;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.CreateOrderResponse;
import com.example.order.producer.OrderEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderEventProducer orderEventProducer;

    public OrderService(OrderEventProducer orderEventProducer) {
        this.orderEventProducer = orderEventProducer;
    }

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        // UUID 기반 orderId/eventId를 발급해 API 레이어와 이벤트 레이어의 추적성을 확보한다.
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

        // 주문 저장소(DB)는 예제에서는 생략했지만, 실무에서는 Outbox 패턴으로 이벤트와 트랜잭션 일관성을 맞추는 것을 권장한다.
        orderEventProducer.sendOrderCreated(event);

        log.info("Order created and event published. orderId={}, eventId={}", orderId, eventId);

        return new CreateOrderResponse(orderId, eventId, "PUBLISHED", now);
    }
}
