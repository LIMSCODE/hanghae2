# 테스트 가이드

## 개요
이 문서는 E-Commerce 상품 주문 서비스의 테스트 전략과 구현된 테스트 코드에 대해 상세히 설명합니다.
단위 테스트부터 통합 테스트, 동시성 테스트까지 포괄적인 테스트 커버리지를 제공합니다.

## 테스트 전략

### 테스트 피라미드
```
        🔺 E2E 테스트
       🔺🔺🔺 통합 테스트  
    🔺🔺🔺🔺🔺 단위 테스트
```

**구성 비율**:
- **단위 테스트**: 70% - 빠르고 안정적인 피드백
- **통합 테스트**: 20% - 컴포넌트 간 상호작용 검증
- **E2E 테스트**: 10% - 전체 시나리오 검증

### 테스트 분류

#### 1. 단위 테스트 (Unit Tests)
- **목적**: 개별 클래스/메서드의 기능 검증
- **범위**: 도메인 로직, 서비스 로직, 컨트롤러 로직
- **도구**: JUnit 5, Mockito, AssertJ
- **격리**: Mock을 사용한 의존성 격리

#### 2. 통합 테스트 (Integration Tests)
- **목적**: 여러 컴포넌트 간 상호작용 검증
- **범위**: 서비스-리포지토리, 컨트롤러-서비스
- **도구**: Spring Boot Test, TestContainers
- **데이터베이스**: H2 인메모리 DB 사용

#### 3. 동시성 테스트 (Concurrency Tests)
- **목적**: 멀티스레드 환경에서의 데이터 일관성 검증
- **범위**: 재고 관리, 잔액 처리
- **도구**: ExecutorService, CountDownLatch
- **핵심**: 실제 동시성 이슈 시뮬레이션

## 구현된 테스트 코드 분석

### 1. 도메인 단위 테스트

#### UserTest.java
**테스트 대상**: User 엔티티의 비즈니스 로직

**주요 테스트 케이스**:
```java
@Nested
@DisplayName("잔액 충전 테스트")
class ChargeBalanceTest {
    
    @Test
    @DisplayName("정상적인 잔액 충전")
    void chargeBalance_Success() {
        // given
        User user = new User("testUser", "test@example.com");
        BigDecimal chargeAmount = BigDecimal.valueOf(10000);
        
        // when
        user.chargeBalance(chargeAmount);
        
        // then
        assertThat(user.getBalance()).isEqualTo(BigDecimal.valueOf(10000));
    }
}
```

**테스트 특징**:
- **순수 도메인 로직**: 외부 의존성 없이 테스트
- **예외 케이스 포함**: null, 음수, 0원 충전 등
- **상태 검증**: 메서드 실행 후 객체 상태 확인
- **명확한 네이밍**: 테스트 의도를 명확히 표현

#### ProductTest.java  
**테스트 대상**: Product 엔티티의 재고 관리 로직

**핵심 테스트**:
```java
@Test
@DisplayName("재고 부족 시 예외 발생")
void deductStock_InsufficientStock_ThrowsException() {
    // given
    Product product = new Product("iPhone 15", "Apple iPhone", 
                                 BigDecimal.valueOf(1000000), 3);
    Integer deductQuantity = 5;

    // when & then
    assertThatThrownBy(() -> product.deductStock(deductQuantity))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("재고가 부족합니다. 요청: 5개, 현재 재고: 3개");
}
```

**검증 포인트**:
- **비즈니스 규칙**: 재고 부족 시 예외 발생
- **예외 메시지**: 사용자 친화적인 오류 메시지
- **경계값 테스트**: 정확한 재고량에서의 동작

### 2. 서비스 단위 테스트

#### UserServiceTest.java
**테스트 대상**: UserService의 비즈니스 로직

