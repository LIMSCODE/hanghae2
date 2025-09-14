package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.application.ecommerce.OrderUseCase;
import kr.hhplus.be.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ECommerceIntegrationTest {

    @Autowired
    private OrderUseCase orderUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User("testuser@example.com", "Test User", new BigDecimal("100000"));
        userRepository.save(testUser);

        // 테스트 상품 생성
        testProduct = new Product("Test Product", new BigDecimal("10000"), 100);
        productRepository.save(testProduct);
    }

    @Test
    @DisplayName("충전 API → 주문 API 전체 플로우 통합 테스트")
    @Transactional
    void fullOrderFlowIntegrationTest() {
        // Given: 사용자가 충전을 하고 상품 주문 준비
        Long userId = testUser.getUserId();
        Long productId = testProduct.getProductId();

        // 잔액 확인 (초기 잔액: 100,000원)
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getBalance()).isEqualByComparingTo(new BigDecimal("100000"));

        // When: 주문 처리
        String idempotencyKey = UUID.randomUUID().toString();
        OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                userId,
                Arrays.asList(new OrderUseCase.OrderItemRequest(productId, 2)),
                idempotencyKey
        );

        OrderUseCase.OrderResult result = orderUseCase.processOrder(command);

        // Then: 주문 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("20000")); // 10,000 * 2
        assertThat(result.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(result.isDuplicate()).isFalse();

        // 잔액 차감 확인
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getBalance()).isEqualByComparingTo(new BigDecimal("80000")); // 100,000 - 20,000

        // 재고 차감 확인
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(98); // 100 - 2

        // 주문 저장 확인
        Order savedOrder = orderRepository.findById(result.getOrderId()).orElseThrow();
        assertThat(savedOrder.getOrderItems()).hasSize(1);
        assertThat(savedOrder.getOrderItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("결제 API idempotency_key 중복 요청 테스트")
    void paymentIdempotencyTest() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                testUser.getUserId(),
                Arrays.asList(new OrderUseCase.OrderItemRequest(testProduct.getProductId(), 1)),
                idempotencyKey
        );

        // When: 같은 idempotency_key로 두 번 요청
        OrderUseCase.OrderResult firstResult = orderUseCase.processOrder(command);
        OrderUseCase.OrderResult secondResult = orderUseCase.processOrder(command);

        // Then: 첫 번째 요청은 정상 처리, 두 번째 요청은 중복 처리
        assertThat(firstResult.isDuplicate()).isFalse();
        assertThat(secondResult.isDuplicate()).isTrue();
        assertThat(firstResult.getOrderId()).isEqualTo(secondResult.getOrderId());

        // 실제로는 한 번만 처리되었는지 확인
        User user = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(user.getBalance()).isEqualByComparingTo(new BigDecimal("90000")); // 100,000 - 10,000 (한 번만 차감)

        Product product = productRepository.findById(testProduct.getProductId()).orElseThrow();
        assertThat(product.getStock()).isEqualTo(99); // 100 - 1 (한 번만 차감)
    }

    @Test
    @DisplayName("쿠폰 발급 경쟁 조건 테스트 - 동시에 요청 시 한 명만 성공")
    void couponIssueConcurrencyTest() throws InterruptedException {
        // Given: 수량이 제한된 쿠폰 생성
        Coupon limitedCoupon = new Coupon("Limited Coupon", new BigDecimal("5000"), 1); // 수량 1개
        couponRepository.save(limitedCoupon);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 동시에 쿠폰 발급 시도
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            // 테스트 사용자들 생성
            User user = new User("user" + userId + "@test.com", "User " + userId, new BigDecimal("10000"));
            userRepository.save(user);

            executorService.submit(() -> {
                try {
                    UserCoupon userCoupon = new UserCoupon(userId, limitedCoupon.getCouponId());
                    userCouponRepository.save(userCoupon);

                    // 쿠폰 수량 감소
                    Coupon coupon = couponRepository.findById(limitedCoupon.getCouponId()).orElseThrow();
                    if (coupon.getQuantity() > 0) {
                        coupon.decreaseQuantity();
                        couponRepository.save(coupon);
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 오직 한 명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        // 쿠폰 수량 확인
        Coupon finalCoupon = couponRepository.findById(limitedCoupon.getCouponId()).orElseThrow();
        assertThat(finalCoupon.getQuantity()).isEqualTo(0);

        // 발급된 쿠폰 수 확인
        List<UserCoupon> issuedCoupons = userCouponRepository.findAll()
                .stream()
                .filter(uc -> uc.getCouponId().equals(limitedCoupon.getCouponId()))
                .toList();
        assertThat(issuedCoupons).hasSize(1);
    }

    @Test
    @DisplayName("재고 부족 시 주문 실패 테스트")
    void orderFailWhenInsufficientStock() {
        // Given: 재고가 부족한 상품
        Product lowStockProduct = new Product("Low Stock Product", new BigDecimal("5000"), 1);
        productRepository.save(lowStockProduct);

        // When: 재고보다 많은 수량 주문 시도
        OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                testUser.getUserId(),
                Arrays.asList(new OrderUseCase.OrderItemRequest(lowStockProduct.getProductId(), 5)), // 재고 1개인데 5개 주문
                UUID.randomUUID().toString()
        );

        // Then: 예외 발생
        assertThatThrownBy(() -> orderUseCase.processOrder(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");

        // 잔액과 재고가 변경되지 않았는지 확인
        User user = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(user.getBalance()).isEqualByComparingTo(new BigDecimal("100000")); // 변경 없음

        Product product = productRepository.findById(lowStockProduct.getProductId()).orElseThrow();
        assertThat(product.getStock()).isEqualTo(1); // 변경 없음
    }

    @Test
    @DisplayName("잔액 부족 시 주문 실패 테스트")
    void orderFailWhenInsufficientBalance() {
        // Given: 잔액이 부족한 사용자
        User poorUser = new User("poor@test.com", "Poor User", new BigDecimal("1000")); // 1000원만 보유
        userRepository.save(poorUser);

        // When: 잔액보다 비싼 상품 주문 시도
        OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                poorUser.getUserId(),
                Arrays.asList(new OrderUseCase.OrderItemRequest(testProduct.getProductId(), 1)), // 10,000원 상품
                UUID.randomUUID().toString()
        );

        // Then: 예외 발생
        assertThatThrownBy(() -> orderUseCase.processOrder(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");

        // 재고가 변경되지 않았는지 확인
        Product product = productRepository.findById(testProduct.getProductId()).orElseThrow();
        assertThat(product.getStock()).isEqualTo(100); // 변경 없음
    }
}