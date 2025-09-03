# 코드 구조 상세 분석

## 개요
이 문서는 구현된 E-Commerce 상품 주문 서비스의 코드 구조와 각 클래스의 역할을 상세히 분석합니다.

## 전체 아키텍처

### 레이어드 아키텍처
```
┌─────────────────────────────────────┐
│          Presentation Layer         │  
│  (Controllers, DTOs, Exception)     │
├─────────────────────────────────────┤
│           Service Layer             │
│    (Business Logic, Events)         │
├─────────────────────────────────────┤
│         Repository Layer            │
│     (Data Access, Queries)          │
├─────────────────────────────────────┤
│           Domain Layer              │
│   (Entities, Domain Logic)          │
└─────────────────────────────────────┘
```

## 도메인 계층 (Domain Layer)

### User.java
**역할**: 사용자 정보 및 잔액 관리

**핵심 코드 분석**:
```java
@Entity
@Table(name = "users")
public class User {
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;  // 낙관적 락을 위한 버전 필드
    
    public void chargeBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.balance = this.balance.add(amount);
    }
    
    public void deductBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("잔액이 부족합니다. 현재 잔액: " + this.balance);
        }
        this.balance = this.balance.subtract(amount);
    }
}
```

**설계 원칙**:
- **캡슐화**: 잔액 변경 로직을 엔티티 내부에서 관리
- **불변성 보장**: 잘못된 입력에 대한 예외 처리
- **동시성 제어**: `@Version` 어노테이션으로 낙관적 락 구현

### Product.java
**역할**: 상품 정보 및 재고 관리

**핵심 코드 분석**:
```java
@Entity
@Table(name = "products")
public class Product {
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
    public void deductStock(Integer quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException(
                "재고가 부족합니다. 요청: " + quantity + "개, 현재 재고: " + this.stockQuantity + "개"
            );
        }
        this.stockQuantity -= quantity;
    }
    
    public BigDecimal calculateSubtotal(Integer quantity) {
        return this.price.multiply(BigDecimal.valueOf(quantity));
    }
}
```

**설계 특징**:
- **비즈니스 규칙 캡슐화**: 재고 부족 검사 및 소계 계산
- **명확한 예외 메시지**: 사용자 친화적인 오류 정보 제공

### Order.java
**역할**: 주문 정보 및 상태 관리

**핵심 코드 분석**:
```java
@Entity
@Table(name = "orders") 
public class Order {
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private OrderStatus orderStatus = OrderStatus.PENDING;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();
    
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);  // 양방향 연관관계 설정
    }
    
    public enum OrderStatus {
        PENDING, COMPLETED, CANCELLED, FAILED
    }
}
```

**설계 특징**:
- **상태 패턴**: 명확한 주문 상태 정의
- **연관관계 관리**: OrderItem과의 일대다 관계 적절히 처리
- **지연 로딩**: 성능 최적화를 위한 LAZY 전략

## 리포지토리 계층 (Repository Layer)

### UserRepository.java
**역할**: 사용자 데이터 액세스 및 동시성 제어

**핵심 코드 분석**:
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    Optional<User> findByIdWithLock(@Param("userId") Long userId);
    
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

**설계 포인트**:
- **명시적 락 제어**: `@Lock` 어노테이션으로 비관적 락 적용
- **쿼리 최적화**: 필요한 경우에만 락 사용하도록 메서드 분리

### ProductSalesStatisticsRepository.java
**역할**: 판매 통계 데이터 관리 및 복잡 쿼리

