# 구현 가이드

## 개요
이 문서는 E-Commerce 상품 주문 서비스의 실제 구현 내용을 상세히 기술합니다.
동시성 이슈를 고려한 안전한 주문 시스템의 구현 방법과 핵심 기술들을 설명합니다.

## 프로젝트 구조

### 패키지 구조
```
kr.hhplus.be.server
├── ServerApplication.java              # Spring Boot 메인 클래스
├── config/                            # 설정 클래스
│   └── AsyncConfig.java               # 비동기 처리 설정
├── domain/                            # 도메인 엔티티
│   ├── User.java                      # 사용자 엔티티
│   ├── Product.java                   # 상품 엔티티
│   ├── Order.java                     # 주문 엔티티
│   ├── OrderItem.java                 # 주문 항목 엔티티
│   ├── BalanceHistory.java            # 잔액 변경 이력 엔티티
│   └── ProductSalesStatistics.java    # 상품 판매 통계 엔티티
├── repository/                        # 데이터 액세스 계층
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── OrderRepository.java
│   ├── BalanceHistoryRepository.java
│   └── ProductSalesStatisticsRepository.java
├── service/                           # 비즈니스 로직 계층
│   ├── UserService.java               # 사용자/잔액 관리
│   ├── ProductService.java            # 상품 관리
│   ├── OrderService.java              # 주문/결제 처리
│   ├── StatisticsService.java         # 통계 처리
│   ├── DataPlatformService.java       # 외부 연동 (Mock)
│   └── StatisticsUpdateService.java   # 통계 업데이트
├── controller/                        # 웹 계층
│   ├── BalanceController.java         # 잔액 API
│   ├── ProductController.java         # 상품 API
│   └── OrderController.java           # 주문 API
├── dto/                              # 데이터 전송 객체
│   ├── ApiResponse.java              # 공통 응답 포맷
│   ├── BalanceChargeRequest.java     # 잔액 충전 요청
│   ├── BalanceResponse.java          # 잔액 응답
│   ├── ProductResponse.java          # 상품 응답
│   ├── OrderRequest.java             # 주문 요청
│   ├── OrderResponse.java            # 주문 응답
│   ├── PopularProductDto.java        # 인기 상품 DTO
│   └── PopularProductsResponse.java  # 인기 상품 응답
└── exception/                        # 예외 처리
    ├── BusinessException.java        # 비즈니스 예외
    ├── ErrorCode.java               # 에러 코드 열거형
    └── GlobalExceptionHandler.java  # 전역 예외 처리기
```

## 핵심 구현 내용

### 1. 도메인 모델 설계

#### User 엔티티
**파일**: `domain/User.java`

**핵심 특징**:
- 낙관적 락을 위한 `@Version` 필드 사용
- 잔액 관리 비즈니스 로직 캡슐화
- 잔액 부족 시 예외 발생으로 데이터 무결성 보장

**주요 메서드**:
```java
public void chargeBalance(BigDecimal amount)    // 잔액 충전
public void deductBalance(BigDecimal amount)    // 잔액 차감
public boolean hasEnoughBalance(BigDecimal amount)  // 잔액 충분성 검사
```

**동시성 제어**:
- `@Version` 어노테이션으로 낙관적 락 구현
- JPA가 자동으로 버전 체크 및 업데이트 수행

#### Product 엔티티
**파일**: `domain/Product.java`

**핵심 특징**:
- 재고 관리 비즈니스 로직 포함
- 상품 비활성화 기능 지원
- 소계 계산 메서드 제공

**주요 메서드**:
```java
public void deductStock(Integer quantity)       // 재고 차감
public boolean hasEnoughStock(Integer quantity) // 재고 충분성 검사
public BigDecimal calculateSubtotal(Integer quantity) // 소계 계산
```

#### Order 엔티티
**파일**: `domain/Order.java`

**핵심 특징**:
- 주문 상태 관리 (PENDING, COMPLETED, CANCELLED, FAILED)
- OrderItem과의 일대다 관계 설정
- 주문 완료 상태 확인 메서드 제공

### 2. 동시성 제어 전략

#### 비관적 락 (Pessimistic Lock)
**적용 대상**: 상품 재고 관리

