package com.example.notification.service;

import com.example.common.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    public void sendOrderConfirmation(OrderCreatedEvent event) {
        // 실제 현업에서는 외부 Email/SMS API를 호출하고, HTTP 상태코드 기반 예외 매핑을 구현한다.
        // 데모에서는 customerId가 fail-로 시작하면 강제 실패시켜 retry + DLQ 동작을 재현한다.
        if (event.customerId().startsWith("fail-")) {
            throw new IllegalStateException("Simulated downstream failure for customerId=" + event.customerId());
        }

        log.info(
                "Notification sent. eventId={}, orderId={}, customerEmail={}, amount={} {}",
                event.eventId(), event.orderId(), event.customerEmail(), event.totalAmount(), event.currency()
        );
    }
}
