## 프로젝트

한국항해99 부트캠프 과제로 구현된 **Clean Architecture 기반 Spring Boot 서버**입니다.

- **e-커머스 상품 주문 서비스** (Infrastructure Layer 구현 완료)
- **콘서트 예약 서비스** (대기열 시스템 포함)

## 🏗️ 아키텍처

### Clean Architecture 적용
```
┌─────────────────────────────────────────┐
│              API Layer                  │
│         (Controllers)                   │
├─────────────────────────────────────────┤
│           Application Layer             │
│            (Use Cases)                  │
├─────────────────────────────────────────┤
│             Domain Layer                │
│    (Entities, Repositories Interface)   │
├─────────────────────────────────────────┤
│          Infrastructure Layer           │
│         (JPA Repositories)              │
└─────────────────────────────────────────┘
```

### 기술 스택
- **Framework**: Spring Boot 3.4.1 + Java 17
- **Database**: MySQL 8.0 + HikariCP Connection Pool
- **ORM**: Spring Data JPA + Hibernate
- **Build**: Gradle 8.11.1 (Kotlin DSL)
- **Test**: JUnit 5 + AssertJ + Testcontainers

## 🛒 e-커머스 상품 주문 서비스 (과제 4 Infrastructure Layer)

### 핵심 기능

#### 1. Infrastructure Layer 구현
- **Repository Pattern**: Clean Architecture 원칙에 따른 Domain Interface → Infrastructure Implementation
- **JPA 기반 구현**: Spring Data JPA + 커스텀 쿼리를 통한 고성능 데이터 처리
- **트랜잭션 관리**: 복잡한 비즈니스 로직에서 데이터 일관성 보장

#### 2. 외부 메시지 전송 (Outbox Pattern)
- **Outbox 테이블**: 이벤트 발행 실패에 대비한 안정적인 메시지 처리
- **MockMessageProducer**: 외부 데이터 플랫폼 연동 시뮬레이션 (90% 성공률)
- **자동 재시도**: 실패한 이벤트에 대한 최대 3회 재시도 메커니즘
- **스케줄링**: 10초마다 대기 중인 이벤트 처리 및 실패 이벤트 재시도

#### 3. Idempotency 보장
- **중복 요청 방지**: UUID 기반 idempotency_key를 통한 결제 중복 처리 방지
- **원자성 보장**: 재고 차감, 잔액 차감, 주문 생성이 하나의 트랜잭션으로 처리

#### 4. 동시성 처리
- **선착순 쿠폰 발급**: 비관적 락(Pessimistic Lock)을 통한 동시 요청 제어
- **재고 관리**: 낙관적 업데이트로 재고 부족 시 즉시 실패 처리

### Repository 구조

#### Domain Layer (Interfaces)
```
kr.hhplus.be.server.domain.ecommerce.repository/
├── UserBalanceRepository.java    # 사용자 잔액 관리
├── ProductRepository.java        # 상품 및 재고 관리
├── OrderRepository.java          # 주문 관리
├── CouponRepository.java         # 쿠폰 발급 및 관리
└── OutboxRepository.java         # 외부 메시지 이벤트 관리
```

#### Infrastructure Layer (Implementations)
```
kr.hhplus.be.server.infrastructure.ecommerce/
├── JpaUserBalanceRepository.java  # 잔액 충전/차감 + 비관적 락
├── JpaProductRepository.java      # 재고 관리 + 판매 통계 업데이트
├── JpaOrderRepository.java        # 주문 생성 + idempotency 처리
├── JpaCouponRepository.java       # 선착순 쿠폰 발급 제어
└── JpaOutboxRepository.java       # 이벤트 발행 및 재시도 처리
```

### 통합 테스트

#### 주요 테스트 시나리오
- **전체 주문 플로우**: 충전 → 재고 확인 → 주문 → 잔액 차감 → 외부 전송
- **Idempotency 테스트**: 동일한 key로 중복 요청 시 한 번만 처리
- **동시성 테스트**: 쿠폰 발급 경쟁 조건에서 한 명만 성공
- **실패 시나리오**: 재고 부족, 잔액 부족 상황에서 롤백 처리

### API 명세

#### 주문 및 결제 (idempotency 지원)
```http
POST /api/orders
Content-Type: application/json
Idempotency-Key: {uuid}

{
  "userId": 1,
  "orderItems": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

#### 잔액 충전
```http
POST /api/balance/charge
Content-Type: application/json

{
  "userId": 1,
  "amount": 50000
}
```

#### 선착순 쿠폰 발급
```http
POST /api/coupons/{couponId}/issue
Content-Type: application/json