**핵심 코드 분석**:
```java
@Query("""
    SELECT new kr.hhplus.be.server.dto.PopularProductDto(
        p.productId, 
        p.name, 
        p.price, 
        SUM(pss.totalQuantity), 
        SUM(pss.totalAmount)
    )
    FROM ProductSalesStatistics pss 
    JOIN Product p ON pss.productId = p.productId
    WHERE pss.salesDate BETWEEN :startDate AND :endDate
    AND p.isActive = true
    GROUP BY p.productId, p.name, p.price
    ORDER BY SUM(pss.totalQuantity) DESC
""")
List<PopularProductDto> findTopProductsByPeriod(
    @Param("startDate") LocalDate startDate, 
    @Param("endDate") LocalDate endDate
);

@Modifying
@Query("""
    INSERT INTO ProductSalesStatistics (...) VALUES (...)
    ON DUPLICATE KEY UPDATE 
    totalQuantity = totalQuantity + VALUES(totalQuantity),
    totalAmount = totalAmount + VALUES(totalAmount)
""")
void upsertDailySales(...);
```

**설계 특징**:
- **복잡한 집계 쿼리**: JOIN과 GROUP BY를 활용한 통계 조회
- **UPSERT 패턴**: 존재하면 업데이트, 없으면 삽입하는 효율적인 방식
- **DTO 프로젝션**: 필요한 데이터만 선택적으로 조회

## 서비스 계층 (Service Layer)

### UserService.java
**역할**: 사용자 및 잔액 관리 비즈니스 로직

**핵심 코드 분석**:
```java
@Service
@Transactional(readOnly = true)
public class UserService {
    
    @Transactional  // 쓰기 작업은 명시적 트랜잭션
    public BalanceResponse chargeBalance(Long userId, BigDecimal amount) {
        validateChargeAmount(amount);  // 비즈니스 규칙 검증
        
        User user = userRepository.findByIdWithLock(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        BigDecimal balanceBefore = user.getBalance();
        user.chargeBalance(amount);  // 도메인 로직 위임
        
        User savedUser = userRepository.save(user);
        
        // 이력 저장
        BalanceHistory history = BalanceHistory.createChargeHistory(
            userId, amount, balanceBefore, savedUser.getBalance()
        );
        balanceHistoryRepository.save(history);

        return new BalanceResponse(userId, savedUser.getBalance(), amount, history.getCreatedAt());
    }
    
    private void validateChargeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "충전 금액은 1,000원 이상이어야 합니다.");
        }
        if (amount.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "1회 충전 한도는 1,000,000원입니다.");
        }
    }
}
```

**설계 원칙**:
- **단일 책임**: 사용자/잔액 관련 로직만 담당
- **트랜잭션 관리**: 읽기/쓰기 작업 명확히 분리
- **예외 처리**: 비즈니스 규칙 위반 시 명확한 예외 발생
- **이력 관리**: 모든 잔액 변경에 대한 감사 추적

### OrderService.java
**역할**: 주문 생성 및 결제 처리의 핵심 비즈니스 로직

**핵심 코드 분석**:
```java
@Service
@Transactional(readOnly = true)
public class OrderService {
    
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Long userId = orderRequest.getUserId();
        
        // 1. 사용자 조회 (비관적 락)
        User user = userService.getUserWithLock(userId);
        
        // 2. 상품 조회 및 재고 확인 (비관적 락)
        Map<Long, Product> productMap = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderRequest.OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            Product product = productService.getProductWithLock(itemRequest.getProductId());
            productService.validateStock(product, itemRequest.getQuantity());
            
            productMap.put(product.getProductId(), product);
            totalAmount = totalAmount.add(product.calculateSubtotal(itemRequest.getQuantity()));
        }
        
        // 3. 잔액 확인
        if (!user.hasEnoughBalance(totalAmount)) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, 
                String.format("잔액이 부족합니다. 현재 잔액: %s원", user.getBalance()));
        }
        
        // 4-7. 주문 생성, 재고 차감, 결제 처리, 주문 완료
        Order order = new Order(userId, totalAmount);
        
        for (OrderRequest.OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            Product product = productMap.get(itemRequest.getProductId());
            OrderItem orderItem = OrderItem.create(product, itemRequest.getQuantity());
            order.addOrderItem(orderItem);
            productService.deductStock(product, itemRequest.getQuantity());
        }
        
        userService.processPayment(user, totalAmount, null);
        order.complete();
        Order savedOrder = orderRepository.save(order);
        
        // 8. 이벤트 발행 (비동기)
        OrderCompletedEvent event = new OrderCompletedEvent(savedOrder);
        eventPublisher.publishEvent(event);
        
        return new OrderResponse(savedOrder, user.getBalance());
    }
}
```

