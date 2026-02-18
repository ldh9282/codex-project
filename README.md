# Kafka Event-Driven MSA Portfolio

Spring Boot 멀티 모듈 기반의 Kafka 이벤트 드리븐 예제 프로젝트입니다.  
핵심 시나리오는 **주문/상품 이벤트 발행**과 **주문 알림 소비(생성/배송 상태)** 입니다.

## 1) 프로젝트 개요

이 프로젝트는 3개의 서비스로 구성됩니다.

- `order-service`
  - 주문 생성 이벤트(`order.created.v1`) 발행
  - 주문 상태 변경 이벤트(`order.shipped.v1`) 발행
- `product-service`
  - 상품 생성 이벤트(`product.created.v1`) 발행
- `notification-service`
  - 주문 생성/배송 이벤트 소비
  - Redis 기반 idempotency 처리
  - Retry + DLQ 처리

## 2) 주요 특징

- **서비스 간 비동기 통신**: Kafka를 통해 서비스 결합도를 낮춤
- **메시지 키 기반 파티셔닝**: `orderId`/`productId` 키 사용
- **운영 친화적 에러 처리**: `DefaultErrorHandler + DeadLetterPublishingRecoverer`
- **중복 처리 방지**: Redis `SETNX + TTL` 기반 idempotency
- **추적 가능한 로그**: topic/partition/offset/eventId 중심 로깅

## 3) 아키텍처 (텍스트)

```text
[Client]
   | POST /api/orders, /api/orders/{orderId}/status, /api/products
   v
[order-service] ----------------------> order.created.v1 / order.shipped.v1
[product-service] --------------------> product.created.v1
                    (Kafka Broker)
                          |
                          | consumer group: notification-consumer-group
                          v
                 [notification-service]
                   - consume order.created.v1
                   - consume order.shipped.v1
                   - idempotency (Redis)
                   - retry 3회 후 DLQ 전송
                          |
                          +--> order.created.v1.dlq
                          +--> order.shipped.v1.dlq
```

> 참고: `product.created.v1.dlq` 토픽은 현재 `product-service`에서 토픽 생성용으로 정의되어 있으며, 본 프로젝트 내 소비자는 구현되어 있지 않습니다.

## 4) 모듈 구조

```text
.
├─ common                  # 이벤트 DTO / 공통 SerDe
├─ order-service           # 주문 API + 주문 이벤트 발행
├─ product-service         # 상품 API + 상품 이벤트 발행
├─ notification-service    # 주문 이벤트 소비 + 알림 처리
├─ docker-compose.yml      # Kafka, Zookeeper, Redis
└─ README.md
```

## 5) 토픽

- `order.created.v1`
- `order.created.v1.dlq`
- `order.shipped.v1`
- `order.shipped.v1.dlq`
- `product.created.v1`
- `product.created.v1.dlq` (현재 소비자 없음)

## 6) 로컬 실행

### 6.1 인프라 실행

```bash
docker compose up -d
```

### 6.2 빌드

```bash
mvn clean package
```

### 6.3 서비스 실행 (각각 별도 터미널)

```bash
mvn -pl order-service spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl product-service spring-boot:run
```

## 7) API 예시

### 7.1 주문 생성

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

### 7.2 주문 상태 변경

허용 전이:
- `20 -> 25`
- `25 -> 80`

```bash
curl -X POST http://localhost:8081/api/orders/<orderId>/status \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "user-1001",
    "customerEmail": "user1001@example.com",
    "previousStatus": 20,
    "currentStatus": 25
  }'
```

### 7.3 상품 생성

```bash
curl -X POST http://localhost:8083/api/products \
  -H 'Content-Type: application/json' \
  -d '{
    "productName": "wireless-keyboard",
    "price": 59900,
    "currency": "KRW",
    "stockQuantity": 120
  }'
```

## 8) 장애/중복 처리 전략

### Retry + DLQ

- `notification-service`에서 주문 이벤트 소비 실패 시
  - `FixedBackOff(2000ms, 3회)` 재시도
  - 이후 각 DLQ 토픽으로 이동

### Idempotency

- `eventId` 기준 Redis 예약(`SETNX`) 성공 시에만 처리
- 이미 처리된 이벤트면 `DUPLICATE`로 스킵
- 처리 실패 시 예약 키를 해제하여 재처리 가능하게 보장

## 9) 테스트용 실패 시나리오

`customerId`를 `fail-` prefix로 전달하면 `notification-service`의 전송 로직에서 예외를 발생시켜 retry/DLQ 흐름을 확인할 수 있습니다.

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

## 10) 다음 개선 아이디어

- Outbox Pattern + CDC(Debezium)
- Schema Registry(Avro/Protobuf)
- DLQ 재처리 워커 + 운영 대시보드
