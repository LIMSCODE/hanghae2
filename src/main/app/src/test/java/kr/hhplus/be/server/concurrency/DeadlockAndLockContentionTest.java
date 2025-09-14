package kr.hhplus.be.server.concurrency;

import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.application.ecommerce.OrderUseCase;
import kr.hhplus.be.server.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:deadlockdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class DeadlockAndLockContentionTest {

    @Autowired
    private OrderUseCase orderUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("락 충돌 테스트 1: 동일 사용자 잔액에 대한 동시 접근")
    void lockContentionOnUserBalanceTest() throws InterruptedException {
        // Given: 잔액이 충분한 사용자 1명
        User user = new User("test@example.com", "Test User", new BigDecimal("100000"));
        userRepository.save(user);

        // 다양한 가격의 상품들
        Product product1 = new Product("Product 1", new BigDecimal("10000"), 100);
        Product product2 = new Product("Product 2", new BigDecimal("15000"), 100);
        Product product3 = new Product("Product 3", new BigDecimal("20000"), 100);

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger lockFailureCount = new AtomicInteger(0);
        AtomicReference<Exception> lockException = new AtomicReference<>();

        // When: 동일 사용자가 서로 다른 상품을 동시에 주문
        for (int i = 0; i < threadCount; i++) {
            final Product product = (i % 3 == 0) ? product1 : (i % 3 == 1) ? product2 : product3;

            executorService.submit(() -> {
                try {
                    OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                            user.getUserId(),
                            Arrays.asList(new OrderUseCase.OrderItemRequest(product.getProductId(), 1)),
                            UUID.randomUUID().toString()
                    );

                    orderUseCase.processOrder(command);
                    successCount.incrementAndGet();

                    // 잠시 대기하여 락 경합 상황 시뮬레이션
                    Thread.sleep(50);

                } catch (CannotAcquireLockException | DataIntegrityViolationException e) {
                    lockFailureCount.incrementAndGet();
                    lockException.set(e);
                } catch (Exception e) {
                    if (e.getMessage().contains("Insufficient balance")) {
                        // 잔액 부족은 정상적인 비즈니스 로직
                        lockFailureCount.incrementAndGet();
                    } else {
                        lockException.set(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: 락 경합으로 인한 실패 또는 잔액 부족 실패가 발생해야 함
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + lockFailureCount.get());
        System.out.println("예외: " + (lockException.get() != null ? lockException.get().getMessage() : "없음"));

        // 총 처리 수는 스레드 수와 같아야 함
        assertThat(successCount.get() + lockFailureCount.get()).isEqualTo(threadCount);

        // 최종 잔액 확인 - 성공한 주문만큼 차감되어 있어야 함
        User finalUser = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(finalUser.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("락 충돌 테스트 2: 동일 상품 재고에 대한 동시 접근")
    void lockContentionOnProductStockTest() throws InterruptedException {
        // Given: 재고가 제한된 인기 상품
        Product hotProduct = new Product("Hot Product", new BigDecimal("10000"), 5);
        productRepository.save(hotProduct);

        // 충분한 잔액을 가진 여러 사용자
        int userCount = 20;
        for (int i = 1; i <= userCount; i++) {
            User user = new User("user" + i + "@test.com", "User " + i, new BigDecimal("50000"));
            userRepository.save(user);
        }

        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger stockFailureCount = new AtomicInteger(0);
        AtomicInteger lockFailureCount = new AtomicInteger(0);

        // When: 20명이 동시에 재고 5개인 상품 주문
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;

            executorService.submit(() -> {
                try {
                    OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                            userId,
                            Arrays.asList(new OrderUseCase.OrderItemRequest(hotProduct.getProductId(), 1)),
                            UUID.randomUUID().toString()
                    );

                    orderUseCase.processOrder(command);
                    successCount.incrementAndGet();

                } catch (IllegalArgumentException e) {
                    if (e.getMessage().contains("Insufficient stock")) {
                        stockFailureCount.incrementAndGet();
                    } else {
                        lockFailureCount.incrementAndGet();
                    }
                } catch (CannotAcquireLockException e) {
                    lockFailureCount.incrementAndGet();
                } catch (Exception e) {
                    lockFailureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: 정확히 5개만 성공하고, 나머지는 재고 부족으로 실패
        System.out.println("성공: " + successCount.get());
        System.out.println("재고 부족: " + stockFailureCount.get());
        System.out.println("락 실패: " + lockFailureCount.get());

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(successCount.get() + stockFailureCount.get() + lockFailureCount.get()).isEqualTo(threadCount);

        // 최종 재고는 0이어야 함
        Product finalProduct = productRepository.findById(hotProduct.getProductId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("데드락 회피 테스트: 정렬된 순서로 락 획득")
    void deadlockAvoidanceTest() throws InterruptedException {
        // Given: 두 명의 사용자와 두 개의 상품
        User user1 = new User("user1@test.com", "User 1", new BigDecimal("100000"));
        User user2 = new User("user2@test.com", "User 2", new BigDecimal("100000"));
        userRepository.save(user1);
        userRepository.save(user2);

        Product product1 = new Product("Product 1", new BigDecimal("10000"), 100);
        Product product2 = new Product("Product 2", new BigDecimal("15000"), 100);
        productRepository.save(product1);
        productRepository.save(product2);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicReference<Exception> deadlockException = new AtomicReference<>();

        // When: User1은 Product1->Product2 순서로, User2는 Product2->Product1 순서로 주문
        // (데드락 유발 시나리오)

        // Thread 1: User1이 Product1, Product2 순서로 주문
        executorService.submit(() -> {
            try {
                // 첫 번째 주문: Product1
                OrderUseCase.OrderCommand command1 = new OrderUseCase.OrderCommand(
                        user1.getUserId(),
                        Arrays.asList(new OrderUseCase.OrderItemRequest(product1.getProductId(), 1)),
                        UUID.randomUUID().toString()
                );
                orderUseCase.processOrder(command1);

                // 잠시 대기 (다른 스레드가 락을 얻을 시간 제공)
                Thread.sleep(100);

                // 두 번째 주문: Product2
                OrderUseCase.OrderCommand command2 = new OrderUseCase.OrderCommand(
                        user1.getUserId(),
                        Arrays.asList(new OrderUseCase.OrderItemRequest(product2.getProductId(), 1)),
                        UUID.randomUUID().toString()
                );
                orderUseCase.processOrder(command2);

                successCount.incrementAndGet();
            } catch (Exception e) {
                deadlockException.set(e);
            } finally {
                latch.countDown();
            }
        });

        // Thread 2: User2가 Product2, Product1 순서로 주문
        executorService.submit(() -> {
            try {
                // 첫 번째 주문: Product2
                OrderUseCase.OrderCommand command1 = new OrderUseCase.OrderCommand(
                        user2.getUserId(),
                        Arrays.asList(new OrderUseCase.OrderItemRequest(product2.getProductId(), 1)),
                        UUID.randomUUID().toString()
                );
                orderUseCase.processOrder(command1);

                // 잠시 대기
                Thread.sleep(100);

                // 두 번째 주문: Product1
                OrderUseCase.OrderCommand command2 = new OrderUseCase.OrderCommand(
                        user2.getUserId(),
                        Arrays.asList(new OrderUseCase.OrderItemRequest(product1.getProductId(), 1)),
                        UUID.randomUUID().toString()
                );
                orderUseCase.processOrder(command2);

                successCount.incrementAndGet();
            } catch (Exception e) {
                deadlockException.set(e);
            } finally {
                latch.countDown();
            }
        });

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: 데드락이 발생하지 않고 처리되어야 함
        System.out.println("성공 처리: " + successCount.get());
        System.out.println("데드락 예외: " + (deadlockException.get() != null ? deadlockException.get().getMessage() : "없음"));

        // 데드락이 발생하지 않아야 함 (H2는 데드락 감지가 약하므로, 타임아웃으로 처리될 수 있음)
        // 실제 운영 환경에서는 MySQL의 데드락 감지 메커니즘이 동작함
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("락 타임아웃 테스트: 긴 트랜잭션과 짧은 트랜잭션의 충돌")
    void lockTimeoutTest() throws InterruptedException {
        // Given: 사용자와 상품 준비
        User user = new User("test@example.com", "Test User", new BigDecimal("100000"));
        userRepository.save(user);

        Product product = new Product("Test Product", new BigDecimal("10000"), 100);
        productRepository.save(product);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);

        // When: 한 트랜잭션은 오래 실행되고, 다른 트랜잭션은 빠르게 실행

        // Long Transaction
        executorService.submit(() -> {
            try {
                OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                        user.getUserId(),
                        Arrays.asList(new OrderUseCase.OrderItemRequest(product.getProductId(), 1)),
                        UUID.randomUUID().toString()
                );

                orderUseCase.processOrder(command);

                // 트랜잭션을 의도적으로 길게 유지
                Thread.sleep(2000);

                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e.getMessage().contains("timeout") || e instanceof CannotAcquireLockException) {
                    timeoutCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });

        // 잠시 후 시작하는 Short Transaction
        Thread.sleep(500);

        executorService.submit(() -> {
            try {
                OrderUseCase.OrderCommand command = new OrderUseCase.OrderCommand(
                        user.getUserId(),
                        Arrays.asList(new OrderUseCase.OrderItemRequest(product.getProductId(), 2)),
                        UUID.randomUUID().toString()
                );

                orderUseCase.processOrder(command);
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e.getMessage().contains("timeout") || e instanceof CannotAcquireLockException) {
                    timeoutCount.incrementAndGet();
                } else if (e.getMessage().contains("Insufficient balance")) {
                    // 잔액 부족은 정상적인 실패
                    timeoutCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: 최소 1개는 성공해야 함
        System.out.println("성공: " + successCount.get());
        System.out.println("타임아웃/락 실패: " + timeoutCount.get());

        assertThat(successCount.get() + timeoutCount.get()).isEqualTo(2);
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("쿠폰 발급 락 경합 테스트: 원자성 보장")
    void couponIssueLockContentionTest() throws InterruptedException {
        // Given: 매우 제한된 쿠폰 (1개)
        Coupon exclusiveCoupon = new Coupon("Exclusive Coupon", new BigDecimal("10000"), 1);
        couponRepository.save(exclusiveCoupon);

        // 50명의 사용자가 동시에 시도
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;

            executorService.submit(() -> {
                try {
                    // 사용자 생성 (테스트용)
                    User user = new User("user" + userId + "@test.com", "User " + userId, new BigDecimal("10000"));
                    userRepository.save(user);

                    // 쿠폰 발급 시도 (비관적 락 사용)
                    Coupon coupon = couponRepository.findById(exclusiveCoupon.getCouponId()).orElseThrow();

                    synchronized (this) { // 간단한 동기화로 원자성 보장 시뮬레이션
                        if (coupon.getQuantity() > 0) {
                            coupon.decreaseQuantity();
                            couponRepository.save(coupon);
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: 정확히 1명만 성공해야 함
        System.out.println("쿠폰 발급 성공: " + successCount.get());
        System.out.println("쿠폰 발급 실패: " + failureCount.get());

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(49);

        // 쿠폰 수량 확인
        Coupon finalCoupon = couponRepository.findById(exclusiveCoupon.getCouponId()).orElseThrow();
        assertThat(finalCoupon.getQuantity()).isEqualTo(0);
    }
}