**Mock 사용 패턴**:
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock 
    private BalanceHistoryRepository balanceHistoryRepository;
    
    @InjectMocks
    private UserService userService;
}
```

**핵심 테스트 케이스**:
```java
@Test
@DisplayName("정상적인 잔액 충전")
void chargeBalance_Success() {
    // given - Mock 동작 정의
    given(userRepository.findByIdWithLock(userId)).willReturn(Optional.of(user));
    given(userRepository.save(any(User.class))).willReturn(user);
    
    // when - 실제 메서드 호출
    BalanceResponse response = userService.chargeBalance(userId, chargeAmount);
    
    // then - 결과 및 상호작용 검증
    assertThat(response.getBalance()).isEqualTo(chargeAmount);
    verify(userRepository).findByIdWithLock(userId);
    verify(balanceHistoryRepository).save(any(BalanceHistory.class));
}
```

**테스트 패턴**:
- **Given-When-Then 구조**: 명확한 테스트 구조
- **Mock 동작 정의**: `given().willReturn()` 패턴
- **상호작용 검증**: `verify()` 를 통한 메서드 호출 확인
- **예외 시나리오**: 다양한 실패 케이스 테스트

#### OrderServiceTest.java
**테스트 대상**: 복잡한 주문 생성 로직

**복잡한 시나리오 테스트**:
```java
@Test
@DisplayName("정상적인 주문 생성")
void createOrder_Success() {
    // given - 여러 Mock 객체 설정
    given(userService.getUserWithLock(userId)).willReturn(user);
    given(productService.getProductWithLock(productId1)).willReturn(product1);
    doNothing().when(productService).validateStock(any(), any());
    
    // when
    OrderResponse response = orderService.createOrder(orderRequest);
    
    // then - 복잡한 비즈니스 플로우 검증
    assertThat(response.getOrderStatus()).isEqualTo("COMPLETED");
    verify(productService).deductStock(product1, 2);
    verify(eventPublisher).publishEvent(any(OrderCompletedEvent.class));
}
```

**테스트 특징**:
- **복잡한 의존성**: 여러 서비스 간 상호작용
- **트랜잭션 동작**: 전체 플로우의 원자성 검증
- **이벤트 발행**: 비동기 이벤트 발행 확인

### 3. 컨트롤러 테스트

#### BalanceControllerTest.java
**테스트 대상**: REST API 엔드포인트

**Web Layer 테스트**:
```java
@WebMvcTest(BalanceController.class)
class BalanceControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
}
```

**API 테스트 패턴**:
```java
@Test
@DisplayName("정상적인 잔액 충전")
void chargeBalance_Success() throws Exception {
    // given
    given(userService.chargeBalance(eq(userId), eq(chargeAmount)))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/v1/users/{userId}/balance/charge", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.balance").value(15000));
}
```

**검증 포인트**:
- **HTTP 상태 코드**: 적절한 응답 코드 반환
- **JSON 응답**: 응답 데이터 구조 및 값 검증
- **입력 검증**: `@Valid` 어노테이션 동작 확인
- **예외 처리**: GlobalExceptionHandler 동작 확인

### 4. 통합 테스트

#### OrderIntegrationTest.java
**테스트 대상**: 전체 애플리케이션 컨텍스트

**통합 테스트 설정**:
```java
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class OrderIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserRepository userRepository;
}
```

**실제 데이터베이스 검증**:
```java
@Test
@DisplayName("정상적인 주문 생성 통합 테스트")
void createOrder_Integration_Success() throws Exception {
    // given - 실제 데이터 저장
    User savedUser = userRepository.save(user);
    Product savedProduct = productRepository.save(product);

    // when - 실제 API 호출
    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(orderRequest)))
            .andExpect(status().isOk());

    // then - 데이터베이스 상태 검증
    User updatedUser = userRepository.findById(savedUser.getUserId()).orElseThrow();
    assertThat(updatedUser.getBalance()).isEqualTo(expectedBalance);
}
```

**통합 테스트 특징**:
- **실제 컴포넌트**: Mock 대신 실제 빈 사용
- **데이터베이스 트랜잭션**: 실제 DB 연산 검증
- **전체 플로우**: 컨트롤러부터 리포지토리까지

### 5. 동시성 테스트

#### 재고 동시성 테스트
**목적**: 한정된 재고에 대한 동시 접근 검증

```java
@Test
@DisplayName("동시성 테스트 - 재고 1개 상품에 대한 동시 주문")
void createOrder_Concurrency_SingleStockItem() throws InterruptedException {
    // given
    int threadCount = 100;
    CountDownLatch latch = new CountDownLatch(threadCount);
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    // 재고 1개인 상품 생성
    Product product = new Product("Limited Edition", "한정판 상품", 
                                 BigDecimal.valueOf(50000), 1);

    // 100명의 동시 주문 시뮬레이션
    for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
            try {
                orderService.createOrder(orderRequest);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
    }

    // when
    latch.await();
    
    // then - 동시성 안전성 검증
    assertThat(successCount.get()).isEqualTo(1);  // 1명만 성공
    assertThat(failureCount.get()).isEqualTo(99); // 99명 실패
    assertThat(finalProduct.getStockQuantity()).isEqualTo(0); // 재고 정확히 0
}
```

#### 잔액 동시성 테스트
**목적**: 동일 사용자의 동시 결제 검증

```java
@Test
@DisplayName("동시성 테스트 - 같은 사용자의 동시 잔액 차감")
void createOrder_Concurrency_SameUserBalance() throws InterruptedException {
    // given
    User user = createUserWithBalance(100000); // 10만원 보유
    Product product = createProduct(15000, 100); // 1만5천원 상품, 충분한 재고
    
    // 10번의 동시 주문 (총 15만원 필요, 하지만 10만원만 보유)
    int threadCount = 10;
    
    // when - 동시 실행
    
    // then
    assertThat(successCount.get()).isLessThanOrEqualTo(6); // 최대 6개만 성공 가능
    
    // 잔액 정확성 검증
    BigDecimal expectedBalance = BigDecimal.valueOf(100000)
            .subtract(BigDecimal.valueOf(successCount.get() * 15000));
    assertThat(finalUser.getBalance()).isEqualTo(expectedBalance);
}
```

**동시성 테스트 특징**:
- **실제 경쟁 조건**: ExecutorService로 실제 멀티스레드 환경 구현
- **원자성 검증**: AtomicInteger로 안전한 카운터 구현
- **데이터 일관성**: 최종 데이터 상태의 정확성 검증
- **경계값 테스트**: 정확히 한계점에서의 동작 검증

## 테스트 실행 전략

### 테스트 분류별 실행
```bash
# 단위 테스트만 실행
./gradlew test --tests "*Test"