{
  "userId": 1
}
```

### 외부 연동 (Outbox Pattern)

#### 이벤트 발행 흐름
1. **주문 완료 시**: OrderCompletedEvent를 Outbox 테이블에 저장
2. **스케줄링**: 10초마다 대기 중인 이벤트 처리
3. **외부 전송**: MockMessageProducer로 데이터 플랫폼에 전송 시뮬레이션
4. **재시도**: 실패 시 최대 3회까지 자동 재시도
5. **정리**: 7일 이상 지난 처리 완료 이벤트 자동 삭제

## 🎵 콘서트 예약 서비스 (과제 2,3,4 구현)

### 핵심 기능

#### 1. 대기열 관리 시스템 (심화 과제)
- **토큰 기반 대기열**: UUID 토큰으로 사용자 식별
- **동시 활성화 제한**: 최대 100명까지 동시 서비스 이용 가능
- **자동 대기열 처리**: 30초마다 스케줄링으로 토큰 상태 관리
- **예상 대기 시간**: 대기 순서 기반 시간 계산

#### 2. 좌석 예약 시스템
- **임시 예약**: 좌석 선택 시 5분간 임시 배정
- **자동 만료**: 결제 미완료 시 자동으로 좌석 해제
- **좌석 확정**: 결제 완료 시 좌석 소유권 이전
- **동시성 처리**: 다중 사용자 예약 경쟁 상황 대응

#### 3. 결제 시스템
- **잔액 기반 결제**: 사전 충전된 잔액으로 결제
- **원자성 보장**: 결제-예약확정-토큰완료 트랜잭션 처리

### API 명세

#### 대기열 관리
```http
POST /api/concerts/queue/token
Content-Type: application/json

{
  "userId": 1
}
```

```http
GET /api/concerts/queue/status?tokenUuid={uuid}
```

#### 콘서트 조회
```http
GET /api/concerts/schedules
Queue-Token: {uuid}
```

```http
GET /api/concerts/schedules/{scheduleId}/seats
Queue-Token: {uuid}
```

#### 예약 및 결제
```http
POST /api/concerts/reservations
Queue-Token: {uuid}
Content-Type: application/json

{
  "userId": 1,
  "seatId": 1,
  "price": 50000
}
```

```http
POST /api/concerts/payments
Queue-Token: {uuid}
Content-Type: application/json

{
  "userId": 1,
  "reservationId": 1
}
```

### 도메인 모델

#### 콘서트 관련
- **Concert**: 콘서트 기본 정보
- **ConcertSchedule**: 콘서트 일정 및 좌석 현황
- **Seat**: 좌석 정보 및 예약 상태 (AVAILABLE, TEMPORARY_RESERVED, RESERVED)
- **Reservation**: 예약 정보 및 상태 (TEMPORARY, CONFIRMED, CANCELLED)

#### 대기열 관리
- **QueueToken**: 대기열 토큰 및 상태 (WAITING, ACTIVE, EXPIRED, COMPLETED)

### Clean Architecture 구현 상세

#### Domain Layer
```
kr.hhplus.be.server.domain.concert/
├── Concert.java
├── ConcertSchedule.java
├── Seat.java
├── Reservation.java
└── repository/
    ├── ConcertRepository.java
    ├── SeatRepository.java
    └── ReservationRepository.java

kr.hhplus.be.server.domain.queue/
├── QueueToken.java
└── repository/
    └── QueueTokenRepository.java
```

#### Application Layer (Use Cases)
```
kr.hhplus.be.server.application/
├── concert/
│   └── ReservationUseCase.java
└── queue/
    └── QueueManagementUseCase.java
```

#### Infrastructure Layer
```
kr.hhplus.be.server.infrastructure/
├── concert/
│   ├── JpaSeatRepository.java
│   └── JpaReservationRepository.java
└── queue/
    └── JpaQueueTokenRepository.java
```

### 테스트 구현

#### 단위 테스트 (Mock 활용)
- `ReservationUseCaseTest`: 예약/결제 비즈니스 로직 검증
- `QueueManagementUseCaseTest`: 대기열 관리 로직 검증

#### 통합 테스트
- `ConcertReservationIntegrationTest`: 전체 플로우 및 동시성 테스트

### 동시성 처리

#### 좌석 예약 경쟁 상황
```java
// 동시에 10명이 같은 좌석 예약 시도
// 결과: 오직 1명만 성공, 9명은 실패
@Test
void concurrentSeatReservationTest() {
    // 실제 동시성 테스트 구현
}
```

#### 대기열 토큰 관리
- **스케줄링**: 30초마다 만료 토큰 정리 및 새 토큰 활성화
- **순서 보장**: 대기 순서에 따른 토큰 활성화

## Getting Started

### Prerequisites

#### Running Docker Containers

`local` profile 로 실행하기 위하여 인프라가 설정되어 있는 Docker 컨테이너를 실행해주셔야 합니다.

```bash
docker-compose up -d
```

### 애플리케이션 실행

```bash
# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun
```

### API 테스트 예시

#### 1. 대기열 토큰 발급
```bash
curl -X POST http://localhost:8080/api/concerts/queue/token \
  -H "Content-Type: application/json" \
  -d '{"userId": 1}'
```

#### 2. 대기열 상태 확인
```bash
curl "http://localhost:8080/api/concerts/queue/status?tokenUuid={token-uuid}"
```

#### 3. 예약 가능한 콘서트 조회
```bash
curl http://localhost:8080/api/concerts/schedules \
  -H "Queue-Token: {token-uuid}"

