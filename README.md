# Kafka Event-Driven MSA Portfolio (Order → Notification)

실무형 설계를 목표로 만든 Spring Boot 기반 Kafka 이벤트 드리븐 프로젝트입니다.

## 1) 프로젝트 개요

이 프로젝트는 `order-service`에서 주문 생성 API를 호출하면 `OrderCreatedEvent`를 Kafka로 발행하고,
`notification-service`가 해당 이벤트를 소비하여 알림을 전송하는 구조입니다.

핵심 포인트:
- Producer/Consumer 분리 (서비스 책임 분리)
- Consumer Group + Concurrency 기반 병렬 처리
- Retry + DLQ(Dead Letter Queue)로 장애 메시지 격리
- Redis 기반 Idempotency(중복 처리 방지)
- 운영 추적에 필요한 로그/메타데이터(eventId, partition, offset) 출력

---

## 2) 아키텍처 다이어그램 (텍스트)

```text
[Client]
   |
   | POST /api/orders
   v
[order-service]
   | 1) 주문 생성
   | 2) OrderCreatedEvent 발행 (topic: order.created.v1)
   v
[Kafka Broker]  --- partition(0..2)
   |
   | consumer group: notification-consumer-group (concurrency=3)
   v
[notification-service]
   | 1) eventId 기반 Redis SETNX (idempotency)
   | 2) 알림 전송
   | 3) 실패 시 retry(3회) -> DLQ(topic: order.created.v1.dlq)
   v
[DLQ Consumer]
   | 운영 알람/재처리 연계 포인트
   v
[Ops / Alerting / Reprocessor]
```

---

## 3) 전체 프로젝트 구조

```text
kafka-msa-portfolio/
├─ pom.xml
├─ docker-compose.yml
├─ README.md
├─ common/
│  ├─ pom.xml
│  └─ src/main/java/com/example/common/
│     ├─ event/OrderCreatedEvent.java
│     └─ serde/KafkaEventSerDe.java
├─ order-service/
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/example/order/
│     │  ├─ OrderServiceApplication.java
│     │  ├─ config/KafkaProducerConfig.java
│     │  ├─ controller/OrderController.java
│     │  ├─ controller/GlobalExceptionHandler.java
│     │  ├─ dto/CreateOrderRequest.java
│     │  ├─ dto/CreateOrderResponse.java
│     │  ├─ producer/OrderEventProducer.java
│     │  └─ service/OrderService.java
│     └─ resources/application.yml
└─ notification-service/
   ├─ pom.xml
   └─ src/main/
      ├─ java/com/example/notification/
      │  ├─ NotificationServiceApplication.java
      │  ├─ config/KafkaConsumerConfig.java
      │  ├─ config/KafkaTemplateConfig.java
      │  ├─ consumer/OrderEventConsumer.java
      │  ├─ consumer/OrderEventDlqConsumer.java
      │  ├─ domain/NotificationStatus.java
      │  ├─ dto/NotificationResult.java
      │  ├─ repository/ProcessedEventRepository.java
      │  └─ service/
      │     ├─ NotificationSender.java
      │     └─ NotificationService.java
      └─ resources/application.yml
```

---

## 4) Kafka를 선택한 이유

1. **서비스 간 강결합 완화**
   - 주문 서비스는 알림 서비스의 상태를 몰라도 이벤트만 발행하면 됩니다.
2. **확장성**
   - 동일 이벤트를 다른 소비자(정산/통계/추천)로 손쉽게 확장할 수 있습니다.
3. **내고장성**
   - 소비자 장애 시에도 이벤트는 브로커에 유지되며 재처리가 가능합니다.
4. **운영성**
   - offset, partition, consumer lag 관측이 가능해 운영 가시성이 좋습니다.

---

## 5) DLQ 및 장애 대응 전략

### Retry
- `DefaultErrorHandler + FixedBackOff(2000ms, 3회)` 사용.
- 일시적 장애(외부 API timeout 등)에서 자동 회복 기회를 제공.

### DLQ
- 재시도 후 실패한 메시지는 `order.created.v1.dlq` 토픽으로 이동.
- DLQ 컨슈머에서 장애 메시지 로깅 후 운영 알람/재처리 시스템으로 연결 가능.

### 왜 필요한가?
- 무한 재시도는 파티션 처리를 막고 지연을 누적시킵니다.
- 실패 메시지를 격리해야 정상 메시지 흐름을 보호할 수 있습니다.

---

## 6) Idempotency(중복 처리) 전략

- Kafka는 기본적으로 at-least-once 전달이므로 중복 소비가 가능.
- `eventId`를 키로 Redis `SETNX + TTL(7일)` 저장.
- 이미 처리된 eventId는 즉시 `DUPLICATE`로 스킵.

장점:
- 멀티 인스턴스 환경에서도 원자적으로 중복 방지 가능.
- TTL로 저장소 무한 증가 방지.

---

## 7) 로컬 실행 방법

### 7.1 인프라 기동
```bash
docker compose up -d
```

### 7.2 빌드
```bash
mvn clean package
```

### 7.3 서비스 실행
```bash
# 터미널 1
mvn -pl order-service spring-boot:run

# 터미널 2
mvn -pl notification-service spring-boot:run
```

### 7.4 주문 생성 API 호출
```bash
curl -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "user-1001",
    "customerEmail": "user1001@example.com",
    "totalAmount": 12900,
    "currency": "KRW"
  }'
```

### 7.5 DLQ 테스트 (강제 실패)
```bash
curl -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "fail-user-1",
    "customerEmail": "fail@example.com",
    "totalAmount": 5000,
    "currency": "KRW"
  }'
```
- `customerId`가 `fail-`로 시작하면 notification 전송 로직이 예외를 던져 retry 후 DLQ로 이동합니다.

---

## 8) 트러블슈팅 예시

### 문제 1) Consumer가 메시지를 못 읽음
- 점검:
  - `spring.kafka.bootstrap-servers` 값
  - 토픽 존재 여부 및 파티션 수
  - consumer group lag

### 문제 2) DLQ에 메시지가 누적됨
- 점검:
  - 하위 시스템(메일/SMS) 장애 여부
  - 예외 타입별 분류(재시도 가능/불가)
  - retry 횟수/백오프 값 적정성

### 문제 3) 중복 알림 발송
- 점검:
  - Redis 연결 상태
  - eventId 생성 규칙(전역 유일성)
  - idempotency TTL 정책이 너무 짧지 않은지

---

## 9) 면접 어필 포인트 (핵심 5가지)

1. **Kafka 실패 처리 체계(Retry + DLQ)를 코드 레벨로 구현**
2. **at-least-once를 고려한 Redis 기반 Idempotency 설계**
3. **Consumer Group + Concurrency로 병렬 처리 및 확장성 확보**
4. **토픽 키 전략(orderId key)으로 파티션 순서 보장 고려**
5. **운영 관점 로그(eventId/partition/offset)와 장애 재현 시나리오 포함**

---

## 10) 다음 고도화 제안

- Order DB + Outbox Pattern + CDC(Debezium) 도입
- Schema Registry(Avro/Protobuf)로 이벤트 스키마 버전 관리
- DLQ 재처리 워커와 운영용 대시보드 구축
- Testcontainers 기반 통합 테스트 자동화