# 통합 테스트만 실행  
./gradlew test --tests "*IntegrationTest"

# 특정 클래스 테스트
./gradlew test --tests "UserServiceTest"

# 특정 메서드 테스트
./gradlew test --tests "UserServiceTest.chargeBalance_Success"
```

### 테스트 프로파일
```yaml
# test profile (application-test.yml)
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

### 테스트 데이터 관리
```java
@TestMethodOrder(OrderAnnotation.class)
@Transactional
class IntegrationTest {
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
    }
    
    @AfterEach  
    void tearDown() {
        // 데이터 정리
    }
}
```

## 테스트 커버리지

### 목표 커버리지
- **라인 커버리지**: 85% 이상
- **브랜치 커버리지**: 80% 이상
- **메서드 커버리지**: 90% 이상

### 커버리지 측정
```bash
./gradlew test jacocoTestReport

# 리포트 위치: build/reports/jacoco/test/html/index.html
```

### 커버리지 제외 대상
- Configuration 클래스
- DTO/Entity의 getter/setter
- Exception 클래스
- Main 클래스

## 테스트 Best Practices

### 1. 테스트 네이밍 규칙
```java
@DisplayName("메서드명_상황_예상결과")
void chargeBalance_InsufficientAmount_ThrowsException()
```

### 2. Given-When-Then 패턴
```java
@Test
void testMethod() {
    // given - 테스트 데이터 준비
    
    // when - 실제 실행
    
    // then - 결과 검증
}
```

### 3. 한 테스트당 하나의 검증
```java
// Good
@Test
void userBalance_ShouldIncrease_WhenCharging() {
    // 잔액 증가만 검증
}

@Test  
void balanceHistory_ShouldBeSaved_WhenCharging() {
    // 이력 저장만 검증
}
```

### 4. 테스트 독립성
```java
@TestMethodOrder(MethodOrderer.Random.class) // 실행 순서 무관
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

### 5. Mock 최소화
```java
// 단위 테스트에만 Mock 사용
// 통합 테스트는 실제 구현체 사용
```

## 테스트 실행 결과 분석

### 성공 지표
- **모든 테스트 통과**: 초록불 상태 유지
- **빌드 시간**: 전체 테스트 5분 이내
- **동시성 테스트**: 일관된 결과 보장

### 실패 시 대응
1. **즉시 원인 분석**: 실패한 테스트부터 우선 해결
2. **회귀 테스트**: 수정 후 전체 테스트 재실행
3. **테스트 개선**: 불안정한 테스트 리팩토링

## CI/CD와의 연동

### GitHub Actions 예시
```yaml
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run tests
        run: ./gradlew test
      - name: Generate test report
        run: ./gradlew jacocoTestReport
```

### 배포 전 필수 조건
- ✅ 모든 단위 테스트 통과
- ✅ 통합 테스트 통과  
- ✅ 동시성 테스트 통과
- ✅ 커버리지 목표 달성

이 테스트 전략을 통해 E-Commerce 주문 시스템의 안정성과 신뢰성을 보장하며, 지속적인 코드 품질 향상을 달성할 수 있습니다.