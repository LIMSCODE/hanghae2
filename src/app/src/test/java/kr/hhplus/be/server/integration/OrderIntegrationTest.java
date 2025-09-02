package kr.hhplus.be.server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.OrderRequest;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.repository.UserRepository;
import kr.hhplus.be.server.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("주문 통합 테스트")
class OrderIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("정상적인 주문 생성 통합 테스트")
    @Transactional
    void createOrder_Integration_Success() throws Exception {
        // given
        User user = new User("testUser", "test@example.com");
        user.chargeBalance(BigDecimal.valueOf(100000));
        User savedUser = userRepository.save(user);

        Product product1 = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(30000), 10);
        Product product2 = new Product("Galaxy S24", "Samsung Galaxy", BigDecimal.valueOf(25000), 5);
        Product savedProduct1 = productRepository.save(product1);
        Product savedProduct2 = productRepository.save(product2);

        OrderRequest.OrderItemRequest item1 = new OrderRequest.OrderItemRequest(savedProduct1.getProductId(), 2);
        OrderRequest.OrderItemRequest item2 = new OrderRequest.OrderItemRequest(savedProduct2.getProductId(), 1);
        OrderRequest orderRequest = new OrderRequest(savedUser.getUserId(), Arrays.asList(item1, item2));

        // when & then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk());

        // 데이터 검증
        User updatedUser = userRepository.findById(savedUser.getUserId()).orElseThrow();
        Product updatedProduct1 = productRepository.findById(savedProduct1.getProductId()).orElseThrow();
        Product updatedProduct2 = productRepository.findById(savedProduct2.getProductId()).orElseThrow();

        assertThat(updatedUser.getBalance()).isEqualTo(BigDecimal.valueOf(15000)); // 100000 - 85000
        assertThat(updatedProduct1.getStockQuantity()).isEqualTo(8); // 10 - 2
        assertThat(updatedProduct2.getStockQuantity()).isEqualTo(4); // 5 - 1
    }

    @Test
    @DisplayName("동시성 테스트 - 재고 1개 상품에 대한 동시 주문")
    @Transactional
    void createOrder_Concurrency_SingleStockItem() throws InterruptedException {
        // given
        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 재고 1개인 상품 생성
        Product product = new Product("Limited Edition", "한정판 상품", BigDecimal.valueOf(50000), 1);
        Product savedProduct = productRepository.save(product);

        // 100명의 사용자 생성 (각각 충분한 잔액 보유)
        for (int i = 0; i < threadCount; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    User user = new User("user" + userIndex, "user" + userIndex + "@test.com");
                    user.chargeBalance(BigDecimal.valueOf(100000));
                    User savedUser = userRepository.save(user);

                    OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest(savedProduct.getProductId(), 1);
                    OrderRequest orderRequest = new OrderRequest(savedUser.getUserId(), Arrays.asList(item));

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
        executorService.shutdown();

        // then
        Product finalProduct = productRepository.findById(savedProduct.getProductId()).orElseThrow();
        
        assertThat(successCount.get()).isEqualTo(1); // 1명만 성공
        assertThat(failureCount.get()).isEqualTo(99); // 99명 실패
        assertThat(finalProduct.getStockQuantity()).isEqualTo(0); // 재고 0개
    }

    @Test
    @DisplayName("동시성 테스트 - 같은 사용자의 동시 잔액 차감")
    @Transactional
    void createOrder_Concurrency_SameUserBalance() throws InterruptedException {
        // given
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 잔액 100,000원인 사용자 생성
        User user = new User("testUser", "test@example.com");
        user.chargeBalance(BigDecimal.valueOf(100000));
        User savedUser = userRepository.save(user);

        // 가격 15,000원인 상품 생성 (충분한 재고)
        Product product = new Product("Test Product", "테스트 상품", BigDecimal.valueOf(15000), 100);
        Product savedProduct = productRepository.save(product);

        // 10번의 동시 주문 (각각 15,000원씩 총 150,000원 필요)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest(savedProduct.getProductId(), 1);
                    OrderRequest orderRequest = new OrderRequest(savedUser.getUserId(), Arrays.asList(item));

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
        executorService.shutdown();

        // then
        User finalUser = userRepository.findById(savedUser.getUserId()).orElseThrow();
        
        // 100,000원으로는 15,000원짜리 상품을 최대 6개까지만 살 수 있음
        assertThat(successCount.get()).isLessThanOrEqualTo(6);
        assertThat(failureCount.get()).isGreaterThanOrEqualTo(4);
        assertThat(finalUser.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        
        // 잔액 정확성 검증: 초기 잔액 - (성공한 주문 수 × 상품 가격)
        BigDecimal expectedBalance = BigDecimal.valueOf(100000)
                .subtract(BigDecimal.valueOf(successCount.get() * 15000));
        assertThat(finalUser.getBalance()).isEqualTo(expectedBalance);
    }
}