**구현 위치**: `repository/ProductRepository.java`
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.productId = :productId")
Optional<Product> findByIdWithLock(@Param("productId") Long productId);
```

**사용 시나리오**:
- 주문 생성 시 상품 재고 차감
- `SELECT FOR UPDATE` 쿼리로 행 단위 잠금
- 동시에 같은 상품을 주문해도 재고 오버플로우 방지

#### 낙관적 락 (Optimistic Lock)
**적용 대상**: 사용자 잔액 관리

**구현 위치**: `domain/User.java`
```java
@Version
@Column(name = "version", nullable = false)
private Long version = 0L;
```

**동작 방식**:
- 엔티티 수정 시 버전 필드 자동 증가
- 업데이트 시 버전 불일치 감지하면 OptimisticLockException 발생
- GlobalExceptionHandler에서 적절한 에러 응답 처리

### 3. 트랜잭션 관리

#### 주문 처리 트랜잭션
**파일**: `service/OrderService.java`

**트랜잭션 경계**:
```java
@Transactional
public OrderResponse createOrder(OrderRequest orderRequest)
```

**처리 순서**:
1. 사용자 조회 (비관적 락)
2. 상품들 조회 및 재고 검증 (비관적 락)
3. 잔액 검증
4. 주문 생성
5. 재고 차감
6. 결제 처리
7. 주문 완료
8. 이벤트 발행 (비동기)

**실패 시 롤백**:
- 어느 단계에서든 예외 발생 시 전체 롤백
- 데이터 일관성 보장

### 4. 비동기 처리 구현

#### 이벤트 기반 아키텍처
**이벤트 클래스**: `service/OrderService.OrderCompletedEvent`

**이벤트 발행**:
```java
OrderCompletedEvent event = new OrderCompletedEvent(savedOrder);
eventPublisher.publishEvent(event);
```

**이벤트 처리**:
1. **외부 데이터 플랫폼 연동** (`DataPlatformService.java`)
   - `@Async` + `@EventListener` 조합
   - Mock 구현으로 실제 HTTP 호출 시뮬레이션
   - 10% 확률로 실패 상황 시뮬레이션

2. **판매 통계 업데이트** (`StatisticsUpdateService.java`)
   - 주문 완료 시 상품별 판매량 집계
   - UPSERT 패턴으로 일별 통계 업데이트

#### 비동기 설정
**파일**: `config/AsyncConfig.java`

**스레드 풀 설정**:
```java
executor.setCorePoolSize(5);        // 기본 스레드 수
executor.setMaxPoolSize(10);        // 최대 스레드 수
executor.setQueueCapacity(100);     // 큐 용량
executor.setRejectedExecutionHandler(
    new ThreadPoolExecutor.CallerRunsPolicy()  // 거부된 작업 처리
);
```

### 5. API 설계 및 구현

#### 공통 응답 포맷
**파일**: `dto/ApiResponse.java`

**구조**:
```java
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": { ... },
  "timestamp": "2024-01-01T10:00:00"
}
```

**팩토리 메서드**:
```java
public static <T> ApiResponse<T> success(T data)
public static ApiResponse<Void> error(String code, String message)
```

#### REST API 엔드포인트

**1. 잔액 관리 API**
- `POST /api/v1/users/{userId}/balance/charge` - 잔액 충전
- `GET /api/v1/users/{userId}/balance` - 잔액 조회

**2. 상품 관리 API**
- `GET /api/v1/products` - 상품 목록 조회 (페이징, 정렬)
- `GET /api/v1/products/{productId}` - 상품 상세 조회
- `GET /api/v1/products/popular` - 인기 상품 조회

**3. 주문 관리 API**
- `POST /api/v1/orders` - 주문 생성 및 결제
- `GET /api/v1/orders/{orderId}` - 주문 상세 조회
- `GET /api/v1/users/{userId}/orders` - 사용자 주문 목록

### 6. 예외 처리 전략

#### 예외 계층 구조
**파일**: `exception/ErrorCode.java`

**에러 코드 분류**:
```java
// 사용자 관련
USER_NOT_FOUND("사용자를 찾을 수 없습니다")
INSUFFICIENT_BALANCE("잔액이 부족합니다")

// 상품 관련  
PRODUCT_NOT_FOUND("상품을 찾을 수 없습니다")
INSUFFICIENT_STOCK("재고가 부족합니다")

// 동시성 관련
OPTIMISTIC_LOCK_EXCEPTION("동시성 충돌이 발생했습니다")
```

#### 전역 예외 처리
**파일**: `exception/GlobalExceptionHandler.java`

**처리 방식**:
- `@RestControllerAdvice`로 모든 컨트롤러 예외 처리
- 비즈니스 예외는 적절한 HTTP 상태 코드와 함께 반환
- 예상치 못한 예외는 500 Internal Server Error로 처리
- 모든 예외 로깅으로 디버깅 지원

### 7. 데이터베이스 설계

#### 인덱스 전략
**성능 최적화를 위한 인덱스 설계**:

```sql
-- 사용자 관련
idx_users_username (username)
idx_users_email (email)

-- 상품 관련
idx_products_name (name)
idx_products_is_active (is_active)

-- 주문 관련
idx_orders_user_id (user_id)
idx_orders_ordered_at (ordered_at)

