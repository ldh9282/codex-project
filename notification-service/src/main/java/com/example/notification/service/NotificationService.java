package com.example.notification.service;

import com.example.common.event.OrderCreatedEvent;
import com.example.notification.domain.NotificationStatus;
import com.example.notification.dto.NotificationResult;
import com.example.notification.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationSender notificationSender;

    public NotificationService(ProcessedEventRepository processedEventRepository, NotificationSender notificationSender) {
        this.processedEventRepository = processedEventRepository;
        this.notificationSender = notificationSender;
    }

    public NotificationResult processOrderCreated(OrderCreatedEvent event) {
        // at-least-once 전달 특성 때문에 eventId를 기준으로 선점 처리해 idempotency를 보장한다.
        boolean reserved = processedEventRepository.reserveIfAbsent(event.eventId());
        if (!reserved) {
            log.info("Duplicate event ignored. eventId={}, orderId={}", event.eventId(), event.orderId());
            return new NotificationResult(event.eventId(), event.orderId(), NotificationStatus.DUPLICATE, Instant.now(), "Already processed");
        }

        try {
            notificationSender.sendOrderConfirmation(event);
            return new NotificationResult(event.eventId(), event.orderId(), NotificationStatus.SENT, Instant.now(), "Notification sent");
        } catch (RuntimeException exception) {
            // 처리 실패 시 예약 키를 해제해 Kafka 재시도/재처리에서 정상 재실행될 수 있게 한다.
            processedEventRepository.releaseReservation(event.eventId());
            throw exception;
        }
    }
}
