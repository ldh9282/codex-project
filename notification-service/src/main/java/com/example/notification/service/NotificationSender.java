package com.example.notification.service;

import com.example.common.event.OrderCreatedEvent;
import com.example.common.event.OrderShippedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    public void sendOrderConfirmation(OrderCreatedEvent event) {
        if (event.customerId().startsWith("fail-")) {
            log.error("Simulating downstream failure for customerId={}", event.customerId());
            throw new IllegalStateException("Simulated downstream failure for customerId=" + event.customerId());
        }

        log.info(
                "Notification sent. eventId={}, orderId={}, customerEmail={}, amount={} {}",
                event.eventId(), event.orderId(), event.customerEmail(), event.totalAmount(), event.currency()
        );
    }

    public void sendOrderShippingUpdate(OrderShippedEvent event) {
        if (event.customerId().startsWith("fail-")) {
            throw new IllegalStateException("Simulated downstream failure for customerId=" + event.customerId());
        }

        log.info(
                "Shipping notification sent. eventId={}, orderId={}, customerEmail={}, previousStatus={}, currentStatus={}",
                event.eventId(), event.orderId(), event.customerEmail(), event.previousStatus(), event.currentStatus()
        );
    }
}
