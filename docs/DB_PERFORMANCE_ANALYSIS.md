# DB 성능 분석 및 최적화 보고서

## 📊 개요

본 보고서는 e-커머스 및 콘서트 예약 서비스의 데이터베이스 성능을 분석하고, 조회 성능이 저하될 수 있는 지점을 식별하여 최적화 방안을 제시합니다.

## 🔍 성능 병목 지점 분석

### 1. e-커머스 서비스 성능 이슈

#### 🟥 HIGH RISK: 인기 상품 조회 (ProductSalesStatistics)

**문제점:**
```sql
-- 현재 쿼리: 최근 3일간 상위 5개 상품 조회
SELECT p.product_id, p.name, pss.sales_count
FROM products p
JOIN product_sales_statistics pss ON p.product_id = pss.product_id
WHERE pss.created_at >= DATE_SUB(NOW(), INTERVAL 3 DAY)
ORDER BY pss.sales_count DESC
LIMIT 5;
```

**성능 이슈:**
- `product_sales_statistics` 테이블의 **풀 스캔** 발생
- `ORDER BY sales_count DESC` 시 **정렬 부하** 증가
- 매번 3일치 데이터를 실시간 집계

**예상 성능:**
- 상품 1만개, 주문 10만건 기준: **2-5초** 응답 시간
- 동시 사용자 증가 시 **타임아웃** 위험

#### 🟨 MEDIUM RISK: 주문 내역 조회 (Order + OrderItem)

**문제점:**
```sql
-- 사용자별 주문 내역 조회
SELECT o.order_id, o.total_amount, oi.product_id, oi.quantity
FROM orders o
LEFT JOIN order_items oi ON o.order_id = oi.order_id
WHERE o.user_id = ?
ORDER BY o.created_at DESC;
```

**성능 이슈:**
- `user_id` 인덱스 없을 시 **풀 스캔**
- `ORDER BY created_at` 정렬 비용
- N+1 문제 가능성 (ORM 사용 시)

#### 🟨 MEDIUM RISK: 상품 재고 확인 (Product)

**문제점:**
```sql
-- 재고 있는 상품만 조회
SELECT * FROM products WHERE stock > 0 ORDER BY created_at DESC;
```

**성능 이슈:**
- `stock` 컬럼에 인덱스 부재 시 풀 스캔
- 상품 수 증가 시 성능 급격히 저하

### 2. 콘서트 예약 서비스 성능 이슈

#### 🟥 HIGH RISK: 예약 가능한 좌석 조회 (Seat)

**문제점:**
```sql
-- 특정 콘서트 일정의 예약 가능한 좌석
SELECT s.seat_id, s.seat_number, s.seat_status
FROM seats s
JOIN concert_schedules cs ON s.schedule_id = cs.schedule_id
WHERE cs.schedule_id = ?
  AND s.seat_status = 'AVAILABLE'
  AND (s.expires_at IS NULL OR s.expires_at <= NOW())
ORDER BY s.seat_number;
```

**성능 이슈:**
- 복잡한 `WHERE` 조건으로 **인덱스 효율성 저하**
- 만료 시간 체크로 **함수 기반 조건** 사용
- 실시간 좌석 상태 확인으로 **락 경합** 가능성

#### 🟨 MEDIUM RISK: 대기열 상태 조회 (QueueToken)

**문제점:**
```sql
-- 대기 중인 토큰 수 조회
SELECT COUNT(*) FROM queue_tokens WHERE token_status = 'WAITING';

-- 사용자별 대기 순서 조회
SELECT queue_position FROM queue_tokens
WHERE user_id = ? AND token_status = 'WAITING';
```

**성능 이슈:**
- `COUNT(*)` 연산의 **높은 비용**
- 대기열 길이 증가 시 **성능 급격히 저하**

## 🚀 최적화 솔루션

### 1. 인덱스 최적화

#### e-커머스 인덱스 전략
```sql
-- 1. 복합 인덱스: 인기 상품 조회 최적화
CREATE INDEX idx_product_sales_created_count
ON product_sales_statistics(created_at, sales_count DESC);

-- 2. 사용자별 주문 조회 최적화
CREATE INDEX idx_orders_user_created
ON orders(user_id, created_at DESC);

-- 3. 재고 있는 상품 조회 최적화
CREATE INDEX idx_products_stock_created
ON products(stock, created_at DESC)
WHERE stock > 0;  -- 부분 인덱스

-- 4. 주문 상태별 조회 최적화
CREATE INDEX idx_orders_status_created
ON orders(order_status, created_at DESC);
```

#### 콘서트 예약 인덱스 전략
```sql
-- 1. 좌석 예약 가능 여부 확인 최적화
CREATE INDEX idx_seats_schedule_status_expires
ON seats(schedule_id, seat_status, expires_at);

-- 2. 대기열 토큰 상태별 조회 최적화
CREATE INDEX idx_queue_tokens_status_position
ON queue_tokens(token_status, queue_position);

-- 3. 사용자별 토큰 조회 최적화
CREATE INDEX idx_queue_tokens_user_status
ON queue_tokens(user_id, token_status);

-- 4. 콘서트 일정별 예약 현황 최적화
CREATE INDEX idx_concert_schedules_date_available
ON concert_schedules(concert_date, available_seats);
```

