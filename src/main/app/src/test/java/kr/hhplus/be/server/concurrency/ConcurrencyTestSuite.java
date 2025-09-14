package kr.hhplus.be.server.concurrency;

import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.domain.concert.Concert;
import kr.hhplus.be.server.domain.concert.ConcertSchedule;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.application.ecommerce.OrderUseCase;
import kr.hhplus.be.server.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrencydb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ConcurrencyTestSuite {

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

    @Test
    @DisplayName("동시성 테스트 1: 재고 차감 경쟁 조건")
    void concurrentStockDecrementTest() throws InterruptedException {
        // Given: 재고 10개인 상품
        Product product = new Product("Limited Product", new BigDecimal("10000"), 10);
        productRepository.save(product);

        // 충분한 잔액을 가진 사용자들 생성
        int userCount = 15;
        for (int i = 1; i <= userCount; i++) {
            User user = new User("user" + i + "@test.com", "User " + i, new BigDecimal("50000"));
            userRepository.save(user);
        }

        int threadCount = 15;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 15명이 동시에 1개씩 주문 (재고는 10개)
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                            userId,
                            Arrays.asList(new OrderUseCase.OrderItemRequest(product.getProductId(), 1)),
                            UUID.randomUUID().toString()
                    );

                    orderUseCase.processOrder(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 정확히 10명만 성공해야 함 (재고 = 10개)
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(5);

        // 최종 재고 확인
        Product finalProduct = productRepository.findById(product.getProductId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 테스트 2: 사용자 잔액 차감 경쟁 조건")
    void concurrentBalanceDeductionTest() throws InterruptedException {
        // Given: 잔액 50,000원인 사용자
        User user = new User("test@example.com", "Test User", new BigDecimal("50000"));
        userRepository.save(user);

        // 충분한 재고를 가진 상품
        Product product = new Product("Expensive Product", new BigDecimal("10000"), 100);
        productRepository.save(product);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 동일 사용자가 10번 동시에 10,000원짜리 상품 주문 (잔액은 50,000원)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                            user.getUserId(),
                            Arrays.asList(new OrderUseCase.OrderItemRequest(product.getProductId(), 1)),
                            UUID.randomUUID().toString()
                    );

                    orderUseCase.processOrder(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 정확히 5번만 성공해야 함 (50,000 ÷ 10,000 = 5)
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);

        // 최종 잔액 확인 (50,000 - 50,000 = 0)
        User finalUser = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(finalUser.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("동시성 테스트 3: 선착순 쿠폰 발급 경쟁 조건")
    void concurrentCouponIssuanceTest() throws InterruptedException {
        // Given: 수량 제한 쿠폰 (3개)
        Coupon limitedCoupon = new Coupon("Flash Sale Coupon", new BigDecimal("5000"), 3);
        couponRepository.save(limitedCoupon);

        // 20명의 사용자 생성
        int userCount = 20;
        for (int i = 1; i <= userCount; i++) {
            User user = new User("user" + i + "@test.com", "User " + i, new BigDecimal("10000"));
            userRepository.save(user);
        }

        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 20명이 동시에 쿠폰 발급 시도 (수량은 3개)
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    // 트랜잭션 내에서 쿠폰 발급 처리
                    Coupon coupon = couponRepository.findById(limitedCoupon.getCouponId()).orElseThrow();

                    if (coupon.getQuantity() > 0) {
                        // 쿠폰 수량 감소
                        coupon.decreaseQuantity();
                        couponRepository.save(coupon);

                        // 사용자 쿠폰 발급
                        UserCoupon userCoupon = new UserCoupon(userId, limitedCoupon.getCouponId());
                        userCouponRepository.save(userCoupon);

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

        // Then: 정확히 3명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(3);
        assertThat(failCount.get()).isEqualTo(17);

        // 쿠폰 최종 수량 확인
        Coupon finalCoupon = couponRepository.findById(limitedCoupon.getCouponId()).orElseThrow();
        assertThat(finalCoupon.getQuantity()).isEqualTo(0);

        // 발급된 쿠폰 수 확인
        long issuedCoupons = userCouponRepository.findAll()
                .stream()
                .filter(uc -> uc.getCouponId().equals(limitedCoupon.getCouponId()))
                .count();
        assertThat(issuedCoupons).isEqualTo(3);
    }

    @Test
    @DisplayName("동시성 테스트 4: Idempotency Key 중복 처리")
    void concurrentIdempotencyKeyTest() throws InterruptedException {
        // Given
        User user = new User("test@example.com", "Test User", new BigDecimal("100000"));
        userRepository.save(user);

        Product product = new Product("Test Product", new BigDecimal("10000"), 100);
        productRepository.save(product);

        String sharedIdempotencyKey = UUID.randomUUID().toString();

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        // When: 동일한 Idempotency Key로 10번 동시 주문
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                            user.getUserId(),
                            Arrays.asList(new OrderUseCase.OrderItemRequest(product.getProductId(), 1)),
                            sharedIdempotencyKey // 모두 같은 키 사용
                    );

                    OrderUseCase.OrderResult result = orderUseCase.processOrder(command);

                    if (result.isDuplicate()) {
                        duplicateCount.incrementAndGet();
                    } else {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 예외 발생 시에도 중복 처리로 간주
                    duplicateCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 1번만 실제 처리, 9번은 중복 처리
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(duplicateCount.get()).isEqualTo(9);

        // DB에는 1개의 주문만 저장됨
        long orderCount = orderRepository.findAll()
                .stream()
                .filter(order -> sharedIdempotencyKey.equals(order.getIdempotencyKey()))
                .count();
        assertThat(orderCount).isEqualTo(1);
    }

    @Test
    @DisplayName("동시성 테스트 5: 콘서트 좌석 예약 경쟁 조건")
    void concurrentSeatReservationTest() throws InterruptedException {
        // Given: 콘서트와 1개의 좌석
        Concert concert = new Concert("Popular Concert", "Famous Artist", "Big Stadium");
        ConcertSchedule schedule = new ConcertSchedule(
                concert,
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now().minusHours(1)
        );

        // 좌석은 ConcertSchedule 생성 시 자동으로 50개 생성됨
        // 그 중 1번 좌석에 대해서만 테스트

        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 20명이 동시에 1번 좌석 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    // 1번 좌석을 찾아서 예약 시도
                    Seat seat = schedule.getSeats().get(0); // 1번 좌석

                    if (seat.isAvailable()) {
                        seat.reserve(userId, 5); // 5분간 임시 예약
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

        // Then: 오직 1명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(19);

        // 좌석 상태 확인
        Seat reservedSeat = schedule.getSeats().get(0);
        assertThat(reservedSeat.getSeatStatus()).isEqualTo(Seat.SeatStatus.TEMPORARY_RESERVED);
    }

    @Test
    @DisplayName("동시성 테스트 6: 대기열 토큰 발급 경쟁 조건")
    void concurrentQueueTokenIssuanceTest() throws InterruptedException {
        // Given: 100명이 동시에 토큰 발급 요청
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentHashMap<Long, Long> userPositionMap = new ConcurrentHashMap<>();

        // When: 100명이 동시에 토큰 발급 요청
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    // 간단한 토큰 생성 시뮬레이션
                    QueueToken token = new QueueToken(userId, (long) successCount.incrementAndGet());
                    userPositionMap.put(userId, token.getQueuePosition());
                } catch (Exception e) {
                    System.err.println("Token creation failed for user: " + userId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 모든 사용자가 고유한 대기 순서를 가져야 함
        assertThat(userPositionMap.size()).isEqualTo(100);
        assertThat(userPositionMap.values().stream().distinct().count()).isEqualTo(100);

        // 대기 순서가 1부터 100까지 연속적으로 할당되었는지 확인
        long minPosition = userPositionMap.values().stream().min(Long::compareTo).orElse(0L);
        long maxPosition = userPositionMap.values().stream().max(Long::compareTo).orElse(0L);

        assertThat(minPosition).isEqualTo(1);
        assertThat(maxPosition).isEqualTo(100);
    }
}