-- 통계 관련
idx_sales_stats_product_date (product_id, sales_date) - UNIQUE
idx_sales_stats_quantity (total_quantity)
```

#### 통계 데이터 처리
**파일**: `repository/ProductSalesStatisticsRepository.java`

**UPSERT 쿼리**:
```java
@Modifying
@Query("""
    INSERT INTO ProductSalesStatistics (...) 
    VALUES (...)
    ON DUPLICATE KEY UPDATE 
    totalQuantity = totalQuantity + VALUES(totalQuantity),
    totalAmount = totalAmount + VALUES(totalAmount)
""")
void upsertDailySales(...);
```

### 8. 테스트 데이터 및 설정

#### 초기 데이터
**파일**: `resources/data.sql`

**포함 데이터**:
- 테스트 사용자 3명 (다양한 잔액)
- 테스트 상품 5개 (스마트폰, 태블릿, 노트북 등)
- 최근 3일간 판매 통계 데이터

#### 설정 파일
**파일**: `resources/application.yml`

**환경별 설정**:
- **local**: MySQL + DDL 자동 생성
- **test**: H2 인메모리 DB
- **production**: MySQL + DDL 검증만

**주요 설정**:
```yaml
# 커넥션 풀 설정
hikari:
  maximum-pool-size: 3
  connection-timeout: 10000
  max-lifetime: 60000

# JPA 설정  
jpa:
  hibernate:
    ddl-auto: validate  # 운영: validate, 개발: create-drop
  open-in-view: false   # OSIV 비활성화로 성능 최적화
```

## 동시성 테스트 시나리오

### 재고 관리 테스트
**시나리오**: 100명이 동시에 재고 1개 상품 주문

**예상 결과**:
- 1명만 주문 성공
- 99명은 "재고 부족" 오류
- 재고는 정확히 0개

**구현 메커니즘**:
- 비관적 락으로 상품 조회
- 재고 검증 후 차감
- 트랜잭션 커밋 시점에 락 해제

### 잔액 관리 테스트
**시나리오**: 동일 사용자가 여러 기기에서 동시 결제

**예상 결과**:
- 잔액 부족 시 일부 주문만 성공
- 잔액 계산 정확성 100%
- OptimisticLockException 발생 시 재시도 안내

## 성능 고려사항

### 1. N+1 쿼리 방지
- `@EntityGraph` 또는 JOIN FETCH 사용
- 지연 로딩 최적화

### 2. 커넥션 풀 튜닝
- HikariCP 설정 최적화
- 최대 연결 수: 3개 (개발환경)

### 3. 트랜잭션 최적화
- 읽기 전용 트랜잭션 분리 (`@Transactional(readOnly = true)`)
- 트랜잭션 범위 최소화

### 4. 캐싱 전략 (향후 확장)
- Redis 캐시 도입 계획
- 상품 정보, 인기 상품 통계 캐싱

## 보안 고려사항

### 1. 입력 검증
- `@Valid` 어노테이션으로 요청 데이터 검증
- 금액 범위, 수량 제한 등 비즈니스 규칙 검증

### 2. SQL 인젝션 방지
- JPA 쿼리 메서드 사용
- `@Query`에서 파라미터 바인딩

### 3. 민감 정보 보호
- 비밀번호 등 민감 정보 암호화 (향후 구현)
- 로그에 개인정보 노출 방지

## 모니터링 및 로깅

### 1. 로깅 전략
**레벨별 로깅**:
- **DEBUG**: 비즈니스 로직 상세 정보
- **INFO**: 주요 이벤트 (주문 생성, 결제 완료)
- **WARN**: 비즈니스 예외 상황
- **ERROR**: 시스템 오류, 예상치 못한 예외

### 2. 메트릭 수집
- Spring Boot Actuator 활용
- Health Check, 메트릭 엔드포인트 제공

### 3. 분산 추적 (향후 확장)
- 마이크로서비스 환경에서 요청 추적
- Zipkin, Jaeger 연동 계획

## 확장 계획

### 1. 캐싱 시스템
- Redis 도입으로 성능 향상
- 상품 정보, 사용자 세션 캐싱

### 2. 메시지 큐
- RabbitMQ/Apache Kafka 도입
- 외부 시스템 연동 안정성 향상

### 3. 마이크로서비스 분할
- 사용자, 상품, 주문 서비스 분리
- API Gateway 도입

### 4. 데이터베이스 샤딩
- 사용자 기반 데이터 분산
- 읽기 전용 복제본 활용

이 구현 가이드는 실제 운영 환경에서 사용할 수 있는 수준의 e-커머스 주문 시스템 구축 방법을 상세히 설명합니다. 동시성 이슈 해결과 확장성을 고려한 설계로, 대용량 트래픽 처리가 가능한 안정적인 시스템입니다.