### 2. 테이블 구조 최적화

#### 2.1 비정규화를 통한 조회 성능 향상

**인기 상품 집계 테이블 분리:**
```sql
-- 기존: 실시간 집계 (느림)
-- 개선: 배치 집계 테이블 생성

CREATE TABLE popular_products_daily (
    date DATE PRIMARY KEY,
    product_id BIGINT,
    product_name VARCHAR(255),
    daily_sales_count INT,
    ranking INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_date_ranking(date, ranking)
);

-- 배치로 매일 집계 후 저장
-- 조회 시 O(1) 성능 달성
```

**좌석 예약 현황 캐시 테이블:**
```sql
CREATE TABLE seat_availability_cache (
    schedule_id BIGINT PRIMARY KEY,
    available_count INT,
    total_count INT,
    last_updated TIMESTAMP,

    INDEX idx_last_updated(last_updated)
);

-- 좌석 상태 변경 시 트리거로 업데이트
-- 복잡한 조인 없이 즉시 예약 가능 좌석 수 확인
```

#### 2.2 파티셔닝 전략

**주문 테이블 월별 파티셔닝:**
```sql
CREATE TABLE orders (
    order_id BIGINT AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    -- 기타 컬럼들
    created_at TIMESTAMP NOT NULL
)
PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202401 VALUES LESS THAN (202402),
    PARTITION p202402 VALUES LESS THAN (202403),
    -- 매월 파티션 추가
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

**대기열 토큰 상태별 파티셔닝:**
```sql
CREATE TABLE queue_tokens (
    token_id BIGINT AUTO_INCREMENT,
    -- 기타 컬럼들
    token_status ENUM('WAITING', 'ACTIVE', 'EXPIRED', 'COMPLETED')
)
PARTITION BY LIST COLUMNS(token_status) (
    PARTITION p_waiting VALUES IN ('WAITING'),
    PARTITION p_active VALUES IN ('ACTIVE'),
    PARTITION p_completed VALUES IN ('EXPIRED', 'COMPLETED')
);
```

### 3. 쿼리 최적화

#### 3.1 커버링 인덱스 활용

**인기 상품 조회 최적화:**
```sql
-- 기존: 테이블 랜덤 액세스 발생
-- 개선: 인덱스만으로 모든 데이터 조회

CREATE INDEX idx_covering_popular_products
ON product_sales_statistics(created_at, sales_count DESC, product_id, product_name);

-- 쿼리가 인덱스만 읽고 결과 반환 (테이블 접근 없음)
```

#### 3.2 읽기 전용 복제본 활용

**조회 성능 분산:**
```java
// Master: 쓰기 전용
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order); // Master DB
}

// Slave: 읽기 전용
@Transactional(readOnly = true)
public List<Order> getUserOrders(Long userId) {
    return orderRepository.findByUserId(userId); // Replica DB
}
```

## 📈 예상 성능 개선 효과

| 기능 | 현재 예상 성능 | 최적화 후 성능 | 개선율 |
|------|---------------|----------------|--------|
| 인기 상품 조회 | 2-5초 | 50-100ms | **95% 개선** |
| 주문 내역 조회 | 500ms-1s | 50-150ms | **80% 개선** |
| 좌석 예약 가능 확인 | 300-800ms | 30-80ms | **85% 개선** |
| 대기열 상태 조회 | 200-500ms | 20-50ms | **90% 개선** |

## 🎯 구현 우선순위

### Phase 1 (즉시 적용)
1. **기본 인덱스 생성** - 사용자별, 상태별 조회 최적화
2. **읽기 복제본 분리** - 조회 부하 분산
3. **쿼리 튜닝** - N+1 문제 해결

### Phase 2 (단기 적용)
1. **복합 인덱스 최적화** - 복잡한 조회 조건 최적화
2. **커버링 인덱스** - 랜덤 액세스 제거
3. **배치 집계 테이블** - 인기 상품 실시간 집계 대체

### Phase 3 (중장기 적용)
1. **테이블 파티셔닝** - 대용량 데이터 처리
2. **캐시 레이어 도입** - Redis 기반 조회 성능 향상
3. **샤딩 전략** - 수평적 확장 대비

## 📊 모니터링 지표

**핵심 성능 지표 (KPI):**
- **P95 응답시간** < 200ms
- **DB CPU 사용률** < 70%
- **Slow Query** 0건 (1초 이상)
- **Lock Wait Time** < 100ms

**모니터링 도구:**
- `EXPLAIN ANALYZE`로 실행 계획 분석
- `pt-query-digest`로 슬로우 쿼리 분석
- `SHOW PROCESSLIST`로 락 대기 상황 모니터링