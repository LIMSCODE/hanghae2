## 프로젝트

한국항해99 부트캠프 과제로 구현된 **Clean Architecture 기반 Spring Boot 서버**입니다.

- **e-커머스 상품 주문 서비스**
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
```