**설계 특징**:
- **복잡한 비즈니스 플로우**: 8단계 주문 처리 과정
- **동시성 안전**: 비관적 락으로 경쟁 조건 방지
- **원자성 보장**: 전체 과정이 하나의 트랜잭션에서 실행
- **이벤트 기반**: 비동기 후처리를 위한 이벤트 발행
- **실패 안전**: 어느 단계든 실패 시 전체 롤백

### DataPlatformService.java
**역할**: 외부 데이터 플랫폼 연동 (Mock 구현)

**핵심 코드 분석**:
```java
@Service
public class DataPlatformService {
    
    @Async
    @EventListener
    public void handleOrderCompletedEvent(OrderService.OrderCompletedEvent event) {
        Order order = event.getOrder();
        
        try {
            sendOrderDataToPlatform(order);
            logger.info("Order data sent to external platform successfully. OrderId: {}", 
                       order.getOrderId());
        } catch (Exception e) {
            logger.error("Failed to send order data to external platform. OrderId: {}, Error: {}", 
                        order.getOrderId(), e.getMessage(), e);
        }
    }
    
    public void sendOrderDataToPlatform(Order order) {
        Map<String, Object> orderData = createOrderDataPayload(order);
        
        simulateNetworkDelay();  // 100-500ms 랜덤 지연
        
        // 10% 확률로 실패 시뮬레이션
        if (Math.random() < 0.1) {
            throw new RuntimeException("External platform is temporarily unavailable");
        }
        
        logger.info("Mock: Order data sent to external platform: {}", orderData);
    }
}
```

**설계 특징**:
- **비동기 처리**: `@Async`로 메인 플로우와 분리
- **이벤트 기반**: 주문 완료 이벤트에 반응
- **실패 시뮬레이션**: 실제 운영 환경의 네트워크 이슈 모방
- **모니터링**: 성공/실패 로그로 추적 가능

## 컨트롤러 계층 (Presentation Layer)

### OrderController.java
**역할**: 주문 관련 REST API 엔드포인트

**핵심 코드 분석**:
```java
@RestController
@RequestMapping("/api/v1")
@Validated
public class OrderController {
    
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request) {
        
        OrderResponse response = orderService.createOrder(request);
        
        return ResponseEntity.ok(
            ApiResponse.success("주문이 완료되었습니다", response)
        );
    }
    
    @GetMapping("/users/{userId}/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<OrderResponse> response = orderService.getUserOrders(userId, page, size);
        
        return ResponseEntity.ok(
            ApiResponse.success("주문 목록 조회가 완료되었습니다", response)
        );
    }
}
```

**설계 특징**:
- **REST 규약 준수**: HTTP 메서드와 상태 코드 적절히 사용
- **입력 검증**: `@Valid`와 `@Validated`로 요청 데이터 검증
- **일관된 응답**: `ApiResponse` 래퍼로 통일된 응답 형식
- **페이징 지원**: 대량 데이터 조회 시 성능 고려

## DTO 계층 (Data Transfer Objects)

### ApiResponse.java
**역할**: 공통 API 응답 포맷

**핵심 코드 분석**:
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private String code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "요청이 성공적으로 처리되었습니다.", data);
    }
    
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

**설계 특징**:
- **제네릭 타입**: 다양한 데이터 타입 지원
- **팩토리 메서드**: 일관된 객체 생성 패턴
- **JSON 최적화**: null 필드 제외로 응답 크기 최소화