## 📊 DB 성능 분석 및 최적화 (과제 4 선택/심화)

### 성능 분석 개요

본 프로젝트의 데이터베이스 성능을 분석하고, 조회 성능이 저하될 수 있는 지점을 식별하여 최적화 방안을 제시합니다.

### 주요 성능 병목 지점

#### 🟥 HIGH RISK: 인기 상품 조회 (ProductSalesStatistics)
- **문제**: 매번 3일치 데이터를 실시간 집계하며 풀 스캔 발생
- **예상 성능**: 상품 1만개, 주문 10만건 기준 2-5초 응답 시간
- **솔루션**: 배치 집계 테이블 분리 + 복합 인덱스

#### 🟥 HIGH RISK: 예약 가능한 좌석 조회 (Seat)
- **문제**: 복잡한 WHERE 조건과 만료 시간 체크로 인덱스 효율성 저하
- **솔루션**: 좌석 예약 현황 캐시 테이블 + 최적화된 인덱스

#### 🟨 MEDIUM RISK: 주문 내역 조회, 대기열 상태 조회
- **문제**: user_id 인덱스 부재 시 풀 스캐, COUNT(*) 연산 비용
- **솔루션**: 사용자별 복합 인덱스 + 상태별 파티셔닝

### 최적화 솔루션

#### 1. 인덱스 최적화
```sql
-- e-커머스: 복합 인덱스로 인기 상품 조회 최적화
CREATE INDEX idx_product_sales_created_count
ON product_sales_statistics(created_at, sales_count DESC);

-- 콘서트: 좌석 예약 가능 여부 확인 최적화
CREATE INDEX idx_seats_schedule_status_expires
ON seats(schedule_id, seat_status, expires_at);

-- 대기열: 토큰 상태별 조회 최적화
CREATE INDEX idx_queue_tokens_status_position
ON queue_tokens(token_status, queue_position);
```

#### 2. 테이블 구조 최적화
```sql
-- 인기 상품 집계 테이블 분리 (실시간 → 배치)
CREATE TABLE popular_products_daily (
    date DATE PRIMARY KEY,
    product_id BIGINT,
    product_name VARCHAR(255),
    daily_sales_count INT,
    ranking INT,
    INDEX idx_date_ranking(date, ranking)
);

-- 좌석 예약 현황 캐시 테이블
CREATE TABLE seat_availability_cache (
    schedule_id BIGINT PRIMARY KEY,
    available_count INT,
    total_count INT,
    last_updated TIMESTAMP
);
```

#### 3. 파티셔닝 전략
- **주문 테이블**: 월별 파티셔닝으로 대용량 데이터 처리
- **대기열 토큰**: 상태별 파티셔닝 (WAITING, ACTIVE, COMPLETED)

### 예상 성능 개선 효과

| 기능 | 현재 예상 성능 | 최적화 후 성능 | 개선율 |
|------|---------------|----------------|--------|
| 인기 상품 조회 | 2-5초 | 50-100ms | **95% 개선** |
| 주문 내역 조회 | 500ms-1s | 50-150ms | **80% 개선** |
| 좌석 예약 가능 확인 | 300-800ms | 30-80ms | **85% 개선** |
| 대기열 상태 조회 | 200-500ms | 20-50ms | **90% 개선** |

### 구현 우선순위

#### Phase 1 (즉시 적용)
1. **기본 인덱스 생성** - 사용자별, 상태별 조회 최적화
2. **읽기 복제본 분리** - 조회 부하 분산
3. **쿼리 튜닝** - N+1 문제 해결

#### Phase 2 (단기 적용)
1. **복합 인덱스 최적화** - 복잡한 조회 조건 최적화
2. **커버링 인덱스** - 랜덤 액세스 제거
3. **배치 집계 테이블** - 인기 상품 실시간 집계 대체

#### Phase 3 (중장기 적용)
1. **테이블 파티셔닝** - 대용량 데이터 처리
2. **캐시 레이어 도입** - Redis 기반 조회 성능 향상
3. **샤딩 전략** - 수평적 확장 대비

### 동시성 테스트 구현

주요 기능별 동시성 이슈를 미리 식별하고 테스트로 규칙 설정:

#### 테스트 시나리오
1. **재고 차감 경쟁 조건**: 15명이 10개 재고에 동시 주문 → 정확히 10명만 성공
2. **사용자 잔액 차감**: 동일 사용자 동시 주문 시 잔액 초과 방지
3. **선착순 쿠폰 발급**: 20명이 3개 쿠폰에 동시 요청 → 정확히 3명만 성공
4. **Idempotency Key 중복**: 동일 키로 10번 요청 → 1번만 처리, 9번은 중복 처리
5. **콘서트 좌석 예약**: 20명이 1개 좌석 예약 → 1명만 성공
6. **대기열 토큰 발급**: 100명 동시 발급 → 고유한 대기 순서 보장

자세한 성능 분석 내용은 [`docs/DB_PERFORMANCE_ANALYSIS.md`](docs/DB_PERFORMANCE_ANALYSIS.md)를 참고하세요.
```