### OrderRequest.java
**역할**: 주문 생성 요청 데이터

**핵심 코드 분석**:
```java
public class OrderRequest {
    
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<OrderItemRequest> orderItems;
    
    public static class OrderItemRequest {
        @NotNull(message = "상품 ID는 필수입니다.")
        private Long productId;

        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 1개 이상이어야 합니다.")
        private Integer quantity;
    }
}
```

**설계 특징**:
- **중첩 클래스**: 관련 데이터 구조를 논리적으로 그룹화
- **Bean Validation**: 선언적 입력 검증
- **명확한 오류 메시지**: 사용자 친화적인 검증 메시지

## 예외 처리 계층

### GlobalExceptionHandler.java
**역할**: 전역 예외 처리 및 오류 응답 표준화

**핵심 코드 분석**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        logger.warn("Business exception occurred: {}", e.getMessage(), e);
        
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), e.getMessage());
        
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
    
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockException(OptimisticLockException e) {
        logger.warn("Optimistic lock exception occurred: {}", e.getMessage(), e);
        
        ErrorCode errorCode = ErrorCode.OPTIMISTIC_LOCK_EXCEPTION;
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
            ErrorCode.INVALID_PARAMETER.getCode(),
            "입력값 검증에 실패했습니다",
            errors
        );
        
        return ResponseEntity.badRequest().body(response);
    }
}
```

**설계 특징**:
- **계층별 예외 처리**: 비즈니스 예외, 검증 예외, 시스템 예외 분류
- **적절한 HTTP 상태**: 예외 유형에 맞는 상태 코드 반환
- **상세한 로깅**: 문제 추적을 위한 로그 레벨 구분
- **사용자 친화적**: 기술적 세부사항 숨기고 의미있는 메시지 제공

## 설정 클래스

### AsyncConfig.java
**역할**: 비동기 처리 설정 및 스레드 풀 관리

**핵심 코드 분석**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // 기본 스레드 수
        executor.setMaxPoolSize(10);        // 최대 스레드 수  
        executor.setQueueCapacity(100);     // 큐 용량
        executor.setThreadNamePrefix("Async-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()  // 큐 풀 시 호출 스레드에서 실행
        );
        executor.initialize();
        return executor;
    }
}
```

**설계 특징**:
- **적절한 스레드 풀 크기**: CPU 집약적이지 않은 I/O 작업에 최적화
- **큐 용량 제한**: 메모리 사용량 제어
- **거부 정책**: CallerRunsPolicy로 시스템 안정성 보장

## 코드 품질 특징

### 1. SOLID 원칙 준수
- **단일 책임**: 각 클래스는 하나의 명확한 책임
- **개방 폐쇄**: 인터페이스를 통한 확장 가능
- **리스코프 치환**: 상속 관계에서 치환 가능
- **인터페이스 분리**: 클라이언트별 인터페이스 분리
- **의존성 역전**: 추상화에 의존, 구체화 X

### 2. 클린 코드 원칙
- **의미있는 이름**: 변수, 메서드, 클래스명이 명확
- **작은 함수**: 한 가지 일만 하는 작은 메서드
- **주석보다 코드**: 자명한 코드로 의도 표현
- **일관된 형식**: 코딩 스타일 일관성 유지

### 3. 테스트 가능한 설계
- **의존성 주입**: Constructor Injection으로 테스트 용이성
- **순수 함수**: 사이드 이펙트 최소화
- **Mock 친화적**: 인터페이스 기반 설계

### 4. 성능 고려사항
- **지연 로딩**: 필요한 시점에만 데이터 로딩
- **배치 처리**: N+1 쿼리 방지
- **커넥션 풀링**: 데이터베이스 연결 최적화
- **인덱스 활용**: 쿼리 성능 최적화

이 코드 구조는 유지보수성, 확장성, 테스트 용이성을 모두 고려한 엔터프라이즈급 애플리케이션의 표준적인 구조를 보여줍니다.